package com.neuroflow.app.data.repository

import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.dao.GoalDao
import com.neuroflow.app.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val preferencesDataStore: UserPreferencesDataStore? = null
) {
    constructor(goalDao: GoalDao) : this(goalDao, null)

    fun observeActiveGoals(): Flow<List<GoalEntity>> = goalDao.observeActiveGoals()
    fun observeAll(): Flow<List<GoalEntity>> = goalDao.observeAll()

    suspend fun getById(id: String): GoalEntity? = goalDao.getById(id)
    suspend fun getActiveGoals(): List<GoalEntity> = goalDao.getActiveGoals()
    suspend fun insert(goal: GoalEntity) = goalDao.insert(goal)
    suspend fun update(goal: GoalEntity) = goalDao.update(goal)
    suspend fun delete(goal: GoalEntity) = goalDao.delete(goal)
    suspend fun deleteAll() = goalDao.deleteAll()

    /**
     * ADR-005 Lazy migration: If GoalDao.getActiveGoals() is empty but legacy DataStore
     * yearlyGoals / weeklyGoals exist, insert them as GoalEntities into Room, mark the period
     * as handled so upgrading users aren't nagged with empty refill cards, and clear the legacy
     * DataStore strings. If active goals already exist in Room, ensure handled markers are initialized.
     */
    suspend fun checkAndMigrateLegacyGoals() {
        val active = goalDao.getActiveGoals()
        if (preferencesDataStore != null) {
            val prefs = preferencesDataStore.preferencesFlow.first()
            val now = System.currentTimeMillis()
            val currentYear = com.neuroflow.app.domain.engine.FreshStartEngine.isoYear(now)
            val currentWeek = com.neuroflow.app.domain.engine.FreshStartEngine.isoWeekNumber(now)
            val currentYearWeekKey = currentYear * 100 + currentWeek

            if (active.isEmpty()) {
                val legacyYearly = prefs.yearlyGoals.filter { it.isNotBlank() }
                val legacyWeekly = prefs.weeklyGoals.filter { it.isNotBlank() }
                if (legacyYearly.isNotEmpty() || legacyWeekly.isNotEmpty()) {
                    legacyYearly.forEach { title ->
                        goalDao.insert(
                            GoalEntity(
                                title = title,
                                description = "Yearly Objective",
                                createdAt = now
                            )
                        )
                    }
                    legacyWeekly.forEach { title ->
                        goalDao.insert(
                            GoalEntity(
                                title = title,
                                description = "Weekly Objective",
                                createdAt = now
                            )
                        )
                    }
                    val yearlyYear = if (legacyYearly.isNotEmpty()) currentYear else 0
                    val weeklyWeek = if (legacyWeekly.isNotEmpty()) currentYearWeekKey else 0
                    preferencesDataStore.clearGoalStringsAndMarkHandled(yearlyYear, weeklyWeek)
                }
            } else {
                // Active goals already exist in Room: ensure handled markers are initialized and legacy strings cleared
                if (prefs.lastYearlyGoalHandledYear == 0 || prefs.lastWeeklyGoalHandledWeek == 0 ||
                    prefs.yearlyGoals.isNotEmpty() || prefs.weeklyGoals.isNotEmpty()) {
                    preferencesDataStore.clearGoalStringsAndMarkHandled(currentYear, currentYearWeekKey)
                }
            }
        }
    }
}
