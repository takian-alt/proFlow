package com.neuroflow.app.presentation.v2.focus.service

import android.app.NotificationManager
import android.content.Context
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.local.entity.startOfPreviousLocalDay
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.engine.AutonomyNudgeEngine
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.presentation.v2.focus.FocusTimerModeV2
import com.neuroflow.app.worker.FocusWidgetUpdateWorker
import com.neuroflow.app.worker.TaskReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Atomic, idempotent coordinator for closing focus sessions and crediting task/streak stats.
 * Guarantees that concurrent completion or timer expiry across [FocusViewModelV2],
 * [FocusService], and [com.neuroflow.app.presentation.widget.CompleteTaskWidgetAction]
 * never double-credits focus minutes, session counts, or completion streaks.
 */
@Singleton
class FocusSessionFinalizer @Inject constructor(
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) {
    private val mutex = Mutex()
    private val finalizedSessionIds = object : LinkedHashMap<String, Boolean>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean =
            size > 64
    }

    data class FinalizationResult(
        val task: TaskEntity? = null,
        val durationMinutes: Float = 0f,
        val pointsEarned: Int = 0,
        val didCreditSession: Boolean = false,
        val didCompleteTask: Boolean = false
    )

    suspend fun finalize(
        taskId: String,
        markComplete: Boolean,
        now: Long = System.currentTimeMillis(),
        fallbackStartedAt: Long = 0L,
        fallbackAccumulatedPauseMs: Long = 0L,
        fallbackPausedAtMs: Long? = null,
        fallbackIsPaused: Boolean = false,
        fallbackDurationSeconds: Long = 0L,
        fallbackPauseCount: Int = 0,
        fallbackSessionId: String? = null,
        wasSprintAlreadyCompleted: Boolean = false,
        context: Context? = null
    ): FinalizationResult = withContext(NonCancellable + Dispatchers.IO) {
        mutex.withLock {
            var creditedDurationMins = 0f
            var didCreditSession = false

            val openSession = runCatching { sessionRepository.getOpenSessionForTask(taskId) }.getOrNull()
            if (openSession != null) {
                if (openSession.id !in finalizedSessionIds) {
                    val baseStartedAt = openSession.startedAt.takeIf { it > 0L }
                        ?: fallbackStartedAt.takeIf { it > 0L }
                        ?: now
                    val targetDurationSec = fallbackDurationSeconds.takeIf { it > 0L }
                        ?: FocusTimerModeV2.entries.firstOrNull { it.name == openSession.sessionType }?.durationSeconds
                        ?: 0L
                    val effectiveAccPause = maxOf(fallbackAccumulatedPauseMs, openSession.totalPausedMs)
                    val pauseStart = openSession.pausedAt ?: if (fallbackIsPaused) fallbackPausedAtMs else null
                    val currentPauseMs = if (pauseStart != null) maxOf(0L, now - pauseStart) else 0L
                    val totalPauseMs = (effectiveAccPause + currentPauseMs).coerceAtLeast(0L)
                    val netDurationMs = maxOf(0L, now - baseStartedAt - totalPauseMs)
                    val maxAllowedMins = if (targetDurationSec > 0L) {
                        targetDurationSec / 60f
                    } else {
                        TimeSessionEntity.LIVE_SESSION_CEILING_MINUTES
                    }
                    val durationMins = (netDurationMs / 60000f).coerceIn(1f, maxAllowedMins)
                    val calculatedEndAt = if (targetDurationSec > 0L) {
                        minOf(now, baseStartedAt + (targetDurationSec * 1000L) + totalPauseMs)
                    } else {
                        minOf(now, baseStartedAt + (maxAllowedMins * 60_000L).toLong() + totalPauseMs)
                    }
                    val mergedPauseCount = maxOf(fallbackPauseCount, openSession.pauseResumeCount)
                    sessionRepository.update(
                        openSession.copy(
                            endedAt = calculatedEndAt,
                            pausedAt = null,
                            totalPausedMs = totalPauseMs,
                            durationMinutes = durationMins,
                            pauseResumeCount = mergedPauseCount
                        )
                    )
                    finalizedSessionIds[openSession.id] = true
                    creditedDurationMins = durationMins
                    didCreditSession = true
                }
            } else if (!wasSprintAlreadyCompleted &&
                fallbackStartedAt > 0L &&
                fallbackSessionId != null &&
                fallbackSessionId !in finalizedSessionIds
            ) {
                val targetDurationSec = fallbackDurationSeconds
                val currentPauseMs = if (fallbackIsPaused && fallbackPausedAtMs != null) {
                    maxOf(0L, now - fallbackPausedAtMs)
                } else 0L
                val totalPauseMs = (fallbackAccumulatedPauseMs + currentPauseMs).coerceAtLeast(0L)
                val netDurationMs = maxOf(0L, now - fallbackStartedAt - totalPauseMs)
                val maxAllowedMins = if (targetDurationSec > 0L) {
                    targetDurationSec / 60f
                } else {
                    TimeSessionEntity.LIVE_SESSION_CEILING_MINUTES
                }
                creditedDurationMins = (netDurationMs / 60000f).coerceIn(1f, maxAllowedMins)
                finalizedSessionIds[fallbackSessionId] = true
                didCreditSession = true
            }

            if (markComplete && context != null) {
                runCatching { AutonomyNudgeEngine.cancelNudge(context, taskId) }
                runCatching { TaskReminderWorker.cancelReminder(context, taskId) }
                runCatching {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    nm?.cancel(taskId.hashCode())
                }
            }

            val task = runCatching { taskRepository.getById(taskId) }.getOrNull()
            if (!markComplete) {
                var resultTask = task
                if (didCreditSession && task != null) {
                    val finalTracked = task.totalTimeTrackedMinutes + creditedDurationMins
                    val updatedTask = task.copy(
                        totalTimeTrackedMinutes = finalTracked,
                        actualDurationMinutes = finalTracked,
                        sessionCount = task.sessionCount + 1,
                        lastSessionDurationMinutes = creditedDurationMins
                    )
                    taskRepository.update(updatedTask)
                    resultTask = updatedTask

                    val prefsMinutes = max(1, creditedDurationMins.roundToInt())
                    runCatching {
                        preferencesDataStore.updatePreferences { prefs ->
                            val newTotalFocus = prefs.totalFocusMinutes + prefsMinutes
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
                                dailyStreak = newStreak,
                                longestStreak = longest,
                                lastActiveDate = now
                            )
                        }
                    }
                }
                if (context != null) {
                    runCatching { FocusWidgetUpdateWorker.enqueueImmediateUpdate(context) }
                }
                return@withContext FinalizationResult(
                    task = resultTask,
                    durationMinutes = creditedDurationMins,
                    pointsEarned = 0,
                    didCreditSession = didCreditSession,
                    didCompleteTask = false
                )
            }

            // markComplete == true
            val isUntracked = taskId == TaskEntity.TASK_UNTRACKED_FOCUS
            if (isUntracked && task != null) {
                val addedMins = if (didCreditSession) creditedDurationMins else 0f
                val finalTracked = task.totalTimeTrackedMinutes + addedMins
                val updatedUntracked = task.copy(
                    status = TaskStatus.ACTIVE,
                    completedAt = null,
                    totalTimeTrackedMinutes = finalTracked,
                    actualDurationMinutes = finalTracked,
                    sessionCount = task.sessionCount + (if (didCreditSession) 1 else 0),
                    lastSessionDurationMinutes = if (didCreditSession) creditedDurationMins else task.lastSessionDurationMinutes,
                    updatedAt = now
                )
                taskRepository.update(updatedUntracked)
                val prefsFocusIncrement = if (didCreditSession) max(1, creditedDurationMins.roundToInt()) else 0
                if (prefsFocusIncrement > 0) {
                    runCatching {
                        preferencesDataStore.updatePreferences { prefs ->
                            val newTotalFocus = prefs.totalFocusMinutes + prefsFocusIncrement
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
                                dailyStreak = newStreak,
                                longestStreak = longest,
                                lastActiveDate = now
                            )
                        }
                    }
                }
                if (context != null) {
                    runCatching { FocusWidgetUpdateWorker.enqueueImmediateUpdate(context) }
                }
                return@withContext FinalizationResult(
                    task = updatedUntracked,
                    durationMinutes = creditedDurationMins,
                    pointsEarned = 0,
                    didCreditSession = didCreditSession,
                    didCompleteTask = false
                )
            }

            if (task != null && task.status != TaskStatus.COMPLETED) {
                val addedMins = if (didCreditSession) creditedDurationMins else 0f
                val finalTracked = task.totalTimeTrackedMinutes + addedMins
                val est = TaskBlockPlan.plannedMinutes(task).toFloat()
                val mape = if (est > 0f) abs(finalTracked - est) / est else null
                val smape = if (est > 0f || finalTracked > 0f) {
                    (2f * abs(finalTracked - est)) / (est + finalTracked)
                } else null
                val points = 50 + (if (task.isFrog) 50 else 0) + (task.impactScore / 2)

                val taskToComplete = task.copy(
                    totalTimeTrackedMinutes = finalTracked,
                    actualDurationMinutes = finalTracked,
                    sessionCount = task.sessionCount + (if (didCreditSession) 1 else 0),
                    lastSessionDurationMinutes = if (didCreditSession) creditedDurationMins else task.lastSessionDurationMinutes,
                    estimationErrorMape = mape,
                    estimationErrorSmape = smape,
                    focusModePoints = points
                )
                taskRepository.completeAndRecur(taskToComplete, now)
                val finalSnapshot = runCatching { taskRepository.getById(taskId) }.getOrNull()
                    ?: taskToComplete.copy(status = TaskStatus.COMPLETED, completedAt = now)

                val prefsFocusIncrement = if (didCreditSession) max(1, creditedDurationMins.roundToInt()) else 0
                runCatching {
                    preferencesDataStore.updatePreferences { prefs ->
                        val newTotalFocus = prefs.totalFocusMinutes + prefsFocusIncrement
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
                }
                if (context != null) {
                    runCatching { FocusWidgetUpdateWorker.enqueueImmediateUpdate(context) }
                }
                return@withContext FinalizationResult(
                    task = finalSnapshot,
                    durationMinutes = creditedDurationMins,
                    pointsEarned = points,
                    didCreditSession = didCreditSession,
                    didCompleteTask = true
                )
            } else {
                val didComplete = if (task == null) {
                    runCatching { taskRepository.markTaskCompleted(taskId, now) }.getOrDefault(false)
                } else false
                if (context != null) {
                    runCatching { FocusWidgetUpdateWorker.enqueueImmediateUpdate(context) }
                }
                return@withContext FinalizationResult(
                    task = task,
                    durationMinutes = creditedDurationMins,
                    pointsEarned = task?.focusModePoints ?: 0,
                    didCreditSession = didCreditSession,
                    didCompleteTask = didComplete
                )
            }
        }
    }
}
