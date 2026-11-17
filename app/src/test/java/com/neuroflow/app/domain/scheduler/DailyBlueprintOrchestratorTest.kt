package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.DailyCircadianSummary
import com.neuroflow.app.domain.service.DailyBlueprintCoachingResult
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.domain.service.GeminiAiOnlineService
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.ScheduledPlacement
import com.neuroflow.app.domain.service.SchedulingOutcome
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.domain.service.TimeSlotWindow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DailyBlueprintOrchestratorTest {
    private val taskRepository = mockk<TaskRepository>(relaxed = true)
    private val schedulerService = mockk<TaskSchedulerService>(relaxed = true)
    private val circadianEnergyService = mockk<CircadianEnergyService>(relaxed = true)
    private val geminiAiService = mockk<GeminiAiOnlineService>(relaxed = true)
    private val sleepTelemetryRepository = mockk<SleepTelemetryRepository>(relaxed = true)
    private val preferencesDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
    private val calendarSyncRepository = mockk<CalendarSyncRepository>(relaxed = true)

    private val zoneId = ZoneId.systemDefault()
    private val todayStart = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()

    private lateinit var orchestrator: DailyBlueprintOrchestrator

    @Before
    fun setUp() {
        coEvery { preferencesDataStore.preferencesFlow } returns flowOf(
            UserPreferences(
                dailyDeepWorkTargetMinutes = 240,
                wakeTimeHour = 7,
                wakeTimeMinute = 0,
                geminiApiKey = "test_key",
                adaptiveSystemDirective = "Focus on frogs first"
            )
        )
        every { sleepTelemetryRepository.observeAllSleepLogs() } returns flowOf(emptyList())

        every {
            circadianEnergyService.computeDailySummary(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns DailyCircadianSummary(
            currentSnapshot = CircadianSnapshot(
                timestampMillis = todayStart,
                usableEnergy = 80,
                homeostaticPressureS = 0.3,
                circadianAlertnessC = 0.8,
                focusDepletionPenalty = 0.0,
                phase = EnergyPhase.PEAK
            ),
            peakWindowStartHour = 9,
            peakWindowEndHour = 12,
            peakEnergyScore = 85,
            dipWindowStartHour = 13,
            dipWindowEndHour = 15,
            recommendedDeepWorkMinutes = 240
        )

        every {
            circadianEnergyService.evaluateSnapshot(any(), any(), any(), any(), any())
        } returns CircadianSnapshot(
            timestampMillis = todayStart + 10 * 3600_000L,
            usableEnergy = 85,
            homeostaticPressureS = 0.2,
            circadianAlertnessC = 0.85,
            focusDepletionPenalty = 0.0,
            phase = EnergyPhase.PEAK
        )

        coEvery {
            geminiAiService.generateDailyBlueprintCoaching(any(), any(), any(), any(), any(), any(), any())
        } returns DailyBlueprintCoachingResult(
            headline = "Optimal Alignment",
            strategyBody = "Tackle deep architecture during your 9-12 peak window.",
            peakRecommendation = "Reserve morning for frog"
        )

        orchestrator = DailyBlueprintOrchestrator(
            taskRepository = taskRepository,
            schedulerService = schedulerService,
            circadianEnergyService = circadianEnergyService,
            geminiAiOnlineService = geminiAiService,
            sleepTelemetryRepository = sleepTelemetryRepository,
            preferencesDataStore = preferencesDataStore,
            calendarSyncRepository = calendarSyncRepository
        )
    }

    @Test
    fun testGenerateBlueprint_computesCapacityAndSelectsTasks() = runBlocking {
        val tasks = listOf(
            TaskEntity(id = "task_1", title = "Task 1", estimatedDurationMinutes = 60, quadrant = Quadrant.DO_FIRST, energyLevel = EnergyLevel.HIGH),
            TaskEntity(id = "task_2", title = "Task 2", estimatedDurationMinutes = 45, quadrant = Quadrant.SCHEDULE, energyLevel = EnergyLevel.MEDIUM)
        )
        coEvery { taskRepository.getActiveTasks() } returns tasks

        val blueprint = orchestrator.generateBlueprint(todayStart)
        assertNotNull(blueprint)
        assertEquals(todayStart, blueprint.targetDateMillis)
        assertEquals(240, blueprint.capacityTargetMinutes)
        assertTrue(blueprint.aiStrategyRationale.contains("Optimal Alignment") || blueprint.aiStrategyRationale.contains("Tackle deep"))
    }

    @Test
    fun testGenerateBlueprint_boundsPlannedMinutesAndOverflowsExcessTasks() = runBlocking {
        val task1 = TaskEntity(id = "t1", title = "Core Architecture", estimatedDurationMinutes = 120, quadrant = Quadrant.DO_FIRST)
        val task2 = TaskEntity(id = "t2", title = "Database Optimization", estimatedDurationMinutes = 100, quadrant = Quadrant.SCHEDULE)
        val task3 = TaskEntity(id = "t3", title = "Excess Heavy Task", estimatedDurationMinutes = 80, quadrant = Quadrant.SCHEDULE)

        coEvery { taskRepository.getActiveTasks() } returns listOf(task1, task2, task3)

        // Return placements for all 3 tasks (120 + 100 + 80 = 300 > 240 capacity)
        val p1 = ScheduledPlacement(
            task = SchedulableTask(id = "t1", title = task1.title, durationMinutes = 120),
            window = TimeSlotWindow(todayStart + 9 * 3600_000L, todayStart + 11 * 3600_000L)
        )
        val p2 = ScheduledPlacement(
            task = SchedulableTask(id = "t2", title = task2.title, durationMinutes = 100),
            window = TimeSlotWindow(todayStart + 11 * 3600_000L + 15 * 60_000L, todayStart + 13 * 3600_000L)
        )
        val p3 = ScheduledPlacement(
            task = SchedulableTask(id = "t3", title = task3.title, durationMinutes = 80),
            window = TimeSlotWindow(todayStart + 14 * 3600_000L, todayStart + 15 * 3600_000L + 20 * 60_000L)
        )

        every {
            schedulerService.scheduleDayWithOutcome(
                tasks = any(),
                dayDate = any(),
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
        } returns SchedulingOutcome(placements = listOf(p1, p2, p3), deferrals = emptyList())

        val blueprint = orchestrator.generateBlueprint(todayStart)

        // Tasks within 240m capacity: t1 (120m) + t2 (100m) = 220m.
        // t3 would push to 300m > 240m capacity, so t3 becomes overflow.
        assertEquals(2, blueprint.items.size)
        assertEquals("t1", blueprint.items[0].task.id)
        assertEquals("t2", blueprint.items[1].task.id)
        assertEquals(220, blueprint.plannedMinutes)
        assertEquals(240, blueprint.capacityTargetMinutes)
        assertEquals((220f / 240f) * 100f, blueprint.capacityUtilizationPercent, 0.01f)

        assertEquals(1, blueprint.overflowTasks.size)
        assertEquals("t3", blueprint.overflowTasks[0].id)
    }

    @Test
    fun testGenerateBlueprint_computesEnergyPhaseAndPeakWindow() = runBlocking {
        val task = TaskEntity(id = "t1", title = "Peak Deep Work", estimatedDurationMinutes = 60, quadrant = Quadrant.DO_FIRST)
        coEvery { taskRepository.getActiveTasks() } returns listOf(task)

        val slotStart = todayStart + 10 * 3600_000L // 10:00 AM (within 9-12 peak)
        val slotEnd = todayStart + 11 * 3600_000L

        val p1 = ScheduledPlacement(
            task = SchedulableTask(id = "t1", title = task.title, durationMinutes = 60),
            window = TimeSlotWindow(slotStart, slotEnd)
        )

        every {
            schedulerService.scheduleDayWithOutcome(
                tasks = any(),
                dayDate = any(),
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
        } returns SchedulingOutcome(placements = listOf(p1))

        val blueprint = orchestrator.generateBlueprint(todayStart)
        assertEquals(1, blueprint.items.size)
        val item = blueprint.items[0]
        assertEquals(EnergyPhase.PEAK, item.energyPhase)
        assertTrue(item.isDuringPeak)
        assertTrue(item.isIncluded)
        assertNull(item.conflictWarning)
    }

    @Test
    fun testGenerateBlueprint_offlineFallbackProducesCoachingWithoutApiKey() = runBlocking {
        coEvery { preferencesDataStore.preferencesFlow } returns flowOf(
            UserPreferences(dailyDeepWorkTargetMinutes = 240, geminiApiKey = "")
        )
        val task = TaskEntity(id = "t1", title = "Offline Task", estimatedDurationMinutes = 60, quadrant = Quadrant.DO_FIRST)
        coEvery { taskRepository.getActiveTasks() } returns listOf(task)

        val p1 = ScheduledPlacement(
            task = SchedulableTask(id = "t1", title = task.title, durationMinutes = 60),
            window = TimeSlotWindow(todayStart + 9 * 3600_000L, todayStart + 10 * 3600_000L)
        )
        every {
            schedulerService.scheduleDayWithOutcome(
                tasks = any(),
                dayDate = any(),
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
        } returns SchedulingOutcome(placements = listOf(p1))

        coEvery {
            geminiAiService.generateDailyBlueprintCoaching(any(), any(), any(), any(), any(), any(), any())
        } returns DailyBlueprintCoachingResult(
            headline = "Local Heuristic Blueprint",
            strategyBody = "Planned 60m focus based on local circadian analysis.",
            peakRecommendation = "Morning focus recommended"
        )

        val blueprint = orchestrator.generateBlueprint(todayStart)
        assertNotNull(blueprint)
        assertTrue(blueprint.aiStrategyRationale.contains("Local Heuristic Blueprint") || blueprint.aiStrategyRationale.contains("Planned 60m"))
    }

    @Test
    fun testApplyBlueprint_onlyUpdatesIncludedTasksAndSyncsCalendar() = runBlocking {
        val task1 = TaskEntity(id = "t1", title = "Task 1", estimatedDurationMinutes = 60)
        val task2 = TaskEntity(id = "t2", title = "Task 2", estimatedDurationMinutes = 45)

        coEvery { taskRepository.getById("t1") } returns task1
        coEvery { taskRepository.getById("t2") } returns task2

        val blueprint = DailyBlueprint(
            targetDateMillis = todayStart,
            plannedMinutes = 105,
            capacityTargetMinutes = 240,
            capacityUtilizationPercent = 43.75f,
            aiStrategyRationale = "Strategy",
            items = listOf(
                BlueprintTaskItem(
                    task = task1,
                    proposedStartMillis = todayStart + 9 * 3600_000L,
                    proposedEndMillis = todayStart + 10 * 3600_000L,
                    energyPhase = EnergyPhase.PEAK,
                    isDuringPeak = true,
                    isIncluded = true
                ),
                BlueprintTaskItem(
                    task = task2,
                    proposedStartMillis = todayStart + 10 * 3600_000L,
                    proposedEndMillis = todayStart + 10 * 3600_000L + 45 * 60_000L,
                    energyPhase = EnergyPhase.PEAK,
                    isDuringPeak = true,
                    isIncluded = false
                )
            ),
            overflowTasks = emptyList()
        )

        // Only include "t1"
        val result = orchestrator.applyBlueprint(blueprint, includedTaskIds = setOf("t1"))

        assertEquals(1, result.committedCount)
        assertEquals(listOf("t1"), result.committedTaskIds)

        coVerify(exactly = 1) {
            taskRepository.update(match {
                it.id == "t1" &&
                it.scheduledDate == todayStart &&
                it.scheduledTime == 9 * 3600_000L
            })
        }
        coVerify(exactly = 1) {
            calendarSyncRepository.handleTaskStateChanged(match { it.id == "t1" })
        }

        // Verify "t2" was NOT updated
        coVerify(exactly = 0) {
            taskRepository.update(match { it.id == "t2" })
        }
        coVerify(exactly = 0) {
            calendarSyncRepository.handleTaskStateChanged(match { it.id == "t2" })
        }
    }

    @Test
    fun testApplyBlueprint_handlesNullCalendarSyncRepositoryGracefully() = runBlocking {
        val task = TaskEntity(id = "t1", title = "Task 1", estimatedDurationMinutes = 60)
        coEvery { taskRepository.getById("t1") } returns task

        val orchestratorNoCalendar = DailyBlueprintOrchestrator(
            taskRepository = taskRepository,
            schedulerService = schedulerService,
            circadianEnergyService = circadianEnergyService,
            geminiAiOnlineService = geminiAiService,
            sleepTelemetryRepository = sleepTelemetryRepository,
            preferencesDataStore = preferencesDataStore,
            calendarSyncRepository = null
        )

        val blueprint = DailyBlueprint(
            targetDateMillis = todayStart,
            plannedMinutes = 60,
            capacityTargetMinutes = 240,
            capacityUtilizationPercent = 25.0f,
            aiStrategyRationale = "Strategy",
            items = listOf(
                BlueprintTaskItem(
                    task = task,
                    proposedStartMillis = todayStart + 9 * 3600_000L,
                    proposedEndMillis = todayStart + 10 * 3600_000L,
                    energyPhase = EnergyPhase.PEAK,
                    isDuringPeak = true
                )
            ),
            overflowTasks = emptyList()
        )

        val result = orchestratorNoCalendar.applyBlueprint(blueprint, includedTaskIds = setOf("t1"))
        assertEquals(1, result.committedCount)
        assertEquals(listOf("t1"), result.committedTaskIds)
        coVerify(exactly = 1) {
            taskRepository.update(match { it.id == "t1" })
        }
    }

    @Test
    fun testGenerateBlueprint_includesOverdueAndUnscheduledBacklogWithoutBlockingFixedWindows() = runBlocking {
        val yesterdayStart = LocalDate.now(zoneId).minusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val tomorrowStart = LocalDate.now(zoneId).plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val overdueTask = TaskEntity(
            id = "overdue-1",
            title = "Overdue Draft",
            scheduledDate = yesterdayStart,
            scheduledTime = 9 * 3600_000L,
            estimatedDurationMinutes = 30,
            isAutoScheduled = true,
            quadrant = Quadrant.DO_FIRST
        )
        // Backlog task carrying a default time-of-day offset without scheduledDate must NOT block busyWindows
        val backlogWithOffset = TaskEntity(
            id = "backlog-1",
            title = "Backlog Deep Work",
            scheduledDate = null,
            scheduledTime = 11 * 3600_000L,
            estimatedDurationMinutes = 45,
            isAutoScheduled = false,
            quadrant = Quadrant.SCHEDULE
        )
        coEvery { taskRepository.getActiveTasks() } returns listOf(overdueTask, backlogWithOffset)
        coEvery { taskRepository.getAllTasks() } returns listOf(overdueTask, backlogWithOffset)

        every {
            schedulerService.scheduleDayWithOutcome(
                tasks = any(),
                dayDate = any(),
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
        } returns SchedulingOutcome(emptyList(), emptyList())

        orchestrator.generateBlueprint(todayStart)
        coVerify {
            schedulerService.scheduleDayWithOutcome(
                tasks = match { tasks ->
                    tasks.any { it.id == "overdue-1" } && tasks.any { it.id == "backlog-1" }
                },
                dayDate = LocalDate.now(zoneId),
                nowMillis = any(),
                wakeTimeMillis = any(),
                lastSleepDurationMinutes = any(),
                busyWindows = match { it.isEmpty() },
                protectedSlots = any(),
                calibrationRatio = any(),
                forceReplan = true,
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
        }

        // Future target date should ALSO receive unscheduled backlog items (minus overdue)
        orchestrator.generateBlueprint(tomorrowStart)
        coVerify {
            schedulerService.scheduleDayWithOutcome(
                tasks = match { tasks -> tasks.any { it.id == "backlog-1" } },
                dayDate = LocalDate.now(zoneId).plusDays(1),
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
        }
    }

    @Test
    fun testGenerateBlueprint_deductsCompletedAndFixedCognitiveMinutesAndGroupsDynamicSplitAtomically() = runBlocking {
        val completedToday = TaskEntity(
            id = "done-today",
            title = "Finished Morning Spec",
            scheduledDate = todayStart,
            plannedBlockMinutes = 60,
            estimatedDurationMinutes = 60,
            energyLevel = EnergyLevel.HIGH,
            status = com.neuroflow.app.domain.model.TaskStatus.COMPLETED,
            completedAt = todayStart + 8 * 3600_000L
        )
        val lockedToday = TaskEntity(
            id = "locked-today",
            title = "Locked Sync",
            scheduledDate = todayStart,
            scheduledTime = 9 * 3600_000L,
            plannedBlockMinutes = 60,
            estimatedDurationMinutes = 60,
            energyLevel = EnergyLevel.MEDIUM,
            isScheduleLocked = true,
            isAutoScheduled = false,
            status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE
        )
        val normalTask = TaskEntity(
            id = "normal-fit",
            title = "Fits Budget",
            estimatedDurationMinutes = 60,
            quadrant = Quadrant.DO_FIRST
        )
        val bigSplitTask = TaskEntity(
            id = "big-split",
            title = "Big Design Work",
            estimatedDurationMinutes = 90,
            quadrant = Quadrant.SCHEDULE
        )

        coEvery { taskRepository.getActiveTasks() } returns listOf(lockedToday, normalTask, bigSplitTask)
        coEvery { taskRepository.getAllTasks() } returns listOf(completedToday, lockedToday, normalTask, bigSplitTask)

        // Daily target = 240m, precommitted = 120m (60 done + 60 locked) -> remaining = 120m.
        // Normal (60m) + Big Split (45m + 45m = 90m) total 150m > 120m remaining.
        // Both Big Split parts must move to overflow together rather than leaving orphaned Part 1!
        val pNormal = ScheduledPlacement(
            task = SchedulableTask(id = "normal-fit", title = "Fits Budget", durationMinutes = 60),
            window = TimeSlotWindow(todayStart + 10 * 3600_000L, todayStart + 11 * 3600_000L)
        )
        val pSplit1 = ScheduledPlacement(
            task = SchedulableTask(id = "big-split#part1", title = "Big Design Work", durationMinutes = 45),
            window = TimeSlotWindow(todayStart + 11 * 3600_000L, todayStart + 11 * 3600_000L + 45 * 60_000L)
        )
        val pSplit2 = ScheduledPlacement(
            task = SchedulableTask(id = "big-split#part2", title = "Big Design Work", durationMinutes = 45),
            window = TimeSlotWindow(todayStart + 12 * 3600_000L, todayStart + 12 * 3600_000L + 45 * 60_000L)
        )

        every {
            schedulerService.scheduleDayWithOutcome(
                tasks = any(),
                dayDate = any(),
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
        } returns SchedulingOutcome(placements = listOf(pNormal, pSplit1, pSplit2), deferrals = emptyList())

        val blueprint = orchestrator.generateBlueprint(todayStart)

        assertEquals(120, blueprint.precommittedMinutes)
        assertEquals(60, blueprint.plannedMinutes)
        assertEquals(1, blueprint.items.size)
        assertEquals("normal-fit", blueprint.items.first().task.id)
        assertEquals(1, blueprint.overflowTasks.size)
        assertEquals("big-split", blueprint.overflowTasks.first().id)
        assertEquals((180f / 240f) * 100f, blueprint.capacityUtilizationPercent, 0.01f)
    }
}
