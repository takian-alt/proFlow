package com.neuroflow.app.presentation.widget

import android.content.Context
import android.content.Intent
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.worker.FocusWidgetUpdateWorker
import dagger.hilt.android.EntryPointAccessors
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class CompleteTaskReceiverTest : StringSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeTest {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(EntryPointAccessors::class)
        mockkObject(FocusWidgetUpdateWorker.Companion)
        every { FocusWidgetUpdateWorker.enqueueImmediateUpdate(any()) } just Runs
    }

    afterTest {
        unmockkStatic(EntryPointAccessors::class)
        unmockkObject(FocusWidgetUpdateWorker.Companion)
        Dispatchers.resetMain()
    }

    "CompleteTaskWidgetAction.execute marks task complete and enqueues immediate widget update" {
        val context = mockk<Context>(relaxed = true)
        val taskRepo = mockk<TaskRepository>()
        val prefsDataStore = mockk<com.neuroflow.app.data.local.UserPreferencesDataStore>(relaxed = true)
        coEvery { taskRepo.markTaskCompleted("task-123", any()) } returns true

        val entryPoint = mockk<CompleteTaskWidgetAction.WidgetActionEntryPoint>()
        every { entryPoint.taskRepository() } returns taskRepo
        every { entryPoint.preferencesDataStore() } returns prefsDataStore
        every { EntryPointAccessors.fromApplication(any(), CompleteTaskWidgetAction.WidgetActionEntryPoint::class.java) } returns entryPoint

        val result = CompleteTaskWidgetAction.execute(context, "task-123")
        result shouldBe true

        coVerify(exactly = 1) { taskRepo.markTaskCompleted("task-123", any()) }
        coVerify(exactly = 1) { prefsDataStore.updatePreferences(any()) }
        verify(exactly = 1) { FocusWidgetUpdateWorker.enqueueImmediateUpdate(context) }
    }

    "CompleteTaskWidgetAction.execute returns false and skips processing when taskId is blank" {
        val context = mockk<Context>(relaxed = true)
        val result = CompleteTaskWidgetAction.execute(context, "")
        result shouldBe false
        verify(exactly = 0) { FocusWidgetUpdateWorker.enqueueImmediateUpdate(any()) }
    }

    "CompleteTaskReceiver ignores broadcast with wrong action" {
        val receiver = CompleteTaskReceiver()
        val context = mockk<Context>(relaxed = true)
        val intent = Intent("com.neuroflow.app.action.WRONG_ACTION").apply {
            putExtra(CompleteTaskReceiver.EXTRA_TASK_ID, "task-123")
        }

        receiver.onReceive(context, intent)
    }

    "CompleteTaskReceiver ignores broadcast with null or blank taskId" {
        val receiver = CompleteTaskReceiver()
        val context = mockk<Context>(relaxed = true)
        val intent = Intent(CompleteTaskReceiver.ACTION_COMPLETE_TASK)

        receiver.onReceive(context, intent)
    }
})
