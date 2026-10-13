package com.neuroflow.app.data.local.entity

import com.neuroflow.app.domain.model.Recurrence
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar
import java.util.TimeZone

class TaskTimeExtensionsTest : StringSpec({

    val todayStart = startOfLocalDay(System.currentTimeMillis())
    val nineAmOffset = 9 * 3600_000L // 9:00 AM

    "effectiveScheduleAnchorMillis returns correct timestamp for non-recurring task" {
        val task = TaskEntity(
            title = "One-off Task",
            scheduledDate = todayStart,
            scheduledTime = nineAmOffset
        )
        task.effectiveScheduleAnchorMillis() shouldBe (todayStart + nineAmOffset)
    }

    "effectiveScheduleAnchorMillis includes scheduledTime for recurring habit" {
        val task = TaskEntity(
            title = "Daily Habit",
            recurrence = Recurrence.DAILY,
            habitDate = todayStart,
            scheduledTime = nineAmOffset
        )
        task.effectiveScheduleAnchorMillis() shouldBe (todayStart + nineAmOffset)
    }

    "timelineStartMinuteOfDay returns 540 (9 AM) for recurring task scheduled at 9 AM" {
        val task = TaskEntity(
            title = "Morning Reading",
            recurrence = Recurrence.DAILY,
            habitDate = todayStart,
            scheduledTime = nineAmOffset
        )
        task.timelineStartMinuteOfDay() shouldBe 540
    }

    "effectiveReminderTargetMillis defaults to 9 AM when scheduledTime is null for recurring habit" {
        val task = TaskEntity(
            title = "Meditation",
            recurrence = Recurrence.DAILY,
            habitDate = todayStart,
            scheduledTime = null
        )
        task.effectiveReminderTargetMillis() shouldBe (todayStart + nineAmOffset)
    }

    "local day boundaries follow DST transitions" {
        val originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        try {
            val springForwardDay = Calendar.getInstance().apply {
                set(2026, Calendar.MARCH, 8, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val nextDay = startOfNextLocalDay(springForwardDay)
            nextDay - startOfLocalDay(springForwardDay) shouldBe 23 * 60 * 60 * 1000L

            val fallBackNextDay = Calendar.getInstance().apply {
                set(2026, Calendar.NOVEMBER, 2, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val previousDay = startOfPreviousLocalDay(fallBackNextDay)
            fallBackNextDay - previousDay shouldBe 25 * 60 * 60 * 1000L
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }
})
