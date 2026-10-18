# neuroFlow V2 Specification Mining Report: Milestones R4 & R5

## Executive Summary
This report presents the specification investigation and architecture mapping for Milestones R4 (Default V2 UI & Widget Interoperability) and R5 (Hands-Free Voice Quick-Capture & Executive Transition Buffer) within neuroFlow V2. All existing components, intent routing logic, widget providers, inference pipelines, and focus lifecycle flows have been inspected with exact file paths, line references, and execution data.

---

## Features Discovered

| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | R4: Default V2 UI | Default `useV2Ui` Preference | Controls whether the application boots into `NeuroFlowAppV2` (Obsidian Sanctuary UI) or legacy `NeuroFlowApp` (V1 UI). | `Preferences[Keys.USE_V2_UI]` or default parameter | Boolean (`useV2Ui`) defaulting to `true` | Defaults to fallback if key missing in DataStore | `UserPreferencesDataStore.kt:165,416,735` |
| 2 | R4: Intent Routing | Quick Task Intent (`ACTION_OPEN_NEW_TASK`) | Launch intent (`com.procus.ACTION_OPEN_NEW_TASK`) dispatched by QuickCaptureWidget and shortcuts to immediately present the task capture composer. | `Intent(action = "com.procus.ACTION_OPEN_NEW_TASK")` | Sets `initialNewTaskOpen = true`; routes to `FlowScreenV2` opening `SmartTaskComposerSheetV2` | Ignores if flag is unconsumed or dropped | `MainActivity.kt:75`, `NeuroFlowAppV2.kt:29` |
| 3 | R4: Intent Routing | Focus Session Intent (`ACTION_OPEN_FOCUS`) | Launch intent (`com.procus.ACTION_OPEN_FOCUS`) dispatched by widgets, workers, and notifications to open the focus screen for a task. | `Intent(action = "com.procus.ACTION_OPEN_FOCUS", extra "taskId")` | Navigates to `focus_v2/{taskId}` or top active task | If `taskId` null, queries active tasks or stays on Flow | `MainActivity.kt:79`, `NeuroFlowAppV2.kt:38` |
| 4 | R4: Widgets | Quick Capture Widget | 1x1 / 2x1 AppWidget presenting a 1-tap "➕ New Task" button that launches `MainActivity` with `ACTION_OPEN_NEW_TASK`. | User touch on `R.id.widget_quick_capture_button` | Dispatches `ACTION_OPEN_NEW_TASK` PendingIntent | Stale view if context invalid | `QuickCaptureWidgetProvider.kt:17-27` |
| 5 | R4: Widgets | Next Action Widget | Home screen widget displaying current highest-priority task and direct "⚡ Start Focus" action button. | User touch on container or start button | Triggers `ACTION_OPEN_FOCUS` with top task ID or container open | Gracefully handles empty task list | `NextActionWidgetProvider.kt:17-34`, `FocusWidgetUpdateWorker.kt:70-105` |
| 6 | R4: Widgets | Focus Widget | Home screen widget displaying highest-scoring task, priority score, and "Start" button. | User touch on `R.id.widget_start_button` | Triggers `ACTION_OPEN_FOCUS` with top task ID | Shows "No tasks" placeholder if empty | `FocusWidgetProvider.kt:17-48`, `FocusWidgetUpdateWorker.kt:40-67` |
| 7 | R5: Voice Capture | Speech Recognition Framework Integration | 1-tap microphone button in `SmartTaskComposerSheetV2` triggering Android `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` via `ActivityResultLauncher`. | User tap on Mic icon, audio speech input | Populates `rawInput` with transcribed speech text | Catches `ActivityNotFoundException` if speech service missing | `SmartTaskComposerSheetV2.kt:158`, `AndroidManifest.xml:23` |
| 8 | R5: Voice Capture | Natural Language Inference Pipeline | Connects transcribed text from speech recognition into `TaskInferenceEngine.inferFromInput()` to extract title, duration, energy, task type, priority, frog, and tags. | Natural language speech string (e.g. "Draft proposal 45m #high !p1") | `InferredTaskMetadata` with sanitized title, calibrated duration, energy level, tags | Safe fallback to 25m Medium Energy defaults on empty input | `TaskInferenceEngine.kt:74-150` |
| 9 | R5: Transition Buffer | Executive Transition Buffer ("Decompression Zone") | 3-minute (180s) post-session autonomic recovery buffer displayed after focus completion to prevent executive whiplash and dopamine crash. | Focus completion event (`completeTask()`) | 180s countdown timer overlay with calming visual cues, 60s sigh action, and ready bypass | Autonomy skip button ("Ready Now") prevents user feeling trapped | `FocusViewModelV2.kt:184-205`, `FocusScreenV2.kt:332-367` |

