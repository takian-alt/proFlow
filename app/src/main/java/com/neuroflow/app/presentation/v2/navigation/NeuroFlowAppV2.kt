package com.neuroflow.app.presentation.v2.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neuroflow.app.presentation.settings.SettingsScreen
import com.neuroflow.app.presentation.v2.flow.FlowScreenV2
import com.neuroflow.app.presentation.v2.focus.FocusScreenV2
import com.neuroflow.app.presentation.v2.insights.InsightsScreenV2
import com.neuroflow.app.presentation.v2.plan.PlanScreenV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors

@Composable
fun NeuroFlowAppV2(
    initialTaskId: String? = null,
    onInitialTaskConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(initialTaskId) {
        if (!initialTaskId.isNullOrBlank()) {
            navController.navigate("focus_v2/$initialTaskId")
            onInitialTaskConsumed()
        }
    }

    NeuroFlowThemeV2 {
        Scaffold(
            containerColor = NeuroFlowThemeV2Colors.Obsidian,
            bottomBar = {
                NavigationBar(
                    containerColor = NeuroFlowThemeV2Colors.SlateSurface,
                    contentColor = NeuroFlowThemeV2Colors.TextPrimary
                ) {
                    NavHubsV2.items.forEach { hub ->
                        val selected = currentDestination?.hierarchy?.any { it.route == hub.route } == true
                        NavigationBarItem(
                            icon = { Icon(hub.icon, contentDescription = hub.title) },
                            label = { Text(hub.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(hub.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                                selectedTextColor = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                indicatorColor = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                                unselectedIconColor = NeuroFlowThemeV2Colors.TextSecondary,
                                unselectedTextColor = NeuroFlowThemeV2Colors.TextSecondary
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = NavHubsV2.Flow.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(NavHubsV2.Flow.route) {
                    FlowScreenV2(
                        onNavigateToSettings = { navController.navigate("settings") },
                        onStartFocus = { taskId -> navController.navigate("focus_v2/$taskId") }
                    )
                }

                composable(NavHubsV2.Plan.route) {
                    PlanScreenV2(
                        onNavigateToFocus = { taskId -> navController.navigate("focus_v2/$taskId") }
                    )
                }

                composable(NavHubsV2.Insights.route) {
                    InsightsScreenV2()
                }

                composable("settings") {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPriorityWeights = { }
                    )
                }

                composable(
                    route = "focus_v2/{taskId}",
                    arguments = listOf(
                        navArgument("taskId") { type = NavType.StringType }
                    )
                ) {
                    FocusScreenV2(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToNextTask = { nextTaskId ->
                            navController.navigate("focus_v2/$nextTaskId") {
                                popUpTo(NavHubsV2.Flow.route)
                            }
                        }
                    )
                }

                // Backward-compatible fallback for legacy focus route
                composable(
                    route = "focus/{taskId}?skipped={skipped}",
                    arguments = listOf(
                        navArgument("taskId") { type = NavType.StringType },
                        navArgument("skipped") { type = NavType.StringType; defaultValue = "" }
                    )
                ) {
                    FocusScreenV2(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToNextTask = { nextTaskId ->
                            navController.navigate("focus_v2/$nextTaskId") {
                                popUpTo(NavHubsV2.Flow.route)
                            }
                        }
                    )
                }
            }
        }
    }
}

