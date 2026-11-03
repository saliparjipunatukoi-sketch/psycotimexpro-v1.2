package com.example.ui.dialogs

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RaceRecordEntity
import com.example.service.AiSportsScienceAnalyzer
import com.example.service.StartFinishPostureAnalysis
import com.example.ui.theme.*
import java.io.File

/**
 * Full-featured AI Intelligence Posture Reader & Slit Photo-Finish Export Inspector Dialog.
 * Displays:
 * 1. Exported Photo-Finish Slit Image preview & one-click local export
 * 2. Cam 1 Real Starting Body Posture analysis (Correct vs Wrong posture + Next Drills)
 * 3. Cam 2 Real Finishing Body Posture analysis (Correct vs Wrong posture + Next Drills)
 */
@Composable
fun AiPostureReaderDialog(
    race: RaceRecordEntity,
    exportedSlitPath: String?,
    onExportSlitNow: (RaceRecordEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val posture: StartFinishPostureAnalysis = remember(race) {
        AiSportsScienceAnalyzer.analyzePostureForRace(race)
    }

    val previewBitmap = remember(exportedSlitPath, race.photoFinishUri) {
        val candidatePath = exportedSlitPath ?: race.photoFinishUri
        candidatePath?.let { path ->
            val f = File(path)
            if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
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
                            .size(38.dp)
                            .background(ElectricCyan.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, ElectricCyan, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.AccessibilityNew,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AI Intelligence: Pembaca Postur & Slit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "${race.title} • ${race.athleteName} (${race.formattedTime})",
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
                // 1. PHOTO-FINISH SLIT IMAGE EXPORT CARD
                Surface(
                    color = StadiumSurfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, FinishLineRed.copy(alpha = 0.6f)),
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
                                Icon(Icons.Default.PhotoCameraFront, contentDescription = null, tint = FinishLineRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Imej Slit Photo-Finish Rasmi",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Surface(
                                color = FinishLineRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "SLIT 1280x720 PNG",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = FinishLineRed,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Exported Slit Photo Finish",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(145.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, FinishLineRed, RoundedCornerShape(10.dp))
                            )
                        }

                        if (exportedSlitPath != null) {
                            Surface(
                                color = SuccessGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "✓ Imej Slit Berjaya Dieksport ke Storan & Galeri:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                    Text(
                                        text = exportedSlitPath,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { onExportSlitNow(race) },
                            colors = ButtonDefaults.buttonColors(containerColor = FinishLineRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialog_export_slit_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (exportedSlitPath != null) "Eksport Semula Imej Slit Photo-Finish (PNG)" else "Eksport Imej Slit Photo-Finish Sekarang",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }

                // 2. OVERALL AI POSTURE SCORE & SUMMARY
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SKOR POSTUR BADAN AI (START + FINISH)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = ElectricCyan
                            )
                            Text(
                                text = "${posture.overallPostureScore}%",
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = if (posture.overallPostureScore >= 85) SuccessGreen else SpeedAmber
                            )
                        }

                        // Visual biomechanical skeleton posture comparison canvas
                        PostureSkeletonDiagram(
                            startAngleDeg = posture.startBodyLeanAngleDeg,
                            finishAngleDeg = posture.finishTorsoDipAngleDeg,
                            isStartCorrect = posture.isStartPostureCorrect,
                            isFinishCorrect = posture.isFinishPostureCorrect
                        )

                        Text(
                            text = posture.overallActionSummary,
                            fontSize = 11.sp,
                            color = Color.White,
                            lineHeight = 15.sp
                        )
                    }
                }

                // 3. CAM 1: STARTING BODY POSTURE ANALYSIS (WRONG VS CORRECT + NEXT STEPS)
                PosturePhaseCard(
                    phaseTitle = "1. Fasa Permulaan (CAM 1 — Blok & Pecutan Awal)",
                    statusBadge = posture.startPostureStatusBadge,
                    isCorrect = posture.isStartPostureCorrect,
                    metricsLine1 = "Sudut Condong Badan Start: ${"%.1f".format(posture.startBodyLeanAngleDeg)}° (Ideal: 42°–48°)",
                    metricsLine2 = "Sudut Lutut Blok: Depan ${posture.startFrontKneeAngleDeg}° / Belakang ${posture.startRearKneeAngleDeg}° • Leher: ${posture.startSpineAlignment}",
                    correctPostureTitle = "Analisis Postur Permulaan Betul (Correct Posture):",
                    correctPostureBody = posture.startCorrectPostureAnalysis,
                    wrongPostureTitle = "Analisis Postur Permulaan Salah (Wrong Posture):",
                    wrongPostureBody = posture.startWrongPostureAnalysis,
                    nextStepsTitle = "Tindakan & Latihan Pembetulan Start Seterusnya:",
                    nextStepsList = posture.startNextActionDrills
                )

                // 4. CAM 2: FINISH LINE BODY POSTURE ANALYSIS (WRONG VS CORRECT + NEXT STEPS)
                PosturePhaseCard(
                    phaseTitle = "2. Fasa Garisan Penamat (CAM 2 — Torso Slit Dip)",
                    statusBadge = posture.finishPostureStatusBadge,
                    isCorrect = posture.isFinishPostureCorrect,
                    metricsLine1 = "Sudut Condong Torso Penamat: ${"%.1f".format(posture.finishTorsoDipAngleDeg)}° (Ideal: 12°–18°)",
                    metricsLine2 = "Kestabilan Pinggul: ${posture.finishPelvicStabilityPct}% • Lengan: ${posture.finishArmDriveTechnique}",
                    correctPostureTitle = "Analisis Postur Penamat Betul (Correct Posture):",
                    correctPostureBody = posture.finishCorrectPostureAnalysis,
                    wrongPostureTitle = "Analisis Postur Penamat Salah (Wrong Posture):",
                    wrongPostureBody = posture.finishWrongPostureAnalysis,
                    nextStepsTitle = "Tindakan & Latihan Pembetulan Finish Seterusnya:",
                    nextStepsList = posture.finishNextActionDrills
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
            ) {
                Text("Tutup Laporan Postur AI", color = Color.Black, fontWeight = FontWeight.Black)
            }
        }
    )
}

