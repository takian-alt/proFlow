package com.neuroflow.app.feature.hyperfocus

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.HyperFocusSessionMode
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.feature.hyperfocus.data.AppCatalogRepository
import com.neuroflow.app.feature.hyperfocus.data.GateRequestLogger
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusDataStore
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.feature.hyperfocus.domain.UnlockCodeRepository
import com.neuroflow.app.feature.hyperfocus.surface.SystemExempt
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class HyperFocusProgressTest : StringSpec({
    val testDispatcher = StandardTestDispatcher()

    beforeTest {
        Dispatchers.setMain(testDispatcher)
    }

    afterTest {
        Dispatchers.resetMain()
    }

    fun completed(id: String) = TaskEntity(id = id, title = id, status = TaskStatus.COMPLETED)

    fun viewModelFor(
        prefs: HyperFocusPreferences,
        completedTasks: List<TaskEntity>
    ): Triple<HyperFocusViewModel, MutableStateFlow<List<TaskEntity>>, CoroutineScope> {
        val completedState = MutableStateFlow(completedTasks)
        val taskRepository = mockk<TaskRepository>().apply {
            every { observeCompletedTasks() } returns completedState
            every { observeActiveTasks() } returns MutableStateFlow(emptyList())
            coEvery { getAllTasks() } returns completedState.value
        }
        val dataStore = mockk<HyperFocusDataStore>().apply {
            every { flow } returns MutableStateFlow(prefs)
            coEvery { current() } returns prefs
        }
        val viewModel = HyperFocusViewModel(
            hyperFocusManager = mockk<HyperFocusManager>(relaxed = true),
            hyperFocusDataStore = dataStore,
            unlockCodeRepository = mockk<UnlockCodeRepository>(relaxed = true),
            taskRepository = taskRepository,
            // Relaxed: this suite reads `progress`, which touches none of the three. The catalog
            // sweep they would take runs in an init coroutine, not in the flow under test.
            gateRequestLogger = mockk<GateRequestLogger>(relaxed = true),
            appCatalogRepository = mockk<AppCatalogRepository>(relaxed = true),
            systemExempt = mockk<SystemExempt>(relaxed = true)
        )
        // progress is a WhileSubscribed StateFlow — it only computes once something collects it.
        val scope = CoroutineScope(testDispatcher + Job())
        scope.launch { viewModel.progress.collect {} }
        return Triple(viewModel, completedState, scope)
    }

    "progress tracks completions the moment the task table changes" {
        val (viewModel, completedState, scope) = viewModelFor(
            prefs = HyperFocusPreferences(dailyTaskTarget = 3),
            completedTasks = emptyList()
        )
        testDispatcher.scheduler.runCurrent()
        viewModel.progress.value.completedSinceActivation shouldBe 0

        completedState.value = listOf(completed("a"))
        testDispatcher.scheduler.runCurrent()
        viewModel.progress.value.completedSinceActivation shouldBe 1

        completedState.value = listOf(completed("a"), completed("b"))
        testDispatcher.scheduler.runCurrent()
        viewModel.progress.value.completedSinceActivation shouldBe 2
        scope.cancel()
    }

    "only the tasks locked into the session count toward its target" {
        val (viewModel, _, scope) = viewModelFor(
            prefs = HyperFocusPreferences(dailyTaskTarget = 2, lockedTaskIds = setOf("b")),
            completedTasks = listOf(completed("a"), completed("b"))
        )
        testDispatcher.scheduler.runCurrent()
        viewModel.progress.value.completedSinceActivation shouldBe 1
        scope.cancel()
    }

    "a time-based session reports no task progress" {
        val (viewModel, _, scope) = viewModelFor(
            prefs = HyperFocusPreferences(
                dailyTaskTarget = 3,
                sessionMode = HyperFocusSessionMode.TIME_BASED
            ),
            completedTasks = listOf(completed("a"))
        )
        testDispatcher.scheduler.runCurrent()
        viewModel.progress.value.completedSinceActivation shouldBe 0
        viewModel.progress.value.totalTarget shouldBe 0
        scope.cancel()
    }
})
