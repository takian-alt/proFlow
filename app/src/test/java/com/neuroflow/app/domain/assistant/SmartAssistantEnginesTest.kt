package com.neuroflow.app.domain.assistant

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
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

    // --- 6. Sleep Interval Resolution & AUTO_DEFAULT Filtering Tests ---

    @Test
    fun `resolveLastNightSleepInterval handles cross-midnight, post-midnight, and future wake edge cases`() {
        val nowMorning = epochAt(9, 0)

        // Cross-midnight: 11:00 PM -> 7:00 AM = yesterday 23:00 to today 07:00 (480m)
        val (crossBed, crossWake) = SmartAssistantCodecs.resolveLastNightSleepInterval(
            bedHour = 23,
            bedMinute = 0,
            wakeHour = 7,
            wakeMinute = 0,
            nowMillis = nowMorning,
            zoneId = zone
        )
        assertEquals(epochAt(23, 0, testDate.minusDays(1)), crossBed)
        assertEquals(epochAt(7, 0, testDate), crossWake)
        assertEquals(480L, (crossWake - crossBed) / 60_000L)

        // Post-midnight: 1:30 AM -> 8:00 AM = today 01:30 to today 08:00 (390m)
        val (postBed, postWake) = SmartAssistantCodecs.resolveLastNightSleepInterval(
            bedHour = 1,
            bedMinute = 30,
            wakeHour = 8,
            wakeMinute = 0,
            nowMillis = nowMorning,
            zoneId = zone
        )
        assertEquals(epochAt(1, 30, testDate), postBed)
        assertEquals(epochAt(8, 0, testDate), postWake)
        assertEquals(390L, (postWake - postBed) / 60_000L)

        // Wake time in the future (10:00 AM when now is 08:00 AM) anchors to yesterday
        val nowEarly = epochAt(8, 0)
        val (pastBed, pastWake) = SmartAssistantCodecs.resolveLastNightSleepInterval(
            bedHour = 23,
            bedMinute = 0,
            wakeHour = 10,
            wakeMinute = 0,
            nowMillis = nowEarly,
            zoneId = zone
        )
        assertEquals(epochAt(23, 0, testDate.minusDays(2)), pastBed)
        assertEquals(epochAt(10, 0, testDate.minusDays(1)), pastWake)
        assertTrue("Wake time must never land in the future", pastWake <= nowEarly)
    }

    @Test
    fun `proactive coordinator ignores AUTO_DEFAULT sleep logs and respects MANUAL sleep logs`() {
        val nowMorning = epochAt(8, 45) // SLEEP_RECAP default is 8:30
        val configsJson = SmartAssistantCodecs.encodeCheckInConfigs(
            CheckInLogType.entries.map {
                CheckInConfigEntry(type = it, enabled = it == CheckInLogType.SLEEP_RECAP)
            }
        )
        val prefs = UserPreferences(
            checkInsMasterEnabled = true,
            checkInConfigJson = configsJson
        )
        val autoDefaultSleep = SleepLogEntity(
            id = "auto-1",
            startAt = epochAt(23, 0, testDate.minusDays(1)),
            endAt = epochAt(7, 0, testDate),
            durationMinutes = 480,
            source = "AUTO_DEFAULT"
        )

        val dueWithOnlyAuto = ProactiveCheckInCoordinator.evaluateNextDueCheckIn(
            prefs = prefs,
            nowMillis = nowMorning,
            isFocusSessionActive = false,
            sleepLogs = listOf(autoDefaultSleep),
            zoneId = zone
        )
        assertEquals(
            "Synthetic AUTO_DEFAULT sleep log must not suppress morning Sleep Check-In",
            CheckInLogType.SLEEP_RECAP,
            dueWithOnlyAuto?.type
        )

        val manualSleep = autoDefaultSleep.copy(id = "manual-1", source = "MANUAL")
        val dueWithManual = ProactiveCheckInCoordinator.evaluateNextDueCheckIn(
            prefs = prefs,
            nowMillis = nowMorning,
            isFocusSessionActive = false,
            sleepLogs = listOf(autoDefaultSleep, manualSleep),
            zoneId = zone
        )
        assertNull("Real MANUAL sleep log should mark Sleep Check-In as covered", dueWithManual)
    }

    // --- 7. Goal Tags, Priority & Target Date Auto-Schedule Integration Tests ---

    @Test
    fun `mergeTagsCsv merges and deduplicates goal and task tags case-insensitively`() {
        val merged = com.neuroflow.app.domain.scheduler.SchedulableTaskMapper.mergeTagsCsv(
            primaryCsv = "#Coding, backend",
            secondaryCsv = "coding, deepwork, #Backend"
        )
        assertEquals("Coding,backend,deepwork", merged)
    }

    @Test
    fun `toSchedulableTask inherits parent goal tags and boosts risk for high priority or approaching target date`() {
        val now = epochAt(9, 0)
        val highGoal = com.neuroflow.app.data.local.entity.GoalEntity(
            id = "goal_high",
            title = "Ship Android App",
            priority = Priority.HIGH,
            tags = "coding,deepwork",
            targetDate = null
        )
        val urgentGoal = com.neuroflow.app.data.local.entity.GoalEntity(
            id = "goal_urgent",
            title = "Final Exam",
            priority = Priority.MEDIUM,
            tags = "study",
            targetDate = now + 3L * 86_400_000L // 3 days away (within 7 days)
        )
        val plainStep = TaskEntity(
            id = "step_1",
            title = "Implement OAuth callback",
            goalId = highGoal.id,
            priority = Priority.MEDIUM,
            tags = ""
        )
        val urgentStep = TaskEntity(
            id = "step_2",
            title = "Review chapter 5",
            goalId = urgentGoal.id,
            priority = Priority.LOW,
            tags = ""
        )

        val goalsById = mapOf(highGoal.id to highGoal, urgentGoal.id to urgentGoal)

        val mappedHigh = com.neuroflow.app.domain.scheduler.SchedulableTaskMapper.toSchedulableTask(
            task = plainStep,
            dayStartMillis = now,
            goalsById = goalsById
        )
        assertEquals(
            "Step with blank tags should inherit DEEP_WORK category from parent goal's coding/deepwork tags",
            com.neuroflow.app.domain.service.TaskCategory.DEEP_WORK,
            mappedHigh.category
        )
        assertTrue(
            "HIGH-priority goal should elevate goalRiskLevel to at least 1",
            mappedHigh.goalRiskLevel >= 1
        )
        assertTrue(
            "Goal-linked step should set hasGoalAlignment",
            mappedHigh.hasGoalAlignment
        )
        assertTrue(
            "Merged coding/deepwork goal tags should lower fragmentationTolerance below default 0.5f",
            mappedHigh.fragmentationTolerance <= 0.25f
        )

        val mappedUrgent = com.neuroflow.app.domain.scheduler.SchedulableTaskMapper.toSchedulableTask(
            task = urgentStep,
            dayStartMillis = now,
            goalsById = goalsById
        )
        assertEquals(
            "Goal within 7 days of targetDate should elevate goalRiskLevel to 2",
            2,
            mappedUrgent.goalRiskLevel
        )
    }

    // --- 8. Overtime Assistant Morning Suppression & Daytime Slot Priority Tests ---

    @Test
    fun `overtime assistant card stays hidden in the morning even with a deadline tonight`() {
        assertTrue(
            "Morning with normal workday remaining must NOT prompt to enable Overtime",
            !OvertimeAssistantPolicy.shouldShowAssistantCard(
                isEveningShutdownTime = false,
                dailyTargetReached = false,
                isOvertimeActive = false,
                eveningShutdownDismissed = false
            )
        )
        assertTrue(
            "Evening after workDayEnd should show the Evening & Overtime Assistant",
            OvertimeAssistantPolicy.shouldShowAssistantCard(
                isEveningShutdownTime = true,
                dailyTargetReached = false,
                isOvertimeActive = false,
                eveningShutdownDismissed = false
            )
        )
        assertTrue(
            "Active Overtime Mode should remain visible so user can toggle it off",
            OvertimeAssistantPolicy.shouldShowAssistantCard(
                isEveningShutdownTime = false,
                dailyTargetReached = false,
                isOvertimeActive = true,
                eveningShutdownDismissed = false
            )
        )
        assertTrue(
            "Dismissed card stays hidden",
            !OvertimeAssistantPolicy.shouldShowAssistantCard(
                isEveningShutdownTime = true,
                dailyTargetReached = true,
                isOvertimeActive = false,
                eveningShutdownDismissed = true
            )
        )
    }

    @Test
    fun `slot recommender proposes daytime slots in the morning for a low-energy task due tonight`() {
        val nowMorning = epochAt(9, 0)
        val tonightDeadline = epochAt(23, 0)
        val todayMidnight = epochAt(0, 0)
        val prefs = UserPreferences(workDayStart = 9, workDayEnd = 18, wakeUpHour = 7)

        val proposals = TaskSlotRecommender.recommendSlots(
            durationMinutes = 30,
            energyLevel = EnergyLevel.LOW,
            targetDateMillis = todayMidnight,
            deadlineMillis = tonightDeadline,
            existingTasks = emptyList(),
            energyTimeline = emptyList(),
            prefs = prefs,
            nowMillis = nowMorning,
            zoneId = zone
        )

        assertTrue("Should propose slots today", proposals.isNotEmpty())
        assertTrue(
            "Morning task due tonight must pick regular daytime slots, NOT overtime slots",
            proposals.all { !it.isOvertimeSlot }
        )
    }

    // --- 9. Burnout Guard, Persistent 25m Rest Check-In, Auto-Slot & Auto-Plan Prevention Tests ---

    @Test
    fun `burnout guard elevates risk after sustained heavy focus and discharges after logging rest`() {
        val now = epochAt(11, 30)
        val deepWorkTask = TaskEntity(
            id = "dw-1",
            title = "Core Architecture Refactor",
            estimatedDurationMinutes = 95,
            energyLevel = EnergyLevel.HIGH,
            isFrog = true,
            status = TaskStatus.ACTIVE
        )
        val longSession = com.neuroflow.app.data.local.entity.TimeSessionEntity(
            id = "sess-1",
            taskId = deepWorkTask.id,
            startedAt = epochAt(9, 45),
            endedAt = epochAt(11, 25) // 100 min heavy session
        )
        val snapshot = sampleSnapshot(usableEnergy = 48, phase = EnergyPhase.REBOUND, timestampMillis = now)
        val prefsNoRest = UserPreferences(wellnessNudgesEnabled = true)

        val elevated = BurnoutGuardEngine.evaluateBurnoutRisk(
            prefs = prefsNoRest,
            sessions = listOf(longSession),
            completedTasks = emptyList(),
            activeTasks = listOf(deepWorkTask),
            snapshot = snapshot,
            nowMillis = now,
            zoneId = zone
        )
        assertTrue("100m heavy focus without rest must elevate burnout risk", elevated.isElevated)
        assertEquals(BurnoutRiskLevel.HIGH, elevated.riskLevel)
        assertTrue("Unbroken weighted load should be >= 90m", elevated.unbrokenLoadMinutes >= 90)

        // Now user logs a 15-minute rest break right at 11:28
        val restLogJson = SmartAssistantCodecs.encodeRestBreakLogs(
            listOf(
                RestBreakLog(
                    id = "rest-1",
                    completedAtMillis = epochAt(11, 28),
                    durationMinutes = 15,
                    source = RestBreakSource.MANUAL_CONFIRM
                )
            )
        )
        val prefsAfterRest = prefsNoRest.copy(restBreakLogsJson = restLogJson)
        val recovered = BurnoutGuardEngine.evaluateBurnoutRisk(
            prefs = prefsAfterRest,
            sessions = listOf(longSession),
            completedTasks = emptyList(),
            activeTasks = listOf(deepWorkTask),
            snapshot = snapshot,
            nowMillis = now,
            zoneId = zone
        )
        assertEquals("Logging a 15m rest must discharge burnout risk back to CALM", BurnoutRiskLevel.CALM, recovered.riskLevel)
        assertTrue("Discharged load must be much lower than pre-rest load", recovered.unbrokenLoadMinutes < elevated.unbrokenLoadMinutes / 2)
    }

    @Test
    fun `persistent rest check-in re-asks every 25 minutes until rest is logged and never interrupts focus`() {
        val t0 = epochAt(11, 30)
        val highRisk = BurnoutRiskState(
            riskScore = 0.78f,
            riskLevel = BurnoutRiskLevel.HIGH,
            unbrokenLoadMinutes = 95,
            recommendedRestMinutes = 15,
            reasonSummary = "95m unbroken load without rest",
            usableEnergy = 42
        )
        val prefsInitial = UserPreferences(wellnessNudgesEnabled = true)

        // Suppressed during active focus session or active break timer
        assertNull(
            BurnoutGuardEngine.evaluatePersistentRestCheckIn(
                prefs = prefsInitial,
                riskState = highRisk,
                nowMillis = t0,
                isFocusSessionActive = true
            )
        )
        assertNull(
            BurnoutGuardEngine.evaluatePersistentRestCheckIn(
                prefs = prefsInitial,
                riskState = highRisk,
                nowMillis = t0,
                isFocusSessionActive = false,
                isBreakTimerActive = true
            )
        )

        // Prompts immediately when browsing with high burnout risk
        val firstPrompt = BurnoutGuardEngine.evaluatePersistentRestCheckIn(
            prefs = prefsInitial,
            riskState = highRisk,
            nowMillis = t0,
            isFocusSessionActive = false
        )
        assertNotNull("Should ask 'Have you rested?' when burnout is near", firstPrompt)
        assertEquals(25, firstPrompt?.followUpMinutes)

        // User taps 'Not yet' -> snoozed for 25 minutes
        val prefsSnoozed = prefsInitial.copy(
            lastRestCheckInPromptAtMillis = t0,
            nextRestCheckInAllowedAtMillis = t0 + 25 * 60_000L
        )
        assertNull(
            "Should stay quiet 15m after tapping Not Yet",
            BurnoutGuardEngine.evaluatePersistentRestCheckIn(
                prefs = prefsSnoozed,
                riskState = highRisk,
                nowMillis = t0 + 15 * 60_000L,
                isFocusSessionActive = false
            )
        )

        // 25 minutes later, burnout is still elevated and no rest was logged -> re-asks!
        val rePrompt = BurnoutGuardEngine.evaluatePersistentRestCheckIn(
            prefs = prefsSnoozed,
            riskState = highRisk,
            nowMillis = t0 + 25 * 60_000L,
            isFocusSessionActive = false
        )
        assertNotNull("Must re-ask after 25m when burnout stays high without logged rest", rePrompt)
    }

    @Test
    fun `auto-slot inserts recovery buffer when burnout is elevated and breaks up heavy streaks`() {
        val now = epochAt(10, 0)
        val todayMidnight = epochAt(0, 0)
        val prefs = UserPreferences(workDayStart = 9, workDayEnd = 18, wakeUpHour = 7, peakEnergyStart = 9, peakEnergyEnd = 12)

        // Case 1: Active burnout risk right now pushes a new heavy task past a 15m recovery buffer
        val highBurnout = BurnoutRiskState(
            riskScore = 0.75f,
            riskLevel = BurnoutRiskLevel.HIGH,
            unbrokenLoadMinutes = 90,
            recommendedRestMinutes = 15,
            reasonSummary = "90m heavy focus without rest",
            usableEnergy = 45
        )
        val postRecoverySlots = TaskSlotRecommender.recommendSlots(
            durationMinutes = 45,
            energyLevel = EnergyLevel.HIGH,
            targetDateMillis = todayMidnight,
            deadlineMillis = null,
            existingTasks = emptyList(),
            energyTimeline = emptyList(),
            prefs = prefs,
            nowMillis = now,
            zoneId = zone,
            burnoutRiskState = highBurnout
        )
        assertTrue(postRecoverySlots.isNotEmpty())
        val topPostRecovery = postRecoverySlots.first()
        assertTrue(
            "Top heavy slot under high burnout must start after at least a 15m recovery buffer",
            topPostRecovery.startMillis >= epochAt(10, 15)
        )
        assertTrue("Slot should be tagged as including a recovery buffer", topPostRecovery.includesRecoveryBuffer)

        // Case 2: Heavy scheduled task 9:00-10:00 -> new 60m heavy task should leave >=15m recovery buffer rather than stacking at 10:05
        val existingHeavy = TaskEntity(
            id = "heavy-existing",
            title = "Deep Systems Design",
            estimatedDurationMinutes = 60,
            energyLevel = EnergyLevel.HIGH,
            status = TaskStatus.ACTIVE,
            scheduledDate = todayMidnight,
            scheduledTime = 9 * 3600_000L
        )
        val streakSafeSlots = TaskSlotRecommender.recommendSlots(
            durationMinutes = 60,
            energyLevel = EnergyLevel.HIGH,
            targetDateMillis = todayMidnight,
            deadlineMillis = null,
            existingTasks = listOf(existingHeavy),
            energyTimeline = emptyList(),
            prefs = prefs,
            nowMillis = epochAt(8, 30),
            zoneId = zone
        )
        assertTrue(streakSafeSlots.isNotEmpty())
        val topStreakSafe = streakSafeSlots.first()
        assertTrue(
            "Heavy task after a 60m heavy block must leave >= 15m recovery gap (start >= 10:15)",
            topStreakSafe.startMillis >= epochAt(10, 15)
        )
        assertTrue("Should report recovery buffer protection", topStreakSafe.includesRecoveryBuffer)
    }

    @Test
    fun `auto-plan optimizer interleaves lighter tasks and auto-inserts recovery buffers on heavy streaks`() {
        val heavy1 = TaskEntity(id = "h1", title = "Deep Code 1", estimatedDurationMinutes = 60, energyLevel = EnergyLevel.HIGH)
        val heavy2 = TaskEntity(id = "h2", title = "Deep Code 2", estimatedDurationMinutes = 60, energyLevel = EnergyLevel.HIGH)
        val light1 = TaskEntity(id = "l1", title = "Quick Inbox", estimatedDurationMinutes = 30, energyLevel = EnergyLevel.LOW)

        // Scenario A: Heavy1 (9:00-10:00), Heavy2 (10:05-11:05), Light1 (11:10-11:40) -> optimizer interleaves Light1 between Heavy1 & Heavy2
        val packedWithLight = listOf(
            com.neuroflow.app.domain.scheduler.BlueprintTaskItem(heavy1, epochAt(9, 0), epochAt(10, 0), EnergyPhase.PEAK, true),
            com.neuroflow.app.domain.scheduler.BlueprintTaskItem(heavy2, epochAt(10, 5), epochAt(11, 5), EnergyPhase.PEAK, true),
            com.neuroflow.app.domain.scheduler.BlueprintTaskItem(light1, epochAt(11, 10), epochAt(11, 40), EnergyPhase.REBOUND, false)
        )
        val interleaved = BurnoutGuardEngine.optimizeBlueprintForBurnout(
            items = packedWithLight,
            busyWindows = emptyList(),
            dayEndMillis = epochAt(18, 0),
            nowMillis = epochAt(8, 30),
            isToday = true,
            burnoutRisk = BurnoutRiskState()
        )
        assertEquals(
            "Lighter task should be interleaved into index 1 to break up back-to-back heavy tasks",
            light1.id,
            interleaved.items[1].task.id
        )

        // Scenario B: Two back-to-back heavy tasks with NO lighter task -> optimizer inserts a 15m Recovery Buffer
        val heavyOnly = listOf(
            com.neuroflow.app.domain.scheduler.BlueprintTaskItem(heavy1, epochAt(9, 0), epochAt(10, 0), EnergyPhase.PEAK, true),
            com.neuroflow.app.domain.scheduler.BlueprintTaskItem(heavy2, epochAt(10, 5), epochAt(11, 5), EnergyPhase.PEAK, true)
        )
        val buffered = BurnoutGuardEngine.optimizeBlueprintForBurnout(
            items = heavyOnly,
            busyWindows = emptyList(),
            dayEndMillis = epochAt(18, 0),
            nowMillis = epochAt(8, 30),
            isToday = true,
            burnoutRisk = BurnoutRiskState(),
            breakDurationMinutes = 15
        )
        assertTrue("Optimizer must insert a recovery buffer between heavy streaks", buffered.recoveryBuffers.isNotEmpty())
        val gapAfterOptimization = (buffered.items[1].proposedStartMillis - buffered.items[0].proposedEndMillis) / 60_000L
        assertTrue("Second heavy task must be shifted to leave >= 15m recovery gap", gapAfterOptimization >= 15L)
    }
}
