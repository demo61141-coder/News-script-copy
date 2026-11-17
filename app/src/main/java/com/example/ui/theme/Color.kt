package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.model.ReaderSurfaceTheme

// Editorial Broadsheet Palette
val EditorialPaper = Color(0xFFFAF7F2)
val EditorialPaperSurface = Color(0xFFF3EFE6)
val EditorialPaperCard = Color(0xFFFFFFFF)
val EditorialInk = Color(0xFF171A1D)
val EditorialSlate = Color(0xFF5A6065)
val EditorialOutline = Color(0xFFE3DDD1)

val EditorialTeal = Color(0xFF153E50)
val EditorialTealContainer = Color(0xFFD5E6EE)
val EditorialTerracotta = Color(0xFFD4522E)
val EditorialTerracottaContainer = Color(0xFFFFE5DD)
val EditorialGold = Color(0xFFB57F2A)

val EditorialNightBg = Color(0xFF101316)
val EditorialNightSurface = Color(0xFF181C20)
val EditorialNightCard = Color(0xFF20252B)
val EditorialNightInk = Color(0xFFECEAE4)
val EditorialNightMuted = Color(0xFF9BA3AB)
val EditorialNightOutline = Color(0xFF2D343C)

data class ReaderPalette(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val quoteBackground: Color,
    val divider: Color,
    val isDark: Boolean
)

fun ReaderSurfaceTheme.toPalette(): ReaderPalette = when (this) {
    ReaderSurfaceTheme.PAPER -> ReaderPalette(
        background = Color(0xFFFAF7F2),
        surface = Color(0xFFF1ECE2),
        textPrimary = Color(0xFF1A1917),
        textSecondary = Color(0xFF5D5952),
        accent = EditorialTerracotta,
        quoteBackground = Color(0xFFF1ECE1),
        divider = Color(0xFFE2DCD0),
        isDark = false
    )
    ReaderSurfaceTheme.LIGHT -> ReaderPalette(
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFF5F6F8),
        textPrimary = Color(0xFF121417),
        textSecondary = Color(0xFF5B616A),
        accent = EditorialTeal,
        quoteBackground = Color(0xFFF2F5F7),
        divider = Color(0xFFE6E8EC),
        isDark = false
    )
    ReaderSurfaceTheme.SEPIA -> ReaderPalette(
        background = Color(0xFFF4ECD8),
        surface = Color(0xFFEBE0C6),
        textPrimary = Color(0xFF3A2D1E),
        textSecondary = Color(0xFF6B5942),
        accent = Color(0xFFA94422),
        quoteBackground = Color(0xFFEAE0C8),
        divider = Color(0xFFD9CBAE),
        isDark = false
    )
    ReaderSurfaceTheme.DARK -> ReaderPalette(
        background = Color(0xFF111315),
        surface = Color(0xFF1A1E22),
        textPrimary = Color(0xFFE6E3DC),
        textSecondary = Color(0xFF9BA2AA),
        accent = Color(0xFFF07B56),
        quoteBackground = Color(0xFF1B2026),
        divider = Color(0xFF2A3037),
        isDark = true
    )
}
