## 2026-10-16T15:33:34Z
You are a specialized worker implementing Milestone 1: 100% V1 Feature Parity & Navigation Connectivity.

Your working directory is: `/home/takian/Project/PROCUS/.agents/worker_v2_m1`
The project root is: `/home/takian/Project/PROCUS`
Authoritative User Request: `/home/takian/Project/PROCUS/.agents/ORIGINAL_REQUEST.md` (You MUST read this file, especially section `## 2026-10-16T15:15:13Z` - R1).
Master Plan: `/home/takian/Project/PROCUS/.agents/orchestrator_3/PROJECT.md`
Explorer Findings: `/home/takian/Project/PROCUS/.agents/explorer_v2_survey_1/handoff.md`

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Your exclusive write boundaries:
- `app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt`
- `app/src/main/java/com/neuroflow/app/presentation/v2/theme/NeuroFlowThemeV2.kt`
- `app/src/main/java/com/neuroflow/app/presentation/v2/notes/QuickNotesBottomSheet.kt`
- `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt`
- `app/src/main/java/com/neuroflow/app/presentation/v2/insights/InsightsScreenV2.kt`
- `app/src/main/java/com/neuroflow/app/presentation/settings/SettingsScreen.kt`
- `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt` (WOOP anchor trigger)
- `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt` (saveWoop implementation)
- `app/src/main/java/com/neuroflow/app/presentation/common/NeuroFlowApp.kt` (make HyperFocusAllowlistRoute accessible)
- `app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt` & `TimelineScheduleView.kt`
- `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt` & `PlanViewModelV2.kt`

Your Scope & Implementation Steps:
1. Full Notes System:
   - In `NeuroFlowThemeV2.kt`, provide `CompositionLocalProvider(LocalIsDarkTheme provides true)` so Keep-style note cards render in dark sensory colors instead of pastel.
   - In `QuickNotesBottomSheet.kt`, add `onOpenFullNotes: () -> Unit = {}` and wire header button / recent note click to open full notes.
   - In `NeuroFlowAppV2.kt`, declare `"notes"` route hosting `NotesScreen(onNavigateBack = { navController.popBackStack() })`.
2. Weekly Schedule:
   - In `NeuroFlowAppV2.kt`, declare `"weekly_schedule"` route hosting `WeeklyScheduleScreen(onNavigateBack = { navController.popBackStack() }, onNavigateToDay = { ... }, onNavigateToFocus = { taskId -> navController.navigate("focus_v2/$taskId") })`.
   - In `PlanScreenV2.kt`, add calendar button in top header row to navigate to `"weekly_schedule"`.
3. Deep Analytics & History:
   - In `InsightsScreenV2.kt`, integrate 7d/30d velocity, 24-hr circadian focus density heatmap (`BehavioralInsightsEngine.calculateHourlyCircadianDensity`), and estimation accuracy (sMAPE).
   - Add buttons to open full `"analytics"` and `"history"` screens.
   - In `NeuroFlowAppV2.kt`, declare `"analytics"` and `"history"` routes.
4. Settings Routes:
   - Make `HyperFocusAllowlistRoute` in `NeuroFlowApp.kt` internal/public so it is importable.
   - In `SettingsScreen.kt`, pass callbacks for all settings routes.
   - In `NeuroFlowAppV2.kt`, wire callbacks to navigate to `"priority_weights"`, `"account_sync"`, `"hyperfocus_allowlist"`, `"privacy_permissions"`, and `"app_guide"`.
   - In `NeuroFlowAppV2.kt`, declare composable routes for all 5 settings screens.
5. Identity & WOOP Anchor:
   - In `NeuroFlowAppV2.kt`, declare `"identity"` route hosting `IdentityScreen`.
   - In `InsightsScreenV2.kt`, display active identity affirmations with a button to open `"identity"`.
   - In `FocusScreenV2.kt`, when `state.woopData` is empty/blank, render a 1-tap "Set WOOP Anchor" card opening `WoopPromptSheet` and saving via `FocusViewModelV2.saveWoop`.
   - In `FocusViewModelV2.kt`, implement `saveWoop(wish, outcome, obstacle, plan)`.
6. Retrospective Time Logging:
   - In `NeuroFlowAppV2.kt`, declare `"log_time"` route hosting `LogTimeScreen`.
   - In task options menu/card on Flow and Plan, add "Log Past Time" option opening `ManualTimeLogSheet`.
   - In `FlowViewModelV2.kt` and `PlanViewModelV2.kt`, implement `logPastTime(task, minutes)`.
