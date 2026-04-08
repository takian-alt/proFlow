package com.neuroflow.app.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.UserManager
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.neuroflow.app.BuildConfig
import com.neuroflow.app.presentation.launcher.LauncherActivity
import com.neuroflow.app.presentation.launcher.service.LauncherKeepAliveScheduler
import com.neuroflow.app.presentation.launcher.service.LauncherKeepAliveService
import com.neuroflow.app.receiver.DeviceAdminReceiver
import com.neuroflow.app.worker.KioskPolicyWorker
import java.util.concurrent.TimeUnit

object DeviceOwnerKioskManager {
    private const val TAG = "DeviceOwnerKiosk"
    private const val KIOSK_POLICY_WORK_NAME = "kiosk_policy_watchdog"
    private const val PREFS_FILE = "kiosk_prefs"
    private const val KEY_STRICT_MODE = "strict_mode"
    private const val RESTRICTION_DISALLOW_UNINSTALL_APPS = "no_uninstall_apps"
    private const val RESTRICTION_DISALLOW_APPS_CONTROL = "no_control_apps"

    fun isStrictModeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_STRICT_MODE, BuildConfig.KIOSK_STRICT_MODE)
    }

    fun setStrictModeEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_STRICT_MODE, enabled).apply()
    }

    /**
     * Mixed kiosk protection:
     * 1) DPC/device-owner policy (when enrolled)
     * 2) Alarm-based keepalive service schedule
     * 3) WorkManager periodic watchdog fallback
     */
    fun enableHybridProtection(context: Context) {
        try {
            LauncherKeepAliveService.start(context)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to start keepalive service", e)
        }

        LauncherKeepAliveScheduler.schedule(context)
        schedulePolicyWatchdog(context)
        applyDeviceOwnerPolicies(context)
    }

    fun onBootCompleted(context: Context) {
        enableHybridProtection(context)
        if (isDeviceOwner(context) && isStrictModeEnabled(context)) {
            launchKioskHome(context)
        }
    }

    fun isDeviceOwner(context: Context): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return false
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun isAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return false
        return dpm.isAdminActive(adminComponent(context))
    }

    fun canUseStrictMode(context: Context): Boolean {
        return isDeviceOwner(context) || isAdminActive(context)
    }

    fun shouldRequireLauncherAsHome(context: Context): Boolean {
        return isStrictModeEnabled(context) && isDeviceOwner(context)
    }

    fun isStrictKioskEnforcementEnabled(context: Context): Boolean {
        return isStrictModeEnabled(context) && canUseStrictMode(context)
    }

    fun applyDeviceOwnerPolicies(context: Context): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return false
        if (!dpm.isDeviceOwnerApp(context.packageName)) return false

        val admin = adminComponent(context)
        val strictMode = isStrictModeEnabled(context)
        return try {
            dpm.setLockTaskPackages(admin, arrayOf(context.packageName))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val features = if (strictMode) {
                    DevicePolicyManager.LOCK_TASK_FEATURE_NONE
                } else {
                    DevicePolicyManager.LOCK_TASK_FEATURE_HOME or
                        DevicePolicyManager.LOCK_TASK_FEATURE_OVERVIEW or
                        DevicePolicyManager.LOCK_TASK_FEATURE_NOTIFICATIONS
                }
                dpm.setLockTaskFeatures(admin, features)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dpm.setStatusBarDisabled(admin, strictMode)
                dpm.setKeyguardDisabled(admin, strictMode)
            }

            if (strictMode) {
                dpm.addUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT)
                dpm.addUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)
                dpm.addUserRestriction(admin, UserManager.DISALLOW_ADD_USER)
            } else {
                dpm.clearUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT)
                dpm.clearUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)
                dpm.clearUserRestriction(admin, UserManager.DISALLOW_ADD_USER)
            }

            val homeFilter = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            dpm.addPersistentPreferredActivity(
                admin,
                homeFilter,
                ComponentName(context, LauncherActivity::class.java)
            )

            Log.i(TAG, "Device owner kiosk policies applied (strict=$strictMode)")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to apply device-owner policies", e)
            false
        }
    }

    fun enforceLockTask(activity: Activity) {
        Log.i(TAG, "Lock task pinning disabled; skipping startLockTask")
    }

    fun exitLockTaskIfActive(activity: Activity) {
        if (!isInLockTaskMode(activity)) return

        try {
            activity.stopLockTask()
            Log.i(TAG, "Lock task stopped")
        } catch (e: SecurityException) {
            Log.e(TAG, "Unable to stop lock task", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Unable to stop lock task", e)
        }
    }

    fun syncLockTaskMode(activity: Activity) {
        exitLockTaskIfActive(activity)
    }

    fun launchKioskHome(context: Context) {
        if (!canUseStrictMode(context)) return
        val homeIntent = Intent(context, LauncherActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(homeIntent)
    }

    fun bringLauncherToFront(context: Context) {
        if (!canUseStrictMode(context)) return
        val intent = Intent(context, LauncherActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        runCatching { context.startActivity(intent) }
            .onFailure { Log.w(TAG, "Unable to bring launcher to front", it) }
    }

    /**
     * Tighten app-management controls while Hyper Focus is active so users cannot
     * uninstall or force-stop via App Info paths on fully managed devices.
     *
     * Requires device-owner privileges; no-op otherwise.
     */
    fun setHyperFocusSelfProtection(context: Context, enabled: Boolean): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return false
        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            Log.i(TAG, "Hyper Focus self-protection skipped: not device owner")
            return false
        }

        val admin = adminComponent(context)
        return try {
            dpm.setUninstallBlocked(admin, context.packageName, enabled)
            if (enabled) {
                dpm.addUserRestriction(admin, RESTRICTION_DISALLOW_UNINSTALL_APPS)
                dpm.addUserRestriction(admin, RESTRICTION_DISALLOW_APPS_CONTROL)
            } else {
                dpm.clearUserRestriction(admin, RESTRICTION_DISALLOW_UNINSTALL_APPS)
                dpm.clearUserRestriction(admin, RESTRICTION_DISALLOW_APPS_CONTROL)
            }
            Log.i(TAG, "Hyper Focus self-protection updated: enabled=$enabled")
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to update Hyper Focus self-protection", e)
            false
        }
    }

    private fun adminComponent(context: Context): ComponentName {
        return ComponentName(context, DeviceAdminReceiver::class.java)
    }

    private fun schedulePolicyWatchdog(context: Context) {
        val request = PeriodicWorkRequestBuilder<KioskPolicyWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            KIOSK_POLICY_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun isInLockTaskMode(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
        } else {
            @Suppress("DEPRECATION")
            activityManager.isInLockTaskMode
        }
    }
}
