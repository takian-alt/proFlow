package com.neuroflow.app.domain.assistant

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.buildCircadianEnergySummary
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.EnergyPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SmartAssistantEnginesTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val testDate: LocalDate = LocalDate.of(2026, 11, 21)

    private fun epochAt(hour: Int, minute: Int = 0, date: LocalDate = testDate): Long =
        date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()

    private fun sampleSnapshot(
        usableEnergy: Int = 65,
        phase: EnergyPhase = EnergyPhase.PEAK,
        timestampMillis: Long = epochAt(10, 0)
    ): CircadianSnapshot = CircadianSnapshot(
        timestampMillis = timestampMillis,
        usableEnergy = usableEnergy,
        homeostaticPressureS = 0.35,
        circadianAlertnessC = 0.7,
        focusDepletionPenalty = 0.0,
        phase = phase,
        predictionConfidence = 0.9
    )

    // --- 1. Non-Zero Budget & Overtime Tests ---

    @Test
    fun `budget is never zero during evening wind-down and flags stretch mode`() {
        val summary = buildCircadianEnergySummary(
            dailySummary = null,
            snapshot = sampleSnapshot(usableEnergy = 32, phase = EnergyPhase.WIND_DOWN),
            sleepHour = 23,
            completedDeepWorkMinutes = 180,
            currentTime = LocalTime.of(21, 45),
            isOvertimeActive = false
        )
        assertTrue("Evening budget must remain non-zero", summary.cognitiveBudgetMinutesRemaining >= 25)
        assertTrue("Evening should activate stretch mode", summary.isStretchMode)
    }

    @Test
    fun `budget remains positive after 240m daily target is reached`() {
        val summary = buildCircadianEnergySummary(
            dailySummary = null,
            snapshot = sampleSnapshot(usableEnergy = 72, phase = EnergyPhase.PEAK),
            dailyDeepWorkTargetMinutes = 240,
            sleepHour = 23,
            completedDeepWorkMinutes = 260,
            currentTime = LocalTime.of(15, 0),
            isOvertimeActive = false
        )
        assertTrue("Post-target budget must stay positive", summary.cognitiveBudgetMinutesRemaining >= 25)
        assertTrue(summary.isStretchMode)
        assertEquals(20, summary.overtimeMinutesWorked)
    }

    @Test
    fun `overtime mode unlocks expanded evening capacity`() {
        val summary = buildCircadianEnergySummary(
            dailySummary = null,
            snapshot = sampleSnapshot(usableEnergy = 45, phase = EnergyPhase.WIND_DOWN),
            dailyDeepWorkTargetMinutes = 240,
            sleepHour = 23,
            completedDeepWorkMinutes = 240,
            currentTime = LocalTime.of(21, 15),
            isOvertimeActive = true
        )
        assertTrue(summary.isOvertimeActive)
        assertTrue("Overtime should grant expanded reserve", summary.cognitiveBudgetMinutesRemaining >= 90)
    }

    // --- 2. Proactive Check-In Coordinator Tests ---

    @Test
    fun `check-in triggers after default lunch time and respects focus suppression`() {
        val nowLunch = epochAt(13, 40) // Lunch default is 13:30 (810m)
        val prefs = UserPreferences(checkInsMasterEnabled = true)

        val suppressedDuringFocus = ProactiveCheckInCoordinator.evaluateNextDueCheckIn(
            prefs = prefs,
            nowMillis = nowLunch,
            isFocusSessionActive = true,
            zoneId = zone
        )
        assertNull("Check-ins must not interrupt active focus sessions", suppressedDuringFocus)

        val due = ProactiveCheckInCoordinator.evaluateNextDueCheckIn(
            prefs = prefs,
            nowMillis = nowLunch,
            isFocusSessionActive = false,
            zoneId = zone
        )
        assertNotNull(due)
        assertEquals(CheckInLogType.LUNCH, due?.type)
    }

    @Test
    fun `snoozed check-in stays quiet before target time and re-prompts immediately after`() {
        val epochDay = testDate.toEpochDay()
        val snoozeTarget = epochAt(14, 30)
        val stateJson = SmartAssistantCodecs.encodeDailyCheckInState(
            epochDay = epochDay,
            records = mapOf(
                CheckInLogType.LUNCH to CheckInDailyRecord(
                    type = CheckInLogType.LUNCH,
                    status = CheckInDailyStatus.SNOOZED,
                    snoozedUntilMillis = snoozeTarget
                )
            )
        )
        val prefs = UserPreferences(
            checkInsMasterEnabled = true,
            dailyCheckInStateJson = stateJson,
            lastCheckInPromptAtMillis = epochAt(14, 25) // inside 20m cooldown, but matured snooze overrides
        )

        val beforeSnooze = ProactiveCheckInCoordinator.evaluateNextDueCheckIn(
            prefs = prefs,
            nowMillis = epochAt(14, 28),
            isFocusSessionActive = false,
            zoneId = zone
        )
        assertNull("Should respect cooldown & snooze before target time", beforeSnooze)

        val afterSnooze = ProactiveCheckInCoordinator.evaluateNextDueCheckIn(
            prefs = prefs,
            nowMillis = epochAt(14, 31),
            isFocusSessionActive = false,
            zoneId = zone
        )
        assertNotNull(afterSnooze)
        assertEquals(CheckInLogType.LUNCH, afterSnooze?.type)
    }

    @Test
    fun `skipped check-in does not re-prompt and reflects in today logbook`() {
        val epochDay = testDate.toEpochDay()
        val configsJson = SmartAssistantCodecs.encodeCheckInConfigs(
            CheckInLogType.entries.map {
                CheckInConfigEntry(type = it, enabled = it == CheckInLogType.DINNER)
            }
        )
        val stateJson = SmartAssistantCodecs.encodeDailyCheckInState(
            epochDay = epochDay,
            records = mapOf(
                CheckInLogType.DINNER to CheckInDailyRecord(
                    type = CheckInLogType.DINNER,
                    status = CheckInDailyStatus.SKIPPED
                )
            )
        )
        val prefs = UserPreferences(
            checkInsMasterEnabled = true,
            checkInConfigJson = configsJson,
            dailyCheckInStateJson = stateJson
        )

        val due = ProactiveCheckInCoordinator.evaluateNextDueCheckIn(
            prefs = prefs,
            nowMillis = epochAt(20, 30),
            isFocusSessionActive = false,
            zoneId = zone
        )
        assertNull(due)

        val logbook = ProactiveCheckInCoordinator.buildTodayLogbookEntries(
            prefs = prefs,
            nowMillis = epochAt(20, 30),
            zoneId = zone
        )
        val dinnerEntry = logbook.first { it.type == CheckInLogType.DINNER }
        assertEquals(CheckInDailyStatus.SKIPPED, dinnerEntry.status)
    }

    // --- 3. Wellness Nudge Evaluator Tests ---

    @Test
    fun `wellness evaluator triggers long focus break and crash easy win nudges`() {
        val prefs = UserPreferences(wellnessNudgesEnabled = true, focusBreakThresholdMinutes = 50)
        val now = epochAt(11, 0)

        val longFocus = WellnessNudgeEvaluator.evaluateWellnessNudge(
            prefs = prefs,
            usableEnergy = 70,
            phase = EnergyPhase.PEAK,
            isCrashWindow = false,
            continuousFocusMinutes = 55,
            nowMillis = now,
            isFocusSessionActive = false
        )
        assertEquals(WellnessNudgeType.LONG_FOCUS_BREAK, longFocus?.type)

        val crash = WellnessNudgeEvaluator.evaluateWellnessNudge(
            prefs = prefs,
            usableEnergy = 28,
            phase = EnergyPhase.CRASH,
            isCrashWindow = true,
            continuousFocusMinutes = 10,
            nowMillis = now,
            isFocusSessionActive = false
        )
        assertEquals(WellnessNudgeType.CRASH_EASY_WIN, crash?.type)

        val suppressedInFocus = WellnessNudgeEvaluator.evaluateWellnessNudge(
            prefs = prefs,
            usableEnergy = 28,
            phase = EnergyPhase.CRASH,
            isCrashWindow = true,
            continuousFocusMinutes = 60,
            nowMillis = now,
            isFocusSessionActive = true
        )
        assertNull(suppressedInFocus)
    }

    // --- 4. Live Task Slot Recommender Tests ---

    @Test
    fun `slot recommender avoids busy tasks and unlocks tonight for urgent tomorrow-AM deadline`() {
        val nowEvening = epochAt(20, 30)
        val tomorrowMorningDeadline = epochAt(8, 30, testDate.plusDays(1))
        val todayMidnight = epochAt(0, 0)

        val busyTask = TaskEntity(
            id = "busy-1",
            title = "Existing Call",
            estimatedDurationMinutes = 30,
            energyLevel = EnergyLevel.MEDIUM,
            status = TaskStatus.ACTIVE,
            scheduledDate = todayMidnight,
            scheduledTime = 20 * 3600_000L + 30 * 60_000L
        )
        val prefs = UserPreferences(workDayStart = 9, workDayEnd = 18, wakeUpHour = 7)

        val proposals = TaskSlotRecommender.recommendSlots(
            durationMinutes = 30,
            energyLevel = EnergyLevel.MEDIUM,
            targetDateMillis = todayMidnight,
            deadlineMillis = tomorrowMorningDeadline,
            existingTasks = listOf(busyTask),
            energyTimeline = listOf(
                sampleSnapshot(usableEnergy = 55, phase = EnergyPhase.WIND_DOWN, timestampMillis = epochAt(21, 0))
            ),
            prefs = prefs,
            nowMillis = nowEvening,
            zoneId = zone
        )

        assertTrue("Should propose at least one slot", proposals.isNotEmpty())
        val top = proposals.first()
        assertEquals("Urgent tomorrow-AM task must schedule tonight", todayMidnight, top.dateMillis)
        assertTrue("Must not overlap busy task ending at 21:00", top.startMillis >= epochAt(21, 0))
        assertTrue("Tonight slot after workDayEnd should be marked overtime", top.isOvertimeSlot)
    }

    @Test
    fun `slot recommender reports gap context between surrounding scheduled tasks`() {
        val nowMorning = epochAt(8, 30)
        val todayMidnight = epochAt(0, 0)
        val standup = TaskEntity(
            id = "t-standup",
            title = "Standup",
            estimatedDurationMinutes = 30,
            energyLevel = EnergyLevel.LOW,
            status = TaskStatus.ACTIVE,
            scheduledDate = todayMidnight,
            scheduledTime = 9 * 3600_000L
        )
        val designReview = TaskEntity(
            id = "t-review",
            title = "Design Review",
            estimatedDurationMinutes = 60,
            energyLevel = EnergyLevel.HIGH,
            status = TaskStatus.ACTIVE,
            scheduledDate = todayMidnight,
            scheduledTime = 11 * 3600_000L
        )
        val prefs = UserPreferences(workDayStart = 9, workDayEnd = 18, wakeUpHour = 7, peakEnergyStart = 9, peakEnergyEnd = 12)

        val proposals = TaskSlotRecommender.recommendSlots(
            durationMinutes = 45,
            energyLevel = EnergyLevel.HIGH,
            targetDateMillis = todayMidnight,
            deadlineMillis = null,
            existingTasks = listOf(standup, designReview),
            energyTimeline = emptyList(),
            prefs = prefs,
            nowMillis = nowMorning,
            zoneId = zone
        )

        assertTrue("Expected top match plus alternates", proposals.size == 3)
        val top = proposals.first()
        assertTrue("Top high-energy slot should fit between Standup and Design Review", top.startMillis in epochAt(9, 30)..epochAt(10, 15))
        assertTrue("Gap context should mention Standup", top.gapContextLabel.contains("Standup"))
        assertTrue("Gap context should mention Design Review", top.gapContextLabel.contains("Design Review"))
        assertTrue("Energy match badge should highlight peak energy", top.energyMatchLabel.contains("Peak") || top.energyMatchLabel.contains("Great"))
    }

    @Test
    fun `slot recommender excludes edited task from self-collision`() {
        val nowMorning = epochAt(8, 30)
        val todayMidnight = epochAt(0, 0)
        val editingTask = TaskEntity(
            id = "task-self",
            title = "Write Architecture Spec",
            estimatedDurationMinutes = 60,
            energyLevel = EnergyLevel.HIGH,
            status = TaskStatus.ACTIVE,
            scheduledDate = todayMidnight,
            scheduledTime = 10 * 3600_000L
        )
        val blockerBefore = TaskEntity(
            id = "task-before",
            title = "Morning Sync",
            estimatedDurationMinutes = 55,
            energyLevel = EnergyLevel.LOW,
            status = TaskStatus.ACTIVE,
            scheduledDate = todayMidnight,
            scheduledTime = 9 * 3600_000L
        )
        val blockerAfter = TaskEntity(
            id = "task-after",
            title = "Client Demo",
            estimatedDurationMinutes = 120,
            energyLevel = EnergyLevel.MEDIUM,
            status = TaskStatus.ACTIVE,
            scheduledDate = todayMidnight,
            scheduledTime = 11 * 3600_000L
        )
        val prefs = UserPreferences(workDayStart = 9, workDayEnd = 13, wakeUpHour = 7, peakEnergyStart = 9, peakEnergyEnd = 12)

        val proposals = TaskSlotRecommender.recommendSlots(
            durationMinutes = 60,
            energyLevel = EnergyLevel.HIGH,
            targetDateMillis = todayMidnight,
            deadlineMillis = null,
            existingTasks = listOf(blockerBefore, editingTask, blockerAfter),
            energyTimeline = emptyList(),
            prefs = prefs,
            nowMillis = nowMorning,
            zoneId = zone,
            excludeTaskId = "task-self"
        )

        assertTrue(proposals.isNotEmpty())
        assertEquals("Self-excluded editing task can keep its 10:00 peak slot", epochAt(10, 0), proposals.first().startMillis)
    }

    @Test
    fun `slot recommender falls back to tomorrow with flag when today is fully booked`() {
        val nowLate = epochAt(17, 30)
        val todayMidnight = epochAt(0, 0)
        val tomorrowMidnight = epochAt(0, 0, testDate.plusDays(1))
        val prefs = UserPreferences(workDayStart = 9, workDayEnd = 18, wakeUpHour = 7, peakEnergyStart = 9, peakEnergyEnd = 12)

        val proposals = TaskSlotRecommender.recommendSlots(
            durationMinutes = 60,
            energyLevel = EnergyLevel.HIGH,
            targetDateMillis = todayMidnight,
            deadlineMillis = null,
            existingTasks = emptyList(),
            energyTimeline = emptyList(),
            prefs = prefs,
            nowMillis = nowLate,
            zoneId = zone
        )

        assertTrue("Should propose tomorrow slots when today has no room", proposals.isNotEmpty())
        val top = proposals.first()
        assertEquals(tomorrowMidnight, top.dateMillis)
        assertTrue("Fallback flag should be set when today is full", top.isTodayFullFallback)
        assertTrue("Reason should explain today is full", top.reasonLabel.contains("Today is full"))
    }

    // --- 5. Goal Breakdown Engine Tests ---

    @Test
    fun `goal breakdown generates structured milestones and subtasks across domains`() {
        val studyPlan = GoalBreakdownEngine.generateLocalBreakdown("Pass Biology Final Exam")
        assertTrue(studyPlan.size >= 3)
        assertTrue(studyPlan.all { it.subtasks.isNotEmpty() })

        val buildPlan = GoalBreakdownEngine.generateLocalBreakdown("Launch Indie Android App")
        assertTrue(buildPlan.size >= 3)
        assertTrue(buildPlan.first().title.contains("Scope", ignoreCase = true))
    }
}
