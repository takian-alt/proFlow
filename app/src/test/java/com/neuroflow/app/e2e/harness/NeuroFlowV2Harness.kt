package com.neuroflow.app.e2e.harness

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.domain.inference.InferredTaskMetadata
import com.neuroflow.app.domain.inference.TaskInferenceEngine
import com.neuroflow.app.domain.model.TaskStatus
import java.util.Random
import java.util.UUID
import kotlin.math.*

/**
 * Authoritative E2E Test Harness providing reference oracles, DSP audio models,
 * wall-clock timing engines, intent dispatch routers, and validation models for
 * neuroFlow V2 Parity & Neuro-Capabilities (Milestones R1-R5).
 */
object NeuroFlowV2Harness {

    // =========================================================================
    // 1. V2 Navigation Routes Oracle
    // =========================================================================
    object NavigationOracle {
        const val ROUTE_FLOW = "flow"
        const val ROUTE_PLAN = "plan"
        const val ROUTE_INSIGHTS = "insights"
        const val ROUTE_SETTINGS = "settings"
        const val ROUTE_FOCUS_PATTERN = "focus_v2/{taskId}"
        const val ROUTE_NOTES = "notes"
        const val ROUTE_WEEKLY_SCHEDULE = "weekly_schedule"
        const val ROUTE_ANALYTICS = "analytics"
        const val ROUTE_HISTORY = "history"
        const val ROUTE_PRIORITY_WEIGHTS = "priority_weights"
        const val ROUTE_ACCOUNT_SYNC = "account_sync"
        const val ROUTE_HYPERFOCUS_ALLOWLIST = "hyperfocus_allowlist"
        const val ROUTE_PRIVACY_PERMISSIONS = "privacy_permissions"
        const val ROUTE_APP_GUIDE = "app_guide"
        const val ROUTE_IDENTITY = "identity"
        const val ROUTE_LOG_TIME = "log_time"

        val ALL_PARITY_ROUTES = listOf(
            ROUTE_FLOW,
            ROUTE_PLAN,
            ROUTE_INSIGHTS,
            ROUTE_SETTINGS,
            ROUTE_NOTES,
            ROUTE_WEEKLY_SCHEDULE,
            ROUTE_ANALYTICS,
            ROUTE_HISTORY,
            ROUTE_PRIORITY_WEIGHTS,
            ROUTE_ACCOUNT_SYNC,
            ROUTE_HYPERFOCUS_ALLOWLIST,
            ROUTE_PRIVACY_PERMISSIONS,
            ROUTE_APP_GUIDE,
            ROUTE_IDENTITY,
            ROUTE_LOG_TIME
        )

        fun isValidRoute(route: String): Boolean {
            if (route.startsWith("focus_v2/")) return true
            return route in ALL_PARITY_ROUTES
        }

        fun focusRouteFor(taskId: String): String = "focus_v2/$taskId"

        fun getBackDestination(route: String): String {
            return when (route) {
                ROUTE_NOTES -> ROUTE_FLOW
                ROUTE_WEEKLY_SCHEDULE -> ROUTE_PLAN
                ROUTE_ANALYTICS, ROUTE_HISTORY, ROUTE_IDENTITY -> ROUTE_INSIGHTS
                ROUTE_PRIORITY_WEIGHTS, ROUTE_ACCOUNT_SYNC, ROUTE_HYPERFOCUS_ALLOWLIST,
                ROUTE_PRIVACY_PERMISSIONS, ROUTE_APP_GUIDE -> ROUTE_SETTINGS
                ROUTE_LOG_TIME -> ROUTE_FLOW
                else -> ROUTE_FLOW
            }
        }
    }

    // =========================================================================
    // 2. Neural Soundscape DSP Synthesis Oracle
    // =========================================================================
    enum class SoundscapeType(val displayName: String, val subtitle: String) {
        NONE("Off", "Silence"),
        GAMMA_40HZ("40 Hz Gamma", "Binaural Prefrontal Synchrony"),
        BROWN_NOISE("Deep Brown Noise", "1/f² Stochastic Acoustic Masking"),
        PINK_NOISE("Flow Pink Noise / Rain", "1/f Calming Spectral Density")
    }

