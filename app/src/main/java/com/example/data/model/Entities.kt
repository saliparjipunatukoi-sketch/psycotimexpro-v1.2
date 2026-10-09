package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "athletes")
data class Athlete(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String, // "Track" or "Field"
    val specificEvent: String, // e.g., "100m", "200m", "Lompat Jauh", "Lontar Peluru"
    val pbValue: String, // e.g. "10.45" for seconds, "7.35" for meters
    val pbUnit: String, // "Saat (s)", "Meter (m)", "Centimeter (cm)"
    val photoUri: String? = null,
    val clubName: String = "Psyco Track & Field Elite",
    val coachEmail: String = "saliparjipun.atukoi@gmail.com",
    val registrationFee: Double = 50.0,
    val feePaidStatus: Boolean = true,
    val dateOfBirth: String = "2008-05-14",
    val gender: String = "Lelaki",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// Alias for backwards compatibility with earlier components
typealias AthleteEntity = Athlete

@Entity(tableName = "training_sessions")
data class TrainingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionTitle: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val venue: String = "Stadium Balapan & Padang",
    val coachEmail: String = "saliparjipun.atukoi@gmail.com",
    val category: String = "Track", // "Track", "Field", "Combined"
    val notes: String = "",
    val isCompleted: Boolean = true
)

@Entity(tableName = "performance_metrics")
data class PerformanceMetrics(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val athleteId: Long,
    val athleteName: String,
    val sessionId: Long? = null,
    val eventName: String,
    val pbValue: String, // Personal Best value
    val pbUnit: String, // "Saat (s)", "Meter (m)"
    val isNewPb: Boolean = false,
    val measuredTimeMillis: Long? = null,
    val measuredDistanceOrHeight: Double? = null,
    val cadenceSpM: Int = 260, // Steps per minute
    val strideFrequencyHz: Double = 4.35, // Stride frequency Hz
    val groundContactTimeMs: Int = 108, // Contact time ms
    val torsoLeanAngleDeg: Double = 14.5, // Torso lean degrees
    val windSpeed: String = "+0.8 m/s",
    val notes: String = "",
    val recordedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "race_records")
data class RaceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, // "Race 1", "Race 2", custom edited title
    val eventName: String, // "100m Sprint", "200m", "110m Hurdles"
    val athleteId: Long? = null,
    val athleteName: String = "Atlit Terbuka",
    val recordedTimeMillis: Long, // e.g. 10240 ms -> 10.24 s
    val windReading: String = "+0.8 m/s",
    val lane: Int = 4,
    val createdByEmail: String = "saliparjipun.atukoi@gmail.com",
    val coachEmail: String = "saliparjipun.atukoi@gmail.com",
    val cam1VideoUri: String? = null,
    val cam2VideoUri: String? = null,
    val photoFinishUri: String? = null,
    val hasMotionDetected: Boolean = true,
    val cadenceSpM: Int = 260, // Steps per minute
    val strideFreqHz: Double = 4.35, // Stride Frequency Hz
    val groundContactTimeMs: Int = 108, // Milliseconds contact
    val torsoLeanAngleDeg: Double = 14.5, // Torso lean degrees at finish beam
    val notes: String = "",
    val syncedWithWeb: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() {
            val hours = (recordedTimeMillis / 3600000).toInt()
            val minutes = ((recordedTimeMillis % 3600000) / 60000).toInt()
            val seconds = ((recordedTimeMillis % 60000) / 1000).toInt()
            val millis = (recordedTimeMillis % 1000).toInt()
            return String.format("%d:%02d:%02d:%03d", hours, minutes, seconds, millis)
        }
}

@Entity(tableName = "sub_users")
data class SubUserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val fullName: String,
    val role: String, // "Urusetia", "Pegawai Teknikal", "Penolong Jurulatih"
    val parentCoachEmail: String,
    val pinCode: String = "1234",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "fee_records")
data class FeeRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val athleteId: Long,
    val athleteName: String,
    val feeType: String, // "Pendaftaran", "Yuran Bulanan", "Sewa Spike / Kit ET"
    val amount: Double,
    val isPaid: Boolean,
    val receiptNo: String,
    val paymentDate: String,
    val notes: String = ""
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val athleteId: Long,
    val athleteName: String,
    val tournamentName: String,
    val eventName: String,
    val medal: String, // "Emas", "Perak", "Gangsa", "Finalis"
    val resultRecord: String,
    val yearOrDate: String
)

data class ClubProfile(
    val clubName: String = "Psyco Track & Field Elite",
    val logoUri: String? = null,
    val coachName: String = "Coach Salipar Jipun",
    val coachEmail: String = "saliparjipun.atukoi@gmail.com",
    val coachPhone: String = "+60 12-345 6789",
    val coachPhotoUri: String? = null
)
