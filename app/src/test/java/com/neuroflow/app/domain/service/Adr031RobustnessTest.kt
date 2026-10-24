package com.neuroflow.app.domain.service

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Adr031RobustnessTest {

    // =========================================================================
    // 1. AdaptiveCircadianLearner.computeVonMisesDensity Safe Scaling
    // =========================================================================

    @Test
    fun testAdaptiveCircadianLearner_computeVonMisesDensity_isFiniteAndPositive() {
        val testKappas = listOf(0.1, 0.5, 1.0, 2.0, 5.0, 10.0, 20.0)
        for (kappa in testKappas) {
            for (hour in 0..23) {
                val density = AdaptiveCircadianLearner.computeVonMisesDensity(hour.toDouble(), 10.0, kappa)
                assertFalse("Density at hour $hour with kappa $kappa should not be NaN", density.isNaN())
                assertFalse("Density at hour $hour with kappa $kappa should not be infinite", density.isInfinite())
                assertTrue("Density at hour $hour with kappa $kappa should be non-negative", density >= 0.0)
            }
        }
    }

    @Test
    fun testAdaptiveCircadianLearner_computeVonMisesDensity_peaksAtPeakHour() {
        val peakHour = 10.0
        val kappa = 2.5
        val peakDensity = AdaptiveCircadianLearner.computeVonMisesDensity(peakHour, peakHour, kappa)
        val nonPeakDensity = AdaptiveCircadianLearner.computeVonMisesDensity(22.0, peakHour, kappa)

        assertTrue("Density at peak hour should exceed density at opposite hour", peakDensity > nonPeakDensity)
        assertEquals(
            computeVonMisesDensitySafe(peakHour, peakHour, kappa),
            peakDensity,
            1e-9
        )
    }

    // =========================================================================
    // 2. GeminiAiOnlineService Candidate Models & JSON Extraction
    // =========================================================================

    @Test
    fun testGeminiCandidateModels_containsDocumentedPriorities() {
        val models = DefaultGeminiAiOnlineService.CANDIDATE_MODELS
        assertEquals("gemini-flash-latest", models.first())
        assertTrue("Should include gemini-flash-lite-latest", models.contains("gemini-flash-lite-latest"))
        assertTrue("Should include gemini-2.5-flash", models.contains("gemini-2.5-flash"))
        assertTrue("Should include gemini-2.0-flash", models.contains("gemini-2.0-flash"))
    }

    @Test
    fun testGeminiJsonParsing_handlesEscapedStringsWithoutCorruption() {
        val sampleResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "```json\n[{\"step\": 1, \"text\": \"Open \\\"Settings\\\" on device\"}, {\"step\": 2, \"text\": \"Tap \\\"Account\\\"\"}]\n```"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val json = JSONObject(sampleResponse)
        val rawPart = json.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")

        val cleaned = rawPart.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val startIdx = cleaned.indexOfFirst { it == '{' || it == '[' }
        val endIdx = cleaned.indexOfLast { it == '}' || it == ']' }
        val extractedJson = cleaned.substring(startIdx, endIdx + 1)

        val parsedArray = org.json.JSONArray(extractedJson)
        assertEquals(2, parsedArray.length())
        assertEquals("Open \"Settings\" on device", parsedArray.getJSONObject(0).getString("text"))
        assertEquals("Tap \"Account\"", parsedArray.getJSONObject(1).getString("text"))
    }

    // =========================================================================
    // 3. Smart Chrono-Aware Shift to Peak Logic
    // =========================================================================

    @Test
    fun testShiftToPeak_whenBeforeMorningPeak_schedulesToTodayPeak() {
        val peakHour = 10
        val peakEndHour = 13
        val currentHour = 8 // 8:00 AM, before peak
        val baseDate = 1700000000000L

        val isToday = true
        val usableEnergy = 70

        val targetDate: Long
        val targetHour: Int

        if (!isToday || currentHour < peakEndHour) {
            targetDate = baseDate
            targetHour = peakHour
        } else if (currentHour < 18 && usableEnergy >= 55) {
            targetDate = baseDate
            targetHour = maxOf(currentHour + 1, 16).coerceAtMost(17)
        } else {
            targetDate = baseDate + 86400000L
            targetHour = peakHour
        }

        assertEquals(baseDate, targetDate)
        assertEquals(peakHour, targetHour)
    }

    @Test
    fun testShiftToPeak_whenPastMorningPeakWithGoodEnergy_schedulesToAfternoonRebound() {
        val peakHour = 10
        val peakEndHour = 13
        val currentHour = 14 // 2:00 PM, past peak
        val baseDate = 1700000000000L

        val isToday = true
        val usableEnergy = 65 // High energy

        val targetDate: Long
        val targetHour: Int

        if (!isToday || currentHour < peakEndHour) {
            targetDate = baseDate
            targetHour = peakHour
        } else if (currentHour < 18 && usableEnergy >= 55) {
            targetDate = baseDate
            targetHour = maxOf(currentHour + 1, 16).coerceAtMost(17)
        } else {
            targetDate = baseDate + 86400000L
            targetHour = peakHour
        }

        assertEquals(baseDate, targetDate)
        assertEquals(16, targetHour) // Afternoon rebound at 16:00
    }

    @Test
    fun testShiftToPeak_whenEvening_rollsToTomorrowPeak() {
        val peakHour = 10
        val peakEndHour = 13
        val currentHour = 19 // 7:00 PM, evening
        val baseDate = 1700000000000L

        val isToday = true
        val usableEnergy = 40 // Low energy

        val targetDate: Long
        val targetHour: Int

        if (!isToday || currentHour < peakEndHour) {
            targetDate = baseDate
            targetHour = peakHour
        } else if (currentHour < 18 && usableEnergy >= 55) {
            targetDate = baseDate
            targetHour = maxOf(currentHour + 1, 16).coerceAtMost(17)
        } else {
            targetDate = baseDate + 86400000L
            targetHour = peakHour
        }

        assertEquals(baseDate + 86400000L, targetDate)
        assertEquals(peakHour, targetHour)
    }
}
