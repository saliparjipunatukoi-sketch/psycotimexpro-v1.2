package com.example.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private const val TAG = "RemoteAppControl"

data class RemoteAppStatus(
    val isSuperadmin: Boolean = false,
    val killSwitchActive: Boolean = false,
    val isApproved: Boolean = true,
    val userStatus: String = "active", // "active", "pending", "blocked", "expired", "system_locked"
    val plan: String = "trial",
    val planName: String = "Pelan Percubaan",
    val subscriptionEnd: String = "",
    val message: String = "",
    val lastCheckedTimestamp: Long = System.currentTimeMillis()
)

class RemoteAppControlManager(
    private var endpointUrl: String = "https://www.psycotimexpro.my/training-management/api/app-control.php"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val superAdminEmail = "saliparjipun.atukoi@gmail.com"

    suspend fun checkAppStatus(
        email: String,
        name: String = "",
        club: String = "",
        country: String = "Malaysia",
        phone: String = "",
        cert: String = "",
        localTrialExpiryMillis: Long = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000)
    ): RemoteAppStatus = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        val isSuper = trimmedEmail.equals(superAdminEmail, ignoreCase = true)

        if (isSuper) {
            return@withContext RemoteAppStatus(
                isSuperadmin = true,
                killSwitchActive = false,
                isApproved = true,
                userStatus = "active",
                plan = "superadmin",
                planName = "Superadmin Unlimited",
                subscriptionEnd = "2099-12-31",
                message = "Akses Master Coach Salipar Jipun disahkan."
            )
        }

        try {
            val encodedEmail = URLEncoder.encode(trimmedEmail, "UTF-8")
            val encodedName = URLEncoder.encode(name, "UTF-8")
            val encodedClub = URLEncoder.encode(club, "UTF-8")
            val encodedCountry = URLEncoder.encode(country, "UTF-8")
            val encodedPhone = URLEncoder.encode(phone, "UTF-8")
            val encodedCert = URLEncoder.encode(cert, "UTF-8")
            val requestUrl = "$endpointUrl?email=$encodedEmail&name=$encodedName&club=$encodedClub&country=$encodedCountry&phone=$encodedPhone&cert=$encodedCert"

            val request = Request.Builder()
                .url(requestUrl)
                .get()
                .addHeader("User-Agent", "PsycoTimeXPro-SecurityGuard/2.4")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful && bodyStr.isNotBlank() && bodyStr.trim().startsWith("{")) {
                    val json = JSONObject(bodyStr)
                    val status = json.optString("status", "active")
                    val killSwitch = json.optBoolean("killSwitchActive", false)
                    val isApproved = json.optBoolean("isApproved", status == "active")
                    val userStatus = json.optString("userStatus", if (isApproved) "active" else "pending")
                    val plan = json.optString("plan", "trial")
                    val planName = json.optString("planName", "Percubaan Percuma (7 Hari)")
                    val subEnd = json.optString("subscriptionEnd", "")
                    val msg = json.optString("message", "")

                    return@withContext RemoteAppStatus(
                        isSuperadmin = false,
                        killSwitchActive = killSwitch,
                        isApproved = isApproved,
                        userStatus = userStatus,
                        plan = plan,
                        planName = planName,
                        subscriptionEnd = subEnd,
                        message = msg,
                        lastCheckedTimestamp = System.currentTimeMillis()
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal menyemak status aplikasi dari pelayan: ${e.message}")
        }

        // Fallback pintar: Benarkan Akses Percuma 7 Hari (Auto Free 7 Days Trial) jika masih dalam tempoh sah
        val isWithinLocalTrial = System.currentTimeMillis() <= localTrialExpiryMillis
        RemoteAppStatus(
            isSuperadmin = isSuper,
            killSwitchActive = false,
            isApproved = isSuper || isWithinLocalTrial,
            userStatus = if (isSuper || isWithinLocalTrial) "active" else "expired",
            plan = if (isSuper) "superadmin" else "trial",
            planName = if (isSuper) "Superadmin Unlimited" else "Percubaan Percuma (7 Hari)",
            message = if (isSuper || isWithinLocalTrial) {
                "Akses Percubaan Percuma 7 Hari Aktif."
            } else {
                "Tempoh percubaan 7 hari telah tamat. Sila langgan pakej atau hubungi Admin WhatsApp +6019-5326399."
            }
        )
    }

    suspend fun toggleKillSwitch(
        adminEmail: String,
        active: Boolean,
        message: String = ""
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("adminEmail", adminEmail)
                put("action", "toggle_kill_switch")
                put("active", active)
                if (message.isNotBlank()) put("message", message)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = payload.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(endpointUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val success = json.optString("status") == "success"
                val msg = json.optString("message", "Perubahan berjaya dihantar ke pelayan.")
                return@withContext Pair(success, msg)
            }
        } catch (e: Exception) {
            return@withContext Pair(false, "Ralat sambungan: ${e.localizedMessage}")
        }
    }

    suspend fun approveUser(
        adminEmail: String,
        targetEmail: String,
        days: Int = 30,
        plan: String = "plan_rm30"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("adminEmail", adminEmail)
                put("action", "approve_user")
                put("targetEmail", targetEmail)
                put("days", days)
                put("plan", plan)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = payload.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(endpointUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val success = json.optString("status") == "success"
                val msg = json.optString("message", "Status pengguna dikemaskini.")
                return@withContext Pair(success, msg)
            }
        } catch (e: Exception) {
            return@withContext Pair(false, "Ralat sambungan: ${e.localizedMessage}")
        }
    }

    suspend fun blockUser(
        adminEmail: String,
        targetEmail: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("adminEmail", adminEmail)
                put("action", "block_user")
                put("targetEmail", targetEmail)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = payload.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(endpointUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val success = json.optString("status") == "success"
                val msg = json.optString("message", "Pengguna berjaya disekat.")
                return@withContext Pair(success, msg)
            }
        } catch (e: Exception) {
            return@withContext Pair(false, "Ralat sambungan: ${e.localizedMessage}")
        }
    }
}
