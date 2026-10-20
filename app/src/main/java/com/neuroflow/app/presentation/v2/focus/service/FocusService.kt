package com.neuroflow.app.presentation.v2.focus.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.neuroflow.app.MainActivity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.presentation.v2.focus.engine.FocusTimingCalculator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class FocusService : Service() {

    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var sessionRepository: SessionRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var tickerJob: Job? = null

    private var taskId: String = ""
    private var taskTitle: String = "Deep Focus"
    private var durationSeconds: Long = 1500L
    private var startedAtMillis: Long = 0L
    private var accumulatedPauseMillis: Long = 0L
    private var pausedAtMillis: Long? = null
    private var isPaused: Boolean = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Throwable) {
            // Defensive: Headless execution / unit test environments
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        when (intent.action) {
            ACTION_START -> {
                taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: taskId
                taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: taskTitle
                durationSeconds = intent.getLongExtra(EXTRA_DURATION_SECONDS, durationSeconds)
                startedAtMillis = intent.getLongExtra(EXTRA_STARTED_AT_MILLIS, System.currentTimeMillis())
                accumulatedPauseMillis = 0L
                pausedAtMillis = null
                isPaused = false
                _activeSession.value = ActiveFocusSession(
                    taskId = taskId,
                    taskTitle = taskTitle,
                    remainingSeconds = durationSeconds,
                    totalSeconds = durationSeconds,
                    isPaused = false
                )
                updateNotification()
                startTicker()
            }
            ACTION_PAUSE -> {
                if (!isPaused) {
                    val now = System.currentTimeMillis()
                    pausedAtMillis = FocusTimingCalculator.onPause(pausedAtMillis, now)
                    isPaused = true
                    _activeSession.value = _activeSession.value?.copy(isPaused = true)
                    updateNotification()
                }
            }
            ACTION_RESUME -> {
                if (isPaused) {
                    val now = System.currentTimeMillis()
                    val currentPause = pausedAtMillis ?: now
                    val (newAcc, _) = FocusTimingCalculator.onResume(currentPause, accumulatedPauseMillis, now)
                    accumulatedPauseMillis = newAcc
                    pausedAtMillis = null
                    isPaused = false
                    _activeSession.value = _activeSession.value?.copy(isPaused = false)
                    updateNotification()
                    startTicker()
                }
            }
            ACTION_COMPLETE -> {
                handleCompleteAndStop(taskId)
                return START_NOT_STICKY
            }
            ACTION_STOP -> {
                stopForegroundAndSelf()
                return START_NOT_STICKY
            }
        }

        return START_STICKY
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                if (!isActive) break
                val now = System.currentTimeMillis()
                val timing = FocusTimingCalculator.calculate(
                    startedAtMillis = startedAtMillis,
                    accumulatedPauseMillis = accumulatedPauseMillis,
                    pausedAtMillis = pausedAtMillis,
                    nowMillis = now,
                    targetDurationSeconds = durationSeconds
                )
                _activeSession.value = ActiveFocusSession(
                    taskId = taskId,
                    taskTitle = taskTitle,
                    remainingSeconds = timing.remainingSeconds,
                    totalSeconds = durationSeconds,
                    isPaused = isPaused
                )
                if (!isPaused) {
                    updateNotification()
                }
            }
        }
    }

    private fun updateNotification() {
        try {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            val notif = buildNotification()
            nm.notify(NOTIFICATION_ID, notif)
        } catch (_: Throwable) {
            // Defensive: headless / unit test
        }
    }

    private fun buildNotification(): Notification {
        val now = System.currentTimeMillis()
        val timing = FocusTimingCalculator.calculate(
            startedAtMillis = startedAtMillis,
            accumulatedPauseMillis = accumulatedPauseMillis,
            pausedAtMillis = pausedAtMillis,
            nowMillis = now,
            targetDurationSeconds = durationSeconds
        )

        val isStopwatch = durationSeconds <= 0L
        val totalSec = if (isStopwatch) 0 else durationSeconds.toInt()
        val elapsedSec = timing.elapsedSeconds.toInt()

        val formattedTime = formatTime(if (isStopwatch) timing.elapsedSeconds else timing.remainingSeconds)
        val contentText = if (isStopwatch) {
            if (isPaused) "$formattedTime elapsed (Paused)" else "$formattedTime elapsed"
        } else {
            if (isPaused) "$formattedTime remaining (Paused)" else "$formattedTime remaining"
        }

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            action = "com.procus.ACTION_OPEN_FOCUS"
            putExtra("taskId", taskId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val action1PendingIntent = if (isPaused) {
            val resumeIntent = Intent(this, FocusService::class.java).apply { action = ACTION_RESUME }
            PendingIntent.getService(this, 1, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            val pauseIntent = Intent(this, FocusService::class.java).apply { action = ACTION_PAUSE }
            PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val action1Title = if (isPaused) "Resume" else "Pause"
        val action1Icon = if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause

        val completeIntent = Intent(this, FocusService::class.java).apply { action = ACTION_COMPLETE }
        val completePendingIntent = PendingIntent.getService(
            this,
            2,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (taskTitle.isNotBlank()) taskTitle else "Deep Focus"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(totalSec, elapsedSec, isStopwatch)
            .setContentIntent(contentPendingIntent)
            .addAction(action1Icon, action1Title, action1PendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Complete", completePendingIntent)
            .build()
    }

    private fun formatTime(seconds: Long): String {
        val safe = maxOf(0L, seconds)
        val minutes = safe / 60
        val secs = safe % 60
        val mStr = minutes.toString().padStart(2, '0')
        val sStr = secs.toString().padStart(2, '0')
        return "$mStr:$sStr"
    }

    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Live focus session countdown and controls"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    enableVibration(false)
                    setSound(null, null)
                }
                val nm = getSystemService(NotificationManager::class.java)
                nm?.createNotificationChannel(channel)
            }
        } catch (_: Throwable) {
            // Defensive: headless / unit test
        }
    }

    private fun handleCompleteAndStop(activeTaskId: String) {
        tickerJob?.cancel()
        tickerJob = null
        serviceScope.launch {
            withContext(NonCancellable + Dispatchers.IO) {
                try {
                    if (::taskRepository.isInitialized && ::sessionRepository.isInitialized && activeTaskId.isNotBlank()) {
                        val now = System.currentTimeMillis()
                        val openSession = sessionRepository.getOpenSessionForTask(activeTaskId)
                        if (openSession != null) {
                            val durationMins = ((now - openSession.startedAt) / 60000f).coerceAtLeast(1f)
                            sessionRepository.update(openSession.copy(endedAt = now, durationMinutes = durationMins))
                        }
                        val task = taskRepository.getById(activeTaskId)
                        if (task != null) {
                            taskRepository.completeAndRecur(task, now)
                        }
                    }
                } catch (_: Throwable) {
                    // Defensive: protect teardown flow
                }
            }
            stopForegroundAndSelf()
        }
    }

    private fun stopForegroundAndSelf() {
        tickerJob?.cancel()
        tickerJob = null
        _activeSession.value = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.cancel(NOTIFICATION_ID)
        } catch (_: Throwable) {}
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
        serviceScope.cancel()
        _activeSession.value = null
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm?.cancel(NOTIFICATION_ID)
        } catch (_: Throwable) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        data class ActiveFocusSession(
            val taskId: String,
            val taskTitle: String,
            val remainingSeconds: Long,
            val totalSeconds: Long,
            val isPaused: Boolean
        )

        private val _activeSession = kotlinx.coroutines.flow.MutableStateFlow<ActiveFocusSession?>(null)
        val activeSession: kotlinx.coroutines.flow.StateFlow<ActiveFocusSession?> = _activeSession

        const val CHANNEL_ID = "focus_live"
        const val CHANNEL_NAME = "Active Focus Session"
        const val NOTIFICATION_ID = 3001

        const val ACTION_START = "com.neuroflow.app.focus.ACTION_START"
        const val ACTION_PAUSE = "com.neuroflow.app.focus.ACTION_PAUSE"
        const val ACTION_RESUME = "com.neuroflow.app.focus.ACTION_RESUME"
        const val ACTION_COMPLETE = "com.neuroflow.app.focus.ACTION_COMPLETE"
        const val ACTION_STOP = "com.neuroflow.app.focus.ACTION_STOP"

        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_TASK_TITLE = "taskTitle"
        const val EXTRA_DURATION_SECONDS = "durationSeconds"
        const val EXTRA_STARTED_AT_MILLIS = "startedAtMillis"

        fun start(
            context: Context?,
            taskId: String,
            taskTitle: String,
            durationSeconds: Long,
            startedAtMillis: Long = System.currentTimeMillis()
        ) {
            if (context == null) return
            try {
                val intent = Intent(context, FocusService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_TASK_ID, taskId)
                    putExtra(EXTRA_TASK_TITLE, taskTitle)
                    putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
                    putExtra(EXTRA_STARTED_AT_MILLIS, startedAtMillis)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Throwable) {}
        }

        fun startService(
            context: Context?,
            taskId: String,
            taskTitle: String,
            durationSeconds: Long,
            startedAtMillis: Long = System.currentTimeMillis()
        ) = start(context, taskId, taskTitle, durationSeconds, startedAtMillis)

        fun pause(context: Context?) {
            if (context == null) return
            try {
                val intent = Intent(context, FocusService::class.java).apply {
                    action = ACTION_PAUSE
                }
                context.startService(intent)
            } catch (_: Throwable) {}
        }

        fun pauseService(context: Context?) = pause(context)

        fun resume(context: Context?) {
            if (context == null) return
            try {
                val intent = Intent(context, FocusService::class.java).apply {
                    action = ACTION_RESUME
                }
                context.startService(intent)
            } catch (_: Throwable) {}
        }

        fun resumeService(context: Context?) = resume(context)

        fun stop(context: Context?) {
            if (context == null) return
            try {
                val intent = Intent(context, FocusService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
                context.stopService(intent)
            } catch (_: Throwable) {}
        }

        fun stopService(context: Context?) = stop(context)
    }
}
