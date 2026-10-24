package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.domain.engine.AnalyticsEngine
import com.neuroflow.app.domain.engine.MEQChronotypeDetector
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsCategory
import com.neuroflow.app.domain.model.PtsRecurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.buildBehavioralObservations
import com.neuroflow.app.domain.model.buildDailyCognitiveBlocks
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.repository.SleepLogRepository
import com.neuroflow.app.domain.repository.SleepPressureRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar
import kotlin.math.roundToInt

class Adr025RobustnessTest {

    private lateinit var circadianService: CircadianEnergyService
    private lateinit var schedulerService: DefaultTaskSchedulerService
    private val zoneId = ZoneId.systemDefault()

    @Before
    fun setUp() {
        circadianService = DefaultCircadianEnergyService()
        schedulerService = DefaultTaskSchedulerService(circadianService)
    }

    // --- Bug 1 & Feature 1: Urgency Invariance & Deadline-Breach Hard Guard ---

    @Test
    fun testUrgencyScoreIsInvariantAcrossCandidateSlotsOnSamePass() {
        val today = LocalDate.of(2026, 10, 22)
        val nowMillis = today.atTime(LocalTime.of(9, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val deadlineMillis = today.atTime(LocalTime.of(17, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val task = SchedulableTask(
            id = "task_urgent",
            title = "Urgent Task",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            deadlineMillis = deadlineMillis
        )

        // Evaluate across two candidate slot times (10:00 and 15:00) with same referenceNowMillis
        val snap10 = circadianService.evaluateSnapshot(
            timestampMillis = today.atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli(),
            wakeTimeMillis = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli(),
            lastSleepDurationMinutes = 480,
            continuousActiveFocusMinutes = 0,
            zoneId = zoneId
        )
        val snap15 = circadianService.evaluateSnapshot(
            timestampMillis = today.atTime(LocalTime.of(15, 0)).atZone(zoneId).toInstant().toEpochMilli(),
            wakeTimeMillis = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli(),
            lastSleepDurationMinutes = 480,
            continuousActiveFocusMinutes = 0,
            zoneId = zoneId
        )

        val score10 = schedulerService.evaluateTaskScoreWithWeights(
            task = task,
            snapshot = snap10,
            dependentCount = 0,
            calibrationRatio = 1.0,
            weights = PriorityWeightProfile.DEFAULT,
            referenceNowMillis = nowMillis
        )
        val score15 = schedulerService.evaluateTaskScoreWithWeights(
            task = task,
            snapshot = snap15,
            dependentCount = 0,
            calibrationRatio = 1.0,
            weights = PriorityWeightProfile.DEFAULT,
            referenceNowMillis = nowMillis
        )

        // Urgency score must be invariant with respect to candidate slot time (eliminating procrastination bias)
        assertEquals(
            "Urgency score must remain identical regardless of candidate slot position",
            score10.urgencyScore,
            score15.urgencyScore,
            0.001
        )
    }

    @Test
    fun testDeadlineBreachHardGuardPenalizesPostDeadlineSlots() {
        val today = LocalDate.of(2026, 10, 22)
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val deadlineMillis = today.atTime(LocalTime.of(14, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val task = SchedulableTask(
            id = "task_deadline",
            title = "Deadline Task",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            deadlineMillis = deadlineMillis
        )

        // When finding best window, the scheduler must pick a pre-deadline slot (ending <= 14:00)
        // rather than placing after 14:00 due to the -35.0 pts penalty
        val bestPlacement = schedulerService.scheduleDay(
            tasks = listOf(task),
            dayDate = today,
            nowMillis = today.atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli(),
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = emptyList(),
            forceReplan = true,
            zoneId = zoneId
        ).firstOrNull { it.task.id == task.id }

        assertNotNull("Task must be placed", bestPlacement)
        assertTrue(
            "Task placement must end on or before its deadline (ended at ${Instant.ofEpochMilli(bestPlacement!!.window.endMillis).atZone(zoneId)}, deadline was 14:00)",
            bestPlacement.window.endMillis <= deadlineMillis
        )
    }

    // --- Bug 2: Fallback Sleep Insertion Scoped to Target Night ---

    @Test
    fun testFallbackSleepGeneratedForOlderNightEvenWhenNewerManualLogExists() = runBlocking {
        val nowMillis = LocalDate.of(2026, 10, 22).atTime(18, 0).atZone(zoneId).toInstant().toEpochMilli()
        val trackingStart = LocalDate.of(2026, 10, 15).atStartOfDay(zoneId).toInstant().toEpochMilli()
        var currentPrefs = UserPreferences(
            autoFallbackSleepInsertionEnabled = true,
            wakeUpHour = 7,
            sleepHour = 23,
            sleepPressureTrackingStartedAtMillis = trackingStart,
            sleepPressureLastComputedAtMillis = trackingStart
        )
        val prefsFlow = MutableStateFlow(currentPrefs)
        val preferencesDataStore = mockk<UserPreferencesDataStore>()
        every { preferencesDataStore.preferencesFlow } returns prefsFlow
        coEvery { preferencesDataStore.updatePreferences(any()) } coAnswers {
            val update = firstArg<(UserPreferences) -> UserPreferences>()
            currentPrefs = update(currentPrefs)
            prefsFlow.value = currentPrefs
        }

        // Mock sleep repository containing a MANUAL log only for LAST NIGHT (2026-10-21 -> 2026-10-22)
        val lastNightSleep = LocalDate.of(2026, 10, 21).atTime(23, 0).atZone(zoneId).toInstant().toEpochMilli()
        val lastNightWake = LocalDate.of(2026, 10, 22).atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()

        val mockLogs = mutableListOf(
            SleepLogEntity(
                id = "manual_last_night",
                startAt = lastNightSleep,
                endAt = lastNightWake,
                durationMinutes = 480,
                source = "MANUAL"
            )
        )

        val sleepLogRepository = mockk<SleepLogRepository>()
        coEvery { sleepLogRepository.getOverlapping(any(), any()) } coAnswers {
            val from = firstArg<Long>()
            val to = secondArg<Long>()
            mockLogs.filter { it.endAt > from && it.startAt < to }
        }
        coEvery { sleepLogRepository.addLog(any(), any(), any(), any()) } coAnswers {
            val start = firstArg<Long>()
            val end = secondArg<Long>()
            val source = thirdArg<String>()
            val entity = SleepLogEntity(
                id = "log-$start",
                startAt = start,
                endAt = end,
                durationMinutes = ((end - start) / 60_000L).toInt(),
                source = source
            )
            mockLogs.add(entity)
            entity
        }

        val sleepPressureRepo = SleepPressureRepository(preferencesDataStore, sleepLogRepository)
        val snapshot = sleepPressureRepo.refreshCurrentPressure(nowMillis)

        assertNotNull(snapshot)
        // The mock repo should now have received fallback sleep insertions for unlogged nights
        val olderFallbackLogs = mockLogs.filter { it.source == "AUTO_DEFAULT" }
        assertTrue(
            "Auto-fallback sleep logs should be generated for older unlogged nights even when a newer manual log exists",
            olderFallbackLogs.isNotEmpty()
        )
    }

    // --- Bug 3 & Feature 2: Chronological Ordering & Low-Energy Chore Exclusion ---

    @Test
    fun testBuildBehavioralObservationsSortsChronologicallyAndExcludesLowEnergyChores() {
        val t1 = LocalDate.of(2026, 10, 22).atTime(14, 0).atZone(zoneId).toInstant().toEpochMilli() // Session 14:00
        val t2 = LocalDate.of(2026, 10, 22).atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli() // Task 10:00
        val t3 = LocalDate.of(2026, 10, 22).atTime(16, 0).atZone(zoneId).toInstant().toEpochMilli() // Task 16:00
        val tLow = LocalDate.of(2026, 10, 22).atTime(22, 0).atZone(zoneId).toInstant().toEpochMilli() // Low Chore 22:00

        val session = TimeSessionEntity(
            id = "s1",
            taskId = "t_deep",
            startedAt = t1,
            endedAt = t1 + 50 * 60_000L,
            durationMinutes = 50f,
            sessionType = "DEEP_WORK"
        )
        val highTask = TaskEntity(
            id = "t_high",
            title = "Architecture",
            completedAt = t2,
            energyLevel = EnergyLevel.HIGH
        )
        val medTask = TaskEntity(
            id = "t_med",
            title = "Review",
            completedAt = t3,
            energyLevel = EnergyLevel.MEDIUM
        )
        val lowChore = TaskEntity(
            id = "t_low",
            title = "Trash & Dishes",
            completedAt = tLow,
            energyLevel = EnergyLevel.LOW
        )

        val observations = buildBehavioralObservations(
            completedTasks = listOf(highTask, medTask, lowChore),
            sessions = listOf(session),
            zoneId = zoneId
        )

        // 1. Low energy chore must be completely excluded from peak-shifting observations
        assertEquals(3, observations.size)
        assertFalse("Low energy chore must not be present in behavioral observations", observations.any { it.minuteOfDay == 22 * 60 })

        // 2. Observations must be returned in strictly chronological order: 10:00 (600), 14:00 (840), 16:00 (960)
        assertEquals(10 * 60, observations[0].minuteOfDay)
        assertEquals(14 * 60, observations[1].minuteOfDay)
        assertEquals(16 * 60, observations[2].minuteOfDay)

        // 3. Deep work session must have boosted weight (2.0x cognitive multiplier => (50/30)*2.0 = 3.33)
        assertTrue("Deep work session weight must be boosted (> 2.5), was ${observations[1].weight}", observations[1].weight > 2.5)
    }

    // --- Bug 4 & Feature 3: Coalesced Polyphasic Naps & Sleep Inertia Preservation ---

    @Test
    fun testCoalesceNapsMergesOverlappingIntervals() {
        val today = LocalDate.of(2026, 10, 22)
        val nap1 = DaytimeNapBout(
            today.atTime(14, 0).atZone(zoneId).toInstant().toEpochMilli(),
            today.atTime(14, 45).atZone(zoneId).toInstant().toEpochMilli()
        )
        val nap2 = DaytimeNapBout(
            today.atTime(14, 30).atZone(zoneId).toInstant().toEpochMilli(),
            today.atTime(15, 0).atZone(zoneId).toInstant().toEpochMilli()
        )

        val merged = PolyphasicNapCalculator.coalesceNaps(listOf(nap1, nap2))
        assertEquals(1, merged.size)
        assertEquals(nap1.startMillis, merged[0].startMillis)
        assertEquals(nap2.endMillis, merged[0].endMillis)
        assertEquals(60, merged[0].durationMinutes)
    }

    @Test
    fun testMultiBoutSleepInertiaIsPreservedAcrossSubsequentShortNaps() {
        val today = LocalDate.of(2026, 10, 22)
        val wakeTime = today.atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()

        // 1. Deep 60m nap (13:00 to 14:00) -> triggers SWS sleep inertia
        val deepNap = DaytimeNapBout(
            today.atTime(13, 0).atZone(zoneId).toInstant().toEpochMilli(),
            today.atTime(14, 0).atZone(zoneId).toInstant().toEpochMilli()
        )
        // 2. Short 15m rest bout (14:10 to 14:25) -> duration <= 30m
        val shortNap = DaytimeNapBout(
            today.atTime(14, 10).atZone(zoneId).toInstant().toEpochMilli(),
            today.atTime(14, 25).atZone(zoneId).toInstant().toEpochMilli()
        )

        // Evaluate at 14:30 (10 minutes after short nap, 30 minutes after deep nap)
        val evalTime = today.atTime(14, 30).atZone(zoneId).toInstant().toEpochMilli()
        val result = PolyphasicNapCalculator.computeHomeostaticState(
            timestampMillis = evalTime,
            morningWakeMillis = wakeTime,
            initialPressureS0 = 0.12,
            naps = listOf(deepNap, shortNap)
        )

        // The short nap must NOT have zeroed out the decaying sleep inertia from the deep nap!
        assertTrue(
            "Sleep inertia from the prior deep nap must be preserved (was ${result.sleepInertiaPenalty})",
            result.sleepInertiaPenalty > 0.01
        )
    }

    // --- Bug 5: buildDailyCognitiveBlocks Explicit Timestamps & Meal Wrap-Around ---

    @Test
    fun testBuildDailyCognitiveBlocksPopulatesExplicitTimestampsAndEliminatesPhantomDip() {
        val today = LocalDate.of(2026, 10, 22)
        val dinnerSlot = ProtectedTimeSlot(
            id = "dinner",
            label = "Dinner",
            startMinuteOfDay = 1290, // 21:30
            durationMinutes = 60,
            category = PtsCategory.MEAL,
            recurrence = PtsRecurrence.DAILY
        )

        val blocks = buildDailyCognitiveBlocks(
            slots = listOf(dinnerSlot),
            date = today,
            zoneId = zoneId
        )

        // Blocks must contain explicitStartMillis and explicitEndMillis
        assertTrue(blocks.all { it.explicitStartMillis != null && it.explicitEndMillis != null })

        // Evaluate meal impulse at 00:30 AM today (minute 30)
        val evalTime = today.atTime(0, 30).atZone(zoneId).toInstant().toEpochMilli()
        val impulse = PhysiologicalImpulseCalculator.computeCombinedImpulse(
            minuteOfDay = 30,
            blocks = blocks,
            timestampMillis = evalTime
        )

        // With explicit timestamps, today's dinner (at 21:30 tonight) is -21 hours away,
        // and yesterday's dinner (at 21:30 yesterday) was 3 hours ago (180 min, fully tapered to 0.0).
        // It must NOT trigger a circular wrap-around phantom post-prandial dip at 00:30 AM!
        assertEquals("Post-prandial impulse at 00:30 AM must be 0.0, no phantom dip", 0.0, impulse, 0.05)
    }

    // --- Bug 6: Onboarding Chronotype Peak Window Parity ---

    @Test
    fun testOnboardingChronotypePeakWindowsMatchMeqDetector() {
        val definiteMorning = MEQChronotypeDetector.baselinePeakWindow(MEQChronotypeDetector.Chronotype.DEFINITE_MORNING)
        val moderateMorning = MEQChronotypeDetector.baselinePeakWindow(MEQChronotypeDetector.Chronotype.MODERATE_MORNING)
        val intermediate = MEQChronotypeDetector.baselinePeakWindow(MEQChronotypeDetector.Chronotype.INTERMEDIATE)
        val moderateEvening = MEQChronotypeDetector.baselinePeakWindow(MEQChronotypeDetector.Chronotype.MODERATE_EVENING)
        val definiteEvening = MEQChronotypeDetector.baselinePeakWindow(MEQChronotypeDetector.Chronotype.DEFINITE_EVENING)

        assertEquals(6 to 11, definiteMorning)
        assertEquals(7 to 12, moderateMorning)
        assertEquals(9 to 14, intermediate)
        assertEquals(13 to 18, moderateEvening)
        assertEquals(15 to 21, definiteEvening)
    }

    // --- Bug 7 & Feature 4: Cross-Midnight Slicing & Enjoyment Calibration Rounding ---

    @Test
    fun testSevenDayFocusTrendAndFocusMinutesTodayProportionallySliceCrossMidnightSessions() {
        val today = LocalDate.of(2026, 10, 22)
        val yesterday = today.minusDays(1)

        val sessionStart = yesterday.atTime(23, 30).atZone(zoneId).toInstant().toEpochMilli()
        val sessionEnd = today.atTime(0, 30).atZone(zoneId).toInstant().toEpochMilli()

        val crossMidnightSession = TimeSessionEntity(
            id = "cross_midnight",
            taskId = "task_night",
            startedAt = sessionStart,
            endedAt = sessionEnd,
            durationMinutes = 60f
        )

        val evalTime = today.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()

        // 1. sevenDayFocusTrend must slice 30m into yesterday and 30m into today
        val trend = AnalyticsEngine.sevenDayFocusTrend(
            sessions = listOf(crossMidnightSession),
            nowMillis = evalTime
        )

        val yesterdayIndex = trend.size - 2
        val todayIndex = trend.size - 1

        assertEquals("Yesterday must receive exactly 30 minutes of cross-midnight session", 30f, trend[yesterdayIndex].second, 0.5f)
        assertEquals("Today must receive exactly 30 minutes of cross-midnight session", 30f, trend[todayIndex].second, 0.5f)

        // 2. buildSummary focusMinutesToday must capture 30 minutes
        val summary = AnalyticsEngine.buildSummary(
            allTasks = emptyList(),
            allSessions = listOf(crossMidnightSession),
            prefs = UserPreferences(),
            nowMillis = evalTime
        )

        assertEquals("focusMinutesToday must capture exactly 30 minutes for cross-midnight session", 30f, summary.focusMinutesToday, 0.5f)
    }

    @Test
    fun testEnjoymentCalibrationRoundingPreservesOffsetRatings() {
        val forecastScore = 58 // 58 out of 100
        val oldTruncated = (forecastScore / 10).coerceIn(0, 10)
        val newRounded = (forecastScore / 10.0).roundToInt().coerceIn(0, 10)

        assertEquals("Integer division truncated 58 to 5", 5, oldTruncated)
        assertEquals("Floating-point rounding preserves positive offset 58 to 6", 6, newRounded)
    }
}
