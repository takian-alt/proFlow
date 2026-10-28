package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.entity.TaskEntity
import java.util.Locale

enum class TagEnergyDemand {
    LOW,
    MEDIUM,
    HIGH
}

enum class TagPreferredWindow {
    MORNING,
    MIDDAY,
    EVENING,
    FLEXIBLE
}

enum class TaskCategory {
    MINDFULNESS,
    EXERCISE,
    PHYSICAL,
    ANALYTICAL,
    HARD_WORK,
    CREATIVE,
    ROUTINE,
    FLEXIBLE
}

data class TagSchedulingProfile(
    val tag: String,
    val energyDemand: TagEnergyDemand,
    val preferredContext: String?,
    val fragmentationTolerance: Float,
    val preferredWindow: TagPreferredWindow,
    val category: TaskCategory = TaskCategory.FLEXIBLE
)

object TaskTagSchedulingProfile {
    val starterTags: List<String> = listOf(
        "study",
        "chores",
        "physical",
        "admin",
        "creative",
        "deep work",
        "meetings",
        "errands",
        "health",
        "finance",
        "social",
        "maintenance",
        "planning",
        "learning",
        "review",
        "writing",
        "coding",
        "reading",
        "household",
        "mindfulness",
        "meditation",
        "breathwork",
        "yoga",
        "journaling",
        "reflection",
        "exercise",
        "workout",
        "gym",
        "running",
        "stretching",
        "walking",
        "hard work"
    )

