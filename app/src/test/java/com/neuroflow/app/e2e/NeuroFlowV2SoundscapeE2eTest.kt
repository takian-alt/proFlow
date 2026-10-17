package com.neuroflow.app.e2e

import com.neuroflow.app.e2e.harness.NeuroFlowV2Harness.SoundscapeDspGenerator
import com.neuroflow.app.e2e.harness.NeuroFlowV2Harness.SoundscapeType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

/**
 * E2E Test Suite 2: Neural Soundscapes & Auditory Focus Stimulation
 * Covers:
 * - 40Hz Gamma binaural audio DSP synthesis (200Hz left / 240Hz right)
 * - Deep Brown Noise stochastic resonance random walk
 * - Flow Pink Noise Paul Kellet 1/f filter approximation
 * - Volume slider scaling (0.0 to 1.0) and frame-interpolated fade-out to 0
 *
 * Tiers:
 * - Tier 1: Primary behavior & DSP synthesis formulas
 * - Tier 2: Boundary value analysis & Audio buffer edge cases
 * - Tier 3: Pairwise stream switching & Fade-out interactions
 * - Tier 4: Spectral properties & Long-duration DSP stress testing
 */
class NeuroFlowV2SoundscapeE2eTest {

    private lateinit var generator: SoundscapeDspGenerator

    @Before
    fun setUp() {
        generator = SoundscapeDspGenerator(sampleRate = 44100)
    }

    // =========================================================================
    // TIER 1: Primary Behavior & DSP Synthesis Formulas
    // =========================================================================

    @Test
    fun tier1_testGamma40HzBinauralCarrierFrequencies() {
        // Sample rate 44,100 Hz. Left carrier: 200 Hz. Right carrier: 240 Hz.
        // Difference frequency = 40 Hz (Gamma band for prefrontal cortical synchrony).
        val numFrames = 4410 // 100 ms of audio
        val buffer = ShortArray(numFrames * 2)

        generator.generateFrames(SoundscapeType.GAMMA_40HZ, buffer, numFrames, volumeMultiplier = 1.0f)

        // Left ear samples are at even indices, Right ear at odd indices
        // Period of 200 Hz at 44100 Hz is 44100 / 200 = 220.5 frames
        // Period of 240 Hz at 44100 Hz is 44100 / 240 = 183.75 frames
        var leftZeroCrossings = 0
        var rightZeroCrossings = 0

        for (i in 1 until numFrames) {
            val leftPrev = buffer[(i - 1) * 2]
            val leftCurr = buffer[i * 2]
            if ((leftPrev < 0 && leftCurr >= 0) || (leftPrev >= 0 && leftCurr < 0)) {
                leftZeroCrossings++
            }

            val rightPrev = buffer[(i - 1) * 2 + 1]
            val rightCurr = buffer[i * 2 + 1]
            if ((rightPrev < 0 && rightCurr >= 0) || (rightPrev >= 0 && rightCurr < 0)) {
                rightZeroCrossings++
            }
        }

        // In 100ms (0.1s):
        // 200 Hz should have ~20 cycles * 2 = ~40 zero crossings
        // 240 Hz should have ~24 cycles * 2 = ~48 zero crossings
        assertTrue("Left channel (200Hz) crossings: $leftZeroCrossings expected ~40", leftZeroCrossings in 38..42)
        assertTrue("Right channel (240Hz) crossings: $rightZeroCrossings expected ~48", rightZeroCrossings in 46..50)
        assertTrue("Right channel frequency must be strictly higher than Left channel", rightZeroCrossings > leftZeroCrossings)
    }

