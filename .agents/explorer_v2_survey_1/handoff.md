# Handoff Report: neuroFlow V2 Survey for Milestone R1 (100% V1 Feature Parity & Navigation Connectivity)

## 1. Observation

### 1.1 Full Notes System
- **File paths**:
  - `app/src/main/java/com/neuroflow/app/presentation/notes/NotesScreen.kt` (331 lines)
  - `app/src/main/java/com/neuroflow/app/presentation/notes/NoteEditorScreen.kt` (265 lines)
  - `app/src/main/java/com/neuroflow/app/presentation/notes/NotesViewModel.kt` (126 lines)
  - `app/src/main/java/com/neuroflow/app/presentation/notes/NoteColors.kt` (75 lines)
  - `app/src/main/java/com/neuroflow/app/presentation/v2/notes/QuickNotesBottomSheet.kt` (202 lines)
  - `app/src/main/java/com/neuroflow/app/presentation/common/theme/Theme.kt` (line 16: `val LocalIsDarkTheme = staticCompositionLocalOf { false }`)
- **Verbatim code observations**:
  - In `NotesScreen.kt:46`: `val isDark = LocalIsDarkTheme.current`. In `NeuroFlowThemeV2.kt`, `LocalIsDarkTheme` is NOT provided, so `isDark` evaluates to `false` (Light mode) even though the OLED Obsidian theme is active.
  - In `NoteColors.kt:70-73`:
    ```kotlin
    fun getColor(colorId: String?, isDark: Boolean): Color {
        val found = items.find { it.id.equals(colorId, ignoreCase = true) } ?: items.first()
        return if (isDark) found.darkColor else found.lightColor
    }
    ```
    Because `isDark` evaluates to `false`, `NoteColors.getColor(...)` returns light pastel colors (e.g. `Color(0xFFFFF9C4)` Butter, `Color(0xFFF7F7F9)` Default) on top of the dark obsidian background.
  - In `NotesScreen.kt:48-60`: `NotesScreen` hosts `NoteEditorScreen` inline when `uiState.isEditing && uiState.editingNote != null`. No separate sub-navigation is strictly needed for editing.
  - In `QuickNotesBottomSheet.kt:28-32`:
    ```kotlin
    fun QuickNotesBottomSheet(
        notes: List<NoteEntity>,
        onSaveNote: (title: String, content: String) -> Unit,
        onDismiss: () -> Unit
    )
    ```
    There is no callback `onOpenFullNotes: () -> Unit` or `onOpenNote: (NoteEntity) -> Unit`. The recent notes `Surface` (lines 151-193) lacks a click handler.
  - In `FlowScreenV2.kt:326` and `PlanScreenV2.kt:231`, `QuickNotesBottomSheet` is opened but has no pathway to the full Keep-style workspace.
  - In `NeuroFlowAppV2.kt:85-162`: There is NO `"notes"` route registered in `NavHost`.

### 1.2 Weekly Schedule
- **File paths**:
  - `app/src/main/java/com/neuroflow/app/presentation/schedule/WeeklyScheduleScreen.kt` (588 lines)
  - `app/src/main/java/com/neuroflow/app/presentation/schedule/WeeklyScheduleViewModel.kt`
  - `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt` (774 lines)
  - `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt` (254 lines)
- **Verbatim code observations**:
  - `WeeklyScheduleScreen.kt:60-67`:
    ```kotlin
    fun WeeklyScheduleScreen(
        onNavigateBack: () -> Unit,
        onNavigateToDay: (Long) -> Unit,
        onNavigateToFocus: (String) -> Unit,
        hideTopBar: Boolean = false,
        viewModel: WeeklyScheduleViewModel = hiltViewModel()
    )
    ```
    The screen already provides `hideTopBar = false` support for embedded hosting.
  - Components include: 7-day multi-column horizontal timetable, week switcher (`previousWeek()`, `nextWeek()`, `goToCurrentWeek()`), weekly intention banner (`uiState.weeklyIntent`), protected time slots sheet (`ProtectedSlotsSheet`), task click to focus (`onNavigateToFocus`), and day header click (`onNavigateToDay`).
  - In `PlanViewModelV2.kt:29-33`:
    ```kotlin
    enum class PlanViewModeV2(val title: String) {
        TIMELINE("Timeline"),
        MATRIX("Matrix"),
        AUTOPILOT("AI Autopilot")
    }
    ```
    Plan mode segmented tabs only support Timeline, Matrix, and AI Autopilot.
  - In `PlanScreenV2.kt:61-118`: Header bar has Quick Notes and Auto-Plan buttons, but no button to open the Weekly Calendar.
  - In `NeuroFlowAppV2.kt:85-162`: There is NO `"weekly_schedule"` route registered in `NavHost`.

