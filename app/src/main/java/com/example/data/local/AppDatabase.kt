package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        GameProgressEntity::class,
        LevelRecordEntity::class,
        SaveSlotEntity::class,
        AchievementEntity::class,
        DailyChallengeRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameProgressDao(): GameProgressDao
    abstract fun levelRecordDao(): LevelRecordDao
    abstract fun saveSlotDao(): SaveSlotDao
    abstract fun achievementDao(): AchievementDao
    abstract fun dailyChallengeDao(): DailyChallengeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "exact_maze_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
