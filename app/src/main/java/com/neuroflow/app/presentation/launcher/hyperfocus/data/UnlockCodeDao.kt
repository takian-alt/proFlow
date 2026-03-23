package com.neuroflow.app.presentation.launcher.hyperfocus.data

import androidx.room.*

@Dao
interface UnlockCodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(codes: List<UnlockCodeEntity>)

    @Query("SELECT * FROM unlock_codes WHERE sessionId = :sessionId AND isUsed = 0")
    suspend fun getUnusedBySession(sessionId: String): List<UnlockCodeEntity>

    @Query("UPDATE unlock_codes SET isUsed = 1, usedAt = :usedAt, unlockedUntil = :unlockedUntil WHERE id = :id")
    suspend fun markUsed(id: String, usedAt: Long, unlockedUntil: Long?)

    @Query("DELETE FROM unlock_codes WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}
