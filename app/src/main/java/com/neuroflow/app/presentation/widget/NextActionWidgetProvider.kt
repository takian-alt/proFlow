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
        // Trigger an immediate data-driven update via WorkManager without flashing empty layout XML
        FocusWidgetUpdateWorker.enqueueImmediateUpdate(context)
    }
}
