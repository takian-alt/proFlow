package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.entity.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoSchedulingTransparencyTest {

    @Test
    fun testAllTasksPlacedWhenCapacitySufficient() {
        val tasks = listOf(
            TaskEntity(id = "t1", title = "Task 1", estimatedDurationMinutes = 30),
            TaskEntity(id = "t2", title = "Task 2", estimatedDurationMinutes = 45)
        )
        val report = AutoSchedulingEngineCoordinator.planSchedule(
            tasks = tasks,
            availableSlotsMinutes = 120,
            dailyCognitiveCapacityMinutes = 240
        )
        assertEquals(2, report.placedCount)
        assertEquals(0, report.deferredCount)
        assertEquals(2, report.placementRationales.size)
        assertTrue(report.deferrals.isEmpty())
        assertTrue(report.dailyCognitiveLoadUtilized > 0f)
    }

    @Test
    fun testNoSilentDropsWhenCognitiveCapacityExceeded() {
        val tasks = listOf(
            TaskEntity(id = "t1", title = "Task 1", estimatedDurationMinutes = 60),
            TaskEntity(id = "t2", title = "Task 2", estimatedDurationMinutes = 60)
        )
        val report = AutoSchedulingEngineCoordinator.planSchedule(
            tasks = tasks,
            availableSlotsMinutes = 120,
            dailyCognitiveCapacityMinutes = 90
        )
        assertEquals(1, report.placedCount)
        assertEquals(1, report.deferredCount)
        assertEquals(tasks.size, report.placedCount + report.deferredCount)
        assertEquals(DeferralReasonCode.COGNITIVE_BUDGET_EXCEEDED, report.deferrals.first().reasonCode)
        assertTrue(report.deferrals.first().actionableSuggestion.isNotBlank())
    }

    @Test
    fun testNoSilentDropsWhenSlotInsufficient() {
        val tasks = listOf(
            TaskEntity(id = "large", title = "Deep refactoring", estimatedDurationMinutes = 180)
        )
        val report = AutoSchedulingEngineCoordinator.planSchedule(
            tasks = tasks,
            availableSlotsMinutes = 60,
            dailyCognitiveCapacityMinutes = 240
        )
        assertEquals(0, report.placedCount)
        assertEquals(1, report.deferredCount)
        assertEquals(DeferralReasonCode.INSUFFICIENT_TIME, report.deferrals.first().reasonCode)
    }

    @Test
    fun testBehavioralInsightsBayesianShrinkage() {
        val factorUnderestimated = BehavioralInsightsEngine.calculateCalibrationFactor(
            actualMinutes = listOf(60, 60, 60, 60),
            estimatedMinutes = listOf(30, 30, 30, 30)
        )
        assertTrue("Calibration factor must calibrate upward for underestimated tasks", factorUnderestimated > 1.1f)

        val factorAccurate = BehavioralInsightsEngine.calculateCalibrationFactor(
            actualMinutes = listOf(30, 30, 30),
            estimatedMinutes = listOf(30, 30, 30)
        )
        assertEquals(1.0f, factorAccurate, 0.05f)
    }

    @Test
    fun testBehavioralCircadianDensityDistribution() {
        val cluster = mapOf(9 to 45, 10 to 45, 11 to 30)
        val density = BehavioralInsightsEngine.calculateHourlyCircadianDensity(cluster)
        assertEquals(24, density.size)
        assertEquals(1.0f, density.sum(), 0.01f)
        assertTrue(density[9] + density[10] + density[11] > 0.8f)
    }
}
