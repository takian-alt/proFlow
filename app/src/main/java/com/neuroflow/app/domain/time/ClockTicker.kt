package com.neuroflow.app.domain.time

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared application-wide minute-boundary clock ticker for reactive circadian curves,
 * energy battery recalculation, and time-of-day UI transitions (e.g., Evening Shutdown).
 *
 * ViewModels inject [ClockTicker] optionally (nullable default in constructor) so unit tests
 * can remain deterministic without hanging on background infinite delay loops.
 */
interface ClockTicker {
    val currentMinuteTick: StateFlow<Long>
}

@Singleton
class DefaultClockTicker @Inject constructor() : ClockTicker {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _currentMinuteTick = MutableStateFlow(System.currentTimeMillis() / 60_000L)
    override val currentMinuteTick: StateFlow<Long> = _currentMinuteTick.asStateFlow()

    init {
        scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val delayMs = (60_000L - (now % 60_000L)).coerceAtLeast(1_000L)
                delay(delayMs)
                _currentMinuteTick.value = System.currentTimeMillis() / 60_000L
            }
        }
    }
}
