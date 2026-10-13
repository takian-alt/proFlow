package com.neuroflow.app.domain.engine

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TaskScoringEngineTest : StringSpec({

    val defaultPrefs = UserPreferences()
    val now = System.currentTimeMillis()

    fun makeTask(
        quadrant: Quadrant = Quadrant.SCHEDULE,
        priority: Priority = Priority.MEDIUM,
        deadlineDate: Long? = null,
        scheduledDate: Long? = null,
        scheduledTime: Long? = null,
        estimatedDuration: Int = 0,
        impactScore: Int = 50,
        isFrog: Boolean = false,
        postponeCount: Int = 0
    ) = TaskEntity(
        title = "Test Task",
        quadrant = quadrant,
        priority = priority,
        deadlineDate = deadlineDate,
        scheduledDate = scheduledDate,
        scheduledTime = scheduledTime,
        estimatedDurationMinutes = estimatedDuration,
        impactScore = impactScore,
        isFrog = isFrog,
        postponeCount = postponeCount
    )

    "DO_FIRST quadrant scores higher than ELIMINATE" {
        val doFirst = TaskScoringEngine.score(makeTask(quadrant = Quadrant.DO_FIRST), defaultPrefs, nowMillis = now)
        val eliminate = TaskScoringEngine.score(makeTask(quadrant = Quadrant.ELIMINATE), defaultPrefs, nowMillis = now)

        (doFirst > eliminate) shouldBe true
    }

    "HIGH priority scores higher than LOW" {
        val high = TaskScoringEngine.score(makeTask(priority = Priority.HIGH), defaultPrefs, nowMillis = now)
        val low = TaskScoringEngine.score(makeTask(priority = Priority.LOW), defaultPrefs, nowMillis = now)

        (high > low) shouldBe true
    }

    "overdue deadline scores higher than far-future deadline" {
        val overdue = TaskScoringEngine.score(makeTask(deadlineDate = now - 3_600_000), defaultPrefs, nowMillis = now)
        val future = TaskScoringEngine.score(makeTask(deadlineDate = now + 30 * 86_400_000L), defaultPrefs, nowMillis = now)

        (overdue > future) shouldBe true
    }

    "postponed tasks score higher than non-postponed" {
        val postponed = TaskScoringEngine.score(makeTask(postponeCount = 5), defaultPrefs, nowMillis = now)
        val fresh = TaskScoringEngine.score(makeTask(postponeCount = 0), defaultPrefs, nowMillis = now)

        (postponed > fresh) shouldBe true
    }

    "sortedByScore filters out completed tasks" {
        val tasks = listOf(
            makeTask(priority = Priority.HIGH),
            makeTask(priority = Priority.LOW).copy(status = TaskStatus.COMPLETED)
        )

        val sorted = TaskScoringEngine.sortedByScore(tasks, defaultPrefs, nowMillis = now)

        sorted.size shouldBe 1
        sorted[0].priority shouldBe Priority.HIGH
    }

    "autoAssignQuadrant assigns DO_FIRST for urgent important tasks" {
        val task = makeTask(priority = Priority.HIGH, impactScore = 80, deadlineDate = now + 24 * 3_600_000)

        TaskScoringEngine.autoAssignQuadrant(task, now) shouldBe Quadrant.DO_FIRST
    }

    "autoAssignQuadrant assigns SCHEDULE for important non-urgent tasks" {
        val task = makeTask(priority = Priority.LOW, impactScore = 80, deadlineDate = now + 30 * 86_400_000L)

        TaskScoringEngine.autoAssignQuadrant(task, now) shouldBe Quadrant.SCHEDULE
    }

    "autoAssignQuadrant assigns ELIMINATE for non-urgent non-important" {
        val task = makeTask(priority = Priority.LOW, impactScore = 20)

        TaskScoringEngine.autoAssignQuadrant(task, now) shouldBe Quadrant.ELIMINATE
    }

    "blocked task returns low score" {
        val task = makeTask(deadlineDate = now + 3_600_000).copy(dependsOnTaskIds = "other123")
        val dep = makeTask(deadlineDate = now + 72_000_000).copy(id = "other123")

        val score = TaskScoringEngine.score(task, defaultPrefs, listOf(task, dep), now)

        (score < 25f) shouldBe true
    }

    "near-deadline task scores higher than far-future" {
        val urgent = makeTask(deadlineDate = now + 3_600_000)
        val distant = makeTask(deadlineDate = now + 7 * 86_400_000L)

        val urgentScore = TaskScoringEngine.score(urgent, defaultPrefs, listOf(urgent, distant), now)
        val distantScore = TaskScoringEngine.score(distant, defaultPrefs, listOf(urgent, distant), now)

        (urgentScore > distantScore) shouldBe true
    }

    "scheduled soon task boosts score relative to schedule far away" {
        val soon = makeTask(scheduledDate = now + 30 * 60_000L, scheduledTime = 0L)
        val later = makeTask(scheduledDate = now + 24 * 86_400_000L, scheduledTime = 0L)

        val soonScore = TaskScoringEngine.score(soon, defaultPrefs, listOf(soon, later), now)
        val laterScore = TaskScoringEngine.score(later, defaultPrefs, listOf(soon, later), now)

        (soonScore > laterScore) shouldBe true
    }

    "locked task retains non-zero score with proximity ramp" {
        val farLocked = makeTask().copy(isScheduleLocked = true, habitDate = now + 20 * 60_000L)
        val nearLocked = makeTask().copy(isScheduleLocked = true, habitDate = now + 5 * 60_000L)

        val farScore = TaskScoringEngine.score(farLocked, defaultPrefs, listOf(farLocked, nearLocked), now)
        val nearScore = TaskScoringEngine.score(nearLocked, defaultPrefs, listOf(farLocked, nearLocked), now)

        (farScore > 0f) shouldBe true
        (nearScore > farScore) shouldBe true
    }

    "weightFrogBonus amplifies frog score" {
        val frogTask = makeTask(isFrog = true, impactScore = 50)

        val suppressed = TaskScoringEngine.score(frogTask, defaultPrefs.copy(weightFrogBonus = 0.2f), listOf(frogTask), now)
        val plain = TaskScoringEngine.score(frogTask, defaultPrefs, listOf(frogTask), now)
        val amplified = TaskScoringEngine.score(frogTask, defaultPrefs.copy(weightFrogBonus = 1.8f), listOf(frogTask), now)

        (amplified > plain) shouldBe true
        (plain > suppressed) shouldBe true
    }

    "weightPlanBoost amplifies if-then plan score" {
        val plannedTask = makeTask(impactScore = 50).copy(ifThenPlan = "IF 9am THEN open IDE")

        val suppressed = TaskScoringEngine.score(plannedTask, defaultPrefs.copy(weightPlanBoost = 0.2f), listOf(plannedTask), now)
        val plain = TaskScoringEngine.score(plannedTask, defaultPrefs, listOf(plannedTask), now)
        val amplified = TaskScoringEngine.score(plannedTask, defaultPrefs.copy(weightPlanBoost = 1.8f), listOf(plannedTask), now)

        (amplified > plain) shouldBe true
        (plain > suppressed) shouldBe true
    }

    "weightSessionProgress amplifies started tasks" {
        val progressTask = makeTask(impactScore = 50).copy(sessionCount = 3)

        val suppressed = TaskScoringEngine.score(progressTask, defaultPrefs.copy(weightSessionProgress = 0.2f), listOf(progressTask), now)
        val plain = TaskScoringEngine.score(progressTask, defaultPrefs, listOf(progressTask), now)
        val amplified = TaskScoringEngine.score(progressTask, defaultPrefs.copy(weightSessionProgress = 1.8f), listOf(progressTask), now)

        (amplified > plain) shouldBe true
        (plain > suppressed) shouldBe true
    }

    "weightEnjoyment modulates enjoyment score" {
        val enjoyedTask = makeTask(impactScore = 50).copy(enjoymentScore = 90, effortScore = 80)

        val suppressed = TaskScoringEngine.score(enjoyedTask, defaultPrefs.copy(weightEnjoyment = 0.2f), listOf(enjoyedTask), now)
        val plain = TaskScoringEngine.score(enjoyedTask, defaultPrefs, listOf(enjoyedTask), now)
        val amplified = TaskScoringEngine.score(enjoyedTask, defaultPrefs.copy(weightEnjoyment = 1.8f), listOf(enjoyedTask), now)

        (amplified > plain) shouldBe true
        (plain > suppressed) shouldBe true
    }

    "scoring a task list matches scoring through a prebuilt context" {
        val blocker = makeTask().copy(id = "blocker")
        val waiting = makeTask().copy(id = "waiting", dependsOnTaskIds = "blocker")
        val free = makeTask().copy(id = "free")
        val tasks = listOf(blocker, waiting, free)

        val viaList = tasks.map { TaskScoringEngine.score(it, defaultPrefs, tasks, now) }
        val context = TaskScoringEngine.ScoringContext.from(tasks)
        val viaContext = tasks.map { TaskScoringEngine.score(it, defaultPrefs, context, now) }

        viaList shouldBe viaContext
        context.blockedByCount(waiting) shouldBe 1
        context.blockedByCount(free) shouldBe 0
        context.unblockCount(blocker) shouldBe 1
    }
})
