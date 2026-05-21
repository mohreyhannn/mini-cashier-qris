package com.example.minicashier.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    secondary = Color(0xFF0EA5E9),
    tertiary = Color(0xFF38BDF8),

    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),

    primaryContainer = Color(0xFFDBEAFE),
    secondaryContainer = Color(0xFFE0F2FE),
    tertiaryContainer = Color(0xFFE0F7FA),

    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,

    onPrimaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFF075985),
    onTertiaryContainer = Color(0xFF155E75),

    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B),

    error = Color(0xFFDC2626)
)

private val AppTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(
        fontFamily = FontFamily.SansSerif
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontFamily = FontFamily.SansSerif
    ),
    headlineSmall = Typography().headlineSmall.copy(
        fontFamily = FontFamily.SansSerif
    ),
    titleLarge = Typography().titleLarge.copy(
        fontFamily = FontFamily.SansSerif
    ),
    titleMedium = Typography().titleMedium.copy(
        fontFamily = FontFamily.SansSerif
    ),
    bodyMedium = Typography().bodyMedium.copy(
        fontFamily = FontFamily.SansSerif
    )
)

@Composable
fun MiniCashierTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content
    )
}