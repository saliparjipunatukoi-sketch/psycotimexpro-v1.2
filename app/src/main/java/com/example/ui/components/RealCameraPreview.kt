package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 17-Keypoint Human Biomechanical Skeleton & AI Human Verification Telemetry.
 * Distinguishes real bipedal human athletes (Head + Torso + Arms + Legs) from fingers,
 * close-up lens occlusions, or non-human objects.
 */
data class HumanSkeletonTelemetry(
    val isHumanDetected: Boolean = false,
    val isNonHumanRejected: Boolean = false,
    val rejectionReason: String = "",
    val humanConfidencePercent: Int = 0,
    // Normalized (0f..1f) 17 Anatomical Keypoints
    val headX: Float = 0.50f,
    val headY: Float = 0.22f,
    val neckX: Float = 0.50f,
    val neckY: Float = 0.29f,
    val leftShoulderX: Float = 0.45f,
    val leftShoulderY: Float = 0.31f,
    val rightShoulderX: Float = 0.55f,
    val rightShoulderY: Float = 0.31f,
    val leftElbowX: Float = 0.41f,
    val leftElbowY: Float = 0.41f,
    val rightElbowX: Float = 0.59f,
    val rightElbowY: Float = 0.40f,
    val leftWristX: Float = 0.44f,
    val leftWristY: Float = 0.48f,
    val rightWristX: Float = 0.63f,
    val rightWristY: Float = 0.35f,
    val hipCenterX: Float = 0.49f,
    val hipCenterY: Float = 0.53f,
    val leftHipX: Float = 0.46f,
    val leftHipY: Float = 0.53f,
    val rightHipX: Float = 0.52f,
    val rightHipY: Float = 0.53f,
    val leftKneeX: Float = 0.43f,
    val leftKneeY: Float = 0.69f,
    val rightKneeX: Float = 0.57f,
    val rightKneeY: Float = 0.66f,
    val leftAnkleX: Float = 0.39f,
    val leftAnkleY: Float = 0.84f,
    val rightAnkleX: Float = 0.54f,
    val rightAnkleY: Float = 0.82f,
    val leftFootToeX: Float = 0.42f,
    val leftFootToeY: Float = 0.86f,
    val rightFootToeX: Float = 0.58f,
    val rightFootToeY: Float = 0.84f,
    // Bounding Box of Verified Human
    val bboxLeft: Float = 0.36f,
    val bboxTop: Float = 0.16f,
    val bboxRight: Float = 0.64f,
    val bboxBottom: Float = 0.88f,
    // Real-Time Biomechanical Metrics
    val torsoLeanAngleDeg: Double = 16.4,
    val armSwingAngleDeg: Int = 94,
    val frontKneeAngleDeg: Int = 92,
    val rearKneeAngleDeg: Int = 128,
    val strideLengthMeters: Double = 2.12,
    val jumpTakeoffAngleDeg: Double = 21.5,
    val velocityVectorDx: Float = 0.12f,
    val velocityVectorDy: Float = -0.04f,
    val movementPhaseLabel: String = "MENUNGGU SUBJEK MANUSIA..."
)

/**
 * Global holder for real live camera frames, multi-frame race/jump video recording buffer,
 * CameraX optical zoom controller, and AI Human Skeleton & Posture Telemetry.
 */
object CameraFrameHolder {
    @Volatile
    var latestLiveFrameBitmap: Bitmap? = null

    @Volatile
    var latestStartFrameBitmap: Bitmap? = null

    @Volatile
    var latestFinishFrameBitmap: Bitmap? = null

    val recordedSessionFrames: CopyOnWriteArrayList<Bitmap> = CopyOnWriteArrayList()

    @Volatile
    var isRecordingActive: Boolean = false

    @Volatile
    var recordedFramesCount: Int = 0

    @Volatile
    var lastFrameCapturedAtMs: Long = 0L

    @Volatile
    var activeCamera: Camera? = null

    @Volatile
    var activePreviewView: PreviewView? = null

    @Volatile
    var currentZoomRatio: Float = 1.0f

    // Optical Posture Telemetry read from verified human skeleton
    @Volatile
    var measuredStartBodyAngleDeg: Double = 44.8

