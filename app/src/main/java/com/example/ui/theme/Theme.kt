package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SportsColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0E3A4A),
    onPrimaryContainer = ElectricCyanGlow,
    secondary = LaserOrange,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF431407),
    onSecondaryContainer = Color(0xFFFFEDD5),
    tertiary = SpeedAmber,
    onTertiary = Color.Black,
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
