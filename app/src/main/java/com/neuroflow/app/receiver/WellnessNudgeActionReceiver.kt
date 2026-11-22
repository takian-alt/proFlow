package com.neuroflow.app.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.domain.assistant.RestBreakSource
import com.neuroflow.app.worker.ProactiveWellnessNotificationService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles 1-tap actions directly from proactive wellness notifications without opening the app:
 * - Log hydration (updates live energy curve, hydration counter, and shared nudge cooldown)
 * - Log rest / recovery break (updates Burnout Guard, Flow energy curve, and shared cooldown)
 * - Snooze 45 minutes (sets snooze window and schedules a WorkManager follow-up check)
 */
@AndroidEntryPoint
class WellnessNudgeActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferencesDataStore: UserPreferencesDataStore

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getIntExtra(
            EXTRA_NOTIFICATION_ID,
            ProactiveWellnessNotificationService.NOTIFICATION_ID_WELLNESS
        )
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(notificationId)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_LOG_HYDRATION -> {
                        preferencesDataStore.recordHydrationDrink()
                    }
                    ACTION_LOG_REST_BREAK -> {
                        val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 10)
                            .coerceIn(5, 60)
                        preferencesDataStore.recordRestBreak(
                            durationMinutes = durationMinutes,
                            source = RestBreakSource.MANUAL_CONFIRM
                        )
                    }
                    ACTION_SNOOZE_45M -> {
                        preferencesDataStore.snoozeEnergyBoosterNudge(
                            snoozeMinutes = ProactiveWellnessNotificationService.DEFAULT_SNOOZE_FOLLOWUP_MINUTES.toInt()
                        )
                        ProactiveWellnessNotificationService.scheduleFollowUpWork(
                            context = context,
                            delayMinutes = ProactiveWellnessNotificationService.DEFAULT_SNOOZE_FOLLOWUP_MINUTES
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_LOG_HYDRATION = "com.neuroflow.app.action.WELLNESS_LOG_HYDRATION"
        const val ACTION_LOG_REST_BREAK = "com.neuroflow.app.action.WELLNESS_LOG_REST_BREAK"
        const val ACTION_SNOOZE_45M = "com.neuroflow.app.action.WELLNESS_SNOOZE_45M"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
        const val EXTRA_DURATION_MINUTES = "durationMinutes"
    }
}
