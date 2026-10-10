package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class SportsRepository(private val db: AppDatabase) {

    private val athleteDao = db.athleteDao()
    private val trainingSessionDao = db.trainingSessionDao()
    private val performanceMetricsDao = db.performanceMetricsDao()
    private val raceDao = db.raceDao()
    private val subUserDao = db.subUserDao()
    private val feeDao = db.feeDao()
    private val achievementDao = db.achievementDao()

    // Athlete queries
    fun getAllAthletes(): Flow<List<Athlete>> = athleteDao.getAllAthletes()
    fun getAthletesByCoach(coachEmail: String): Flow<List<Athlete>> = athleteDao.getAthletesByCoach(coachEmail)
    suspend fun getAthleteById(id: Long): Athlete? = athleteDao.getAthleteById(id)
    suspend fun insertAthlete(athlete: Athlete): Long = athleteDao.insertAthlete(athlete)
    suspend fun updateAthlete(athlete: Athlete) = athleteDao.updateAthlete(athlete)
    suspend fun deleteAthlete(athlete: Athlete) = athleteDao.deleteAthlete(athlete)

    // TrainingSession queries
    fun getAllSessions(): Flow<List<TrainingSession>> = trainingSessionDao.getAllSessions()
    fun getSessionsByCoach(coachEmail: String): Flow<List<TrainingSession>> = trainingSessionDao.getSessionsByCoach(coachEmail)
    suspend fun getSessionById(id: Long): TrainingSession? = trainingSessionDao.getSessionById(id)
    suspend fun insertSession(session: TrainingSession): Long = trainingSessionDao.insertSession(session)
    suspend fun updateSession(session: TrainingSession) = trainingSessionDao.updateSession(session)
    suspend fun deleteSession(session: TrainingSession) = trainingSessionDao.deleteSession(session)

    // PerformanceMetrics & PB queries
    fun getAllMetrics(): Flow<List<PerformanceMetrics>> = performanceMetricsDao.getAllMetrics()
    fun getMetricsForAthlete(athleteId: Long): Flow<List<PerformanceMetrics>> = performanceMetricsDao.getMetricsForAthlete(athleteId)
    fun getPbsForAthlete(athleteId: Long): Flow<List<PerformanceMetrics>> = performanceMetricsDao.getPbsForAthlete(athleteId)
    fun getMetricsForSession(sessionId: Long): Flow<List<PerformanceMetrics>> = performanceMetricsDao.getMetricsForSession(sessionId)
    suspend fun getMetricById(id: Long): PerformanceMetrics? = performanceMetricsDao.getMetricById(id)
    suspend fun insertMetrics(metrics: PerformanceMetrics): Long = performanceMetricsDao.insertMetrics(metrics)
    suspend fun updateMetrics(metrics: PerformanceMetrics) = performanceMetricsDao.updateMetrics(metrics)
    suspend fun deleteMetrics(metrics: PerformanceMetrics) = performanceMetricsDao.deleteMetrics(metrics)

    // Race queries
    fun getAllRaces(): Flow<List<RaceRecordEntity>> = raceDao.getAllRaces()
    fun getRacesForUser(coachEmail: String, userEmail: String): Flow<List<RaceRecordEntity>> =
        raceDao.getRacesForUser(coachEmail, userEmail)
    fun getRacesForAthlete(athleteId: Long): Flow<List<RaceRecordEntity>> = raceDao.getRacesForAthlete(athleteId)
    suspend fun getRaceById(id: Long): RaceRecordEntity? = raceDao.getRaceById(id)
    suspend fun getRaceCount(): Int = raceDao.getRaceCount()
    suspend fun insertRace(race: RaceRecordEntity): Long = raceDao.insertRace(race)
    suspend fun updateRace(race: RaceRecordEntity) = raceDao.updateRace(race)
    suspend fun deleteRace(race: RaceRecordEntity) = raceDao.deleteRace(race)

    // SubUser queries
    fun getSubUsersForCoach(parentEmail: String): Flow<List<SubUserEntity>> =
        subUserDao.getSubUsersForCoach(parentEmail)
    fun getAllSubUsers(): Flow<List<SubUserEntity>> = subUserDao.getAllSubUsers()
    suspend fun countSubUsers(parentEmail: String): Int = subUserDao.countSubUsers(parentEmail)
    suspend fun insertSubUser(subUser: SubUserEntity): Long = subUserDao.insertSubUser(subUser)
    suspend fun deleteSubUser(subUser: SubUserEntity) = subUserDao.deleteSubUser(subUser)

    // Fee queries
    fun getAllFees(): Flow<List<FeeRecordEntity>> = feeDao.getAllFees()
    fun getFeesForAthlete(athleteId: Long): Flow<List<FeeRecordEntity>> = feeDao.getFeesForAthlete(athleteId)
    suspend fun insertFee(fee: FeeRecordEntity): Long = feeDao.insertFee(fee)
    suspend fun updateFee(fee: FeeRecordEntity) = feeDao.updateFee(fee)
    suspend fun deleteFee(fee: FeeRecordEntity) = feeDao.deleteFee(fee)

    // Achievement queries
    fun getAchievementsForAthlete(athleteId: Long): Flow<List<AchievementEntity>> =
        achievementDao.getAchievementsForAthlete(athleteId)
    fun getAllAchievements(): Flow<List<AchievementEntity>> = achievementDao.getAllAchievements()
    suspend fun insertAchievement(achievement: AchievementEntity): Long = achievementDao.insertAchievement(achievement)
    suspend fun deleteAchievement(achievement: AchievementEntity) = achievementDao.deleteAchievement(achievement)

    suspend fun seedSampleDataIfEmpty() {
        // App starts clean and fresh with 0 initial athletes or races
    }
}
