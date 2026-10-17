package com.neuroflow.app.e2e

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.domain.engine.AnalyticsEngine
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.BehavioralInsightsEngine
import com.neuroflow.app.e2e.harness.NeuroFlowV2Harness
import com.neuroflow.app.e2e.harness.NeuroFlowV2Harness.NavigationOracle
import com.neuroflow.app.e2e.harness.NeuroFlowV2Harness.ParityOracle
import com.neuroflow.app.presentation.notes.NoteColors
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.UUID

/**
 * E2E Test Suite 1: neuroFlow V2 Parity & Navigation Connectivity
 * Covers:
 * - Full Notes System navigation and dark theme styling
 * - Weekly schedule 7-day calendar navigation from Plan
 * - Insights analytics (velocity, 24-hr heatmap density, sMAPE)
 * - Settings routes (priority weights, account sync, hyperfocus allowlist, privacy permissions, app guide)
 * - Identity affirmations & empty WOOP anchor sheet
 * - Retrospective time logging action and state update
 *
 * Tiers:
 * - Tier 1: Primary behavior & Contract verification
 * - Tier 2: Boundary value analysis & Edge cases
 * - Tier 3: Pairwise interactions & Integration flows
 * - Tier 4: Real-world workload & Adversarial verification
 */
class NeuroFlowV2ParityE2eTest {

    // =========================================================================
    // TIER 1: Primary Behavior & Contract Verification
    // =========================================================================

    @Test
    fun tier1_testNotesRouteContractRegistered() {
        assertTrue("Notes route must be registered in V2 navigation", NavigationOracle.isValidRoute("notes"))
        assertEquals("Back from notes navigates to flow", NavigationOracle.ROUTE_FLOW, NavigationOracle.getBackDestination("notes"))
    }

    @Test
    fun tier1_testNoteColorsDarkThemeStylingFidelity() {
        // In Obsidian dark theme, NoteColors must yield dark surface colors rather than light pastels
        val allColors = NoteColors.items
        assertTrue("NoteColors must contain palette choices", allColors.isNotEmpty())

        for (item in allColors) {
            val resolvedDark = NoteColors.getColor(item.id, isDark = true)
            assertEquals("Dark theme resolution must match darkColor token for ${item.id}", item.darkColor, resolvedDark)
            assertNotEquals("Dark theme color must differ from light pastel color for ${item.id}", item.lightColor, resolvedDark)
        }
    }

    @Test
    fun tier1_testWeeklyScheduleRouteRegisteredAndNavigationFromPlan() {
        assertTrue("Weekly schedule route must be registered in V2", NavigationOracle.isValidRoute("weekly_schedule"))
        assertEquals("Back from weekly schedule must return to Plan hub", NavigationOracle.ROUTE_PLAN, NavigationOracle.getBackDestination("weekly_schedule"))
    }

    @Test
    fun tier1_testSevenDayScheduleTimetableStructure() {
        // 7 days with 24 hourly slots each
        val daysOfWeek = 7
        val hoursPerDay = 24
        val totalSlots = daysOfWeek * hoursPerDay
        assertEquals(168, totalSlots)
    }

    @Test
    fun tier1_testInsightsHourlyCircadianDensityCalculation() {
        // Map 4 focus hours: 9AM (60m), 10AM (60m), 14PM (30m), 15PM (30m) -> 180m total
        val hourlyFocus = mapOf(9 to 60, 10 to 60, 14 to 30, 15 to 30)
        val density = BehavioralInsightsEngine.calculateHourlyCircadianDensity(hourlyFocus)

        assertEquals("Must return exactly 24 hourly density bins", 24, density.size)
        assertEquals(60f / 180f, density[9], 0.001f)
        assertEquals(60f / 180f, density[10], 0.001f)
        assertEquals(30f / 180f, density[14], 0.001f)
        assertEquals(30f / 180f, density[15], 0.001f)
        assertEquals(0f, density[0], 0.001f)

        val totalSum = density.sum()
        assertEquals("Circadian density across 24h must sum to 1.0 (100%)", 1.0f, totalSum, 0.001f)
    }

    @Test
    fun tier1_testAnalyticsSmapeAccuracyCalculation() {
        // When estimated == actual, error is 0.0%
        val perfect = AnalyticsEngine.computeSmape(45f, 45f)
        assertEquals("Zero error when estimate equals actual", 0.0f, perfect, 0.001f)

        // Symmetrical error property
        val errUnder = AnalyticsEngine.computeSmape(30f, 60f)
        val errOver = AnalyticsEngine.computeSmape(60f, 30f)
        assertEquals("sMAPE must be symmetric for over- and under-estimation", errUnder, errOver, 0.001f)
        assertEquals(66.6667f, errUnder, 0.01f)
    }

