package com.neuroflow.app.feature.hyperfocus.data

import android.content.ComponentName
import android.os.UserHandle
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest

class AllowlistMigrationRunnerTest : StringSpec({

    val hyperFocusDataStore = mockk<HyperFocusDataStore>()
    val appCatalogRepository = mockk<AppCatalogRepository>()
    val legacyLauncherPrefs = mockk<LegacyLauncherPrefs>()
    val runner = AllowlistMigrationRunner(hyperFocusDataStore, appCatalogRepository, legacyLauncherPrefs)

    beforeTest {
        clearAllMocks()
        coEvery { hyperFocusDataStore.update(any()) } just Runs
        coEvery { legacyLauncherPrefs.distractionScoresSnapshot() } returns emptyMap()
    }

    fun catalog(vararg packages: String) = packages.map {
        AppCatalogRepository.AppEntry(
            it, it, mockk<ComponentName>(relaxed = true), mockk<UserHandle>(relaxed = true)
        )
    }

    fun run(
        prefs: HyperFocusPreferences,
        launchable: List<AppCatalogRepository.AppEntry> = catalog("com.todoist"),
        legacyScores: Map<String, Int> = emptyMap()
    ): HyperFocusPreferences {
        coEvery { hyperFocusDataStore.current() } returns prefs
        coEvery { appCatalogRepository.launchableApps() } returns launchable
        coEvery { legacyLauncherPrefs.distractionScoresSnapshot() } returns legacyScores
        val transform = slot<(HyperFocusPreferences) -> HyperFocusPreferences>()

        runTest { runner.runIfNeeded() }

        coVerify(exactly = 1) { hyperFocusDataStore.update(capture(transform)) }
        return transform.captured(prefs)
    }

    "MR-1: an unmigrated install gets the complement of its denylist and the stamp" {
        val prefs = HyperFocusPreferences(blockedPackages = setOf("com.instagram.android"))

        val updated = run(
            prefs,
            launchable = catalog("com.android.dialer", "com.instagram.android", "com.todoist")
        )

        updated.essentialPackages shouldContain "com.android.dialer"
        updated.allowedPackages shouldContain "com.todoist"
        updated.allowedPackages shouldNotContain "com.instagram.android"
        // The denylist still drives enforcement until Task 12 flips it — the migration must not clear it.
        updated.blockedPackages shouldBe setOf("com.instagram.android")
        updated.allowlistMigratedAt.shouldNotBeNull()
    }

    "MR-2: an already migrated install reads nothing and writes nothing" {
        coEvery { hyperFocusDataStore.current() } returns HyperFocusPreferences(
            essentialPackages = setOf("com.android.dialer"),
            allowlistMigratedAt = 1_000L
        )

        runTest { runner.runIfNeeded() }

        coVerify(exactly = 0) { appCatalogRepository.launchableApps() }
        coVerify(exactly = 0) { hyperFocusDataStore.update(any()) }
    }

    "MR-3: a live session is never rewritten underneath" {
        coEvery { hyperFocusDataStore.current() } returns HyperFocusPreferences(
            isActive = true,
            blockedPackages = setOf("com.instagram.android")
        )

        runTest { runner.runIfNeeded() }

        coVerify(exactly = 0) { hyperFocusDataStore.update(any()) }
    }

    "MR-4: an empty catalog is a failed query, so the install stays unmigrated and retries" {
        coEvery { hyperFocusDataStore.current() } returns HyperFocusPreferences(
            blockedPackages = setOf("com.instagram.android")
        )
        coEvery { appCatalogRepository.launchableApps() } returns emptyList()

        runTest { runner.runIfNeeded() }

        coVerify(exactly = 0) { hyperFocusDataStore.update(any()) }
    }

    "MR-5: legacy scores are copied into an empty map" {
        val updated = run(HyperFocusPreferences(), legacyScores = mapOf("com.instagram.android" to 82))

        updated.distractionScores shouldBe mapOf("com.instagram.android" to 82)
    }

    "MR-6: scores the user already wrote are never overwritten by the migration" {
        val written = mapOf("com.netflix.mediaclient" to 12)
        val updated = run(
            HyperFocusPreferences(distractionScores = written),
            legacyScores = mapOf("com.instagram.android" to 82)
        )

        updated.distractionScores shouldBe written
    }

    "MR-7: a call-path app exempt from enforcement still lands in a tier" {
        val updated = run(
            HyperFocusPreferences(),
            launchable = catalog("com.google.android.dialer", "com.todoist")
        )

        updated.allowedPackages shouldContain "com.google.android.dialer"
    }
})
