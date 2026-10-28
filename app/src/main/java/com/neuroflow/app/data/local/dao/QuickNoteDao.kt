package com.neuroflow.app.data.local.dao

import androidx.room.*
import com.neuroflow.app.data.local.entity.QuickNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickNoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: QuickNoteEntity)

    @Update
    suspend fun update(note: QuickNoteEntity)

    @Delete
    suspend fun delete(note: QuickNoteEntity)

    @Query("SELECT * FROM quick_notes WHERE id = :id")
    suspend fun getById(id: String): QuickNoteEntity?

    @Query("SELECT * FROM quick_notes ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAll(): List<QuickNoteEntity>

    @Query("SELECT * FROM quick_notes ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<QuickNoteEntity>>

    @Query("SELECT * FROM quick_notes WHERE isPinned = 1 ORDER BY updatedAt DESC")
    fun observePinned(): Flow<List<QuickNoteEntity>>

    @Query("SELECT * FROM quick_notes WHERE convertedTaskId IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun observeUnconvertedNotes(): Flow<List<QuickNoteEntity>>
}
