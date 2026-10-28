package com.neuroflow.app.feature.hyperfocus

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neuroflow.app.kiosk.DeviceOwnerKioskManager
import com.neuroflow.app.feature.hyperfocus.gate.GateTaskScreen
import com.neuroflow.app.feature.hyperfocus.gate.GatewayScreen
import com.neuroflow.app.feature.hyperfocus.screens.AllowlistSettingsScreen
import com.neuroflow.app.feature.hyperfocus.screens.BlockingOverlayScreen
import com.neuroflow.app.feature.hyperfocus.screens.HyperFocusActivationSheet
import com.neuroflow.app.feature.hyperfocus.screens.PlanningPromptScreen
import com.neuroflow.app.feature.hyperfocus.screens.RewardsScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class HyperFocusActivity : ComponentActivity() {

    private var navController: NavHostController? = null

    // Read by the composition, so a reused instance can show the app the newest intent named.
    private var blockedPackage by mutableStateOf("")

    override fun onResume() {
        super.onResume()
        DeviceOwnerKioskManager.syncLockTaskMode(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // The enforcer starts this activity with SINGLE_TOP, so an open gate or gateway does not get
        // torn down when a bounce arrives — it gets a new intent. Without setIntent the composition
        // keeps reading the original extras and shows the wrong app, or the wrong screen, indefinitely.
        setIntent(intent)
        applyIntent(intent)
    }

    private fun applyIntent(intent: Intent) {
        blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE).orEmpty()
        val controller = navController ?: return
        controller.navigate(intent.getStringExtra(EXTRA_START_DESTINATION) ?: DESTINATION_OVERLAY) {
            popUpTo(controller.graph.id) { inclusive = true }
        }
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        applyIntent(intent)

        // Intercept back gesture across all Android versions (API 26+)
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentRoute = navController?.currentBackStackEntry?.destination?.route
                if (currentRoute == DESTINATION_OVERLAY) {
                    // Consume back gesture on blocking overlay so user can't return to blocked app
                } else {
                    if (navController?.popBackStack() == false) {
                        finish()
                    }
                }
            }
        })

        setContent {
            val controller = rememberNavController().also { navController = it }
            val startDestination =
                intent.getStringExtra(EXTRA_START_DESTINATION) ?: DESTINATION_OVERLAY
            val viewModel: HyperFocusViewModel = hiltViewModel()
            var showActivationSheet by remember { mutableStateOf(false) }

            // The gate logs which app it was opened for, and the log row is written by a screen two
            // navigations away. Naming the app here, before anything is rendered, is what survives it.
            // Unconditional: a bounce always names the app it bounced, and the black home names
            // nothing — leaving the previous bounce's name in the store would log a pass for an app
            // the user never asked for.
            if (startDestination == DESTINATION_GATE) {
                LaunchedEffect(blockedPackage) { viewModel.beginGate(blockedPackage) }
            }

            NavHost(navController = controller, startDestination = startDestination) {
                composable(DESTINATION_OVERLAY) {
                    BlockingOverlayScreen(blockedPackage, viewModel, controller)
                }
                composable(DESTINATION_PLANNING) {
                    PlanningPromptScreen(
                        viewModel = viewModel,
                        onDismiss = {
                            controller.popBackStack()
                        }
                    )
                }
                composable(DESTINATION_GATE) {
                    GateTaskScreen(
                        viewModel = viewModel,
                        onPassed = { controller.navigate(DESTINATION_GATEWAY) },
                        onBack = { finish() }
                    )
                }
                composable(DESTINATION_GATEWAY) {
                    GatewayScreen(
                        viewModel = viewModel,
                        // Straight into the app: the route would need a field to carry the package the
                        // gateway chose, and launchApp already finishes this activity.
                        onLaunch = { packageName -> launchApp(packageName) },
                        onBack = { finish() }
                    )
                }
                composable(DESTINATION_LAUNCH) {
                    // Transparent screen — immediately launches the granted app and finishes
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                    LaunchedEffect(Unit) {
                        launchApp(blockedPackage)
                    }
                }
                // The same editor the More menu opens, reached with no session and no launcher to hand
                // off to — the state where it is the only useful thing on the screen. Back is finish():
                // the gesture callback above already eats the system back.
                composable(DESTINATION_SETUP) {
                    AllowlistSettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { finish() },
                        onStartSession = { showActivationSheet = true }
                    )
                }
                composable(DESTINATION_REWARDS) {
                    RewardsScreen(
                        onNavigateBack = { controller.popBackStack() },
                        viewModel = viewModel
                    )
                }
            }
            if (showActivationSheet) {
                HyperFocusActivationSheet(
                    viewModel = viewModel,
                    onDismiss = { showActivationSheet = false }
                )
            }
        }
    }

    fun launchApp(packageName: String) {
        if (packageName.isBlank()) {
            finish()
            return
        }
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(launchIntent)
        }
        finish()
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
        const val EXTRA_START_DESTINATION = "start_destination"

        // No destination string literal outside this companion: a typo in a route name is a blank
        // screen at runtime, not a compile error. SETUP is named here and routed by Task 16.
        const val DESTINATION_OVERLAY = "blocking_overlay"
        const val DESTINATION_PLANNING = "planning_prompt"
        const val DESTINATION_GATE = "gate"
        const val DESTINATION_GATEWAY = "gateway"
        const val DESTINATION_LAUNCH = "launch_app"
        const val DESTINATION_SETUP = "setup"
        const val DESTINATION_REWARDS = "rewards"
    }
}
