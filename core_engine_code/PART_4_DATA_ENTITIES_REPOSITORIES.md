# neuroFlow Core Engine — Part 4: Data Layer Architecture, Room Schemas, Entities, DAOs & Repositories

**Description:** Room Database v22 Schema, TaskEntity (composite indices), TaskMicroStepEntity, TaskTimeExtensions (dual-format timestamp resolution), EnergyPredictionEntity, ProtectedSlotEntity, SleepLogEntity, DAOs, TaskRepository (reactive StateFlow, completeAndRecur), Type Converters, UserPreferencesDataStore (BYOK, chronotype, directive, avoidance telemetry), and Hilt Dependency Injection Modules.  
**Generated for Vice Head Review:** 2026-10-23 22:05:10  
**Files:** 19  
**Lines of Code:** 2,674  
**Uncompressed Size:** 125.0 KB  

---

## Table of Contents

- [`app/src/main/java/com/neuroflow/app/data/local/entity/TaskEntity.kt`](#appsrcmainjavacomneuroflowappdatalocalentitytaskentitykt)
- [`app/src/main/java/com/neuroflow/app/data/local/entity/TaskMicroStepEntity.kt`](#appsrcmainjavacomneuroflowappdatalocalentitytaskmicrostepentitykt)
- [`app/src/main/java/com/neuroflow/app/data/local/entity/TaskTimeExtensions.kt`](#appsrcmainjavacomneuroflowappdatalocalentitytasktimeextensionskt)
- [`app/src/main/java/com/neuroflow/app/data/local/entity/EnergyPredictionEntity.kt`](#appsrcmainjavacomneuroflowappdatalocalentityenergypredictionentitykt)
- [`app/src/main/java/com/neuroflow/app/data/local/entity/ProtectedSlotEntity.kt`](#appsrcmainjavacomneuroflowappdatalocalentityprotectedslotentitykt)
- [`app/src/main/java/com/neuroflow/app/data/local/entity/SleepLogEntity.kt`](#appsrcmainjavacomneuroflowappdatalocalentitysleeplogentitykt)
- [`app/src/main/java/com/neuroflow/app/data/local/dao/TaskDao.kt`](#appsrcmainjavacomneuroflowappdatalocaldaotaskdaokt)
- [`app/src/main/java/com/neuroflow/app/data/local/dao/TaskMicroStepDao.kt`](#appsrcmainjavacomneuroflowappdatalocaldaotaskmicrostepdaokt)
- [`app/src/main/java/com/neuroflow/app/data/local/dao/ProtectedSlotDao.kt`](#appsrcmainjavacomneuroflowappdatalocaldaoprotectedslotdaokt)
- [`app/src/main/java/com/neuroflow/app/data/local/dao/EnergyPredictionDao.kt`](#appsrcmainjavacomneuroflowappdatalocaldaoenergypredictiondaokt)
- [`app/src/main/java/com/neuroflow/app/data/local/dao/SleepLogDao.kt`](#appsrcmainjavacomneuroflowappdatalocaldaosleeplogdaokt)
- [`app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt`](#appsrcmainjavacomneuroflowappdatarepositorytaskrepositorykt)
- [`app/src/main/java/com/neuroflow/app/data/local/Converters.kt`](#appsrcmainjavacomneuroflowappdatalocalconverterskt)
- [`app/src/main/java/com/neuroflow/app/data/local/UserPreferencesDataStore.kt`](#appsrcmainjavacomneuroflowappdatalocaluserpreferencesdatastorekt)
- [`app/src/main/java/com/neuroflow/app/domain/model/Enums.kt`](#appsrcmainjavacomneuroflowappdomainmodelenumskt)
- [`app/src/main/java/com/neuroflow/app/domain/model/ProtectedTimeSlot.kt`](#appsrcmainjavacomneuroflowappdomainmodelprotectedtimeslotkt)
- [`app/src/main/java/com/neuroflow/app/domain/model/StepByStepPlan.kt`](#appsrcmainjavacomneuroflowappdomainmodelstepbystepplankt)
- [`app/src/main/java/com/neuroflow/app/di/DomainServiceModule.kt`](#appsrcmainjavacomneuroflowappdidomainservicemodulekt)
- [`app/src/main/java/com/neuroflow/app/di/AppModule.kt`](#appsrcmainjavacomneuroflowappdiappmodulekt)

---

## Source Code Listings

<a id="appsrcmainjavacomneuroflowappdatalocalentitytaskentitykt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/entity/TaskEntity.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/entity/TaskEntity.kt`
- **Lines of Code:** 111
- **Size:** 5,010 bytes (4.9 KB)

```kotlin
package com.neuroflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import java.util.UUID

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["status", "scheduledDate"], name = "index_tasks_status_scheduledDate"),
        Index(value = ["status", "deadlineDate"], name = "index_tasks_status_deadlineDate"),
        Index(value = ["parentTaskId"], name = "index_tasks_parentTaskId")
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",

    // Classification
    val quadrant: Quadrant = Quadrant.SCHEDULE,
    val priority: Priority = Priority.MEDIUM,
    val tags: String = "",

    // Timing
    val deadlineDate: Long? = null,
    val deadlineTime: Long? = null,
    val scheduledDate: Long? = null,
    val scheduledTime: Long? = null,
    val isScheduleLocked: Boolean = false,
    val estimatedDurationMinutes: Int = 0,
    val recurrence: Recurrence = Recurrence.NONE,
    val recurrenceIntervalDays: Int = 1, // used when recurrence == CUSTOM

    // Reminders (bitmask: 15min=1, 30min=2, 1hr=4, 1day=8)
    val reminderFlags: Int = 0,

    // Scoring inputs
    val impactScore: Int = 50,          // 0-100: strategic value/impact
    val valueScore: Int = 50,           // 0-100: intrinsic value (separate from impact)
    val effortScore: Int = 50,          // 0-100: effort required (higher = harder, lowers priority for quick wins)
    val parentTaskId: String? = null,
    val blockingTaskIds: String = "",   // legacy: kept for migration compat
    val dependsOnTaskIds: String = "",  // IDs of tasks THIS task depends on (must be done first)
    val waitingFor: String = "",        // external dependency description (non-task)

    // Time tracking
    val totalTimeTrackedMinutes: Float = 0f,
    val sessionCount: Int = 0,
    val lastSessionDurationMinutes: Float? = null,

    // Adaptive difficulty
    val actualDurationMinutes: Float? = null,
    val estimationErrorMape: Float? = null,
    val estimationErrorSmape: Float? = null,

    // State
    val status: TaskStatus = TaskStatus.ACTIVE,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // Neuroscience extras
    val energyLevel: EnergyLevel = EnergyLevel.MEDIUM,
    val taskType: TaskType = TaskType.ANALYTICAL,   // circadian rhythm matching
    val contextTag: String = "",
    val goalId: String? = null,
    val ifThenPlan: String = "",
    val isHabitual: Boolean = false,
    val habitStreak: Int = 0,
    val isFrog: Boolean = false,
    val postponeCount: Int = 0,
    val focusModePoints: Int = 0,

    // Science-backed productivity boosters
    val enjoymentScore: Int = 50,       // 0-100: how enjoyable the task is (temptation bundling)
    val isPublicCommitment: Boolean = false, // commitment device — social accountability
    val isAnxietyTask: Boolean = false,  // stress inoculation — prevents avoidance spiral
    val goalRiskLevel: Int = 0,          // 0=none, 1=at risk, 2=critical — loss aversion boost
    val isAutoScheduled: Boolean = false,
    val lastAutoScheduledAt: Long? = null, // timestamp when task was last auto-scheduled (for replanning cooldown)

    // Habit anchor date — for recurring tasks this is always set and shifts by the recurrence interval
    // on each completion. Independent of deadline/scheduledDate so habits work without a deadline.
    val habitDate: Long? = null,

    // Behavioral motivation engine fields
    val affectiveForecastError: Float? = null,  // REAL nullable — difference between predicted and actual enjoyment
    val woopPromptShown: Boolean = false,        // INTEGER NOT NULL DEFAULT 0 — whether WOOP prompt has been shown

    // Digital Wellbeing distraction score (0–100), refreshed by DistractionSyncWorker
    // -1 = not yet computed (no usage data / permission not granted)
    val distractionScore: Float = -1f,

    // Explicit scheduling category — overrides tag-inferred category when set.
    // Null means the category is inferred at runtime from tags/taskType via determineCategory().
    val schedulingCategory: String? = null,

    // Minutes the auto-scheduler actually reserved for this task at its scheduled start. The
    // planner inflates the raw estimate by the user's learned bias and the slot's energy, so
    // anything asking "did this task get its time?" must read this rather than re-derive it.
    // 0 = never placed automatically (manual schedule, or scheduled before this field existed).
    val plannedBlockMinutes: Int = 0
)
```

---

<a id="appsrcmainjavacomneuroflowappdatalocalentitytaskmicrostepentitykt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/entity/TaskMicroStepEntity.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/entity/TaskMicroStepEntity.kt`
- **Lines of Code:** 30
- **Size:** 803 bytes (0.8 KB)

```kotlin
package com.neuroflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "task_micro_steps",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["taskId", "sortOrder"], name = "index_task_micro_steps_task_id_sort")
    ]
)
data class TaskMicroStepEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val title: String,
    val sortOrder: Int,
    val isCompleted: Boolean = false,
    val estimatedMinutes: Int = 5
)
```

---

<a id="appsrcmainjavacomneuroflowappdatalocalentitytasktimeextensionskt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/entity/TaskTimeExtensions.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/entity/TaskTimeExtensions.kt`
- **Lines of Code:** 139
- **Size:** 4,698 bytes (4.6 KB)

```kotlin
package com.neuroflow.app.data.local.entity

import com.neuroflow.app.domain.model.Recurrence
import java.util.Calendar

private const val MINUTES_PER_DAY = 24 * 60

/**
 * Ids of the tasks this task depends on.
 */
fun TaskEntity.dependencyIds(): List<String> =
    dependsOnTaskIds.split(',').map { it.trim() }.filter { it.isNotBlank() }

/**
 * Recurring tasks should anchor on habitDate when present.
 */
fun TaskEntity.isRecurringWithAnchor(): Boolean =
    recurrence != Recurrence.NONE && habitDate != null

const val ONE_DAY_MILLIS = 86_400_000L

/**
 * Resolves the scheduled epoch millis handling dual representation:
 * - scheduledTime as an intraday offset (< 86_400_000L)
 * - scheduledTime as an absolute epoch timestamp (>= 86_400_000L)
 */
fun TaskEntity.resolveScheduledEpochMillis(referenceDateMillis: Long = System.currentTimeMillis()): Long? {
    if (isRecurringWithAnchor()) {
        val baseDate = habitDate ?: return null
        val timeOffset = scheduledTime ?: deadlineTime ?: 0L
        return if (timeOffset >= ONE_DAY_MILLIS) timeOffset else startOfLocalDay(baseDate) + timeOffset
    }
    val rawTime = scheduledTime
    if (rawTime != null && rawTime >= ONE_DAY_MILLIS) {
        return rawTime
    }
    if (scheduledDate != null) {
        return scheduledDate + (rawTime ?: 0L)
    }
    if (rawTime != null && rawTime >= 0L) {
        return startOfLocalDay(referenceDateMillis) + rawTime
    }
    return habitDate?.let { startOfLocalDay(it) }
}

/**
 * Resolves the deadline epoch millis handling dual representation:
 * - deadlineTime as intraday offset vs absolute epoch millis
 */
fun TaskEntity.resolveDeadlineEpochMillis(): Long? {
    val rawTime = deadlineTime
    if (rawTime != null && rawTime >= ONE_DAY_MILLIS) return rawTime
    return deadlineDate?.let { it + (rawTime ?: 0L) }
}

/**
 * Normalizes an epoch timestamp into standard date (start of day) and time offset pair.
 */
fun buildNormalizedScheduleTimestamps(startEpochMillis: Long): Pair<Long, Long> {
    val dayStart = startOfLocalDay(startEpochMillis)
    val timeOffset = localTimeOfDayOffset(startEpochMillis)
    return dayStart to timeOffset
}

/**
 * Target used by reminder/notification flows.
 */
fun TaskEntity.effectiveReminderTargetMillis(): Long? {
    if (isRecurringWithAnchor()) {
        val baseDate = habitDate ?: return null
        val timeOffset = scheduledTime ?: deadlineTime ?: (9 * 3600_000L)
        return if (timeOffset >= ONE_DAY_MILLIS) timeOffset else startOfLocalDay(baseDate) + timeOffset
    }
    val deadline = resolveDeadlineEpochMillis()
    if (deadline != null) return deadline
    val scheduled = resolveScheduledEpochMillis()
    if (scheduled != null) return scheduled
    return habitDate?.let { startOfLocalDay(it) + (scheduledTime ?: deadlineTime ?: (9 * 3600_000L)) }
}

/**
 * Anchor used by schedule/time-block style flows.
 */
fun TaskEntity.effectiveScheduleAnchorMillis(): Long? {
    return resolveScheduledEpochMillis()
}

/**
 * Local minute-of-day for timeline placement.
 */
fun TaskEntity.timelineStartMinuteOfDay(): Int {
    val anchorMillis = resolveScheduledEpochMillis() ?: return 0
    val cal = Calendar.getInstance().apply { timeInMillis = anchorMillis }
    return (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE))
        .coerceIn(0, MINUTES_PER_DAY - 1)
}

/**
 * Local minute-of-day with explicit ZoneId for timeline placement.
 */
fun TaskEntity.timelineStartMinuteOfDay(zoneId: java.time.ZoneId): Int {
    val anchorMillis = resolveScheduledEpochMillis() ?: return 0
    val zdt = java.time.Instant.ofEpochMilli(anchorMillis).atZone(zoneId)
    return (zdt.hour * 60 + zdt.minute).coerceIn(0, MINUTES_PER_DAY - 1)
}

fun startOfLocalDay(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

fun startOfNextLocalDay(millis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = startOfLocalDay(millis)
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis

fun startOfPreviousLocalDay(millis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = startOfLocalDay(millis)
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis

fun localTimeOfDayOffset(millis: Long): Long {
    val dayStart = startOfLocalDay(millis)
    return (millis - dayStart).coerceAtLeast(0L)
}

fun splitToLocalDateAndTime(millis: Long): Pair<Long, Long> {
    val date = startOfLocalDay(millis)
    val time = localTimeOfDayOffset(millis)
    return date to time
}
```

---

<a id="appsrcmainjavacomneuroflowappdatalocalentityenergypredictionentitykt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/entity/EnergyPredictionEntity.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/entity/EnergyPredictionEntity.kt`
- **Lines of Code:** 65
- **Size:** 3,128 bytes (3.1 KB)

```kotlin
package com.neuroflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 2 telemetry entity: stores snapshots of energy predictions with full component breakdowns.
 * Used for audit trails, backtesting, calibration validation, and local analytics.
 *
 * One record per prediction refresh (~60 seconds). Bounded retention: 30 days raw, 90 days aggregated.
 */
@Entity(
    tableName = "energy_predictions",
    indices = [
        Index(value = ["predictedAtMillis"], name = "index_energy_predictions_predictedAtMillis")
    ]
)
data class EnergyPredictionEntity(
    @PrimaryKey
    val id: String,

    // Temporal context
    val predictedAtMillis: Long,
    val predictedAtDayOfWeek: Int,        // 1=Monday .. 7=Sunday (ISO-8601, from java.time.DayOfWeek.value)
    val predictedAtHourOfDay: Int,        // 0..23

    // Ground truth inputs (freshness guards)
    val peakDetectionAgeMillis: Long,     // How old was the peak detection?
    val sleepLogAgeMillis: Long,          // How recently was sleep logged?
    val sessionDataAgeMillis: Long,       // How old is the last session event?

    // Component scores (for diagnostics and calibration)
    val baselineRawEnergy: Float,         // -100..100 raw energy before moment adjustment
    val peakScore: Float,                 // Circadian baseline contribution
    val fatiguePenalty: Float,            // Sleep pressure penalty to baseline
    val sleepPressurePoints: Int,         // Raw sleep pressure accumulation
    val fatiguePercent: Int,              // 0..100% fatigue zone

    // Moment adjustment layer (Phase 1 guards applied)
    val momentAdjustment: Float,          // ±24pt from interruptions/tasks/notifications
    val momentConfidence: Float,          // 0..1 confidence in moment adjustment
    val momentSupportScore: Float,        // Focus density, calmness signal
    val momentPressureScore: Float,       // Sleep pressure, peak age, task load

    // Final output
    val adjustedRawEnergy: Float,         // Baseline + (adjustment × confidence)
    val usableEnergy: Int,                // 0..100 user-facing energy percentage

    // Chronotype context (for future per-type adaptive tuning)
    val chronotype: String,               // DEFINITE_MORNING, MODERATE_MORNING, INTERMEDIATE, etc.
    val wakeUpHour: Int,                  // User's configured wake time
    val peakMinuteOfDay: Int,             // Computed peak time for this chronotype
    val peakConfidence: Float,            // Confidence in baseline peak computation

    // Feature signals (for feature importance tracking)
    val recentFocusMinutes: Float,        // Last 180-min focused work time
    val recentInterruptionCount: Int,     // Session interruptions in window
    val recentAppSwitchCount: Int,        // App switches in window
    val activeTaskCount: Int,             // Currently active open tasks
    val notificationCount: Int,           // Badge count from notifications

    // Retention marker (for cleanup)
    val createdAtMillis: Long = System.currentTimeMillis()
)
```

---

<a id="appsrcmainjavacomneuroflowappdatalocalentityprotectedslotentitykt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/entity/ProtectedSlotEntity.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/entity/ProtectedSlotEntity.kt`
- **Lines of Code:** 16
- **Size:** 436 bytes (0.4 KB)

```kotlin
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
```

---

<a id="appsrcmainjavacomneuroflowappdatalocalentitysleeplogentitykt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/entity/SleepLogEntity.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/entity/SleepLogEntity.kt`
- **Lines of Code:** 23
- **Size:** 569 bytes (0.6 KB)

```kotlin
package com.neuroflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sleep_logs",
    indices = [
        Index(value = ["startAt"]),
        Index(value = ["endAt"])
    ]
)
data class SleepLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val startAt: Long,
    val endAt: Long,
    val durationMinutes: Int,
    val source: String = "MANUAL",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
```

---

<a id="appsrcmainjavacomneuroflowappdatalocaldaotaskdaokt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/dao/TaskDao.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/dao/TaskDao.kt`
- **Lines of Code:** 78
- **Size:** 2,902 bytes (2.8 KB)

```kotlin
package com.neuroflow.app.data.local.dao

import androidx.room.*
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: TaskStatus): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE quadrant = :quadrant AND status = 'ACTIVE' ORDER BY createdAt DESC")
    fun observeByQuadrant(quadrant: Quadrant): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    fun observeActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun observeCompletedTasks(): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE status = 'ACTIVE'
          AND (
                (scheduledDate >= :dayStart AND scheduledDate < :dayEnd)
                OR
                (recurrence != 'NONE' AND habitDate >= :dayStart AND habitDate < :dayEnd)
              )
    """)
    fun observeTasksForDate(dayStart: Long, dayEnd: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'ACTIVE'")
    suspend fun getActiveTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status = 'COMPLETED'")
    suspend fun getCompletedTasks(): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'ACTIVE' AND quadrant = :quadrant")
    fun observeQuadrantCount(quadrant: Quadrant): Flow<Int>

    @Query("SELECT * FROM tasks WHERE contextTag = :tag AND status = 'ACTIVE'")
    fun observeByContextTag(tag: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE goalId = :goalId AND status = 'ACTIVE'")
    fun observeByGoalId(goalId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentId")
    fun observeSubtasks(parentId: String): Flow<List<TaskEntity>>

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Query("UPDATE tasks SET estimationErrorMape = NULL, estimationErrorSmape = NULL, actualDurationMinutes = NULL WHERE status = 'COMPLETED'")
    suspend fun resetEstimationErrors()
}
```

---

<a id="appsrcmainjavacomneuroflowappdatalocaldaotaskmicrostepdaokt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/dao/TaskMicroStepDao.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/dao/TaskMicroStepDao.kt`
- **Lines of Code:** 32
- **Size:** 1,084 bytes (1.1 KB)

```kotlin
package com.neuroflow.app.data.local.dao

import androidx.room.*
import com.neuroflow.app.data.local.entity.TaskMicroStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskMicroStepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: TaskMicroStepEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(steps: List<TaskMicroStepEntity>)

    @Update
    suspend fun update(step: TaskMicroStepEntity)

    @Delete
    suspend fun delete(step: TaskMicroStepEntity)

    @Query("DELETE FROM task_micro_steps WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: String)

    @Query("SELECT * FROM task_micro_steps WHERE taskId = :taskId ORDER BY sortOrder ASC")
    fun observeByTaskId(taskId: String): Flow<List<TaskMicroStepEntity>>

    @Query("SELECT * FROM task_micro_steps WHERE taskId = :taskId ORDER BY sortOrder ASC")
    suspend fun getByTaskId(taskId: String): List<TaskMicroStepEntity>

    @Query("SELECT * FROM task_micro_steps")
    suspend fun getAll(): List<TaskMicroStepEntity>
}
```

---

<a id="appsrcmainjavacomneuroflowappdatalocaldaoprotectedslotdaokt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/dao/ProtectedSlotDao.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/dao/ProtectedSlotDao.kt`
- **Lines of Code:** 32
- **Size:** 982 bytes (1.0 KB)

```kotlin
package com.neuroflow.app.data.local.dao

import androidx.room.*
import com.neuroflow.app.data.local.entity.ProtectedSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProtectedSlotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(slot: ProtectedSlotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(slots: List<ProtectedSlotEntity>)

    @Update
    suspend fun update(slot: ProtectedSlotEntity)

    @Delete
    suspend fun delete(slot: ProtectedSlotEntity)

    @Query("SELECT * FROM protected_slots WHERE isEnabled = 1")
    fun observeActive(): Flow<List<ProtectedSlotEntity>>

    @Query("SELECT * FROM protected_slots")
    fun observeAll(): Flow<List<ProtectedSlotEntity>>

    @Query("SELECT * FROM protected_slots WHERE isEnabled = 1")
    suspend fun getActive(): List<ProtectedSlotEntity>

    @Query("SELECT * FROM protected_slots")
    suspend fun getAll(): List<ProtectedSlotEntity>
}
```

---

<a id="appsrcmainjavacomneuroflowappdatalocaldaoenergypredictiondaokt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/dao/EnergyPredictionDao.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/dao/EnergyPredictionDao.kt`
- **Lines of Code:** 31
- **Size:** 1,247 bytes (1.2 KB)

```kotlin
package com.neuroflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.neuroflow.app.data.local.entity.EnergyPredictionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EnergyPredictionDao {
    @Insert
    suspend fun insert(entity: EnergyPredictionEntity)

    @Query("SELECT * FROM energy_predictions ORDER BY predictedAtMillis DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<EnergyPredictionEntity>

    @Query("SELECT * FROM energy_predictions WHERE predictedAtMillis >= :startMillis AND predictedAtMillis <= :endMillis ORDER BY predictedAtMillis DESC")
    suspend fun getInRange(startMillis: Long, endMillis: Long): List<EnergyPredictionEntity>

    @Query("SELECT COUNT(*) FROM energy_predictions")
    suspend fun count(): Int

    @Query("DELETE FROM energy_predictions WHERE createdAtMillis < :beforeMillis")
    suspend fun deleteOlderThan(beforeMillis: Long): Int

    @Query("DELETE FROM energy_predictions")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM energy_predictions WHERE predictedAtMillis >= :afterMillis ORDER BY predictedAtMillis DESC")
    fun observeAfter(afterMillis: Long): Flow<List<EnergyPredictionEntity>>
}
```

---

<a id="appsrcmainjavacomneuroflowappdatalocaldaosleeplogdaokt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/dao/SleepLogDao.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/dao/SleepLogDao.kt`
- **Lines of Code:** 32
- **Size:** 1,069 bytes (1.0 KB)

```kotlin
package com.neuroflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neuroflow.app.data.local.entity.SleepLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: SleepLogEntity)

    @Query("SELECT * FROM sleep_logs ORDER BY startAt DESC")
    fun observeAll(): Flow<List<SleepLogEntity>>

    @Query("SELECT * FROM sleep_logs ORDER BY startAt DESC")
    suspend fun getAll(): List<SleepLogEntity>

    @Query("SELECT * FROM sleep_logs WHERE endAt > :fromMillis AND startAt < :toMillis ORDER BY startAt ASC")
    suspend fun getOverlapping(fromMillis: Long, toMillis: Long): List<SleepLogEntity>

    @Query("SELECT * FROM sleep_logs WHERE id = :id")
    suspend fun getById(id: String): SleepLogEntity?

    @Query("DELETE FROM sleep_logs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM sleep_logs")
    suspend fun deleteAll()
}
```

---

<a id="appsrcmainjavacomneuroflowappdatarepositorytaskrepositorykt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt`
- **Lines of Code:** 399
- **Size:** 16,590 bytes (16.2 KB)

```kotlin
package com.neuroflow.app.data.repository

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.neuroflow.app.data.local.dao.TaskDao
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.AutoSchedulingContracts
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.worker.ScheduleAutoTasksWorker
import com.neuroflow.app.data.local.dao.TaskMicroStepDao
import com.neuroflow.app.data.local.entity.TaskMicroStepEntity
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private fun Long.toDayStart(): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = this@toDayStart }
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun Calendar.addRecurrenceStep(recurrence: Recurrence, customDays: Int) {
    when (recurrence) {
        Recurrence.DAILY -> add(Calendar.DAY_OF_YEAR, 1)
        Recurrence.WEEKLY -> add(Calendar.WEEK_OF_YEAR, 1)
        Recurrence.MONTHLY -> add(Calendar.MONTH, 1)
        Recurrence.CUSTOM -> add(Calendar.DAY_OF_YEAR, customDays.coerceAtLeast(1))
        Recurrence.NONE -> Unit
    }
}

private fun nextRecurringAnchorAfter(
    recurrence: Recurrence,
    customDays: Int,
    currentAnchor: Long,
    now: Long
): Long {
    if (recurrence == Recurrence.NONE) return currentAnchor
    val cal = Calendar.getInstance().apply { timeInMillis = currentAnchor }
    // Always move to the next cycle, then roll forward if the anchor is still in the past.
    cal.addRecurrenceStep(recurrence, customDays)
    while (cal.timeInMillis <= now) {
        cal.addRecurrenceStep(recurrence, customDays)
    }
    return cal.timeInMillis
}


@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val hyperFocusManager: Lazy<HyperFocusManager>,
    private val workManager: WorkManager,
    private val taskMicroStepDao: TaskMicroStepDao? = null
) {
    // Secondary constructor for tests or callers providing only base dependencies
    constructor(
        taskDao: TaskDao,
        hyperFocusManager: Lazy<HyperFocusManager>,
        workManager: WorkManager
    ) : this(taskDao, hyperFocusManager, workManager, null)

    fun observeMicroSteps(taskId: String): Flow<List<TaskMicroStepEntity>> =
        taskMicroStepDao?.observeByTaskId(taskId) ?: flowOf(emptyList())

    suspend fun getMicroSteps(taskId: String): List<TaskMicroStepEntity> =
        taskMicroStepDao?.getByTaskId(taskId) ?: emptyList()

    suspend fun insertMicroStep(step: TaskMicroStepEntity) {
        taskMicroStepDao?.insert(step)
    }

    suspend fun insertMicroSteps(steps: List<TaskMicroStepEntity>) {
        taskMicroStepDao?.insertAll(steps)
    }

    suspend fun updateMicroStep(step: TaskMicroStepEntity) {
        taskMicroStepDao?.update(step)
    }

    suspend fun deleteMicroStep(step: TaskMicroStepEntity) {
        taskMicroStepDao?.delete(step)
    }

    suspend fun deleteMicroStepsByTaskId(taskId: String) {
        taskMicroStepDao?.deleteByTaskId(taskId)
    }

    suspend fun toggleMicroStepCompleted(step: TaskMicroStepEntity) {
        taskMicroStepDao?.update(step.copy(isCompleted = !step.isCompleted))
    }
    fun observeAll(): Flow<List<TaskEntity>> = taskDao.observeAll()
    fun observeActiveTasks(): Flow<List<TaskEntity>> = taskDao.observeActiveTasks()
    fun observeCompletedTasks(): Flow<List<TaskEntity>> = taskDao.observeCompletedTasks()
    fun observeByQuadrant(quadrant: Quadrant): Flow<List<TaskEntity>> = taskDao.observeByQuadrant(quadrant)
    fun observeByStatus(status: TaskStatus): Flow<List<TaskEntity>> = taskDao.observeByStatus(status)
    fun observeTasksForDate(date: Long): Flow<List<TaskEntity>> {
        val dayStart = date.toDayStart()
        val dayEnd = Calendar.getInstance().apply {
            timeInMillis = dayStart
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        return taskDao.observeTasksForDate(dayStart, dayEnd)
    }
    fun observeQuadrantCount(quadrant: Quadrant): Flow<Int> = taskDao.observeQuadrantCount(quadrant)
    fun observeById(id: String): Flow<TaskEntity?> = taskDao.observeById(id)
    fun observeByContextTag(tag: String): Flow<List<TaskEntity>> = taskDao.observeByContextTag(tag)
    fun observeByGoalId(goalId: String): Flow<List<TaskEntity>> = taskDao.observeByGoalId(goalId)
    fun observeSubtasks(parentId: String): Flow<List<TaskEntity>> = taskDao.observeSubtasks(parentId)

    suspend fun getById(id: String): TaskEntity? = taskDao.getById(id)
    suspend fun getActiveTasks(): List<TaskEntity> = taskDao.getActiveTasks()
    suspend fun getAllTasks(): List<TaskEntity> = taskDao.getAllTasks()
    suspend fun getCompletedTasks(): List<TaskEntity> = taskDao.getCompletedTasks()

    suspend fun insert(task: TaskEntity) {
        taskDao.insert(task)
        if (!AutoSchedulingContracts.hasManualScheduleData(task) && task.status != TaskStatus.COMPLETED) {
            enqueueAutoSchedulingRecheck()
        }
    }
    suspend fun update(task: TaskEntity) {
        val old = taskDao.getById(task.id)
        // plannedBlockMinutes is the length the planner reserved, and TaskBlockPlan prefers it over
        // the estimate. Left stale it silently overrides a new duration until the task happens to be
        // replanned, so a reschedule that never fires keeps blocking and expiring the old block.
        val estimateChanged = old != null && old.estimatedDurationMinutes != task.estimatedDurationMinutes
        val toWrite = if (estimateChanged) task.copy(plannedBlockMinutes = 0) else task
        taskDao.update(toWrite)

        // Trigger rescheduling if relevant fields changed
        if (old != null && shouldTriggerReschedule(old, toWrite)) {
            enqueueAutoSchedulingRecheck()
        }
    }

    private fun shouldTriggerReschedule(old: TaskEntity, new: TaskEntity): Boolean {
        return old.deadlineDate != new.deadlineDate ||
               old.deadlineTime != new.deadlineTime ||
               old.priority != new.priority ||
               old.estimatedDurationMinutes != new.estimatedDurationMinutes ||
               old.effortScore != new.effortScore ||
               old.tags != new.tags ||
               old.taskType != new.taskType ||
               old.energyLevel != new.energyLevel ||
               // Task was unscheduled (manual or incomplete)
               (old.scheduledDate != null && new.scheduledDate == null)
    }

    suspend fun markIncomplete(task: TaskEntity, timeSpentMinutes: Int) {
        update(task.copy(
            scheduledDate = null,
            scheduledTime = null,
            totalTimeTrackedMinutes = task.totalTimeTrackedMinutes + timeSpentMinutes,
            updatedAt = System.currentTimeMillis()
        ))
        // Rescheduling trigger already called in update()
    }

    suspend fun postponeTask(task: TaskEntity) {
        update(task.copy(
            postponeCount = task.postponeCount + 1,
            scheduledDate = null,
            scheduledTime = null,
            updatedAt = System.currentTimeMillis()
        ))
        // Rescheduling trigger already called in update()
    }

    /**
     * Moves a task to tomorrow immediately, releasing the time-of-day so the auto-scheduler
     * claims it. A date-only change is not covered by shouldTriggerReschedule, so the recheck
     * is enqueued here.
     */
    suspend fun rescheduleToTomorrow(task: TaskEntity) {
        val tomorrowStart = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        update(task.copy(
            postponeCount = task.postponeCount + 1,
            scheduledDate = tomorrowStart,
            scheduledTime = null,
            isAutoScheduled = false,
            updatedAt = System.currentTimeMillis()
        ))
        enqueueAutoSchedulingRecheck()
    }
    suspend fun delete(task: TaskEntity) {
        if (hyperFocusManager.get().isTaskDeletionBlocked(task.id)) return
        taskDao.delete(task)
        // Clean up dependencies referencing this deleted task to avoid orphan deadlocks
        val activeTasks = taskDao.getActiveTasks()
        activeTasks.forEach { other ->
            if (other.dependsOnTaskIds.isNotBlank()) {
                val currentDeps = other.dependsOnTaskIds.split(",").map { it.trim() }.filter { it.isNotBlank() }
                if (task.id in currentDeps) {
                    val remainingDeps = currentDeps.filter { it != task.id }
                    taskDao.update(
                        other.copy(
                            dependsOnTaskIds = remainingDeps.joinToString(","),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        enqueueAutoSchedulingRecheck()
    }
    suspend fun deleteAll() = taskDao.deleteAll()
    suspend fun resetEstimationErrors() = taskDao.resetEstimationErrors()

    /**
     * Splits a task into multiple sequential sessions (e.g. 45-min chunks).
     * Connects sessions via `parentTaskId` and sequential `dependsOnTaskIds`.
     * Marks the parent task as completed/archived and triggers auto-scheduling.
     */
    suspend fun splitTaskIntoSessions(
        task: TaskEntity,
        sessionDurationMinutes: Int = 45
    ): List<TaskEntity> {
        val totalMinutes = if (task.estimatedDurationMinutes > 0) {
            task.estimatedDurationMinutes
        } else {
            90
        }

        val effectiveSessionMinutes = sessionDurationMinutes.coerceIn(15, 120)
        val sessionCount = maxOf(2, ((totalMinutes + effectiveSessionMinutes - 1) / effectiveSessionMinutes))
        val minutesPerSession = (totalMinutes / sessionCount).coerceAtLeast(15)
        val remainder = totalMinutes - (minutesPerSession * (sessionCount - 1))

        val now = System.currentTimeMillis()
        val createdSessions = mutableListOf<TaskEntity>()
        var previousSessionId = ""

        for (i in 1..sessionCount) {
            val duration = if (i == sessionCount) remainder.coerceAtLeast(15) else minutesPerSession
            val sessionTask = TaskEntity(
                id = java.util.UUID.randomUUID().toString(),
                title = "${task.title} (Part $i/$sessionCount)",
                description = task.description,
                quadrant = task.quadrant,
                priority = task.priority,
                deadlineDate = task.deadlineDate,
                deadlineTime = task.deadlineTime,
                estimatedDurationMinutes = duration,
                parentTaskId = task.id,
                dependsOnTaskIds = previousSessionId,
                impactScore = task.impactScore,
                valueScore = task.valueScore,
                effortScore = task.effortScore,
                taskType = task.taskType,
                energyLevel = task.energyLevel,
                contextTag = task.contextTag,
                goalId = task.goalId,
                isFrog = if (i == 1) task.isFrog else false,
                tags = task.tags,
                ifThenPlan = task.ifThenPlan,
                status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE,
                createdAt = now + i,
                updatedAt = now + i
            )
            createdSessions.add(sessionTask)
            taskDao.insert(sessionTask)
            previousSessionId = sessionTask.id
        }

        // Archive parent task as completed
        taskDao.update(
            task.copy(
                status = com.neuroflow.app.domain.model.TaskStatus.COMPLETED,
                completedAt = now,
                updatedAt = now
            )
        )

        enqueueAutoSchedulingRecheck()
        return createdSessions
    }

    private fun enqueueAutoSchedulingRecheck() {
        val request = OneTimeWorkRequestBuilder<ScheduleAutoTasksWorker>().build()
        workManager.enqueueUniqueWork(
            "${ScheduleAutoTasksWorker.WORK_NAME}_trigger",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    suspend fun markTaskCompleted(taskId: String, now: Long = System.currentTimeMillis()): Boolean {
        val task = getById(taskId) ?: return false
        completeAndRecur(task, now)
        return true
    }

    /**
     * Marks [task] as completed and, if it has recurrence, inserts the next occurrence.
     * Uses calendar-aware stepping (daily/weekly/monthly/custom days) and guarantees
     * the new occurrence anchor lands strictly in the future.
     */
    suspend fun completeAndRecur(task: TaskEntity, now: Long): String? {
        update(
            task.copy(
                status = TaskStatus.COMPLETED,
                completedAt = now,
                isHabitual = task.recurrence != Recurrence.NONE,
                habitStreak = if (task.recurrence != Recurrence.NONE) task.habitStreak + 1 else task.habitStreak,
                updatedAt = now
            )
        )
        hyperFocusManager.get().onTaskCompleted()
        enqueueAutoSchedulingRecheck()

        if (task.recurrence == Recurrence.NONE) return null

        // Anchor for recurrence progression: prefer habitDate, then scheduled/deadline with time, then now.
        val currentAnchor =
            task.habitDate
                ?: task.scheduledDate?.let { it + (task.scheduledTime ?: 0L) }
                ?: task.deadlineDate?.let { it + (task.deadlineTime ?: 0L) }
                ?: now
        val nextAnchor = nextRecurringAnchorAfter(
            recurrence = task.recurrence,
            customDays = task.recurrenceIntervalDays,
            currentAnchor = currentAnchor,
            now = now
        )
        val nextDayStart = nextAnchor.toDayStart()
        val (nextScheduledDate, nextScheduledTime) = if (task.scheduledDate != null) {
            nextDayStart to task.scheduledTime
        } else {
            null to null
        }
        val (nextDeadlineDate, nextDeadlineTime) = if (task.deadlineDate != null) {
            val anchorDate = Instant.ofEpochMilli(startOfLocalDay(currentAnchor))
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val deadlineDate = Instant.ofEpochMilli(startOfLocalDay(task.deadlineDate))
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val deadlineDayOffset = ChronoUnit.DAYS.between(anchorDate, deadlineDate)
                .coerceAtLeast(0L)
            val nextDeadline = Calendar.getInstance().apply {
                timeInMillis = nextDayStart
                add(Calendar.DAY_OF_YEAR, deadlineDayOffset.toInt())
            }.timeInMillis
            nextDeadline to task.deadlineTime
        } else {
            null to null
        }

        val newId = UUID.randomUUID().toString()
        insert(
            task.copy(
                id = newId,
                status = TaskStatus.ACTIVE,
                completedAt = null,
                habitDate = nextAnchor.toDayStart(),
                deadlineDate = nextDeadlineDate,
                deadlineTime = nextDeadlineTime,
                scheduledDate = nextScheduledDate,
                scheduledTime = nextScheduledTime,
                isScheduleLocked = task.isScheduleLocked,
                totalTimeTrackedMinutes = 0f,
                sessionCount = 0,
                lastSessionDurationMinutes = null,
                actualDurationMinutes = null,
                estimationErrorMape = null,
                estimationErrorSmape = null,
                focusModePoints = 0,
                postponeCount = 0,
                habitStreak = task.habitStreak + 1,
                isHabitual = true,
                createdAt = now,
                updatedAt = now
            )
        )
        return newId
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdatalocalconverterskt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/Converters.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/Converters.kt`
- **Lines of Code:** 92
- **Size:** 3,432 bytes (3.4 KB)

```kotlin
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
```

---

<a id="appsrcmainjavacomneuroflowappdatalocaluserpreferencesdatastorekt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/UserPreferencesDataStore.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/UserPreferencesDataStore.kt`
- **Lines of Code:** 1,197
- **Size:** 73,477 bytes (71.8 KB)

```kotlin
package com.neuroflow.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.scheduler.TaskTagSchedulingProfile
import com.neuroflow.app.domain.model.AppTheme
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsCategory
import com.neuroflow.app.domain.model.PtsRecurrence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.ceil
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * ADR-004 ARCHITECTURAL DIRECTIVE (Track 1):
 * FROZEN TO STRICTLY SCALAR PRIMITIVES GOING FORWARD.
 *
 * All complex, nested, or relational domain models MUST be persisted in Room child tables
 * (e.g. Room v22 `task_micro_steps`, `focus_sessions`, `quick_notes`, `protected_slots`).
 * ViewModels collecting [preferencesFlow] MUST apply `.distinctUntilChanged { old, new -> ... }`
 * on only the specific scalar fields required for their state evaluation.
 */
data class UserPreferences(
    val wakeUpHour: Int = 7,
    val sleepHour: Int = 23,
    val peakEnergyStart: Int = 9,
    val peakEnergyEnd: Int = 12,
    val workDayStart: Int = 8,
    val workDayEnd: Int = 20,
    val defaultPomodoroMinutes: Int = 25,
    val defaultBreakMinutes: Int = 5,
    val identityLabel: String = "",
    val topGoal: String = "",
    val dailyStreak: Int = 0,
    val lastActiveDate: Long = 0L,
    val longestStreak: Int = 0,
    val totalTasksCompleted: Int = 0,
    val totalFocusMinutes: Int = 0,
    val weightQuadrant: Float = 1.0f,
    val weightDeadlineUrgency: Float = 1.0f,
    val weightPriorityLevel: Float = 1.0f,
    val weightDuration: Float = 1.0f,
    val weightImpact: Float = 1.0f,
    val weightFocusMode: Float = 1.0f,
    val weightCircadianMatch: Float = 1.0f,
    val weightFrogBonus: Float = 1.0f,
    val weightPlanBoost: Float = 1.0f,
    val weightSessionProgress: Float = 1.0f,
    val weightEnjoyment: Float = 1.0f,
    // Enjoyment points subtracted from every new enjoyment forecast, learned from how finished
    // tasks actually felt. Negative means the user tends to overestimate how much they'll enjoy work.
    val enjoymentForecastBias: Float = 0f,
    val onboardingCompleted: Boolean = false,
    val theme: AppTheme = AppTheme.SYSTEM,
    val weeklyIntent: String = "",
    val weeklyIntentIsoWeek: Int = 0,
    val weeklyIntentIsoYear: Int = 0,
    val lastFreshStartShownWeek: Int = 0,
    val lastFreshStartShownYear: Int = 0,
    val lastAppOpenMillis: Long = 0L,
    val firstAppOpenMillis: Long = 0L,
    // Dynamic peak energy detection
    val detectedPeakStart: Int = -1,      // -1 = not yet detected
    val detectedPeakEnd: Int = -1,
    val detectedPeakMinuteOfDay: Int = -1,
    val peakDetectionConfidence: Float = 0f,  // 0.0–1.0
    // Blended effective peak (manual lerp'd with detected, based on confidence)
    val effectivePeakStart: Int = -1,     // -1 = use peakEnergyStart
    val effectivePeakEnd: Int = -1,       // -1 = use peakEnergyEnd
    val effectivePeakMinuteOfDay: Int = -1,
    val quizPeakEnabled: Boolean = true,
    // Subliminal affirmations — stored as JSON array string
    val affirmations: List<String> = emptyList(),
    // Persistent task tag catalog shown in task creation and history filters
    val tagCatalog: List<String> = emptyList(),
    // Auto scheduling controls
    val autoSchedulingEnabled: Boolean = true,
    val isAutoScheduleToggleOn: Boolean = false,
    val autoSchedulingHorizonDays: Int = 3,
    val autoSchedulingBreakAfterCognitiveMinutes: Int = 90,
    val autoSchedulingBreakDurationMinutes: Int = 15,
    val autoSchedulingBackgroundThrottleMinutes: Int = 30,
    // Top 3 goals for the year (JSON array)
    val yearlyGoals: List<String> = emptyList(),
    // Top 3 goals for the current week (JSON array)
    val weeklyGoals: List<String> = emptyList(),
    // Tracks when we last showed the yearly goals refill prompt
    val lastYearlyGoalShownYear: Int = 0,
    // Tracks when we last showed the weekly goals refill prompt (ISO week + year)
    val lastWeeklyGoalShownWeek: Int = 0,
    val lastWeeklyGoalShownYear: Int = 0,
    // ADR-005: Persistent Skip & Handled Guards (scalar ints)
    val lastYearlyGoalHandledYear: Int = 0,
    val lastWeeklyGoalHandledWeek: Int = 0,
    // Focus behaviour toggles
    val woopEnabled: Boolean = true,
    val autoTrackerEnabled: Boolean = false,
    // Notification preferences
    val dailyPlanNotificationsEnabled: Boolean = true,
    val streakNotificationsEnabled: Boolean = true,
    val autonomyNudgeNotificationsEnabled: Boolean = true,
    val deadlineReminderNotificationsEnabled: Boolean = true,
    val deadlineEscalationNotificationsEnabled: Boolean = true,
    val dailyPlanNotificationHour: Int = 7,
    val streakCheckNotificationHour: Int = 21,
    val userGuidePromptShown: Boolean = false,
    // Left page quick note
    val leftPageQuickNote: String = "",
    // MEQ Chronotype - manual user selection from onboarding
    val manualChronotype: String? = null,
    // MEQ Chronotype - result from completed quiz
    val quizChronotype: String? = null,
    // Quiz progress - JSON string storing which questions answered and selected answers
    val quizProgress: String = "{}",
    // Sleep pressure state (recomputed from logs + elapsed awake time)
    val sleepPressurePoints: Int = 0,
    val sleepPressureTrackingStartedAtMillis: Long = 0L,
    val sleepPressureLastComputedAtMillis: Long = 0L,
    val autoFallbackSleepInsertionEnabled: Boolean = true,
    val dismissedSleepFallbacks: Set<String> = emptySet(),
    // Daily deep-work budget on the Today screen, and the apps that count as distraction
    val dailyDeepWorkTargetMinutes: Int = 240,
    val distractingPackages: Set<String> = BehavioralSupportService.DEFAULT_DISTRACTING_PACKAGES,
    // Moment model sensitivity controls
    val momentInterruptionSensitivity: Float = 1.0f,
    val momentNotificationSensitivity: Float = 1.0f,
    val momentTaskPressureSensitivity: Float = 1.0f,
    // Local telemetry privacy controls
    val energyTelemetryEnabled: Boolean = true,
    val energyTelemetryRetentionDays: Int = 30,
    // Morning calibration auto-tune coefficients (bounded adaptive blend)
    val morningTuneSleepWeight: Float = 0.30f,
    val morningTuneWakeWeight: Float = 0.25f,
    val morningTuneBehaviorWeight: Float = 0.25f,
    val morningTuneBaseWeight: Float = 0.20f,
    val morningTuneUpdatedAtMillis: Long = 0L,
    val morningTuneVersion: Int = 1,
    val adaptivePeakFreezeEnabled: Boolean = false,
    val peakQualityDegradeStreak: Int = 0,
    val peakConfidenceAbstentionEnabled: Boolean = false,
    val peakConfidenceAbstentionReason: String = "",
    val peakConfidenceAbstentionTriggerCount: Int = 0,
    val peakConfidenceAbstentionRecoveryCount: Int = 0,
    val peakConfidenceAbstentionLastChangedAtMillis: Long = 0L,
    val peakConfidenceAbstentionReasonFreezeCount: Int = 0,
    val peakConfidenceAbstentionReasonLowSamplesCount: Int = 0,
    val peakConfidenceAbstentionReasonLowCoverageCount: Int = 0,
    val peakConfidenceAbstentionReasonWakeVarianceCount: Int = 0,
    val peakConfidenceAbstentionReasonDivergenceCount: Int = 0,
    val peakConfidenceAbstentionReasonOtherCount: Int = 0,
    // Manual peak profile override controls
    val manualPeakProfileEnabled: Boolean = false,
    val manualPeakProfileType: String = "AUTO",
    val manualPeakAnchorMinuteOfDay: Int = 360,
    val manualPeakWindow1StartOffsetMinutes: Int = 0,
    val manualPeakWindow2StartOffsetMinutes: Int = 570,
    val manualPeakWindow3StartOffsetMinutes: Int = 810,
    val manualPeakWindow1DurationMinutes: Int = 210,
    val manualPeakWindow2DurationMinutes: Int = 150,
    val manualPeakWindow3DurationMinutes: Int = 60,
    val manualPeakWindow1Amplitude: Float = 1.0f,
    val manualPeakWindow2Amplitude: Float = 0.8f,
    val manualPeakWindow3Amplitude: Float = 0.6f,
    val protectedTimeSlots: List<ProtectedTimeSlot> = emptyList(),
    val dynamicColorEnabled: Boolean = true,
    val amoledDarkEnabled: Boolean = false,
    val dndEnabled: Boolean = true,
    val useV2Ui: Boolean = true,
    val geminiApiKey: String = "",
    val geminiAiEnabled: Boolean = true,
    val adaptiveSystemDirective: String = "",
    val weeklyAiAuditJson: String = "",
    val recentlyDeletedTasksJson: String = "[]",
    val stimulantLogsJson: String = "[]",
    val quickPhysiologicalLogsJson: String = "[]",
    val meqScore: Int = 0
)

@Singleton
class UserPreferencesDataStore @Inject constructor(
    private val context: Context,
    private val protectedSlotDao: com.neuroflow.app.data.local.dao.ProtectedSlotDao? = null
) {
    private object Keys {
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val GEMINI_AI_ENABLED = booleanPreferencesKey("gemini_ai_enabled")
        val ADAPTIVE_SYSTEM_DIRECTIVE = stringPreferencesKey("adaptive_system_directive")
        val WEEKLY_AI_AUDIT_JSON = stringPreferencesKey("weekly_ai_audit_json")
        val RECENTLY_DELETED_TASKS_JSON = stringPreferencesKey("recently_deleted_tasks_json")
        val STIMULANT_LOGS_JSON = stringPreferencesKey("stimulant_logs_json")
        val QUICK_PHYSIOLOGICAL_LOGS_JSON = stringPreferencesKey("quick_physiological_logs_json")
        val WAKE_UP_HOUR = intPreferencesKey("wake_up_hour")
        val SLEEP_HOUR = intPreferencesKey("sleep_hour")
        val PEAK_ENERGY_START = intPreferencesKey("peak_energy_start")
        val PEAK_ENERGY_END = intPreferencesKey("peak_energy_end")
        val WORK_DAY_START = intPreferencesKey("work_day_start")
        val WORK_DAY_END = intPreferencesKey("work_day_end")
        val DEFAULT_POMODORO_MINUTES = intPreferencesKey("default_pomodoro_minutes")
        val DEFAULT_BREAK_MINUTES = intPreferencesKey("default_break_minutes")
        val IDENTITY_LABEL = stringPreferencesKey("identity_label")
        val TOP_GOAL = stringPreferencesKey("top_goal")
        val DAILY_STREAK = intPreferencesKey("daily_streak")
        val LAST_ACTIVE_DATE = longPreferencesKey("last_active_date")
        val LONGEST_STREAK = intPreferencesKey("longest_streak")
        val TOTAL_TASKS_COMPLETED = intPreferencesKey("total_tasks_completed")
        val TOTAL_FOCUS_MINUTES = intPreferencesKey("total_focus_minutes")
        val WEIGHT_QUADRANT = floatPreferencesKey("weight_quadrant")
        val WEIGHT_DEADLINE_URGENCY = floatPreferencesKey("weight_deadline_urgency")
        val WEIGHT_PRIORITY_LEVEL = floatPreferencesKey("weight_priority_level")
        val WEIGHT_DURATION = floatPreferencesKey("weight_duration")
        val WEIGHT_IMPACT = floatPreferencesKey("weight_impact")
        val WEIGHT_FOCUS_MODE = floatPreferencesKey("weight_focus_mode")
        val WEIGHT_CIRCADIAN_MATCH = floatPreferencesKey("weight_circadian_match")
        val WEIGHT_FROG_BONUS = floatPreferencesKey("weight_frog_bonus")
        val WEIGHT_PLAN_BOOST = floatPreferencesKey("weight_plan_boost")
        val WEIGHT_SESSION_PROGRESS = floatPreferencesKey("weight_session_progress")
        val WEIGHT_ENJOYMENT = floatPreferencesKey("weight_enjoyment")
        val ENJOYMENT_FORECAST_BIAS = floatPreferencesKey("enjoyment_forecast_bias")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME = stringPreferencesKey("theme")
        val WEEKLY_INTENT = stringPreferencesKey("weekly_intent")
        val WEEKLY_INTENT_ISO_WEEK = intPreferencesKey("weekly_intent_iso_week")
        val WEEKLY_INTENT_ISO_YEAR = intPreferencesKey("weekly_intent_iso_year")
        val LAST_FRESH_START_SHOWN_WEEK = intPreferencesKey("last_fresh_start_shown_week")
        val LAST_FRESH_START_SHOWN_YEAR = intPreferencesKey("last_fresh_start_shown_year")
        val LAST_APP_OPEN_MILLIS = longPreferencesKey("last_app_open_millis")
        val FIRST_APP_OPEN_MILLIS = longPreferencesKey("first_app_open_millis")
        val DETECTED_PEAK_START = intPreferencesKey("detected_peak_start")
        val DETECTED_PEAK_END = intPreferencesKey("detected_peak_end")
        val DETECTED_PEAK_MINUTE_OF_DAY = intPreferencesKey("detected_peak_minute_of_day")
        val PEAK_DETECTION_CONFIDENCE = floatPreferencesKey("peak_detection_confidence")
        val EFFECTIVE_PEAK_START = intPreferencesKey("effective_peak_start")
        val EFFECTIVE_PEAK_END = intPreferencesKey("effective_peak_end")
        val EFFECTIVE_PEAK_MINUTE_OF_DAY = intPreferencesKey("effective_peak_minute_of_day")
        val QUIZ_PEAK_ENABLED = booleanPreferencesKey("quiz_peak_enabled")
        val AFFIRMATIONS = stringPreferencesKey("affirmations")
        val TAG_CATALOG = stringPreferencesKey("tag_catalog")
        val AUTO_SCHEDULING_ENABLED = booleanPreferencesKey("auto_scheduling_enabled")
        val IS_AUTO_SCHEDULE_TOGGLE_ON = booleanPreferencesKey("is_auto_schedule_toggle_on")
        val AUTO_SCHEDULING_HORIZON_DAYS = intPreferencesKey("auto_scheduling_horizon_days")
        val AUTO_SCHEDULING_BREAK_AFTER_COGNITIVE_MINUTES = intPreferencesKey("auto_scheduling_break_after_cognitive_minutes")
        val AUTO_SCHEDULING_BREAK_DURATION_MINUTES = intPreferencesKey("auto_scheduling_break_duration_minutes")
        val AUTO_SCHEDULING_BACKGROUND_THROTTLE_MINUTES = intPreferencesKey("auto_scheduling_background_throttle_minutes")
        val YEARLY_GOALS = stringPreferencesKey("yearly_goals")
        val WEEKLY_GOALS = stringPreferencesKey("weekly_goals")
        val LAST_YEARLY_GOAL_SHOWN_YEAR = intPreferencesKey("last_yearly_goal_shown_year")
        val LAST_WEEKLY_GOAL_SHOWN_WEEK = intPreferencesKey("last_weekly_goal_shown_week")
        val LAST_WEEKLY_GOAL_SHOWN_YEAR = intPreferencesKey("last_weekly_goal_shown_year")
        val LAST_YEARLY_GOAL_HANDLED_YEAR = intPreferencesKey("last_yearly_goal_handled_year")
        val LAST_WEEKLY_GOAL_HANDLED_WEEK = intPreferencesKey("last_weekly_goal_handled_week")
        val WOOP_ENABLED = booleanPreferencesKey("woop_enabled")
        val AUTO_TRACKER_ENABLED = booleanPreferencesKey("auto_tracker_enabled")
        val DAILY_PLAN_NOTIFICATIONS_ENABLED = booleanPreferencesKey("daily_plan_notifications_enabled")
        val STREAK_NOTIFICATIONS_ENABLED = booleanPreferencesKey("streak_notifications_enabled")
        val AUTONOMY_NUDGE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("autonomy_nudge_notifications_enabled")
        val DEADLINE_REMINDER_NOTIFICATIONS_ENABLED = booleanPreferencesKey("deadline_reminder_notifications_enabled")
        val DEADLINE_ESCALATION_NOTIFICATIONS_ENABLED = booleanPreferencesKey("deadline_escalation_notifications_enabled")
        val DAILY_PLAN_NOTIFICATION_HOUR = intPreferencesKey("daily_plan_notification_hour")
        val STREAK_CHECK_NOTIFICATION_HOUR = intPreferencesKey("streak_check_notification_hour")
        val USER_GUIDE_PROMPT_SHOWN = booleanPreferencesKey("user_guide_prompt_shown")
        val LEFT_PAGE_QUICK_NOTE = stringPreferencesKey("left_page_quick_note")
        val MANUAL_CHRONOTYPE = stringPreferencesKey("manual_chronotype")
        val QUIZ_CHRONOTYPE = stringPreferencesKey("quiz_chronotype")
        val QUIZ_PROGRESS = stringPreferencesKey("quiz_progress")
        val MEQ_SCORE = intPreferencesKey("meq_score")
        val SLEEP_PRESSURE_POINTS = intPreferencesKey("sleep_pressure_points")
        val SLEEP_PRESSURE_TRACKING_STARTED_AT = longPreferencesKey("sleep_pressure_tracking_started_at")
        val SLEEP_PRESSURE_LAST_COMPUTED_AT = longPreferencesKey("sleep_pressure_last_computed_at")
        val AUTO_FALLBACK_SLEEP_INSERTION_ENABLED = booleanPreferencesKey("auto_fallback_sleep_insertion_enabled")
        val DISMISSED_SLEEP_FALLBACKS = stringSetPreferencesKey("dismissed_sleep_fallbacks")
        val DAILY_DEEP_WORK_TARGET_MINUTES = intPreferencesKey("daily_deep_work_target_minutes")
        val DISTRACTING_PACKAGES = stringSetPreferencesKey("distracting_packages")
        val MOMENT_INTERRUPTION_SENSITIVITY = floatPreferencesKey("moment_interruption_sensitivity")
        val MOMENT_NOTIFICATION_SENSITIVITY = floatPreferencesKey("moment_notification_sensitivity")
        val MOMENT_TASK_PRESSURE_SENSITIVITY = floatPreferencesKey("moment_task_pressure_sensitivity")
        val ENERGY_TELEMETRY_ENABLED = booleanPreferencesKey("energy_telemetry_enabled")
        val ENERGY_TELEMETRY_RETENTION_DAYS = intPreferencesKey("energy_telemetry_retention_days")
        val MORNING_TUNE_SLEEP_WEIGHT = floatPreferencesKey("morning_tune_sleep_weight")
        val MORNING_TUNE_WAKE_WEIGHT = floatPreferencesKey("morning_tune_wake_weight")
        val MORNING_TUNE_BEHAVIOR_WEIGHT = floatPreferencesKey("morning_tune_behavior_weight")
        val MORNING_TUNE_BASE_WEIGHT = floatPreferencesKey("morning_tune_base_weight")
        val MORNING_TUNE_UPDATED_AT = longPreferencesKey("morning_tune_updated_at")
        val MORNING_TUNE_VERSION = intPreferencesKey("morning_tune_version")
        val ADAPTIVE_PEAK_FREEZE_ENABLED = booleanPreferencesKey("adaptive_peak_freeze_enabled")
        val PEAK_QUALITY_DEGRADE_STREAK = intPreferencesKey("peak_quality_degrade_streak")
        val PEAK_CONFIDENCE_ABSTENTION_ENABLED = booleanPreferencesKey("peak_confidence_abstention_enabled")
        val PEAK_CONFIDENCE_ABSTENTION_REASON = stringPreferencesKey("peak_confidence_abstention_reason")
        val PEAK_CONFIDENCE_ABSTENTION_TRIGGER_COUNT = intPreferencesKey("peak_confidence_abstention_trigger_count")
        val PEAK_CONFIDENCE_ABSTENTION_RECOVERY_COUNT = intPreferencesKey("peak_confidence_abstention_recovery_count")
        val PEAK_CONFIDENCE_ABSTENTION_LAST_CHANGED_AT = longPreferencesKey("peak_confidence_abstention_last_changed_at")
        val PEAK_CONFIDENCE_ABSTENTION_REASON_FREEZE_COUNT = intPreferencesKey("peak_confidence_abstention_reason_freeze_count")
        val PEAK_CONFIDENCE_ABSTENTION_REASON_LOW_SAMPLES_COUNT = intPreferencesKey("peak_confidence_abstention_reason_low_samples_count")
        val PEAK_CONFIDENCE_ABSTENTION_REASON_LOW_COVERAGE_COUNT = intPreferencesKey("peak_confidence_abstention_reason_low_coverage_count")
        val PEAK_CONFIDENCE_ABSTENTION_REASON_WAKE_VARIANCE_COUNT = intPreferencesKey("peak_confidence_abstention_reason_wake_variance_count")
        val PEAK_CONFIDENCE_ABSTENTION_REASON_DIVERGENCE_COUNT = intPreferencesKey("peak_confidence_abstention_reason_divergence_count")
        val PEAK_CONFIDENCE_ABSTENTION_REASON_OTHER_COUNT = intPreferencesKey("peak_confidence_abstention_reason_other_count")
        val MANUAL_PEAK_PROFILE_ENABLED = booleanPreferencesKey("manual_peak_profile_enabled")
        val MANUAL_PEAK_PROFILE_TYPE = stringPreferencesKey("manual_peak_profile_type")
        val MANUAL_PEAK_ANCHOR_MINUTE_OF_DAY = intPreferencesKey("manual_peak_anchor_minute_of_day")
        val MANUAL_PEAK_WINDOW_1_START_OFFSET = intPreferencesKey("manual_peak_window_1_start_offset")
        val MANUAL_PEAK_WINDOW_2_START_OFFSET = intPreferencesKey("manual_peak_window_2_start_offset")
        val MANUAL_PEAK_WINDOW_3_START_OFFSET = intPreferencesKey("manual_peak_window_3_start_offset")
        val MANUAL_PEAK_WINDOW_1_DURATION = intPreferencesKey("manual_peak_window_1_duration")
        val MANUAL_PEAK_WINDOW_2_DURATION = intPreferencesKey("manual_peak_window_2_duration")
        val MANUAL_PEAK_WINDOW_3_DURATION = intPreferencesKey("manual_peak_window_3_duration")
        val MANUAL_PEAK_WINDOW_1_AMPLITUDE = floatPreferencesKey("manual_peak_window_1_amplitude")
        val MANUAL_PEAK_WINDOW_2_AMPLITUDE = floatPreferencesKey("manual_peak_window_2_amplitude")
        val MANUAL_PEAK_WINDOW_3_AMPLITUDE = floatPreferencesKey("manual_peak_window_3_amplitude")
        val PROTECTED_TIME_SLOTS = stringPreferencesKey("protected_time_slots")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val AMOLED_DARK_ENABLED = booleanPreferencesKey("amoled_dark_enabled")
        val DND_ENABLED = booleanPreferencesKey("dnd_enabled")
        val USE_V2_UI = booleanPreferencesKey("use_v2_ui")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            wakeUpHour = prefs[Keys.WAKE_UP_HOUR] ?: 7,
            sleepHour = prefs[Keys.SLEEP_HOUR] ?: 23,
            peakEnergyStart = prefs[Keys.PEAK_ENERGY_START] ?: 9,
            peakEnergyEnd = prefs[Keys.PEAK_ENERGY_END] ?: 12,
            workDayStart = prefs[Keys.WORK_DAY_START] ?: 8,
            workDayEnd = prefs[Keys.WORK_DAY_END] ?: 20,
            defaultPomodoroMinutes = prefs[Keys.DEFAULT_POMODORO_MINUTES] ?: 25,
            defaultBreakMinutes = prefs[Keys.DEFAULT_BREAK_MINUTES] ?: 5,
            identityLabel = prefs[Keys.IDENTITY_LABEL] ?: "",
            topGoal = prefs[Keys.TOP_GOAL] ?: "",
            dailyStreak = prefs[Keys.DAILY_STREAK] ?: 0,
            lastActiveDate = prefs[Keys.LAST_ACTIVE_DATE] ?: 0L,
            longestStreak = prefs[Keys.LONGEST_STREAK] ?: 0,
            totalTasksCompleted = prefs[Keys.TOTAL_TASKS_COMPLETED] ?: 0,
            totalFocusMinutes = prefs[Keys.TOTAL_FOCUS_MINUTES] ?: 0,
            weightQuadrant = prefs[Keys.WEIGHT_QUADRANT] ?: 1.0f,
            weightDeadlineUrgency = prefs[Keys.WEIGHT_DEADLINE_URGENCY] ?: 1.0f,
            weightPriorityLevel = prefs[Keys.WEIGHT_PRIORITY_LEVEL] ?: 1.0f,
            weightDuration = prefs[Keys.WEIGHT_DURATION] ?: 1.0f,
            weightImpact = prefs[Keys.WEIGHT_IMPACT] ?: 1.0f,
            weightFocusMode = prefs[Keys.WEIGHT_FOCUS_MODE] ?: 1.0f,
            weightCircadianMatch = prefs[Keys.WEIGHT_CIRCADIAN_MATCH] ?: 1.0f,
            weightFrogBonus = prefs[Keys.WEIGHT_FROG_BONUS] ?: 1.0f,
            weightPlanBoost = prefs[Keys.WEIGHT_PLAN_BOOST] ?: 1.0f,
            weightSessionProgress = prefs[Keys.WEIGHT_SESSION_PROGRESS] ?: 1.0f,
            weightEnjoyment = prefs[Keys.WEIGHT_ENJOYMENT] ?: 1.0f,
            enjoymentForecastBias = prefs[Keys.ENJOYMENT_FORECAST_BIAS] ?: 0f,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            theme = try {
                AppTheme.valueOf(prefs[Keys.THEME] ?: AppTheme.SYSTEM.name)
            } catch (_: Exception) { AppTheme.SYSTEM },
            weeklyIntent = prefs[Keys.WEEKLY_INTENT] ?: "",
            weeklyIntentIsoWeek = prefs[Keys.WEEKLY_INTENT_ISO_WEEK] ?: 0,
            weeklyIntentIsoYear = prefs[Keys.WEEKLY_INTENT_ISO_YEAR] ?: 0,
            lastFreshStartShownWeek = prefs[Keys.LAST_FRESH_START_SHOWN_WEEK] ?: 0,
            lastFreshStartShownYear = prefs[Keys.LAST_FRESH_START_SHOWN_YEAR] ?: 0,
            lastAppOpenMillis = prefs[Keys.LAST_APP_OPEN_MILLIS] ?: 0L,
            firstAppOpenMillis = prefs[Keys.FIRST_APP_OPEN_MILLIS] ?: 0L,
            detectedPeakStart = prefs[Keys.DETECTED_PEAK_START] ?: -1,
            detectedPeakEnd = prefs[Keys.DETECTED_PEAK_END] ?: -1,
            detectedPeakMinuteOfDay = prefs[Keys.DETECTED_PEAK_MINUTE_OF_DAY] ?: -1,
            peakDetectionConfidence = prefs[Keys.PEAK_DETECTION_CONFIDENCE] ?: 0f,
            effectivePeakStart = prefs[Keys.EFFECTIVE_PEAK_START] ?: -1,
            effectivePeakEnd = prefs[Keys.EFFECTIVE_PEAK_END] ?: -1,
            effectivePeakMinuteOfDay = prefs[Keys.EFFECTIVE_PEAK_MINUTE_OF_DAY] ?: -1,
            quizPeakEnabled = prefs[Keys.QUIZ_PEAK_ENABLED] ?: true,
            affirmations = parseAffirmations(prefs[Keys.AFFIRMATIONS]),
            tagCatalog = withStarterTagCatalog(parseAffirmations(prefs[Keys.TAG_CATALOG])),
            autoSchedulingEnabled = prefs[Keys.AUTO_SCHEDULING_ENABLED] ?: true,
            isAutoScheduleToggleOn = prefs[Keys.IS_AUTO_SCHEDULE_TOGGLE_ON] ?: false,
            autoSchedulingHorizonDays = (prefs[Keys.AUTO_SCHEDULING_HORIZON_DAYS] ?: 3).coerceIn(1, 7),
            autoSchedulingBreakAfterCognitiveMinutes = (prefs[Keys.AUTO_SCHEDULING_BREAK_AFTER_COGNITIVE_MINUTES] ?: 90).coerceIn(30, 180),
            autoSchedulingBreakDurationMinutes = (prefs[Keys.AUTO_SCHEDULING_BREAK_DURATION_MINUTES] ?: 15).coerceIn(5, 30),
            autoSchedulingBackgroundThrottleMinutes = (prefs[Keys.AUTO_SCHEDULING_BACKGROUND_THROTTLE_MINUTES] ?: 30).coerceIn(15, 240),
            yearlyGoals = parseAffirmations(prefs[Keys.YEARLY_GOALS]),
            weeklyGoals = parseAffirmations(prefs[Keys.WEEKLY_GOALS]),
            lastYearlyGoalShownYear = prefs[Keys.LAST_YEARLY_GOAL_SHOWN_YEAR] ?: 0,
            lastWeeklyGoalShownWeek = prefs[Keys.LAST_WEEKLY_GOAL_SHOWN_WEEK] ?: 0,
            lastWeeklyGoalShownYear = prefs[Keys.LAST_WEEKLY_GOAL_SHOWN_YEAR] ?: 0,
            lastYearlyGoalHandledYear = prefs[Keys.LAST_YEARLY_GOAL_HANDLED_YEAR] ?: 0,
            lastWeeklyGoalHandledWeek = prefs[Keys.LAST_WEEKLY_GOAL_HANDLED_WEEK] ?: 0,
            woopEnabled = prefs[Keys.WOOP_ENABLED] ?: true,
            autoTrackerEnabled = prefs[Keys.AUTO_TRACKER_ENABLED] ?: false,
            dailyPlanNotificationsEnabled = prefs[Keys.DAILY_PLAN_NOTIFICATIONS_ENABLED] ?: true,
            streakNotificationsEnabled = prefs[Keys.STREAK_NOTIFICATIONS_ENABLED] ?: true,
            autonomyNudgeNotificationsEnabled = prefs[Keys.AUTONOMY_NUDGE_NOTIFICATIONS_ENABLED] ?: true,
            deadlineReminderNotificationsEnabled = prefs[Keys.DEADLINE_REMINDER_NOTIFICATIONS_ENABLED] ?: true,
            deadlineEscalationNotificationsEnabled = prefs[Keys.DEADLINE_ESCALATION_NOTIFICATIONS_ENABLED] ?: true,
            dailyPlanNotificationHour = prefs[Keys.DAILY_PLAN_NOTIFICATION_HOUR] ?: 7,
            streakCheckNotificationHour = prefs[Keys.STREAK_CHECK_NOTIFICATION_HOUR] ?: 21,
            userGuidePromptShown = prefs[Keys.USER_GUIDE_PROMPT_SHOWN] ?: false,
            leftPageQuickNote = prefs[Keys.LEFT_PAGE_QUICK_NOTE] ?: "",
            manualChronotype = prefs[Keys.MANUAL_CHRONOTYPE],
            quizChronotype = prefs[Keys.QUIZ_CHRONOTYPE],
            quizProgress = prefs[Keys.QUIZ_PROGRESS] ?: "{}",
            meqScore = prefs[Keys.MEQ_SCORE] ?: 0,
            sleepPressurePoints = prefs[Keys.SLEEP_PRESSURE_POINTS] ?: 0,
            sleepPressureTrackingStartedAtMillis = prefs[Keys.SLEEP_PRESSURE_TRACKING_STARTED_AT] ?: 0L,
            sleepPressureLastComputedAtMillis = prefs[Keys.SLEEP_PRESSURE_LAST_COMPUTED_AT] ?: 0L,
            autoFallbackSleepInsertionEnabled = prefs[Keys.AUTO_FALLBACK_SLEEP_INSERTION_ENABLED] ?: true,
            dismissedSleepFallbacks = prefs[Keys.DISMISSED_SLEEP_FALLBACKS] ?: emptySet(),
            dailyDeepWorkTargetMinutes = (prefs[Keys.DAILY_DEEP_WORK_TARGET_MINUTES] ?: 240).coerceIn(30, 720),
            distractingPackages = prefs[Keys.DISTRACTING_PACKAGES] ?: BehavioralSupportService.DEFAULT_DISTRACTING_PACKAGES,
            momentInterruptionSensitivity = (prefs[Keys.MOMENT_INTERRUPTION_SENSITIVITY] ?: 1.0f).coerceIn(0.5f, 2.0f),
            momentNotificationSensitivity = (prefs[Keys.MOMENT_NOTIFICATION_SENSITIVITY] ?: 1.0f).coerceIn(0.5f, 2.0f),
            momentTaskPressureSensitivity = (prefs[Keys.MOMENT_TASK_PRESSURE_SENSITIVITY] ?: 1.0f).coerceIn(0.5f, 2.0f),
            energyTelemetryEnabled = prefs[Keys.ENERGY_TELEMETRY_ENABLED] ?: true,
            energyTelemetryRetentionDays = (prefs[Keys.ENERGY_TELEMETRY_RETENTION_DAYS] ?: 30).coerceIn(1, 90),
            morningTuneSleepWeight = prefs[Keys.MORNING_TUNE_SLEEP_WEIGHT] ?: 0.30f,
            morningTuneWakeWeight = prefs[Keys.MORNING_TUNE_WAKE_WEIGHT] ?: 0.25f,
            morningTuneBehaviorWeight = prefs[Keys.MORNING_TUNE_BEHAVIOR_WEIGHT] ?: 0.25f,
            morningTuneBaseWeight = prefs[Keys.MORNING_TUNE_BASE_WEIGHT] ?: 0.20f,
            morningTuneUpdatedAtMillis = prefs[Keys.MORNING_TUNE_UPDATED_AT] ?: 0L,
            morningTuneVersion = prefs[Keys.MORNING_TUNE_VERSION] ?: 1,
            adaptivePeakFreezeEnabled = prefs[Keys.ADAPTIVE_PEAK_FREEZE_ENABLED] ?: false,
            peakQualityDegradeStreak = prefs[Keys.PEAK_QUALITY_DEGRADE_STREAK] ?: 0,
            peakConfidenceAbstentionEnabled = prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_ENABLED] ?: false,
            peakConfidenceAbstentionReason = prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON] ?: "",
            peakConfidenceAbstentionTriggerCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_TRIGGER_COUNT] ?: 0).coerceAtLeast(0),
            peakConfidenceAbstentionRecoveryCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_RECOVERY_COUNT] ?: 0).coerceAtLeast(0),
            peakConfidenceAbstentionLastChangedAtMillis = prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_LAST_CHANGED_AT] ?: 0L,
            peakConfidenceAbstentionReasonFreezeCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_FREEZE_COUNT] ?: 0).coerceAtLeast(0),
            peakConfidenceAbstentionReasonLowSamplesCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_LOW_SAMPLES_COUNT] ?: 0).coerceAtLeast(0),
            peakConfidenceAbstentionReasonLowCoverageCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_LOW_COVERAGE_COUNT] ?: 0).coerceAtLeast(0),
            peakConfidenceAbstentionReasonWakeVarianceCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_WAKE_VARIANCE_COUNT] ?: 0).coerceAtLeast(0),
            peakConfidenceAbstentionReasonDivergenceCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_DIVERGENCE_COUNT] ?: 0).coerceAtLeast(0),
            peakConfidenceAbstentionReasonOtherCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_OTHER_COUNT] ?: 0).coerceAtLeast(0),
            manualPeakProfileEnabled = prefs[Keys.MANUAL_PEAK_PROFILE_ENABLED] ?: false,
            manualPeakProfileType = prefs[Keys.MANUAL_PEAK_PROFILE_TYPE] ?: "AUTO",
            manualPeakAnchorMinuteOfDay = prefs[Keys.MANUAL_PEAK_ANCHOR_MINUTE_OF_DAY] ?: 360,
            manualPeakWindow1StartOffsetMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_1_START_OFFSET] ?: 0,
            manualPeakWindow2StartOffsetMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_2_START_OFFSET] ?: 570,
            manualPeakWindow3StartOffsetMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_3_START_OFFSET] ?: 810,
            manualPeakWindow1DurationMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_1_DURATION] ?: 210,
            manualPeakWindow2DurationMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_2_DURATION] ?: 150,
            manualPeakWindow3DurationMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_3_DURATION] ?: 60,
            manualPeakWindow1Amplitude = prefs[Keys.MANUAL_PEAK_WINDOW_1_AMPLITUDE] ?: 1.0f,
            manualPeakWindow2Amplitude = prefs[Keys.MANUAL_PEAK_WINDOW_2_AMPLITUDE] ?: 0.8f,
            manualPeakWindow3Amplitude = prefs[Keys.MANUAL_PEAK_WINDOW_3_AMPLITUDE] ?: 0.6f,
            protectedTimeSlots = parseProtectedTimeSlots(prefs[Keys.PROTECTED_TIME_SLOTS]),
            dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR_ENABLED] ?: true,
            amoledDarkEnabled = prefs[Keys.AMOLED_DARK_ENABLED] ?: false,
            dndEnabled = prefs[Keys.DND_ENABLED] ?: true,
            useV2Ui = prefs[Keys.USE_V2_UI] ?: true,
            geminiApiKey = prefs[Keys.GEMINI_API_KEY] ?: "",
            geminiAiEnabled = prefs[Keys.GEMINI_AI_ENABLED] ?: true,
            adaptiveSystemDirective = prefs[Keys.ADAPTIVE_SYSTEM_DIRECTIVE] ?: "",
            weeklyAiAuditJson = prefs[Keys.WEEKLY_AI_AUDIT_JSON] ?: "",
            recentlyDeletedTasksJson = prefs[Keys.RECENTLY_DELETED_TASKS_JSON] ?: "[]",
            stimulantLogsJson = prefs[Keys.STIMULANT_LOGS_JSON] ?: "[]",
            quickPhysiologicalLogsJson = prefs[Keys.QUICK_PHYSIOLOGICAL_LOGS_JSON] ?: "[]"
        )
    }

    companion object {
        private val jsonStringListCache = java.util.concurrent.ConcurrentHashMap<String, List<String>>()
        private val protectedSlotsCache = java.util.concurrent.ConcurrentHashMap<String, List<ProtectedTimeSlot>>()

        internal fun parseAffirmations(json: String?): List<String> {
            if (json.isNullOrBlank()) return emptyList()
            return jsonStringListCache.computeIfAbsent(json) { raw ->
                try {
                    val arr = JSONArray(raw)
                    List(arr.length()) { arr.getString(it) }
                } catch (_: Exception) { emptyList() }
            }
        }

        internal fun encodeAffirmations(list: List<String>): String {
            val arr = JSONArray()
            list.forEach { arr.put(it) }
            return arr.toString()
        }

        internal fun parseProtectedTimeSlots(json: String?): List<ProtectedTimeSlot> {
            if (json.isNullOrBlank()) return emptyList()
            return protectedSlotsCache.computeIfAbsent(json) { raw ->
                try {
                    val arr = JSONArray(raw)
                    val list = mutableListOf<ProtectedTimeSlot>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            ProtectedTimeSlot(
                                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                                label = obj.optString("label", "Protected Block"),
                                startMinuteOfDay = obj.optInt("startMinuteOfDay", 480).coerceIn(0, 1439),
                                durationMinutes = obj.optInt("durationMinutes", 60).coerceIn(15, 360),
                                recurrence = runCatching { PtsRecurrence.valueOf(obj.optString("recurrence", "DAILY")) }.getOrDefault(PtsRecurrence.DAILY),
                                weekdayBitmask = obj.optInt("weekdayBitmask", 0b1111111),
                                category = runCatching { PtsCategory.valueOf(obj.optString("category", "BREAK")) }.getOrDefault(PtsCategory.BREAK),
                                recoveryBufferMinutes = obj.optInt("recoveryBufferMinutes", 0).coerceIn(0, 120),
                                isEnabled = obj.optBoolean("isEnabled", true),
                                dateMillis = if (obj.has("dateMillis") && !obj.isNull("dateMillis")) obj.optLong("dateMillis") else null
                            )
                        )
                    }
                    list
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }

        internal fun encodeProtectedTimeSlots(list: List<ProtectedTimeSlot>): String {
            val arr = JSONArray()
            list.forEach { slot ->
                val obj = JSONObject().apply {
                    put("id", slot.id)
                    put("label", slot.label)
                    put("startMinuteOfDay", slot.startMinuteOfDay)
                    put("durationMinutes", slot.durationMinutes)
                    put("recurrence", slot.recurrence.name)
                    put("weekdayBitmask", slot.weekdayBitmask)
                    put("category", slot.category.name)
                    put("recoveryBufferMinutes", slot.recoveryBufferMinutes)
                    put("isEnabled", slot.isEnabled)
                    if (slot.dateMillis != null) {
                        put("dateMillis", slot.dateMillis)
                    }
                }
                arr.put(obj)
            }
            return arr.toString()
        }
    }

    suspend fun updatePreferences(update: (UserPreferences) -> UserPreferences) {
        context.dataStore.edit { prefs ->
            val current = UserPreferences(
                wakeUpHour = prefs[Keys.WAKE_UP_HOUR] ?: 7,
                sleepHour = prefs[Keys.SLEEP_HOUR] ?: 23,
                peakEnergyStart = prefs[Keys.PEAK_ENERGY_START] ?: 9,
                peakEnergyEnd = prefs[Keys.PEAK_ENERGY_END] ?: 12,
                workDayStart = prefs[Keys.WORK_DAY_START] ?: 8,
                workDayEnd = prefs[Keys.WORK_DAY_END] ?: 20,
                defaultPomodoroMinutes = prefs[Keys.DEFAULT_POMODORO_MINUTES] ?: 25,
                defaultBreakMinutes = prefs[Keys.DEFAULT_BREAK_MINUTES] ?: 5,
                identityLabel = prefs[Keys.IDENTITY_LABEL] ?: "",
                topGoal = prefs[Keys.TOP_GOAL] ?: "",
                dailyStreak = prefs[Keys.DAILY_STREAK] ?: 0,
                lastActiveDate = prefs[Keys.LAST_ACTIVE_DATE] ?: 0L,
                longestStreak = prefs[Keys.LONGEST_STREAK] ?: 0,
                totalTasksCompleted = prefs[Keys.TOTAL_TASKS_COMPLETED] ?: 0,
                totalFocusMinutes = prefs[Keys.TOTAL_FOCUS_MINUTES] ?: 0,
                weightQuadrant = prefs[Keys.WEIGHT_QUADRANT] ?: 1.0f,
                weightDeadlineUrgency = prefs[Keys.WEIGHT_DEADLINE_URGENCY] ?: 1.0f,
                weightPriorityLevel = prefs[Keys.WEIGHT_PRIORITY_LEVEL] ?: 1.0f,
                weightDuration = prefs[Keys.WEIGHT_DURATION] ?: 1.0f,
                weightImpact = prefs[Keys.WEIGHT_IMPACT] ?: 1.0f,
                weightFocusMode = prefs[Keys.WEIGHT_FOCUS_MODE] ?: 1.0f,
                weightCircadianMatch = prefs[Keys.WEIGHT_CIRCADIAN_MATCH] ?: 1.0f,
                weightFrogBonus = prefs[Keys.WEIGHT_FROG_BONUS] ?: 1.0f,
                weightPlanBoost = prefs[Keys.WEIGHT_PLAN_BOOST] ?: 1.0f,
                weightSessionProgress = prefs[Keys.WEIGHT_SESSION_PROGRESS] ?: 1.0f,
                weightEnjoyment = prefs[Keys.WEIGHT_ENJOYMENT] ?: 1.0f,
            enjoymentForecastBias = prefs[Keys.ENJOYMENT_FORECAST_BIAS] ?: 0f,
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                theme = try {
                    AppTheme.valueOf(prefs[Keys.THEME] ?: AppTheme.SYSTEM.name)
                } catch (_: Exception) { AppTheme.SYSTEM },
                weeklyIntent = prefs[Keys.WEEKLY_INTENT] ?: "",
                weeklyIntentIsoWeek = prefs[Keys.WEEKLY_INTENT_ISO_WEEK] ?: 0,
                weeklyIntentIsoYear = prefs[Keys.WEEKLY_INTENT_ISO_YEAR] ?: 0,
                lastFreshStartShownWeek = prefs[Keys.LAST_FRESH_START_SHOWN_WEEK] ?: 0,
                lastFreshStartShownYear = prefs[Keys.LAST_FRESH_START_SHOWN_YEAR] ?: 0,
                lastAppOpenMillis = prefs[Keys.LAST_APP_OPEN_MILLIS] ?: 0L,
                firstAppOpenMillis = prefs[Keys.FIRST_APP_OPEN_MILLIS] ?: 0L,
                detectedPeakStart = prefs[Keys.DETECTED_PEAK_START] ?: -1,
                detectedPeakEnd = prefs[Keys.DETECTED_PEAK_END] ?: -1,
                detectedPeakMinuteOfDay = prefs[Keys.DETECTED_PEAK_MINUTE_OF_DAY] ?: -1,
                peakDetectionConfidence = prefs[Keys.PEAK_DETECTION_CONFIDENCE] ?: 0f,
                effectivePeakStart = prefs[Keys.EFFECTIVE_PEAK_START] ?: -1,
                effectivePeakEnd = prefs[Keys.EFFECTIVE_PEAK_END] ?: -1,
                effectivePeakMinuteOfDay = prefs[Keys.EFFECTIVE_PEAK_MINUTE_OF_DAY] ?: -1,
                quizPeakEnabled = prefs[Keys.QUIZ_PEAK_ENABLED] ?: true,
                affirmations = parseAffirmations(prefs[Keys.AFFIRMATIONS]),
                tagCatalog = withStarterTagCatalog(parseAffirmations(prefs[Keys.TAG_CATALOG])),
                autoSchedulingEnabled = prefs[Keys.AUTO_SCHEDULING_ENABLED] ?: true,
                isAutoScheduleToggleOn = prefs[Keys.IS_AUTO_SCHEDULE_TOGGLE_ON] ?: false,
                autoSchedulingHorizonDays = (prefs[Keys.AUTO_SCHEDULING_HORIZON_DAYS] ?: 3).coerceIn(1, 7),
                autoSchedulingBreakAfterCognitiveMinutes = (prefs[Keys.AUTO_SCHEDULING_BREAK_AFTER_COGNITIVE_MINUTES] ?: 90).coerceIn(30, 180),
                autoSchedulingBreakDurationMinutes = (prefs[Keys.AUTO_SCHEDULING_BREAK_DURATION_MINUTES] ?: 15).coerceIn(5, 30),
                autoSchedulingBackgroundThrottleMinutes = (prefs[Keys.AUTO_SCHEDULING_BACKGROUND_THROTTLE_MINUTES] ?: 30).coerceIn(15, 240),
                yearlyGoals = parseAffirmations(prefs[Keys.YEARLY_GOALS]),
                weeklyGoals = parseAffirmations(prefs[Keys.WEEKLY_GOALS]),
                lastYearlyGoalShownYear = prefs[Keys.LAST_YEARLY_GOAL_SHOWN_YEAR] ?: 0,
                lastWeeklyGoalShownWeek = prefs[Keys.LAST_WEEKLY_GOAL_SHOWN_WEEK] ?: 0,
                lastWeeklyGoalShownYear = prefs[Keys.LAST_WEEKLY_GOAL_SHOWN_YEAR] ?: 0,
                lastYearlyGoalHandledYear = prefs[Keys.LAST_YEARLY_GOAL_HANDLED_YEAR] ?: 0,
                lastWeeklyGoalHandledWeek = prefs[Keys.LAST_WEEKLY_GOAL_HANDLED_WEEK] ?: 0,
                woopEnabled = prefs[Keys.WOOP_ENABLED] ?: true,
                autoTrackerEnabled = prefs[Keys.AUTO_TRACKER_ENABLED] ?: false,
                dailyPlanNotificationsEnabled = prefs[Keys.DAILY_PLAN_NOTIFICATIONS_ENABLED] ?: true,
                streakNotificationsEnabled = prefs[Keys.STREAK_NOTIFICATIONS_ENABLED] ?: true,
                autonomyNudgeNotificationsEnabled = prefs[Keys.AUTONOMY_NUDGE_NOTIFICATIONS_ENABLED] ?: true,
                deadlineReminderNotificationsEnabled = prefs[Keys.DEADLINE_REMINDER_NOTIFICATIONS_ENABLED] ?: true,
                deadlineEscalationNotificationsEnabled = prefs[Keys.DEADLINE_ESCALATION_NOTIFICATIONS_ENABLED] ?: true,
                dailyPlanNotificationHour = prefs[Keys.DAILY_PLAN_NOTIFICATION_HOUR] ?: 7,
                streakCheckNotificationHour = prefs[Keys.STREAK_CHECK_NOTIFICATION_HOUR] ?: 21,
                userGuidePromptShown = prefs[Keys.USER_GUIDE_PROMPT_SHOWN] ?: false,
                leftPageQuickNote = prefs[Keys.LEFT_PAGE_QUICK_NOTE] ?: "",
                manualChronotype = prefs[Keys.MANUAL_CHRONOTYPE],
                quizChronotype = prefs[Keys.QUIZ_CHRONOTYPE],
                quizProgress = prefs[Keys.QUIZ_PROGRESS] ?: "{}",
                meqScore = prefs[Keys.MEQ_SCORE] ?: 0,
                sleepPressurePoints = prefs[Keys.SLEEP_PRESSURE_POINTS] ?: 0,
                sleepPressureTrackingStartedAtMillis = prefs[Keys.SLEEP_PRESSURE_TRACKING_STARTED_AT] ?: 0L,
                sleepPressureLastComputedAtMillis = prefs[Keys.SLEEP_PRESSURE_LAST_COMPUTED_AT] ?: 0L,
                autoFallbackSleepInsertionEnabled = prefs[Keys.AUTO_FALLBACK_SLEEP_INSERTION_ENABLED] ?: true,
                dismissedSleepFallbacks = prefs[Keys.DISMISSED_SLEEP_FALLBACKS] ?: emptySet(),
                dailyDeepWorkTargetMinutes = (prefs[Keys.DAILY_DEEP_WORK_TARGET_MINUTES] ?: 240).coerceIn(30, 720),
                distractingPackages = prefs[Keys.DISTRACTING_PACKAGES] ?: BehavioralSupportService.DEFAULT_DISTRACTING_PACKAGES,
                momentInterruptionSensitivity = (prefs[Keys.MOMENT_INTERRUPTION_SENSITIVITY] ?: 1.0f).coerceIn(0.5f, 2.0f),
                momentNotificationSensitivity = (prefs[Keys.MOMENT_NOTIFICATION_SENSITIVITY] ?: 1.0f).coerceIn(0.5f, 2.0f),
                momentTaskPressureSensitivity = (prefs[Keys.MOMENT_TASK_PRESSURE_SENSITIVITY] ?: 1.0f).coerceIn(0.5f, 2.0f),
                energyTelemetryEnabled = prefs[Keys.ENERGY_TELEMETRY_ENABLED] ?: true,
                energyTelemetryRetentionDays = (prefs[Keys.ENERGY_TELEMETRY_RETENTION_DAYS] ?: 30).coerceIn(1, 90),
                morningTuneSleepWeight = prefs[Keys.MORNING_TUNE_SLEEP_WEIGHT] ?: 0.30f,
                morningTuneWakeWeight = prefs[Keys.MORNING_TUNE_WAKE_WEIGHT] ?: 0.25f,
                morningTuneBehaviorWeight = prefs[Keys.MORNING_TUNE_BEHAVIOR_WEIGHT] ?: 0.25f,
                morningTuneBaseWeight = prefs[Keys.MORNING_TUNE_BASE_WEIGHT] ?: 0.20f,
                morningTuneUpdatedAtMillis = prefs[Keys.MORNING_TUNE_UPDATED_AT] ?: 0L,
                morningTuneVersion = prefs[Keys.MORNING_TUNE_VERSION] ?: 1,
                adaptivePeakFreezeEnabled = prefs[Keys.ADAPTIVE_PEAK_FREEZE_ENABLED] ?: false,
                peakQualityDegradeStreak = prefs[Keys.PEAK_QUALITY_DEGRADE_STREAK] ?: 0,
                peakConfidenceAbstentionEnabled = prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_ENABLED] ?: false,
                peakConfidenceAbstentionReason = prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON] ?: "",
                peakConfidenceAbstentionTriggerCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_TRIGGER_COUNT] ?: 0).coerceAtLeast(0),
                peakConfidenceAbstentionRecoveryCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_RECOVERY_COUNT] ?: 0).coerceAtLeast(0),
                peakConfidenceAbstentionLastChangedAtMillis = prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_LAST_CHANGED_AT] ?: 0L,
                peakConfidenceAbstentionReasonFreezeCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_FREEZE_COUNT] ?: 0).coerceAtLeast(0),
                peakConfidenceAbstentionReasonLowSamplesCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_LOW_SAMPLES_COUNT] ?: 0).coerceAtLeast(0),
                peakConfidenceAbstentionReasonLowCoverageCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_LOW_COVERAGE_COUNT] ?: 0).coerceAtLeast(0),
                peakConfidenceAbstentionReasonWakeVarianceCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_WAKE_VARIANCE_COUNT] ?: 0).coerceAtLeast(0),
                peakConfidenceAbstentionReasonDivergenceCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_DIVERGENCE_COUNT] ?: 0).coerceAtLeast(0),
                peakConfidenceAbstentionReasonOtherCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_OTHER_COUNT] ?: 0).coerceAtLeast(0),
                manualPeakProfileEnabled = prefs[Keys.MANUAL_PEAK_PROFILE_ENABLED] ?: false,
                manualPeakProfileType = prefs[Keys.MANUAL_PEAK_PROFILE_TYPE] ?: "AUTO",
                manualPeakAnchorMinuteOfDay = prefs[Keys.MANUAL_PEAK_ANCHOR_MINUTE_OF_DAY] ?: 360,
                manualPeakWindow1StartOffsetMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_1_START_OFFSET] ?: 0,
                manualPeakWindow2StartOffsetMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_2_START_OFFSET] ?: 570,
                manualPeakWindow3StartOffsetMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_3_START_OFFSET] ?: 810,
                manualPeakWindow1DurationMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_1_DURATION] ?: 210,
                manualPeakWindow2DurationMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_2_DURATION] ?: 150,
                manualPeakWindow3DurationMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_3_DURATION] ?: 60,
                manualPeakWindow1Amplitude = prefs[Keys.MANUAL_PEAK_WINDOW_1_AMPLITUDE] ?: 1.0f,
                manualPeakWindow2Amplitude = prefs[Keys.MANUAL_PEAK_WINDOW_2_AMPLITUDE] ?: 0.8f,
                manualPeakWindow3Amplitude = prefs[Keys.MANUAL_PEAK_WINDOW_3_AMPLITUDE] ?: 0.6f,
                protectedTimeSlots = parseProtectedTimeSlots(prefs[Keys.PROTECTED_TIME_SLOTS]),
                dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR_ENABLED] ?: true,
                amoledDarkEnabled = prefs[Keys.AMOLED_DARK_ENABLED] ?: false,
                dndEnabled = prefs[Keys.DND_ENABLED] ?: true,
                useV2Ui = prefs[Keys.USE_V2_UI] ?: true,
                geminiApiKey = prefs[Keys.GEMINI_API_KEY] ?: "",
                geminiAiEnabled = prefs[Keys.GEMINI_AI_ENABLED] ?: true,
                adaptiveSystemDirective = prefs[Keys.ADAPTIVE_SYSTEM_DIRECTIVE] ?: "",
                weeklyAiAuditJson = prefs[Keys.WEEKLY_AI_AUDIT_JSON] ?: "",
                recentlyDeletedTasksJson = prefs[Keys.RECENTLY_DELETED_TASKS_JSON] ?: "[]",
                stimulantLogsJson = prefs[Keys.STIMULANT_LOGS_JSON] ?: "[]",
                quickPhysiologicalLogsJson = prefs[Keys.QUICK_PHYSIOLOGICAL_LOGS_JSON] ?: "[]"
            )
            val updated = update(current)
            prefs[Keys.WAKE_UP_HOUR] = updated.wakeUpHour
            prefs[Keys.SLEEP_HOUR] = (if (updated.sleepHour >= 24) updated.sleepHour % 24 else updated.sleepHour).coerceIn(0, 23)
            prefs[Keys.PEAK_ENERGY_START] = updated.peakEnergyStart
            prefs[Keys.PEAK_ENERGY_END] = updated.peakEnergyEnd
            prefs[Keys.WORK_DAY_START] = updated.workDayStart
            prefs[Keys.WORK_DAY_END] = updated.workDayEnd
            prefs[Keys.DEFAULT_POMODORO_MINUTES] = updated.defaultPomodoroMinutes
            prefs[Keys.DEFAULT_BREAK_MINUTES] = updated.defaultBreakMinutes
            prefs[Keys.IDENTITY_LABEL] = updated.identityLabel
            prefs[Keys.TOP_GOAL] = updated.topGoal
            prefs[Keys.DAILY_STREAK] = updated.dailyStreak
            prefs[Keys.LAST_ACTIVE_DATE] = updated.lastActiveDate
            prefs[Keys.LONGEST_STREAK] = updated.longestStreak
            prefs[Keys.TOTAL_TASKS_COMPLETED] = updated.totalTasksCompleted
            prefs[Keys.TOTAL_FOCUS_MINUTES] = updated.totalFocusMinutes
            prefs[Keys.WEIGHT_QUADRANT] = updated.weightQuadrant
            prefs[Keys.WEIGHT_DEADLINE_URGENCY] = updated.weightDeadlineUrgency
            prefs[Keys.WEIGHT_PRIORITY_LEVEL] = updated.weightPriorityLevel
            prefs[Keys.WEIGHT_DURATION] = updated.weightDuration
            prefs[Keys.WEIGHT_IMPACT] = updated.weightImpact
            prefs[Keys.WEIGHT_FOCUS_MODE] = updated.weightFocusMode
            prefs[Keys.WEIGHT_CIRCADIAN_MATCH] = updated.weightCircadianMatch
            prefs[Keys.WEIGHT_FROG_BONUS] = updated.weightFrogBonus
            prefs[Keys.WEIGHT_PLAN_BOOST] = updated.weightPlanBoost
            prefs[Keys.WEIGHT_SESSION_PROGRESS] = updated.weightSessionProgress
            prefs[Keys.WEIGHT_ENJOYMENT] = updated.weightEnjoyment
            prefs[Keys.ENJOYMENT_FORECAST_BIAS] = updated.enjoymentForecastBias
            prefs[Keys.ONBOARDING_COMPLETED] = updated.onboardingCompleted
            prefs[Keys.THEME] = updated.theme.name
            prefs[Keys.WEEKLY_INTENT] = updated.weeklyIntent
            prefs[Keys.WEEKLY_INTENT_ISO_WEEK] = updated.weeklyIntentIsoWeek
            prefs[Keys.WEEKLY_INTENT_ISO_YEAR] = updated.weeklyIntentIsoYear
            prefs[Keys.LAST_FRESH_START_SHOWN_WEEK] = updated.lastFreshStartShownWeek
            prefs[Keys.LAST_FRESH_START_SHOWN_YEAR] = updated.lastFreshStartShownYear
            prefs[Keys.LAST_APP_OPEN_MILLIS] = updated.lastAppOpenMillis
            prefs[Keys.FIRST_APP_OPEN_MILLIS] = updated.firstAppOpenMillis
            prefs[Keys.DETECTED_PEAK_START] = updated.detectedPeakStart
            prefs[Keys.DETECTED_PEAK_END] = updated.detectedPeakEnd
            prefs[Keys.DETECTED_PEAK_MINUTE_OF_DAY] = updated.detectedPeakMinuteOfDay
            prefs[Keys.PEAK_DETECTION_CONFIDENCE] = updated.peakDetectionConfidence
            prefs[Keys.EFFECTIVE_PEAK_START] = updated.effectivePeakStart
            prefs[Keys.EFFECTIVE_PEAK_END] = updated.effectivePeakEnd
            prefs[Keys.EFFECTIVE_PEAK_MINUTE_OF_DAY] = updated.effectivePeakMinuteOfDay
            prefs[Keys.QUIZ_PEAK_ENABLED] = updated.quizPeakEnabled
            prefs[Keys.AFFIRMATIONS] = encodeAffirmations(updated.affirmations)
            prefs[Keys.TAG_CATALOG] = encodeAffirmations(withStarterTagCatalog(updated.tagCatalog))
            prefs[Keys.AUTO_SCHEDULING_ENABLED] = updated.autoSchedulingEnabled
            prefs[Keys.IS_AUTO_SCHEDULE_TOGGLE_ON] = updated.isAutoScheduleToggleOn
            prefs[Keys.AUTO_SCHEDULING_HORIZON_DAYS] = updated.autoSchedulingHorizonDays.coerceIn(1, 7)
            prefs[Keys.AUTO_SCHEDULING_BREAK_AFTER_COGNITIVE_MINUTES] = updated.autoSchedulingBreakAfterCognitiveMinutes.coerceIn(30, 180)
            prefs[Keys.AUTO_SCHEDULING_BREAK_DURATION_MINUTES] = updated.autoSchedulingBreakDurationMinutes.coerceIn(5, 30)
            prefs[Keys.AUTO_SCHEDULING_BACKGROUND_THROTTLE_MINUTES] = updated.autoSchedulingBackgroundThrottleMinutes.coerceIn(15, 240)
            prefs[Keys.YEARLY_GOALS] = encodeAffirmations(updated.yearlyGoals)
            prefs[Keys.WEEKLY_GOALS] = encodeAffirmations(updated.weeklyGoals)
            prefs[Keys.LAST_YEARLY_GOAL_SHOWN_YEAR] = updated.lastYearlyGoalShownYear
            prefs[Keys.LAST_WEEKLY_GOAL_SHOWN_WEEK] = updated.lastWeeklyGoalShownWeek
            prefs[Keys.LAST_WEEKLY_GOAL_SHOWN_YEAR] = updated.lastWeeklyGoalShownYear
            prefs[Keys.LAST_YEARLY_GOAL_HANDLED_YEAR] = updated.lastYearlyGoalHandledYear
            prefs[Keys.LAST_WEEKLY_GOAL_HANDLED_WEEK] = updated.lastWeeklyGoalHandledWeek
            prefs[Keys.WOOP_ENABLED] = updated.woopEnabled
            prefs[Keys.AUTO_TRACKER_ENABLED] = updated.autoTrackerEnabled
            prefs[Keys.DAILY_PLAN_NOTIFICATIONS_ENABLED] = updated.dailyPlanNotificationsEnabled
            prefs[Keys.STREAK_NOTIFICATIONS_ENABLED] = updated.streakNotificationsEnabled
            prefs[Keys.AUTONOMY_NUDGE_NOTIFICATIONS_ENABLED] = updated.autonomyNudgeNotificationsEnabled
            prefs[Keys.DEADLINE_REMINDER_NOTIFICATIONS_ENABLED] = updated.deadlineReminderNotificationsEnabled
            prefs[Keys.DEADLINE_ESCALATION_NOTIFICATIONS_ENABLED] = updated.deadlineEscalationNotificationsEnabled
            prefs[Keys.DAILY_PLAN_NOTIFICATION_HOUR] = updated.dailyPlanNotificationHour.coerceIn(0, 23)
            prefs[Keys.STREAK_CHECK_NOTIFICATION_HOUR] = updated.streakCheckNotificationHour.coerceIn(0, 23)
            prefs[Keys.USER_GUIDE_PROMPT_SHOWN] = updated.userGuidePromptShown
            prefs[Keys.LEFT_PAGE_QUICK_NOTE] = updated.leftPageQuickNote
            if (updated.manualChronotype != null) {
                prefs[Keys.MANUAL_CHRONOTYPE] = updated.manualChronotype
            } else {
                prefs.remove(Keys.MANUAL_CHRONOTYPE)
            }
            if (updated.quizChronotype != null) {
                prefs[Keys.QUIZ_CHRONOTYPE] = updated.quizChronotype
            } else {
                prefs.remove(Keys.QUIZ_CHRONOTYPE)
            }
            prefs[Keys.QUIZ_PROGRESS] = updated.quizProgress
            prefs[Keys.MEQ_SCORE] = updated.meqScore
            prefs[Keys.SLEEP_PRESSURE_POINTS] = updated.sleepPressurePoints
            prefs[Keys.SLEEP_PRESSURE_TRACKING_STARTED_AT] = updated.sleepPressureTrackingStartedAtMillis
            prefs[Keys.SLEEP_PRESSURE_LAST_COMPUTED_AT] = updated.sleepPressureLastComputedAtMillis
            prefs[Keys.AUTO_FALLBACK_SLEEP_INSERTION_ENABLED] = updated.autoFallbackSleepInsertionEnabled
            prefs[Keys.DISMISSED_SLEEP_FALLBACKS] = updated.dismissedSleepFallbacks
            prefs[Keys.DAILY_DEEP_WORK_TARGET_MINUTES] = updated.dailyDeepWorkTargetMinutes.coerceIn(30, 720)
            prefs[Keys.DISTRACTING_PACKAGES] = updated.distractingPackages
            prefs[Keys.MOMENT_INTERRUPTION_SENSITIVITY] = updated.momentInterruptionSensitivity.coerceIn(0.5f, 2.0f)
            prefs[Keys.MOMENT_NOTIFICATION_SENSITIVITY] = updated.momentNotificationSensitivity.coerceIn(0.5f, 2.0f)
            prefs[Keys.MOMENT_TASK_PRESSURE_SENSITIVITY] = updated.momentTaskPressureSensitivity.coerceIn(0.5f, 2.0f)
            prefs[Keys.ENERGY_TELEMETRY_ENABLED] = updated.energyTelemetryEnabled
            prefs[Keys.ENERGY_TELEMETRY_RETENTION_DAYS] = updated.energyTelemetryRetentionDays.coerceIn(1, 90)
            prefs[Keys.MORNING_TUNE_SLEEP_WEIGHT] = updated.morningTuneSleepWeight
            prefs[Keys.MORNING_TUNE_WAKE_WEIGHT] = updated.morningTuneWakeWeight
            prefs[Keys.MORNING_TUNE_BEHAVIOR_WEIGHT] = updated.morningTuneBehaviorWeight
            prefs[Keys.MORNING_TUNE_BASE_WEIGHT] = updated.morningTuneBaseWeight
            prefs[Keys.MORNING_TUNE_UPDATED_AT] = updated.morningTuneUpdatedAtMillis
            prefs[Keys.MORNING_TUNE_VERSION] = updated.morningTuneVersion
            prefs[Keys.ADAPTIVE_PEAK_FREEZE_ENABLED] = updated.adaptivePeakFreezeEnabled
            prefs[Keys.PEAK_QUALITY_DEGRADE_STREAK] = updated.peakQualityDegradeStreak.coerceAtLeast(0)
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_ENABLED] = updated.peakConfidenceAbstentionEnabled
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON] = updated.peakConfidenceAbstentionReason
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_TRIGGER_COUNT] = updated.peakConfidenceAbstentionTriggerCount.coerceAtLeast(0)
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_RECOVERY_COUNT] = updated.peakConfidenceAbstentionRecoveryCount.coerceAtLeast(0)
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_LAST_CHANGED_AT] = updated.peakConfidenceAbstentionLastChangedAtMillis
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_FREEZE_COUNT] = updated.peakConfidenceAbstentionReasonFreezeCount.coerceAtLeast(0)
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_LOW_SAMPLES_COUNT] = updated.peakConfidenceAbstentionReasonLowSamplesCount.coerceAtLeast(0)
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_LOW_COVERAGE_COUNT] = updated.peakConfidenceAbstentionReasonLowCoverageCount.coerceAtLeast(0)
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_WAKE_VARIANCE_COUNT] = updated.peakConfidenceAbstentionReasonWakeVarianceCount.coerceAtLeast(0)
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_DIVERGENCE_COUNT] = updated.peakConfidenceAbstentionReasonDivergenceCount.coerceAtLeast(0)
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_OTHER_COUNT] = updated.peakConfidenceAbstentionReasonOtherCount.coerceAtLeast(0)
            prefs[Keys.MANUAL_PEAK_PROFILE_ENABLED] = updated.manualPeakProfileEnabled
            prefs[Keys.MANUAL_PEAK_PROFILE_TYPE] = updated.manualPeakProfileType
            prefs[Keys.MANUAL_PEAK_ANCHOR_MINUTE_OF_DAY] = updated.manualPeakAnchorMinuteOfDay.coerceIn(0, 1439)
            prefs[Keys.MANUAL_PEAK_WINDOW_1_START_OFFSET] = updated.manualPeakWindow1StartOffsetMinutes.coerceIn(0, 1439)
            prefs[Keys.MANUAL_PEAK_WINDOW_2_START_OFFSET] = updated.manualPeakWindow2StartOffsetMinutes.coerceIn(0, 1439)
            prefs[Keys.MANUAL_PEAK_WINDOW_3_START_OFFSET] = updated.manualPeakWindow3StartOffsetMinutes.coerceIn(0, 1439)
            prefs[Keys.MANUAL_PEAK_WINDOW_1_DURATION] = updated.manualPeakWindow1DurationMinutes.coerceIn(30, 360)
            prefs[Keys.MANUAL_PEAK_WINDOW_2_DURATION] = updated.manualPeakWindow2DurationMinutes.coerceIn(30, 360)
            prefs[Keys.MANUAL_PEAK_WINDOW_3_DURATION] = updated.manualPeakWindow3DurationMinutes.coerceIn(30, 360)
            prefs[Keys.MANUAL_PEAK_WINDOW_1_AMPLITUDE] = updated.manualPeakWindow1Amplitude.coerceIn(0.2f, 1f)
            prefs[Keys.MANUAL_PEAK_WINDOW_2_AMPLITUDE] = updated.manualPeakWindow2Amplitude.coerceIn(0.2f, 1f)
            prefs[Keys.MANUAL_PEAK_WINDOW_3_AMPLITUDE] = updated.manualPeakWindow3Amplitude.coerceIn(0.2f, 1f)
            prefs[Keys.PROTECTED_TIME_SLOTS] = encodeProtectedTimeSlots(updated.protectedTimeSlots)
            prefs[Keys.DYNAMIC_COLOR_ENABLED] = updated.dynamicColorEnabled
            prefs[Keys.AMOLED_DARK_ENABLED] = updated.amoledDarkEnabled
            prefs[Keys.DND_ENABLED] = updated.dndEnabled
            prefs[Keys.USE_V2_UI] = updated.useV2Ui
            prefs[Keys.GEMINI_API_KEY] = updated.geminiApiKey
            prefs[Keys.GEMINI_AI_ENABLED] = updated.geminiAiEnabled
            prefs[Keys.ADAPTIVE_SYSTEM_DIRECTIVE] = updated.adaptiveSystemDirective
            prefs[Keys.WEEKLY_AI_AUDIT_JSON] = updated.weeklyAiAuditJson
            prefs[Keys.RECENTLY_DELETED_TASKS_JSON] = updated.recentlyDeletedTasksJson
            prefs[Keys.STIMULANT_LOGS_JSON] = updated.stimulantLogsJson
            prefs[Keys.QUICK_PHYSIOLOGICAL_LOGS_JSON] = updated.quickPhysiologicalLogsJson
        }
    }

    suspend fun setUseV2Ui(enabled: Boolean) {
        updatePreferences { prefs ->
            prefs.copy(useV2Ui = enabled)
        }
    }

    suspend fun setLastYearlyGoalHandledYear(year: Int) {
        updatePreferences { prefs ->
            prefs.copy(lastYearlyGoalHandledYear = year)
        }
    }

    suspend fun setLastWeeklyGoalHandledWeek(weekKey: Int) {
        updatePreferences { prefs ->
            prefs.copy(lastWeeklyGoalHandledWeek = weekKey)
        }
    }

    suspend fun clearGoalStrings() {
        updatePreferences { prefs ->
            prefs.copy(yearlyGoals = emptyList(), weeklyGoals = emptyList())
        }
    }

    suspend fun clearGoalStringsAndMarkHandled(yearlyHandledYear: Int, weeklyHandledWeek: Int) {
        updatePreferences { prefs ->
            prefs.copy(
                yearlyGoals = emptyList(),
                weeklyGoals = emptyList(),
                lastYearlyGoalHandledYear = if (yearlyHandledYear > 0) yearlyHandledYear else prefs.lastYearlyGoalHandledYear,
                lastWeeklyGoalHandledWeek = if (weeklyHandledWeek > 0) weeklyHandledWeek else prefs.lastWeeklyGoalHandledWeek
            )
        }
    }

    suspend fun setAutoScheduleToggleOn(enabled: Boolean) {
        updatePreferences { prefs ->
            prefs.copy(isAutoScheduleToggleOn = enabled)
        }
    }

    suspend fun updateProtectedTimeSlots(slots: List<ProtectedTimeSlot>) {
        updatePreferences { prefs ->
            prefs.copy(protectedTimeSlots = slots)
        }
        protectedSlotDao?.let { dao ->
            runCatching {
                dao.insertAll(slots.map { slot ->
                    com.neuroflow.app.data.local.entity.ProtectedSlotEntity(
                        id = slot.id,
                        title = slot.label,
                        startHour = slot.startMinuteOfDay / 60,
                        startMinute = slot.startMinuteOfDay % 60,
                        durationMinutes = slot.durationMinutes,
                        daysOfWeekMask = slot.weekdayBitmask,
                        isEnabled = slot.isEnabled
                    )
                })
            }
        }
    }

    suspend fun mergeTagCatalog(tags: Collection<String>) {
        val cleaned = tags.mapNotNull { it.trim().takeIf(String::isNotBlank) }
        if (cleaned.isEmpty()) return

        updatePreferences { prefs ->
            prefs.copy(tagCatalog = mergeTags(prefs.tagCatalog, cleaned))
        }
    }

    suspend fun removeTagFromCatalog(tag: String) {
        val cleaned = tag.trim()
        if (cleaned.isBlank()) return

        updatePreferences { prefs ->
            prefs.copy(tagCatalog = prefs.tagCatalog.filterNot { it.equals(cleaned, ignoreCase = true) })
        }
    }

    private fun mergeTags(existing: List<String>, incoming: Collection<String>): List<String> {
        val merged = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        fun addTag(tag: String) {
            val key = tag.lowercase()
            if (seen.add(key)) {
                merged += tag
            }
        }

        existing.forEach(::addTag)
        incoming.forEach(::addTag)
        return withStarterTagCatalog(merged)
    }

    private fun withStarterTagCatalog(existing: List<String>): List<String> {
        val merged = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        fun addTag(tag: String) {
            val cleaned = tag.trim()
            if (cleaned.isBlank()) return
            val key = cleaned.lowercase()
            if (seen.add(key)) {
                merged += cleaned
            }
        }

        existing.forEach(::addTag)
        TaskTagSchedulingProfile.starterTags.forEach(::addTag)
        return merged.sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    suspend fun recordDeletedTask(title: String, durationMinutes: Int, category: String, postponeCount: Int) {
        val trimmedTitle = title.trim().take(60)
        if (trimmedTitle.isBlank()) return
        updatePreferences { prefs ->
            val list = try {
                val arr = JSONArray(prefs.recentlyDeletedTasksJson.ifBlank { "[]" })
                val items = mutableListOf<JSONObject>()
                for (i in 0 until arr.length()) items.add(arr.getJSONObject(i))
                items
            } catch (_: Exception) {
                mutableListOf<JSONObject>()
            }
            val newItem = JSONObject().apply {
                put("title", trimmedTitle)
                put("durationMinutes", durationMinutes)
                put("category", category)
                put("postponeCount", postponeCount)
                put("deletedAt", System.currentTimeMillis())
            }
            list.add(0, newItem)
            val trimmedList = list.take(15)
            val jsonArr = JSONArray()
            trimmedList.forEach { jsonArr.put(it) }
            prefs.copy(recentlyDeletedTasksJson = jsonArr.toString())
        }
    }

    suspend fun updateAdaptiveSystemDirective(directive: String, auditJson: String) {
        updatePreferences { prefs ->
            prefs.copy(
                adaptiveSystemDirective = directive.trim(),
                weeklyAiAuditJson = auditJson.trim()
            )
        }
    }

    suspend fun recordStimulantDose(
        type: com.neuroflow.app.domain.service.StimulantType,
        doseMg: Double = when (type) {
            com.neuroflow.app.domain.service.StimulantType.CAFFEINE -> 100.0
            com.neuroflow.app.domain.service.StimulantType.GREEN_TEA_L_THEANINE -> 50.0
            com.neuroflow.app.domain.service.StimulantType.PRESCRIPTION_STIMULANT_IR -> 10.0
            com.neuroflow.app.domain.service.StimulantType.PRESCRIPTION_STIMULANT_XR -> 20.0
        },
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        updatePreferences { prefs ->
            val list = try {
                val arr = JSONArray(prefs.stimulantLogsJson.ifBlank { "[]" })
                val items = mutableListOf<JSONObject>()
                for (i in 0 until arr.length()) items.add(arr.getJSONObject(i))
                items
            } catch (_: Exception) {
                mutableListOf<JSONObject>()
            }
            val newItem = JSONObject().apply {
                put("type", type.name)
                put("doseMg", doseMg)
                put("timestampMillis", timestampMillis)
            }
            list.add(0, newItem)
            val trimmedList = list.take(30)
            val jsonArr = JSONArray()
            trimmedList.forEach { jsonArr.put(it) }
            prefs.copy(stimulantLogsJson = jsonArr.toString())
        }
    }

    suspend fun undoLastStimulantDose(): Boolean {
        var removed = false
        updatePreferences { prefs ->
            val list = try {
                val arr = JSONArray(prefs.stimulantLogsJson.ifBlank { "[]" })
                val items = mutableListOf<JSONObject>()
                for (i in 0 until arr.length()) items.add(arr.getJSONObject(i))
                items
            } catch (_: Exception) {
                mutableListOf<JSONObject>()
            }
            if (list.isNotEmpty()) {
                list.removeAt(0)
                removed = true
                val jsonArr = JSONArray()
                list.forEach { jsonArr.put(it) }
                prefs.copy(stimulantLogsJson = jsonArr.toString())
            } else {
                prefs
            }
        }
        return removed
    }

    suspend fun removeStimulantDose(timestampMillis: Long, type: String? = null): Boolean {
        var removed = false
        updatePreferences { prefs ->
            val list = try {
                val arr = JSONArray(prefs.stimulantLogsJson.ifBlank { "[]" })
                val items = mutableListOf<JSONObject>()
                for (i in 0 until arr.length()) items.add(arr.getJSONObject(i))
                items
            } catch (_: Exception) {
                mutableListOf<JSONObject>()
            }
            val index = list.indexOfFirst { obj ->
                val ts = obj.optLong("timestampMillis")
                val t = obj.optString("type")
                ts == timestampMillis && (type == null || t == type)
            }
            if (index >= 0) {
                list.removeAt(index)
                removed = true
                val jsonArr = JSONArray()
                list.forEach { jsonArr.put(it) }
                prefs.copy(stimulantLogsJson = jsonArr.toString())
            } else {
                prefs
            }
        }
        return removed
    }

    suspend fun clearStimulantLogs() {
        updatePreferences { prefs ->
            prefs.copy(stimulantLogsJson = "[]")
        }
    }

    suspend fun recordPhysiologicalEvent(
        type: PhysiologicalEventType,
        timestampMillis: Long = System.currentTimeMillis(),
        durationMinutes: Int = when (type) {
            PhysiologicalEventType.MEAL -> 30
            PhysiologicalEventType.WORKOUT -> 45
            PhysiologicalEventType.NAP -> 20
        },
        intensity: Double = 1.0
    ) {
        updatePreferences { prefs ->
            val list = try {
                val arr = JSONArray(prefs.quickPhysiologicalLogsJson.ifBlank { "[]" })
                val items = mutableListOf<JSONObject>()
                for (i in 0 until arr.length()) items.add(arr.getJSONObject(i))
                items
            } catch (_: Exception) {
                mutableListOf<JSONObject>()
            }
            val newItem = JSONObject().apply {
                put("type", type.name)
                put("timestampMillis", timestampMillis)
                put("durationMinutes", durationMinutes)
                put("intensity", intensity)
            }
            list.add(0, newItem)
            val trimmedList = list.take(30)
            val jsonArr = JSONArray()
            trimmedList.forEach { jsonArr.put(it) }
            prefs.copy(quickPhysiologicalLogsJson = jsonArr.toString())
        }
    }

    suspend fun undoLastPhysiologicalEvent(): Boolean {
        var removed = false
        updatePreferences { prefs ->
            val list = try {
                val arr = JSONArray(prefs.quickPhysiologicalLogsJson.ifBlank { "[]" })
                val items = mutableListOf<JSONObject>()
                for (i in 0 until arr.length()) items.add(arr.getJSONObject(i))
                items
            } catch (_: Exception) {
                mutableListOf<JSONObject>()
            }
            if (list.isNotEmpty()) {
                list.removeAt(0)
                removed = true
                val jsonArr = JSONArray()
                list.forEach { jsonArr.put(it) }
                prefs.copy(quickPhysiologicalLogsJson = jsonArr.toString())
            } else {
                prefs
            }
        }
        return removed
    }

    suspend fun clearPhysiologicalLogs() {
        updatePreferences { prefs ->
            prefs.copy(quickPhysiologicalLogsJson = "[]")
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}

enum class PhysiologicalEventType {
    MEAL, WORKOUT, NAP
}

data class PhysiologicalEventLog(
    val type: PhysiologicalEventType,
    val timestampMillis: Long,
    val durationMinutes: Int = 0,
    val intensity: Double = 1.0
)

fun parsePhysiologicalLogs(json: String?): List<PhysiologicalEventLog> {
    if (json.isNullOrBlank() || json == "[]") return emptyList()
    return try {
        val arr = JSONArray(json)
        val list = mutableListOf<PhysiologicalEventLog>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val typeStr = obj.optString("type", "MEAL")
            val type = try {
                PhysiologicalEventType.valueOf(typeStr.uppercase())
            } catch (_: Exception) {
                PhysiologicalEventType.MEAL
            }
            val ts = obj.optLong("timestampMillis", 0L)
            val dur = obj.optInt("durationMinutes", 0)
            val intensity = obj.optDouble("intensity", 1.0)
            if (ts > 0L) {
                list.add(PhysiologicalEventLog(type = type, timestampMillis = ts, durationMinutes = dur, intensity = intensity))
            }
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

fun parseStimulantLogs(json: String?): List<com.neuroflow.app.domain.service.StimulantDoseBout> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        val list = mutableListOf<com.neuroflow.app.domain.service.StimulantDoseBout>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val typeStr = obj.optString("type", "CAFFEINE")
            val type = com.neuroflow.app.domain.service.StimulantType.values().firstOrNull { it.name.equals(typeStr, ignoreCase = true) }
                ?: com.neuroflow.app.domain.service.StimulantType.CAFFEINE
            list.add(
                com.neuroflow.app.domain.service.StimulantDoseBout(
                    timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis()),
                    doseMg = obj.optDouble("doseMg", 100.0),
                    type = type
                )
            )
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

fun UserPreferences.isAdvancedFeaturesUnlocked(nowMillis: Long = System.currentTimeMillis()): Boolean {
    if (dailyStreak >= 7) return true
    if (firstAppOpenMillis == 0L) {
        return onboardingCompleted
    }
    val oneWeekMillis = 7L * 24 * 60 * 60 * 1000L
    return (nowMillis - firstAppOpenMillis) >= oneWeekMillis
}

fun UserPreferences.daysUntilAdvancedUnlock(nowMillis: Long = System.currentTimeMillis()): Int {
    if (isAdvancedFeaturesUnlocked(nowMillis)) return 0
    if (firstAppOpenMillis == 0L) return 7
    val oneWeekMillis = 7L * 24 * 60 * 60 * 1000L
    val oneDayMillis = 24.0 * 60 * 60 * 1000.0
    val elapsed = nowMillis - firstAppOpenMillis
    val remaining = oneWeekMillis - elapsed
    return if (remaining <= 0) 0 else ceil(remaining / oneDayMillis).toInt().coerceIn(1, 7)
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainmodelenumskt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/model/Enums.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/model/Enums.kt`
- **Lines of Code:** 74
- **Size:** 1,319 bytes (1.3 KB)

```kotlin
package com.neuroflow.app.domain.model

enum class Quadrant {
    DO_FIRST,
    SCHEDULE,
    DELEGATE,
    ELIMINATE
}

enum class Priority {
    HIGH,
    MEDIUM,
    LOW
}

enum class TaskStatus {
    ACTIVE,
    COMPLETED,
    ARCHIVED
}

enum class Recurrence {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    CUSTOM
}

enum class EnergyLevel {
    HIGH,
    MEDIUM,
    LOW
}

enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM
}

// Circadian rhythm task type — used to match task to optimal time of day
// ANALYTICAL: deep work, problem solving, writing (best in peak hours)
// CREATIVE: brainstorming, design, ideation (best mid-morning or late afternoon)
// ADMIN: emails, scheduling, routine tasks(best in low-energy slots)
// PHYSICAL: exercise, errands, calls (flexible)
enum class TaskType {
    ANALYTICAL,
    CREATIVE,
    ADMIN,
    PHYSICAL,
    GENERAL,
    MINDFULNESS
}

enum class HyperFocusState {
    INACTIVE,
    ACTIVE,
    // All tasks done — user must add ≥3 tasks for tomorrow before apps unlock
    FULL_REWARD_PENDING
}

enum class HyperFocusSessionMode {
    TASK_BASED,
    TIME_BASED
}

enum class RewardTier(val unlockMinutes: Int, val taskThreshold: Int) {
    NONE(0, 0),
    MICRO(2, 1),
    PARTIAL(10, 3),
    EARNED(30, 5),
    FULL(Int.MAX_VALUE, Int.MAX_VALUE)
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainmodelprotectedtimeslotkt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/model/ProtectedTimeSlot.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/model/ProtectedTimeSlot.kt`
- **Lines of Code:** 33
- **Size:** 741 bytes (0.7 KB)

```kotlin
package com.neuroflow.app.domain.model

enum class PtsRecurrence {
    DAILY,
    WEEKDAYS,
    WEEKENDS,
    CUSTOM,
    SPECIFIC_DATE
}

enum class PtsCategory {
    MINDFULNESS,
    EXERCISE,
    MEAL,
    FAMILY,
    BREAK,
    WORK,
    STUDY,
    CUSTOM
}

data class ProtectedTimeSlot(
    val id: String,
    val label: String,
    val startMinuteOfDay: Int, // 0..1439
    val durationMinutes: Int,   // 15..360
    val recurrence: PtsRecurrence = PtsRecurrence.DAILY,
    val weekdayBitmask: Int = 0b1111111, // bit 1 = Sunday, bit 2 = Monday, etc.
    val category: PtsCategory = PtsCategory.BREAK,
    val recoveryBufferMinutes: Int = 0, // buffer after slot
    val isEnabled: Boolean = true,
    val dateMillis: Long? = null
)
```

---

<a id="appsrcmainjavacomneuroflowappdomainmodelstepbystepplankt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/model/StepByStepPlan.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/model/StepByStepPlan.kt`
- **Lines of Code:** 58
- **Size:** 2,001 bytes (2.0 KB)

```kotlin
package com.neuroflow.app.domain.model

import java.util.Locale

data class StepByStepPlanStep(
    val text: String,
    val completed: Boolean
)

object StepByStepPlanCodec {

    fun parse(raw: String): List<StepByStepPlanStep> {
        return raw
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\r", "\n")
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                val normalized = line.lowercase(Locale.getDefault())
                when {
                    normalized.startsWith("[x] ") -> StepByStepPlanStep(
                        text = line.drop(4).trim(),
                        completed = true
                    )
                    normalized.startsWith("[ ] ") -> StepByStepPlanStep(
                        text = line.drop(4).trim(),
                        completed = false
                    )
                    normalized.startsWith("- [x] ") -> StepByStepPlanStep(
                        text = line.drop(6).trim(),
                        completed = true
                    )
                    normalized.startsWith("- [ ] ") -> StepByStepPlanStep(
                        text = line.drop(6).trim(),
                        completed = false
                    )
                    else -> StepByStepPlanStep(text = line, completed = false)
                }
            }
            .toList()
    }

    fun serialize(steps: List<StepByStepPlanStep>): String {
        return steps.joinToString(separator = "\n") { step ->
            if (step.completed) "[x] ${step.text}" else "[ ] ${step.text}"
        }
    }

    fun setStepCompleted(raw: String, index: Int, completed: Boolean): String {
        val steps = parse(raw)
        if (index !in steps.indices) return raw
        val updated = steps.toMutableList()
        updated[index] = updated[index].copy(completed = completed)
        return serialize(updated)
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdidomainservicemodulekt"></a>
### File: `app/src/main/java/com/neuroflow/app/di/DomainServiceModule.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/di/DomainServiceModule.kt`
- **Lines of Code:** 70
- **Size:** 2,332 bytes (2.3 KB)

```kotlin
package com.neuroflow.app.di

import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.DefaultBehavioralSupportService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultSmartTaskDecompositionService
import com.neuroflow.app.domain.service.DefaultTaskNaturalLanguageParser
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.SmartTaskDecompositionService
import com.neuroflow.app.domain.service.TaskNaturalLanguageParser
import com.neuroflow.app.domain.service.TaskSchedulerService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DomainServiceModule {

    @Binds
    @Singleton
    abstract fun bindCircadianEnergyService(
        impl: DefaultCircadianEnergyService
    ): CircadianEnergyService

    @Binds
    @Singleton
    abstract fun bindTaskSchedulerService(
        impl: DefaultTaskSchedulerService
    ): TaskSchedulerService

    @Binds
    @Singleton
    abstract fun bindTaskNaturalLanguageParser(
        impl: DefaultTaskNaturalLanguageParser
    ): TaskNaturalLanguageParser

    @Binds
    @Singleton
    abstract fun bindBehavioralSupportService(
        impl: DefaultBehavioralSupportService
    ): BehavioralSupportService

    @Binds
    @Singleton
    abstract fun bindSmartTaskDecompositionService(
        impl: DefaultSmartTaskDecompositionService
    ): SmartTaskDecompositionService

    @Binds
    @Singleton
    abstract fun bindGeminiAiOnlineService(
        impl: com.neuroflow.app.domain.service.DefaultGeminiAiOnlineService
    ): com.neuroflow.app.domain.service.GeminiAiOnlineService

    @Binds
    @Singleton
    abstract fun bindCalendarSyncRepository(
        impl: com.neuroflow.app.data.calendar.CalendarSyncDataSource
    ): com.neuroflow.app.data.calendar.CalendarSyncRepository

    @Binds
    @Singleton
    abstract fun bindSleepTelemetryRepository(
        impl: com.neuroflow.app.data.sleep.DefaultSleepTelemetryRepository
    ): com.neuroflow.app.data.sleep.SleepTelemetryRepository
}
```

---

<a id="appsrcmainjavacomneuroflowappdiappmodulekt"></a>
### File: `app/src/main/java/com/neuroflow/app/di/AppModule.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/di/AppModule.kt`
- **Lines of Code:** 162
- **Size:** 6,137 bytes (6.0 KB)

```kotlin
package com.neuroflow.app.di

import android.content.Context
import android.content.pm.LauncherApps
import androidx.room.Room
import androidx.work.WorkManager
import com.neuroflow.app.data.local.MIGRATION_1_2
import com.neuroflow.app.data.local.MIGRATION_2_3
import com.neuroflow.app.data.local.MIGRATION_3_4
import com.neuroflow.app.data.local.MIGRATION_4_5
import com.neuroflow.app.data.local.MIGRATION_5_6
import com.neuroflow.app.data.local.MIGRATION_6_7
import com.neuroflow.app.data.local.MIGRATION_7_8
import com.neuroflow.app.data.local.MIGRATION_8_9
import com.neuroflow.app.data.local.MIGRATION_9_10
import com.neuroflow.app.data.local.MIGRATION_10_11
import com.neuroflow.app.data.local.MIGRATION_11_12
import com.neuroflow.app.data.local.MIGRATION_12_13
import com.neuroflow.app.data.local.MIGRATION_13_14
import com.neuroflow.app.data.local.MIGRATION_14_15
import com.neuroflow.app.data.local.MIGRATION_15_16
import com.neuroflow.app.data.local.MIGRATION_16_17
import com.neuroflow.app.data.local.MIGRATION_17_18
import com.neuroflow.app.data.local.MIGRATION_18_19
import com.neuroflow.app.data.local.MIGRATION_17_19
import com.neuroflow.app.data.local.MIGRATION_19_20
import com.neuroflow.app.data.local.MIGRATION_20_21
import com.neuroflow.app.data.local.MIGRATION_21_22
import com.neuroflow.app.data.local.NeuroFlowDatabase
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.dao.EnergyPredictionDao
import com.neuroflow.app.data.local.dao.FocusSessionDao
import com.neuroflow.app.data.local.dao.GoalDao
import com.neuroflow.app.data.local.dao.NoteDao
import com.neuroflow.app.data.local.dao.ProtectedSlotDao
import com.neuroflow.app.data.local.dao.QuickNoteDao
import com.neuroflow.app.data.local.dao.SleepLogDao
import com.neuroflow.app.data.local.dao.TaskDao
import com.neuroflow.app.data.local.dao.TaskMicroStepDao
import com.neuroflow.app.data.local.dao.TimeSessionDao
import com.neuroflow.app.data.local.dao.UlyssesContractDao
import com.neuroflow.app.data.local.dao.WoopDao
import com.neuroflow.app.data.repository.NoteRepository
import com.neuroflow.app.data.repository.UlyssesContractRepository
import com.neuroflow.app.data.repository.WoopRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NeuroFlowDatabase {
        return Room.databaseBuilder(
            context,
            NeuroFlowDatabase::class.java,
            "neuroflow_database"
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14,
                MIGRATION_14_15,
                MIGRATION_15_16,
                MIGRATION_16_17,
                MIGRATION_17_18,
                MIGRATION_18_19,
                MIGRATION_17_19,
                MIGRATION_19_20,
                MIGRATION_20_21,
                MIGRATION_21_22
            )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideTaskDao(database: NeuroFlowDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideTimeSessionDao(database: NeuroFlowDatabase): TimeSessionDao = database.timeSessionDao()

    @Provides
    fun provideTaskMicroStepDao(database: NeuroFlowDatabase): TaskMicroStepDao = database.taskMicroStepDao()

    @Provides
    fun provideFocusSessionDao(database: NeuroFlowDatabase): FocusSessionDao = database.focusSessionDao()

    @Provides
    fun provideQuickNoteDao(database: NeuroFlowDatabase): QuickNoteDao = database.quickNoteDao()

    @Provides
    fun provideProtectedSlotDao(database: NeuroFlowDatabase): ProtectedSlotDao = database.protectedSlotDao()

    @Provides
    fun provideGoalDao(database: NeuroFlowDatabase): GoalDao = database.goalDao()

    @Provides
    fun provideWoopDao(database: NeuroFlowDatabase): WoopDao = database.woopDao()

    @Provides
    fun provideUlyssesContractDao(database: NeuroFlowDatabase): UlyssesContractDao = database.ulyssesContractDao()

    @Provides
    fun provideSleepLogDao(database: NeuroFlowDatabase): SleepLogDao = database.sleepLogDao()

    @Provides
    fun provideEnergyPredictionDao(database: NeuroFlowDatabase): EnergyPredictionDao = database.energyPredictionDao()

    @Provides
    fun provideNoteDao(database: NeuroFlowDatabase): NoteDao = database.noteDao()

    @Provides
    @Singleton
    fun provideNoteRepository(dao: NoteDao): NoteRepository = NoteRepository(dao)

    @Provides
    @Singleton
    fun provideWoopRepository(dao: WoopDao): WoopRepository = WoopRepository(dao)

    @Provides
    @Singleton
    fun provideUlyssesContractRepository(dao: UlyssesContractDao): UlyssesContractRepository = UlyssesContractRepository(dao)

    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(
        @ApplicationContext context: Context,
        protectedSlotDao: ProtectedSlotDao
    ): UserPreferencesDataStore = UserPreferencesDataStore(context, protectedSlotDao)

    @Provides
    @Singleton
    fun provideLauncherApps(@ApplicationContext context: Context): LauncherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun providePassiveSleepSyncRepository(
        manager: com.neuroflow.app.data.repository.PassiveSleepSyncManager
    ): com.neuroflow.app.domain.repository.PassiveSleepSyncRepository = manager
}
```

---

