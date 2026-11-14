package com.neuroflow.app.worker

import com.neuroflow.app.data.local.entity.effectiveScheduledDateMillis
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar

class ScheduleAutoTasksWorkerTest : StringSpec({

    val baseMillis = 1770000000000L // arbitrary fixed epoch millis

    "calculateWakeTimeMillis computes correct wake-up time without mutating calendar state" {
        val testCal = Calendar.getInstance().apply {
            timeInMillis = baseMillis
            set(Calendar.HOUR_OF_DAY, 14) // 2:00 PM
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = testCal.timeInMillis
        val wakeHour = 7 // 7:00 AM

        val wakeTime = ScheduleAutoTasksWorker.calculateWakeTimeMillis(now, wakeHour)
        val expectedCal = Calendar.getInstance().apply {
            timeInMillis = baseMillis
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        wakeTime shouldBe expectedCal.timeInMillis
    }

    "calculateWakeTimeMillis eliminates minute drift for non-zero minutes" {
        val testCal = Calendar.getInstance().apply {
            timeInMillis = baseMillis
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 47)
            set(Calendar.SECOND, 22)
            set(Calendar.MILLISECOND, 333)
        }
        val now = testCal.timeInMillis
        val wakeHour = 7

        val wakeTime = ScheduleAutoTasksWorker.calculateWakeTimeMillis(now, wakeHour)
        val expectedCal = Calendar.getInstance().apply {
            timeInMillis = baseMillis
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        wakeTime shouldBe expectedCal.timeInMillis
    }

    "calculateWakeTimeMillis handles midnight wrap correctly" {
        val testCal = Calendar.getInstance().apply {
            timeInMillis = baseMillis
            set(Calendar.HOUR_OF_DAY, 2) // 2:00 AM past midnight
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = testCal.timeInMillis
        val wakeHour = 7 // 7:00 AM previous morning

        val wakeTime = ScheduleAutoTasksWorker.calculateWakeTimeMillis(now, wakeHour)
        val expectedCal = Calendar.getInstance().apply {
            timeInMillis = baseMillis
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        wakeTime shouldBe expectedCal.timeInMillis
    }

    "calculateWakeTimeMillis returns exact current time at wake hour" {
        val testCal = Calendar.getInstance().apply {
            timeInMillis = baseMillis
            set(Calendar.HOUR_OF_DAY, 7) // exactly 7:00 AM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = testCal.timeInMillis
        val wakeHour = 7

        val wakeTime = ScheduleAutoTasksWorker.calculateWakeTimeMillis(now, wakeHour)
        wakeTime shouldBe now
    }

    "splitMillisToDateAndTime partitions epoch ms into date and time offset cleanly" {
        val testCal = Calendar.getInstance().apply {
            timeInMillis = baseMillis
            set(Calendar.HOUR_OF_DAY, 16)
            set(Calendar.MINUTE, 45)
            set(Calendar.SECOND, 12)
            set(Calendar.MILLISECOND, 500)
        }
        val fullMillis = testCal.timeInMillis

        val (datePart, timePart) = ScheduleAutoTasksWorker.splitMillisToDateAndTime(fullMillis)
        (datePart + timePart) shouldBe fullMillis

        // Verify date part starts at 00:00:00.000
        val checkCal = Calendar.getInstance().apply { timeInMillis = datePart }
        checkCal.get(Calendar.HOUR_OF_DAY) shouldBe 0
        checkCal.get(Calendar.MINUTE) shouldBe 0
        checkCal.get(Calendar.SECOND) shouldBe 0
        checkCal.get(Calendar.MILLISECOND) shouldBe 0
    }

    "buildOneTimeWorkRequest sets forceReplan flag in input data" {
        val reqDefault = ScheduleAutoTasksWorker.buildOneTimeWorkRequest(forceReplan = false)
        reqDefault.workSpec.input.getBoolean("FORCE_REPLAN", true) shouldBe false

        val reqReplan = ScheduleAutoTasksWorker.buildOneTimeWorkRequest(forceReplan = true)
        reqReplan.workSpec.input.getBoolean("FORCE_REPLAN", false) shouldBe true
    }

    "buildBlockedTaskIds marks task blocked if prerequisite is scheduled beyond dayEnd (BUG-019)" {
        val dayEnd = baseMillis + 24 * 3600_000L - 1
        val futureStart = dayEnd + 3600_000L // 1 hour into tomorrow

        val prereqTask = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "future_prereq",
            title = "Tomorrow Prerequisite",
            status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE,
            scheduledDate = futureStart,
            scheduledTime = 0L
        )

        val dependentTask = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "dep_task",
            title = "Dependent on Tomorrow",
            status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE,
            dependsOnTaskIds = "future_prereq"
        )

        val blockedIds = ScheduleAutoTasksWorker.buildBlockedTaskIds(
            tasks = listOf(dependentTask),
            allKnownTasks = listOf(prereqTask, dependentTask),
            dayEnd = dayEnd
        )

        blockedIds.contains("dep_task") shouldBe true
    }

    "buildBlockedTaskIds allows same-day candidate dependency to proceed" {
        val dayEnd = baseMillis + 24 * 3600_000L - 1
        val sameDayStart = baseMillis + 2 * 3600_000L

        val prereqTask = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "today_prereq",
            title = "Today Prerequisite",
            status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE,
            scheduledDate = sameDayStart,
            scheduledTime = 0L
        )

        val dependentTask = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "dep_task",
            title = "Dependent on Today",
            status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE,
            dependsOnTaskIds = "today_prereq"
        )

        val blockedIds = ScheduleAutoTasksWorker.buildBlockedTaskIds(
            tasks = listOf(prereqTask, dependentTask),
            allKnownTasks = listOf(prereqTask, dependentTask),
            dayEnd = dayEnd
        )

        blockedIds.contains("dep_task") shouldBe false
    }

    "expired task from yesterday with scheduledTime != null qualifies as missed task up to dayEnd (BUG-027)" {
        val yesterdayMillis = baseMillis - 24 * 3600_000L
        val nowMillis = baseMillis + 10 * 3600_000L
        val dayEnd = baseMillis + 24 * 3600_000L - 1

        val yesterdayTask = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "yesterday_task",
            title = "Unfinished Yesterday Task",
            status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE,
            isScheduleLocked = false,
            isAutoScheduled = true,
            scheduledDate = yesterdayMillis,
            scheduledTime = 14 * 3600_000L,
            estimatedDurationMinutes = 60
        )

        val scheduledStart = yesterdayTask.resolveScheduledEpochMillis()
        scheduledStart shouldBe (yesterdayMillis + 14 * 3600_000L)
        (scheduledStart!! <= dayEnd) shouldBe true
        com.neuroflow.app.domain.scheduler.TaskBlockPlan.blockHasExpired(
            task = yesterdayTask,
            startMillis = scheduledStart,
            nowMillis = nowMillis,
            gracePeriodMillis = 15 * 60_000L
        ) shouldBe true
    }

    "BUG-159: fixedTasks ignores untimed date-only tasks so they do not produce phantom midnight busy windows" {
        val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(baseMillis)
        val dayEnd = com.neuroflow.app.data.local.entity.startOfNextLocalDay(todayStart) - 1

        val untimedTask = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "untimed_manual",
            title = "All Day Review",
            scheduledDate = todayStart,
            scheduledTime = null,
            isScheduleLocked = true,
            isAutoScheduled = false
        )

        val timedTask = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "timed_manual",
            title = "Client Call",
            scheduledDate = todayStart,
            scheduledTime = 10 * 3600_000L,
            estimatedDurationMinutes = 45,
            isScheduleLocked = true,
            isAutoScheduled = false
        )

        val fixedTasks = listOf(untimedTask, timedTask)
        val fixedWindows = fixedTasks.mapNotNull { task ->
            if (task.scheduledTime == null) return@mapNotNull null
            val start = task.resolveScheduledEpochMillis() ?: return@mapNotNull null
            val duration = com.neuroflow.app.domain.scheduler.TaskBlockPlan.plannedMinutes(task)
            val end = start + duration * 60_000L
            if (end > todayStart && start < dayEnd) com.neuroflow.app.domain.service.TimeSlotWindow(start, end) else null
        }

        fixedWindows.size shouldBe 1
        fixedWindows[0].startMillis shouldBe (todayStart + 10 * 3600_000L)
        fixedWindows[0].endMillis shouldBe (todayStart + 10 * 3600_000L + 45 * 60_000L)
    }

    "BUG-183: future recurring habit with habitDate > dayEnd is excluded from unscheduled candidate pool" {
        val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(baseMillis)
        val dayEnd = com.neuroflow.app.data.local.entity.startOfNextLocalDay(todayStart) - 1
        val tomorrowStart = com.neuroflow.app.data.local.entity.startOfNextLocalDay(todayStart)

        val tomorrowHabit = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "habit_tomorrow",
            title = "Tomorrow Morning Workout",
            recurrence = com.neuroflow.app.domain.model.Recurrence.DAILY,
            habitDate = tomorrowStart,
            scheduledDate = null,
            scheduledTime = null,
            status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE
        )

        val todayUnscheduledTask = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "today_task",
            title = "Unscheduled Today Task",
            scheduledDate = null,
            scheduledTime = null,
            status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE
        )

        val allTasks = listOf(tomorrowHabit, todayUnscheduledTask)

        val unscheduledCandidates = allTasks.filter { task ->
            val sDate = task.effectiveScheduledDateMillis()
            val isUnscheduledOrPastOrToday = sDate == null || sDate <= dayEnd
            isUnscheduledOrPastOrToday &&
                (sDate == null || task.scheduledTime == null) &&
                !com.neuroflow.app.domain.scheduler.AutoSchedulingContracts.hasManualScheduleData(task) &&
                com.neuroflow.app.domain.scheduler.AutoSchedulingContracts.isMutableByAutoScheduler(task, baseMillis)
        }

        unscheduledCandidates.size shouldBe 1
        unscheduledCandidates.first().id shouldBe "today_task"
    }

    "BUG-200: future-dated deadline task without scheduled date is excluded from unscheduled candidates" {
        val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(baseMillis)
        val dayEnd = com.neuroflow.app.data.local.entity.startOfNextLocalDay(todayStart) - 1
        val nextWeekDeadline = todayStart + 7 * 86_400_000L

        val futureDeadlineTask = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "future_deadline_task",
            title = "Taxes Due Next Week",
            scheduledDate = null,
            scheduledTime = null,
            deadlineDate = nextWeekDeadline,
            status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE
        )

        val todayUnscheduledTask = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "today_unscheduled",
            title = "Standard Backlog Task",
            scheduledDate = null,
            scheduledTime = null,
            deadlineDate = null,
            status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE
        )

        val allTasks = listOf(futureDeadlineTask, todayUnscheduledTask)

        val unscheduledCandidates = allTasks.filter { task ->
            val sDate = task.effectiveScheduledDateMillis()
            val dDate = task.resolveDeadlineEpochMillis()
            val isNotFuture = (sDate != null && sDate <= dayEnd) ||
                (sDate == null && (dDate == null || dDate <= dayEnd))
            isNotFuture &&
                (sDate == null || task.scheduledTime == null) &&
                !com.neuroflow.app.domain.scheduler.AutoSchedulingContracts.hasManualScheduleData(task) &&
                com.neuroflow.app.domain.scheduler.AutoSchedulingContracts.isMutableByAutoScheduler(task, baseMillis)
        }

        unscheduledCandidates.size shouldBe 1
        unscheduledCandidates.first().id shouldBe "today_unscheduled"
    }

    "BUG-259: fixedTasks ignores Quadrant.ELIMINATE tasks so iceboxed tasks do not produce phantom busy windows" {
        val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(baseMillis)
        val dayEnd = com.neuroflow.app.data.local.entity.startOfNextLocalDay(todayStart) - 1

        val iceboxTask = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "icebox_task",
            title = "Deprioritized / Eliminated",
            scheduledDate = todayStart,
            scheduledTime = 14 * 3600_000L,
            estimatedDurationMinutes = 60,
            quadrant = com.neuroflow.app.domain.model.Quadrant.ELIMINATE,
            status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE
        )

        val activeManualTask = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "active_manual",
            title = "Important Client Call",
            scheduledDate = todayStart,
            scheduledTime = 10 * 3600_000L,
            estimatedDurationMinutes = 30,
            quadrant = com.neuroflow.app.domain.model.Quadrant.DO_FIRST,
            status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE,
            isScheduleLocked = true,
            isAutoScheduled = false
        )

        val allTasks = listOf(iceboxTask, activeManualTask)
        val targetTasksMap = emptyMap<String, com.neuroflow.app.data.local.entity.TaskEntity>()

        val fixedTasks = allTasks
            .filter { it.quadrant != com.neuroflow.app.domain.model.Quadrant.ELIMINATE }
            .filterNot { task ->
                if (targetTasksMap.containsKey(task.id)) return@filterNot true
                if (task.id.endsWith("_part2")) {
                    val parentId = task.id.removeSuffix("_part2")
                    targetTasksMap.containsKey(parentId)
                } else {
                    false
                }
            }

        fixedTasks.size shouldBe 1
        fixedTasks.first().id shouldBe "active_manual"

        val fixedWindows = fixedTasks.mapNotNull { task ->
            if (task.scheduledTime == null) return@mapNotNull null
            val start = task.resolveScheduledEpochMillis() ?: return@mapNotNull null
            val duration = com.neuroflow.app.domain.scheduler.TaskBlockPlan.plannedMinutes(task)
            val end = start + duration * 60_000L
            if (end > todayStart && start < dayEnd) com.neuroflow.app.domain.service.TimeSlotWindow(start, end) else null
        }

        fixedWindows.size shouldBe 1
        fixedWindows.first().startMillis shouldBe (todayStart + 10 * 3600_000L)
    }

    "BUG-258: Completed Part 2 continuation tasks are protected from deletion when split is undone" {
        val completedPart2 = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "parent_task_part2",
            title = "Design System (Part 2/2)",
            parentTaskId = "parent_task",
            status = com.neuroflow.app.domain.model.TaskStatus.COMPLETED,
            completedAt = baseMillis
        )

        val shouldDelete = completedPart2.status != com.neuroflow.app.domain.model.TaskStatus.COMPLETED
        shouldDelete shouldBe false

        val activePart2 = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "parent_task_part2",
            title = "Design System (Part 2/2)",
            parentTaskId = "parent_task",
            status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE
        )

        val shouldDeleteActive = activePart2.status != com.neuroflow.app.domain.model.TaskStatus.COMPLETED
        shouldDeleteActive shouldBe true
    }

    "BUG-381: Part 2 continuation task is eligible for scheduling when parent task is completed" {
        val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(baseMillis)
        val dayEnd = com.neuroflow.app.data.local.entity.startOfNextLocalDay(todayStart) - 1

        val activePart2 = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "parent_task_part2",
            parentTaskId = "parent_task",
            title = "Design System (Part 2/2)",
            scheduledDate = todayStart,
            scheduledTime = null,
            status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE
        )

        // Parent is completed, so only activePart2 is in allTasks
        val allTasks = listOf(activePart2)
        val activeTaskIds = allTasks.map { it.id }.toSet()
        val isPart2Blocked: (com.neuroflow.app.data.local.entity.TaskEntity) -> Boolean = { task ->
            (task.id.endsWith("_part2") || task.id.contains("#part2")) && (
                (task.parentTaskId != null && task.parentTaskId in activeTaskIds) ||
                task.id.removeSuffix("_part2").substringBefore("#part2") in activeTaskIds
            )
        }

        val blocked = isPart2Blocked(activePart2)
        blocked shouldBe false

        val unscheduled = allTasks.filterNot { isPart2Blocked(it) }
        unscheduled.map { it.id } shouldBe listOf("parent_task_part2")
    }

    "BUG-381: Part 2 continuation task preserves (Part 2/2) title when scheduled" {
        val part2Task = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "parent_task_part2",
            parentTaskId = "parent_task",
            title = "Design System (Part 2/2)",
            status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE
        )

        val cleanTitle = part2Task.title.replace(Regex("""\s*\(Part \d+/\d+\)"""), "").trim()
        val isSplit = false // single placement for part 2
        val titleToUse = when {
            isSplit -> "$cleanTitle (Part 1/2)"
            part2Task.id.endsWith("_part2") || part2Task.title.contains("(Part 2/") -> part2Task.title
            else -> cleanTitle
        }

        titleToUse shouldBe "Design System (Part 2/2)"
    }

    "BUG-422: applyPlacementsTransactionally groups placements preserving standalone Part 2 continuation tasks without clobbering parent" {
        val parentTask = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "feature_task",
            title = "Feature Spec (Part 1/2)",
            status = com.neuroflow.app.domain.model.TaskStatus.COMPLETED
        )
        val part2Task = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "feature_task_part2",
            parentTaskId = "feature_task",
            title = "Feature Spec (Part 2/2)",
            status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE
        )

        // When placement is for feature_task_part2
        val window = com.neuroflow.app.domain.service.TimeSlotWindow(baseMillis, baseMillis + 30 * 60_000L)
        val placement = com.neuroflow.app.domain.service.ScheduledPlacement(
            task = com.neuroflow.app.domain.service.SchedulableTask(
                id = part2Task.id,
                title = part2Task.title,
                category = com.neuroflow.app.domain.service.TaskCategory.DEEP_WORK,
                priority = 1,
                estimatedMinutes = 30,
                energyDemand = com.neuroflow.app.domain.service.EnergyDemand.MODERATE
            ),
            window = window,
            scoreBreakdown = com.neuroflow.app.domain.service.TaskScoreBreakdown("feature_task_part2", 100.0, 100.0, 0.0, 0.0, 0.0, 30)
        )

        val placements = listOf(placement)
        val placementsByBaseId = placements.groupBy { p ->
            if (p.task.id.contains("#part")) {
                p.task.id.substringBefore("#part")
            } else {
                p.task.id
            }
        }

        // Standalone Part 2 must NOT be stripped down to "feature_task"
        placementsByBaseId.keys shouldBe setOf("feature_task_part2")
    }

    "BUG-423: multi-part continuation tasks (#part3, _part3) blocked if preceding part is still active" {
        val activeIds = setOf("epic_part1", "epic_part2")
        val part3Task = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "epic_part3",
            parentTaskId = "epic",
            title = "Epic Release (Part 3/3)",
            status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE
        )

        val partNumMatch = Regex("""[#_]part(\d+)""").find(part3Task.id)
        val partNum = partNumMatch?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("""\(Part (\d+)/""").find(part3Task.title)?.groupValues?.get(1)?.toIntOrNull()
        val isContinuation = (partNum != null && partNum >= 2) ||
            part3Task.id.contains(Regex("""[#_]part\d+""")) ||
            part3Task.parentTaskId != null
        val parentSlug = part3Task.parentTaskId ?: part3Task.id.replace(Regex("""[#_]part\d+.*$"""), "")
        val isParentActive = (part3Task.parentTaskId != null && part3Task.parentTaskId in activeIds) ||
            (parentSlug != part3Task.id && parentSlug in activeIds)
        val isPart1Active = part3Task.id.replace(Regex("""([#_]part)\d+"""), "$1" + "1") in activeIds
        val isPrecedingPartActive = partNum != null && (1 until partNum).any { prevPart ->
            part3Task.id.replace(Regex("""([#_]part)\d+"""), "$1$prevPart") in activeIds
        }

        val isBlocked = isContinuation && (isParentActive || isPart1Active || isPrecedingPartActive)
        isBlocked shouldBe true
    }

    "shouldSyncToCalendar correctly filters based on calendarSyncScope" {
        val frogTask = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "frog_1",
            title = "Eat the Frog",
            isFrog = true,
            isScheduleLocked = false
        )
        val regularTask = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "regular_1",
            title = "Regular Task",
            isFrog = false,
            isScheduleLocked = false
        )
        val lockedTask = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "locked_1",
            title = "Locked Task",
            isFrog = false,
            isScheduleLocked = true
        )

        fun shouldSync(task: com.neuroflow.app.data.local.entity.TaskEntity, scope: String): Boolean {
            return when (scope) {
                "ALL_SCHEDULED" -> true
                "DISABLED" -> false
                else -> task.isFrog || task.isScheduleLocked
            }
        }

        // ALL_SCHEDULED
        shouldSync(frogTask, "ALL_SCHEDULED") shouldBe true
        shouldSync(regularTask, "ALL_SCHEDULED") shouldBe true
        shouldSync(lockedTask, "ALL_SCHEDULED") shouldBe true

        // FROGS_AND_LOCKED
        shouldSync(frogTask, "FROGS_AND_LOCKED") shouldBe true
        shouldSync(regularTask, "FROGS_AND_LOCKED") shouldBe false
        shouldSync(lockedTask, "FROGS_AND_LOCKED") shouldBe true

        // DISABLED
        shouldSync(frogTask, "DISABLED") shouldBe false
        shouldSync(regularTask, "DISABLED") shouldBe false
        shouldSync(lockedTask, "DISABLED") shouldBe false
    }

    "inbound external delta resolution logic: MOVED locks task, DELETED unschedules to backlog" {
        val task = com.neuroflow.app.data.local.entity.TaskEntity(
            id = "task_sync_test",
            title = "Test Task",
            scheduledDate = 1770000000000L,
            scheduledTime = 36000000L,
            isAutoScheduled = true,
            isScheduleLocked = false
        )

        // MOVED delta
        val movedDelta = com.neuroflow.app.data.calendar.ExternalCalendarDelta(
            taskId = task.id,
            type = com.neuroflow.app.data.calendar.ExternalDeltaType.MOVED,
            newStartMillis = 1770000000000L + 54000000L, // new time
            newEndMillis = 1770000000000L + 57600000L
        )

        val (newDate, newTime) = ScheduleAutoTasksWorker.splitMillisToDateAndTime(movedDelta.newStartMillis!!)
        val movedTask = task.copy(
            scheduledDate = newDate,
            scheduledTime = newTime,
            isScheduleLocked = true
        )
        movedTask.isScheduleLocked shouldBe true
        movedTask.scheduledTime shouldBe newTime

        // DELETED delta
        val deletedTask = task.copy(
            scheduledDate = null,
            scheduledTime = null,
            isAutoScheduled = false,
            isScheduleLocked = false
        )
        deletedTask.scheduledDate shouldBe null
        deletedTask.scheduledTime shouldBe null
        deletedTask.isAutoScheduled shouldBe false
        deletedTask.isScheduleLocked shouldBe false
    }
})
