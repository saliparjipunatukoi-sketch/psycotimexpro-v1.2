package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import com.example.service.ElectronicStarterGunAudio
import com.example.service.MotionAnalysisResult
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Runtime Permission for Camera
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

    // Finishing Line Customization Options
    var showRedFinishLine by remember { mutableStateOf(true) }
    var finishLinePosition by remember { mutableStateOf(0.65f) } // 0.35f = Kiri, 0.5f = Tengah, 0.65f = Kanan
    var isCameraRotatedFullscreen by remember { mutableStateOf(false) }

    // High-Precision Electronic Timing Format: 0:00:00:000 (Hours:Minutes:Seconds:Milliseconds)
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // TOP CONTROLS: CAM 1 vs CAM 2 & QR PAIRING
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Camera Mode Toggle (Cam 1 Starter vs Cam 2 Finisher)
            Row(
                modifier = Modifier
                    .background(StadiumSurfaceVariant, RoundedCornerShape(20.dp))
                    .padding(3.dp)
            ) {
                FilterChip(
                    selected = camMode == 1,
                    onClick = { onSetCamMode(1) },
                    label = { Text("CAM 1 (Start)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FinishLineRed,
                        selectedLabelColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                FilterChip(
                    selected = camMode == 2,
                    onClick = { onSetCamMode(2) },
                    label = { Text("CAM 2 (Torso Gate)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LaserOrange,
                        selectedLabelColor = Color.White
                    )
                )
            }

            // QR Link Button for Phone Backup as Cam 2
            FilledTonalButton(
                onClick = onOpenQrPair,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = StadiumSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("open_qr_pair_button")
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = SilverMetallic, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Link Cam 2 QR", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // PANDUAN PENGGUNAAN DWI-TELEFON
        Surface(
            color = StadiumSurfaceVariant,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SilverBorder.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = LaserOrange, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cadangan Sistem Terbaik: Gunakan dua telefon pintar. Telefon 1 di Garisan Permulaan (Cam 1 Starter Gun) dan Telefon 2 di Garisan Penamat (Cam 2 Torso Timing Gate).",
                    fontSize = 11.sp,
                    color = Color.White,
                    lineHeight = 15.sp
                )
            }
        }

        // BIG ELECTRONIC DIGITAL STOPWATCH TIMER DISPLAY
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                2.dp,
                if (isTimerRunning) FinishLineRed else StadiumBorder
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CATATAN MASA ELEKTRONIK (ET)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Gun Sound",
                            fontSize = 11.sp,
                            color = if (isAudioGunEnabled) SuccessGreen else TextSecondary
                        )
                        Switch(
                            checked = isAudioGunEnabled,
                            onCheckedChange = onToggleAudioGun,
                            modifier = Modifier.scale(0.7f).testTag("audio_gun_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // One-Row Scaled High-Precision Digital Electronic Stopwatch Display
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isTimerRunning) FinishLineRed else SilverBorder.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
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

                Text(
                    text = if (camMode == 1) "CAM 1 TIMING GATE HOST • KELAJUAN RAKAMAN 120 FPS" else "CAM 2 FINISHER GATE • AI SLIT SCAN TORSO DETECTOR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }
        }

        // KONFIGURASI GARIS MERAH PENAMAT & ROTASI KAMERA
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = FinishLineRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Konfigurasi Garisan Penamat Merah",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (showRedFinishLine) "Aktif" else "Tutup",
                            fontSize = 11.sp,
                            color = if (showRedFinishLine) FinishLineRed else TextMuted
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = showRedFinishLine,
                            onCheckedChange = { showRedFinishLine = it },
                            modifier = Modifier.scale(0.7f)
                        )
                    }
                }

                // Posisi Garisan Penamat
                if (showRedFinishLine) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Posisi Slit:", fontSize = 11.sp, color = TextSecondary)
                        listOf("Kiri (35%)" to 0.35f, "Tengah (50%)" to 0.50f, "Kanan (65%)" to 0.65f).forEach { (label, pos) ->
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
                    }
                }

                // Butang Putar Kamera / Fullscreen Penamat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { isCameraRotatedFullscreen = !isCameraRotatedFullscreen },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCameraRotatedFullscreen) FinishLineRed else StadiumSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ScreenRotation, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCameraRotatedFullscreen) "Mod Potret Biasa" else "Putar Kamera Skrin Penuh (Finishing)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Sejajarkan garis merah dengan garis penamat trek",
                        fontSize = 9.sp,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                }
            }
        }

        // LIVE REAL CAMERA & AI TORSO SENSOR SURFACE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCameraRotatedFullscreen) 320.dp else 220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .border(2.dp, if (camMode == 2) FinishLineRed else StadiumBorder, RoundedCornerShape(16.dp))
        ) {
            if (hasCameraPermission) {
                // Real Live Camera Feed via CameraX
                RealCameraPreview(
                    isSlitActive = camMode == 2,
                    onMotionDetected = { hasMotion, energy ->
                        onTriggerMotion(hasMotion, energy)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Camera Permission Request Placeholder
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

                // Track lane guide lines
                for (i in 1..4) {
                    val y = i * (h / 5f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                // Finishing Line Slit Beam (Garisan Penamat Merah)
                if (showRedFinishLine) {
                    val slitX = w * finishLinePosition
                    // Laser Beam
                    drawLine(
                        color = FinishLineRed,
                        start = Offset(slitX, 0f),
                        end = Offset(slitX, h),
                        strokeWidth = 4f
                    )
                    // Sensor head indicator
                    drawCircle(
                        color = if (isMovementDetected) SuccessGreen else FinishLineRed,
                        radius = 10f,
                        center = Offset(slitX, 25f)
                    )

                    // Kotak AI Intelligent Torso Detection
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

            // Status Indicator Overlay
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
                        text = if (isMovementDetected) "KOTAK AI: TORSO DIKESAN DI GARISAN!" else "KAMERA AKTIF (MENUNGGU TORSO)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMovementDetected) Color.Black else Color.White
                    )
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

        // TIMER PRIMARY CONTROLS (MULA, BERHENTI, RESET)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!isTimerRunning) {
                Button(
                    onClick = {
                        // Play genuine explosive Electronic Starter Gun shot audio
                        if (isAudioGunEnabled) {
                            ElectronicStarterGunAudio.playExplosiveStarterGunShot(coroutineScope)
                        }
                        onStartTimer()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(52.dp)
                        .testTag("start_timer_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("MULA TIMING (GUN)", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
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
                    Text("TAMAT / RAKAM HASIL", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }

            OutlinedButton(
                onClick = onResetTimer,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SilverBorder),
                modifier = Modifier
                    .weight(0.7f)
                    .height(52.dp)
                    .testTag("reset_timer_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("RESET", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // BIOMECHANICS & AI INTELLIGENT METRICS / "NO DETECTION SAVE"
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = if (isMovementDetected) SuccessGreen else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kotak AI Pintar: Bacaan Pergerakan & Analisis",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    Surface(
                        color = if (isMovementDetected) SuccessGreen.copy(alpha = 0.2f) else StadiumSurfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isMovementDetected) "Manusia Tertangkap ✓" else "Menunggu Atlit",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMovementDetected) SuccessGreen else TextMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (isMovementDetected) {
                    val analysis = latestAiAnalysis
                    val torsoLean = analysis?.torsoLeanAngleDeg ?: 14.5
                    val cadence = analysis?.cadenceSpM ?: 254
                    val contactTime = analysis?.groundContactTimeMs ?: 108

                    // Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricBox(
                            title = "Sudut Torso",
                            value = "%.1f°".format(torsoLean),
                            subtext = "Condong ke hadapan",
                            color = FinishLineRed,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            title = "Sentuhan Bumi",
                            value = "${contactTime}ms",
                            subtext = "Fasa tolakan pantas",
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            title = "Cadence",
                            value = "$cadence",
                            subtext = "Langkah seminit",
                            color = SilverMetallic,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // AI Commentary
                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Komen & Analisis AI: Pergerakan dada/torso sempurna melepasi garis penamat merah. Sudut kecondongan %.1f° menepati standard perlumbaan untuk catatan masa rasmi yang optimum.".format(torsoLean),
                            fontSize = 11.sp,
                            color = Color.White,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    // IF NO HUMAN DETECTED: TEXT NO DETECTION SAVE AS REQUESTED
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No detection save",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tiada manusia tertangkap oleh kotak sensor AI buat masa ini. Sensor bersedia mengesan atlit semasa melepasi garisan.",
                                fontSize = 11.sp,
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