### 1.3 Deep Analytics & History
- **File paths**:
  - `app/src/main/java/com/neuroflow/app/presentation/analytics/AnalyticsScreen.kt` (1705 lines) & `AnalyticsViewModel.kt`
  - `app/src/main/java/com/neuroflow/app/presentation/history/HistoryScreen.kt` (484 lines) & `HistoryViewModel.kt`
  - `app/src/main/java/com/neuroflow/app/domain/engine/AnalyticsEngine.kt` (lines 26-47: `computeSmape`, `computeWeightedSmape`)
  - `app/src/main/java/com/neuroflow/app/domain/scheduler/BehavioralInsightsEngine.kt` (lines 35-43: `calculateHourlyCircadianDensity`)
  - `app/src/main/java/com/neuroflow/app/presentation/v2/insights/InsightsScreenV2.kt` (457 lines) & `InsightsViewModelV2.kt` (147 lines)
- **Verbatim code observations**:
  - `AnalyticsScreen.kt:40-44`:
    ```kotlin
    fun AnalyticsScreen(
        hideTopBar: Boolean = false,
        mode: AnalyticsMode = AnalyticsMode.ALL,
        viewModel: AnalyticsViewModel = hiltViewModel()
    )
    ```
  - `AnalyticsScreen` includes `SmapeCard` (line 665), `SevenDayTrendCard` (line 754), `PeakHourCard` (line 840), `DynamicPeakCard` (line 1030), and `MorningCalibrationCard` (line 1354).
  - `HistoryScreen.kt:40-43`:
    ```kotlin
    fun HistoryScreen(
        hideTopBar: Boolean = false,
        viewModel: HistoryViewModel = hiltViewModel()
    )
    ```
    Includes `HistoryDateRangeFilters` (lines 89-92), `HistoryTagFilters` (lines 95-100), task completion list with undo delete snackbar, and `HistoryDetailSheet`.
  - In `InsightsScreenV2.kt`: Shows Borbély Two-Process card, Circadian Match (hardcoded or based on scheduled time vs peak window), Flow Efficiency, Cognitive Friction & Interruption Radar, Daily & Weekly Momentum, Ulysses Contracts, and Evidence-Based Recommendations.
  - Missing in `InsightsScreenV2`:
    - 7-day / 30-day velocity visualization
    - 24-hour focus distribution / circadian density heatmap (backed by `BehavioralInsightsEngine.calculateHourlyCircadianDensity`)
    - Estimation accuracy (sMAPE and weighted sMAPE)
    - Action buttons or links to open the full `AnalyticsScreen` and `HistoryScreen`.
  - In `NeuroFlowAppV2.kt`: Neither `"analytics"` nor `"history"` is declared in `NavHost`.

### 1.4 Settings Routes
- **File paths**:
  - `app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt` (lines 108-115)
  - `app/src/main/java/com/neuroflow/app/presentation/settings/SettingsScreen.kt` (lines 43-52, 87, 778-890)
  - `app/src/main/java/com/neuroflow/app/presentation/settings/PriorityWeightsScreen.kt`
  - `app/src/main/java/com/neuroflow/app/presentation/settings/AccountSyncScreen.kt`
  - `app/src/main/java/com/neuroflow/app/presentation/settings/PrivacyPermissionsScreen.kt`
  - `app/src/main/java/com/neuroflow/app/presentation/onboarding/AppGuideScreen.kt`
  - `app/src/main/java/com/neuroflow/app/presentation/common/NeuroFlowApp.kt` (lines 493-495, 598-613: `HyperFocusAllowlistRoute`)
