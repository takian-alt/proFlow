package com.neuroflow.app.feature.hyperfocus.data

import com.neuroflow.app.domain.model.HyperFocusState
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class HyperFocusAllowlistFieldsTest : StringSpec({

    "HP-1: defaults carry an empty allowlist and no pass" {
        val prefs = HyperFocusPreferences()
        prefs.essentialPackages.shouldBeEmpty()
        prefs.allowedPackages.shouldBeEmpty()
        prefs.activePassPackage.shouldBeNull()
        prefs.activePassExpiresAt.shouldBeNull()
        prefs.realHomeComponent.shouldBeNull()
        prefs.isActive shouldBe false
        prefs.state shouldBe HyperFocusState.INACTIVE
    }
})
