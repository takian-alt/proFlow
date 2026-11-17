package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.domain.service.GeminiAiOnlineService
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TimeSlotWindow
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SchedulerPriorityAndBalancingEnhancementsTest {

    private val zoneId = ZoneId.of("UTC")
    private val circadianService = DefaultCircadianEnergyService()
    private val schedulerService = DefaultTaskSchedulerService(circadianService)

    @Test
    fun priorityScore_avoidsFlatlineSaturation_andSeparatesImpactTiers() {
        val day = LocalDate.of(2026, 11, 17)
        val now = day.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli()
        val snapshot = CircadianSnapshot(
            timestampMillis = now,
            usableEnergy = 85,
            baselineBiologicalEnergy = 85,
            circadianAlertnessC = 0.9,
            homeostaticPressureS = 0.2,
            focusDepletionPenalty = 0.0,
            phase = EnergyPhase.PEAK
        )

        val highStandard = SchedulableTask(
            id = "high_std",
            title = "Standard High Priority",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            impactScore = 50,
            isFrog = false,
            createdAtMillis = now
        )
        val highImpactFrog = highStandard.copy(
            id = "high_frog",
            impactScore = 90,
            isFrog = true
        )
        val lowRoutine = highStandard.copy(
            id = "low_routine",
            category = TaskCategory.ADMIN,
            priority = 0,
            energyDemand = EnergyDemand.LOW,
            impactScore = 20
        )

        val stdScore = schedulerService.evaluateTaskScore(highStandard, snapshot, calibrationRatio = 1.0)
        val frogScore = schedulerService.evaluateTaskScore(highImpactFrog, snapshot, dependentCount = 1, calibrationRatio = 1.0)
        val lowScore = schedulerService.evaluateTaskScore(lowRoutine, snapshot, calibrationRatio = 1.0)

        assertTrue("Standard HIGH task impact should not flatline at 100 (was ${stdScore.impactScore})", stdScore.impactScore < 60.0)
        assertTrue("HIGH Frog + high impact must stay above 50 (was ${frogScore.impactScore})", frogScore.impactScore > 50.0)
        assertTrue("HIGH Frog impact must exceed standard HIGH impact", frogScore.impactScore > stdScore.impactScore)
        assertTrue("Total score tiers must separate cleanly: frog=${frogScore.totalScore} > std=${stdScore.totalScore} > low=${lowScore.totalScore}",
            frogScore.totalScore > stdScore.totalScore && stdScore.totalScore > lowScore.totalScore
        )
    }

    @Test
    fun autoSchedule_urgentDeadlineOutranksTrivialNoDeadlinePersonalTask() {
        val day = LocalDate.of(2026, 11, 17)
        val now = day.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli()
        val wake = day.atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()

        val trivialPersonal = SchedulableTask(
            id = "personal_chore",
            title = "Organize desktop folder",
            category = TaskCategory.ADMIN,
            priority = 0,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.LOW,
            isPersonalTask = true,
            deadlineMillis = null,
            createdAtMillis = now
        )
        val urgentWork = SchedulableTask(
            id = "urgent_deadline",
            title = "Submit production tax filing",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            isFrog = true,
            isPersonalTask = false,
            deadlineMillis = now + 90 * 60_000L, // due in 1.5 hours
            createdAtMillis = now
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(trivialPersonal, urgentWork),
            dayDate = day,
            nowMillis = now,
            wakeTimeMillis = wake,
            lastSleepDurationMinutes = 480,
            calibrationRatio = 1.0,
            zoneId = zoneId,
            workDayStartHour = 9,
            workDayEndHour = 18,
            dailyCognitiveCapacityMinutes = 60 // Tight budget forces choice
        )

        assertEquals(1, outcome.placements.size)
        assertEquals("Urgent deadline task must beat trivial no-deadline personal chore", "urgent_deadline", outcome.placements.first().task.id)
    }

    @Test
    fun autoSchedule_asymmetricSplitFallbackFitsIrregularPockets() {
        val day = LocalDate.of(2026, 11, 17)
        val now = day.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli()
        val wake = day.atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()

        // Free windows: 09:05..09:50 (45m) and 14:00..15:10 (70m) -> symmetric 50/50 split (50m+50m) cannot fit 45m pocket,
        // but asymmetric 40/60 split (40m + 60m) fits cleanly!
        val morningBusy = TimeSlotWindow(
            day.atTime(9, 50).atZone(zoneId).toInstant().toEpochMilli(),
            day.atTime(14, 0).atZone(zoneId).toInstant().toEpochMilli()
        )
        val afternoonBusy = TimeSlotWindow(
            day.atTime(15, 10).atZone(zoneId).toInstant().toEpochMilli(),
            day.atTime(20, 0).atZone(zoneId).toInstant().toEpochMilli()
        )

        val longTask = SchedulableTask(
            id = "asymmetric_task",
            title = "Architecture Writeup",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 100,
            energyDemand = EnergyDemand.HIGH,
            fragmentationTolerance = 0.8f
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(longTask),
            dayDate = day,
            nowMillis = now,
            wakeTimeMillis = wake,
            lastSleepDurationMinutes = 480,
            busyWindows = listOf(morningBusy, afternoonBusy),
            calibrationRatio = 1.0,
            zoneId = zoneId,
            workDayStartHour = 9,
            workDayEndHour = 17
        )

        assertEquals("Asymmetric split must place both parts", 2, outcome.placements.size)
        assertTrue(outcome.placements.any { it.task.id == "asymmetric_task#part1" })
        assertTrue(outcome.placements.any { it.task.id == "asymmetric_task#part2" })
        assertTrue(outcome.deferrals.isEmpty())
    }

    @Test
    fun dailyBlueprint_deductsPrecommittedCompletedCognitiveWorkFromRemainingCapacity() = runTest {
        val today = LocalDate.now(ZoneId.systemDefault())
        val dayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val completedMorning = TaskEntity(
            id = "done_deep",
            title = "Morning Deep Work",
            status = TaskStatus.COMPLETED,
            energyLevel = EnergyLevel.HIGH,
            taskType = TaskType.ANALYTICAL,
            estimatedDurationMinutes = 120,
            scheduledDate = dayStart
        )
        val cand1 = TaskEntity(
            id = "cand_1",
            title = "Candidate 1",
            status = TaskStatus.ACTIVE,
            priority = Priority.HIGH,
            quadrant = Quadrant.DO_FIRST,
            energyLevel = EnergyLevel.HIGH,
            estimatedDurationMinutes = 100,
            scheduledDate = dayStart
        )
        val cand2 = TaskEntity(
            id = "cand_2",
            title = "Candidate 2",
            status = TaskStatus.ACTIVE,
            priority = Priority.MEDIUM,
            quadrant = Quadrant.SCHEDULE,
            energyLevel = EnergyLevel.MEDIUM,
            estimatedDurationMinutes = 90,
            scheduledDate = dayStart
        )

        val taskRepo = mockk<TaskRepository>(relaxed = true)
        coEvery { taskRepo.getActiveTasks() } returns listOf(cand1, cand2)
        coEvery { taskRepo.getAllTasks() } returns listOf(completedMorning, cand1, cand2)

        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        coEvery { prefsDataStore.preferencesFlow } returns flowOf(
            UserPreferences(dailyDeepWorkTargetMinutes = 240, workDayStart = 6, workDayEnd = 23)
        )

        val aiService = mockk<GeminiAiOnlineService>(relaxed = true)
        val orchestrator = DailyBlueprintOrchestrator(
            taskRepository = taskRepo,
            schedulerService = DefaultTaskSchedulerService(DefaultCircadianEnergyService()),
            circadianEnergyService = DefaultCircadianEnergyService(),
            geminiAiOnlineService = aiService,
            preferencesDataStore = prefsDataStore
        )

        val blueprint = orchestrator.generateBlueprint(dayStart)

        // 240m daily target - 120m already completed today = 120m remaining budget -> only cand_1 (100m) fits, cand_2 (90m) overflows
        assertTrue("Planned minutes (${blueprint.plannedMinutes}) must respect remaining 120m capacity", blueprint.plannedMinutes <= 120)
        assertTrue("At least one excess candidate should overflow after precommitted deduction", blueprint.overflowTasks.isNotEmpty())
    }

    @Test
    fun multiDayAutoBalance_isolatesFutureLockedTasksAndSmoothsLoadAcrossHorizon() = runTest {
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        coEvery { prefsDataStore.preferencesFlow } returns flowOf(
            UserPreferences(
                wakeTimeHour = 7,
                wakeTimeMinute = 0,
                dailyDeepWorkTargetMinutes = 240,
                workDayStart = 8,
                workDayEnd = 20
            )
        )
        val orchestrator = MultiDayScheduleOrchestrator(
            schedulerService = schedulerService,
            circadianEnergyService = circadianService,
            preferencesDataStore = prefsDataStore
        )

        val startDate = LocalDate.of(2026, 11, 20)
        val day2LockedStart = startDate.plusDays(2).atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val futureLocked = SchedulableTask(
            id = "locked_day2",
            title = "Dentist Appointment",
            category = TaskCategory.ADMIN,
            priority = 1,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.LOW,
            isLocked = true,
            scheduledStartMillis = day2LockedStart
        )
        // 4 x 90m = 360m unlocked non-urgent work over 4 days -> smoothing should prevent cramming all into Days 0-1
        val unlockedTasks = (1..4).map { idx ->
            SchedulableTask(
                id = "smooth_$idx",
                title = "Module $idx",
                category = TaskCategory.DEEP_WORK,
                priority = 1,
                estimatedMinutes = 90,
                energyDemand = EnergyDemand.HIGH
            )
        }

        val outcome = orchestrator.orchestrateMultiDaySchedule(
            tasks = listOf(futureLocked) + unlockedTasks,
            startDate = startDate,
            horizonDays = 4,
            zoneId = zoneId,
            referenceNowMillis = startDate.atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli()
        )

        val day0 = outcome.dayOutcomes[startDate]!!
        val day2 = outcome.dayOutcomes[startDate.plusDays(2)]!!
        val day3 = outcome.dayOutcomes[startDate.plusDays(3)]!!

        assertFalse("Future locked task must NOT be falsely deferred on Day 0", day0.deferrals.any { it.taskId == "locked_day2" })
        assertTrue("Future locked task must be placed on Day 2", day2.placements.any { it.task.id == "locked_day2" })
        assertTrue("All 5 tasks should be placed across 4 days", outcome.totalScheduledTasks == 5)
        assertTrue("Load smoothing should distribute unlocked tasks across Day 2/Day 3 instead of cramming only Day 0",
            day3.placements.isNotEmpty() || day2.placements.count { it.task.id.startsWith("smooth_") } > 0
        )
    }
}
