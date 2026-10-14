package com.neuroflow.app.feature.hyperfocus.home

import android.content.Context
import com.neuroflow.app.feature.hyperfocus.data.AppCatalogRepository
import com.neuroflow.app.feature.hyperfocus.data.GateGrantSource
import com.neuroflow.app.feature.hyperfocus.data.GateRequestLogger
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusDataStore
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.feature.hyperfocus.gate.GateTaskEngine
import com.neuroflow.app.feature.hyperfocus.surface.SystemExempt
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
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
 * Ending a session lifts every block on the device at once, so it asks for the same attention the
 * gateway does. These tests pin both halves: the exit is refused without a real attempt, and it is
 * never actually locked — three mismatches still walk the user out, because a session the user
 * cannot end at all is a trap.
 *
 * Every test builds its own ViewModel and its own mocks. A shared `HyperFocusManager` mock makes
 * `exactly = 1` ambiguous: a call left over from the previous test lands in this one's verification.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HyperFocusEndGateTest : StringSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeTest {
        Dispatchers.setMain(testDispatcher)
    }

    afterTest {
        Dispatchers.resetMain()
    }

    class Fixture {
        val context = mockk<Context>(relaxed = true)
        val dataStore = mockk<HyperFocusDataStore>(relaxed = true)
        val manager = mockk<HyperFocusManager>(relaxed = true)
        val catalog = mockk<AppCatalogRepository>(relaxed = true)
        val systemExempt = mockk<SystemExempt>(relaxed = true)
        val gateRequestLogger = mockk<GateRequestLogger>(relaxed = true)
        val resolver = mockk<RealHomeResolver>(relaxed = true)

        init {
            every { context.packageName } returns "com.neuroflow.app"
            every { dataStore.flow } returns
                MutableStateFlow(HyperFocusPreferences(isActive = true))
            coEvery { dataStore.current() } returns HyperFocusPreferences(isActive = true)
            coEvery { catalog.launchableApps() } returns emptyList()
            coEvery { systemExempt.packages() } returns emptySet()
        }

        val viewModel = HyperFocusHomeViewModel(
            context = context,
            hyperFocusDataStore = dataStore,
            hyperFocusManager = manager,
            appCatalogRepository = catalog,
            systemExempt = systemExempt,
            gateRequestLogger = gateRequestLogger,
            realHomeResolver = resolver
        )
    }

    val ready = GateTaskEngine.MIN_ELAPSED_MILLIS
    val reason = "Meeting finished early"

    "EG-1: a correct attempt ends the session and writes the audit row" {
        val fixture = Fixture()
        val answer = fixture.viewModel.endGateTask.value.expected

        fixture.viewModel.submitEndAttempt(answer, reason, mismatchCount = 0, elapsedMillis = ready) shouldBe true
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) {
            fixture.gateRequestLogger.log(
                packageName = "",
                statedReason = reason,
                taskType = any<String>(),
                mismatchCount = 0,
                elapsedMillis = ready,
                grantSource = GateGrantSource.ABANDONED
            )
        }
        coVerify(exactly = 1) { fixture.manager.deactivate() }
    }

    "EG-2: a wrong answer with no mismatches yet is refused" {
        val fixture = Fixture()

        fixture.viewModel.submitEndAttempt(
            fixture.viewModel.endGateTask.value.expected.map { "nonsense" },
            reason,
            mismatchCount = 0,
            elapsedMillis = ready
        ) shouldBe false
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 0) { fixture.manager.deactivate() }
    }

    "EG-3: no written reason means no exit, however correct the answer" {
        val fixture = Fixture()
        val answer = fixture.viewModel.endGateTask.value.expected

        fixture.viewModel.submitEndAttempt(answer, "short", mismatchCount = 0, elapsedMillis = ready) shouldBe false
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 0) { fixture.manager.deactivate() }
    }

    "EG-4: the attention floor holds even on a perfect answer" {
        val fixture = Fixture()
        val answer = fixture.viewModel.endGateTask.value.expected

        fixture.viewModel.submitEndAttempt(answer, reason, mismatchCount = 0, elapsedMillis = ready - 1) shouldBe false
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 0) { fixture.manager.deactivate() }
    }

    "EG-5: three mismatches still let the user out" {
        val fixture = Fixture()

        fixture.viewModel.submitEndAttempt(
            fixture.viewModel.endGateTask.value.expected.map { "nonsense" },
            reason,
            mismatchCount = GateTaskEngine.MAX_MISMATCHES,
            elapsedMillis = ready
        ) shouldBe true
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) { fixture.manager.deactivate() }
    }
})
