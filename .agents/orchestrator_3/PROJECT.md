# Project: neuroFlow V2 Parity & Neuro-Capabilities

## Architecture
neuroFlow V2 is an offline-first, neuroscience-backed Android application built with Jetpack Compose, Kotlin Coroutines/Flow, Room Database, and Material 3 ("Obsidian Sanctuary" sensory design).

```
┌────────────────────────────────────────────────────────────────────────┐
│                        V2 UI Presentation Layer                        │
│                                                                        │
│  ┌──────────────────────┐ ┌──────────────────────┐ ┌────────────────┐  │
│  │     Flow Screen      │ │     Plan Screen      │ │Insights Screen │  │
│  │  - Circadian Barom.  │ │  - 4 Eisenhower Qs   │ │ - Chronotype   │  │
│  │  - Hero Next Action  │ │  - Time Blocking     │ │ - Circadian Pk │  │
│  │  - Timeline Stream   │ │  - Weekly Schedule   │ │ - 7d/30d Veloc │  │
│  │  - Zero-Guilt Rollov │ │    7-Day Calendar    │ │ - 24h Heatmap  │  │
│  │  - Magic Wand Scaff. │ │  - Tag Filter Pills  │ │ - sMAPE Accur. │  │
│  │  - 4s Non-Destr Undo │ │  - Notes BottomSheet │ │ - Identity Aff.│  │
│  │  - Evening Shutdown  │ └──────────────────────┘ └────────────────┘  │
│  │  - Retrospective Log │                                              │
│  └──────────────────────┘ ┌──────────────────────────────────────────┐  │
│                           │          Focus Screen & Service          │  │
│  ┌──────────────────────┐ │  - 4 Modes: 5m, 25m, 90m, Flow Stopwatch │  │
│  │ Smart Task Composer  │ │  - Drift-Proof Wall-Clock Timing         │  │
│  │  - 1-Tap Mic Voice   │ │    (startedAtMillis, accumulatedPause)   │  │
│  │  - Natural Language  │ │  - Neural Soundscapes (Gamma/Brown/Pink) │  │
│  │    Inference Pipeline│ │  - Tactile Volume Slider & Fade-Out      │  │
│  │  - Progressive Fields│ │  - 1-Tap "Set WOOP Anchor" Sheet         │  │
│  └──────────────────────┘ │  - 3-Min Executive Decompression Buffer  │  │
│                           │  - Lock-Screen Live Notification Service │  │
│                           │  - 60s Physiological Sigh Guide          │  │
│                           └──────────────────────────────────────────┘  │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         V2 Domain Engine Layer                         │
│  - NeuralSoundscapeGenerator: Pure DSP PCM synthesis (Gamma/Brown/Pink)│
│  - FocusTimingCalculator: Wall-clock epoch drift-proof calculations    │
│  - TaskInferenceEngine: Natural language keyword & speech inference    │
│  - BehavioralInsightsEngine: 24-hr hourly circadian density heatmap    │
│  - AnalyticsEngine: sMAPE and weighted sMAPE estimation calibration   │
│  - BreakItDownEngine: 3-step initiation scaffolder -> ifThenPlan       │
│  - TaskScoringEngine: LiveEnergyContext ranking & rationale generation │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                    Data, Storage & Interop Layer                       │
│  - UserPreferencesDataStore: useV2Ui = true default                    │
│  - MainActivity: com.procus.ACTION_OPEN_NEW_TASK & ACTION_OPEN_FOCUS   │
│  - Widgets: QuickCaptureWidget, NextActionWidget, FocusWidget          │
│  - Room Database (v21 schema, 11 tables, 21 migrations, 100% backward) │
│  - TimeSessionDao & TaskDao: Wall-clock session persistence            │
└────────────────────────────────────────────────────────────────────────┘
```

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Full Notes System Integration | Wire `QuickNotesBottomSheet` to Keep-style `NotesScreen` and `NoteEditorScreen` via `onOpenFullNotes` callback and `"notes"` route in `NeuroFlowAppV2`; provide `LocalIsDarkTheme provides true` in `NeuroFlowThemeV2` so notes render with sensory Obsidian dark cards | M1 | Survey 1 / R1 |
| 2 | Weekly Schedule Integration | Connect `WeeklyScheduleScreen` (7-day multi-column timetable, week switcher, protected time slots) via `"weekly_schedule"` route in `NeuroFlowAppV2` and calendar action/tab in `PlanScreenV2` | M1 | Survey 1 / R1 |
| 3 | Deep Analytics & History Hub | Embed 7d/30d velocity, 24-hr circadian focus density heatmap (`calculateHourlyCircadianDensity`), and sMAPE estimation accuracy into `InsightsScreenV2`; wire `"analytics"` and `"history"` routes in `NeuroFlowAppV2` | M1 | Survey 1 / R1 |
| 4 | Settings Routes Wiring | Wire missing routes (`priority_weights`, `account_sync`, `hyperfocus_allowlist`, `privacy_permissions`, `app_guide`) into `NeuroFlowAppV2` and replace no-op lambdas in `SettingsScreen` | M1 | Survey 1 / R1 |
| 5 | Identity & WOOP Anchor | Display affirmations in `InsightsScreenV2` and wire route `"identity"`; add 1-tap "Set WOOP Anchor" card and modal sheet to `FocusScreenV2` when `woopData` is empty | M1 | Survey 1 / R1 |
| 6 | Retrospective Time Logging | Add "Log Past Time" action on task options in Flow & Plan calling ViewModel `logPastTime(task, minutes)`, and register route `"log_time"` in `NeuroFlowAppV2` | M1 | Survey 1 / R1 |
| 7 | Pure Kotlin DSP Soundscape Generator | Implement `NeuralSoundscapeGenerator`: 40Hz Gamma binaural beat (200Hz left / 240Hz right), Deep Brown Noise (stochastic leaky random walk), Pink Noise (Paul Kellet 1/f filter approximation), and frame-interpolated fade-out | M2 | Survey 2 / R2 |
| 8 | Soundscapes UI & Audio Lifecycle | Add soundscape selector chips, tactile volume slider (min 48dp target), active animated indicator, and automatic fade-out on pause/finish to `FocusScreenV2` and `FocusViewModelV2` | M2 | Survey 2 / R2 |
| 9 | Drift-Proof Wall-Clock Timing Engine | Re-architect `FocusViewModelV2` and `FocusTimingCalculator` to calculate elapsed and remaining time strictly from `startedAtMillis`, `accumulatedPauseMillis`, and `pausedAtMillis` against wall-clock epoch time | M3 | Survey 2 / R3 |
| 10 | Lock-Screen Live Notification Service | Implement `FocusService` foreground service with `focus_live` channel, live countdown progress bar, task title, and instant 1-tap "Pause" / "Resume" and "Complete" action PendingIntents | M3 | Survey 2 / R3 |
| 11 | Default V2 UI Setting | Set `useV2Ui = true` by default in `UserPreferencesDataStore.kt` constructor (line 165) and fallback (line 416) while preserving manual user override | M4 | Survey 3 / R4 |
| 12 | MainActivity Intent Forwarding | Connect `com.procus.ACTION_OPEN_NEW_TASK` and `ACTION_OPEN_FOCUS` through `MainActivity` -> `NeuroFlowAppV2` -> `FlowScreenV2` / `focus_v2/{taskId}` | M4 | Survey 3 / R4 |
| 13 | Widget Interoperability | Wire `QuickCaptureWidgetProvider`, `NextActionWidgetProvider`, `FocusWidgetProvider`, and `FocusWidgetUpdateWorker` directly to V2 destinations, handling empty states | M4 | Survey 3 / R4 |
| 14 | Hands-Free Voice Quick-Capture | Add 1-tap mic button in `SmartTaskComposerSheetV2` launching `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` and piping recognized speech directly into `TaskInferenceEngine` | M5 | Survey 3 / R5 |
| 15 | Executive Transition Buffer ("Decompression Zone") | Implement 3-minute post-focus decompression countdown buffer with calming overlay, 60s physiological sigh guide trigger, and "Ready Now" autonomy bypass in `FocusViewModelV2` and `FocusScreenV2` | M5 | Survey 3 / R5 |
| 16 | Full Verification & E2E Testing Suite | Pass 100% of unit tests (`./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1`), execute adversarial coverage tests, and verify zero regressions | M6 | AC |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | 100% V1 Feature Parity & Navigation Connectivity | Features 1-6: Notes, Weekly Schedule, Analytics, Settings routes, Identity affirmations, WOOP anchor, Retrospective logging | None | DONE |
| M2 | Neural Soundscapes & Auditory Focus Stimulation | Features 7-8: Pure DSP generator (40Hz Gamma, Brown Noise, Pink Noise), tactile volume slider, fade-out | None | DONE |
| M3 | Drift-Proof Background Focus & Live Notification | Features 9-10: Wall-clock timing engine, FocusService foreground notification with countdown and 1-tap actions | M2 | IN_PROGRESS |
| M4 | Default V2 UI & Home Screen Widget Interoperability | Features 11-13: useV2Ui = true, MainActivity intent handling, QuickCapture/NextAction/Focus widgets wired to V2 | None | PLANNED |
| M5 | Hands-Free Voice Quick-Capture & Transition Buffer | Features 14-15: Mic capture in composer, speech -> TaskInferenceEngine, 3-minute Decompression Zone | M1, M3 | PLANNED |
| M6 | Full Verification, E2E Testing & Coverage Hardening | Feature 16: 100% test pass, adversarial verification, zero memory leaks, final forensic audit | M1, M2, M3, M4, M5 | PLANNED |

