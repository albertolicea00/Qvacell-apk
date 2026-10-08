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
    private val showNetworkStatusKey = booleanPreferencesKey("show_network_status")
    private val ussdCaptureEnabledKey = booleanPreferencesKey("ussd_capture_enabled")
    private val smsCaptureEnabledKey = booleanPreferencesKey("sms_capture_enabled")
    private val hasRunSmsBackfillKey = booleanPreferencesKey("has_run_sms_backfill")
    private val hasCompletedOnboardingKey = booleanPreferencesKey("has_completed_onboarding")
    private val dashboardModeKey = stringPreferencesKey("dashboard_mode")
    private val quickActionsStyleKey = stringPreferencesKey("quick_actions_style")

    val themeMode = context.dataStore.data.map {
        ThemeMode.valueOf(it[themeModeKey] ?: ThemeMode.SYSTEM.name)
    }

    val defaultTab = context.dataStore.data.map {
        val tab = it[defaultTabKey] ?: "home"
        if (tab == "settings") "options" else tab
    }

    // Hex string, e.g. "#0099CC" — defaults to the brand cyan from the logo/iOS's Color.brandCyan.
    val accentColor = context.dataStore.data.map { it[accentColorKey] ?: DEFAULT_ACCENT_COLOR }

    val debugDatabaseSearchEnabled = context.dataStore.data.map {
        (it[debugDbSearchKey] ?: "false").toBoolean()
    }

    // "Activar por Defecto..." — Compras' own "Acción sin Confirmación" toggle starts matching
    // this every time that screen opens, same as iOS's AppStorage("quickPurchaseNoConfirmDefault").
    val quickPurchaseNoConfirmDefault = context.dataStore.data.map { it[quickPurchaseNoConfirmKey] ?: false }

    // "Aviso de señal celular" — shows a banner on Home/Compras/Ayuda/SMS warning about weak
    // cellular signal before a USSD dial, same AppStorage flag as iOS's showNetworkStatus.
    val showNetworkStatus = context.dataStore.data.map { it[showNetworkStatusKey] ?: false }

    // "Consulta automática de saldo (experimental)" — silent TelephonyManager.sendUssdRequest
    // capture instead of the plain system-dialer flow. Off by default: ETECSA's gateway
    // reliability through this API is unverified. Full flavor only (see BuildConfig.DASHBOARD_CAPTURE_ENABLED).
    val ussdCaptureEnabled = context.dataStore.data.map { it[ussdCaptureEnabledKey] ?: false }

    // "Detección automática por SMS" — reads new + historical ETECSA SMS and the call log to
    // estimate usage. Off by default; enabling it requests RECEIVE_SMS/READ_SMS/READ_CALL_LOG.
    val smsCaptureEnabled = context.dataStore.data.map { it[smsCaptureEnabledKey] ?: false }

    // One-way — the historical SMS scan only ever runs once per install, right after READ_SMS is
    // newly granted.
    val hasRunSmsBackfill = context.dataStore.data.map { it[hasRunSmsBackfillKey] ?: false }

    val hasCompletedOnboarding = context.dataStore.data.map { it[hasCompletedOnboardingKey] ?: false }

    // "dynamic" or "manual" — set during onboarding, empty string means not yet chosen.
    val dashboardMode = context.dataStore.data.map { it[dashboardModeKey] ?: "" }

    // "filled" (default) or "outline" — tile card style for "manual" dashboard mode.
    val quickActionsStyle = context.dataStore.data.map { it[quickActionsStyleKey] ?: "filled" }

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

    suspend fun setShowNetworkStatus(enabled: Boolean) {
        context.dataStore.edit { it[showNetworkStatusKey] = enabled }
    }

    suspend fun setUssdCaptureEnabled(enabled: Boolean) {
        context.dataStore.edit { it[ussdCaptureEnabledKey] = enabled }
    }

    suspend fun setSmsCaptureEnabled(enabled: Boolean) {
        context.dataStore.edit { it[smsCaptureEnabledKey] = enabled }
    }

    suspend fun setHasRunSmsBackfill(enabled: Boolean) {
        context.dataStore.edit { it[hasRunSmsBackfillKey] = enabled }
    }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { it[hasCompletedOnboardingKey] = completed }
    }

    suspend fun setDashboardMode(mode: String) {
        context.dataStore.edit { it[dashboardModeKey] = mode }
    }

    suspend fun setQuickActionsStyle(style: String) {
        context.dataStore.edit { it[quickActionsStyleKey] = style }
    }

    companion object {
        const val DEFAULT_ACCENT_COLOR = "#0099CC"
    }
}
