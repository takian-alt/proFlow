package com.neuroflow.app.data.local

import com.neuroflow.app.data.local.entity.ContractOutcome
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull

class ConvertersAdversarialStressTest : StringSpec({

    val converters = Converters()

    val adversarialInputs = listOf(
        "",
        " ",
        "   ",
        "\t",
        "\n",
        "\r\n",
        "\u0000",
        "\u0000\u0001\u0002\u0003",
        "\uFFFF",
        "",
        "null",
        "NULL",
        "Null",
        "undefined",
        "NaN",
        "0",
        "-1",
        "99999",
        "{}",
        "[]",
        "<xml></xml>",
        "<script>alert('xss')</script>",
        "' OR '1'='1",
        "'; DROP TABLE tasks; --",
        "1 UNION SELECT * FROM tasks",
        "🔥🧠⚡️🚀💎",
        "a".repeat(10000),
        "SPECIAL_!@#$%^&*()_+-=[]{}|;:,.<>?/~`",
        "   DO_FIRST   ",
        "do_first",
        "Do_First",
        "schedule",
        "HIGH_LEVEL_FUTURE_ENUM",
        "SOMEDAY",
        "URGENT_CRITICAL",
        "ARCHIVED_IN_VAULT",
        "BIWEEKLY_RECURRENCE",
        "HYPER_FOCUS_REWARD_TIER_DIAMOND",
        "QUANTUM_TASK_TYPE"
    )

    "Adversarial fuzzing: toQuadrant never throws and falls back to SCHEDULE on invalid input" {
        for (input in adversarialInputs) {
            val result = converters.toQuadrant(input)
            result shouldBe Quadrant.SCHEDULE
        }
    }

    "Adversarial fuzzing: toPriority never throws and falls back to MEDIUM on invalid input" {
        for (input in adversarialInputs) {
            val result = converters.toPriority(input)
            result shouldBe Priority.MEDIUM
        }
    }

    "Adversarial fuzzing: toTaskStatus never throws and falls back to ACTIVE on invalid input" {
        for (input in adversarialInputs) {
            val result = converters.toTaskStatus(input)
            result shouldBe TaskStatus.ACTIVE
        }
    }

    "Adversarial fuzzing: toRecurrence never throws and falls back to NONE on invalid input" {
        for (input in adversarialInputs) {
            val result = converters.toRecurrence(input)
            result shouldBe Recurrence.NONE
        }
    }

    "Adversarial fuzzing: toEnergyLevel never throws and falls back to MEDIUM on invalid input" {
        for (input in adversarialInputs) {
            val result = converters.toEnergyLevel(input)
            result shouldBe EnergyLevel.MEDIUM
        }
    }

    "Adversarial fuzzing: toTaskType never throws and falls back to ANALYTICAL on invalid input" {
        for (input in adversarialInputs) {
            val result = converters.toTaskType(input)
            result shouldBe TaskType.ANALYTICAL
        }
    }

    "Adversarial fuzzing: toRewardTier never throws and falls back to NONE on invalid input" {
        for (input in adversarialInputs) {
            val result = converters.toRewardTier(input)
            result shouldBe RewardTier.NONE
        }
    }

    "Adversarial fuzzing: toContractOutcome never throws and falls back to null on invalid/null input" {
        converters.toContractOutcome(null).shouldBeNull()
        for (input in adversarialInputs) {
            converters.toContractOutcome(input).shouldBeNull()
        }
    }

    "Round-trip integrity: all declared Quadrant enum constants serialize and deserialize perfectly" {
        for (quadrant in Quadrant.values()) {
            val serialized = converters.fromQuadrant(quadrant)
            val deserialized = converters.toQuadrant(serialized)
            deserialized shouldBe quadrant
        }
    }

    "Round-trip integrity: all declared Priority enum constants serialize and deserialize perfectly" {
        for (priority in Priority.values()) {
            val serialized = converters.fromPriority(priority)
            val deserialized = converters.toPriority(serialized)
            deserialized shouldBe priority
        }
    }

    "Round-trip integrity: all declared TaskStatus enum constants serialize and deserialize perfectly" {
        for (status in TaskStatus.values()) {
            val serialized = converters.fromTaskStatus(status)
            val deserialized = converters.toTaskStatus(serialized)
            deserialized shouldBe status
        }
    }

    "Round-trip integrity: all declared Recurrence enum constants serialize and deserialize perfectly" {
        for (recurrence in Recurrence.values()) {
            val serialized = converters.fromRecurrence(recurrence)
            val deserialized = converters.toRecurrence(serialized)
            deserialized shouldBe recurrence
        }
    }

    "Round-trip integrity: all declared EnergyLevel enum constants serialize and deserialize perfectly" {
        for (energyLevel in EnergyLevel.values()) {
            val serialized = converters.fromEnergyLevel(energyLevel)
            val deserialized = converters.toEnergyLevel(serialized)
            deserialized shouldBe energyLevel
        }
    }

    "Round-trip integrity: all declared TaskType enum constants serialize and deserialize perfectly" {
        for (taskType in TaskType.values()) {
            val serialized = converters.fromTaskType(taskType)
            val deserialized = converters.toTaskType(serialized)
            deserialized shouldBe taskType
        }
    }

    "Round-trip integrity: all declared RewardTier enum constants serialize and deserialize perfectly" {
        for (tier in RewardTier.values()) {
            val serialized = converters.fromRewardTier(tier)
            val deserialized = converters.toRewardTier(serialized)
            deserialized shouldBe tier
        }
    }

    "Round-trip integrity: all declared ContractOutcome enum constants and null serialize and deserialize perfectly" {
        converters.toContractOutcome(converters.fromContractOutcome(null)).shouldBeNull()
        for (outcome in ContractOutcome.values()) {
            val serialized = converters.fromContractOutcome(outcome)
            val deserialized = converters.toContractOutcome(serialized)
            deserialized shouldBe outcome
        }
    }
})
