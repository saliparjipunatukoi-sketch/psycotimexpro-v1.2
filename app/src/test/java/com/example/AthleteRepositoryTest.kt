package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Athlete
import com.example.data.repository.AthleteRepository
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
class AthleteRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: AthleteRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AthleteRepository(db.athleteDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAthleteRepository_InsertAndGetById() = runBlocking {
        val athlete = Athlete(
            name = "Harith Haiqal",
            category = "Track",
            specificEvent = "100m Sprint",
            pbValue = "10.35",
            pbUnit = "Saat (s)",
            coachEmail = "saliparjipun.atukoi@gmail.com"
        )

        val id = repository.insertAthlete(athlete)
        assertTrue(id > 0)

        val found = repository.getAthleteById(id)
        assertNotNull(found)
        assertEquals("Harith Haiqal", found?.name)
        assertEquals("10.35", found?.pbValue)
    }

    @Test
    fun testAthleteRepository_FlowAllAndFilterByCoachAndCategory() = runBlocking {
        val a1 = Athlete(name = "Azizul", category = "Track", specificEvent = "200m", pbValue = "21.2", pbUnit = "s", coachEmail = "saliparjipun.atukoi@gmail.com")
        val a2 = Athlete(name = "Hakim", category = "Field", specificEvent = "Lompat Jauh", pbValue = "7.45", pbUnit = "m", coachEmail = "saliparjipun.atukoi@gmail.com")
        val a3 = Athlete(name = "Zul", category = "Track", specificEvent = "400m", pbValue = "47.8", pbUnit = "s", coachEmail = "other@coach.com")

        repository.insertAthlete(a1)
        repository.insertAthlete(a2)
        repository.insertAthlete(a3)

        // 1. All athletes flow
        val all = repository.allAthletes.first()
        assertEquals(3, all.size)

        // 2. Filter by coach
        val coachAthletes = repository.getAthletesByCoach("saliparjipun.atukoi@gmail.com").first()
        assertEquals(2, coachAthletes.size)

        // 3. Filter by category
        val fieldAthletes = repository.getAthletesByCategory("Field").first()
        assertEquals(1, fieldAthletes.size)
        assertEquals("Hakim", fieldAthletes.first().name)
    }

    @Test
    fun testAthleteRepository_UpdateAndDelete() = runBlocking {
        val athlete = Athlete(
            name = "Syafiq",
            category = "Track",
            specificEvent = "100m",
            pbValue = "10.80",
            pbUnit = "s"
        )
        val id = repository.insertAthlete(athlete)

        // Update PB
        val updated = athlete.copy(id = id, pbValue = "10.55")
        repository.updateAthlete(updated)

        val retrieved = repository.getAthleteById(id)
        assertEquals("10.55", retrieved?.pbValue)

        // Delete
        repository.deleteAthlete(updated)
        val afterDelete = repository.getAthleteById(id)
        assertNull(afterDelete)
    }

    @Test
    fun testAthleteSearchFilter_ByName() = runBlocking {
        repository.insertAthlete(Athlete(name = "Muhammad Danish", category = "Track", specificEvent = "100m", pbValue = "10.45", pbUnit = "s"))
        repository.insertAthlete(Athlete(name = "Danish Hakim", category = "Field", specificEvent = "Lompat Jauh", pbValue = "7.15", pbUnit = "m"))
        repository.insertAthlete(Athlete(name = "Nur Aisyah", category = "Track", specificEvent = "200m", pbValue = "24.10", pbUnit = "s"))

        val all = repository.allAthletes.first()

        // Filter by "Danish"
        val query = "Danish"
        val filtered = all.filter { it.name.contains(query, ignoreCase = true) }
        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.name == "Muhammad Danish" })
        assertTrue(filtered.any { it.name == "Danish Hakim" })

        // Filter by "Aisyah"
        val filteredAisyah = all.filter { it.name.contains("Aisyah", ignoreCase = true) }
        assertEquals(1, filteredAisyah.size)
        assertEquals("Nur Aisyah", filteredAisyah.first().name)

        // Filter by non-existent name
        val emptyFilter = all.filter { it.name.contains("NonExistent", ignoreCase = true) }
        assertEquals(0, emptyFilter.size)
    }
}
