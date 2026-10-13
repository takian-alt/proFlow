package com.neuroflow.app.feature.hyperfocus.home

import android.content.ComponentName
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

private const val SELF = "com.neuroflow.app"

/**
 * `ComponentName` is a stub on the JVM test classpath — its constructor stores nothing and its
 * getters answer null — so the candidates are mocks whose two fields the code under test actually
 * reads, and the pick is asserted by identity rather than by equality of a constructed value.
 */
private fun component(pkg: String, cls: String): ComponentName =
    mockk<ComponentName> {
        every { packageName } returns pkg
        every { className } returns cls
    }

class RealHomeChoiceTest : StringSpec({

    "RH-1: a stored key that is still installed is the one picked" {
        val current = component("com.google.android.apps.nexuslauncher", ".Launcher")
        val other = component("com.sec.android.app.launcher", ".Launcher")
        pickHandoff(listOf(other, current), componentKey(current), SELF) shouldBe current
    }

    "RH-2: a malformed stored key is ignored, not crashed on" {
        val only = component("sec.androidlauncher", ".Launch")
        pickHandoff(listOf(only), "not-a-component", SELF) shouldBe only
    }

    "RH-3: this app is never the hand-off target" {
        pickHandoff(
            candidates = listOf(component(SELF, "com.neuroflow.app.feature.hyperfocus.home.HyperFocusHomeActivity")),
            storedKey = null,
            selfPackage = SELF
        ).shouldBeNull()
    }

    "RH-4: a candidate that matches the stored key but is gone falls through" {
        val installed = component("com.google.android.apps.nexuslauncher", ".Launcher")
        pickHandoff(
            candidates = listOf(installed),
            storedKey = componentKey(component("com.thirdparty.launcher", ".Main")),
            selfPackage = SELF
        ) shouldBe installed
    }

    "RH-5: no candidate at all is no hand-off, not a crash" {
        pickHandoff(emptyList(), storedKey = null, selfPackage = SELF).shouldBeNull()
    }

    "RH-6: a key with either half missing is not a component" {
        parseComponentKey(null).shouldBeNull()
        parseComponentKey("not-a-component").shouldBeNull()
        parseComponentKey("/ClassName").shouldBeNull()
        parseComponentKey("pkg/").shouldBeNull()
    }

    "RH-7: the key carries both halves, which is what makes the self-filter work" {
        componentKey(component("com.example.app", "MainActivity")) shouldBe "com.example.app/MainActivity"
    }
})
