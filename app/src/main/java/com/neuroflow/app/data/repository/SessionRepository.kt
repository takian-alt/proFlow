package com.neuroflow.app.data.repository

import com.neuroflow.app.data.local.dao.TimeSessionDao
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.dao.FocusSessionDao
import com.neuroflow.app.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val timeSessionDao: TimeSessionDao,
    private val focusSessionDao: FocusSessionDao? = null
) {
    constructor(timeSessionDao: TimeSessionDao) : this(timeSessionDao, null)
    data class InterruptionSummary(
        val totalPauseResumeCount: Int,
        val totalAppSwitchCount: Int,
        val totalInterruptionBursts: Int
    )
    fun observeByTaskId(taskId: String): Flow<List<TimeSessionEntity>> =
        timeSessionDao.observeByTaskId(taskId)

    fun observeAll(): Flow<List<TimeSessionEntity>> = timeSessionDao.observeAll()

    fun observeSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<TimeSessionEntity>> =
        timeSessionDao.observeSessionsForDay(startOfDay, endOfDay)

    fun observeTotalMinutesForDay(startOfDay: Long, endOfDay: Long): Flow<Float?> =
        timeSessionDao.observeTotalMinutesForDay(startOfDay, endOfDay)

    suspend fun getByTaskId(taskId: String): List<TimeSessionEntity> =
        timeSessionDao.getByTaskId(taskId)

    suspend fun getAllSessions(): List<TimeSessionEntity> =
        timeSessionDao.getAll()

    suspend fun getInterruptionSummary(fromMillis: Long): InterruptionSummary {
        val sessions = timeSessionDao.getAll()
            .filter { it.startedAt >= fromMillis }
        return InterruptionSummary(
            totalPauseResumeCount = sessions.sumOf { it.pauseResumeCount },
            totalAppSwitchCount = sessions.sumOf { it.appSwitchCount },
            totalInterruptionBursts = sessions.sumOf { it.interruptionBurstCount }
        )
    }

    suspend fun getOpenSessionForTask(taskId: String): TimeSessionEntity? =
        timeSessionDao.getOpenSessionForTask(taskId)

    fun observeOpenSessions(): Flow<List<TimeSessionEntity>> =
        timeSessionDao.observeOpenSessions()

    suspend fun getOpenSessions(): List<TimeSessionEntity> =
        timeSessionDao.getOpenSessions()

    fun observeFocusSessionsByTaskId(taskId: String): Flow<List<FocusSessionEntity>> =
        focusSessionDao?.observeByTaskId(taskId) ?: flowOf(emptyList())

    fun observeFocusSessionsInRange(startMillis: Long, endMillis: Long): Flow<List<FocusSessionEntity>> =
        focusSessionDao?.observeSessionsInRange(startMillis, endMillis) ?: flowOf(emptyList())

    suspend fun recordFocusSession(session: FocusSessionEntity) {
        focusSessionDao?.insert(session)
    }

    suspend fun updateFocusSession(session: FocusSessionEntity) {
        focusSessionDao?.update(session)
    }

    suspend fun insert(session: TimeSessionEntity) {
        timeSessionDao.insert(session)
        syncFocusSession(session)
    }

    suspend fun update(session: TimeSessionEntity) {
        timeSessionDao.update(session)
        syncFocusSession(session)
    }

    private suspend fun syncFocusSession(session: TimeSessionEntity) {
        val dao = focusSessionDao ?: return
        val durationSec = (session.durationMinutes * 60).toInt()
        val interruptions = session.pauseResumeCount + session.appSwitchCount + session.interruptionBurstCount
        dao.insert(
            FocusSessionEntity(
                id = session.id,
                taskId = session.taskId,
                startedAt = session.startedAt,
                endedAt = session.endedAt,
                durationSeconds = durationSec,
                interruptionCount = interruptions,
                energyAtStart = 50,
                wasCompleted = session.endedAt != null
            )
        )
    }

    suspend fun delete(session: TimeSessionEntity) {
        timeSessionDao.delete(session)
        val entity = focusSessionDao?.getById(session.id)
        if (entity != null) {
            focusSessionDao.delete(entity)
        }
    }

    suspend fun deleteAll() = timeSessionDao.deleteAll()
}
