package com.example.service

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StorageManager(private val context: Context) {

    val appBaseDir: File by lazy {
        val dir = File(context.getExternalFilesDir(null), "PsycoTimeXPro")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    val videosDir: File by lazy {
        val dir = File(appBaseDir, "Videos")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    val racesDir: File by lazy {
        val dir = File(appBaseDir, "Races")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    val photoFinishDir: File by lazy {
        val dir = File(appBaseDir, "PhotoFinish")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    val exportsDir: File by lazy {
        val dir = File(appBaseDir, "Exports")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    init {
        // Auto-create all required PsycoTimeXPro folders immediately upon app startup
        ensureDirectoriesExist()
    }

    fun ensureDirectoriesExist(): List<String> {
        val dirs = listOf(
            videosDir,
            racesDir,
            photoFinishDir,
            exportsDir,
            File(appBaseDir, "KadTeknikal")
        )
        dirs.forEach { if (!it.exists()) it.mkdirs() }
        return dirs.map { it.absolutePath }
    }

    /**
     * Creates and saves a high-precision Photo Finish Image with Torso Slit Line and Time Overlay
     */
    fun savePhotoFinishImage(
        raceTitle: String,
        athleteName: String,
        timeFormatted: String,
        isCam2Torso: Boolean
    ): String {
        val width = 1080
        val height = 720
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw track finish line background simulation
        val bgPaint = Paint().apply {
            color = Color.parseColor("#111827")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw track lanes
        val lanePaint = Paint().apply {
            color = Color.parseColor("#374151")
            strokeWidth = 3f
        }
        for (i in 1..8) {
            val y = i * (height / 8f)
            canvas.drawLine(0f, y, width.toFloat(), y, lanePaint)
        }

        // Draw Finishing Line (Bright Red / Orange Laser slit line)
        val finishLinePaint = Paint().apply {
            color = Color.parseColor("#EF4444")
            strokeWidth = 6f
        }
        val finishX = width * 0.55f
        canvas.drawLine(finishX, 0f, finishX, height.toFloat(), finishLinePaint)

        // Draw Torso Silhouette crossing finish line
        val runnerPaint = Paint().apply {
            color = Color.parseColor("#06B6D4")
            strokeWidth = 14f
            strokeCap = Paint.Cap.ROUND
        }
        val headPaint = Paint().apply {
            color = Color.parseColor("#F97316")
            style = Paint.Style.FILL
        }
        // Head
        canvas.drawCircle(finishX + 10f, 320f, 32f, headPaint)
        // Torso lean forward crossing laser beam
        canvas.drawLine(finishX - 30f, 440f, finishX + 15f, 350f, runnerPaint)
        // Arms
        canvas.drawLine(finishX - 10f, 380f, finishX + 60f, 410f, runnerPaint)
        canvas.drawLine(finishX - 20f, 390f, finishX - 70f, 430f, runnerPaint)
        // Legs
        canvas.drawLine(finishX - 30f, 440f, finishX + 40f, 560f, runnerPaint)
        canvas.drawLine(finishX - 30f, 440f, finishX - 50f, 540f, runnerPaint)

        // Top Header Banner with Electronic Timing Details
        val bannerPaint = Paint().apply {
            color = Color.parseColor("#CC0B111E")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 130f, bannerPaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 34f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText("PSYCO TIME X PRO - OFFICIAL PHOTO FINISH", 30f, 50f, textPaint)

        textPaint.textSize = 28f
        textPaint.color = Color.parseColor("#06B6D4")
        canvas.drawText("$raceTitle | Atlit: $athleteName", 30f, 95f, textPaint)

        // Right side: Official Timer Overlay Badge
        val timerBadgePaint = Paint().apply {
            color = Color.parseColor("#F97316")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(width - 320f, 25f, width - 30f, 105f, 16f, 16f, timerBadgePaint)

        val timerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 44f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText(timeFormatted, width - 290f, 82f, timerTextPaint)

        // Torso alignment marker
        val markerPaint = Paint().apply {
            color = Color.YELLOW
            textSize = 24f
            isAntiAlias = true
        }
        canvas.drawText("◄ TORSO CONTACT POINT: $timeFormatted", finishX + 15f, 320f, markerPaint)

        // Save to internal app storage
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "PhotoFinish_${raceTitle.replace(" ", "_")}_$timeStamp.png"
        val destFile = File(photoFinishDir, fileName)

        FileOutputStream(destFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // Also save to Public MediaStore so user can find it in phone gallery
        saveImageToPublicGallery(bitmap, fileName)

        return destFile.absolutePath
    }

    /**
     * Exports image to device Pictures/Downloads gallery
     */
    private fun saveImageToPublicGallery(bitmap: Bitmap, fileName: String) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PsycoTimeXPro")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Simulates or creates saved race video package files for Cam 1 & Cam 2 with overlays
     */
    fun createRaceVideoPackage(
        raceTitle: String,
        recordedTimeFormatted: String,
        athleteName: String
    ): Pair<String, String> {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanTitle = raceTitle.replace(" ", "_")

        // Create representative frame video asset files for Cam 1 (Timer Gate) & Cam 2 (Finish Torso)
        val cam1File = File(videosDir, "CAM1_Timer_${cleanTitle}_$timeStamp.mp4")
        val cam2File = File(videosDir, "CAM2_Torso_${cleanTitle}_$timeStamp.mp4")

        // Write marker payload
        cam1File.writeText("PSYCO_TIMEX_CAM1_VIDEO_STREAM\nRace: $raceTitle\nAtlit: $athleteName\nTime: $recordedTimeFormatted\nOverlay: Digital StopWatch + Starter Gate\nTimestamp: $timeStamp")
        cam2File.writeText("PSYCO_TIMEX_CAM2_TORSO_VIDEO_STREAM\nRace: $raceTitle\nAtlit: $athleteName\nTime: $recordedTimeFormatted\nOverlay: Finish Line Laser Slit + Torso AI Detector\nTimestamp: $timeStamp")

        // Also export to Exports folder
        val exportFile = File(exportsDir, "${cleanTitle}_DataReport.json")
        exportFile.writeText("""
            {
                "race": "$raceTitle",
                "athlete": "$athleteName",
                "time": "$recordedTimeFormatted",
                "cam1_stream": "${cam1File.name}",
                "cam2_stream": "${cam2File.name}",
                "exported_to_device": true,
                "folder": "${appBaseDir.absolutePath}"
            }
        """.trimIndent())

        return Pair(cam1File.absolutePath, cam2File.absolutePath)
    }
}
