# Modernization Reconnaissance Report: UI & Architecture (R2 & R3)

**Author:** UI and Architecture Explorer  
**Target:** neuroFlow Modernization Project  
**Date:** 2026-10-16  
**Status:** Complete Reconnaissance & Synthesis  

---

## 1. Observation

Direct observations from the codebase reconnaissance:

### 1.1 Task Creation & Inference (Requirement R2)

1. **Monolithic Task Creation Dialog (`NewTaskSheet.kt`)**:
   - Location: `app/src/main/java/com/neuroflow/app/presentation/common/NewTaskSheet.kt` (1,612 lines).
   - In lines 187–192, despite the name `NewTaskSheet`, the component is instantiated as a full-screen `Dialog`:
     ```kotlin
     Dialog(onDismissRequest = { requestDismiss() }) {
         Surface(
             modifier = Modifier.fillMaxSize(),
             color = MaterialTheme.colorScheme.surface,
             shape = RoundedCornerShape(0.dp)
         ) { ... }
     }
     ```
   - It contains a massive scrollable form with dozens of controls: Task Title (lines 232–246), When chips (lines 249–302), Duration chips (lines 305–325), Frog & Impact chips (lines 328–352), Eisenhower Quadrant chips (lines 355–374), Primary Save CTA (lines 377–393), and an accordion titled "More Details & Options" (line 415) housing Description, Priority dropdown, Energy level chips, Task type chips, Timing & Reminders, Effort sliders, Execution & Dependencies, Tags step, Auto-scheduling review, and a duplicate Save button (lines 589–601).
   - Zero parsing of raw title input exists anywhere in this sheet.

2. **Wizard State Remains in `NewTaskFlowViewModel.kt`**:
   - Location: `app/src/main/java/com/neuroflow/app/presentation/common/NewTaskFlowViewModel.kt` (493 lines).
   - Contains legacy multi-step wizard state machine logic (`visibleSteps()`, `nextStep()`, `previousStep()`, `NewTaskFlowStep` enum in lines 203–254) even though `NewTaskSheet` was converted to a single scrolling page with an accordion.

3. **Naïve Quick Brain Dump in `TodayScreen.kt` & `TodayViewModel.kt`**:
   - Location: `app/src/main/java/com/neuroflow/app/presentation/today/TodayScreen.kt` lines 198–231 (`OutlinedTextField` with placeholder `"Quick thought or task... (press Enter)"`).
   - Location: `app/src/main/java/com/neuroflow/app/presentation/today/TodayViewModel.kt` lines 327–345:
     ```kotlin
     fun quickBrainDump(title: String) {
         if (title.isBlank()) return
         viewModelScope.launch {
             val cal = Calendar.getInstance().apply {
                 set(Calendar.HOUR_OF_DAY, 0)
                 set(Calendar.MINUTE, 0)
                 set(Calendar.SECOND, 0)
                 set(Calendar.MILLISECOND, 0)
             }
             val newTask = TaskEntity(
                 title = title.trim(),
                 quadrant = Quadrant.SCHEDULE,
                 scheduledDate = cal.timeInMillis,
                 impactScore = 50,
                 status = TaskStatus.ACTIVE
             )
             taskRepository.insert(newTask)
         }
     }
     ```
   - No inference is performed on `title`. It sets `quadrant = Quadrant.SCHEDULE`, `impactScore = 50`, `scheduledDate = today`, and leaves duration at `0`, energy level at default `MEDIUM`, task type at default `ANALYTICAL`, and tags empty.

4. **Existing Taxonomy & Heuristics Disconnected from Creation**:
   - Location: `app/src/main/java/com/neuroflow/app/domain/scheduler/TaskTagSchedulingProfile.kt`.
   - Lines 40–123 define `TaskTagSchedulingProfile` with 33 starter tags ("study", "chores", "deep work", "admin", "coding", "exercise", "mindfulness", etc.), mapping each to `TagEnergyDemand` (LOW, MEDIUM, HIGH), `preferredContext` ("@computer", "@home", "@work", "@errands"), `preferredWindow`, and `TaskCategory`.
   - Lines 125–187 define `TaskEntity.determineCategory()` using tag keyword matching (e.g., `"coding", "programming", "dev", "study"` -> `HARD_WORK`).
   - **Crucial observation**: This logic operates exclusively on pre-existing tags (`task.tags.split(',')`) after task persistence, never on the raw user input text during quick task creation.

