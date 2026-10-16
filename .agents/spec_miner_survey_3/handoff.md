# Specification Mining Report: Focus Mode (R4) & Auto-Scheduling Engine with Behavioral Insights (R5)

**Agent**: `spec_miner_survey_3`  
**Working Directory**: `/home/takian/Project/PROCUS/.agents/spec_miner_survey_3`  
**Date**: 2026-10-15T21:21:00Z  
**Target Milestone**: Survey Phase / Modernization Specification for R4 & R5  

---

## 1. Observation

### 1.1 Focus Mode & HyperFocus (R4)
1. **FocusScreen Architecture**:
   - Location: `app/src/main/java/com/neuroflow/app/presentation/focus/FocusScreen.kt` (1,218 lines)
   - Backed by: `FocusViewModel.kt` (652 lines), `FocusSessionManager.kt`, `FocusCompletionManager.kt`, `FocusWoopManager.kt`.
   - **Contrast-breaking colors & Hardcoded Theme Violations**:
     - `FocusScreen.kt:409`: `CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))` (hardcoded light pastel blue).
     - `FocusScreen.kt:413, 415, 416`: hardcoded text colors `Color(0xFF1565C0)`, `Color(0xFF0D47A1)`.
     - `FocusScreen.kt:435`: `CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))` (hardcoded light cream).
     - `FocusScreen.kt:537`: `CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))` (hardcoded light peach).
     - `FocusScreen.kt:572`: `CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))` (hardcoded light pink).
     - `FocusScreen.kt:635, 662, 833`: `deadlineCardColor`, `scheduledCardColor`, `pomodoroCardColor` read from `NeuroFlowColors.kt:34-36` which defines:
       - `val DeadlineCard = Color(0xFFFFEBEE)`
       - `val ScheduledCard = Color(0xFFE3F2FD)`
       - `val PomodoroCard = Color(0xFFF3E5F5)`
     - `FocusScreen.kt:946`: Done button container color hardcoded to `Color.Black` (`containerColor = Color.Black, contentColor = Color.White`), rendering it invisible/jarring in Dark/AMOLED modes.
   - **Timer and Pomodoro Logic Decoupling**:
     - `FocusViewModel.kt:449-464`: `startPomodoro()` runs an independent countdown timer job (`pomodoroJob`) which increments `pomodoroSeconds` up to `pomodoroTotal`. It does NOT start tracking time in `TimeSessionEntity`.
     - `FocusViewModel.kt:305-320`: `startTracking()` launches `sessionManager.startSession(taskId, now)` and starts `timerJob`. It does NOT start or synchronize with Pomodoro.
     - There is no custom Pomodoro duration selector in `FocusScreen.kt`; it strictly pulls `prefs.defaultPomodoroMinutes * 60` from `UserPreferences`.
   - **WOOP & Implementation Intentions**:
     - `FocusScreen.kt:404-429`: If WOOP exists, it renders an uncollapsible `Card(containerColor = Color(0xFFE3F2FD))` taking up ~120dp.
     - `FocusScreen.kt:504-528`: Implementation intentions (`task.ifThenPlan`) are rendered in a static uncollapsible column of checkboxes.
     - `WoopEngine.kt:7-22`: Implements only basic check `shouldShowPrompt`, `generateIfThenPlan("If $obstacle, then I will ")`, and `dreadedTaskInsight`.
   - **Affordance Difficulty Feedback**:
     - `AffordanceRatingSheet.kt:48-57`: Slider from `-2f` to `+2f` ("Worse than expected" to "Better than expected").
     - `FocusWoopManager.kt:76-84`: Saves rating to `task.affectiveForecastError`, calls `syncEnjoymentForecastBias()`.
     - `TaskScoringEngine.kt:738-739`: Corrects `task.enjoymentScore` with `AnalyticsEngine.EnjoymentCalibration(prefs.enjoymentForecastBias).forecast(task.enjoymentScore)`.

