package com.neuroflow.app.feature.hyperfocus.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private fun app(pkg: String, label: String = pkg) = InstalledApp(pkg, label, "$pkg.MainActivity")

class AppCatalogFilterTest : StringSpec({

    "AC-1: ordinary launchable apps keep" {
        AppCatalogFilter.keep(listOf(app("com.todoist")), setOf("android"))
            .map { it.packageName } shouldContainExactly listOf("com.todoist")
    }

    "AC-2: framework packages drop unless exempt" {
        val kept = AppCatalogFilter.keep(
            listOf(app("com.android.systemui"), app("android"), app("com.android.dialer")),
            setOf("android", "com.android.dialer")
        )
        // "android" is named in the exempt set, so it is kept; only com.android.systemui,
        // which is neither exempt nor seeded, drops.
        kept.map { it.packageName } shouldContainExactly listOf("android", "com.android.dialer")
    }

    "AC-3: blank activity rows are not launchable and drop" {
        AppCatalogFilter.keep(
            listOf(InstalledApp("com.example", "Example", "")), emptySet()
        ).size shouldBe 0
    }

    "AC-4: duplicates by package collapse to the first, in input order" {
        val kept = AppCatalogFilter.keep(
            listOf(app("com.todoist", "Todoist"), app("com.todoist", "Todoist Lite")), emptySet()
        )
        kept.size shouldBe 1
        kept.first().label shouldBe "Todoist"
    }

    "AC-5: ordering is stable by label, then package" {
        val kept = AppCatalogFilter.keep(
            listOf(app("z", "Alpha"), app("m", "Beta"), app("a", "Alpha")), emptySet()
        )
        kept.map { it.packageName } shouldContainExactly listOf("a", "z", "m")
    }

    "AC-6: the default essential seed is never filtered out of the catalog" {
        val seeded = listOf(
            "com.android.dialer", "com.android.mms", "com.android.contacts",
            "com.android.camera", "com.android.deskclock"
        )
        AppCatalogFilter.keep(seeded.map { app(it) }, emptySet())
            .map { it.packageName } shouldContainExactly seeded.sorted()
    }
})
