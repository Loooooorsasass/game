package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.engine.LevelDef
import com.example.core.engine.MazeConfig
import com.example.ui.theme.MazeAccent
import com.example.ui.theme.MazeDanger
import com.example.ui.theme.MazeStar
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.GameViewModel

@Composable
fun HomeScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val progress = uiState.progress
    val records = uiState.levelRecords
    val saveSlot = uiState.saveSlot

    var showHelpDialog by remember { mutableStateOf(false) }
    var displayedLevelCount by remember { mutableIntStateOf(24) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var resetCheckState by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // TOP BAR: Brand Title, Stars, Coins, Settings & Cloud
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🧩 Exact-Step Maze",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Mê Cung Từng Bước Chính Xác",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Total Stars badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MazeStar,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${progress.totalStars}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Coins badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🪙", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${progress.coins}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Cloud sync icon button
                IconButton(
                    onClick = { viewModel.openCloudSync() },
                    modifier = Modifier.size(36.dp).testTag("cloud_sync_icon_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Cloud Sync",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Help button
                IconButton(
                    onClick = { showHelpDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Help",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // QUICK NAVIGATION BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickNavChip(
                label = "Hôm Nay",
                emoji = "📅",
                badge = "Hot",
                onClick = { viewModel.navigateTo(AppScreen.DAILY_CHALLENGE) },
                modifier = Modifier.weight(1f).testTag("nav_daily")
            )
            QuickNavChip(
                label = "Xếp Hạng",
                emoji = "🏆",
                onClick = { viewModel.navigateTo(AppScreen.LEADERBOARD) },
                modifier = Modifier.weight(1f).testTag("nav_leaderboard")
            )
            QuickNavChip(
                label = "Cửa Hàng",
                emoji = "🛒",
                onClick = { viewModel.navigateTo(AppScreen.SHOP) },
                modifier = Modifier.weight(1f).testTag("nav_shop")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // RESUME GAME BANNER (if saveSlot exists)
        if (saveSlot != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    .testTag("resume_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val title = if (saveSlot.defId.startsWith("IMPOSSIBLE")) {
                            "IMPOSSIBLE (Tầng ${saveSlot.tier ?: "1"})"
                        } else {
                            "Level ${saveSlot.defId}"
                        }
                        Text(
                            text = "▶ Ván chơi đang lưu: $title",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${saveSlot.w}×${saveSlot.h} · Đã đi ${saveSlot.moves} bước · ${saveSlot.elapsedSec}s",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.resumeFromSave() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("resume_continue_btn")
                        ) {
                            Text("Tiếp tục", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.discardAndExit() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MazeDanger)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Xoá", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // DAILY CHALLENGE TEASER CARD
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.navigateTo(AppScreen.DAILY_CHALLENGE) }
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🔥", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Thử Thách Mê Cung Hôm Nay",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Mê cung cố định 14×14 toàn cầu · Nhận 200 Xu",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { viewModel.startDailyChallenge() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Đua Tốc Độ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PROGRESS TOWARD IMPOSSIBLE
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tiến trình mở khóa IMPOSSIBLE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val clearedCount = minOf(progress.highestCleared, MazeConfig.PASSES_TO_UNLOCK_IMPOSSIBLE)
                Text(
                    text = "$clearedCount / ${MazeConfig.PASSES_TO_UNLOCK_IMPOSSIBLE} Level",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val progPercent = minOf(progress.highestCleared, MazeConfig.PASSES_TO_UNLOCK_IMPOSSIBLE) / MazeConfig.PASSES_TO_UNLOCK_IMPOSSIBLE.toFloat()
            LinearProgressIndicator(
                progress = { progPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // HORIZONTAL SCROLLING LEVEL MAP (LEVEL 1 .. N)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Danh Sách Màn Chơi (5×5 → 299×299)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "← Vuốt ngang →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val levelScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(levelScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val maxToShow = minOf(MazeConfig.MAX_LEVEL, maxOf(progress.highestCleared + 1, displayedLevelCount))
            for (levelNum in 1..maxToShow) {
                val def = MazeConfig.generateLevelDef(levelNum)
                val isCleared = levelNum <= progress.highestCleared
                val reqStars = MazeConfig.starReqFor(levelNum)
                val isUnlocked = (levelNum <= progress.highestCleared + 1) && (reqStars == 0 || progress.totalStars >= reqStars)
                val starsEarned = records[levelNum.toString()]?.stars ?: 0
                val isSaved = saveSlot?.defId == levelNum.toString()

                LevelButton(
                    levelNumber = levelNum,
                    size = def.w,
                    isUnlocked = isUnlocked,
                    isCleared = isCleared,
                    starsEarned = starsEarned,
                    reqStars = reqStars,
                    currentStars = progress.totalStars,
                    isSaved = isSaved,
                    onClick = {
                        if (isSaved) viewModel.resumeFromSave()
                        else viewModel.startLevel(def)
                    }
                )
            }
        }

        if (displayedLevelCount < MazeConfig.MAX_LEVEL) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { displayedLevelCount = minOf(MazeConfig.MAX_LEVEL, displayedLevelCount + 20) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Hiện thêm màn chơi ▾", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // IMPOSSIBLE & SIÊU CẤP CARD
        val isImpossibleUnlocked = progress.highestCleared >= MazeConfig.PASSES_TO_UNLOCK_IMPOSSIBLE
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    color = if (isImpossibleUnlocked) MazeDanger else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "👑", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CHẾ ĐỘ IMPOSSIBLE",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (isImpossibleUnlocked) MazeDanger else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (!isImpossibleUnlocked) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${progress.highestCleared}/50 Màn", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isImpossibleUnlocked) {
                        "3 tầng cố định: 300×300 (900 bước) → 400×400 (1600 bước) → 500×500 (2500 bước). Đạt < 0.5s/bước để mở tầng tiếp theo. Vượt cả 3 mở 👑 SIÊU CẤP 1000×1000 (10.000 bước)."
                    } else {
                        "Vượt qua 50 màn chơi thường để mở khóa chế độ thử thách cực hạn với tầm nhìn 10×10 và kích thước mê cung khổng lồ."
                    },
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Impossible tiers row
                val impScroll = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(impScroll),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (tier in 1..MazeConfig.IMPOSSIBLE_TIER_SIZES.size) {
                        val def = MazeConfig.impossibleDefForTier(tier)
                        val tierUnlocked = isImpossibleUnlocked && (tier <= progress.impossibleCleared + 1)
                        val tierCleared = tier <= progress.impossibleCleared
                        val isSaved = saveSlot?.defId == def.id

                        ImpossibleTierButton(
                            title = "${def.w}×${def.h}",
                            subtitle = "${def.target} bước",
                            isUnlocked = tierUnlocked,
                            isCleared = tierCleared,
                            isSaved = isSaved,
                            onClick = {
                                if (isSaved) viewModel.resumeFromSave()
                                else viewModel.startLevel(def)
                            }
                        )
                    }

                    // Super tier button
                    val superDef = MazeConfig.superDef()
                    val superUnlocked = isImpossibleUnlocked && progress.impossibleCleared >= MazeConfig.IMPOSSIBLE_TIER_SIZES.size
                    val superCleared = progress.superCleared
                    val isSuperSaved = saveSlot?.defId == superDef.id

                    ImpossibleTierButton(
                        title = "SIÊU CẤP",
                        subtitle = "1000×1000 · 10k",
                        isUnlocked = superUnlocked,
                        isCleared = superCleared,
                        isSaved = isSuperSaved,
                        isSuper = true,
                        onClick = {
                            if (isSuperSaved) viewModel.resumeFromSave()
                            else viewModel.startLevel(superDef)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // AUDIO & HAPTIC CONTROLS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.toggleSound() }) {
                Icon(
                    imageVector = if (progress.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = "Sound Toggle",
                    tint = if (progress.soundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(onClick = { viewModel.toggleMusic() }) {
                Icon(
                    imageVector = if (progress.musicEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                    contentDescription = "Music Toggle",
                    tint = if (progress.musicEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(onClick = { viewModel.toggleHaptics() }) {
                Icon(
                    imageVector = Icons.Default.Vibration,
                    contentDescription = "Haptics Toggle",
                    tint = if (progress.hapticEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Reset progress link
        TextButton(
            onClick = { showResetConfirmDialog = true },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Xóa tiến trình (Chơi lại từ đầu)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // HELP MODAL
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text(
                    text = "📖 Hướng Dẫn Chơi",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "• Mỗi level tăng +1 kích thước: 5×5 → 6×6 → ... → 299×299. Vượt qua 50 màn mở ngay chế độ IMPOSSIBLE.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Đi đúng số bước mục tiêu để tới đích — không thừa, không thiếu.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Tiện lợi 1 tay: Vuốt trực tiếp bất kỳ đâu trên bảng mê cung hoặc dùng nút D-pad.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Xếp hạng sao theo tốc độ:\n  - ≤ 0.7s/ô → ⭐⭐⭐\n  - 0.7s – 1.0s/ô → ⭐⭐\n  - 1.0s – 2.0s/ô → ⭐\n  - > 2.0s/ô → Thua (Quá giờ)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Ván chơi tự động hỗ trợ Lưu & Tiếp tục khi thoát giữa chừng.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showHelpDialog = false }) {
                    Text("Đã hiểu")
                }
            }
        )
    }

    // RESET PROGRESS MODAL
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text(
                    text = "⚠️ Xóa Toàn Bộ Tiến Trình?",
                    fontWeight = FontWeight.Bold,
                    color = MazeDanger
                )
            },
            text = {
                Column {
                    Text(
                        text = "Thao tác này sẽ xoá vĩnh viễn: level đã vượt, số sao đã đạt, tiến trình IMPOSSIBLE và ván đang lưu. Không thể hoàn tác.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { resetCheckState = !resetCheckState }
                    ) {
                        Checkbox(
                            checked = resetCheckState,
                            onCheckedChange = { resetCheckState = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tôi chắc chắn muốn xoá hết",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmResetAll()
                        showResetConfirmDialog = false
                        resetCheckState = false
                    },
                    enabled = resetCheckState,
                    colors = ButtonDefaults.buttonColors(containerColor = MazeDanger)
                ) {
                    Text("Xóa Vĩnh Viễn")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Huỷ")
                }
            }
        )
    }
}

@Composable
private fun QuickNavChip(
    label: String,
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 6.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = emoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MazeDanger)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(text = badge, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LevelButton(
    levelNumber: Int,
    size: Int,
    isUnlocked: Boolean,
    isCleared: Boolean,
    starsEarned: Int,
    reqStars: Int,
    currentStars: Int,
    isSaved: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isSaved -> MaterialTheme.colorScheme.primary
        isCleared -> MazeAccent
        isUnlocked -> MaterialTheme.colorScheme.outlineVariant
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = Modifier
            .size(width = 86.dp, height = 98.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isUnlocked) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(if (isCleared || isSaved) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = isUnlocked, onClick = onClick)
            .padding(6.dp)
            .testTag("level_btn_$levelNumber"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$levelNumber",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Text(
                text = "${size}×${size}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (!isUnlocked) {
                if (reqStars > 0) {
                    Text(
                        text = "$currentStars/$reqStars ⭐",
                        fontSize = 9.sp,
                        color = MazeDanger,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    for (i in 1..3) {
                        Text(
                            text = "★",
                            fontSize = 12.sp,
                            color = if (i <= starsEarned) MazeStar else MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }

        if (isSaved) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(text = "Lưu", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun ImpossibleTierButton(
    title: String,
    subtitle: String,
    isUnlocked: Boolean,
    isCleared: Boolean,
    isSaved: Boolean,
    isSuper: Boolean = false,
    onClick: () -> Unit
) {
    val borderColor = when {
        isSaved -> MaterialTheme.colorScheme.primary
        isCleared -> MazeAccent
        isUnlocked -> if (isSuper) Color(0xFFFFD700) else MazeDanger
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        modifier = Modifier
            .size(width = 110.dp, height = 96.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = isUnlocked, onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = if (isSuper) 13.sp else 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isUnlocked) (if (isSuper) Color(0xFFFFD700) else MazeDanger) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isCleared) {
                Text(text = "★ ĐÃ VƯỢT", fontSize = 10.sp, color = MazeAccent, fontWeight = FontWeight.Bold)
            } else if (!isUnlocked) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (isSaved) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(text = "Lưu", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
