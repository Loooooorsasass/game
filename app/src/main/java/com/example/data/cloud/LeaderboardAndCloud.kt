package com.example.data.cloud

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

data class LeaderboardEntry(
    val rank: Int,
    val playerName: String,
    val avatarEmoji: String,
    val scoreText: String,
    val timeSec: Int,
    val stars: Int,
    val isCurrentPlayer: Boolean = false
)

object LeaderboardManager {
    fun getLevelLeaderboard(levelNumber: Int, playerBestTimeSec: Int?, playerStars: Int): List<LeaderboardEntry> {
        val list = mutableListOf<LeaderboardEntry>()

        if (playerBestTimeSec != null && playerBestTimeSec > 0) {
            list.add(
                LeaderboardEntry(
                    rank = 1,
                    playerName = "Bạn",
                    avatarEmoji = "🚀",
                    scoreText = "${playerBestTimeSec}s (${playerStars}★)",
                    timeSec = playerBestTimeSec,
                    stars = playerStars,
                    isCurrentPlayer = true
                )
            )
        }
        return list
    }

    fun getDailyLeaderboard(playerScoreSec: Int?): List<LeaderboardEntry> {
        val list = mutableListOf<LeaderboardEntry>()

        if (playerScoreSec != null && playerScoreSec > 0) {
            list.add(
                LeaderboardEntry(
                    rank = 1,
                    playerName = "Bạn",
                    avatarEmoji = "🚀",
                    scoreText = "${playerScoreSec}s (⭐⭐⭐)",
                    timeSec = playerScoreSec,
                    stars = 3,
                    isCurrentPlayer = true
                )
            )
        }
        return list
    }
}

object DailyChallengeManager {
    fun getTodayDateKey(): String {
        val now = LocalDate.now()
        return now.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    fun getTodaySeed(): ULong {
        val now = LocalDate.now()
        val seed = (now.year * 10000 + now.dayOfYear * 7919).toLong().toULong()
        return seed
    }

    const val DAILY_SIZE = 14
    const val DAILY_TARGET = 95
}
