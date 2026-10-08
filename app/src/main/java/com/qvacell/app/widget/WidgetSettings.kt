package com.qvacell.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import androidx.core.content.edit

object WidgetSettings {

    private const val PREFS_NAME = "widget_global_settings"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Show SMS services
    fun getShowSmsServices(context: Context): Boolean =
        prefs(context).getBoolean("show_sms_services", false)

    fun setShowSmsServices(context: Context, value: Boolean) {
        prefs(context).edit { putBoolean("show_sms_services", value) }
    }

    // Content style: "icon_only" | "icon_code" | "icon_text" | "text_only" | "text_code"
    fun getContentStyle(context: Context): String =
        prefs(context).getString("content_style", "icon_only") ?: "icon_only"

    fun setContentStyle(context: Context, value: String) {
        prefs(context).edit { putString("content_style", value) }
    }

    // Background color (ARGB Int, includes alpha for transparency)
    fun getBackgroundColor(context: Context): Int =
        prefs(context).getInt("background_color", Color.parseColor("#FF0099CC"))

    fun setBackgroundColor(context: Context, value: Int) {
        prefs(context).edit { putInt("background_color", value) }
    }

    // Icon tint color
    fun getIconColor(context: Context): Int =
        prefs(context).getInt("icon_color", Color.WHITE)

    fun setIconColor(context: Context, value: Int) {
        prefs(context).edit { putInt("icon_color", value) }
    }

    // Text/label color
    fun getTextColor(context: Context): Int =
        prefs(context).getInt("text_color", Color.WHITE)

    fun setTextColor(context: Context, value: Int) {
        prefs(context).edit { putInt("text_color", value) }
    }

    // Icon shape: "circle" | "square" | "rounded_square"
    fun getIconShape(context: Context): String =
        prefs(context).getString("icon_shape", "rounded_square") ?: "rounded_square"

    fun setIconShape(context: Context, value: String) {
        prefs(context).edit { putString("icon_shape", value) }
    }

    // Icon shape background color (ARGB, default semi-transparent black)
    fun getIconShapeBgColor(context: Context): Int =
        prefs(context).getInt("icon_shape_bg_color", 0x33000000)

    fun setIconShapeBgColor(context: Context, value: Int) {
        prefs(context).edit { putInt("icon_shape_bg_color", value) }
    }

    // Refresh all existing widgets after a setting change
    fun refreshAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, QuickActionsWidget::class.java))
        for (id in ids) {
            QuickActionsWidget.updateWidget(context, manager, id)
        }
    }
}
