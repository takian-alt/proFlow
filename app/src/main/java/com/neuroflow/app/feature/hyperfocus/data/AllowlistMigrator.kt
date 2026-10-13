package com.neuroflow.app.feature.hyperfocus.data

data class AllowlistTiers(
    val essentialPackages: Set<String>,
    val allowedPackages: Set<String>
)

object AllowlistMigrator {

    fun derive(
        blockedPackages: Set<String>,
        launchable: Set<String>,
        essentialSeed: Set<String>,
        systemExempt: Set<String>,
        selfPackage: String
    ): AllowlistTiers {
        val essential = (launchable intersect essentialSeed) - blockedPackages
        val allowed = launchable - blockedPackages - systemExempt - essential - selfPackage
        return AllowlistTiers(essentialPackages = essential, allowedPackages = allowed)
    }
}
