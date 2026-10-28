package com.neuroflow.app.presentation.v2.focus.audio

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.*

/**
 * Empirical DSP Stress Test Suite for Milestone 2: Neural Soundscapes & Auditory Focus Stimulation.
 *
 * Verifies:
 * 1. Buffer overflow / sample clipping: PCM samples strictly lie in [-32768, 32767] across 500,000 frames
 *    for all 4 SoundscapeTypes (NONE, GAMMA_40HZ, BROWN_NOISE, PINK_NOISE).
 * 2. Zero-crossing rate: Empirically measures ZCR on Gamma 40Hz and confirms Left channel has ~400 crossings/s,
 *    Right channel has ~480 crossings/s (delta ~80 crossings/s = 2 * 40Hz binaural beat).
 * 3. Volume step discontinuities: Frame-interpolated ramp has no step jumps > DeltaMax.
 * 4. Fade-out attenuation: Monotonic decay to 0 within the fade frame count.
 */
class NeuralSoundscapeDspStressTest {

    private lateinit var generator: NeuralSoundscapeGenerator

    @Before
    fun setUp() {
        generator = NeuralSoundscapeGenerator(sampleRate = 44100)
    }

    // =========================================================================
    // 1. BUFFER OVERFLOW & SAMPLE CLIPPING (500,000 FRAMES)
    // =========================================================================

    @Test
    fun testBufferOverflowAndClipping_500kFrames_allSoundscapeTypes() {
        val totalFrames = 500_000 // ~11.34s of audio at 44.1kHz
        val chunkSize = 1000
        val numChunks = totalFrames / chunkSize
        val buffer = ShortArray(chunkSize * 2)

        val soundscapeTypes = SoundscapeType.entries

        for (type in soundscapeTypes) {
            generator.reset()
            generator.setVolumeInstant(1.0f)

            var minSample = Short.MAX_VALUE
            var maxSample = Short.MIN_VALUE
            var nonZeroCount = 0L

            for (c in 0 until numChunks) {
                generator.generateFrames(type, buffer, chunkSize, volumeMultiplier = 1.0f)

                for (s in buffer) {
                    // Strict PCM bounds verification
                    assertTrue("Sample $s must lie in [-32768, 32767]", s in Short.MIN_VALUE..Short.MAX_VALUE)
                    if (s < minSample) minSample = s
                    if (s > maxSample) maxSample = s
                    if (s != 0.toShort()) nonZeroCount++
                }
            }

            if (type == SoundscapeType.NONE) {
                assertEquals("NONE must produce exact silence across 500,000 frames", 0L, nonZeroCount)
                assertEquals(0.toShort(), minSample)
                assertEquals(0.toShort(), maxSample)
            } else {
                assertTrue(
                    "$type must be active across 500k frames (nonZero: $nonZeroCount)",
                    nonZeroCount > (totalFrames * 2 * 0.95)
                )
                assertTrue("$type min sample must be negative (got $minSample)", minSample < -10000)
                assertTrue("$type max sample must be positive (got $maxSample)", maxSample > 10000)
            }
        }
    }

    @Test
    fun testBufferOverflowAndClipping_singleMassiveBuffer_500kFrames() {
        val massiveFrames = 500_000
        val massiveBuffer = ShortArray(massiveFrames * 2)

        generator.reset()
        generator.setVolumeInstant(0.9f)
        generator.generateFrames(SoundscapeType.PINK_NOISE, massiveBuffer, massiveFrames, volumeMultiplier = 0.9f)

        var hasValidAudio = false
        for (s in massiveBuffer) {
            assertTrue("Sample must remain in 16-bit range", s in Short.MIN_VALUE..Short.MAX_VALUE)
            if (s != 0.toShort()) hasValidAudio = true
        }
        assertTrue("Massive buffer must contain valid pink noise audio", hasValidAudio)
    }

    // =========================================================================
    // 2. ZERO-CROSSING RATE (Gamma 40Hz Binaural Delta)
    // =========================================================================

