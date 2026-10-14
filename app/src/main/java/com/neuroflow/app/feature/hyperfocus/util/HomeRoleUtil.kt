package com.neuroflow.app.feature.hyperfocus.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Which app the Home button opens. The black home is only a home if the user picked it: this device
 * is held as a device administrator, not a device owner, so `addPersistentPreferredActivity` never
 * runs and nothing pins HOME for us.
 */
object HomeRoleUtil {

    /** Null when the query fails or nothing resolves — an unknown answer is never "we are the home". */
    fun defaultHomePackage(context: Context): String? = runCatching {
        context.packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.packageName
    }.getOrNull()

    fun holdsHomeRole(context: Context): Boolean =
        isHomeRoleHeld(defaultHomePackage(context), context.packageName)
}

/** Kept apart from the PackageManager call so the decision is testable on the JVM. */
fun isHomeRoleHeld(defaultHomePackage: String?, selfPackage: String): Boolean =
    defaultHomePackage == selfPackage
