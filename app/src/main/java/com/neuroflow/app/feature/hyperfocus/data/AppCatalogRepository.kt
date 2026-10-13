package com.neuroflow.app.feature.hyperfocus.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppCatalogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val launcherApps: LauncherApps
) {
    data class AppEntry(
        val packageName: String,
        val label: String,
        val component: ComponentName,
        val user: UserHandle
    )

    suspend fun launchableApps(): List<AppEntry> = withContext(Dispatchers.Default) {
        val users: List<UserHandle> =
            context.getSystemService(UserManager::class.java)?.userProfiles
                ?: listOf(Process.myUserHandle())
        // One paused work profile must not empty the catalog: getActivityList throws there.
        val rows = users.flatMap { user ->
            runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())
                .map { info ->
                    val pkg = info.componentName.packageName
                    val label = info.label.toString()
                    InstalledApp(pkg, label, info.componentName.className) to
                        AppEntry(pkg, label, info.componentName, user)
                }
        }
        val firstByPackage = mutableMapOf<String, AppEntry>()
        rows.forEach { (_, entry) -> firstByPackage.putIfAbsent(entry.packageName, entry) }
        AppCatalogFilter.keep(rows.map { it.first }).mapNotNull { firstByPackage[it.packageName] }
    }

    /** Re-queried, not cached: a `UserHandle` from an earlier enumeration throws on launch. */
    suspend fun launch(packageName: String): Boolean = withContext(Dispatchers.Default) {
        val target = runCatching { launchableApps().firstOrNull { it.packageName == packageName } }
            .getOrNull() ?: return@withContext false
        runCatching {
            launcherApps.startMainActivity(target.component, target.user, null, null)
        }.onFailure { Log.w("AppCatalog", "launch failed for $packageName", it) }.isSuccess
    }

    /** Personal profile only, so a work-profile package logs its package name in the audit row. */
    suspend fun labelFor(packageName: String): String = withContext(Dispatchers.Default) {
        runCatching {
            launcherApps.getActivityList(packageName, Process.myUserHandle())
                .firstOrNull()?.label?.toString()
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: packageName
    }
}
