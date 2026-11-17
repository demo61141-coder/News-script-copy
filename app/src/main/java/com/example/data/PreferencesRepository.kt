package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import com.example.model.ReaderFontStyle
import com.example.model.ReaderPreferences
import com.example.model.ReaderSurfaceTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.readerPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "reader_preferences"
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val FONT_SIZE_SP = intPreferencesKey("font_size_sp")
        val LINE_HEIGHT = floatPreferencesKey("line_height")
        val SURFACE_THEME = stringPreferencesKey("surface_theme")
        val FONT_STYLE = stringPreferencesKey("font_style")
        val SHOW_IMAGES = booleanPreferencesKey("show_images")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val preferences: Flow<ReaderPreferences> = context.readerPrefsDataStore.data.map { prefs ->
        ReaderPreferences(
            fontSizeSp = prefs[Keys.FONT_SIZE_SP] ?: 18,
            lineHeightMultiplier = prefs[Keys.LINE_HEIGHT] ?: 1.65f,
            surfaceTheme = prefs[Keys.SURFACE_THEME]?.let { name ->
                ReaderSurfaceTheme.entries.firstOrNull { it.name == name }
            } ?: ReaderSurfaceTheme.PAPER,
            fontStyle = prefs[Keys.FONT_STYLE]?.let { name ->
                ReaderFontStyle.entries.firstOrNull { it.name == name }
            } ?: ReaderFontStyle.SERIF,
            showImages = prefs[Keys.SHOW_IMAGES] ?: true,
            language = prefs[Keys.LANGUAGE]?.let { name ->
                AppLanguage.entries.firstOrNull { it.name == name }
            } ?: AppLanguage.BENGALI
        )
    }

    suspend fun updateFontSize(fontSizeSp: Int) {
        context.readerPrefsDataStore.edit { it[Keys.FONT_SIZE_SP] = fontSizeSp.coerceIn(14, 26) }
    }

    suspend fun updateLineHeight(multiplier: Float) {
        context.readerPrefsDataStore.edit { it[Keys.LINE_HEIGHT] = multiplier.coerceIn(1.35f, 2.0f) }
    }

    suspend fun updateSurfaceTheme(theme: ReaderSurfaceTheme) {
        context.readerPrefsDataStore.edit { it[Keys.SURFACE_THEME] = theme.name }
    }

    suspend fun updateFontStyle(style: ReaderFontStyle) {
        context.readerPrefsDataStore.edit { it[Keys.FONT_STYLE] = style.name }
    }

    suspend fun updateShowImages(show: Boolean) {
        context.readerPrefsDataStore.edit { it[Keys.SHOW_IMAGES] = show }
    }

    suspend fun updateLanguage(language: AppLanguage) {
        context.readerPrefsDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }
}
