package com.neuroflow.app.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.neuroflow.app.data.local.entity.ContractOutcome
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.UlyssesContractEntity
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.repository.UlyssesContractRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

class UlyssesEvaluatorWorkerTest : StringSpec({

    val context = mockk<Context>(relaxed = true)
    val workerParams = mockk<WorkerParameters>(relaxed = true)
    val contractRepo = mockk<UlyssesContractRepository>(relaxed = true)
    val taskRepo = mockk<TaskRepository>(relaxed = true)

    "doWork returns success and does not mark LOSS prematurely when deadline is in the future and task is active" {
        val contractId = "contract-1"
        val taskId = "task-1"
        val futureDeadline = System.currentTimeMillis() + 3_600_000L

        val contract = UlyssesContractEntity(
            id = contractId,
            taskId = taskId,
            deadlineAt = futureDeadline,
            consequence = "Donate $50",
            outcome = null
        )
        val activeTask = TaskEntity(
            id = taskId,
            title = "Crucial Report",
            completedAt = null
        )

        every { workerParams.inputData } returns workDataOf("contractId" to contractId)
        coEvery { contractRepo.getById(contractId) } returns contract
        coEvery { taskRepo.getById(taskId) } returns activeTask

        val worker = UlyssesEvaluatorWorker(
            appContext = context,
            workerParams = workerParams,
            contractRepository = contractRepo,
            taskRepository = taskRepo
        )

        val result = worker.doWork()
        result shouldBe ListenableWorker.Result.success()
        // Must NOT update the contract with LOSS prematurely!
        coVerify(exactly = 0) { contractRepo.update(match { it.outcome == ContractOutcome.LOSS }) }
    }

    "doWork marks WIN when task completed before deadline" {
        val contractId = "contract-win"
        val taskId = "task-win"
        val deadline = System.currentTimeMillis() + 3_600_000L

        val contract = UlyssesContractEntity(
            id = contractId,
            taskId = taskId,
            deadlineAt = deadline,
            consequence = "Donate $50",
            outcome = null
        )
        val completedTask = TaskEntity(
            id = taskId,
            title = "Finished Task",
            completedAt = deadline - 1000L
        )

        every { workerParams.inputData } returns workDataOf("contractId" to contractId)
        coEvery { contractRepo.getById(contractId) } returns contract
        coEvery { taskRepo.getById(taskId) } returns completedTask

        val worker = UlyssesEvaluatorWorker(
            appContext = context,
            workerParams = workerParams,
            contractRepository = contractRepo,
            taskRepository = taskRepo
        )

        val result = worker.doWork()
        result shouldBe ListenableWorker.Result.success()
        coVerify(exactly = 1) { contractRepo.update(match { it.id == contractId && it.outcome == ContractOutcome.WIN }) }
    }

    "doWork marks LOSS when deadline has passed and task was not completed" {
        val contractId = "contract-loss"
        val taskId = "task-loss"
        val pastDeadline = System.currentTimeMillis() - 1000L

        val contract = UlyssesContractEntity(
            id = contractId,
            taskId = taskId,
            deadlineAt = pastDeadline,
            consequence = "Donate $50",
            outcome = null
        )
        val incompleteTask = TaskEntity(
            id = taskId,
            title = "Unfinished Task",
            completedAt = null
        )

        every { workerParams.inputData } returns workDataOf("contractId" to contractId)
        coEvery { contractRepo.getById(contractId) } returns contract
        coEvery { taskRepo.getById(taskId) } returns incompleteTask

        val worker = UlyssesEvaluatorWorker(
            appContext = context,
            workerParams = workerParams,
            contractRepository = contractRepo,
            taskRepository = taskRepo
        )

        val result = worker.doWork()
        result shouldBe ListenableWorker.Result.success()
        coVerify(exactly = 1) { contractRepo.update(match { it.id == contractId && it.outcome == ContractOutcome.LOSS }) }
    }

    "BUG-201: selecting task with future deadline synchronizes contract deadline" {
        val now = System.currentTimeMillis()
        val futureTaskDeadline = now + 48 * 3600_000L
        val taskWithDeadline = TaskEntity(
            id = "task-with-deadline",
            title = "Crucial Pitch",
            deadlineDate = futureTaskDeadline
        )

        var deadlineMillis = now + 86_400_000L
        val taskDeadline = taskWithDeadline.resolveDeadlineEpochMillis()
        if (taskDeadline != null && taskDeadline > now) {
            deadlineMillis = taskDeadline
        }

        deadlineMillis shouldBe futureTaskDeadline
    }
})
