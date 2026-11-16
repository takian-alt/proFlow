package com.neuroflow.app.feature.hyperfocus

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.HyperFocusSessionMode
import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.feature.hyperfocus.data.AllowlistMigrationRunner
import com.neuroflow.app.feature.hyperfocus.data.AppCatalogRepository
import com.neuroflow.app.feature.hyperfocus.data.GateRequestLogger
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusDataStore
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import com.neuroflow.app.feature.hyperfocus.data.UnlockCodeEntity
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.feature.hyperfocus.domain.UnlockCodeRepository
import com.neuroflow.app.feature.hyperfocus.surface.SystemExempt
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HyperFocusClaimRewardTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `claimRewardForTier persists pendingCodeId updates rewardCounts and reuses unused pending code`() = runTest(testDispatcher) {
        val initialPrefs = HyperFocusPreferences(
            isActive = true,
            sessionId = "hf-sess-1",
            sessionMode = HyperFocusSessionMode.TASK_BASED,
            dailyTaskTarget = 4,
            lockedTaskIds = setOf("t1", "t2", "t3", "t4")
        )
        val prefsFlow = MutableStateFlow(initialPrefs)
        val dataStore = mockk<HyperFocusDataStore>(relaxed = true).apply {
            every { flow } returns prefsFlow
            coEvery { current() } answers { prefsFlow.value }
            coEvery { update(any()) } coAnswers {
                val transform = firstArg<(HyperFocusPreferences) -> HyperFocusPreferences>()
                prefsFlow.value = transform(prefsFlow.value)
            }
        }

        val completedTasks = MutableStateFlow(
            listOf(TaskEntity(id = "t1", title = "Completed 1", status = TaskStatus.COMPLETED))
        )
        val taskRepo = mockk<TaskRepository>(relaxed = true).apply {
            every { observeCompletedTasks() } returns completedTasks
            every { observeActiveTasks() } returns MutableStateFlow(emptyList())
        }

        val unlockRepo = mockk<UnlockCodeRepository>(relaxed = true)
        coEvery { unlockRepo.getClaimableCode("hf-sess-1", RewardTier.MICRO) } returns ("code-micro-1" to "AB34CD")
        coEvery {
            unlockRepo.getCodeEntityById("code-micro-1")
        } returns UnlockCodeEntity(
            id = "code-micro-1",
            encryptedCode = "enc",
            tier = RewardTier.MICRO,
            sessionId = "hf-sess-1",
            isUsed = false
        )
        coEvery { unlockRepo.getCodeById("code-micro-1") } returns "AB34CD"
        coEvery { unlockRepo.countUnusedByTier("hf-sess-1", RewardTier.MICRO) } returns 1
        coEvery { unlockRepo.countUnusedByTier("hf-sess-1", RewardTier.PARTIAL) } returns 1
        coEvery { unlockRepo.countUnusedByTier("hf-sess-1", RewardTier.EARNED) } returns 1
        coEvery { unlockRepo.countUnusedByTier("hf-sess-1", RewardTier.FULL) } returns 1

        val vm = HyperFocusViewModel(
            hyperFocusManager = mockk<HyperFocusManager>(relaxed = true),
            hyperFocusDataStore = dataStore,
            unlockCodeRepository = unlockRepo,
            taskRepository = taskRepo,
            gateRequestLogger = mockk<GateRequestLogger>(relaxed = true),
            appCatalogRepository = mockk<AppCatalogRepository>(relaxed = true),
            systemExempt = mockk<SystemExempt>(relaxed = true),
            allowlistMigrationRunner = mockk<AllowlistMigrationRunner>(relaxed = true)
        )

        val collectJob = backgroundScope.launch { vm.progress.collect {} }
        testScheduler.runCurrent()
        assertEquals(1, vm.progress.value.completedSinceActivation)

        // First claim issues and persists pendingCodeId
        vm.claimRewardForTier(RewardTier.MICRO)
        testScheduler.runCurrent()

        assertEquals("code-micro-1", prefsFlow.value.pendingCodeId)
        assertEquals(RewardTier.MICRO, vm.claimedCodeTier.value)
        assertEquals("AB34CD", vm.claimedCodeToShow.value)
        assertEquals(1, vm.rewardCounts.value[RewardTier.MICRO])
        coVerify(exactly = 1) { unlockRepo.getClaimableCode("hf-sess-1", RewardTier.MICRO) }

        // Dismiss and re-claim same tier while code is still unused -> reuses existing code
        vm.dismissClaimedCode()
        vm.claimRewardForTier(RewardTier.MICRO)
        testScheduler.runCurrent()

        assertEquals(RewardTier.MICRO, vm.claimedCodeTier.value)
        assertEquals("AB34CD", vm.claimedCodeToShow.value)
        coVerify(exactly = 1) { unlockRepo.getClaimableCode("hf-sess-1", RewardTier.MICRO) }

        collectJob.cancel()
    }
}
