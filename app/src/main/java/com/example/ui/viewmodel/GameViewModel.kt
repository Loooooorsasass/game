package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.audio.SoundManager
import com.example.core.engine.EAST
import com.example.core.engine.LevelDef
import com.example.core.engine.Maze
import com.example.core.engine.MazeBuilder
import com.example.core.engine.MazeConfig
import com.example.core.engine.MazeSolver
import com.example.core.engine.NORTH
import com.example.core.engine.Point
import com.example.core.engine.SOUTH
import com.example.core.engine.WEST
import com.example.core.haptics.HapticManager
import com.example.data.cloud.DailyChallengeManager
import com.example.data.local.AchievementEntity
import com.example.data.local.AppDatabase
import com.example.data.local.GameProgressEntity
import com.example.data.local.GameRepository
import com.example.data.local.LevelRecordEntity
import com.example.data.local.SaveSlotEntity
import com.example.data.shop.ShopCatalog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray

enum class AppScreen {
    HOME,
    PLAYING,
    REPLAY,
    LEADERBOARD,
    ACHIEVEMENTS,
    DAILY_CHALLENGE,
    SHOP
}

data class ActiveGameState(
    val maze: Maze? = null,
    val levelDef: LevelDef? = null,
    val player: Point = Point(0, 0),
    val moves: Int = 0,
    val elapsedSec: Int = 0,
    val visitedCells: Set<Int> = emptySet(),
    val pathHistory: List<Point> = emptyList(),
    val distCache: IntArray? = null,
    val wallHits: Int = 0,
    val gameOver: Boolean = false,
    val hasWon: Boolean = false,
    val earnedStars: Int = 0,
    val secPerCell: Double = 0.0,
    val isPaused: Boolean = false,
    val showExitDialog: Boolean = false,
    val showHint: Boolean = false,
    val justUnlockedImpossible: Boolean = false,
    val justUnlockedNextTier: Boolean = false,
    val nextTierDef: LevelDef? = null,
    val justClearedSuper: Boolean = false
)

data class ReplayState(
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val speed: Float = 1f
)

