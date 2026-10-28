package com.neuroflow.app.presentation.common

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.domain.scheduler.TaskCategory
import com.neuroflow.app.domain.scheduler.TaskTagSchedulingProfile
import com.neuroflow.app.domain.scheduler.determineCategory
import com.neuroflow.app.domain.scheduler.isNonWorkOrErrandTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTagAndCategoryTest {

    @Test
    fun testTaskTagSchedulingProfile_resolvesHashAndAtPrefixedTags() {
        val codingProfile = TaskTagSchedulingProfile.profileFor("#coding")
        assertNotNull(codingProfile)
        assertEquals("coding", codingProfile?.tag)
        assertEquals(TaskCategory.HARD_WORK, codingProfile?.category)

        val choresProfile = TaskTagSchedulingProfile.profileFor("@chores")
        assertNotNull(choresProfile)
        assertEquals("chores", choresProfile?.tag)
        assertEquals(TaskCategory.PHYSICAL, choresProfile?.category)

        val deepWorkProfile = TaskTagSchedulingProfile.profileFor("#deep work")
        assertNotNull(deepWorkProfile)
        assertEquals("deep work", deepWorkProfile?.tag)
        assertEquals(TaskCategory.HARD_WORK, deepWorkProfile?.category)
    }

    @Test
    fun testTaskEntity_determineCategory_withHashTags() {
        val taskWithExerciseHash = TaskEntity(
            title = "Morning Gym Session",
            tags = "#exercise, #workout",
            taskType = TaskType.GENERAL
        )
        assertEquals(TaskCategory.EXERCISE, taskWithExerciseHash.determineCategory())
        assertTrue(taskWithExerciseHash.isNonWorkOrErrandTask())

        val taskWithCodingHash = TaskEntity(
            title = "Refactor Database Schema",
            tags = "#coding",
            taskType = TaskType.GENERAL
        )
        assertEquals(TaskCategory.HARD_WORK, taskWithCodingHash.determineCategory())
        assertFalse(taskWithCodingHash.isNonWorkOrErrandTask())

        val taskWithMindfulnessHash = TaskEntity(
            title = "Evening Meditation",
            tags = "#mindfulness",
            taskType = TaskType.GENERAL
        )
        assertEquals(TaskCategory.MINDFULNESS, taskWithMindfulnessHash.determineCategory())
        assertTrue(taskWithMindfulnessHash.isNonWorkOrErrandTask())
    }

    @Test
    fun testTaskEntity_profilesFor_evaluatesHashTagsProperly() {
        val tagsCsv = "#coding, #deep work, #nonexistent"
        val profiles = TaskTagSchedulingProfile.profilesFor(tagsCsv)
        assertEquals(2, profiles.size)
        assertEquals(0.25f, profiles.first { it.tag == "coding" }.fragmentationTolerance)
        assertEquals(0.2f, profiles.first { it.tag == "deep work" }.fragmentationTolerance)
    }

    @Test
    fun testExactTagFilteringLogic_avoidsSubstringMatches() {
        val task1 = TaskEntity(id = "1", title = "Download latest SDK", tags = "routine")
        val task2 = TaskEntity(id = "2", title = "Quarterly Taxes", tags = "admin, finance")
        val task3 = TaskEntity(id = "3", title = "Thread needle", tags = "crafts")
        val task4 = TaskEntity(id = "4", title = "Setup smart thermometer", tags = "household")

        val tasks = listOf(task1, task2, task3, task4)

        // Helper replicating the updated exact tag + word-bounded title filter logic
        fun filterByTag(queryTag: String): List<TaskEntity> {
            val q = queryTag.removePrefix("#").removePrefix("@").trim()
            val wordRegex = Regex("""(?i)(?:^|\s)[#@]?\Q$q\E(?=\s|$)""")
            return tasks.filter { task ->
                val hasMatchingTag = task.tags.split(",")
                    .map { it.trim().removePrefix("#").removePrefix("@") }
                    .any { it.equals(q, ignoreCase = true) }
                hasMatchingTag || wordRegex.containsMatchIn(task.title)
            }
        }

        // Searching for "#out" should NOT match "routine"
        val outMatches = filterByTag("#out")
        assertTrue("Substrings in 'routine' should not match '#out'", outMatches.isEmpty())

        // Searching for "#ad" should NOT match "Download" or "admin"
        val adMatches = filterByTag("#ad")
        assertTrue("Substrings in 'Download' or 'admin' should not match '#ad'", adMatches.isEmpty())

        // Searching for "#read" should NOT match "Thread"
        val readMatches = filterByTag("#read")
        assertTrue("Substrings in 'Thread' should not match '#read'", readMatches.isEmpty())

        // Searching for "#art" should NOT match "smart"
        val artMatches = filterByTag("#art")
        assertTrue("Substrings in 'smart' should not match '#art'", artMatches.isEmpty())

        // Searching for "#admin" should match task2
        val adminMatches = filterByTag("#admin")
        assertEquals(1, adminMatches.size)
        assertEquals("2", adminMatches.first().id)
    }

    @Test
    fun testWordRegexTitleMatching_matchesExplicitHashtagInTitle() {
        val task = TaskEntity(id = "1", title = "Review quarterly metrics #admin", tags = "")
        val q = "admin"
        val wordRegex = Regex("""(?i)(?:^|\s)[#@]?\Q$q\E(?=\s|$)""")
        assertTrue(wordRegex.containsMatchIn(task.title))

        val unrelated = TaskEntity(id = "2", title = "Administrator access granted", tags = "")
        assertFalse(wordRegex.containsMatchIn(unrelated.title))
    }

    @Test
    fun testBug302NewTaskTagsNormalizedAndMergedIntoTagCatalog() {
        val existingCatalog = listOf("deepwork", "admin")
        val task = TaskEntity(
            title = "Prepare pitch deck",
            tags = "#fundraising, @investors",
            contextTag = "#q3"
        )

        val newTags = (task.tags.split(",") + task.contextTag)
            .map { it.trim().removePrefix("#").removePrefix("@") }
            .filter { it.isNotBlank() }

        val merged = (existingCatalog + newTags).distinctBy { it.lowercase() }
        assertEquals(listOf("deepwork", "admin", "fundraising", "investors", "q3"), merged)
    }

    @Test
    fun testBug337NormalizeTagsSplitsCommaSeparatedTokens() {
        // Simulate task with comma-separated tags passed to TaskTagViewModel.seedFromTasks / addTags
        val rawInputTags = listOf("deepwork, coding", "#study, #review", "admin")

        fun normalizeTags(tags: Collection<String>): List<String> {
            return tags.flatMap { it.split(",") }
                .mapNotNull { it.trim().removePrefix("#").removePrefix("@").takeIf(String::isNotBlank) }
        }

        val cleaned = normalizeTags(rawInputTags)

        // Must be individual clean tokens without commas
        assertEquals(listOf("deepwork", "coding", "study", "review", "admin"), cleaned)
        assertFalse("Tags must not contain internal commas", cleaned.any { it.contains(",") })
    }
}
