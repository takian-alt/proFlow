package com.neuroflow.app.presentation.common

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsCategory
import com.neuroflow.app.domain.model.PtsRecurrence
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.util.Calendar

class DrawerWeeklyPlanSectionTest : StringSpec({

    "getWeekDays should return exactly 7 days starting from Monday" {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 2, 10, 0, 0) // Friday Oct 2, 2026
            set(Calendar.MILLISECOND, 0)
        }
        val days = getWeekDays(cal.timeInMillis)
        days shouldHaveSize 7

        val mondayCal = Calendar.getInstance().apply { timeInMillis = days.first() }
        mondayCal.get(Calendar.DAY_OF_WEEK) shouldBe Calendar.MONDAY
        mondayCal.get(Calendar.DAY_OF_MONTH) shouldBe 28 // Monday Sep 28, 2026

        val sundayCal = Calendar.getInstance().apply { timeInMillis = days.last() }
        sundayCal.get(Calendar.DAY_OF_WEEK) shouldBe Calendar.SUNDAY
        sundayCal.get(Calendar.DAY_OF_MONTH) shouldBe 4 // Sunday Oct 4, 2026
    }

    "isSameCalendarDay should return true for timestamps on the same day" {
        val cal1 = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 2, 9, 30, 0)
        }
        val cal2 = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 2, 18, 45, 0)
        }
        val cal3 = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 3, 9, 30, 0)
        }

        isSameCalendarDay(cal1.timeInMillis, cal2.timeInMillis) shouldBe true
        isSameCalendarDay(cal1.timeInMillis, cal3.timeInMillis) shouldBe false
    }

    "isTaskOnDay should identify tasks scheduled for a specific day" {
        val dayStart = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 2, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayEnd = dayStart + 24 * 60 * 60 * 1000L

        val scheduledTask = TaskEntity(
            id = "t1",
            title = "Scheduled Task",
            scheduledDate = dayStart + 10 * 3600_000L
        )
        val otherDayTask = TaskEntity(
            id = "t2",
            title = "Tomorrow Task",
            scheduledDate = dayEnd + 3600_000L
        )

        isTaskOnDay(scheduledTask, dayStart, dayEnd) shouldBe true
        isTaskOnDay(otherDayTask, dayStart, dayEnd) shouldBe false
    }

    "isProtectedSlotOnDay should match recurrence correctly" {
        // Friday Oct 2, 2026
        val friday = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 2, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Saturday Oct 3, 2026
        val saturday = friday + 24 * 60 * 60 * 1000L

        val weekdaySlot = ProtectedTimeSlot(
            id = "s1",
            label = "Work Block",
            startMinuteOfDay = 540,
            durationMinutes = 60,
            recurrence = PtsRecurrence.WEEKDAYS,
            category = PtsCategory.WORK
        )

        val weekendSlot = ProtectedTimeSlot(
            id = "s2",
            label = "Gym",
            startMinuteOfDay = 600,
            durationMinutes = 90,
            recurrence = PtsRecurrence.WEEKENDS,
            category = PtsCategory.EXERCISE
        )

        isProtectedSlotOnDay(weekdaySlot, friday) shouldBe true
        isProtectedSlotOnDay(weekdaySlot, saturday) shouldBe false

        isProtectedSlotOnDay(weekendSlot, friday) shouldBe false
        isProtectedSlotOnDay(weekendSlot, saturday) shouldBe true
    }
})
