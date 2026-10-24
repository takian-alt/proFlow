package com.neuroflow.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.effectiveReminderTargetMillis
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.local.entity.startOfPreviousLocalDay
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.determineCategory
import com.neuroflow.app.domain.service.ChronotypeProfile
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

const val CHANNEL_TASKS = "tasks"
const val CHANNEL_FOCUS = "focus"
const val CHANNEL_DAILY = "daily_plan"
const val WORK_DAILY_PLAN = "daily_plan"
const val WORK_STREAK_CHECK = "streak_check"
const val WORK_DEADLINE_ESCALATION = "deadline_escalation"

internal data class NotificationWorkerPlan(
    val dailyPlanEnabled: Boolean,
    val dailyPlanDelayMs: Long,
    val streakEnabled: Boolean,
    val streakDelayMs: Long,
    val escalationEnabled: Boolean
)

internal fun delayUntilHour(hour: Int, nowMillis: Long = System.currentTimeMillis()): Long {
    val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val target = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
    return target.timeInMillis - now.timeInMillis
}

internal fun buildNotificationWorkerPlan(
    prefs: UserPreferences,
    nowMillis: Long = System.currentTimeMillis()
): NotificationWorkerPlan {
    return NotificationWorkerPlan(
        dailyPlanEnabled = prefs.dailyPlanNotificationsEnabled,
        dailyPlanDelayMs = delayUntilHour(prefs.dailyPlanNotificationHour, nowMillis),
        streakEnabled = prefs.streakNotificationsEnabled,
        streakDelayMs = delayUntilHour(prefs.streakCheckNotificationHour, nowMillis),
        escalationEnabled = prefs.deadlineEscalationNotificationsEnabled
    )
}