### 1.2 Distraction Interventions & Hostile Blocking Copy
1. **Punitive / Hostile Copy in `BlockingOverlayScreen.kt`**:
   - `BlockingOverlayScreen.kt:114`: `"Lol. Don't say, (you lost your motivation)."`
   - `BlockingOverlayScreen.kt:130`: `"Is this really worth it? Do you really want to become dumber?"`
   - `BlockingOverlayScreen.kt:131`: `"Look! who we have here!! Lamao,lol.. I thought ,you have finally became smarter. Ig, you are still stupid"`
   - `BlockingOverlayScreen.kt:654`: `"⚠️ WARNING: If you use this emergency bypass, you will lose your next intermediate rewards and must complete ALL daily tasks to earn any further unlocks."`
2. **Punitive Reward Cancellation**:
   - `HyperFocusManager.kt:414-415`: Calling `triggerEmergencyBypass()` sets `emergencyUsed = true` and `currentTier = RewardTier.NONE`.
   - `RewardEngine.kt:75`: `if (emergencyUsed) return RewardTier.NONE // Skips intermediate rewards if emergency used`.
   - `RewardEngine.kt:90`: `if (emergencyUsed) return t - completed // Must complete all remaining tasks`.
3. **Rigid Friction**:
   - `HyperFocusActivationSheet.kt:334`: Requires user to explicitly type `"FOCUS"` in uppercase to start a session.

### 1.3 Auto-Scheduling Pipeline & Silent Task Drop Root Causes (R5)
1. **Code Locations**:
   - `AutoSchedulingEngine.kt` (2,439 lines) in `com.neuroflow.app.domain.scheduler`.
   - `AutoSchedulingContracts.kt` (63 lines): defines `AutoScheduleRejectionReason`.
   - `ScheduleAutoTasksWorker.kt` (525 lines): WorkManager background sync.
2. **Silent Drop Mechanisms**:
   - **Interface Return Contract**:
     `AutoSchedulingEngine.kt:164`: `suspend fun planAutoSchedule(...): List<ScheduleDecision>`.
     The return type is solely `List<ScheduleDecision>`. Any task not scheduled is completely omitted without returning an error or rejection telemetry.
   - **Early Loop Escapes (Silent Drops in `AutoSchedulingEngine.kt`)**:
     - Line 187: `if (eligibleTasks.isEmpty()) return emptyList()` (tasks blocked by dependencies or manual date/time are dropped with no feedback).
     - Line 193: `if (!prefs.autoSchedulingEnabled) return emptyList()`
     - Line 205: `if (horizon.slots.isEmpty()) return emptyList()`
     - Line 392: `if (taskFits.isEmpty()) return@forEach` (dropped if no slots match energy or deadline requirements).
     - Line 414: `if (slot.hourOfDay > maxAllowedHour) return@forEach` (dropped due to workday limit).
     - Line 425: `val cStart = findEarliestAvailableStartTime(...) ?: return@forEach` (dropped because no contiguous free block exists in occupied intervals).
     - Line 429: `if (deadlineMillis != null && cStart + duration > deadlineMillis) return@forEach` (dropped because task duration would exceed deadline).
     - Line 446: `if (!isDeadlineUrgent && !canAssignWithoutBurnout(...)) return@forEach` (dropped because daily cognitive limit or consecutive hours exceeded).
     - Line 489: `val chosen = ... ?: return@forEach` (dropped if all candidate slots failed score threshold).
   - **Caller Ignorance**:
     - `ScheduleAutoTasksWorker.kt:237-241`: Simply checks `if (decisions.isNotEmpty()) applyDecisionsTransactionally(decisions, allTasks)` and returns `Result.success()`. Unplaced tasks are neither logged nor reported.
     - `ScheduleViewModel.kt:96-98`: Unconditionally posts `autoScheduleResultMessage = "Auto-scheduling complete"`.
   - **Database Schema**:
     - `TaskEntity.kt:14-103`: Contains fields `isAutoScheduled`, `plannedBlockMinutes`, `lastAutoScheduledAt`, but lacks any fields for `placementRationale`, `deferralReasonCode`, or `deferralActionSuggestion`.

