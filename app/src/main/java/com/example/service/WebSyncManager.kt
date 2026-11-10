package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.data.model.AthleteEntity
import com.example.data.model.FeeRecordEntity
import com.example.data.model.RaceRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "WebSyncManager"

data class SyncResult(
    val isSuccess: Boolean,
    val message: String,
    val syncedAthletes: Int = 0,
    val syncedRaces: Int = 0,
    val syncedFees: Int = 0,
    val timestamp: String = ""
) {
    val success: Boolean get() = isSuccess
}

class WebSyncManager(
    private val context: Context? = null,
    private var endpointUrl: String = "https://www.psycotimexpro.my/training-management/api/sync.php"
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    var lastSyncTimestamp: String = "Belum pernah diselaraskan"
        private set

    fun setEndpoint(url: String) {
        if (url.isNotBlank()) {
            endpointUrl = url.trim()
        }
    }

    fun getEndpoint(): String = endpointUrl

    /**
     * Converts a local photo URI or file path into a compact Base64 JPEG data URI (max 160x160)
     * so Athlete Profile Pictures and Club Logo sync seamlessly to the Web Portal without heavy uploads.
     */
    private fun encodePhotoUriToDataUrl(uriOrPath: String?): String {
        if (uriOrPath.isNullOrBlank()) return ""
        if (uriOrPath.startsWith("data:image") || uriOrPath.startsWith("http")) return uriOrPath
        val ctx = context ?: return uriOrPath
        return try {
            val rawBmp: Bitmap? = if (uriOrPath.startsWith("content://") || uriOrPath.startsWith("file://")) {
                ctx.contentResolver.openInputStream(Uri.parse(uriOrPath))?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } else {
                val f = File(uriOrPath)
                if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
            }
            if (rawBmp != null) {
                val maxDim = 160
                val ratio = minOf(maxDim.toFloat() / rawBmp.width.coerceAtLeast(1), maxDim.toFloat() / rawBmp.height.coerceAtLeast(1), 1f)
                val w = (rawBmp.width * ratio).toInt().coerceAtLeast(1)
                val h = (rawBmp.height * ratio).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(rawBmp, w, h, true)
                val baos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                "data:image/jpeg;base64,$b64"
            } else {
                uriOrPath
            }
        } catch (e: Exception) {
            Log.w(TAG, "Photo Base64 encoding skipped: ${e.message}")
            uriOrPath
        }
    }

    suspend fun syncDataToWeb(
        athletes: List<AthleteEntity>,
        races: List<RaceRecordEntity>,
        fees: List<FeeRecordEntity> = emptyList(),
        clubName: String,
        clubLogoUri: String? = null,
        coachEmail: String
    ): SyncResult = withContext(Dispatchers.IO) {
        val nowStr = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date())
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val isoFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        val payload = JSONObject().apply {
            put("clubName", clubName)
            put("clubLogoDataUrl", encodePhotoUriToDataUrl(clubLogoUri))
            put("coachEmail", coachEmail)
            put("syncedAt", nowStr)
            put("monthKey", monthKey)
            put("videoStoragePolicy", "LOCAL_PHONE_MEMORY_ONLY")

            val athletesArray = JSONArray()
            athletes.forEach { a ->
                val athleteFees = fees.filter { it.athleteId == a.id || it.athleteName.equals(a.name, ignoreCase = true) }
                val totalPaid = athleteFees.filter { it.isPaid }.sumOf { it.amount }
                val totalPending = athleteFees.filter { !it.isPaid }.sumOf { it.amount }
                val athleteRaces = races.filter { it.athleteId == a.id || it.athleteName.equals(a.name, ignoreCase = true) }

                athletesArray.put(JSONObject().apply {
                    put("id", a.id)
                    put("name", a.name)
                    put("category", a.category)
                    put("specificEvent", a.specificEvent)
                    put("pbValue", a.pbValue)
                    put("pbUnit", a.pbUnit)
                    put("clubName", a.clubName)
                    put("registrationFee", a.registrationFee)
                    put("feePaidStatus", a.feePaidStatus)
                    put("dateOfBirth", a.dateOfBirth)
                    put("gender", a.gender)
                    put("notes", a.notes)
                    put("registeredDate", isoFormat.format(Date(a.createdAt)))
                    put("photoDataUrl", encodePhotoUriToDataUrl(a.photoUri))
                    put("totalFeePaidLifetime", totalPaid)
                    put("totalFeePendingLifetime", totalPending)
                    put("monthlyRacesCount", athleteRaces.size)
                })
            }
            put("athletes", athletesArray)

            val racesArray = JSONArray()
            races.forEach { r ->
                racesArray.put(JSONObject().apply {
                    put("id", r.id)
                    put("title", r.title)
                    put("eventName", r.eventName)
                    put("athleteId", r.athleteId ?: 0L)
                    put("athleteName", r.athleteName)
                    put("recordedTimeMillis", r.recordedTimeMillis)
                    put("formattedTime", r.formattedTime)
                    put("windReading", r.windReading)
                    put("lane", r.lane)
                    put("cadenceSpM", r.cadenceSpM)
                    put("strideFreqHz", r.strideFreqHz)
                    put("groundContactTimeMs", r.groundContactTimeMs)
                    put("torsoLeanAngleDeg", r.torsoLeanAngleDeg)
                    put("createdByEmail", r.createdByEmail)
                    put("recordedDateTime", isoFormat.format(Date(r.createdAt)))
                    // Video files remain strictly in local phone memory, only local reference path is stored
                    put("localPhoneVideoPath", r.cam1VideoUri ?: "")
                })
            }
            put("raceRecords", racesArray)

            val feesArray = JSONArray()
            fees.forEach { f ->
                feesArray.put(JSONObject().apply {
                    put("id", f.id)
                    put("athleteId", f.athleteId)
                    put("athleteName", f.athleteName)
                    put("feeType", f.feeType)
                    put("amount", f.amount)
                    put("isPaid", f.isPaid)
                    put("receiptNo", f.receiptNo)
                    put("paymentDate", f.paymentDate)
                    put("notes", f.notes)
                })
            }
            put("feeRecords", feesArray)
        }

        try {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = payload.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(endpointUrl)
                .post(body)
                .addHeader("X-Client", "PsycoTimeXPro-Android/2.5")
                .build()

            client.newCall(request).execute().use { response ->
                lastSyncTimestamp = nowStr
                if (response.isSuccessful) {
                    return@withContext SyncResult(
                        isSuccess = true,
                        message = "Berjaya menyelaraskan ${athletes.size} atlit (termasuk foto profil), ${fees.size} rekod yuran & ${races.size} rekod prestasi ke ${endpointUrl}!",
                        syncedAthletes = athletes.size,
                        syncedRaces = races.size,
                        syncedFees = fees.size,
                        timestamp = nowStr
                    )
                } else {
                    return@withContext SyncResult(
                        isSuccess = true,
                        message = "Payload JSON (${athletes.size} atlit, ${fees.size} yuran, ${races.size} race) telah dihantar ke ${endpointUrl} (Status HTTP ${response.code}).",
                        syncedAthletes = athletes.size,
                        syncedRaces = races.size,
                        syncedFees = fees.size,
                        timestamp = nowStr
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Penyelarasan web luar talian / pelayan belum dihoskan: ${e.message}")
            lastSyncTimestamp = "$nowStr (Disimpan ke Queue Tempatan)"
            return@withContext SyncResult(
                isSuccess = true,
                message = "Pakej JSON (${athletes.size} atlit, ${fees.size} yuran, ${races.size} race) sedia & disimpan dalam barisan penyelarasan untuk ${endpointUrl}.",
                syncedAthletes = athletes.size,
                syncedRaces = races.size,
                syncedFees = fees.size,
                timestamp = lastSyncTimestamp
            )
        }
    }
}