## Interface Contracts

### 1. V2 Navigation Routes (`NeuroFlowAppV2.kt`)
```kotlin
// Registered routes:
"flow", "plan", "insights", "focus_v2/{taskId}", "settings"
"notes" -> NotesScreen(onNavigateBack = { navController.popBackStack() })
"weekly_schedule" -> WeeklyScheduleScreen(onNavigateBack = { navController.popBackStack() }, onNavigateToDay = { ... }, onNavigateToFocus = { taskId -> navController.navigate("focus_v2/$taskId") })
"analytics" -> AnalyticsScreen(onNavigateBack = { navController.popBackStack() })
"history" -> HistoryScreen(onNavigateBack = { navController.popBackStack() })
"priority_weights" -> PriorityWeightsScreen(onNavigateBack = { navController.popBackStack() })
"account_sync" -> AccountSyncScreen(onNavigateBack = { navController.popBackStack() })
"hyperfocus_allowlist" -> HyperFocusAllowlistRoute(onNavigateBack = { navController.popBackStack() })
"privacy_permissions" -> PrivacyPermissionsScreen(onNavigateBack = { navController.popBackStack() })
"app_guide" -> AppGuideScreen(onNavigateBack = { navController.popBackStack() })
"identity" -> IdentityScreen(onNavigateBack = { navController.popBackStack() })
"log_time" -> LogTimeScreen(onNavigateBack = { navController.popBackStack() })
```

