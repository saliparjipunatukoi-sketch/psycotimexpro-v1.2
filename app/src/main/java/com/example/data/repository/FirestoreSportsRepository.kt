package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AthleteEntity
import com.example.data.model.RaceRecordEntity
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

@IgnoreExtraProperties
data class CloudAthlete(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "Track",
    val specificEvent: String = "100m Sprint",
    val pbValue: String = "10.50",
    val pbUnit: String = "Saat (s)",
    val clubName: String = "Psyco Track & Field Elite",
    val coachEmail: String = "",
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudRaceRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "Race 1",
    val eventName: String = "100m Sprint",
    val athleteName: String = "Atlit Terbuka",
    val recordedTimeMillis: Long = 0L,
    val windReading: String = "+0.0 m/s",
    val lane: Long = 4L,
    val coachEmail: String = "",
    val cadenceSpM: Long? = null,
    val strideFreqHz: Double? = null,
    val groundContactTimeMs: Long? = null,
    val torsoLeanAngleDeg: Double? = null,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class FirestoreSportsRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth
        get() = Firebase.auth

    private fun requireUserId(): String =
        checkNotNull(auth.currentUser?.uid) { "User must be signed in before accessing Firestore" }

    suspend fun createAthlete(
        name: String,
        category: String = "Track",
        specificEvent: String = "100m Sprint",
        pbValue: String = "10.45",
        pbUnit: String = "Saat (s)",
        clubName: String = "Psyco Track & Field Elite",
        coachEmail: String = "coach@psycotimexpro.my",
        gender: String? = "Lelaki",
        dateOfBirth: String? = "2006-05-14",
        notes: String? = null,
        customDocumentId: String? = null
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val collectionRef = db.collection(COLLECTION_ATHLETES)
        val docRef = if (!customDocumentId.isNullOrBlank()) {
            collectionRef.document(customDocumentId)
        } else {
            collectionRef.document()
        }
        val payload = mapOf(
            "userId" to uid,
            "name" to name.take(120).ifBlank { "Atlit Baru" },
            "category" to category.take(40).ifBlank { "Track" },
            "specificEvent" to specificEvent.take(60).ifBlank { "100m Sprint" },
            "pbValue" to pbValue.take(40).ifBlank { "10.50" },
            "pbUnit" to pbUnit.take(40).ifBlank { "Saat (s)" },
            "clubName" to clubName.take(120).ifBlank { "Psyco Track & Field Elite" },
            "coachEmail" to coachEmail.take(120).ifBlank { "coach@psycotimexpro.my" },
            "gender" to gender?.take(30)?.takeIf { it.isNotBlank() },
            "dateOfBirth" to dateOfBirth?.take(30)?.takeIf { it.isNotBlank() },
            "notes" to notes?.take(500)?.takeIf { it.isNotBlank() },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun syncLocalAthleteToCloud(athlete: AthleteEntity): Result<String> {
        val safeDocId = "ath_${athlete.id}_${
            athlete.name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(40)
        }"
        return createAthlete(
            name = athlete.name,
            category = athlete.category,
            specificEvent = athlete.specificEvent,
            pbValue = athlete.pbValue,
            pbUnit = athlete.pbUnit,
            clubName = athlete.clubName,
            coachEmail = athlete.coachEmail,
            gender = athlete.gender,
            dateOfBirth = athlete.dateOfBirth,
            notes = athlete.notes,
            customDocumentId = safeDocId
        )
    }

    suspend fun getUserAthletes(): Result<List<CloudAthlete>> = runCatching {
        val uid = requireUserId()
        try {
            val snapshot = db.collection(COLLECTION_ATHLETES)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snapshot.toObjects(CloudAthlete::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, COLLECTION_ATHLETES)
            throw e
        }
    }

    suspend fun getAthleteById(athleteId: String): Result<CloudAthlete?> = runCatching {
        val docRef = db.collection(COLLECTION_ATHLETES).document(athleteId)
        try {
            val snapshot = docRef.get().await()
            snapshot.toObject(CloudAthlete::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    fun observeAthletes(userId: String = requireUserId()): Flow<List<CloudAthlete>> {
        return db.collection(COLLECTION_ATHLETES)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CloudAthlete::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
    }

    suspend fun createRaceRecord(
        title: String,
        eventName: String = "100m Sprint",
        athleteName: String = "Atlit Terbuka",
        recordedTimeMillis: Long = 10240L,
        windReading: String = "+0.8 m/s",
        lane: Int = 4,
        coachEmail: String = "coach@psycotimexpro.my",
        cadenceSpM: Int? = 260,
        strideFreqHz: Double? = 4.35,
        groundContactTimeMs: Int? = 108,
        torsoLeanAngleDeg: Double? = 14.5,
        notes: String? = null,
        customDocumentId: String? = null
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val collectionRef = db.collection(COLLECTION_RACES)
        val docRef = if (!customDocumentId.isNullOrBlank()) {
            collectionRef.document(customDocumentId)
        } else {
            collectionRef.document()
        }
        val payload = mapOf(
            "userId" to uid,
            "title" to title.take(120).ifBlank { "Race 1" },
            "eventName" to eventName.take(80).ifBlank { "100m Sprint" },
            "athleteName" to athleteName.take(120).ifBlank { "Atlit Terbuka" },
            "recordedTimeMillis" to recordedTimeMillis.coerceIn(0L, 86400000L),
            "windReading" to windReading.take(40).ifBlank { "+0.0 m/s" },
            "lane" to lane.coerceIn(1, 12),
            "coachEmail" to coachEmail.take(120).ifBlank { "coach@psycotimexpro.my" },
            "cadenceSpM" to cadenceSpM?.coerceIn(0, 500),
            "strideFreqHz" to strideFreqHz?.coerceIn(0.0, 20.0),
            "groundContactTimeMs" to groundContactTimeMs?.coerceIn(0, 2000),
            "torsoLeanAngleDeg" to torsoLeanAngleDeg?.coerceIn(-45.0, 90.0),
            "notes" to notes?.take(500)?.takeIf { it.isNotBlank() },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun syncLocalRaceToCloud(race: RaceRecordEntity): Result<String> {
        val safeDocId = "race_${race.id}_${race.createdAt}"
        return createRaceRecord(
            title = race.title,
            eventName = race.eventName,
            athleteName = race.athleteName,
            recordedTimeMillis = race.recordedTimeMillis,
            windReading = race.windReading,
            lane = race.lane,
            coachEmail = race.coachEmail,
            cadenceSpM = race.cadenceSpM,
            strideFreqHz = race.strideFreqHz,
            groundContactTimeMs = race.groundContactTimeMs,
            torsoLeanAngleDeg = race.torsoLeanAngleDeg,
            notes = race.notes,
            customDocumentId = safeDocId
        )
    }

    suspend fun getUserRaces(): Result<List<CloudRaceRecord>> = runCatching {
        val uid = requireUserId()
        try {
            val snapshot = db.collection(COLLECTION_RACES)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snapshot.toObjects(CloudRaceRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, COLLECTION_RACES)
            throw e
        }
    }

    fun observeRaces(userId: String = requireUserId()): Flow<List<CloudRaceRecord>> {
        return db.collection(COLLECTION_RACES)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CloudRaceRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
    }

    companion object {
        const val COLLECTION_ATHLETES = "athletes"
        const val COLLECTION_RACES = "race_records"
    }
}
