package com.neuroflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "protected_slots")
data class ProtectedSlotEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val startHour: Int,
    val startMinute: Int,
    val durationMinutes: Int,
    val daysOfWeekMask: Int = 127,
    val isEnabled: Boolean = true
)
