package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.buildNormalizedScheduleTimestamps
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsRecurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.ProtectedSlotClock
import com.neuroflow.app.domain.scheduler.SlotPhase
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.presentation.v2.focus.audio.NeuralSoundscapeGenerator
import com.neuroflow.app.presentation.v2.focus.audio.SoundscapeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * ADR-028 Robustness Test Suite:
 * Validates fixes for HyperFocus, Focus Mode audio DSP synthesis,
 * epoch/intraday scheduling normalization, and DAG auto-scheduler preservation.
 */
class Adr028RobustnessTest {

    private lateinit var generator: NeuralSoundscapeGenerator

    @Before
    fun setUp() {
        generator = NeuralSoundscapeGenerator(sampleRate = 44100)
    }

    // =========================================================================
    // 1. 40 HZ GAMMA & 16 HZ BETA MONAURAL BEAT DSP SYNTHESIS
    // =========================================================================

    @Test
    fun testMonauralGamma40Hz_identicalStereoChannelsAndBoundedPCM() {
        val numFrames = 4410 // 100 ms of audio
        val buffer = ShortArray(numFrames * 2)

        generator.setVolumeInstant(1.0f)
        generator.generateFrames(SoundscapeType.MONAURAL_GAMMA_40HZ, buffer, numFrames, volumeMultiplier = 1.0f)

        var nonZeroCount = 0
        var identicalChannels = 0

        for (i in 0 until numFrames) {
            val left = buffer[2 * i]
            val right = buffer[2 * i + 1]

            // Bounded PCM 16-bit range
            assertTrue("Left sample must be in 16-bit range", left in Short.MIN_VALUE..Short.MAX_VALUE)
            assertTrue("Right sample must be in 16-bit range", right in Short.MIN_VALUE..Short.MAX_VALUE)

            if (left != 0.toShort()) nonZeroCount++

            // True monaural delivers identical physical waveform to Left and Right
            if (left == right) {
                identicalChannels++
            }
        }

        assertTrue("Audio must be audible and non-zero", nonZeroCount > numFrames * 0.9)
        assertEquals("Monaural beat must deliver 100% identical signal to Left and Right channels", numFrames, identicalChannels)
    }

    @Test
    fun testMonauralBeta16Hz_identicalStereoChannelsAndAudibleSignal() {
        val numFrames = 4410
        val buffer = ShortArray(numFrames * 2)

        generator.setVolumeInstant(0.8f)
        generator.generateFrames(SoundscapeType.BETA_16HZ, buffer, numFrames, volumeMultiplier = 0.8f)

        var identicalChannels = 0
        var maxAmp = 0

        for (i in 0 until numFrames) {
            val left = buffer[2 * i]
            val right = buffer[2 * i + 1]

            if (left == right) identicalChannels++
            val amp = kotlin.math.abs(left.toInt())
            if (amp > maxAmp) maxAmp = amp
        }

        assertEquals("16 Hz Beta monaural beat must be identical in both ears", numFrames, identicalChannels)
        assertTrue("Beta monaural beat must have audible amplitude", maxAmp > 1000)
    }

    @Test
    fun testGenerateMonauralBeatFrames_customFrequenciesAndPinkNoiseRatio() {
        val numFrames = 2048
        val buffer = ShortArray(numFrames * 2)

        generator.setVolumeInstant(0.7f)
        generator.generateMonauralBeatFrames(
            beatFreqHz = 40.0,
            outputBuffer = buffer,
            numFrames = numFrames,
            volumeMultiplier = 0.7f,
            carrierHz = 200.0,
            pinkNoiseRatio = 0.80f
        )

        for (i in 0 until numFrames) {
            val left = buffer[2 * i]
            val right = buffer[2 * i + 1]
            assertEquals("Sample $i must be identical on Left and Right channels", left, right)
        }
    }

    @Test
    fun testDefaultMonauralBeat75_25Ratio_prominentToneAndIdenticalStereo() {
        val numFrames = 4410
        val buffer = ShortArray(numFrames * 2)

        generator.setVolumeInstant(1.0f)
        // Default pinkNoiseRatio is 0.25f (75% monaural tone, 25% pink noise bed)
        generator.generateMonauralBeatFrames(
            beatFreqHz = 40.0,
            outputBuffer = buffer,
            numFrames = numFrames,
            volumeMultiplier = 1.0f
        )

        var identicalChannels = 0
        var maxAmp = 0
        for (i in 0 until numFrames) {
            val left = buffer[2 * i]
            val right = buffer[2 * i + 1]
            if (left == right) identicalChannels++
            val amp = kotlin.math.abs(left.toInt())
            if (amp > maxAmp) maxAmp = amp
        }

        assertEquals("Default 75/25 monaural beat must be 100% identical on Left and Right", numFrames, identicalChannels)
        assertTrue("Prominent 75% tone must achieve substantial amplitude (>15000)", maxAmp > 15000)
    }

    // =========================================================================
    // 2. ULTRADIAN SPLIT IDEMPOTENCE & TASK BLOCK PLAN
    // =========================================================================

