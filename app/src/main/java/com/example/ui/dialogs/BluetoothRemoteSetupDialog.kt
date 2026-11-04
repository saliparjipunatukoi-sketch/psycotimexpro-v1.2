package com.example.ui.dialogs

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.KeyEvent
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.BleM3StartMode
import com.example.service.BluetoothRemoteDeviceInfo
import com.example.service.BluetoothSelfieTriggerManager
import com.example.service.ElectronicStarterGunAudio
import com.example.service.RemoteKeySignal
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dialog for connecting, syncing, and calibrating the BLE-M3 Bluetooth Remote
 * (and AB Shutter 3 / Bluetooth Selfie Triggers) with dedicated "Set to Click Start (5 Sec Press)" synchronization.
 */
@Composable
fun BluetoothRemoteSetupDialog(
    triggerManager: BluetoothSelfieTriggerManager,
    isBluetoothTriggerEnabled: Boolean,
    lastRemoteSignal: RemoteKeySignal?,
    onToggleBluetoothTrigger: (Boolean) -> Unit,
    onTestFireStarterGun: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var detectedDevices by remember { mutableStateOf(triggerManager.getDetectedRemoteDevices()) }
    var isBtOn by remember { mutableStateOf(triggerManager.isBluetoothEnabled()) }
    var selectedStartMode by remember { mutableStateOf(triggerManager.getStartMode()) }
    var isPointerInterceptOn by remember { mutableStateOf(triggerManager.isBlePointerInterceptEnabled()) }

    // 5-Second BLE-M3 Sync / Set to Click Start State
    var is5SecSyncRunning by remember { mutableStateOf(false) }
    var syncRemainingTenths by remember { mutableStateOf(50) } // 5.0 seconds = 50 tenths
    var syncCompletedMessage by remember {
        mutableStateOf<String?>(
            if (triggerManager.isBleM3Synced()) {
                "BLE-M3 Disync: ${BluetoothSelfieTriggerManager.getReadableKeyName(triggerManager.getSyncedKeyCode())} (${triggerManager.getSyncedDeviceName()})"
            } else null
        )
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        isBtOn = triggerManager.isBluetoothEnabled()
        detectedDevices = triggerManager.getDetectedRemoteDevices()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !triggerManager.hasBluetoothConnectPermission()) {
            btPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        } else {
            isBtOn = triggerManager.isBluetoothEnabled()
            detectedDevices = triggerManager.getDetectedRemoteDevices()
        }
    }

    // Watch incoming signal during 5-sec sync window
    LaunchedEffect(lastRemoteSignal) {
        if (lastRemoteSignal != null) {
            triggerManager.saveBleM3SyncConfiguration(lastRemoteSignal.keyCode, lastRemoteSignal.deviceName)
            syncCompletedMessage = "✓ BLE-M3 Berjaya Disync ke Click Start! (${lastRemoteSignal.keyName})"
            if (is5SecSyncRunning) {
                is5SecSyncRunning = false
            }
        }
    }

    // 5-Second Press Sync Timer Loop
    LaunchedEffect(is5SecSyncRunning) {
        if (is5SecSyncRunning) {
            syncRemainingTenths = 50
            while (syncRemainingTenths > 0 && is5SecSyncRunning) {
                delay(100)
                syncRemainingTenths -= 1
            }
            if (is5SecSyncRunning) {
                is5SecSyncRunning = false
                val targetCode = lastRemoteSignal?.keyCode ?: KeyEvent.KEYCODE_VOLUME_UP
                val devName = detectedDevices.firstOrNull { it.isBleM3Model }?.name
                    ?: lastRemoteSignal?.deviceName
                    ?: "BLE-M3 Remote"
                triggerManager.saveBleM3SyncConfiguration(targetCode, devName)
                onToggleBluetoothTrigger(true)
                syncCompletedMessage = "✓ BLE-M3 Set to Click Start (5 Sec Press) Disahkan & Dikunci!"
                ElectronicStarterGunAudio.playExplosiveStarterGunShot(coroutineScope)
                onTestFireStarterGun()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StadiumSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .background(ElectricCyan.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, ElectricCyan, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.BluetoothConnected,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Sync Bluetooth BLE-M3 & Starter Gun",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Set to Click Start (5 Sec Press) • Khas untuk BLE-M3 & Shutter",
                            fontSize = 10.sp,
                            color = ElectricCyan
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. DEDICATED BLE-M3 "SET TO CLICK START (5 SEC PRESS)" SYNC PANEL
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.5.dp,
                        if (is5SecSyncRunning) SpeedAmber else SuccessGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.TouchApp,
                                    contentDescription = null,
                                    tint = SpeedAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SYNC BLE-M3: SET TO CLICK START (5 SEC PRESS)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SpeedAmber
                                )
                            }
                            Surface(
                                color = SuccessGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "BLE-M3 READY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SuccessGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Untuk alat kawalan jauh jenama BLE-M3: Tekan butang di bawah dan tahan/tekan butang Kamera pada BLE-M3 selama 5 saat untuk kunci (Sync) terus ke butang MULA RAKAM + BUNYI GUN.",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            lineHeight = 14.sp
                        )

                        if (is5SecSyncRunning) {
                            val secLeft = syncRemainingTenths / 10.0f
                            val progress = (50 - syncRemainingTenths) / 50f
                            Surface(
                                color = SpeedAmber.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, SpeedAmber),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = String.format("TEKAN & TAHAN BUTANG BLE-M3... %.1f SAAT", secLeft),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        color = SpeedAmber
                                    )
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        color = SpeedAmber,
                                        trackColor = StadiumSurfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                    )
                                    Text(
                                        text = "Mengesan isyarat HID Key / Touch-Click daripada BLE-M3...",
                                        fontSize = 9.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        syncCompletedMessage?.let { msg ->
                            Surface(
                                color = SuccessGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        Button(
                            onClick = { is5SecSyncRunning = !is5SecSyncRunning },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (is5SecSyncRunning) FinishLineRed else SpeedAmber
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("sync_ble_m3_5sec_btn")
                        ) {
                            Icon(
                                imageVector = if (is5SecSyncRunning) Icons.Default.Stop else Icons.Default.Sync,
                                contentDescription = null,
                                tint = if (is5SecSyncRunning) Color.White else Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (is5SecSyncRunning) "BATALKAN KIRAAN 5 SAAT" else "SET TO CLICK START (SYNC 5 SAAT BLE-M3)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (is5SecSyncRunning) Color.White else Color.Black
                            )
                        }
                    }
                }

                // 2. BLE-M3 MODE SELECTOR (INSTANT CLICK vs 5-SEC PRESS + POINTER INTERCEPT)
                Surface(
                    color = StadiumSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StadiumBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Mod Tindakan Butang BLE-M3:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        BleM3StartMode.entries.forEach { mode ->
                            val isSelected = selectedStartMode == mode
                            Surface(
                                color = if (isSelected) ElectricCyan.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) ElectricCyan else StadiumBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedStartMode = mode
                                        triggerManager.setStartMode(mode)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedStartMode = mode
                                            triggerManager.setStartMode(mode)
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = mode.label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) ElectricCyan else Color.White
                                        )
                                        Text(
                                            text = mode.description,
                                            fontSize = 9.sp,
                                            color = TextSecondary,
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Toggle for BLE-M3 Touch/Mouse Pointer Intercept
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Tangkap Klik Kursor BLE-M3",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tukar sebarang klik tetikus/skrin daripada BLE-M3 terus kepada butang START",
                                    fontSize = 9.sp,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = isPointerInterceptOn,
                                onCheckedChange = {
                                    isPointerInterceptOn = it
                                    triggerManager.setBlePointerInterceptEnabled(it)
                                },
                                modifier = Modifier.scale(0.75f)
                            )
                        }
                    }
                }

                // 3. ENABLE / DISABLE SWITCH & DETECTED BLE-M3 / BLUETOOTH DEVICES
                Surface(
                    color = StadiumSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isBluetoothTriggerEnabled) SuccessGreen else StadiumBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Aktifkan Bluetooth Selfie Starter",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isBluetoothTriggerEnabled) "Aktif: Sah untuk START sahaja + Starter Gun" else "Dimatikan",
                                    fontSize = 10.sp,
                                    color = if (isBluetoothTriggerEnabled) SuccessGreen else TextSecondary
                                )
                            }
                            Switch(
                                checked = isBluetoothTriggerEnabled,
                                onCheckedChange = onToggleBluetoothTrigger,
                                modifier = Modifier
                                    .scale(0.8f)
                                    .testTag("bluetooth_remote_enable_switch")
                            )
                        }

                        if (detectedDevices.isNotEmpty()) {
                            detectedDevices.forEach { dev: BluetoothRemoteDeviceInfo ->
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (dev.isBleM3Model) SpeedAmber else if (dev.isConnectedHid) ElectricCyan.copy(alpha = 0.6f) else StadiumBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(
                                                imageVector = Icons.Default.SettingsRemote,
                                                contentDescription = null,
                                                tint = if (dev.isBleM3Model) SpeedAmber else ElectricCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = dev.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = dev.deviceTypeLabel,
                                                    fontSize = 9.sp,
                                                    color = if (dev.isBleM3Model) SpeedAmber else ElectricCyan
                                                )
                                            }
                                        }
                                        Surface(
                                            color = SuccessGreen.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (dev.isBleM3Model) "BLE-M3 SYNC" else "SEDIA",
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
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ElectricCyan),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .testTag("open_system_bluetooth_settings_btn")
                            ) {
                                Icon(Icons.Default.SettingsBluetooth, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pair BLE-M3", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    isBtOn = triggerManager.isBluetoothEnabled()
                                    detectedDevices = triggerManager.getDetectedRemoteDevices()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StadiumSurface),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SilverBorder),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .testTag("rescan_bluetooth_remote_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Imbas Semula", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 4. LIVE SIGNAL MONITOR & TEST STARTER GUN BUTTON
                Button(
                    onClick = {
                        ElectronicStarterGunAudio.playExplosiveStarterGunShot(coroutineScope)
                        onTestFireStarterGun()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("test_starter_gun_audio_btn")
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Uji Bunyi Tembakan Starter Gun Sekarang",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
            ) {
                Text("Selesai & Sedia Mula", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }
    )
}
