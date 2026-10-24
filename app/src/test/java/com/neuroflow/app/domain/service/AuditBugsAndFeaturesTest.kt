package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.sync.fromBackupMap
import com.neuroflow.app.data.sync.toBackupMap
import com.neuroflow.app.domain.model.FatigueZone
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsCategory
import com.neuroflow.app.domain.model.PtsRecurrence
import com.neuroflow.app.domain.model.buildCircadianEnergySummary
import com.neuroflow.app.domain.scheduler.ConflictResolver
import com.neuroflow.app.domain.scheduler.PlacementStability
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.domain.scheduler.determineCategory
import org.junit.Assert.*
import org.junit.Test

class AuditBugsAndFeaturesTest {

    @Test
    fun testNightOwlSleepHourNormalization() {
        val normalize = { hour: Int -> (if (hour >= 24) hour % 24 else hour).coerceIn(0, 23) }
        assertEquals(0, normalize(24)) // 12:00 AM
        assertEquals(1, normalize(25)) // 1:00 AM
        assertEquals(2, normalize(26)) // 2:00 AM
        assertEquals(23, normalize(23)) // 11:00 PM
        assertEquals(22, normalize(22)) // 10:00 PM
    }

    @Test
    fun testAffirmationEncodingAndDecodingRoundtrip() {
        val sample = listOf("I finish what I start", "Deep focus creates freedom", "Progress over perfection")
        val json = UserPreferencesDataStore.encodeAffirmations(sample)
        val decoded = UserPreferencesDataStore.parseAffirmations(json)
        assertEquals(sample, decoded)
    }

    @Test
    fun testProtectedTimeSlotsEncodingAndDecodingRoundtrip() {
        val sample = listOf(
            ProtectedTimeSlot(
                id = "slot-lunch",
                label = "Lunch Break",
                startMinuteOfDay = 720,
                durationMinutes = 60,
                category = PtsCategory.MEAL,
                recurrence = PtsRecurrence.DAILY,
                weekdayBitmask = 0b1111111,
                recoveryBufferMinutes = 10,
                isEnabled = true
            )
        )
        val json = UserPreferencesDataStore.encodeProtectedTimeSlots(sample)
        val decoded: List<ProtectedTimeSlot> = UserPreferencesDataStore.parseProtectedTimeSlots(json)
        assertEquals(1, decoded.size)
        assertEquals("slot-lunch", decoded[0].id)
        assertEquals("Lunch Break", decoded[0].label)
        assertEquals(720, decoded[0].startMinuteOfDay)
        assertEquals(60, decoded[0].durationMinutes)
        assertEquals(PtsCategory.MEAL, decoded[0].category)
    }

    @Test
    fun testCloudBackupMapIncludesAllPreservedEntities() {
        val prefs = UserPreferences(
            affirmations = listOf("Affirmation 1", "Affirmation 2"),
            tagCatalog = listOf("urgent", "writing"),
            yearlyGoals = listOf("Ship V2", "Run Marathon"),
            weeklyGoals = listOf("Close Milestone 1", "Clean Architecture"),
            protectedTimeSlots = listOf(
                ProtectedTimeSlot(
                    id = "test-slot",
                    label = "Deep Work Shield",
                    startMinuteOfDay = 540,
                    durationMinutes = 90
                )
            ),
            meqScore = 58
        )

        val map = prefs.toBackupMap()
        assertTrue(map.containsKey("affirmations"))
        assertTrue(map.containsKey("tagCatalog"))
        assertTrue(map.containsKey("yearlyGoals"))
        assertTrue(map.containsKey("weeklyGoals"))
        assertTrue(map.containsKey("protectedTimeSlots"))
        assertEquals("58", map["meqScore"])

        val restored = UserPreferences().fromBackupMap(map)
        assertEquals(prefs.affirmations, restored.affirmations)
        assertEquals(prefs.tagCatalog, restored.tagCatalog)
        assertEquals(prefs.yearlyGoals, restored.yearlyGoals)
        assertEquals(prefs.weeklyGoals, restored.weeklyGoals)
        assertEquals(1, restored.protectedTimeSlots.size)
        assertEquals("test-slot", restored.protectedTimeSlots[0].id)
        assertEquals(58, restored.meqScore)
    }

