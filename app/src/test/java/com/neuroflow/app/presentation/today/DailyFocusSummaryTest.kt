package com.neuroflow.app.presentation.today

import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.domain.engine.DistractionEngine
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class DailyFocusSummaryTest : StringSpec({

    "minutes below an hour render as minutes" {
        formatDurationMinutes(0f) shouldBe "0 min"
        formatDurationMinutes(45f) shouldBe "45 min"
        formatDurationMinutes(59.4f) shouldBe "59 min"
    }

    "whole hours render without a trailing zero" {
        formatDurationMinutes(120f) shouldBe "2 h"
        formatDurationMinutes(60f) shouldBe "1 h"
    }

    "mixed durations render hours and minutes" {
        formatDurationMinutes(130f) shouldBe "2 h 10 m"
        formatDurationMinutes(275f) shouldBe "4 h 35 m"
    }

    "progress is clamped to the bar and survives a zero target" {
        deepWorkProgress(doneMinutes = 120f, targetMinutes = 240) shouldBe 0.5f
        deepWorkProgress(doneMinutes = 400f, targetMinutes = 240) shouldBe 1f
        deepWorkProgress(doneMinutes = 30f, targetMinutes = 0) shouldBe 0f
    }

    "a single short session is not yet a deep-work day" {
        deepWorkVerdict(10f, 240, 10) shouldContain "one 25 minutes starts the count"
    }

    "hitting the target stops selling another block" {
        deepWorkVerdict(240f, 240, 14) shouldContain "Target cleared"
        deepWorkVerdict(300f, 240, 14) shouldContain "Target cleared"
    }

    "mid-day with room left reads as an invitation, not a warning" {
        deepWorkVerdict(90f, 240, 11) shouldBe "2 h 30 m of deep work still fits in today."
        deepWorkVerdict(200f, 240, 11) shouldBe "40 min between you and today's target."
    }

    "late in the day the message changes" {
        deepWorkVerdict(90f, 240, 21) shouldContain "banking it to tomorrow"
        deepWorkVerdict(200f, 240, 21) shouldContain "a short block still counts"
    }

    "distraction score compares flagged apps against deep work" {
        DistractionEngine.dailyDistractionScore(distractedMs = 0L, focusMinutes = 60f) shouldBe 0f
        DistractionEngine.dailyDistractionScore(30L * 60_000L, 30f) shouldBe 50f
        DistractionEngine.dailyDistractionScore(60L * 60_000L, 0f) shouldBe 100f
        DistractionEngine.dailyDistractionScore(0L, 0f) shouldBe 0f
    }

    "without usage access the card never guesses" {
        distractionVerdict(state(permission = false, distracted = 0f, focus = 0f)) shouldContain "usage access"
    }

    "an empty flagged list is reported as no measurement, not as focus" {
        val unmeasured = state(permission = true, distracted = 0f, focus = 240f, flaggedApps = 0)
        distractionVerdict(unmeasured) shouldContain "not being measured"
        distractionAdvice(unmeasured, deepWorkRemainingMinutes = 0f) shouldContain "Flag the apps"
    }

    "a quiet day says so instead of inventing a verdict" {
        val quiet = state(permission = true, distracted = 2f, focus = 3f)
        quiet.hasEnoughSignal shouldBe false
        distractionVerdict(quiet) shouldContain "Not enough"
    }

    "flagged apps outspending deep work is called what it is" {
        val state = state(permission = true, distracted = 120f, focus = 60f)
        state.hasEnoughSignal shouldBe true
        distractionVerdict(state) shouldContain "outspent"
    }

    "advice points at the fix, not the feeling" {
        val heavy = state(permission = true, distracted = 120f, focus = 60f, topApp = "Instagram")
        distractionAdvice(heavy, deepWorkRemainingMinutes = 90f) shouldContain "Hyper Focus"
        distractionAdvice(heavy, deepWorkRemainingMinutes = 0f) shouldContain "Instagram"

        val clean = state(permission = true, distracted = 1f, focus = 120f)
        distractionAdvice(clean, deepWorkRemainingMinutes = 60f) shouldContain "Nothing to fix"
    }

    "the default list flags social and games but not messaging" {
        val defaults = DistractionEngine.DEFAULT_DISTRACTING_PACKAGES
        defaults shouldContain "com.instagram.android"
        defaults shouldContain "com.facebook.katana"
        defaults.contains("com.google.android.apps.messaging") shouldBe false
    }

    "today's sessions are measured from timestamps when the stored duration is missing" {
        val todayStart = startOfLocalDay(System.currentTimeMillis())
        val now = todayStart + 30 * 60_000L
        val sessions = listOf(
            TimeSessionEntity(
                taskId = "worked", startedAt = todayStart + 60_000,
                endedAt = todayStart + 30 * 60_000L, durationMinutes = 30f
            ),
            TimeSessionEntity(
                taskId = "open", startedAt = todayStart + 20 * 60_000L,
                endedAt = null, durationMinutes = 0f
            ),
            TimeSessionEntity(
                taskId = "recovered", startedAt = todayStart + 60_000,
                endedAt = todayStart + 11 * 60_000L, durationMinutes = 0f
            ),
            TimeSessionEntity(
                taskId = "empty", startedAt = todayStart + 60_000,
                endedAt = todayStart + 60_000, durationMinutes = 0f
            ),
            TimeSessionEntity(
                taskId = "yesterday", startedAt = todayStart - 3_600_000,
                endedAt = todayStart - 1_800_000, durationMinutes = 45f
            )
        )

        todayFocusSessions(sessions, now).map { it.taskId } shouldBe listOf("worked", "open", "recovered")
        focusMinutesOf(todayFocusSessions(sessions, now), now) shouldBe 50f
    }

    "a running block already counts toward today" {
        val todayStart = startOfLocalDay(System.currentTimeMillis())
        val live = TimeSessionEntity(
            taskId = "live", startedAt = todayStart + 60_000,
            endedAt = null, durationMinutes = 0f
        )
        val sessions = listOf(live)

        focusMinutesOf(todayFocusSessions(sessions, todayStart + 31 * 60_000L), todayStart + 31 * 60_000L) shouldBe 30f
    }

    "a timer abandoned hours ago is not a day's work" {
        val todayStart = startOfLocalDay(System.currentTimeMillis())
        val runaway = TimeSessionEntity(
            taskId = "runaway", startedAt = todayStart + 60_000,
            endedAt = null, durationMinutes = 0f
        )

        focusMinutesOf(listOf(runaway), todayStart + 10 * 3_600_000L) shouldBe
            TimeSessionEntity.LIVE_SESSION_CEILING_MINUTES
    }

    "time spent paused is not time spent focusing" {
        val todayStart = startOfLocalDay(System.currentTimeMillis())
        val paused = TimeSessionEntity(
            taskId = "paused", startedAt = todayStart,
            endedAt = null, pausedAt = todayStart + 10 * 60_000L, durationMinutes = 0f
        )

        focusMinutesOf(listOf(paused), todayStart + 20 * 60_000L) shouldBe 10f
    }

    "manually logged time is deep work too" {
        val now = System.currentTimeMillis()
        val todayStart = startOfLocalDay(now)
        val logged = TimeSessionEntity(
            taskId = "logged",
            startedAt = todayStart + 60_000,
            endedAt = todayStart + 1_500_000,
            durationMinutes = 24f,
            sessionType = "MANUAL_LOG"
        )

        focusMinutesOf(todayFocusSessions(listOf(logged), now), now) shouldBe 24f
    }

    "a session that opens just before midnight stays yesterday's" {
        val todayStart = startOfLocalDay(System.currentTimeMillis())
        val straddling = TimeSessionEntity(
            taskId = "straddling",
            startedAt = todayStart - 60_000,
            endedAt = todayStart + 1_800_000,
            durationMinutes = 31f
        )

        todayFocusSessions(listOf(straddling), todayStart + 600_000) shouldBe emptyList()
    }

    "focus windows stop where the session or the clock stops" {
        val todayStart = startOfLocalDay(System.currentTimeMillis())
        val now = todayStart + 30 * 60_000L
        val sessions = listOf(
            TimeSessionEntity(taskId = "open", startedAt = todayStart + 20 * 60_000L, endedAt = null),
            TimeSessionEntity(
                taskId = "closed", startedAt = todayStart + 60_000,
                endedAt = todayStart + 11 * 60_000L
            ),
            TimeSessionEntity(
                taskId = "empty", startedAt = todayStart + 60_000,
                endedAt = todayStart + 60_000
            ),
            TimeSessionEntity(
                taskId = "future", startedAt = todayStart + 40 * 60_000L,
                endedAt = todayStart + 45 * 60_000L
            )
        )

        focusWindows(sessions, now) shouldBe listOf(
            (todayStart + 20 * 60_000L) to now,
            (todayStart + 60_000L) to (todayStart + 11 * 60_000L)
        )
    }
})

private fun state(
    permission: Boolean,
    distracted: Float,
    focus: Float,
    topApp: String? = null,
    flaggedApps: Int = 3,
    overlap: Float = 0f
): DailyDistractionState {
    val distractedMs = (distracted * 60_000f).toLong()
    val trackedMs = distractedMs + (focus * 60_000f).toLong()
    return DailyDistractionState(
        permissionGranted = permission,
        screenMinutes = distracted + focus,
        distractingMinutes = distracted,
        focusMinutes = focus,
        distractionScore = DistractionEngine.dailyDistractionScore(distractedMs, focus),
        hasEnoughSignal = trackedMs >= DistractionEngine.MIN_TRACKED_MS_FOR_VERDICT,
        flaggedAppCount = flaggedApps,
        overlapMinutes = overlap,
        topApps = if (topApp != null) listOf(topApp to distracted) else emptyList()
    )
}
