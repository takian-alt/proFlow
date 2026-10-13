package com.neuroflow.app.kiosk

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.SharedPreferences
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

/**
 * syncAllowlistSuspension receives the keep-set and the launchable set and suspends what lies
 * between them. These tests pin the direction of that subtraction, the exclusion of our own
 * package, and the tracked-set bookkeeping that lets a later call release the apps this one froze.
 */
class DeviceOwnerSuspensionSyncTest : StringSpec({

    val SELF = "com.neuroflow.app"
    val context = mockk<Context>()
    val dpm = mockk<DevicePolicyManager>()
    val store = mockk<SharedPreferences>()
    val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    val written = slot<Set<String>>()

    var trackedOnDisk: Set<String> = emptySet()
    var refused: Array<String> = emptyArray()
    val suspended = mutableListOf<List<String>>()
    val released = mutableListOf<List<String>>()

    beforeTest {
        clearAllMocks()
        trackedOnDisk = emptySet()
        refused = emptyArray()
        suspended.clear()
        released.clear()
        every { context.packageName } returns SELF
        every { context.getSystemService(DevicePolicyManager::class.java) } returns dpm
        every { context.getSharedPreferences(any(), any()) } returns store
        every { store.getStringSet(any(), any()) } answers { trackedOnDisk }
        every { store.edit() } returns editor
        every { editor.putStringSet(any(), capture(written)) } returns editor
        every { dpm.isDeviceOwnerApp(any()) } returns true
        every { dpm.setPackagesSuspended(any(), any(), any()) } answers {
            val packages = secondArg<Array<String>>().toList()
            if (thirdArg()) suspended += packages else released += packages
            refused
        }
    }

    fun sync(keepSet: Set<String>, launchable: Set<String>, enabled: Boolean) =
        DeviceOwnerKioskManager.syncAllowlistSuspension(context, keepSet, launchable, enabled)

    "KS-1: it suspends launchable minus keepSet, not keepSet minus launchable" {
        val result = sync(
            keepSet = setOf("com.android.dialer", "com.todoist", "android", SELF),
            launchable = setOf("com.android.dialer", "com.todoist", "com.instagram.android", "com.netflix.mediaclient", SELF),
            enabled = true
        )

        result shouldBe true
        suspended.single().toSet() shouldContainExactly setOf("com.instagram.android", "com.netflix.mediaclient")
        released.isEmpty() shouldBe true
    }

    "KS-2: our own package is never suspended, even when the catalog lists it outside the keep-set" {
        sync(
            keepSet = setOf("com.todoist"),
            launchable = setOf(SELF, "com.todoist", " ", "com.instagram.android"),
            enabled = true
        )

        val requested = suspended.single().toSet()
        requested shouldContain "com.instagram.android"
        (SELF in requested) shouldBe false
        ("" in requested) shouldBe false
    }

    "KS-3: packages the platform refused are left out of the tracked set" {
        refused = arrayOf("com.netflix.mediaclient")

        sync(
            keepSet = setOf("com.todoist", SELF),
            launchable = setOf("com.instagram.android", "com.netflix.mediaclient", "com.todoist", SELF),
            enabled = true
        )

        // Tracking a refusal as a suspension would let the next deactivation believe it had
        // released an app that is still frozen.
        written.captured shouldContainExactly setOf("com.instagram.android")
    }

    "KS-4: disabling releases the tracked set plus the recomputed complement" {
        trackedOnDisk = setOf("com.whatsapp")

        sync(
            keepSet = setOf("com.todoist", SELF),
            launchable = setOf("com.instagram.android", "com.todoist", SELF),
            enabled = false
        )

        released.single().toSet() shouldContainExactly setOf("com.whatsapp", "com.instagram.android")
    }

    "KS-5: enabling releases tracked apps that the new allowlist covers" {
        trackedOnDisk = setOf("com.netflix.mediaclient")

        sync(
            keepSet = setOf("com.netflix.mediaclient", "com.todoist", SELF),
            launchable = setOf("com.netflix.mediaclient", "com.instagram.android", "com.todoist", SELF),
            enabled = true
        )

        released.single().toSet() shouldContainExactly setOf("com.netflix.mediaclient")
        suspended.single().toSet() shouldContainExactly setOf("com.instagram.android")
        written.captured shouldContainExactly setOf("com.instagram.android")
    }

    "KS-6: without device owner nothing is touched" {
        every { dpm.isDeviceOwnerApp(any()) } returns false

        val result = sync(
            keepSet = setOf("com.todoist", SELF),
            launchable = setOf("com.instagram.android", "com.todoist", SELF),
            enabled = true
        )

        result shouldBe false
        suspended.isEmpty() shouldBe true
        released.isEmpty() shouldBe true
        verify(exactly = 0) { store.edit() }
    }
})