### 1.4 Behavioral Pattern Insights (R5)
1. **Analytics Engine Implementation**:
   - `AnalyticsEngine.kt:21-50`: Computes MAPE, SMAPE, weighted MAPE, and weighted SMAPE.
   - `AnalyticsEngine.kt:69-136`: `PlanningCalibration` calculates global and category-level inflation factors:
     - Uses geometric mean of $\text{actual} / \text{estimated}$ ratios (clamped $[0.4, 3.0]$).
     - Applies Bayesian shrinkage toward prior ($1.0$, prior strength $K_0 = 4.0$).
     - Factor clamped $[0.8, 1.6]$.
   - `AnalyticsEngine.kt:146-175`: `EnjoymentCalibration` adjusts enjoyment score by $[-15, +15]$ points based on `affectiveForecastError`.
   - `AnalyticsEngine.kt:456-491`: Computes `peakHourFocusMinutes` and `offPeakFocusMinutes` by checking whether session timestamps overlap with static/quiz peak hours.
2. **Current Behavioral Feedback Gaps**:
   - `TaskScoringEngine.kt` does NOT use `estimationErrorMape` or `actualDurationMinutes` to adjust task prioritization, causing underestimated multi-hour tasks to be mis-scored as quick wins.
   - Peak hours are determined solely from static preferences or the one-time MEQ quiz (`effectivePeakStart`/`effectivePeakEnd`); there is no continuous dynamic empirical peak detection from the user's historical completion/focus timestamps.

---

## 2. Features Discovered

| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | Focus Mode | Time Tracking Engine | Live session tracking with pause/resume and DB persistence | `taskId`, `nowMillis` | `TimeSessionEntity`, live elapsed seconds | Intercepts back navigation; auto-pauses on leave | `FocusViewModel.kt`, `FocusSessionManager.kt` |
| 2 | Focus Mode | Pomodoro Countdown | Standalone countdown timer (default 25m) | `pomodoroTotal` (seconds) | `pomodoroSeconds`, progress | Decoupled from session time logging | `FocusViewModel.kt:449` |
| 3 | Focus Mode | WOOP Reflection & If-Then Plan | Obstacle & plan entry with automatic plan suggestion | `wish, outcome, obstacle, plan` | `WoopEntity`, `ifThenPlan` | Shown once; reopenable from button | `FocusWoopManager.kt`, `WoopPromptSheet.kt` |
| 4 | Focus Mode | Affordance Difficulty Rating | Post-session subjective feeling rating (-2 to +2) | Rating float `[-2.0, 2.0]` | Updates `affectiveForecastError` & `enjoymentForecastBias` | Dismissible via Skip | `AffordanceRatingSheet.kt`, `FocusWoopManager.kt` |
| 5 | Focus Mode | Launch Countdown | 8s wait + 5s countdown auto-starting focus session | `autoTrackerEnabled` pref | Auto-calls `startTracking()` | Cancelled if user taps Start manually | `FocusViewModel.kt:626` |
| 6 | HyperFocus | App Blocking Enforcer | Accessibility service detecting foreground apps & redirecting | Foreground package name | Launches `HyperFocusActivity` overlay | Falls back to `GLOBAL_ACTION_HOME` if launch denied | `AppBlockingService.kt` |
| 7 | HyperFocus | Cognitive Gateway | Mindful delay gate with arithmetic/pattern task & reason | 12s delay, reason string, answer | One-app temporary pass (`activePassPackage`) | Auto-passes after 3 mismatches | `GateTaskScreen.kt`, `GateTaskEngine.kt` |
| 8 | HyperFocus | Emergency Bypass | 10-minute pass to bypass gate without task | Package name | Grants 10m pass; sets `emergencyUsed = true` | Punitive: wipes intermediate rewards, locks to all tasks | `EmergencyControl.kt`, `HyperFocusManager.kt` |
| 9 | Auto-Schedule | 3-Day Capacity Horizon | Generates 1-hour slots with energy zone & availability | `nowMillis`, `horizonDays`, `energyScoreFn` | `CapacityHorizon` with `TimeSlot` list | Excludes past slots (<5m left) | `AutoSchedulingEngine.kt:1175` |
| 10 | Auto-Schedule | Calibrated Duration Inflation | Multiplies estimate by error MAPE, planning bias, & energy zone | `task`, `slot`, `durationFactor` | Inflated duration (mins, capped at 360) | Can cause duration to exceed slot capacity | `AutoSchedulingEngine.kt:922` |
| 11 | Auto-Schedule | Burnout & Break Protection | Caps daily cognitive minutes & forces breaks after load | `highCognitiveMinutesByDay`, `breakPolicy` | Slot reservation / block index | Silently drops task if budget exceeded | `AutoSchedulingEngine.kt:434, 1075` |
| 12 | Auto-Schedule | Protected Slot Shielding | Blocks task scheduling over recurring protected time blocks | `prefs.protectedTimeSlots` | Adds occupied intervals | Cannot schedule during protected blocks | `AutoSchedulingEngine.kt:243` |
| 13 | Analytics | Estimation Error Calibration | Bayesian shrinkage of actual/estimated ratio geometric mean | Completed tasks with actual & estimate | `PlanningCalibration(globalFactor, categoryFactors)` | Falls back to `UNIFORM` (1.0) if <5 samples | `AnalyticsEngine.kt:69` |
| 14 | Analytics | Peak Focus Analytics | Hourly window overlap metrics (peak vs off-peak minutes) | `TimeSessionEntity` list, peak start/end | `peakHourFocusMinutes`, `offPeakFocusMinutes` | Handles midnight rollover windows | `AnalyticsEngine.kt:456` |