- **Verbatim code observations**:
  - In `NeuroFlowAppV2.kt:108-115`:
    ```kotlin
    composable("settings") {
        SettingsScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToPriorityWeights = { },
            onNavigateToMEQQuiz = { navController.navigate("meq_quiz") },
            onNavigateToSleepLogs = { navController.navigate("sleep_logs") }
        )
    }
    ```
  - `onNavigateToPriorityWeights` is a no-op `{ }`.
  - `onNavigateToAccountSync`, `onNavigateToHyperFocusAllowlist`, `onNavigateToAppGuide`, and `onNavigateToPrivacyPermissions` are completely omitted and fall back to empty default parameters.
  - The following routes are missing from `NavHost`:
    - `composable("priority_weights")`
    - `composable("account_sync")`
    - `composable("hyperfocus_allowlist")`
    - `composable("privacy_permissions")`
    - `composable("app_guide")`
  - `HyperFocusAllowlistRoute` is currently private to `presentation/common/NeuroFlowApp.kt:598`. To be accessible in `NeuroFlowAppV2`, it needs to be made public, internal, or placed in a shared navigation/feature file.

### 1.5 Identity & WOOP
- **File paths**:
  - `app/src/main/java/com/neuroflow/app/presentation/identity/IdentityScreen.kt` (181 lines)
  - `app/src/main/java/com/neuroflow/app/presentation/identity/IdentityViewModel.kt` (53 lines)
  - `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt` (lines 196-230)
  - `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt` (lines 80-101)
  - `app/src/main/java/com/neuroflow/app/presentation/focus/WoopPromptSheet.kt` (145 lines)
  - `app/src/main/java/com/neuroflow/app/presentation/focus/MiniWoopReflectionScreen.kt` (165 lines)
- **Verbatim code observations**:
  - `IdentityScreen` manages user subliminal identity affirmations stored in `UserPreferences.affirmations`.
  - In `InsightsScreenV2.kt`, there is no display of identity affirmations or link to `IdentityScreen`.
  - In `NeuroFlowAppV2.kt`, there is no `"identity"` route.
  - In `FocusScreenV2.kt:197-198`:
    ```kotlin
    val woop = state.woopData
    if (woop != null && (woop.wish.isNotBlank() || woop.obstacle.isNotBlank() || woop.plan.isNotBlank())) {
        ...
    }
    ```
    If `woop` is null or all fields are blank, nothing is rendered. There is no affordance for the user to set a WOOP anchor from `FocusScreenV2`.
  - `FocusViewModelV2` already injects `WoopRepository` and loads `woopData`, but lacks a `saveWoop(wish, outcome, obstacle, plan)` function to save WOOP data during an active session.

### 1.6 Retrospective Time Logging
- **File paths**:
  - `app/src/main/java/com/neuroflow/app/presentation/common/ManualTimeLogSheet.kt` (136 lines)
  - `app/src/main/java/com/neuroflow/app/presentation/logtime/LogTimeScreen.kt` (104 lines)
  - `app/src/main/java/com/neuroflow/app/presentation/matrix/MatrixViewModel.kt` (lines 185-207: `logTimeOnly`)
  - `app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt` (`TimelineTaskCard`)
  - `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt` (`TimelineScheduleView`)
- **Verbatim code observations**:
  - In `ManualTimeLogSheet.kt:23-28`:
    ```kotlin
    fun ManualTimeLogSheet(
        taskTitle: String,
        prefillMinutes: Int = 0,
        onConfirm: (Float) -> Unit,
        onSkip: () -> Unit
    )
    ```
  - In `MatrixViewModel.kt:185-207`:
    ```kotlin
    fun logTimeOnly(task: TaskEntity, manualMinutes: Float) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val session = TimeSessionEntity(
                taskId = task.id,
                startedAt = now - (manualMinutes * 60_000f).toLong(),
                endedAt = now,
                durationMinutes = manualMinutes,
                sessionType = "MANUAL_LOG"
            )
            sessionRepository.insert(session)
            taskRepository.update(...)
            preferencesDataStore.updatePreferences { prefs ->
                prefs.copy(totalFocusMinutes = prefs.totalFocusMinutes + manualMinutes.toInt())
            }
        }
    }
    ```
  - Neither `TimelineTaskCard` (Flow) nor `TimelineScheduleView` (Plan) offers a "Log Past Time" action or button.
  - In `NeuroFlowAppV2.kt`, there is no `"log_time"` route.

