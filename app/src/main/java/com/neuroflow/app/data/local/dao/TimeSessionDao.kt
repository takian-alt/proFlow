package com.neuroflow.app.data.local.dao

import androidx.room.*
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: TimeSessionEntity)

    @Update
    suspend fun update(session: TimeSessionEntity)

    @Delete
    suspend fun delete(session: TimeSessionEntity)

    @Query("SELECT * FROM time_sessions WHERE taskId = :taskId ORDER BY startedAt DESC")
    fun observeByTaskId(taskId: String): Flow<List<TimeSessionEntity>>

    @Query("SELECT * FROM time_sessions WHERE taskId = :taskId ORDER BY startedAt DESC")
    suspend fun getByTaskId(taskId: String): List<TimeSessionEntity>

    @Query("SELECT * FROM time_sessions WHERE taskId = :taskId AND endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getOpenSessionForTask(taskId: String): TimeSessionEntity?

    @Query("SELECT * FROM time_sessions WHERE endedAt IS NULL ORDER BY startedAt DESC")
    fun observeOpenSessions(): Flow<List<TimeSessionEntity>>

    @Query("SELECT * FROM time_sessions WHERE endedAt IS NULL ORDER BY startedAt DESC")
    suspend fun getOpenSessions(): List<TimeSessionEntity>

    @Query("SELECT * FROM time_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<TimeSessionEntity>>

    @Query("SELECT * FROM time_sessions ORDER BY startedAt DESC")
    suspend fun getAll(): List<TimeSessionEntity>

    @Query("SELECT * FROM time_sessions WHERE startedAt >= :startOfDay AND startedAt < :endOfDay ORDER BY startedAt DESC")
    fun observeSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<TimeSessionEntity>>

    @Query("SELECT * FROM time_sessions WHERE startedAt >= :fromMillis ORDER BY startedAt DESC")
    suspend fun getSessionsSince(fromMillis: Long): List<TimeSessionEntity>

    @Query("""
        SELECT SUM(
            CASE 
                WHEN durationMinutes > 0 AND startedAt >= :startOfDay AND (endedAt IS NULL OR endedAt <= :endOfDay) 
                THEN durationMinutes
                ELSE (MIN(COALESCE(endedAt, :endOfDay), :endOfDay) - MAX(startedAt, :startOfDay)) / 60000.0
            END
        )
        FROM time_sessions
        WHERE startedAt < :endOfDay AND (endedAt IS NULL OR endedAt > :startOfDay)
    """)
    fun observeTotalMinutesForDay(startOfDay: Long, endOfDay: Long): Flow<Float?>

    @Query("DELETE FROM time_sessions WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: String)

    @Query("DELETE FROM time_sessions WHERE taskId = :taskId AND endedAt IS NULL")
    suspend fun deleteOpenSessionsByTaskId(taskId: String)

    @Query("DELETE FROM time_sessions")
    suspend fun deleteAll()
}
