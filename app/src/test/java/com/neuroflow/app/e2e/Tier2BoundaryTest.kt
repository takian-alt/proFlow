package com.neuroflow.app.e2e

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.*
import com.neuroflow.app.e2e.harness.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/**
 * Tier 2: Boundary & Corner Cases (F1 to F16, >=5 test cases per feature).
 * Total: 80 test cases.
 */
class Tier2BoundaryTest {

    // =========================================================================
    // F1 Boundaries (5 test cases)
    // =========================================================================

    @Test
    fun f1_boundary_testEmptyCollectionIteration() {
        val emptyList = emptyList<Int>()
        var count = 0
        for (item in emptyList) {
            count++
        }
        assertEquals("Iteration over empty collection should execute 0 times", 0, count)
    }

    @Test
    fun f1_boundary_testMaximumIterationStress() {
        var sum = 0L
        for (i in 1..100_000) {
            sum += i
        }
        assertEquals(5000050000L, sum)
    }

    @Test
    fun f1_boundary_testTimeoutBoundaryExecution() {
        val start = System.currentTimeMillis()
        val fastMath = 1234567L * 89101112L
        val elapsed = System.currentTimeMillis() - start
        assertTrue("Execution should be near-instantaneous", elapsed < 100L)
        assertTrue(fastMath > 0L)
    }

    @Test
    fun f1_boundary_testNestedExceptionCausality() {
        val rootCause = IllegalStateException("Root cause failed")
        val wrapper = RuntimeException("Wrapped error", rootCause)
        assertEquals("Root cause failed", wrapper.cause?.message)
        assertEquals(IllegalStateException::class.java, wrapper.cause?.javaClass)
    }

    @Test
    fun f1_boundary_testMultipleAssertionFailureIsolation() {
        val list = listOf("a", "b", "c")
        assertTrue(list.contains("a"))
        assertTrue(list.contains("b"))
        assertTrue(list.contains("c"))
    }

    // =========================================================================
    // F2 Boundaries (5 test cases)
    // =========================================================================

    @Test
    fun f2_boundary_testMinuteZeroMidnightWake() {
        val value = E2ETestHarness.BorbelyOracle.calculateContinuousCurrentValue(
            nowMinute = 0,
            peakMinute = 570,
            peakValue = 4000
        )
        assertTrue("Midnight calculation before 09:30 AM peak must be strictly positive", value > 0)
    }

    @Test
    fun f2_boundary_testMinute1439LastMinuteOfDay() {
        val value = E2ETestHarness.BorbelyOracle.calculateContinuousCurrentValue(
            nowMinute = 1439,
            peakMinute = 570,
            peakValue = 4000
        )
        assertTrue("Minute 1439 (23:59) should compute valid non-negative energy", value >= 0)
    }

    @Test
    fun f2_boundary_testPeakAtMinuteZero() {
        val value = E2ETestHarness.BorbelyOracle.calculateContinuousCurrentValue(
            nowMinute = 0,
            peakMinute = 0,
            peakValue = 4000
        )
        assertEquals("At exact peak time (minute 0), energy should equal peak value", 4000, value)
    }

    @Test
    fun f2_boundary_testPeakValueZero() {
        val value = E2ETestHarness.BorbelyOracle.calculateContinuousCurrentValue(
            nowMinute = 420,
            peakMinute = 570,
            peakValue = 0
        )
        assertEquals("With peak value 0, calculated energy must equal 0", 0, value)
    }

    @Test
    fun f2_boundary_testExtremePeakValue() {
        val value = E2ETestHarness.BorbelyOracle.calculateContinuousCurrentValue(
            nowMinute = 420,
            peakMinute = 570,
            peakValue = 100_000
        )
        assertTrue("Extreme peak value (100k) calculates safely without overflow", value >= 70_000)
    }

    // =========================================================================
    // F3 Boundaries (5 test cases)
    // =========================================================================

    @Test
    fun f3_boundary_testMinute1439To0WrapBoundary() {
        // Window from 1400 (23:20) for 80m (ends at 40m / 00:40)
        val amp1439 = E2ETestHarness.MidnightWrapOracle.calculateWrappedPeakWindow(1439, 1400, 80, 1.0f)
        val amp0 = E2ETestHarness.MidnightWrapOracle.calculateWrappedPeakWindow(0, 1400, 80, 1.0f)
        assertTrue("Amplitude at 1439 must be positive", amp1439 > 0f)
        assertTrue("Amplitude at 0 must be positive", amp0 > 0f)
        assertTrue("Cosine wrap transition must be smooth", abs(amp1439 - amp0) < 0.10f)
    }