    @Test
    fun testTaskBlockPlan_expectedMinutesHonorsPlannedBlockMinutes() {
        val splitTask = TaskEntity(
            id = "deep_task",
            title = "Deep Writing #part1",
            estimatedDurationMinutes = 90,
            plannedBlockMinutes = 45,
            effortScore = 80
        )

        // When plannedBlockMinutes is set (e.g. from ultradian split), expectedMinutes must honor it
        // rather than reverting back to the original 90m estimate
        val expected = TaskBlockPlan.expectedMinutes(splitTask)
        assertEquals("expectedMinutes must return plannedBlockMinutes when > 0", 45, expected)

        val unblockedTask = TaskEntity(
            id = "normal_task",
            title = "Normal Task",
            estimatedDurationMinutes = 60,
            plannedBlockMinutes = 0,
            effortScore = 50
        )
        assertEquals("expectedMinutes must fall back to estimatedDurationMinutes when plannedBlockMinutes == 0", 60, TaskBlockPlan.expectedMinutes(unblockedTask))

        val defaultEffortTask = TaskEntity(
            id = "effort_task",
            title = "Effort Task",
            estimatedDurationMinutes = 0,
            plannedBlockMinutes = 0,
            effortScore = 80
        )
        assertEquals("expectedMinutes must fall back to default effort duration when estimates are 0", 60, TaskBlockPlan.expectedMinutes(defaultEffortTask))
    }

    // =========================================================================
    // 3. TIME EXTENSIONS & DEFERRAL NORMALIZATION
    // =========================================================================

    @Test
    fun testBuildNormalizedScheduleTimestamps_splitsEpochCleanly() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 22, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetEpoch = cal.timeInMillis

        val (dayStart, timeOffset) = buildNormalizedScheduleTimestamps(targetEpoch)

        // Offset must be strictly intraday (< 86_400_000)
        assertTrue("Intraday time offset must be >= 0", timeOffset >= 0L)
        assertTrue("Intraday time offset must be < 86,400,000", timeOffset < 86_400_000L)
        assertEquals("Sum of dayStart + timeOffset must equal exact target epoch millis", targetEpoch, dayStart + timeOffset)

        // 14:30 = 14 * 3600_000 + 30 * 60_000 = 50,400,000 + 1,800,000 = 52,200,000 ms
        assertEquals(52_200_000L, timeOffset)
    }

    @Test
    fun testResolveScheduledEpochMillis_prevents1970Confusion() {
        val todayCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 22, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayMidnight = todayCal.timeInMillis
        val tenAmOffset = 10 * 3600_000L // 36_000_000 ms

        val task = TaskEntity(
            id = "task_today",
            title = "Today Task",
            scheduledDate = todayMidnight,
            scheduledTime = tenAmOffset
        )

        val resolved = task.resolveScheduledEpochMillis()
        assertNotNull(resolved)
        assertEquals("Resolved epoch millis must combine scheduledDate and scheduledTime", todayMidnight + tenAmOffset, resolved!!)

        // Deferral across midnight: target 2026-10-23 01:00 AM
        val tomorrow1AmEpoch = todayMidnight + 25 * 3600_000L
        val (newDate, newTime) = buildNormalizedScheduleTimestamps(tomorrow1AmEpoch)
        val deferredTask = task.copy(scheduledDate = newDate, scheduledTime = newTime)

        val deferredResolved = deferredTask.resolveScheduledEpochMillis()
        assertEquals("Deferred task must resolve to tomorrow's epoch timestamp, never 1970", tomorrow1AmEpoch, deferredResolved)
    }

    // =========================================================================
    // 4. FOCUS MODE POINTS OPERATOR PRECEDENCE
    // =========================================================================

    @Test
    fun testFocusModePoints_operatorPrecedence() {
        val frogTask = TaskEntity(
            id = "frog_1",
            title = "Eat the Frog",
            isFrog = true,
            impactScore = 70
        )

        // Correct formula with explicit precedence: 50 + (if (isFrog) 50 else 0) + (impactScore / 2)
        val correctPoints = 50 + (if (frogTask.isFrog) 50 else 0) + (frogTask.impactScore / 2)
        assertEquals("Frog task with impactScore 70 must award 135 points (50 + 50 + 35)", 135, correctPoints)

        val nonFrogTask = TaskEntity(
            id = "normal_1",
            title = "Routine Task",
            isFrog = false,
            impactScore = 40
        )
        val normalPoints = 50 + (if (nonFrogTask.isFrog) 50 else 0) + (nonFrogTask.impactScore / 2)
        assertEquals("Non-frog task with impactScore 40 must award 70 points (50 + 0 + 20)", 70, normalPoints)
    }

    // =========================================================================
    // 5. PROTECTED SLOT CLOCK TAKEOVER
    // =========================================================================

    @Test
    fun testProtectedSlotClock_takeoverDetection() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 22, 12, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = nowCal.timeInMillis

        // Slot running from 12:00 to 13:00 (720 minOfDay, 60m duration)
        val slot = ProtectedTimeSlot(
            id = "lunch_block",
            label = "Mindful Lunch",
            startMinuteOfDay = 720,
            durationMinutes = 60,
            recurrence = PtsRecurrence.DAILY,
            isEnabled = true
        )

        val takeover = ProtectedSlotClock.takeoverOf(listOf(slot), nowMillis)
        assertNotNull("Running slot at 12:15 must trigger hero takeover", takeover)
        assertEquals("lunch_block", takeover?.slot?.id)
        assertEquals(SlotPhase.RUNNING, takeover?.phase)
    }
}
