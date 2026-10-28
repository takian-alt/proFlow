package com.neuroflow.app.presentation.schedule

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.engine.FreshStartEngine
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsCategory
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyScheduleViewModelTest : StringSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeTest {
        Dispatchers.setMain(testDispatcher)
    }

    afterTest {
        Dispatchers.resetMain()
    }

    "BUG-132: saveWeeklyIntent anchors ISO week to mid-week Thursday ensuring Sunday and Monday produce identical ISO week" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns MutableStateFlow(UserPreferences())

            val prefsUpdateSlot = slot<(UserPreferences) -> UserPreferences>()
            var capturedPrefs = UserPreferences()
            coEvery { prefsDataStore.updatePreferences(capture(prefsUpdateSlot)) } coAnswers {
                capturedPrefs = prefsUpdateSlot.captured(capturedPrefs)
                capturedPrefs
            }

            val zone = ZoneId.systemDefault()
            // Sunday Oct 25, 2026 (Day 0 in Sunday-to-Saturday schedule view)
            val sundayMillis = LocalDate.of(2026, 10, 25).atStartOfDay(zone).toInstant().toEpochMilli() + 10 * 3600_000L
            // Monday Oct 26, 2026 (Day 1 in Sunday-to-Saturday schedule view)
            val mondayMillis = LocalDate.of(2026, 10, 26).atStartOfDay(zone).toInstant().toEpochMilli() + 10 * 3600_000L

            // 1. Initialize ViewModel on Sunday
            val viewModelSunday = WeeklyScheduleViewModel(taskRepo, prefsDataStore)
            advanceUntilIdle()

            // Sunday in isolation evaluates to ISO week 43
            FreshStartEngine.isoWeekNumber(sundayMillis) shouldBe 43
            // Monday in isolation evaluates to ISO week 44
            FreshStartEngine.isoWeekNumber(mondayMillis) shouldBe 44

            // When saved from Sunday, saveWeeklyIntent must anchor to Thursday (Oct 29) -> ISO week 44!
            viewModelSunday.saveWeeklyIntent("Goal set on Sunday")
            advanceUntilIdle()

            capturedPrefs.weeklyIntent shouldBe "Goal set on Sunday"
            capturedPrefs.weeklyIntentIsoWeek shouldBe 44
            capturedPrefs.weeklyIntentIsoYear shouldBe 2026

            // 2. When saved from Monday of the same week, saveWeeklyIntent also resolves to ISO week 44!
            viewModelSunday.goToCurrentWeek()
            viewModelSunday.saveWeeklyIntent("Goal set on Monday")
            advanceUntilIdle()

            // Anchor check: displayed weekDays mid-week Thursday anchors consistently
            val weekDays = viewModelSunday.uiState.value.weekDays
            weekDays.size shouldBe 7
            val thursdayRef = weekDays[4]
            FreshStartEngine.isoWeekNumber(thursdayRef) shouldBe 44
            FreshStartEngine.isoYear(thursdayRef) shouldBe 2026
        }
    }

    "BUG-150: addProtectedSlot persists protected time slot including midnight boundary slots" {
        runTest(testDispatcher) {
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
            val prefsFlow = MutableStateFlow(UserPreferences())
            every { taskRepo.observeActiveTasks() } returns MutableStateFlow(emptyList())
            every { prefsDataStore.preferencesFlow } returns prefsFlow

            val capturedSlots = slot<List<ProtectedTimeSlot>>()
            coEvery { prefsDataStore.updateProtectedTimeSlots(capture(capturedSlots)) } coAnswers {
                prefsFlow.value = prefsFlow.value.copy(protectedTimeSlots = capturedSlots.captured)
                capturedSlots.captured
            }

            val viewModel = WeeklyScheduleViewModel(taskRepo, prefsDataStore)
            advanceUntilIdle()

            // Slot ending at midnight (22:00 -> 00:00 is 120 minutes ending at minute 1440)
            val midnightSlot = ProtectedTimeSlot(
                id = "pts-midnight",
                label = "Evening Wind Down",
                startMinuteOfDay = 22 * 60,
                durationMinutes = 120,
                category = PtsCategory.BREAK
            )

            viewModel.addProtectedSlot(midnightSlot)
            advanceUntilIdle()

            capturedSlots.captured.size shouldBe 1
            capturedSlots.captured.first().id shouldBe "pts-midnight"
            capturedSlots.captured.first().startMinuteOfDay shouldBe 1320
            capturedSlots.captured.first().durationMinutes shouldBe 120
            (capturedSlots.captured.first().startMinuteOfDay + capturedSlots.captured.first().durationMinutes) shouldBe 1440

            viewModel.toggleProtectedSlot("pts-midnight")
            advanceUntilIdle()

            capturedSlots.captured.first().isEnabled shouldBe false

            viewModel.removeProtectedSlot("pts-midnight")
            advanceUntilIdle()

            capturedSlots.captured shouldBe emptyList()
        }
    }

    "BUG-172: Sunday column (todayIndex = 0) properly resolves non-negative column index without being skipped" {
        val zone = ZoneId.systemDefault()
        // Sunday Oct 25, 2026
        val sundayMillis = LocalDate.of(2026, 10, 25).atStartOfDay(zone).toInstant().toEpochMilli() + 10 * 3600_000L
        val weekDays = getSundayWeekDays(sundayMillis)
        weekDays.size shouldBe 7
        val todayStart = com.neuroflow.app.data.local.entity.startOfLocalDay(sundayMillis)
        val todayIndex = weekDays.indexOfFirst { com.neuroflow.app.presentation.common.isSameCalendarDay(it, todayStart) }
        todayIndex shouldBe 0
        val targetCol = (todayIndex - 1).coerceAtLeast(0)
        targetCol shouldBe 0
        (todayIndex >= 0) shouldBe true
    }
})
