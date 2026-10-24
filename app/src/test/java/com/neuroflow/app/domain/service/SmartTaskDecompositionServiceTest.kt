package com.neuroflow.app.domain.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SmartTaskDecompositionServiceTest {

    private lateinit var service: DefaultSmartTaskDecompositionService

    @Before
    fun setup() {
        service = DefaultSmartTaskDecompositionService()
    }

    @Test
    fun testArchetypeClassification() {
        assertEquals(
            TaskArchetype.CODING_TECH,
            DefaultSmartTaskDecompositionService.classifyArchetype("Fix authentication endpoint bug")
        )
        assertEquals(
            TaskArchetype.CODING_TECH,
            DefaultSmartTaskDecompositionService.classifyArchetype("Refactor database SQL schema")
        )
        assertEquals(
            TaskArchetype.WRITING_RESEARCH,
            DefaultSmartTaskDecompositionService.classifyArchetype("Draft blog post about circadian rhythm")
        )
        assertEquals(
            TaskArchetype.STUDY_ACADEMIC,
            DefaultSmartTaskDecompositionService.classifyArchetype("Study chapter 4 for biology exam")
        )
        assertEquals(
            TaskArchetype.CHORE_MAINTENANCE,
            DefaultSmartTaskDecompositionService.classifyArchetype("Clean kitchen and do laundry")
        )
        assertEquals(
            TaskArchetype.ADMIN_LOGISTICS,
            DefaultSmartTaskDecompositionService.classifyArchetype("File tax return and submit invoice")
        )
        assertEquals(
            TaskArchetype.GENERAL,
            DefaultSmartTaskDecompositionService.classifyArchetype("Plan weekend family trip")
        )
    }

    @Test
    fun testLocalDecompositionMeetsActionabilityAndFrictionInvariants() {
        val codingSteps = service.decomposeTaskLocally("Implement OAuth login", 60)
        assertTrue("Coding archetype should produce 3-4 steps", codingSteps.size in 3..4)
        assertTrue("First step must have low activation friction (<= 10m)", codingSteps.first().estimatedMinutes <= 10)
        assertTrue("Steps should preserve task context", codingSteps.any { it.title.contains("Implement OAuth login") })

        val writingSteps = service.decomposeTaskLocally("Write quarterly performance report", 90)
        assertTrue("Writing archetype should produce 3-4 steps", writingSteps.size in 3..4)
        assertTrue("First step <= 10m", writingSteps.first().estimatedMinutes <= 10)
        assertTrue("Context preserved", writingSteps.any { it.title.contains("Write quarterly performance report") })

        val choreSteps = service.decomposeTaskLocally("Clean garage and organize tools", 30)
        assertTrue("Chore archetype should produce 3-4 steps", choreSteps.size in 3..4)
        assertTrue("First step <= 10m", choreSteps.first().estimatedMinutes <= 10)

        val generalSteps = service.decomposeTaskLocally("Misc random task", 45)
        assertTrue("General archetype should produce 3-4 steps", generalSteps.size in 3..4)
        assertTrue("First step <= 10m", generalSteps.first().estimatedMinutes <= 10)
    }

    @Test
    fun testArchetypeDecompositionPhysicalDoorwayStep() {
        val steps = service.decomposeTaskLocally("Fix SQL migration bug", 45)
        assertEquals(4, steps.size)
        assertTrue("Step 1 must be doorway action", steps[0].title.startsWith("Open IDE, checkout branch"))
        assertTrue("Step 1 estimated minutes must be <= 4", steps[0].estimatedMinutes <= 4)
    }

    @Test
    fun testParseGeminiResponseStandardJson() {
        val json = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "[{\"title\":\"Review rubric\",\"estimatedMinutes\":5},{\"title\":\"Write section 1\",\"estimatedMinutes\":20}]"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val steps = DefaultSmartTaskDecompositionService.parseGeminiResponse(json)
        assertEquals(2, steps.size)
        assertEquals("Review rubric", steps[0].title)
        assertEquals(5, steps[0].estimatedMinutes)
        assertEquals("Write section 1", steps[1].title)
        assertEquals(20, steps[1].estimatedMinutes)
    }

    @Test
    fun testParseGeminiResponseMarkdownCodeFence() {
        val json = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "```json\n[\n  {\"title\": \"Step A\", \"estimatedMinutes\": 10},\n  {\"title\": \"Step B\", \"estimatedMinutes\": 15}\n]\n```"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val steps = DefaultSmartTaskDecompositionService.parseGeminiResponse(json)
        assertEquals(2, steps.size)
        assertEquals("Step A", steps[0].title)
        assertEquals(10, steps[0].estimatedMinutes)
        assertEquals("Step B", steps[1].title)
        assertEquals(15, steps[1].estimatedMinutes)
    }

    @Test
    fun testParseGeminiResponseMalformedOrEmptyReturnsEmptyList() {
        val malformed = """{"candidates": []}"""
        val emptySteps = DefaultSmartTaskDecompositionService.parseGeminiResponse(malformed)
        assertTrue(emptySteps.isEmpty())

        val invalidText = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "I am unable to break down this task."
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()
        val textSteps = DefaultSmartTaskDecompositionService.parseGeminiResponse(invalidText)
        assertTrue(textSteps.isEmpty())
    }

    @Test
    fun testDecomposeTaskFallsBackToLocalWhenAiDisabledOrKeyBlank() = runBlocking {
        val stepsNoAi = service.decomposeTask("Fix bug in billing API", 45, apiKey = "test-key", useAi = false)
        assertTrue(stepsNoAi.isNotEmpty())
        assertTrue(stepsNoAi.any { it.title.contains("Fix bug in billing API") })

        val stepsBlankKey = service.decomposeTask("Fix bug in billing API", 45, apiKey = "", useAi = true)
        assertTrue(stepsBlankKey.isNotEmpty())
        assertTrue(stepsBlankKey.any { it.title.contains("Fix bug in billing API") })
    }
}
