package com.neuroflow.app.presentation.launcher.hyperfocus

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neuroflow.app.presentation.launcher.hyperfocus.screens.BlockingOverlayScreen
import com.neuroflow.app.presentation.launcher.hyperfocus.screens.CodeEntryScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class HyperFocusActivity : ComponentActivity() {

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Back press is intentionally disabled during Hyper Focus
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val blockedPackage = intent.getStringExtra("blocked_package") ?: ""
            val viewModel: HyperFocusViewModel = hiltViewModel()
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = "blocking_overlay"
            ) {
                composable("blocking_overlay") {
                    BlockingOverlayScreen(blockedPackage, viewModel, navController)
                }
                composable("code_entry") {
                    CodeEntryScreen(viewModel, navController)
                }
            }
        }
    }
}
