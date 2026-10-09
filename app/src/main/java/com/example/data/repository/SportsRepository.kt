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
        if (raceDao.getRaceCount() == 0) {
            // Seed Athletes
            val athlete1Id = athleteDao.insertAthlete(
                AthleteEntity(
                    name = "Muhammad Danish Haikal",
                    category = "Track",
                    specificEvent = "100m Sprint",
                    pbValue = "10.48",
                    pbUnit = "Saat (s)",
                    clubName = "Psyco Track & Field Elite",
                    coachEmail = "saliparjipun.atukoi@gmail.com",
                    registrationFee = 50.0,
                    feePaidStatus = true,
                    gender = "Lelaki",
                    notes = "Atlit Elit MSSN. Fasa permulaan blok pantas."
                )
            )

            val athlete2Id = athleteDao.insertAthlete(
                AthleteEntity(
                    name = "Nur Aisyah Binti Zulkifli",
                    category = "Track",
                    specificEvent = "200m Sprint",
                    pbValue = "24.62",
                    pbUnit = "Saat (s)",
                    clubName = "Psyco Track & Field Elite",
                    coachEmail = "saliparjipun.atukoi@gmail.com",
                    registrationFee = 50.0,
                    feePaidStatus = true,
                    gender = "Perempuan",
                    notes = "Penyandang Emas Sukma. Kelajuan selekoh tajam."
                )
            )

            val athlete3Id = athleteDao.insertAthlete(
                AthleteEntity(
                    name = "Ahmad Farhan Bin Roslan",
                    category = "Field",
                    specificEvent = "Lompat Jauh",
                    pbValue = "7.38",
                    pbUnit = "Meter (m)",
                    clubName = "Psyco Track & Field Elite",
                    coachEmail = "saliparjipun.atukoi@gmail.com",
                    registrationFee = 50.0,
                    feePaidStatus = false,
                    gender = "Lelaki",
                    notes = "Lari landas 38 meter dengan irama stabil."
                )
            )

            val athlete4Id = athleteDao.insertAthlete(
                AthleteEntity(
                    name = "Siti Khadijah Amira",
                    category = "Field",
                    specificEvent = "Lompat Tinggi",
                    pbValue = "1.74",
                    pbUnit = "Meter (m)",
                    clubName = "Psyco Track & Field Elite",
                    coachEmail = "saliparjipun.atukoi@gmail.com",
                    registrationFee = 50.0,
                    feePaidStatus = true,
                    gender = "Perempuan",
                    notes = "Gaya Fosbury Flop fleksibel."
                )
            )

            // Seed Races
            raceDao.insertRace(
                RaceRecordEntity(
                    title = "Race 1 - Ujian Masa 100m",
                    eventName = "100m Sprint",
                    athleteId = athlete1Id,
                    athleteName = "Muhammad Danish Haikal",
                    recordedTimeMillis = 10480,
                    windReading = "+1.1 m/s",
                    lane = 4,
                    createdByEmail = "saliparjipun.atukoi@gmail.com",
                    coachEmail = "saliparjipun.atukoi@gmail.com",
                    hasMotionDetected = true,
                    cadenceSpM = 264,
                    strideFreqHz = 4.40,
                    groundContactTimeMs = 104,
                    torsoLeanAngleDeg = 15.2,
                    notes = "Catatan rekod peribadi latihan terbaru dengan garisan laser Cam 2."
                )
            )

            raceDao.insertRace(
                RaceRecordEntity(
                    title = "Race 2 - Fasa Kelajuan Puncak",
                    eventName = "200m Sprint",
                    athleteId = athlete2Id,
                    athleteName = "Nur Aisyah Binti Zulkifli",
                    recordedTimeMillis = 24620,
                    windReading = "+0.4 m/s",
                    lane = 5,
                    createdByEmail = "saliparjipun.atukoi@gmail.com",
                    coachEmail = "saliparjipun.atukoi@gmail.com",
                    hasMotionDetected = true,
                    cadenceSpM = 252,
                    strideFreqHz = 4.20,
                    groundContactTimeMs = 112,
                    torsoLeanAngleDeg = 13.8,
                    notes = "Penyelarasan torso tegak pada meter ke-160 hingga garisan akhir."
                )
            )

            // Seed Sub-Users (Max 10 per coach as requested in instruction 1 & 5)
            subUserDao.insertSubUser(
                SubUserEntity(
                    username = "urusetia1",
                    fullName = "Encik Zulkarnain (Urusetia Kejohanan)",
                    role = "Urusetia",
                    parentCoachEmail = "saliparjipun.atukoi@gmail.com",
                    pinCode = "1001"
                )
            )

            subUserDao.insertSubUser(
                SubUserEntity(
                    username = "teknikal_gate2",
                    fullName = "Cikgu Razif (Pegawai Teknikal Cam 2)",
                    role = "Pegawai Teknikal",
                    parentCoachEmail = "saliparjipun.atukoi@gmail.com",
                    pinCode = "1002"
                )
            )

            // Seed Fees
            feeDao.insertFee(
                FeeRecordEntity(
                    athleteId = athlete1Id,
                    athleteName = "Muhammad Danish Haikal",
                    feeType = "Yuran Bulanan Latihan",
                    amount = 80.0,
                    isPaid = true,
                    receiptNo = "PTX-2026-081",
                    paymentDate = "01 Okt 2026",
                    notes = "Dibayar penuh secara online"
                )
            )

            feeDao.insertFee(
                FeeRecordEntity(
                    athleteId = athlete3Id,
                    athleteName = "Ahmad Farhan Bin Roslan",
                    feeType = "Yuran Bulanan Latihan",
                    amount = 80.0,
                    isPaid = false,
                    receiptNo = "PTX-2026-092",
                    paymentDate = "Tertunggak",
                    notes = "Pemberitahuan telah dihantar"
                )
            )

            // Seed Achievements
            achievementDao.insertAchievement(
                AchievementEntity(
                    athleteId = athlete1Id,
                    athleteName = "Muhammad Danish Haikal",
                    tournamentName = "Kejohanan Olahraga MSSD 2026",
                    eventName = "100m Lelaki B18",
                    medal = "Emas",
                    resultRecord = "10.52 s",
                    yearOrDate = "Mac 2026"
                )
            )

            achievementDao.insertAchievement(
                AchievementEntity(
                    athleteId = athlete2Id,
                    athleteName = "Nur Aisyah Binti Zulkifli",
                    tournamentName = "Kejohanan Terbuka Kebangsaan",
                    eventName = "200m Wanita",
                    medal = "Perak",
                    resultRecord = "24.70 s",
                    yearOrDate = "Julai 2026"
                )
            )

            // Seed TrainingSession
            val sessionId = trainingSessionDao.insertSession(
                TrainingSession(
                    sessionTitle = "Sesi Ujian Masa & Pecutan Maksimum 100m/200m",
                    venue = "Stadium Trek Olahraga Shah Alam",
                    coachEmail = "saliparjipun.atukoi@gmail.com",
                    category = "Track",
                    notes = "Ujian masa ET menggunakan dwi-kamera Cam 1 (Starter) dan Cam 2 (Torso Gate)."
                )
            )

            // Seed PerformanceMetrics (PB records stored locally)
            performanceMetricsDao.insertMetrics(
                PerformanceMetrics(
                    athleteId = athlete1Id,
                    athleteName = "Muhammad Danish Haikal",
                    sessionId = sessionId,
                    eventName = "100m Sprint",
                    pbValue = "10.48",
                    pbUnit = "Saat (s)",
                    isNewPb = true,
                    measuredTimeMillis = 10480L,
                    cadenceSpM = 264,
                    strideFrequencyHz = 4.40,
                    groundContactTimeMs = 104,
                    torsoLeanAngleDeg = 15.2,
                    notes = "Catatan Rekod Peribadi (PB) rasmi baharu."
                )
            )

            performanceMetricsDao.insertMetrics(
                PerformanceMetrics(
                    athleteId = athlete2Id,
                    athleteName = "Nur Aisyah Binti Zulkifli",
                    sessionId = sessionId,
                    eventName = "200m Sprint",
                    pbValue = "24.62",
                    pbUnit = "Saat (s)",
                    isNewPb = true,
                    measuredTimeMillis = 24620L,
                    cadenceSpM = 252,
                    strideFrequencyHz = 4.20,
                    groundContactTimeMs = 112,
                    torsoLeanAngleDeg = 13.8,
                    notes = "PB 200m selekoh tajam."
                )
            )

            performanceMetricsDao.insertMetrics(
                PerformanceMetrics(
                    athleteId = athlete3Id,
                    athleteName = "Ahmad Farhan Bin Roslan",
                    sessionId = sessionId,
                    eventName = "Lompat Jauh",
                    pbValue = "7.38",
                    pbUnit = "Meter (m)",
                    isNewPb = true,
                    measuredDistanceOrHeight = 7.38,
                    notes = "PB lompat jauh percubaan ke-3."
                )
            )
        }
    }
}