### 2. Neural Soundscape Generator (`NeuralSoundscapeGenerator.kt`)
```kotlin
enum class SoundscapeType(val displayName: String, val subtitle: String) {
    NONE("Off", "Silence"),
    GAMMA_40HZ("40 Hz Gamma", "Binaural Prefrontal Synchrony"),
    BROWN_NOISE("Deep Brown Noise", "1/f² Stochastic Acoustic Masking"),
    PINK_NOISE("Flow Pink Noise / Rain", "1/f Calming Spectral Density")
}

class NeuralSoundscapeGenerator(sampleRate: Int = 44100) {
    fun generateFrames(type: SoundscapeType, outputBuffer: ShortArray, numFrames: Int, volumeMultiplier: Float = 0.7f)
}
```

### 3. Drift-Proof Focus Timing (`FocusTimingCalculator.kt`)
```kotlin
data class CalculatedTiming(
    val elapsedSeconds: Long,
    val remainingSeconds: Long,
    val isExpired: Boolean
)

object FocusTimingCalculator {
    fun calculate(
        startedAtMillis: Long,
        accumulatedPauseMillis: Long,
        pausedAtMillis: Long?,
        nowMillis: Long,
        targetDurationSeconds: Long
    ): CalculatedTiming
}
```

### 4. Intent Routing & Forwarding Contract
```kotlin
// Intent actions:
"com.procus.ACTION_OPEN_NEW_TASK" -> opens SmartTaskComposerSheetV2
"com.procus.ACTION_OPEN_FOCUS" with extra "taskId" -> opens focus_v2/{taskId}
```

## Code Layout
- `app/src/main/java/com/neuroflow/app/data/local/UserPreferencesDataStore.kt`: Default V2 preferences
- `app/src/main/java/com/neuroflow/app/MainActivity.kt`: Intent handling & V2 routing
- `app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt`: Master V2 navigation graph
- `app/src/main/java/com/neuroflow/app/presentation/v2/theme/NeuroFlowThemeV2.kt`: Theme & LocalIsDarkTheme provider
- `app/src/main/java/com/neuroflow/app/presentation/v2/notes/QuickNotesBottomSheet.kt`: Notes bottom sheet with full notes trigger
- `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt`: Plan hub with weekly schedule entry
- `app/src/main/java/com/neuroflow/app/presentation/v2/insights/InsightsScreenV2.kt`: Insights with heatmap, velocity, sMAPE & affirmations
- `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt`: Focus screen with soundscapes, WOOP anchor, & Decompression Zone
- `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt`: Drift-proof timing & focus state machine
- `app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/`: Neural soundscape DSP generator & player
- `app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt`: Foreground live notification service
- `app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt`: Voice capture & natural language inference
- `app/src/main/java/com/neuroflow/app/presentation/widget/`: Home screen widget providers & update worker
- `app/src/test/java/com/neuroflow/app/`: Test suites
