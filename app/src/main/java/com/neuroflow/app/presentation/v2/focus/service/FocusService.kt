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
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.local.entity.startOfPreviousLocalDay
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.presentation.v2.focus.FocusDndHelper
import com.neuroflow.app.presentation.v2.focus.engine.FocusTimingCalculator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
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
    @Inject lateinit var preferencesDataStore: com.neuroflow.app.data.local.UserPreferencesDataStore
    @Inject lateinit var focusSessionFinalizer: FocusSessionFinalizer

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

        val intentTaskId = intent.getStringExtra(EXTRA_TASK_ID)
        if (taskId.isBlank() && !intentTaskId.isNullOrBlank()) {
            taskId = intentTaskId
            taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: taskTitle
            durationSeconds = intent.getLongExtra(EXTRA_DURATION_SECONDS, durationSeconds)
            startedAtMillis = intent.getLongExtra(EXTRA_STARTED_AT_MILLIS, startedAtMillis)
            accumulatedPauseMillis = intent.getLongExtra(EXTRA_ACCUMULATED_PAUSE_MILLIS, accumulatedPauseMillis)
            if (intent.hasExtra(EXTRA_PAUSED_AT_MILLIS)) {
                pausedAtMillis = intent.getLongExtra(EXTRA_PAUSED_AT_MILLIS, 0L)
            }
            if (intent.hasExtra(EXTRA_IS_PAUSED)) {
                isPaused = intent.getBooleanExtra(EXTRA_IS_PAUSED, isPaused)
            }
        }

        when (intent.action) {
            ACTION_START -> {
                val newTaskId = intent.getStringExtra(EXTRA_TASK_ID) ?: taskId
                val newStartedAt = intent.getLongExtra(EXTRA_STARTED_AT_MILLIS, System.currentTimeMillis())
                val newAccPause = intent.getLongExtra(EXTRA_ACCUMULATED_PAUSE_MILLIS, 0L)
                val hasPausedAtExtra = intent.hasExtra(EXTRA_PAUSED_AT_MILLIS)
                val newPausedAt = if (hasPausedAtExtra) intent.getLongExtra(EXTRA_PAUSED_AT_MILLIS, 0L) else null
                val newIsPaused = intent.getBooleanExtra(EXTRA_IS_PAUSED, false)

                val isSameTaskAndSession = (newTaskId == taskId && startedAtMillis == newStartedAt && startedAtMillis > 0L)
                taskId = newTaskId
                taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: taskTitle
                durationSeconds = intent.getLongExtra(EXTRA_DURATION_SECONDS, durationSeconds)
                startedAtMillis = newStartedAt
                if (!isSameTaskAndSession) {
                    accumulatedPauseMillis = newAccPause
                    pausedAtMillis = newPausedAt
                    isPaused = newIsPaused
                } else {
                    if (newAccPause > accumulatedPauseMillis) {
                        accumulatedPauseMillis = newAccPause
                    }
                    if (hasPausedAtExtra) {
                        pausedAtMillis = newPausedAt
                        isPaused = newIsPaused
                    }
                }
                val now = System.currentTimeMillis()
                val timing = FocusTimingCalculator.calculate(
                    startedAtMillis = startedAtMillis,
                    accumulatedPauseMillis = accumulatedPauseMillis,
                    pausedAtMillis = pausedAtMillis,
                    nowMillis = now,
                    targetDurationSeconds = durationSeconds
                )
                val isStopwatch = durationSeconds <= 0L
                _activeSession.value = ActiveFocusSession(
                    taskId = taskId,
                    taskTitle = taskTitle,
                    remainingSeconds = if (isStopwatch) timing.elapsedSeconds else timing.remainingSeconds,
                    totalSeconds = durationSeconds,
                    isPaused = isPaused
                )
                updateNotification()
                if (!isPaused) {
                    serviceScope.launch(Dispatchers.IO) {
                        try {
                            if (::preferencesDataStore.isInitialized) {
                                val prefs = preferencesDataStore.preferencesFlow.first()
                                if (prefs.dndEnabled) {
                                    FocusDndHelper.enableDndIfPermitted(applicationContext)
                                }
                            }
                        } catch (_: Throwable) {}
                    }
                    startTicker()
                } else {
                    tickerJob?.cancel()
                    tickerJob = null
                }
            }
            ACTION_PAUSE -> {
                if (!isPaused) {
                    val now = System.currentTimeMillis()
                    pausedAtMillis = FocusTimingCalculator.onPause(pausedAtMillis, now)
                    isPaused = true
                    FocusDndHelper.disableDnd(applicationContext)
                    tickerJob?.cancel()
                    tickerJob = null
                    val timing = FocusTimingCalculator.calculate(
                        startedAtMillis = startedAtMillis,
                        accumulatedPauseMillis = accumulatedPauseMillis,
                        pausedAtMillis = pausedAtMillis,
                        nowMillis = now,
                        targetDurationSeconds = durationSeconds
                    )
                    val isStopwatch = durationSeconds <= 0L
                    _activeSession.value = ActiveFocusSession(
                        taskId = taskId,
                        taskTitle = taskTitle,
                        remainingSeconds = if (isStopwatch) timing.elapsedSeconds else timing.remainingSeconds,
                        totalSeconds = durationSeconds,
                        isPaused = true
                    )
                    updateNotification()
                    serviceScope.launch(Dispatchers.IO) {
                        try {
                            if (::sessionRepository.isInitialized && taskId.isNotBlank()) {
                                val open = sessionRepository.getOpenSessionForTask(taskId)
                                if (open != null) {
                                    sessionRepository.update(
                                        open.copy(
                                            pausedAt = pausedAtMillis,
                                            pauseResumeCount = open.pauseResumeCount + 1
                                        )
                                    )
                                }
                            }
                        } catch (_: Throwable) {}
                    }
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
                    serviceScope.launch(Dispatchers.IO) {
                        try {
                            if (::preferencesDataStore.isInitialized) {
                                val prefs = preferencesDataStore.preferencesFlow.first()
                                if (prefs.dndEnabled) {
                                    FocusDndHelper.enableDndIfPermitted(applicationContext)
                                }
                            }
                        } catch (_: Throwable) {}
                    }
                    val timing = FocusTimingCalculator.calculate(
                        startedAtMillis = startedAtMillis,
                        accumulatedPauseMillis = accumulatedPauseMillis,
                        pausedAtMillis = pausedAtMillis,
                        nowMillis = now,
                        targetDurationSeconds = durationSeconds
                    )
                    val isStopwatch = durationSeconds <= 0L
                    _activeSession.value = ActiveFocusSession(
                        taskId = taskId,
                        taskTitle = taskTitle,
                        remainingSeconds = if (isStopwatch) timing.elapsedSeconds else timing.remainingSeconds,
                        totalSeconds = durationSeconds,
                        isPaused = false
                    )
                    updateNotification()
                    startTicker()
                    serviceScope.launch(Dispatchers.IO) {
                        try {
                            if (::sessionRepository.isInitialized && taskId.isNotBlank()) {
                                val open = sessionRepository.getOpenSessionForTask(taskId)
                                if (open != null) {
                                    sessionRepository.update(
                                        open.copy(
                                            pausedAt = null,
                                            totalPausedMs = newAcc
                                        )
                                    )
                                }
                            }
                        } catch (_: Throwable) {}
                    }
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
                val isExpired = durationSeconds > 0L && timing.isExpired
                if (isExpired) {
                    _activeSession.value = ActiveFocusSession(
                        taskId = taskId,
                        taskTitle = taskTitle,
                        remainingSeconds = 0L,
                        totalSeconds = durationSeconds,
                        isPaused = false
                    )
                    if (::focusSessionFinalizer.isInitialized && taskId.isNotBlank()) {
                        val capturedTaskId = taskId
                        val capturedStartedAt = startedAtMillis
                        val capturedAccPause = accumulatedPauseMillis
                        val capturedPausedAt = pausedAtMillis
                        val capturedDurationSec = durationSeconds
                        val appCtx = applicationContext
                        CoroutineScope(NonCancellable + Dispatchers.IO).launch {
                            runCatching {
                                focusSessionFinalizer.finalize(
                                    taskId = capturedTaskId,
                                    markComplete = false,
                                    now = now,
                                    fallbackStartedAt = capturedStartedAt,
                                    fallbackAccumulatedPauseMs = capturedAccPause,
                                    fallbackPausedAtMs = capturedPausedAt,
                                    fallbackIsPaused = false,
                                    fallbackDurationSeconds = capturedDurationSec,
                                    fallbackPauseCount = 0,
                                    context = appCtx
                                )
                            }
                        }
                    }
                    FocusDndHelper.disableDnd(applicationContext)
                    showSprintCompletedNotification()
                    com.neuroflow.app.worker.FocusWidgetUpdateWorker.enqueueImmediateUpdate(applicationContext)
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                        } else {
                            @Suppress("DEPRECATION")
                            stopForeground(true)
                        }
                    } catch (_: Throwable) {}
                    stopSelf()
                    break
                }
                val isStopwatch = durationSeconds <= 0L
                _activeSession.value = ActiveFocusSession(
                    taskId = taskId,
                    taskTitle = taskTitle,
                    remainingSeconds = if (isStopwatch) timing.elapsedSeconds else timing.remainingSeconds,
                    totalSeconds = durationSeconds,
                    isPaused = isPaused
                )
                if (!isPaused) {
                    updateNotification()
                }
            }
        }
    }

    private fun showSprintCompletedNotification() {
        try {
            val nm = getSystemService(NotificationManager::class.java) ?: return
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

            val title = if (taskTitle.isNotBlank()) taskTitle else "Deep Focus"
            val notif = NotificationCompat.Builder(this, CHANNEL_ID_COMPLETED)
                .setContentTitle("🎯 Sprint Complete: $title")
                .setContentText("Great job completing your sprint! Tap to keep going or take a rest.")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(false)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentPendingIntent)
                .build()

            nm.notify(NOTIFICATION_ID_COMPLETED, notif)
        } catch (_: Throwable) {
            // Defensive: headless / unit test
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
            val resumeIntent = Intent(this, FocusService::class.java).apply {
                action = ACTION_RESUME
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_TITLE, taskTitle)
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
                putExtra(EXTRA_STARTED_AT_MILLIS, startedAtMillis)
                putExtra(EXTRA_ACCUMULATED_PAUSE_MILLIS, accumulatedPauseMillis)
            }
            PendingIntent.getService(this, 1, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            val pauseIntent = Intent(this, FocusService::class.java).apply {
                action = ACTION_PAUSE
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_TITLE, taskTitle)
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
                putExtra(EXTRA_STARTED_AT_MILLIS, startedAtMillis)
                putExtra(EXTRA_ACCUMULATED_PAUSE_MILLIS, accumulatedPauseMillis)
            }
            PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val action1Title = if (isPaused) "Resume" else "Pause"
        val action1Icon = if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause

        val completeIntent = Intent(this, FocusService::class.java).apply {
            action = ACTION_COMPLETE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
            putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
            putExtra(EXTRA_STARTED_AT_MILLIS, startedAtMillis)
            putExtra(EXTRA_ACCUMULATED_PAUSE_MILLIS, accumulatedPauseMillis)
        }
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
                val liveChannel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Live focus session countdown and controls"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    enableVibration(false)
                    setSound(null, null)
                }
                val completedChannel = NotificationChannel(
                    CHANNEL_ID_COMPLETED,
                    CHANNEL_NAME_COMPLETED,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when your deep focus sprint timer completes"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    enableVibration(true)
                }
                val nm = getSystemService(NotificationManager::class.java)
                nm?.createNotificationChannel(liveChannel)
                nm?.createNotificationChannel(completedChannel)
            }
        } catch (_: Throwable) {
            // Defensive: headless / unit test
        }
    }

    private fun handleCompleteAndStop(activeTaskId: String) {
        FocusDndHelper.disableDnd(applicationContext)
        tickerJob?.cancel()
        tickerJob = null
        val targetTaskId = if (activeTaskId.isNotBlank()) activeTaskId else taskId
        serviceScope.launch {
            withContext(NonCancellable + Dispatchers.IO) {
                try {
                    if (::focusSessionFinalizer.isInitialized && targetTaskId.isNotBlank()) {
                        focusSessionFinalizer.finalize(
                            taskId = targetTaskId,
                            markComplete = true,
                            now = System.currentTimeMillis(),
                            fallbackStartedAt = startedAtMillis,
                            fallbackAccumulatedPauseMs = accumulatedPauseMillis,
                            fallbackPausedAtMs = pausedAtMillis,
                            fallbackIsPaused = isPaused,
                            fallbackDurationSeconds = durationSeconds,
                            fallbackPauseCount = 0,
                            context = applicationContext
                        )
                    } else if (::taskRepository.isInitialized && ::sessionRepository.isInitialized && targetTaskId.isNotBlank()) {
                        val now = System.currentTimeMillis()
                        val openSession = sessionRepository.getOpenSessionForTask(targetTaskId)
                        val baseStartedAt = openSession?.startedAt?.takeIf { it > 0L }
                            ?: startedAtMillis.takeIf { it > 0L }
                            ?: now
                        val effectiveAccPause = maxOf(accumulatedPauseMillis, openSession?.totalPausedMs ?: 0L)
                        val currentPause = if (isPaused) (now - (pausedAtMillis ?: now)) else 0L
                        val totalPause = (effectiveAccPause + currentPause).coerceAtLeast(0L)
                        val rawDurationMs = (now - baseStartedAt).coerceAtLeast(0L)
                        val netDurationMs = (rawDurationMs - totalPause).coerceAtLeast(0L)
                        val maxAllowedMins = if (durationSeconds > 0L) (durationSeconds / 60f) else com.neuroflow.app.data.local.entity.TimeSessionEntity.LIVE_SESSION_CEILING_MINUTES
                        val durationMins = (netDurationMs / 60000f).coerceIn(1f, maxAllowedMins)
                        val calculatedEndAt = if (durationSeconds > 0L) {
                            minOf(now, baseStartedAt + (durationSeconds * 1000L) + totalPause)
                        } else {
                            minOf(now, baseStartedAt + (maxAllowedMins * 60_000L).toLong() + totalPause)
                        }

                        if (openSession != null) {
                            sessionRepository.update(
                                openSession.copy(
                                    endedAt = calculatedEndAt,
                                    pausedAt = null,
                                    totalPausedMs = totalPause,
                                    durationMinutes = durationMins
                                )
                            )
                        }
                        AutonomyNudgeEngine.cancelNudge(applicationContext, targetTaskId)
                        com.neuroflow.app.worker.TaskReminderWorker.cancelReminder(applicationContext, targetTaskId)
                        try {
                            val nm = getSystemService(NotificationManager::class.java)
                            nm?.cancel(targetTaskId.hashCode())
                        } catch (_: Throwable) {}
                        val task = taskRepository.getById(targetTaskId)
                        if (task != null && task.status != com.neuroflow.app.domain.model.TaskStatus.COMPLETED) {
                            val finalTracked = task.totalTimeTrackedMinutes + durationMins
                            val est = TaskBlockPlan.plannedMinutes(task).toFloat()
                            val mape = if (est > 0f) Math.abs(finalTracked - est) / est else null
                            val smape = if (est > 0f || finalTracked > 0f) (2f * Math.abs(finalTracked - est)) / (est + finalTracked) else null
                            val points = 50 + (if (task.isFrog) 50 else 0) + (task.impactScore / 2)
                            val taskToComplete = task.copy(
                                totalTimeTrackedMinutes = finalTracked,
                                actualDurationMinutes = finalTracked,
                                sessionCount = task.sessionCount + 1,
                                lastSessionDurationMinutes = durationMins,
                                estimationErrorMape = mape,
                                estimationErrorSmape = smape,
                                focusModePoints = points
                            )
                            taskRepository.completeAndRecur(taskToComplete, now)
                            try {
                                preferencesDataStore.updatePreferences { prefs ->
                                    val newTotalFocus = prefs.totalFocusMinutes + durationMins.toInt()
                                    val newCompleted = prefs.totalTasksCompleted + 1
                                    val lastActive = prefs.lastActiveDate
                                    val todayStart = startOfLocalDay(now)
                                    val isNewDay = lastActive < todayStart
                                    val isConsecutive = lastActive >= startOfPreviousLocalDay(todayStart) && isNewDay
                                    val newStreak = when {
                                        isConsecutive -> prefs.dailyStreak + 1
                                        isNewDay -> 1
                                        else -> prefs.dailyStreak
                                    }
                                    val longest = maxOf(prefs.longestStreak, newStreak)
                                    prefs.copy(
                                        totalFocusMinutes = newTotalFocus,
                                        totalTasksCompleted = newCompleted,
                                        lastActiveDate = now,
                                        dailyStreak = newStreak,
                                        longestStreak = longest
                                    )
                                }
                            } catch (_: Throwable) {
                                // Defensive: headless/test environment
                            }
                        }
                    }
                } catch (_: Throwable) {
                    // Defensive: protect teardown flow
                }
                com.neuroflow.app.worker.FocusWidgetUpdateWorker.enqueueImmediateUpdate(applicationContext)
            }
            stopForegroundAndSelf()
        }
    }

    private fun stopForegroundAndSelf() {
        FocusDndHelper.disableDnd(applicationContext)
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
        com.neuroflow.app.worker.FocusWidgetUpdateWorker.enqueueImmediateUpdate(applicationContext)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        FocusDndHelper.disableDnd(applicationContext)
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
        const val CHANNEL_ID_COMPLETED = "focus_completed"
        const val CHANNEL_NAME_COMPLETED = "Sprint Completion Alerts"
        const val NOTIFICATION_ID = 3001
        const val NOTIFICATION_ID_COMPLETED = 3002

        const val ACTION_START = "com.neuroflow.app.focus.ACTION_START"
        const val ACTION_PAUSE = "com.neuroflow.app.focus.ACTION_PAUSE"
        const val ACTION_RESUME = "com.neuroflow.app.focus.ACTION_RESUME"
        const val ACTION_COMPLETE = "com.neuroflow.app.focus.ACTION_COMPLETE"
        const val ACTION_STOP = "com.neuroflow.app.focus.ACTION_STOP"

        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_TASK_TITLE = "taskTitle"
        const val EXTRA_DURATION_SECONDS = "durationSeconds"
        const val EXTRA_STARTED_AT_MILLIS = "startedAtMillis"
        const val EXTRA_ACCUMULATED_PAUSE_MILLIS = "accumulatedPauseMillis"
        const val EXTRA_PAUSED_AT_MILLIS = "pausedAtMillis"
        const val EXTRA_IS_PAUSED = "isPaused"

        fun start(
            context: Context?,
            taskId: String,
            taskTitle: String,
            durationSeconds: Long,
            startedAtMillis: Long = System.currentTimeMillis(),
            accumulatedPauseMillis: Long = 0L,
            pausedAtMillis: Long? = null,
            isPaused: Boolean = false
        ) {
            if (context == null) return
            try {
                val intent = Intent(context, FocusService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_TASK_ID, taskId)
                    putExtra(EXTRA_TASK_TITLE, taskTitle)
                    putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
                    putExtra(EXTRA_STARTED_AT_MILLIS, startedAtMillis)
                    putExtra(EXTRA_ACCUMULATED_PAUSE_MILLIS, accumulatedPauseMillis)
                    if (pausedAtMillis != null) {
                        putExtra(EXTRA_PAUSED_AT_MILLIS, pausedAtMillis)
                    }
                    putExtra(EXTRA_IS_PAUSED, isPaused)
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
            startedAtMillis: Long = System.currentTimeMillis(),
            accumulatedPauseMillis: Long = 0L,
            pausedAtMillis: Long? = null,
            isPaused: Boolean = false
        ) = start(context, taskId, taskTitle, durationSeconds, startedAtMillis, accumulatedPauseMillis, pausedAtMillis, isPaused)

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

        fun complete(context: Context?, taskId: String = "") {
            if (context == null) return
            try {
                val intent = Intent(context, FocusService::class.java).apply {
                    action = ACTION_COMPLETE
                    if (taskId.isNotBlank()) {
                        putExtra(EXTRA_TASK_ID, taskId)
                    }
                }
                context.startService(intent)
            } catch (_: Throwable) {}
        }

        fun completeService(context: Context?, taskId: String = "") = complete(context, taskId)

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
