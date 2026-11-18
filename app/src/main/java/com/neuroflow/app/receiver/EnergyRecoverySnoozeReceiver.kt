package com.neuroflow.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.domain.service.EnergyRecoveryThresholdAdvisor
import com.neuroflow.app.worker.EnergyRecoveryNotificationWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles the "Snooze 1h" action from the predicted energy recovery break notification.
 * Dismisses the active notification, sets remaining cooldown to 1 hour, and schedules
 * a follow-up check in 1 hour.
 */
@AndroidEntryPoint
class EnergyRecoverySnoozeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferencesDataStore: UserPreferencesDataStore

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(
            "notificationId",
            EnergyRecoveryNotificationWorker.NOTIFICATION_ID_RECOVERY
        )
        NotificationManagerCompat.from(context).cancel(notificationId)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val oneHourSnoozeAnchor = (now - (EnergyRecoveryThresholdAdvisor.ALERT_COOLDOWN_MILLIS - 3_600_000L))
                    .coerceAtLeast(1L)
                preferencesDataStore.recordRecoveryAlertSent(oneHourSnoozeAnchor)
                EnergyRecoveryNotificationWorker.enqueueSnoozedCheck(context, delayHours = 1L)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
