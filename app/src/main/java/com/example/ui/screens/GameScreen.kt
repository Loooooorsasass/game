package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.engine.LevelDef
import com.example.core.engine.MazeConfig
import com.example.core.engine.Point
import com.example.data.shop.ShopCatalog
import com.example.ui.components.DPad
import com.example.ui.components.MazeCanvas
import com.example.ui.theme.MazeAccent
import com.example.ui.theme.MazeDanger
import com.example.ui.theme.MazeStar
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val game = uiState.activeGame
    val maze = game.maze ?: return
    val levelDef = game.levelDef ?: return

    val theme = ShopCatalog.getThemeById(uiState.progress.currentMazeThemeId)
    val skin = ShopCatalog.getSkinById(uiState.progress.currentSkinId)

    // Android Physical Back Handler
    BackHandler {
        viewModel.requestExit()
    }

    val currentDistance = game.distCache?.get(game.player.y * maze.w + game.player.x) ?: 0
    val paceLabel = if (!levelDef.isFinal && levelDef.target > 0) {
        val spc = game.elapsedSec.toDouble() / levelDef.target
        when {
            spc <= 0.7 -> "⭐⭐⭐"
            spc <= 1.0 -> "⭐⭐"
            spc <= 2.0 -> "⭐"
            else -> "💀 Quá chậm"
        }
    } else ""

    Box(modifier = modifier.fillMaxSize().background(theme.bgColor)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR: Back button & Level Title & Powerups
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.requestExit() },
                    modifier = Modifier.testTag("game_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = theme.wallColor
                    )
                }

                val titleText = when {
                    levelDef.tier == "DAILY" -> "📅 Thử Thách Ngày — ${maze.w}×${maze.h}"
                    levelDef.isFinal -> "👑 ${if (levelDef.tier == "SUPER") "SIÊU CẤP" else "IMPOSSIBLE"} — ${maze.w}×${maze.h}"
                    else -> "Level ${levelDef.id} — ${maze.w}×${maze.h}"
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = titleText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = theme.wallColor
                    )
                    Text(
                        text = "Mục tiêu: đúng ${levelDef.target} bước",
                        fontSize = 12.sp,
                        color = theme.accentColor,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row {
                    IconButton(onClick = { viewModel.useHint() }) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Gợi ý",
                            tint = if (game.showHint) Color(0xFFFBBF24) else theme.wallColor.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = { viewModel.toggleSound() }) {
                        Icon(
                            imageVector = if (uiState.progress.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Âm thanh",
                            tint = theme.wallColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // HUD STATS BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.panelColor)
                    .border(1.dp, theme.wallColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HudItem(
                    label = "Bước / Mục tiêu",
                    value = "$currentDistance / ${levelDef.target}",
                    textColor = theme.accentColor
                )

                if (levelDef.moveCap != null) {
                    HudItem(
                        label = "Lượt di chuyển",
                        value = "${game.moves} / ${levelDef.moveCap}",
                        textColor = if (game.moves > levelDef.moveCap * 0.8) MazeDanger else theme.wallColor
                    )
                } else {
                    HudItem(
                        label = "Lượt đi",
                        value = "${game.moves}",
                        textColor = theme.wallColor
                    )
                }

                HudItem(
                    label = "Thời gian",
                    value = "${game.elapsedSec}s",
                    textColor = theme.wallColor
                )

                if (paceLabel.isNotEmpty()) {
                    HudItem(
                        label = "Dự kiến",
                        value = paceLabel,
                        textColor = MazeStar
                    )
                }

                HudItem(
                    label = "Tọa độ",
                    value = MazeConfig.encodeCoord(game.player),
                    textColor = theme.wallColor.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Thanh chuyển đổi chế độ điều khiển: Tự động (Vuốt / Giữ liên tục) vs Từng bước
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Điều khiển:",
                    fontSize = 12.sp,
                    color = theme.wallColor.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )

                Surface(
                    onClick = { viewModel.toggleControlMode() },
                    shape = RoundedCornerShape(16.dp),
                    color = if (game.controlMode == com.example.ui.components.GridControlMode.AUTO)
                        theme.accentColor.copy(alpha = 0.18f)
                    else
                        theme.wallColor.copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        if (game.controlMode == com.example.ui.components.GridControlMode.AUTO)
                            theme.accentColor
                        else
                            theme.wallColor.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("toggle_control_mode_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = game.controlMode.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (game.controlMode == com.example.ui.components.GridControlMode.AUTO)
                                theme.accentColor
                            else
                                theme.wallColor
                        )
                        Text(
                            text = "(${game.controlMode.description})",
                            fontSize = 10.sp,
                            color = theme.wallColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // MAZE CANVAS BOARD (SWIPE ENABLED)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentAlignment = Alignment.Center
            ) {
                MazeCanvas(
                    maze = maze,
                    player = game.player,
                    visitedCells = game.visitedCells,
                    theme = theme,
                    skin = skin,
                    vision = levelDef.vision,
                    hintPath = if (game.showHint) maze.spine else null,
                    controlMode = game.controlMode,
                    onMove = { dx, dy -> viewModel.tryMove(dx, dy) },
                    modifier = Modifier.testTag("game_maze_canvas")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // D-PAD & ASSIST CONTROLS (Tối ưu 1 tay)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Quick Actions (Hint, Time Boost)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistButton(
                        icon = "💡",
                        label = "Gợi ý (${uiState.progress.hintCount})",
                        onClick = { viewModel.useHint() }
                    )
                    AssistButton(
                        icon = "⏱️",
                        label = "+30s (Xem Ad)",
                        onClick = { viewModel.showRewardedAdPrompt("TIME") }
                    )
                }

                // Center: D-Pad
                DPad(
                    onMove = { dx, dy -> viewModel.tryMove(dx, dy) },
                    modifier = Modifier.testTag("game_dpad")
                )

                // Right Quick Actions (Skip, Free Coins)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistButton(
                        icon = "⏩",
                        label = "Bỏ qua (${uiState.progress.skipTokens})",
                        onClick = { viewModel.skipCurrentLevel() }
                    )
                    AssistButton(
                        icon = "🪙",
                        label = "+50 Xu (Xem Ad)",
                        onClick = { viewModel.showRewardedAdPrompt("COINS") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        // EXIT CONFIRMATION MODAL (Android back flow)
        if (game.showExitDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissExitDialog() },
                title = { Text(text = "Bạn muốn thoát ván chơi?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(text = "Đã đi ${game.moves} bước trong ${game.elapsedSec} giây. Bạn có thể lưu lại để chơi tiếp sau.")
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.saveAndExit() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("save_and_exit_btn")
                    ) {
                        Text("Lưu & Thoát")
                    }
                },
                dismissButton = {
                    Row {
                        OutlinedButton(
                            onClick = { viewModel.discardAndExit() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MazeDanger)
                        ) {
                            Text("Thoát Không Lưu")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(onClick = { viewModel.dismissExitDialog() }) {
                            Text("Hủy")
                        }
                    }
                }
            )
        }

        // WIN OVERLAY MODAL
        if (game.gameOver && game.hasWon) {
            GameWinModal(
                levelDef = levelDef,
                moves = game.moves,
                elapsedSec = game.elapsedSec,
                earnedStars = game.earnedStars,
                justUnlockedImpossible = game.justUnlockedImpossible,
                justUnlockedNextTier = game.justUnlockedNextTier,
                nextTierDef = game.nextTierDef,
                justClearedSuper = game.justClearedSuper,
                onNextLevel = {
                    if (levelDef.isFinal) {
                        val nextDef = game.nextTierDef ?: levelDef
                        viewModel.startLevel(nextDef)
                    } else if (levelDef.numericLevel != null) {
                        viewModel.startLevel(MazeConfig.generateLevelDef(levelDef.numericLevel + 1))
                    }
                },
                onReplay = { viewModel.startReplay() },
                onGoHome = { viewModel.navigateTo(AppScreen.HOME) }
            )
        }

        // LOSS OVERLAY MODAL
        if (game.gameOver && !game.hasWon) {
            GameLossModal(
                reason = uiState.toastMessage ?: "Không thể hoàn thành mục tiêu",
                onRetry = { viewModel.startLevel(levelDef) },
                onAddExtraTimeAd = { viewModel.showRewardedAdPrompt("TIME") },
                onGoHome = { viewModel.navigateTo(AppScreen.HOME) }
            )
        }
    }
}

@Composable
private fun HudItem(
    label: String,
    value: String,
    textColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun AssistButton(
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.width(104.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GameWinModal(
    levelDef: LevelDef,
    moves: Int,
    elapsedSec: Int,
    earnedStars: Int,
    justUnlockedImpossible: Boolean,
    justUnlockedNextTier: Boolean,
    nextTierDef: LevelDef?,
    justClearedSuper: Boolean,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit,
    onGoHome: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                .testTag("win_modal")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉 HOÀN THÀNH!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Stars display
                if (!levelDef.isFinal) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (i in 1..3) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (i <= earnedStars) MazeStar else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = "$moves lượt di chuyển · ${elapsedSec}s hoàn thành",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (justUnlockedImpossible) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "👑 Chúc mừng! Đã mở khóa IMPOSSIBLE!",
                        color = MazeDanger,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (justClearedSuper) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "👑 Huyền thoại! Bạn đã chinh phục SIÊU CẤP 1000×1000!",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }

                if (justUnlockedNextTier) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "🔥 Tốc độ siêu việt! Đã mở tầng tiếp theo!",
                        color = MazeAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNextLevel,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("win_next_level_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = if (levelDef.isFinal) "Tiếp Tục Tầng Tiếp Theo" else "Màn Tiếp Theo (${levelDef.w + 1}×${levelDef.h + 1})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onReplay,
                        modifier = Modifier.fillMaxWidth().height(44.dp).testTag("win_replay_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Xem Lại Đường Đi (Replay)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    TextButton(
                        onClick = onGoHome,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Về Trang Chủ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun GameLossModal(
    reason: String,
    onRetry: () -> Unit,
    onAddExtraTimeAd: () -> Unit,
    onGoHome: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(2.dp, MazeDanger, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "💔 CHƯA HOÀN THÀNH",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MazeDanger
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = reason,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Thử Lại (Mê Cung Mới)", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onAddExtraTimeAd,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("📺 Xem Ad Nhận +30s Chơi Tiếp")
                    }

                    TextButton(
                        onClick = onGoHome,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Về Trang Chủ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
