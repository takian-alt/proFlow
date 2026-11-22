package com.neuroflow.app.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.neuroflow.app.MainActivity
import com.neuroflow.app.domain.assistant.ProactiveWellnessNotificationPayload
import com.neuroflow.app.domain.assistant.WellnessNotificationActionType
import com.neuroflow.app.receiver.WellnessNudgeActionReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkManager-backed notification service that schedules, triggers, dispatches, and cancels
 * proactive, energy-aware wellness nudges (hydration, focus breaks, recovery, and wind-down).
 */
@Singleton
class ProactiveWellnessNotificationService @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    fun ensureNotificationChannel() {
        createChannel(appContext)
    }

    fun schedulePeriodic(intervalMinutes: Long = DEFAULT_EVALUATION_INTERVAL_MINUTES) {
        schedulePeriodicWork(appContext, intervalMinutes)
    }

    fun cancel() {
        cancelAllWork(appContext)
    }

    fun scheduleFollowUp(delayMinutes: Long = DEFAULT_SNOOZE_FOLLOWUP_MINUTES) {
        scheduleFollowUpWork(appContext, delayMinutes)
    }

    fun triggerImmediateCheck(forcePreview: Boolean = false) {
        enqueueImmediateWork(appContext, forcePreview)
    }

    fun canPostNotifications(): Boolean = hasNotificationPermission(appContext)

    fun dispatchNotification(
        payload: ProactiveWellnessNotificationPayload,
        context: Context = appContext
    ): Boolean = dispatch(context, payload)

    fun cancelActiveNotification(context: Context = appContext) {
        cancelNotification(context)
    }

    companion object {
        const val CHANNEL_WELLNESS_NUDGES = "proactive_wellness_nudges"
        const val WORK_PERIODIC_WELLNESS_NUDGE = "proactive_wellness_nudges"
        const val WORK_FOLLOWUP_WELLNESS_NUDGE = "proactive_wellness_nudge_followup"
        const val WORK_IMMEDIATE_WELLNESS_NUDGE = "proactive_wellness_nudge_immediate"
        const val NOTIFICATION_ID_WELLNESS = 10_008
        const val INPUT_FORCE_PREVIEW = "force_preview"
        const val DEFAULT_EVALUATION_INTERVAL_MINUTES = 30L
        const val DEFAULT_SNOOZE_FOLLOWUP_MINUTES = 45L

        private const val REQUEST_CODE_OPEN_APP = 10_080
        private const val REQUEST_CODE_PRIMARY_ACTION = 10_081
        private const val REQUEST_CODE_SNOOZE_ACTION = 10_082

        fun buildChannel(): NotificationChannel {
            return NotificationChannel(
                CHANNEL_WELLNESS_NUDGES,
                "Hydration, Breaks & Recovery",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Proactive context-aware nudges for hydration, focus breaks, and energy recovery based on your live energy curve"
            }
        }

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return
                manager.createNotificationChannel(buildChannel())
            }
        }

        fun hasNotificationPermission(context: Context): Boolean {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        }

        fun schedulePeriodicWork(
            context: Context,
            intervalMinutes: Long = DEFAULT_EVALUATION_INTERVAL_MINUTES
        ) {
            val safeInterval = intervalMinutes.coerceAtLeast(15L)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
            val request = PeriodicWorkRequestBuilder<ProactiveWellnessNudgeWorker>(
                safeInterval,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(WORK_PERIODIC_WELLNESS_NUDGE)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_PERIODIC_WELLNESS_NUDGE,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun scheduleFollowUpWork(
            context: Context,
            delayMinutes: Long = DEFAULT_SNOOZE_FOLLOWUP_MINUTES
        ) {
            val safeDelay = delayMinutes.coerceAtLeast(15L)
            val request = OneTimeWorkRequestBuilder<ProactiveWellnessNudgeWorker>()
                .setInitialDelay(safeDelay, TimeUnit.MINUTES)
                .addTag(WORK_PERIODIC_WELLNESS_NUDGE)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_FOLLOWUP_WELLNESS_NUDGE,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun enqueueImmediateWork(
            context: Context,
            forcePreview: Boolean = false
        ) {
            val request = OneTimeWorkRequestBuilder<ProactiveWellnessNudgeWorker>()
                .setInputData(workDataOf(INPUT_FORCE_PREVIEW to forcePreview))
                .addTag(WORK_PERIODIC_WELLNESS_NUDGE)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_IMMEDIATE_WELLNESS_NUDGE,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancelNotification(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.cancel(NOTIFICATION_ID_WELLNESS)
        }

        fun cancelAllWork(context: Context) {
            cancelNotification(context)
            val wm = WorkManager.getInstance(context)
            wm.cancelUniqueWork(WORK_PERIODIC_WELLNESS_NUDGE)
            wm.cancelUniqueWork(WORK_FOLLOWUP_WELLNESS_NUDGE)
            wm.cancelUniqueWork(WORK_IMMEDIATE_WELLNESS_NUDGE)
        }

        fun dispatch(
            context: Context,
            payload: ProactiveWellnessNotificationPayload
        ): Boolean {
            if (!hasNotificationPermission(context)) return false
            createChannel(context)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return false

            val openIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("notificationId", NOTIFICATION_ID_WELLNESS)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE_OPEN_APP,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeIntent = Intent(context, WellnessNudgeActionReceiver::class.java).apply {
                action = WellnessNudgeActionReceiver.ACTION_SNOOZE_45M
                putExtra(WellnessNudgeActionReceiver.EXTRA_NOTIFICATION_ID, NOTIFICATION_ID_WELLNESS)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_SNOOZE_ACTION,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_WELLNESS_NUDGES)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(payload.title)
                .setContentText(payload.shortBody)
                .setStyle(NotificationCompat.BigTextStyle().bigText(payload.expandedBody))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(openPendingIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)

            when (payload.primaryActionType) {
                WellnessNotificationActionType.LOG_HYDRATION -> {
                    val hydrateIntent = Intent(context, WellnessNudgeActionReceiver::class.java).apply {
                        action = WellnessNudgeActionReceiver.ACTION_LOG_HYDRATION
                        putExtra(WellnessNudgeActionReceiver.EXTRA_NOTIFICATION_ID, NOTIFICATION_ID_WELLNESS)
                    }
                    val hydratePendingIntent = PendingIntent.getBroadcast(
                        context,
                        REQUEST_CODE_PRIMARY_ACTION,
                        hydrateIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.addAction(
                        android.R.drawable.checkbox_on_background,
                        payload.primaryActionLabel,
                        hydratePendingIntent
                    )
                }
                WellnessNotificationActionType.LOG_REST_BREAK -> {
                    val restIntent = Intent(context, WellnessNudgeActionReceiver::class.java).apply {
                        action = WellnessNudgeActionReceiver.ACTION_LOG_REST_BREAK
                        putExtra(WellnessNudgeActionReceiver.EXTRA_NOTIFICATION_ID, NOTIFICATION_ID_WELLNESS)
                        putExtra(
                            WellnessNudgeActionReceiver.EXTRA_DURATION_MINUTES,
                            payload.recommendedDurationMinutes
                        )
                    }
                    val restPendingIntent = PendingIntent.getBroadcast(
                        context,
                        REQUEST_CODE_PRIMARY_ACTION,
                        restIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.addAction(
                        android.R.drawable.checkbox_on_background,
                        payload.primaryActionLabel,
                        restPendingIntent
                    )
                }
                WellnessNotificationActionType.OPEN_FLOW -> {
                    builder.addAction(
                        android.R.drawable.ic_menu_view,
                        payload.primaryActionLabel,
                        openPendingIntent
                    )
                }
            }

            builder.addAction(
                android.R.drawable.ic_menu_recent_history,
                "Snooze 45m",
                snoozePendingIntent
            )

            manager.notify(NOTIFICATION_ID_WELLNESS, builder.build())
            return true
        }
    }
}
