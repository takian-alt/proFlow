package com.neuroflow.app.presentation.launcher.hyperfocus.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.neuroflow.app.BuildConfig
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
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class AppBlockingService : AccessibilityService() {

    @Inject lateinit var hyperFocusDataStore: HyperFocusDataStore
    @Inject lateinit var hyperFocusManager: HyperFocusManager

    private val heartbeatScope = CoroutineScope(Dispatchers.IO)
    private var lastBlockedPackage: String? = null
    private var lastBlockTime: Long = 0

    companion object {
        private const val TAG = "AppBlockingService"
        private const val BLOCK_DEBOUNCE_MS = 1000L // Prevent rapid re-blocking
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Only process window state changes and window changes (app switches)
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            return
        }

        val pkg = event.packageName?.toString() ?: return

        // Always allow our own app
        if (pkg == BuildConfig.APPLICATION_ID) return

        val prefs = runBlocking { hyperFocusDataStore.current() }

        // If Hyper Focus is not active, service is idle (this is normal)
        if (!prefs.isActive) return

        // If this app is not in the blocked list, allow it
        if (pkg !in prefs.blockedPackages) return

        // If user has an active unlock, allow the app
        if (RewardEngine.isUnlockActive(prefs)) return

        // Debounce: prevent blocking the same app multiple times in quick succession
        val now = System.currentTimeMillis()
        if (pkg == lastBlockedPackage && (now - lastBlockTime) < BLOCK_DEBOUNCE_MS) {
            return
        }

        lastBlockedPackage = pkg
        lastBlockTime = now

        Log.d(TAG, "Blocking app: $pkg")

        // Launch the blocking overlay
        val intent = Intent(this, HyperFocusActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("blocked_package", pkg)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        Log.d(TAG, "AppBlockingService interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "AppBlockingService connected")

        // Provide service info for better Android Settings display
        serviceInfo?.let { info ->
            info.flags = info.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }

        heartbeatScope.launch {
            while (true) {
                delay(30_000L)
                hyperFocusManager.updateHeartbeat()
            }
        }

        // Polling as backup detection method (in case accessibility events are missed)
        heartbeatScope.launch {
            while (true) {
                delay(500L) // Poll every 500ms
                val prefs = hyperFocusDataStore.current()
                if (prefs.isActive) {
                    // Check current foreground app using UsageStats as backup
                    val currentApp = getForegroundAppFromUsageStats()
                    if (currentApp != null && currentApp != lastBlockedPackage) {
                        checkAndBlockApp(currentApp)
                    }
                }
            }
        }
    }

    private fun checkAndBlockApp(pkg: String) {
        if (pkg == BuildConfig.APPLICATION_ID) return

        val prefs = runBlocking { hyperFocusDataStore.current() }
        if (!prefs.isActive) return
        if (pkg !in prefs.blockedPackages) return
        if (RewardEngine.isUnlockActive(prefs)) return

        val now = System.currentTimeMillis()
        if (pkg == lastBlockedPackage && (now - lastBlockTime) < BLOCK_DEBOUNCE_MS) {
            return
        }

        lastBlockedPackage = pkg
        lastBlockTime = now
        Log.d(TAG, "Blocking app (polling): $pkg")

        val intent = Intent(this, HyperFocusActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("blocked_package", pkg)
        }
        startActivity(intent)
    }

    private fun getForegroundAppFromUsageStats(): String? {
        return try {
            val usageStatsManager = getSystemService(android.content.Context.USAGE_STATS_SERVICE)
                as? android.app.usage.UsageStatsManager
            val now = System.currentTimeMillis()
            val stats = usageStatsManager?.queryUsageStats(
                android.app.usage.UsageStatsManager.INTERVAL_BEST,
                now - 2000L,
                now
            )
            stats?.maxByOrNull { it.lastTimeUsed }?.packageName
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AppBlockingService destroyed")
        heartbeatScope.cancel()
    }
}