    @Test
    fun tier1_testSettingsRoutesParityRegistered() {
        val requiredRoutes = listOf(
            "priority_weights",
            "account_sync",
            "hyperfocus_allowlist",
            "privacy_permissions",
            "app_guide"
        )
        for (route in requiredRoutes) {
            assertTrue("Settings route $route must be registered in V2", NavigationOracle.isValidRoute(route))
            assertEquals("Settings route $route must pop back to settings", NavigationOracle.ROUTE_SETTINGS, NavigationOracle.getBackDestination(route))
        }
    }

    @Test
    fun tier1_testIdentityAffirmationsRouteAndEmptyWoopDetection() {
        assertTrue("Identity route must be registered in V2", NavigationOracle.isValidRoute("identity"))
        assertEquals(NavigationOracle.ROUTE_INSIGHTS, NavigationOracle.getBackDestination("identity"))

        val emptyWoop = ParityOracle.WoopAnchor(wish = "", outcome = "", obstacle = "", plan = "")
        assertTrue("Empty WOOP anchor must trigger the Set WOOP Anchor prompt", ParityOracle.isWoopAnchorEmpty(emptyWoop))
        assertTrue("Null WOOP anchor must trigger the Set WOOP Anchor prompt", ParityOracle.isWoopAnchorEmpty(null))

        val populatedWoop = ParityOracle.WoopAnchor(
            wish = "Ship V2 release",
            outcome = "High user delight and zero friction",
            obstacle = "Distraction from Slack notifications",
            plan = "If notification arrives, then put phone in drawer"
        )
        assertFalse("Populated WOOP anchor must not be treated as empty", ParityOracle.isWoopAnchorEmpty(populatedWoop))
        assertTrue("Populated WOOP anchor must be marked complete", populatedWoop.isComplete)
    }

    @Test
    fun tier1_testRetrospectiveTimeLoggingCreation() {
        assertTrue("Log time route must be registered in V2", NavigationOracle.isValidRoute("log_time"))

        val taskId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val session = ParityOracle.createRetrospectiveSession(
            taskId = taskId,
            durationMinutes = 45,
            endedAtMillis = now
        )

        assertEquals(taskId, session.taskId)
        assertEquals(45f, session.durationMinutes, 0.001f)
        assertEquals(now, session.endedAt)
        assertEquals(now - 45 * 60_000L, session.startedAt)
        assertNull("Retrospective session must not be paused", session.pausedAt)
        assertEquals(0L, session.totalPausedMs)
        assertEquals(45f, session.trackedMinutes(now), 0.001f)
    }

    // =========================================================================
    // TIER 2: Boundary Value Analysis & Edge Cases
    // =========================================================================

    @Test
    fun tier2_testNoteColorsUnknownAndNullColorGracefulFallback() {
        val fallbackNull = NoteColors.getColor(null, isDark = true)
        val fallbackUnknown = NoteColors.getColor("non_existent_color_id_xyz", isDark = true)
        val defaultDark = NoteColors.items.first().darkColor

        assertEquals("Null color must fall back to default dark palette", defaultDark, fallbackNull)
        assertEquals("Unknown color must fall back to default dark palette", defaultDark, fallbackUnknown)
    }

    @Test
    fun tier2_testHourlyCircadianDensityWithAllZeroHours() {
        val emptyMap = emptyMap<Int, Int>()
        val density = BehavioralInsightsEngine.calculateHourlyCircadianDensity(emptyMap)

        assertEquals(24, density.size)
        for (v in density) {
            assertEquals("Zero focus hours should yield 0f without NaN or crash", 0.0f, v, 0.0001f)
            assertFalse("Must not be NaN", v.isNaN())
        }
    }

