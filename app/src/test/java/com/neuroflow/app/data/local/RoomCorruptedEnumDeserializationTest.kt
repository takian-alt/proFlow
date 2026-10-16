package com.neuroflow.app.data.local

import androidx.room.Room
import com.neuroflow.app.data.local.entity.ContractOutcome
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RoomCorruptedEnumDeserializationTest {

    private lateinit var db: NeuroFlowDatabase

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, NeuroFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testRoomDeserializesCorruptedTaskEnumsWithoutCrashing() {
        runBlocking {
            // Direct raw SQL insert with unknown / forward-compatible / garbage enum values in SQLite
            db.openHelper.writableDatabase.execSQL("""
                INSERT INTO tasks (
                    id, title, description, quadrant, priority, recurrence, status,
                    energyLevel, taskType, tags, reminderFlags, impactScore, valueScore,
                    effortScore, blockingTaskIds, dependsOnTaskIds, waitingFor,
                    totalTimeTrackedMinutes, sessionCount, isScheduleLocked,
                    estimatedDurationMinutes, recurrenceIntervalDays, createdAt, updatedAt,
                    contextTag, ifThenPlan, isHabitual, habitStreak, isFrog, postponeCount,
                    focusModePoints, enjoymentScore, isPublicCommitment, isAnxietyTask,
                    goalRiskLevel, woopPromptShown, distractionScore, isAutoScheduled, plannedBlockMinutes
                ) VALUES (
                    'corrupted_task_1', 'Corrupted Task', 'Desc', 'FUTURE_QUADRANT_AI', 'URGENT_CRITICAL', 'TRIWEEKLY', 'VAULT_ARCHIVED',
                    'SUPERNOVA_ENERGY', 'QUANTUM_SOLVING', '', 0, 50, 50,
                    50, '', '', '',
                    0.0, 0, 0,
                    30, 1, 10000, 10000,
                    '', '', 0, 0, 0, 0,
                    0, 50, 0, 0,
                    0, 0, -1.0, 0, 0
                )
            """.trimIndent())

            // Read through Room DAO
            val task = db.taskDao().getById("corrupted_task_1")

            assertNotNull("Task must be successfully retrieved despite unknown enums in DB", task)
            task?.let {
                assertEquals("Corrupted quadrant should fall back to SCHEDULE", Quadrant.SCHEDULE, it.quadrant)
                assertEquals("Corrupted priority should fall back to MEDIUM", Priority.MEDIUM, it.priority)
                assertEquals("Corrupted recurrence should fall back to NONE", Recurrence.NONE, it.recurrence)
                assertEquals("Corrupted status should fall back to ACTIVE", TaskStatus.ACTIVE, it.status)
                assertEquals("Corrupted energyLevel should fall back to MEDIUM", EnergyLevel.MEDIUM, it.energyLevel)
                assertEquals("Corrupted taskType should fall back to ANALYTICAL", TaskType.ANALYTICAL, it.taskType)
            }
        }
    }

    @Test
    fun testRoomDeserializesCorruptedUlyssesContractOutcomeWithoutCrashing() {
        runBlocking {
            db.openHelper.writableDatabase.execSQL("""
                INSERT INTO ulysses_contracts (
                    id, taskId, deadlineAt, consequence, outcome, createdAt
                ) VALUES (
                    'contract_1', 'task_1', 20000, 'Donate to charity', 'INVALID_OUTCOME_VOID', 10000
                )
            """.trimIndent())

            val contract = db.ulyssesContractDao().getById("contract_1")
            assertNotNull(contract)
            assertNull("Corrupted ContractOutcome should deserialize to null", contract?.outcome)
        }
    }

    @Test
    fun testRoomDeserializesCorruptedUnlockCodeTierWithoutCrashing() {
        runBlocking {
            db.openHelper.writableDatabase.execSQL("""
                INSERT INTO unlock_codes (
                    id, encryptedCode, tier, sessionId, isUsed, createdAt
                ) VALUES (
                    'code_1', 'enc_123', 'ULTRA_PLATINUM_TIER', 'session_1', 0, 10000
                )
            """.trimIndent())

            val codes = db.unlockCodeDao().getUnusedBySession("session_1")
            assertEquals(1, codes.size)
            assertEquals("Unknown tier must deserialize to RewardTier.NONE", RewardTier.NONE, codes[0].tier)
        }
    }
}
