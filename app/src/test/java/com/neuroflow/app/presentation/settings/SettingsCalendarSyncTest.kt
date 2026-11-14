package com.neuroflow.app.presentation.settings

import android.content.Context
import com.neuroflow.app.data.calendar.AvailableCalendar
import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.DatabaseCleaner
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.domain.repository.EnergyMetricsRepository
import com.neuroflow.app.domain.repository.SleepPressureRepository
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsCalendarSyncTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private val prefsDataStore = mockk<UserPreferencesDataStore>(relaxed = true)
    private val sleepPressureRepo = mockk<SleepPressureRepository>(relaxed = true)
    private val energyMetricsRepo = mockk<EnergyMetricsRepository>(relaxed = true)
    private val dbCleaner = mockk<DatabaseCleaner>(relaxed = true)
    private val circadianService: CircadianEnergyService = DefaultCircadianEnergyService()
    private val calendarRepo = mockk<CalendarSyncRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = org.robolectric.RuntimeEnvironment.getApplication()
        every { prefsDataStore.preferencesFlow } returns flowOf(UserPreferences())
        every { prefsDataStore.calendarSyncScope } returns flowOf("FROGS_AND_LOCKED")
        every { prefsDataStore.calendarSyncTargetId } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSettingsViewModel_exposesCalendarSyncScopeAndAvailableCalendars() = runTest(testDispatcher) {
        val calendars = listOf(
            AvailableCalendar(id = 1L, displayName = "Personal", accountName = "user@gmail.com", isPrimary = true),
            AvailableCalendar(id = 2L, displayName = "Work", accountName = "user@work.com", isPrimary = false)
        )
        coEvery { calendarRepo.getAvailableCalendars() } returns calendars

        val viewModel = SettingsViewModel(
            context = context,
            preferencesDataStore = prefsDataStore,
            sleepPressureRepository = sleepPressureRepo,
            energyMetricsRepository = energyMetricsRepo,
            databaseCleaner = dbCleaner,
            circadianEnergyService = circadianService,
            sleepTelemetryRepository = null,
            calendarSyncRepository = calendarRepo
        )

        advanceUntilIdle()

        assertEquals("FROGS_AND_LOCKED", viewModel.calendarSyncScope.value)
        assertEquals(null, viewModel.calendarSyncTargetId.value)

        viewModel.loadAvailableCalendars()
        advanceUntilIdle()

        assertEquals(2, viewModel.availableCalendars.value.size)
        assertEquals("Personal", viewModel.availableCalendars.value[0].displayName)

        viewModel.setCalendarSyncScope("ALL_SCHEDULED")
        advanceUntilIdle()
        coVerify { prefsDataStore.setCalendarSyncScope("ALL_SCHEDULED") }

        viewModel.setTargetCalendar(2L)
        advanceUntilIdle()
        coVerify { prefsDataStore.setCalendarSyncTargetId(2L) }
    }
}
