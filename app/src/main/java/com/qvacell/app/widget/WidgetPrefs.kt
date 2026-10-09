package com.qvacell.app.widget

import android.content.Context
import androidx.core.content.edit

object WidgetPrefs {

    private const val PREFS_NAME = "widget_prefs"
    private const val DEFAULT_CODE_ID = "main-balance"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveCodeId(context: Context, widgetId: Int, codeId: String) {
        prefs(context).edit { putString("${widgetId}_code", codeId) }
    }

    fun getCodeId(context: Context, widgetId: Int): String =
        prefs(context).getString("${widgetId}_code", null) ?: DEFAULT_CODE_ID

    fun saveSimSlot(context: Context, widgetId: Int, slot: Int) {
        prefs(context).edit { putInt("${widgetId}_sim_slot", slot) }
    }

    // -1 = use global default from SettingsDataStore
    fun getSimSlot(context: Context, widgetId: Int): Int =
        prefs(context).getInt("${widgetId}_sim_slot", -1)

    fun deleteConfig(context: Context, widgetId: Int) {
        prefs(context).edit {
            remove("${widgetId}_code")
            remove("${widgetId}_sim_slot")
        }
    }
}
