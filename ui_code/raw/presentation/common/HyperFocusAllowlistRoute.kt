package com.neuroflow.app.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.neuroflow.app.feature.hyperfocus.HyperFocusViewModel
import com.neuroflow.app.feature.hyperfocus.screens.AllowlistSettingsScreen
import com.neuroflow.app.feature.hyperfocus.screens.HyperFocusActivationSheet

@Composable
fun HyperFocusAllowlistRoute(onNavigateBack: () -> Unit) {
    val viewModel: HyperFocusViewModel = hiltViewModel()
    var showActivationSheet by remember { mutableStateOf(false) }

    AllowlistSettingsScreen(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
        onStartSession = { showActivationSheet = true }
    )
    if (showActivationSheet) {
        HyperFocusActivationSheet(
            viewModel = viewModel,
            onDismiss = { showActivationSheet = false }
        )
    }
}
