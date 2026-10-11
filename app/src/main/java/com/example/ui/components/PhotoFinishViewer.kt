package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RaceRecordEntity
import com.example.ui.theme.*
import java.io.File

@Composable
fun PhotoFinishViewer(
    race: RaceRecordEntity,
    onCapturePhotoFinish: (currentFrameMs: Long, isCam2: Boolean) -> Unit,
    onDownloadToPhone: (isCam2: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedCam by remember { mutableStateOf(2) } // Default Cam 2 (Finish Torso)
    val baseTimeMs = race.recordedTimeMillis
    var currentFrameOffsetMs by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var finishLineXRatio by remember { mutableStateOf(0.55f) } // Adjustable red finish slit cursor
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Load actual saved Photo Finish Image from device storage if available
    val savedBitmap = remember(race.photoFinishUri) {
        race.photoFinishUri?.let { path ->
            val file = File(path)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null
        }
    }

    val displayedTimeMs = (baseTimeMs + currentFrameOffsetMs).coerceAtLeast(0L)
    val dispHours = (displayedTimeMs / 3600000).toInt()
    val dispMins = ((displayedTimeMs % 3600000) / 60000).toInt()
    val dispSecs = ((displayedTimeMs % 60000) / 1000).toInt()
    val dispMillis = (displayedTimeMs % 1000).toInt()
    val formattedDisplayTime = String.format("%d:%02d:%02d:%03d", dispHours, dispMins, dispSecs, dispMillis)

    // Playback loop
    LaunchedEffect(isPlaying, playbackSpeed) {
        if (isPlaying) {
            while (isPlaying) {
                kotlinx.coroutines.delay((33 / playbackSpeed).toLong())
                currentFrameOffsetMs += 33
                if (currentFrameOffsetMs > 3000) {
                    currentFrameOffsetMs = -2000
                }
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Title, Close button, Camera Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${race.title} - ${race.athleteName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "${race.eventName} • Lorong ${race.lane} • Angin: ${race.windReading}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Cam 1 vs Cam 2 Tab Switcher
                    Row(
                        modifier = Modifier
                            .background(StadiumSurfaceVariant, RoundedCornerShape(20.dp))
                            .padding(2.dp)
                    ) {
                        FilterChip(
                            selected = selectedCam == 1,
                            onClick = { selectedCam = 1 },
                            label = { Text("Cam 1 (Start)", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FinishLineRed,
                                selectedLabelColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        FilterChip(
                            selected = selectedCam == 2,
                            onClick = { selectedCam = 2 },
                            label = { Text("Cam 2 (Torso)", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LaserOrange,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    if (onClose != null) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(32.dp)
                                .background(StadiumSurfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // VIDEO / PHOTO FINISH SCREEN (NO CARTOONS - REAL CAPTURE DISPLAY)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(2.dp, if (selectedCam == 2) FinishLineRed else SilverBorder, RoundedCornerShape(12.dp))
            ) {
                if (savedBitmap != null) {
                    // Render actual real captured photo finish image saved from camera
                    Image(
                        bitmap = savedBitmap.asImageBitmap(),
                        contentDescription = "Real Photo Finish Frame",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // High-Tech Realistic Track Finish Line Slit-Scan View (Zero cartoons)
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { _, dragAmount ->
                                    finishLineXRatio = (finishLineXRatio + dragAmount.x / size.width).coerceIn(0.1f, 0.9f)
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height

                        // Stadium Track Lanes with realistic athletics synthetic track color
                        drawRect(color = Color(0xFF1E293B), size = size)

                        // 8 Lane markings
                        for (i in 1..8) {
                            val y = i * (h / 8f)
                            drawLine(
                                color = Color.White.copy(alpha = 0.25f),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1.5f
                            )
                        }

                        // Slit Scan Finish Line (Garisan Penamat Merah)
                        val slitX = w * finishLineXRatio
                        drawLine(
                            color = FinishLineRed,
                            start = Offset(slitX, 0f),
                            end = Offset(slitX, h),
                            strokeWidth = 4f
                        )
                        // Sensor bead indicator
                        drawCircle(
                            color = FinishLineRed,
                            radius = 8f,
                            center = Offset(slitX, 20f)
                        )

                        // AI Torso Bounding Box (Only if motion detected)
                        if (race.hasMotionDetected) {
                            val boxWidth = 90f
                            val boxHeight = 140f
                            val boxLeft = slitX - (boxWidth / 2f)
                            val boxTop = (h / 2f) - (boxHeight / 2f)

                            drawRect(
                                color = ElectricCyan.copy(alpha = 0.15f),
                                topLeft = Offset(boxLeft, boxTop),
                                size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight)
                            )
                        }
                    }
                }

                // Top Left Overlay: Camera Perspective Badge
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = if (selectedCam == 1) "CAM 1: STARTING LINE TIMING GATE" else "CAM 2: FINISH LINE TORSO SLIT GATE",
                        color = if (selectedCam == 1) Color.White else FinishLineRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Top Right: High-Precision Electronic Timing Readout
                Surface(
                    color = Color.Black.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FinishLineRed),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = formattedDisplayTime,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = FinishLineRed,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                // Drag Slit Indicator prompt
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Seret Garis Merah untuk sesuaikan torso",
                        color = SilverMetallic,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI INTELLIGENT TORSO DETECTION STATUS (Real Analysis vs "No detection save")
            Surface(
                color = if (race.hasMotionDetected) StadiumSurfaceVariant else Color.Black,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (race.hasMotionDetected) SuccessGreen.copy(alpha = 0.5f) else TextMuted.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (race.hasMotionDetected) Icons.Default.Psychology else Icons.Default.SensorsOff,
                        contentDescription = null,
                        tint = if (race.hasMotionDetected) SuccessGreen else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (race.hasMotionDetected) {
                            Text(
                                text = "KOTAK AI TORSO: ATLETIK DIKESAN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = SuccessGreen
                            )
                            Text(
                                text = "Kecondongan Torso: ${race.torsoLeanAngleDeg}° • Sentuhan Bumi: ${race.groundContactTimeMs}ms • Cadence: ${race.cadenceSpM} spm. Condongan torso melepasi garisan adalah sah.",
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "No detection save",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            Text(
                                text = "Tiada pergerakan manusia dikesan pada garisan slit dalam rakaman ini.",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // FRAME-BY-FRAME CONTROLS: FORWARD / REVERSE 1 SEC & PER FRAME (+33ms)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reverse 1 Second (-1000ms)
                OutlinedButton(
                    onClick = { currentFrameOffsetMs -= 1000 },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SilverBorder),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.FastRewind, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("-1s", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Step Back 1 Frame (-33ms)
                IconButton(
                    onClick = { currentFrameOffsetMs -= 33 },
                    modifier = Modifier
                        .size(38.dp)
                        .background(StadiumSurfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Undur 1 Frame (-33ms)", tint = ElectricCyan)
                }

                // Play / Pause Toggle
                FilledIconButton(
                    onClick = { isPlaying = !isPlaying },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isPlaying) LaserOrange else FinishLineRed
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Jeda" else "Mainkan",
                        tint = Color.White
                    )
                }

                // Step Forward 1 Frame (+33ms)
                IconButton(
                    onClick = { currentFrameOffsetMs += 33 },
                    modifier = Modifier
                        .size(38.dp)
                        .background(StadiumSurfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Maju 1 Frame (+33ms)", tint = ElectricCyan)
                }

                // Forward 1 Second (+1000ms)
                OutlinedButton(
                    onClick = { currentFrameOffsetMs += 1000 },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SilverBorder),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("+1s", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Speed Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Kelajuan: ", fontSize = 11.sp, color = TextSecondary)
                listOf(0.25f, 0.5f, 1.0f).forEach { speed ->
                    FilterChip(
                        selected = playbackSpeed == speed,
                        onClick = { playbackSpeed = speed },
                        label = { Text("${speed}x", fontSize = 10.sp) },
                        modifier = Modifier.padding(horizontal = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Capture Photo Finish, Download ke Fon, Padam Video
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onCapturePhotoFinish(displayedTimeMs, selectedCam == 2)
                        snackbarMessage = "Photo Finish Rasmi berjaya dijana & disimpan!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Capture Torso", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = {
                        onDownloadToPhone(selectedCam == 2)
                        snackbarMessage = "Fail disimpan ke /PsycoTimeXPro/Videos/"
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SilverBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                if (onDelete != null) {
                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Padam", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            snackbarMessage?.let { msg ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "✓ $msg",
                    color = SuccessGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (showDeleteConfirmDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Padam Rakaman Video?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Adakah anda pasti mahu memadam rakaman '${race.title}' ini? Tindakan ini akan menjimatkan storan peranti anda.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed)
                ) {
                    Text("Ya, Padam", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal", color = SilverMetallic)
                }
            }
        )
    }
}
