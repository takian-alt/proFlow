package com.neuroflow.app.feature.hyperfocus

import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.HyperFocusSessionMode
import com.neuroflow.app.feature.hyperfocus.data.AllowlistMigrationRunner
import com.neuroflow.app.feature.hyperfocus.data.AppCatalogRepository
import com.neuroflow.app.feature.hyperfocus.data.GateGrantSource
import com.neuroflow.app.feature.hyperfocus.data.GateRequestDao
import com.neuroflow.app.feature.hyperfocus.data.GateRequestEntity
import com.neuroflow.app.feature.hyperfocus.data.GateRequestLogger
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusDataStore
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.feature.hyperfocus.domain.UnlockCodeRepository
import com.neuroflow.app.feature.hyperfocus.gate.GateTask
import com.neuroflow.app.feature.hyperfocus.gate.GateTaskType
import com.neuroflow.app.feature.hyperfocus.gate.gatewayBudgetLabel
import com.neuroflow.app.feature.hyperfocus.surface.SystemExempt
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * The gate, the gateway and the emergency control are composables and cannot be tested here, but
 * every decision they make lives in the ViewModel: what a pass may spend, what the gateway may
 * list, and when a request is written down. Those decisions are what this suite pins.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HyperFocusGateControlTest : StringSpec({

    val testDispatcher = StandardTestDispatcher()
    val row = slot<GateRequestEntity>()
    val minutes = slot<Int>()
    val cutoff = slot<Long>()

    val manager = mockk<HyperFocusManager>(relaxed = true)
    val catalog = mockk<AppCatalogRepository>(relaxed = true)
    val systemExempt = mockk<SystemExempt>(relaxed = true)
    val gateRequestDao = mockk<GateRequestDao>(relaxed = true)
    val taskRepository = mockk<TaskRepository>(relaxed = true)
    val dataStore = mockk<HyperFocusDataStore>(relaxed = true)
    val unlockCodeRepository = mockk<UnlockCodeRepository>(relaxed = true)

    class Harness(
        val viewModel: HyperFocusViewModel,
        val prefsFlow: MutableStateFlow<HyperFocusPreferences>,
        val scope: CoroutineScope,
    ) {
        fun subscribe(flow: StateFlow<*>) {
            scope.launch { flow.collect {} }
            testDispatcher.scheduler.runCurrent()
        }
    }

    beforeTest {
        Dispatchers.setMain(testDispatcher)
        clearAllMocks()
        every { taskRepository.observeCompletedTasks() } returns MutableStateFlow(emptyList())
        every { taskRepository.observeActiveTasks() } returns MutableStateFlow(emptyList())
        coEvery { systemExempt.packages() } returns setOf("android", "com.android.systemui")
        coEvery { catalog.launchableApps() } returns emptyList()
        coEvery { catalog.labelFor(any()) } answers { firstArg() }
    }

    afterTest {
        Dispatchers.resetMain()
    }

    fun harness(prefs: HyperFocusPreferences): Harness {
        val prefsFlow = MutableStateFlow(prefs)
        every { dataStore.flow } returns prefsFlow
        coEvery { dataStore.current() } answers { prefsFlow.value }
        coEvery { dataStore.update(any()) } coAnswers {
            prefsFlow.value = firstArg<(HyperFocusPreferences) -> HyperFocusPreferences>()(prefsFlow.value)
        }

        val viewModel = HyperFocusViewModel(
            hyperFocusManager = manager,
            hyperFocusDataStore = dataStore,
            unlockCodeRepository = unlockCodeRepository,
            taskRepository = taskRepository,
            gateRequestLogger = GateRequestLogger(gateRequestDao, catalog, dataStore),
            appCatalogRepository = catalog,
            systemExempt = systemExempt,
            // Relaxed: activation is not under test here, and its migration call has a suite of its
            // own (HyperFocusActivationMigrationTest).
            allowlistMigrationRunner = mockk<AllowlistMigrationRunner>(relaxed = true),
        )
        return Harness(viewModel, prefsFlow, CoroutineScope(testDispatcher + Job()))
    }

    // A live budget that no app holds: the shape E1 says is still spendable.
    fun budgetPrefs(secondsFromNow: Long) = HyperFocusPreferences(
        isActive = true,
        sessionId = "session-1",
        essentialPackages = setOf("com.android.dialer"),
        allowedPackages = setOf("com.todoist"),
        activePassExpiresAt = System.currentTimeMillis() + secondsFromNow * 1000L
    )

    fun entry(pkg: String) = AppCatalogRepository.AppEntry(
        pkg, pkg, mockk(relaxed = true), mockk(relaxed = true)
    )

    val transcribe = GateTask(GateTaskType.TRANSCRIBE, "Type exactly what you see:\n\namber frost", listOf("amber frost"))

    val gatePrefs = HyperFocusPreferences(
        isActive = true,
        sessionId = "session-1",
        pendingGatePackage = "com.instagram.android",
        activePassExpiresAt = System.currentTimeMillis() + 600_000L
    )

    "GC-1: with no budget the gateway grants nothing and launches nothing" {
        val h = harness(gatePrefs.copy(activePassExpiresAt = null))
        var launched = false

        h.viewModel.grantPassFor("com.instagram.android") { launched = true }
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 0) { manager.grantPass(any(), any()) }
        launched shouldBe false
        h.scope.cancel()
    }

    "GC-2: a live budget opens the chosen app once the grant has actually landed" {
        val h = harness(gatePrefs)
        coEvery { manager.grantPass(any(), any()) } coAnswers {
            h.prefsFlow.value = h.prefsFlow.value.copy(
                activePassPackage = firstArg(),
                activePassExpiresAt = System.currentTimeMillis() + 60_000L
            )
        }
        var launched = false

        h.viewModel.grantPassFor("com.instagram.android") { launched = true }
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) { manager.grantPass("com.instagram.android", capture(minutes)) }
        h.prefsFlow.value.activePassPackage shouldBe "com.instagram.android"
        launched shouldBe true
        h.scope.cancel()
    }

    "GC-3: a grant that did not take effect never launches the app" {
        val h = harness(gatePrefs)
        // The manager keeps its own guards (session ended, blank package): it returns without writing.
        coEvery { manager.grantPass(any(), any()) } returns Unit
        var launched = false

        h.viewModel.grantPassFor("com.instagram.android") { launched = true }
        testDispatcher.scheduler.runCurrent()

        launched shouldBe false
        h.scope.cancel()
    }

    "GC-4: a budget under a minute still asks for a positive number of minutes" {
        val h = harness(gatePrefs.copy(activePassExpiresAt = System.currentTimeMillis() + 30_000L))
        coEvery { manager.grantPass(any(), any()) } returns Unit

        h.viewModel.grantPassFor("com.instagram.android") {}
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) { manager.grantPass("com.instagram.android", capture(minutes)) }
        // grantPass refuses durationMinutes <= 0, so a truncated zero would silently kill the open.
        (minutes.captured >= 1) shouldBe true
        h.scope.cancel()
    }

    "GC-5: the gateway lists what the allowlist does not keep" {
        val h = harness(budgetPrefs(600L))
        coEvery { catalog.launchableApps() } returns listOf(
            entry("com.android.dialer"), entry("com.todoist"), entry("com.instagram.android"),
            entry("com.android.systemui"), entry("com.neuroflow.app")
        )
        h.subscribe(h.viewModel.hiddenApps)

        val listed = h.viewModel.hiddenApps.value.map { it.packageName }
        listed shouldContain "com.instagram.android"
        listed shouldNotContain "com.todoist"
        listed shouldNotContain "com.android.systemui"
        listed shouldNotContain "com.neuroflow.app"
        h.scope.cancel()
    }

    "GC-6: prefs churn re-filters the list, it does not re-enumerate the device" {
        val h = harness(budgetPrefs(600L))
        coEvery { catalog.launchableApps() } returns listOf(entry("com.instagram.android"))
        h.subscribe(h.viewModel.hiddenApps)

        repeat(25) { index -> h.prefsFlow.value = budgetPrefs(600L - index) }
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) { catalog.launchableApps() }
        coVerify(exactly = 1) { systemExempt.packages() }
        h.scope.cancel()
    }

    "GC-7: with no session there is nothing to hide" {
        val h = harness(budgetPrefs(600L).copy(isActive = false))
        coEvery { catalog.launchableApps() } returns listOf(entry("com.instagram.android"))
        h.subscribe(h.viewModel.hiddenApps)

        h.viewModel.hiddenApps.value.size shouldBe 0
        h.scope.cancel()
    }

    "GC-8: an unspent budget keeps ticking even though no app holds it (E1)" {
        val h = harness(budgetPrefs(300L))
        h.subscribe(h.viewModel.gateBudgetSecondsRemaining)

        val remaining = h.viewModel.gateBudgetSecondsRemaining.value
        (remaining != null && remaining in 295L..300L) shouldBe true
        h.scope.cancel()
    }

    "GC-9: a budget whose deadline has gone reads as no budget" {
        val h = harness(budgetPrefs(-30L))
        h.subscribe(h.viewModel.gateBudgetSecondsRemaining)

        h.viewModel.gateBudgetSecondsRemaining.value shouldBe null
        h.scope.cancel()
    }

    "GC-10: a correct answer past the floor logs the request and opens the gate" {
        val h = harness(gatePrefs)
        coEvery { catalog.labelFor("com.instagram.android") } returns "Instagram"
        h.viewModel.gateTask.value = transcribe

        h.viewModel.submitGateAnswer(
            answer = listOf("amber frost"),
            reason = "need the file for the report",
            mismatchCount = 0,
            elapsedMillis = 20_000L
        )
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) { gateRequestDao.insert(capture(row)) }
        row.captured.packageName shouldBe "com.instagram.android"
        row.captured.appLabel shouldBe "Instagram"
        row.captured.sessionId shouldBe "session-1"
        row.captured.taskType shouldBe GateTaskType.TRANSCRIBE.name
        row.captured.statedReason shouldBe "need the file for the report"
        row.captured.mismatchCount shouldBe 0
        row.captured.elapsedMillis shouldBe 20_000L
        row.captured.grantSource shouldBe GateGrantSource.REWARD_CODE.name
        h.viewModel.gatePassed.value shouldBe true
        h.scope.cancel()
    }

    "GC-11: three mismatches auto-pass with a wrong answer and are still logged" {
        val h = harness(gatePrefs)
        h.viewModel.gateTask.value = transcribe

        h.viewModel.submitGateAnswer(
            answer = listOf("wrong"), reason = "need the file", mismatchCount = 3, elapsedMillis = 20_000L
        )
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) { gateRequestDao.insert(capture(row)) }
        row.captured.mismatchCount shouldBe 3
        h.viewModel.gatePassed.value shouldBe true
        h.scope.cancel()
    }

    "GC-12: a wrong answer below the mismatch threshold is not a pass" {
        val h = harness(gatePrefs)
        h.viewModel.gateTask.value = transcribe

        h.viewModel.submitGateAnswer(
            answer = listOf("wrong"), reason = "need the file", mismatchCount = 1, elapsedMillis = 20_000L
        )
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 0) { gateRequestDao.insert(any()) }
        h.viewModel.gatePassed.value shouldBe false
        h.scope.cancel()
    }

    "GC-13: a correct answer under the twelve-second floor is not a pass either" {
        val h = harness(gatePrefs)
        h.viewModel.gateTask.value = transcribe

        h.viewModel.submitGateAnswer(
            answer = listOf("amber frost"), reason = "need the file", mismatchCount = 0, elapsedMillis = 5_000L
        )
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 0) { gateRequestDao.insert(any()) }
        h.viewModel.gatePassed.value shouldBe false
        h.scope.cancel()
    }

    "GC-14: a failed log write still opens the gate" {
        val h = harness(gatePrefs)
        coEvery { gateRequestDao.insert(any()) } throws IllegalStateException("disk full")
        h.viewModel.gateTask.value = transcribe

        h.viewModel.submitGateAnswer(
            answer = listOf("amber frost"), reason = "need the file", mismatchCount = 0, elapsedMillis = 20_000L
        )
        testDispatcher.scheduler.runCurrent()

        h.viewModel.gatePassed.value shouldBe true
        h.scope.cancel()
    }

    "GC-15: writing a request row also ages the table out" {
        val h = harness(gatePrefs)
        h.viewModel.gateTask.value = transcribe

        h.viewModel.submitGateAnswer(
            answer = listOf("amber frost"), reason = "need the file", mismatchCount = 0, elapsedMillis = 20_000L
        )
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) { gateRequestDao.deleteOlderThan(capture(cutoff)) }
        val expected = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
        (cutoff.captured in (expected - 5_000)..(expected + 5_000)) shouldBe true
        h.scope.cancel()
    }

    "GC-16: the emergency control logs the grant it made" {
        val h = harness(gatePrefs.copy(activePassPackage = null))
        coEvery { catalog.labelFor("com.instagram.android") } returns "Instagram"

        h.viewModel.grantEmergencyFor("com.instagram.android")
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) { manager.triggerEmergencyBypass("com.instagram.android") }
        coVerify(exactly = 1) { gateRequestDao.insert(capture(row)) }
        row.captured.packageName shouldBe "com.instagram.android"
        row.captured.appLabel shouldBe "Instagram"
        row.captured.taskType shouldBe "NONE"
        row.captured.mismatchCount shouldBe 0
        row.captured.grantSource shouldBe GateGrantSource.EMERGENCY.name
        h.scope.cancel()
    }

    "GC-17: an emergency that was already spent grants nothing, so nothing is logged" {
        val h = harness(gatePrefs.copy(emergencyUsed = true))

        h.viewModel.grantEmergencyFor("com.instagram.android")
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 0) { gateRequestDao.insert(any()) }
        h.scope.cancel()
    }

    "GC-18: opening the gate records which app it was opened for" {
        val h = harness(gatePrefs.copy(pendingGatePackage = null))

        h.viewModel.beginGate("com.netflix.mediaclient")
        testDispatcher.scheduler.runCurrent()
        h.prefsFlow.value.pendingGatePackage shouldBe "com.netflix.mediaclient"

        h.viewModel.beginGate("")
        testDispatcher.scheduler.runCurrent()
        h.prefsFlow.value.pendingGatePackage shouldBe null
        h.scope.cancel()
    }

    "GC-19: consuming the pass shuts the gate and re-rolls the task" {
        val h = harness(gatePrefs)
        h.viewModel.gateTask.value = transcribe
        h.viewModel.submitGateAnswer(
            answer = listOf("amber frost"), reason = "need the file", mismatchCount = 0, elapsedMillis = 20_000L
        )
        testDispatcher.scheduler.runCurrent()
        h.viewModel.gatePassed.value shouldBe true

        h.viewModel.consumeGatePass()

        h.viewModel.gatePassed.value shouldBe false
        // A fresh stimulus every visit: re-entering the gate must not show the text already typed.
        (h.viewModel.gateTask.value === transcribe) shouldBe false
        h.scope.cancel()
    }

    "GC-20: emergency is a live, task-based, not-yet-used session fact" {
        val h = harness(HyperFocusPreferences(isActive = true))
        h.subscribe(h.viewModel.emergencyAvailable)
        h.viewModel.emergencyAvailable.value shouldBe true

        h.prefsFlow.value = h.prefsFlow.value.copy(emergencyUsed = true)
        testDispatcher.scheduler.runCurrent()
        h.viewModel.emergencyAvailable.value shouldBe false

        h.prefsFlow.value = HyperFocusPreferences(
            isActive = true, sessionMode = HyperFocusSessionMode.TIME_BASED
        )
        testDispatcher.scheduler.runCurrent()
        h.viewModel.emergencyAvailable.value shouldBe false

        h.prefsFlow.value = HyperFocusPreferences(isActive = false)
        testDispatcher.scheduler.runCurrent()
        h.viewModel.emergencyAvailable.value shouldBe false
        h.scope.cancel()
    }

    "GC-21: the budget readout stays honest at both ends" {
        gatewayBudgetLabel(null) shouldBe
            "No budget yet. Enter a reward code, or use Emergency on the home screen."
        gatewayBudgetLabel(90L) shouldBe "Budget remaining: 1m 30s"
        gatewayBudgetLabel(60L) shouldBe "Budget remaining: 1m 0s"
        // FULL's deadline is Int.MAX_VALUE minutes away; a minute counter would read 35791394m.
        gatewayBudgetLabel(Int.MAX_VALUE.toLong() * 60L) shouldBe "Budget remaining: no timer on this pass"
    }
})
