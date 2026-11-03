package com.example.ui.dialogs

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.BluetoothRemoteDeviceInfo
import com.example.service.BluetoothSelfieTriggerManager
import com.example.service.ElectronicStarterGunAudio
import com.example.service.RemoteKeySignal
import com.example.ui.theme.*

/**
 * Dialog for connecting, scanning, and calibrating a Bluetooth Camera Selfie Remote Trigger
 * (e.g. AB Shutter 3 / Bluetooth Remote Shutter) as the official Sprint Starter Gun Trigger.
 * Strictly valid ONLY for the START button and always fires the Starter Gun sound together with starting the timer.
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StadiumSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .background(ElectricCyan.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, ElectricCyan, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.BluetoothConnected,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Tetapan Bluetooth Selfie Trigger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Alat Kawalan Jauh Kamera Selfie (Pelepas Larian)",
                            fontSize = 11.sp,
                            color = ElectricCyan
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
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
                // SAFETY RULE BANNER: START ONLY + STARTER GUN SOUND
                Surface(
                    color = FinishLineRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, FinishLineRed.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Campaign,
                            contentDescription = null,
                            tint = FinishLineRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "SAH UNTUK BUTANG START SAHAJA + BUNYI PISTOL PELEPAS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = FinishLineRed
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Menekan butang Bluetooth Selfie Trigger atau butang MULA TIMING dalam aplikasi akan serentak membunyikan tembakan Starter Gun & memulakan masa pecutan. Tekanan kali kedua semasa larian sedang berjalan TIDAK akan menghentikan masa.",
                                fontSize = 11.sp,
                                color = Color.White,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // ENABLE / DISABLE SWITCH
                Surface(
                    color = StadiumSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isBluetoothTriggerEnabled) SuccessGreen else StadiumBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aktifkan Bluetooth Selfie Starter",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (isBluetoothTriggerEnabled) "Aktif: Sedia menerima isyarat butang Shutter/Volume" else "Dimatikan",
                                fontSize = 11.sp,
                                color = if (isBluetoothTriggerEnabled) SuccessGreen else TextSecondary
                            )
                        }
                        Switch(
                            checked = isBluetoothTriggerEnabled,
                            onCheckedChange = onToggleBluetoothTrigger,
                            modifier = Modifier
                                .scale(0.85f)
                                .testTag("bluetooth_remote_enable_switch")
                        )
                    }
                }

                // STEP 1: PAIR & SCAN BLUETOOTH DEVICES
                Surface(
                    color = StadiumSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StadiumBorder),
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
                            Text(
                                text = "1. Sambungan Peranti Bluetooth Remote",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Surface(
                                color = if (isBtOn) SuccessGreen.copy(alpha = 0.2f) else SpeedAmber.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isBtOn) "Bluetooth ON" else "Semak Bluetooth",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBtOn) SuccessGreen else SpeedAmber,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (detectedDevices.isEmpty()) {
                            Text(
                                text = "Tiada peranti Bluetooth Selfie Remote dikesan lagi. Hidupkan suis pada alat kawalan jauh selfie anda (cth: AB Shutter3) dan pasangkan (Pair) di Tetapan Bluetooth telefon.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        } else {
                            detectedDevices.forEach { dev: BluetoothRemoteDeviceInfo ->
                                Surface(
                                    color = Color.Black.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (dev.isConnectedHid) ElectricCyan.copy(alpha = 0.6f) else StadiumBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(
                                                imageVector = if (dev.isConnectedHid) Icons.Default.SettingsRemote else Icons.Default.Bluetooth,
                                                contentDescription = null,
                                                tint = if (dev.isConnectedHid) ElectricCyan else SilverMetallic,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = dev.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = dev.deviceTypeLabel,
                                                    fontSize = 10.sp,
                                                    color = if (dev.isConnectedHid) ElectricCyan else TextSecondary
                                                )
                                            }
                                        }
                                        Surface(
                                            color = SuccessGreen.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "SEDIA",
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
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ElectricCyan),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_system_bluetooth_settings_btn")
                            ) {
                                Icon(Icons.Default.SettingsBluetooth, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pair Bluetooth", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    isBtOn = triggerManager.isBluetoothEnabled()
                                    detectedDevices = triggerManager.getDetectedRemoteDevices()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StadiumSurface),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, SilverBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rescan_bluetooth_remote_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Imbas Semula", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // STEP 2: LIVE SIGNAL CALIBRATION & STARTER GUN AUDIO TEST
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.5.dp,
                        if (lastRemoteSignal != null) SuccessGreen else ElectricCyan.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "2. Kotak Uji & Kalibrasi Isyarat Butang Remote",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = ElectricCyan
                        )
                        Text(
                            text = "Tekan butang pada Bluetooth Camera Selfie Trigger anda sekarang untuk menguji sambungan & bunyi Starter Gun:",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        if (lastRemoteSignal != null) {
                            Surface(
                                color = SuccessGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SuccessGreen),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "✓ ISYARAT REMOTE DITERIMA!",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = SuccessGreen
                                    )
                                    Text(
                                        text = "Butang: ${lastRemoteSignal.keyName}",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Peranti: ${lastRemoteSignal.deviceName}",
                                        fontSize = 10.sp,
                                        color = SilverMetallic
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = StadiumSurfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Menunggu tekanan butang Bluetooth Selfie Trigger (Volume Up / Camera Shutter / Enter)...",
                                    fontSize = 11.sp,
                                    color = SpeedAmber,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                ElectronicStarterGunAudio.playExplosiveStarterGunShot(coroutineScope)
                                onTestFireStarterGun()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_starter_gun_audio_btn")
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Uji Bunyi Tembakan Starter Gun Sekarang",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
            ) {
                Text("Selesai & Sedia Mula", color = Color.Black, fontWeight = FontWeight.Black)
            }
        }
    )
}
