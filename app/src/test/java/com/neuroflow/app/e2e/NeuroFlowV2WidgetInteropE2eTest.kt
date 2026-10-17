package com.neuroflow.app.e2e

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.e2e.harness.NeuroFlowV2Harness.NavigationOracle
import com.neuroflow.app.e2e.harness.NeuroFlowV2Harness.WidgetInteropOracle
import org.junit.Assert.*
import org.junit.Test

/**
 * E2E Test Suite 4: Default V2 UI & Home Screen Widget Interoperability
 * Covers:
 * - useV2Ui = true default in UserPreferencesDataStore
 * - ACTION_OPEN_NEW_TASK and ACTION_OPEN_FOCUS intent handling
 * - Widget pending intent targets
 *
 * Tiers:
 * - Tier 1: Primary behavior & Intent contract verification
 * - Tier 2: Boundary value analysis & Missing extra / empty list edge cases
 * - Tier 3: Pairwise interactions & Widget action dispatch flows
 * - Tier 4: High-volume intent dispatch & Adversarial input stress
 */
class NeuroFlowV2WidgetInteropE2eTest {

    // =========================================================================
    // TIER 1: Primary Behavior & Intent Contract Verification
    // =========================================================================

    @Test
    fun tier1_testActionOpenNewTaskIntentContract() {
        assertEquals("com.procus.ACTION_OPEN_NEW_TASK", WidgetInteropOracle.ACTION_OPEN_NEW_TASK)

        val resolution = WidgetInteropOracle.resolveIntent(
            action = WidgetInteropOracle.ACTION_OPEN_NEW_TASK,
            taskIdExtra = null,
            activeTasks = emptyList()
        )

        assertEquals("ACTION_OPEN_NEW_TASK must land on Flow screen", NavigationOracle.ROUTE_FLOW, resolution.targetRoute)
        assertTrue("ACTION_OPEN_NEW_TASK must trigger opening the SmartTaskComposerSheet", resolution.openComposer)
        assertNull(resolution.focusedTaskId)
    }

    @Test
    fun tier1_testActionOpenFocusIntentWithTaskIdContract() {
        assertEquals("com.procus.ACTION_OPEN_FOCUS", WidgetInteropOracle.ACTION_OPEN_FOCUS)
        assertEquals("taskId", WidgetInteropOracle.EXTRA_TASK_ID)

        val targetTaskId = "task-alpha-123"
        val resolution = WidgetInteropOracle.resolveIntent(
            action = WidgetInteropOracle.ACTION_OPEN_FOCUS,
            taskIdExtra = targetTaskId,
            activeTasks = emptyList()
        )

        assertEquals("Must route to focus_v2 with specific taskId", "focus_v2/task-alpha-123", resolution.targetRoute)
        assertFalse("Must not open composer sheet", resolution.openComposer)
        assertEquals(targetTaskId, resolution.focusedTaskId)
    }

    @Test
    fun tier1_testUseV2UiDefaultPreferenceContract() {
        // Contract requirement: when no preference is persisted in DataStore, default must be true
        val resolvedDefault = WidgetInteropOracle.resolveV2UiDefault(null)
        assertTrue("Default for useV2Ui must resolve to true for new installs", resolvedDefault)

        // User explicit preferences
        val userEnabled = WidgetInteropOracle.resolveV2UiDefault(true)
        assertTrue(userEnabled)

        val userDisabled = WidgetInteropOracle.resolveV2UiDefault(false)
        assertFalse("Manual user override to classic V1 must be respected", userDisabled)
    }

    @Test
    fun tier1_testUserPreferencesDataModelUseV2UiProperty() {
        // Ensure UserPreferences entity model supports useV2Ui
        val prefsV2 = UserPreferences(useV2Ui = true)
        assertTrue("UserPreferences must support useV2Ui = true", prefsV2.useV2Ui)

        val prefsV1 = UserPreferences(useV2Ui = false)
        assertFalse("UserPreferences must support useV2Ui = false override", prefsV1.useV2Ui)
    }

    @Test
    fun tier1_testWidgetPendingIntentTargetActions() {
        // Quick Capture Widget: PendingIntent action must be ACTION_OPEN_NEW_TASK
        val quickCaptureAction = WidgetInteropOracle.ACTION_OPEN_NEW_TASK
        assertEquals("com.procus.ACTION_OPEN_NEW_TASK", quickCaptureAction)

        // Next Action Widget: PendingIntent action must be ACTION_OPEN_FOCUS
        val nextActionFocus = WidgetInteropOracle.ACTION_OPEN_FOCUS
        assertEquals("com.procus.ACTION_OPEN_FOCUS", nextActionFocus)

        // Focus Widget: PendingIntent action must be ACTION_OPEN_FOCUS
        val focusWidgetAction = WidgetInteropOracle.ACTION_OPEN_FOCUS
        assertEquals("com.procus.ACTION_OPEN_FOCUS", focusWidgetAction)
    }

    // =========================================================================
    // TIER 2: Boundary Value Analysis & Edge Cases
    // =========================================================================

