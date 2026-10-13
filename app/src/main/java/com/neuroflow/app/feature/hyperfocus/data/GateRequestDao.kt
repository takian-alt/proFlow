package com.neuroflow.app.feature.hyperfocus.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

data class PackageRequestCount(
    val packageName: String,
    val requests: Int,
    val abandoned: Int
)

@Dao
interface GateRequestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: GateRequestEntity)

    @Query("SELECT * FROM `gate_requests` ORDER BY `createdAtMillis` DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<GateRequestEntity>

    @Query(
        "SELECT packageName, COUNT(*) AS requests, " +
            "COALESCE(SUM(CASE WHEN grantSource = 'ABANDONED' THEN 1 ELSE 0 END), 0) AS abandoned " +
            "FROM `gate_requests` WHERE createdAtMillis BETWEEN :from AND :to " +
            "GROUP BY packageName ORDER BY requests DESC"
    )
    suspend fun countsByPackage(from: Long, to: Long): List<PackageRequestCount>

    @Query("DELETE FROM `gate_requests` WHERE createdAtMillis < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long): Int
}
