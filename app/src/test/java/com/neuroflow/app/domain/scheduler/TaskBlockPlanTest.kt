package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.entity.TaskEntity
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TaskBlockPlanTest : StringSpec({

    "a recorded block length wins over the task's own estimate" {
        val task = TaskEntity(
            title = "Recorded",
            estimatedDurationMinutes = 60,
            plannedBlockMinutes = 120
        )
        TaskBlockPlan.plannedMinutes(task) shouldBe 120
    }

    "the estimate is used when the planner recorded nothing" {
        val task = TaskEntity(title = "Legacy", estimatedDurationMinutes = 45)
        TaskBlockPlan.plannedMinutes(task) shouldBe 45
    }

    "unestimated work falls back to the effort band" {
        TaskBlockPlan.plannedMinutes(TaskEntity(title = "Hard", effortScore = 85)) shouldBe 60
        TaskBlockPlan.plannedMinutes(TaskEntity(title = "Medium", effortScore = 65)) shouldBe 45
        TaskBlockPlan.plannedMinutes(TaskEntity(title = "Easy", effortScore = 20)) shouldBe 30
    }

    "a zero or negative recorded block never reserves no time" {
        TaskBlockPlan.plannedMinutes(
            TaskEntity(title = "Bad", estimatedDurationMinutes = 30, plannedBlockMinutes = 0)
        ) shouldBe 30
        TaskBlockPlan.plannedMinutes(
            TaskEntity(title = "Bad", estimatedDurationMinutes = 30, plannedBlockMinutes = -5)
        ) shouldBe 30
    }

    "the scheduled end uses the recorded block, not the raw estimate" {
        val task = TaskEntity(
            title = "Recorded",
            estimatedDurationMinutes = 60,
            plannedBlockMinutes = 150
        )
        TaskBlockPlan.scheduledEndMillis(0L, task) shouldBe 150 * 60_000L
    }

    "a task still inside its recorded block is not missed" {
        val task = TaskEntity(
            title = "Recorded",
            estimatedDurationMinutes = 60,
            plannedBlockMinutes = 150
        )
        // 70 minutes in: past what the raw estimate implied, well inside the reserved block.
        TaskBlockPlan.blockHasExpired(
            task = task,
            startMillis = 0L,
            nowMillis = 70 * 60_000L,
            gracePeriodMillis = 5 * 60_000L
        ) shouldBe false
    }

    "a task past its recorded block and grace period is missed" {
        val task = TaskEntity(
            title = "Recorded",
            estimatedDurationMinutes = 60,
            plannedBlockMinutes = 150
        )
        TaskBlockPlan.blockHasExpired(
            task = task,
            startMillis = 0L,
            nowMillis = 156 * 60_000L,
            gracePeriodMillis = 5 * 60_000L
        ) shouldBe true
    }
})
