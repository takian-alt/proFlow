package com.neuroflow.app.feature.hyperfocus.home

import android.content.ComponentName
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.neuroflow.app.feature.hyperfocus.HyperFocusActivity
import com.neuroflow.app.kiosk.DeviceOwnerKioskManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicBoolean

@AndroidEntryPoint
class HyperFocusHomeActivity : ComponentActivity() {

    // A field, not a call inside onResume: hiltViewModel() needs the activity's
    // LocalViewModelStoreOwner and has none when invoked from a lifecycle callback body.
    private val viewModel: HyperFocusHomeViewModel by viewModels()
    private val handoffGuard = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.resolveHandoff()
        setContent {
            val state by viewModel.state.collectAsState()
            val target by viewModel.handoffTarget.collectAsState()
            val resolved by viewModel.handoffResolved.collectAsState()
            val hiddenApps by viewModel.hiddenApps.collectAsState()
            val emergencyAvailable by viewModel.emergencyAvailable.collectAsState()
            val endTask by viewModel.endGateTask.collectAsState()
            // The exit gate is a surface of its own rather than a dialog over the grid: the grid
            // behind it would still be showing essentials while the user argues with a task.
            var showingEndGate by remember { mutableStateOf(false) }
            // The hand-off is driven off the collected pair, not off onResume: resolveHandoff()
            // is a coroutine, so on a cold start the target arrives after onResume has finished.
            LaunchedEffect(state, target) {
                if (state is HomeSurfaceState.HandOff && target != null) handOffIfAllowed()
            }
            if (showingEndGate) {
                EndSessionGateScreen(
                    task = endTask,
                    onSubmit = { answer, reason, mismatches, elapsed ->
                        val ended = viewModel.submitEndAttempt(answer, reason, mismatches, elapsed)
                        if (ended) showingEndGate = false
                        ended
                    },
                    onCancel = { showingEndGate = false }
                )
            } else {
                HyperFocusHomeScreen(
                    state = state,
                    canHandOff = target != null,
                    handoffResolved = resolved,
                    hiddenApps = hiddenApps,
                    emergencyAvailable = emergencyAvailable,
                    onOpenEssential = viewModel::open,
                    onOpenGate = { openHyperFocus(HyperFocusActivity.DESTINATION_GATE) },
                    onEmergencyOpen = viewModel::emergencyOpen,
                    onDeactivate = { showingEndGate = true },
                    onOpenAllowlistEditor = { openHyperFocus(HyperFocusActivity.DESTINATION_SETUP) },
                    onOpenHomeAppSettings = viewModel::openHomeAppSettings
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        DeviceOwnerKioskManager.syncLockTaskMode(this)
        handoffGuard.set(false)
        viewModel.resolveHandoff()
        viewModel.refresh()
    }

    override fun onPause() {
        super.onPause()
        handoffGuard.set(false)
    }

    private fun openHyperFocus(startDestination: String) {
        startActivity(
            Intent(this, HyperFocusActivity::class.java)
                .putExtra(HyperFocusActivity.EXTRA_START_DESTINATION, startDestination)
        )
    }

    private fun handOffIfAllowed() {
        val target = viewModel.handoffTarget.value ?: return
        if (target.packageName == packageName) return
        if (!handoffGuard.compareAndSet(false, true)) return
        runCatching {
            // setComponent is load-bearing: a bare HOME intent re-resolves through the
            // persistent-preferred rule, which pins HOME at *this* package, and the hand-off would
            // deliver the intent straight back here and loop.
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .setComponent(target)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            // New task, not clear task: CLEAR_TASK wipes the launcher's stack, so every hand-off
            // cold-starts it and the swap is visible as a rebuild rather than a resume.
            // This window has no business staying behind the launcher it just resumed.
            finish()
        }.onFailure {
            handoffGuard.set(false)
            Log.w(TAG, "Hand-off to ${componentKey(target)} failed", it)
        }
    }

    companion object {
        private const val TAG = "HyperFocusHome"
    }
}
