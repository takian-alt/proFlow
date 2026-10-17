package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsCategory
import com.neuroflow.app.domain.model.PtsRecurrence
import com.neuroflow.app.presentation.schedule.formatCustomDays
import com.neuroflow.app.presentation.schedule.getPtsCategoryColor
import com.neuroflow.app.presentation.schedule.getPtsCategoryEmoji
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar

class ProtectedTimeSlotTest : StringSpec({

    "formatCustomDays should format single and multiple days correctly" {
        val monBit = 1 shl (Calendar.MONDAY - 1)
        val tueBit = 1 shl (Calendar.TUESDAY - 1)
        val wedBit = 1 shl (Calendar.WEDNESDAY - 1)
        val friBit = 1 shl (Calendar.FRIDAY - 1)

        formatCustomDays(monBit) shouldBe "Every Monday"
        formatCustomDays(tueBit) shouldBe "Every Tuesday"
        formatCustomDays(monBit or tueBit) shouldBe "Every Mon, Tue"
        formatCustomDays(monBit or wedBit or friBit) shouldBe "Every Mon, Wed, Fri"
        formatCustomDays(0) shouldBe "No days selected"
    }

    "PtsCategory WORK and STUDY have emojis and colors defined" {
        getPtsCategoryEmoji(PtsCategory.WORK) shouldBe "💼"
        getPtsCategoryEmoji(PtsCategory.STUDY) shouldBe "📚"
        getPtsCategoryColor(PtsCategory.WORK) shouldBe androidx.compose.ui.graphics.Color(0xFF5C6BC0)
        getPtsCategoryColor(PtsCategory.STUDY) shouldBe androidx.compose.ui.graphics.Color(0xFF26A69A)
    }

    "ProtectedSlotClock should only resolve SPECIFIC_DATE protected intervals on that designated day" {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 5, 0, 0, 0) // Oct 5, 2026
            set(Calendar.MILLISECOND, 0)
        }
        val targetDayMillis = cal.timeInMillis

        val slot = ProtectedTimeSlot(
            id = "pts_once",
            label = "Deep Work Session",
            startMinuteOfDay = 540, // 09:00
            durationMinutes = 120, // 2 hours
            recurrence = PtsRecurrence.SPECIFIC_DATE,
            dateMillis = targetDayMillis,
            category = PtsCategory.WORK
        )

        ProtectedSlotClock.occursOnDay(slot, targetDayMillis) shouldBe true
        ProtectedSlotClock.occursOnDay(slot, targetDayMillis + 24 * 3600_000L) shouldBe false
        ProtectedSlotClock.occursOnDay(slot, targetDayMillis + 48 * 3600_000L) shouldBe false
    }

    "ProtectedSlotClock should resolve CUSTOM recurrence only on selected weekdays" {
        // Oct 5, 2026 is a Monday (Calendar.MONDAY)
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 5, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val mondayMillis = cal.timeInMillis

        val monBit = 1 shl (Calendar.MONDAY - 1)
        val tueBit = 1 shl (Calendar.TUESDAY - 1)

        val slot = ProtectedTimeSlot(
            id = "pts_mon_tue",
            label = "Study Block",
            startMinuteOfDay = 600, // 10:00
            durationMinutes = 60,
            recurrence = PtsRecurrence.CUSTOM,
            weekdayBitmask = monBit or tueBit,
            category = PtsCategory.STUDY
        )

        ProtectedSlotClock.occursOnDay(slot, mondayMillis) shouldBe true
        ProtectedSlotClock.occursOnDay(slot, mondayMillis + 24 * 3600_000L) shouldBe true // Tuesday
        ProtectedSlotClock.occursOnDay(slot, mondayMillis + 48 * 3600_000L) shouldBe false // Wednesday
        ProtectedSlotClock.occursOnDay(slot, mondayMillis + 72 * 3600_000L) shouldBe false // Thursday
    }
})