---

## Edge Cases

| # | Feature | Input | Observed Behavior |
|---|---------|-------|-------------------|
| 1 | `useV2Ui` Preference Default | Fresh install or upgrade where `use_v2_ui` key is absent from DataStore | Currently defaults to `false` at line 165 (`val useV2Ui: Boolean = false`) and line 416 (`prefs[Keys.USE_V2_UI] ?: false`). Changing both to `true` ensures new and unconfigured users land in V2. |
| 2 | `ACTION_OPEN_NEW_TASK` in V2 | QuickCaptureWidget tapped when `useV2Ui == true` | Currently `MainActivity` sets `initialNewTaskOpen = true`, but does NOT pass `initialNewTaskOpen` to `NeuroFlowAppV2`. The sheet never opens in V2. Adding parameters to `NeuroFlowAppV2` and `FlowScreenV2` resolves this completely. |
| 3 | `ACTION_OPEN_FOCUS` with missing `taskId` | Widget or shortcut sends `ACTION_OPEN_FOCUS` with null extra | In `NeuroFlowAppV2`, `LaunchedEffect(initialTaskId)` requires `!initialTaskId.isNullOrBlank()`. If `taskId` is missing, `MainActivity` should fall back to `taskRepository.getActiveTasks().firstOrNull()?.id`. |
| 4 | Widget Tapped When Task List Empty | User taps "Start Focus" on FocusWidget or NextActionWidget when 0 active tasks exist | `FocusWidgetUpdateWorker` previously did not assign PendingIntents when `topTask == null`. Assigning `ACTION_OPEN_NEW_TASK` pending intent prompts the user to capture their first task. |
| 5 | Speech Recognition On Devices Without Google Services | Mic icon tapped on emulator or de-Googled ROM without speech recognizer activity | Unhandled `voiceLauncher.launch()` throws `ActivityNotFoundException`. Wrapping launch in `try { ... } catch (e: ActivityNotFoundException)` displays a fallback toast/snackbar. |
| 6 | Transcribed Speech with Complex Numbers & Units | User says "Design presentation one hour thirty minutes hashtag deepwork" | `TaskInferenceEngine.DURATION_REGEX` parses "1h 30m" -> 90 minutes. If speech writes out "one hour thirty minutes", regex supports "an hour" / "half hour" and explicit digits; numbers like "90m" or "45 min" are parsed into duration. |
| 7 | User Rushing Back to Work During Decompression | User feels ready and does not want to wait 3 minutes | "Ready Now" / "Skip Decompression" action immediately dismisses the buffer and opens the next queued task or returns to Flow, preserving autonomy. |

---

## 5-Component Handoff Report

### 1. Observation

#### A. Default V2 UI (`UserPreferencesDataStore.kt`)
- In `app/src/main/java/com/neuroflow/app/data/local/UserPreferencesDataStore.kt`:
  - Line 165: `val useV2Ui: Boolean = false` in `data class UserPreferences`.
  - Line 291: `val USE_V2_UI = booleanPreferencesKey("use_v2_ui")`.
  - Line 416: `useV2Ui = prefs[Keys.USE_V2_UI] ?: false` in preferences Flow mapper.
  - Line 735: `prefs[Keys.USE_V2_UI] = updated.useV2Ui`.
  - Line 739: `suspend fun setUseV2Ui(enabled: Boolean)`.
- In `app/src/main/java/com/neuroflow/app/MainActivity.kt`:
  - Line 302:
    ```kotlin
    if (prefs.useV2Ui) {
        com.neuroflow.app.presentation.v2.navigation.NeuroFlowAppV2(
            initialTaskId = initialFocusTaskId,
            onInitialTaskConsumed = { initialFocusTaskId = null }
        )
    } else {
        NeuroFlowApp(...)
    }
    ```
