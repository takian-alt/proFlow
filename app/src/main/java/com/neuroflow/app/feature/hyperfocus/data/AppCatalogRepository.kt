package com.neuroflow.app.feature.hyperfocus.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.neuroflow.app.feature.hyperfocus.surface.SystemExempt
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppCatalogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val launcherApps: LauncherApps,
    private val systemExempt: SystemExempt
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
        val rows = users.flatMap { user ->
            launcherApps.getActivityList(null, user).map { info ->
                InstalledApp(
                    packageName = info.componentName.packageName,
                    label = info.label.toString(),
                    activity = info.componentName.className
                ) to AppEntry(info.componentName.packageName, info.label.toString(), info.componentName, user)
            }
        }
        val keep = AppCatalogFilter.keep(rows.map { it.first }, systemExempt.packages())
            .map { it.packageName }.toSet()
        rows.filter { it.first.packageName in keep }.map { it.second }.distinctBy { it.packageName }
    }

    suspend fun launch(packageName: String): Boolean = withContext(Dispatchers.Default) {
        val target = launchableApps().firstOrNull { it.packageName == packageName }
            ?: return@withContext false
        runCatching {
            launcherApps.startMainActivity(target.component, target.user, null, null)
        }.isSuccess
    }

    fun labelFor(packageName: String): String =
        runCatching {
            launcherApps.getActivityList(packageName, Process.myUserHandle())
                .firstOrNull()?.label?.toString()
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: packageName
}
