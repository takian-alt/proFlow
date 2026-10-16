package com.neuroflow.app.presentation.v2.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavHubsV2(val route: String, val title: String, val icon: ImageVector) {
    data object Flow : NavHubsV2("flow_v2", "Flow", Icons.Default.Today)
    data object Plan : NavHubsV2("plan_v2", "Plan", Icons.Default.CalendarMonth)
    data object Insights : NavHubsV2("insights_v2", "Insights", Icons.Default.TrendingUp)

    companion object {
        val items = listOf(Flow, Plan, Insights)
    }
}
