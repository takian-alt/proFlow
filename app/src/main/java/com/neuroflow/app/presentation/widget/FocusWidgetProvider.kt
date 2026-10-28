package com.neuroflow.app.presentation.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.neuroflow.app.MainActivity
import com.neuroflow.app.R
import com.neuroflow.app.worker.FocusWidgetUpdateWorker

class FocusWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Trigger an immediate data-driven update via WorkManager without flashing empty layout XML
        FocusWidgetUpdateWorker.enqueueImmediateUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Handle Start tap — launch MainActivity with ACTION_OPEN_FOCUS
        if (intent.action == "com.procus.ACTION_OPEN_FOCUS" || intent.action == "com.neuroflow.app.WIDGET_START") {
            val taskId = intent.getStringExtra("taskId")
                ?: intent.getStringExtra("task_id")
                ?: intent.getStringExtra("extra_task_id")
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.procus.ACTION_OPEN_FOCUS"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                taskId?.let { putExtra("taskId", it) }
            }
            context.startActivity(launchIntent)
        }
    }
}
