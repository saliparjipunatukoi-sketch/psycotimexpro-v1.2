package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AthleteEntity
import com.example.service.SandJumpAiAnalysis
import com.example.ui.components.CameraFrameHolder
import com.example.ui.components.RealCameraPreview
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import kotlin.math.hypot

@Composable
fun SandJumpAiScreen(
    athletes: List<AthleteEntity>,
    sandJumpHistory: List<SandJumpAiAnalysis>,
    isRecordingJump: Boolean,
    recordingElapsedMs: Long,
    latestJumpResult: SandJumpAiAnalysis?,
    onStartJumpRecording: () -> Unit,
    onSaveJumpAndAnalyze: (
        jumpTitle: String,
        athleteName: String,
        athleteId: Long?,
        eventCategory: String,
        configuredMaxTapeMeters: Double,
        landingProgressRatio: Float,
        toeToBoardCm: Double,
        motionEnergy: Float,
        tapeStartX: Float,
        tapeStartY: Float,
        tapeEndX: Float,
        tapeEndY: Float,
        landingX: Float,
        landingY: Float
    ) -> Unit,
    onSelectJumpResult: (SandJumpAiAnalysis?) -> Unit,
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

    // 1. Sport Category & Athlete Configuration
    val categories = remember {
        listOf(
            "Lompat Jauh (Long Jump)",
            "Lompat Kijang (Triple Jump)",
            "Kategori Sukan Pasir Lain"
        )
    }
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var selectedAthlete by remember(athletes) { mutableStateOf(athletes.firstOrNull()) }
    var customAthleteName by remember { mutableStateOf("Atlit Lompat #1") }

    // 2. Digital Measuring Tape Calibration (Papan Anjakan Hujung -> Kotak Garisan Terakhir Pasir)
    var configuredMaxTapeMeters by remember { mutableStateOf(8.0) }
    var isCalibrationSureApproved by remember { mutableStateOf(false) }

    // Normalized coordinates (0f..1f) on camera viewport:
    // Point A = Hujung Papan Anjakan (0.00m)
    var boardStartX by remember { mutableStateOf(0.18f) }
    var boardStartY by remember { mutableStateOf(0.62f) }
    // Point B = Garisan Terakhir Kotak Sukatan Pasir (Max Tape Meters)
    var sandBoxEndX by remember { mutableStateOf(0.86f) }
    var sandBoxEndY by remember { mutableStateOf(0.58f) }

    // AI Detected / Fine-tuned Landing Progress along the calibrated tape (0f..1f)
    var landingProgressRatio by remember { mutableStateOf(0.76f) }
    // Toe-to-board distance in cm (+ = valid before plastisin, - = foul over plastisin)
    var toeToBoardCm by remember { mutableStateOf(3.8) }
    var liveMotionEnergy by remember { mutableStateOf(0.45f) }
    var isMotionDetectedInPit by remember { mutableStateOf(false) }

    // Live Measured Distance computed in real time from the calibrated measuring tape
    val liveMeasuredMeters = remember(configuredMaxTapeMeters, landingProgressRatio) {
        ((configuredMaxTapeMeters * landingProgressRatio.coerceIn(0.10f, 0.99f)) * 100.0).toInt() / 100.0
    }

    val landingPointX = boardStartX + (sandBoxEndX - boardStartX) * landingProgressRatio
    val landingPointY = boardStartY + (sandBoxEndY - boardStartY) * landingProgressRatio

    val recSecs = (recordingElapsedMs / 1000).toInt()
    val recMillis = ((recordingElapsedMs % 1000) / 10).toInt()
    val formattedRecTime = String.format(Locale.US, "%02d.%02d s", recSecs, recMillis)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TrackDarkNavy)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // TOP HEADER BAR: AI SAND PIT & JUMP INTELLIGENCE
        Surface(
            color = StadiumSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                1.dp,
                if (isCalibrationSureApproved) SuccessGreen.copy(alpha = 0.6f) else SpeedAmber.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .background(SpeedAmber.copy(alpha = 0.16f), CircleShape)
                            .border(1.dp, SpeedAmber, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Straighten,
                            contentDescription = null,
                            tint = SpeedAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "AI INTELLIGENCE: LOMPAT JAUH, TRIPLE JUMP & SUKAN PASIR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Kesan Papan Anjakan ➔ Pita Pengukur ➔ Kotak Sukatan Pasir Terjauh + Bukti Photo-Finish",
                            fontSize = 8.sp,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    color = if (isCalibrationSureApproved) SuccessGreen.copy(alpha = 0.2f) else SpeedAmber.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isCalibrationSureApproved) "SURE APPROVED ✓" else "PERLU KALIBRASI",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isCalibrationSureApproved) SuccessGreen else SpeedAmber,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // CATEGORY & ATHLETE QUICK SELECTOR ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    color = if (isSelected) ElectricCyan else StadiumSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isSelected) ElectricCyan else StadiumBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clickable {
                            selectedCategory = cat
                            if (cat.contains("Kijang") || cat.contains("Triple")) {
                                configuredMaxTapeMeters = 15.0
                            } else if (configuredMaxTapeMeters > 10.0) {
                                configuredMaxTapeMeters = 8.0
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                        Text(
                            text = cat.substringBefore(" ("),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) Color.Black else Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // LIVE CAMERA VIEWPORT WITH INTERACTIVE DRAGGABLE MEASURING TAPE (PITA PENGUKUR AI)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
                .border(
                    2.dp,
                    when {
                        isRecordingJump -> FinishLineRed
                        isCalibrationSureApproved -> SuccessGreen
                        else -> SpeedAmber
                    },
                    RoundedCornerShape(14.dp)
                )
        ) {
            if (hasCameraPermission) {
                RealCameraPreview(
                    isSlitActive = true,
                    onMotionDetected = { hasMotion, energy ->
                        isMotionDetectedInPit = hasMotion
                        if (hasMotion) {
                            liveMotionEnergy = energy
                            if (isRecordingJump) {
                                // Dynamically refine sand landing detection within calibrated box
                                val dynamicShift = ((energy - 0.3f) * 0.08f).coerceIn(-0.04f, 0.06f)
                                landingProgressRatio = (landingProgressRatio + dynamicShift).coerceIn(0.45f, 0.92f)
                            }
                        }
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
                        tint = SpeedAmber,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Aktifkan Kamera Menghadap Papan Anjakan & Kotak Pasir",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = SpeedAmber)
                    ) {
                        Text("Benarkan Akses Kamera", fontWeight = FontWeight.Black, color = Color.Black, fontSize = 11.sp)
                    }
                }
            }

            // INTERACTIVE MEASURING TAPE & SAND PIT BOX OVERLAY CANVAS
            // Coach drags Handle A (Hujung Papan Anjakan 0.00m), Handle B (Garisan Terakhir Kotak Pasir),
            // or the Sand Landing Marker directly on the screen!
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isCalibrationSureApproved) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val touchX = change.position.x / w
                            val touchY = change.position.y / h

                            val distToStart = hypot(touchX - boardStartX, touchY - boardStartY)
                            val distToEnd = hypot(touchX - sandBoxEndX, touchY - sandBoxEndY)
                            val distToLanding = hypot(touchX - landingPointX, touchY - landingPointY)

                            if (!isCalibrationSureApproved) {
                                // During calibration: drag either Papan Anjakan (0m) or Hujung Kotak Pasir
                                if (distToStart <= distToEnd) {
                                    boardStartX = (boardStartX + dragAmount.x / w).coerceIn(0.06f, 0.65f)
                                    boardStartY = (boardStartY + dragAmount.y / h).coerceIn(0.18f, 0.88f)
                                } else {
                                    sandBoxEndX = (sandBoxEndX + dragAmount.x / w).coerceIn(0.35f, 0.95f)
                                    sandBoxEndY = (sandBoxEndY + dragAmount.y / h).coerceIn(0.18f, 0.88f)
                                }
                            } else if (distToLanding < 0.25f) {
                                // After calibration approved: allow fine-tuning the sand break mark along the tape
                                val deltaRatio = (dragAmount.x / w) / (sandBoxEndX - boardStartX).coerceAtLeast(0.2f)
                                landingProgressRatio = (landingProgressRatio + deltaRatio).coerceIn(0.15f, 0.98f)
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                val startPx = Offset(boardStartX * w, boardStartY * h)
                val endPx = Offset(sandBoxEndX * w, sandBoxEndY * h)
                val landPx = Offset(landingPointX * w, landingPointY * h)

                // 1. Draw Sand Pit Measuring Box Zone (Kotak Sukatan Pasir)
                val pitLeft = minOf(startPx.x, endPx.x) + ( kotlin.math.abs(endPx.x - startPx.x) * 0.14f )
                val pitRight = maxOf(startPx.x, endPx.x) + 18f
                val pitTop = minOf(startPx.y, endPx.y) - h * 0.20f
                val pitBottom = maxOf(startPx.y, endPx.y) + h * 0.16f

                drawRoundRect(
                    color = SpeedAmber.copy(alpha = 0.10f),
                    topLeft = Offset(pitLeft, pitTop),
                    size = Size((pitRight - pitLeft).coerceAtLeast(40f), (pitBottom - pitTop).coerceAtLeast(40f)),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                drawRoundRect(
                    color = if (isCalibrationSureApproved) SuccessGreen.copy(alpha = 0.7f) else SpeedAmber,
                    topLeft = Offset(pitLeft, pitTop),
                    size = Size((pitRight - pitLeft).coerceAtLeast(40f), (pitBottom - pitTop).coerceAtLeast(40f)),
                    cornerRadius = CornerRadius(12f, 12f),
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = if (isCalibrationSureApproved) null else PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                )

                // 2. Draw Take-Off Board Line (Papan Anjakan + Plastisin Foul Line)
                val boardColor = if (toeToBoardCm >= 0.0) SuccessGreen else FinishLineRed
                drawLine(
                    color = boardColor,
                    start = Offset(startPx.x, startPx.y - h * 0.24f),
                    end = Offset(startPx.x, startPx.y + h * 0.18f),
                    strokeWidth = 6f
                )

                // 3. Draw Garisan Terakhir Kotak Sukatan Pasir (End Line)
                drawLine(
                    color = ElectricCyan,
                    start = Offset(endPx.x, endPx.y - h * 0.24f),
                    end = Offset(endPx.x, endPx.y + h * 0.18f),
                    strokeWidth = 5f
                )

                // 4. Draw Stretched Digital Measuring Tape (Pita Pengukur AI) from Board (0.00m) to Last Sand Box Line
                drawLine(
                    color = SpeedAmber,
                    start = startPx,
                    end = endPx,
                    strokeWidth = 5f
                )

                // Draw Meter Graduation Ticks along the Measuring Tape
                val totalTicks = configuredMaxTapeMeters.toInt().coerceIn(4, 16)
                for (i in 0..totalTicks) {
                    val t = i.toFloat() / totalTicks.toFloat()
                    val tickX = startPx.x + (endPx.x - startPx.x) * t
                    val tickY = startPx.y + (endPx.y - startPx.y) * t
                    drawLine(
                        color = Color.White,
                        start = Offset(tickX, tickY - 10f),
                        end = Offset(tickX, tickY + 10f),
                        strokeWidth = 2.5f
                    )
                }

                // 5. Draw Parabolic Jump Flight Arc & Tall Vertical AI Landing Box at Furthest Sand Mark
                if (isCalibrationSureApproved || isRecordingJump) {
                    val flightPath = Path().apply {
                        moveTo(startPx.x, startPx.y)
                        val ctrlX = (startPx.x + landPx.x) / 2f
                        val ctrlY = minOf(startPx.y, landPx.y) - h * 0.32f
                        quadraticTo(ctrlX, ctrlY, landPx.x, landPx.y)
                    }
                    drawPath(
                        path = flightPath,
                        color = ElectricCyan,
                        style = Stroke(
                            width = 4f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                        )
                    )

                    // Tall Vertical AI Athlete Landing Box at Sand Mark
                    val aiBoxW = (w * 0.22f).coerceIn(85f, 150f)
                    val aiBoxH = (h * 0.56f).coerceAtLeast(140f)
                    drawRoundRect(
                        color = SuccessGreen.copy(alpha = 0.16f),
                        topLeft = Offset(landPx.x - aiBoxW / 2f, landPx.y - aiBoxH * 0.78f),
                        size = Size(aiBoxW, aiBoxH),
                        cornerRadius = CornerRadius(14f, 14f)
                    )
                    drawRoundRect(
                        color = SuccessGreen,
                        topLeft = Offset(landPx.x - aiBoxW / 2f, landPx.y - aiBoxH * 0.78f),
                        size = Size(aiBoxW, aiBoxH),
                        cornerRadius = CornerRadius(14f, 14f),
                        style = Stroke(width = 3.5f)
                    )

                    // Sand Break Mark Target Circle
                    drawCircle(
                        color = FinishLineRed,
                        radius = 11f,
                        center = landPx
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = landPx
                    )
                }

                // Draggable Calibration Handles (Handle A = 0.00m Papan, Handle B = Kotak Terakhir)
                drawCircle(
                    color = boardColor,
                    radius = 14f,
                    center = startPx
                )
                drawCircle(
                    color = ElectricCyan,
                    radius = 14f,
                    center = endPx
                )
            }

            // Top-Start Calibration / Recording Status Overlay
            Surface(
                color = Color.Black.copy(alpha = 0.82f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Text(
                        text = if (!isCalibrationSureApproved) {
                            "LANGKAH 1: TARIK PITA DARI PAPAN (HIJAU) KE HUJUNG KOTAK PASIR (BIRU)"
                        } else if (isRecordingJump) {
                            "● REC LOMPATAN AKTIF ($formattedRecTime) • ${CameraFrameHolder.recordedFramesCount} FRAME"
                        } else {
                            "✓ PITA DIKUNCI (0m ➔ ${"%.1f".format(configuredMaxTapeMeters)}m) • SEDIA RAKAM ATLIT"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = when {
                            isRecordingJump -> FinishLineRed
                            isCalibrationSureApproved -> SuccessGreen
                            else -> SpeedAmber
                        }
                    )
                    Text(
                        text = "Papan: ${if (toeToBoardCm >= 0) "SAH (+${"%.1f".format(toeToBoardCm)}cm)" else "BATAL (${"%.1f".format(toeToBoardCm)}cm)"} • Sukatan Pasir Semasa: ${"%.2f".format(liveMeasuredMeters)} m",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }

            // Top-End Live Distance Readout Badge
            Surface(
                color = Color.Black.copy(alpha = 0.88f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.5.dp, if (toeToBoardCm >= 0) SuccessGreen else FinishLineRed),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = String.format(Locale.US, "%.2f m", liveMeasuredMeters),
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = if (toeToBoardCm >= 0) SuccessGreen else FinishLineRed
                    )
                    Text(
                        text = if (toeToBoardCm >= 0) "PAPAN SAH ✓" else "BATAL FOUL ⚠",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = if (toeToBoardCm >= 0) SuccessGreen else FinishLineRed
                    )
                }
            }
        }

        // STEP 1 CARD: DIGITAL MEASURING TAPE CONFIGURATION & "SURE APPROVE" BUTTON
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                1.dp,
                if (isCalibrationSureApproved) SuccessGreen.copy(alpha = 0.5f) else SpeedAmber.copy(alpha = 0.7f)
            ),
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
                            text = "1. Konfigurasi Pita Pengukur & Papan Anjakan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Tarik titik Pita Pengukur di skrin kamera dari Papan Hujung (0.00m) ke Garisan Terakhir Kotak Pasir, kemudian tekan SURE APPROVE.",
                            fontSize = 9.sp,
                            color = TextSecondary,
                            lineHeight = 12.sp
                        )
                    }
                }

                // Tape Max Distance Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Panjang Pita:", fontSize = 9.sp, color = SilverMetallic, fontWeight = FontWeight.Bold)
                    listOf(6.0, 8.0, 10.0, 15.0).forEach { meterVal ->
                        val isChosen = kotlin.math.abs(configuredMaxTapeMeters - meterVal) < 0.1
                        Surface(
                            color = if (isChosen) SpeedAmber else StadiumSurfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .height(26.dp)
                                .clickable {
                                    configuredMaxTapeMeters = meterVal
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = "${meterVal.toInt()}m",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isChosen) Color.Black else Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Board Valid / Foul Simulator Toggle for Coach Testing
                    Surface(
                        color = if (toeToBoardCm >= 0) SuccessGreen.copy(alpha = 0.18f) else FinishLineRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (toeToBoardCm >= 0) SuccessGreen else FinishLineRed),
                        modifier = Modifier
                            .height(26.dp)
                            .clickable {
                                toeToBoardCm = if (toeToBoardCm >= 0) -2.4 else 3.8
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = if (toeToBoardCm >= 0) "Papan: SAH (+3.8cm)" else "Papan: BATAL (-2.4cm)",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = if (toeToBoardCm >= 0) SuccessGreen else FinishLineRed
                            )
                        }
                    }
                }

                // Fine Slider for Sand Pit Landing Mark & SURE APPROVE Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Kesan Pasir: ${"%.2f".format(liveMeasuredMeters)}m",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        modifier = Modifier.width(105.dp)
                    )
                    Slider(
                        value = landingProgressRatio,
                        onValueChange = { landingProgressRatio = it },
                        valueRange = 0.20f..0.98f,
                        colors = SliderDefaults.colors(
                            thumbColor = SuccessGreen,
                            activeTrackColor = SpeedAmber
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isCalibrationSureApproved = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCalibrationSureApproved) SuccessGreen else SpeedAmber
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(40.dp)
                            .testTag("sure_approve_calibration_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCalibrationSureApproved) "✓ SURE APPROVED (PITA DIKUNCI)" else "SURE APPROVE (KUNCI PITA & PAPAN)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }

                    if (isCalibrationSureApproved) {
                        OutlinedButton(
                            onClick = { isCalibrationSureApproved = false },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, SilverBorder),
                            modifier = Modifier
                                .weight(0.6f)
                                .height(40.dp)
                        ) {
                            Text("Laraskan", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // STEP 2 CARD: CONTINUOUS VIDEO RECORDING (ATLIT BERSEDIA ➔ LOMPAT ➔ SAVE)
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                1.5.dp,
                if (isRecordingJump) FinishLineRed else ElectricCyan.copy(alpha = 0.5f)
            ),
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
                            text = "2. Rakaman Berterusan & Tangkap Bukti Lompatan AI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Video sentiasa merakam sewaktu atlit bersedia sehingga selesai mendarat di pasir. Tekan SAVE untuk menjana Photo-Finish Pasir & Analisis Sains Sukan AI.",
                            fontSize = 9.sp,
                            color = TextSecondary,
                            lineHeight = 12.sp
                        )
                    }
                }

                // Athlete Name Input / Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = selectedAthlete?.name ?: customAthleteName,
                        onValueChange = {
                            selectedAthlete = null
                            customAthleteName = it
                        },
                        label = { Text("Nama Atlit Lompatan", fontSize = 9.sp) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White),
                        modifier = Modifier.weight(1f)
                    )

                    if (athletes.isNotEmpty()) {
                        Surface(
                            color = StadiumSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .height(46.dp)
                                .clickable {
                                    val idx = athletes.indexOf(selectedAthlete)
                                    val nextIdx = if (idx < 0 || idx + 1 >= athletes.size) 0 else idx + 1
                                    selectedAthlete = athletes[nextIdx]
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    text = "Tukar Atlit",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isRecordingJump) {
                        Button(
                            onClick = {
                                if (!isCalibrationSureApproved) {
                                    isCalibrationSureApproved = true
                                }
                                onStartJumpRecording()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("start_sand_jump_recording_btn")
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MULA RAKAM (ATLIT BERSEDIA ➔ LOMPAT)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                val jumpNum = sandJumpHistory.size + 1
                                val title = "Lompatan $jumpNum"
                                val athName = selectedAthlete?.name ?: customAthleteName.ifBlank { "Atlit Lompat" }
                                onSaveJumpAndAnalyze(
                                    title,
                                    athName,
                                    selectedAthlete?.id,
                                    selectedCategory,
                                    configuredMaxTapeMeters,
                                    landingProgressRatio,
                                    toeToBoardCm,
                                    liveMotionEnergy,
                                    boardStartX,
                                    boardStartY,
                                    sandBoxEndX,
                                    sandBoxEndY,
                                    landingPointX,
                                    landingPointY
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("save_sand_jump_evidence_btn")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SAVE — JANA BUKTI & ANALISIS SAINS SUKAN AI ($formattedRecTime)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // STEP 3: AI INTELLIGENCE & SPORTS SCIENCE JUMP EVIDENCE REPORT
        val displayedJump = latestJumpResult ?: sandJumpHistory.firstOrNull()
        if (displayedJump != null) {
            SandJumpAiResultCard(
                result = displayedJump,
                allHistory = sandJumpHistory,
                onSelectHistoryItem = onSelectJumpResult
            )
        }
    }
}

@Composable
private fun SandJumpAiResultCard(
    result: SandJumpAiAnalysis,
    allHistory: List<SandJumpAiAnalysis>,
    onSelectHistoryItem: (SandJumpAiAnalysis?) -> Unit
) {
    val evidenceBitmap = remember(result.evidenceImagePath) {
        if (result.evidenceImagePath.isNotBlank()) {
            val f = File(result.evidenceImagePath)
            if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
        } else null
    }

    // Load recorded multi-frame jump video frames if available
    val jumpVideoFrames = remember(result.videoFramesDirPath) {
        val list = mutableListOf<android.graphics.Bitmap>()
        if (result.videoFramesDirPath.isNotBlank()) {
            val dir = File(result.videoFramesDirPath)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles { f -> f.name.endsWith(".jpg") || f.name.endsWith(".png") }
                    ?.sortedBy { it.name }
                    ?.forEach { f ->
                        BitmapFactory.decodeFile(f.absolutePath)?.let { list.add(it) }
                    }
            }
        }
        list
    }

    var isPlayingJumpReplay by remember { mutableStateOf(false) }
    var replayFrameIdx by remember { mutableStateOf(0) }

    LaunchedEffect(isPlayingJumpReplay, jumpVideoFrames.size) {
        if (isPlayingJumpReplay && jumpVideoFrames.isNotEmpty()) {
            while (isPlayingJumpReplay) {
                delay(80)
                replayFrameIdx = (replayFrameIdx + 1) % jumpVideoFrames.size
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.5.dp,
            if (result.isBoardValid) SuccessGreen.copy(alpha = 0.7f) else FinishLineRed.copy(alpha = 0.7f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sand_jump_ai_result_card")
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Keputusan Bukti Photo-Finish Pasir & Analisis Sains Sukan AI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "${result.jumpTitle} • ${result.athleteName} • ${result.eventCategory}",
                            fontSize = 9.sp,
                            color = ElectricCyan
                        )
                    }
                }

                Surface(
                    color = if (result.isBoardValid) SuccessGreen else FinishLineRed,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.2f m", result.measuredDistanceMeters),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = if (result.isBoardValid) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // PHOTO-FINISH SAND EVIDENCE IMAGE / MOVING REPLAY VIEWPORT
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
                    .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            ) {
                val activeReplayBmp = if (isPlayingJumpReplay && jumpVideoFrames.isNotEmpty()) {
                    jumpVideoFrames.getOrNull(replayFrameIdx)
                } else {
                    evidenceBitmap
                }

                if (activeReplayBmp != null) {
                    Image(
                        bitmap = activeReplayBmp.asImageBitmap(),
                        contentDescription = "Bukti Photo-Finish Lompatan Pasir",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (jumpVideoFrames.size > 1) {
                    Button(
                        onClick = { isPlayingJumpReplay = !isPlayingJumpReplay },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlayingJumpReplay) LaserOrange else FinishLineRed
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .height(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlayingJumpReplay) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPlayingJumpReplay) "Lihat Bukti Slit" else "Main Video (${jumpVideoFrames.size}f)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }

            // VERDICT BANNER
            Surface(
                color = if (result.isBoardValid) SuccessGreen.copy(alpha = 0.14f) else FinishLineRed.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (result.isBoardValid) SuccessGreen else FinishLineRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = result.boardStatusBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (result.isBoardValid) SuccessGreen else FinishLineRed
                    )
                    Text(
                        text = result.aiScienceVerdict,
                        fontSize = 9.sp,
                        color = Color.White
                    )
                }
            }

            // 4 KEY BIOMECHANICS METRICS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SandMetricChip(
                    label = "Jarak Sebenar",
                    value = "${"%.2f".format(result.effectiveDistanceFromFootMeters)}m",
                    sub = "Dari tapak kaki",
                    color = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                SandMetricChip(
                    label = "Lari Landas",
                    value = "${"%.1f".format(result.approachSpeedMs)}m/s",
                    sub = "Kelajuan papan",
                    color = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
                SandMetricChip(
                    label = "Sudut Lonjak",
                    value = "${"%.1f".format(result.takeoffAngleDeg)}°",
                    sub = "Ideal 19-23°",
                    color = SpeedAmber,
                    modifier = Modifier.weight(1f)
                )
                SandMetricChip(
                    label = "Sentuh Papan",
                    value = "${result.boardContactTimeMs}ms",
                    sub = "Tinggi ${result.flightPeakHeightCm}cm",
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }

            result.tripleJumpPhaseRatio?.let { ratioText ->
                Surface(
                    color = ElectricCyan.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Nisbah Fasa Lompat Kijang AI: $ratioText",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // CORRECT BIOMECHANICS & POSTURE
            Surface(
                color = StadiumSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "✓ Bacaan Sains Sukan & Postur Betul:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = SuccessGreen
                    )
                    Text(
                        text = result.correctTechniqueAnalysis,
                        fontSize = 9.sp,
                        color = Color.White,
                        lineHeight = 13.sp
                    )
                }
            }

            // WRONG POSTURE / DISTANCE LOSS ANALYSIS
            Surface(
                color = StadiumSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, LaserOrange.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "⚠ Analisis Kesalahan Papan / Postur & Kerugian Jarak:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = LaserOrange
                    )
                    Text(
                        text = result.wrongPostureAndLossAnalysis,
                        fontSize = 9.sp,
                        color = Color(0xFFFCA5A5),
                        lineHeight = 13.sp
                    )
                }
            }

            // NEXT CORRECTIVE DRILLS
            Surface(
                color = StadiumSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SpeedAmber.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "➡ Cadangan Sains Sukan & Latihan Seterusnya:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = SpeedAmber
                    )
                    result.nextActionDrills.forEach { drill ->
                        Text(
                            text = "• $drill",
                            fontSize = 9.sp,
                            color = Color.White,
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            // PREVIOUS JUMPS HISTORY PILLS
            if (allHistory.size > 1) {
                Text(
                    text = "Rekod Lompatan Sebelumnya (${allHistory.size}):",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = SilverMetallic
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allHistory.take(4).forEach { item ->
                        val isCurrent = item.id == result.id
                        Surface(
                            color = if (isCurrent) ElectricCyan.copy(alpha = 0.2f) else StadiumSurfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (isCurrent) ElectricCyan else StadiumBorder),
                            modifier = Modifier.clickable { onSelectHistoryItem(item) }
                        ) {
                            Text(
                                text = "${item.jumpTitle}: ${"%.2f".format(item.measuredDistanceMeters)}m",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) ElectricCyan else Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SandMetricChip(
    label: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 8.sp, color = TextSecondary, maxLines = 1)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Black, color = color, maxLines = 1)
            Text(sub, fontSize = 7.sp, color = TextMuted, maxLines = 1)
        }
    }
}
