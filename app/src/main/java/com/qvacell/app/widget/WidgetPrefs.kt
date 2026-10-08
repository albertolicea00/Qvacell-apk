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

    fun deleteConfig(context: Context, widgetId: Int) {
        prefs(context).edit { remove("${widgetId}_code") }
    }
}
