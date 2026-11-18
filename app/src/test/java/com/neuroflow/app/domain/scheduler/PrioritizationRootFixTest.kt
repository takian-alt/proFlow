package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.DailyCircadianSummary
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.domain.service.GeminiAiOnlineService
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.ScheduledPlacement
import com.neuroflow.app.domain.service.SchedulingOutcome
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.domain.service.TaskScoreBreakdown
import com.neuroflow.app.domain.service.TimeSlotWindow
import com.neuroflow.app.presentation.v2.flow.FlowViewModelV2
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class PrioritizationRootFixTest {

    private val testDispatcher = StandardTestDispatcher()
    private val zoneId = ZoneId.of("UTC")
    private val circadianService = DefaultCircadianEnergyService()
    private val schedulerService = DefaultTaskSchedulerService(circadianService)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `explicit Quadrant DO_FIRST outranks DELEGATE with identical priority and impact`() {
        val day = LocalDate.of(2026, 11, 18)
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

        val doFirst = SchedulableTask(
            id = "do_first",
            title = "Critical Architecture Fix",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            impactScore = 50,
            quadrant = Quadrant.DO_FIRST,
            createdAtMillis = now
        )
        val delegate = doFirst.copy(
            id = "delegate",
            title = "Delegable Sync",
            quadrant = Quadrant.DELEGATE
        )

        val doFirstScore = schedulerService.evaluateTaskScoreWithFatigue(doFirst, snapshot, calibrationRatio = 1.0)
        val delegateScore = schedulerService.evaluateTaskScoreWithFatigue(delegate, snapshot, calibrationRatio = 1.0)

        assertTrue(
            "DO_FIRST (${doFirstScore.totalScore}) must clearly outrank DELEGATE (${delegateScore.totalScore})",
            doFirstScore.totalScore > delegateScore.totalScore + 8.0
        )
    }

    @Test
    fun `overdue child task does not poison unblocked prerequisite parent out of day schedule`() {
        val day = LocalDate.of(2026, 11, 18)
        val wake = day.atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()
        val now = day.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli()
        val yesterdayOverdue = day.minusDays(1).atTime(17, 0).atZone(zoneId).toInstant().toEpochMilli()

        val parent = SchedulableTask(
            id = "parent_blocker",
            title = "Unblock Data Migration",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            isDurationExplicit = true,
            deadlineMillis = null
        )
        val overdueChild = SchedulableTask(
            id = "child_overdue",
            title = "Submit Migration Report",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            isDurationExplicit = true,
            deadlineMillis = yesterdayOverdue,
            prerequisiteIds = setOf("parent_blocker")
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(parent, overdueChild),
            dayDate = day,
            nowMillis = now,
            wakeTimeMillis = wake,
            lastSleepDurationMinutes = 480,
            calibrationRatio = 1.0,
            zoneId = zoneId,
            workDayStartHour = 9,
            workDayEndHour = 18
        )

        assertTrue("Parent blocker must be placed despite overdue child", outcome.placements.any { it.task.id == "parent_blocker" })
        assertTrue("Overdue child must also be placed after parent", outcome.placements.any { it.task.id == "child_overdue" })
        val pEnd = outcome.placements.first { it.task.id == "parent_blocker" }.window.endMillis
        val cStart = outcome.placements.first { it.task.id == "child_overdue" }.window.startMillis
        assertTrue("Parent ($pEnd) must finish before child ($cStart)", pEnd <= cStart)
    }

    @Test
    fun `afternoon dip pocket does not let trivial low chore steal tight budget from HIGH task when rebound exists`() {
        val day = LocalDate.of(2026, 11, 18)
        val wake = day.atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()
        // Afternoon dip at 13:30..14:30 followed by rebound 16:00..18:00
        val now = day.atTime(13, 30).atZone(zoneId).toInstant().toEpochMilli()

        val trivialChore = SchedulableTask(
            id = "low_chore",
            title = "Clear Inbox",
            category = TaskCategory.ADMIN,
            priority = 0,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.LOW,
            quadrant = Quadrant.DELEGATE,
            impactScore = 20,
            isDurationExplicit = true
        )
        val deepFeature = SchedulableTask(
            id = "high_feature",
            title = "Ship Core Payment Engine",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            isFrog = true,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            quadrant = Quadrant.DO_FIRST,
            impactScore = 90,
            isDurationExplicit = true
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(trivialChore, deepFeature),
            dayDate = day,
            nowMillis = now,
            wakeTimeMillis = wake,
            lastSleepDurationMinutes = 480,
            calibrationRatio = 1.0,
            zoneId = zoneId,
            workDayStartHour = 9,
            workDayEndHour = 19,
            dailyCognitiveCapacityMinutes = 50
        )

        assertEquals(1, outcome.placements.size)
        assertEquals("high_feature", outcome.placements.first().task.id)
    }

    @Test
    fun `MultiDayScheduleOrchestrator admits higher priority placements even when filler sits earlier in timeline`() = runTest {
        val today = LocalDate.of(2026, 11, 18)
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val mockScheduler = mockk<TaskSchedulerService>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        coEvery { prefsDataStore.preferencesFlow } returns flowOf(
            UserPreferences(wakeTimeHour = 7, wakeTimeMinute = 0, dailyDeepWorkTargetMinutes = 120)
        )

        val morningFiller = SchedulableTask(
            id = "filler",
            title = "Minor Cleanup",
            category = TaskCategory.ADMIN,
            priority = 0,
            estimatedMinutes = 90,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.LOW,
            quadrant = Quadrant.DELEGATE
        )
        val afternoonCritical = SchedulableTask(
            id = "critical",
            title = "Security Patch",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 90,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.HIGH,
            isFrog = true,
            deadlineMillis = dayStart + 18 * 3600_000L,
            quadrant = Quadrant.DO_FIRST
        )

        val pFiller = ScheduledPlacement(
            task = morningFiller,
            window = TimeSlotWindow(dayStart + 9 * 3600_000L, dayStart + 10 * 3600_000L + 30 * 60_000L),
            scoreBreakdown = TaskScoreBreakdown("filler", 45.0, 20.0, 20.0, 50.0, 10.0, 90)
        )
        val pCritical = ScheduledPlacement(
            task = afternoonCritical,
            window = TimeSlotWindow(dayStart + 14 * 3600_000L, dayStart + 15 * 3600_000L + 30 * 60_000L),
            scoreBreakdown = TaskScoreBreakdown("critical", 88.0, 85.0, 90.0, 80.0, 50.0, 90)
        )

        every {
            mockScheduler.scheduleDayWithOutcome(
                tasks = any(), dayDate = today, nowMillis = any(), wakeTimeMillis = any(),
                lastSleepDurationMinutes = any(), busyWindows = any(), protectedSlots = any(),
                calibrationRatio = any(), forceReplan = any(), zoneId = any(), naps = any(),
                chronotype = any(), manualPeakStartHour = any(), priorityWeights = any(),
                learnedProfile = any(), rollingAverageSleepMinutes = any(), physiologicalBlocks = any(),
                stimulantDoses = any(), socialJetlagLagHours = any(), activeUncompletedTaskIds = any(),
                workDayStartHour = any(), workDayEndHour = any(), dailyCognitiveCapacityMinutes = any(),
                rejectionPatternsJson = any()
            )
        } returns SchedulingOutcome(listOf(pFiller, pCritical))

        val orchestrator = MultiDayScheduleOrchestrator(
            schedulerService = mockScheduler,
            circadianEnergyService = circadianService,
            preferencesDataStore = prefsDataStore
        )

        val outcome = orchestrator.orchestrateMultiDaySchedule(
            tasks = listOf(morningFiller, afternoonCritical),
            startDate = today,
            horizonDays = 2,
            zoneId = zoneId
        )

        val day0 = outcome.dayOutcomes[today]?.placements.orEmpty()
        assertEquals(1, day0.size)
        assertEquals("critical", day0.first().task.id)
    }

    @Test
    fun `DailyBlueprintOrchestrator keeps DO_FIRST task over earlier DELEGATE filler under tight capacity`() = runTest {
        val today = LocalDate.now(zoneId)
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val mockScheduler = mockk<TaskSchedulerService>(relaxed = true)
        val mockCircadian = mockk<CircadianEnergyService>(relaxed = true)
        val aiService = mockk<GeminiAiOnlineService>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        coEvery { prefsDataStore.preferencesFlow } returns flowOf(UserPreferences(dailyDeepWorkTargetMinutes = 120))
        every {
            mockCircadian.computeDailySummary(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns DailyCircadianSummary(
            currentSnapshot = CircadianSnapshot(dayStart, 80, 0.2, 0.8, 0.0, EnergyPhase.PEAK),
            peakWindowStartHour = 9, peakWindowEndHour = 12, peakEnergyScore = 85,
            dipWindowStartHour = 13, dipWindowEndHour = 15, recommendedDeepWorkMinutes = 120
        )
        every { mockCircadian.evaluateSnapshot(any(), any(), any(), any(), any()) } returns
            CircadianSnapshot(dayStart + 10 * 3600_000L, 80, 0.2, 0.8, 0.0, EnergyPhase.PEAK)

        val filler = TaskEntity(id = "filler", title = "Low Priority Admin", estimatedDurationMinutes = 90, quadrant = Quadrant.DELEGATE, priority = Priority.LOW)
        val critical = TaskEntity(id = "critical", title = "Urgent Release", estimatedDurationMinutes = 90, quadrant = Quadrant.DO_FIRST, priority = Priority.HIGH, isFrog = true)
        coEvery { taskRepo.getActiveTasks() } returns listOf(filler, critical)

        val pFiller = ScheduledPlacement(
            task = SchedulableTask("filler", filler.title, durationMinutes = 90),
            window = TimeSlotWindow(dayStart + 9 * 3600_000L, dayStart + 10 * 3600_000L + 30 * 60_000L),
            scoreBreakdown = TaskScoreBreakdown("filler", 40.0, 20.0, 20.0, 50.0, 10.0, 90)
        )
        val pCritical = ScheduledPlacement(
            task = SchedulableTask("critical", critical.title, durationMinutes = 90),
            window = TimeSlotWindow(dayStart + 11 * 3600_000L, dayStart + 12 * 3600_000L + 30 * 60_000L),
            scoreBreakdown = TaskScoreBreakdown("critical", 90.0, 85.0, 95.0, 85.0, 40.0, 90)
        )
        every {
            mockScheduler.scheduleDayWithOutcome(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any()
            )
        } returns SchedulingOutcome(listOf(pFiller, pCritical))

        val orchestrator = DailyBlueprintOrchestrator(
            taskRepository = taskRepo,
            schedulerService = mockScheduler,
            circadianEnergyService = mockCircadian,
            geminiAiOnlineService = aiService,
            preferencesDataStore = prefsDataStore
        )

        val blueprint = orchestrator.generateBlueprint(dayStart)
        assertEquals(1, blueprint.items.size)
        assertEquals("critical", blueprint.items.first().task.id)
        assertEquals(listOf("filler"), blueprint.overflowTasks.map { it.id })
    }

    @Test
    fun `FlowViewModelV2 surfaces urgent DO_FIRST HIGH task even during MEDIUM energy window`() = runTest(testDispatcher) {
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

        val today = LocalDate.of(2026, 11, 18)
        val nowMillis = today.atTime(LocalTime.of(14, 0)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val trivialMedium = TaskEntity(
            id = "trivial_med",
            title = "Tidy Notes",
            quadrant = Quadrant.DELEGATE,
            priority = Priority.LOW,
            energyLevel = EnergyLevel.MEDIUM,
            estimatedDurationMinutes = 30,
            impactScore = 20,
            status = TaskStatus.ACTIVE
        )
        val urgentHigh = TaskEntity(
            id = "urgent_high",
            title = "Production Hotfix Due Soon",
            quadrant = Quadrant.DO_FIRST,
            priority = Priority.HIGH,
            isFrog = true,
            energyLevel = EnergyLevel.HIGH,
            estimatedDurationMinutes = 45,
            impactScore = 95,
            deadlineDate = nowMillis + 90 * 60_000L,
            status = TaskStatus.ACTIVE
        )

        every { taskRepo.observeActiveTasks() } returns MutableStateFlow(listOf(trivialMedium, urgentHigh))
        every { taskRepo.observeCompletedTasks() } returns MutableStateFlow(emptyList())
        every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences(wakeTimeHour = 7, wakeTimeMinute = 0))

        val vm = FlowViewModelV2(
            taskRepository = taskRepo,
            preferencesDataStore = prefsDataStore,
            taskSchedulerService = DefaultTaskSchedulerService(DefaultCircadianEnergyService()),
            nowProvider = { nowMillis }
        )

        val state = vm.uiState.filter { !it.isLoading && it.recommendedTask != null }.first()
        assertEquals("urgent_high", state.recommendedTask?.id)
    }

    @Test
    fun `waitingFor external blocker yields tight daily capacity to actionable unblocked task`() {
        val day = LocalDate.of(2026, 11, 18)
        val wake = day.atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()
        val now = day.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli()

        val waitingTask = TaskEntity(
            id = "waiting_task",
            title = "Finalize Vendor Contract",
            quadrant = Quadrant.DO_FIRST,
            priority = Priority.HIGH,
            energyLevel = EnergyLevel.HIGH,
            estimatedDurationMinutes = 45,
            impactScore = 70,
            waitingFor = "Legal team review email"
        )
        val actionableTask = TaskEntity(
            id = "actionable_task",
            title = "Implement Checkout Retry",
            quadrant = Quadrant.DO_FIRST,
            priority = Priority.HIGH,
            energyLevel = EnergyLevel.HIGH,
            estimatedDurationMinutes = 45,
            impactScore = 70
        )

        val sWaiting = SchedulableTaskMapper.toSchedulableTask(waitingTask, dayStartMillis = day.atStartOfDay(zoneId).toInstant().toEpochMilli())
        val sActionable = SchedulableTaskMapper.toSchedulableTask(actionableTask, dayStartMillis = day.atStartOfDay(zoneId).toInstant().toEpochMilli())

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(sWaiting, sActionable),
            dayDate = day,
            nowMillis = now,
            wakeTimeMillis = wake,
            lastSleepDurationMinutes = 480,
            calibrationRatio = 1.0,
            zoneId = zoneId,
            workDayStartHour = 9,
            workDayEndHour = 18,
            dailyCognitiveCapacityMinutes = 50
        )

        assertEquals(1, outcome.placements.size)
        assertEquals("actionable_task", outcome.placements.first().task.id)
    }

    @Test
    fun `goalRiskLevel publicCommitment habitStreak and affectiveForecastError increase score cleanly`() {
        val day = LocalDate.of(2026, 11, 18)
        val dayStart = day.atStartOfDay(zoneId).toInstant().toEpochMilli()
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

        val baselineEntity = TaskEntity(
            id = "baseline_task",
            title = "Routine Review",
            quadrant = Quadrant.SCHEDULE,
            priority = Priority.MEDIUM,
            estimatedDurationMinutes = 30,
            createdAt = now
        )
        val enrichedEntity = baselineEntity.copy(
            id = "enriched_task",
            goalId = "goal-q4",
            goalRiskLevel = 2,
            isPublicCommitment = true,
            isHabitual = true,
            habitStreak = 10,
            sessionCount = 2,
            affectiveForecastError = 25f
        )

        val baselineScore = schedulerService.evaluateTaskScoreWithFatigue(
            SchedulableTaskMapper.toSchedulableTask(baselineEntity, dayStartMillis = dayStart),
            snapshot,
            calibrationRatio = 1.0,
            referenceNowMillis = now
        )
        val enrichedScore = schedulerService.evaluateTaskScoreWithFatigue(
            SchedulableTaskMapper.toSchedulableTask(enrichedEntity, dayStartMillis = dayStart),
            snapshot,
            calibrationRatio = 1.0,
            referenceNowMillis = now
        )

        assertTrue(
            "Goal-risk/commitment/habit-streak task (${enrichedScore.totalScore}) must clearly outrank baseline (${baselineScore.totalScore})",
            enrichedScore.totalScore > baselineScore.totalScore + 6.0
        )
    }

    @Test
    fun `task committed to today via preferredDayStartMillis outranks uncommitted identical task under tight budget`() {
        val day = LocalDate.of(2026, 11, 18)
        val dayStart = day.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val wake = day.atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()
        val now = day.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli()

        val uncommitted = SchedulableTask(
            id = "backlog_peer",
            title = "Unscheduled Backlog Work",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 45,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.HIGH,
            preferredDayStartMillis = null
        )
        val committedToday = uncommitted.copy(
            id = "today_committed",
            title = "Today Committed Milestone",
            preferredDayStartMillis = dayStart
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(uncommitted, committedToday),
            dayDate = day,
            nowMillis = now,
            wakeTimeMillis = wake,
            lastSleepDurationMinutes = 480,
            calibrationRatio = 1.0,
            zoneId = zoneId,
            workDayStartHour = 9,
            workDayEndHour = 18,
            dailyCognitiveCapacityMinutes = 50
        )

        assertEquals(1, outcome.placements.size)
        assertEquals("today_committed", outcome.placements.first().task.id)
    }
}
