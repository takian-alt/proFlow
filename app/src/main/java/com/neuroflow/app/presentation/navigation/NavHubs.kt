package com.neuroflow.app.presentation.navigation

sealed class NavHub(val route: String, val title: String) {
    data object Flow : NavHub("flow", "Flow")
    data object Plan : NavHub("plan", "Plan")
    data object Insights : NavHub("insights", "Insights")

    companion object {
        val ALL = listOf(Flow, Plan, Insights)

        fun resolveDeepLink(uri: String): String {
            val target = uri.substringAfter("://", uri).lowercase()
            return when {
                target.contains("schedule") || target.contains("plan") -> Plan.route
                target.contains("analytics") || target.contains("insights") -> Insights.route
                target.contains("today") || target.contains("flow") -> Flow.route
                else -> Flow.route
            }
        }
    }
}
