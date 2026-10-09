package com.example.data.repository

import com.example.data.local.AthleteDao
import com.example.data.model.Athlete
import kotlinx.coroutines.flow.Flow

/**
 * Repository that abstracts data operations for the [Athlete] DAO,
 * providing clean reactive and suspend access for the ViewModel layer.
 */
class AthleteRepository(private val athleteDao: AthleteDao) {

    /**
     * Observes all registered athletes ordered alphabetically by name.
     */
    val allAthletes: Flow<List<Athlete>> = athleteDao.getAllAthletes()

    /**
     * Observes athletes registered under a specific coach email.
     */
    fun getAthletesByCoach(coachEmail: String): Flow<List<Athlete>> {
        return athleteDao.getAthletesByCoach(coachEmail)
    }

    /**
     * Observes athletes filtered by category ("Track" vs "Field").
     */
    fun getAthletesByCategory(category: String): Flow<List<Athlete>> {
        return athleteDao.getAthletesByCategory(category)
    }

    /**
     * Retrieves an athlete by ID.
     */
    suspend fun getAthleteById(id: Long): Athlete? {
        return athleteDao.getAthleteById(id)
    }

    /**
     * Inserts a new athlete or replaces an existing one, returning the generated row ID.
     */
    suspend fun insertAthlete(athlete: Athlete): Long {
        return athleteDao.insertAthlete(athlete)
    }

    /**
     * Updates an existing athlete record.
     */
    suspend fun updateAthlete(athlete: Athlete) {
        athleteDao.updateAthlete(athlete)
    }

    /**
     * Deletes an athlete record.
     */
    suspend fun deleteAthlete(athlete: Athlete) {
        athleteDao.deleteAthlete(athlete)
    }
}
