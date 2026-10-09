package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AthleteDao {
    @Query("SELECT * FROM athletes ORDER BY name ASC")
    fun getAllAthletes(): Flow<List<Athlete>>

    @Query("SELECT * FROM athletes WHERE coachEmail = :coachEmail ORDER BY name ASC")
    fun getAthletesByCoach(coachEmail: String): Flow<List<Athlete>>

    @Query("SELECT * FROM athletes WHERE category = :category ORDER BY name ASC")
    fun getAthletesByCategory(category: String): Flow<List<Athlete>>

    @Query("SELECT * FROM athletes WHERE id = :id LIMIT 1")
    suspend fun getAthleteById(id: Long): Athlete?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAthlete(athlete: Athlete): Long

    @Update
    suspend fun updateAthlete(athlete: Athlete)

    @Delete
    suspend fun deleteAthlete(athlete: Athlete)
}

@Dao
interface RaceDao {
    @Query("SELECT * FROM race_records ORDER BY createdAt DESC")
    fun getAllRaces(): Flow<List<RaceRecordEntity>>

    @Query("SELECT * FROM race_records WHERE coachEmail = :coachEmail OR createdByEmail = :userEmail ORDER BY createdAt DESC")
    fun getRacesForUser(coachEmail: String, userEmail: String): Flow<List<RaceRecordEntity>>

    @Query("SELECT * FROM race_records WHERE athleteId = :athleteId ORDER BY createdAt DESC")
    fun getRacesForAthlete(athleteId: Long): Flow<List<RaceRecordEntity>>

    @Query("SELECT * FROM race_records WHERE id = :id LIMIT 1")
    suspend fun getRaceById(id: Long): RaceRecordEntity?

    @Query("SELECT COUNT(*) FROM race_records")
    suspend fun getRaceCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRace(race: RaceRecordEntity): Long

    @Update
    suspend fun updateRace(race: RaceRecordEntity)

    @Delete
    suspend fun deleteRace(race: RaceRecordEntity)
}

@Dao
interface SubUserDao {
    @Query("SELECT * FROM sub_users WHERE parentCoachEmail = :parentEmail ORDER BY createdAt DESC")
    fun getSubUsersForCoach(parentEmail: String): Flow<List<SubUserEntity>>

    @Query("SELECT * FROM sub_users ORDER BY createdAt DESC")
    fun getAllSubUsers(): Flow<List<SubUserEntity>>

    @Query("SELECT COUNT(*) FROM sub_users WHERE parentCoachEmail = :parentEmail")
    suspend fun countSubUsers(parentEmail: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubUser(subUser: SubUserEntity): Long

    @Delete
    suspend fun deleteSubUser(subUser: SubUserEntity)
}

@Dao
interface FeeDao {
    @Query("SELECT * FROM fee_records ORDER BY id DESC")
    fun getAllFees(): Flow<List<FeeRecordEntity>>

    @Query("SELECT * FROM fee_records WHERE athleteId = :athleteId ORDER BY id DESC")
    fun getFeesForAthlete(athleteId: Long): Flow<List<FeeRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFee(fee: FeeRecordEntity): Long

    @Update
    suspend fun updateFee(fee: FeeRecordEntity)

    @Delete
    suspend fun deleteFee(fee: FeeRecordEntity)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements WHERE athleteId = :athleteId ORDER BY id DESC")
    fun getAchievementsForAthlete(athleteId: Long): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements ORDER BY id DESC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity): Long

    @Delete
    suspend fun deleteAchievement(achievement: AchievementEntity)
}

@Dao
interface TrainingSessionDao {
    @Query("SELECT * FROM training_sessions ORDER BY dateMillis DESC")
    fun getAllSessions(): Flow<List<TrainingSession>>

    @Query("SELECT * FROM training_sessions WHERE coachEmail = :coachEmail ORDER BY dateMillis DESC")
    fun getSessionsByCoach(coachEmail: String): Flow<List<TrainingSession>>

    @Query("SELECT * FROM training_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): TrainingSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TrainingSession): Long

    @Update
    suspend fun updateSession(session: TrainingSession)

    @Delete
    suspend fun deleteSession(session: TrainingSession)
}

@Dao
interface PerformanceMetricsDao {
    @Query("SELECT * FROM performance_metrics ORDER BY recordedAt DESC")
    fun getAllMetrics(): Flow<List<PerformanceMetrics>>

    @Query("SELECT * FROM performance_metrics WHERE athleteId = :athleteId ORDER BY recordedAt DESC")
    fun getMetricsForAthlete(athleteId: Long): Flow<List<PerformanceMetrics>>

    @Query("SELECT * FROM performance_metrics WHERE athleteId = :athleteId AND isNewPb = 1 ORDER BY recordedAt DESC")
    fun getPbsForAthlete(athleteId: Long): Flow<List<PerformanceMetrics>>

    @Query("SELECT * FROM performance_metrics WHERE sessionId = :sessionId ORDER BY recordedAt DESC")
    fun getMetricsForSession(sessionId: Long): Flow<List<PerformanceMetrics>>

    @Query("SELECT * FROM performance_metrics WHERE id = :id LIMIT 1")
    suspend fun getMetricById(id: Long): PerformanceMetrics?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetrics(metrics: PerformanceMetrics): Long

    @Update
    suspend fun updateMetrics(metrics: PerformanceMetrics)

    @Delete
    suspend fun deleteMetrics(metrics: PerformanceMetrics)
}

