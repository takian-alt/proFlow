package com.neuroflow.app.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.neuroflow.app.MainActivity
import com.neuroflow.app.R
import com.neuroflow.app.worker.FocusWidgetUpdateWorker

class NextActionWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val request = OneTimeWorkRequestBuilder<FocusWidgetUpdateWorker>().build()
        WorkManager.getInstance(context).enqueue(request)

        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_next_action)
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                widgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_next_action_container, pendingIntent)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
