package com.neuroflow.app.presentation.v2.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.neuroflow.app.presentation.analytics.AnalyticsScreen
import com.neuroflow.app.presentation.focus.FocusScreen
import com.neuroflow.app.presentation.plan.PlanScreen
import com.neuroflow.app.presentation.settings.SettingsScreen
import com.neuroflow.app.presentation.v2.flow.FlowScreenV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors

@Composable
fun NeuroFlowAppV2(
    onInitialTaskConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

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
                        onStartFocus = { taskId -> navController.navigate("focus/$taskId") }
                    )
                }

                composable(NavHubsV2.Plan.route) {
                    PlanScreen(
                        onOpenMoreMenu = { },
                        onNavigateToFocus = { taskId -> navController.navigate("focus/$taskId") },
                        onNavigateToQuadrant = { /* matrix quadrant view */ },
                        onNavigateToSettings = { navController.navigate("settings") }
                    )
                }

                composable(NavHubsV2.Insights.route) {
                    AnalyticsScreen()
                }

                composable("settings") {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPriorityWeights = { }
                    )
                }

                composable(
                    route = "focus/{taskId}?skipped={skipped}",
                    arguments = listOf(
                        navArgument("taskId") { type = NavType.StringType },
                        navArgument("skipped") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { backStackEntry ->
                    val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                    FocusScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToNextTask = { nextTaskId, newSkipped ->
                            navController.navigate("focus/$nextTaskId?skipped=$newSkipped") {
                                popUpTo("focus/$taskId") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
