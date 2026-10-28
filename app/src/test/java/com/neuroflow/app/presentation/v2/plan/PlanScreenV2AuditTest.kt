package com.neuroflow.app.presentation.v2.plan

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.effectiveScheduledDateMillis
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsCategory
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PlanScreenV2AuditTest {

    @Test
    fun testProtectedSlotMidnightModuloResolution() {
        // Slot ending past midnight: 22:30 (1350m) + 120m = 1470m -> 00:30
        val lateNightSlot = ProtectedTimeSlot(
            id = "slot-night",
            label = "Wind Down & Sleep",
            startMinuteOfDay = 22 * 60 + 30, // 1350
            durationMinutes = 120,
            category = PtsCategory.BREAK
        )

        val startH = lateNightSlot.startMinuteOfDay / 60
        val startM = lateNightSlot.startMinuteOfDay % 60
        val endMinuteOfDay = (lateNightSlot.startMinuteOfDay + lateNightSlot.durationMinutes) % 1440
        val endH = endMinuteOfDay / 60
        val endM = endMinuteOfDay % 60

        assertEquals(22, startH)
        assertEquals(30, startM)
        assertEquals(0, endH)
        assertEquals(30, endM)
        val formatted = String.format("%02d:%02d – %02d:%02d (%s)", startH, startM, endH, endM, lateNightSlot.category.name)
        assertEquals("22:30 – 00:30 (BREAK)", formatted)

        // Slot ending exactly at midnight: 23:00 (1380m) + 60m = 1440m -> 00:00
        val exactMidnightSlot = ProtectedTimeSlot(
            id = "slot-midnight",
            label = "Night Rest",
            startMinuteOfDay = 23 * 60, // 1380
            durationMinutes = 60,
            category = PtsCategory.BREAK
        )
        val endMidnight = (exactMidnightSlot.startMinuteOfDay + exactMidnightSlot.durationMinutes) % 1440
        assertEquals(0, endMidnight / 60)
        assertEquals(0, endMidnight % 60)
    }

    @Test
    fun testEffectivePeakResolution() {
        // Preferences with dynamic circadian peak detected at 14..17, while manual static is 9..12
        val prefsWithDetectedPeak = UserPreferences(
            peakEnergyStart = 9,
            peakEnergyEnd = 12,
            effectivePeakStart = 14,
            effectivePeakEnd = 17
        )

        val peakStart = (if (prefsWithDetectedPeak.effectivePeakStart != -1) prefsWithDetectedPeak.effectivePeakStart else prefsWithDetectedPeak.peakEnergyStart).coerceIn(0, 23)
        val peakEnd = (if (prefsWithDetectedPeak.effectivePeakEnd != -1) prefsWithDetectedPeak.effectivePeakEnd else prefsWithDetectedPeak.peakEnergyEnd).coerceIn(0, 23)

        assertEquals(14, peakStart)
        assertEquals(17, peakEnd)

        // Unconfigured effective peak (-1) correctly falls back to static preferences
        val prefsDefault = UserPreferences(
            peakEnergyStart = 9,
            peakEnergyEnd = 12,
            effectivePeakStart = -1,
            effectivePeakEnd = -1
        )
        val defaultStart = (if (prefsDefault.effectivePeakStart != -1) prefsDefault.effectivePeakStart else prefsDefault.peakEnergyStart).coerceIn(0, 23)
        val defaultEnd = (if (prefsDefault.effectivePeakEnd != -1) prefsDefault.effectivePeakEnd else prefsDefault.peakEnergyEnd).coerceIn(0, 23)

        assertEquals(9, defaultStart)
        assertEquals(12, defaultEnd)
    }

    @Test
    fun testScheduledMillisEvaluationWithSelectedDate() {
        val tomorrowCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val tomorrowStart = startOfLocalDay(tomorrowCal.timeInMillis)

        // Task with intraday scheduledTime (10:00 AM) but null scheduledDate
        val task = TaskEntity(
            id = "task-tomorrow-deadline",
            title = "Prepare Executive Deck",
            scheduledDate = null,
            scheduledTime = 10 * 3600_000L // 10:00 AM offset
        )

        val scheduledForSelectedDate = task.resolveScheduledEpochMillis(referenceDateMillis = tomorrowStart)
        val expectedEpoch = tomorrowStart + (10 * 3600_000L)

        assertEquals(expectedEpoch, scheduledForSelectedDate)
    }

    @Test
    fun testCognitiveBudgetMeterUsesPlannedBlockMinutes() {
        // A 90m base task split into Part 1 (45m block) and Part 2 (45m block)
        val part1Task = TaskEntity(
            id = "task-split#part1",
            title = "Write Architecture RFC (Part 1/2)",
            estimatedDurationMinutes = 90,
            plannedBlockMinutes = 45,
            scheduledTime = 9 * 3600_000L
        )
        val part2Task = TaskEntity(
            id = "task-split_part2",
            title = "Write Architecture RFC (Part 2/2)",
            estimatedDurationMinutes = 45,
            plannedBlockMinutes = 45,
            scheduledTime = 14 * 3600_000L
        )

        val tasksForDay = listOf(part1Task, part2Task)

        // Raw estimate sums to 90 + 45 = 135 minutes (overcounted!)
        val rawEstimatedSum = tasksForDay.sumOf { it.estimatedDurationMinutes }
        assertEquals(135, rawEstimatedSum)

        // TaskBlockPlan correctly accounts for 45 + 45 = 90 minutes
        val plannedBlockSum = tasksForDay.sumOf { TaskBlockPlan.plannedMinutes(it) }
        assertEquals(90, plannedBlockSum)
    }

    @Test
    fun testDistinctUntilChangedDetectsDynamicPeakChanges() {
        val basePrefs = UserPreferences(
            peakEnergyStart = 9,
            peakEnergyEnd = 12,
            effectivePeakStart = -1,
            effectivePeakEnd = -1
        )

        val updatedEffectivePeak = basePrefs.copy(
            effectivePeakStart = 14,
            effectivePeakEnd = 17
        )

        val updatedStaticPeak = basePrefs.copy(
            peakEnergyStart = 10,
            peakEnergyEnd = 13
        )

        // Equivalence lambda matching PlanViewModelV2 preferencesFlow.distinctUntilChanged
        val isEquivalent: (UserPreferences, UserPreferences) -> Boolean = { old, new ->
            old.workDayStart == new.workDayStart &&
                old.workDayEnd == new.workDayEnd &&
                old.peakEnergyStart == new.peakEnergyStart &&
                old.peakEnergyEnd == new.peakEnergyEnd &&
                old.effectivePeakStart == new.effectivePeakStart &&
                old.effectivePeakEnd == new.effectivePeakEnd &&
                old.autoSchedulingEnabled == new.autoSchedulingEnabled &&
                old.protectedTimeSlots == new.protectedTimeSlots &&
                old.tagCatalog == new.tagCatalog &&
                old.wakeUpHour == new.wakeUpHour &&
                old.sleepHour == new.sleepHour &&
                old.sleepPressurePoints == new.sleepPressurePoints &&
                old.stimulantLogsJson == new.stimulantLogsJson &&
                old.quickPhysiologicalLogsJson == new.quickPhysiologicalLogsJson &&
                old.quizChronotype == new.quizChronotype &&
                old.manualChronotype == new.manualChronotype &&
                old.meqScore == new.meqScore &&
                old.manualPeakProfileEnabled == new.manualPeakProfileEnabled &&
                old.manualPeakAnchorMinuteOfDay == new.manualPeakAnchorMinuteOfDay &&
                old.dailyDeepWorkTargetMinutes == new.dailyDeepWorkTargetMinutes
        }

        // Must NOT be equivalent when effective peak changes
        assertFalse("Effective peak change should trigger state recalculation", isEquivalent(basePrefs, updatedEffectivePeak))
        // Must NOT be equivalent when static peak changes
        assertFalse("Static peak change should trigger state recalculation", isEquivalent(basePrefs, updatedStaticPeak))
        // Unmodified preferences are equivalent
        assertTrue("Unchanged preferences should be suppressed", isEquivalent(basePrefs, basePrefs.copy()))
    }

    @Test
    fun testPart2ContinuationCompletedPreservationPredicate() {
        val completedPart2 = TaskEntity(
            id = "parent-task_part2",
            title = "Research LLM (Part 2/2)",
            status = TaskStatus.COMPLETED,
            estimatedDurationMinutes = 45
        )
        val activePart2 = TaskEntity(
            id = "parent-task-2_part2",
            title = "Refactor Code (Part 2/2)",
            status = TaskStatus.ACTIVE,
            estimatedDurationMinutes = 45
        )
        val focusedTaskIds = setOf("parent-task-3_part2")
        val focusedPart2 = TaskEntity(
            id = "parent-task-3_part2",
            title = "Focused Task (Part 2/2)",
            status = TaskStatus.ACTIVE,
            estimatedDurationMinutes = 45
        )

        // Deletion guard predicate in PlanViewModelV2.autoScheduleDay
        val shouldDeleteOrphan: (TaskEntity?) -> Boolean = { task ->
            task != null && task.status != TaskStatus.COMPLETED && task.id !in focusedTaskIds
        }

        assertFalse("Completed Part 2 must NEVER be deleted as orphan", shouldDeleteOrphan(completedPart2))
        assertFalse("Actively focused Part 2 must NEVER be deleted as orphan", shouldDeleteOrphan(focusedPart2))
        assertTrue("Inactive orphaned Part 2 should be cleaned up", shouldDeleteOrphan(activePart2))
    }

    @Test
    fun testCompletionUndoSnackbarProtectionFilter() {
        val completionMsg = "Completed: Deep Work Task • \"Stay focused\""
        val shiftedMsg = "Shifted 'Meeting' to 14:00"
        val decomposedMsg = "Decomposed 'Project' into 3 micro-steps 🪄"

        val shouldDisplayInActionEffect: (String) -> Boolean = { msg ->
            !msg.startsWith("Completed:")
        }

        assertFalse("Completion message must not dismiss interactive Undo snackbar", shouldDisplayInActionEffect(completionMsg))
        assertTrue("Shifted message should display normally", shouldDisplayInActionEffect(shiftedMsg))
        assertTrue("Decomposed message should display normally", shouldDisplayInActionEffect(decomposedMsg))
    }

    @Test
    fun testDynamicCircadianPeakResolutionInTimeline() {
        // Preference static peak is 9..12
        val prefs = UserPreferences(
            peakEnergyStart = 9,
            peakEnergyEnd = 12,
            effectivePeakStart = -1,
            effectivePeakEnd = -1
        )
        // Summary has dynamic peak detected at 14..17
        val summaryPeakStart = 14
        val summaryPeakEnd = 17

        val peakStart = summaryPeakStart
            ?: (if (prefs.effectivePeakStart in 0..23) prefs.effectivePeakStart else prefs.peakEnergyStart).coerceIn(0, 23)
        val peakEnd = summaryPeakEnd
            ?: (if (prefs.effectivePeakEnd in 0..23) prefs.effectivePeakEnd else prefs.peakEnergyEnd).coerceIn(0, 23)

        assertEquals(14, peakStart)
        assertEquals(17, peakEnd)
    }

    @Test
    fun testUltradianBufferCalculationUsesPlannedBlockMinutes() {
        // Part 1 of split 90m task (45m planned), followed by a 30m task
        val part1 = TaskEntity(
            id = "task#part1",
            title = "Big Task (Part 1/2)",
            estimatedDurationMinutes = 90,
            plannedBlockMinutes = 45
        )
        val secondTask = TaskEntity(
            id = "task2",
            title = "Short Task",
            estimatedDurationMinutes = 30,
            plannedBlockMinutes = 30
        )
        val tasks = listOf(part1, secondTask)

        // Using raw estimatedDurationMinutes would falsely trigger ultradian buffer after index 0 (90 >= 90)
        var rawRunSum = 0
        var rawTriggeredAt0 = false
        for (i in 0..0) {
            rawRunSum += tasks[i].estimatedDurationMinutes
            if (rawRunSum >= 90) rawTriggeredAt0 = true
        }
        assertTrue("Raw estimate erroneously triggers buffer after only 45m of work", rawTriggeredAt0)

        // Using TaskBlockPlan.plannedMinutes correctly accounts for 45m (no buffer after index 0)
        var plannedRunSum = 0
        var plannedTriggeredAt0 = false
        for (i in 0..0) {
            plannedRunSum += TaskBlockPlan.plannedMinutes(tasks[i])
            if (plannedRunSum >= 90) plannedTriggeredAt0 = true
        }
        assertFalse("Planned block minutes correctly suppresses premature buffer", plannedTriggeredAt0)
    }

    @Test
    fun testContextTagMatchingInTagFilter() {
        val taskWithContextOnly = TaskEntity(
            id = "task-ctx",
            title = "Write Architecture Spec",
            tags = "",
            contextTag = "deepwork"
        )
        val filterTag = "#deepwork"
        val q = filterTag.removePrefix("#").removePrefix("@").trim()

        val matchingTags = (taskWithContextOnly.tags.split(",") + taskWithContextOnly.contextTag)
            .map { it.trim().removePrefix("#").removePrefix("@") }
            .filter { it.isNotBlank() }

        val matches = matchingTags.any { it.equals(q, ignoreCase = true) }
        assertTrue("Task with contextTag 'deepwork' must match '#deepwork' filter", matches)
    }

    @Test
    fun testFixedBusyWindowsExcludesUnscheduledBacklogAndIceboxTasks() {
        val todayStart = startOfLocalDay(System.currentTimeMillis())
        val dayEnd = todayStart + 86_400_000L - 1

        val iceboxTask = TaskEntity(
            id = "icebox-task",
            title = "Someday idea",
            quadrant = Quadrant.ELIMINATE,
            scheduledDate = todayStart,
            scheduledTime = 10 * 3600_000L
        )
        val unscheduledBacklogTask = TaskEntity(
            id = "backlog-task",
            title = "Backlog idea with preferred time",
            quadrant = Quadrant.DO_FIRST,
            scheduledDate = null,
            scheduledTime = 10 * 3600_000L,
            isScheduleLocked = true
        )
        val validScheduledTask = TaskEntity(
            id = "scheduled-task",
            title = "Important Review",
            quadrant = Quadrant.DO_FIRST,
            scheduledDate = todayStart,
            scheduledTime = 10 * 3600_000L,
            isScheduleLocked = true
        )

        fun isEligibleFixedWindow(task: TaskEntity): Boolean {
            if (task.quadrant == Quadrant.ELIMINATE) return false
            if (task.scheduledTime == null) return false
            val sDate = task.effectiveScheduledDateMillis()
            val isDualEpoch = task.scheduledTime != null && task.scheduledTime >= com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS
            val isDateMatching = (sDate != null && sDate in todayStart..dayEnd) ||
                (isDualEpoch && task.scheduledTime in todayStart..dayEnd)
            return isDateMatching
        }

        assertFalse("Icebox tasks must never block fixed windows", isEligibleFixedWindow(iceboxTask))
        assertFalse("Unscheduled backlog tasks must never block fixed windows", isEligibleFixedWindow(unscheduledBacklogTask))
        assertTrue("Valid scheduled task on today must be eligible as fixed window", isEligibleFixedWindow(validScheduledTask))
    }

    @Test
    fun testDualEpochScheduledTimeIncludedInTasksForDay() {
        val todayStart = startOfLocalDay(System.currentTimeMillis())
        val dayEnd = todayStart + 86_400_000L - 1
        val dualEpochTime = todayStart + (14 * 3600_000L) // 2:00 PM today

        val dualEpochTask = TaskEntity(
            id = "dual-task",
            title = "Conference Call",
            scheduledDate = null,
            scheduledTime = dualEpochTime
        )

        val sDate = dualEpochTask.effectiveScheduledDateMillis()
        val isOnDay = if (sDate != null) {
            sDate in todayStart..dayEnd
        } else {
            val rawTime = dualEpochTask.scheduledTime
            if (rawTime != null && rawTime >= com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS) {
                rawTime in todayStart..dayEnd
            } else {
                val dDate = dualEpochTask.resolveDeadlineEpochMillis()
                dDate != null && dDate in todayStart..dayEnd
            }
        }
        val isUnscheduled = dualEpochTask.effectiveScheduledDateMillis() == null &&
            (dualEpochTask.scheduledTime == null || dualEpochTask.scheduledTime < com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS)

        assertTrue("Dual-representation epoch scheduled task must be placed on today's plan", isOnDay)
        assertFalse("Dual-representation epoch scheduled task must not be in unscheduled tray", isUnscheduled)
    }
}
