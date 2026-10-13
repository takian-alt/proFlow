package com.neuroflow.app.domain.repository

import com.neuroflow.app.data.local.entity.TimeSessionEntity
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SessionWindowMetricsTest : StringSpec({

    val now = 1_000_000_000_000L
    fun minutes(m: Long) = m * 60_000L

    fun session(
        startedOffset: Long,
        endedOffset: Long? = null,
        durationMinutes: Float = 0f,
        totalPausedMs: Long = 0L,
        pauseResumeCount: Int = 0,
        appSwitchCount: Int = 0,
        interruptionBurstCount: Int = 0
    ) = TimeSessionEntity(
        taskId = "t",
        startedAt = now - minutes(startedOffset),
        endedAt = endedOffset?.let { now - minutes(it) },
        durationMinutes = durationMinutes,
        totalPausedMs = totalPausedMs,
        pauseResumeCount = pauseResumeCount,
        appSwitchCount = appSwitchCount,
        interruptionBurstCount = interruptionBurstCount
    )

    val sessions = listOf(
        // Ran 10 to 6 minutes ago, 4 focused minutes, 3 interruptions.
        session(startedOffset = 10, endedOffset = 6, durationMinutes = 4f, pauseResumeCount = 2, appSwitchCount = 1),
        // Two hours ago, 10 focused minutes, 4 interruptions.
        session(startedOffset = 120, endedOffset = 110, durationMinutes = 10f, appSwitchCount = 3, interruptionBurstCount = 1),
        // Still open, started 3 minutes ago, one minute of that spent paused.
        session(startedOffset = 3, durationMinutes = 0f, totalPausedMs = 60_000L, pauseResumeCount = 1)
    )

    "every window comes from one pass with its own figures" {
        val windows = EnergyScoreRepository.summarizeRecentSessionWindows(sessions, now, listOf(5, 15, 60, 180))

        with(windows[0]) {
            recentFocusMinutes shouldBe 2f
            recentInterruptionCount shouldBe 1
            recentAppSwitchCount shouldBe 0
            recentPauseResumeCount shouldBe 1
            activeSessionCount shouldBe 1
        }
        with(windows[1]) {
            recentFocusMinutes shouldBe 6f
            recentInterruptionCount shouldBe 4
            recentAppSwitchCount shouldBe 1
            recentPauseResumeCount shouldBe 3
            activeSessionCount shouldBe 1
        }
        with(windows[2]) {
            recentFocusMinutes shouldBe 6f
            recentInterruptionCount shouldBe 4
            recentAppSwitchCount shouldBe 1
            recentPauseResumeCount shouldBe 3
            activeSessionCount shouldBe 1
        }
        with(windows[3]) {
            recentFocusMinutes shouldBe 16f
            recentInterruptionCount shouldBe 8
            recentAppSwitchCount shouldBe 4
            recentPauseResumeCount shouldBe 3
            activeSessionCount shouldBe 1
        }
    }

    "a window never credits more focus than its own length" {
        val longSession = listOf(session(startedOffset = 1, endedOffset = 0, durationMinutes = 40f))
        val windows = EnergyScoreRepository.summarizeRecentSessionWindows(longSession, now, listOf(5, 60))

        windows[0].recentFocusMinutes shouldBe 5f
        windows[1].recentFocusMinutes shouldBe 40f
    }

    "a single window asks for one entry" {
        val windows = EnergyScoreRepository.summarizeRecentSessionWindows(sessions, now, listOf(180))
        windows.size shouldBe 1
        windows[0].recentFocusMinutes shouldBe 16f
    }
})
