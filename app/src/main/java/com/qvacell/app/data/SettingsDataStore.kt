package com.qvacell.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
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
    private val quickPurchaseNoConfirmKey = booleanPreferencesKey("quick_purchase_no_confirm_default")

    val themeMode = context.dataStore.data.map {
        ThemeMode.valueOf(it[themeModeKey] ?: ThemeMode.SYSTEM.name)
    }

    val defaultTab = context.dataStore.data.map { it[defaultTabKey] ?: "home" }

    // Hex string, e.g. "#0099CC" — defaults to the brand cyan from the logo/iOS's Color.brandCyan.
    val accentColor = context.dataStore.data.map { it[accentColorKey] ?: DEFAULT_ACCENT_COLOR }

    val debugDatabaseSearchEnabled = context.dataStore.data.map {
        (it[debugDbSearchKey] ?: "false").toBoolean()
    }

    // "Activar por Defecto..." — Compras' own "Acción sin Confirmación" toggle starts matching
    // this every time that screen opens, same as iOS's AppStorage("quickPurchaseNoConfirmDefault").
    val quickPurchaseNoConfirmDefault = context.dataStore.data.map { it[quickPurchaseNoConfirmKey] ?: false }

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

    suspend fun setQuickPurchaseNoConfirmDefault(enabled: Boolean) {
        context.dataStore.edit { it[quickPurchaseNoConfirmKey] = enabled }
    }

    companion object {
        const val DEFAULT_ACCENT_COLOR = "#0099CC"
    }
}
