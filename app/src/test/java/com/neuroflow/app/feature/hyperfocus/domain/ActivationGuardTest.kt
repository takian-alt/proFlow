package com.neuroflow.app.feature.hyperfocus.domain

import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import com.neuroflow.app.feature.hyperfocus.home.renderableEssentials
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private const val SELF = "com.neuroflow.app"
private val EXEMPT = setOf("com.android.settings", "com.android.systemui")

class ActivationGuardTest : StringSpec({

    "AG-1: a surface with one real tile activates" {
        val prefs = HyperFocusPreferences(essentialPackages = setOf("com.todoist"))
        decideActivation(prefs, EXEMPT, SELF) shouldBe ActivationDecision.Allowed
    }

    "AG-2: an essentials tier made only of exempt packages cannot activate" {
        val prefs = HyperFocusPreferences(
            essentialPackages = setOf("com.android.settings", "com.android.systemui", SELF)
        )
        decideActivation(prefs, EXEMPT, SELF) shouldBe
            ActivationDecision.Rejected(ActivationRejection.EmptySurface)
    }

    "AG-3: an empty essentials tier cannot activate even when the allowed tier is full" {
        val prefs = HyperFocusPreferences(
            essentialPackages = emptySet(),
            allowedPackages = setOf("com.todoist", "com.somelab.frog")
        )
        decideActivation(prefs, EXEMPT, SELF) shouldBe
            ActivationDecision.Rejected(ActivationRejection.EmptySurface)
    }

    "AG-4: activation while a session is already running is refused" {
        val prefs = HyperFocusPreferences(isActive = true, essentialPackages = setOf("com.todoist"))
        decideActivation(prefs, EXEMPT, SELF) shouldBe
            ActivationDecision.Rejected(ActivationRejection.AlreadyActive)
    }

    "AG-5: the guard and the renderer agree about what counts as empty" {
        val cases = listOf(
            HyperFocusPreferences(essentialPackages = setOf("com.todoist")),
            HyperFocusPreferences(essentialPackages = EXEMPT),
            HyperFocusPreferences(essentialPackages = emptySet(), allowedPackages = setOf("com.todoist")),
            HyperFocusPreferences(essentialPackages = setOf(SELF))
        )
        for (prefs in cases) {
            val allowed = decideActivation(prefs.copy(isActive = false), EXEMPT, SELF) == ActivationDecision.Allowed
            val hasTiles = renderableEssentials(prefs, EXEMPT, SELF).isNotEmpty()
            allowed shouldBe hasTiles
        }
    }
})
