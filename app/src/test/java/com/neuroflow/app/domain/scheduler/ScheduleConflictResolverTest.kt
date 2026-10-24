package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.TaskStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar

class ScheduleConflictResolverTest : StringSpec({

    fun scheduledTask(
        id: String,
        title: String,
        minuteOfDay: Int,
        durationMinutes: Int
    ): TaskEntity {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        return TaskEntity(
            id = id,
            title = title,
            scheduledDate = startOfDay,
            scheduledTime = minuteOfDay * 60_000L,
            estimatedDurationMinutes = durationMinutes,
            status = TaskStatus.ACTIVE
        )
    }

    "analyzeScheduleConflicts: returns empty list when schedule is conflict-free" {
        val task1 = scheduledTask("1", "Morning Review", minuteOfDay = 9 * 60, durationMinutes = 45) // 9:00 - 9:45
        val task2 = scheduledTask("2", "Deep Coding", minuteOfDay = 10 * 60, durationMinutes = 60)  // 10:00 - 11:00
        val protectedSlots = listOf(
            ProtectedTimeSlot(id = "p1", label = "Lunch", startMinuteOfDay = 12 * 60, durationMinutes = 60, isEnabled = true)
        )

        val conflicts = ConflictResolver.analyzeScheduleConflicts(
            tasks = listOf(task1, task2),
            protectedSlots = protectedSlots
        )

        conflicts shouldBe emptyList()
    }

    "analyzeScheduleConflicts: detects collision with protected slot and suggests shifting past slot end" {
        // Task scheduled 12:30 - 13:30 (750m - 810m)
        val task = scheduledTask("t1", "Client Call", minuteOfDay = 12 * 60 + 30, durationMinutes = 60)
        // Lunch protected slot: 12:00 - 13:00 (720m - 780m)
        val lunchSlot = ProtectedTimeSlot(
            id = "lunch",
            label = "Lunch Break",
            startMinuteOfDay = 12 * 60,
            durationMinutes = 60,
            isEnabled = true
        )

        val conflicts = ConflictResolver.analyzeScheduleConflicts(
            tasks = listOf(task),
            protectedSlots = listOf(lunchSlot),
            workDayStart = 9,
            workDayEnd = 17
        )

        conflicts.size shouldBe 1
        val conflict = conflicts.first()
        conflict.taskId shouldBe "t1"
        conflict.conflictingTargetName shouldBe "Lunch Break"
        conflict.conflictStartMinute shouldBe 750 // 12:30
        conflict.conflictEndMinute shouldBe 780   // 13:00
        conflict.suggestedNewStartMinute shouldBe 780 // 13:00 (end of lunch slot)
    }

    "analyzeScheduleConflicts: ignores disabled protected slots" {
        val task = scheduledTask("t1", "Meeting", minuteOfDay = 12 * 60 + 30, durationMinutes = 60)
        val disabledSlot = ProtectedTimeSlot(
            id = "disabled",
            label = "Old Slot",
            startMinuteOfDay = 12 * 60,
            durationMinutes = 60,
            isEnabled = false
        )

        val conflicts = ConflictResolver.analyzeScheduleConflicts(
            tasks = listOf(task),
            protectedSlots = listOf(disabledSlot)
        )

        conflicts shouldBe emptyList()
    }

    "analyzeScheduleConflicts: detects task vs task overlap and suggests moving overlapping task" {
        // Task A: 10:00 - 11:00 (600m - 660m)
        val taskA = scheduledTask("a", "Feature Spec", minuteOfDay = 10 * 60, durationMinutes = 60)
        // Task B: 10:30 - 11:30 (630m - 710m)
        val taskB = scheduledTask("b", "Code Review", minuteOfDay = 10 * 60 + 30, durationMinutes = 60)

        val conflicts = ConflictResolver.analyzeScheduleConflicts(
            tasks = listOf(taskA, taskB),
            protectedSlots = emptyList(),
            workDayStart = 9,
            workDayEnd = 18
        )

        conflicts.size shouldBe 1
        val conflict = conflicts.first()
        conflict.taskId shouldBe "b"
        conflict.conflictingTargetName shouldBe "Feature Spec"
        conflict.conflictStartMinute shouldBe 630 // 10:30
        conflict.conflictEndMinute shouldBe 660   // 11:00
        conflict.suggestedNewStartMinute shouldBe 660 // 11:00 (after Task A finishes)
    }

    "analyzeScheduleConflicts: suggested start minute is clamped within work day end bounds" {
        // Task scheduled 16:30 - 17:30 (990m - 1050m) overlapping with a slot ending at 17:00
        val task = scheduledTask("late", "Evening Wrapup", minuteOfDay = 16 * 60 + 30, durationMinutes = 60)
        val protectedSlot = ProtectedTimeSlot(
            id = "slot",
            label = "Review Hour",
            startMinuteOfDay = 16 * 60,
            durationMinutes = 60, // ends at 17:00 (1020m)
            isEnabled = true
        )

        val conflicts = ConflictResolver.analyzeScheduleConflicts(
            tasks = listOf(task),
            protectedSlots = listOf(protectedSlot),
            workDayStart = 9,
            workDayEnd = 17 // workDayEnd * 60 = 1020m. With slot ending at 17:00, 0 minutes left before workDayEnd
        )

        conflicts.size shouldBe 1
        val conflict = conflicts.first()
        // ADR-024 Bug 4: Must return null instead of clamping backward into the conflicting interval
        conflict.suggestedNewStartMinute shouldBe null
    }

    "analyzeScheduleConflicts: filters out protected slots that do not occur on the selected day" {
        // Monday calendar day: 2026-10-19
        val mondayCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 19) // Monday
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val mondayMillis = mondayCal.timeInMillis

        // Task on Monday at 12:30
        val mondayTask = TaskEntity(
            id = "t_mon",
            title = "Monday Sync",
            scheduledDate = mondayMillis,
            scheduledTime = (12 * 60 + 30) * 60_000L,
            estimatedDurationMinutes = 60,
            status = TaskStatus.ACTIVE
        )

        // Weekend-only protected slot (Saturday & Sunday)
        val weekendSlot = ProtectedTimeSlot(
            id = "weekend_slot",
            label = "Weekend Family Time",
            startMinuteOfDay = 12 * 60,
            durationMinutes = 60,
            recurrence = com.neuroflow.app.domain.model.PtsRecurrence.WEEKENDS,
            isEnabled = true
        )

        val conflicts = ConflictResolver.analyzeScheduleConflicts(
            tasks = listOf(mondayTask),
            protectedSlots = listOf(weekendSlot),
            dayMillis = mondayMillis
        )

        // Because it's Monday and slot is WEEKENDS only, no conflict should be flagged
        conflicts shouldBe emptyList()
    }
})