    class SoundscapeDspGenerator(val sampleRate: Int = 44100) {
        companion object {
            const val GAMMA_LEFT_FREQ = 200.0   // 200 Hz carrier (Left)
            const val GAMMA_RIGHT_FREQ = 240.0  // 240 Hz carrier (Right) -> 40 Hz binaural beat
            const val PEAK_AMPLITUDE = 28000.0  // 16-bit safe ceiling (avoids clipping)
        }

        private var gammaPhaseLeft = 0.0
        private var gammaPhaseRight = 0.0

        // Brown noise leaky integrator state
        private var brownStateLeft = 0.0
        private var brownStateRight = 0.0

        // Pink noise Paul Kellet 1/f IIR filter states (b0..b6)
        private var p0L = 0.0; private var p1L = 0.0; private var p2L = 0.0; private var p3L = 0.0; private var p4L = 0.0; private var p5L = 0.0
        private var p0R = 0.0; private var p1R = 0.0; private var p2R = 0.0; private var p3R = 0.0; private var p4R = 0.0; private var p5R = 0.0

        private val random = Random(42L)

        fun reset() {
            gammaPhaseLeft = 0.0
            gammaPhaseRight = 0.0
            brownStateLeft = 0.0
            brownStateRight = 0.0
            p0L = 0.0; p1L = 0.0; p2L = 0.0; p3L = 0.0; p4L = 0.0; p5L = 0.0
            p0R = 0.0; p1R = 0.0; p2R = 0.0; p3R = 0.0; p4R = 0.0; p5R = 0.0
        }

        fun generateFrames(
            type: SoundscapeType,
            outputBuffer: ShortArray,
            numFrames: Int,
            volumeMultiplier: Float = 0.7f
        ) {
            val safeVolume = volumeMultiplier.coerceIn(0.0f, 1.0f)
            val channels = 2 // Stereo (L, R)
            val requiredSize = numFrames * channels
            require(outputBuffer.size >= requiredSize) {
                "Buffer size ${outputBuffer.size} is insufficient for $numFrames stereo frames"
            }

            if (type == SoundscapeType.NONE || safeVolume == 0.0f) {
                outputBuffer.fill(0, 0, requiredSize)
                return
            }

            val phaseIncLeft = 2.0 * Math.PI * GAMMA_LEFT_FREQ / sampleRate
            val phaseIncRight = 2.0 * Math.PI * GAMMA_RIGHT_FREQ / sampleRate
            val twoPi = 2.0 * Math.PI

            var idx = 0
            for (f in 0 until numFrames) {
                var sampleLeft: Double
                var sampleRight: Double

                when (type) {
                    SoundscapeType.GAMMA_40HZ -> {
                        sampleLeft = sin(gammaPhaseLeft) * PEAK_AMPLITUDE
                        sampleRight = sin(gammaPhaseRight) * PEAK_AMPLITUDE
                        gammaPhaseLeft = (gammaPhaseLeft + phaseIncLeft) % twoPi
                        gammaPhaseRight = (gammaPhaseRight + phaseIncRight) % twoPi
                    }
                    SoundscapeType.BROWN_NOISE -> {
                        // Leaky random walk: 1/f² stochastic resonance
                        val whiteL = (random.nextDouble() * 2.0 - 1.0)
                        val whiteR = (random.nextDouble() * 2.0 - 1.0)
                        brownStateLeft = (brownStateLeft + (0.025 * whiteL)) / 1.025
                        brownStateRight = (brownStateRight + (0.025 * whiteR)) / 1.025
                        sampleLeft = brownStateLeft * PEAK_AMPLITUDE * 3.5
                        sampleRight = brownStateRight * PEAK_AMPLITUDE * 3.5
                    }
                    SoundscapeType.PINK_NOISE -> {
                        // Paul Kellet's filter approximation for 1/f spectral density
                        val whiteL = (random.nextDouble() * 2.0 - 1.0)
                        val whiteR = (random.nextDouble() * 2.0 - 1.0)

                        p0L = 0.99886 * p0L + whiteL * 0.0555179
                        p1L = 0.99332 * p1L + whiteL * 0.0750759
                        p2L = 0.96900 * p2L + whiteL * 0.1538520
                        p3L = 0.86650 * p3L + whiteL * 0.3104856
                        p4L = 0.55000 * p4L + whiteL * 0.5329522
                        p5L = -0.7616 * p5L - whiteL * 0.0168980
                        val pinkL = p0L + p1L + p2L + p3L + p4L + p5L + (whiteL * 0.5362)
                        sampleLeft = pinkL * (PEAK_AMPLITUDE * 0.18)

                        p0R = 0.99886 * p0R + whiteR * 0.0555179
                        p1R = 0.99332 * p1R + whiteR * 0.0750759
                        p2R = 0.96900 * p2R + whiteR * 0.1538520
                        p3R = 0.86650 * p3R + whiteR * 0.3104856
                        p4R = 0.55000 * p4R + whiteR * 0.5329522
                        p5R = -0.7616 * p5R - whiteR * 0.0168980
                        val pinkR = p0R + p1R + p2R + p3R + p4R + p5R + (whiteR * 0.5362)
                        sampleRight = pinkR * (PEAK_AMPLITUDE * 0.18)
                    }
                    SoundscapeType.NONE -> {
                        sampleLeft = 0.0
                        sampleRight = 0.0
                    }
                }

                val scaledL = (sampleLeft * safeVolume).roundToInt().coerceIn(-32768, 32767)
                val scaledR = (sampleRight * safeVolume).roundToInt().coerceIn(-32768, 32767)

                outputBuffer[idx++] = scaledL.toShort()
                outputBuffer[idx++] = scaledR.toShort()
            }
        }

