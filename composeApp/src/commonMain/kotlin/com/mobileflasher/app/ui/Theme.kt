package com.mobileflasher.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF42A5F5),
    secondary = Color(0xFFFFA726),
    background = Color(0xFF1E1E1E),
    surface = Color(0xFF2A2A2A),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E88E5),
    secondary = Color(0xFFFB8C00),
    background = Color(0xFFF5F5F5),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun MobileFlasherTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}
