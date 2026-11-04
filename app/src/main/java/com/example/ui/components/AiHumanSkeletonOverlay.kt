package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Draws the 17-Keypoint AI Human Biomechanical Skeleton, Limb Movement Lines (Arms, Torso, Knees, Feet),
 * Propulsion Velocity Vector Arrow, and Angle Arcs directly onto a Camera Monitor Canvas.
 */
fun DrawScope.drawAiHumanBiomechanicalSkeleton(
    skeleton: HumanSkeletonTelemetry,
    isJumpMode: Boolean = false
) {
    if (!skeleton.isHumanDetected) return

    val w = size.width
    val h = size.height

    fun pt(nx: Float, ny: Float) = Offset(nx * w, ny * h)

    val head = pt(skeleton.headX, skeleton.headY)
    val neck = pt(skeleton.neckX, skeleton.neckY)
    val lShoulder = pt(skeleton.leftShoulderX, skeleton.leftShoulderY)
    val rShoulder = pt(skeleton.rightShoulderX, skeleton.rightShoulderY)
    val lElbow = pt(skeleton.leftElbowX, skeleton.leftElbowY)
    val rElbow = pt(skeleton.rightElbowX, skeleton.rightElbowY)
    val lWrist = pt(skeleton.leftWristX, skeleton.leftWristY)
    val rWrist = pt(skeleton.rightWristX, skeleton.rightWristY)
    val hipCenter = pt(skeleton.hipCenterX, skeleton.hipCenterY)
    val lHip = pt(skeleton.leftHipX, skeleton.leftHipY)
    val rHip = pt(skeleton.rightHipX, skeleton.rightHipY)
    val lKnee = pt(skeleton.leftKneeX, skeleton.leftKneeY)
    val rKnee = pt(skeleton.rightKneeX, skeleton.rightKneeY)
    val lAnkle = pt(skeleton.leftAnkleX, skeleton.leftAnkleY)
    val rAnkle = pt(skeleton.rightAnkleX, skeleton.rightAnkleY)
    val lToe = pt(skeleton.leftFootToeX, skeleton.leftFootToeY)
    val rToe = pt(skeleton.rightFootToeX, skeleton.rightFootToeY)

    // 1. Verified Human Bounding Box with High-Tech Corner Brackets
    val boxL = skeleton.bboxLeft * w
    val boxT = skeleton.bboxTop * h
    val boxR = skeleton.bboxRight * w
    val boxB = skeleton.bboxBottom * h
    val boxW = (boxR - boxL).coerceAtLeast(50f)
    val boxH = (boxB - boxT).coerceAtLeast(90f)

    drawRoundRect(
        color = SuccessGreen.copy(alpha = 0.08f),
        topLeft = Offset(boxL, boxT),
        size = Size(boxW, boxH),
        cornerRadius = CornerRadius(12f, 12f)
    )
    drawRoundRect(
        color = SuccessGreen.copy(alpha = 0.65f),
        topLeft = Offset(boxL, boxT),
        size = Size(boxW, boxH),
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(
            width = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
        )
    )

    // 2. Vertical Reference Axis through Hip Center to visualize Torso Lean Angle
    drawLine(
        color = Color.White.copy(alpha = 0.45f),
        start = Offset(hipCenter.x, neck.y - 25f),
        end = Offset(hipCenter.x, hipCenter.y + 20f),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
    )

    // 3. GARIS BADAN / TORSO SPINE & SHOULDER/PELVIS GIRDLE (SpeedAmber)
    drawLine(color = SpeedAmber, start = head, end = neck, strokeWidth = 5f)
    drawLine(color = SpeedAmber, start = neck, end = hipCenter, strokeWidth = 6.5f)
    drawLine(color = SpeedAmber, start = lShoulder, end = rShoulder, strokeWidth = 4.5f)
    drawLine(color = SpeedAmber, start = lHip, end = rHip, strokeWidth = 4.5f)

    // 4. GARIS PERGERAKAN TANGAN KIRI & KANAN (ElectricCyan — Arm Drive Vectors)
    drawLine(color = ElectricCyan, start = lShoulder, end = lElbow, strokeWidth = 4.5f)
    drawLine(color = ElectricCyan, start = lElbow, end = lWrist, strokeWidth = 4.5f)
    drawLine(color = ElectricCyan, start = rShoulder, end = rElbow, strokeWidth = 4.5f)
    drawLine(color = ElectricCyan, start = rElbow, end = rWrist, strokeWidth = 4.5f)

    // Arm swing trajectory arc between wrists
    val armArcPath = Path().apply {
        moveTo(lWrist.x, lWrist.y)
        quadraticTo(
            neck.x,
            (lElbow.y + rElbow.y) * 0.5f + 16f,
            rWrist.x,
            rWrist.y
        )
    }
    drawPath(
        path = armArcPath,
        color = ElectricCyan.copy(alpha = 0.55f),
        style = Stroke(
            width = 2.2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
        )
    )

    // 5. GARIS PERGERAKAN KAKI, LUTUT & BUKU LALI (SuccessGreen & FinishLineRed — Leg Drive Vectors)
    // Left Leg (Green)
    drawLine(color = SuccessGreen, start = lHip, end = lKnee, strokeWidth = 5.5f)
    drawLine(color = SuccessGreen, start = lKnee, end = lAnkle, strokeWidth = 5.5f)
    drawLine(color = SuccessGreen, start = lAnkle, end = lToe, strokeWidth = 4.5f)

    // Right Leg (LaserOrange / Red)
    drawLine(color = LaserOrange, start = rHip, end = rKnee, strokeWidth = 5.5f)
    drawLine(color = LaserOrange, start = rKnee, end = rAnkle, strokeWidth = 5.5f)
    drawLine(color = LaserOrange, start = rAnkle, end = rToe, strokeWidth = 4.5f)

    // Stride Opening Line between Left Ankle & Right Ankle
    drawLine(
        color = Color.White.copy(alpha = 0.7f),
        start = lAnkle,
        end = rAnkle,
        strokeWidth = 2.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f), 0f)
    )

    // 6. PROPULSION / JUMP TRAJECTORY VECTOR ARROW from Center of Mass (Hip/Torso)
    val vecEnd = Offset(
        hipCenter.x + skeleton.velocityVectorDx * w,
        hipCenter.y + skeleton.velocityVectorDy * h - (if (isJumpMode) h * 0.08f else 0f)
    )
    drawLine(
        color = FinishLineRed,
        start = hipCenter,
        end = vecEnd,
        strokeWidth = 4.5f
    )
    drawCircle(
        color = FinishLineRed,
        radius = 6.5f,
        center = vecEnd
    )

    // 7. Draw 17 Glowing Anatomical Joint Nodes
    // Head Halo
    drawCircle(
        color = SpeedAmber.copy(alpha = 0.28f),
        radius = 16f,
        center = head
    )
    drawCircle(
        color = SpeedAmber,
        radius = 12f,
        center = head,
        style = Stroke(width = 3f)
    )
    drawCircle(color = Color.White, radius = 4f, center = head)

    val allJoints = listOf(
        neck to SpeedAmber,
        lShoulder to ElectricCyan,
        rShoulder to ElectricCyan,
        lElbow to ElectricCyan,
        rElbow to ElectricCyan,
        lWrist to Color.White,
        rWrist to Color.White,
        hipCenter to SpeedAmber,
        lHip to SuccessGreen,
        rHip to LaserOrange,
        lKnee to SuccessGreen,
        rKnee to LaserOrange,
        lAnkle to SuccessGreen,
        rAnkle to LaserOrange,
        lToe to Color.White,
        rToe to Color.White
    )

    allJoints.forEach { (pos, jointColor) ->
        drawCircle(color = Color.Black.copy(alpha = 0.75f), radius = 6.5f, center = pos)
        drawCircle(color = jointColor, radius = 5f, center = pos)
    }
}