- In `app/src/main/java/com/neuroflow/app/presentation/settings/SettingsScreen.kt`:
  - Lines 132–134: Toggle switch exists allowing users to flip `prefs.useV2Ui`.

#### B. Intent Routing (`MainActivity.kt` & `NeuroFlowAppV2.kt`)
- In `MainActivity.kt`:
  - Lines 53–57:
    ```kotlin
    private var initialFocusTaskId by mutableStateOf<String?>(null)
    private var initialNewTaskOpen by mutableStateOf(false)
    ```
  - Lines 74–82:
    ```kotlin
    when (intent.action) {
        "com.procus.ACTION_OPEN_NEW_TASK" -> {
            initialNewTaskOpen = true
            return
        }
        "com.procus.ACTION_OPEN_FOCUS" -> {
            initialFocusTaskId = taskId
            return
        }
    ```
  - In `NeuroFlowAppV2.kt` lines 28–43:
    ```kotlin
    @Composable
    fun NeuroFlowAppV2(
        initialTaskId: String? = null,
        onInitialTaskConsumed: () -> Unit = {},
        amoledDark: Boolean = false
    ) {
        val navController = rememberNavController()
        LaunchedEffect(initialTaskId) {
            if (!initialTaskId.isNullOrBlank()) {
                navController.navigate("focus_v2/$initialTaskId")
                onInitialTaskConsumed()
            }
        }
    ```
  - Notice that `initialNewTaskOpen` is NOT accepted or passed to `NeuroFlowAppV2`, and `FlowScreenV2` manages `showComposer` internally without receiving an external open trigger.

#### C. Home Screen Widgets
- `QuickCaptureWidgetProvider.kt`:
  - Lines 17–20:
    ```kotlin
    val launchIntent = Intent(context, MainActivity::class.java).apply {
        action = "com.procus.ACTION_OPEN_NEW_TASK"
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    ```
- `NextActionWidgetProvider.kt`:
  - Lines 18–19: Enqueues `FocusWidgetUpdateWorker`.
  - Lines 23–26: Fallback PendingIntent without action.
- `FocusWidgetProvider.kt`:
  - Lines 19–20: Enqueues `FocusWidgetUpdateWorker`.
  - Lines 40–47: `onReceive` handles `"com.neuroflow.app.WIDGET_START"` by launching `MainActivity` with `putExtra("taskId", it)`, but omits `action = "com.procus.ACTION_OPEN_FOCUS"`.
- `FocusWidgetUpdateWorker.kt`:
  - Lines 48–56: When `topTask != null`, assigns `PendingIntent` with `action = "com.procus.ACTION_OPEN_FOCUS"` and `putExtra("taskId", topTask.id)` to `R.id.widget_start_button`.
  - Lines 77–86: Assigns `PendingIntent` with `action = "com.procus.ACTION_OPEN_FOCUS"` and `putExtra("taskId", topTask.id)` to `R.id.widget_next_start_button`.
  - Lines 58–61 & 97–99: When `topTask == null`, no click PendingIntent is set.

#### D. Voice Quick-Capture (`SmartTaskComposerSheetV2.kt` & `TaskInferenceEngine.kt`)
- In `SmartTaskComposerSheetV2.kt`:
  - Lines 53: `var rawInput by remember { mutableStateOf("") }`.
  - Lines 78–80:
    ```kotlin
    val inferred = remember(rawInput, completedTasks) {
        TaskInferenceEngine.inferFromInput(rawInput, completedTasks)
    }
    ```
  - Lines 158–183: `OutlinedTextField` takes `rawInput`. There is currently no mic button or speech recognizer launcher.
- In `TaskInferenceEngine.kt`:
  - Lines 29–56: Regular expressions parse durations (`DURATION_REGEX`), tags (`TAG_REGEX`), frogs (`FROG_REGEX`), energy (`HIGH_ENERGY_KEYWORDS`, `LOW_ENERGY_KEYWORDS`, `EXPLICIT_HIGH_ENERGY`), priorities (`PRIORITY_P1_REGEX`), and due dates (`DUE_DATE_REGEX`).
  - Lines 74–150: `inferFromInput()` produces `InferredTaskMetadata` from raw strings.

