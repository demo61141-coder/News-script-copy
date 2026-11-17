package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7CB7CE),
    onPrimary = Color(0xFF0A2430),
    primaryContainer = Color(0xFF1F4B5E),
    onPrimaryContainer = Color(0xFFD5E6EE),
    secondary = Color(0xFFF07B56),
    onSecondary = Color(0xFF331107),
    secondaryContainer = Color(0xFF5A2516),
    onSecondaryContainer = Color(0xFFFFE5DD),
    background = EditorialNightBg,
    onBackground = EditorialNightInk,
    surface = EditorialNightSurface,
    onSurface = EditorialNightInk,
    surfaceVariant = EditorialNightCard,
    onSurfaceVariant = EditorialNightMuted,
    outline = EditorialNightOutline,
    outlineVariant = EditorialNightOutline
)

private val LightColorScheme = lightColorScheme(
    primary = EditorialTeal,
    onPrimary = Color.White,
    primaryContainer = EditorialTealContainer,
    onPrimaryContainer = EditorialTeal,
    secondary = EditorialTerracotta,
    onSecondary = Color.White,
    secondaryContainer = EditorialTerracottaContainer,
    onSecondaryContainer = Color(0xFF6B200B),
    background = EditorialPaper,
    onBackground = EditorialInk,
    surface = EditorialPaperCard,
    onSurface = EditorialInk,
    surfaceVariant = EditorialPaperSurface,
    onSurfaceVariant = EditorialSlate,
    outline = EditorialOutline,
    outlineVariant = EditorialOutline
)

@Composable
fun CleanReadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CleanReadTypography,
        content = content
    )
}
