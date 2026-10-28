package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.engine.AnalyticsEngine
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsCategory
import com.neuroflow.app.domain.model.PtsRecurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.extractStimulantDosesFromSlots
import com.neuroflow.app.domain.repository.SleepPressureRepository
import com.neuroflow.app.domain.scheduler.ConflictResolver
import com.neuroflow.app.domain.scheduler.ProtectedSlotClock
import com.neuroflow.app.domain.scheduler.SchedulePreparationOrchestrator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class Adr030RobustnessTest {

    private lateinit var circadianService: CircadianEnergyService
    private lateinit var schedulerService: DefaultTaskSchedulerService
    private lateinit var orchestrator: SchedulePreparationOrchestrator

    @Before
    fun setUp() {
        circadianService = DefaultCircadianEnergyService()
        schedulerService = DefaultTaskSchedulerService(circadianService)
        orchestrator = SchedulePreparationOrchestrator(
            taskSchedulerService = schedulerService,
            behavioralSupportService = DefaultBehavioralSupportService()
        )
    }

    // =========================================================================
    // 1. CognitiveBlockWindow.toTimeSlotWindow Explicit Timestamps (Bug 1)
    // =========================================================================

    @Test
    fun testCognitiveBlockWindow_toTimeSlotWindow_respectsExplicitTimestamps() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.of(2026, 10, 23)
        val yesterdayMillis = today.minusDays(1).atStartOfDay(zone).plusMinutes(20 * 60).toInstant().toEpochMilli()
        val yesterdayEndMillis = yesterdayMillis + 45 * 60_000L

        val explicitBlock = CognitiveBlockWindow(
            label = "Dinner Yesterday",
            category = CognitiveBlockCategory.MEAL,
            startMinuteOfDay = 20 * 60,
            endMinuteOfDay = 20 * 60 + 45,
            explicitStartMillis = yesterdayMillis,
            explicitEndMillis = yesterdayEndMillis
        )

        val window = explicitBlock.toTimeSlotWindow(today, zone)
        assertEquals("Must return explicitStartMillis directly", yesterdayMillis, window.startMillis)
        assertEquals("Must return explicitEndMillis directly", yesterdayEndMillis, window.endMillis)
    }

    @Test
    fun testCognitiveBlockWindow_toTimeSlotWindow_fallsBackToDateWhenExplicitNull() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.of(2026, 10, 23)

        val standardBlock = CognitiveBlockWindow(
            label = "Lunch Today",
            category = CognitiveBlockCategory.MEAL,
            startMinuteOfDay = 12 * 60,
            endMinuteOfDay = 12 * 60 + 45,
            explicitStartMillis = null,
            explicitEndMillis = null
        )

        val window = standardBlock.toTimeSlotWindow(today, zone)
        val expectedStart = today.atStartOfDay(zone).plusMinutes(12 * 60).toInstant().toEpochMilli()
        val expectedEnd = expectedStart + 45 * 60_000L

        assertEquals("Should compute start from targetDate and startMinuteOfDay", expectedStart, window.startMillis)
        assertEquals("Should compute end from start + duration", expectedEnd, window.endMillis)
    }

    // =========================================================================
    // 2. Bedtime SWS Suppression Accounts for Sleep-Onset Latency (Bug 2)
    // =========================================================================

    @Test
    fun testPredictBedtimeSwsSuppression_accountsForSleepOnsetLatency() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.of(2026, 10, 23)
        val bedtime = today.atStartOfDay(zone).plusMinutes(23 * 60).toInstant().toEpochMilli()

        // Dose taken right at bedtime (23:00) — peak concentration happens during sleep-onset latency (23:30–23:45)
        val bedtimeDose = StimulantDoseBout(
            timestampMillis = bedtime,
            doseMg = 150.0,
            type = StimulantType.CAFFEINE
        )

        val suppression = PharmacokineticStimulantCalculator.predictBedtimeSwsSuppression(
            plannedSleepMillis = bedtime,
            doses = listOf(bedtimeDose)
        )

        assertTrue(
            "Suppression factor must be significantly positive for bedtime dose evaluated across sleep-onset latency",
            suppression > 0.15
        )
    }

    // =========================================================================
    // 3. Sweep-Line Coalesced Sleep Intervals in SleepPressureRepository (Bug 5)
    // =========================================================================

    @Test
    fun testSleepPressureRepository_sweepLineCoalescesContiguousIntervals() {
        val baseMillis = 1700000000000L
        // Two contiguous 45m sleep intervals (0m to 45m, and 45m to 90m) totaling a 90m SWS cycle
        val interval1 = SleepPressureRepository.SleepInterval(baseMillis, baseMillis + 45 * 60_000L)
        val interval2 = SleepPressureRepository.SleepInterval(baseMillis + 45 * 60_000L, baseMillis + 90 * 60_000L)

        val merged = SleepPressureRepository.coalesceSleepIntervals(listOf(interval1, interval2))
        assertEquals("Contiguous intervals must be coalesced into 1 interval", 1, merged.size)
        assertEquals(baseMillis, merged[0].startAt)
        assertEquals(baseMillis + 90 * 60_000L, merged[0].endAt)
    }

    @Test
    fun testSleepPressureRepository_sweepLineCoalescesOverlappingIntervals() {
        val baseMillis = 1700000000000L
        // Two overlapping intervals (0m to 50m, and 40m to 90m)
        val interval1 = SleepPressureRepository.SleepInterval(baseMillis, baseMillis + 50 * 60_000L)
        val interval2 = SleepPressureRepository.SleepInterval(baseMillis + 40 * 60_000L, baseMillis + 90 * 60_000L)

        val merged = SleepPressureRepository.coalesceSleepIntervals(listOf(interval1, interval2))
        assertEquals("Overlapping intervals must be coalesced into 1 interval", 1, merged.size)
        assertEquals(baseMillis, merged[0].startAt)
        assertEquals(baseMillis + 90 * 60_000L, merged[0].endAt)
    }

    // =========================================================================
    // 4. Workday Start Clamping in AutoSchedulingRationales (Bug 6)
    // =========================================================================

    @Test
    fun testFindConflictFreeStart_clampsCandidateToWorkdayStart() {
        val searchStartMinute = 480 // 8:00 AM
        val durationMinutes = 30
        val workDayStartHour = 9 // 9:00 AM (540m)
        val workDayEndHour = 17  // 5:00 PM (1020m)
        val busyIntervals = emptyList<Pair<Int, Int>>()

        val conflictFreeStart = ConflictResolver.findConflictFreeStart(
            searchStartMinute = searchStartMinute,
            duration = durationMinutes,
            workDayStartHour = workDayStartHour,
            workDayEndHour = workDayEndHour,
            occupiedIntervals = busyIntervals
        )

        assertNotNull("Should find a conflict-free start", conflictFreeStart)
        assertTrue(
            "Conflict free start must be >= workDayStart (540m)",
            conflictFreeStart!! >= 540
        )
        assertEquals(540, conflictFreeStart)
    }

    // =========================================================================
    // 5. DST-Safe Stimulant Doses Extraction (Bug 4)
    // =========================================================================

    @Test
    fun testExtractStimulantDosesFromSlots_resolvesDstWallClockSafely() {
        val zone = ZoneId.systemDefault()
        val testDate = LocalDate.of(2026, 10, 23)

        val coffeeSlot = ProtectedTimeSlot(
            id = "pts-coffee",
            label = "Morning Coffee",
            category = PtsCategory.BREAK,
            startMinuteOfDay = 9 * 60 + 30, // 9:30 AM
            durationMinutes = 15,
            recurrence = PtsRecurrence.DAILY,
            isEnabled = true
        )

        val doses = extractStimulantDosesFromSlots(listOf(coffeeSlot), testDate, zone)
        assertTrue("Doses should not be empty", doses.isNotEmpty())
        val todayDose = doses.first { it.timestampMillis >= testDate.atStartOfDay(zone).toInstant().toEpochMilli() }
        val expectedEpoch = testDate.atStartOfDay(zone).plusMinutes(9 * 60 + 30).toInstant().toEpochMilli()
        assertEquals("Epoch timestamp must match date.atTime wall clock resolution", expectedEpoch, todayDose.timestampMillis)
    }

    // =========================================================================
    // 6. AnalyticsEngine Clamps Future Timestamps (Bug 7)
    // =========================================================================

    @Test
    fun testAnalyticsEngine_clampsFutureCompletedTasks() {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.of(LocalDate.of(2026, 10, 23), LocalTime.of(14, 0), zone).toInstant().toEpochMilli()
        val futureMillis = now + 3600_000L // 1 hour in the future

        val validTask = TaskEntity(
            id = "valid-task",
            title = "Completed Earlier",
            status = TaskStatus.COMPLETED,
            completedAt = now - 1800_000L, // 30 min ago
            estimatedDurationMinutes = 30
        )

        val futureTask = TaskEntity(
            id = "future-task",
            title = "Ghost Future Completion",
            status = TaskStatus.COMPLETED,
            completedAt = futureMillis,
            estimatedDurationMinutes = 45
        )

        val summary = AnalyticsEngine.buildSummary(
            allTasks = listOf(validTask, futureTask),
            allSessions = emptyList(),
            prefs = UserPreferences(),
            nowMillis = now
        )

        assertEquals("Only past/current completed tasks today must be counted", 1, summary.completedToday)
    }

    // =========================================================================
    // 7. ProtectedSlotClock.approaching Custom Lead-in Window
    // =========================================================================

    @Test
    fun testProtectedSlotClock_approachingSupportsCustomLeadIn() {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.of(LocalDate.of(2026, 10, 23), LocalTime.of(12, 0), zone).toInstant().toEpochMilli()

        // Slot at 12:35 (starts in 35 min)
        val slot35m = ProtectedTimeSlot(
            id = "slot-35m",
            label = "Lunch Walk",
            category = PtsCategory.EXERCISE,
            startMinuteOfDay = 12 * 60 + 35,
            durationMinutes = 30,
            recurrence = PtsRecurrence.DAILY,
            isEnabled = true
        )

        // Slot at 13:00 (starts in 60 min)
        val slot60m = ProtectedTimeSlot(
            id = "slot-60m",
            label = "Afternoon Call",
            category = PtsCategory.WORK,
            startMinuteOfDay = 13 * 60,
            durationMinutes = 30,
            recurrence = PtsRecurrence.DAILY,
            isEnabled = true
        )

        val approaching45m = ProtectedSlotClock.approaching(listOf(slot35m, slot60m), now, leadInMinutes = 45)
        assertEquals("Only slot starting in 35m falls within 45m lead-in", 1, approaching45m.size)
        assertEquals("slot-35m", approaching45m[0].slot.id)

        val approaching90m = ProtectedSlotClock.approaching(listOf(slot35m, slot60m), now, leadInMinutes = 90)
        assertEquals("Both slots fall within 90m lead-in", 2, approaching90m.size)
    }
}