---

### 1.2 Design System & Theming (Requirement R3)

1. **Theme Definitions & AMOLED Failure**:
   - Location: `app/src/main/java/com/neuroflow/app/presentation/common/theme/Theme.kt` (lines 72–115) and `Color.kt` (lines 5–54).
   - In `Theme.kt`, lines 96–104:
     ```kotlin
     val colorScheme = if (amoledDark && darkTheme) {
         baseColorScheme.copy(
             background = Color.Black,
             surface = Color.Black,
             surfaceVariant = Color(0xFF121212)
         )
     } else {
         baseColorScheme
     }
     ```
   - In `Color.kt`, lines 31–36, pastel constants are defined:
     ```kotlin
     val FocusBg       = Color(0xFFE8F5E9) // Light pastel green
     val DeadlineCard    = Color(0xFFFFEBEE) // Light pastel pink
     val ScheduledCard   = Color(0xFFE3F2FD) // Light pastel blue
     val PomodoroCard    = Color(0xFFF3E5F5) // Light pastel purple
     ```
   - Missing `Shape.kt`: There is no centralized shape token definition anywhere in the project. Random corner radii (`8.dp`, `10.dp`, `12.dp`, `14.dp`, `16.dp`, `20.dp`) are hardcoded per screen.

2. **Severe Contrast-Breaking Colors in `FocusScreen.kt`**:
   - Location: `app/src/main/java/com/neuroflow/app/presentation/focus/FocusScreen.kt`.
   - Line 186 & Line 205:
     ```kotlin
     val focusBg = NeuroFlowColors.FocusBg
     ...
     Column(
         modifier = Modifier
             .fillMaxSize()
             .padding(innerPadding)
             .background(focusBg) // Hardcoded 0xFFE8F5E9 light green background!
     ```
   - In Dark and AMOLED modes, the entire background of `FocusScreen` remains bright light pastel green (`#E8F5E9`), blinding users and breaking dark mode.
   - Lines 635, 662, 833:
     - Deadline card: `containerColor = deadlineCardColor` (`0xFFFFEBEE` - pastel pink).
     - Scheduled card: `containerColor = scheduledCardColor` (`0xFFE3F2FD` - pastel blue).
     - Pomodoro card: `containerColor = pomodoroCardColor` (`0xFFF3E5F5` - pastel purple).
   - Hardcoded container and text colors throughout `FocusScreen.kt`:
     - Line 409: `containerColor = Color(0xFFE3F2FD)`, lines 413–416: text `Color(0xFF1565C0)`, `Color(0xFF0D47A1)`.
     - Line 435: `containerColor = Color(0xFFFFF8E1)`, lines 439–441: text `Color(0xFFF57F17)`, `Color(0xFFE65100)`.
     - Line 457: `containerColor = Color(0xFFF3E5F5)`, lines 461–463: text `Color(0xFF6A1B9A)`, `Color(0xFF4A148C)`.
     - Line 477: `containerColor = Color(0xFFE8F5E9)`, lines 484–518: text `Color(0xFF2E7D32)`, `Color(0xFF1B5E20)`.
     - Line 537: `containerColor = Color(0xFFFFF3E0)`, lines 545–547: text `Color(0xFFE65100)`, `Color(0xFFBF360C)`.
     - Line 572: `containerColor = Color(0xFFFFEBEE)`, line 579: text `Color(0xFFB71C1C)`.

