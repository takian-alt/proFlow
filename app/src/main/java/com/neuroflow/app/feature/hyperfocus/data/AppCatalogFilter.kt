package com.neuroflow.app.feature.hyperfocus.data

data class InstalledApp(
    val packageName: String,
    val label: String,
    val activity: String
)

object AppCatalogFilter {

    /**
     * No namespace rule by design: a package dropped here has no route back, because the migrator
     * and the picker both read this list. See the Global Constraint on catalog completeness.
     */
    fun keep(apps: List<InstalledApp>): List<InstalledApp> =
        apps.asSequence()
            .filter { it.packageName.isNotBlank() && it.activity.isNotBlank() }
            .distinctBy { it.packageName }
            .sortedWith(compareBy({ it.label.lowercase() }, { it.packageName }))
            .toList()
}