---

## 3. Edge Cases

| # | Feature | Input | Observed Behavior |
|---|---------|-------|-------------------|
| 1 | FocusScreen Theme | Dark / AMOLED mode active | Blinding contrast violations: cards render in hardcoded pastel pink (`0xFFFFEBEE`), pastel blue (`0xFFE3F2FD`), cream (`0xFFFFF8E1`), peach (`0xFFFFF3E0`); Done button has `containerColor = Color.Black`. |
| 2 | Auto-Schedule Horizon | Task realistic duration (e.g. 110 min) > largest available slot gap (e.g. 60 min) | `findEarliestAvailableStartTime` returns `null`; task is silently dropped without notification or deferral record. |
| 3 | Auto-Schedule Energy | High-effort task submitted when all horizon slots are LOW or CRITICAL energy | Non-urgent task is rejected by `energyDemandFitPenalty`; silently dropped. |
| 4 | Auto-Schedule Burnout | Daily high-cognitive load cap reached (e.g. >240 min) | `canAssignWithoutBurnout` returns `false`; remaining high-cognitive tasks are silently dropped for that day. |
| 5 | Auto-Schedule Overdue | Task deadline was yesterday | Overdue boost (`+0.35f`) triggers; task bypasses burnout guard and energy hard-blocks to land on day 0. |
| 6 | HyperFocus Emergency | User triggers Emergency Bypass in task-based mode | Grants 10-minute pass; wipes current reward tier to `NONE`; forces user to complete ALL remaining tasks for next unlock. |
| 7 | Session Rollover | Focus session runs across local midnight | `TimeSessionEntity.startedAt` is day $N$, `endedAt` is day $N+1$; `sevenDayFocusTrend` and peak-hour splitting correctly segment durations across midnight. |
| 8 | Daylight Saving Time | Auto-scheduler calculates capacity across 23h (spring forward) or 25h (fall back) day | `Calendar.add(Calendar.HOUR_OF_DAY, 1)` correctly advances time without hour-skipping or duplicated millisecond timestamps. |
| 9 | Dependency Deadlock | Task A depends on Task B, Task B depends on Task A | `detectBlockedTasks` identifies circular dependency; both tasks are silently excluded from scheduling. |
| 10 | Unfinished External Blocker | Task has non-empty `waitingFor` (e.g., "Waiting on client feedback") | `detectBlockedTasks` marks task blocked; excluded from auto-scheduling until `resolveWaitingFor()` is called. |