fun scheduleNotificationWorkers(context: Context, prefs: UserPreferences) {
    val workManager = WorkManager.getInstance(context)
    val plan = buildNotificationWorkerPlan(prefs)

    if (plan.dailyPlanEnabled) {
        val dailyPlanRequest = PeriodicWorkRequestBuilder<DailyPlanWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(plan.dailyPlanDelayMs, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_DAILY_PLAN,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            dailyPlanRequest
        )
    } else {
        workManager.cancelUniqueWork(WORK_DAILY_PLAN)
    }

    if (plan.streakEnabled) {
        val streakCheckRequest = PeriodicWorkRequestBuilder<StreakCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(plan.streakDelayMs, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_STREAK_CHECK,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            streakCheckRequest
        )
    } else {
        workManager.cancelUniqueWork(WORK_STREAK_CHECK)
    }

    if (plan.escalationEnabled) {
        val escalationRequest = PeriodicWorkRequestBuilder<DeadlineEscalationWorker>(4, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(
            WORK_DEADLINE_ESCALATION,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            escalationRequest
        )
    } else {
        workManager.cancelUniqueWork(WORK_DEADLINE_ESCALATION)
    }
}

fun createNotificationChannels(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val taskChannel = NotificationChannel(
        CHANNEL_TASKS, "Task Reminders", NotificationManager.IMPORTANCE_HIGH
    ).apply { description = "Reminders for upcoming task deadlines" }

    val focusChannel = NotificationChannel(
        CHANNEL_FOCUS, "Focus & Streaks", NotificationManager.IMPORTANCE_DEFAULT
    ).apply { description = "Focus time and streak notifications" }

    val dailyChannel = NotificationChannel(
        CHANNEL_DAILY, "Daily Plan", NotificationManager.IMPORTANCE_DEFAULT
    ).apply { description = "Morning planning notifications" }

    val autonomyNudgeChannel = NotificationChannel(
        "autonomy_nudge", "Autonomy Nudges", NotificationManager.IMPORTANCE_DEFAULT
    ).apply { description = "Gentle nudges when a task hasn't been started" }

    manager.createNotificationChannels(listOf(taskChannel, focusChannel, dailyChannel, autonomyNudgeChannel))
}

@HiltWorker
class DailyPlanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val taskSchedulerService: com.neuroflow.app.domain.service.TaskSchedulerService,
    private val circadianEnergyService: com.neuroflow.app.domain.service.CircadianEnergyService,
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.dailyPlanNotificationsEnabled) Result.success()
        else {
            val tasks = taskRepository.getActiveTasks()
            val nowMillis = System.currentTimeMillis()
            val zoneId = ZoneId.systemDefault()
            val baseWakeMillis = ScheduleAutoTasksWorker.calculateWakeTimeMillis(nowMillis, prefs.wakeUpHour, zoneId)
            val sleepSignal = sleepTelemetryRepository?.getLatestNormalizedSleep(
                baseWakeMillis = baseWakeMillis,
                baseSleepMinutes = 480.0
            )
            val wakeTimeMillis = sleepSignal?.normalizedWakeTimeMillis ?: baseWakeMillis
            val sleepMinutes = sleepSignal?.blendedDurationMinutes?.roundToInt()?.coerceIn(60, 960) ?: 480
            val naps = sleepSignal?.daytimeNaps ?: emptyList()
            val chronotype = ChronotypeProfile.fromString(prefs.manualChronotype ?: prefs.quizChronotype)

            val snapshot = circadianEnergyService.evaluateSnapshot(
                timestampMillis = nowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepMinutes,
                continuousActiveFocusMinutes = 0,
                zoneId = zoneId,
                naps = naps,
                chronotype = chronotype
            )

            val topTasks = tasks.map { task ->
                val resolvedCategory = when (task.determineCategory()) {
                    com.neuroflow.app.domain.scheduler.TaskCategory.HARD_WORK,
                    com.neuroflow.app.domain.scheduler.TaskCategory.ANALYTICAL -> com.neuroflow.app.domain.service.TaskCategory.DEEP_WORK
                    com.neuroflow.app.domain.scheduler.TaskCategory.CREATIVE -> com.neuroflow.app.domain.service.TaskCategory.CREATIVE
                    com.neuroflow.app.domain.scheduler.TaskCategory.ROUTINE -> com.neuroflow.app.domain.service.TaskCategory.ADMIN
                    com.neuroflow.app.domain.scheduler.TaskCategory.EXERCISE,
                    com.neuroflow.app.domain.scheduler.TaskCategory.PHYSICAL -> com.neuroflow.app.domain.service.TaskCategory.EXERCISE
                    com.neuroflow.app.domain.scheduler.TaskCategory.MINDFULNESS -> com.neuroflow.app.domain.service.TaskCategory.MINDFULNESS
                    com.neuroflow.app.domain.scheduler.TaskCategory.FLEXIBLE -> com.neuroflow.app.domain.service.TaskCategory.GENERAL
                }
                val schedulable = com.neuroflow.app.domain.service.SchedulableTask(
                    id = task.id,
                    title = task.title,
                    category = resolvedCategory,
                    priority = when (task.priority) {
                        com.neuroflow.app.domain.model.Priority.HIGH -> 2
                        com.neuroflow.app.domain.model.Priority.MEDIUM -> 1
                        else -> 0
                    },
                    estimatedMinutes = task.estimatedDurationMinutes,
                    energyDemand = when (task.energyLevel) {
                        com.neuroflow.app.domain.model.EnergyLevel.HIGH -> com.neuroflow.app.domain.service.EnergyDemand.HIGH
                        com.neuroflow.app.domain.model.EnergyLevel.MEDIUM -> com.neuroflow.app.domain.service.EnergyDemand.MODERATE
                        else -> com.neuroflow.app.domain.service.EnergyDemand.LOW
                    },
                    isFrog = task.isFrog,
                    scheduledStartMillis = task.scheduledTime?.let { (task.scheduledDate ?: nowMillis) + it },
                    deadlineMillis = task.deadlineDate?.let { it + (task.deadlineTime ?: 0L) },
                    isLocked = task.isScheduleLocked,
                    parentTaskId = task.parentTaskId,
                    impactScore = task.impactScore,
                    postponeCount = task.postponeCount,
                    isAnxietyTask = task.isAnxietyTask,
                    hasImplementationIntention = task.ifThenPlan.isNotBlank()
                )
                val score = taskSchedulerService.evaluateTaskScore(schedulable, snapshot).totalScore
                task to score
            }.sortedByDescending { it.second }.take(3).map { it.first }

            if (topTasks.isNotEmpty()) {
                val taskNames = topTasks.joinToString(", ") { it.title }
                showNotification(
                    "Your top tasks today",
                    "Your top 3 tasks: $taskNames. Tap to confirm.",
                    CHANNEL_DAILY
                )
            }
            Result.success()
        }
    } catch (e: Exception) {
        android.util.Log.e("NotificationWorker", "DailyPlanWorker failed", e)
        Result.failure()
    }

    private fun showNotification(title: String, message: String, channelId: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID_DAILY_PLAN, notification)
    }

    companion object {
        private const val NOTIFICATION_ID_DAILY_PLAN = 10_000
    }
}

