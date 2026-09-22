package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.engine.Point
import com.example.data.shop.ShopCatalog
import com.example.ui.components.MazeCanvas
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.GameViewModel

@Composable
fun ReplayScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val game = uiState.activeGame
    val replay = uiState.replay
    val maze = game.maze ?: return
    val levelDef = game.levelDef ?: return

    val history: List<Point> = game.pathHistory
    val currentStep: Point = if (history.isNotEmpty()) {
        history[replay.currentIndex.coerceIn(0, history.size - 1)]
    } else maze.start

    val trailSoFar: List<Point> = if (history.isNotEmpty()) {
        history.subList(0, (replay.currentIndex + 1).coerceAtMost(history.size))
    } else emptyList()

    val theme = ShopCatalog.getThemeById(uiState.progress.currentMazeThemeId)
    val skin = ShopCatalog.getSkinById(uiState.progress.currentSkinId)

    BackHandler {
        viewModel.navigateTo(AppScreen.HOME)
    }

    Box(modifier = modifier.fillMaxSize().background(theme.bgColor)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(AppScreen.HOME) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = theme.wallColor
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🎬 XEM LẠI ĐƯỜNG ĐI",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = theme.wallColor
                    )
                    Text(
                        text = "Bước ${replay.currentIndex + 1} / ${history.size}",
                        fontSize = 12.sp,
                        color = theme.accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = { viewModel.startReplay() }) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Xem lại từ đầu",
                        tint = theme.wallColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // BOARD CANVAS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentAlignment = Alignment.Center
            ) {
                MazeCanvas(
                    maze = maze,
                    player = currentStep,
                    visitedCells = trailSoFar.map { it.y * maze.w + it.x }.toSet(),
                    theme = theme,
                    skin = skin,
                    vision = null, // In replay, let player see full maze or vision
                    replayTrail = trailSoFar,
                    onMove = { _, _ -> },
                    modifier = Modifier.testTag("replay_canvas")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // REPLAY CONTROLS
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = theme.panelColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, theme.wallColor.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Playback progress slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${replay.currentIndex + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.wallColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = replay.currentIndex.toFloat(),
                            onValueChange = {
                                // Manual scrub
                            },
                            valueRange = 0f..(history.size - 1).coerceAtLeast(1).toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = theme.accentColor,
                                activeTrackColor = theme.accentColor
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${history.size}",
                            fontSize = 12.sp,
                            color = theme.wallColor.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speed buttons
                        listOf(1f, 2f, 4f).forEach { spd ->
                            Button(
                                onClick = { viewModel.setReplaySpeed(spd) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (replay.speed == spd) theme.accentColor else theme.panelColor
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, theme.wallColor.copy(alpha = 0.2f)),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(
                                    text = "${spd.toInt()}x",
                                    color = if (replay.speed == spd) Color.Black else theme.wallColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Play/Pause button
                        Button(
                            onClick = { viewModel.toggleReplayPlayPause() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                            modifier = Modifier.size(width = 110.dp, height = 44.dp).testTag("replay_play_pause_btn")
                        ) {
                            Icon(
                                imageVector = if (replay.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (replay.isPlaying) "Tạm dừng" else "Phát",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { viewModel.navigateTo(AppScreen.HOME) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text(text = "Về Trang Chủ", color = theme.wallColor)
            }
        }
    }
}