@Composable
private fun PostureSkeletonDiagram(
    startAngleDeg: Double,
    finishAngleDeg: Double,
    isStartCorrect: Boolean,
    isFinishCorrect: Boolean
) {
    Surface(
        color = StadiumSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, StadiumBorder),
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left half: Cam 1 Start Drive Vector
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    // Ground line
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(10f, h - 12f),
                        end = Offset(w - 10f, h - 12f),
                        strokeWidth = 2f
                    )
                    // Ideal 45 deg reference dashed vector
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(w * 0.28f, h - 14f),
                        end = Offset(w * 0.68f, h * 0.22f),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                    // Measured athlete start body vector
                    val vectorColor = if (isStartCorrect) Color(0xFF10B981) else Color(0xFFF97316)
                    val rad = Math.toRadians(startAngleDeg.coerceIn(25.0, 75.0))
                    val len = h * 0.68f
                    val startX = w * 0.28f
                    val startY = h - 14f
                    val endX = startX + (kotlin.math.cos(rad) * len).toFloat()
                    val endY = startY - (kotlin.math.sin(rad) * len).toFloat()
                    drawLine(
                        color = vectorColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = vectorColor,
                        radius = 7f,
                        center = Offset(endX, endY)
                    )
                }
                Text(
                    text = "CAM 1 START: ${"%.1f".format(startAngleDeg)}°",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isStartCorrect) SuccessGreen else LaserOrange,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }

            Divider(
                color = StadiumBorder,
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
            )

            // Right half: Cam 2 Finish Torso Dip Vector
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val slitX = w * 0.62f
                    // Red Laser Finish Slit Line
                    drawLine(
                        color = Color(0xFFEF4444),
                        start = Offset(slitX, 6f),
                        end = Offset(slitX, h - 6f),
                        strokeWidth = 3.5f
                    )
                    // Athlete finish torso lean vector (measured from vertical)
                    val vectorColor = if (isFinishCorrect) Color(0xFF06B6D4) else Color(0xFFF97316)
                    val radFromVert = Math.toRadians(finishAngleDeg.coerceIn(2.0, 35.0))
                    val len = h * 0.65f
                    val hipX = slitX - 16f
                    val hipY = h - 16f
                    val chestX = hipX + (kotlin.math.sin(radFromVert) * len).toFloat()
                    val chestY = hipY - (kotlin.math.cos(radFromVert) * len).toFloat()
                    drawLine(
                        color = vectorColor,
                        start = Offset(hipX, hipY),
                        end = Offset(chestX, chestY),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = vectorColor,
                        radius = 7f,
                        center = Offset(chestX, chestY)
                    )
                }
                Text(
                    text = "CAM 2 FINISH: ${"%.1f".format(finishAngleDeg)}°",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isFinishCorrect) ElectricCyan else LaserOrange,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }
    }
}

@Composable
private fun PosturePhaseCard(
    phaseTitle: String,
    statusBadge: String,
    isCorrect: Boolean,
    metricsLine1: String,
    metricsLine2: String,
    correctPostureTitle: String,
    correctPostureBody: String,
    wrongPostureTitle: String,
    wrongPostureBody: String,
    nextStepsTitle: String,
    nextStepsList: List<String>
) {
    Surface(
        color = StadiumSurfaceVariant,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isCorrect) SuccessGreen.copy(alpha = 0.5f) else LaserOrange.copy(alpha = 0.6f)),
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
                    text = phaseTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = if (isCorrect) SuccessGreen.copy(alpha = 0.2f) else LaserOrange.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isCorrect) "BETUL ✓" else "PERLU BAIKI ⚠",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isCorrect) SuccessGreen else LaserOrange,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Measured Angles Box
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = metricsLine1,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                    Text(
                        text = metricsLine2,
                        fontSize = 10.sp,
                        color = SilverMetallic
                    )
                }
            }

            // CORRECT POSTURE ANALYSIS
            Surface(
                color = SuccessGreen.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = correctPostureTitle,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = SuccessGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = correctPostureBody,
                        fontSize = 10.sp,
                        color = Color.White,
                        lineHeight = 14.sp
                    )
                }
            }

            // WRONG POSTURE ANALYSIS
            Surface(
                color = FinishLineRed.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, FinishLineRed.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = FinishLineRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = wrongPostureTitle,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = FinishLineRed
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = wrongPostureBody,
                        fontSize = 10.sp,
                        color = Color.White,
                        lineHeight = 14.sp
                    )
                }
            }

            // WHAT MUST BE DONE NEXT (CORRECTIVE DRILLS)
            Surface(
                color = SpeedAmber.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SpeedAmber.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = SpeedAmber, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = nextStepsTitle,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = SpeedAmber
                        )
                    }
                    nextStepsList.forEach { step ->
                        Text(
                            text = "• $step",
                            fontSize = 10.sp,
                            color = Color.White,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
