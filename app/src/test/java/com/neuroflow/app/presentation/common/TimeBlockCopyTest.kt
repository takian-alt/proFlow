package com.neuroflow.app.presentation.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * The time-block card on the Focus screen used to read the schedule alone, so it kept counting down
 * to a start time while the session for that very task was recording.
 */
class TimeBlockCopyTest : StringSpec({

    val now = 1_760_000_000_000L
    val minutes = 60_000L

    "a running session reads FOCUSING NOW instead of a countdown" {
        timeBlockHeadline(
            anchorMillis = now + 59 * minutes,
            hasTime = true,
            isTracking = true,
            now = now
        ) shouldBe "Focusing now"

        timeBlockLabel(isRecurring = false, isTracking = true) shouldBe "🎯 FOCUSING NOW"
    }

    "a paused session still owns the block" {
        timeBlockHeadline(
            anchorMillis = now + 59 * minutes,
            hasTime = true,
            isTracking = true,
            now = now
        ) shouldBe "Focusing now"
    }

    "an idle screen keeps the countdown" {
        timeBlockHeadline(
            anchorMillis = now + 59 * minutes,
            hasTime = true,
            isTracking = false,
            now = now
        ) shouldBe "In 59 mins"

        timeBlockLabel(isRecurring = false, isTracking = false) shouldBe "📅 SCHEDULED"
    }

    "a recurring task keeps its recurrence label" {
        timeBlockLabel(isRecurring = true, isTracking = false) shouldBe "🔁 RECURRENCE TIME"
        timeBlockLabel(isRecurring = true, isTracking = true) shouldBe "🎯 FOCUSING NOW"
    }
})
