package com.neuroflow.app.domain.scheduler

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

data class TagSchedulingProfile(
    val tag: String,
    val energyDemand: TagEnergyDemand,
    val preferredContext: String?,
    val fragmentationTolerance: Float,
    val preferredWindow: TagPreferredWindow
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
        "household"
    )

    private val profilesByTag: Map<String, TagSchedulingProfile> = listOf(
        TagSchedulingProfile("study", TagEnergyDemand.MEDIUM, "@computer", 0.35f, TagPreferredWindow.MORNING),
        TagSchedulingProfile("chores", TagEnergyDemand.LOW, "@home", 0.9f, TagPreferredWindow.FLEXIBLE),
        TagSchedulingProfile("physical", TagEnergyDemand.HIGH, null, 0.45f, TagPreferredWindow.MORNING),
        TagSchedulingProfile("admin", TagEnergyDemand.LOW, "@computer", 0.95f, TagPreferredWindow.MIDDAY),
        TagSchedulingProfile("creative", TagEnergyDemand.MEDIUM, null, 0.4f, TagPreferredWindow.EVENING),
        TagSchedulingProfile("deep work", TagEnergyDemand.HIGH, "@computer", 0.2f, TagPreferredWindow.MORNING),
        TagSchedulingProfile("meetings", TagEnergyDemand.MEDIUM, "@work", 0.8f, TagPreferredWindow.MIDDAY),
        TagSchedulingProfile("errands", TagEnergyDemand.LOW, "@errands", 1.0f, TagPreferredWindow.FLEXIBLE),
        TagSchedulingProfile("health", TagEnergyDemand.MEDIUM, null, 0.6f, TagPreferredWindow.MORNING),
        TagSchedulingProfile("finance", TagEnergyDemand.HIGH, "@computer", 0.3f, TagPreferredWindow.MORNING),
        TagSchedulingProfile("social", TagEnergyDemand.LOW, null, 0.95f, TagPreferredWindow.EVENING),
        TagSchedulingProfile("maintenance", TagEnergyDemand.LOW, "@home", 0.85f, TagPreferredWindow.FLEXIBLE),
        TagSchedulingProfile("planning", TagEnergyDemand.MEDIUM, "@computer", 0.5f, TagPreferredWindow.MORNING),
        TagSchedulingProfile("learning", TagEnergyDemand.MEDIUM, "@computer", 0.45f, TagPreferredWindow.MORNING),
        TagSchedulingProfile("review", TagEnergyDemand.LOW, "@computer", 0.8f, TagPreferredWindow.MIDDAY),
        TagSchedulingProfile("writing", TagEnergyDemand.MEDIUM, "@computer", 0.4f, TagPreferredWindow.MORNING),
        TagSchedulingProfile("coding", TagEnergyDemand.HIGH, "@computer", 0.25f, TagPreferredWindow.MORNING),
        TagSchedulingProfile("reading", TagEnergyDemand.LOW, null, 0.75f, TagPreferredWindow.EVENING),
        TagSchedulingProfile("household", TagEnergyDemand.LOW, "@home", 0.95f, TagPreferredWindow.FLEXIBLE)
    ).associateBy { normalize(it.tag) }

    fun profileFor(tag: String): TagSchedulingProfile? = profilesByTag[normalize(tag)]

    fun profilesFor(tagsCsv: String): List<TagSchedulingProfile> {
        return tagsCsv
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { profileFor(it) }
    }

    private fun normalize(tag: String): String = tag.trim().lowercase(Locale.getDefault())
}
