package com.neuroflow.app.feature.hyperfocus

import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.feature.hyperfocus.data.AllowlistMigrationRunner
import com.neuroflow.app.feature.hyperfocus.data.AppCatalogRepository
import com.neuroflow.app.feature.hyperfocus.data.GateRequestLogger
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusDataStore
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.feature.hyperfocus.domain.UnlockCodeRepository
import com.neuroflow.app.feature.hyperfocus.surface.SystemExempt
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Activation is the moment the allowlist tiers stop being optional: a session started on tiers that
 * were never derived has no essential tiles to show and gates every app the user owns. These tests
 * pin that the pending migration is run before the guard asks its question, not after the user is
 * told to pin something they cannot pin from this screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HyperFocusActivationMigrationTest : StringSpec({

    val testDispatcher = StandardTestDispatcher()
    val manager = mockk<HyperFocusManager>(relaxed = true)
    val dataStore = mockk<HyperFocusDataStore>(relaxed = true)
    val unlockCodeRepository = mockk<UnlockCodeRepository>(relaxed = true)
    val taskRepository = mockk<TaskRepository>(relaxed = true)
    val gateRequestLogger = mockk<GateRequestLogger>(relaxed = true)
    val catalog = mockk<AppCatalogRepository>(relaxed = true)
    val systemExempt = mockk<SystemExempt>(relaxed = true)
    val migrationRunner = mockk<AllowlistMigrationRunner>(relaxed = true)

    beforeTest {
        Dispatchers.setMain(testDispatcher)
        clearAllMocks()
        every { taskRepository.observeCompletedTasks() } returns MutableStateFlow(emptyList())
        every { taskRepository.observeActiveTasks() } returns MutableStateFlow(emptyList())
        coEvery { taskRepository.getActiveTasks() } returns emptyList()
        coEvery { systemExempt.packages() } returns setOf("android", "com.android.systemui")
        coEvery { catalog.launchableApps() } returns emptyList()
    }

    afterTest {
        Dispatchers.resetMain()
    }

    fun harness(prefs: HyperFocusPreferences): Pair<HyperFocusViewModel, MutableStateFlow<HyperFocusPreferences>> {
        val prefsFlow = MutableStateFlow(prefs)
        every { dataStore.flow } returns prefsFlow
        coEvery { dataStore.current() } answers { prefsFlow.value }
        coEvery { dataStore.update(any()) } coAnswers {
            prefsFlow.value = firstArg<(HyperFocusPreferences) -> HyperFocusPreferences>()(prefsFlow.value)
        }
        return HyperFocusViewModel(
            hyperFocusManager = manager,
            hyperFocusDataStore = dataStore,
            unlockCodeRepository = unlockCodeRepository,
            taskRepository = taskRepository,
            gateRequestLogger = gateRequestLogger,
            appCatalogRepository = catalog,
            systemExempt = systemExempt,
            allowlistMigrationRunner = migrationRunner,
        ) to prefsFlow
    }

    // What the runner does when the install is unmigrated: fills the tiers it derives.
    fun migrateInside(prefsFlow: MutableStateFlow<HyperFocusPreferences>) {
        coEvery { migrationRunner.runIfNeeded() } coAnswers {
            prefsFlow.value = prefsFlow.value.copy(
                essentialPackages = setOf("com.android.dialer"),
                allowlistMigratedAt = 1_000L
            )
        }
    }

    "AM-1: a timed activation derives the missing allowlist instead of refusing an empty surface" {
        val (viewModel, prefsFlow) = harness(HyperFocusPreferences())
        migrateInside(prefsFlow)

        viewModel.activateTimed(30)
        testDispatcher.scheduler.runCurrent()

        viewModel.activationMessage.value shouldBe null
        coVerify(exactly = 1) { migrationRunner.runIfNeeded() }
        coVerify(exactly = 1) { manager.activateTimed(30) }
    }

    "AM-2: the migration runs before the guard reads the prefs, so the guard sees the tiers" {
        // The order is the whole fix. Decide first and an unmigrated install is refused with
        // "Pin at least one app as essential first" on a screen that cannot pin anything.
        val (viewModel, prefsFlow) = harness(HyperFocusPreferences())
        migrateInside(prefsFlow)
        val calls = mutableListOf<String>()
        coEvery { migrationRunner.runIfNeeded() } coAnswers {
            calls += "migrate"
            prefsFlow.value = prefsFlow.value.copy(
                essentialPackages = setOf("com.android.dialer"),
                allowlistMigratedAt = 1_000L
            )
        }
        coEvery { manager.activateTimed(any()) } coAnswers { calls += "activate" }

        viewModel.activateTimed(45)
        testDispatcher.scheduler.runCurrent()

        calls shouldBe listOf("migrate", "activate")
    }

    "AM-3: a task-based activation runs the migration too" {
        val (viewModel, prefsFlow) = harness(HyperFocusPreferences())
        migrateInside(prefsFlow)

        viewModel.activate(setOf("task-1"))
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) { migrationRunner.runIfNeeded() }
    }

    "AM-4: with nothing left to migrate and no essentials, activation is still refused" {
        val (viewModel, _) = harness(HyperFocusPreferences(allowlistMigratedAt = 1_000L))
        coEvery { migrationRunner.runIfNeeded() } returns Unit

        viewModel.activateTimed(30)
        testDispatcher.scheduler.runCurrent()

        viewModel.activationMessage.value shouldBe "Pin at least one app as essential first."
        coVerify(exactly = 0) { manager.activateTimed(any()) }
    }
})
