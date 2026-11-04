# Security Specification (Phase 0: Payload-First Security TDD)

## 1. Data Invariants
1. **Identity Isolation (`userId`)**: Every document in `/athletes/{athleteId}` and `/race_records/{raceId}` MUST belong to the authenticated user (`userId == request.auth.uid`). Cross-user reads, queries, updates, and deletes are strictly prohibited.
2. **Path Variable Hardening**: Every single-document ID (`athleteId`, `raceId`) MUST match `^[a-zA-Z0-9_\-]+$` with length `1..128`.
3. **Immutable Ownership & Creation Timestamp**: On `update`, `userId` and `createdAt` MUST remain identical to `resource.data`.
4. **Temporal Integrity**: `createdAt` (and `updatedAt` when present) MUST be a server `timestamp` (`<= request.time`).
5. **Strict Key Allowlisting & Size Bounds**: Every string field enforces `.size()` limits, and `keys().hasOnly(...)` / `affectedKeys().hasOnly(...)` blocks shadow/ghost fields.
6. **Query Enforcement**: `allow list` enforces `resource.data.userId == request.auth.uid` so unaligned queries without `.whereEqualTo("userId", uid)` are rejected.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Read/List**: `unauthDb.collection("athletes").get()` -> REJECTED.
2. **Cross-User Read (`get`)**: Alice reading Bob's `/athletes/bob_athlete_1` -> REJECTED.
3. **Unfiltered Collection Query (`list`)**: Alice querying `athletes` without `.where("userId", "==", ALICE_UID)` -> REJECTED.
4. **Identity Spoofing on Create**: Alice creating `/athletes/a1` with `userId: BOB_UID` -> REJECTED.
5. **Shadow / Ghost Field Injection on Create**: Creating `/athletes/a1` with `isAdmin: true` -> REJECTED.
6. **Shadow / Ghost Field Injection on Update**: Updating `/athletes/a1` with `isVerified: true` -> REJECTED.
7. **Ownership Hijack on Update**: Alice mutating `userId` on `/athletes/a1` to `BOB_UID` -> REJECTED.
8. **Immortal Field Mutation (`createdAt`)**: Updating `createdAt` on `/athletes/a1` -> REJECTED.
9. **String Overflow (Denial of Wallet)**: Creating `/athletes/a1` with `name` > 120 chars -> REJECTED.
10. **Invalid Path ID Poisoning**: Creating `/athletes/bad$id!` -> REJECTED.
11. **Value Type Poisoning on Update**: Updating `recordedTimeMillis` on `/race_records/r1` with a String -> REJECTED.
12. **Negative Sprint Time Injection**: Creating `/race_records/r1` with `recordedTimeMillis: -50` -> REJECTED.