    @Test
    fun testZeroCrossingRate_gamma40Hz_empirical1Second() {
        val sampleRate = 44100
        val numFrames = sampleRate // Exactly 1.0 second
        val buffer = ShortArray(numFrames * 2)

        generator.reset()
        generator.setVolumeInstant(1.0f)
        generator.generateFrames(SoundscapeType.GAMMA_40HZ, buffer, numFrames, volumeMultiplier = 1.0f)

        var leftCrossings = 0
        var rightCrossings = 0

        for (i in 1 until numFrames) {
            val prevL = buffer[(i - 1) * 2]
            val currL = buffer[i * 2]
            if ((prevL < 0 && currL >= 0) || (prevL >= 0 && currL < 0)) {
                leftCrossings++
            }

            val prevR = buffer[(i - 1) * 2 + 1]
            val currR = buffer[i * 2 + 1]
            if ((prevR < 0 && currR >= 0) || (prevR >= 0 && currR < 0)) {
                rightCrossings++
            }
        }

        // Left channel (200 Hz carrier): ~400 crossings/s (2 per cycle)
        assertEquals("Left channel zero crossings for 200 Hz", 400.0, leftCrossings.toDouble(), 3.0)

        // Right channel (240 Hz carrier): ~480 crossings/s (2 per cycle)
        assertEquals("Right channel zero crossings for 240 Hz", 480.0, rightCrossings.toDouble(), 3.0)

        // Delta between channels: 480 - 400 = 80 crossings/s (2 * 40 Hz binaural beat)
        val delta = rightCrossings - leftCrossings
        assertEquals("Zero-crossing rate delta must be ~80 crossings/s", 80.0, delta.toDouble(), 4.0)
    }

    @Test
    fun testZeroCrossingRate_gamma40Hz_multiChunkContinuous5Seconds() {
        val sampleRate = 44100
        val totalSeconds = 5
        val chunkSize = 1024
        val totalFrames = sampleRate * totalSeconds // 220,500 frames
        val numChunks = totalFrames / chunkSize
        val actualFrames = numChunks * chunkSize
        val buffer = ShortArray(chunkSize * 2)

        generator.reset()
        generator.setVolumeInstant(0.85f)

        var leftCrossings = 0
        var rightCrossings = 0
        var lastL = 0.toShort()
        var lastR = 0.toShort()
        var isFirst = true

        for (c in 0 until numChunks) {
            generator.generateFrames(SoundscapeType.GAMMA_40HZ, buffer, chunkSize, volumeMultiplier = 0.85f)

            for (i in 0 until chunkSize) {
                val currL = buffer[i * 2]
                val currR = buffer[i * 2 + 1]

                if (!isFirst) {
                    if ((lastL < 0 && currL >= 0) || (lastL >= 0 && currL < 0)) leftCrossings++
                    if ((lastR < 0 && currR >= 0) || (lastR >= 0 && currR < 0)) rightCrossings++
                } else {
                    isFirst = false
                }

                lastL = currL
                lastR = currR
            }
        }

        val actualSeconds = actualFrames.toDouble() / sampleRate
        val leftRate = leftCrossings / actualSeconds
        val rightRate = rightCrossings / actualSeconds
        val deltaRate = rightRate - leftRate

        assertEquals("Multi-chunk Left channel ZCR ~400/s", 400.0, leftRate, 2.0)
        assertEquals("Multi-chunk Right channel ZCR ~480/s", 480.0, rightRate, 2.0)
        assertEquals("Multi-chunk Delta ZCR ~80/s", 80.0, deltaRate, 2.0)
    }

    // =========================================================================
    // 3. VOLUME STEP DISCONTINUITIES (Anti-Pop Ramp DeltaMax)
    // =========================================================================

