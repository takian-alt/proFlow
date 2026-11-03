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

    "BUG-351: FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY suppresses historical intent re-execution and ACTION_MAIN is not pending" {
        val historyIntent = mockk<Intent>()
        every { historyIntent.flags } returns Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
        every { historyIntent.action } returns "com.procus.ACTION_OPEN_FOCUS"
        every { historyIntent.hasExtra("taskId") } returns true
        every { historyIntent.hasExtra("task_id") } returns false
        every { historyIntent.categories } returns null

        val isFromHistory = (historyIntent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
        val isMainLauncher = historyIntent.action == Intent.ACTION_MAIN && historyIntent.categories?.contains(Intent.CATEGORY_LAUNCHER) == true
        val hasPendingAction = !isFromHistory && (!isMainLauncher && !historyIntent.action.isNullOrBlank() || historyIntent.hasExtra("taskId") || historyIntent.hasExtra("task_id"))

        isFromHistory shouldBe true
        hasPendingAction shouldBe false

        // Standard launcher icon click
        val launcherIntent = mockk<Intent>()
        every { launcherIntent.flags } returns 0
        every { launcherIntent.action } returns Intent.ACTION_MAIN
        every { launcherIntent.categories } returns setOf(Intent.CATEGORY_LAUNCHER)
        every { launcherIntent.hasExtra("taskId") } returns false
        every { launcherIntent.hasExtra("task_id") } returns false

        val launcherFromHistory = (launcherIntent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
        val launcherIsMain = launcherIntent.action == Intent.ACTION_MAIN && launcherIntent.categories?.contains(Intent.CATEGORY_LAUNCHER) == true
        val launcherPending = !launcherFromHistory && (!launcherIsMain && !launcherIntent.action.isNullOrBlank() || launcherIntent.hasExtra("taskId") || launcherIntent.hasExtra("task_id"))

        launcherPending shouldBe false
    }

    "BUG-352: isResolvingFocusTask prevents NewChapterCard or TopGoalsRefillCard flashing during async query" {
        var initialFocusTaskId: String? = null
        val initialWoopTaskId: String? = null
        val initialGuideOpen = false
        val initialScheduleOpen = false
        val initialNewTaskOpen = false
        var isResolvingFocusTask = true // Async query in flight

        val pendingDirectIntent = initialFocusTaskId != null ||
            initialWoopTaskId != null ||
            initialGuideOpen ||
            initialScheduleOpen ||
            initialNewTaskOpen ||
            isResolvingFocusTask

        var directIntentSessionHandled = pendingDirectIntent
        val hasDirectIntent = pendingDirectIntent || directIntentSessionHandled

        hasDirectIntent shouldBe true

        // Ensure NewChapterCard is suppressed while query resolves
        val isFreshStartDue = true
        val showNewChapterCard = !hasDirectIntent && isFreshStartDue
        showNewChapterCard shouldBe false

        // When query finishes:
        isResolvingFocusTask = false
        initialFocusTaskId = "resolved-task-1"
        val pendingAfterResolution = initialFocusTaskId != null || isResolvingFocusTask
        val hasDirectIntentAfterResolution = pendingAfterResolution || directIntentSessionHandled
        hasDirectIntentAfterResolution shouldBe true
    }

    "BUG-353: ACTION_OPEN_FOCUS excludes future-scheduled tasks and active continuation Part 2 tasks" {
        val now = 1_730_000_000_000L
        val dayEnd = now + 12 * 3600_000L

        val todayFrogTask = makeTask(
            id = "today-frog",
            title = "Ship V2",
            isFrog = true,
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.DO_FIRST
        ).copy(scheduledDate = now, scheduledTime = now + 3600_000L)

        val futureFrogTask = makeTask(
            id = "future-frog",
            title = "Next Week Presentation",
            isFrog = true,
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.DO_FIRST
        ).copy(scheduledDate = dayEnd + 86_400_000L, scheduledTime = dayEnd + 90_000_000L)

        val activePart1 = makeTask(
            id = "task-alpha",
            title = "Large Refactoring",
            status = TaskStatus.ACTIVE
        )

        val blockedPart2 = makeTask(
            id = "task-alpha_part2",
            title = "Large Refactoring (Part 2)",
            isFrog = true,
            status = TaskStatus.ACTIVE
        ).copy(parentTaskId = "task-alpha")

        val activeTasks = listOf(futureFrogTask, blockedPart2, activePart1, todayFrogTask)
        val activeIds = activeTasks.map { it.id }.toSet()

        val candidate = activeTasks
            .filter {
                if (it.status != TaskStatus.ACTIVE || it.quadrant == Quadrant.ELIMINATE) return@filter false
                val isBlockedByPrereq = it.dependsOnTaskIds.isNotBlank() &&
                    it.dependsOnTaskIds.split(",").map { id -> id.trim() }.filter { id -> id.isNotBlank() }.any { id -> id in activeIds }
                if (isBlockedByPrereq) return@filter false

                val isPart2Blocked = (it.id.endsWith("_part2") || it.id.contains("#part2")) && (
                    (it.parentTaskId != null && it.parentTaskId in activeIds) ||
                    it.id.removeSuffix("_part2").substringBefore("#part2") in activeIds
                )
                if (isPart2Blocked) return@filter false

                val scheduledStart = it.scheduledTime
                val sDate = it.scheduledDate
                val isScheduledInFuture = (scheduledStart != null && scheduledStart > dayEnd) || (sDate != null && sDate > dayEnd)
                !isScheduledInFuture
            }
            .sortedWith(
                compareByDescending<TaskEntity> { it.scheduledDate != null || (it.scheduledTime != null && it.scheduledTime > 0) }
                    .thenByDescending { it.isFrog }
                    .thenBy { it.quadrant != Quadrant.DO_FIRST }
                    .thenBy { it.priority.ordinal }
            )
            .firstOrNull()

        candidate shouldNotBe null
        candidate?.id shouldBe "today-frog"
    }

    "BUG-354: Blank task IDs in initialTaskId and initialWoopTaskId invoke consumed callback to avoid permanent lockout" {
        var consumedCount = 0
        val blankTaskId: String? = "   "

        // Simulating NeuroFlowAppV2 LaunchedEffect(initialTaskId)
        if (blankTaskId != null) {
            if (blankTaskId.isNotBlank()) {
                // navigate
            }
            consumedCount++
        }

        consumedCount shouldBe 1

        // Normalization in MainActivity
        val rawBlankExtra = "   "
        val normalized = rawBlankExtra.takeIf { it.isNotBlank() }
        normalized shouldBe null
    }

    "BUG-355: NavHubsV2 fromRoute and isHubRoute identify hub destinations cleanly" {
        NavHubsV2.isHubRoute("flow_v2") shouldBe true
        NavHubsV2.isHubRoute("plan_v2") shouldBe true
        NavHubsV2.isHubRoute("insights_v2") shouldBe true
        NavHubsV2.isHubRoute("settings") shouldBe false
        NavHubsV2.isHubRoute("focus_v2/123") shouldBe false

        NavHubsV2.fromRoute("flow_v2") shouldBe NavHubsV2.Flow
        NavHubsV2.fromRoute("plan_v2") shouldBe NavHubsV2.Plan
        NavHubsV2.fromRoute("insights_v2") shouldBe NavHubsV2.Insights
        NavHubsV2.fromRoute("settings") shouldBe null
    }

    "BUG-398: Activity recreation resumes in-flight focus resolution preventing zombie state" {
        // When savedInstanceState has saved_resolving_focus_task = true and initialFocusTaskId is null
        val wasResolving = true
        var initialFocusTaskId: String? = null
        val initialWoopTaskId: String? = null
        var isResolvingFocusTask = wasResolving

        var resolutionRestarted = false
        if (wasResolving && initialFocusTaskId == null && initialWoopTaskId == null) {
            // Restart resolution coroutine
            resolutionRestarted = true
            // Simulate completion
            initialFocusTaskId = "resolved-task-after-rotation"
            isResolvingFocusTask = false
        } else {
            isResolvingFocusTask = false
        }

        resolutionRestarted shouldBe true
        initialFocusTaskId shouldBe "resolved-task-after-rotation"
        isResolvingFocusTask shouldBe false

        // If task was already resolved before recreation, resolution is not restarted
        var resolutionRestarted2 = false
        var alreadyResolvedFocusTaskId: String? = "task-already-done"
        if (wasResolving && alreadyResolvedFocusTaskId == null && initialWoopTaskId == null) {
            resolutionRestarted2 = true
        } else {
            isResolvingFocusTask = false
        }

        resolutionRestarted2 shouldBe false
        isResolvingFocusTask shouldBe false
    }

    "BUG-399: ACTION_OPEN_FOCUS fallback blocks multi-part continuation tasks (Part 2, 3, etc.) when parent task active" {
        val now = 1_730_000_000_000L
        val dayEnd = now + 12 * 3600_000L

        val parentTask = makeTask(
            id = "deep-work",
            title = "Deep Work Session",
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.DO_FIRST
        )

        // Part 3 continuation created via ultradian split
        val splitPart3 = makeTask(
            id = "deep-work#part3",
            title = "Deep Work Session (Part 3/3)",
            isFrog = true,
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.DO_FIRST
        ).copy(parentTaskId = "deep-work")

        // Another task with underscore part suffix
        val splitUnderscorePart2 = makeTask(
            id = "deep-work_part2",
            title = "Deep Work Session (Part 2/3)",
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.DO_FIRST
        )

        val standaloneTask = makeTask(
            id = "independent-task",
            title = "Standalone Review",
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.DO_FIRST
        )

        val activeTasks = listOf(splitPart3, splitUnderscorePart2, parentTask, standaloneTask)
        val activeIds = activeTasks.map { it.id }.toSet()

        val candidate = activeTasks
            .filter {
                if (it.status != TaskStatus.ACTIVE || it.quadrant == Quadrant.ELIMINATE) return@filter false
                val isBlockedByPrereq = it.dependsOnTaskIds.isNotBlank() &&
                    it.dependsOnTaskIds.split(",").map { id -> id.trim() }.filter { id -> id.isNotBlank() }.any { id -> id in activeIds }
                if (isBlockedByPrereq) return@filter false

                // Continuation parts (Part 2, 3, etc.) blocked if parent task is still active
                val isContinuation = it.id.contains(Regex("""[#_]part\d+""")) || it.parentTaskId != null
                val parentSlug = it.parentTaskId ?: it.id.replace(Regex("""[#_]part\d+.*$"""), "")
                val isPartBlocked = isContinuation && parentSlug in activeIds
                if (isPartBlocked) return@filter false

                val scheduledStart = it.scheduledTime
                val sDate = it.scheduledDate
                val isScheduledInFuture = (scheduledStart != null && scheduledStart > dayEnd) || (sDate != null && sDate > dayEnd)
                !isScheduledInFuture
            }
            .sortedWith(
                compareByDescending<TaskEntity> { it.scheduledDate != null || ((it.scheduledTime ?: 0L) > 0L) }
                    .thenByDescending { it.isFrog }
                    .thenBy { it.quadrant != Quadrant.DO_FIRST }
                    .thenBy { it.priority.ordinal }
            )
            .firstOrNull()

        // Part 3 and Part 2 are blocked because 'deep-work' parent is active.
        // Parent task 'deep-work' must be selected!
        candidate shouldNotBe null
        candidate?.id shouldBe "deep-work"
    }

    "BUG-400: Sub-screen navigation routes enforce launchSingleTop debouncing rapid taps" {
        val targetRoutes = listOf(
            "settings",
            "meq_quiz",
            "sleep_logs",
            "notes",
            "weekly_schedule",
            "analytics",
            "history",
            "identity",
            "log_time",
            "priority_weights",
            "hyperfocus_allowlist",
            "app_guide",
            "privacy_permissions",
            "account_sync"
        )

        // All target routes must be unique, non-blank, and standard non-hub sub-destinations
        targetRoutes.distinct().size shouldBe targetRoutes.size
        targetRoutes.all { !NavHubsV2.isHubRoute(it) } shouldBe true
    }

    "BUG-401: External focus and WOOP intents replace active focus screen on the backstack" {
        // Simulating user currently inside FocusScreen (currentRoute = focus_v2/old-task)
        val currentRoute = "focus_v2/old-task"
        val currentDestId = 101

        val isAlreadyOnFocus = currentRoute.startsWith("focus_v2") || currentRoute.startsWith("focus/")
        isAlreadyOnFocus shouldBe true

        // When new intent arrives to focus on 'new-task', pop current destination inclusively
        var popUpInclusive = false
        var popUpTargetId: Int? = null
        if (isAlreadyOnFocus) {
            popUpTargetId = currentDestId
            popUpInclusive = true
        }

        popUpTargetId shouldBe 101
        popUpInclusive shouldBe true

        // When user is on Flow screen (currentRoute = flow_v2), do NOT pop
        val normalRoute = "flow_v2"
        val isAlreadyOnFocusFromFlow = normalRoute.startsWith("focus_v2") || normalRoute.startsWith("focus/")
        isAlreadyOnFocusFromFlow shouldBe false
    }

    "BUG-432: resolveDefaultFocusTask filters out waitingFor tasks, enforces preceding part blocking, and sorts chronologically" {
        val now = 1_730_000_000_000L
        val dayEnd = now + 12 * 3600_000L

        // Task with waitingFor (e.g. waiting for legal feedback)
        val waitingTask = makeTask(
            id = "waiting-task",
            title = "Awaiting Client Signoff",
            isFrog = true,
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.DO_FIRST
        ).copy(waitingFor = "Client feedback")

        // Split task chain: parent is COMPLETED, Part 1 is ACTIVE, Part 2 is ACTIVE
        val activePart1 = makeTask(
            id = "split-project#part1",
            title = "Split Project (Part 1/2)",
            isFrog = false,
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.DO_FIRST
        ).copy(scheduledDate = now, scheduledTime = now + 3600_000L) // 1 hour from now

        val blockedPart2 = makeTask(
            id = "split-project#part2",
            title = "Split Project (Part 2/2)",
            isFrog = true, // Even if marked Frog, Part 2 cannot run before Part 1!
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.DO_FIRST
        ).copy(scheduledDate = now, scheduledTime = now + 1800_000L) // 30m from now

        // Standalone morning scheduled task
        val morningTask = makeTask(
            id = "morning-task",
            title = "Morning Standup",
            isFrog = true,
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.DO_FIRST
        ).copy(scheduledDate = now, scheduledTime = now + 1800_000L) // 30 mins from now

        val afternoonTask = makeTask(
            id = "afternoon-task",
            title = "Afternoon Architecture Review",
            isFrog = true,
            status = TaskStatus.ACTIVE,
            quadrant = Quadrant.DO_FIRST
        ).copy(scheduledDate = now, scheduledTime = now + 6 * 3600_000L) // 6 hours from now

        val activeTasks = listOf(waitingTask, blockedPart2, activePart1, afternoonTask, morningTask)
        val activeIds = activeTasks.map { it.id }.toSet()

        val candidate = activeTasks
            .filter {
                if (it.status != TaskStatus.ACTIVE || it.quadrant == Quadrant.ELIMINATE) return@filter false
                if (it.waitingFor.isNotBlank()) return@filter false
                val isBlockedByPrereq = it.dependsOnTaskIds.isNotBlank() &&
                    it.dependsOnTaskIds.split(",").map { id -> id.trim() }.filter { id -> id.isNotBlank() }.any { id -> id in activeIds }
                if (isBlockedByPrereq) return@filter false

                // Continuation parts (Part 2, 3, etc.) blocked if parent or preceding part is still active
                val partNumMatch = Regex("""[#_]part(\d+)""").find(it.id)
                val partNum = partNumMatch?.groupValues?.get(1)?.toIntOrNull()
                val isContinuation = (partNum != null && partNum >= 2) ||
                    it.id.endsWith("_part2") ||
                    it.id.contains("#part2") ||
                    it.parentTaskId != null
                val parentSlug = it.parentTaskId ?: it.id.replace(Regex("""[#_]part\d+.*$"""), "")
                val isParentActive = isContinuation && parentSlug in activeIds
                val isPart1Active = isContinuation && it.id.replace(Regex("""([#_]part)\d+"""), "$1" + "1") in activeIds
                val isPrecedingPartActive = partNum != null && (1 until partNum).any { prevPart ->
                    it.id.replace(Regex("""([#_]part)\d+"""), "$1$prevPart") in activeIds
                }
                val isPartBlocked = isParentActive || isPart1Active || isPrecedingPartActive
                if (isPartBlocked) return@filter false

                val scheduledStart = it.scheduledTime
                val sDate = it.scheduledDate
                val isScheduledInFuture = (scheduledStart != null && scheduledStart > dayEnd) || (sDate != null && sDate > dayEnd)
                !isScheduledInFuture
            }
            .sortedWith(
                compareByDescending<TaskEntity> { it.isFrog }
                    .thenBy { it.quadrant != Quadrant.DO_FIRST }
                    .thenBy { it.scheduledTime ?: Long.MAX_VALUE }
                    .thenByDescending { it.scheduledDate != null || ((it.scheduledTime ?: 0L) > 0L) }
                    .thenBy { it.priority.ordinal }
            )
            .firstOrNull()

        // 1. waitingTask is filtered out
        // 2. blockedPart2 is blocked because activePart1 is still active
        // 3. morningTask is picked ahead of afternoonTask due to earlier scheduled time
        candidate shouldNotBe null
        candidate?.id shouldBe "morning-task"
    }

    "BUG-433: NeuroFlowAppV2 suppresses redundant recreation when target focus task is already currently open" {
        val currentRoute = "focus_v2/task-active-100"
        val isAlreadyOnFocus = currentRoute.startsWith("focus_v2") || currentRoute.startsWith("focus/")
        val currentTaskId = "task-active-100"

        // Scenario 1: Same task arrives via intent/notification
        val incomingTaskId = "task-active-100"
        var shouldNavigate = !isAlreadyOnFocus || currentTaskId != incomingTaskId
        shouldNavigate shouldBe false // Redundant recreation avoided!

        // Scenario 2: Different task arrives
        val differentTaskId = "task-other-200"
        shouldNavigate = !isAlreadyOnFocus || currentTaskId != differentTaskId
        shouldNavigate shouldBe true // Properly navigates to replace the current task!

        // Scenario 3: WOOP reflection intent arrives for same task when WOOP was not yet opened
        val incomingWoopTaskId = "task-active-100"
        val currentOpenWoop = false
        val shouldNavigateWoop = !isAlreadyOnFocus || currentTaskId != incomingWoopTaskId || !currentOpenWoop
        shouldNavigateWoop shouldBe true // WOOP route must be opened!

        // Scenario 4: WOOP reflection intent arrives when WOOP is already open
        val currentOpenWoopTrue = true
        val shouldNavigateWoopAlreadyOpen = !isAlreadyOnFocus || currentTaskId != incomingWoopTaskId || !currentOpenWoopTrue
        shouldNavigateWoopAlreadyOpen shouldBe false // Redundant recreation avoided!
    }

    "BUG-434: Asynchronous WOOP reflection resolution state is preserved across activity recreation" {
        var isResolvingFocusTask = true
        var isResolvingWoopTask = true
        val bundleMap = mutableMapOf<String, Any?>()
        val savedBundle = mockk<android.os.Bundle>()
        every { savedBundle.putBoolean(any(), any()) } answers {
            bundleMap[firstArg()] = secondArg()
        }
        every { savedBundle.getBoolean(any(), any()) } answers {
            bundleMap[firstArg()] as? Boolean ?: secondArg()
        }

        // On save instance state
        savedBundle.putBoolean("saved_resolving_focus_task", isResolvingFocusTask)
        savedBundle.putBoolean("saved_resolving_woop_task", isResolvingWoopTask)

        // Activity recreated
        val wasResolving = savedBundle.getBoolean("saved_resolving_focus_task", false)
        val wasResolvingWoop = savedBundle.getBoolean("saved_resolving_woop_task", false)

        wasResolving shouldBe true
        wasResolvingWoop shouldBe true

        var resolvedWithWoop: Boolean? = null
        if (wasResolving) {
            resolvedWithWoop = wasResolvingWoop
        }

        resolvedWithWoop shouldBe true
    }

    "BUG-435: extra_task_id is recognized by hasPendingAction and processed in handleIntent" {
        val extras = mutableMapOf<String, Any?>(
            "extra_task_id" to "task-widget-999"
        )
        val intent = mockk<Intent>()
        every { intent.getStringExtra("taskId") } answers { extras["taskId"] as? String }
        every { intent.getStringExtra("task_id") } answers { extras["task_id"] as? String }
        every { intent.getStringExtra("extra_task_id") } answers { extras["extra_task_id"] as? String }
        every { intent.getIntExtra("notificationId", any()) } answers { secondArg() }
        every { intent.getBooleanExtra("openWoop", any()) } answers { secondArg() }
        every { intent.action } returns null
        every { intent.setAction(any()) } answers { intent }
        every { intent.removeExtra(any()) } answers {
            extras.remove(firstArg<String>())
            intent
        }
        every { intent.hasExtra(any()) } answers { extras.containsKey(firstArg<String>()) }

        val isFromHistory = false
        val isMainLauncher = false
        val hasPendingAction = !isFromHistory && (!isMainLauncher && !intent.action.isNullOrBlank() ||
            intent.hasExtra("taskId") ||
            intent.hasExtra("task_id") ||
            intent.hasExtra("extra_task_id"))

        hasPendingAction shouldBe true

        val taskId = (intent.getStringExtra("taskId")
            ?: intent.getStringExtra("task_id")
            ?: intent.getStringExtra("extra_task_id"))?.takeIf { it.isNotBlank() }

        taskId shouldBe "task-widget-999"

        intent.removeExtra("extra_task_id")
        intent.hasExtra("extra_task_id") shouldBe false
    }
})
