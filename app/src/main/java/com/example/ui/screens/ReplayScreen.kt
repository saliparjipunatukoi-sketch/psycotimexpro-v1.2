package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    modifier: Modifier = Modifier
) {
    // User must click on a list item to show the video player (does not show monitor immediately)
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
                        text = "Replay & Rakaman Masa Race",
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
                        "Pilih mana-mana rekod perlumbaan di bawah untuk memaparkan video Cam 1 & Cam 2 dan analisis torso."
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
                    Button(
                        onClick = { selectedRace = null },
                        colors = ButtonDefaults.buttonColors(containerColor = StadiumSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("← Tutup Pemain & Kembali ke Senarai", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
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
            // IF NO RACE SELECTED: SHOW THE CLEAN LIST OF RACES ONLY
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
                        text = "Pilih untuk tonton video",
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

                    Card(
                        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRace = race }
                            .testTag("race_item_${race.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                // Contoh: Race 1 - Mohd Ali ( 20-10-2026, 10:30 AM )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "${race.title} - ${race.athleteName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(3.dp))

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

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        color = Color.Black,
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, FinishLineRed)
                                    ) {
                                        Text(
                                            text = race.formattedTime,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = FinishLineRed,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Surface(
                                        color = if (race.hasMotionDetected) SuccessGreen.copy(alpha = 0.2f) else TextMuted.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (race.hasMotionDetected) "Torso Dikesan ✓" else "No Detection",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (race.hasMotionDetected) SuccessGreen else TextMuted,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Butang Tonton Video
                                Button(
                                    onClick = { selectedRace = race },
                                    colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tonton Video", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                // Butang Padam Video (Delete to reduce file storage)
                                IconButton(
                                    onClick = { raceToDelete = race },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF7F1D1D).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Padam Rekod",
                                        tint = FinishLineRed,
                                        modifier = Modifier.size(18.dp)
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
