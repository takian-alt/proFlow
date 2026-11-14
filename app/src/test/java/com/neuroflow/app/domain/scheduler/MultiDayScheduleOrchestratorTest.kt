package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.TaskCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MultiDayScheduleOrchestratorTest {
    @Test
    fun testMultiDayOrchestrator_cascadesOverflowToNextDay() = runTest {
        val circadianService = DefaultCircadianEnergyService()
        val orchestrator = MultiDayScheduleOrchestrator(
            schedulerService = DefaultTaskSchedulerService(circadianService),
            circadianEnergyService = circadianService,
            calendarSyncRepository = null
        )
        val today = LocalDate.of(2026, 11, 14)
        val outcome = orchestrator.orchestrateMultiDaySchedule(
            tasks = emptyList<SchedulableTask>(),
            startDate = today,
            horizonDays = 7,
            zoneId = ZoneId.systemDefault()
        )
        assertEquals(7, outcome.dayOutcomes.size)
    }

    @Test
    fun testMultiDayOrchestrator_spillsExcessTasksToTomorrow() = runTest {
        val circadianService = DefaultCircadianEnergyService()
        val orchestrator = MultiDayScheduleOrchestrator(
            schedulerService = DefaultTaskSchedulerService(circadianService),
            circadianEnergyService = circadianService,
            calendarSyncRepository = null
        )
        val today = LocalDate.of(2026, 11, 14)

        // Create 6 tasks each of 120 mins (720 mins total - exceeds 1 day capacity ~480 mins)
        val heavyTasks = (1..6).map { i ->
            SchedulableTask(
                id = "heavy-task-$i",
                title = "Heavy Task $i",
                category = TaskCategory.DEEP_WORK,
                priority = 2,
                estimatedMinutes = 120,
                energyDemand = EnergyDemand.HIGH
            )
        }

        val outcome = orchestrator.orchestrateMultiDaySchedule(
            tasks = heavyTasks,
            startDate = today,
            horizonDays = 3,
            zoneId = ZoneId.systemDefault()
        )

        val day0Placements = outcome.dayOutcomes[today]?.placements ?: emptyList()
        val day1Placements = outcome.dayOutcomes[today.plusDays(1)]?.placements ?: emptyList()

        assertTrue("Day 0 must place tasks", day0Placements.isNotEmpty())
        assertTrue("Day 1 must receive spilled-over tasks", day1Placements.isNotEmpty())
        assertTrue("Total scheduled must be greater than day 0 alone", outcome.totalScheduledTasks > day0Placements.size)
    }
}
