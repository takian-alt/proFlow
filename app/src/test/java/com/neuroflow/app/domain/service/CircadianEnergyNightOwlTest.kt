package com.neuroflow.app.domain.service

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CircadianEnergyNightOwlTest {

    private val service = DefaultCircadianEnergyService()
    private val zoneId = ZoneId.of("UTC")

    @Test
    fun `computeDailySummary preserves continuous homeostatic buildup for night-owl post-midnight session`() {
        val yesterday = LocalDate.of(2026, 4, 14)
        val today = LocalDate.of(2026, 4, 15)

        // User woke up yesterday at 16:00 UTC and is still awake at 02:30 UTC today (10.5 hours awake)
        val wakeYesterday1600 = yesterday.atTime(16, 0).atZone(zoneId).toInstant().toEpochMilli()
        val now0230 = today.atTime(2, 30).atZone(zoneId).toInstant().toEpochMilli()

        val summary = service.computeDailySummary(
            timestampMillis = now0230,
            wakeTimeMillis = wakeYesterday1600,
            lastSleepDurationMinutes = 480,
            continuousActiveFocusMinutes = 25,
            zoneId = zoneId
        )

        // Compare against a fresh 30m-awake baseline: 10.5h awake must carry substantially higher homeostatic pressure
        val freshSnapshot = service.evaluateSnapshot(
            timestampMillis = wakeYesterday1600 + 30 * 60_000L,
            wakeTimeMillis = wakeYesterday1600,
            lastSleepDurationMinutes = 480,
            continuousActiveFocusMinutes = 0,
            zoneId = zoneId
        )
        assertTrue(
            "Night-owl homeostatic pressure (${summary.currentSnapshot.homeostaticPressureS}) should exceed fresh wake (${freshSnapshot.homeostaticPressureS}) and remain < 0.85",
            summary.currentSnapshot.homeostaticPressureS > freshSnapshot.homeostaticPressureS + 0.15 &&
                summary.currentSnapshot.homeostaticPressureS < 0.85
        )
        assertTrue(
            "Night-owl usableEnergy should be bounded and non-zero, got ${summary.currentSnapshot.usableEnergy}",
            summary.currentSnapshot.usableEnergy in 1..100
        )

        // Project timeline should preserve post-midnight pressure buildup instead of resetting before 16:00 today
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val timeline = service.projectDayTimeline(
            dayStartMillis = dayStart,
            wakeTimeMillis = wakeYesterday1600,
            lastSleepDurationMinutes = 480,
            stepMinutes = 30,
            zoneId = zoneId
        )
        val twoAmSnapshot = timeline.first {
            Instant.ofEpochMilli(it.timestampMillis).atZone(zoneId).hour == 2
        }
        assertTrue(
            "02:00 UTC after waking yesterday at 16:00 must retain accumulated pressure, got ${twoAmSnapshot.homeostaticPressureS}",
            twoAmSnapshot.homeostaticPressureS > freshSnapshot.homeostaticPressureS + 0.15
        )
        assertTrue(summary.peakEnergyScore in 1..100)
        assertTrue(summary.recommendedDeepWorkMinutes > 0)
    }
}