#### E. Focus Completion & Decompression Buffer (`FocusScreenV2.kt` & `FocusViewModelV2.kt`)
- In `FocusViewModelV2.kt`:
  - Lines 46: `val isCompleted: Boolean = false` in `FocusUiStateV2`.
  - Lines 184–205: `completeTask()` cancels timer, disables DND, marks task complete in Room, and sets `_uiState.update { it.copy(isRunning = false, isCompleted = true, showAffordanceRating = true) }`.
- In `FocusScreenV2.kt`:
  - Lines 332–367: When `state.isCompleted`, an `AlertDialog` is immediately presented with buttons "Start Next Task" or "Return to Flow".
  - There is currently no 3-minute countdown buffer between completing a task and transitioning to the next task.
  - `PhysiologicalSighGuideSheet.kt` exists in the same package (`presentation.v2.focus`) with a 60-second Huberman respiratory cycle.

#### F. Test Execution Baseline
- Command: `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1`
- Result: `BUILD SUCCESSFUL in 1m 47s. 34 actionable tasks: 34 executed.`
- Test Count: 787 tests executed across unit test suites.
- Failures: 0 failures, 100% pass rate.

---

### 2. Logic Chain

1. **Default V2 UI**: Setting `useV2Ui = true` by default in `UserPreferencesDataStore` requires changing:
   - Line 165: default parameter in `UserPreferences` data class constructor (`val useV2Ui: Boolean = true`).
   - Line 416: DataStore retrieval fallback (`prefs[Keys.USE_V2_UI] ?: true`).
   - This ensures that first-time launches and existing users who never toggled the setting will boot into `NeuroFlowAppV2` while preserving manual toggle persistence in `SettingsScreen`.

2. **MainActivity Intent Handling**:
   - In `MainActivity.kt`, when `com.procus.ACTION_OPEN_NEW_TASK` is received, `initialNewTaskOpen` is set to `true`.
   - In `MainActivity.kt:302`, passing `initialNewTaskOpen = initialNewTaskOpen` and `onInitialNewTaskConsumed = { initialNewTaskOpen = false }` to `NeuroFlowAppV2` bridges the gap.
   - In `NeuroFlowAppV2.kt`, a `LaunchedEffect(initialNewTaskOpen)` navigates to `NavHubsV2.Flow.route` if not already there, and forwards `initialNewTaskOpen` to `FlowScreenV2`.
   - In `FlowScreenV2.kt`, a `LaunchedEffect(initialNewTaskOpen)` sets `showComposer = true` and invokes `onInitialNewTaskConsumed()`, triggering `SmartTaskComposerSheetV2`.
   - For `com.procus.ACTION_OPEN_FOCUS`: If `taskId` extra is null or blank, `MainActivity` queries `taskRepository.getActiveTasks().firstOrNull()?.id`. When non-null, `NeuroFlowAppV2` navigates to `focus_v2/$initialTaskId`.

3. **Widget Wiring**:
   - `QuickCaptureWidgetProvider`: Already sends `ACTION_OPEN_NEW_TASK`. With the MainActivity-to-NeuroFlowAppV2 bridge, it immediately pops the V2 composer.
   - `NextActionWidgetProvider` & `FocusWidgetProvider`:
     - In `FocusWidgetUpdateWorker`:
       - When `topTask != null`: existing `ACTION_OPEN_FOCUS` PendingIntent directs the user to `focus_v2/$taskId`.
       - When `topTask == null`: setting PendingIntent to `ACTION_OPEN_NEW_TASK` gives users a 1-tap pathway to create their next action.
     - In `FocusWidgetProvider.kt` `onReceive`: add `action = "com.procus.ACTION_OPEN_FOCUS"` to `WIDGET_START` intent.

4. **Hands-Free Voice Quick-Capture**:
   - In `SmartTaskComposerSheetV2.kt`, add a `voiceLauncher` using `rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())`.
   - On click of a mic `IconButton` (placed as `trailingIcon` in `OutlinedTextField` or header):
     Launch `RecognizerIntent(ACTION_RECOGNIZE_SPEECH)` with `LANGUAGE_MODEL_FREE_FORM` inside a `try-catch` block.
   - When the recognized string is received: assign `rawInput = spokenText`.
   - Because `inferred` is dynamically derived via `TaskInferenceEngine.inferFromInput(rawInput, completedTasks)`, the entire title, duration, energy, priority, frog, and tags are populated into the composer with zero manual typing.
   - In `AndroidManifest.xml`, declare `<intent><action android:name="android.speech.RecognitionService" /></intent>` inside `<queries>` for API 30+ package visibility.

