package com.neuroflow.app.feature.hyperfocus.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.neuroflow.app.feature.hyperfocus.gate.GateTaskType
import java.util.UUID

enum class GateGrantSource { REWARD_CODE, EMERGENCY, ABANDONED }

// Not backed up to Firestore: the log is a device-local record of what the user reached for.
@Entity(tableName = "gate_requests")
data class GateRequestEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String?,
    val packageName: String,
    val appLabel: String,
    val statedReason: String,
    // Stored as text rather than GateTaskType/GateGrantSource: a read of an enum value that was
    // later deleted crashes the analytics query, and this table is read-only history.
    val taskType: String,
    val mismatchCount: Int,
    val elapsedMillis: Long,
    val grantSource: String,
    val createdAtMillis: Long
) {
    companion object {
        fun new(
            packageName: String,
            appLabel: String,
            statedReason: String,
            taskType: GateTaskType,
            mismatchCount: Int,
            elapsedMillis: Long,
            grantSource: GateGrantSource,
            sessionId: String? = null,
            createdAtMillis: Long = System.currentTimeMillis()
        ) = GateRequestEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            packageName = packageName,
            appLabel = appLabel,
            statedReason = statedReason,
            taskType = taskType.name,
            mismatchCount = mismatchCount,
            elapsedMillis = elapsedMillis,
            grantSource = grantSource.name,
            createdAtMillis = createdAtMillis
        )
    }
}
