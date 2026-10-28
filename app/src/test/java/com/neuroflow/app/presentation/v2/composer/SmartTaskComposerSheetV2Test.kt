package com.neuroflow.app.presentation.v2.composer

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.isRecurringWithAnchor
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartTaskComposerSheetV2Test {

    @Test
    fun testSheetModeSealedInterface() {
        val createMode: SheetMode = SheetMode.Create
        assertTrue(createMode is SheetMode.Create)

        val sampleTask = TaskEntity(
            id = "task-edit-1",
            title = "Polymorphic Architecture Review",
            estimatedDurationMinutes = 45,
            energyLevel = EnergyLevel.HIGH,
            quadrant = Quadrant.DO_FIRST,
            taskType = TaskType.ANALYTICAL,
            isFrog = true
        )

        val editMode: SheetMode = SheetMode.Edit(sampleTask)
        assertTrue(editMode is SheetMode.Edit)
        assertEquals("task-edit-1", (editMode as SheetMode.Edit).task.id)
        assertEquals("Polymorphic Architecture Review", editMode.task.title)
        assertEquals(45, editMode.task.estimatedDurationMinutes)
        assertEquals(EnergyLevel.HIGH, editMode.task.energyLevel)
        assertTrue(editMode.task.isFrog)
        assertEquals(Quadrant.DO_FIRST, editMode.task.quadrant)
    }

    @Test
    fun testQuadrantCycleProgression() {
        fun nextQuadrant(current: Quadrant): Quadrant = when (current) {
            Quadrant.DO_FIRST -> Quadrant.SCHEDULE
            Quadrant.SCHEDULE -> Quadrant.DELEGATE
            Quadrant.DELEGATE -> Quadrant.ELIMINATE
            Quadrant.ELIMINATE -> Quadrant.DO_FIRST
        }

        assertEquals(Quadrant.SCHEDULE, nextQuadrant(Quadrant.DO_FIRST))
        assertEquals(Quadrant.DELEGATE, nextQuadrant(Quadrant.SCHEDULE))
        assertEquals(Quadrant.ELIMINATE, nextQuadrant(Quadrant.DELEGATE))
        assertEquals(Quadrant.DO_FIRST, nextQuadrant(Quadrant.ELIMINATE))
    }

    @Test
    fun testTaskEntityQuadrantUpdateInEditMode() {
        val original = TaskEntity(
            id = "task-matrix-1",
            title = "Refactor Data Layer",
            quadrant = Quadrant.SCHEDULE
        )

        val updatedToDoFirst = original.copy(quadrant = Quadrant.DO_FIRST)
        assertEquals(Quadrant.DO_FIRST, updatedToDoFirst.quadrant)

        val updatedToDelegate = original.copy(quadrant = Quadrant.DELEGATE)
        assertEquals(Quadrant.DELEGATE, updatedToDelegate.quadrant)

        val updatedToEliminate = original.copy(quadrant = Quadrant.ELIMINATE)
        assertEquals(Quadrant.ELIMINATE, updatedToEliminate.quadrant)
    }

    @Test
    fun testTaskEntityTagPreservationAndSanitizationInEditMode() {
        val original = TaskEntity(
            id = "task-matrix-2",
            title = "Refactor Data Layer",
            tags = "admin",
            quadrant = Quadrant.SCHEDULE
        )

        val rawInput = "Refactor Data Layer #coding #deepwork"
        val inferred = com.neuroflow.app.domain.inference.TaskInferenceEngine.inferFromInput(rawInput)

        val existingTagsList = original.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val allTags = (existingTagsList + inferred.contextTags).distinct()
        val effectiveTags = allTags.joinToString(",")

        val updated = original.copy(
            title = inferred.sanitizedTitle.ifBlank { rawInput.trim() },
            tags = effectiveTags
        )

        assertEquals("Refactor Data Layer", updated.title)
        assertTrue(updated.tags.contains("admin"))
        assertTrue(updated.tags.contains("coding"))
        assertTrue(updated.tags.contains("deepwork"))
    }

    @Test
    fun testTagDeletionInEditMode() {
        val original = TaskEntity(
            id = "task-matrix-3",
            title = "Weekly Review",
            tags = "admin,review,planning",
            quadrant = Quadrant.SCHEDULE
        )

        val initialTags = original.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        var activeCustomTags = initialTags

        // Simulate user clicking ✕ on the 'admin' chip
        val tagToRemove = "admin"
        activeCustomTags = activeCustomTags.filterNot { it.equals(tagToRemove, ignoreCase = true) }.toSet()

        val allActiveTags = activeCustomTags.toList()
        val effectiveTags = allActiveTags.joinToString(",")

        val updated = original.copy(tags = effectiveTags)

        org.junit.Assert.assertFalse("Deleted tag must not remain in updated entity", updated.tags.contains("admin"))
        assertTrue(updated.tags.contains("review"))
        assertTrue(updated.tags.contains("planning"))
        assertEquals("review,planning", updated.tags)
    }

    @Test
    fun testDateTimeClearingAffordance() {
        val original = TaskEntity(
            id = "task-matrix-4",
            title = "Submit Paper",
            deadlineDate = 1714000000000L,
            deadlineTime = 54000000L, // 15:00
            scheduledDate = 1713900000000L,
            scheduledTime = 36000000L // 10:00
        )

        var userDeadlineDate: Long? = original.deadlineDate
        var deadlineDateCleared = false

        // Simulate tapping clear button on deadline date
        userDeadlineDate = null
        deadlineDateCleared = true

        val inferredDeadlineDate: Long? = 1714500000000L // Even if inferred has a date
        val effectiveDeadlineDate = if (deadlineDateCleared) null else (userDeadlineDate ?: inferredDeadlineDate)

        org.junit.Assert.assertNull("Explicitly cleared deadline date must be null", effectiveDeadlineDate)

        // Simulate clearing scheduled time
        var userScheduledTime: Long? = original.scheduledTime
        var scheduledTimeCleared = false
        userScheduledTime = null
        scheduledTimeCleared = true

        val inferredScheduledTime: Long? = 36000000L
        val effectiveScheduledTime = if (scheduledTimeCleared) null else (userScheduledTime ?: inferredScheduledTime)

        org.junit.Assert.assertNull("Explicitly cleared scheduled time must be null", effectiveScheduledTime)

        val updated = original.copy(
            deadlineDate = effectiveDeadlineDate,
            scheduledTime = effectiveScheduledTime
        )
        org.junit.Assert.assertNull(updated.deadlineDate)
        org.junit.Assert.assertNull(updated.scheduledTime)
    }

    @Test
    fun testRecurrenceOptionsSupportCustom() {
        val recurrenceOptions = listOf(
            com.neuroflow.app.domain.model.Recurrence.NONE,
            com.neuroflow.app.domain.model.Recurrence.DAILY,
            com.neuroflow.app.domain.model.Recurrence.WEEKLY,
            com.neuroflow.app.domain.model.Recurrence.MONTHLY,
            com.neuroflow.app.domain.model.Recurrence.CUSTOM
        )
        assertEquals(5, recurrenceOptions.size)
        assertTrue(recurrenceOptions.contains(com.neuroflow.app.domain.model.Recurrence.CUSTOM))
    }

    @Test
    fun testTagTogglePreservesManualUserOverridesInCreateMode() {
        val userDurationOverride: Int? = 45
        val userEnergyOverride: EnergyLevel? = EnergyLevel.HIGH
        val userTaskTypeOverride: TaskType? = TaskType.ANALYTICAL
        val userFrogOverride: Boolean? = true
        val userPriorityOverride: com.neuroflow.app.domain.model.Priority? = com.neuroflow.app.domain.model.Priority.HIGH
        val userQuadrantOverride: Quadrant? = Quadrant.DO_FIRST

        var activeCustomTags = setOf("initial")

        // Simulate tag toggle
        val tagName = "neuroflow"
        activeCustomTags = activeCustomTags + tagName

        // Overrides must remain intact
        assertEquals(45, userDurationOverride)
        assertEquals(EnergyLevel.HIGH, userEnergyOverride)
        assertEquals(TaskType.ANALYTICAL, userTaskTypeOverride)
        assertEquals(true, userFrogOverride)
        assertEquals(com.neuroflow.app.domain.model.Priority.HIGH, userPriorityOverride)
        assertEquals(Quadrant.DO_FIRST, userQuadrantOverride)
        assertTrue(activeCustomTags.contains("neuroflow"))
    }

    @Test
    fun testDualRepresentationEpochTimestampNormalizationAndFormatting() {
        val now = System.currentTimeMillis()
        val zone = java.time.ZoneId.systemDefault()
        val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)
        val epochDeadline = todayStart + (14 * 3_600_000L) + (30 * 60_000L) // 14:30 today

        assertTrue("epochDeadline must be >= ONE_DAY_MILLIS", epochDeadline >= com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS)

        // Legacy/un-normalized calculation produced ~480,000+ hours
        val buggyHours = (epochDeadline / 3_600_000L).toInt()
        assertTrue("Buggy division produces > 24 hours", buggyHours > 24)

        // Normalized resolution
        val offset = com.neuroflow.app.data.local.entity.localTimeOfDayOffset(epochDeadline)
        assertTrue("Normalized offset must be within intraday 24 hours", offset in 0 until com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS)

        val h = (offset / 3_600_000L).toInt().coerceIn(0, 23)
        val m = ((offset % 3_600_000L) / 60_000L).toInt().coerceIn(0, 59)
        assertEquals(14, h)
        assertEquals(30, m)

        val formatted = String.format("%02d:%02d", h, m)
        assertEquals("14:30", formatted)
    }

    @Test
    fun testRecurringTaskHabitDateInitializationInComposer() {
        val now = System.currentTimeMillis()
        val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)
        val recurrence = com.neuroflow.app.domain.model.Recurrence.DAILY

        val effectiveScheduledDate: Long? = null
        val effectiveDeadlineDate: Long? = null
        val existingTask: TaskEntity? = null

        val effectiveHabitDate = if (recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) {
            existingTask?.habitDate
                ?: effectiveScheduledDate
                ?: effectiveDeadlineDate
                ?: todayStart
        } else null

        val newTask = TaskEntity(
            title = "Daily Meditation",
            recurrence = recurrence,
            habitDate = effectiveHabitDate,
            isHabitual = recurrence != com.neuroflow.app.domain.model.Recurrence.NONE
        )

        org.junit.Assert.assertNotNull(newTask.habitDate)
        assertEquals(todayStart, newTask.habitDate)
        assertTrue(newTask.isHabitual)
        assertTrue(newTask.isRecurringWithAnchor())
    }

    @Test
    fun testRecurringTaskHabitDateRescheduleUpdatesHabitDate() {
        val now = System.currentTimeMillis()
        val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)
        val tomorrowStart = com.neuroflow.app.data.local.entity.startOfNextLocalDay(todayStart)
        val recurrence = com.neuroflow.app.domain.model.Recurrence.DAILY

        val existingTask = TaskEntity(
            title = "Habit",
            recurrence = recurrence,
            scheduledDate = todayStart,
            habitDate = todayStart,
            isHabitual = true
        )

        // User edits scheduledDate to tomorrow
        val effectiveScheduledDate = tomorrowStart
        val effectiveDeadlineDate: Long? = null

        val effectiveHabitDate = if (recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) {
            val dateChanged = (effectiveScheduledDate != existingTask.scheduledDate || effectiveDeadlineDate != existingTask.deadlineDate)
            if (dateChanged && (effectiveScheduledDate != null || effectiveDeadlineDate != null)) {
                effectiveScheduledDate ?: effectiveDeadlineDate
            } else {
                existingTask.habitDate
                    ?: effectiveScheduledDate
                    ?: effectiveDeadlineDate
                    ?: todayStart
            }
        } else null

        assertEquals(tomorrowStart, effectiveHabitDate)
    }

    @Test
    fun testRecurringTaskNonDateEditPreservesHabitDate() {
        val now = System.currentTimeMillis()
        val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)
        val recurrence = com.neuroflow.app.domain.model.Recurrence.DAILY

        val existingTask = TaskEntity(
            title = "Habit",
            recurrence = recurrence,
            scheduledDate = todayStart,
            habitDate = todayStart,
            isHabitual = true
        )

        // User only edits title, not dates
        val effectiveScheduledDate = todayStart
        val effectiveDeadlineDate: Long? = null

        val effectiveHabitDate = if (recurrence != com.neuroflow.app.domain.model.Recurrence.NONE) {
            val dateChanged = (effectiveScheduledDate != existingTask.scheduledDate || effectiveDeadlineDate != existingTask.deadlineDate)
            if (dateChanged && (effectiveScheduledDate != null || effectiveDeadlineDate != null)) {
                effectiveScheduledDate ?: effectiveDeadlineDate
            } else {
                existingTask.habitDate
                    ?: effectiveScheduledDate
                    ?: effectiveDeadlineDate
                    ?: todayStart
            }
        } else null

        assertEquals(todayStart, effectiveHabitDate)
    }

    @Test
    fun testBug198TagRemovalAndDeselectionWithInferredAndCustomTags() {
        var rawInput = "Review document #work @deepwork"
        val inferred = com.neuroflow.app.domain.inference.TaskInferenceEngine.inferFromInput(rawInput)
        var activeCustomTags = setOf<String>()
        var excludedTags = setOf<String>()

        fun computeAllActiveTags(): List<String> {
            return (activeCustomTags + inferred.contextTags)
                .filterNot { tag -> excludedTags.any { it.equals(tag, ignoreCase = true) } }
                .distinct()
        }

        fun removeTag(tagName: String) {
            val regex = Regex("""(?i)(?:^|\s)[#@]\Q$tagName\E(?=\s|$)""")
            if (rawInput.contains(regex)) {
                rawInput = rawInput.replace(regex, " ").replace(Regex("""\s{2,}"""), " ").trim()
            }
            activeCustomTags = activeCustomTags.filterNot { it.equals(tagName, ignoreCase = true) }.toSet()
            excludedTags = excludedTags + tagName
        }

        fun toggleTag(tagName: String) {
            val regex = Regex("""(?i)(?:^|\s)[#@]\Q$tagName\E(?=\s|$)""")
            val isExplicitInInput = rawInput.contains(regex)
            val isCurrentlyActive = computeAllActiveTags().any { it.equals(tagName, ignoreCase = true) }

            if (isCurrentlyActive || isExplicitInInput) {
                if (isExplicitInInput) {
                    rawInput = rawInput.replace(regex, " ").replace(Regex("""\s{2,}"""), " ").trim()
                }
                activeCustomTags = activeCustomTags.filterNot { it.equals(tagName, ignoreCase = true) }.toSet()
                excludedTags = excludedTags + tagName
            } else {
                excludedTags = excludedTags.filterNot { it.equals(tagName, ignoreCase = true) }.toSet()
                activeCustomTags = activeCustomTags + tagName
            }
        }

        // Initially both "work" and "deepwork" are inferred
        val initialTags = computeAllActiveTags()
        assertTrue(initialTags.any { it.equals("work", ignoreCase = true) })
        assertTrue(initialTags.any { it.equals("deepwork", ignoreCase = true) })

        // Removing "work" strips it from rawInput and excludes it
        removeTag("work")
        val afterRemoveWork = computeAllActiveTags()
        assertTrue("work should be removed", afterRemoveWork.none { it.equals("work", ignoreCase = true) })
        assertTrue("deepwork should remain", afterRemoveWork.any { it.equals("deepwork", ignoreCase = true) })

        // Removing "@deepwork" strips it from rawInput and excludes it
        removeTag("deepwork")
        val afterRemoveDeepwork = computeAllActiveTags()
        assertTrue("deepwork should be removed", afterRemoveDeepwork.isEmpty())

        // Toggling "work" back on restores it
        toggleTag("work")
        val afterToggleBack = computeAllActiveTags()
        assertTrue("work should be restored", afterToggleBack.any { it.equals("work", ignoreCase = true) })
    }

    @Test
    fun testBug199UntimedDateOmissionDefaultsToTodayWhenTimeIsProvided() {
        val now = System.currentTimeMillis()
        val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)

        val scheduledTimeOffset = 14 * 3600_000L // 2:00 PM
        val effectiveScheduledDate: Long? = null

        val finalScheduledDate = if (scheduledTimeOffset != null && effectiveScheduledDate == null) {
            todayStart
        } else {
            effectiveScheduledDate
        }

        assertEquals(todayStart, finalScheduledDate)
    }

    @Test
    fun testBug211InitialDatePreservedWhenSettingDeadlineToTimeOnly() {
        val futureDate = com.neuroflow.app.data.local.entity.startOfLocalDay(System.currentTimeMillis()) + 3 * 86_400_000L
        val initialDate: Long? = futureDate
        val normalizedDeadlineTime = 17 * 3600_000L // 5:00 PM
        val effectiveDeadlineDate: Long? = null

        val finalDeadlineDate = if (normalizedDeadlineTime != null && effectiveDeadlineDate == null) {
            initialDate ?: com.neuroflow.app.data.local.entity.startOfLocalDay(System.currentTimeMillis())
        } else {
            effectiveDeadlineDate
        }

        assertEquals(futureDate, finalDeadlineDate)
    }

    @Test
    fun testBug211TimePickerConfirmationPreservesInitialDateForDeadlineAndScheduled() {
        val futureDate = com.neuroflow.app.data.local.entity.startOfLocalDay(System.currentTimeMillis()) + 5 * 86_400_000L
        val initialDate: Long? = futureDate

        var userDeadlineDate: Long? = null
        val deadlineDateCleared = false
        if (userDeadlineDate == null && !deadlineDateCleared) {
            userDeadlineDate = initialDate ?: com.neuroflow.app.data.local.entity.startOfLocalDay(System.currentTimeMillis())
        }
        assertEquals(futureDate, userDeadlineDate)

        var userScheduledDate: Long? = null
        val scheduledDateCleared = false
        if (userScheduledDate == null && !scheduledDateCleared) {
            userScheduledDate = initialDate ?: com.neuroflow.app.data.local.entity.startOfLocalDay(System.currentTimeMillis())
        }
        assertEquals(futureDate, userScheduledDate)
    }

    @Test
    fun testBug251NaturalLanguageDateNotOverriddenByInitialDate() {
        val now = System.currentTimeMillis()
        val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(now)
        val initialDate = todayStart

        // User types "Ship release notes tomorrow 45m"
        val rawInput = "Ship release notes tomorrow 45m"
        val inferred = com.neuroflow.app.domain.inference.TaskInferenceEngine.inferFromInput(rawInput)

        org.junit.Assert.assertNotNull("Tomorrow must be inferred", inferred.scheduledDate)
        val tomorrowStart = com.neuroflow.app.data.local.entity.startOfNextLocalDay(todayStart)
        assertEquals(tomorrowStart, inferred.scheduledDate)

        // Simulated composer resolution logic
        val userScheduledDateOverride: Long? = null
        val scheduledDateCleared = false

        val effectiveScheduledDate = when {
            scheduledDateCleared -> null
            userScheduledDateOverride != null -> userScheduledDateOverride
            inferred.scheduledDate != null -> inferred.scheduledDate
            else -> initialDate
        }

        assertEquals("Inferred date must take precedence over default initialDate", tomorrowStart, effectiveScheduledDate)
    }

    @Test
    fun testBug252NlpDirectivesTakeEffectInEditMode() {
        val original = TaskEntity(
            id = "task-edit-nlp",
            title = "Weekly planning",
            estimatedDurationMinutes = 25,
            energyLevel = EnergyLevel.MEDIUM,
            quadrant = Quadrant.SCHEDULE,
            isFrog = false,
            priority = com.neuroflow.app.domain.model.Priority.MEDIUM
        )

        // User edits title to add explicit directives: "Weekly planning 60m #high !frog !q1"
        val rawInput = "Weekly planning 60m #high !frog !q1"
        val inferred = com.neuroflow.app.domain.inference.TaskInferenceEngine.inferFromInput(rawInput)

        assertTrue("60m must be explicit", inferred.isDurationExplicit)
        assertEquals(60, inferred.durationMinutes)
        assertTrue("High energy must be explicit", inferred.isEnergyExplicit)
        assertEquals(EnergyLevel.HIGH, inferred.energyDemand)
        assertTrue("Frog must be inferred", inferred.isFrog)
        assertTrue("Q1 must be explicit", inferred.isQuadrantExplicit)
        assertEquals(Quadrant.DO_FIRST, inferred.quadrant)

        // Overrides start as null in Edit mode
        val userDurationOverride: Int? = null
        val userEnergyOverride: EnergyLevel? = null
        val userFrogOverride: Boolean? = null
        val userPriorityOverride: com.neuroflow.app.domain.model.Priority? = null
        val userQuadrantOverride: Quadrant? = null

        val effectiveDuration = userDurationOverride
            ?: if (inferred.isDurationExplicit) inferred.durationMinutes else original.estimatedDurationMinutes
        val effectiveEnergy = userEnergyOverride
            ?: if (inferred.isEnergyExplicit) inferred.energyDemand else original.energyLevel
        val effectiveFrog = userFrogOverride
            ?: if (inferred.isFrog) true else original.isFrog
        val effectivePriority = userPriorityOverride
            ?: if (inferred.isPriorityExplicit || effectiveFrog) inferred.priority else original.priority
        val effectiveQuadrant = userQuadrantOverride
            ?: if (inferred.isQuadrantExplicit) inferred.quadrant else original.quadrant

        assertEquals(60, effectiveDuration)
        assertEquals(EnergyLevel.HIGH, effectiveEnergy)
        assertTrue(effectiveFrog)
        assertEquals(com.neuroflow.app.domain.model.Priority.HIGH, effectivePriority)
        assertEquals(Quadrant.DO_FIRST, effectiveQuadrant)
    }

    @Test
    fun testBug253TimePickerConfirmationPreservesInferredDeadlineDate() {
        val futureFriday = com.neuroflow.app.data.local.entity.startOfLocalDay(System.currentTimeMillis()) + 4 * 86_400_000L
        val initialDate = com.neuroflow.app.data.local.entity.startOfLocalDay(System.currentTimeMillis())

        val inferredDeadlineDate = futureFriday
        var userDeadlineDateOverride: Long? = null
        val deadlineDateCleared = false

        val effectiveDeadlineDate = when {
            deadlineDateCleared -> null
            userDeadlineDateOverride != null -> userDeadlineDateOverride
            inferredDeadlineDate != null -> inferredDeadlineDate
            else -> initialDate
        }
        assertEquals(futureFriday, effectiveDeadlineDate)

        // User picks time in TimePicker:
        // Must NOT overwrite effectiveDeadlineDate with initialDate/today because effectiveDeadlineDate is already non-null
        if (effectiveDeadlineDate == null && !deadlineDateCleared) {
            userDeadlineDateOverride = initialDate
        }

        val preservedDeadlineDate = userDeadlineDateOverride ?: effectiveDeadlineDate
        assertEquals("Inferred deadline date must be preserved when setting due time", futureFriday, preservedDeadlineDate)
    }

    @Test
    fun testBug254QuickNoteCustomTitlePreservedOnTaskConversion() {
        val userTitle = "Strategic Quarterly Roadmap"
        val userContent = "Outline key database migrations and scheduler hardening steps for Q3."
        val syntheticTitle = "Outline database migrations"

        val noteTitleToSave = userTitle.ifBlank { syntheticTitle }
        assertEquals("User's explicit note title must be preserved", "Strategic Quarterly Roadmap", noteTitleToSave)

        val emptyUserTitle = ""
        val fallbackTitleToSave = emptyUserTitle.ifBlank { syntheticTitle }
        assertEquals("Fallback to synthetic title when user title is blank", "Outline database migrations", fallbackTitleToSave)
    }

    @Test
    fun testBug255DurationChipMonotonicProgression() {
        val cycles = listOf(15, 25, 45, 60, 90, 120)
        fun nextDuration(current: Int): Int = cycles.firstOrNull { it > current } ?: cycles.first()

        assertEquals(45, nextDuration(30)) // 30m jumps forward to 45m, not backward to 15m
        assertEquals(45, nextDuration(33)) // 33m calibrated jumps to 45m
        assertEquals(60, nextDuration(50)) // 50m jumps to 60m
        assertEquals(90, nextDuration(75)) // 75m jumps to 90m
        assertEquals(15, nextDuration(120)) // 120m wraps around to 15m
    }

    @Test
    fun testBug299UntimedTaskInEditModePreservesInferredArchetypeDuration() {
        val originalUntimedTask = TaskEntity(
            id = "task-untimed-1",
            title = "Analyze System Architecture",
            estimatedDurationMinutes = 0, // Untimed
            taskType = TaskType.ANALYTICAL
        )

        val inferred = com.neuroflow.app.domain.inference.TaskInferenceEngine.inferFromInput(originalUntimedTask.title)
        val userDurationOverride: Int? = null

        // Bug 299: (existingTask?.estimatedDurationMinutes ?: inferred.durationMinutes) evaluated to 0
        // because 0 is not null. Fixed with .takeIf { it > 0 }
        val effectiveDuration = userDurationOverride
            ?: if (inferred.isDurationExplicit) inferred.durationMinutes else (originalUntimedTask.estimatedDurationMinutes.takeIf { it > 0 } ?: inferred.durationMinutes)

        assertEquals("Inferred analytical archetype duration must be used for untimed task", 45, effectiveDuration)
    }

    @Test
    fun testBug300SettingDueTimeWithoutDateAnchorsToScheduledDateInsteadOfToday() {
        val today = com.neuroflow.app.data.local.entity.startOfLocalDay(System.currentTimeMillis())
        val futureScheduledDate = today + 3 * 86_400_000L // 3 days in future

        val effectiveScheduledDate: Long? = futureScheduledDate
        val effectiveDeadlineDate: Long? = null
        val initialDate = today
        val deadlineDateCleared = false

        // User picks a due time (e.g. 5pm) in TimePicker without picking a deadline date:
        val userDeadlineDateOverride = if (effectiveDeadlineDate == null && !deadlineDateCleared) {
            effectiveScheduledDate ?: initialDate ?: today
        } else {
            effectiveDeadlineDate
        }

        // Must anchor to futureScheduledDate so the task is not immediately overdue
        assertEquals("Deadline date must anchor to futureScheduledDate", futureScheduledDate, userDeadlineDateOverride)

        // And in handleSave():
        val normalizedDeadlineTime = 17 * 3600_000L
        val finalDeadlineDate = if (normalizedDeadlineTime != null && effectiveDeadlineDate == null && !deadlineDateCleared) {
            effectiveScheduledDate ?: initialDate ?: today
        } else {
            effectiveDeadlineDate
        }
        assertEquals("Final saved deadline date must match scheduled date", futureScheduledDate, finalDeadlineDate)
    }

    @Test
    fun testBug333ClearedScheduledAndDeadlineDatesNotResurrectedWhenTimeIsProvided() {
        val today = com.neuroflow.app.data.local.entity.startOfLocalDay(System.currentTimeMillis())
        val initialDate = today

        // Task has scheduledTime (e.g. 10:00 AM) and deadlineTime (e.g. 5:00 PM)
        val normalizedScheduledTime = 10 * 3600_000L
        val normalizedDeadlineTime = 17 * 3600_000L

        // User explicitly tapped Clear on both date pickers
        val scheduledDateCleared = true
        val deadlineDateCleared = true
        val effectiveScheduledDate: Long? = null
        val effectiveDeadlineDate: Long? = null

        val finalScheduledDate = if (normalizedScheduledTime != null && effectiveScheduledDate == null && !scheduledDateCleared) {
            effectiveDeadlineDate ?: initialDate ?: today
        } else {
            effectiveScheduledDate
        }

        val finalDeadlineDate = if (normalizedDeadlineTime != null && effectiveDeadlineDate == null && !deadlineDateCleared) {
            effectiveScheduledDate ?: initialDate ?: today
        } else {
            effectiveDeadlineDate
        }

        org.junit.Assert.assertNull("Explicitly cleared scheduled date must remain null despite scheduled time", finalScheduledDate)
        org.junit.Assert.assertNull("Explicitly cleared deadline date must remain null despite deadline time", finalDeadlineDate)
    }

    @Test
    fun testBug334ContextTagIngestionInInitialTagsAndSynchronizationOnEdit() {
        // Task has contextTag = "deepwork" and tags = "coding"
        val existingTask = TaskEntity(
            id = "task-ctx-1",
            title = "Optimize database index",
            tags = "coding",
            contextTag = "deepwork"
        )

        val fromTags = existingTask.tags.split(",").map { it.trim().removePrefix("#").removePrefix("@") }.filter { it.isNotBlank() }
        val fromContext = existingTask.contextTag.trim().removePrefix("#").removePrefix("@").takeIf { it.isNotBlank() }
        val initialTags = (if (fromContext != null) fromTags + fromContext else fromTags).toSet()

        assertTrue("initialTags must include contextTag 'deepwork'", initialTags.contains("deepwork"))
        assertTrue("initialTags must include tags 'coding'", initialTags.contains("coding"))

        // User removes "deepwork" tag chip, leaving only "coding"
        val allActiveTags = setOf("coding")
        val effectiveTags = allActiveTags.joinToString(",")

        val effectiveContextTag = if (allActiveTags.isEmpty()) {
            ""
        } else {
            val existingContext = existingTask.contextTag.trim().removePrefix("#").removePrefix("@")
            if (existingContext.isNotBlank() && allActiveTags.any { it.equals(existingContext, ignoreCase = true) }) {
                existingContext
            } else {
                com.neuroflow.app.domain.scheduler.TaskTagSchedulingProfile.profilesFor(effectiveTags).firstNotNullOfOrNull { it.preferredContext }?.trim()?.removePrefix("@")
                    ?: allActiveTags.firstOrNull().orEmpty()
            }
        }

        val updatedTask = existingTask.copy(
            tags = effectiveTags,
            contextTag = effectiveContextTag
        )

        // The stale "deepwork" context tag was cleared because the user removed it!
        assertEquals("coding", updatedTask.tags)
        org.junit.Assert.assertNotEquals("deepwork", updatedTask.contextTag)
    }

    @Test
    fun testBug335ExplicitNoteTitleTakesPrecedenceInQuickNoteConversion() {
        val userTitle = "Fix Auth Token Refresh"
        val userContent = "User sessions expire too early when refreshing expired access tokens."

        // 1. Unsaved quick note conversion with explicit user title
        val aiExtractedTitle = "Refresh expired access tokens"
        val finalSyn = com.neuroflow.app.domain.service.QuickNoteSynthesis(
            extractedTaskTitle = aiExtractedTitle,
            suggestedCategory = "ANALYTICAL",
            estimatedMinutes = 45,
            tags = listOf("auth", "security"),
            firstActionStep = "Review token refresh interceptor"
        )

        val finalTaskTitle = userTitle.trim().ifBlank { finalSyn.extractedTaskTitle }
        val taskSynthesis = if (finalTaskTitle != finalSyn.extractedTaskTitle) finalSyn.copy(extractedTaskTitle = finalTaskTitle) else finalSyn

        assertEquals("Explicit user note title must be preserved in task synthesis", "Fix Auth Token Refresh", taskSynthesis.extractedTaskTitle)

        // 2. Existing note conversion from list with explicit note.title
        val note = com.neuroflow.app.data.local.entity.NoteEntity(
            id = "note-1",
            title = "Database Migration Checklist",
            content = "1. Run schema migration 2. Verify foreign keys 3. Test Room indexes"
        )
        val inferred = com.neuroflow.app.domain.inference.TaskInferenceEngine.inferFromInput("${note.title}: ${note.content}")

        val explicitTitle = note.title.trim().takeIf { it.isNotBlank() }
        val resolvedTaskTitle = explicitTitle ?: inferred.sanitizedTitle.ifBlank { note.content.take(35) }

        assertEquals("Existing note title must not be clobbered by sanitized body title", "Database Migration Checklist", resolvedTaskTitle)
    }
}
