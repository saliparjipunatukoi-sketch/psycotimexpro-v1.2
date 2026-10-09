package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RaceRecordEntity
import com.example.ui.theme.*

@Composable
fun PhotoFinishViewer(
    race: RaceRecordEntity,
    onCapturePhotoFinish: (currentFrameMs: Long, isCam2: Boolean) -> Unit,
    onDownloadToPhone: (isCam2: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCam by remember { mutableStateOf(2) } // Default Cam 2 (Finish Torso)
    val baseTimeMs = race.recordedTimeMillis
    var currentFrameOffsetMs by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var finishLineXRatio by remember { mutableStateOf(0.55f) } // Adjustable finish slit cursor
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

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
                if (currentFrameOffsetMs > 2500) {
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
            // Header: Title and Camera Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = race.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${race.eventName} • Lorong ${race.lane} • Angin: ${race.windReading}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Cam 1 vs Cam 2 Tab Switcher
                Row(
                    modifier = Modifier
                        .background(StadiumSurfaceVariant, RoundedCornerShape(20.dp))
                        .padding(3.dp)
                ) {
                    FilterChip(
                        selected = selectedCam == 1,
                        onClick = { selectedCam = 1 },
                        label = { Text("CAM 1 (Timer)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan,
                            selectedLabelColor = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = selectedCam == 2,
                        onClick = { selectedCam = 2 },
                        label = { Text("CAM 2 (Torso Gate)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LaserOrange,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // High Precision Video / Slit Scan Surface with Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF030712))
                    .border(2.dp, if (selectedCam == 2) LaserOrange else ElectricCyan, RoundedCornerShape(12.dp))
            ) {
                // Track Canvas simulation with Torso & Finish Line
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                // Allow dragging the finish slit cursor across screen
                                finishLineXRatio = (finishLineXRatio + dragAmount.x / size.width).coerceIn(0.1f, 0.9f)
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Draw track lane lanes
                    for (i in 1..6) {
                        val y = i * (h / 6f)
                        drawLine(
                            color = Color(0xFF1F2937),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 2f
                        )
                    }

                    // Runner movement simulation based on currentFrameOffsetMs
                    val normalizedProgress = ((currentFrameOffsetMs + 2000f) / 4500f).coerceIn(0f, 1f)
                    val runnerX = w * normalizedProgress
                    val runnerY = h * 0.45f

                    // Draw Runner Silhouette
                    val runnerColor = if (selectedCam == 2) Color(0xFF22D3EE) else Color(0xFFF97316)
                    // Head
                    drawCircle(
                        color = Color.White,
                        radius = 16f,
                        center = Offset(runnerX + 18f, runnerY - 45f)
                    )
                    // Torso with sprint lean
                    drawLine(
                        color = runnerColor,
                        start = Offset(runnerX - 10f, runnerY + 25f),
                        end = Offset(runnerX + 22f, runnerY - 25f),
                        strokeWidth = 16f
                    )
                    // Legs & Arms
                    drawLine(color = runnerColor, start = Offset(runnerX - 10f, runnerY + 25f), end = Offset(runnerX + 35f, runnerY + 80f), strokeWidth = 8f)
                    drawLine(color = runnerColor, start = Offset(runnerX - 10f, runnerY + 25f), end = Offset(runnerX - 35f, runnerY + 75f), strokeWidth = 8f)

                    if (selectedCam == 2) {
                        // CAM 2: Laser Finish Slit Line (Red/Orange)
                        val slitX = w * finishLineXRatio
                        drawLine(
                            color = Color(0xFFEF4444),
                            start = Offset(slitX, 0f),
                            end = Offset(slitX, h),
                            strokeWidth = 4f
                        )
                        // Sensor cursor circle
                        drawCircle(
                            color = Color(0xFFEF4444),
                            radius = 10f,
                            center = Offset(slitX, 25f)
                        )
                    }
                }

                // Top Left Overlay: Camera perspective badge
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = if (selectedCam == 1) "CAM 1: STARTER & TIMER GATE" else "CAM 2: FINISHER TORSO HIGH-SPEED SLIT",
                        color = if (selectedCam == 1) ElectricCyan else LaserOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Top Right: Live Overlay Timer (Item 13)
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = formattedDisplayTime,
                        color = SpeedAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Bottom Center hint
                if (selectedCam == 2) {
                    Text(
                        text = "◄ Tarik garisan merah untuk selaraskan Torso pelari ►",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Frame-by-Frame Scrubbing Controls (Item 13)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Back 1 Frame (-10ms)
                IconButton(
                    onClick = { currentFrameOffsetMs -= 10 },
                    modifier = Modifier
                        .testTag("frame_step_back_button")
                        .size(42.dp)
                        .background(StadiumSurfaceVariant, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Undur 1 Frame (-10ms)",
                        tint = ElectricCyan
                    )
                }

                // Step Back Fine (-1ms)
                OutlinedButton(
                    onClick = { currentFrameOffsetMs -= 1 },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("-1ms", fontSize = 11.sp)
                }

                // Play / Pause Toggle
                FilledIconButton(
                    onClick = { isPlaying = !isPlaying },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isPlaying) LaserOrange else ElectricCyan
                    ),
                    modifier = Modifier
                        .testTag("play_pause_button")
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Jeda" else "Mainkan",
                        tint = Color.Black
                    )
                }

                // Step Forward Fine (+1ms)
                OutlinedButton(
                    onClick = { currentFrameOffsetMs += 1 },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("+1ms", fontSize = 11.sp)
                }

                // Step Forward 1 Frame (+10ms)
                IconButton(
                    onClick = { currentFrameOffsetMs += 10 },
                    modifier = Modifier
                        .testTag("frame_step_forward_button")
                        .size(42.dp)
                        .background(StadiumSurfaceVariant, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Maju 1 Frame (+10ms)",
                        tint = ElectricCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                        label = { Text("${speed}x", fontSize = 11.sp) },
                        modifier = Modifier.padding(horizontal = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Capture Photo Finish & Download ke Telefon (Items 8 & 13)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onCapturePhotoFinish(displayedTimeMs, selectedCam == 2)
                        snackbarMessage = "Photo Finish Rasmi berjaya dijana & disimpan!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LaserOrange),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .testTag("capture_photo_finish_button")
                        .weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Capture Photo Finish", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        onDownloadToPhone(selectedCam == 2)
                        snackbarMessage = "Video Cam $selectedCam dimuat turun ke folder /PsycoTimeXPro/Videos/"
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan),
                    modifier = Modifier
                        .testTag("download_video_button")
                        .weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download ke Fon", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Status message
            snackbarMessage?.let { msg ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "✓ $msg",
                    color = SuccessGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
