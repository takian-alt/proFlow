package com.neuroflow.app.feature.hyperfocus.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface HyperFocusSessionDao {

    @Insert
    suspend fun insert(session: HyperFocusSessionEntity)

    @Update
    suspend fun update(session: HyperFocusSessionEntity)

    @Query("SELECT * FROM hyperfocus_sessions WHERE id = :id")
    suspend fun getById(id: String): HyperFocusSessionEntity?

    @Query("SELECT * FROM hyperfocus_sessions")
    suspend fun getAll(): List<HyperFocusSessionEntity>
}
