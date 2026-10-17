package com.neuroflow.app.e2e

import com.neuroflow.app.e2e.harness.NeuroFlowV2Harness.FocusTimingOracle
import org.junit.Assert.*
import org.junit.Test

/**
 * E2E Test Suite 3: Drift-Proof Background Focus & Wall-Clock Timing Engine
 * Covers:
 * - Wall-clock timing engine (startedAtMillis, accumulatedPauseMillis, pausedAtMillis)
 * - Drift-proof calculation across simulated Doze mode / screen-off time jumps
 * - Single and multiple pause accumulation
 * - Stopwatch mode vs Pomodoro mode
 *
 * Tiers:
 * - Tier 1: Primary behavior & Wall-clock calculation formulas
 * - Tier 2: Boundary value analysis & Simulated Doze screen-off time jumps
 * - Tier 3: Pairwise multi-pause accumulation & Mode interactions
 * - Tier 4: Drift comparison against coroutine ticks & Rapid-toggle stress
 */
class NeuroFlowV2FocusDriftE2eTest {

    // Reference epoch timestamp: 1,700,000,000,000 ms (~Nov 2023)
    private val t0 = 1_700_000_000_000L

    // =========================================================================
    // TIER 1: Primary Behavior & Wall-Clock Calculation Formulas
    // =========================================================================

    @Test
    fun tier1_testFreshPomodoroSessionInitialState() {
        // 25-minute Pomodoro (1500 seconds)
        val timing = FocusTimingOracle.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = null,
            nowMillis = t0,
            targetDurationSeconds = 1500L
        )

