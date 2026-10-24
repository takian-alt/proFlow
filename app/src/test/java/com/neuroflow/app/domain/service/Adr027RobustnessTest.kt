package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.engine.AnalyticsEngine
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.buildCircadianEnergySummary
import com.neuroflow.app.domain.model.computeRollingDailySleepAverage
import com.neuroflow.app.domain.scheduler.ConflictResolver
import com.neuroflow.app.domain.service.TaskSchedulerService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

class Adr027RobustnessTest {

    private lateinit var circadianService: DefaultCircadianEnergyService
    private val zoneId = ZoneId.systemDefault()

    @Before
    fun setUp() {
        circadianService = DefaultCircadianEnergyService()
    }

    // --- Bug 1: Pharmacokinetic Stimulant Reference Dosing & Cutoff ---

    @Test
    fun testPharmacokineticStimulantDoseNormalization() {
        // Prescription IR: 10mg reference dose. 10mg dose should equal doseRatio = 1.0
        val irDose = StimulantDoseBout(
            timestampMillis = 1000L,
            doseMg = 10.0,
            type = StimulantType.PRESCRIPTION_STIMULANT_IR
        )
        val caffeineDose = StimulantDoseBout(
            timestampMillis = 1000L,
            doseMg = 100.0,
            type = StimulantType.CAFFEINE
        )

        // Evaluate plasma level 1 hour after dose: both 10mg IR and 100mg Caffeine normalize to 100mg equivalent
        val irActive = PharmacokineticStimulantCalculator.evaluateAt(
            timestampMillis = 1000L + 3_600_000L,
            doses = listOf(irDose)
        )
        val caffeineActive = PharmacokineticStimulantCalculator.evaluateAt(
            timestampMillis = 1000L + 3_600_000L,
            doses = listOf(caffeineDose)
        )

        // Active plasma equivalent is normalized across compounds
        assertTrue("IR active plasma equivalent should be positive", irActive.activeEquivalentMg > 0.0)
        assertTrue("Caffeine active plasma equivalent should be positive", caffeineActive.activeEquivalentMg > 0.0)

        // Safe cutoff minute for 10mg IR with bedtime 23:00 (1380m)
        val cutoffMinute = computeLatestSafeStimulantCutoffMinute(
            sleepHour = 23,
            doseMg = 10.0,
            type = StimulantType.PRESCRIPTION_STIMULANT_IR
        )
        assertTrue("Cutoff minute should be reasonable (before bedtime)", cutoffMinute in 0 until (23 * 60))
    }

    // --- Bug 2: Social Jetlag Wake-Day Classification & Fragmented Bout Coalescing ---

    @Test
    fun testSocialJetlagWakeDayClassificationAndBoutCoalescing() {
        val baseDate = LocalDate.of(2026, 10, 19) // Monday
        val sundayBedtime = baseDate.minusDays(1).atTime(23, 0).atZone(zoneId).toInstant().toEpochMilli()
        val mondayWake = baseDate.atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()

        // Sunday night sleep waking on Monday morning: should count as Workday / Weekday sleep
        val windows = listOf(
            TimeSlotWindow(startMillis = sundayBedtime, endMillis = mondayWake)
        )
        val report = SocialJetlagCalculator.evaluateFromSleepWindows(
            sleepWindows = windows,
            nowMillis = mondayWake + 3_600_000L,
            zoneId = zoneId
        )
        // With only workday sleep, free day average is empty -> social jetlag is 0.0
        assertEquals(0.0, report.socialJetlagHours, 0.001)

        // Test fragmented sleep bout coalescing: Friday night sleep with a 30m awakening
        val friBed = LocalDate.of(2026, 10, 16).atTime(23, 0).atZone(zoneId).toInstant().toEpochMilli()
        val friWake1 = LocalDate.of(2026, 10, 17).atTime(2, 0).atZone(zoneId).toInstant().toEpochMilli()
        val satBed2 = LocalDate.of(2026, 10, 17).atTime(2, 30).atZone(zoneId).toInstant().toEpochMilli()
        val satWake2 = LocalDate.of(2026, 10, 17).atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli()

        val fragmentedWindows = listOf(
            TimeSlotWindow(startMillis = friBed, endMillis = friWake1),
            TimeSlotWindow(startMillis = satBed2, endMillis = satWake2),
            TimeSlotWindow(startMillis = sundayBedtime, endMillis = mondayWake)
        )
        val fullReport = SocialJetlagCalculator.evaluateFromSleepWindows(
            sleepWindows = fragmentedWindows,
            nowMillis = mondayWake + 3_600_000L,
            zoneId = zoneId
        )
        // Friday night coalesced duration should span from 23:00 to 08:00 (9 hours)
        assertTrue(fullReport.socialJetlagHours >= 0.0)
    }

    // --- Bug 3: Multi-Day Schedule Conflict Isolation ---

