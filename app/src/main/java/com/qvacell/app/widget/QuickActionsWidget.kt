package com.qvacell.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.qvacell.app.R
import com.qvacell.app.data.CatalogRepository

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

            val intent = Intent(context, DialTrampolineActivity::class.java).apply {
                putExtra(DialTrampolineActivity.EXTRA_CODE_ID, codeId)
            }
            val pi = PendingIntent.getActivity(
                context, widgetId, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.tile_label, pi)

            manager.updateAppWidget(widgetId, views)
        }
    }
}
