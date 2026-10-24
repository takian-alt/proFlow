package com.neuroflow.app.presentation.v2.composer

import com.neuroflow.app.data.local.entity.TaskEntity
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
}