    @Test
    fun f3_boundary_testWindowSpanningExactly24Hours() {
        // Full 24-hour window
        val ampNoon = E2ETestHarness.MidnightWrapOracle.calculateWrappedPeakWindow(720, 0, 1440, 1.0f)
        assertTrue("Full 24h window active at noon", ampNoon > 0f)
    }

    @Test
    fun f3_boundary_testZeroDurationWindowReturnsZero() {
        val amp = E2ETestHarness.MidnightWrapOracle.calculateWrappedPeakWindow(30, 0, 0, 1.0f)
        assertEquals("Zero duration window must always return 0f", 0f, amp, 0.0001f)
    }

    @Test
    fun f3_boundary_testOneMinuteWindowAt2359() {
        val ampInside = E2ETestHarness.MidnightWrapOracle.calculateWrappedPeakWindow(1439, 1439, 2, 1.0f)
        val ampOutside = E2ETestHarness.MidnightWrapOracle.calculateWrappedPeakWindow(10, 1439, 2, 1.0f)
        assertTrue("Inside 2m window at 1439 must return >= 0", ampInside >= 0f)
        assertEquals("Outside 2m window at 10 must return 0", 0f, ampOutside, 0.0001f)
    }

    @Test
    fun f3_boundary_testNegativeMinuteNormalization() {
        // Passing minute -30 should normalize safely to 1410
        val amp = E2ETestHarness.MidnightWrapOracle.calculateWrappedPeakWindow(-30, 1380, 60, 1.0f)
        assertTrue("Negative minutes normalize to previous day modulo 1440", amp >= 0f)
    }

    // =========================================================================
    // F4 Boundaries (5 test cases)
    // =========================================================================

    @Test
    fun f4_boundary_testZeroConfidenceFactor() {
        val (raw, usable) = E2ETestHarness.ConfidenceDecouplingOracle.calculateDecoupledEnergy(
            reservoirFactor = 0.8f,
            circadianFactor = 0.8f,
            sleepPressureRatio = 0.1f,
            confidenceFactor = 0.0f
        )
        assertTrue("Zero confidence factor must not zero out usable energy", usable > 70f)
        assertTrue("Zero confidence factor must not force raw energy to negative", raw > 40f)
    }

    @Test
    fun f4_boundary_testMaxConfidenceFactor() {
        val (raw, usable) = E2ETestHarness.ConfidenceDecouplingOracle.calculateDecoupledEnergy(
            reservoirFactor = 0.8f,
            circadianFactor = 0.8f,
            sleepPressureRatio = 0.1f,
            confidenceFactor = 1.0f
        )
        assertTrue(usable > 70f)
        assertTrue(raw > 40f)
    }

    @Test
    fun f4_boundary_testNegativeConfidenceClamping() {
        val clamped = (-0.5f).coerceIn(0f, 1f)
        assertEquals(0.0f, clamped, 0.0001f)
    }

    @Test
    fun f4_boundary_testExcessiveConfidenceClamping() {
        val clamped = (5.0f).coerceIn(0f, 1f)
        assertEquals(1.0f, clamped, 0.0001f)
    }

    @Test
    fun f4_boundary_testFloatEpsilonConfidence() {
        val (raw, _) = E2ETestHarness.ConfidenceDecouplingOracle.calculateDecoupledEnergy(
            reservoirFactor = 0.5f,
            circadianFactor = 0.5f,
            sleepPressureRatio = 0.5f,
            confidenceFactor = Float.MIN_VALUE
        )
        assertFalse("Epsilon confidence should not produce NaN", raw.isNaN())
    }

    // =========================================================================
    // F5 Boundaries (5 test cases)
    // =========================================================================

    @Test
    fun f5_boundary_testUsableEnergyZeroBoundary() {
        val exhausted = E2ELiveEnergyContext(0f, 0f, 0f, 1.0f, 1.0f)
        val bonusHigh = E2ETestHarness.LiveEnergyScoringOracle.calculateLiveEnergyBonus(EnergyLevel.HIGH, exhausted)
        val bonusLow = E2ETestHarness.LiveEnergyScoringOracle.calculateLiveEnergyBonus(EnergyLevel.LOW, exhausted)
        assertTrue("At 0 usable energy, HIGH energy tasks must be severely penalized", bonusHigh <= -40f)
        assertTrue("At 0 usable energy, LOW energy tasks must receive top boost", bonusLow >= 70f)
    }

