package com.neuroflow.app.presentation.progress

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.domain.model.TaskStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar
import java.util.TimeZone

class ProgressViewModelTest : StringSpec({

    "weekly comparison counts last week across the calendar year boundary" {
        val originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        try {
            fun at(year: Int, month: Int, day: Int): Long =
                Calendar.getInstance().apply {
                    set(year, month, day, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

            val tasks = listOf(
                TaskEntity(
                    id = "this-week",
                    title = "This week",
                    status = TaskStatus.COMPLETED,
                    completedAt = at(2026, Calendar.JANUARY, 5),
                    totalTimeTrackedMinutes = 600f
                ),
                TaskEntity(
                    id = "last-week",
                    title = "Last week",
                    status = TaskStatus.COMPLETED,
                    completedAt = at(2025, Calendar.DECEMBER, 31)
                ),
                TaskEntity(
                    id = "older",
                    title = "Older",
                    status = TaskStatus.COMPLETED,
                    completedAt = at(2025, Calendar.DECEMBER, 20)
                )
            )

            val sessions = listOf(
                // This week: one block with a stored duration, one closed without it.
                TimeSessionEntity(
                    taskId = "this-week", startedAt = at(2026, Calendar.JANUARY, 4) - 3_600_000,
                    endedAt = at(2026, Calendar.JANUARY, 4) - 1_200_000, durationMinutes = 40f
                ),
                TimeSessionEntity(
                    taskId = "no-duration", startedAt = at(2026, Calendar.JANUARY, 5) - 600_000,
                    endedAt = at(2026, Calendar.JANUARY, 5), durationMinutes = 0f
                ),
                // Last week and earlier: this week's total must stay clean of both.
                TimeSessionEntity(
                    taskId = "last-week", startedAt = at(2025, Calendar.DECEMBER, 31) - 3_600_000,
                    endedAt = at(2025, Calendar.DECEMBER, 31), durationMinutes = 30f
                ),
                TimeSessionEntity(
                    taskId = "long-ago", startedAt = at(2025, Calendar.DECEMBER, 20) - 3_600_000,
                    endedAt = at(2025, Calendar.DECEMBER, 20), durationMinutes = 90f
                )
            )

            val summary = calculateWeeklyProgressSummary(
                completed = tasks,
                sessions = sessions,
                nowMillis = at(2026, Calendar.JANUARY, 5)
            )

            summary.completedThisWeek shouldBe 1
            summary.deltaTasksVsLastWeek shouldBe 0
            // 40 + 10 recovered from timestamps, not the task's 600 minute lifetime total.
            summary.focusMinutesThisWeek shouldBe 50
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }
})
