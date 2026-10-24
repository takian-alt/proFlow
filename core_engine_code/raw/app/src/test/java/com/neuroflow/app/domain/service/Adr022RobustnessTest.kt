package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.PhysiologicalEventLog
import com.neuroflow.app.data.local.PhysiologicalEventType
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.sync.fromBackupMap
import com.neuroflow.app.data.sync.toBackupMap
import com.neuroflow.app.domain.model.computeEnergyUiState
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class Adr022RobustnessTest {

    @Test
    fun testQuickPhysiologicalLogsMergedIntoComputeEnergyUiState() {
        val circadianService = DefaultCircadianEnergyService()
        val zoneId = ZoneId.systemDefault()
        val now = System.currentTimeMillis()

        val jsonArray = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "MEAL")
                put("timestampMillis", now - 60 * 60_000L)
                put("durationMinutes", 45)
                put("intensity", 1.0)
            })
            put(JSONObject().apply {
                put("type", "WORKOUT")
                put("timestampMillis", now - 30 * 60_000L)
                put("durationMinutes", 45)
                put("intensity", 1.0)
            })
            put(JSONObject().apply {
                put("type", "NAP")
                put("timestampMillis", now - 90 * 60_000L)
                put("durationMinutes", 20)
                put("intensity", 1.0)
            })
        }

        val prefs = UserPreferences(
            wakeUpHour = 7,
            sleepHour = 23,
            quickPhysiologicalLogsJson = jsonArray.toString()
        )

        val (snapshot, energyUi) = computeEnergyUiState(
            circadianEnergyService = circadianService,
            sleepTelemetryRepository = null,
            prefs = prefs,
            nowMillis = now
        )

        assertNotNull(snapshot)
        assertNotNull(energyUi)

        // Energy value must be bounded within [0, 100]
        assertTrue("Available energy in valid range", energyUi.availableEnergy in 0..100)
    }

    @Test
    fun testCloudBackupRoundTripPreservesAllAdr022Fields() {
        val originalPrefs = UserPreferences(
            adaptiveSystemDirective = "Enforce 2-minute doorway steps on coding tasks",
            weeklyAiAuditJson = """{"postponedCount": 4, "prescriptions": ["Break down coding tasks"]}""",
            stimulantLogsJson = """[{"id":"c1","type":"CAFFEINE","timestampMillis":1000000,"doseMg":100.0}]""",
            quickPhysiologicalLogsJson = """[{"type":"MEAL","timestampMillis":2000000,"durationMinutes":45,"intensity":1.0}]""",
            quizChronotype = "MODERATE_MORNING",
            manualChronotype = "DEFINITE_MORNING",
            meqScore = 65
        )

        val backupMap = originalPrefs.toBackupMap()

        assertEquals("Enforce 2-minute doorway steps on coding tasks", backupMap["adaptiveSystemDirective"])
        assertTrue(backupMap["weeklyAiAuditJson"]!!.contains("postponedCount"))
        assertTrue(backupMap["stimulantLogsJson"]!!.contains("CAFFEINE"))
        assertTrue(backupMap["quickPhysiologicalLogsJson"]!!.contains("MEAL"))
        assertEquals("MODERATE_MORNING", backupMap["quizChronotype"])
        assertEquals("DEFINITE_MORNING", backupMap["manualChronotype"])
        assertEquals("65", backupMap["meqScore"])

        // Test deserialization round-trip
        val blankPrefs = UserPreferences()
        val restoredPrefs = blankPrefs.fromBackupMap(backupMap)

        assertEquals(originalPrefs.adaptiveSystemDirective, restoredPrefs.adaptiveSystemDirective)
        assertEquals(originalPrefs.weeklyAiAuditJson, restoredPrefs.weeklyAiAuditJson)
        assertEquals(originalPrefs.stimulantLogsJson, restoredPrefs.stimulantLogsJson)
        assertEquals(originalPrefs.quickPhysiologicalLogsJson, restoredPrefs.quickPhysiologicalLogsJson)
        assertEquals(originalPrefs.quizChronotype, restoredPrefs.quizChronotype)
        assertEquals(originalPrefs.manualChronotype, restoredPrefs.manualChronotype)
        assertEquals(originalPrefs.meqScore, restoredPrefs.meqScore)
    }
}
