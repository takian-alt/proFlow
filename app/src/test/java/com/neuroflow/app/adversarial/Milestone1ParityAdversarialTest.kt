package com.neuroflow.app.adversarial

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.neuroflow.app.e2e.harness.NeuroFlowV2Harness.NavigationOracle
import com.neuroflow.app.presentation.notes.NoteColors
import com.neuroflow.app.presentation.v2.navigation.NavHubsV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Empirical Adversarial Test Suite for Milestone 1: 100% V1 Feature Parity & Navigation Connectivity.
 *
 * Verifies:
 * 1. Clean resolution of all 11 target routes ('notes', 'weekly_schedule', 'analytics', 'history',
 *    'priority_weights', 'account_sync', 'hyperfocus_allowlist', 'privacy_permissions', 'app_guide',
 *    'identity', 'log_time') declared in NeuroFlowAppV2.kt.
 * 2. Static source AST audit verifying composable route definitions and back-navigation in NeuroFlowAppV2.kt.
 * 3. LocalIsDarkTheme provides true verification in NeuroFlowThemeV2.kt.
 * 4. Mathematical relative luminance and WCAG 2.1 contrast ratios for all NoteColors items under isDark=true:
 *    - Relative luminance L <= 0.05 (sensory obsidian dark cards).
 *    - Zero pastel glare (L < 0.10).
 *    - TextPrimary (#F8FAFC) on dark surface >= 7.0:1 (WCAG AAA).
 *    - TextSecondary (#94A3B8) on dark surface >= 4.5:1 (WCAG AA).
 * 5. Anti-case proof: under isDark=false, light pastels produce high luminance glare (L > 0.50) and contrast breaks (< 2.0:1).
 * 6. NoteColors boundary robustness (null, empty, whitespace, unknown, case-insensitivity, adversarial injection).
 * 7. Verification that invalid/bogus routes do not resolve cleanly.
 */
class Milestone1ParityAdversarialTest {

    private val targetParityRoutes = listOf(
        "notes",
        "weekly_schedule",
        "analytics",
        "history",
        "priority_weights",
        "account_sync",
        "hyperfocus_allowlist",
        "privacy_permissions",
        "app_guide",
        "identity",
        "log_time"
    )

    // =========================================================================
    // SECTION 1: Route Resolution & Navigation Stress Tests
    // =========================================================================

    @Test
    fun testEveryTargetRouteResolvesCleanlyInNavigationContract() {
        for (route in targetParityRoutes) {
            assertTrue(
                "Target parity route '$route' must be valid and registered in NavigationOracle",
                NavigationOracle.isValidRoute(route)
            )

            val backDest = NavigationOracle.getBackDestination(route)
            assertNotNull("Back destination for '$route' must not be null", backDest)
            assertTrue("Back destination must be a valid hub", NavigationOracle.isValidRoute(backDest))
        }
    }

    @Test
    fun testParameterizedFocusRoutesResolveCleanly() {
        val sampleTaskIds = listOf(
            "task-42",
            "task_abc-123_uuid",
            "c3b4a2f8-9e1d-4f6a-8b2c-123456789abc",
            "task.with.dots",
            "123456"
        )

        for (taskId in sampleTaskIds) {
            val route = NavigationOracle.focusRouteFor(taskId)
            assertTrue(
                "Focus route '$route' must be valid",
                NavigationOracle.isValidRoute(route)
            )
        }
    }

    @Test
    fun testUnregisteredAndMalformedRoutesFailResolution() {
        val malformedRoutes = listOf(
            "unknown_dead_end",
            "notes_v2",
            "weekly_calendar",
            "analytics_dashboard",
            "settings/advanced",
            "flow/invalid",
            "",
            "   ",
            "null",
            "undefined"
        )

        for (badRoute in malformedRoutes) {
            assertFalse(
                "Malformed/unregistered route '$badRoute' must NOT resolve as valid",
                NavigationOracle.isValidRoute(badRoute)
            )
        }
    }

    @Test
    fun testNeuroFlowAppV2SourceContainsAllDeclaredParityRoutes() {
        val sourceFile = File("src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt")
        val altFile = File("app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt")
        val target = if (sourceFile.exists()) sourceFile else altFile
        assertTrue("NeuroFlowAppV2.kt source file must exist", target.exists())
        val content = target.readText()

        for (route in targetParityRoutes) {
            val pattern = "composable(\"$route\")"
            assertTrue(
                "NeuroFlowAppV2.kt must explicitly declare '$pattern'",
                content.contains(pattern)
            )
        }

        // Verify root hubs and parameterized routes
        assertTrue("Must declare flow hub", content.contains("NavHubsV2.Flow.route"))
        assertTrue("Must declare plan hub", content.contains("NavHubsV2.Plan.route"))
        assertTrue("Must declare insights hub", content.contains("NavHubsV2.Insights.route"))
        assertTrue("Must declare focus_v2/{taskId}", content.contains("\"focus_v2/{taskId}\""))
        assertTrue("Must declare backward-compatible legacy focus route", content.contains("\"focus/{taskId}?skipped={skipped}\""))
    }

    @Test
    fun testNeuroFlowThemeV2SourceProvidesLocalIsDarkThemeTrue() {
        val themeFile = File("src/main/java/com/neuroflow/app/presentation/v2/theme/NeuroFlowThemeV2.kt")
        val altFile = File("app/src/main/java/com/neuroflow/app/presentation/v2/theme/NeuroFlowThemeV2.kt")
        val target = if (themeFile.exists()) themeFile else altFile
        assertTrue("NeuroFlowThemeV2.kt source file must exist", target.exists())
        val content = target.readText()

        assertTrue(
            "NeuroFlowThemeV2 must provide LocalIsDarkTheme provides true",
            content.contains("LocalIsDarkTheme provides true")
        )
    }

    // =========================================================================
    // SECTION 2: Dark Note Colors Luminance & Glare Avoidance Math
    // =========================================================================

    private fun calculateLuminance(color: Color): Double {
        fun channel(c: Float): Double {
            val cd = c.toDouble()
            return if (cd <= 0.03928) cd / 12.92 else ((cd + 0.055) / 1.055).pow(2.4)
        }
        val r = channel(color.red)
        val g = channel(color.green)
        val b = channel(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun calculateContrastRatio(c1: Color, c2: Color): Double {
        val l1 = calculateLuminance(c1)
        val l2 = calculateLuminance(c2)
        val brighter = max(l1, l2)
        val darker = min(l1, l2)
        return (brighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun testAllNoteColorsInDarkThemeProduceSensoryDarkSurfacesWithoutGlare() {
        val allItems = NoteColors.items
        assertTrue("NoteColors palette must not be empty", allItems.isNotEmpty())

        val textPrimary = NeuroFlowThemeV2Colors.StaticTextPrimary // #F8FAFC
        val textSecondary = NeuroFlowThemeV2Colors.StaticTextSecondary // #94A3B8

        for (item in allItems) {
            val darkColor = NoteColors.getColor(item.id, isDark = true)
            assertEquals("Under isDark=true, must return item.darkColor for ${item.id}", item.darkColor, darkColor)

            val darkLum = calculateLuminance(darkColor)

            // Sensory Obsidian requirement: Dark card background luminance must be very low (<= 0.05)
            assertTrue(
                "Note dark color for ${item.id} luminance ($darkLum) must be <= 0.05 (calm sensory surface)",
                darkLum <= 0.05
            )

            // Strictly no glare (glare threshold is > 0.10)
            assertTrue(
                "Note dark color for ${item.id} must have no glare (L < 0.10, actual: $darkLum)",
                darkLum < 0.10
            )

            // WCAG 2.1 AAA Contrast Ratio with TextPrimary (>= 7.0:1)
            val contrastPrimary = calculateContrastRatio(textPrimary, darkColor)
            assertTrue(
                "TextPrimary on ${item.id} dark surface must meet WCAG AAA >= 7.0:1 (actual: $contrastPrimary)",
                contrastPrimary >= 7.0
            )

            // WCAG 2.1 AA Contrast Ratio with TextSecondary (>= 4.5:1)
            val contrastSecondary = calculateContrastRatio(textSecondary, darkColor)
            assertTrue(
                "TextSecondary on ${item.id} dark surface must meet WCAG AA >= 4.5:1 (actual: $contrastSecondary)",
                contrastSecondary >= 4.5
            )
        }
    }

    @Test
    fun testAntiStressLightColorsExhibitHighPastelGlareAndFailDarkThemeContrast() {
        // Empirically demonstrates why isDark=true is strictly required in NeuroFlowThemeV2:
        // Light pastel colors have excessive luminance and cause washed-out unreadable text if accidentally used.
        val textPrimary = NeuroFlowThemeV2Colors.StaticTextPrimary // #F8FAFC (white-ish)

        for (item in NoteColors.items) {
            val lightColor = NoteColors.getColor(item.id, isDark = false)
            val lightLum = calculateLuminance(lightColor)

            // Light pastel colors have high luminance (> 0.50)
            assertTrue(
                "Light pastel ${item.id} exhibits high luminance glare (actual: $lightLum)",
                lightLum > 0.50
            )

            // TextPrimary (white) on light pastel gives terrible contrast (< 2.0:1, failing all standards)
            val brokenContrast = calculateContrastRatio(textPrimary, lightColor)
            assertTrue(
                "White TextPrimary on light pastel ${item.id} produces unreadable glare (contrast $brokenContrast < 2.0:1)",
                brokenContrast < 2.0
            )
        }
    }

    // =========================================================================
    // SECTION 3: NoteColors Adversarial Input & Robustness Fuzzing
    // =========================================================================

    @Test
    fun testNoteColorsAdversarialFallbackRobustness() {
        val defaultDark = NoteColors.items.first().darkColor // DEFAULT dark: Color(0xFF1E1E24)

        val adversarialColorIds = listOf(
            null,
            "",
            "   ",
            "NON_EXISTENT",
            "NEON_PINK",
            "<script>alert(1)</script>",
            "' OR '1'='1",
            "\u0000\u0007\u001B",
            "🔥🌈🚀",
            "A".repeat(1000)
        )

        for (id in adversarialColorIds) {
            val resolved = NoteColors.getColor(id, isDark = true)
            assertNotNull("Adversarial id '$id' must not resolve to null", resolved)
            assertEquals("Adversarial id '$id' must fall back safely to DEFAULT dark color", defaultDark, resolved)
        }
    }

    @Test
    fun testNoteColorsCaseInsensitivity() {
        val testCases = listOf(
            "default" to "DEFAULT",
            "butter" to "BUTTER",
            "Butter" to "BUTTER",
            "bUtTeR" to "BUTTER",
            "peach" to "PEACH",
            "cOrAl" to "CORAL",
            "MINT" to "mint",
            "teal" to "TEAL",
            "sKy" to "SKY",
            "LaVeNdEr" to "LAVENDER",
            "rose" to "ROSE"
        )

        for ((input, expectedId) in testCases) {
            val resolved = NoteColors.getColor(input, isDark = true)
            val expected = NoteColors.getColor(expectedId, isDark = true)
            assertEquals("Color resolution for '$input' must match '$expectedId'", expected, resolved)
        }
    }
}