---

## 4. Logic Chain

1. **Focus Mode Visual & Ergonomic Friction**:
   - Observations in `FocusScreen.kt:409-572` and `Color.kt:34-36` prove that light pastel colors and `Color.Black` are hardcoded into cards and buttons.
   - Observation in `FocusScreen.kt:404-528` proves WOOP reminders and if-then plans are non-collapsible static views taking up prominent screen height.
   - Observation in `FocusViewModel.kt:449-464` proves Pomodoro and time tracking sessions run in parallel isolation without unified state or custom timers.
   - *Inference*: Rebuilding Focus Mode requires unifying the session timer with Pomodoro/custom timer presets, converting WOOP and implementation intentions into collapsible cards, and substituting dynamic Material 3 color roles (`surfaceContainer`, `onSurface`, `primaryContainer`) for all hardcoded pastels.

2. **App Blocking & Hostile Copy Transformation**:
   - Observations in `BlockingOverlayScreen.kt:114, 130, 131, 654` and `HyperFocusManager.kt:414` prove that the existing system relies on shaming insults ("become dumber", "still stupid") and punitive reward destruction.
   - User requirement R4 mandates: "Replace punitive/hostile blocking copy with constructive, supportive interventions across three user-selectable tiers (Zen, Shield, Fortress)."
   - *Inference*: The architecture must replace insulting quotes with supportive, neuroscience-backed cognitive reframing, remove punitive reward resets upon emergency access, and establish 3 distinct user-selectable intervention tiers:
     - **Zen Tier**: Gentle, mindful friction (non-blocking mindful interstitial, 5s breath/intention check, 0 penalty).
     - **Shield Tier**: Supportive boundary (cognitive pause, 10s reflection holding gate, temporary passes).
     - **Fortress Tier**: Ulysses commitment (strict allowlist, Accessibility/Kiosk enforcement, respectful emergency access with paused rather than erased progress).

3. **Auto-Scheduling Silent Drops & Modular Decomposition**:
   - Observation in `AutoSchedulingEngine.kt:164` shows the return type is `List<ScheduleDecision>` with no mechanism to communicate why unscheduled tasks failed.
   - Observation in `AutoSchedulingEngine.kt:392, 414, 425, 429, 446, 489` shows 6 distinct early-return branches where tasks are dropped silently.
   - Observation in `TaskEntity.kt` shows no fields exist to store placement rationales or deferral reasons.
   - *Inference*: To eliminate silent drops, the engine must return a unified `SchedulingReport(placements, deferrals)`. The 2,439-line monolith must be decomposed into modular components (`SchedulingHorizonManager`, `SchedulingEligibilityGate`, `CalibratedDurationEstimator`, `TaskSlotScorer`, `CognitiveBudgetGuard`, `IntervalPlacementCursor`, `SchedulingEngineCoordinator`).

4. **Behavioral Insights Feedback Loop**:
   - Observation in `AnalyticsEngine.kt:69-136` shows `PlanningCalibration` calculates geometric mean ratios and shrinkage, but `TaskScoringEngine.kt` does not consume estimation error data.
   - Observation in `AnalyticsEngine.kt:456-491` shows peak hours are statically matched against initial questionnaire/settings rather than learned from historical session timestamps.
   - *Inference*: Mathematical formulas must be formalized for (a) empirical peak distribution (24h histogram of productive minutes/completions) to dynamically calibrate peak windows, and (b) feeding estimation uncertainty/inflation into `TaskScoringEngine` so chronically underestimated tasks are properly calibrated.

---

## 5. Architectural Specifications & Data Structures

### 5.1 Auto-Scheduling Rationale & Actionable Deferral Models

