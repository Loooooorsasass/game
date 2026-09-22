package com.example.data.shop

import androidx.compose.ui.graphics.Color

data class PlayerSkin(
    val id: String,
    val name: String,
    val description: String,
    val priceCoins: Int,
    val iconEmoji: String,
    val primaryColor: Color,
    val glowColor: Color,
    val isVipOnly: Boolean = false
)

data class MazeTheme(
    val id: String,
    val name: String,
    val priceCoins: Int,
    val bgColor: Color,
    val panelColor: Color,
    val wallColor: Color,
    val pathVisitedColor: Color,
    val accentColor: Color,
    val goalColor: Color
)

object ShopCatalog {
    val SKINS = listOf(
        PlayerSkin(
            id = "classic_blue",
            name = "Neon Sphere",
            description = "Hình cầu năng lượng chuẩn",
            priceCoins = 0,
            iconEmoji = "🔵",
            primaryColor = Color(0xFF60A5FA),
            glowColor = Color(0xFF93C5FD)
        ),
        PlayerSkin(
            id = "cyber_glow",
            name = "Cyber Cube",
            description = "Khối lập phương công nghệ số tương lai",
            priceCoins = 300,
            iconEmoji = "🔷",
            primaryColor = Color(0xFF06B6D4),
            glowColor = Color(0xFF67E8F9)
        ),
        PlayerSkin(
            id = "golden_star",
            name = "Sao Hoàng Kim",
            description = "Ngôi sao rực rỡ tượng trưng cho tốc độ",
            priceCoins = 500,
            iconEmoji = "⭐",
            primaryColor = Color(0xFFF59E0B),
            glowColor = Color(0xFFFDE68A)
        ),
        PlayerSkin(
            id = "cute_cat",
            name = "Mèo May Mắn",
            description = "Nhanh nhẹn và linh hoạt khám phá ngóc ngách",
            priceCoins = 600,
            iconEmoji = "🐱",
            primaryColor = Color(0xFFEC4899),
            glowColor = Color(0xFFFBCFE8)
        ),
        PlayerSkin(
            id = "fire_flame",
            name = "Ngọn Lửa Bất Diệt",
            description = "Sức mạnh bùng cháy xuyên qua mê cung",
            priceCoins = 800,
            iconEmoji = "🔥",
            primaryColor = Color(0xFFEF4444),
            glowColor = Color(0xFFFCA5A5)
        ),
        PlayerSkin(
            id = "vip_crown",
            name = "Vương Miện Hoàng Gia",
            description = "Độc quyền dành riêng cho thành viên VIP",
            priceCoins = 0,
            iconEmoji = "👑",
            primaryColor = Color(0xFFFFD700),
            glowColor = Color(0xFFFFFBEB),
            isVipOnly = true
        )
    )

    val THEMES = listOf(
        MazeTheme(
            id = "midnight_cyber",
            name = "Đêm Cyberpunk",
            priceCoins = 0,
            bgColor = Color(0xFF0F1220),
            panelColor = Color(0xFF171B2E),
            wallColor = Color(0xFFE6E8F5),
            pathVisitedColor = Color(0x336EE7B7),
            accentColor = Color(0xFF6EE7B7),
            goalColor = Color(0xFFF97316)
        ),
        MazeTheme(
            id = "emerald_matrix",
            name = "Ma Trận Ngọc Bích",
            priceCoins = 350,
            bgColor = Color(0xFF061A14),
            panelColor = Color(0xFF0D2820),
            wallColor = Color(0xFF34D399),
            pathVisitedColor = Color(0x3310B981),
            accentColor = Color(0xFF10B981),
            goalColor = Color(0xFFFBBF24)
        ),
        MazeTheme(
            id = "sunset_neon",
            name = "Hoàng Hôn Synthwave",
            priceCoins = 450,
            bgColor = Color(0xFF1A102E),
            panelColor = Color(0xFF261842),
            wallColor = Color(0xFFF472B6),
            pathVisitedColor = Color(0x33C084FC),
            accentColor = Color(0xFFC084FC),
            goalColor = Color(0xFF38BDF8)
        ),
        MazeTheme(
            id = "obsidian_gold",
            name = "Hắc Thạch Hoàng Gia",
            priceCoins = 600,
            bgColor = Color(0xFF121214),
            panelColor = Color(0xFF1E1E24),
            wallColor = Color(0xFFF59E0B),
            pathVisitedColor = Color(0x33FBBF24),
            accentColor = Color(0xFFFBBF24),
            goalColor = Color(0xFFEF4444)
        )
    )

    fun getSkinById(id: String): PlayerSkin {
        return SKINS.find { it.id == id } ?: SKINS.first()
    }

    fun getThemeById(id: String): MazeTheme {
        return THEMES.find { it.id == id } ?: THEMES.first()
    }
}
