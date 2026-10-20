package com.neuroflow.app.domain.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeminiAiOnlineServiceTest {

    private lateinit var service: DefaultGeminiAiOnlineService

    @Before
    fun setup() {
        service = DefaultGeminiAiOnlineService()
    }

    @Test
    fun testModelIdIsGeminiFlashLatest() {
        assertEquals("gemini-flash-latest", DefaultGeminiAiOnlineService.MODEL_ID)
        assertTrue(
            "Endpoint must target gemini-flash-latest model",
            DefaultGeminiAiOnlineService.ENDPOINT_BASE.contains("gemini-flash-latest")
        )
    }

    @Test
    fun testLocalFallbackWhenApiKeyIsBlankOrAiDisabled() = runBlocking {
        // When AI is disabled, local fallback must return valid, non-null, robust results
        val enrichment = service.enrichTaskWithAi(
            title = "Refactor Database Schema",
            durationMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            currentHour = 10,
            isFrog = true,
            apiKey = "",
            useAi = false
        )
        assertNotNull(enrichment)
        assertTrue("Micro-steps must not be empty", enrichment.microSteps.isNotEmpty())
        assertEquals(10, enrichment.suggestedSlotStartHour) // High demand / Frog placed in morning peak
        assertEquals(EnergyDemand.HIGH, enrichment.recommendedEnergyDemand)

        // Task decomposition fallback
        val steps = service.decomposeTask(
            title = "Write quarterly performance report",
            durationMinutes = 45,
            apiKey = null,
            useAi = true // No key provided
        )
        assertTrue("Decomposition fallback must yield steps", steps.size >= 3)
        assertTrue("First step must have low initiation friction (<= 10m)", steps.first().estimatedMinutes <= 10)
    }

    @Test
    fun testSlotRecommendationFallback() = runBlocking {
        val window = TimeSlotWindow(1700000000000L, 1700003600000L)
        val rec = service.suggestTaskSlot(
            taskTitle = "Deep Coding",
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            isFrog = true,
            currentEpochMillis = 1700000000000L,
            candidateWindows = listOf(window),
            apiKey = "",
            useAi = false
        )
        assertNotNull(rec)
        assertEquals(window.startMillis, rec.suggestedStartMillis)
        assertEquals(window.endMillis, rec.suggestedEndMillis)
        assertEquals("Morning Peak", rec.circadianPhase)
        assertTrue(rec.confidence >= 80)
    }

    @Test
    fun testEnergyAdvisoryCircadianLogic() = runBlocking {
        // Peak phase
        val peakAdvice = service.adviseEnergyControl(
            phase = EnergyPhase.PEAK,
            usableEnergyPercent = 88,
            hoursAwake = 3.5f,
            continuousFocusMinutes = 45,
            hasSleepDebt = false,
            apiKey = "",
            useAi = false
        )
        assertEquals("Biological Peak Window", peakAdvice.statusTitle)
        assertEquals("DEEP_WORK", peakAdvice.suggestedCategory)
        assertEquals(EnergyDemand.HIGH, peakAdvice.suggestedEnergyDemand)
        assertFalse(peakAdvice.powerNapRecommended)

        // Dip phase
        val dipAdvice = service.adviseEnergyControl(
            phase = EnergyPhase.DIP,
            usableEnergyPercent = 42,
            hoursAwake = 7.0f,
            continuousFocusMinutes = 30,
            hasSleepDebt = false,
            apiKey = "",
            useAi = false
        )
        assertEquals("Circadian Post-Lunch Dip", dipAdvice.statusTitle)
        assertEquals("ADMIN", dipAdvice.suggestedCategory)
        assertTrue("Power nap recommended during dip", dipAdvice.powerNapRecommended)

        // Continuous focus >= 90 mins synaptic depletion
        val depletionAdvice = service.adviseEnergyControl(
            phase = EnergyPhase.PEAK,
            usableEnergyPercent = 75,
            hoursAwake = 4.0f,
            continuousFocusMinutes = 95,
            hasSleepDebt = false,
            apiKey = "",
            useAi = false
        )
        assertEquals("Synaptic Saturation Warning", depletionAdvice.statusTitle)
        assertEquals("RECOVERY", depletionAdvice.suggestedCategory)
        assertEquals(EnergyDemand.LOW, depletionAdvice.suggestedEnergyDemand)
    }

    @Test
    fun testWeeklyAvoidanceAuditAndDirectiveFallback() = runBlocking {
        val result = service.conductWeeklyAuditAndDirective(
            weeklyTelemetryJson = "{}",
            recentlyDeletedTasksJson = "[]",
            chronicallyPostponedTasksJson = "[]",
            apiKey = "",
            useAi = false
        )
        assertNotNull(result)
        assertTrue("Directive must not be empty", result.newSystemDirective.isNotBlank())
        assertTrue("Directive must mention Adaptive System Directive", result.newSystemDirective.contains("Adaptive System Directive"))
        assertTrue("Audit JSON must contain primaryAvoidancePattern", result.avoidanceAuditJson.contains("primaryAvoidancePattern"))
    }

    @Test
    fun testSynthesizeQuickNoteFallback() = runBlocking {
        val synthesis = service.synthesizeQuickNote(
            rawContent = "Buy milk and groceries\nMake sure to get almond milk",
            apiKey = "",
            useAi = false
        )
        assertEquals("Buy milk and groceries", synthesis.extractedTaskTitle)
        assertEquals("GENERAL", synthesis.suggestedCategory)
        assertTrue("Tags must contain quick-note", synthesis.tags.contains("quick-note"))
    }

    @Test
    fun testCandidateModelsContainsLatestAndFallback() {
        val models = DefaultGeminiAiOnlineService.CANDIDATE_MODELS
        assertTrue("Primary must be gemini-flash-latest", models.contains("gemini-flash-latest"))
        assertTrue("Must have flash-lite fallback", models.contains("gemini-flash-lite-latest"))
        assertTrue("Must have 3.5-flash fallback", models.contains("gemini-3.5-flash"))
    }

    @Test
    fun testTestConnectionBlankKeyFailsFast() = runBlocking {
        val result = service.testConnection("")
        assertTrue("Blank key must fail", result.isFailure)
    }
}
