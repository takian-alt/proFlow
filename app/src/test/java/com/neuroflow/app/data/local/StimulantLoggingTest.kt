package com.neuroflow.app.data.local

import com.neuroflow.app.domain.service.PharmacokineticStimulantCalculator
import com.neuroflow.app.domain.service.StimulantDoseBout
import com.neuroflow.app.domain.service.StimulantType
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StimulantLoggingTest {

    @Test
    fun testParseStimulantLogsEmptyAndInvalid() {
        assertEquals(emptyList<StimulantDoseBout>(), parseStimulantLogs(null))
        assertEquals(emptyList<StimulantDoseBout>(), parseStimulantLogs(""))
        assertEquals(emptyList<StimulantDoseBout>(), parseStimulantLogs("invalid json"))
        assertEquals(emptyList<StimulantDoseBout>(), parseStimulantLogs("[]"))
    }

    @Test
    fun testParseAllFourStimulantTypes() {
        val now = 1_700_000_000_000L
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("type", StimulantType.CAFFEINE.name)
                put("doseMg", 100.0)
                put("timestampMillis", now)
            })
            put(JSONObject().apply {
                put("type", StimulantType.GREEN_TEA_L_THEANINE.name)
                put("doseMg", 50.0)
                put("timestampMillis", now - 3600_000L)
            })
            put(JSONObject().apply {
                put("type", StimulantType.PRESCRIPTION_STIMULANT_IR.name)
                put("doseMg", 10.0)
                put("timestampMillis", now - 7200_000L)
            })
            put(JSONObject().apply {
                put("type", StimulantType.PRESCRIPTION_STIMULANT_XR.name)
                put("doseMg", 20.0)
                put("timestampMillis", now - 10800_000L)
            })
        }

        val parsed = parseStimulantLogs(arr.toString())
        assertEquals(4, parsed.size)
        assertEquals(StimulantType.CAFFEINE, parsed[0].type)
        assertEquals(100.0, parsed[0].doseMg, 0.001)

        assertEquals(StimulantType.GREEN_TEA_L_THEANINE, parsed[1].type)
        assertEquals(50.0, parsed[1].doseMg, 0.001)

        assertEquals(StimulantType.PRESCRIPTION_STIMULANT_IR, parsed[2].type)
        assertEquals(10.0, parsed[2].doseMg, 0.001)

        assertEquals(StimulantType.PRESCRIPTION_STIMULANT_XR, parsed[3].type)
        assertEquals(20.0, parsed[3].doseMg, 0.001)
    }

    @Test
    fun testUndoStimulantDosePopsHead() {
        val now = 1_700_000_000_000L
        val arr = JSONArray().apply {
            put(JSONObject().apply {
                put("type", StimulantType.PRESCRIPTION_STIMULANT_XR.name)
                put("doseMg", 20.0)
                put("timestampMillis", now)
            })
            put(JSONObject().apply {
                put("type", StimulantType.CAFFEINE.name)
                put("doseMg", 100.0)
                put("timestampMillis", now - 3600_000L)
            })
        }

        // Simulate undo
        val list = mutableListOf<JSONObject>()
        for (i in 0 until arr.length()) list.add(arr.getJSONObject(i))
        assertEquals(2, list.size)

        val removed = list.removeAt(0)
        assertEquals(StimulantType.PRESCRIPTION_STIMULANT_XR.name, removed.getString("type"))
        assertEquals(1, list.size)

        val remainingJson = JSONArray().apply { list.forEach { put(it) } }.toString()
        val parsed = parseStimulantLogs(remainingJson)
        assertEquals(1, parsed.size)
        assertEquals(StimulantType.CAFFEINE, parsed[0].type)
    }

    @Test
    fun testActivePlasmaEvaluation() {
        val now = 1_700_000_000_000L
        val dose = StimulantDoseBout(
            timestampMillis = now - 3600_000L, // 1 hour ago
            doseMg = 100.0,
            type = StimulantType.CAFFEINE
        )

        val effect = PharmacokineticStimulantCalculator.evaluateAt(now, listOf(dose))
        // Caffeine half-life is 5 hours, so after 1 hour remaining active mg should be ~87 mg
        assertTrue("Active plasma should be positive", effect.activeEquivalentMg > 70.0)
        assertTrue("Active plasma should not exceed dose", effect.activeEquivalentMg <= 100.0)
        assertTrue("Acute alertness boost should be active", effect.acuteAlertnessBoostPoints > 0.0)
    }
}