```kotlin
package com.neuroflow.app.domain.scheduler.model

import com.neuroflow.app.domain.scheduler.AutoSchedulingEngine.EnergyZone

/**
 * Explains exactly why a task was scheduled at a specific slot.
 */
data class PlacementRationale(
    val primaryFactor: PlacementFactor,
    val humanReadableSummary: String,
    val energyMatchScore: Float,
    val tagCircadianFitScore: Float,
    val deadlineUrgencyScore: Float,
    val compositeScore: Float,
    val calibratedDurationMinutes: Int,
    val rawEstimatedMinutes: Int,
    val durationInflationMultiplier: Float, // e.g. 1.25x due to moderate energy + planning bias
    val recoveryBreakReservedMinutes: Int
)

enum class PlacementFactor {
    PEAK_ENERGY_MATCH,
    IMMINENT_DEADLINE,
    HIGH_IMPACT_PRIORITY,
    CIRCADIAN_TAG_ALIGNMENT,
    TODAY_SLACK_ABSORPTION,
    DEPENDENCY_SEQUENCE_PROGRESSION
}

/**
 * Actionable explanation for why a task could not be scheduled, preventing silent drops.
 */
data class DeferralRationale(
    val taskId: String,
    val taskTitle: String,
    val code: DeferralReasonCode,
    val explanation: String,
    val actionableSuggestions: List<ActionableSuggestion>
)

enum class DeferralReasonCode {
    DEPENDENCY_BLOCKED,
    EXTERNAL_WAITING_FOR,
    INSUFFICIENT_CONTIGUOUS_CAPACITY,
    ENERGY_MISMATCH,
    COGNITIVE_BUDGET_EXCEEDED,
    HORIZON_CAPACITY_EXHAUSTED,
    DEADLINE_UNATTAINABLE,
    OUTSIDE_PERMITTED_WORK_HOURS,
    SCHEDULE_LOCKED_CONFLICT
}

data class ActionableSuggestion(
    val type: SuggestionType,
    val label: String,
    val actionPayload: String? = null
)

enum class SuggestionType {
    SPLIT_INTO_SPRINTS,        // Suggest splitting e.g. 90m task into two 45m sessions
    EXTEND_DEADLINE,           // Suggest pushing deadline by X hours
    EXPAND_HORIZON_DAYS,       // Suggest expanding 3-day horizon to 5 days
    RELAX_ENERGY_REQUIREMENT,  // Suggest allowing task in Moderate energy slots
    INCREASE_COGNITIVE_BUDGET, // Suggest raising daily high-effort limit
    RESOLVE_BLOCKER            // Deep-link to unblock dependent task or clear waitingFor
}

/**
 * Dual result returned by the auto-scheduling engine.
 */
data class SchedulingReport(
    val generatedAtMillis: Long,
    val placedTasks: List<PlacedTaskDecision>,
    val deferredTasks: List<DeferralRationale>
)

data class PlacedTaskDecision(
    val taskId: String,
    val scheduledStartMillis: Long,
    val plannedBlockMinutes: Int,
    val rationale: PlacementRationale
)
```

### 5.2 Modular Auto-Scheduling Architecture

```
com.neuroflow.app.domain.scheduler/
├── AutoSchedulingContracts.kt          // Safety invariants & immutability gates
├── SchedulingEngineCoordinator.kt      // Main entry: orchestrates planning & emits SchedulingReport
├── horizon/
│   ├── SchedulingHorizonManager.kt     // Computes 3-day time slots & overlay intervals
│   └── ProtectedSlotResolver.kt        // Resolves recurring Protected Time Slots & recovery buffers
├── eligibility/
│   ├── SchedulingEligibilityGate.kt    // Filters locked tasks & checks contracts
│   └── DependencyGraphResolver.kt      // Topological sorting, circular & waitingFor detection
├── estimation/
│   └── CalibratedDurationEstimator.kt  // Applies MAPE/SMAPE + PlanningCalibration + EnergyZone
├── scoring/
│   ├── TaskSlotScorer.kt               // Pure slot fit scoring (energy, tag, deadline, priority)
│   └── CategoryAlignmentEngine.kt      // Chronotype-aware behavioral category windows
├── placement/
│   ├── IntervalPlacementCursor.kt      // Contiguous non-overlapping free interval search
│   └── CognitiveBudgetGuard.kt         // Burnout protection & consecutive high-load enforcement
└── model/
    ├── PlacementRationale.kt           // Rationale data models
    └── DeferralRationale.kt            // Actionable deferral data models
```

