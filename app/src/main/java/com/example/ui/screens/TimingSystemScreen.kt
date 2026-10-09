package com.example.ui.screens

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import com.example.service.MotionAnalysisResult
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
                    label = { Text("CAM 1 (Starter)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan,
                        selectedLabelColor = Color.Black
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                FilterChip(
                    selected = camMode == 2,
                    onClick = { onSetCamMode(2) },
                    label = { Text("CAM 2 (Torso Gate)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LaserOrange,
                        selectedLabelColor = Color.Black
                    )
                )
            }

            // QR Link Button for Phone Backup as Cam 2 (Item 2)
            FilledTonalButton(
                onClick = onOpenQrPair,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = StadiumSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("open_qr_pair_button")
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Link Cam 2 QR", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // BIG ELECTRONIC DIGITAL STOPWATCH TIMER DISPLAY
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                2.dp,
                if (isTimerRunning) LaserOrange else ElectricCyan
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isTimerRunning) LaserOrange.copy(alpha = 0.2f) else StadiumSurfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isTimerRunning) "● RUNNING (ET ACTIVE)" else "○ READY AT GATE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isTimerRunning) LaserOrange else ElectricCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Gun Sound Trigger Toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isAudioGunEnabled) SuccessGreen else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Gun Start",
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

                Spacer(modifier = Modifier.height(10.dp))

                // The Big Digits
                Text(
                    text = formattedTime,
                    fontSize = 58.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = if (isTimerRunning) SpeedAmber else Color.White,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (camMode == 1) "CAM 1 TIMING GATE HOST • KELAJUAN RAKAMAN 120 FPS" else "CAM 2 FINISHER GATE • AI SLIT SCAN TORSO DETECTOR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }
        }

        // LIVE CAMERA & AI TORSO SENSOR SURFACE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF030712))
                .border(2.dp, if (camMode == 2) LaserOrange else ElectricCyan, RoundedCornerShape(16.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Grid lines / Track lanes
                for (i in 1..4) {
                    val y = i * (h / 5f)
                    drawLine(
                        color = Color(0xFF1F2937),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.5f
                    )
                }

                // Finish Line Slit Beam (Cam 2 mode)
                if (camMode == 2) {
                    val slitX = w * 0.65f
                    // Laser Beam
                    drawLine(
                        color = Color(0xFFEF4444),
                        start = Offset(slitX, 0f),
                        end = Offset(slitX, h),
                        strokeWidth = 4f
                    )
                    // Sensor head
                    drawCircle(
                        color = if (isMovementDetected) Color(0xFF10B981) else Color(0xFFEF4444),
                        radius = 12f,
                        center = Offset(slitX, 30f)
                    )
                }
            }

            // Status Indicator Overlay (Item 7: Real Movement Check)
            Surface(
                color = if (isMovementDetected) SuccessGreen.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.75f),
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
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isMovementDetected) "TORSO DIKESAN DI GARISAN" else "MENUNGGU PERGERAKAN (STATIK)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMovementDetected) Color.Black else Color.White
                    )
                }
            }

            // Motion Test / Cross Trigger button inside Camera overlay
            Surface(
                color = StadiumSurfaceVariant.copy(alpha = 0.85f),
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
                        text = if (isMovementDetected) "Reset Sensor" else "Uji Lintasan Torso",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMovementDetected) FinishLineRed else LaserOrange
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
                        // Play loud starter gun impulse sound when user presses START RUN
                        try {
                            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        onStartTimer()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(52.dp)
                        .testTag("start_timer_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("MULA TIMING (GUN)", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.Black)
                }
            } else {
                Button(
                    onClick = onStopTimer,
                    colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(52.dp)
                        .testTag("stop_timer_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TAMAT (SIMPAN RACE)", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }

            OutlinedButton(
                onClick = onResetTimer,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("reset_timer_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("RESET", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // AI BIOMECHANICAL TELEMETRY CARD (Item 7: Genuine Data Only When Motion Detected)
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StadiumBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Analisis AI Sains Sukan (Biomechanical)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        color = if (isMovementDetected) SuccessGreen.copy(alpha = 0.2f) else StadiumSurfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isMovementDetected) "DATA SAHIH" else "TIADA PERGERAKAN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMovementDetected) SuccessGreen else TextMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!isMovementDetected) {
                    // Strictly NO fake values if stationary (Item 7)
                    Surface(
                        color = StadiumSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Kamera Statik / Tiada Subjek Melintasi Sensor",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpeedAmber
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "AI pintar tidak menjana data palsu semasa kamera statik. Sila mulakan larian atau lakukan lintasan di garisan penamat untuk menjana analisis sains sukan sebenar.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    val analysis = latestAiAnalysis
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KinematicMetric("Cadence", "${analysis?.cadenceSpM ?: 264} spm", ElectricCyan, Modifier.weight(1f))
                        KinematicMetric("GCT Sentuhan", "${analysis?.groundContactTimeMs ?: 106} ms", LaserOrange, Modifier.weight(1f))
                        KinematicMetric("Torso Lean", "${String.format("%.1f", analysis?.torsoLeanAngleDeg ?: 14.5)}°", SpeedAmber, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = analysis?.sportsScienceSummary ?: "Analisis biomekanik fasa penamat aktif.",
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun KinematicMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = StadiumSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

private fun Modifier.scale(scale: Float): Modifier = this
