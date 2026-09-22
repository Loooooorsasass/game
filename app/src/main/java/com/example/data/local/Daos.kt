package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameProgressDao {
    @Query("SELECT * FROM game_progress WHERE id = 1 LIMIT 1")
    fun getProgress(): Flow<GameProgressEntity?>

    @Query("SELECT * FROM game_progress WHERE id = 1 LIMIT 1")
    suspend fun getProgressOnce(): GameProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: GameProgressEntity)
}

@Dao
interface LevelRecordDao {
    @Query("SELECT * FROM level_records")
    fun getAllRecords(): Flow<List<LevelRecordEntity>>

    @Query("SELECT * FROM level_records WHERE levelId = :levelId LIMIT 1")
    suspend fun getRecordForLevel(levelId: String): LevelRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: LevelRecordEntity)

    @Query("DELETE FROM level_records")
    suspend fun clearAll()
}

@Dao
interface SaveSlotDao {
    @Query("SELECT * FROM save_slot WHERE id = 1 LIMIT 1")
    fun getSaveSlot(): Flow<SaveSlotEntity?>

    @Query("SELECT * FROM save_slot WHERE id = 1 LIMIT 1")
    suspend fun getSaveSlotOnce(): SaveSlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(saveSlot: SaveSlotEntity)

    @Query("DELETE FROM save_slot WHERE id = 1")
    suspend fun deleteSaveSlot()
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<AchievementEntity>)

    @Update
    suspend fun update(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AchievementEntity?

    @Query("DELETE FROM achievements")
    suspend fun clearAll()
}

@Dao
interface DailyChallengeDao {
    @Query("SELECT * FROM daily_challenge_records WHERE dateString = :dateString LIMIT 1")
    fun getRecordForDate(dateString: String): Flow<DailyChallengeRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: DailyChallengeRecordEntity)
}

/**
 * Data Access Object quản lý dữ liệu cấu trúc màn chơi mê cung trong Room
 */
@Dao
interface MazeLevelDao {
    @Query("SELECT * FROM maze_levels WHERE levelId = :levelId LIMIT 1")
    fun getLevel(levelId: String): Flow<MazeLevelEntity?>

    @Query("SELECT * FROM maze_levels WHERE levelId = :levelId LIMIT 1")
    suspend fun getLevelSync(levelId: String): MazeLevelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevel(level: MazeLevelEntity)

    @Query("SELECT * FROM maze_levels ORDER BY createdAt ASC")
    fun getAllLevels(): Flow<List<MazeLevelEntity>>

    @Query("DELETE FROM maze_levels WHERE levelId = :levelId")
    suspend fun deleteLevel(levelId: String)

    @Query("DELETE FROM maze_levels")
    suspend fun clearAllLevels()
}