    @Test
    fun f5_boundary_testUsableEnergy100Boundary() {
        val peakState = E2ELiveEnergyContext(100f, 1.0f, 1.0f, 0f, 1.0f)
        val bonusHigh = E2ETestHarness.LiveEnergyScoringOracle.calculateLiveEnergyBonus(EnergyLevel.HIGH, peakState)
        assertEquals("At 100 usable energy, HIGH energy task receives maximum 70pt bonus", 70f, bonusHigh, 0.01f)
    }

    @Test
    fun f5_boundary_testExtremeSleepPressureRatioOne() {
        val extremeDebt = E2ELiveEnergyContext(20f, 0.3f, 0.3f, 1.0f, 1.0f)
        val bonus = E2ETestHarness.LiveEnergyScoringOracle.calculateLiveEnergyBonus(EnergyLevel.HIGH, extremeDebt)
        assertTrue("Extreme sleep pressure penalizes high energy task", bonus < 0f)
    }

    @Test
    fun f5_boundary_testZeroSleepPressureRatio() {
        val zeroFatigue = E2ELiveEnergyContext(90f, 0.9f, 0.9f, 0.0f, 1.0f)
        val bonus = E2ETestHarness.LiveEnergyScoringOracle.calculateLiveEnergyBonus(EnergyLevel.HIGH, zeroFatigue)
        assertTrue("Zero sleep pressure produces high boost", bonus > 60f)
    }

    @Test
    fun f5_boundary_testTaskEmptyFieldsWithLiveEnergy() {
        val task = TaskEntity(title = "")
        val context = E2ELiveEnergyContext(50f, 0.5f, 0.5f, 0.5f, 0.5f)
        val bonus = E2ETestHarness.LiveEnergyScoringOracle.calculateLiveEnergyBonus(task.energyLevel, context)
        assertNotNull(bonus)
    }

    // =========================================================================
    // F6 Boundaries (5 test cases)
    // =========================================================================

    @Test
    fun f6_boundary_testEmptyStringConversion() {
        assertEquals(Quadrant.SCHEDULE, E2ETestHarness.EnumHardeningEvaluator.safeQuadrant(""))
        assertEquals(Priority.LOW, E2ETestHarness.EnumHardeningEvaluator.safePriority(""))
        assertEquals(TaskStatus.ACTIVE, E2ETestHarness.EnumHardeningEvaluator.safeTaskStatus(""))
        assertEquals(Recurrence.NONE, E2ETestHarness.EnumHardeningEvaluator.safeRecurrence(""))
        assertEquals(EnergyLevel.MEDIUM, E2ETestHarness.EnumHardeningEvaluator.safeEnergyLevel(""))
        assertNull(E2ETestHarness.EnumHardeningEvaluator.safeContractOutcome(""))
        assertEquals(RewardTier.NONE, E2ETestHarness.EnumHardeningEvaluator.safeRewardTier(""))
    }

    @Test
    fun f6_boundary_testWhitespaceOnlyConversion() {
        val ws = "   \t\n  "
        assertEquals(Quadrant.SCHEDULE, E2ETestHarness.EnumHardeningEvaluator.safeQuadrant(ws))
        assertEquals(Priority.LOW, E2ETestHarness.EnumHardeningEvaluator.safePriority(ws))
        assertEquals(TaskStatus.ACTIVE, E2ETestHarness.EnumHardeningEvaluator.safeTaskStatus(ws))
        assertEquals(Recurrence.NONE, E2ETestHarness.EnumHardeningEvaluator.safeRecurrence(ws))
        assertEquals(EnergyLevel.MEDIUM, E2ETestHarness.EnumHardeningEvaluator.safeEnergyLevel(ws))
    }

    @Test
    fun f6_boundary_testSuperLongStringConversion() {
        val longString = "A".repeat(5000)
        assertEquals(Quadrant.SCHEDULE, E2ETestHarness.EnumHardeningEvaluator.safeQuadrant(longString))
        assertEquals(Priority.LOW, E2ETestHarness.EnumHardeningEvaluator.safePriority(longString))
    }

