package com.neuroflow.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.neuroflow.app.worker.DailyPlanWorker
import com.neuroflow.app.worker.DeadlineEscalationWorker
import com.neuroflow.app.worker.FocusWidgetUpdateWorker
import com.neuroflow.app.worker.StreakCheckWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Reschedules all periodic WorkManager jobs after a device reboot.
 * Without this, periodic workers are lost when the device restarts.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val wm = WorkManager.getInstance(context)

        wm.enqueueUniquePeriodicWork(
            "daily_plan",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DailyPlanWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayUntilHour(7), TimeUnit.MILLISECONDS)
                .build()
        )
        wm.enqueueUniquePeriodicWork(
            "streak_check",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<StreakCheckWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayUntilHour(21), TimeUnit.MILLISECONDS)
                .build()
        )
        wm.enqueueUniquePeriodicWork(
            "deadline_escalation",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DeadlineEscalationWorker>(4, TimeUnit.HOURS).build()
        )
        wm.enqueueUniquePeriodicWork(
            "focus_widget_update",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<FocusWidgetUpdateWorker>(15, TimeUnit.MINUTES).build()
        )
    }

    private fun delayUntilHour(hour: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis - now.timeInMillis
    }
}