3. **Hardcoded Contrast Breakage in `TodayScreen.kt` & `TaskRow.kt`**:
   - Location: `app/src/main/java/com/neuroflow/app/presentation/today/TodayScreen.kt`:
     - Lines 673–675: `SlateCard = Color(0xFF1E293B)`, `SlateMuted = Color(0xFF94A3B8)`, `SlateTrack = Color(0xFF334155)`.
     - Line 873: `containerColor = Color(0xFF1E293B)` (hardcoded dark card, even in Light theme).
     - Line 144: FAB `containerColor = NeuroFlowColors.Purple, contentColor = Color.White`.
     - Lines 283 & 471: `NeuroFlowColors.DoFirstText` (`0xFFC62828`) and `NeuroFlowColors.ScheduleText` (`0xFF2E7D32`) have insufficient contrast against dark backgrounds.
   - Location: `app/src/main/java/com/neuroflow/app/presentation/common/TaskRow.kt`:
     - Lines 198–216 (`TagChip`):
       ```kotlin
       val chipColors = listOf(
           NeuroFlowColors.LightSurfaceAlt, NeuroFlowColors.PomodoroCard, NeuroFlowColors.ScheduleBg,
           NeuroFlowColors.DelegateBg, NeuroFlowColors.DoFirstBg, Color(0xFFE0F1F2)
       )
       ...
       color = chipColors[colorIndex],
       ...
       color = MaterialTheme.colorScheme.onSurface // Light/White text in dark mode placed over light pastel backgrounds!
       ```
     - Lines 236–254: `getQuadrantBgColor()` and `getQuadrantTextColor()` return fixed light pastel backgrounds and dark texts regardless of theme.

---

### 1.3 Accessibility (Requirement R3)

1. **Touch Target Dimensions Violating 48x48dp**:
   - `TaskRow.kt`:
     - Line 58: `IconButton(onClick = onCompleteClick, modifier = Modifier.size(32.dp))` (32dp < 48dp).
     - Line 135: `IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp))` (28dp < 48dp).
   - `FocusScreen.kt`:
     - Line 317: `IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(24.dp))` (24dp < 48dp).
   - `MatrixScreen.kt`:
     - Line 705: `FilledIconButton(..., modifier = Modifier.size(32.dp))` (32dp < 48dp).
     - Line 895: `FilledIconButton(..., modifier = Modifier.size(24.dp))` (24dp < 48dp).
   - `WeeklyScheduleScreen.kt`:
     - Line 181: `IconButton(onClick = { viewModel.previousWeek() }, modifier = Modifier.size(32.dp))` (32dp < 48dp).
     - Line 195: `IconButton(onClick = { viewModel.nextWeek() }, modifier = Modifier.size(32.dp))` (32dp < 48dp).
   - `HistoryScreen.kt`:
     - Line 293: `IconButton(onClick = onDelete, modifier = Modifier.size(36.dp))` (36dp < 48dp).
   - `NewTaskSheet.kt`:
     - Line 1457: `.clickable(onClick = onClear)` on a 16dp icon.
     - Line 1507: `.clickable(onClick = onClear)` on a 16dp icon.

2. **Semantic Roles & Content Descriptions**:
   - Search across `app/src/main/java/com/neuroflow/app/presentation` for `role = Role` or `Role.Button` returned **0 results**.
   - Search for `Modifier.semantics` returned **0 results**.
   - Over 50 `.clickable { ... }` elements (e.g., cards in `MatrixScreen.kt` lines 448, 627, 741, 927; `MoreMenuSheet.kt` line 176; `TodayScreen.kt`) lack semantic role announcement (`Role.Button`), leaving screen readers unable to declare them as interactive buttons.

---

### 1.4 Navigation & Dead Code (Requirement R3)

1. **Dead Drawer Components**:
   - `app/src/main/java/com/neuroflow/app/presentation/common/DrawerGoalsSection.kt`: Completely unreferenced across the entire codebase (0 usages).
   - `app/src/main/java/com/neuroflow/app/presentation/common/DrawerWeeklyPlanSection.kt`: Completely unreferenced across the entire codebase (0 usages).
   - `app/src/main/java/com/neuroflow/app/presentation/common/DrawerViewModel.kt`: Legacy drawer artifact. It exposes `activeTasks` (unused by `NeuroFlowApp`), and only provides helper functions for goals dialogs and guide prompts.
   - `app/src/main/java/com/neuroflow/app/presentation/matrix/MatrixScreen.kt`: Defines `onOpenDrawer: (() -> Unit)? = null` and a dead hamburger button (lines 85–86) that is never wired up.

