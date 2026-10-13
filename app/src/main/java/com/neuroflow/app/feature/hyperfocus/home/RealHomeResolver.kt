package com.neuroflow.app.feature.hyperfocus.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enumerates the HOME candidates and picks this app out of them. `queryIntentActivities` is not
 * filtered by the current default, which is what makes the real launcher findable even while a
 * persistent-preferred rule (or the user's own default choice) points HOME at us.
 */
@Singleton
class RealHomeResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun candidates(): List<ComponentName> {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return context.packageManager
            .queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { info ->
                info.activityInfo?.let { ComponentName(it.packageName, it.name) }
            }
            .distinctBy { it.packageName }
    }

    fun handoff(storedKey: String?): ComponentName? =
        pickHandoff(candidates(), storedKey, context.packageName)
}
