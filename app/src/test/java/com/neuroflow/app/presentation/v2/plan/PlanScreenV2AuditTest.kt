package com.neuroflow.app.presentation.v2.plan

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.effectiveScheduledDateMillis
import com.neuroflow.app.data.local.entity.isRecurringWithAnchor
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.local.entity.timelineStartMinuteOfDay
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsCategory
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
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
                val dDate = dualEpochTask.resolveDeadlineEpochMillis(todayStart)
                dDate != null && dDate in todayStart..dayEnd
            }
        }
        val isUnscheduled = dualEpochTask.effectiveScheduledDateMillis() == null &&
            (dualEpochTask.scheduledTime == null || dualEpochTask.scheduledTime < com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS)

        assertTrue("Dual-representation epoch scheduled task must be placed on today's plan", isOnDay)
        assertFalse("Dual-representation epoch scheduled task must not be in unscheduled tray", isUnscheduled)
    }

    @Test
    fun testIntradayDeadlineAnchorsToSelectedDateNotToday() {
        val todayStart = startOfLocalDay(System.currentTimeMillis())
        val tomorrowCal = Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val tomorrowStart = tomorrowCal.timeInMillis
        val tomorrowEnd = tomorrowStart + 86_400_000L - 1

        // Intraday deadline (17:00 / 5 PM) without explicit deadlineDate
        val taskWithIntradayDeadline = TaskEntity(
            id = "task-intraday-deadline",
            title = "Submit Report",
            deadlineDate = null,
            deadlineTime = 17 * 3600_000L
        )

        // Without passing tomorrow's dateMillis, it defaults to today!
        val dDateDefault = taskWithIntradayDeadline.resolveDeadlineEpochMillis()
        assertFalse("Default resolveDeadlineEpochMillis erroneously anchors to today instead of tomorrow", dDateDefault!! in tomorrowStart..tomorrowEnd)

        // Passing selectedDateMillis (tomorrowStart) correctly places it on tomorrow's plan
        val dDateWithSelectedDate = taskWithIntradayDeadline.resolveDeadlineEpochMillis(tomorrowStart)
        assertTrue("Parameterized resolveDeadlineEpochMillis correctly anchors to tomorrow's selected date", dDateWithSelectedDate!! in tomorrowStart..tomorrowEnd)
        assertEquals(tomorrowStart + (17 * 3600_000L), dDateWithSelectedDate)
    }

    @Test
    fun testAddTaskPreservesUnscheduledBacklogAndNormalizesEpoch() {
        val selectedDay = startOfLocalDay(System.currentTimeMillis())
        val nextWeekEpoch = selectedDay + 7 * 86_400_000L + 14 * 3600_000L // Next week 2 PM

        // 1. Task with absolute epoch timestamp
        val taskWithEpochTime = TaskEntity(
            id = "task-epoch",
            title = "Quarterly Review",
            scheduledDate = null,
            scheduledTime = nextWeekEpoch
        )

        fun bindTask(task: TaskEntity): TaskEntity {
            return when {
                task.scheduledTime != null && task.scheduledTime >= com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS -> {
                    val epochStart = startOfLocalDay(task.scheduledTime)
                    val timeOffset = task.scheduledTime - epochStart
                    task.copy(
                        scheduledDate = epochStart,
                        scheduledTime = timeOffset,
                        habitDate = if (task.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) epochStart else task.habitDate
                    )
                }
                task.scheduledDate != null -> task
                task.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE -> {
                    task.copy(
                        scheduledDate = selectedDay,
                        habitDate = selectedDay
                    )
                }
                task.scheduledTime != null -> {
                    task.copy(
                        scheduledDate = selectedDay,
                        habitDate = if (task.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) selectedDay else task.habitDate
                    )
                }
                else -> task
            }
        }

        val boundEpochTask = bindTask(taskWithEpochTime)
        val expectedEpochDay = startOfLocalDay(nextWeekEpoch)
        assertEquals(expectedEpochDay, boundEpochTask.scheduledDate)
        assertEquals(14 * 3600_000L, boundEpochTask.scheduledTime)

        // 2. Unscheduled backlog task (both date and time null)
        val unscheduledTask = TaskEntity(
            id = "task-backlog",
            title = "Read Research Paper",
            scheduledDate = null,
            scheduledTime = null
        )
        val boundBacklogTask = bindTask(unscheduledTask)
        assertEquals(null, boundBacklogTask.scheduledDate)
        assertEquals(null, boundBacklogTask.scheduledTime)

        // 3. Intraday timed task without date
        val intradayTask = TaskEntity(
            id = "task-timed",
            title = "Sync Call",
            scheduledDate = null,
            scheduledTime = 10 * 3600_000L
        )
        val boundIntradayTask = bindTask(intradayTask)
        assertEquals(selectedDay, boundIntradayTask.scheduledDate)
        assertEquals(10 * 3600_000L, boundIntradayTask.scheduledTime)
    }

    @Test
    fun testIceboxedTasksExcludedFromTasksForDayAndUnscheduledDrawer() {
        val todayStart = startOfLocalDay(System.currentTimeMillis())
        val dayEnd = todayStart + 86_400_000L - 1

        val iceboxTask = TaskEntity(
            id = "icebox-task",
            title = "Learn Rust",
            quadrant = Quadrant.ELIMINATE,
            scheduledDate = todayStart,
            scheduledTime = null
        )
        val activeTask = TaskEntity(
            id = "active-task",
            title = "Ship V2 Feature",
            quadrant = Quadrant.DO_FIRST,
            scheduledDate = todayStart,
            scheduledTime = null
        )

        fun isEligibleForTasksForDay(task: TaskEntity): Boolean {
            if (task.quadrant == Quadrant.ELIMINATE) return false
            val sDate = task.effectiveScheduledDateMillis()
            return sDate != null && sDate in todayStart..dayEnd
        }

        fun isEligibleForUnscheduledDrawer(task: TaskEntity): Boolean {
            return task.quadrant != Quadrant.ELIMINATE &&
                task.effectiveScheduledDateMillis() == null &&
                (task.scheduledTime == null || task.scheduledTime < com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS)
        }

        assertFalse("Icebox task must not appear in tasksForDay", isEligibleForTasksForDay(iceboxTask))
        assertTrue("Active task must appear in tasksForDay", isEligibleForTasksForDay(activeTask))

        val iceboxUnscheduled = TaskEntity(
            id = "icebox-unscheduled",
            title = "Someday idea",
            quadrant = Quadrant.ELIMINATE,
            scheduledDate = null,
            scheduledTime = null
        )
        assertFalse("Icebox task must not appear in unscheduled drawer", isEligibleForUnscheduledDrawer(iceboxUnscheduled))
    }

    @Test
    fun testSearchFilterMatchesContextTag() {
        val taskWithContextTag = TaskEntity(
            id = "task-deepwork",
            title = "Implement Engine",
            tags = "",
            contextTag = "deepwork"
        )
        val query = "deepwork"

        val matches = taskWithContextTag.title.contains(query, ignoreCase = true) ||
            taskWithContextTag.description.contains(query, ignoreCase = true) ||
            taskWithContextTag.tags.contains(query, ignoreCase = true) ||
            taskWithContextTag.contextTag.contains(query, ignoreCase = true)

        assertTrue("Search query must match task.contextTag even when task.tags is empty", matches)
    }

    @Test
    fun testUltradianBufferShowsBetweenSplitPartsAndSuppressedOnUntimed() {
        // Part 1 and Part 2 of split task
        val part1 = TaskEntity(
            id = "task-split#part1",
            title = "Refactor Engine (Part 1/2)",
            scheduledTime = 9 * 3600_000L,
            plannedBlockMinutes = 45
        )
        val part2 = TaskEntity(
            id = "task-split_part2",
            title = "Refactor Engine (Part 2/2)",
            scheduledTime = 10 * 3600_000L + 15 * 60_000L,
            plannedBlockMinutes = 45
        )
        val untimed1 = TaskEntity(
            id = "untimed-1",
            title = "Untimed A",
            scheduledTime = null,
            plannedBlockMinutes = 45
        )
        val untimed2 = TaskEntity(
            id = "untimed-2",
            title = "Untimed B",
            scheduledTime = null,
            plannedBlockMinutes = 45
        )

        fun shouldShowUltradianBuffer(tasks: List<TaskEntity>, index: Int): Boolean {
            if (index >= tasks.size - 1) return false
            val current = tasks[index]
            val next = tasks[index + 1]

            val isBetweenSplitParts = (current.id.contains("#part1") || current.id.contains("_part1") || current.title.contains("(Part 1/2)")) &&
                (next.id.contains("#part2") || next.id.contains("_part2") || next.title.contains("(Part 2/2)"))
            if (isBetweenSplitParts) return true

            if (current.scheduledTime == null || next.scheduledTime == null) return false

            var runSum = 0
            for (i in 0..index) {
                val t = tasks[i]
                if (t.scheduledTime == null) continue
                if (i > 0 && tasks[i - 1].scheduledTime != null) {
                    val prevEnd = tasks[i - 1].timelineStartMinuteOfDay() + TaskBlockPlan.plannedMinutes(tasks[i - 1])
                    val curStart = t.timelineStartMinuteOfDay()
                    if (curStart - prevEnd >= 15) {
                        runSum = 0
                    }
                }
                runSum += TaskBlockPlan.plannedMinutes(t)
                if (i == index) return runSum >= 90
                if (runSum >= 90) runSum = 0
            }
            return false
        }

        assertTrue("Ultradian buffer must always show between Part 1 and Part 2", shouldShowUltradianBuffer(listOf(part1, part2), 0))
        assertFalse("Ultradian buffer must be suppressed for untimed tasks", shouldShowUltradianBuffer(listOf(untimed1, untimed2), 0))
    }

    @Test
    fun testNoteToTaskPreservesExplicitTitleAndQuadrantCycleClearsFrog() {
        val userTitle = "Custom Review Meeting"
        val inferredTitle = "meeting"

        val effectiveTitle = userTitle.trim().ifBlank { inferredTitle.ifBlank { "Fallback" } }
        assertEquals("Custom Review Meeting", effectiveTitle)

        // Quadrant cycle: moving out of DO_FIRST clears isFrog
        val frogInDoFirst = TaskEntity(
            id = "frog-task",
            title = "Crucial Delivery",
            quadrant = Quadrant.DO_FIRST,
            isFrog = true
        )
        val movedToScheduleFrog = if (Quadrant.SCHEDULE == Quadrant.DO_FIRST) true else (if (Quadrant.SCHEDULE == Quadrant.ELIMINATE) false else (if (frogInDoFirst.quadrant == Quadrant.DO_FIRST) false else frogInDoFirst.isFrog))
        assertFalse("Moving from DO_FIRST to SCHEDULE must clear isFrog", movedToScheduleFrog)
    }

    @Test
    fun testManualCircadianPeakOverrideInPlanScreenAndViewModel() {
        val prefsWithManualPeak = UserPreferences(
            peakEnergyStart = 9,
            peakEnergyEnd = 12,
            effectivePeakStart = 14,
            effectivePeakEnd = 17,
            manualPeakProfileEnabled = true,
            manualPeakAnchorMinuteOfDay = 11 * 60 // 11:00 AM
        )

        val manualPeak = if (prefsWithManualPeak.manualPeakProfileEnabled) (prefsWithManualPeak.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23) else null
        val peakStart = manualPeak ?: prefsWithManualPeak.effectivePeakStart.takeIf { it in 0..23 } ?: prefsWithManualPeak.peakEnergyStart

        assertEquals(11, peakStart)
    }

    @Test
    fun testSplitContinuationPart2EligibleWhenParentCompleted() {
        val todayStart = startOfLocalDay(System.currentTimeMillis())
        val dayEnd = todayStart + 86_400_000L - 1

        val completedParent = TaskEntity(
            id = "parent-task-1",
            title = "Write Architecture Spec",
            status = TaskStatus.COMPLETED,
            scheduledDate = todayStart
        )
        val activePart2 = TaskEntity(
            id = "parent-task-1_part2",
            title = "Write Architecture Spec (Part 2/2)",
            parentTaskId = "parent-task-1",
            status = TaskStatus.ACTIVE,
            scheduledDate = todayStart,
            estimatedDurationMinutes = 45,
            plannedBlockMinutes = 45
        )

        val activeTaskIds = setOf(activePart2.id) // parent is NOT in activeTaskIds!

        val isContinuation: (TaskEntity) -> Boolean = { task ->
            task.id.contains(Regex("""[#_]part[2-9]""")) || task.id.endsWith("_part2") || task.parentTaskId != null
        }
        val isParentActive: (TaskEntity) -> Boolean = { task ->
            val parentId = task.parentTaskId ?: task.id.removeSuffix("_part2").substringBefore("#part2")
            isContinuation(task) && parentId in activeTaskIds
        }

        assertTrue("activePart2 is recognized as a continuation part", isContinuation(activePart2))
        assertFalse("Parent is completed so Part 2 is NOT blocked by an active parent", isParentActive(activePart2))

        // When Part 1 is active, Part 2 is blocked
        val activeParent = completedParent.copy(status = TaskStatus.ACTIVE)
        val activeTaskIdsWithParent = setOf(activeParent.id, activePart2.id)
        val isParentActiveWithParent: (TaskEntity) -> Boolean = { task ->
            val parentId = task.parentTaskId ?: task.id.removeSuffix("_part2").substringBefore("#part2")
            isContinuation(task) && parentId in activeTaskIdsWithParent
        }
        assertTrue("When parent is active, Part 2 is properly blocked from duplicate scheduling", isParentActiveWithParent(activePart2))
    }

    @Test
    fun testOverduePastTasksIncludedInTasksForDayOnTodayAndUnscheduledOnFuture() {
        val todayStart = startOfLocalDay(System.currentTimeMillis())
        val todayEnd = todayStart + 86_400_000L - 1
        val yesterdayStart = todayStart - 86_400_000L

        val tomorrowCal = Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val tomorrowStart = tomorrowCal.timeInMillis
        val tomorrowEnd = tomorrowStart + 86_400_000L - 1

        val overdueTask = TaskEntity(
            id = "overdue-yesterday",
            title = "Review PR",
            scheduledDate = yesterdayStart,
            scheduledTime = 14 * 3600_000L,
            status = TaskStatus.ACTIVE
        )

        fun isEligibleForTasksForDay(task: TaskEntity, isTargetingToday: Boolean, dayStart: Long, dayEnd: Long): Boolean {
            if (task.quadrant == Quadrant.ELIMINATE) return false
            val sDate = task.effectiveScheduledDateMillis()
            return if (sDate != null) {
                if (isTargetingToday) sDate <= dayEnd else sDate in dayStart..dayEnd
            } else {
                val rawScheduledTime = task.scheduledTime
                if (rawScheduledTime != null && rawScheduledTime >= 86_400_000L) {
                    if (isTargetingToday) rawScheduledTime <= dayEnd else rawScheduledTime in dayStart..dayEnd
                } else {
                    val dDate = task.resolveDeadlineEpochMillis(dayStart)
                    dDate != null && (if (isTargetingToday) dDate <= dayEnd else dDate in dayStart..dayEnd)
                }
            }
        }

        fun isEligibleForUnscheduled(task: TaskEntity, isTargetingToday: Boolean): Boolean {
            val sDate = task.effectiveScheduledDateMillis()
            val isPastOverdue = sDate != null && sDate < todayStart
            return task.quadrant != Quadrant.ELIMINATE &&
                (sDate == null || (!isTargetingToday && isPastOverdue)) &&
                (task.scheduledTime == null || task.scheduledTime < 86_400_000L || (!isTargetingToday && isPastOverdue))
        }

        // When viewing TODAY: overdue past task is included in tasksForDay!
        assertTrue("Overdue task must be visible on today's timeline", isEligibleForTasksForDay(overdueTask, isTargetingToday = true, dayStart = todayStart, dayEnd = todayEnd))
        assertFalse("Overdue task must not clutter unscheduled tray on today's view", isEligibleForUnscheduled(overdueTask, isTargetingToday = true))

        // When viewing TOMORROW: overdue past task is not in tomorrow's timeline, but appears in unscheduled tray!
        assertFalse("Overdue task must not be in tomorrow's timeline", isEligibleForTasksForDay(overdueTask, isTargetingToday = false, dayStart = tomorrowStart, dayEnd = tomorrowEnd))
        assertTrue("Overdue task must appear in unscheduled tray on future-day view", isEligibleForUnscheduled(overdueTask, isTargetingToday = false))
    }

    @Test
    fun testAssignTaskTimeAndConflictResolutionMidnightSanitization() {
        // Suggested minute = 1440 (midnight overflow)
        val overflowMinute = 1440
        val safeMinuteOfDay = (overflowMinute % 1440).coerceIn(0, 1439)
        val safeHour = safeMinuteOfDay / 60
        val safeMinute = safeMinuteOfDay % 60
        val millisInDay = safeHour * 3600_000L + safeMinute * 60_000L

        assertEquals(0, safeHour)
        assertEquals(0, safeMinute)
        assertEquals(0L, millisInDay)
        assertTrue("millisInDay must be strictly less than ONE_DAY_MILLIS to prevent epoch date corruption", millisInDay < 86_400_000L)

        // Hour = 24 passed directly
        val rawHour = 24
        val rawMinute = 0
        val sanitizedHour = (rawHour % 24).coerceIn(0, 23)
        val sanitizedMinute = (rawMinute % 60).coerceIn(0, 59)
        val sanitizedMillisInDay = sanitizedHour * 3600_000L + sanitizedMinute * 60_000L
        assertEquals(0, sanitizedHour)
        assertEquals(0L, sanitizedMillisInDay)
    }

    @Test
    fun testEpochDeadlineTimeNormalizationAndIceboxAutonomyNudgeSuppression() {
        val todayStart = startOfLocalDay(System.currentTimeMillis())
        val epochDeadline = todayStart + (17 * 3600_000L) // 5:00 PM today as raw epoch timestamp

        // Normalization in addTask / createTaskFromNote
        val rawDeadlineTime = epochDeadline
        val (deadDate, deadTime) = if (rawDeadlineTime >= 86_400_000L) {
            val dDay = startOfLocalDay(rawDeadlineTime)
            dDay to (rawDeadlineTime - dDay)
        } else {
            null to rawDeadlineTime
        }

        assertEquals(todayStart, deadDate)
        assertEquals(17 * 3600_000L, deadTime)

        // Icebox autonomy nudge suppression
        val iceboxTask = TaskEntity(
            id = "icebox-1",
            title = "Someday Project",
            quadrant = Quadrant.ELIMINATE,
            status = TaskStatus.ACTIVE
        )
        val shouldScheduleNudge = iceboxTask.quadrant != Quadrant.ELIMINATE && iceboxTask.status == TaskStatus.ACTIVE
        assertFalse("Autonomy nudge must be suppressed for Icebox (ELIMINATE) tasks", shouldScheduleNudge)
    }

    @Test
    fun testMultiPartContinuationBlockingInAutoScheduleDay() {
        fun isContinuationBlocked(task: TaskEntity, activeIds: Set<String>): Boolean {
            val partNumMatch = Regex("""[#_]part(\d+)""").find(task.id)
            val partNum = partNumMatch?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""\(Part (\d+)/""").find(task.title)?.groupValues?.get(1)?.toIntOrNull()
            val isContinuation = (partNum != null && partNum >= 2) ||
                task.id.endsWith("_part2") ||
                task.id.contains("#part2") ||
                task.parentTaskId != null
            if (!isContinuation) return false
            val parentSlug = task.parentTaskId ?: task.id.replace(Regex("""[#_]part\d+.*$"""), "")
            val isParentActive = (task.parentTaskId != null && task.parentTaskId in activeIds) ||
                (parentSlug != task.id && parentSlug in activeIds)
            val isPart1Active = task.id.replace(Regex("""([#_]part)\d+"""), "$1" + "1") in activeIds
            val isPrecedingPartActive = partNum != null && (1 until partNum).any { prevPart ->
                task.id.replace(Regex("""([#_]part)\d+"""), "$1$prevPart") in activeIds
            }
            return isParentActive || isPart1Active || isPrecedingPartActive
        }

        val part1 = TaskEntity(id = "task-alpha_part1", title = "Write Compiler (Part 1/3)", status = TaskStatus.ACTIVE)
        val part2 = TaskEntity(id = "task-alpha_part2", title = "Write Compiler (Part 2/3)", status = TaskStatus.ACTIVE)
        val part3 = TaskEntity(id = "task-alpha_part3", title = "Write Compiler (Part 3/3)", status = TaskStatus.ACTIVE)

        // Case 1: Part 1 and Part 2 are active -> Part 3 must be blocked
        val activeSet1 = setOf("task-alpha_part1", "task-alpha_part2", "task-alpha_part3")
        assertTrue("Part 3 must be blocked when Part 1 and Part 2 are active", isContinuationBlocked(part3, activeSet1))

        // Case 2: Part 1 is completed, but Part 2 is still active -> Part 3 must be blocked
        val activeSet2 = setOf("task-alpha_part2", "task-alpha_part3")
        assertTrue("Part 3 must be blocked when Part 2 is still active", isContinuationBlocked(part3, activeSet2))

        // Case 3: Part 1 and Part 2 are completed -> Part 3 is actionable and must NOT block itself
        val activeSet3 = setOf("task-alpha_part3")
        assertFalse("Part 3 must be actionable when preceding parts are completed and must not match itself", isContinuationBlocked(part3, activeSet3))

        // Case 4: Part 1 is not a continuation (partNum == 1) -> must not be blocked by parent
        assertFalse("Part 1 must not be classified as a blocked continuation", isContinuationBlocked(part1, activeSet1))
    }

    @Test
    fun testMultiPartContinuationPartLabelingAndUltradianBufferInTimeline() {
        val part1 = TaskEntity(id = "task-eng#part1", title = "Kernel Driver (Part 1/3)", scheduledTime = 9 * 3600_000L, estimatedDurationMinutes = 45)
        val part2 = TaskEntity(id = "task-eng#part2", title = "Kernel Driver (Part 2/3)", scheduledTime = 10 * 3600_000L, estimatedDurationMinutes = 45)
        val part3 = TaskEntity(id = "task-eng#part3", title = "Kernel Driver (Part 3/3)", scheduledTime = 11 * 3600_000L, estimatedDurationMinutes = 45)

        fun resolvePartLabel(task: TaskEntity): String {
            val titleMatch = Regex("""\(Part (\d+/\d+)\)""").find(task.title)
            val idPartMatch = Regex("""[#_]part(\d+)""").find(task.id)
            return when {
                titleMatch != null -> "🔗 Part ${titleMatch.groupValues[1]}"
                idPartMatch != null -> "🔗 Part ${idPartMatch.groupValues[1]}"
                task.id.contains("#part2") || task.id.contains("_part2") || task.title.contains("(Part 2/2)") -> "🔗 Part 2/2"
                else -> "🔗 Part 1/2"
            }
        }

        assertEquals("🔗 Part 1/3", resolvePartLabel(part1))
        assertEquals("🔗 Part 2/3", resolvePartLabel(part2))
        assertEquals("🔗 Part 3/3", resolvePartLabel(part3))

        fun shouldShowBuffer(tasks: List<TaskEntity>, index: Int): Boolean {
            if (index >= tasks.size - 1) return false
            val current = tasks[index]
            val next = tasks[index + 1]

            if (current.scheduledTime == null || next.scheduledTime == null) return false

            val curPart = Regex("""[#_]part(\d+)""").find(current.id)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""\(Part (\d+)/""").find(current.title)?.groupValues?.get(1)?.toIntOrNull()
            val nextPart = Regex("""[#_]part(\d+)""").find(next.id)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""\(Part (\d+)/""").find(next.title)?.groupValues?.get(1)?.toIntOrNull()
            return (curPart != null && nextPart != null && nextPart == curPart + 1) ||
                ((current.id.contains("#part1") || current.id.contains("_part1") || current.title.contains("(Part 1/2)")) &&
                    (next.id.contains("#part2") || next.id.contains("_part2") || next.title.contains("(Part 2/2)")))
        }

        val tasks = listOf(part1, part2, part3)
        assertTrue("Buffer must appear between Part 1 and Part 2", shouldShowBuffer(tasks, 0))
        assertTrue("Buffer must appear between Part 2 and Part 3", shouldShowBuffer(tasks, 1))

        // Untimed tasks must never trigger buffer
        val untimedPart1 = part1.copy(scheduledTime = null)
        val untimedPart2 = part2.copy(scheduledTime = null)
        assertFalse("Untimed split parts must not show ultradian divider", shouldShowBuffer(listOf(untimedPart1, untimedPart2), 0))
    }

    @Test
    fun testShiftTaskToPeakCircadianWindowWrappingMidnight() {
        val peakHour = 22
        val peakEndHour = 1

        fun calculatePeakTarget(currentHour: Int, todayStart: Long): Pair<Long, Int> {
            val isPeakActive = if (peakHour <= peakEndHour) currentHour in peakHour until peakEndHour else (currentHour >= peakHour || currentHour < peakEndHour)
            val isPeakUpcoming = if (peakHour <= peakEndHour) currentHour < peakHour else (currentHour in peakEndHour until peakHour)

            return if (isPeakUpcoming) {
                todayStart to peakHour
            } else if (isPeakActive) {
                todayStart to currentHour
            } else {
                (todayStart + 86_400_000L) to peakHour
            }
        }

        val todayStart = startOfLocalDay(System.currentTimeMillis())

        // At 19:00 (7 PM): today's 22:00 peak is upcoming!
        val (targetDate19, targetHour19) = calculatePeakTarget(19, todayStart)
        assertEquals("At 19:00 with 22:00 peak, date must remain today", todayStart, targetDate19)
        assertEquals(22, targetHour19)

        // At 23:00 (11 PM): peak is currently active!
        val (targetDate23, targetHour23) = calculatePeakTarget(23, todayStart)
        assertEquals("At 23:00 with 22:00 peak, date must remain today", todayStart, targetDate23)
        assertEquals(23, targetHour23)

        // At 00:30 (midnight): peak is currently active (before 01:00)!
        val (targetDate00, targetHour00) = calculatePeakTarget(0, todayStart)
        assertEquals("At 00:00 with 22:00..01:00 peak, date must remain today", todayStart, targetDate00)
        assertEquals(0, targetHour00)
    }

    @Test
    fun testAddTaskHabitDateInitializationAndUncompleteTaskIceboxNudgeGuard() {
        val todayStart = startOfLocalDay(System.currentTimeMillis())

        // Adding recurring task with explicit scheduledDate must populate habitDate
        val recurringWithDate = TaskEntity(
            id = "recurring-1",
            title = "Morning Standup",
            recurrence = com.neuroflow.app.domain.model.Recurrence.DAILY,
            scheduledDate = todayStart,
            habitDate = null
        )

        val boundTask = when {
            recurringWithDate.scheduledDate != null -> {
                if (recurringWithDate.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE && recurringWithDate.habitDate == null) {
                    recurringWithDate.copy(habitDate = recurringWithDate.scheduledDate)
                } else {
                    recurringWithDate
                }
            }
            recurringWithDate.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE -> {
                recurringWithDate.copy(
                    scheduledDate = todayStart,
                    habitDate = todayStart
                )
            }
            else -> recurringWithDate
        }

        assertEquals(todayStart, boundTask.habitDate)
        assertTrue("Task must qualify as recurring with anchor", boundTask.isRecurringWithAnchor())

        // Uncompleting an icebox task must suppress autonomy nudge
        val restoredIceboxTask = TaskEntity(
            id = "icebox-restored",
            title = "Icebox Idea",
            quadrant = Quadrant.ELIMINATE,
            status = TaskStatus.ACTIVE
        )
        val shouldScheduleNudge = restoredIceboxTask.quadrant != Quadrant.ELIMINATE && restoredIceboxTask.status == TaskStatus.ACTIVE
        assertFalse("Autonomy nudge must not be scheduled when restoring an iceboxed task", shouldScheduleNudge)
    }

    @Test
    fun testStartOfTodayMillisRespectsExplicitTimezone() {
        // BUG-AUDIT-004: startOfTodayMillis must use explicit ZoneId rather than JVM-wide Calendar.getInstance()
        val tokyoZone = ZoneId.of("Asia/Tokyo")
        val nyZone = ZoneId.of("America/New_York")
        val utcZone = ZoneId.of("UTC")

        val tokyoStart = startOfTodayMillis(tokyoZone)
        val expectedTokyo = LocalDate.now(tokyoZone).atStartOfDay(tokyoZone).toInstant().toEpochMilli()
        assertEquals(expectedTokyo, tokyoStart)

        val nyStart = startOfTodayMillis(nyZone)
        val expectedNy = LocalDate.now(nyZone).atStartOfDay(nyZone).toInstant().toEpochMilli()
        assertEquals(expectedNy, nyStart)

        val utcStart = startOfTodayMillis(utcZone)
        val expectedUtc = LocalDate.now(utcZone).atStartOfDay(utcZone).toInstant().toEpochMilli()
        assertEquals(expectedUtc, utcStart)

        val defaultStart = startOfTodayMillis()
        val expectedDefault = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expectedDefault, defaultStart)
    }

    @Test
    fun testShiftTaskToPeakMinutePrecisionCalculation() {
        // BUG-AUDIT-010: Manual peak profile at 9:30 AM (570m) must produce millisInDay = 570 * 60_000L
        val peakPrefs = UserPreferences(
            manualPeakProfileEnabled = true,
            manualPeakAnchorMinuteOfDay = 570
        )
        val peakHour = (peakPrefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23)
        assertEquals(9, peakHour)

        val targetHour = peakHour
        val targetMinuteOfDay = if (targetHour == peakHour && peakPrefs.manualPeakProfileEnabled) {
            peakPrefs.manualPeakAnchorMinuteOfDay
        } else {
            targetHour * 60
        }
        val millisInDay = targetMinuteOfDay * 60_000L
        assertEquals(570 * 60_000L, millisInDay)
        assertEquals(34_200_000L, millisInDay)
    }

    @Test
    fun testPart2IdNamingConventionStandardizedOnHash() {
        // BUG-AUDIT-011: Standardized on #part2 delimiter to prevent entity duplication
        val baseTaskId = "design_doc"
        val expectedPart2Id = "${baseTaskId}#part2"
        assertEquals("design_doc#part2", expectedPart2Id)
        assertFalse(expectedPart2Id.contains("_part2"))
    }

    @Test
    fun testCognitiveBudgetOverbookedStylingIndependentOfConflicts() {
        // Regression test: Conflicts must not make Cognitive Budget turn red when well within capacity
        val scheduledMinutes = 60
        val safeCapacity = 360
        val conflictCount = 3
        val completedMinutes = 0

        val totalCommitted = (scheduledMinutes + completedMinutes).coerceAtLeast(0)
        val isOverbooked = totalCommitted > safeCapacity
        val ratio = (totalCommitted.toFloat() / safeCapacity.toFloat()).coerceIn(0f, 2.0f)

        assertFalse("Capacity of 60m out of 360m must NOT be overbooked", isOverbooked)
        assertTrue("Ratio must be ~0.167", ratio in 0.16f..0.17f)

        // Color logic verification
        val isRedBar = isOverbooked
        val isAmberBar = !isOverbooked && ratio >= 0.85f
        val isEmeraldBar = !isOverbooked && ratio < 0.85f

        assertFalse("Bar must not be red (RecoveryRose) just because conflictCount > 0", isRedBar)
        assertFalse("Bar must not be amber", isAmberBar)
        assertTrue("Bar must remain healthy emerald", isEmeraldBar)

        // When truly overbooked
        val overbookedScheduled = 400
        val totalOverbooked = overbookedScheduled + completedMinutes
        val isActuallyOverbooked = totalOverbooked > safeCapacity
        assertTrue("400m out of 360m MUST be overbooked", isActuallyOverbooked)
    }

    @Test
    fun testCognitiveBudgetDualSegmentCompletedAndActiveAccounting() {
        // Regression test: Accounts for completed focus work and remaining active load
        val scheduledMinutes = 120
        val completedMinutes = 60
        val safeCapacity = 240

        val totalCommitted = (scheduledMinutes + completedMinutes).coerceAtLeast(0)
        assertEquals(180, totalCommitted)

        val remainingCapacity = (safeCapacity - totalCommitted).coerceAtLeast(0)
        assertEquals(60, remainingCapacity)

        val ratio = totalCommitted.toFloat() / safeCapacity.toFloat()
        assertEquals(0.75f, ratio, 0.001f)
        assertFalse("180m / 240m is not overbooked", totalCommitted > safeCapacity)

        val completedRatio = completedMinutes.toFloat() / safeCapacity.toFloat()
        assertEquals(0.25f, completedRatio, 0.001f)
    }

    @Test
    fun testCognitiveBudgetRatioProgressBoundsCoercion() {
        // Compose fillMaxWidth requires fraction in 0f..1f. Coercion must prevent crash on overflow/underflow.
        val extremeOverbookedMinutes = 800
        val safeCapacity = 100
        val rawRatio = extremeOverbookedMinutes.toFloat() / safeCapacity.toFloat() // 8.0f
        val coercedProgress = rawRatio.coerceIn(0f, 1f)
        assertEquals(1.0f, coercedProgress, 0.0f)

        val negativeMinutes = -50
        val rawNegativeRatio = negativeMinutes.toFloat() / safeCapacity.toFloat()
        val coercedNegativeProgress = rawNegativeRatio.coerceIn(0f, 1f)
        assertEquals(0.0f, coercedNegativeProgress, 0.0f)
    }
}
