package com.example.viewmodel

import android.app.Application
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AthleteRepository
import com.example.data.repository.FirestoreSportsRepository
import com.example.data.repository.SportsRepository
import com.example.service.*
import com.example.ui.components.CameraFrameHolder
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val athleteRepository = AthleteRepository(db.athleteDao())
    val repository = SportsRepository(db)
    val storageManager = StorageManager(application)
    val webSyncManager = WebSyncManager()
    val bluetoothTriggerManager = BluetoothSelfieTriggerManager(application)

    // Provisioned Firestore Named Database & Repository
    private val firestoreDatabaseId: String =
        application.getString(R.string.firestore_database_id)
    private val firestoreDb: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(firestoreDatabaseId)
    }
    val firestoreRepository: FirestoreSportsRepository by lazy {
        FirestoreSportsRepository(firestoreDb)
    }

    // Current User & Authentication
    val superAdminEmail = "saliparjipun.atukoi@gmail.com"
    val remoteAppControlManager = RemoteAppControlManager()
    private val _remoteAppStatus = MutableStateFlow(RemoteAppStatus(isApproved = true))
    val remoteAppStatus: StateFlow<RemoteAppStatus> = _remoteAppStatus.asStateFlow()

    private val _currentLanguage = MutableStateFlow(AppLanguage.MALAY)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun setLanguage(lang: AppLanguage) {
        _currentLanguage.value = lang
    }

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserEmail = MutableStateFlow("")
    val currentUserEmail: StateFlow<String> = _currentUserEmail.asStateFlow()

    private val _currentUserName = MutableStateFlow("")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    private val _isSuperAdmin = MutableStateFlow(false)
    val isSuperAdmin: StateFlow<Boolean> = _isSuperAdmin.asStateFlow()

    // Club Profile
    private val _clubProfile = MutableStateFlow(ClubProfile())
    val clubProfile: StateFlow<ClubProfile> = _clubProfile.asStateFlow()

    // Electronic Stopwatch, Live Video Recording & Timing Engine
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _isRecordingVideo = MutableStateFlow(false)
    val isRecordingVideo: StateFlow<Boolean> = _isRecordingVideo.asStateFlow()

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    private var timerJob: Job? = null
    private var timerStartTime = 0L

    // Camera Modes: 1 = Cam 1 (Portrait Runner Tracker), 2 = Cam 2 (Finisher Torso Gate), 3 = 1-Phone 100M Horizon AI
    private val _camMode = MutableStateFlow(1)
    val camMode: StateFlow<Int> = _camMode.asStateFlow()

    // Audio Gun Detector & Bluetooth BLE-M3 / Selfie Remote Trigger
    private val _isAudioGunEnabled = MutableStateFlow(true)
    val isAudioGunEnabled: StateFlow<Boolean> = _isAudioGunEnabled.asStateFlow()
    private var audioGunDetector: AudioGunDetector? = null

    private val _isBluetoothTriggerEnabled = MutableStateFlow(true)
    val isBluetoothTriggerEnabled: StateFlow<Boolean> = _isBluetoothTriggerEnabled.asStateFlow()

    private val _showBluetoothRemoteSetupDialog = MutableStateFlow(false)
    val showBluetoothRemoteSetupDialog: StateFlow<Boolean> = _showBluetoothRemoteSetupDialog.asStateFlow()

    private val _lastRemoteKeySignal = MutableStateFlow<RemoteKeySignal?>(null)
    val lastRemoteKeySignal: StateFlow<RemoteKeySignal?> = _lastRemoteKeySignal.asStateFlow()

    // Strict Tab Lock: Lock Remote Cursor & Trigger to Timing ET tab only
    private val _isTimingTabActive = MutableStateFlow(false)
    val isTimingTabActive: StateFlow<Boolean> = _isTimingTabActive.asStateFlow()

    fun setTimingTabActive(isActive: Boolean) {
        _isTimingTabActive.value = isActive
    }

    // Real Motion & AI Detection
    private val _motionEnergy = MutableStateFlow(0.0f)
    val motionEnergy: StateFlow<Float> = _motionEnergy.asStateFlow()

    private val _isMovementDetected = MutableStateFlow(false)
    val isMovementDetected: StateFlow<Boolean> = _isMovementDetected.asStateFlow()

    private val _latestAiAnalysis = MutableStateFlow<MotionAnalysisResult?>(null)
    val latestAiAnalysis: StateFlow<MotionAnalysisResult?> = _latestAiAnalysis.asStateFlow()

    // Sand Jump AI (Lompat Jauh / Triple Jump / Sukan Pasir) State
    private val _isRecordingSandJump = MutableStateFlow(false)
    val isRecordingSandJump: StateFlow<Boolean> = _isRecordingSandJump.asStateFlow()

    private val _sandJumpElapsedMs = MutableStateFlow(0L)
    val sandJumpElapsedMs: StateFlow<Long> = _sandJumpElapsedMs.asStateFlow()

    private var sandJumpTimerJob: Job? = null
    private var sandJumpStartTimeMs: Long = 0L

    private val _sandJumpHistory = MutableStateFlow<List<SandJumpAiAnalysis>>(emptyList())
    val sandJumpHistory: StateFlow<List<SandJumpAiAnalysis>> = _sandJumpHistory.asStateFlow()

    private val _latestSandJumpResult = MutableStateFlow<SandJumpAiAnalysis?>(null)
    val latestSandJumpResult: StateFlow<SandJumpAiAnalysis?> = _latestSandJumpResult.asStateFlow()

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

    private val _selectedAthleteDetail = MutableStateFlow<AthleteEntity?>(null)
    val selectedAthleteDetail: StateFlow<AthleteEntity?> = _selectedAthleteDetail.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncResult?>(null)
    val syncStatus: StateFlow<SyncResult?> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _selectedVideoFolderPath = MutableStateFlow(storageManager.getSelectedVideoFolderPath())
    val selectedVideoFolderPath: StateFlow<String> = _selectedVideoFolderPath.asStateFlow()

    // AI Posture Reader & Direct Analytics Table Slit Export State
    private val _selectedRaceForPostureAi = MutableStateFlow<RaceRecordEntity?>(null)
    val selectedRaceForPostureAi: StateFlow<RaceRecordEntity?> = _selectedRaceForPostureAi.asStateFlow()

    private val _lastExportedSlitPath = MutableStateFlow<String?>(null)
    val lastExportedSlitPath: StateFlow<String?> = _lastExportedSlitPath.asStateFlow()

    // Firebase AuthStateListener
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            val email = (user.email ?: _currentUserEmail.value).ifBlank { "coach@psycotimexpro.my" }
            val isSuper = email.equals(superAdminEmail, ignoreCase = true)
            val name = user.displayName ?: _currentUserName.value.ifBlank {
                if (isSuper) "Coach Salipar Jipun (Superadmin)" else "Jurulatih (${email.substringBefore("@")})"
            }
            _currentUserEmail.value = email
            _currentUserName.value = name
            _isSuperAdmin.value = isSuper
            _isLoggedIn.value = true
        } else {
            _isLoggedIn.value = false
        }
    }

    fun updateSelectedVideoFolder(path: String): FolderValidationResult {
        val result = storageManager.saveSelectedVideoFolder(path)
        if (result.isValid) {
            _selectedVideoFolderPath.value = result.resolvedPath
        }
        return result
    }

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
            races.filter { it.coachEmail.equals(email, ignoreCase = true) || it.createdByEmail.equals(email, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
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
        try {
            Firebase.auth.addAuthStateListener(authStateListener)
        } catch (e: Exception) {
            Log.w("MainViewModel", "Firebase Auth listener registration deferred: ${e.message}")
        }
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    fun login(
        email: String,
        name: String,
        club: String,
        country: String = "Malaysia",
        phone: String = "",
        cert: String = "",
        specialization: String = "",
        isSuper: Boolean
    ) {
        _currentUserEmail.value = email
        _currentUserName.value = name
        _isSuperAdmin.value = isSuper
        _clubProfile.value = _clubProfile.value.copy(
            clubName = if (club.isNotBlank()) club else _clubProfile.value.clubName,
            coachName = name,
            coachEmail = email,
            coachPhone = if (phone.isNotBlank()) phone else _clubProfile.value.coachPhone,
            country = if (country.isNotBlank()) country else _clubProfile.value.country,
            coachCertifications = if (cert.isNotBlank()) cert else _clubProfile.value.coachCertifications,
            specializationEvent = if (specialization.isNotBlank()) specialization else _clubProfile.value.specializationEvent
        )
        _isLoggedIn.value = true

        checkRemoteAppStatus()

        if (Firebase.auth.currentUser != null) {
            syncAllToFirestoreCloud()
        }
    }

    fun checkRemoteAppStatus() {
        viewModelScope.launch {
            val profile = _clubProfile.value
            val status = remoteAppControlManager.checkAppStatus(
                email = _currentUserEmail.value,
                name = _currentUserName.value,
                club = profile.clubName,
                country = profile.country,
                phone = profile.coachPhone,
                cert = profile.coachCertifications
            )
            _remoteAppStatus.value = status
        }
    }

    fun toggleMasterKillSwitch(active: Boolean, message: String = "", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = remoteAppControlManager.toggleKillSwitch(superAdminEmail, active, message)
            if (result.first) {
                checkRemoteAppStatus()
            }
            onResult(result.first, result.second)
        }
    }

    fun approveUserRemotely(targetEmail: String, days: Int = 30, plan: String = "plan_rm30", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = remoteAppControlManager.approveUser(superAdminEmail, targetEmail, days, plan)
            onResult(result.first, result.second)
        }
    }

    fun blockUserRemotely(targetEmail: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = remoteAppControlManager.blockUser(superAdminEmail, targetEmail)
            onResult(result.first, result.second)
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(getApplication())
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("MainViewModel", "Failed to clear credential state", e)
            }
            try {
                Firebase.auth.signOut()
            } catch (e: Exception) {
                Log.w("MainViewModel", "Firebase signOut error", e)
            }
            _isLoggedIn.value = false
            _currentUserEmail.value = ""
            _currentUserName.value = ""
            _isSuperAdmin.value = false
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

    /**
     * Immediately starts the sprint timer, starts live Portrait race video recording, AND fires
     * the explosive Electronic Starter Gun sound simultaneously.
     * Strictly idempotent: if already running, calling startTimer() again will NOT reset or stop it.
     */
    fun startTimer(fireGunSound: Boolean = true) {
        if (_isTimerRunning.value) return
        if (fireGunSound) {
            ElectronicStarterGunAudio.playExplosiveStarterGunShot(viewModelScope)
        }
        CameraFrameHolder.startLiveRecordingSession(
            enable100mFocusTrack = (_camMode.value == 1 || _camMode.value == 3)
        )
        _isRecordingVideo.value = true
        _isTimerRunning.value = true
        timerStartTime = System.currentTimeMillis() - _elapsedMillis.value
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (_isTimerRunning.value) {
                _elapsedMillis.value = System.currentTimeMillis() - timerStartTime
                delay(10) // 10ms high precision tick
            }
        }
    }

    /**
     * Handles incoming hardware KeyEvent from a connected BLE-M3 Remote or Bluetooth Camera Selfie Trigger.
     * STRICT RULES:
     * 1. Consumes BOTH ACTION_DOWN and ACTION_UP (returns true) for all remote keys so Android's
     *    FocusManager NEVER moves the focus cursor to other bottom navigation tabs!
     * 2. Trigger is strictly locked to the Timing ET tab (_isTimingTabActive == true) and START action only!
     */
    fun handleBluetoothRemoteKeyEvent(event: KeyEvent): Boolean {
        if (!bluetoothTriggerManager.isRemoteKeyToConsume(event)) return false

        val shouldFire = bluetoothTriggerManager.isSupportedTriggerKey(event)
        if (shouldFire) {
            val signal = bluetoothTriggerManager.extractSignal(event)
            _lastRemoteKeySignal.value = signal

            if (_showBluetoothRemoteSetupDialog.value) {
                ElectronicStarterGunAudio.playExplosiveStarterGunShot(viewModelScope)
                return true
            }

            if (_isBluetoothTriggerEnabled.value && _isTimingTabActive.value) {
                if (!_isTimerRunning.value) {
                    startTimer(fireGunSound = true)
                }
            }
        }
        // Always consume the remote key event (including ACTION_UP & D-Pad) so cursor NEVER jumps to other tabs!
        return _isBluetoothTriggerEnabled.value || _showBluetoothRemoteSetupDialog.value
    }

    /**
     * Handles incoming external Bluetooth HID Pointer / Touch-Click / Cursor Move events from BLE-M3 remotes.
     * Consumes all external BLE-M3 cursor movement & click events so the cursor stays locked in Timing ET
     * and never clicks on other tabs!
     */
    fun handleBluetoothRemoteMotionEvent(event: MotionEvent): Boolean {
        if (!bluetoothTriggerManager.isExternalBleHidMotionEvent(event)) return false

        if (bluetoothTriggerManager.isBleM3ExternalPointerTrigger(event)) {
            val signal = bluetoothTriggerManager.extractPointerSignal(event)
            _lastRemoteKeySignal.value = signal

            if (_showBluetoothRemoteSetupDialog.value) {
                ElectronicStarterGunAudio.playExplosiveStarterGunShot(viewModelScope)
                return true
            }

            if (_isBluetoothTriggerEnabled.value && _isTimingTabActive.value && !_isTimerRunning.value) {
                startTimer(fireGunSound = true)
            }
        }
        // Always consume external BLE-M3 cursor motion/click so it never moves to or clicks other tabs!
        return _isBluetoothTriggerEnabled.value || _showBluetoothRemoteSetupDialog.value
    }

    fun toggleBluetoothTrigger(enabled: Boolean) {
        _isBluetoothTriggerEnabled.value = enabled
    }

    fun setShowBluetoothRemoteSetupDialog(show: Boolean) {
        _showBluetoothRemoteSetupDialog.value = show
    }

    fun openPostureReaderForRace(race: RaceRecordEntity?) {
        _selectedRaceForPostureAi.value = race
        if (race == null) {
            _lastExportedSlitPath.value = null
        }
    }

    fun exportSlitImageFromAnalyticsTable(race: RaceRecordEntity, onExported: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val posture = AiSportsScienceAnalyzer.analyzePostureForRace(race)
            val liveBmp = CameraFrameHolder.latestFinishFrameBitmap
                ?: CameraFrameHolder.latestLiveFrameBitmap
            val exportedPath = withContext(Dispatchers.IO) {
                storageManager.exportPhotoFinishSlitForRace(
                    race = race,
                    posture = posture,
                    realCameraFrame = liveBmp
                )
            }
            _lastExportedSlitPath.value = exportedPath
            _selectedRaceForPostureAi.value = race
            onExported?.invoke(exportedPath)
        }
    }

    // SAND JUMP AI (LOMPAT JAUH / TRIPLE JUMP / SUKAN PASIR) RECORDING & EVIDENCE ENGINE
    fun startSandJumpRecording() {
        if (_isRecordingSandJump.value) return
        CameraFrameHolder.startLiveRecordingSession(enable100mFocusTrack = false)
        _isRecordingSandJump.value = true
        _sandJumpElapsedMs.value = 0L
        sandJumpStartTimeMs = System.currentTimeMillis()
        sandJumpTimerJob?.cancel()
        sandJumpTimerJob = viewModelScope.launch(Dispatchers.Default) {
            while (_isRecordingSandJump.value) {
                _sandJumpElapsedMs.value = System.currentTimeMillis() - sandJumpStartTimeMs
                delay(30)
            }
        }
    }

    fun saveSandJumpAndAnalyze(
        jumpTitle: String,
        athleteName: String,
        athleteId: Long?,
        eventCategory: String,
        configuredMaxTapeMeters: Double,
        landingProgressRatio: Float,
        toeToBoardCm: Double,
        motionEnergy: Float,
        tapeStartX: Float,
        tapeStartY: Float,
        tapeEndX: Float,
        tapeEndY: Float,
        landingX: Float,
        landingY: Float
    ) {
        val durationMs = _sandJumpElapsedMs.value
        _isRecordingSandJump.value = false
        CameraFrameHolder.stopLiveRecordingSession()
        sandJumpTimerJob?.cancel()

        viewModelScope.launch {
            val rawAnalysis = AiSportsScienceAnalyzer.evaluateSandJumpPerformance(
                jumpTitle = jumpTitle,
                athleteName = athleteName,
                eventCategory = eventCategory,
                configuredMaxTapeMeters = configuredMaxTapeMeters,
                landingProgressRatio = landingProgressRatio,
                toeToBoardCm = toeToBoardCm,
                motionEnergy = motionEnergy,
                recordedDurationMs = durationMs
            )

            val liveBmp = CameraFrameHolder.latestFinishFrameBitmap ?: CameraFrameHolder.latestLiveFrameBitmap
            val (evidencePath, framesDirPath) = withContext(Dispatchers.IO) {
                val evPath = storageManager.saveSandJumpEvidenceImage(
                    jumpTitle = jumpTitle,
                    athleteName = athleteName,
                    eventCategory = eventCategory,
                    measuredDistanceMeters = rawAnalysis.measuredDistanceMeters,
                    boardStatusValid = rawAnalysis.isBoardValid,
                    toeToBoardCm = rawAnalysis.toeToBoardCm,
                    takeoffAngleDeg = rawAnalysis.takeoffAngleDeg,
                    approachSpeedMs = rawAnalysis.approachSpeedMs,
                    tapeStartRatioX = tapeStartX,
                    tapeStartRatioY = tapeStartY,
                    tapeEndRatioX = tapeEndX,
                    tapeEndRatioY = tapeEndY,
                    landingRatioX = landingX,
                    landingRatioY = landingY,
                    configuredMaxMeters = configuredMaxTapeMeters,
                    realCameraFrame = liveBmp
                )
                val (cam1Uri, _) = storageManager.createRaceVideoPackage(
                    raceTitle = jumpTitle,
                    recordedTimeFormatted = String.format(Locale.US, "%.2f m", rawAnalysis.measuredDistanceMeters),
                    athleteName = athleteName
                )
                val fDir = try {
                    File(cam1Uri).readLines()
                        .firstOrNull { it.startsWith("FramesDir:") }
                        ?.substringAfter("FramesDir:")
                        ?.trim() ?: ""
                } catch (_: Exception) {
                    ""
                }
                evPath to fDir
            }

            val completedAnalysis = rawAnalysis.copy(
                evidenceImagePath = evidencePath,
                videoFramesDirPath = framesDirPath,
                recordedFramesCount = CameraFrameHolder.recordedSessionFrames.size.coerceAtLeast(1)
            )

            _latestSandJumpResult.value = completedAnalysis
            _sandJumpHistory.value = listOf(completedAnalysis) + _sandJumpHistory.value

            if (athleteId != null) {
                val metric = PerformanceMetrics(
                    athleteId = athleteId,
                    athleteName = athleteName,
                    eventName = eventCategory,
                    pbValue = String.format(Locale.US, "%.2f", completedAnalysis.measuredDistanceMeters),
                    pbUnit = "Meter (m)",
                    isNewPb = completedAnalysis.isBoardValid,
                    measuredDistanceOrHeight = completedAnalysis.measuredDistanceMeters,
                    torsoLeanAngleDeg = completedAnalysis.takeoffAngleDeg,
                    notes = completedAnalysis.aiScienceVerdict
                )
                repository.insertMetrics(metric)
            }
        }
    }

    fun selectSandJumpResult(result: SandJumpAiAnalysis?) {
        _latestSandJumpResult.value = result
    }

    fun stopTimer() {
        _isTimerRunning.value = false
        _isRecordingVideo.value = false
        CameraFrameHolder.stopLiveRecordingSession()
        timerJob?.cancel()

        viewModelScope.launch {
            val count = repository.getRaceCount()
            _suggestedRaceTitle.value = "Race ${count + 1}"
            _showSaveRaceDialog.value = true
        }
    }

    fun resetTimer() {
        _isTimerRunning.value = false
        _isRecordingVideo.value = false
        CameraFrameHolder.stopLiveRecordingSession()
        timerJob?.cancel()
        _elapsedMillis.value = 0L
    }

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
        notes: String,
        targetFolder: java.io.File? = null
    ) {
        val timeMs = _elapsedMillis.value
        viewModelScope.launch {
            val (cam1Uri, cam2Uri) = withContext(Dispatchers.IO) {
                storageManager.createRaceVideoPackage(
                    raceTitle = title,
                    recordedTimeFormatted = String.format(Locale.US, "%.2f s", timeMs / 1000.0),
                    athleteName = athleteName,
                    targetFolder = targetFolder
                )
            }

            val liveBmp = CameraFrameHolder.latestFinishFrameBitmap ?: CameraFrameHolder.latestLiveFrameBitmap
            val photoFinishUri = withContext(Dispatchers.IO) {
                storageManager.savePhotoFinishImage(
                    raceTitle = title,
                    athleteName = athleteName,
                    timeFormatted = String.format(Locale.US, "%.2f s", timeMs / 1000.0),
                    isCam2Torso = true,
                    realCameraFrame = liveBmp,
                    targetFolder = targetFolder
                )
            }

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
                cadenceSpM = analysis?.cadenceSpM?.takeIf { it > 0 } ?: 254,
                strideFreqHz = analysis?.strideFrequencyHz?.takeIf { it > 0.0 } ?: 4.25,
                groundContactTimeMs = analysis?.groundContactTimeMs?.takeIf { it > 0 } ?: 110,
                torsoLeanAngleDeg = analysis?.torsoLeanAngleDeg?.takeIf { it > 1.0 } ?: 14.2,
                notes = notes
            )

            repository.insertRace(newRace)

            if (Firebase.auth.currentUser != null) {
                firestoreRepository.syncLocalRaceToCloud(newRace)
            }

            if (athleteId != null) {
                val metric = PerformanceMetrics(
                    athleteId = athleteId,
                    athleteName = athleteName,
                    eventName = eventName,
                    pbValue = String.format(Locale.US, "%.2f", timeMs / 1000.0),
                    pbUnit = "Saat (s)",
                    isNewPb = true,
                    measuredTimeMillis = timeMs,
                    cadenceSpM = analysis?.cadenceSpM?.takeIf { it > 0 } ?: 254,
                    strideFrequencyHz = analysis?.strideFrequencyHz?.takeIf { it > 0.0 } ?: 4.25,
                    groundContactTimeMs = analysis?.groundContactTimeMs?.takeIf { it > 0 } ?: 110,
                    torsoLeanAngleDeg = analysis?.torsoLeanAngleDeg?.takeIf { it > 1.0 } ?: 14.2,
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
            if (Firebase.auth.currentUser != null) {
                firestoreRepository.syncLocalAthleteToCloud(athlete.copy(id = id))
            }
            _showAddAthleteDialog.value = false
            syncWithWeb()
        }
    }

    fun updateAthlete(athlete: AthleteEntity) {
        viewModelScope.launch {
            athleteRepository.updateAthlete(athlete)
            if (Firebase.auth.currentUser != null) {
                firestoreRepository.syncLocalAthleteToCloud(athlete)
            }
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

    fun updateFee(fee: FeeRecordEntity) {
        viewModelScope.launch {
            repository.updateFee(fee)
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

    fun deleteRace(race: RaceRecordEntity) {
        viewModelScope.launch {
            repository.deleteRace(race)
            race.cam1VideoUri?.let { uri ->
                try { java.io.File(uri).delete() } catch (_: Exception) {}
            }
            race.cam2VideoUri?.let { uri ->
                try { java.io.File(uri).delete() } catch (_: Exception) {}
            }
            race.photoFinishUri?.let { uri ->
                try { java.io.File(uri).delete() } catch (_: Exception) {}
            }
        }
    }

    fun capturePhotoFinish(race: RaceRecordEntity, currentFrameMs: Long, isCam2: Boolean) {
        viewModelScope.launch {
            val liveBmp = CameraFrameHolder.latestFinishFrameBitmap ?: CameraFrameHolder.latestLiveFrameBitmap
            withContext(Dispatchers.IO) {
                storageManager.savePhotoFinishImage(
                    raceTitle = race.title,
                    athleteName = race.athleteName,
                    timeFormatted = String.format(Locale.US, "%.2f s", currentFrameMs / 1000.0),
                    isCam2Torso = isCam2,
                    realCameraFrame = liveBmp
                )
            }
        }
    }

    fun downloadVideoToPhone(race: RaceRecordEntity, isCam2: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            storageManager.createRaceVideoPackage(
                raceTitle = race.title,
                recordedTimeFormatted = race.formattedTime,
                athleteName = race.athleteName
            )
        }
    }

    private fun syncAllToFirestoreCloud() {
        viewModelScope.launch {
            if (Firebase.auth.currentUser == null) return@launch
            allAthletes.value.forEach { athlete ->
                firestoreRepository.syncLocalAthleteToCloud(athlete)
            }
            allRaces.value.forEach { race ->
                firestoreRepository.syncLocalRaceToCloud(race)
            }
        }
    }

    fun syncWithWeb() {
        viewModelScope.launch {
            _isSyncing.value = true
            val athletesList = allAthletes.value
            val racesList = allRaces.value
            if (Firebase.auth.currentUser != null) {
                athletesList.forEach { firestoreRepository.syncLocalAthleteToCloud(it) }
                racesList.forEach { firestoreRepository.syncLocalRaceToCloud(it) }
            }
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
        try {
            Firebase.auth.removeAuthStateListener(authStateListener)
        } catch (_: Exception) {}
        audioGunDetector?.stopListening()
        timerJob?.cancel()
        sandJumpTimerJob?.cancel()
    }
}
