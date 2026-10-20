package com.neuroflow.app.presentation.v2.focus.engine

data class CalculatedTiming(
    val elapsedSeconds: Long,
    val remainingSeconds: Long,
    val isExpired: Boolean
)

object FocusTimingCalculator {
    fun calculate(
        startedAtMillis: Long,
        accumulatedPauseMillis: Long,
        pausedAtMillis: Long?,
        nowMillis: Long,
        targetDurationSeconds: Long
    ): CalculatedTiming {
        if (startedAtMillis <= 0L) {
            val isStopwatch = targetDurationSeconds <= 0L
            val remainingSeconds = if (isStopwatch) 0L else maxOf(0L, targetDurationSeconds)
            return CalculatedTiming(0L, remainingSeconds, false)
        }
        val safeNow = maxOf(nowMillis, startedAtMillis)
        val effectiveElapsedMillis = if (pausedAtMillis != null) {
            val safePause = maxOf(pausedAtMillis, startedAtMillis)
            maxOf(0L, safePause - startedAtMillis - accumulatedPauseMillis)
        } else {
            maxOf(0L, safeNow - startedAtMillis - accumulatedPauseMillis)
        }
        val elapsedSeconds = effectiveElapsedMillis / 1000L
        val isStopwatch = targetDurationSeconds <= 0L
        val remainingSeconds = if (isStopwatch) 0L else maxOf(0L, targetDurationSeconds - elapsedSeconds)
        val isExpired = (!isStopwatch) && (elapsedSeconds >= targetDurationSeconds)
        return CalculatedTiming(elapsedSeconds, remainingSeconds, isExpired)
    }

    fun onPause(currentPausedAt: Long?, nowMillis: Long): Long {
        return currentPausedAt ?: nowMillis
    }

    fun onResume(pausedAtMillis: Long, accumulatedPauseMillis: Long, nowMillis: Long): Pair<Long, Long?> {
        val pauseDuration = maxOf(0L, nowMillis - pausedAtMillis)
        val newAccumulated = accumulatedPauseMillis + pauseDuration
        return Pair(newAccumulated, null)
    }
}
