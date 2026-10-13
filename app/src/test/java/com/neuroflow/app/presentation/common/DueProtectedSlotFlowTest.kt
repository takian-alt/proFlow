package com.neuroflow.app.presentation.common

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsRecurrence
import com.neuroflow.app.domain.scheduler.DueSlot
import com.neuroflow.app.domain.scheduler.SlotPhase
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class DueProtectedSlotFlowTest : StringSpec({

    fun inUtc(body: TestScope.() -> Unit) {
        val original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        try {
            runTest { body() }
        } finally {
            TimeZone.setDefault(original)
        }
    }

    fun at(hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 12, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun gymAtSix(durationMinutes: Int = 60) = ProtectedTimeSlot(
        id = "gym",
        label = "Gym",
        startMinuteOfDay = 18 * 60,
        durationMinutes = durationMinutes,
        recurrence = PtsRecurrence.DAILY
    )

    fun prefsWith(vararg slots: ProtectedTimeSlot) =
        MutableStateFlow(UserPreferences(protectedTimeSlots = slots.toList()))

    fun TestScope.collectSlots(
        preferences: MutableStateFlow<UserPreferences>,
        clock: () -> Long
    ): Pair<Job, MutableList<Any?>> {
        val emitted = mutableListOf<Any?>()
        val job = launch {
            dueProtectedSlotFlow(preferences, clock).collect { emitted.add(it ?: "none") }
        }
        return job to emitted
    }

    fun describe(value: Any?): Any? = when (value) {
        null -> "none"
        is DueSlot -> value.phase
        else -> value
    }

    "the running block takes over without waiting for a data write" {
        inUtc {
            var fakeNow = at(17, 50)
            val (job, emitted) = collectSlots(prefsWith(gymAtSix())) { fakeNow }
            runCurrent()
            describe(emitted.last()) shouldBe "none"

            fakeNow = at(18, 5)
            advanceTimeBy(70_000)
            describe(emitted.last()) shouldBe SlotPhase.RUNNING

            job.cancel()
        }
    }

    "turning the block off clears the takeover straight away" {
        inUtc {
            val preferences = prefsWith(gymAtSix())
            val (job, emitted) = collectSlots(preferences) { at(18, 5) }
            runCurrent()
            describe(emitted.last()) shouldBe SlotPhase.RUNNING
            ((emitted.last() as DueSlot).slot.label) shouldBe "Gym"

            preferences.value = UserPreferences(
                protectedTimeSlots = listOf(gymAtSix().copy(isEnabled = false))
            )
            runCurrent()
            describe(emitted.last()) shouldBe "none"

            job.cancel()
        }
    }

    "a block that has finished drops out after its wrap window" {
        inUtc {
            var fakeNow = at(18, 50)
            val (job, emitted) = collectSlots(prefsWith(gymAtSix())) { fakeNow }
            runCurrent()
            describe(emitted.last()) shouldBe SlotPhase.RUNNING

            fakeNow = at(19, 5)
            advanceTimeBy(70_000)
            describe(emitted.last()) shouldBe SlotPhase.WRAPPING

            fakeNow = at(19, 20)
            advanceTimeBy(70_000)
            describe(emitted.last()) shouldBe "none"

            job.cancel()
        }
    }
})
