package com.neuroflow.app.feature.hyperfocus.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class HomeRoleUtilTest : StringSpec({

    val self = "com.neuroflow.app"

    "HR-1: nothing resolves, so we do not hold the role" {
        // No default home at all. An unknown answer must never read as "the black home is live",
        // because the sheet would then hide the only button that sets it.
        isHomeRoleHeld(null, self) shouldBe false
    }

    "HR-2: our package is the default home" {
        isHomeRoleHeld(self, self) shouldBe true
    }

    "HR-3: another launcher holds the role" {
        isHomeRoleHeld("sec.android.touchwiz.launcher", self) shouldBe false
    }

    "HR-4: the system resolver is not us" {
        // With no default chosen, resolveActivity returns the internal picker, whose package is
        // "android". Pressing Home opens the chooser, not the black home.
        isHomeRoleHeld("android", self) shouldBe false
    }

    "HR-5: a prefix match is not the role" {
        isHomeRoleHeld("com.neuroflow.app.other", self) shouldBe false
    }
})