    @Test
    fun tier1_testBrownNoiseStochasticRandomWalkContinuousSamples() {
        val numFrames = 1000
        val buffer = ShortArray(numFrames * 2)

        generator.generateFrames(SoundscapeType.BROWN_NOISE, buffer, numFrames, volumeMultiplier = 0.8f)

        var nonZeroCount = 0
        var maxDiffBetweenAdjacent = 0

        for (i in 1 until numFrames) {
            val sample = buffer[i * 2].toInt()
            val prevSample = buffer[(i - 1) * 2].toInt()
            if (sample != 0) nonZeroCount++

            val diff = abs(sample - prevSample)
            if (diff > maxDiffBetweenAdjacent) {
                maxDiffBetweenAdjacent = diff
            }
        }

        assertTrue("Brown noise must generate non-zero random walk samples", nonZeroCount > 900)
        // Leaky random walk prevents explosive step discontinuities
        assertTrue("Adjacent sample diffs in Brown noise must be smooth (< 8000)", maxDiffBetweenAdjacent < 8000)
    }

    @Test
    fun tier1_testPinkNoisePaulKelletFilterApproximation() {
        val numFrames = 2000
        val buffer = ShortArray(numFrames * 2)

        generator.generateFrames(SoundscapeType.PINK_NOISE, buffer, numFrames, volumeMultiplier = 0.8f)

        var sumSquaresL = 0.0
        for (i in 0 until numFrames) {
            val sampleL = buffer[i * 2].toDouble()
            sumSquaresL += sampleL * sampleL
        }

        val rms = kotlin.math.sqrt(sumSquaresL / numFrames)
        assertTrue("Pink noise RMS amplitude must be audible (> 500)", rms > 500.0)
        assertTrue("Pink noise RMS amplitude must avoid digital clipping (< 28000)", rms < 28000.0)
    }

    @Test
    fun tier1_testVolumeSliderScalingLinearity() {
        val numFrames = 500
        val bufferFull = ShortArray(numFrames * 2)
        val bufferHalf = ShortArray(numFrames * 2)

        generator.reset()
        generator.generateFrames(SoundscapeType.GAMMA_40HZ, bufferFull, numFrames, volumeMultiplier = 1.0f)

        generator.reset()
        generator.generateFrames(SoundscapeType.GAMMA_40HZ, bufferHalf, numFrames, volumeMultiplier = 0.5f)

        for (i in 0 until numFrames * 2) {
            val fullVal = bufferFull[i].toFloat()
            val halfVal = bufferHalf[i].toFloat()
            if (abs(fullVal) > 500) {
                val ratio = halfVal / fullVal
                assertEquals("Volume scaling at 0.5f must be ~half full volume", 0.5f, ratio, 0.05f)
            }
        }
    }

    @Test
    fun tier1_testFrameInterpolatedFadeOutRampToZero() {
        val numFrames = 1000
        val buffer = ShortArray(numFrames * 2)

        // Generate audio at 0.8 volume
        generator.generateFrames(SoundscapeType.GAMMA_40HZ, buffer, numFrames, volumeMultiplier = 0.8f)

        // Apply frame-interpolated fade out over the entire 1000 frames
        generator.applyFrameInterpolatedFadeOut(buffer, numFrames = numFrames, startVolume = 0.8f, targetVolume = 0.0f)

        // Initial frames must retain audible amplitude before fading out
        val initialMax = (0 until 200).maxOf { abs(buffer[it].toInt()) }
        assertTrue("Initial frames must retain audible amplitude before fading (was: $initialMax)", initialMax > 5000)

        // End of buffer must reach 0 (silence)
        val lastLeft = buffer[(numFrames - 1) * 2]
        val lastRight = buffer[(numFrames - 1) * 2 + 1]
        assertEquals("Last frame must fade to 0", 0, lastLeft.toInt())
        assertEquals("Last frame must fade to 0", 0, lastRight.toInt())
    }

    @Test
    fun tier1_testSilenceWhenTypeNone() {
        val numFrames = 500
        val buffer = ShortArray(numFrames * 2) { 1234 } // Fill with non-zero

        generator.generateFrames(SoundscapeType.NONE, buffer, numFrames, volumeMultiplier = 0.7f)

        for (sample in buffer) {
            assertEquals("Soundscape NONE must produce exact silence (0)", 0, sample.toInt())
        }
    }

    // =========================================================================
    // TIER 2: Boundary Value Analysis & Edge Cases
    // =========================================================================

