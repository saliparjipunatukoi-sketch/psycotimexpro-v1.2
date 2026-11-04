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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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

/**
 * camMode values:
 * 1 = CAM 1 (Portrait Hadapan Pelari — Ikut Pergerakan Pelari ke Garisan Penamat)
 * 2 = CAM 2 (Penangkap Pemenang Garisan Penamat — Auto-Generate Photo Finish Torso)
 * 3 = 1 TELEFON (Syor Coach: Garisan Mula Menghala Pelari ➔ Fokus Pelari & AI Detect 100M Penamat)
 */
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

    var showRedFinishLine by remember { mutableStateOf(true) }
    var finishLinePosition by remember { mutableStateOf(0.55f) }
    var horizon100mLineYRatio by remember { mutableStateOf(0.24f) }
    var autoDetectFinishTorso by remember { mutableStateOf(true) }
    var isPortraitTallExpanded by remember { mutableStateOf(true) }
    var showSlitTuningDrawer by remember { mutableStateOf(false) }

    // Auto-trigger focus on runner when timer starts in 1-Phone 100M AI Mode or Cam 1
    LaunchedEffect(isTimerRunning, camMode) {
        if (isTimerRunning && (camMode == 1 || camMode == 3)) {
            CameraFrameHolder.triggerRunnerFocusAnd100mTrack(
                zoomRatioLinear = if (camMode == 3) 0.18f else 0.08f
            )
        } else if (!isTimerRunning) {
            CameraFrameHolder.resetCameraZoom()
        }
    }

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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // COMPACT PROFESSIONAL TOP TOOLBAR: 3 CAMERA MODES + BLE-M3 PILL + QR + SLIT TUNE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 3-Mode Segmented Selector: CAM 1 (Pelari) | 1 FON (100M AI) | CAM 2 (Torso Finish)
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
                        text = "CAM 1 Pelari",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    color = if (camMode == 3) ElectricCyan else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable { onSetCamMode(3) }
                        .testTag("mode_1phone_100m_button")
                ) {
                    Text(
                        text = "1 Fon 100M AI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (camMode == 3) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    color = if (camMode == 2) LaserOrange else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onSetCamMode(2) }
                ) {
                    Text(
                        text = "CAM 2 Torso",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (camMode == 2) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            // Right-side compact utility pills (BLE-M3 + QR + Slit Config)
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isBluetoothTriggerEnabled) ElectricCyan.copy(alpha = 0.15f) else StadiumSurfaceVariant,
                    shape = RoundedCornerShape(9.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isBluetoothTriggerEnabled) ElectricCyan.copy(alpha = 0.65f) else StadiumBorder
                    ),
                    modifier = Modifier
                        .height(30.dp)
                        .clickable { onOpenBluetoothRemoteSetup() }
                        .testTag("open_bluetooth_remote_setup_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (isBluetoothTriggerEnabled) SuccessGreen else TextMuted,
                                    CircleShape
                                )
                        )
                        Icon(
                            imageVector = Icons.Default.BluetoothConnected,
                            contentDescription = "BLE-M3 Trigger",
                            tint = if (isBluetoothTriggerEnabled) ElectricCyan else TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (lastRemoteSignal != null) "BLE-M3 ✓" else "BLE-M3",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isBluetoothTriggerEnabled) Color.White else TextSecondary
                        )
                    }
                }

                Surface(
                    color = StadiumSurfaceVariant,
                    shape = RoundedCornerShape(9.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
                    modifier = Modifier
                        .height(30.dp)
                        .clickable { onOpenQrPair() }
                        .testTag("open_qr_pair_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "Link Cam 2 QR",
                            tint = SilverMetallic,
                            modifier = Modifier.size(13.dp)
                        )
                        Text("QR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Surface(
                    color = if (showSlitTuningDrawer) FinishLineRed.copy(alpha = 0.2f) else StadiumSurfaceVariant,
                    shape = RoundedCornerShape(9.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (showSlitTuningDrawer) FinishLineRed else StadiumBorder
                    ),
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { showSlitTuningDrawer = !showSlitTuningDrawer }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Tetapan Garisan & 100M AI",
                            tint = if (showSlitTuningDrawer) FinishLineRed else SilverMetallic,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        // COMPACT MODE GUIDANCE STRIP FOR COACH
        Surface(
            color = StadiumSurface,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                when (camMode) {
                    3 -> ElectricCyan.copy(alpha = 0.5f)
                    2 -> LaserOrange.copy(alpha = 0.5f)
                    else -> FinishLineRed.copy(alpha = 0.4f)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when (camMode) {
                            3 -> Icons.Default.CenterFocusStrong
                            2 -> Icons.Default.PhotoCameraFront
                            else -> Icons.Default.DirectionsRun
                        },
                        contentDescription = null,
                        tint = when (camMode) {
                            3 -> ElectricCyan
                            2 -> LaserOrange
                            else -> FinishLineRed
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (camMode) {
                            3 -> "SYOR 1 TELEFON (PORTRAIT): Letak di Garisan Mula menghala pelari. Bila Start ditekan, kamera auto-fokus ikut pelari & AI kesan Garisan Penamat 100M."
                            2 -> "CAM 2 GARISAN PENAMAT: Tangkap pemenang di garisan penamat & Auto-Generate Photo Finish mengikut Torso pelari."
                            else -> "CAM 1 PORTRAIT HADAPAN PELARI: Kotak AI Besar Menegak mengikut pergerakan penuh pelari dari Mula sehingga ke Garisan Penamat."
                        },
                        fontSize = 9.sp,
                        color = Color.White,
                        lineHeight = 12.sp
                    )
                }
            }
        }

        // COLLAPSIBLE SLIT LINE & 100M HORIZON TUNING BAR
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
                            text = "Tetapan Garisan Slit, 100M Horizon AI & Auto Photo-Finish",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (autoDetectFinishTorso) "Auto Torso ON" else "Manual",
                                fontSize = 9.sp,
                                color = if (autoDetectFinishTorso) SuccessGreen else TextMuted
                            )
                            Switch(
                                checked = autoDetectFinishTorso,
                                onCheckedChange = { autoDetectFinishTorso = it },
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
                                label = { Text(label, fontSize = 9.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FinishLineRed,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { isPortraitTallExpanded = !isPortraitTallExpanded },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Default.AspectRatio,
                                contentDescription = "Saiz Portrait",
                                tint = if (isPortraitTallExpanded) ElectricCyan else SilverMetallic,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (camMode == 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Zon Penamat 100M AI:", fontSize = 9.sp, color = ElectricCyan)
                            Slider(
                                value = horizon100mLineYRatio,
                                onValueChange = { horizon100mLineYRatio = it },
                                valueRange = 0.14f..0.45f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // ELECTRONIC DIGITAL STOPWATCH CONSOLE (COMPACT BROADCAST BAR)
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isTimerRunning) FinishLineRed else StadiumBorder
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 10.dp),
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
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = SilverMetallic
                        )
                        if (isRecordingVideo || isTimerRunning) {
                            Surface(
                                color = FinishLineRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "● REC PORTRAIT (${CameraFrameHolder.recordedFramesCount}f)",
                                    fontSize = 8.sp,
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
                            fontSize = 9.sp,
                            color = if (isAudioGunEnabled) SuccessGreen else TextSecondary
                        )
                        Switch(
                            checked = isAudioGunEnabled,
                            onCheckedChange = onToggleAudioGun,
                            modifier = Modifier.scale(0.6f).testTag("audio_gun_switch")
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
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedTime,
                            fontSize = 28.sp,
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
                        text = when (camMode) {
                            3 -> "MOD 1 FON: GARISAN MULA ➔ FOKUS PELARI ➔ 100M AI"
                            2 -> "CAM 2: PENANGKAP PEMENANG & AUTO PHOTO-FINISH TORSO"
                            else -> "CAM 1: PORTRAIT BERDEPAN PELARI (KOTAK AI MENEGAK)"
                        },
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (camMode == 3) ElectricCyan else TextSecondary
                    )
                    if (lastRemoteSignal != null) {
                        Text(
                            text = "Trigger: ${lastRemoteSignal.keyName}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                    }
                }
            }
        }

        // LIVE PORTRAIT REAL CAMERA & TALL VERTICAL AI INTELLIGENCE BOX (KOTAK AI BESAR MENEGAK)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isPortraitTallExpanded) 380.dp else 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .border(
                    2.dp,
                    when {
                        isTimerRunning -> FinishLineRed
                        camMode == 3 -> ElectricCyan
                        camMode == 2 -> LaserOrange
                        else -> StadiumBorder
                    },
                    RoundedCornerShape(16.dp)
                )
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
                        text = "Benarkan akses kamera untuk rakaman Portrait Cam 1, pengesanan 100M AI & Photo-Finish Torso.",
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

            // TALL VERTICAL AI INTELLIGENCE BOX (KOTAK AI BESAR MENEGAK) & 100M HORIZON / SLIT OVERLAY
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Subtle reference grid lines
                for (i in 1..4) {
                    val y = i * (h / 5f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.12f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                // If 1-Phone 100M AI Mode (camMode == 3): Draw 100M Horizon AI Finish Gate + Perspective Lane Guides
                if (camMode == 3) {
                    val horizonY = h * horizon100mLineYRatio
                    // Perspective lane corridor from Start Line (bottom) to 100M Finish Gate (horizonY)
                    drawLine(
                        color = ElectricCyan.copy(alpha = 0.55f),
                        start = Offset(w * 0.14f, h),
                        end = Offset(w * 0.36f, horizonY),
                        strokeWidth = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                    drawLine(
                        color = ElectricCyan.copy(alpha = 0.55f),
                        start = Offset(w * 0.86f, h),
                        end = Offset(w * 0.64f, horizonY),
                        strokeWidth = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                    // 100M AI Finish Gate Horizontal Laser Line at the far end
                    drawLine(
                        color = FinishLineRed,
                        start = Offset(w * 0.24f, horizonY),
                        end = Offset(w * 0.76f, horizonY),
                        strokeWidth = 5f
                    )
                    drawCircle(
                        color = FinishLineRed,
                        radius = 7f,
                        center = Offset(w * 0.24f, horizonY)
                    )
                    drawCircle(
                        color = FinishLineRed,
                        radius = 7f,
                        center = Offset(w * 0.76f, horizonY)
                    )
                }

                // Vertical Red Finish Slit Line (Always shown on Cam 2, optional on Cam 1)
                if (showRedFinishLine && (camMode == 2 || camMode == 1)) {
                    val slitX = w * finishLinePosition
                    drawLine(
                        color = FinishLineRed,
                        start = Offset(slitX, 0f),
                        end = Offset(slitX, h),
                        strokeWidth = if (camMode == 2) 5f else 3f
                    )
                    drawCircle(
                        color = if (isMovementDetected) SuccessGreen else FinishLineRed,
                        radius = 9f,
                        center = Offset(slitX, 22f)
                    )
                }

                // TALL VERTICAL AI INTELLIGENCE BOX (KOTAK AI BESAR MENEGAK)
                // Always visible in Portrait to frame the runner vertically from head to toe,
                // glowing bright green when movement/runner is locked!
                val trackedCenterRatio = if (camMode == 2) {
                    finishLinePosition
                } else {
                    CameraFrameHolder.trackedHorizontalCenterRatio.coerceIn(0.28f, 0.72f)
                }
                val boxW = (w * 0.46f).coerceIn(150f, w * 0.62f)
                val boxH = (h * 0.78f).coerceAtLeast(210f) // Tall vertical box spanning 78% of screen height!
                val boxX = (w * trackedCenterRatio - boxW / 2f).coerceIn(12f, (w - boxW - 12f).coerceAtLeast(12f))
                val boxY = (h - boxH) / 2f

                val aiColor = when {
                    isMovementDetected -> SuccessGreen
                    isTimerRunning -> ElectricCyan
                    else -> ElectricCyan.copy(alpha = 0.65f)
                }

                // Translucent AI fill
                drawRoundRect(
                    color = aiColor.copy(alpha = if (isMovementDetected) 0.14f else 0.06f),
                    topLeft = Offset(boxX, boxY),
                    size = Size(boxW, boxH),
                    cornerRadius = CornerRadius(18f, 18f)
                )

                // Tall Vertical AI Intelligence Frame Stroke
                drawRoundRect(
                    color = aiColor,
                    topLeft = Offset(boxX, boxY),
                    size = Size(boxW, boxH),
                    cornerRadius = CornerRadius(18f, 18f),
                    style = Stroke(width = if (isMovementDetected) 4.5f else 2.5f)
                )

                // Upper Torso / Chest Lock Line inside the Tall Vertical AI Box
                val chestY = boxY + boxH * 0.34f
                drawLine(
                    color = if (camMode == 2) FinishLineRed else aiColor.copy(alpha = 0.8f),
                    start = Offset(boxX + 10f, chestY),
                    end = Offset(boxX + boxW - 10f, chestY),
                    strokeWidth = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                )

                // Hip / Stride Drive Lock Line inside the Tall Vertical AI Box
                val hipY = boxY + boxH * 0.56f
                drawLine(
                    color = SpeedAmber.copy(alpha = 0.75f),
                    start = Offset(boxX + 16f, hipY),
                    end = Offset(boxX + boxW - 16f, hipY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            }

            // Top-Start Status Indicator Overlay
            Surface(
                color = if (isMovementDetected) SuccessGreen.copy(alpha = 0.92f) else Color.Black.copy(alpha = 0.75f),
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
                        tint = if (isMovementDetected) Color.Black else ElectricCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when {
                            isMovementDetected && camMode == 2 -> "TORSO PEMENANG DIKESAN (AUTO PHOTO-FINISH)"
                            isMovementDetected && camMode == 3 -> "PELARI DIJEJAK ➔ GARISAN PENAMAT 100M AI"
                            isMovementDetected -> "KOTAK AI MENEGAK: PELARI DIKESAN"
                            camMode == 3 -> "MOD 1 FON: GARISAN MULA ➔ 100M AI AKTIF"
                            camMode == 2 -> "CAM 2: PENANGKAP TORSO GARISAN PENAMAT"
                            else -> "CAM 1 PORTRAIT: KOTAK AI BESAR MENEGAK"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isMovementDetected) Color.Black else Color.White
                    )
                }
            }

            // Top-End Live Video Recording Indicator Badge
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
                                .size(6.dp)
                                .background(Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "REC PORTRAIT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }

            // Bottom-Start Quick Focus 100M Button
            Surface(
                color = Color.Black.copy(alpha = 0.78f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clickable {
                        CameraFrameHolder.triggerRunnerFocusAnd100mTrack(
                            zoomRatioLinear = if (camMode == 3) 0.20f else 0.10f
                        )
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CenterFocusStrong,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Fokus Laluan Pelari 100M",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                }
            }

            // Bottom-End Motion Test / Cross Trigger button inside Camera overlay
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
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("simulate_motion_button")
                ) {
                    Text(
                        text = if (isMovementDetected) "Reset Sensor" else "Uji Sensor AI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMovementDetected) FinishLineRed else SilverMetallic
                    )
                }
            }
        }

        // TIMER PRIMARY CONTROLS (MULA + RAKAMAN + GUN, BERHENTI, RESET)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isTimerRunning) {
                Button(
                    onClick = { onStartTimer() },
                    colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.6f)
                        .height(48.dp)
                        .testTag("start_timer_button")
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("MULA RAKAM + BUNYI GUN", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            } else {
                Button(
                    onClick = { onStopTimer() },
                    colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.6f)
                        .height(48.dp)
                        .testTag("stop_timer_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TAMAT & AUTO PHOTO-FINISH", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }

            OutlinedButton(
                onClick = onResetTimer,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SilverBorder),
                modifier = Modifier
                    .weight(0.65f)
                    .height(48.dp)
                    .testTag("reset_timer_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("RESET", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // COMPACT BIOMECHANICS & AI INTELLIGENT POSTURE READER
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
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
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = if (isMovementDetected) SuccessGreen else ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Pembaca Postur AI Pintar (Start & Finish)",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Cam 1 (Kotak Menegak Pelari) & Cam 2 (Photo-Finish Torso)",
                                fontSize = 9.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Surface(
                        color = if (isMovementDetected) SuccessGreen.copy(alpha = 0.2f) else StadiumSurfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isMovementDetected) "Postur Aktif ✓" else "Sedia Membaca",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMovementDetected) SuccessGreen else TextMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                val analysis = latestAiAnalysis
                val torsoLean = analysis?.torsoLeanAngleDeg?.takeIf { it > 1.0 } ?: CameraFrameHolder.measuredFinishTorsoAngleDeg
                val cadence = analysis?.cadenceSpM?.takeIf { it > 0 } ?: 254
                val contactTime = analysis?.groundContactTimeMs?.takeIf { it > 0 } ?: 108
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

                if (isMovementDetected) {
                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (posture.isStartPostureCorrect) SuccessGreen.copy(alpha = 0.5f) else LaserOrange.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "1. Postur Permulaan (CAM 1): ${posture.startPostureStatusBadge}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = ElectricCyan
                            )
                            Text(
                                text = "✓ Betul: ${posture.startCorrectPostureAnalysis}",
                                fontSize = 9.sp,
                                color = Color.White,
                                lineHeight = 13.sp
                            )
                            Text(
                                text = "⚠ Salah: ${posture.startWrongPostureAnalysis}",
                                fontSize = 9.sp,
                                color = Color(0xFFFCA5A5),
                                lineHeight = 13.sp
                            )
                            Text(
                                text = "➡ Seterusnya: ${posture.startNextActionDrills.firstOrNull() ?: ""}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpeedAmber,
                                lineHeight = 13.sp
                            )
                        }
                    }

                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (posture.isFinishPostureCorrect) ElectricCyan.copy(alpha = 0.5f) else FinishLineRed.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "2. Postur Penamat (CAM 2 Torso): ${posture.finishPostureStatusBadge}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = FinishLineRed
                            )
                            Text(
                                text = "✓ Betul: ${posture.finishCorrectPostureAnalysis}",
                                fontSize = 9.sp,
                                color = Color.White,
                                lineHeight = 13.sp
                            )
                            Text(
                                text = "⚠ Salah: ${posture.finishWrongPostureAnalysis}",
                                fontSize = 9.sp,
                                color = Color(0xFFFCA5A5),
                                lineHeight = 13.sp
                            )
                            Text(
                                text = "➡ Seterusnya: ${posture.finishNextActionDrills.firstOrNull() ?: ""}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpeedAmber,
                                lineHeight = 13.sp
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
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 8.sp, color = TextSecondary)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = color)
            Text(subtext, fontSize = 7.sp, color = TextMuted, maxLines = 1)
        }
    }
}
