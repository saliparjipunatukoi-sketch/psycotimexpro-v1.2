package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Athlete
import com.example.ui.theme.*

/**
 * Full-screen Jetpack Compose form to register a new athlete into the Room database,
 * including dedicated fields for name, age, and specialty (Track / Field & specific events),
 * alongside PB values, club information, photo upload, and coach credentials.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterAthleteScreen(
    onSaveAthlete: (Athlete) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Handle device hardware / predictive back press
    BackHandler(onBack = onNavigateBack)

    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var specialty by remember { mutableStateOf("100m Sprint") }
    var category by remember { mutableStateOf("Track") } // "Track" or "Field"
    var pbValue by remember { mutableStateOf("") }
    var pbUnit by remember { mutableStateOf("Saat (s)") }
    var fieldPbDropdownExpanded by remember { mutableStateOf(false) }
    var gender by remember { mutableStateOf("Lelaki") }
    var clubName by remember { mutableStateOf("Psyco Track & Field Elite") }
    var registrationFee by remember { mutableStateOf("50.0") }
    var feePaid by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }
    var eventDropdownExpanded by remember { mutableStateOf(false) }

    // Validation state
    var showErrors by remember { mutableStateOf(false) }
    val isNameValid = name.isNotBlank()
    val isAgeValid = age.toIntOrNull()?.let { it in 5..99 } ?: false

    // Photo picker launcher (Zero-permission Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri.toString()
        }
    }

    val trackSpecialties = listOf(
        "100m Sprint", "200m Sprint", "400m", "800m", "1500m",
        "110m Lari Berpagar", "400m Lari Berpagar", "4x100m Berganti-ganti"
    )

    val fieldSpecialties = listOf(
        "Lompat Jauh", "Lompat Kijang", "Lompat Tinggi", "Lompat Bergalah",
        "Lontar Peluru", "Lempar Cakera", "Merejam Lembing", "Baling Tukul Besi"
    )

    val currentSpecialties = if (category == "Track") trackSpecialties else fieldSpecialties

    // Auto-update specialty and PB unit when category toggles
    LaunchedEffect(category) {
        if (category == "Track") {
            pbUnit = "Saat (s)"
            if (!trackSpecialties.contains(specialty)) specialty = trackSpecialties.first()
        } else {
            pbUnit = if (specialty.contains("Tinggi") || specialty.contains("Bergalah")) {
                "Meter (Ketinggian)"
            } else {
                "Meter (Jarak)"
            }
            if (!fieldSpecialties.contains(specialty)) specialty = fieldSpecialties.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Daftar Atlit Baharu",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Simpan rekod ke pangkalan data Room",
                            fontSize = 11.sp,
                            color = ElectricCyanGlow
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_register_athlete")
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StadiumSurface)
            )
        },
        bottomBar = {
            Surface(
                color = StadiumSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_cancel_registration"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            if (isNameValid && isAgeValid) {
                                val birthYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - (age.toIntOrNull() ?: 16)
                                val calculatedDob = "$birthYear-01-01"
                                val finalFee = registrationFee.toDoubleOrNull() ?: 50.0

                                val newAthlete = Athlete(
                                    name = name.trim(),
                                    category = category,
                                    specificEvent = specialty,
                                    pbValue = if (pbValue.isNotBlank()) pbValue.trim() else if (category == "Track") "11.20" else "6.50",
                                    pbUnit = pbUnit,
                                    photoUri = selectedPhotoUri,
                                    clubName = clubName.trim(),
                                    coachEmail = "saliparjipun.atukoi@gmail.com",
                                    registrationFee = finalFee,
                                    feePaidStatus = feePaid,
                                    dateOfBirth = calculatedDob,
                                    gender = gender,
                                    notes = notes.trim()
                                )
                                onSaveAthlete(newAthlete)
                            } else {
                                showErrors = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        modifier = Modifier
                            .weight(2f)
                            .height(50.dp)
                            .testTag("btn_save_athlete_to_room")
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Simpan & Sync ke Web",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        },
        containerColor = TrackDarkNavy,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricCyan.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Default.AppRegistration,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Borang Pendaftaran Atlit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Sila lengkapkan maklumat Nama, Umur, dan Acara Khusus (Specialty).",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Photo Upload
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(StadiumSurfaceVariant)
                    .border(2.dp, ElectricCyan, CircleShape)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("register_athlete_photo_picker")
            ) {
                if (selectedPhotoUri != null) {
                    AsyncImage(
                        model = selectedPhotoUri,
                        contentDescription = "Foto Atlit",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            contentDescription = "Pilih Foto",
                            tint = ElectricCyan,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tambah Foto",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Section: Maklumat Asas Atlit
            Card(
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "MAKLUMAT ASAS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = ElectricCyan,
                        letterSpacing = 1.sp
                    )

                    // 1. Field: Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Penuh Atlit *") },
                        placeholder = { Text("cth: Muhammad Azeem Fahmi") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = ElectricCyan)
                        },
                        isError = showErrors && !isNameValid,
                        supportingText = {
                            if (showErrors && !isNameValid) {
                                Text("Nama atlit wajib diisi", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_athlete_name")
                    )

                    // 2. Field: Age
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = age,
                            onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 2) age = it },
                            label = { Text("Umur (Tahun) *") },
                            placeholder = { Text("cth: 17") },
                            leadingIcon = {
                                Icon(Icons.Default.Cake, contentDescription = null, tint = ElectricCyan)
                            },
                            trailingIcon = {
                                Text("thn ", color = TextSecondary, fontSize = 12.sp)
                            },
                            isError = showErrors && !isAgeValid,
                            supportingText = {
                                if (showErrors && !isAgeValid) {
                                    Text("Umur sah (5-99)", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_athlete_age")
                        )

                        // Gender Selection
                        OutlinedTextField(
                            value = gender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Jantina") },
                            trailingIcon = {
                                TextButton(onClick = {
                                    gender = if (gender == "Lelaki") "Perempuan" else "Lelaki"
                                }) {
                                    Text(
                                        text = if (gender == "Lelaki") "Tukar ♀" else "Tukar ♂",
                                        color = LaserOrange,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_athlete_gender")
                        )
                    }

                    // Club Name
                    OutlinedTextField(
                        value = clubName,
                        onValueChange = { clubName = it },
                        label = { Text("Nama Kelab / Pasukan") },
                        leadingIcon = {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = TextSecondary)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_athlete_club")
                    )
                }
            }

            // Section: Kategori & Acara Khusus (Specialty)
            Card(
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "KEPAKARAN & ACARA (SPECIALTY)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = LaserOrange,
                        letterSpacing = 1.sp
                    )

                    // Category Selector: Track vs Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = category == "Track",
                            onClick = { category = "Track" },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DirectionsRun, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Balapan (Track)", fontWeight = FontWeight.Bold)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_category_track")
                        )

                        FilterChip(
                            selected = category == "Field",
                            onClick = { category = "Field" },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccessibilityNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Padang (Field)", fontWeight = FontWeight.Bold)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LaserOrange,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_category_field")
                        )
                    }

                    // 3. Field: Specialty (Specific Event Dropdown)
                    ExposedDropdownMenuBox(
                        expanded = eventDropdownExpanded,
                        onExpandedChange = { eventDropdownExpanded = !eventDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = specialty,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Acara Khusus / Kepakaran (Specialty) *") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (category == "Track") Icons.Default.DirectionsRun else Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = if (category == "Track") ElectricCyan else LaserOrange
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = eventDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("input_athlete_specialty")
                        )

                        ExposedDropdownMenu(
                            expanded = eventDropdownExpanded,
                            onDismissRequest = { eventDropdownExpanded = false }
                        ) {
                            currentSpecialties.forEach { ev ->
                                DropdownMenuItem(
                                    text = { Text(ev) },
                                    onClick = {
                                        specialty = ev
                                        if (category == "Field") {
                                            pbUnit = if (ev.contains("Tinggi") || ev.contains("Bergalah")) {
                                                "Meter (Ketinggian)"
                                            } else {
                                                "Meter (Jarak)"
                                            }
                                        }
                                        eventDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // PB (Personal Best) Field adapted according to Specialty
                    if (category == "Track") {
                        OutlinedTextField(
                            value = pbValue,
                            onValueChange = { pbValue = it },
                            label = { Text("Personal Best (PB) Dalam Saat") },
                            placeholder = { Text("cth: 10.45") },
                            leadingIcon = {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = ElectricCyan)
                            },
                            trailingIcon = {
                                Text("saat  ", color = ElectricCyan, fontWeight = FontWeight.Bold)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_athlete_pb_track")
                        )
                    } else {
                        val fieldUnitOptions = listOf(
                            "Meter (Jarak - Lompat Jauh/Kijang)",
                            "Meter (Ketinggian - Lompat Tinggi/Bergalah)",
                            "Meter (Balingan - Lontar Peluru/Lembing/Cakera)",
                            "Mata (Points - Dekatlon/Heptatlon)"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExposedDropdownMenuBox(
                                expanded = fieldPbDropdownExpanded,
                                onExpandedChange = { fieldPbDropdownExpanded = !fieldPbDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = pbUnit,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Pilihan Unit / Jenis PB Padang") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Straighten, contentDescription = null, tint = LaserOrange)
                                    },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fieldPbDropdownExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                        .testTag("field_pb_unit_dropdown")
                                )

                                ExposedDropdownMenu(
                                    expanded = fieldPbDropdownExpanded,
                                    onDismissRequest = { fieldPbDropdownExpanded = false }
                                ) {
                                    fieldUnitOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt) },
                                            onClick = {
                                                pbUnit = opt
                                                fieldPbDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = pbValue,
                                onValueChange = { pbValue = it },
                                label = { Text("Nilai Personal Best (PB): $pbUnit") },
                                placeholder = { Text("cth: 7.35 atau 2.05 atau 17.50") },
                                leadingIcon = {
                                    Icon(Icons.Default.Score, contentDescription = null, tint = LaserOrange)
                                },
                                trailingIcon = {
                                    Text(
                                        if (pbUnit.contains("Mata")) "pts  " else "meter  ",
                                        color = LaserOrange,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_athlete_pb_field")
                            )
                        }
                    }
                }
            }

            // Section: Yuran Pendaftaran & Catatan
            Card(
                colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "STATUS YURAN & SASARAN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.LightGray,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = registrationFee,
                            onValueChange = { registrationFee = it },
                            label = { Text("Yuran Pendaftaran (RM)") },
                            leadingIcon = {
                                Icon(Icons.Default.AttachMoney, contentDescription = null, tint = TextSecondary)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_athlete_fee")
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { feePaid = !feePaid }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = feePaid,
                                onCheckedChange = { feePaid = it }
                            )
                            Text(
                                text = if (feePaid) "Telah Bayar" else "Tertunggak",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (feePaid) SuccessGreen else SpeedAmber
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan / Sasaran Kejohanan") },
                        placeholder = { Text("Sasaran SUKMA / MSSM / Kejohanan Terbuka") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_athlete_notes")
                    )
                }
            }
        }
    }
}
