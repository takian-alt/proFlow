package com.neuroflow.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.neuroflow.app.worker.AutonomyNudgeWorker
import java.util.concurrent.TimeUnit

/**
 * Handles the "I'm not ready yet" snooze action from the autonomy nudge notification.
 * Re-enqueues [AutonomyNudgeWorker] with a 1-hour delay without requiring the app to open.
 */
class NudgeSnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("taskId") ?: return
        val notificationId = intent.getIntExtra("notificationId", Int.MIN_VALUE)
        if (notificationId != Int.MIN_VALUE) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }

        val uniqueWorkName = "autonomy_nudge_$taskId"
        val request = OneTimeWorkRequestBuilder<AutonomyNudgeWorker>()
            .setInitialDelay(1, TimeUnit.HOURS)
            .setInputData(workDataOf("taskId" to taskId))
            .addTag(uniqueWorkName)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.REPLACE,
            request
        )

        val confirmation = NotificationCompat.Builder(context, "autonomy_nudge")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Reminder snoozed")
            .setContentText("We'll remind you again in 1 hour.")
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(uniqueWorkName.hashCode(), confirmation)
    }
}