    private val profilesByTag: Map<String, TagSchedulingProfile> = listOf(
        TagSchedulingProfile("study", TagEnergyDemand.MEDIUM, "@computer", 0.35f, TagPreferredWindow.MORNING, TaskCategory.HARD_WORK),
        TagSchedulingProfile("chores", TagEnergyDemand.LOW, "@home", 0.9f, TagPreferredWindow.FLEXIBLE, TaskCategory.PHYSICAL),
        TagSchedulingProfile("physical", TagEnergyDemand.HIGH, null, 0.45f, TagPreferredWindow.MORNING, TaskCategory.PHYSICAL),
        TagSchedulingProfile("admin", TagEnergyDemand.LOW, "@computer", 0.95f, TagPreferredWindow.MIDDAY, TaskCategory.ROUTINE),
        TagSchedulingProfile("creative", TagEnergyDemand.MEDIUM, null, 0.4f, TagPreferredWindow.EVENING, TaskCategory.CREATIVE),
        TagSchedulingProfile("deep work", TagEnergyDemand.HIGH, "@computer", 0.2f, TagPreferredWindow.MORNING, TaskCategory.HARD_WORK),
        TagSchedulingProfile("meetings", TagEnergyDemand.MEDIUM, "@work", 0.8f, TagPreferredWindow.MIDDAY, TaskCategory.ROUTINE),
        TagSchedulingProfile("errands", TagEnergyDemand.LOW, "@errands", 1.0f, TagPreferredWindow.FLEXIBLE, TaskCategory.PHYSICAL),
        TagSchedulingProfile("health", TagEnergyDemand.MEDIUM, null, 0.6f, TagPreferredWindow.MORNING, TaskCategory.PHYSICAL),
        TagSchedulingProfile("finance", TagEnergyDemand.HIGH, "@computer", 0.3f, TagPreferredWindow.MORNING, TaskCategory.ANALYTICAL),
        TagSchedulingProfile("social", TagEnergyDemand.LOW, null, 0.95f, TagPreferredWindow.EVENING, TaskCategory.ROUTINE),
        TagSchedulingProfile("maintenance", TagEnergyDemand.LOW, "@home", 0.85f, TagPreferredWindow.FLEXIBLE, TaskCategory.PHYSICAL),
        TagSchedulingProfile("planning", TagEnergyDemand.MEDIUM, "@computer", 0.5f, TagPreferredWindow.MORNING, TaskCategory.ROUTINE),
        TagSchedulingProfile("learning", TagEnergyDemand.MEDIUM, "@computer", 0.45f, TagPreferredWindow.MORNING, TaskCategory.HARD_WORK),
        TagSchedulingProfile("review", TagEnergyDemand.LOW, "@computer", 0.8f, TagPreferredWindow.MIDDAY, TaskCategory.ROUTINE),
        TagSchedulingProfile("writing", TagEnergyDemand.MEDIUM, "@computer", 0.4f, TagPreferredWindow.MORNING, TaskCategory.HARD_WORK),
        TagSchedulingProfile("coding", TagEnergyDemand.HIGH, "@computer", 0.25f, TagPreferredWindow.MORNING, TaskCategory.HARD_WORK),
        TagSchedulingProfile("reading", TagEnergyDemand.LOW, null, 0.75f, TagPreferredWindow.EVENING, TaskCategory.ROUTINE),
        TagSchedulingProfile("household", TagEnergyDemand.LOW, "@home", 0.95f, TagPreferredWindow.FLEXIBLE, TaskCategory.PHYSICAL),

        // Smart category tags
        TagSchedulingProfile("mindfulness", TagEnergyDemand.LOW, null, 0.8f, TagPreferredWindow.MORNING, TaskCategory.MINDFULNESS),
        TagSchedulingProfile("meditation", TagEnergyDemand.LOW, null, 0.8f, TagPreferredWindow.MORNING, TaskCategory.MINDFULNESS),
        TagSchedulingProfile("breathwork", TagEnergyDemand.LOW, null, 0.9f, TagPreferredWindow.MORNING, TaskCategory.MINDFULNESS),
        TagSchedulingProfile("yoga", TagEnergyDemand.LOW, null, 0.85f, TagPreferredWindow.MORNING, TaskCategory.MINDFULNESS),
        TagSchedulingProfile("journaling", TagEnergyDemand.LOW, null, 0.9f, TagPreferredWindow.MORNING, TaskCategory.MINDFULNESS),
        TagSchedulingProfile("reflection", TagEnergyDemand.LOW, null, 0.9f, TagPreferredWindow.MORNING, TaskCategory.MINDFULNESS),
        TagSchedulingProfile("exercise", TagEnergyDemand.HIGH, null, 0.5f, TagPreferredWindow.MORNING, TaskCategory.EXERCISE),
        TagSchedulingProfile("workout", TagEnergyDemand.HIGH, null, 0.5f, TagPreferredWindow.MORNING, TaskCategory.EXERCISE),
        TagSchedulingProfile("gym", TagEnergyDemand.HIGH, null, 0.5f, TagPreferredWindow.MORNING, TaskCategory.EXERCISE),
        TagSchedulingProfile("running", TagEnergyDemand.MEDIUM, null, 0.7f, TagPreferredWindow.MORNING, TaskCategory.EXERCISE),
        TagSchedulingProfile("stretching", TagEnergyDemand.LOW, null, 0.85f, TagPreferredWindow.MORNING, TaskCategory.EXERCISE),
        TagSchedulingProfile("walking", TagEnergyDemand.LOW, null, 0.95f, TagPreferredWindow.FLEXIBLE, TaskCategory.EXERCISE),
        TagSchedulingProfile("hard work", TagEnergyDemand.HIGH, "@computer", 0.2f, TagPreferredWindow.MORNING, TaskCategory.HARD_WORK)
    ).associateBy { normalize(it.tag) }

    fun profileFor(tag: String): TagSchedulingProfile? = profilesByTag[normalize(tag)]

    fun profilesFor(tagsCsv: String): List<TagSchedulingProfile> {
        return tagsCsv
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { profileFor(it) }
    }

    private fun normalize(tag: String): String = tag.trim().trimStart('@', '#').lowercase(Locale.getDefault())

    fun tagMatchesKeywords(tag: String, keywords: List<String>): Boolean {
        val words = tag.split(Regex("[^a-zA-Z0-9_-]+")).filter { it.isNotBlank() }
        return keywords.any { kw ->
            if (kw.contains(' ') || kw.contains('-')) {
                tag.equals(kw, ignoreCase = true) || tag.contains(kw)
            } else {
                words.any { it.equals(kw, ignoreCase = true) }
            }
        }
    }
}

