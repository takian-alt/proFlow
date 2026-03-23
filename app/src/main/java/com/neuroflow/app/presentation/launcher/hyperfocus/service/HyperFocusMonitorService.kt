package com.neuroflow.app.presentation.launcher.hyperfocus.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.neuroflow.app.MainActivity
import com.neuroflow.app.R
import com.neuroflow.app.presentation.launcher.hyperfocus.data.HyperFocusDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that keeps Hyper Focus active and monitors accessibility service health.
 * Runs only during active Hyper Focus sessions to ensure app blocking remains active.
 */
@AndroidEntryPoint
class HyperFocusMonitorService : Service() {

    @Inject
    lateinit var hyperFocusDataStore: HyperFocusDataStore

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var monitorJob: Job? = null

    companion object {
        private const val CHANNEL_ID = "hyperfocus_monitor"
        private const val CHANNEL_NAME = "Hyper Focus Active"
        private const val NOTIFICATION_ID = 2002
        private const val CHECK_INTERVAL_MS = 10_000L // Check every 10 seconds
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (true) {
                val prefs = hyperFocusDataStore.flow.first()

                // If Hyper Focus is no longer active, stop this service
                if (!prefs.isActive) {
                    stopSelf()
                    break
                }

                // Check if accessibility service is still enabled
                val accessibilityEnabled = isAccessibilityServiceEnabled()
                if (!accessibilityEnabled) {
                    // Update notification to warn user
                    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        buildWarningNotification()
                    )
                } else {
                    // Update notification with normal status
                    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        buildNotification()
                    )
                }

                delay(CHECK_INTERVAL_MS)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when Hyper Focus is actively blocking apps"
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Hyper Focus Active")
        .setContentText("Apps are being blocked. Complete tasks to unlock.")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    private fun buildWarningNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("⚠️ Hyper Focus - Service Disabled")
        .setContentText("Accessibility service is off. Tap to re-enable.")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(
            "${packageName}/.presentation.launcher.hyperfocus.service.AppBlockingService",
            ignoreCase = true
        ) || enabled.contains("AppBlockingService", ignoreCase = true)
    }
}
