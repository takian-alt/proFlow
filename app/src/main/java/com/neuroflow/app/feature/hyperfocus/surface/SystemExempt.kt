package com.neuroflow.app.feature.hyperfocus.surface

import android.content.Context
import android.view.inputmethod.InputMethodManager
import com.neuroflow.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Packages that must never be gated: gating them locks the user out of their own device.
 * Read fresh because the enabled keyboard changes without our knowing.
 */
@Singleton
class SystemExempt @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun packages(): Set<String> = buildSet {
        addAll(STATIC)
        add(BuildConfig.APPLICATION_ID)
        val imm = context.getSystemService(InputMethodManager::class.java)
        imm?.enabledInputMethodList?.forEach { add(it.packageName) }
    }

    companion object {
        val STATIC: Set<String> = setOf(
            "android",
            "com.android.systemui",
            "com.android.server.telecom",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.miui.packageinstaller",
            "com.android.settings",
            "com.samsung.android.settings",
            "com.miui.securitycenter"
        )
    }
}