    @Test
    fun testTaskBlockPlanRecognizesAllSplitVariations() {
        val part1Task = TaskEntity(
            id = "task-100",
            title = "Research Paper (Part 1/2)",
            estimatedDurationMinutes = 90,
            plannedBlockMinutes = 45
        )
        val part2Task = TaskEntity(
            id = "task-100_part2",
            title = "Research Paper (Part 2/2)",
            estimatedDurationMinutes = 45,
            plannedBlockMinutes = 45
        )
        val hashPartTask = TaskEntity(
            id = "task-100#part2",
            title = "Research Paper #part2",
            estimatedDurationMinutes = 45,
            plannedBlockMinutes = 45
        )
        val normalTask = TaskEntity(
            id = "task-200",
            title = "Inbox Zero",
            estimatedDurationMinutes = 30,
            plannedBlockMinutes = 0
        )

        assertEquals(45, TaskBlockPlan.expectedMinutes(part1Task))
        assertEquals(45, TaskBlockPlan.expectedMinutes(part2Task))
        assertEquals(45, TaskBlockPlan.expectedMinutes(hashPartTask))
        assertEquals(30, TaskBlockPlan.expectedMinutes(normalTask))
    }

    @Test
    fun testPriorityWeightProfileSupportsAllElevenSliders() {
        val weights = PriorityWeightProfile(
            urgencyWeight = 1.5,
            impactWeight = 1.2,
            circadianMatchWeight = 1.8,
            momentumWeight = 1.1,
            frogBonusMultiplier = 1.5,
            quadrantWeight = 1.4,
            priorityLevelWeight = 1.3,
            durationWeight = 0.9,
            focusModeWeight = 1.2,
            planBoostWeight = 1.1,
            enjoymentWeight = 1.5
        )

        val task = SchedulableTask(
            id = "task-test",
            title = "Heavy Strategy Document",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            isFrog = true,
            distractionScore = 0.2f
        )

        val scheduler = DefaultTaskSchedulerService(DefaultCircadianEnergyService())
        val snapshot = CircadianSnapshot(
            timestampMillis = System.currentTimeMillis(),
            usableEnergy = 85,
            baselineBiologicalEnergy = 85,
            circadianAlertnessC = 0.9,
            homeostaticPressureS = 0.2,
            focusDepletionPenalty = 0.0,
            phase = EnergyPhase.PEAK
        )

        val breakdown = scheduler.evaluateTaskScoreWithWeights(task, snapshot, weights = weights)
        assertTrue(breakdown.totalScore in 0.0..100.0)
        assertTrue(breakdown.impactScore > 50.0)
    }

    @Test
    fun testCircadianEnergySummaryRespectsDailyDeepWorkTarget() {
        val snapshot = CircadianSnapshot(
            timestampMillis = System.currentTimeMillis(),
            usableEnergy = 75,
            baselineBiologicalEnergy = 75,
            circadianAlertnessC = 0.8,
            homeostaticPressureS = 0.25,
            focusDepletionPenalty = 0.0,
            phase = EnergyPhase.PEAK
        )

        val summaryCustom = buildCircadianEnergySummary(
            dailySummary = null,
            snapshot = snapshot,
            dailyDeepWorkTargetMinutes = 300
        )
        assertEquals(300, summaryCustom.cognitiveBudgetMinutesRemaining)

        val summaryFallback = buildCircadianEnergySummary(
            dailySummary = null,
            snapshot = snapshot,
            dailyDeepWorkTargetMinutes = null
        )
        assertEquals(240, summaryFallback.cognitiveBudgetMinutesRemaining)
    }

    @Test
    fun testEisenhowerQuadrantModulationNeverInverts() {
        val scheduler = DefaultTaskSchedulerService(DefaultCircadianEnergyService())
        val snapshot = CircadianSnapshot(
            timestampMillis = System.currentTimeMillis(),
            usableEnergy = 80,
            baselineBiologicalEnergy = 80,
            circadianAlertnessC = 0.8,
            homeostaticPressureS = 0.2,
            focusDepletionPenalty = 0.0,
            phase = EnergyPhase.PEAK
        )

        val doFirstTask = SchedulableTask(
            id = "t1",
            title = "Crucial Urgent Task",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            impactScore = 90,
            deadlineMillis = System.currentTimeMillis() + 3600_000L
        )
        val eliminateTask = SchedulableTask(
            id = "t2",
            title = "Trivial Distraction",
            category = TaskCategory.ADMIN,
            priority = 0,
            estimatedMinutes = 15,
            energyDemand = EnergyDemand.LOW,
            impactScore = 5,
            deadlineMillis = null
        )

        // At quadrantWeight = 1.0 (default baseline)
        val weightsDefault = PriorityWeightProfile(quadrantWeight = 1.0)
        val scoreDoFirstDefault = scheduler.evaluateTaskScoreWithWeights(doFirstTask, snapshot, weights = weightsDefault).totalScore
        val scoreEliminateDefault = scheduler.evaluateTaskScoreWithWeights(eliminateTask, snapshot, weights = weightsDefault).totalScore
        assertTrue("Do First must outscore Eliminate at weight 1.0", scoreDoFirstDefault > scoreEliminateDefault)

        // At quadrantWeight = 0.0 (neutralized, spread = 0.0, zero inversion)
        val weightsZero = PriorityWeightProfile(quadrantWeight = 0.0)
        val scoreDoFirstZero = scheduler.evaluateTaskScoreWithWeights(doFirstTask, snapshot, weights = weightsZero).totalScore
        val scoreEliminateZero = scheduler.evaluateTaskScoreWithWeights(eliminateTask, snapshot, weights = weightsZero).totalScore
        assertTrue("Do First must still outscore Eliminate even with quadrant weight neutralized to 0.0", scoreDoFirstZero > scoreEliminateZero)
    }

