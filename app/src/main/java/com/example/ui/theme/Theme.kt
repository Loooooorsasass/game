package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MazeAccentMint,
    onPrimary = Color(0xFF061E14),
    primaryContainer = Color(0xFF0E3D2A),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = MazePlayer,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFBFDBFE),
    tertiary = MazeStar,
    onTertiary = Color(0xFF451A03),
    background = MazeBgDark,
    onBackground = Color(0xFFE6E8F5),
    surface = MazePanelDark,
    onSurface = Color(0xFFE6E8F5),
    surfaceVariant = MazeLineDark,
    onSurfaceVariant = Color(0xFF9AA0C3),
    error = MazeDanger,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = MazeAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = MazePlayer,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E40AF),
    tertiary = MazeStar,
    onTertiary = Color.White,
    background = MazeBgLight,
    onBackground = MazeTextLight,
    surface = MazePanelLight,
    onSurface = MazeTextLight,
    surfaceVariant = MazeLineLight,
    onSurfaceVariant = MazeMutedLight,
    error = MazeDanger,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