2. **Orphan Routes in `NeuroFlowApp.kt`**:
   - Bottom navigation currently displays 3 items: `Today`, `Plan`, `Progress`.
   - `PlanScreen.kt` already embeds `ScheduleScreen` (Day), `WeeklyScheduleScreen` (Week), and `MatrixScreen` (Matrix).
   - `ProgressScreen.kt` already embeds `AnalyticsScreen` (Overview/Insights) and `HistoryScreen` (History).
   - Yet, `NeuroFlowApp.kt` registers standalone routes in `NavHost` that are dead or orphan:
     - `Screen.Matrix.route` (`"matrix"`): 0 incoming navigations.
     - `Screen.WeeklySchedule.route` (`"weekly_schedule"`): 0 incoming navigations.
     - `Screen.History.route` (`"history"`): 0 incoming navigations.
     - `Screen.Analytics.route` (`"analytics"`): 0 incoming navigations.
     - `Screen.Rewards.route` (`"rewards"`): 0 incoming navigations.
     - `"schedule?date={date}"`: Navigated to from `TodayScreen` and `WeeklyScheduleScreen`, bypassing the `PlanScreen` container even though Day schedule is a tab inside `Plan`.

---

## 2. Logic Chain

```
[Observation 1.1.1: NewTaskSheet is 1612-line Dialog with ~20 controls]
[Observation 1.1.3: Today quickBrainDump creates blank task with zero inference]
[Observation 1.1.4: TaskTagSchedulingProfile has rich keyword taxonomy only used after creation]
       │
       ▼
[Deduction 1: High user friction & cognitive overhead on task capture]
Because creating a task requires either opening a monolithic dialog or dumping raw text that fails to infer duration, energy, or tags, users face high friction.
       │
       ▼
[Actionable R2 Solution: TaskInferenceEngine + QuickTaskModalBottomSheet]
Extract regex & keyword parsing rules into a standalone engine. Replace the Dialog with a ModalBottomSheet that provides instant visual chips for auto-inferred metadata, allowing 1-tap/Enter task creation with progressive disclosure for deep configuration.
```

```
[Observation 1.2.1 & 1.2.2: FocusScreen background hardcoded to FocusBg 0xFFE8F5E9]
[Observation 1.2.2: Cards hardcoded to light pastel pink/blue/purple]
[Observation 1.2.3: TagChip places white onSurface text over light pastel backgrounds]
       │
       ▼
[Deduction 2: Dark & AMOLED themes are severely broken]
In dark and AMOLED modes, background and cards do not adhere to black/dark surfaces. White text on light pastel backgrounds produces a contrast ratio well below the WCAG 4.5:1 minimum (often < 2:1).
       │
       ▼
[Actionable R3 Theme Solution: Material 3 Semantic Color System]
Deprecate hardcoded pastel hexes. Bind FocusScreen to MaterialTheme.colorScheme.background. Replace card containers with surfaceContainer, surfaceVariant, primaryContainer, and errorContainer with paired on-container text tokens.
```

```
[Observation 1.3.1: TaskRow, FocusScreen, MatrixScreen use 24dp–36dp touch targets]
[Observation 1.3.2: 0 instances of Role.Button or Modifier.semantics]
       │
       ▼
[Deduction 3: Violates Android Accessibility Guidelines and WCAG 2.1 Success Criterion 2.5.5]
Interactive components fail touch target size minimum (48x48dp) and do not declare their role to accessibility services.
       │
       ▼
[Actionable R3 Accessibility Solution: Minimum Touch Target & Semantics]
Enforce Modifier.minimumInteractiveComponentSize() or sizeIn(minWidth=48.dp, minHeight=48.dp) on all IconButtons, and supply role=Role.Button + onClickLabel to all clickable surfaces.
```

```
[Observation 1.4.1: DrawerGoalsSection and DrawerWeeklyPlanSection are unused]
[Observation 1.4.2: 6 orphan routes exist in NavHost while Plan and Progress already embed them]
       │
       ▼
[Deduction 4: Navigation clutter, dead code bloat, and fractured backstack]
       │
       ▼
[Actionable R3 Navigation Solution: 3 Clean Core Hubs]
Consolidate into Flow (Today/Execution), Plan (Day/Week/Matrix), and Insights (Progress/History/Analytics). Delete dead drawer files and prune orphan routes from NavHost.
```

---

## 3. Detailed Architectural Blueprints

### 3.1 Requirement R2: Single-Field Quick Task Capture Architecture

#### Heuristic Inference Rules (`TaskInferenceEngine`)

