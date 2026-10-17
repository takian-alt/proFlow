package com.neuroflow.app.data.local.dao

import androidx.room.*
import com.neuroflow.app.data.local.entity.TaskMicroStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskMicroStepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: TaskMicroStepEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(steps: List<TaskMicroStepEntity>)

    @Update
    suspend fun update(step: TaskMicroStepEntity)

    @Delete
    suspend fun delete(step: TaskMicroStepEntity)

    @Query("DELETE FROM task_micro_steps WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: String)

    @Query("SELECT * FROM task_micro_steps WHERE taskId = :taskId ORDER BY sortOrder ASC")
    fun observeByTaskId(taskId: String): Flow<List<TaskMicroStepEntity>>

    @Query("SELECT * FROM task_micro_steps WHERE taskId = :taskId ORDER BY sortOrder ASC")
    suspend fun getByTaskId(taskId: String): List<TaskMicroStepEntity>
}
