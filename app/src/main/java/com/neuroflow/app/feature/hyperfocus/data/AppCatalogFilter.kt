package com.neuroflow.app.feature.hyperfocus.data

import com.neuroflow.app.feature.hyperfocus.surface.FocusSurfaceModel

data class InstalledApp(
    val packageName: String,
    val label: String,
    val activity: String
)

object AppCatalogFilter {

    private val FRAMEWORK_PREFIX = "com.android."

    fun keep(apps: List<InstalledApp>, systemExempt: Set<String>): List<InstalledApp> =
        apps.asSequence()
            .filter { it.packageName.isNotBlank() && it.activity.isNotBlank() }
            .filter {
                !it.packageName.startsWith(FRAMEWORK_PREFIX) ||
                    it.packageName in systemExempt ||
                    it.packageName in FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy({ it.label.lowercase() }, { it.packageName }))
            .toList()
}
