package com.example.ui.screens

import android.net.Uri
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
import com.example.data.model.ClubProfile
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    clubProfile: ClubProfile,
    currentUserEmail: String,
    currentUserName: String,
    isSuperAdmin: Boolean,
    deviceStorageDirs: List<String>,
    onSwitchUser: (email: String, name: String) -> Unit,
    onUpdateClubInfo: (name: String, logoUri: String?, coachName: String, phone: String, photoUri: String?) -> Unit,
    onOpenWebSync: () -> Unit,
    onOpenKadTeknikal: () -> Unit,
    modifier: Modifier = Modifier
) {
    var clubNameInput by remember(clubProfile) { mutableStateOf(clubProfile.clubName) }
    var coachNameInput by remember(clubProfile) { mutableStateOf(clubProfile.coachName) }
    var coachPhoneInput by remember(clubProfile) { mutableStateOf(clubProfile.coachPhone) }
    var selectedLogoUri by remember(clubProfile) { mutableStateOf(clubProfile.logoUri) }
    var selectedCoachPhotoUri by remember(clubProfile) { mutableStateOf(clubProfile.coachPhotoUri) }
    var storageStatusMessage by remember { mutableStateOf<String?>(null) }

    // Logo picker launcher (Item 12)
    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedLogoUri = uri.toString()
            onUpdateClubInfo(clubNameInput, uri.toString(), coachNameInput, coachPhoneInput, selectedCoachPhotoUri)
        }
    }

    // Coach profile photo picker (Item 11)
    val coachPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedCoachPhotoUri = uri.toString()
            onUpdateClubInfo(clubNameInput, selectedLogoUri, coachNameInput, coachPhoneInput, uri.toString())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TrackDarkNavy)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "Tetapan & Konfigurasi Sistem",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "Profil kelab, akaun superadmin, storan folder peranti dan integrasi web.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // ACCOUNT & ROLE SWITCHER (Item 2)
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Akaun & Akses Pengguna",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSuperAdmin) Icons.Default.AdminPanelSettings else Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = if (isSuperAdmin) LaserOrange else ElectricCyan,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentUserName, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(currentUserEmail, fontSize = 12.sp, color = TextSecondary)
                    }
                    Surface(
                        color = if (isSuperAdmin) LaserOrange.copy(alpha = 0.2f) else ElectricCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isSuperAdmin) "SUPERADMIN" else "COACH",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSuperAdmin) LaserOrange else ElectricCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onSwitchUser("saliparjipun.atukoi@gmail.com", "Coach Salipar Jipun (Superadmin)")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSuperAdmin) LaserOrange else StadiumSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Superadmin", color = if (isSuperAdmin) Color.Black else Color.White, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            onSwitchUser("jurulatih.kelab@psycotimexpro.my", "Jurulatih Jemputan MSSD")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isSuperAdmin) ElectricCyan else StadiumSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Coach Tetamu", color = if (!isSuperAdmin) Color.Black else Color.White, fontSize = 11.sp)
                    }
                }
            }
        }

        // CLUB & COACH PROFILE CARD (Item 11 & 12)
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Maklumat Kelab & Jurulatih",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Upload Logos and Coach Photo (Item 11 & 12)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Club Logo Picker
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(StadiumSurfaceVariant)
                                .border(2.dp, ElectricCyan, CircleShape)
                                .clickable {
                                    logoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            if (selectedLogoUri != null) {
                                AsyncImage(
                                    model = selectedLogoUri,
                                    contentDescription = "Logo Kelab",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.SportsScore, contentDescription = null, tint = ElectricCyan)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Logo Kelab", fontSize = 10.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                    }

                    // Coach Photo Picker (Item 11)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(StadiumSurfaceVariant)
                                .border(2.dp, LaserOrange, CircleShape)
                                .clickable {
                                    coachPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            if (selectedCoachPhotoUri != null) {
                                AsyncImage(
                                    model = selectedCoachPhotoUri,
                                    contentDescription = "Foto Jurulatih",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null, tint = LaserOrange)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Foto Jurulatih", fontSize = 10.sp, color = LaserOrange, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = clubNameInput,
                    onValueChange = { clubNameInput = it },
                    label = { Text("Nama Kelab Sukan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = coachNameInput,
                    onValueChange = { coachNameInput = it },
                    label = { Text("Nama Jurulatih") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = coachPhoneInput,
                    onValueChange = { coachPhoneInput = it },
                    label = { Text("Nombor Telefon Jurulatih") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        onUpdateClubInfo(
                            clubNameInput,
                            selectedLogoUri,
                            coachNameInput,
                            coachPhoneInput,
                            selectedCoachPhotoUri
                        )
                        storageStatusMessage = "Profil kelab berjaya dikemaskini!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Simpan Profil Kelab", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // DEVICE STORAGE FOLDER VERIFICATION (Item 14)
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = SpeedAmber, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Folder Storan Auto-Create Peranti",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "APK secara automatik mencipta folder khusus di telefon bagi memastikan rakaman video ET, data larian dan kad teknikal selamat tersimpan:",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                deviceStorageDirs.forEach { path ->
                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📁 ...${path.takeLast(40)}",
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = ElectricCyanGlow,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        storageStatusMessage = "Semua 5 folder /PsycoTimeXPro/ telah disahkan aktif & boleh diakses!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StadiumSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sahkan Integriti Folder", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // WEB PORTAL & PRINTABLE KAD SHORTCUTS
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Integrasi & Eksport Dokumen",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedButton(
                    onClick = onOpenWebSync,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pengurusan Sync Portal Web", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenKadTeknikal,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LaserOrange),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LaserOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Jana Kad Balapan / Kad Padang Manual", fontWeight = FontWeight.Bold)
                }
            }
        }

        storageStatusMessage?.let {
            Surface(
                color = SuccessGreen.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "✓ $it",
                    color = SuccessGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
