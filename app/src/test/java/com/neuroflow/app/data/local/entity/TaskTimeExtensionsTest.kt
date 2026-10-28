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

    "BUG-157: effectiveReminderTargetMillis returns null for untimed non-recurring tasks preventing false midnight alarms" {
        val untimedScheduled = TaskEntity(
            title = "Untimed Scheduled",
            scheduledDate = todayStart + 86_400_000L,
            scheduledTime = null
        )
        untimedScheduled.effectiveReminderTargetMillis() shouldBe null

        val untimedDeadline = TaskEntity(
            title = "Untimed Deadline",
            deadlineDate = todayStart + 86_400_000L,
            deadlineTime = null
        )
        untimedDeadline.effectiveReminderTargetMillis() shouldBe null

        val timedScheduled = TaskEntity(
            title = "Timed Scheduled",
            scheduledDate = todayStart + 86_400_000L,
            scheduledTime = nineAmOffset
        )
        timedScheduled.effectiveReminderTargetMillis() shouldBe (todayStart + 86_400_000L + nineAmOffset)

        val timedDeadline = TaskEntity(
            title = "Timed Deadline",
            deadlineDate = todayStart + 86_400_000L,
            deadlineTime = nineAmOffset
        )
        timedDeadline.effectiveReminderTargetMillis() shouldBe (todayStart + 86_400_000L + nineAmOffset)
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

    "utcDateMillisToLocalStartOfDay and localStartOfDayToUtcDateMillis round-trip across timezones (BUG-037)" {
        val nyZone = java.time.ZoneId.of("America/New_York")
        val tokyoZone = java.time.ZoneId.of("Asia/Tokyo")
        val utcZone = java.time.ZoneId.of("UTC")

        // 2026-10-25 in UTC is 1761350400000L (2026-10-25T00:00:00Z)
        val oct25Utc = java.time.LocalDate.of(2026, 10, 25).atStartOfDay(utcZone).toInstant().toEpochMilli()

        // In New York (UTC-4 in Oct):
        val nyLocalStart = utcDateMillisToLocalStartOfDay(oct25Utc, nyZone)
        val nyLocalDate = java.time.Instant.ofEpochMilli(nyLocalStart).atZone(nyZone).toLocalDate()
        nyLocalDate.toString() shouldBe "2026-10-25"
        localStartOfDayToUtcDateMillis(nyLocalStart, nyZone) shouldBe oct25Utc

        // In Tokyo (UTC+9):
        val tokyoLocalStart = utcDateMillisToLocalStartOfDay(oct25Utc, tokyoZone)
        val tokyoLocalDate = java.time.Instant.ofEpochMilli(tokyoLocalStart).atZone(tokyoZone).toLocalDate()
        tokyoLocalDate.toString() shouldBe "2026-10-25"
        localStartOfDayToUtcDateMillis(tokyoLocalStart, tokyoZone) shouldBe oct25Utc
    }

    "streak continuity is preserved across 25-hour DST fall-back transition (BUG-133)" {
        val originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        try {
            // Nov 1, 2026 is DST fall-back day in America/New_York (25 hours)
            val fallBackMidnight = Calendar.getInstance().apply {
                set(2026, Calendar.NOVEMBER, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val nextDayMidnight = startOfNextLocalDay(fallBackMidnight)
            // Yesterday's start is 25 hours earlier (90,000,000 ms)
            val previousDayMidnight = startOfPreviousLocalDay(nextDayMidnight)
            nextDayMidnight - previousDayMidnight shouldBe 25 * 3600_000L

            // User active early yesterday at 00:30 AM
            val earlyYesterdayActive = previousDayMidnight + 30 * 60_000L
            val isNewDay = earlyYesterdayActive < nextDayMidnight

            // Buggy check with 86_400_000L fails on 25h day:
            val buggyConsecutive = earlyYesterdayActive >= nextDayMidnight - 86_400_000L && isNewDay
            buggyConsecutive shouldBe false

            // Canonical fix with startOfPreviousLocalDay succeeds:
            val fixedConsecutive = earlyYesterdayActive >= startOfPreviousLocalDay(nextDayMidnight) && isNewDay
            fixedConsecutive shouldBe true
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }
})
