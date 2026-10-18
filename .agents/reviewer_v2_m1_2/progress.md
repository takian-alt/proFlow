# Progress — reviewer_v2_m1_2

Last visited: 2026-10-16T16:07:45Z

## Current Status
- Completed in-depth code inspection of:
  - `InsightsScreenV2.kt` (velocity 7d/30d, 24-hr circadian heatmap, sMAPE, "analytics" and "history" buttons, identity affirmations)
  - `FocusScreenV2.kt` & `FocusViewModelV2.kt` (WOOP Anchor empty-state card, WoopPromptSheet, saveWoop logic)
  - `FlowViewModelV2.kt` & `PlanViewModelV2.kt` (task options "Log Past Time", ManualTimeLogSheet, logPastTime implementation)
  - `NeuroFlowAppV2.kt` (routing for analytics, history, identity, settings, notes, weekly_schedule, log_time)
- Key Review Discoveries:
  - 1. Missing test coverage for `FlowViewModelV2.logPastTime(task, minutes)` and `PlanViewModelV2.logPastTime(task, minutes)`. Existing E2E tests only tested a mock test harness `ParityOracle.createRetrospectiveSession` and accumulated time manually inside test code rather than testing actual ViewModel implementations.
  - 2. `FocusViewModelV2.saveWoop(wish, outcome, obstacle, plan)` is also unexercised by unit tests.
  - 3. In `InsightsScreenV2.kt`, circadian heatmap peak hour check (`isPeakHour = hour in peakStart until peakEnd`) does not handle circadian wrap (e.g. evening chronotype 22:00 to 02:00), resulting in an empty range (`22 until 2`) where no peak hours are highlighted.
  - 4. In `InsightsScreenV2.kt`, when `weightedSmape == 0f` (no completed tasks yet), the metric tile shows "Calibrating", but the subtitle incorrectly displays "Moderate Drift" because `weightedSmape in 0.1f..25f` evaluates to false.
  - 5. In `FlowScreenV2.kt:326`, `QuickNotesBottomSheet` is instantiated without wiring `onOpenFullNotes` to `"notes"`, unlike `PlanScreenV2` which does wire it.
- Waiting for test execution `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1` (task-141) to complete.
