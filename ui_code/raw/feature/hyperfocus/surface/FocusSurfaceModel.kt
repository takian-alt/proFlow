package com.neuroflow.app.feature.hyperfocus.surface

import com.neuroflow.app.BuildConfig
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences

/**
 * The single source of truth for "may this package be used right now".
 * Everything that gates an app reads resolvedAllowlist(); nothing computes its own view.
 */
object FocusSurfaceModel {

    val DEFAULT_ESSENTIAL_SEED: Set<String> = setOf(
        "com.android.dialer", "com.google.android.apps.dialer", "com.samsung.android.dialer",
        "com.android.mms", "com.google.android.apps.messaging",
        "com.android.contacts",
        "com.android.camera", "com.google.android.Camera",
        "com.android.deskclock", "com.google.android.deskclock",
        BuildConfig.APPLICATION_ID
    )

    fun resolvedAllowlist(
        prefs: HyperFocusPreferences,
        systemExempt: Set<String>,
        selfPackage: String,
        nowMillis: Long
    ): Set<String> = buildSet {
        addAll(prefs.essentialPackages)
        addAll(prefs.allowedPackages)
        addAll(systemExempt)
        add(selfPackage)
        val pass = prefs.activePassPackage
        val deadline = prefs.activePassExpiresAt
        if (pass != null && deadline != null && deadline > nowMillis) add(pass)
    }

    fun isGated(
        packageName: String,
        prefs: HyperFocusPreferences,
        systemExempt: Set<String>,
        selfPackage: String,
        nowMillis: Long
    ): Boolean = packageName !in resolvedAllowlist(prefs, systemExempt, selfPackage, nowMillis)
}