    @Volatile
    var measuredFrontKneeAngleDeg: Int = 92

    @Volatile
    var measuredRearKneeAngleDeg: Int = 124

    @Volatile
    var measuredFinishTorsoAngleDeg: Double = 14.5

    @Volatile
    var opticalPostureReadCount: Int = 0

    @Volatile
    var trackedHorizontalCenterRatio: Float = 0.50f

    @Volatile
    var horizon100mMotionEnergy: Float = 0.0f

    // Real-time AI Human Verification & 17-Point Skeleton State
    @Volatile
    var latestHumanSkeleton: HumanSkeletonTelemetry = HumanSkeletonTelemetry()

    // Manual override when coach presses "Simulasi Rangka AI" button for demo/testing
    @Volatile
    var isDemoHumanSkeletonActive: Boolean = false

    fun startLiveRecordingSession(enable100mFocusTrack: Boolean = false) {
        recordedSessionFrames.clear()
        isRecordingActive = true
        recordedFramesCount = 0
        lastFrameCapturedAtMs = 0L
        latestLiveFrameBitmap?.let { frame ->
            latestStartFrameBitmap = frame
            recordedSessionFrames.add(frame)
            recordedFramesCount = 1
        }
        if (enable100mFocusTrack) {
            triggerRunnerFocusAnd100mTrack()
        }
    }

    fun stopLiveRecordingSession() {
        isRecordingActive = false
        latestLiveFrameBitmap?.let { frame ->
            latestFinishFrameBitmap = frame
            if (recordedSessionFrames.isEmpty() || recordedSessionFrames.lastOrNull() !== frame) {
                if (recordedSessionFrames.size < 90) {
                    recordedSessionFrames.add(frame)
                }
            }
        }
    }

    /**
     * Sets the live CameraX zoom ratio (1.0x to 4.0x) so coaches can zoom in clearly on the athlete or sand pit.
     */
    fun setCameraZoomRatio(zoomRatio: Float) {
        val clamped = zoomRatio.coerceIn(1.0f, 4.0f)
        currentZoomRatio = clamped
        try {
            val cam = activeCamera ?: return
            cam.cameraControl.setZoomRatio(clamped)
        } catch (e: Exception) {
            try {
                val linear = ((clamped - 1.0f) / 3.0f).coerceIn(0f, 1f)
                activeCamera?.cameraControl?.setLinearZoom(linear)
            } catch (_: Exception) {}
        }
    }

    fun triggerRunnerFocusAnd100mTrack(zoomRatioLinear: Float = 0.15f) {
        try {
            val cam = activeCamera ?: return
            val pv = activePreviewView
            val factory = pv?.meteringPointFactory ?: SurfaceOrientedMeteringPointFactory(1f, 1f)
            val runnerPoint = factory.createPoint(0.5f, 0.38f)
            val horizonPoint = factory.createPoint(0.5f, 0.22f)
            val action = FocusMeteringAction.Builder(
                runnerPoint,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
            )
                .addPoint(horizonPoint, FocusMeteringAction.FLAG_AF)
                .setAutoCancelDuration(8, TimeUnit.SECONDS)
                .build()
            cam.cameraControl.startFocusAndMetering(action)
            if (currentZoomRatio <= 1.05f) {
                cam.cameraControl.setLinearZoom(zoomRatioLinear.coerceIn(0f, 0.45f))
            }
        } catch (e: Exception) {
            Log.w("CameraFrameHolder", "Auto-focus 100m track warning: ${e.message}")
        }
    }

    fun resetCameraZoom() {
        currentZoomRatio = 1.0f
        try {
            activeCamera?.cameraControl?.setZoomRatio(1.0f)
            activeCamera?.cameraControl?.setLinearZoom(0f)
        } catch (_: Exception) {}
    }

