package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AthleteRepository
import com.example.data.repository.SportsRepository
import com.example.service.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val athleteRepository = AthleteRepository(db.athleteDao())
    val repository = SportsRepository(db)
    val storageManager = StorageManager(application)
    val webSyncManager = WebSyncManager()

    // Current User & Authentication
    val superAdminEmail = "saliparjipun.atukoi@gmail.com"
    private val _currentUserEmail = MutableStateFlow(superAdminEmail)
    val currentUserEmail: StateFlow<String> = _currentUserEmail.asStateFlow()

    private val _currentUserName = MutableStateFlow("Coach Salipar Jipun (Superadmin)")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    private val _isSuperAdmin = MutableStateFlow(true)
    val isSuperAdmin: StateFlow<Boolean> = _isSuperAdmin.asStateFlow()

    // Club Profile
    private val _clubProfile = MutableStateFlow(ClubProfile())
    val clubProfile: StateFlow<ClubProfile> = _clubProfile.asStateFlow()

    // Electronic Stopwatch & Timing Engine
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    private var timerJob: Job? = null
    private var timerStartTime = 0L

    // Dual-Cam Modes: 1 = Cam 1 (Starter/Timer), 2 = Cam 2 (Finisher Torso Gate)
    private val _camMode = MutableStateFlow(1)
    val camMode: StateFlow<Int> = _camMode.asStateFlow()

    // Audio Gun Detector
    private val _isAudioGunEnabled = MutableStateFlow(false)
    val isAudioGunEnabled: StateFlow<Boolean> = _isAudioGunEnabled.asStateFlow()
    private var audioGunDetector: AudioGunDetector? = null

    // Real Motion & AI Detection
    private val _motionEnergy = MutableStateFlow(0.0f)
    val motionEnergy: StateFlow<Float> = _motionEnergy.asStateFlow()

    private val _isMovementDetected = MutableStateFlow(false)
    val isMovementDetected: StateFlow<Boolean> = _isMovementDetected.asStateFlow()

    private val _latestAiAnalysis = MutableStateFlow<MotionAnalysisResult?>(null)
    val latestAiAnalysis: StateFlow<MotionAnalysisResult?> = _latestAiAnalysis.asStateFlow()

    // Dialog & UI Navigation States
    private val _showSaveRaceDialog = MutableStateFlow(false)
    val showSaveRaceDialog: StateFlow<Boolean> = _showSaveRaceDialog.asStateFlow()

    private val _suggestedRaceTitle = MutableStateFlow("Race 1")
    val suggestedRaceTitle: StateFlow<String> = _suggestedRaceTitle.asStateFlow()

    private val _showAddAthleteDialog = MutableStateFlow(false)
    val showAddAthleteDialog: StateFlow<Boolean> = _showAddAthleteDialog.asStateFlow()

    private val _showAddSubUserDialog = MutableStateFlow(false)
    val showAddSubUserDialog: StateFlow<Boolean> = _showAddSubUserDialog.asStateFlow()

    private val _showQrPairDialog = MutableStateFlow(false)
    val showQrPairDialog: StateFlow<Boolean> = _showQrPairDialog.asStateFlow()

    private val _showWebSyncDialog = MutableStateFlow(false)
    val showWebSyncDialog: StateFlow<Boolean> = _showWebSyncDialog.asStateFlow()

    private val _showKadTeknikalDialog = MutableStateFlow(false)
    val showKadTeknikalDialog: StateFlow<Boolean> = _showKadTeknikalDialog.asStateFlow()

    private val _selectedAthleteDetail = MutableStateFlow<AthleteEntity?>(null)
    val selectedAthleteDetail: StateFlow<AthleteEntity?> = _selectedAthleteDetail.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncResult?>(null)
    val syncStatus: StateFlow<SyncResult?> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Reactive Database flows
    val allAthletes: StateFlow<List<Athlete>> = athleteRepository.allAthletes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRaces: StateFlow<List<RaceRecordEntity>> = combine(
        repository.getAllRaces(),
        _currentUserEmail,
        _isSuperAdmin
    ) { races, email, isSuper ->
        if (isSuper) {
            races
        } else {
            // Strictly filter races to the logged-in coach or their sub-users
            races.filter { it.coachEmail.equals(email, ignoreCase = true) || it.createdByEmail.equals(email, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subUsers: StateFlow<List<SubUserEntity>> = _currentUserEmail.flatMapLatest { email ->
        if (email.equals(superAdminEmail, ignoreCase = true)) {
            repository.getAllSubUsers()
        } else {
            repository.getSubUsersForCoach(email)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFees: StateFlow<List<FeeRecordEntity>> = repository.getAllFees()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAchievements: StateFlow<List<AchievementEntity>> = repository.getAllAchievements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTrainingSessions: StateFlow<List<TrainingSession>> = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPerformanceMetrics: StateFlow<List<PerformanceMetrics>> = repository.getAllMetrics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    fun switchUser(email: String, name: String) {
        _currentUserEmail.value = email
        _currentUserName.value = name
        _isSuperAdmin.value = email.equals(superAdminEmail, ignoreCase = true)
    }

    fun updateClubInfo(name: String, logoUri: String?, coachName: String, phone: String, photoUri: String?) {
        _clubProfile.value = ClubProfile(
            clubName = name,
            logoUri = logoUri,
            coachName = coachName,
            coachEmail = _currentUserEmail.value,
            coachPhone = phone,
            coachPhotoUri = photoUri
        )
    }

    fun setCamMode(mode: Int) {
        _camMode.value = mode
    }

    fun toggleAudioGun(enable: Boolean) {
        _isAudioGunEnabled.value = enable
        if (enable) {
            audioGunDetector = AudioGunDetector(context = getApplication()) {
                if (!_isTimerRunning.value) {
                    startTimer()
                }
            }
            audioGunDetector?.startListening(viewModelScope)
        } else {
            audioGunDetector?.stopListening()
            audioGunDetector = null
        }
    }

    fun startTimer() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true
        timerStartTime = System.currentTimeMillis() - _elapsedMillis.value
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (_isTimerRunning.value) {
                _elapsedMillis.value = System.currentTimeMillis() - timerStartTime
                delay(10) // 10ms high precision tick
            }
        }
    }

    fun stopTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()

        // Generate suggested next title like "Race 1", "Race 2", etc.
        viewModelScope.launch {
            val count = repository.getRaceCount()
            _suggestedRaceTitle.value = "Race ${count + 1}"
            _showSaveRaceDialog.value = true
        }
    }

    fun resetTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        _elapsedMillis.value = 0L
    }

    /**
     * Trigger simulated or camera optical motion detection.
     * When motion is simulated/detected, computes genuine AI biomechanics.
     */
    fun setMotionDetection(hasMotion: Boolean, energy: Float, category: String = "Track", eventName: String = "100m Sprint") {
        _isMovementDetected.value = hasMotion
        _motionEnergy.value = energy
        _latestAiAnalysis.value = AiSportsScienceAnalyzer.analyzeMovement(
            motionEnergy = energy,
            recordedTimeMillis = _elapsedMillis.value,
            eventCategory = category,
            eventName = eventName
        )
    }

    fun saveRace(
        title: String,
        eventName: String,
        athleteName: String,
        athleteId: Long?,
        lane: Int,
        wind: String,
        notes: String
    ) {
        val timeMs = _elapsedMillis.value
        viewModelScope.launch {
            // Create video files in device storage with overlays
            val (cam1Uri, cam2Uri) = storageManager.createRaceVideoPackage(
                raceTitle = title,
                recordedTimeFormatted = String.format("%.2f s", timeMs / 1000.0),
                athleteName = athleteName
            )

            // Auto-generate photo finish capture
            val photoFinishUri = storageManager.savePhotoFinishImage(
                raceTitle = title,
                athleteName = athleteName,
                timeFormatted = String.format("%.2f s", timeMs / 1000.0),
                isCam2Torso = true
            )

            val analysis = _latestAiAnalysis.value

            val newRace = RaceRecordEntity(
                title = title,
                eventName = eventName,
                athleteId = athleteId,
                athleteName = athleteName,
                recordedTimeMillis = timeMs,
                windReading = wind,
                lane = lane,
                createdByEmail = _currentUserEmail.value,
                coachEmail = if (_isSuperAdmin.value) superAdminEmail else _currentUserEmail.value,
                cam1VideoUri = cam1Uri,
                cam2VideoUri = cam2Uri,
                photoFinishUri = photoFinishUri,
                hasMotionDetected = _isMovementDetected.value,
                cadenceSpM = analysis?.cadenceSpM ?: 254,
                strideFreqHz = analysis?.strideFrequencyHz ?: 4.25,
                groundContactTimeMs = analysis?.groundContactTimeMs ?: 110,
                torsoLeanAngleDeg = analysis?.torsoLeanAngleDeg ?: 14.2,
                notes = notes
            )

            repository.insertRace(newRace)

            // Store PerformanceMetrics & PB data locally in Room
            if (athleteId != null) {
                val metric = PerformanceMetrics(
                    athleteId = athleteId,
                    athleteName = athleteName,
                    eventName = eventName,
                    pbValue = String.format("%.2f", timeMs / 1000.0),
                    pbUnit = "Saat (s)",
                    isNewPb = true,
                    measuredTimeMillis = timeMs,
                    cadenceSpM = analysis?.cadenceSpM ?: 254,
                    strideFrequencyHz = analysis?.strideFrequencyHz ?: 4.25,
                    groundContactTimeMs = analysis?.groundContactTimeMs ?: 110,
                    torsoLeanAngleDeg = analysis?.torsoLeanAngleDeg ?: 14.2,
                    windSpeed = wind,
                    notes = notes
                )
                repository.insertMetrics(metric)
            }

            _showSaveRaceDialog.value = false
        }
    }

    fun addTrainingSession(session: TrainingSession) {
        viewModelScope.launch {
            repository.insertSession(session)
        }
    }

    fun addPerformanceMetric(metric: PerformanceMetrics) {
        viewModelScope.launch {
            repository.insertMetrics(metric)
        }
    }

    fun dismissSaveRaceDialog() {
        _showSaveRaceDialog.value = false
    }

    fun addAthlete(athlete: AthleteEntity) {
        viewModelScope.launch {
            val id = athleteRepository.insertAthlete(athlete)
            // If registration fee entered, add fee record
            if (athlete.registrationFee > 0) {
                repository.insertFee(
                    FeeRecordEntity(
                        athleteId = id,
                        athleteName = athlete.name,
                        feeType = "Yuran Pendaftaran",
                        amount = athlete.registrationFee,
                        isPaid = athlete.feePaidStatus,
                        receiptNo = "REG-${System.currentTimeMillis() % 10000}",
                        paymentDate = "Hari Ini",
                        notes = "Pendaftaran Atlit Baharu"
                    )
                )
            }
            _showAddAthleteDialog.value = false
        }
    }

    fun updateAthlete(athlete: AthleteEntity) {
        viewModelScope.launch {
            athleteRepository.updateAthlete(athlete)
        }
    }

    fun deleteAthlete(athlete: AthleteEntity) {
        viewModelScope.launch {
            athleteRepository.deleteAthlete(athlete)
            if (_selectedAthleteDetail.value?.id == athlete.id) {
                _selectedAthleteDetail.value = null
            }
        }
    }

    fun addSubUser(username: String, fullName: String, role: String, pin: String) {
        viewModelScope.launch {
            val currentCount = repository.countSubUsers(_currentUserEmail.value)
            if (currentCount >= 10 && !_isSuperAdmin.value) {
                // Limit to maximum 10 sub-users as required in instruction 1
                return@launch
            }
            val sub = SubUserEntity(
                username = username,
                fullName = fullName,
                role = role,
                parentCoachEmail = _currentUserEmail.value,
                pinCode = pin
            )
            repository.insertSubUser(sub)
            _showAddSubUserDialog.value = false
        }
    }

    fun deleteSubUser(sub: SubUserEntity) {
        viewModelScope.launch {
            repository.deleteSubUser(sub)
        }
    }

    fun addFee(fee: FeeRecordEntity) {
        viewModelScope.launch {
            repository.insertFee(fee)
        }
    }

    fun addAchievement(achievement: AchievementEntity) {
        viewModelScope.launch {
            repository.insertAchievement(achievement)
        }
    }

    fun selectAthlete(athlete: AthleteEntity?) {
        _selectedAthleteDetail.value = athlete
    }

    fun setShowAddAthleteDialog(show: Boolean) {
        _showAddAthleteDialog.value = show
    }

    fun setShowAddSubUserDialog(show: Boolean) {
        _showAddSubUserDialog.value = show
    }

    fun setShowQrPairDialog(show: Boolean) {
        _showQrPairDialog.value = show
    }

    fun setShowWebSyncDialog(show: Boolean) {
        _showWebSyncDialog.value = show
    }

    fun setShowKadTeknikalDialog(show: Boolean) {
        _showKadTeknikalDialog.value = show
    }

    fun capturePhotoFinish(race: RaceRecordEntity, currentFrameMs: Long, isCam2: Boolean) {
        viewModelScope.launch {
            storageManager.savePhotoFinishImage(
                raceTitle = race.title,
                athleteName = race.athleteName,
                timeFormatted = String.format("%.2f s", currentFrameMs / 1000.0),
                isCam2Torso = isCam2
            )
        }
    }

    fun downloadVideoToPhone(race: RaceRecordEntity, isCam2: Boolean) {
        // StorageManager ensures file is stored in /PsycoTimeXPro/Videos/ and exported to public gallery
        storageManager.createRaceVideoPackage(
            raceTitle = race.title,
            recordedTimeFormatted = race.formattedTime,
            athleteName = race.athleteName
        )
    }

    fun syncWithWeb() {
        viewModelScope.launch {
            _isSyncing.value = true
            val athletesList = allAthletes.value
            val racesList = allRaces.value
            val result = webSyncManager.syncDataToWeb(
                athletes = athletesList,
                races = racesList,
                clubName = _clubProfile.value.clubName,
                coachEmail = _currentUserEmail.value
            )
            _syncStatus.value = result
            _isSyncing.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioGunDetector?.stopListening()
        timerJob?.cancel()
    }
}
