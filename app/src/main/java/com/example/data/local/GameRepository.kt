package com.example.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class GameRepository(private val db: AppDatabase) {
    val progressFlow: Flow<GameProgressEntity?> = db.gameProgressDao().getProgress()
    val levelRecordsFlow: Flow<List<LevelRecordEntity>> = db.levelRecordDao().getAllRecords()
    val saveSlotFlow: Flow<SaveSlotEntity?> = db.saveSlotDao().getSaveSlot()
    val achievementsFlow: Flow<List<AchievementEntity>> = db.achievementDao().getAllAchievements()

    suspend fun ensureInitialized() {
        val currentProgress = db.gameProgressDao().getProgressOnce()
        if (currentProgress == null) {
            db.gameProgressDao().insertOrUpdate(
                GameProgressEntity(
                    id = 1,
                    highestCleared = 0,
                    totalStars = 0,
                    coins = 200,
                    gems = 15,
                    impossibleCleared = 0,
                    superCleared = false
                )
            )
        }

        // No fabricated achievements or fake data are pre-populated.
        // Achievements table remains empty waiting for actual player achievements or server sync.
    }

    suspend fun saveProgress(progress: GameProgressEntity) {
        db.gameProgressDao().insertOrUpdate(progress)
    }

    suspend fun recordLevelCompletion(levelId: String, stars: Int, timeSec: Int, moves: Int) {
        val existing = db.levelRecordDao().getRecordForLevel(levelId)
        val bestStars = existing?.let { maxOf(it.stars, stars) } ?: stars
        val bestTime = existing?.let { minOf(it.bestTimeSec, timeSec) } ?: timeSec
        val bestMoves = existing?.let { minOf(it.bestMoves, moves) } ?: moves

        db.levelRecordDao().insertOrUpdate(
            LevelRecordEntity(
                levelId = levelId,
                stars = bestStars,
                bestTimeSec = bestTime,
                bestMoves = bestMoves,
                completedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveGameSlot(slot: SaveSlotEntity) {
        db.saveSlotDao().save(slot)
    }

    suspend fun clearSaveSlot() {
        db.saveSlotDao().deleteSaveSlot()
    }

    suspend fun updateAchievementProgress(id: String, increment: Int = 1, absolute: Int? = null) {
        val ach = db.achievementDao().getById(id) ?: return
        val newProgress = absolute ?: (ach.currentProgress + increment)
        val isUnlocked = ach.isUnlocked || (newProgress >= ach.maxProgress)
        db.achievementDao().update(
            ach.copy(
                currentProgress = newProgress.coerceAtMost(ach.maxProgress),
                isUnlocked = isUnlocked
            )
        )
    }

    suspend fun claimAchievement(id: String): Int {
        val ach = db.achievementDao().getById(id) ?: return 0
        if (ach.isUnlocked && !ach.isClaimed) {
            db.achievementDao().update(ach.copy(isClaimed = true))
            val current = db.gameProgressDao().getProgressOnce() ?: GameProgressEntity()
            db.gameProgressDao().insertOrUpdate(current.copy(coins = current.coins + ach.rewardCoins))
            return ach.rewardCoins
        }
        return 0
    }

    suspend fun resetAllProgress() {
        db.levelRecordDao().clearAll()
        db.saveSlotDao().deleteSaveSlot()
        db.achievementDao().clearAll()
        db.gameProgressDao().insertOrUpdate(
            GameProgressEntity(
                id = 1,
                highestCleared = 0,
                totalStars = 0,
                coins = 100,
                gems = 10,
                impossibleCleared = 0,
                superCleared = false
            )
        )
    }

    // Room Database Methods cho MazeLevel
    suspend fun saveMazeLevel(level: MazeLevelEntity) {
        db.mazeLevelDao().insertLevel(level)
    }

    suspend fun getMazeLevel(levelId: String): MazeLevelEntity? {
        return db.mazeLevelDao().getLevelSync(levelId)
    }

    fun getMazeLevelFlow(levelId: String): Flow<MazeLevelEntity?> {
        return db.mazeLevelDao().getLevel(levelId)
    }
}

