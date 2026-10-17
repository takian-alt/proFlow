package com.neuroflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["startedAt"], name = "index_focus_sessions_started_at"),
        Index(value = ["taskId"], name = "index_focus_sessions_task_id")
    ]
)
data class FocusSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val durationSeconds: Int = 0,
    val interruptionCount: Int = 0,
    val energyAtStart: Int = 50,
    val wasCompleted: Boolean = false
)
