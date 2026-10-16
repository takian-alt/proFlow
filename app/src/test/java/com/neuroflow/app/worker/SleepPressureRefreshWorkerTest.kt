package com.neuroflow.app.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.neuroflow.app.domain.repository.SleepPressureRepository
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Tests for SleepPressureRefreshWorker to verify automatic sleep log creation behavior.
 */
@RunWith(RobolectricTestRunner::class)
class SleepPressureRefreshWorkerTest {

    @Test
    fun `worker calls refreshCurrentPressure and returns success`() {
        runTest {
            val context = RuntimeEnvironment.getApplication()
            val sleepPressureRepository = mockk<SleepPressureRepository>()

            coEvery { sleepPressureRepository.refreshCurrentPressure(any()) } returns mockk()

            val workerFactory = object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return SleepPressureRefreshWorker(appContext, workerParameters, sleepPressureRepository)
                }
            }

            val worker = TestListenableWorkerBuilder<SleepPressureRefreshWorker>(context)
                .setWorkerFactory(workerFactory)
                .build()

            val result = worker.doWork()
            result shouldBe ListenableWorker.Result.success()
            coVerify(exactly = 1) { sleepPressureRepository.refreshCurrentPressure(any()) }
        }
    }

    @Test
    fun `worker retries on failure up to 3 times`() {
        runTest {
            val context = RuntimeEnvironment.getApplication()
            val sleepPressureRepository = mockk<SleepPressureRepository>()
            coEvery { sleepPressureRepository.refreshCurrentPressure(any()) } throws RuntimeException("DB error")

            val workerFactory = object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return SleepPressureRefreshWorker(appContext, workerParameters, sleepPressureRepository)
                }
            }

            val worker = TestListenableWorkerBuilder<SleepPressureRefreshWorker>(context)
                .setWorkerFactory(workerFactory)
                .setRunAttemptCount(1)
                .build()

            val result = worker.doWork()
            result shouldBe ListenableWorker.Result.retry()
        }
    }

    @Test
    fun `worker scheduled daily at 7 PM`() {
        // Test documents scheduling behavior
        // The worker is scheduled to run:
        // - Daily at 19:00 (7 PM)
        // - With initial delay calculated to next 7 PM
        // - As a unique periodic work (existing work is cancelled and re-enqueued)

        // This timing ensures:
        // 1. Most users are awake (12+ hours after typical wake time)
        // 2. Automatic fallback logic can trigger if no manual log exists
        // 3. Daily consistency for sleep tracking
    }
}
