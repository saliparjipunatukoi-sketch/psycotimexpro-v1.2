package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AthleteEntity
import com.example.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KadTeknikalDialog(
    athletes: List<AthleteEntity>,
    onDismiss: () -> Unit,
    onSaveKadToStorage: (type: String, content: String) -> String
) {
    var selectedKadType by remember { mutableStateOf("Balapan") } // "Balapan" vs "Padang"
    var tournamentName by remember { mutableStateOf("KEJOHANAN OLAHRAGA PSYCO TIME X PRO 2026") }
    var eventName by remember { mutableStateOf("100m Lelaki Terbuka") }
    var heatOrFlight by remember { mutableStateOf("Saringan 1 / Akhir") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Janaan Kad Padang / Kad Balapan Rasmi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Format Manual Pengadil & Urusetia Teknikal",
                    color = SpeedAmber,
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Toggle Type
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedKadType == "Balapan",
                        onClick = {
                            selectedKadType = "Balapan"
                            eventName = "100m Lelaki Terbuka"
                        },
                        label = { Text("Kad Balapan (Track)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedKadType == "Padang",
                        onClick = {
                            selectedKadType = "Padang"
                            eventName = "Lompat Jauh Lelaki"
                        },
                        label = { Text("Kad Padang (Field)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LaserOrange,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = tournamentName,
                    onValueChange = { tournamentName = it },
                    label = { Text("Nama Kejohanan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = eventName,
                        onValueChange = { eventName = it },
                        label = { Text("Acara") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f)
                    )
                    OutlinedTextField(
                        value = heatOrFlight,
                        onValueChange = { heatOrFlight = it },
                        label = { Text("Pusingan") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Official Printable Card Sheet Preview
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = tournamentName.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "BORANG HAKIM TEKNIKAL: KAD ${selectedKadType.uppercase()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (selectedKadType == "Balapan") Color(0xFF0369A1) else Color(0xFFC2410C)
                        )
                        Text(
                            text = "ACARA: $eventName | $heatOrFlight",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                        Divider(color = Color.Black, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // Table layout based on Card Type
                        if (selectedKadType == "Balapan") {
                            Text(
                                text = "LOR | BIB | NAMA PESERTA | MASA ET | KDKN",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Divider(color = Color.LightGray, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
                            val displayAthletes = athletes.take(6).ifEmpty {
                                listOf(AthleteEntity(name = "Contoh Atlit", category = "Track", specificEvent = "100m", pbValue = "10.8", pbUnit = "s"))
                            }
                            displayAthletes.forEachIndexed { idx, ath ->
                                val lane = idx + 2
                                val bib = "B-${100 + idx}"
                                val shortName = if (ath.name.length > 14) ath.name.take(12) + ".." else ath.name
                                Text(
                                    text = String.format(" %d  | %s | %-14s | [   .  ] | [  ]", lane, bib, shortName),
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Black
                                )
                            }
                        } else {
                            Text(
                                text = "GIL | BIB | NAMA | C1 | C2 | C3 | TERBAIK | KDKN",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Divider(color = Color.LightGray, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
                            val displayAthletes = athletes.take(5).ifEmpty {
                                listOf(AthleteEntity(name = "Contoh Atlit Padang", category = "Field", specificEvent = "Lompat Jauh", pbValue = "7.20", pbUnit = "m"))
                            }
                            displayAthletes.forEachIndexed { idx, ath ->
                                val bib = "P-${200 + idx}"
                                val shortName = if (ath.name.length > 10) ath.name.take(8) + ".." else ath.name
                                Text(
                                    text = String.format(" %d  | %s | %-8s | [  ] | [  ] | [  ] | [    ] | [  ]", idx + 1, bib, shortName),
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("T.Tangan Pelepas: ________", fontSize = 8.sp, color = Color.DarkGray)
                            Text("T.Tangan Ketua Hakim: ________", fontSize = 8.sp, color = Color.DarkGray)
                        }
                    }
                }

                statusMessage?.let {
                    Text(text = "✓ $it", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rawKad = """
                        $tournamentName
                        BORANG TEKNIKAL RESMI: KAD ${selectedKadType.uppercase()}
                        ACARA: $eventName ($heatOrFlight)
                        Tarikh Janaan: ${System.currentTimeMillis()}
                        Dikeluarkan oleh: Psyco Time X Pro Electronic Timing System
                    """.trimIndent()
                    val savedPath = onSaveKadToStorage(selectedKadType, rawKad)
                    statusMessage = "Kad disimpan ke peranti: $savedPath"
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                modifier = Modifier.testTag("download_kad_teknikal_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Download Kad Teknikal", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = TextSecondary)
            }
        },
        containerColor = StadiumSurface
    )
}