### 5.3 Focus Mode & 3 Supportive Tiers Specification (Zen, Shield, Fortress)

```kotlin
package com.neuroflow.app.feature.hyperfocus.domain

enum class FocusInterventionTier(
    val displayName: String,
    val description: String,
    val isStrictEnforcement: Boolean
) {
    ZEN(
        displayName = "Zen",
        description = "Mindful awareness without barriers. Gentle pause to notice intention.",
        isStrictEnforcement = false
    ),
    SHIELD(
        displayName = "Shield",
        description = "Supportive boundaries. Brief cognitive check before opening distracting apps.",
        isStrictEnforcement = false
    ),
    FORTRESS(
        displayName = "Fortress",
        description = "Deep immersion. Strict app lock holding your commitment until completion.",
        isStrictEnforcement = true
    )
}
```

#### Supportive Copy Matrix (Replacing Punitive & Hostile Strings)

| Screen / Touchpoint | Legacy Hostile / Punitive Copy | New Supportive Copy (Zen / Shield / Fortress) |
|---------------------|-------------------------------|-----------------------------------------------|
| `BlockingOverlayScreen` Main Quote 1 | `"Lol. Don't say, (you lost your motivation)."` | *Zen/Shield*: "Notice what pulled you here. Is this serving your focus right now?" |
| `BlockingOverlayScreen` Main Quote 2 | `"Is this really worth it? Do you really want to become dumber?"` | *Zen/Shield*: "Every reset starts with awareness. Take a slow breath and choose what's next." |
| `BlockingOverlayScreen` Main Quote 3 | `"Look! who we have here!! Lamao,lol.. I thought ,you have finally became smarter. Ig, you are still stupid"` | *Fortress*: "Your focus on [Task Title] is protected. We're holding the perimeter so you can finish." |
| Emergency Access Dialog | `"⚠️ WARNING: If you use this emergency bypass, you will lose your next intermediate rewards and must complete ALL daily tasks..."` | *"Emergency Break (10m)": "Need a quick reset? Take 10 minutes. Your progress will be paused and ready when you return."* |
| Activation Confirmation | Forced `"Type FOCUS to confirm"` | Clean single-tap confirmation: *"Start Focus Session"* with progressive summary. |
| Tamper Alert | `"⚠️ Tamper detected: Unusual activity detected"` | *"Session Boundary Notice": "Accessibility settings were adjusted. Re-enable to keep your focus protection active."* |
| Exit Gate Prompt | `"Before you end it... lifts every block on this device"` | *"Check in with yourself": "Want to conclude your session early? 10 seconds to reflect on your progress today."* |

### 5.4 Behavioral Pattern Insights: Mathematical Formulas

1. **Empirical Productivity Density Function (24-Hour Circadian Fit)**:
   Given a set of historical completed sessions $S$, each session $s \in S$ spanning $[t_{\text{start}}, t_{\text{end}}]$ with duration minutes $D_s$ and affordance rating $A_s \in [-2, +2]$:
   $$\text{Weight}(s) = 1.0 + 0.15 \times A_s$$
   For each hour of the day $h \in [0, 23]$, calculate the empirical focus volume:
   $$V(h) = \sum_{s \in S} \text{OverlapMinutes}(s, h) \times \text{Weight}(s)$$
   Normalize into a 24-hour density distribution:
   $$P(h) = \frac{V(h)}{\sum_{i=0}^{23} V(i)}$$
   The **Empirical Peak Window** $[h_{\text{peak\_start}}, h_{\text{peak\_end}}]$ is the continuous window of length $W \in [2, 3]$ hours that maximizes:
   $$\arg\max_{h} \sum_{k=h}^{h + W - 1} P(k \pmod{24})$$
   **Adaptive Chronotype Blending**:
   $$\text{EffectivePeakStart} = (1 - \alpha) \cdot \text{MEQPeakStart} + \alpha \cdot h_{\text{peak\_start}}$$
   Where $\alpha = \frac{|S|}{|S| + 10} \in [0, 0.75]$, ensuring empirical data smoothly calibrates self-reported chronotypes as session history grows.

