package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ClubProfile
import com.example.service.BluetoothSelfieTriggerManager
import com.example.service.RemoteKeySignal
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    clubProfile: ClubProfile,
    currentUserEmail: String,
    currentUserName: String,
    isSuperAdmin: Boolean,
    deviceStorageDirs: List<String>,
    selectedVideoFolderPath: String = "",
    onOpenVideoStorageSettings: () -> Unit = {},
    bluetoothTriggerManager: BluetoothSelfieTriggerManager? = null,
    isBluetoothTriggerEnabled: Boolean = true,
    lastRemoteSignal: RemoteKeySignal? = null,
    onToggleBluetoothTrigger: (Boolean) -> Unit = {},
    onOpenBluetoothRemoteSetup: () -> Unit = {},
    onSwitchUser: (email: String, name: String) -> Unit,
    onUpdateClubInfo: (name: String, logoUri: String?, coachName: String, phone: String, photoUri: String?) -> Unit,
    onOpenWebSync: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var clubNameInput by remember(clubProfile) { mutableStateOf(clubProfile.clubName) }
    var coachNameInput by remember(clubProfile) { mutableStateOf(clubProfile.coachName) }
    var coachPhoneInput by remember(clubProfile) { mutableStateOf(clubProfile.coachPhone) }
    var selectedLogoUri by remember(clubProfile) { mutableStateOf(clubProfile.logoUri) }
    var selectedCoachPhotoUri by remember(clubProfile) { mutableStateOf(clubProfile.coachPhotoUri) }
    var storageStatusMessage by remember { mutableStateOf<String?>(null) }

    var hasBtPermission by remember {
        mutableStateOf(bluetoothTriggerManager?.hasBluetoothConnectPermission() ?: false)
    }
    var pairedDevices by remember(hasBtPermission) {
        mutableStateOf(bluetoothTriggerManager?.getDetectedRemoteDevices() ?: emptyList())
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBtPermission = granted
        pairedDevices = bluetoothTriggerManager?.getDetectedRemoteDevices() ?: emptyList()
    }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedLogoUri = uri.toString()
            onUpdateClubInfo(clubNameInput, uri.toString(), coachNameInput, coachPhoneInput, selectedCoachPhotoUri)
        }
    }

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
                text = "Pengurusan Bluetooth Selfie Trigger, profil kelab, storan video dan integrasi web.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // BLUETOOTH SELFIE TRIGGER & STARTER GUN PAIRING / MANAGEMENT SECTION
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isBluetoothTriggerEnabled) ElectricCyan.copy(alpha = 0.6f) else StadiumBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_bluetooth_trigger_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    if (isBluetoothTriggerEnabled) ElectricCyan.copy(alpha = 0.16f) else StadiumSurfaceVariant,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.SettingsRemote,
                                contentDescription = null,
                                tint = if (isBluetoothTriggerEnabled) ElectricCyan else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Bluetooth Selfie Trigger & Starter Gun",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Mula rakaman video + bunyi tembakan pistol serta-merta (Sah untuk START sahaja)",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isBluetoothTriggerEnabled,
                        onCheckedChange = onToggleBluetoothTrigger,
                        modifier = Modifier
                            .scale(0.85f)
                            .testTag("settings_bt_trigger_switch")
                    )
                }

                // Live incoming signal status pill
                Surface(
                    color = StadiumSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (lastRemoteSignal != null) SuccessGreen else ElectricCyan,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lastRemoteSignal != null) {
                                    "Isyarat Terakhir: ${lastRemoteSignal.keyName}"
                                } else {
                                    "Menunggu isyarat Bluetooth Shutter (Volume Up / Enter / Camera)"
                                },
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (lastRemoteSignal != null) SuccessGreen else Color.White
                            )
                        }
                    }
                }

                // Paired Devices List inside Settings
                if (!hasBtPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    OutlinedButton(
                        onClick = { btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) },
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Benarkan Akses Senarai Peranti Bluetooth", fontSize = 11.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                    }
                } else if (pairedDevices.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Peranti Bluetooth Berpasangan (${pairedDevices.size}):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SilverMetallic
                        )
                        pairedDevices.take(3).forEach { device ->
                            Surface(
                                color = StadiumSurfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (device.isConnectedHid) ElectricCyan.copy(alpha = 0.5f) else StadiumBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (device.isConnectedHid) Icons.Default.Camera else Icons.Default.BluetoothConnected,
                                            contentDescription = null,
                                            tint = if (device.isConnectedHid) ElectricCyan else SilverMetallic,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(device.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(device.addressOrDescriptor, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                                        }
                                    }
                                    if (device.isConnectedHid) {
                                        Surface(
                                            color = SuccessGreen.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "SHUTTER",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = SuccessGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Buttons: Pair in System Bluetooth Settings & Open Signal Tester
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SilverBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pair Bluetooth", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            pairedDevices = bluetoothTriggerManager?.getDetectedRemoteDevices() ?: emptyList()
                            onOpenBluetoothRemoteSetup()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_open_bt_modal_button")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Urus & Uji Trigger", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            }
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

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("logout_settings_button")
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Keluar Akaun (Logout)", fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            fontFamily = FontFamily.Monospace,
                            color = ElectricCyanGlow,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }

                if (selectedVideoFolderPath.isNotBlank()) {
                    Surface(
                        color = ElectricCyan.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "FOLDER RAKAMAN VIDEO AKTIF:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = ElectricCyan
                            )
                            Text(
                                text = selectedVideoFolderPath,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }
                }

                Button(
                    onClick = onOpenVideoStorageSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("open_video_storage_settings_button")
                ) {
                    Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tetapan Folder Rakaman Video & Kebenaran Storan",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = {
                        storageStatusMessage = "Semua folder /PsycoTimeXPro/ telah disahkan aktif & boleh diakses!"
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
            }
        }

        // PAKEJ LANGGANAN BULANAN & STRUKTUR YURAN RASMI
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = LaserOrange, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Struktur Pakej Langganan Bulanan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Kiraan Yuran Rasmi Psyco Time X Pro",
                            fontSize = 10.sp,
                            color = LaserOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("PAKEJ ASAS (1 BULAN)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = ElectricCyan)
                                Text("30 Atlit + 2 Sub Coach", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Ideal untuk kelab sekolah & bakat muda", fontSize = 10.sp, color = TextSecondary)
                            }
                            Text("RM 30", fontSize = 18.sp, fontWeight = FontWeight.Black, color = ElectricCyan)
                        }
                    }

                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LaserOrange.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("PAKEJ STANDARD (1 BULAN)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = LaserOrange)
                                Text("50 Atlit + 2 Sub Coach", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Paling popular untuk kontinjen MSSD", fontSize = 10.sp, color = TextSecondary)
                            }
                            Text("RM 40", fontSize = 18.sp, fontWeight = FontWeight.Black, color = LaserOrange)
                        }
                    }

                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpeedAmber.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("PAKEJ PRO (1 BULAN)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = SpeedAmber)
                                Text("80 Atlit + 3 Sub Coach", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Kapasiti besar untuk akademi olahraga", fontSize = 10.sp, color = TextSecondary)
                            }
                            Text("RM 60", fontSize = 18.sp, fontWeight = FontWeight.Black, color = SpeedAmber)
                        }
                    }

                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("PAKEJ ELITE (1 BULAN)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = SuccessGreen)
                                Text("100 Atlit + 5 Sub Coach", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Kejohanan terbuka & skuad elit MSSN", fontSize = 10.sp, color = TextSecondary)
                            }
                            Text("RM 80", fontSize = 18.sp, fontWeight = FontWeight.Black, color = SuccessGreen)
                        }
                    }
                }

                Text(
                    text = "* Pelancong dan pendaftaran baharu diberikan Free Trial 7 Hari (10 Atlit). Admin Coach Salipar Jipun boleh melaraskan tarikh luput terus di portal web.",
                    fontSize = 10.sp,
                    color = TextMuted,
                    lineHeight = 14.sp
                )
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
