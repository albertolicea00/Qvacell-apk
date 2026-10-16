package com.qvacell.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
    private val contactCallModeKey = stringPreferencesKey("contact_call_mode")
    private val ussdCaptureEnabledKey = booleanPreferencesKey("ussd_capture_enabled")
    private val smsCaptureEnabledKey = booleanPreferencesKey("sms_capture_enabled")
    private val smsHistoryEnabledKey = booleanPreferencesKey("sms_history_enabled")
    private val hasRunSmsBackfillKey = booleanPreferencesKey("has_run_sms_backfill")
    private val hasCompletedOnboardingKey = booleanPreferencesKey("has_completed_onboarding")
    private val quickActionsStyleKey = stringPreferencesKey("quick_actions_style")
    private val cardTapModeKey = stringPreferencesKey("card_tap_mode")
    private val dashboardLoadingStyleKey = stringPreferencesKey("dashboard_loading_style")
    private val manualHiddenSectionsKey = stringSetPreferencesKey("manual_hidden_sections")
    private val selectedSimSlotKey = intPreferencesKey("selected_sim_slot")
    private val staleDaysThresholdKey = intPreferencesKey("stale_days_threshold")
    private val autoUpdateCheckKey = stringPreferencesKey("auto_update_check")
    private val lastSuccessfulUpdateCheckAtKey = longPreferencesKey("last_successful_update_check_at")

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

    // "dialer" (default) or "direct" for *99 and hidden-number calls from Contacts.
    val contactCallMode = context.dataStore.data.map { it[contactCallModeKey] ?: "dialer" }

    // "Consulta automática de saldo (experimental)" — silent TelephonyManager.sendUssdRequest
    // capture instead of the plain system-dialer flow. Off by default: ETECSA's gateway
    // reliability through this API is unverified. Full flavor only (see BuildConfig.DASHBOARD_CAPTURE_ENABLED).
    val ussdCaptureEnabled = context.dataStore.data.map { it[ussdCaptureEnabledKey] ?: false }

    // New ETECSA SMS capture and call-log estimation. Off by default.
    val smsCaptureEnabled = context.dataStore.data.map { it[smsCaptureEnabledKey] ?: false }

    // Separate consent for importing existing SMS. Previous users who completed the one-time
    // import keep the history switch on when this preference is introduced.
    val smsHistoryEnabled = context.dataStore.data.map {
        it[smsHistoryEnabledKey] ?: (it[hasRunSmsBackfillKey] ?: false)
    }

    // The historical SMS scan runs once per install after READ_SMS is granted.
    val hasRunSmsBackfill = context.dataStore.data.map { it[hasRunSmsBackfillKey] ?: false }

    val hasCompletedOnboarding = context.dataStore.data.map { it[hasCompletedOnboardingKey] ?: false }

    // "filled" (default), "outline", or "list" for manual-dashboard actions.
    val quickActionsStyle = context.dataStore.data.map { it[quickActionsStyleKey] ?: "filled" }

    // How dashboard cards trigger USSD queries: tap_confirm (default), tap, double_tap,
    // long_confirm, long, triple_tap.
    val cardTapMode = context.dataStore.data.map { it[cardTapModeKey] ?: "tap_confirm" }
    val dashboardLoadingStyle = context.dataStore.data.map { it[dashboardLoadingStyleKey] ?: "sweep" }

    // Empty by default, so all manual-dashboard sections are visible.
    // Optional hidden sections: "consultas", "transferir", "recargar", "adelanta".
    val manualHiddenSections = context.dataStore.data.map { it[manualHiddenSectionsKey] ?: emptySet() }

    // 0-based SIM slot index (-1 = system default). Only meaningful on multi-SIM devices.
    val selectedSimSlot = context.dataStore.data.map { it[selectedSimSlotKey] ?: -1 }

    val staleDaysThreshold = context.dataStore.data.map { it[staleDaysThresholdKey] ?: 14 }

    // "unlocked" (default), "playstore", or "none"
    val autoUpdateCheck = context.dataStore.data.map { it[autoUpdateCheckKey] ?: "unlocked" }
    val lastSuccessfulUpdateCheckAt = context.dataStore.data.map { it[lastSuccessfulUpdateCheckAtKey] ?: 0L }

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

    suspend fun setContactCallMode(mode: String) {
        context.dataStore.edit { it[contactCallModeKey] = mode }
    }

    suspend fun setUssdCaptureEnabled(enabled: Boolean) {
        context.dataStore.edit { it[ussdCaptureEnabledKey] = enabled }
    }

    suspend fun setSmsCaptureEnabled(enabled: Boolean) {
        context.dataStore.edit { it[smsCaptureEnabledKey] = enabled }
    }

    suspend fun setSmsHistoryEnabled(enabled: Boolean) {
        context.dataStore.edit { it[smsHistoryEnabledKey] = enabled }
    }

    suspend fun setHasRunSmsBackfill(enabled: Boolean) {
        context.dataStore.edit { it[hasRunSmsBackfillKey] = enabled }
    }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { it[hasCompletedOnboardingKey] = completed }
    }

    suspend fun setQuickActionsStyle(style: String) {
        context.dataStore.edit { it[quickActionsStyleKey] = style }
    }

    suspend fun setCardTapMode(mode: String) {
        context.dataStore.edit { it[cardTapModeKey] = mode }
    }

    suspend fun setDashboardLoadingStyle(style: String) {
        context.dataStore.edit { it[dashboardLoadingStyleKey] = style }
    }

    suspend fun setManualHiddenSections(sections: Set<String>) {
        context.dataStore.edit { it[manualHiddenSectionsKey] = sections }
    }

    suspend fun setSelectedSimSlot(slot: Int) {
        context.dataStore.edit { it[selectedSimSlotKey] = slot }
    }

    suspend fun setStaleDaysThreshold(days: Int) {
        context.dataStore.edit { it[staleDaysThresholdKey] = days }
    }

    suspend fun setAutoUpdateCheck(mode: String) {
        context.dataStore.edit {
            if (it[autoUpdateCheckKey] != mode) it.remove(lastSuccessfulUpdateCheckAtKey)
            it[autoUpdateCheckKey] = mode
        }
    }

    suspend fun setLastSuccessfulUpdateCheckAt(timeMillis: Long) {
        context.dataStore.edit { it[lastSuccessfulUpdateCheckAtKey] = timeMillis }
    }

    companion object {
        const val DEFAULT_ACCENT_COLOR = "#0099CC"
    }
}