2. **Calibrated Task Duration Formula**:
   $$D_{\text{calibrated}} = \min\left(360, \left\lfloor D_{\text{historical}} \times \left(1 + \frac{\min(\text{MAPE}, \text{SMAPE})}{100}\right) \times F_{\text{category}} \times E_{\text{zone}}\right\rfloor\right)$$
   Where:
   - $D_{\text{historical}} = \frac{D_{\text{base}} + D_{\text{actual}}}{2}$ if actual duration exists, else $D_{\text{base}}$.
   - $F_{\text{category}} = \text{PlanningCalibration.categoryFactor}$ (Bayesian-shrunk geometric mean of ratios, clamped $[0.8, 1.6]$).
   - $E_{\text{zone}}$ is the energy expansion penalty:
     - PEAK: $1.0\times$
     - HIGH: $1.1\times$
     - MODERATE: $1.25\times$
     - LOW: $1.45\times$
     - CRITICAL: $1.85\times$

---

## 6. Caveats

1. **Accessibility Service Limitations on Android 12+**:
   Android 12+ imposes background activity launch restrictions. When launching `HyperFocusActivity` overlay from `AppBlockingService`, fallback to `performGlobalAction(GLOBAL_ACTION_HOME)` must be maintained for devices without overlay permissions.
2. **Database Schema Additions**:
   Persisting `placementRationale` and `deferralRationale` directly on `TaskEntity` or in a new table `task_scheduling_status` requires Room schema migration (v11 to v12). Any additions must be strictly non-destructive per acceptance criteria.
3. **No Code Modified**:
   Per Specification Miner role constraints, zero source code or test files were modified during this investigation.

---

## 7. Conclusion

1. **Focus Mode (R4)** currently suffers from severe contrast-breaking hardcoded light pastels on `FocusScreen`, completely decoupled Pomodoro vs time-tracking states, and non-collapsible WOOP/intention views.
2. **App Blocking & Distraction Intervention (R4)** contains explicitly hostile copy (insulting quotes, abusive emergency penalties). It requires replacement with the 3 constructive, supportive tiers (**Zen**, **Shield**, **Fortress**) and non-judgmental reframing copy.
3. **Auto-Scheduling Engine (R5)** silently drops tasks across 6 early returns in `AutoSchedulingEngine.kt` because its interface only returns `List<ScheduleDecision>` and neither reports nor persists rejections. Decomposing the 2,439-line monolith into modular components returning a dual `SchedulingReport(placements, deferrals)` resolves all silent drop issues.
4. **Behavioral Insights (R5)** already possesses the foundation in `AnalyticsEngine.PlanningCalibration`, but lacks empirical 24-hour peak density calculations and does not connect estimation error calibration into `TaskScoringEngine`.

---

## 8. Verification Method

1. **Verify File Findings**:
   - Inspect hostile copy in `BlockingOverlayScreen.kt:114, 130, 131, 654`:
     `grep -nE "(dumber|stupid|Lamao|lost your motivation)" app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/BlockingOverlayScreen.kt`
   - Inspect hardcoded pastel colors in `FocusScreen.kt:409, 435, 537, 572, 946`:
     `grep -n "0xFF" app/src/main/java/com/neuroflow/app/presentation/focus/FocusScreen.kt`
   - Inspect silent drop return paths in `AutoSchedulingEngine.kt:164, 392, 425, 429, 446`:
     `grep -n "return@forEach" app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingEngine.kt`
2. **Run Unit Tests**:
   - Run existing auto-scheduling and analytics tests:
     `./gradlew testDebugUnitTest --tests "com.neuroflow.app.domain.scheduler.*" --tests "com.neuroflow.app.domain.engine.AnalyticsEngine*"`
