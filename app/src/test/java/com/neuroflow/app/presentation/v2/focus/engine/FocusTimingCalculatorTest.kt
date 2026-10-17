package com.neuroflow.app.presentation.v2.focus.engine

import org.junit.Assert.*
import org.junit.Test

class FocusTimingCalculatorTest {

    private val t0 = 1_700_000_000_000L

    @Test
    fun testStartingStateFreshSession() {
        // Target: 25 minutes = 1500 seconds
        val timing = FocusTimingCalculator.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = null,
            nowMillis = t0,
            targetDurationSeconds = 1500L
        )

        assertEquals(0L, timing.elapsedSeconds)
        assertEquals(1500L, timing.remainingSeconds)
        assertFalse(timing.isExpired)
    }

    @Test
    fun testRunningProgression() {
        // 300 seconds (5 minutes) elapsed
        val now = t0 + 300_000L
        val timing = FocusTimingCalculator.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = null,
            nowMillis = now,
            targetDurationSeconds = 1500L
        )

        assertEquals(300L, timing.elapsedSeconds)
        assertEquals(1200L, timing.remainingSeconds)
        assertFalse(timing.isExpired)
    }

    @Test
    fun testPauseFreeze() {
        // User pauses after 200 seconds
        val pauseAt = t0 + 200_000L
        // Current time is 1000 seconds after start
        val now = t0 + 1_000_000L

        val timing = FocusTimingCalculator.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = pauseAt,
            nowMillis = now,
            targetDurationSeconds = 1500L
        )

        // Elapsed time must be frozen at 200 seconds
        assertEquals(200L, timing.elapsedSeconds)
        assertEquals(1300L, timing.remainingSeconds)
        assertFalse(timing.isExpired)
    }

    @Test
    fun testOnPauseHelper() {
        val now = t0 + 100_000L
        // When not already paused, onPause records nowMillis
        val pausedAt1 = FocusTimingCalculator.onPause(null, now)
        assertEquals(now, pausedAt1)

        // When already paused, onPause keeps existing pausedAt
        val pausedAt2 = FocusTimingCalculator.onPause(pausedAt1, now + 50_000L)
        assertEquals(pausedAt1, pausedAt2)
    }

    @Test
    fun testResumeAccumulation() {
        // Paused at t0 + 100s, resumed at t0 + 400s (300s paused)
        val pauseAt = t0 + 100_000L
        val resumeAt = t0 + 400_000L

        val (newAcc, newPausedAt) = FocusTimingCalculator.onResume(
            pausedAtMillis = pauseAt,
            accumulatedPauseMillis = 0L,
            nowMillis = resumeAt
        )

        assertEquals(300_000L, newAcc)
        assertNull(newPausedAt)

        // Run for 50 more seconds after resume (now = resumeAt + 50s = t0 + 450s)
        val now = resumeAt + 50_000L
        val timing = FocusTimingCalculator.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = newAcc,
            pausedAtMillis = null,
            nowMillis = now,
            targetDurationSeconds = 1500L
        )

        // Active elapsed: 100s + 50s = 150s
        assertEquals(150L, timing.elapsedSeconds)
        assertEquals(1350L, timing.remainingSeconds)
        assertFalse(timing.isExpired)
    }

    @Test
    fun testMultiplePausesAccumulation() {
        var acc = 0L

        // Pause 1: from 100s to 160s (60s)
        val (acc1, _) = FocusTimingCalculator.onResume(t0 + 100_000L, acc, t0 + 160_000L)
        acc = acc1
        assertEquals(60_000L, acc)

        // Pause 2: from 300s to 380s (80s)
        val (acc2, _) = FocusTimingCalculator.onResume(t0 + 300_000L, acc, t0 + 380_000L)
        acc = acc2
        assertEquals(140_000L, acc)

        // At t0 + 500s: total time 500s, paused 140s => active 360s
        val timing = FocusTimingCalculator.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = acc,
            pausedAtMillis = null,
            nowMillis = t0 + 500_000L,
            targetDurationSeconds = 1500L
        )

        assertEquals(360L, timing.elapsedSeconds)
        assertEquals(1140L, timing.remainingSeconds)
        assertFalse(timing.isExpired)
    }

    @Test
    fun testSimulatedDozeTimeJump30Minutes() {
        // Device goes into Doze mode / screen turned off for 30 minutes (1,800,000 ms)
        val dozeDuration = 30 * 60_000L
        val afterDoze = t0 + dozeDuration

        val timing = FocusTimingCalculator.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = null,
            nowMillis = afterDoze,
            targetDurationSeconds = 1500L // 25m Pomodoro
        )

        // 30 minutes elapsed = 1800s, target was 1500s
        assertEquals(1800L, timing.elapsedSeconds)
        assertEquals(0L, timing.remainingSeconds)
        assertTrue("Session must be expired after 30-minute Doze sleep", timing.isExpired)
    }

    @Test
    fun testSimulatedDozeWhilePausedDoesNotAdvanceElapsed() {
        val pausedAt = t0 + 300_000L // paused at 5m
        val afterDoze = t0 + 3600_000L // 1 hour later

        val timing = FocusTimingCalculator.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = pausedAt,
            nowMillis = afterDoze,
            targetDurationSeconds = 1500L
        )

        assertEquals(300L, timing.elapsedSeconds)
        assertEquals(1200L, timing.remainingSeconds)
        assertFalse(timing.isExpired)
    }

    @Test
    fun testCountdownExpirationExactAndBeyond() {
        // Exact expiration
        val timingExact = FocusTimingCalculator.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = null,
            nowMillis = t0 + 1500_000L,
            targetDurationSeconds = 1500L
        )
        assertEquals(1500L, timingExact.elapsedSeconds)
        assertEquals(0L, timingExact.remainingSeconds)
        assertTrue(timingExact.isExpired)

        // 100 seconds past expiration
        val timingOver = FocusTimingCalculator.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = null,
            nowMillis = t0 + 1600_000L,
            targetDurationSeconds = 1500L
        )
        assertEquals(1600L, timingOver.elapsedSeconds)
        assertEquals(0L, timingOver.remainingSeconds)
        assertTrue(timingOver.isExpired)
    }

    @Test
    fun testStopwatchModeNeverExpires() {
        // Target duration <= 0 is stopwatch mode
        val timing = FocusTimingCalculator.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = null,
            nowMillis = t0 + 7200_000L, // 2 hours
            targetDurationSeconds = 0L
        )

        assertEquals(7200L, timing.elapsedSeconds)
        assertEquals(0L, timing.remainingSeconds)
        assertFalse(timing.isExpired)
    }

    @Test
    fun testNegativeClockJumpClamp() {
        // Clock jumped backwards before startedAt
        val timing = FocusTimingCalculator.calculate(
            startedAtMillis = t0,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = null,
            nowMillis = t0 - 60_000L,
            targetDurationSeconds = 1500L
        )

        assertEquals(0L, timing.elapsedSeconds)
        assertEquals(1500L, timing.remainingSeconds)
        assertFalse(timing.isExpired)
    }

    @Test
    fun testUnstartedSessionZeroStartedAt() {
        val timing = FocusTimingCalculator.calculate(
            startedAtMillis = 0L,
            accumulatedPauseMillis = 0L,
            pausedAtMillis = null,
            nowMillis = t0,
            targetDurationSeconds = 1500L
        )

        assertEquals(0L, timing.elapsedSeconds)
        assertEquals(1500L, timing.remainingSeconds)
        assertFalse(timing.isExpired)
    }
}
