package com.neuroflow.app.domain.service

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class CircadianSimulationTest {

    @Test
    fun testSimulatePowerNapDischargesSleepPressureAndBoostsAfternoonAlertness() {
        val service: CircadianEnergyService = DefaultCircadianEnergyService()
        val today = LocalDate.of(2026, 10, 18)
        val zoneId = ZoneId.of("UTC")
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val baseTimeline = service.projectDayTimeline(
            dayStartMillis = dayStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            stepMinutes = 15,
            zoneId = zoneId
        )

        val napStart = today.atTime(LocalTime.of(14, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val simulatedNap = DaytimeNapBout(napStart, napStart + 20 * 60_000L)

        val outcome = service.simulateDayInterventions(
            baseTimeline = baseTimeline,
            dayStartMillis = dayStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            stepMinutes = 15,
            zoneId = zoneId,
            simulatedNaps = listOf(simulatedNap)
        )

        assertTrue("Net energy delta should be positive", outcome.netEnergyDeltaPercent > 0)
        assertTrue("Sleep pressure delta should reflect discharge", outcome.sleepPressureDeltaPercent > 0)
        assertTrue("Insight should mention sleep pressure", outcome.summaryInsight.contains("sleep pressure", ignoreCase = true))
    }
}
