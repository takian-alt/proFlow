package com.neuroflow.app.presentation.v2.focus.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Streaming AudioTrack player for Neural Soundscapes.
 *
 * Encapsulates:
 * - Real-time PCM stereo streaming on a dedicated playback thread
 * - Audio focus management with [AudioManager.OnAudioFocusChangeListener]
 * - Smooth anti-pop volume adjustment and frame-interpolated fade-out
 * - Leak-proof release and lifecycle cleanup
 */
class NeuralSoundscapePlayer(
    private val context: Context? = null,
    val generator: NeuralSoundscapeGenerator = NeuralSoundscapeGenerator(SAMPLE_RATE)
) {
    companion object {
        private const val TAG = "NeuralSoundscapePlayer"
        const val SAMPLE_RATE = 44100
        const val CHUNK_FRAMES = 1024 // ~23.2ms latency per synthesis buffer
        const val FADE_DURATION_MS = 400L
        const val FADE_OUT_DURATION_MS = 400L
    }

    private val audioManager: AudioManager? by lazy {
        try {
            context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to get AudioManager: ${t.message}")
            null
        }
    }

    private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null

    @Volatile
    var currentType: SoundscapeType = SoundscapeType.NONE
        private set

    @Volatile
    var currentVolume: Float = 0.7f
        private set

    private val isPlaying = AtomicBoolean(false)
    private val isFadingOut = AtomicBoolean(false)
    private var wasPlayingBeforeFocusLoss = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                wasPlayingBeforeFocusLoss = false
                stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (isPlaying.get()) {
                    wasPlayingBeforeFocusLoss = true
                    pauseWithFadeOut()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (wasPlayingBeforeFocusLoss) {
                    wasPlayingBeforeFocusLoss = false
                    play(currentType)
                }
            }
        }
    }

    private var audioFocusRequest: Any? = null

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pauseWithFadeOut()
            }
        }
    }
    private var isNoisyReceiverRegistered = false

    private fun registerNoisyReceiver() {
        if (context != null && !isNoisyReceiverRegistered) {
            try {
                val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                context.registerReceiver(noisyReceiver, filter)
                isNoisyReceiverRegistered = true
            } catch (t: Throwable) {
                Log.w(TAG, "Could not register noisy receiver: ${t.message}")
            }
        }
    }

    private fun unregisterNoisyReceiver() {
        if (context != null && isNoisyReceiverRegistered) {
            try {
                context.unregisterReceiver(noisyReceiver)
            } catch (t: Throwable) {
                Log.w(TAG, "Could not unregister noisy receiver: ${t.message}")
            } finally {
                isNoisyReceiverRegistered = false
            }
        }
    }

    /**
     * Starts or switches active soundscape stream.
     */
    @Synchronized
    fun play(type: SoundscapeType) {
        if (type == SoundscapeType.NONE) {
            pauseWithFadeOut()
            currentType = SoundscapeType.NONE
            return
        }

        currentType = type
        isFadingOut.set(false)

        if (isPlaying.get()) {
            // Already streaming: generator seamlessly updates synthesis stream
            return
        }

        requestAudioFocus()
        registerNoisyReceiver()
        initAudioTrackIfNeeded()

        try {
            audioTrack?.play()
        } catch (t: Throwable) {
            Log.w(TAG, "AudioTrack play failed: ${t.message}")
            unregisterNoisyReceiver()
            return
        }

        isPlaying.set(true)
        startPlaybackThread()
    }

    /**
     * Updates target soundscape volume (0.0f .. 1.0f).
     */
    @Synchronized
    fun setVolume(volume: Float) {
        currentVolume = if (volume.isNaN()) 0f else volume.coerceIn(0f, 1f)
    }

    /**
     * Initiates smooth fade-out over [FADE_OUT_DURATION_MS] and then halts playback.
     */
    @Synchronized
    fun pauseWithFadeOut() {
        if (!isPlaying.get() || isFadingOut.get()) return
        isFadingOut.set(true)
    }

    /**
     * Stops playback immediately, flushes buffers, and abandons AudioFocus.
     */
    @Synchronized
    fun stop() {
        isPlaying.set(false)
        isFadingOut.set(false)
        stopPlaybackThread()
        unregisterNoisyReceiver()

        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (t: Throwable) {
            Log.w(TAG, "AudioTrack pause/flush failed: ${t.message}")
        }
        abandonAudioFocus()
    }

    /**
     * Disposes AudioTrack and releases all resources.
     */
    @Synchronized
    fun release() {
        stop()
        unregisterNoisyReceiver()
        try {
            audioTrack?.release()
        } catch (t: Throwable) {
            Log.w(TAG, "AudioTrack release failed: ${t.message}")
        } finally {
            audioTrack = null
        }
        generator.reset()
    }

    private fun initAudioTrackIfNeeded() {
        if (audioTrack != null) return

        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, CHUNK_FRAMES * 2 * 2 * 2)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (t: Throwable) {
            Log.w(TAG, "AudioTrack init failed: ${t.message}")
            audioTrack = null
        }
    }

    private fun startPlaybackThread() {
        stopPlaybackThread()
        playbackThread = thread(name = "NeuralSoundscapeAudio", isDaemon = true) {
            val buffer = ShortArray(CHUNK_FRAMES * 2)
            var fadeStartVol = currentVolume
            var fadeStartTime = 0L
            val fadeInStartTime = System.currentTimeMillis()

            try {
                while (isPlaying.get() && !Thread.currentThread().isInterrupted) {
                    val track = audioTrack
                    if (track == null || track.state != AudioTrack.STATE_INITIALIZED) {
                        break
                    }

                    val type = currentType
                    if (type == SoundscapeType.NONE) {
                        break
                    }

                    val vol: Float
                    if (isFadingOut.get()) {
                        if (fadeStartTime == 0L) {
                            fadeStartTime = System.currentTimeMillis()
                            fadeStartVol = generator.currentVolume
                        }
                        val elapsed = System.currentTimeMillis() - fadeStartTime
                        val fraction = (elapsed.toFloat() / FADE_OUT_DURATION_MS).coerceIn(0f, 1f)
                        // Click-free raised cosine fade-out envelope
                        val cosineFadeFactor = (0.5 * (1.0 + kotlin.math.cos(kotlin.math.PI * fraction))).toFloat()
                        vol = (fadeStartVol * cosineFadeFactor).coerceAtLeast(0f)

                        generator.generateFrames(type, buffer, CHUNK_FRAMES, vol)

                        if (fraction >= 1f || vol <= 0.0001f) {
                            // Fade completed cleanly
                            try {
                                track.write(buffer, 0, buffer.size)
                                track.pause()
                                track.flush()
                            } catch (_: Throwable) {}
                            isPlaying.set(false)
                            isFadingOut.set(false)
                            abandonAudioFocus()
                            break
                        }
                    } else {
                        fadeStartTime = 0L
                        val baseVol = currentVolume
                        val elapsedIn = System.currentTimeMillis() - fadeInStartTime
                        val inFraction = (elapsedIn.toFloat() / FADE_DURATION_MS).coerceIn(0f, 1f)
                        // Click-free raised cosine fade-in envelope
                        val cosineFadeIn = if (inFraction < 1.0f) {
                            (0.5 * (1.0 - kotlin.math.cos(kotlin.math.PI * inFraction))).toFloat()
                        } else {
                            1.0f
                        }
                        vol = (baseVol * cosineFadeIn).coerceIn(0f, 1f)
                        generator.generateFrames(type, buffer, CHUNK_FRAMES, vol)
                    }

                    val written = track.write(buffer, 0, buffer.size)
                    if (written < 0) {
                        Log.w(TAG, "AudioTrack write returned: $written")
                        break
                    }
                }
            } finally {
                isPlaying.set(false)
                isFadingOut.set(false)
                abandonAudioFocus()
                try {
                    audioTrack?.stop()
                } catch (_: Throwable) {}
            }
        }
    }

    private fun stopPlaybackThread() {
        val thread = playbackThread
        playbackThread = null
        if (thread != null && thread.isAlive) {
            thread.interrupt()
            try {
                thread.join(200)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return true
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val request = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                audioFocusRequest = request
                am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Could not request audio focus: ${t.message}")
            true
        }
    }

    private fun abandonAudioFocus() {
        unregisterNoisyReceiver()
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (audioFocusRequest as? android.media.AudioFocusRequest)?.let {
                    am.abandonAudioFocusRequest(it)
                }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Could not abandon audio focus: ${t.message}")
        }
    }
}
