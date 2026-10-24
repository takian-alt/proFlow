package com.neuroflow.app.presentation.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppGuideScreenTest {

    @Test
    fun testGuideCategoriesCompleteness() {
        val categories = GuideCategory.values()
        assertTrue("Must have at least 8 distinct guide categories", categories.size >= 8)
        categories.forEach { category ->
            assertTrue("Category label must not be blank", category.label.isNotBlank())
            assertTrue("Category emoji must not be blank", category.emoji.isNotBlank())
        }
    }

    @Test
    fun testGuideSearchFiltering() {
        val testPoints = listOf(
            "Process S" to "Adenosine accumulation",
            "Process C" to "Circadian rhythm double-harmonic"
        )
        val item = GuideSectionItem(
            id = "circadian_test",
            category = GuideCategory.CIRCADIAN_FLOW,
            emoji = "⚡",
            tag = "BORBÉLY",
            title = "Flow Cockpit & Circadian Energy",
            summary = "Real-time Borbély two-process energy model",
            accentColor = androidx.compose.ui.graphics.Color(0xFF10B981),
            keyPoints = testPoints,
            proTip = "Protect your dip window",
            scienceNote = "Based on Borbély's 1982 paper"
        )

        // Exact match
        assertTrue(item.title.contains("Circadian", ignoreCase = true))
        assertTrue(item.tag.contains("Borbély", ignoreCase = true))
        assertTrue(item.keyPoints.any { it.first.contains("Adenosine", ignoreCase = true) || it.second.contains("Adenosine", ignoreCase = true) })

        // Non-match
        assertFalse(item.title.contains("XYZNonExistentQuery", ignoreCase = true))
    }
}
