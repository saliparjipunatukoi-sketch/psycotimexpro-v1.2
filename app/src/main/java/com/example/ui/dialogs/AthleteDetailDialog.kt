package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AchievementEntity
import com.example.data.model.Athlete
import com.example.data.model.PerformanceMetrics
import com.example.ui.theme.*

@Composable
fun AthleteDetailDialog(
    athlete: Athlete,
    achievements: List<AchievementEntity>,
    performanceMetrics: List<PerformanceMetrics> = emptyList(),
    onDismiss: () -> Unit,
    onDeleteAthlete: () -> Unit,
    onAddAchievement: (tournament: String, event: String, medal: String, record: String) -> Unit
) {
    var showAddAchievementDialog by remember { mutableStateOf(false) }
    var tournamentInput by remember { mutableStateOf("") }
    var eventInput by remember { mutableStateOf(athlete.specificEvent) }
    var medalInput by remember { mutableStateOf("Emas") }
    var recordInput by remember { mutableStateOf(athlete.pbValue) }

    val athleteMetrics = performanceMetrics.filter { it.athleteId == athlete.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profil Prestasi Atlit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDeleteAthlete) {
                    Icon(Icons.Default.Delete, contentDescription = "Padam Atlit", tint = FinishLineRed)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Athlete Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(StadiumSurfaceVariant)
                            .border(2.dp, ElectricCyan, CircleShape)
                    ) {
                        if (athlete.photoUri != null) {
                            AsyncImage(
                                model = athlete.photoUri,
                                contentDescription = athlete.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(36.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = athlete.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${athlete.specificEvent} • ${athlete.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricCyan
                        )
                        Text(
                            text = athlete.clubName,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // PB Highlight Card
                Surface(
                    color = StadiumSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (athlete.category == "Track") "PERSONAL BEST (SAAT)" else "PERSONAL BEST (JARAK/TINGGI)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Text(
                                text = "${athlete.pbValue} ${athlete.pbUnit}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = SpeedAmber
                            )
                        }

                        Surface(
                            color = if (athlete.feePaidStatus) SuccessGreen.copy(alpha = 0.2f) else SpeedAmber.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (athlete.feePaidStatus) "YURAN LUNAS" else "YURAN TERTUNGGAK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (athlete.feePaidStatus) SuccessGreen else SpeedAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Local PerformanceMetrics / Stored PBs Section
                if (athleteMetrics.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Rekod Catatan PB & Metrik Tempatan (Room DB)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        athleteMetrics.take(3).forEach { metric ->
                            Surface(
                                color = StadiumSurfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${metric.eventName} • PB: ${metric.pbValue} ${metric.pbUnit}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Cadence: ${metric.cadenceSpM} spm • GCT: ${metric.groundContactTimeMs}ms • Torso: ${metric.torsoLeanAngleDeg}°",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    if (metric.isNewPb) {
                                        Surface(
                                            color = LaserOrange.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "PB BARU",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = LaserOrange,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // AI Sports Science Biomechanical & Running Gait Stats
                val latestMetric = athleteMetrics.firstOrNull()
                val displayCadence = latestMetric?.cadenceSpM ?: 258
                val displayGct = latestMetric?.groundContactTimeMs ?: 106
                val displayTorso = latestMetric?.torsoLeanAngleDeg ?: 14.5
                val displayStrideFreq = latestMetric?.strideFrequencyHz ?: (displayCadence / 60.0)

                Surface(
                    color = StadiumSurfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Analisis Sains Sukan & Gaya Larian (Gait)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Surface(
                                color = ElectricCyan.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "AI BIOMECHANICS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Kinematic Grid
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = Color.Black,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Cadence", fontSize = 9.sp, color = TextSecondary)
                                    Text("$displayCadence spm", fontSize = 13.sp, fontWeight = FontWeight.Black, color = ElectricCyan)
                                    Text("%.2f Hz".format(displayStrideFreq), fontSize = 8.sp, color = TextMuted)
                                }
                            }
                            Surface(
                                color = Color.Black,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Sentuhan GCT", fontSize = 9.sp, color = TextSecondary)
                                    Text("${displayGct} ms", fontSize = 13.sp, fontWeight = FontWeight.Black, color = LaserOrange)
                                    Text(if (displayGct <= 110) "Reaktif Elit" else "Sederhana", fontSize = 8.sp, color = SuccessGreen)
                                }
                            }
                            Surface(
                                color = Color.Black,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Sudut Torso", fontSize = 9.sp, color = TextSecondary)
                                    Text("%.1f°".format(displayTorso), fontSize = 13.sp, fontWeight = FontWeight.Black, color = FinishLineRed)
                                    Text("Condongan Garis", fontSize = 8.sp, color = TextMuted)
                                }
                            }
                        }

                        // Sports Science Coaching Feedback
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "💡 Pandangan Sains Sukan & Cadangan Jurulatih:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LaserOrange
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (displayGct <= 110)
                                        "Kadar sentuhan tanah ${displayGct}ms sangat efisien dengan fasa pecutan pantas. Irama langkah ${displayCadence} spm stabil pada 30 meter penamat."
                                    else
                                        "Lakukan latihan 'A-Skips' dan plyometric ankle bounce untuk memendekkan Ground Contact Time bawah 110ms bagi pecutan lebih meletup.",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                // Achievements List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pencapaian Kejohanan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    TextButton(onClick = { showAddAchievementDialog = true }) {
                        Text("+ Tambah", color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                val athleteAchievements = achievements.filter { it.athleteId == athlete.id }
                if (athleteAchievements.isEmpty()) {
                    Text("Belum ada pencapaian direkodkan.", fontSize = 11.sp, color = TextSecondary)
                } else {
                    athleteAchievements.forEach { ach ->
                        Surface(
                            color = StadiumSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(ach.tournamentName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("${ach.eventName} • Rekod: ${ach.resultRecord}", fontSize = 10.sp, color = TextSecondary)
                                }
                                Surface(
                                    color = when (ach.medal) {
                                        "Emas" -> PodiumGold.copy(alpha = 0.2f)
                                        "Perak" -> Color.LightGray.copy(alpha = 0.2f)
                                        else -> LaserOrange.copy(alpha = 0.2f)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = ach.medal,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (ach.medal) {
                                            "Emas" -> PodiumGold
                                            "Perak" -> Color.White
                                            else -> LaserOrange
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Add Achievement Mini Dialog
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
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)) {
                Text("Tutup", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = StadiumSurface
    )
}