    @Test
    fun testWoopBonusAppliesOnlyWhenImplementationIntentionPresent() {
        val scheduler = DefaultTaskSchedulerService(DefaultCircadianEnergyService())
        val snapshot = CircadianSnapshot(
            timestampMillis = System.currentTimeMillis(),
            usableEnergy = 80,
            baselineBiologicalEnergy = 80,
            circadianAlertnessC = 0.8,
            homeostaticPressureS = 0.2,
            focusDepletionPenalty = 0.0,
            phase = EnergyPhase.PEAK
        )

        val taskWithoutWoop = SchedulableTask(
            id = "t-no-woop",
            title = "Plain Task",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.MODERATE,
            hasImplementationIntention = false
        )
        val taskWithWoop = SchedulableTask(
            id = "t-woop",
            title = "Planned Task",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.MODERATE,
            hasImplementationIntention = true
        )

        val weightsBoost = PriorityWeightProfile(planBoostWeight = 2.0)
        val scoreNoWoop = scheduler.evaluateTaskScoreWithWeights(taskWithoutWoop, snapshot, weights = weightsBoost)
        val scoreWithWoop = scheduler.evaluateTaskScoreWithWeights(taskWithWoop, snapshot, weights = weightsBoost)

        assertTrue(
            "Task with WOOP should have higher momentum than task without",
            scoreWithWoop.momentumScore > scoreNoWoop.momentumScore
        )
    }

    @Test
    fun testTaskBlockPlanRecognizesParentTaskIdSubtask() {
        val subtask = TaskEntity(
            id = "child-1",
            title = "Continuation Subtask",
            parentTaskId = "parent-123",
            estimatedDurationMinutes = 90,
            plannedBlockMinutes = 45
        )
        assertEquals(45, TaskBlockPlan.expectedMinutes(subtask))
    }

    @Test
    fun testMeditationAndTeamMeetingSlotsDoNotInjectStimulants() {
        val date = java.time.LocalDate.of(2026, 4, 13)
        val zone = java.time.ZoneId.systemDefault()
        val slots = listOf(
            ProtectedTimeSlot(id = "1", label = "Meditation", startMinuteOfDay = 480, durationMinutes = 30, category = PtsCategory.MINDFULNESS),
            ProtectedTimeSlot(id = "2", label = "Team Meeting", startMinuteOfDay = 600, durationMinutes = 60, category = PtsCategory.WORK),
            ProtectedTimeSlot(id = "3", label = "Medical Checkup", startMinuteOfDay = 700, durationMinutes = 45, category = PtsCategory.CUSTOM),
            ProtectedTimeSlot(id = "4", label = "Morning Coffee", startMinuteOfDay = 500, durationMinutes = 15, category = PtsCategory.BREAK)
        )
        val doses = com.neuroflow.app.domain.model.extractStimulantDosesFromSlots(slots, date, zone)
        assertTrue(doses.all { it.type == StimulantType.CAFFEINE })
        val todayDoses = doses.filter { java.time.Instant.ofEpochMilli(it.timestampMillis).atZone(zone).toLocalDate() == date }
        assertEquals(1, todayDoses.size)
        assertEquals(StimulantType.CAFFEINE, todayDoses[0].type)
    }