@HiltWorker
class StreakCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        var prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.streakNotificationsEnabled) Result.success()
        else {
            val today = startOfLocalDay(System.currentTimeMillis())
            val yesterday = startOfPreviousLocalDay(today)

            val completedToday = taskRepository.getCompletedTasks().any { task ->
                task.completedAt != null && task.completedAt >= today
            }

            if (!completedToday && prefs.lastActiveDate < yesterday && prefs.dailyStreak > 0) {
                preferencesDataStore.updatePreferences { it.copy(dailyStreak = 0) }
                prefs = prefs.copy(dailyStreak = 0)
            }

            if (!completedToday && prefs.dailyStreak > 0 && taskRepository.getActiveTasks().isNotEmpty()) {
                showNotification(
                    "Streak at risk! 🔥",
                    "Your ${prefs.dailyStreak}-day streak ends tonight. Complete 1 task!",
                    CHANNEL_FOCUS
                )
            }

            Result.success()
        }
    } catch (e: Exception) {
        android.util.Log.e("NotificationWorker", "StreakCheckWorker failed", e)
        Result.failure()
    }

    private fun showNotification(title: String, message: String, channelId: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID_STREAK, notification)
    }

    companion object {
        private const val NOTIFICATION_ID_STREAK = 10_001
    }
}

@HiltWorker
class DeadlineEscalationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.deadlineEscalationNotificationsEnabled) Result.success()
        else {
            val now = System.currentTimeMillis()
            val threshold = 48 * 3_600_000L

            val urgentTasks = taskRepository.getActiveTasks().filter { task ->
                task.quadrant != com.neuroflow.app.domain.model.Quadrant.DO_FIRST &&
                task.recurrence == com.neuroflow.app.domain.model.Recurrence.NONE &&
                task.deadlineDate != null &&
                (task.deadlineDate + (task.deadlineTime ?: 0L) - now) in 0L..threshold
            }

            if (urgentTasks.isNotEmpty()) {
                val names = urgentTasks.take(3).joinToString(", ") { it.title }
                val more = if (urgentTasks.size > 3) " +${urgentTasks.size - 3} more" else ""
                showNotification(
                    "⚠️ ${urgentTasks.size} task${if (urgentTasks.size > 1) "s" else ""} due soon",
                    "$names$more — deadline within 48 hours.",
                    CHANNEL_TASKS
                )
            }

            Result.success()
        }
    } catch (e: Exception) {
        android.util.Log.e("NotificationWorker", "DeadlineEscalationWorker failed", e)
        Result.failure()
    }

    private fun showNotification(title: String, message: String, channelId: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID_ESCALATION, notification)
    }

    companion object {
        private const val NOTIFICATION_ID_ESCALATION = 10_002
    }
}

@HiltWorker
class TaskReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = preferencesDataStore.preferencesFlow.first()
            if (!prefs.deadlineReminderNotificationsEnabled) return Result.success()

            val taskId = inputData.getString("taskId") ?: return Result.failure()
            val task = taskRepository.getById(taskId) ?: return Result.success()
            if (task.status != TaskStatus.ACTIVE) return Result.success()
            val taskTitle = inputData.getString("taskTitle") ?: task.title
            val minutesBefore = inputData.getLong("minutesBefore", 0L)
            val expectedTargetMs = inputData.getLong("targetMs", -1L)

            val actualTargetMs = task.effectiveReminderTargetMillis() ?: return Result.success()

            if (expectedTargetMs > 0 && kotlin.math.abs(actualTargetMs - expectedTargetMs) > 5 * 60_000L) {
                return Result.success()
            }

            val now = System.currentTimeMillis()
            if (actualTargetMs < now - 10 * 60_000L) return Result.success()

            val timeLabel = when (minutesBefore) {
                15L   -> "15 minutes"
                30L   -> "30 minutes"
                60L   -> "1 hour"
                1440L -> "1 day"
                else  -> "$minutesBefore minutes"
            }

            showNotification(
                "⏰ Task due in $timeLabel",
                taskTitle,
                CHANNEL_TASKS
            )

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("NotificationWorker", "TaskReminderWorker failed", e)
            Result.failure()
        }
    }

    private fun showNotification(title: String, message: String, channelId: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        val taskId = inputData.getString("taskId") ?: ""
        manager.notify(taskId.hashCode(), notification)
    }
}