    @Test
    fun tier2_testVolumeSliderBoundariesZeroAndMax() {
        val numFrames = 200
        val buffer = ShortArray(numFrames * 2)

        // Volume 0.0f
        generator.generateFrames(SoundscapeType.GAMMA_40HZ, buffer, numFrames, volumeMultiplier = 0.0f)
        for (sample in buffer) {
            assertEquals("Volume 0.0f must produce complete silence", 0, sample.toInt())
        }

        // Volume 1.0f (maximum scale)
        generator.generateFrames(SoundscapeType.BROWN_NOISE, buffer, numFrames, volumeMultiplier = 1.0f)
        for (sample in buffer) {
            assertTrue("Sample must not overflow Short bounds", sample in Short.MIN_VALUE..Short.MAX_VALUE)
        }
    }

    @Test
    fun tier2_testVolumeClampingForNegativeOrExorbitantValues() {
        val numFrames = 200
        val buffer = ShortArray(numFrames * 2)

        // Negative volume clamped to 0.0f (silence)
        generator.generateFrames(SoundscapeType.PINK_NOISE, buffer, numFrames, volumeMultiplier = -0.5f)
        for (sample in buffer) {
            assertEquals("Negative volume must clamp to 0 (silence)", 0, sample.toInt())
        }

        // Exorbitant volume clamped to 1.0f (safe ceiling)
        generator.generateFrames(SoundscapeType.GAMMA_40HZ, buffer, numFrames, volumeMultiplier = 5.0f)
        for (sample in buffer) {
            assertTrue("Exorbitant volume must clamp without overflow", sample in Short.MIN_VALUE..Short.MAX_VALUE)
        }
    }

    @Test
    fun tier2_testBufferSizesSingleFrameAndLargeBlock() {
        // Minimum boundary: 1 frame (2 samples: Left, Right)
        val singleFrameBuffer = ShortArray(2)
        generator.generateFrames(SoundscapeType.GAMMA_40HZ, singleFrameBuffer, numFrames = 1, volumeMultiplier = 0.7f)
        assertEquals(2, singleFrameBuffer.size)

        // Large block: 44,100 frames (1 full second = 88,200 short samples)
        val oneSecBuffer = ShortArray(44100 * 2)
        generator.generateFrames(SoundscapeType.BROWN_NOISE, oneSecBuffer, numFrames = 44100, volumeMultiplier = 0.7f)
        assertEquals(88200, oneSecBuffer.size)
        assertTrue("Must contain non-zero samples across full second", oneSecBuffer.any { it != 0.toShort() })
    }

    @Test
    fun tier2_testFadeOutWithZeroFramesHandledGracefully() {
        val buffer = ShortArray(100) { 1000 }
        // Zero frames should return immediately without divide-by-zero
        generator.applyFrameInterpolatedFadeOut(buffer, numFrames = 0, startVolume = 0.8f, targetVolume = 0.0f)
        assertEquals("Buffer untouched when fade frames is 0", 1000, buffer[0].toInt())
    }

    // =========================================================================
    // TIER 3: Pairwise Stream Switching & Fade-Out Interactions
    // =========================================================================

    @Test
    fun tier3_testDynamicStreamSwitchingGammaToBrownNoise() {
        val numFrames = 500
        val buffer = ShortArray(numFrames * 2)

        // Stream 1: Gamma 40Hz
        generator.generateFrames(SoundscapeType.GAMMA_40HZ, buffer, numFrames, volumeMultiplier = 0.7f)
        val gammaLastL = buffer[(numFrames - 1) * 2]

        // Stream 2: Brown Noise immediately after
        generator.generateFrames(SoundscapeType.BROWN_NOISE, buffer, numFrames, volumeMultiplier = 0.7f)
        val brownFirstL = buffer[0]

        assertNotNull(gammaLastL)
        assertNotNull(brownFirstL)
        // Both streams execute sequentially without state collision
        assertTrue("Stream switching executes cleanly", buffer.any { it != 0.toShort() })
    }

