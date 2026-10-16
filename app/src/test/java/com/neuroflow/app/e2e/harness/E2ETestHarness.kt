package com.neuroflow.app.e2e.harness

import com.neuroflow.app.data.local.Converters
import com.neuroflow.app.data.local.entity.ContractOutcome
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.engine.PeakEnergyEngine
import com.neuroflow.app.domain.engine.EnergyScoreEngine
import com.neuroflow.app.domain.engine.TaskScoringEngine
import com.neuroflow.app.domain.model.*
import java.util.Locale
import kotlin.math.*

/**
 * Authoritative E2E Test Harness providing opaque-box evaluation, reference oracles,
 * contract validators, and reflection bridges for neuroFlow modernization features F1-F16.
 */
object E2ETestHarness {

    // ─────────────────────────────────────────────────────────────────────────────
    // F2: Borbély Energy Baseline Continuous Pre-Peak Calculation Oracle
    // ─────────────────────────────────────────────────────────────────────────────
    object BorbelyOracle {
        /**
         * Authoritative reference calculation for pre-peak energy.
         * For a user waking before their circadian peak, energy must not drop to 0.
         * Instead, baseline usable energy rises smoothly towards peak.
         */
        fun calculateContinuousCurrentValue(
            nowMinute: Int,
            peakMinute: Int,
            peakValue: Int,
            wakeMinute: Int = 420 // 7:00 AM
        ): Int {
            return if (nowMinute >= peakMinute) {
                val minutesSincePeak = nowMinute - peakMinute
                val decay = (minutesSincePeak * (peakValue.toFloat() / 960f)).roundToInt()
                (peakValue - decay).coerceAtLeast(0)
            } else {
                // Pre-peak window: baseline rises from morning wake or maintains continuous floor
                if (nowMinute >= wakeMinute) {
                    val prePeakDuration = (peakMinute - wakeMinute).coerceAtLeast(1)
                    val progress = (nowMinute - wakeMinute).toFloat() / prePeakDuration.toFloat()
                    val morningFloor = (peakValue * 0.70f).roundToInt()
                    (morningFloor + progress * (peakValue - morningFloor)).roundToInt().coerceIn(0, peakValue)
                } else {
                    // Pre-wake nocturnal period
                    (peakValue * 0.60f).roundToInt()
                }
            }
        }