| Entity | Regex / Keyword Pattern | Inferred Attribute | Default if Unspecified |
|---|---|---|---|
| **Duration** | `(?i)(?:^\|\s)(?:for\s+)?(\d+)\s*(?:m\|min\|mins\|minute\|minutes)\b` | `estimatedDurationMinutes = $1` | Inferred from TaskType (15–45m) |
| **Duration (Hours)** | `(?i)(?:^\|\s)(?:for\s+)?(\d+(?:\.\d+)?)\s*(?:h\|hr\|hrs\|hour\|hours)\b` | `estimatedDurationMinutes = ($1 * 60)` | — |
| **Explicit Context** | `(?i)(?:^\|\s)(@\w+)` | `contextTag = $1` (e.g. `@computer`, `@errands`, `@home`) | `""` |
| **Analytical / Deep Work** | `(?i)\b(code\|debug\|refactor\|dev\|software\|analyze\|study\|math\|thesis\|audit\|architecture\|write\|paper)\b` | `taskType = TaskType.ANALYTICAL`, `energyLevel = EnergyLevel.HIGH`, duration = `45m` | — |
| **Creative** | `(?i)\b(design\|ui\|ux\|draw\|sketch\|draft\|brainstorm\|illustrate\|ideate\|concept\|logo)\b` | `taskType = TaskType.CREATIVE`, `energyLevel = EnergyLevel.MEDIUM`, duration = `45m` | — |
| **Admin / Routine** | `(?i)\b(email\|reply\|inbox\|invoice\|receipt\|pay\|bill\|schedule\|register\|submit\|form\|clean desk\|timesheet)\b` | `taskType = TaskType.ADMIN`, `energyLevel = EnergyLevel.LOW`, duration = `15m` | — |
| **Physical / Errands** | `(?i)\b(workout\|gym\|run\|lift\|exercise\|walk\|stretch\|groceries\|buy\|clean\|laundry\|mow\|dishes)\b` | `taskType = TaskType.PHYSICAL`, `energyLevel = EnergyLevel.MEDIUM`, duration = `30m` | — |
| **Mindfulness** | `(?i)\b(meditate\|breathe\|breathwork\|yoga\|journal\|reflect\|calm)\b` | `energyLevel = EnergyLevel.LOW`, duration = `15m`, category = `MINDFULNESS` | — |
| **Urgency / Do First** | `(?i)(?:!+\b\|\b(urgent\|asap\|critical\|emergency)\b)` | `quadrant = Quadrant.DO_FIRST`, `priority = Priority.HIGH`, `impactScore = 85` | `Quadrant.SCHEDULE`, `Priority.MEDIUM` |
| **Eat the Frog** | `(?i)(?:🐸\|\b(frog\|eat the frog\|hardest\|dreaded)\b)` | `isFrog = true`, `quadrant = Quadrant.DO_FIRST` | `isFrog = false` |
| **Date: Today** | `(?i)\b(today\|tonight\|this morning\|this afternoon)\b` | `scheduledDate = todayMidnight` | `todayMidnight` |
| **Date: Tomorrow** | `(?i)\b(tomorrow)\b` | `scheduledDate = tomorrowMidnight` | — |
| **Date: Weekend** | `(?i)\b(this weekend\|weekend\|saturday\|sunday)\b` | `scheduledDate = upcomingSaturdayMidnight` | — |

#### Title Sanitization
The engine removes matched duration tokens (`45m`, `1h`), context tags (`@computer`), urgency flags (`!`), and frog symbols (`🐸`) so the resulting task title is clean:
- **Input**: `"Refactor auth token engine 45m @computer !"`
- **Resulting Task**:
  - `title`: `"Refactor auth token engine"`
  - `estimatedDurationMinutes`: `45`
  - `contextTag`: `"@computer"`
  - `taskType`: `TaskType.ANALYTICAL`
  - `energyLevel`: `EnergyLevel.HIGH`
  - `quadrant`: `Quadrant.DO_FIRST`
  - `priority`: `Priority.HIGH`
  - `tags`: `"coding,deep work"`

#### ModalBottomSheet with Progressive Disclosure
- **Surface**: `ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false))`
- **Default (Collapsed) State**:
  1. Title field with live auto-focus.
  2. Live Inferred Chips Row (horizontally scrollable):
     - `[⏱️ 45m ▾]` (clickable: quick-pick 15, 30, 45, 60, custom)
     - `[⚡ High Energy ▾]` (clickable: High, Medium, Low)
     - `[🧠 Analytical ▾]` (clickable: Analytical, Creative, Admin, Physical)
     - `[📍 @computer ▾]`
     - `[🎯 Do First ▾]`
     - `[🐸 Frog]`
  3. Date chips: `[Today]` `[Tomorrow]` `[Pick Date...]`
  4. Primary `Create Task` button + `More options ▾` text button.
