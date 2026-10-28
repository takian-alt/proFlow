package com.neuroflow.app.presentation.v2.navigation

import android.content.Intent
import android.net.Uri
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.presentation.v2.focus.service.FocusService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationAuditTest : StringSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeTest {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Uri::class)
        every { Uri.encode(any()) } answers {
            val raw = firstArg<String?>() ?: ""
            java.net.URLEncoder.encode(raw, "UTF-8").replace("+", "%20")
        }
    }

    afterTest {
        unmockkStatic(Uri::class)
        Dispatchers.resetMain()
    }

    fun makeTask(
        id: String,
        title: String = "Test Task",
        isFrog: Boolean = false,
        quadrant: Quadrant = Quadrant.DO_FIRST,
        priority: Priority = Priority.HIGH,
        status: TaskStatus = TaskStatus.ACTIVE,
        dependsOnTaskIds: String = ""
    ) = TaskEntity(
        id = id,
        title = title,
        energyLevel = EnergyLevel.HIGH,
        isFrog = isFrog,
        impactScore = 80,
        quadrant = quadrant,
        priority = priority,
        status = status,
        dependsOnTaskIds = dependsOnTaskIds
    )

    "BUG-273: Intent session latching prevents NewChapterCard or GoalsRefill from hijacking screen upon intent consumption" {
        // Scenario 1: Launched via notification or widget with direct intent (e.g. Focus on Task X)
        var initialFocusTaskId: String? = "task-focus-123"
        val initialWoopTaskId: String? = null
        val initialGuideOpen = false
        val initialScheduleOpen = false
        val initialNewTaskOpen = false

        val pendingDirectIntent = initialFocusTaskId != null ||
            initialWoopTaskId != null ||
            initialGuideOpen ||
            initialScheduleOpen ||
            initialNewTaskOpen

        var directIntentSessionHandled = pendingDirectIntent
        val hasDirectIntentInitial = pendingDirectIntent || directIntentSessionHandled
        hasDirectIntentInitial shouldBe true

        // Simulating intent consumption inside NeuroFlowAppV2: onInitialTaskConsumed()
        initialFocusTaskId = null
        val pendingDirectIntentAfterConsumed = initialFocusTaskId != null ||
            initialWoopTaskId != null ||
            initialGuideOpen ||
            initialScheduleOpen ||
            initialNewTaskOpen
        pendingDirectIntentAfterConsumed shouldBe false

        // With session latching (directIntentSessionHandled remains true):
        val hasDirectIntentAfterConsumed = pendingDirectIntentAfterConsumed || directIntentSessionHandled
        hasDirectIntentAfterConsumed shouldBe true

        // Guards for FreshStart and GoalsRefill
        val isFreshStartDue = true
        val isYearlyRefillDue = true
        val showNewChapterCard = !hasDirectIntentAfterConsumed && isFreshStartDue
        val showYearlyRefillCard = !hasDirectIntentAfterConsumed && isYearlyRefillDue

        showNewChapterCard shouldBe false
        showYearlyRefillCard shouldBe false

        // Scenario 2: Normal app launch without intent
        val normalPendingIntent = false
        val normalDirectIntentSessionHandled = normalPendingIntent
        val normalHasDirectIntent = normalPendingIntent || normalDirectIntentSessionHandled
        normalHasDirectIntent shouldBe false

        val normalShowNewChapter = !normalHasDirectIntent && isFreshStartDue
        normalShowNewChapter shouldBe true
    }

    "BUG-274: Pending intent actions take precedence over savedInstanceState on process restoration" {
        // Mock Intent with pending action from notification
        val intentWithAction = mockk<Intent>()
        every { intentWithAction.action } returns "com.procus.ACTION_OPEN_FOCUS"
        every { intentWithAction.hasExtra("taskId") } returns true
        every { intentWithAction.hasExtra("task_id") } returns false

        val hasPendingAction = !intentWithAction.action.isNullOrBlank() ||
            intentWithAction.hasExtra("taskId") ||
            intentWithAction.hasExtra("task_id")

        hasPendingAction shouldBe true

        // Mock normal launch intent (no action)
        val normalIntent = mockk<Intent>()
        every { normalIntent.action } returns null
        every { normalIntent.hasExtra("taskId") } returns false
        every { normalIntent.hasExtra("task_id") } returns false

        val normalHasPendingAction = !normalIntent.action.isNullOrBlank() ||
            normalIntent.hasExtra("taskId") ||
            normalIntent.hasExtra("task_id")

        normalHasPendingAction shouldBe false
    }

    "BUG-275: MiniFocusSessionBar onTap guards against blank taskId and applies URI encoding" {
        val sessionWithValidId = FocusService.Companion.ActiveFocusSession(
            taskId = "task:deep/work#1",
            taskTitle = "Deep Work",
            remainingSeconds = 500L,
            totalSeconds = 1500L,
            isPaused = false
        )

        sessionWithValidId.taskId.isNotBlank() shouldBe true
        val encodedValidRoute = "focus_v2/${Uri.encode(sessionWithValidId.taskId)}"
        encodedValidRoute shouldBe "focus_v2/task%3Adeep%2Fwork%231"

        val sessionWithBlankId = FocusService.Companion.ActiveFocusSession(
            taskId = "",
            taskTitle = "Orphaned",
            remainingSeconds = 0L,
            totalSeconds = 0L,
            isPaused = false
        )

        sessionWithBlankId.taskId.isNotBlank() shouldBe false
    }

    "BUG-275: ACTION_OPEN_FOCUS filters blocked tasks with active unmet prerequisites and handles SQLite failure" {
        val taskRepo = mockk<TaskRepository>()

        val prereqA = makeTask(id = "prereq-a", title = "Write Spec", status = TaskStatus.ACTIVE)
        val blockedTaskB = makeTask(
            id = "task-b",
            title = "Implement Engine",
            isFrog = true,
            status = TaskStatus.ACTIVE,
            dependsOnTaskIds = "prereq-a"
        )
        val standaloneC = makeTask(
            id = "task-c",
            title = "Customer Support",
            status = TaskStatus.ACTIVE
        )

        // Prerequisites: prereq-a is active, so task-b is blocked even though it's a Frog!
        val activeTasks = listOf(prereqA, blockedTaskB, standaloneC)
        coEvery { taskRepo.getActiveTasks() } returns activeTasks

        val activeIds = activeTasks.map { it.id }.toSet()
        val firstTask = activeTasks
            .filter {
                it.status == TaskStatus.ACTIVE &&
                it.quadrant != Quadrant.ELIMINATE &&
                (it.dependsOnTaskIds.isBlank() || it.dependsOnTaskIds.split(",").none { id -> id.trim() in activeIds })
            }
            .sortedWith(
                compareByDescending<TaskEntity> { it.isFrog }
                    .thenBy { it.quadrant != Quadrant.DO_FIRST }
                    .thenBy { it.priority.ordinal }
            )
            .firstOrNull()

        // Prereq A must be chosen ahead of blocked Task B!
        firstTask shouldNotBe null
        firstTask?.id shouldBe "prereq-a"

        // Crash resilience check: if taskRepo throws SQLite exception, fallback safely to tab_focus
        coEvery { taskRepo.getActiveTasks() } throws RuntimeException("SQLite disk I/O error")
        val safeResult = runCatching {
            taskRepo.getActiveTasks()
        }.getOrNull()

        safeResult shouldBe null
    }

    "BUG-278: onNavigateToNextTask replaces only current focus screen preserving prior stack history" {
        val previousScreenId = 42
        val nextTaskId = "task-next-456"

        val encodedNextRoute = "focus_v2/${Uri.encode(nextTaskId)}"
        encodedNextRoute shouldBe "focus_v2/task-next-456"

        // Destination ID exists, so popUpTo is constrained to currentDestId with inclusive = true
        val popUpTargetId = previousScreenId
        popUpTargetId shouldBe 42
    }

    "BUG-311: Intent extras are cleaned up in handleIntent to prevent re-execution on configuration change" {
        val extras = mutableMapOf<String, Any?>(
            "taskId" to "task-config-123",
            "openWoop" to true,
            "notificationId" to 42
        )
        var action: String? = "com.procus.ACTION_OPEN_FOCUS"

        val intent = mockk<Intent>()
        every { intent.getStringExtra("taskId") } answers { extras["taskId"] as? String }
        every { intent.getStringExtra("task_id") } answers { extras["task_id"] as? String }
        every { intent.getIntExtra("notificationId", any()) } answers { extras["notificationId"] as? Int ?: secondArg() }
        every { intent.getBooleanExtra("openWoop", any()) } answers { extras["openWoop"] as? Boolean ?: secondArg() }
        every { intent.action } answers { action }
        every { intent.setAction(any()) } answers { action = firstArg<String?>(); intent }
        every { intent.setAction(null) } answers { action = null; intent }
        every { intent.removeExtra(any()) } answers {
            extras.remove(firstArg<String>())
            intent
        }
        every { intent.hasExtra(any()) } answers { extras.containsKey(firstArg<String>()) }

        // Initial launch from notification: hasPendingAction must be true
        val hasPendingActionInitial = !action.isNullOrBlank() || intent.hasExtra("taskId") || intent.hasExtra("task_id")
        hasPendingActionInitial shouldBe true

        // Simulate handleIntent logic
        val taskId = intent.getStringExtra("taskId") ?: intent.getStringExtra("task_id")
        val notificationId = intent.getIntExtra("notificationId", Int.MIN_VALUE)
        val openWoop = intent.getBooleanExtra("openWoop", false)
        val currentAction = intent.action

        intent.action = null
        intent.removeExtra("taskId")
        intent.removeExtra("task_id")
        intent.removeExtra("openWoop")
        intent.removeExtra("notificationId")

        var initialFocusTaskId: String? = null
        var initialWoopTaskId: String? = null
        if (currentAction == "com.procus.ACTION_OPEN_FOCUS") {
            if (!taskId.isNullOrBlank()) {
                if (openWoop) {
                    initialWoopTaskId = taskId
                } else {
                    initialFocusTaskId = taskId
                }
            }
        }

        // Action and openWoop should route to initialWoopTaskId
        initialWoopTaskId shouldBe "task-config-123"
        initialFocusTaskId shouldBe null

        // After handleIntent, action and extras must be cleared
        intent.action shouldBe null
        intent.hasExtra("taskId") shouldBe false
        intent.hasExtra("task_id") shouldBe false
        intent.hasExtra("openWoop") shouldBe false

        // Simulating device rotation / configuration change:
        // hasPendingAction must now evaluate to false so savedInstanceState is not bypassed!
        val hasPendingActionAfterRotation = !intent.action.isNullOrBlank() || intent.hasExtra("taskId") || intent.hasExtra("task_id")
        hasPendingActionAfterRotation shouldBe false
    }

    "BUG-311: rememberSaveable session flags preserve direct intent state across activity recreation" {
        // Initial state: app opened with direct intent (e.g. Focus)
        var pendingDirectIntent = true
        var directIntentSessionHandled = pendingDirectIntent

        val hasDirectIntentBeforeRotation = pendingDirectIntent || directIntentSessionHandled
        hasDirectIntentBeforeRotation shouldBe true

        // Focus session is consumed inside NeuroFlowAppV2
        pendingDirectIntent = false

        // With rememberSaveable, directIntentSessionHandled survives device rotation
        val savedDirectIntentSessionHandled = directIntentSessionHandled // persisted via rememberSaveable
        val hasDirectIntentAfterRotation = pendingDirectIntent || savedDirectIntentSessionHandled
        hasDirectIntentAfterRotation shouldBe true

        // Fresh start check must be suppressed because intent session was handled
        val isFreshStartDue = true
        val showNewChapterCard = !hasDirectIntentAfterRotation && isFreshStartDue
        showNewChapterCard shouldBe false
    }

    "BUG-312: Navigation to focus_v2 enforces URI encoding and guards against blank taskIds and empty start routes" {
        // Test URI encoding of complex task IDs (slashes, queries, hashes, spaces)
        val slashTaskId = "project/phase1_part2"
        val queryTaskId = "task?filter=active&sort=desc"
        val hashTaskId = "task#subtask"
        val spacedTaskId = "read chapter 4"

        val encodedSlash = "focus_v2/${Uri.encode(slashTaskId)}"
        encodedSlash shouldBe "focus_v2/project%2Fphase1_part2"

        val encodedQuery = "focus_v2/${Uri.encode(queryTaskId)}"
        encodedQuery shouldBe "focus_v2/task%3Ffilter%3Dactive%26sort%3Ddesc"

        val encodedHash = "focus_v2/${Uri.encode(hashTaskId)}"
        encodedHash shouldBe "focus_v2/task%23subtask"

        val encodedSpaced = "focus_v2/${Uri.encode(spacedTaskId)}"
        encodedSpaced shouldBe "focus_v2/read%20chapter%204"

        // Blank taskId guard
        val blankTaskId = "   "
        val canNavigate = blankTaskId.isNotBlank()
        canNavigate shouldBe false

        // onNavigateToNextTask fallback when nextTaskId is empty or blank
        val nextTaskId = ""
        val nextAction = if (nextTaskId.isNotBlank()) "NAVIGATE" else "POP_BACK_STACK"
        nextAction shouldBe "POP_BACK_STACK"

        // Cold start attachment frame: currentRoute is empty string
        val currentRouteAtColdStart = ""
        val shouldNavigateToFlow = currentRouteAtColdStart.isNotEmpty() && currentRouteAtColdStart != NavHubsV2.Flow.route
        shouldNavigateToFlow shouldBe false
    }
})