fun TaskEntity.determineCategory(): TaskCategory {
    // 0. Respect explicitly persisted category (user override or previously inferred+saved)
    if (!schedulingCategory.isNullOrBlank()) {
        try {
            return TaskCategory.valueOf(schedulingCategory)
        } catch (_: IllegalArgumentException) {
            // Unknown value in DB — fall through to inference
        }
    }

    val tagsList = tags.split(',')
        .map { it.trim().trimStart('@', '#').lowercase(Locale.getDefault()) }
        .filter { it.isNotBlank() }

    // 1. Try to find a matching tag and use its profile category if available
    for (tag in tagsList) {
        val profile = TaskTagSchedulingProfile.profileFor(tag)
        if (profile != null && profile.category != TaskCategory.FLEXIBLE) {
            return profile.category
        }
    }

    // 2. Explicit keywords fallback (catches custom tags not in profilesByTag)
    return when {
        tagsList.any { tag ->
            TaskTagSchedulingProfile.tagMatchesKeywords(tag, listOf("mindfulness", "meditation", "breathwork", "yoga", "reflection", "journaling", "journal", "mental health", "calm"))
        } -> {
            TaskCategory.MINDFULNESS
        }
        tagsList.any { tag ->
            TaskTagSchedulingProfile.tagMatchesKeywords(tag, listOf("exercise", "workout", "gym", "running", "run", "cardio", "fitness", "training", "sports", "stretching", "walking", "swim"))
        } -> {
            TaskCategory.EXERCISE
        }
        tagsList.any { tag ->
            TaskTagSchedulingProfile.tagMatchesKeywords(tag, listOf("hard work", "deep work", "coding", "programming", "dev", "software", "writing", "study", "learning", "finance", "focus", "math", "research", "analysis"))
        } -> {
            TaskCategory.HARD_WORK
        }
        tagsList.any { tag ->
            TaskTagSchedulingProfile.tagMatchesKeywords(tag, listOf("creative", "design", "art", "ui", "ux", "drafting", "music", "painting", "drawing", "brainstorm"))
        } -> {
            TaskCategory.CREATIVE
        }
        tagsList.any { tag ->
            TaskTagSchedulingProfile.tagMatchesKeywords(tag, listOf("physical", "chores", "errands", "household", "cleaning", "maintenance", "laundry", "repairs"))
        } -> {
            TaskCategory.PHYSICAL
        }
        tagsList.any { tag ->
            TaskTagSchedulingProfile.tagMatchesKeywords(tag, listOf("admin", "meetings", "email", "bills", "invoices", "paperwork", "review", "planning", "organize"))
        } -> {
            TaskCategory.ROUTINE
        }
        // 3. Fall back to TaskType mapping
        // Bug 4 Fix (ADR-026): Map MINDFULNESS to MINDFULNESS and GENERAL to FLEXIBLE
        else -> when (taskType) {
            com.neuroflow.app.domain.model.TaskType.ANALYTICAL -> TaskCategory.ANALYTICAL
            com.neuroflow.app.domain.model.TaskType.CREATIVE -> TaskCategory.CREATIVE
            com.neuroflow.app.domain.model.TaskType.ADMIN -> TaskCategory.ROUTINE
            com.neuroflow.app.domain.model.TaskType.PHYSICAL -> TaskCategory.PHYSICAL
            com.neuroflow.app.domain.model.TaskType.GENERAL -> TaskCategory.FLEXIBLE
            com.neuroflow.app.domain.model.TaskType.MINDFULNESS -> TaskCategory.MINDFULNESS
        }
    }
}

fun TaskEntity.isNonWorkOrErrandTask(): Boolean {
    val category = determineCategory()
    if (category in listOf(TaskCategory.MINDFULNESS, TaskCategory.EXERCISE, TaskCategory.PHYSICAL)) {
        return true
    }

    val tagsList = tags.split(',')
        .map { it.trim().trimStart('@', '#').lowercase(Locale.getDefault()) }
        .filter { it.isNotBlank() }

    val nonWorkKeywords = listOf(
        "errand", "errands", "chore", "chores", "personal", "life", "home",
        "household", "cleaning", "maintenance", "shopping", "groceries", "health",
        "routine", "leisure", "break", "family", "social", "meditation", "mindfulness",
        "exercise", "workout", "gym", "running", "walking", "stretching", "non-work",
        "not work", "not-work", "off-hours"
    )

    return tagsList.any { tag -> TaskTagSchedulingProfile.tagMatchesKeywords(tag, nonWorkKeywords) }
}
