package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Athlete
import com.example.data.model.PerformanceMetrics
import com.example.data.model.TrainingSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomDatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveAthleteRegistration() = runBlocking {
        val athleteDao = db.athleteDao()
        val athlete = Athlete(
            name = "Ahmad Adam",
            category = "Track",
            specificEvent = "100m Sprint",
            pbValue = "10.45",
            pbUnit = "Saat (s)",
            clubName = "Psyco Track Elite",
            coachEmail = "saliparjipun.atukoi@gmail.com",
            registrationFee = 50.0,
            feePaidStatus = true
        )

        val athleteId = athleteDao.insertAthlete(athlete)
        assertTrue(athleteId > 0)

        val retrieved = athleteDao.getAthleteById(athleteId)
        assertNotNull(retrieved)
        assertEquals("Ahmad Adam", retrieved?.name)
        assertEquals("10.45", retrieved?.pbValue)
        assertEquals("Saat (s)", retrieved?.pbUnit)
    }

    @Test
    fun testInsertTrainingSessionAndPerformanceMetricsPB() = runBlocking {
        val sessionDao = db.trainingSessionDao()
        val metricsDao = db.performanceMetricsDao()

        // 1. Store TrainingSession locally
        val session = TrainingSession(
            sessionTitle = "Ujian Masa Pecutan",
            venue = "Stadium Shah Alam",
            category = "Track"
        )
        val sessionId = sessionDao.insertSession(session)
        assertTrue(sessionId > 0)

        val sessions = sessionDao.getAllSessions().first()
        assertEquals(1, sessions.size)
        assertEquals("Ujian Masa Pecutan", sessions.first().sessionTitle)

        // 2. Store PerformanceMetrics & PB locally
        val metric = PerformanceMetrics(
            athleteId = 101L,
            athleteName = "Ahmad Adam",
            sessionId = sessionId,
            eventName = "100m Sprint",
            pbValue = "10.42",
            pbUnit = "Saat (s)",
            isNewPb = true,
            measuredTimeMillis = 10420L,
            cadenceSpM = 268,
            torsoLeanAngleDeg = 14.5
        )
        val metricId = metricsDao.insertMetrics(metric)
        assertTrue(metricId > 0)

        val athletePBs = metricsDao.getPbsForAthlete(101L).first()
        assertEquals(1, athletePBs.size)
        assertEquals("10.42", athletePBs.first().pbValue)
        assertTrue(athletePBs.first().isNewPb)
    }

    @Test
    fun testAthleteDao_CategoryQueryAndUpdates() = runBlocking {
        val athleteDao = db.athleteDao()
        val a1 = Athlete(name = "Pelari 1", category = "Track", specificEvent = "100m", pbValue = "10.5", pbUnit = "s")
        val a2 = Athlete(name = "Pelompat 1", category = "Field", specificEvent = "Lompat Jauh", pbValue = "7.2", pbUnit = "m")

        val id1 = athleteDao.insertAthlete(a1)
        val id2 = athleteDao.insertAthlete(a2)

        val trackAthletes = athleteDao.getAthletesByCategory("Track").first()
        assertEquals(1, trackAthletes.size)
        assertEquals("Pelari 1", trackAthletes.first().name)

        // Update test
        val updated = a1.copy(id = id1, pbValue = "10.38")
        athleteDao.updateAthlete(updated)

        val refreshed = athleteDao.getAthleteById(id1)
        assertEquals("10.38", refreshed?.pbValue)

        // Delete test
        athleteDao.deleteAthlete(updated)
        val afterDelete = athleteDao.getAthleteById(id1)
        assertNull(afterDelete)
    }

    @Test
    fun testAppDatabase_SingletonInitialization() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val instance1 = AppDatabase.getDatabase(context)
        val instance2 = AppDatabase.getDatabase(context)

        assertNotNull(instance1)
        assertSame(instance1, instance2)
        assertNotNull(instance1.athleteDao())
        assertNotNull(instance1.trainingSessionDao())
        assertNotNull(instance1.performanceMetricsDao())
    }
}
