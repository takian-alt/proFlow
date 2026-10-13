package com.neuroflow.app.feature.hyperfocus.data

import com.neuroflow.app.feature.hyperfocus.surface.FocusSurfaceModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe

private const val SELF = "com.neuroflow.app"
private val SEED = FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED
private val EXEMPT = setOf("android", "com.android.systemui", "com.android.settings")
private val LAUNCHABLE = setOf(
    "com.android.dialer", "com.android.mms", "com.android.settings", "com.android.systemui",
    "com.instagram.android", "com.netflix.mediaclient", "com.todoist", "org.telegram.messenger", SELF
)

class AllowlistMigratorTest : StringSpec({

    val blocked = setOf("com.instagram.android", "com.netflix.mediaclient")
    fun derive(blockedPackages: Set<String>, launchable: Set<String> = LAUNCHABLE) =
        AllowlistMigrator.derive(blockedPackages, launchable, SEED, EXEMPT, SELF)

    "AM-1: the complement of the denylist becomes the allowed tier" {
        val tiers = derive(blocked)
        tiers.allowedPackages shouldContain "com.todoist"
        tiers.allowedPackages shouldContain "org.telegram.messenger"
        tiers.allowedPackages shouldNotContainAnyOf listOf("com.instagram.android", "com.netflix.mediaclient")
    }

    "AM-2: the essential tier is the seed intersected with what is installed" {
        val tiers = derive(blocked)
        tiers.essentialPackages shouldContainExactly setOf("com.android.dialer", "com.android.mms", SELF)
    }

    "AM-3: blocked packages are never promoted into essential" {
        val tiers = derive(setOf("com.android.dialer"))
        tiers.essentialPackages shouldNotContain "com.android.dialer"
        tiers.allowedPackages shouldNotContain "com.android.dialer"
    }

    "AM-4: exempt packages and self never appear in the user-facing allowed tier" {
        val tiers = derive(blocked)
        tiers.allowedPackages shouldNotContainAnyOf listOf("com.android.systemui", "com.android.settings", SELF)
    }

    "AM-5: an empty denylist still yields a usable essential tier" {
        val tiers = derive(emptySet())
        (tiers.essentialPackages.isNotEmpty()) shouldBe true
        tiers.allowedPackages shouldContain "com.instagram.android"
    }

    "AM-6: nothing installed means nothing to allow, without inventing packages" {
        val tiers = derive(emptySet(), emptySet())
        tiers.essentialPackages.shouldBeEmpty()
        tiers.allowedPackages.shouldBeEmpty()
    }

    "AM-7: the two tiers cover every reachable app exactly once" {
        val tiers = derive(blocked)
        (tiers.essentialPackages intersect tiers.allowedPackages).shouldBeEmpty()
        tiers.essentialPackages + tiers.allowedPackages shouldBe
            setOf("com.android.dialer", "com.android.mms", SELF, "com.todoist", "org.telegram.messenger")
    }

    "AM-8: a call-path dialer stays in a tier rather than vanishing from both" {
        val tiers = derive(blocked, LAUNCHABLE + "com.google.android.dialer")
        tiers.allowedPackages shouldContain "com.google.android.dialer"
    }
})