        fun applyFrameInterpolatedFadeOut(
            buffer: ShortArray,
            numFrames: Int,
            startVolume: Float,
            targetVolume: Float = 0.0f
        ) {
            val channels = 2
            val totalSamples = numFrames * channels
            if (numFrames <= 0 || totalSamples > buffer.size) return

            for (f in 0 until numFrames) {
                val progress = if (numFrames > 1) f.toFloat() / (numFrames - 1).toFloat() else 1.0f
                val currentVol = startVolume + progress * (targetVolume - startVolume)
                val scale = if (startVolume > 0f) currentVol / startVolume else 0f
                val idx = f * channels
                buffer[idx] = (buffer[idx] * scale).roundToInt().coerceIn(-32768, 32767).toShort()
                buffer[idx + 1] = (buffer[idx + 1] * scale).roundToInt().coerceIn(-32768, 32767).toShort()
            }
        }

        fun computeZeroCrossingRate(samples: ShortArray): Float {
            if (samples.size < 4) return 0f
            val numFrames = samples.size / 2
            var crossings = 0
            for (i in 1 until numFrames) {
                val prev = samples[(i - 1) * 2]
                val curr = samples[i * 2]
                if ((prev < 0 && curr >= 0) || (prev >= 0 && curr < 0)) {
                    crossings++
                }
            }
            return crossings.toFloat() / (numFrames - 1)
        }
    }

    // =========================================================================
    // 3. Drift-Proof Wall-Clock Timing Oracle
    // =========================================================================
    data class CalculatedTiming(
        val elapsedSeconds: Long,
        val remainingSeconds: Long,
        val isExpired: Boolean
    )