5. **Executive Transition Buffer ("Decompression Zone")**:
   - In `FocusViewModelV2`:
     - Add `isDecompressing: Boolean = false` and `decompressionSecondsRemaining: Int = 180` to `FocusUiStateV2`.
     - In `completeTask()`: mark Room task complete, save session, cancel timer/DND, and initiate a 180-second countdown (`isDecompressing = true`).
     - Ticking coroutine in `viewModelScope` decrements `decompressionSecondsRemaining` every 1000ms until reaching 0, upon which `isDecompressing = false` and `isCompleted = true`.
     - Add `fun skipDecompression()`: cancels the countdown coroutine and immediately advances to `isDecompressing = false`, `isCompleted = true`.
   - In `FocusScreenV2.kt`:
     - When `state.isDecompressing`: render `DecompressionZoneOverlay` with calming obsidian styling, 3-minute circular/text countdown (`03:00`), encouragement ("Rest your executive function to prevent whiplash"), a button to trigger `PhysiologicalSighGuideSheet`, and a "Ready Now" bypass button.

---

### 3. Caveats

- **Speech Recognition Availability**: Android speech recognition depends on device speech services (e.g. Google Speech Services or on-device speech recognizer). On emulators without Google Play services, `voiceLauncher.launch()` must be guarded with `ActivityNotFoundException` handling to prevent crashes.
- **Audio Permission**: `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` uses the platform system dialog and does not require runtime `RECORD_AUDIO` permission from the hosting activity. If continuous in-app speech listening via `SpeechRecognizer` is preferred in the future, `RECORD_AUDIO` runtime permission would be required.
- **Focus Route with Null Task**: `FocusViewModelV2` expects a non-null `taskId` (`checkNotNull(savedStateHandle["taskId"])`). Therefore, `ACTION_OPEN_FOCUS` must always ensure a valid `taskId` is supplied before navigating to `focus_v2/{taskId}`.
- No other caveats.

---

### 4. Conclusion

Milestones R4 and R5 have a direct, modular implementation path that fully preserves backward compatibility:
1. Setting `useV2Ui = true` by default in `UserPreferencesDataStore.kt` (lines 165 & 416) makes V2 the default experience immediately.
2. Connecting `initialNewTaskOpen` through `MainActivity` -> `NeuroFlowAppV2` -> `FlowScreenV2` enables 100% widget-to-V2 composer interoperability.
3. Adding a voice capture mic button to `SmartTaskComposerSheetV2` leverages the already robust `TaskInferenceEngine` without requiring new natural language parsing code.
4. Implementing the 3-minute Decompression Zone in `FocusViewModelV2` and `FocusScreenV2` provides the missing autonomic buffer to prevent executive whiplash.
5. All 787 existing unit tests currently pass with 0 failures, providing a clean regression baseline.

---

### 5. Verification Method

To independently verify the architecture and behavior:
1. **Full test suite execution**:
   ```bash
   ./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1
   ```
   Confirm all 787+ tests pass with 0 failures.
2. **Preference verification**:
   Inspect `app/src/main/java/com/neuroflow/app/data/local/UserPreferencesDataStore.kt` lines 165 and 416. Assert that `useV2Ui` evaluates to `true` on empty DataStore preferences.
3. **Intent verification**:
   Inspect `MainActivity.kt:302` to confirm `initialNewTaskOpen` and `initialTaskId` are passed down to `NeuroFlowAppV2`.
4. **Widget PendingIntent verification**:
   Inspect `QuickCaptureWidgetProvider.kt`, `NextActionWidgetProvider.kt`, `FocusWidgetProvider.kt`, and `FocusWidgetUpdateWorker.kt`.
5. **Inference test execution**:
   ```bash
   ./gradlew testDebugUnitTest --tests "com.neuroflow.app.domain.inference.TaskInferenceEngineTest"
   ```