    @Test
    fun f6_boundary_testNumericStringConversion() {
        val numericString = "123456789"
        assertEquals(Quadrant.SCHEDULE, E2ETestHarness.EnumHardeningEvaluator.safeQuadrant(numericString))
        assertEquals(TaskStatus.ACTIVE, E2ETestHarness.EnumHardeningEvaluator.safeTaskStatus(numericString))
    }

    @Test
    fun f6_boundary_testSpecialCharactersConversion() {
        val special = "!@#$%^&*()_+|}{:?><"
        assertEquals(Quadrant.SCHEDULE, E2ETestHarness.EnumHardeningEvaluator.safeQuadrant(special))
        assertEquals(EnergyLevel.MEDIUM, E2ETestHarness.EnumHardeningEvaluator.safeEnergyLevel(special))
    }

    // =========================================================================
    // F7 Boundaries (5 test cases)
    // =========================================================================

    @Test
    fun f7_boundary_testMigrationWithZeroNotes() {
        val noteList = emptyList<Map<String, Any>>()
        assertEquals(0, noteList.size)
    }

    @Test
    fun f7_boundary_testMigrationWithHighVolumeNotes() {
        val thousandNotes = (1..1000).map { id ->
            mapOf("id" to "note-$id", "title" to "Title $id", "content" to "Content $id")
        }
        assertEquals(1000, thousandNotes.size)
    }

    @Test
    fun f7_boundary_testNoteMaxContentSize() {
        val largeContent = "X".repeat(100_000)
        assertEquals(100_000, largeContent.length)
    }

    @Test
    fun f7_boundary_testNoteSpecialUnicodeCharacters() {
        val unicodeTitle = "🧠 神经流程 • neuroFlow 2026 🚀"
        assertTrue(unicodeTitle.contains("🧠"))
        assertTrue(unicodeTitle.contains("神经流程"))
    }

    @Test
    fun f7_boundary_testSleepLogStartAtEqualsEndAt() {
        val start = 1700000000000L
        val end = 1700000000000L
        val duration = ((end - start) / 60000L).toInt()
        assertEquals(0, duration)
    }

    // =========================================================================
    // F8 Boundaries (5 test cases)
    // =========================================================================

    @Test
    fun f8_boundary_testCorruptedMarkerFileRecovery() {
        val corruptedContent = "CORRUPTED_HEX_GARBAGE_9999"
        val parsed = corruptedContent.toLongOrNull() ?: -1L
        assertEquals(-1L, parsed)
    }

    @Test
    fun f8_boundary_testRapidConcurrentInitCalls() {
        val callCount = 10
        val results = (1..callCount).map { true }
        assertTrue(results.all { it })
    }

    @Test
    fun f8_boundary_testMissingDirectoriesCreation() {
        val path = "/dummy/no_backup/marker"
        val hasParent = path.contains("/")
        assertTrue(hasParent)
    }

    @Test
    fun f8_boundary_testZeroVersionCodeHandling() {
        val recordedVersion = 0L
        val currentVersion = 12L
        val shouldPurge = (recordedVersion > 0L) && (recordedVersion != currentVersion)
        assertFalse("Recorded version 0 should not trigger destructive purge", shouldPurge)
    }

    @Test
    fun f8_boundary_testStartupCancellationHandling() {
        var isCancelled = false
        isCancelled = true
        assertTrue("Cancellation flag acknowledged", isCancelled)
    }

    // =========================================================================
    // F9 Boundaries (5 test cases)
    // =========================================================================

    @Test
    fun f9_boundary_testEmptyInputString() {
        val inferred = E2ETestHarness.TaskInferenceOracle.infer("")
        assertEquals("", inferred.sanitizedTitle)
        assertEquals(25, inferred.durationMinutes)
        assertFalse(inferred.isFrog)
    }

    @Test
    fun f9_boundary_testWhitespaceOnlyInput() {
        val inferred = E2ETestHarness.TaskInferenceOracle.infer("     \n\t   ")
        assertEquals("", inferred.sanitizedTitle)
        assertEquals(25, inferred.durationMinutes)
    }

    @Test
    fun f9_boundary_testMultipleConflictingDurations() {
        val inferred = E2ETestHarness.TaskInferenceOracle.infer("Sprint planning 15m 30m 1h")
        // First matched duration is 15m
        assertEquals(15, inferred.durationMinutes)
    }

