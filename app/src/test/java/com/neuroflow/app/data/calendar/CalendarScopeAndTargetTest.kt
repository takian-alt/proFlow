package com.neuroflow.app.data.calendar

import android.content.Context
import com.neuroflow.app.data.local.UserPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CalendarScopeAndTargetTest {
    private lateinit var context: Context
    private lateinit var dataSource: CalendarSyncDataSource
    private lateinit var prefsDataStore: UserPreferencesDataStore

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        dataSource = CalendarSyncDataSource(context)
        prefsDataStore = UserPreferencesDataStore(context)
    }

    @Test
    fun testGetAvailableCalendars_returnsEmptyWhenNoPermissions() = runTest {
        val calendars = dataSource.getAvailableCalendars()
        assertNotNull(calendars)
        assertTrue(calendars.isEmpty())
    }

    @Test
    fun testCalendarSyncPreferences_defaultAndUpdates() = runTest {
        val initialPrefs = prefsDataStore.preferencesFlow.first()
        assertEquals("FROGS_AND_LOCKED", initialPrefs.calendarSyncScope)
        assertNull(initialPrefs.calendarSyncTargetId)

        prefsDataStore.setCalendarSyncScope("ALL_SCHEDULED")
        prefsDataStore.setCalendarSyncTargetId(42L)

        val updatedPrefs = prefsDataStore.preferencesFlow.first()
        assertEquals("ALL_SCHEDULED", updatedPrefs.calendarSyncScope)
        assertEquals(42L, updatedPrefs.calendarSyncTargetId)

        // Clear target
        prefsDataStore.setCalendarSyncTargetId(null)
        val clearedPrefs = prefsDataStore.preferencesFlow.first()
        assertNull(clearedPrefs.calendarSyncTargetId)
    }
}
