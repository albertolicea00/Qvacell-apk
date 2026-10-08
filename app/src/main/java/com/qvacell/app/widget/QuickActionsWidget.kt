package com.qvacell.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.qvacell.app.R
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.ui.resolveAndroidIcon

class QuickActionsWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, QuickActionsWidget::class.java)
            )
            for (id in ids) updateWidget(context, manager, id)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            WidgetPrefs.deleteConfig(context, widgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val codeId = WidgetPrefs.getCodeId(context, widgetId)
            val code = CatalogRepository(context).findCodeById(codeId)
            val views = RemoteViews(context.packageName, R.layout.widget_quick_actions)

            val settings = WidgetSettings
            val contentStyle = settings.getContentStyle(context)
            val bgColor = settings.getBackgroundColor(context)
            val iconColor = settings.getIconColor(context)
            val textColor = settings.getTextColor(context)
            val iconShape = settings.getIconShape(context)

            val showIcon = contentStyle in listOf("icon_only", "icon_text")
            val showText = contentStyle in listOf("icon_text", "text_only")

            // Background color via setColorFilter on the background ImageView
            views.setInt(R.id.tile_bg_view, "setColorFilter", bgColor)

            // Content visibility
            views.setViewVisibility(R.id.tile_icon_container, if (showIcon) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.tile_icon_space, if (showIcon && showText) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.tile_label, if (showText) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.tile_code, View.GONE)

            // Text content and colors
            views.setTextViewText(R.id.tile_label, code?.title?.value ?: "")
            views.setTextColor(R.id.tile_label, textColor)

            // Icon bitmap
            if (code != null && showIcon) {
                runCatching {
                    imageVectorToBitmap(resolveAndroidIcon(code.icon), sizePx = 80, tintArgb = iconColor)
                }.getOrNull()?.let { views.setImageViewBitmap(R.id.tile_icon, it) }
            }

            val intent = Intent(context, DialTrampolineActivity::class.java).apply {
                putExtra(DialTrampolineActivity.EXTRA_CODE_ID, codeId)
            }
            val pi = PendingIntent.getActivity(
                context, widgetId, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, pi)

            manager.updateAppWidget(widgetId, views)
        }
    }
}
