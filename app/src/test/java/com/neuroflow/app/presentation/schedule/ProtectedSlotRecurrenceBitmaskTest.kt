package com.neuroflow.app.presentation.schedule

import androidx.room.Room
import com.neuroflow.app.data.local.NeuroFlowDatabase
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsCategory
import com.neuroflow.app.domain.model.PtsRecurrence
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ProtectedSlotRecurrenceBitmaskTest {

    @Test
    fun testWeekdaysAndWeekendsRecurrenceMapsToCorrectRoomBitmasks() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(context, NeuroFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val protectedSlotDao = db.protectedSlotDao()
        val preferencesDataStore = UserPreferencesDataStore(context, protectedSlotDao)

        try {
            val weekdaySlot = ProtectedTimeSlot(
                id = "slot-weekdays",
                label = "Work Block",
                startMinuteOfDay = 9 * 60,
                durationMinutes = 120,
                category = PtsCategory.WORK,
                recurrence = PtsRecurrence.WEEKDAYS,
                weekdayBitmask = 0b0111110
            )

            val weekendSlot = ProtectedTimeSlot(
                id = "slot-weekends",
                label = "Family Time",
                startMinuteOfDay = 10 * 60,
                durationMinutes = 180,
                category = PtsCategory.FAMILY,
                recurrence = PtsRecurrence.WEEKENDS,
                weekdayBitmask = 0b1000001
            )

            val dailySlot = ProtectedTimeSlot(
                id = "slot-daily",
                label = "Morning Meditation",
                startMinuteOfDay = 7 * 60,
                durationMinutes = 30,
                category = PtsCategory.MINDFULNESS,
                recurrence = PtsRecurrence.DAILY,
                weekdayBitmask = 0b1111111
            )

            preferencesDataStore.updateProtectedTimeSlots(listOf(weekdaySlot, weekendSlot, dailySlot))

            val inDb = protectedSlotDao.getAll().associateBy { it.id }
            assertEquals(3, inDb.size)

            // Mon-Fri: 0b0111110 = 62
            assertEquals(62, inDb["slot-weekdays"]?.daysOfWeekMask)
            // Sat, Sun: 0b1000001 = 65
            assertEquals(65, inDb["slot-weekends"]?.daysOfWeekMask)
            // Daily: 0b1111111 = 127
            assertEquals(127, inDb["slot-daily"]?.daysOfWeekMask)
        } finally {
            db.close()
        }
    }
}
