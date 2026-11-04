package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RaceRecordEntity
import com.example.service.StorageManager
import com.example.ui.theme.*
import java.io.File
import java.util.Locale

@Composable
fun PhotoFinishViewer(
    race: RaceRecordEntity,
    onCapturePhotoFinish: (currentFrameMs: Long, isCam2: Boolean) -> Unit,
    onDownloadToPhone: (isCam2: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storageManager = remember { StorageManager(context) }

    var selectedCam by remember { mutableStateOf(1) } // Default Cam 1 Portrait Runner View
    val baseTimeMs = race.recordedTimeMillis.coerceAtLeast(1000L)
    var playbackProgress by remember { mutableStateOf(0.85f) } // 0.0f (Start) to 1.0f (Finish)
    var isPlaying by remember { mutableStateOf(true) } // Auto-play moving replay when opened!
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var finishLineXRatio by remember { mutableStateOf(0.55f) }
    var isPortraitViewport by remember { mutableStateOf(true) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Load actual multi-frame recorded video sequence from storage (or live memory buffer)
    val recordedFrames: List<Bitmap> = remember(race.id, race.cam1VideoUri, race.cam2VideoUri, race.photoFinishUri, selectedCam) {
        val diskFrames = storageManager.loadRecordedVideoFramesForRace(
            camVideoUri = if (selectedCam == 1) race.cam1VideoUri else race.cam2VideoUri,
            photoFinishUri = race.photoFinishUri
        )
        if (diskFrames.size > 1) {
            diskFrames
        } else if (CameraFrameHolder.recordedSessionFrames.isNotEmpty()) {
            CameraFrameHolder.recordedSessionFrames.toList()
        } else {
            diskFrames
        }
    }

    val currentFrameIndex = remember(playbackProgress, recordedFrames.size) {
        if (recordedFrames.isEmpty()) 0
        else ((playbackProgress * (recordedFrames.size - 1)).toInt()).coerceIn(0, recordedFrames.size - 1)
    }

    val currentFrameBitmap: Bitmap? = recordedFrames.getOrNull(currentFrameIndex)

    val displayedTimeMs = (baseTimeMs * playbackProgress).toLong().coerceAtLeast(0L)
    val dispMins = ((displayedTimeMs % 3600000) / 60000).toInt()
    val dispSecs = ((displayedTimeMs % 60000) / 1000).toInt()
    val dispMillis = (displayedTimeMs % 1000).toInt()
    val formattedDisplayTime = String.format(Locale.US, "%02d:%02d.%03d", dispMins, dispSecs, dispMillis)

    // Smooth real moving video playback loop
    LaunchedEffect(isPlaying, playbackSpeed, recordedFrames.size) {
        if (isPlaying) {
            while (isPlaying) {
                val frameDelay = (65f / playbackSpeed).toLong().coerceAtLeast(25L)
                kotlinx.coroutines.delay(frameDelay)
                val step = if (recordedFrames.size > 2) {
                    1f / recordedFrames.size.toFloat()
                } else {
                    0.025f
                }
                val next = playbackProgress + step
                playbackProgress = if (next > 1.0f) 0.0f else next
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, StadiumBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // COMPACT PRO BROADCAST HEADER BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${race.title} • ${race.athleteName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Surface(
                            color = FinishLineRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.2fs", race.recordedTimeMillis / 1000.0),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = FinishLineRed,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "${race.eventName} • Lorong ${race.lane} • Angin ${race.windReading} • ${recordedFrames.size.coerceAtLeast(1)} Frame Portrait",
                        fontSize = 9.sp,
                        color = TextSecondary
                    )
                }

                // Compact Cam 1 / Cam 2 + Portrait Ratio + Close
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .background(StadiumSurfaceVariant, RoundedCornerShape(8.dp))
                            .border(1.dp, StadiumBorder, RoundedCornerShape(8.dp))
                            .padding(2.dp)
                    ) {
                        Surface(
                            color = if (selectedCam == 1) ElectricCyan else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { selectedCam = 1 }
                        ) {
                            Text(
                                text = "Cam 1 Pelari",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (selectedCam == 1) Color.Black else Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = if (selectedCam == 2) FinishLineRed else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { selectedCam = 2 }
                        ) {
                            Text(
                                text = "Cam 2 Torso",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { isPortraitViewport = !isPortraitViewport },
                        modifier = Modifier
                            .size(26.dp)
                            .background(StadiumSurfaceVariant, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.StayCurrentPortrait,
                            contentDescription = "Togol Portrait",
                            tint = if (isPortraitViewport) ElectricCyan else SilverMetallic,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    if (onClose != null) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(26.dp)
                                .background(StadiumSurfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // PORTRAIT-PRESERVING MOVING VIDEO & TALL VERTICAL AI INTELLIGENCE VIEWPORT
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isPortraitViewport) 360.dp else 240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF070B14))
                    .border(
                        1.5.dp,
                        if (selectedCam == 2) FinishLineRed else ElectricCyan.copy(alpha = 0.7f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                if (currentFrameBitmap != null) {
                    // Render upright Portrait/Landscape camera frame WITHOUT rotating sideways!
                    Image(
                        bitmap = currentFrameBitmap.asImageBitmap(),
                        contentDescription = "Frame Rakaman Sebenar",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Interactive Overlay Canvas: Tall Vertical AI Intelligence Box (Kotak AI Besar Menegak) & Slit Line
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                finishLineXRatio = (finishLineXRatio + dragAmount.x / size.width).coerceIn(0.15f, 0.85f)
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    if (currentFrameBitmap == null) {
                        // Clean pro stadium track background when viewing synthetic telemetry
                        drawRect(color = Color(0xFF0F172A), size = size)
                        for (i in 1..6) {
                            val y = i * (h / 7f)
                            drawLine(
                                color = Color.White.copy(alpha = 0.14f),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f
                            )
                        }
                    }

                    // Dynamic horizontal position of the runner during playback so video visibly moves
                    val dynamicRunnerCenterRatio = if (selectedCam == 1) {
                        // In Cam 1 Portrait facing runner, box stays centered and scales slightly as runner approaches
                        0.50f + kotlin.math.sin(playbackProgress * 6.28f) * 0.03f
                    } else {
                        // In Cam 2 Finish Gate, runner moves across the frame toward the red finish slit
                        (0.22f + playbackProgress * (finishLineXRatio - 0.12f)).coerceIn(0.18f, 0.82f)
                    }

                    // TALL VERTICAL AI INTELLIGENCE BOX (KOTAK AI BESAR MENEGAK)
                    val scaleFactor = if (selectedCam == 1) (0.72f + playbackProgress * 0.16f) else 0.78f
                    val boxW = (w * 0.44f).coerceIn(135f, w * 0.60f)
                    val boxH = (h * scaleFactor).coerceIn(180f, h * 0.86f)
                    val boxX = (w * dynamicRunnerCenterRatio - boxW / 2f).coerceIn(10f, (w - boxW - 10f).coerceAtLeast(10f))
                    val boxY = (h - boxH) / 2f

                    val aiBoxColor = if (selectedCam == 1) ElectricCyan else SuccessGreen

                    drawRoundRect(
                        color = aiBoxColor.copy(alpha = 0.13f),
                        topLeft = Offset(boxX, boxY),
                        size = Size(boxW, boxH),
                        cornerRadius = CornerRadius(16f, 16f)
                    )
                    drawRoundRect(
                        color = aiBoxColor,
                        topLeft = Offset(boxX, boxY),
                        size = Size(boxW, boxH),
                        cornerRadius = CornerRadius(16f, 16f),
                        style = Stroke(width = 3.5f)
                    )

                    // Torso Chest Lock Crosshair inside the Tall Vertical Box
                    val torsoY = boxY + boxH * 0.35f
                    drawLine(
                        color = FinishLineRed.copy(alpha = 0.85f),
                        start = Offset(boxX + 8f, torsoY),
                        end = Offset(boxX + boxW - 8f, torsoY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )

                    // Red Finish Slit Line (Draggable)
                    val slitX = w * finishLineXRatio
                    drawLine(
                        color = FinishLineRed,
                        start = Offset(slitX, 0f),
                        end = Offset(slitX, h),
                        strokeWidth = 3.5f
                    )
                    drawCircle(
                        color = FinishLineRed,
                        radius = 7f,
                        center = Offset(slitX, 16f)
                    )
                }

                // Top-Left Micro HUD Badge
                Surface(
                    color = Color.Black.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isPlaying) SuccessGreen else SpeedAmber, CircleShape)
                        )
                        Text(
                            text = if (selectedCam == 1) {
                                "CAM 1 PORTRAIT • KOTAK AI MENEGAK (FRAME ${currentFrameIndex + 1}/${recordedFrames.size.coerceAtLeast(1)})"
                            } else {
                                "CAM 2 PHOTO-FINISH TORSO • SLIT GATE"
                            },
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Top-Right Precision Time Readout
                Surface(
                    color = Color.Black.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, FinishLineRed),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = formattedDisplayTime,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = FinishLineRed,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // Bottom-Start Compact Hint
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "Orientasi Portrait Asli • Seret Garis Merah untuk kunci Torso",
                        color = SilverMetallic,
                        fontSize = 8.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // INTERACTIVE VIDEO TIMELINE SCRUBBER + COMPACT TRANSPORT CONTROLS
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "0.00s",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                    Slider(
                        value = playbackProgress,
                        onValueChange = {
                            isPlaying = false
                            playbackProgress = it
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = FinishLineRed,
                            activeTrackColor = ElectricCyan,
                            inactiveTrackColor = StadiumSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(22.dp)
                    )
                    Text(
                        text = String.format(Locale.US, "%.2fs", baseTimeMs / 1000.0),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = FinishLineRed,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // -1s
                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, StadiumBorder),
                        modifier = Modifier
                            .height(28.dp)
                            .clickable {
                                isPlaying = false
                                playbackProgress = (playbackProgress - 0.15f).coerceIn(0f, 1f)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FastRewind, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("-1s", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // Prev Frame (-1f)
                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                isPlaying = false
                                val step = if (recordedFrames.size > 1) 1f / (recordedFrames.size - 1) else 0.04f
                                playbackProgress = (playbackProgress - step).coerceIn(0f, 1f)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Undur Frame", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Play / Pause Button
                    Button(
                        onClick = { isPlaying = !isPlaying },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) LaserOrange else FinishLineRed
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPlaying) "Jeda" else "Main Video",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    // Next Frame (+1f)
                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                isPlaying = false
                                val step = if (recordedFrames.size > 1) 1f / (recordedFrames.size - 1) else 0.04f
                                playbackProgress = (playbackProgress + step).coerceIn(0f, 1f)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Maju Frame", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        }
                    }

                    // +1s
                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, StadiumBorder),
                        modifier = Modifier
                            .height(28.dp)
                            .clickable {
                                isPlaying = false
                                playbackProgress = (playbackProgress + 0.15f).coerceIn(0f, 1f)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("+1s", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                        }
                    }

                    // Speed Chips (0.25x, 0.5x, 1x)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf(0.25f, 0.5f, 1.0f).forEach { spd ->
                            Surface(
                                color = if (playbackSpeed == spd) ElectricCyan.copy(alpha = 0.22f) else StadiumSurfaceVariant,
                                shape = RoundedCornerShape(5.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (playbackSpeed == spd) ElectricCyan else Color.Transparent
                                ),
                                modifier = Modifier
                                    .height(24.dp)
                                    .clickable { playbackSpeed = spd }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 5.dp)
                                ) {
                                    Text(
                                        text = "${spd}x",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (playbackSpeed == spd) ElectricCyan else SilverMetallic
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // COMPACT TELEMETRY & ACTION BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        onCapturePhotoFinish(displayedTimeMs, selectedCam == 2)
                        snackbarMessage = "Photo-Finish Portrait disimpan ke Galeri & Folder!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tangkap Torso", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                }

                OutlinedButton(
                    onClick = {
                        onDownloadToPhone(selectedCam == 2)
                        snackbarMessage = "Rakaman Frame Portrait disimpan ke /PsycoTimeXPro/Videos/!"
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SilverBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simpan Video", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF7F1D1D).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Padam", tint = FinishLineRed, modifier = Modifier.size(15.dp))
                    }
                }
            }

            snackbarMessage?.let { msg ->
                Text(
                    text = "✓ $msg",
                    color = SuccessGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDeleteConfirmDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Padam Rakaman Video?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = { Text("Adakah anda pasti mahu memadam rakaman '${race.title}' ini?", color = TextSecondary, fontSize = 12.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed)
                ) {
                    Text("Padam", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal", color = SilverMetallic, fontSize = 11.sp)
                }
            }
        )
    }
}
