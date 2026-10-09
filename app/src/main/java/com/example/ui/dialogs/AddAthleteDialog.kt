package com.example.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Person
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
import com.example.data.model.AthleteEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAthleteDialog(
    onDismiss: () -> Unit,
    onSaveAthlete: (AthleteEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Track") } // "Track" or "Field"
    var specificEvent by remember { mutableStateOf("100m Sprint") }
    var pbValue by remember { mutableStateOf("") }
    var pbUnit by remember { mutableStateOf("Saat (s)") }
    var gender by remember { mutableStateOf("Lelaki") }
    var registrationFee by remember { mutableStateOf("50.0") }
    var feePaid by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }

    // Photo picker launcher (Android zero-permission photo picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri.toString()
        }
    }

    val trackEvents = listOf(
        "100m Sprint", "200m Sprint", "400m", "800m", "1500m",
        "110m Lari Berpagar", "400m Lari Berpagar", "4x100m Berganti-ganti"
    )

    val fieldEvents = listOf(
        "Lompat Jauh", "Lompat Kijang", "Lompat Tinggi", "Lompat Bergalah",
        "Lontar Peluru", "Lempar Cakera", "Merejam Lembing", "Baling Tukul Besi"
    )

    val currentEventsList = if (category == "Track") trackEvents else fieldEvents
    var eventDropdownExpanded by remember { mutableStateOf(false) }

    // Update units automatically when category changes (Item 9)
    LaunchedEffect(category) {
        if (category == "Track") {
            pbUnit = "Saat (s)"
            if (!trackEvents.contains(specificEvent)) specificEvent = trackEvents.first()
        } else {
            pbUnit = if (specificEvent.contains("Tinggi") || specificEvent.contains("Bergalah")) "Meter (Ketinggian)" else "Meter (Jarak)"
            if (!fieldEvents.contains(specificEvent)) specificEvent = fieldEvents.first()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pendaftaran Atlit Baharu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Photo upload from Local Storage (Item 10)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(StadiumSurfaceVariant)
                        .border(2.dp, ElectricCyan, CircleShape)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                ) {
                    if (selectedPhotoUri != null) {
                        AsyncImage(
                            model = selectedPhotoUri,
                            contentDescription = "Gambar Atlit",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = "Tambah Gambar",
                                tint = ElectricCyan,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                "Tambah Foto",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Athlete Full Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Penuh Atlit *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("athlete_name_input")
                )

                // Athlete Age
                OutlinedTextField(
                    value = age,
                    onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 2) age = it },
                    label = { Text("Umur (Tahun) *") },
                    placeholder = { Text("cth: 17") },
                    trailingIcon = { Text("tahun  ", color = TextSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("athlete_age_input")
                )

                // Category Selection: Track vs Field (Item 9)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = category == "Track",
                        onClick = { category = "Track" },
                        label = { Text("Acara Balapan (Track)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = category == "Field",
                        onClick = { category = "Field" },
                        label = { Text("Acara Padang (Field)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LaserOrange,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Specific Event Dropdown
                ExposedDropdownMenuBox(
                    expanded = eventDropdownExpanded,
                    onExpandedChange = { eventDropdownExpanded = !eventDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = specificEvent,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pilihan Acara Utama") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = eventDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = eventDropdownExpanded,
                        onDismissRequest = { eventDropdownExpanded = false }
                    ) {
                        currentEventsList.forEach { ev ->
                            DropdownMenuItem(
                                text = { Text(ev) },
                                onClick = {
                                    specificEvent = ev
                                    if (category == "Field") {
                                        pbUnit = if (ev.contains("Tinggi") || ev.contains("Bergalah")) "Meter (Ketinggian)" else "Meter (Jarak)"
                                    }
                                    eventDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // PB Input tailored by Category (Item 9)
                if (category == "Track") {
                    OutlinedTextField(
                        value = pbValue,
                        onValueChange = { pbValue = it },
                        label = { Text("Personal Best (PB) Dalam Saat (s) *") },
                        placeholder = { Text("cth: 10.45") },
                        trailingIcon = { Text("saat  ", color = ElectricCyan, fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("track_pb_input")
                    )
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = pbValue,
                            onValueChange = { pbValue = it },
                            label = { Text("Personal Best (PB) $pbUnit *") },
                            placeholder = { Text("cth: 7.35 atau 1.85") },
                            trailingIcon = { Text("meter  ", color = LaserOrange, fontWeight = FontWeight.Bold) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("field_pb_input")
                        )
                    }
                }

                // Gender & Fee
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = registrationFee,
                        onValueChange = { registrationFee = it },
                        label = { Text("Yuran Pendaftaran (RM)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = feePaid,
                            onCheckedChange = { feePaid = it }
                        )
                        Text(
                            text = if (feePaid) "Telah Bayar" else "Tertunggak",
                            fontSize = 12.sp,
                            color = if (feePaid) SuccessGreen else SpeedAmber
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan / Sasaran Atlit") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val fee = registrationFee.toDoubleOrNull() ?: 50.0
                        val athleteAge = age.toIntOrNull() ?: 16
                        val birthYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - athleteAge
                        val newAthlete = AthleteEntity(
                            name = name.trim(),
                            category = category,
                            specificEvent = specificEvent,
                            pbValue = if (pbValue.isNotBlank()) pbValue.trim() else if (category == "Track") "11.20" else "6.50",
                            pbUnit = pbUnit,
                            photoUri = selectedPhotoUri,
                            registrationFee = fee,
                            feePaidStatus = feePaid,
                            dateOfBirth = "$birthYear-01-01",
                            gender = gender,
                            notes = notes
                        )
                        onSaveAthlete(newAthlete)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                modifier = Modifier.testTag("confirm_add_athlete_button")
            ) {
                Text("Daftar Atlit", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondary)
            }
        },
        containerColor = StadiumSurface
    )
}
