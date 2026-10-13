package com.neuroflow.app.data.repository

import androidx.work.WorkManager
import com.neuroflow.app.data.local.dao.TaskDao
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.presentation.launcher.hyperfocus.domain.HyperFocusManager
import dagger.Lazy
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot

class TaskRepositoryEstimateEditTest : StringSpec({

    val taskDao = mockk<TaskDao>(relaxed = true)
    val hyperFocus = mockk<HyperFocusManager>(relaxed = true)
    val workManager = mockk<WorkManager>(relaxed = true)
    val repository = TaskRepository(taskDao, Lazy { hyperFocus }, workManager)

    fun stored(estimate: Int, reserved: Int) = TaskEntity(
        id = "t1",
        title = "Write report",
        estimatedDurationMinutes = estimate,
        plannedBlockMinutes = reserved
    )

    "editing the estimate drops the block length the planner reserved for the old one" {
        val old = stored(estimate = 30, reserved = 90)
        coEvery { taskDao.getById("t1") } returns old
        val written = slot<TaskEntity>()
        coEvery { taskDao.update(capture(written)) } returns Unit

        repository.update(old.copy(estimatedDurationMinutes = 60))

        written.captured.estimatedDurationMinutes shouldBe 60
        // TaskBlockPlan prefers the reservation, so leaving it at 90 would keep the old length.
        written.captured.plannedBlockMinutes shouldBe 0
    }

    "an untouched estimate keeps the reservation" {
        val old = stored(estimate = 30, reserved = 90)
        coEvery { taskDao.getById("t1") } returns old
        val written = slot<TaskEntity>()
        coEvery { taskDao.update(capture(written)) } returns Unit

        repository.update(old.copy(title = "Write final report"))

        written.captured.plannedBlockMinutes shouldBe 90
    }

    "a task the planner never sized keeps its zero reservation" {
        val old = stored(estimate = 30, reserved = 0)
        coEvery { taskDao.getById("t1") } returns old
        val written = slot<TaskEntity>()
        coEvery { taskDao.update(capture(written)) } returns Unit

        repository.update(old.copy(estimatedDurationMinutes = 15))

        written.captured.plannedBlockMinutes shouldBe 0
    }
})