        fun isContinuousPrePeak(nowMinute: Int, peakMinute: Int, currentValue: Int, peakValue: Int): Boolean {
            if (nowMinute < peakMinute && peakValue > 0) {
                // In pre-peak hours, currentValue must be strictly positive (never zeroed out)
                return currentValue > 0
            }
            return true
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // F3: Circadian Midnight Wrap Oracle
    // ─────────────────────────────────────────────────────────────────────────────
    object MidnightWrapOracle {
        /**
         * Calculates cosine-window amplitude supporting smooth midnight wrap across 24:00 (1440m).
         */
        fun calculateWrappedPeakWindow(
            minute: Int,
            startMinute: Int,
            durationMinutes: Int,
            amplitude: Float
        ): Float {
            if (durationMinutes <= 0) return 0f
            val normMinute = ((minute % 1440) + 1440) % 1440
            val normStart = ((startMinute % 1440) + 1440) % 1440
            val endMinute = normStart + durationMinutes

            val inWindow = if (endMinute <= 1440) {
                normMinute in normStart until endMinute
            } else {
                // Window wraps across midnight
                normMinute >= normStart || normMinute < (endMinute - 1440)
            }

            if (!inWindow) return 0f

            val offset = if (normMinute >= normStart) {
                normMinute - normStart
            } else {
                normMinute + 1440 - normStart
            }

            val phase = offset.toFloat() / durationMinutes.toFloat()
            val raw = 0.5f * (1f - cos((2.0 * Math.PI * phase).toFloat()))
            return (amplitude * raw).coerceAtLeast(0f)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // F4: Statistical Confidence Decoupling Oracle
    // ─────────────────────────────────────────────────────────────────────────────
    object ConfidenceDecouplingOracle {
        /**
         * Decouples confidence factor from energy magnitude.
         * Statistical confidence factor serves as estimation metadata and does not zero peakScore.
         */
        fun calculateDecoupledEnergy(
            reservoirFactor: Float,
            circadianFactor: Float,
            sleepPressureRatio: Float,
            confidenceFactor: Float
        ): Pair<Float, Float> {
            val percentScale = 100f
            // Magnitude does not multiply by confidenceFactor
            val peakScore = (percentScale * reservoirFactor * circadianFactor).coerceIn(0f, percentScale)
            val fatiguePenalty = (percentScale * sleepPressureRatio).coerceIn(0f, percentScale)
            val rawEnergy = (peakScore - fatiguePenalty).coerceIn(-percentScale, percentScale)
            val usableEnergy = ((rawEnergy + percentScale) / 2f).coerceIn(0f, percentScale)
            return Pair(rawEnergy, usableEnergy)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // F5: Live Energy State Scoring Oracle
    // ─────────────────────────────────────────────────────────────────────────────
    object LiveEnergyScoringOracle {
        fun calculateLiveEnergyBonus(
            taskEnergyLevel: EnergyLevel,
            liveEnergy: E2ELiveEnergyContext
        ): Float {
            val usable = liveEnergy.usableEnergy
            return when {
                usable >= 70f -> when (taskEnergyLevel) {
                    EnergyLevel.HIGH -> 70f * (usable / 100f)
                    EnergyLevel.MEDIUM -> 20f
                    EnergyLevel.LOW -> -25f
                }
                usable <= 35f -> when (taskEnergyLevel) {
                    EnergyLevel.LOW -> 70f * ((100f - usable) / 100f)
                    EnergyLevel.MEDIUM -> 15f
                    EnergyLevel.HIGH -> -40f
                }
                else -> when (taskEnergyLevel) {
                    EnergyLevel.MEDIUM -> 30f
                    EnergyLevel.HIGH -> 10f
                    EnergyLevel.LOW -> 10f
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // F6: Room Enum Hardening Safe Fallback Evaluator
    // ─────────────────────────────────────────────────────────────────────────────
    object EnumHardeningEvaluator {
        fun safeQuadrant(raw: String?): Quadrant {
            return try {
                if (raw.isNullOrBlank()) Quadrant.SCHEDULE else Quadrant.valueOf(raw)
            } catch (_: Exception) {
                Quadrant.SCHEDULE
            }
        }

        fun safePriority(raw: String?): Priority {
            return try {
                if (raw.isNullOrBlank()) Priority.LOW else Priority.valueOf(raw)
            } catch (_: Exception) {
                Priority.LOW
            }
        }

        fun safeTaskStatus(raw: String?): TaskStatus {
            return try {
                if (raw.isNullOrBlank()) TaskStatus.ACTIVE else TaskStatus.valueOf(raw)
            } catch (_: Exception) {
                TaskStatus.ACTIVE
            }
        }

        fun safeRecurrence(raw: String?): Recurrence {
            return try {
                if (raw.isNullOrBlank()) Recurrence.NONE else Recurrence.valueOf(raw)
            } catch (_: Exception) {
                Recurrence.NONE
            }
        }

        fun safeEnergyLevel(raw: String?): EnergyLevel {
            return try {
                if (raw.isNullOrBlank()) EnergyLevel.MEDIUM else EnergyLevel.valueOf(raw)
            } catch (_: Exception) {
                EnergyLevel.MEDIUM
            }
        }

        fun safeContractOutcome(raw: String?): ContractOutcome? {
            return try {
                if (raw.isNullOrBlank()) null else ContractOutcome.valueOf(raw)
            } catch (_: Exception) {
                null
            }
        }

        fun safeRewardTier(raw: String?): RewardTier {
            return try {
                if (raw.isNullOrBlank()) RewardTier.NONE else RewardTier.valueOf(raw)
            } catch (_: Exception) {
                RewardTier.NONE
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // F9: Task Inference Engine Specification & Reference Parser
    // ─────────────────────────────────────────────────────────────────────────────
    object TaskInferenceOracle {
        private val DURATION_REGEX = Regex("""(?i)\b(?:(\d+)\s*h(?:ours?)?)?\s*(?:(\d+)\s*(?:m|min|mins|minutes?))\b|\b(\d+(?:\.\d+)?)\s*h(?:ours?)?\b""")
        private val TAG_REGEX = Regex("""[@#]([a-zA-Z0-9_\-]+)""")
        private val FROG_REGEX = Regex("""(?i)(?:^|\s)(?:!frog|frog:|\b(?:eat\s+the\s+frog|high\s+impact)\b)""")

        fun infer(rawInput: String): E2EInferredTaskMetadata {
            val text = rawInput.trim()
            if (text.isEmpty()) {
                return E2EInferredTaskMetadata(
                    sanitizedTitle = "",
                    durationMinutes = 25,
                    energyDemand = EnergyLevel.MEDIUM,
                    taskType = TaskType.ANALYTICAL,
                    contextTags = emptyList(),
                    isFrog = false
                )
            }

            // Duration extraction
            var durationMinutes = 25
            val durationMatch = DURATION_REGEX.find(text)
            if (durationMatch != null) {
                val hoursPart = durationMatch.groups[1]?.value?.toIntOrNull() ?: 0
                val minsPart = durationMatch.groups[2]?.value?.toIntOrNull() ?: 0
                val decimalHours = durationMatch.groups[3]?.value?.toFloatOrNull()
                durationMinutes = when {
                    decimalHours != null -> (decimalHours * 60).roundToInt()
                    hoursPart > 0 || minsPart > 0 -> hoursPart * 60 + minsPart
                    else -> 25
                }
            }

            // Context tags extraction
            val tags = TAG_REGEX.findAll(text).map { it.groupValues[1].lowercase(Locale.ROOT) }.distinct().toList()

            // Frog priority detection
            val isFrog = FROG_REGEX.containsMatchIn(text)

            // Energy demand heuristic
            val lower = text.lowercase(Locale.ROOT)
            val energyDemand = when {
                lower.contains("deep") || lower.contains("deep work") || lower.contains("coding") || lower.contains("write report") || lower.contains("architect") ||
                lower.contains("study") || lower.contains("exam") || lower.contains("complex") || lower.contains("thesis") -> EnergyLevel.HIGH
                lower.contains("quick call") || lower.contains("clean desk") || lower.contains("water plants") ||
                lower.contains("tidy") || lower.contains("file expense") || lower.contains("errand") -> EnergyLevel.LOW
                else -> EnergyLevel.MEDIUM
            }

            // Task type heuristic
            val taskType = when {
                lower.contains("code") || lower.contains("coding") || lower.contains("debug") ||
                lower.contains("math") || lower.contains("analysis") || lower.contains("algorithm") -> TaskType.ANALYTICAL
                lower.contains("design") || lower.contains("write") || lower.contains("draft") ||
                lower.contains("brainstorm") || lower.contains("compose") -> TaskType.CREATIVE
                lower.contains("call") || lower.contains("meet") || lower.contains("email") ||
                lower.contains("sync") || lower.contains("discuss") -> TaskType.ADMIN
                lower.contains("errand") || lower.contains("admin") || lower.contains("organize") ||
                lower.contains("clean") || lower.contains("invoice") -> TaskType.ADMIN
                lower.contains("gym") || lower.contains("run") || lower.contains("walk") ||
                lower.contains("exercise") -> TaskType.PHYSICAL
                else -> TaskType.ANALYTICAL
            }

            // Title sanitization
            var cleanTitle = text
                .replace(FROG_REGEX, " ")
                .replace(DURATION_REGEX, " ")
                .replace(Regex("""[@#][a-zA-Z0-9_\-]+"""), " ")
                .replace(Regex("""\s+"""), " ")
                .trim()
            if (cleanTitle.isEmpty()) {
                cleanTitle = text.take(30)
            }

            return E2EInferredTaskMetadata(
                sanitizedTitle = cleanTitle,
                durationMinutes = durationMinutes.coerceAtLeast(5),
                energyDemand = energyDemand,
                taskType = taskType,
                contextTags = tags,
                isFrog = isFrog
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // F11: Contrast Ratio and Color Token Evaluator
    // ─────────────────────────────────────────────────────────────────────────────
    object ThemeContrastEvaluator {
        /**
         * Calculates WCAG relative luminance from an ARGB Int.
         */
        fun relativeLuminance(colorArgb: Int): Double {
            val r = ((colorArgb shr 16) and 0xFF) / 255.0
            val g = ((colorArgb shr 8) and 0xFF) / 255.0
            val b = (colorArgb and 0xFF) / 255.0

            val rLin = if (r <= 0.03928) r / 12.92 else ((r + 0.055) / 1.055).pow(2.4)
            val gLin = if (g <= 0.03928) g / 12.92 else ((g + 0.055) / 1.055).pow(2.4)
            val bLin = if (b <= 0.03928) b / 12.92 else ((b + 0.055) / 1.055).pow(2.4)

            return 0.2126 * rLin + 0.7152 * gLin + 0.0722 * bLin
        }

        /**
         * Computes WCAG contrast ratio between two ARGB colors. Must be >= 4.5:1 for AA normal text.
         */
        fun contrastRatio(foregroundArgb: Int, backgroundArgb: Int): Double {
            val l1 = relativeLuminance(foregroundArgb)
            val l2 = relativeLuminance(backgroundArgb)
            val lighter = max(l1, l2)
            val darker = min(l1, l2)
            return (lighter + 0.05) / (darker + 0.05)
        }

        fun isAmoledBlack(colorArgb: Int): Boolean {
            return (colorArgb and 0x00FFFFFF) == 0x00000000
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // F14: Focus Mode Supportive Tiers & Copy Evaluator
    // ─────────────────────────────────────────────────────────────────────────────
    object FocusTierEvaluator {
        private val HOSTILE_WORDS = listOf(
            "dumber", "stupid", "lamao", "lol", "become dumber", "you are still stupid"
        )

        fun isSupportiveCopy(text: String): Boolean {
            val lower = text.lowercase(Locale.ROOT)
            return HOSTILE_WORDS.none { lower.contains(it) }
        }

        fun getTierIntervention(tier: E2EFocusInterventionTier): E2EFocusIntervention {
            return when (tier) {
                E2EFocusInterventionTier.ZEN -> E2EFocusIntervention(
                    tier = tier,
                    heading = "Take a Mindful Breath",
                    message = "Notice the urge to switch. Take a slow inhale before deciding.",
                    primaryActionLabel = "Return to Focus",
                    dismissActionLabel = "Pause Mindfully"
                )
                E2EFocusInterventionTier.SHIELD -> E2EFocusIntervention(
                    tier = tier,
                    heading = "Reflective Pause",
                    message = "You are in a protected focus block. Is this switch truly aligned with your current goal?",
                    primaryActionLabel = "Stay Focused",
                    dismissActionLabel = "Proceed Consciously"
                )
                E2EFocusInterventionTier.FORTRESS -> E2EFocusIntervention(
                    tier = tier,
                    heading = "Deep Work Sanctuary Active",
                    message = "Strict boundary engaged for high-leverage execution. Take a 10s cooldown before opening emergency bypass.",
                    primaryActionLabel = "Protect Deep Work",
                    dismissActionLabel = "Emergency Exit (Preserves Streak)"
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // F15: Modular Auto-Scheduler & Deferral Oracle
    // ─────────────────────────────────────────────────────────────────────────────
    object AutoSchedulerOracle {
        fun planSchedule(
            tasks: List<TaskEntity>,
            availableSlotsMinutes: Int,
            dailyCognitiveCapacityMinutes: Int
        ): E2ESchedulingReport {
            var usedMinutes = 0
            val placedRationales = mutableListOf<E2EPlacementRationale>()
            val deferrals = mutableListOf<E2EDeferralRationale>()

            for (task in tasks) {
                val duration = if (task.estimatedDurationMinutes > 0) task.estimatedDurationMinutes else 25
                if (usedMinutes + duration > dailyCognitiveCapacityMinutes) {
                    deferrals.add(
                        E2EDeferralRationale(
                            taskId = task.id,
                            reasonCode = E2EDeferralReasonCode.COGNITIVE_BUDGET_EXCEEDED,
                            explanation = "Daily cognitive limit of ${dailyCognitiveCapacityMinutes}m reached.",
                            actionableSuggestion = "Schedule for tomorrow's morning peak window."
                        )
                    )
                } else if (duration > availableSlotsMinutes) {
                    deferrals.add(
                        E2EDeferralRationale(
                            taskId = task.id,
                            reasonCode = E2EDeferralReasonCode.INSUFFICIENT_TIME,
                            explanation = "Task duration (${duration}m) exceeds available slot (${availableSlotsMinutes}m).",
                            actionableSuggestion = "Break this task into smaller 25m focus blocks."
                        )
                    )
                } else {
                    val slotStart = System.currentTimeMillis() + usedMinutes.toLong() * 60_000L
                    val slotEnd = slotStart + duration.toLong() * 60_000L
                    placedRationales.add(
                        E2EPlacementRationale(
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

            val totalTasks = tasks.size
            check(placedRationales.size + deferrals.size == totalTasks) {
                "Invariant violated: Total tasks must equal placed plus deferred (no silent drops allowed)."
            }

            return E2ESchedulingReport(
                placedCount = placedRationales.size,
                deferredCount = deferrals.size,
                placementRationales = placedRationales,
                deferrals = deferrals,
                dailyCognitiveLoadUtilized = usedMinutes.toFloat() / dailyCognitiveCapacityMinutes.toFloat()
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // F16: Behavioral Pattern Insights Oracle
    // ─────────────────────────────────────────────────────────────────────────────
    object BehavioralInsightsOracle {
        /**
         * Bayesian shrinkage formula for duration estimation calibration.
         * shrinkageFactor = (k0 * 1.0 + n * observedRatio) / (k0 + n)
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
}
