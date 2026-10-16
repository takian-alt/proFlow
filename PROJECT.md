# Project: neuroFlow Modernization

## Architecture
neuroFlow is an offline-first, neuroscience-backed Android execution engine built with Jetpack Compose, Kotlin Coroutines/Flow, Room Database, and Material 3.

```
                    ┌─────────────────────────────────────────┐
                    │               UI Layer                  │
                    │  (3 Core Hubs: Flow / Plan / Insights)  │
                    │  (FocusScreen: Zen / Shield / Fortress) │
                    │  (QuickTaskModalBottomSheet: Inference) │
                    └────────────────────┬────────────────────┘
                                         │
                                         ▼
                    ┌─────────────────────────────────────────┐
                    │               Domain Layer              │
                    │  - Borbély Two-Process & Peak Engines   │
                    │  - TaskInferenceEngine (Regex/Keywords) │
                    │  - Modular AutoSchedulingEngine:        │
                    │    * PlacementRationale                 │
                    │    * DeferralRationale                  │
                    │    * BehavioralPatternTracker           │
                    │  - TaskScoringEngine (Live Energy Link) │
                    └────────────────────┬────────────────────┘
                                         │
                                         ▼
                    ┌─────────────────────────────────────────┐
                    │               Data Layer                │
                    │  - Room DB (v12 schema, hardened enums) │
                    │  - Non-destructive Migrations (Notes OK)│
                    │  - Async Non-blocking Init (App startup)│
                    └─────────────────────────────────────────┘
```

## Feature Inventory
Every feature from the survey phase and ORIGINAL_REQUEST.md:

| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Test Runner Config | Add junit-vintage-engine to enable JUnit 4 + JUnit 5 tests under `testDebugUnitTest` | M1 | Survey / R1 |
| 2 | Borbély Baseline Fix | Fix pre-peak morning zeroing in `PeakEnergyEngine.currentValueAt` / `EnergyScoreEngine` | M1 | Survey / R1 |
| 3 | Circadian Midnight Wrap | Support circadian window wrapping across midnight for evening chronotypes | M1 | Survey / R1 |
| 4 | Confidence Decoupling | Decouple statistical confidence factor from physical energy magnitude in `EnergyScoreEngine` | M1 | Survey / R1 |
| 5 | Live Energy Integration | Connect live energy state directly to `TaskScoringEngine` and auto-scheduling | M1 | Survey / R1 |
| 6 | Room Enum Hardening | Harden `Converters.kt` against unknown enum strings for all 7 enums | M1 | Survey / R1 |
| 7 | Non-Destructive Migrations | Provide `12.json` schema, verify migration 11->12, and preserve user notes across migrations | M1 | Survey / R1 |
| 8 | Async App Startup | Eliminate `runBlocking` and main-thread disk I/O in `NeuroFlowApplication.onCreate()` | M1 | Survey / R1 |
| 9 | Task Inference Engine | Pure parser extracting duration, energy, task type, tags, and frog priority from title | M2 | Survey / R2 |
| 10 | Quick Task ModalBottomSheet | Streamlined `ModalBottomSheet` with progressive disclosure replacing monolithic dialog | M2 | Survey / R2 |
| 11 | Material 3 Theme & AMOLED | Establish pure Dark & AMOLED mode support with proper surface/background tokens | M3 | Survey / R3 |
| 12 | Contrast Color Elimination | Remove 250+ hardcoded pastel hex colors from `FocusScreen`, `TodayScreen`, cards, chips | M3 | Survey / R3 |
| 13 | 48dp Touch Targets | Expand all interactive buttons/chips to minimum 48x48dp touch targets | M3 | Survey / R3 |
| 14 | Semantic Roles & A11y | Add `Role.Button` and accessibility semantics across clickable components | M3 | Survey / R3 |
| 15 | 3-Hub Navigation | Consolidate navigation into Flow, Plan, Insights; eliminate dead drawer files & orphan routes | M3 | Survey / R3 |
| 16 | Rebuild Focus Screen | Clean timers (Pomodoro & custom), state synchronization, and difficulty feedback | M4 | Survey / R4 |
| 17 | Collapsible WOOP | Implementation intentions and collapsible WOOP card on FocusScreen | M4 | Survey / R4 |
| 18 | Supportive Focus Tiers | Replace hostile/insulting copy with 3 supportive tiers (Zen, Shield, Fortress) | M4 | Survey / R4 |
| 19 | Modular Auto-Scheduler | Decompose `AutoSchedulingEngine` into modular pipeline components | M5 | Survey / R5 |
| 20 | Placement & Deferral Rationales | Provide clear placement rationales and actionable deferral suggestions (no silent drops) | M5 | Survey / R5 |
| 21 | Behavioral Insights | Compute empirical circadian productivity peaks and estimation error calibration | M5 | Survey / R5 |
| 22 | E2E Verification & Hardening | 100% pass of Tiers 1-4 tests, followed by Tier 5 adversarial coverage hardening | M6 | Survey / AC |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Core Engineering & Math Pipeline Fixes | Features 1-8: Gradle test config, energy engines, live energy scoring, Room enum hardening, migrations & schemas, non-blocking startup | None | IN_PROGRESS |
| M2 | Minimal-Friction Task Creation & Smart Inference | Features 9-10: TaskInferenceEngine, QuickTaskModalBottomSheet, TodayViewModel quick capture integration | M1 | PLANNED |
| M3 | Modern Design System, Accessible UI & Navigation Hubs | Features 11-15: Material 3 tokens, Dark/AMOLED fix, 48dp touch targets, semantic roles, 3-hub navigation | M1 | PLANNED |
| M4 | Supportive Focus & Tiered HyperFocus | Features 16-18: Focus mode timers, Pomodoro sync, collapsible WOOP, Zen/Shield/Fortress tiers | M1, M3 | PLANNED |
| M5 | Auto-Scheduling Transparency & Behavioral Insights | Features 19-21: AutoSchedulingEngine decomposition, Placement/Deferral rationales, behavioral calibration | M1, M2 | PLANNED |
| M6 | Full E2E Verification & Coverage Hardening | Feature 22: Pass 100% of E2E tests (Tiers 1-4) published by E2E track, then Tier 5 adversarial hardening | M1, M2, M3, M4, M5 | PLANNED |