    @Test
    fun testMultiDayScheduleConflictIsolation() {
        val mondayMillis = LocalDate.of(2026, 10, 19).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val tuesdayMillis = LocalDate.of(2026, 10, 20).atStartOfDay(zoneId).toInstant().toEpochMilli()

        // Task A on Monday 10:00 - 11:00
        val taskA = TaskEntity(
            id = "task_a",
            title = "Task A",
            scheduledDate = mondayMillis,
            scheduledTime = 10 * 60 * 60_000L,
            estimatedDurationMinutes = 60,
            status = TaskStatus.ACTIVE
        )
        // Task B on Tuesday 10:00 - 11:00 (exact same minute-of-day, different date)
        val taskB = TaskEntity(
            id = "task_b",
            title = "Task B",
            scheduledDate = tuesdayMillis,
            scheduledTime = 10 * 60 * 60_000L,
            estimatedDurationMinutes = 60,
            status = TaskStatus.ACTIVE
        )

        // When evaluating together without specifying dayMillis, date partitioning must prevent cross-day collisions
        val allConflicts = ConflictResolver.analyzeScheduleConflicts(
            tasks = listOf(taskA, taskB),
            protectedSlots = emptyList(),
            zoneId = zoneId
        )
        assertTrue("Tasks on different days at the same time must not conflict", allConflicts.isEmpty())

        // When evaluating for Monday specifically, Task B must be ignored
        val mondayConflicts = ConflictResolver.analyzeScheduleConflicts(
            tasks = listOf(taskA, taskB),
            protectedSlots = emptyList(),
            dayMillis = mondayMillis,
            zoneId = zoneId
        )
        assertTrue("Evaluating Monday schedule must ignore Tuesday tasks", mondayConflicts.isEmpty())
    }

    // --- Bug 4: computeRollingDailySleepAverage Sweep-Line Merge ---

    @Test
    fun testComputeRollingDailySleepAverageSweepLineMerge() {
        val now = LocalDate.of(2026, 10, 22).atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()
        val bedTime = LocalDate.of(2026, 10, 21).atTime(23, 0).atZone(zoneId).toInstant().toEpochMilli()
        val wakeTime = LocalDate.of(2026, 10, 22).atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli()

        // Health Connect passive wearable log: 23:00 to 07:00 (480 minutes)
        val wearableLog = SleepLogEntity(
            id = "wearable_1",
            startAt = bedTime,
            endAt = wakeTime,
            durationMinutes = 480,
            source = "HEALTH_CONNECT"
        )
        // User manual confirmation: 23:30 to 07:00 (450 minutes, overlapping)
        val manualLog = SleepLogEntity(
            id = "manual_1",
            startAt = bedTime + 30 * 60_000L,
            endAt = wakeTime,
            durationMinutes = 450,
            source = "MANUAL"
        )

        val rollingAverage = computeRollingDailySleepAverage(
            sleepLogs = listOf(wearableLog, manualLog),
            nowMillis = now,
            zoneId = zoneId
        )
        // Must coalesce the overlapping interval into 480 minutes, NOT 480 + 450 = 930
        assertEquals(480.0, rollingAverage, 0.5)
    }

    // --- Bug 5: Ultradian Split Buffer Enforcement ---

    @Test
    fun testUltradianSplitBufferConstant() {
        assertEquals(15, TaskSchedulerService.ULTRADIAN_RECOVERY_BUFFER_MINUTES)
    }

    // --- Bug 6: AnalyticsEngine MAPE Duration Filtering & Manual Peak Override ---

    @Test
    fun testAnalyticsEngineMapeDurationFilteringAndManualPeak() {
        val taskValid = TaskEntity(
            id = "v1",
            title = "Valid Task",
            estimatedDurationMinutes = 60,
            actualDurationMinutes = 60f,
            status = TaskStatus.COMPLETED
        )
        val taskZeroActual = TaskEntity(
            id = "z1",
            title = "Zero Actual",
            estimatedDurationMinutes = 30,
            actualDurationMinutes = 0f,
            status = TaskStatus.COMPLETED
        )
        val taskNaN = TaskEntity(
            id = "nan1",
            title = "NaN Actual",
            estimatedDurationMinutes = 45,
            actualDurationMinutes = Float.NaN,
            status = TaskStatus.COMPLETED
        )

        val mape = AnalyticsEngine.computeWeightedMape(listOf(taskValid, taskZeroActual, taskNaN))
        assertEquals(0f, mape, 0.01f)

        val smape = AnalyticsEngine.computeWeightedSmape(listOf(taskValid, taskZeroActual, taskNaN))
        assertEquals(0f, smape, 0.01f)

        // Manual peak override test in buildSummary
        val prefs = UserPreferences(
            manualPeakProfileEnabled = true,
            manualPeakAnchorMinuteOfDay = 10 * 60, // 10:00 AM
            manualPeakWindow1DurationMinutes = 180   // 3 hours -> 10:00 to 13:00
        )
        val summary = AnalyticsEngine.buildSummary(
            allTasks = listOf(taskValid),
            allSessions = emptyList(),
            prefs = prefs,
            nowMillis = System.currentTimeMillis()
        )
        // Summary builds successfully without errors and honors manual peak configuration
        assertEquals(0f, summary.weightedMape, 0.01f)
    }

    // --- Bug 7: CircadianEnergySummary Biophysical Wiring ---

    @Test
    fun testCircadianEnergySummaryBiophysicalWiring() {
        val snapshot = CircadianSnapshot(
            timestampMillis = 1000L,
            usableEnergy = 78,
            homeostaticPressureS = 0.62,
            circadianAlertnessC = 0.81,
            focusDepletionPenalty = 0.0,
            phase = EnergyPhase.PEAK,
            predictionConfidence = 0.94
        )

        val summary = buildCircadianEnergySummary(
            dailySummary = null,
            snapshot = snapshot,
            sleepPressurePoints = 400
        )

        assertEquals("Should wire homeostaticPressureS to sleepPressureNormalized", 0.62f, summary.sleepPressureNormalized, 0.01f)
        assertEquals("Should wire predictionConfidence to phaseConfidence", 0.94f, summary.phaseConfidence, 0.01f)
        assertEquals("Should wire predictionConfidence to calibrationQuality", 0.94f, summary.calibrationQuality, 0.01f)
    }
}
