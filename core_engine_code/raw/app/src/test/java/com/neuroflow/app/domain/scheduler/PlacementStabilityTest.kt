package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.TaskStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar

/**
 * The scheduler used to rewrite a placement as often as it woke up, so a block could move out from
 * under a user who was already working in it. These cases pin the agreed stability policy: a block
 * starting within two hours is frozen even against a forced re-plan, the cooldown gates every rule
 * rather than only the periodic one, and a placement that is not materially different is not
 * written at all.
 */
class PlacementStabilityTest : StringSpec({

    val now = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 12, 9, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val minutes = 60_000L
    val hours = 60 * minutes

    fun daySplit(millis: Long): Pair<Long, Long> {
        val dayStart = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return dayStart to (millis - dayStart)
    }

    fun placedTask(
        startMillis: Long,
        lastAutoScheduledAt: Long?,
        deadlineMillis: Long? = null,
        postponeCount: Int = 0
    ): TaskEntity {
        val (date, time) = daySplit(startMillis)
        val (deadlineDate, deadlineTime) = deadlineMillis?.let { daySplit(it) } ?: (null to null)
        return TaskEntity(
            id = "task-$startMillis",
            title = "Placed task",
            status = TaskStatus.ACTIVE,
            priority = Priority.MEDIUM,
            estimatedDurationMinutes = 60,
            scheduledDate = date,
            scheduledTime = time,
            isAutoScheduled = true,
            lastAutoScheduledAt = lastAutoScheduledAt,
            updatedAt = now - 24 * hours,
            deadlineDate = deadlineDate,
            deadlineTime = deadlineTime,
            postponeCount = postponeCount
        )
    }

    val stablePrefs = UserPreferences(autoSchedulingEnabled = true, isAutoScheduleToggleOn = true)

    "a block starting within two hours survives a forced re-plan" {
        val task = placedTask(now + hours, lastAutoScheduledAt = now - 3 * hours)

        PlacementStability.verdict(task, now, stablePrefs, forceReplan = true) shouldBe
            PlacementStability.ReplanVerdict.NEAR_TERM_FREEZE
        PlacementStability.verdict(task, now, stablePrefs, forceReplan = true).allowsReplan shouldBe false
    }

    "a deadline rescue respects the cooldown instead of firing every pass" {
        val task = placedTask(
            startMillis = now + 4 * hours,
            lastAutoScheduledAt = now - 5 * minutes,
            deadlineMillis = now + 5 * hours
        )

        PlacementStability.verdict(task, now, stablePrefs, forceReplan = false) shouldBe
            PlacementStability.ReplanVerdict.COOLDOWN
        PlacementStability.verdict(task, now, stablePrefs, forceReplan = false).allowsReplan shouldBe false
    }

    "a postponed task is not re-planned twice a minute" {
        val task = placedTask(
            startMillis = now + 3 * hours,
            lastAutoScheduledAt = now - 10 * minutes,
            postponeCount = 3
        )

        PlacementStability.verdict(task, now, stablePrefs, forceReplan = false) shouldBe
            PlacementStability.ReplanVerdict.COOLDOWN
        PlacementStability.verdict(task, now, stablePrefs, forceReplan = false).allowsReplan shouldBe false
    }

    "a forced re-plan still reaches blocks further out than two hours" {
        val task = placedTask(now + 5 * hours, lastAutoScheduledAt = now - 3 * hours)

        PlacementStability.verdict(task, now, stablePrefs, forceReplan = true).allowsReplan shouldBe true
    }

    "the periodic refresh still paces a stable plan" {
        val pastCooldown = placedTask(now + 5 * hours, lastAutoScheduledAt = now - 5 * hours)
        val insideCooldown = placedTask(now + 5 * hours, lastAutoScheduledAt = now - hours)

        PlacementStability.verdict(pastCooldown, now, stablePrefs, forceReplan = false)
            .allowsReplan shouldBe true
        PlacementStability.verdict(insideCooldown, now, stablePrefs, forceReplan = false)
            .allowsReplan shouldBe false
    }

    "an unchanged placement is not rewritten" {
        val previous = now + 3 * hours

        PlacementStability.isMoveWorthApplying(previous, previous, now) shouldBe false
    }

    "a future block only moves when the shift is at least half an hour" {
        val previous = now + 3 * hours

        PlacementStability.isMoveWorthApplying(previous, previous + 12 * minutes, now) shouldBe false
        PlacementStability.isMoveWorthApplying(previous, previous + 45 * minutes, now) shouldBe true
    }

    "a block already in the past always takes the new slot" {
        PlacementStability.isMoveWorthApplying(now - 10 * minutes, now + 2 * hours, now) shouldBe true
        PlacementStability.isMoveWorthApplying(null, now + 2 * hours, now) shouldBe true
    }
})
