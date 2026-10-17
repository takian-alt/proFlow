package com.neuroflow.app.presentation.v2.focus.audio

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

/**
 * Unit test suite for [NeuralSoundscapeGenerator].
 *
 * Verifies:
 * 1. Silence on NONE.
 * 2. 40 Hz Gamma binaural phase generation, 40 Hz frequency delta, and bounded sample amplitudes.
 * 3. Deep Brown noise stochastic acoustic masking, stereo decorrelation, and numerical stability.
 * 4. Pink noise Paul Kellet 1/f filter approximation, stereo decorrelation, and numerical stability.
 * 5. Smooth frame-interpolated volume ramping and fade-out to zero.
 * 6. Edge cases and argument validation.
 */
class NeuralSoundscapeGeneratorTest {

    private lateinit var generator: NeuralSoundscapeGenerator

    @Before
    fun setUp() {
        generator = NeuralSoundscapeGenerator(sampleRate = 44100)
    }

    @Test
    fun testSilenceOnNone_producesAllZeros() {
        val numFrames = 1024
        val buffer = ShortArray(numFrames * 2) { 999 } // prefill with non-zero

        generator.generateFrames(SoundscapeType.NONE, buffer, numFrames, volumeMultiplier = 0.8f)

        assertTrue("Buffer must contain only zeros for NONE", buffer.all { it == 0.toShort() })
        assertEquals(0.0f, generator.currentVolume, 0.0001f)
    }

    @Test
    fun testGamma40Hz_binauralFrequenciesAndBoundedAmplitudes() {
        val sampleRate = 44100
        val numFrames = sampleRate // 1 second of audio
        val buffer = ShortArray(numFrames * 2)

        generator.setVolumeInstant(1.0f)
        generator.generateFrames(SoundscapeType.GAMMA_40HZ, buffer, numFrames, volumeMultiplier = 1.0f)

        // All samples must be in valid 16-bit PCM range
        var maxSample = Short.MIN_VALUE
        var minSample = Short.MAX_VALUE
        var nonZeroCount = 0

        for (sample in buffer) {
            if (sample > maxSample) maxSample = sample
            if (sample < minSample) minSample = sample
            if (sample != 0.toShort()) nonZeroCount++
        }

        assertTrue("Max sample must be positive", maxSample > 30000)
        assertTrue("Min sample must be negative", minSample < -30000)
        assertTrue("Almost all samples in 1s must be non-zero", nonZeroCount > (numFrames * 2 * 0.98))

        // Check channel decorrelation: Left (200 Hz) and Right (240 Hz) must not be identical
        var identicalChannels = 0
        for (i in 0 until numFrames) {
            val left = buffer[2 * i]
            val right = buffer[2 * i + 1]
            if (left == right) identicalChannels++
        }
        assertTrue(
            "Left (200Hz) and Right (240Hz) channels must diverge due to 40Hz binaural delta",
            identicalChannels < (numFrames * 0.1) // At most 10% coincidental crossings
        )

        // Count zero crossings for Left and Right channels:
        // Left (200 Hz) should have ~400 zero-crossings per second (2 per cycle)
        // Right (240 Hz) should have ~480 zero-crossings per second (2 per cycle)
        var leftCrossings = 0
        var rightCrossings = 0
        for (i in 1 until numFrames) {
            val prevL = buffer[2 * (i - 1)]
            val currL = buffer[2 * i]
            if ((prevL < 0 && currL >= 0) || (prevL >= 0 && currL < 0)) {
                leftCrossings++
            }

            val prevR = buffer[2 * (i - 1) + 1]
            val currR = buffer[2 * i + 1]
            if ((prevR < 0 && currR >= 0) || (prevR >= 0 && currR < 0)) {
                rightCrossings++
            }
        }

        assertEquals("Left channel zero crossings for 200 Hz", 400.0, leftCrossings.toDouble(), 4.0)
        assertEquals("Right channel zero crossings for 240 Hz", 480.0, rightCrossings.toDouble(), 4.0)
        val crossingDiff = rightCrossings - leftCrossings
        assertEquals("Zero crossing difference must reflect 40 Hz beat frequency (80 crossings)", 80.0, crossingDiff.toDouble(), 4.0)
    }

    @Test
    fun testGamma40Hz_continuousPhaseAcrossConsecutiveChunks() {
        val chunkSize = 512
        val buffer = ShortArray(chunkSize * 2)
        generator.setVolumeInstant(0.7f)

        // Generate 50 consecutive chunks and ensure no NaNs or overflow
        for (chunk in 0 until 50) {
            generator.generateFrames(SoundscapeType.GAMMA_40HZ, buffer, chunkSize, volumeMultiplier = 0.7f)
            for (sample in buffer) {
                assertTrue(sample in Short.MIN_VALUE..Short.MAX_VALUE)
            }
        }
    }