data class GameUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val progress: GameProgressEntity = GameProgressEntity(),
    val levelRecords: Map<String, LevelRecordEntity> = emptyMap(),
    val saveSlot: SaveSlotEntity? = null,
    val achievements: List<AchievementEntity> = emptyList(),
    val activeGame: ActiveGameState = ActiveGameState(),
    val replay: ReplayState = ReplayState(),
    val rewardAdPrompt: String? = null,
    val showCloudSyncDialog: Boolean = false,
    val showResetModal: Boolean = false,
    val selectedLeaderboardLevel: Int = 1,
    val toastMessage: String? = null
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = GameRepository(db)

    val soundManager = SoundManager(application)
    val hapticManager = HapticManager(application)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var replayJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }

        viewModelScope.launch {
            repository.progressFlow.collect { p ->
                if (p != null) {
                    _uiState.update { it.copy(progress = p) }
                    soundManager.isSoundEnabled = p.soundEnabled
                    soundManager.isMusicEnabled = p.musicEnabled
                    hapticManager.isHapticEnabled = p.hapticEnabled
                }
            }
        }

        viewModelScope.launch {
            repository.levelRecordsFlow.collect { records ->
                val map = records.associateBy { it.levelId }
                _uiState.update { it.copy(levelRecords = map) }
            }
        }

        viewModelScope.launch {
            repository.saveSlotFlow.collect { slot ->
                _uiState.update { it.copy(saveSlot = slot) }
            }
        }

        viewModelScope.launch {
            repository.achievementsFlow.collect { list ->
                _uiState.update { it.copy(achievements = list) }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        soundManager.playClick()
        if (screen == AppScreen.PLAYING && _uiState.value.activeGame.maze != null) {
            resumeTimer()
        } else if (screen != AppScreen.PLAYING) {
            pauseTimer()
        }
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun startLevel(def: LevelDef) {
        soundManager.playClick()
        val seed = MazeBuilder.randomSeed()
        val maze = MazeBuilder.buildMaze(def.w, def.h, def.target, seed)
        val distCache = MazeSolver.computeDistances(maze)

        _uiState.update {
            it.copy(
                currentScreen = AppScreen.PLAYING,
                activeGame = ActiveGameState(
                    maze = maze,
                    levelDef = def,
                    player = maze.start,
                    moves = 0,
                    elapsedSec = 0,
                    visitedCells = setOf(maze.start.y * maze.w + maze.start.x),
                    pathHistory = listOf(maze.start),
                    distCache = distCache,
                    wallHits = 0,
                    gameOver = false,
                    hasWon = false,
                    earnedStars = 0,
                    secPerCell = 0.0,
                    isPaused = false,
                    showExitDialog = false,
                    showHint = false
                )
            )
        }
        startTimer()
    }

    fun startDailyChallenge() {
        val seed = DailyChallengeManager.getTodaySeed()
        val def = LevelDef(
            id = "DAILY_${DailyChallengeManager.getTodayDateKey()}",
            numericLevel = null,
            tier = "DAILY",
            w = DailyChallengeManager.DAILY_SIZE,
            h = DailyChallengeManager.DAILY_SIZE,
            target = DailyChallengeManager.DAILY_TARGET,
            moveCap = null,
            vision = MazeConfig.VISION_WINDOW,
            isFinal = false,
            noTimer = false
        )
        val maze = MazeBuilder.buildMaze(def.w, def.h, def.target, seed)
        val distCache = MazeSolver.computeDistances(maze)

        _uiState.update {
            it.copy(
                currentScreen = AppScreen.PLAYING,
                activeGame = ActiveGameState(
                    maze = maze,
                    levelDef = def,
                    player = maze.start,
                    moves = 0,
                    elapsedSec = 0,
                    visitedCells = setOf(maze.start.y * maze.w + maze.start.x),
                    pathHistory = listOf(maze.start),
                    distCache = distCache,
                    wallHits = 0,
                    gameOver = false,
                    hasWon = false,
                    earnedStars = 0,
                    secPerCell = 0.0,
                    isPaused = false,
                    showExitDialog = false,
                    showHint = false
                )
            )
        }
        startTimer()
    }

    fun resumeFromSave() {
        val slot = _uiState.value.saveSlot ?: return
        soundManager.playClick()
        val isImp = slot.defId.startsWith("IMPOSSIBLE")
        val def = if (isImp) {
            if (slot.tier == "SUPER") MazeConfig.superDef()
            else MazeConfig.impossibleDefForTier(slot.tier?.toIntOrNull() ?: 1)
        } else {
            val num = slot.defId.toIntOrNull() ?: 1
            MazeConfig.generateLevelDef(num)
        }

        val seed = slot.seedStr.toULongOrNull() ?: 42UL
        val maze = MazeBuilder.buildMaze(def.w, def.h, def.target, seed)
        val distCache = MazeSolver.computeDistances(maze)

        val visitedSet = try {
            val jsonArr = JSONArray(slot.visitedIndicesJson)
            (0 until jsonArr.length()).map { jsonArr.getInt(it) }.toSet()
        } catch (_: Exception) {
            setOf(slot.playerY * def.w + slot.playerX)
        }

        val historyList = try {
            val jsonArr = JSONArray(slot.pathHistoryJson)
            (0 until jsonArr.length()).map { idx ->
                val str = jsonArr.getString(idx)
                val parts = str.split(",")
                Point(parts[0].toInt(), parts[1].toInt())
            }
        } catch (_: Exception) {
            listOf(Point(slot.playerX, slot.playerY))
        }

        _uiState.update {
            it.copy(
                currentScreen = AppScreen.PLAYING,
                activeGame = ActiveGameState(
                    maze = maze,
                    levelDef = def,
                    player = Point(slot.playerX, slot.playerY),
                    moves = slot.moves,
                    elapsedSec = slot.elapsedSec,
                    visitedCells = visitedSet,
                    pathHistory = historyList,
                    distCache = distCache,
                    wallHits = 0,
                    gameOver = false,
                    hasWon = false,
                    isPaused = false
                )
            )
        }
        startTimer()
    }

    fun tryMove(dx: Int, dy: Int) {
        val game = _uiState.value.activeGame
        val maze = game.maze ?: return
        if (game.gameOver || game.isPaused) return

        val (cx, cy) = game.player
        val dir = when {
            dx == 1 && dy == 0 -> EAST
            dx == -1 && dy == 0 -> WEST
            dx == 0 && dy == 1 -> NORTH
            dx == 0 && dy == -1 -> SOUTH
            else -> return
        }

        val mask = maze.cellAt(cx, cy)
        // Check if wall blocks this direction
        if ((mask and dir) == 0) {
            // Wall bump!
            soundManager.playBump()
            hapticManager.vibrateBump()
            _uiState.update {
                it.copy(activeGame = it.activeGame.copy(wallHits = it.activeGame.wallHits + 1))
            }
            return
        }

        // Valid move!
        val nx = cx + dx
        val ny = cy + dy
        val newPlayer = Point(nx, ny)
        val newMoves = game.moves + 1
        val nIdx = ny * maze.w + nx
        val newVisited = game.visitedCells + nIdx
        val newHistory = game.pathHistory + newPlayer

        soundManager.playStep()
        hapticManager.vibrateStep()

        // Check moveCap if present
        if (game.levelDef?.moveCap != null && newMoves > game.levelDef.moveCap) {
            handleLoss("🚫 Vượt quá ${game.levelDef.moveCap} bước di chuyển!")
            return
        }

        // Check BFS distance
        val dist = game.distCache?.get(nIdx) ?: 0

        _uiState.update {
            it.copy(
                activeGame = it.activeGame.copy(
                    player = newPlayer,
                    moves = newMoves,
                    visitedCells = newVisited,
                    pathHistory = newHistory
                )
            )
        }

        // Check win condition: must reach goal cell and have exact target distance
        if (nx == maze.goal.x && ny == maze.goal.y && dist == maze.target) {
            handleWin()
        }
    }

    private fun handleWin() {
        pauseTimer()
        soundManager.playWin()
        hapticManager.vibrateWin()

        val game = _uiState.value.activeGame
        val levelDef = game.levelDef ?: return
        val currentProgress = _uiState.value.progress

        var earnedStars = 0
        var spc = 0.0
        if (!levelDef.isFinal) {
            spc = game.elapsedSec.toDouble() / maxOf(1, levelDef.target)
            earnedStars = MazeConfig.starsFromPace(spc)
        }

        var justUnlockedImpossible = false
        var justUnlockedNextTier = false
        var nextTierDef: LevelDef? = null
        var justClearedSuper = false

        var newHighest = currentProgress.highestCleared
        var newImpCleared = currentProgress.impossibleCleared
        var newSuperCleared = currentProgress.superCleared
        var newCoins = currentProgress.coins + (earnedStars * 25) + 50

        if (!levelDef.isFinal && levelDef.numericLevel != null) {
            val wasUnlocked = currentProgress.highestCleared >= MazeConfig.PASSES_TO_UNLOCK_IMPOSSIBLE
            newHighest = maxOf(currentProgress.highestCleared, levelDef.numericLevel)
            justUnlockedImpossible = !wasUnlocked && (newHighest >= MazeConfig.PASSES_TO_UNLOCK_IMPOSSIBLE)
        } else if (levelDef.tier == "SUPER") {
            if (!currentProgress.superCleared) {
                newSuperCleared = true
                justClearedSuper = true
                newCoins += 1000
            }
        } else if (levelDef.tier != null && levelDef.tier != "DAILY") {
            val tierNum = levelDef.tier.toIntOrNull() ?: 1
            val impSpc = game.elapsedSec.toDouble() / maxOf(1, levelDef.target)
            if (impSpc < 0.5 && tierNum == currentProgress.impossibleCleared + 1) {
                newImpCleared = tierNum
                justUnlockedNextTier = true
                nextTierDef = if (tierNum < MazeConfig.IMPOSSIBLE_TIER_SIZES.size) {
                    MazeConfig.impossibleDefForTier(tierNum + 1)
                } else {
                    MazeConfig.superDef()
                }
            }
        }

        viewModelScope.launch {
            // Save record
            repository.recordLevelCompletion(
                levelId = levelDef.id,
                stars = earnedStars,
                timeSec = game.elapsedSec,
                moves = game.moves
            )

            // Clear saved slot if matching
            val currentSlot = _uiState.value.saveSlot
            if (currentSlot != null && currentSlot.defId == levelDef.id) {
                repository.clearSaveSlot()
            }

            // Calculate new total stars
            val allRecords = repository.levelRecordsFlow
            // Update achievements
            if (levelDef.numericLevel != null) {
                repository.updateAchievementProgress("first_step", absolute = newHighest)
                repository.updateAchievementProgress("fifty_levels", absolute = newHighest)
            }
            if (earnedStars == 3) {
                repository.updateAchievementProgress("perfect_speed", increment = 1)
            }
            if (game.wallHits == 0) {
                repository.updateAchievementProgress("no_wall_hit", increment = 1)
            }
            if (levelDef.vision != null) {
                repository.updateAchievementProgress("fog_master", increment = 1)
            }
            if (levelDef.tier == "DAILY") {
                repository.updateAchievementProgress("daily_champion", increment = 1)
            }

            // Save new progress
            repository.saveProgress(
                currentProgress.copy(
                    highestCleared = newHighest,
                    impossibleCleared = newImpCleared,
                    superCleared = newSuperCleared,
                    coins = newCoins,
                    totalStars = currentProgress.totalStars + earnedStars
                )
            )
        }

        _uiState.update {
            it.copy(
                activeGame = it.activeGame.copy(
                    gameOver = true,
                    hasWon = true,
                    earnedStars = earnedStars,
                    secPerCell = spc,
                    justUnlockedImpossible = justUnlockedImpossible,
                    justUnlockedNextTier = justUnlockedNextTier,
                    nextTierDef = nextTierDef,
                    justClearedSuper = justClearedSuper
                )
            )
        }
    }

    private fun handleLoss(reason: String) {
        pauseTimer()
        soundManager.playLose()
        _uiState.update {
            it.copy(
                toastMessage = reason,
                activeGame = it.activeGame.copy(
                    gameOver = true,
                    hasWon = false
                )
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val current = _uiState.value.activeGame
                if (!current.gameOver && !current.isPaused) {
                    val newSec = current.elapsedSec + 1
                    val levelDef = current.levelDef
                    if (levelDef != null && !levelDef.isFinal && levelDef.target > 0) {
                        val spc = newSec.toDouble() / levelDef.target
                        if (spc > 2.0) {
                            handleLoss("⏱️ Quá thời gian (Tốc độ > 2.0s/ô) — Thua!")
                            break
                        }
                    }
                    _uiState.update {
                        it.copy(activeGame = it.activeGame.copy(elapsedSec = newSec))
                    }
                }
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun resumeTimer() {
        if (timerJob == null || timerJob?.isActive == false) {
            startTimer()
        }
    }

    fun requestExit() {
        val game = _uiState.value.activeGame
        if (!game.gameOver && game.moves > 0) {
            pauseTimer()
            _uiState.update { it.copy(activeGame = it.activeGame.copy(showExitDialog = true)) }
        } else {
            navigateTo(AppScreen.HOME)
        }
    }

    fun dismissExitDialog() {
        _uiState.update { it.copy(activeGame = it.activeGame.copy(showExitDialog = false)) }
        resumeTimer()
    }

    fun saveAndExit() {
        val game = _uiState.value.activeGame
        val maze = game.maze ?: return
        val def = game.levelDef ?: return

        val visitedArr = JSONArray(game.visitedCells.toList())
        val historyArr = JSONArray(game.pathHistory.map { "${it.x},${it.y}" })

        val slot = SaveSlotEntity(
            id = 1,
            defId = def.id,
            tier = def.tier,
            w = def.w,
            h = def.h,
            seedStr = maze.seedStr,
            playerX = game.player.x,
            playerY = game.player.y,
            moves = game.moves,
            elapsedSec = game.elapsedSec,
            visitedIndicesJson = visitedArr.toString(),
            pathHistoryJson = historyArr.toString()
        )

        viewModelScope.launch {
            repository.saveGameSlot(slot)
            _uiState.update {
                it.copy(
                    currentScreen = AppScreen.HOME,
                    toastMessage = "✓ Đã lưu ván chơi thành công!"
                )
            }
        }
    }

    fun discardAndExit() {
        val currentSlot = _uiState.value.saveSlot
        val defId = _uiState.value.activeGame.levelDef?.id
        viewModelScope.launch {
            if (currentSlot != null && currentSlot.defId == defId) {
                repository.clearSaveSlot()
            }
            _uiState.update { it.copy(currentScreen = AppScreen.HOME) }
        }
    }

    // Replay mode controls
    fun startReplay() {
        val history = _uiState.value.activeGame.pathHistory
        if (history.isEmpty()) return
        soundManager.playClick()
        pauseTimer()
        replayJob?.cancel()

        _uiState.update {
            it.copy(
                currentScreen = AppScreen.REPLAY,
                replay = ReplayState(currentIndex = 0, isPlaying = true, speed = 1f)
            )
        }
        playReplayLoop()
    }

    private fun playReplayLoop() {
        replayJob?.cancel()
        replayJob = viewModelScope.launch {
            while (isActive) {
                val replay = _uiState.value.replay
                val history = _uiState.value.activeGame.pathHistory
                if (!replay.isPlaying) {
                    delay(100)
                    continue
                }

                if (replay.currentIndex < history.size - 1) {
                    val nextIdx = replay.currentIndex + 1
                    soundManager.playStep()
                    _uiState.update { it.copy(replay = it.replay.copy(currentIndex = nextIdx)) }
                    val interval = (240 / replay.speed).toLong().coerceAtLeast(40)
                    delay(interval)
                } else {
                    _uiState.update { it.copy(replay = it.replay.copy(isPlaying = false)) }
                    soundManager.playStarChime()
                    break
                }
            }
        }
    }

    fun toggleReplayPlayPause() {
        val playing = !_uiState.value.replay.isPlaying
        _uiState.update { it.copy(replay = it.replay.copy(isPlaying = playing)) }
        if (playing) {
            val history = _uiState.value.activeGame.pathHistory
            if (_uiState.value.replay.currentIndex >= history.size - 1) {
                _uiState.update { it.copy(replay = it.replay.copy(currentIndex = 0)) }
            }
            playReplayLoop()
        } else {
            replayJob?.cancel()
        }
    }

    fun setReplaySpeed(multiplier: Float) {
        _uiState.update { it.copy(replay = it.replay.copy(speed = multiplier)) }
    }

    // Rewarded Ad triggers
    fun showRewardedAdPrompt(rewardType: String) {
        _uiState.update { it.copy(rewardAdPrompt = rewardType) }
    }

    fun dismissRewardedAd() {
        _uiState.update { it.copy(rewardAdPrompt = null) }
    }

    fun onRewardEarned(type: String) {
        soundManager.playStarChime()
        when (type) {
            "HINT" -> {
                _uiState.update {
                    it.copy(
                        rewardAdPrompt = null,
                        toastMessage = "Đã mở khóa gợi ý đường đi!",
                        activeGame = it.activeGame.copy(showHint = true)
                    )
                }
            }
            "TIME" -> {
                val current = _uiState.value.activeGame
                val reduced = maxOf(0, current.elapsedSec - 30)
                _uiState.update {
                    it.copy(
                        rewardAdPrompt = null,
                        toastMessage = "Đã nhận thêm 30 giây!",
                        activeGame = it.activeGame.copy(elapsedSec = reduced)
                    )
                }
            }
            "COINS" -> {
                val currentProgress = _uiState.value.progress
                val updated = currentProgress.copy(coins = currentProgress.coins + 50)
                viewModelScope.launch {
                    repository.saveProgress(updated)
                }
                _uiState.update {
                    it.copy(
                        rewardAdPrompt = null,
                        toastMessage = "Nhận thành công +50 Xu!"
                    )
                }
            }
        }
    }

    fun useHint() {
        val currentProgress = _uiState.value.progress
        if (currentProgress.hintCount > 0) {
            viewModelScope.launch {
                repository.saveProgress(currentProgress.copy(hintCount = currentProgress.hintCount - 1))
            }
            soundManager.playStarChime()
            _uiState.update {
                it.copy(
                    activeGame = it.activeGame.copy(showHint = true),
                    toastMessage = "Gợi ý đường đi đã hiển thị!"
                )
            }
        } else {
            showRewardedAdPrompt("HINT")
        }
    }

    fun skipCurrentLevel() {
        val currentProgress = _uiState.value.progress
        if (currentProgress.skipTokens > 0) {
            viewModelScope.launch {
                repository.saveProgress(currentProgress.copy(skipTokens = currentProgress.skipTokens - 1))
            }
            handleWin()
        } else {
            _uiState.update { it.copy(toastMessage = "Bạn chưa có thẻ Bỏ Qua Màn!") }
        }
    }

    // Shop actions
    fun buySkin(skinId: String) {
        val skin = ShopCatalog.getSkinById(skinId)
        val p = _uiState.value.progress
        if (p.coins >= skin.priceCoins) {
            val unlocked = p.unlockedSkins.split(",").toMutableSet()
            unlocked.add(skinId)
            val updated = p.copy(
                coins = p.coins - skin.priceCoins,
                unlockedSkins = unlocked.joinToString(","),
                currentSkinId = skinId
            )
            viewModelScope.launch {
                repository.saveProgress(updated)
                repository.updateAchievementProgress("skin_collector", absolute = unlocked.size)
            }
            soundManager.playStarChime()
            _uiState.update { it.copy(toastMessage = "Đã mua & trang bị skin ${skin.name}!") }
        } else {
            _uiState.update { it.copy(toastMessage = "Không đủ xu! Hãy xem quảng cáo hoặc hoàn thành thêm màn.") }
        }
    }

    fun equipSkin(skinId: String) {
        val p = _uiState.value.progress
        viewModelScope.launch {
            repository.saveProgress(p.copy(currentSkinId = skinId))
        }
        soundManager.playClick()
    }

    fun buyTheme(themeId: String) {
        val theme = ShopCatalog.getThemeById(themeId)
        val p = _uiState.value.progress
        if (p.coins >= theme.priceCoins) {
            val unlocked = p.unlockedThemes.split(",").toMutableSet()
            unlocked.add(themeId)
            val updated = p.copy(
                coins = p.coins - theme.priceCoins,
                unlockedThemes = unlocked.joinToString(","),
                currentMazeThemeId = themeId
            )
            viewModelScope.launch {
                repository.saveProgress(updated)
            }
            soundManager.playStarChime()
            _uiState.update { it.copy(toastMessage = "Đã mua & đổi giao diện ${theme.name}!") }
        } else {
            _uiState.update { it.copy(toastMessage = "Không đủ xu!") }
        }
    }

    fun equipTheme(themeId: String) {
        val p = _uiState.value.progress
        viewModelScope.launch {
            repository.saveProgress(p.copy(currentMazeThemeId = themeId))
        }
        soundManager.playClick()
    }

    fun buyVipPass() {
        val p = _uiState.value.progress
        val unlockedSkins = p.unlockedSkins.split(",").toMutableSet().apply { add("vip_crown") }
        val updated = p.copy(
            isVipNoAds = true,
            coins = p.coins + 500,
            unlockedSkins = unlockedSkins.joinToString(","),
            currentSkinId = "vip_crown"
        )
        viewModelScope.launch {
            repository.saveProgress(updated)
        }
        soundManager.playStarChime()
        _uiState.update { it.copy(toastMessage = "👑 Chúc mừng! Bạn đã là thành viên VIP!") }
    }

    fun claimAchievement(id: String) {
        viewModelScope.launch {
            val reward = repository.claimAchievement(id)
            if (reward > 0) {
                soundManager.playStarChime()
                _uiState.update { it.copy(toastMessage = "+$reward Xu từ thành tựu!") }
            }
        }
    }

    fun toggleSound() {
        val p = _uiState.value.progress
        val newVal = !p.soundEnabled
        soundManager.isSoundEnabled = newVal
        viewModelScope.launch { repository.saveProgress(p.copy(soundEnabled = newVal)) }
    }

    fun toggleMusic() {
        val p = _uiState.value.progress
        val newVal = !p.musicEnabled
        soundManager.isMusicEnabled = newVal
        viewModelScope.launch { repository.saveProgress(p.copy(musicEnabled = newVal)) }
    }

    fun toggleHaptics() {
        val p = _uiState.value.progress
        val newVal = !p.hapticEnabled
        hapticManager.isHapticEnabled = newVal
        viewModelScope.launch { repository.saveProgress(p.copy(hapticEnabled = newVal)) }
    }

    fun openCloudSync() {
        soundManager.playClick()
        _uiState.update { it.copy(showCloudSyncDialog = true) }
    }

    fun closeCloudSync() {
        _uiState.update { it.copy(showCloudSyncDialog = false) }
    }

    fun confirmCloudSync() {
        val now = System.currentTimeMillis()
        val p = _uiState.value.progress
        viewModelScope.launch {
            repository.saveProgress(p.copy(lastCloudSyncTime = now))
        }
        soundManager.playStarChime()
    }

    fun openResetModal() {
        soundManager.playClick()
        _uiState.update { it.copy(showResetModal = true) }
    }

    fun closeResetModal() {
        _uiState.update { it.copy(showResetModal = false) }
    }

    fun confirmResetAll() {
        viewModelScope.launch {
            repository.resetAllProgress()
            _uiState.update {
                it.copy(
                    showResetModal = false,
                    currentScreen = AppScreen.HOME,
                    toastMessage = "Đã xóa toàn bộ tiến trình!"
                )
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun selectLeaderboardLevel(lvl: Int) {
        _uiState.update { it.copy(selectedLeaderboardLevel = lvl) }
    }
}
