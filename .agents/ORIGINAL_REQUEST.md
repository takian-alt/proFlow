# Original User Request

## 2026-10-15T21:09:25Z

Rebuild and modernize the Android application neuroFlow (in /home/takian/Project/PROCUS), transforming it from a complex, bug-ridden productivity dashboard into a clean, neuroscience-backed, highly automated execution engine that drastically reduces cognitive friction.

Working directory: /home/takian/Project/PROCUS
Integrity mode: development

## Requirements

### R1. Core Engineering & Math Pipeline Fixes
- Fix Borbély Two-Process and Peak Energy engines: continuous baseline without zeroing pre-peak hours, support circadian midnight wrap for evening chronotypes, and decouple confidence factor from baseline energy magnitude.
- Connect live energy state directly to task scoring and auto-scheduling decisions.
- Harden Room type converters against unknown enum deserialization, fix database migrations non-destructively, and resolve main thread startup blocking (runBlocking in Application class).
- Configure test runners so all JUnit 4 & JUnit 5 test suites execute properly without being silently skipped.

### R2. Minimal-Friction Task Creation & Smart Inference
- Implement single-field quick task capture that auto-infers task type, estimated duration, energy demands, and context tags using lightweight heuristics/keywords.
- Convert monolithic task creation dialogs into a streamlined ModalBottomSheet with progressive disclosure.

### R3. Modern Design System & Accessible UI
- Establish a consistent Material 3 design system supporting Light, Dark, and AMOLED modes with zero hardcoded contrast-breaking colors (especially on FocusScreen and Today/Flow screens).
- Ensure all interactive elements conform to Android accessibility guidelines (minimum 48x48dp touch targets, semantic roles).
- Consolidate navigation into 3 clean core hubs (Flow / Plan / Insights) eliminating orphan routes and legacy dead drawer code.

### R4. Supportive Focus & Tiered HyperFocus
- Rebuild Focus Mode with clean Pomodoro/custom timers, collapsible WOOP/implementation intentions, and affordance difficulty feedback.
- Replace punitive/hostile blocking copy with constructive, supportive interventions across three user-selectable tiers (Zen, Shield, Fortress).

### R5. Auto-Scheduling Transparency & Behavioral Insights
- Decompose the auto-scheduling engine into modular components that provide transparent placement rationales and actionable deferral suggestions instead of silent drops.
- Implement behavioral pattern insights (productivity peaks, estimation errors) that adaptively inform scheduling and prioritization.

## Acceptance Criteria

### Verification & Testing
- [ ] `./gradlew testDebugUnitTest` runs all unit test suites without missing JUnit 4 tests, and all engine tests pass.
- [ ] Application cold start executes without disk I/O or `runBlocking` on the Android UI thread.
- [ ] Database migration from v11 to v12 executes cleanly without dropping user notes or data.

### Functional & UI Correctness
- [ ] Quick task creation allows instant creation with title only, auto-inferring sensible duration and energy tags.
- [ ] Focus screen and dashboard display correctly in both pure Dark and AMOLED themes with readable contrast and no hardcoded light pastel backgrounds.
- [ ] All primary interactive icons/buttons have touch target dimensions of at least 48dp.
- [ ] Auto-scheduling reports clear placement status or actionable deferral reasons for any unplaced tasks.

## 2026-10-16T13:05:30Z

Complete the neuroFlow V2 UI by migrating 100% of classic capabilities into the modern V2 architecture, implementing ADHD cognitive safety nets (Zero-Guilt Fresh Start, Break-It-Down Scaffolder, 60s Physiological Sigh) wrapped in a calm, sensory-friendly OLED Obsidian design system.

Working directory: /home/takian/Project/PROCUS
Integrity mode: development

## Requirements

