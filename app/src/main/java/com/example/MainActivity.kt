package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AchievementEntity
import com.example.service.configureAppCheck
import com.example.ui.dialogs.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

enum class ScreenTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Utama", Icons.Default.Dashboard),
    TIMING("Timing ET", Icons.Default.Timer),
    SAND_JUMP_AI("Lompat AI", Icons.Default.Straighten),
    REPLAY("Replay", Icons.Default.SlowMotionVideo),
    ATHLETES("Atlit & PB", Icons.Default.DirectionsRun),
    REGISTER_ATHLETE("Daftar Atlit", Icons.Default.PersonAdd),
    FEES("Yuran", Icons.Default.ReceiptLong),
    SUBUSERS("Pegawai", Icons.Default.Group),
    SPORTS_SCIENCE_AI("Sains AI", Icons.Default.Science),
    WEB_PORTAL("Web Portal", Icons.Default.Public),
    SETTINGS("Tetapan", Icons.Default.Settings),
    VIDEO_STORAGE_SETTINGS("Storan Video", Icons.Default.FolderSpecial)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (viewModel.handleBluetoothRemoteKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        if (viewModel.handleBluetoothRemoteMotionEvent(ev)) {
            return true
        }
        return super.dispatchGenericMotionEvent(ev)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (viewModel.handleBluetoothRemoteMotionEvent(ev)) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
                var showSplash by remember { mutableStateOf(true) }
                var currentTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }
                var showLogoutConfirmDialog by remember { mutableStateOf(false) }

                LaunchedEffect(currentTab, isLoggedIn, showSplash) {
                    viewModel.setTimingTabActive(!showSplash && isLoggedIn && currentTab == ScreenTab.TIMING)
                }

                if (showSplash) {
                    SplashScreen(
                        onDismissSplash = { showSplash = false }
                    )
                } else if (!isLoggedIn) {
                    AuthScreen(
                        onLoginSuccess = { email, name, club, isSuper ->
                            viewModel.login(email, name, club, isSuper)
                            currentTab = ScreenTab.DASHBOARD
                            Toast.makeText(this@MainActivity, "Selamat kembali, $name!", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    val clubProfile by viewModel.clubProfile.collectAsStateWithLifecycle()
                    val athletes by viewModel.allAthletes.collectAsStateWithLifecycle()
                    val races by viewModel.allRaces.collectAsStateWithLifecycle()
                    val subUsers by viewModel.subUsers.collectAsStateWithLifecycle()
                    val fees by viewModel.allFees.collectAsStateWithLifecycle()
                    val achievements by viewModel.allAchievements.collectAsStateWithLifecycle()
                    val performanceMetrics by viewModel.allPerformanceMetrics.collectAsStateWithLifecycle()

                    val currentUserEmail by viewModel.currentUserEmail.collectAsStateWithLifecycle()
                    val currentUserName by viewModel.currentUserName.collectAsStateWithLifecycle()
                    val isSuperAdmin by viewModel.isSuperAdmin.collectAsStateWithLifecycle()

                    val isTimerRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()
                    val isRecordingVideo by viewModel.isRecordingVideo.collectAsStateWithLifecycle()
                    val elapsedMillis by viewModel.elapsedMillis.collectAsStateWithLifecycle()
                    val camMode by viewModel.camMode.collectAsStateWithLifecycle()
                    val isAudioGunEnabled by viewModel.isAudioGunEnabled.collectAsStateWithLifecycle()
                    val isMovementDetected by viewModel.isMovementDetected.collectAsStateWithLifecycle()
                    val motionEnergy by viewModel.motionEnergy.collectAsStateWithLifecycle()
                    val latestAiAnalysis by viewModel.latestAiAnalysis.collectAsStateWithLifecycle()

                    val isRecordingSandJump by viewModel.isRecordingSandJump.collectAsStateWithLifecycle()
                    val sandJumpElapsedMs by viewModel.sandJumpElapsedMs.collectAsStateWithLifecycle()
                    val sandJumpHistory by viewModel.sandJumpHistory.collectAsStateWithLifecycle()
                    val latestSandJumpResult by viewModel.latestSandJumpResult.collectAsStateWithLifecycle()

                    val showSaveRaceDialog by viewModel.showSaveRaceDialog.collectAsStateWithLifecycle()
                    val suggestedRaceTitle by viewModel.suggestedRaceTitle.collectAsStateWithLifecycle()
                    val showAddAthleteDialog by viewModel.showAddAthleteDialog.collectAsStateWithLifecycle()
                    val showAddSubUserDialog by viewModel.showAddSubUserDialog.collectAsStateWithLifecycle()
                    val showQrPairDialog by viewModel.showQrPairDialog.collectAsStateWithLifecycle()
                    val showWebSyncDialog by viewModel.showWebSyncDialog.collectAsStateWithLifecycle()
                    val selectedAthleteDetail by viewModel.selectedAthleteDetail.collectAsStateWithLifecycle()
                    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
                    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
                    val selectedVideoFolderPath by viewModel.selectedVideoFolderPath.collectAsStateWithLifecycle()
                    val isBluetoothTriggerEnabled by viewModel.isBluetoothTriggerEnabled.collectAsStateWithLifecycle()
                    val showBluetoothRemoteSetupDialog by viewModel.showBluetoothRemoteSetupDialog.collectAsStateWithLifecycle()
                    val lastRemoteKeySignal by viewModel.lastRemoteKeySignal.collectAsStateWithLifecycle()
                    val selectedRaceForPostureAi by viewModel.selectedRaceForPostureAi.collectAsStateWithLifecycle()
                    val lastExportedSlitPath by viewModel.lastExportedSlitPath.collectAsStateWithLifecycle()

                    BackHandler(enabled = currentTab != ScreenTab.DASHBOARD) {
                        currentTab = if (currentTab == ScreenTab.VIDEO_STORAGE_SETTINGS) {
                            ScreenTab.SETTINGS
                        } else {
                            ScreenTab.DASHBOARD
                        }
                    }

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            text = "Psyco Time X Pro",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (isSuperAdmin) "Superadmin: $currentUserName" else "Coach: $currentUserName",
                                            fontSize = 9.sp,
                                            color = if (isSuperAdmin) FinishLineRed else SilverMetallic
                                        )
                                    }
                                },
                                actions = {
                                    // COMPACT PRO BLE-M3 / BLUETOOTH REMOTE TRIGGER BUTTON NEXT TO TOP MENU
                                    Surface(
                                        color = if (isBluetoothTriggerEnabled) ElectricCyan.copy(alpha = 0.16f) else StadiumSurfaceVariant,
                                        shape = RoundedCornerShape(16.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isBluetoothTriggerEnabled) ElectricCyan.copy(alpha = 0.65f) else StadiumBorder
                                        ),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .focusProperties { canFocus = false }
                                            .clickable { viewModel.setShowBluetoothRemoteSetupDialog(true) }
                                            .testTag("top_bar_bt_trigger_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(
                                                        if (isBluetoothTriggerEnabled) SuccessGreen else TextMuted,
                                                        CircleShape
                                                    )
                                            )
                                            Icon(
                                                Icons.Default.SettingsRemote,
                                                contentDescription = "BLE-M3 Remote Starter",
                                                tint = if (isBluetoothTriggerEnabled) ElectricCyan else SilverMetallic,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = if (currentTab == ScreenTab.TIMING) "BLE-M3 LOCKED" else "BLE-M3",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isBluetoothTriggerEnabled) Color.White else SilverMetallic
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { currentTab = ScreenTab.SUBUSERS },
                                        modifier = Modifier.focusProperties { canFocus = false }
                                    ) {
                                        Icon(
                                            Icons.Default.Group,
                                            contentDescription = "Pegawai",
                                            tint = if (currentTab == ScreenTab.SUBUSERS) FinishLineRed else Color.White
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.setShowWebSyncDialog(true) },
                                        modifier = Modifier.focusProperties { canFocus = false }
                                    ) {
                                        Icon(
                                            Icons.Default.CloudSync,
                                            contentDescription = "Sync Web",
                                            tint = Color.White
                                        )
                                    }
                                    IconButton(
                                        onClick = { currentTab = ScreenTab.SETTINGS },
                                        modifier = Modifier.focusProperties { canFocus = false }
                                    ) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = "Tetapan",
                                            tint = if (currentTab == ScreenTab.SETTINGS) FinishLineRed else Color.White
                                        )
                                    }
                                    IconButton(
                                        onClick = { showLogoutConfirmDialog = true },
                                        modifier = Modifier
                                            .focusProperties { canFocus = false }
                                            .testTag("top_bar_logout_button")
                                    ) {
                                        Icon(
                                            Icons.Default.ExitToApp,
                                            contentDescription = "Log Keluar",
                                            tint = FinishLineRed
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = StadiumSurface
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = StadiumSurface,
                                modifier = Modifier
                                    .focusProperties { canFocus = false }
                                    .testTag("bottom_navigation_bar")
                            ) {
                                val tabs = listOf(
                                    ScreenTab.DASHBOARD,
                                    ScreenTab.TIMING,
                                    ScreenTab.SAND_JUMP_AI,
                                    ScreenTab.REPLAY,
                                    ScreenTab.ATHLETES,
                                    ScreenTab.FEES,
                                    ScreenTab.SPORTS_SCIENCE_AI
                                )
                                tabs.forEach { tab ->
                                    val selected = currentTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { currentTab = tab },
                                        modifier = Modifier.focusProperties { canFocus = false },
                                        icon = {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.title,
                                                tint = if (selected) FinishLineRed else SilverBorder
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.title,
                                                fontSize = 8.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selected) FinishLineRed else SilverBorder,
                                                maxLines = 1
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = FinishLineRed.copy(alpha = 0.2f)
                                        )
                                    )
                                }
                            }
                        },
                        containerColor = TrackDarkNavy
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            when (currentTab) {
                                ScreenTab.DASHBOARD -> DashboardScreen(
                                    clubProfile = clubProfile,
                                    athletes = athletes,
                                    races = races,
                                    currentUserEmail = currentUserEmail,
                                    isSuperAdmin = isSuperAdmin,
                                    onNavigateToTiming = { currentTab = ScreenTab.TIMING },
                                    onNavigateToAthletes = { currentTab = ScreenTab.ATHLETES },
                                    onNavigateToReplays = { currentTab = ScreenTab.REPLAY },
                                    onOpenAddAthlete = { currentTab = ScreenTab.REGISTER_ATHLETE },
                                    onOpenWebsite = {
                                        try {
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.psycotimexpro.my"))
                                            startActivity(browserIntent)
                                        } catch (_: Exception) {
                                            Toast.makeText(this@MainActivity, "Membuka www.psycotimexpro.my", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onOpenWebSync = { viewModel.setShowWebSyncDialog(true) },
                                    onLogout = { showLogoutConfirmDialog = true },
                                    onSelectAthlete = { viewModel.selectAthlete(it) },
                                    onUpdateClubLogo = { uri ->
                                        viewModel.updateClubInfo(
                                            clubProfile.clubName,
                                            uri,
                                            clubProfile.coachName,
                                            clubProfile.coachPhone,
                                            clubProfile.coachPhotoUri
                                        )
                                    }
                                )

                                ScreenTab.TIMING -> TimingSystemScreen(
                                    camMode = camMode,
                                    isTimerRunning = isTimerRunning,
                                    elapsedMillis = elapsedMillis,
                                    isAudioGunEnabled = isAudioGunEnabled,
                                    isMovementDetected = isMovementDetected,
                                    motionEnergy = motionEnergy,
                                    latestAiAnalysis = latestAiAnalysis,
                                    onSetCamMode = { viewModel.setCamMode(it) },
                                    onToggleAudioGun = { viewModel.toggleAudioGun(it) },
                                    onStartTimer = { viewModel.startTimer(fireGunSound = true) },
                                    onStopTimer = { viewModel.stopTimer() },
                                    onResetTimer = { viewModel.resetTimer() },
                                    onTriggerMotion = { hasMotion, energy ->
                                        viewModel.setMotionDetection(hasMotion, energy)
                                    },
                                    onOpenQrPair = { viewModel.setShowQrPairDialog(true) },
                                    isBluetoothTriggerEnabled = isBluetoothTriggerEnabled,
                                    isRecordingVideo = isRecordingVideo,
                                    lastRemoteSignal = lastRemoteKeySignal,
                                    onOpenBluetoothRemoteSetup = { viewModel.setShowBluetoothRemoteSetupDialog(true) }
                                )

                                ScreenTab.SAND_JUMP_AI -> SandJumpAiScreen(
                                    athletes = athletes,
                                    sandJumpHistory = sandJumpHistory,
                                    isRecordingJump = isRecordingSandJump,
                                    recordingElapsedMs = sandJumpElapsedMs,
                                    latestJumpResult = latestSandJumpResult,
                                    onStartJumpRecording = {
                                        viewModel.startSandJumpRecording()
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Rakaman Lompatan AI bermula! Atlit boleh memulakan lari landas.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onSaveJumpAndAnalyze = { title, athName, athId, cat, maxM, ratio, toeCm, energy, sx, sy, ex, ey, lx, ly ->
                                        viewModel.saveSandJumpAndAnalyze(
                                            jumpTitle = title,
                                            athleteName = athName,
                                            athleteId = athId,
                                            eventCategory = cat,
                                            configuredMaxTapeMeters = maxM,
                                            landingProgressRatio = ratio,
                                            toeToBoardCm = toeCm,
                                            motionEnergy = energy,
                                            tapeStartX = sx,
                                            tapeStartY = sy,
                                            tapeEndX = ex,
                                            tapeEndY = ey,
                                            landingX = lx,
                                            landingY = ly
                                        )
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Bukti Photo-Finish Pasir & Analisis Sains Sukan AI telah dijana!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    },
                                    onSelectJumpResult = { viewModel.selectSandJumpResult(it) }
                                )

                                ScreenTab.REPLAY -> ReplayScreen(
                                    races = races,
                                    currentUserEmail = currentUserEmail,
                                    isSuperAdmin = isSuperAdmin,
                                    onCapturePhotoFinish = { race, ms, isCam2 ->
                                        viewModel.capturePhotoFinish(race, ms, isCam2)
                                    },
                                    onDownloadVideo = { race, isCam2 ->
                                        viewModel.downloadVideoToPhone(race, isCam2)
                                    },
                                    onDeleteRace = { race ->
                                        viewModel.deleteRace(race)
                                        Toast.makeText(this@MainActivity, "Rekod race '${race.title}' berjaya dipadam.", Toast.LENGTH_SHORT).show()
                                    },
                                    onExportSlitFromTable = { race ->
                                        viewModel.exportSlitImageFromAnalyticsTable(race) { savedPath ->
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Imej Slit Photo-Finish dieksport ke: $savedPath",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    },
                                    onOpenPostureAiForRace = { race ->
                                        viewModel.openPostureReaderForRace(race)
                                    }
                                )

                                ScreenTab.ATHLETES -> AthleteListScreen(
                                    athletes = athletes,
                                    onOpenAddAthlete = { currentTab = ScreenTab.REGISTER_ATHLETE },
                                    onSelectAthlete = { viewModel.selectAthlete(it) },
                                    onDeleteAthlete = {
                                        viewModel.deleteAthlete(it)
                                        Toast.makeText(this@MainActivity, "Atlit '${it.name}' dipadam.", Toast.LENGTH_SHORT).show()
                                    }
                                )

                                ScreenTab.REGISTER_ATHLETE -> RegisterAthleteScreen(
                                    onSaveAthlete = { newAthlete ->
                                        viewModel.addAthlete(newAthlete)
                                        currentTab = ScreenTab.ATHLETES
                                        Toast.makeText(this@MainActivity, "Atlit '${newAthlete.name}' berjaya didaftarkan & diselaraskan ke web!", Toast.LENGTH_SHORT).show()
                                    },
                                    onNavigateBack = { currentTab = ScreenTab.DASHBOARD }
                                )

                                ScreenTab.FEES -> FeeManagementScreen(
                                    fees = fees,
                                    athletes = athletes,
                                    onAddFee = { fee -> viewModel.addFee(fee) },
                                    onUpdateFee = { fee -> viewModel.updateFee(fee) }
                                )

                                ScreenTab.SUBUSERS -> SubUserScreen(
                                    subUsers = subUsers,
                                    currentUserEmail = currentUserEmail,
                                    isSuperAdmin = isSuperAdmin,
                                    onOpenAddSubUser = { viewModel.setShowAddSubUserDialog(true) },
                                    onDeleteSubUser = {
                                        viewModel.deleteSubUser(it)
                                    }
                                )

                                ScreenTab.SPORTS_SCIENCE_AI -> SportsScienceAiHubScreen(
                                    athletes = athletes,
                                    races = races,
                                    sandJumpHistory = sandJumpHistory,
                                    coachName = currentUserName,
                                    onApplyDrillToAllAthletes = { drill, _ ->
                                        athletes.forEach { athlete ->
                                            viewModel.addAchievement(
                                                AchievementEntity(
                                                    athleteId = athlete.id,
                                                    athleteName = athlete.name,
                                                    tournamentName = "Program Latihan Rumah AI (${drill.sport.shortLabel})",
                                                    eventName = drill.title,
                                                    medal = if (drill.type == SuggestionType.WARM_UP) "WARM-UP RAMP" else "TRAINING AI",
                                                    resultRecord = drill.setsAndReps,
                                                    yearOrDate = "2026"
                                                )
                                            )
                                        }
                                    }
                                )

                                ScreenTab.WEB_PORTAL -> WebPortalScreen(
                                    initialUrl = "https://www.psycotimexpro.my",
                                    onNavigateBack = { currentTab = ScreenTab.DASHBOARD }
                                )

                                ScreenTab.SETTINGS -> SettingsScreen(
                                    clubProfile = clubProfile,
                                    currentUserEmail = currentUserEmail,
                                    currentUserName = currentUserName,
                                    isSuperAdmin = isSuperAdmin,
                                    deviceStorageDirs = viewModel.storageManager.ensureDirectoriesExist(),
                                    selectedVideoFolderPath = selectedVideoFolderPath,
                                    onOpenVideoStorageSettings = { currentTab = ScreenTab.VIDEO_STORAGE_SETTINGS },
                                    bluetoothTriggerManager = viewModel.bluetoothTriggerManager,
                                    isBluetoothTriggerEnabled = isBluetoothTriggerEnabled,
                                    lastRemoteSignal = lastRemoteKeySignal,
                                    onToggleBluetoothTrigger = { viewModel.toggleBluetoothTrigger(it) },
                                    onOpenBluetoothRemoteSetup = { viewModel.setShowBluetoothRemoteSetupDialog(true) },
                                    onSwitchUser = { email, name -> viewModel.switchUser(email, name) },
                                    onUpdateClubInfo = { name, logo, coach, phone, photo ->
                                        viewModel.updateClubInfo(name, logo, coach, phone, photo)
                                    },
                                    onOpenWebSync = { viewModel.setShowWebSyncDialog(true) },
                                    onLogout = { showLogoutConfirmDialog = true }
                                )

                                ScreenTab.VIDEO_STORAGE_SETTINGS -> VideoStorageSettingsScreen(
                                    storageManager = viewModel.storageManager,
                                    selectedVideoFolderPath = selectedVideoFolderPath,
                                    onUpdateSelectedVideoFolder = { path ->
                                        viewModel.updateSelectedVideoFolder(path)
                                    },
                                    onNavigateBack = { currentTab = ScreenTab.SETTINGS }
                                )
                            }
                        }
                    }

                    // DIALOGS
                    if (showSaveRaceDialog) {
                        SaveRaceDialog(
                            suggestedTitle = suggestedRaceTitle,
                            elapsedMillis = elapsedMillis,
                            athletes = athletes,
                            availableFolders = viewModel.storageManager.getAvailableStorageFolders(),
                            onCreateFolder = { name -> viewModel.storageManager.createCustomFolder(name) },
                            onDismiss = { viewModel.dismissSaveRaceDialog() },
                            onSave = { title, event, athleteName, athleteId, lane, wind, notes, folder ->
                                viewModel.saveRace(title, event, athleteName, athleteId, lane, wind, notes, folder)
                                Toast.makeText(this@MainActivity, "Race '$title' disimpan ke folder '${folder?.name ?: "Videos"}'!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    if (showAddAthleteDialog) {
                        AddAthleteDialog(
                            onDismiss = { viewModel.setShowAddAthleteDialog(false) },
                            onSaveAthlete = { athlete ->
                                viewModel.addAthlete(athlete)
                                Toast.makeText(this@MainActivity, "Atlit '${athlete.name}' didaftarkan!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    if (showAddSubUserDialog) {
                        AddSubUserDialog(
                            currentSubUserCount = subUsers.size,
                            onDismiss = { viewModel.setShowAddSubUserDialog(false) },
                            onAddSubUser = { username, fullName, role, pin ->
                                viewModel.addSubUser(username, fullName, role, pin)
                                Toast.makeText(this@MainActivity, "Sub-user '$username' didaftarkan!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    if (showQrPairDialog) {
                        QrPairDialog(
                            sessionPayload = "PSYCO_CAM2_PAIR:${System.currentTimeMillis()}:GATE2",
                            onDismiss = { viewModel.setShowQrPairDialog(false) },
                            onSwitchToCam2Mode = { viewModel.setCamMode(2) }
                        )
                    }

                    if (showWebSyncDialog) {
                        WebSyncDialog(
                            currentEndpoint = viewModel.webSyncManager.getEndpoint(),
                            athletesCount = athletes.size,
                            racesCount = races.size,
                            lastSyncTimestamp = viewModel.webSyncManager.lastSyncTimestamp,
                            isSyncing = isSyncing,
                            syncResult = syncStatus,
                            onDismiss = { viewModel.setShowWebSyncDialog(false) },
                            onTriggerSync = { viewModel.syncWithWeb() }
                        )
                    }

                    if (showBluetoothRemoteSetupDialog) {
                        BluetoothRemoteSetupDialog(
                            triggerManager = viewModel.bluetoothTriggerManager,
                            isBluetoothTriggerEnabled = isBluetoothTriggerEnabled,
                            lastRemoteSignal = lastRemoteKeySignal,
                            onToggleBluetoothTrigger = { viewModel.toggleBluetoothTrigger(it) },
                            onTestFireStarterGun = {
                                Toast.makeText(this@MainActivity, "Bunyi Tembakan Starter Gun Diuji!", Toast.LENGTH_SHORT).show()
                            },
                            onDismiss = { viewModel.setShowBluetoothRemoteSetupDialog(false) }
                        )
                    }

                    selectedRaceForPostureAi?.let { selectedRace ->
                        AiPostureReaderDialog(
                            race = selectedRace,
                            exportedSlitPath = lastExportedSlitPath,
                            onExportSlitNow = { race ->
                                viewModel.exportSlitImageFromAnalyticsTable(race) { savedPath ->
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Imej Slit Photo-Finish disimpan: $savedPath",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            onDismiss = { viewModel.openPostureReaderForRace(null) }
                        )
                    }

                    selectedAthleteDetail?.let { athlete ->
                        AthleteDetailDialog(
                            athlete = athlete,
                            achievements = achievements,
                            performanceMetrics = performanceMetrics,
                            onDismiss = { viewModel.selectAthlete(null) },
                            onDeleteAthlete = {
                                viewModel.deleteAthlete(athlete)
                                Toast.makeText(this@MainActivity, "Atlit dipadam.", Toast.LENGTH_SHORT).show()
                            },
                            onAddAchievement = { tournament, event, medal, record ->
                                viewModel.addAchievement(
                                    AchievementEntity(
                                        athleteId = athlete.id,
                                        athleteName = athlete.name,
                                        tournamentName = tournament,
                                        eventName = event,
                                        medal = medal,
                                        resultRecord = record,
                                        yearOrDate = "2026"
                                    )
                                )
                            }
                        )
                    }

                    if (showLogoutConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showLogoutConfirmDialog = false },
                            title = {
                                Text(
                                    text = "Log Keluar Akaun?",
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            },
                            text = {
                                Text(
                                    text = "Adakah anda pasti mahu log keluar daripada akaun '$currentUserEmail'? Anda perlu log masuk semula untuk mengakses sistem.",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showLogoutConfirmDialog = false
                                        viewModel.logout()
                                        Toast.makeText(this@MainActivity, "Log keluar berjaya.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed)
                                ) {
                                    Text("Log Keluar", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                                    Text("Batal", color = SilverMetallic)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
