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

class ConvertersTest : StringSpec({

    val converters = Converters()

    "Quadrant converter handles valid, unknown and corrupted values safely" {
        converters.fromQuadrant(Quadrant.DO_FIRST) shouldBe "DO_FIRST"
        converters.toQuadrant("DO_FIRST") shouldBe Quadrant.DO_FIRST
        converters.toQuadrant("SCHEDULE") shouldBe Quadrant.SCHEDULE
        converters.toQuadrant("UNKNOWN_QUADRANT") shouldBe Quadrant.SCHEDULE
        converters.toQuadrant("") shouldBe Quadrant.SCHEDULE
        converters.toQuadrant("corrupted#123") shouldBe Quadrant.SCHEDULE
    }

    "Priority converter handles valid, unknown and corrupted values safely" {
        converters.fromPriority(Priority.HIGH) shouldBe "HIGH"
        converters.toPriority("HIGH") shouldBe Priority.HIGH
        converters.toPriority("LOW") shouldBe Priority.LOW
        converters.toPriority("URGENT_CUSTOM") shouldBe Priority.MEDIUM
        converters.toPriority("") shouldBe Priority.MEDIUM
        converters.toPriority("null") shouldBe Priority.MEDIUM
    }

    "TaskStatus converter handles valid, unknown and corrupted values safely" {
        converters.fromTaskStatus(TaskStatus.COMPLETED) shouldBe "COMPLETED"
        converters.toTaskStatus("COMPLETED") shouldBe TaskStatus.COMPLETED
        converters.toTaskStatus("ACTIVE") shouldBe TaskStatus.ACTIVE
        converters.toTaskStatus("ARCHIVED_FUTURE") shouldBe TaskStatus.ACTIVE
        converters.toTaskStatus("") shouldBe TaskStatus.ACTIVE
    }

    "Recurrence converter handles valid, unknown and corrupted values safely" {
        converters.fromRecurrence(Recurrence.DAILY) shouldBe "DAILY"
        converters.toRecurrence("DAILY") shouldBe Recurrence.DAILY
        converters.toRecurrence("WEEKLY") shouldBe Recurrence.WEEKLY
        converters.toRecurrence("BIWEEKLY_CUSTOM") shouldBe Recurrence.NONE
        converters.toRecurrence("") shouldBe Recurrence.NONE
    }

    "EnergyLevel converter handles valid, unknown and corrupted values safely" {
        converters.fromEnergyLevel(EnergyLevel.HIGH) shouldBe "HIGH"
        converters.toEnergyLevel("HIGH") shouldBe EnergyLevel.HIGH
        converters.toEnergyLevel("LOW") shouldBe EnergyLevel.LOW
        converters.toEnergyLevel("SUPER_HIGH") shouldBe EnergyLevel.MEDIUM
        converters.toEnergyLevel("") shouldBe EnergyLevel.MEDIUM
    }

    "TaskType converter handles valid, unknown and corrupted values safely" {
        converters.fromTaskType(TaskType.CREATIVE) shouldBe "CREATIVE"
        converters.toTaskType("CREATIVE") shouldBe TaskType.CREATIVE
        converters.toTaskType("UNKNOWN_TYPE") shouldBe TaskType.ANALYTICAL
        converters.toTaskType("") shouldBe TaskType.ANALYTICAL
    }

    "ContractOutcome converter handles valid, unknown, corrupted and null values safely" {
        converters.fromContractOutcome(ContractOutcome.WIN) shouldBe "WIN"
        converters.fromContractOutcome(ContractOutcome.LOSS) shouldBe "LOSS"
        converters.fromContractOutcome(null) shouldBe null
        converters.toContractOutcome("WIN") shouldBe ContractOutcome.WIN
        converters.toContractOutcome("LOSS") shouldBe ContractOutcome.LOSS
        converters.toContractOutcome("UNKNOWN_OUTCOME") shouldBe null
        converters.toContractOutcome("") shouldBe null
        converters.toContractOutcome(null) shouldBe null
    }

    "RewardTier converter handles valid, unknown and corrupted values safely" {
        converters.fromRewardTier(RewardTier.MICRO) shouldBe "MICRO"
        converters.fromRewardTier(RewardTier.EARNED) shouldBe "EARNED"
        converters.toRewardTier("MICRO") shouldBe RewardTier.MICRO
        converters.toRewardTier("EARNED") shouldBe RewardTier.EARNED
        converters.toRewardTier("DIAMOND_UNKNOWN") shouldBe RewardTier.NONE
        converters.toRewardTier("") shouldBe RewardTier.NONE
    }
})