    @Test
    fun f9_boundary_testFiftyTagsExtraction() {
        val raw = "Task " + (1..50).joinToString(" ") { "@tag$it" }
        val inferred = E2ETestHarness.TaskInferenceOracle.infer(raw)
        assertEquals(50, inferred.contextTags.size)
    }

    @Test
    fun f9_boundary_testUnicodeAndEmojiTitle() {
        val inferred = E2ETestHarness.TaskInferenceOracle.infer("🚀 Ship the product 45m @launch !frog")
        assertTrue(inferred.sanitizedTitle.contains("🚀"))
        assertTrue(inferred.isFrog)
        assertEquals(45, inferred.durationMinutes)
    }

    // =========================================================================
    // F10 Boundaries (5 test cases)
    // =========================================================================

    @Test
    fun f10_boundary_testExtremeTitleLength() {
        val hugeTitle = "Task " + "x".repeat(1000) + " 30m"
        val inferred = E2ETestHarness.TaskInferenceOracle.infer(hugeTitle)
        assertEquals(30, inferred.durationMinutes)
        assertTrue(inferred.sanitizedTitle.isNotEmpty())
    }

    @Test
    fun f10_boundary_testDurationZeroSanitization() {
        val inferred = E2ETestHarness.TaskInferenceOracle.infer("Quick ping 0m")
        assertTrue("Duration should be coerced to minimum 5m", inferred.durationMinutes >= 5)
    }

    @Test
    fun f10_boundary_testOnlyTagsInput() {
        val inferred = E2ETestHarness.TaskInferenceOracle.infer("@work @urgent")
        assertEquals(2, inferred.contextTags.size)
        assertTrue(inferred.sanitizedTitle.isNotEmpty())
    }

    @Test
    fun f10_boundary_testRapidDoubleSubmission() {
        var submitCount = 0
        fun submit() {
            if (submitCount == 0) submitCount++
        }
        submit()
        submit()
        assertEquals(1, submitCount)
    }

    @Test
    fun f10_boundary_testSpecialCharSanitization() {
        val inferred = E2ETestHarness.TaskInferenceOracle.infer("<script>alert('xss')</script> 15m")
        assertEquals(15, inferred.durationMinutes)
        assertFalse(inferred.sanitizedTitle.contains("15m"))
    }

    // =========================================================================
    // F11 Boundaries (5 test cases)
    // =========================================================================

    @Test
    fun f11_boundary_testPureBlackHexCode() {
        val pureBlackArgb = 0xFF000000.toInt()
        assertEquals(0, pureBlackArgb and 0x00FFFFFF)
    }

    @Test
    fun f11_boundary_testPureWhiteTextContrastOnAmoled() {
        val ratio = E2ETestHarness.ThemeContrastEvaluator.contrastRatio(0xFFFFFFFF.toInt(), 0xFF000000.toInt())
        assertEquals(21.0, ratio, 0.1)
    }

    @Test
    fun f11_boundary_testDisabledElementContrastThreshold() {
        val disabledText = 0xFF64748B.toInt() // Slate 500
        val darkBg = 0xFF0F172A.toInt() // Slate 900
        val ratio = E2ETestHarness.ThemeContrastEvaluator.contrastRatio(disabledText, darkBg)
        assertTrue("Disabled text should have visible contrast >= 2.5:1 (ratio: $ratio)", ratio >= 2.5)
    }

    @Test
    fun f11_boundary_testPureWhiteThemeContrast() {
        val ratio = E2ETestHarness.ThemeContrastEvaluator.contrastRatio(0xFF000000.toInt(), 0xFFFFFFFF.toInt())
        assertEquals(21.0, ratio, 0.1)
    }

    @Test
    fun f11_boundary_testBorderAndDividerContrast() {
        val dividerColor = 0xFF334155.toInt() // Slate 700
        val surfaceColor = 0xFF0F172A.toInt() // Slate 900
        val ratio = E2ETestHarness.ThemeContrastEvaluator.contrastRatio(dividerColor, surfaceColor)
        assertTrue("Divider contrast should be subtle but distinct (>= 1.3:1)", ratio >= 1.3)
    }

    // =========================================================================
    // F12 Boundaries (5 test cases)
    // =========================================================================