### 1.7 Unit Tests Survey
- **Test execution command**:
  `./gradlew testDebugUnitTest --tests "com.neuroflow.app.presentation.v2.NeuroFlowV2UiTest" --no-daemon -Dorg.gradle.workers.max=1`
- **Result**: `BUILD SUCCESSFUL in 36s`, 0 failures, 100% pass rate.
- **Coverage breakdown**:
  - `NoteRepositoryTest.kt`: Tests search, save, toggle pin, update color, delete note.
  - `NavHubsTest.kt`: Tests V1 `NavHub` enum, ALL list, and deep link resolver. No counterpart currently exists for `NavHubsV2` or `NeuroFlowAppV2` routes.
  - `HistoryDateRangeFilterTest.kt`: Tests date range filtering logic for `ALL`, `TODAY`, `LAST_7_DAYS`, `LAST_30_DAYS`.
  - `AnalyticsEngineTest.kt`: Tests `computeSmape`, `computeWeightedSmape`, streak calculation, and velocity.
  - `SettingsViewModelPreferencePersistenceTest.kt`: Tests preference persistence when modifying theme and toggles.
  - `NeuroFlowV2UiTest.kt`: Tests `FlowViewModelV2`, `PlanViewModelV2`, `FocusViewModelV2`, and `InsightsViewModelV2`.
- **Gaps identified**:
  - No test verifies route connectivity or deep link dispatching in `NeuroFlowAppV2`.
  - No test covers retrospective time logging in `FlowViewModelV2` or `PlanViewModelV2`.
  - No test covers setting a WOOP anchor from `FocusViewModelV2` when initially empty.

---

## 2. Logic Chain

1. **Premise**: Milestone R1 mandates 100% V1 feature parity, sensory OLED obsidian styling, and zero dead routes in `NeuroFlowAppV2`.
2. **Notes Parity**:
   - `NotesScreen` and `NoteEditorScreen` exist and function in V1 with search, pinned items, color customization, and delete confirmation.
   - However, `NeuroFlowThemeV2` does not provide `LocalIsDarkTheme provides true`, leading `NotesScreen` and `NoteColors` to choose light theme colors on OLED backgrounds. Providing `LocalIsDarkTheme provides true` in `NeuroFlowThemeV2` ensures dark note cards (`#1E1E24`, etc.).
   - `QuickNotesBottomSheet` lacks a hook to open the full notes view. Adding an "Open Workspace" icon/button calling `onOpenFullNotes` and wiring it to navigate to `"notes"` connects the scratchpad to the full Keep-style workspace.
3. **Weekly Schedule Parity**:
   - `WeeklyScheduleScreen` is a multi-column visual time-blocking calendar that already includes a `hideTopBar` option.
   - Adding a `"weekly_schedule"` composable to `NeuroFlowAppV2` and providing an entry point from `PlanScreenV2` (via a calendar week header icon and/or a `"Week"` tab in `PlanViewModeV2`) provides parity.
4. **Deep Analytics & History Parity**:
   - `AnalyticsScreen` and `HistoryScreen` are functional in V1 and both support `hideTopBar = false`.
   - `InsightsScreenV2` has summary metrics (sleep pressure, streak, ulysses contracts) but lacks 7d/30d velocity trends, hourly 24-bin focus density (which `BehavioralInsightsEngine` already calculates), and sMAPE accuracy cards.
   - Embedding these cards into `InsightsScreenV2` while providing clear navigation links to full `"analytics"` and `"history"` routes restores full visibility.
5. **Settings Routes Parity**:
   - `SettingsScreen` already has UI rows and click handlers for `PriorityWeights`, `AccountSync`, `HyperFocusAllowlist`, `PrivacyPermissions`, and `AppGuide`.
   - `NeuroFlowAppV2` passed empty lambdas or omitted these parameters, and did not declare the destination routes in `NavHost`.
   - Adding the 5 routes and wiring their navigation callbacks resolves all dead routes in settings.
6. **Identity & WOOP Parity**:
   - Affirmations are stored in `UserPreferences.affirmations` and managed by `IdentityViewModel`. Displaying them in `InsightsScreenV2` and registering route `"identity"` integrates affirmations.
   - `FocusScreenV2` hides the WOOP card when `woopData` is empty. Adding a 1-tap "Set WOOP Anchor" card opening a WOOP sheet and saving via `FocusViewModelV2.saveWoop(...)` fulfills the anti-paralysis requirement.
