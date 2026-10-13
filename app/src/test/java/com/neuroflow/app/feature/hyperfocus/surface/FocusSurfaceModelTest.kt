package com.neuroflow.app.feature.hyperfocus.surface

import com.neuroflow.app.BuildConfig
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val SELF = "com.neuroflow.app"
private val EXEMPT = setOf("com.android.systemui", "android")
private const val NOW = 1_000_000L

class FocusSurfaceModelTest : StringSpec({

    "FS-1: essential and allowed both appear" {
        val prefs = HyperFocusPreferences(
            essentialPackages = setOf("com.android.dialer"),
            allowedPackages = setOf("com.todoist")
        )
        FocusSurfaceModel.resolvedAllowlist(prefs, EXEMPT, SELF, NOW) shouldContainAll
            setOf("com.android.dialer", "com.todoist", "com.android.systemui", "android", SELF)
    }

    "FS-2: an expired pass contributes nothing" {
        val prefs = HyperFocusPreferences(
            essentialPackages = setOf("com.android.dialer"),
            activePassPackage = "com.instagram.android",
            activePassExpiresAt = NOW - 1
        )
        FocusSurfaceModel.resolvedAllowlist(prefs, EXEMPT, SELF, NOW) shouldNotContain "com.instagram.android"
    }

    "FS-3: a live pass contributes exactly one package" {
        val prefs = HyperFocusPreferences(
            essentialPackages = setOf("com.android.dialer"),
            activePassPackage = "com.instagram.android",
            activePassExpiresAt = NOW + 60_000
        )
        FocusSurfaceModel.resolvedAllowlist(prefs, EXEMPT, SELF, NOW) shouldContain "com.instagram.android"
    }

    "FS-4: a live deadline with no package contributes nothing" {
        val prefs = HyperFocusPreferences(activePassExpiresAt = NOW + 60_000)
        FocusSurfaceModel.resolvedAllowlist(prefs, EXEMPT, SELF, NOW).size shouldBe EXEMPT.size + 1
    }

    "FS-5: a package with no deadline is not a pass" {
        val prefs = HyperFocusPreferences(activePassPackage = "com.instagram.android")
        FocusSurfaceModel.resolvedAllowlist(prefs, EXEMPT, SELF, NOW) shouldNotContain "com.instagram.android"
    }

    "FS-6: gating is the negation of the allowlist" {
        val prefs = HyperFocusPreferences(essentialPackages = setOf("com.android.dialer"))
        FocusSurfaceModel.isGated("com.android.dialer", prefs, EXEMPT, SELF, NOW) shouldBe false
        FocusSurfaceModel.isGated("com.netflix.mediaclient", prefs, EXEMPT, SELF, NOW) shouldBe true
        FocusSurfaceModel.isGated("com.android.systemui", prefs, EXEMPT, SELF, NOW) shouldBe false
        FocusSurfaceModel.isGated(SELF, prefs, EXEMPT, SELF, NOW) shouldBe false
    }

    "FS-7: empty user tiers still never gate the exempt set or self" {
        val prefs = HyperFocusPreferences()
        FocusSurfaceModel.resolvedAllowlist(prefs, EXEMPT, SELF, NOW) shouldContainAll setOf(
            "com.android.systemui", "android", SELF
        )
    }

    "FS-8: the default seed is the six spec packages" {
        FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED shouldBe setOf(
            "com.android.dialer", "com.google.android.apps.dialer", "com.samsung.android.dialer",
            "com.android.mms", "com.google.android.apps.messaging",
            "com.android.contacts", "com.android.camera", "com.google.android.Camera",
            "com.android.deskclock", "com.google.android.deskclock", SELF
        )
    }

    "FS-9: a pass whose deadline is exactly now is over" {
        val prefs = HyperFocusPreferences(
            activePassPackage = "com.instagram.android",
            activePassExpiresAt = NOW
        )
        FocusSurfaceModel.resolvedAllowlist(prefs, EXEMPT, SELF, NOW) shouldNotContain "com.instagram.android"
        FocusSurfaceModel.isGated("com.instagram.android", prefs, EXEMPT, SELF, NOW) shouldBe true
    }

    "FS-10: no essential seed entry is hidden by the exempt machinery set" {
        val seededWithoutSelf = FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED - BuildConfig.APPLICATION_ID
        // STATIC members are never gated *and* never an icon (spec 6.4), so the seed must not
        // overlap it; the dialers are seeded *and* protected, so the seed must overlap CALL_PATH.
        (seededWithoutSelf intersect SystemExempt.STATIC) shouldBe emptySet<String>()
        (FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED intersect SystemExempt.CALL_PATH) shouldNotBe emptySet<String>()
    }
})
