package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.service.AiSportsScienceAnalyzer
import com.example.service.MotionAnalysisResult
import com.example.service.RemoteKeySignal
import com.example.ui.components.CameraFrameHolder
import com.example.ui.components.RealCameraPreview
import com.example.ui.theme.*

@Composable
fun TimingSystemScreen(
    camMode: Int,
    isTimerRunning: Boolean,
    elapsedMillis: Long,
    isAudioGunEnabled: Boolean,
    isMovementDetected: Boolean,
    motionEnergy: Float,
    latestAiAnalysis: MotionAnalysisResult?,
    onSetCamMode: (Int) -> Unit,
    onToggleAudioGun: (Boolean) -> Unit,
    onStartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onTriggerMotion: (hasMotion: Boolean, energy: Float) -> Unit,
    onOpenQrPair: () -> Unit,
    isBluetoothTriggerEnabled: Boolean = true,
    isRecordingVideo: Boolean = false,
    lastRemoteSignal: RemoteKeySignal? = null,
    onOpenBluetoothRemoteSetup: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Finishing Line Customization Options (Collapsed by default for a clean pro broadcast layout)
    var showRedFinishLine by remember { mutableStateOf(true) }
    var finishLinePosition by remember { mutableStateOf(0.65f) }
    var isCameraRotatedFullscreen by remember { mutableStateOf(false) }
    var showSlitTuningDrawer by remember { mutableStateOf(false) }

    val hours = (elapsedMillis / 3600000).toInt()
    val minutes = ((elapsedMillis % 3600000) / 60000).toInt()
    val seconds = ((elapsedMillis % 60000) / 1000).toInt()
    val millis = (elapsedMillis % 1000).toInt()
    val formattedTime = String.format("%d:%02d:%02d:%03d", hours, minutes, seconds, millis)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TrackDarkNavy)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // COMPACT PROFESSIONAL TOP TOOLBAR: CAM 1/2 + COMPACT BLUETOOTH PILL + QR + SLIT TUNE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact Cam 1 / Cam 2 Segmented Selector
            Row(
                modifier = Modifier
                    .background(StadiumSurfaceVariant, RoundedCornerShape(10.dp))
                    .border(1.dp, StadiumBorder, RoundedCornerShape(10.dp))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (camMode == 1) FinishLineRed else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onSetCamMode(1) }
                ) {
                    Text(
                        text = "CAM 1 Start",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Surface(
                    color = if (camMode == 2) LaserOrange else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onSetCamMode(2) }
                ) {
                    Text(
                        text = "CAM 2 Gate",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Right-side compact utility pills next to menu (BT Trigger + QR + Slit Config)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Compact Professional Bluetooth Selfie Trigger Pill Button
                Surface(
                    color = if (isBluetoothTriggerEnabled) ElectricCyan.copy(alpha = 0.14f) else StadiumSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isBluetoothTriggerEnabled) ElectricCyan.copy(alpha = 0.65f) else StadiumBorder
                    ),
                    modifier = Modifier
                        .height(32.dp)
                        .clickable { onOpenBluetoothRemoteSetup() }
                        .testTag("open_bluetooth_remote_setup_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(
                                    if (isBluetoothTriggerEnabled) SuccessGreen else TextMuted,
                                    CircleShape
                                )
                        )
                        Icon(
                            imageVector = Icons.Default.BluetoothConnected,
                            contentDescription = "Bluetooth Selfie Trigger",
                            tint = if (isBluetoothTriggerEnabled) ElectricCyan else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (lastRemoteSignal != null) "BT ✓" else "BT Start",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBluetoothTriggerEnabled) Color.White else TextSecondary
                        )
                    }
                }

                // Compact QR Pair Icon Pill
                Surface(
                    color = StadiumSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
                    modifier = Modifier
                        .height(32.dp)
                        .clickable { onOpenQrPair() }
                        .testTag("open_qr_pair_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "Link Cam 2 QR",
                            tint = SilverMetallic,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("QR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Compact Slit Line Tuning Toggle Icon
                Surface(
                    color = if (showSlitTuningDrawer) FinishLineRed.copy(alpha = 0.2f) else StadiumSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (showSlitTuningDrawer) FinishLineRed else StadiumBorder
                    ),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { showSlitTuningDrawer = !showSlitTuningDrawer }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Tetapan Garisan Slit",
                            tint = if (showSlitTuningDrawer) FinishLineRed else SilverMetallic,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // COLLAPSIBLE SLIT LINE TUNING BAR (Only visible when user taps the Tune icon)
        AnimatedVisibility(visible = showSlitTuningDrawer) {
            Surface(
                color = StadiumSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Garisan Penamat Slit & Orientasi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (showRedFinishLine) "Garis Aktif" else "Sembunyi",
                                fontSize = 10.sp,
                                color = if (showRedFinishLine) FinishLineRed else TextMuted
                            )
                            Switch(
                                checked = showRedFinishLine,
                                onCheckedChange = { showRedFinishLine = it },
                                modifier = Modifier.scale(0.65f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Kiri 35%" to 0.35f, "Tengah 50%" to 0.50f, "Kanan 65%" to 0.65f).forEach { (label, pos) ->
                            FilterChip(
                                selected = kotlin.math.abs(finishLinePosition - pos) < 0.05f,
                                onClick = { finishLinePosition = pos },
                                label = { Text(label, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FinishLineRed,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { isCameraRotatedFullscreen = !isCameraRotatedFullscreen },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ScreenRotation,
                                contentDescription = "Putar Kamera",
                                tint = if (isCameraRotatedFullscreen) FinishLineRed else SilverMetallic,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // ELECTRONIC DIGITAL STOPWATCH CONSOLE
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isTimerRunning) FinishLineRed else StadiumBorder
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "STOPWATCH ELEKTRONIK (ET)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = SilverMetallic,
                            letterSpacing = 0.8.sp
                        )
                        if (isRecordingVideo || isTimerRunning) {
                            Surface(
                                color = FinishLineRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "● REC VIDEO + GUN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = FinishLineRed,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Audio Gun",
                            fontSize = 10.sp,
                            color = if (isAudioGunEnabled) SuccessGreen else TextSecondary
                        )
                        Switch(
                            checked = isAudioGunEnabled,
                            onCheckedChange = onToggleAudioGun,
                            modifier = Modifier.scale(0.65f).testTag("audio_gun_switch")
                        )
                    }
                }

                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isTimerRunning) FinishLineRed else SilverBorder.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedTime,
                            fontSize = 32.sp,
                            maxLines = 1,
                            softWrap = false,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = if (isTimerRunning) FinishLineRed else Color.White,
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (camMode == 1) "CAM 1 START GATE • 120 FPS" else "CAM 2 TORSO SLIT GATE • AI",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    if (lastRemoteSignal != null) {
                        Text(
                            text = "BT Trigger: ${lastRemoteSignal.keyName}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                    }
                }
            }
        }

        // LIVE REAL CAMERA & AI TORSO SENSOR SURFACE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCameraRotatedFullscreen) 320.dp else 230.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .border(2.dp, if (isTimerRunning || camMode == 2) FinishLineRed else StadiumBorder, RoundedCornerShape(16.dp))
        ) {
            if (hasCameraPermission) {
                RealCameraPreview(
                    isSlitActive = camMode == 2,
                    onMotionDetected = { hasMotion, energy ->
                        onTriggerMotion(hasMotion, energy)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = FinishLineRed,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Kamera Belum Diaktifkan",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Benarkan akses kamera untuk melihat garisan penamat & pengesanan torso.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed)
                    ) {
                        Text("Aktifkan Kamera Sekarang", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Laser / Slit Overlay Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                for (i in 1..4) {
                    val y = i * (h / 5f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                if (showRedFinishLine) {
                    val slitX = w * finishLinePosition
                    drawLine(
                        color = FinishLineRed,
                        start = Offset(slitX, 0f),
                        end = Offset(slitX, h),
                        strokeWidth = 4f
                    )
                    drawCircle(
                        color = if (isMovementDetected) SuccessGreen else FinishLineRed,
                        radius = 10f,
                        center = Offset(slitX, 25f)
                    )

                    if (isMovementDetected) {
                        val boxW = 80f
                        val boxH = 130f
                        val boxX = slitX - (boxW / 2f)
                        val boxY = (h / 2f) - (boxH / 2f)

                        drawRect(
                            color = FinishLineRed,
                            topLeft = Offset(boxX, boxY),
                            size = Size(boxW, boxH),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                        )
                    }
                }
            }

            // Top-Start Status Indicator Overlay
            Surface(
                color = if (isMovementDetected) SuccessGreen.copy(alpha = 0.95f) else Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isMovementDetected) Icons.Default.CheckCircle else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = if (isMovementDetected) Color.Black else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isMovementDetected) "TORSO DIKESAN DI GARISAN!" else "KAMERA AKTIF",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMovementDetected) Color.Black else Color.White
                    )
                }
            }

            // Top-End Live Video Recording Indicator Badge when started via Bluetooth or Start Button
            if (isRecordingVideo || isTimerRunning) {
                Surface(
                    color = FinishLineRed.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "REC VIDEO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }

            // Motion Test / Cross Trigger button inside Camera overlay
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                TextButton(
                    onClick = {
                        val newMotion = !isMovementDetected
                        onTriggerMotion(newMotion, if (newMotion) 0.85f else 0.05f)
                    },
                    modifier = Modifier.testTag("simulate_motion_button")
                ) {
                    Text(
                        text = if (isMovementDetected) "Reset Sensor" else "Uji Manual",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMovementDetected) FinishLineRed else SilverMetallic
                    )
                }
            }
        }

        // TIMER PRIMARY CONTROLS (MULA + RAKAMAN + GUN, BERHENTI, RESET)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!isTimerRunning) {
                Button(
                    onClick = { onStartTimer() },
                    colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(52.dp)
                        .testTag("start_timer_button")
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("MULA RAKAM + BUNYI GUN", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            } else {
                Button(
                    onClick = { onStopTimer() },
                    colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(52.dp)
                        .testTag("stop_timer_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TAMAT / SIMPAN RAKAMAN", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }

            OutlinedButton(
                onClick = onResetTimer,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SilverBorder),
                modifier = Modifier
                    .weight(0.65f)
                    .height(52.dp)
                    .testTag("reset_timer_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("RESET", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // BIOMECHANICS & AI INTELLIGENT POSTURE READER (START & FINISH POSTURE: WRONG VS CORRECT + NEXT STEPS)
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
            modifier = Modifier.fillMaxWidth()
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
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = if (isMovementDetected) SuccessGreen else ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Pembaca Postur AI Pintar (Start & Finish)",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Analisis Postur Sebenar Cam 1 (Blok) & Cam 2 (Garisan Penamat)",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Surface(
                        color = if (isMovementDetected) SuccessGreen.copy(alpha = 0.2f) else StadiumSurfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isMovementDetected) "Postur Dikesan ✓" else "Menunggu Atlit",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMovementDetected) SuccessGreen else TextMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (isMovementDetected) {
                    val analysis = latestAiAnalysis
                    val torsoLean = analysis?.torsoLeanAngleDeg ?: CameraFrameHolder.measuredFinishTorsoAngleDeg
                    val cadence = analysis?.cadenceSpM ?: 254
                    val contactTime = analysis?.groundContactTimeMs ?: 108
                    val posture = analysis?.postureAnalysis ?: AiSportsScienceAnalyzer.evaluateStartAndFinishPosture(
                        startTrunkAngleDeg = CameraFrameHolder.measuredStartBodyAngleDeg,
                        frontKneeAngleDeg = CameraFrameHolder.measuredFrontKneeAngleDeg,
                        rearKneeAngleDeg = CameraFrameHolder.measuredRearKneeAngleDeg,
                        finishTorsoAngleDeg = torsoLean,
                        cadenceSpM = cadence,
                        groundContactTimeMs = contactTime
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetricBox(
                            title = "Postur Start",
                            value = "%.1f°".format(posture.startBodyLeanAngleDeg),
                            subtext = if (posture.isStartPostureCorrect) "Betul (42-48°)" else "Perlu Baiki",
                            color = if (posture.isStartPostureCorrect) SuccessGreen else LaserOrange,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            title = "Torso Finish",
                            value = "%.1f°".format(posture.finishTorsoDipAngleDeg),
                            subtext = if (posture.isFinishPostureCorrect) "Betul (12-18°)" else "Perlu Baiki",
                            color = if (posture.isFinishPostureCorrect) ElectricCyan else FinishLineRed,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            title = "GCT Bumi",
                            value = "${contactTime}ms",
                            subtext = "Tolakan kaki",
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            title = "Cadence",
                            value = "$cadence",
                            subtext = "Langkah/min",
                            color = SilverMetallic,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (posture.isStartPostureCorrect) SuccessGreen.copy(alpha = 0.5f) else LaserOrange.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "1. Postur Permulaan (CAM 1 — Blok & Pecutan)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ElectricCyan
                                )
                                Text(
                                    text = posture.startPostureStatusBadge,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (posture.isStartPostureCorrect) SuccessGreen else LaserOrange
                                )
                            }
                            Text(
                                text = "✓ Postur Betul: ${posture.startCorrectPostureAnalysis}",
                                fontSize = 10.sp,
                                color = Color.White,
                                lineHeight = 14.sp
                            )
                            Text(
                                text = "⚠ Analisis Postur Salah: ${posture.startWrongPostureAnalysis}",
                                fontSize = 10.sp,
                                color = Color(0xFFFCA5A5),
                                lineHeight = 14.sp
                            )
                            Text(
                                text = "➡ Tindakan Seterusnya: ${posture.startNextActionDrills.firstOrNull() ?: ""}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpeedAmber,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (posture.isFinishPostureCorrect) ElectricCyan.copy(alpha = 0.5f) else FinishLineRed.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "2. Postur Garisan Penamat (CAM 2 — Torso Slit Dip)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = FinishLineRed
                                )
                                Text(
                                    text = posture.finishPostureStatusBadge,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (posture.isFinishPostureCorrect) SuccessGreen else FinishLineRed
                                )
                            }
                            Text(
                                text = "✓ Postur Betul: ${posture.finishCorrectPostureAnalysis}",
                                fontSize = 10.sp,
                                color = Color.White,
                                lineHeight = 14.sp
                            )
                            Text(
                                text = "⚠ Analisis Postur Salah: ${posture.finishWrongPostureAnalysis}",
                                fontSize = 10.sp,
                                color = Color(0xFFFCA5A5),
                                lineHeight = 14.sp
                            )
                            Text(
                                text = "➡ Tindakan Seterusnya: ${posture.finishNextActionDrills.firstOrNull() ?: ""}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpeedAmber,
                                lineHeight = 14.sp
                            )
                        }
                    }
                } else {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No detection save",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Sensor AI sedia membaca postur badan Permulaan (Cam 1) dan Penamat (Cam 2) secara langsung.",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    subtext: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 9.sp, color = TextSecondary)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = color)
            Text(subtext, fontSize = 8.sp, color = TextMuted, maxLines = 1)
        }
    }
}
