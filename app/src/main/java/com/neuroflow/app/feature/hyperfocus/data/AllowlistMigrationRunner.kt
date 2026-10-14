package com.neuroflow.app.feature.hyperfocus.data

import com.neuroflow.app.BuildConfig
import com.neuroflow.app.feature.hyperfocus.surface.FocusSurfaceModel
import com.neuroflow.app.feature.hyperfocus.surface.SystemExempt
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot per install: turns the pre-allowlist denylist into its complement allowlist and copies the
 * launcher's distraction scores across before launcher_prefs stops being read.
 */
@Singleton
class AllowlistMigrationRunner @Inject constructor(
    private val hyperFocusDataStore: HyperFocusDataStore,
    private val appCatalogRepository: AppCatalogRepository,
    private val legacyLauncherPrefs: LegacyLauncherPrefs
) {

    suspend fun runIfNeeded() {
        val prefs = hyperFocusDataStore.current()
        if (prefs.allowlistMigratedAt != null) return
        // Never rewrite tiers underneath a live session: AppBlockingService reads them on every
        // foreground event. Tiers that were never derived are not a rewrite — `isActive` survives an
        // upgrade from the pre-allowlist build, and guarding on it alone skipped the migration on
        // every startup of the very install that needs it most.
        val hasTiers = prefs.essentialPackages.isNotEmpty() || prefs.allowedPackages.isNotEmpty()
        if (prefs.isActive && hasTiers) return

        val launchable = appCatalogRepository.launchableApps().map { it.packageName }.toSet()
        // An empty catalog is a failed query, not a user who installed nothing. Leaving the flag
        // unset retries at the next startup instead of stamping an empty allowlist as migrated.
        if (launchable.isEmpty()) return

        val tiers = AllowlistMigrator.derive(
            // Read from the blob, not the model: the denylist is not a live concept (B2), and this is
            // the only code allowed to look at it.
            blockedPackages = hyperFocusDataStore.legacyBlockedPackages(),
            launchable = launchable,
            essentialSeed = FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED,
            // STATIC, not packages(): an app exempt for enforcement must still land in a user tier, or
            // the Pixel dialer belongs to neither tier and the editor's counts lie about it.
            systemExempt = SystemExempt.STATIC,
            selfPackage = BuildConfig.APPLICATION_ID
        )
        val legacyScores = legacyLauncherPrefs.distractionScoresSnapshot()

        hyperFocusDataStore.update {
            it.copy(
                essentialPackages = tiers.essentialPackages,
                allowedPackages = tiers.allowedPackages,
                // Fill only what the user has not already written, in case a score landed mid-run.
                distractionScores = it.distractionScores.ifEmpty { legacyScores },
                allowlistMigratedAt = System.currentTimeMillis()
            )
        }
    }
}
