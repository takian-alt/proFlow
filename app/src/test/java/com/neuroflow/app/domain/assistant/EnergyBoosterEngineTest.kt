package com.neuroflow.app.domain.assistant

import com.neuroflow.app.data.local.PhysiologicalEventType
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.CognitiveBlockCategory
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import com.neuroflow.app.domain.service.DaytimeNapBout
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.EnergyPhase
import com.neuroflow.app.domain.service.PhysiologicalImpulseCalculator
import com.neuroflow.app.domain.service.StimulantDoseBout
import com.neuroflow.app.domain.service.StimulantType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class EnergyBoosterEngineTest {

    private val zoneId: ZoneId = ZoneId.of("UTC")
    private val service = DefaultCircadianEnergyService()

    private fun epochMillis(hour: Int, minute: Int = 0): Long {
        return LocalDate.of(2026, 6, 15)
            .atTime(hour, minute)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun boosterImpulses_peakAtCompletionAndStayBoundedWhenStacked() {
        val now = epochMillis(14, 30)
        val minOfDay = 14 * 60 + 30
        val hydration = CognitiveBlockWindow(minOfDay, minOfDay + 5, CognitiveBlockCategory.HYDRATION, 1.0, "Hydration", now, now + 5 * 60_000L)
        val snack = CognitiveBlockWindow(minOfDay, minOfDay + 10, CognitiveBlockCategory.SNACK, 1.0, "Snack", now, now + 10 * 60_000L)
        val walk = CognitiveBlockWindow(minOfDay, minOfDay + 12, CognitiveBlockCategory.SUNLIGHT_WALK, 1.0, "Walk", now, now + 12 * 60_000L)
        val rest = CognitiveBlockWindow(minOfDay, minOfDay + 15, CognitiveBlockCategory.REST_BREAK, 1.0, "Rest", now, now + 15 * 60_000L)

        // Before ramp window: 0
        val before = epochMillis(14, 0)
        assertEquals(0.0, PhysiologicalImpulseCalculator.computeCombinedImpulse(14 * 60, listOf(hydration, snack, walk, rest), before), 1e-6)

        // Individual impulses at completion timestamp (dt == 0) are positive and realistic
        val hVal = PhysiologicalImpulseCalculator.computeHydrationImpulse(minOfDay, listOf(hydration), now)
        val sVal = PhysiologicalImpulseCalculator.computeSnackImpulse(minOfDay, listOf(snack), now)
        val wVal = PhysiologicalImpulseCalculator.computeSunlightWalkImpulse(minOfDay, listOf(walk), now)
        val rVal = PhysiologicalImpulseCalculator.computeRestBreakImpulse(minOfDay, listOf(rest), now)
        assertTrue("Hydration boost should be ~6 pts, got $hVal", hVal in 5.5..6.5)
        assertTrue("Snack boost should be ~8 pts, got $sVal", sVal in 7.5..8.5)
        assertTrue("Walk boost should be ~9.5 pts, got $wVal", wVal in 9.0..10.0)
        assertTrue("15m Rest boost should be ~11 pts, got $rVal", rVal in 10.5..11.5)

        // Stacked boost is soft-capped at +22.0 pts
        val stacked = PhysiologicalImpulseCalculator.computeCombinedImpulse(minOfDay, listOf(hydration, snack, walk, rest), now)
        assertTrue("Stacked positive booster impulse must be capped at 22.0, got $stacked", stacked <= 22.0001 && stacked >= 21.5)
    }

    @Test
    fun completingBoosters_increasesLiveCircadianEnergyAboveUnboostedBaseline() {
        val wake = epochMillis(7, 0)
        val now = epochMillis(14, 30)
        val minOfDay = 14 * 60 + 30

        val unboosted = service.evaluateSnapshot(
            timestampMillis = now,
            wakeTimeMillis = wake,
            lastSleepDurationMinutes = 420,
            zoneId = zoneId
        )

        val walkBlock = CognitiveBlockWindow(
            startMinuteOfDay = minOfDay,
            endMinuteOfDay = minOfDay + 12,
            category = CognitiveBlockCategory.SUNLIGHT_WALK,
            intensity = 1.0,
            label = "Sunlight Walk",
            explicitStartMillis = now,
            explicitEndMillis = now + 12 * 60_000L
        )
        val boostedByWalk = service.evaluateSnapshot(
            timestampMillis = now,
            wakeTimeMillis = wake,
            lastSleepDurationMinutes = 420,
            zoneId = zoneId,
            cognitiveBlocks = listOf(walkBlock)
        )

        val nap = DaytimeNapBout(startMillis = now - 20 * 60_000L, endMillis = now)
        val boostedByNap = service.evaluateSnapshot(
            timestampMillis = now,
            wakeTimeMillis = wake,
            lastSleepDurationMinutes = 420,
            zoneId = zoneId,
            naps = listOf(nap)
        )

        assertTrue(
            "Sunlight walk should increase live usableEnergy (${boostedByWalk.usableEnergy} vs ${unboosted.usableEnergy})",
            boostedByWalk.usableEnergy >= unboosted.usableEnergy + 6
        )
        assertTrue(
            "20m power nap should increase live usableEnergy (${boostedByNap.usableEnergy} vs ${unboosted.usableEnergy})",
            boostedByNap.usableEnergy > unboosted.usableEnergy
        )
    }

    @Test
    fun recommendBoosters_neverSuggestsCaffeineAfterSafeCutoffAndPrioritizesWindDownAtNight() {
        val prefs = UserPreferences(wakeUpHour = 7, sleepHour = 23)
        val eveningNow = epochMillis(18, 30)
        val eveningSnap = CircadianSnapshot(
            timestampMillis = eveningNow,
            usableEnergy = 46,
            homeostaticPressureS = 0.62,
            circadianAlertnessC = 0.45,
            focusDepletionPenalty = 0.05,
            phase = EnergyPhase.DIP
        )

        val eveningRecs = EnergyBoosterEngine.recommendBoosters(
            prefs = prefs,
            snapshot = eveningSnap,
            burnoutRiskState = BurnoutRiskState(),
            stimulants = emptyList(),
            naps = emptyList(),
            nowMillis = eveningNow,
            zoneId = zoneId,
            maxResults = 6
        )
        assertTrue(
            "Smart caffeine must NEVER be recommended after safe cutoff",
            eveningRecs.none { it.type == EnergyBoosterType.SMART_CAFFEINE }
        )

        val lateNightNow = epochMillis(22, 10)
        val lateNightSnap = eveningSnap.copy(
            timestampMillis = lateNightNow,
            usableEnergy = 32,
            phase = EnergyPhase.WIND_DOWN
        )
        val nightRecs = EnergyBoosterEngine.recommendBoosters(
            prefs = prefs,
            snapshot = lateNightSnap,
            burnoutRiskState = BurnoutRiskState(),
            stimulants = emptyList(),
            naps = emptyList(),
            nowMillis = lateNightNow,
            zoneId = zoneId,
            maxResults = 3
        )
        assertEquals(
            "Sleep Wind-Down should be top recommendation near bedtime",
            EnergyBoosterType.SLEEP_WIND_DOWN,
            nightRecs.first().type
        )
        assertTrue(nightRecs.none { it.type == EnergyBoosterType.POWER_NAP })
        assertTrue(nightRecs.none { it.type == EnergyBoosterType.SHORT_WORKOUT })
    }

    @Test
    fun recommendBoosters_afternoonDipSuggestsPowerNapAndRestWhenBurnoutElevated() {
        val prefs = UserPreferences(wakeUpHour = 7, sleepHour = 23)
        val afternoonNow = epochMillis(14, 15)
        val dipSnap = CircadianSnapshot(
            timestampMillis = afternoonNow,
            usableEnergy = 44,
            homeostaticPressureS = 0.58,
            circadianAlertnessC = 0.42,
            focusDepletionPenalty = 0.12,
            phase = EnergyPhase.DIP
        )
        val burnout = BurnoutRiskState(
            riskLevel = BurnoutRiskLevel.HIGH,
            riskScore = 82f,
            unbrokenLoadMinutes = 110,
            minutesSinceLastRest = 120
        )

        val recs = EnergyBoosterEngine.recommendBoosters(
            prefs = prefs,
            snapshot = dipSnap,
            burnoutRiskState = burnout,
            stimulants = emptyList(),
            naps = emptyList(),
            nowMillis = afternoonNow,
            zoneId = zoneId,
            maxResults = 3
        )

        assertTrue("Top recommendations should include Rest Break during high burnout", recs.any { it.type == EnergyBoosterType.REST_BREAK })
        assertTrue("Top recommendations should include Power Nap during afternoon dip", recs.any { it.type == EnergyBoosterType.POWER_NAP })
        assertTrue("Rest Break should support break timer", recs.first { it.type == EnergyBoosterType.REST_BREAK }.supportsTimer)
    }

    @Test
    fun buildBoosterCognitiveBlocksAndMarkers_unifiesPhysLogsAndRestBreaksWithoutDuplicates() {
        val dayStart = epochMillis(0, 0)
        val hydrationTs = epochMillis(10, 0)
        val restTs = epochMillis(13, 30)

        val physJson = """
            [
              {"type":"${PhysiologicalEventType.HYDRATION.name}","timestampMillis":$hydrationTs,"durationMinutes":5,"intensity":1.0},
              {"type":"${PhysiologicalEventType.REST_BREAK.name}","timestampMillis":$restTs,"durationMinutes":10,"intensity":1.0}
            ]
        """.trimIndent()
        val restJson = SmartAssistantCodecs.encodeRestBreakLogs(
            listOf(
                RestBreakLog("r1", restTs + 30_000L, 10, RestBreakSource.MANUAL_CONFIRM), // duplicate within 2m
                RestBreakLog("r2", epochMillis(16, 0), 15, RestBreakSource.BREAK_TIMER) // distinct rest
            )
        )
        val prefs = UserPreferences(
            quickPhysiologicalLogsJson = physJson,
            restBreakLogsJson = restJson
        )

        val blocks = EnergyBoosterEngine.buildBoosterCognitiveBlocks(prefs, dayStart, zoneId)
        assertEquals(3, blocks.size) // 1 hydration + 1 phys rest + 1 distinct break-timer rest
        assertEquals(1, blocks.count { it.category == CognitiveBlockCategory.HYDRATION })
        assertEquals(2, blocks.count { it.category == CognitiveBlockCategory.REST_BREAK })

        val markers = EnergyBoosterEngine.buildTodayGraphMarkers(
            prefs = prefs,
            naps = listOf(DaytimeNapBout(epochMillis(14, 0), epochMillis(14, 20))),
            stimulants = listOf(StimulantDoseBout(timestampMillis = epochMillis(9, 0), doseMg = 95.0, type = StimulantType.CAFFEINE)),
            dayStartMillis = dayStart,
            zoneId = zoneId
        )
        assertTrue("Should include coffee, hydration, rest, and nap markers", markers.size >= 4)
        assertEquals("☕", markers.first().emoji)
    }

    @Test
    fun evaluateAssistantNudge_respectsFocusTimerBreakTimerAndCooldown() {
        val now = epochMillis(14, 30)
        val dipSnap = CircadianSnapshot(
            timestampMillis = now,
            usableEnergy = 48,
            homeostaticPressureS = 0.56,
            circadianAlertnessC = 0.44,
            focusDepletionPenalty = 0.08,
            phase = EnergyPhase.DIP
        )
        val prefs = UserPreferences(wakeUpHour = 7, sleepHour = 23, wellnessNudgesEnabled = true)
        val recs = EnergyBoosterEngine.recommendBoosters(prefs, dipSnap, BurnoutRiskState(), emptyList(), emptyList(), now, zoneId)

        // Fires when energy dips and user is idle
        val activeNudge = EnergyBoosterEngine.evaluateAssistantNudge(
            prefs = prefs,
            snapshot = dipSnap,
            recommendations = recs,
            nowMillis = now,
            isFocusSessionActive = false,
            isBreakTimerActive = false
        )
        assertNotNull("Assistant nudge should fire when energy dips below 62%", activeNudge)

        // Silenced during active Focus session
        assertNull(
            EnergyBoosterEngine.evaluateAssistantNudge(
                prefs = prefs,
                snapshot = dipSnap,
                recommendations = recs,
                nowMillis = now,
                isFocusSessionActive = true,
                isBreakTimerActive = false
            )
        )

        // Silenced during active Break timer
        assertNull(
            EnergyBoosterEngine.evaluateAssistantNudge(
                prefs = prefs,
                snapshot = dipSnap,
                recommendations = recs,
                nowMillis = now,
                isFocusSessionActive = false,
                isBreakTimerActive = true
            )
        )

        // Silenced within 45m cooldown
        val recentCooldownPrefs = prefs.copy(lastEnergyBoosterNudgeAtMillis = now - 20 * 60_000L)
        assertNull(
            EnergyBoosterEngine.evaluateAssistantNudge(
                prefs = recentCooldownPrefs,
                snapshot = dipSnap,
                recommendations = recs,
                nowMillis = now,
                isFocusSessionActive = false,
                isBreakTimerActive = false
            )
        )
    }

    @Test
    fun loggingBalancedSnack_coversSnacksCheckInAndAppearsInVaultLogbook() {
        val snackTs = epochMillis(16, 10)
        val now = epochMillis(16, 30)
        val snacksOnlyConfig = SmartAssistantCodecs.encodeCheckInConfigs(
            CheckInLogType.entries.map { type ->
                CheckInConfigEntry(
                    type = type,
                    enabled = type == CheckInLogType.SNACKS,
                    defaultMinuteOfDay = if (type == CheckInLogType.SNACKS) 16 * 60 else type.defaultMinuteOfDay
                )
            }
        )
        val snackPhysJson = """
            [{"type":"${PhysiologicalEventType.SNACK.name}","timestampMillis":$snackTs,"durationMinutes":10,"intensity":1.0}]
        """.trimIndent()

        // Without snack log, SNACKS check-in is due
        val pendingPrefs = UserPreferences(
            checkInsMasterEnabled = true,
            checkInConfigJson = snacksOnlyConfig
        )
        val dueBefore = ProactiveCheckInCoordinator.evaluateNextDueCheckIn(
            prefs = pendingPrefs,
            nowMillis = now,
            isFocusSessionActive = false,
            zoneId = zoneId
        )
        assertEquals(CheckInLogType.SNACKS, dueBefore?.type)

        // After logging Balanced Snack booster (PhysiologicalEventType.SNACK), SNACKS check-in is covered & LOGGED
        val loggedPrefs = pendingPrefs.copy(quickPhysiologicalLogsJson = snackPhysJson)
        val dueAfter = ProactiveCheckInCoordinator.evaluateNextDueCheckIn(
            prefs = loggedPrefs,
            nowMillis = now,
            isFocusSessionActive = false,
            zoneId = zoneId
        )
        assertNull("Snacks check-in should be suppressed once a SNACK event is logged", dueAfter)

        val logbook = ProactiveCheckInCoordinator.buildTodayLogbookEntries(
            prefs = loggedPrefs,
            nowMillis = now,
            zoneId = zoneId
        )
        val snackEntry = logbook.first { it.type == CheckInLogType.SNACKS }
        assertEquals(CheckInDailyStatus.LOGGED, snackEntry.status)
        assertEquals(snackTs, snackEntry.loggedAtMillis)
    }

    @Test
    fun energyCoachAndWellnessNudge_shareUnifiedDismissalAndSnoozeCooldown() {
        val now = epochMillis(14, 30)
        val dipSnap = CircadianSnapshot(
            timestampMillis = now,
            usableEnergy = 30,
            homeostaticPressureS = 0.65,
            circadianAlertnessC = 0.38,
            focusDepletionPenalty = 0.10,
            phase = EnergyPhase.CRASH
        )
        val basePrefs = UserPreferences(wakeUpHour = 7, sleepHour = 23, wellnessNudgesEnabled = true)
        val recs = EnergyBoosterEngine.recommendBoosters(
            prefs = basePrefs,
            snapshot = dipSnap,
            burnoutRiskState = BurnoutRiskState(),
            stimulants = emptyList(),
            naps = emptyList(),
            nowMillis = now,
            zoneId = zoneId
        )

        // 1. Snoozing Energy Coach also suppresses Wellness Nudge immediately
        val snoozedBoosterPrefs = basePrefs.copy(
            snoozedEnergyBoosterUntilMillis = now + 45 * 60_000L,
            lastEnergyBoosterNudgeAtMillis = now - 5 * 60_000L
        )
        assertNull(
            "Wellness Nudge must stay suppressed while Energy Coach is snoozed",
            WellnessNudgeEvaluator.evaluateWellnessNudge(
                prefs = snoozedBoosterPrefs,
                usableEnergy = 30,
                phase = EnergyPhase.CRASH,
                isCrashWindow = true,
                continuousFocusMinutes = 60,
                nowMillis = now,
                isFocusSessionActive = false
            )
        )

        // 2. Dismissing Wellness Nudge also suppresses Energy Coach within the shared cooldown
        val dismissedWellnessPrefs = basePrefs.copy(
            lastWellnessNudgeDismissedAtMillis = now - 10 * 60_000L
        )
        assertNull(
            "Energy Coach must stay suppressed after dismissing Wellness Nudge",
            EnergyBoosterEngine.evaluateAssistantNudge(
                prefs = dismissedWellnessPrefs,
                snapshot = dipSnap,
                recommendations = recs,
                nowMillis = now,
                isFocusSessionActive = false,
                isBreakTimerActive = false
            )
        )
    }
}