- **Expanded (Progressive Disclosure) State**:
  - Unfolds Description, Eisenhower quadrant picker, Eisenhower sliders, Dependencies, and WOOP fields.

---

### 3.2 Requirement R3: Design System & Semantic Color Token Migration

#### Token Replacement Guide for Contrast & AMOLED Compliance

| Location | Existing Hardcoded Color | Contrast Issue | Recommended M3 Token Replacement |
|---|---|---|---|
| `FocusScreen.kt:205` | `NeuroFlowColors.FocusBg` (`0xFFE8F5E9`) | Blinds user in Dark/AMOLED mode | `MaterialTheme.colorScheme.background` |
| `FocusScreen.kt:635` | `NeuroFlowColors.DeadlineCard` (`0xFFFFEBEE`) | Light pastel in dark mode | `MaterialTheme.colorScheme.errorContainer` (text: `onErrorContainer`) |
| `FocusScreen.kt:662` | `NeuroFlowColors.ScheduledCard` (`0xFFE3F2FD`) | Light pastel in dark mode | `MaterialTheme.colorScheme.secondaryContainer` (text: `onSecondaryContainer`) |
| `FocusScreen.kt:833` | `NeuroFlowColors.PomodoroCard` (`0xFFF3E5F5`) | Light pastel in dark mode | `MaterialTheme.colorScheme.tertiaryContainer` (text: `onTertiaryContainer`) |
| `FocusScreen.kt:409` | `Color(0xFFE3F2FD)` (WOOP card) | Inverted contrast | `MaterialTheme.colorScheme.surfaceVariant` (text: `onSurfaceVariant`) |
| `FocusScreen.kt:477` | `Color(0xFFE8F5E9)` (Timer card) | Light green background | `MaterialTheme.colorScheme.surfaceContainer` |
| `TodayScreen.kt:144` | `NeuroFlowColors.Purple` | Hardcoded FAB | `MaterialTheme.colorScheme.primaryContainer` (content: `onPrimaryContainer`) |
| `TodayScreen.kt:673` | `SlateCard = Color(0xFF1E293B)` | Dark slate card in light mode | `MaterialTheme.colorScheme.surfaceVariant` |
| `TodayScreen.kt:947` | `Color(0xFF2E7D32)` & `Color(0xFFE65100)` | Hardcoded swipe actions | Semantic theme status colors: `secondary` / `tertiary` |
| `TaskRow.kt:199` | `TagChip` light pastels (`DoFirstBg`, etc.) | White text on light pastels | Theme-aware chip colors using `MaterialTheme.colorScheme.surfaceContainerHigh` + `onSurface` |
| `TaskRow.kt:237` | `getQuadrantBgColor()` | Fixed light pastels | Theme-aware quadrant containers: tinted in light, dark muted in dark, pure black outline in AMOLED |
| `Theme.kt:96` | AMOLED dark background | SurfaceVariant not pure black | Ensure `surface`, `background`, and root cards are `Color.Black` with 1dp `outlineVariant` border in AMOLED |

---

### 3.3 Requirement R3: Accessibility Audit & Remediation

#### Touch Target Remediation (Min 48x48dp)
1. **`TaskRow.kt:58`** (Checkbox button):
   - *Current*: `IconButton(modifier = Modifier.size(32.dp))`
   - *Fix*: `IconButton(modifier = Modifier.size(48.dp))` with inner `Icon(modifier = Modifier.size(24.dp))`.
2. **`TaskRow.kt:135`** (Edit task button):
   - *Current*: `IconButton(modifier = Modifier.size(28.dp))`
   - *Fix*: `IconButton(modifier = Modifier.size(48.dp))` with inner `Icon(modifier = Modifier.size(18.dp))`.
3. **`FocusScreen.kt:317`** (Accordion toggle):
   - *Current*: `IconButton(modifier = Modifier.size(24.dp))`
   - *Fix*: `IconButton(modifier = Modifier.size(48.dp))` with inner `Icon(modifier = Modifier.size(20.dp))`.
