package com.neuroflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "mindmap_edges",
    indices = [
        Index(value = ["sourceId"], name = "index_mindmap_edges_sourceId"),
        Index(value = ["targetId"], name = "index_mindmap_edges_targetId")
    ]
)
data class MindmapEdgeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sourceId: String,
    val targetId: String,
    val edgeType: String = "VISUAL", // "VISUAL", "DEPENDENCY", "GOAL_LINK"
    val createdAt: Long = System.currentTimeMillis()
)
