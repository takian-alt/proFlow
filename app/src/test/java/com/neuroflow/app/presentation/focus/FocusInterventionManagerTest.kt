package com.neuroflow.app.presentation.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class FocusInterventionManagerTest {

    private val hostilePhrases = listOf(
        "dumber", "stupid", "lamao", "lol", "become dumber", "you are still stupid"
    )

    @Test
    fun testAllThreeTiersDefined() {
        assertEquals(3, FocusInterventionTier.entries.size)
        assertTrue(FocusInterventionTier.entries.contains(FocusInterventionTier.ZEN))
        assertTrue(FocusInterventionTier.entries.contains(FocusInterventionTier.SHIELD))
        assertTrue(FocusInterventionTier.entries.contains(FocusInterventionTier.FORTRESS))
    }

    @Test
    fun testAllTiersProvideSupportiveCopy() {
        for (tier in FocusInterventionTier.entries) {
            val intervention = FocusInterventionManager.getTierIntervention(tier)
            assertEquals(tier, intervention.tier)
            assertTrue(intervention.heading.isNotBlank())
            assertTrue(intervention.message.isNotBlank())
            assertTrue(intervention.primaryActionLabel.isNotBlank())
            assertTrue(intervention.dismissActionLabel.isNotBlank())

            val fullText = "${intervention.heading} ${intervention.message}".lowercase(Locale.ROOT)
            for (hostile in hostilePhrases) {
                assertFalse("Tier $tier contains hostile copy: $hostile", fullText.contains(hostile))
            }
        }
    }
}
