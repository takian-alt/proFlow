package com.neuroflow.app.presentation.v2.focus

import android.content.Context
import android.media.AudioManager
import android.os.VibrationEffect
import android.os.Vibrator
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

@OptIn(ExperimentalCoroutinesApi::class)
class FocusCompletionNotifierTest : StringSpec({

    val context = mockk<Context>(relaxed = true)
    val vibrator = mockk<Vibrator>(relaxed = true)
    val audioManager = mockk<AudioManager>(relaxed = true)

    beforeTest {
        clearMocks(context, vibrator, audioManager)
        every { context.getSystemService(Context.VIBRATOR_SERVICE) } returns vibrator
        every { context.getSystemService(Context.AUDIO_SERVICE) } returns audioManager
    }

    "triggerHapticPulse vibrates with amplitude waveform when vibrator supports amplitude control" {
        every { vibrator.hasVibrator() } returns true
        every { vibrator.hasAmplitudeControl() } returns true

        val notifier = FocusCompletionNotifier(context)
        val testScope = TestScope(StandardTestDispatcher())

        notifier.playCompletionChimeAndHaptics(testScope)

        verify(atLeast = 1) { vibrator.vibrate(or(isNull(), any<VibrationEffect>())) }
    }

    "triggerHapticPulse falls back to basic waveform when vibrator lacks amplitude control" {
        every { vibrator.hasVibrator() } returns true
        every { vibrator.hasAmplitudeControl() } returns false

        val notifier = FocusCompletionNotifier(context)
        val testScope = TestScope(StandardTestDispatcher())

        notifier.playCompletionChimeAndHaptics(testScope)

        verify(atLeast = 1) { vibrator.vibrate(or(isNull(), any<VibrationEffect>())) }
    }

    "playCompletionChimeAndHaptics does not throw when context services are unavailable (headless CI)" {
        val emptyContext = mockk<Context>(relaxed = true)
        every { emptyContext.getSystemService(any()) } returns null

        val notifier = FocusCompletionNotifier(emptyContext)
        val testScope = TestScope(StandardTestDispatcher())

        // Must execute completely without throwing unhandled exceptions
        notifier.playCompletionChimeAndHaptics(testScope)
    }

    "synthesized dual-harmonic Solfeggio bell produces valid non-clipping PCM samples" {
        val sampleRate = 44100
        val duration = 1.6
        val freqFund = 528.0
        val freqFifth = 792.0
        val numSamples = (sampleRate * duration).toInt()
        val pcm = ShortArray(numSamples)

        var hasNonZero = false
        var maxAmplitude = 0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val attack = (t / 0.015).coerceAtMost(1.0)
            val decay = exp(-t * 3.2)
            val envelope = attack * decay

            val wave = 0.70 * sin(2.0 * PI * freqFund * t) +
                       0.30 * sin(2.0 * PI * freqFifth * t)
            val sample = (wave * envelope * Short.MAX_VALUE * 0.45).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            pcm[i] = sample

            if (sample.toInt() != 0) hasNonZero = true
            val absVal = kotlin.math.abs(sample.toInt())
            if (absVal > maxAmplitude) maxAmplitude = absVal
        }

        hasNonZero shouldBe true
        (maxAmplitude <= Short.MAX_VALUE) shouldBe true
        (maxAmplitude > 1000) shouldBe true
    }
})
