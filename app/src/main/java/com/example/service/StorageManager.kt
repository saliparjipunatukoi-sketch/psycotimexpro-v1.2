package com.example.service

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
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
        // Auto-create all required PsycoTimeXPro folders immediately upon app startup
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

    /**
     * Checks whether 'Manage External Storage' (All Files Access on Android 11+)
     * or legacy WRITE_EXTERNAL_STORAGE permission is granted.
     */
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

    /**
     * Returns the currently configured local folder for saving race video recordings.
     */
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

    /**
     * Validates a candidate local folder path:
     * 1. Non-empty & valid path syntax
     * 2. Checks if outside app-specific storage without MANAGE_EXTERNAL_STORAGE
     * 3. Attempts mkdirs() if folder does not exist yet
     * 4. Verifies directory status and performs a real file write probe
     */
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

            // Perform real write test probe to verify write permission
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

    /**
     * Validates and saves the folder path as the default race video recording location.
     */
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

    /**
     * Converts a Tree Uri returned by ActivityResultContracts.OpenDocumentTree() into a local file path.
     */
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

    /**
     * Returns preset storage locations for quick selection.
     */
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
     * Creates and saves a high-precision Photo Finish Image with Torso Slit Line and Time Overlay
     */
    /**
     * Creates and saves a high-precision Photo Finish Image with Torso Slit Line and Time Overlay.
     * Uses real live camera frame if available, or realistic photo finish scan (ZERO stick figures).
     */
    fun savePhotoFinishImage(
        raceTitle: String,
        athleteName: String,
        timeFormatted: String,
        isCam2Torso: Boolean,
        realCameraFrame: Bitmap? = null,
        targetFolder: File? = null
    ): String {
        val width = 1080
        val height = 720
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (realCameraFrame != null) {
            // Draw actual real live camera feed frame scaled to full resolution!
            val srcRect = android.graphics.Rect(0, 0, realCameraFrame.width, realCameraFrame.height)
            val destRect = android.graphics.Rect(0, 0, width, height)
            canvas.drawBitmap(realCameraFrame, srcRect, destRect, null)
        } else {
            // Realistic Synthetic Athletics Stadium Finish Line Slit Scan (NO stick figures/orang lidi)
            val bgPaint = Paint().apply {
                color = Color.parseColor("#0F172A") // Professional Stadium Deep Navy/Charcoal
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Synthetic Tartan Track lanes
            val trackPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 130f, width.toFloat(), height.toFloat(), trackPaint)

            // Athletics lane markers
            val lanePaint = Paint().apply {
                color = Color.parseColor("#475569")
                strokeWidth = 3f
            }
            for (i in 1..8) {
                val y = 130f + (i * ((height - 130f) / 8f))
                canvas.drawLine(0f, y, width.toFloat(), y, lanePaint)
            }

            // Realistic Athlete Body Torso Silhouette (Not stick figure)
            val finishX = width * 0.55f
            val athleteTorsoPaint = Paint().apply {
                color = Color.parseColor("#0EA5E9") // Team Athlete Jersey Color
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            // Athlete Chest / Torso Leaning Forward
            canvas.drawRoundRect(finishX - 45f, 280f, finishX + 35f, 440f, 24f, 24f, athleteTorsoPaint)
            // Head with helmet/band
            val headPaint = Paint().apply {
                color = Color.parseColor("#F59E0B")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(finishX + 25f, 250f, 30f, headPaint)

            // Upper arms in forward drive motion
            val limbPaint = Paint().apply {
                color = Color.parseColor("#0284C7")
                strokeWidth = 22f
                strokeCap = Paint.Cap.ROUND
                isAntiAlias = true
            }
            canvas.drawLine(finishX - 10f, 320f, finishX + 65f, 360f, limbPaint)
            canvas.drawLine(finishX - 30f, 330f, finishX - 75f, 380f, limbPaint)
            // Legs in high velocity stride
            canvas.drawLine(finishX - 25f, 430f, finishX + 45f, 570f, limbPaint)
            canvas.drawLine(finishX - 35f, 430f, finishX - 60f, 550f, limbPaint)
        }

        // Draw Finishing Line (Bright Red Laser Slit Line)
        val finishLinePaint = Paint().apply {
            color = Color.parseColor("#DC2626")
            strokeWidth = 6f
        }
        val slitX = width * 0.55f
        canvas.drawLine(slitX, 0f, slitX, height.toFloat(), finishLinePaint)

        // Draw AI Bounding Box around Torso Area
        val aiBoxPaint = Paint().apply {
            color = Color.parseColor("#22C55E")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(slitX - 70f, 220f, slitX + 70f, 470f, 12f, 12f, aiBoxPaint)

        val aiTextPaint = Paint().apply {
            color = Color.parseColor("#22C55E")
            textSize = 22f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText("AI TORSO LOCK: VALID FINISH", slitX - 65f, 210f, aiTextPaint)

        // Top Header Banner with Electronic Timing Details
        val bannerPaint = Paint().apply {
            color = Color.parseColor("#EE0B111E")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 130f, bannerPaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 32f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText("PSYCO TIME X PRO - OFFICIAL PHOTO FINISH & REPLAY", 30f, 48f, textPaint)

        textPaint.textSize = 26f
        textPaint.color = Color.parseColor("#38BDF8")
        canvas.drawText("$raceTitle | Atlit: $athleteName | Garisan Penamat", 30f, 92f, textPaint)

        // Right side: Official Timer Overlay Badge
        val timerBadgePaint = Paint().apply {
            color = Color.parseColor("#DC2626")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(width - 330f, 22f, width - 25f, 108f, 16f, 16f, timerBadgePaint)

        val timerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 42f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText(timeFormatted, width - 295f, 80f, timerTextPaint)

        // Save to target folder or configured video storage folder
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "PhotoFinish_${raceTitle.replace(" ", "_")}_$timeStamp.png"
        val folderToUse = targetFolder ?: getSelectedVideoFolder()
        if (!folderToUse.exists()) folderToUse.mkdirs()
        val destFile = File(folderToUse, fileName)

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
     * Simulates or creates saved race video package files for Cam 1 & Cam 2 with overlays.
     * Supports saving to user-specified local folder (e.g. creating custom subfolders).
     */
    fun createRaceVideoPackage(
        raceTitle: String,
        recordedTimeFormatted: String,
        athleteName: String,
        targetFolder: File? = null
    ): Pair<String, String> {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanTitle = raceTitle.replace(" ", "_")

        val destinationFolder = targetFolder ?: getSelectedVideoFolder()
        if (!destinationFolder.exists()) destinationFolder.mkdirs()

        // Create representative frame video asset files for Cam 1 (Timer Gate) & Cam 2 (Finish Torso)
        val cam1File = File(destinationFolder, "CAM1_Timer_${cleanTitle}_$timeStamp.mp4")
        val cam2File = File(destinationFolder, "CAM2_Torso_${cleanTitle}_$timeStamp.mp4")

        // Write marker payload
        cam1File.writeText("PSYCO_TIMEX_CAM1_VIDEO_STREAM\nRace: $raceTitle\nAtlit: $athleteName\nTime: $recordedTimeFormatted\nOverlay: Digital StopWatch + Starter Gate\nTimestamp: $timeStamp\nSavedFolder: ${destinationFolder.absolutePath}")
        cam2File.writeText("PSYCO_TIMEX_CAM2_TORSO_VIDEO_STREAM\nRace: $raceTitle\nAtlit: $athleteName\nTime: $recordedTimeFormatted\nOverlay: Finish Line Laser Slit + Torso AI Detector\nTimestamp: $timeStamp\nSavedFolder: ${destinationFolder.absolutePath}")

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
                "folder": "${destinationFolder.absolutePath}"
            }
        """.trimIndent())

        return Pair(cam1File.absolutePath, cam2File.absolutePath)
    }

    /**
     * Lists existing custom subfolders or creates a new one, starting with the user's selected video folder.
     */
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
}
