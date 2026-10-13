package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsCategory
import com.neuroflow.app.domain.model.PtsRecurrence
import com.neuroflow.app.presentation.schedule.formatCustomDays
import com.neuroflow.app.presentation.schedule.getPtsCategoryColor
import com.neuroflow.app.presentation.schedule.getPtsCategoryEmoji
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Calendar

class ProtectedTimeSlotTest : StringSpec({

    fun buildEngine(prefs: UserPreferences = UserPreferences()): AutoSchedulingEngine {
        val dataStore = mockk<UserPreferencesDataStore>()
        every { dataStore.preferencesFlow } returns MutableStateFlow(prefs)
        return AutoSchedulingEngine(dataStore)
    }

    "formatCustomDays should format single and multiple days correctly" {
        // Monday bit: 1 shl (Calendar.MONDAY - 1) = 1 shl 1 = 2
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

    "AutoSchedulingEngine should only resolve SPECIFIC_DATE protected intervals on that designated day" {
        val engine = buildEngine()

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

        // Query 7 days starting from Oct 5, 2026
        val intervals = engine.resolveProtectedIntervals(
            protectedSlots = listOf(slot),
            nowMillis = targetDayMillis,
            horizonDays = 7
        )

        // Should only resolve 1 interval for the entire 7-day horizon
        intervals shouldHaveSize 1
        intervals.first().startMillis shouldBe (targetDayMillis + 540 * 60_000L)
        intervals.first().endMillis shouldBe (targetDayMillis + (540 + 120) * 60_000L)
    }

    "AutoSchedulingEngine should resolve CUSTOM recurrence only on selected weekdays" {
        val engine = buildEngine()

        // Oct 5, 2026 is a Monday (Calendar.MONDAY)
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 5, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val mondayMillis = cal.timeInMillis

        // Recur only on Monday and Tuesday
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

        // 7 days starting from Monday Oct 5 to Sunday Oct 11
        val intervals = engine.resolveProtectedIntervals(
            protectedSlots = listOf(slot),
            nowMillis = mondayMillis,
            horizonDays = 7
        )

        // Mon Oct 5 and Tue Oct 6 should have intervals (2 intervals total in the 7 days)
        intervals shouldHaveSize 2
    }
})
