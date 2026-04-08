package com.neuroflow.app.presentation.launcher.hyperfocus.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.neuroflow.app.BuildConfig
import com.neuroflow.app.domain.model.HyperFocusSessionMode
import com.neuroflow.app.kiosk.DeviceOwnerKioskManager
import com.neuroflow.app.presentation.launcher.hyperfocus.HyperFocusActivity
import com.neuroflow.app.presentation.launcher.hyperfocus.data.HyperFocusDataStore
import com.neuroflow.app.presentation.launcher.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.presentation.launcher.hyperfocus.domain.RewardEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppBlockingService : AccessibilityService() {

    @Inject lateinit var hyperFocusDataStore: HyperFocusDataStore
    @Inject lateinit var hyperFocusManager: HyperFocusManager

    private val heartbeatScope = CoroutineScope(Dispatchers.IO)
    private var lastBlockedPackage: String? = null
    private var lastBlockTime: Long = 0
    @Volatile private var lastSeenForegroundPackage: String? = null

    companion object {
        private const val TAG = "AppBlockingService"
        private const val BLOCK_DEBOUNCE_MS = 1000L
        private const val HEARTBEAT_INTERVAL_MS = 2_000L
        private const val POLLING_INTERVAL_MS = 300L

        // Core tamper paths across common OEM builds.
        private val TAMPER_SENSITIVE_PACKAGES = setOf(
            "com.android.settings",
            "com.google.android.permissioncontroller",
            "com.samsung.android.settings",
            "com.miui.securitycenter",
            "com.coloros.safecenter",
            "com.oplus.safecenter",
            "com.huawei.systemmanager"
        )
    }

    // When the unlock timer expires, reset debounce so the polling immediately re-blocks
    private val unlockExpiredReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == UnlockTimerService.ACTION_UNLOCK_EXPIRED) {
                Log.d(TAG, "Unlock expired — resetting debounce to re-block current app")
                lastBlockedPackage = null
                lastBlockTime = 0
            }
        }
    }

    private val packageTamperReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            val packageName = intent.data?.schemeSpecificPart ?: return

            heartbeatScope.launch {
                val prefs = hyperFocusDataStore.current()
                if (!prefs.isActive) return@launch

                if (packageName == BuildConfig.APPLICATION_ID) {
                    Log.w(TAG, "Tamper: own package event detected ($action).")
                    hyperFocusManager.reportTamper("App package event: $action")
                }

                if (packageName in prefs.blockedPackages) {
                    Log.w(TAG, "Tamper: blocked package event detected ($action) for $packageName")
                    hyperFocusManager.reportTamper("Blocked app package event: $action for $packageName")
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return

        // Prefer rootInActiveWindow package — more reliable than event.packageName
        // event.packageName can be system UI or launcher on some ROMs
        val pkg = rootInActiveWindow?.packageName?.toString()
            ?: event.packageName?.toString()
            ?: return

        lastSeenForegroundPackage = pkg

        checkAndBlockApp(pkg)
    }

    override fun onInterrupt() {
        Log.d(TAG, "AppBlockingService interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "AppBlockingService connected")

        runCatching {
            DeviceOwnerKioskManager.enableHybridProtection(this)
        }.onFailure { error ->
            Log.w(TAG, "Failed to re-arm kiosk/keepalive protections from accessibility service", error)
        }

        serviceInfo?.let { info ->
            info.flags = info.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }

        registerReceiverCompat(
            unlockExpiredReceiver,
            IntentFilter(UnlockTimerService.ACTION_UNLOCK_EXPIRED)
        )

        registerReceiverCompat(
            packageTamperReceiver,
            IntentFilter().apply {
                addDataScheme("package")
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addAction(Intent.ACTION_PACKAGE_RESTARTED)
            }
        )

        // Heartbeat
        heartbeatScope.launch {
            while (true) {
                hyperFocusManager.updateHeartbeat()  // now suspend, safe to call here
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }

        // Polling backup — handles kicking user out when timer expires
        // Uses rootInActiveWindow as primary (no permission needed), UsageStats as secondary
        heartbeatScope.launch {
            while (true) {
                delay(POLLING_INTERVAL_MS)
                val prefs = hyperFocusDataStore.current()
                if (prefs.isActive) {
                    val currentApp = rootInActiveWindow?.packageName?.toString()
                        ?: getForegroundAppFromUsageStats()
                        ?: lastSeenForegroundPackage
                    if (currentApp != null) {
                        checkAndBlockApp(currentApp)
                    }
                }
            }
        }
    }

    private fun checkAndBlockApp(pkg: String) {
        if (pkg == BuildConfig.APPLICATION_ID) return

        heartbeatScope.launch {
            val prefs = hyperFocusDataStore.current()
            if (!prefs.isActive) return@launch

            if (prefs.sessionMode == HyperFocusSessionMode.TIME_BASED) {
                val endsAt = prefs.sessionEndsAtMillis
                if (endsAt != null && endsAt <= System.currentTimeMillis()) {
                    hyperFocusManager.deactivate()
                    return@launch
                }
            }

            val isTamperSensitive = pkg in TAMPER_SENSITIVE_PACKAGES
            val isUserBlockedApp = pkg in prefs.blockedPackages
            if (!isUserBlockedApp && !isTamperSensitive) return@launch
            if (RewardEngine.isUnlockActive(prefs)) return@launch

            if (isTamperSensitive && !isUserBlockedApp) {
                hyperFocusManager.reportTamper("Protected settings opened: $pkg")
            }

            val now = System.currentTimeMillis()
            if (pkg == lastBlockedPackage && (now - lastBlockTime) < BLOCK_DEBOUNCE_MS) return@launch

            lastBlockedPackage = pkg
            lastBlockTime = now
            Log.d(TAG, "Blocking app: $pkg")

            val launchIntent = Intent(this@AppBlockingService, HyperFocusActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("blocked_package", pkg)
            }

            val launched = runCatching {
                startActivity(launchIntent)
            }.onFailure { error ->
                Log.w(TAG, "Failed to launch blocking overlay for $pkg", error)
            }.isSuccess

            if (!launched) {
                if (DeviceOwnerKioskManager.isStrictKioskEnforcementEnabled(this@AppBlockingService)) {
                    DeviceOwnerKioskManager.bringLauncherToFront(this@AppBlockingService)
                }
                // Android 12+ can deny background launches; force HOME so user exits blocked app.
                runCatching {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }.onFailure { error ->
                    Log.w(TAG, "Fallback HOME action failed after overlay launch denial", error)
                }
            }
        }
    }

    private fun registerReceiverCompat(receiver: BroadcastReceiver, filter: IntentFilter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            ContextCompat.registerReceiver(
                this,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    private fun getForegroundAppFromUsageStats(): String? {
        return try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
            val now = System.currentTimeMillis()
            usm?.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_BEST, now - 2000L, now)
                ?.maxByOrNull { it.lastTimeUsed }?.packageName
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AppBlockingService destroyed")
        runCatching { unregisterReceiver(unlockExpiredReceiver) }
        runCatching { unregisterReceiver(packageTamperReceiver) }
        heartbeatScope.cancel()
    }
}