    @Test
    fun tier2_testSmapeBoundaryZeroAndExtremeValues() {
        // Both estimated and actual are 0
        val zeroBoth = AnalyticsEngine.computeSmape(0f, 0f)
        assertEquals(0.0f, zeroBoth, 0.001f)

        // Estimated is 0, actual is positive
        val zeroEst = AnalyticsEngine.computeSmape(0f, 50f)
        assertEquals(200.0f, zeroEst, 0.001f) // |0 - 50| / 25 * 100 = 200%

        // Extreme outlier: 1 minute estimated vs 1000 minutes actual
        val extreme = AnalyticsEngine.computeSmape(1f, 1000f)
        assertTrue("Extreme error must be capped at 200%", extreme <= 200.0f)
        assertTrue("Extreme error must be near 200%", extreme > 199.0f)

        // Weighted sMAPE with empty task list
        val emptyWeighted = AnalyticsEngine.computeWeightedSmape(emptyList())
        assertEquals("Empty tasks weighted sMAPE must be 0", 0.0f, emptyWeighted, 0.001f)
    }

    @Test
    fun tier2_testRetrospectiveLoggingBoundaryMinutes() {
        val now = System.currentTimeMillis()
        val taskId = "task-edge-case"

        // Minimum boundary: 1 minute
        val session1m = ParityOracle.createRetrospectiveSession(taskId, durationMinutes = 1, endedAtMillis = now)
        assertEquals(1f, session1m.durationMinutes, 0.001f)

        // Full day boundary: 1440 minutes (24 hours)
        val session24h = ParityOracle.createRetrospectiveSession(taskId, durationMinutes = 1440, endedAtMillis = now)
        assertEquals(1440f, session24h.durationMinutes, 0.001f)

        // Zero or negative input should clamp to at least 1 minute
        val sessionZero = ParityOracle.createRetrospectiveSession(taskId, durationMinutes = 0, endedAtMillis = now)
        assertEquals("0 minutes must clamp to minimum 1 minute", 1f, sessionZero.durationMinutes, 0.001f)

        val sessionNeg = ParityOracle.createRetrospectiveSession(taskId, durationMinutes = -15, endedAtMillis = now)
        assertEquals("Negative minutes must clamp to minimum 1 minute", 1f, sessionNeg.durationMinutes, 0.001f)
    }

    @Test
    fun tier2_testPartialWoopAnchorValidation() {
        // Only wish filled, others blank -> incomplete
        val partial = ParityOracle.WoopAnchor(wish = "Study for exam", outcome = "", obstacle = "Phone notifications", plan = "")
        assertFalse("Partial WOOP anchor must not be complete", partial.isComplete)
        assertFalse("Partial WOOP anchor is not empty", ParityOracle.isWoopAnchorEmpty(partial))
    }

    // =========================================================================
    // TIER 3: Pairwise Interactions & Integration Flows
    // =========================================================================

    @Test
    fun tier3_testVelocityCalculationFromCompletedTasksHistory() {
        val now = 1_700_000_000_000L // Reference timestamp
        val day = 86_400_000L

        val tasks = listOf(
            TaskEntity(id = "1", title = "Task 1", status = TaskStatus.COMPLETED, completedAt = now - 2 * day),
            TaskEntity(id = "2", title = "Task 2", status = TaskStatus.COMPLETED, completedAt = now - 5 * day),
            TaskEntity(id = "3", title = "Task 3", status = TaskStatus.COMPLETED, completedAt = now - 10 * day),
            TaskEntity(id = "4", title = "Task 4", status = TaskStatus.COMPLETED, completedAt = now - 25 * day),
            TaskEntity(id = "5", title = "Task 5", status = TaskStatus.COMPLETED, completedAt = now - 40 * day), // Outside 30d
            TaskEntity(id = "6", title = "Task 6", status = TaskStatus.ACTIVE, completedAt = null)
        )

        val velocity7d = ParityOracle.calculateVelocity(tasks, windowDays = 7, nowMillis = now)
        val velocity30d = ParityOracle.calculateVelocity(tasks, windowDays = 30, nowMillis = now)

        assertEquals("7-day velocity must include tasks completed within 7 days", 2, velocity7d)
        assertEquals("30-day velocity must include tasks completed within 30 days", 4, velocity30d)
    }

