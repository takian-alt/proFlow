package com.neuroflow.app.feature.hyperfocus.data

import com.neuroflow.app.feature.hyperfocus.surface.FocusSurfaceModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private fun app(pkg: String, label: String = pkg, activity: String = "$pkg.MainActivity") =
    InstalledApp(pkg, label, activity)

class AppCatalogFilterTest : StringSpec({

    "AC-1: ordinary launchable apps keep" {
        AppCatalogFilter.keep(listOf(app("com.todoist")))
            .map { it.packageName } shouldContainExactly listOf("com.todoist")
    }

    "AC-2: the catalog hides nothing by namespace — a com.android user app survives" {
        val kept = AppCatalogFilter.keep(
            listOf(app("com.android.chrome"), app("com.android.vending"),
                   app("com.android.systemui"), app("com.todoist"))
        )
        kept.map { it.packageName } shouldContainExactly
            listOf("com.android.chrome", "com.android.systemui", "com.android.vending", "com.todoist")
    }

    "AC-3: blank activity rows are not launchable and drop" {
        AppCatalogFilter.keep(listOf(InstalledApp("com.example", "Example", ""))).size shouldBe 0
    }

    "AC-4: duplicates collapse to the row seen first, not the row that sorts first" {
        val kept = AppCatalogFilter.keep(listOf(app("com.todoist", "Zeta"), app("com.todoist", "Alpha")))
        kept.size shouldBe 1
        kept.first().label shouldBe "Zeta"
    }

    "AC-5: ordering is stable by label, then package" {
        val kept = AppCatalogFilter.keep(listOf(app("z", "Alpha"), app("m", "Beta"), app("a", "Alpha")))
        kept.map { it.packageName } shouldContainExactly listOf("a", "z", "m")
    }

    "AC-6: the seeded essentials are never dropped from the catalog" {
        val seed = FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED.toList()
        val kept = AppCatalogFilter.keep(seed.map { app(it) })
        kept.size shouldBe seed.size
        kept.map { it.packageName } shouldContainAll seed
    }
})