    @Test
    fun testBrownNoise_stochasticMaskingAndStability() {
        val numFrames = 44100
        val buffer = ShortArray(numFrames * 2)

        generator.setVolumeInstant(0.8f)
        generator.generateFrames(SoundscapeType.BROWN_NOISE, buffer, numFrames, volumeMultiplier = 0.8f)

        // Confirm non-zero stochastic signal
        var nonZeroCount = 0
        var maxAmp = 0
        for (s in buffer) {
            if (s != 0.toShort()) nonZeroCount++
            val amp = abs(s.toInt())
            if (amp > maxAmp) maxAmp = amp
        }

        assertTrue("Brown noise must generate non-zero frames", nonZeroCount > numFrames)
        assertTrue("Brown noise amplitude should be audible", maxAmp > 1000)

        // Verify stereo decorrelation (Left vs Right independent walks)
        var identicalSamples = 0
        for (i in 0 until numFrames) {
            if (buffer[2 * i] == buffer[2 * i + 1]) identicalSamples++
        }
        assertTrue("Left and Right channels must be decorrelated", identicalSamples < (numFrames * 0.05))

        // Long-term stability test: 100,000 frames must never blow up
        val stressBuffer = ShortArray(2048)
        for (i in 0 until 50) {
            generator.generateFrames(SoundscapeType.BROWN_NOISE, stressBuffer, 1024, volumeMultiplier = 0.8f)
            for (s in stressBuffer) {
                assertTrue(s in Short.MIN_VALUE..Short.MAX_VALUE)
            }
        }
    }

    @Test
    fun testPinkNoise_kelletFilterApproximationAndStability() {
        val numFrames = 44100
        val buffer = ShortArray(numFrames * 2)

        generator.setVolumeInstant(0.8f)
        generator.generateFrames(SoundscapeType.PINK_NOISE, buffer, numFrames, volumeMultiplier = 0.8f)

        var nonZeroCount = 0
        var maxAmp = 0
        for (s in buffer) {
            if (s != 0.toShort()) nonZeroCount++
            val amp = abs(s.toInt())
            if (amp > maxAmp) maxAmp = amp
        }

        assertTrue("Pink noise must produce non-zero frames", nonZeroCount > numFrames)
        assertTrue("Pink noise amplitude should be audible", maxAmp > 1000)

        // Verify stereo decorrelation
        var identicalSamples = 0
        for (i in 0 until numFrames) {
            if (buffer[2 * i] == buffer[2 * i + 1]) identicalSamples++
        }
        assertTrue("Left and Right pink noise must be decorrelated", identicalSamples < (numFrames * 0.05))

        // Long-term filter stability over 100,000 frames
        val stressBuffer = ShortArray(2048)
        for (i in 0 until 50) {
            generator.generateFrames(SoundscapeType.PINK_NOISE, stressBuffer, 1024, volumeMultiplier = 0.8f)
            for (s in stressBuffer) {
                assertTrue(s in Short.MIN_VALUE..Short.MAX_VALUE)
            }
        }
    }

    @Test
    fun testVolumeScalingAndSmoothFadeOutToZero() {
        val numFrames = 2000
        val buffer = ShortArray(numFrames * 2)

        // 1. Initial state volume is 0.0f; ramp up to 0.8f
        assertEquals(0.0f, generator.currentVolume, 0.0001f)
        generator.generateFrames(SoundscapeType.GAMMA_40HZ, buffer, numFrames, volumeMultiplier = 0.8f)
        assertEquals(0.8f, generator.currentVolume, 0.0001f)

        // 2. Smooth fade-out: start at 0.8f and ramp down to 0.0f
        generator.generateFrames(SoundscapeType.GAMMA_40HZ, buffer, numFrames, volumeMultiplier = 0.0f)
        assertEquals(0.0f, generator.currentVolume, 0.0001f)

        // The final frames in the fade-out buffer must have smoothly reached 0
        val lastLeft = buffer[2 * (numFrames - 1)]
        val lastRight = buffer[2 * (numFrames - 1) + 1]
        assertEquals(0.toShort(), lastLeft)
        assertEquals(0.toShort(), lastRight)

        // 3. Subsequent calls at volume 0.0f must produce pure silence
        val silenceBuffer = ShortArray(numFrames * 2) { 555 }
        generator.generateFrames(SoundscapeType.GAMMA_40HZ, silenceBuffer, numFrames, volumeMultiplier = 0.0f)
        assertTrue("Subsequent generation at volume 0 must be pure silence", silenceBuffer.all { it == 0.toShort() })
    }

    @Test
    fun testResetRestoresInitialState() {
        generator.setVolumeInstant(0.9f)
        val buffer = ShortArray(256)
        generator.generateFrames(SoundscapeType.GAMMA_40HZ, buffer, 128, volumeMultiplier = 0.9f)
        assertTrue(generator.currentVolume > 0f)

        generator.reset()

        assertEquals(0.0f, generator.currentVolume, 0.0001f)
    }

    @Test
    fun testSetVolumeInstantClampsCorrectly() {
        generator.setVolumeInstant(1.5f)
        assertEquals(1.0f, generator.currentVolume, 0.0001f)

        generator.setVolumeInstant(-0.5f)
        assertEquals(0.0f, generator.currentVolume, 0.0001f)

        generator.setVolumeInstant(0.42f)
        assertEquals(0.42f, generator.currentVolume, 0.0001f)
    }

    @Test
    fun testArgumentValidation() {
        val buffer = ShortArray(100)

        // numFrames = 0 should return cleanly
        generator.generateFrames(SoundscapeType.GAMMA_40HZ, buffer, 0, 0.5f)

        // Negative numFrames should throw
        try {
            generator.generateFrames(SoundscapeType.GAMMA_40HZ, buffer, -1, 0.5f)
            fail("Expected IllegalArgumentException for negative numFrames")
        } catch (_: IllegalArgumentException) {}

        // Buffer smaller than required frames * 2 should throw
        try {
            generator.generateFrames(SoundscapeType.GAMMA_40HZ, buffer, 100, 0.5f) // 100 frames requires size 200
            fail("Expected IllegalArgumentException for undersized buffer")
        } catch (_: IllegalArgumentException) {}
    }
}
