package com.neuroflow.app.presentation.widget

import android.content.Context
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.local.entity.startOfPreviousLocalDay
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.presentation.v2.focus.service.FocusService
import com.neuroflow.app.worker.FocusWidgetUpdateWorker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Priority 3 (ADR-003): Interactive Home Screen Widget Action
 *
 * Encapsulates the task completion action callback for widget interactions,
 * executing Room v22 update under NonCancellable + Dispatchers.IO and triggering
 * immediate widget state refresh.
 */
object CompleteTaskWidgetAction {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetActionEntryPoint {
        fun taskRepository(): TaskRepository
        fun sessionRepository(): SessionRepository
        fun preferencesDataStore(): UserPreferencesDataStore
    }

    suspend fun execute(context: Context, taskId: String): Boolean {
        if (taskId.isBlank()) return false
        return withContext(NonCancellable + Dispatchers.IO) {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetActionEntryPoint::class.java
            )
            val now = System.currentTimeMillis()
            var durationMins = 0f
            runCatching {
                val sessionRepo = runCatching { entryPoint.sessionRepository() }.getOrNull()
                val openSession = sessionRepo?.getOpenSessionForTask(taskId)
                if (openSession != null) {
                    val targetDurationSec = com.neuroflow.app.presentation.v2.focus.FocusTimerModeV2.entries
                        .firstOrNull { it.name == openSession.sessionType }?.durationSeconds ?: 0L
                    val pauseStart = openSession.pausedAt
                    val currentPauseMs = if (pauseStart != null) maxOf(0L, now - pauseStart) else 0L
                    val effectiveTotalPause = openSession.totalPausedMs + currentPauseMs
                    val netDurationMs = maxOf(0L, now - openSession.startedAt - effectiveTotalPause)
                    val maxAllowedMins = if (targetDurationSec > 0L) (targetDurationSec / 60f) else com.neuroflow.app.data.local.entity.TimeSessionEntity.LIVE_SESSION_CEILING_MINUTES
                    durationMins = (netDurationMs / 60000f).coerceIn(1f, maxAllowedMins)
                    val calculatedEndAt = if (targetDurationSec > 0L) {
                        minOf(now, openSession.startedAt + (targetDurationSec * 1000L) + effectiveTotalPause)
                    } else {
                        minOf(now, openSession.startedAt + (maxAllowedMins * 60_000L).toLong() + effectiveTotalPause)
                    }
                    sessionRepo.update(
                        openSession.copy(
                            endedAt = calculatedEndAt,
                            pausedAt = null,
                            totalPausedMs = effectiveTotalPause,
                            durationMinutes = durationMins
                        )
                    )
                }
                if (FocusService.activeSession.value?.taskId == taskId) {
                    FocusService.stopService(context)
                }
            }
            val taskRepo = entryPoint.taskRepository()
            val existingTask = runCatching { taskRepo.getById(taskId) }.getOrNull()
            val success = if (existingTask != null && existingTask.status != TaskStatus.COMPLETED) {
                val finalTracked = existingTask.totalTimeTrackedMinutes + durationMins
                val est = TaskBlockPlan.plannedMinutes(existingTask).toFloat()
                val mape = if (est > 0f) Math.abs(finalTracked - est) / est else null
                val smape = if (est > 0f || finalTracked > 0f) (2f * Math.abs(finalTracked - est)) / (est + finalTracked) else null
                val points = 50 + (if (existingTask.isFrog) 50 else 0) + (existingTask.impactScore / 2)
                val taskToComplete = existingTask.copy(
                    totalTimeTrackedMinutes = finalTracked,
                    actualDurationMinutes = finalTracked,
                    sessionCount = existingTask.sessionCount + (if (durationMins > 0f) 1 else 0),
                    lastSessionDurationMinutes = if (durationMins > 0f) durationMins else existingTask.lastSessionDurationMinutes,
                    estimationErrorMape = mape,
                    estimationErrorSmape = smape,
                    focusModePoints = points
                )
                runCatching {
                    taskRepo.completeAndRecur(taskToComplete, now)
                    true
                }.getOrDefault(false)
            } else {
                runCatching {
                    taskRepo.markTaskCompleted(taskId)
                }.getOrDefault(false)
            }
            if (success) {
                runCatching {
                    com.neuroflow.app.domain.engine.AutonomyNudgeEngine.cancelNudge(context, taskId)
                    com.neuroflow.app.worker.TaskReminderWorker.cancelReminder(context, taskId)
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                    nm?.cancel(taskId.hashCode())
                }
                runCatching {
                    entryPoint.preferencesDataStore().updatePreferences { prefs ->
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
                            lastActiveDate = now,
                            dailyStreak = newStreak,
                            longestStreak = longest,
                            totalTasksCompleted = prefs.totalTasksCompleted + 1,
                            totalFocusMinutes = prefs.totalFocusMinutes + durationMins.toInt()
                        )
                    }
                }
            }
            FocusWidgetUpdateWorker.enqueueImmediateUpdate(context)
            success
        }
    }
}
