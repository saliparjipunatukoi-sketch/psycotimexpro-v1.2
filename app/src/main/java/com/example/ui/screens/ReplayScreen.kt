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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RaceRecordEntity
import com.example.ui.components.PhotoFinishViewer
import com.example.ui.theme.*

@Composable
fun ReplayScreen(
    races: List<RaceRecordEntity>,
    currentUserEmail: String,
    isSuperAdmin: Boolean,
    onCapturePhotoFinish: (race: RaceRecordEntity, frameMs: Long, isCam2: Boolean) -> Unit,
    onDownloadVideo: (race: RaceRecordEntity, isCam2: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRace by remember { mutableStateOf<RaceRecordEntity?>(races.firstOrNull()) }

    // Synchronize selection if list changes
    LaunchedEffect(races) {
        if (selectedRace == null && races.isNotEmpty()) {
            selectedRace = races.first()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TrackDarkNavy)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // HEADER & FILTER STATUS (Items 4 & 5)
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
                        color = if (isSuperAdmin) LaserOrange.copy(alpha = 0.2f) else ElectricCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isSuperAdmin) "SEMUA VIDEO (SUPERADMIN)" else "VIDEO PASUKAN SENDIRI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSuperAdmin) LaserOrange else ElectricCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isSuperAdmin) {
                        "Akses Superadmin: Melihat semua rekod ET kelab dan urusetia."
                    } else {
                        "Hanya video di bawah akaun $currentUserEmail dan sub-user berdaftar yang dipaparkan."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // PRO VIDEO PLAYER & FRAME-BY-FRAME PHOTO FINISH (Items 8 & 13)
        item {
            if (selectedRace != null) {
                PhotoFinishViewer(
                    race = selectedRace!!,
                    onCapturePhotoFinish = { frameMs, isCam2 ->
                        onCapturePhotoFinish(selectedRace!!, frameMs, isCam2)
                    },
                    onDownloadToPhone = { isCam2 ->
                        onDownloadVideo(selectedRace!!, isCam2)
                    }
                )
            } else {
                Surface(
                    color = StadiumSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Tiada rakaman race dipilih atau belum ada race yang disimpan.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // SECTION: SENARAI RACE RECORDED (Item 6)
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
                    text = "Pilih untuk tonton semula",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        if (races.isEmpty()) {
            item {
                Surface(
                    color = StadiumSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.VideocamOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tiada rakaman race lagi.", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Jalankan ujian masa di Tab ET Timing untuk merekodkan Race 1, Race 2...", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(races) { race ->
                val isSelected = selectedRace?.id == race.id
                RaceHistoryCard(
                    race = race,
                    isSelected = isSelected,
                    onClick = { selectedRace = race }
                )
            }
        }
    }
}

@Composable
private fun RaceHistoryCard(
    race: RaceRecordEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) StadiumSurfaceVariant else StadiumSurface
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) LaserOrange else StadiumBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("race_item_${race.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Camera Footage Badge
            Surface(
                color = if (isSelected) LaserOrange else ElectricCyan.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SlowMotionVideo,
                        contentDescription = null,
                        tint = if (isSelected) Color.Black else ElectricCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Race Title & Details (Item 6)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = race.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${race.athleteName} • ${race.eventName} • Lorong ${race.lane}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Cam 1 ✓", fontSize = 10.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                    Text(" • ", fontSize = 10.sp, color = TextMuted)
                    Text("Cam 2 Torso ✓", fontSize = 10.sp, color = LaserOrange, fontWeight = FontWeight.Bold)
                    Text(" • ", fontSize = 10.sp, color = TextMuted)
                    Text("Photo Finish ✓", fontSize = 10.sp, color = SpeedAmber, fontWeight = FontWeight.Bold)
                }
            }

            // Recorded Official Time Badge
            Surface(
                color = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) LaserOrange else ElectricCyan)
            ) {
                Text(
                    text = race.formattedTime,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = SpeedAmber,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
