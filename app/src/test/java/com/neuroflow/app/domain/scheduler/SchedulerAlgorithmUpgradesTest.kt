package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.EnergyDemand
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SchedulerAlgorithmUpgradesTest {

    private val zoneId = ZoneId.of("UTC")
    private val circadianService = DefaultCircadianEnergyService()
    private val schedulerService = DefaultTaskSchedulerService(circadianService)

    @Test
    fun feedbackStore_separatesWeekdayAndWeekendPatterns() {
        val now = LocalDate.of(2026, 11, 21).atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli() // Saturday
        val json = SchedulingFeedbackStore.recordFeedback(
            currentJson = "{}",
            category = "DEEP_WORK",
            hour = 9,
            isNegative = true,
            timestampMillis = now,
            reasonTag = "ROUTINE_CONFLICT",
            isWeekend = true
        )

        val weekendPenalty = SchedulingFeedbackStore.computePenalty(
            json = json,
            category = "DEEP_WORK",
            hour = 9,
            nowMillis = now,
            targetIsWeekend = true
        )
        val weekdayPenalty = SchedulingFeedbackStore.computePenalty(
            json = json,
            category = "DEEP_WORK",
            hour = 9,
            nowMillis = now,
            targetIsWeekend = false
        )

        assertTrue("Weekend penalty ($weekendPenalty) should be much stronger than weekday penalty ($weekdayPenalty)", weekendPenalty > weekdayPenalty * 2.0)
        assertTrue("Weekend penalty should remain substantial (>12.0), was $weekendPenalty", weekendPenalty > 12.0)
    }

    @Test
    fun futureDayScheduling_doesNotPrematurelyDecayTodaysFeedback() {
        val today = LocalDate.of(2026, 11, 16) // Monday
        val futureDay = today.plusDays(7)      // Next Monday (same weekday type)
        val now = today.atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli()
        val wakeFuture = futureDay.atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()

        // Record strong rejection at 9 AM today
        var json = "{}"
        repeat(2) {
            json = SchedulingFeedbackStore.recordFeedback(
                currentJson = json,
                category = "DEEP_WORK",
                hour = 9,
                isNegative = true,
                timestampMillis = now,
                reasonTag = "ROUTINE_CONFLICT",
                isWeekend = false
            )
        }

        val task = SchedulableTask(
            id = "future_deep",
            title = "Architecture Design",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 60,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.HIGH
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(task),
            dayDate = futureDay,
            nowMillis = now,
            wakeTimeMillis = wakeFuture,
            lastSleepDurationMinutes = 480,
            calibrationRatio = 1.0,
            zoneId = zoneId,
            workDayStartHour = 9,
            workDayEndHour = 17,
            rejectionPatternsJson = json
        )

        assertEquals(1, outcome.placements.size)
        val placedHour = Instant.ofEpochMilli(outcome.placements.first().window.startMillis).atZone(zoneId).hour
        assertTrue(
            "Feedback recorded today must remain full-strength on Day +7 and steer away from rejected 9 AM (got $placedHour:00)",
            placedHour != 9
        )
    }

    @Test
    fun needsLongerBlock_prefersSpaciousIntervalAndResistsSplitting() {
        val day = LocalDate.of(2026, 11, 17) // Tuesday
        val now = day.atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli()
        val wake = day.atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()

        var json = "{}"
        repeat(2) {
            json = SchedulingFeedbackStore.recordFeedback(
                currentJson = json,
                category = "DEEP_WORK",
                hour = 14,
                isNegative = true,
                timestampMillis = now,
                reasonTag = "NEEDS_LONGER_BLOCK",
                isWeekend = false
            )
        }

        val pref = SchedulingFeedbackStore.computeLongerBlockPreference(
            json = json,
            category = "DEEP_WORK",
            nowMillis = now,
            targetIsWeekend = false
        )
        assertTrue("NEEDS_LONGER_BLOCK preference should be strong (>0.7), was $pref", pref > 0.7)

        // Two pockets: 09:00..09:45 (45m) and 14:00..15:00 (60m).
        // A 90m task with default fragmentationTolerance = 0.5f cannot fit contiguously.
        // Because user signaled NEEDS_LONGER_BLOCK strongly, scheduler should resist splitting instead of chopping it up.
        val busy1 = TimeSlotWindow(
            day.atTime(9, 45).atZone(zoneId).toInstant().toEpochMilli(),
            day.atTime(14, 0).atZone(zoneId).toInstant().toEpochMilli()
        )
        val busy2 = TimeSlotWindow(
            day.atTime(15, 0).atZone(zoneId).toInstant().toEpochMilli(),
            day.atTime(20, 0).atZone(zoneId).toInstant().toEpochMilli()
        )
        val deepTask = SchedulableTask(
            id = "deep_unsplit",
            title = "Systems Architecture",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 90,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.HIGH,
            fragmentationTolerance = 0.5f
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(deepTask),
            dayDate = day,
            nowMillis = now,
            wakeTimeMillis = wake,
            lastSleepDurationMinutes = 480,
            busyWindows = listOf(busy1, busy2),
            calibrationRatio = 1.0,
            zoneId = zoneId,
            workDayStartHour = 9,
            workDayEndHour = 18,
            rejectionPatternsJson = json
        )

        assertTrue(
            "Task with strong NEEDS_LONGER_BLOCK feedback must resist dynamic splitting (placements=${outcome.placements.map { it.task.id }})",
            outcome.placements.none { it.task.id.contains("#part") }
        )
    }

    @Test
    fun rationale_usesPersonalizedCircadianProfileInsteadOfDefaultMorningCurve() {
        val day = LocalDate.of(2026, 11, 17)
        val now = day.atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli()
        val wake = day.atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()

        // Open window 09:45..11:00; 200mg caffeine at 09:15 elevates personalized phase to SURGE
        // whereas unpersonalized evaluateSnapshot (ignoring stimulants) reports PEAK/MORNING_RAMP.
        val earlyBusy = TimeSlotWindow(
            day.atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli(),
            day.atTime(9, 45).atZone(zoneId).toInstant().toEpochMilli()
        )
        val lateBusy = TimeSlotWindow(
            day.atTime(11, 0).atZone(zoneId).toInstant().toEpochMilli(),
            day.atTime(22, 0).atZone(zoneId).toInstant().toEpochMilli()
        )
        val stimulantDoses = listOf(
            com.neuroflow.app.domain.service.StimulantDoseBout(
                timestampMillis = day.atTime(9, 15).atZone(zoneId).toInstant().toEpochMilli(),
                type = com.neuroflow.app.domain.service.StimulantType.CAFFEINE,
                doseMg = 200.0
            )
        )
        val task = SchedulableTask(
            id = "stim_surge_deep",
            title = "Deep Focus Sprint",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 60,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.HIGH
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(task),
            dayDate = day,
            nowMillis = now,
            wakeTimeMillis = wake,
            lastSleepDurationMinutes = 480,
            busyWindows = listOf(earlyBusy, lateBusy),
            calibrationRatio = 1.0,
            zoneId = zoneId,
            stimulantDoses = stimulantDoses,
            workDayStartHour = 8,
            workDayEndHour = 22
        )

        assertEquals(1, outcome.placements.size)
        val placement = outcome.placements.first()
        val midpoint = placement.window.startMillis + (placement.window.durationMinutes * 30_000L)
        val defaultSnap = circadianService.evaluateSnapshot(
            timestampMillis = midpoint,
            wakeTimeMillis = wake,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )
        val personalizedSnap = circadianService.evaluateSnapshotWithStimulants(
            timestampMillis = midpoint,
            wakeTimeMillis = wake,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            stimulantDoses = stimulantDoses
        )
        val personalizedPhaseLabel = personalizedSnap.phase.name
            .replace('_', ' ')
            .lowercase()
            .replaceFirstChar { it.uppercase() }

        assertTrue(
            "Test setup sanity: default phase (${defaultSnap.phase}) should differ from personalized phase (${personalizedSnap.phase})",
            defaultSnap.phase != personalizedSnap.phase
        )
        assertTrue(
            "Rationale must reflect personalized phase '$personalizedPhaseLabel' instead of default '${defaultSnap.phase}': '${placement.rationale}'",
            placement.rationale.contains(personalizedPhaseLabel, ignoreCase = true)
        )
    }

    @Test
    fun placementStability_enforcesUtilityGainHysteresisForRoutineRefreshes() {
        val now = 1_763_400_000_000L
        val prev = now + 3 * 3_600_000L
        val next = prev + 60 * 60_000L // 60 min shift (> 30 min threshold)

        // Small +2.0 gain on routine refresh must NOT churn the visible future block
        assertFalse(
            PlacementStability.isMoveWorthApplying(
                previousStartMillis = prev,
                nextStartMillis = next,
                nowMillis = now,
                previousUtility = 72.0,
                nextUtility = 74.0,
                bypassUtilityThreshold = false
            )
        )

        // Meaningful +8.0 gain DOES justify moving the block
        assertTrue(
            PlacementStability.isMoveWorthApplying(
                previousStartMillis = prev,
                nextStartMillis = next,
                nowMillis = now,
                previousUtility = 72.0,
                nextUtility = 80.0,
                bypassUtilityThreshold = false
            )
        )

        // Forced/rescue moves bypass the utility threshold
        assertTrue(
            PlacementStability.isMoveWorthApplying(
                previousStartMillis = prev,
                nextStartMillis = next,
                nowMillis = now,
                previousUtility = 72.0,
                nextUtility = 73.0,
                bypassUtilityThreshold = true
            )
        )
    }

    @Test
    fun shiftAndRepackRescue_compactsFlexibleNeighborToFitNonSplittableTaskContiguously() {
        val day = LocalDate.of(2026, 11, 17)
        val now = day.atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli()
        val wake = day.atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()

        // Single open 140m window: 09:00..11:20 (busy after 11:20)
        val afterWindowBusy = TimeSlotWindow(
            day.atTime(11, 20).atZone(zoneId).toInstant().toEpochMilli(),
            day.atTime(20, 0).atZone(zoneId).toInstant().toEpochMilli()
        )

        // High-priority 30m flexible task gets placed FIRST; reject 9 AM so it lands around 10:00..10:30,
        // fragmenting 09:00..11:20 into [09:00..10:00] (60m) and [10:35..11:20] (45m).
        var json = "{}"
        repeat(2) {
            json = SchedulingFeedbackStore.recordFeedback(
                currentJson = json,
                category = "ADMIN",
                hour = 9,
                isNegative = true,
                timestampMillis = now,
                reasonTag = "ROUTINE_CONFLICT",
                isWeekend = false
            )
        }
        val firstFlexTask = SchedulableTask(
            id = "flex_30m",
            title = "Urgent Admin Sync",
            category = TaskCategory.ADMIN,
            priority = 2,
            estimatedMinutes = 30,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.LOW,
            deadlineMillis = day.atTime(11, 20).atZone(zoneId).toInstant().toEpochMilli()
        )
        // Second task: 90m NON-SPLITTABLE deep work (30m + 5m buffer + 90m = 125m <= 140m total!).
        // Without Shift-and-Repack, 60m and 45m pockets both reject 90m and defer it!
        val bigUnsplittableTask = SchedulableTask(
            id = "big_90m",
            title = "Deep Algorithm Proof",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 90,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.HIGH,
            fragmentationTolerance = 0.0f
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(firstFlexTask, bigUnsplittableTask),
            dayDate = day,
            nowMillis = now,
            wakeTimeMillis = wake,
            lastSleepDurationMinutes = 480,
            busyWindows = listOf(afterWindowBusy),
            calibrationRatio = 1.0,
            zoneId = zoneId,
            workDayStartHour = 9,
            workDayEndHour = 18,
            rejectionPatternsJson = json
        )

        assertEquals(
            "Both tasks must be placed contiguously via Shift-and-Repack rescue (deferrals=${outcome.deferrals})",
            2,
            outcome.placements.size
        )
        assertTrue(outcome.placements.any { it.task.id == "big_90m" && it.window.durationMinutes == 90 })
        assertTrue(outcome.placements.any { it.task.id == "flex_30m" && it.window.durationMinutes == 30 })
        assertTrue("No tasks should be deferred", outcome.deferrals.isEmpty())

        // Verify zero overlap between placements
        val sorted = outcome.placements.sortedBy { it.window.startMillis }
        assertTrue(
            "Repacked placements must not overlap",
            sorted[0].window.endMillis <= sorted[1].window.startMillis
        )
    }

    @Test
    fun multiDayBalance_seesCrowdedFutureMeetingsAndShiftsFlexibleLoadToOpenToday() = runTest {
        val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
        coEvery { prefsDataStore.preferencesFlow } returns flowOf(
            UserPreferences(
                wakeTimeHour = 7,
                wakeTimeMinute = 0,
                dailyDeepWorkTargetMinutes = 240,
                workDayStart = 9,
                workDayEnd = 17
            )
        )
        val orchestrator = MultiDayScheduleOrchestrator(
            schedulerService = schedulerService,
            circadianEnergyService = circadianService,
            preferencesDataStore = prefsDataStore
        )

        val day0 = LocalDate.of(2026, 11, 17)
        val day1 = day0.plusDays(1)
        val day0Now = day0.atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli()

        // Day 1 is packed with back-to-back meetings from 09:00 to 16:45 (leaving only 15m unusable sliver)
        val day1AllDayMeeting = TimeSlotWindow(
            day1.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli(),
            day1.atTime(16, 45).atZone(zoneId).toInstant().toEpochMilli()
        )

        // 4 x 55m = 220m flexible tasks over a 2-day horizon where Day 0 is open and Day 1 is fully booked.
        // Calendar-aware Balance should see Day 1 has ~0 open calendar capacity and fit all 220m on Day 0!
        val tasks = (1..4).map { i ->
            SchedulableTask(
                id = "cal_task_$i",
                title = "Flexible Task $i",
                category = TaskCategory.DEEP_WORK,
                priority = 1,
                estimatedMinutes = 55,
                isDurationExplicit = true,
                energyDemand = EnergyDemand.MODERATE
            )
        }

        val outcome = orchestrator.orchestrateMultiDayScheduleWithContext(
            tasks = tasks,
            startDate = day0,
            horizonDays = 2,
            zoneId = zoneId,
            referenceNowMillis = day0Now,
            externalAndFixedBusyWindowsByDate = mapOf(day1 to listOf(day1AllDayMeeting))
        )

        val day0Placed = outcome.dayOutcomes[day0]?.placements.orEmpty()
        assertEquals(
            "Day 0 should absorb all 4 tasks when Day 1 is booked solid with meetings (got ${day0Placed.size}, deferred=${outcome.finalDeferred.size})",
            4,
            day0Placed.size
        )
        assertTrue("No tasks should remain deferred", outcome.finalDeferred.isEmpty())
    }

    @Test
    fun workerHysteresisConflictGuard_flagsCandidateWhenAnotherPlacementClaimsItsOldSlot() {
        val day = LocalDate.of(2026, 11, 17)
        val dayStart = day.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val now = day.atTime(8, 30).atZone(zoneId).toInstant().toEpochMilli()

        val taskA = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "task_a",
            title = "Task A",
            scheduledDate = dayStart,
            scheduledTime = 10 * 3600_000L, // 10:00..11:00
            estimatedDurationMinutes = 60,
            plannedBlockMinutes = 60,
            isAutoScheduled = true
        )
        val taskC = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "task_c",
            title = "Task C",
            scheduledDate = dayStart,
            scheduledTime = 14 * 3600_000L, // 14:00..15:00
            estimatedDurationMinutes = 60,
            plannedBlockMinutes = 60,
            isAutoScheduled = true
        )

        // Fresh solver pass moved Task A to 11:30 and placed Task B into Task A's vacated 10:00..11:00 slot.
        // Task C was proposed at 14:30 with no other task claiming 14:00..15:00.
        val placements = listOf(
            com.neuroflow.app.domain.service.ScheduledPlacement(
                task = SchedulableTask(
                    id = "task_b",
                    title = "Task B",
                    category = TaskCategory.DEEP_WORK,
                    priority = 2,
                    estimatedMinutes = 60,
                    isDurationExplicit = true,
                    energyDemand = EnergyDemand.HIGH
                ),
                window = TimeSlotWindow(
                    day.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli(),
                    day.atTime(11, 0).atZone(zoneId).toInstant().toEpochMilli()
                )
            ),
            com.neuroflow.app.domain.service.ScheduledPlacement(
                task = SchedulableTask(
                    id = "task_a",
                    title = "Task A",
                    category = TaskCategory.ADMIN,
                    priority = 1,
                    estimatedMinutes = 60,
                    isDurationExplicit = true,
                    energyDemand = EnergyDemand.LOW
                ),
                window = TimeSlotWindow(
                    day.atTime(11, 30).atZone(zoneId).toInstant().toEpochMilli(),
                    day.atTime(12, 30).atZone(zoneId).toInstant().toEpochMilli()
                )
            ),
            com.neuroflow.app.domain.service.ScheduledPlacement(
                task = SchedulableTask(
                    id = "task_c",
                    title = "Task C",
                    category = TaskCategory.ADMIN,
                    priority = 1,
                    estimatedMinutes = 60,
                    isDurationExplicit = true,
                    energyDemand = EnergyDemand.LOW
                ),
                window = TimeSlotWindow(
                    day.atTime(14, 30).atZone(zoneId).toInstant().toEpochMilli(),
                    day.atTime(15, 30).atZone(zoneId).toInstant().toEpochMilli()
                )
            )
        )

        val conflicting = com.neuroflow.app.worker.ScheduleAutoTasksWorker.findConflictingPreviousSlotIds(
            replanCandidates = listOf(taskA, taskC),
            placements = placements,
            busyWindows = emptyList(),
            nowMillis = now
        )

        assertTrue("Task A must be marked conflicting because Task B claimed 10:00..11:00", "task_a" in conflicting)
        assertFalse("Task C's old slot 14:00..15:00 has no conflict and stays eligible for hysteresis", "task_c" in conflicting)
    }

    @Test
    fun perCategoryCalibration_scalesDeepWorkAndAdminIndependently() {
        val day = LocalDate.of(2026, 11, 17)
        val now = day.atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli()
        val wake = day.atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()

        val deepTask = SchedulableTask(
            id = "cat_deep",
            title = "Architecture Spec",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 60,
            isDurationExplicit = false,
            energyDemand = EnergyDemand.HIGH
        )
        val adminTask = SchedulableTask(
            id = "cat_admin",
            title = "Expense Receipts",
            category = TaskCategory.ADMIN,
            priority = 1,
            estimatedMinutes = 60,
            isDurationExplicit = false,
            energyDemand = EnergyDemand.LOW
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(deepTask, adminTask),
            dayDate = day,
            nowMillis = now,
            wakeTimeMillis = wake,
            lastSleepDurationMinutes = 480,
            calibrationRatio = 1.0,
            zoneId = zoneId,
            workDayStartHour = 9,
            workDayEndHour = 18,
            categoryCalibrationRatios = mapOf(
                TaskCategory.DEEP_WORK to 1.50,
                TaskCategory.ADMIN to 0.85
            )
        )

        val deepPlacement = outcome.placements.first { it.task.id == "cat_deep" }
        val adminPlacement = outcome.placements.first { it.task.id == "cat_admin" }

        assertEquals("Deep Work should scale by 1.50x (60m -> 90m)", 90, deepPlacement.window.durationMinutes)
        assertEquals("Admin should scale by 0.85x (60m -> 51m)", 51, adminPlacement.window.durationMinutes)
    }

    @Test
    fun twoOpt_preservesAnchoredPlacementsAndSwapsUnequalDurationFlexibleTasksWithSlack() {
        val day = LocalDate.of(2026, 11, 17)
        val now = day.atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli()
        val wake = day.atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()

        val morningAdmin = SchedulableTask(
            id = "swap_admin",
            title = "Morning Admin",
            category = TaskCategory.ADMIN,
            priority = 1,
            estimatedMinutes = 30,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.LOW
        )
        val afternoonDeep = SchedulableTask(
            id = "swap_deep",
            title = "Afternoon Deep Work",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 60,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.HIGH
        )

        // Two free intervals: morning 09:00..10:30 (90m) and afternoon 14:00..15:30 (90m).
        // Admin (30m) starts at 09:45 (leaving 45m slack before it, only 15m after -> naive p1.start+60m overflows 10:30!),
        // while Deep Work (60m) is stuck in the 14:15 afternoon trough.
        val freeBefore = listOf(
            TimeSlotWindow(
                day.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli(),
                day.atTime(10, 30).atZone(zoneId).toInstant().toEpochMilli()
            ),
            TimeSlotWindow(
                day.atTime(14, 0).atZone(zoneId).toInstant().toEpochMilli(),
                day.atTime(15, 30).atZone(zoneId).toInstant().toEpochMilli()
            )
        )
        val initialPlacements = listOf(
            com.neuroflow.app.domain.service.ScheduledPlacement(
                task = morningAdmin,
                window = TimeSlotWindow(
                    day.atTime(9, 45).atZone(zoneId).toInstant().toEpochMilli(),
                    day.atTime(10, 15).atZone(zoneId).toInstant().toEpochMilli()
                )
            ),
            com.neuroflow.app.domain.service.ScheduledPlacement(
                task = afternoonDeep,
                window = TimeSlotWindow(
                    day.atTime(14, 15).atZone(zoneId).toInstant().toEpochMilli(),
                    day.atTime(15, 15).atZone(zoneId).toInstant().toEpochMilli()
                )
            )
        )

        // 1. Anchored protection: when swap_admin is anchored, 2-opt MUST NOT move either task
        val anchoredResult = schedulerService.optimizePlacementsWith2Opt(
            placements = initialPlacements,
            wakeTimeMillis = wake,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            referenceNowMillis = now,
            freeIntervalsBeforePlacement = freeBefore,
            dayStartMillis = day.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli(),
            anchoredPlacementIds = setOf("swap_admin")
        )
        assertEquals(
            "Anchored placement must not be moved by 2-opt",
            initialPlacements.first { it.task.id == "swap_admin" }.window,
            anchoredResult.first { it.task.id == "swap_admin" }.window
        )

        // 2. Flexible slack-aware swap: 60m Deep Work swaps into morning peak (shifting left into slack to fit <= 10:30)
        // while 30m Admin moves to the afternoon trough.
        val optimized = schedulerService.optimizePlacementsWith2Opt(
            placements = initialPlacements,
            wakeTimeMillis = wake,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            referenceNowMillis = now,
            freeIntervalsBeforePlacement = freeBefore,
            dayStartMillis = day.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli()
        )
        val deepAfter = optimized.first { it.task.id == "swap_deep" }
        val adminAfter = optimized.first { it.task.id == "swap_admin" }

        assertTrue(
            "Slack-aware 2-opt should swap 60m Deep Work into the 09:00..10:30 morning peak window (got ${deepAfter.window})",
            deepAfter.window.startMillis >= freeBefore[0].startMillis && deepAfter.window.endMillis <= freeBefore[0].endMillis
        )
        assertTrue(
            "30m Admin should move to the 14:00..15:30 afternoon window (got ${adminAfter.window})",
            adminAfter.window.startMillis >= freeBefore[1].startMillis && adminAfter.window.endMillis <= freeBefore[1].endMillis
        )
    }

    @Test
    fun schedulingContextFactory_extractsPerCategoryCalibrationAndUnifiesDaySignals() {
        val day = LocalDate.of(2026, 11, 17)
        val now = day.atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli()

        // 5 completed Deep Work tasks that consistently took 1.5x longer, plus 5 Admin tasks that took 0.85x
        val historyTasks = (1..5).map { i ->
            com.neuroflow.app.data.local.entity.TaskEntity(
                id = "hist_deep_$i",
                title = "Deep $i",
                status = com.neuroflow.app.domain.model.TaskStatus.COMPLETED,
                taskType = com.neuroflow.app.domain.model.TaskType.ANALYTICAL,
                estimatedDurationMinutes = 60,
                actualDurationMinutes = 90f
            )
        } + (1..5).map { i ->
            com.neuroflow.app.data.local.entity.TaskEntity(
                id = "hist_admin_$i",
                title = "Admin $i",
                status = com.neuroflow.app.domain.model.TaskStatus.COMPLETED,
                taskType = com.neuroflow.app.domain.model.TaskType.ADMIN,
                estimatedDurationMinutes = 60,
                actualDurationMinutes = 51f
            )
        }

        val prefs = UserPreferences(wakeTimeHour = 7, wakeTimeMinute = 30)
        val horizon = SchedulingContextFactory.buildHorizonContext(
            allKnownTasks = historyTasks,
            closedSessions = emptyList(),
            sleepLogs = emptyList(),
            prefs = prefs,
            referenceNowMillis = now,
            zoneId = zoneId
        )
        val dayCtx = SchedulingContextFactory.buildDayContext(
            horizon = horizon,
            dayDate = day,
            prefs = prefs,
            referenceNowMillis = now,
            zoneId = zoneId
        )

        val deepRatio = horizon.calibrationRatioFor(TaskCategory.DEEP_WORK)
        val adminRatio = horizon.calibrationRatioFor(TaskCategory.ADMIN)
        assertTrue(
            "DEEP_WORK category ratio ($deepRatio) should exceed ADMIN category ratio ($adminRatio)",
            deepRatio > adminRatio + 0.15
        )
        assertEquals(
            "Day wake time should honor wakeTimeHour + wakeTimeMinute (07:30)",
            day.atTime(7, 30).atZone(zoneId).toInstant().toEpochMilli(),
            dayCtx.wakeTimeMillis
        )
    }
}