4. **`WeeklyScheduleScreen.kt:181, 195`** (Week navigation chevrons):
   - *Current*: `IconButton(modifier = Modifier.size(32.dp))`
   - *Fix*: `IconButton(modifier = Modifier.size(48.dp))`.
5. **`MatrixScreen.kt:705, 895`** (Focus and Add buttons):
   - *Current*: `FilledIconButton(modifier = Modifier.size(32.dp / 24.dp))`
   - *Fix*: `FilledIconButton(modifier = Modifier.size(48.dp))` or use standard M3 button sizing.
6. **`NewTaskSheet.kt:1457, 1507`** (Date/Time clear buttons):
   - *Current*: `Icon(..., modifier = Modifier.size(16.dp).clickable { onClear() })`
   - *Fix*: Wrap in `IconButton(modifier = Modifier.size(48.dp))` with `contentDescription = "Clear date"`.

#### Semantic Roles & Content Descriptions
1. Add `role = Role.Button` and `onClickLabel` to all custom clickable cards:
   ```kotlin
   Modifier.clickable(
       role = Role.Button,
       onClickLabel = "Open task details",
       onClick = onTaskClick
   )
   ```
2. Replace null content descriptions on actionable icons with localized descriptive strings.

---

### 3.4 Requirement R3: Navigation Consolidation Plan

#### 3 Clean Core Hubs

```
┌─────────────────────────────────────────────────────────────┐
│                       neuroFlow App                         │
├─────────────────┬─────────────────────────┬─────────────────┤
│    HUB 1: FLOW   │       HUB 2: PLAN       │ HUB 3: INSIGHTS │
│  (Today/Execute)│    (Schedule/Matrix)    │(Progress/Stats) │
├─────────────────┼─────────────────────────┼─────────────────┤
│ • Hero Task     │ • Day Timeline (Sched)  │ • Weekly Review │
│ • Queue / Frog  │ • Week Matrix View      │ • History Log   │
│ • Quick Capture │ • Eisenhower Matrix     │ • Behavioral    │
│ • Active Focus  │ • Protected Time Slots  │   Analytics     │
└─────────────────┴─────────────────────────┴─────────────────┘
```

1. **Hub 1: Flow (`Screen.Flow`)**:
   - Route: `"flow"` (with legacy redirect from `"today"`).
   - Hosts `TodayScreen`.
   - Primary execution engine: displays current hero focus task, protected slot takeover, upcoming queue, quick capture bar, and swipe actions.

2. **Hub 2: Plan (`Screen.Plan`)**:
   - Route: `"plan?view={view}&date={date}"`.
   - Hosts `PlanScreen`.
   - Manages three sub-views via top segmented button: Day (`ScheduleScreen`), Week (`WeeklyScheduleScreen`), Matrix (`MatrixScreen`).
   - Deep-linking from Flow directly sets view mode and date in `PlanScreen`, eliminating redundant standalone routes.

3. **Hub 3: Insights (`Screen.Insights`)**:
   - Route: `"insights?tab={tab}"` (with legacy redirect from `"progress"`).
   - Hosts `ProgressScreen`.
   - Manages Overview, History (`HistoryScreen`), and Analytics (`AnalyticsScreen`).

#### Legacy Dead Code & Orphan Route Pruning
- **Delete Files**:
  - `app/src/main/java/com/neuroflow/app/presentation/common/DrawerGoalsSection.kt`
  - `app/src/main/java/com/neuroflow/app/presentation/common/DrawerWeeklyPlanSection.kt`
- **Prune Routes from `NavHost` in `NeuroFlowApp.kt`**:
  - Remove `Screen.Matrix.route`
  - Remove `Screen.WeeklySchedule.route`
  - Remove `Screen.History.route`
  - Remove `Screen.Analytics.route`
  - Remove `Screen.Rewards.route`
  - Route `"schedule?date={date}"` into `Screen.Plan.route` with date argument.
- **Refactor `DrawerViewModel.kt`**:
  - Rename to `AppPreferencesViewModel` or integrate into root state. Remove unused `activeTasks` StateFlow.
- **Clean `MatrixScreen.kt`**:
  - Remove unused `onOpenDrawer: (() -> Unit)? = null` parameter and dead hamburger button.

---

## 4. Caveats