### R1. Calm Sensory Design System ("Obsidian Sanctuary")
- **Palette**: OLED Deep Obsidian (`#0A0B0E`) background with elevated Slate surfaces (`#13151B`, `#1C1F28`) and subtle borders (`#2A2E3B`) to minimize sensory arousal.
- **Accents**: Electric Indigo (`#6366F1`) for focus, Sage Green (`#10B981`) for biological peaks, Warm Amber (`#F59E0B`) for sustained alertness, and Muted Coral (`#F43F5E`) for rest/recovery.
- **Zero Alarmist Red**: Replace punishing red overdue warnings with supportive, low-anxiety visual indicators to prevent amygdala threat responses.

### R2. Progressive Disclosure & Anti-Paralysis Scaffolding
- **Home (Flow)**: Displays only the Circadian Barometer, a single prominent "Recommended Next Action" hero card, and the vertical timeline stream.
- **Zero-Guilt Fresh Start**: A gentle prompt appears when $>3$ tasks are overdue, allowing a 1-tap rollover of stale tasks to the Icebox without shame.
- **"Break It Down" Magic Wand**: A 1-tap button on tasks that breaks complex titles into 3 microscopic steps to overcome executive initiation inertia.
- **4-Second Non-Destructive Undo**: Floating snackbar on task completion and deferral for accidental tap recovery.
- **Evening Shutdown Complete**: Dynamic reflection card appearing after 7:00 PM to close open mental loops.

### R3. Focus Experience & Autonomic Recovery
- **Modes**: "Just 5 Minutes" dopamine micro-start, 25m Pomodoro, 90m Ultradian Rhythm, and open Flow Stopwatch.
- **Autonomic Reset**: 60-second physiological sigh breathing guide (2 quick inhales, 1 slow exhale) during break transitions to reset cortisol.
- **Micro-Step Checklist & Thought Parking**: Embedded checklist on focus screen with an instant bottom drawer to park intrusive thoughts.
- **Post-Session Affordance Rating**: 1-tap effort rating (1–5 scale) to calibrate machine learning estimation accuracy.
- **System DND**: Automatically engages Priority Do Not Disturb policy during active tracking.

### R4. Contextual Integration of Secondary Tools
- Surface the distraction-free Notes scratchpad via smooth bottom sheets from Flow and Plan.
- Link the Chronotype MEQ Quiz and Sleep Log visualizer directly from the Circadian Barometer.
- Connect Ulysses Pre-Commitment Contracts with stakes directly into Insights and high-resistance tasks.
- Floating context/tag pills (`#deepwork`, `#offline`, `#admin`) on Flow and Plan screens.

## Acceptance Criteria

### Automated Verification & Integrity
- [ ] `./gradlew testDebugUnitTest` runs all test suites and passes with 0 failures.
- [ ] Existing Room database tables, migrations, and preference keys remain 100% backward-compatible.

### Visual & Functional Quality
- [ ] Visual design adheres strictly to the OLED Obsidian & Electric Indigo sensory-friendly theme.
- [ ] Overdue tasks never display alarming red text or shaming badges.
- [ ] Zero-Guilt Fresh Start, Break-It-Down scaffolder, and 60s Physiological Sigh are fully functional.
- [ ] Completing tasks provides a non-destructive 4-second undo buffer with instant reversal.
- [ ] All classic features (Notes, Sleep Logs, MEQ, Ulysses, Protected Slots) are accessible in V2 without launching the classic navigation tree.

## 2026-10-16T15:15:13Z

Rebuild and integrate the remaining critical neuro-capabilities into neuroFlow V2: achieve 100% feature parity with classic V1 (full notes workspace, 7-day weekly schedule, analytics heatmap, priority weights, account sync, identity affirmations, manual time log), neural soundscapes (40Hz gamma / brown noise), drift-proof lock-screen focus notification, home screen widget interoperability, voice task capture, and executive transition decompression.

Working directory: /home/takian/Project/PROCUS
Integrity mode: development

## Requirements