        assertEquals("At start, elapsed seconds must be 0", 0L, timing.elapsedSeconds)
        assertEquals("At start, remaining seconds must be target duration", 1500L, timing.remainingSeconds)
        assertFalse("At start, session must not be expired", timing.isExpired)
    }

    @Test
    fun tier1_testRunningSessionElapsedSecondsProgression() {
        // Run for 180 seconds (3 minutes)
        val tNow = t0 + 180_000L
        val timing = FocusTimingOracle.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = null,
            nowMillis = tNow,
            targetDurationSeconds = 1500L
        )

        assertEquals("Elapsed seconds must equal 180", 180L, timing.elapsedSeconds)
        assertEquals("Remaining seconds must equal 1500 - 180 = 1320", 1320L, timing.remainingSeconds)
        assertFalse(timing.isExpired)
    }

    @Test
    fun tier1_testPauseFreezesElapsedTimeAccurately() {
        // User pauses after 300 seconds (5 minutes)
        val tPaused = t0 + 300_000L
        // Current wall-clock has advanced 20 minutes (1200 seconds) since start
        val tNow = t0 + 1_200_000L

        val timing = FocusTimingOracle.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = tPaused,
            nowMillis = tNow,
            targetDurationSeconds = 1500L
        )

        assertEquals("Elapsed time must remain frozen at exactly 300s while paused", 300L, timing.elapsedSeconds)
        assertEquals("Remaining time must remain frozen at 1200s while paused", 1200L, timing.remainingSeconds)
        assertFalse(timing.isExpired)
    }

    @Test
    fun tier1_testResumeAddsPauseDurationToAccumulatedPauseMillis() {
        // Paused at t0 + 300s, resumed at t0 + 900s (paused for 600s / 10m)
        val tPaused = t0 + 300_000L
        val tResume = t0 + 900_000L

        val (newAccumulated, newPausedAt) = FocusTimingOracle.onResume(
            pausedAtMillis = tPaused,
            accumulatedPauseMillis = 0L,
            nowMillis = tResume
        )

        assertEquals("Accumulated pause must be 600,000 ms", 600_000L, newAccumulated)
        assertNull("Paused timestamp must be cleared to null on resume", newPausedAt)

        // Now run for an additional 100 seconds (at tResume + 100s = t0 + 1000s)
        val tNow = tResume + 100_000L
        val timing = FocusTimingOracle.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = newAccumulated,
            pausedAtMillis = null,
            nowMillis = tNow,
            targetDurationSeconds = 1500L
        )

        // Total elapsed active focus: 300s + 100s = 400s
        assertEquals("Total elapsed focus must be 400s", 400L, timing.elapsedSeconds)
        assertEquals("Remaining must be 1500 - 400 = 1100s", 1100L, timing.remainingSeconds)
        assertFalse(timing.isExpired)
    }

    @Test
    fun tier1_testPomodoroExpirationExactDetection() {
        // Exactly reached 1500 seconds
        val tExpired = t0 + 1500_000L
        val timing = FocusTimingOracle.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = null,
            nowMillis = tExpired,
            targetDurationSeconds = 1500L
        )

        assertEquals(1500L, timing.elapsedSeconds)
        assertEquals(0L, timing.remainingSeconds)
        assertTrue("Session must be marked expired at exactly target duration", timing.isExpired)
    }

    @Test
    fun tier1_testStopwatchModeZeroRemainingAndInfiniteRun() {
        // Target duration = 0 denotes open Flow Stopwatch
        val tNow = t0 + 7200_000L // 2 hours later
        val timing = FocusTimingOracle.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = null,
            nowMillis = tNow,
            targetDurationSeconds = 0L
        )

        assertEquals("Elapsed seconds must be 7200", 7200L, timing.elapsedSeconds)
        assertEquals("Stopwatch remaining seconds must always be 0", 0L, timing.remainingSeconds)
        assertFalse("Stopwatch mode never expires", timing.isExpired)
    }

    // =========================================================================
    // TIER 2: Boundary Value Analysis & Simulated Doze Screen-Off Time Jumps
    // =========================================================================

    @Test
    fun tier2_testSimulatedDozeModeScreenOffJump25Minutes() {
        // Device goes into Android Doze / lock screen deep sleep for 25 minutes (1,500,000 ms)
        // Coroutine delays would freeze, but wall-clock evaluation evaluates against system epoch.
        val dozeDurationMillis = 25 * 60_000L
        val tAfterDoze = t0 + dozeDurationMillis

        val timing = FocusTimingOracle.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = null,
            nowMillis = tAfterDoze,
            targetDurationSeconds = 1500L // 25m Pomodoro
        )

        assertEquals("Elapsed time after Doze must be exactly 1500s (25 minutes)", 1500L, timing.elapsedSeconds)
        assertEquals("Remaining time after Doze must be exactly 0s", 0L, timing.remainingSeconds)
        assertTrue("Pomodoro must be marked expired immediately upon screen wake", timing.isExpired)
    }

    @Test
    fun tier2_testSimulatedDozeModeWhilePausedPreservesPauseState() {
        // Device sleeps for 2 hours while the user has paused at minute 10 (600s)
        val tPaused = t0 + 600_000L
        val tAfterDoze = t0 + 7_200_000L // 2 hours later

        val timing = FocusTimingOracle.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = tPaused,
            nowMillis = tAfterDoze,
            targetDurationSeconds = 1500L
        )

        assertEquals("Elapsed focus time must remain frozen at exactly 600s", 600L, timing.elapsedSeconds)
        assertEquals("Remaining focus time must remain 900s", 900L, timing.remainingSeconds)
        assertFalse("Paused session must not expire during Doze", timing.isExpired)
    }

    @Test
    fun tier2_testNegativeClockJumpResilienceNtpSync() {
        // NTP sync or manual clock setting jumps backwards before startedAt
        val tBackward = t0 - 50_000L
        val timing = FocusTimingOracle.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = null,
            nowMillis = tBackward,
            targetDurationSeconds = 1500L
        )

        assertEquals("Negative clock jump must clamp elapsed to 0 without crashing", 0L, timing.elapsedSeconds)
        assertEquals("Remaining must remain 1500", 1500L, timing.remainingSeconds)
        assertFalse(timing.isExpired)
    }

    @Test
    fun tier2_testTargetDurationBoundary1SecondAnd24Hours() {
        // 1-second dopamine micro-start target
        val timing1s = FocusTimingOracle.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = null,
            nowMillis = t0 + 1000L,
            targetDurationSeconds = 1L
        )
        assertEquals(1L, timing1s.elapsedSeconds)
        assertEquals(0L, timing1s.remainingSeconds)
        assertTrue(timing1s.isExpired)

        // 24-hour ultra session target (86400s)
        val timing24h = FocusTimingOracle.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = null,
            nowMillis = t0 + 3600_000L, // 1 hour elapsed
            targetDurationSeconds = 86400L
        )
        assertEquals(3600L, timing24h.elapsedSeconds)
        assertEquals(86400L - 3600L, timing24h.remainingSeconds)
        assertFalse(timing24h.isExpired)
    }

    @Test
    fun tier2_testImmediateZeroDurationPauseResume() {
        // Immediate pause and instant resume (0 ms elapsed pause)
        val (accumulated, pausedAt) = FocusTimingOracle.onResume(
            pausedAtMillis = t0 + 100_000L,
            accumulatedPauseMillis = 50_000L,
            nowMillis = t0 + 100_000L
        )
        assertEquals("Accumulated pause remains 50,000ms", 50_000L, accumulated)
        assertNull(pausedAt)
    }

    // =========================================================================
    // TIER 3: Pairwise Multi-Pause Accumulation & Mode Interactions
    // =========================================================================

    @Test
    fun tier3_testMultiplePauseAccumulationLifecycle() {
        // Sequence:
        // 1. Run 100s (0..100s)
        // 2. Pause 1 for 50s (100..150s)
        // 3. Run 200s (150..350s)
        // 4. Pause 2 for 80s (350..430s)
        // 5. Run 300s (430..730s)
        // Expected: Total running = 600s, Total paused = 130s, Total wall-clock = 730s.

        var currentAccumulatedPause = 0L

        // Pause 1
        val pause1Start = t0 + 100_000L
        val resume1Time = t0 + 150_000L
        val res1 = FocusTimingOracle.onResume(pause1Start, currentAccumulatedPause, resume1Time)
        currentAccumulatedPause = res1.first
        assertEquals(50_000L, currentAccumulatedPause)

        // Pause 2
        val pause2Start = t0 + 350_000L
        val resume2Time = t0 + 430_000L
        val res2 = FocusTimingOracle.onResume(pause2Start, currentAccumulatedPause, resume2Time)
        currentAccumulatedPause = res2.first
        assertEquals(130_000L, currentAccumulatedPause)

        // Final measurement at t0 + 730s
        val tFinal = t0 + 730_000L
        val timing = FocusTimingOracle.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = currentAccumulatedPause,
            pausedAtMillis = null,
            nowMillis = tFinal,
            targetDurationSeconds = 1500L
        )

        assertEquals("Total active elapsed focus time must be 600s", 600L, timing.elapsedSeconds)
        assertEquals("Remaining must be 1500 - 600 = 900s", 900L, timing.remainingSeconds)
        assertFalse(timing.isExpired)
    }

    @Test
    fun tier3_testStopwatchModeWithMultiplePauses() {
        // Stopwatch mode with 2 pauses
        val accumulatedPause = 120_000L // 2 minutes total paused
        val tNow = t0 + 600_000L // 10 minutes total wall-clock time

        val timing = FocusTimingOracle.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = accumulatedPause,
            pausedAtMillis = null,
            nowMillis = tNow,
            targetDurationSeconds = 0L // Stopwatch
        )

        // 10m - 2m = 8m (480s)
        assertEquals(480L, timing.elapsedSeconds)
        assertEquals(0L, timing.remainingSeconds)
        assertFalse(timing.isExpired)
    }

    @Test
    fun tier3_testPomodoroOvertimeBeyondTarget() {
        // User continues working past the 25m target (30m total)
        val tOvertime = t0 + 1800_000L // 30 minutes
        val timing = FocusTimingOracle.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = null,
            nowMillis = tOvertime,
            targetDurationSeconds = 1500L
        )

        assertEquals("Elapsed seconds must be 1800", 1800L, timing.elapsedSeconds)
        assertEquals("Remaining seconds clamped to 0", 0L, timing.remainingSeconds)
        assertTrue("Session must be marked expired", timing.isExpired)
    }

    // =========================================================================
    // TIER 4: Drift Comparison Against Coroutine Ticks & Rapid-Toggle Stress
    // =========================================================================

    @Test
    fun tier4_testDriftComparisonWallClockVsCoroutineTicks() {
        // Compare drift over a 1-hour session (3,600 seconds = 3,600,000 ms).
        // Under standard Android coroutine delay(1000), typical GC pause & thread scheduling
        // introduces an average jitter of ~25ms per tick (1025ms per real second).
        val targetSeconds = 3600L
        val realElapsedMillis = 3_600_000L

        // Wall-clock calculation:
        val wallClockTiming = FocusTimingOracle.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = null,
            nowMillis = t0 + realElapsedMillis,
            targetDurationSeconds = targetSeconds
        )

        // Simulated coroutine loop: each 1000ms delay took 1025ms in wall-clock time
        // So in 3,600,000 ms, only (3,600,000 / 1025) = ~3512 ticks were executed.
        val coroutineEstimatedTicks = (realElapsedMillis / 1025L)
        val coroutineDriftSeconds = targetSeconds - coroutineEstimatedTicks

        assertEquals("Wall-clock timing engine must have exactly 0 seconds of drift", 3600L, wallClockTiming.elapsedSeconds)
        assertTrue("Coroutine loop suffers significant drift (> 80 seconds)", coroutineDriftSeconds > 80)
        assertTrue("Wall-clock timing outperforms coroutine ticks by eliminating drift completely",
            wallClockTiming.elapsedSeconds > coroutineEstimatedTicks)
    }

    @Test
    fun tier4_testAdversarialRapidPauseResumeStress() {
        // Simulate 50 rapid pause/resume toggles within 5 seconds
        var accumulated = 0L
        var currentT = t0

        for (i in 1..50) {
            currentT += 50L // 50ms active run
            val pauseTime = currentT
            currentT += 50L // 50ms paused
            val (newAcc, _) = FocusTimingOracle.onResume(pauseTime, accumulated, currentT)
            accumulated = newAcc
        }

        // Total time: 50 * 100ms = 5000ms (5 seconds)
        // Total paused: 50 * 50ms = 2500ms
        // Total active: 2500ms (2 seconds elapsed)
        assertEquals(2500L, accumulated)

        val timing = FocusTimingOracle.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = accumulated,
            pausedAtMillis = null,
            nowMillis = currentT,
            targetDurationSeconds = 1500L
        )

        assertEquals("Active elapsed time must be exactly 2 seconds", 2L, timing.elapsedSeconds)
        assertEquals(1500L - 2L, timing.remainingSeconds)
    }
}
