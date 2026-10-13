package com.neuroflow.app.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.neuroflow.app.MainActivity
import com.neuroflow.app.R

class QuickCaptureWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_quick_capture)
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.procus.ACTION_OPEN_NEW_TASK"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                widgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_quick_capture_button, pendingIntent)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
