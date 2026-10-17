package com.neuroflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "quick_notes")
data class QuickNoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isPinned: Boolean = false,
    val convertedTaskId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