    object FocusTimingOracle {
        fun calculate(
            startedAtMillis: Long,
            accumulatedPauseMillis: Long,
            pausedAtMillis: Long?,
            nowMillis: Long,
            targetDurationSeconds: Long
        ): CalculatedTiming {
            val safeNow = maxOf(nowMillis, startedAtMillis)
            val effectiveElapsedMillis = if (pausedAtMillis != null) {
                val safePause = maxOf(pausedAtMillis, startedAtMillis)
                maxOf(0L, safePause - startedAtMillis - accumulatedPauseMillis)
            } else {
                maxOf(0L, safeNow - startedAtMillis - accumulatedPauseMillis)
            }

            val elapsedSeconds = effectiveElapsedMillis / 1000L
            val isStopwatch = targetDurationSeconds <= 0L

            val remainingSeconds = if (isStopwatch) {
                0L
            } else {
                maxOf(0L, targetDurationSeconds - elapsedSeconds)
            }

            val isExpired = (!isStopwatch) && (elapsedSeconds >= targetDurationSeconds)

            return CalculatedTiming(
                elapsedSeconds = elapsedSeconds,
                remainingSeconds = remainingSeconds,
                isExpired = isExpired
            )
        }

        fun onPause(
            currentPausedAt: Long?,
            nowMillis: Long
        ): Long {
            return currentPausedAt ?: nowMillis
        }

        fun onResume(
            pausedAtMillis: Long,
            accumulatedPauseMillis: Long,
            nowMillis: Long
        ): Pair<Long, Long?> {
            val pauseDuration = maxOf(0L, nowMillis - pausedAtMillis)
            val newAccumulated = accumulatedPauseMillis + pauseDuration
            return Pair(newAccumulated, null)
        }
    }

    // =========================================================================
    // 4. Widget Interop & Intent Routing Oracle
    // =========================================================================
    object WidgetInteropOracle {
        const val ACTION_OPEN_NEW_TASK = "com.procus.ACTION_OPEN_NEW_TASK"
        const val ACTION_OPEN_FOCUS = "com.procus.ACTION_OPEN_FOCUS"
        const val EXTRA_TASK_ID = "taskId"

        data class IntentResolution(
            val targetRoute: String,
            val openComposer: Boolean,
            val focusedTaskId: String?
        )

        fun resolveIntent(
            action: String?,
            taskIdExtra: String?,
            activeTasks: List<TaskEntity>
        ): IntentResolution {
            return when (action) {
                ACTION_OPEN_NEW_TASK -> {
                    IntentResolution(
                        targetRoute = NavigationOracle.ROUTE_FLOW,
                        openComposer = true,
                        focusedTaskId = null
                    )
                }
                ACTION_OPEN_FOCUS -> {
                    val resolvedTaskId = if (!taskIdExtra.isNullOrBlank()) {
                        taskIdExtra
                    } else {
                        activeTasks.firstOrNull()?.id
                    }
                    if (resolvedTaskId != null) {
                        IntentResolution(
                            targetRoute = NavigationOracle.focusRouteFor(resolvedTaskId),
                            openComposer = false,
                            focusedTaskId = resolvedTaskId
                        )
                    } else {
                        // Empty active task list fallback -> land on Flow
                        IntentResolution(
                            targetRoute = NavigationOracle.ROUTE_FLOW,
                            openComposer = false,
                            focusedTaskId = null
                        )
                    }
                }
                else -> {
                    IntentResolution(
                        targetRoute = NavigationOracle.ROUTE_FLOW,
                        openComposer = false,
                        focusedTaskId = null
                    )
                }
            }
        }

        fun resolveV2UiDefault(persistedSetting: Boolean?): Boolean {
            // Specification: default must be true for all installs
            return persistedSetting ?: true
        }
    }

    // =========================================================================
    // 5. Voice Transcription & Decompression Zone Oracle
    // =========================================================================
    object VoiceAndDecompressionOracle {
        const val DECOMPRESSION_TOTAL_SECONDS = 180
        const val PHYSIOLOGICAL_SIGH_SECONDS = 60