### R1. 100% V1 Feature Parity & Navigation Connectivity
- **Full Notes System**: Connect `QuickNotesBottomSheet` to the full Keep-style `NotesScreen` and `NoteEditorScreen` styled in the OLED obsidian design system.
- **Weekly Schedule**: Integrate the 7-day multi-column visual time-blocking calendar (`WeeklyScheduleScreen`) into the Plan Hub.
- **Deep Analytics & History**: Integrate historical 7-day/30-day velocity, hourly focus heatmaps, and estimation accuracy (sMAPE) from `AnalyticsScreen`, plus completed tasks history from `HistoryScreen` into the Insights Hub.
- **Settings Routes**: Wire `PriorityWeightsScreen` (1-click strategy presets & mathematical weight tuning), `AccountSyncScreen` (Firebase cloud sync & Google auth), `HyperFocusAllowlistRoute` (Fortress mode app picker), `PrivacyPermissionsScreen`, and `AppGuideScreen` into `NeuroFlowAppV2` and `SettingsScreen`.
- **Identity & WOOP**: Wire `IdentityScreen` affirmations into Insights and add a 1-tap "Set WOOP Anchor" reflection sheet on `FocusScreenV2` when empty.
- **Retrospective Time Logging**: Add a "Log Past Time" action to task options for off-device work.

### R2. Neural Soundscapes & Auditory Focus Stimulation
- Add an integrated soundscape generator to `FocusScreenV2` supporting 3 neuroscience-backed audio streams:
  - **40 Hz Gamma Waves**: Binaural stimulation to drive prefrontal cortex neural synchrony and working memory.
  - **Deep Brown Noise**: 1/f² stochastic resonance acoustic masking to dampen ADHD sensory distractibility.
  - **Flow Pink Noise / Rain**: 1/f calming spectral density for sustained concentration.
- Include a tactile volume slider and automatic smooth fade-out upon timer completion or manual pause.

### R3. Drift-Proof Background Focus & Lock-Screen Live Notification
- Re-architect focus session elapsed time to track wall-clock timestamps (`startedAtMillis`, `accumulatedPauseMillis`) so timing never drifts or freezes when the device locks or when switching applications.
- Implement an ongoing Foreground Notification with live progress countdown, current task title, and instant 1-tap "Pause" / "Resume" and "Complete" action buttons.

### R4. Default V2 UI & Home Screen Widget Interoperability
- Set `useV2Ui = true` by default in `UserPreferencesDataStore`.
- Update `MainActivity` intent handling so `com.procus.ACTION_OPEN_NEW_TASK` and `ACTION_OPEN_FOCUS` directly trigger the V2 Smart Task Composer sheet and V2 Focus screen.
- Wire `QuickCaptureWidgetProvider`, `NextActionWidgetProvider`, and `FocusWidgetProvider` to open V2 destinations directly.

### R5. Hands-Free Voice Quick-Capture & Executive Transition Buffer
- Add a 1-tap microphone capture button in `SmartTaskComposerSheetV2` leveraging Android's speech recognition framework.
- Pipe recognized speech directly into `TaskInferenceEngine` to auto-extract task title, duration, energy demand, and tags without typing friction.
- Implement a 3-minute post-focus decompression countdown buffer ("Decompression Zone") to prevent executive whiplash and dopamine crashes between back-to-back demanding tasks.

## Acceptance Criteria

### Automated Verification & Integrity
- [ ] `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1` runs all unit test suites with 100% pass rate (0 failures, 0 regressions).
- [ ] Heap allocations strictly conform to memory bounds in `gradle.properties` without triggering system OOM.

### Functional Correctness
- [ ] Zero dead routes in `NeuroFlowAppV2`: Notes, Weekly Calendar, Priority Weights, Account Sync, HyperFocus Allowlist, History, and Identity screens open smoothly.
- [ ] Focus timer remains accurate to the second after device screen is turned off and unlocked after an extended period.
- [ ] Soundscape generator starts, pauses, and fades out cleanly without audio clipping or memory leaks.
- [ ] Tapping the home screen Quick Capture widget opens the V2 Smart Task Composer sheet directly.
- [ ] Voice capture successfully transcribes speech into inferred tasks.
- [ ] Insights screen renders historical 7-day focus distribution and circadian alignment trends.
