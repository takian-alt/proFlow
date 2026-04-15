package com.neuroflow.app.domain.repository

import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.domain.engine.EnergyScoreEngine
import com.neuroflow.app.domain.engine.PeakEnergyEngine
import com.neuroflow.app.domain.engine.SleepPressureDetector
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

@Singleton
class EnergyScoreRepository @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore,
    private val peakEnergyRepository: PeakEnergyRepository
) {

    data class EnergyUiModel(
        val availableEnergy: Int,
        val rawEnergy: Float,
        val peakScore: Float,
        val fatiguePenalty: Float,
        val currentPeakValue: Int,
        val peakValue: Int,
        val peakDrop: Int,
        val minutesSincePeak: Int,
        val minutesUntilPeakReset: Int,
        val sleepPressurePoints: Int,
        val fatiguePercent: Int,
        val fatigueZone: SleepPressureDetector.FatigueZone,
        val circadianFactor: Float,
        val reservoirFactor: Float,
        val confidenceFactor: Float,
        val effectivePeakProfile: PeakEnergyEngine.EffectivePeakProfile? = null,
        val refreshedAtMillis: Long
    )

    fun observeEnergy(refreshIntervalMillis: Long = 60_000L): Flow<EnergyUiModel> {
        val ticker = flow {
            while (true) {
                emit(System.currentTimeMillis())
                delay(refreshIntervalMillis)
            }
        }

        return combine(
            peakEnergyRepository.peakEnergyFlow,
            preferencesDataStore.preferencesFlow,
            ticker
        ) { peakDetection, prefs, nowMillis ->
            val sleepPressurePoints = prefs.sleepPressurePoints.coerceAtLeast(0)
            val score = EnergyScoreEngine.calculateDetailed(
                EnergyScoreEngine.EnergySnapshot(
                    peakEnergy = peakDetection,
                    sleepPressurePoints = sleepPressurePoints,
                    nowMillis = nowMillis
                )
            )

            val currentPeakValue = peakDetection.currentValueAt(nowMillis)
            val peakValue = peakDetection.peakValue
            val peakDrop = (peakValue - currentPeakValue).coerceAtLeast(0)
            val minutesSincePeak = peakDetection.minutesSincePeak(nowMillis)
            val minutesUntilPeakReset = ((24 * 60) - minutesSincePeak).coerceAtLeast(0)

            EnergyUiModel(
                availableEnergy = score.usableEnergy.toInt().coerceIn(0, 100),
                rawEnergy = score.rawEnergy,
                peakScore = score.peakScore,
                fatiguePenalty = score.fatiguePenalty,
                currentPeakValue = currentPeakValue,
                peakValue = peakValue,
                peakDrop = peakDrop,
                minutesSincePeak = minutesSincePeak,
                minutesUntilPeakReset = minutesUntilPeakReset,
                sleepPressurePoints = sleepPressurePoints,
                fatiguePercent = SleepPressureDetector.fatiguePercent(sleepPressurePoints),
                fatigueZone = SleepPressureDetector.fatigueZone(sleepPressurePoints),
                circadianFactor = score.circadianFactor,
                reservoirFactor = score.reservoirFactor,
                confidenceFactor = score.confidenceFactor,
                effectivePeakProfile = peakDetection.effectiveProfile,
                refreshedAtMillis = nowMillis
            )
        }
    }
}