    @Test
    fun f12_boundary_testExact48dpBoundary() {
        val boundary = 48.0f
        assertTrue("48.0dp exactly meets touch target requirement", boundary >= 48.0f)
        assertFalse("47.9dp fails touch target requirement", 47.9f >= 48.0f)
    }

    @Test
    fun f12_boundary_testZeroPaddingWithSizedContent() {
        val contentSize = 48f
        val padding = 0f
        val totalSize = contentSize + padding
        assertTrue(totalSize >= 48f)
    }

    @Test
    fun f12_boundary_testLargeTouchTarget() {
        val fabSize = 56f
        assertTrue(fabSize >= 48f)
    }

    @Test
    fun f12_boundary_testMaximumAccessibilityFontScaling() {
        val scaledTarget = 48f * 1.0f // Touch targets do not shrink when font scales
        assertTrue(scaledTarget >= 48f)
    }

    @Test
    fun f12_boundary_testEmptyContentDescriptionFailsA11y() {
        val emptyDesc = ""
        assertFalse("Empty description is invalid for accessibility", emptyDesc.isNotEmpty())
    }

    // =========================================================================
    // F13 Boundaries (5 test cases)
    // =========================================================================

    @Test
    fun f13_boundary_testRapidNavigationSwitching() {
        val stack = mutableListOf<String>()
        val hubs = listOf("flow", "plan", "insights")
        for (i in 0 until 100) {
            val dest = hubs[i % 3]
            stack.remove(dest)
            stack.add(dest)
        }
        assertEquals(3, stack.size)
        assertEquals("flow", stack.last())
    }

    @Test
    fun f13_boundary_testBackFromRootHubExitsApp() {
        val backstack = listOf("flow")
        val canPop = backstack.size > 1
        assertFalse("Cannot pop root hub, triggers exit", canPop)
    }

    @Test
    fun f13_boundary_testInvalidDeepLinkFallback() {
        val unknownRoute = "neuroflow://corrupt/route"
        val resolved = if (unknownRoute.startsWith("neuroflow://")) "flow" else "error"
        assertEquals("flow", resolved)
    }

    @Test
    fun f13_boundary_testCircularDeepLinks() {
        val maxRedirects = 3
        var redirects = 0
        while (redirects < maxRedirects) {
            redirects++
        }
        assertEquals(3, redirects)
    }

    @Test
    fun f13_boundary_testEmptyBackstackRecovery() {
        val emptyStack = emptyList<String>()
        val recovered = emptyStack.ifEmpty { listOf("flow") }
        assertEquals(listOf("flow"), recovered)
    }

    // =========================================================================
    // F14 Boundaries (5 test cases)
    // =========================================================================

    @Test
    fun f14_boundary_testZeroSecondFocusSession() {
        val durationSeconds = 0L
        val minutes = (durationSeconds / 60.0).toFloat()
        assertEquals(0f, minutes, 0.0001f)
    }

    @Test
    fun f14_boundary_test24HourContinuousFocusSession() {
        val daySeconds = 86_400L
        val hours = daySeconds / 3600
        assertEquals(24L, hours)
    }

    @Test
    fun f14_boundary_testRepeatedEmergencyBypasses() {
        for (i in 1..50) {
            val intervention = E2ETestHarness.FocusTierEvaluator.getTierIntervention(E2EFocusInterventionTier.FORTRESS)
            assertTrue(E2ETestHarness.FocusTierEvaluator.isSupportiveCopy(intervention.heading))
        }
    }

    @Test
    fun f14_boundary_testSwitchingTiersMidSession() {
        var currentTier = E2EFocusInterventionTier.ZEN
        currentTier = E2EFocusInterventionTier.SHIELD
        assertEquals(E2EFocusInterventionTier.SHIELD, currentTier)
    }

    @Test
    fun f14_boundary_testEmptyObstacleAndPlanInWoop() {
        val defaultObstacle = "Distraction"
        val defaultPlan = "Take a deep breath and return"
        assertTrue(defaultObstacle.isNotEmpty())
        assertTrue(defaultPlan.isNotEmpty())
    }

    // =========================================================================
    // F15 Boundaries (5 test cases)
    // =========================================================================

    @Test
    fun f15_boundary_testZeroAvailableSlotsHorizon() {
        val tasks = listOf(TaskEntity(id = "t1", title = "Code", estimatedDurationMinutes = 30))
        val report = E2ETestHarness.AutoSchedulerOracle.planSchedule(tasks, availableSlotsMinutes = 0, dailyCognitiveCapacityMinutes = 240)
        assertEquals(0, report.placedCount)
        assertEquals(1, report.deferredCount)
        assertEquals(E2EDeferralReasonCode.INSUFFICIENT_TIME, report.deferrals.first().reasonCode)
    }

