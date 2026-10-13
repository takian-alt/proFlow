package com.neuroflow.app.feature.hyperfocus.domain

import android.content.ComponentName
import android.content.Context
import android.os.UserHandle
import com.neuroflow.app.BuildConfig
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.feature.hyperfocus.data.AppCatalogRepository
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusDataStore
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import com.neuroflow.app.feature.hyperfocus.surface.SystemExempt
import com.neuroflow.app.kiosk.DeviceOwnerKioskManager
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest

/**
 * Under an allowlist the suspended set is "everything launchable that is not kept", so the manager
 * computes both sets and DeviceOwnerKioskManager only applies them. These tests pin the set math
 * and the ordering the set math depends on; the DevicePolicyManager call itself is device-owner
 * only and is mocked at the object level.
 */
class SuspensionResyncTest : StringSpec({

    val context = mockk<Context>(relaxed = true)
    val dataStore = mockk<HyperFocusDataStore>()
    val unlockCodeRepository = mockk<UnlockCodeRepository>()
    val taskRepository = mockk<TaskRepository>()
    val catalog = mockk<AppCatalogRepository>()
    val systemExempt = mockk<SystemExempt>()

    val keepSet = slot<Set<String>>()
    val launchableSet = slot<Set<String>>()
    val enabledFlag = slot<Boolean>()

    val allPackages = setOf(
        "com.android.dialer", "com.todoist", "com.instagram.android", "com.netflix.mediaclient"
    )

    var store = HyperFocusPreferences()

    fun catalogEntries(packages: Set<String>) = packages.map {
        AppCatalogRepository.AppEntry(
            it, it, mockk<ComponentName>(relaxed = true), mockk<UserHandle>(relaxed = true)
        )
    }

    val manager by lazy {
        HyperFocusManagerImpl(
            context, dataStore, unlockCodeRepository, taskRepository, catalog, systemExempt
        )
    }

    beforeTest {
        clearAllMocks()
        store = HyperFocusPreferences(
            isActive = true,
            essentialPackages = setOf("com.android.dialer"),
            allowedPackages = setOf("com.todoist")
        )
        coEvery { dataStore.current() } answers { store }
        coEvery { dataStore.update(any()) } coAnswers {
            store = firstArg<(HyperFocusPreferences) -> HyperFocusPreferences>()(store)
        }
        coEvery { dataStore.updateHeartbeat(any()) } just Runs
        coEvery { unlockCodeRepository.deleteExpiredCodes() } just Runs
        coEvery { unlockCodeRepository.generateCodePool(any()) } just Runs
        coEvery { unlockCodeRepository.deleteSessionCodes(any()) } just Runs
        coEvery { taskRepository.getAllTasks() } returns emptyList()
        every { systemExempt.packages() } returns setOf("android", "com.android.systemui")
        coEvery { catalog.launchableApps() } returns catalogEntries(allPackages)

        mockkObject(DeviceOwnerKioskManager)
        every { DeviceOwnerKioskManager.isDeviceOwner(any()) } returns true
        every { DeviceOwnerKioskManager.isStrictKioskEnforcementEnabled(any()) } returns true
        every { DeviceOwnerKioskManager.enableHybridProtection(any()) } just Runs
        every { DeviceOwnerKioskManager.bringLauncherToFront(any()) } just Runs
        every { DeviceOwnerKioskManager.setHyperFocusSelfProtection(any(), any()) } returns true
        every {
            DeviceOwnerKioskManager.syncAllowlistSuspension(
                any(), capture(keepSet), capture(launchableSet), capture(enabledFlag)
            )
        } returns true
    }

    afterAny { unmockkObject(DeviceOwnerKioskManager) }

    "RS-1: the suspension call receives the keep-set and the launchable set, not a denylist" {
        runTest { manager.resyncSuspension(true) }

        keepSet.captured shouldContainAll setOf(
            "com.android.dialer", "com.todoist", "android", "com.android.systemui",
            BuildConfig.APPLICATION_ID
        )
        keepSet.captured shouldNotContain "com.instagram.android"
        keepSet.captured shouldNotContain "com.netflix.mediaclient"
        launchableSet.captured shouldBe allPackages
        enabledFlag.captured shouldBe true
    }

    "RS-2: a live pass keeps the granted app off the suspension list" {
        store = store.copy(
            activePassPackage = "com.instagram.android",
            activePassExpiresAt = System.currentTimeMillis() + 60_000L
        )

        runTest { manager.resyncSuspension(true) }

        keepSet.captured shouldContain "com.instagram.android"
    }

    "RS-3: a spent pass does not keep the app" {
        store = store.copy(
            activePassPackage = "com.instagram.android",
            activePassExpiresAt = System.currentTimeMillis() - 1L
        )

        runTest { manager.resyncSuspension(true) }

        keepSet.captured shouldNotContain "com.instagram.android"
    }

    "RS-4: lifting enforcement still sends real sets, so the disable path can repair them" {
        runTest { manager.resyncSuspension(false) }

        enabledFlag.captured shouldBe false
        launchableSet.captured shouldBe allPackages
    }

    "RS-5: without device owner nothing is enumerated and nothing is called" {
        every { DeviceOwnerKioskManager.isDeviceOwner(any()) } returns false

        runTest { manager.resyncSuspension(true) }

        coVerify(exactly = 0) { catalog.launchableApps() }
        coVerify(exactly = 0) {
            DeviceOwnerKioskManager.syncAllowlistSuspension(any(), any(), any(), any())
        }
    }

    "RS-6: activation computes the keep-set after the session preferences are written" {
        store = HyperFocusPreferences(
            essentialPackages = setOf("com.android.dialer"),
            allowedPackages = setOf("com.todoist"),
            activePassPackage = "com.instagram.android",
            activePassExpiresAt = System.currentTimeMillis() + 60_000L
        )

        runTest { manager.activate(emptySet(), 0, emptySet()) }

        // afterSession() drops the previous session's pass, so a keep-set read before the update
        // would still spare com.instagram.android.
        keepSet.captured shouldNotContain "com.instagram.android"
        coVerifyOrder {
            dataStore.update(any())
            DeviceOwnerKioskManager.syncAllowlistSuspension(any(), any(), any(), any())
        }
    }

    "RS-7: an active session in lenient mode asks for no suspension" {
        every { DeviceOwnerKioskManager.isStrictKioskEnforcementEnabled(any()) } returns false

        runTest { manager.resyncSuspension(true) }

        enabledFlag.captured shouldBe false
    }
})
