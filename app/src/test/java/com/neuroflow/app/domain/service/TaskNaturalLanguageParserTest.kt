package com.neuroflow.app.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskNaturalLanguageParserTest {

    private lateinit var parser: TaskNaturalLanguageParser

    @Before
    fun setUp() {
        parser = DefaultTaskNaturalLanguageParser()
    }

    @Test
    fun testParseDurationTokens() {
        val result1 = parser.parse("Call accountant 45m")
        assertEquals("Call accountant", result1.cleanTitle)
        assertEquals(45, result1.estimatedMinutes)
        assertTrue(result1.isDurationExplicit)

        val result2 = parser.parse("Study chapter 2 hours")
        assertEquals("Study chapter", result2.cleanTitle)
        assertEquals(120, result2.estimatedMinutes)
        assertTrue(result2.isDurationExplicit)

        val result3 = parser.parse("Weekly sync half hour")
        assertEquals("Weekly sync", result3.cleanTitle)
        assertEquals(30, result3.estimatedMinutes)
        assertTrue(result3.isDurationExplicit)
    }

    @Test
    fun testParseFrogBadge() {
        val result1 = parser.parse("🐸 File quarterly taxes")
        assertEquals("File quarterly taxes", result1.cleanTitle)
        assertTrue(result1.isFrog)

        val result2 = parser.parse("Annual audit #frog")
        assertEquals("Annual audit", result2.cleanTitle)
        assertTrue(result2.isFrog)

        val result3 = parser.parse("Routine documentation")
        assertFalse(result3.isFrog)
    }

    @Test
    fun testParseEnergyAndCategory() {
        val result1 = parser.parse("⚡ Deep code refactoring")
        assertTrue(result1.energyDemand == EnergyDemand.HIGH)
        assertEquals(TaskCategory.DEEP_WORK, result1.category)

        val result2 = parser.parse("Clean mailbox low energy")
        assertTrue(result2.energyDemand == EnergyDemand.LOW)
        assertEquals(TaskCategory.ADMIN, result2.category)

        val result3 = parser.parse("Morning gym workout 1h")
        assertEquals(TaskCategory.EXERCISE, result3.category)
        assertEquals(60, result3.estimatedMinutes)
    }
}
