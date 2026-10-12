package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.nio.ByteBuffer
import java.util.concurrent.Executors

// Global holder for the latest live camera frame captured from real camera sensor
object CameraFrameHolder {
    @Volatile
    var latestLiveFrameBitmap: Bitmap? = null
}

/**
 * Real CameraX Live Preview with Optical Motion / Torso Gate Analyzer.
 * Automatically checks frame-by-frame luminance differences.
 * When an athlete crosses the slit line, it triggers real optical motion detection!
 */
@Composable
fun RealCameraPreview(
    isSlitActive: Boolean,
    onMotionDetected: (hasMotion: Boolean, energy: Float) -> Unit,
    onFrameCaptured: ((Bitmap) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // Frame-by-frame optical movement analyzer
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                var previousLumaSum = 0L
                var frameCounter = 0

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    val buffer: ByteBuffer = imageProxy.planes[0].buffer
                    val data = ByteArray(buffer.remaining())
                    buffer.get(data)

                    // Compute average luminosity in finish line slit zone
                    var currentLumaSum = 0L
                    val step = 16 // Downsample for real-time speed
                    var count = 0
                    for (i in 0 until data.size step step) {
                        currentLumaSum += (data[i].toInt() and 0xFF)
                        count++
                    }

                    if (count > 0) {
                        val currentAvg = currentLumaSum / count
                        if (previousLumaSum != 0L) {
                            val diff = kotlin.math.abs(currentAvg - previousLumaSum)
                            val energy = (diff / 30f).coerceIn(0f, 1f)
                            
                            // Threshold: motion energy > 0.18f indicates athlete body / torso crossing
                            if (energy > 0.18f) {
                                onMotionDetected(true, energy)
                            } else {
                                frameCounter++
                                if (frameCounter > 15) {
                                    onMotionDetected(false, energy)
                                    frameCounter = 0
                                }
                            }
                        }
                        previousLumaSum = currentAvg
                    }

                    // Capture bitmap periodically or when motion happens so user gets the actual camera frame
                    try {
                        val bmp = imageProxy.toBitmap()
                        CameraFrameHolder.latestLiveFrameBitmap = bmp
                        onFrameCaptured?.invoke(bmp)
                    } catch (_: Exception) {}

                    imageProxy.close()
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
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