/**
 * High-Tech Telemetry HUD Bar displayed inside the Camera Monitor showing:
 * - Human vs Non-Human (Finger/Object) AI Filter Status
 * - Live Biomechanical Angles (Badan, Tangan, Lutut, Langkah / Lompatan)
 * - Camera Optical Zoom Controls (1x, 1.5x, 2x, 3x, 4x)
 */
@Composable
fun AiCameraMonitorTelemetryHud(
    skeleton: HumanSkeletonTelemetry,
    currentZoom: Float,
    onSelectZoom: (Float) -> Unit,
    isDemoSkeletonActive: Boolean,
    onToggleDemoSkeleton: () -> Unit,
    isJumpMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Row 1: AI Human Verification Status + Live Limb Angles Readout
        Surface(
            color = Color.Black.copy(alpha = 0.84f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(
                1.dp,
                when {
                    skeleton.isHumanDetected -> SuccessGreen
                    skeleton.isNonHumanRejected -> FinishLineRed
                    else -> ElectricCyan.copy(alpha = 0.6f)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(
                                    when {
                                        skeleton.isHumanDetected -> SuccessGreen
                                        skeleton.isNonHumanRejected -> FinishLineRed
                                        else -> SpeedAmber
                                    },
                                    CircleShape
                                )
                        )
                        Text(
                            text = when {
                                skeleton.isHumanDetected ->
                                    "AI HUMAN VERIFIED (${skeleton.humanConfidencePercent}%): GARIS RANGKA MANUSIA AKTIF"
                                skeleton.isNonHumanRejected ->
                                    "🛡️ AI FILTER: ${skeleton.rejectionReason}"
                                else ->
                                    "AI HUMAN DETECTOR: HANYA KESAN MANUSIA (JARI/OBJEK DIABAIKAN)"
                            },
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = when {
                                skeleton.isHumanDetected -> SuccessGreen
                                skeleton.isNonHumanRejected -> FinishLineRed
                                else -> SpeedAmber
                            },
                            maxLines = 1
                        )
                    }

                    // Toggle Demo / Live AI Skeleton View
                    Surface(
                        color = if (isDemoSkeletonActive) SuccessGreen else StadiumSurfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (isDemoSkeletonActive) SuccessGreen else ElectricCyan),
                        modifier = Modifier
                            .height(20.dp)
                            .clickable { onToggleDemoSkeleton() }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = if (isDemoSkeletonActive) "Rangka AI: ON ✓" else "Papar Rangka AI",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDemoSkeletonActive) Color.Black else ElectricCyan
                            )
                        }
                    }
                }

                // Live Kinematik Joint Angles Strip (Badan, Tangan, Lutut, Kaki / Lompatan)
                if (skeleton.isHumanDetected) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Badan: ${"%.1f".format(skeleton.torsoLeanAngleDeg)}°",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = SpeedAmber
                        )
                        Text(
                            text = "Tangan: ${skeleton.armSwingAngleDeg}°",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                        Text(
                            text = "Lutut: ${skeleton.frontKneeAngleDeg}°/${skeleton.rearKneeAngleDeg}°",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        Text(
                            text = if (isJumpMode) {
                                "Lonjakan: ${"%.1f".format(skeleton.jumpTakeoffAngleDeg)}°"
                            } else {
                                "Langkah: ${"%.2f".format(skeleton.strideLengthMeters)}m"
                            },
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Row 2: Live Camera Zoom Selector Pills (1.0x, 1.5x, 2.0x, 3.0x, 4.0x)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.80f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, StadiumBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.ZoomIn,
                        contentDescription = "Zoom Kamera",
                        tint = ElectricCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "ZOOM:",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = SilverMetallic
                    )
                    listOf(1.0f, 1.5f, 2.0f, 3.0f, 4.0f).forEach { z ->
                        val selected = kotlin.math.abs(currentZoom - z) < 0.1f
                        Surface(
                            color = if (selected) ElectricCyan else Color.Transparent,
                            shape = RoundedCornerShape(5.dp),
                            modifier = Modifier.clickable { onSelectZoom(z) }
                        ) {
                            Text(
                                text = "${z}x",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = if (selected) Color.Black else Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