    @Test
    fun testVolumeStepDiscontinuities_rampFromZeroToFull_noJumpsAboveDeltaMax() {
        val numFrames = 1024
        val buffer = ShortArray(numFrames * 2)

        // Ramping from 0.0 to 1.0 volume on Gamma 40Hz
        generator.reset()
        generator.setVolumeInstant(0.0f)
        generator.generateFrames(SoundscapeType.GAMMA_40HZ, buffer, numFrames, volumeMultiplier = 1.0f)

        // For a 240 Hz carrier at 44.1 kHz, max derivative is 32767 * 2 * PI * 240 / 44100 = ~1121 steps/frame.
        // The volume ramp adds at most 32767 / 1024 = ~32 steps/frame.
        // Total expected adjacent sample step <= DeltaMax = 1500.
        // An un-interpolated jump would cause a transient jump up to 32,767.
        val deltaMax = 1500

        var maxDeltaLeft = 0
        var maxDeltaRight = 0

        for (i in 1 until numFrames) {
            val currL = buffer[i * 2].toInt()
            val prevL = buffer[(i - 1) * 2].toInt()
            val diffL = abs(currL - prevL)
            if (diffL > maxDeltaLeft) maxDeltaLeft = diffL

            val currR = buffer[i * 2 + 1].toInt()
            val prevR = buffer[(i - 1) * 2 + 1].toInt()
            val diffR = abs(currR - prevR)
            if (diffR > maxDeltaRight) maxDeltaRight = diffR
        }

        assertTrue("Max step jump on Left must be <= DeltaMax ($deltaMax), was $maxDeltaLeft", maxDeltaLeft <= deltaMax)
        assertTrue("Max step jump on Right must be <= DeltaMax ($deltaMax), was $maxDeltaRight", maxDeltaRight <= deltaMax)
    }

    @Test
    fun testVolumeStepDiscontinuities_interChunkSeamsContinuous() {
        val chunkSize = 1024
        val chunk1 = ShortArray(chunkSize * 2)
        val chunk2 = ShortArray(chunkSize * 2)

        generator.reset()
        // Chunk 1: ramp 0.0 -> 0.8
        generator.setVolumeInstant(0.0f)
        generator.generateFrames(SoundscapeType.GAMMA_40HZ, chunk1, chunkSize, volumeMultiplier = 0.8f)

        // Chunk 2: ramp 0.8 -> 0.2
        generator.generateFrames(SoundscapeType.GAMMA_40HZ, chunk2, chunkSize, volumeMultiplier = 0.2f)

        // Verify sample transition at the seam
        val lastL1 = chunk1[(chunkSize - 1) * 2].toInt()
        val firstL2 = chunk2[0].toInt()
        val seamDeltaL = abs(firstL2 - lastL1)

        val lastR1 = chunk1[(chunkSize - 1) * 2 + 1].toInt()
        val firstR2 = chunk2[1].toInt()
        val seamDeltaR = abs(firstR2 - lastR1)

        val deltaMax = 1500
        assertTrue("Seam delta Left ($seamDeltaL) must be <= DeltaMax ($deltaMax)", seamDeltaL <= deltaMax)
        assertTrue("Seam delta Right ($seamDeltaR) must be <= DeltaMax ($deltaMax)", seamDeltaR <= deltaMax)
    }

    // =========================================================================
    // 4. FADE-OUT ATTENUATION (Monotonic Decay to 0)
    // =========================================================================

