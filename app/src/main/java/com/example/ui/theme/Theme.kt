package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette Tema: Merah, Hitam, Silver, Putih
private val SportsColorScheme = darkColorScheme(
    primary = FinishLineRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF450A0A),
    onPrimaryContainer = Color(0xFFFECACA),
    secondary = SilverMetallic,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF27272A),
    onSecondaryContainer = Color.White,
    tertiary = LaserOrange,
    onTertiary = Color.White,
    background = TrackDarkNavy,
    onBackground = TextPrimary,
    surface = StadiumSurface,
    onSurface = TextPrimary,
    surfaceVariant = StadiumSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = StadiumBorder,
    error = FinishLineRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SportsColorScheme,
        typography = Typography,
        content = content
    )
}