## Interface Contracts

### 1. Energy Engine ↔ Task Scoring (`EnergyScoreEngine` & `TaskScoringEngine`)
```kotlin
data class LiveEnergyContext(
    val usableEnergy: Float,      // 0f .. 100f
    val reservoirFactor: Float,   // 0f .. 1f
    val circadianFactor: Float,   // 0f .. 1f
    val sleepPressureRatio: Float,// 0f .. 1f
    val confidence: Float         // 0f .. 1f (statistical weight, does not zero magnitude)
)

object TaskScoringEngine {
    fun score(
        task: TaskEntity,
        prefs: UserPreferences,
        context: ScoringContext = ScoringContext.EMPTY,
        liveEnergy: LiveEnergyContext? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): Float
}
```

### 2. Task Inference ↔ Task Creation UI (`TaskInferenceEngine`)
```kotlin
data class InferredTaskMetadata(
    val sanitizedTitle: String,
    val durationMinutes: Int,
    val energyDemand: EnergyLevel,
    val taskType: TaskType,
    val contextTags: List<String>,
    val isFrog: Boolean,
    val scheduledDate: Long?
)

object TaskInferenceEngine {
    fun inferFromInput(rawInput: String): InferredTaskMetadata
}
```

### 3. Auto-Scheduling ↔ Presentation (`AutoSchedulingEngineCoordinator`)
```kotlin
data class PlacementRationale(
    val taskId: String,
    val scheduledSlotStart: Long,
    val scheduledSlotEnd: Long,
    val energyFitScore: Float,
    val primaryReason: String
)

data class DeferralRationale(
    val taskId: String,
    val reasonCode: DeferralReasonCode, // INSUFFICIENT_TIME, ENERGY_MISMATCH, COGNITIVE_BUDGET_EXCEEDED, DEADLINE_CONFLICT
    val explanation: String,
    val actionableSuggestion: String
)

data class SchedulingReport(
    val placedDecisions: List<ScheduleDecision>,
    val placementRationales: List<PlacementRationale>,
    val deferrals: List<DeferralRationale>,
    val dailyCognitiveLoadUtilized: Float
)
```

### 4. Focus Mode Supportive Tiers (`FocusInterventionManager`)
```kotlin
enum class FocusInterventionTier {
    ZEN,        // Mindful pause, breathing prompt
    SHIELD,     // Cognitive gate, deliberate friction & reflection
    FORTRESS    // Strict boundary, intentional barrier
}

data class FocusIntervention(
    val tier: FocusInterventionTier,
    val heading: String,
    val message: String,
    val primaryActionLabel: String,
    val dismissActionLabel: String
)
```

## Code Layout
- `app/build.gradle.kts`: Gradle build configuration, testOptions, dependencies
- `app/src/main/java/com/neuroflow/app/NeuroFlowApplication.kt`: Application entry point & non-blocking async initialization
- `app/src/main/java/com/neuroflow/app/data/local/`: Room database, entities, DAOs, `Converters.kt`
- `app/src/main/java/com/neuroflow/app/domain/engine/`: Borbély two-process, peak energy, task scoring engines
- `app/src/main/java/com/neuroflow/app/domain/inference/`: `TaskInferenceEngine.kt` quick capture heuristic parser
- `app/src/main/java/com/neuroflow/app/domain/scheduler/`: Modular auto-scheduling engine, placement/deferral rationales, behavioral insights
- `app/src/main/java/com/neuroflow/app/presentation/common/`: Theme (`Theme.kt`, `Color.kt`, `Type.kt`), `QuickTaskModalBottomSheet.kt`, `TaskRow.kt`
- `app/src/main/java/com/neuroflow/app/presentation/focus/`: `FocusScreen.kt`, `FocusViewModel.kt`, supportive tier interventions
- `app/src/main/java/com/neuroflow/app/presentation/navigation/`: Consolidated 3-hub navigation (`NeuroFlowNavHost.kt`, `Screen.kt`)
