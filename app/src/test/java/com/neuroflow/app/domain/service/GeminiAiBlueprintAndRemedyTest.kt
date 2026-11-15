package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.scheduler.SchedulingFeedbackStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GeminiAiBlueprintAndRemedyTest {
    private val service = GeminiAiOnlineService()

    @Test
    fun testGenerateDailyBlueprintCoaching_offlineFallbackProducesStructuredCoaching() = runBlocking {
        val tasks = listOf(
            TaskEntity(
                id = "1",
                title = "Deep Architecture",
                estimatedDurationMinutes = 60,
                energyLevel = EnergyLevel.HIGH,
                quadrant = Quadrant.DO_FIRST,
                isFrog = true
            )
        )
        val result = service.generateDailyBlueprintCoaching(
            tasks = tasks,
            plannedMinutes = 60,
            capacityMinutes = 240,
            peakStartHour = 9,
            peakEndHour = 12,
            apiKey = "",
            userDirective = ""
        )
        assertNotNull(result)
        assertTrue(result.headline.isNotBlank())
        assertTrue(result.strategyBody.isNotBlank())
        assertTrue(result.peakRecommendation.isNotBlank())
    }

    @Test
    fun testRemedyRejectedSlot_offlineFallbackProposesAlternativeSlot() = runBlocking {
        val task = TaskEntity(
            id = "1",
            title = "Write Spec",
            estimatedDurationMinutes = 45,
            energyLevel = EnergyLevel.HIGH,
            quadrant = Quadrant.SCHEDULE
        )
        val freeWindows = listOf(
            TimeSlotWindow(1000L + 9 * 3600_000L, 1000L + 11 * 3600_000L),
            TimeSlotWindow(1000L + 14 * 3600_000L, 1000L + 16 * 3600_000L)
        )
        val remedy = service.remedyRejectedSlot(
            task = task,
            rejectedStartHour = 14,
            reasonTag = "TOO_LOW_ENERGY",
            availableWindows = freeWindows,
            apiKey = "",
            userDirective = ""
        )
        assertNotNull(remedy)
        assertTrue(remedy.recommendedStartMillis in freeWindows.first().startMillis..freeWindows.first().endMillis)
        assertTrue(remedy.reasoning.contains("energy", ignoreCase = true) || remedy.reasoning.contains("peak", ignoreCase = true))
    }

    @Test
    fun testRemedyRejectedSlot_offlineFallbackHandlesDifferentReasonTags() = runBlocking {
        val task = TaskEntity(
            id = "2",
            title = "Heavy Refactoring",
            estimatedDurationMinutes = 90,
            energyLevel = EnergyLevel.HIGH,
            quadrant = Quadrant.DO_FIRST
        )
        val freeWindows = listOf(
            TimeSlotWindow(1000L + 8 * 3600_000L, 1000L + 9 * 3600_000L), // 60m
            TimeSlotWindow(1000L + 10 * 3600_000L, 1000L + 12 * 3600_000L), // 120m
            TimeSlotWindow(1000L + 15 * 3600_000L, 1000L + 18 * 3600_000L) // 180m
        )

        // Case 1: NEEDS_LONGER_BLOCK should pick the largest available window (180m at 15:00)
        val remedyBlock = service.remedyRejectedSlot(
            task = task,
            rejectedStartHour = 8,
            reasonTag = "NEEDS_LONGER_BLOCK",
            availableWindows = freeWindows,
            apiKey = "",
            userDirective = ""
        )
        assertEquals(freeWindows[2].startMillis, remedyBlock.recommendedStartMillis)
        assertTrue(remedyBlock.reasoning.contains("block", ignoreCase = true) || remedyBlock.reasoning.contains("contiguous", ignoreCase = true))

        // Case 2: TOO_EARLY should pick a later slot
        val remedyEarly = service.remedyRejectedSlot(
            task = task,
            rejectedStartHour = 8,
            reasonTag = "TOO_EARLY",
            availableWindows = freeWindows,
            apiKey = "",
            userDirective = ""
        )
        assertTrue(remedyEarly.recommendedStartMillis > freeWindows[0].startMillis)
    }

    @Test
    fun testSchedulingFeedbackStore_persistsReasonTagBackwardCompatible() {
        val json = SchedulingFeedbackStore.recordFeedback(
            currentJson = "{}",
            category = "DEEP_WORK",
            hour = 14,
            isNegative = true,
            reasonTag = "TOO_LOW_ENERGY"
        )
        assertTrue(json.contains("TOO_LOW_ENERGY"))
        val penalty = SchedulingFeedbackStore.computePenalty(json, "DEEP_WORK", 14)
        assertTrue(penalty > 0.0)

        // Backward compatibility: record without reasonTag
        val jsonWithoutTag = SchedulingFeedbackStore.recordFeedback(
            currentJson = json,
            category = "DEEP_WORK",
            hour = 10,
            isNegative = false
        )
        assertFalse(jsonWithoutTag.contains("\"tag\":null"))
        val penaltyAt10 = SchedulingFeedbackStore.computePenalty(jsonWithoutTag, "DEEP_WORK", 10)
        assertEquals(0.0, penaltyAt10, 0.001)
    }

    @Test
    fun testSchedulingFeedbackStore_formatSummaryIncludesReasonTags() {
        val now = 1731580000000L
        var json = "{}"
        json = SchedulingFeedbackStore.recordFeedback(
            currentJson = json,
            category = "DEEP_WORK",
            hour = 14,
            isNegative = true,
            timestampMillis = now,
            reasonTag = "TOO_LOW_ENERGY"
        )
        val summary = SchedulingFeedbackStore.formatSummaryForLlm(json, nowMillis = now)
        assertTrue(summary.contains("DEEP_WORK at 2PM"))
        assertTrue(summary.contains("TOO_LOW_ENERGY"))
    }

    @Test
    fun testRemedyRejectedSlot_routineConflictPicksDifferentHour() = runBlocking {
        val task = TaskEntity(
            id = "3",
            title = "Team Sync Prep",
            estimatedDurationMinutes = 30,
            energyLevel = EnergyLevel.MEDIUM,
            quadrant = Quadrant.SCHEDULE
        )
        val freeWindows = listOf(
            TimeSlotWindow(1000L + 12 * 3600_000L, 1000L + 13 * 3600_000L), // Lunch hour (12:00)
            TimeSlotWindow(1000L + 15 * 3600_000L, 1000L + 16 * 3600_000L)  // Afternoon (15:00)
        )
        val remedy = service.remedyRejectedSlot(
            task = task,
            rejectedStartHour = 12,
            reasonTag = "ROUTINE_CONFLICT",
            availableWindows = freeWindows,
            apiKey = "",
            userDirective = ""
        )
        assertEquals(freeWindows[1].startMillis, remedy.recommendedStartMillis)
        assertTrue(remedy.reasoning.contains("routine", ignoreCase = true))
    }
}
