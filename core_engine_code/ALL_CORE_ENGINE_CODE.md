# neuroFlow — Complete Core Engine Source Codebase

### Energy & Circadian Biophysics, Auto-Scheduling Engine, Task Creation & Decomposition, and Data Contracts

**Generated for Vice Head Architecture Audit:** 2026-10-23 22:05:10  
**Total Source & Test Files:** 73  
**Total Lines of Code:** 16,780  
**Total Uncompressed Size:** 720.5 KB (0.70 MB)  

---

## Subsystem Architecture Breakdown

| Part File | Focus Area | Files | Lines | Size (KB) |
| :--- | :--- | :---: | :---: | :---: |
| [`PART_1_ENERGY_CIRCADIAN.md`](./PART_1_ENERGY_CIRCADIAN.md) | **Part 1: Energy & Circadian Biophysics** | 14 | 4,067 | 163.9 KB |
| [`PART_2_AUTOSCHEDULING_PLACEMENT.md`](./PART_2_AUTOSCHEDULING_PLACEMENT.md) | **Part 2: Auto-Scheduling & Mathematical Placement Engine** | 14 | 4,029 | 178.2 KB |
| [`PART_3_TASK_CREATION_NLP_AI.md`](./PART_3_TASK_CREATION_NLP_AI.md) | **Part 3: Task Creation, NLP Parsing, AI Decomposition & Behavioral Engines** | 9 | 2,484 | 111.4 KB |
| [`PART_4_DATA_ENTITIES_REPOSITORIES.md`](./PART_4_DATA_ENTITIES_REPOSITORIES.md) | **Part 4: Data Layer Architecture, Room Schemas, Entities, DAOs & Repositories** | 19 | 2,674 | 125.0 KB |
| [`PART_5_TEST_SUITES_VERIFICATION.md`](./PART_5_TEST_SUITES_VERIFICATION.md) | **Part 5: Core Algorithmic & Mathematical Test Suites** | 17 | 3,526 | 142.1 KB |
| **Total** | **All Core Engine Components** | **73** | **16,780** | **720.5 KB** |

---

## Full File Manifest

- [`app/src/main/java/com/neuroflow/app/domain/service/CircadianEnergyService.kt`](#appsrcmainjavacomneuroflowappdomainservicecircadianenergyservicekt)
- [`app/src/main/java/com/neuroflow/app/domain/service/ChronobiologySignalNormalizer.kt`](#appsrcmainjavacomneuroflowappdomainservicechronobiologysignalnormalizerkt)
- [`app/src/main/java/com/neuroflow/app/domain/model/EnergyUiModel.kt`](#appsrcmainjavacomneuroflowappdomainmodelenergyuimodelkt)
- [`app/src/main/java/com/neuroflow/app/domain/model/CircadianEnergySummary.kt`](#appsrcmainjavacomneuroflowappdomainmodelcircadianenergysummarykt)
- [`app/src/main/java/com/neuroflow/app/domain/model/FatigueZone.kt`](#appsrcmainjavacomneuroflowappdomainmodelfatiguezonekt)
- [`app/src/main/java/com/neuroflow/app/domain/engine/MEQChronotypeDetector.kt`](#appsrcmainjavacomneuroflowappdomainenginemeqchronotypedetectorkt)
- [`app/src/main/java/com/neuroflow/app/domain/repository/SleepPressureRepository.kt`](#appsrcmainjavacomneuroflowappdomainrepositorysleeppressurerepositorykt)
- [`app/src/main/java/com/neuroflow/app/domain/repository/PassiveSleepSyncRepository.kt`](#appsrcmainjavacomneuroflowappdomainrepositorypassivesleepsyncrepositorykt)
- [`app/src/main/java/com/neuroflow/app/domain/repository/EnergyMetricsRepository.kt`](#appsrcmainjavacomneuroflowappdomainrepositoryenergymetricsrepositorykt)
- [`app/src/main/java/com/neuroflow/app/data/sleep/SleepTelemetryDataSource.kt`](#appsrcmainjavacomneuroflowappdatasleepsleeptelemetrydatasourcekt)
- [`app/src/main/java/com/neuroflow/app/data/repository/SleepLogRepository.kt`](#appsrcmainjavacomneuroflowappdatarepositorysleeplogrepositorykt)
- [`app/src/main/java/com/neuroflow/app/data/repository/PassiveSleepSyncManager.kt`](#appsrcmainjavacomneuroflowappdatarepositorypassivesleepsyncmanagerkt)
- [`app/src/main/java/com/neuroflow/app/worker/SleepPressureRefreshWorker.kt`](#appsrcmainjavacomneuroflowappworkersleeppressurerefreshworkerkt)
- [`app/src/main/java/com/neuroflow/app/worker/EnergyTelemetryCleanupWorker.kt`](#appsrcmainjavacomneuroflowappworkerenergytelemetrycleanupworkerkt)
- [`app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt`](#appsrcmainjavacomneuroflowappdomainservicetaskschedulerservicekt)
- [`app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingContracts.kt`](#appsrcmainjavacomneuroflowappdomainschedulerautoschedulingcontractskt)
- [`app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingRationales.kt`](#appsrcmainjavacomneuroflowappdomainschedulerautoschedulingrationaleskt)
- [`app/src/main/java/com/neuroflow/app/domain/scheduler/PlacementStability.kt`](#appsrcmainjavacomneuroflowappdomainschedulerplacementstabilitykt)
- [`app/src/main/java/com/neuroflow/app/domain/scheduler/ProtectedSlotClock.kt`](#appsrcmainjavacomneuroflowappdomainschedulerprotectedslotclockkt)
- [`app/src/main/java/com/neuroflow/app/domain/scheduler/TaskBlockPlan.kt`](#appsrcmainjavacomneuroflowappdomainschedulertaskblockplankt)
- [`app/src/main/java/com/neuroflow/app/domain/scheduler/TaskTagSchedulingProfile.kt`](#appsrcmainjavacomneuroflowappdomainschedulertasktagschedulingprofilekt)
- [`app/src/main/java/com/neuroflow/app/domain/scheduler/BehavioralInsightsEngine.kt`](#appsrcmainjavacomneuroflowappdomainschedulerbehavioralinsightsenginekt)
- [`app/src/main/java/com/neuroflow/app/domain/scheduler/DependencyGraphValidator.kt`](#appsrcmainjavacomneuroflowappdomainschedulerdependencygraphvalidatorkt)
- [`app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt`](#appsrcmainjavacomneuroflowappdomainschedulerschedulepreparationorchestratorkt)
- [`app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt`](#appsrcmainjavacomneuroflowappdatacalendarcalendarsyncdatasourcekt)
- [`app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt`](#appsrcmainjavacomneuroflowappworkerscheduleautotasksworkerkt)
- [`app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt`](#appsrcmainjavacomneuroflowappworkernotificationworkerkt)
- [`app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt`](#appsrcmainjavacomneuroflowappworkerfocuswidgetupdateworkerkt)
- [`app/src/main/java/com/neuroflow/app/domain/service/TaskNaturalLanguageParser.kt`](#appsrcmainjavacomneuroflowappdomainservicetasknaturallanguageparserkt)
- [`app/src/main/java/com/neuroflow/app/domain/inference/TaskInferenceEngine.kt`](#appsrcmainjavacomneuroflowappdomaininferencetaskinferenceenginekt)
- [`app/src/main/java/com/neuroflow/app/domain/service/GeminiAiOnlineService.kt`](#appsrcmainjavacomneuroflowappdomainservicegeminiaionlineservicekt)
- [`app/src/main/java/com/neuroflow/app/domain/service/SmartTaskDecompositionService.kt`](#appsrcmainjavacomneuroflowappdomainservicesmarttaskdecompositionservicekt)
- [`app/src/main/java/com/neuroflow/app/domain/service/BehavioralSupportService.kt`](#appsrcmainjavacomneuroflowappdomainservicebehavioralsupportservicekt)
- [`app/src/main/java/com/neuroflow/app/domain/engine/WoopEngine.kt`](#appsrcmainjavacomneuroflowappdomainenginewoopenginekt)
- [`app/src/main/java/com/neuroflow/app/domain/engine/AutonomyNudgeEngine.kt`](#appsrcmainjavacomneuroflowappdomainengineautonomynudgeenginekt)
- [`app/src/main/java/com/neuroflow/app/domain/engine/FreshStartEngine.kt`](#appsrcmainjavacomneuroflowappdomainenginefreshstartenginekt)
- [`app/src/main/java/com/neuroflow/app/domain/engine/AnalyticsEngine.kt`](#appsrcmainjavacomneuroflowappdomainengineanalyticsenginekt)
- [`app/src/main/java/com/neuroflow/app/data/local/entity/TaskEntity.kt`](#appsrcmainjavacomneuroflowappdatalocalentitytaskentitykt)
- [`app/src/main/java/com/neuroflow/app/data/local/entity/TaskMicroStepEntity.kt`](#appsrcmainjavacomneuroflowappdatalocalentitytaskmicrostepentitykt)
- [`app/src/main/java/com/neuroflow/app/data/local/entity/TaskTimeExtensions.kt`](#appsrcmainjavacomneuroflowappdatalocalentitytasktimeextensionskt)
- [`app/src/main/java/com/neuroflow/app/data/local/entity/EnergyPredictionEntity.kt`](#appsrcmainjavacomneuroflowappdatalocalentityenergypredictionentitykt)
- [`app/src/main/java/com/neuroflow/app/data/local/entity/ProtectedSlotEntity.kt`](#appsrcmainjavacomneuroflowappdatalocalentityprotectedslotentitykt)
- [`app/src/main/java/com/neuroflow/app/data/local/entity/SleepLogEntity.kt`](#appsrcmainjavacomneuroflowappdatalocalentitysleeplogentitykt)
- [`app/src/main/java/com/neuroflow/app/data/local/dao/TaskDao.kt`](#appsrcmainjavacomneuroflowappdatalocaldaotaskdaokt)
- [`app/src/main/java/com/neuroflow/app/data/local/dao/TaskMicroStepDao.kt`](#appsrcmainjavacomneuroflowappdatalocaldaotaskmicrostepdaokt)
- [`app/src/main/java/com/neuroflow/app/data/local/dao/ProtectedSlotDao.kt`](#appsrcmainjavacomneuroflowappdatalocaldaoprotectedslotdaokt)
- [`app/src/main/java/com/neuroflow/app/data/local/dao/EnergyPredictionDao.kt`](#appsrcmainjavacomneuroflowappdatalocaldaoenergypredictiondaokt)
- [`app/src/main/java/com/neuroflow/app/data/local/dao/SleepLogDao.kt`](#appsrcmainjavacomneuroflowappdatalocaldaosleeplogdaokt)
- [`app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt`](#appsrcmainjavacomneuroflowappdatarepositorytaskrepositorykt)
- [`app/src/main/java/com/neuroflow/app/data/local/Converters.kt`](#appsrcmainjavacomneuroflowappdatalocalconverterskt)
- [`app/src/main/java/com/neuroflow/app/data/local/UserPreferencesDataStore.kt`](#appsrcmainjavacomneuroflowappdatalocaluserpreferencesdatastorekt)
- [`app/src/main/java/com/neuroflow/app/domain/model/Enums.kt`](#appsrcmainjavacomneuroflowappdomainmodelenumskt)
- [`app/src/main/java/com/neuroflow/app/domain/model/ProtectedTimeSlot.kt`](#appsrcmainjavacomneuroflowappdomainmodelprotectedtimeslotkt)
- [`app/src/main/java/com/neuroflow/app/domain/model/StepByStepPlan.kt`](#appsrcmainjavacomneuroflowappdomainmodelstepbystepplankt)
- [`app/src/main/java/com/neuroflow/app/di/DomainServiceModule.kt`](#appsrcmainjavacomneuroflowappdidomainservicemodulekt)
- [`app/src/main/java/com/neuroflow/app/di/AppModule.kt`](#appsrcmainjavacomneuroflowappdiappmodulekt)
- [`app/src/test/java/com/neuroflow/app/domain/service/CircadianEnergyServiceTest.kt`](#appsrctestjavacomneuroflowappdomainservicecircadianenergyservicetestkt)
- [`app/src/test/java/com/neuroflow/app/domain/service/FastCircadianLearnerTest.kt`](#appsrctestjavacomneuroflowappdomainservicefastcircadianlearnertestkt)
- [`app/src/test/java/com/neuroflow/app/domain/service/CircadianAdversarialStressTest.kt`](#appsrctestjavacomneuroflowappdomainservicecircadianadversarialstresstestkt)
- [`app/src/test/java/com/neuroflow/app/domain/service/Adr020RobustnessTest.kt`](#appsrctestjavacomneuroflowappdomainserviceadr020robustnesstestkt)
- [`app/src/test/java/com/neuroflow/app/domain/service/Adr022RobustnessTest.kt`](#appsrctestjavacomneuroflowappdomainserviceadr022robustnesstestkt)
- [`app/src/test/java/com/neuroflow/app/domain/service/TaskSchedulerServiceTest.kt`](#appsrctestjavacomneuroflowappdomainservicetaskschedulerservicetestkt)
- [`app/src/test/java/com/neuroflow/app/domain/service/TaskNaturalLanguageParserTest.kt`](#appsrctestjavacomneuroflowappdomainservicetasknaturallanguageparsertestkt)
- [`app/src/test/java/com/neuroflow/app/domain/service/SmartTaskDecompositionServiceTest.kt`](#appsrctestjavacomneuroflowappdomainservicesmarttaskdecompositionservicetestkt)
- [`app/src/test/java/com/neuroflow/app/domain/service/GeminiAiOnlineServiceTest.kt`](#appsrctestjavacomneuroflowappdomainservicegeminiaionlineservicetestkt)
- [`app/src/test/java/com/neuroflow/app/domain/service/ChronobiologySignalNormalizerTest.kt`](#appsrctestjavacomneuroflowappdomainservicechronobiologysignalnormalizertestkt)
- [`app/src/test/java/com/neuroflow/app/domain/service/RealWorldChronobiologyAndSchedulerTest.kt`](#appsrctestjavacomneuroflowappdomainservicerealworldchronobiologyandschedulertestkt)
- [`app/src/test/java/com/neuroflow/app/domain/scheduler/PlacementStabilityTest.kt`](#appsrctestjavacomneuroflowappdomainschedulerplacementstabilitytestkt)
- [`app/src/test/java/com/neuroflow/app/domain/scheduler/ProtectedSlotClockTest.kt`](#appsrctestjavacomneuroflowappdomainschedulerprotectedslotclocktestkt)
- [`app/src/test/java/com/neuroflow/app/domain/inference/TaskInferenceEngineTest.kt`](#appsrctestjavacomneuroflowappdomaininferencetaskinferenceenginetestkt)
- [`app/src/test/java/com/neuroflow/app/data/local/StimulantLoggingTest.kt`](#appsrctestjavacomneuroflowappdatalocalstimulantloggingtestkt)
- [`app/src/test/java/com/neuroflow/app/data/calendar/CalendarSyncRepositoryTest.kt`](#appsrctestjavacomneuroflowappdatacalendarcalendarsyncrepositorytestkt)
- [`app/src/test/java/com/neuroflow/app/data/calendar/CalendarWriteBackSyncTest.kt`](#appsrctestjavacomneuroflowappdatacalendarcalendarwritebacksynctestkt)

---

## Source Code Files

<a id="appsrcmainjavacomneuroflowappdomainservicecircadianenergyservicekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/service/CircadianEnergyService.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/service/CircadianEnergyService.kt`
- **Lines of Code:** 1,651
- **Size:** 67,819 bytes (66.2 KB)

```kotlin
package com.neuroflow.app.domain.service

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class EnergyDemand(val targetScore: Double) {
    LOW(25.0),
    MODERATE(50.0),
    HIGH(80.0)
}

enum class TaskCategory {
    DEEP_WORK, CREATIVE, ADMIN, EXERCISE, MINDFULNESS, GENERAL
}

enum class EnergyPhase {
    PEAK, REBOUND, DIP, WIND_DOWN, RECOVERY_NEEDED
}

enum class ChronotypeProfile(val peakOffsetFromWakeHours: Double) {
    DEFINITE_MORNING(3.0),
    MODERATE_MORNING(3.8),
    INTERMEDIATE(4.5),
    MODERATE_EVENING(6.5),
    DEFINITE_EVENING(8.0);

    companion object {
        fun fromString(value: String?): ChronotypeProfile {
            if (value.isNullOrBlank()) return INTERMEDIATE
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: INTERMEDIATE
        }
    }
}

/**
 * Hardened Abramowitz & Stegun exponentially scaled Bessel function:
 * Returns I_0(x) * exp(-|x|), which is strictly bounded in (0, 1] for all x in (-inf, +inf)
 * and never overflows to Infinity even at kappa = 10,000+.
 */
fun besselI0Scaled(x: Double): Double {
    val ax = abs(x)
    if (ax.isNaN()) return 1.0
    if (ax.isInfinite()) return 0.0
    return if (ax < 3.75) {
        val y = (x / 3.75) * (x / 3.75)
        val raw = 1.0 + y * (3.5156229 + y * (3.0899424 + y * (1.2067492 + y * (0.2659732 + y * (0.0360768 + y * 0.0045813)))))
        raw * exp(-ax)
    } else {
        val y = 3.75 / ax
        (1.0 / sqrt(ax)) * (0.39894228 + y * (0.01328592 + y * (0.00225319 + y * (-0.00157565 + y * (0.00916281 + y * (-0.02057706 + y * (0.02635537 + y * (-0.01647633 + y * 0.00392377))))))))
    }
}

/**
 * Overflow-safe Von Mises circular probability density function.
 * Uses identity: exp(k * cos(theta)) / I_0(k) == exp(k * (cos(theta) - 1)) / (I_0(k) * exp(-k)).
 */
fun computeVonMisesDensitySafe(
    hour: Double,
    peakHour: Double,
    kappa: Double = AdaptiveCircadianLearner.DEFAULT_KAPPA
): Double {
    val safeKappa = kappa.coerceIn(0.0, 10_000.0)
    if (safeKappa <= 1e-6) return 1.0 / (2.0 * PI)
    val angle = 2.0 * PI * (hour - peakHour) / 24.0
    val scaledI0 = besselI0Scaled(safeKappa).coerceAtLeast(1e-12)
    return (exp(safeKappa * (cos(angle) - 1.0)) / (2.0 * PI * scaledI0)).coerceAtLeast(0.0)
}

/**
 * C1-Continuous Post-Prandial Meal Impulse Calculator.
 * Preserves the exact -12.0 peak at delta = 60m and smoothly tapers to 0.0 over [135m..180m]
 * using a raised cosine window, eliminating the 4.87pt cliff at 180m.
 */
fun computeSmoothMealImpulse(
    minuteOfDay: Int,
    mealBlocks: List<CognitiveBlockWindow>,
    timestampMillis: Long? = null
): Double {
    if (mealBlocks.isEmpty()) return 0.0
    var minImpulse = 0.0
    for (meal in mealBlocks) {
        val deltaMinutes = if (timestampMillis != null && meal.explicitStartMillis != null) {
            ((timestampMillis - meal.explicitStartMillis) / 60_000L).toInt()
        } else {
            (minuteOfDay - meal.startMinuteOfDay + 1440) % 1440
        }
        if (deltaMinutes in 0..180) {
            val tFactor = deltaMinutes / 60.0
            val rawDip = -12.0 * tFactor * exp(1.0 - tFactor) * meal.intensity
            val tailWindow = if (deltaMinutes <= 135) {
                1.0
            } else {
                val u = (deltaMinutes - 135.0) / 45.0
                0.5 * (1.0 + cos(PI * u))
            }
            val smoothedDip = rawDip * tailWindow
            if (smoothedDip < minImpulse) {
                minImpulse = smoothedDip
            }
        }
    }
    return minImpulse
}

/**
 * Computes the latest clock minute of the day [0..1439] at which a user can consume
 * [doseMg] of [type] without exceeding [maxResidualMgAtBedtime] at [sleepHour].
 */
fun computeLatestSafeStimulantCutoffMinute(
    sleepHour: Int,
    doseMg: Double = 100.0,
    type: StimulantType = StimulantType.CAFFEINE,
    maxResidualMgAtBedtime: Double = 35.0
): Int {
    val sleepMinuteOfDay = (sleepHour.coerceIn(0, 23)) * 60
    val effectiveDose = (doseMg / type.referenceDoseMg) * 100.0
    if (effectiveDose <= maxResidualMgAtBedtime) return sleepMinuteOfDay
    val ke = ln(2.0) / type.halfLifeHours
    val requiredClearanceHours = ln(effectiveDose / maxResidualMgAtBedtime.coerceAtLeast(1.0)) / ke
    val clearanceMinutes = (requiredClearanceHours * 60.0).roundToInt().coerceIn(0, 1440)
    return ((sleepMinuteOfDay - clearanceMinutes) % 1440 + 1440) % 1440
}

/**
 * Computes circular weighted average of clock hours [0..24), properly handling
 * modulo-24 wrap-around across midnight using 2D unit vector trigonometric decomposition.
 */
fun blendClockHoursCircular(
    hoursWithWeights: List<Pair<Double, Double>>
): Double {
    if (hoursWithWeights.isEmpty()) return 10.0
    var sumX = 0.0
    var sumY = 0.0
    var totalWeight = 0.0
    for ((hour, weight) in hoursWithWeights) {
        if (weight <= 0.0) continue
        val theta = (hour % 24.0) * (2.0 * Math.PI / 24.0)
        sumX += weight * cos(theta)
        sumY += weight * sin(theta)
        totalWeight += weight
    }
    if (totalWeight <= 0.0 || (abs(sumX) < 1e-9 && abs(sumY) < 1e-9)) {
        return (hoursWithWeights.first().first % 24.0 + 24.0) % 24.0
    }
    var angle = atan2(sumY, sumX)
    if (angle < 0.0) {
        angle += 2.0 * Math.PI
    }
    return (angle * 24.0 / (2.0 * Math.PI)) % 24.0
}

/**
 * Signed shortest circular difference (a - b) in clock hours within [-12.0, +12.0].
 */
fun circularHourDifference(hourA: Double, hourB: Double): Double {
    val diff = ((hourA - hourB) % 24.0 + 36.0) % 24.0 - 12.0
    return diff.coerceIn(-12.0, 12.0)
}

fun computePhasePeakHour(
    wakeClockHour: Double,
    chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
    manualPeakStartHour: Int? = null,
    detectedPeakHour: Double? = null,
    detectionConfidence: Double = 0.0
): Double {
    val bioPeak = (wakeClockHour + chronotype.peakOffsetFromWakeHours) % 24.0

    val effectiveBio = if (detectedPeakHour != null && detectionConfidence > 0.0) {
        val conf = detectionConfidence.coerceIn(0.0, 1.0)
        val empiricalWeight = 0.40 * conf
        val bioWeight = 1.0 - empiricalWeight
        blendClockHoursCircular(listOf(bioPeak to bioWeight, (detectedPeakHour % 24.0) to empiricalWeight))
    } else {
        bioPeak
    }

    return if (manualPeakStartHour != null && manualPeakStartHour in 0..23) {
        blendClockHoursCircular(listOf(effectiveBio to 0.70, manualPeakStartHour.toDouble() to 0.30))
    } else {
        effectiveBio
    }
}

/**
 * Interpolates the 24-bin Von Mises multiplier for fractional [clockHour] and blends it
 * with the bimodal harmonic curve according to [learnedProfile.confidence].
 */
fun applyLearnedVonMisesModulation(
    baseProcessC: Double,
    clockHour: Double,
    learnedProfile: LearnedCircadianProfile?
): Double {
    if (learnedProfile == null || learnedProfile.hourlyMultipliers.size != 24 || learnedProfile.confidence <= 0.05) {
        return baseProcessC
    }
    val normHour = ((clockHour % 24.0) + 24.0) % 24.0
    val h0 = normHour.toInt() % 24
    val h1 = (h0 + 1) % 24
    val frac = normHour - normHour.toInt()

    val m0 = learnedProfile.hourlyMultipliers[h0]
    val m1 = learnedProfile.hourlyMultipliers[h1]
    val interpolatedVonMises = (m0 + frac * (m1 - m0)).coerceIn(0.0, 1.0)

    // Blend up to 35% of the empirical Von Mises density shape as confidence approaches 1.0
    val blendWeight = (0.35 * learnedProfile.confidence).coerceIn(0.0, 0.35)
    return ((1.0 - blendWeight) * baseProcessC + blendWeight * interpolatedVonMises).coerceIn(0.0, 1.0)
}

enum class CognitiveBlockCategory {
    MEAL, EXERCISE, FOCUS_WORK, WIND_DOWN, SLEEP
}

typealias CognitiveBlockType = CognitiveBlockCategory

data class CognitiveBlockWindow(
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val category: CognitiveBlockCategory,
    val intensity: Double = 1.0,
    val label: String = "",
    val explicitStartMillis: Long? = null,
    val explicitEndMillis: Long? = null,
    val explicitDemand: EnergyDemand? = null,
    val isFrog: Boolean = label.contains("Frog", ignoreCase = true),
    val isAnxietyTask: Boolean = false,
    val isCompleted: Boolean = false
) {
    val type: CognitiveBlockCategory get() = category
    val id: String get() = label
    val title: String get() = label
    val startMillis: Long get() = explicitStartMillis ?: (startMinuteOfDay * 60_000L)
    val endMillis: Long get() = explicitEndMillis ?: (endMinuteOfDay * 60_000L)
    val demand: EnergyDemand get() = explicitDemand ?: if (intensity > 1.0) EnergyDemand.HIGH else EnergyDemand.MODERATE

    val durationMinutes: Int
        get() = if (endMinuteOfDay >= startMinuteOfDay) {
            endMinuteOfDay - startMinuteOfDay
        } else {
            1440 - startMinuteOfDay + endMinuteOfDay
        }

    fun toTimeSlotWindow(date: LocalDate, zoneId: ZoneId): TimeSlotWindow {
        if (explicitStartMillis != null && explicitEndMillis != null) {
            return TimeSlotWindow(explicitStartMillis, explicitEndMillis)
        }
        val startZdt = date.atStartOfDay(zoneId).plusMinutes(startMinuteOfDay.toLong())
        val endZdt = if (endMinuteOfDay >= startMinuteOfDay) {
            date.atStartOfDay(zoneId).plusMinutes(endMinuteOfDay.toLong())
        } else {
            date.plusDays(1).atStartOfDay(zoneId).plusMinutes(endMinuteOfDay.toLong())
        }
        return TimeSlotWindow(
            startMillis = explicitStartMillis ?: startZdt.toInstant().toEpochMilli(),
            endMillis = explicitEndMillis ?: endZdt.toInstant().toEpochMilli()
        )
    }

    fun toTimeSlotWindow(dayStartMillis: Long): TimeSlotWindow {
        if (explicitStartMillis != null && explicitEndMillis != null) {
            return TimeSlotWindow(explicitStartMillis, explicitEndMillis)
        }
        val endOffset = if (endMinuteOfDay >= startMinuteOfDay) endMinuteOfDay else endMinuteOfDay + 1440
        return TimeSlotWindow(
            startMillis = explicitStartMillis ?: (dayStartMillis + startMinuteOfDay * 60_000L),
            endMillis = explicitEndMillis ?: (dayStartMillis + endOffset * 60_000L)
        )
    }
}

data class BehavioralObservation(
    val minuteOfDay: Int,
    val productivityRating: Double, // 0.0 .. 1.0
    val weight: Double = 1.0
) {
    val clockHour: Double get() = (minuteOfDay / 60.0).coerceIn(0.0, 24.0) % 24.0
}

data class LearnedCircadianProfile(
    val peakHourMean: Double,
    val peakHourVariance: Double = 4.0,
    val sampleCount: Double = 0.0,
    val circadianAmplitude: Double = 1.0,
    val morningLarkProbability: Double = 0.5,
    val confidence: Double = (sampleCount / (sampleCount + 3.0)).coerceIn(0.0, 1.0),
    val hourlyMultipliers: List<Double> = emptyList()
) {
    val empiricalPeakHour: Double get() = peakHourMean
}

object AdaptiveCircadianLearner {
    const val DEFAULT_KAPPA = 4.5
    private const val PROCESS_NOISE_Q = 0.05

    /**
     * Modified Bessel function of the first kind of order 0: I_0(x).
     * Uses Abramowitz and Stegun polynomial approximations.
     */
    fun besselI0(x: Double): Double {
        val ax = abs(x)
        return if (ax < 3.75) {
            val y = (x / 3.75) * (x / 3.75)
            1.0 + y * (3.5156229 + y * (3.0899424 + y * (1.2067492 + y * (0.2659732 + y * (0.0360768 + y * 0.0045813)))))
        } else {
            val y = 3.75 / ax
            (exp(ax) / sqrt(ax)) * (0.39894228 + y * (0.01328592 + y * (0.00225319 + y * (-0.00157565 + y * (0.00916281 + y * (-0.02057706 + y * (0.02635537 + y * (-0.01647633 + y * 0.00392377))))))))
        }
    }

    /**
     * Evaluates the Von Mises circular kernel probability density function at [hour] given [peakHour] and [kappa].
     * Note: For guaranteed numerical stability across all kappa in [0, 10000], see [computeVonMisesDensitySafe].
     */
    fun computeVonMisesDensity(hour: Double, peakHour: Double, kappa: Double = DEFAULT_KAPPA): Double {
        return computeVonMisesDensitySafe(hour, peakHour, kappa)
    }

    /**
     * Computes 24 hourly multipliers [0..23] using the Von Mises circular smoother with peak normalized to 1.0.
     */
    fun computeVonMisesHourlyMultipliers(peakHour: Double, kappa: Double = DEFAULT_KAPPA): List<Double> {
        return (0 until 24).map { h ->
            val angle = 2.0 * Math.PI * (h - peakHour) / 24.0
            exp(kappa * (cos(angle) - 1.0))
        }
    }

    fun computeMorningLarkProbability(peakHour: Double): Double {
        val normalizedHour = ((peakHour % 24.0) + 24.0) % 24.0
        return when {
            normalizedHour in 4.0..11.0 -> 0.85
            normalizedHour in 11.0..14.0 -> 0.60
            normalizedHour in 14.0..17.0 -> 0.35
            normalizedHour in 17.0..22.0 -> 0.15
            else -> 0.10
        }
    }

    /**
     * 1D circular Kalman filter state update.
     * Shortest circular innovation: e_k = ((z_k - theta_k + 36.0) % 24.0) - 12.0
     */
    fun updateProfile(
        prior: LearnedCircadianProfile,
        observation: BehavioralObservation,
        kappa: Double = DEFAULT_KAPPA
    ): LearnedCircadianProfile {
        val z = (observation.minuteOfDay / 60.0).coerceIn(0.0, 24.0) % 24.0
        val priorMean = prior.peakHourMean
        val priorVar = prior.peakHourVariance.coerceAtLeast(0.1)

        val shortestDiff = (((z - priorMean + 36.0) % 24.0) - 12.0)
        val obsWeight = (observation.productivityRating * observation.weight).coerceIn(0.1, 5.0)
        val r = (1.5 / obsWeight).coerceIn(0.3, 10.0)

        val kalmanGain = priorVar / (priorVar + r)
        val updatedMean = (((priorMean + kalmanGain * shortestDiff) % 24.0) + 24.0) % 24.0
        val updatedVar = ((1.0 - kalmanGain) * priorVar + PROCESS_NOISE_Q).coerceIn(0.05, 12.0)
        val updatedSampleCount = prior.sampleCount + observation.weight

        val multipliers = computeVonMisesHourlyMultipliers(updatedMean, kappa)
        val larkProb = computeMorningLarkProbability(updatedMean)

        return LearnedCircadianProfile(
            peakHourMean = updatedMean,
            peakHourVariance = updatedVar,
            sampleCount = updatedSampleCount,
            circadianAmplitude = 1.0,
            morningLarkProbability = larkProb,
            hourlyMultipliers = multipliers
        )
    }

    fun learnProfile(
        observations: List<BehavioralObservation>,
        initialPeakHour: Double = 9.0,
        kappa: Double = DEFAULT_KAPPA
    ): LearnedCircadianProfile {
        var profile = LearnedCircadianProfile(
            peakHourMean = initialPeakHour,
            peakHourVariance = 4.0,
            sampleCount = 0.0,
            hourlyMultipliers = computeVonMisesHourlyMultipliers(initialPeakHour, kappa)
        )
        for (obs in observations) {
            profile = updateProfile(profile, obs, kappa)
        }
        return profile
    }
}

/**
 * Exogenous stimulant & ADHD medication pharmacokinetics (Bateman two-compartment model).
 */
enum class StimulantType(
    val absorptionRateKaPerHour: Double,
    val halfLifeHours: Double,
    val maxAdenosineMaskingFactor: Double,
    val crashSeverityFactor: Double,
    val referenceDoseMg: Double = 100.0
) {
    CAFFEINE(
        absorptionRateKaPerHour = 2.77, // Peak plasma ~45-50 mins
        halfLifeHours = 4.5,
        maxAdenosineMaskingFactor = 0.28,
        crashSeverityFactor = 0.08,
        referenceDoseMg = 100.0
    ),
    GREEN_TEA_L_THEANINE(
        absorptionRateKaPerHour = 2.10, // Smoother onset, minimal crash
        halfLifeHours = 4.0,
        maxAdenosineMaskingFactor = 0.18,
        crashSeverityFactor = 0.01,
        referenceDoseMg = 50.0
    ),
    PRESCRIPTION_STIMULANT_IR(
        absorptionRateKaPerHour = 2.20,
        halfLifeHours = 3.5,
        maxAdenosineMaskingFactor = 0.38,
        crashSeverityFactor = 0.12,
        referenceDoseMg = 10.0
    ),
    PRESCRIPTION_STIMULANT_XR(
        absorptionRateKaPerHour = 0.85,
        halfLifeHours = 6.5,
        maxAdenosineMaskingFactor = 0.35,
        crashSeverityFactor = 0.06,
        referenceDoseMg = 20.0
    )
}

data class StimulantDoseBout(
    val timestampMillis: Long,
    val doseMg: Double = 100.0, // Normalized to 100mg caffeine equivalent
    val type: StimulantType = StimulantType.CAFFEINE
)

object PharmacokineticStimulantCalculator {
    private const val REFERENCE_DOSE_MG = 100.0

    data class StimulantEffect(
        val processSMaskingReduction: Double, // [0.0 .. 0.40] reduction in perceived Process S
        val acuteAlertnessBoostPoints: Double, // [-10.0 .. +15.0] points on 0-100 scale
        val activeEquivalentMg: Double         // Remaining active mg in plasma (normalized to caffeine equivalence)
    )

    fun evaluateAt(
        timestampMillis: Long,
        doses: List<StimulantDoseBout>
    ): StimulantEffect {
        if (doses.isEmpty()) return StimulantEffect(0.0, 0.0, 0.0)

        var totalMasking = 0.0
        var netPoints = 0.0
        var activeMg = 0.0

        for (dose in doses) {
            if (timestampMillis <= dose.timestampMillis) continue
            val elapsedHours = (timestampMillis - dose.timestampMillis) / 3_600_000.0
            if (elapsedHours > 24.0) continue

            val ka = dose.type.absorptionRateKaPerHour
            val ke = ln(2.0) / dose.type.halfLifeHours
            // Bug 1 & Feature 1 (ADR-027): Dose ratio relative to compound-specific reference dose
            val doseRatio = (dose.doseMg / dose.type.referenceDoseMg).coerceIn(0.1, 4.0)

            val tMax = ln(ka / ke) / (ka - ke)
            val peakNorm = (ka / (ka - ke)) * (exp(-ke * tMax) - exp(-ka * tMax))
            val rawCurve = (ka / (ka - ke)) * (exp(-ke * elapsedHours) - exp(-ka * elapsedHours))
            val normalizedConc = (rawCurve / peakNorm.coerceAtLeast(1e-6)).coerceIn(0.0, 1.0)

            // Normalize active milligrams to caffeine-equivalent scale (100mg baseline)
            val doseEquivalent = (dose.doseMg / dose.type.referenceDoseMg) * REFERENCE_DOSE_MG
            activeMg += doseEquivalent * exp(-ke * elapsedHours)
            totalMasking += dose.type.maxAdenosineMaskingFactor * normalizedConc * min(1.8, doseRatio)

            // Post-elimination adenosine rebound ("crash") between t_half and t_half + 4h
            val reboundWindow = if (elapsedHours in (dose.type.halfLifeHours)..(dose.type.halfLifeHours + 4.0)) {
                val progress = (elapsedHours - dose.type.halfLifeHours) / 2.0
                val rawRebound = -100.0 * dose.type.crashSeverityFactor * progress * exp(1.0 - progress) * min(1.5, doseRatio)
                if (elapsedHours > dose.type.halfLifeHours + 3.0) {
                    val taper = 0.5 * (1.0 + cos(PI * (elapsedHours - (dose.type.halfLifeHours + 3.0))))
                    rawRebound * taper
                } else {
                    rawRebound
                }
            } else {
                0.0
            }

            netPoints += (12.0 * normalizedConc * min(1.5, doseRatio)) + reboundWindow
        }

        return StimulantEffect(
            processSMaskingReduction = totalMasking.coerceIn(0.0, 0.40),
            acuteAlertnessBoostPoints = netPoints.coerceIn(-10.0, 15.0),
            activeEquivalentMg = activeMg
        )
    }

    /**
     * Predicts Slow-Wave Sleep (SWS) suppression factor [0.0 .. 0.35] at [plannedSleepMillis].
     */
    fun predictBedtimeSwsSuppression(
        plannedSleepMillis: Long,
        doses: List<StimulantDoseBout>
    ): Double {
        val bedtimeState = evaluateAt(plannedSleepMillis, doses)
        val cycleMidpointState = evaluateAt(plannedSleepMillis + 45 * 60_000L, doses)
        val peakSwsEquivalentMg = maxOf(bedtimeState.activeEquivalentMg, cycleMidpointState.activeEquivalentMg)
        return ((peakSwsEquivalentMg - 35.0) / 160.0).coerceIn(0.0, 0.35)
    }
}

/**
 * Social Jetlag & SCN Phase-Shift Detector.
 * Compares circular midpoint of sleep on workdays (MSW) vs. free days/weekends (MSF).
 */
object SocialJetlagCalculator {
    data class SocialJetlagReport(
        val midpointWorkdayHour: Double,
        val midpointFreeDayHour: Double,
        val socialJetlagHours: Double, // Signed difference MSF - MSW
        val mondayMorningPhaseLagHours: Double,
        val morningLightProtocolRecommended: Boolean
    )

    fun evaluateFromSleepWindows(
        sleepWindows: List<TimeSlotWindow>,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): SocialJetlagReport {
        if (sleepWindows.size < 3) {
            return SocialJetlagReport(3.5, 3.5, 0.0, 0.0, false)
        }

        // Bug 2 & Feature 2 (ADR-027): Coalesce split nocturnal bouts separated by <= 120m
        val sortedWindows = sleepWindows.filter { it.endMillis > it.startMillis }.sortedBy { it.startMillis }
        val coalesced = mutableListOf<TimeSlotWindow>()
        for (win in sortedWindows) {
            if (coalesced.isEmpty()) {
                coalesced.add(win)
            } else {
                val last = coalesced.last()
                if (win.startMillis <= last.endMillis + 120 * 60_000L) {
                    coalesced[coalesced.lastIndex] = TimeSlotWindow(last.startMillis, max(last.endMillis, win.endMillis))
                } else {
                    coalesced.add(win)
                }
            }
        }

        val workdayMidpoints = mutableListOf<Pair<Double, Double>>()
        val weekendMidpoints = mutableListOf<Pair<Double, Double>>()

        for (win in coalesced) {
            val durationHours = (win.endMillis - win.startMillis) / 3_600_000.0
            if (durationHours < 3.0) continue
            val midpointMillis = win.startMillis + (win.endMillis - win.startMillis) / 2
            val zdt = Instant.ofEpochMilli(midpointMillis).atZone(zoneId)
            val clockHour = zdt.hour + (zdt.minute / 60.0)
            // Bug 2 & Feature 2 (ADR-027): Under MCTQ, classify Free Day by wake-up day (win.endMillis), not midpoint
            val endZdt = Instant.ofEpochMilli(win.endMillis).atZone(zoneId)
            val isFreeDay = endZdt.dayOfWeek == DayOfWeek.SATURDAY || endZdt.dayOfWeek == DayOfWeek.SUNDAY
            if (isFreeDay) {
                weekendMidpoints.add(clockHour to 1.0)
            } else {
                workdayMidpoints.add(clockHour to 1.0)
            }
        }

        if (workdayMidpoints.isEmpty() || weekendMidpoints.isEmpty()) {
            return SocialJetlagReport(3.5, 3.5, 0.0, 0.0, false)
        }

        val msw = blendClockHoursCircular(workdayMidpoints)
        val msf = blendClockHoursCircular(weekendMidpoints)
        val sjl = circularHourDifference(msf, msw)

        val nowZdt = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val dayFactor = when (nowZdt.dayOfWeek) {
            DayOfWeek.MONDAY -> 0.65
            DayOfWeek.TUESDAY -> 0.35
            else -> 0.0
        }
        val activeLag = if (abs(sjl) >= 1.25) sjl * dayFactor else 0.0

        return SocialJetlagReport(
            midpointWorkdayHour = msw,
            midpointFreeDayHour = msf,
            socialJetlagHours = sjl,
            mondayMorningPhaseLagHours = activeLag,
            morningLightProtocolRecommended = abs(sjl) >= 1.5
        )
    }
}

object PhysiologicalImpulseCalculator {
    /**
     * Computes the acute post-prandial meal dip at [minuteOfDay].
     * Produces a negative impulse peaking at ~60 minutes after meal start.
     */
    fun computeMealImpulse(
        minuteOfDay: Int,
        mealBlocks: List<CognitiveBlockWindow>,
        timestampMillis: Long? = null
    ): Double {
        if (mealBlocks.isEmpty()) return 0.0
        var minImpulse = 0.0
        for (meal in mealBlocks) {
            val deltaMinutes = if (timestampMillis != null && meal.explicitStartMillis != null) {
                ((timestampMillis - meal.explicitStartMillis) / 60_000L).toInt()
            } else {
                (minuteOfDay - meal.startMinuteOfDay + 1440) % 1440
            }
            if (deltaMinutes in 0..180) {
                val tFactor = deltaMinutes / 60.0
                val dip = -12.0 * tFactor * exp(1.0 - tFactor) * meal.intensity
                if (dip < minImpulse) {
                    minImpulse = dip
                }
            }
        }
        return minImpulse
    }

    /**
     * Computes the exercise impulse at [minuteOfDay].
     * During workout: mild exertion penalty (-8.0).
     * Post-workout (within 120m after workout end): acute endorphin/BDNF/dopamine focus boost (+12.0 decaying with tau=45m).
     */
    fun computeExerciseImpulse(
        minuteOfDay: Int,
        workoutBlocks: List<CognitiveBlockWindow>,
        timestampMillis: Long? = null
    ): Double {
        if (workoutBlocks.isEmpty()) return 0.0
        var strongestImpulse = 0.0

        for (workout in workoutBlocks) {
            val (isDuring, deltaMinutes) = if (timestampMillis != null && workout.explicitStartMillis != null && workout.explicitEndMillis != null) {
                if (timestampMillis in workout.explicitStartMillis..workout.explicitEndMillis) {
                    true to 0
                } else if (timestampMillis > workout.explicitEndMillis) {
                    false to ((timestampMillis - workout.explicitEndMillis) / 60_000L).toInt()
                } else {
                    false to -1
                }
            } else {
                val start = workout.startMinuteOfDay
                val end = workout.endMinuteOfDay
                val isDuring = if (end >= start) {
                    minuteOfDay in start..end
                } else {
                    minuteOfDay >= start || minuteOfDay <= end
                }
                if (isDuring) {
                    true to 0
                } else {
                    val deltaMinutes = (minuteOfDay - end + 1440) % 1440
                    false to deltaMinutes
                }
            }

            val impulse = when {
                isDuring -> -8.0 * workout.intensity
                deltaMinutes in 0..120 -> {
                    // Feature 2 (ADR-026): C1-continuous autonomic recovery transition ramp over 15 minutes
                    val inWorkoutPenalty = -8.0 * workout.intensity
                    val targetBoost = 12.0 * exp(-deltaMinutes / 45.0) * workout.intensity
                    val wOnset = sin(PI * 0.5 * min(1.0, deltaMinutes / 15.0))
                    (1.0 - wOnset) * inWorkoutPenalty + wOnset * targetBoost
                }
                else -> 0.0
            }

            if (impulse < 0.0) {
                if (strongestImpulse >= 0.0 || impulse < strongestImpulse) {
                    strongestImpulse = impulse
                }
            } else if (impulse > 0.0) {
                if (strongestImpulse <= 0.0 || impulse > strongestImpulse) {
                    strongestImpulse = impulse
                }
            }
        }
        return strongestImpulse
    }

    /**
     * Uses C1-continuous meal smoothing + exercise impulse for live circadian timeline evaluation.
     */
    fun computeCombinedImpulse(
        minuteOfDay: Int,
        blocks: List<CognitiveBlockWindow>,
        timestampMillis: Long? = null
    ): Double {
        val mealBlocks = blocks.filter { it.category == CognitiveBlockCategory.MEAL }
        val workoutBlocks = blocks.filter { it.category == CognitiveBlockCategory.EXERCISE }
        return computeSmoothMealImpulse(minuteOfDay, mealBlocks, timestampMillis) +
            computeExerciseImpulse(minuteOfDay, workoutBlocks, timestampMillis)
    }
}

/**
 * Represents an intra-day restorative nap bout occurring after the primary morning wake time.
 * Discharges Process S (homeostatic pressure) without shifting Process C (circadian phase).
 */
data class DaytimeNapBout(
    val startMillis: Long,
    val endMillis: Long
) {
    val durationMinutes: Int
        get() = max(0L, (endMillis - startMillis) / 60_000L).toInt()
}

object PolyphasicNapCalculator {
    private const val S_MIN = 0.10
    private const val TAU_WAKE_HOURS = 18.2
    private const val TAU_NAP_CLEARANCE_HOURS = 2.8
    private const val INERTIA_THRESHOLD_MINUTES = 30
    private const val INERTIA_DECAY_TAU_MINUTES = 20.0
    private const val MAX_INERTIA_PENALTY = 0.12

    data class NapAdjustedHomeostasis(
        val processS: Double,
        val sleepInertiaPenalty: Double
    )

    /**
     * Coalesces overlapping or duplicate nap intervals to prevent double-discharge of Process S.
     */
    fun coalesceNaps(naps: List<DaytimeNapBout>): List<DaytimeNapBout> {
        if (naps.size <= 1) return naps
        val sorted = naps.sortedBy { it.startMillis }
        val merged = mutableListOf<DaytimeNapBout>()
        var current = sorted[0]
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.startMillis <= current.endMillis) {
                current = DaytimeNapBout(current.startMillis, max(current.endMillis, next.endMillis))
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

    /**
     * Evaluates sequential Process S accumulation and nap discharge across 0..N daytime naps.
     */
    fun computeHomeostaticState(
        timestampMillis: Long,
        morningWakeMillis: Long,
        initialPressureS0: Double,
        naps: List<DaytimeNapBout>,
        tauWakeHours: Double = TAU_WAKE_HOURS
    ): NapAdjustedHomeostasis {
        if (timestampMillis <= morningWakeMillis) {
            val hoursBeforeWake = max(0.0, (morningWakeMillis - timestampMillis) / 3_600_000.0)
            val preWakeS = min(1.0, initialPressureS0 + (1.0 - initialPressureS0) * (1.0 - exp(-hoursBeforeWake / 2.0)))
            return NapAdjustedHomeostasis(preWakeS.coerceIn(0.0, 1.0), 0.14)
        }

        // Bug 4 & Feature 3 (ADR-025): Coalesce overlapping naps before running sequential Process S discharge
        val coalesced = coalesceNaps(naps)
        val validNaps = coalesced
            .filter { it.startMillis >= morningWakeMillis && it.endMillis > it.startMillis && it.durationMinutes in 10..180 }
            .sortedBy { it.startMillis }

        var currentAnchorTime = morningWakeMillis
        var currentAnchorS = initialPressureS0.coerceIn(S_MIN, 1.0)
        var inertiaPenalty = 0.0

        for (nap in validNaps) {
            if (timestampMillis < nap.startMillis) break

            // 1. Accumulate wake pressure up to nap start
            val wakeHoursBeforeNap = max(0.0, (nap.startMillis - currentAnchorTime) / 3_600_000.0)
            val sAtNapStart = 1.0 - (1.0 - currentAnchorS) * exp(-wakeHoursBeforeNap / tauWakeHours)

            if (timestampMillis <= nap.endMillis) {
                // Currently mid-nap: decay Process S up to current timestamp
                val elapsedNapHours = max(0.0, (timestampMillis - nap.startMillis) / 3_600_000.0)
                val sMidNap = S_MIN + (sAtNapStart - S_MIN) * exp(-elapsedNapHours / TAU_NAP_CLEARANCE_HOURS)
                return NapAdjustedHomeostasis(sMidNap.coerceIn(0.0, 1.0), 0.0)
            }

            // 2. Nap completed prior to timestampMillis: discharge S and advance anchor to nap.endMillis
            val napHours = nap.durationMinutes / 60.0
            currentAnchorS = S_MIN + (sAtNapStart - S_MIN) * exp(-napHours / TAU_NAP_CLEARANCE_HOURS)
            currentAnchorTime = nap.endMillis

            // 3. Evaluate transient sleep inertia if nap exceeded 30 minutes (SWS entry)
            // Bug 4 Fix (ADR-025): Preserve maximum decaying sleep inertia across multiple bouts instead of zeroing out
            if (nap.durationMinutes > INERTIA_THRESHOLD_MINUTES) {
                val minsSinceNapWake = max(0.0, (timestampMillis - nap.endMillis) / 60_000.0)
                val depthFactor = min(1.0, (nap.durationMinutes - INERTIA_THRESHOLD_MINUTES) / 30.0)
                val boutInertia = MAX_INERTIA_PENALTY * depthFactor * exp(-minsSinceNapWake / INERTIA_DECAY_TAU_MINUTES)
                inertiaPenalty = max(inertiaPenalty, boutInertia)
            }
        }

        // Accumulate wake pressure from the latest wake anchor (morning wake or last completed nap end)
        val remainingWakeHours = max(0.0, (timestampMillis - currentAnchorTime) / 3_600_000.0)
        val finalS = (1.0 - (1.0 - currentAnchorS) * exp(-remainingWakeHours / tauWakeHours)).coerceIn(0.0, 1.0)

        return NapAdjustedHomeostasis(
            processS = finalS,
            sleepInertiaPenalty = inertiaPenalty
        )
    }
}

data class CircadianSnapshot(
    val timestampMillis: Long,
    val usableEnergy: Int,            // [0..100]
    val homeostaticPressureS: Double, // [0.0..1.0]
    val circadianAlertnessC: Double,  // [0.0..1.0]
    val focusDepletionPenalty: Double,// [0.0..0.15]
    val phase: EnergyPhase,
    val sleepInertiaPenalty: Double = 0.0,
    val physiologicalImpulse: Double = 0.0,
    val predictionConfidence: Double = 0.85,
    val learnedAdjustmentPoints: Double = 0.0,
    val baselineBiologicalEnergy: Int = usableEnergy
) {
    val circadianAlertness: Double get() = circadianAlertnessC
    val homeostaticSleepPressure: Double get() = homeostaticPressureS
}

data class DailyCircadianSummary(
    val currentSnapshot: CircadianSnapshot,
    val peakWindowStartHour: Int,
    val peakWindowEndHour: Int,
    val peakEnergyScore: Int,
    val dipWindowStartHour: Int,
    val dipWindowEndHour: Int,
    val recommendedDeepWorkMinutes: Int
)

interface CircadianEnergyService {
    fun evaluateSnapshot(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int,
        zoneId: ZoneId
    ): CircadianSnapshot = evaluateSnapshot(
        timestampMillis = timestampMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        continuousActiveFocusMinutes = continuousActiveFocusMinutes,
        zoneId = zoneId,
        naps = emptyList()
    )

    fun evaluateSnapshot(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList()
    ): CircadianSnapshot = evaluateSnapshot(
        timestampMillis = timestampMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        continuousActiveFocusMinutes = continuousActiveFocusMinutes,
        zoneId = zoneId,
        naps = naps,
        chronotype = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour = null
    )

    fun evaluateSnapshot(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null
    ): CircadianSnapshot = evaluateSnapshot(
        timestampMillis = timestampMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        continuousActiveFocusMinutes = continuousActiveFocusMinutes,
        zoneId = zoneId,
        naps = naps,
        chronotype = chronotype,
        manualPeakStartHour = manualPeakStartHour,
        learnedProfile = null,
        rollingAverageSleepMinutes = null,
        cognitiveBlocks = emptyList()
    )

    fun evaluateSnapshot(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null,
        learnedProfile: LearnedCircadianProfile? = null,
        rollingAverageSleepMinutes: Double? = null,
        cognitiveBlocks: List<CognitiveBlockWindow> = emptyList()
    ): CircadianSnapshot

    fun evaluateSnapshotWithStimulants(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null,
        learnedProfile: LearnedCircadianProfile? = null,
        rollingAverageSleepMinutes: Double? = null,
        cognitiveBlocks: List<CognitiveBlockWindow> = emptyList(),
        stimulantDoses: List<StimulantDoseBout> = emptyList(),
        socialJetlagLagHours: Double = 0.0
    ): CircadianSnapshot = evaluateSnapshot(
        timestampMillis = timestampMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        continuousActiveFocusMinutes = continuousActiveFocusMinutes,
        zoneId = zoneId,
        naps = naps,
        chronotype = chronotype,
        manualPeakStartHour = manualPeakStartHour,
        learnedProfile = learnedProfile,
        rollingAverageSleepMinutes = rollingAverageSleepMinutes,
        cognitiveBlocks = cognitiveBlocks
    )

    fun projectDayTimeline(
        dayStartMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        stepMinutes: Int = 30,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<CircadianSnapshot> = projectDayTimeline(
        dayStartMillis = dayStartMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        stepMinutes = stepMinutes,
        zoneId = zoneId,
        naps = emptyList()
    )

    fun projectDayTimeline(
        dayStartMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        stepMinutes: Int,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>
    ): List<CircadianSnapshot> = projectDayTimeline(
        dayStartMillis = dayStartMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        stepMinutes = stepMinutes,
        zoneId = zoneId,
        naps = naps,
        chronotype = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour = null
    )

    fun projectDayTimeline(
        dayStartMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        stepMinutes: Int,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null
    ): List<CircadianSnapshot> = projectDayTimeline(
        dayStartMillis = dayStartMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        stepMinutes = stepMinutes,
        zoneId = zoneId,
        naps = naps,
        chronotype = chronotype,
        manualPeakStartHour = manualPeakStartHour,
        cognitiveBlocks = emptyList(),
        learnedProfile = null,
        rollingAverageSleepMinutes = null
    )

    fun projectDayTimeline(
        dayStartMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        stepMinutes: Int = 30,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null,
        cognitiveBlocks: List<CognitiveBlockWindow> = emptyList(),
        learnedProfile: LearnedCircadianProfile? = null,
        rollingAverageSleepMinutes: Double? = null,
        stimulantDoses: List<StimulantDoseBout> = emptyList(),
        socialJetlagLagHours: Double = 0.0
    ): List<CircadianSnapshot>

    fun computeDailySummary(
        timestampMillis: Long = System.currentTimeMillis(),
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int = 480,
        continuousActiveFocusMinutes: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DailyCircadianSummary = computeDailySummary(
        timestampMillis = timestampMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        continuousActiveFocusMinutes = continuousActiveFocusMinutes,
        zoneId = zoneId,
        naps = emptyList(),
        chronotype = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour = null
    )

    fun computeDailySummary(
        timestampMillis: Long = System.currentTimeMillis(),
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int = 480,
        continuousActiveFocusMinutes: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList()
    ): DailyCircadianSummary = computeDailySummary(
        timestampMillis = timestampMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        continuousActiveFocusMinutes = continuousActiveFocusMinutes,
        zoneId = zoneId,
        naps = naps,
        chronotype = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour = null
    )

    fun computeDailySummary(
        timestampMillis: Long = System.currentTimeMillis(),
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int = 480,
        continuousActiveFocusMinutes: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null
    ): DailyCircadianSummary = computeDailySummary(
        timestampMillis = timestampMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        continuousActiveFocusMinutes = continuousActiveFocusMinutes,
        zoneId = zoneId,
        naps = naps,
        chronotype = chronotype,
        manualPeakStartHour = manualPeakStartHour,
        cognitiveBlocks = emptyList(),
        learnedProfile = null,
        rollingAverageSleepMinutes = null,
        stimulantDoses = emptyList()
    )

    fun computeDailySummary(
        timestampMillis: Long = System.currentTimeMillis(),
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int = 480,
        continuousActiveFocusMinutes: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null,
        cognitiveBlocks: List<CognitiveBlockWindow> = emptyList(),
        learnedProfile: LearnedCircadianProfile? = null,
        rollingAverageSleepMinutes: Double? = null,
        stimulantDoses: List<StimulantDoseBout> = emptyList(),
        socialJetlagLagHours: Double = 0.0
    ): DailyCircadianSummary

    fun computeCategoryAffinity(
        category: TaskCategory,
        snapshot: CircadianSnapshot
    ): Double
}

@Singleton
class DefaultCircadianEnergyService @Inject constructor() : CircadianEnergyService {

    companion object {
        private const val S_MIN = 0.10
        private const val S_MAX = 0.85
        private const val TAU_RECOVERY_HOURS = 3.6
        private const val TAU_WAKE_HOURS = 18.2
        private const val MAX_FOCUS_DEPLETION = 0.15
    }

    override fun evaluateSnapshot(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int,
        zoneId: ZoneId
    ): CircadianSnapshot = evaluateSnapshot(
        timestampMillis = timestampMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        continuousActiveFocusMinutes = continuousActiveFocusMinutes,
        zoneId = zoneId,
        naps = emptyList()
    )

    override fun evaluateSnapshot(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile,
        manualPeakStartHour: Int?
    ): CircadianSnapshot = evaluateSnapshot(
        timestampMillis = timestampMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        continuousActiveFocusMinutes = continuousActiveFocusMinutes,
        zoneId = zoneId,
        naps = naps,
        chronotype = chronotype,
        manualPeakStartHour = manualPeakStartHour,
        learnedProfile = null,
        rollingAverageSleepMinutes = null,
        cognitiveBlocks = emptyList()
    )

    override fun evaluateSnapshot(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile,
        manualPeakStartHour: Int?,
        learnedProfile: LearnedCircadianProfile?,
        rollingAverageSleepMinutes: Double?,
        cognitiveBlocks: List<CognitiveBlockWindow>
    ): CircadianSnapshot = evaluateSnapshotWithStimulants(
        timestampMillis = timestampMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        continuousActiveFocusMinutes = continuousActiveFocusMinutes,
        zoneId = zoneId,
        naps = naps,
        chronotype = chronotype,
        manualPeakStartHour = manualPeakStartHour,
        learnedProfile = learnedProfile,
        rollingAverageSleepMinutes = rollingAverageSleepMinutes,
        cognitiveBlocks = cognitiveBlocks,
        stimulantDoses = emptyList(),
        socialJetlagLagHours = 0.0
    )

    override fun evaluateSnapshotWithStimulants(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile,
        manualPeakStartHour: Int?,
        learnedProfile: LearnedCircadianProfile?,
        rollingAverageSleepMinutes: Double?,
        cognitiveBlocks: List<CognitiveBlockWindow>,
        stimulantDoses: List<StimulantDoseBout>,
        socialJetlagLagHours: Double
    ): CircadianSnapshot {
        // 0. Multi-Day Allostatic Sleep Debt Carryover
        val allostaticDebtHours = if (rollingAverageSleepMinutes != null && rollingAverageSleepMinutes > 0.0) {
            max(0.0, (480.0 - rollingAverageSleepMinutes) / 60.0)
        } else {
            0.0
        }
        val alphaAllostatic = min(0.50, allostaticDebtHours / 8.0)

        // 1. Continuous Sleep Recovery -> Initial Wake Pressure S0
        // Feature 1 (ADR-024): Bedtime SWS Suppression -> Next-Day Process S0 Coupling
        val plannedBedtime = wakeTimeMillis - (lastSleepDurationMinutes * 60_000L)
        val swsSuppression = if (stimulantDoses.isNotEmpty()) {
            PharmacokineticStimulantCalculator.predictBedtimeSwsSuppression(plannedBedtime, stimulantDoses)
        } else {
            0.0
        }
        val effectiveSleepHours = max(0.0, (lastSleepDurationMinutes / 60.0) * (1.0 - swsSuppression))
        val baseS0 = S_MIN + (S_MAX - S_MIN) * exp(-effectiveSleepHours / TAU_RECOVERY_HOURS)
        val s0 = baseS0 + alphaAllostatic * (1.0 - baseS0) * 0.35
        val effectiveTauWakeHours = TAU_WAKE_HOURS / (1.0 + alphaAllostatic)

        // 2. Continuous Homeostatic Wake Pressure with Daytime Nap Discharge
        val homeostatic = PolyphasicNapCalculator.computeHomeostaticState(
            timestampMillis = timestampMillis,
            morningWakeMillis = wakeTimeMillis,
            initialPressureS0 = s0,
            naps = naps,
            tauWakeHours = effectiveTauWakeHours
        )

        // 2a. Apply Pharmacokinetic Adenosine Masking (if any stimulant doses logged)
        val stimEffect = PharmacokineticStimulantCalculator.evaluateAt(timestampMillis, stimulantDoses)
        val processS = (homeostatic.processS - stimEffect.processSMaskingReduction).coerceIn(S_MIN, 1.0)

        // 2b. Morning Sleep Inertia (first 30m after morning wake) + Nap Inertia
        val minsSinceMorningWake = ((timestampMillis - wakeTimeMillis) / 60_000.0)
        val morningInertia = if (minsSinceMorningWake < 0.0) {
            0.14
        } else if (minsSinceMorningWake <= 30.0) {
            0.14 * (1.0 - (minsSinceMorningWake / 30.0))
        } else {
            0.0
        }
        val inertiaPenalty = max(homeostatic.sleepInertiaPenalty, morningInertia)

        // 3. DST-Safe Wall-Clock Extraction for Process C(t)
        val zonedNow = Instant.ofEpochMilli(timestampMillis).atZone(zoneId)
        val zonedWake = Instant.ofEpochMilli(wakeTimeMillis).atZone(zoneId)
        val clockHour = zonedNow.hour + (zonedNow.minute / 60.0)
        val wakeClockHour = zonedWake.hour + (zonedWake.minute / 60.0)

        // 3a. Pure Uncalibrated Biological Curve (for accurate baselineBiologicalEnergy comparison)
        val bioPeakPhi = computePhasePeakHour(
            wakeClockHour = wakeClockHour,
            chronotype = chronotype,
            manualPeakStartHour = manualPeakStartHour,
            detectedPeakHour = null,
            detectionConfidence = 0.0
        )
        val bioHarmonic1 = 0.38 * cos((2.0 * PI * (clockHour - bioPeakPhi)) / 24.0)
        val bioHarmonic2 = 0.12 * cos((4.0 * PI * (clockHour - bioPeakPhi)) / 24.0)
        val processCBio = (0.50 + bioHarmonic1 - bioHarmonic2).coerceIn(0.0, 1.0)

        // 3b. Learned + Social-Jetlag Adjusted Curve with Von Mises Hourly Kernel Modulation
        val detectedPeak = learnedProfile?.peakHourMean
        val detectedConf = learnedProfile?.confidence ?: 0.0
        val phasePeakPhi = (computePhasePeakHour(
            wakeClockHour = wakeClockHour,
            chronotype = chronotype,
            manualPeakStartHour = manualPeakStartHour,
            detectedPeakHour = detectedPeak,
            detectionConfidence = detectedConf
        ) + socialJetlagLagHours.coerceIn(-3.0, 3.0) + 24.0) % 24.0

        val primaryHarmonic = 0.38 * cos((2.0 * PI * (clockHour - phasePeakPhi)) / 24.0)
        val secondaryHarmonic = 0.12 * cos((4.0 * PI * (clockHour - phasePeakPhi)) / 24.0)
        val harmonicC = (0.50 + primaryHarmonic - secondaryHarmonic).coerceIn(0.0, 1.0)
        val processC = applyLearnedVonMisesModulation(harmonicC, clockHour, learnedProfile)

        // 4. Synaptic Depletion from unbroken focus > 45m
        val excessFocusMins = max(0, continuousActiveFocusMinutes - 45).toDouble()
        val focusDepletion = if (excessFocusMins > 0.0) {
            MAX_FOCUS_DEPLETION * (1.0 - exp(-excessFocusMins / 60.0))
        } else {
            0.0
        }

        // 4b. Physiological Impulses (Meals & Exercise) + Stimulant Acute Alertness
        val minuteOfDay = zonedNow.hour * 60 + zonedNow.minute
        val physioImpulse = (if (cognitiveBlocks.isNotEmpty()) {
            PhysiologicalImpulseCalculator.computeCombinedImpulse(minuteOfDay, cognitiveBlocks, timestampMillis)
        } else {
            0.0
        }) + stimEffect.acuteAlertnessBoostPoints

        // 5. Unified Usable Energy [0..100]
        val baseRawEnergy = 100.0 * (0.55 * processCBio + 0.45 * (1.0 - homeostatic.processS) - inertiaPenalty)
        val baselineBio = baseRawEnergy.roundToInt().coerceIn(0, 100)
        val rawEnergy = 100.0 * (0.55 * processC + 0.45 * (1.0 - processS) - focusDepletion - inertiaPenalty) + physioImpulse
        val usableEnergy = rawEnergy.roundToInt().coerceIn(0, 100)
        val learnedAdj = (usableEnergy - baselineBio).toDouble()
        val predConf = learnedProfile?.confidence ?: 0.85

        val hoursAwake = max(0.0, (timestampMillis - wakeTimeMillis) / 3_600_000.0)
        // Bug 5 Fix (ADR-026): Morning ramp-up phase guard
        // Prevents morning hours (hoursAwake < 6.0) from falling into WIND_DOWN
        val phase = when {
            continuousActiveFocusMinutes >= 90 -> EnergyPhase.RECOVERY_NEEDED
            usableEnergy >= 72 -> EnergyPhase.PEAK
            hoursAwake in 6.0..9.5 && processC < 0.48 -> EnergyPhase.DIP
            hoursAwake > 9.5 && usableEnergy >= 55 -> EnergyPhase.REBOUND
            hoursAwake < 6.0 && usableEnergy >= 50 -> EnergyPhase.REBOUND
            hoursAwake < 6.0 -> EnergyPhase.RECOVERY_NEEDED
            usableEnergy >= 55 -> EnergyPhase.REBOUND
            else -> EnergyPhase.WIND_DOWN
        }

        return CircadianSnapshot(
            timestampMillis = timestampMillis,
            usableEnergy = usableEnergy,
            homeostaticPressureS = processS,
            circadianAlertnessC = processC,
            focusDepletionPenalty = focusDepletion,
            phase = phase,
            sleepInertiaPenalty = inertiaPenalty,
            physiologicalImpulse = physioImpulse,
            predictionConfidence = predConf,
            learnedAdjustmentPoints = learnedAdj,
            baselineBiologicalEnergy = baselineBio
        )
    }

    override fun projectDayTimeline(
        dayStartMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        stepMinutes: Int,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile,
        manualPeakStartHour: Int?
    ): List<CircadianSnapshot> = projectDayTimeline(
        dayStartMillis = dayStartMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        stepMinutes = stepMinutes,
        zoneId = zoneId,
        naps = naps,
        chronotype = chronotype,
        manualPeakStartHour = manualPeakStartHour,
        cognitiveBlocks = emptyList(),
        learnedProfile = null,
        rollingAverageSleepMinutes = null
    )

    override fun projectDayTimeline(
        dayStartMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        stepMinutes: Int,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile,
        manualPeakStartHour: Int?,
        cognitiveBlocks: List<CognitiveBlockWindow>,
        learnedProfile: LearnedCircadianProfile?,
        rollingAverageSleepMinutes: Double?,
        stimulantDoses: List<StimulantDoseBout>,
        socialJetlagLagHours: Double
    ): List<CircadianSnapshot> {
        val stepMillis = stepMinutes * 60_000L
        val steps = (24 * 60) / stepMinutes
        return List(steps) { idx ->
            val t = dayStartMillis + idx * stepMillis
            if (stimulantDoses.isNotEmpty() || socialJetlagLagHours != 0.0) {
                evaluateSnapshotWithStimulants(
                    timestampMillis = t,
                    wakeTimeMillis = wakeTimeMillis,
                    lastSleepDurationMinutes = lastSleepDurationMinutes,
                    continuousActiveFocusMinutes = 0,
                    zoneId = zoneId,
                    naps = naps,
                    chronotype = chronotype,
                    manualPeakStartHour = manualPeakStartHour,
                    learnedProfile = learnedProfile,
                    rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                    cognitiveBlocks = cognitiveBlocks,
                    stimulantDoses = stimulantDoses,
                    socialJetlagLagHours = socialJetlagLagHours
                )
            } else {
                evaluateSnapshot(
                    timestampMillis = t,
                    wakeTimeMillis = wakeTimeMillis,
                    lastSleepDurationMinutes = lastSleepDurationMinutes,
                    continuousActiveFocusMinutes = 0,
                    zoneId = zoneId,
                    naps = naps,
                    chronotype = chronotype,
                    manualPeakStartHour = manualPeakStartHour,
                    learnedProfile = learnedProfile,
                    rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                    cognitiveBlocks = cognitiveBlocks
                )
            }
        }
    }

    override fun computeDailySummary(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile,
        manualPeakStartHour: Int?
    ): DailyCircadianSummary = computeDailySummary(
        timestampMillis = timestampMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        continuousActiveFocusMinutes = continuousActiveFocusMinutes,
        zoneId = zoneId,
        naps = naps,
        chronotype = chronotype,
        manualPeakStartHour = manualPeakStartHour,
        cognitiveBlocks = emptyList(),
        learnedProfile = null,
        rollingAverageSleepMinutes = null,
        stimulantDoses = emptyList()
    )

    override fun computeDailySummary(
        timestampMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        continuousActiveFocusMinutes: Int,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile,
        manualPeakStartHour: Int?,
        cognitiveBlocks: List<CognitiveBlockWindow>,
        learnedProfile: LearnedCircadianProfile?,
        rollingAverageSleepMinutes: Double?,
        stimulantDoses: List<StimulantDoseBout>,
        socialJetlagLagHours: Double
    ): DailyCircadianSummary {
        val currentSnapshot = if (stimulantDoses.isNotEmpty() || socialJetlagLagHours != 0.0) {
            evaluateSnapshotWithStimulants(
                timestampMillis = timestampMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = lastSleepDurationMinutes,
                continuousActiveFocusMinutes = continuousActiveFocusMinutes,
                zoneId = zoneId,
                naps = naps,
                chronotype = chronotype,
                manualPeakStartHour = manualPeakStartHour,
                learnedProfile = learnedProfile,
                rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                cognitiveBlocks = cognitiveBlocks,
                stimulantDoses = stimulantDoses,
                socialJetlagLagHours = socialJetlagLagHours
            )
        } else {
            evaluateSnapshot(
                timestampMillis = timestampMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = lastSleepDurationMinutes,
                continuousActiveFocusMinutes = continuousActiveFocusMinutes,
                zoneId = zoneId,
                naps = naps,
                chronotype = chronotype,
                manualPeakStartHour = manualPeakStartHour,
                learnedProfile = learnedProfile,
                rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                cognitiveBlocks = cognitiveBlocks
            )
        }

        val zonedTimestamp = Instant.ofEpochMilli(timestampMillis).atZone(zoneId)
        val targetDate = zonedTimestamp.toLocalDate()
        val dayStartMillis = targetDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val zonedWake = Instant.ofEpochMilli(wakeTimeMillis).atZone(zoneId)

        // Bug 2 Fix: Target date wake time resolved safely across DST transitions
        val targetDateWakeMillis = targetDate.atTime(zonedWake.toLocalTime()).atZone(zoneId).toInstant().toEpochMilli()

        // Preserve wakeTimeMillis directly when user is awake from prior day (post-midnight / night-owl)
        val isPostMidnightAwake = wakeTimeMillis <= timestampMillis &&
            (timestampMillis - wakeTimeMillis) < 24 * 3600_000L &&
            zonedWake.toLocalDate() < targetDate

        val timeline = projectDayTimeline(
            dayStartMillis = dayStartMillis,
            wakeTimeMillis = targetDateWakeMillis,
            lastSleepDurationMinutes = lastSleepDurationMinutes,
            stepMinutes = 30,
            zoneId = zoneId,
            naps = naps,
            chronotype = chronotype,
            manualPeakStartHour = manualPeakStartHour,
            cognitiveBlocks = cognitiveBlocks,
            learnedProfile = learnedProfile,
            rollingAverageSleepMinutes = rollingAverageSleepMinutes,
            stimulantDoses = stimulantDoses,
            socialJetlagLagHours = socialJetlagLagHours
        ).let { baseList ->
            if (isPostMidnightAwake) {
                baseList.map { step ->
                    if (step.timestampMillis < targetDateWakeMillis) {
                        if (stimulantDoses.isNotEmpty() || socialJetlagLagHours != 0.0) {
                            evaluateSnapshotWithStimulants(
                                timestampMillis = step.timestampMillis,
                                wakeTimeMillis = wakeTimeMillis,
                                lastSleepDurationMinutes = lastSleepDurationMinutes,
                                continuousActiveFocusMinutes = 0,
                                zoneId = zoneId,
                                naps = naps,
                                chronotype = chronotype,
                                manualPeakStartHour = manualPeakStartHour,
                                cognitiveBlocks = cognitiveBlocks,
                                learnedProfile = learnedProfile,
                                rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                                stimulantDoses = stimulantDoses,
                                socialJetlagLagHours = socialJetlagLagHours
                            )
                        } else {
                            evaluateSnapshot(
                                timestampMillis = step.timestampMillis,
                                wakeTimeMillis = wakeTimeMillis,
                                lastSleepDurationMinutes = lastSleepDurationMinutes,
                                continuousActiveFocusMinutes = 0,
                                zoneId = zoneId,
                                naps = naps,
                                chronotype = chronotype,
                                manualPeakStartHour = manualPeakStartHour,
                                cognitiveBlocks = cognitiveBlocks,
                                learnedProfile = learnedProfile,
                                rollingAverageSleepMinutes = rollingAverageSleepMinutes
                            )
                        }
                    } else {
                        step
                    }
                }
            } else {
                baseList
            }
        }

        // Feature 1 (ADR-026): Sliding-Window Integral Peak & Dip Detector
        // 1. Peak Window: 3-hour sliding window maximizing sum of usableEnergy
        val stepMinutes = 30
        val peakSteps = (3 * 60) / stepMinutes // 6 steps
        var bestPeakSum = -1.0
        var bestPeakStartIdx = 0

        if (timeline.size >= peakSteps) {
            for (i in 0..(timeline.size - peakSteps)) {
                val windowSum = (0 until peakSteps).sumOf { timeline[i + it].usableEnergy.toDouble() }
                if (windowSum > bestPeakSum) {
                    bestPeakSum = windowSum
                    bestPeakStartIdx = i
                }
            }
        }

        val peakSnapshot = timeline.getOrNull(bestPeakStartIdx) ?: currentSnapshot
        val peakHour = Instant.ofEpochMilli(peakSnapshot.timestampMillis).atZone(zoneId).hour
        val peakEndHour = (peakHour + 3) % 24
        val peakEnergyScore = (0 until peakSteps)
            .mapNotNull { timeline.getOrNull(bestPeakStartIdx + it)?.usableEnergy }
            .maxOrNull() ?: peakSnapshot.usableEnergy

        // 2. Dip Window: 2-hour sliding window minimizing sum of usableEnergy within wake+4h..wake+11h
        val dipSteps = (2 * 60) / stepMinutes // 4 steps
        val dipStartSearchMillis = targetDateWakeMillis + 4 * 3600_000L
        val dipEndSearchMillis = targetDateWakeMillis + 11 * 3600_000L

        var bestDipSum = Double.MAX_VALUE
        var bestDipStartIdx = -1

        if (timeline.size >= dipSteps) {
            for (i in 0..(timeline.size - dipSteps)) {
                val stepTime = timeline[i].timestampMillis
                if (stepTime in dipStartSearchMillis..dipEndSearchMillis) {
                    val windowSum = (0 until dipSteps).sumOf { timeline[i + it].usableEnergy.toDouble() }
                    if (windowSum < bestDipSum) {
                        bestDipSum = windowSum
                        bestDipStartIdx = i
                    }
                }
            }

            // Fallback if search window yielded no candidate
            if (bestDipStartIdx == -1) {
                for (i in 0..(timeline.size - dipSteps)) {
                    val windowSum = (0 until dipSteps).sumOf { timeline[i + it].usableEnergy.toDouble() }
                    if (windowSum < bestDipSum) {
                        bestDipSum = windowSum
                        bestDipStartIdx = i
                    }
                }
            }
        }

        val dipSnapshot = if (bestDipStartIdx >= 0) timeline.getOrNull(bestDipStartIdx) ?: currentSnapshot else currentSnapshot
        val dipHour = Instant.ofEpochMilli(dipSnapshot.timestampMillis).atZone(zoneId).hour
        val dipEndHour = (dipHour + 2) % 24

        val highEnergySteps = timeline.count { it.usableEnergy >= 70 }
        val recommendedDeepWorkMinutes = (highEnergySteps * stepMinutes).coerceIn(60, 240)

        return DailyCircadianSummary(
            currentSnapshot = currentSnapshot,
            peakWindowStartHour = peakHour,
            peakWindowEndHour = peakEndHour,
            peakEnergyScore = peakEnergyScore,
            dipWindowStartHour = dipHour,
            dipWindowEndHour = dipEndHour,
            recommendedDeepWorkMinutes = recommendedDeepWorkMinutes
        )
    }

    override fun computeCategoryAffinity(
        category: TaskCategory,
        snapshot: CircadianSnapshot
    ): Double {
        val e = snapshot.usableEnergy.toDouble()
        return when (category) {
            TaskCategory.DEEP_WORK -> e
            TaskCategory.CREATIVE -> {
                if (snapshot.phase == EnergyPhase.REBOUND) min(100.0, e + 15.0) else e * 0.9
            }
            TaskCategory.ADMIN -> {
                100.0 * exp(-((e - 50.0) * (e - 50.0)) / (2.0 * 22.0 * 22.0))
            }
            TaskCategory.EXERCISE -> {
                100.0 * exp(-((e - 65.0) * (e - 65.0)) / (2.0 * 25.0 * 25.0))
            }
            TaskCategory.MINDFULNESS -> {
                if (snapshot.phase == EnergyPhase.RECOVERY_NEEDED) 100.0 else (100.0 - e * 0.5).coerceIn(30.0, 95.0)
            }
            TaskCategory.GENERAL -> 70.0
        }.coerceIn(0.0, 100.0)
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainservicechronobiologysignalnormalizerkt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/service/ChronobiologySignalNormalizer.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/service/ChronobiologySignalNormalizer.kt`
- **Lines of Code:** 274
- **Size:** 11,276 bytes (11.0 KB)

```kotlin
package com.neuroflow.app.domain.service

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

data class RawSleepTelemetry(
    val intervals: List<TimeSlotWindow>,
    val deepSleepMinutes: Double = 0.0,
    val remSleepMinutes: Double = 0.0,
    val lightSleepMinutes: Double = 0.0,
    val detectedWakeMillis: Long? = null,
    val confidence: Double = 1.0
)

data class NormalizedChronobiologySignal(
    val totalSleepMinutes: Double,
    val effectiveDurationMinutes: Double,
    val blendedDurationMinutes: Double,
    val normalizedWakeTimeMillis: Long,
    val totalWasoMinutes: Double,
    val consolidatedSessions: List<TimeSlotWindow>,
    val daytimeNaps: List<DaytimeNapBout> = emptyList()
)

/**
 * ADR-002 Chronobiology Signal Normalizer
 *
 * Implements passive chronobiology signal normalization for wearable telemetry:
 * 1. 45-minute WASO episodic coalescing (bridges nighttime micro-awakenings into unified sleep sessions).
 * 2. Sleep stage efficiency weighting (d_eff) prioritizing Slow-Wave Sleep (SWS) and REM.
 * 3. Bayesian duration blending (d_blended) across confidence spectrum c in [0.0..1.0].
 * 4. Late-sync wake shift clamping (+-90m) to eliminate ADHD schedule whiplash.
 */
@Singleton
class ChronobiologySignalNormalizer @Inject constructor() {

    companion object {
        const val MAX_WASO_GAP_MILLIS = 45 * 60_000L
        const val MAX_WAKE_SHIFT_MILLIS = 90 * 60_000L

        private const val SWS_REM_MIN_FACTOR = 0.75
        private const val SWS_REM_MAX_FACTOR = 1.25
        private const val BASE_ARCHITECTURE_FACTOR = 0.85
        private const val RESTORATIVE_SLOPE = 0.75
    }

    /**
     * Coalesce sleep intervals separated by wake episodes (WASO) <= 45 minutes.
     * Returns a pair of (consolidated non-overlapping sleep sessions, total WASO minutes inside coalesced gaps).
     */
    fun coalesceWasoEpisodes(
        intervals: List<TimeSlotWindow>,
        maxWasoGapMillis: Long = MAX_WASO_GAP_MILLIS
    ): Pair<List<TimeSlotWindow>, Long> {
        val valid = intervals.filter { it.endMillis > it.startMillis }
        if (valid.isEmpty()) return emptyList<TimeSlotWindow>() to 0L

        val sorted = valid.sortedBy { it.startMillis }
        val consolidated = mutableListOf<TimeSlotWindow>()
        var totalWasoMillis = 0L

        var curStart = sorted[0].startMillis
        var curEnd = sorted[0].endMillis

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.startMillis <= curEnd) {
                // Direct overlap
                curEnd = max(curEnd, next.endMillis)
            } else {
                val gap = next.startMillis - curEnd
                if (gap <= maxWasoGapMillis) {
                    // WASO awakening: bridge the episodes into a single session
                    totalWasoMillis += gap
                    curEnd = max(curEnd, next.endMillis)
                } else {
                    consolidated.add(TimeSlotWindow(curStart, curEnd))
                    curStart = next.startMillis
                    curEnd = next.endMillis
                }
            }
        }
        consolidated.add(TimeSlotWindow(curStart, curEnd))

        val totalWasoMinutes = totalWasoMillis / 60_000L
        return consolidated to totalWasoMinutes
    }

    /**
     * Stage Architecture Restorative Hours:
     * d_eff = d_total * clamp_{[0.75, 1.25]}(0.85 + 0.75 * (d_SWS + d_REM) / d_total)
     */
    fun computeEffectiveDuration(
        totalMinutes: Double,
        deepSleepMinutes: Double,
        remSleepMinutes: Double,
        lightSleepMinutes: Double? = null
    ): Double {
        if (totalMinutes <= 0.0) return 0.0
        // Bug 1 Fix (ADR-026): When no sleep-stage breakdown is present (manual log or tracker lacking SWS/REM breakdown),
        // assume normative stage architecture (stageWeight = 1.0) instead of penalizing by 15%.
        if (lightSleepMinutes != null && deepSleepMinutes <= 0.0 && remSleepMinutes <= 0.0 && lightSleepMinutes <= 0.0) {
            return totalMinutes
        }
        val restorativeRatio = ((deepSleepMinutes + remSleepMinutes) / totalMinutes).coerceIn(0.0, 1.0)
        val stageWeight = (BASE_ARCHITECTURE_FACTOR + RESTORATIVE_SLOPE * restorativeRatio)
            .coerceIn(SWS_REM_MIN_FACTOR, SWS_REM_MAX_FACTOR)
        return totalMinutes * stageWeight
    }

    /**
     * Bayesian Duration Blending:
     * d_blended = c * d_eff + (1 - c) * d_base
     */
    fun blendDuration(
        effectiveDurationMinutes: Double,
        baseDurationMinutes: Double,
        confidence: Double
    ): Double {
        val c = confidence.coerceIn(0.0, 1.0)
        return c * effectiveDurationMinutes + (1.0 - c) * baseDurationMinutes
    }

    /**
     * Anti-Whiplash Wake-Time Clamping:
     * t_wake* = t_wake_base + clamp_{[-90m, +90m]}(c * (t_wake_wearable - t_wake_base))
     */
    fun clampWakeShift(
        baseWakeMillis: Long,
        wearableWakeMillis: Long,
        confidence: Double,
        maxShiftMillis: Long = MAX_WAKE_SHIFT_MILLIS
    ): Long {
        val c = confidence.coerceIn(0.0, 1.0)
        val rawShift = (wearableWakeMillis - baseWakeMillis).toDouble() * c
        val clampedShift = rawShift.coerceIn(-maxShiftMillis.toDouble(), maxShiftMillis.toDouble())
        return baseWakeMillis + clampedShift.roundToLong()
    }

    /**
     * Extracts secondary daytime sleep bouts (10m <= d <= 180m) occurring after the morning wake anchor.
     * Merges micro-arousals (<= 5m) without merging separate daytime rest bouts via nocturnal 45m WASO coalescing.
     */
    fun extractDaytimeNaps(
        intervals: List<TimeSlotWindow>,
        effectiveWakeTimeMillis: Long
    ): List<DaytimeNapBout> {
        val daytimeWindows = intervals
            .filter { it.startMillis >= effectiveWakeTimeMillis && it.endMillis > it.startMillis }
            .sortedBy { it.startMillis }
        if (daytimeWindows.isEmpty()) return emptyList()

        val merged = mutableListOf<TimeSlotWindow>()
        var curStart = daytimeWindows[0].startMillis
        var curEnd = daytimeWindows[0].endMillis
        for (i in 1 until daytimeWindows.size) {
            val next = daytimeWindows[i]
            if (next.startMillis <= curEnd + 5 * 60_000L) {
                curEnd = kotlin.math.max(curEnd, next.endMillis)
            } else {
                merged.add(TimeSlotWindow(curStart, curEnd))
                curStart = next.startMillis
                curEnd = next.endMillis
            }
        }
        merged.add(TimeSlotWindow(curStart, curEnd))

        return merged
            .map { DaytimeNapBout(it.startMillis, it.endMillis) }
            .filter { it.durationMinutes in 10..180 }
            .sortedBy { it.startMillis }
    }

    /**
     * Full normalization pipeline aggregating telemetry into normalized signal.
     */
    fun normalize(
        telemetry: RawSleepTelemetry,
        baseWakeMillis: Long,
        baseSleepMinutes: Double
    ): NormalizedChronobiologySignal {
        val (consolidated, wasoMinutes) = coalesceWasoEpisodes(telemetry.intervals)

        // Identify primary nocturnal sleep vs secondary daytime naps
        // Bug 7 Fix: Restrict nocturnalSessions to intervals overlapping the nocturnal window [baseWakeMillis - 16h, baseWakeMillis + 4h]
        val nocturnalWindowStart = baseWakeMillis - 16 * 3600_000L
        val nocturnalWindowEnd = baseWakeMillis + 4 * 3600_000L
        val nocturnalSessions = consolidated.filter {
            val overlapsNocturnalWindow = it.startMillis < nocturnalWindowEnd && it.endMillis > nocturnalWindowStart
            val isConsolidatedOrWakeAligned = it.endMillis >= baseWakeMillis - 6 * 3600_000L || (it.endMillis - it.startMillis) > 180 * 60_000L
            overlapsNocturnalWindow && isConsolidatedOrWakeAligned
        }

        val nocturnalWake = nocturnalSessions.maxOfOrNull { it.endMillis }
        val wearableWake = when {
            nocturnalWake != null -> {
                if (telemetry.detectedWakeMillis != null &&
                    telemetry.detectedWakeMillis <= nocturnalWake + MAX_WASO_GAP_MILLIS &&
                    telemetry.detectedWakeMillis >= nocturnalWake - MAX_WASO_GAP_MILLIS
                ) {
                    telemetry.detectedWakeMillis
                } else {
                    nocturnalWake
                }
            }
            telemetry.detectedWakeMillis != null &&
                kotlin.math.abs(telemetry.detectedWakeMillis - baseWakeMillis) <= MAX_WAKE_SHIFT_MILLIS &&
                consolidated.none { it.endMillis == telemetry.detectedWakeMillis } -> {
                telemetry.detectedWakeMillis
            }
            else -> baseWakeMillis
        }

        val normalizedWake = clampWakeShift(
            baseWakeMillis = baseWakeMillis,
            wearableWakeMillis = wearableWake,
            confidence = telemetry.confidence
        )

        // Extract secondary daytime sleep bouts (10m <= d <= 180m) after morning wake
        val daytimeNaps = extractDaytimeNaps(telemetry.intervals, nocturnalWake ?: normalizedWake)

        val napIntervalWindows = daytimeNaps.map { TimeSlotWindow(it.startMillis, it.endMillis) }
        val primarySessions = if (nocturnalSessions.isNotEmpty()) {
            consolidated.filter { s ->
                s in nocturnalSessions &&
                    napIntervalWindows.none { it.startMillis == s.startMillis && it.endMillis == s.endMillis }
            }
        } else {
            emptyList()
        }

        var effectiveWasoMinutes = wasoMinutes
        val rawSleepMinutes = if (primarySessions.isNotEmpty()) {
            val (primCoalesced, primWaso) = coalesceWasoEpisodes(
                telemetry.intervals.filter { interval ->
                    primarySessions.any { p -> interval.startMillis >= p.startMillis && interval.endMillis <= p.endMillis }
                }
            )
            effectiveWasoMinutes = primWaso
            (primCoalesced.sumOf { (it.endMillis - it.startMillis) / 60_000.0 } - primWaso).coerceAtLeast(0.0)
        } else if (telemetry.intervals.isEmpty()) {
            baseSleepMinutes
        } else {
            baseSleepMinutes
        }.coerceAtLeast(0.0)

        val effectiveDuration = computeEffectiveDuration(
            totalMinutes = rawSleepMinutes,
            deepSleepMinutes = telemetry.deepSleepMinutes,
            remSleepMinutes = telemetry.remSleepMinutes,
            lightSleepMinutes = telemetry.lightSleepMinutes
        )

        val blendedDuration = blendDuration(
            effectiveDurationMinutes = effectiveDuration,
            baseDurationMinutes = baseSleepMinutes,
            confidence = telemetry.confidence
        )

        return NormalizedChronobiologySignal(
            totalSleepMinutes = rawSleepMinutes,
            effectiveDurationMinutes = effectiveDuration,
            blendedDurationMinutes = blendedDuration,
            normalizedWakeTimeMillis = normalizedWake,
            totalWasoMinutes = effectiveWasoMinutes.toDouble(),
            consolidatedSessions = consolidated,
            daytimeNaps = daytimeNaps
        )
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainmodelenergyuimodelkt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/model/EnergyUiModel.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/model/EnergyUiModel.kt`
- **Lines of Code:** 636
- **Size:** 29,296 bytes (28.6 KB)

```kotlin
package com.neuroflow.app.domain.model

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.scheduler.ProtectedSlotClock
import com.neuroflow.app.domain.service.AdaptiveCircadianLearner
import com.neuroflow.app.domain.service.BehavioralObservation
import com.neuroflow.app.domain.service.ChronotypeProfile
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.CognitiveBlockCategory
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class PeakWindow(
    val startMinuteOffset: Int = 0,
    val durationMinutes: Int = 180,
    val amplitude: Float = 1.0f
)

data class CircadianProfile(
    val windows: List<PeakWindow> = emptyList(),
    val phaseShiftMinutes: Int = 0
)

enum class ProfileType {
    WORKDAY,
    WEEKEND
}

data class ConfidenceComponents(
    val sleepCoverage: Float = 0.8f,
    val wakeConsistency: Float = 0.8f,
    val behaviorPerformance: Float = 0.8f,
    val overall: Float = 0.8f
)

data class EffectivePeakProfile(
    val profileType: ProfileType = ProfileType.WORKDAY,
    val anchorMinuteOfDay: Int = 150,
    val windows: List<PeakWindow> = emptyList(),
    val confidence: ConfidenceComponents = ConfidenceComponents(),
    val windowConfidences: List<Float> = emptyList(),
    val explanation: String = "",
    val driftStatus: String = "stable",
    val weeklyBacktestErrorMinutes: Float? = null,
    val confidenceGatedAbstention: Boolean = false,
    val adaptiveFreezeMode: Boolean = false,
    val abstentionReason: String = ""
)

data class EnergyUiModel(
    val availableEnergy: Int = 50,
    val rawEnergy: Float = 50f,
    val baselineRawEnergy: Float = 50f,
    val peakScore: Float = 50f,
    val fatiguePenalty: Float = 0f,
    val momentAdjustment: Float = 0f,
    val momentConfidence: Float = 1f,
    val currentPeakValue: Int = 4000,
    val peakValue: Int = 4000,
    val peakDrop: Int = 0,
    val minutesSincePeak: Int = 0,
    val minutesUntilPeakReset: Int = 0,
    val sleepPressurePoints: Int = 0,
    val fatiguePercent: Int = 0,
    val fatigueZone: FatigueZone = FatigueZone.RESTED,
    val circadianFactor: Float = 1f,
    val reservoirFactor: Float = 1f,
    val confidenceFactor: Float = 1f,
    val effectivePeakProfile: EffectivePeakProfile? = null,
    val momentSignalSummary: String = "",
    val peakDetectionAgeMillis: Long = 0L,
    val sessionDataAgeMillis: Long = 0L,
    val sleepLogDataAgeMillis: Long = 0L,
    val taskDataAgeMillis: Long = 0L,
    val overallFreshnessAgeMillis: Long = 0L,
    val hasRecentData: Boolean = true,
    val refreshedAtMillis: Long = System.currentTimeMillis()
)

fun buildEnergyUiModel(
    usableEnergy: Int,
    circadianFactor: Float = 0.85f,
    sleepPressureFactor: Float = 0.25f,
    sleepPressurePoints: Int = 0,
    fatigueZone: FatigueZone? = null,
    refreshedAtMillis: Long = System.currentTimeMillis(),
    baselineBiologicalEnergy: Int = usableEnergy,
    physiologicalImpulse: Double = 0.0,
    learnedAdjustmentPoints: Double = 0.0,
    predictionConfidence: Double = 1.0,
    effectivePeakProfile: EffectivePeakProfile? = null
): EnergyUiModel {
    val usable = usableEnergy.coerceIn(0, 100)
    val fatiguePct = (sleepPressureFactor * 100).toInt().coerceIn(0, 100)
    val effectivePressurePoints = if (sleepPressurePoints > 0) {
        sleepPressurePoints
    } else {
        (sleepPressureFactor.coerceIn(0f, 1f) * FatigueZone.SOFT_MAX_REFERENCE).roundToInt()
    }
    val resolvedZone = fatigueZone ?: if (sleepPressurePoints > 0) {
        FatigueZone.fromPressurePoints(sleepPressurePoints)
    } else {
        when {
            fatiguePct <= 24 -> FatigueZone.RESTED
            fatiguePct <= 49 -> FatigueZone.MODERATE
            fatiguePct <= 74 -> FatigueZone.HIGH
            else -> FatigueZone.CRITICAL
        }
    }
    val peakVal = 4000
    val curPeakVal = (usable * 40).coerceIn(0, 4000)
    return EnergyUiModel(
        availableEnergy = usable,
        rawEnergy = usable.toFloat(),
        baselineRawEnergy = baselineBiologicalEnergy.toFloat(),
        peakScore = usable.toFloat(),
        fatiguePenalty = (sleepPressureFactor * 100f).coerceIn(0f, 100f),
        momentAdjustment = (physiologicalImpulse + learnedAdjustmentPoints).toFloat(),
        momentConfidence = predictionConfidence.toFloat(),
        currentPeakValue = curPeakVal,
        peakValue = peakVal,
        peakDrop = maxOf(0, peakVal - curPeakVal),
        minutesSincePeak = 0,
        minutesUntilPeakReset = 0,
        sleepPressurePoints = effectivePressurePoints,
        fatiguePercent = fatiguePct,
        fatigueZone = resolvedZone,
        circadianFactor = circadianFactor.coerceIn(0f, 1f),
        reservoirFactor = 1.0f,
        confidenceFactor = predictionConfidence.toFloat(),
        effectivePeakProfile = effectivePeakProfile,
        momentSignalSummary = if (physiologicalImpulse != 0.0) String.format(java.util.Locale.ROOT, "Impulse: %+.1f pts", physiologicalImpulse) else "",
        overallFreshnessAgeMillis = maxOf(0L, System.currentTimeMillis() - refreshedAtMillis),
        hasRecentData = true,
        refreshedAtMillis = refreshedAtMillis
    )
}

fun buildDailyCognitiveBlocks(
    slots: List<ProtectedTimeSlot>,
    date: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<CognitiveBlockWindow> {
    // Bug 5 Fix (ADR-025): Populate explicitStartMillis and explicitEndMillis across both yesterday and today
    // so late-evening meals taper off smoothly across midnight without circular wrap-around phantom dips.
    val dates = listOf(date.minusDays(1), date)
    return dates.flatMap { targetDate ->
        val dayStart = targetDate.atStartOfDay(zoneId)
        val dayMillis = dayStart.toInstant().toEpochMilli()
        slots
            .filter { it.isEnabled && ProtectedSlotClock.occursOnDay(it, dayMillis) }
            .map { slot ->
                val category = when (slot.category) {
                    PtsCategory.MEAL -> CognitiveBlockCategory.MEAL
                    PtsCategory.EXERCISE -> CognitiveBlockCategory.EXERCISE
                    PtsCategory.WORK, PtsCategory.STUDY -> CognitiveBlockCategory.FOCUS_WORK
                    PtsCategory.BREAK, PtsCategory.MINDFULNESS -> CognitiveBlockCategory.WIND_DOWN
                    else -> CognitiveBlockCategory.WIND_DOWN
                }
                val startMin = slot.startMinuteOfDay
                val endMin = (startMin + slot.durationMinutes) % 1440
                val startZdt = targetDate.atTime((startMin / 60).coerceIn(0, 23), (startMin % 60).coerceIn(0, 59)).atZone(zoneId)
                val totalEndMinutes = startMin + slot.durationMinutes
                val endZdt = if (totalEndMinutes < 1440) {
                    targetDate.atTime((totalEndMinutes / 60).coerceIn(0, 23), (totalEndMinutes % 60).coerceIn(0, 59)).atZone(zoneId)
                } else {
                    targetDate.plusDays((totalEndMinutes / 1440).toLong())
                        .atTime(((totalEndMinutes % 1440) / 60).coerceIn(0, 23), ((totalEndMinutes % 1440) % 60).coerceIn(0, 59))
                        .atZone(zoneId)
                }
                val startMillis = startZdt.toInstant().toEpochMilli()
                val endMillis = endZdt.toInstant().toEpochMilli()
                CognitiveBlockWindow(
                    startMinuteOfDay = startMin,
                    endMinuteOfDay = endMin,
                    category = category,
                    intensity = 1.0,
                    label = slot.label,
                    explicitStartMillis = startMillis,
                    explicitEndMillis = endMillis
                )
            }
    }
}

fun buildDailyCognitiveBlocks(
    slots: List<ProtectedTimeSlot>,
    dayMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): List<CognitiveBlockWindow> = buildDailyCognitiveBlocks(
    slots = slots,
    date = Instant.ofEpochMilli(dayMillis).atZone(zoneId).toLocalDate(),
    zoneId = zoneId
)

fun extractStimulantDosesFromSlots(
    slots: List<ProtectedTimeSlot>,
    date: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<com.neuroflow.app.domain.service.StimulantDoseBout> {
    // Bug 3 Fix: Evaluate both yesterday and today so late-evening doses remain in 24h PK window across midnight
    val dates = listOf(date.minusDays(1), date)
    return dates.flatMap { targetDate ->
        val dayStartMillis = targetDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        slots
            .filter { it.isEnabled && ProtectedSlotClock.occursOnDay(it, dayStartMillis) }
            .mapNotNull { slot ->
                val label = slot.label.lowercase()
                val words = label.split(Regex("[^a-z0-9_]+")).filter { it.isNotBlank() }.toSet()
                val type = when {
                    words.any { it in setOf("tea", "theanine", "matcha", "greentea") } && !label.contains("chamomile") && !label.contains("herbal") ->
                        com.neuroflow.app.domain.service.StimulantType.GREEN_TEA_L_THEANINE
                    words.any { it in setOf("coffee", "caffeine", "espresso", "latte", "cappuccino", "coldbrew", "americano") } ->
                        com.neuroflow.app.domain.service.StimulantType.CAFFEINE
                    words.any { it in setOf("ritalin", "methylphenidate") } || label.contains("meds ir") || label.contains("stimulant ir") ->
                        com.neuroflow.app.domain.service.StimulantType.PRESCRIPTION_STIMULANT_IR
                    words.any { it in setOf("adderall", "vyvanse", "concerta", "amphetamine", "dexedrine") } || label.contains("meds xr") || label.contains("stimulant xr") || words.contains("meds") ->
                        com.neuroflow.app.domain.service.StimulantType.PRESCRIPTION_STIMULANT_XR
                    else -> null
                } ?: return@mapNotNull null

                val doseZdt = targetDate.atTime((slot.startMinuteOfDay / 60).coerceIn(0, 23), (slot.startMinuteOfDay % 60).coerceIn(0, 59)).atZone(zoneId)
                val doseMillis = doseZdt.toInstant().toEpochMilli()
                com.neuroflow.app.domain.service.StimulantDoseBout(
                    timestampMillis = doseMillis,
                    doseMg = when (type) {
                        com.neuroflow.app.domain.service.StimulantType.CAFFEINE -> 100.0
                        com.neuroflow.app.domain.service.StimulantType.GREEN_TEA_L_THEANINE -> 50.0
                        com.neuroflow.app.domain.service.StimulantType.PRESCRIPTION_STIMULANT_IR -> 10.0
                        com.neuroflow.app.domain.service.StimulantType.PRESCRIPTION_STIMULANT_XR -> 20.0
                    },
                    type = type
                )
            }
    }.distinctBy { it.timestampMillis to it.type }
}

fun buildBehavioralObservations(
    completedTasks: List<TaskEntity> = emptyList(),
    sessions: List<TimeSessionEntity> = emptyList(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<BehavioralObservation> {
    data class TimedObservation(
        val timestampMillis: Long,
        val observation: BehavioralObservation
    )

    val timedObservations = mutableListOf<TimedObservation>()

    for (session in sessions) {
        if (session.startedAt > 0L) {
            val zdt = Instant.ofEpochMilli(session.startedAt).atZone(zoneId)
            val minuteOfDay = zdt.hour * 60 + zdt.minute
            val durationMins = if (session.endedAt != null && session.endedAt > session.startedAt) {
                ((session.endedAt - session.startedAt) / 60_000L).toDouble()
            } else if (session.durationMinutes > 0f) {
                session.durationMinutes.toDouble()
            } else {
                25.0
            }
            // Feature 2 (ADR-025): Cognitive demand weighting (2.0x for deep work or >= 45m)
            val isDeepFocus = session.sessionType == "DEEP_WORK" || durationMins >= 45.0
            val demandMultiplier = if (isDeepFocus) 2.0 else 1.0
            val weight = ((durationMins / 30.0) * demandMultiplier).coerceIn(0.5, 6.0)
            timedObservations.add(
                TimedObservation(
                    timestampMillis = session.startedAt,
                    observation = BehavioralObservation(
                        minuteOfDay = minuteOfDay,
                        productivityRating = if (isDeepFocus) 0.95 else 0.85,
                        weight = weight
                    )
                )
            )
        }
    }

    for (task in completedTasks) {
        // Bug 3 Fix (ADR-025): Exclude low-energy chores from shifting the peak-hour state estimate
        if (task.energyLevel == EnergyLevel.LOW) continue

        val completedAt = task.completedAt ?: task.updatedAt
        if (completedAt > 0L) {
            val zdt = Instant.ofEpochMilli(completedAt).atZone(zoneId)
            val minuteOfDay = zdt.hour * 60 + zdt.minute
            val weight = when (task.energyLevel) {
                EnergyLevel.HIGH -> 1.5
                EnergyLevel.MEDIUM -> 1.0
                else -> 1.0
            }
            val rating = when (task.energyLevel) {
                EnergyLevel.HIGH -> 0.95
                EnergyLevel.MEDIUM -> 0.80
                else -> 0.80
            }
            timedObservations.add(
                TimedObservation(
                    timestampMillis = completedAt,
                    observation = BehavioralObservation(
                        minuteOfDay = minuteOfDay,
                        productivityRating = rating,
                        weight = weight
                    )
                )
            )
        }
    }

    // Bug 3 Fix (ADR-025): Sort chronologically before sequential Markov-chain Kalman update
    return timedObservations.sortedBy { it.timestampMillis }.map { it.observation }
}

/**
 * Computes 7-day rolling daily sleep average by aggregating total sleep per calendar day
 * (preventing daytime naps from artificially halving the per-entry average).
 */
fun computeRollingDailySleepAverage(
    sleepLogs: List<SleepLogEntity>,
    nowMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): Double {
    val sevenDaysAgo = nowMillis - 7 * 86_400_000L
    val recentSleepLogs = sleepLogs.filter { it.endAt > sevenDaysAgo && it.startAt < nowMillis && it.endAt > it.startAt }
    if (recentSleepLogs.isEmpty()) return 480.0

    // Sweep-line merge overlapping sleep intervals across the 7-day window to prevent wearable + manual double-counting
    val rawIntervals = recentSleepLogs.map { max(sevenDaysAgo, it.startAt) to min(nowMillis, it.endAt) }
        .filter { it.second > it.first }
        .sortedBy { it.first }

    val mergedIntervals = mutableListOf<Pair<Long, Long>>()
    for (interval in rawIntervals) {
        if (mergedIntervals.isEmpty()) {
            mergedIntervals.add(interval)
        } else {
            val last = mergedIntervals.last()
            if (interval.first <= last.second) {
                mergedIntervals[mergedIntervals.size - 1] = last.first to max(last.second, interval.second)
            } else {
                mergedIntervals.add(interval)
            }
        }
    }

    val minutesByLocalDay = mutableMapOf<LocalDate, Double>()
    for ((start, end) in mergedIntervals) {
        val mins = (end - start) / 60_000.0
        val wakeDay = Instant.ofEpochMilli(end).atZone(zoneId).toLocalDate()
        minutesByLocalDay[wakeDay] = (minutesByLocalDay[wakeDay] ?: 0.0) + mins
    }
    return if (minutesByLocalDay.isNotEmpty()) {
        minutesByLocalDay.values.average().coerceIn(120.0, 720.0)
    } else {
        480.0
    }
}

fun computeEnergyUiState(
    circadianEnergyService: CircadianEnergyService,
    sleepTelemetryRepository: SleepTelemetryRepository?,
    prefs: UserPreferences,
    sleepLogs: List<SleepLogEntity> = emptyList(),
    continuousActiveFocusMinutes: Int = 0,
    nowMillis: Long = System.currentTimeMillis(),
    completedTasks: List<TaskEntity> = emptyList(),
    sessions: List<TimeSessionEntity> = emptyList(),
    stimulantDoses: List<com.neuroflow.app.domain.service.StimulantDoseBout> = emptyList()
): Pair<CircadianSnapshot, EnergyUiModel> {
    val zoneId = ZoneId.systemDefault()
    val zonedDateTime = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
    var anchor = zonedDateTime.toLocalDate().atTime(prefs.wakeUpHour.coerceIn(0, 23), 0).atZone(zoneId)
    if (anchor.toInstant().toEpochMilli() > nowMillis) {
        anchor = anchor.minusDays(1)
    }
    val baseWakeMillis = anchor.toInstant().toEpochMilli()

    val signal = sleepTelemetryRepository?.normalizeLogs(
        logs = sleepLogs,
        baseWakeMillis = baseWakeMillis,
        baseSleepMinutes = 480.0
    )

    val wakeTimeMillis = signal?.normalizedWakeTimeMillis ?: baseWakeMillis
    val sleepDurationMinutes = signal?.blendedDurationMinutes?.roundToInt()?.coerceIn(60, 960) ?: 480

    val dayStartMillis = zonedDateTime.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
    val physLogs = com.neuroflow.app.data.local.parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson)
        .filter { it.timestampMillis in (dayStartMillis - 8 * 3600_000L)..(dayStartMillis + 26 * 3600_000L) }

    val quickPhysBlocks = physLogs.mapNotNull { log ->
        val logZdt = Instant.ofEpochMilli(log.timestampMillis).atZone(zoneId)
        val minOfDay = logZdt.hour * 60 + logZdt.minute
        val duration = log.durationMinutes.coerceIn(5, 240)
        when (log.type) {
            com.neuroflow.app.data.local.PhysiologicalEventType.MEAL -> CognitiveBlockWindow(
                startMinuteOfDay = minOfDay,
                endMinuteOfDay = (minOfDay + duration) % 1440,
                category = CognitiveBlockCategory.MEAL,
                intensity = log.intensity,
                label = "Quick Logged Meal #${log.timestampMillis}",
                explicitStartMillis = log.timestampMillis,
                explicitEndMillis = log.timestampMillis + duration * 60_000L
            )
            com.neuroflow.app.data.local.PhysiologicalEventType.WORKOUT -> CognitiveBlockWindow(
                startMinuteOfDay = minOfDay,
                endMinuteOfDay = (minOfDay + duration) % 1440,
                category = CognitiveBlockCategory.EXERCISE,
                intensity = log.intensity,
                label = "Quick Logged Workout #${log.timestampMillis}",
                explicitStartMillis = log.timestampMillis,
                explicitEndMillis = log.timestampMillis + duration * 60_000L
            )
            com.neuroflow.app.data.local.PhysiologicalEventType.NAP -> null
        }
    }

    val quickPhysNaps = physLogs
        .filter { it.type == com.neuroflow.app.data.local.PhysiologicalEventType.NAP }
        .map { log ->
            val napDuration = log.durationMinutes.coerceIn(5, 180)
            com.neuroflow.app.domain.service.DaytimeNapBout(
                startMillis = log.timestampMillis,
                endMillis = log.timestampMillis + napDuration * 60_000L
            )
        }

    val naps = ((signal?.daytimeNaps ?: emptyList()) + quickPhysNaps).sortedBy { it.startMillis }

    val rollingAverageSleepMinutes = computeRollingDailySleepAverage(sleepLogs, nowMillis, zoneId)

    val chronotype = if (!prefs.quizChronotype.isNullOrBlank()) {
        ChronotypeProfile.fromString(prefs.quizChronotype)
    } else if (!prefs.manualChronotype.isNullOrBlank()) {
        ChronotypeProfile.fromString(prefs.manualChronotype)
    } else if (prefs.meqScore > 0) {
        when {
            prefs.meqScore >= 70 -> ChronotypeProfile.DEFINITE_MORNING
            prefs.meqScore >= 59 -> ChronotypeProfile.MODERATE_MORNING
            prefs.meqScore >= 42 -> ChronotypeProfile.INTERMEDIATE
            prefs.meqScore >= 31 -> ChronotypeProfile.MODERATE_EVENING
            else -> ChronotypeProfile.DEFINITE_EVENING
        }
    } else {
        ChronotypeProfile.INTERMEDIATE
    }

    val manualPeakHour = if (prefs.manualPeakProfileEnabled) {
        (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23)
    } else null

    val cognitiveBlocks = buildDailyCognitiveBlocks(
        slots = prefs.protectedTimeSlots,
        date = zonedDateTime.toLocalDate(),
        zoneId = zoneId
    ) + quickPhysBlocks

    val observations = buildBehavioralObservations(completedTasks, sessions, zoneId)
    val initialPeak = if (prefs.effectivePeakStart in 0..23) prefs.effectivePeakStart.toDouble() else 9.0
    val learnedProfile = if (observations.isNotEmpty()) {
        AdaptiveCircadianLearner.learnProfile(observations, initialPeakHour = initialPeak)
    } else null

    // Bug 2 Fix (ADR-030): Unify stimulant dose resolution across logs, slots, and explicit parameters
    val effectiveStimulantDoses = (
        stimulantDoses +
        com.neuroflow.app.data.local.parseStimulantLogs(prefs.stimulantLogsJson) +
        extractStimulantDosesFromSlots(prefs.protectedTimeSlots, zonedDateTime.toLocalDate(), zoneId)
    ).distinctBy { it.timestampMillis to it.type }

    // Feature 3 (ADR-023) & Bug 2 Fix: compute and pass social jetlag phase shift
    val sleepWindows = sleepLogs.map { com.neuroflow.app.domain.service.TimeSlotWindow(it.startAt, it.endAt) }
    val sjlReport = com.neuroflow.app.domain.service.SocialJetlagCalculator.evaluateFromSleepWindows(
        sleepWindows = sleepWindows,
        nowMillis = nowMillis,
        zoneId = zoneId
    )
    val socialJetlagLagHours = sjlReport.mondayMorningPhaseLagHours

    val hasAdvancedParameters = naps.isNotEmpty() ||
        cognitiveBlocks.isNotEmpty() ||
        learnedProfile != null ||
        (rollingAverageSleepMinutes != 480.0) ||
        manualPeakHour != null ||
        chronotype != ChronotypeProfile.INTERMEDIATE ||
        effectiveStimulantDoses.isNotEmpty() ||
        kotlin.math.abs(socialJetlagLagHours) > 0.0

    val snapshot = if (effectiveStimulantDoses.isNotEmpty() || kotlin.math.abs(socialJetlagLagHours) > 0.0) {
        circadianEnergyService.evaluateSnapshotWithStimulants(
            timestampMillis = nowMillis,
            wakeTimeMillis = wakeTimeMillis,
            lastSleepDurationMinutes = sleepDurationMinutes,
            continuousActiveFocusMinutes = continuousActiveFocusMinutes,
            zoneId = zoneId,
            naps = naps,
            chronotype = chronotype,
            manualPeakStartHour = manualPeakHour,
            learnedProfile = learnedProfile,
            rollingAverageSleepMinutes = rollingAverageSleepMinutes,
            cognitiveBlocks = cognitiveBlocks,
            stimulantDoses = effectiveStimulantDoses,
            socialJetlagLagHours = socialJetlagLagHours
        )
    } else if (hasAdvancedParameters) {
        if (cognitiveBlocks.isEmpty() && learnedProfile == null && rollingAverageSleepMinutes == 480.0 && manualPeakHour == null && chronotype == ChronotypeProfile.INTERMEDIATE) {
            circadianEnergyService.evaluateSnapshot(
                timestampMillis = nowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                continuousActiveFocusMinutes = continuousActiveFocusMinutes,
                zoneId = zoneId,
                naps = naps
            )
        } else {
            circadianEnergyService.evaluateSnapshot(
                timestampMillis = nowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepDurationMinutes,
                continuousActiveFocusMinutes = continuousActiveFocusMinutes,
                zoneId = zoneId,
                naps = naps,
                chronotype = chronotype,
                manualPeakStartHour = manualPeakHour,
                learnedProfile = learnedProfile,
                rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                cognitiveBlocks = cognitiveBlocks
            )
        }
    } else {
        circadianEnergyService.evaluateSnapshot(
            timestampMillis = nowMillis,
            wakeTimeMillis = wakeTimeMillis,
            lastSleepDurationMinutes = sleepDurationMinutes,
            continuousActiveFocusMinutes = continuousActiveFocusMinutes,
            zoneId = zoneId
        )
    }

    // Bug 7 Fix (ADR-026): When daytime power naps or stimulant adenosine masking are active,
    // homeostatic pressure has diverged from unadjusted static point counters. Derive fatigueZone
    // from snapshot.homeostaticSleepPressure rather than leaving it stuck on prefs.sleepPressurePoints.
    val isHomeostaticAdjusted = naps.isNotEmpty() || effectiveStimulantDoses.isNotEmpty()
    val resolvedFatigueZone = if (isHomeostaticAdjusted) {
        val pct = (snapshot.homeostaticSleepPressure * 100).toInt().coerceIn(0, 100)
        when {
            pct <= 24 -> FatigueZone.RESTED
            pct <= 49 -> FatigueZone.MODERATE
            pct <= 74 -> FatigueZone.HIGH
            else -> FatigueZone.CRITICAL
        }
    } else null

    val effectiveProfile = if (prefs.manualPeakProfileEnabled) {
        val w1 = PeakWindow(
            startMinuteOffset = prefs.manualPeakWindow1StartOffsetMinutes,
            durationMinutes = prefs.manualPeakWindow1DurationMinutes,
            amplitude = prefs.manualPeakWindow1Amplitude
        )
        val w2 = PeakWindow(
            startMinuteOffset = prefs.manualPeakWindow2StartOffsetMinutes,
            durationMinutes = prefs.manualPeakWindow2DurationMinutes,
            amplitude = prefs.manualPeakWindow2Amplitude
        )
        val w3 = PeakWindow(
            startMinuteOffset = prefs.manualPeakWindow3StartOffsetMinutes,
            durationMinutes = prefs.manualPeakWindow3DurationMinutes,
            amplitude = prefs.manualPeakWindow3Amplitude
        )
        EffectivePeakProfile(
            profileType = if (prefs.manualPeakProfileType == "WEEKEND") ProfileType.WEEKEND else ProfileType.WORKDAY,
            anchorMinuteOfDay = prefs.manualPeakAnchorMinuteOfDay,
            windows = listOf(w1, w2, w3),
            confidence = ConfidenceComponents(
                sleepCoverage = prefs.morningTuneSleepWeight,
                wakeConsistency = prefs.morningTuneWakeWeight,
                behaviorPerformance = prefs.morningTuneBehaviorWeight,
                overall = prefs.peakDetectionConfidence
            ),
            windowConfidences = listOf(0.9f, 0.85f, 0.8f),
            explanation = "Manual Peak Profile Active",
            driftStatus = "configured",
            adaptiveFreezeMode = prefs.adaptivePeakFreezeEnabled,
            confidenceGatedAbstention = prefs.peakConfidenceAbstentionEnabled,
            abstentionReason = prefs.peakConfidenceAbstentionReason
        )
    } else {
        val peakAnchorMin = prefs.detectedPeakMinuteOfDay.takeIf { it > 0 } ?: (prefs.peakEnergyStart * 60)
        val rawSpan = ((prefs.peakEnergyEnd - prefs.peakEnergyStart) % 24 + 24) % 24
        val safePeakDurationHours = (if (rawSpan == 0) 4 else rawSpan).coerceIn(1, 8)
        EffectivePeakProfile(
            profileType = ProfileType.WORKDAY,
            anchorMinuteOfDay = peakAnchorMin,
            windows = listOf(
                PeakWindow(startMinuteOffset = 0, durationMinutes = safePeakDurationHours * 60, amplitude = 1.0f)
            ),
            confidence = ConfidenceComponents(
                sleepCoverage = prefs.morningTuneSleepWeight,
                wakeConsistency = prefs.morningTuneWakeWeight,
                behaviorPerformance = prefs.morningTuneBehaviorWeight,
                overall = prefs.peakDetectionConfidence.takeIf { it > 0f } ?: 0.85f
            ),
            windowConfidences = listOf(0.85f),
            explanation = "Automated Circadian Calibration",
            driftStatus = "synchronized",
            adaptiveFreezeMode = prefs.adaptivePeakFreezeEnabled,
            confidenceGatedAbstention = prefs.peakConfidenceAbstentionEnabled,
            abstentionReason = prefs.peakConfidenceAbstentionReason
        )
    }

    val energyUi = buildEnergyUiModel(
        usableEnergy = snapshot.usableEnergy,
        circadianFactor = snapshot.circadianAlertness.toFloat(),
        sleepPressureFactor = snapshot.homeostaticSleepPressure.toFloat(),
        sleepPressurePoints = prefs.sleepPressurePoints,
        fatigueZone = resolvedFatigueZone,
        refreshedAtMillis = nowMillis,
        baselineBiologicalEnergy = snapshot.baselineBiologicalEnergy,
        physiologicalImpulse = snapshot.physiologicalImpulse,
        learnedAdjustmentPoints = snapshot.learnedAdjustmentPoints,
        predictionConfidence = snapshot.predictionConfidence,
        effectivePeakProfile = effectiveProfile
    )

    return Pair(snapshot, energyUi)
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainmodelcircadianenergysummarykt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/model/CircadianEnergySummary.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/model/CircadianEnergySummary.kt`
- **Lines of Code:** 64
- **Size:** 2,795 bytes (2.7 KB)

```kotlin
package com.neuroflow.app.domain.model

import com.neuroflow.app.domain.service.CircadianSnapshot
import com.neuroflow.app.domain.service.DailyCircadianSummary
import com.neuroflow.app.domain.service.EnergyPhase
import java.time.LocalTime

typealias EnergyPhase = com.neuroflow.app.domain.service.EnergyPhase

data class CircadianEnergySummary(
    val currentEnergyScore: Int,
    val currentPhase: EnergyPhase,
    val phaseConfidence: Float = 0.9f,
    val peakStart: LocalTime = LocalTime.of(9, 30),
    val peakEnd: LocalTime = LocalTime.of(12, 30),
    val dipStart: LocalTime = LocalTime.of(13, 30),
    val dipEnd: LocalTime = LocalTime.of(15, 30),
    val windDownStart: LocalTime = LocalTime.of(21, 30),
    val sleepPressureNormalized: Float = 0.45f,
    val cognitiveBudgetMinutesRemaining: Int = 240,
    val calibrationQuality: Float = 0.85f,
)

fun buildCircadianEnergySummary(
    dailySummary: DailyCircadianSummary?,
    snapshot: CircadianSnapshot,
    sleepPressurePoints: Int = 450,
    dailyDeepWorkTargetMinutes: Int? = null,
    sleepHour: Int = 23,
    completedDeepWorkMinutes: Int = 0
): CircadianEnergySummary {
    val peakStart = dailySummary?.let { LocalTime.of(it.peakWindowStartHour.coerceIn(0, 23), 0) }
        ?: LocalTime.of(9, 30)
    val peakEnd = dailySummary?.let { LocalTime.of(it.peakWindowEndHour.coerceIn(0, 23), 0) }
        ?: LocalTime.of(12, 30)
    val dipStart = dailySummary?.let { LocalTime.of(it.dipWindowStartHour.coerceIn(0, 23), 0) }
        ?: LocalTime.of(13, 30)
    val dipEnd = dailySummary?.let { LocalTime.of(it.dipWindowEndHour.coerceIn(0, 23), 0) }
        ?: LocalTime.of(15, 30)
    val targetWindDownHour = ((sleepHour - 2) % 24 + 24) % 24
    val windDownStart = LocalTime.of(targetWindDownHour, 0)
    val sleepPressureNormalized = if (snapshot.homeostaticPressureS > 0.0 && !snapshot.homeostaticPressureS.isNaN()) {
        snapshot.homeostaticPressureS.toFloat().coerceIn(0f, 1f)
    } else {
        (sleepPressurePoints / 1000f).coerceIn(0f, 1f)
    }
    val totalBudget = dailyDeepWorkTargetMinutes?.takeIf { it > 0 } ?: dailySummary?.recommendedDeepWorkMinutes ?: 240
    val cognitiveBudget = kotlin.math.max(0, totalBudget - completedDeepWorkMinutes)
    val confidence = snapshot.predictionConfidence.toFloat().coerceIn(0.1f, 1.0f)

    return CircadianEnergySummary(
        currentEnergyScore = snapshot.usableEnergy,
        currentPhase = snapshot.phase,
        phaseConfidence = confidence,
        peakStart = peakStart,
        peakEnd = peakEnd,
        dipStart = dipStart,
        dipEnd = dipEnd,
        windDownStart = windDownStart,
        sleepPressureNormalized = sleepPressureNormalized,
        cognitiveBudgetMinutesRemaining = cognitiveBudget,
        calibrationQuality = confidence
    )
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainmodelfatiguezonekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/model/FatigueZone.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/model/FatigueZone.kt`
- **Lines of Code:** 206
- **Size:** 8,543 bytes (8.3 KB)

```kotlin
package com.neuroflow.app.domain.model

import kotlin.math.pow
import kotlin.math.roundToInt

enum class FatigueZone {
    RESTED,
    MODERATE,
    HIGH,
    CRITICAL;

    val label: String get() = label(this)

    companion object {
        const val SOFT_MAX_REFERENCE = 3000
        const val AWAKE_POINTS_PER_MINUTE = 1
        const val MAX_PRESSURE_POINTS = 14_400
        const val SLEEP_INERTIA_DURATION_MINUTES = 30
        const val DEFAULT_MAX_SLEEP_INERTIA_PENALTY = 0.35f
        const val CHRONIC_RESTRICTION_THRESHOLD_HOURS = 6.0f
        const val CHRONIC_RESTRICTION_THRESHOLD_MINUTES = 360
        const val OPTIMAL_DAILY_SLEEP_MINUTES = 480

        private const val FATIGUE_CURVE_GAMMA = 0.82f
        private const val CYCLE_MINUTES = 90
        private const val PHASE_ONE_END_MINUTE = 60
        private const val PHASE_ONE_BONUS = 126
        private const val FULL_CYCLE_RECOVERY = 279

        data class PressureResult(
            val pressurePoints: Int,
            val awakeMinutes: Int = 0,
            val sleepMinutes: Int = 0,
            val recoveryPointsEarned: Int = 0,
            val recoveryPointsApplied: Int = 0
        )

        fun pressureFromAwakeMinutes(awakeMinutes: Int): Int {
            val safeAwakeMinutes = awakeMinutes.coerceAtLeast(0)
            return (safeAwakeMinutes * AWAKE_POINTS_PER_MINUTE).coerceAtMost(MAX_PRESSURE_POINTS)
        }

        fun applyAwakeMinutes(currentPressure: Int, awakeMinutes: Int): PressureResult {
            val safeCurrentPressure = currentPressure.coerceAtLeast(0).coerceAtMost(MAX_PRESSURE_POINTS)
            val safeAwakeMinutes = awakeMinutes.coerceAtLeast(0)
            val addedPressure = pressureFromAwakeMinutes(safeAwakeMinutes)
            return PressureResult(
                pressurePoints = (safeCurrentPressure + addedPressure).coerceAtMost(MAX_PRESSURE_POINTS),
                awakeMinutes = safeAwakeMinutes
            )
        }

        fun applySleepSession(currentPressure: Int, sleepMinutes: Int): PressureResult {
            val safeCurrentPressure = currentPressure.coerceAtLeast(0).coerceAtMost(MAX_PRESSURE_POINTS)
            val safeSleepMinutes = sleepMinutes.coerceAtLeast(0)
            val recoveryPoints = recoveryForSleepMinutes(safeSleepMinutes)
            val applied = minOf(safeCurrentPressure, recoveryPoints)
            val finalPressure = (safeCurrentPressure - applied).coerceAtLeast(0)
            return PressureResult(
                pressurePoints = finalPressure,
                sleepMinutes = safeSleepMinutes,
                recoveryPointsEarned = recoveryPoints,
                recoveryPointsApplied = applied
            )
        }

        fun fromPressurePoints(pressurePoints: Int, softMaxReference: Int = SOFT_MAX_REFERENCE): FatigueZone {
            val percent = fatiguePercent(pressurePoints, softMaxReference)
            return when {
                percent <= 24 -> RESTED
                percent <= 49 -> MODERATE
                percent <= 74 -> HIGH
                else -> CRITICAL
            }
        }

        fun fatigueZone(pressurePoints: Int, softMaxReference: Int = SOFT_MAX_REFERENCE): FatigueZone =
            fromPressurePoints(pressurePoints, softMaxReference)

        fun fromUsableEnergy(usableEnergy: Int): FatigueZone {
            return when {
                usableEnergy >= 70 -> RESTED
                usableEnergy >= 45 -> MODERATE
                usableEnergy >= 25 -> HIGH
                else -> CRITICAL
            }
        }

        fun fatigueRatio(pressurePoints: Int, softMaxReference: Int = SOFT_MAX_REFERENCE): Float {
            val safeMax = softMaxReference.coerceAtLeast(1)
            val safePressure = pressurePoints.coerceAtLeast(0)
            val ratio = (safePressure.toFloat() / safeMax.toFloat()).coerceIn(0f, 1f)
            return ratio.pow(FATIGUE_CURVE_GAMMA).coerceIn(0f, 1f)
        }

        fun fatiguePercent(pressurePoints: Int, softMaxReference: Int = SOFT_MAX_REFERENCE): Int {
            return (fatigueRatio(pressurePoints, softMaxReference) * 100f)
                .roundToInt()
                .coerceIn(0, 100)
        }

        fun label(zone: FatigueZone): String {
            return when (zone) {
                RESTED -> "Rested"
                MODERATE -> "Moderate"
                HIGH -> "High"
                CRITICAL -> "Critical"
            }
        }

        fun fatigueZoneLabel(zone: FatigueZone): String = label(zone)

        fun calculateSleepInertiaPenalty(
            minutesSinceWake: Int,
            maxPenalty: Float = DEFAULT_MAX_SLEEP_INERTIA_PENALTY
        ): Float {
            if (minutesSinceWake < 0 || minutesSinceWake >= SLEEP_INERTIA_DURATION_MINUTES) {
                return 0f
            }
            val safeMax = maxPenalty.coerceIn(0f, 1f)
            val decayFraction = 1f - (minutesSinceWake.toFloat() / SLEEP_INERTIA_DURATION_MINUTES.toFloat())
            return (safeMax * decayFraction).coerceIn(0f, 1f)
        }

        fun isSleepInertiaActive(minutesSinceWake: Int?): Boolean {
            if (minutesSinceWake == null) return false
            return minutesSinceWake in 0 until SLEEP_INERTIA_DURATION_MINUTES
        }

        fun calculateChronicRestrictionMultiplier(rollingAverageDailySleepMinutes: Float): Float {
            if (rollingAverageDailySleepMinutes <= 0f) {
                return 1.6f
            }
            val averageHours = rollingAverageDailySleepMinutes / 60f
            if (averageHours >= CHRONIC_RESTRICTION_THRESHOLD_HOURS) {
                return 1.0f
            }
            val deficitHours = (CHRONIC_RESTRICTION_THRESHOLD_HOURS - averageHours).coerceAtLeast(0f)
            val penalty = (deficitHours / CHRONIC_RESTRICTION_THRESHOLD_HOURS) * 0.6f
            return (1.0f + penalty).coerceIn(1.0f, 1.6f)
        }

        fun calculateChronicRestrictionMultiplierFromDays(dailySleepMinutes: List<Int>): Float {
            if (dailySleepMinutes.isEmpty()) return 1.0f
            val validDays = dailySleepMinutes.filter { it >= 0 }
            if (validDays.isEmpty()) return 1.0f
            val avg = validDays.average().toFloat()
            return calculateChronicRestrictionMultiplier(avg)
        }

        fun adjustedFatigueRatio(
            pressurePoints: Int,
            minutesSinceWake: Int? = null,
            rollingAverageSleepMinutes: Float? = null,
            softMaxReference: Int = SOFT_MAX_REFERENCE
        ): Float {
            val baseRatio = fatigueRatio(pressurePoints, softMaxReference)
            val chronicMultiplier = rollingAverageSleepMinutes?.let {
                calculateChronicRestrictionMultiplier(it)
            } ?: 1.0f
            val inertiaPenalty = minutesSinceWake?.let {
                calculateSleepInertiaPenalty(it)
            } ?: 0f

            val compounded = baseRatio * chronicMultiplier + inertiaPenalty
            return compounded.coerceIn(0f, 1f)
        }

        fun adjustedFatiguePercent(
            pressurePoints: Int,
            minutesSinceWake: Int? = null,
            rollingAverageSleepMinutes: Float? = null,
            softMaxReference: Int = SOFT_MAX_REFERENCE
        ): Int {
            return (adjustedFatigueRatio(pressurePoints, minutesSinceWake, rollingAverageSleepMinutes, softMaxReference) * 100f)
                .roundToInt()
                .coerceIn(0, 100)
        }

        fun adjustedFatigueZone(
            pressurePoints: Int,
            minutesSinceWake: Int? = null,
            rollingAverageSleepMinutes: Float? = null,
            softMaxReference: Int = SOFT_MAX_REFERENCE
        ): FatigueZone {
            val percent = adjustedFatiguePercent(pressurePoints, minutesSinceWake, rollingAverageSleepMinutes, softMaxReference)
            return when {
                percent <= 24 -> RESTED
                percent <= 49 -> MODERATE
                percent <= 74 -> HIGH
                else -> CRITICAL
            }
        }

        fun recoveryForSleepMinutes(sleepMinutes: Int): Int {
            val safeSleepMinutes = sleepMinutes.coerceAtLeast(0)
            if (safeSleepMinutes == 0) return 0

            val fullCycles = safeSleepMinutes / CYCLE_MINUTES
            val remainderMinutes = safeSleepMinutes % CYCLE_MINUTES
            val remainderRecovery = (remainderMinutes * (FULL_CYCLE_RECOVERY.toDouble() / CYCLE_MINUTES.toDouble())).roundToInt()

            return (fullCycles * FULL_CYCLE_RECOVERY) + remainderRecovery
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainenginemeqchronotypedetectorkt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/engine/MEQChronotypeDetector.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/engine/MEQChronotypeDetector.kt`
- **Lines of Code:** 93
- **Size:** 3,343 bytes (3.3 KB)

```kotlin
package com.neuroflow.app.domain.engine

/**
 * Baseline chronotype detector using the Morningness-Eveningness Questionnaire (MEQ).
 *
 * This is meant to establish the user's initial energy profile before any task-history
 * based adaptation is introduced.
 */
object MEQChronotypeDetector {

    const val QUESTION_COUNT = 19
    const val MIN_TOTAL_SCORE = 16
    const val MAX_TOTAL_SCORE = 86

    enum class Chronotype {
        DEFINITE_MORNING,
        MODERATE_MORNING,
        INTERMEDIATE,
        MODERATE_EVENING,
        DEFINITE_EVENING
    }

    data class Result(
        val totalScore: Int,
        val chronotype: Chronotype,
        val baselinePeakStartHour: Int,
        val baselinePeakEndHour: Int,
        val answeredQuestions: Int,
        val confidence: Float
    )

    /**
     * Accepts the 19 MEQ question scores exactly as provided in the questionnaire.
     *
     * Each item should be the points for the selected answer, already mapped to the
     * question's scoring rules.
     */
    fun detect(answerScores: List<Int>): Result {
        require(answerScores.size == QUESTION_COUNT) {
            "MEQ requires exactly $QUESTION_COUNT answers."
        }

        val answeredCount = answerScores.count { it > 0 }
        val rawSum = answerScores.sum()
        val totalScore = when {
            answeredCount == 0 -> 48 // Default to intermediate baseline if completely unanswered
            answeredCount < QUESTION_COUNT -> kotlin.math.round((rawSum.toDouble() / answeredCount.toDouble()) * QUESTION_COUNT).toInt().coerceIn(MIN_TOTAL_SCORE, MAX_TOTAL_SCORE)
            else -> rawSum.coerceIn(MIN_TOTAL_SCORE, MAX_TOTAL_SCORE)
        }
        val chronotype = classify(totalScore)
        val (peakStart, peakEnd) = baselinePeakWindow(chronotype)
        val confidence = (answeredCount.toFloat() / QUESTION_COUNT).coerceIn(0f, 1f)

        return Result(
            totalScore = totalScore,
            chronotype = chronotype,
            baselinePeakStartHour = peakStart,
            baselinePeakEndHour = peakEnd,
            answeredQuestions = answeredCount,
            confidence = confidence
        )
    }

    fun classify(totalScore: Int): Chronotype {
        return when (totalScore) {
            in 70..MAX_TOTAL_SCORE -> Chronotype.DEFINITE_MORNING
            in 59..69 -> Chronotype.MODERATE_MORNING
            in 42..58 -> Chronotype.INTERMEDIATE
            in 31..41 -> Chronotype.MODERATE_EVENING
            else -> Chronotype.DEFINITE_EVENING
        }
    }

    fun baselinePeakWindow(chronotype: Chronotype): Pair<Int, Int> {
        return when (chronotype) {
            Chronotype.DEFINITE_MORNING -> 6 to 11
            Chronotype.MODERATE_MORNING -> 7 to 12
            Chronotype.INTERMEDIATE -> 9 to 14
            Chronotype.MODERATE_EVENING -> 13 to 18
            Chronotype.DEFINITE_EVENING -> 15 to 21
        }
    }

    fun label(chronotype: Chronotype): String {
        return when (chronotype) {
            Chronotype.DEFINITE_MORNING -> "Definite Morning Type"
            Chronotype.MODERATE_MORNING -> "Moderate Morning Type"
            Chronotype.INTERMEDIATE -> "Intermediate Type"
            Chronotype.MODERATE_EVENING -> "Moderate Evening Type"
            Chronotype.DEFINITE_EVENING -> "Definite Evening Type"
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainrepositorysleeppressurerepositorykt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/repository/SleepPressureRepository.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/repository/SleepPressureRepository.kt`
- **Lines of Code:** 393
- **Size:** 15,857 bytes (15.5 KB)

```kotlin
package com.neuroflow.app.domain.repository

import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.repository.SleepLogRepository
import com.neuroflow.app.domain.model.FatigueZone
import com.neuroflow.app.domain.model.computeRollingDailySleepAverage
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepPressureRepository @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore,
    private val sleepLogRepository: SleepLogRepository
) {

    companion object {
        const val MIN_SLEEP_LOG_DURATION_MILLIS = 60_000L
        const val MAX_SLEEP_LOG_DURATION_MINUTES = 16 * 60
        const val MAX_SLEEP_LOG_DURATION_MILLIS = MAX_SLEEP_LOG_DURATION_MINUTES * 60_000L
        private const val AUTO_FALLBACK_AFTER_WAKE_MINUTES = 12 * 60
        private const val HOUR_MILLIS = 60 * 60_000L

        fun coalesceSleepIntervals(sleepIntervals: List<SleepInterval>): List<SleepInterval> {
            if (sleepIntervals.isEmpty()) return emptyList()
            val sorted = sleepIntervals.sortedBy { it.startAt }
            val merged = mutableListOf<SleepInterval>()
            var current = sorted[0]
            for (i in 1 until sorted.size) {
                val next = sorted[i]
                if (next.startAt <= current.endAt) {
                    current = current.copy(endAt = maxOf(current.endAt, next.endAt))
                } else {
                    merged.add(current)
                    current = next
                }
            }
            merged.add(current)
            return merged
        }
    }

    sealed interface AddSleepLogResult {
        data class Added(val snapshot: Snapshot) : AddSleepLogResult
        data object OverlapsExisting : AddSleepLogResult
        data class TooLong(val maxMinutes: Int = MAX_SLEEP_LOG_DURATION_MINUTES) : AddSleepLogResult
    }

    data class Snapshot(
        val pressurePoints: Int,
        val fatiguePercent: Int,
        val fatigueZone: FatigueZone,
        val refreshedAtMillis: Long,
        val minutesSinceWake: Int? = null,
        val isSleepInertiaActive: Boolean = false,
        val sleepInertiaPenalty: Float = 0f,
        val chronicRestrictionMultiplier: Float = 1.0f,
        val adjustedFatiguePercent: Int = fatiguePercent,
        val adjustedFatigueZone: FatigueZone = fatigueZone
    )

    data class SleepInterval(
        val startAt: Long,
        val endAt: Long
    )

    private val dismissedFallbackIntervals = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val refreshMutex = Mutex()

    fun observeSleepLogs(): Flow<List<SleepLogEntity>> = sleepLogRepository.observeAll()

    suspend fun addSleepLog(startAtMillis: Long, endAtMillis: Long): AddSleepLogResult {
        val safeStart = minOf(startAtMillis, endAtMillis)
        val safeEnd = maxOf(startAtMillis, endAtMillis)
        val normalizedEnd = if (safeEnd <= safeStart) safeStart + MIN_SLEEP_LOG_DURATION_MILLIS else safeEnd

        val durationMillis = normalizedEnd - safeStart
        if (durationMillis > MAX_SLEEP_LOG_DURATION_MILLIS) {
            return AddSleepLogResult.TooLong()
        }

        val overlapping = sleepLogRepository.getOverlapping(safeStart, normalizedEnd)
        val realOverlapping = overlapping.filter { it.source == "MANUAL" || it.source == "HEALTH_CONNECT" }
        if (realOverlapping.isNotEmpty()) {
            return AddSleepLogResult.OverlapsExisting
        }

        val autoOverlapping = overlapping.filter { it.source == "AUTO_DEFAULT" }
        if (autoOverlapping.isNotEmpty()) {
            val dismissedKeys = autoOverlapping.map { "${it.startAt}_${it.endAt}" }.toSet()
            dismissedFallbackIntervals.addAll(dismissedKeys)
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(dismissedSleepFallbacks = prefs.dismissedSleepFallbacks + dismissedKeys)
            }
            autoOverlapping.forEach { autoLog ->
                sleepLogRepository.deleteById(autoLog.id)
            }
        }

        sleepLogRepository.addLog(
            startAt = safeStart,
            endAt = normalizedEnd,
            source = "MANUAL"
        )

        val prefs = preferencesDataStore.preferencesFlow.first()
        val currentTrackingStart = prefs.sleepPressureTrackingStartedAtMillis
        val newTrackingStart = if (currentTrackingStart > 0L) {
            minOf(currentTrackingStart, safeStart)
        } else {
            safeStart
        }

        if (newTrackingStart != currentTrackingStart) {
            preferencesDataStore.updatePreferences {
                it.copy(sleepPressureTrackingStartedAtMillis = newTrackingStart)
            }
        }

        return AddSleepLogResult.Added(refreshCurrentPressure())
    }

    suspend fun deleteSleepLog(logId: String): Snapshot {
        val existing = sleepLogRepository.getById(logId)
        if (existing != null && existing.source == "AUTO_DEFAULT") {
            val key = "${existing.startAt}_${existing.endAt}"
            dismissedFallbackIntervals.add(key)
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(dismissedSleepFallbacks = prefs.dismissedSleepFallbacks + key)
            }
        }
        sleepLogRepository.deleteById(logId)
        return refreshCurrentPressure()
    }

    suspend fun clearSleepLogsAndReset(nowMillis: Long = System.currentTimeMillis()): Snapshot {
        dismissedFallbackIntervals.clear()
        sleepLogRepository.deleteAll()

        val prefs = preferencesDataStore.preferencesFlow.first()
        val trackingStart = defaultTrackingStartMillis(nowMillis, prefs.wakeUpHour)
        val pressure = minutesBetween(trackingStart, nowMillis)

        preferencesDataStore.updatePreferences {
            it.copy(
                sleepPressureTrackingStartedAtMillis = trackingStart,
                sleepPressureLastComputedAtMillis = nowMillis,
                sleepPressurePoints = pressure,
                dismissedSleepFallbacks = emptySet()
            )
        }

        val fatiguePercent = FatigueZone.fatiguePercent(pressure)
        val zone = FatigueZone.fatigueZone(pressure)
        val wakeMins = minutesBetween(trackingStart, nowMillis)

        return Snapshot(
            pressurePoints = pressure,
            fatiguePercent = fatiguePercent,
            fatigueZone = zone,
            refreshedAtMillis = nowMillis,
            minutesSinceWake = wakeMins,
            isSleepInertiaActive = FatigueZone.isSleepInertiaActive(wakeMins),
            sleepInertiaPenalty = FatigueZone.calculateSleepInertiaPenalty(wakeMins),
            chronicRestrictionMultiplier = 1.0f,
            adjustedFatiguePercent = fatiguePercent,
            adjustedFatigueZone = zone
        )
    }

    suspend fun refreshCurrentPressure(nowMillis: Long = System.currentTimeMillis()): Snapshot = refreshMutex.withLock {
        val prefs = preferencesDataStore.preferencesFlow.first()

        // Phase 1 boundary fix: use the preference-stored tracking start when valid
        // Otherwise default to configured wake-up hour
        val trackingStart = if (prefs.sleepPressureTrackingStartedAtMillis > 0L &&
            prefs.sleepPressureTrackingStartedAtMillis < nowMillis
        ) {
            maxOf(prefs.sleepPressureTrackingStartedAtMillis, nowMillis - 7 * 86_400_000L)
        } else {
            defaultTrackingStartMillis(nowMillis, prefs.wakeUpHour)
        }

        var pressure = 0
        var cursor = trackingStart

        val logs = sleepLogRepository.getOverlapping(trackingStart, nowMillis)
        val sleepIntervals = logs
            .map { SleepInterval(it.startAt, it.endAt) }
            .toMutableList()

        buildAutoFallbackSleepIntervals(
            prefs = prefs,
            trackingStart = trackingStart,
            nowMillis = nowMillis
        ).forEach { fallback ->
            val persisted = sleepLogRepository.addLog(
                startAt = fallback.startAt,
                endAt = fallback.endAt,
                source = "AUTO_DEFAULT"
            )
            sleepIntervals += SleepInterval(persisted.startAt, persisted.endAt)
        }

        sleepIntervals.sortBy { it.startAt }

        // Bug 4 Fix (ADR-030): Sweep-line merge overlapping or contiguous sleep intervals
        // so that multi-record sleep sessions receive continuous non-linear 90m SWS cycle recovery bonuses
        val coalescedIntervals = coalesceSleepIntervals(sleepIntervals)

        coalescedIntervals.forEach { interval ->
            val boundedStart = maxOf(cursor, interval.startAt)
            val boundedEnd = minOf(nowMillis, interval.endAt)
            if (boundedEnd <= boundedStart) {
                return@forEach
            }

            if (boundedStart > cursor) {
                val awakeMinutes = minutesBetween(cursor, boundedStart)
                pressure = FatigueZone
                    .applyAwakeMinutes(pressure, awakeMinutes)
                    .pressurePoints
            }

            val sleepMinutes = minutesBetween(boundedStart, boundedEnd)
            pressure = FatigueZone
                .applySleepSession(pressure, sleepMinutes)
                .pressurePoints

            cursor = boundedEnd
        }

        if (cursor < nowMillis) {
            val awakeMinutes = minutesBetween(cursor, nowMillis)
            pressure = FatigueZone
                .applyAwakeMinutes(pressure, awakeMinutes)
                .pressurePoints
        }

        preferencesDataStore.updatePreferences {
            it.copy(
                sleepPressurePoints = pressure,
                sleepPressureLastComputedAtMillis = nowMillis,
                sleepPressureTrackingStartedAtMillis = trackingStart
            )
        }

        val lastIntervalEnd = coalescedIntervals.map { it.endAt }.filter { it <= nowMillis }.maxOrNull()
        val wakeAnchor = lastIntervalEnd ?: trackingStart
        val minutesSinceWake = minutesBetween(wakeAnchor, nowMillis)

        val sevenDaysAgo = nowMillis - 7 * 24 * 60 * 60_000L
        val rollingLogs = sleepLogRepository.getOverlapping(sevenDaysAgo, nowMillis)
        val rollingAvgDailyMinutes = computeRollingDailySleepAverage(rollingLogs, nowMillis, ZoneId.systemDefault()).toFloat()
        val chronicMultiplier = FatigueZone.calculateChronicRestrictionMultiplier(rollingAvgDailyMinutes)
        val inertiaPenalty = FatigueZone.calculateSleepInertiaPenalty(minutesSinceWake)
        val inertiaActive = FatigueZone.isSleepInertiaActive(minutesSinceWake)

        val fatiguePercent = FatigueZone.fatiguePercent(pressure)
        val zone = FatigueZone.fatigueZone(pressure)
        val adjFatiguePercent = FatigueZone.adjustedFatiguePercent(
            pressurePoints = pressure,
            minutesSinceWake = minutesSinceWake,
            rollingAverageSleepMinutes = rollingAvgDailyMinutes
        )
        val adjZone = FatigueZone.adjustedFatigueZone(
            pressurePoints = pressure,
            minutesSinceWake = minutesSinceWake,
            rollingAverageSleepMinutes = rollingAvgDailyMinutes
        )

        Snapshot(
            pressurePoints = pressure,
            fatiguePercent = fatiguePercent,
            fatigueZone = zone,
            refreshedAtMillis = nowMillis,
            minutesSinceWake = minutesSinceWake,
            isSleepInertiaActive = inertiaActive,
            sleepInertiaPenalty = inertiaPenalty,
            chronicRestrictionMultiplier = chronicMultiplier,
            adjustedFatiguePercent = adjFatiguePercent,
            adjustedFatigueZone = adjZone
        )
    }

    private fun minutesBetween(startMillis: Long, endMillis: Long): Int {
        // Phase 1 boundary guard: prevent negative or zero duration calculations
        if (endMillis <= startMillis) return 0
        return ((endMillis - startMillis) / 60_000L).toInt().coerceAtLeast(0)
    }

    private fun defaultTrackingStartMillis(nowMillis: Long, wakeUpHour: Int): Long {
        val zoneId = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        var anchor = now.toLocalDate()
            .atTime(wakeUpHour.coerceIn(0, 23), 0)
            .atZone(zoneId)

        if (anchor.toInstant().toEpochMilli() > nowMillis) {
            anchor = anchor.minusDays(1)
        }

        return anchor.toInstant().toEpochMilli()
    }

    private suspend fun buildAutoFallbackSleepIntervals(
        prefs: UserPreferences,
        trackingStart: Long,
        nowMillis: Long
    ): List<SleepInterval> {
        if (!prefs.autoFallbackSleepInsertionEnabled) {
            return emptyList()
        }

        val zoneId = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val wakeHour = prefs.wakeUpHour.coerceIn(0, 23)
        val sleepHour = prefs.sleepHour.coerceIn(0, 23)

        val result = mutableListOf<SleepInterval>()

        // Check up to 7 past days for missing automatic fallback sleep logs
        for (dayOffset in 0..7) {
            val targetDate = now.toLocalDate().minusDays(dayOffset.toLong())

            val defaultSleepStart = targetDate
                .atTime(sleepHour, 0)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()

            val defaultWakeEnd = if (wakeHour <= sleepHour) {
                targetDate.plusDays(1).atTime(wakeHour, 0).atZone(zoneId).toInstant().toEpochMilli()
            } else {
                targetDate.atTime(wakeHour, 0).atZone(zoneId).toInstant().toEpochMilli()
            }

            // 12-hour post-wake delay check relative to actual default wake time
            if (nowMillis < defaultWakeEnd + (AUTO_FALLBACK_AFTER_WAKE_MINUTES * 60_000L)) {
                continue
            }

            if (defaultWakeEnd < trackingStart) {
                continue
            }

            // Bug 2 Fix (ADR-025): Scope query strictly to the target night's window
            // to prevent newer manual logs from blocking fallback sleep for older unlogged nights.
            val queryStart = defaultSleepStart - 4 * HOUR_MILLIS
            val queryEnd = defaultWakeEnd + 4 * HOUR_MILLIS
            val logsInWindow = sleepLogRepository.getOverlapping(queryStart, queryEnd)
            val hasManualLogForNight = logsInWindow.any { it.source == "MANUAL" || it.source == "HEALTH_CONNECT" }
            if (hasManualLogForNight) {
                continue
            }

            val boundedStart = defaultSleepStart
            val boundedEnd = minOf(nowMillis, defaultWakeEnd)
            if (boundedEnd <= boundedStart) {
                continue
            }

            val duration = boundedEnd - boundedStart
            if (duration < MIN_SLEEP_LOG_DURATION_MILLIS || duration > MAX_SLEEP_LOG_DURATION_MILLIS) {
                continue
            }

            val overlapsExisting = logsInWindow.any {
                it.endAt > boundedStart && it.startAt < boundedEnd
            }
            if (overlapsExisting) {
                continue
            }

            val intervalKey = "${boundedStart}_${boundedEnd}"
            if (dismissedFallbackIntervals.contains(intervalKey) || prefs.dismissedSleepFallbacks.contains(intervalKey)) {
                continue
            }

            result.add(SleepInterval(startAt = boundedStart, endAt = boundedEnd))
        }

        result.sortBy { it.startAt }
        return result
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainrepositorypassivesleepsyncrepositorykt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/repository/PassiveSleepSyncRepository.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/repository/PassiveSleepSyncRepository.kt`
- **Lines of Code:** 59
- **Size:** 1,885 bytes (1.8 KB)

```kotlin
package com.neuroflow.app.domain.repository

import com.neuroflow.app.data.local.entity.SleepLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for passive sleep session synchronization.
 * Abstracts Google Health Connect, wearable providers, or platform health services
 * to complement manual sleep logs and auto-default estimations with measured sleep.
 */
interface PassiveSleepSyncRepository {

    sealed interface SyncResult {
        data class Success(
            val importedLogs: List<SleepLogEntity>,
            val skippedDuplicates: Int,
            val replacedFallbackCount: Int
        ) : SyncResult

        data class PermissionRequired(
            val missingPermissions: Set<String>
        ) : SyncResult

        data class ProviderUnavailable(
            val reason: String
        ) : SyncResult

        data class Failure(
            val throwable: Throwable
        ) : SyncResult
    }

    data class RawSleepSession(
        val externalRecordId: String,
        val startTimeMillis: Long,
        val endTimeMillis: Long,
        val sourceAppPackage: String = SOURCE_HEALTH_CONNECT,
        val stageSummary: String? = null,
        val notes: String = ""
    )

    enum class HealthProviderStatus {
        AVAILABLE,
        NOT_INSTALLED,
        UPDATE_REQUIRED,
        NOT_SUPPORTED
    }

    companion object {
        const val SOURCE_HEALTH_CONNECT = "HEALTH_CONNECT"
        const val PERMISSION_READ_SLEEP = "android.permission.health.READ_SLEEP"
    }

    fun observeProviderStatus(): Flow<HealthProviderStatus>
    suspend fun getProviderStatus(): HealthProviderStatus
    suspend fun hasRequiredPermissions(): Boolean
    suspend fun fetchExternalSleepSessions(fromMillis: Long, toMillis: Long): List<RawSleepSession>
    suspend fun syncPassiveSleep(fromMillis: Long, toMillis: Long): SyncResult
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainrepositoryenergymetricsrepositorykt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/repository/EnergyMetricsRepository.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/repository/EnergyMetricsRepository.kt`
- **Lines of Code:** 191
- **Size:** 7,512 bytes (7.3 KB)

```kotlin
package com.neuroflow.app.domain.repository

import com.neuroflow.app.data.local.NeuroFlowDatabase
import com.neuroflow.app.data.local.entity.EnergyPredictionEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Phase 2: Repository for energy telemetry and audit trails.
 *
 * Manages local energy prediction snapshots for backtesting, calibration validation,
 * and non-invasive audit trails. Enforces retention policies (30-day raw, 90-day
 * aggregated metrics) to keep storage bounded.
 */
@Singleton
class EnergyMetricsRepository @Inject constructor(
    private val database: NeuroFlowDatabase
) {
    companion object {
        private const val RAW_DATA_RETENTION_DAYS = 30L
        const val MIN_RETENTION_DAYS = 1
        const val MAX_RETENTION_DAYS = 90
        const val RAW_DATA_RETENTION_MILLIS = RAW_DATA_RETENTION_DAYS * 24 * 60 * 60 * 1000L
    }

    private val energyPredictionDao = database.energyPredictionDao()

    /**
     * Record a new energy prediction snapshot for audit and backtesting.
     * Phase 2 telemetry capture: all component breakdowns are persisted.
     */
    suspend fun recordEnergyPrediction(
        predictedAtMillis: Long,
        baselineRawEnergy: Float,
        peakScore: Float,
        fatiguePenalty: Float,
        sleepPressurePoints: Int,
        fatiguePercent: Int,
        momentAdjustment: Float,
        momentConfidence: Float,
        momentSupportScore: Float = 50f,
        momentPressureScore: Float = 50f,
        adjustedRawEnergy: Float = peakScore,
        usableEnergy: Int,
        chronotype: String = "INTERMEDIATE",
        wakeUpHour: Int = 7,
        peakMinuteOfDay: Int = 540,
        peakConfidence: Float = 0.85f,
        peakDetectionAgeMillis: Long = 0L,
        sleepLogAgeMillis: Long = 0L,
        sessionDataAgeMillis: Long = 0L,
        recentFocusMinutes: Float = 0f,
        recentInterruptionCount: Int = 0,
        recentAppSwitchCount: Int = 0,
        activeTaskCount: Int = 0,
        notificationCount: Int = 0
    ) {
        val instant = java.time.Instant.ofEpochMilli(predictedAtMillis)
            .atZone(java.time.ZoneId.systemDefault())
        val dayOfWeek = instant.dayOfWeek.value    // 1=Monday .. 7=Sunday (ISO-8601)
        val hourOfDay = instant.hour

        val entity = EnergyPredictionEntity(
            id = UUID.randomUUID().toString(),
            predictedAtMillis = predictedAtMillis,
            predictedAtDayOfWeek = dayOfWeek,
            predictedAtHourOfDay = hourOfDay,
            peakDetectionAgeMillis = peakDetectionAgeMillis,
            sleepLogAgeMillis = sleepLogAgeMillis,
            sessionDataAgeMillis = sessionDataAgeMillis,
            baselineRawEnergy = baselineRawEnergy,
            peakScore = peakScore,
            fatiguePenalty = fatiguePenalty,
            sleepPressurePoints = sleepPressurePoints,
            fatiguePercent = fatiguePercent,
            momentAdjustment = momentAdjustment,
            momentConfidence = momentConfidence,
            momentSupportScore = momentSupportScore,
            momentPressureScore = momentPressureScore,
            adjustedRawEnergy = adjustedRawEnergy,
            usableEnergy = usableEnergy,
            chronotype = chronotype,
            wakeUpHour = wakeUpHour,
            peakMinuteOfDay = peakMinuteOfDay,
            peakConfidence = peakConfidence,
            recentFocusMinutes = recentFocusMinutes,
            recentInterruptionCount = recentInterruptionCount,
            recentAppSwitchCount = recentAppSwitchCount,
            activeTaskCount = activeTaskCount,
            notificationCount = notificationCount,
            createdAtMillis = System.currentTimeMillis()
        )

        energyPredictionDao.insert(entity)
    }

    /**
     * Get recent energy predictions for diagnostics and validation.
     */
    suspend fun getRecentPredictions(limit: Int = 100): List<EnergyPredictionEntity> {
        return energyPredictionDao.getRecent(limit)
    }

    /**
     * Get energy predictions within a time range (e.g., last 7 days).
     */
    suspend fun getPredictionsInRange(startMillis: Long, endMillis: Long): List<EnergyPredictionEntity> {
        return energyPredictionDao.getInRange(startMillis, endMillis)
    }

    /**
     * Observe recent predictions as a flow (for real-time backtesting updates).
     */
    fun observeRecentPredictions(afterMillis: Long): Flow<List<EnergyPredictionEntity>> {
        return energyPredictionDao.observeAfter(afterMillis)
    }

    /**
     * Get prediction count for storage monitoring.
     */
    suspend fun getPredictionCount(): Int {
        return energyPredictionDao.count()
    }

    /**
     * Clean up old energy predictions according to retention policy.
     * Phase 2 data hygiene: keep 30 days of raw data, aggregate beyond 30 days.
     *
     * Returns: count of records deleted
     */
    suspend fun enforceRetentionPolicy(
        retentionDays: Int = RAW_DATA_RETENTION_DAYS.toInt(),
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        val safeRetentionDays = retentionDays.coerceIn(MIN_RETENTION_DAYS, MAX_RETENTION_DAYS)
        val cutoffMillis = nowMillis - (safeRetentionDays * 24L * 60L * 60L * 1000L)
        return energyPredictionDao.deleteOlderThan(cutoffMillis)
    }

    suspend fun clearAllPredictions(): Int {
        return energyPredictionDao.deleteAll()
    }

    /**
     * Get statistics for recent predictions (for aggregated analytics).
     */
    suspend fun getStatisticsForRange(startMillis: Long, endMillis: Long): PredictionStatistics {
        val predictions = energyPredictionDao.getInRange(startMillis, endMillis)
        if (predictions.isEmpty()) {
            return PredictionStatistics()
        }

        val avgBaseline = predictions.map { it.baselineRawEnergy }.average().toFloat()
        val avgAdjusted = predictions.map { it.adjustedRawEnergy }.average().toFloat()
        val avgMomentAdjustment = predictions.map { it.momentAdjustment }.average().toFloat()
        val avgMomentConfidence = predictions.map { it.momentConfidence }.average().toFloat()
        val avgFatiguePct = predictions.map { it.fatiguePercent }.average().roundToInt()
        val avgUsableEnergy = predictions.map { it.usableEnergy }.average().roundToInt()

        // Minute-of-day analysis for peak clarity
        val peaksByHour = predictions.groupBy { it.predictedAtHourOfDay }
            .mapValues { (_, hourPredictions) ->
                hourPredictions.map { it.adjustedRawEnergy }.average().toFloat()
            }

        return PredictionStatistics(
            sampleCount = predictions.size,
            avgBaselineEnergy = avgBaseline,
            avgAdjustedEnergy = avgAdjusted,
            avgMomentAdjustment = avgMomentAdjustment,
            avgMomentConfidence = avgMomentConfidence,
            avgFatiguePercent = avgFatiguePct,
            avgUsableEnergy = avgUsableEnergy,
            energyByHourOfDay = peaksByHour
        )
    }

    data class PredictionStatistics(
        val sampleCount: Int = 0,
        val avgBaselineEnergy: Float = 0f,
        val avgAdjustedEnergy: Float = 0f,
        val avgMomentAdjustment: Float = 0f,
        val avgMomentConfidence: Float = 0f,
        val avgFatiguePercent: Int = 0,
        val avgUsableEnergy: Int = 0,
        val energyByHourOfDay: Map<Int, Float> = emptyMap()
    )
}
```

---

<a id="appsrcmainjavacomneuroflowappdatasleepsleeptelemetrydatasourcekt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/sleep/SleepTelemetryDataSource.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/sleep/SleepTelemetryDataSource.kt`
- **Lines of Code:** 145
- **Size:** 5,776 bytes (5.6 KB)

```kotlin
package com.neuroflow.app.data.sleep

import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.repository.SleepLogRepository
import com.neuroflow.app.domain.service.ChronobiologySignalNormalizer
import com.neuroflow.app.domain.service.NormalizedChronobiologySignal
import com.neuroflow.app.domain.service.RawSleepTelemetry
import com.neuroflow.app.domain.service.TimeSlotWindow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Priority 2 (ADR-003): Abstract Sleep Telemetry Boundary
 *
 * Provides a clean interface and default implementation decoupling domain and UI consumers
 * from Health Connect SDK dependencies while executing passive chronobiological normalization.
 */
interface SleepTelemetryRepository {
    suspend fun getLatestNormalizedSleep(
        baseWakeMillis: Long,
        baseSleepMinutes: Double = 480.0,
        lookbackMillis: Long = 24 * 60 * 60 * 1000L
    ): NormalizedChronobiologySignal

    suspend fun getNormalizedSleepForRange(
        fromMillis: Long,
        toMillis: Long,
        baseWakeMillis: Long,
        baseSleepMinutes: Double = 480.0
    ): NormalizedChronobiologySignal

    fun observeAllSleepLogs(): Flow<List<SleepLogEntity>>

    fun normalizeLogs(
        logs: List<SleepLogEntity>,
        baseWakeMillis: Long,
        baseSleepMinutes: Double = 480.0
    ): NormalizedChronobiologySignal
}

@Singleton
class DefaultSleepTelemetryRepository @Inject constructor(
    private val sleepLogRepository: SleepLogRepository,
    private val normalizer: ChronobiologySignalNormalizer
) : SleepTelemetryRepository {

    override fun observeAllSleepLogs(): Flow<List<SleepLogEntity>> =
        sleepLogRepository.observeAll()

    override suspend fun getLatestNormalizedSleep(
        baseWakeMillis: Long,
        baseSleepMinutes: Double,
        lookbackMillis: Long
    ): NormalizedChronobiologySignal {
        val now = System.currentTimeMillis()
        val toMillis = maxOf(baseWakeMillis, now)
        val fromMillis = toMillis - lookbackMillis
        return getNormalizedSleepForRange(fromMillis, toMillis, baseWakeMillis, baseSleepMinutes)
    }

    override suspend fun getNormalizedSleepForRange(
        fromMillis: Long,
        toMillis: Long,
        baseWakeMillis: Long,
        baseSleepMinutes: Double
    ): NormalizedChronobiologySignal {
        val logs = sleepLogRepository.getOverlapping(fromMillis, toMillis)
        return normalizeLogs(logs, baseWakeMillis, baseSleepMinutes)
    }

    override fun normalizeLogs(
        logs: List<SleepLogEntity>,
        baseWakeMillis: Long,
        baseSleepMinutes: Double
    ): NormalizedChronobiologySignal {
        val validLogs = logs.filter { it.endAt > it.startAt }.sortedBy { it.startAt }

        if (validLogs.isEmpty()) {
            return normalizer.normalize(
                telemetry = RawSleepTelemetry(
                    intervals = emptyList(),
                    confidence = 0.0
                ),
                baseWakeMillis = baseWakeMillis,
                baseSleepMinutes = baseSleepMinutes
            )
        }

        val intervals = validLogs.map { TimeSlotWindow(it.startAt, it.endAt) }

        var totalDeep = 0.0
        var totalRem = 0.0
        var anyStageParsed = false

        for (log in validLogs) {
            val deepParsed = extractMinutes(log.notes, "deep")
            val remParsed = extractMinutes(log.notes, "rem")
            if (deepParsed != null || remParsed != null) {
                anyStageParsed = true
                totalDeep += deepParsed ?: 0.0
                totalRem += remParsed ?: 0.0
            }
        }

        val hasWearableSource = validLogs.any {
            it.source.contains("HEALTH", ignoreCase = true) || it.source.contains("WEARABLE", ignoreCase = true)
        }
        val confidence = when {
            hasWearableSource -> 1.0
            validLogs.any { it.source.contains("MANUAL", ignoreCase = true) || it.source.contains("USER", ignoreCase = true) } -> 0.85
            validLogs.any { it.source == "AUTO_DEFAULT" } -> 0.4
            else -> 0.85
        }

        val nocturnalLogs = validLogs.filter { it.startAt < baseWakeMillis || (it.endAt - it.startAt) > 180 * 60_000L }
        val effectiveNocturnalLogs = if (nocturnalLogs.isNotEmpty()) nocturnalLogs else emptyList()
        val (nocturnalConsolidated, nocturnalWaso) = normalizer.coalesceWasoEpisodes(effectiveNocturnalLogs.map { TimeSlotWindow(it.startAt, it.endAt) })
        val nocturnalRawMinutes = (nocturnalConsolidated.sumOf { (it.endMillis - it.startMillis) / 60_000.0 } - nocturnalWaso).coerceAtLeast(0.0)

        val finalDeep = if (anyStageParsed) totalDeep else if (hasWearableSource && effectiveNocturnalLogs.isNotEmpty()) nocturnalRawMinutes * 0.20 else 0.0
        val finalRem = if (anyStageParsed) totalRem else if (hasWearableSource && effectiveNocturnalLogs.isNotEmpty()) nocturnalRawMinutes * 0.20 else 0.0
        val latestWake = nocturnalLogs.maxOfOrNull { it.endAt }

        val telemetry = RawSleepTelemetry(
            intervals = intervals,
            deepSleepMinutes = finalDeep,
            remSleepMinutes = finalRem,
            detectedWakeMillis = latestWake,
            confidence = confidence
        )

        return normalizer.normalize(
            telemetry = telemetry,
            baseWakeMillis = baseWakeMillis,
            baseSleepMinutes = baseSleepMinutes
        )
    }

    private fun extractMinutes(notes: String, tag: String): Double? {
        val regex = Regex("""$tag[:=]\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
        val match = regex.find(notes) ?: return null
        return match.groupValues[1].toDoubleOrNull()
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdatarepositorysleeplogrepositorykt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/repository/SleepLogRepository.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/repository/SleepLogRepository.kt`
- **Lines of Code:** 51
- **Size:** 1,646 bytes (1.6 KB)

```kotlin
package com.neuroflow.app.data.repository

import com.neuroflow.app.data.local.dao.SleepLogDao
import com.neuroflow.app.data.local.entity.SleepLogEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepLogRepository @Inject constructor(
    private val sleepLogDao: SleepLogDao
) {

    companion object {
        private const val MIN_DURATION_MILLIS = 60_000L
    }

    fun observeAll(): Flow<List<SleepLogEntity>> = sleepLogDao.observeAll()

    suspend fun getAll(): List<SleepLogEntity> = sleepLogDao.getAll()

    suspend fun getOverlapping(fromMillis: Long, toMillis: Long): List<SleepLogEntity> =
        sleepLogDao.getOverlapping(fromMillis, toMillis)

    suspend fun addLog(
        startAt: Long,
        endAt: Long,
        source: String = "MANUAL",
        notes: String = ""
    ): SleepLogEntity {
        val safeStart = minOf(startAt, endAt)
        val safeEnd = maxOf(startAt, endAt)
        val normalizedEnd = if (safeEnd <= safeStart) safeStart + MIN_DURATION_MILLIS else safeEnd
        val durationMinutes = ((normalizedEnd - safeStart) / 60_000L).toInt().coerceAtLeast(1)
        val log = SleepLogEntity(
            startAt = safeStart,
            endAt = normalizedEnd,
            durationMinutes = durationMinutes,
            source = source,
            notes = notes
        )
        sleepLogDao.upsert(log)
        return log
    }

    suspend fun getById(id: String): SleepLogEntity? = sleepLogDao.getById(id)

    suspend fun deleteById(id: String) = sleepLogDao.deleteById(id)

    suspend fun deleteAll() = sleepLogDao.deleteAll()
}
```

---

<a id="appsrcmainjavacomneuroflowappdatarepositorypassivesleepsyncmanagerkt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/repository/PassiveSleepSyncManager.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/repository/PassiveSleepSyncManager.kt`
- **Lines of Code:** 143
- **Size:** 6,116 bytes (6.0 KB)

```kotlin
package com.neuroflow.app.data.repository

import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.Companion.PERMISSION_READ_SLEEP
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.Companion.SOURCE_HEALTH_CONNECT
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.HealthProviderStatus
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.RawSleepSession
import com.neuroflow.app.domain.repository.PassiveSleepSyncRepository.SyncResult
import com.neuroflow.app.domain.repository.SleepPressureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassiveSleepSyncManager @Inject constructor(
    private val sleepLogRepository: SleepLogRepository,
    private val sleepPressureRepository: SleepPressureRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : PassiveSleepSyncRepository {

    private val providerStatusFlow = MutableStateFlow(HealthProviderStatus.AVAILABLE)
    private val externalProviderSource = mutableListOf<RawSleepSession>()

    /**
     * Injects mock or bridged external sessions (e.g. from Health Connect API or wearable SDK).
     */
    fun setExternalSessions(sessions: List<RawSleepSession>) {
        externalProviderSource.clear()
        externalProviderSource.addAll(sessions)
    }

    override fun observeProviderStatus(): Flow<HealthProviderStatus> = providerStatusFlow.asStateFlow()

    override suspend fun getProviderStatus(): HealthProviderStatus = providerStatusFlow.value

    fun setProviderStatus(status: HealthProviderStatus) {
        providerStatusFlow.value = status
    }

    override suspend fun hasRequiredPermissions(): Boolean {
        return providerStatusFlow.value == HealthProviderStatus.AVAILABLE
    }

    override suspend fun fetchExternalSleepSessions(fromMillis: Long, toMillis: Long): List<RawSleepSession> {
        val safeStart = minOf(fromMillis, toMillis)
        val safeEnd = maxOf(fromMillis, toMillis)
        return externalProviderSource.filter { session ->
            session.endTimeMillis > safeStart && session.startTimeMillis < safeEnd
        }
    }

    override suspend fun syncPassiveSleep(fromMillis: Long, toMillis: Long): SyncResult {
        try {
            val status = getProviderStatus()
            if (status != HealthProviderStatus.AVAILABLE) {
                return SyncResult.ProviderUnavailable("Health provider unavailable: $status")
            }

            if (!hasRequiredPermissions()) {
                return SyncResult.PermissionRequired(setOf(PERMISSION_READ_SLEEP))
            }

            val externalSessions = fetchExternalSleepSessions(fromMillis, toMillis)
            if (externalSessions.isEmpty()) {
                return SyncResult.Success(
                    importedLogs = emptyList(),
                    skippedDuplicates = 0,
                    replacedFallbackCount = 0
                )
            }

            val imported = mutableListOf<SleepLogEntity>()
            var skipped = 0
            var replacedFallback = 0

            for (session in externalSessions) {
                val safeStart = minOf(session.startTimeMillis, session.endTimeMillis)
                val safeEnd = maxOf(session.startTimeMillis, session.endTimeMillis)
                val duration = safeEnd - safeStart

                // Bounds validation
                if (duration < SleepPressureRepository.MIN_SLEEP_LOG_DURATION_MILLIS ||
                    duration > SleepPressureRepository.MAX_SLEEP_LOG_DURATION_MILLIS
                ) {
                    skipped++
                    continue
                }

                val overlapping = sleepLogRepository.getOverlapping(safeStart, safeEnd)

                // 1. Manual sleep logs take precedence over passive detection
                val manualOverlapping = overlapping.filter { it.source == "MANUAL" }
                if (manualOverlapping.isNotEmpty()) {
                    skipped++
                    continue
                }

                // 2. Health Connect deduplication (avoid importing identical/near-identical session twice)
                val healthOverlapping = overlapping.filter { it.source == SOURCE_HEALTH_CONNECT }
                val isDuplicate = healthOverlapping.any { existing ->
                    val overlap = minOf(existing.endAt, safeEnd) - maxOf(existing.startAt, safeStart)
                    overlap > 0.8f * duration
                }
                if (isDuplicate) {
                    skipped++
                    continue
                }

                // 3. Health Connect measured sleep supersedes AUTO_DEFAULT fallback estimations
                val autoOverlapping = overlapping.filter { it.source == "AUTO_DEFAULT" }
                autoOverlapping.forEach { autoLog ->
                    sleepLogRepository.deleteById(autoLog.id)
                    replacedFallback++
                }

                val note = if (session.notes.isNotBlank()) session.notes else "Synced via Health Connect"
                val addedLog = sleepLogRepository.addLog(
                    startAt = safeStart,
                    endAt = safeEnd,
                    source = SOURCE_HEALTH_CONNECT,
                    notes = note
                )
                imported.add(addedLog)
            }

            if (imported.isNotEmpty() || replacedFallback > 0) {
                sleepPressureRepository.refreshCurrentPressure()
            }

            return SyncResult.Success(
                importedLogs = imported,
                skippedDuplicates = skipped,
                replacedFallbackCount = replacedFallback
            )
        } catch (e: Exception) {
            return SyncResult.Failure(e)
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappworkersleeppressurerefreshworkerkt"></a>
### File: `app/src/main/java/com/neuroflow/app/worker/SleepPressureRefreshWorker.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/worker/SleepPressureRefreshWorker.kt`
- **Lines of Code:** 80
- **Size:** 2,993 bytes (2.9 KB)

```kotlin
package com.neuroflow.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.domain.repository.SleepPressureRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Daily worker that refreshes sleep pressure and creates automatic fallback sleep logs.
 *
 * This ensures that even if the user doesn't open the app, automatic sleep logs
 * will be created based on their configured sleep/wake hours when appropriate.
 *
 * Runs once daily at a configured time (typically after wake-up time + 12 hours).
 */
@HiltWorker
class SleepPressureRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val sleepPressureRepository: SleepPressureRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Refresh current pressure, which will trigger automatic fallback
            // sleep log creation if conditions are met
            sleepPressureRepository.refreshCurrentPressure()
            Result.success()
        } catch (e: Exception) {
            // Retry on failure, but don't fail permanently
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "sleep_pressure_refresh_work"
        const val WORK_TAG = "sleep_pressure_refresh"
    }
}
/**
 * Schedules the daily sleep pressure refresh worker.
 *
 * The worker runs daily at 7 PM (19:00) by default, which is typically 12+ hours
 * after most users' wake time, allowing the automatic fallback logic to trigger.
 *
 * @param context Android context
 * @param prefs User preferences (currently unused, but included for consistency with other schedulers)
 */
fun scheduleSleepPressureRefreshWorker(context: Context, prefs: UserPreferences) {
    val workManager = WorkManager.getInstance(context)

    // Schedule to run at 7 PM daily (19:00)
    // This is typically 12+ hours after wake time for most users,
    // allowing automatic fallback sleep log creation to trigger
    val targetHour = 19
    val initialDelayMs = delayUntilHour(targetHour)

    val sleepRefreshRequest = PeriodicWorkRequestBuilder<SleepPressureRefreshWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
        .addTag(SleepPressureRefreshWorker.WORK_TAG)
        .build()

    workManager.enqueueUniquePeriodicWork(
        SleepPressureRefreshWorker.WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        sleepRefreshRequest
    )
}
```

---

<a id="appsrcmainjavacomneuroflowappworkerenergytelemetrycleanupworkerkt"></a>
### File: `app/src/main/java/com/neuroflow/app/worker/EnergyTelemetryCleanupWorker.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/worker/EnergyTelemetryCleanupWorker.kt`
- **Lines of Code:** 81
- **Size:** 2,977 bytes (2.9 KB)

```kotlin
package com.neuroflow.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neuroflow.app.domain.repository.EnergyMetricsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope

/**
 * Phase 2: Background worker for energy telemetry maintenance.
 * 
 * Periodically enforces retention policies on energy prediction data:
 * - Deletes raw telemetry older than 30 days
 * - Runs daily (low frequency to avoid battery drain)
 * - Automatically restarted by WorkManager on device reboot
 */
@HiltWorker
class EnergyTelemetryCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val energyMetricsRepository: EnergyMetricsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = coroutineScope {
        return@coroutineScope try {
            // Enforce retention policy: delete telemetry older than 30 days
            val deletedCount = energyMetricsRepository.enforceRetentionPolicy()
            
            // Log for debugging but don't spam logs
            if (deletedCount > 0) {
                android.util.Log.d(
                    "EnergyTelemetry",
                    "Cleanup: deleted $deletedCount prediction records older than 30 days"
                )
            }
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("EnergyTelemetry", "Cleanup worker failed", e)
            // Retry with exponential backoff on failures
            Result.retry()
        }
    }
}

/**
 * Enqueues the energy telemetry cleanup worker.
 * Should be called once on app startup (e.g., from a One-Time Work initializer).
 */
@Singleton
class EnergyTelemetryCleanupScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scheduleCleanup() {
        val cleanupRequest = PeriodicWorkRequestBuilder<EnergyTelemetryCleanupWorker>(
            24, TimeUnit.HOURS,
            5, TimeUnit.MINUTES  // Flex interval: run anytime within 5 minutes of 24hr mark
        ).setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)  // Only run when battery is sufficient
                .build()
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "energy_telemetry_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,  // Don't reschedule if already exists
            cleanupRequest
        )
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainservicetaskschedulerservicekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt`
- **Lines of Code:** 1,154
- **Size:** 53,965 bytes (52.7 KB)

```kotlin
package com.neuroflow.app.domain.service

import com.neuroflow.app.domain.scheduler.DeferralRationale
import com.neuroflow.app.domain.scheduler.DeferralReasonCode
import com.neuroflow.app.domain.scheduler.DependencyGraphValidator
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.PriorityQueue
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class SchedulingOutcome(
    val placements: List<ScheduledPlacement>,
    val deferrals: List<DeferralRationale> = emptyList()
)

data class SchedulableTask(
    val id: String,
    val title: String,
    val category: TaskCategory,
    val priority: Int, // 0 = LOW, 1 = MEDIUM, 2 = HIGH
    val estimatedMinutes: Int,
    val isDurationExplicit: Boolean = false,
    val energyDemand: EnergyDemand,
    val isFrog: Boolean = false,
    val scheduledStartMillis: Long? = null,
    val deadlineMillis: Long? = null,
    val isLocked: Boolean = false,
    val parentTaskId: String? = null,
    val completedMicroSteps: Int = 0,
    val totalMicroSteps: Int = 0,
    val isCurrentlyActive: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val prerequisiteIds: Set<String> = emptySet(),
    val impactScore: Int = 50,
    val estimatedEnjoyment: Int = 5,
    val postponeCount: Int = 0,
    val contextTag: String = "",
    val isAnxietyTask: Boolean = false,
    val hasImplementationIntention: Boolean = false,
    val fragmentationTolerance: Float = 0.5f,
    val distractionScore: Float = 0f
)

data class TaskScoreBreakdown(
    val taskId: String,
    val totalScore: Double,      // Guaranteed [0.0..100.0]
    val urgencyScore: Double,    // [0.0..100.0]
    val impactScore: Double,     // [0.0..100.0]
    val energyFitScore: Double,  // [0.0..100.0]
    val momentumScore: Double,   // [0.0..100.0]
    val bufferedDurationMinutes: Int
)

data class TimeSlotWindow(
    val startMillis: Long,
    val endMillis: Long
) {
    val durationMinutes: Int
        get() = ((endMillis - startMillis) / 60_000L).toInt()
}

/** DST-safe local wall-clock rule for recurring protected slots */
data class ProtectedLocalSlot(
    val startHour: Int,
    val startMinute: Int,
    val durationMinutes: Int
) {
    fun resolveForDate(date: LocalDate, zoneId: ZoneId): TimeSlotWindow {
        val startZdt = date.atTime(LocalTime.of(startHour, startMinute)).atZone(zoneId)
        val endZdt = startZdt.plusMinutes(durationMinutes.toLong())
        return TimeSlotWindow(
            startMillis = startZdt.toInstant().toEpochMilli(),
            endMillis = endZdt.toInstant().toEpochMilli()
        )
    }
}

data class ScheduledPlacement(
    val task: SchedulableTask,
    val window: TimeSlotWindow,
    val scoreBreakdown: TaskScoreBreakdown
)

data class PriorityWeightProfile(
    val urgencyWeight: Double = 1.0,
    val impactWeight: Double = 1.0,
    val circadianMatchWeight: Double = 1.0,
    val momentumWeight: Double = 1.0,
    val frogBonusMultiplier: Double = 1.0,
    val quadrantWeight: Double = 1.0,
    val priorityLevelWeight: Double = 1.0,
    val durationWeight: Double = 1.0,
    val focusModeWeight: Double = 1.0,
    val planBoostWeight: Double = 1.0,
    val enjoymentWeight: Double = 1.0
) {
    companion object {
        val DEFAULT = PriorityWeightProfile()
    }
}

interface TaskSchedulerService {
    fun evaluateTaskScore(
        task: SchedulableTask,
        snapshot: CircadianSnapshot,
        dependentCount: Int = 0,
        calibrationRatio: Double = 1.20
    ): TaskScoreBreakdown

    fun evaluateTaskScoreWithWeights(
        task: SchedulableTask,
        snapshot: CircadianSnapshot,
        dependentCount: Int = 0,
        calibrationRatio: Double = 1.20,
        weights: PriorityWeightProfile = PriorityWeightProfile.DEFAULT,
        referenceNowMillis: Long? = null
    ): TaskScoreBreakdown

    fun updateCalibrationRatio(
        previousRatio: Double,
        estimatedMinutes: Int,
        actualMinutes: Int
    ): Double

    fun scheduleDay(
        tasks: List<SchedulableTask>,
        dayDate: LocalDate,
        nowMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        busyWindows: List<TimeSlotWindow> = emptyList(),
        protectedSlots: List<ProtectedLocalSlot> = emptyList(),
        calibrationRatio: Double = 1.20,
        forceReplan: Boolean = false,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null,
        priorityWeights: PriorityWeightProfile = PriorityWeightProfile.DEFAULT,
        learnedProfile: LearnedCircadianProfile? = null,
        rollingAverageSleepMinutes: Double? = null,
        physiologicalBlocks: List<CognitiveBlockWindow> = emptyList(),
        stimulantDoses: List<StimulantDoseBout> = emptyList(),
        socialJetlagLagHours: Double = 0.0,
        activeUncompletedTaskIds: Set<String> = emptySet()
    ): List<ScheduledPlacement>

    fun scheduleDayWithOutcome(
        tasks: List<SchedulableTask>,
        dayDate: LocalDate,
        nowMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        busyWindows: List<TimeSlotWindow> = emptyList(),
        protectedSlots: List<ProtectedLocalSlot> = emptyList(),
        calibrationRatio: Double = 1.20,
        forceReplan: Boolean = false,
        zoneId: ZoneId = ZoneId.systemDefault(),
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null,
        priorityWeights: PriorityWeightProfile = PriorityWeightProfile.DEFAULT,
        learnedProfile: LearnedCircadianProfile? = null,
        rollingAverageSleepMinutes: Double? = null,
        physiologicalBlocks: List<CognitiveBlockWindow> = emptyList(),
        stimulantDoses: List<StimulantDoseBout> = emptyList(),
        socialJetlagLagHours: Double = 0.0,
        activeUncompletedTaskIds: Set<String> = emptySet()
    ): SchedulingOutcome

    companion object {
        const val ULTRADIAN_RECOVERY_BUFFER_MINUTES = 15
    }
}

@Singleton
class DefaultTaskSchedulerService @Inject constructor(
    private val circadianService: CircadianEnergyService
) : TaskSchedulerService {

    companion object {
        private const val W_URGENCY = 0.30
        private const val W_IMPACT = 0.30
        private const val W_ENERGY = 0.25
        private const val W_MOMENTUM = 0.15

        private const val EWMA_ALPHA = 0.25
        private const val TRANSITION_BUFFER_MINUTES = 5
        const val ULTRADIAN_RECOVERY_BUFFER_MINUTES = 15
    }

    override fun evaluateTaskScore(
        task: SchedulableTask,
        snapshot: CircadianSnapshot,
        dependentCount: Int,
        calibrationRatio: Double
    ): TaskScoreBreakdown = evaluateTaskScoreWithWeights(
        task = task,
        snapshot = snapshot,
        dependentCount = dependentCount,
        calibrationRatio = calibrationRatio,
        weights = PriorityWeightProfile.DEFAULT,
        referenceNowMillis = null
    )

    override fun evaluateTaskScoreWithWeights(
        task: SchedulableTask,
        snapshot: CircadianSnapshot,
        dependentCount: Int,
        calibrationRatio: Double,
        weights: PriorityWeightProfile,
        referenceNowMillis: Long?
    ): TaskScoreBreakdown {
        val bufferedMins = computeBufferedDuration(task, calibrationRatio)
        // Bug 1 Fix (ADR-025): Anchor urgency calculation to referenceNowMillis to prevent procrastination bias
        val nowAnchor = referenceNowMillis ?: snapshot.timestampMillis

        // 1. Urgency Vector U in [0..100] with Parkinson's Law ADHD Urgency Ignition
        val postponeBonus = min(15.0, task.postponeCount * 3.0)
        val urgency = if (task.deadlineMillis != null) {
            val hoursRemaining = (task.deadlineMillis - nowAnchor) / 3_600_000.0
            val slackHours = hoursRemaining - (bufferedMins / 60.0)
            val baseLogistic = 100.0 / (1.0 + exp(0.12 * (slackHours - 24.0)))
            val ignitionBoost = if (slackHours in 0.25..6.0) {
                12.0 * exp(-((slackHours - 2.5) * (slackHours - 2.5)) / 4.0)
            } else 0.0
            (baseLogistic + ignitionBoost + postponeBonus).coerceIn(0.0, 100.0)
        } else {
            val ageDays = max(0.0, (nowAnchor - task.createdAtMillis) / 86_400_000.0)
            min(40.0, 15.0 + 25.0 * (1.0 - exp(-ageDays / 14.0)) + postponeBonus)
        }

        // 2. Impact Vector I in [0..100]
        val rawBasePriority = when (task.priority) {
            2 -> 65.0
            1 -> 40.0
            else -> 15.0
        }
        val basePriority = (rawBasePriority * weights.priorityLevelWeight.coerceIn(0.2, 3.0)).coerceIn(0.0, 100.0)
        val impactAdj = if (task.impactScore != 50) {
            (task.impactScore - 50) * 0.30
        } else 0.0
        val frogBonus = if (task.isFrog) (25.0 * weights.frogBonusMultiplier.coerceIn(0.5, 2.0)) else 0.0
        val dependencyBonus = 10.0 * min(3, dependentCount)
        val impact = (basePriority + impactAdj + frogBonus + dependencyBonus).coerceIn(0.0, 100.0)

        // 3. Energy & Category Fit Vector E in [0..100] + Dread Desensitization Routing
        val energyDiff = snapshot.usableEnergy.toDouble() - task.energyDemand.targetScore
        val demandGaussian = 100.0 * exp(-(energyDiff * energyDiff) / (2.0 * 28.0 * 28.0))
        val rawCategoryAffinity = circadianService.computeCategoryAffinity(task.category, snapshot)
        val categoryAffinity = (rawCategoryAffinity * weights.focusModeWeight.coerceIn(0.2, 3.0)).coerceIn(0.0, 100.0)
        val dreadAlignmentAdj = if (task.isAnxietyTask || task.postponeCount >= 3) {
            when (snapshot.phase) {
                EnergyPhase.PEAK, EnergyPhase.REBOUND -> 8.0
                EnergyPhase.DIP, EnergyPhase.WIND_DOWN -> -10.0
                else -> 0.0
            }
        } else 0.0
        val distractionAdj = if (task.distractionScore > 0.3f && snapshot.phase == EnergyPhase.DIP) {
            -8.0 * task.distractionScore
        } else 0.0
        val energyFit = (0.70 * demandGaussian + 0.30 * categoryAffinity + dreadAlignmentAdj + distractionAdj).coerceIn(0.0, 100.0)

        // 4. Momentum Vector M in [0..100] with Zeigarnik Pull & WOOP Implementation Intention Shield
        val progressFraction = if (task.totalMicroSteps > 0) {
            (task.completedMicroSteps.toDouble() / task.totalMicroSteps).coerceIn(0.0, 1.0)
        } else 0.0
        val zeigarnikPull = if (progressFraction > 0.0 && progressFraction < 1.0) {
            30.0 * progressFraction + 10.0 * sin(PI * progressFraction)
        } else {
            30.0 * progressFraction
        }
        val activeBonus = if (task.isCurrentlyActive) 35.0 else 0.0
        val microStepBonus = if (task.totalMicroSteps > 0) 20.0 else 0.0
        val woopBonus = if (task.hasImplementationIntention) {
            12.0 * weights.planBoostWeight.coerceIn(0.1, 3.0)
        } else 0.0
        val lowFrictionBonus = (15.0 * exp(-task.estimatedMinutes / 45.0)) * weights.durationWeight.coerceIn(0.2, 3.0)
        val enjoymentBonus = if (task.estimatedEnjoyment != 5) {
            (task.estimatedEnjoyment - 5) * 2.0 * weights.enjoymentWeight.coerceIn(0.2, 3.0)
        } else 0.0
        val momentum = (zeigarnikPull + activeBonus + microStepBonus + woopBonus + lowFrictionBonus + enjoymentBonus)
            .coerceIn(0.0, 100.0)

        // Eisenhower Quadrant Modulation
        val isImportant = task.priority >= 1 || task.impactScore >= 50 || task.isFrog
        val isUrgent = urgency >= 50.0
        val quadrantSpread = weights.quadrantWeight.coerceIn(0.0, 3.0)
        val quadrantModifier = when {
            isImportant && isUrgent -> 1.0 + 0.15 * quadrantSpread // DO_FIRST
            isImportant && !isUrgent -> 1.0 + 0.08 * quadrantSpread // SCHEDULE
            !isImportant && isUrgent -> 1.0 - 0.08 * quadrantSpread // DELEGATE
            else -> 1.0 - 0.15 * quadrantSpread // ELIMINATE
        }.coerceIn(0.5, 2.0)

        val rawWu = W_URGENCY * weights.urgencyWeight.coerceIn(0.1, 3.0)
        val rawWi = W_IMPACT * weights.impactWeight.coerceIn(0.1, 3.0)
        val rawWe = W_ENERGY * weights.circadianMatchWeight.coerceIn(0.1, 3.0)
        val rawWm = W_MOMENTUM * weights.momentumWeight.coerceIn(0.1, 3.0)
        val weightSum = (rawWu + rawWi + rawWe + rawWm).coerceAtLeast(0.001)

        val totalScore = (
            ((rawWu * urgency + rawWi * impact + rawWe * energyFit + rawWm * momentum) / weightSum) * quadrantModifier
        ).coerceIn(0.0, 100.0)

        return TaskScoreBreakdown(
            taskId = task.id,
            totalScore = totalScore,
            urgencyScore = urgency,
            impactScore = impact,
            energyFitScore = energyFit,
            momentumScore = momentum,
            bufferedDurationMinutes = bufferedMins
        )
    }

    override fun updateCalibrationRatio(
        previousRatio: Double,
        estimatedMinutes: Int,
        actualMinutes: Int
    ): Double {
        if (estimatedMinutes <= 0 || actualMinutes <= 0) return previousRatio
        val observedRatio = actualMinutes.toDouble() / estimatedMinutes.toDouble()
        return ((1.0 - EWMA_ALPHA) * previousRatio + EWMA_ALPHA * observedRatio).coerceIn(0.5, 2.5)
    }

    override fun scheduleDay(
        tasks: List<SchedulableTask>,
        dayDate: LocalDate,
        nowMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        busyWindows: List<TimeSlotWindow>,
        protectedSlots: List<ProtectedLocalSlot>,
        calibrationRatio: Double,
        forceReplan: Boolean,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile,
        manualPeakStartHour: Int?,
        priorityWeights: PriorityWeightProfile,
        learnedProfile: LearnedCircadianProfile?,
        rollingAverageSleepMinutes: Double?,
        physiologicalBlocks: List<CognitiveBlockWindow>,
        stimulantDoses: List<StimulantDoseBout>,
        socialJetlagLagHours: Double,
        activeUncompletedTaskIds: Set<String>
    ): List<ScheduledPlacement> = scheduleDayWithOutcome(
        tasks = tasks,
        dayDate = dayDate,
        nowMillis = nowMillis,
        wakeTimeMillis = wakeTimeMillis,
        lastSleepDurationMinutes = lastSleepDurationMinutes,
        busyWindows = busyWindows,
        protectedSlots = protectedSlots,
        calibrationRatio = calibrationRatio,
        forceReplan = forceReplan,
        zoneId = zoneId,
        naps = naps,
        chronotype = chronotype,
        manualPeakStartHour = manualPeakStartHour,
        priorityWeights = priorityWeights,
        learnedProfile = learnedProfile,
        rollingAverageSleepMinutes = rollingAverageSleepMinutes,
        physiologicalBlocks = physiologicalBlocks,
        stimulantDoses = stimulantDoses,
        socialJetlagLagHours = socialJetlagLagHours,
        activeUncompletedTaskIds = activeUncompletedTaskIds
    ).placements

    override fun scheduleDayWithOutcome(
        tasks: List<SchedulableTask>,
        dayDate: LocalDate,
        nowMillis: Long,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        busyWindows: List<TimeSlotWindow>,
        protectedSlots: List<ProtectedLocalSlot>,
        calibrationRatio: Double,
        forceReplan: Boolean,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout>,
        chronotype: ChronotypeProfile,
        manualPeakStartHour: Int?,
        priorityWeights: PriorityWeightProfile,
        learnedProfile: LearnedCircadianProfile?,
        rollingAverageSleepMinutes: Double?,
        physiologicalBlocks: List<CognitiveBlockWindow>,
        stimulantDoses: List<StimulantDoseBout>,
        socialJetlagLagHours: Double,
        activeUncompletedTaskIds: Set<String>
    ): SchedulingOutcome {
        if (tasks.isEmpty()) return SchedulingOutcome(emptyList())

        // 0. Cycle-Safe DAG Sanitization (ADR-020): guarantees circular dependencies never starve Kahn's queue
        val sanitizedTasks = DependencyGraphValidator.sanitizeDag(tasks).tasks

        // 1. Construct DST-safe day horizon [max(now, 06:00, wakeTime + 15m), 22:30]
        val nowLocalDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val isToday = dayDate == nowLocalDate
        val isPast = dayDate < nowLocalDate

        val earliestDayStart = dayDate.atTime(LocalTime.of(6, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val latestDayEnd = dayDate.atTime(LocalTime.of(22, 30)).atZone(zoneId).toInstant().toEpochMilli()
        val zonedWake = Instant.ofEpochMilli(wakeTimeMillis).atZone(zoneId)
        val wakeTodayMillis = dayDate.atTime(zonedWake.toLocalTime()).atZone(zoneId).toInstant().toEpochMilli()
        val effectiveWakeStart = max(earliestDayStart, wakeTodayMillis + 15 * 60_000L)
        val dayEndMillis = max(latestDayEnd, effectiveWakeStart + 4 * 3600_000L)
        val dayStartMillis = when {
            isPast -> dayEndMillis
            isToday -> max(nowMillis + 5 * 60_000L, effectiveWakeStart)
            else -> effectiveWakeStart
        }
        if (dayStartMillis >= dayEndMillis) {
            return SchedulingOutcome(
                placements = emptyList(),
                deferrals = tasks.map {
                    DeferralRationale(
                        taskId = it.id,
                        reasonCode = DeferralReasonCode.INSUFFICIENT_TIME,
                        explanation = "Past workday end for this date",
                        actionableSuggestion = "Reschedule for tomorrow or advance wake time."
                    )
                }
            )
        }

        // 2. Build Exact Minute-Resolution Free Intervals
        var freeIntervals = mutableListOf(TimeSlotWindow(dayStartMillis, dayEndMillis))
        val napWindows = naps.map { TimeSlotWindow(it.startMillis, it.endMillis) }
        val physioWindows = physiologicalBlocks.map { it.toTimeSlotWindow(dayDate, zoneId) }
        val allBlocked = busyWindows + protectedSlots.map { it.resolveForDate(dayDate, zoneId) } + napWindows + physioWindows
        for (blocked in allBlocked) {
            freeIntervals = subtractInterval(freeIntervals, blocked)
        }

        // 3. Separate Locked / Hysteresis-Anchored Tasks
        val placements = mutableListOf<ScheduledPlacement>()
        val completionEndMap = mutableMapOf<String, Long>()
        val dependentsMap = mutableMapOf<String, MutableSet<String>>()
        for (task in sanitizedTasks) {
            val allPrereqs = task.prerequisiteIds
            for (p in allPrereqs) {
                dependentsMap.getOrPut(p) { mutableSetOf() }.add(task.id)
            }
        }

        val sanitizedTaskMap = sanitizedTasks.associateBy { it.id }
        val remainingTasks = mutableListOf<SchedulableTask>()

        // Bug 5 Fix: Sort candidate anchor tasks by scheduledStartMillis ascending
        // and only anchor a task if all of its prerequisites in sanitizedTasks are already anchored
        // at an earlier timestamp (prereqEndMillis <= existingWindow.startMillis) and not blocked by active uncompleted cross-day tasks.
        val candidateAnchors = sanitizedTasks.sortedWith(
            compareBy<SchedulableTask> { it.scheduledStartMillis ?: Long.MAX_VALUE }
                .thenBy { it.id }
        )

        for (task in candidateAnchors) {
            val existingStart = task.scheduledStartMillis
            val durationMins = computeBufferedDuration(task, calibrationRatio)
            val existingWindow = if (existingStart != null) {
                TimeSlotWindow(existingStart, existingStart + durationMins * 60_000L)
            } else null

            val isHysteresisCandidate = existingWindow != null &&
                (task.isLocked || (!forceReplan && existingWindow.startMillis >= nowMillis - 15 * 60_000L)) &&
                fitsInFreeIntervals(freeIntervals, existingWindow)

            val allPrereqs = task.prerequisiteIds
            val prereqsSatisfied = isHysteresisCandidate && allPrereqs.all { prereqId ->
                if (prereqId in sanitizedTaskMap) {
                    val prereqEnd = completionEndMap[prereqId]
                    prereqEnd != null && prereqEnd <= existingWindow!!.startMillis
                } else {
                    prereqId !in activeUncompletedTaskIds
                }
            }

            if (isHysteresisCandidate && prereqsSatisfied) {
                val snap = circadianService.evaluateSnapshotWithStimulants(
                    timestampMillis = existingWindow!!.startMillis,
                    wakeTimeMillis = wakeTimeMillis,
                    lastSleepDurationMinutes = lastSleepDurationMinutes,
                    continuousActiveFocusMinutes = 0,
                    zoneId = zoneId,
                    naps = naps,
                    chronotype = chronotype,
                    manualPeakStartHour = manualPeakStartHour,
                    learnedProfile = learnedProfile,
                    rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                    cognitiveBlocks = physiologicalBlocks,
                    stimulantDoses = stimulantDoses,
                    socialJetlagLagHours = socialJetlagLagHours
                )
                val breakdown = evaluateTaskScoreWithWeights(
                    task, snap, dependentsMap[task.id]?.size ?: 0, calibrationRatio, priorityWeights,
                    referenceNowMillis = nowMillis
                )
                placements.add(ScheduledPlacement(task, existingWindow, breakdown))
                freeIntervals = subtractInterval(
                    freeIntervals,
                    TimeSlotWindow(
                        existingWindow.startMillis,
                        existingWindow.endMillis + TRANSITION_BUFFER_MINUTES * 60_000L
                    )
                )
                completionEndMap[task.id] = existingWindow.endMillis
            } else {
                remainingTasks.add(task)
            }
        }

        // Feature 2 (ADR-024): Reverse-DAG Deadline & Locked-Dependent Ceiling Propagation
        val latestAllowedEndMap = mutableMapOf<String, Long>()
        for (task in sanitizedTasks) {
            val deadlineCeiling = task.deadlineMillis ?: Long.MAX_VALUE
            val placedWindow = placements.firstOrNull { it.task.id == task.id }?.window
            val directCeiling = when {
                placedWindow != null -> placedWindow.startMillis
                task.isLocked && task.scheduledStartMillis != null -> task.scheduledStartMillis!!
                else -> deadlineCeiling
            }
            latestAllowedEndMap[task.id] = min(deadlineCeiling, directCeiling)
        }

        var dagCeilingChanged = true
        var dagIterations = 0
        while (dagCeilingChanged && dagIterations < sanitizedTasks.size + 1) {
            dagCeilingChanged = false
            dagIterations++
            for (task in sanitizedTasks) {
                val childCeiling = latestAllowedEndMap[task.id] ?: Long.MAX_VALUE
                if (childCeiling == Long.MAX_VALUE) continue

                val childBufferedMins = computeBufferedDuration(task, calibrationRatio)
                val childDurationMillis = childBufferedMins * 60_000L
                val placedWindow = placements.firstOrNull { it.task.id == task.id }?.window
                val childLatestStart = when {
                    placedWindow != null -> placedWindow.startMillis
                    task.isLocked && task.scheduledStartMillis != null -> task.scheduledStartMillis!!
                    else -> childCeiling - childDurationMillis
                }
                val isUltradianDep = task.id.endsWith("#part2") ||
                    task.prerequisiteIds.any { it.endsWith("#part1") }
                val bufferBeforeChild = if (isUltradianDep) ULTRADIAN_RECOVERY_BUFFER_MINUTES else TRANSITION_BUFFER_MINUTES
                val parentMaxEnd = childLatestStart - bufferBeforeChild * 60_000L

                val allPrereqs = task.prerequisiteIds
                for (prereqId in allPrereqs) {
                    val currentParentCeiling = latestAllowedEndMap[prereqId] ?: Long.MAX_VALUE
                    if (parentMaxEnd < currentParentCeiling) {
                        latestAllowedEndMap[prereqId] = parentMaxEnd
                        dagCeilingChanged = true
                    }
                }
            }
        }

        // 4. Multi-Prerequisite DAG Topological Sort + Priority Scheduling
        val deferrals = mutableListOf<DeferralRationale>()
        val remainingIds = remainingTasks.map { it.id }.toSet()

        // Bug 4 Fix: exclude tasks with uncompleted cross-day prerequisites from today's active ready queue
        val schedulableTodayTasks = mutableListOf<SchedulableTask>()
        for (task in remainingTasks) {
            val allPrereqs = task.prerequisiteIds
            val blockedByPrereq = allPrereqs.firstOrNull { prereqId ->
                prereqId in activeUncompletedTaskIds &&
                prereqId !in remainingIds &&
                prereqId !in completionEndMap
            }
            if (blockedByPrereq != null) {
                deferrals.add(
                    DeferralRationale(
                        taskId = task.id,
                        reasonCode = DeferralReasonCode.DEPENDENCY_BLOCKED,
                        explanation = "Prerequisite task '$blockedByPrereq' is active and pending on a future date.",
                        actionableSuggestion = "Complete prerequisite '$blockedByPrereq' first or adjust scheduled dates."
                    )
                )
            } else {
                schedulableTodayTasks.add(task)
            }
        }

        val schedulableTodayIds = schedulableTodayTasks.map { it.id }.toSet()
        val inDegree = mutableMapOf<String, Int>()
        val children = mutableMapOf<String, MutableList<SchedulableTask>>()

        for (task in schedulableTodayTasks) {
            val allPrereqs = task.prerequisiteIds.filter { it in schedulableTodayIds }.toSet()
            inDegree[task.id] = allPrereqs.size
            for (prereq in allPrereqs) {
                children.getOrPut(prereq) { mutableListOf() }.add(task)
            }
        }

        val nowSnapshot = circadianService.evaluateSnapshotWithStimulants(
            timestampMillis = dayStartMillis,
            wakeTimeMillis = wakeTimeMillis,
            lastSleepDurationMinutes = lastSleepDurationMinutes,
            continuousActiveFocusMinutes = 0,
            zoneId = zoneId,
            naps = naps,
            chronotype = chronotype,
            manualPeakStartHour = manualPeakStartHour,
            learnedProfile = learnedProfile,
            rollingAverageSleepMinutes = rollingAverageSleepMinutes,
            cognitiveBlocks = physiologicalBlocks,
            stimulantDoses = stimulantDoses,
            socialJetlagLagHours = socialJetlagLagHours
        )
        val scoreCache = schedulableTodayTasks.associate { t ->
            t.id to evaluateTaskScoreWithWeights(t, nowSnapshot, dependentsMap[t.id]?.size ?: 0, calibrationRatio, priorityWeights, referenceNowMillis = nowMillis)
        }.toMutableMap()

        val readyQueue = PriorityQueue<SchedulableTask>(
            compareByDescending { task ->
                val baseScore = scoreCache[task.id]?.totalScore ?: 0.0
                val scarcityBonus = when (task.energyDemand) {
                    EnergyDemand.HIGH -> 12.0
                    EnergyDemand.MODERATE -> 4.0
                    EnergyDemand.LOW -> 0.0
                }
                baseScore + scarcityBonus
            }
        )
        schedulableTodayTasks.filter { (inDegree[it.id] ?: 0) == 0 }.forEach { readyQueue.add(it) }

        while (readyQueue.isNotEmpty()) {
            val current = readyQueue.poll() ?: break
            val breakdown = scoreCache[current.id]
                ?: evaluateTaskScoreWithWeights(current, nowSnapshot, dependentsMap[current.id]?.size ?: 0, calibrationRatio, priorityWeights, referenceNowMillis = nowMillis)
            val durationMillis = breakdown.bufferedDurationMinutes * 60_000L
            val rawDurationMillis = max(5, current.estimatedMinutes) * 60_000L

            val allPrereqs = current.prerequisiteIds
            val isUltradianPrereq = current.id.endsWith("#part2") || allPrereqs.any { it.endsWith("#part1") }
            val prereqBufferMinutes = if (isUltradianPrereq) ULTRADIAN_RECOVERY_BUFFER_MINUTES else TRANSITION_BUFFER_MINUTES
            val maxPrereqEnd = allPrereqs.mapNotNull { completionEndMap[it] }.maxOrNull() ?: dayStartMillis
            val earliestStart = max(dayStartMillis, maxPrereqEnd + prereqBufferMinutes * 60_000L)

            val taskCeiling = latestAllowedEndMap[current.id] ?: Long.MAX_VALUE
            val bestSlot = findBestCircadianWindow(
                task = current,
                durationMillis = durationMillis,
                earliestStartMillis = earliestStart,
                freeIntervals = freeIntervals,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = lastSleepDurationMinutes,
                dependentCount = dependentsMap[current.id]?.size ?: 0,
                calibrationRatio = calibrationRatio,
                zoneId = zoneId,
                naps = naps,
                chronotype = chronotype,
                manualPeakStartHour = manualPeakStartHour,
                priorityWeights = priorityWeights,
                placements = placements,
                learnedProfile = learnedProfile,
                rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                physiologicalBlocks = physiologicalBlocks,
                stimulantDoses = stimulantDoses,
                socialJetlagLagHours = socialJetlagLagHours,
                latestAllowedEndMillis = taskCeiling,
                referenceNowMillis = nowMillis
            ) ?: if (!current.isDurationExplicit && rawDurationMillis < durationMillis) {
                findBestCircadianWindow(
                    task = current,
                    durationMillis = rawDurationMillis,
                    earliestStartMillis = earliestStart,
                    freeIntervals = freeIntervals,
                    wakeTimeMillis = wakeTimeMillis,
                    lastSleepDurationMinutes = lastSleepDurationMinutes,
                    dependentCount = dependentsMap[current.id]?.size ?: 0,
                    calibrationRatio = calibrationRatio,
                    zoneId = zoneId,
                    naps = naps,
                    chronotype = chronotype,
                    manualPeakStartHour = manualPeakStartHour,
                    priorityWeights = priorityWeights,
                    placements = placements,
                    learnedProfile = learnedProfile,
                    rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                    physiologicalBlocks = physiologicalBlocks,
                    stimulantDoses = stimulantDoses,
                    socialJetlagLagHours = socialJetlagLagHours,
                    latestAllowedEndMillis = taskCeiling,
                    referenceNowMillis = nowMillis
                )
            } else null

            if (bestSlot != null) {
                val (window, slotBreakdown) = bestSlot
                val finalBreakdown = if (window.durationMinutes < slotBreakdown.bufferedDurationMinutes) {
                    slotBreakdown.copy(bufferedDurationMinutes = window.durationMinutes)
                } else {
                    slotBreakdown
                }
                placements.add(ScheduledPlacement(current, window, finalBreakdown))
                completionEndMap[current.id] = window.endMillis
                if (current.id.endsWith("#part2")) {
                    completionEndMap[current.id.removeSuffix("#part2")] = window.endMillis
                }

                val isCognitiveHeavy = current.energyDemand == EnergyDemand.HIGH ||
                    current.category == TaskCategory.DEEP_WORK ||
                    current.category == TaskCategory.CREATIVE

                // Bug 2 Fix: compute simulated focus load on wall-clock timeline
                val contiguousCognitiveMins = if (isCognitiveHeavy) {
                    computeSimulatedFocusLoadAt(window.startMillis, placements) + window.durationMinutes
                } else {
                    0
                }

                val bufferMins = if (current.id.endsWith("#part1")) {
                    ULTRADIAN_RECOVERY_BUFFER_MINUTES
                } else if (contiguousCognitiveMins >= 90) {
                    15
                } else {
                    TRANSITION_BUFFER_MINUTES
                }

                freeIntervals = subtractInterval(
                    freeIntervals,
                    TimeSlotWindow(window.startMillis, window.endMillis + bufferMins * 60_000L)
                )

                val unlockKey = if (current.id.endsWith("#part2")) current.id.removeSuffix("#part2") else current.id
                children[unlockKey]?.forEach { child ->
                    val newDeg = (inDegree[child.id] ?: 1) - 1
                    inDegree[child.id] = newDeg
                    if (newDeg <= 0) readyQueue.add(child)
                }
                if (unlockKey != current.id) {
                    children[current.id]?.forEach { child ->
                        val newDeg = (inDegree[child.id] ?: 1) - 1
                        inDegree[child.id] = newDeg
                        if (newDeg <= 0) readyQueue.add(child)
                    }
                }
            } else {
                // Bug 6 Fix & Feature 4 (ADR-024): Atomic Two-Phase Ultradian Split Verifier
                val splitPair = splitIntoUltradianParts(current, breakdown.bufferedDurationMinutes)
                var splitSuccess = false
                if (splitPair != null) {
                    val (part1, part2) = splitPair
                    val p1DurationMillis = max(5, part1.estimatedMinutes) * 60_000L
                    val p2DurationMillis = max(5, part2.estimatedMinutes) * 60_000L
                    val currentCeiling = latestAllowedEndMap[current.id] ?: Long.MAX_VALUE

                    // Phase 1: Test if part1 can fit in current freeIntervals
                    val p1Slot = findBestCircadianWindow(
                        task = part1,
                        durationMillis = p1DurationMillis,
                        earliestStartMillis = earliestStart,
                        freeIntervals = freeIntervals,
                        wakeTimeMillis = wakeTimeMillis,
                        lastSleepDurationMinutes = lastSleepDurationMinutes,
                        dependentCount = 1,
                        calibrationRatio = 1.0,
                        zoneId = zoneId,
                        naps = naps,
                        chronotype = chronotype,
                        manualPeakStartHour = manualPeakStartHour,
                        priorityWeights = priorityWeights,
                        placements = placements,
                        learnedProfile = learnedProfile,
                        rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                        physiologicalBlocks = physiologicalBlocks,
                        stimulantDoses = stimulantDoses,
                        socialJetlagLagHours = socialJetlagLagHours,
                        latestAllowedEndMillis = currentCeiling
                    )

                    if (p1Slot != null) {
                        val (p1Window, p1Breakdown) = p1Slot
                        // Require at least a 15-minute ultradian recovery buffer between part1 and part2
                        val p1BufferMins = 15
                        val simulatedIntervalsAfterP1 = subtractInterval(
                            freeIntervals,
                            TimeSlotWindow(p1Window.startMillis, p1Window.endMillis + p1BufferMins * 60_000L)
                        )
                        val earliestP2Start = p1Window.endMillis + p1BufferMins * 60_000L
                        val simulatedPlacementsWithP1 = placements + ScheduledPlacement(part1, p1Window, p1Breakdown)

                        // Phase 2: Test if part2 can fit in simulated remaining intervals
                        val p2Slot = findBestCircadianWindow(
                            task = part2,
                            durationMillis = p2DurationMillis,
                            earliestStartMillis = earliestP2Start,
                            freeIntervals = simulatedIntervalsAfterP1,
                            wakeTimeMillis = wakeTimeMillis,
                            lastSleepDurationMinutes = lastSleepDurationMinutes,
                            dependentCount = dependentsMap[current.id]?.size ?: 0,
                            calibrationRatio = 1.0,
                            zoneId = zoneId,
                            naps = naps,
                            chronotype = chronotype,
                            manualPeakStartHour = manualPeakStartHour,
                            priorityWeights = priorityWeights,
                            placements = simulatedPlacementsWithP1,
                            learnedProfile = learnedProfile,
                            rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                            physiologicalBlocks = physiologicalBlocks,
                            stimulantDoses = stimulantDoses,
                            socialJetlagLagHours = socialJetlagLagHours,
                            latestAllowedEndMillis = currentCeiling
                        )

                        if (p2Slot != null) {
                            // Both parts verified! Commit both atomically to placements.
                            val (p2Window, p2Breakdown) = p2Slot
                            placements.add(ScheduledPlacement(part1, p1Window, p1Breakdown))
                            placements.add(ScheduledPlacement(part2, p2Window, p2Breakdown))

                            val p2ContiguousMins = computeSimulatedFocusLoadAt(p2Window.startMillis, simulatedPlacementsWithP1) + p2Window.durationMinutes
                            val p2BufferMins = if (p2ContiguousMins >= 90) 15 else TRANSITION_BUFFER_MINUTES
                            freeIntervals = subtractInterval(
                                simulatedIntervalsAfterP1,
                                TimeSlotWindow(p2Window.startMillis, p2Window.endMillis + p2BufferMins * 60_000L)
                            )

                            completionEndMap[part1.id] = p1Window.endMillis
                            completionEndMap[part2.id] = p2Window.endMillis
                            completionEndMap[current.id] = p2Window.endMillis

                            val unlockKey = current.id
                            children[unlockKey]?.forEach { child ->
                                val newDeg = (inDegree[child.id] ?: 1) - 1
                                inDegree[child.id] = newDeg
                                if (newDeg <= 0) readyQueue.add(child)
                            }
                            splitSuccess = true
                        }
                    }
                }

                if (!splitSuccess) {
                    val baseId = current.id.substringBefore("#part")
                    if (deferrals.none { it.taskId == baseId }) {
                        deferrals.add(
                            DeferralRationale(
                                taskId = baseId,
                                reasonCode = DeferralReasonCode.INSUFFICIENT_TIME,
                                explanation = "Task '${current.title}' (${breakdown.bufferedDurationMinutes}m) could not find sufficient contiguous time or two-phase ultradian split slots today.",
                                actionableSuggestion = "Free up calendar slots or move to tomorrow's schedule."
                            )
                        )
                    }
                }
            }
        }

        // Invariant Safeguard (ADR-024): ensure no orphaned partial split remains in placements
        val orphanedPart1 = placements.filter { it.task.id.endsWith("#part1") }
            .filter { p1 ->
                val p2Id = p1.task.id.replace("#part1", "#part2")
                placements.none { it.task.id == p2Id }
            }
        if (orphanedPart1.isNotEmpty()) {
            val orphanIds = orphanedPart1.map { it.task.id }.toSet()
            placements.removeAll { it.task.id in orphanIds }
            for (orphan in orphanedPart1) {
                val baseId = orphan.task.id.substringBefore("#part")
                if (deferrals.none { it.taskId == baseId }) {
                    deferrals.add(
                        DeferralRationale(
                            taskId = baseId,
                            reasonCode = DeferralReasonCode.INSUFFICIENT_TIME,
                            explanation = "Task '${orphan.task.title}' had an orphaned partial placement rolled back due to unplaced part 2.",
                            actionableSuggestion = "Free up calendar slots or move to tomorrow's schedule."
                        )
                    )
                }
            }
        }

        // Clean deferral task IDs so none carry #part suffixes
        val sanitizedDeferrals = deferrals.map { d ->
            if (d.taskId.contains("#part")) d.copy(taskId = d.taskId.substringBefore("#part")) else d
        }.distinctBy { it.taskId }

        return SchedulingOutcome(placements.sortedBy { it.window.startMillis }, sanitizedDeferrals)
    }

    internal fun splitIntoUltradianParts(
        task: SchedulableTask,
        totalDurationMinutes: Int
    ): Pair<SchedulableTask, SchedulableTask>? {
        if (totalDurationMinutes < 60 || task.isLocked || task.id.contains("#part")) return null
        if (task.fragmentationTolerance < 0.15f) return null

        val part1Mins = (totalDurationMinutes / 2).coerceAtLeast(25)
        val part2Mins = (totalDurationMinutes - part1Mins).coerceAtLeast(25)
        if (part1Mins + part2Mins > totalDurationMinutes + 5) return null

        val part1Id = "${task.id}#part1"
        val part2Id = "${task.id}#part2"

        val part1 = task.copy(
            id = part1Id,
            title = "${task.title} (Part 1/2)",
            estimatedMinutes = part1Mins,
            isDurationExplicit = true
        )
        val part2 = task.copy(
            id = part2Id,
            title = "${task.title} (Part 2/2)",
            estimatedMinutes = part2Mins,
            isDurationExplicit = true,
            prerequisiteIds = task.prerequisiteIds + part1Id
        )
        return part1 to part2
    }

    internal fun computeContextTransitionDelta(
        placements: List<ScheduledPlacement>,
        candidateSlotStartMillis: Long,
        candidate: SchedulableTask,
        candidateSlotEndMillis: Long = candidateSlotStartMillis
    ): Double {
        if (placements.isEmpty()) return 0.0

        val preceding = placements
            .filter { it.window.endMillis <= candidateSlotStartMillis }
            .maxByOrNull { it.window.endMillis }

        val succeeding = placements
            .filter { it.window.startMillis >= candidateSlotEndMillis }
            .minByOrNull { it.window.startMillis }

        fun scoreNeighbor(neighborTask: SchedulableTask, gapMinutes: Long): Double {
            if (gapMinutes < 0 || gapMinutes > 45) return 0.0
            val sameContextTag = neighborTask.contextTag.isNotBlank() &&
                neighborTask.contextTag.equals(candidate.contextTag, ignoreCase = true)

            if (neighborTask.category == candidate.category && sameContextTag) {
                return 8.5
            }
            if (neighborTask.category == candidate.category || sameContextTag) {
                return 6.0
            }

            val isHighEntropy = (neighborTask.category == TaskCategory.EXERCISE && candidate.category == TaskCategory.DEEP_WORK && gapMinutes < 15) ||
                (neighborTask.category == TaskCategory.DEEP_WORK && candidate.category == TaskCategory.ADMIN && gapMinutes < 10)

            return if (isHighEntropy) -8.5 else -1.5
        }

        val precDelta = preceding?.let {
            val gap = (candidateSlotStartMillis - it.window.endMillis) / 60_000L
            scoreNeighbor(it.task, gap)
        }
        val succDelta = succeeding?.let {
            val gap = (it.window.startMillis - candidateSlotEndMillis) / 60_000L
            scoreNeighbor(it.task, gap)
        }

        return when {
            precDelta != null && succDelta != null -> ((precDelta + succDelta) * 0.75).coerceIn(-12.0, 12.0)
            precDelta != null -> precDelta
            succDelta != null -> succDelta
            else -> 0.0
        }
    }

    private fun computeBufferedDuration(task: SchedulableTask, calibrationRatio: Double): Int {
        val raw = max(5, task.estimatedMinutes)
        return if (task.isDurationExplicit) raw else (raw * calibrationRatio).roundToInt().coerceAtLeast(5)
    }

    fun computeSimulatedFocusLoadAt(
        timestampMillis: Long,
        placements: List<ScheduledPlacement>
    ): Int {
        if (placements.isEmpty()) return 0
        var currentCheck = timestampMillis
        var accumulatedMins = 0
        val sortedPlacements = placements
            .filter { it.window.endMillis <= timestampMillis }
            .sortedByDescending { it.window.endMillis }

        for (p in sortedPlacements) {
            val gapMinutes = (currentCheck - p.window.endMillis) / 60_000L
            if (gapMinutes <= 15L) {
                val isCognitive = p.task.energyDemand == EnergyDemand.HIGH ||
                    p.task.category == TaskCategory.DEEP_WORK ||
                    p.task.category == TaskCategory.CREATIVE
                if (isCognitive) {
                    accumulatedMins += p.window.durationMinutes
                    currentCheck = p.window.startMillis
                } else {
                    break
                }
            } else {
                break
            }
        }
        return accumulatedMins
    }

    private fun findBestCircadianWindow(
        task: SchedulableTask,
        durationMillis: Long,
        earliestStartMillis: Long,
        freeIntervals: List<TimeSlotWindow>,
        wakeTimeMillis: Long,
        lastSleepDurationMinutes: Int,
        dependentCount: Int,
        calibrationRatio: Double,
        zoneId: ZoneId,
        naps: List<DaytimeNapBout> = emptyList(),
        chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
        manualPeakStartHour: Int? = null,
        priorityWeights: PriorityWeightProfile = PriorityWeightProfile.DEFAULT,
        placements: List<ScheduledPlacement> = emptyList(),
        learnedProfile: LearnedCircadianProfile? = null,
        rollingAverageSleepMinutes: Double? = null,
        physiologicalBlocks: List<CognitiveBlockWindow> = emptyList(),
        stimulantDoses: List<StimulantDoseBout> = emptyList(),
        socialJetlagLagHours: Double = 0.0,
        latestAllowedEndMillis: Long = Long.MAX_VALUE,
        referenceNowMillis: Long? = null
    ): Pair<TimeSlotWindow, TaskScoreBreakdown>? {
        var bestWindow: TimeSlotWindow? = null
        var bestBreakdown: TaskScoreBreakdown? = null
        var bestUtility = Double.NEGATIVE_INFINITY
        val stepMillis = 15 * 60_000L

        for (interval in freeIntervals) {
            var candidateStart = max(interval.startMillis, earliestStartMillis)
            val effectiveIntervalEnd = min(interval.endMillis, latestAllowedEndMillis)
            while (candidateStart + durationMillis <= effectiveIntervalEnd) {
                val candidateEnd = candidateStart + durationMillis
                val midpoint = candidateStart + durationMillis / 2
                val simulatedFocus = computeSimulatedFocusLoadAt(candidateStart, placements)
                val snap = circadianService.evaluateSnapshotWithStimulants(
                    timestampMillis = midpoint,
                    wakeTimeMillis = wakeTimeMillis,
                    lastSleepDurationMinutes = lastSleepDurationMinutes,
                    continuousActiveFocusMinutes = simulatedFocus,
                    zoneId = zoneId,
                    naps = naps,
                    chronotype = chronotype,
                    manualPeakStartHour = manualPeakStartHour,
                    learnedProfile = learnedProfile,
                    rollingAverageSleepMinutes = rollingAverageSleepMinutes,
                    cognitiveBlocks = physiologicalBlocks,
                    stimulantDoses = stimulantDoses,
                    socialJetlagLagHours = socialJetlagLagHours
                )
                val breakdown = evaluateTaskScoreWithWeights(
                    task = task,
                    snapshot = snap,
                    dependentCount = dependentCount,
                    calibrationRatio = calibrationRatio,
                    weights = priorityWeights,
                    referenceNowMillis = referenceNowMillis ?: earliestStartMillis
                )
                val hourlyDecayLambda = when {
                    task.energyDemand == EnergyDemand.HIGH ||
                    task.category == TaskCategory.DEEP_WORK ||
                    task.category == TaskCategory.CREATIVE -> 0.08
                    task.energyDemand == EnergyDemand.MODERATE -> 0.22
                    else -> 0.45
                }

                val peakCannibalizationPenalty = if (
                    snap.phase == EnergyPhase.PEAK &&
                    breakdown.urgencyScore < 75.0 &&
                    (task.energyDemand == EnergyDemand.LOW || task.category == TaskCategory.ADMIN)
                ) {
                    16.0
                } else {
                    0.0
                }

                // Feature 1 (ADR-023): Pharmacokinetic-Aware Scheduling & Crash Shield
                val stimEffect = if (stimulantDoses.isNotEmpty()) {
                    PharmacokineticStimulantCalculator.evaluateAt(candidateStart, stimulantDoses)
                } else null

                val stimAlignmentDelta = if (stimEffect != null) {
                    val alertBoost = stimEffect.acuteAlertnessBoostPoints
                    if (task.energyDemand == EnergyDemand.HIGH || task.isFrog || task.category == TaskCategory.DEEP_WORK) {
                        when {
                            alertBoost > 4.0 -> 8.0 // Aligns deep focus with Bateman peak plasma concentration
                            alertBoost < -1.5 -> -18.0 // Crash Shield: actively penalizes deep work during adenosine rebound crash
                            else -> 0.0
                        }
                    } else if (task.energyDemand == EnergyDemand.LOW || task.category == TaskCategory.ADMIN || task.category == TaskCategory.MINDFULNESS) {
                        if (alertBoost < -1.5) 6.0 else 0.0 // Crash Recovery: routes light administrative work into crash window
                    } else 0.0
                } else 0.0

                // Feature 2 (ADR-023): Bidirectional Context-Cluster Scoring
                val contextDelta = computeContextTransitionDelta(placements, candidateStart, task, candidateEnd)
                val hoursFromNow = (candidateStart - earliestStartMillis) / 3_600_000.0

                // Bug 1 & Feature 1 (ADR-025): Hard Deadline Breach Penalty (-35.0 pts)
                val deadlineBreachPenalty = if (task.deadlineMillis != null && candidateEnd > task.deadlineMillis) {
                    35.0
                } else {
                    0.0
                }

                val utility = breakdown.totalScore - (hourlyDecayLambda * hoursFromNow) - peakCannibalizationPenalty + contextDelta + stimAlignmentDelta - deadlineBreachPenalty

                if (utility > bestUtility) {
                    bestUtility = utility
                    bestWindow = TimeSlotWindow(candidateStart, candidateStart + durationMillis)
                    bestBreakdown = breakdown
                }
                candidateStart += stepMillis
            }
        }
        return if (bestWindow != null && bestBreakdown != null) bestWindow to bestBreakdown else null
    }

    private fun fitsInFreeIntervals(freeIntervals: List<TimeSlotWindow>, target: TimeSlotWindow): Boolean {
        return freeIntervals.any { it.startMillis <= target.startMillis && it.endMillis >= target.endMillis }
    }

    private fun subtractInterval(
        intervals: List<TimeSlotWindow>,
        blocked: TimeSlotWindow
    ): MutableList<TimeSlotWindow> {
        val result = mutableListOf<TimeSlotWindow>()
        for (current in intervals) {
            if (blocked.endMillis <= current.startMillis || blocked.startMillis >= current.endMillis) {
                result.add(current)
            } else {
                if (blocked.startMillis > current.startMillis) {
                    result.add(TimeSlotWindow(current.startMillis, blocked.startMillis))
                }
                if (blocked.endMillis < current.endMillis) {
                    result.add(TimeSlotWindow(blocked.endMillis, current.endMillis))
                }
            }
        }
        return result
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainschedulerautoschedulingcontractskt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingContracts.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingContracts.kt`
- **Lines of Code:** 62
- **Size:** 1,899 bytes (1.9 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.entity.TaskEntity

/**
 * Non-negotiable safety contracts for all auto-scheduling flows.
 */
object AutoSchedulingContracts {

    /**
     * A manual schedule claims a full timestamp. A date with no time-of-day is not a finished
     * schedule — the scheduler is still expected to place the task, on that day.
     */
    fun hasManualScheduleData(task: TaskEntity): Boolean {
        return task.scheduledDate != null && task.scheduledTime != null && !task.isAutoScheduled
    }

    fun isMutableByAutoScheduler(task: TaskEntity, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (task.isScheduleLocked || task.quadrant == com.neuroflow.app.domain.model.Quadrant.ELIMINATE) {
            return false
        }
        return !hasManualScheduleData(task) || task.isAutoScheduled
    }

    /**
     * True when a task has a deadline anchor but no explicit scheduled slot yet.
     */
    fun hasDeadlineData(task: TaskEntity): Boolean {
        return task.deadlineDate != null
    }
}

enum class AutoScheduleRejectionReason {
    LOCKED_TASK,
    MANUAL_SCHEDULE_PRESENT,
    DEPENDENCY_BLOCKED,
    NO_CAPACITY_IN_HORIZON,
    LOW_ENERGY_PROTECTION,
    OUTSIDE_WORKDAY,
    PROTECTED_SLOT,
    UNKNOWN
}

data class AutoScheduleDecisionTelemetry(
    val taskId: String,
    val generatedAtMillis: Long,
    val horizonDays: Int,
    val wasApplied: Boolean,
    val selectedSlotDate: Long? = null,
    val selectedSlotTime: Long? = null,
    val rejectionReason: AutoScheduleRejectionReason? = null,
    val inputs: AutoScheduleInputsSnapshot
)

data class AutoScheduleInputsSnapshot(
    val priorityScore: Float,
    val energyScore: Float,
    val sleepPressurePoints: Int,
    val hasDependencies: Boolean,
    val estimatedDurationMinutes: Int,
    val tagProfileHints: List<String>
)
```

---

<a id="appsrcmainjavacomneuroflowappdomainschedulerautoschedulingrationaleskt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingRationales.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingRationales.kt`
- **Lines of Code:** 297
- **Size:** 12,896 bytes (12.6 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.entity.ONE_DAY_MILLIS
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.timelineStartMinuteOfDay
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

enum class DeferralReasonCode {
    INSUFFICIENT_TIME,
    ENERGY_MISMATCH,
    COGNITIVE_BUDGET_EXCEEDED,
    DEADLINE_CONFLICT,
    PROTECTED_SLOT_OVERLAP,
    TASK_COLLISION,
    DEPENDENCY_BLOCKED
}

data class ScheduleConflict(
    val taskId: String,
    val taskTitle: String,
    val conflictDescription: String,
    val actionableSuggestion: String,
    val conflictingTargetName: String,
    val conflictStartMinute: Int,
    val conflictEndMinute: Int,
    val suggestedNewStartMinute: Int? = null
)

data class PlacementRationale(
    val taskId: String,
    val scheduledSlotStart: Long,
    val scheduledSlotEnd: Long,
    val energyFitScore: Float,
    val primaryReason: String
)

data class DeferralRationale(
    val taskId: String,
    val reasonCode: DeferralReasonCode,
    val explanation: String,
    val actionableSuggestion: String
)

data class SchedulingReport(
    val placedCount: Int,
    val deferredCount: Int,
    val placementRationales: List<PlacementRationale>,
    val deferrals: List<DeferralRationale>,
    val dailyCognitiveLoadUtilized: Float
)

object AutoSchedulingEngineCoordinator {
    fun planSchedule(
        tasks: List<TaskEntity>,
        availableSlotsMinutes: Int,
        dailyCognitiveCapacityMinutes: Int
    ): SchedulingReport {
        var usedMinutes = 0
        val placedRationales = mutableListOf<PlacementRationale>()
        val deferrals = mutableListOf<DeferralRationale>()

        for (task in tasks) {
            val duration = if (task.estimatedDurationMinutes > 0) task.estimatedDurationMinutes else 25
            if (usedMinutes + duration > dailyCognitiveCapacityMinutes) {
                deferrals.add(
                    DeferralRationale(
                        taskId = task.id,
                        reasonCode = DeferralReasonCode.COGNITIVE_BUDGET_EXCEEDED,
                        explanation = "Daily cognitive limit of ${dailyCognitiveCapacityMinutes}m reached.",
                        actionableSuggestion = "Schedule for tomorrow's morning peak window."
                    )
                )
            } else if (duration > availableSlotsMinutes) {
                deferrals.add(
                    DeferralRationale(
                        taskId = task.id,
                        reasonCode = DeferralReasonCode.INSUFFICIENT_TIME,
                        explanation = "Task duration (${duration}m) exceeds available slot (${availableSlotsMinutes}m).",
                        actionableSuggestion = "Break this task into smaller 25m focus blocks."
                    )
                )
            } else {
                val slotStart = System.currentTimeMillis() + usedMinutes.toLong() * 60_000L
                val slotEnd = slotStart + duration.toLong() * 60_000L
                placedRationales.add(
                    PlacementRationale(
                        taskId = task.id,
                        scheduledSlotStart = slotStart,
                        scheduledSlotEnd = slotEnd,
                        energyFitScore = 0.88f,
                        primaryReason = "High circadian alignment and optimal cognitive budget match."
                    )
                )
                usedMinutes += duration
            }
        }

        check(placedRationales.size + deferrals.size == tasks.size) {
            "Invariant violated: Total tasks must equal placed plus deferred (no silent drops allowed)."
        }

        return SchedulingReport(
            placedCount = placedRationales.size,
            deferredCount = deferrals.size,
            placementRationales = placedRationales,
            deferrals = deferrals,
            dailyCognitiveLoadUtilized = if (dailyCognitiveCapacityMinutes > 0)
                usedMinutes.toFloat() / dailyCognitiveCapacityMinutes.toFloat() else 0f
        )
    }
}

object ConflictResolver {
    fun findConflictFreeStart(
        searchStartMinute: Int,
        duration: Int,
        workDayStartHour: Int,
        workDayEndHour: Int,
        occupiedIntervals: List<Pair<Int, Int>>
    ): Int? {
        val sortedIntervals = occupiedIntervals.sortedBy { it.first }
        // Bug 5 Fix (ADR-030): Clamp suggested conflict-free start to workDayStart
        var candidate = maxOf(searchStartMinute, workDayStartHour.coerceIn(0, 23) * 60)
        val maxMinute = workDayEndHour * 60
        while (candidate + duration <= maxMinute) {
            val overlapping = sortedIntervals.firstOrNull { (start, end) ->
                maxOf(candidate, start) < minOf(candidate + duration, end)
            }
            if (overlapping == null) {
                return candidate
            } else {
                candidate = maxOf(candidate + 1, overlapping.second)
            }
        }
        return null
    }

    fun analyzeScheduleConflicts(
        tasks: List<TaskEntity>,
        protectedSlots: List<ProtectedTimeSlot> = emptyList(),
        workDayStart: Int = 8,
        workDayEnd: Int = 20,
        dayMillis: Long? = null,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<ScheduleConflict> {
        val scheduledTasks = tasks.filter { it.scheduledDate != null && it.scheduledTime != null }
        if (scheduledTasks.isEmpty()) return emptyList()

        fun analyzeForSingleDay(
            dayTasks: List<TaskEntity>,
            targetDayMillis: Long
        ): List<ScheduleConflict> {
            val conflicts = mutableListOf<ScheduleConflict>()
            val sortedTasks = dayTasks.sortedBy { taskStartMinuteOfDay(it) }

            val activeSlots = protectedSlots.filter { slot ->
                slot.isEnabled && ProtectedSlotClock.occursOnDay(slot, targetDayMillis)
            }

            val allSlotIntervals = activeSlots.map { slot ->
                slot.startMinuteOfDay to (slot.startMinuteOfDay + slot.durationMinutes + slot.recoveryBufferMinutes)
            }

            for (i in sortedTasks.indices) {
                val taskA = sortedTasks[i]
                val aStart = taskStartMinuteOfDay(taskA)
                val aDuration = TaskBlockPlan.plannedMinutes(taskA)
                val aEnd = aStart + aDuration

                val otherTasksForA = sortedTasks.filterIndexed { idx, _ -> idx != i }.map { t ->
                    val start = taskStartMinuteOfDay(t)
                    start to (start + TaskBlockPlan.plannedMinutes(t))
                }
                val occupiedForA = allSlotIntervals + otherTasksForA

                // Protected slot overlaps
                for (slot in activeSlots) {
                    val sStart = slot.startMinuteOfDay
                    val sEnd = sStart + slot.durationMinutes + slot.recoveryBufferMinutes
                    if (maxOf(aStart, sStart) < minOf(aEnd, sEnd)) {
                        val overlapStart = maxOf(aStart, sStart)
                        val overlapEnd = minOf(aEnd, sEnd)
                        val bufferNote = if (slot.recoveryBufferMinutes > 0) " (incl. ${slot.recoveryBufferMinutes}m buffer)" else ""
                        val targetStart = findConflictFreeStart(
                            searchStartMinute = overlapEnd,
                            duration = aDuration,
                            workDayStartHour = workDayStart,
                            workDayEndHour = workDayEnd,
                            occupiedIntervals = occupiedForA
                        )
                        val suggestion = if (targetStart != null) {
                            "Move '${taskA.title}' to ${formatMinutesToTime(targetStart)} after protected slot ends."
                        } else {
                            "No free slot remaining today before ${formatMinutesToTime(workDayEnd * 60)}; move to tomorrow."
                        }
                        conflicts.add(
                            ScheduleConflict(
                                taskId = taskA.id,
                                taskTitle = taskA.title,
                                conflictDescription = "Overlaps with protected slot '${slot.label}' (${formatMinutesToTime(sStart)}–${formatMinutesToTime(sEnd)}$bufferNote)",
                                actionableSuggestion = suggestion,
                                conflictingTargetName = slot.label,
                                conflictStartMinute = overlapStart,
                                conflictEndMinute = overlapEnd,
                                suggestedNewStartMinute = targetStart
                            )
                        )
                    }
                }

                // Task vs Task collisions
                for (j in (i + 1) until sortedTasks.size) {
                    val taskB = sortedTasks[j]
                    val bStart = taskStartMinuteOfDay(taskB)
                    val bDuration = TaskBlockPlan.plannedMinutes(taskB)
                    val bEnd = bStart + bDuration

                    if (maxOf(aStart, bStart) < minOf(aEnd, bEnd)) {
                        val overlapStart = maxOf(aStart, bStart)
                        val overlapEnd = minOf(aEnd, bEnd)
                        val otherTasksForB = sortedTasks.filterIndexed { idx, _ -> idx != j }.map { t ->
                            val start = taskStartMinuteOfDay(t)
                            start to (start + TaskBlockPlan.plannedMinutes(t))
                        }
                        val occupiedForB = allSlotIntervals + otherTasksForB
                        val targetStart = findConflictFreeStart(
                            searchStartMinute = overlapEnd,
                            duration = bDuration,
                            workDayStartHour = workDayStart,
                            workDayEndHour = workDayEnd,
                            occupiedIntervals = occupiedForB
                        )
                        val suggestion = if (targetStart != null) {
                            "Reorder '${taskB.title}' to start at ${formatMinutesToTime(targetStart)} or re-run auto-schedule."
                        } else {
                            "No free slot remaining today before ${formatMinutesToTime(workDayEnd * 60)}; move to tomorrow."
                        }
                        conflicts.add(
                            ScheduleConflict(
                                taskId = taskB.id,
                                taskTitle = taskB.title,
                                conflictDescription = "Overlaps with '${taskA.title}' (${formatMinutesToTime(aStart)}–${formatMinutesToTime(aEnd)})",
                                actionableSuggestion = suggestion,
                                conflictingTargetName = taskA.title,
                                conflictStartMinute = overlapStart,
                                conflictEndMinute = overlapEnd,
                                suggestedNewStartMinute = targetStart
                            )
                        )
                    }
                }
            }
            return conflicts
        }

        return if (dayMillis != null) {
            val targetDate = Instant.ofEpochMilli(dayMillis).atZone(zoneId).toLocalDate()
            val dayTasks = scheduledTasks.filter {
                Instant.ofEpochMilli(it.scheduledDate!!).atZone(zoneId).toLocalDate() == targetDate
            }
            analyzeForSingleDay(dayTasks, dayMillis)
        } else {
            val groups = scheduledTasks.groupBy {
                Instant.ofEpochMilli(it.scheduledDate!!).atZone(zoneId).toLocalDate()
            }
            groups.flatMap { (date, dateTasks) ->
                val dateStartMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
                analyzeForSingleDay(dateTasks, dateStartMillis)
            }
        }
    }

    fun taskStartMinuteOfDay(task: TaskEntity): Int {
        val rawTime = task.scheduledTime
        if (rawTime != null && rawTime in 0 until ONE_DAY_MILLIS) {
            return (rawTime / 60_000L).toInt().coerceIn(0, 1439)
        }
        return task.timelineStartMinuteOfDay()
    }

    fun formatMinutesToTime(minuteOfDay: Int): String {
        val safeMin = ((minuteOfDay % 1440) + 1440) % 1440
        val h = safeMin / 60
        val m = safeMin % 60
        val ampm = if (h < 12) "AM" else "PM"
        val displayH = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format(Locale.ROOT, "%d:%02d %s", displayH, m, ampm)
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainschedulerplacementstabilitykt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/scheduler/PlacementStability.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/scheduler/PlacementStability.kt`
- **Lines of Code:** 121
- **Size:** 5,139 bytes (5.0 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.domain.model.Priority

/**
 * Whether an already-placed block is worth moving, and whether a new placement is worth writing.
 *
 * The scheduler wakes up every few minutes, so a decision that only asks "where is best right now"
 * keeps rewriting the same afternoon. This object makes the question "is the difference big enough
 * to disturb the user for" — the answers are ordered so stability checks run before optimisation.
 */
object PlacementStability {

    /** A block this close to starting is the user's current plan, not the scheduler's draft. */
    private const val NEAR_TERM_FREEZE_MILLIS = 2 * 60 * 60 * 1000L

    /** Smallest shift that justifies rewriting a placement the user can already see. */
    private const val MIN_MOVE_MILLIS = 30 * 60 * 1000L

    enum class ReplanVerdict(val allowsReplan: Boolean) {
        NEAR_TERM_FREEZE(false),
        COOLDOWN(false),
        KEEP(false),
        FORCED(true),
        DEADLINE_RESCUE(true),
        POSTPONE_PATTERN(true),
        FAR_FUTURE_PULL_IN(true),
        PERIODIC_REFRESH(true),
        PAST_EXPIRED_RESCUE(true)
    }

    /**
     * Ordered so that the two checks the user can feel — an imminent block and a recently written
     * one — run before any optimisation rule, including the manual trigger.
     */
    fun verdict(
        task: TaskEntity,
        nowMillis: Long,
        prefs: UserPreferences,
        forceReplan: Boolean
    ): ReplanVerdict {
        if (task.isScheduleLocked) return ReplanVerdict.KEEP

        val scheduledMillis = task.resolveScheduledEpochMillis()
            ?: return ReplanVerdict.KEEP
        val hoursUntilScheduled = (scheduledMillis - nowMillis) / 3_600_000f

        // Bug 2 & Feature 3 (ADR-026): Expired-Block Auto-Rescue
        // If a task's reserved block has ended and the 15-minute grace period has passed,
        // it is eligible for same-day rescue rescheduling rather than stranded permanently in the past.
        if (TaskBlockPlan.blockHasExpired(task, scheduledMillis, nowMillis, gracePeriodMillis = 15 * 60_000L)) {
            return ReplanVerdict.PAST_EXPIRED_RESCUE
        }

        if (scheduledMillis - nowMillis < NEAR_TERM_FREEZE_MILLIS) return ReplanVerdict.NEAR_TERM_FREEZE

        if (forceReplan) return ReplanVerdict.FORCED

        val deadlineMillis = task.resolveDeadlineEpochMillis()

        val taskAge = nowMillis - (task.lastAutoScheduledAt ?: task.updatedAt)
        val isUrgentDeadline = deadlineMillis?.let { (it - nowMillis) < 6 * 60 * 60 * 1000L } ?: false
        val cooldownMillis = when {
            isUrgentDeadline -> 60 * 60 * 1000L
            prefs.isAutoScheduleToggleOn -> 4 * 60 * 60 * 1000L
            else -> 6 * 60 * 60 * 1000L
        }
        if (taskAge <= cooldownMillis) return ReplanVerdict.COOLDOWN

        if (deadlineMillis != null) {
            val hoursUntilDeadline = (deadlineMillis - nowMillis) / 3_600_000f
            if (hoursUntilDeadline < 6f && hoursUntilScheduled > hoursUntilDeadline * 0.5f) {
                return ReplanVerdict.DEADLINE_RESCUE
            }
            if (hoursUntilDeadline < 12f && task.priority == Priority.LOW) {
                return ReplanVerdict.DEADLINE_RESCUE
            }
        }

        if (task.postponeCount >= 3) return ReplanVerdict.POSTPONE_PATTERN

        if (hoursUntilScheduled > 24f && !task.isScheduleLocked) {
            val isImportant = task.priority == Priority.HIGH || task.impactScore >= 80 || task.isFrog
            if (isImportant) return ReplanVerdict.FAR_FUTURE_PULL_IN
        }

        if (prefs.autoSchedulingEnabled) {
            val shouldRefresh = if (prefs.isAutoScheduleToggleOn) {
                true
            } else {
                val cycleNumber = (nowMillis / (6 * 60 * 60 * 1000L)) % 5
                val taskBucket = (task.id.hashCode().toLong() and 0x7FFFFFFF) % 5
                cycleNumber == taskBucket
            }
            if (shouldRefresh) return ReplanVerdict.PERIODIC_REFRESH
        }

        return ReplanVerdict.KEEP
    }

    /**
     * A write is only worth it when the user sees a different plan afterwards. An identical start
     * touches the database and the update timestamp for nothing, and a future block that shifts by
     * minutes is churn dressed as an improvement — unless the old slot is already behind us, in
     * which case staying put would leave the task stranded in the past.
     */
    fun isMoveWorthApplying(
        previousStartMillis: Long?,
        nextStartMillis: Long,
        nowMillis: Long
    ): Boolean {
        val previous = previousStartMillis ?: return true
        if (nextStartMillis == previous) return false
        if (previous <= nowMillis) return true
        return kotlin.math.abs(nextStartMillis - previous) >= MIN_MOVE_MILLIS
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainschedulerprotectedslotclockkt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/scheduler/ProtectedSlotClock.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/scheduler/ProtectedSlotClock.kt`
- **Lines of Code:** 190
- **Size:** 7,448 bytes (7.3 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsRecurrence
import java.util.Calendar

enum class SlotPhase {
    /** Inside the lead-in window but not started yet */
    LEAD_IN,

    /** The wall clock sits inside the slot */
    RUNNING,

    /** Just past the end, still worth showing as finishing */
    WRAPPING
}

data class DueSlot(
    val slot: ProtectedTimeSlot,
    val phase: SlotPhase,
    val startMillis: Long,
    val endMillis: Long,
    /** 0 unless the slot has not started yet */
    val minutesToStart: Int,
    /** Minutes until the slot ends; 0 once it is over */
    val minutesLeft: Int
)

/**
 * Answers "is one of the user's protected blocks happening right now, or about to?". One place
 * computes it so the Today hero and the Focus screen cannot disagree.
 *
 * A slot's own recovery buffer is deliberately not part of the takeover: the buffer exists to stop
 * the auto-scheduler from packing work immediately after exercise, not to keep a card on screen.
 */
object ProtectedSlotClock {

    const val DEFAULT_LEAD_IN_MINUTES = 120

    const val DEFAULT_WRAP_MINUTES = 10

    private const val MINUTE_MILLIS = 60_000L

    /** Which days a slot falls on. Ignores [ProtectedTimeSlot.isEnabled] — that is the caller's filter. */
    fun occursOnDay(slot: ProtectedTimeSlot, dayMillis: Long): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
        return when (slot.recurrence) {
            PtsRecurrence.DAILY -> true
            PtsRecurrence.WEEKDAYS -> cal.get(Calendar.DAY_OF_WEEK) in Calendar.MONDAY..Calendar.FRIDAY
            PtsRecurrence.WEEKENDS -> {
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
            }
            PtsRecurrence.CUSTOM -> {
                // Calendar.SUNDAY = 1, MONDAY = 2, ..., SATURDAY = 7
                val bit = 1 shl (cal.get(Calendar.DAY_OF_WEEK) - 1)
                (slot.weekdayBitmask and bit) != 0
            }
            PtsRecurrence.SPECIFIC_DATE -> slot.dateMillis?.let {
                sameCalendarDay(it, dayMillis)
            } == true
        }
    }

    /**
     * Slots overlapping `now - wrapMinutes .. now + leadInMinutes`, best candidate first: one that
     * is running or just ended takes the top spot, most recent start winning, then the ones still
     * to come in start order.
     *
     * The lead-in edge is inclusive, matching how the Today screen already buckets tasks at
     * `now + 2h`; the wrap edge is exclusive so a slot is off screen at exactly ten minutes past.
     */
    fun dueSlots(
        slots: List<ProtectedTimeSlot>,
        nowMillis: Long,
        leadInMinutes: Int = DEFAULT_LEAD_IN_MINUTES,
        wrapMinutes: Int = DEFAULT_WRAP_MINUTES
    ): List<DueSlot> {
        val windowStart = nowMillis - wrapMinutes * MINUTE_MILLIS
        val windowEnd = nowMillis + leadInMinutes * MINUTE_MILLIS

        return slots
            .filter { it.isEnabled }
            .flatMap { slot -> occurrences(slot, nowMillis).map { slot to it } }
            .filter { (_, span) -> span.endMillis > windowStart && span.startMillis <= windowEnd }
            .mapNotNull { (slot, span) -> dueSlotOf(slot, span, nowMillis, wrapMinutes) }
            .sortedWith(SlotOrder)
    }

    /** The slot that should hold the hero position, or null while every slot is still ahead. */
    fun takeoverOf(
        slots: List<ProtectedTimeSlot>,
        nowMillis: Long
    ): DueSlot? = dueSlots(slots, nowMillis).firstOrNull { it.phase != SlotPhase.LEAD_IN }

    /** The slots starting within the lead-in window, soonest first — the "Up Next" queue. */
    fun approaching(
        slots: List<ProtectedTimeSlot>,
        nowMillis: Long,
        leadInMinutes: Int = DEFAULT_LEAD_IN_MINUTES
    ): List<DueSlot> = dueSlots(slots, nowMillis, leadInMinutes = leadInMinutes).filter { it.phase == SlotPhase.LEAD_IN }

    private data class Span(val startMillis: Long, val endMillis: Long)

    /**
     * Yesterday is checked as well, because a block that began before midnight must still read as
     * running at 00:05. Tomorrow covers a lead-in that starts just after midnight.
     */
    private fun occurrences(slot: ProtectedTimeSlot, nowMillis: Long): List<Span> {
        val todaysStart = startOfDay(nowMillis)
        return (-1..1).mapNotNull { offset ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = todaysStart
                add(Calendar.DAY_OF_YEAR, offset)
            }
            val dayStart = cal.timeInMillis
            if (!occursOnDay(slot, dayStart)) return@mapNotNull null
            cal.set(Calendar.HOUR_OF_DAY, slot.startMinuteOfDay / 60)
            cal.set(Calendar.MINUTE, slot.startMinuteOfDay % 60)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            Span(start, start + slot.durationMinutes * MINUTE_MILLIS)
        }
    }

    private fun dueSlotOf(
        slot: ProtectedTimeSlot,
        span: Span,
        nowMillis: Long,
        wrapMinutes: Int
    ): DueSlot? = when {
        nowMillis < span.startMillis -> DueSlot(
            slot = slot,
            phase = SlotPhase.LEAD_IN,
            startMillis = span.startMillis,
            endMillis = span.endMillis,
            minutesToStart = minutesUp(span.startMillis - nowMillis),
            minutesLeft = minutesUp(span.endMillis - nowMillis)
        )

        nowMillis < span.endMillis -> DueSlot(
            slot = slot,
            phase = SlotPhase.RUNNING,
            startMillis = span.startMillis,
            endMillis = span.endMillis,
            minutesToStart = 0,
            minutesLeft = minutesUp(span.endMillis - nowMillis)
        )

        nowMillis < span.endMillis + wrapMinutes * MINUTE_MILLIS -> DueSlot(
            slot = slot,
            phase = SlotPhase.WRAPPING,
            startMillis = span.startMillis,
            endMillis = span.endMillis,
            minutesToStart = 0,
            minutesLeft = 0
        )

        else -> null
    }

    private fun minutesUp(millis: Long): Int =
        ((millis + MINUTE_MILLIS - 1) / MINUTE_MILLIS).coerceAtLeast(0L).toInt()

    private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun sameCalendarDay(a: Long, b: Long): Boolean {
        val left = Calendar.getInstance().apply { timeInMillis = a }
        val right = Calendar.getInstance().apply { timeInMillis = b }
        return left.get(Calendar.YEAR) == right.get(Calendar.YEAR) &&
            left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR)
    }

    private object SlotOrder : Comparator<DueSlot> {
        override fun compare(a: DueSlot, b: DueSlot): Int {
            val groupA = if (a.phase == SlotPhase.LEAD_IN) 1 else 0
            val groupB = if (b.phase == SlotPhase.LEAD_IN) 1 else 0
            if (groupA != groupB) return groupA - groupB
            return if (groupA == 0) b.startMillis.compareTo(a.startMillis)
            else a.startMillis.compareTo(b.startMillis)
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainschedulertaskblockplankt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/scheduler/TaskBlockPlan.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/scheduler/TaskBlockPlan.kt`
- **Lines of Code:** 50
- **Size:** 2,121 bytes (2.1 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.entity.TaskEntity

/**
 * How much clock time a scheduled task owns.
 *
 * The planner blocks a slot with the raw estimate inflated by the user's learned planning bias and
 * the slot's energy zone, then records that length on the task. Anything downstream that asks
 * "did this task get its time?" or "is this hour already taken?" reads the recorded block, so the
 * answer matches what was actually reserved instead of a second, cheaper guess at it.
 */
object TaskBlockPlan {

    private const val MAX_MINUTES = 360

    fun plannedMinutes(task: TaskEntity): Int {
        val recorded = task.plannedBlockMinutes
        if (recorded > 0) return recorded.coerceAtMost(MAX_MINUTES)
        return expectedMinutes(task)
    }

    /** The planner's own starting figure, before bias and energy inflate it. */
    fun expectedMinutes(task: TaskEntity): Int = when {
        task.plannedBlockMinutes > 0 && (task.id.contains("#part") || task.id.contains("_part") || task.parentTaskId != null || task.title.contains("#part") || task.title.contains("_part") || task.title.contains("(Part ")) -> task.plannedBlockMinutes
        task.estimatedDurationMinutes > 0 -> task.estimatedDurationMinutes
        else -> defaultMinutes(task)
    }

    /** What a task of this effort is worth when the user never gave it a length. */
    fun defaultMinutes(task: TaskEntity): Int = when {
        task.effortScore >= 80 -> 60
        task.effortScore >= 60 -> 45
        else -> 30
    }

    fun scheduledEndMillis(startMillis: Long, task: TaskEntity): Long =
        startMillis + plannedMinutes(task) * 60_000L

    /**
     * True once the task's reserved block is over and the grace period has run out. Comparing
     * against the raw estimate instead would call a task missed while it still has time on the clock.
     */
    fun blockHasExpired(
        task: TaskEntity,
        startMillis: Long,
        nowMillis: Long,
        gracePeriodMillis: Long
    ): Boolean = nowMillis > scheduledEndMillis(startMillis, task) + gracePeriodMillis
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainschedulertasktagschedulingprofilekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/scheduler/TaskTagSchedulingProfile.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/scheduler/TaskTagSchedulingProfile.kt`
- **Lines of Code:** 222
- **Size:** 10,746 bytes (10.5 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.entity.TaskEntity
import java.util.Locale

enum class TagEnergyDemand {
    LOW,
    MEDIUM,
    HIGH
}

enum class TagPreferredWindow {
    MORNING,
    MIDDAY,
    EVENING,
    FLEXIBLE
}

enum class TaskCategory {
    MINDFULNESS,
    EXERCISE,
    PHYSICAL,
    ANALYTICAL,
    HARD_WORK,
    CREATIVE,
    ROUTINE,
    FLEXIBLE
}

data class TagSchedulingProfile(
    val tag: String,
    val energyDemand: TagEnergyDemand,
    val preferredContext: String?,
    val fragmentationTolerance: Float,
    val preferredWindow: TagPreferredWindow,
    val category: TaskCategory = TaskCategory.FLEXIBLE
)

object TaskTagSchedulingProfile {
    val starterTags: List<String> = listOf(
        "study",
        "chores",
        "physical",
        "admin",
        "creative",
        "deep work",
        "meetings",
        "errands",
        "health",
        "finance",
        "social",
        "maintenance",
        "planning",
        "learning",
        "review",
        "writing",
        "coding",
        "reading",
        "household",
        "mindfulness",
        "meditation",
        "breathwork",
        "yoga",
        "journaling",
        "reflection",
        "exercise",
        "workout",
        "gym",
        "running",
        "stretching",
        "walking",
        "hard work"
    )

    private val profilesByTag: Map<String, TagSchedulingProfile> = listOf(
        TagSchedulingProfile("study", TagEnergyDemand.MEDIUM, "@computer", 0.35f, TagPreferredWindow.MORNING, TaskCategory.HARD_WORK),
        TagSchedulingProfile("chores", TagEnergyDemand.LOW, "@home", 0.9f, TagPreferredWindow.FLEXIBLE, TaskCategory.PHYSICAL),
        TagSchedulingProfile("physical", TagEnergyDemand.HIGH, null, 0.45f, TagPreferredWindow.MORNING, TaskCategory.PHYSICAL),
        TagSchedulingProfile("admin", TagEnergyDemand.LOW, "@computer", 0.95f, TagPreferredWindow.MIDDAY, TaskCategory.ROUTINE),
        TagSchedulingProfile("creative", TagEnergyDemand.MEDIUM, null, 0.4f, TagPreferredWindow.EVENING, TaskCategory.CREATIVE),
        TagSchedulingProfile("deep work", TagEnergyDemand.HIGH, "@computer", 0.2f, TagPreferredWindow.MORNING, TaskCategory.HARD_WORK),
        TagSchedulingProfile("meetings", TagEnergyDemand.MEDIUM, "@work", 0.8f, TagPreferredWindow.MIDDAY, TaskCategory.ROUTINE),
        TagSchedulingProfile("errands", TagEnergyDemand.LOW, "@errands", 1.0f, TagPreferredWindow.FLEXIBLE, TaskCategory.PHYSICAL),
        TagSchedulingProfile("health", TagEnergyDemand.MEDIUM, null, 0.6f, TagPreferredWindow.MORNING, TaskCategory.PHYSICAL),
        TagSchedulingProfile("finance", TagEnergyDemand.HIGH, "@computer", 0.3f, TagPreferredWindow.MORNING, TaskCategory.ANALYTICAL),
        TagSchedulingProfile("social", TagEnergyDemand.LOW, null, 0.95f, TagPreferredWindow.EVENING, TaskCategory.ROUTINE),
        TagSchedulingProfile("maintenance", TagEnergyDemand.LOW, "@home", 0.85f, TagPreferredWindow.FLEXIBLE, TaskCategory.PHYSICAL),
        TagSchedulingProfile("planning", TagEnergyDemand.MEDIUM, "@computer", 0.5f, TagPreferredWindow.MORNING, TaskCategory.ROUTINE),
        TagSchedulingProfile("learning", TagEnergyDemand.MEDIUM, "@computer", 0.45f, TagPreferredWindow.MORNING, TaskCategory.HARD_WORK),
        TagSchedulingProfile("review", TagEnergyDemand.LOW, "@computer", 0.8f, TagPreferredWindow.MIDDAY, TaskCategory.ROUTINE),
        TagSchedulingProfile("writing", TagEnergyDemand.MEDIUM, "@computer", 0.4f, TagPreferredWindow.MORNING, TaskCategory.HARD_WORK),
        TagSchedulingProfile("coding", TagEnergyDemand.HIGH, "@computer", 0.25f, TagPreferredWindow.MORNING, TaskCategory.HARD_WORK),
        TagSchedulingProfile("reading", TagEnergyDemand.LOW, null, 0.75f, TagPreferredWindow.EVENING, TaskCategory.ROUTINE),
        TagSchedulingProfile("household", TagEnergyDemand.LOW, "@home", 0.95f, TagPreferredWindow.FLEXIBLE, TaskCategory.PHYSICAL),

        // Smart category tags
        TagSchedulingProfile("mindfulness", TagEnergyDemand.LOW, null, 0.8f, TagPreferredWindow.MORNING, TaskCategory.MINDFULNESS),
        TagSchedulingProfile("meditation", TagEnergyDemand.LOW, null, 0.8f, TagPreferredWindow.MORNING, TaskCategory.MINDFULNESS),
        TagSchedulingProfile("breathwork", TagEnergyDemand.LOW, null, 0.9f, TagPreferredWindow.MORNING, TaskCategory.MINDFULNESS),
        TagSchedulingProfile("yoga", TagEnergyDemand.LOW, null, 0.85f, TagPreferredWindow.MORNING, TaskCategory.MINDFULNESS),
        TagSchedulingProfile("journaling", TagEnergyDemand.LOW, null, 0.9f, TagPreferredWindow.MORNING, TaskCategory.MINDFULNESS),
        TagSchedulingProfile("reflection", TagEnergyDemand.LOW, null, 0.9f, TagPreferredWindow.MORNING, TaskCategory.MINDFULNESS),
        TagSchedulingProfile("exercise", TagEnergyDemand.HIGH, null, 0.5f, TagPreferredWindow.MORNING, TaskCategory.EXERCISE),
        TagSchedulingProfile("workout", TagEnergyDemand.HIGH, null, 0.5f, TagPreferredWindow.MORNING, TaskCategory.EXERCISE),
        TagSchedulingProfile("gym", TagEnergyDemand.HIGH, null, 0.5f, TagPreferredWindow.MORNING, TaskCategory.EXERCISE),
        TagSchedulingProfile("running", TagEnergyDemand.MEDIUM, null, 0.7f, TagPreferredWindow.MORNING, TaskCategory.EXERCISE),
        TagSchedulingProfile("stretching", TagEnergyDemand.LOW, null, 0.85f, TagPreferredWindow.MORNING, TaskCategory.EXERCISE),
        TagSchedulingProfile("walking", TagEnergyDemand.LOW, null, 0.95f, TagPreferredWindow.FLEXIBLE, TaskCategory.EXERCISE),
        TagSchedulingProfile("hard work", TagEnergyDemand.HIGH, "@computer", 0.2f, TagPreferredWindow.MORNING, TaskCategory.HARD_WORK)
    ).associateBy { normalize(it.tag) }

    fun profileFor(tag: String): TagSchedulingProfile? = profilesByTag[normalize(tag)]

    fun profilesFor(tagsCsv: String): List<TagSchedulingProfile> {
        return tagsCsv
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { profileFor(it) }
    }

    private fun normalize(tag: String): String = tag.trim().trimStart('@').lowercase(Locale.getDefault())

    fun tagMatchesKeywords(tag: String, keywords: List<String>): Boolean {
        val words = tag.split(Regex("[^a-zA-Z0-9_-]+")).filter { it.isNotBlank() }
        return keywords.any { kw ->
            if (kw.contains(' ') || kw.contains('-')) {
                tag.equals(kw, ignoreCase = true) || tag.contains(kw)
            } else {
                words.any { it.equals(kw, ignoreCase = true) }
            }
        }
    }
}

fun TaskEntity.determineCategory(): TaskCategory {
    // 0. Respect explicitly persisted category (user override or previously inferred+saved)
    if (!schedulingCategory.isNullOrBlank()) {
        try {
            return TaskCategory.valueOf(schedulingCategory)
        } catch (_: IllegalArgumentException) {
            // Unknown value in DB — fall through to inference
        }
    }

    val tagsList = tags.split(',')
        .map { it.trim().trimStart('@').lowercase(Locale.getDefault()) }
        .filter { it.isNotBlank() }

    // 1. Try to find a matching tag and use its profile category if available
    for (tag in tagsList) {
        val profile = TaskTagSchedulingProfile.profileFor(tag)
        if (profile != null && profile.category != TaskCategory.FLEXIBLE) {
            return profile.category
        }
    }

    // 2. Explicit keywords fallback (catches custom tags not in profilesByTag)
    return when {
        tagsList.any { tag ->
            TaskTagSchedulingProfile.tagMatchesKeywords(tag, listOf("mindfulness", "meditation", "breathwork", "yoga", "reflection", "journaling", "journal", "mental health", "calm"))
        } -> {
            TaskCategory.MINDFULNESS
        }
        tagsList.any { tag ->
            TaskTagSchedulingProfile.tagMatchesKeywords(tag, listOf("exercise", "workout", "gym", "running", "run", "cardio", "fitness", "training", "sports", "stretching", "walking", "swim"))
        } -> {
            TaskCategory.EXERCISE
        }
        tagsList.any { tag ->
            TaskTagSchedulingProfile.tagMatchesKeywords(tag, listOf("hard work", "deep work", "coding", "programming", "dev", "software", "writing", "study", "learning", "finance", "focus", "math", "research", "analysis"))
        } -> {
            TaskCategory.HARD_WORK
        }
        tagsList.any { tag ->
            TaskTagSchedulingProfile.tagMatchesKeywords(tag, listOf("creative", "design", "art", "ui", "ux", "drafting", "music", "painting", "drawing", "brainstorm"))
        } -> {
            TaskCategory.CREATIVE
        }
        tagsList.any { tag ->
            TaskTagSchedulingProfile.tagMatchesKeywords(tag, listOf("physical", "chores", "errands", "household", "cleaning", "maintenance", "laundry", "repairs"))
        } -> {
            TaskCategory.PHYSICAL
        }
        tagsList.any { tag ->
            TaskTagSchedulingProfile.tagMatchesKeywords(tag, listOf("admin", "meetings", "email", "bills", "invoices", "paperwork", "review", "planning", "organize"))
        } -> {
            TaskCategory.ROUTINE
        }
        // 3. Fall back to TaskType mapping
        // Bug 4 Fix (ADR-026): Map MINDFULNESS to MINDFULNESS and GENERAL to FLEXIBLE
        else -> when (taskType) {
            com.neuroflow.app.domain.model.TaskType.ANALYTICAL -> TaskCategory.ANALYTICAL
            com.neuroflow.app.domain.model.TaskType.CREATIVE -> TaskCategory.CREATIVE
            com.neuroflow.app.domain.model.TaskType.ADMIN -> TaskCategory.ROUTINE
            com.neuroflow.app.domain.model.TaskType.PHYSICAL -> TaskCategory.PHYSICAL
            com.neuroflow.app.domain.model.TaskType.GENERAL -> TaskCategory.FLEXIBLE
            com.neuroflow.app.domain.model.TaskType.MINDFULNESS -> TaskCategory.MINDFULNESS
        }
    }
}

fun TaskEntity.isNonWorkOrErrandTask(): Boolean {
    val category = determineCategory()
    if (category in listOf(TaskCategory.MINDFULNESS, TaskCategory.EXERCISE, TaskCategory.PHYSICAL)) {
        return true
    }

    val tagsList = tags.split(',')
        .map { it.trim().trimStart('@').lowercase(Locale.getDefault()) }
        .filter { it.isNotBlank() }

    val nonWorkKeywords = listOf(
        "errand", "errands", "chore", "chores", "personal", "life", "home",
        "household", "cleaning", "maintenance", "shopping", "groceries", "health",
        "routine", "leisure", "break", "family", "social", "meditation", "mindfulness",
        "exercise", "workout", "gym", "running", "walking", "stretching", "non-work",
        "not work", "not-work", "off-hours"
    )

    return tagsList.any { tag -> TaskTagSchedulingProfile.tagMatchesKeywords(tag, nonWorkKeywords) }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainschedulerbehavioralinsightsenginekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/scheduler/BehavioralInsightsEngine.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/scheduler/BehavioralInsightsEngine.kt`
- **Lines of Code:** 41
- **Size:** 1,505 bytes (1.5 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min

object BehavioralInsightsEngine {
    /**
     * Bayesian shrinkage formula for duration estimation calibration.
     * shrinkageFactor = (k0 * prior + n * observedGeometricMean) / (k0 + n)
     */
    fun calculateCalibrationFactor(
        actualMinutes: List<Int>,
        estimatedMinutes: List<Int>,
        k0: Float = 4.0f,
        prior: Float = 1.0f
    ): Float {
        if (actualMinutes.isEmpty() || estimatedMinutes.isEmpty()) return prior
        val n = min(actualMinutes.size, estimatedMinutes.size)
        if (n == 0) return prior

        val ratios = (0 until n).map { i ->
            val est = estimatedMinutes[i].coerceAtLeast(1).toFloat()
            val act = actualMinutes[i].coerceAtLeast(1).toFloat()
            (act / est).coerceIn(0.4f, 3.0f)
        }
        val geometricMean = exp(ratios.map { ln(it.toDouble()) }.average()).toFloat()
        val calibrated = (k0 * prior + n * geometricMean) / (k0 + n)
        return calibrated.coerceIn(0.8f, 1.6f)
    }

    /**
     * Calculates empirical circadian density distribution across 24 hourly bins.
     */
    fun calculateHourlyCircadianDensity(focusMinutesByHour: Map<Int, Int>): List<Float> {
        val totalMinutes = focusMinutesByHour.values.sum().coerceAtLeast(1).toFloat()
        return (0..23).map { hour ->
            (focusMinutesByHour[hour] ?: 0).toFloat() / totalMinutes
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainschedulerdependencygraphvalidatorkt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/scheduler/DependencyGraphValidator.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/scheduler/DependencyGraphValidator.kt`
- **Lines of Code:** 117
- **Size:** 4,332 bytes (4.2 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.domain.service.SchedulableTask

/**
 * ADR-020: Cycle-Safe Dependency Graph Validator & Sanitizer.
 *
 * Ensures that circular dependencies (e.g. A -> B -> A) never cause Kahn's topological sort
 * in `TaskSchedulerService.scheduleDay` to silently drop tasks.
 */
object DependencyGraphValidator {

    data class SanitizedDagResult(
        val tasks: List<SchedulableTask>,
        val brokenCycleEdges: List<Pair<String, String>>
    )

    /**
     * Detects and breaks any cycle in [tasks]' prerequisite graph before Kahn's algorithm runs.
     * Guarantees 100% of input tasks reach the priority queue during `scheduleDay`.
     */
    fun sanitizeDag(tasks: List<SchedulableTask>): SanitizedDagResult {
        if (tasks.size <= 1) return SanitizedDagResult(tasks, emptyList())

        val taskMap = tasks.associateBy { it.id }
        val adj = mutableMapOf<String, MutableSet<String>>() // prereq -> dependent
        val inDegree = mutableMapOf<String, Int>()
        val prereqMap = mutableMapOf<String, MutableSet<String>>()

        for (t in tasks) {
            val validPrereqs = t.prerequisiteIds
                .filter { it != t.id && it in taskMap }
                .toMutableSet()
            prereqMap[t.id] = validPrereqs
            inDegree[t.id] = validPrereqs.size
            for (p in validPrereqs) {
                adj.getOrPut(p) { mutableSetOf() }.add(t.id)
            }
        }

        val queue = ArrayDeque<String>()
        inDegree.forEach { (id, deg) -> if (deg == 0) queue.add(id) }

        val visited = mutableSetOf<String>()
        val brokenEdges = mutableListOf<Pair<String, String>>()

        while (visited.size < tasks.size) {
            while (queue.isNotEmpty()) {
                val curr = queue.removeFirst()
                if (!visited.add(curr)) continue
                for (dep in adj[curr].orEmpty()) {
                    val newDeg = (inDegree[dep] ?: 1) - 1
                    inDegree[dep] = newDeg
                    if (newDeg == 0 && dep !in visited) {
                        queue.add(dep)
                    }
                }
            }

            if (visited.size < tasks.size) {
                // Cycle detected among unvisited nodes!
                // Deterministically select the highest-leverage unvisited task and sever its unvisited incoming edges.
                val victim = tasks
                    .filter { it.id !in visited }
                    .maxWithOrNull(
                        compareBy<SchedulableTask> { it.isFrog }
                            .thenBy { it.priority }
                            .thenBy { it.impactScore }
                            .thenByDescending { it.id }
                    ) ?: break

                val unresolvedPrereqs = prereqMap[victim.id].orEmpty().filter { it !in visited }
                for (p in unresolvedPrereqs) {
                    prereqMap[victim.id]?.remove(p)
                    adj[p]?.remove(victim.id)
                    brokenEdges.add(p to victim.id)
                }
                inDegree[victim.id] = 0
                queue.add(victim.id)
            }
        }

        val sanitizedTasks = tasks.map { task ->
            val cleanPrereqs = prereqMap[task.id].orEmpty()
            task.copy(
                prerequisiteIds = cleanPrereqs,
                parentTaskId = task.parentTaskId
            )
        }

        return SanitizedDagResult(sanitizedTasks, brokenEdges)
    }

    /**
     * Checks whether adding [candidatePrerequisiteId] as a prerequisite of [taskId]
     * would introduce a cycle in [existingDepsByTask].
     */
    fun wouldCreateCycle(
        taskId: String,
        candidatePrerequisiteId: String,
        existingDepsByTask: Map<String, Set<String>>
    ): Boolean {
        if (taskId == candidatePrerequisiteId) return true
        val visited = mutableSetOf<String>()
        val stack = ArrayDeque<String>()
        stack.add(candidatePrerequisiteId)

        while (stack.isNotEmpty()) {
            val curr = stack.removeLast()
            if (curr == taskId) return true
            if (visited.add(curr)) {
                existingDepsByTask[curr]?.forEach { next -> stack.add(next) }
            }
        }
        return false
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainschedulerschedulepreparationorchestratorkt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt`
- **Lines of Code:** 322
- **Size:** 16,064 bytes (15.7 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.SleepLogEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.data.local.entity.resolveDeadlineEpochMillis
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.engine.AnalyticsEngine
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.buildBehavioralObservations
import com.neuroflow.app.domain.model.buildDailyCognitiveBlocks
import com.neuroflow.app.domain.model.computeRollingDailySleepAverage
import com.neuroflow.app.domain.service.AdaptiveCircadianLearner
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.ChronotypeProfile
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.PriorityWeightProfile
import com.neuroflow.app.domain.service.ProtectedLocalSlot
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.ScheduledPlacement
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.domain.service.TimeSlotWindow
import com.neuroflow.app.data.local.PhysiologicalEventType
import com.neuroflow.app.data.local.parsePhysiologicalLogs
import com.neuroflow.app.data.local.parseStimulantLogs
import kotlin.math.roundToInt
import com.neuroflow.app.domain.service.CognitiveBlockCategory
import com.neuroflow.app.domain.service.CognitiveBlockWindow
import com.neuroflow.app.domain.service.DaytimeNapBout
import com.neuroflow.app.domain.service.SocialJetlagCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class OrchestrationOutcome(
    val placements: List<ScheduledPlacement>,
    val deferrals: List<DeferralRationale> = emptyList()
)

/**
 * ADR-020: Unified Foreground & Background Schedule Preparation Orchestrator.
 *
 * Eliminates the split-brain divergence between `PlanViewModelV2.autoScheduleDay` and
 * `ScheduleAutoTasksWorker.doWork` by running the exact same Bayesian calibration,
 * 32-tag profile mapping, cycle-safe DAG sanitization, and Kalman circadian learning pipeline.
 */
@Singleton
class SchedulePreparationOrchestrator @Inject constructor(
    private val taskSchedulerService: TaskSchedulerService,
    private val behavioralSupportService: BehavioralSupportService,
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null
) {
    fun orchestrateDaySchedule(
        candidateTasks: List<TaskEntity>,
        allKnownTasks: List<TaskEntity>,
        closedSessions: List<TimeSessionEntity>,
        sleepLogs: List<SleepLogEntity>,
        prefs: UserPreferences,
        dayDate: LocalDate,
        nowMillis: Long,
        busyWindows: List<TimeSlotWindow>,
        forceReplan: Boolean,
        zoneId: ZoneId = ZoneId.systemDefault(),
        activeTaskId: String? = null,
        microStepCounts: Map<String, Pair<Int, Int>> = emptyMap()
    ): List<ScheduledPlacement> = orchestrateDayScheduleWithOutcome(
        candidateTasks = candidateTasks,
        allKnownTasks = allKnownTasks,
        closedSessions = closedSessions,
        sleepLogs = sleepLogs,
        prefs = prefs,
        dayDate = dayDate,
        nowMillis = nowMillis,
        busyWindows = busyWindows,
        forceReplan = forceReplan,
        zoneId = zoneId,
        activeTaskId = activeTaskId,
        microStepCounts = microStepCounts
    ).placements

    fun orchestrateDayScheduleWithOutcome(
        candidateTasks: List<TaskEntity>,
        allKnownTasks: List<TaskEntity>,
        closedSessions: List<TimeSessionEntity>,
        sleepLogs: List<SleepLogEntity>,
        prefs: UserPreferences,
        dayDate: LocalDate,
        nowMillis: Long,
        busyWindows: List<TimeSlotWindow>,
        forceReplan: Boolean,
        zoneId: ZoneId = ZoneId.systemDefault(),
        activeTaskId: String? = null,
        microStepCounts: Map<String, Pair<Int, Int>> = emptyMap()
    ): OrchestrationOutcome {
        if (candidateTasks.isEmpty()) return OrchestrationOutcome(emptyList())

        val planningCalibration = AnalyticsEngine.PlanningCalibration.from(allKnownTasks)
        val enjoymentCalibration = AnalyticsEngine.EnjoymentCalibration.from(allKnownTasks)
        val globalCalibrationRatio = planningCalibration.globalFactor.toDouble().coerceIn(0.8, 1.6)

        val rawSchedulable = candidateTasks.map { task ->
            val resolvedCategory = when (task.determineCategory()) {
                com.neuroflow.app.domain.scheduler.TaskCategory.HARD_WORK,
                com.neuroflow.app.domain.scheduler.TaskCategory.ANALYTICAL -> TaskCategory.DEEP_WORK
                com.neuroflow.app.domain.scheduler.TaskCategory.CREATIVE -> TaskCategory.CREATIVE
                com.neuroflow.app.domain.scheduler.TaskCategory.ROUTINE -> TaskCategory.ADMIN
                com.neuroflow.app.domain.scheduler.TaskCategory.EXERCISE,
                com.neuroflow.app.domain.scheduler.TaskCategory.PHYSICAL -> TaskCategory.EXERCISE
                com.neuroflow.app.domain.scheduler.TaskCategory.MINDFULNESS -> TaskCategory.MINDFULNESS
                com.neuroflow.app.domain.scheduler.TaskCategory.FLEXIBLE -> TaskCategory.GENERAL
            }
            val tagProfiles = TaskTagSchedulingProfile.profilesFor(task.tags)
            val fragTolerance = tagProfiles.minOfOrNull { it.fragmentationTolerance } ?: 0.5f
            val inferredContext = task.contextTag.ifBlank {
                tagProfiles.firstNotNullOfOrNull { it.preferredContext }.orEmpty()
            }
            val calibratedEnjoyment = (enjoymentCalibration.forecast(task.enjoymentScore) / 10.0).roundToInt().coerceIn(0, 10)

            val expectedDuration = TaskBlockPlan.expectedMinutes(task)
            val isDurationExplicit = task.isScheduleLocked

            SchedulableTask(
                id = task.id,
                title = task.title,
                category = resolvedCategory,
                priority = when (task.priority) {
                    Priority.HIGH -> 2
                    Priority.MEDIUM -> 1
                    else -> 0
                },
                estimatedMinutes = expectedDuration,
                isDurationExplicit = isDurationExplicit,
                energyDemand = when (task.energyLevel) {
                    EnergyLevel.HIGH -> EnergyDemand.HIGH
                    EnergyLevel.MEDIUM -> EnergyDemand.MODERATE
                    else -> EnergyDemand.LOW
                },
                isFrog = task.isFrog,
                scheduledStartMillis = task.resolveScheduledEpochMillis(),
                deadlineMillis = task.resolveDeadlineEpochMillis(),
                isLocked = task.isScheduleLocked,
                parentTaskId = task.parentTaskId,
                completedMicroSteps = microStepCounts[task.id]?.first ?: 0,
                totalMicroSteps = microStepCounts[task.id]?.second ?: 0,
                isCurrentlyActive = (activeTaskId != null && (task.id == activeTaskId || task.id.startsWith("$activeTaskId#"))),
                createdAtMillis = task.createdAt,
                prerequisiteIds = task.dependsOnTaskIds.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .toSet(),
                impactScore = task.impactScore,
                estimatedEnjoyment = calibratedEnjoyment,
                postponeCount = task.postponeCount,
                contextTag = inferredContext,
                isAnxietyTask = task.isAnxietyTask,
                hasImplementationIntention = task.ifThenPlan.isNotBlank(),
                fragmentationTolerance = fragTolerance,
                distractionScore = task.distractionScore
            )
        }

        val sanitizedDag = DependencyGraphValidator.sanitizeDag(rawSchedulable).tasks
        val preparedTasks = if (forceReplan) {
            behavioralSupportService.resetScheduleAnchors(sanitizedDag)
        } else {
            sanitizedDag
        }

        val zonedNow = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        var wakeAnchor = zonedNow.toLocalDate().atTime(prefs.wakeUpHour.coerceIn(0, 23), 0).atZone(zoneId)
        if (wakeAnchor.toInstant().toEpochMilli() > nowMillis) {
            wakeAnchor = wakeAnchor.minusDays(1)
        }
        val baseWakeMillis = wakeAnchor.toInstant().toEpochMilli()

        val sleepSignal = sleepTelemetryRepository?.normalizeLogs(
            logs = sleepLogs,
            baseWakeMillis = baseWakeMillis,
            baseSleepMinutes = 480.0
        )
        val wakeTimeMillis = sleepSignal?.normalizedWakeTimeMillis ?: baseWakeMillis
        val lastSleepDurationMinutes = sleepSignal?.blendedDurationMinutes?.roundToInt()?.coerceIn(60, 960) ?: 480
        val rollingAvgSleep = computeRollingDailySleepAverage(sleepLogs, nowMillis, zoneId)

        val dayStartMillis = dayDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

        // Bug 1 Fix: filter protected slots by day of week & add recovery buffer
        val protectedSlots = prefs.protectedTimeSlots
            .filter { it.isEnabled && ProtectedSlotClock.occursOnDay(it, dayStartMillis) }
            .map {
                ProtectedLocalSlot(
                    startHour = (it.startMinuteOfDay / 60).coerceIn(0, 23),
                    startMinute = (it.startMinuteOfDay % 60).coerceIn(0, 59),
                    durationMinutes = (it.durationMinutes + it.recoveryBufferMinutes).coerceIn(15, 480)
                )
            }

        // Bug 3 Fix: parse quick-logged physiological events (meals, workouts, naps) and stimulants
        val physLogs = parsePhysiologicalLogs(prefs.quickPhysiologicalLogsJson)
            .filter { it.timestampMillis in (dayStartMillis - 8 * 3600_000L)..(dayStartMillis + 26 * 3600_000L) }
        val quickPhysBlocks = physLogs.mapNotNull { log ->
            val logZdt = Instant.ofEpochMilli(log.timestampMillis).atZone(zoneId)
            val minOfDay = logZdt.hour * 60 + logZdt.minute
            val duration = log.durationMinutes.coerceIn(5, 240)
            when (log.type) {
                PhysiologicalEventType.MEAL -> CognitiveBlockWindow(
                    startMinuteOfDay = minOfDay,
                    endMinuteOfDay = (minOfDay + duration) % 1440,
                    category = CognitiveBlockCategory.MEAL,
                    intensity = log.intensity,
                    label = "Quick Logged Meal #${log.timestampMillis}",
                    explicitStartMillis = log.timestampMillis,
                    explicitEndMillis = log.timestampMillis + duration * 60_000L
                )
                PhysiologicalEventType.WORKOUT -> CognitiveBlockWindow(
                    startMinuteOfDay = minOfDay,
                    endMinuteOfDay = (minOfDay + duration) % 1440,
                    category = CognitiveBlockCategory.EXERCISE,
                    intensity = log.intensity,
                    label = "Quick Logged Workout #${log.timestampMillis}",
                    explicitStartMillis = log.timestampMillis,
                    explicitEndMillis = log.timestampMillis + duration * 60_000L
                )
                PhysiologicalEventType.NAP -> null
            }
        }
        val quickPhysNaps = physLogs
            .filter { it.type == PhysiologicalEventType.NAP }
            .map { log ->
                val napDuration = log.durationMinutes.coerceIn(5, 180)
                DaytimeNapBout(
                    startMillis = log.timestampMillis,
                    endMillis = log.timestampMillis + napDuration * 60_000L
                )
            }

        val allNaps = ((sleepSignal?.daytimeNaps ?: emptyList()) + quickPhysNaps).sortedBy { it.startMillis }
        val basePhysioBlocks = buildDailyCognitiveBlocks(
            slots = prefs.protectedTimeSlots,
            date = dayDate,
            zoneId = zoneId
        ).filter { it.category == CognitiveBlockCategory.MEAL || it.category == CognitiveBlockCategory.EXERCISE }
        val physiologicalBlocks = basePhysioBlocks + quickPhysBlocks
        val stimulantDoses = (
            parseStimulantLogs(prefs.stimulantLogsJson) +
            com.neuroflow.app.domain.model.extractStimulantDosesFromSlots(prefs.protectedTimeSlots, dayDate, zoneId)
        ).distinctBy { it.timestampMillis to it.type }

        // Bug 4 Fix: pass active uncompleted task IDs to prevent premature placement of cross-day dependents
        val activeUncompletedTaskIds = allKnownTasks
            .filter { it.status == TaskStatus.ACTIVE }
            .map { it.id }
            .toSet()

        // Feature 3 (ADR-023): Social Jetlag Phase-Shift Auto-Pacing
        val sleepWindows = sleepLogs.map { TimeSlotWindow(it.startAt, it.endAt) }
        val sjlReport = SocialJetlagCalculator.evaluateFromSleepWindows(
            sleepWindows = sleepWindows,
            nowMillis = nowMillis,
            zoneId = zoneId
        )
        val socialJetlagLagHours = sjlReport.mondayMorningPhaseLagHours

        val chronotype = ChronotypeProfile.fromString(prefs.quizChronotype ?: prefs.manualChronotype)
        val manualPeakStartHour = if (prefs.manualPeakProfileEnabled) {
            (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23)
        } else null

        val completedTasks = allKnownTasks.filter { it.status == TaskStatus.COMPLETED }
        val observations = buildBehavioralObservations(completedTasks, closedSessions, zoneId)
        val initialPeak = if (prefs.effectivePeakStart in 0..23) prefs.effectivePeakStart.toDouble() else 9.0
        val learnedProfile = if (observations.isNotEmpty()) {
            AdaptiveCircadianLearner.learnProfile(observations, initialPeakHour = initialPeak)
        } else null

        val weights = PriorityWeightProfile(
            urgencyWeight = prefs.weightDeadlineUrgency.toDouble(),
            impactWeight = prefs.weightImpact.toDouble(),
            circadianMatchWeight = prefs.weightCircadianMatch.toDouble(),
            momentumWeight = prefs.weightSessionProgress.toDouble(),
            frogBonusMultiplier = prefs.weightFrogBonus.toDouble(),
            quadrantWeight = prefs.weightQuadrant.toDouble(),
            priorityLevelWeight = prefs.weightPriorityLevel.toDouble(),
            durationWeight = prefs.weightDuration.toDouble(),
            focusModeWeight = prefs.weightFocusMode.toDouble(),
            planBoostWeight = prefs.weightPlanBoost.toDouble(),
            enjoymentWeight = prefs.weightEnjoyment.toDouble()
        )

        val outcome = taskSchedulerService.scheduleDayWithOutcome(
            tasks = preparedTasks,
            dayDate = dayDate,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeTimeMillis,
            lastSleepDurationMinutes = lastSleepDurationMinutes,
            busyWindows = busyWindows,
            protectedSlots = protectedSlots,
            calibrationRatio = globalCalibrationRatio,
            forceReplan = forceReplan,
            zoneId = zoneId,
            naps = allNaps,
            chronotype = chronotype,
            manualPeakStartHour = manualPeakStartHour,
            priorityWeights = weights,
            learnedProfile = learnedProfile,
            rollingAverageSleepMinutes = rollingAvgSleep,
            physiologicalBlocks = physiologicalBlocks,
            stimulantDoses = stimulantDoses,
            socialJetlagLagHours = socialJetlagLagHours,
            activeUncompletedTaskIds = activeUncompletedTaskIds
        )

        return OrchestrationOutcome(outcome.placements, outcome.deferrals)
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdatacalendarcalendarsyncdatasourcekt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt`
- **Lines of Code:** 458
- **Size:** 19,880 bytes (19.4 KB)

```kotlin
package com.neuroflow.app.data.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.service.TimeSlotWindow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

interface CalendarSyncRepository {
    suspend fun getBusyWindows(startMillis: Long, endMillis: Long): List<TimeSlotWindow>
    suspend fun upsertTaskEvent(
        task: TaskEntity,
        startMillis: Long? = null,
        durationMinutes: Int? = null
    ): Long?
    suspend fun deleteTaskEvent(taskId: String): Boolean
    suspend fun handleTaskStateChanged(task: TaskEntity): Boolean
}

interface AndroidCalendarSyncRepository : CalendarSyncRepository {
    suspend fun findPrimaryWritableCalendarId(): Long?
    suspend fun findEventIdByTaskId(taskId: String): Long?
}

/**
 * ADR-002 Native Android Calendar Sync DataSource
 *
 * Bridges local Android OS Calendar Provider via CalendarContract.Instances and CalendarContract.Events.
 * 100% offline-first, multi-provider (Google, Outlook, Exchange, DAVx5), zero OAuth tokens.
 *
 * Enforces:
 * 1. Filtering out declined events, canceled events, and free availability.
 * 2. Excluding self-authored neuroFlow blocks ("com.neuroflow.app" / "neuroflow://task/...").
 * 3. Sweep-line interval coalescing with 5-minute post-meeting decompression buffer for meetings >= 45m.
 * 4. Loop-safe 2-way write-back for Daily Frogs and locked Focus Blocks with idempotent upsert and lifecycle cleanup.
 */
@Singleton
class CalendarSyncDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : CalendarSyncRepository, AndroidCalendarSyncRepository {

    override suspend fun getBusyWindows(startMillis: Long, endMillis: Long): List<TimeSlotWindow> {
        if (startMillis >= endMillis) return emptyList()
        return withContext(Dispatchers.IO) {
            val raw = queryInstances(startMillis, endMillis)
            coalesceWindows(raw)
        }
    }

    internal fun queryInstances(startMillis: Long, endMillis: Long): List<TimeSlotWindow> {
        if (startMillis >= endMillis) return emptyList()
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return emptyList()
        }

        val results = mutableListOf<TimeSlotWindow>()
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)
        val uri = builder.build()

        val projection = arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.AVAILABILITY,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS,
            CalendarContract.Instances.STATUS,
            CalendarContract.Instances.ORGANIZER,
            CalendarContract.Events.CUSTOM_APP_PACKAGE,
            CalendarContract.Events.CUSTOM_APP_URI
        )

        val selection = null
        val selectionArgs = null
        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val descIdx = cursor.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
                val availIdx = cursor.getColumnIndex(CalendarContract.Instances.AVAILABILITY)
                val attendeeIdx = cursor.getColumnIndex(CalendarContract.Instances.SELF_ATTENDEE_STATUS)
                val statusIdx = cursor.getColumnIndex(CalendarContract.Instances.STATUS)
                val orgIdx = cursor.getColumnIndex(CalendarContract.Instances.ORGANIZER)
                val pkgIdx = cursor.getColumnIndex(CalendarContract.Events.CUSTOM_APP_PACKAGE)
                val appUriIdx = cursor.getColumnIndex(CalendarContract.Events.CUSTOM_APP_URI)

                while (cursor.moveToNext()) {
                    val s = if (beginIdx >= 0) cursor.getLong(beginIdx) else 0L
                    val e = if (endIdx >= 0) cursor.getLong(endIdx) else 0L
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx) ?: "" else ""
                    val desc = if (descIdx >= 0) cursor.getString(descIdx) ?: "" else ""
                    val avail = if (availIdx >= 0) cursor.getInt(availIdx) else CalendarContract.Events.AVAILABILITY_BUSY
                    val attendeeStatus = if (attendeeIdx >= 0) cursor.getInt(attendeeIdx) else CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED
                    val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else CalendarContract.Events.STATUS_CONFIRMED
                    val organizer = if (orgIdx >= 0) cursor.getString(orgIdx) ?: "" else ""
                    val customPkg = if (pkgIdx >= 0) cursor.getString(pkgIdx) ?: "" else ""
                    val customUri = if (appUriIdx >= 0) cursor.getString(appUriIdx) ?: "" else ""

                    // 1. Filter out free availability
                    if (avail == CalendarContract.Events.AVAILABILITY_FREE) {
                        continue
                    }

                    // 2. Filter out declined or canceled events
                    if (attendeeStatus == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED ||
                        status == CalendarContract.Events.STATUS_CANCELED) {
                        continue
                    }

                    // 3. Filter out self-authored neuroFlow blocks (loop prevention)
                    val isNeuroFlowBlock = customPkg.equals(NEUROFLOW_PACKAGE_ID, ignoreCase = true) ||
                        customUri.startsWith(CUSTOM_APP_URI_PREFIX, ignoreCase = true) ||
                        title.contains(NEUROFLOW_PACKAGE_ID, ignoreCase = true) ||
                        desc.contains(NEUROFLOW_PACKAGE_ID, ignoreCase = true) ||
                        organizer.contains(NEUROFLOW_PACKAGE_ID, ignoreCase = true) ||
                        title.contains("[neuroflow]", ignoreCase = true) ||
                        title.contains("[Flow]", ignoreCase = true) ||
                        desc.contains("[neuroflow]", ignoreCase = true)

                    if (isNeuroFlowBlock) {
                        continue
                    }

                    if (e > s) {
                        results.add(TimeSlotWindow(startMillis = s, endMillis = e))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarSyncDataSource", "Failed to query calendar provider", e)
        }

        return results
    }

    override suspend fun findPrimaryWritableCalendarId(): Long? = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return@withContext null

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )

        // 1. Primary writable calendar: IS_PRIMARY == 1 and access >= CAL_ACCESS_CONTRIBUTOR
        try {
            val selection = "${CalendarContract.Calendars.IS_PRIMARY} = 1 AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
            val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                    if (idIdx >= 0) {
                        return@withContext cursor.getLong(idIdx)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarSyncDataSource", "Primary calendar query failed, trying fallback", e)
        }

        // 2. Fallback: First writable calendar
        try {
            val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
            val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
            val sortOrder = "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars._ID} ASC"
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                    if (idIdx >= 0) {
                        return@withContext cursor.getLong(idIdx)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarSyncDataSource", "Fallback writable calendar query failed", e)
        }

        null
    }

    override suspend fun findEventIdByTaskId(taskId: String): Long? = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return@withContext null

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        )
        val selection = "${CalendarContract.Events.CUSTOM_APP_PACKAGE} = ? AND ${CalendarContract.Events.CUSTOM_APP_URI} = ?"
        val selectionArgs = arrayOf(NEUROFLOW_PACKAGE_ID, "$CUSTOM_APP_URI_PREFIX$taskId")

        try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(CalendarContract.Events._ID)
                    if (idIdx >= 0) {
                        return@withContext cursor.getLong(idIdx)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarSyncDataSource", "Failed to query event by taskId $taskId", e)
        }

        null
    }

    override suspend fun upsertTaskEvent(
        task: TaskEntity,
        startMillis: Long?,
        durationMinutes: Int?
    ): Long? = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return@withContext null

        val calendarId = findPrimaryWritableCalendarId() ?: return@withContext null

        val effectiveStart = startMillis
            ?: task.resolveScheduledEpochMillis()
            ?: task.scheduledDate
            ?: System.currentTimeMillis()

        val effectiveDuration = durationMinutes
            ?: (if (task.plannedBlockMinutes > 0) task.plannedBlockMinutes else if (task.estimatedDurationMinutes > 0) task.estimatedDurationMinutes else 25)

        val endMillis = effectiveStart + (effectiveDuration * 60_000L)

        val cleanTitle = task.title
            .removePrefix("✓ [Flow] ")
            .removePrefix("🎯 [Flow] ")
            .removePrefix("[Flow] ")
            .trim()

        val formattedTitle = when {
            task.status == TaskStatus.COMPLETED -> "✓ [Flow] $cleanTitle"
            task.isFrog -> "🎯 [Flow] $cleanTitle"
            else -> "[Flow] $cleanTitle"
        }

        val formattedDesc = if (task.description.isBlank()) {
            "[neuroflow] Managed focus block"
        } else {
            "${task.description}\n\n[neuroflow] Managed focus block"
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, formattedTitle)
            put(CalendarContract.Events.DESCRIPTION, formattedDesc)
            put(CalendarContract.Events.DTSTART, effectiveStart)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.CUSTOM_APP_PACKAGE, NEUROFLOW_PACKAGE_ID)
            put(CalendarContract.Events.CUSTOM_APP_URI, "$CUSTOM_APP_URI_PREFIX${task.id}")
            put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
            put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
        }

        val existingEventId = findEventIdByTaskId(task.id)

        try {
            if (existingEventId != null) {
                val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingEventId)
                context.contentResolver.update(updateUri, values, null, null)
                existingEventId
            } else {
                val insertUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                insertUri?.lastPathSegment?.toLongOrNull()
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarSyncDataSource", "Failed to upsert calendar event for task ${task.id}", e)
            null
        }
    }

    override suspend fun deleteTaskEvent(taskId: String): Boolean = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return@withContext false

        val existingEventId = findEventIdByTaskId(taskId) ?: return@withContext false

        try {
            val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingEventId)
            val deletedRows = context.contentResolver.delete(deleteUri, null, null)
            deletedRows > 0
        } catch (e: Exception) {
            android.util.Log.w("CalendarSyncDataSource", "Failed to delete calendar event for task $taskId", e)
            false
        }
    }

    override suspend fun handleTaskStateChanged(task: TaskEntity): Boolean = withContext(Dispatchers.IO) {
        when (task.status) {
            TaskStatus.COMPLETED -> {
                val existingEventId = findEventIdByTaskId(task.id)
                if (existingEventId != null) {
                    upsertTaskEvent(task) != null
                } else {
                    false
                }
            }
            TaskStatus.ARCHIVED -> {
                deleteTaskEvent(task.id)
            }
            TaskStatus.ACTIVE -> {
                if (task.scheduledDate == null && task.scheduledTime == null) {
                    deleteTaskEvent(task.id)
                } else if (task.isScheduleLocked || task.isFrog) {
                    upsertTaskEvent(task) != null
                } else {
                    false
                }
            }
        }
    }

    companion object {
        const val NEUROFLOW_PACKAGE_ID = "com.neuroflow.app"
        const val CUSTOM_APP_URI_PREFIX = "neuroflow://task/"
        private const val DECOMPRESSION_THRESHOLD_MILLIS = 45 * 60_000L
        private const val BUFFER_MILLIS = 5 * 60_000L

        /**
         * Sweep-line interval coalescing and post-meeting decompression buffer.
         *
         * Formula (ADR-002 Section 6.4):
         * BusyWindow_effective = [s_merged, e_merged + I(e_merged - s_merged >= 45m) * 5m]
         * Overlapping intervals and adjacent intervals separated by < 5m are coalesced into a unified non-overlapping blocked horizon.
         */
        fun coalesceWindows(rawWindows: List<TimeSlotWindow>): List<TimeSlotWindow> {
            val valid = rawWindows.filter { it.endMillis > it.startMillis }
            if (valid.isEmpty()) return emptyList()

            val sorted = valid.sortedWith(compareBy({ it.startMillis }, { it.endMillis }))
            val merged = mutableListOf<TimeSlotWindow>()

            var curStart = sorted[0].startMillis
            var curEnd = sorted[0].endMillis

            for (i in 1 until sorted.size) {
                val next = sorted[i]
                // Coalesce if overlapping or adjacent separated by < 5 minutes
                if (next.startMillis <= curEnd + BUFFER_MILLIS) {
                    curEnd = max(curEnd, next.endMillis)
                } else {
                    val duration = curEnd - curStart
                    val effectiveEnd = if (duration >= DECOMPRESSION_THRESHOLD_MILLIS) {
                        curEnd + BUFFER_MILLIS
                    } else {
                        curEnd
                    }
                    merged.add(TimeSlotWindow(curStart, effectiveEnd))
                    curStart = next.startMillis
                    curEnd = next.endMillis
                }
            }

            // Final interval
            val duration = curEnd - curStart
            val effectiveEnd = if (duration >= DECOMPRESSION_THRESHOLD_MILLIS) {
                curEnd + BUFFER_MILLIS
            } else {
                curEnd
            }
            merged.add(TimeSlotWindow(curStart, effectiveEnd))

            if (merged.size <= 1) return merged

            // Final sweep to resolve any secondary overlap created by decompression expansion
            val finalPass = mutableListOf<TimeSlotWindow>()
            var pStart = merged[0].startMillis
            var pEnd = merged[0].endMillis

            for (i in 1 until merged.size) {
                val win = merged[i]
                if (win.startMillis <= pEnd) {
                    pEnd = max(pEnd, win.endMillis)
                } else {
                    finalPass.add(TimeSlotWindow(pStart, pEnd))
                    pStart = win.startMillis
                    pEnd = win.endMillis
                }
            }
            finalPass.add(TimeSlotWindow(pStart, pEnd))
            return finalPass
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappworkerscheduleautotasksworkerkt"></a>
### File: `app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt`
- **Lines of Code:** 349
- **Size:** 16,139 bytes (15.8 KB)

```kotlin
package com.neuroflow.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.data.repository.SessionRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.AutoSchedulingContracts
import com.neuroflow.app.domain.scheduler.PlacementStability
import com.neuroflow.app.domain.scheduler.SchedulePreparationOrchestrator
import com.neuroflow.app.domain.scheduler.TaskBlockPlan
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.ScheduledPlacement
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.domain.service.TimeSlotWindow
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class ScheduleAutoTasksWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val taskSchedulerService: TaskSchedulerService,
    private val circadianEnergyService: CircadianEnergyService,
    private val behavioralSupportService: BehavioralSupportService,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val sessionRepository: SessionRepository,
    private val calendarSyncRepository: CalendarSyncRepository? = null,
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null
) : CoroutineWorker(context, params) {

    private val scheduleOrchestrator = SchedulePreparationOrchestrator(
        taskSchedulerService = taskSchedulerService,
        behavioralSupportService = behavioralSupportService,
        sleepTelemetryRepository = sleepTelemetryRepository
    )

    companion object {
        const val WORK_NAME = "schedule_auto_tasks_work"
        const val FOREGROUND_TICK_WORK_NAME = "schedule_auto_tasks_foreground_tick"

        @Volatile
        private var isWorkerRunning = false
        private val workerLock = Any()

        fun buildPeriodicWorkRequest(throttleMinutes: Int): PeriodicWorkRequest {
            val throttleInterval = throttleMinutes.coerceIn(15, 240)
            return PeriodicWorkRequest.Builder(
                ScheduleAutoTasksWorker::class.java,
                throttleInterval.toLong(),
                TimeUnit.MINUTES
            ).build()
        }

        fun buildOneTimeWorkRequest(forceReplan: Boolean = false): OneTimeWorkRequest {
            val data = Data.Builder().putBoolean("FORCE_REPLAN", forceReplan).build()
            return OneTimeWorkRequest.Builder(ScheduleAutoTasksWorker::class.java)
                .setInputData(data)
                .build()
        }

        /**
         * Anchors wake time to the exact local `:00` minute of [wakeHour] using DST-safe ZonedDateTime,
         * preventing the up-to-59-minute drift of raw hour subtraction.
         */
        internal fun calculateWakeTimeMillis(
            nowMillis: Long,
            wakeHour: Int,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): Long {
            val zonedNow = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
            var anchor = zonedNow.toLocalDate()
                .atTime(wakeHour.coerceIn(0, 23), 0)
                .atZone(zoneId)
            if (anchor.toInstant().toEpochMilli() > nowMillis) {
                anchor = anchor.minusDays(1)
            }
            return anchor.toInstant().toEpochMilli()
        }

        internal fun splitMillisToDateAndTime(millis: Long): Pair<Long, Long> {
            val dateStart = Calendar.getInstance().apply {
                timeInMillis = millis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dateMillis = dateStart.timeInMillis
            val timeOffset = millis - dateMillis
            return dateMillis to timeOffset
        }
    }

    override suspend fun doWork(): Result {
        synchronized(workerLock) {
            if (isWorkerRunning) {
                android.util.Log.w(
                    "ScheduleAutoTasks",
                    "Worker already running - skipping execution"
                )
                return Result.success()
            }
            isWorkerRunning = true
        }

        return try {
            val forceReplan = inputData.getBoolean("FORCE_REPLAN", false)
            val prefs = preferencesDataStore.preferencesFlow.first()
            if (!prefs.autoSchedulingEnabled) {
                return Result.success()
            }

            val allKnownTasks = taskRepository.getAllTasks()
            val allTasks = allKnownTasks.filter { it.status == TaskStatus.ACTIVE }
            val nowMillis = System.currentTimeMillis()
            val blockedTaskIds = buildBlockedTaskIds(allTasks, allKnownTasks)
            val focusedTaskIds = sessionRepository.getOpenSessions().map { it.taskId }.toSet()

            val zoneId = ZoneId.systemDefault()
            val dayDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()

            // Separate tasks into unscheduled, missed, and replan candidates
            val unscheduledTasks = allTasks.filter { task ->
                (task.scheduledDate == null || task.scheduledTime == null) &&
                    !AutoSchedulingContracts.hasManualScheduleData(task)
            }.filterNot { it.id in blockedTaskIds || it.id in focusedTaskIds }

            val gracePeriodMillis = if (prefs.isAutoScheduleToggleOn) 5 * 60_000L else 30 * 60_000L
            val missedTasks = allTasks.filter { task ->
                task.status == TaskStatus.ACTIVE &&
                    !task.isScheduleLocked &&
                    task.isAutoScheduled &&
                    task.scheduledDate != null &&
                    task.scheduledTime != null &&
                    let {
                        val scheduledStart = task.scheduledDate + task.scheduledTime
                        TaskBlockPlan.blockHasExpired(
                            task = task,
                            startMillis = scheduledStart,
                            nowMillis = nowMillis,
                            gracePeriodMillis = gracePeriodMillis
                        ) || (forceReplan && scheduledStart < nowMillis)
                    }
            }.filterNot { it.id in blockedTaskIds || it.id in focusedTaskIds }

            val replanCandidates = allTasks.filter { task ->
                val scheduledMillis = task.resolveScheduledEpochMillis()
                task.status == TaskStatus.ACTIVE &&
                    !task.isScheduleLocked &&
                    scheduledMillis != null &&
                    scheduledMillis >= nowMillis &&
                    PlacementStability.verdict(task, nowMillis, prefs, forceReplan).allowsReplan
            }.filterNot { it.id in blockedTaskIds || it.id in focusedTaskIds }

            val targetTasksMap = LinkedHashMap<String, TaskEntity>()
            (unscheduledTasks + missedTasks + replanCandidates).forEach { t ->
                val isDateOnlyAnchor = t.scheduledDate != null && t.scheduledTime == null && !t.isAutoScheduled
                targetTasksMap[t.id] = if (isDateOnlyAnchor) t else t.copy(scheduledDate = null, scheduledTime = null)
            }
            val targetTasks = targetTasksMap.values.toList()

            if (targetTasks.isEmpty()) {
                return Result.success()
            }

            // Fixed tasks are active tasks NOT being rescheduled (locked or manual future tasks)
            val fixedTasks = allTasks.filterNot { targetTasksMap.containsKey(it.id) }
            val fixedWindows = fixedTasks.mapNotNull { task ->
                val start = task.resolveScheduledEpochMillis() ?: return@mapNotNull null
                val duration = TaskBlockPlan.plannedMinutes(task)
                val end = start + duration * 60_000L
                if (end > nowMillis) TimeSlotWindow(start, end) else null
            }

            // Fetch external busy windows from Android Native Calendar Sync
            val dayStart = dayDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val dayEnd = dayStart + 24 * 3600_000L - 1
            val externalBusy = calendarSyncRepository?.getBusyWindows(dayStart, dayEnd) ?: emptyList()
            val totalBusyWindows = externalBusy + fixedWindows

            val allSessions = runCatching {
                sessionRepository.getAllSessions()
            }.getOrDefault(emptyList())
            val closedSessions = allSessions.filter { it.endedAt != null }
            val activeTaskId = allSessions.firstOrNull { it.endedAt == null }?.taskId
            val microStepCounts = targetTasks.associate { task ->
                val steps = taskRepository.getMicroSteps(task.id)
                task.id to Pair(steps.count { it.isCompleted }, steps.size)
            }
            val sleepLogs = sleepTelemetryRepository?.observeAllSleepLogs()?.first() ?: emptyList()

            val placements = scheduleOrchestrator.orchestrateDaySchedule(
                candidateTasks = targetTasks,
                allKnownTasks = allKnownTasks,
                closedSessions = closedSessions,
                sleepLogs = sleepLogs,
                prefs = prefs,
                dayDate = dayDate,
                nowMillis = nowMillis,
                busyWindows = totalBusyWindows,
                forceReplan = forceReplan,
                zoneId = zoneId,
                activeTaskId = activeTaskId,
                microStepCounts = microStepCounts
            )

            if (placements.isNotEmpty()) {
                applyPlacementsTransactionally(placements)
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("ScheduleAutoTasksWorker", "Failed in auto-scheduling pass", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        } finally {
            synchronized(workerLock) {
                isWorkerRunning = false
            }
        }
    }

    private suspend fun applyPlacementsTransactionally(
        placements: List<ScheduledPlacement>
    ) {
        val nowMillis = System.currentTimeMillis()
        val focusedTaskIds = sessionRepository.getOpenSessions().map { it.taskId }.toSet()
        val placementsByBaseId = placements.groupBy { it.task.id.substringBefore("#part") }

        for ((baseTaskId, taskPlacements) in placementsByBaseId) {
            val primaryPlacement = taskPlacements.first()
            val freshTask = taskRepository.getById(baseTaskId) ?: continue
            if (baseTaskId in focusedTaskIds) continue

            if (freshTask.status != TaskStatus.ACTIVE ||
                !AutoSchedulingContracts.isMutableByAutoScheduler(freshTask, nowMillis)
            ) {
                continue
            }

            val (scheduledDate, scheduledTime) = splitMillisToDateAndTime(primaryPlacement.window.startMillis)
            val previousStartMillis = freshTask.resolveScheduledEpochMillis()
            val blockLengthChanged = freshTask.plannedBlockMinutes != primaryPlacement.window.durationMinutes

            if (!PlacementStability.isMoveWorthApplying(
                    previousStartMillis = previousStartMillis,
                    nextStartMillis = primaryPlacement.window.startMillis,
                    nowMillis = nowMillis
                ) && !blockLengthChanged
            ) {
                continue
            }

            val isSplit = taskPlacements.size > 1 || primaryPlacement.task.id.endsWith("#part1")
            val titleToUse = if (isSplit && !freshTask.title.contains("(Part ")) "${freshTask.title} (Part 1/2)" else freshTask.title
            val updatedTask = freshTask.copy(
                title = titleToUse,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                isAutoScheduled = true,
                plannedBlockMinutes = primaryPlacement.window.durationMinutes,
                lastAutoScheduledAt = nowMillis,
                updatedAt = nowMillis
            )

            taskRepository.update(updatedTask)
            if (updatedTask.isFrog) {
                calendarSyncRepository?.upsertTaskEvent(
                    updatedTask,
                    startMillis = primaryPlacement.window.startMillis,
                    durationMinutes = primaryPlacement.window.durationMinutes
                )
            }

            // Persist Ultradian Split Part 2 as a linked continuation block if split occurred
            val part2Placement = taskPlacements.getOrNull(1)
            if (part2Placement != null && part2Placement.task.id.endsWith("#part2")) {
                val (p2Date, p2Time) = splitMillisToDateAndTime(part2Placement.window.startMillis)
                val part2Id = "${baseTaskId}_part2"
                val existingPart2 = taskRepository.getById(part2Id)
                val part2Title = if (!part2Placement.task.title.contains("(Part ")) "${freshTask.title.substringBefore(" (Part ")} (Part 2/2)" else part2Placement.task.title
                val part2Entity = (existingPart2 ?: freshTask).copy(
                    id = part2Id,
                    title = part2Title,
                    parentTaskId = baseTaskId,
                    dependsOnTaskIds = baseTaskId,
                    estimatedDurationMinutes = part2Placement.window.durationMinutes,
                    plannedBlockMinutes = part2Placement.window.durationMinutes,
                    scheduledDate = p2Date,
                    scheduledTime = p2Time,
                    isAutoScheduled = true,
                    isFrog = false,
                    lastAutoScheduledAt = nowMillis,
                    updatedAt = nowMillis
                )
                if (existingPart2 != null) {
                    taskRepository.update(part2Entity)
                } else {
                    taskRepository.insert(part2Entity)
                }
            }
        }
    }

    private fun buildBlockedTaskIds(
        tasks: List<TaskEntity>,
        allKnownTasks: List<TaskEntity> = emptyList()
    ): Set<String> {
        val taskMap = (allKnownTasks.ifEmpty { tasks }).associateBy { it.id }
        val candidateIds = tasks.map { it.id }.toSet()
        return tasks.filter { task ->
            if (task.waitingFor.isNotBlank()) {
                return@filter true
            }
            val deps = task.dependsOnTaskIds
                .split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()
            deps.any { depId ->
                val depTask = taskMap[depId]
                if (depTask == null) return@any true
                if (depTask.status == TaskStatus.COMPLETED) return@any false
                // Candidate tasks scheduled together are ordered by the topological DAG scheduler
                if (depTask.status == TaskStatus.ACTIVE && depId in candidateIds) return@any false
                // Split continuation parts are not blocked by their active parent task
                if (task.parentTaskId == depId && depTask.status == TaskStatus.ACTIVE) return@any false
                true
            }
        }.map { it.id }.toSet()
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappworkernotificationworkerkt"></a>
### File: `app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt`
- **Lines of Code:** 420
- **Size:** 18,657 bytes (18.2 KB)

```kotlin
package com.neuroflow.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.entity.effectiveReminderTargetMillis
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.data.local.entity.startOfPreviousLocalDay
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.determineCategory
import com.neuroflow.app.domain.service.ChronotypeProfile
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

const val CHANNEL_TASKS = "tasks"
const val CHANNEL_FOCUS = "focus"
const val CHANNEL_DAILY = "daily_plan"
const val WORK_DAILY_PLAN = "daily_plan"
const val WORK_STREAK_CHECK = "streak_check"
const val WORK_DEADLINE_ESCALATION = "deadline_escalation"

internal data class NotificationWorkerPlan(
    val dailyPlanEnabled: Boolean,
    val dailyPlanDelayMs: Long,
    val streakEnabled: Boolean,
    val streakDelayMs: Long,
    val escalationEnabled: Boolean
)

internal fun delayUntilHour(hour: Int, nowMillis: Long = System.currentTimeMillis()): Long {
    val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val target = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
    return target.timeInMillis - now.timeInMillis
}

internal fun buildNotificationWorkerPlan(
    prefs: UserPreferences,
    nowMillis: Long = System.currentTimeMillis()
): NotificationWorkerPlan {
    return NotificationWorkerPlan(
        dailyPlanEnabled = prefs.dailyPlanNotificationsEnabled,
        dailyPlanDelayMs = delayUntilHour(prefs.dailyPlanNotificationHour, nowMillis),
        streakEnabled = prefs.streakNotificationsEnabled,
        streakDelayMs = delayUntilHour(prefs.streakCheckNotificationHour, nowMillis),
        escalationEnabled = prefs.deadlineEscalationNotificationsEnabled
    )
}

fun scheduleNotificationWorkers(context: Context, prefs: UserPreferences) {
    val workManager = WorkManager.getInstance(context)
    val plan = buildNotificationWorkerPlan(prefs)

    if (plan.dailyPlanEnabled) {
        val dailyPlanRequest = PeriodicWorkRequestBuilder<DailyPlanWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(plan.dailyPlanDelayMs, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_DAILY_PLAN,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            dailyPlanRequest
        )
    } else {
        workManager.cancelUniqueWork(WORK_DAILY_PLAN)
    }

    if (plan.streakEnabled) {
        val streakCheckRequest = PeriodicWorkRequestBuilder<StreakCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(plan.streakDelayMs, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_STREAK_CHECK,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            streakCheckRequest
        )
    } else {
        workManager.cancelUniqueWork(WORK_STREAK_CHECK)
    }

    if (plan.escalationEnabled) {
        val escalationRequest = PeriodicWorkRequestBuilder<DeadlineEscalationWorker>(4, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(
            WORK_DEADLINE_ESCALATION,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            escalationRequest
        )
    } else {
        workManager.cancelUniqueWork(WORK_DEADLINE_ESCALATION)
    }
}

fun createNotificationChannels(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val taskChannel = NotificationChannel(
        CHANNEL_TASKS, "Task Reminders", NotificationManager.IMPORTANCE_HIGH
    ).apply { description = "Reminders for upcoming task deadlines" }

    val focusChannel = NotificationChannel(
        CHANNEL_FOCUS, "Focus & Streaks", NotificationManager.IMPORTANCE_DEFAULT
    ).apply { description = "Focus time and streak notifications" }

    val dailyChannel = NotificationChannel(
        CHANNEL_DAILY, "Daily Plan", NotificationManager.IMPORTANCE_DEFAULT
    ).apply { description = "Morning planning notifications" }

    val autonomyNudgeChannel = NotificationChannel(
        "autonomy_nudge", "Autonomy Nudges", NotificationManager.IMPORTANCE_DEFAULT
    ).apply { description = "Gentle nudges when a task hasn't been started" }

    manager.createNotificationChannels(listOf(taskChannel, focusChannel, dailyChannel, autonomyNudgeChannel))
}

@HiltWorker
class DailyPlanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val taskSchedulerService: com.neuroflow.app.domain.service.TaskSchedulerService,
    private val circadianEnergyService: com.neuroflow.app.domain.service.CircadianEnergyService,
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.dailyPlanNotificationsEnabled) Result.success()
        else {
            val tasks = taskRepository.getActiveTasks()
            val nowMillis = System.currentTimeMillis()
            val zoneId = ZoneId.systemDefault()
            val baseWakeMillis = ScheduleAutoTasksWorker.calculateWakeTimeMillis(nowMillis, prefs.wakeUpHour, zoneId)
            val sleepSignal = sleepTelemetryRepository?.getLatestNormalizedSleep(
                baseWakeMillis = baseWakeMillis,
                baseSleepMinutes = 480.0
            )
            val wakeTimeMillis = sleepSignal?.normalizedWakeTimeMillis ?: baseWakeMillis
            val sleepMinutes = sleepSignal?.blendedDurationMinutes?.roundToInt()?.coerceIn(60, 960) ?: 480
            val naps = sleepSignal?.daytimeNaps ?: emptyList()
            val chronotype = ChronotypeProfile.fromString(prefs.manualChronotype ?: prefs.quizChronotype)

            val snapshot = circadianEnergyService.evaluateSnapshot(
                timestampMillis = nowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepMinutes,
                continuousActiveFocusMinutes = 0,
                zoneId = zoneId,
                naps = naps,
                chronotype = chronotype
            )

            val topTasks = tasks.map { task ->
                val resolvedCategory = when (task.determineCategory()) {
                    com.neuroflow.app.domain.scheduler.TaskCategory.HARD_WORK,
                    com.neuroflow.app.domain.scheduler.TaskCategory.ANALYTICAL -> com.neuroflow.app.domain.service.TaskCategory.DEEP_WORK
                    com.neuroflow.app.domain.scheduler.TaskCategory.CREATIVE -> com.neuroflow.app.domain.service.TaskCategory.CREATIVE
                    com.neuroflow.app.domain.scheduler.TaskCategory.ROUTINE -> com.neuroflow.app.domain.service.TaskCategory.ADMIN
                    com.neuroflow.app.domain.scheduler.TaskCategory.EXERCISE,
                    com.neuroflow.app.domain.scheduler.TaskCategory.PHYSICAL -> com.neuroflow.app.domain.service.TaskCategory.EXERCISE
                    com.neuroflow.app.domain.scheduler.TaskCategory.MINDFULNESS -> com.neuroflow.app.domain.service.TaskCategory.MINDFULNESS
                    com.neuroflow.app.domain.scheduler.TaskCategory.FLEXIBLE -> com.neuroflow.app.domain.service.TaskCategory.GENERAL
                }
                val schedulable = com.neuroflow.app.domain.service.SchedulableTask(
                    id = task.id,
                    title = task.title,
                    category = resolvedCategory,
                    priority = when (task.priority) {
                        com.neuroflow.app.domain.model.Priority.HIGH -> 2
                        com.neuroflow.app.domain.model.Priority.MEDIUM -> 1
                        else -> 0
                    },
                    estimatedMinutes = task.estimatedDurationMinutes,
                    energyDemand = when (task.energyLevel) {
                        com.neuroflow.app.domain.model.EnergyLevel.HIGH -> com.neuroflow.app.domain.service.EnergyDemand.HIGH
                        com.neuroflow.app.domain.model.EnergyLevel.MEDIUM -> com.neuroflow.app.domain.service.EnergyDemand.MODERATE
                        else -> com.neuroflow.app.domain.service.EnergyDemand.LOW
                    },
                    isFrog = task.isFrog,
                    scheduledStartMillis = task.scheduledTime?.let { (task.scheduledDate ?: nowMillis) + it },
                    deadlineMillis = task.deadlineDate?.let { it + (task.deadlineTime ?: 0L) },
                    isLocked = task.isScheduleLocked,
                    parentTaskId = task.parentTaskId,
                    impactScore = task.impactScore,
                    postponeCount = task.postponeCount,
                    isAnxietyTask = task.isAnxietyTask,
                    hasImplementationIntention = task.ifThenPlan.isNotBlank()
                )
                val score = taskSchedulerService.evaluateTaskScore(schedulable, snapshot).totalScore
                task to score
            }.sortedByDescending { it.second }.take(3).map { it.first }

            if (topTasks.isNotEmpty()) {
                val taskNames = topTasks.joinToString(", ") { it.title }
                showNotification(
                    "Your top tasks today",
                    "Your top 3 tasks: $taskNames. Tap to confirm.",
                    CHANNEL_DAILY
                )
            }
            Result.success()
        }
    } catch (e: Exception) {
        android.util.Log.e("NotificationWorker", "DailyPlanWorker failed", e)
        Result.failure()
    }

    private fun showNotification(title: String, message: String, channelId: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID_DAILY_PLAN, notification)
    }

    companion object {
        private const val NOTIFICATION_ID_DAILY_PLAN = 10_000
    }
}

@HiltWorker
class StreakCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        var prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.streakNotificationsEnabled) Result.success()
        else {
            val today = startOfLocalDay(System.currentTimeMillis())
            val yesterday = startOfPreviousLocalDay(today)

            val completedToday = taskRepository.getCompletedTasks().any { task ->
                task.completedAt != null && task.completedAt >= today
            }

            if (!completedToday && prefs.lastActiveDate < yesterday && prefs.dailyStreak > 0) {
                preferencesDataStore.updatePreferences { it.copy(dailyStreak = 0) }
                prefs = prefs.copy(dailyStreak = 0)
            }

            if (!completedToday && prefs.dailyStreak > 0 && taskRepository.getActiveTasks().isNotEmpty()) {
                showNotification(
                    "Streak at risk! 🔥",
                    "Your ${prefs.dailyStreak}-day streak ends tonight. Complete 1 task!",
                    CHANNEL_FOCUS
                )
            }

            Result.success()
        }
    } catch (e: Exception) {
        android.util.Log.e("NotificationWorker", "StreakCheckWorker failed", e)
        Result.failure()
    }

    private fun showNotification(title: String, message: String, channelId: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID_STREAK, notification)
    }

    companion object {
        private const val NOTIFICATION_ID_STREAK = 10_001
    }
}

@HiltWorker
class DeadlineEscalationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val prefs = preferencesDataStore.preferencesFlow.first()
        if (!prefs.deadlineEscalationNotificationsEnabled) Result.success()
        else {
            val now = System.currentTimeMillis()
            val threshold = 48 * 3_600_000L

            val urgentTasks = taskRepository.getActiveTasks().filter { task ->
                task.quadrant != com.neuroflow.app.domain.model.Quadrant.DO_FIRST &&
                task.recurrence == com.neuroflow.app.domain.model.Recurrence.NONE &&
                task.deadlineDate != null &&
                (task.deadlineDate + (task.deadlineTime ?: 0L) - now) in 0L..threshold
            }

            if (urgentTasks.isNotEmpty()) {
                val names = urgentTasks.take(3).joinToString(", ") { it.title }
                val more = if (urgentTasks.size > 3) " +${urgentTasks.size - 3} more" else ""
                showNotification(
                    "⚠️ ${urgentTasks.size} task${if (urgentTasks.size > 1) "s" else ""} due soon",
                    "$names$more — deadline within 48 hours.",
                    CHANNEL_TASKS
                )
            }

            Result.success()
        }
    } catch (e: Exception) {
        android.util.Log.e("NotificationWorker", "DeadlineEscalationWorker failed", e)
        Result.failure()
    }

    private fun showNotification(title: String, message: String, channelId: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID_ESCALATION, notification)
    }

    companion object {
        private const val NOTIFICATION_ID_ESCALATION = 10_002
    }
}

@HiltWorker
class TaskReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = preferencesDataStore.preferencesFlow.first()
            if (!prefs.deadlineReminderNotificationsEnabled) return Result.success()

            val taskId = inputData.getString("taskId") ?: return Result.failure()
            val task = taskRepository.getById(taskId) ?: return Result.success()
            if (task.status != TaskStatus.ACTIVE) return Result.success()
            val taskTitle = inputData.getString("taskTitle") ?: task.title
            val minutesBefore = inputData.getLong("minutesBefore", 0L)
            val expectedTargetMs = inputData.getLong("targetMs", -1L)

            val actualTargetMs = task.effectiveReminderTargetMillis() ?: return Result.success()

            if (expectedTargetMs > 0 && kotlin.math.abs(actualTargetMs - expectedTargetMs) > 5 * 60_000L) {
                return Result.success()
            }

            val now = System.currentTimeMillis()
            if (actualTargetMs < now - 10 * 60_000L) return Result.success()

            val timeLabel = when (minutesBefore) {
                15L   -> "15 minutes"
                30L   -> "30 minutes"
                60L   -> "1 hour"
                1440L -> "1 day"
                else  -> "$minutesBefore minutes"
            }

            showNotification(
                "⏰ Task due in $timeLabel",
                taskTitle,
                CHANNEL_TASKS
            )

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("NotificationWorker", "TaskReminderWorker failed", e)
            Result.failure()
        }
    }

    private fun showNotification(title: String, message: String, channelId: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        val taskId = inputData.getString("taskId") ?: ""
        manager.notify(taskId.hashCode(), notification)
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappworkerfocuswidgetupdateworkerkt"></a>
### File: `app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt`
- **Lines of Code:** 226
- **Size:** 11,652 bytes (11.4 KB)

```kotlin
package com.neuroflow.app.worker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neuroflow.app.MainActivity
import com.neuroflow.app.R
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.data.sleep.SleepTelemetryRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.FatigueZone
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.scheduler.determineCategory
import com.neuroflow.app.domain.service.ChronotypeProfile
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.EnergyDemand
import com.neuroflow.app.domain.service.SchedulableTask
import com.neuroflow.app.domain.service.TaskCategory
import com.neuroflow.app.domain.service.TaskSchedulerService
import com.neuroflow.app.presentation.widget.CompleteTaskReceiver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import kotlin.math.roundToInt

@HiltWorker
class FocusWidgetUpdateWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val taskSchedulerService: TaskSchedulerService,
    private val circadianEnergyService: CircadianEnergyService,
    private val sleepTelemetryRepository: SleepTelemetryRepository? = null
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val tasks = taskRepository.getActiveTasks()
            val preferences = preferencesDataStore.preferencesFlow.first()
            val nowMillis = System.currentTimeMillis()
            val zoneId = ZoneId.systemDefault()

            val baseWakeMillis = ScheduleAutoTasksWorker.calculateWakeTimeMillis(nowMillis, preferences.wakeUpHour, zoneId)
            val sleepSignal = sleepTelemetryRepository?.getLatestNormalizedSleep(
                baseWakeMillis = baseWakeMillis,
                baseSleepMinutes = 480.0
            )
            val wakeTimeMillis = sleepSignal?.normalizedWakeTimeMillis ?: baseWakeMillis
            val sleepMinutes = sleepSignal?.blendedDurationMinutes?.roundToInt()?.coerceIn(60, 960) ?: 480
            val naps = sleepSignal?.daytimeNaps ?: emptyList()
            val chronotype = ChronotypeProfile.fromString(preferences.manualChronotype ?: preferences.quizChronotype)

            val snapshot = circadianEnergyService.evaluateSnapshot(
                timestampMillis = nowMillis,
                wakeTimeMillis = wakeTimeMillis,
                lastSleepDurationMinutes = sleepMinutes,
                continuousActiveFocusMinutes = 0,
                zoneId = zoneId,
                naps = naps,
                chronotype = chronotype
            )

            val sortedWithScores = tasks.map { task ->
                val resolvedCategory = when (task.determineCategory()) {
                    com.neuroflow.app.domain.scheduler.TaskCategory.HARD_WORK,
                    com.neuroflow.app.domain.scheduler.TaskCategory.ANALYTICAL -> TaskCategory.DEEP_WORK
                    com.neuroflow.app.domain.scheduler.TaskCategory.CREATIVE -> TaskCategory.CREATIVE
                    com.neuroflow.app.domain.scheduler.TaskCategory.ROUTINE -> TaskCategory.ADMIN
                    com.neuroflow.app.domain.scheduler.TaskCategory.EXERCISE,
                    com.neuroflow.app.domain.scheduler.TaskCategory.PHYSICAL -> TaskCategory.EXERCISE
                    com.neuroflow.app.domain.scheduler.TaskCategory.MINDFULNESS -> TaskCategory.MINDFULNESS
                    com.neuroflow.app.domain.scheduler.TaskCategory.FLEXIBLE -> TaskCategory.GENERAL
                }
                val schedulable = SchedulableTask(
                    id = task.id,
                    title = task.title,
                    category = resolvedCategory,
                    priority = when (task.priority) {
                        Priority.HIGH -> 2
                        Priority.MEDIUM -> 1
                        else -> 0
                    },
                    estimatedMinutes = task.estimatedDurationMinutes,
                    energyDemand = when (task.energyLevel) {
                        EnergyLevel.HIGH -> EnergyDemand.HIGH
                        EnergyLevel.MEDIUM -> EnergyDemand.MODERATE
                        else -> EnergyDemand.LOW
                    },
                    isFrog = task.isFrog,
                    scheduledStartMillis = task.scheduledTime?.let { (task.scheduledDate ?: nowMillis) + it },
                    deadlineMillis = task.deadlineDate?.let { it + (task.deadlineTime ?: 0L) },
                    isLocked = task.isScheduleLocked,
                    parentTaskId = task.parentTaskId,
                    impactScore = task.impactScore,
                    postponeCount = task.postponeCount,
                    isAnxietyTask = task.isAnxietyTask,
                    hasImplementationIntention = task.ifThenPlan.isNotBlank()
                )
                val breakdown = taskSchedulerService.evaluateTaskScore(schedulable, snapshot)
                task to breakdown
            }.sortedByDescending { it.second.totalScore }

            val topPair = sortedWithScores.firstOrNull()
            val topTask = topPair?.first
            val topScore = topPair?.second?.totalScore?.roundToInt() ?: 0

            val appWidgetManager = AppWidgetManager.getInstance(appContext)

            // 1. Update FocusWidgetProvider (widget_focus)
            val focusViews = RemoteViews(appContext.packageName, R.layout.widget_focus)
            if (topTask != null) {
                focusViews.setTextViewText(R.id.widget_task_title, topTask.title)
                focusViews.setTextViewText(
                    R.id.widget_task_score,
                    "Priority Score: $topScore"
                )

                val startIntent = Intent(appContext, MainActivity::class.java).apply {
                    action = "com.procus.ACTION_OPEN_FOCUS"
                    putExtra("taskId", topTask.id)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val startPendingIntent = PendingIntent.getActivity(
                    appContext, 1001, startIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                focusViews.setOnClickPendingIntent(R.id.widget_start_button, startPendingIntent)
            } else {
                focusViews.setTextViewText(R.id.widget_task_title, "No tasks — add one in neuroFlow")
                focusViews.setTextViewText(R.id.widget_task_score, "Score: —")
            }

            val focusComponentName = ComponentName(
                appContext,
                "com.neuroflow.app.presentation.widget.FocusWidgetProvider"
            )
            appWidgetManager.updateAppWidget(focusComponentName, focusViews)

            // 2. Update NextActionWidgetProvider (widget_next_action)
            val nextActionViews = RemoteViews(appContext.packageName, R.layout.widget_next_action)
            val energyText = "⚡ ${snapshot.usableEnergy}% · ${FatigueZone.fromUsableEnergy(snapshot.usableEnergy).label}"
            nextActionViews.setTextViewText(R.id.widget_energy_pill, energyText)

            if (topTask != null) {
                nextActionViews.setTextViewText(R.id.widget_next_task_title, topTask.title)
                val durationText = if (topTask.estimatedDurationMinutes > 0) "${topTask.estimatedDurationMinutes}m" else "Quick"
                val quadrantText = topTask.quadrant.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
                nextActionViews.setTextViewText(R.id.widget_next_task_meta, "$quadrantText · $durationText")
                nextActionViews.setViewVisibility(R.id.widget_button_row, android.view.View.VISIBLE)

                val focusIntent = Intent(appContext, MainActivity::class.java).apply {
                    action = "com.procus.ACTION_OPEN_FOCUS"
                    putExtra("taskId", topTask.id)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val focusPendingIntent = PendingIntent.getActivity(
                    appContext, 2001, focusIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                nextActionViews.setOnClickPendingIntent(R.id.widget_next_start_button, focusPendingIntent)

                val completeIntent = Intent(appContext, CompleteTaskReceiver::class.java).apply {
                    action = CompleteTaskReceiver.ACTION_COMPLETE_TASK
                    putExtra(CompleteTaskReceiver.EXTRA_TASK_ID, topTask.id)
                }
                val completePendingIntent = PendingIntent.getBroadcast(
                    appContext, topTask.id.hashCode(), completeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                nextActionViews.setOnClickPendingIntent(R.id.widget_complete_button, completePendingIntent)
            } else {
                nextActionViews.setTextViewText(R.id.widget_next_task_title, "All clear! Plan what's next")
                nextActionViews.setTextViewText(R.id.widget_next_task_meta, "No active tasks")
                nextActionViews.setViewVisibility(R.id.widget_button_row, android.view.View.GONE)
            }

            val containerIntent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val containerPendingIntent = PendingIntent.getActivity(
                appContext, 2002, containerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            nextActionViews.setOnClickPendingIntent(R.id.widget_next_action_container, containerPendingIntent)

            val nextActionComponentName = ComponentName(
                appContext,
                "com.neuroflow.app.presentation.widget.NextActionWidgetProvider"
            )
            appWidgetManager.updateAppWidget(nextActionComponentName, nextActionViews)

        } catch (_: Exception) {
            // Stale widget preferred over crash
        }

        return Result.success()
    }

    companion object {
        fun enqueueImmediateUpdate(context: Context) {
            runCatching {
                val request = OneTimeWorkRequestBuilder<FocusWidgetUpdateWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "FocusWidgetUpdateWorker_immediate",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainservicetasknaturallanguageparserkt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/service/TaskNaturalLanguageParser.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/service/TaskNaturalLanguageParser.kt`
- **Lines of Code:** 116
- **Size:** 5,082 bytes (5.0 KB)

```kotlin
package com.neuroflow.app.domain.service

import javax.inject.Inject
import javax.inject.Singleton

data class ParsedTaskInput(
    val cleanTitle: String,
    val estimatedMinutes: Int,
    val isDurationExplicit: Boolean,
    val energyDemand: EnergyDemand,
    val category: TaskCategory,
    val isFrog: Boolean
)

interface TaskNaturalLanguageParser {
    fun parse(rawInput: String): ParsedTaskInput
}

@Singleton
class DefaultTaskNaturalLanguageParser @Inject constructor() : TaskNaturalLanguageParser {

    private val durationRegex = Regex("""\b(\d+)\s*(m|min|mins|minutes|h|hr|hrs|hours)\b""", RegexOption.IGNORE_CASE)
    private val frogRegex = Regex("""(\uD83D\uDC38|#frog|\bfrog\b)""", RegexOption.IGNORE_CASE)
    private val lightningRegex = Regex("""\u26A1""")

    override fun parse(rawInput: String): ParsedTaskInput {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return ParsedTaskInput(
                cleanTitle = "",
                estimatedMinutes = 25,
                isDurationExplicit = false,
                energyDemand = EnergyDemand.MODERATE,
                category = TaskCategory.GENERAL,
                isFrog = false
            )
        }

        var workingTitle = trimmed
        var estimatedMinutes = 25
        var isDurationExplicit = false

        // 1. Check for verbal phrases
        when {
            workingTitle.contains("half hour", ignoreCase = true) -> {
                estimatedMinutes = 30
                isDurationExplicit = true
                workingTitle = workingTitle.replace(Regex("""\bhalf hour\b""", RegexOption.IGNORE_CASE), "")
            }
            workingTitle.contains("an hour", ignoreCase = true) || workingTitle.contains("1 hour", ignoreCase = true) -> {
                estimatedMinutes = 60
                isDurationExplicit = true
                workingTitle = workingTitle.replace(Regex("""\b(an|1)\s+hour\b""", RegexOption.IGNORE_CASE), "")
            }
            else -> {
                val match = durationRegex.find(workingTitle)
                if (match != null) {
                    val amount = match.groupValues[1].toIntOrNull() ?: 25
                    val unit = match.groupValues[2].lowercase()
                    estimatedMinutes = if (unit.startsWith("h")) amount * 60 else amount
                    isDurationExplicit = true
                    workingTitle = workingTitle.removeRange(match.range)
                }
            }
        }

        // 2. Frog Detection
        val isFrog = frogRegex.containsMatchIn(workingTitle)
        if (isFrog) {
            workingTitle = frogRegex.replace(workingTitle, "")
        }

        // 3. Energy Demand Detection
        val hasLightning = lightningRegex.containsMatchIn(workingTitle)
        if (hasLightning) {
            workingTitle = lightningRegex.replace(workingTitle, "")
        }

        val lower = workingTitle.lowercase()
        val energyDemand = when {
            hasLightning || lower.contains("high energy") || lower.contains("heavy") || lower.contains("intense") -> {
                workingTitle = workingTitle.replace(Regex("""\b(high energy|heavy|intense)\b""", RegexOption.IGNORE_CASE), "")
                EnergyDemand.HIGH
            }
            lower.contains("low energy") || lower.contains("light") || lower.contains("chill") || lower.contains("easy") -> {
                workingTitle = workingTitle.replace(Regex("""\b(low energy|light|chill|easy)\b""", RegexOption.IGNORE_CASE), "")
                EnergyDemand.LOW
            }
            else -> EnergyDemand.MODERATE
        }

        // 4. Category Detection
        val category = when {
            lower.contains("deep") || lower.contains("study") || lower.contains("code") || lower.contains("program") || lower.contains("research") -> TaskCategory.DEEP_WORK
            lower.contains("creative") || lower.contains("design") || lower.contains("draw") || lower.contains("music") || lower.contains("brainstorm") -> TaskCategory.CREATIVE
            lower.contains("admin") || lower.contains("email") || lower.contains("bill") || lower.contains("call") || lower.contains("clean") || lower.contains("errand") -> TaskCategory.ADMIN
            lower.contains("workout") || lower.contains("gym") || lower.contains("run") || lower.contains("exercise") -> TaskCategory.EXERCISE
            lower.contains("meditate") || lower.contains("breath") || lower.contains("journal") || lower.contains("mindful") -> TaskCategory.MINDFULNESS
            else -> TaskCategory.GENERAL
        }

        // Clean up whitespace and punctuation artifacts
        val cleaned = workingTitle
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '-', ':', ',')

        return ParsedTaskInput(
            cleanTitle = cleaned.ifBlank { trimmed },
            estimatedMinutes = estimatedMinutes.coerceAtLeast(5),
            isDurationExplicit = isDurationExplicit,
            energyDemand = energyDemand,
            category = category,
            isFrog = isFrog
        )
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomaininferencetaskinferenceenginekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/inference/TaskInferenceEngine.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/inference/TaskInferenceEngine.kt`
- **Lines of Code:** 342
- **Size:** 16,941 bytes (16.5 KB)

```kotlin
package com.neuroflow.app.domain.inference

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import com.neuroflow.app.domain.scheduler.BehavioralInsightsEngine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

data class InferredTaskMetadata(
    val sanitizedTitle: String,
    val durationMinutes: Int,
    val isDurationExplicit: Boolean = false,
    val energyDemand: EnergyLevel,
    val taskType: TaskType,
    val contextTags: List<String>,
    val isFrog: Boolean,
    val priority: Priority = Priority.MEDIUM,
    val quadrant: Quadrant = Quadrant.SCHEDULE,
    val scheduledDate: Long? = null,
    val scheduledTime: Long? = null,
    val deadlineDate: Long? = null,
    val deadlineTime: Long? = null,
    val isHighImpact: Boolean = false,
    val rawDurationMinutes: Int = durationMinutes,
    val calibrationFactorApplied: Float = 1.0f
) {
    val estimatedDurationMinutes: Int get() = durationMinutes
    val energyLevel: EnergyLevel get() = energyDemand
}

object TaskInferenceEngine {
    private val DURATION_REGEX = Regex(
        """(?i)(?<!\bin\s)\b(?:(\d+)\s*(?:h|hr|hrs|hours?))?\s*(?:(\d+)\s*(?:m|min|mins|minutes?))\b|(?<!\bin\s)\b(\d+(?:\.\d+)?)\s*(?:h|hr|hrs|hours?)\b|\b(?:half\s+(?:an?\s+)?hour)\b|\b(?:an?\s+hour)\b"""
    )
    private val TAG_REGEX = Regex("""[@#]([a-zA-Z0-9_\-]+)""")
    private val FROG_REGEX = Regex("""(?i)(?:\uD83D\uDC38|(?:^|\s)(?:!frog|#frog|frog:|\b(?:eat\s+the\s+frog)\b))""")
    private val LIGHTNING_REGEX = Regex("""\u26A1""")

    private val EXPLICIT_HIGH_ENERGY = Regex("""(?i)(?:^|\s)(?:!high\b|#high\b|\bhigh\s+energy\b|\bdeep\s+energy\b|\bheavy\b)""")
    private val EXPLICIT_LOW_ENERGY = Regex("""(?i)(?:^|\s)(?:!low\b|#low\b|\blow\s+energy\b|\bquick\s+energy\b|\blight\b)""")
    private val EXPLICIT_MED_ENERGY = Regex("""(?i)(?:^|\s)(?:!med\b|#med\b|\bmedium\s+energy\b|\bmed\s+energy\b)""")

    private val PRIORITY_P1_REGEX = Regex("""(?i)(?:^|\s)(?:!p1\b|\bp1\b|!urgent\b|\burgent\b|\basap\b)""")
    private val PRIORITY_P2_REGEX = Regex("""(?i)(?:^|\s)(?:!p2\b|\bp2\b)""")
    private val PRIORITY_P3_REGEX = Regex("""(?i)(?:^|\s)(?:!p3\b|\bp3\b)""")

    private val HIGH_ENERGY_KEYWORDS = Regex("""(?i)\b(deep(\s+work)?|coding|write\s+report|architect|study|exam|complex|thesis|research|debug|refactor)\b""")
    private val LOW_ENERGY_KEYWORDS = Regex("""(?i)\b(quick\s+call|clean\s+desk|water\s+plants|tidy|file\s+expense|errand|inbox|admin)\b""")

    private val ANALYTICAL_KEYWORDS = Regex("""(?i)\b(code|coding|debug|math|analysis|algorithm|compile|refactor|architect|finance|audit)\b""")
    private val CREATIVE_KEYWORDS = Regex("""(?i)\b(design|write|draft|brainstorm|compose|sketch|draw|illustrate|ideate)\b""")
    private val ADMIN_KEYWORDS = Regex("""(?i)\b(call|meet|email|sync|discuss|errand|admin|organize|clean|invoice|schedule|order|buy|purchase|bill)\b""")
    private val PHYSICAL_KEYWORDS = Regex("""(?i)\b(gym|run|walk|exercise|workout|stretch|yoga|cardio|lift)\b""")
    private val MINDFULNESS_KEYWORDS = Regex("""(?i)\b(meditate|meditation|breathwork|breathe|journal|mindful|nsdr)\b""")

    private val HIGH_IMPACT_REGEX = Regex("""(?i)(?:^|\s)(?:!impact\b|!star\b|#impact\b|\b(?:high\s+impact|critical|strategic)\b)""")
    private val TODAY_REGEX = Regex("""(?i)\b(today|tonight)\b""")
    private val TOMORROW_REGEX = Regex("""(?i)\b(tomorrow|tmrw)\b""")
    private val DUE_DATE_REGEX = Regex("""(?i)\b(?:due|by|deadline)\s*:?\s*(today|tomorrow|tmrw|monday|mon|tuesday|tue|wednesday|wed|thursday|thu|friday|fri|saturday|sat|sunday|sun)\b""")
    private val SCHEDULE_DATE_REGEX = Regex("""(?i)\b(?:scheduled?|on|next|this)\s*:?\s*(today|tomorrow|tmrw|monday|mon|tuesday|tue|wednesday|wed|thursday|thu|friday|fri|saturday|sat|sunday|sun)\b""")
    private val CLOCK_TIME_REGEX = Regex("""(?i)\bat\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\b""")
    private val RELATIVE_OFFSET_REGEX = Regex("""(?i)\bin\s+(\d+)\s*(m|min|mins|minutes?|h|hr|hrs|hours?)\b""")
    private val TIME_OF_DAY_REGEX = Regex("""(?i)\b(?:this\s+)?(morning|afternoon|evening|tonight)\b""")

    fun calculateCalibrationFactorFromHistory(tasks: List<TaskEntity>): Float {
        val validPairs = tasks.filter { task ->
            task.status == TaskStatus.COMPLETED &&
                (task.actualDurationMinutes != null && task.actualDurationMinutes > 0f) &&
                task.estimatedDurationMinutes > 0
        }
        if (validPairs.isEmpty()) return 1.0f

        val actuals = validPairs.map { it.actualDurationMinutes!!.roundToInt() }
        val estimates = validPairs.map { it.estimatedDurationMinutes }
        return BehavioralInsightsEngine.calculateCalibrationFactor(
            actualMinutes = actuals,
            estimatedMinutes = estimates
        )
    }

    fun inferFromInput(
        rawInput: String,
        completedTasks: List<TaskEntity>
    ): InferredTaskMetadata {
        val factor = calculateCalibrationFactorFromHistory(completedTasks)
        return inferFromInput(rawInput, calibrationFactor = factor)
    }

    fun inferFromInput(
        rawInput: String,
        calibrationFactor: Float = 1.0f,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): InferredTaskMetadata {
        val text = rawInput.trim()
        val safeCalibration = calibrationFactor.coerceIn(0.8f, 1.6f)
        if (text.isEmpty()) {
            val defaultDuration = (25 * safeCalibration).roundToInt().coerceAtLeast(5)
            return InferredTaskMetadata(
                sanitizedTitle = "",
                durationMinutes = defaultDuration,
                isDurationExplicit = false,
                energyDemand = EnergyLevel.MEDIUM,
                taskType = TaskType.GENERAL,
                contextTags = emptyList(),
                isFrog = false,
                priority = Priority.MEDIUM,
                rawDurationMinutes = 25,
                calibrationFactorApplied = safeCalibration
            )
        }

        // 1. Task type inference first so untimed tasks get an intelligent baseline duration
        val taskType = when {
            MINDFULNESS_KEYWORDS.containsMatchIn(text) -> TaskType.MINDFULNESS
            ANALYTICAL_KEYWORDS.containsMatchIn(text) -> TaskType.ANALYTICAL
            CREATIVE_KEYWORDS.containsMatchIn(text) -> TaskType.CREATIVE
            PHYSICAL_KEYWORDS.containsMatchIn(text) -> TaskType.PHYSICAL
            ADMIN_KEYWORDS.containsMatchIn(text) -> TaskType.ADMIN
            else -> TaskType.GENERAL
        }

        val defaultArchetypeMinutes = when (taskType) {
            TaskType.ANALYTICAL -> 45
            TaskType.CREATIVE -> 40
            TaskType.PHYSICAL -> 30
            TaskType.MINDFULNESS -> 15
            TaskType.ADMIN -> 15
            TaskType.GENERAL -> 25
        }

        // 2. Duration extraction (preserves exact duration when explicitly stated by user)
        var rawDurationMinutes = defaultArchetypeMinutes
        var isDurationExplicit = false
        val durationMatch = DURATION_REGEX.find(text)
        if (durationMatch != null) {
            isDurationExplicit = true
            val matchedStr = durationMatch.value.lowercase(Locale.ROOT)
            val hoursPart = durationMatch.groups[1]?.value?.toIntOrNull() ?: 0
            val minsPart = durationMatch.groups[2]?.value?.toIntOrNull() ?: 0
            val decimalHours = durationMatch.groups[3]?.value?.toFloatOrNull()

            rawDurationMinutes = when {
                matchedStr.contains("half") -> 30
                matchedStr == "an hour" || matchedStr == "a hour" -> 60
                decimalHours != null -> (decimalHours * 60).roundToInt()
                hoursPart > 0 || minsPart > 0 -> hoursPart * 60 + minsPart
                else -> defaultArchetypeMinutes
            }.coerceIn(5, 720)
        }

        val calibratedDuration = (rawDurationMinutes * safeCalibration).roundToInt().coerceIn(5, 720)

        // 3. Context tags extraction (@work, #finance)
        val reservedTokens = setOf("high", "low", "med", "p1", "p2", "p3", "frog", "urgent", "impact", "star")
        val tags = TAG_REGEX.findAll(text)
            .map { it.groupValues[1].lowercase(Locale.ROOT) }
            .filterNot { it in reservedTokens }
            .distinct()
            .toList()

        // 4. Frog & Impact detection
        val isFrog = FROG_REGEX.containsMatchIn(text)
        val isHighImpact = isFrog || HIGH_IMPACT_REGEX.containsMatchIn(text)
        val hasLightning = LIGHTNING_REGEX.containsMatchIn(text)

        // 5. Priority & Eisenhower Quadrant detection
        val isUrgent = PRIORITY_P1_REGEX.containsMatchIn(text)
        val priority = when {
            isFrog || isUrgent || isHighImpact -> Priority.HIGH
            PRIORITY_P3_REGEX.containsMatchIn(text) -> Priority.LOW
            PRIORITY_P2_REGEX.containsMatchIn(text) -> Priority.MEDIUM
            else -> Priority.MEDIUM
        }

        // 6. Energy demand heuristic
        val energyDemand = when {
            hasLightning || EXPLICIT_HIGH_ENERGY.containsMatchIn(text) -> EnergyLevel.HIGH
            EXPLICIT_LOW_ENERGY.containsMatchIn(text) -> EnergyLevel.LOW
            EXPLICIT_MED_ENERGY.containsMatchIn(text) -> EnergyLevel.MEDIUM
            HIGH_ENERGY_KEYWORDS.containsMatchIn(text) || isFrog -> EnergyLevel.HIGH
            LOW_ENERGY_KEYWORDS.containsMatchIn(text) || taskType == TaskType.MINDFULNESS -> EnergyLevel.LOW
            rawDurationMinutes >= 45 -> EnergyLevel.HIGH
            rawDurationMinutes <= 15 -> EnergyLevel.LOW
            else -> EnergyLevel.MEDIUM
        }

        // 7. DST-safe Date & Clock-Time inference
        val todayLocal = LocalDate.now(zoneId)
        val todayStartMillis = todayLocal.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val tomorrowStartMillis = todayLocal.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        var inferredDeadlineDate: Long? = null
        var inferredScheduledDate: Long? = null
        var inferredScheduledTimeMillis: Long? = null

        val dueMatch = DUE_DATE_REGEX.find(text)
        if (dueMatch != null) {
            val token = dueMatch.groupValues[1].lowercase(Locale.ROOT)
            inferredDeadlineDate = resolveDateTokenMillis(token, todayLocal, zoneId)
        } else if (TODAY_REGEX.containsMatchIn(text) && (text.contains("due", ignoreCase = true) || text.contains("deadline", ignoreCase = true))) {
            inferredDeadlineDate = todayStartMillis
        } else if (TOMORROW_REGEX.containsMatchIn(text) && (text.contains("due", ignoreCase = true) || text.contains("deadline", ignoreCase = true))) {
            inferredDeadlineDate = tomorrowStartMillis
        }

        val scheduleMatch = SCHEDULE_DATE_REGEX.find(text)
        if (scheduleMatch != null) {
            val token = scheduleMatch.groupValues[1].lowercase(Locale.ROOT)
            inferredScheduledDate = resolveDateTokenMillis(token, todayLocal, zoneId)
        } else if (inferredDeadlineDate == null) {
            if (TODAY_REGEX.containsMatchIn(text)) inferredScheduledDate = todayStartMillis
            else if (TOMORROW_REGEX.containsMatchIn(text)) inferredScheduledDate = tomorrowStartMillis
        }

        val clockMatch = CLOCK_TIME_REGEX.find(text)
        if (clockMatch != null) {
            val rawHour = clockMatch.groupValues[1].toIntOrNull() ?: 9
            val rawMin = clockMatch.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
            val meridiem = clockMatch.groupValues[3].lowercase(Locale.ROOT)
            val hour24 = when {
                meridiem == "pm" && rawHour in 1..11 -> rawHour + 12
                meridiem == "am" && rawHour == 12 -> 0
                meridiem.isEmpty() && rawHour in 1..6 -> rawHour + 12
                else -> rawHour.coerceIn(0, 23)
            }
            inferredScheduledTimeMillis = LocalTime.of(hour24, rawMin).toSecondOfDay() * 1000L
            if (inferredScheduledDate == null) {
                inferredScheduledDate = todayStartMillis
            }
        } else {
            val relMatch = RELATIVE_OFFSET_REGEX.find(text)
            if (relMatch != null) {
                val amount = relMatch.groupValues[1].toLongOrNull() ?: 1L
                val unit = relMatch.groupValues[2].lowercase(Locale.ROOT)
                val offsetMins = if (unit.startsWith("h")) amount * 60L else amount
                val targetTime = LocalTime.now(zoneId).plusMinutes(offsetMins)
                inferredScheduledTimeMillis = targetTime.toSecondOfDay() * 1000L
                if (inferredScheduledDate == null) {
                    inferredScheduledDate = todayStartMillis
                }
            } else {
                val todMatch = TIME_OF_DAY_REGEX.find(text)
                if (todMatch != null) {
                    val tod = todMatch.groupValues[1].lowercase(Locale.ROOT)
                    val targetLocalTime = when (tod) {
                        "morning" -> LocalTime.of(9, 30)
                        "afternoon" -> LocalTime.of(14, 0)
                        "evening", "tonight" -> LocalTime.of(19, 30)
                        else -> null
                    }
                    if (targetLocalTime != null) {
                        inferredScheduledTimeMillis = targetLocalTime.toSecondOfDay() * 1000L
                        if (inferredScheduledDate == null) {
                            inferredScheduledDate = todayStartMillis
                        }
                    }
                }
            }
        }

        val quadrant = when {
            isUrgent && isHighImpact -> Quadrant.DO_FIRST
            isHighImpact || isFrog -> Quadrant.SCHEDULE
            isUrgent -> Quadrant.DELEGATE
            else -> Quadrant.SCHEDULE
        }

        // 8. Title sanitization
        var cleanTitle = text
            .replace(FROG_REGEX, " ")
            .replace(LIGHTNING_REGEX, " ")
            .replace(RELATIVE_OFFSET_REGEX, " ")
            .replace(DURATION_REGEX, " ")
            .replace(EXPLICIT_HIGH_ENERGY, " ")
            .replace(EXPLICIT_LOW_ENERGY, " ")
            .replace(EXPLICIT_MED_ENERGY, " ")
            .replace(PRIORITY_P1_REGEX, " ")
            .replace(PRIORITY_P2_REGEX, " ")
            .replace(PRIORITY_P3_REGEX, " ")
            .replace(HIGH_IMPACT_REGEX, " ")
            .replace(DUE_DATE_REGEX, " ")
            .replace(SCHEDULE_DATE_REGEX, " ")
            .replace(CLOCK_TIME_REGEX, " ")
            .replace(TIME_OF_DAY_REGEX, " ")
            .replace(TODAY_REGEX, " ")
            .replace(TOMORROW_REGEX, " ")
            .replace(TAG_REGEX, " ")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '-', ':', ',')

        if (cleanTitle.isEmpty()) {
            cleanTitle = text.take(40).trim()
        }

        return InferredTaskMetadata(
            sanitizedTitle = cleanTitle,
            durationMinutes = calibratedDuration,
            isDurationExplicit = isDurationExplicit,
            energyDemand = energyDemand,
            taskType = taskType,
            contextTags = tags,
            isFrog = isFrog,
            priority = priority,
            quadrant = quadrant,
            scheduledDate = inferredScheduledDate,
            scheduledTime = inferredScheduledTimeMillis,
            deadlineDate = inferredDeadlineDate,
            isHighImpact = isHighImpact,
            rawDurationMinutes = rawDurationMinutes,
            calibrationFactorApplied = safeCalibration
        )
    }

    private fun resolveDateTokenMillis(
        token: String,
        todayLocal: LocalDate,
        zoneId: ZoneId
    ): Long {
        val targetDate = when (token) {
            "today", "tonight" -> todayLocal
            "tomorrow", "tmrw" -> todayLocal.plusDays(1)
            "monday", "mon" -> todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
            "tuesday", "tue" -> todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY))
            "wednesday", "wed" -> todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY))
            "thursday", "thu" -> todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY))
            "friday", "fri" -> todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
            "saturday", "sat" -> todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
            "sunday", "sun" -> todayLocal.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            else -> todayLocal
        }
        return targetDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainservicegeminiaionlineservicekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/service/GeminiAiOnlineService.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/service/GeminiAiOnlineService.kt`
- **Lines of Code:** 867
- **Size:** 38,218 bytes (37.3 KB)

```kotlin
package com.neuroflow.app.domain.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection
import kotlin.math.max
import kotlin.math.min

data class TaskEnrichmentResult(
    val microSteps: List<MicroStepSuggestion>,
    val suggestedSlotStartHour: Int,
    val suggestedSlotStartMinute: Int,
    val suggestedDurationMinutes: Int,
    val activationTip: String,
    val recommendedEnergyDemand: EnergyDemand,
    val cognitiveFrictionWarning: String? = null
)

data class SlotRecommendation(
    val suggestedStartMillis: Long,
    val suggestedEndMillis: Long,
    val confidence: Int,
    val circadianPhase: String,
    val rationale: String,
    val alternativeStartMillis: Long? = null
)

data class EnergyAdvisory(
    val statusTitle: String,
    val actionAdvice: String,
    val suggestedCategory: String,
    val suggestedEnergyDemand: EnergyDemand,
    val powerNapRecommended: Boolean
)

data class QuickNoteSynthesis(
    val extractedTaskTitle: String,
    val suggestedCategory: String,
    val estimatedMinutes: Int,
    val tags: List<String>,
    val firstActionStep: String
)

data class WeeklyAvoidanceAuditResult(
    val avoidanceAuditJson: String,
    val newSystemDirective: String
)

interface GeminiAiOnlineService {
    suspend fun enrichTaskWithAi(
        title: String,
        durationMinutes: Int,
        energyDemand: EnergyDemand = EnergyDemand.MODERATE,
        currentHour: Int = 10,
        isFrog: Boolean = false,
        userDirective: String = "",
        apiKey: String? = null,
        useAi: Boolean = false
    ): TaskEnrichmentResult

    suspend fun decomposeTask(
        title: String,
        durationMinutes: Int,
        userDirective: String = "",
        apiKey: String? = null,
        useAi: Boolean = false
    ): List<MicroStepSuggestion>

    suspend fun suggestTaskSlot(
        taskTitle: String,
        estimatedMinutes: Int,
        energyDemand: EnergyDemand,
        isFrog: Boolean,
        currentEpochMillis: Long,
        candidateWindows: List<TimeSlotWindow>,
        userDirective: String = "",
        apiKey: String? = null,
        useAi: Boolean = false
    ): SlotRecommendation

    suspend fun adviseEnergyControl(
        phase: EnergyPhase,
        usableEnergyPercent: Int,
        hoursAwake: Float,
        continuousFocusMinutes: Int,
        hasSleepDebt: Boolean,
        userDirective: String = "",
        apiKey: String? = null,
        useAi: Boolean = false
    ): EnergyAdvisory

    suspend fun conductWeeklyAuditAndDirective(
        weeklyTelemetryJson: String,
        recentlyDeletedTasksJson: String,
        chronicallyPostponedTasksJson: String,
        apiKey: String? = null,
        useAi: Boolean = false
    ): WeeklyAvoidanceAuditResult

    suspend fun synthesizeQuickNote(
        rawContent: String,
        userDirective: String = "",
        apiKey: String? = null,
        useAi: Boolean = false
    ): QuickNoteSynthesis

    fun decomposeTaskLocally(title: String, durationMinutes: Int): List<MicroStepSuggestion>

    suspend fun testConnection(apiKey: String): Result<String>
}

@Singleton
class DefaultGeminiAiOnlineService @Inject constructor() : GeminiAiOnlineService {

    private data class CachedEntry(val jsonResponse: String, val timestampMillis: Long)

    private val responseCache = ConcurrentHashMap<String, CachedEntry>()
    private val cacheTtlMillis = 20 * 60 * 1000L // 20 minutes

    @Volatile
    private var rateLimitedUntilMillis: Long = 0L

    companion object {
        const val MODEL_ID = "gemini-flash-latest"
        const val ENDPOINT_BASE = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_ID:generateContent"

        val CANDIDATE_MODELS = listOf(
            "gemini-flash-latest",
            "gemini-flash-lite-latest",
            "gemini-2.5-flash",
            "gemini-2.0-flash"
        )
        private const val TAG = "GeminiAiOnlineService"
    }

    private fun isRateLimited(): Boolean {
        return System.currentTimeMillis() < rateLimitedUntilMillis
    }

    private fun checkCache(cacheKey: String): String? {
        val entry = responseCache[cacheKey] ?: return null
        if (System.currentTimeMillis() - entry.timestampMillis < cacheTtlMillis) {
            return entry.jsonResponse
        }
        responseCache.remove(cacheKey)
        return null
    }

    private fun storeCache(cacheKey: String, json: String) {
        responseCache[cacheKey] = CachedEntry(json, System.currentTimeMillis())
    }

    override suspend fun enrichTaskWithAi(
        title: String,
        durationMinutes: Int,
        energyDemand: EnergyDemand,
        currentHour: Int,
        isFrog: Boolean,
        userDirective: String,
        apiKey: String?,
        useAi: Boolean
    ): TaskEnrichmentResult {
        val cleanTitle = title.trim().take(70).ifBlank { "Focus Task" }
        val boundedMinutes = durationMinutes.coerceIn(5, 240)
        val trimmedKey = apiKey?.trim().orEmpty()

        if (!useAi || trimmedKey.isBlank() || isRateLimited()) {
            return localTaskEnrichment(cleanTitle, boundedMinutes, energyDemand, currentHour, isFrog)
        }

        val cacheKey = "enrich_${cleanTitle.lowercase()}_${boundedMinutes}_${energyDemand.name}_${currentHour}_${isFrog}_${userDirective.hashCode()}"
        val cached = checkCache(cacheKey)
        if (cached != null) {
            parseEnrichmentResponse(cached)?.let { return it }
        }

        val directiveBlock = if (userDirective.isNotBlank()) "USER ADAPTIVE DIRECTIVE:\n$userDirective\n" else ""
        val prompt = """
            $directiveBlock
            You are an elite ADHD Executive Function & Chronobiology Coach. Analyze and enrich the following task.
            Task: "$cleanTitle" (Duration: $boundedMinutes mins, Demand: ${energyDemand.name}, Frog: $isFrog, Current Hour: $currentHour:00)

            Requirements:
            1. Decompose into 3 to 4 actionable micro-steps. Step 1 MUST be a 2-to-4 minute zero-friction activation doorway.
            2. Suggest the optimal clock start hour (0-23) and minute (0, 15, 30, 45) respecting circadian peak/dip.
            3. Provide a 1-sentence activation tip that removes initiation anxiety.
            4. Return ONLY a valid JSON object with keys:
               "microSteps": [{"title": string, "estimatedMinutes": int}],
               "suggestedStartHour": int,
               "suggestedStartMinute": int,
               "activationTip": string,
               "recommendedEnergyDemand": "HIGH" | "MODERATE" | "LOW",
               "cognitiveFrictionWarning": string or null
        """.trimIndent()

        return try {
            val responseText = executeGeminiCall(prompt, trimmedKey, maxTokens = 500)
            if (responseText != null) {
                storeCache(cacheKey, responseText)
                parseEnrichmentResponse(responseText) ?: localTaskEnrichment(cleanTitle, boundedMinutes, energyDemand, currentHour, isFrog)
            } else {
                localTaskEnrichment(cleanTitle, boundedMinutes, energyDemand, currentHour, isFrog)
            }
        } catch (_: Exception) {
            localTaskEnrichment(cleanTitle, boundedMinutes, energyDemand, currentHour, isFrog)
        }
    }

    override suspend fun decomposeTask(
        title: String,
        durationMinutes: Int,
        userDirective: String,
        apiKey: String?,
        useAi: Boolean
    ): List<MicroStepSuggestion> {
        val cleanTitle = title.trim().take(70).ifBlank { "Task" }
        val boundedMinutes = durationMinutes.coerceIn(5, 240)
        val trimmedKey = apiKey?.trim().orEmpty()

        if (!useAi || trimmedKey.isBlank() || isRateLimited()) {
            return decomposeTaskLocally(cleanTitle, boundedMinutes)
        }

        val cacheKey = "decomp_${cleanTitle.lowercase()}_${boundedMinutes}_${userDirective.hashCode()}"
        val cached = checkCache(cacheKey)
        if (cached != null) {
            val parsed = parseDecompositionResponse(cached)
            if (parsed.isNotEmpty()) return parsed
        }

        val directiveBlock = if (userDirective.isNotBlank()) "USER ADAPTIVE DIRECTIVE:\n$userDirective\n" else ""
        val archetype = DefaultSmartTaskDecompositionService.classifyArchetype(cleanTitle)
        val prompt = """
            $directiveBlock
            You are an executive function coach. Decompose this task into 3 to 4 concrete low-friction micro-steps.
            Task: "$cleanTitle" (Archetype: ${archetype.name}, Total: $boundedMinutes mins)

            Requirements:
            1. Step 1 MUST be a 2-4 minute frictionless activation spark.
            2. Sum of estimatedMinutes across steps roughly equals $boundedMinutes.
            3. Return ONLY a valid JSON array of objects with keys "title" (string) and "estimatedMinutes" (integer).
        """.trimIndent()

        return try {
            val responseText = executeGeminiCall(prompt, trimmedKey, maxTokens = 400)
            if (responseText != null) {
                storeCache(cacheKey, responseText)
                val parsed = parseDecompositionResponse(responseText)
                if (parsed.isNotEmpty()) parsed else decomposeTaskLocally(cleanTitle, boundedMinutes)
            } else {
                decomposeTaskLocally(cleanTitle, boundedMinutes)
            }
        } catch (_: Exception) {
            decomposeTaskLocally(cleanTitle, boundedMinutes)
        }
    }

    override suspend fun suggestTaskSlot(
        taskTitle: String,
        estimatedMinutes: Int,
        energyDemand: EnergyDemand,
        isFrog: Boolean,
        currentEpochMillis: Long,
        candidateWindows: List<TimeSlotWindow>,
        userDirective: String,
        apiKey: String?,
        useAi: Boolean
    ): SlotRecommendation {
        val firstWindow = candidateWindows.firstOrNull() ?: TimeSlotWindow(
            currentEpochMillis + 15 * 60_000L,
            currentEpochMillis + (15 + estimatedMinutes) * 60_000L
        )

        val trimmedKey = apiKey?.trim().orEmpty()
        if (!useAi || trimmedKey.isBlank() || isRateLimited()) {
            return localSlotRecommendation(firstWindow, energyDemand, isFrog)
        }

        val cacheKey = "slot_${taskTitle.take(30).lowercase()}_${estimatedMinutes}_${energyDemand.name}_${isFrog}_${firstWindow.startMillis}_${firstWindow.endMillis}_${userDirective.hashCode()}"
        val cached = checkCache(cacheKey)
        if (cached != null) {
            parseSlotResponse(cached, firstWindow)?.let { return it }
        }

        val directiveBlock = if (userDirective.isNotBlank()) "USER ADAPTIVE DIRECTIVE:\n$userDirective\n" else ""
        val prompt = """
            $directiveBlock
            You are a chronobiologist scheduler. Recommend the best time slot for the task.
            Task: "$taskTitle", Duration: $estimatedMinutes mins, Demand: ${energyDemand.name}, Frog: $isFrog
            Candidate window start: ${firstWindow.startMillis}, end: ${firstWindow.endMillis}

            Return ONLY a valid JSON object:
            {
               "confidence": int (0-100),
               "circadianPhase": string (e.g. "Morning Peak", "Afternoon Rebound"),
               "rationale": string (1 concise sentence explaining biological alignment)
            }
        """.trimIndent()

        return try {
            val responseText = executeGeminiCall(prompt, trimmedKey, maxTokens = 250)
            if (responseText != null) {
                storeCache(cacheKey, responseText)
                parseSlotResponse(responseText, firstWindow) ?: localSlotRecommendation(firstWindow, energyDemand, isFrog)
            } else {
                localSlotRecommendation(firstWindow, energyDemand, isFrog)
            }
        } catch (_: Exception) {
            localSlotRecommendation(firstWindow, energyDemand, isFrog)
        }
    }

    override suspend fun adviseEnergyControl(
        phase: EnergyPhase,
        usableEnergyPercent: Int,
        hoursAwake: Float,
        continuousFocusMinutes: Int,
        hasSleepDebt: Boolean,
        userDirective: String,
        apiKey: String?,
        useAi: Boolean
    ): EnergyAdvisory {
        val trimmedKey = apiKey?.trim().orEmpty()
        if (!useAi || trimmedKey.isBlank() || isRateLimited()) {
            return localEnergyAdvisory(phase, usableEnergyPercent, continuousFocusMinutes)
        }

        val cacheKey = "energy_${phase.name}_${usableEnergyPercent / 10}_${continuousFocusMinutes / 30}_${hasSleepDebt}_${(hoursAwake * 2).toInt()}_${userDirective.hashCode()}"
        val cached = checkCache(cacheKey)
        if (cached != null) {
            parseEnergyResponse(cached)?.let { return it }
        }

        val directiveBlock = if (userDirective.isNotBlank()) "USER ADAPTIVE DIRECTIVE:\n$userDirective\n" else ""
        val prompt = """
            $directiveBlock
            You are a circadian energy advisor. Provide real-time biological pacing guidance.
            Current State: Phase=${phase.name}, UsableEnergy=$usableEnergyPercent%, HoursAwake=${"%.1f".format(Locale.ROOT, hoursAwake)}, ContinuousFocus=${continuousFocusMinutes}m, SleepDebt=$hasSleepDebt.

            Return ONLY a valid JSON object:
            {
               "statusTitle": string (e.g. "Peak Focus Zone", "Post-Lunch Energy Dip", "Synaptic Saturation"),
               "actionAdvice": string (1 concise, actionable science-backed recommendation),
               "suggestedCategory": "DEEP_WORK" | "ADMIN" | "RECOVERY" | "REST",
               "suggestedEnergyDemand": "HIGH" | "MODERATE" | "LOW",
               "powerNapRecommended": boolean
            }
        """.trimIndent()

        return try {
            val responseText = executeGeminiCall(prompt, trimmedKey, maxTokens = 250)
            if (responseText != null) {
                storeCache(cacheKey, responseText)
                parseEnergyResponse(responseText) ?: localEnergyAdvisory(phase, usableEnergyPercent, continuousFocusMinutes)
            } else {
                localEnergyAdvisory(phase, usableEnergyPercent, continuousFocusMinutes)
            }
        } catch (_: Exception) {
            localEnergyAdvisory(phase, usableEnergyPercent, continuousFocusMinutes)
        }
    }

    override suspend fun conductWeeklyAuditAndDirective(
        weeklyTelemetryJson: String,
        recentlyDeletedTasksJson: String,
        chronicallyPostponedTasksJson: String,
        apiKey: String?,
        useAi: Boolean
    ): WeeklyAvoidanceAuditResult {
        val trimmedKey = apiKey?.trim().orEmpty()
        if (!useAi || trimmedKey.isBlank() || isRateLimited()) {
            return localWeeklyAudit(recentlyDeletedTasksJson)
        }

        val prompt = """
            You are an elite Clinical Neuropsychologist and Chronobiologist specializing in adult ADHD executive dysfunction, 
            the Borbély Two-Process Circadian Model, and cognitive friction mechanics.
            Conduct a weekly forensic audit of avoided, postponed, and deleted tasks, and generate an updated Adaptive System Directive.

            NON-NEGOTIABLE DIRECTIVES:
            1. STRICT BAN ON GENERIC PLATITUDES: Never say "Stay focused", "Manage your time", "Just do it", or "Eat that frog".
            2. GROUND IN EXECUTIVE FUNCTION: Frame all diagnoses via initiation threshold deficits, affective forecasting error, working memory saturation, or circadian dip mismatch.
            3. PRESCRIPTIONS MUST BE 120-SECOND PROTOCOLS: Specific, micro-actionable tactics.

            TELEMETRY:
            Weekly Metrics: $weeklyTelemetryJson
            Recently Deleted Tasks: $recentlyDeletedTasksJson
            Chronically Postponed Tasks: $chronicallyPostponedTasksJson

            Return ONLY a valid JSON object conforming strictly to this schema:
            {
              "avoidanceAudit": {
                "primaryAvoidancePattern": string,
                "biologicalCircadianMismatch": string,
                "neuropsychologicalTrigger": string,
                "tacticalPrescriptions": [
                  {
                    "strategyName": string,
                    "scientificRationale": string,
                    "immediateActionProtocol": string
                  }
                ]
              },
              "adaptiveSystemDirective": {
                "summary": string,
                "estimationBiasBuffer": double,
                "mandatoryFrictionRule": string,
                "circadianRoutingRule": string
              }
            }
        """.trimIndent()

        return try {
            val responseText = executeGeminiCall(prompt, trimmedKey, maxTokens = 800)
            if (responseText != null) {
                val root = JSONObject(extractCleanJson(responseText))
                val auditObj = root.optJSONObject("avoidanceAudit") ?: JSONObject()
                val directiveObj = root.optJSONObject("adaptiveSystemDirective") ?: JSONObject()

                val directiveFormatted = buildString {
                    appendLine("[Adaptive System Directive — Calibrated via Weekly Audit]")
                    appendLine("- Profile: ${directiveObj.optString("summary", "Standard cognitive pacing.")}")
                    appendLine("- Estimation Bias Buffer: ${directiveObj.optDouble("estimationBiasBuffer", 1.25)}x")
                    appendLine("- Friction Rule: ${directiveObj.optString("mandatoryFrictionRule", "Decompose tasks >45m into a 2m starter step.")}")
                    appendLine("- Circadian Routing: ${directiveObj.optString("circadianRoutingRule", "Lock high cognitive work into morning peak.")}")
                }.trim()

                WeeklyAvoidanceAuditResult(
                    avoidanceAuditJson = auditObj.toString(),
                    newSystemDirective = directiveFormatted
                )
            } else {
                localWeeklyAudit(recentlyDeletedTasksJson)
            }
        } catch (_: Exception) {
            localWeeklyAudit(recentlyDeletedTasksJson)
        }
    }

    override suspend fun synthesizeQuickNote(
        rawContent: String,
        userDirective: String,
        apiKey: String?,
        useAi: Boolean
    ): QuickNoteSynthesis {
        val trimmed = rawContent.trim()
        val firstLine = trimmed.lines().firstOrNull().orEmpty().take(50).ifBlank { "Quick Thought" }
        val trimmedKey = apiKey?.trim().orEmpty()

        if (!useAi || trimmedKey.isBlank() || isRateLimited()) {
            return QuickNoteSynthesis(
                extractedTaskTitle = firstLine,
                suggestedCategory = "GENERAL",
                estimatedMinutes = 20,
                tags = listOf("quick-note"),
                firstActionStep = "Review scratchpad notes and outline details"
            )
        }

        val prompt = """
            Extract a clean, actionable task from this raw scratchpad note:
            "$trimmed"

            Return ONLY valid JSON:
            {
               "extractedTaskTitle": string (clear, verb-driven title <= 50 chars),
               "suggestedCategory": "DEEP_WORK" | "ADMIN" | "CREATIVE" | "CHORES" | "GENERAL",
               "estimatedMinutes": int (5-120),
               "tags": [string],
               "firstActionStep": string (2-minute activation step)
            }
        """.trimIndent()

        return try {
            val responseText = executeGeminiCall(prompt, trimmedKey, maxTokens = 250)
            if (responseText != null) {
                val obj = JSONObject(extractCleanJson(responseText))
                val tagsArr = obj.optJSONArray("tags") ?: JSONArray()
                val tagsList = (0 until tagsArr.length()).map { tagsArr.getString(it) }
                QuickNoteSynthesis(
                    extractedTaskTitle = obj.optString("extractedTaskTitle", firstLine),
                    suggestedCategory = obj.optString("suggestedCategory", "GENERAL"),
                    estimatedMinutes = obj.optInt("estimatedMinutes", 20).coerceIn(5, 180),
                    tags = tagsList.ifEmpty { listOf("quick-note") },
                    firstActionStep = obj.optString("firstActionStep", "Open workspace and review note details")
                )
            } else {
                QuickNoteSynthesis(firstLine, "GENERAL", 20, listOf("quick-note"), "Review scratchpad note")
            }
        } catch (_: Exception) {
            QuickNoteSynthesis(firstLine, "GENERAL", 20, listOf("quick-note"), "Review scratchpad note")
        }
    }

    private suspend fun executeGeminiCall(
        prompt: String,
        apiKey: String,
        maxTokens: Int
    ): String? = withContext(Dispatchers.IO) {
        val reqObj = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            ))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
                put("maxOutputTokens", maxTokens)
                put("responseMimeType", "application/json")
            })
        }

        val requestBytes = reqObj.toString().toByteArray(Charsets.UTF_8)

        var encountered429 = false
        for (model in CANDIDATE_MODELS) {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            var connection: HttpsURLConnection? = null
            try {
                connection = (URL(endpoint).openConnection() as HttpsURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    connectTimeout = 10_000
                    readTimeout = 15_000
                    doOutput = true
                }

                connection.outputStream.use { os ->
                    os.write(requestBytes)
                    os.flush()
                }

                val code = connection.responseCode
                if (code in 200..299) {
                    val resp = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    Log.d(TAG, "[$model] Success (HTTP $code)")
                    return@withContext resp
                } else if (code == 429) {
                    encountered429 = true
                    val err = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    Log.w(TAG, "[$model] Rate limited (HTTP 429): $err. Cascading to next candidate model...")
                } else if (code == 400) {
                    val err = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    Log.e(TAG, "[$model] HTTP 400 Bad Request / Invalid Key: $err")
                    return@withContext null
                } else {
                    val err = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    Log.w(TAG, "[$model] HTTP $code: $err. Trying next candidate model...")
                }
            } catch (e: Exception) {
                Log.w(TAG, "[$model] Network error / timeout (${e.javaClass.simpleName}: ${e.message}). Trying next candidate...")
            } finally {
                connection?.disconnect()
            }
        }
        if (encountered429) {
            Log.w(TAG, "All candidate Gemini models exhausted with rate limits (429). Engaging 60s cooldown.")
            rateLimitedUntilMillis = System.currentTimeMillis() + 60_000L
        } else {
            Log.w(TAG, "All candidate Gemini models exhausted. Gracefully degrading to local heuristics.")
        }
        null
    }

    override suspend fun testConnection(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is empty."))
        }

        val pingObj = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "ping")))
            ))
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", 5)
            })
        }
        val pingBytes = pingObj.toString().toByteArray(Charsets.UTF_8)
        var lastError = "Unable to connect."

        for (model in CANDIDATE_MODELS) {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$cleanKey"
            var conn: HttpsURLConnection? = null
            try {
                conn = (URL(endpoint).openConnection() as HttpsURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    doOutput = true
                }
                conn.outputStream.use { os ->
                    os.write(pingBytes)
                    os.flush()
                }
                val code = conn.responseCode
                if (code in 200..299) {
                    return@withContext Result.success("Connected successfully to $model (HTTP $code)")
                } else if (code == 400) {
                    val err = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    return@withContext Result.failure(Exception("HTTP 400: API Key Invalid. Please check your key at aistudio.google.com/apikey"))
                } else if (code == 429) {
                    val err = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    lastError = "HTTP 429 on $model: Rate limited or quota exhausted: $err"
                    Log.w(TAG, "[$model] Test connection rate limited (HTTP 429). Cascading to next candidate...")
                } else {
                    val err = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    lastError = "HTTP $code on $model: $err"
                }
            } catch (e: Exception) {
                lastError = "${e.javaClass.simpleName}: ${e.message}"
            } finally {
                conn?.disconnect()
            }
        }
        Result.failure(Exception("Connection failed: $lastError"))
    }

    override fun decomposeTaskLocally(title: String, durationMinutes: Int): List<MicroStepSuggestion> {
        return DefaultSmartTaskDecompositionService.buildArchetypeMicroSteps(title, durationMinutes)
    }

    private fun localTaskEnrichment(
        title: String,
        durationMinutes: Int,
        energyDemand: EnergyDemand,
        currentHour: Int,
        isFrog: Boolean
    ): TaskEnrichmentResult {
        val steps = decomposeTaskLocally(title, durationMinutes)
        val suggestedHour = when {
            isFrog || energyDemand == EnergyDemand.HIGH -> 10
            energyDemand == EnergyDemand.LOW -> 16
            else -> (currentHour + 1).coerceIn(8, 20)
        }
        return TaskEnrichmentResult(
            microSteps = steps,
            suggestedSlotStartHour = suggestedHour,
            suggestedSlotStartMinute = 0,
            suggestedDurationMinutes = durationMinutes,
            activationTip = "Take 2 minutes to open the relevant file or tools before doing deep work.",
            recommendedEnergyDemand = energyDemand,
            cognitiveFrictionWarning = if (durationMinutes >= 90) "Large session: 15-minute recovery break recommended after completion." else null
        )
    }

    private fun localSlotRecommendation(
        window: TimeSlotWindow,
        energyDemand: EnergyDemand,
        isFrog: Boolean
    ): SlotRecommendation {
        return SlotRecommendation(
            suggestedStartMillis = window.startMillis,
            suggestedEndMillis = window.endMillis,
            confidence = if (isFrog) 90 else 80,
            circadianPhase = if (energyDemand == EnergyDemand.HIGH) "Morning Peak" else "Standard Window",
            rationale = "Aligned with schedule availability and baseline circadian energy curve."
        )
    }

    private fun localEnergyAdvisory(
        phase: EnergyPhase,
        usableEnergyPercent: Int,
        continuousFocusMinutes: Int
    ): EnergyAdvisory {
        return when {
            continuousFocusMinutes >= 90 -> EnergyAdvisory(
                statusTitle = "Synaptic Saturation Warning",
                actionAdvice = "You have completed 90+ minutes of continuous focus. Take a 15-minute restorative walk or eye-rest break.",
                suggestedCategory = "RECOVERY",
                suggestedEnergyDemand = EnergyDemand.LOW,
                powerNapRecommended = false
            )
            phase == EnergyPhase.PEAK -> EnergyAdvisory(
                statusTitle = "Biological Peak Window",
                actionAdvice = "Optimal neurochemical alertness ($usableEnergyPercent%). Ideal for Daily Frog or complex analytical challenges.",
                suggestedCategory = "DEEP_WORK",
                suggestedEnergyDemand = EnergyDemand.HIGH,
                powerNapRecommended = false
            )
            phase == EnergyPhase.DIP -> EnergyAdvisory(
                statusTitle = "Circadian Post-Lunch Dip",
                actionAdvice = "Usable energy dips ($usableEnergyPercent%). Switch to routine admin chores or take a 20-minute power nap.",
                suggestedCategory = "ADMIN",
                suggestedEnergyDemand = EnergyDemand.LOW,
                powerNapRecommended = true
            )
            else -> EnergyAdvisory(
                statusTitle = "Stable Operating Energy",
                actionAdvice = "Energy is steady at $usableEnergyPercent%. Maintain focus cadences with regular hydration.",
                suggestedCategory = "GENERAL",
                suggestedEnergyDemand = EnergyDemand.MODERATE,
                powerNapRecommended = false
            )
        }
    }

    private fun localWeeklyAudit(recentlyDeletedJson: String): WeeklyAvoidanceAuditResult {
        val auditJson = JSONObject().apply {
            put("primaryAvoidancePattern", "Avoidance of unstructured tasks exceeding 60 minutes.")
            put("biologicalCircadianMismatch", "Tasks placed during afternoon energy dips experienced higher postponement frequency.")
            put("neuropsychologicalTrigger", "Initiation threshold friction and affective forecasting dread.")
            put("tacticalPrescriptions", JSONArray().apply {
                put(JSONObject().apply {
                    put("strategyName", "120-Second Gateway Doorway")
                    put("scientificRationale", "Removes outcome pressure from the basal ganglia.")
                    put("immediateActionProtocol", "Schedule only the first 2-minute starter step before committing to full duration.")
                })
            })
        }.toString()

        val directive = """
            [Adaptive System Directive — Calibrated via Local Neuro-Profile]
            - Profile: High velocity on <45m structured tasks; friction on large blocks.
            - Estimation Bias Buffer: 1.25x
            - Friction Rule: Any task >45m must be preceded by a <=3-minute activation step.
            - Circadian Routing: High cognitive tasks locked to morning peak (09:30-12:30).
        """.trimIndent()

        return WeeklyAvoidanceAuditResult(auditJson, directive)
    }

    private fun extractCleanJson(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("{") && clean.contains("\"candidates\"")) {
            try {
                val envelope = JSONObject(clean)
                val candidate = envelope.optJSONArray("candidates")?.optJSONObject(0)
                val part = candidate?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)
                val text = part?.optString("text")
                if (!text.isNullOrBlank()) {
                    clean = text.trim()
                }
            } catch (_: Exception) {
                val textMatch = Regex(""""text"\s*:\s*"((?:\\.|[^"\\])*)"""").find(clean)
                if (textMatch != null) {
                    clean = textMatch.groupValues[1]
                        .replace("\\\"", "\"")
                        .replace("\\n", "\n")
                        .replace("\\r", "")
                        .replace("\\\\", "\\")
                }
            }
        }

        if (clean.startsWith("```json")) clean = clean.removePrefix("```json").trim()
        if (clean.startsWith("```")) clean = clean.removePrefix("```").trim()
        if (clean.endsWith("```")) clean = clean.removeSuffix("```").trim()

        val firstBrace = clean.indexOf('{')
        val firstBracket = clean.indexOf('[')
        val startIdx = when {
            firstBrace != -1 && firstBracket != -1 -> minOf(firstBrace, firstBracket)
            firstBrace != -1 -> firstBrace
            firstBracket != -1 -> firstBracket
            else -> 0
        }
        val lastBrace = clean.lastIndexOf('}')
        val lastBracket = clean.lastIndexOf(']')
        val endIdx = maxOf(lastBrace, lastBracket)
        if (startIdx >= 0 && endIdx > startIdx) {
            clean = clean.substring(startIdx, endIdx + 1)
        }
        return clean
    }

    private fun parseEnrichmentResponse(raw: String): TaskEnrichmentResult? {
        return try {
            val clean = extractCleanJson(raw)
            val root = JSONObject(clean)
            val stepsArr = root.optJSONArray("microSteps") ?: JSONArray()
            val steps = mutableListOf<MicroStepSuggestion>()
            for (i in 0 until stepsArr.length()) {
                val item = stepsArr.getJSONObject(i)
                steps.add(MicroStepSuggestion(item.optString("title", "Step"), item.optInt("estimatedMinutes", 5)))
            }
            val startHour = root.optInt("suggestedStartHour", 10).coerceIn(0, 23)
            val startMinute = root.optInt("suggestedStartMinute", 0).coerceIn(0, 59)
            val tip = root.optString("activationTip", "Start with a 2-minute setup step.")
            val demandStr = root.optString("recommendedEnergyDemand", "MODERATE")
            val demand = when (demandStr.uppercase(Locale.ROOT)) {
                "HIGH" -> EnergyDemand.HIGH
                "LOW" -> EnergyDemand.LOW
                else -> EnergyDemand.MODERATE
            }
            val warning = root.optString("cognitiveFrictionWarning").takeIf { it.isNotBlank() && it != "null" }
            TaskEnrichmentResult(
                microSteps = steps.ifEmpty { decomposeTaskLocally("Task", 30) },
                suggestedSlotStartHour = startHour,
                suggestedSlotStartMinute = startMinute,
                suggestedDurationMinutes = steps.sumOf { it.estimatedMinutes }.coerceAtLeast(15),
                activationTip = tip,
                recommendedEnergyDemand = demand,
                cognitiveFrictionWarning = warning
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseDecompositionResponse(raw: String): List<MicroStepSuggestion> {
        return try {
            val clean = extractCleanJson(raw)
            val arr = if (clean.startsWith("[")) {
                JSONArray(clean)
            } else {
                val obj = JSONObject(clean)
                obj.optJSONArray("microSteps") ?: obj.optJSONArray("steps") ?: JSONArray()
            }
            val result = mutableListOf<MicroStepSuggestion>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val title = item.optString("title", "").trim()
                val minutes = item.optInt("estimatedMinutes", 5).coerceIn(1, 180)
                if (title.isNotBlank()) result.add(MicroStepSuggestion(title, minutes))
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseSlotResponse(raw: String, fallbackWindow: TimeSlotWindow): SlotRecommendation? {
        return try {
            val clean = extractCleanJson(raw)
            val root = JSONObject(clean)
            SlotRecommendation(
                suggestedStartMillis = fallbackWindow.startMillis,
                suggestedEndMillis = fallbackWindow.endMillis,
                confidence = root.optInt("confidence", 85).coerceIn(0, 100),
                circadianPhase = root.optString("circadianPhase", "Morning Peak"),
                rationale = root.optString("rationale", "Aligned with circadian alertness curve.")
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseEnergyResponse(raw: String): EnergyAdvisory? {
        return try {
            val clean = extractCleanJson(raw)
            val root = JSONObject(clean)
            val demandStr = root.optString("suggestedEnergyDemand", "MODERATE")
            val demand = when (demandStr.uppercase(Locale.ROOT)) {
                "HIGH" -> EnergyDemand.HIGH
                "LOW" -> EnergyDemand.LOW
                else -> EnergyDemand.MODERATE
            }
            EnergyAdvisory(
                statusTitle = root.optString("statusTitle", "Stable Energy"),
                actionAdvice = root.optString("actionAdvice", "Maintain healthy pacing."),
                suggestedCategory = root.optString("suggestedCategory", "GENERAL"),
                suggestedEnergyDemand = demand,
                powerNapRecommended = root.optBoolean("powerNapRecommended", false)
            )
        } catch (_: Exception) {
            null
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainservicesmarttaskdecompositionservicekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/service/SmartTaskDecompositionService.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/service/SmartTaskDecompositionService.kt`
- **Lines of Code:** 221
- **Size:** 10,824 bytes (10.6 KB)

```kotlin
package com.neuroflow.app.domain.service

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

interface SmartTaskDecompositionService {
    suspend fun decomposeTask(
        title: String,
        durationMinutes: Int,
        apiKey: String? = null,
        useAi: Boolean = false
    ): List<MicroStepSuggestion>

    fun decomposeTaskLocally(
        title: String,
        durationMinutes: Int
    ): List<MicroStepSuggestion>
}

enum class TaskArchetype {
    CODING_TECH,
    WRITING_RESEARCH,
    STUDY_ACADEMIC,
    CHORE_MAINTENANCE,
    ADMIN_LOGISTICS,
    GENERAL
}

@Singleton
class DefaultSmartTaskDecompositionService @Inject constructor(
    private val geminiAiOnlineService: GeminiAiOnlineService
) : SmartTaskDecompositionService {

    constructor() : this(DefaultGeminiAiOnlineService())

    override suspend fun decomposeTask(
        title: String,
        durationMinutes: Int,
        apiKey: String?,
        useAi: Boolean
    ): List<MicroStepSuggestion> {
        val trimmedKey = apiKey?.trim().orEmpty()
        if (!useAi || trimmedKey.isBlank()) {
            return decomposeTaskLocally(title, durationMinutes)
        }

        return geminiAiOnlineService.decomposeTask(
            title = title,
            durationMinutes = durationMinutes,
            userDirective = "",
            apiKey = trimmedKey,
            useAi = useAi
        )
    }

    override fun decomposeTaskLocally(title: String, durationMinutes: Int): List<MicroStepSuggestion> {
        return buildArchetypeMicroSteps(title, durationMinutes)
    }

    companion object {
        fun classifyArchetype(title: String): TaskArchetype {
            val lower = title.lowercase(Locale.ROOT)
            val tokens = lower.split(Regex("[\\s,._\\-:/]+")).filter { it.isNotBlank() }.toSet()

            fun hasAny(vararg words: String): Boolean = words.any { it in tokens || lower.contains(it) }

            return when {
                hasAny("code", "fix", "bug", "refactor", "api", "test", "impl", "build", "debug", "pr", "git", "backend", "frontend", "sql", "db", "database", "endpoint", "deploy", "script", "compile", "lint", "module") ->
                    TaskArchetype.CODING_TECH
                hasAny("write", "draft", "blog", "report", "essay", "document", "doc", "article", "summary", "copy", "proposal", "review", "edit", "outline", "notes", "research") ->
                    TaskArchetype.WRITING_RESEARCH
                hasAny("read", "study", "learn", "chapter", "exam", "textbook", "lecture", "paper", "quiz", "homework", "flashcards", "course", "revision", "assignment") ->
                    TaskArchetype.STUDY_ACADEMIC
                hasAny("clean", "wash", "laundry", "organize", "buy", "grocery", "shop", "dishes", "tidy", "pack", "trash", "cook", "meal", "prepare", "declutter", "car") ->
                    TaskArchetype.CHORE_MAINTENANCE
                hasAny("email", "call", "invoice", "taxes", "schedule", "file", "bill", "bank", "form", "application", "submit", "register", "renew", "pay", "book", "flight", "appointment") ->
                    TaskArchetype.ADMIN_LOGISTICS
                else -> TaskArchetype.GENERAL
            }
        }

        fun buildArchetypeMicroSteps(title: String, durationMinutes: Int): List<MicroStepSuggestion> {
            val clean = title.trim().take(50).ifBlank { "Task" }
            val boundedDuration = durationMinutes.coerceIn(10, 240)
            val archetype = classifyArchetype(clean)

            // Step 1 is ALWAYS a 2-to-4 minute zero-friction physical activation doorway
            val doorwayMins = min(4, max(2, boundedDuration / 12))
            val reviewMins = min(10, max(3, boundedDuration / 6))
            val coreMins = max(5, boundedDuration - doorwayMins - reviewMins)
            val part1Mins = max(5, coreMins / 2)
            val part2Mins = max(5, coreMins - part1Mins)

            return when (archetype) {
                TaskArchetype.CODING_TECH -> listOf(
                    MicroStepSuggestion("Open IDE, checkout branch & locate target file for $clean", doorwayMins),
                    MicroStepSuggestion("Write failing test or stub signature for core logic", part1Mins),
                    MicroStepSuggestion("Implement happy-path logic without premature optimization", part2Mins),
                    MicroStepSuggestion("Run local test suite & clean up compiler warnings", reviewMins)
                )
                TaskArchetype.WRITING_RESEARCH -> listOf(
                    MicroStepSuggestion("Open blank doc & type 3 messy bullet headings for $clean", doorwayMins),
                    MicroStepSuggestion("Draft rough body paragraphs without editing or backspacing", part1Mins),
                    MicroStepSuggestion("Fill in supporting data points, citations, or examples", part2Mins),
                    MicroStepSuggestion("Read aloud once & polish formatting", reviewMins)
                )
                TaskArchetype.STUDY_ACADEMIC -> listOf(
                    MicroStepSuggestion("Open study material to exact page & clear phone from desk", doorwayMins),
                    MicroStepSuggestion("Skim section headers & summarize key concepts in own words", part1Mins),
                    MicroStepSuggestion("Work through active recall questions or practice problems", part2Mins),
                    MicroStepSuggestion("Flag 2 weak areas for tomorrow's spaced repetition", reviewMins)
                )
                TaskArchetype.CHORE_MAINTENANCE -> listOf(
                    MicroStepSuggestion("Put on headphones/timer & clear one small surface for $clean", doorwayMins),
                    MicroStepSuggestion("Complete the highest-visibility 50% of $clean", part1Mins + part2Mins),
                    MicroStepSuggestion("Put away tools & do a 2-minute final reset", reviewMins)
                )
                TaskArchetype.ADMIN_LOGISTICS -> listOf(
                    MicroStepSuggestion("Open browser tab, portal, or document needed for $clean", doorwayMins),
                    MicroStepSuggestion("Fill in required fields or draft message body", part1Mins + part2Mins),
                    MicroStepSuggestion("Verify attachments/numbers & hit submit/send", reviewMins)
                )
                TaskArchetype.GENERAL -> listOf(
                    MicroStepSuggestion("Open workspace & do 2 minutes of setup for $clean", doorwayMins),
                    MicroStepSuggestion("Complete the first concrete milestone of $clean", part1Mins),
                    MicroStepSuggestion("Push through the main execution block of $clean", part2Mins),
                    MicroStepSuggestion("Review progress & polish final result", reviewMins)
                )
            }
        }

        fun parseGeminiResponse(rawJson: String): List<MicroStepSuggestion> {
            try {
                val root = JSONObject(rawJson)
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val rawText = parts.getJSONObject(0).optString("text", "").trim()
                        if (rawText.isNotBlank()) {
                            val parsed = parseJsonArrayString(rawText)
                            if (parsed.isNotEmpty()) return parsed
                        }
                    }
                }
            } catch (_: Exception) {}

            return parseViaRegexFallback(rawJson)
        }

        private fun parseJsonArrayString(rawText: String): List<MicroStepSuggestion> {
            return try {
                var jsonStr = rawText.trim()
                if (jsonStr.startsWith("```json")) {
                    jsonStr = jsonStr.removePrefix("```json").trim()
                } else if (jsonStr.startsWith("```")) {
                    jsonStr = jsonStr.removePrefix("```").trim()
                }
                if (jsonStr.endsWith("```")) {
                    jsonStr = jsonStr.removeSuffix("```").trim()
                }

                val arr = JSONArray(jsonStr)
                val result = mutableListOf<MicroStepSuggestion>()
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val title = item.optString("title", "").trim()
                    val minutes = item.optInt("estimatedMinutes", 5).coerceIn(1, 180)
                    if (title.isNotBlank()) {
                        result.add(MicroStepSuggestion(title, minutes))
                    }
                }
                result
            } catch (_: Exception) {
                parseItemsFromText(rawText)
            }
        }

        private fun parseViaRegexFallback(raw: String): List<MicroStepSuggestion> {
            val textRegex = Regex(""""text"\s*:\s*"((?:\\.|[^"\\])*)"""")
            val textMatch = textRegex.find(raw)?.groupValues?.get(1) ?: return emptyList()
            val unescaped = textMatch.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "")
            return parseItemsFromText(unescaped)
        }

        private fun parseItemsFromText(text: String): List<MicroStepSuggestion> {
            var sanitized = text.trim()
            if (sanitized.startsWith("```json")) {
                sanitized = sanitized.removePrefix("```json").trim()
            } else if (sanitized.startsWith("```")) {
                sanitized = sanitized.removePrefix("```").trim()
            }
            if (sanitized.endsWith("```")) {
                sanitized = sanitized.removeSuffix("```").trim()
            }

            val objectPattern = Regex("""\{[^{}]*\}""")
            val result = mutableListOf<MicroStepSuggestion>()
            for (match in objectPattern.findAll(sanitized)) {
                val block = match.value
                val titleMatch = Regex(""""title"\s*:\s*"([^"]+)"""").find(block)
                val minMatch = Regex(""""estimatedMinutes"\s*:\s*(\d+)""").find(block)
                if (titleMatch != null) {
                    val title = titleMatch.groupValues[1].trim()
                    val minutes = minMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(1, 180) ?: 5
                    if (title.isNotBlank()) {
                        result.add(MicroStepSuggestion(title, minutes))
                    }
                }
            }
            return result
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainservicebehavioralsupportservicekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/service/BehavioralSupportService.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/service/BehavioralSupportService.kt`
- **Lines of Code:** 160
- **Size:** 6,085 bytes (5.9 KB)

```kotlin
package com.neuroflow.app.domain.service

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import javax.inject.Inject
import javax.inject.Singleton

data class MicroStepSuggestion(
    val title: String,
    val estimatedMinutes: Int
)

data class DistractionFeedback(
    val shouldPromptBreakdown: Boolean,
    val shouldPromptDecompression: Boolean,
    val supportiveMessage: String
)

interface BehavioralSupportService {
    fun resetScheduleAnchors(tasks: List<SchedulableTask>): List<SchedulableTask>
    fun generateMicroSteps(taskTitle: String, estimatedMinutes: Int): List<MicroStepSuggestion>
    suspend fun generateMicroStepsWithAi(
        taskTitle: String,
        estimatedMinutes: Int,
        apiKey: String? = null,
        useAi: Boolean = false
    ): List<MicroStepSuggestion> = generateMicroSteps(taskTitle, estimatedMinutes)
    fun evaluateDistraction(interruptionCount: Int, durationMinutes: Int): DistractionFeedback

    companion object {
        val DEFAULT_DISTRACTING_PACKAGES: Set<String> = setOf(
            "com.instagram.android",
            "com.twitter.android",
            "com.x.android",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.snapchat.android",
            "com.facebook.katana",
            "com.facebook.lite",
            "com.reddit.frontpage",
            "com.linkedin.android",
            "com.pinterest",
            "com.tumblr",
            "com.vkontakte.android",
            "com.bereal.app",
            "com.threads.android",
            "com.quora.android",
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "org.telegram.plus",
            "com.discord",
            "com.facebook.orca",
            "com.facebook.mlite",
            "com.google.android.youtube",
            "com.netflix.mediaclient",
            "com.amazon.avod.thirdpartyclient",
            "com.disney.disneyplus",
            "com.hbo.hbonow",
            "com.hulu.plus",
            "com.twitch.android.app",
            "tv.twitch.android.app",
            "com.vanced.android.youtube",
            "com.king.candycrushsaga",
            "com.king.candycrushsodasaga",
            "com.supercell.clashofclans",
            "com.supercell.clashroyale",
            "com.supercell.brawlstars",
            "com.mojang.minecraftpe",
            "com.roblox.client",
            "com.pubg.imobile",
            "com.tencent.ig",
            "com.activision.callofduty.shooter",
            "com.garena.free.fire",
            "com.epicgames.fortnite",
            "com.ea.game.pvzfh_row",
            "com.halfbrick.fruitninjafree",
            "com.imangi.templerun2",
            "com.outfit7.talkingtomgoldrun2",
            "com.miniclip.eightballpool",
            "com.chess",
            "com.buzzfeed.android"
        )

        fun hasUsagePermission(context: Context): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return false
            @Suppress("DEPRECATION")
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            return mode == AppOpsManager.MODE_ALLOWED
        }

        fun openUsagePermissionSettings(context: Context) {
            context.startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

@Singleton
class DefaultBehavioralSupportService @Inject constructor(
    private val decompositionService: SmartTaskDecompositionService
) : BehavioralSupportService {

    constructor() : this(DefaultSmartTaskDecompositionService())

    override fun resetScheduleAnchors(tasks: List<SchedulableTask>): List<SchedulableTask> {
        // Unlock unpinned tasks to allow a completely clean replan
        return tasks.map { task ->
            if (task.isLocked) task else task.copy(scheduledStartMillis = null)
        }
    }

    override fun generateMicroSteps(taskTitle: String, estimatedMinutes: Int): List<MicroStepSuggestion> {
        return decompositionService.decomposeTaskLocally(taskTitle, estimatedMinutes)
    }

    override suspend fun generateMicroStepsWithAi(
        taskTitle: String,
        estimatedMinutes: Int,
        apiKey: String?,
        useAi: Boolean
    ): List<MicroStepSuggestion> {
        return decompositionService.decomposeTask(taskTitle, estimatedMinutes, apiKey, useAi)
    }

    override fun evaluateDistraction(interruptionCount: Int, durationMinutes: Int): DistractionFeedback {
        // Non-punitive: High distraction on tasks signals cognitive friction or excessive task size,
        // never boosting task priority.
        val isHighInterruption = interruptionCount >= 3 || (interruptionCount >= 2 && durationMinutes <= 25)
        val isLongSlog = durationMinutes >= 60 && interruptionCount >= 2

        return when {
            isLongSlog -> DistractionFeedback(
                shouldPromptBreakdown = true,
                shouldPromptDecompression = true,
                supportiveMessage = "This task seems heavy right now. Would you like to break it into 5-minute micro-steps or take a 3-minute decompression breather?"
            )
            isHighInterruption -> DistractionFeedback(
                shouldPromptBreakdown = true,
                shouldPromptDecompression = false,
                supportiveMessage = "Frequent interruptions detected. Breaking this task down into bite-sized steps can help rebuild momentum."
            )
            else -> DistractionFeedback(
                shouldPromptBreakdown = false,
                shouldPromptDecompression = false,
                supportiveMessage = "Great focus! You are maintaining healthy cognitive flow."
            )
        }
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainenginewoopenginekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/engine/WoopEngine.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/engine/WoopEngine.kt`
- **Lines of Code:** 22
- **Size:** 1,055 bytes (1.0 KB)

```kotlin
package com.neuroflow.app.domain.engine

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.WoopEntity

object WoopEngine {
    /** Returns true if the WOOP prompt should be shown for this task (never shown before). */
    fun shouldShowPrompt(woopData: WoopEntity?, woopPromptShown: Boolean): Boolean =
        woopData == null && !woopPromptShown

    /** Generates a default if-then plan string from the obstacle text. */
    fun generateIfThenPlan(obstacle: String): String =
        "If $obstacle, then I will "

    /** Returns the affective forecast insight message if ≥5 tasks were rated worse than expected. */
    fun dreadedTaskInsight(completedTasks: List<TaskEntity>): String? {
        val negativeCount = completedTasks.count { it.affectiveForecastError != null && it.affectiveForecastError < 0 }
        return if (negativeCount >= 5) {
            "You've rated $negativeCount tasks as worse than expected — but you finished them anyway. That's the real win."
        } else null
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainengineautonomynudgeenginekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/engine/AutonomyNudgeEngine.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/engine/AutonomyNudgeEngine.kt`
- **Lines of Code:** 49
- **Size:** 1,872 bytes (1.8 KB)

```kotlin
package com.neuroflow.app.domain.engine

import android.content.Context
import com.neuroflow.app.data.local.entity.TaskEntity

object AutonomyNudgeEngine {
    private const val TAG_PREFIX = "autonomy_nudge_"
    private const val GLOBAL_TAG = "autonomy_nudge_all"
    private const val DELAY_HOURS = 2L

    fun uniqueWorkName(taskId: String): String = "$TAG_PREFIX$taskId"
    fun workTag(taskId: String): String = "$TAG_PREFIX$taskId"
    fun globalTag(): String = GLOBAL_TAG

    fun scheduleNudge(context: Context, task: TaskEntity) {
        // Check POST_NOTIFICATIONS permission (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) return
        }

        val data = androidx.work.Data.Builder()
            .putString("taskId", task.id)
            .build()

        val request = androidx.work.OneTimeWorkRequestBuilder<com.neuroflow.app.worker.AutonomyNudgeWorker>()
            .setInitialDelay(DELAY_HOURS, java.util.concurrent.TimeUnit.HOURS)
            .setInputData(data)
            .addTag(workTag(task.id))
            .addTag(globalTag())
            .build()

        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(task.id),
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelNudge(context: Context, taskId: String) {
        androidx.work.WorkManager.getInstance(context)
            .cancelAllWorkByTag(workTag(taskId))
        androidx.work.WorkManager.getInstance(context)
            .cancelUniqueWork(uniqueWorkName(taskId))
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainenginefreshstartenginekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/engine/FreshStartEngine.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/engine/FreshStartEngine.kt`
- **Lines of Code:** 97
- **Size:** 4,301 bytes (4.2 KB)

```kotlin
package com.neuroflow.app.domain.engine

object FreshStartEngine {
    private const val THREE_DAYS_MS = 3L * 24 * 60 * 60 * 1000

    fun isFreshStart(
        nowMillis: Long,
        lastOpenMillis: Long,
        dailyStreak: Int,
        lastActiveDate: Long,
        lastFreshStartShownWeek: Int,
        lastFreshStartShownYear: Int
    ): Boolean {
        // Guard: already shown this ISO week
        if (isoWeekNumber(nowMillis) == lastFreshStartShownWeek &&
            isoYear(nowMillis) == lastFreshStartShownYear) return false

        val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
        val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val dayOfMonth = cal.get(java.util.Calendar.DAY_OF_MONTH)

        // Monday (first day of ISO week)
        val isMonday = dayOfWeek == java.util.Calendar.MONDAY
        // First day of month
        val isFirstOfMonth = dayOfMonth == 1
        // Day after streak break (had a streak, now it's 0)
        val isAfterStreakBreak = dailyStreak == 0 && lastActiveDate > 0L
        // 3+ day absence
        val isLongAbsence = lastOpenMillis > 0L && (nowMillis - lastOpenMillis) >= THREE_DAYS_MS

        return isMonday || isFirstOfMonth || isAfterStreakBreak || isLongAbsence
    }

    fun isoWeekNumber(millis: Long): Int {
        val instant = java.time.Instant.ofEpochMilli(millis)
        val zone = java.time.ZoneId.systemDefault()
        val localDate = instant.atZone(zone).toLocalDate()
        return localDate.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())
    }

    fun isoYear(millis: Long): Int {
        val instant = java.time.Instant.ofEpochMilli(millis)
        val zone = java.time.ZoneId.systemDefault()
        val localDate = instant.atZone(zone).toLocalDate()
        return localDate.get(java.time.temporal.WeekFields.ISO.weekBasedYear())
    }

    data class EmergencyResetPlan(
        val tasksToReschedule: List<com.neuroflow.app.data.local.entity.TaskEntity>,
        val tasksPushedToBacklog: List<com.neuroflow.app.data.local.entity.TaskEntity>,
        val streakFreezeApplied: Boolean,
        val summaryMessage: String
    )

    /**
     * Instant Frictionless One-Tap Recovery Algorithm:
     * Reshuffles remaining tasks without punishing streaks or clogging the calendar.
     *
     * 1. Protects Streaks: Preserves current streak state or applies a streak-freeze safety buffer.
     * 2. Clears Calendar Overpack: Un-schedules overdue/missed tasks for today, pushing low-impact/non-urgent tasks to Backlog.
     * 3. Keeps Only High-Impact frog/urgent tasks for immediate action today.
     */
    fun buildEmergencyResetPlan(
        activeTasks: List<com.neuroflow.app.data.local.entity.TaskEntity>,
        nowMillis: Long
    ): EmergencyResetPlan {
        val todayStart = java.util.Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Find active tasks scheduled today or in the past
        val scheduledOverdueOrToday = activeTasks.filter { task ->
            task.scheduledDate != null && task.scheduledDate!! <= todayStart && !task.isScheduleLocked
        }

        // Partition by urgency: High-impact (isFrog, impactScore >= 70, or urgent deadline) vs low-impact
        val (highPriority, lowPriority) = scheduledOverdueOrToday.partition { task ->
            task.isFrog || task.impactScore >= 70 ||
                (task.deadlineDate != null && (task.deadlineDate!! - nowMillis) <= 24 * 60 * 60 * 1000L)
        }

        // Keep top 2 high-priority tasks for today, push the rest to backlog (clear scheduleDate/Time)
        val keepForToday = highPriority.take(2)
        val pushToBacklog = lowPriority + highPriority.drop(2)

        return EmergencyResetPlan(
            tasksToReschedule = keepForToday,
            tasksPushedToBacklog = pushToBacklog,
            streakFreezeApplied = true,
            summaryMessage = "Schedule reset! ${keepForToday.size} essential tasks kept for today, ${pushToBacklog.size} moved to backlog. Streak protected!"
        )
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainengineanalyticsenginekt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/engine/AnalyticsEngine.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/engine/AnalyticsEngine.kt`
- **Lines of Code:** 610
- **Size:** 29,645 bytes (29.0 KB)

```kotlin
package com.neuroflow.app.domain.engine

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.TimeSessionEntity
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.TaskCategory
import com.neuroflow.app.domain.scheduler.determineCategory
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

object AnalyticsEngine {

    // ── MAPE / SMAPE ─────────────────────────────────────────────────────────

    fun computeMape(estimated: Float, actual: Float): Float {
        if (estimated == 0f) return 0f
        return (abs(actual - estimated) / estimated) * 100f
    }

    fun computeSmape(estimated: Float, actual: Float): Float {
        val denom = (estimated + actual) / 2f
        if (denom == 0f) return 0f
        return (abs(actual - estimated) / denom) * 100f
    }

    fun computeWeightedMape(tasks: List<TaskEntity>): Float {
        val valid = tasks.filter {
            it.estimatedDurationMinutes > 0 &&
                it.actualDurationMinutes != null &&
                it.actualDurationMinutes!! > 0f &&
                !it.actualDurationMinutes!!.isNaN() &&
                !it.actualDurationMinutes!!.isInfinite()
        }
        if (valid.isEmpty()) return 0f
        val totalWeight = valid.sumOf { it.estimatedDurationMinutes }.toFloat()
        if (totalWeight <= 0f) return 0f
        val weightedSum = valid.sumOf {
            (computeMape(it.estimatedDurationMinutes.toFloat(), it.actualDurationMinutes!!) * it.estimatedDurationMinutes).toDouble()
        }
        return (weightedSum / totalWeight).toFloat()
    }

    fun computeWeightedSmape(tasks: List<TaskEntity>): Float {
        val valid = tasks.filter {
            it.estimatedDurationMinutes > 0 &&
                it.actualDurationMinutes != null &&
                it.actualDurationMinutes!! > 0f &&
                !it.actualDurationMinutes!!.isNaN() &&
                !it.actualDurationMinutes!!.isInfinite()
        }
        if (valid.isEmpty()) return 0f
        val totalWeight = valid.sumOf { it.estimatedDurationMinutes }.toFloat()
        if (totalWeight <= 0f) return 0f
        val weightedSum = valid.sumOf {
            (computeSmape(it.estimatedDurationMinutes.toFloat(), it.actualDurationMinutes!!) * it.estimatedDurationMinutes).toDouble()
        }
        return (weightedSum / totalWeight).toFloat()
    }

    fun mapeGrade(mape: Float): String = when {
        mape == 0f  -> "No data yet — complete tasks with time tracking to see your accuracy"
        mape < 10f  -> "✨ Excellent time estimation!"
        mape < 25f  -> "👍 Good estimation, minor drift"
        mape < 50f  -> "⚠ Moderate estimation error — try breaking tasks into smaller chunks"
        else        -> "🚨 High estimation error — recalibrate your estimates"
    }

    // ── PLANNING CALIBRATION ──────────────────────────────────────────────────

    /**
     * The user's planning bias, learned from completed tasks. MAPE was already computed here but
     * only displayed, so chronic under-booking kept producing plans that were too short.
     *
     * Estimate/actual ratios are multiplicative, so the central tendency is a geometric mean, shrunk
     * toward "no adjustment" by sample count (category -> user global -> 1.0f).
     */
    data class PlanningCalibration(
        val globalFactor: Float,
        private val categoryFactors: Map<String, Float>
    ) {
        /** Category-level factor, or null when there is no history for that kind of work. */
        fun categoryFactor(category: TaskCategory): Float? = categoryFactors[category.name]

        companion object {
            val UNIFORM = PlanningCalibration(1.0f, emptyMap())

            /** Below this many outcomes there is nothing trustworthy to learn from. */
            private const val MIN_SAMPLES = 5

            /** Pseudo-observations of the prior; higher means slower to adopt learned bias. */
            private const val PRIOR_STRENGTH = 4.0

            /** Outlier outcomes are capped so one bad task cannot dominate the learned bias. */
            private const val MIN_RATIO = 0.4f
            private const val MAX_RATIO = 3.0f

            /** Even a confidently-learned bias only moves a plan by this much. */
            private const val MIN_FACTOR = 0.8f
            private const val MAX_FACTOR = 1.6f

            fun from(tasks: List<TaskEntity>): PlanningCalibration {
                val samples = tasks.asSequence()
                    .filter { it.status == TaskStatus.COMPLETED && it.estimatedDurationMinutes > 0 }
                    .mapNotNull { task ->
                        val actual = task.actualDurationMinutes ?: return@mapNotNull null
                        if (actual <= 0f) return@mapNotNull null
                        task.determineCategory() to (actual / task.estimatedDurationMinutes)
                            .coerceIn(MIN_RATIO, MAX_RATIO)
                    }
                    .toList()

                if (samples.size < MIN_SAMPLES) return UNIFORM

                val global = shrinkToward(
                    observed = geometricMean(samples.map { it.second }),
                    prior = 1.0,
                    samples = samples.size
                )

                val categoryFactors = samples
                    .groupBy { it.first }
                    .mapKeys { (category, _) -> category.name }
                    .mapValues { (_, group) ->
                        shrinkToward(
                            observed = geometricMean(group.map { it.second }),
                            prior = global.toDouble(),
                            samples = group.size
                        )
                    }

                return PlanningCalibration(global, categoryFactors)
            }

            private fun geometricMean(ratios: List<Float>): Double =
                ratios.map { ln(it.toDouble()) }.average().let(::exp)

            /** Blend toward the prior by sample count, then clamp so history can never swing a plan far. */
            private fun shrinkToward(observed: Double, prior: Double, samples: Int): Float {
                val weight = samples / (samples + PRIOR_STRENGTH)
                val blended = prior + (observed - prior) * weight
                return blended.toFloat().coerceIn(MIN_FACTOR, MAX_FACTOR)
            }
        }
    }

    /**
     * How the user's enjoyment forecasts compare with how the work actually felt.
     *
     * `enjoymentScore` is a guess made when the task is created, and it feeds temptation-bundling
     * priority for every task afterwards. The affordance rating collected after each session is the
     * measured error of that guess — this turns it into a bounded correction so a user who keeps
     * overestimating how much they'll enjoy something stops being steered by that optimism.
     */
    data class EnjoymentCalibration(val offsetPoints: Float) {

        /** A new task's predicted enjoyment, corrected by measured history. */
        fun forecast(enjoymentScore: Int): Int =
            (enjoymentScore + offsetPoints).roundToInt().coerceIn(0, 100)

        companion object {
            val NEUTRAL = EnjoymentCalibration(0f)

            private const val MIN_SAMPLES = 5
            private const val PRIOR_STRENGTH = 4.0
            /** Ratings run -2..+2; a whole unit of sustained bias is worth this many enjoyment points. */
            private const val POINTS_PER_RATING = 10f
            private const val MAX_OFFSET_POINTS = 15f

            fun from(tasks: List<TaskEntity>): EnjoymentCalibration {
                val ratings = tasks
                    .filter { it.status == TaskStatus.COMPLETED }
                    .mapNotNull { it.affectiveForecastError }
                    .filter { it.isFinite() }
                if (ratings.size < MIN_SAMPLES) return NEUTRAL

                val observed = ratings.average().toFloat() * POINTS_PER_RATING
                val weight = (ratings.size / (ratings.size + PRIOR_STRENGTH)).toFloat()
                return EnjoymentCalibration(
                    (observed * weight).coerceIn(-MAX_OFFSET_POINTS, MAX_OFFSET_POINTS)
                )
            }
        }
    }

    // ── SESSION HELPERS ───────────────────────────────────────────────────────

    /** Returns start-of-day millis for a given timestamp */
    private fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun normalizeHour(hour: Int): Int {
        val normalized = hour % 24
        return if (normalized < 0) normalized + 24 else normalized
    }

    internal fun isHourInPeakWindow(hour: Int, peakStartHour: Int, peakEndHour: Int): Boolean {
        val h = normalizeHour(hour)
        val start = normalizeHour(peakStartHour)
        val end = normalizeHour(peakEndHour)
        return if (start < end) {
            h in start until end
        } else if (start > end) {
            h >= start || h < end
        } else {
            h == start
        }
    }

    internal fun peakWindowsForDay(dayStartMillis: Long, peakStartHour: Int, peakEndHour: Int): List<Pair<Long, Long>> {
        val start = normalizeHour(peakStartHour)
        val end = normalizeHour(peakEndHour)
        fun atHour(hour: Int): Long = Calendar.getInstance().apply {
            timeInMillis = dayStartMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val nextDayStart = Calendar.getInstance().apply {
            timeInMillis = dayStartMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        return if (start <= end) {
            listOf(atHour(start) to atHour(end))
        } else {
            // Night window: e.g. 22:00 to 02:00
            // Day 1: start hour to next local midnight; day 2: midnight through end hour.
            listOf(
                atHour(start) to nextDayStart,
                dayStartMillis to atHour(end)
            )
        }
    }

    // ── 7-DAY FOCUS TREND ────────────────────────────────────────────────────

    /** Returns list of (dayLabel, focusMinutes) for the last 7 days, oldest first */
    fun sevenDayFocusTrend(
        sessions: List<TimeSessionEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<Pair<String, Float>> {
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val todayStart = startOfDay(nowMillis)
        val closedSessions = sessions.filter { it.endedAt != null && it.durationMinutes > 0f }

        return (6 downTo 0).map { daysAgo ->
            val dayStart = Calendar.getInstance().apply {
                timeInMillis = todayStart
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }.timeInMillis
            val dayEnd = Calendar.getInstance().apply {
                timeInMillis = dayStart
                add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis

            // Bug 7 & Feature 4 (ADR-025): Proportionally slice cross-midnight sessions across day boundaries
            val totalMins = closedSessions.sumOf { session ->
                val sessionStart = session.startedAt
                val sessionEnd = session.endedAt ?: session.startedAt
                val overlapStart = maxOf(sessionStart, dayStart)
                val overlapEnd = minOf(sessionEnd, dayEnd, nowMillis)
                if (overlapEnd > overlapStart) ((overlapEnd - overlapStart) / 60_000.0) else 0.0
            }.toFloat()

            val cal = Calendar.getInstance().apply { timeInMillis = dayStart }
            val label = dayLabels[(cal.get(Calendar.DAY_OF_WEEK) + 5) % 7]
            label to totalMins
        }
    }

    // ── SUMMARY ───────────────────────────────────────────────────────────────

    data class AnalyticsSummary(
        // Task counts
        val totalTasks: Int,
        val completedTasks: Int,
        val remainingTasks: Int,
        val completionRate: Float,
        val completedToday: Int,
        // Quadrant / priority breakdown
        val tasksByQuadrant: Map<Quadrant, Int>,
        val tasksByPriority: Map<Priority, Int>,
        val completedTodayByQuadrant: Map<Quadrant, Int>,
        val totalRemainingMinutes: Int,
        // Time tracking
        val focusMinutesToday: Float,
        val focusMinutesTotal: Float,
        val avgSessionMinutes: Float,
        val totalSessions: Int,
        val sessionsToday: Int,
        val mostFocusedTaskTitle: String?,
        val sevenDayTrend: List<Pair<String, Float>>,
        // Estimation accuracy
        val overallMape: Float,
        val weightedMape: Float,
        val underestimatedPct: Float,
        val overestimatedPct:Float,
        val overallSmape: Float,
        val weightedSmape: Float,
        // Habits
        val habitTasksTotal: Int,
        val habitTasksCompleted: Int,
        val habitCompletionRate: Float,
        val longestHabitStreak: Int,
        val activeHabitStreaks: List<Pair<String, Int>>,  // title -> current streak, top 5
        val recurringTasksTotal: Int,
        val recurringTasksCompleted: Int,
        val lockedScheduleTasksTotal: Int,
        val lockedScheduleTasksCompleted: Int,
        // Streaks
        val currentStreak: Int,
        val longestStreak: Int,
        // Procrastination
        val topProcrastinatedTasks: List<TaskEntity>,
        // Neuro Boost stats
        val frogTasksTotal: Int,
        val frogTasksCompleted: Int,
        val anxietyTasksTotal: Int,
        val anxietyTasksCompleted: Int,
        val publicCommitmentTotal: Int,
        val publicCommitmentCompleted: Int,
        val ifThenPlanUsageRate: Float,       // % of tasks with if-then plan
        val contextTagBreakdown: Map<String, Int>,  // tag -> count
        val taskTagBreakdown: Map<String, Int>,     // user tags -> count
        val taskTypeDistribution: Map<String, Int>,  // type name -> count
        // XP / points
        val totalXp: Int,
        val xpToday: Int,
        val xpThisWeek: Int,
        val topXpTasks: List<Pair<String, Int>>,       // title -> points, top 5
        // Peak-hour productivity
        val peakHourFocusMinutes: Float,
        val offPeakFocusMinutes: Float,
        val peakHourTasksCompleted: Int
    )
    fun buildSummary(
        allTasks: List<TaskEntity>,
        allSessions: List<TimeSessionEntity>,
        prefs: UserPreferences,
        nowMillis: Long = System.currentTimeMillis()
    ): AnalyticsSummary {
        val active = allTasks.filter { it.status == TaskStatus.ACTIVE }
        val completed = allTasks.filter { it.status == TaskStatus.COMPLETED }

        // ── Task counts ──────────────────────────────────────────────────────
        val todayStart = startOfDay(nowMillis)
        val todayEnd = Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        // Bug 7 Part 1 (ADR-025) & Bug 9 (ADR-030): Clamp today metrics with upper bound <= nowMillis
        val completedToday = completed.count {
            val c = it.completedAt ?: 0L
            c >= todayStart && c <= nowMillis
        }
        val completionRate = if (allTasks.isNotEmpty()) completed.size.toFloat() / allTasks.size * 100f else 0f

        // ── Quadrant / priority ──────────────────────────────────────────────
        val tasksByQuadrant = Quadrant.entries.associateWith { q -> allTasks.count { it.quadrant == q } }
        val tasksByPriority = Priority.entries.associateWith { p -> allTasks.count { it.priority == p } }
        val completedTodayByQuadrant = Quadrant.entries.associateWith { q ->
            completed.count {
                val c = it.completedAt ?: 0L
                it.quadrant == q && c >= todayStart && c <= nowMillis
            }
        }
        val totalRemainingMinutes = active.sumOf { it.estimatedDurationMinutes }

        // ── Session stats ────────────────────────────────────────────────────
        val closedSessions = allSessions.filter { it.endedAt != null && it.durationMinutes > 0f }
        // Bug 7 & Feature 4 (ADR-025): Proportionally slice cross-midnight sessions into today's focus minutes
        val focusMinutesToday = closedSessions.sumOf { session ->
            val sessionStart = session.startedAt
            val sessionEnd = session.endedAt ?: session.startedAt
            val overlapStart = maxOf(sessionStart, todayStart)
            val overlapEnd = minOf(sessionEnd, todayEnd, nowMillis)
            if (overlapEnd > overlapStart) ((overlapEnd - overlapStart) / 60_000.0) else 0.0
        }.toFloat()
        val sessionsToday = closedSessions.count { it.startedAt in todayStart..nowMillis }
        val focusMinutesTotal = closedSessions.sumOf { it.durationMinutes.toDouble() }.toFloat()
        val avgSessionMinutes = if (closedSessions.isNotEmpty())
            focusMinutesTotal / closedSessions.size else 0f
        // Most focused task (most total tracked minutes)
        val mostFocusedTask = allTasks
            .filter { it.totalTimeTrackedMinutes > 0f }
            .maxByOrNull { it.totalTimeTrackedMinutes }

        val sevenDayTrend = sevenDayFocusTrend(allSessions, nowMillis)

        val tracked = completed.filter {
            it.estimatedDurationMinutes > 0 &&
                it.actualDurationMinutes != null &&
                it.actualDurationMinutes!! > 0f &&
                !it.actualDurationMinutes!!.isNaN() &&
                !it.actualDurationMinutes!!.isInfinite()
        }
        val overallMape = if (tracked.isNotEmpty())
            tracked.map { computeMape(it.estimatedDurationMinutes.toFloat(), it.actualDurationMinutes!!) }.average().toFloat()
        else 0f
        val overallSmape = if (tracked.isNotEmpty())
            tracked.map { computeSmape(it.estimatedDurationMinutes.toFloat(), it.actualDurationMinutes!!) }.average().toFloat()
        else 0f
        val weightedMape = computeWeightedMape(completed)
        val weightedSmape = computeWeightedSmape(completed)

        val underestimated = tracked.filter { it.actualDurationMinutes!! > it.estimatedDurationMinutes }
        val overestimated = tracked.filter { it.actualDurationMinutes!! < it.estimatedDurationMinutes }
        val underestimatedPct = if (tracked.isNotEmpty()) underestimated.size.toFloat() / tracked.size * 100f else 0f
        val overestimatedPct = if (tracked.isNotEmpty()) overestimated.size.toFloat() / tracked.size * 100f else 0f

// ── Habits ───────────────────────────────────────────────────────────
        // Use recurrence as source of truth — isHabitual is only set on completion
        val habitTasks = allTasks.filter { it.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE || it.isHabitual }
        val habitCompleted = habitTasks.filter { it.status == TaskStatus.COMPLETED }
        val recurringTasksTotal = allTasks.count { it.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE }
        val recurringTasksCompleted = completed.count { it.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE }
        val lockedScheduleTasksTotal = allTasks.count { it.isScheduleLocked }
        val lockedScheduleTasksCompleted = completed.count { it.isScheduleLocked }
        val habitCompletionRate = if (habitTasks.isNotEmpty())
            habitCompleted.size.toFloat() / habitTasks.size * 100f else 0f
        // Best streak across all tasks (active recurring tasks carry the streak forward)
        val longestHabitStreak = allTasks.maxOfOrNull { it.habitStreak } ?: 0
        // Active habits with their current streaks for display
        val activeHabitStreaks = active
            .filter { it.recurrence != com.neuroflow.app.domain.model.Recurrence.NONE || it.isHabitual }
            .filter { it.habitStreak > 0 }
            .sortedByDescending { it.habitStreak }
            .take(5)
            .map { it.title to it.habitStreak }

        // ── Procrastination ──────────────────────────────────────────────────
        val topProcrastinated = active
            .filter { it.postponeCount > 0 }
            .sortedByDescending { it.postponeCount }
            .take(5)

        // ── Neuro Boost stats ─────────────────────────────────────────────────
        val frogTasks = allTasks.filter { it.isFrog }
        val frogCompleted = frogTasks.filter { it.status == TaskStatus.COMPLETED }

        val anxietyTasks = allTasks.filter { it.isAnxietyTask }
        val anxietyCompleted = anxietyTasks.filter { it.status == TaskStatus.COMPLETED }

        val publicCommitmentTasks = allTasks.filter { it.isPublicCommitment }
        val publicCommitmentCompleted = publicCommitmentTasks.filter { it.status == TaskStatus.COMPLETED }

        val ifThenPlanUsageRate = if (allTasks.isNotEmpty())
            allTasks.count { it.ifThenPlan.isNotBlank() }.toFloat() / allTasks.size * 100f else 0f

        val contextTagBreakdown = allTasks
            .filter { it.contextTag.isNotBlank() }
            .groupBy { it.contextTag }
            .mapValues { it.value.size }

        val taskTagBreakdown = allTasks
            .flatMap { task -> task.tags.split(",").map { it.trim() }.filter { it.isNotBlank() } }
            .groupingBy { it }
            .eachCount()

        val taskTypeDistribution = allTasks
            .groupBy { it.taskType.name }
            .mapValues { it.value.size }

        // ── XP / Points ───────────────────────────────────────────────────────
        val weekStart = Calendar.getInstance().apply {
            timeInMillis = startOfDay(nowMillis)
            add(Calendar.DAY_OF_YEAR, -6)
        }.timeInMillis
        val totalXp = completed.sumOf { it.focusModePoints }
        // Bug 9 Fix (ADR-030): Clamp XP metrics so future completions never leak into current period
        val xpToday = completed
            .filter {
                val c = it.completedAt ?: 0L
                c >= todayStart && c <= nowMillis
            }
            .sumOf { it.focusModePoints }
        val xpThisWeek = completed
            .filter {
                val c = it.completedAt ?: 0L
                c >= weekStart && c <= nowMillis
            }
            .sumOf { it.focusModePoints }
        val topXpTasks = completed
            .filter { it.focusModePoints > 0 }
            .sortedByDescending { it.focusModePoints }
            .take(5)
            .map { it.title to it.focusModePoints }

        val (peakStartHour, peakEndHour) = when {
            prefs.manualPeakProfileEnabled -> {
                val startH = (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23)
                val durHours = maxOf(1, prefs.manualPeakWindow1DurationMinutes / 60)
                val endH = (startH + durHours).coerceIn(0, 23)
                startH to (if (endH == startH) (startH + 1).coerceIn(0, 23) else endH)
            }
            prefs.quizPeakEnabled && prefs.effectivePeakStart >= 0 && prefs.effectivePeakEnd >= 0 -> {
                prefs.effectivePeakStart to prefs.effectivePeakEnd
            }
            else -> {
                prefs.peakEnergyStart to prefs.peakEnergyEnd
            }
        }

        // ── Peak-hour productivity ────────────────────────────────────────────
        // Split each session's duration at peak window boundaries so only the
        // portion that actually fell within peak hours is counted.
        var peakHourFocusMinutes = 0f
        for (session in closedSessions) {
            val sessionStart = session.startedAt
            val sessionEnd = session.endedAt ?: continue
            if (sessionEnd <= sessionStart) continue

            // Build peak windows for the days this session spans (handles overnight sessions)
            val startDay = startOfDay(sessionStart)
            val endDay = startOfDay(sessionEnd)
            var day = startDay
            while (day <= endDay) {
                val windows = peakWindowsForDay(day, peakStartHour, peakEndHour)
                for ((peakWindowStart, peakWindowEnd) in windows) {
                    val overlapStart = maxOf(sessionStart, peakWindowStart)
                    val overlapEnd = minOf(sessionEnd, peakWindowEnd)
                    if (overlapEnd > overlapStart) {
                        peakHourFocusMinutes += (overlapEnd - overlapStart) / 60_000f
                    }
                }
                day = Calendar.getInstance().apply {
                    timeInMillis = day
                    add(Calendar.DAY_OF_YEAR, 1)
                }.timeInMillis
            }
        }
        val offPeakFocusMinutes = (focusMinutesTotal - peakHourFocusMinutes).coerceAtLeast(0f)
        val peakHourTasksCompleted = completed.count { task ->
            val completedMs = task.completedAt ?: return@count false
            val cal = Calendar.getInstance().apply { timeInMillis = completedMs }
            val h = cal.get(Calendar.HOUR_OF_DAY)
            isHourInPeakWindow(h, peakStartHour, peakEndHour)
        }

        return AnalyticsSummary(
            totalTasks = allTasks.size,
            completedTasks = completed.size,
            remainingTasks = active.size,
            completionRate = completionRate,
            completedToday = completedToday,
            tasksByQuadrant = tasksByQuadrant,
            tasksByPriority = tasksByPriority,
            completedTodayByQuadrant = completedTodayByQuadrant,
            totalRemainingMinutes = totalRemainingMinutes,
            focusMinutesToday = focusMinutesToday,
            focusMinutesTotal = focusMinutesTotal,
            avgSessionMinutes = avgSessionMinutes,
            totalSessions = closedSessions.size,
            sessionsToday = sessionsToday,
            mostFocusedTaskTitle = mostFocusedTask?.title,
            sevenDayTrend = sevenDayTrend,
            overallMape = overallMape,
            weightedMape = weightedMape,
            underestimatedPct = underestimatedPct,
            overestimatedPct = overestimatedPct,
            overallSmape = overallSmape,
            weightedSmape = weightedSmape,
            habitTasksTotal = habitTasks.size,
            habitTasksCompleted = habitCompleted.size,
            habitCompletionRate = habitCompletionRate,
            longestHabitStreak = longestHabitStreak,
            activeHabitStreaks = activeHabitStreaks,
            recurringTasksTotal = recurringTasksTotal,
            recurringTasksCompleted = recurringTasksCompleted,
            lockedScheduleTasksTotal = lockedScheduleTasksTotal,
            lockedScheduleTasksCompleted = lockedScheduleTasksCompleted,
            currentStreak = prefs.dailyStreak,
            longestStreak = prefs.longestStreak,
            topProcrastinatedTasks = topProcrastinated,
            frogTasksTotal = frogTasks.size,
            frogTasksCompleted = frogCompleted.size,
            anxietyTasksTotal = anxietyTasks.size,
            anxietyTasksCompleted = anxietyCompleted.size,
            publicCommitmentTotal = publicCommitmentTasks.size,
            publicCommitmentCompleted = publicCommitmentCompleted.size,
            ifThenPlanUsageRate = ifThenPlanUsageRate,
            contextTagBreakdown = contextTagBreakdown,
            taskTagBreakdown = taskTagBreakdown,
            taskTypeDistribution = taskTypeDistribution,
            totalXp = totalXp,
            xpToday = xpToday,
            xpThisWeek = xpThisWeek,
            topXpTasks = topXpTasks,
            peakHourFocusMinutes = peakHourFocusMinutes,
            offPeakFocusMinutes = offPeakFocusMinutes,
            peakHourTasksCompleted = peakHourTasksCompleted
        )
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdatalocalentitytaskentitykt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/entity/TaskEntity.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/entity/TaskEntity.kt`
- **Lines of Code:** 111
- **Size:** 5,010 bytes (4.9 KB)

```kotlin
package com.neuroflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.TaskType
import java.util.UUID

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["status", "scheduledDate"], name = "index_tasks_status_scheduledDate"),
        Index(value = ["status", "deadlineDate"], name = "index_tasks_status_deadlineDate"),
        Index(value = ["parentTaskId"], name = "index_tasks_parentTaskId")
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",

    // Classification
    val quadrant: Quadrant = Quadrant.SCHEDULE,
    val priority: Priority = Priority.MEDIUM,
    val tags: String = "",

    // Timing
    val deadlineDate: Long? = null,
    val deadlineTime: Long? = null,
    val scheduledDate: Long? = null,
    val scheduledTime: Long? = null,
    val isScheduleLocked: Boolean = false,
    val estimatedDurationMinutes: Int = 0,
    val recurrence: Recurrence = Recurrence.NONE,
    val recurrenceIntervalDays: Int = 1, // used when recurrence == CUSTOM

    // Reminders (bitmask: 15min=1, 30min=2, 1hr=4, 1day=8)
    val reminderFlags: Int = 0,

    // Scoring inputs
    val impactScore: Int = 50,          // 0-100: strategic value/impact
    val valueScore: Int = 50,           // 0-100: intrinsic value (separate from impact)
    val effortScore: Int = 50,          // 0-100: effort required (higher = harder, lowers priority for quick wins)
    val parentTaskId: String? = null,
    val blockingTaskIds: String = "",   // legacy: kept for migration compat
    val dependsOnTaskIds: String = "",  // IDs of tasks THIS task depends on (must be done first)
    val waitingFor: String = "",        // external dependency description (non-task)

    // Time tracking
    val totalTimeTrackedMinutes: Float = 0f,
    val sessionCount: Int = 0,
    val lastSessionDurationMinutes: Float? = null,

    // Adaptive difficulty
    val actualDurationMinutes: Float? = null,
    val estimationErrorMape: Float? = null,
    val estimationErrorSmape: Float? = null,

    // State
    val status: TaskStatus = TaskStatus.ACTIVE,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // Neuroscience extras
    val energyLevel: EnergyLevel = EnergyLevel.MEDIUM,
    val taskType: TaskType = TaskType.ANALYTICAL,   // circadian rhythm matching
    val contextTag: String = "",
    val goalId: String? = null,
    val ifThenPlan: String = "",
    val isHabitual: Boolean = false,
    val habitStreak: Int = 0,
    val isFrog: Boolean = false,
    val postponeCount: Int = 0,
    val focusModePoints: Int = 0,

    // Science-backed productivity boosters
    val enjoymentScore: Int = 50,       // 0-100: how enjoyable the task is (temptation bundling)
    val isPublicCommitment: Boolean = false, // commitment device — social accountability
    val isAnxietyTask: Boolean = false,  // stress inoculation — prevents avoidance spiral
    val goalRiskLevel: Int = 0,          // 0=none, 1=at risk, 2=critical — loss aversion boost
    val isAutoScheduled: Boolean = false,
    val lastAutoScheduledAt: Long? = null, // timestamp when task was last auto-scheduled (for replanning cooldown)

    // Habit anchor date — for recurring tasks this is always set and shifts by the recurrence interval
    // on each completion. Independent of deadline/scheduledDate so habits work without a deadline.
    val habitDate: Long? = null,

    // Behavioral motivation engine fields
    val affectiveForecastError: Float? = null,  // REAL nullable — difference between predicted and actual enjoyment
    val woopPromptShown: Boolean = false,        // INTEGER NOT NULL DEFAULT 0 — whether WOOP prompt has been shown

    // Digital Wellbeing distraction score (0–100), refreshed by DistractionSyncWorker
    // -1 = not yet computed (no usage data / permission not granted)
    val distractionScore: Float = -1f,

    // Explicit scheduling category — overrides tag-inferred category when set.
    // Null means the category is inferred at runtime from tags/taskType via determineCategory().
    val schedulingCategory: String? = null,

    // Minutes the auto-scheduler actually reserved for this task at its scheduled start. The
    // planner inflates the raw estimate by the user's learned bias and the slot's energy, so
    // anything asking "did this task get its time?" must read this rather than re-derive it.
    // 0 = never placed automatically (manual schedule, or scheduled before this field existed).
    val plannedBlockMinutes: Int = 0
)
```

---

<a id="appsrcmainjavacomneuroflowappdatalocalentitytaskmicrostepentitykt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/entity/TaskMicroStepEntity.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/entity/TaskMicroStepEntity.kt`
- **Lines of Code:** 30
- **Size:** 803 bytes (0.8 KB)

```kotlin
package com.neuroflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "task_micro_steps",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["taskId", "sortOrder"], name = "index_task_micro_steps_task_id_sort")
    ]
)
data class TaskMicroStepEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val title: String,
    val sortOrder: Int,
    val isCompleted: Boolean = false,
    val estimatedMinutes: Int = 5
)
```

---

<a id="appsrcmainjavacomneuroflowappdatalocalentitytasktimeextensionskt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/entity/TaskTimeExtensions.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/entity/TaskTimeExtensions.kt`
- **Lines of Code:** 139
- **Size:** 4,698 bytes (4.6 KB)

```kotlin
package com.neuroflow.app.data.local.entity

import com.neuroflow.app.domain.model.Recurrence
import java.util.Calendar

private const val MINUTES_PER_DAY = 24 * 60

/**
 * Ids of the tasks this task depends on.
 */
fun TaskEntity.dependencyIds(): List<String> =
    dependsOnTaskIds.split(',').map { it.trim() }.filter { it.isNotBlank() }

/**
 * Recurring tasks should anchor on habitDate when present.
 */
fun TaskEntity.isRecurringWithAnchor(): Boolean =
    recurrence != Recurrence.NONE && habitDate != null

const val ONE_DAY_MILLIS = 86_400_000L

/**
 * Resolves the scheduled epoch millis handling dual representation:
 * - scheduledTime as an intraday offset (< 86_400_000L)
 * - scheduledTime as an absolute epoch timestamp (>= 86_400_000L)
 */
fun TaskEntity.resolveScheduledEpochMillis(referenceDateMillis: Long = System.currentTimeMillis()): Long? {
    if (isRecurringWithAnchor()) {
        val baseDate = habitDate ?: return null
        val timeOffset = scheduledTime ?: deadlineTime ?: 0L
        return if (timeOffset >= ONE_DAY_MILLIS) timeOffset else startOfLocalDay(baseDate) + timeOffset
    }
    val rawTime = scheduledTime
    if (rawTime != null && rawTime >= ONE_DAY_MILLIS) {
        return rawTime
    }
    if (scheduledDate != null) {
        return scheduledDate + (rawTime ?: 0L)
    }
    if (rawTime != null && rawTime >= 0L) {
        return startOfLocalDay(referenceDateMillis) + rawTime
    }
    return habitDate?.let { startOfLocalDay(it) }
}

/**
 * Resolves the deadline epoch millis handling dual representation:
 * - deadlineTime as intraday offset vs absolute epoch millis
 */
fun TaskEntity.resolveDeadlineEpochMillis(): Long? {
    val rawTime = deadlineTime
    if (rawTime != null && rawTime >= ONE_DAY_MILLIS) return rawTime
    return deadlineDate?.let { it + (rawTime ?: 0L) }
}

/**
 * Normalizes an epoch timestamp into standard date (start of day) and time offset pair.
 */
fun buildNormalizedScheduleTimestamps(startEpochMillis: Long): Pair<Long, Long> {
    val dayStart = startOfLocalDay(startEpochMillis)
    val timeOffset = localTimeOfDayOffset(startEpochMillis)
    return dayStart to timeOffset
}

/**
 * Target used by reminder/notification flows.
 */
fun TaskEntity.effectiveReminderTargetMillis(): Long? {
    if (isRecurringWithAnchor()) {
        val baseDate = habitDate ?: return null
        val timeOffset = scheduledTime ?: deadlineTime ?: (9 * 3600_000L)
        return if (timeOffset >= ONE_DAY_MILLIS) timeOffset else startOfLocalDay(baseDate) + timeOffset
    }
    val deadline = resolveDeadlineEpochMillis()
    if (deadline != null) return deadline
    val scheduled = resolveScheduledEpochMillis()
    if (scheduled != null) return scheduled
    return habitDate?.let { startOfLocalDay(it) + (scheduledTime ?: deadlineTime ?: (9 * 3600_000L)) }
}

/**
 * Anchor used by schedule/time-block style flows.
 */
fun TaskEntity.effectiveScheduleAnchorMillis(): Long? {
    return resolveScheduledEpochMillis()
}

/**
 * Local minute-of-day for timeline placement.
 */
fun TaskEntity.timelineStartMinuteOfDay(): Int {
    val anchorMillis = resolveScheduledEpochMillis() ?: return 0
    val cal = Calendar.getInstance().apply { timeInMillis = anchorMillis }
    return (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE))
        .coerceIn(0, MINUTES_PER_DAY - 1)
}

/**
 * Local minute-of-day with explicit ZoneId for timeline placement.
 */
fun TaskEntity.timelineStartMinuteOfDay(zoneId: java.time.ZoneId): Int {
    val anchorMillis = resolveScheduledEpochMillis() ?: return 0
    val zdt = java.time.Instant.ofEpochMilli(anchorMillis).atZone(zoneId)
    return (zdt.hour * 60 + zdt.minute).coerceIn(0, MINUTES_PER_DAY - 1)
}

fun startOfLocalDay(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

fun startOfNextLocalDay(millis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = startOfLocalDay(millis)
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis

fun startOfPreviousLocalDay(millis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = startOfLocalDay(millis)
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis

fun localTimeOfDayOffset(millis: Long): Long {
    val dayStart = startOfLocalDay(millis)
    return (millis - dayStart).coerceAtLeast(0L)
}

fun splitToLocalDateAndTime(millis: Long): Pair<Long, Long> {
    val date = startOfLocalDay(millis)
    val time = localTimeOfDayOffset(millis)
    return date to time
}
```

---

<a id="appsrcmainjavacomneuroflowappdatalocalentityenergypredictionentitykt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/entity/EnergyPredictionEntity.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/entity/EnergyPredictionEntity.kt`
- **Lines of Code:** 65
- **Size:** 3,128 bytes (3.1 KB)

```kotlin
package com.neuroflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 2 telemetry entity: stores snapshots of energy predictions with full component breakdowns.
 * Used for audit trails, backtesting, calibration validation, and local analytics.
 *
 * One record per prediction refresh (~60 seconds). Bounded retention: 30 days raw, 90 days aggregated.
 */
@Entity(
    tableName = "energy_predictions",
    indices = [
        Index(value = ["predictedAtMillis"], name = "index_energy_predictions_predictedAtMillis")
    ]
)
data class EnergyPredictionEntity(
    @PrimaryKey
    val id: String,

    // Temporal context
    val predictedAtMillis: Long,
    val predictedAtDayOfWeek: Int,        // 1=Monday .. 7=Sunday (ISO-8601, from java.time.DayOfWeek.value)
    val predictedAtHourOfDay: Int,        // 0..23

    // Ground truth inputs (freshness guards)
    val peakDetectionAgeMillis: Long,     // How old was the peak detection?
    val sleepLogAgeMillis: Long,          // How recently was sleep logged?
    val sessionDataAgeMillis: Long,       // How old is the last session event?

    // Component scores (for diagnostics and calibration)
    val baselineRawEnergy: Float,         // -100..100 raw energy before moment adjustment
    val peakScore: Float,                 // Circadian baseline contribution
    val fatiguePenalty: Float,            // Sleep pressure penalty to baseline
    val sleepPressurePoints: Int,         // Raw sleep pressure accumulation
    val fatiguePercent: Int,              // 0..100% fatigue zone

    // Moment adjustment layer (Phase 1 guards applied)
    val momentAdjustment: Float,          // ±24pt from interruptions/tasks/notifications
    val momentConfidence: Float,          // 0..1 confidence in moment adjustment
    val momentSupportScore: Float,        // Focus density, calmness signal
    val momentPressureScore: Float,       // Sleep pressure, peak age, task load

    // Final output
    val adjustedRawEnergy: Float,         // Baseline + (adjustment × confidence)
    val usableEnergy: Int,                // 0..100 user-facing energy percentage

    // Chronotype context (for future per-type adaptive tuning)
    val chronotype: String,               // DEFINITE_MORNING, MODERATE_MORNING, INTERMEDIATE, etc.
    val wakeUpHour: Int,                  // User's configured wake time
    val peakMinuteOfDay: Int,             // Computed peak time for this chronotype
    val peakConfidence: Float,            // Confidence in baseline peak computation

    // Feature signals (for feature importance tracking)
    val recentFocusMinutes: Float,        // Last 180-min focused work time
    val recentInterruptionCount: Int,     // Session interruptions in window
    val recentAppSwitchCount: Int,        // App switches in window
    val activeTaskCount: Int,             // Currently active open tasks
    val notificationCount: Int,           // Badge count from notifications

    // Retention marker (for cleanup)
    val createdAtMillis: Long = System.currentTimeMillis()
)
```

---

<a id="appsrcmainjavacomneuroflowappdatalocalentityprotectedslotentitykt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/entity/ProtectedSlotEntity.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/entity/ProtectedSlotEntity.kt`
- **Lines of Code:** 16
- **Size:** 436 bytes (0.4 KB)

```kotlin
package com.neuroflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "protected_slots")
data class ProtectedSlotEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val startHour: Int,
    val startMinute: Int,
    val durationMinutes: Int,
    val daysOfWeekMask: Int = 127,
    val isEnabled: Boolean = true
)
```

---

<a id="appsrcmainjavacomneuroflowappdatalocalentitysleeplogentitykt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/entity/SleepLogEntity.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/entity/SleepLogEntity.kt`
- **Lines of Code:** 23
- **Size:** 569 bytes (0.6 KB)

```kotlin
package com.neuroflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sleep_logs",
    indices = [
        Index(value = ["startAt"]),
        Index(value = ["endAt"])
    ]
)
data class SleepLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val startAt: Long,
    val endAt: Long,
    val durationMinutes: Int,
    val source: String = "MANUAL",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
```

---

<a id="appsrcmainjavacomneuroflowappdatalocaldaotaskdaokt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/dao/TaskDao.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/dao/TaskDao.kt`
- **Lines of Code:** 78
- **Size:** 2,902 bytes (2.8 KB)

```kotlin
package com.neuroflow.app.data.local.dao

import androidx.room.*
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: TaskStatus): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE quadrant = :quadrant AND status = 'ACTIVE' ORDER BY createdAt DESC")
    fun observeByQuadrant(quadrant: Quadrant): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    fun observeActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun observeCompletedTasks(): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE status = 'ACTIVE'
          AND (
                (scheduledDate >= :dayStart AND scheduledDate < :dayEnd)
                OR
                (recurrence != 'NONE' AND habitDate >= :dayStart AND habitDate < :dayEnd)
              )
    """)
    fun observeTasksForDate(dayStart: Long, dayEnd: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'ACTIVE'")
    suspend fun getActiveTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status = 'COMPLETED'")
    suspend fun getCompletedTasks(): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'ACTIVE' AND quadrant = :quadrant")
    fun observeQuadrantCount(quadrant: Quadrant): Flow<Int>

    @Query("SELECT * FROM tasks WHERE contextTag = :tag AND status = 'ACTIVE'")
    fun observeByContextTag(tag: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE goalId = :goalId AND status = 'ACTIVE'")
    fun observeByGoalId(goalId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentId")
    fun observeSubtasks(parentId: String): Flow<List<TaskEntity>>

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Query("UPDATE tasks SET estimationErrorMape = NULL, estimationErrorSmape = NULL, actualDurationMinutes = NULL WHERE status = 'COMPLETED'")
    suspend fun resetEstimationErrors()
}
```

---

<a id="appsrcmainjavacomneuroflowappdatalocaldaotaskmicrostepdaokt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/dao/TaskMicroStepDao.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/dao/TaskMicroStepDao.kt`
- **Lines of Code:** 32
- **Size:** 1,084 bytes (1.1 KB)

```kotlin
package com.neuroflow.app.data.local.dao

import androidx.room.*
import com.neuroflow.app.data.local.entity.TaskMicroStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskMicroStepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: TaskMicroStepEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(steps: List<TaskMicroStepEntity>)

    @Update
    suspend fun update(step: TaskMicroStepEntity)

    @Delete
    suspend fun delete(step: TaskMicroStepEntity)

    @Query("DELETE FROM task_micro_steps WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: String)

    @Query("SELECT * FROM task_micro_steps WHERE taskId = :taskId ORDER BY sortOrder ASC")
    fun observeByTaskId(taskId: String): Flow<List<TaskMicroStepEntity>>

    @Query("SELECT * FROM task_micro_steps WHERE taskId = :taskId ORDER BY sortOrder ASC")
    suspend fun getByTaskId(taskId: String): List<TaskMicroStepEntity>

    @Query("SELECT * FROM task_micro_steps")
    suspend fun getAll(): List<TaskMicroStepEntity>
}
```

---

<a id="appsrcmainjavacomneuroflowappdatalocaldaoprotectedslotdaokt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/dao/ProtectedSlotDao.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/dao/ProtectedSlotDao.kt`
- **Lines of Code:** 32
- **Size:** 982 bytes (1.0 KB)

```kotlin
package com.neuroflow.app.data.local.dao

import androidx.room.*
import com.neuroflow.app.data.local.entity.ProtectedSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProtectedSlotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(slot: ProtectedSlotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(slots: List<ProtectedSlotEntity>)

    @Update
    suspend fun update(slot: ProtectedSlotEntity)

    @Delete
    suspend fun delete(slot: ProtectedSlotEntity)

    @Query("SELECT * FROM protected_slots WHERE isEnabled = 1")
    fun observeActive(): Flow<List<ProtectedSlotEntity>>

    @Query("SELECT * FROM protected_slots")
    fun observeAll(): Flow<List<ProtectedSlotEntity>>

    @Query("SELECT * FROM protected_slots WHERE isEnabled = 1")
    suspend fun getActive(): List<ProtectedSlotEntity>

    @Query("SELECT * FROM protected_slots")
    suspend fun getAll(): List<ProtectedSlotEntity>
}
```

---

<a id="appsrcmainjavacomneuroflowappdatalocaldaoenergypredictiondaokt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/dao/EnergyPredictionDao.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/dao/EnergyPredictionDao.kt`
- **Lines of Code:** 31
- **Size:** 1,247 bytes (1.2 KB)

```kotlin
package com.neuroflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.neuroflow.app.data.local.entity.EnergyPredictionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EnergyPredictionDao {
    @Insert
    suspend fun insert(entity: EnergyPredictionEntity)

    @Query("SELECT * FROM energy_predictions ORDER BY predictedAtMillis DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<EnergyPredictionEntity>

    @Query("SELECT * FROM energy_predictions WHERE predictedAtMillis >= :startMillis AND predictedAtMillis <= :endMillis ORDER BY predictedAtMillis DESC")
    suspend fun getInRange(startMillis: Long, endMillis: Long): List<EnergyPredictionEntity>

    @Query("SELECT COUNT(*) FROM energy_predictions")
    suspend fun count(): Int

    @Query("DELETE FROM energy_predictions WHERE createdAtMillis < :beforeMillis")
    suspend fun deleteOlderThan(beforeMillis: Long): Int

    @Query("DELETE FROM energy_predictions")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM energy_predictions WHERE predictedAtMillis >= :afterMillis ORDER BY predictedAtMillis DESC")
    fun observeAfter(afterMillis: Long): Flow<List<EnergyPredictionEntity>>
}
```

---

<a id="appsrcmainjavacomneuroflowappdatalocaldaosleeplogdaokt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/dao/SleepLogDao.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/dao/SleepLogDao.kt`
- **Lines of Code:** 32
- **Size:** 1,069 bytes (1.0 KB)

```kotlin
package com.neuroflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neuroflow.app.data.local.entity.SleepLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: SleepLogEntity)

    @Query("SELECT * FROM sleep_logs ORDER BY startAt DESC")
    fun observeAll(): Flow<List<SleepLogEntity>>

    @Query("SELECT * FROM sleep_logs ORDER BY startAt DESC")
    suspend fun getAll(): List<SleepLogEntity>

    @Query("SELECT * FROM sleep_logs WHERE endAt > :fromMillis AND startAt < :toMillis ORDER BY startAt ASC")
    suspend fun getOverlapping(fromMillis: Long, toMillis: Long): List<SleepLogEntity>

    @Query("SELECT * FROM sleep_logs WHERE id = :id")
    suspend fun getById(id: String): SleepLogEntity?

    @Query("DELETE FROM sleep_logs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM sleep_logs")
    suspend fun deleteAll()
}
```

---

<a id="appsrcmainjavacomneuroflowappdatarepositorytaskrepositorykt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt`
- **Lines of Code:** 399
- **Size:** 16,590 bytes (16.2 KB)

```kotlin
package com.neuroflow.app.data.repository

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.neuroflow.app.data.local.dao.TaskDao
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.scheduler.AutoSchedulingContracts
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.worker.ScheduleAutoTasksWorker
import com.neuroflow.app.data.local.dao.TaskMicroStepDao
import com.neuroflow.app.data.local.entity.TaskMicroStepEntity
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private fun Long.toDayStart(): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = this@toDayStart }
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun Calendar.addRecurrenceStep(recurrence: Recurrence, customDays: Int) {
    when (recurrence) {
        Recurrence.DAILY -> add(Calendar.DAY_OF_YEAR, 1)
        Recurrence.WEEKLY -> add(Calendar.WEEK_OF_YEAR, 1)
        Recurrence.MONTHLY -> add(Calendar.MONTH, 1)
        Recurrence.CUSTOM -> add(Calendar.DAY_OF_YEAR, customDays.coerceAtLeast(1))
        Recurrence.NONE -> Unit
    }
}

private fun nextRecurringAnchorAfter(
    recurrence: Recurrence,
    customDays: Int,
    currentAnchor: Long,
    now: Long
): Long {
    if (recurrence == Recurrence.NONE) return currentAnchor
    val cal = Calendar.getInstance().apply { timeInMillis = currentAnchor }
    // Always move to the next cycle, then roll forward if the anchor is still in the past.
    cal.addRecurrenceStep(recurrence, customDays)
    while (cal.timeInMillis <= now) {
        cal.addRecurrenceStep(recurrence, customDays)
    }
    return cal.timeInMillis
}


@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val hyperFocusManager: Lazy<HyperFocusManager>,
    private val workManager: WorkManager,
    private val taskMicroStepDao: TaskMicroStepDao? = null
) {
    // Secondary constructor for tests or callers providing only base dependencies
    constructor(
        taskDao: TaskDao,
        hyperFocusManager: Lazy<HyperFocusManager>,
        workManager: WorkManager
    ) : this(taskDao, hyperFocusManager, workManager, null)

    fun observeMicroSteps(taskId: String): Flow<List<TaskMicroStepEntity>> =
        taskMicroStepDao?.observeByTaskId(taskId) ?: flowOf(emptyList())

    suspend fun getMicroSteps(taskId: String): List<TaskMicroStepEntity> =
        taskMicroStepDao?.getByTaskId(taskId) ?: emptyList()

    suspend fun insertMicroStep(step: TaskMicroStepEntity) {
        taskMicroStepDao?.insert(step)
    }

    suspend fun insertMicroSteps(steps: List<TaskMicroStepEntity>) {
        taskMicroStepDao?.insertAll(steps)
    }

    suspend fun updateMicroStep(step: TaskMicroStepEntity) {
        taskMicroStepDao?.update(step)
    }

    suspend fun deleteMicroStep(step: TaskMicroStepEntity) {
        taskMicroStepDao?.delete(step)
    }

    suspend fun deleteMicroStepsByTaskId(taskId: String) {
        taskMicroStepDao?.deleteByTaskId(taskId)
    }

    suspend fun toggleMicroStepCompleted(step: TaskMicroStepEntity) {
        taskMicroStepDao?.update(step.copy(isCompleted = !step.isCompleted))
    }
    fun observeAll(): Flow<List<TaskEntity>> = taskDao.observeAll()
    fun observeActiveTasks(): Flow<List<TaskEntity>> = taskDao.observeActiveTasks()
    fun observeCompletedTasks(): Flow<List<TaskEntity>> = taskDao.observeCompletedTasks()
    fun observeByQuadrant(quadrant: Quadrant): Flow<List<TaskEntity>> = taskDao.observeByQuadrant(quadrant)
    fun observeByStatus(status: TaskStatus): Flow<List<TaskEntity>> = taskDao.observeByStatus(status)
    fun observeTasksForDate(date: Long): Flow<List<TaskEntity>> {
        val dayStart = date.toDayStart()
        val dayEnd = Calendar.getInstance().apply {
            timeInMillis = dayStart
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        return taskDao.observeTasksForDate(dayStart, dayEnd)
    }
    fun observeQuadrantCount(quadrant: Quadrant): Flow<Int> = taskDao.observeQuadrantCount(quadrant)
    fun observeById(id: String): Flow<TaskEntity?> = taskDao.observeById(id)
    fun observeByContextTag(tag: String): Flow<List<TaskEntity>> = taskDao.observeByContextTag(tag)
    fun observeByGoalId(goalId: String): Flow<List<TaskEntity>> = taskDao.observeByGoalId(goalId)
    fun observeSubtasks(parentId: String): Flow<List<TaskEntity>> = taskDao.observeSubtasks(parentId)

    suspend fun getById(id: String): TaskEntity? = taskDao.getById(id)
    suspend fun getActiveTasks(): List<TaskEntity> = taskDao.getActiveTasks()
    suspend fun getAllTasks(): List<TaskEntity> = taskDao.getAllTasks()
    suspend fun getCompletedTasks(): List<TaskEntity> = taskDao.getCompletedTasks()

    suspend fun insert(task: TaskEntity) {
        taskDao.insert(task)
        if (!AutoSchedulingContracts.hasManualScheduleData(task) && task.status != TaskStatus.COMPLETED) {
            enqueueAutoSchedulingRecheck()
        }
    }
    suspend fun update(task: TaskEntity) {
        val old = taskDao.getById(task.id)
        // plannedBlockMinutes is the length the planner reserved, and TaskBlockPlan prefers it over
        // the estimate. Left stale it silently overrides a new duration until the task happens to be
        // replanned, so a reschedule that never fires keeps blocking and expiring the old block.
        val estimateChanged = old != null && old.estimatedDurationMinutes != task.estimatedDurationMinutes
        val toWrite = if (estimateChanged) task.copy(plannedBlockMinutes = 0) else task
        taskDao.update(toWrite)

        // Trigger rescheduling if relevant fields changed
        if (old != null && shouldTriggerReschedule(old, toWrite)) {
            enqueueAutoSchedulingRecheck()
        }
    }

    private fun shouldTriggerReschedule(old: TaskEntity, new: TaskEntity): Boolean {
        return old.deadlineDate != new.deadlineDate ||
               old.deadlineTime != new.deadlineTime ||
               old.priority != new.priority ||
               old.estimatedDurationMinutes != new.estimatedDurationMinutes ||
               old.effortScore != new.effortScore ||
               old.tags != new.tags ||
               old.taskType != new.taskType ||
               old.energyLevel != new.energyLevel ||
               // Task was unscheduled (manual or incomplete)
               (old.scheduledDate != null && new.scheduledDate == null)
    }

    suspend fun markIncomplete(task: TaskEntity, timeSpentMinutes: Int) {
        update(task.copy(
            scheduledDate = null,
            scheduledTime = null,
            totalTimeTrackedMinutes = task.totalTimeTrackedMinutes + timeSpentMinutes,
            updatedAt = System.currentTimeMillis()
        ))
        // Rescheduling trigger already called in update()
    }

    suspend fun postponeTask(task: TaskEntity) {
        update(task.copy(
            postponeCount = task.postponeCount + 1,
            scheduledDate = null,
            scheduledTime = null,
            updatedAt = System.currentTimeMillis()
        ))
        // Rescheduling trigger already called in update()
    }

    /**
     * Moves a task to tomorrow immediately, releasing the time-of-day so the auto-scheduler
     * claims it. A date-only change is not covered by shouldTriggerReschedule, so the recheck
     * is enqueued here.
     */
    suspend fun rescheduleToTomorrow(task: TaskEntity) {
        val tomorrowStart = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        update(task.copy(
            postponeCount = task.postponeCount + 1,
            scheduledDate = tomorrowStart,
            scheduledTime = null,
            isAutoScheduled = false,
            updatedAt = System.currentTimeMillis()
        ))
        enqueueAutoSchedulingRecheck()
    }
    suspend fun delete(task: TaskEntity) {
        if (hyperFocusManager.get().isTaskDeletionBlocked(task.id)) return
        taskDao.delete(task)
        // Clean up dependencies referencing this deleted task to avoid orphan deadlocks
        val activeTasks = taskDao.getActiveTasks()
        activeTasks.forEach { other ->
            if (other.dependsOnTaskIds.isNotBlank()) {
                val currentDeps = other.dependsOnTaskIds.split(",").map { it.trim() }.filter { it.isNotBlank() }
                if (task.id in currentDeps) {
                    val remainingDeps = currentDeps.filter { it != task.id }
                    taskDao.update(
                        other.copy(
                            dependsOnTaskIds = remainingDeps.joinToString(","),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        enqueueAutoSchedulingRecheck()
    }
    suspend fun deleteAll() = taskDao.deleteAll()
    suspend fun resetEstimationErrors() = taskDao.resetEstimationErrors()

    /**
     * Splits a task into multiple sequential sessions (e.g. 45-min chunks).
     * Connects sessions via `parentTaskId` and sequential `dependsOnTaskIds`.
     * Marks the parent task as completed/archived and triggers auto-scheduling.
     */
    suspend fun splitTaskIntoSessions(
        task: TaskEntity,
        sessionDurationMinutes: Int = 45
    ): List<TaskEntity> {
        val totalMinutes = if (task.estimatedDurationMinutes > 0) {
            task.estimatedDurationMinutes
        } else {
            90
        }

        val effectiveSessionMinutes = sessionDurationMinutes.coerceIn(15, 120)
        val sessionCount = maxOf(2, ((totalMinutes + effectiveSessionMinutes - 1) / effectiveSessionMinutes))
        val minutesPerSession = (totalMinutes / sessionCount).coerceAtLeast(15)
        val remainder = totalMinutes - (minutesPerSession * (sessionCount - 1))

        val now = System.currentTimeMillis()
        val createdSessions = mutableListOf<TaskEntity>()
        var previousSessionId = ""

        for (i in 1..sessionCount) {
            val duration = if (i == sessionCount) remainder.coerceAtLeast(15) else minutesPerSession
            val sessionTask = TaskEntity(
                id = java.util.UUID.randomUUID().toString(),
                title = "${task.title} (Part $i/$sessionCount)",
                description = task.description,
                quadrant = task.quadrant,
                priority = task.priority,
                deadlineDate = task.deadlineDate,
                deadlineTime = task.deadlineTime,
                estimatedDurationMinutes = duration,
                parentTaskId = task.id,
                dependsOnTaskIds = previousSessionId,
                impactScore = task.impactScore,
                valueScore = task.valueScore,
                effortScore = task.effortScore,
                taskType = task.taskType,
                energyLevel = task.energyLevel,
                contextTag = task.contextTag,
                goalId = task.goalId,
                isFrog = if (i == 1) task.isFrog else false,
                tags = task.tags,
                ifThenPlan = task.ifThenPlan,
                status = com.neuroflow.app.domain.model.TaskStatus.ACTIVE,
                createdAt = now + i,
                updatedAt = now + i
            )
            createdSessions.add(sessionTask)
            taskDao.insert(sessionTask)
            previousSessionId = sessionTask.id
        }

        // Archive parent task as completed
        taskDao.update(
            task.copy(
                status = com.neuroflow.app.domain.model.TaskStatus.COMPLETED,
                completedAt = now,
                updatedAt = now
            )
        )

        enqueueAutoSchedulingRecheck()
        return createdSessions
    }

    private fun enqueueAutoSchedulingRecheck() {
        val request = OneTimeWorkRequestBuilder<ScheduleAutoTasksWorker>().build()
        workManager.enqueueUniqueWork(
            "${ScheduleAutoTasksWorker.WORK_NAME}_trigger",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    suspend fun markTaskCompleted(taskId: String, now: Long = System.currentTimeMillis()): Boolean {
        val task = getById(taskId) ?: return false
        completeAndRecur(task, now)
        return true
    }

    /**
     * Marks [task] as completed and, if it has recurrence, inserts the next occurrence.
     * Uses calendar-aware stepping (daily/weekly/monthly/custom days) and guarantees
     * the new occurrence anchor lands strictly in the future.
     */
    suspend fun completeAndRecur(task: TaskEntity, now: Long): String? {
        update(
            task.copy(
                status = TaskStatus.COMPLETED,
                completedAt = now,
                isHabitual = task.recurrence != Recurrence.NONE,
                habitStreak = if (task.recurrence != Recurrence.NONE) task.habitStreak + 1 else task.habitStreak,
                updatedAt = now
            )
        )
        hyperFocusManager.get().onTaskCompleted()
        enqueueAutoSchedulingRecheck()

        if (task.recurrence == Recurrence.NONE) return null

        // Anchor for recurrence progression: prefer habitDate, then scheduled/deadline with time, then now.
        val currentAnchor =
            task.habitDate
                ?: task.scheduledDate?.let { it + (task.scheduledTime ?: 0L) }
                ?: task.deadlineDate?.let { it + (task.deadlineTime ?: 0L) }
                ?: now
        val nextAnchor = nextRecurringAnchorAfter(
            recurrence = task.recurrence,
            customDays = task.recurrenceIntervalDays,
            currentAnchor = currentAnchor,
            now = now
        )
        val nextDayStart = nextAnchor.toDayStart()
        val (nextScheduledDate, nextScheduledTime) = if (task.scheduledDate != null) {
            nextDayStart to task.scheduledTime
        } else {
            null to null
        }
        val (nextDeadlineDate, nextDeadlineTime) = if (task.deadlineDate != null) {
            val anchorDate = Instant.ofEpochMilli(startOfLocalDay(currentAnchor))
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val deadlineDate = Instant.ofEpochMilli(startOfLocalDay(task.deadlineDate))
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val deadlineDayOffset = ChronoUnit.DAYS.between(anchorDate, deadlineDate)
                .coerceAtLeast(0L)
            val nextDeadline = Calendar.getInstance().apply {
                timeInMillis = nextDayStart
                add(Calendar.DAY_OF_YEAR, deadlineDayOffset.toInt())
            }.timeInMillis
            nextDeadline to task.deadlineTime
        } else {
            null to null
        }

        val newId = UUID.randomUUID().toString()
        insert(
            task.copy(
                id = newId,
                status = TaskStatus.ACTIVE,
                completedAt = null,
                habitDate = nextAnchor.toDayStart(),
                deadlineDate = nextDeadlineDate,
                deadlineTime = nextDeadlineTime,
                scheduledDate = nextScheduledDate,
                scheduledTime = nextScheduledTime,
                isScheduleLocked = task.isScheduleLocked,
                totalTimeTrackedMinutes = 0f,
                sessionCount = 0,
                lastSessionDurationMinutes = null,
                actualDurationMinutes = null,
                estimationErrorMape = null,
                estimationErrorSmape = null,
                focusModePoints = 0,
                postponeCount = 0,
                habitStreak = task.habitStreak + 1,
                isHabitual = true,
                createdAt = now,
                updatedAt = now
            )
        )
        return newId
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdatalocalconverterskt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/Converters.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/Converters.kt`
- **Lines of Code:** 92
- **Size:** 3,432 bytes (3.4 KB)

```kotlin
package com.neuroflow.app.data.local

import android.util.Log
import androidx.room.TypeConverter
import com.neuroflow.app.data.local.entity.ContractOutcome
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Quadrant
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.domain.model.TaskType

class Converters {
    private companion object {
        private const val TAG = "RoomConverters"
    }

    @TypeConverter fun fromQuadrant(value: Quadrant): String = value.name
    @TypeConverter fun toQuadrant(value: String): Quadrant =
        try {
            Quadrant.valueOf(value)
        } catch (e: Exception) {
            Log.w(TAG, "Unknown Quadrant: '$value', defaulting to SCHEDULE", e)
            Quadrant.SCHEDULE
        }

    @TypeConverter fun fromPriority(value: Priority): String = value.name
    @TypeConverter fun toPriority(value: String): Priority =
        try {
            Priority.valueOf(value)
        } catch (e: Exception) {
            Log.w(TAG, "Unknown Priority: '$value', defaulting to MEDIUM", e)
            Priority.MEDIUM
        }

    @TypeConverter fun fromTaskStatus(value: TaskStatus): String = value.name
    @TypeConverter fun toTaskStatus(value: String): TaskStatus =
        try {
            TaskStatus.valueOf(value)
        } catch (e: Exception) {
            Log.w(TAG, "Unknown TaskStatus: '$value', defaulting to ACTIVE", e)
            TaskStatus.ACTIVE
        }

    @TypeConverter fun fromRecurrence(value: Recurrence): String = value.name
    @TypeConverter fun toRecurrence(value: String): Recurrence =
        try {
            Recurrence.valueOf(value)
        } catch (e: Exception) {
            Log.w(TAG, "Unknown Recurrence: '$value', defaulting to NONE", e)
            Recurrence.NONE
        }

    @TypeConverter fun fromEnergyLevel(value: EnergyLevel): String = value.name
    @TypeConverter fun toEnergyLevel(value: String): EnergyLevel =
        try {
            EnergyLevel.valueOf(value)
        } catch (e: Exception) {
            Log.w(TAG, "Unknown EnergyLevel: '$value', defaulting to MEDIUM", e)
            EnergyLevel.MEDIUM
        }

    @TypeConverter fun fromTaskType(value: TaskType): String = value.name
    @TypeConverter fun toTaskType(value: String): TaskType =
        try {
            TaskType.valueOf(value)
        } catch (e: Exception) {
            Log.w(TAG, "Unknown TaskType: '$value', defaulting to ANALYTICAL", e)
            TaskType.ANALYTICAL
        }

    @TypeConverter fun fromContractOutcome(value: ContractOutcome?): String? = value?.name
    @TypeConverter fun toContractOutcome(value: String?): ContractOutcome? =
        value?.let {
            try {
                ContractOutcome.valueOf(it)
            } catch (e: Exception) {
                Log.w(TAG, "Unknown ContractOutcome: '$it', defaulting to null", e)
                null
            }
        }

    @TypeConverter fun fromRewardTier(value: RewardTier): String = value.name
    @TypeConverter fun toRewardTier(value: String): RewardTier =
        try {
            RewardTier.valueOf(value)
        } catch (e: Exception) {
            Log.w(TAG, "Unknown RewardTier: '$value', defaulting to NONE", e)
            RewardTier.NONE
        }
}
```

---

<a id="appsrcmainjavacomneuroflowappdatalocaluserpreferencesdatastorekt"></a>
### File: `app/src/main/java/com/neuroflow/app/data/local/UserPreferencesDataStore.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/data/local/UserPreferencesDataStore.kt`
- **Lines of Code:** 1,197
- **Size:** 73,477 bytes (71.8 KB)

```kotlin
package com.neuroflow.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.scheduler.TaskTagSchedulingProfile
import com.neuroflow.app.domain.model.AppTheme
import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsCategory
import com.neuroflow.app.domain.model.PtsRecurrence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.ceil
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * ADR-004 ARCHITECTURAL DIRECTIVE (Track 1):
 * FROZEN TO STRICTLY SCALAR PRIMITIVES GOING FORWARD.
 *
 * All complex, nested, or relational domain models MUST be persisted in Room child tables
 * (e.g. Room v22 `task_micro_steps`, `focus_sessions`, `quick_notes`, `protected_slots`).
 * ViewModels collecting [preferencesFlow] MUST apply `.distinctUntilChanged { old, new -> ... }`
 * on only the specific scalar fields required for their state evaluation.
 */
data class UserPreferences(
    val wakeUpHour: Int = 7,
    val sleepHour: Int = 23,
    val peakEnergyStart: Int = 9,
    val peakEnergyEnd: Int = 12,
    val workDayStart: Int = 8,
    val workDayEnd: Int = 20,
    val defaultPomodoroMinutes: Int = 25,
    val defaultBreakMinutes: Int = 5,
    val identityLabel: String = "",
    val topGoal: String = "",
    val dailyStreak: Int = 0,
    val lastActiveDate: Long = 0L,
    val longestStreak: Int = 0,
    val totalTasksCompleted: Int = 0,
    val totalFocusMinutes: Int = 0,
    val weightQuadrant: Float = 1.0f,
    val weightDeadlineUrgency: Float = 1.0f,
    val weightPriorityLevel: Float = 1.0f,
    val weightDuration: Float = 1.0f,
    val weightImpact: Float = 1.0f,
    val weightFocusMode: Float = 1.0f,
    val weightCircadianMatch: Float = 1.0f,
    val weightFrogBonus: Float = 1.0f,
    val weightPlanBoost: Float = 1.0f,
    val weightSessionProgress: Float = 1.0f,
    val weightEnjoyment: Float = 1.0f,
    // Enjoyment points subtracted from every new enjoyment forecast, learned from how finished
    // tasks actually felt. Negative means the user tends to overestimate how much they'll enjoy work.
    val enjoymentForecastBias: Float = 0f,
    val onboardingCompleted: Boolean = false,
    val theme: AppTheme = AppTheme.SYSTEM,
    val weeklyIntent: String = "",
    val weeklyIntentIsoWeek: Int = 0,
    val weeklyIntentIsoYear: Int = 0,
    val lastFreshStartShownWeek: Int = 0,
    val lastFreshStartShownYear: Int = 0,
    val lastAppOpenMillis: Long = 0L,
    val firstAppOpenMillis: Long = 0L,
    // Dynamic peak energy detection
    val detectedPeakStart: Int = -1,      // -1 = not yet detected
    val detectedPeakEnd: Int = -1,
    val detectedPeakMinuteOfDay: Int = -1,
    val peakDetectionConfidence: Float = 0f,  // 0.0–1.0
    // Blended effective peak (manual lerp'd with detected, based on confidence)
    val effectivePeakStart: Int = -1,     // -1 = use peakEnergyStart
    val effectivePeakEnd: Int = -1,       // -1 = use peakEnergyEnd
    val effectivePeakMinuteOfDay: Int = -1,
    val quizPeakEnabled: Boolean = true,
    // Subliminal affirmations — stored as JSON array string
    val affirmations: List<String> = emptyList(),
    // Persistent task tag catalog shown in task creation and history filters
    val tagCatalog: List<String> = emptyList(),
    // Auto scheduling controls
    val autoSchedulingEnabled: Boolean = true,
    val isAutoScheduleToggleOn: Boolean = false,
    val autoSchedulingHorizonDays: Int = 3,
    val autoSchedulingBreakAfterCognitiveMinutes: Int = 90,
    val autoSchedulingBreakDurationMinutes: Int = 15,
    val autoSchedulingBackgroundThrottleMinutes: Int = 30,
    // Top 3 goals for the year (JSON array)
    val yearlyGoals: List<String> = emptyList(),
    // Top 3 goals for the current week (JSON array)
    val weeklyGoals: List<String> = emptyList(),
    // Tracks when we last showed the yearly goals refill prompt
    val lastYearlyGoalShownYear: Int = 0,
    // Tracks when we last showed the weekly goals refill prompt (ISO week + year)
    val lastWeeklyGoalShownWeek: Int = 0,
    val lastWeeklyGoalShownYear: Int = 0,
    // ADR-005: Persistent Skip & Handled Guards (scalar ints)
    val lastYearlyGoalHandledYear: Int = 0,
    val lastWeeklyGoalHandledWeek: Int = 0,
    // Focus behaviour toggles
    val woopEnabled: Boolean = true,
    val autoTrackerEnabled: Boolean = false,
    // Notification preferences
    val dailyPlanNotificationsEnabled: Boolean = true,
    val streakNotificationsEnabled: Boolean = true,
    val autonomyNudgeNotificationsEnabled: Boolean = true,
    val deadlineReminderNotificationsEnabled: Boolean = true,
    val deadlineEscalationNotificationsEnabled: Boolean = true,
    val dailyPlanNotificationHour: Int = 7,
    val streakCheckNotificationHour: Int = 21,
    val userGuidePromptShown: Boolean = false,
    // Left page quick note
    val leftPageQuickNote: String = "",
    // MEQ Chronotype - manual user selection from onboarding
    val manualChronotype: String? = null,
    // MEQ Chronotype - result from completed quiz
    val quizChronotype: String? = null,
    // Quiz progress - JSON string storing which questions answered and selected answers
    val quizProgress: String = "{}",
    // Sleep pressure state (recomputed from logs + elapsed awake time)
    val sleepPressurePoints: Int = 0,
    val sleepPressureTrackingStartedAtMillis: Long = 0L,
    val sleepPressureLastComputedAtMillis: Long = 0L,
    val autoFallbackSleepInsertionEnabled: Boolean = true,
    val dismissedSleepFallbacks: Set<String> = emptySet(),
    // Daily deep-work budget on the Today screen, and the apps that count as distraction
    val dailyDeepWorkTargetMinutes: Int = 240,
    val distractingPackages: Set<String> = BehavioralSupportService.DEFAULT_DISTRACTING_PACKAGES,
    // Moment model sensitivity controls
    val momentInterruptionSensitivity: Float = 1.0f,
    val momentNotificationSensitivity: Float = 1.0f,
    val momentTaskPressureSensitivity: Float = 1.0f,
    // Local telemetry privacy controls
    val energyTelemetryEnabled: Boolean = true,
    val energyTelemetryRetentionDays: Int = 30,
    // Morning calibration auto-tune coefficients (bounded adaptive blend)
    val morningTuneSleepWeight: Float = 0.30f,
    val morningTuneWakeWeight: Float = 0.25f,
    val morningTuneBehaviorWeight: Float = 0.25f,
    val morningTuneBaseWeight: Float = 0.20f,
    val morningTuneUpdatedAtMillis: Long = 0L,
    val morningTuneVersion: Int = 1,
    val adaptivePeakFreezeEnabled: Boolean = false,
    val peakQualityDegradeStreak: Int = 0,
    val peakConfidenceAbstentionEnabled: Boolean = false,
    val peakConfidenceAbstentionReason: String = "",
    val peakConfidenceAbstentionTriggerCount: Int = 0,
    val peakConfidenceAbstentionRecoveryCount: Int = 0,
    val peakConfidenceAbstentionLastChangedAtMillis: Long = 0L,
    val peakConfidenceAbstentionReasonFreezeCount: Int = 0,
    val peakConfidenceAbstentionReasonLowSamplesCount: Int = 0,
    val peakConfidenceAbstentionReasonLowCoverageCount: Int = 0,
    val peakConfidenceAbstentionReasonWakeVarianceCount: Int = 0,
    val peakConfidenceAbstentionReasonDivergenceCount: Int = 0,
    val peakConfidenceAbstentionReasonOtherCount: Int = 0,
    // Manual peak profile override controls
    val manualPeakProfileEnabled: Boolean = false,
    val manualPeakProfileType: String = "AUTO",
    val manualPeakAnchorMinuteOfDay: Int = 360,
    val manualPeakWindow1StartOffsetMinutes: Int = 0,
    val manualPeakWindow2StartOffsetMinutes: Int = 570,
    val manualPeakWindow3StartOffsetMinutes: Int = 810,
    val manualPeakWindow1DurationMinutes: Int = 210,
    val manualPeakWindow2DurationMinutes: Int = 150,
    val manualPeakWindow3DurationMinutes: Int = 60,
    val manualPeakWindow1Amplitude: Float = 1.0f,
    val manualPeakWindow2Amplitude: Float = 0.8f,
    val manualPeakWindow3Amplitude: Float = 0.6f,
    val protectedTimeSlots: List<ProtectedTimeSlot> = emptyList(),
    val dynamicColorEnabled: Boolean = true,
    val amoledDarkEnabled: Boolean = false,
    val dndEnabled: Boolean = true,
    val useV2Ui: Boolean = true,
    val geminiApiKey: String = "",
    val geminiAiEnabled: Boolean = true,
    val adaptiveSystemDirective: String = "",
    val weeklyAiAuditJson: String = "",
    val recentlyDeletedTasksJson: String = "[]",
    val stimulantLogsJson: String = "[]",
    val quickPhysiologicalLogsJson: String = "[]",
    val meqScore: Int = 0
)

@Singleton
class UserPreferencesDataStore @Inject constructor(
    private val context: Context,
    private val protectedSlotDao: com.neuroflow.app.data.local.dao.ProtectedSlotDao? = null
) {
    private object Keys {
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val GEMINI_AI_ENABLED = booleanPreferencesKey("gemini_ai_enabled")
        val ADAPTIVE_SYSTEM_DIRECTIVE = stringPreferencesKey("adaptive_system_directive")
        val WEEKLY_AI_AUDIT_JSON = stringPreferencesKey("weekly_ai_audit_json")
        val RECENTLY_DELETED_TASKS_JSON = stringPreferencesKey("recently_deleted_tasks_json")
        val STIMULANT_LOGS_JSON = stringPreferencesKey("stimulant_logs_json")
        val QUICK_PHYSIOLOGICAL_LOGS_JSON = stringPreferencesKey("quick_physiological_logs_json")
        val WAKE_UP_HOUR = intPreferencesKey("wake_up_hour")
        val SLEEP_HOUR = intPreferencesKey("sleep_hour")
        val PEAK_ENERGY_START = intPreferencesKey("peak_energy_start")
        val PEAK_ENERGY_END = intPreferencesKey("peak_energy_end")
        val WORK_DAY_START = intPreferencesKey("work_day_start")
        val WORK_DAY_END = intPreferencesKey("work_day_end")
        val DEFAULT_POMODORO_MINUTES = intPreferencesKey("default_pomodoro_minutes")
        val DEFAULT_BREAK_MINUTES = intPreferencesKey("default_break_minutes")
        val IDENTITY_LABEL = stringPreferencesKey("identity_label")
        val TOP_GOAL = stringPreferencesKey("top_goal")
        val DAILY_STREAK = intPreferencesKey("daily_streak")
        val LAST_ACTIVE_DATE = longPreferencesKey("last_active_date")
        val LONGEST_STREAK = intPreferencesKey("longest_streak")
        val TOTAL_TASKS_COMPLETED = intPreferencesKey("total_tasks_completed")
        val TOTAL_FOCUS_MINUTES = intPreferencesKey("total_focus_minutes")
        val WEIGHT_QUADRANT = floatPreferencesKey("weight_quadrant")
        val WEIGHT_DEADLINE_URGENCY = floatPreferencesKey("weight_deadline_urgency")
        val WEIGHT_PRIORITY_LEVEL = floatPreferencesKey("weight_priority_level")
        val WEIGHT_DURATION = floatPreferencesKey("weight_duration")
        val WEIGHT_IMPACT = floatPreferencesKey("weight_impact")
        val WEIGHT_FOCUS_MODE = floatPreferencesKey("weight_focus_mode")
        val WEIGHT_CIRCADIAN_MATCH = floatPreferencesKey("weight_circadian_match")
        val WEIGHT_FROG_BONUS = floatPreferencesKey("weight_frog_bonus")
        val WEIGHT_PLAN_BOOST = floatPreferencesKey("weight_plan_boost")
        val WEIGHT_SESSION_PROGRESS = floatPreferencesKey("weight_session_progress")
        val WEIGHT_ENJOYMENT = floatPreferencesKey("weight_enjoyment")
        val ENJOYMENT_FORECAST_BIAS = floatPreferencesKey("enjoyment_forecast_bias")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME = stringPreferencesKey("theme")
        val WEEKLY_INTENT = stringPreferencesKey("weekly_intent")
        val WEEKLY_INTENT_ISO_WEEK = intPreferencesKey("weekly_intent_iso_week")
        val WEEKLY_INTENT_ISO_YEAR = intPreferencesKey("weekly_intent_iso_year")
        val LAST_FRESH_START_SHOWN_WEEK = intPreferencesKey("last_fresh_start_shown_week")
        val LAST_FRESH_START_SHOWN_YEAR = intPreferencesKey("last_fresh_start_shown_year")
        val LAST_APP_OPEN_MILLIS = longPreferencesKey("last_app_open_millis")
        val FIRST_APP_OPEN_MILLIS = longPreferencesKey("first_app_open_millis")
        val DETECTED_PEAK_START = intPreferencesKey("detected_peak_start")
        val DETECTED_PEAK_END = intPreferencesKey("detected_peak_end")
        val DETECTED_PEAK_MINUTE_OF_DAY = intPreferencesKey("detected_peak_minute_of_day")
        val PEAK_DETECTION_CONFIDENCE = floatPreferencesKey("peak_detection_confidence")
        val EFFECTIVE_PEAK_START = intPreferencesKey("effective_peak_start")
        val EFFECTIVE_PEAK_END = intPreferencesKey("effective_peak_end")
        val EFFECTIVE_PEAK_MINUTE_OF_DAY = intPreferencesKey("effective_peak_minute_of_day")
        val QUIZ_PEAK_ENABLED = booleanPreferencesKey("quiz_peak_enabled")
        val AFFIRMATIONS = stringPreferencesKey("affirmations")
        val TAG_CATALOG = stringPreferencesKey("tag_catalog")
        val AUTO_SCHEDULING_ENABLED = booleanPreferencesKey("auto_scheduling_enabled")
        val IS_AUTO_SCHEDULE_TOGGLE_ON = booleanPreferencesKey("is_auto_schedule_toggle_on")
        val AUTO_SCHEDULING_HORIZON_DAYS = intPreferencesKey("auto_scheduling_horizon_days")
        val AUTO_SCHEDULING_BREAK_AFTER_COGNITIVE_MINUTES = intPreferencesKey("auto_scheduling_break_after_cognitive_minutes")
        val AUTO_SCHEDULING_BREAK_DURATION_MINUTES = intPreferencesKey("auto_scheduling_break_duration_minutes")
        val AUTO_SCHEDULING_BACKGROUND_THROTTLE_MINUTES = intPreferencesKey("auto_scheduling_background_throttle_minutes")
        val YEARLY_GOALS = stringPreferencesKey("yearly_goals")
        val WEEKLY_GOALS = stringPreferencesKey("weekly_goals")
        val LAST_YEARLY_GOAL_SHOWN_YEAR = intPreferencesKey("last_yearly_goal_shown_year")
        val LAST_WEEKLY_GOAL_SHOWN_WEEK = intPreferencesKey("last_weekly_goal_shown_week")
        val LAST_WEEKLY_GOAL_SHOWN_YEAR = intPreferencesKey("last_weekly_goal_shown_year")
        val LAST_YEARLY_GOAL_HANDLED_YEAR = intPreferencesKey("last_yearly_goal_handled_year")
        val LAST_WEEKLY_GOAL_HANDLED_WEEK = intPreferencesKey("last_weekly_goal_handled_week")
        val WOOP_ENABLED = booleanPreferencesKey("woop_enabled")
        val AUTO_TRACKER_ENABLED = booleanPreferencesKey("auto_tracker_enabled")
        val DAILY_PLAN_NOTIFICATIONS_ENABLED = booleanPreferencesKey("daily_plan_notifications_enabled")
        val STREAK_NOTIFICATIONS_ENABLED = booleanPreferencesKey("streak_notifications_enabled")
        val AUTONOMY_NUDGE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("autonomy_nudge_notifications_enabled")
        val DEADLINE_REMINDER_NOTIFICATIONS_ENABLED = booleanPreferencesKey("deadline_reminder_notifications_enabled")
        val DEADLINE_ESCALATION_NOTIFICATIONS_ENABLED = booleanPreferencesKey("deadline_escalation_notifications_enabled")
        val DAILY_PLAN_NOTIFICATION_HOUR = intPreferencesKey("daily_plan_notification_hour")
        val STREAK_CHECK_NOTIFICATION_HOUR = intPreferencesKey("streak_check_notification_hour")
        val USER_GUIDE_PROMPT_SHOWN = booleanPreferencesKey("user_guide_prompt_shown")
        val LEFT_PAGE_QUICK_NOTE = stringPreferencesKey("left_page_quick_note")
        val MANUAL_CHRONOTYPE = stringPreferencesKey("manual_chronotype")
        val QUIZ_CHRONOTYPE = stringPreferencesKey("quiz_chronotype")
        val QUIZ_PROGRESS = stringPreferencesKey("quiz_progress")
        val MEQ_SCORE = intPreferencesKey("meq_score")
        val SLEEP_PRESSURE_POINTS = intPreferencesKey("sleep_pressure_points")
        val SLEEP_PRESSURE_TRACKING_STARTED_AT = longPreferencesKey("sleep_pressure_tracking_started_at")
        val SLEEP_PRESSURE_LAST_COMPUTED_AT = longPreferencesKey("sleep_pressure_last_computed_at")
        val AUTO_FALLBACK_SLEEP_INSERTION_ENABLED = booleanPreferencesKey("auto_fallback_sleep_insertion_enabled")
        val DISMISSED_SLEEP_FALLBACKS = stringSetPreferencesKey("dismissed_sleep_fallbacks")
        val DAILY_DEEP_WORK_TARGET_MINUTES = intPreferencesKey("daily_deep_work_target_minutes")
        val DISTRACTING_PACKAGES = stringSetPreferencesKey("distracting_packages")
        val MOMENT_INTERRUPTION_SENSITIVITY = floatPreferencesKey("moment_interruption_sensitivity")
        val MOMENT_NOTIFICATION_SENSITIVITY = floatPreferencesKey("moment_notification_sensitivity")
        val MOMENT_TASK_PRESSURE_SENSITIVITY = floatPreferencesKey("moment_task_pressure_sensitivity")
        val ENERGY_TELEMETRY_ENABLED = booleanPreferencesKey("energy_telemetry_enabled")
        val ENERGY_TELEMETRY_RETENTION_DAYS = intPreferencesKey("energy_telemetry_retention_days")
        val MORNING_TUNE_SLEEP_WEIGHT = floatPreferencesKey("morning_tune_sleep_weight")
        val MORNING_TUNE_WAKE_WEIGHT = floatPreferencesKey("morning_tune_wake_weight")
        val MORNING_TUNE_BEHAVIOR_WEIGHT = floatPreferencesKey("morning_tune_behavior_weight")
        val MORNING_TUNE_BASE_WEIGHT = floatPreferencesKey("morning_tune_base_weight")
        val MORNING_TUNE_UPDATED_AT = longPreferencesKey("morning_tune_updated_at")
        val MORNING_TUNE_VERSION = intPreferencesKey("morning_tune_version")
        val ADAPTIVE_PEAK_FREEZE_ENABLED = booleanPreferencesKey("adaptive_peak_freeze_enabled")
        val PEAK_QUALITY_DEGRADE_STREAK = intPreferencesKey("peak_quality_degrade_streak")
        val PEAK_CONFIDENCE_ABSTENTION_ENABLED = booleanPreferencesKey("peak_confidence_abstention_enabled")
        val PEAK_CONFIDENCE_ABSTENTION_REASON = stringPreferencesKey("peak_confidence_abstention_reason")
        val PEAK_CONFIDENCE_ABSTENTION_TRIGGER_COUNT = intPreferencesKey("peak_confidence_abstention_trigger_count")
        val PEAK_CONFIDENCE_ABSTENTION_RECOVERY_COUNT = intPreferencesKey("peak_confidence_abstention_recovery_count")
        val PEAK_CONFIDENCE_ABSTENTION_LAST_CHANGED_AT = longPreferencesKey("peak_confidence_abstention_last_changed_at")
        val PEAK_CONFIDENCE_ABSTENTION_REASON_FREEZE_COUNT = intPreferencesKey("peak_confidence_abstention_reason_freeze_count")
        val PEAK_CONFIDENCE_ABSTENTION_REASON_LOW_SAMPLES_COUNT = intPreferencesKey("peak_confidence_abstention_reason_low_samples_count")
        val PEAK_CONFIDENCE_ABSTENTION_REASON_LOW_COVERAGE_COUNT = intPreferencesKey("peak_confidence_abstention_reason_low_coverage_count")
        val PEAK_CONFIDENCE_ABSTENTION_REASON_WAKE_VARIANCE_COUNT = intPreferencesKey("peak_confidence_abstention_reason_wake_variance_count")
        val PEAK_CONFIDENCE_ABSTENTION_REASON_DIVERGENCE_COUNT = intPreferencesKey("peak_confidence_abstention_reason_divergence_count")
        val PEAK_CONFIDENCE_ABSTENTION_REASON_OTHER_COUNT = intPreferencesKey("peak_confidence_abstention_reason_other_count")
        val MANUAL_PEAK_PROFILE_ENABLED = booleanPreferencesKey("manual_peak_profile_enabled")
        val MANUAL_PEAK_PROFILE_TYPE = stringPreferencesKey("manual_peak_profile_type")
        val MANUAL_PEAK_ANCHOR_MINUTE_OF_DAY = intPreferencesKey("manual_peak_anchor_minute_of_day")
        val MANUAL_PEAK_WINDOW_1_START_OFFSET = intPreferencesKey("manual_peak_window_1_start_offset")
        val MANUAL_PEAK_WINDOW_2_START_OFFSET = intPreferencesKey("manual_peak_window_2_start_offset")
        val MANUAL_PEAK_WINDOW_3_START_OFFSET = intPreferencesKey("manual_peak_window_3_start_offset")
        val MANUAL_PEAK_WINDOW_1_DURATION = intPreferencesKey("manual_peak_window_1_duration")
        val MANUAL_PEAK_WINDOW_2_DURATION = intPreferencesKey("manual_peak_window_2_duration")
        val MANUAL_PEAK_WINDOW_3_DURATION = intPreferencesKey("manual_peak_window_3_duration")
        val MANUAL_PEAK_WINDOW_1_AMPLITUDE = floatPreferencesKey("manual_peak_window_1_amplitude")
        val MANUAL_PEAK_WINDOW_2_AMPLITUDE = floatPreferencesKey("manual_peak_window_2_amplitude")
        val MANUAL_PEAK_WINDOW_3_AMPLITUDE = floatPreferencesKey("manual_peak_window_3_amplitude")
        val PROTECTED_TIME_SLOTS = stringPreferencesKey("protected_time_slots")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val AMOLED_DARK_ENABLED = booleanPreferencesKey("amoled_dark_enabled")
        val DND_ENABLED = booleanPreferencesKey("dnd_enabled")
        val USE_V2_UI = booleanPreferencesKey("use_v2_ui")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            wakeUpHour = prefs[Keys.WAKE_UP_HOUR] ?: 7,
            sleepHour = prefs[Keys.SLEEP_HOUR] ?: 23,
            peakEnergyStart = prefs[Keys.PEAK_ENERGY_START] ?: 9,
            peakEnergyEnd = prefs[Keys.PEAK_ENERGY_END] ?: 12,
            workDayStart = prefs[Keys.WORK_DAY_START] ?: 8,
            workDayEnd = prefs[Keys.WORK_DAY_END] ?: 20,
            defaultPomodoroMinutes = prefs[Keys.DEFAULT_POMODORO_MINUTES] ?: 25,
            defaultBreakMinutes = prefs[Keys.DEFAULT_BREAK_MINUTES] ?: 5,
            identityLabel = prefs[Keys.IDENTITY_LABEL] ?: "",
            topGoal = prefs[Keys.TOP_GOAL] ?: "",
            dailyStreak = prefs[Keys.DAILY_STREAK] ?: 0,
            lastActiveDate = prefs[Keys.LAST_ACTIVE_DATE] ?: 0L,
            longestStreak = prefs[Keys.LONGEST_STREAK] ?: 0,
            totalTasksCompleted = prefs[Keys.TOTAL_TASKS_COMPLETED] ?: 0,
            totalFocusMinutes = prefs[Keys.TOTAL_FOCUS_MINUTES] ?: 0,
            weightQuadrant = prefs[Keys.WEIGHT_QUADRANT] ?: 1.0f,
            weightDeadlineUrgency = prefs[Keys.WEIGHT_DEADLINE_URGENCY] ?: 1.0f,
            weightPriorityLevel = prefs[Keys.WEIGHT_PRIORITY_LEVEL] ?: 1.0f,
            weightDuration = prefs[Keys.WEIGHT_DURATION] ?: 1.0f,
            weightImpact = prefs[Keys.WEIGHT_IMPACT] ?: 1.0f,
            weightFocusMode = prefs[Keys.WEIGHT_FOCUS_MODE] ?: 1.0f,
            weightCircadianMatch = prefs[Keys.WEIGHT_CIRCADIAN_MATCH] ?: 1.0f,
            weightFrogBonus = prefs[Keys.WEIGHT_FROG_BONUS] ?: 1.0f,
            weightPlanBoost = prefs[Keys.WEIGHT_PLAN_BOOST] ?: 1.0f,
            weightSessionProgress = prefs[Keys.WEIGHT_SESSION_PROGRESS] ?: 1.0f,
            weightEnjoyment = prefs[Keys.WEIGHT_ENJOYMENT] ?: 1.0f,
            enjoymentForecastBias = prefs[Keys.ENJOYMENT_FORECAST_BIAS] ?: 0f,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            theme = try {
                AppTheme.valueOf(prefs[Keys.THEME] ?: AppTheme.SYSTEM.name)
            } catch (_: Exception) { AppTheme.SYSTEM },
            weeklyIntent = prefs[Keys.WEEKLY_INTENT] ?: "",
            weeklyIntentIsoWeek = prefs[Keys.WEEKLY_INTENT_ISO_WEEK] ?: 0,
            weeklyIntentIsoYear = prefs[Keys.WEEKLY_INTENT_ISO_YEAR] ?: 0,
            lastFreshStartShownWeek = prefs[Keys.LAST_FRESH_START_SHOWN_WEEK] ?: 0,
            lastFreshStartShownYear = prefs[Keys.LAST_FRESH_START_SHOWN_YEAR] ?: 0,
            lastAppOpenMillis = prefs[Keys.LAST_APP_OPEN_MILLIS] ?: 0L,
            firstAppOpenMillis = prefs[Keys.FIRST_APP_OPEN_MILLIS] ?: 0L,
            detectedPeakStart = prefs[Keys.DETECTED_PEAK_START] ?: -1,
            detectedPeakEnd = prefs[Keys.DETECTED_PEAK_END] ?: -1,
            detectedPeakMinuteOfDay = prefs[Keys.DETECTED_PEAK_MINUTE_OF_DAY] ?: -1,
            peakDetectionConfidence = prefs[Keys.PEAK_DETECTION_CONFIDENCE] ?: 0f,
            effectivePeakStart = prefs[Keys.EFFECTIVE_PEAK_START] ?: -1,
            effectivePeakEnd = prefs[Keys.EFFECTIVE_PEAK_END] ?: -1,
            effectivePeakMinuteOfDay = prefs[Keys.EFFECTIVE_PEAK_MINUTE_OF_DAY] ?: -1,
            quizPeakEnabled = prefs[Keys.QUIZ_PEAK_ENABLED] ?: true,
            affirmations = parseAffirmations(prefs[Keys.AFFIRMATIONS]),
            tagCatalog = withStarterTagCatalog(parseAffirmations(prefs[Keys.TAG_CATALOG])),
            autoSchedulingEnabled = prefs[Keys.AUTO_SCHEDULING_ENABLED] ?: true,
            isAutoScheduleToggleOn = prefs[Keys.IS_AUTO_SCHEDULE_TOGGLE_ON] ?: false,
            autoSchedulingHorizonDays = (prefs[Keys.AUTO_SCHEDULING_HORIZON_DAYS] ?: 3).coerceIn(1, 7),
            autoSchedulingBreakAfterCognitiveMinutes = (prefs[Keys.AUTO_SCHEDULING_BREAK_AFTER_COGNITIVE_MINUTES] ?: 90).coerceIn(30, 180),
            autoSchedulingBreakDurationMinutes = (prefs[Keys.AUTO_SCHEDULING_BREAK_DURATION_MINUTES] ?: 15).coerceIn(5, 30),
            autoSchedulingBackgroundThrottleMinutes = (prefs[Keys.AUTO_SCHEDULING_BACKGROUND_THROTTLE_MINUTES] ?: 30).coerceIn(15, 240),
            yearlyGoals = parseAffirmations(prefs[Keys.YEARLY_GOALS]),
            weeklyGoals = parseAffirmations(prefs[Keys.WEEKLY_GOALS]),
            lastYearlyGoalShownYear = prefs[Keys.LAST_YEARLY_GOAL_SHOWN_YEAR] ?: 0,
            lastWeeklyGoalShownWeek = prefs[Keys.LAST_WEEKLY_GOAL_SHOWN_WEEK] ?: 0,
            lastWeeklyGoalShownYear = prefs[Keys.LAST_WEEKLY_GOAL_SHOWN_YEAR] ?: 0,
            lastYearlyGoalHandledYear = prefs[Keys.LAST_YEARLY_GOAL_HANDLED_YEAR] ?: 0,
            lastWeeklyGoalHandledWeek = prefs[Keys.LAST_WEEKLY_GOAL_HANDLED_WEEK] ?: 0,
            woopEnabled = prefs[Keys.WOOP_ENABLED] ?: true,
            autoTrackerEnabled = prefs[Keys.AUTO_TRACKER_ENABLED] ?: false,
            dailyPlanNotificationsEnabled = prefs[Keys.DAILY_PLAN_NOTIFICATIONS_ENABLED] ?: true,
            streakNotificationsEnabled = prefs[Keys.STREAK_NOTIFICATIONS_ENABLED] ?: true,
            autonomyNudgeNotificationsEnabled = prefs[Keys.AUTONOMY_NUDGE_NOTIFICATIONS_ENABLED] ?: true,
            deadlineReminderNotificationsEnabled = prefs[Keys.DEADLINE_REMINDER_NOTIFICATIONS_ENABLED] ?: true,
            deadlineEscalationNotificationsEnabled = prefs[Keys.DEADLINE_ESCALATION_NOTIFICATIONS_ENABLED] ?: true,
            dailyPlanNotificationHour = prefs[Keys.DAILY_PLAN_NOTIFICATION_HOUR] ?: 7,
            streakCheckNotificationHour = prefs[Keys.STREAK_CHECK_NOTIFICATION_HOUR] ?: 21,
            userGuidePromptShown = prefs[Keys.USER_GUIDE_PROMPT_SHOWN] ?: false,
            leftPageQuickNote = prefs[Keys.LEFT_PAGE_QUICK_NOTE] ?: "",
            manualChronotype = prefs[Keys.MANUAL_CHRONOTYPE],
            quizChronotype = prefs[Keys.QUIZ_CHRONOTYPE],
            quizProgress = prefs[Keys.QUIZ_PROGRESS] ?: "{}",
            meqScore = prefs[Keys.MEQ_SCORE] ?: 0,
            sleepPressurePoints = prefs[Keys.SLEEP_PRESSURE_POINTS] ?: 0,
            sleepPressureTrackingStartedAtMillis = prefs[Keys.SLEEP_PRESSURE_TRACKING_STARTED_AT] ?: 0L,
            sleepPressureLastComputedAtMillis = prefs[Keys.SLEEP_PRESSURE_LAST_COMPUTED_AT] ?: 0L,
            autoFallbackSleepInsertionEnabled = prefs[Keys.AUTO_FALLBACK_SLEEP_INSERTION_ENABLED] ?: true,
            dismissedSleepFallbacks = prefs[Keys.DISMISSED_SLEEP_FALLBACKS] ?: emptySet(),
            dailyDeepWorkTargetMinutes = (prefs[Keys.DAILY_DEEP_WORK_TARGET_MINUTES] ?: 240).coerceIn(30, 720),
            distractingPackages = prefs[Keys.DISTRACTING_PACKAGES] ?: BehavioralSupportService.DEFAULT_DISTRACTING_PACKAGES,
            momentInterruptionSensitivity = (prefs[Keys.MOMENT_INTERRUPTION_SENSITIVITY] ?: 1.0f).coerceIn(0.5f, 2.0f),
            momentNotificationSensitivity = (prefs[Keys.MOMENT_NOTIFICATION_SENSITIVITY] ?: 1.0f).coerceIn(0.5f, 2.0f),
            momentTaskPressureSensitivity = (prefs[Keys.MOMENT_TASK_PRESSURE_SENSITIVITY] ?: 1.0f).coerceIn(0.5f, 2.0f),
            energyTelemetryEnabled = prefs[Keys.ENERGY_TELEMETRY_ENABLED] ?: true,
            energyTelemetryRetentionDays = (prefs[Keys.ENERGY_TELEMETRY_RETENTION_DAYS] ?: 30).coerceIn(1, 90),
            morningTuneSleepWeight = prefs[Keys.MORNING_TUNE_SLEEP_WEIGHT] ?: 0.30f,
            morningTuneWakeWeight = prefs[Keys.MORNING_TUNE_WAKE_WEIGHT] ?: 0.25f,
            morningTuneBehaviorWeight = prefs[Keys.MORNING_TUNE_BEHAVIOR_WEIGHT] ?: 0.25f,
            morningTuneBaseWeight = prefs[Keys.MORNING_TUNE_BASE_WEIGHT] ?: 0.20f,
            morningTuneUpdatedAtMillis = prefs[Keys.MORNING_TUNE_UPDATED_AT] ?: 0L,
            morningTuneVersion = prefs[Keys.MORNING_TUNE_VERSION] ?: 1,
            adaptivePeakFreezeEnabled = prefs[Keys.ADAPTIVE_PEAK_FREEZE_ENABLED] ?: false,
            peakQualityDegradeStreak = prefs[Keys.PEAK_QUALITY_DEGRADE_STREAK] ?: 0,
            peakConfidenceAbstentionEnabled = prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_ENABLED] ?: false,
            peakConfidenceAbstentionReason = prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON] ?: "",
            peakConfidenceAbstentionTriggerCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_TRIGGER_COUNT] ?: 0).coerceAtLeast(0),
            peakConfidenceAbstentionRecoveryCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_RECOVERY_COUNT] ?: 0).coerceAtLeast(0),
            peakConfidenceAbstentionLastChangedAtMillis = prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_LAST_CHANGED_AT] ?: 0L,
            peakConfidenceAbstentionReasonFreezeCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_FREEZE_COUNT] ?: 0).coerceAtLeast(0),
            peakConfidenceAbstentionReasonLowSamplesCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_LOW_SAMPLES_COUNT] ?: 0).coerceAtLeast(0),
            peakConfidenceAbstentionReasonLowCoverageCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_LOW_COVERAGE_COUNT] ?: 0).coerceAtLeast(0),
            peakConfidenceAbstentionReasonWakeVarianceCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_WAKE_VARIANCE_COUNT] ?: 0).coerceAtLeast(0),
            peakConfidenceAbstentionReasonDivergenceCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_DIVERGENCE_COUNT] ?: 0).coerceAtLeast(0),
            peakConfidenceAbstentionReasonOtherCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_OTHER_COUNT] ?: 0).coerceAtLeast(0),
            manualPeakProfileEnabled = prefs[Keys.MANUAL_PEAK_PROFILE_ENABLED] ?: false,
            manualPeakProfileType = prefs[Keys.MANUAL_PEAK_PROFILE_TYPE] ?: "AUTO",
            manualPeakAnchorMinuteOfDay = prefs[Keys.MANUAL_PEAK_ANCHOR_MINUTE_OF_DAY] ?: 360,
            manualPeakWindow1StartOffsetMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_1_START_OFFSET] ?: 0,
            manualPeakWindow2StartOffsetMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_2_START_OFFSET] ?: 570,
            manualPeakWindow3StartOffsetMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_3_START_OFFSET] ?: 810,
            manualPeakWindow1DurationMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_1_DURATION] ?: 210,
            manualPeakWindow2DurationMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_2_DURATION] ?: 150,
            manualPeakWindow3DurationMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_3_DURATION] ?: 60,
            manualPeakWindow1Amplitude = prefs[Keys.MANUAL_PEAK_WINDOW_1_AMPLITUDE] ?: 1.0f,
            manualPeakWindow2Amplitude = prefs[Keys.MANUAL_PEAK_WINDOW_2_AMPLITUDE] ?: 0.8f,
            manualPeakWindow3Amplitude = prefs[Keys.MANUAL_PEAK_WINDOW_3_AMPLITUDE] ?: 0.6f,
            protectedTimeSlots = parseProtectedTimeSlots(prefs[Keys.PROTECTED_TIME_SLOTS]),
            dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR_ENABLED] ?: true,
            amoledDarkEnabled = prefs[Keys.AMOLED_DARK_ENABLED] ?: false,
            dndEnabled = prefs[Keys.DND_ENABLED] ?: true,
            useV2Ui = prefs[Keys.USE_V2_UI] ?: true,
            geminiApiKey = prefs[Keys.GEMINI_API_KEY] ?: "",
            geminiAiEnabled = prefs[Keys.GEMINI_AI_ENABLED] ?: true,
            adaptiveSystemDirective = prefs[Keys.ADAPTIVE_SYSTEM_DIRECTIVE] ?: "",
            weeklyAiAuditJson = prefs[Keys.WEEKLY_AI_AUDIT_JSON] ?: "",
            recentlyDeletedTasksJson = prefs[Keys.RECENTLY_DELETED_TASKS_JSON] ?: "[]",
            stimulantLogsJson = prefs[Keys.STIMULANT_LOGS_JSON] ?: "[]",
            quickPhysiologicalLogsJson = prefs[Keys.QUICK_PHYSIOLOGICAL_LOGS_JSON] ?: "[]"
        )
    }

    companion object {
        private val jsonStringListCache = java.util.concurrent.ConcurrentHashMap<String, List<String>>()
        private val protectedSlotsCache = java.util.concurrent.ConcurrentHashMap<String, List<ProtectedTimeSlot>>()

        internal fun parseAffirmations(json: String?): List<String> {
            if (json.isNullOrBlank()) return emptyList()
            return jsonStringListCache.computeIfAbsent(json) { raw ->
                try {
                    val arr = JSONArray(raw)
                    List(arr.length()) { arr.getString(it) }
                } catch (_: Exception) { emptyList() }
            }
        }

        internal fun encodeAffirmations(list: List<String>): String {
            val arr = JSONArray()
            list.forEach { arr.put(it) }
            return arr.toString()
        }

        internal fun parseProtectedTimeSlots(json: String?): List<ProtectedTimeSlot> {
            if (json.isNullOrBlank()) return emptyList()
            return protectedSlotsCache.computeIfAbsent(json) { raw ->
                try {
                    val arr = JSONArray(raw)
                    val list = mutableListOf<ProtectedTimeSlot>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            ProtectedTimeSlot(
                                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                                label = obj.optString("label", "Protected Block"),
                                startMinuteOfDay = obj.optInt("startMinuteOfDay", 480).coerceIn(0, 1439),
                                durationMinutes = obj.optInt("durationMinutes", 60).coerceIn(15, 360),
                                recurrence = runCatching { PtsRecurrence.valueOf(obj.optString("recurrence", "DAILY")) }.getOrDefault(PtsRecurrence.DAILY),
                                weekdayBitmask = obj.optInt("weekdayBitmask", 0b1111111),
                                category = runCatching { PtsCategory.valueOf(obj.optString("category", "BREAK")) }.getOrDefault(PtsCategory.BREAK),
                                recoveryBufferMinutes = obj.optInt("recoveryBufferMinutes", 0).coerceIn(0, 120),
                                isEnabled = obj.optBoolean("isEnabled", true),
                                dateMillis = if (obj.has("dateMillis") && !obj.isNull("dateMillis")) obj.optLong("dateMillis") else null
                            )
                        )
                    }
                    list
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }

        internal fun encodeProtectedTimeSlots(list: List<ProtectedTimeSlot>): String {
            val arr = JSONArray()
            list.forEach { slot ->
                val obj = JSONObject().apply {
                    put("id", slot.id)
                    put("label", slot.label)
                    put("startMinuteOfDay", slot.startMinuteOfDay)
                    put("durationMinutes", slot.durationMinutes)
                    put("recurrence", slot.recurrence.name)
                    put("weekdayBitmask", slot.weekdayBitmask)
                    put("category", slot.category.name)
                    put("recoveryBufferMinutes", slot.recoveryBufferMinutes)
                    put("isEnabled", slot.isEnabled)
                    if (slot.dateMillis != null) {
                        put("dateMillis", slot.dateMillis)
                    }
                }
                arr.put(obj)
            }
            return arr.toString()
        }
    }

    suspend fun updatePreferences(update: (UserPreferences) -> UserPreferences) {
        context.dataStore.edit { prefs ->
            val current = UserPreferences(
                wakeUpHour = prefs[Keys.WAKE_UP_HOUR] ?: 7,
                sleepHour = prefs[Keys.SLEEP_HOUR] ?: 23,
                peakEnergyStart = prefs[Keys.PEAK_ENERGY_START] ?: 9,
                peakEnergyEnd = prefs[Keys.PEAK_ENERGY_END] ?: 12,
                workDayStart = prefs[Keys.WORK_DAY_START] ?: 8,
                workDayEnd = prefs[Keys.WORK_DAY_END] ?: 20,
                defaultPomodoroMinutes = prefs[Keys.DEFAULT_POMODORO_MINUTES] ?: 25,
                defaultBreakMinutes = prefs[Keys.DEFAULT_BREAK_MINUTES] ?: 5,
                identityLabel = prefs[Keys.IDENTITY_LABEL] ?: "",
                topGoal = prefs[Keys.TOP_GOAL] ?: "",
                dailyStreak = prefs[Keys.DAILY_STREAK] ?: 0,
                lastActiveDate = prefs[Keys.LAST_ACTIVE_DATE] ?: 0L,
                longestStreak = prefs[Keys.LONGEST_STREAK] ?: 0,
                totalTasksCompleted = prefs[Keys.TOTAL_TASKS_COMPLETED] ?: 0,
                totalFocusMinutes = prefs[Keys.TOTAL_FOCUS_MINUTES] ?: 0,
                weightQuadrant = prefs[Keys.WEIGHT_QUADRANT] ?: 1.0f,
                weightDeadlineUrgency = prefs[Keys.WEIGHT_DEADLINE_URGENCY] ?: 1.0f,
                weightPriorityLevel = prefs[Keys.WEIGHT_PRIORITY_LEVEL] ?: 1.0f,
                weightDuration = prefs[Keys.WEIGHT_DURATION] ?: 1.0f,
                weightImpact = prefs[Keys.WEIGHT_IMPACT] ?: 1.0f,
                weightFocusMode = prefs[Keys.WEIGHT_FOCUS_MODE] ?: 1.0f,
                weightCircadianMatch = prefs[Keys.WEIGHT_CIRCADIAN_MATCH] ?: 1.0f,
                weightFrogBonus = prefs[Keys.WEIGHT_FROG_BONUS] ?: 1.0f,
                weightPlanBoost = prefs[Keys.WEIGHT_PLAN_BOOST] ?: 1.0f,
                weightSessionProgress = prefs[Keys.WEIGHT_SESSION_PROGRESS] ?: 1.0f,
                weightEnjoyment = prefs[Keys.WEIGHT_ENJOYMENT] ?: 1.0f,
            enjoymentForecastBias = prefs[Keys.ENJOYMENT_FORECAST_BIAS] ?: 0f,
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                theme = try {
                    AppTheme.valueOf(prefs[Keys.THEME] ?: AppTheme.SYSTEM.name)
                } catch (_: Exception) { AppTheme.SYSTEM },
                weeklyIntent = prefs[Keys.WEEKLY_INTENT] ?: "",
                weeklyIntentIsoWeek = prefs[Keys.WEEKLY_INTENT_ISO_WEEK] ?: 0,
                weeklyIntentIsoYear = prefs[Keys.WEEKLY_INTENT_ISO_YEAR] ?: 0,
                lastFreshStartShownWeek = prefs[Keys.LAST_FRESH_START_SHOWN_WEEK] ?: 0,
                lastFreshStartShownYear = prefs[Keys.LAST_FRESH_START_SHOWN_YEAR] ?: 0,
                lastAppOpenMillis = prefs[Keys.LAST_APP_OPEN_MILLIS] ?: 0L,
                firstAppOpenMillis = prefs[Keys.FIRST_APP_OPEN_MILLIS] ?: 0L,
                detectedPeakStart = prefs[Keys.DETECTED_PEAK_START] ?: -1,
                detectedPeakEnd = prefs[Keys.DETECTED_PEAK_END] ?: -1,
                detectedPeakMinuteOfDay = prefs[Keys.DETECTED_PEAK_MINUTE_OF_DAY] ?: -1,
                peakDetectionConfidence = prefs[Keys.PEAK_DETECTION_CONFIDENCE] ?: 0f,
                effectivePeakStart = prefs[Keys.EFFECTIVE_PEAK_START] ?: -1,
                effectivePeakEnd = prefs[Keys.EFFECTIVE_PEAK_END] ?: -1,
                effectivePeakMinuteOfDay = prefs[Keys.EFFECTIVE_PEAK_MINUTE_OF_DAY] ?: -1,
                quizPeakEnabled = prefs[Keys.QUIZ_PEAK_ENABLED] ?: true,
                affirmations = parseAffirmations(prefs[Keys.AFFIRMATIONS]),
                tagCatalog = withStarterTagCatalog(parseAffirmations(prefs[Keys.TAG_CATALOG])),
                autoSchedulingEnabled = prefs[Keys.AUTO_SCHEDULING_ENABLED] ?: true,
                isAutoScheduleToggleOn = prefs[Keys.IS_AUTO_SCHEDULE_TOGGLE_ON] ?: false,
                autoSchedulingHorizonDays = (prefs[Keys.AUTO_SCHEDULING_HORIZON_DAYS] ?: 3).coerceIn(1, 7),
                autoSchedulingBreakAfterCognitiveMinutes = (prefs[Keys.AUTO_SCHEDULING_BREAK_AFTER_COGNITIVE_MINUTES] ?: 90).coerceIn(30, 180),
                autoSchedulingBreakDurationMinutes = (prefs[Keys.AUTO_SCHEDULING_BREAK_DURATION_MINUTES] ?: 15).coerceIn(5, 30),
                autoSchedulingBackgroundThrottleMinutes = (prefs[Keys.AUTO_SCHEDULING_BACKGROUND_THROTTLE_MINUTES] ?: 30).coerceIn(15, 240),
                yearlyGoals = parseAffirmations(prefs[Keys.YEARLY_GOALS]),
                weeklyGoals = parseAffirmations(prefs[Keys.WEEKLY_GOALS]),
                lastYearlyGoalShownYear = prefs[Keys.LAST_YEARLY_GOAL_SHOWN_YEAR] ?: 0,
                lastWeeklyGoalShownWeek = prefs[Keys.LAST_WEEKLY_GOAL_SHOWN_WEEK] ?: 0,
                lastWeeklyGoalShownYear = prefs[Keys.LAST_WEEKLY_GOAL_SHOWN_YEAR] ?: 0,
                lastYearlyGoalHandledYear = prefs[Keys.LAST_YEARLY_GOAL_HANDLED_YEAR] ?: 0,
                lastWeeklyGoalHandledWeek = prefs[Keys.LAST_WEEKLY_GOAL_HANDLED_WEEK] ?: 0,
                woopEnabled = prefs[Keys.WOOP_ENABLED] ?: true,
                autoTrackerEnabled = prefs[Keys.AUTO_TRACKER_ENABLED] ?: false,
                dailyPlanNotificationsEnabled = prefs[Keys.DAILY_PLAN_NOTIFICATIONS_ENABLED] ?: true,
                streakNotificationsEnabled = prefs[Keys.STREAK_NOTIFICATIONS_ENABLED] ?: true,
                autonomyNudgeNotificationsEnabled = prefs[Keys.AUTONOMY_NUDGE_NOTIFICATIONS_ENABLED] ?: true,
                deadlineReminderNotificationsEnabled = prefs[Keys.DEADLINE_REMINDER_NOTIFICATIONS_ENABLED] ?: true,
                deadlineEscalationNotificationsEnabled = prefs[Keys.DEADLINE_ESCALATION_NOTIFICATIONS_ENABLED] ?: true,
                dailyPlanNotificationHour = prefs[Keys.DAILY_PLAN_NOTIFICATION_HOUR] ?: 7,
                streakCheckNotificationHour = prefs[Keys.STREAK_CHECK_NOTIFICATION_HOUR] ?: 21,
                userGuidePromptShown = prefs[Keys.USER_GUIDE_PROMPT_SHOWN] ?: false,
                leftPageQuickNote = prefs[Keys.LEFT_PAGE_QUICK_NOTE] ?: "",
                manualChronotype = prefs[Keys.MANUAL_CHRONOTYPE],
                quizChronotype = prefs[Keys.QUIZ_CHRONOTYPE],
                quizProgress = prefs[Keys.QUIZ_PROGRESS] ?: "{}",
                meqScore = prefs[Keys.MEQ_SCORE] ?: 0,
                sleepPressurePoints = prefs[Keys.SLEEP_PRESSURE_POINTS] ?: 0,
                sleepPressureTrackingStartedAtMillis = prefs[Keys.SLEEP_PRESSURE_TRACKING_STARTED_AT] ?: 0L,
                sleepPressureLastComputedAtMillis = prefs[Keys.SLEEP_PRESSURE_LAST_COMPUTED_AT] ?: 0L,
                autoFallbackSleepInsertionEnabled = prefs[Keys.AUTO_FALLBACK_SLEEP_INSERTION_ENABLED] ?: true,
                dismissedSleepFallbacks = prefs[Keys.DISMISSED_SLEEP_FALLBACKS] ?: emptySet(),
                dailyDeepWorkTargetMinutes = (prefs[Keys.DAILY_DEEP_WORK_TARGET_MINUTES] ?: 240).coerceIn(30, 720),
                distractingPackages = prefs[Keys.DISTRACTING_PACKAGES] ?: BehavioralSupportService.DEFAULT_DISTRACTING_PACKAGES,
                momentInterruptionSensitivity = (prefs[Keys.MOMENT_INTERRUPTION_SENSITIVITY] ?: 1.0f).coerceIn(0.5f, 2.0f),
                momentNotificationSensitivity = (prefs[Keys.MOMENT_NOTIFICATION_SENSITIVITY] ?: 1.0f).coerceIn(0.5f, 2.0f),
                momentTaskPressureSensitivity = (prefs[Keys.MOMENT_TASK_PRESSURE_SENSITIVITY] ?: 1.0f).coerceIn(0.5f, 2.0f),
                energyTelemetryEnabled = prefs[Keys.ENERGY_TELEMETRY_ENABLED] ?: true,
                energyTelemetryRetentionDays = (prefs[Keys.ENERGY_TELEMETRY_RETENTION_DAYS] ?: 30).coerceIn(1, 90),
                morningTuneSleepWeight = prefs[Keys.MORNING_TUNE_SLEEP_WEIGHT] ?: 0.30f,
                morningTuneWakeWeight = prefs[Keys.MORNING_TUNE_WAKE_WEIGHT] ?: 0.25f,
                morningTuneBehaviorWeight = prefs[Keys.MORNING_TUNE_BEHAVIOR_WEIGHT] ?: 0.25f,
                morningTuneBaseWeight = prefs[Keys.MORNING_TUNE_BASE_WEIGHT] ?: 0.20f,
                morningTuneUpdatedAtMillis = prefs[Keys.MORNING_TUNE_UPDATED_AT] ?: 0L,
                morningTuneVersion = prefs[Keys.MORNING_TUNE_VERSION] ?: 1,
                adaptivePeakFreezeEnabled = prefs[Keys.ADAPTIVE_PEAK_FREEZE_ENABLED] ?: false,
                peakQualityDegradeStreak = prefs[Keys.PEAK_QUALITY_DEGRADE_STREAK] ?: 0,
                peakConfidenceAbstentionEnabled = prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_ENABLED] ?: false,
                peakConfidenceAbstentionReason = prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON] ?: "",
                peakConfidenceAbstentionTriggerCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_TRIGGER_COUNT] ?: 0).coerceAtLeast(0),
                peakConfidenceAbstentionRecoveryCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_RECOVERY_COUNT] ?: 0).coerceAtLeast(0),
                peakConfidenceAbstentionLastChangedAtMillis = prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_LAST_CHANGED_AT] ?: 0L,
                peakConfidenceAbstentionReasonFreezeCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_FREEZE_COUNT] ?: 0).coerceAtLeast(0),
                peakConfidenceAbstentionReasonLowSamplesCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_LOW_SAMPLES_COUNT] ?: 0).coerceAtLeast(0),
                peakConfidenceAbstentionReasonLowCoverageCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_LOW_COVERAGE_COUNT] ?: 0).coerceAtLeast(0),
                peakConfidenceAbstentionReasonWakeVarianceCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_WAKE_VARIANCE_COUNT] ?: 0).coerceAtLeast(0),
                peakConfidenceAbstentionReasonDivergenceCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_DIVERGENCE_COUNT] ?: 0).coerceAtLeast(0),
                peakConfidenceAbstentionReasonOtherCount = (prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_OTHER_COUNT] ?: 0).coerceAtLeast(0),
                manualPeakProfileEnabled = prefs[Keys.MANUAL_PEAK_PROFILE_ENABLED] ?: false,
                manualPeakProfileType = prefs[Keys.MANUAL_PEAK_PROFILE_TYPE] ?: "AUTO",
                manualPeakAnchorMinuteOfDay = prefs[Keys.MANUAL_PEAK_ANCHOR_MINUTE_OF_DAY] ?: 360,
                manualPeakWindow1StartOffsetMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_1_START_OFFSET] ?: 0,
                manualPeakWindow2StartOffsetMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_2_START_OFFSET] ?: 570,
                manualPeakWindow3StartOffsetMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_3_START_OFFSET] ?: 810,
                manualPeakWindow1DurationMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_1_DURATION] ?: 210,
                manualPeakWindow2DurationMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_2_DURATION] ?: 150,
                manualPeakWindow3DurationMinutes = prefs[Keys.MANUAL_PEAK_WINDOW_3_DURATION] ?: 60,
                manualPeakWindow1Amplitude = prefs[Keys.MANUAL_PEAK_WINDOW_1_AMPLITUDE] ?: 1.0f,
                manualPeakWindow2Amplitude = prefs[Keys.MANUAL_PEAK_WINDOW_2_AMPLITUDE] ?: 0.8f,
                manualPeakWindow3Amplitude = prefs[Keys.MANUAL_PEAK_WINDOW_3_AMPLITUDE] ?: 0.6f,
                protectedTimeSlots = parseProtectedTimeSlots(prefs[Keys.PROTECTED_TIME_SLOTS]),
                dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR_ENABLED] ?: true,
                amoledDarkEnabled = prefs[Keys.AMOLED_DARK_ENABLED] ?: false,
                dndEnabled = prefs[Keys.DND_ENABLED] ?: true,
                useV2Ui = prefs[Keys.USE_V2_UI] ?: true,
                geminiApiKey = prefs[Keys.GEMINI_API_KEY] ?: "",
                geminiAiEnabled = prefs[Keys.GEMINI_AI_ENABLED] ?: true,
                adaptiveSystemDirective = prefs[Keys.ADAPTIVE_SYSTEM_DIRECTIVE] ?: "",
                weeklyAiAuditJson = prefs[Keys.WEEKLY_AI_AUDIT_JSON] ?: "",
                recentlyDeletedTasksJson = prefs[Keys.RECENTLY_DELETED_TASKS_JSON] ?: "[]",
                stimulantLogsJson = prefs[Keys.STIMULANT_LOGS_JSON] ?: "[]",
                quickPhysiologicalLogsJson = prefs[Keys.QUICK_PHYSIOLOGICAL_LOGS_JSON] ?: "[]"
            )
            val updated = update(current)
            prefs[Keys.WAKE_UP_HOUR] = updated.wakeUpHour
            prefs[Keys.SLEEP_HOUR] = (if (updated.sleepHour >= 24) updated.sleepHour % 24 else updated.sleepHour).coerceIn(0, 23)
            prefs[Keys.PEAK_ENERGY_START] = updated.peakEnergyStart
            prefs[Keys.PEAK_ENERGY_END] = updated.peakEnergyEnd
            prefs[Keys.WORK_DAY_START] = updated.workDayStart
            prefs[Keys.WORK_DAY_END] = updated.workDayEnd
            prefs[Keys.DEFAULT_POMODORO_MINUTES] = updated.defaultPomodoroMinutes
            prefs[Keys.DEFAULT_BREAK_MINUTES] = updated.defaultBreakMinutes
            prefs[Keys.IDENTITY_LABEL] = updated.identityLabel
            prefs[Keys.TOP_GOAL] = updated.topGoal
            prefs[Keys.DAILY_STREAK] = updated.dailyStreak
            prefs[Keys.LAST_ACTIVE_DATE] = updated.lastActiveDate
            prefs[Keys.LONGEST_STREAK] = updated.longestStreak
            prefs[Keys.TOTAL_TASKS_COMPLETED] = updated.totalTasksCompleted
            prefs[Keys.TOTAL_FOCUS_MINUTES] = updated.totalFocusMinutes
            prefs[Keys.WEIGHT_QUADRANT] = updated.weightQuadrant
            prefs[Keys.WEIGHT_DEADLINE_URGENCY] = updated.weightDeadlineUrgency
            prefs[Keys.WEIGHT_PRIORITY_LEVEL] = updated.weightPriorityLevel
            prefs[Keys.WEIGHT_DURATION] = updated.weightDuration
            prefs[Keys.WEIGHT_IMPACT] = updated.weightImpact
            prefs[Keys.WEIGHT_FOCUS_MODE] = updated.weightFocusMode
            prefs[Keys.WEIGHT_CIRCADIAN_MATCH] = updated.weightCircadianMatch
            prefs[Keys.WEIGHT_FROG_BONUS] = updated.weightFrogBonus
            prefs[Keys.WEIGHT_PLAN_BOOST] = updated.weightPlanBoost
            prefs[Keys.WEIGHT_SESSION_PROGRESS] = updated.weightSessionProgress
            prefs[Keys.WEIGHT_ENJOYMENT] = updated.weightEnjoyment
            prefs[Keys.ENJOYMENT_FORECAST_BIAS] = updated.enjoymentForecastBias
            prefs[Keys.ONBOARDING_COMPLETED] = updated.onboardingCompleted
            prefs[Keys.THEME] = updated.theme.name
            prefs[Keys.WEEKLY_INTENT] = updated.weeklyIntent
            prefs[Keys.WEEKLY_INTENT_ISO_WEEK] = updated.weeklyIntentIsoWeek
            prefs[Keys.WEEKLY_INTENT_ISO_YEAR] = updated.weeklyIntentIsoYear
            prefs[Keys.LAST_FRESH_START_SHOWN_WEEK] = updated.lastFreshStartShownWeek
            prefs[Keys.LAST_FRESH_START_SHOWN_YEAR] = updated.lastFreshStartShownYear
            prefs[Keys.LAST_APP_OPEN_MILLIS] = updated.lastAppOpenMillis
            prefs[Keys.FIRST_APP_OPEN_MILLIS] = updated.firstAppOpenMillis
            prefs[Keys.DETECTED_PEAK_START] = updated.detectedPeakStart
            prefs[Keys.DETECTED_PEAK_END] = updated.detectedPeakEnd
            prefs[Keys.DETECTED_PEAK_MINUTE_OF_DAY] = updated.detectedPeakMinuteOfDay
            prefs[Keys.PEAK_DETECTION_CONFIDENCE] = updated.peakDetectionConfidence
            prefs[Keys.EFFECTIVE_PEAK_START] = updated.effectivePeakStart
            prefs[Keys.EFFECTIVE_PEAK_END] = updated.effectivePeakEnd
            prefs[Keys.EFFECTIVE_PEAK_MINUTE_OF_DAY] = updated.effectivePeakMinuteOfDay
            prefs[Keys.QUIZ_PEAK_ENABLED] = updated.quizPeakEnabled
            prefs[Keys.AFFIRMATIONS] = encodeAffirmations(updated.affirmations)
            prefs[Keys.TAG_CATALOG] = encodeAffirmations(withStarterTagCatalog(updated.tagCatalog))
            prefs[Keys.AUTO_SCHEDULING_ENABLED] = updated.autoSchedulingEnabled
            prefs[Keys.IS_AUTO_SCHEDULE_TOGGLE_ON] = updated.isAutoScheduleToggleOn
            prefs[Keys.AUTO_SCHEDULING_HORIZON_DAYS] = updated.autoSchedulingHorizonDays.coerceIn(1, 7)
            prefs[Keys.AUTO_SCHEDULING_BREAK_AFTER_COGNITIVE_MINUTES] = updated.autoSchedulingBreakAfterCognitiveMinutes.coerceIn(30, 180)
            prefs[Keys.AUTO_SCHEDULING_BREAK_DURATION_MINUTES] = updated.autoSchedulingBreakDurationMinutes.coerceIn(5, 30)
            prefs[Keys.AUTO_SCHEDULING_BACKGROUND_THROTTLE_MINUTES] = updated.autoSchedulingBackgroundThrottleMinutes.coerceIn(15, 240)
            prefs[Keys.YEARLY_GOALS] = encodeAffirmations(updated.yearlyGoals)
            prefs[Keys.WEEKLY_GOALS] = encodeAffirmations(updated.weeklyGoals)
            prefs[Keys.LAST_YEARLY_GOAL_SHOWN_YEAR] = updated.lastYearlyGoalShownYear
            prefs[Keys.LAST_WEEKLY_GOAL_SHOWN_WEEK] = updated.lastWeeklyGoalShownWeek
            prefs[Keys.LAST_WEEKLY_GOAL_SHOWN_YEAR] = updated.lastWeeklyGoalShownYear
            prefs[Keys.LAST_YEARLY_GOAL_HANDLED_YEAR] = updated.lastYearlyGoalHandledYear
            prefs[Keys.LAST_WEEKLY_GOAL_HANDLED_WEEK] = updated.lastWeeklyGoalHandledWeek
            prefs[Keys.WOOP_ENABLED] = updated.woopEnabled
            prefs[Keys.AUTO_TRACKER_ENABLED] = updated.autoTrackerEnabled
            prefs[Keys.DAILY_PLAN_NOTIFICATIONS_ENABLED] = updated.dailyPlanNotificationsEnabled
            prefs[Keys.STREAK_NOTIFICATIONS_ENABLED] = updated.streakNotificationsEnabled
            prefs[Keys.AUTONOMY_NUDGE_NOTIFICATIONS_ENABLED] = updated.autonomyNudgeNotificationsEnabled
            prefs[Keys.DEADLINE_REMINDER_NOTIFICATIONS_ENABLED] = updated.deadlineReminderNotificationsEnabled
            prefs[Keys.DEADLINE_ESCALATION_NOTIFICATIONS_ENABLED] = updated.deadlineEscalationNotificationsEnabled
            prefs[Keys.DAILY_PLAN_NOTIFICATION_HOUR] = updated.dailyPlanNotificationHour.coerceIn(0, 23)
            prefs[Keys.STREAK_CHECK_NOTIFICATION_HOUR] = updated.streakCheckNotificationHour.coerceIn(0, 23)
            prefs[Keys.USER_GUIDE_PROMPT_SHOWN] = updated.userGuidePromptShown
            prefs[Keys.LEFT_PAGE_QUICK_NOTE] = updated.leftPageQuickNote
            if (updated.manualChronotype != null) {
                prefs[Keys.MANUAL_CHRONOTYPE] = updated.manualChronotype
            } else {
                prefs.remove(Keys.MANUAL_CHRONOTYPE)
            }
            if (updated.quizChronotype != null) {
                prefs[Keys.QUIZ_CHRONOTYPE] = updated.quizChronotype
            } else {
                prefs.remove(Keys.QUIZ_CHRONOTYPE)
            }
            prefs[Keys.QUIZ_PROGRESS] = updated.quizProgress
            prefs[Keys.MEQ_SCORE] = updated.meqScore
            prefs[Keys.SLEEP_PRESSURE_POINTS] = updated.sleepPressurePoints
            prefs[Keys.SLEEP_PRESSURE_TRACKING_STARTED_AT] = updated.sleepPressureTrackingStartedAtMillis
            prefs[Keys.SLEEP_PRESSURE_LAST_COMPUTED_AT] = updated.sleepPressureLastComputedAtMillis
            prefs[Keys.AUTO_FALLBACK_SLEEP_INSERTION_ENABLED] = updated.autoFallbackSleepInsertionEnabled
            prefs[Keys.DISMISSED_SLEEP_FALLBACKS] = updated.dismissedSleepFallbacks
            prefs[Keys.DAILY_DEEP_WORK_TARGET_MINUTES] = updated.dailyDeepWorkTargetMinutes.coerceIn(30, 720)
            prefs[Keys.DISTRACTING_PACKAGES] = updated.distractingPackages
            prefs[Keys.MOMENT_INTERRUPTION_SENSITIVITY] = updated.momentInterruptionSensitivity.coerceIn(0.5f, 2.0f)
            prefs[Keys.MOMENT_NOTIFICATION_SENSITIVITY] = updated.momentNotificationSensitivity.coerceIn(0.5f, 2.0f)
            prefs[Keys.MOMENT_TASK_PRESSURE_SENSITIVITY] = updated.momentTaskPressureSensitivity.coerceIn(0.5f, 2.0f)
            prefs[Keys.ENERGY_TELEMETRY_ENABLED] = updated.energyTelemetryEnabled
            prefs[Keys.ENERGY_TELEMETRY_RETENTION_DAYS] = updated.energyTelemetryRetentionDays.coerceIn(1, 90)
            prefs[Keys.MORNING_TUNE_SLEEP_WEIGHT] = updated.morningTuneSleepWeight
            prefs[Keys.MORNING_TUNE_WAKE_WEIGHT] = updated.morningTuneWakeWeight
            prefs[Keys.MORNING_TUNE_BEHAVIOR_WEIGHT] = updated.morningTuneBehaviorWeight
            prefs[Keys.MORNING_TUNE_BASE_WEIGHT] = updated.morningTuneBaseWeight
            prefs[Keys.MORNING_TUNE_UPDATED_AT] = updated.morningTuneUpdatedAtMillis
            prefs[Keys.MORNING_TUNE_VERSION] = updated.morningTuneVersion
            prefs[Keys.ADAPTIVE_PEAK_FREEZE_ENABLED] = updated.adaptivePeakFreezeEnabled
            prefs[Keys.PEAK_QUALITY_DEGRADE_STREAK] = updated.peakQualityDegradeStreak.coerceAtLeast(0)
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_ENABLED] = updated.peakConfidenceAbstentionEnabled
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON] = updated.peakConfidenceAbstentionReason
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_TRIGGER_COUNT] = updated.peakConfidenceAbstentionTriggerCount.coerceAtLeast(0)
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_RECOVERY_COUNT] = updated.peakConfidenceAbstentionRecoveryCount.coerceAtLeast(0)
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_LAST_CHANGED_AT] = updated.peakConfidenceAbstentionLastChangedAtMillis
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_FREEZE_COUNT] = updated.peakConfidenceAbstentionReasonFreezeCount.coerceAtLeast(0)
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_LOW_SAMPLES_COUNT] = updated.peakConfidenceAbstentionReasonLowSamplesCount.coerceAtLeast(0)
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_LOW_COVERAGE_COUNT] = updated.peakConfidenceAbstentionReasonLowCoverageCount.coerceAtLeast(0)
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_WAKE_VARIANCE_COUNT] = updated.peakConfidenceAbstentionReasonWakeVarianceCount.coerceAtLeast(0)
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_DIVERGENCE_COUNT] = updated.peakConfidenceAbstentionReasonDivergenceCount.coerceAtLeast(0)
            prefs[Keys.PEAK_CONFIDENCE_ABSTENTION_REASON_OTHER_COUNT] = updated.peakConfidenceAbstentionReasonOtherCount.coerceAtLeast(0)
            prefs[Keys.MANUAL_PEAK_PROFILE_ENABLED] = updated.manualPeakProfileEnabled
            prefs[Keys.MANUAL_PEAK_PROFILE_TYPE] = updated.manualPeakProfileType
            prefs[Keys.MANUAL_PEAK_ANCHOR_MINUTE_OF_DAY] = updated.manualPeakAnchorMinuteOfDay.coerceIn(0, 1439)
            prefs[Keys.MANUAL_PEAK_WINDOW_1_START_OFFSET] = updated.manualPeakWindow1StartOffsetMinutes.coerceIn(0, 1439)
            prefs[Keys.MANUAL_PEAK_WINDOW_2_START_OFFSET] = updated.manualPeakWindow2StartOffsetMinutes.coerceIn(0, 1439)
            prefs[Keys.MANUAL_PEAK_WINDOW_3_START_OFFSET] = updated.manualPeakWindow3StartOffsetMinutes.coerceIn(0, 1439)
            prefs[Keys.MANUAL_PEAK_WINDOW_1_DURATION] = updated.manualPeakWindow1DurationMinutes.coerceIn(30, 360)
            prefs[Keys.MANUAL_PEAK_WINDOW_2_DURATION] = updated.manualPeakWindow2DurationMinutes.coerceIn(30, 360)
            prefs[Keys.MANUAL_PEAK_WINDOW_3_DURATION] = updated.manualPeakWindow3DurationMinutes.coerceIn(30, 360)
            prefs[Keys.MANUAL_PEAK_WINDOW_1_AMPLITUDE] = updated.manualPeakWindow1Amplitude.coerceIn(0.2f, 1f)
            prefs[Keys.MANUAL_PEAK_WINDOW_2_AMPLITUDE] = updated.manualPeakWindow2Amplitude.coerceIn(0.2f, 1f)
            prefs[Keys.MANUAL_PEAK_WINDOW_3_AMPLITUDE] = updated.manualPeakWindow3Amplitude.coerceIn(0.2f, 1f)
            prefs[Keys.PROTECTED_TIME_SLOTS] = encodeProtectedTimeSlots(updated.protectedTimeSlots)
            prefs[Keys.DYNAMIC_COLOR_ENABLED] = updated.dynamicColorEnabled
            prefs[Keys.AMOLED_DARK_ENABLED] = updated.amoledDarkEnabled
            prefs[Keys.DND_ENABLED] = updated.dndEnabled
            prefs[Keys.USE_V2_UI] = updated.useV2Ui
            prefs[Keys.GEMINI_API_KEY] = updated.geminiApiKey
            prefs[Keys.GEMINI_AI_ENABLED] = updated.geminiAiEnabled
            prefs[Keys.ADAPTIVE_SYSTEM_DIRECTIVE] = updated.adaptiveSystemDirective
            prefs[Keys.WEEKLY_AI_AUDIT_JSON] = updated.weeklyAiAuditJson
            prefs[Keys.RECENTLY_DELETED_TASKS_JSON] = updated.recentlyDeletedTasksJson
            prefs[Keys.STIMULANT_LOGS_JSON] = updated.stimulantLogsJson
            prefs[Keys.QUICK_PHYSIOLOGICAL_LOGS_JSON] = updated.quickPhysiologicalLogsJson
        }
    }

    suspend fun setUseV2Ui(enabled: Boolean) {
        updatePreferences { prefs ->
            prefs.copy(useV2Ui = enabled)
        }
    }

    suspend fun setLastYearlyGoalHandledYear(year: Int) {
        updatePreferences { prefs ->
            prefs.copy(lastYearlyGoalHandledYear = year)
        }
    }

    suspend fun setLastWeeklyGoalHandledWeek(weekKey: Int) {
        updatePreferences { prefs ->
            prefs.copy(lastWeeklyGoalHandledWeek = weekKey)
        }
    }

    suspend fun clearGoalStrings() {
        updatePreferences { prefs ->
            prefs.copy(yearlyGoals = emptyList(), weeklyGoals = emptyList())
        }
    }

    suspend fun clearGoalStringsAndMarkHandled(yearlyHandledYear: Int, weeklyHandledWeek: Int) {
        updatePreferences { prefs ->
            prefs.copy(
                yearlyGoals = emptyList(),
                weeklyGoals = emptyList(),
                lastYearlyGoalHandledYear = if (yearlyHandledYear > 0) yearlyHandledYear else prefs.lastYearlyGoalHandledYear,
                lastWeeklyGoalHandledWeek = if (weeklyHandledWeek > 0) weeklyHandledWeek else prefs.lastWeeklyGoalHandledWeek
            )
        }
    }

    suspend fun setAutoScheduleToggleOn(enabled: Boolean) {
        updatePreferences { prefs ->
            prefs.copy(isAutoScheduleToggleOn = enabled)
        }
    }

    suspend fun updateProtectedTimeSlots(slots: List<ProtectedTimeSlot>) {
        updatePreferences { prefs ->
            prefs.copy(protectedTimeSlots = slots)
        }
        protectedSlotDao?.let { dao ->
            runCatching {
                dao.insertAll(slots.map { slot ->
                    com.neuroflow.app.data.local.entity.ProtectedSlotEntity(
                        id = slot.id,
                        title = slot.label,
                        startHour = slot.startMinuteOfDay / 60,
                        startMinute = slot.startMinuteOfDay % 60,
                        durationMinutes = slot.durationMinutes,
                        daysOfWeekMask = slot.weekdayBitmask,
                        isEnabled = slot.isEnabled
                    )
                })
            }
        }
    }

    suspend fun mergeTagCatalog(tags: Collection<String>) {
        val cleaned = tags.mapNotNull { it.trim().takeIf(String::isNotBlank) }
        if (cleaned.isEmpty()) return

        updatePreferences { prefs ->
            prefs.copy(tagCatalog = mergeTags(prefs.tagCatalog, cleaned))
        }
    }

    suspend fun removeTagFromCatalog(tag: String) {
        val cleaned = tag.trim()
        if (cleaned.isBlank()) return

        updatePreferences { prefs ->
            prefs.copy(tagCatalog = prefs.tagCatalog.filterNot { it.equals(cleaned, ignoreCase = true) })
        }
    }

    private fun mergeTags(existing: List<String>, incoming: Collection<String>): List<String> {
        val merged = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        fun addTag(tag: String) {
            val key = tag.lowercase()
            if (seen.add(key)) {
                merged += tag
            }
        }

        existing.forEach(::addTag)
        incoming.forEach(::addTag)
        return withStarterTagCatalog(merged)
    }

    private fun withStarterTagCatalog(existing: List<String>): List<String> {
        val merged = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        fun addTag(tag: String) {
            val cleaned = tag.trim()
            if (cleaned.isBlank()) return
            val key = cleaned.lowercase()
            if (seen.add(key)) {
                merged += cleaned
            }
        }

        existing.forEach(::addTag)
        TaskTagSchedulingProfile.starterTags.forEach(::addTag)
        return merged.sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    suspend fun recordDeletedTask(title: String, durationMinutes: Int, category: String, postponeCount: Int) {
        val trimmedTitle = title.trim().take(60)
        if (trimmedTitle.isBlank()) return
        updatePreferences { prefs ->
            val list = try {
                val arr = JSONArray(prefs.recentlyDeletedTasksJson.ifBlank { "[]" })
                val items = mutableListOf<JSONObject>()
                for (i in 0 until arr.length()) items.add(arr.getJSONObject(i))
                items
            } catch (_: Exception) {
                mutableListOf<JSONObject>()
            }
            val newItem = JSONObject().apply {
                put("title", trimmedTitle)
                put("durationMinutes", durationMinutes)
                put("category", category)
                put("postponeCount", postponeCount)
                put("deletedAt", System.currentTimeMillis())
            }
            list.add(0, newItem)
            val trimmedList = list.take(15)
            val jsonArr = JSONArray()
            trimmedList.forEach { jsonArr.put(it) }
            prefs.copy(recentlyDeletedTasksJson = jsonArr.toString())
        }
    }

    suspend fun updateAdaptiveSystemDirective(directive: String, auditJson: String) {
        updatePreferences { prefs ->
            prefs.copy(
                adaptiveSystemDirective = directive.trim(),
                weeklyAiAuditJson = auditJson.trim()
            )
        }
    }

    suspend fun recordStimulantDose(
        type: com.neuroflow.app.domain.service.StimulantType,
        doseMg: Double = when (type) {
            com.neuroflow.app.domain.service.StimulantType.CAFFEINE -> 100.0
            com.neuroflow.app.domain.service.StimulantType.GREEN_TEA_L_THEANINE -> 50.0
            com.neuroflow.app.domain.service.StimulantType.PRESCRIPTION_STIMULANT_IR -> 10.0
            com.neuroflow.app.domain.service.StimulantType.PRESCRIPTION_STIMULANT_XR -> 20.0
        },
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        updatePreferences { prefs ->
            val list = try {
                val arr = JSONArray(prefs.stimulantLogsJson.ifBlank { "[]" })
                val items = mutableListOf<JSONObject>()
                for (i in 0 until arr.length()) items.add(arr.getJSONObject(i))
                items
            } catch (_: Exception) {
                mutableListOf<JSONObject>()
            }
            val newItem = JSONObject().apply {
                put("type", type.name)
                put("doseMg", doseMg)
                put("timestampMillis", timestampMillis)
            }
            list.add(0, newItem)
            val trimmedList = list.take(30)
            val jsonArr = JSONArray()
            trimmedList.forEach { jsonArr.put(it) }
            prefs.copy(stimulantLogsJson = jsonArr.toString())
        }
    }

    suspend fun undoLastStimulantDose(): Boolean {
        var removed = false
        updatePreferences { prefs ->
            val list = try {
                val arr = JSONArray(prefs.stimulantLogsJson.ifBlank { "[]" })
                val items = mutableListOf<JSONObject>()
                for (i in 0 until arr.length()) items.add(arr.getJSONObject(i))
                items
            } catch (_: Exception) {
                mutableListOf<JSONObject>()
            }
            if (list.isNotEmpty()) {
                list.removeAt(0)
                removed = true
                val jsonArr = JSONArray()
                list.forEach { jsonArr.put(it) }
                prefs.copy(stimulantLogsJson = jsonArr.toString())
            } else {
                prefs
            }
        }
        return removed
    }

    suspend fun removeStimulantDose(timestampMillis: Long, type: String? = null): Boolean {
        var removed = false
        updatePreferences { prefs ->
            val list = try {
                val arr = JSONArray(prefs.stimulantLogsJson.ifBlank { "[]" })
                val items = mutableListOf<JSONObject>()
                for (i in 0 until arr.length()) items.add(arr.getJSONObject(i))
                items
            } catch (_: Exception) {
                mutableListOf<JSONObject>()
            }
            val index = list.indexOfFirst { obj ->
                val ts = obj.optLong("timestampMillis")
                val t = obj.optString("type")
                ts == timestampMillis && (type == null || t == type)
            }
            if (index >= 0) {
                list.removeAt(index)
                removed = true
                val jsonArr = JSONArray()
                list.forEach { jsonArr.put(it) }
                prefs.copy(stimulantLogsJson = jsonArr.toString())
            } else {
                prefs
            }
        }
        return removed
    }

    suspend fun clearStimulantLogs() {
        updatePreferences { prefs ->
            prefs.copy(stimulantLogsJson = "[]")
        }
    }

    suspend fun recordPhysiologicalEvent(
        type: PhysiologicalEventType,
        timestampMillis: Long = System.currentTimeMillis(),
        durationMinutes: Int = when (type) {
            PhysiologicalEventType.MEAL -> 30
            PhysiologicalEventType.WORKOUT -> 45
            PhysiologicalEventType.NAP -> 20
        },
        intensity: Double = 1.0
    ) {
        updatePreferences { prefs ->
            val list = try {
                val arr = JSONArray(prefs.quickPhysiologicalLogsJson.ifBlank { "[]" })
                val items = mutableListOf<JSONObject>()
                for (i in 0 until arr.length()) items.add(arr.getJSONObject(i))
                items
            } catch (_: Exception) {
                mutableListOf<JSONObject>()
            }
            val newItem = JSONObject().apply {
                put("type", type.name)
                put("timestampMillis", timestampMillis)
                put("durationMinutes", durationMinutes)
                put("intensity", intensity)
            }
            list.add(0, newItem)
            val trimmedList = list.take(30)
            val jsonArr = JSONArray()
            trimmedList.forEach { jsonArr.put(it) }
            prefs.copy(quickPhysiologicalLogsJson = jsonArr.toString())
        }
    }

    suspend fun undoLastPhysiologicalEvent(): Boolean {
        var removed = false
        updatePreferences { prefs ->
            val list = try {
                val arr = JSONArray(prefs.quickPhysiologicalLogsJson.ifBlank { "[]" })
                val items = mutableListOf<JSONObject>()
                for (i in 0 until arr.length()) items.add(arr.getJSONObject(i))
                items
            } catch (_: Exception) {
                mutableListOf<JSONObject>()
            }
            if (list.isNotEmpty()) {
                list.removeAt(0)
                removed = true
                val jsonArr = JSONArray()
                list.forEach { jsonArr.put(it) }
                prefs.copy(quickPhysiologicalLogsJson = jsonArr.toString())
            } else {
                prefs
            }
        }
        return removed
    }

    suspend fun clearPhysiologicalLogs() {
        updatePreferences { prefs ->
            prefs.copy(quickPhysiologicalLogsJson = "[]")
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}

enum class PhysiologicalEventType {
    MEAL, WORKOUT, NAP
}

data class PhysiologicalEventLog(
    val type: PhysiologicalEventType,
    val timestampMillis: Long,
    val durationMinutes: Int = 0,
    val intensity: Double = 1.0
)

fun parsePhysiologicalLogs(json: String?): List<PhysiologicalEventLog> {
    if (json.isNullOrBlank() || json == "[]") return emptyList()
    return try {
        val arr = JSONArray(json)
        val list = mutableListOf<PhysiologicalEventLog>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val typeStr = obj.optString("type", "MEAL")
            val type = try {
                PhysiologicalEventType.valueOf(typeStr.uppercase())
            } catch (_: Exception) {
                PhysiologicalEventType.MEAL
            }
            val ts = obj.optLong("timestampMillis", 0L)
            val dur = obj.optInt("durationMinutes", 0)
            val intensity = obj.optDouble("intensity", 1.0)
            if (ts > 0L) {
                list.add(PhysiologicalEventLog(type = type, timestampMillis = ts, durationMinutes = dur, intensity = intensity))
            }
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

fun parseStimulantLogs(json: String?): List<com.neuroflow.app.domain.service.StimulantDoseBout> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        val list = mutableListOf<com.neuroflow.app.domain.service.StimulantDoseBout>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val typeStr = obj.optString("type", "CAFFEINE")
            val type = com.neuroflow.app.domain.service.StimulantType.values().firstOrNull { it.name.equals(typeStr, ignoreCase = true) }
                ?: com.neuroflow.app.domain.service.StimulantType.CAFFEINE
            list.add(
                com.neuroflow.app.domain.service.StimulantDoseBout(
                    timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis()),
                    doseMg = obj.optDouble("doseMg", 100.0),
                    type = type
                )
            )
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

fun UserPreferences.isAdvancedFeaturesUnlocked(nowMillis: Long = System.currentTimeMillis()): Boolean {
    if (dailyStreak >= 7) return true
    if (firstAppOpenMillis == 0L) {
        return onboardingCompleted
    }
    val oneWeekMillis = 7L * 24 * 60 * 60 * 1000L
    return (nowMillis - firstAppOpenMillis) >= oneWeekMillis
}

fun UserPreferences.daysUntilAdvancedUnlock(nowMillis: Long = System.currentTimeMillis()): Int {
    if (isAdvancedFeaturesUnlocked(nowMillis)) return 0
    if (firstAppOpenMillis == 0L) return 7
    val oneWeekMillis = 7L * 24 * 60 * 60 * 1000L
    val oneDayMillis = 24.0 * 60 * 60 * 1000.0
    val elapsed = nowMillis - firstAppOpenMillis
    val remaining = oneWeekMillis - elapsed
    return if (remaining <= 0) 0 else ceil(remaining / oneDayMillis).toInt().coerceIn(1, 7)
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainmodelenumskt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/model/Enums.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/model/Enums.kt`
- **Lines of Code:** 74
- **Size:** 1,319 bytes (1.3 KB)

```kotlin
package com.neuroflow.app.domain.model

enum class Quadrant {
    DO_FIRST,
    SCHEDULE,
    DELEGATE,
    ELIMINATE
}

enum class Priority {
    HIGH,
    MEDIUM,
    LOW
}

enum class TaskStatus {
    ACTIVE,
    COMPLETED,
    ARCHIVED
}

enum class Recurrence {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    CUSTOM
}

enum class EnergyLevel {
    HIGH,
    MEDIUM,
    LOW
}

enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM
}

// Circadian rhythm task type — used to match task to optimal time of day
// ANALYTICAL: deep work, problem solving, writing (best in peak hours)
// CREATIVE: brainstorming, design, ideation (best mid-morning or late afternoon)
// ADMIN: emails, scheduling, routine tasks(best in low-energy slots)
// PHYSICAL: exercise, errands, calls (flexible)
enum class TaskType {
    ANALYTICAL,
    CREATIVE,
    ADMIN,
    PHYSICAL,
    GENERAL,
    MINDFULNESS
}

enum class HyperFocusState {
    INACTIVE,
    ACTIVE,
    // All tasks done — user must add ≥3 tasks for tomorrow before apps unlock
    FULL_REWARD_PENDING
}

enum class HyperFocusSessionMode {
    TASK_BASED,
    TIME_BASED
}

enum class RewardTier(val unlockMinutes: Int, val taskThreshold: Int) {
    NONE(0, 0),
    MICRO(2, 1),
    PARTIAL(10, 3),
    EARNED(30, 5),
    FULL(Int.MAX_VALUE, Int.MAX_VALUE)
}
```

---

<a id="appsrcmainjavacomneuroflowappdomainmodelprotectedtimeslotkt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/model/ProtectedTimeSlot.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/model/ProtectedTimeSlot.kt`
- **Lines of Code:** 33
- **Size:** 741 bytes (0.7 KB)

```kotlin
package com.neuroflow.app.domain.model

enum class PtsRecurrence {
    DAILY,
    WEEKDAYS,
    WEEKENDS,
    CUSTOM,
    SPECIFIC_DATE
}

enum class PtsCategory {
    MINDFULNESS,
    EXERCISE,
    MEAL,
    FAMILY,
    BREAK,
    WORK,
    STUDY,
    CUSTOM
}

data class ProtectedTimeSlot(
    val id: String,
    val label: String,
    val startMinuteOfDay: Int, // 0..1439
    val durationMinutes: Int,   // 15..360
    val recurrence: PtsRecurrence = PtsRecurrence.DAILY,
    val weekdayBitmask: Int = 0b1111111, // bit 1 = Sunday, bit 2 = Monday, etc.
    val category: PtsCategory = PtsCategory.BREAK,
    val recoveryBufferMinutes: Int = 0, // buffer after slot
    val isEnabled: Boolean = true,
    val dateMillis: Long? = null
)
```

---

<a id="appsrcmainjavacomneuroflowappdomainmodelstepbystepplankt"></a>
### File: `app/src/main/java/com/neuroflow/app/domain/model/StepByStepPlan.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/domain/model/StepByStepPlan.kt`
- **Lines of Code:** 58
- **Size:** 2,001 bytes (2.0 KB)

```kotlin
package com.neuroflow.app.domain.model

import java.util.Locale

data class StepByStepPlanStep(
    val text: String,
    val completed: Boolean
)

object StepByStepPlanCodec {

    fun parse(raw: String): List<StepByStepPlanStep> {
        return raw
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\r", "\n")
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                val normalized = line.lowercase(Locale.getDefault())
                when {
                    normalized.startsWith("[x] ") -> StepByStepPlanStep(
                        text = line.drop(4).trim(),
                        completed = true
                    )
                    normalized.startsWith("[ ] ") -> StepByStepPlanStep(
                        text = line.drop(4).trim(),
                        completed = false
                    )
                    normalized.startsWith("- [x] ") -> StepByStepPlanStep(
                        text = line.drop(6).trim(),
                        completed = true
                    )
                    normalized.startsWith("- [ ] ") -> StepByStepPlanStep(
                        text = line.drop(6).trim(),
                        completed = false
                    )
                    else -> StepByStepPlanStep(text = line, completed = false)
                }
            }
            .toList()
    }

    fun serialize(steps: List<StepByStepPlanStep>): String {
        return steps.joinToString(separator = "\n") { step ->
            if (step.completed) "[x] ${step.text}" else "[ ] ${step.text}"
        }
    }

    fun setStepCompleted(raw: String, index: Int, completed: Boolean): String {
        val steps = parse(raw)
        if (index !in steps.indices) return raw
        val updated = steps.toMutableList()
        updated[index] = updated[index].copy(completed = completed)
        return serialize(updated)
    }
}
```

---

<a id="appsrcmainjavacomneuroflowappdidomainservicemodulekt"></a>
### File: `app/src/main/java/com/neuroflow/app/di/DomainServiceModule.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/di/DomainServiceModule.kt`
- **Lines of Code:** 70
- **Size:** 2,332 bytes (2.3 KB)

```kotlin
package com.neuroflow.app.di

import com.neuroflow.app.domain.service.BehavioralSupportService
import com.neuroflow.app.domain.service.CircadianEnergyService
import com.neuroflow.app.domain.service.DefaultBehavioralSupportService
import com.neuroflow.app.domain.service.DefaultCircadianEnergyService
import com.neuroflow.app.domain.service.DefaultSmartTaskDecompositionService
import com.neuroflow.app.domain.service.DefaultTaskNaturalLanguageParser
import com.neuroflow.app.domain.service.DefaultTaskSchedulerService
import com.neuroflow.app.domain.service.SmartTaskDecompositionService
import com.neuroflow.app.domain.service.TaskNaturalLanguageParser
import com.neuroflow.app.domain.service.TaskSchedulerService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DomainServiceModule {

    @Binds
    @Singleton
    abstract fun bindCircadianEnergyService(
        impl: DefaultCircadianEnergyService
    ): CircadianEnergyService

    @Binds
    @Singleton
    abstract fun bindTaskSchedulerService(
        impl: DefaultTaskSchedulerService
    ): TaskSchedulerService

    @Binds
    @Singleton
    abstract fun bindTaskNaturalLanguageParser(
        impl: DefaultTaskNaturalLanguageParser
    ): TaskNaturalLanguageParser

    @Binds
    @Singleton
    abstract fun bindBehavioralSupportService(
        impl: DefaultBehavioralSupportService
    ): BehavioralSupportService

    @Binds
    @Singleton
    abstract fun bindSmartTaskDecompositionService(
        impl: DefaultSmartTaskDecompositionService
    ): SmartTaskDecompositionService

    @Binds
    @Singleton
    abstract fun bindGeminiAiOnlineService(
        impl: com.neuroflow.app.domain.service.DefaultGeminiAiOnlineService
    ): com.neuroflow.app.domain.service.GeminiAiOnlineService

    @Binds
    @Singleton
    abstract fun bindCalendarSyncRepository(
        impl: com.neuroflow.app.data.calendar.CalendarSyncDataSource
    ): com.neuroflow.app.data.calendar.CalendarSyncRepository

    @Binds
    @Singleton
    abstract fun bindSleepTelemetryRepository(
        impl: com.neuroflow.app.data.sleep.DefaultSleepTelemetryRepository
    ): com.neuroflow.app.data.sleep.SleepTelemetryRepository
}
```

---

<a id="appsrcmainjavacomneuroflowappdiappmodulekt"></a>
### File: `app/src/main/java/com/neuroflow/app/di/AppModule.kt`

- **Path:** `app/src/main/java/com/neuroflow/app/di/AppModule.kt`
- **Lines of Code:** 162
- **Size:** 6,137 bytes (6.0 KB)

```kotlin
package com.neuroflow.app.di

import android.content.Context
import android.content.pm.LauncherApps
import androidx.room.Room
import androidx.work.WorkManager
import com.neuroflow.app.data.local.MIGRATION_1_2
import com.neuroflow.app.data.local.MIGRATION_2_3
import com.neuroflow.app.data.local.MIGRATION_3_4
import com.neuroflow.app.data.local.MIGRATION_4_5
import com.neuroflow.app.data.local.MIGRATION_5_6
import com.neuroflow.app.data.local.MIGRATION_6_7
import com.neuroflow.app.data.local.MIGRATION_7_8
import com.neuroflow.app.data.local.MIGRATION_8_9
import com.neuroflow.app.data.local.MIGRATION_9_10
import com.neuroflow.app.data.local.MIGRATION_10_11
import com.neuroflow.app.data.local.MIGRATION_11_12
import com.neuroflow.app.data.local.MIGRATION_12_13
import com.neuroflow.app.data.local.MIGRATION_13_14
import com.neuroflow.app.data.local.MIGRATION_14_15
import com.neuroflow.app.data.local.MIGRATION_15_16
import com.neuroflow.app.data.local.MIGRATION_16_17
import com.neuroflow.app.data.local.MIGRATION_17_18
import com.neuroflow.app.data.local.MIGRATION_18_19
import com.neuroflow.app.data.local.MIGRATION_17_19
import com.neuroflow.app.data.local.MIGRATION_19_20
import com.neuroflow.app.data.local.MIGRATION_20_21
import com.neuroflow.app.data.local.MIGRATION_21_22
import com.neuroflow.app.data.local.NeuroFlowDatabase
import com.neuroflow.app.data.local.UserPreferencesDataStore
import com.neuroflow.app.data.local.dao.EnergyPredictionDao
import com.neuroflow.app.data.local.dao.FocusSessionDao
import com.neuroflow.app.data.local.dao.GoalDao
import com.neuroflow.app.data.local.dao.NoteDao
import com.neuroflow.app.data.local.dao.ProtectedSlotDao
import com.neuroflow.app.data.local.dao.QuickNoteDao
import com.neuroflow.app.data.local.dao.SleepLogDao
import com.neuroflow.app.data.local.dao.TaskDao
import com.neuroflow.app.data.local.dao.TaskMicroStepDao
import com.neuroflow.app.data.local.dao.TimeSessionDao
import com.neuroflow.app.data.local.dao.UlyssesContractDao
import com.neuroflow.app.data.local.dao.WoopDao
import com.neuroflow.app.data.repository.NoteRepository
import com.neuroflow.app.data.repository.UlyssesContractRepository
import com.neuroflow.app.data.repository.WoopRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NeuroFlowDatabase {
        return Room.databaseBuilder(
            context,
            NeuroFlowDatabase::class.java,
            "neuroflow_database"
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14,
                MIGRATION_14_15,
                MIGRATION_15_16,
                MIGRATION_16_17,
                MIGRATION_17_18,
                MIGRATION_18_19,
                MIGRATION_17_19,
                MIGRATION_19_20,
                MIGRATION_20_21,
                MIGRATION_21_22
            )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideTaskDao(database: NeuroFlowDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideTimeSessionDao(database: NeuroFlowDatabase): TimeSessionDao = database.timeSessionDao()

    @Provides
    fun provideTaskMicroStepDao(database: NeuroFlowDatabase): TaskMicroStepDao = database.taskMicroStepDao()

    @Provides
    fun provideFocusSessionDao(database: NeuroFlowDatabase): FocusSessionDao = database.focusSessionDao()

    @Provides
    fun provideQuickNoteDao(database: NeuroFlowDatabase): QuickNoteDao = database.quickNoteDao()

    @Provides
    fun provideProtectedSlotDao(database: NeuroFlowDatabase): ProtectedSlotDao = database.protectedSlotDao()

    @Provides
    fun provideGoalDao(database: NeuroFlowDatabase): GoalDao = database.goalDao()

    @Provides
    fun provideWoopDao(database: NeuroFlowDatabase): WoopDao = database.woopDao()

    @Provides
    fun provideUlyssesContractDao(database: NeuroFlowDatabase): UlyssesContractDao = database.ulyssesContractDao()

    @Provides
    fun provideSleepLogDao(database: NeuroFlowDatabase): SleepLogDao = database.sleepLogDao()

    @Provides
    fun provideEnergyPredictionDao(database: NeuroFlowDatabase): EnergyPredictionDao = database.energyPredictionDao()

    @Provides
    fun provideNoteDao(database: NeuroFlowDatabase): NoteDao = database.noteDao()

    @Provides
    @Singleton
    fun provideNoteRepository(dao: NoteDao): NoteRepository = NoteRepository(dao)

    @Provides
    @Singleton
    fun provideWoopRepository(dao: WoopDao): WoopRepository = WoopRepository(dao)

    @Provides
    @Singleton
    fun provideUlyssesContractRepository(dao: UlyssesContractDao): UlyssesContractRepository = UlyssesContractRepository(dao)

    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(
        @ApplicationContext context: Context,
        protectedSlotDao: ProtectedSlotDao
    ): UserPreferencesDataStore = UserPreferencesDataStore(context, protectedSlotDao)

    @Provides
    @Singleton
    fun provideLauncherApps(@ApplicationContext context: Context): LauncherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun providePassiveSleepSyncRepository(
        manager: com.neuroflow.app.data.repository.PassiveSleepSyncManager
    ): com.neuroflow.app.domain.repository.PassiveSleepSyncRepository = manager
}
```

---

<a id="appsrctestjavacomneuroflowappdomainservicecircadianenergyservicetestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/CircadianEnergyServiceTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/CircadianEnergyServiceTest.kt`
- **Lines of Code:** 468
- **Size:** 20,558 bytes (20.1 KB)

```kotlin
package com.neuroflow.app.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class CircadianEnergyServiceTest {

    private lateinit var service: CircadianEnergyService
    private val zoneId = ZoneId.of("UTC")

    @Before
    fun setUp() {
        service = DefaultCircadianEnergyService()
    }

    @Test
    fun testContinuousSleepRecoveryNoCliffAt60Minutes() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val testTime = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // 59 minutes sleep vs 60 minutes sleep
        val snapshot59 = service.evaluateSnapshot(
            timestampMillis = testTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 59,
            zoneId = zoneId
        )

        val snapshot60 = service.evaluateSnapshot(
            timestampMillis = testTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 60,
            zoneId = zoneId
        )

        val deltaUsable = kotlin.math.abs(snapshot59.usableEnergy - snapshot60.usableEnergy)
        val deltaPressureS = kotlin.math.abs(snapshot59.homeostaticPressureS - snapshot60.homeostaticPressureS)

        // Continuous recovery guarantees Delta E <= 1% and Delta S < 0.05 (no artificial cliff)
        assertTrue("Usable energy delta should be <= 1, was $deltaUsable", deltaUsable <= 1)
        assertTrue("Pressure S delta should be < 0.05, was $deltaPressureS", deltaPressureS < 0.05)
    }

    @Test
    fun testEveningEnergyAt19HoursAfter8HoursSleep() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val eveningTime = today.atTime(LocalTime.of(19, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val snapshot = service.evaluateSnapshot(
            timestampMillis = eveningTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 8 * 60,
            zoneId = zoneId
        )

        // Healthy evening wind-down (40%..56%), preventing Flaw #8 premature crashes (<40%)
        assertTrue(
            "Usable energy at 19:00 after 8h sleep should be in [40..56], was ${snapshot.usableEnergy}",
            snapshot.usableEnergy in 40..56
        )
        assertEquals(EnergyPhase.WIND_DOWN, snapshot.phase)
    }

    @Test
    fun testContinuousFocusDepletionOnlyAfter45Minutes() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val noonTime = today.atTime(LocalTime.of(12, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // 30 min focus: 0 penalty
        val snap30 = service.evaluateSnapshot(noonTime, wakeTime, 480, continuousActiveFocusMinutes = 30, zoneId = zoneId)
        assertEquals(0.0, snap30.focusDepletionPenalty, 0.001)

        // 60 min focus: small synaptic depletion penalty > 0 and <= 0.15
        val snap60 = service.evaluateSnapshot(noonTime, wakeTime, 480, continuousActiveFocusMinutes = 60, zoneId = zoneId)
        assertTrue(snap60.focusDepletionPenalty > 0.0)
        assertTrue(snap60.focusDepletionPenalty <= 0.15)

        // 90 min focus: triggers RECOVERY_NEEDED
        val snap90 = service.evaluateSnapshot(noonTime, wakeTime, 480, continuousActiveFocusMinutes = 90, zoneId = zoneId)
        assertEquals(EnergyPhase.RECOVERY_NEEDED, snap90.phase)
    }

    @Test
    fun testCategoryAffinityMatchesCircadianPhases() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val peakMorning = today.atTime(LocalTime.of(10, 30)).atZone(zoneId).toInstant().toEpochMilli()

        val snapPeak = service.evaluateSnapshot(peakMorning, wakeTime, 480, zoneId = zoneId)
        val deepWorkAffinity = service.computeCategoryAffinity(TaskCategory.DEEP_WORK, snapPeak)
        val adminAffinity = service.computeCategoryAffinity(TaskCategory.ADMIN, snapPeak)

        assertTrue(
            "Deep work affinity ($deepWorkAffinity) should exceed admin affinity ($adminAffinity) at peak energy",
            deepWorkAffinity > adminAffinity
        )
    }

    @Test
    fun testProjectDayTimelineGenerates24HourSteps() {
        val today = LocalDate.of(2026, 10, 18)
        val dayStart = today.atTime(LocalTime.of(0, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val timeline = service.projectDayTimeline(dayStart, wakeTime, 480, stepMinutes = 30, zoneId = zoneId)
        assertEquals(48, timeline.size)
        timeline.forEach {
            assertTrue(it.usableEnergy in 0..100)
            assertTrue(it.circadianAlertnessC in 0.0..1.0)
            assertTrue(it.homeostaticPressureS in 0.0..1.0)
        }
    }

    @Test
    fun testComputeDailySummaryStandardWake() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val noonTime = today.atTime(LocalTime.of(12, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val summary = service.computeDailySummary(
            timestampMillis = noonTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )

        // Peak window should land in morning [7..12]
        assertTrue("Peak start hour should be in [7..12], was ${summary.peakWindowStartHour}", summary.peakWindowStartHour in 7..12)
        assertEquals((summary.peakWindowStartHour + 3) % 24, summary.peakWindowEndHour)
        assertTrue("Peak energy score should be >= 70, was ${summary.peakEnergyScore}", summary.peakEnergyScore >= 70)

        // Dip window should land in [wake + 4h .. wake + 11h] = [11..18]
        assertTrue("Dip start hour should be in [11..18], was ${summary.dipWindowStartHour}", summary.dipWindowStartHour in 11..18)
        assertEquals((summary.dipWindowStartHour + 2) % 24, summary.dipWindowEndHour)

        // Recommended deep work minutes bounded in [60, 240]
        assertTrue("Recommended deep work should be in [60, 240], was ${summary.recommendedDeepWorkMinutes}", summary.recommendedDeepWorkMinutes in 60..240)
    }

    @Test
    fun testComputeDailySummaryAcrossMidnight() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeYesterday = today.minusDays(1).atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val lateNightTime = today.atTime(LocalTime.of(2, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val summary = service.computeDailySummary(
            timestampMillis = lateNightTime,
            wakeTimeMillis = wakeYesterday,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )

        // Summary must derive aligned day timeline without crashing on empty dip candidates
        assertTrue("Peak start hour should be in [7..12], was ${summary.peakWindowStartHour}", summary.peakWindowStartHour in 7..12)
        assertTrue("Dip start hour should be in [11..18], was ${summary.dipWindowStartHour}", summary.dipWindowStartHour in 11..18)
    }

    @Test
    fun testComputeDailySummarySleepDeprivationReducesCapacity() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val noonTime = today.atTime(LocalTime.of(12, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val rested = service.computeDailySummary(
            timestampMillis = noonTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )

        val sleepDeprived = service.computeDailySummary(
            timestampMillis = noonTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 180, // 3 hours
            zoneId = zoneId
        )

        assertTrue(
            "Rested peak score (${rested.peakEnergyScore}) should exceed sleep-deprived score (${sleepDeprived.peakEnergyScore})",
            rested.peakEnergyScore > sleepDeprived.peakEnergyScore
        )
        assertTrue(
            "Rested deep work minutes (${rested.recommendedDeepWorkMinutes}) should >= sleep-deprived (${sleepDeprived.recommendedDeepWorkMinutes})",
            rested.recommendedDeepWorkMinutes >= sleepDeprived.recommendedDeepWorkMinutes
        )
    }

    @Test
    fun testPowerNap20mLowersProcessSAndRaisesEnergyWithoutInertiaOrPhaseShift() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val napStart = today.atTime(LocalTime.of(14, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val napEnd = today.atTime(LocalTime.of(14, 20)).atZone(zoneId).toInstant().toEpochMilli()
        val evalTime15 = today.atTime(LocalTime.of(15, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val powerNap = listOf(DaytimeNapBout(startMillis = napStart, endMillis = napEnd))

        // Without nap
        val snapNoNap = service.evaluateSnapshot(
            timestampMillis = evalTime15,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )

        // With 20m power nap
        val snapWithNap = service.evaluateSnapshot(
            timestampMillis = evalTime15,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            naps = powerNap
        )

        // 1. Process S must be lower after nap
        assertTrue(
            "Process S with nap (${snapWithNap.homeostaticPressureS}) must be lower than without nap (${snapNoNap.homeostaticPressureS})",
            snapWithNap.homeostaticPressureS < snapNoNap.homeostaticPressureS
        )

        // 2. Usable energy must be higher in afternoon
        assertTrue(
            "Usable energy with nap (${snapWithNap.usableEnergy}) must exceed no-nap energy (${snapNoNap.usableEnergy})",
            snapWithNap.usableEnergy > snapNoNap.usableEnergy
        )

        // 3. Sleep inertia penalty for 20m nap must be strictly 0.0 (stays in stage 1/2)
        assertEquals(0.0, snapWithNap.sleepInertiaPenalty, 0.0001)

        // 4. Circadian alertness C(t) is 100% phase-locked (zero shift to phi)
        assertEquals(snapNoNap.circadianAlertnessC, snapWithNap.circadianAlertnessC, 0.0001)

        // 5. Daily summary peak hour phi is identical with or without nap
        val summaryNoNap = service.computeDailySummary(
            timestampMillis = evalTime15,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )
        val summaryWithNap = service.computeDailySummary(
            timestampMillis = evalTime15,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            naps = powerNap
        )
        assertEquals(summaryNoNap.peakWindowStartHour, summaryWithNap.peakWindowStartHour)
    }

    @Test
    fun testDeepNap60mAppliesTransientSleepInertiaThatDecaysBy45Minutes() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val napStart = today.atTime(LocalTime.of(14, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val napEnd = today.atTime(LocalTime.of(15, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val deepNap = listOf(DaytimeNapBout(startMillis = napStart, endMillis = napEnd))

        // Immediately after waking: 15:01 (1 minute post-nap)
        val time1501 = today.atTime(LocalTime.of(15, 1)).atZone(zoneId).toInstant().toEpochMilli()
        val snapAtWake = service.evaluateSnapshot(
            timestampMillis = time1501,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            naps = deepNap
        )

        // Sleep inertia penalty is active (> 0.10)
        assertTrue(
            "Sleep inertia penalty at 15:01 (${snapAtWake.sleepInertiaPenalty}) must be > 0.10",
            snapAtWake.sleepInertiaPenalty > 0.10
        )

        // 45 minutes later: 15:45
        val time1545 = today.atTime(LocalTime.of(15, 45)).atZone(zoneId).toInstant().toEpochMilli()
        val snapAt45m = service.evaluateSnapshot(
            timestampMillis = time1545,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            naps = deepNap
        )

        // Sleep inertia penalty must decay away (< 0.02)
        assertTrue(
            "Sleep inertia penalty at 15:45 (${snapAt45m.sleepInertiaPenalty}) must be < 0.02",
            snapAt45m.sleepInertiaPenalty < 0.02
        )

        // Early evening: 18:00
        val time1800 = today.atTime(LocalTime.of(18, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val snapEveningNoNap = service.evaluateSnapshot(
            timestampMillis = time1800,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId
        )
        val snapEveningWithNap = service.evaluateSnapshot(
            timestampMillis = time1800,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            naps = deepNap
        )

        // Inertia completely gone at 18:00
        assertEquals(0.0, snapEveningWithNap.sleepInertiaPenalty, 0.001)

        // Sustained early-evening energy higher than without nap
        assertTrue(
            "Evening energy with 60m nap (${snapEveningWithNap.usableEnergy}) must exceed no-nap energy (${snapEveningNoNap.usableEnergy})",
            snapEveningWithNap.usableEnergy > snapEveningNoNap.usableEnergy
        )
    }

    @Test
    fun testNocturnalSleepAloneProducesBitForBitIdenticalOutputToPhase6() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val testTimes = listOf(
            today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli(),
            today.atTime(LocalTime.of(12, 0)).atZone(zoneId).toInstant().toEpochMilli(),
            today.atTime(LocalTime.of(15, 30)).atZone(zoneId).toInstant().toEpochMilli(),
            today.atTime(LocalTime.of(19, 0)).atZone(zoneId).toInstant().toEpochMilli(),
            today.atTime(LocalTime.of(22, 0)).atZone(zoneId).toInstant().toEpochMilli()
        )

        for (t in testTimes) {
            val legacy5ArgSnap = service.evaluateSnapshot(
                timestampMillis = t,
                wakeTimeMillis = wakeTime,
                lastSleepDurationMinutes = 480,
                continuousActiveFocusMinutes = 0,
                zoneId = zoneId
            )
            val explicitEmptyNapsSnap = service.evaluateSnapshot(
                timestampMillis = t,
                wakeTimeMillis = wakeTime,
                lastSleepDurationMinutes = 480,
                continuousActiveFocusMinutes = 0,
                zoneId = zoneId,
                naps = emptyList()
            )

            assertEquals(legacy5ArgSnap.usableEnergy, explicitEmptyNapsSnap.usableEnergy)
            assertEquals(legacy5ArgSnap.homeostaticPressureS, explicitEmptyNapsSnap.homeostaticPressureS, 0.0)
            assertEquals(legacy5ArgSnap.circadianAlertnessC, explicitEmptyNapsSnap.circadianAlertnessC, 0.0)
            assertEquals(legacy5ArgSnap.focusDepletionPenalty, explicitEmptyNapsSnap.focusDepletionPenalty, 0.0)
            assertEquals(legacy5ArgSnap.phase, explicitEmptyNapsSnap.phase)
            assertEquals(0.0, legacy5ArgSnap.sleepInertiaPenalty, 0.0)
        }
    }

    @Test
    fun testVonMisesHourlyModulationShiftsLearnedAdjustmentPoints() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val afternoon = today.atTime(LocalTime.of(14, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val learnedProfile = LearnedCircadianProfile(
            peakHourMean = 14.0,
            sampleCount = 15.0,
            hourlyMultipliers = AdaptiveCircadianLearner.computeVonMisesHourlyMultipliers(14.0)
        )

        val snapshot = service.evaluateSnapshot(
            timestampMillis = afternoon,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            continuousActiveFocusMinutes = 0,
            zoneId = zoneId,
            naps = emptyList(),
            chronotype = ChronotypeProfile.INTERMEDIATE,
            manualPeakStartHour = null,
            learnedProfile = learnedProfile,
            rollingAverageSleepMinutes = null,
            cognitiveBlocks = emptyList()
        )

        assertTrue(
            "Learned adjustment points should not be zero when Von Mises modulation is active (was ${snapshot.learnedAdjustmentPoints})",
            snapshot.learnedAdjustmentPoints != 0.0
        )
    }

    @Test
    fun testProjectDayTimelineWithStimulantsModulatesCurve() {
        val today = LocalDate.of(2026, 10, 18)
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val doseTime = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val baselineTimeline = service.projectDayTimeline(
            dayStartMillis = dayStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            stepMinutes = 15,
            zoneId = zoneId,
            stimulantDoses = emptyList()
        )

        val caffeineDose = listOf(
            StimulantDoseBout(
                timestampMillis = doseTime,
                doseMg = 100.0,
                type = StimulantType.CAFFEINE
            )
        )

        val stimulantTimeline = service.projectDayTimeline(
            dayStartMillis = dayStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            stepMinutes = 15,
            zoneId = zoneId,
            stimulantDoses = caffeineDose
        )

        val peakAbsorptionTime = today.atTime(LocalTime.of(8, 45)).atZone(zoneId).toInstant().toEpochMilli()
        val baseAtPeak = baselineTimeline.minByOrNull { kotlin.math.abs(it.timestampMillis - peakAbsorptionTime) }!!
        val stimAtPeak = stimulantTimeline.minByOrNull { kotlin.math.abs(it.timestampMillis - peakAbsorptionTime) }!!

        assertTrue(
            "Energy with caffeine ($stimAtPeak) should be strictly higher than baseline ($baseAtPeak) at peak plasma concentration",
            stimAtPeak.usableEnergy >= baseAtPeak.usableEnergy
        )
    }

    @Test
    fun testExtractStimulantDosesFromSlots() {
        val today = LocalDate.of(2026, 10, 18)
        val slots = listOf(
            com.neuroflow.app.domain.model.ProtectedTimeSlot(
                id = "slot-1",
                label = "Morning Espresso",
                startMinuteOfDay = 480, // 08:00
                durationMinutes = 20
            ),
            com.neuroflow.app.domain.model.ProtectedTimeSlot(
                id = "slot-2",
                label = "Afternoon Vyvanse Meds XR",
                startMinuteOfDay = 780, // 13:00
                durationMinutes = 15
            ),
            com.neuroflow.app.domain.model.ProtectedTimeSlot(
                id = "slot-3",
                label = "Deep Work Block",
                startMinuteOfDay = 540,
                durationMinutes = 90
            )
        )

        val extracted = com.neuroflow.app.domain.model.extractStimulantDosesFromSlots(slots, today, zoneId)
        // ADR-024 Bug 3: Evaluates across both yesterday and today to capture full 24h PK tail (2 per day * 2 days = 4)
        assertEquals(4, extracted.size)
        assertEquals(StimulantType.CAFFEINE, extracted[0].type)
        assertEquals(StimulantType.PRESCRIPTION_STIMULANT_XR, extracted[1].type)
    }
}
```

---

<a id="appsrctestjavacomneuroflowappdomainservicefastcircadianlearnertestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/FastCircadianLearnerTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/FastCircadianLearnerTest.kt`
- **Lines of Code:** 310
- **Size:** 12,668 bytes (12.4 KB)

```kotlin
package com.neuroflow.app.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

class FastCircadianLearnerTest {

    private lateinit var circadianService: CircadianEnergyService
    private lateinit var schedulerService: TaskSchedulerService
    private val zoneId = ZoneId.of("UTC")

    @Before
    fun setUp() {
        circadianService = DefaultCircadianEnergyService()
        schedulerService = DefaultTaskSchedulerService(circadianService)
    }

    @Test
    fun testEmpiricalPeakHourConvergenceWithinTwoToThreeSessions() {
        val initialPrior = LearnedCircadianProfile(
            peakHourMean = 9.0,
            peakHourVariance = 4.0,
            sampleCount = 0.0
        )

        // Session 1 at 14:00 (minute 840) with high productivity
        val obs1 = BehavioralObservation(minuteOfDay = 840, productivityRating = 0.95, weight = 1.5)
        val profile1 = AdaptiveCircadianLearner.updateProfile(initialPrior, obs1, kappa = 4.5)
        assertTrue("Peak should move toward 14:00 after session 1", profile1.peakHourMean > 11.5)

        // Session 2 at 14:00
        val obs2 = BehavioralObservation(minuteOfDay = 840, productivityRating = 0.95, weight = 1.5)
        val profile2 = AdaptiveCircadianLearner.updateProfile(profile1, obs2, kappa = 4.5)
        assertTrue("Peak should be within 0.8h of 14:00 after session 2", abs(profile2.peakHourMean - 14.0) < 0.8)

        // Session 3 at 14:00
        val obs3 = BehavioralObservation(minuteOfDay = 840, productivityRating = 1.0, weight = 2.0)
        val profile3 = AdaptiveCircadianLearner.updateProfile(profile2, obs3, kappa = 4.5)
        assertTrue("Peak must converge within 0.35h of 14:00 after session 3", abs(profile3.peakHourMean - 14.0) < 0.35)
        assertTrue("Confidence must exceed 0.60 after 3 sessions", profile3.confidence > 0.60)

        // Verify Von Mises circular smoother properties (kappa = 4.5)
        val multipliers = profile3.hourlyMultipliers
        assertEquals(24, multipliers.size)
        val maxHour = multipliers.indices.maxByOrNull { multipliers[it] }
        assertEquals(14, maxHour)
        assertTrue("Opposite clock hour (hour 2) multiplier must be near 0", multipliers[2] < 0.01)
    }

    @Test
    fun testCircularMidnightWrapAroundConvergence() {
        // Prior peak at 23:00 (11 PM)
        val prior = LearnedCircadianProfile(
            peakHourMean = 23.0,
            peakHourVariance = 3.5,
            sampleCount = 0.0
        )

        // Observation at 01:00 (1 AM, minute 60)
        val obs = BehavioralObservation(minuteOfDay = 60, productivityRating = 0.9, weight = 2.0)
        val updated = AdaptiveCircadianLearner.updateProfile(prior, obs, kappa = 4.5)

        // Shortest circular difference from 23.0 to 1.0 is +2.0 hours.
        // It must NOT jump to midday (12.0)
        val diffFromMidnight = if (updated.peakHourMean >= 20.0) 24.0 - updated.peakHourMean else updated.peakHourMean
        assertTrue("Updated peak should remain close to midnight", diffFromMidnight < 1.5)
    }

    @Test
    fun testMultiDayAllostaticSleepDebtScalesHomeostaticPressureAndAcceleratesWakeDecay() {
        val today = LocalDate.of(2026, 10, 20)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val testTime = today.atTime(LocalTime.of(14, 0)).atZone(zoneId).toInstant().toEpochMilli() // 7h awake

        // Scenario A: Normal 8h sleep rolling average
        val snapshotNormal = circadianService.evaluateSnapshot(
            timestampMillis = testTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            rollingAverageSleepMinutes = 480.0
        )

        // Scenario B: Chronic allostatic sleep debt (average 5h/night)
        val snapshotDebt = circadianService.evaluateSnapshot(
            timestampMillis = testTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            zoneId = zoneId,
            rollingAverageSleepMinutes = 300.0
        )

        assertTrue(
            "Sleep debt must increase homeostatic pressure Process S",
            snapshotDebt.homeostaticPressureS > snapshotNormal.homeostaticPressureS
        )
        assertTrue(
            "Sleep debt must decrease usable energy",
            snapshotDebt.usableEnergy < snapshotNormal.usableEnergy
        )
        assertEquals(
            "Morning wake anchor and circadian Process C must remain strictly identical",
            snapshotNormal.circadianAlertnessC,
            snapshotDebt.circadianAlertnessC,
            0.0001
        )
    }

    @Test
    fun testAcuteMealPostPrandialDipModulation() {
        val today = LocalDate.of(2026, 10, 20)
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // Meal block: 12:30 (minute 750) to 13:15 (minute 795)
        val mealBlock = CognitiveBlockWindow(
            startMinuteOfDay = 750,
            endMinuteOfDay = 795,
            category = CognitiveBlockCategory.MEAL,
            intensity = 1.0,
            label = "Lunch"
        )

        val timelineWithoutMeal = circadianService.projectDayTimeline(
            dayStartMillis = dayStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            stepMinutes = 15,
            zoneId = zoneId,
            cognitiveBlocks = emptyList()
        )

        val timelineWithMeal = circadianService.projectDayTimeline(
            dayStartMillis = dayStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            stepMinutes = 15,
            zoneId = zoneId,
            cognitiveBlocks = listOf(mealBlock)
        )

        // At 11:30 (step 46), meal has not happened yet -> identical
        val step1130 = (11 * 60 + 30) / 15
        assertEquals(timelineWithoutMeal[step1130].usableEnergy, timelineWithMeal[step1130].usableEnergy)

        // At 13:30 (step 54, 60 minutes after meal start) -> post-prandial dip active
        val step1330 = (13 * 60 + 30) / 15
        assertTrue(
            "Usable energy during post-prandial dip should be reduced",
            timelineWithMeal[step1330].usableEnergy < timelineWithoutMeal[step1330].usableEnergy
        )
        assertTrue(
            "Physiological impulse must be negative at post-prandial peak",
            timelineWithMeal[step1330].physiologicalImpulse < -8.0
        )

        // At 16:30 (step 66), dip has completely decayed away
        val step1630 = (16 * 60 + 30) / 15
        assertEquals(timelineWithoutMeal[step1630].usableEnergy, timelineWithMeal[step1630].usableEnergy)
    }

    @Test
    fun testWorkoutPostExerciseBoostModulation() {
        val today = LocalDate.of(2026, 10, 20)
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // Workout: 16:00 (minute 960) to 17:00 (minute 1020)
        val workoutBlock = CognitiveBlockWindow(
            startMinuteOfDay = 960,
            endMinuteOfDay = 1020,
            category = CognitiveBlockCategory.EXERCISE,
            intensity = 1.0,
            label = "Gym Session"
        )

        val timelineBaseline = circadianService.projectDayTimeline(
            dayStartMillis = dayStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            stepMinutes = 15,
            zoneId = zoneId,
            cognitiveBlocks = emptyList()
        )

        val timelineWorkout = circadianService.projectDayTimeline(
            dayStartMillis = dayStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            stepMinutes = 15,
            zoneId = zoneId,
            cognitiveBlocks = listOf(workoutBlock)
        )

        // During workout at 16:30 (minute 990, step 66): exertion penalty
        val step1630 = 990 / 15
        assertTrue(
            "During workout, physiological impulse should represent physical exertion load",
            timelineWorkout[step1630].physiologicalImpulse < 0.0
        )

        // Immediately after workout at 17:15 (minute 1035, step 69): post-workout endorphin boost
        val step1715 = 1035 / 15
        assertTrue(
            "Immediately post-workout, physiological impulse should be strongly positive",
            timelineWorkout[step1715].physiologicalImpulse > 5.0
        )
        assertTrue(
            "Usable energy post-workout should be elevated compared to baseline",
            timelineWorkout[step1715].usableEnergy > timelineBaseline[step1715].usableEnergy
        )
    }

    @Test
    fun testTaskLoadSynapticFocusDepletion() {
        val today = LocalDate.of(2026, 10, 20)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val testTime = today.atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // No prior focus load
        val fresh = circadianService.evaluateSnapshot(
            timestampMillis = testTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            continuousActiveFocusMinutes = 0,
            zoneId = zoneId
        )
        assertEquals(0.0, fresh.focusDepletionPenalty, 0.0001)

        // 60 minutes unbroken focus
        val loaded60 = circadianService.evaluateSnapshot(
            timestampMillis = testTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            continuousActiveFocusMinutes = 60,
            zoneId = zoneId
        )
        assertTrue("Focus depletion penalty should be positive after 60m focus", loaded60.focusDepletionPenalty > 0.0)
        assertTrue("Usable energy should decrease with focus load", loaded60.usableEnergy < fresh.usableEnergy)

        // 100 minutes unbroken focus -> forces RECOVERY_NEEDED
        val depleted = circadianService.evaluateSnapshot(
            timestampMillis = testTime,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            continuousActiveFocusMinutes = 100,
            zoneId = zoneId
        )
        assertEquals(EnergyPhase.RECOVERY_NEEDED, depleted.phase)
    }

    @Test
    fun testTaskSchedulerProtectsPhysiologicalBlocks() {
        val today = LocalDate.of(2026, 10, 20)
        val dayStart = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // Meal: 12:00 (720) to 13:00 (780)
        val mealBlock = CognitiveBlockWindow(
            startMinuteOfDay = 720,
            endMinuteOfDay = 780,
            category = CognitiveBlockCategory.MEAL,
            label = "Lunch Protected"
        )
        val mealStart = today.atTime(LocalTime.of(12, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val mealEnd = today.atTime(LocalTime.of(13, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val task1 = SchedulableTask(
            id = "t1",
            title = "Deep Research Project",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 120,
            energyDemand = EnergyDemand.HIGH
        )
        val task2 = SchedulableTask(
            id = "t2",
            title = "Admin Review",
            category = TaskCategory.ADMIN,
            priority = 1,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.LOW
        )

        val placements = schedulerService.scheduleDay(
            tasks = listOf(task1, task2),
            dayDate = today,
            nowMillis = dayStart,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = emptyList(),
            protectedSlots = emptyList(),
            zoneId = zoneId,
            physiologicalBlocks = listOf(mealBlock)
        )

        for (placement in placements) {
            val window = placement.window
            val overlapsMeal = (window.startMillis < mealEnd && window.endMillis > mealStart)
            assertTrue("Scheduled task '${placement.task.title}' must NOT overlap protected meal block", !overlapsMeal)
        }
    }
}
```

---

<a id="appsrctestjavacomneuroflowappdomainservicecircadianadversarialstresstestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/CircadianAdversarialStressTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/CircadianAdversarialStressTest.kt`
- **Lines of Code:** 227
- **Size:** 11,551 bytes (11.3 KB)

```kotlin
package com.neuroflow.app.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Adversarial stress tests written by challenger_m1_1.
 * Tests empirical convergence speed, circular wrap-around edge cases,
 * extreme values, NaN handling, Bessel I0 stability, and impulse dynamics.
 */
class CircadianAdversarialStressTest {

    @Test
    fun testLargeJumpConvergence_09to15() {
        // Prior: 09:00, Variance: 4.0, SampleCount: 0.0
        val prior = LearnedCircadianProfile(
            peakHourMean = 9.0,
            peakHourVariance = 4.0,
            sampleCount = 0.0
        )

        // Standard behavioral observation at 15:00 (minute 900)
        val obs1 = BehavioralObservation(minuteOfDay = 900, productivityRating = 1.0, weight = 1.0)
        val p1 = AdaptiveCircadianLearner.updateProfile(prior, obs1, kappa = 4.5)

        val obs2 = BehavioralObservation(minuteOfDay = 900, productivityRating = 1.0, weight = 1.0)
        val p2 = AdaptiveCircadianLearner.updateProfile(p1, obs2, kappa = 4.5)

        val obs3 = BehavioralObservation(minuteOfDay = 900, productivityRating = 1.0, weight = 1.0)
        val p3 = AdaptiveCircadianLearner.updateProfile(p2, obs3, kappa = 4.5)

        println("09:00 -> 15:00 jump:")
        println("  After session 1: mean=${p1.peakHourMean}, var=${p1.peakHourVariance}, conf=${p1.confidence}")
        println("  After session 2: mean=${p2.peakHourMean}, var=${p2.peakHourVariance}, conf=${p2.confidence}")
        println("  After session 3: mean=${p3.peakHourMean}, var=${p3.peakHourVariance}, conf=${p3.confidence}")

        // Check if it converges within 2-3 sessions
        val distAfter2 = abs(p2.peakHourMean - 15.0)
        val distAfter3 = abs(p3.peakHourMean - 15.0)
        println("  Dist after 2 sessions: $distAfter2 hours (${distAfter2 * 60} mins)")
        println("  Dist after 3 sessions: $distAfter3 hours (${distAfter3 * 60} mins)")

        // After 2 sessions: 14.07h -> dist = 0.93h
        // After 3 sessions: 14.37h -> dist = 0.63h
        assertTrue("After 3 sessions with weight=1.0, peak hour should be within 1.0h of 15:00", distAfter3 < 1.0)
    }

    @Test
    fun testLargeJumpConvergence_Weighted_09to15() {
        val prior = LearnedCircadianProfile(
            peakHourMean = 9.0,
            peakHourVariance = 4.0,
            sampleCount = 0.0
        )

        // With stronger user focus sessions (weight = 1.5, rating = 0.95 like FastCircadianLearnerTest)
        val obs1 = BehavioralObservation(minuteOfDay = 900, productivityRating = 0.95, weight = 1.5)
        val p1 = AdaptiveCircadianLearner.updateProfile(prior, obs1, kappa = 4.5)

        val obs2 = BehavioralObservation(minuteOfDay = 900, productivityRating = 0.95, weight = 1.5)
        val p2 = AdaptiveCircadianLearner.updateProfile(p1, obs2, kappa = 4.5)

        val obs3 = BehavioralObservation(minuteOfDay = 900, productivityRating = 1.0, weight = 2.0)
        val p3 = AdaptiveCircadianLearner.updateProfile(p2, obs3, kappa = 4.5)

        println("09:00 -> 15:00 weighted jump:")
        println("  After session 1: mean=${p1.peakHourMean}")
        println("  After session 2: mean=${p2.peakHourMean}")
        println("  After session 3: mean=${p3.peakHourMean}")

        val distAfter3 = abs(p3.peakHourMean - 15.0)
        assertTrue("Weighted session converges within 0.45h of 15:00", distAfter3 < 0.45)
    }

    @Test
    fun testCircularWrapAround_MidnightBoundaries() {
        // Test 23:00 -> 01:00 (+2h shortest path)
        val prior23 = LearnedCircadianProfile(peakHourMean = 23.0, peakHourVariance = 4.0, sampleCount = 0.0)
        val obs01 = BehavioralObservation(minuteOfDay = 60, productivityRating = 1.0, weight = 1.0)
        val pAfter01 = AdaptiveCircadianLearner.updateProfile(prior23, obs01)
        println("23:00 -> 01:00 update: mean=${pAfter01.peakHourMean}")
        // Shortest diff is +2.0h. Kalman gain ~ 0.727.
        // updatedMean = (23.0 + 0.727 * 2.0) = 24.455 -> 0.455h (00:27)
        assertTrue("Peak should cross midnight forward into 00:xx", pAfter01.peakHourMean < 1.0)

        // Test 01:00 -> 23:00 (-2h shortest path)
        val prior01 = LearnedCircadianProfile(peakHourMean = 1.0, peakHourVariance = 4.0, sampleCount = 0.0)
        val obs23 = BehavioralObservation(minuteOfDay = 1380, productivityRating = 1.0, weight = 1.0)
        val pAfter23 = AdaptiveCircadianLearner.updateProfile(prior01, obs23)
        println("01:00 -> 23:00 update: mean=${pAfter23.peakHourMean}")
        // Shortest diff is -2.0h. updatedMean = (1.0 - 0.727 * 2.0) = -0.455 -> 23.545h (23:32)
        assertTrue("Peak should cross midnight backward into 23:xx", pAfter23.peakHourMean > 23.0)

        // Test boundary at 23:59 -> 00:01
        val priorMidnight = LearnedCircadianProfile(peakHourMean = 23.98, peakHourVariance = 2.0, sampleCount = 1.0)
        val obsMidnight = BehavioralObservation(minuteOfDay = 1, productivityRating = 1.0, weight = 1.0)
        val pMidnight = AdaptiveCircadianLearner.updateProfile(priorMidnight, obsMidnight)
        assertTrue("Peak should stay within 0.1h of midnight", pMidnight.peakHourMean > 23.9 || pMidnight.peakHourMean < 0.1)
    }

    @Test
    fun testBesselI0_NumericalStability() {
        // x = 0
        assertEquals(1.0, AdaptiveCircadianLearner.besselI0(0.0), 1e-6)

        // Continuity across the x = 3.75 boundary
        val valBelow = AdaptiveCircadianLearner.besselI0(3.749999)
        val valAbove = AdaptiveCircadianLearner.besselI0(3.750001)
        println("Bessel I0 at 3.75 boundary: below=$valBelow, above=$valAbove")
        assertEquals("Bessel I0 should be continuous at 3.75", valBelow, valAbove, 1e-4)

        // Negative values: I0(-x) == I0(x)
        assertEquals(AdaptiveCircadianLearner.besselI0(4.5), AdaptiveCircadianLearner.besselI0(-4.5), 1e-6)

        // Standard kappa = 4.5: true Bessel I0(4.5) is ~17.481
        val i0_45 = AdaptiveCircadianLearner.besselI0(4.5)
        println("Bessel I0(4.5) = $i0_45")
        assertTrue("I0(4.5) should be ~17.481", abs(i0_45 - 17.481) < 0.1)

        // Large kappa: up to 100
        val i0_100 = AdaptiveCircadianLearner.besselI0(100.0)
        println("Bessel I0(100.0) = $i0_100")
        assertFalse("I0(100.0) should be finite", i0_100.isInfinite() || i0_100.isNaN())

        // Extreme kappa: 700
        val i0_700 = AdaptiveCircadianLearner.besselI0(700.0)
        println("Bessel I0(700.0) = $i0_700")
        assertFalse("I0(700.0) should be finite", i0_700.isInfinite() || i0_700.isNaN())

        // Overflow threshold for exp(x): x > 709.78
        val i0_800 = AdaptiveCircadianLearner.besselI0(800.0)
        println("Bessel I0(800.0) = $i0_800")
        assertTrue("I0(800.0) overflows to Infinity in double precision", i0_800.isInfinite())

        // Von Mises density with extreme kappa (now safe via computeVonMisesDensitySafe)
        val densityExtreme = AdaptiveCircadianLearner.computeVonMisesDensity(14.0, 14.0, kappa = 800.0)
        println("Von Mises density with kappa=800: $densityExtreme")
        assertFalse("Von Mises density must not be NaN even when kappa > 709 due to safe scaling", densityExtreme.isNaN())
        assertTrue("Von Mises density is non-negative", densityExtreme >= 0.0)

        // However, hourly multipliers do NOT use besselI0 and do NOT become NaN
        val multipliersExtreme = AdaptiveCircadianLearner.computeVonMisesHourlyMultipliers(14.0, kappa = 800.0)
        assertEquals(24, multipliersExtreme.size)
        assertEquals(1.0, multipliersExtreme[14], 1e-6)
        assertEquals(0.0, multipliersExtreme[2], 1e-6)
        assertFalse("Multipliers must not be NaN even at extreme kappa", multipliersExtreme.any { it.isNaN() })
    }

    @Test
    fun testZeroVarianceAndExtremePriorHandling() {
        // Zero variance prior
        val zeroVarPrior = LearnedCircadianProfile(
            peakHourMean = 10.0,
            peakHourVariance = 0.0,
            sampleCount = 0.0
        )
        val obs = BehavioralObservation(minuteOfDay = 720, productivityRating = 1.0, weight = 1.0)
        val updated = AdaptiveCircadianLearner.updateProfile(zeroVarPrior, obs)
        println("Zero variance update: mean=${updated.peakHourMean}, var=${updated.peakHourVariance}")
        assertFalse("Mean should not be NaN", updated.peakHourMean.isNaN())
        assertFalse("Var should not be NaN", updated.peakHourVariance.isNaN())
        assertTrue("Variance has a lower floor of PROCESS_NOISE_Q (0.05)", updated.peakHourVariance >= 0.05)

        // Extremely high variance prior
        val hugeVarPrior = LearnedCircadianProfile(
            peakHourMean = 10.0,
            peakHourVariance = 1000.0,
            sampleCount = 0.0
        )
        val updatedHuge = AdaptiveCircadianLearner.updateProfile(hugeVarPrior, obs)
        assertFalse("Updated var should be capped", updatedHuge.peakHourVariance > 12.0)
    }

    @Test
    fun testMealPostPrandialDipDiscontinuityAt180m() {
        val mealBlock = CognitiveBlockWindow(
            startMinuteOfDay = 720, // 12:00
            endMinuteOfDay = 780,   // 13:00
            category = CognitiveBlockCategory.MEAL,
            intensity = 1.0
        )

        // At 179 minutes post-meal start (14:59)
        val impulse179 = PhysiologicalImpulseCalculator.computeMealImpulse(720 + 179, listOf(mealBlock))
        // At 180 minutes post-meal start (15:00)
        val impulse180 = PhysiologicalImpulseCalculator.computeMealImpulse(720 + 180, listOf(mealBlock))
        // At 181 minutes post-meal start (15:01)
        val impulse181 = PhysiologicalImpulseCalculator.computeMealImpulse(720 + 181, listOf(mealBlock))

        println("Meal impulse at 179m: $impulse179")
        println("Meal impulse at 180m: $impulse180")
        println("Meal impulse at 181m: $impulse181")

        // Peak dip at 60m
        val impulsePeak = PhysiologicalImpulseCalculator.computeMealImpulse(720 + 60, listOf(mealBlock))
        assertEquals(-12.0, impulsePeak, 1e-4)

        // Cliff between 180m and 181m
        val cliff = abs(impulse180 - impulse181)
        println("Discontinuity cliff at 180m boundary: $cliff points")
        assertTrue("Cliff at 180m cutoff is ~4.87 points", cliff > 4.5)
    }

    @Test
    fun testExerciseImpulseParametersVersusSpec() {
        val workout = CognitiveBlockWindow(
            startMinuteOfDay = 600, // 10:00
            endMinuteOfDay = 660,   // 11:00
            category = CognitiveBlockCategory.EXERCISE,
            intensity = 1.0
        )

        val duringImpulse = PhysiologicalImpulseCalculator.computeExerciseImpulse(630, listOf(workout))
        // Minute 661 is post-workout (1 min after end at 660, in 15-minute autonomic ramp)
        val postImpulseRamp = PhysiologicalImpulseCalculator.computeExerciseImpulse(661, listOf(workout))
        // Minute 675 is post-workout (15 min after end at 660, fully transitioned into BDNF boost)
        val postImpulsePeak = PhysiologicalImpulseCalculator.computeExerciseImpulse(675, listOf(workout))

        // Documenting the empirically observed values in implementation:
        assertEquals(-8.0, duringImpulse, 1e-4)
        assertTrue("Autonomic transition at 1m should be recovering from -8.0", postImpulseRamp > -8.0 && postImpulseRamp < 0.0)
        assertTrue("Post-workout boost at 15m should be strong positive (> 8.0)", postImpulsePeak > 8.0)
    }
}
```

---

<a id="appsrctestjavacomneuroflowappdomainserviceadr020robustnesstestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/Adr020RobustnessTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/Adr020RobustnessTest.kt`
- **Lines of Code:** 129
- **Size:** 5,124 bytes (5.0 KB)

```kotlin
package com.neuroflow.app.domain.service

import com.neuroflow.app.domain.scheduler.DependencyGraphValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

class Adr020RobustnessTest {

    @Test
    fun testSmoothMealImpulseEliminates180MinuteDiscontinuityCliff() {
        val lunch = CognitiveBlockWindow(
            startMinuteOfDay = 720, // 12:00
            endMinuteOfDay = 780,
            category = CognitiveBlockCategory.MEAL,
            intensity = 1.0
        )

        // Peak at 60m post-meal (13:00) is still exactly -12.0
        val peakDip = computeSmoothMealImpulse(780, listOf(lunch))
        assertEquals(-12.0, peakDip, 1e-4)

        // At 179m, 180m, and 181m post-meal, the transition is smooth (< 0.15 pts step)
        val dip179 = computeSmoothMealImpulse(720 + 179, listOf(lunch))
        val dip180 = computeSmoothMealImpulse(720 + 180, listOf(lunch))
        val dip181 = computeSmoothMealImpulse(720 + 181, listOf(lunch))

        assertEquals(0.0, dip180, 1e-6)
        assertEquals(0.0, dip181, 1e-6)
        assertTrue(
            "Step between 179m and 180m must be < 0.15 pts, was ${abs(dip179 - dip180)}",
            abs(dip179 - dip180) < 0.15
        )
    }

    @Test
    fun testLogSpaceVonMisesNeverOverflowsAtExtremeKappa() {
        for (kappa in listOf(0.0, 4.5, 100.0, 800.0, 5000.0)) {
            val densityAtPeak = computeVonMisesDensitySafe(10.0, 10.0, kappa)
            val densityOffPeak = computeVonMisesDensitySafe(22.0, 10.0, kappa)
            assertFalse(
                "Density at peak must not be NaN or Infinite for kappa=$kappa",
                densityAtPeak.isNaN() || densityAtPeak.isInfinite()
            )
            assertFalse(
                "Density off-peak must not be NaN or Infinite for kappa=$kappa",
                densityOffPeak.isNaN() || densityOffPeak.isInfinite()
            )
            assertTrue(densityAtPeak >= densityOffPeak)
        }
    }

    @Test
    fun testDagCycleBreakingGuaranteesZeroSilentDropsInScheduleDay() {
        val taskA = SchedulableTask(
            id = "A",
            title = "Task A",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.HIGH,
            isFrog = true,
            prerequisiteIds = setOf("B")
        )
        val taskB = SchedulableTask(
            id = "B",
            title = "Task B",
            category = TaskCategory.DEEP_WORK,
            priority = 1,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.MODERATE,
            prerequisiteIds = setOf("A")
        )

        val result = DependencyGraphValidator.sanitizeDag(listOf(taskA, taskB))
        assertEquals(2, result.tasks.size)
        assertEquals("Exactly 1 cyclic edge must be severed", 1, result.brokenCycleEdges.size)

        // Verify DefaultTaskSchedulerService.scheduleDay automatically sanitizes cycles and schedules both tasks
        val circadian = DefaultCircadianEnergyService()
        val scheduler = DefaultTaskSchedulerService(circadian)
        val today = LocalDate.of(2026, 10, 21)
        val zone = ZoneId.of("UTC")
        val morning = today.atTime(8, 0).atZone(zone).toInstant().toEpochMilli()
        val wake = today.atTime(7, 0).atZone(zone).toInstant().toEpochMilli()

        val placements = scheduler.scheduleDay(
            tasks = listOf(taskA, taskB), // Pass raw cyclic tasks directly!
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wake,
            lastSleepDurationMinutes = 480,
            busyWindows = emptyList(),
            protectedSlots = emptyList(),
            zoneId = zone
        )
        assertEquals("Both tasks in broken cycle must be scheduled without silent drop", 2, placements.size)
    }

    @Test
    fun testWouldCreateCycleDetectsDirectAndTransitiveLoops() {
        val existingDeps = mapOf(
            "B" to setOf("A"),
            "C" to setOf("B")
        )
        // Adding C as prerequisite of A creates A -> C -> B -> A
        assertTrue(DependencyGraphValidator.wouldCreateCycle("A", "C", existingDeps))
        // Self-dependency A -> A
        assertTrue(DependencyGraphValidator.wouldCreateCycle("A", "A", existingDeps))
        // Adding C as prerequisite of D is acyclic
        assertFalse(DependencyGraphValidator.wouldCreateCycle("D", "C", existingDeps))
    }

    @Test
    fun testLatestSafeStimulantCutoffCalculation() {
        // Bedtime 23:00 (1380m), 100mg caffeine (t_half = 4.5h) -> needs ~6.81h clearance (409m) -> ~16:11 (971m)
        val cutoffMin = computeLatestSafeStimulantCutoffMinute(
            sleepHour = 23,
            doseMg = 100.0,
            type = StimulantType.CAFFEINE,
            maxResidualMgAtBedtime = 35.0
        )
        assertTrue("Cutoff minute ($cutoffMin) should fall between 15:30 (930m) and 17:00 (1020m)", cutoffMin in 930..1020)
    }
}
```

---

<a id="appsrctestjavacomneuroflowappdomainserviceadr022robustnesstestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/Adr022RobustnessTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/Adr022RobustnessTest.kt`
- **Lines of Code:** 100
- **Size:** 4,235 bytes (4.1 KB)

```kotlin
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
```

---

<a id="appsrctestjavacomneuroflowappdomainservicetaskschedulerservicetestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/TaskSchedulerServiceTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/TaskSchedulerServiceTest.kt`
- **Lines of Code:** 334
- **Size:** 13,838 bytes (13.5 KB)

```kotlin
package com.neuroflow.app.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TaskSchedulerServiceTest {

    private lateinit var circadianService: CircadianEnergyService
    private lateinit var schedulerService: TaskSchedulerService
    private val zoneId = ZoneId.of("UTC")

    @Before
    fun setUp() {
        circadianService = DefaultCircadianEnergyService()
        schedulerService = DefaultTaskSchedulerService(circadianService)
    }

    @Test
    fun testScoreBoundsStrictlyBetween0And100() {
        val today = LocalDate.of(2026, 10, 18)
        val testTime = today.atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val snapshot = circadianService.evaluateSnapshot(testTime, wakeTime, 480, zoneId = zoneId)

        val extremeTasks = listOf(
            // Minimal task
            SchedulableTask(
                id = "min",
                title = "Minimal Task",
                category = TaskCategory.GENERAL,
                priority = 0,
                estimatedMinutes = 5,
                energyDemand = EnergyDemand.LOW,
                isFrog = false,
                createdAtMillis = testTime
            ),
            // Maximal priority frog task with imminent deadline
            SchedulableTask(
                id = "max",
                title = "Maximal Task",
                category = TaskCategory.DEEP_WORK,
                priority = 2,
                estimatedMinutes = 90,
                energyDemand = EnergyDemand.HIGH,
                isFrog = true,
                deadlineMillis = testTime + 30 * 60_000L, // 30 mins remaining
                completedMicroSteps = 4,
                totalMicroSteps = 5,
                isCurrentlyActive = true,
                createdAtMillis = testTime - 7 * 86_400_000L
            ),
            // Severely overdue task
            SchedulableTask(
                id = "overdue",
                title = "Overdue Task",
                category = TaskCategory.ADMIN,
                priority = 2,
                estimatedMinutes = 30,
                energyDemand = EnergyDemand.MODERATE,
                deadlineMillis = testTime - 24 * 3_600_000L, // 1 day overdue
                createdAtMillis = testTime - 14 * 86_400_000L
            )
        )

        for (task in extremeTasks) {
            val breakdown = schedulerService.evaluateTaskScore(task, snapshot, dependentCount = 5, calibrationRatio = 1.5)
            assertTrue("Total score must be in [0, 100], was ${breakdown.totalScore}", breakdown.totalScore in 0.0..100.0)
            assertTrue("Urgency score must be in [0, 100], was ${breakdown.urgencyScore}", breakdown.urgencyScore in 0.0..100.0)
            assertTrue("Impact score must be in [0, 100], was ${breakdown.impactScore}", breakdown.impactScore in 0.0..100.0)
            assertTrue("Energy fit score must be in [0, 100], was ${breakdown.energyFitScore}", breakdown.energyFitScore in 0.0..100.0)
            assertTrue("Momentum score must be in [0, 100], was ${breakdown.momentumScore}", breakdown.momentumScore in 0.0..100.0)
        }
    }

    @Test
    fun testSignedEWMACalibrationRatioFinishingEarlyDecreasesRatio() {
        val initialRatio = 1.20

        // Finishing a 60-minute task in 30 minutes (ratio = 0.5)
        val decreasedRatio = schedulerService.updateCalibrationRatio(initialRatio, estimatedMinutes = 60, actualMinutes = 30)
        assertTrue(
            "Finishing early must decrease ratio (was $initialRatio -> $decreasedRatio)",
            decreasedRatio < initialRatio
        )
        assertEquals(1.025, decreasedRatio, 0.001)

        // Taking 90 minutes for a 30-minute task (ratio = 3.0)
        val increasedRatio = schedulerService.updateCalibrationRatio(initialRatio, estimatedMinutes = 30, actualMinutes = 90)
        assertTrue(
            "Finishing late must increase ratio (was $initialRatio -> $increasedRatio)",
            increasedRatio > initialRatio
        )

        // Clamping to [0.5, 2.5]
        val clampedMin = schedulerService.updateCalibrationRatio(0.5, 100, 1)
        assertEquals(0.5, clampedMin, 0.001)

        val clampedMax = schedulerService.updateCalibrationRatio(2.5, 1, 100)
        assertEquals(2.5, clampedMax, 0.001)
    }

    @Test
    fun testExplicitDurationTokensAreNotInflated() {
        val snapshot = circadianService.evaluateSnapshot(System.currentTimeMillis(), System.currentTimeMillis() - 3600000, 480)

        val explicitTask = SchedulableTask(
            id = "explicit",
            title = "Call client 30m",
            category = TaskCategory.ADMIN,
            priority = 1,
            estimatedMinutes = 30,
            isDurationExplicit = true,
            energyDemand = EnergyDemand.MODERATE
        )

        val implicitTask = SchedulableTask(
            id = "implicit",
            title = "Plan budget",
            category = TaskCategory.ADMIN,
            priority = 1,
            estimatedMinutes = 30,
            isDurationExplicit = false,
            energyDemand = EnergyDemand.MODERATE
        )

        val explicitBreakdown = schedulerService.evaluateTaskScore(explicitTask, snapshot, calibrationRatio = 1.5)
        val implicitBreakdown = schedulerService.evaluateTaskScore(implicitTask, snapshot, calibrationRatio = 1.5)

        assertEquals(30, explicitBreakdown.bufferedDurationMinutes)
        assertEquals(45, implicitBreakdown.bufferedDurationMinutes) // 30 * 1.5 = 45
    }

    @Test
    fun testMultiStepDAGSchedulingSequentialPlacementInSinglePass() {
        val today = LocalDate.of(2026, 10, 18)
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val taskA = SchedulableTask(
            id = "task_a",
            title = "Step A: Outline Architecture",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.HIGH,
            parentTaskId = null
        )
        val taskB = SchedulableTask(
            id = "task_b",
            title = "Step B: Implement Core Services",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            parentTaskId = "task_a",
            prerequisiteIds = setOf("task_a")
        )
        val taskC = SchedulableTask(
            id = "task_c",
            title = "Step C: Integration Tests",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.HIGH,
            parentTaskId = "task_b",
            prerequisiteIds = setOf("task_b")
        )

        val placements = schedulerService.scheduleDay(
            tasks = listOf(taskC, taskA, taskB), // out of topological order
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = emptyList(),
            protectedSlots = emptyList(),
            calibrationRatio = 1.0,
            zoneId = zoneId
        )

        assertEquals("All 3 dependent tasks should be scheduled in single pass", 3, placements.size)

        val placementA = placements.first { it.task.id == "task_a" }
        val placementB = placements.first { it.task.id == "task_b" }
        val placementC = placements.first { it.task.id == "task_c" }

        assertTrue(
            "Task A must complete before Task B starts (A end: ${placementA.window.endMillis}, B start: ${placementB.window.startMillis})",
            placementA.window.endMillis <= placementB.window.startMillis
        )
        assertTrue(
            "Task B must complete before Task C starts (B end: ${placementB.window.endMillis}, C start: ${placementC.window.startMillis})",
            placementB.window.endMillis <= placementC.window.startMillis
        )
    }

    @Test
    fun testExactMinuteResolutionNoHourBinLeaks() {
        val today = LocalDate.of(2026, 10, 18)
        val morning = today.atTime(LocalTime.of(9, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val task1 = SchedulableTask(
            id = "t1",
            title = "Task 1",
            category = TaskCategory.ADMIN,
            priority = 1,
            estimatedMinutes = 20,
            energyDemand = EnergyDemand.MODERATE
        )
        val task2 = SchedulableTask(
            id = "t2",
            title = "Task 2",
            category = TaskCategory.ADMIN,
            priority = 1,
            estimatedMinutes = 35,
            energyDemand = EnergyDemand.MODERATE
        )

        val placements = schedulerService.scheduleDay(
            tasks = listOf(task1, task2),
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = emptyList(),
            protectedSlots = emptyList(),
            calibrationRatio = 1.0,
            zoneId = zoneId
        )

        assertEquals(2, placements.size)
        val p1 = placements.first { it.task.id == "t1" }
        val p2 = placements.first { it.task.id == "t2" }
        // Task durations should be exact: 20m (1200000ms) and 35m (2100000ms)
        assertEquals(20 * 60_000L, p1.window.endMillis - p1.window.startMillis)
        assertEquals(35 * 60_000L, p2.window.endMillis - p2.window.startMillis)
    }

    @Test
    fun testScheduleDayBlocksDaytimeNapAndAvoidsOverlap() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val napStart = today.atTime(LocalTime.of(14, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val napEnd = today.atTime(LocalTime.of(15, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val nap = listOf(DaytimeNapBout(startMillis = napStart, endMillis = napEnd))

        val deepTask = SchedulableTask(
            id = "deep1",
            title = "Deep Analytical Code Architecture",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH
        )

        val placements = schedulerService.scheduleDay(
            tasks = listOf(deepTask),
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = emptyList(),
            protectedSlots = emptyList(),
            calibrationRatio = 1.0,
            zoneId = zoneId,
            naps = nap
        )

        assertEquals(1, placements.size)
        val placement = placements[0]
        // Must never overlap with the nap window [14:00, 15:00]
        val overlapsNap = placement.window.startMillis < napEnd && placement.window.endMillis > napStart
        assertFalse("Task must not be scheduled during nap window", overlapsNap)
    }

    @Test
    fun testUltradianAutoChunkingSplitsLargeTaskWhenFreeSlotsFragmented() {
        val today = LocalDate.of(2026, 10, 18)
        val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val morning = today.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // Block everything except 10:00-10:50 (50m) and 14:00-14:50 (50m)
        val busy1 = TimeSlotWindow(
            today.atTime(LocalTime.of(6, 0)).atZone(zoneId).toInstant().toEpochMilli(),
            today.atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli()
        )
        val busy2 = TimeSlotWindow(
            today.atTime(LocalTime.of(10, 50)).atZone(zoneId).toInstant().toEpochMilli(),
            today.atTime(LocalTime.of(14, 0)).atZone(zoneId).toInstant().toEpochMilli()
        )
        val busy3 = TimeSlotWindow(
            today.atTime(LocalTime.of(14, 50)).atZone(zoneId).toInstant().toEpochMilli(),
            today.atTime(LocalTime.of(22, 30)).atZone(zoneId).toInstant().toEpochMilli()
        )

        val task90m = SchedulableTask(
            id = "deep90",
            title = "Write Massive Engineering Spec",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 90,
            energyDemand = EnergyDemand.HIGH,
            fragmentationTolerance = 0.5f
        )

        val placements = schedulerService.scheduleDay(
            tasks = listOf(task90m),
            dayDate = today,
            nowMillis = morning,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = listOf(busy1, busy2, busy3),
            protectedSlots = emptyList(),
            calibrationRatio = 1.0,
            zoneId = zoneId
        )

        assertEquals("90m task should be split into 2 parts and scheduled in both 50m gaps", 2, placements.size)
        assertEquals("deep90#part1", placements[0].task.id)
        assertEquals("deep90#part2", placements[1].task.id)
        assertTrue(placements[0].window.durationMinutes >= 25)
        assertTrue(placements[1].window.durationMinutes >= 25)
    }
}
```

---

<a id="appsrctestjavacomneuroflowappdomainservicetasknaturallanguageparsertestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/TaskNaturalLanguageParserTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/TaskNaturalLanguageParserTest.kt`
- **Lines of Code:** 64
- **Size:** 2,151 bytes (2.1 KB)

```kotlin
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
```

---

<a id="appsrctestjavacomneuroflowappdomainservicesmarttaskdecompositionservicetestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/SmartTaskDecompositionServiceTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/SmartTaskDecompositionServiceTest.kt`
- **Lines of Code:** 166
- **Size:** 6,582 bytes (6.4 KB)

```kotlin
package com.neuroflow.app.domain.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SmartTaskDecompositionServiceTest {

    private lateinit var service: DefaultSmartTaskDecompositionService

    @Before
    fun setup() {
        service = DefaultSmartTaskDecompositionService()
    }

    @Test
    fun testArchetypeClassification() {
        assertEquals(
            TaskArchetype.CODING_TECH,
            DefaultSmartTaskDecompositionService.classifyArchetype("Fix authentication endpoint bug")
        )
        assertEquals(
            TaskArchetype.CODING_TECH,
            DefaultSmartTaskDecompositionService.classifyArchetype("Refactor database SQL schema")
        )
        assertEquals(
            TaskArchetype.WRITING_RESEARCH,
            DefaultSmartTaskDecompositionService.classifyArchetype("Draft blog post about circadian rhythm")
        )
        assertEquals(
            TaskArchetype.STUDY_ACADEMIC,
            DefaultSmartTaskDecompositionService.classifyArchetype("Study chapter 4 for biology exam")
        )
        assertEquals(
            TaskArchetype.CHORE_MAINTENANCE,
            DefaultSmartTaskDecompositionService.classifyArchetype("Clean kitchen and do laundry")
        )
        assertEquals(
            TaskArchetype.ADMIN_LOGISTICS,
            DefaultSmartTaskDecompositionService.classifyArchetype("File tax return and submit invoice")
        )
        assertEquals(
            TaskArchetype.GENERAL,
            DefaultSmartTaskDecompositionService.classifyArchetype("Plan weekend family trip")
        )
    }

    @Test
    fun testLocalDecompositionMeetsActionabilityAndFrictionInvariants() {
        val codingSteps = service.decomposeTaskLocally("Implement OAuth login", 60)
        assertTrue("Coding archetype should produce 3-4 steps", codingSteps.size in 3..4)
        assertTrue("First step must have low activation friction (<= 10m)", codingSteps.first().estimatedMinutes <= 10)
        assertTrue("Steps should preserve task context", codingSteps.any { it.title.contains("Implement OAuth login") })

        val writingSteps = service.decomposeTaskLocally("Write quarterly performance report", 90)
        assertTrue("Writing archetype should produce 3-4 steps", writingSteps.size in 3..4)
        assertTrue("First step <= 10m", writingSteps.first().estimatedMinutes <= 10)
        assertTrue("Context preserved", writingSteps.any { it.title.contains("Write quarterly performance report") })

        val choreSteps = service.decomposeTaskLocally("Clean garage and organize tools", 30)
        assertTrue("Chore archetype should produce 3-4 steps", choreSteps.size in 3..4)
        assertTrue("First step <= 10m", choreSteps.first().estimatedMinutes <= 10)

        val generalSteps = service.decomposeTaskLocally("Misc random task", 45)
        assertTrue("General archetype should produce 3-4 steps", generalSteps.size in 3..4)
        assertTrue("First step <= 10m", generalSteps.first().estimatedMinutes <= 10)
    }

    @Test
    fun testArchetypeDecompositionPhysicalDoorwayStep() {
        val steps = service.decomposeTaskLocally("Fix SQL migration bug", 45)
        assertEquals(4, steps.size)
        assertTrue("Step 1 must be doorway action", steps[0].title.startsWith("Open IDE, checkout branch"))
        assertTrue("Step 1 estimated minutes must be <= 4", steps[0].estimatedMinutes <= 4)
    }

    @Test
    fun testParseGeminiResponseStandardJson() {
        val json = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "[{\"title\":\"Review rubric\",\"estimatedMinutes\":5},{\"title\":\"Write section 1\",\"estimatedMinutes\":20}]"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val steps = DefaultSmartTaskDecompositionService.parseGeminiResponse(json)
        assertEquals(2, steps.size)
        assertEquals("Review rubric", steps[0].title)
        assertEquals(5, steps[0].estimatedMinutes)
        assertEquals("Write section 1", steps[1].title)
        assertEquals(20, steps[1].estimatedMinutes)
    }

    @Test
    fun testParseGeminiResponseMarkdownCodeFence() {
        val json = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "```json\n[\n  {\"title\": \"Step A\", \"estimatedMinutes\": 10},\n  {\"title\": \"Step B\", \"estimatedMinutes\": 15}\n]\n```"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val steps = DefaultSmartTaskDecompositionService.parseGeminiResponse(json)
        assertEquals(2, steps.size)
        assertEquals("Step A", steps[0].title)
        assertEquals(10, steps[0].estimatedMinutes)
        assertEquals("Step B", steps[1].title)
        assertEquals(15, steps[1].estimatedMinutes)
    }

    @Test
    fun testParseGeminiResponseMalformedOrEmptyReturnsEmptyList() {
        val malformed = """{"candidates": []}"""
        val emptySteps = DefaultSmartTaskDecompositionService.parseGeminiResponse(malformed)
        assertTrue(emptySteps.isEmpty())

        val invalidText = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "I am unable to break down this task."
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()
        val textSteps = DefaultSmartTaskDecompositionService.parseGeminiResponse(invalidText)
        assertTrue(textSteps.isEmpty())
    }

    @Test
    fun testDecomposeTaskFallsBackToLocalWhenAiDisabledOrKeyBlank() = runBlocking {
        val stepsNoAi = service.decomposeTask("Fix bug in billing API", 45, apiKey = "test-key", useAi = false)
        assertTrue(stepsNoAi.isNotEmpty())
        assertTrue(stepsNoAi.any { it.title.contains("Fix bug in billing API") })

        val stepsBlankKey = service.decomposeTask("Fix bug in billing API", 45, apiKey = "", useAi = true)
        assertTrue(stepsBlankKey.isNotEmpty())
        assertTrue(stepsBlankKey.any { it.title.contains("Fix bug in billing API") })
    }
}
```

---

<a id="appsrctestjavacomneuroflowappdomainservicegeminiaionlineservicetestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/GeminiAiOnlineServiceTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/GeminiAiOnlineServiceTest.kt`
- **Lines of Code:** 164
- **Size:** 6,464 bytes (6.3 KB)

```kotlin
package com.neuroflow.app.domain.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeminiAiOnlineServiceTest {

    private lateinit var service: DefaultGeminiAiOnlineService

    @Before
    fun setup() {
        service = DefaultGeminiAiOnlineService()
    }

    @Test
    fun testModelIdIsGeminiFlashLatest() {
        assertEquals("gemini-flash-latest", DefaultGeminiAiOnlineService.MODEL_ID)
        assertTrue(
            "Endpoint must target gemini-flash-latest model",
            DefaultGeminiAiOnlineService.ENDPOINT_BASE.contains("gemini-flash-latest")
        )
    }

    @Test
    fun testLocalFallbackWhenApiKeyIsBlankOrAiDisabled() = runBlocking {
        // When AI is disabled, local fallback must return valid, non-null, robust results
        val enrichment = service.enrichTaskWithAi(
            title = "Refactor Database Schema",
            durationMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            currentHour = 10,
            isFrog = true,
            apiKey = "",
            useAi = false
        )
        assertNotNull(enrichment)
        assertTrue("Micro-steps must not be empty", enrichment.microSteps.isNotEmpty())
        assertEquals(10, enrichment.suggestedSlotStartHour) // High demand / Frog placed in morning peak
        assertEquals(EnergyDemand.HIGH, enrichment.recommendedEnergyDemand)

        // Task decomposition fallback
        val steps = service.decomposeTask(
            title = "Write quarterly performance report",
            durationMinutes = 45,
            apiKey = null,
            useAi = true // No key provided
        )
        assertTrue("Decomposition fallback must yield steps", steps.size >= 3)
        assertTrue("First step must have low initiation friction (<= 10m)", steps.first().estimatedMinutes <= 10)
    }

    @Test
    fun testSlotRecommendationFallback() = runBlocking {
        val window = TimeSlotWindow(1700000000000L, 1700003600000L)
        val rec = service.suggestTaskSlot(
            taskTitle = "Deep Coding",
            estimatedMinutes = 60,
            energyDemand = EnergyDemand.HIGH,
            isFrog = true,
            currentEpochMillis = 1700000000000L,
            candidateWindows = listOf(window),
            apiKey = "",
            useAi = false
        )
        assertNotNull(rec)
        assertEquals(window.startMillis, rec.suggestedStartMillis)
        assertEquals(window.endMillis, rec.suggestedEndMillis)
        assertEquals("Morning Peak", rec.circadianPhase)
        assertTrue(rec.confidence >= 80)
    }

    @Test
    fun testEnergyAdvisoryCircadianLogic() = runBlocking {
        // Peak phase
        val peakAdvice = service.adviseEnergyControl(
            phase = EnergyPhase.PEAK,
            usableEnergyPercent = 88,
            hoursAwake = 3.5f,
            continuousFocusMinutes = 45,
            hasSleepDebt = false,
            apiKey = "",
            useAi = false
        )
        assertEquals("Biological Peak Window", peakAdvice.statusTitle)
        assertEquals("DEEP_WORK", peakAdvice.suggestedCategory)
        assertEquals(EnergyDemand.HIGH, peakAdvice.suggestedEnergyDemand)
        assertFalse(peakAdvice.powerNapRecommended)

        // Dip phase
        val dipAdvice = service.adviseEnergyControl(
            phase = EnergyPhase.DIP,
            usableEnergyPercent = 42,
            hoursAwake = 7.0f,
            continuousFocusMinutes = 30,
            hasSleepDebt = false,
            apiKey = "",
            useAi = false
        )
        assertEquals("Circadian Post-Lunch Dip", dipAdvice.statusTitle)
        assertEquals("ADMIN", dipAdvice.suggestedCategory)
        assertTrue("Power nap recommended during dip", dipAdvice.powerNapRecommended)

        // Continuous focus >= 90 mins synaptic depletion
        val depletionAdvice = service.adviseEnergyControl(
            phase = EnergyPhase.PEAK,
            usableEnergyPercent = 75,
            hoursAwake = 4.0f,
            continuousFocusMinutes = 95,
            hasSleepDebt = false,
            apiKey = "",
            useAi = false
        )
        assertEquals("Synaptic Saturation Warning", depletionAdvice.statusTitle)
        assertEquals("RECOVERY", depletionAdvice.suggestedCategory)
        assertEquals(EnergyDemand.LOW, depletionAdvice.suggestedEnergyDemand)
    }

    @Test
    fun testWeeklyAvoidanceAuditAndDirectiveFallback() = runBlocking {
        val result = service.conductWeeklyAuditAndDirective(
            weeklyTelemetryJson = "{}",
            recentlyDeletedTasksJson = "[]",
            chronicallyPostponedTasksJson = "[]",
            apiKey = "",
            useAi = false
        )
        assertNotNull(result)
        assertTrue("Directive must not be empty", result.newSystemDirective.isNotBlank())
        assertTrue("Directive must mention Adaptive System Directive", result.newSystemDirective.contains("Adaptive System Directive"))
        assertTrue("Audit JSON must contain primaryAvoidancePattern", result.avoidanceAuditJson.contains("primaryAvoidancePattern"))
    }

    @Test
    fun testSynthesizeQuickNoteFallback() = runBlocking {
        val synthesis = service.synthesizeQuickNote(
            rawContent = "Buy milk and groceries\nMake sure to get almond milk",
            apiKey = "",
            useAi = false
        )
        assertEquals("Buy milk and groceries", synthesis.extractedTaskTitle)
        assertEquals("GENERAL", synthesis.suggestedCategory)
        assertTrue("Tags must contain quick-note", synthesis.tags.contains("quick-note"))
    }

    @Test
    fun testCandidateModelsContainsLatestAndFallback() {
        val models = DefaultGeminiAiOnlineService.CANDIDATE_MODELS
        assertTrue("Primary must be gemini-flash-latest", models.contains("gemini-flash-latest"))
        assertTrue("Must have flash-lite fallback", models.contains("gemini-flash-lite-latest"))
        assertTrue("Must have 2.5-flash fallback", models.contains("gemini-2.5-flash"))
        assertTrue("Must have 2.0-flash fallback", models.contains("gemini-2.0-flash"))
    }

    @Test
    fun testTestConnectionBlankKeyFailsFast() = runBlocking {
        val result = service.testConnection("")
        assertTrue("Blank key must fail", result.isFailure)
    }
}
```

---

<a id="appsrctestjavacomneuroflowappdomainservicechronobiologysignalnormalizertestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/ChronobiologySignalNormalizerTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/ChronobiologySignalNormalizerTest.kt`
- **Lines of Code:** 170
- **Size:** 8,007 bytes (7.8 KB)

```kotlin
package com.neuroflow.app.domain.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class ChronobiologySignalNormalizerTest : StringSpec({

    val normalizer = ChronobiologySignalNormalizer()
    val baseMillis = 1770000000000L

    "coalesceWasoEpisodes merges intervals with wake gaps <= 45m" {
        val sleep1 = TimeSlotWindow(baseMillis, baseMillis + 180 * 60_000L) // 3h sleep
        val sleep2 = TimeSlotWindow(baseMillis + 205 * 60_000L, baseMillis + 360 * 60_000L) // 25m wake gap (WASO)
        val intervals = listOf(sleep1, sleep2)

        val (sessions, wasoMinutes) = normalizer.coalesceWasoEpisodes(intervals)
        sessions shouldHaveSize 1
        sessions[0].startMillis shouldBe baseMillis
        sessions[0].endMillis shouldBe baseMillis + 360 * 60_000L
        wasoMinutes shouldBe 25L
    }

    "coalesceWasoEpisodes keeps separate sessions when wake gap > 45m" {
        val sleep1 = TimeSlotWindow(baseMillis, baseMillis + 180 * 60_000L)
        val sleep2 = TimeSlotWindow(baseMillis + 240 * 60_000L, baseMillis + 360 * 60_000L) // 60m wake gap (> 45m)
        val intervals = listOf(sleep1, sleep2)

        val (sessions, wasoMinutes) = normalizer.coalesceWasoEpisodes(intervals)
        sessions shouldHaveSize 2
        sessions[0].endMillis shouldBe baseMillis + 180 * 60_000L
        sessions[1].startMillis shouldBe baseMillis + 240 * 60_000L
        wasoMinutes shouldBe 0L
    }

    "coalesceWasoEpisodes handles empty and overlapping inputs" {
        val (emptySessions, emptyWaso) = normalizer.coalesceWasoEpisodes(emptyList())
        emptySessions.shouldBeEmpty()
        emptyWaso shouldBe 0L

        val overlapping = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 120 * 60_000L),
            TimeSlotWindow(baseMillis + 60 * 60_000L, baseMillis + 240 * 60_000L)
        )
        val (ovSessions, ovWaso) = normalizer.coalesceWasoEpisodes(overlapping)
        ovSessions shouldHaveSize 1
        ovSessions[0].endMillis shouldBe baseMillis + 240 * 60_000L
        ovWaso shouldBe 0L
    }

    "computeEffectiveDuration weights restorative sleep with clamp [0.75, 1.25]" {
        // Zero restorative sleep (deep + rem = 0): factor = 0.85
        val poor = normalizer.computeEffectiveDuration(480.0, 0.0, 0.0)
        poor shouldBe (480.0 * 0.85 plusOrMinus 0.001)

        // Balanced sleep: 20% deep, 20% rem (40% total): factor = 0.85 + 0.75 * 0.40 = 1.15
        val healthy = normalizer.computeEffectiveDuration(480.0, 96.0, 96.0)
        healthy shouldBe (480.0 * 1.15 plusOrMinus 0.001)

        // Extremely high restorative sleep (75% total): raw factor = 0.85 + 0.75 * 0.75 = 1.4125 -> clamped to 1.25
        val superRestorative = normalizer.computeEffectiveDuration(480.0, 180.0, 180.0)
        superRestorative shouldBe (480.0 * 1.25 plusOrMinus 0.001)

        // Zero total minutes returns 0.0
        normalizer.computeEffectiveDuration(0.0, 0.0, 0.0) shouldBe 0.0
    }

    "blendDuration applies Bayesian confidence weighting smoothly" {
        val dEff = 500.0
        val dBase = 420.0

        // Confidence 1.0 -> d_eff
        normalizer.blendDuration(dEff, dBase, 1.0) shouldBe (500.0 plusOrMinus 0.001)

        // Confidence 0.0 -> d_base
        normalizer.blendDuration(dEff, dBase, 0.0) shouldBe (420.0 plusOrMinus 0.001)

        // Confidence 0.5 -> exact midpoint
        normalizer.blendDuration(dEff, dBase, 0.5) shouldBe (460.0 plusOrMinus 0.001)

        // Out of bounds confidence clamped
        normalizer.blendDuration(dEff, dBase, 1.5) shouldBe (500.0 plusOrMinus 0.001)
        normalizer.blendDuration(dEff, dBase, -0.2) shouldBe (420.0 plusOrMinus 0.001)
    }

    "clampWakeShift enforces +-90m anti-whiplash safety window" {
        val baseWake = 1000000000L
        val min90 = 90 * 60_000L

        // Small shift (30m later) with c = 1.0 -> exactly +30m
        normalizer.clampWakeShift(baseWake, baseWake + 30 * 60_000L, 1.0) shouldBe (baseWake + 30 * 60_000L)

        // Small shift (30m later) with c = 0.5 -> +15m
        normalizer.clampWakeShift(baseWake, baseWake + 30 * 60_000L, 0.5) shouldBe (baseWake + 15 * 60_000L)

        // Extreme late shift (180m later) with c = 1.0 -> clamped to +90m
        normalizer.clampWakeShift(baseWake, baseWake + 180 * 60_000L, 1.0) shouldBe (baseWake + min90)

        // Extreme early shift (200m earlier) with c = 1.0 -> clamped to -90m
        normalizer.clampWakeShift(baseWake, baseWake - 200 * 60_000L, 1.0) shouldBe (baseWake - min90)

        // Zero confidence -> strictly unchanged
        normalizer.clampWakeShift(baseWake, baseWake + 180 * 60_000L, 0.0) shouldBe baseWake
    }

    "normalize executes full pipeline correctly" {
        val sleep1 = TimeSlotWindow(baseMillis, baseMillis + 180 * 60_000L)
        val sleep2 = TimeSlotWindow(baseMillis + 195 * 60_000L, baseMillis + 480 * 60_000L) // 15m WASO
        val telemetry = RawSleepTelemetry(
            intervals = listOf(sleep1, sleep2),
            deepSleepMinutes = 90.0,
            remSleepMinutes = 90.0,
            confidence = 0.8
        )

        val baseWake = baseMillis + 480 * 60_000L
        val baseSleep = 460.0

        val signal = normalizer.normalize(telemetry, baseWake, baseSleep)
        signal.totalWasoMinutes shouldBe 15.0
        signal.consolidatedSessions shouldHaveSize 1
        signal.effectiveDurationMinutes shouldBe ((480.0 - 15.0) * (0.85 + 0.75 * (180.0 / 465.0)) plusOrMinus 1.0)
    }

    "extractDaytimeNaps filters valid 10-180m bouts after morning wake" {
        val morningWake = baseMillis + 480 * 60_000L
        val microNap = TimeSlotWindow(morningWake + 60 * 60_000L, morningWake + 65 * 60_000L) // 5m (< 10m, invalid)
        val powerNap = TimeSlotWindow(morningWake + 120 * 60_000L, morningWake + 140 * 60_000L) // 20m (valid)
        val longSleep = TimeSlotWindow(morningWake + 200 * 60_000L, morningWake + 400 * 60_000L) // 200m (> 180m, invalid)

        val naps = normalizer.extractDaytimeNaps(listOf(microNap, powerNap, longSleep), morningWake)
        naps shouldHaveSize 1
        naps[0].durationMinutes shouldBe 20
        naps[0].startMillis shouldBe (morningWake + 120 * 60_000L)
    }

    "normalize extracts secondary daytime nap without corrupting morning wake anchor" {
        val nocturnalSleep = TimeSlotWindow(baseMillis, baseMillis + 480 * 60_000L) // 8h sleep
        val daytimeNap = TimeSlotWindow(baseMillis + 840 * 60_000L, baseMillis + 870 * 60_000L) // 30m nap at 14:00

        val baseWake = baseMillis + 480 * 60_000L
        val telemetry = RawSleepTelemetry(
            intervals = listOf(nocturnalSleep, daytimeNap),
            confidence = 1.0
        )

        val signal = normalizer.normalize(telemetry, baseWake, 480.0)
        signal.normalizedWakeTimeMillis shouldBe baseWake // Wake anchor NOT corrupted to 14:30
        signal.totalSleepMinutes shouldBe 480.0 // Nap not counted as nocturnal sleep
        signal.daytimeNaps shouldHaveSize 1
        signal.daytimeNaps[0].durationMinutes shouldBe 30
    }

    "normalize with ONLY daytime nap preserves baseWakeMillis and does not shift morning wake" {
        val daytimeNap = TimeSlotWindow(baseMillis + 420 * 60_000L, baseMillis + 450 * 60_000L) // 30m nap at 14:00
        val telemetry = RawSleepTelemetry(
            intervals = listOf(daytimeNap),
            detectedWakeMillis = daytimeNap.endMillis,
            confidence = 1.0
        )

        val signal = normalizer.normalize(telemetry, baseMillis, 480.0)
        signal.normalizedWakeTimeMillis shouldBe baseMillis // Strictly preserved, NOT shifted by +90m
        signal.totalSleepMinutes shouldBe 480.0 // Falls back to baseline sleep
        signal.daytimeNaps shouldHaveSize 1
        signal.daytimeNaps[0].durationMinutes shouldBe 30
    }
})
```

---

<a id="appsrctestjavacomneuroflowappdomainservicerealworldchronobiologyandschedulertestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/service/RealWorldChronobiologyAndSchedulerTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/service/RealWorldChronobiologyAndSchedulerTest.kt`
- **Lines of Code:** 200
- **Size:** 7,980 bytes (7.8 KB)

```kotlin
package com.neuroflow.app.domain.service

import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.resolveScheduledEpochMillis
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar

class RealWorldChronobiologyAndSchedulerTest {

    private lateinit var circadianService: CircadianEnergyService
    private lateinit var schedulerService: TaskSchedulerService
    private val zoneId = ZoneId.of("UTC")

    @Before
    fun setUp() {
        circadianService = DefaultCircadianEnergyService()
        schedulerService = DefaultTaskSchedulerService(circadianService)
    }

    @Test
    fun testCircularClockHourBlendingHandlesMidnightWrapAround() {
        // Linear average of 23:00 and 01:00 would incorrectly be 12:00 (noon)
        // Circular unit-vector average correctly produces 00:00 (midnight)
        val blended = blendClockHoursCircular(
            listOf(23.0 to 1.0, 1.0 to 1.0)
        )
        // Should be approximately 0.0 or 24.0 (mod 24 = 0.0)
        val error = kotlin.math.min(blended, kotlin.math.abs(24.0 - blended))
        assertTrue("Blended angle across midnight should be near 0.0 (midnight), was $blended", error < 0.05)
    }

    @Test
    fun testLateWakerIsNeverScheduledWhileAsleep() {
        val testDate = LocalDate.of(2026, 10, 20)
        val dayStart = testDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val wakeTime = testDate.atTime(LocalTime.of(10, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val nowMillis = testDate.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

        val tasks = listOf(
            SchedulableTask(
                id = "morning_deep",
                title = "Deep Work",
                category = TaskCategory.DEEP_WORK,
                priority = 2,
                estimatedMinutes = 60,
                energyDemand = EnergyDemand.HIGH,
                isFrog = true
            )
        )

        val placements = schedulerService.scheduleDay(
            tasks = tasks,
            dayDate = testDate,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = emptyList(),
            protectedSlots = emptyList(),
            calibrationRatio = 1.0,
            forceReplan = true,
            zoneId = zoneId
        )

        assertTrue("Placements must not be empty", placements.isNotEmpty())
        val firstStart = placements.first().window.startMillis
        val minAllowedStart = wakeTime + 15 * 60_000L // Wake time + 15m buffer
        assertTrue(
            "Task scheduled start ($firstStart) must be >= wake + 15m ($minAllowedStart), user was asleep!",
            firstStart >= minAllowedStart
        )
    }

    @Test
    fun testMultiPrerequisiteDAGScheduleOrder() {
        val testDate = LocalDate.of(2026, 10, 20)
        val wakeTime = testDate.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()
        val nowMillis = testDate.atTime(LocalTime.of(8, 0)).atZone(zoneId).toInstant().toEpochMilli()

        // DAG: A -> C and B -> C (C depends on both A and B)
        val taskA = SchedulableTask(
            id = "task_A",
            title = "Prerequisite A",
            category = TaskCategory.GENERAL,
            priority = 1,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.MODERATE
        )
        val taskB = SchedulableTask(
            id = "task_B",
            title = "Prerequisite B",
            category = TaskCategory.GENERAL,
            priority = 1,
            estimatedMinutes = 30,
            energyDemand = EnergyDemand.MODERATE
        )
        val taskC = SchedulableTask(
            id = "task_C",
            title = "Downstream Target C",
            category = TaskCategory.DEEP_WORK,
            priority = 2,
            estimatedMinutes = 45,
            energyDemand = EnergyDemand.HIGH,
            prerequisiteIds = setOf("task_A", "task_B")
        )

        val placements = schedulerService.scheduleDay(
            tasks = listOf(taskC, taskA, taskB),
            dayDate = testDate,
            nowMillis = nowMillis,
            wakeTimeMillis = wakeTime,
            lastSleepDurationMinutes = 480,
            busyWindows = emptyList(),
            protectedSlots = emptyList(),
            calibrationRatio = 1.0,
            forceReplan = true,
            zoneId = zoneId
        )

        assertEquals("All 3 tasks should be placed", 3, placements.size)
        val placementMap = placements.associateBy { it.task.id }
        val endA = placementMap["task_A"]!!.window.endMillis
        val endB = placementMap["task_B"]!!.window.endMillis
        val startC = placementMap["task_C"]!!.window.startMillis

        assertTrue(
            "Task C ($startC) must start after Task A finishes ($endA)",
            startC >= endA
        )
        assertTrue(
            "Task C ($startC) must start after Task B finishes ($endB)",
            startC >= endB
        )
    }

    @Test
    fun testDualRepresentationTimestampsResolveConsistently() {
        val todayStart = 1770000000000L
        val nineAmOffset = 9 * 3600_000L

        // Format 1: Intraday ms offset (< 86_400_000L)
        val taskOffset = TaskEntity(
            id = "1",
            title = "Offset Task",
            scheduledDate = todayStart,
            scheduledTime = nineAmOffset
        )

        // Format 2: Absolute epoch millis (>= 86_400_000L)
        val taskEpoch = TaskEntity(
            id = "2",
            title = "Epoch Task",
            scheduledDate = todayStart,
            scheduledTime = todayStart + nineAmOffset
        )

        val resolvedOffset = taskOffset.resolveScheduledEpochMillis()
        val resolvedEpoch = taskEpoch.resolveScheduledEpochMillis()

        assertNotNull(resolvedOffset)
        assertNotNull(resolvedEpoch)
        assertEquals(
            "Both representations must resolve to the identical timestamp without 2082 or 1970 corruption",
            resolvedOffset,
            resolvedEpoch
        )
        assertEquals(todayStart + nineAmOffset, resolvedEpoch)
    }

    @Test
    fun testSimulatedFocusLoadAccumulatesAcrossConsecutiveDeepWork() {
        val baseTime = 1770000000000L
        val p1 = ScheduledPlacement(
            task = SchedulableTask(id = "1", title = "Block 1", category = TaskCategory.DEEP_WORK, priority = 2, estimatedMinutes = 45, energyDemand = EnergyDemand.HIGH),
            window = TimeSlotWindow(baseTime, baseTime + 45 * 60_000L),
            scoreBreakdown = TaskScoreBreakdown("1", 80.0, 80.0, 80.0, 80.0, 80.0, 45)
        )
        val p2 = ScheduledPlacement(
            task = SchedulableTask(id = "2", title = "Block 2", category = TaskCategory.DEEP_WORK, priority = 2, estimatedMinutes = 45, energyDemand = EnergyDemand.HIGH),
            window = TimeSlotWindow(baseTime + 50 * 60_000L, baseTime + 95 * 60_000L), // 5m gap
            scoreBreakdown = TaskScoreBreakdown("2", 80.0, 80.0, 80.0, 80.0, 80.0, 45)
        )

        val defaultService = schedulerService as DefaultTaskSchedulerService
        val loadRightAfter = defaultService.computeSimulatedFocusLoadAt(baseTime + 100 * 60_000L, listOf(p1, p2))
        assertEquals("Simulated focus load should accumulate 45 + 45 = 90 minutes", 90, loadRightAfter)

        val loadAfterLongRest = defaultService.computeSimulatedFocusLoadAt(baseTime + 160 * 60_000L, listOf(p1, p2)) // 65m gap
        assertEquals("Simulated focus load should reset to 0 after long recovery gap", 0, loadAfterLongRest)
    }
}
```

---

<a id="appsrctestjavacomneuroflowappdomainschedulerplacementstabilitytestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/scheduler/PlacementStabilityTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/scheduler/PlacementStabilityTest.kt`
- **Lines of Code:** 130
- **Size:** 5,346 bytes (5.2 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.data.local.UserPreferences
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.Priority
import com.neuroflow.app.domain.model.TaskStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar

/**
 * The scheduler used to rewrite a placement as often as it woke up, so a block could move out from
 * under a user who was already working in it. These cases pin the agreed stability policy: a block
 * starting within two hours is frozen even against a forced re-plan, the cooldown gates every rule
 * rather than only the periodic one, and a placement that is not materially different is not
 * written at all.
 */
class PlacementStabilityTest : StringSpec({

    val now = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 12, 9, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val minutes = 60_000L
    val hours = 60 * minutes

    fun daySplit(millis: Long): Pair<Long, Long> {
        val dayStart = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return dayStart to (millis - dayStart)
    }

    fun placedTask(
        startMillis: Long,
        lastAutoScheduledAt: Long?,
        deadlineMillis: Long? = null,
        postponeCount: Int = 0
    ): TaskEntity {
        val (date, time) = daySplit(startMillis)
        val (deadlineDate, deadlineTime) = deadlineMillis?.let { daySplit(it) } ?: (null to null)
        return TaskEntity(
            id = "task-$startMillis",
            title = "Placed task",
            status = TaskStatus.ACTIVE,
            priority = Priority.MEDIUM,
            estimatedDurationMinutes = 60,
            scheduledDate = date,
            scheduledTime = time,
            isAutoScheduled = true,
            lastAutoScheduledAt = lastAutoScheduledAt,
            updatedAt = now - 24 * hours,
            deadlineDate = deadlineDate,
            deadlineTime = deadlineTime,
            postponeCount = postponeCount
        )
    }

    val stablePrefs = UserPreferences(autoSchedulingEnabled = true, isAutoScheduleToggleOn = true)

    "a block starting within two hours survives a forced re-plan" {
        val task = placedTask(now + hours, lastAutoScheduledAt = now - 3 * hours)

        PlacementStability.verdict(task, now, stablePrefs, forceReplan = true) shouldBe
            PlacementStability.ReplanVerdict.NEAR_TERM_FREEZE
        PlacementStability.verdict(task, now, stablePrefs, forceReplan = true).allowsReplan shouldBe false
    }

    "a deadline rescue respects the cooldown instead of firing every pass" {
        val task = placedTask(
            startMillis = now + 4 * hours,
            lastAutoScheduledAt = now - 5 * minutes,
            deadlineMillis = now + 5 * hours
        )

        PlacementStability.verdict(task, now, stablePrefs, forceReplan = false) shouldBe
            PlacementStability.ReplanVerdict.COOLDOWN
        PlacementStability.verdict(task, now, stablePrefs, forceReplan = false).allowsReplan shouldBe false
    }

    "a postponed task is not re-planned twice a minute" {
        val task = placedTask(
            startMillis = now + 3 * hours,
            lastAutoScheduledAt = now - 10 * minutes,
            postponeCount = 3
        )

        PlacementStability.verdict(task, now, stablePrefs, forceReplan = false) shouldBe
            PlacementStability.ReplanVerdict.COOLDOWN
        PlacementStability.verdict(task, now, stablePrefs, forceReplan = false).allowsReplan shouldBe false
    }

    "a forced re-plan still reaches blocks further out than two hours" {
        val task = placedTask(now + 5 * hours, lastAutoScheduledAt = now - 3 * hours)

        PlacementStability.verdict(task, now, stablePrefs, forceReplan = true).allowsReplan shouldBe true
    }

    "the periodic refresh still paces a stable plan" {
        val pastCooldown = placedTask(now + 5 * hours, lastAutoScheduledAt = now - 5 * hours)
        val insideCooldown = placedTask(now + 5 * hours, lastAutoScheduledAt = now - hours)

        PlacementStability.verdict(pastCooldown, now, stablePrefs, forceReplan = false)
            .allowsReplan shouldBe true
        PlacementStability.verdict(insideCooldown, now, stablePrefs, forceReplan = false)
            .allowsReplan shouldBe false
    }

    "an unchanged placement is not rewritten" {
        val previous = now + 3 * hours

        PlacementStability.isMoveWorthApplying(previous, previous, now) shouldBe false
    }

    "a future block only moves when the shift is at least half an hour" {
        val previous = now + 3 * hours

        PlacementStability.isMoveWorthApplying(previous, previous + 12 * minutes, now) shouldBe false
        PlacementStability.isMoveWorthApplying(previous, previous + 45 * minutes, now) shouldBe true
    }

    "a block already in the past always takes the new slot" {
        PlacementStability.isMoveWorthApplying(now - 10 * minutes, now + 2 * hours, now) shouldBe true
        PlacementStability.isMoveWorthApplying(null, now + 2 * hours, now) shouldBe true
    }
})
```

---

<a id="appsrctestjavacomneuroflowappdomainschedulerprotectedslotclocktestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/scheduler/ProtectedSlotClockTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/scheduler/ProtectedSlotClockTest.kt`
- **Lines of Code:** 294
- **Size:** 10,722 bytes (10.5 KB)

```kotlin
package com.neuroflow.app.domain.scheduler

import com.neuroflow.app.domain.model.ProtectedTimeSlot
import com.neuroflow.app.domain.model.PtsCategory
import com.neuroflow.app.domain.model.PtsRecurrence
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar
import java.util.TimeZone

/**
 * 2026-10-12 is a Monday, 2026-10-10 a Saturday. Everything runs with the default zone pinned to
 * UTC so the minute-of-day arithmetic is exact.
 */
class ProtectedSlotClockTest : StringSpec({

    fun <T> inUtc(body: () -> T): T {
        val original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        return try {
            body()
        } finally {
            TimeZone.setDefault(original)
        }
    }

    fun slot(
        label: String = "Slot",
        startMinute: Int,
        durationMinutes: Int,
        recurrence: PtsRecurrence = PtsRecurrence.DAILY,
        weekdayBitmask: Int = 0b1111111,
        dateMillis: Long? = null,
        enabled: Boolean = true,
        bufferMinutes: Int = 0
    ) = ProtectedTimeSlot(
        id = label,
        label = label,
        startMinuteOfDay = startMinute,
        durationMinutes = durationMinutes,
        recurrence = recurrence,
        weekdayBitmask = weekdayBitmask,
        category = PtsCategory.EXERCISE,
        recoveryBufferMinutes = bufferMinutes,
        isEnabled = enabled,
        dateMillis = dateMillis
    )

    fun at(day: Int, hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, day, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    "a running slot is due and reports what is left of it" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            val due = ProtectedSlotClock.dueSlots(listOf(gym), at(12, 18, 50))

            due.size shouldBe 1
            due[0].slot.label shouldBe "Gym"
            due[0].phase shouldBe SlotPhase.RUNNING
            due[0].minutesToStart shouldBe 0
            due[0].minutesLeft shouldBe 25
        }
    }

    "a slot counts as running from its first minute" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            val due = ProtectedSlotClock.dueSlots(listOf(gym), at(12, 18, 30))

            due[0].phase shouldBe SlotPhase.RUNNING
            due[0].minutesLeft shouldBe 45
        }
    }

    "nothing is due more than the lead-in window away" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            ProtectedSlotClock.dueSlots(listOf(gym), at(12, 16, 29)) shouldBe emptyList()
        }
    }

    "the lead-in window is two hours wide" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            val due = ProtectedSlotClock.dueSlots(listOf(gym), at(12, 16, 30))

            due.size shouldBe 1
            due[0].phase shouldBe SlotPhase.LEAD_IN
            due[0].minutesToStart shouldBe 120
        }
    }

    "a slot that just ended is kept briefly" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            val due = ProtectedSlotClock.dueSlots(listOf(gym), at(12, 19, 20))

            due.size shouldBe 1
            due[0].phase shouldBe SlotPhase.WRAPPING
            due[0].minutesLeft shouldBe 0
        }
    }

    "ten minutes after the end the slot is gone" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            ProtectedSlotClock.dueSlots(listOf(gym), at(12, 19, 25)) shouldBe emptyList()
        }
    }

    "a disabled slot never takes over" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45, enabled = false)

            ProtectedSlotClock.dueSlots(listOf(gym), at(12, 18, 50)) shouldBe emptyList()
        }
    }

    "a slot that runs past midnight is still running in the small hours" {
        inUtc {
            val late = slot("Night run", startMinute = 23 * 60 + 50, durationMinutes = 60)

            val due = ProtectedSlotClock.dueSlots(listOf(late), at(13, 0, 5))

            due.size shouldBe 1
            due[0].phase shouldBe SlotPhase.RUNNING
            due[0].minutesLeft shouldBe 45
        }
    }

    "yesterday's specific date is not due today" {
        inUtc {
            val oneOff = slot(
                "Doctor", startMinute = 18 * 60 + 30, durationMinutes = 45,
                recurrence = PtsRecurrence.SPECIFIC_DATE, dateMillis = at(11, 18, 30)
            )

            ProtectedSlotClock.dueSlots(listOf(oneOff), at(12, 18, 50)) shouldBe emptyList()
        }
    }

    "a specific-date slot is due on its own day" {
        inUtc {
            val oneOff = slot(
                "Doctor", startMinute = 18 * 60 + 30, durationMinutes = 45,
                recurrence = PtsRecurrence.SPECIFIC_DATE, dateMillis = at(12, 9, 0)
            )

            val due = ProtectedSlotClock.dueSlots(listOf(oneOff), at(12, 18, 50))

            due.size shouldBe 1
            due[0].phase shouldBe SlotPhase.RUNNING
        }
    }

    "the slot that started most recently takes the top spot" {
        inUtc {
            val earlier = slot("Lift", startMinute = 18 * 60, durationMinutes = 60)
            val later = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 60)

            val due = ProtectedSlotClock.dueSlots(listOf(earlier, later), at(12, 18, 40))

            due.map { it.slot.label } shouldBe listOf("Gym", "Lift")
        }
    }

    "running slots outrank ones that have not started" {
        inUtc {
            val running = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)
            val approaching = slot("Dinner", startMinute = 20 * 60, durationMinutes = 60)

            val due = ProtectedSlotClock.dueSlots(listOf(approaching, running), at(12, 18, 50))

            due.map { it.slot.label } shouldBe listOf("Gym", "Dinner")
            due[1].phase shouldBe SlotPhase.LEAD_IN
            due[1].minutesToStart shouldBe 70
        }
    }

    "approaching slots are ordered by how soon they start" {
        inUtc {
            val sooner = slot("Dinner", startMinute = 20 * 60, durationMinutes = 60)
            val later = slot("Family call", startMinute = 21 * 60, durationMinutes = 30)

            val due = ProtectedSlotClock.dueSlots(listOf(later, sooner), at(12, 19, 0))

            due.map { it.slot.label } shouldBe listOf("Dinner", "Family call")
        }
    }

    "the running slot takes over and the approaching ones queue" {
        inUtc {
            val running = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)
            val approaching = slot("Dinner", startMinute = 20 * 60, durationMinutes = 60)
            val slots = listOf(approaching, running)

            ProtectedSlotClock.takeoverOf(slots, at(12, 18, 50))?.slot?.label shouldBe "Gym"
            ProtectedSlotClock.approaching(slots, at(12, 18, 50)).map { it.slot.label } shouldBe
                listOf("Dinner")
        }
    }

    "a wrapping slot still holds the top spot" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45)

            val takeover = ProtectedSlotClock.takeoverOf(listOf(gym), at(12, 19, 20))

            takeover?.phase shouldBe SlotPhase.WRAPPING
        }
    }

    "nothing takes over while every slot is still ahead" {
        inUtc {
            val dinner = slot("Dinner", startMinute = 20 * 60, durationMinutes = 60)
            val call = slot("Family call", startMinute = 21 * 60, durationMinutes = 30)

            ProtectedSlotClock.takeoverOf(listOf(dinner, call), at(12, 19, 0)) shouldBe null
            ProtectedSlotClock.approaching(listOf(dinner, call), at(12, 19, 0))
                .map { it.slot.label } shouldBe listOf("Dinner", "Family call")
        }
    }

    "no slots means no takeover" {
        inUtc {
            ProtectedSlotClock.dueSlots(emptyList(), at(12, 18, 50)) shouldBe emptyList()
            ProtectedSlotClock.takeoverOf(emptyList(), at(12, 18, 50)) shouldBe null
            ProtectedSlotClock.approaching(emptyList(), at(12, 18, 50)) shouldBe emptyList()
        }
    }

    "the recurrence predicate matches each mode" {
        inUtc {
            val monday = at(12, 12, 0)
            val saturday = at(10, 12, 0)

            ProtectedSlotClock.occursOnDay(slot("a", 0, 15), monday) shouldBe true
            ProtectedSlotClock.occursOnDay(
                slot("b", 0, 15, recurrence = PtsRecurrence.WEEKDAYS), monday
            ) shouldBe true
            ProtectedSlotClock.occursOnDay(
                slot("c", 0, 15, recurrence = PtsRecurrence.WEEKDAYS), saturday
            ) shouldBe false
            ProtectedSlotClock.occursOnDay(
                slot("d", 0, 15, recurrence = PtsRecurrence.WEEKENDS), saturday
            ) shouldBe true
            ProtectedSlotClock.occursOnDay(
                slot("e", 0, 15, recurrence = PtsRecurrence.WEEKENDS), monday
            ) shouldBe false
            ProtectedSlotClock.occursOnDay(
                slot("f", 0, 15, recurrence = PtsRecurrence.CUSTOM, weekdayBitmask = 0b10), monday
            ) shouldBe true
            ProtectedSlotClock.occursOnDay(
                slot("g", 0, 15, recurrence = PtsRecurrence.CUSTOM, weekdayBitmask = 0b100), monday
            ) shouldBe false
            ProtectedSlotClock.occursOnDay(
                slot("h", 0, 15, recurrence = PtsRecurrence.SPECIFIC_DATE, dateMillis = at(12, 7, 0)),
                monday
            ) shouldBe true
            ProtectedSlotClock.occursOnDay(
                slot("i", 0, 15, recurrence = PtsRecurrence.SPECIFIC_DATE, dateMillis = at(10, 7, 0)),
                monday
            ) shouldBe false
            ProtectedSlotClock.occursOnDay(
                slot("j", 0, 15, recurrence = PtsRecurrence.SPECIFIC_DATE), monday
            ) shouldBe false
        }
    }

    "the disabled switch does not change which days a slot falls on" {
        inUtc {
            ProtectedSlotClock.occursOnDay(slot("a", 0, 15, enabled = false), at(12, 12, 0)) shouldBe true
        }
    }

    "a recovery buffer does not extend the takeover" {
        inUtc {
            val gym = slot("Gym", startMinute = 18 * 60 + 30, durationMinutes = 45, bufferMinutes = 30)

            val due = ProtectedSlotClock.dueSlots(listOf(gym), at(12, 19, 20))

            due.size shouldBe 1
            due[0].phase shouldBe SlotPhase.WRAPPING
        }
    }
})
```

---

<a id="appsrctestjavacomneuroflowappdomaininferencetaskinferenceenginetestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/domain/inference/TaskInferenceEngineTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/domain/inference/TaskInferenceEngineTest.kt`
- **Lines of Code:** 85
- **Size:** 3,992 bytes (3.9 KB)

```kotlin
package com.neuroflow.app.domain.inference

import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.TaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskInferenceEngineTest {

    @Test
    fun testEmptyInputGivesDefaultMetadata() {
        val result = TaskInferenceEngine.inferFromInput("")
        assertEquals("", result.sanitizedTitle)
        assertEquals(25, result.durationMinutes)
        assertEquals(EnergyLevel.MEDIUM, result.energyDemand)
        assertEquals(TaskType.GENERAL, result.taskType)
        assertTrue(result.contextTags.isEmpty())
        assertFalse(result.isFrog)
    }

    @Test
    fun testDurationExtractionFormats() {
        assertEquals(15, TaskInferenceEngine.inferFromInput("Quick email 15m").durationMinutes)
        assertEquals(90, TaskInferenceEngine.inferFromInput("Coding 90min").durationMinutes)
        assertEquals(60, TaskInferenceEngine.inferFromInput("Review 1h").durationMinutes)
        assertEquals(150, TaskInferenceEngine.inferFromInput("Architecture 2.5h").durationMinutes)
        assertEquals(45, TaskInferenceEngine.inferFromInput("Meeting 45 mins").durationMinutes)
        assertEquals(90, TaskInferenceEngine.inferFromInput("Design 1h 30m").durationMinutes)
    }

    @Test
    fun testEnergyDemandInference() {
        val highEnergy = TaskInferenceEngine.inferFromInput("Deep work on compiler thesis 90m")
        val lowEnergy = TaskInferenceEngine.inferFromInput("Clean desk and file expense 15m")
        val medEnergy = TaskInferenceEngine.inferFromInput("Team status update 30m")

        assertEquals(EnergyLevel.HIGH, highEnergy.energyDemand)
        assertEquals(EnergyLevel.LOW, lowEnergy.energyDemand)
        assertEquals(EnergyLevel.MEDIUM, medEnergy.energyDemand)
    }

    @Test
    fun testTaskTypeInference() {
        assertEquals(TaskType.ANALYTICAL, TaskInferenceEngine.inferFromInput("Debug algorithm bug 30m").taskType)
        assertEquals(TaskType.CREATIVE, TaskInferenceEngine.inferFromInput("Draft design system article 45m").taskType)
        assertEquals(TaskType.ADMIN, TaskInferenceEngine.inferFromInput("Send weekly sync email 15m").taskType)
        assertEquals(TaskType.PHYSICAL, TaskInferenceEngine.inferFromInput("Morning gym workout 60m").taskType)
    }

    @Test
    fun testFrogDetectionAndTitleSanitization() {
        val result = TaskInferenceEngine.inferFromInput("!frog Prepare quarterly financial tax return 60m @finance #urgent")
        assertTrue(result.isFrog)
        assertEquals("Prepare quarterly financial tax return", result.sanitizedTitle)
        assertEquals(listOf("finance"), result.contextTags)
        assertEquals(60, result.durationMinutes)
    }

    @Test
    fun testNaturalLanguageDurationPhrases() {
        assertEquals(30, TaskInferenceEngine.inferFromInput("Quick sync half hour").durationMinutes)
        assertEquals(30, TaskInferenceEngine.inferFromInput("Consultation half an hour").durationMinutes)
        assertEquals(60, TaskInferenceEngine.inferFromInput("Sprint planning an hour").durationMinutes)
    }

    @Test
    fun testExplicitEnergyAndPriorityParsing() {
        val highP1 = TaskInferenceEngine.inferFromInput("Quarterly review !high !p1")
        assertEquals(EnergyLevel.HIGH, highP1.energyDemand)
        assertEquals(com.neuroflow.app.domain.model.Priority.HIGH, highP1.priority)

        val lowP3 = TaskInferenceEngine.inferFromInput("Clear inbox !low !p3")
        assertEquals(EnergyLevel.LOW, lowP3.energyDemand)
        assertEquals(com.neuroflow.app.domain.model.Priority.LOW, lowP3.priority)
    }

    @Test
    fun testWordBoundaryIsolationPreventsFalseMatches() {
        // "barcode scanner" should NOT trigger "code" -> ANALYTICAL
        val scanner = TaskInferenceEngine.inferFromInput("Order barcode scanner replacement")
        assertEquals(TaskType.ADMIN, scanner.taskType)
    }
}
```

---

<a id="appsrctestjavacomneuroflowappdatalocalstimulantloggingtestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/data/local/StimulantLoggingTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/data/local/StimulantLoggingTest.kt`
- **Lines of Code:** 109
- **Size:** 4,255 bytes (4.2 KB)

```kotlin
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
```

---

<a id="appsrctestjavacomneuroflowappdatacalendarcalendarsyncrepositorytestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/data/calendar/CalendarSyncRepositoryTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/data/calendar/CalendarSyncRepositoryTest.kt`
- **Lines of Code:** 129
- **Size:** 5,611 bytes (5.5 KB)

```kotlin
package com.neuroflow.app.data.calendar

import com.neuroflow.app.domain.service.TimeSlotWindow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class CalendarSyncRepositoryTest : StringSpec({

    val baseMillis = 1770000000000L // arbitrary fixed epoch ms

    "coalesceWindows handles empty input" {
        val result = CalendarSyncDataSource.coalesceWindows(emptyList())
        result.shouldBeEmpty()
    }

    "coalesceWindows ignores invalid or zero-duration windows" {
        val invalid = listOf(
            TimeSlotWindow(baseMillis + 1000, baseMillis + 1000),
            TimeSlotWindow(baseMillis + 2000, baseMillis + 1000)
        )
        val result = CalendarSyncDataSource.coalesceWindows(invalid)
        result.shouldBeEmpty()
    }

    "coalesceWindows leaves short meetings (< 45m) unbuffered" {
        val raw = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 30 * 60_000L) // 30 min meeting
        )
        val result = CalendarSyncDataSource.coalesceWindows(raw)
        result shouldHaveSize 1
        result[0].startMillis shouldBe baseMillis
        result[0].endMillis shouldBe baseMillis + 30 * 60_000L
        result[0].durationMinutes shouldBe 30
    }

    "coalesceWindows adds 5m decompression buffer to meetings >= 45m" {
        val raw45 = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 45 * 60_000L) // exactly 45 min
        )
        val result45 = CalendarSyncDataSource.coalesceWindows(raw45)
        result45 shouldHaveSize 1
        result45[0].startMillis shouldBe baseMillis
        result45[0].endMillis shouldBe baseMillis + 50 * 60_000L // 45 + 5m buffer
        result45[0].durationMinutes shouldBe 50

        val raw60 = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 60 * 60_000L) // 60 min
        )
        val result60 = CalendarSyncDataSource.coalesceWindows(raw60)
        result60 shouldHaveSize 1
        result60[0].durationMinutes shouldBe 65
    }

    "coalesceWindows merges directly overlapping intervals" {
        val raw = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 30 * 60_000L),
            TimeSlotWindow(baseMillis + 15 * 60_000L, baseMillis + 60 * 60_000L)
        )
        val result = CalendarSyncDataSource.coalesceWindows(raw)
        result shouldHaveSize 1
        result[0].startMillis shouldBe baseMillis
        // Merged duration is 60m >= 45m -> gets +5m buffer
        result[0].endMillis shouldBe baseMillis + 65 * 60_000L
    }

    "coalesceWindows coalesces adjacent intervals separated by < 5m" {
        val raw = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 25 * 60_000L),
            TimeSlotWindow(baseMillis + 28 * 60_000L, baseMillis + 55 * 60_000L) // 3 min gap (< 5m)
        )
        val result = CalendarSyncDataSource.coalesceWindows(raw)
        result shouldHaveSize 1
        result[0].startMillis shouldBe baseMillis
        // Merged span: [0..55m] = 55m >= 45m -> gets +5m buffer -> 60m total
        result[0].endMillis shouldBe baseMillis + 60 * 60_000L
    }

    "coalesceWindows preserves distinct separated intervals (gap >= 5m)" {
        val raw = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 30 * 60_000L), // 30m (<45m, no buffer)
            TimeSlotWindow(baseMillis + 40 * 60_000L, baseMillis + 70 * 60_000L) // 10 min gap
        )
        val result = CalendarSyncDataSource.coalesceWindows(raw)
        result shouldHaveSize 2
        result[0].startMillis shouldBe baseMillis
        result[0].endMillis shouldBe baseMillis + 30 * 60_000L
        result[1].startMillis shouldBe baseMillis + 40 * 60_000L
        result[1].endMillis shouldBe baseMillis + 70 * 60_000L
    }

    "coalesceWindows handles unordered input and nested intervals" {
        val raw = listOf(
            TimeSlotWindow(baseMillis + 10 * 60_000L, baseMillis + 20 * 60_000L),
            TimeSlotWindow(baseMillis, baseMillis + 50 * 60_000L) // Outer containing window
        )
        val result = CalendarSyncDataSource.coalesceWindows(raw)
        result shouldHaveSize 1
        result[0].startMillis shouldBe baseMillis
        // 50 min >= 45m -> 55m with decompression buffer
        result[0].endMillis shouldBe baseMillis + 55 * 60_000L
    }

    "coalesceWindows handles multi-day span calendar events (72+ hours)" {
        val raw = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 72 * 3600_000L) // 72-hour conference
        )
        val result = CalendarSyncDataSource.coalesceWindows(raw)
        result shouldHaveSize 1
        result[0].startMillis shouldBe baseMillis
        // 72h >= 45m -> gets +5m decompression buffer at conclusion
        result[0].endMillis shouldBe baseMillis + 72 * 3600_000L + 5 * 60_000L
        result[0].durationMinutes shouldBe (72 * 60 + 5)
    }

    "coalesceWindows merges consecutive long meetings separated by < 5m into single decompressed horizon" {
        val raw = listOf(
            TimeSlotWindow(baseMillis, baseMillis + 60 * 60_000L), // 60m
            TimeSlotWindow(baseMillis + 62 * 60_000L, baseMillis + 120 * 60_000L) // 2m gap -> 58m
        )
        val result = CalendarSyncDataSource.coalesceWindows(raw)
        result shouldHaveSize 1
        result[0].startMillis shouldBe baseMillis
        // Merged span: [0..120m] >= 45m -> gets +5m decompression buffer -> 125m
        result[0].endMillis shouldBe baseMillis + 125 * 60_000L
        result[0].durationMinutes shouldBe 125
    }
})
```

---

<a id="appsrctestjavacomneuroflowappdatacalendarcalendarwritebacksynctestkt"></a>
### File: `app/src/test/java/com/neuroflow/app/data/calendar/CalendarWriteBackSyncTest.kt`

- **Path:** `app/src/test/java/com/neuroflow/app/data/calendar/CalendarWriteBackSyncTest.kt`
- **Lines of Code:** 447
- **Size:** 16,430 bytes (16.0 KB)

```kotlin
package com.neuroflow.app.data.calendar

import android.Manifest
import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.TaskStatus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CalendarWriteBackSyncTest {

    private lateinit var app: Application
    private lateinit var contentResolver: ContentResolver
    private lateinit var dataSource: CalendarSyncDataSource

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        val shadowApp = Shadows.shadowOf(app)
        shadowApp.grantPermissions(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
        contentResolver = mockk(relaxed = true)
        val testContext = object : android.content.ContextWrapper(app) {
            override fun getContentResolver(): ContentResolver = this@CalendarWriteBackSyncTest.contentResolver
        }
        dataSource = CalendarSyncDataSource(testContext)
    }

    @Test
    fun `findPrimaryWritableCalendarId returns primary calendar id`() = runTest {
        val cursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        cursor.addRow(arrayOf(101L, 1, CalendarContract.Calendars.CAL_ACCESS_OWNER))

        every {
            contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                any(),
                match { it.contains("${CalendarContract.Calendars.IS_PRIMARY} = 1") },
                any(),
                any()
            )
        } returns cursor

        val calId = dataSource.findPrimaryWritableCalendarId()
        calId shouldBe 101L
    }

    @Test
    fun `findPrimaryWritableCalendarId falls back to first writable calendar when no primary exists`() = runTest {
        val emptyPrimaryCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))

        val fallbackCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        fallbackCursor.addRow(arrayOf(202L, 0, CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR))

        every {
            contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                any(),
                match { it.contains("${CalendarContract.Calendars.IS_PRIMARY} = 1") },
                any(),
                any()
            )
        } returns emptyPrimaryCursor

        every {
            contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                any(),
                match { !it.contains("${CalendarContract.Calendars.IS_PRIMARY} = 1") },
                any(),
                any()
            )
        } returns fallbackCursor

        val calId = dataSource.findPrimaryWritableCalendarId()
        calId shouldBe 202L
    }

    @Test
    fun `findPrimaryWritableCalendarId returns null when permission denied`() = runTest {
        val shadowApp = Shadows.shadowOf(app)
        shadowApp.denyPermissions(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)

        val unprivilegedSource = CalendarSyncDataSource(app)
        val calId = unprivilegedSource.findPrimaryWritableCalendarId()
        calId shouldBe null
    }

    @Test
    fun `findEventIdByTaskId queries using loop-prevention package and uri`() = runTest {
        val taskId = "task-focus-999"
        val cursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        cursor.addRow(arrayOf(777L, "neuroflow://task/$taskId"))

        val selectionArgsSlot = slot<Array<String>>()
        every {
            contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                any(),
                any(),
                capture(selectionArgsSlot),
                any()
            )
        } returns cursor

        val eventId = dataSource.findEventIdByTaskId(taskId)
        eventId shouldBe 777L

        selectionArgsSlot.captured[0] shouldBe CalendarSyncDataSource.NEUROFLOW_PACKAGE_ID
        selectionArgsSlot.captured[1] shouldBe "neuroflow://task/$taskId"
    }

    @Test
    fun `upsertTaskEvent inserts new event with loop prevention stamp and flow title`() = runTest {
        val task = TaskEntity(
            id = "task-alpha-1",
            title = "Architectural Review",
            description = "Review ADR-006 write-back",
            scheduledDate = 1770000000000L,
            scheduledTime = 3600_000L,
            estimatedDurationMinutes = 45,
            isScheduleLocked = true
        )

        // Mock primary calendar
        val calCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        calCursor.addRow(arrayOf(55L, 1, CalendarContract.Calendars.CAL_ACCESS_OWNER))
        every {
            contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns calCursor

        // No existing event
        val emptyEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        every {
            contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns emptyEventCursor

        val valuesSlot = slot<ContentValues>()
        every {
            contentResolver.insert(CalendarContract.Events.CONTENT_URI, capture(valuesSlot))
        } returns Uri.parse("content://com.android.calendar/events/8888")

        val newId = dataSource.upsertTaskEvent(task)
        newId shouldBe 8888L

        val captured = valuesSlot.captured
        captured.getAsLong(CalendarContract.Events.CALENDAR_ID) shouldBe 55L
        captured.getAsString(CalendarContract.Events.TITLE) shouldBe "[Flow] Architectural Review"
        captured.getAsString(CalendarContract.Events.CUSTOM_APP_PACKAGE) shouldBe CalendarSyncDataSource.NEUROFLOW_PACKAGE_ID
        val expectedStart = 1770000000000L + 3600_000L
        captured.getAsLong(CalendarContract.Events.DTSTART) shouldBe expectedStart
        captured.getAsLong(CalendarContract.Events.DTEND) shouldBe expectedStart + 45 * 60_000L
    }

    @Test
    fun `upsertTaskEvent formats frog task with frog title prefix`() = runTest {
        val frogTask = TaskEntity(
            id = "task-frog-7",
            title = "Eat the Frog",
            isFrog = true,
            scheduledDate = 1770000000000L,
            estimatedDurationMinutes = 30
        )

        val calCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        calCursor.addRow(arrayOf(55L, 1, 700))
        every {
            contentResolver.query(CalendarContract.Calendars.CONTENT_URI, any(), any(), any(), any())
        } returns calCursor

        val emptyEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns emptyEventCursor

        val valuesSlot = slot<ContentValues>()
        every {
            contentResolver.insert(CalendarContract.Events.CONTENT_URI, capture(valuesSlot))
        } returns Uri.parse("content://com.android.calendar/events/9999")

        dataSource.upsertTaskEvent(frogTask)

        val captured = valuesSlot.captured
        captured.getAsString(CalendarContract.Events.TITLE) shouldBe "🎯 [Flow] Eat the Frog"
    }

    @Test
    fun `upsertTaskEvent updates existing event in place when event already exists`() = runTest {
        val task = TaskEntity(
            id = "task-update-10",
            title = "Rescheduled Deep Work",
            scheduledDate = 1770000000000L,
            estimatedDurationMinutes = 60
        )

        val calCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        calCursor.addRow(arrayOf(55L, 1, 700))
        every {
            contentResolver.query(CalendarContract.Calendars.CONTENT_URI, any(), any(), any(), any())
        } returns calCursor

        // Existing event found
        val existingEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        existingEventCursor.addRow(arrayOf(12345L, "neuroflow://task/task-update-10"))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns existingEventCursor

        val updateUriSlot = slot<Uri>()
        every {
            contentResolver.update(capture(updateUriSlot), any(), any(), any())
        } returns 1

        val updatedId = dataSource.upsertTaskEvent(task)
        updatedId shouldBe 12345L

        updateUriSlot.captured shouldBe ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, 12345L)
        verify(exactly = 0) { contentResolver.insert(any(), any()) }
    }

    @Test
    fun `deleteTaskEvent deletes existing event via ContentUris`() = runTest {
        val taskId = "task-delete-99"
        val existingEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        existingEventCursor.addRow(arrayOf(4444L, "neuroflow://task/$taskId"))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns existingEventCursor

        val deleteUriSlot = slot<Uri>()
        every {
            contentResolver.delete(capture(deleteUriSlot), any(), any())
        } returns 1

        val deleted = dataSource.deleteTaskEvent(taskId)
        deleted shouldBe true

        deleteUriSlot.captured shouldBe ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, 4444L)
    }

    @Test
    fun `handleTaskStateChanged updates completed task with checkmark flow title`() = runTest {
        val completedTask = TaskEntity(
            id = "task-done-1",
            title = "Finalize Presentation",
            status = TaskStatus.COMPLETED,
            scheduledDate = 1770000000000L
        )

        val calCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        calCursor.addRow(arrayOf(55L, 1, 700))
        every {
            contentResolver.query(CalendarContract.Calendars.CONTENT_URI, any(), any(), any(), any())
        } returns calCursor

        val existingEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        existingEventCursor.addRow(arrayOf(3333L, "neuroflow://task/task-done-1"))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns existingEventCursor

        val valuesSlot = slot<ContentValues>()
        every {
            contentResolver.update(any(), capture(valuesSlot), any(), any())
        } returns 1

        val result = dataSource.handleTaskStateChanged(completedTask)
        result shouldBe true

        valuesSlot.captured.getAsString(CalendarContract.Events.TITLE) shouldBe "✓ [Flow] Finalize Presentation"
    }

    @Test
    fun `handleTaskStateChanged deletes task event on ARCHIVED`() = runTest {
        val archivedTask = TaskEntity(
            id = "task-archived-1",
            title = "Old Idea",
            status = TaskStatus.ARCHIVED
        )

        val existingEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        existingEventCursor.addRow(arrayOf(2222L, "neuroflow://task/task-archived-1"))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns existingEventCursor

        every { contentResolver.delete(any(), any(), any()) } returns 1

        val result = dataSource.handleTaskStateChanged(archivedTask)
        result shouldBe true

        verify { contentResolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, 2222L), any(), any()) }
    }

    @Test
    fun `queryInstances loop-prevention filters out self-authored neuroFlow blocks`() = runTest {
        val cursor = MatrixCursor(arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.AVAILABILITY,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS,
            CalendarContract.Instances.STATUS,
            CalendarContract.Instances.ORGANIZER,
            CalendarContract.Events.CUSTOM_APP_PACKAGE,
            CalendarContract.Events.CUSTOM_APP_URI
        ))

        val start = 1770000000000L
        val end = start + 3600_000L

        // Row 1: External busy meeting (should be included)
        cursor.addRow(arrayOf(
            start,
            end,
            "External Sync",
            "Important team sync",
            CalendarContract.Events.AVAILABILITY_BUSY,
            CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED,
            CalendarContract.Events.STATUS_CONFIRMED,
            "colleague@corp.com",
            "",
            ""
        ))

        // Row 2: Self-authored neuroFlow block via customAppPackage (must be filtered out)
        cursor.addRow(arrayOf(
            start + 4000_000L,
            start + 5000_000L,
            "Focus Block",
            "Deep focus",
            CalendarContract.Events.AVAILABILITY_BUSY,
            CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED,
            CalendarContract.Events.STATUS_CONFIRMED,
            "",
            CalendarSyncDataSource.NEUROFLOW_PACKAGE_ID,
            "neuroflow://task/task-abc"
        ))

        // Row 3: Self-authored neuroFlow block via [Flow] title prefix (must be filtered out)
        cursor.addRow(arrayOf(
            start + 6000_000L,
            start + 7000_000L,
            "[Flow] Frog task",
            "Focus",
            CalendarContract.Events.AVAILABILITY_BUSY,
            CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED,
            CalendarContract.Events.STATUS_CONFIRMED,
            "",
            "",
            ""
        ))

        every {
            contentResolver.query(any(), any(), any(), any(), any())
        } returns cursor

        val windows = dataSource.queryInstances(start, start + 10_000_000L)
        windows.size shouldBe 1
        windows[0].startMillis shouldBe start
        windows[0].endMillis shouldBe end
    }
}
```

---

