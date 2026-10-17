package com.neuroflow.app.e2e

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.inference.TaskInferenceEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.e2e.harness.NeuroFlowV2Harness.VoiceAndDecompressionOracle
import org.junit.Assert.*
import org.junit.Test

/**
 * E2E Test Suite 5: Hands-Free Voice Quick-Capture & Executive Transition Buffer
 * Covers:
 * - Speech transcription string piped into TaskInferenceEngine.inferFromInput()
 * - 3-minute (180s) Decompression Zone countdown, sigh guide action, and "Ready Now" bypass
 *
 * Tiers:
 * - Tier 1: Primary behavior & Speech parsing + Decompression state machine
 * - Tier 2: Boundary value analysis & Unintelligible speech + Countdown clamps
 * - Tier 3: Pairwise tag/priority extraction & Dual-countdown synchronization
 * - Tier 4: End-to-end voice-to-decompression scenario & Adversarial noise stress
 */
class NeuroFlowV2VoiceAndDecompressionE2eTest {

    // =========================================================================
    // TIER 1: Primary Behavior & Speech Parsing + Decompression State Machine
    // =========================================================================

    @Test
    fun tier1_testSpeechTranscriptionInferenceHappyPath() {
        val transcript = "Draft quarterly architecture plan 45m #high !frog @work"
        val inferred = VoiceAndDecompressionOracle.pipeSpeechTranscription(transcript)

        assertEquals("Draft quarterly architecture plan", inferred.sanitizedTitle)
        assertEquals(45, inferred.durationMinutes)
        assertEquals(EnergyLevel.HIGH, inferred.energyDemand)
        assertTrue("Frog keyword should mark task as frog", inferred.isFrog)
        assertEquals(Priority.HIGH, inferred.priority)
        assertTrue("Context tags should contain 'work'", inferred.contextTags.contains("work"))
    }

    @Test
    fun tier1_testSpeechSpokenDurationsParsing() {
        // "an hour" -> 60 minutes
        val res1 = VoiceAndDecompressionOracle.pipeSpeechTranscription("Client consultation an hour")
        assertEquals(60, res1.durationMinutes)

        // "half hour" -> 30 minutes
        val res2 = VoiceAndDecompressionOracle.pipeSpeechTranscription("Standup sync half hour")
        assertEquals(30, res2.durationMinutes)

        // "2 hours" -> 120 minutes
        val res3 = VoiceAndDecompressionOracle.pipeSpeechTranscription("Deep coding sprint 2 hours")
        assertEquals(120, res3.durationMinutes)

        // "15 mins" -> 15 minutes
        val res4 = VoiceAndDecompressionOracle.pipeSpeechTranscription("Quick check 15 mins")
        assertEquals(15, res4.durationMinutes)
    }

    @Test
    fun tier1_testDecompressionZoneInitialDuration180Seconds() {
        val engine = VoiceAndDecompressionOracle.DecompressionEngine()
        val state = engine.state

        assertEquals("Decompression Zone must initialize to exactly 180 seconds (3 minutes)", 180, state.remainingSeconds)
        assertFalse(state.isSighActive)
        assertEquals(0, state.sighRemainingSeconds)
        assertFalse(state.isCompleted)
        assertFalse(state.isBypassed)
    }

    @Test
    fun tier1_testDecompressionCountdownTicking() {
        val engine = VoiceAndDecompressionOracle.DecompressionEngine(initialSeconds = 180)
        // Tick 10 seconds
        repeat(10) { engine.tickSecond() }

        assertEquals(170, engine.state.remainingSeconds)
        assertFalse(engine.state.isCompleted)
    }

    @Test
    fun tier1_testPhysiologicalSighProtocolTrigger() {
        val engine = VoiceAndDecompressionOracle.DecompressionEngine()
        engine.triggerPhysiologicalSigh()

        assertTrue("Physiological sigh breathing guide must become active", engine.state.isSighActive)
        assertEquals("Physiological sigh duration must be 60 seconds", 60, engine.state.sighRemainingSeconds)
    }

    @Test
    fun tier1_testReadyNowBypassAction() {
        val engine = VoiceAndDecompressionOracle.DecompressionEngine(initialSeconds = 180)
        // User taps "Ready Now" after 15 seconds
        repeat(15) { engine.tickSecond() }
        assertEquals(165, engine.state.remainingSeconds)

        engine.readyNowBypass()

        assertTrue("Session must be marked bypassed", engine.state.isBypassed)
        assertTrue("Session must be marked completed immediately", engine.state.isCompleted)
        assertEquals("Remaining seconds must drop to 0", 0, engine.state.remainingSeconds)
        assertFalse(engine.state.isSighActive)
    }

