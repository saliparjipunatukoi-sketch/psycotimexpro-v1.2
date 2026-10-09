package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        Athlete::class,
        TrainingSession::class,
        PerformanceMetrics::class,
        RaceRecordEntity::class,
        SubUserEntity::class,
        FeeRecordEntity::class,
        AchievementEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun athleteDao(): AthleteDao
    abstract fun trainingSessionDao(): TrainingSessionDao
    abstract fun performanceMetricsDao(): PerformanceMetricsDao
    abstract fun raceDao(): RaceDao
    abstract fun subUserDao(): SubUserDao
    abstract fun feeDao(): FeeDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "psyco_timex_pro_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