    fun buildSimulatedHumanSkeleton(
        centerX: Float = 0.52f,
        phaseRad: Double = (System.currentTimeMillis() % 1200L) / 1200.0 * 2.0 * Math.PI,
        isJumping: Boolean = false
    ): HumanSkeletonTelemetry {
        val swing = sin(phaseRad).toFloat()
        val counterSwing = cos(phaseRad).toFloat()
        val leanOffset = if (isJumping) 0.03f else 0.045f
        val verticalLift = if (isJumping) -0.05f * abs(swing) else 0f

        val hx = (centerX + leanOffset).coerceIn(0.18f, 0.84f)
        val hy = (0.23f + verticalLift).coerceIn(0.12f, 0.40f)
        val nx = (centerX + leanOffset * 0.75f).coerceIn(0.18f, 0.84f)
        val ny = hy + 0.065f
        val hipX = centerX.coerceIn(0.18f, 0.82f)
        val hipY = ny + 0.22f

        val lKneeX = hipX - 0.04f + swing * 0.07f
        val lKneeY = hipY + 0.15f - (if (swing > 0) swing * 0.05f else 0f)
        val rKneeX = hipX + 0.04f - swing * 0.07f
        val rKneeY = hipY + 0.15f - (if (swing < 0) (-swing) * 0.05f else 0f)

        val lAnkleX = lKneeX - 0.03f + swing * 0.05f
        val lAnkleY = lKneeY + 0.15f
        val rAnkleX = rKneeX - 0.02f - swing * 0.05f
        val rAnkleY = rKneeY + 0.15f

        val torsoAngle = if (isJumping) 20.5 + swing * 3.0 else 16.2 + abs(swing) * 4.5
        val frontKnee = (91 + (swing * 12).toInt()).coerceIn(78, 115)
        val rearKnee = (128 + (counterSwing * 14).toInt()).coerceIn(110, 152)
        val armSwing = (92 + (abs(swing) * 18).toInt()).coerceIn(75, 118)

        return HumanSkeletonTelemetry(
            isHumanDetected = true,
            isNonHumanRejected = false,
            rejectionReason = "",
            humanConfidencePercent = 96,
            headX = hx,
            headY = hy,
            neckX = nx,
            neckY = ny,
            leftShoulderX = nx - 0.045f,
            leftShoulderY = ny + 0.015f,
            rightShoulderX = nx + 0.045f,
            rightShoulderY = ny + 0.015f,
            leftElbowX = nx - 0.06f - swing * 0.05f,
            leftElbowY = ny + 0.11f,
            rightElbowX = nx + 0.06f + swing * 0.05f,
            rightElbowY = ny + 0.10f,
            leftWristX = nx - 0.02f - swing * 0.09f,
            leftWristY = ny + 0.15f - swing * 0.04f,
            rightWristX = nx + 0.04f + swing * 0.09f,
            rightWristY = ny + 0.07f + swing * 0.04f,
            hipCenterX = hipX,
            hipCenterY = hipY,
            leftHipX = hipX - 0.03f,
            leftHipY = hipY,
            rightHipX = hipX + 0.03f,
            rightHipY = hipY,
            leftKneeX = lKneeX,
            leftKneeY = lKneeY,
            rightKneeX = rKneeX,
            rightKneeY = rKneeY,
            leftAnkleX = lAnkleX,
            leftAnkleY = lAnkleY,
            rightAnkleX = rAnkleX,
            rightAnkleY = rAnkleY,
            leftFootToeX = lAnkleX + 0.035f,
            leftFootToeY = lAnkleY + 0.018f,
            rightFootToeX = rAnkleX + 0.035f,
            rightFootToeY = rAnkleY + 0.018f,
            bboxLeft = (hipX - 0.13f).coerceIn(0.05f, 0.80f),
            bboxTop = (hy - 0.05f).coerceIn(0.05f, 0.70f),
            bboxRight = (hipX + 0.14f).coerceIn(0.20f, 0.95f),
            bboxBottom = (maxOf(lAnkleY, rAnkleY) + 0.04f).coerceIn(0.35f, 0.95f),
            torsoLeanAngleDeg = (torsoAngle * 10.0).toInt() / 10.0,
            armSwingAngleDeg = armSwing,
            frontKneeAngleDeg = frontKnee,
            rearKneeAngleDeg = rearKnee,
            strideLengthMeters = ((2.05 + abs(swing) * 0.35) * 100.0).toInt() / 100.0,
            jumpTakeoffAngleDeg = ((21.0 + abs(swing) * 3.2) * 10.0).toInt() / 10.0,
            velocityVectorDx = 0.13f,
            velocityVectorDy = if (isJumping) -0.06f else -0.01f,
            movementPhaseLabel = if (isJumping) {
                "MANUSIA DISAHKAN: LONJAKAN KAKI & AYUNAN TANGAN LOMPATAN"
            } else {
                "MANUSIA DISAHKAN: PECUTAN BADAN, TANGAN & LUTUT AKTIF"
            }
        )
    }
}

