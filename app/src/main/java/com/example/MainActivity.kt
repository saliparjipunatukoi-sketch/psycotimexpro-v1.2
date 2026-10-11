package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
                val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
                var showSplash by remember { mutableStateOf(true) }
                var currentTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }
                var showLogoutConfirmDialog by remember { mutableStateOf(false) }

                if (showSplash) {
                    SplashScreen(
                        onDismissSplash = { showSplash = false }
                    )
                } else if (!isLoggedIn) {
                    // MUST LOGIN FIRST: Does NOT enter Superadmin automatically
                    AuthScreen(
                        onLoginSuccess = { email, name, club, isSuper ->
                            viewModel.login(email, name, club, isSuper)
                            currentTab = ScreenTab.DASHBOARD
                            Toast.makeText(this@MainActivity, "Selamat kembali, $name!", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    // State collection for logged-in user
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
                                            text = if (isSuperAdmin) "Superadmin: $currentUserName" else "Coach: $currentUserName",
                                            fontSize = 10.sp,
                                            color = if (isSuperAdmin) FinishLineRed else SilverMetallic
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { viewModel.setShowWebSyncDialog(true) }) {
                                        Icon(
                                            Icons.Default.CloudSync,
                                            contentDescription = "Sync Web",
                                            tint = Color.White
                                        )
                                    }
                                    IconButton(onClick = { currentTab = ScreenTab.SETTINGS }) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = "Tetapan",
                                            tint = if (currentTab == ScreenTab.SETTINGS) FinishLineRed else Color.White
                                        )
                                    }
                                    // CLEAR LOGOUT BUTTON IN TOP APP BAR
                                    IconButton(
                                        onClick = { showLogoutConfirmDialog = true },
                                        modifier = Modifier.testTag("top_bar_logout_button")
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
                                modifier = Modifier.testTag("bottom_navigation_bar")
                            ) {
                                val tabs = listOf(
                                    ScreenTab.DASHBOARD,
                                    ScreenTab.TIMING,
                                    ScreenTab.REPLAY,
                                    ScreenTab.ATHLETES,
                                    ScreenTab.FEES,
                                    ScreenTab.SUBUSERS,
                                    ScreenTab.WEB_PORTAL
                                )
                                tabs.forEach { tab ->
                                    val selected = currentTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { currentTab = tab },
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
                                                fontSize = 9.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selected) FinishLineRed else SilverBorder
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
                                    onCapturePhotoFinish = { race, ms, isCam2 ->
                                        viewModel.capturePhotoFinish(race, ms, isCam2)
                                    },
                                    onDownloadVideo = { race, isCam2 ->
                                        viewModel.downloadVideoToPhone(race, isCam2)
                                    },
                                    onDeleteRace = { race ->
                                        viewModel.deleteRace(race)
                                        Toast.makeText(this@MainActivity, "Rekod race '${race.title}' berjaya dipadam.", Toast.LENGTH_SHORT).show()
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
                                    onLogout = { showLogoutConfirmDialog = true }
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

                    // LOGOUT CONFIRMATION DIALOG
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
