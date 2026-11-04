package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
    var selectedRace by remember(races.firstOrNull()?.id) { mutableStateOf(races.firstOrNull()) }
    var raceToDelete by remember { mutableStateOf<RaceRecordEntity?>(null) }
    var showCompactTable by remember { mutableStateOf(true) }

    val dateFormat = remember {
        SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TrackDarkNavy)
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 84.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ULTRA-COMPACT PRO BROADCAST STUDIO BAR (Zero bloated fonts)
        item {
            Surface(
                color = StadiumSurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, StadiumBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .background(FinishLineRed.copy(alpha = 0.18f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.SlowMotionVideo,
                                contentDescription = null,
                                tint = FinishLineRed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "STUDIO REPLAY PORTRAIT & TELEMETRI AI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "${races.size} Rekod • Kotak AI Menegak • Photo-Finish Slit",
                                fontSize = 8.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Surface(
                            color = if (showCompactTable) ElectricCyan.copy(alpha = 0.18f) else StadiumSurfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (showCompactTable) ElectricCyan else StadiumBorder),
                            modifier = Modifier
                                .height(24.dp)
                                .clickable { showCompactTable = !showCompactTable }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = if (showCompactTable) ElectricCyan else SilverMetallic,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Jadual",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (showCompactTable) ElectricCyan else Color.White
                                )
                            }
                        }

                        Surface(
                            color = if (isSuperAdmin) FinishLineRed.copy(alpha = 0.18f) else StadiumSurfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isSuperAdmin) "ADMIN" else "COACH",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSuperAdmin) FinishLineRed else SilverMetallic,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // ACTIVE PORTRAIT VIDEO & PHOTO-FINISH VIEWER (Docked right at top for instant replay!)
        selectedRace?.let { activeRace ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { onExportSlitFromTable(activeRace) },
                            colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eksport Imej Slit (PNG)", fontWeight = FontWeight.Black, color = Color.White, fontSize = 10.sp)
                        }

                        FilledTonalButton(
                            onClick = { onOpenPostureAiForRace(activeRace) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = ElectricCyan.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                        ) {
                            Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Laporan Postur AI", fontWeight = FontWeight.Black, color = ElectricCyan, fontSize = 10.sp)
                        }
                    }

                    PhotoFinishViewer(
                        race = activeRace,
                        onCapturePhotoFinish = { frameMs, isCam2 ->
                            onCapturePhotoFinish(activeRace, frameMs, isCam2)
                        },
                        onDownloadToPhone = { isCam2 ->
                            onDownloadVideo(activeRace, isCam2)
                        },
                        onDelete = {
                            val target = activeRace
                            selectedRace = races.firstOrNull { it.id != target.id }
                            onDeleteRace(target)
                        },
                        onClose = {
                            selectedRace = null
                        }
                    )
                }
            }
        }

        // COMPACT ANALYTICS TABLE WITH ONE-TAP SLIT EXPORT & POSTURE AI
        if (races.isNotEmpty() && showCompactTable) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("race_analytics_table_card")
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Jadual Analitik & Eksport Slit Pantas",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Klik baris untuk tonton video",
                                fontSize = 8.sp,
                                color = TextSecondary
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            // Compact Header
                            Row(
                                modifier = Modifier
                                    .background(Color.Black, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .padding(vertical = 5.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("RACE / ATLIT", fontSize = 8.sp, fontWeight = FontWeight.Black, color = ElectricCyan, modifier = Modifier.width(120.dp))
                                Text("MASA", fontSize = 8.sp, fontWeight = FontWeight.Black, color = ElectricCyan, modifier = Modifier.width(68.dp))
                                Text("POSTUR START/FINISH", fontSize = 8.sp, fontWeight = FontWeight.Black, color = ElectricCyan, modifier = Modifier.width(125.dp))
                                Text("TINDAKAN PANTAS", fontSize = 8.sp, fontWeight = FontWeight.Black, color = ElectricCyan, modifier = Modifier.width(175.dp))
                            }

                            races.forEachIndexed { index, race ->
                                val posture = remember(race) { AiSportsScienceAnalyzer.analyzePostureForRace(race) }
                                val isCurrentSelected = selectedRace?.id == race.id
                                val rowBg = when {
                                    isCurrentSelected -> ElectricCyan.copy(alpha = 0.14f)
                                    index % 2 == 0 -> StadiumSurfaceVariant.copy(alpha = 0.55f)
                                    else -> StadiumSurface
                                }
                                Row(
                                    modifier = Modifier
                                        .background(rowBg)
                                        .clickable { selectedRace = race }
                                        .padding(vertical = 5.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.width(120.dp)) {
                                        Text(
                                            text = "${race.title} • ${race.athleteName}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${race.eventName} (L${race.lane})",
                                            fontSize = 8.sp,
                                            color = SilverMetallic,
                                            maxLines = 1
                                        )
                                    }

                                    Column(modifier = Modifier.width(68.dp)) {
                                        Text(
                                            text = String.format(Locale.US, "%.2fs", race.recordedTimeMillis / 1000.0),
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black,
                                            color = FinishLineRed
                                        )
                                        Text(
                                            text = "${race.cadenceSpM} spm",
                                            fontSize = 7.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Column(modifier = Modifier.width(125.dp)) {
                                        Text(
                                            text = "S: ${"%.1f".format(posture.startBodyLeanAngleDeg)}° ${if (posture.isStartPostureCorrect) "✓" else "⚠"} | F: ${"%.1f".format(posture.finishTorsoDipAngleDeg)}° ${if (posture.isFinishPostureCorrect) "✓" else "⚠"}",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (posture.isStartPostureCorrect && posture.isFinishPostureCorrect) SuccessGreen else SpeedAmber
                                        )
                                        Text(
                                            text = "Skor AI: ${posture.overallPostureScore}%",
                                            fontSize = 7.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.width(175.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { onExportSlitFromTable(race) },
                                            colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier
                                                .height(24.dp)
                                                .testTag("export_slit_table_btn_${race.id}")
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Slit", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White)
                                        }

                                        FilledTonalButton(
                                            onClick = { onOpenPostureAiForRace(race) },
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = ElectricCyan.copy(alpha = 0.2f)
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier
                                                .height(24.dp)
                                                .testTag("posture_ai_table_btn_${race.id}")
                                        ) {
                                            Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Postur", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                                        }

                                        Surface(
                                            color = StadiumSurfaceVariant,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier
                                                .height(24.dp)
                                                .clickable { selectedRace = race }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                                Text("Play", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
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

        // COMPACT RACE RECORD CARDS LIST
        if (races.isEmpty()) {
            item {
                Surface(
                    color = StadiumSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.VideocamOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Tiada rakaman race lagi.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Jalankan ujian masa di Tab Timing ET untuk merekodkan video Portrait.", color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        } else {
            items(races) { race ->
                val dateStr = try {
                    dateFormat.format(Date(race.createdAt))
                } catch (_: Exception) {
                    "20/10/26"
                }
                val posture = remember(race) { AiSportsScienceAnalyzer.analyzePostureForRace(race) }
                val isSelected = selectedRace?.id == race.id

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) StadiumSurfaceVariant else StadiumSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) ElectricCyan else StadiumBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedRace = race }
                        .testTag("race_item_${race.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${race.title} • ${race.athleteName}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = dateStr,
                                        fontSize = 8.sp,
                                        color = TextMuted
                                    )
                                }
                                Text(
                                    text = "${race.eventName} • Lorong ${race.lane} • Angin ${race.windReading} • Start ${"%.1f".format(posture.startBodyLeanAngleDeg)}° | Torso ${"%.1f".format(posture.finishTorsoDipAngleDeg)}°",
                                    fontSize = 9.sp,
                                    color = SilverMetallic,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                color = Color.Black,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, FinishLineRed)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.2fs", race.recordedTimeMillis / 1000.0),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = FinishLineRed,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Slim Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Button(
                                    onClick = { selectedRace = race },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) ElectricCyan else FinishLineRed
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.Black else Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isSelected) "Sedang Main" else "Main Video",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isSelected) Color.Black else Color.White
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onExportSlitFromTable(race) },
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, FinishLineRed),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = FinishLineRed, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Eksport Slit", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                FilledTonalButton(
                                    onClick = { onOpenPostureAiForRace(race) },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = ElectricCyan.copy(alpha = 0.16f)
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Postur AI", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                                }
                            }

                            IconButton(
                                onClick = { raceToDelete = race },
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(Color(0xFF7F1D1D).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Padam Rekod",
                                    tint = FinishLineRed,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (raceToDelete != null) {
        AlertDialog(
            onDismissRequest = { raceToDelete = null },
            title = {
                Text(
                    text = "Padam Rekod & Video?",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Adakah anda pasti mahu memadam rakaman '${raceToDelete?.title} - ${raceToDelete?.athleteName}'?",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = raceToDelete!!
                        raceToDelete = null
                        if (selectedRace?.id == target.id) {
                            selectedRace = null
                        }
                        onDeleteRace(target)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed)
                ) {
                    Text("Padam", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { raceToDelete = null }) {
                    Text("Batal", color = SilverMetallic, fontSize = 11.sp)
                }
            }
        )
    }
}
