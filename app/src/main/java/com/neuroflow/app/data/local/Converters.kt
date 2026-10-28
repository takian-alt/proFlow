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
import com.neuroflow.app.domain.model.HyperFocusState

class Converters {
    private companion object {
        private const val TAG = "RoomConverters"
    }

    @TypeConverter fun fromQuadrant(value: Quadrant): String = value.name
    @TypeConverter fun toQuadrant(value: String?): Quadrant =
        value?.let {
            try {
                Quadrant.valueOf(it)
            } catch (e: Exception) {
                Log.w(TAG, "Unknown Quadrant: '$it', defaulting to SCHEDULE", e)
                Quadrant.SCHEDULE
            }
        } ?: Quadrant.SCHEDULE

    @TypeConverter fun fromPriority(value: Priority): String = value.name
    @TypeConverter fun toPriority(value: String?): Priority =
        value?.let {
            try {
                Priority.valueOf(it)
            } catch (e: Exception) {
                Log.w(TAG, "Unknown Priority: '$it', defaulting to MEDIUM", e)
                Priority.MEDIUM
            }
        } ?: Priority.MEDIUM

    @TypeConverter fun fromTaskStatus(value: TaskStatus): String = value.name
    @TypeConverter fun toTaskStatus(value: String?): TaskStatus =
        value?.let {
            try {
                TaskStatus.valueOf(it)
            } catch (e: Exception) {
                Log.w(TAG, "Unknown TaskStatus: '$it', defaulting to ACTIVE", e)
                TaskStatus.ACTIVE
            }
        } ?: TaskStatus.ACTIVE

    @TypeConverter fun fromRecurrence(value: Recurrence): String = value.name
    @TypeConverter fun toRecurrence(value: String?): Recurrence =
        value?.let {
            try {
                Recurrence.valueOf(it)
            } catch (e: Exception) {
                Log.w(TAG, "Unknown Recurrence: '$it', defaulting to NONE", e)
                Recurrence.NONE
            }
        } ?: Recurrence.NONE

    @TypeConverter fun fromEnergyLevel(value: EnergyLevel): String = value.name
    @TypeConverter fun toEnergyLevel(value: String?): EnergyLevel =
        value?.let {
            try {
                EnergyLevel.valueOf(it)
            } catch (e: Exception) {
                Log.w(TAG, "Unknown EnergyLevel: '$it', defaulting to MEDIUM", e)
                EnergyLevel.MEDIUM
            }
        } ?: EnergyLevel.MEDIUM

    @TypeConverter fun fromTaskType(value: TaskType): String = value.name
    @TypeConverter fun toTaskType(value: String?): TaskType =
        value?.let {
            try {
                TaskType.valueOf(it)
            } catch (e: Exception) {
                Log.w(TAG, "Unknown TaskType: '$it', defaulting to ANALYTICAL", e)
                TaskType.ANALYTICAL
            }
        } ?: TaskType.ANALYTICAL

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
    @TypeConverter fun toRewardTier(value: String?): RewardTier =
        value?.let {
            try {
                RewardTier.valueOf(it)
            } catch (e: Exception) {
                Log.w(TAG, "Unknown RewardTier: '$it', defaulting to NONE", e)
                RewardTier.NONE
            }
        } ?: RewardTier.NONE

    @TypeConverter fun fromHyperFocusState(value: HyperFocusState): String = value.name
    @TypeConverter fun toHyperFocusState(value: String?): HyperFocusState =
        value?.let {
            try {
                HyperFocusState.valueOf(it)
            } catch (e: Exception) {
                Log.w(TAG, "Unknown HyperFocusState: '$it', defaulting to INACTIVE", e)
                HyperFocusState.INACTIVE
            }
        } ?: HyperFocusState.INACTIVE
}
