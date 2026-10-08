package com.qvacell.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
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

            views.setTextViewText(R.id.tile_label, code?.title?.value ?: "")
            if (code != null) {
                val iconBitmap = imageVectorToBitmap(
                    resolveAndroidIcon(code.icon),
                    sizePx = 96,
                    tintArgb = Color.WHITE
                )
                views.setImageViewBitmap(R.id.tile_icon, iconBitmap)
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