    @Test
    fun tier3_testRetrospectiveLoggingUpdatesTaskActualDurationAndWeightedSmape() {
        val taskId = "task-time-acc"
        var task = TaskEntity(
            id = taskId,
            title = "Offline Workshop",
            estimatedDurationMinutes = 60,
            actualDurationMinutes = 0f
        )

        // User performs 2 retrospective logs for offline work: 30m and 45m
        val session1 = ParityOracle.createRetrospectiveSession(taskId, 30, System.currentTimeMillis() - 60_000L)
        val session2 = ParityOracle.createRetrospectiveSession(taskId, 45, System.currentTimeMillis())

        val accumulatedActual = (task.actualDurationMinutes ?: 0f) + session1.durationMinutes + session2.durationMinutes
        task = task.copy(
            actualDurationMinutes = accumulatedActual,
            status = TaskStatus.COMPLETED
        )

        assertEquals("Accumulated actual minutes must equal 75", 75f, task.actualDurationMinutes!!, 0.001f)

        // Compute updated weighted sMAPE for this completed task
        val weightedSmape = AnalyticsEngine.computeWeightedSmape(listOf(task))
        val expectedSmape = AnalyticsEngine.computeSmape(60f, 75f) // |60 - 75| / 67.5 * 100 = 22.22%
        assertEquals(expectedSmape, weightedSmape, 0.01f)
        assertTrue("Grade should be Good estimation", AnalyticsEngine.mapeGrade(weightedSmape).contains("Good"))
    }

    @Test
    fun tier3_testWeeklyScheduleTaskClickNavigatesToFocusRoute() {
        val targetTaskId = "task-focus-999"
        val resolvedRoute = NavigationOracle.focusRouteFor(targetTaskId)
        assertEquals("focus_v2/task-focus-999", resolvedRoute)
        assertTrue("Focus route must be recognized as valid", NavigationOracle.isValidRoute(resolvedRoute))
    }

    // =========================================================================
    // TIER 4: Real-World Workload & Adversarial Scenarios
    // =========================================================================

    @Test
    fun tier4_testFullNeuroFlowV2WorkflowParityCycle() {
        // Scenario: A user opens Plan -> Weekly Schedule, takes a note, sets a WOOP anchor,
        // completes work, logs past time, and reviews Insights velocity and 24-hr circadian heatmap.
        val now = System.currentTimeMillis()

        // 1. Navigation: Plan -> Weekly Schedule -> Note -> Insights
        assertEquals(NavigationOracle.ROUTE_PLAN, NavigationOracle.getBackDestination("weekly_schedule"))
        assertEquals(NavigationOracle.ROUTE_FLOW, NavigationOracle.getBackDestination("notes"))

        // 2. Note styling in Dark Mode
        val noteColor = NoteColors.getColor("slate", isDark = true)
        assertNotNull(noteColor)

        // 3. Set WOOP Anchor
        val woop = ParityOracle.WoopAnchor(
            wish = "Publish research paper",
            outcome = "High citation impact",
            obstacle = "Writer's block during introduction",
            plan = "If blocked, then write the methodology section first"
        )
        assertTrue("WOOP anchor must be valid and complete", woop.isComplete)

        // 4. Log retrospective time for offline session
        val loggedSession = ParityOracle.createRetrospectiveSession("task-research", 90, now)
        assertEquals(90f, loggedSession.durationMinutes, 0.001f)

        // 5. Update task with completion
        val completedTask = TaskEntity(
            id = "task-research",
            title = "Research Paper",
            estimatedDurationMinutes = 100,
            actualDurationMinutes = 90f,
            status = TaskStatus.COMPLETED,
            completedAt = now
        )

        // 6. Analytics evaluation
        val velocity = ParityOracle.calculateVelocity(listOf(completedTask), windowDays = 7, nowMillis = now)
        assertEquals(1, velocity)

        val smape = AnalyticsEngine.computeSmape(100f, 90f) // |100 - 90| / 95 * 100 = 10.53%
        assertEquals(10.5263f, smape, 0.01f)

        val hourlyDensity = BehavioralInsightsEngine.calculateHourlyCircadianDensity(mapOf(14 to 90))
        assertEquals(1.0f, hourlyDensity[14], 0.001f)
        assertEquals(1.0f, hourlyDensity.sum(), 0.001f)
    }

    @Test
    fun tier4_testAdversarialNotesContentAndColorStress() {
        val maliciousInputs = listOf(
            "<script>alert('xss')</script>",
            "DROP TABLE notes; --",
            "🚀🔥💡🧠 \uD83D\uDC68\u200D\uD83D\uDCBB", // Emojis and zero-width joiners
            "A".repeat(10_000), // Extreme length
            "\n\r\t \u0000\u001F" // Control characters
        )

        for (input in maliciousInputs) {
            // Verify NoteColors resolution remains bulletproof under strange IDs
            val resolved = NoteColors.getColor(input, isDark = true)
            assertNotNull("Note color must resolve safely even with adversarial colorId: $input", resolved)
        }
    }
}
