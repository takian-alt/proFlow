package com.neuroflow.app.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavHubsTest {

    @Test
    fun testCoreThreeHubsDefined() {
        assertEquals(3, NavHub.ALL.size)
        val routes = NavHub.ALL.map { it.route }
        assertTrue(routes.contains("flow"))
        assertTrue(routes.contains("plan"))
        assertTrue(routes.contains("insights"))
    }

    @Test
    fun testDeepLinkResolution() {
        assertEquals("flow", NavHub.resolveDeepLink("neuroflow://today"))
        assertEquals("flow", NavHub.resolveDeepLink("neuroflow://flow"))
        assertEquals("plan", NavHub.resolveDeepLink("neuroflow://schedule"))
        assertEquals("plan", NavHub.resolveDeepLink("neuroflow://plan"))
        assertEquals("insights", NavHub.resolveDeepLink("neuroflow://analytics"))
        assertEquals("insights", NavHub.resolveDeepLink("neuroflow://insights"))
        assertEquals("flow", NavHub.resolveDeepLink("neuroflow://unknown_fallback"))
    }
}
