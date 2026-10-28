package com.neuroflow.app.presentation.widget

import android.content.Context
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.local.entity.startOfPreviousLocalDay
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
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
            runCatching {
                val sessionRepo = runCatching { entryPoint.sessionRepository() }.getOrNull()
                val openSession = sessionRepo?.getOpenSessionForTask(taskId)
                if (openSession != null) {
                    val durationMins = openSession.trackedMinutes(now)
                    sessionRepo.update(
                        openSession.copy(
                            endedAt = now,
                            pausedAt = null,
                            durationMinutes = durationMins
                        )
                    )
                }
                if (FocusService.activeSession.value?.taskId == taskId) {
                    FocusService.stopService(context)
                }
            }
            val success = runCatching {
                entryPoint.taskRepository().markTaskCompleted(taskId)
            }.getOrDefault(false)
            if (success) {
                val now = System.currentTimeMillis()
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
                            totalTasksCompleted = prefs.totalTasksCompleted + 1
                        )
                    }
                }
            }
            FocusWidgetUpdateWorker.enqueueImmediateUpdate(context)
            success
        }
    }
}
