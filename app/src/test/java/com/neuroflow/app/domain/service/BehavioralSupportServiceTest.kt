package com.neuroflow.app.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BehavioralSupportServiceTest {

    private lateinit var service: BehavioralSupportService

    @Before
    fun setUp() {
        service = DefaultBehavioralSupportService()
    }

    @Test
    fun testResetScheduleAnchorsClearsOnlyUnlockedTasks() {
        val lockedTask = SchedulableTask(
            id = "locked",
            title = "Doctor Appointment",
            category = TaskCategory.GENERAL,
            priority = 2,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.MODERATE,
            scheduledStartMillis = 10000000L,
            isLocked = true
        )
        val unlockedTask = SchedulableTask(
            id = "unlocked",
            title = "Read article",
            category = TaskCategory.GENERAL,
            priority = 1,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.LOW,
            scheduledStartMillis = 20000000L,
            isLocked = false
        )

        val resetTasks = service.resetScheduleAnchors(listOf(lockedTask, unlockedTask))

        val resetLocked = resetTasks.first { it.id == "locked" }
        val resetUnlocked = resetTasks.first { it.id == "unlocked" }

        assertEquals(10000000L, resetLocked.scheduledStartMillis)
        assertNull(resetUnlocked.scheduledStartMillis)
    }

    @Test
    fun testGenerateMicroStepsProvidesActionableSteps() {
        val steps = service.generateMicroSteps("Build authentication module", 60)
        assertTrue("Should produce 3-4 micro-steps for a 60m task", steps.size >= 3)
        assertTrue("First step should be low-friction setup (<= 10m)", steps.first().estimatedMinutes <= 10)
        assertTrue("Step titles should reference task context", steps.any { it.title.contains("Build authentication module") })
    }

    @Test
    fun testEvaluateDistractionProvidesNonPunitiveSupport() {
        // High interruption triggers breakdown prompt, not punitive ranking
        val feedbackHigh = service.evaluateDistraction(interruptionCount = 4, durationMinutes = 20)
        assertTrue(feedbackHigh.shouldPromptBreakdown)
        assertFalse(feedbackHigh.shouldPromptDecompression)

        // Long slog with multiple interruptions triggers decompression prompt
        val feedbackSlog = service.evaluateDistraction(interruptionCount = 3, durationMinutes = 75)
        assertTrue(feedbackSlog.shouldPromptBreakdown)
        assertTrue(feedbackSlog.shouldPromptDecompression)

        // Smooth session
        val feedbackSmooth = service.evaluateDistraction(interruptionCount = 0, durationMinutes = 45)
        assertFalse(feedbackSmooth.shouldPromptBreakdown)
        assertFalse(feedbackSmooth.shouldPromptDecompression)
    }
}
