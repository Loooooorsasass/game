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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.shop.MazeTheme
import com.example.data.shop.PlayerSkin
import com.example.data.shop.ShopCatalog
import com.example.ui.theme.MazeAccent
import com.example.ui.theme.MazeStar
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.GameViewModel

@Composable
fun ShopScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val progress = uiState.progress
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Skins, 1: Themes, 2: VIP & Coins

    val unlockedSkins = progress.unlockedSkins.split(",").toSet()
    val unlockedThemes = progress.unlockedThemes.split(",").toSet()

    BackHandler {
        viewModel.navigateTo(AppScreen.HOME)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // TOP BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateTo(AppScreen.HOME) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "🛒 Cửa Hàng & VIP",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Coin balance badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🪙", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${progress.coins}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // TAB ROW
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Skins", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Chủ Đề", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Gói VIP", fontWeight = FontWeight.Bold) })
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedTab) {
            0 -> {
                // SKINS LIST
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ShopCatalog.SKINS) { skin ->
                        val isUnlocked = unlockedSkins.contains(skin.id) || (skin.isVipOnly && progress.isVipNoAds)
                        val isEquipped = progress.currentSkinId == skin.id

                        SkinItemCard(
                            skin = skin,
                            isUnlocked = isUnlocked,
                            isEquipped = isEquipped,
                            onEquip = { viewModel.equipSkin(skin.id) },
                            onBuy = { viewModel.buySkin(skin.id) }
                        )
                    }
                }
            }
            1 -> {
                // THEMES LIST
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ShopCatalog.THEMES) { theme ->
                        val isUnlocked = unlockedThemes.contains(theme.id)
                        val isEquipped = progress.currentMazeThemeId == theme.id

                        ThemeItemCard(
                            theme = theme,
                            isUnlocked = isUnlocked,
                            isEquipped = isEquipped,
                            onEquip = { viewModel.equipTheme(theme.id) },
                            onBuy = { viewModel.buyTheme(theme.id) }
                        )
                    }
                }
            }
            2 -> {
                // VIP & MONETIZATION
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        // VIP Pass Banner
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    2.dp,
                                    Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFF59E0B))),
                                    RoundedCornerShape(20.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "👑 GÓI THÀNH VIÊN VIP",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp,
                                            color = Color(0xFFFFD700)
                                        )
                                        Text(
                                            text = "Trải nghiệm không giới hạn & Không quảng cáo",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("✓ Tắt toàn bộ quảng cáo vĩnh viễn", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("✓ Mở khóa độc quyền Skin Vương Miện Hoàng Gia", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("✓ Tặng ngay +500 Xu vào tài khoản", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("✓ Nhân đôi số xu nhận được sau mỗi màn chơi", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                if (progress.isVipNoAds) {
                                    Button(
                                        onClick = {},
                                        enabled = false,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("✓ ĐÃ KÍCH HOẠT GÓI VIP", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.buyVipPass() },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("buy_vip_btn")
                                    ) {
                                        Text("Kích Hoạt VIP Ngay (Miễn phí trải nghiệm)", fontWeight = FontWeight.ExtraBold, color = Color.Black)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Free coins via Rewarded Ad
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "📺 Xem Quảng Cáo Nhận Xu", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "Nhận ngay +50 Xu để mở khóa skins và giao diện", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { viewModel.showRewardedAdPrompt("COINS") },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.testTag("watch_ad_coins_btn")
                                ) {
                                    Text("+50 🪙", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkinItemCard(
    skin: PlayerSkin,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    onEquip: () -> Unit,
    onBuy: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isEquipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(16.dp)
            )
            .testTag("skin_item_${skin.id}")
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
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(skin.glowColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = skin.iconEmoji, fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = skin.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (skin.isVipOnly) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "👑 VIP", fontSize = 10.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = skin.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isEquipped) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MazeAccent)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Đang dùng", fontSize = 12.sp, color = MazeAccent, fontWeight = FontWeight.Bold)
                }
            } else if (isUnlocked) {
                OutlinedButton(
                    onClick = onEquip,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Trang bị", fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = onBuy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("${skin.priceCoins} 🪙", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ThemeItemCard(
    theme: MazeTheme,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    onEquip: () -> Unit,
    onBuy: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isEquipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(16.dp)
            )
            .testTag("theme_item_${theme.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Color palette preview circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(theme.bgColor)
                        .border(2.dp, theme.wallColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(theme.accentColor)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = theme.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (theme.priceCoins == 0) "Mặc định" else "${theme.priceCoins} Xu",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isEquipped) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MazeAccent)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Đang dùng", fontSize = 12.sp, color = MazeAccent, fontWeight = FontWeight.Bold)
                }
            } else if (isUnlocked) {
                OutlinedButton(
                    onClick = onEquip,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Sử dụng", fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = onBuy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("${theme.priceCoins} 🪙", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
