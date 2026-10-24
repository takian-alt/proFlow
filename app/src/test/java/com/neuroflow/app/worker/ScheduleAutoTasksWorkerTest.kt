package com.neuroflow.app.worker

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
})