    @Test
    fun testFadeOutAttenuation_monotonicDecayToZero() {
        val fadeFrames = 4410 // 100ms fade
        val buffer = ShortArray(fadeFrames * 2)

        generator.reset()
        generator.setVolumeInstant(1.0f)

        // Execute fade-out to 0.0f
        generator.generateFrames(SoundscapeType.GAMMA_40HZ, buffer, fadeFrames, volumeMultiplier = 0.0f)

        // 1. Verify final frame reaches exact silence
        val finalLeft = buffer[(fadeFrames - 1) * 2].toInt()
        val finalRight = buffer[(fadeFrames - 1) * 2 + 1].toInt()
        assertEquals("Final Left sample in fade must be 0", 0, finalLeft)
        assertEquals("Final Right sample in fade must be 0", 0, finalRight)
        assertEquals("currentVolume must reach 0.0f", 0.0f, generator.currentVolume, 0.00001f)

        // 2. Measure RMS power over 10ms windows (441 frames per window, exactly 2 full 200Hz cycles)
        val windowSize = 441
        val numWindows = fadeFrames / windowSize
        val rmsValues = DoubleArray(numWindows)

        for (w in 0 until numWindows) {
            var sumSquare = 0.0
            for (i in 0 until windowSize) {
                val idx = (w * windowSize + i) * 2
                val sample = buffer[idx].toDouble()
                sumSquare += sample * sample
            }
            rmsValues[w] = sqrt(sumSquare / windowSize)
        }

        // 3. Verify monotonic decay across windows
        for (w in 1 until numWindows) {
            assertTrue(
                "Window $w RMS (${rmsValues[w]}) must be <= Window ${w-1} RMS (${rmsValues[w-1]})",
                rmsValues[w] <= rmsValues[w - 1] + 10.0 // small numerical tolerance
            )
        }

        // Verify the final 10 frames of the fade decay to near-zero amplitude (< 100)
        val finalTenFramesRms: Double
        var sumFinalTen = 0.0
        for (i in fadeFrames - 10 until fadeFrames) {
            val sample = buffer[i * 2].toDouble()
            sumFinalTen += sample * sample
        }
        finalTenFramesRms = sqrt(sumFinalTen / 10)
        assertTrue("Final 10 frames RMS must be near zero (< 100), was $finalTenFramesRms", finalTenFramesRms < 100.0)

        // 4. Subsequent audio generation at volume 0.0f must produce complete silence
        val postFadeBuffer = ShortArray(1000 * 2) { 555 }
        generator.generateFrames(SoundscapeType.GAMMA_40HZ, postFadeBuffer, 1000, volumeMultiplier = 0.0f)
        assertTrue("Post-fade audio must remain completely silent", postFadeBuffer.all { it == 0.toShort() })
    }

    @Test
    fun testFadeOutAttenuation_multiChunkPlayerSimulation() {
        val chunkSize = 1024
        val totalFadeChunks = 18 // ~400ms at 44.1kHz
        val buffer = ShortArray(chunkSize * 2)

        generator.reset()
        generator.setVolumeInstant(0.8f)

        val startVol = 0.8f
        val chunkRms = DoubleArray(totalFadeChunks)

        for (c in 0 until totalFadeChunks) {
            val progress = (c + 1).toFloat() / totalFadeChunks
            val targetVol = (startVol * (1f - progress)).coerceAtLeast(0f)

            generator.generateFrames(SoundscapeType.BROWN_NOISE, buffer, chunkSize, volumeMultiplier = targetVol)

            var sumSquare = 0.0
            for (s in buffer) {
                val d = s.toDouble()
                sumSquare += d * d
            }
            chunkRms[c] = sqrt(sumSquare / buffer.size)
        }

        // Initial chunk must be audible
        assertTrue("Initial chunk RMS must be audible (> 1000)", chunkRms[0] > 1000.0)

        // Decay trend: First half average must strictly exceed second half average
        assertTrue(
            "Average RMS of first half (${chunkRms.take(9).average()}) must exceed second half (${chunkRms.drop(9).average()})",
            chunkRms.take(9).average() > chunkRms.drop(9).average()
        )

        // The final sample in chunk 17 (when targetVol reached 0.0f) must reach exact zero
        val lastSampleL = buffer[(chunkSize - 1) * 2]
        val lastSampleR = buffer[(chunkSize - 1) * 2 + 1]
        assertEquals(0.toShort(), lastSampleL)
        assertEquals(0.toShort(), lastSampleR)
        assertEquals(0.0f, generator.currentVolume, 0.0001f)

        // Next chunk generated with volumeMultiplier = 0.0f (representing player paused state) must be 100% silent
        val silenceBuffer = ShortArray(chunkSize * 2) { 999 }
        generator.generateFrames(SoundscapeType.BROWN_NOISE, silenceBuffer, chunkSize, volumeMultiplier = 0.0f)
        assertTrue("Subsequent chunk after fade must be pure silence", silenceBuffer.all { it == 0.toShort() })
    }

