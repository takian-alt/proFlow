package com.neuroflow.app.data.local

import androidx.room.Room
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class UserPreferencesProtectedSlotsReconciliationTest {

    @Test
    fun testUpdateProtectedTimeSlotsReconcilesRoomDeletions() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(context, NeuroFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val protectedSlotDao = db.protectedSlotDao()
        val preferencesDataStore = UserPreferencesDataStore(context, protectedSlotDao)

        try {
            val slot1 = ProtectedTimeSlot(
                id = "slot-1",
                label = "Morning Deep Work",
                startMinuteOfDay = 9 * 60,
                durationMinutes = 120,
                category = PtsCategory.WORK
            )
            val slot2 = ProtectedTimeSlot(
                id = "slot-2",
                label = "Lunch Break",
                startMinuteOfDay = 12 * 60,
                durationMinutes = 60,
                category = PtsCategory.MEAL
            )

            // 1. Initial insert of 2 slots
            preferencesDataStore.updateProtectedTimeSlots(listOf(slot1, slot2))

            val initialInDb = protectedSlotDao.getAll()
            assertEquals(2, initialInDb.size)
            assertTrue(initialInDb.any { it.id == "slot-1" })
            assertTrue(initialInDb.any { it.id == "slot-2" })

            // 2. Remove slot-2 (pass only slot-1)
            preferencesDataStore.updateProtectedTimeSlots(listOf(slot1))

            val afterRemovalInDb = protectedSlotDao.getAll()
            assertEquals(1, afterRemovalInDb.size)
            assertEquals("slot-1", afterRemovalInDb.first().id)

            // 3. Remove all slots
            preferencesDataStore.updateProtectedTimeSlots(emptyList())
            val afterAllRemoved = protectedSlotDao.getAll()
            assertTrue(afterAllRemoved.isEmpty())
        } finally {
            db.close()
        }
    }
}