    // =========================================================================
    // TIER 2: Boundary Value Analysis & Edge Cases
    // =========================================================================

    @Test
    fun tier2_testSpeechEmptyAndWhitespaceTranscript() {
        // Blank or empty audio transcription
        val inferredEmpty = VoiceAndDecompressionOracle.pipeSpeechTranscription("")
        assertEquals("", inferredEmpty.sanitizedTitle)
        assertEquals(25, inferredEmpty.durationMinutes)
        assertEquals(EnergyLevel.MEDIUM, inferredEmpty.energyDemand)

        val inferredWhitespace = VoiceAndDecompressionOracle.pipeSpeechTranscription("   \n\t  ")
        assertEquals("", inferredWhitespace.sanitizedTitle)
        assertEquals(25, inferredWhitespace.durationMinutes)
    }

    @Test
    fun tier2_testSpeechExtremeDurations() {
        // Extreme short: "5m"
        val inferred5m = VoiceAndDecompressionOracle.pipeSpeechTranscription("Quick check 5m")
        assertEquals(5, inferred5m.durationMinutes)

        // Extreme long: "10 hours" (600m)
        val inferred10h = VoiceAndDecompressionOracle.pipeSpeechTranscription("Marathon hackathon 10 hours")
        assertEquals(600, inferred10h.durationMinutes)
    }

    @Test
    fun tier2_testDecompressionCountdownClampingAtZero() {
        val engine = VoiceAndDecompressionOracle.DecompressionEngine(initialSeconds = 5)
        // Tick 10 times (past 5 seconds)
        repeat(10) { engine.tickSecond() }

        assertEquals("Remaining seconds must clamp to 0 and not become negative", 0, engine.state.remainingSeconds)
        assertTrue("Session must be marked completed", engine.state.isCompleted)
    }

    @Test
    fun tier2_testPhysiologicalSighExpiryAfter60Seconds() {
        val engine = VoiceAndDecompressionOracle.DecompressionEngine(initialSeconds = 180)
        engine.triggerPhysiologicalSigh()

        // Tick 60 seconds
        repeat(60) { engine.tickSecond() }

        assertFalse("Sigh guide must deactivate after 60 seconds", engine.state.isSighActive)
        assertEquals(0, engine.state.sighRemainingSeconds)
        assertEquals(120, engine.state.remainingSeconds) // 180 - 60 = 120s remaining in overall zone
    }

    // =========================================================================
    // TIER 3: Pairwise Interactions & Workflow Integration
    // =========================================================================

    @Test
    fun tier3_testSpeechContextTagsAndPriorityPairwiseInteractions() {
        val combinations = listOf(
            Pair("Write report 30m #finance !p1", Pair("finance", Priority.HIGH)),
            Pair("Team alignment 20m @meeting !p2", Pair("meeting", Priority.MEDIUM)),
            Pair("Clean desk 10m #admin !p3", Pair("admin", Priority.LOW)),
            Pair("System architecture 90m #core !frog", Pair("core", Priority.HIGH))
        )

        for ((input, expected) in combinations) {
            val inferred = VoiceAndDecompressionOracle.pipeSpeechTranscription(input)
            assertTrue("Expected tag ${expected.first} in ${inferred.contextTags}", inferred.contextTags.contains(expected.first))
            assertEquals("Expected priority ${expected.second} for $input", expected.second, inferred.priority)
        }
    }

    @Test
    fun tier3_testDualCountdownSynchronizationSighAndZone() {
        val engine = VoiceAndDecompressionOracle.DecompressionEngine(initialSeconds = 180)
        // User lets 30 seconds elapse, then activates physiological sigh
        repeat(30) { engine.tickSecond() }
        assertEquals(150, engine.state.remainingSeconds)

        engine.triggerPhysiologicalSigh()
        assertEquals(60, engine.state.sighRemainingSeconds)

        // Tick 25 seconds of sigh guide
        repeat(25) { engine.tickSecond() }

        // Overall remaining: 150 - 25 = 125s
        // Sigh remaining: 60 - 25 = 35s
        assertEquals(125, engine.state.remainingSeconds)
        assertEquals(35, engine.state.sighRemainingSeconds)
        assertTrue(engine.state.isSighActive)
    }

