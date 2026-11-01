package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.service.FolderValidationResult
import com.example.service.StorageManager
import com.example.ui.theme.*
import java.io.File

@Composable
fun VideoStorageSettingsScreen(
    storageManager: StorageManager,
    selectedVideoFolderPath: String,
    onUpdateSelectedVideoFolder: (String) -> FolderValidationResult,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    BackHandler {
        onNavigateBack()
    }

    var hasManageStoragePerm by remember {
        mutableStateOf(storageManager.hasManageExternalStoragePermission())
    }
    var folderPathInput by remember(selectedVideoFolderPath) {
        mutableStateOf(selectedVideoFolderPath)
    }
    var validationResult by remember {
        mutableStateOf<FolderValidationResult?>(
            storageManager.validateFolderPath(selectedVideoFolderPath)
        )
    }
    var availableFolders by remember {
        mutableStateOf(storageManager.getAvailableStorageFolders())
    }
    var showCreateSubfolderDialog by remember { mutableStateOf(false) }
    var newSubfolderName by remember { mutableStateOf("") }

    // Refresh permission and folder validation when returning from Android System Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasManageStoragePerm = storageManager.hasManageExternalStoragePermission()
                validationResult = storageManager.validateFolderPath(folderPathInput)
                availableFolders = storageManager.getAvailableStorageFolders()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Launcher for Android 11+ MANAGE_EXTERNAL_STORAGE settings screen
    val manageStorageSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasManageStoragePerm = storageManager.hasManageExternalStoragePermission()
        validationResult = storageManager.validateFolderPath(folderPathInput)
    }

    // Launcher for legacy runtime storage permissions (Android 10 and below)
    val legacyStoragePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasManageStoragePerm = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true ||
                storageManager.hasManageExternalStoragePermission()
        validationResult = storageManager.validateFolderPath(folderPathInput)
    }

    // Launcher for Android System Directory Picker (OpenDocumentTree)
    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}
            val resolvedPath = storageManager.resolveTreeUriToLocalPath(treeUri)
            folderPathInput = resolvedPath
            val result = onUpdateSelectedVideoFolder(resolvedPath)
            validationResult = result
            availableFolders = storageManager.getAvailableStorageFolders()
            if (result.isValid) {
                Toast.makeText(
                    context,
                    "Folder rakaman video disimpan: ${result.resolvedPath}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun requestManageExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                manageStorageSettingsLauncher.launch(intent)
            } catch (_: Exception) {
                try {
                    val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    manageStorageSettingsLauncher.launch(fallbackIntent)
                } catch (_: Exception) {
                    Toast.makeText(
                        context,
                        "Sila aktifkan kebenaran Storan dalam Tetapan Peranti.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            legacyStoragePermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TrackDarkNavy)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("video_storage_settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header with Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(StadiumSurface)
                    .testTag("storage_settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali ke Tetapan",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tetapan Storan Rakaman Video",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Pilih folder tempatan, sahkan laluan & kebenaran Manage External Storage",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // 1. MANAGE EXTERNAL STORAGE PERMISSION CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.dp,
                if (hasManageStoragePerm) SuccessGreen.copy(alpha = 0.6f) else LaserOrange.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (hasManageStoragePerm) Icons.Default.VerifiedUser else Icons.Default.Security,
                            contentDescription = null,
                            tint = if (hasManageStoragePerm) SuccessGreen else LaserOrange,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Manage External Storage",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Akses Storan Penuh untuk Rakaman Video Race",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Surface(
                        color = if (hasManageStoragePerm) SuccessGreen.copy(alpha = 0.2f) else FinishLineRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("manage_storage_permission_badge")
                    ) {
                        Text(
                            text = if (hasManageStoragePerm) "DIBENARKAN" else "DIPERLUKAN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (hasManageStoragePerm) SuccessGreen else FinishLineRed,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = if (hasManageStoragePerm) {
                        "Kebenaran 'Manage External Storage' telah aktif. Aplikasi boleh menyimpan rakaman video Cam 1 & Cam 2 ke mana-mana folder tempatan pada peranti anda."
                    } else {
                        "Benarkan akses 'Manage External Storage' (All Files Access) supaya rakaman video perlumbaan boleh disimpan terus ke folder pilihan anda di luar ruang dalaman aplikasi."
                    },
                    fontSize = 12.sp,
                    color = SilverMetallic
                )

                Button(
                    onClick = { requestManageExternalStorage() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasManageStoragePerm) StadiumSurfaceVariant else LaserOrange
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("request_manage_storage_button")
                ) {
                    Icon(
                        imageVector = if (hasManageStoragePerm) Icons.Default.CheckCircle else Icons.Default.FolderShared,
                        contentDescription = null,
                        tint = if (hasManageStoragePerm) SuccessGreen else Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasManageStoragePerm) {
                            "Kebenaran Manage External Storage Aktif (Urus)"
                        } else {
                            "Benarkan 'Manage External Storage'"
                        },
                        fontWeight = FontWeight.Black,
                        color = if (hasManageStoragePerm) Color.White else Color.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 2. LOCAL FOLDER PATH INPUT, PICKER & VALIDATION CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, StadiumBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VideoSettings,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Laluan Folder Rakaman Video",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = { showCreateSubfolderDialog = true },
                        border = BorderStroke(1.dp, ElectricCyan),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("create_subfolder_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Cipta Folder",
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Folder Baharu", fontSize = 11.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                    }
                }

                // Active saved folder display
                Surface(
                    color = StadiumSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "FOLDER AKTIF SEMASA:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = ElectricCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedVideoFolderPath,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            modifier = Modifier.testTag("active_video_folder_path_text")
                        )
                    }
                }

                // Editable local folder path field
                OutlinedTextField(
                    value = folderPathInput,
                    onValueChange = {
                        folderPathInput = it
                    },
                    label = { Text("Laluan Folder Tempatan (Local Folder Path)") },
                    placeholder = { Text("/storage/emulated/0/Movies/PsycoTimeXPro") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = ElectricCyan)
                    },
                    trailingIcon = {
                        if (folderPathInput.isNotEmpty()) {
                            IconButton(onClick = { folderPathInput = "" }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Kosongkan laluan",
                                    tint = SilverMetallic
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = StadiumBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = ElectricCyan,
                        unfocusedLabelColor = SilverMetallic
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_path_input")
                )

                // Browse Directory Picker & Validate Path Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { directoryPickerLauncher.launch(null) },
                        border = BorderStroke(1.dp, ElectricCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("browse_local_folder_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pilih Folder",
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            validationResult = storageManager.validateFolderPath(folderPathInput)
                        },
                        border = BorderStroke(1.dp, LaserOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("validate_folder_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FactCheck,
                            contentDescription = null,
                            tint = LaserOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sahkan Laluan",
                            color = LaserOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Validation Status Result Banner
                validationResult?.let { result ->
                    val statusColor = if (result.isValid) SuccessGreen else FinishLineRed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(statusColor.copy(alpha = 0.14f))
                            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                            .testTag("folder_validation_status"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (result.isValid) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (result.isValid) "LALUAN FOLDER SAH (VALID)" else "LALUAN TIDAK SAH (INVALID)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = statusColor
                            )
                            Text(
                                text = result.message,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // Save & Apply Validated Folder Button
                Button(
                    onClick = {
                        val result = onUpdateSelectedVideoFolder(folderPathInput)
                        validationResult = result
                        availableFolders = storageManager.getAvailableStorageFolders()
                        if (result.isValid) {
                            folderPathInput = result.resolvedPath
                            Toast.makeText(
                                context,
                                "Folder rakaman video berjaya disahkan & disimpan!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                result.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("save_video_folder_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sahkan & Simpan Folder Rakaman Video",
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 3. PRESET & EXISTING FOLDERS QUICK SELECT CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, StadiumBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pilihan Pantas Lokasi Folder Tempatan",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Tekan mana-mana folder di bawah untuk menguji dan menetapkan laluan rakaman video:",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                val presets = remember { storageManager.getPresetStorageLocations() }
                presets.forEachIndexed { index, (label, path) ->
                    val isSelected = selectedVideoFolderPath == path
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) ElectricCyan.copy(alpha = 0.15f) else StadiumSurfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isSelected) ElectricCyan else StadiumBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                folderPathInput = path
                                val res = onUpdateSelectedVideoFolder(path)
                                validationResult = res
                                availableFolders = storageManager.getAvailableStorageFolders()
                            }
                            .padding(12.dp)
                            .testTag("preset_folder_item_$index"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.FolderSpecial else Icons.Default.Folder,
                            contentDescription = null,
                            tint = if (isSelected) ElectricCyan else SilverMetallic,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) ElectricCyan else Color.White
                            )
                            Text(
                                text = path,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = SilverMetallic
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Aktif",
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (availableFolders.isNotEmpty()) {
                    HorizontalDivider(color = StadiumBorder, modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Subfolder Sedia Ada pada Peranti:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SilverMetallic
                    )
                    availableFolders.forEach { folder: File ->
                        val isSelected = selectedVideoFolderPath == folder.absolutePath
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(StadiumSurfaceVariant.copy(alpha = 0.6f))
                                .clickable {
                                    folderPathInput = folder.absolutePath
                                    validationResult = onUpdateSelectedVideoFolder(folder.absolutePath)
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SubdirectoryArrowRight,
                                contentDescription = null,
                                tint = if (isSelected) SuccessGreen else ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = folder.absolutePath,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                )
                            }
                            if (isSelected) {
                                Text(
                                    text = "AKTIF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create New Subfolder Dialog
    if (showCreateSubfolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateSubfolderDialog = false },
            containerColor = StadiumSurface,
            title = {
                Text(
                    text = "Cipta Folder Rakaman Baharu",
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Subfolder baharu akan dicipta di dalam folder semasa dan ditetapkan sebagai lokasi simpanan video:",
                        fontSize = 12.sp,
                        color = SilverMetallic
                    )
                    OutlinedTextField(
                        value = newSubfolderName,
                        onValueChange = { newSubfolderName = it },
                        label = { Text("Nama Folder (cth: Kejohanan_MSSD_2026)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_storage_subfolder_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubfolderName.isNotBlank()) {
                            val created = storageManager.createCustomFolder(newSubfolderName)
                            folderPathInput = created.absolutePath
                            validationResult = onUpdateSelectedVideoFolder(created.absolutePath)
                            availableFolders = storageManager.getAvailableStorageFolders()
                            newSubfolderName = ""
                            showCreateSubfolderDialog = false
                            Toast.makeText(
                                context,
                                "Folder '${created.name}' berjaya dicipta & diaktifkan!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Text("Cipta & Pilih", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateSubfolderDialog = false }) {
                    Text("Batal", color = SilverMetallic)
                }
            }
        )
    }
}
