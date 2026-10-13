package com.neuroflow.app.feature.hyperfocus.home

import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val SELF = "com.neuroflow.app"
private val EXEMPT = setOf("com.android.systemui", "com.android.settings")
private const val NOW = 1_000_000L
private val LABELS = mapOf(
    "com.todoist" to "Todoist",
    "com.android.dialer" to "Dialer",
    "com.somelab.frog" to "Frog"
)

class HomeSurfaceStateTest : StringSpec({

    "HFH-1: an inactive session is a hand-off, not an empty black screen" {
        val prefs = HyperFocusPreferences(
            isActive = false,
            essentialPackages = setOf("com.todoist")
        )
        homeSurfaceStateOf(prefs, LABELS, EXEMPT, SELF, NOW) shouldBe HomeSurfaceState.HandOff
    }

    "HFH-2: essentials render as tiles sorted by label" {
        val prefs = HyperFocusPreferences(
            isActive = true,
            essentialPackages = setOf("com.todoist", "com.somelab.frog")
        )
        val state = homeSurfaceStateOf(prefs, LABELS, EXEMPT, SELF, NOW)
            as HomeSurfaceState.Active
        state.essentials.map { it.label } shouldBe listOf("Frog", "Todoist")
    }

    "HFH-3: an exempt package is never a tile even when the user put it in the tier" {
        val prefs = HyperFocusPreferences(
            isActive = true,
            essentialPackages = setOf("com.android.settings", "com.todoist")
        )
        val state = homeSurfaceStateOf(prefs, LABELS, EXEMPT, SELF, NOW)
            as HomeSurfaceState.Active
        state.essentials.map { it.packageName } shouldContainExactly setOf("com.todoist")
    }

    "HFH-4: this app is never a tile" {
        val prefs = HyperFocusPreferences(
            isActive = true,
            essentialPackages = setOf(SELF, "com.todoist")
        )
        renderableEssentials(prefs, EXEMPT, SELF) shouldContainExactly setOf("com.todoist")
    }

    "HFH-5: a missing label falls back to the package name, never blank" {
        val prefs = HyperFocusPreferences(
            isActive = true,
            essentialPackages = setOf("com.unlabelled.app")
        )
        val state = homeSurfaceStateOf(prefs, LABELS, EXEMPT, SELF, NOW)
            as HomeSurfaceState.Active
        state.essentials.single().label shouldBe "com.unlabelled.app"
    }

    "HFH-6: a live pass surfaces its seconds and package; an expired one surfaces neither" {
        val live = HyperFocusPreferences(
            isActive = true,
            essentialPackages = setOf("com.todoist"),
            activePassPackage = "com.instagram.android",
            activePassExpiresAt = NOW + 120_000L
        )
        val state = homeSurfaceStateOf(live, LABELS, EXEMPT, SELF, NOW)
            as HomeSurfaceState.Active
        state.passSecondsRemaining shouldBe 120L
        state.passPackage shouldBe "com.instagram.android"

        val dead = live.copy(activePassExpiresAt = NOW - 1L)
        val expired = homeSurfaceStateOf(dead, LABELS, EXEMPT, SELF, NOW)
            as HomeSurfaceState.Active
        expired.passSecondsRemaining.shouldBeNull()
        expired.passPackage.shouldBeNull()
    }

    "HFH-7: zero essentials is still an active surface" {
        val prefs = HyperFocusPreferences(isActive = true, essentialPackages = emptySet())
        val state = homeSurfaceStateOf(prefs, LABELS, EXEMPT, SELF, NOW)
        (state is HomeSurfaceState.Active) shouldBe true
        (state as HomeSurfaceState.Active).essentials shouldBe emptyList()
    }
})
