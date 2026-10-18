package com.neuroflow.app.presentation.v2.focus

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioFocusRequest
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

@Singleton
class FocusCompletionNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SAMPLE_RATE = 44100
        private const val DURATION_SECONDS = 1.6
        private const val FREQ_FUNDAMENTAL = 528.0 // Solfeggio C5 warm tone
        private const val FREQ_FIFTH = 792.0       // Perfect fifth G5 harmonic
    }

    fun playCompletionChimeAndHaptics(scope: CoroutineScope) {
        triggerHapticPulse()
        scope.launch(Dispatchers.Default) {
            playSynthesizedBell()
        }
    }

    private fun triggerHapticPulse() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return
            val timings = longArrayOf(0L, 120L, 100L, 220L)
            val effect = if (vibrator.hasAmplitudeControl()) {
                val amplitudes = intArrayOf(0, 160, 0, 220)
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            } else {
                VibrationEffect.createWaveform(timings, -1)
            }
            vibrator.vibrate(effect)
        }
    }

    private suspend fun playSynthesizedBell() {
        val audioManager = runCatching { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }.getOrNull()
        var focusRequest: AudioFocusRequest? = null
        var track: AudioTrack? = null

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .build()
            focusRequest = req
            audioManager?.requestAudioFocus(req)

            val numSamples = (SAMPLE_RATE * DURATION_SECONDS).toInt()
            val pcm = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                // Fast 15ms attack, smooth exponential decay
                val attack = (t / 0.015).coerceAtMost(1.0)
                val decay = exp(-t * 3.2)
                val envelope = attack * decay

                val wave = 0.70 * sin(2.0 * PI * FREQ_FUNDAMENTAL * t) +
                           0.30 * sin(2.0 * PI * FREQ_FIFTH * t)
                pcm[i] = (wave * envelope * Short.MAX_VALUE * 0.45).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcm, 0, pcm.size)
            track.play()
            delay((DURATION_SECONDS * 1000).toLong() + 100L)
        } catch (_: Throwable) {
            // Guard against audio hardware unavailability on headless CI or restricted audio HALs
        } finally {
            runCatching {
                track?.stop()
                track?.release()
            }
            focusRequest?.let { req ->
                runCatching { audioManager?.abandonAudioFocusRequest(req) }
            }
        }
    }
}
