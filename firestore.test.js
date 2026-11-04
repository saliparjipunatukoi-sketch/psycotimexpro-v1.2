const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_coach_123";
const BOB_UID = "bob_coach_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 2000);
}

function validAthletePayload(userId = ALICE_UID) {
  const ts = pastTimestamp();
  return {
    userId,
    name: "Muhd Azeem Fahmi",
    category: "Track",
    specificEvent: "100m Sprint",
    pbValue: "10.09",
    pbUnit: "Saat (s)",
    clubName: "Psyco Track & Field Elite",
    coachEmail: "saliparjipun.atukoi@gmail.com",
    gender: "Lelaki",
    dateOfBirth: "2004-04-29",
    notes: "National record holder",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validRacePayload(userId = ALICE_UID) {
  const ts = pastTimestamp();
  return {
    userId,
    title: "Race 1",
    eventName: "100m Sprint",
    athleteName: "Muhd Azeem Fahmi",
    recordedTimeMillis: 10090,
    windReading: "+0.8 m/s",
    lane: 4,
    coachEmail: "saliparjipun.atukoi@gmail.com",
    cadenceSpM: 265,
    strideFreqHz: 4.42,
    groundContactTimeMs: 102,
    torsoLeanAngleDeg: 14.8,
    notes: "Electronic starter gun + Cam 2 torso gate",
    createdAt: ts,
    updatedAt: ts,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- DIRTY DOZEN SECURITY BOUNDS TESTS ---

test("1. Unauthenticated user: cannot read or list athletes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("athletes").get());
  await assertFails(unauthDb.collection("athletes").doc("ath_1").get());
});

test("2. Cross-user access: Alice cannot read Bob's athlete or race record", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("athletes").doc("bob_ath").set(validAthletePayload(BOB_UID));
    await context.firestore().collection("race_records").doc("bob_race").set(validRacePayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("athletes").doc("bob_ath").get());
  await assertFails(aliceDb.collection("race_records").doc("bob_race").get());
});

test("3. Unfiltered query: Authenticated user fails query without userId filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("athletes").get());
  await assertFails(aliceDb.collection("race_records").get());
});

test("4. Identity spoofing on create: Alice cannot create document with Bob's userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("athletes").doc("spoof_ath").set(validAthletePayload(BOB_UID))
  );
  await assertFails(
    aliceDb.collection("race_records").doc("spoof_race").set(validRacePayload(BOB_UID))
  );
});

test("5. Shadow field injection on create: rejects unexpected ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("athletes").doc("ghost_ath").set({
      ...validAthletePayload(ALICE_UID),
      isAdmin: true,
    })
  );
});

test("6. Shadow field injection on update: rejects unexpected ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("athletes").doc("ath_1").set(validAthletePayload(ALICE_UID))
  );
  await assertFails(
    aliceDb.collection("athletes").doc("ath_1").update({
      pbValue: "10.05",
      isVerified: true,
    })
  );
});

test("7. Ownership hijack on update: Alice cannot transfer userId to Bob", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("athletes").doc("ath_1").set(validAthletePayload(ALICE_UID))
  );
  await assertFails(
    aliceDb.collection("athletes").doc("ath_1").update({
      userId: BOB_UID,
    })
  );
});

test("8. Negative sprint time or invalid lane: rejected by schema validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("race_records").doc("bad_race_1").set({
      ...validRacePayload(ALICE_UID),
      recordedTimeMillis: -100,
    })
  );
  await assertFails(
    aliceDb.collection("race_records").doc("bad_race_2").set({
      ...validRacePayload(ALICE_UID),
      lane: 99,
    })
  );
});

// --- CLIENT QUERY ALIGNMENT & VALID CRUD TESTS ---

test("9. Authenticated owner: can create, update, query, and delete own athletes and races", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("athletes").doc("ath_valid").set(validAthletePayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("athletes").doc("ath_valid").update({
      pbValue: "10.02",
      updatedAt: pastTimestamp(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("athletes").where("userId", "==", ALICE_UID).get()
  );

  await assertSucceeds(
    aliceDb.collection("race_records").doc("race_valid").set(validRacePayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("race_records").where("userId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("race_records").doc("race_valid").delete()
  );
});
