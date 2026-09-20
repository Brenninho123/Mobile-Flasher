package com.mobileflasher.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileflasher.app.settings.ThemeMode

val BoltAmber = Color(0xFFFFB300)
val BoltInk = Color(0xFF12151C)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6C9BFF),
    onPrimary = Color(0xFF05122E),
    primaryContainer = Color(0xFF1F3266),
    onPrimaryContainer = Color(0xFFD9E4FF),
    secondary = BoltAmber,
    onSecondary = Color(0xFF231700),
    secondaryContainer = Color(0xFF4A3600),
    onSecondaryContainer = Color(0xFFFFE2A3),
    tertiary = Color(0xFF6FE3C4),
    background = Color(0xFF0D0F14),
    onBackground = Color(0xFFE6E8EF),
    surface = Color(0xFF14171F),
    onSurface = Color(0xFFE6E8EF),
    surfaceVariant = Color(0xFF232837),
    onSurfaceVariant = Color(0xFF9AA2B5),
    surfaceContainerLowest = Color(0xFF0A0C10),
    surfaceContainerLow = Color(0xFF11141B),
    surfaceContainer = Color(0xFF181C25),
    surfaceContainerHigh = Color(0xFF202532),
    surfaceContainerHighest = Color(0xFF2A3040),
    outline = Color(0xFF4A5165),
    outlineVariant = Color(0xFF2C3242),
    error = Color(0xFFFF6B6B)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F63E0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF0B1E52),
    secondary = Color(0xFFC77F00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE7B3),
    onSecondaryContainer = Color(0xFF3A2500),
    tertiary = Color(0xFF00806A),
    background = Color(0xFFF2F4F9),
    onBackground = Color(0xFF14171F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14171F),
    surfaceVariant = Color(0xFFE4E8F1),
    onSurfaceVariant = Color(0xFF5B6478),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F8FC),
    surfaceContainer = Color(0xFFEEF0F6),
    surfaceContainerHigh = Color(0xFFE6E9F1),
    surfaceContainerHighest = Color(0xFFDDE1EC),
    outline = Color(0xFF9199AD),
    outlineVariant = Color(0xFFD3D8E4),
    error = Color(0xFFD93636)
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MobileFlasherTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (dark) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
