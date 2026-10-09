package com.qvacell.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            WidgetPrefs.deleteConfig(context, widgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            try { updateWidgetInternal(context, manager, widgetId) }
            catch (e: Exception) { android.util.Log.e("QvaWidget", "update failed", e) }
        }

        private fun updateWidgetInternal(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val codeId = WidgetPrefs.getCodeId(context, widgetId)
            val code = CatalogRepository(context).findCodeById(codeId)
            val views = RemoteViews(context.packageName, R.layout.widget_quick_actions)

            val settings = WidgetSettings
            val contentStyle = settings.getContentStyle(context)
            val bgColor = settings.getBackgroundColor(context)
            val iconColor = settings.getIconColor(context)
            val textColor = settings.getTextColor(context)
            val iconShape = settings.getIconShape(context)
            val alignment = settings.getAlignment(context)

            val showIcon = contentStyle in listOf("icon_only", "icon_text")
            val showText = contentStyle in listOf("icon_text", "text_only")

            // Alignment handling: LinearLayout supports setGravity(int)
            val gravity = when (alignment) {
                "top" -> android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
                "bottom" -> android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                "center" -> android.view.Gravity.CENTER
                else -> android.view.Gravity.FILL
            }
            views.setInt(R.id.widget_root, "setGravity", gravity)

            val options = manager.getAppWidgetOptions(widgetId)
            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
            val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

            val effectiveW = if (minW > 0) minW else maxW
            val effectiveH = if (minH > 0) minH else maxH
            val density = context.resources.displayMetrics.density

            if (alignment == "fill") {
                views.setViewLayoutWidth(R.id.widget_alignment_container, -1f, android.util.TypedValue.COMPLEX_UNIT_PX) // MATCH_PARENT (-1)
                views.setViewLayoutHeight(R.id.widget_alignment_container, -1f, android.util.TypedValue.COMPLEX_UNIT_PX)
            } else {
                if (effectiveW > 0 && effectiveH > 0) {
                    val sideDp = minOf(effectiveW, effectiveH).toFloat()
                    val sidePx = sideDp * density
                    views.setViewLayoutWidth(R.id.widget_alignment_container, sidePx, android.util.TypedValue.COMPLEX_UNIT_PX)
                    views.setViewLayoutHeight(R.id.widget_alignment_container, sidePx, android.util.TypedValue.COMPLEX_UNIT_PX)
                } else {
                    views.setViewLayoutWidth(R.id.widget_alignment_container, -2f, android.util.TypedValue.COMPLEX_UNIT_PX) // WRAP_CONTENT (-2)
                    views.setViewLayoutHeight(R.id.widget_alignment_container, -2f, android.util.TypedValue.COMPLEX_UNIT_PX)
                }
            }

            // Background: try shaped bitmap, fall back to plain color filter
            val bgBitmap = runCatching { shapeBackgroundBitmap(200, bgColor, iconShape) }.getOrNull()
            if (bgBitmap != null) {
                views.setImageViewBitmap(R.id.tile_bg_view, bgBitmap)
            } else {
                views.setInt(R.id.tile_bg_view, "setColorFilter", bgColor)
            }

            // Content visibility
            views.setViewVisibility(R.id.tile_icon_shape_bg, View.GONE)
            views.setViewVisibility(R.id.tile_icon_container, if (showIcon) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.tile_icon_space, if (showIcon && showText) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.tile_label, if (showText) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.tile_code, View.GONE)

            // Text content and colors
            views.setTextViewText(R.id.tile_label, code?.title?.value ?: "")
            views.setTextColor(R.id.tile_label, textColor)

            // Icon bitmap
            if (code != null && showIcon) {
                val iconSizePx = if (showText) 100 else 180
                runCatching {
                    imageVectorToBitmap(resolveAndroidIcon(code.icon), sizePx = iconSizePx, tintArgb = iconColor)
                }.getOrNull()?.let { views.setImageViewBitmap(R.id.tile_icon, it) }
            }

            // Text size
            val textSizeSp = if (showIcon) 11f else 16f
            views.setTextViewTextSize(R.id.tile_label, android.util.TypedValue.COMPLEX_UNIT_SP, textSizeSp)

            val intent = Intent(context, DialTrampolineActivity::class.java).apply {
                putExtra(DialTrampolineActivity.EXTRA_CODE_ID, codeId)
                putExtra(DialTrampolineActivity.EXTRA_WIDGET_ID, widgetId)
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