7. **Retrospective Time Logging Parity**:
   - `ManualTimeLogSheet` already implements the number-picker UI.
   - Adding a "Log Past Time" option to task items on Flow/Plan and implementing `logPastTime(task, minutes)` in the ViewModels enables off-device tracking.

---

## 3. Caveats

- **Network Mode**: The investigation was conducted locally without internet access (offline code analysis). All findings are derived directly from the local repository.
- **V1 vs V2 View Models**: Classic V1 `ScheduleScreen` and `ScheduleViewModel` rely on legacy date pagination, whereas V2 `PlanViewModelV2` uses millisecond timestamps. When opening `WeeklyScheduleScreen`, passing `onNavigateToDay = { dateMillis -> ... }` allows smooth round-tripping to `PlanScreenV2` without state desynchronization.
- **Theme Composition Local**: `LocalIsDarkTheme` is declared in `com.neuroflow.app.presentation.common.theme.Theme.kt`. Any V2 composable relying on `LocalIsDarkTheme` needs `CompositionLocalProvider(LocalIsDarkTheme provides true)` inside `NeuroFlowThemeV2`.
- **HyperFocus Visibility**: `HyperFocusAllowlistRoute` was private to `NeuroFlowApp.kt`. It must be shared or made accessible to `NeuroFlowAppV2`.

---

## 4. Conclusion

The neuroFlow V1 feature suite is substantially intact in code, but disconnected from `NeuroFlowAppV2`. The exact gaps for Milestone R1 are:
1. **Notes**: Missing `"notes"` route in `NeuroFlowAppV2`, missing `onOpenFullNotes` callback in `QuickNotesBottomSheet`, and missing `LocalIsDarkTheme provides true` in `NeuroFlowThemeV2`.
2. **Weekly Schedule**: Missing `"weekly_schedule"` route in `NeuroFlowAppV2` and missing navigation trigger in `PlanScreenV2`.
3. **Analytics & History**: Missing `"analytics"` and `"history"` routes in `NeuroFlowAppV2`, and missing velocity, hourly heatmap, and sMAPE integration in `InsightsScreenV2`.
4. **Settings**: Five missing routes (`priority_weights`, `account_sync`, `hyperfocus_allowlist`, `privacy_permissions`, `app_guide`) and dead-end lambdas in `NeuroFlowAppV2`.
5. **Identity & WOOP**: Missing `"identity"` route, missing affirmation card in `InsightsScreenV2`, and missing "Set WOOP Anchor" sheet trigger on `FocusScreenV2` when empty.
6. **Retrospective Time Logging**: Missing "Log Past Time" option on V2 task cards, missing `logPastTime` handler in ViewModels, and missing `"log_time"` route.

---

## 5. Verification Method

### Independent Verification Commands:
1. **Targeted V2 UI Test**:
   ```bash
   ./gradlew testDebugUnitTest --tests "com.neuroflow.app.presentation.v2.NeuroFlowV2UiTest" --no-daemon -Dorg.gradle.workers.max=1
   ```
2. **Full Unit Test Suite**:
   ```bash
   ./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1
   ```
3. **File Inspections**:
   - Inspect `app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt` lines 85-162 to verify all required routes (`notes`, `weekly_schedule`, `analytics`, `history`, `priority_weights`, `account_sync`, `hyperfocus_allowlist`, `privacy_permissions`, `app_guide`, `identity`, `log_time`) are declared.
   - Inspect `app/src/main/java/com/neuroflow/app/presentation/v2/notes/QuickNotesBottomSheet.kt` to verify `onOpenFullNotes` callback exists and is invoked from the UI.
   - Inspect `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt` to verify the "Set WOOP Anchor" button appears when `woopData` is empty.
   - Inspect `app/src/main/java/com/neuroflow/app/presentation/v2/theme/NeuroFlowThemeV2.kt` to verify `LocalIsDarkTheme provides true` is wrapped in `CompositionLocalProvider`.

### Invalidation Conditions:
- If `NeuroFlowAppV2` contains routes that throw `IllegalArgumentException` on navigation.
- If notes render with light pastel backgrounds when viewed in V2 OLED mode.
- If tapping "Priority Weights" or other settings rows does not navigate to their respective screens.
- If `./gradlew testDebugUnitTest` fails on any test.
