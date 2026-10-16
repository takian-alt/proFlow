package com.neuroflow.app.data.local

import android.util.Log
import androidx.room.TypeConverter
import com.neuroflow.app.data.local.entity.ContractOutcome
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.domain.model.TaskType

class Converters {
    private companion object {
        private const val TAG = "RoomConverters"
    }

    @TypeConverter fun fromQuadrant(value: Quadrant): String = value.name
    @TypeConverter fun toQuadrant(value: String): Quadrant =
        try {
            Quadrant.valueOf(value)
        } catch (e: Exception) {
            Log.w(TAG, "Unknown Quadrant: '$value', defaulting to SCHEDULE", e)
            Quadrant.SCHEDULE
        }

    @TypeConverter fun fromPriority(value: Priority): String = value.name
    @TypeConverter fun toPriority(value: String): Priority =
        try {
            Priority.valueOf(value)
        } catch (e: Exception) {
            Log.w(TAG, "Unknown Priority: '$value', defaulting to MEDIUM", e)
            Priority.MEDIUM
        }

    @TypeConverter fun fromTaskStatus(value: TaskStatus): String = value.name
    @TypeConverter fun toTaskStatus(value: String): TaskStatus =
        try {
            TaskStatus.valueOf(value)
        } catch (e: Exception) {
            Log.w(TAG, "Unknown TaskStatus: '$value', defaulting to ACTIVE", e)
            TaskStatus.ACTIVE
        }

    @TypeConverter fun fromRecurrence(value: Recurrence): String = value.name
    @TypeConverter fun toRecurrence(value: String): Recurrence =
        try {
            Recurrence.valueOf(value)
        } catch (e: Exception) {
            Log.w(TAG, "Unknown Recurrence: '$value', defaulting to NONE", e)
            Recurrence.NONE
        }

    @TypeConverter fun fromEnergyLevel(value: EnergyLevel): String = value.name
    @TypeConverter fun toEnergyLevel(value: String): EnergyLevel =
        try {
            EnergyLevel.valueOf(value)
        } catch (e: Exception) {
            Log.w(TAG, "Unknown EnergyLevel: '$value', defaulting to MEDIUM", e)
            EnergyLevel.MEDIUM
        }

    @TypeConverter fun fromTaskType(value: TaskType): String = value.name
    @TypeConverter fun toTaskType(value: String): TaskType =
        try {
            TaskType.valueOf(value)
        } catch (e: Exception) {
            Log.w(TAG, "Unknown TaskType: '$value', defaulting to ANALYTICAL", e)
            TaskType.ANALYTICAL
        }

    @TypeConverter fun fromContractOutcome(value: ContractOutcome?): String? = value?.name
    @TypeConverter fun toContractOutcome(value: String?): ContractOutcome? =
        value?.let {
            try {
                ContractOutcome.valueOf(it)
            } catch (e: Exception) {
                Log.w(TAG, "Unknown ContractOutcome: '$it', defaulting to null", e)
                null
            }
        }

    @TypeConverter fun fromRewardTier(value: RewardTier): String = value.name
    @TypeConverter fun toRewardTier(value: String): RewardTier =
        try {
            RewardTier.valueOf(value)
        } catch (e: Exception) {
            Log.w(TAG, "Unknown RewardTier: '$value', defaulting to NONE", e)
            RewardTier.NONE
        }
}
