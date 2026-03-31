package com.neuroflow.app.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.presentation.launcher.hyperfocus.data.HyperFocusDataStore
import com.neuroflow.app.worker.FocusWidgetUpdateWorker
import com.neuroflow.app.worker.scheduleNotificationWorkers
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Reschedules all periodic WorkManager jobs after a device reboot.
 * Without this, periodic workers are lost when the device restarts.
 * Also runs a HyperFocus watchdog to alert the user if the blocking service
 * is not running while a session is active.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var hyperFocusDataStore: HyperFocusDataStore

    @Inject
    lateinit var userPreferencesDataStore: UserPreferencesDataStore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val wm = WorkManager.getInstance(context)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = userPreferencesDataStore.preferencesFlow.first()
                scheduleNotificationWorkers(context, prefs)
            } finally {
                pendingResult.finish()
            }
        }

        wm.enqueueUniquePeriodicWork(
            "focus_widget_update",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<FocusWidgetUpdateWorker>(15, TimeUnit.MINUTES).build()
        )

        // HyperFocus watchdog: alert user if blocking service is compromised
        val watchdogPendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val hfPrefs = hyperFocusDataStore.current()
                if (hfPrefs.isActive) {
                    val serviceEnabled = isAccessibilityServiceEnabled(context)
                    val heartbeatStale = (System.currentTimeMillis() - hfPrefs.lastServiceHeartbeat) > 60_000L
                    if (!serviceEnabled || heartbeatStale) {
                        showHyperFocusWatchdogNotification(context)
                    }
                }
            } finally {
                watchdogPendingResult.finish()
            }
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return com.neuroflow.app.presentation.launcher.hyperfocus.util.AccessibilityUtil
            .isAppBlockingServiceEnabled(context)
    }

    private fun showHyperFocusWatchdogNotification(context: Context) {
        val channelId = "hyperfocus_watchdog"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Hyper Focus Watchdog",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Hyper Focus: Attention Required")
            .setContentText("Your focus session may be compromised. Tap to re-enable.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

        notificationManager.notify(2002, notification)
    }
}