        fun pipeSpeechTranscription(
            transcript: String,
            calibrationFactor: Float = 1.0f
        ): InferredTaskMetadata {
            return TaskInferenceEngine.inferFromInput(transcript, calibrationFactor)
        }

        data class DecompressionState(
            val remainingSeconds: Int,
            val isSighActive: Boolean,
            val sighRemainingSeconds: Int,
            val isCompleted: Boolean,
            val isBypassed: Boolean
        )

        class DecompressionEngine(
            val initialSeconds: Int = DECOMPRESSION_TOTAL_SECONDS
        ) {
            var state = DecompressionState(
                remainingSeconds = initialSeconds,
                isSighActive = false,
                sighRemainingSeconds = 0,
                isCompleted = false,
                isBypassed = false
            )
                private set

            fun tickSecond() {
                if (state.isCompleted || state.isBypassed) return

                val newRemaining = maxOf(0, state.remainingSeconds - 1)
                val newSighRemaining = if (state.isSighActive) maxOf(0, state.sighRemainingSeconds - 1) else 0
                val newSighActive = state.isSighActive && newSighRemaining > 0

                state = state.copy(
                    remainingSeconds = newRemaining,
                    isSighActive = newSighActive,
                    sighRemainingSeconds = newSighRemaining,
                    isCompleted = newRemaining == 0
                )
            }

            fun triggerPhysiologicalSigh() {
                if (state.isCompleted || state.isBypassed) return
                state = state.copy(
                    isSighActive = true,
                    sighRemainingSeconds = PHYSIOLOGICAL_SIGH_SECONDS
                )
            }

            fun readyNowBypass() {
                state = state.copy(
                    isBypassed = true,
                    isCompleted = true,
                    remainingSeconds = 0,
                    isSighActive = false
                )
            }
        }
    }

    // =========================================================================
    // 6. Parity Features Oracle (Velocity, WOOP, Retrospective Logging)
    // =========================================================================
    object ParityOracle {
        fun calculateVelocity(
            completedTasks: List<TaskEntity>,
            windowDays: Int,
            nowMillis: Long
        ): Int {
            val windowMillis = windowDays.toLong() * 86_400_000L
            val threshold = nowMillis - windowMillis
            return completedTasks.count { task ->
                task.status == TaskStatus.COMPLETED &&
                        task.completedAt != null &&
                        task.completedAt!! >= threshold
            }
        }

        fun calculateFocusVelocityMinutes(
            sessions: List<TimeSessionEntity>,
            windowDays: Int,
            nowMillis: Long
        ): Long {
            val windowMillis = windowDays.toLong() * 86_400_000L
            val threshold = nowMillis - windowMillis
            return sessions.filter { it.startedAt >= threshold }.sumOf { it.trackedMinutes(nowMillis).toLong() }
        }

        fun createRetrospectiveSession(
            taskId: String,
            durationMinutes: Int,
            endedAtMillis: Long
        ): TimeSessionEntity {
            val safeDuration = durationMinutes.coerceAtLeast(1)
            val startedAt = endedAtMillis - (safeDuration.toLong() * 60_000L)
            return TimeSessionEntity(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                startedAt = startedAt,
                endedAt = endedAtMillis,
                durationMinutes = safeDuration.toFloat(),
                totalPausedMs = 0L,
                pauseResumeCount = 0,
                sessionType = "MANUAL_RETROSPECTIVE"
            )
        }

        data class WoopAnchor(
            val wish: String,
            val outcome: String,
            val obstacle: String,
            val plan: String
        ) {
            val isComplete: Boolean get() = wish.isNotBlank() && outcome.isNotBlank() && obstacle.isNotBlank() && plan.isNotBlank()
        }

        fun isWoopAnchorEmpty(woop: WoopAnchor?): Boolean {
            return woop == null || (woop.wish.isBlank() && woop.outcome.isBlank() && woop.obstacle.isBlank() && woop.plan.isBlank())
        }
    }
}
