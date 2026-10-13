package com.neuroflow.app.domain.repository

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.SleepLogRepository
import com.neuroflow.app.data.repository.TaskRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

class PeakEnergyRepositoryManualProfileTest : StringSpec({
    "manual peak windows are used when the chronotype quiz is not enabled" {
        runTest {
            val preferences = mockk<UserPreferencesDataStore>()
            every { preferences.preferencesFlow } returns MutableStateFlow(
                UserPreferences(
                    quizPeakEnabled = false,
                    manualPeakProfileEnabled = true,
                    manualPeakAnchorMinuteOfDay = 600,
                    manualPeakWindow1StartOffsetMinutes = 30,
                    manualPeakWindow1DurationMinutes = 90,
                    manualPeakWindow1Amplitude = 0.7f
                )
            )

            val repository = PeakEnergyRepository(
                preferencesDataStore = preferences,
                sleepLogRepository = mockk<SleepLogRepository>(relaxed = true),
                sessionRepository = mockk<SessionRepository>(relaxed = true),
                taskRepository = mockk<TaskRepository>(relaxed = true)
            )

            val detection = repository.getPeakEnergyDetection()

            detection.peakMinuteOfDay shouldBe 600
            detection.circadianProfile.windows.first().startMinuteOffset shouldBe 30
            detection.circadianProfile.windows.first().durationMinutes shouldBe 90
            detection.circadianProfile.windows.first().amplitude shouldBe 0.7f
            detection.effectiveProfile.explanation shouldBe "Manual profile"
        }
    }
})
