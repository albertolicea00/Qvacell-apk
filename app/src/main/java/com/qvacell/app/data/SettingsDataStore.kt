package com.qvacell.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "qvacell_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class SettingsDataStore(private val context: Context) {

    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val defaultTabKey = stringPreferencesKey("default_tab")
    private val accentColorKey = stringPreferencesKey("accent_color")
    private val debugDbSearchKey = stringPreferencesKey("debug_db_search_enabled")

    val themeMode = context.dataStore.data.map {
        ThemeMode.valueOf(it[themeModeKey] ?: ThemeMode.SYSTEM.name)
    }

    val defaultTab = context.dataStore.data.map { it[defaultTabKey] ?: "home" }

    val accentColor = context.dataStore.data.map { it[accentColorKey] ?: "blue" }

    val debugDatabaseSearchEnabled = context.dataStore.data.map {
        (it[debugDbSearchKey] ?: "false").toBoolean()
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeModeKey] = mode.name }
    }

    suspend fun setDefaultTab(tab: String) {
        context.dataStore.edit { it[defaultTabKey] = tab }
    }

    suspend fun setAccentColor(color: String) {
        context.dataStore.edit { it[accentColorKey] = color }
    }

    suspend fun setDebugDatabaseSearchEnabled(enabled: Boolean) {
        context.dataStore.edit { it[debugDbSearchKey] = enabled.toString() }
    }
}
