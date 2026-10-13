package com.neuroflow.app.feature.hyperfocus.gate

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class GateTaskEngineTest : StringSpec({

    val engine = GateTaskEngine(Random(42))

    "GT-1: count-back produces four descending values stepping by 7" {
        repeat(20) {
            val task = GateTaskEngine(Random(it)).newTask()
            if (task.type == GateTaskType.COUNT_BACK) {
                task.expected shouldHaveSize 4
                val values = task.expected.map { it.toInt() }
                values[1] shouldBe values[0] - 7
                values[2] shouldBe values[0] - 14
                values[3] shouldBe values[0] - 21
                values[0] shouldBeInRange 100..999
                (values[0] % 100 == 0) shouldBe false
            }
        }
    }

    "GT-2: every task shows its stimulus — the prompt contains the first expected value" {
        repeat(30) {
            val task = GateTaskEngine(Random(it)).newTask()
            when (task.type) {
                GateTaskType.TRANSCRIBE, GateTaskType.RESTATED -> task.expected shouldHaveSize 1
                else -> {}
            }
            task.prompt.contains(task.expected.first()) shouldBe true
        }
    }

    "GT-3: exact answer passes, case and padding insensitive" {
        val task = GateTask(GateTaskType.TRANSCRIBE, "Type exactly what you see", listOf("quiet desk"))
        engine.checkAnswer(task, listOf("  QUIET DESK ")) shouldBe true
    }

    "GT-4: one wrong character fails" {
        val task = GateTask(GateTaskType.TRANSCRIBE, "p", listOf("quiet desk"))
        engine.checkAnswer(task, listOf("quiet deskl")).shouldBeFalse()
    }

    "GT-5: count-back must match all four values" {
        val task = GateTask(GateTaskType.COUNT_BACK, "p", listOf("411", "404", "397", "390"))
        engine.checkAnswer(task, listOf("411", "404", "397", "391")).shouldBeFalse()
        engine.checkAnswer(task, listOf("411", "404", "397", "390")).shouldBeTrue()
    }

    "GT-6: blank answer on a shorter list fails instead of crashing" {
        val task = GateTask(GateTaskType.COUNT_BACK, "p", listOf("411", "404", "397", "390"))
        engine.checkAnswer(task, emptyList()).shouldBeFalse()
    }

    "GT-7: reason needs eight trimmed characters" {
        engine.isReasonValid("It's me").shouldBeFalse()         // 7 trimmed
        engine.isReasonValid("It's me ").shouldBeFalse()        // still 7 after trim
        engine.isReasonValid("I need this one").shouldBeTrue()   // 15
        engine.isReasonValid("        ").shouldBeFalse()
    }

    "GT-8: three mismatches auto-pass, two do not" {
        engine.shouldAutoPass(2).shouldBeFalse()
        engine.shouldAutoPass(3).shouldBeTrue()
        engine.shouldAutoPass(7).shouldBeTrue()
    }

    "GT-9: submit is blocked before the twelve-second floor" {
        engine.allowsSubmit(11_999L, "because it is mine").shouldBeFalse()
        engine.allowsSubmit(12_000L, "because it is mine").shouldBeTrue()
    }

    "GT-10: submit is blocked while the reason is short even after the floor" {
        engine.allowsSubmit(60_000L, "short").shouldBeFalse()
    }

    "GT-11: the mismatch count never closes the button, and one mismatch earns no pass" {
        engine.allowsSubmit(60_000L, "because it is mine").shouldBeTrue()
        engine.shouldAutoPass(1).shouldBeFalse()
    }

    "GT-12: the task types are reachable and never null" {
        val seen = (0 until 200).map { GateTaskEngine(Random(it)).newTask().type }.toSet()
        seen shouldBe GateTaskType.entries.toSet()
    }
})
