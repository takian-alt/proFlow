package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.repository.SleepLog
import com.neuroflow.app.data.repository.SleepTelemetryRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.ScheduledPlacement
import com.neuroflow.app.domain.service.SchedulingOutcome
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.domain.service.TimeSlotWindow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MultiDayScheduleOrchestratorTest {
    private val schedulerService = mockk<TaskSchedulerService>(relaxed = true)
    private val sleepRepository = mockk<SleepTelemetryRepository>(relaxed = true)
    private val preferencesDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
    private lateinit var orchestrator: MultiDayScheduleOrchestrator

    @Before
    fun setUp() {
        coEvery { preferencesDataStore.preferencesFlow } returns flowOf(
            UserPreferences(wakeTimeHour = 8, wakeTimeMinute = 30, dailyDeepWorkTargetMinutes = 180)
        )
        every { sleepRepository.observeAllSleepLogs() } returns flowOf(
            listOf(
                SleepLog(
                    id = "sleep_1",
                    bedTimeMillis = System.currentTimeMillis() - 9 * 3600_000L,
                    wakeTimeMillis = System.currentTimeMillis() - 1 * 3600_000L,
                    qualityRating = 4,
                    notes = ""
                )
            )
        )
        orchestrator = MultiDayScheduleOrchestrator(
            schedulerService = schedulerService,
            circadianEnergyService = mockk(relaxed = true),
            calendarSyncRepository = null,
            sleepTelemetryRepository = sleepRepository,
            preferencesDataStore = preferencesDataStore
        )
    }

    @Test
    fun testOrchestrateMultiDaySchedule_usesDynamicWakeAnchor() = runBlocking {
        val tasks = listOf(
            SchedulableTask(id = "task_1", title = "Deep Work", durationMinutes = 60, energyLevel = EnergyLevel.HIGH, quadrant = Quadrant.DO_FIRST)
        )
        val outcome = orchestrator.orchestrateMultiDaySchedule(tasks, startDate = LocalDate.now(), horizonDays = 3)
        assertNotNull(outcome)
        assertEquals(3, outcome.dayOutcomes.size)
    }

    @Test
    fun testOrchestrateMultiDaySchedule_enforcesCognitiveCapacityOverflow() = runBlocking {
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val task1 = SchedulableTask(id = "task_1", title = "Task 1", estimatedMinutes = 120, category = TaskCategory.DEEP_WORK, priority = 2, energyDemand = EnergyDemand.HIGH)
        val task2 = SchedulableTask(id = "task_2", title = "Task 2", estimatedMinutes = 120, category = TaskCategory.DEEP_WORK, priority = 2, energyDemand = EnergyDemand.HIGH)

        val placement1 = ScheduledPlacement(task1, TimeSlotWindow(dayStart + 9 * 3600_000L, dayStart + 11 * 3600_000L), 90.0)
        val placement2 = ScheduledPlacement(task2, TimeSlotWindow(dayStart + 11 * 3600_000L, dayStart + 13 * 3600_000L), 80.0)

        // Mock scheduler returning both placements for Day 1 (total 240m > 180m daily budget)
        every {
            schedulerService.scheduleDayWithOutcome(
                tasks = match { it.any { t -> t.id == "task_1" } },
                dayDate = today,
                nowMillis = any(),
                wakeTimeMillis = any(),
                lastSleepDurationMinutes = any(),
                busyWindows = any(),
                protectedSlots = any(),
                calibrationRatio = any(),
                forceReplan = any(),
                zoneId = any(),
                naps = any(),
                chronotype = any(),
                manualPeakStartHour = any(),
                priorityWeights = any(),
                learnedProfile = any(),
                rollingAverageSleepMinutes = any(),
                physiologicalBlocks = any(),
                stimulantDoses = any(),
                socialJetlagLagHours = any(),
                activeUncompletedTaskIds = any(),
                workDayStartHour = any(),
                workDayEndHour = any(),
                dailyCognitiveCapacityMinutes = any(),
                rejectionPatternsJson = any()
            )
        } returns SchedulingOutcome(listOf(placement1, placement2))

        val nextDay = today.plusDays(1)
        val nextDayStart = nextDay.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val placement2Day2 = ScheduledPlacement(task2, TimeSlotWindow(nextDayStart + 9 * 3600_000L, nextDayStart + 11 * 3600_000L), 80.0)
        every {
            schedulerService.scheduleDayWithOutcome(
                tasks = match { it.size == 1 && it[0].id == "task_2" },
                dayDate = nextDay,
                nowMillis = any(),
                wakeTimeMillis = any(),
                lastSleepDurationMinutes = any(),
                busyWindows = any(),
                protectedSlots = any(),
                calibrationRatio = any(),
                forceReplan = any(),
                zoneId = any(),
                naps = any(),
                chronotype = any(),
                manualPeakStartHour = any(),
                priorityWeights = any(),
                learnedProfile = any(),
                rollingAverageSleepMinutes = any(),
                physiologicalBlocks = any(),
                stimulantDoses = any(),
                socialJetlagLagHours = any(),
                activeUncompletedTaskIds = any(),
                workDayStartHour = any(),
                workDayEndHour = any(),
                dailyCognitiveCapacityMinutes = any(),
                rejectionPatternsJson = any()
            )
        } returns SchedulingOutcome(listOf(placement2Day2))

        val outcome = orchestrator.orchestrateMultiDaySchedule(listOf(task1, task2), startDate = today, horizonDays = 2)

        val day1Placements = outcome.dayOutcomes[today]?.placements ?: emptyList()
        assertEquals(1, day1Placements.size)
        assertEquals("task_1", day1Placements[0].task.id)

        val day2Placements = outcome.dayOutcomes[nextDay]?.placements ?: emptyList()
        assertEquals(1, day2Placements.size)
        assertEquals("task_2", day2Placements[0].task.id)
    }

    @Test
    fun testMultiDayOrchestrator_cascadesOverflowToNextDay() = runTest {
        val circadianService = DefaultCircadianEnergyService()
        val realOrchestrator = MultiDayScheduleOrchestrator(
            schedulerService = DefaultTaskSchedulerService(circadianService),
            circadianEnergyService = circadianService,
            calendarSyncRepository = null
        )
        val today = LocalDate.of(2026, 11, 14)
        val outcome = realOrchestrator.orchestrateMultiDaySchedule(
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
        val realOrchestrator = MultiDayScheduleOrchestrator(
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

        val outcome = realOrchestrator.orchestrateMultiDaySchedule(
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

    @Test
    fun testMultiDayOrchestrator_crossDaySplitsArbitraryParts() = runBlocking {
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val part1 = SchedulableTask(id = "epic_task#part1", title = "Epic Task (Part 1/3)", estimatedMinutes = 90, category = TaskCategory.DEEP_WORK, priority = 2, energyDemand = EnergyDemand.HIGH)
        val part2 = SchedulableTask(id = "epic_task#part2", title = "Epic Task (Part 2/3)", estimatedMinutes = 90, category = TaskCategory.DEEP_WORK, priority = 2, energyDemand = EnergyDemand.HIGH)
        val part3 = SchedulableTask(id = "epic_task#part3", title = "Epic Task (Part 3/3)", estimatedMinutes = 90, category = TaskCategory.DEEP_WORK, priority = 2, energyDemand = EnergyDemand.HIGH)

        val placementPart1 = ScheduledPlacement(part1, TimeSlotWindow(dayStart + 9 * 3600_000L, dayStart + 10 * 3600_000L + 30 * 60_000L), 90.0)

        every {
            schedulerService.scheduleDayWithOutcome(
                tasks = match { it.any { t -> t.id == "epic_task#part1" } },
                dayDate = today,
                nowMillis = any(),
                wakeTimeMillis = any(),
                lastSleepDurationMinutes = any(),
                busyWindows = any(),
                protectedSlots = any(),
                calibrationRatio = any(),
                forceReplan = any(),
                zoneId = any(),
                naps = any(),
                chronotype = any(),
                manualPeakStartHour = any(),
                priorityWeights = any(),
                learnedProfile = any(),
                rollingAverageSleepMinutes = any(),
                physiologicalBlocks = any(),
                stimulantDoses = any(),
                socialJetlagLagHours = any(),
                activeUncompletedTaskIds = any(),
                workDayStartHour = any(),
                workDayEndHour = any(),
                dailyCognitiveCapacityMinutes = any(),
                rejectionPatternsJson = any()
            )
        } returns SchedulingOutcome(listOf(placementPart1))

        val outcome = orchestrator.orchestrateMultiDaySchedule(listOf(part1, part2, part3), startDate = today, horizonDays = 3)
        assertEquals(1, outcome.crossDaySplits.size)
        val splitInfo = outcome.crossDaySplits[0]
        assertEquals("epic_task", splitInfo.parentTaskId)
        assertEquals("epic_task#part1", splitInfo.part1Placement.task.id)
        assertEquals(today.plusDays(1), splitInfo.part2Date)
        assertEquals(15, splitInfo.part2WarmupMinutes)
    }
}
