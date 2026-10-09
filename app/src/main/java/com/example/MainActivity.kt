package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AchievementEntity
import com.example.ui.dialogs.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import java.io.File

enum class ScreenTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Utama", Icons.Default.Dashboard),
    TIMING("Timing ET", Icons.Default.Timer),
    REPLAY("Replay", Icons.Default.SlowMotionVideo),
    ATHLETES("Atlit & PB", Icons.Default.DirectionsRun),
    REGISTER_ATHLETE("Daftar Atlit", Icons.Default.PersonAdd),
    FEES("Yuran", Icons.Default.ReceiptLong),
    SUBUSERS("Pegawai", Icons.Default.Group),
    WEB_PORTAL("Web Portal", Icons.Default.Public),
    SETTINGS("Tetapan", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var showSplash by remember { mutableStateOf(true) }
                var currentTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }

                if (showSplash) {
                    SplashScreen(
                        onDismissSplash = { showSplash = false }
                    )
                } else {
                    // State collection
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
                    val elapsedMillis by viewModel.elapsedMillis.collectAsStateWithLifecycle()
                    val camMode by viewModel.camMode.collectAsStateWithLifecycle()
                    val isAudioGunEnabled by viewModel.isAudioGunEnabled.collectAsStateWithLifecycle()
                    val isMovementDetected by viewModel.isMovementDetected.collectAsStateWithLifecycle()
                    val motionEnergy by viewModel.motionEnergy.collectAsStateWithLifecycle()
                    val latestAiAnalysis by viewModel.latestAiAnalysis.collectAsStateWithLifecycle()

                    // Dialog states
                    val showSaveRaceDialog by viewModel.showSaveRaceDialog.collectAsStateWithLifecycle()
                    val suggestedRaceTitle by viewModel.suggestedRaceTitle.collectAsStateWithLifecycle()
                    val showAddAthleteDialog by viewModel.showAddAthleteDialog.collectAsStateWithLifecycle()
                    val showAddSubUserDialog by viewModel.showAddSubUserDialog.collectAsStateWithLifecycle()
                    val showQrPairDialog by viewModel.showQrPairDialog.collectAsStateWithLifecycle()
                    val showWebSyncDialog by viewModel.showWebSyncDialog.collectAsStateWithLifecycle()
                    val showKadTeknikalDialog by viewModel.showKadTeknikalDialog.collectAsStateWithLifecycle()
                    val selectedAthleteDetail by viewModel.selectedAthleteDetail.collectAsStateWithLifecycle()
                    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
                    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

                    // Back navigation handler
                    BackHandler(enabled = currentTab != ScreenTab.DASHBOARD) {
                        currentTab = ScreenTab.DASHBOARD
                    }

                    Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = "Psyco Time X Pro",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Electronic Timing & Sports Management",
                                        fontSize = 10.sp,
                                        color = ElectricCyanGlow
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { viewModel.setShowWebSyncDialog(true) }) {
                                    Icon(
                                        Icons.Default.CloudSync,
                                        contentDescription = "Sync Web",
                                        tint = ElectricCyan
                                    )
                                }
                                IconButton(onClick = { currentTab = ScreenTab.SETTINGS }) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Tetapan",
                                        tint = if (currentTab == ScreenTab.SETTINGS) LaserOrange else Color.White
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
                            modifier = Modifier.testTag("main_bottom_nav")
                        ) {
                            ScreenTab.values().filter { it != ScreenTab.SETTINGS }.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            tab.icon,
                                            contentDescription = tab.title,
                                            tint = if (currentTab == tab) ElectricCyan else TextSecondary
                                        )
                                    },
                                    label = {
                                        Text(
                                            tab.title,
                                            fontSize = 9.sp,
                                            fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentTab == tab) ElectricCyan else TextSecondary
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = StadiumSurfaceVariant
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
                                onOpenAddAthlete = { viewModel.setShowAddAthleteDialog(true) },
                                onOpenKadTeknikal = { viewModel.setShowKadTeknikalDialog(true) },
                                onOpenWebSync = { viewModel.setShowWebSyncDialog(true) },
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
                                onStartTimer = { viewModel.startTimer() },
                                onStopTimer = { viewModel.stopTimer() },
                                onResetTimer = { viewModel.resetTimer() },
                                onTriggerMotion = { hasMotion, energy ->
                                    viewModel.setMotionDetection(hasMotion, energy)
                                },
                                onOpenQrPair = { viewModel.setShowQrPairDialog(true) }
                            )

                            ScreenTab.REPLAY -> ReplayScreen(
                                races = races,
                                currentUserEmail = currentUserEmail,
                                isSuperAdmin = isSuperAdmin,
                                onCapturePhotoFinish = { race, frameMs, isCam2 ->
                                    viewModel.capturePhotoFinish(race, frameMs, isCam2)
                                    Toast.makeText(this@MainActivity, "Photo Finish disimpan ke telefon!", Toast.LENGTH_SHORT).show()
                                },
                                onDownloadVideo = { race, isCam2 ->
                                    viewModel.downloadVideoToPhone(race, isCam2)
                                    Toast.makeText(this@MainActivity, "Video dimuat turun ke folder /PsycoTimeXPro/Videos/", Toast.LENGTH_SHORT).show()
                                }
                            )

                            ScreenTab.ATHLETES -> AthleteListScreen(
                                athletes = athletes,
                                onOpenAddAthlete = { currentTab = ScreenTab.REGISTER_ATHLETE },
                                onSelectAthlete = { viewModel.selectAthlete(it) },
                                onDeleteAthlete = { viewModel.deleteAthlete(it) }
                            )

                            ScreenTab.REGISTER_ATHLETE -> RegisterAthleteScreen(
                                onSaveAthlete = { newAthlete ->
                                    viewModel.addAthlete(newAthlete)
                                    currentTab = ScreenTab.ATHLETES
                                    Toast.makeText(this@MainActivity, "Atlit '${newAthlete.name}' berjaya didaftarkan ke Room!", Toast.LENGTH_SHORT).show()
                                },
                                onNavigateBack = {
                                    currentTab = ScreenTab.ATHLETES
                                }
                            )

                            ScreenTab.FEES -> FeeManagementScreen(
                                fees = fees,
                                athletes = athletes,
                                onAddFee = { viewModel.addFee(it) },
                                onUpdateFee = { viewModel.addFee(it) }
                            )

                            ScreenTab.SUBUSERS -> SubUserScreen(
                                subUsers = subUsers,
                                currentUserEmail = currentUserEmail,
                                isSuperAdmin = isSuperAdmin,
                                onOpenAddSubUser = { viewModel.setShowAddSubUserDialog(true) },
                                onDeleteSubUser = { viewModel.deleteSubUser(it) }
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
                                onSwitchUser = { email, name -> viewModel.switchUser(email, name) },
                                onUpdateClubInfo = { name, logo, coach, phone, photo ->
                                    viewModel.updateClubInfo(name, logo, coach, phone, photo)
                                },
                                onOpenWebSync = { viewModel.setShowWebSyncDialog(true) },
                                onOpenKadTeknikal = { viewModel.setShowKadTeknikalDialog(true) }
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
                        onDismiss = { viewModel.dismissSaveRaceDialog() },
                        onSave = { title, event, athleteName, athleteId, lane, wind, notes ->
                            viewModel.saveRace(title, event, athleteName, athleteId, lane, wind, notes)
                            Toast.makeText(this@MainActivity, "Race '$title' disimpan!", Toast.LENGTH_SHORT).show()
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

                if (showKadTeknikalDialog) {
                    KadTeknikalDialog(
                        athletes = athletes,
                        onDismiss = { viewModel.setShowKadTeknikalDialog(false) },
                        onSaveKadToStorage = { type, content ->
                            val file = File(viewModel.storageManager.appBaseDir, "KadTeknikal/Kad_${type}_${System.currentTimeMillis()}.txt")
                            file.parentFile?.mkdirs()
                            file.writeText(content)
                            file.absolutePath
                        }
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
            }
        }
    }
}
}