1. **Deep-Link Intent Compatibility**:
   - `MainActivity.kt` handles explicit action strings:
     - `"com.procus.ACTION_OPEN_NEW_TASK"`
     - `"com.procus.ACTION_OPEN_FOCUS"`
     - `"com.procus.ACTION_OPEN_GUIDE"`
     - `"com.procus.ACTION_OPEN_SCHEDULE"`
   - When redirecting `"schedule"` into the `Plan` hub, `initialScheduleOpen` in `MainActivity.kt` must navigate to `Plan` with Day mode pre-selected so widgets and launcher shortcuts continue working without regression.
2. **HyperFocus Module**:
   - The `feature/hyperfocus` directory contains its own activities (`HyperFocusActivity`, `HyperFocusHomeActivity`) with kiosk mode and app-blocking overlays (`BlockingOverlayScreen`). These have dedicated dark surfaces (`Color(0xFF0A0A0A)`). The core app theme refactoring should provide tokens that HyperFocus screens can reuse cleanly without breaking kiosk security constraints.
3. **Room Schema Invariance**:
   - The proposed single-field quick capture and heuristics map directly to existing columns on `TaskEntity` (`estimatedDurationMinutes`, `taskType`, `energyLevel`, `contextTag`, `isFrog`, `quadrant`, `priority`). No Room schema modification or database migration is required for R2!

---

## 5. Conclusion

- **R2 (Task Creation & Inference)**:
  - Current task creation is bifurcated between a 1,612-line monolithic dialog (`NewTaskSheet`) and a dumb brain-dump field (`TodayViewModel.quickBrainDump`).
  - By introducing a pure, unit-tested `TaskInferenceEngine` and replacing the dialog with a progressive disclosure `QuickTaskModalBottomSheet`, task creation friction drops from ~20 taps to a single input with auto-inferred pills.
- **R3 (Design System & Theming)**:
  - AMOLED and Dark themes are broken primarily due to hardcoded pastel constants (`FocusBg = 0xFFE8F5E9`, `DeadlineCard`, `ScheduledCard`, `PomodoroCard`) and inverted chip text in `TaskRow.kt`.
  - Migrating to M3 semantic container tokens resolves all contrast violations and restores true AMOLED pitch-black backgrounds.
- **R3 (Accessibility)**:
  - Multiple icon buttons violate the 48x48dp minimum touch target (24dp–36dp). Zero elements declare `Role.Button`.
  - Standardizing touch targets to 48dp and declaring semantic roles satisfies WCAG 2.1 AA and Android accessibility guidelines.
- **R3 (Navigation)**:
  - Unused drawer files (`DrawerGoalsSection.kt`, `DrawerWeeklyPlanSection.kt`) and 6 orphan routes in `NavHost` cause bloat and broken backstack flows.
  - Consolidating into 3 core hubs (**Flow**, **Plan**, **Insights**) creates a modern, unified information architecture.

---

## 6. Verification Method

To independently verify these findings:

1. **Verify Touch Target Dimensions**:
   - Inspect `TaskRow.kt` lines 58 & 135: `Modifier.size(32.dp)` and `Modifier.size(28.dp)`.
   - Inspect `FocusScreen.kt` line 317: `Modifier.size(24.dp)`.
   - Inspect `WeeklyScheduleScreen.kt` lines 181 & 195: `Modifier.size(32.dp)`.
2. **Verify Hardcoded Contrast Breakage**:
   - Inspect `FocusScreen.kt` lines 186 & 205: Verify `.background(focusBg)` sets the background to `Color(0xFFE8F5E9)`.
   - Inspect `TaskRow.kt` lines 198–216: Verify `TagChip` sets light pastel colors with `color = MaterialTheme.colorScheme.onSurface`.
3. **Verify Dead Drawer Code & Orphan Routes**:
   - Run:
     ```bash
     git grep "DrawerGoalsSection"
     git grep "DrawerWeeklyPlanSection"
     git grep "Screen.Matrix.route"
     git grep "Screen.Analytics.route"
     git grep "Screen.History.route"
     ```
   - Verify that `DrawerGoalsSection` and `DrawerWeeklyPlanSection` are unreferenced outside their own definitions, and the orphan routes have 0 incoming navigations.
4. **Compile & Test Baseline**:
   - Execute:
     ```bash
     ./gradlew testDebugUnitTest
     ```
   - All tests must pass before and after applying modernizations.
