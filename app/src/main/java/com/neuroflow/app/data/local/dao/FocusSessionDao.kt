package com.neuroflow.app.data.local.dao

import androidx.room.*
import com.neuroflow.app.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: FocusSessionEntity)

    @Update
    suspend fun update(session: FocusSessionEntity)

    @Delete
    suspend fun delete(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions WHERE id = :id")
    suspend fun getById(id: String): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions WHERE taskId = :taskId AND endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getOpenSessionForTask(taskId: String): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions WHERE taskId = :taskId ORDER BY startedAt DESC")
    fun observeByTaskId(taskId: String): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE startedAt >= :startMillis AND startedAt < :endMillis ORDER BY startedAt DESC")
    fun observeSessionsInRange(startMillis: Long, endMillis: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    suspend fun getAll(): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions WHERE endedAt IS NULL")
    suspend fun getOpenSessions(): List<FocusSessionEntity>

    @Query("DELETE FROM focus_sessions WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: String)

    @Query("DELETE FROM focus_sessions WHERE taskId = :taskId AND endedAt IS NULL")
    suspend fun deleteOpenSessionsByTaskId(taskId: String)

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAll()
}
