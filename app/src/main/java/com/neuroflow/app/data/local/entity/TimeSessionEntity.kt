package com.neuroflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "time_sessions")
data class TimeSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val startedAt: Long,
    val endedAt: Long? = null,       // null = session still open/active
    val pausedAt: Long? = null,      // null = not paused
    val totalPausedMs: Long = 0L,    // accumulated paused time
    val durationMinutes: Float = 0f, // filled on stop
    val pauseResumeCount: Int = 0,   // number of pause/resume toggles during session
    val appSwitchCount: Int = 0,     // foreground app switches observed while session active
    val interruptionBurstCount: Int = 0, // grouped interruption bursts
    val sessionType: String = "MANUAL",
    val pomodoroNumber: Int = 0,
    val notes: String = ""
) {
    /**
     * Minutes actually worked. [durationMinutes] is only filled by the two paths that close a
     * session, so rows closed elsewhere and sessions still running have to be measured from the
     * timestamps. A live session is capped, because a timer left running by a killed process is
     * not a day's worth of deep work.
     */
    fun trackedMinutes(
        nowMillis: Long,
        liveCeilingMinutes: Float = LIVE_SESSION_CEILING_MINUTES
    ): Float {
        if (durationMinutes > 0f) return durationMinutes
        val isLive = endedAt == null
        val end = endedAt ?: nowMillis
        val pauseStart = pausedAt
        val currentPauseMs = if (isLive && pauseStart != null) maxOf(0L, end - pauseStart) else 0L
        val elapsed = (end - startedAt - totalPausedMs - currentPauseMs).coerceAtLeast(0L) / 60_000f
        return if (isLive) minOf(elapsed, liveCeilingMinutes) else elapsed
    }

    companion object {
        const val LIVE_SESSION_CEILING_MINUTES = 120f
    }
}
