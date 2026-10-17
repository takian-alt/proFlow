package com.neuroflow.app.data.local.dao

import androidx.room.*
import com.neuroflow.app.data.local.entity.ProtectedSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProtectedSlotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(slot: ProtectedSlotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(slots: List<ProtectedSlotEntity>)

    @Update
    suspend fun update(slot: ProtectedSlotEntity)

    @Delete
    suspend fun delete(slot: ProtectedSlotEntity)

    @Query("SELECT * FROM protected_slots WHERE isEnabled = 1")
    fun observeActive(): Flow<List<ProtectedSlotEntity>>

    @Query("SELECT * FROM protected_slots")
    fun observeAll(): Flow<List<ProtectedSlotEntity>>

    @Query("SELECT * FROM protected_slots WHERE isEnabled = 1")
    suspend fun getActive(): List<ProtectedSlotEntity>
}
