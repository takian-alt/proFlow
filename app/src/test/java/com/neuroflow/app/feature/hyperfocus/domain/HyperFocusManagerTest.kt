package com.neuroflow.app.feature.hyperfocus.domain

import android.content.Context
import android.content.Intent
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusDataStore
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import com.neuroflow.app.feature.hyperfocus.data.UnlockCodeEntity
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import kotlinx.coroutines.test.runTest

/**
 * Unit tests for HyperFocusManagerImpl.
 *
 * Validates: Requirements 2.4, 2.5
 *
 * Tests:
 * - submitCode with lockout active returns UnlockResult.Lockout without DB writes
 * - submitCode with valid code returns UnlockResult.Success and resets wrongCodeAttempts
 * - submitCode with 3 wrong codes sets lockoutExpiresAt and resets attempts to 0
 * - onTaskCompleted when inactive is a no-op
 * - activate() calls generateCodePool() before writing isActive = true
 * - grantPass / clearActivePass / expirePassIfDue move the per-app pass and its budget
 */
class HyperFocusManagerTest : StringSpec({

    val context = mockk<Context>(relaxed = true)
    val hyperFocusDataStore = mockk<HyperFocusDataStore>()
    val unlockCodeRepository = mockk<UnlockCodeRepository>()
    val taskRepository = mockk<TaskRepository>()

    beforeTest {
        clearAllMocks()
        coEvery { hyperFocusDataStore.update(any()) } just Runs
        coEvery { hyperFocusDataStore.updateHeartbeat(any()) } just Runs
        coEvery { unlockCodeRepository.generateCodePool(any()) } just Runs
        coEvery { unlockCodeRepository.deleteExpiredCodes() } just Runs
        coEvery { unlockCodeRepository.deleteSessionCodes(any()) } just Runs
        coEvery { taskRepository.getAllTasks() } returns emptyList()
    }

    val manager by lazy {
        HyperFocusManagerImpl(
            context, hyperFocusDataStore, unlockCodeRepository, taskRepository,
            // Both are inert here: resyncSuspension stops at the device-owner guard, which a
            // relaxed Context answers false, so neither mock needs behaviour.
            mockk<com.neuroflow.app.feature.hyperfocus.data.AppCatalogRepository>(relaxed = true),
            mockk<com.neuroflow.app.feature.hyperfocus.surface.SystemExempt>(relaxed = true)
        )
    }

    "submitCode with lockout active returns UnlockResult.Lockout without DB writes" {
        runTest {
            val lockoutExpiresAt = System.currentTimeMillis() + 300_000L
            val prefs = HyperFocusPreferences(
                isActive = true,
                lockoutExpiresAt = lockoutExpiresAt
            )
            coEvery { hyperFocusDataStore.current() } returns prefs

            val result = manager.submitCode("ABCDEF")

            result.shouldBeInstanceOf<UnlockResult.Lockout>()
            coVerify(exactly = 0) { unlockCodeRepository.validateAndClaim(any(), any()) }
            coVerify(exactly = 0) { hyperFocusDataStore.update(any()) }
        }
    }

    "submitCode with valid code returns UnlockResult.Success and resets wrongCodeAttempts" {
        runTest {
            val prefs = HyperFocusPreferences(
                isActive = true,
                lockoutExpiresAt = null,
                wrongCodeAttempts = 1,
                sessionId = "session1",
                // An earlier grant is still live when this code is spent.
                activePassPackage = "com.instagram.android",
                activePassExpiresAt = System.currentTimeMillis() + 300_000L
            )
            coEvery { hyperFocusDataStore.current() } returns prefs

            val matchedEntity = UnlockCodeEntity(
                id = "code-id-1",
                encryptedCode = "encrypted",
                tier = RewardTier.MICRO,
                sessionId = "session1"
            )
            coEvery { unlockCodeRepository.validateAndClaim("session1", "ABCDEF") } returns matchedEntity
            coEvery { unlockCodeRepository.markUsed(any(), any()) } just Runs

            val result = manager.submitCode("ABCDEF")

            result.shouldBeInstanceOf<UnlockResult.Success>()
            val updateSlot = slot<(HyperFocusPreferences) -> HyperFocusPreferences>()
            coVerify { hyperFocusDataStore.update(capture(updateSlot)) }
            // D3: a code buys a budget, never access to a specific app.
            updateSlot.captured(prefs).activePassPackage shouldBe null
        }
    }

    "submitCode with 3 wrong codes sets lockoutExpiresAt and resets attempts to 0" {
        runTest {
            val prefs = HyperFocusPreferences(
                isActive = true,
                lockoutExpiresAt = null,
                wrongCodeAttempts = 2,
                sessionId = "session1"
            )
            coEvery { hyperFocusDataStore.current() } returns prefs
            coEvery { unlockCodeRepository.validateAndClaim(any(), any()) } returns null

            val result = manager.submitCode("WRONG1")

            result shouldBe UnlockResult.InvalidCode

            val updateSlot = slot<(HyperFocusPreferences) -> HyperFocusPreferences>()
            coVerify { hyperFocusDataStore.update(capture(updateSlot)) }

            val updated = updateSlot.captured(prefs)
            (updated.lockoutExpiresAt != null) shouldBe true
            updated.wrongCodeAttempts shouldBe 0
        }
    }

    "onTaskCompleted when inactive is a no-op" {
        runTest {
            val prefs = HyperFocusPreferences(isActive = false)
            coEvery { hyperFocusDataStore.current() } returns prefs

            manager.onTaskCompleted()

            coVerify(exactly = 0) { taskRepository.getAllTasks() }
        }
    }

    "activate calls generateCodePool before writing isActive = true" {
        runTest {
            coEvery { hyperFocusDataStore.current() } returns HyperFocusPreferences()
            coEvery { taskRepository.getAllTasks() } returns emptyList()

            manager.activate(setOf("com.example.app"), 5, emptySet())

            coVerifyOrder {
                unlockCodeRepository.generateCodePool(any())
                hyperFocusDataStore.update(any())
            }
        }
    }

    "reportTamper sets tamper fields in data store" {
        runTest {
            val prefs = HyperFocusPreferences(isActive = true)
            coEvery { hyperFocusDataStore.current() } returns prefs

            manager.reportTamper("Test tamper event")

            val updateSlot = slot<(HyperFocusPreferences) -> HyperFocusPreferences>()
            coVerify { hyperFocusDataStore.update(capture(updateSlot)) }
            val updated = updateSlot.captured(prefs)

            updated.isTamperDetected shouldBe true
            updated.tamperReason shouldBe "Test tamper event"
            updated.tamperDetectedAt shouldNotBe null
        }
    }

    "completePlanning deactivates session so a new one can be started" {
        runTest {
            val prefs = HyperFocusPreferences(
                isActive = true,
                sessionId = "session1"
            )
            coEvery { hyperFocusDataStore.current() } returns prefs

            manager.completePlanning()

            val updateSlot = slot<(HyperFocusPreferences) -> HyperFocusPreferences>()
            coVerify { hyperFocusDataStore.update(capture(updateSlot)) }

            val updated = updateSlot.captured(prefs)
            updated.isActive shouldBe false
        }
    }

    "triggerEmergencyBypass grants one-time 10 minute pass for one app and marks emergencyUsed" {
        runTest {
            val prefs = HyperFocusPreferences(
                isActive = true,
                emergencyUsed = false
            )
            coEvery { hyperFocusDataStore.current() } returns prefs

            val before = System.currentTimeMillis()
            manager.triggerEmergencyBypass("com.instagram.android")
            val after = System.currentTimeMillis()

            val updateSlot = slot<(HyperFocusPreferences) -> HyperFocusPreferences>()
            coVerify(exactly = 1) { hyperFocusDataStore.update(capture(updateSlot)) }
            val updated = updateSlot.captured(prefs)

            updated.emergencyUsed shouldBe true
            updated.currentTier shouldBe RewardTier.NONE

            updated.activePassPackage shouldBe "com.instagram.android"
            updated.activePassExpiresAt shouldNotBe null
            val expiresAt = updated.activePassExpiresAt!!
            val minExpected = before + 10 * 60 * 1000L
            val maxExpected = after + 10 * 60 * 1000L
            (expiresAt >= minExpected) shouldBe true
            (expiresAt <= maxExpected) shouldBe true

            verify(exactly = 1) { context.startForegroundService(any<Intent>()) }
        }
    }

    "triggerEmergencyBypass is blocked after emergency already used" {
        runTest {
            val prefs = HyperFocusPreferences(
                isActive = true,
                emergencyUsed = true
            )
            coEvery { hyperFocusDataStore.current() } returns prefs

            manager.triggerEmergencyBypass("com.instagram.android")

            coVerify(exactly = 0) { hyperFocusDataStore.update(any()) }
            verify(exactly = 0) { context.startForegroundService(any<Intent>()) }
        }
    }

    "grantPass spends a live budget on one package and inherits its deadline" {
        runTest {
            val liveDeadline = System.currentTimeMillis() + 300_000L
            val prefs = HyperFocusPreferences(
                isActive = true,
                activePassPackage = null,
                activePassExpiresAt = liveDeadline
            )
            coEvery { hyperFocusDataStore.current() } returns prefs

            manager.grantPass("com.instagram.android", 10)

            val updateSlot = slot<(HyperFocusPreferences) -> HyperFocusPreferences>()
            coVerify(exactly = 1) { hyperFocusDataStore.update(capture(updateSlot)) }
            val updated = updateSlot.captured(prefs)
            updated.activePassPackage shouldBe "com.instagram.android"
            updated.activePassExpiresAt shouldBe liveDeadline
            verify(exactly = 1) { context.startForegroundService(any<Intent>()) }
        }
    }

    "grantPass is refused with no session, and writes nothing" {
        runTest {
            coEvery { hyperFocusDataStore.current() } returns HyperFocusPreferences(isActive = false)

            manager.grantPass("com.instagram.android", 10)

            coVerify(exactly = 0) { hyperFocusDataStore.update(any()) }
            verify(exactly = 0) { context.startForegroundService(any<Intent>()) }
        }
    }

    "expirePassIfDue clears package and budget once the budget is spent" {
        runTest {
            val spent = System.currentTimeMillis() - 1L
            val prefs = HyperFocusPreferences(
                isActive = true,
                activePassPackage = "com.instagram.android",
                activePassExpiresAt = spent
            )

            manager.expirePassIfDue(System.currentTimeMillis())

            val updateSlot = slot<(HyperFocusPreferences) -> HyperFocusPreferences>()
            coVerify(exactly = 1) { hyperFocusDataStore.update(capture(updateSlot)) }
            val updated = updateSlot.captured(prefs)
            updated.activePassPackage shouldBe null
            updated.activePassExpiresAt shouldBe null
        }
    }

    "expirePassIfDue leaves a live pass untouched" {
        runTest {
            val prefs = HyperFocusPreferences(
                isActive = true,
                activePassPackage = "com.instagram.android",
                activePassExpiresAt = System.currentTimeMillis() + 60_000L
            )

            manager.expirePassIfDue(System.currentTimeMillis())

            val updateSlot = slot<(HyperFocusPreferences) -> HyperFocusPreferences>()
            coVerify(exactly = 1) { hyperFocusDataStore.update(capture(updateSlot)) }
            updateSlot.captured(prefs) shouldBe prefs
        }
    }

    "expirePassIfDue keeps a budget that has no package" {
        runTest {
            val prefs = HyperFocusPreferences(
                isActive = true,
                activePassPackage = null,
                activePassExpiresAt = System.currentTimeMillis() + 60_000L
            )

            manager.expirePassIfDue(System.currentTimeMillis())

            val updateSlot = slot<(HyperFocusPreferences) -> HyperFocusPreferences>()
            coVerify(exactly = 1) { hyperFocusDataStore.update(capture(updateSlot)) }
            updateSlot.captured(prefs) shouldBe prefs
        }
    }

    "clearActivePass closes the app but keeps the budget ticking (E1)" {
        runTest {
            val deadline = System.currentTimeMillis() + 300_000L
            val prefs = HyperFocusPreferences(
                isActive = true,
                activePassPackage = "com.instagram.android",
                activePassExpiresAt = deadline
            )

            manager.clearActivePass()

            val updateSlot = slot<(HyperFocusPreferences) -> HyperFocusPreferences>()
            coVerify(exactly = 1) { hyperFocusDataStore.update(capture(updateSlot)) }
            val updated = updateSlot.captured(prefs)
            updated.activePassPackage shouldBe null
            updated.activePassExpiresAt shouldBe deadline
        }
    }

    "updateHeartbeat records timestamp to data store without full state update" {
        runTest {
            manager.updateHeartbeat()

            coVerify(exactly = 1) { hyperFocusDataStore.updateHeartbeat(any()) }
            coVerify(exactly = 0) { hyperFocusDataStore.update(any()) }
        }
    }
})

