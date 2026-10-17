package com.neuroflow.app.domain.service

import javax.inject.Inject
import javax.inject.Singleton

data class MicroStepSuggestion(
    val title: String,
    val estimatedMinutes: Int
)

data class DistractionFeedback(
    val shouldPromptBreakdown: Boolean,
    val shouldPromptDecompression: Boolean,
    val supportiveMessage: String
)

interface BehavioralSupportService {
    fun resetScheduleAnchors(tasks: List<SchedulableTask>): List<SchedulableTask>
    fun generateMicroSteps(taskTitle: String, estimatedMinutes: Int): List<MicroStepSuggestion>
    fun evaluateDistraction(interruptionCount: Int, durationMinutes: Int): DistractionFeedback
}

@Singleton
class DefaultBehavioralSupportService @Inject constructor() : BehavioralSupportService {

    override fun resetScheduleAnchors(tasks: List<SchedulableTask>): List<SchedulableTask> {
        // Unlock unpinned tasks to allow a completely clean replan
        return tasks.map { task ->
            if (task.isLocked) task else task.copy(scheduledStartMillis = null)
        }
    }

    override fun generateMicroSteps(taskTitle: String, estimatedMinutes: Int): List<MicroStepSuggestion> {
        val clean = taskTitle.trim().take(30)
        return listOf(
            MicroStepSuggestion("Open workspace & gather materials for $clean", 5),
            MicroStepSuggestion("Spend 5 uninterrupted minutes on the very first sub-step", 5),
            MicroStepSuggestion("Review progress & polish final result", 5)
        )
    }

    override fun evaluateDistraction(interruptionCount: Int, durationMinutes: Int): DistractionFeedback {
        // Non-punitive: High distraction on tasks signals cognitive friction or excessive task size,
        // never boosting task priority.
        val isHighInterruption = interruptionCount >= 3 || (interruptionCount >= 2 && durationMinutes <= 25)
        val isLongSlog = durationMinutes >= 60 && interruptionCount >= 2

        return when {
            isLongSlog -> DistractionFeedback(
                shouldPromptBreakdown = true,
                shouldPromptDecompression = true,
                supportiveMessage = "This task seems heavy right now. Would you like to break it into 5-minute micro-steps or take a 3-minute decompression breather?"
            )
            isHighInterruption -> DistractionFeedback(
                shouldPromptBreakdown = true,
                shouldPromptDecompression = false,
                supportiveMessage = "Frequent interruptions detected. Breaking this task down into bite-sized steps can help rebuild momentum."
            )
            else -> DistractionFeedback(
                shouldPromptBreakdown = false,
                shouldPromptDecompression = false,
                supportiveMessage = "Great focus! You are maintaining healthy cognitive flow."
            )
        }
    }
}
