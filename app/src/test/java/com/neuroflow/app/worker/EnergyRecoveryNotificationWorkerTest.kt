package com.neuroflow.app.worker

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.EnergyPhase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EnergyRecoveryNotificationWorkerTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        Shadows.shadowOf(RuntimeEnvironment.getApplication())
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        createNotificationChannels(context)
    }

    @Test
    fun `createNotificationChannels registers energy recovery channel`() {
        val channel = notificationManager.getNotificationChannel(CHANNEL_RECOVERY)
        assertNotNull(channel)
        assertEquals("Energy Recovery Breaks", channel?.name?.toString())
    }

    @Test
    fun `worker dispatches break notification when waking energy drops below threshold`() = runTest {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val currentHour = Instant.ofEpochMilli(now).atZone(zone).hour
        val wakeHour = (currentHour - 6).coerceAtLeast(0)
        val sleepHour = ((currentHour + 4) % 24).let { if (it == wakeHour) (wakeHour + 16) % 24 else it }

        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val prefsStore = mockk<UserPreferencesDataStore>(relaxed = true)
        val circadianService = mockk<CircadianEnergyService>(relaxed = true)

        coEvery { taskRepo.getCompletedTasks() } returns emptyList()
        coEvery { sessionRepo.getAllSessions() } returns emptyList()
        every { prefsStore.preferencesFlow } returns flowOf(
            UserPreferences(
                energyRecoveryAlertsEnabled = true,
                recoveryThresholdAutoPersonalized = false,
                recoveryThresholdBasePercent = 50,
                wakeUpHour = wakeHour,
                sleepHour = sleepHour,
                lastRecoveryAlertSentAtMillis = 0L
            )
        )
        coEvery { prefsStore.recordRecoveryAlertSent(any()) } returns Unit

        every {
            circadianService.projectDayTimeline(
                dayStartMillis = any(),
                wakeTimeMillis = any(),
                lastSleepDurationMinutes = any(),
                stepMinutes = any(),
                zoneId = any(),
                naps = any(),
                chronotype = any(),
                manualPeakStartHour = any(),
                cognitiveBlocks = any(),
                learnedProfile = any(),
                rollingAverageSleepMinutes = any(),
                stimulantDoses = any(),
                socialJetlagLagHours = any()
            )
        } returns listOf(
            CircadianSnapshot(
                timestampMillis = now,
                usableEnergy = 34,
                homeostaticPressureS = 0.65,
                circadianAlertnessC = 0.34,
                focusDepletionPenalty = 0.05,
                phase = EnergyPhase.DIP
            )
        )

        val workerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker = EnergyRecoveryNotificationWorker(
                context = appContext,
                params = workerParameters,
                taskRepository = taskRepo,
                sessionRepository = sessionRepo,
                preferencesDataStore = prefsStore,
                circadianEnergyService = circadianService,
                sleepTelemetryRepository = null
            )
        }

        val worker = TestListenableWorkerBuilder<EnergyRecoveryNotificationWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()

        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)

        val shadowNm = Shadows.shadowOf(notificationManager)
        val posted = shadowNm.getNotification(EnergyRecoveryNotificationWorker.NOTIFICATION_ID_RECOVERY)
        assertNotNull("Expected break suggestion notification to be posted", posted)
        assertTrue(posted!!.channelId == CHANNEL_RECOVERY)
        coVerify(exactly = 1) { prefsStore.recordRecoveryAlertSent(any()) }
    }

    @Test
    fun `worker suppresses notification when energy recovery alerts are disabled`() = runTest {
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        val prefsStore = mockk<UserPreferencesDataStore>(relaxed = true)
        val circadianService = mockk<CircadianEnergyService>(relaxed = true)

        every { prefsStore.preferencesFlow } returns flowOf(
            UserPreferences(energyRecoveryAlertsEnabled = false)
        )

        val workerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker = EnergyRecoveryNotificationWorker(
                context = appContext,
                params = workerParameters,
                taskRepository = taskRepo,
                sessionRepository = sessionRepo,
                preferencesDataStore = prefsStore,
                circadianEnergyService = circadianService,
                sleepTelemetryRepository = null
            )
        }

        val worker = TestListenableWorkerBuilder<EnergyRecoveryNotificationWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()

        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)

        val posted = Shadows.shadowOf(notificationManager)
            .getNotification(EnergyRecoveryNotificationWorker.NOTIFICATION_ID_RECOVERY)
        assertNull(posted)
        coVerify(exactly = 0) { prefsStore.recordRecoveryAlertSent(any()) }
    }
}
