package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.PlacementStability
import com.neuroflow.app.domain.scheduler.SchedulePreparationOrchestrator
import com.neuroflow.app.domain.scheduler.SchedulingFeedbackStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class Adr031RobustnessTest {

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

    @Test
    fun testSchedulingOutcome_guaranteesZeroOverlapsInPlacements() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.of(2026, 11, 14)
        val nowMillis = today.atTime(LocalTime.of(8, 0)).atZone(zone).toInstant().toEpochMilli()
        val wakeMillis = today.atTime(LocalTime.of(7, 0)).atZone(zone).toInstant().toEpochMilli()

        // 4 long tasks that would force splitting or tight placement
        val tasks = listOf(
            SchedulableTask(
                id = "task-1",
                title = "Deep Coding Architecture",
                category = TaskCategory.DEEP_WORK,
                priority = 2,
                estimatedMinutes = 150,
                energyDemand = EnergyDemand.HIGH,
                fragmentationTolerance = 0.5f
            ),
            SchedulableTask(
                id = "task-2",
                title = "Client Presentation Review",
                category = TaskCategory.ADMIN,
                priority = 2,
                estimatedMinutes = 90,
                energyDemand = EnergyDemand.MODERATE,
                fragmentationTolerance = 0.5f
            ),
            SchedulableTask(
                id = "task-3",
                title = "Financial Budgeting",
                category = TaskCategory.DEEP_WORK,
                priority = 1,
                estimatedMinutes = 120,
                energyDemand = EnergyDemand.HIGH,
                fragmentationTolerance = 0.5f
            ),
            SchedulableTask(
                id = "task-4",
                title = "Email Inbox Zero",
                category = TaskCategory.ADMIN,
                priority = 0,
                estimatedMinutes = 45,
                energyDemand = EnergyDemand.LOW
            )
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = tasks,
            dayDate = today,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeMillis,
            lastSleepDurationMinutes = 480,
            zoneId = zone
        )

        val placements = outcome.placements
        assertTrue("Placements must not be empty", placements.isNotEmpty())

        // Invariant: No two placements may overlap in time
        for (i in 0 until placements.size - 1) {
            val current = placements[i]
            val next = placements[i + 1]
            assertTrue(
                "Placement '${current.task.title}' (${current.window.startMillis}..${current.window.endMillis}) " +
                    "must not overlap with '${next.task.title}' (${next.window.startMillis}..${next.window.endMillis})",
                current.window.endMillis <= next.window.startMillis
            )
        }
    }

    @Test
    fun testSchedulingFeedbackStore_recordFeedbackAndComputePenalty() {
        val now = 1731580000000L
        var json = "{}"

        // Dislike 9 AM coding placement
        json = SchedulingFeedbackStore.recordFeedback(
            currentJson = json,
            category = "HARD_WORK",
            hour = 9,
            isNegative = true,
            timestampMillis = now
        )

        val penaltyAt9 = SchedulingFeedbackStore.computePenalty(
            json = json,
            category = "HARD_WORK",
            hour = 9,
            nowMillis = now
        )
        val penaltyAt14 = SchedulingFeedbackStore.computePenalty(
            json = json,
            category = "HARD_WORK",
            hour = 14,
            nowMillis = now
        )

        assertTrue("Penalty at disliked 9 AM must be >= 9.0, was $penaltyAt9", penaltyAt9 >= 9.0)
        assertEquals("Penalty at unrated 14 PM must be 0.0", 0.0, penaltyAt14, 0.001)

        // Approve 14 PM placement
        json = SchedulingFeedbackStore.recordFeedback(
            currentJson = json,
            category = "HARD_WORK",
            hour = 14,
            isNegative = false,
            timestampMillis = now
        )

        val penaltyAt14AfterApproval = SchedulingFeedbackStore.computePenalty(
            json = json,
            category = "HARD_WORK",
            hour = 14,
            nowMillis = now
        )
        assertEquals("Approved slot penalty should remain 0.0", 0.0, penaltyAt14AfterApproval, 0.001)

        val summary = SchedulingFeedbackStore.formatSummaryForLlm(json, nowMillis = now)
        assertTrue("Summary must mention HARD_WORK at 9AM", summary.contains("HARD_WORK at 9AM"))
    }

    @Test
    fun testPlacementStability_rejectionRescueTriggersImmediateReplan() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.of(2026, 11, 14)
        val testStartMillis = today.atTime(LocalTime.of(9, 30)).atZone(zone).toInstant().toEpochMilli()
        val nowMillis = today.atTime(LocalTime.of(9, 0)).atZone(zone).toInstant().toEpochMilli()

        val prefs = UserPreferences(autoSchedulingEnabled = true, isAutoScheduleToggleOn = true)

        // Normal task within 2 hours would be NEAR_TERM_FREEZE
        val normalTask = TaskEntity(
            id = "task-normal",
            title = "Normal Task",
            status = TaskStatus.ACTIVE,
            scheduledDate = today.atStartOfDay(zone).toInstant().toEpochMilli(),
            scheduledTime = 9 * 3600_000L + 30 * 60_000L,
            isAutoScheduled = true
        )
        val normalVerdict = PlacementStability.verdict(normalTask, nowMillis, prefs, forceReplan = false)
        assertEquals(PlacementStability.ReplanVerdict.NEAR_TERM_FREEZE, normalVerdict)
        assertFalse(normalVerdict.allowsReplan)

        // Disliked placement with userSchedulingRating = -1 at hour 9
        val dislikedTask = normalTask.copy(
            userSchedulingRating = -1,
            schedulingRejectedSlotHour = 9
        )
        val dislikedVerdict = PlacementStability.verdict(dislikedTask, nowMillis, prefs, forceReplan = false)
        assertEquals(PlacementStability.ReplanVerdict.REJECTION_RESCUE, dislikedVerdict)
        assertTrue("Disliked task must allow replan immediately", dislikedVerdict.allowsReplan)
    }

    @Test
    fun testConfidenceScoreAndRationalePopulated() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.of(2026, 11, 14)
        val nowMillis = today.atTime(LocalTime.of(8, 0)).atZone(zone).toInstant().toEpochMilli()
        val wakeMillis = today.atTime(LocalTime.of(7, 0)).atZone(zone).toInstant().toEpochMilli()

        val task = SchedulableTask(
            id = "task-conf",
            title = "Focus Architecture Review",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH
        )

        val outcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(task),
            dayDate = today,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeMillis,
            lastSleepDurationMinutes = 480,
            zoneId = zone
        )

        val placement = outcome.placements.firstOrNull()
        assertNotNull("Placement must be generated", placement)
        assertTrue(
            "Confidence score must be in [0.1..1.0], was ${placement!!.confidenceScore}",
            placement.confidenceScore in 0.1f..1.0f
        )
        assertTrue(
            "Rationale must not be blank",
            placement.rationale.isNotBlank()
        )
    }

    @Test
    fun testRejectionPenalty_steersSchedulerToAlternateWindow() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.of(2026, 11, 14)
        val nowMillis = today.atTime(LocalTime.of(8, 0)).atZone(zone).toInstant().toEpochMilli()
        val wakeMillis = today.atTime(LocalTime.of(7, 0)).atZone(zone).toInstant().toEpochMilli()

        val task = SchedulableTask(
            id = "task-rejection-steer",
            title = "Heavy Calculation Work",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH
        )

        // Baseline run without rejection
        val baseline = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(task),
            dayDate = today,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeMillis,
            lastSleepDurationMinutes = 480,
            zoneId = zone
        )
        val baselinePlacement = baseline.placements.first()
        val baselineStartHour = ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(baselinePlacement.window.startMillis),
            zone
        ).hour

        // Now inject heavy historical rejection at baseline's start hour
        var feedbackJson = "{}"
        for (i in 1..3) {
            feedbackJson = SchedulingFeedbackStore.recordFeedback(
                currentJson = feedbackJson,
                category = "DEEP_WORK",
                hour = baselineStartHour,
                isNegative = true,
                timestampMillis = nowMillis - (i * 3600_000L)
            )
        }

        // Run with rejection feedback and direct task rejection
        val dislikedTask = task.copy(
            userSchedulingRating = -1,
            schedulingRejectedSlotHour = baselineStartHour
        )
        val penalizedOutcome = schedulerService.scheduleDayWithOutcome(
            tasks = listOf(dislikedTask),
            dayDate = today,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeMillis,
            lastSleepDurationMinutes = 480,
            zoneId = zone,
            rejectionPatternsJson = feedbackJson
        )

        val penalizedPlacement = penalizedOutcome.placements.first()
        val penalizedStartHour = ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(penalizedPlacement.window.startMillis),
            zone
        ).hour

        assertTrue(
            "Penalized placement start hour ($penalizedStartHour) must avoid rejected hour ($baselineStartHour)",
            penalizedStartHour != baselineStartHour
        )
    }
}
