package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsCategory
import com.neuroflow.app.domain.model.PtsRecurrence
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar
import java.util.TimeZone

/**
 * 2026-10-12 is a Monday, 2026-10-10 a Saturday. Everything runs with the default zone pinned to
 * UTC so the minute-of-day arithmetic is exact.
 */
class ProtectedSlotClockTest : StringSpec({

    fun <T> inUtc(body: () -> T): T {
        val original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        return try {
            body()
        } finally {
            TimeZone.setDefault(original)
        }
    }

    fun slot(
        label: String = "Slot",
        startMinute: Int,
        durationMinutes: Int,
        recurrence: PtsRecurrence = PtsRecurrence.DAILY,
        weekdayBitmask: Int = 0b1111111,
        dateMillis: Long? = null,
        enabled: Boolean = true,
        bufferMinutes: Int = 0
    ) = ProtectedTimeSlot(
        id = label,
        label = label,
        startMinuteOfDay = startMinute,
        durationMinutes = durationMinutes,
        recurrence = recurrence,
        weekdayBitmask = weekdayBitmask,
        category = PtsCategory.EXERCISE,
        recoveryBufferMinutes = bufferMinutes,
        isEnabled = enabled,
        dateMillis = dateMillis
    )

    fun at(day: Int, hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, day, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    "a running slot is due and reports what is left of it" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            val due = ProtectedSlotClock.dueSlots(listOf(gym), at(12, 18, 50))

            due.size shouldBe 1
            due[0].slot.label shouldBe "Gym"
            due[0].phase shouldBe SlotPhase.RUNNING
            due[0].minutesToStart shouldBe 0
            due[0].minutesLeft shouldBe 25
        }
    }

    "a slot counts as running from its first minute" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            val due = ProtectedSlotClock.dueSlots(listOf(gym), at(12, 18, 30))

            due[0].phase shouldBe SlotPhase.RUNNING
            due[0].minutesLeft shouldBe 45
        }
    }

    "nothing is due more than the lead-in window away" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            ProtectedSlotClock.dueSlots(listOf(gym), at(12, 16, 29)) shouldBe emptyList()
        }
    }

    "the lead-in window is two hours wide" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            val due = ProtectedSlotClock.dueSlots(listOf(gym), at(12, 16, 30))

            due.size shouldBe 1
            due[0].phase shouldBe SlotPhase.LEAD_IN
            due[0].minutesToStart shouldBe 120
        }
    }

    "a slot that just ended is kept briefly" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            val due = ProtectedSlotClock.dueSlots(listOf(gym), at(12, 19, 20))

            due.size shouldBe 1
            due[0].phase shouldBe SlotPhase.WRAPPING
            due[0].minutesLeft shouldBe 0
        }
    }

    "ten minutes after the end the slot is gone" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            ProtectedSlotClock.dueSlots(listOf(gym), at(12, 19, 25)) shouldBe emptyList()
        }
    }

    "a disabled slot never takes over" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45, enabled = false)

            ProtectedSlotClock.dueSlots(listOf(gym), at(12, 18, 50)) shouldBe emptyList()
        }
    }

    "a slot that runs past midnight is still running in the small hours" {
        inUtc {
            val late = slot("Night run", startMinute = 23 * 60 + 50, durationMinutes = 60)

            val due = ProtectedSlotClock.dueSlots(listOf(late), at(13, 0, 5))

            due.size shouldBe 1
            due[0].phase shouldBe SlotPhase.RUNNING
            due[0].minutesLeft shouldBe 45
        }
    }

    "yesterday's specific date is not due today" {
        inUtc {
            val oneOff = slot(
                "Doctor", startMinute = 18 * 60 + 30, durationMinutes = 45,
                recurrence = PtsRecurrence.SPECIFIC_DATE, dateMillis = at(11, 18, 30)
            )

            ProtectedSlotClock.dueSlots(listOf(oneOff), at(12, 18, 50)) shouldBe emptyList()
        }
    }

    "a specific-date slot is due on its own day" {
        inUtc {
            val oneOff = slot(
                "Doctor", startMinute = 18 * 60 + 30, durationMinutes = 45,
                recurrence = PtsRecurrence.SPECIFIC_DATE, dateMillis = at(12, 9, 0)
            )

            val due = ProtectedSlotClock.dueSlots(listOf(oneOff), at(12, 18, 50))

            due.size shouldBe 1
            due[0].phase shouldBe SlotPhase.RUNNING
        }
    }

    "the slot that started most recently takes the top spot" {
        inUtc {
            val earlier = slot("Lift", startMinute = 18 * 60, durationMinutes = 60)
            val later = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 60)

            val due = ProtectedSlotClock.dueSlots(listOf(earlier, later), at(12, 18, 40))

            due.map { it.slot.label } shouldBe listOf("Gym", "Lift")
        }
    }

    "running slots outrank ones that have not started" {
        inUtc {
            val running = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)
            val approaching = slot("Dinner", startMinute = 20 * 60, durationMinutes = 60)

            val due = ProtectedSlotClock.dueSlots(listOf(approaching, running), at(12, 18, 50))

            due.map { it.slot.label } shouldBe listOf("Gym", "Dinner")
            due[1].phase shouldBe SlotPhase.LEAD_IN
            due[1].minutesToStart shouldBe 70
        }
    }

    "approaching slots are ordered by how soon they start" {
        inUtc {
            val sooner = slot("Dinner", startMinute = 20 * 60, durationMinutes = 60)
            val later = slot("Family call", startMinute = 21 * 60, durationMinutes = 30)

            val due = ProtectedSlotClock.dueSlots(listOf(later, sooner), at(12, 19, 0))

            due.map { it.slot.label } shouldBe listOf("Dinner", "Family call")
        }
    }

    "the running slot takes over and the approaching ones queue" {
        inUtc {
            val running = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)
            val approaching = slot("Dinner", startMinute = 20 * 60, durationMinutes = 60)
            val slots = listOf(approaching, running)

            ProtectedSlotClock.takeoverOf(slots, at(12, 18, 50))?.slot?.label shouldBe "Gym"
            ProtectedSlotClock.approaching(slots, at(12, 18, 50)).map { it.slot.label } shouldBe
                listOf("Dinner")
        }
    }

    "a wrapping slot still holds the top spot" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            val takeover = ProtectedSlotClock.takeoverOf(listOf(gym), at(12, 19, 20))

            takeover?.phase shouldBe SlotPhase.WRAPPING
        }
    }

    "nothing takes over while every slot is still ahead" {
        inUtc {
            val dinner = slot("Dinner", startMinute = 20 * 60, durationMinutes = 60)
            val call = slot("Family call", startMinute = 21 * 60, durationMinutes = 30)

            ProtectedSlotClock.takeoverOf(listOf(dinner, call), at(12, 19, 0)) shouldBe null
            ProtectedSlotClock.approaching(listOf(dinner, call), at(12, 19, 0))
                .map { it.slot.label } shouldBe listOf("Dinner", "Family call")
        }
    }

    "no slots means no takeover" {
        inUtc {
            ProtectedSlotClock.dueSlots(emptyList(), at(12, 18, 50)) shouldBe emptyList()
            ProtectedSlotClock.takeoverOf(emptyList(), at(12, 18, 50)) shouldBe null
            ProtectedSlotClock.approaching(emptyList(), at(12, 18, 50)) shouldBe emptyList()
        }
    }

    "the recurrence predicate matches each mode" {
        inUtc {
            val monday = at(12, 12, 0)
            val saturday = at(10, 12, 0)

            ProtectedSlotClock.occursOnDay(slot("a", 0, 15), monday) shouldBe true
            ProtectedSlotClock.occursOnDay(
                slot("b", 0, 15, recurrence = PtsRecurrence.WEEKDAYS), monday
            ) shouldBe true
            ProtectedSlotClock.occursOnDay(
                slot("c", 0, 15, recurrence = PtsRecurrence.WEEKDAYS), saturday
            ) shouldBe false
            ProtectedSlotClock.occursOnDay(
                slot("d", 0, 15, recurrence = PtsRecurrence.WEEKENDS), saturday
            ) shouldBe true
            ProtectedSlotClock.occursOnDay(
                slot("e", 0, 15, recurrence = PtsRecurrence.WEEKENDS), monday
            ) shouldBe false
            ProtectedSlotClock.occursOnDay(
                slot("f", 0, 15, recurrence = PtsRecurrence.CUSTOM, weekdayBitmask = 0b10), monday
            ) shouldBe true
            ProtectedSlotClock.occursOnDay(
                slot("g", 0, 15, recurrence = PtsRecurrence.CUSTOM, weekdayBitmask = 0b100), monday
            ) shouldBe false
            ProtectedSlotClock.occursOnDay(
                slot("h", 0, 15, recurrence = PtsRecurrence.SPECIFIC_DATE, dateMillis = at(12, 7, 0)),
                monday
            ) shouldBe true
            ProtectedSlotClock.occursOnDay(
                slot("i", 0, 15, recurrence = PtsRecurrence.SPECIFIC_DATE, dateMillis = at(10, 7, 0)),
                monday
            ) shouldBe false
            ProtectedSlotClock.occursOnDay(
                slot("j", 0, 15, recurrence = PtsRecurrence.SPECIFIC_DATE), monday
            ) shouldBe false
        }
    }

    "the disabled switch does not change which days a slot falls on" {
        inUtc {
            ProtectedSlotClock.occursOnDay(slot("a", 0, 15, enabled = false), at(12, 12, 0)) shouldBe true
        }
    }

    "a recovery buffer does not extend the takeover" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45, bufferMinutes = 30)

            val due = ProtectedSlotClock.dueSlots(listOf(gym), at(12, 19, 20))

            due.size shouldBe 1
            due[0].phase shouldBe SlotPhase.WRAPPING
        }
    }
})
