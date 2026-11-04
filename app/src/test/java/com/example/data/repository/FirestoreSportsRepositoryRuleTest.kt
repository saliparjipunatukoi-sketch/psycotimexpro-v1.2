package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FirestoreSportsRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAthleteAndRace_validPayload_createsDocumentsAndQueriesMatch() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreSportsRepository(firestore)

        val athleteIdResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createAthlete(
                name = "Muhd Azeem Fahmi",
                category = "Track",
                specificEvent = "100m Sprint",
                pbValue = "10.09"
            )
        }
        assertTrue(athleteIdResult.isSuccess)
        val athleteId = athleteIdResult.getOrThrow()
        assertTrue(athleteId.isNotBlank())

        val athletesListResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserAthletes()
        }
        assertTrue(athletesListResult.isSuccess)
        assertTrue(athletesListResult.getOrThrow().any { it.id == athleteId })

        val raceIdResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createRaceRecord(
                title = "Race 1 Final",
                eventName = "100m Sprint",
                athleteName = "Muhd Azeem Fahmi",
                recordedTimeMillis = 10090L,
                lane = 4
            )
        }
        assertTrue(raceIdResult.isSuccess)
        val raceId = raceIdResult.getOrThrow()

        val emittedRaces = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeRaces(uid).first { list -> list.any { it.id == raceId } }
        }
        assertTrue(emittedRaces.any { it.id == raceId })
    }

    @Test
    fun getAthleteById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreSportsRepository(firestore)
        val athleteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createAthlete(name = "Alice Sprinter").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = FirestoreSportsRepository(firestore)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getAthleteById(athleteId).getOrThrow()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user access")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun observeAthletes_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = FirestoreSportsRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeAthletes("unauth_uid").first()
            }
            fail("Expected exception for unauthenticated listener")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice.coach@test.com"
        const val BOB_EMAIL = "bob.coach@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
