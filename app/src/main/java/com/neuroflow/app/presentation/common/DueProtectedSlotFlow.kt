package com.neuroflow.app.presentation.common

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.domain.scheduler.DueSlot
import com.neuroflow.app.domain.scheduler.ProtectedSlotClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

private const val MINUTE_MILLIS = 60_000L

/**
 * A protected block changes phase on the wall clock, not on a database write, so a surface that only
 * recomputes when its data moves would keep showing the previous card until the next tap. This
 * re-reads the takeover at every minute boundary as well as whenever the preferences move.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun dueProtectedSlotFlow(
    preferences: Flow<UserPreferences>,
    nowMillis: () -> Long = { System.currentTimeMillis() }
): Flow<DueSlot?> = preferences.flatMapLatest { prefs ->
    flow {
        while (true) {
            val now = nowMillis()
            emit(ProtectedSlotClock.takeoverOf(prefs.protectedTimeSlots, now))
            delay(MINUTE_MILLIS - now % MINUTE_MILLIS)
        }
    }
}
