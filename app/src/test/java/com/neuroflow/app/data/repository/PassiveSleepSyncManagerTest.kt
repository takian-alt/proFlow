package com.neuroflow.app.data.repository

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.Companion.SOURCE_HEALTH_CONNECT
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.HealthProviderStatus
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.RawSleepSession
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.SyncResult
import com.neuroflow.app.domain.repository.SleepPressureRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

class PassiveSleepSyncManagerTest : StringSpec({

    val now = 1_700_000_000_000L
    val sevenHoursMs = 7L * 3600_000L
    val sevenHoursMin = 420

    fun setupManager(
        initialPrefs: UserPreferences = UserPreferences()
    ): Triple<PassiveSleepSyncManager, SleepLogRepository, SleepPressureRepository> {
        val sleepLogRepo = mockk<SleepLogRepository>(relaxed = true)
        val sleepPressureRepo = mockk<SleepPressureRepository>(relaxed = true)
        val prefsDataStore = mockk<UserPreferencesDataStore>()

        val prefsFlow = MutableStateFlow(initialPrefs)
        every { prefsDataStore.preferencesFlow } returns prefsFlow

        val manager = PassiveSleepSyncManager(
            sleepLogRepository = sleepLogRepo,
            sleepPressureRepository = sleepPressureRepo,
            preferencesDataStore = prefsDataStore
        )

        return Triple(manager, sleepLogRepo, sleepPressureRepo)
    }

    "syncPassiveSleep: returns ProviderUnavailable when provider is not available" {
        runTest {
            val (manager, _, _) = setupManager()
            manager.setProviderStatus(HealthProviderStatus.NOT_INSTALLED)

            val result = manager.syncPassiveSleep(now - sevenHoursMs, now)
            result.shouldBeInstanceOf<SyncResult.ProviderUnavailable>()
        }
    }

    "syncPassiveSleep: successfully imports valid session and refreshes sleep pressure" {
        runTest {
            val (manager, sleepLogRepo, sleepPressureRepo) = setupManager()
            val startTime = now - sevenHoursMs
            val endTime = now

            val rawSession = RawSleepSession(
                externalRecordId = "rec_001",
                startTimeMillis = startTime,
                endTimeMillis = endTime,
                notes = "Oura Ring sync"
            )
            manager.setExternalSessions(listOf(rawSession))

            val createdLog = SleepLogEntity(
                id = "log_101",
                startAt = startTime,
                endAt = endTime,
                durationMinutes = sevenHoursMin,
                source = SOURCE_HEALTH_CONNECT,
                notes = "Oura Ring sync"
            )
            coEvery { sleepLogRepo.getOverlapping(startTime, endTime) } returns emptyList()
            coEvery { sleepLogRepo.addLog(startTime, endTime, SOURCE_HEALTH_CONNECT, "Oura Ring sync") } returns createdLog

            val result = manager.syncPassiveSleep(startTime - 1000, endTime + 1000)
            result.shouldBeInstanceOf<SyncResult.Success>()
            val success = result as SyncResult.Success
            success.importedLogs.size shouldBe 1
            success.importedLogs.first().id shouldBe "log_101"
            success.skippedDuplicates shouldBe 0
            success.replacedFallbackCount shouldBe 0

            coVerify(exactly = 1) { sleepPressureRepo.refreshCurrentPressure(any()) }
        }
    }

    "syncPassiveSleep: preserves manual log and skips passive session on overlap" {
        runTest {
            val (manager, sleepLogRepo, sleepPressureRepo) = setupManager()
            val startTime = now - sevenHoursMs
            val endTime = now

            manager.setExternalSessions(listOf(
                RawSleepSession("rec_002", startTime, endTime)
            ))

            val manualLog = SleepLogEntity(
                id = "manual_42",
                startAt = startTime,
                endAt = endTime,
                durationMinutes = sevenHoursMin,
                source = "MANUAL"
            )
            coEvery { sleepLogRepo.getOverlapping(startTime, endTime) } returns listOf(manualLog)

            val result = manager.syncPassiveSleep(startTime - 1000, endTime + 1000)
            result.shouldBeInstanceOf<SyncResult.Success>()
            val success = result as SyncResult.Success
            success.importedLogs shouldBe emptyList()
            success.skippedDuplicates shouldBe 1
            success.replacedFallbackCount shouldBe 0

            coVerify(exactly = 0) { sleepLogRepo.addLog(any(), any(), any(), any()) }
            coVerify(exactly = 0) { sleepPressureRepo.refreshCurrentPressure() }
        }
    }

    "syncPassiveSleep: skips session if >80% overlapping with existing Health Connect session" {
        runTest {
            val (manager, sleepLogRepo, sleepPressureRepo) = setupManager()
            val startTime = now - sevenHoursMs
            val endTime = now

            manager.setExternalSessions(listOf(
                RawSleepSession("rec_003", startTime, endTime)
            ))

            // Existing session covers 6.5h out of 7h (> 92% overlap)
            val existingHealthLog = SleepLogEntity(
                id = "health_88",
                startAt = startTime,
                endAt = endTime - 1800_000L,
                durationMinutes = 390,
                source = SOURCE_HEALTH_CONNECT
            )
            coEvery { sleepLogRepo.getOverlapping(startTime, endTime) } returns listOf(existingHealthLog)

            val result = manager.syncPassiveSleep(startTime - 1000, endTime + 1000)
            result.shouldBeInstanceOf<SyncResult.Success>()
            val success = result as SyncResult.Success
            success.importedLogs shouldBe emptyList()
            success.skippedDuplicates shouldBe 1

            coVerify(exactly = 0) { sleepLogRepo.addLog(any(), any(), any(), any()) }
        }
    }

    "syncPassiveSleep: supersedes and deletes AUTO_DEFAULT fallback log" {
        runTest {
            val (manager, sleepLogRepo, sleepPressureRepo) = setupManager()
            val startTime = now - sevenHoursMs
            val endTime = now

            manager.setExternalSessions(listOf(
                RawSleepSession("rec_004", startTime, endTime)
            ))

            val autoFallbackLog = SleepLogEntity(
                id = "auto_7",
                startAt = startTime,
                endAt = endTime,
                durationMinutes = sevenHoursMin,
                source = "AUTO_DEFAULT"
            )
            coEvery { sleepLogRepo.getOverlapping(startTime, endTime) } returns listOf(autoFallbackLog)
            coEvery { sleepLogRepo.addLog(startTime, endTime, SOURCE_HEALTH_CONNECT, any()) } returns SleepLogEntity(
                id = "log_102",
                startAt = startTime,
                endAt = endTime,
                durationMinutes = sevenHoursMin,
                source = SOURCE_HEALTH_CONNECT
            )

            val result = manager.syncPassiveSleep(startTime - 1000, endTime + 1000)
            result.shouldBeInstanceOf<SyncResult.Success>()
            val success = result as SyncResult.Success
            success.importedLogs.size shouldBe 1
            success.replacedFallbackCount shouldBe 1

            coVerify(exactly = 1) { sleepLogRepo.deleteById("auto_7") }
            coVerify(exactly = 1) { sleepLogRepo.addLog(startTime, endTime, SOURCE_HEALTH_CONNECT, any()) }
            coVerify(exactly = 1) { sleepPressureRepo.refreshCurrentPressure(any()) }
        }
    }

    "syncPassiveSleep: bounds check skips durations under 1 min or over 24h" {
        runTest {
            val (manager, sleepLogRepo, _) = setupManager()
            val thirtySecondsSession = RawSleepSession("rec_short", now - 30_000L, now)
            val thirtyHoursSession = RawSleepSession("rec_long", now - 30L * 3600_000L, now)

            manager.setExternalSessions(listOf(thirtySecondsSession, thirtyHoursSession))

            val result = manager.syncPassiveSleep(now - 40L * 3600_000L, now)
            result.shouldBeInstanceOf<SyncResult.Success>()
            val success = result as SyncResult.Success
            success.importedLogs shouldBe emptyList()
            success.skippedDuplicates shouldBe 2

            coVerify(exactly = 0) { sleepLogRepo.addLog(any(), any(), any(), any()) }
        }
    }
})
