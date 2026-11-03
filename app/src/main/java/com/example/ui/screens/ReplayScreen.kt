package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RaceRecordEntity
import com.example.service.AiSportsScienceAnalyzer
import com.example.ui.components.PhotoFinishViewer
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReplayScreen(
    races: List<RaceRecordEntity>,
    currentUserEmail: String,
    isSuperAdmin: Boolean,
    onCapturePhotoFinish: (race: RaceRecordEntity, frameMs: Long, isCam2: Boolean) -> Unit,
    onDownloadVideo: (race: RaceRecordEntity, isCam2: Boolean) -> Unit,
    onDeleteRace: (race: RaceRecordEntity) -> Unit,
    onExportSlitFromTable: (race: RaceRecordEntity) -> Unit = {},
    onOpenPostureAiForRace: (race: RaceRecordEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedRace by remember { mutableStateOf<RaceRecordEntity?>(null) }
    var raceToDelete by remember { mutableStateOf<RaceRecordEntity?>(null) }

    val dateFormat = remember {
        SimpleDateFormat("dd-MM-yyyy, hh:mm a", Locale.getDefault())
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TrackDarkNavy)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // HEADER & FILTER STATUS
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Replay, Jadual Analitik & Postur AI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Surface(
                        color = if (isSuperAdmin) FinishLineRed.copy(alpha = 0.2f) else SilverBorder.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isSuperAdmin) "SEMUA VIDEO (SUPERADMIN)" else "PASUKAN SENDIRI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSuperAdmin) FinishLineRed else Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (selectedRace != null) {
                        "Menayangkan video: ${selectedRace?.title} - ${selectedRace?.athleteName}. Tekan 'Kembali ke Senarai' untuk memilih race lain."
                    } else {
                        "Eksport imej Slit Photo-Finish secara terus daripada Jadual Analitik di bawah atau buka Pembaca Postur AI (Start & Finish)."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // IF RACE IS SELECTED: SHOW VIDEO PLAYER
        if (selectedRace != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { selectedRace = null },
                            colors = ButtonDefaults.buttonColors(containerColor = StadiumSurfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("← Kembali ke Jadual", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { onExportSlitFromTable(selectedRace!!) },
                            colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eksport Slit & Postur AI", fontWeight = FontWeight.Black, color = Color.White, fontSize = 11.sp)
                        }
                    }

                    PhotoFinishViewer(
                        race = selectedRace!!,
                        onCapturePhotoFinish = { frameMs, isCam2 ->
                            onCapturePhotoFinish(selectedRace!!, frameMs, isCam2)
                        },
                        onDownloadToPhone = { isCam2 ->
                            onDownloadVideo(selectedRace!!, isCam2)
                        },
                        onDelete = {
                            val target = selectedRace!!
                            selectedRace = null
                            onDeleteRace(target)
                        },
                        onClose = {
                            selectedRace = null
                        }
                    )
                }
            }
        } else {
            // 1. DIRECT ANALYTICS TABLE WITH ONE-TAP PHOTO-FINISH SLIT IMAGE EXPORT & AI POSTURE READER
            if (races.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("race_analytics_table_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.TableChart,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Jadual Analitik Keputusan Race & Eksport Slit",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Eksport imej Slit Photo-Finish & baca Postur Start/Finish AI terus dari jadual",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }

                            // Scrollable Analytics Table
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                // Table Header
                                Row(
                                    modifier = Modifier
                                        .background(Color.Black, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .padding(vertical = 8.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("RACE / ATLIT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = ElectricCyan, modifier = Modifier.width(135.dp))
                                    Text("MASA (s)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = ElectricCyan, modifier = Modifier.width(85.dp))
                                    Text("POSTUR START/FINISH", fontSize = 10.sp, fontWeight = FontWeight.Black, color = ElectricCyan, modifier = Modifier.width(145.dp))
                                    Text("EKSPORT SLIT & POSTUR AI", fontSize = 10.sp, fontWeight = FontWeight.Black, color = ElectricCyan, modifier = Modifier.width(210.dp))
                                }

                                // Table Rows
                                races.forEachIndexed { index, race ->
                                    val posture = remember(race) { AiSportsScienceAnalyzer.analyzePostureForRace(race) }
                                    val rowBg = if (index % 2 == 0) StadiumSurfaceVariant.copy(alpha = 0.65f) else StadiumSurface
                                    Row(
                                        modifier = Modifier
                                            .background(rowBg)
                                            .padding(vertical = 8.dp, horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Column 1: Race & Athlete
                                        Column(modifier = Modifier.width(135.dp)) {
                                            Text(
                                                text = race.title,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${race.athleteName} (${race.eventName})",
                                                fontSize = 10.sp,
                                                color = SilverMetallic,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Column 2: Official Time
                                        Column(modifier = Modifier.width(85.dp)) {
                                            Text(
                                                text = String.format(Locale.US, "%.2f s", race.recordedTimeMillis / 1000.0),
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Black,
                                                color = FinishLineRed
                                            )
                                            Text(
                                                text = "${race.cadenceSpM} spm",
                                                fontSize = 9.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        // Column 3: Start & Finish Posture Summary
                                        Column(modifier = Modifier.width(145.dp)) {
                                            Text(
                                                text = "Start: ${"%.1f".format(posture.startBodyLeanAngleDeg)}° ${if (posture.isStartPostureCorrect) "✓" else "⚠"}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (posture.isStartPostureCorrect) SuccessGreen else LaserOrange
                                            )
                                            Text(
                                                text = "Finish: ${"%.1f".format(posture.finishTorsoDipAngleDeg)}° ${if (posture.isFinishPostureCorrect) "✓" else "⚠"}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (posture.isFinishPostureCorrect) ElectricCyan else FinishLineRed
                                            )
                                        }

                                        // Column 4: Direct Slit Export & AI Posture Buttons
                                        Row(
                                            modifier = Modifier.width(210.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = { onExportSlitFromTable(race) },
                                                colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .height(32.dp)
                                                    .testTag("export_slit_table_btn_${race.id}")
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Eksport Slit", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                                            }

                                            FilledTonalButton(
                                                onClick = { onOpenPostureAiForRace(race) },
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = ElectricCyan.copy(alpha = 0.2f)
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .height(32.dp)
                                                    .testTag("posture_ai_table_btn_${race.id}")
                                            ) {
                                                Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Postur AI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. LIST OF RECORDED RACES
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Senarai Rakaman Selesai (${races.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Pilih untuk tonton video / analisis",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            if (races.isEmpty()) {
                item {
                    Surface(
                        color = StadiumSurface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.VideocamOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Tiada rakaman race lagi.", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Jalankan ujian masa di Tab ET Timing untuk merekodkan Race 1, Race 2...", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(races) { race ->
                    val dateStr = try {
                        dateFormat.format(Date(race.createdAt))
                    } catch (_: Exception) {
                        "20-10-2026, 10:00 AM"
                    }
                    val posture = remember(race) { AiSportsScienceAnalyzer.analyzePostureForRace(race) }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, StadiumBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRace = race }
                            .testTag("race_item_${race.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${race.title} - ${race.athleteName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Tarikh & Masa: $dateStr",
                                        fontSize = 11.sp,
                                        color = SilverMetallic
                                    )
                                    Text(
                                        text = "${race.eventName} • Lorong ${race.lane} • Angin: ${race.windReading}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }

                                Surface(
                                    color = Color.Black,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, FinishLineRed)
                                ) {
                                    Text(
                                        text = race.formattedTime,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = FinishLineRed,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // SAINS SUKAN, BIOMEKANIK & POSTUR START/FINISH BADGES
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = StadiumSurfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "⚡ Cadence: ${race.cadenceSpM} spm",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricCyan,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = StadiumSurfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "🚀 Start: ${"%.1f".format(posture.startBodyLeanAngleDeg)}° (${if (posture.isStartPostureCorrect) "Betul" else "Salah"})",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (posture.isStartPostureCorrect) SuccessGreen else LaserOrange,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = StadiumSurfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "📐 Finish: ${"%.1f".format(posture.finishTorsoDipAngleDeg)}° (${if (posture.isFinishPostureCorrect) "Betul" else "Salah"})",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (posture.isFinishPostureCorrect) ElectricCyan else FinishLineRed,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // ACTION ROW: TONTON VIDEO, EKSPORT SLIT, POSTUR AI, PADAM
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { selectedRace = race },
                                        colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Video", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    OutlinedButton(
                                        onClick = { onExportSlitFromTable(race) },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, FinishLineRed),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, tint = FinishLineRed, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Eksport Slit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    FilledTonalButton(
                                        onClick = { onOpenPostureAiForRace(race) },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = ElectricCyan.copy(alpha = 0.18f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Postur AI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                                    }
                                }

                                IconButton(
                                    onClick = { raceToDelete = race },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color(0xFF7F1D1D).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Padam Rekod",
                                        tint = FinishLineRed,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG PENGESAHAN PADAM REKOD RACE
    if (raceToDelete != null) {
        AlertDialog(
            onDismissRequest = { raceToDelete = null },
            title = {
                Text(
                    text = "Padam Rekod & Video?",
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Adakah anda pasti mahu memadam rakaman '${raceToDelete?.title} - ${raceToDelete?.athleteName}'? Tindakan ini akan mengosongkan storan telefon anda.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = raceToDelete!!
                        raceToDelete = null
                        onDeleteRace(target)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed)
                ) {
                    Text("Padam Sekarang", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { raceToDelete = null }) {
                    Text("Batal", color = SilverMetallic)
                }
            }
        )
    }
}
