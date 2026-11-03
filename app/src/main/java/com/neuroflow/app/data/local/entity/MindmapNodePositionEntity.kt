package com.neuroflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "mindmap_node_positions",
    indices = [
        Index(value = ["entityId"], unique = true, name = "index_mindmap_node_positions_entityId")
    ]
)
data class MindmapNodePositionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val entityId: String,
    val entityType: String, // "TASK" or "GOAL"
    val x: Float,
    val y: Float,
    val updatedAt: Long = System.currentTimeMillis()
)
