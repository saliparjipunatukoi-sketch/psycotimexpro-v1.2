package com.example.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.AchievementEntity
import com.example.data.model.Athlete
import com.example.data.model.FeeRecordEntity
import com.example.data.model.PerformanceMetrics
import com.example.data.model.RaceRecordEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AthleteDetailDialog(
    athlete: Athlete,
    races: List<RaceRecordEntity> = emptyList(),
    fees: List<FeeRecordEntity> = emptyList(),
    achievements: List<AchievementEntity>,
    performanceMetrics: List<PerformanceMetrics> = emptyList(),
    onDismiss: () -> Unit,
    onUpdateAthlete: (Athlete) -> Unit = {},
    onDeleteAthlete: () -> Unit,
    onAddFeeForAthlete: (FeeRecordEntity) -> Unit = {},
    onToggleFeePaid: (FeeRecordEntity) -> Unit = {},
    onExportMonthlyPdf: (Athlete) -> Unit = {},
    onAddAchievement: (tournament: String, event: String, medal: String, record: String) -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Prestasi Sebulan, 1 = Rekod Yuran Sepanjang Masa, 2 = Sains Sukan & Sijil
    var isEditingProfile by remember { mutableStateOf(false) }

    var editName by remember(athlete) { mutableStateOf(athlete.name) }
    var editEvent by remember(athlete) { mutableStateOf(athlete.specificEvent) }
    var editPbValue by remember(athlete) { mutableStateOf(athlete.pbValue) }
    var editClubName by remember(athlete) { mutableStateOf(athlete.clubName) }
    var editFeeAmount by remember(athlete) { mutableStateOf(athlete.registrationFee.toString()) }
    var currentPhotoUri by remember(athlete) { mutableStateOf(athlete.photoUri) }

    // Add Fee quick inputs inside Athlete Profile
    var showAddFeeInline by remember { mutableStateOf(false) }
    var newFeeType by remember { mutableStateOf("Yuran Bulanan Latihan (${SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())})") }
    var newFeeAmount by remember { mutableStateOf("80.0") }
    var newFeePaid by remember { mutableStateOf(true) }

    // Add Achievement inputs
    var showAddAchievementDialog by remember { mutableStateOf(false) }
    var tournamentInput by remember { mutableStateOf("") }
    var eventInput by remember { mutableStateOf(athlete.specificEvent) }
    var medalInput by remember { mutableStateOf("Emas") }
    var recordInput by remember { mutableStateOf(athlete.pbValue) }

    // Photo Picker for Athlete Profile Picture
    val athletePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentPhotoUri = uri.toString()
            onUpdateAthlete(athlete.copy(photoUri = uri.toString()))
        }
    }

    val dateTimeFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    val dateOnlyFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val registeredDateStr = remember(athlete.createdAt) {
        dateOnlyFormatter.format(Date(athlete.createdAt))
    }

    val oneMonthAgoMillis = remember { System.currentTimeMillis() - (31L * 24 * 60 * 60 * 1000) }
    val athleteRaces = remember(races, athlete) {
        races.filter { it.athleteId == athlete.id || it.athleteName.equals(athlete.name, ignoreCase = true) }
            .sortedByDescending { it.createdAt }
    }
    val monthlyRaces = remember(athleteRaces, oneMonthAgoMillis) {
        athleteRaces.filter { it.createdAt >= oneMonthAgoMillis }
    }
    val athleteMetrics = remember(performanceMetrics, athlete) {
        performanceMetrics.filter { it.athleteId == athlete.id || it.athleteName.equals(athlete.name, ignoreCase = true) }
            .sortedByDescending { it.recordedAt }
    }
    val athleteFees = remember(fees, athlete) {
        fees.filter { it.athleteId == athlete.id || it.athleteName.equals(athlete.name, ignoreCase = true) }
            .sortedByDescending { it.id }
    }

    val totalLifetimePaid = remember(athleteFees, athlete) {
        if (athleteFees.isNotEmpty()) {
            athleteFees.filter { it.isPaid }.sumOf { it.amount }
        } else if (athlete.feePaidStatus) {
            athlete.registrationFee
        } else 0.0
    }
    val totalLifetimePending = remember(athleteFees, athlete) {
        if (athleteFees.isNotEmpty()) {
            athleteFees.filter { !it.isPaid }.sumOf { it.amount }
        } else if (!athlete.feePaidStatus) {
            athlete.registrationFee
        } else 0.0
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = StadiumSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, ElectricCyan.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("athlete_detail_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Profile Header
                Surface(
                    color = TrackDarkNavy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PROFIL LENGKAP ATLIT & PENGURUSAN KELAB",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = ElectricCyan
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { isEditingProfile = !isEditingProfile },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Profil Atlit",
                                        tint = if (isEditingProfile) LaserOrange else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onDeleteAthlete,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Padam Atlit",
                                        tint = FinishLineRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Tutup",
                                        tint = SilverMetallic,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Athlete Photo Avatar (Clickable to Upload/Change Photo) + Bio
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(74.dp)
                                        .clip(CircleShape)
                                        .background(StadiumSurfaceVariant)
                                        .border(2.dp, ElectricCyan, CircleShape)
                                        .clickable {
                                            athletePhotoPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                        .testTag("athlete_profile_photo_picker")
                                ) {
                                    if (!currentPhotoUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = currentPhotoUri,
                                            contentDescription = athlete.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.AddAPhoto,
                                            contentDescription = "Muat Naik Gambar Atlit",
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "📷 Tukar Foto",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan,
                                    modifier = Modifier.clickable {
                                        athletePhotoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = athlete.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "${athlete.specificEvent} (${athlete.category}) • ${athlete.gender}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan
                                )
                                Text(
                                    text = "Kelab: ${athlete.clubName}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Didaftarkan Sejak: $registeredDateStr (Auto-Sync Web ✓)",
                                    fontSize = 10.sp,
                                    color = SuccessGreen
                                )
                            }

                            // PB Highlight Pill
                            Surface(
                                color = SpeedAmber.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SpeedAmber.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("REKOD PB", fontSize = 8.sp, fontWeight = FontWeight.Black, color = SpeedAmber)
                                    Text(
                                        text = athlete.pbValue,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(athlete.pbUnit.take(8), fontSize = 8.sp, color = TextSecondary)
                                }
                            }
                        }

                        // Export Monthly PDF Report Button directly in Header
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onExportMonthlyPdf(athlete) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("btn_export_athlete_monthly_pdf")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Muat Turun Laporan Bulanan Atlit & Yuran (PDF)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                    }
                }

                // Navigation Tabs inside Athlete Profile
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = StadiumSurfaceVariant,
                    contentColor = ElectricCyan
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Text(
                                "Prestasi Sebulan (${monthlyRaces.size + athleteMetrics.size})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Text(
                                "Laporan Yuran (${athleteFees.size})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = {
                            Text(
                                "AI Sains & Sijil",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }

                // Scrollable Body Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Inline Edit Profile Section if toggled
                    if (isEditingProfile) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StadiumSurfaceVariant),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, LaserOrange),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "KEMASKINI PROFIL & PB ATLIT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = LaserOrange
                                )
                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    label = { Text("Nama Penuh Atlit") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = editEvent,
                                        onValueChange = { editEvent = it },
                                        label = { Text("Acara Utama") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = editPbValue,
                                        onValueChange = { editPbValue = it },
                                        label = { Text("Rekod PB (${athlete.pbUnit})") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = editClubName,
                                        onValueChange = { editClubName = it },
                                        label = { Text("Nama Kelab") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = editFeeAmount,
                                        onValueChange = { editFeeAmount = it },
                                        label = { Text("Yuran (RM)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { isEditingProfile = false }) {
                                        Text("Batal", color = TextSecondary)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                            val updated = athlete.copy(
                                                name = editName.trim().ifBlank { athlete.name },
                                                specificEvent = editEvent.trim().ifBlank { athlete.specificEvent },
                                                pbValue = editPbValue.trim().ifBlank { athlete.pbValue },
                                                clubName = editClubName.trim().ifBlank { athlete.clubName },
                                                registrationFee = editFeeAmount.toDoubleOrNull() ?: athlete.registrationFee,
                                                photoUri = currentPhotoUri
                                            )
                                            onUpdateAthlete(updated)
                                            isEditingProfile = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                    ) {
                                        Text("Simpan & Sync Web", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    when (activeTab) {
                        0 -> {
                            // TAB 0: 1-MONTH PERFORMANCE REPORT BY DATE & TIME
                            Surface(
                                color = ElectricCyan.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "📅 REKOD PRESTASI SEBULAN MENGIKUT TARIKH & MASA",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ElectricCyan
                                    )
                                    Text(
                                        text = "Menunjukkan semua ujian masa ET, kelajuan angin, dan bacaan biomekanik AI bagi '${athlete.name}' sepanjang 30 hari terkini.",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            if (monthlyRaces.isEmpty() && athleteMetrics.isEmpty()) {
                                Surface(
                                    color = StadiumSurfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.TimerOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Tiada rekod ujian masa bagi bulan ini.",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Rekodkan larian di tab 'Timing ET' atau 'Lompat AI' dan pilih nama ${athlete.name}.",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            } else {
                                monthlyRaces.forEach { race ->
                                    Surface(
                                        color = StadiumSurfaceVariant,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, StadiumBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "${race.title} • ${race.eventName}",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = "🕒 Tarikh & Masa: ${dateTimeFormatter.format(Date(race.createdAt))}",
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = ElectricCyan
                                                    )
                                                }
                                                Surface(
                                                    color = FinishLineRed.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = String.format(Locale.US, "%.2f s", race.recordedTimeMillis / 1000.0),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = FinishLineRed,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Lorong ${race.lane} • Angin ${race.windReading}",
                                                    fontSize = 10.sp,
                                                    color = TextSecondary
                                                )
                                                Text(
                                                    text = "Cadence: ${race.cadenceSpM} spm • GCT: ${race.groundContactTimeMs}ms • Torso: ${race.torsoLeanAngleDeg}°",
                                                    fontSize = 10.sp,
                                                    color = SpeedAmber,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }

                                athleteMetrics.forEach { metric ->
                                    Surface(
                                        color = StadiumSurfaceVariant,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, LaserOrange.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "${metric.eventName} (Rekod Metrik & PB)",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "🕒 Tarikh & Masa: ${dateTimeFormatter.format(Date(metric.recordedAt))}",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = LaserOrange
                                                )
                                                Text(
                                                    text = "Cadence: ${metric.cadenceSpM} spm • GCT: ${metric.groundContactTimeMs}ms • Torso: ${metric.torsoLeanAngleDeg}°",
                                                    fontSize = 10.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                            Text(
                                                text = "${metric.pbValue} ${metric.pbUnit.take(5)}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                color = LaserOrange
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // TAB 1: LIFETIME FEE PAYMENT REPORT SINCE REGISTRATION
                            Surface(
                                color = StadiumSurfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "💳 REKOD PEMBAYARAN YURAN SEPANJANG MASA (SEJAK DAFTAR: $registeredDateStr)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = SuccessGreen
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = Color.Black,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("Jumlah Lunas (Sejak Daftar)", fontSize = 9.sp, color = TextSecondary)
                                                Text(
                                                    text = String.format(Locale.US, "RM %.2f", totalLifetimePaid),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = SuccessGreen
                                                )
                                            }
                                        }
                                        Surface(
                                            color = Color.Black,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("Jumlah Tertunggak", fontSize = 9.sp, color = TextSecondary)
                                                Text(
                                                    text = String.format(Locale.US, "RM %.2f", totalLifetimePending),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = SpeedAmber
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = { showAddFeeInline = !showAddFeeInline },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.AddCard, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "+ Rekod Bayaran Yuran Baharu Untuk ${athlete.name}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }

                            if (showAddFeeInline) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = TrackDarkNavy),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, ElectricCyan),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Tambah Rekod Yuran Atlit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                                        OutlinedTextField(
                                            value = newFeeType,
                                            onValueChange = { newFeeType = it },
                                            label = { Text("Bulan / Jenis Yuran") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = newFeeAmount,
                                                onValueChange = { newFeeAmount = it },
                                                label = { Text("Jumlah (RM)") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable { newFeePaid = !newFeePaid }
                                            ) {
                                                Checkbox(checked = newFeePaid, onCheckedChange = { newFeePaid = it })
                                                Text(if (newFeePaid) "Lunas" else "Tertunggak", fontSize = 11.sp, color = Color.White)
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                val amt = newFeeAmount.toDoubleOrNull() ?: 80.0
                                                val nowStr = dateTimeFormatter.format(Date())
                                                onAddFeeForAthlete(
                                                    FeeRecordEntity(
                                                        athleteId = athlete.id,
                                                        athleteName = athlete.name,
                                                        feeType = newFeeType,
                                                        amount = amt,
                                                        isPaid = newFeePaid,
                                                        receiptNo = "PTX-${System.currentTimeMillis() % 100000}",
                                                        paymentDate = nowStr,
                                                        notes = "Direkodkan melalui Profil Atlit"
                                                    )
                                                )
                                                showAddFeeInline = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Simpan Rekod Yuran", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            if (athleteFees.isEmpty()) {
                                Surface(
                                    color = StadiumSurfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Yuran Pendaftaran Asal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("Tarikh Daftar: $registeredDateStr", fontSize = 10.sp, color = TextSecondary)
                                        }
                                        Text(
                                            text = String.format(Locale.US, "RM %.2f (%s)", athlete.registrationFee, if (athlete.feePaidStatus) "LUNAS" else "TERTUNGGAK"),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (athlete.feePaidStatus) SuccessGreen else SpeedAmber
                                        )
                                    }
                                }
                            } else {
                                athleteFees.forEach { fee ->
                                    Surface(
                                        color = StadiumSurfaceVariant,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, if (fee.isPaid) SuccessGreen.copy(alpha = 0.4f) else SpeedAmber.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(fee.feeType, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text("Resit: ${fee.receiptNo} • Tarikh: ${fee.paymentDate}", fontSize = 10.sp, color = TextSecondary)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = String.format(Locale.US, "RM %.2f", fee.amount),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (fee.isPaid) SuccessGreen else SpeedAmber
                                                )
                                                TextButton(
                                                    onClick = { onToggleFeePaid(fee.copy(isPaid = !fee.isPaid)) },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                                ) {
                                                    Text(
                                                        text = if (fee.isPaid) "✓ Lunas (Tukar)" else "⏳ Tanda Lunas",
                                                        fontSize = 10.sp,
                                                        color = ElectricCyan,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // TAB 2: AI SPORTS SCIENCE BIOMECHANICS & ACHIEVEMENTS
                            val latestMetric = athleteMetrics.firstOrNull()
                            val displayCadence = latestMetric?.cadenceSpM ?: athleteRaces.firstOrNull()?.cadenceSpM ?: 258
                            val displayGct = latestMetric?.groundContactTimeMs ?: athleteRaces.firstOrNull()?.groundContactTimeMs ?: 106
                            val displayTorso = latestMetric?.torsoLeanAngleDeg ?: athleteRaces.firstOrNull()?.torsoLeanAngleDeg ?: 14.5
                            val displayStrideFreq = latestMetric?.strideFrequencyHz ?: (displayCadence / 60.0)

                            Surface(
                                color = StadiumSurfaceVariant,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "🔬 ANALISIS BIOMEKANIK & ENJIN SAINS SUKAN AI",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ElectricCyan
                                    )
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(color = Color.Black, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Cadence", fontSize = 9.sp, color = TextSecondary)
                                                Text("$displayCadence spm", fontSize = 13.sp, fontWeight = FontWeight.Black, color = ElectricCyan)
                                                Text("%.2f Hz".format(displayStrideFreq), fontSize = 8.sp, color = TextMuted)
                                            }
                                        }
                                        Surface(color = Color.Black, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Sentuhan GCT", fontSize = 9.sp, color = TextSecondary)
                                                Text("${displayGct} ms", fontSize = 13.sp, fontWeight = FontWeight.Black, color = LaserOrange)
                                                Text(if (displayGct <= 110) "RSI Elit (2.68)" else "Sederhana (2.25)", fontSize = 8.sp, color = SuccessGreen)
                                            }
                                        }
                                        Surface(color = Color.Black, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Sudut Torso", fontSize = 9.sp, color = TextSecondary)
                                                Text("%.1f°".format(displayTorso), fontSize = 13.sp, fontWeight = FontWeight.Black, color = FinishLineRed)
                                                Text("Garisan Penamat", fontSize = 8.sp, color = TextMuted)
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Pencapaian Kejohanan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                TextButton(onClick = { showAddAchievementDialog = true }) {
                                    Text("+ Tambah Pencapaian", color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            val athleteAchievements = achievements.filter { it.athleteId == athlete.id }
                            if (athleteAchievements.isEmpty()) {
                                Text("Belum ada pencapaian kejohanan direkodkan.", fontSize = 11.sp, color = TextSecondary)
                            } else {
                                athleteAchievements.forEach { ach ->
                                    Surface(
                                        color = StadiumSurfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(ach.tournamentName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text("${ach.eventName} • Rekod: ${ach.resultRecord}", fontSize = 10.sp, color = TextSecondary)
                                            }
                                            Text(ach.medal, fontSize = 11.sp, fontWeight = FontWeight.Black, color = PodiumGold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddAchievementDialog) {
        AlertDialog(
            onDismissRequest = { showAddAchievementDialog = false },
            title = { Text("Rekod Pencapaian Baharu", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tournamentInput,
                        onValueChange = { tournamentInput = it },
                        label = { Text("Nama Kejohanan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = eventInput,
                        onValueChange = { eventInput = it },
                        label = { Text("Acara") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = medalInput,
                        onValueChange = { medalInput = it },
                        label = { Text("Pingat (Emas / Perak / Gangsa)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = recordInput,
                        onValueChange = { recordInput = it },
                        label = { Text("Catatan Keputusan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tournamentInput.isNotBlank()) {
                            onAddAchievement(tournamentInput, eventInput, medalInput, recordInput)
                            showAddAchievementDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Text("Simpan", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAchievementDialog = false }) { Text("Batal") }
            },
            containerColor = StadiumSurface
        )
    }
}