    @Test
    fun testContinuousSleepRecoveryInFatigueZone() {
        assertEquals(0, FatigueZone.recoveryForSleepMinutes(0))
        assertEquals(183, FatigueZone.recoveryForSleepMinutes(59))
        assertEquals(186, FatigueZone.recoveryForSleepMinutes(60))
        assertEquals(279, FatigueZone.recoveryForSleepMinutes(90))
        // Verify monotonic continuous ramp without a 127-point cliff
        val diff59to60 = FatigueZone.recoveryForSleepMinutes(60) - FatigueZone.recoveryForSleepMinutes(59)
        assertEquals(3, diff59to60)
    }

    @Test
    fun testDependencyGraphValidatorRetainsCrossDayParentTaskId() {
        val tasks = listOf(
            SchedulableTask(
                id = "child-task",
                title = "Child Task",
                category = TaskCategory.DEEP_WORK,
                priority = 1,
                estimatedMinutes = 30,
                energyDemand = EnergyDemand.MODERATE,
                parentTaskId = "cross-day-parent-999"
            )
        )
        val result = com.neuroflow.app.domain.scheduler.DependencyGraphValidator.sanitizeDag(tasks)
        assertEquals(1, result.tasks.size)
        assertEquals("cross-day-parent-999", result.tasks[0].parentTaskId)
    }

    @Test
    fun testTaskTagSchedulingProfileTokenizedMatching() {
        val smartTask = TaskEntity(id = "1", title = "Smart Contract Review", tags = "smart,contract")
        val artTask = TaskEntity(id = "2", title = "Art Design", tags = "digital-art,ui")
        val pruneTask = TaskEntity(id = "3", title = "Prune branches", tags = "prune,garden")

        assertNotEquals(com.neuroflow.app.domain.scheduler.TaskCategory.CREATIVE, smartTask.determineCategory())
        assertEquals(com.neuroflow.app.domain.scheduler.TaskCategory.CREATIVE, artTask.determineCategory())
        assertNotEquals(com.neuroflow.app.domain.scheduler.TaskCategory.EXERCISE, pruneTask.determineCategory())
    }

    @Test
    fun testPlacementStabilityLockedTaskAlwaysKeeps() {
        val task = TaskEntity(
            id = "locked-1",
            title = "Locked Task",
            isScheduleLocked = true,
            scheduledDate = System.currentTimeMillis() - 100_000L,
            scheduledTime = 100_000L
        )
        val verdict = com.neuroflow.app.domain.scheduler.PlacementStability.verdict(
            task = task,
            nowMillis = System.currentTimeMillis(),
            prefs = UserPreferences(),
            forceReplan = true
        )
        assertEquals(PlacementStability.ReplanVerdict.KEEP, verdict)
    }

    @Test
    fun testMidnightAndCrossMidnightFormatMinutesToTime() {
        assertEquals("12:00 AM", ConflictResolver.formatMinutesToTime(0))
        assertEquals("12:00 AM", ConflictResolver.formatMinutesToTime(1440))
        assertEquals("12:30 AM", ConflictResolver.formatMinutesToTime(1470))
        assertEquals("11:59 PM", ConflictResolver.formatMinutesToTime(1439))
    }

    @Test
    fun testMEQChronotypeDetectorScalesIncompleteQuiz() {
        // User answers 10 questions with score 4 (e.g. total 40)
        val answers = List(10) { 4 } + List(9) { 0 }
        val result = com.neuroflow.app.domain.engine.MEQChronotypeDetector.detect(answers)
        // Scaled score should be round((40 / 10) * 19) = 76 -> DEFINITE_MORNING
        assertEquals(76, result.totalScore)
        assertEquals(com.neuroflow.app.domain.engine.MEQChronotypeDetector.Chronotype.DEFINITE_MORNING, result.chronotype)
    }

    @Test
    fun testCircadianEnergySummaryDynamicWindDownAndDeepWorkDeduction() {
        val snapshot = CircadianSnapshot(
            timestampMillis = System.currentTimeMillis(),
            usableEnergy = 75,
            homeostaticPressureS = 0.3,
            circadianAlertnessC = 0.7,
            focusDepletionPenalty = 0.0,
            phase = EnergyPhase.PEAK
        )
        val summary = buildCircadianEnergySummary(
            dailySummary = null,
            snapshot = snapshot,
            sleepHour = 22,
            dailyDeepWorkTargetMinutes = 240,
            completedDeepWorkMinutes = 90
        )
        assertEquals(java.time.LocalTime.of(20, 0), summary.windDownStart)
        assertEquals(150, summary.cognitiveBudgetMinutesRemaining)
    }
}
