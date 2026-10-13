package com.neuroflow.app.worker

import com.neuroflow.app.data.local.UserPreferences
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar

class NotificationWorkerPlanTest : StringSpec({

    val minutes = 60 * 1000L
    val hours = 60 * minutes

    "delayUntilHour targets the coming hour on the same day" {
        val now = at(6, 30)
        val delay = delayUntilHour(7, now)

        (delay in 1..(30 * minutes)) shouldBe true
        hourAt(now + delay) shouldBe 7
    }

    "delayUntilHour rolls to next day when hour already passed" {
        val now = at(22, 0)
        val delay = delayUntilHour(21, now)

        // the next 21:00 is tomorrow: a day short of an hour, allow a DST hour either way
        (delay > 22 * hours && delay <= 24 * hours) shouldBe true
        hourAt(now + delay) shouldBe 21
    }

    "buildNotificationWorkerPlan reflects toggles and hours" {
        val prefs = UserPreferences(
            dailyPlanNotificationsEnabled = true,
            streakNotificationsEnabled = false,
            deadlineEscalationNotificationsEnabled = true,
            dailyPlanNotificationHour = 8,
            streakCheckNotificationHour = 20
        )

        val plan = buildNotificationWorkerPlan(prefs, at(7, 15))

        plan.dailyPlanEnabled shouldBe true
        plan.streakEnabled shouldBe false
        plan.escalationEnabled shouldBe true
        (plan.dailyPlanDelayMs > 0) shouldBe true
        (plan.streakDelayMs > 0) shouldBe true
    }

    "delayUntilHour clamps invalid hour inputs" {
        val now = at(10, 0)

        delayUntilHour(-5, now) shouldBe delayUntilHour(0, now)
        delayUntilHour(40, now) shouldBe delayUntilHour(23, now)
    }

    "delayUntilHour only ever reads the clock it is given" {
        val evening = at(23, 0)

        // a fixed 09:00 target must never resolve against the device's current date
        delayUntilHour(9, evening) shouldBe 10 * hours
    }
})

private fun at(hour: Int, minute: Int): Long = Calendar.getInstance().apply {
    set(2026, Calendar.MARCH, 26, hour, minute, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun hourAt(millis: Long): Int = Calendar.getInstance().apply {
    timeInMillis = millis
}.get(Calendar.HOUR_OF_DAY)
