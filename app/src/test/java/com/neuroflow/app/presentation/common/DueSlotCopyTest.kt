package com.neuroflow.app.presentation.common

import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsRecurrence
import com.neuroflow.app.domain.scheduler.DueSlot
import com.neuroflow.app.domain.scheduler.ProtectedSlotClock
import com.neuroflow.app.domain.scheduler.SlotPhase
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar
import java.util.TimeZone

class DueSlotCopyTest : StringSpec({

    fun <T> inUtc(body: () -> T): T {
        val original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        return try {
            body()
        } finally {
            TimeZone.setDefault(original)
        }
    }

    fun due(
        phase: SlotPhase,
        minutesToStart: Int = 0,
        minutesLeft: Int = 0
    ) = DueSlot(
        slot = ProtectedTimeSlot(
            id = "gym",
            label = "Gym",
            startMinuteOfDay = 18 * 60,
            durationMinutes = 60,
            recurrence = PtsRecurrence.DAILY
        ),
        phase = phase,
        startMillis = 0L,
        endMillis = 0L,
        minutesToStart = minutesToStart,
        minutesLeft = minutesLeft
    )

    "a running block counts down the part that is still ahead" {
        inUtc {
            due(SlotPhase.RUNNING, minutesLeft = 25).timeText() shouldBe "25 min left"
        }
    }

    "a long block is written in hours so the card stays scannable" {
        inUtc {
            due(SlotPhase.RUNNING, minutesLeft = 200).timeText() shouldBe "3 h 20 min left"
            due(SlotPhase.RUNNING, minutesLeft = 120).timeText() shouldBe "2 h left"
        }
    }

    "a block that just ended reads as finishing rather than zero minutes left" {
        inUtc {
            due(SlotPhase.WRAPPING).timeText() shouldBe "Wrapping up"
        }
    }

    "an approaching block counts down to its start" {
        inUtc {
            due(SlotPhase.LEAD_IN, minutesToStart = 45).timeText() shouldBe "Starts in 45 min"
            due(SlotPhase.LEAD_IN, minutesToStart = 90).timeText() shouldBe "Starts in 1 h 30 min"
            due(SlotPhase.LEAD_IN, minutesToStart = 120).timeText() shouldBe "Starts in 2 h"
        }
    }

    "the copy reads correctly off a slot built by the clock" {
        inUtc {
            val gym = ProtectedTimeSlot(
                id = "gym",
                label = "Gym",
                startMinuteOfDay = 18 * 60,
                durationMinutes = 60,
                recurrence = PtsRecurrence.DAILY
            )
            val now = Calendar.getInstance().apply {
                set(2026, Calendar.OCTOBER, 12, 18, 30, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val due = ProtectedSlotClock.dueSlots(listOf(gym), now).first()

            due.title() shouldBe "Gym"
            due.statusText() shouldBe "In progress"
            due.timeText() shouldBe "30 min left"
        }
    }
})
