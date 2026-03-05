package com.communicationcoach.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CoachBlue = Color(0xFF2196F3)
private val CoachBlueDark = Color(0xFF1976D2)
private val CoachOrange = Color(0xFFFF5722)
private val CoachSurface = Color(0xFFF8FAFE)
private val CoachBackground = Color(0xFFF3F7FF)

private val LightColors = lightColorScheme(
    primary = CoachBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E8FF),
    onPrimaryContainer = Color(0xFF001D35),
    secondary = CoachBlueDark,
    onSecondary = Color.White,
    tertiary = CoachOrange,
    onTertiary = Color.White,
    background = CoachBackground,
    onBackground = Color(0xFF1A1C1E),
    surface = CoachSurface,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41484D),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

@Composable
fun CoachTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
