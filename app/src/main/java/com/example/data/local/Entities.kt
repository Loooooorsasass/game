package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_progress")
data class GameProgressEntity(
    @PrimaryKey val id: Int = 1,
    val highestCleared: Int = 0,
    val totalStars: Int = 0,
    val coins: Int = 100,
    val gems: Int = 10,
    val impossibleCleared: Int = 0,
    val superCleared: Boolean = false,
    val currentSkinId: String = "classic_blue",
    val currentMazeThemeId: String = "midnight_cyber",
    val unlockedSkins: String = "classic_blue",
    val unlockedThemes: String = "midnight_cyber",
    val isVipNoAds: Boolean = false,
    val hintCount: Int = 3,
    val skipTokens: Int = 1,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val lastCloudSyncTime: Long = 0L
)

@Entity(tableName = "level_records")
data class LevelRecordEntity(
    @PrimaryKey val levelId: String,
    val stars: Int,
    val bestTimeSec: Int,
    val bestMoves: Int,
    val completedAt: Long
)

@Entity(tableName = "save_slot")
data class SaveSlotEntity(
    @PrimaryKey val id: Int = 1,
    val defId: String,
    val tier: String? = null,
    val w: Int,
    val h: Int,
    val seedStr: String,
    val playerX: Int,
    val playerY: Int,
    val moves: Int,
    val elapsedSec: Int,
    val visitedIndicesJson: String,
    val pathHistoryJson: String
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val currentProgress: Int,
    val maxProgress: Int,
    val isUnlocked: Boolean,
    val isClaimed: Boolean,
    val rewardCoins: Int,
    val iconName: String
)

@Entity(tableName = "daily_challenge_records")
data class DailyChallengeRecordEntity(
    @PrimaryKey val dateString: String,
    val moves: Int,
    val elapsedSec: Int,
    val stars: Int,
    val completedAt: Long
)
