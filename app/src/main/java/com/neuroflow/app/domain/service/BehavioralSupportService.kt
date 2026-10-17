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
        val clean = taskTitle.trim()
        val totalMins = estimatedMinutes.coerceAtLeast(15)
        val stepCount = when {
            totalMins >= 60 -> 4
            totalMins >= 30 -> 3
            else -> 2
        }
        val perStepMins = (totalMins / stepCount).coerceIn(5, 20)

        return when (stepCount) {
            4 -> listOf(
                MicroStepSuggestion("Gather materials & open required tools for '$clean'", 5),
                MicroStepSuggestion("Outline the first section or quick draft", perStepMins),
                MicroStepSuggestion("Execute core implementation of '$clean'", perStepMins),
                MicroStepSuggestion("Review, verify and finalize", 5)
            )
            3 -> listOf(
                MicroStepSuggestion("Set up workspace & review instructions for '$clean'", 5),
                MicroStepSuggestion("Draft first rough version without judgment", perStepMins),
                MicroStepSuggestion("Polish and wrap up '$clean'", 5)
            )
            else -> listOf(
                MicroStepSuggestion("Take the first 5-minute starter step for '$clean'", 5),
                MicroStepSuggestion("Complete the remainder of '$clean'", totalMins - 5)
            )
        }
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