    @Test
    fun tier3_testCalibratedDurationLearningWithSpeechInput() {
        // User history demonstrates taking 1.5x longer than estimated
        val completedHistory = listOf(
            TaskEntity(id = "h1", title = "H1", estimatedDurationMinutes = 20, actualDurationMinutes = 30f, status = TaskStatus.COMPLETED),
            TaskEntity(id = "h2", title = "H2", estimatedDurationMinutes = 40, actualDurationMinutes = 60f, status = TaskStatus.COMPLETED),
            TaskEntity(id = "h3", title = "H3", estimatedDurationMinutes = 30, actualDurationMinutes = 45f, status = TaskStatus.COMPLETED),
            TaskEntity(id = "h4", title = "H4", estimatedDurationMinutes = 50, actualDurationMinutes = 75f, status = TaskStatus.COMPLETED),
            TaskEntity(id = "h5", title = "H5", estimatedDurationMinutes = 20, actualDurationMinutes = 30f, status = TaskStatus.COMPLETED),
            TaskEntity(id = "h6", title = "H6", estimatedDurationMinutes = 60, actualDurationMinutes = 90f, status = TaskStatus.COMPLETED)
        )

        val factor = TaskInferenceEngine.calculateCalibrationFactorFromHistory(completedHistory)
        assertTrue("Calibration factor should be around 1.3 (was: $factor)", factor in 1.25f..1.35f)

        // Spoken input says 20m:
        val inferred = TaskInferenceEngine.inferFromInput("Prepare client invoice 20m", calibrationFactor = factor)
        assertEquals(20, inferred.rawDurationMinutes)
        assertTrue("Calibrated duration should be adjusted upwards (was: ${inferred.durationMinutes})",
            inferred.durationMinutes > 20)
    }

    // =========================================================================
    // TIER 4: End-to-End Voice Scenario & Adversarial Noise Stress
    // =========================================================================

    @Test
    fun tier4_testFullVoiceCaptureToDecompressionEndToEndWorkflow() {
        // 1. Spoken task capture
        val transcript = "Ship production patch 1 hour #high !p1 @infra"
        val inferred = VoiceAndDecompressionOracle.pipeSpeechTranscription(transcript)

        assertEquals("Ship production patch", inferred.sanitizedTitle)
        assertEquals(60, inferred.durationMinutes)
        assertEquals(EnergyLevel.HIGH, inferred.energyDemand)
        assertEquals(Priority.HIGH, inferred.priority)
        assertTrue(inferred.contextTags.contains("infra"))

        // 2. Focus session completes -> Decompression Zone triggers
        val decompression = VoiceAndDecompressionOracle.DecompressionEngine(initialSeconds = 180)
        assertEquals(180, decompression.state.remainingSeconds)

        // 3. User rests for 20 seconds
        repeat(20) { decompression.tickSecond() }
        assertEquals(160, decompression.state.remainingSeconds)

        // 4. User starts 60s Physiological Sigh
        decompression.triggerPhysiologicalSigh()
        assertTrue(decompression.state.isSighActive)
        assertEquals(60, decompression.state.sighRemainingSeconds)

        // 5. User performs 40 seconds of guided breathing
        repeat(40) { decompression.tickSecond() }
        assertEquals(20, decompression.state.sighRemainingSeconds)
        assertEquals(120, decompression.state.remainingSeconds)

        // 6. User feels energized and taps "Ready Now" autonomy bypass
        decompression.readyNowBypass()
        assertTrue("Decompression must immediately finish", decompression.state.isCompleted)
        assertTrue("Bypass flag must be recorded", decompression.state.isBypassed)
        assertEquals(0, decompression.state.remainingSeconds)
    }

    @Test
    fun tier4_testAdversarialSpeechTranscriptGarbageAndAudioGlitches() {
        val adversarialTranscripts = listOf(
            "??? !!! ... @@@ #$$% ^^^ &&& ***",
            "uh um like you know basically actually ahhh 45m #high",
            "🚀🔥💡🧠".repeat(50),
            "Word ".repeat(500), // Huge transcription text
            "Mixed 1h 30m 45m 2h all in one sentence",
            "DROP TABLE tasks; SELECT * FROM users;"
        )

        for (transcript in adversarialTranscripts) {
            val inferred = VoiceAndDecompressionOracle.pipeSpeechTranscription(transcript)
            assertNotNull(inferred)
            assertTrue("Duration must remain strictly positive", inferred.durationMinutes > 0)
            assertNotNull(inferred.energyDemand)
            assertNotNull(inferred.taskType)
        }
    }
}