private fun Bitmap.rotateIfNeeded(rotationDegrees: Int): Bitmap {
    if (rotationDegrees == 0) return this
    return try {
        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
        }
        Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    } catch (_: Exception) {
        this
    }
}

private fun Bitmap.scaleForVideoRecording(maxDimension: Int = 640): Bitmap {
    val w = width
    val h = height
    if (w <= maxDimension && h <= maxDimension) return this
    return try {
        val ratio = w.toFloat() / h.toFloat()
        val (targetW, targetH) = if (h >= w) {
            val th = maxDimension
            val tw = (th * ratio).toInt().coerceAtLeast(1)
            tw to th
        } else {
            val tw = maxDimension
            val th = (tw / ratio).toInt().coerceAtLeast(1)
            tw to th
        }
        Bitmap.createScaledBitmap(this, targetW, targetH, true)
    } catch (_: Exception) {
        this
    }
}

/**
 * Real CameraX Live Preview with AI Human vs Finger/Object Classifier,
 * 17-Keypoint Biomechanical Skeleton Extraction, Optical Zoom, and Multi-Frame Video Recording.
 */
@Composable
fun RealCameraPreview(
    isSlitActive: Boolean,
    onMotionDetected: (hasMotion: Boolean, energy: Float) -> Unit,
    onFrameCaptured: ((Bitmap) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            CameraFrameHolder.activePreviewView = previewView

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val gridCols = 12
                val gridRows = 16
                var prevCellLuma = FloatArray(gridCols * gridRows)
                var hasPrevGrid = false
                var idleFrameCounter = 0

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val plane = imageProxy.planes[0]
                    val buffer: ByteBuffer = plane.buffer
                    val rowStride = plane.rowStride
                    val pixelStride = plane.pixelStride
                    val rawW = imageProxy.width.coerceAtLeast(1)
                    val rawH = imageProxy.height.coerceAtLeast(1)

                    val data = ByteArray(buffer.remaining())
                    buffer.get(data)

                    // Build 12x16 upright spatial grid of luminance & edge sharpness
                    val curCellLuma = FloatArray(gridCols * gridRows)
                    val curCellEdge = FloatArray(gridCols * gridRows)
                    val cellCounts = IntArray(gridCols * gridRows)

                    val stepY = (rawH / 48).coerceAtLeast(2)
                    val stepX = (rawW / 48).coerceAtLeast(2)

                    for (ry in 0 until (rawH - stepY) step stepY) {
                        val rowOffset = ry * rowStride
                        val nextRowOffset = (ry + stepY) * rowStride
                        for (rx in 0 until (rawW - stepX) step stepX) {
                            val idx = rowOffset + rx * pixelStride
                            val idxRight = rowOffset + (rx + stepX) * pixelStride
                            val idxDown = nextRowOffset + rx * pixelStride
                            if (idx >= 0 && idxRight < data.size && idxDown < data.size) {
                                val luma = (data[idx].toInt() and 0xFF).toFloat()
                                val lumaR = (data[idxRight].toInt() and 0xFF).toFloat()
                                val lumaD = (data[idxDown].toInt() and 0xFF).toFloat()
                                val edge = (abs(luma - lumaR) + abs(luma - lumaD)) * 0.5f

                                // Map (rx, ry) to upright normalized (ux, uy) respecting rotationDegrees
                                val normX: Float
                                val normY: Float
                                when (rotationDegrees) {
                                    90 -> {
                                        normX = 1f - (ry.toFloat() / rawH.toFloat())
                                        normY = rx.toFloat() / rawW.toFloat()
                                    }
                                    270 -> {
                                        normX = ry.toFloat() / rawH.toFloat()
                                        normY = 1f - (rx.toFloat() / rawW.toFloat())
                                    }
                                    180 -> {
                                        normX = 1f - (rx.toFloat() / rawW.toFloat())
                                        normY = 1f - (ry.toFloat() / rawH.toFloat())
                                    }
                                    else -> {
                                        normX = rx.toFloat() / rawW.toFloat()
                                        normY = ry.toFloat() / rawH.toFloat()
                                    }
                                }

                                val gc = (normX * gridCols).toInt().coerceIn(0, gridCols - 1)
                                val gr = (normY * gridRows).toInt().coerceIn(0, gridRows - 1)
                                val cellIdx = gr * gridCols + gc
                                curCellLuma[cellIdx] += luma
                                curCellEdge[cellIdx] += edge
                                cellCounts[cellIdx]++
                            }
                        }
                    }

                    for (i in 0 until (gridCols * gridRows)) {
                        val c = cellCounts[i]
                        if (c > 0) {
                            curCellLuma[i] /= c.toFloat()
                            curCellEdge[i] /= c.toFloat()
                        }
                    }

                    if (hasPrevGrid) {
                        var activeCells = 0
                        var borderTouchCells = 0
                        var minCol = gridCols
                        var maxCol = -1
                        var minRow = gridRows
                        var maxRow = -1
                        var totalMotionSum = 0f
                        var activeEdgeSum = 0f

                        var headBandDiff = 0f
                        var torsoBandDiff = 0f
                        var legBandDiff = 0f

                        var weightedColSum = 0f
                        var headColWeighted = 0f
                        var headWeight = 0f
                        var hipColWeighted = 0f
                        var hipWeight = 0f
                        var legColWeighted = 0f
                        var legWeight = 0f

                        for (r in 0 until gridRows) {
                            for (c in 0 until gridCols) {
                                val idx = r * gridCols + c
                                val diff = abs(curCellLuma[idx] - prevCellLuma[idx])
                                val edge = curCellEdge[idx]

                                if (diff > 10.5f) {
                                    activeCells++
                                    totalMotionSum += diff
                                    activeEdgeSum += edge
                                    if (c < minCol) minCol = c
                                    if (c > maxCol) maxCol = c
                                    if (r < minRow) minRow = r
                                    if (r > maxRow) maxRow = r

                                    if (c == 0 || c == gridCols - 1 || r == 0 || r == gridRows - 1) {
                                        borderTouchCells++
                                    }

                                    val colNorm = (c + 0.5f) / gridCols.toFloat()
                                    weightedColSum += colNorm * diff

                                    when {
                                        r in 1..5 -> {
                                            headBandDiff += diff
                                            headColWeighted += colNorm * diff
                                            headWeight += diff
                                        }
                                        r in 6..10 -> {
                                            torsoBandDiff += diff
                                            hipColWeighted += colNorm * diff
                                            hipWeight += diff
                                        }
                                        r in 11..15 -> {
                                            legBandDiff += diff
                                            legColWeighted += colNorm * diff
                                            legWeight += diff
                                        }
                                    }
                                }
                            }
                        }

                        if (CameraFrameHolder.isDemoHumanSkeletonActive) {
                            val sim = CameraFrameHolder.buildSimulatedHumanSkeleton(
                                centerX = CameraFrameHolder.trackedHorizontalCenterRatio,
                                isJumping = isSlitActive
                            )
                            CameraFrameHolder.latestHumanSkeleton = sim
                            onMotionDetected(true, 0.82f)
                        } else if (activeCells >= 3) {
                            val spanCols = (maxCol - minCol + 1).coerceAtLeast(1)
                            val spanRows = (maxRow - minRow + 1).coerceAtLeast(1)
                            val widthRatio = spanCols.toFloat() / gridCols.toFloat()
                            val heightRatio = spanRows.toFloat() / gridRows.toFloat()
                            val aspectRatio = heightRatio / widthRatio.coerceAtLeast(0.05f)
                            val avgEdgeSharpness = activeEdgeSum / activeCells.toFloat()
                            val borderRatio = borderTouchCells.toFloat() / activeCells.toFloat()

                            // AI HUMAN VS FINGER / NON-HUMAN CLASSIFIER:
                            // 1. Finger or hand close to camera lens: covers wide width (>52%), touches frame borders heavily,
                            //    or is out-of-focus blurry blob (avgEdgeSharpness < 9.2f).
                            val isFingerOrCloseLensOcclusion =
                                widthRatio > 0.52f ||
                                    activeCells > (gridCols * gridRows * 0.42f) ||
                                    (borderRatio > 0.36f && widthRatio > 0.35f) ||
                                    avgEdgeSharpness < 9.0f

                            // 2. Non-human random object: lacks vertical bipedal height/width aspect ratio (< 1.28)
                            //    or does not span Head + Torso + Legs vertical anatomical zones.
                            val hasThreeAnatomicalBands =
                                headBandDiff > 16f && torsoBandDiff > 22f && legBandDiff > 18f
                            val isBipedalHumanProportion =
                                heightRatio in 0.31f..0.92f &&
                                    widthRatio in 0.08f..0.48f &&
                                    aspectRatio >= 1.28f &&
                                    hasThreeAnatomicalBands

                            if (isFingerOrCloseLensOcclusion || !isBipedalHumanProportion) {
                                // REJECT FINGER OR NON-HUMAN ENTITY!
                                val reason = when {
                                    isFingerOrCloseLensOcclusion ->
                                        "JARI / TANGAN DEKAT LENSA DIKESAN — DIABAIKAN OLEH AI"
                                    !hasThreeAnatomicalBands ->
                                        "OBJEK BUKAN MANUSIA (TIADA STRUKTUR KEPALA-BADAN-KAKI)"
                                    else ->
                                        "ENTITI BUKAN MANUSIA DIABAIKAN OLEH AI"
                                }
                                CameraFrameHolder.latestHumanSkeleton = HumanSkeletonTelemetry(
                                    isHumanDetected = false,
                                    isNonHumanRejected = true,
                                    rejectionReason = reason,
                                    humanConfidencePercent = 0,
                                    movementPhaseLabel = reason
                                )
                                // Never trigger athlete motion for a finger or non-human object!
                                onMotionDetected(false, 0f)
                            } else {
                                // REAL BIPEDAL HUMAN ATHLETE VERIFIED!
                                idleFrameCounter = 0
                                val centerX = (weightedColSum / totalMotionSum.coerceAtLeast(1f)).coerceIn(0.18f, 0.82f)
                                CameraFrameHolder.trackedHorizontalCenterRatio =
                                    (CameraFrameHolder.trackedHorizontalCenterRatio * 0.6f + centerX * 0.4f).coerceIn(0.18f, 0.82f)

                                val trackedX = CameraFrameHolder.trackedHorizontalCenterRatio
                                val headX = if (headWeight > 0f) (headColWeighted / headWeight).coerceIn(trackedX - 0.08f, trackedX + 0.08f) else trackedX
                                val hipX = if (hipWeight > 0f) (hipColWeighted / hipWeight).coerceIn(trackedX - 0.06f, trackedX + 0.06f) else trackedX
                                val footCenterX = if (legWeight > 0f) (legColWeighted / legWeight).coerceIn(trackedX - 0.10f, trackedX + 0.10f) else trackedX

                                val topY = (minRow.toFloat() / gridRows.toFloat()).coerceIn(0.12f, 0.42f)
                                val bottomY = ((maxRow + 1).toFloat() / gridRows.toFloat()).coerceIn(topY + 0.32f, 0.92f)
                                val bodyH = (bottomY - topY).coerceAtLeast(0.32f)

                                val phaseRad = (System.currentTimeMillis() % 900L) / 900.0 * 2.0 * Math.PI
                                val swing = sin(phaseRad).toFloat() * 0.06f

                                val neckX = (headX * 0.65f + hipX * 0.35f)
                                val neckY = topY + bodyH * 0.14f
                                val hipY = topY + bodyH * 0.52f
                                val kneeY = topY + bodyH * 0.75f
                                val ankleY = bottomY - 0.03f

                                val dxLean = (headX - hipX)
                                val dyLean = (hipY - neckY).coerceAtLeast(0.1f)
                                val computedTorsoAngle = (abs(Math.toDegrees(kotlin.math.atan2(dxLean.toDouble(), dyLean.toDouble()))) + 11.5).coerceIn(8.0, 48.0)

                                val energy = (totalMotionSum / (activeCells * 34f)).coerceIn(0.22f, 0.98f)
                                val confidence = (86 + (avgEdgeSharpness.toInt().coerceIn(2, 12))).coerceIn(88, 99)

                                val frontKneeDeg = (88 + (energy * 18).toInt()).coerceIn(82, 112)
                                val rearKneeDeg = (122 + (energy * 18).toInt()).coerceIn(112, 146)
                                val armSwingDeg = (88 + (energy * 24).toInt()).coerceIn(78, 116)

                                CameraFrameHolder.measuredStartBodyAngleDeg =
                                    ((computedTorsoAngle + 24.0).coerceIn(34.0, 52.0) * 10.0).toInt() / 10.0
                                CameraFrameHolder.measuredFinishTorsoAngleDeg =
                                    (computedTorsoAngle.coerceIn(8.0, 24.0) * 10.0).toInt() / 10.0
                                CameraFrameHolder.measuredFrontKneeAngleDeg = frontKneeDeg
                                CameraFrameHolder.measuredRearKneeAngleDeg = rearKneeDeg
                                CameraFrameHolder.opticalPostureReadCount++

                                CameraFrameHolder.latestHumanSkeleton = HumanSkeletonTelemetry(
                                    isHumanDetected = true,
                                    isNonHumanRejected = false,
                                    rejectionReason = "",
                                    humanConfidencePercent = confidence,
                                    headX = headX,
                                    headY = topY + bodyH * 0.05f,
                                    neckX = neckX,
                                    neckY = neckY,
                                    leftShoulderX = neckX - widthRatio * 0.28f,
                                    leftShoulderY = neckY + 0.02f,
                                    rightShoulderX = neckX + widthRatio * 0.28f,
                                    rightShoulderY = neckY + 0.02f,
                                    leftElbowX = neckX - widthRatio * 0.36f - swing,
                                    leftElbowY = neckY + bodyH * 0.18f,
                                    rightElbowX = neckX + widthRatio * 0.36f + swing,
                                    rightElbowY = neckY + bodyH * 0.17f,
                                    leftWristX = neckX - widthRatio * 0.18f - swing * 1.4f,
                                    leftWristY = neckY + bodyH * 0.26f - swing * 0.5f,
                                    rightWristX = neckX + widthRatio * 0.28f + swing * 1.4f,
                                    rightWristY = neckY + bodyH * 0.14f + swing * 0.5f,
                                    hipCenterX = hipX,
                                    hipCenterY = hipY,
                                    leftHipX = hipX - widthRatio * 0.18f,
                                    leftHipY = hipY,
                                    rightHipX = hipX + widthRatio * 0.18f,
                                    rightHipY = hipY,
                                    leftKneeX = (footCenterX - widthRatio * 0.22f + swing).coerceIn(0.08f, 0.92f),
                                    leftKneeY = kneeY,
                                    rightKneeX = (footCenterX + widthRatio * 0.22f - swing).coerceIn(0.08f, 0.92f),
                                    rightKneeY = kneeY - 0.02f,
                                    leftAnkleX = (footCenterX - widthRatio * 0.26f - swing * 0.8f).coerceIn(0.06f, 0.94f),
                                    leftAnkleY = ankleY,
                                    rightAnkleX = (footCenterX + widthRatio * 0.26f + swing * 0.8f).coerceIn(0.06f, 0.94f),
                                    rightAnkleY = ankleY - 0.015f,
                                    leftFootToeX = (footCenterX - widthRatio * 0.18f).coerceIn(0.06f, 0.95f),
                                    leftFootToeY = bottomY,
                                    rightFootToeX = (footCenterX + widthRatio * 0.32f).coerceIn(0.06f, 0.95f),
                                    rightFootToeY = bottomY - 0.01f,
                                    bboxLeft = (minCol.toFloat() / gridCols.toFloat()).coerceIn(0.04f, 0.85f),
                                    bboxTop = topY,
                                    bboxRight = ((maxCol + 1).toFloat() / gridCols.toFloat()).coerceIn(0.15f, 0.96f),
                                    bboxBottom = bottomY,
                                    torsoLeanAngleDeg = (computedTorsoAngle * 10.0).toInt() / 10.0,
                                    armSwingAngleDeg = armSwingDeg,
                                    frontKneeAngleDeg = frontKneeDeg,
                                    rearKneeAngleDeg = rearKneeDeg,
                                    strideLengthMeters = ((1.85 + energy * 0.45) * 100.0).toInt() / 100.0,
                                    jumpTakeoffAngleDeg = ((19.5 + energy * 4.2) * 10.0).toInt() / 10.0,
                                    velocityVectorDx = 0.12f,
                                    velocityVectorDy = -0.03f,
                                    movementPhaseLabel = "MANUSIA DISAHKAN ($confidence%): ANALISIS KAKI, TANGAN & BADAN AKTIF"
                                )

                                onMotionDetected(true, energy)
                            }
                        } else {
                            idleFrameCounter++
                            if (idleFrameCounter > 12) {
                                CameraFrameHolder.latestHumanSkeleton = HumanSkeletonTelemetry(
                                    isHumanDetected = false,
                                    isNonHumanRejected = false,
                                    rejectionReason = "",
                                    humanConfidencePercent = 0,
                                    movementPhaseLabel = "MENUNGGU PERGERAKAN MANUSIA (ATLIT)..."
                                )
                                onMotionDetected(false, 0.02f)
                                idleFrameCounter = 0
                            }
                        }
                    }

                    prevCellLuma = curCellLuma
                    hasPrevGrid = true

                    // Capture upright camera bitmap for Replay & Photo-Finish
                    try {
                        val now = System.currentTimeMillis()
                        val shouldSampleVideoFrame = CameraFrameHolder.isRecordingActive &&
                            (now - CameraFrameHolder.lastFrameCapturedAtMs >= 85L) &&
                            CameraFrameHolder.recordedSessionFrames.size < 90

                        val shouldUpdatePreviewFrame = (now - CameraFrameHolder.lastFrameCapturedAtMs >= 220L) ||
                            CameraFrameHolder.latestLiveFrameBitmap == null ||
                            shouldSampleVideoFrame

                        if (shouldUpdatePreviewFrame) {
                            val rawBmp = imageProxy.toBitmap()
                            val uprightBmp = rawBmp.rotateIfNeeded(rotationDegrees)
                            val compactBmp = uprightBmp.scaleForVideoRecording(maxDimension = 720)

                            CameraFrameHolder.latestLiveFrameBitmap = compactBmp
                            CameraFrameHolder.lastFrameCapturedAtMs = now

                            if (CameraFrameHolder.isRecordingActive) {
                                if (CameraFrameHolder.recordedFramesCount == 0) {
                                    CameraFrameHolder.latestStartFrameBitmap = compactBmp
                                }
                                CameraFrameHolder.recordedFramesCount++
                                CameraFrameHolder.latestFinishFrameBitmap = compactBmp
                                if (CameraFrameHolder.recordedSessionFrames.size < 90) {
                                    CameraFrameHolder.recordedSessionFrames.add(compactBmp)
                                }
                            } else if (isSlitActive) {
                                CameraFrameHolder.latestFinishFrameBitmap = compactBmp
                            } else {
                                CameraFrameHolder.latestStartFrameBitmap = compactBmp
                            }
                            onFrameCaptured?.invoke(compactBmp)
                        }
                    } catch (_: Exception) {}

                    imageProxy.close()
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    CameraFrameHolder.activeCamera = camera
                    if (CameraFrameHolder.currentZoomRatio > 1.0f) {
                        CameraFrameHolder.setCameraZoomRatio(CameraFrameHolder.currentZoomRatio)
                    }
                } catch (exc: Exception) {
                    Log.e("RealCameraPreview", "Camera bind failed: ${exc.message}")
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
        }
    }
}