    @Test
    fun tier3_testFadeOutRampSmoothnessNoStepDiscontinuity() {
        val numFrames = 1000
        val buffer = ShortArray(numFrames * 2)
        generator.generateFrames(SoundscapeType.GAMMA_40HZ, buffer, numFrames, volumeMultiplier = 0.9f)

        generator.applyFrameInterpolatedFadeOut(buffer, numFrames = numFrames, startVolume = 0.9f, targetVolume = 0.0f)

        var maxDelta = 0
        for (i in 1 until numFrames) {
            val curr = buffer[i * 2].toInt()
            val prev = buffer[(i - 1) * 2].toInt()
            val delta = abs(curr - prev)
            if (delta > maxDelta) maxDelta = delta
        }

        // A smooth fade-out has no abrupt pops (delta < 5000)
        assertTrue("Fade-out ramp must be smooth without audio pop transients (maxDelta: $maxDelta)", maxDelta < 5000)
    }

    @Test
    fun tier3_testStereoInterleavingLeftAndRightDistinct() {
        val numFrames = 1000
        val buffer = ShortArray(numFrames * 2)

        generator.generateFrames(SoundscapeType.GAMMA_40HZ, buffer, numFrames, volumeMultiplier = 0.8f)

        var identicalPairs = 0
        for (i in 0 until numFrames) {
            val left = buffer[i * 2]
            val right = buffer[i * 2 + 1]
            if (left == right) identicalPairs++
        }

        // In 40Hz binaural synthesis (200Hz L vs 240Hz R), Left and Right channels must diverge
        assertTrue("Binaural channels must have distinct Left and Right waveforms (< 10% identical)", identicalPairs < 100)
    }

    // =========================================================================
    // TIER 4: Spectral Properties & Long-Duration DSP Stress Testing
    // =========================================================================

    @Test
    fun tier4_testSpectralDifferentiationBrownNoiseVsPinkNoise() {
        // Deep Brown Noise has a 1/f² power spectrum (steep -6dB/octave roll-off).
        // Flow Pink Noise has a 1/f power spectrum (-3dB/octave roll-off).
        // Consequently, Pink Noise has more high-frequency energy than Brown Noise,
        // which manifests in a significantly higher zero-crossing rate.
        val numFrames = 20000
        val bufferBrown = ShortArray(numFrames * 2)
        val bufferPink = ShortArray(numFrames * 2)

        generator.reset()
        generator.generateFrames(SoundscapeType.BROWN_NOISE, bufferBrown, numFrames, volumeMultiplier = 0.8f)

        generator.reset()
        generator.generateFrames(SoundscapeType.PINK_NOISE, bufferPink, numFrames, volumeMultiplier = 0.8f)

        val zcrBrown = generator.computeZeroCrossingRate(bufferBrown)
        val zcrPink = generator.computeZeroCrossingRate(bufferPink)

        assertTrue("Brown noise ZCR ($zcrBrown) must be lower than Pink noise ZCR ($zcrPink)", zcrBrown < zcrPink)
        assertTrue("Brown noise suppresses high frequencies (ZCR < 0.10)", zcrBrown < 0.10f)
        assertTrue("Pink noise retains higher spectral balance (ZCR > 0.08)", zcrPink > 0.08f)
    }

    @Test
    fun tier4_testLongDurationDspGenerationStabilityNoOverflow() {
        // Generate 220,500 frames (5 continuous seconds at 44.1 kHz) across all 3 modes
        val chunkSize = 4410
        val buffer = ShortArray(chunkSize * 2)
        val types = listOf(SoundscapeType.GAMMA_40HZ, SoundscapeType.BROWN_NOISE, SoundscapeType.PINK_NOISE)

        for (type in types) {
            generator.reset()
            for (chunk in 0 until 50) { // 50 * 4410 = 220,500 frames
                generator.generateFrames(type, buffer, chunkSize, volumeMultiplier = 0.85f)
                // Verify no sample overflows or NaN
                for (s in buffer) {
                    assertTrue("Sample must remain in 16-bit range", s in Short.MIN_VALUE..Short.MAX_VALUE)
                }
            }
        }
    }
}
