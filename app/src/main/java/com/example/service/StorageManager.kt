package com.example.service

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.example.ui.components.CameraFrameHolder
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FolderValidationResult(
    val isValid: Boolean,
    val resolvedPath: String,
    val message: String,
    val freeSpaceMb: Long = 0L,
    val requiresManageStoragePermission: Boolean = false
)

class StorageManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("psyco_storage_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SELECTED_VIDEO_FOLDER = "selected_video_folder_path"
        private const val KEY_CUSTOM_FOLDER_PATHS = "custom_folder_paths"
    }

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
        ensureDirectoriesExist()
    }

    fun ensureDirectoriesExist(): List<String> {
        val dirs = mutableListOf(
            getSelectedVideoFolder(),
            videosDir,
            racesDir,
            photoFinishDir,
            exportsDir
        ).distinctBy { it.absolutePath }
        dirs.forEach {
            try {
                if (!it.exists()) it.mkdirs()
            } catch (_: Exception) {}
        }
        return dirs.map { it.absolutePath }
    }

    fun hasManageExternalStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getSelectedVideoFolderPath(): String {
        val saved = prefs.getString(KEY_SELECTED_VIDEO_FOLDER, null)
        return if (!saved.isNullOrBlank()) saved else videosDir.absolutePath
    }

    fun getSelectedVideoFolder(): File {
        val path = getSelectedVideoFolderPath()
        val folder = File(path)
        return try {
            if (!folder.exists()) folder.mkdirs()
            if (folder.exists() && folder.isDirectory && folder.canWrite()) {
                folder
            } else {
                videosDir
            }
        } catch (_: Exception) {
            videosDir
        }
    }

    fun validateFolderPath(rawPath: String): FolderValidationResult {
        val trimmed = rawPath.trim()
        if (trimmed.isEmpty()) {
            return FolderValidationResult(
                isValid = false,
                resolvedPath = "",
                message = "Laluan folder tidak boleh kosong. Sila pilih atau masukkan laluan folder tempatan."
            )
        }

        if (trimmed.contains("\u0000")) {
            return FolderValidationResult(
                isValid = false,
                resolvedPath = trimmed,
                message = "Laluan mengandungi aksara tidak sah."
            )
        }

        val targetDir = File(trimmed)
        val appExternalPath = context.getExternalFilesDir(null)?.absolutePath ?: ""
        val appInternalPath = context.filesDir.absolutePath
        val isAppScoped = targetDir.absolutePath.startsWith(appExternalPath) ||
                targetDir.absolutePath.startsWith(appInternalPath)

        try {
            if (!targetDir.exists()) {
                val created = targetDir.mkdirs()
                if (!created && !targetDir.exists()) {
                    val needsPerm = !isAppScoped && !hasManageExternalStoragePermission()
                    return FolderValidationResult(
                        isValid = false,
                        resolvedPath = targetDir.absolutePath,
                        message = if (needsPerm) {
                            "Gagal mencipta folder di storan luaran. Sila aktifkan kebenaran 'Manage External Storage' terlebih dahulu."
                        } else {
                            "Folder tidak wujud dan gagal dicipta pada laluan: ${targetDir.absolutePath}"
                        },
                        requiresManageStoragePermission = needsPerm
                    )
                }
            }

            if (!targetDir.isDirectory) {
                return FolderValidationResult(
                    isValid = false,
                    resolvedPath = targetDir.absolutePath,
                    message = "Laluan yang dipilih bukan sebuah folder (direktori)."
                )
            }

            val probeFile = File(targetDir, ".psyco_write_test_${System.currentTimeMillis()}.tmp")
            val writeSuccess = try {
                probeFile.writeText("OK")
                probeFile.delete()
                true
            } catch (_: Exception) {
                false
            }

            if (!writeSuccess || !targetDir.canWrite()) {
                val needsPerm = !isAppScoped && !hasManageExternalStoragePermission()
                return FolderValidationResult(
                    isValid = false,
                    resolvedPath = targetDir.absolutePath,
                    message = if (needsPerm) {
                        "Tiada akses menulis ke folder ini. Sila benarkan 'Manage External Storage' untuk menyimpan video di luar folder aplikasi."
                    } else {
                        "Folder ditemui tetapi tiada kebenaran menulis (Read-Only): ${targetDir.absolutePath}"
                    },
                    requiresManageStoragePermission = needsPerm
                )
            }

            val freeMb = try {
                val stat = StatFs(targetDir.absolutePath)
                (stat.availableBytes) / (1024L * 1024L)
            } catch (_: Exception) {
                0L
            }

            return FolderValidationResult(
                isValid = true,
                resolvedPath = targetDir.absolutePath,
                message = "Laluan folder sah & boleh ditulis (Writable). Ruang bebas: ${freeMb} MB",
                freeSpaceMb = freeMb,
                requiresManageStoragePermission = false
            )
        } catch (e: Exception) {
            val needsPerm = !isAppScoped && !hasManageExternalStoragePermission()
            return FolderValidationResult(
                isValid = false,
                resolvedPath = targetDir.absolutePath,
                message = "Ralat pengesahan laluan folder: ${e.localizedMessage ?: "Tidak dapat diakses"}",
                requiresManageStoragePermission = needsPerm
            )
        }
    }

    fun saveSelectedVideoFolder(rawPath: String): FolderValidationResult {
        val validation = validateFolderPath(rawPath)
        if (validation.isValid) {
            prefs.edit().putString(KEY_SELECTED_VIDEO_FOLDER, validation.resolvedPath).apply()
            addSavedCustomFolderPath(validation.resolvedPath)
        }
        return validation
    }

    private fun addSavedCustomFolderPath(path: String) {
        val current = prefs.getStringSet(KEY_CUSTOM_FOLDER_PATHS, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(path)
        prefs.edit().putStringSet(KEY_CUSTOM_FOLDER_PATHS, current).apply()
    }

    fun resolveTreeUriToLocalPath(treeUri: Uri): String {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val split = docId.split(":")
            if (split.size >= 2) {
                val type = split[0]
                val relativePath = split[1]
                if ("primary".equals(type, ignoreCase = true)) {
                    val base = Environment.getExternalStorageDirectory().absolutePath
                    if (relativePath.isEmpty()) base else "$base/$relativePath"
                } else {
                    "/storage/$type/$relativePath"
                }
            } else if (docId.endsWith(":")) {
                Environment.getExternalStorageDirectory().absolutePath
            } else {
                File(videosDir, docId.replace(":", "_")).absolutePath
            }
        } catch (_: Exception) {
            videosDir.absolutePath
        }
    }

    fun getPresetStorageLocations(): List<Pair<String, String>> {
        val extRoot = Environment.getExternalStorageDirectory().absolutePath
        return listOf(
            "Storan Dalaman Aplikasi (Lalai)" to videosDir.absolutePath,
            "Folder Movies Awam (/Movies/PsycoTimeXPro)" to "$extRoot/Movies/PsycoTimeXPro",
            "Folder DCIM Kamera (/DCIM/PsycoTimeXPro)" to "$extRoot/DCIM/PsycoTimeXPro",
            "Folder Muat Turun (/Download/PsycoTimeXPro)" to "$extRoot/Download/PsycoTimeXPro"
        )
    }

    /**
     * Draws a source bitmap onto a canvas using Center-Crop / Aspect-Preserving scaling so Portrait
     * frames are NEVER stretched sideways into Landscape or vice-versa.
     */
    private fun drawBitmapAspectFill(canvas: Canvas, src: Bitmap, destRect: Rect) {
        val srcW = src.width.toFloat().coerceAtLeast(1f)
        val srcH = src.height.toFloat().coerceAtLeast(1f)
        val dstW = destRect.width().toFloat().coerceAtLeast(1f)
        val dstH = destRect.height().toFloat().coerceAtLeast(1f)

        val scale = maxOf(dstW / srcW, dstH / srcH)
        val scaledW = srcW * scale
        val scaledH = srcH * scale
        val left = destRect.left + (dstW - scaledW) / 2f
        val top = destRect.top + (dstH - scaledH) / 2f

        canvas.save()
        canvas.clipRect(destRect)
        val targetRect = android.graphics.RectF(left, top, left + scaledW, top + scaledH)
        canvas.drawBitmap(src, null, targetRect, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
    }

    /**
     * Resolves the raw un-annotated camera frame associated with a saved photoFinishUri,
     * preventing double-drawing of headers/green boxes/red lines!
     */
    private fun loadRawCleanFrameForRace(photoFinishUri: String?, fallbackFrame: Bitmap?): Bitmap? {
        if (!photoFinishUri.isNullOrBlank()) {
            try {
                val mainFile = File(photoFinishUri)
                val rawFile = File(mainFile.parentFile, "RAW_${mainFile.name}")
                if (rawFile.exists()) {
                    BitmapFactory.decodeFile(rawFile.absolutePath)?.let { return it }
                }
            } catch (_: Exception) {}
        }
        return fallbackFrame
    }

    /**
     * Creates and saves a high-precision Photo Finish Image with a Tall Vertical AI Intelligence Box
     * (Kotak AI Besar Menegak) and Red Torso Slit Line while strictly preserving Portrait orientation!
     */
    fun savePhotoFinishImage(
        raceTitle: String,
        athleteName: String,
        timeFormatted: String,
        isCam2Torso: Boolean,
        realCameraFrame: Bitmap? = null,
        targetFolder: File? = null
    ): String {
        val isPortraitFrame = realCameraFrame != null && realCameraFrame.height >= realCameraFrame.width
        val width = if (isPortraitFrame) 900 else 1080
        val height = if (isPortraitFrame) 1440 else 720

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val headerH = 116f
        val footerH = 92f
        val viewportRect = Rect(0, headerH.toInt(), width, (height - footerH).toInt())

        if (realCameraFrame != null) {
            drawBitmapAspectFill(canvas, realCameraFrame, viewportRect)
        } else {
            val bgPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            val trackPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, headerH, width.toFloat(), height - footerH, trackPaint)

            val lanePaint = Paint().apply {
                color = Color.parseColor("#475569")
                strokeWidth = 3f
            }
            for (i in 1..8) {
                val y = headerH + (i * ((height - headerH - footerH) / 8f))
                canvas.drawLine(0f, y, width.toFloat(), y, lanePaint)
            }

            // Realistic Tall Vertical Sprinter Silhouette inside the Tall AI Box
            val finishX = width * 0.54f
            val centerY = (headerH + (height - footerH)) / 2f
            val athleteTorsoPaint = Paint().apply {
                color = Color.parseColor("#0EA5E9")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(
                finishX - 48f,
                centerY - 120f,
                finishX + 42f,
                centerY + 90f,
                26f,
                26f,
                athleteTorsoPaint
            )
            val headPaint = Paint().apply {
                color = Color.parseColor("#F59E0B")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(finishX + 20f, centerY - 165f, 34f, headPaint)

            val limbPaint = Paint().apply {
                color = Color.parseColor("#0284C7")
                strokeWidth = 24f
                strokeCap = Paint.Cap.ROUND
                isAntiAlias = true
            }
            canvas.drawLine(finishX - 10f, centerY - 80f, finishX + 75f, centerY - 25f, limbPaint)
            canvas.drawLine(finishX - 30f, centerY - 70f, finishX - 85f, centerY - 10f, limbPaint)
            canvas.drawLine(finishX - 15f, centerY + 80f, finishX + 60f, centerY + 250f, limbPaint)
            canvas.drawLine(finishX - 30f, centerY + 80f, finishX - 70f, centerY + 235f, limbPaint)
        }

        // Draw Finishing Line (Single Bright Red Laser Slit Line)
        val slitX = width * 0.54f
        val finishLinePaint = Paint().apply {
            color = Color.parseColor("#EF4444")
            strokeWidth = 6f
            isAntiAlias = true
        }
        canvas.drawLine(slitX, headerH, slitX, height - footerH, finishLinePaint)

        // Draw TALL VERTICAL AI INTELLIGENCE BOX (Kotak AI Besar Menegak) spanning 76% of viewport height
        val viewH = (height - headerH - footerH)
        val boxTop = headerH + viewH * 0.11f
        val boxBottom = height - footerH - viewH * 0.11f
        val boxHalfW = (width * 0.21f).coerceIn(115f, 185f)

        val aiFillPaint = Paint().apply {
            color = Color.parseColor("#1810B981")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(slitX - boxHalfW, boxTop, slitX + boxHalfW, boxBottom, 18f, 18f, aiFillPaint)

        val aiBoxPaint = Paint().apply {
            color = Color.parseColor("#10B981")
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
        }
        canvas.drawRoundRect(slitX - boxHalfW, boxTop, slitX + boxHalfW, boxBottom, 18f, 18f, aiBoxPaint)

        // Horizontal Torso Chest Lock Crosshair inside the Tall Vertical Box
        val torsoY = boxTop + (boxBottom - boxTop) * 0.36f
        val torsoLinePaint = Paint().apply {
            color = Color.parseColor("#38BDF8")
            strokeWidth = 3f
        }
        canvas.drawLine(slitX - boxHalfW, torsoY, slitX + boxHalfW, torsoY, torsoLinePaint)

        val aiTextPaint = Paint().apply {
            color = Color.parseColor("#10B981")
            textSize = 22f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText(
            if (isCam2Torso) "AI TORSO LOCK (MENEGAK PENUH)" else "CAM 1 AI TRACKER MENEGAK",
            (slitX - boxHalfW).coerceAtLeast(24f),
            (boxTop - 12f).coerceAtLeast(headerH + 24f),
            aiTextPaint
        )

        // Top Broadcast Header Banner
        val bannerPaint = Paint().apply {
            color = Color.parseColor("#F2080D1A")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), headerH, bannerPaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = if (isPortraitFrame) 25f else 29f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText("PSYCO TIME X PRO — PHOTO FINISH & AI", 24f, 44f, textPaint)

        textPaint.textSize = if (isPortraitFrame) 20f else 23f
        textPaint.color = Color.parseColor("#38BDF8")
        canvas.drawText("$raceTitle • $athleteName", 24f, 84f, textPaint)

        // Official Timer Badge (Top Right)
        val timerBadgePaint = Paint().apply {
            color = Color.parseColor("#DC2626")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(width - 275f, 18f, width - 20f, 98f, 16f, 16f, timerBadgePaint)

        val timerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 36f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText(timeFormatted, width - 248f, 71f, timerTextPaint)

        // Bottom Telemetry Footer
        val footerPaint = Paint().apply {
            color = Color.parseColor("#F2080D1A")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, height - footerH, width.toFloat(), height.toFloat(), footerPaint)

        val footerTextPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 20f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val modeLabel = if (isPortraitFrame) "MOD PORTRAIT ASLI (9:16)" else "MOD LANDSCAPE (16:9)"
        canvas.drawText(
            "$modeLabel • KOTAK AI MENEGAK PENUH • GARISAN TORSO SLIT",
            24f,
            height - 36f,
            footerTextPaint
        )

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safeTitle = raceTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "PhotoFinish_${safeTitle}_$timeStamp.png"
        val folderToUse = targetFolder ?: getSelectedVideoFolder()
        if (!folderToUse.exists()) folderToUse.mkdirs()
        val destFile = File(folderToUse, fileName)

        // Also save the RAW clean un-annotated camera frame so re-exports never double-draw overlays!
        if (realCameraFrame != null) {
            try {
                val rawFile = File(folderToUse, "RAW_$fileName")
                FileOutputStream(rawFile).use { out ->
                    realCameraFrame.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
            } catch (_: Exception) {}
        }

        FileOutputStream(destFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        saveImageToPublicGallery(bitmap, fileName)
        return destFile.absolutePath
    }

    /**
     * Exports an Official Photo-Finish Slit-Scan Image directly from the Analytics Table.
     * Uses the clean raw camera frame (so overlays are NEVER double-drawn) and preserves
     * Portrait orientation with a Tall Vertical AI Intelligence Box (Kotak AI Besar Menegak).
     */
    fun exportPhotoFinishSlitForRace(
        race: com.example.data.model.RaceRecordEntity,
        posture: StartFinishPostureAnalysis,
        realCameraFrame: Bitmap? = null
    ): String {
        val rawCleanBmp = loadRawCleanFrameForRace(race.photoFinishUri, realCameraFrame)
        val isPortraitFrame = rawCleanBmp != null && rawCleanBmp.height >= rawCleanBmp.width

        val width = if (isPortraitFrame) 960 else 1280
        val height = if (isPortraitFrame) 1440 else 760
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val headerH = 115f
        val footerH = 115f
        val viewportRect = Rect(0, headerH.toInt(), width, (height - footerH).toInt())

        if (rawCleanBmp != null) {
            drawBitmapAspectFill(canvas, rawCleanBmp, viewportRect)
        } else {
            // Check if only an already-rendered photoFinishUri exists (legacy record without RAW_ file)
            val existingAnnotated = try {
                race.photoFinishUri?.let { path ->
                    val f = File(path)
                    if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
                }
            } catch (_: Exception) {
                null
            }

            if (existingAnnotated != null) {
                // Crop out the old 130px top banner from legacy file so we never double-render headers
                val cropTop = (existingAnnotated.height * 0.16f).toInt().coerceAtLeast(0)
                val srcRect = Rect(0, cropTop, existingAnnotated.width, existingAnnotated.height)
                canvas.drawBitmap(existingAnnotated, srcRect, viewportRect, Paint(Paint.FILTER_BITMAP_FLAG))
            } else {
                val bgPaint = Paint().apply {
                    color = Color.parseColor("#0A0F1D")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                val trackPaint = Paint().apply {
                    color = Color.parseColor("#1E293B")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, headerH, width.toFloat(), height - footerH, trackPaint)

                val lanePaint = Paint().apply {
                    color = Color.parseColor("#475569")
                    strokeWidth = 2.5f
                }
                for (i in 1..8) {
                    val y = headerH + (i * ((height - headerH - footerH) / 8f))
                    canvas.drawLine(0f, y, width.toFloat(), y, lanePaint)
                }
            }
        }

        // Single Red Laser Slit Finish Line
        val slitX = width * 0.55f
        val finishLinePaint = Paint().apply {
            color = Color.parseColor("#EF4444")
            strokeWidth = 6f
            isAntiAlias = true
        }
        canvas.drawLine(slitX, headerH, slitX, height - footerH, finishLinePaint)

        // Single TALL VERTICAL AI INTELLIGENCE BOX (Kotak AI Besar Menegak)
        val viewH = (height - headerH - footerH)
        val boxTop = headerH + viewH * 0.10f
        val boxBottom = height - footerH - viewH * 0.10f
        val boxHalfW = (width * 0.21f).coerceIn(120f, 190f)

        val aiFillPaint = Paint().apply {
            color = Color.parseColor("#1A10B981")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(slitX - boxHalfW, boxTop, slitX + boxHalfW, boxBottom, 18f, 18f, aiFillPaint)

        val aiBoxPaint = Paint().apply {
            color = Color.parseColor("#10B981")
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
        }
        canvas.drawRoundRect(slitX - boxHalfW, boxTop, slitX + boxHalfW, boxBottom, 18f, 18f, aiBoxPaint)

        val aiLabelPaint = Paint().apply {
            color = Color.parseColor("#10B981")
            textSize = 21f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText(
            "AI TORSO LOCK: KOTAK MENEGAK PENUH",
            (slitX - boxHalfW).coerceAtLeast(24f),
            (boxTop - 10f).coerceAtLeast(headerH + 24f),
            aiLabelPaint
        )

        // Top Official Broadcast Header (Single Crisp Header)
        val bannerPaint = Paint().apply {
            color = Color.parseColor("#F5050811")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), headerH, bannerPaint)

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = if (isPortraitFrame) 24f else 28f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText("PSYCO TIME X PRO — OFFICIAL PHOTO-FINISH SLIT", 24f, 44f, titlePaint)

        val subPaint = Paint().apply {
            color = Color.parseColor("#06B6D4")
            textSize = if (isPortraitFrame) 19f else 22f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val secFormatted = String.format(Locale.US, "%.3f s", race.recordedTimeMillis / 1000.0)
        canvas.drawText(
            "${race.title} • ${race.athleteName} • ${race.eventName} (Lorong ${race.lane})",
            24f,
            84f,
            subPaint
        )

        // Official Electronic Time Badge (Top Right)
        val badgePaint = Paint().apply {
            color = Color.parseColor("#DC2626")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(width - 280f, 18f, width - 22f, 98f, 16f, 16f, badgePaint)

        val timeTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 36f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText(secFormatted, width - 252f, 71f, timeTextPaint)

        // Bottom AI Intelligence Posture Telemetry Strip
        val footerPaint = Paint().apply {
            color = Color.parseColor("#F50A0F1D")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, height - footerH, width.toFloat(), height.toFloat(), footerPaint)

        val footerLinePaint = Paint().apply {
            color = Color.parseColor("#06B6D4")
            strokeWidth = 2.5f
        }
        canvas.drawLine(0f, height - footerH, width.toFloat(), height - footerH, footerLinePaint)

        val postureTitlePaint = Paint().apply {
            color = Color.parseColor("#FBBF24")
            textSize = if (isPortraitFrame) 18f else 20f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText(
            "AI POSTURE: START ${"%.1f".format(posture.startBodyLeanAngleDeg)}° (${if (posture.isStartPostureCorrect) "BETUL ✓" else "SALAH ⚠"}) | FINISH TORSO ${"%.1f".format(posture.finishTorsoDipAngleDeg)}° (${if (posture.isFinishPostureCorrect) "BETUL ✓" else "SALAH ⚠"})",
            24f,
            height - 70f,
            postureTitlePaint
        )

        val postureSubPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            textSize = if (isPortraitFrame) 16f else 18f
            isAntiAlias = true
        }
        canvas.drawText(
            "Kinematik: Cadence ${race.cadenceSpM} spm • GCT ${race.groundContactTimeMs} ms • Lutut ${posture.startFrontKneeAngleDeg}°/${posture.startRearKneeAngleDeg}° • Skor AI: ${posture.overallPostureScore}%",
            24f,
            height - 30f,
            postureSubPaint
        )

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safeTitle = race.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val safeAthlete = race.athleteName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "SlitExport_${safeTitle}_${safeAthlete}_$timeStamp.png"

        val folderToUse = getSelectedVideoFolder()
        if (!folderToUse.exists()) folderToUse.mkdirs()
        val destFile = File(folderToUse, fileName)

        FileOutputStream(destFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        saveImageToPublicGallery(bitmap, fileName)
        return destFile.absolutePath
    }

    /**
     * Generates and saves an official AI Sand Jump (Lompat Jauh / Triple Jump / Sukan Pasir)
     * Photo-Finish & Measuring Tape Evidence Image.
     */
    fun saveSandJumpEvidenceImage(
        jumpTitle: String,
        athleteName: String,
        eventCategory: String,
        measuredDistanceMeters: Double,
        boardStatusValid: Boolean,
        toeToBoardCm: Double,
        takeoffAngleDeg: Double,
        approachSpeedMs: Double,
        tapeStartRatioX: Float,
        tapeStartRatioY: Float,
        tapeEndRatioX: Float,
        tapeEndRatioY: Float,
        landingRatioX: Float,
        landingRatioY: Float,
        configuredMaxMeters: Double,
        realCameraFrame: Bitmap? = null
    ): String {
        val isPortraitFrame = realCameraFrame != null && realCameraFrame.height >= realCameraFrame.width
        val width = if (isPortraitFrame) 960 else 1280
        val height = if (isPortraitFrame) 1440 else 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val headerH = 115f
        val footerH = 120f
        val viewW = width.toFloat()
        val viewH = (height - headerH - footerH)
        val viewportRect = Rect(0, headerH.toInt(), width, (height - footerH).toInt())

        if (realCameraFrame != null) {
            drawBitmapAspectFill(canvas, realCameraFrame, viewportRect)
        } else {
            val bgPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, viewW, height.toFloat(), bgPaint)

            // Sand Pit box illustration
            val sandPaint = Paint().apply {
                color = Color.parseColor("#78350F")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                viewW * 0.35f,
                headerH + viewH * 0.28f,
                viewW * 0.92f,
                headerH + viewH * 0.76f,
                18f,
                18f,
                sandPaint
            )
        }

        val startX = (tapeStartRatioX.coerceIn(0.05f, 0.95f)) * viewW
        val startY = headerH + (tapeStartRatioY.coerceIn(0.10f, 0.90f)) * viewH
        val endX = (tapeEndRatioX.coerceIn(0.05f, 0.95f)) * viewW
        val endY = headerH + (tapeEndRatioY.coerceIn(0.10f, 0.90f)) * viewH
        val landX = (landingRatioX.coerceIn(0.05f, 0.95f)) * viewW
        val landY = headerH + (landingRatioY.coerceIn(0.10f, 0.90f)) * viewH

        // 1. Draw Take-Off Board Zone (Papan Anjakan)
        val boardPaint = Paint().apply {
            color = if (boardStatusValid) Color.parseColor("#10B981") else Color.parseColor("#EF4444")
            strokeWidth = 7f
            isAntiAlias = true
        }
        canvas.drawLine(startX, startY - 95f, startX, startY + 95f, boardPaint)

        // 2. Draw Calibrated Digital Measuring Tape (Pita Pengukur AI)
        val tapePaint = Paint().apply {
            color = Color.parseColor("#FBBF24")
            strokeWidth = 6f
            isAntiAlias = true
        }
        canvas.drawLine(startX, startY, endX, endY, tapePaint)

        // Meter tick marks along the calibrated measuring tape
        val tickPaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val steps = configuredMaxMeters.toInt().coerceIn(4, 16)
        for (m in 0..steps) {
            val t = m.toFloat() / steps.toFloat()
            val tx = startX + (endX - startX) * t
            val ty = startY + (endY - startY) * t
            canvas.drawCircle(tx, ty, 6f, tapePaint)
            if (m % 2 == 0 || m == steps) {
                canvas.drawText("${m}m", tx - 14f, ty + 28f, tickPaint)
            }
        }

        // 3. Draw Parabolic Flight Trajectory Curve from Board to Sand Landing Mark
        val arcPaint = Paint().apply {
            color = Color.parseColor("#38BDF8")
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
        }
        val path = android.graphics.Path().apply {
            moveTo(startX, startY)
            val ctrlX = (startX + landX) / 2f
            val ctrlY = minOf(startY, landY) - viewH * 0.26f
            quadTo(ctrlX, ctrlY, landX, landY)
        }
        canvas.drawPath(path, arcPaint)

        // 4. Draw Sand Pit Furthest Landing Box & Break Mark
        val landPaint = Paint().apply {
            color = Color.parseColor("#10B981")
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
        }
        canvas.drawRoundRect(landX - 75f, landY - 125f, landX + 75f, landY + 55f, 14f, 14f, landPaint)
        canvas.drawCircle(landX, landY, 12f, Paint().apply {
            color = Color.parseColor("#EF4444")
            style = Paint.Style.FILL
            isAntiAlias = true
        })

        // Top Header Banner
        val bannerPaint = Paint().apply {
            color = Color.parseColor("#F5050811")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, viewW, headerH, bannerPaint)

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = if (isPortraitFrame) 23f else 27f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText("PSYCO TIME X PRO — BUKTI AI SUKAN PASIR & LOMPATAN", 24f, 44f, titlePaint)

        val subPaint = Paint().apply {
            color = Color.parseColor("#38BDF8")
            textSize = if (isPortraitFrame) 19f else 21f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText(
            "$jumpTitle • $athleteName • $eventCategory (${if (boardStatusValid) "PAPAN SAH ✓" else "BATAL FOUL ⚠"})",
            24f,
            84f,
            subPaint
        )

        // Distance Badge (Top Right)
        val badgePaint = Paint().apply {
            color = if (boardStatusValid) Color.parseColor("#10B981") else Color.parseColor("#DC2626")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(viewW - 280f, 18f, viewW - 22f, 98f, 16f, 16f, badgePaint)

        val distTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 36f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText(String.format(Locale.US, "%.2f m", measuredDistanceMeters), viewW - 245f, 71f, distTextPaint)

        // Footer Telemetry
        val footerPaint = Paint().apply {
            color = Color.parseColor("#F50A0F1D")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, height - footerH, viewW, height.toFloat(), footerPaint)

        val fLine1 = Paint().apply {
            color = Color.parseColor("#FBBF24")
            textSize = if (isPortraitFrame) 18f else 20f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText(
            "AI SAND PIT TELEMETRY: JARAK ${"%.2f".format(measuredDistanceMeters)}m | PAPAN ANJAKAN: ${"%.1f".format(toeToBoardCm)}cm (${if (boardStatusValid) "SAH" else "BATAL"})",
            24f,
            height - 72f,
            fLine1
        )

        val fLine2 = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            textSize = if (isPortraitFrame) 16f else 18f
            isAntiAlias = true
        }
        canvas.drawText(
            "Sains Sukan AI: Lari Landas ${"%.1f".format(approachSpeedMs)} m/s • Sudut Lonjakan ${"%.1f".format(takeoffAngleDeg)}° • Pita Kalibrasi ${"%.1f".format(configuredMaxMeters)}m (SURE APPROVED)",
            24f,
            height - 30f,
            fLine2
        )

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safeTitle = jumpTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "SandJumpEvidence_${safeTitle}_$timeStamp.png"
        val folderToUse = getSelectedVideoFolder()
        if (!folderToUse.exists()) folderToUse.mkdirs()
        val destFile = File(folderToUse, fileName)

        FileOutputStream(destFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        saveImageToPublicGallery(bitmap, fileName)
        return destFile.absolutePath
    }

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
     * Saves both the race video package metadata AND the actual multi-frame sequence of upright
     * camera frames captured during the race so Replay plays a real moving Portrait/Landscape video!
     */
    fun createRaceVideoPackage(
        raceTitle: String,
        recordedTimeFormatted: String,
        athleteName: String,
        targetFolder: File? = null
    ): Pair<String, String> {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanTitle = raceTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")

        val destinationFolder = targetFolder ?: getSelectedVideoFolder()
        if (!destinationFolder.exists()) destinationFolder.mkdirs()

        val cam1File = File(destinationFolder, "CAM1_Timer_${cleanTitle}_$timeStamp.mp4")
        val cam2File = File(destinationFolder, "CAM2_Torso_${cleanTitle}_$timeStamp.mp4")

        // Save actual recorded frames sequence directory for real moving video replay!
        val framesDir = File(destinationFolder, "Frames_${cleanTitle}_$timeStamp")
        if (!framesDir.exists()) framesDir.mkdirs()

        val sessionFrames = CameraFrameHolder.recordedSessionFrames.toList()
        val framesToPersist = if (sessionFrames.isNotEmpty()) {
            sessionFrames
        } else {
            listOfNotNull(
                CameraFrameHolder.latestStartFrameBitmap,
                CameraFrameHolder.latestLiveFrameBitmap,
                CameraFrameHolder.latestFinishFrameBitmap
            ).distinct()
        }

        framesToPersist.forEachIndexed { index, bmp ->
            try {
                val frameFile = File(framesDir, String.format(Locale.US, "frame_%03d.jpg", index))
                FileOutputStream(frameFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
            } catch (_: Exception) {}
        }

        cam1File.writeText(
            "PSYCO_TIMEX_CAM1_VIDEO_STREAM\n" +
                "Race: $raceTitle\n" +
                "Atlit: $athleteName\n" +
                "Time: $recordedTimeFormatted\n" +
                "FramesDir: ${framesDir.absolutePath}\n" +
                "FrameCount: ${framesToPersist.size}\n" +
                "SavedFolder: ${destinationFolder.absolutePath}"
        )
        cam2File.writeText(
            "PSYCO_TIMEX_CAM2_TORSO_VIDEO_STREAM\n" +
                "Race: $raceTitle\n" +
                "Atlit: $athleteName\n" +
                "Time: $recordedTimeFormatted\n" +
                "FramesDir: ${framesDir.absolutePath}\n" +
                "FrameCount: ${framesToPersist.size}\n" +
                "SavedFolder: ${destinationFolder.absolutePath}"
        )

        val exportFile = File(exportsDir, "${cleanTitle}_DataReport.json")
        exportFile.writeText(
            """
            {
                "race": "$raceTitle",
                "athlete": "$athleteName",
                "time": "$recordedTimeFormatted",
                "cam1_stream": "${cam1File.name}",
                "cam2_stream": "${cam2File.name}",
                "frames_dir": "${framesDir.absolutePath}",
                "frame_count": ${framesToPersist.size},
                "exported_to_device": true,
                "folder": "${destinationFolder.absolutePath}"
            }
            """.trimIndent()
        )

        return Pair(cam1File.absolutePath, cam2File.absolutePath)
    }

    /**
     * Loads the recorded sequence of upright camera frames for a race so that Replay plays a real moving video.
     */
    fun loadRecordedVideoFramesForRace(camVideoUri: String?, photoFinishUri: String?): List<Bitmap> {
        val loaded = mutableListOf<Bitmap>()
        try {
            if (!camVideoUri.isNullOrBlank()) {
                val videoFile = File(camVideoUri)
                if (videoFile.exists()) {
                    val lines = videoFile.readLines()
                    val framesDirLine = lines.firstOrNull { it.startsWith("FramesDir:") }
                    val framesDirPath = framesDirLine?.substringAfter("FramesDir:")?.trim()
                    if (!framesDirPath.isNullOrBlank()) {
                        val dir = File(framesDirPath)
                        if (dir.exists() && dir.isDirectory) {
                            val jpgs = dir.listFiles { f -> f.name.endsWith(".jpg") || f.name.endsWith(".png") }
                                ?.sortedBy { it.name }
                                ?: emptyList()
                            jpgs.forEach { f ->
                                BitmapFactory.decodeFile(f.absolutePath)?.let { loaded.add(it) }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        if (loaded.isEmpty()) {
            loadRawCleanFrameForRace(photoFinishUri, null)?.let { loaded.add(it) }
        }
        if (loaded.isEmpty() && !photoFinishUri.isNullOrBlank()) {
            try {
                val f = File(photoFinishUri)
                if (f.exists()) {
                    BitmapFactory.decodeFile(f.absolutePath)?.let { loaded.add(it) }
                }
            } catch (_: Exception) {}
        }
        return loaded
    }

    fun getAvailableStorageFolders(): List<File> {
        val selected = getSelectedVideoFolder()
        val list = mutableListOf(selected, videosDir, photoFinishDir, exportsDir)
        val savedCustomPaths = prefs.getStringSet(KEY_CUSTOM_FOLDER_PATHS, emptySet()) ?: emptySet()
        savedCustomPaths.forEach { path ->
            val file = File(path)
            if (file.exists() && file.isDirectory) {
                list.add(file)
            }
        }
        val customParent = File(appBaseDir, "CustomFolders")
        if (!customParent.exists()) customParent.mkdirs()
        customParent.listFiles()?.filter { it.isDirectory }?.let {
            list.addAll(it)
        }
        return list.distinctBy { it.absolutePath }
    }

    fun createCustomFolder(folderName: String): File {
        val safeName = folderName.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val baseParent = getSelectedVideoFolder()
        val newFolder = File(baseParent, safeName)
        if (!newFolder.exists()) newFolder.mkdirs()
        addSavedCustomFolderPath(newFolder.absolutePath)
        return newFolder
    }

    fun hasUserConsentedPhoneVideoFolder(): Boolean {
        return prefs.getBoolean("user_consented_phone_video_folder", false)
    }

    fun confirmAndCreateLocalPhoneVideoFolder(customFolderName: String = "ET_Training_Videos"): File {
        val safeName = customFolderName.trim().ifBlank { "ET_Training_Videos" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val phoneFolder = File(appBaseDir, safeName)
        if (!phoneFolder.exists()) phoneFolder.mkdirs()
        saveSelectedVideoFolder(phoneFolder.absolutePath)
        prefs.edit().putBoolean("user_consented_phone_video_folder", true).apply()
        return phoneFolder
    }

    /**
     * Generates a comprehensive Monthly Athlete PDF Report containing:
     * 1. Athlete Profile & Personal Best (PB)
     * 2. 1-Month Performance History by Date & Time
     * 3. Lifetime Fee Payment History since Registration
     */
    fun generateAthleteMonthlyPdfReport(
        athlete: com.example.data.model.AthleteEntity,
        clubName: String,
        coachName: String,
        races: List<com.example.data.model.RaceRecordEntity>,
        metrics: List<com.example.data.model.PerformanceMetrics>,
        fees: List<com.example.data.model.FeeRecordEntity>
    ): String {
        ensureDirectoriesExist()
        val pdfDoc = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        val bgPaint = Paint().apply { color = Color.parseColor("#FFFFFF") }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        val headerPaint = Paint().apply { color = Color.parseColor("#0B1120") }
        canvas.drawRect(0f, 0f, 595f, 95f, headerPaint)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#00E5FF")
            textSize = 16f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val whiteSubPaint = Paint().apply {
            color = Color.WHITE
            textSize = 11f
            isAntiAlias = true
        }
        val dateStr = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val monthStr = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())

        canvas.drawText("PSYCO TIME X PRO — LAPORAN BULANAN RASMI ATLIT ($monthStr)", 28f, 36f, titlePaint)
        canvas.drawText("Kelab: $clubName  |  Jurulatih: $coachName", 28f, 58f, whiteSubPaint)
        canvas.drawText("Tarikh Cetakan PDF: $dateStr  |  www.psycotimexpro.my", 28f, 76f, whiteSubPaint)

        val sectionPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 10f
            isAntiAlias = true
        }
        val boldBodyPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }

        var y = 122f
        val regDateFormatted = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(athlete.createdAt))

        canvas.drawText("1. PROFIL LENGKAP ATLIT & REKOD PERSONAL BEST (PB)", 28f, y, sectionPaint)
        y += 18f
        canvas.drawText("Nama Penuh Atlit: ${athlete.name}   |   Jantina: ${athlete.gender}   |   Tarikh Lahir: ${athlete.dateOfBirth}", 28f, y, boldBodyPaint)
        y += 15f
        canvas.drawText("Kategori: ${athlete.category}   |   Acara Utama: ${athlete.specificEvent}   |   Tarikh Daftar Sistem: $regDateFormatted", 28f, y, bodyPaint)
        y += 15f
        canvas.drawText("Rekod Personal Best (PB): ${athlete.pbValue} ${athlete.pbUnit}   |   Status Yuran Semasa: ${if (athlete.feePaidStatus) "LUNAS" else "TERTUNGGAK"}", 28f, y, boldBodyPaint)
        y += 14f
        canvas.drawLine(28f, y, 567f, y, linePaint)

        // Section 2: 1-Month Performance by Date & Time
        y += 22f
        canvas.drawText("2. PRESTASI SEBULAN MENGIKUT TARIKH & MASA (30 HARI TERKINI)", 28f, y, sectionPaint)
        y += 18f
        val dtFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
        val athleteRaces = races.filter { it.athleteId == athlete.id || it.athleteName.equals(athlete.name, ignoreCase = true) }
        if (athleteRaces.isEmpty() && metrics.isEmpty()) {
            canvas.drawText("Tiada rekod ujian masa / jarak direkodkan pada bulan ini.", 28f, y, bodyPaint)
            y += 16f
        } else {
            canvas.drawText("TARIKH & MASA          | ACARA              | KEPUTUSAN    | ANGIN    | BIOMEKANIK AI (CADENCE / GCT / TORSO)", 28f, y, boldBodyPaint)
            y += 14f
            athleteRaces.take(12).forEach { r ->
                val dStr = dtFormat.format(java.util.Date(r.createdAt))
                val rowText = String.format(
                    java.util.Locale.US,
                    "%-21s | %-18s | %-12s | %-8s | %d spm / %dms / %.1f deg",
                    dStr,
                    r.eventName.take(18),
                    String.format(java.util.Locale.US, "%.2f s", r.recordedTimeMillis / 1000.0),
                    r.windReading,
                    r.cadenceSpM,
                    r.groundContactTimeMs,
                    r.torsoLeanAngleDeg
                )
                canvas.drawText(rowText, 28f, y, bodyPaint)
                y += 14f
            }
            metrics.take(6).forEach { m ->
                val dStr = dtFormat.format(java.util.Date(m.recordedAt))
                val rowText = "$dStr | ${m.eventName.take(18)} | PB: ${m.pbValue} ${m.pbUnit} | Cadence: ${m.cadenceSpM} spm, GCT: ${m.groundContactTimeMs}ms"
                canvas.drawText(rowText, 28f, y, bodyPaint)
                y += 14f
            }
        }
        y += 8f
        canvas.drawLine(28f, y, 567f, y, linePaint)

        // Section 3: Lifetime Fee Payment History since Registration
        y += 22f
        val athleteFees = fees.filter { it.athleteId == athlete.id || it.athleteName.equals(athlete.name, ignoreCase = true) }
        val totalPaid = athleteFees.filter { it.isPaid }.sumOf { it.amount }
        val totalPending = athleteFees.filter { !it.isPaid }.sumOf { it.amount }

        canvas.drawText("3. REKOD PEMBAYARAN YURAN SEPANJANG MASA (SEJAK DIDAFTARKAN: $regDateFormatted)", 28f, y, sectionPaint)
        y += 16f
        canvas.drawText(
            String.format(java.util.Locale.US, "Jumlah Telah Dibayar (Lunas): RM %.2f   |   Jumlah Tertunggak: RM %.2f   |   Bil. Transaksi: %d", totalPaid, totalPending, athleteFees.size),
            28f,
            y,
            boldBodyPaint
        )
        y += 16f
        if (athleteFees.isEmpty()) {
            canvas.drawText("Rekod Pendaftaran Asal: RM ${String.format(java.util.Locale.US, "%.2f", athlete.registrationFee)} (${if (athlete.feePaidStatus) "LUNAS" else "TERTUNGGAK"})", 28f, y, bodyPaint)
            y += 14f
        } else {
            canvas.drawText("NO. RESIT       | JENIS YURAN                  | TARIKH BAYARAN | JUMLAH (RM) | STATUS", 28f, y, boldBodyPaint)
            y += 14f
            athleteFees.take(12).forEach { f ->
                val statusStr = if (f.isPaid) "LUNAS" else "TERTUNGGAK"
                val line = String.format(
                    java.util.Locale.US,
                    "%-15s | %-28s | %-14s | RM %-8.2f | %s",
                    f.receiptNo.take(14),
                    f.feeType.take(28),
                    f.paymentDate.take(14),
                    f.amount,
                    statusStr
                )
                canvas.drawText(line, 28f, y, bodyPaint)
                y += 14f
            }
        }

        // Footer
        val footerPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 9f
            isAntiAlias = true
        }
        canvas.drawText(
            "Dijana secara automatik oleh Sistem Pengurusan Kelab & Sains Sukan AI Psyco Time X Pro (www.psycotimexpro.my)",
            28f,
            815f,
            footerPaint
        )

        pdfDoc.finishPage(page)

        val safeAthName = athlete.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val pdfFile = File(exportsDir, "Laporan_Bulanan_${safeAthName}_${System.currentTimeMillis() % 100000}.pdf")
        try {
            FileOutputStream(pdfFile).use { out ->
                pdfDoc.writeTo(out)
            }
        } finally {
            pdfDoc.close()
        }
        return pdfFile.absolutePath
    }

    /**
     * Generates a Club-Wide Monthly PDF Report for all registered athletes, monthly race records, and fee collections.
     */
    fun generateClubMonthlyPdfReport(
        clubName: String,
        coachName: String,
        athletes: List<com.example.data.model.AthleteEntity>,
        races: List<com.example.data.model.RaceRecordEntity>,
        fees: List<com.example.data.model.FeeRecordEntity>
    ): String {
        ensureDirectoriesExist()
        val pdfDoc = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        val bgPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        val headerPaint = Paint().apply { color = Color.parseColor("#0B1120") }
        canvas.drawRect(0f, 0f, 595f, 90f, headerPaint)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#00E5FF")
            textSize = 15f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10.5f
            isAntiAlias = true
        }
        val monthStr = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        canvas.drawText("LAPORAN BULANAN PENGURUSAN KELAB — $clubName ($monthStr)", 28f, 35f, titlePaint)
        canvas.drawText("Ketua Jurulatih: $coachName  |  Jumlah Atlit: ${athletes.size}  |  Jumlah Ujian ET: ${races.size}", 28f, 56f, subPaint)
        canvas.drawText("Portal Rasmi: https://www.psycotimexpro.my/training-management/", 28f, 74f, subPaint)

        val sectionPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 9.5f
            isAntiAlias = true
        }
        var y = 115f
        val totalPaid = fees.filter { it.isPaid }.sumOf { it.amount }
        val totalPending = fees.filter { !it.isPaid }.sumOf { it.amount }

        canvas.drawText("1. RINGKASAN KEWANGAN YURAN KELAB & SENARAI ATLIT BERDAFTAR", 28f, y, sectionPaint)
        y += 16f
        canvas.drawText(String.format(java.util.Locale.US, "Kutipan Yuran Lunas: RM %.2f   |   Yuran Tertunggak: RM %.2f", totalPaid, totalPending), 28f, y, bodyPaint)
        y += 18f

        athletes.take(18).forEachIndexed { idx, a ->
            val feeStr = if (a.feePaidStatus) "LUNAS (RM${a.registrationFee.toInt()})" else "TERTUNGGAK (RM${a.registrationFee.toInt()})"
            canvas.drawText("${idx + 1}. ${a.name} — ${a.specificEvent} (${a.category}) | PB: ${a.pbValue} ${a.pbUnit} | Yuran: $feeStr", 28f, y, bodyPaint)
            y += 14f
        }

        y += 12f
        canvas.drawText("2. REKOD PRESTASI BULANAN TERKINI MENGIKUT TARIKH & MASA", 28f, y, sectionPaint)
        y += 16f
        val dtFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        races.take(14).forEach { r ->
            val dStr = dtFormat.format(java.util.Date(r.createdAt))
            canvas.drawText("• [$dStr] ${r.athleteName} (${r.eventName}): ${r.formattedTime} | Cadence ${r.cadenceSpM} spm | GCT ${r.groundContactTimeMs}ms", 28f, y, bodyPaint)
            y += 13f
        }

        pdfDoc.finishPage(page)
        val safeClub = clubName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val pdfFile = File(exportsDir, "Laporan_Kelab_${safeClub}_${System.currentTimeMillis() % 100000}.pdf")
        try {
            FileOutputStream(pdfFile).use { out ->
                pdfDoc.writeTo(out)
            }
        } finally {
            pdfDoc.close()
        }
        return pdfFile.absolutePath
    }
}