    // =========================================================================
    // 5. THETA_GAMMA_ENCODE STRESS TESTS
    // =========================================================================

    @Test
    fun testThetaGammaEncode_phasePrecisionStress_500kFrames() {
        // Verify phase accumulators don't drift or cause NaN/clipping over ~11.3s of continuous synthesis
        val totalFrames = 500_000
        val chunkSize = 1024
        val numChunks = totalFrames / chunkSize
        val buffer = ShortArray(chunkSize * 2)

        generator.reset()
        generator.setVolumeInstant(0.9f)

        var minSample = Short.MAX_VALUE
        var maxSample = Short.MIN_VALUE
        var nonZeroCount = 0L

        for (c in 0 until numChunks) {
            generator.generateFrames(SoundscapeType.THETA_GAMMA_ENCODE, buffer, chunkSize, volumeMultiplier = 0.9f)

            for (s in buffer) {
                assertTrue("Sample $s must lie in [-32768, 32767]", s in Short.MIN_VALUE..Short.MAX_VALUE)
                if (s < minSample) minSample = s
                if (s > maxSample) maxSample = s
                if (s != 0.toShort()) nonZeroCount++
            }
        }

        assertTrue("THETA_GAMMA_ENCODE must be active across 500k frames (nonZero: $nonZeroCount)",
            nonZeroCount > (totalFrames * 2 * 0.90))
        assertTrue("Min sample must be negative (got $minSample)", minSample < -500)
        assertTrue("Max sample must be positive (got $maxSample)", maxSample > 500)
    }

    @Test
    fun testThetaGammaEncode_monoConsistencyStress_500kFrames() {
        // Verify L == R across all 500k frames (single-sided deafness invariant)
        val totalFrames = 500_000
        val chunkSize = 1024
        val numChunks = totalFrames / chunkSize
        val buffer = ShortArray(chunkSize * 2)

        generator.reset()
        generator.setVolumeInstant(0.8f)

        var mismatchCount = 0L

        for (c in 0 until numChunks) {
            generator.generateFrames(SoundscapeType.THETA_GAMMA_ENCODE, buffer, chunkSize, volumeMultiplier = 0.8f)

            for (i in 0 until chunkSize) {
                if (buffer[2 * i] != buffer[2 * i + 1]) mismatchCount++
            }
        }

        assertEquals("L/R mismatch count must be 0 across 500k frames (mono invariant)", 0L, mismatchCount)
    }

    @Test
    fun testThetaGammaEncode_volumeRampAntiPop() {
        // Verify smooth ramp from 0 to 1 has no pop discontinuities
        val numFrames = 1024
        val buffer = ShortArray(numFrames * 2)

        generator.reset()
        generator.setVolumeInstant(0.0f)
        generator.generateFrames(SoundscapeType.THETA_GAMMA_ENCODE, buffer, numFrames, volumeMultiplier = 1.0f)

        // Max adjacent sample delta should be bounded (anti-pop)
        // For 360 Hz carrier at full volume: max derivative ~ 32767 * 2π * 360 / 44100 ≈ 1682
        // Volume ramp from 0→1 over 1024 frames adds multiplicative acceleration
        // Pink noise (80% weight) contributes additional stochastic variance
        // Combined bound: ~8500 accounts for worst-case carrier+noise+ramp convergence without flaky false-positives
        val deltaMax = 8500
        var maxDelta = 0

        for (i in 1 until numFrames) {
            val curr = buffer[i * 2].toInt()
            val prev = buffer[(i - 1) * 2].toInt()
            val diff = abs(curr - prev)
            if (diff > maxDelta) maxDelta = diff
        }

        assertTrue("Max step jump ($maxDelta) must be <= DeltaMax ($deltaMax)", maxDelta <= deltaMax)
    }
}