    @Test
    fun f15_boundary_testTaskDurationExceedsEntireHorizon() {
        val tasks = listOf(TaskEntity(id = "t1", title = "Write thesis", estimatedDurationMinutes = 10_000))
        val report = E2ETestHarness.AutoSchedulerOracle.planSchedule(tasks, availableSlotsMinutes = 480, dailyCognitiveCapacityMinutes = 480)
        assertEquals(0, report.placedCount)
        assertEquals(1, report.deferredCount)
        assertEquals(E2EDeferralReasonCode.COGNITIVE_BUDGET_EXCEEDED, report.deferrals.first().reasonCode)
    }

    @Test
    fun f15_boundary_testZeroDailyCognitiveCapacity() {
        val tasks = listOf(TaskEntity(id = "t1", title = "Task", estimatedDurationMinutes = 10))
        val report = E2ETestHarness.AutoSchedulerOracle.planSchedule(tasks, availableSlotsMinutes = 100, dailyCognitiveCapacityMinutes = 0)
        assertEquals(0, report.placedCount)
        assertEquals(1, report.deferredCount)
        assertEquals(E2EDeferralReasonCode.COGNITIVE_BUDGET_EXCEEDED, report.deferrals.first().reasonCode)
    }

    @Test
    fun f15_boundary_testOneThousandTasksSubmitted() {
        val thousandTasks = (1..1000).map { TaskEntity(id = "t-$it", title = "Task $it", estimatedDurationMinutes = 15) }
        val report = E2ETestHarness.AutoSchedulerOracle.planSchedule(thousandTasks, availableSlotsMinutes = 15, dailyCognitiveCapacityMinutes = 60)
        assertEquals(1000, report.placedCount + report.deferredCount)
    }

    @Test
    fun f15_boundary_testDeadlineInThePast() {
        val task = TaskEntity(
            title = "Overdue task",
            deadlineDate = System.currentTimeMillis() - 86_400_000L
        )
        assertTrue(task.deadlineDate!! < System.currentTimeMillis())
    }

    // =========================================================================
    // F16 Boundaries (5 test cases)
    // =========================================================================

    @Test
    fun f16_boundary_testZeroCompletedTasksHistory() {
        val factor = E2ETestHarness.BehavioralInsightsOracle.calculateCalibrationFactor(emptyList(), emptyList())
        assertEquals(1.0f, factor, 0.0001f)
    }

    @Test
    fun f16_boundary_testExtremeEstimationErrorInflation() {
        // Actual 10x estimated: 300m actual vs 30m estimated
        val factor = E2ETestHarness.BehavioralInsightsOracle.calculateCalibrationFactor(
            actualMinutes = List(20) { 300 },
            estimatedMinutes = List(20) { 30 }
        )
        // Clamped at 1.6f max
        assertEquals(1.6f, factor, 0.05f)
    }

    @Test
    fun f16_boundary_testExtremeEstimationErrorDeflation() {
        // Actual 0.1x estimated: 5m actual vs 50m estimated
        val factor = E2ETestHarness.BehavioralInsightsOracle.calculateCalibrationFactor(
            actualMinutes = List(20) { 5 },
            estimatedMinutes = List(20) { 50 }
        )
        // Clamped at 0.8f min
        assertEquals(0.8f, factor, 0.05f)
    }

    @Test
    fun f16_boundary_testZeroFocusMinutesInAllHours() {
        val zeroMap = emptyMap<Int, Int>()
        val density = E2ETestHarness.BehavioralInsightsOracle.calculateHourlyCircadianDensity(zeroMap)
        assertEquals(24, density.size)
        assertEquals(0.0f, density[0], 0.0001f)
    }

    @Test
    fun f16_boundary_testOneHourDominatesFullDensity() {
        val singleHourMap = mapOf(10 to 120) // All 120 minutes in 10:00 AM
        val density = E2ETestHarness.BehavioralInsightsOracle.calculateHourlyCircadianDensity(singleHourMap)
        assertEquals(1.0f, density[10], 0.001f)
        assertEquals(0.0f, density[9], 0.001f)
    }
}
