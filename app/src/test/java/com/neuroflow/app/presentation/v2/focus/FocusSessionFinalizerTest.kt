package com.neuroflow.app.presentation.v2.focus

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.presentation.v2.focus.service.FocusSessionFinalizer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusSessionFinalizerTest {

    @Test
    fun `finalize is idempotent across duplicate and concurrent natural expiry calls`() = runTest {
        val now = 1_760_000_000_000L
        val startedAt = now - (25 * 60_000L)

        var currentTask = TaskEntity(
            id = "focus-task-1",
            title = "Deep Coding",
            estimatedDurationMinutes = 25,
            status = TaskStatus.ACTIVE,
            totalTimeTrackedMinutes = 0f,
            sessionCount = 0
        )
        var currentSession: TimeSessionEntity? = TimeSessionEntity(
            id = "session-1",
            taskId = "focus-task-1",
            startedAt = startedAt,
            endedAt = null,
            sessionType = FocusTimerModeV2.POMODORO_25.name
        )
        val prefsFlow = MutableStateFlow(UserPreferences(totalFocusMinutes = 10))

        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { taskRepo.getById("focus-task-1") } answers { currentTask }
        coEvery { taskRepo.update(any()) } answers {
            currentTask = firstArg()
        }
        coEvery { sessionRepo.getOpenSessionForTask("focus-task-1") } answers {
            currentSession?.takeIf { it.endedAt == null }
        }
        coEvery { sessionRepo.update(any()) } answers {
            currentSession = firstArg()
        }
        coEvery { prefsDataStore.updatePreferences(any()) } coAnswers {
            val transform = firstArg<(UserPreferences) -> UserPreferences>()
            prefsFlow.value = transform(prefsFlow.value)
            mockk(relaxed = true)
        }

        val finalizer = FocusSessionFinalizer(
            taskRepository = taskRepo,
            sessionRepository = sessionRepo,
            preferencesDataStore = prefsDataStore
        )

        // Fire two concurrent expiry finalizations for the same sprint
        val results = listOf(
            async {
                finalizer.finalize(
                    taskId = "focus-task-1",
                    markComplete = false,
                    now = now,
                    fallbackStartedAt = startedAt,
                    fallbackDurationSeconds = 1500L,
                    fallbackSessionId = "session-1"
                )
            },
            async {
                finalizer.finalize(
                    taskId = "focus-task-1",
                    markComplete = false,
                    now = now,
                    fallbackStartedAt = startedAt,
                    fallbackDurationSeconds = 1500L,
                    fallbackSessionId = "session-1"
                )
            }
        ).awaitAll()

        assertEquals(1, results.count { it.didCreditSession })
        assertEquals(1, results.count { !it.didCreditSession })
        assertEquals(25f, currentTask.totalTimeTrackedMinutes, 0.01f)
        assertEquals(1, currentTask.sessionCount)
        assertEquals(35, prefsFlow.value.totalFocusMinutes)
        coVerify(exactly = 1) { sessionRepo.update(any()) }

        // Sequential fallback call with same session ID must also stay idempotent
        val thirdResult = finalizer.finalize(
            taskId = "focus-task-1",
            markComplete = false,
            now = now + 1000L,
            fallbackStartedAt = startedAt,
            fallbackDurationSeconds = 1500L,
            fallbackSessionId = "session-1"
        )
        assertFalse(thirdResult.didCreditSession)
        assertEquals(25f, currentTask.totalTimeTrackedMinutes, 0.01f)
        assertEquals(1, currentTask.sessionCount)
        assertEquals(35, prefsFlow.value.totalFocusMinutes)
        assertTrue(currentSession?.endedAt != null)
    }
}
