package com.neuroflow.app.domain.inference

import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.TaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskInferenceEngineTest {

    @Test
    fun testEmptyInputGivesDefaultMetadata() {
        val result = TaskInferenceEngine.inferFromInput("")
        assertEquals("", result.sanitizedTitle)
        assertEquals(25, result.durationMinutes)
        assertEquals(EnergyLevel.MEDIUM, result.energyDemand)
        assertEquals(TaskType.GENERAL, result.taskType)
        assertTrue(result.contextTags.isEmpty())
        assertFalse(result.isFrog)
    }

    @Test
    fun testDurationExtractionFormats() {
        assertEquals(15, TaskInferenceEngine.inferFromInput("Quick email 15m").durationMinutes)
        assertEquals(90, TaskInferenceEngine.inferFromInput("Coding 90min").durationMinutes)
        assertEquals(60, TaskInferenceEngine.inferFromInput("Review 1h").durationMinutes)
        assertEquals(150, TaskInferenceEngine.inferFromInput("Architecture 2.5h").durationMinutes)
        assertEquals(45, TaskInferenceEngine.inferFromInput("Meeting 45 mins").durationMinutes)
        assertEquals(90, TaskInferenceEngine.inferFromInput("Design 1h 30m").durationMinutes)
    }

    @Test
    fun testEnergyDemandInference() {
        val highEnergy = TaskInferenceEngine.inferFromInput("Deep work on compiler thesis 90m")
        val lowEnergy = TaskInferenceEngine.inferFromInput("Clean desk and file expense 15m")
        val medEnergy = TaskInferenceEngine.inferFromInput("Team status update 30m")

        assertEquals(EnergyLevel.HIGH, highEnergy.energyDemand)
        assertEquals(EnergyLevel.LOW, lowEnergy.energyDemand)
        assertEquals(EnergyLevel.MEDIUM, medEnergy.energyDemand)
    }

    @Test
    fun testTaskTypeInference() {
        assertEquals(TaskType.ANALYTICAL, TaskInferenceEngine.inferFromInput("Debug algorithm bug 30m").taskType)
        assertEquals(TaskType.CREATIVE, TaskInferenceEngine.inferFromInput("Draft design system article 45m").taskType)
        assertEquals(TaskType.ADMIN, TaskInferenceEngine.inferFromInput("Send weekly sync email 15m").taskType)
        assertEquals(TaskType.PHYSICAL, TaskInferenceEngine.inferFromInput("Morning gym workout 60m").taskType)
    }

    @Test
    fun testFrogDetectionAndTitleSanitization() {
        val result = TaskInferenceEngine.inferFromInput("!frog Prepare quarterly financial tax return 60m @finance #urgent")
        assertTrue(result.isFrog)
        assertEquals("Prepare quarterly financial tax return", result.sanitizedTitle)
        assertEquals(listOf("finance"), result.contextTags)
        assertEquals(60, result.durationMinutes)
    }

    @Test
    fun testNaturalLanguageDurationPhrases() {
        assertEquals(30, TaskInferenceEngine.inferFromInput("Quick sync half hour").durationMinutes)
        assertEquals(30, TaskInferenceEngine.inferFromInput("Consultation half an hour").durationMinutes)
        assertEquals(60, TaskInferenceEngine.inferFromInput("Sprint planning an hour").durationMinutes)
    }

    @Test
    fun testExplicitEnergyAndPriorityParsing() {
        val highP1 = TaskInferenceEngine.inferFromInput("Quarterly review !high !p1")
        assertEquals(EnergyLevel.HIGH, highP1.energyDemand)
        assertEquals(com.neuroflow.app.domain.model.Priority.HIGH, highP1.priority)

        val lowP3 = TaskInferenceEngine.inferFromInput("Clear inbox !low !p3")
        assertEquals(EnergyLevel.LOW, lowP3.energyDemand)
        assertEquals(com.neuroflow.app.domain.model.Priority.LOW, lowP3.priority)
    }

    @Test
    fun testWordBoundaryIsolationPreventsFalseMatches() {
        // "barcode scanner" should NOT trigger "code" -> ANALYTICAL
        val scanner = TaskInferenceEngine.inferFromInput("Order barcode scanner replacement")
        assertEquals(TaskType.ADMIN, scanner.taskType)
    }

    @Test
    fun testExplicitEisenhowerQuadrantInferenceAndTitleSanitization() {
        val q1 = TaskInferenceEngine.inferFromInput("Ship hotfix !q1")
        assertEquals(com.neuroflow.app.domain.model.Quadrant.DO_FIRST, q1.quadrant)
        assertEquals("Ship hotfix", q1.sanitizedTitle)

        val doFirst = TaskInferenceEngine.inferFromInput("Pay taxes !dofirst 30m")
        assertEquals(com.neuroflow.app.domain.model.Quadrant.DO_FIRST, doFirst.quadrant)
        assertEquals("Pay taxes", doFirst.sanitizedTitle)

        val q2 = TaskInferenceEngine.inferFromInput("Study system design #q2")
        assertEquals(com.neuroflow.app.domain.model.Quadrant.SCHEDULE, q2.quadrant)
        assertEquals("Study system design", q2.sanitizedTitle)

        val q3 = TaskInferenceEngine.inferFromInput("Sort utility invoices !delegate")
        assertEquals(com.neuroflow.app.domain.model.Quadrant.DELEGATE, q3.quadrant)
        assertEquals("Sort utility invoices", q3.sanitizedTitle)

        val q4 = TaskInferenceEngine.inferFromInput("Clean ancient desktop icons !eliminate")
        assertEquals(com.neuroflow.app.domain.model.Quadrant.ELIMINATE, q4.quadrant)
        assertEquals("Clean ancient desktop icons", q4.sanitizedTitle)
    }

    @Test
    fun testDueNextDayParsingPreservesDeadlinesAndDoesNotPolluteScheduledDate() {
        val result = TaskInferenceEngine.inferFromInput("Submit quarterly report due next friday at 5pm")
        org.junit.Assert.assertNotNull(result.deadlineDate)
        org.junit.Assert.assertNotNull(result.deadlineTime)
        assertEquals(17 * 3600_000L, result.deadlineTime)
        org.junit.Assert.assertNull(result.scheduledDate)
        org.junit.Assert.assertNull(result.scheduledTime)
        assertEquals("Submit quarterly report", result.sanitizedTitle)
    }

    @Test
    fun testScheduleForNextDayParsingSetsScheduledDate() {
        val result = TaskInferenceEngine.inferFromInput("Team sync scheduled for next monday at 10am")
        org.junit.Assert.assertNotNull(result.scheduledDate)
        assertEquals(10 * 3600_000L, result.scheduledTime)
        org.junit.Assert.assertNull(result.deadlineDate)
        assertEquals("Team sync", result.sanitizedTitle)
    }

    @Test
    fun testDueTomorrowAfternoonPreservesDeadlineAndDoesNotScheduleForToday() {
        val result = TaskInferenceEngine.inferFromInput("Submit quarterly taxes due tomorrow afternoon")
        org.junit.Assert.assertNotNull(result.deadlineDate)
        org.junit.Assert.assertNotNull(result.deadlineTime)
        assertEquals(14 * 3600_000L, result.deadlineTime)
        org.junit.Assert.assertNull(result.scheduledDate)
        org.junit.Assert.assertNull(result.scheduledTime)
        assertEquals("Submit quarterly taxes", result.sanitizedTitle)
    }

    @Test
    fun testDueFridayMorningPreservesDeadlineAndDoesNotScheduleForToday() {
        val result = TaskInferenceEngine.inferFromInput("Pay server bills due next friday morning")
        org.junit.Assert.assertNotNull(result.deadlineDate)
        org.junit.Assert.assertNotNull(result.deadlineTime)
        assertEquals(9 * 3600_000L + 30 * 60_000L, result.deadlineTime)
        org.junit.Assert.assertNull(result.scheduledDate)
        org.junit.Assert.assertNull(result.scheduledTime)
        assertEquals("Pay server bills", result.sanitizedTitle)
    }

    @Test
    fun testDueInRelativeOffsetPreservesDeadline() {
        val result = TaskInferenceEngine.inferFromInput("Submit incident report due in 2 hours")
        org.junit.Assert.assertNotNull(result.deadlineDate)
        org.junit.Assert.assertNotNull(result.deadlineTime)
        org.junit.Assert.assertNull(result.scheduledDate)
        org.junit.Assert.assertNull(result.scheduledTime)
        assertEquals("Submit incident report", result.sanitizedTitle)
    }

    @Test
    fun testScheduleInRelativeOffsetSchedulesTask() {
        val result = TaskInferenceEngine.inferFromInput("Team catchup in 2 hours")
        org.junit.Assert.assertNotNull(result.scheduledDate)
        org.junit.Assert.assertNotNull(result.scheduledTime)
        org.junit.Assert.assertNull(result.deadlineDate)
        org.junit.Assert.assertNull(result.deadlineTime)
        assertEquals("Team catchup", result.sanitizedTitle)
    }
}
