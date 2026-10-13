package com.neuroflow.app.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Baseline Profile Generator for the HyperFocus black home
 *
 * Causes ART to AOT-compile the code the home screen runs on its first frame, so the grid composes
 * without JIT warm-up jank.
 *
 * Critical paths covered:
 * - HyperFocusHomeActivity cold start
 * - Essentials grid composition and scroll
 * - Per-tile icon resolution through PackageManager
 *
 * The launcher surfaces this profile used to cover (app drawer, dock, quick-stats overlay, task card)
 * were archived at tag archive/launcher-2026-10-12, so they are gone rather than renamed: the swipes
 * below act on the surviving grid.
 *
 * To generate the profile:
 * ./gradlew :app:generateBaselineProfile
 *
 * The generated baseline-prof.txt will be placed in app/src/main/
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HyperFocusBaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() {
        baselineProfileRule.collect(
            packageName = "com.neuroflow.app",
            maxIterations = 5,
            stableIterations = 3
        ) {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

            // Ensure we're starting from home
            device.pressHome()
            device.waitForIdle()

            // Critical Path 1: HyperFocusHomeActivity cold start
            startActivityAndWait()
            device.waitForIdle(2000)

            // Critical Path 2: the essentials grid is on screen
            device.wait(Until.hasObject(By.pkg("com.neuroflow.app")), 3000)
            device.waitForIdle(1000)

            val displayHeight = device.displayHeight
            val displayWidth = device.displayWidth

            // Critical Path 3: scroll the grid, which resolves each tile's icon on first bind
            repeat(3) {
                device.swipe(
                    displayWidth / 2,
                    displayHeight / 2,
                    displayWidth / 2,
                    displayHeight / 4,
                    10
                )
                device.waitForIdle(500)
            }

            repeat(2) {
                device.swipe(
                    displayWidth / 2,
                    displayHeight / 4,
                    displayWidth / 2,
                    displayHeight / 2,
                    10
                )
                device.waitForIdle(500)
            }

            // Return to stable state
            device.pressHome()
            device.waitForIdle()
        }
    }
}
