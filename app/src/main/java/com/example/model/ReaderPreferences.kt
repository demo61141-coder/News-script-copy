package com.example.model

enum class ReaderSurfaceTheme {
    PAPER,
    LIGHT,
    SEPIA,
    DARK
}

enum class ReaderFontStyle {
    SERIF,
    SANS
}

enum class AppLanguage {
    BENGALI,
    ENGLISH
}

data class ReaderPreferences(
    val fontSizeSp: Int = 18,
    val lineHeightMultiplier: Float = 1.65f,
    val surfaceTheme: ReaderSurfaceTheme = ReaderSurfaceTheme.PAPER,
    val fontStyle: ReaderFontStyle = ReaderFontStyle.SERIF,
    val showImages: Boolean = true,
    val language: AppLanguage = AppLanguage.BENGALI
)
