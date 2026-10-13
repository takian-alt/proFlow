package com.neuroflow.app.feature.hyperfocus.surface

import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private const val SELF = "com.neuroflow.app"
private val EXEMPT = setOf("com.android.systemui", "com.android.settings")

class AllowlistEditorTest : StringSpec({

    "AE-1: promoting an allowed app to essential moves it instead of duplicating it" {
        val draft = AllowlistDraft(essential = emptySet(), allowed = setOf("com.todoist"))
        val next = toggleTier(draft, "com.todoist", AllowlistTier.Essential)
        next.essential shouldContainExactly setOf("com.todoist")
        next.allowed shouldBe emptySet()
    }

    "AE-2: tapping the tier a package is already in removes it" {
        val draft = AllowlistDraft(essential = setOf("com.todoist"), allowed = emptySet())
        val next = toggleTier(draft, "com.todoist", AllowlistTier.Essential)
        next.essential shouldBe emptySet()
        next.allowed shouldBe emptySet()
    }

    "AE-3: no toggle sequence can leave a package in both tiers" {
        var draft = AllowlistDraft(emptySet(), emptySet())
        val pkgs = listOf("a", "b", "c")
        for (i in 0 until 24) {
            draft = toggleTier(draft, pkgs[i % 3], if (i % 2 == 0) AllowlistTier.Essential else AllowlistTier.Allowed)
            (draft.essential intersect draft.allowed) shouldBe emptySet()
        }
    }

    "AE-4: draftFrom and applyDraft round-trip through preferences" {
        val prefs = HyperFocusPreferences(
            essentialPackages = setOf("com.android.dialer"),
            allowedPackages = setOf("com.todoist"),
        )
        applyDraft(prefs, draftFrom(prefs)) shouldBe prefs
    }

    "AE-5: applying a draft rewrites only the two tiers" {
        val prefs = HyperFocusPreferences(
            isActive = true,
            sessionId = "session-1",
            dailyTaskTarget = 4,
            essentialPackages = emptySet(),
            allowedPackages = emptySet()
        )
        val saved = applyDraft(prefs, AllowlistDraft(setOf("com.todoist"), setOf("com.somelab.frog")))
        saved.isActive shouldBe true
        saved.sessionId shouldBe "session-1"
        saved.dailyTaskTarget shouldBe 4
        saved.essentialPackages shouldContainExactly setOf("com.todoist")
        saved.allowedPackages shouldContainExactly setOf("com.somelab.frog")
    }

    "AE-6: exempt packages and this app are not offered for the tiers" {
        editableApps(
            all = setOf("com.todoist", "com.android.settings", "com.android.systemui", SELF),
            systemExempt = EXEMPT,
            selfPackage = SELF
        ) shouldContainExactly setOf("com.todoist")
    }
})
