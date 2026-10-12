package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AthleteEntity
import com.example.ui.theme.*

import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveRaceDialog(
    suggestedTitle: String,
    elapsedMillis: Long,
    athletes: List<AthleteEntity>,
    availableFolders: List<File> = emptyList(),
    onCreateFolder: ((String) -> File)? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, event: String, athleteName: String, athleteId: Long?, lane: Int, wind: String, notes: String, targetFolder: File?) -> Unit
) {
    var title by remember { mutableStateOf(suggestedTitle) }
    var selectedEvent by remember { mutableStateOf("100m Sprint") }
    var selectedAthleteName by remember {
        mutableStateOf(athletes.firstOrNull()?.name ?: "Atlit Terbuka")
    }
    var selectedAthleteId by remember {
        mutableStateOf(athletes.firstOrNull()?.id)
    }
    var lane by remember { mutableStateOf("4") }
    var wind by remember { mutableStateOf("+0.8 m/s") }
    var notes by remember { mutableStateOf("") }
    
    // Folder selection & creation states
    var selectedFolder by remember { mutableStateOf(availableFolders.firstOrNull()) }
    var folderExpanded by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }
    var foldersList by remember(availableFolders) { mutableStateOf(availableFolders) }

    val hours = (elapsedMillis / 3600000).toInt()
    val minutes = ((elapsedMillis % 3600000) / 60000).toInt()
    val seconds = ((elapsedMillis % 60000) / 1000).toInt()
    val millis = (elapsedMillis % 1000).toInt()
    val formattedTime = String.format("%d:%02d:%02d:%03d", hours, minutes, seconds, millis)

    val eventsList = listOf(
        "100m Sprint", "200m Sprint", "400m", "110m Lari Berpagar",
        "400m Lari Berpagar", "800m", "1500m", "4x100m Berganti-ganti"
    )
    var eventExpanded by remember { mutableStateOf(false) }
    var athleteExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Simpan Keputusan & Rakaman Race",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Masa Rasmi: $formattedTime (Cam 1 & Cam 2)",
                    color = LaserOrange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
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
                Text(
                    text = "Sila tetapkan tajuk dan maklumat larian. Rakaman Cam 1 dan Cam 2 akan disimpan secara automatik ke Tab Replay & Rakaman.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // Race Title Input (Item 6)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tajuk Race (cth: Race 1, Saringan 1)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("race_title_input")
                )

                // Event Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = eventExpanded,
                    onExpandedChange = { eventExpanded = !eventExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedEvent,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Acara Larian") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = eventExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = eventExpanded,
                        onDismissRequest = { eventExpanded = false }
                    ) {
                        eventsList.forEach { ev ->
                            DropdownMenuItem(
                                text = { Text(ev) },
                                onClick = {
                                    selectedEvent = ev
                                    eventExpanded = false
                                }
                            )
                        }
                    }
                }

                // Athlete Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = athleteExpanded,
                    onExpandedChange = { athleteExpanded = !athleteExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAthleteName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Nama Atlit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = athleteExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = athleteExpanded,
                        onDismissRequest = { athleteExpanded = false }
                    ) {
                        athletes.forEach { ath ->
                            DropdownMenuItem(
                                text = { Text("${ath.name} (${ath.specificEvent})") },
                                onClick = {
                                    selectedAthleteName = ath.name
                                    selectedAthleteId = ath.id
                                    athleteExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Atlit Terbuka / Tetamu") },
                            onClick = {
                                selectedAthleteName = "Atlit Terbuka"
                                selectedAthleteId = null
                                athleteExpanded = false
                            }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = lane,
                        onValueChange = { lane = it },
                        label = { Text("Lorong") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = wind,
                        onValueChange = { wind = it },
                        label = { Text("Angin") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // FOLDER STORAGE SELECTION (PILIHAN FOLDER SIMPANAN LOKAL)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Folder Storan Simpanan Video & Data:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = folderExpanded,
                            onExpandedChange = { folderExpanded = !folderExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedFolder?.name ?: "Videos (Default)",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = folderExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FinishLineRed,
                                    unfocusedBorderColor = StadiumBorder
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = folderExpanded,
                                onDismissRequest = { folderExpanded = false }
                            ) {
                                foldersList.forEach { f ->
                                    DropdownMenuItem(
                                        text = { Text(f.name) },
                                        onClick = {
                                            selectedFolder = f
                                            folderExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Butang Tambah / Cipta Folder Baharu
                        Button(
                            onClick = { showCreateFolderDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = StadiumSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = FinishLineRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Folder", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = "Lokasi: ${selectedFolder?.absolutePath ?: "Storan Dalaman /PsycoTimeXPro/Videos"}",
                        fontSize = 9.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Teknikal (Pilihan)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val laneNum = lane.toIntOrNull() ?: 4
                    onSave(title, selectedEvent, selectedAthleteName, selectedAthleteId, laneNum, wind, notes, selectedFolder)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                modifier = Modifier.testTag("confirm_save_race_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan Race", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondary)
            }
        },
        containerColor = StadiumSurface
    )

    // Dialog Cipta Folder Baharu
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = {
                Text("Cipta Folder Storan Baharu", fontWeight = FontWeight.Black, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Masukkan nama folder untuk mengasingkan rakaman video perlumbaan ini:", color = TextSecondary, fontSize = 12.sp)
                    OutlinedTextField(
                        value = newFolderNameInput,
                        onValueChange = { newFolderNameInput = it },
                        label = { Text("Nama Folder (cth: MSSD_2026, Ujian_Masa)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderNameInput.isNotBlank()) {
                            val created = onCreateFolder?.invoke(newFolderNameInput.trim())
                            if (created != null) {
                                foldersList = foldersList + created
                                selectedFolder = created
                            }
                            newFolderNameInput = ""
                            showCreateFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed)
                ) {
                    Text("Cipta Folder", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Batal", color = SilverMetallic)
                }
            }
        )
    }
}