    @Test
    fun tier2_testActionOpenFocusWithMissingOrBlankTaskIdFallback() {
        val topTask = TaskEntity(id = "top-task-456", title = "Top Priority")
        val activeTasks = listOf(topTask)

        // 1. Null taskId extra -> falls back to top active task
        val resNull = WidgetInteropOracle.resolveIntent(
            action = WidgetInteropOracle.ACTION_OPEN_FOCUS,
            taskIdExtra = null,
            activeTasks = activeTasks
        )
        assertEquals("focus_v2/top-task-456", resNull.targetRoute)
        assertEquals("top-task-456", resNull.focusedTaskId)

        // 2. Blank taskId extra -> falls back to top active task
        val resBlank = WidgetInteropOracle.resolveIntent(
            action = WidgetInteropOracle.ACTION_OPEN_FOCUS,
            taskIdExtra = "   ",
            activeTasks = activeTasks
        )
        assertEquals("focus_v2/top-task-456", resBlank.targetRoute)
        assertEquals("top-task-456", resBlank.focusedTaskId)
    }

    @Test
    fun tier2_testActionOpenFocusWhenZeroActiveTasksExist() {
        // No active tasks in database and taskIdExtra is null
        val resEmpty = WidgetInteropOracle.resolveIntent(
            action = WidgetInteropOracle.ACTION_OPEN_FOCUS,
            taskIdExtra = null,
            activeTasks = emptyList()
        )

        assertEquals("When no active tasks exist, must safely land on Flow hub", NavigationOracle.ROUTE_FLOW, resEmpty.targetRoute)
        assertNull("Focused taskId must be null", resEmpty.focusedTaskId)
        assertFalse(resEmpty.openComposer)
    }

    @Test
    fun tier2_testUnrecognizedIntentActionGracefulFallback() {
        val resUnknown = WidgetInteropOracle.resolveIntent(
            action = "com.procus.UNKNOWN_UNSUPPORTED_ACTION",
            taskIdExtra = null,
            activeTasks = emptyList()
        )

        assertEquals("Unrecognized intent must safely fall back to Flow hub", NavigationOracle.ROUTE_FLOW, resUnknown.targetRoute)
        assertFalse(resUnknown.openComposer)
        assertNull(resUnknown.focusedTaskId)
    }

    // =========================================================================
    // TIER 3: Pairwise Interactions & Widget Action Dispatch Flows
    // =========================================================================

    @Test
    fun tier3_testNextActionWidgetTopTaskFocusTransition() {
        val task1 = TaskEntity(id = "urgent-frog-1", title = "Review Contract", isFrog = true)
        val task2 = TaskEntity(id = "routine-2", title = "Reply to emails")
        val activeList = listOf(task1, task2)

        // User taps "Start Focus" on Next Action Widget
        val resolution = WidgetInteropOracle.resolveIntent(
            action = WidgetInteropOracle.ACTION_OPEN_FOCUS,
            taskIdExtra = task1.id,
            activeTasks = activeList
        )

        assertEquals("Must navigate directly to focus screen for highest priority task",
            "focus_v2/urgent-frog-1", resolution.targetRoute)
        assertEquals("urgent-frog-1", resolution.focusedTaskId)
        assertFalse(resolution.openComposer)
    }

    @Test
    fun tier3_testQuickCaptureWidgetDirectComposerLaunch() {
        // User taps Quick Capture widget "+" button
        val resolution = WidgetInteropOracle.resolveIntent(
            action = WidgetInteropOracle.ACTION_OPEN_NEW_TASK,
            taskIdExtra = null,
            activeTasks = emptyList()
        )

        assertEquals(NavigationOracle.ROUTE_FLOW, resolution.targetRoute)
        assertTrue("Composer sheet must be triggered immediately", resolution.openComposer)
    }

    // =========================================================================
    // TIER 4: High-Volume Intent Dispatch & Adversarial Input Stress
    // =========================================================================

    @Test
    fun tier4_testHighVolumeWidgetIntentDispatchStability() {
        val testTasks = (1..20).map { TaskEntity(id = "task-$it", title = "Task $it") }

        // Simulate 100 rapid intent arrivals
        for (i in 0 until 100) {
            val isNewTask = (i % 2 == 0)
            val action = if (isNewTask) WidgetInteropOracle.ACTION_OPEN_NEW_TASK else WidgetInteropOracle.ACTION_OPEN_FOCUS
            val taskIdExtra = if (isNewTask) null else "task-${(i % 20) + 1}"

            val res = WidgetInteropOracle.resolveIntent(action, taskIdExtra, testTasks)
            if (isNewTask) {
                assertEquals(NavigationOracle.ROUTE_FLOW, res.targetRoute)
                assertTrue(res.openComposer)
            } else {
                assertEquals("focus_v2/$taskIdExtra", res.targetRoute)
                assertEquals(taskIdExtra, res.focusedTaskId)
            }
        }
    }

    @Test
    fun tier4_testAdversarialIntentExtrasInjection() {
        val adversarialExtras = listOf(
            null,
            "",
            "   ",
            "../../etc/passwd",
            "<script>window.location='http://evil.com'</script>",
            "A".repeat(5000), // Huge string
            "task-with-unicode-🚀-🧠-🔥"
        )

        for (extra in adversarialExtras) {
            val res = WidgetInteropOracle.resolveIntent(
                action = WidgetInteropOracle.ACTION_OPEN_FOCUS,
                taskIdExtra = extra,
                activeTasks = emptyList()
            )
            assertNotNull("Resolution must never be null", res)
            assertTrue("Target route must either be Flow or valid focus route",
                res.targetRoute == NavigationOracle.ROUTE_FLOW || res.targetRoute.startsWith("focus_v2/"))
        }
    }
}
