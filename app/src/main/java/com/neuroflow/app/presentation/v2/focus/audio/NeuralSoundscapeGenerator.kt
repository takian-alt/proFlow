package com.neuroflow.app.presentation.v2.focus.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Pure Kotlin DSP Audio Generator for Neural Soundscapes.
 *
 * Implements 3 neuroscience-backed audio streams:
 * 1. 40 Hz Gamma Binaural Beats (200 Hz carrier Left, 240 Hz Right) for prefrontal cortex synchrony.
 * 2. Deep Brown Noise (1/f² stochastic acoustic masking using stereo leaky random walk).
 * 3. Flow Pink Noise (Paul Kellet 1/f filter approximation for calming spectral density).
 *
 * Frame-interpolated smooth volume attenuation and fade-out prevent audio pops and clicks.
 * Pure JVM Kotlin code without Android framework dependencies (100% unit-testable).
 */
class NeuralSoundscapeGenerator(
    val sampleRate: Int = 44100
) {
    companion object {
        const val CARRIER_HZ = 200.0
        const val GAMMA_BEAT_HZ = 40.0
        const val BETA_BEAT_HZ = 16.0
        const val LEFT_FREQ = CARRIER_HZ
        const val RIGHT_FREQ = CARRIER_HZ + GAMMA_BEAT_HZ // 240.0 Hz

        // Theta-Gamma PAC constants for THETA_GAMMA_ENCODE profile
        // 6 Hz theta envelope: hippocampal timing scaffold for sequential indexing
        const val THETA_FREQ_HZ = 6.0
        // 40 Hz gamma envelope: cortical item-binding within each theta window
        const val GAMMA_ENV_FREQ_HZ = 40.0
        // 360 Hz carrier: above vocal band, Δf/f = 40/360 ≈ 11% for clean isochronic perception
        const val GAMMA_CARRIER_HZ = 360.0
        // 0.08 Hz anti-habituation LFO: 12.5s breathing cycle prevents auditory cortex adaptation
        const val LFO_FREQ_HZ = 0.08
        // LFO depth centre and swing: oscillates PAC gain between 0.14 and 0.22
        const val LFO_DEPTH_CENTER = 0.18
        const val LFO_DEPTH_SWING = 0.04
        // Pink noise dominance ratio (80%) to protect phonological loop
        const val PAC_NOISE_RATIO = 0.80f
    }

    // Phase accumulators for binaural carriers
    private var phaseLeft = 0.0
    private var phaseRight = 0.0

    // Phase accumulators for monaural beats
    private var monauralPhase1 = 0.0
    private var monauralPhase2 = 0.0

    // Phase accumulators for Theta-Gamma PAC (THETA_GAMMA_ENCODE profile)
    // All Double-precision, wrapped modulo 2π to prevent mantissa precision loss over long sessions
    private var phaseThetaEnv = 0.0   // 6 Hz theta envelope
    private var phaseGammaEnv = 0.0   // 40 Hz gamma envelope
    private var phaseGammaCarrier = 0.0 // 360 Hz gamma carrier
    private var phaseLfo = 0.0        // 0.08 Hz anti-habituation LFO

    // Brownian motion state (independent stereo leaky random walks)
    private var brownLeft = 0.0f
    private var brownRight = 0.0f

    // Kellet 1/f Pink noise filter state (independent stereo poles)
    private var b0L = 0f; private var b1L = 0f; private var b2L = 0f; private var b3L = 0f; private var b4L = 0f; private var b5L = 0f; private var b6L = 0f
    private var b0R = 0f; private var b1R = 0f; private var b2R = 0f; private var b3R = 0f; private var b4R = 0f; private var b5R = 0f; private var b6R = 0f

    // Volume tracking for smooth anti-pop frame interpolation
    var currentVolume: Float = 0.0f
        private set

    /**
     * Resets generator internal filter states, phases, and volume state.
     */
    fun reset() {
        phaseLeft = 0.0
        phaseRight = 0.0
        monauralPhase1 = 0.0
        monauralPhase2 = 0.0
        phaseThetaEnv = 0.0
        phaseGammaEnv = 0.0
        phaseGammaCarrier = 0.0
        phaseLfo = 0.0
        brownLeft = 0.0f
        brownRight = 0.0f
        b0L = 0f; b1L = 0f; b2L = 0f; b3L = 0f; b4L = 0f; b5L = 0f; b6L = 0f
        b0R = 0f; b1R = 0f; b2R = 0f; b3R = 0f; b4R = 0f; b5R = 0f; b6R = 0f
        currentVolume = 0.0f
    }

    /**
     * Sets volume immediately without interpolation (useful for test setup).
     */
    fun setVolumeInstant(volume: Float) {
        currentVolume = if (volume.isNaN()) 0f else volume.coerceIn(0f, 1f)
    }

    /**
     * Generates stereo 16-bit PCM frames into [outputBuffer] (interleaved L, R).
     * [outputBuffer] size must be at least [numFrames] * 2.
     * [volumeMultiplier] targets a range of 0.0f .. 1.0f.
     * Linearly interpolates volume across [numFrames] to eliminate audio pops and clicks.
     */
    fun generateFrames(
        type: SoundscapeType,
        outputBuffer: ShortArray,
        numFrames: Int,
        volumeMultiplier: Float = 0.7f
    ) {
        require(numFrames >= 0) { "numFrames cannot be negative: $numFrames" }
        require(outputBuffer.size >= numFrames * 2) {
            "outputBuffer size (${outputBuffer.size}) is smaller than required frames (${numFrames * 2})"
        }

        if (numFrames == 0) return

        if (type == SoundscapeType.NONE) {
            outputBuffer.fill(0, 0, numFrames * 2)
            currentVolume = 0.0f
            return
        }

        val safeVolume = if (volumeMultiplier.isNaN()) 0f else volumeMultiplier
        val targetVol = safeVolume.coerceIn(0f, 1f)
        val startVol = if (currentVolume.isNaN()) 0f else currentVolume

        // If target is 0 and current is 0, silence
        if (targetVol <= 0.00001f && startVol <= 0.00001f) {
            outputBuffer.fill(0, 0, numFrames * 2)
            currentVolume = 0.0f
            return
        }

        val stepLeft = 2.0 * PI * LEFT_FREQ / sampleRate
        val stepRight = 2.0 * PI * RIGHT_FREQ / sampleRate
        val stepMonauralCarrier = 2.0 * PI * CARRIER_HZ / sampleRate
        val stepMonauralGammaOffset = 2.0 * PI * (CARRIER_HZ + GAMMA_BEAT_HZ) / sampleRate
        val stepMonauralBetaOffset = 2.0 * PI * (CARRIER_HZ + BETA_BEAT_HZ) / sampleRate
        val volDelta = targetVol - startVol

        for (i in 0 until numFrames) {
            // Frame-interpolated volume attenuation / ramp
            val fraction = (i + 1).toFloat() / numFrames
            val vol = (startVol + volDelta * fraction).coerceIn(0f, 1f)

            val leftSampleFloat: Float
            val rightSampleFloat: Float

            when (type) {
                SoundscapeType.GAMMA_40HZ -> {
                    leftSampleFloat = sin(phaseLeft).toFloat()
                    rightSampleFloat = sin(phaseRight).toFloat()
                    phaseLeft += stepLeft
                    phaseRight += stepRight
                    if (phaseLeft >= 2.0 * PI) phaseLeft -= 2.0 * PI
                    if (phaseRight >= 2.0 * PI) phaseRight -= 2.0 * PI
                }
                SoundscapeType.MONAURAL_GAMMA_40HZ,
                SoundscapeType.BETA_16HZ -> {
                    val stepOffset = if (type == SoundscapeType.MONAURAL_GAMMA_40HZ) {
                        stepMonauralGammaOffset
                    } else {
                        stepMonauralBetaOffset
                    }
                    monauralPhase1 += stepMonauralCarrier
                    if (monauralPhase1 >= 2.0 * PI) monauralPhase1 -= 2.0 * PI
                    monauralPhase2 += stepOffset
                    if (monauralPhase2 >= 2.0 * PI) monauralPhase2 -= 2.0 * PI

                    val monauralBeat = 0.5f * (sin(monauralPhase1).toFloat() + sin(monauralPhase2).toFloat())

                    // Synthesize pink noise bed sample (Paul Kellet 1/f filter)
                    val w = Random.nextFloat() * 2f - 1f
                    b0L = 0.99886f * b0L + w * 0.0555179f
                    b1L = 0.99332f * b1L + w * 0.0750759f
                    b2L = 0.96900f * b2L + w * 0.1538520f
                    b3L = 0.86650f * b3L + w * 0.3104856f
                    b4L = 0.55000f * b4L + w * 0.5329522f
                    b5L = -0.7616f * b5L - w * 0.0168980f
                    val pinkNoiseSample = ((b0L + b1L + b2L + b3L + b4L + b5L + b6L + w * 0.5362f) * 0.11f).coerceIn(-1f, 1f)
                    b6L = w * 0.115926f

                    // 75% prominent monaural tone + 25% subtle pink noise cushion
                    val mixedSample = (monauralBeat * 0.75f + pinkNoiseSample * 0.25f).coerceIn(-1.0f, 1.0f)
                    leftSampleFloat = mixedSample
                    rightSampleFloat = mixedSample
                }
                SoundscapeType.BROWN_NOISE -> {
                    val whiteL = Random.nextFloat() * 2f - 1f
                    val whiteR = Random.nextFloat() * 2f - 1f
                    brownLeft = (brownLeft + (0.02f * whiteL)) / 1.02f
                    brownRight = (brownRight + (0.02f * whiteR)) / 1.02f
                    leftSampleFloat = (brownLeft * 3.2f).coerceIn(-1f, 1f)
                    rightSampleFloat = (brownRight * 3.2f).coerceIn(-1f, 1f)
                }
                SoundscapeType.PINK_NOISE -> {
                    val wL = Random.nextFloat() * 2f - 1f
                    val wR = Random.nextFloat() * 2f - 1f
                    b0L = 0.99886f * b0L + wL * 0.0555179f
                    b1L = 0.99332f * b1L + wL * 0.0750759f
                    b2L = 0.96900f * b2L + wL * 0.1538520f
                    b3L = 0.86650f * b3L + wL * 0.3104856f
                    b4L = 0.55000f * b4L + wL * 0.5329522f
                    b5L = -0.7616f * b5L - wL * 0.0168980f
                    leftSampleFloat = ((b0L + b1L + b2L + b3L + b4L + b5L + b6L + wL * 0.5362f) * 0.11f).coerceIn(-1f, 1f)
                    b6L = wL * 0.115926f

                    b0R = 0.99886f * b0R + wR * 0.0555179f
                    b1R = 0.99332f * b1R + wR * 0.0750759f
                    b2R = 0.96900f * b2R + wR * 0.1538520f
                    b3R = 0.86650f * b3R + wR * 0.3104856f
                    b4R = 0.55000f * b4R + wR * 0.5329522f
                    b5R = -0.7616f * b5R - wR * 0.0168980f
                    rightSampleFloat = ((b0R + b1R + b2R + b3R + b4R + b5R + b6R + wR * 0.5362f) * 0.11f).coerceIn(-1f, 1f)
                    b6R = wR * 0.115926f
                }
                SoundscapeType.THETA_GAMMA_ENCODE -> {
                    // ── Theta-Gamma Phase-Amplitude Coupling (Monaural) ──
                    // Designed for fast memorization via theta-gamma nesting:
                    //   - 6 Hz theta envelope: hippocampal timing scaffold for sequential indexing
                    //   - 40 Hz gamma envelope: cortical item-binding within each theta window
                    //   - 360 Hz carrier: above vocal band for clean isochronic perception
                    //   - Anti-habituation LFO (0.08 Hz): prevents auditory cortex adaptation
                    //   - 80% pink noise bed: protects phonological loop during verbal encoding
                    // Adapted for single-sided deafness: identical mono signal on both channels.

                    // Phase-amplitude coupling: gamma bursts gated by theta peaks
                    // E_θ(t) = 0.5 * (1 + cos(phaseTheta))
                    val thetaEnv = 0.5 * (1.0 + cos(phaseThetaEnv))
                    // E_γ(t) = 0.5 * (1 + cos(phaseGamma))
                    val gammaEnv = 0.5 * (1.0 + cos(phaseGammaEnv))
                    // S_PAC(t) = E_θ * E_γ * sin(phaseCarrier)
                    val pacSignal = (thetaEnv * gammaEnv * sin(phaseGammaCarrier)).toFloat()

                    // Anti-habituation LFO: depth oscillates between 0.14 and 0.22
                    val lfoDepth = (LFO_DEPTH_CENTER + LFO_DEPTH_SWING * sin(phaseLfo)).toFloat()

                    // Pink noise bed (Paul Kellet 1/f filter, mono for single-sided deafness)
                    val w = Random.nextFloat() * 2f - 1f
                    b0L = 0.99886f * b0L + w * 0.0555179f
                    b1L = 0.99332f * b1L + w * 0.0750759f
                    b2L = 0.96900f * b2L + w * 0.1538520f
                    b3L = 0.86650f * b3L + w * 0.3104856f
                    b4L = 0.55000f * b4L + w * 0.5329522f
                    b5L = -0.7616f * b5L - w * 0.0168980f
                    val pinkNoise = ((b0L + b1L + b2L + b3L + b4L + b5L + b6L + w * 0.5362f) * 0.11f).coerceIn(-1f, 1f)
                    b6L = w * 0.115926f

                    // Final mix: 80% pink noise + LFO-modulated PAC signal
                    val monoSample = (PAC_NOISE_RATIO * pinkNoise + lfoDepth * pacSignal).coerceIn(-1f, 1f)
                    leftSampleFloat = monoSample
                    rightSampleFloat = monoSample

                    // Advance phase accumulators (Double precision, wrapped mod 2π)
                    phaseThetaEnv += 2.0 * PI * THETA_FREQ_HZ / sampleRate
                    if (phaseThetaEnv >= 2.0 * PI) phaseThetaEnv -= 2.0 * PI
                    phaseGammaEnv += 2.0 * PI * GAMMA_ENV_FREQ_HZ / sampleRate
                    if (phaseGammaEnv >= 2.0 * PI) phaseGammaEnv -= 2.0 * PI
                    phaseGammaCarrier += 2.0 * PI * GAMMA_CARRIER_HZ / sampleRate
                    if (phaseGammaCarrier >= 2.0 * PI) phaseGammaCarrier -= 2.0 * PI
                    phaseLfo += 2.0 * PI * LFO_FREQ_HZ / sampleRate
                    if (phaseLfo >= 2.0 * PI) phaseLfo -= 2.0 * PI
                }
                SoundscapeType.NONE -> {
                    leftSampleFloat = 0f
                    rightSampleFloat = 0f
                }
            }

            // Convert to 16-bit signed PCM
            val leftPcm = (leftSampleFloat * vol * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            val rightPcm = (rightSampleFloat * vol * 32767f).toInt().coerceIn(-32768, 32767).toShort()

            outputBuffer[2 * i] = leftPcm
            outputBuffer[2 * i + 1] = rightPcm
        }

        currentVolume = targetVol
    }

    /**
     * Synthesizes monaural beat frames with customizable carrier, beat frequency, and noise bed.
     * Delivers an identical acoustic interference envelope to Left and Right channels.
     * Section 6.44 Ratified Formula.
     */
    fun generateMonauralBeatFrames(
        beatFreqHz: Double,
        outputBuffer: ShortArray,
        numFrames: Int,
        volumeMultiplier: Float = 0.7f,
        carrierHz: Double = CARRIER_HZ,
        pinkNoiseRatio: Float = 0.25f
    ) {
        require(numFrames >= 0) { "numFrames cannot be negative: $numFrames" }
        require(outputBuffer.size >= numFrames * 2) {
            "outputBuffer size (${outputBuffer.size}) is smaller than required frames (${numFrames * 2})"
        }
        if (numFrames == 0) return

        val safeVolume = if (volumeMultiplier.isNaN()) 0f else volumeMultiplier
        val targetVol = safeVolume.coerceIn(0f, 1f)
        val startVol = if (currentVolume.isNaN()) 0f else currentVolume

        if (targetVol <= 0.00001f && startVol <= 0.00001f) {
            outputBuffer.fill(0, 0, numFrames * 2)
            currentVolume = 0.0f
            return
        }

        val stepCarrier = 2.0 * PI * carrierHz / sampleRate
        val stepOffset = 2.0 * PI * (carrierHz + beatFreqHz) / sampleRate
        val volDelta = targetVol - startVol
        val toneRatio = (1.0f - pinkNoiseRatio).coerceIn(0f, 1f)

        for (i in 0 until numFrames) {
            val fraction = (i + 1).toFloat() / numFrames
            val vol = (startVol + volDelta * fraction).coerceIn(0f, 1f)

            monauralPhase1 += stepCarrier
            if (monauralPhase1 >= 2.0 * PI) monauralPhase1 -= 2.0 * PI
            monauralPhase2 += stepOffset
            if (monauralPhase2 >= 2.0 * PI) monauralPhase2 -= 2.0 * PI

            val monauralBeat = 0.5f * (sin(monauralPhase1).toFloat() + sin(monauralPhase2).toFloat())

            val w = Random.nextFloat() * 2f - 1f
            b0L = 0.99886f * b0L + w * 0.0555179f
            b1L = 0.99332f * b1L + w * 0.0750759f
            b2L = 0.96900f * b2L + w * 0.1538520f
            b3L = 0.86650f * b3L + w * 0.3104856f
            b4L = 0.55000f * b4L + w * 0.5329522f
            b5L = -0.7616f * b5L - w * 0.0168980f
            val pinkNoise = ((b0L + b1L + b2L + b3L + b4L + b5L + b6L + w * 0.5362f) * 0.11f).coerceIn(-1f, 1f)
            b6L = w * 0.115926f

            val mixedSample = (monauralBeat * toneRatio + pinkNoise * pinkNoiseRatio).coerceIn(-1.0f, 1.0f)
            val pcm = (mixedSample * vol * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            outputBuffer[2 * i] = pcm
            outputBuffer[2 * i + 1] = pcm
        }
        currentVolume = targetVol
    }
}
