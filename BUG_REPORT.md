# PROCUS Bug & Feature Audit Report

*Generated automatically during the bug hunt test run.*  
*Last updated: 2026-10-31*

---

### Iterative Audit Progression

Each scheduled session audits strictly **one field/subsystem** in sequential depth. Once an area is verified clean, the next session proceeds deeper into the feature matrix.

| Run # | Target Field / Subsystem | Scope / Key Files | Audit Status | Next Scheduled Target |
|---|---|---|---|---|
| **#1** | **Main UI & Navigation** | `NeuroFlowAppV2.kt`, `MainActivity.kt`, `NeuroFlowV2UiTest.kt` | **Verified Clean** (Unit tests passing, route guards & mini-focus bar intact) | **Main Core Logic** (`TaskSchedulerService.kt`, `AutoSchedulingRationales.kt`) |
| **#2** | **CalendarSync (Secondary UI & Features)** | `CalendarSyncDataSource.kt`, `CalendarSyncRepositoryTest.kt`, `CalendarWriteBackSyncTest.kt` | **Verified Clean** (All-day event filtering, recurring habit anchors & primary calendar decoupling resolved, 15 unit tests passing) | **Tag & Category Management** (`TaskTagViewModel.kt`, `CategoryPickerSheet.kt`, `SmartTaskComposerSheetV2.kt`) |
| **#3** | **Tag & Category Management (Secondary UI & Features)** | `TaskTagViewModel.kt`, `TaskTagSchedulingProfile.kt`, `UserPreferencesDataStore.kt`, `SmartTaskComposerSheetV2.kt`, `TaskTagAndCategoryTest.kt` | **Verified Clean** (Starter tag deletion, `#` and `@` normalization, Plan screen tag filter synchronization, and exact tag token matching resolved, 100% unit tests passing) | **Neural Soundscape DSP Engine (Deeper Subsystems & Background Services)** (`NeuralSoundscapeGenerator.kt`, `NeuralSoundscapePlayer.kt`, `NeuralSoundscapeDspStressTest.kt`) |
| **#4** | **Neural Soundscape DSP Engine (Deeper Subsystems & Background Services)** | `NeuralSoundscapeGenerator.kt`, `NeuralSoundscapePlayer.kt`, `FocusViewModelV2.kt`, `NeuralSoundscapeAdversarialStressTest.kt`, `NeuralSoundscapeDspStressTest.kt` | **Verified Clean** (Active soundscape fade-out preservation on `play(NONE)`, `AudioTrack.flush()` tail-truncation removal, native `AudioTrack` leak prevention via `releaseWithFadeOut()`, and single-chunk crossfading resolved, 56/56 unit tests passing) | **Focus Session Recovery (Deeper Subsystems & Background Services)** (`FocusViewModelV2.kt`, `FocusTimingCalculator.kt`, `FocusService.kt`, `TimeSessionDao.kt`) |
| **#5** | **Focus Session Recovery (Deeper Subsystems & Background Services)** | `FocusViewModelV2.kt`, `FocusService.kt`, `FocusTimingCalculator.kt`, `TimeSessionDao.kt`, `CompleteTaskWidgetAction.kt`, `FocusSessionRecoveryTest.kt` | **Verified Clean** (Timer mode recovery across all durations, background service pause state retention, fresh UUID allocation on `keepGoing()`, auto-tracker guarding on completed tasks, and `ORDER BY startedAt DESC` SQLite resolution, 100% unit tests passing) | **PlanScreenV2 (Main UI Screens)** (`PlanScreenV2.kt`, `PlanViewModelV2.kt`, `PlanAndComposerUpgradesV2.kt`) |
| **#6** | **PlanScreenV2 (Main UI Screens)** | `PlanScreenV2.kt`, `PlanViewModelV2.kt`, `PlanAndComposerUpgradesV2.kt`, `PlanScreenV2AuditTest.kt` | **Verified Clean** (Protected time slot midnight modulo overflow, dynamic circadian peak integration, stale swipe-to-dismiss closure state, multi-day energy timeline anchor alignment, and completed task flow exposure for task inference calibration resolved; 100% unit tests passing) | **FlowScreenV2 (Main UI Screens)** (`FlowScreenV2.kt`, `FlowViewModelV2.kt`, `FlowHubUpgradesV2.kt`) |
| **#7** | **FlowScreenV2 (Main UI Screens)** | `FlowScreenV2.kt`, `FlowViewModelV2.kt`, `FlowHubUpgradesV2.kt`, `EnergyTimelineStream.kt`, `FlowScreenV2AuditTest.kt` | **Verified Clean** (Swipe-to-dismiss stale closure state, spotlight hero future/icebox task leakage & tag filtering desync, untimed tasks 00:00 midnight sort order, micro-step doorway all-completed clamping, peak shift nudge & anchor preservation, and undo snackbar message protection resolved; 100% unit tests passing) | **FocusScreenV2 (Main UI Screens)** (`FocusScreenV2.kt`, `FocusViewModelV2.kt`, `FocusService.kt`) |
| **#8** | **FocusScreenV2 (Main UI Screens)** | `FocusScreenV2.kt`, `FocusViewModelV2.kt`, `FocusService.kt`, `FocusAndRegulationUpgradesV2.kt`, `FocusScreenV2AuditTest.kt` | **Verified Clean** (Paused session onCleared persistence across navigation, stopwatch mode elapsed time synchronization in MiniFocusBar, zombie WOOP sheet loop prevention, completed micro-step resurrection suppression in doorway spotlight, mode chips horizontal scroll overflow, untracked task focus minute fabrication, ongoing pause duration accumulation, and physiological sigh race cancellation resolved; 100% unit tests passing) | **Main Core Logic** (`TaskSchedulerService.kt`, `AutoSchedulingRationales.kt`) |
| **#9** | **Main Core Logic** | `TaskSchedulerService.kt`, `AutoSchedulingRationales.kt`, `PlacementStability.kt`, `DependencyGraphValidator.kt`, `PlanViewModelV2.kt`, `SchedulerAuditTest.kt` | **Verified Clean** (External prerequisite stripping resolved, overdue task deadline ceiling recovery enabled, slot budget overflow prevented, future-day conflict epoch anchor aligned, and ultradian split reference anchor stabilized; 100% unit tests passing) | **SmartTaskComposerSheetV2 & QuickNotesBottomSheet (Secondary UI & Features)** (`SmartTaskComposerSheetV2.kt`, `QuickNotesBottomSheet.kt`, `TaskComposerUpgradesV2.kt`) |
| **#10** | **SmartTaskComposerSheetV2 & QuickNotesBottomSheet (Secondary UI & Features)** | `SmartTaskComposerSheetV2.kt`, `QuickNotesBottomSheet.kt`, `TaskInferenceEngine.kt`, `SmartTaskComposerSheetV2Test.kt` | **Verified Clean** (NLP date shadowing resolved, edit mode directive paralysis fixed, time picker inferred deadline preservation enabled, quick note custom title clobbering & coroutine safety fixed, and duration chip progression smoothed; 100% unit tests passing) | **ScheduleAutoTasksWorker & NotificationWorker (Deeper Subsystems & Background Services)** (`ScheduleAutoTasksWorker.kt`, `NotificationWorker.kt`, `AutonomyNudgeEngine.kt`) |
| **#11** | **ScheduleAutoTasksWorker & NotificationWorker (Deeper Subsystems & Background Services)** | `ScheduleAutoTasksWorker.kt`, `NotificationWorker.kt`, `AutonomyNudgeEngine.kt`, `AutonomyNudgeWorker.kt`, `NotificationWorkerAuditTest.kt` | **Verified Clean** (Deadline escalation to `DO_FIRST` enabled & spam loop broken, future task autonomy nudge delay anchored to `dueAt`, completed Part 2 continuation preservation, icebox phantom busy window removal, and fresh title synchronization resolved; 100% unit tests passing) | **FocusWidgetProvider & NextActionWidgetProvider (Deeper Subsystems & Background Services)** (`FocusWidgetProvider.kt`, `FocusWidgetUpdateWorker.kt`, `CompleteTaskWidgetAction.kt`, `NextActionWidgetProvider.kt`) |
| **#12** | **FocusWidgetProvider & NextActionWidgetProvider (Deeper Subsystems & Background Services)** | `FocusWidgetProvider.kt`, `FocusWidgetUpdateWorker.kt`, `CompleteTaskWidgetAction.kt`, `NextActionWidgetProvider.kt`, `CompleteTaskReceiver.kt`, `FocusWidgetAuditTest.kt` | **Verified Clean** (Active service session isolation, empty-state button suppression & action routing, Part 2 continuation retention, prerequisite blocking, crash resilience, and idempotency resolved; 100% unit tests passing) | **Deep Concurrency, Database Migrations & Edge Cases** (`AppDatabase.kt`, `Room` migrations, `TimeSessionDao.kt`, `TaskDao.kt`) |
| **#13** | **Deep Concurrency, Database Migrations & Edge Cases** | `NeuroFlowDatabase.kt`, `Converters.kt`, `SessionRepository.kt`, `TaskRepository.kt`, `FocusSessionDao.kt`, `TaskMicroStepDao.kt`, `RoomDatabaseAuditTest.kt` | **Verified Clean** (Schema v1 `MIGRATION_1_2` duplicate column idempotency, foreign key constraint crash prevention on session sync, `HyperFocusState` enum deserialization crash protection, `MIGRATION_21_22` statement leak & duration calculations fixed, and `microStepCounts` fallback enabled; 100% unit tests passing) | **Main UI & Navigation (Second Cycle Hardening)** (`NeuroFlowAppV2.kt`, `MainActivity.kt`, `NavHubsV2.kt`) |
| **#14** | **Main UI & Navigation (Second Cycle Hardening)** | `NeuroFlowAppV2.kt`, `MainActivity.kt`, `NavHubsV2.kt`, `NavigationAuditTest.kt` | **Verified Clean** (Direct intent session latching against `NewChapterCard` / `TopGoalsRefillCard` hijacking, pending action precedence over `savedInstanceState`, duplicate backstack push suppression, dual `FlowViewModelV2` elimination, dynamic color propagation, and focus transition backstack retention resolved; 100% unit tests passing) | **PlanScreenV2 (Main UI Screens)** (`PlanScreenV2.kt`, `PlanViewModelV2.kt`) |
| **#15** | **PlanScreenV2 (Main UI Screens)** | `PlanScreenV2.kt`, `PlanViewModelV2.kt`, `PlanAndComposerUpgradesV2.kt`, `PlanScreenV2AuditTest.kt` | **Verified Clean** (Part 2 continuation completed task overwrite and deletion in `autoScheduleDay` fixed, dynamic circadian peak & static peak detection added to `distinctUntilChanged`, `shiftTaskToPeak` dynamic peak fallback & `habitDate` retention & `AutonomyNudgeEngine` notification scheduling wired across all rescheduling/editing actions, completion Undo snackbar race condition resolved, cognitive budget meter calibrated with `TaskBlockPlan.plannedMinutes`; 100% unit tests passing) | **FlowScreenV2 (Main UI Screens)** (`FlowScreenV2.kt`, `FlowViewModelV2.kt`, `FlowHubUpgradesV2.kt`) |
| **#16** | **FlowScreenV2 (Main UI Screens - Second Cycle Hardening)** | `FlowScreenV2.kt`, `FlowViewModelV2.kt`, `FlowHubUpgradesV2.kt`, `EnergyTimelineStream.kt`, `FlowScreenV2AuditTest.kt` | **Verified Clean** (Circadian peak `distinctUntilChanged` preference flow retention, split Part 2 continuation actionable inclusion & recommendation eligibility, autonomy nudge scheduling across task updates, undo completion and undo deferral, `TaskBlockPlan.plannedMinutes` duration meter calibration, and recurring habit `habitDate` emergency reset retention resolved; 100% unit tests passing) | **FocusScreenV2 (Main UI Screens)** (`FocusScreenV2.kt`, `FocusViewModelV2.kt`, `FocusService.kt`, `FocusScreenV2AuditTest.kt`) |
| **#17** | **FocusScreenV2 (Main UI Screens - Second Cycle Hardening)** | `FocusScreenV2.kt`, `FocusViewModelV2.kt`, `FocusService.kt`, `FocusScreenV2AuditTest.kt` | **Verified Clean** (Premature completion status drop in `completeAndRecur` fixed, modal celebration trap on already-completed tasks removed with dedicated flag, autonomy nudge cancellation in service & rescheduling on non-started exit wired, frozen 00:00 timer reset on decompression exit, and `TaskBlockPlan.plannedMinutes` duration meter & MAPE calibration applied; 100% unit tests passing) | **Main Core Logic (Second Cycle Hardening)** (`TaskSchedulerService.kt`, `AutoSchedulingRationales.kt`, `PlacementStability.kt`, `DependencyGraphValidator.kt`) |
| **#18** | **Main Core Logic (Second Cycle Hardening)** | `TaskSchedulerService.kt`, `AutoSchedulingRationales.kt`, `PlacementStability.kt`, `SchedulePreparationOrchestrator.kt`, `SchedulerAuditTest.kt` | **Verified Clean** (Phase 1 headroom ceiling for ultradian split Part 2, future-day dateless task anchoring in orchestrator, parameterized `nowMillis` epoch resolution in `PlacementStability.verdict`, cognitive load meter duration calibration in `AutoSchedulingEngineCoordinator.planSchedule`, zero-width/inverted interval protection in `subtractInterval`, and parentTaskId linking on Part 2 resolved; 100% unit tests passing) | **Secondary UI & Features (Second Cycle Hardening)** (`SmartTaskComposerSheetV2.kt`, `QuickNotesBottomSheet.kt`, `CalendarSyncDataSource.kt`, `TaskTagViewModel.kt`) |
| **#19** | **Secondary UI & Features (Second Cycle Hardening)** | `SmartTaskComposerSheetV2.kt`, `QuickNotesBottomSheet.kt`, `CalendarSyncDataSource.kt`, `TaskTagViewModel.kt`, `FlowViewModelV2.kt`, `PlanViewModelV2.kt`, `SmartTaskComposerSheetV2Test.kt`, `CalendarWriteBackSyncTest.kt`, `TaskTagAndCategoryTest.kt` | **Verified Clean** (Untimed edit duration collapse to 0 fixed with archetype fallback, due time without date cross-anchoring to scheduled date resolved, calendar sync dateless timed anchor to effective date and TaskBlockPlan duration standardization applied, tag catalog dynamic synchronization across viewmodels wired, and analytical keyword expansion verified; 100% unit tests passing) | **Deeper Subsystems & Background Services (Second Cycle Hardening)** (`NeuralSoundscapeGenerator.kt`, `NeuralSoundscapePlayer.kt`, `FocusService.kt`, `ScheduleAutoTasksWorker.kt`, `NotificationWorker.kt`, `FocusWidgetUpdateWorker.kt`) |
| **#20** | **Deeper Subsystems & Background Services (Second Cycle Hardening)** | `NeuralSoundscapeGenerator.kt`, `NeuralSoundscapePlayer.kt`, `FocusService.kt`, `ScheduleAutoTasksWorker.kt`, `NotificationWorker.kt`, `FocusWidgetUpdateWorker.kt`, `AutonomyNudgeWorker.kt`, `AutonomyNudgeEngine.kt`, `TaskRepository.kt`, `NotificationWorkerAuditTest.kt`, `AutonomyNudgeWorkerTest.kt`, `FocusWidgetAuditTest.kt` | **Verified Clean** (Sprint complete notification cancellation race condition resolved with separate notification ID & widget refresh wired, autonomy nudge future-deadline false suppression fixed, repository write-path autonomy nudge synchronization via WorkManager enabled, Part 2 continuation eligibility in DailyPlanWorker restored, TaskBlockPlan planned duration metrics standardized, and clean session tracking metrics reset on fresh Part 2 split continuation verified; 100% unit tests passing) | **Deep Concurrency, Database Migrations & Edge Cases (Second Cycle Hardening)** (`NeuroFlowDatabase.kt`, `Converters.kt`, `SessionRepository.kt`, `TaskRepository.kt`, `TimeSessionDao.kt`, `TaskDao.kt`) |
| **#21** | **Deep Concurrency, Database Migrations & Edge Cases (Second Cycle Hardening)** | `NeuroFlowDatabase.kt`, `Converters.kt`, `SessionRepository.kt`, `TaskRepository.kt`, `TimeSessionDao.kt`, `TaskDao.kt`, `RoomDatabaseAuditTest.kt` | **Verified Clean** (Type converter null crash resilience, subtask cascade deletion and dependency deadlock cleanup on Part 2/subtasks, zombie open time session leak prevention, task session splitting prerequisite and schedule preservation, and idempotent micro-step migration resolved; 100% unit tests passing) | **Main UI & Navigation (Third Cycle Hardening)** (`NeuroFlowAppV2.kt`, `MainActivity.kt`, `NavHubsV2.kt`) |
| **#22** | **Main UI & Navigation (Third Cycle Hardening)** | `NeuroFlowAppV2.kt`, `MainActivity.kt`, `NavHubsV2.kt`, `NavigationAuditTest.kt` | **Verified Clean** (Launch intent extra retention across configuration changes preventing reopen of completed tasks, session guard `rememberSaveable` persistence, and URI-encoding / `launchSingleTop` across all `focus_v2` routes resolved; 100% unit tests passing) | **PlanScreenV2 (Main UI Screens - Third Cycle Hardening)** (`PlanScreenV2.kt`, `PlanViewModelV2.kt`, `PlanAndComposerUpgradesV2.kt`) |
| **#23** | **PlanScreenV2 (Main UI Screens - Third Cycle Hardening)** | `PlanScreenV2.kt`, `PlanViewModelV2.kt`, `PlanAndComposerUpgradesV2.kt`, `PlanScreenV2AuditTest.kt` | **Verified Clean** (Dynamic circadian peak synchronization in timeline view, `TaskBlockPlan.plannedMinutes` duration meter & ultradian rest buffer calibration, unscheduled backlog & icebox fixed window false blocking removal, `contextTag` matching in tag filtering, dual-representation epoch timestamp plan ingestion, and autonomy nudge rescheduling on uncomplete & quadrant cycle resolved; 100% unit tests passing) | **FlowScreenV2 (Main UI Screens - Third Cycle Hardening)** (`FlowScreenV2.kt`, `FlowViewModelV2.kt`, `FlowHubUpgradesV2.kt`) |
| **#24** | **FlowScreenV2 (Main UI Screens - Third Cycle Hardening)** | `FlowScreenV2.kt`, `FlowViewModelV2.kt`, `FlowHubUpgradesV2.kt`, `EnergyTimelineStream.kt`, `FlowScreenV2AuditTest.kt` | **Verified Clean** (Dual-representation epoch timestamp leak & deadline drop resolved, `task.contextTag` inclusion in dynamic tags & filtering & deep work minutes wired, manual circadian peak override & epoch anchor in `shiftTaskToPeak` preserved, and split continuation `_part2` indicator fixed; 100% unit tests passing) | **FocusScreenV2 (Main UI Screens - Third Cycle Hardening)** (`FocusScreenV2.kt`, `FocusViewModelV2.kt`, `FocusService.kt`, `FocusAndRegulationUpgradesV2.kt`) |
| **#25** | **FocusScreenV2 (Main UI Screens - Third Cycle Hardening)** | `FocusScreenV2.kt`, `FocusViewModelV2.kt`, `FocusService.kt`, `FocusAndRegulationUpgradesV2.kt`, `FocusScreenV2AuditTest.kt` | **Verified Clean** (Foreground service notification ID collision resolved, external completion state synchronization fixed, dual-representation timestamp next task ranking & blocked task filtering enabled, decompression zone `startedAtMillis` reset enforced to eliminate phantom minutes, and empty decomposition micro-step protection added; 100% unit tests passing) | **Main Core Logic (Third Cycle Hardening)** (`TaskSchedulerService.kt`, `AutoSchedulingRationales.kt`, `PlacementStability.kt`, `DependencyGraphValidator.kt`) |
| **#26** | **Main Core Logic (Third Cycle Hardening)** | `TaskSchedulerService.kt`, `AutoSchedulingRationales.kt`, `PlacementStability.kt`, `DependencyGraphValidator.kt`, `SchedulePreparationOrchestrator.kt`, `TaskTimeExtensions.kt`, `SchedulerAuditTest.kt` | **Verified Clean** (Continuation Part 2 duration & title clobbering resolved, `_part2` ultradian recovery buffer & end tracking aligned, downstream dependency chaining to split continuations enforced, intraday deadline epoch fallback enabled, and hysteresis Part 1 15m recovery buffer wired; 100% unit tests passing) | **Secondary UI & Features (Third Cycle Hardening)** (`SmartTaskComposerSheetV2.kt`, `QuickNotesBottomSheet.kt`, `CalendarSyncDataSource.kt`, `TaskTagViewModel.kt`) |
| **#27** | **Secondary UI & Features (Third Cycle Hardening)** | `SmartTaskComposerSheetV2.kt`, `QuickNotesBottomSheet.kt`, `CalendarSyncDataSource.kt`, `TaskTagViewModel.kt`, `UserPreferencesDataStore.kt`, `SmartTaskComposerSheetV2Test.kt`, `CalendarWriteBackSyncTest.kt`, `TaskTagAndCategoryTest.kt` | **Verified Clean** (Cleared scheduled and deadline date resurrection prevented, `contextTag` synchronization across creation & editing wired, note-to-task conversion title preservation enforced, calendar cloud sync event duplication prevented via description marker fallback, and comma-separated tag catalog splitting resolved; 100% unit tests passing) | **Deeper Subsystems & Background Services (Third Cycle Hardening)** (`NeuralSoundscapeGenerator.kt`, `NeuralSoundscapePlayer.kt`, `FocusService.kt`, `ScheduleAutoTasksWorker.kt`, `NotificationWorker.kt`, `FocusWidgetUpdateWorker.kt`) |
| **#28** | **Deeper Subsystems & Background Services (Third Cycle Hardening)** | `NeuralSoundscapeGenerator.kt`, `NeuralSoundscapePlayer.kt`, `FocusService.kt`, `ScheduleAutoTasksWorker.kt`, `NotificationWorker.kt`, `FocusWidgetUpdateWorker.kt`, `CompleteTaskWidgetAction.kt`, `TaskTimeExtensions.kt` | **Verified Clean** (Dateless timed task future date leak, notification action task identity loss & 50-year duration fallback, silent sprint completion alert channel, widget completion focus minutes drop, expired session auto-scheduling deadlock, overdue task deadline escalation & at-target reminder drops, and audio focus resume race condition resolved; 100% unit tests passing) | **Deep Concurrency, Database Migrations & Edge Cases (Third Cycle Hardening)** (`NeuroFlowDatabase.kt`, `Converters.kt`, `SessionRepository.kt`, `TaskRepository.kt`, `TimeSessionDao.kt`, `TaskDao.kt`) |
| **#29** | **Deep Concurrency, Database Migrations & Edge Cases (Third Cycle Hardening)** | `NeuroFlowDatabase.kt`, `Converters.kt`, `SessionRepository.kt`, `TaskRepository.kt`, `TimeSessionDao.kt`, `TaskDao.kt`, `FocusSessionDao.kt`, `TaskMicroStepDao.kt`, `RoomDatabaseAuditTest.kt` | **Verified Clean** (Foreign key cascade deletion on task insert/replace resolved with `@Upsert`, dateless timed task omission in `observeTasksForDate` & `getTasksForDate` fixed, `#part` continuation task cascade deletion & dependency cleanup in `TaskRepository.delete` enabled, recurring habit extinction on `splitTaskIntoSessions` prevented via `completeAndRecur`, absolute epoch timestamp schedule & deadline preservation in recurrence progression fixed, and `FocusSessionDao` task deletion synchronization wired; 100% unit tests passing) | **Main UI & Navigation (Fourth Cycle Hardening)** (`NeuroFlowAppV2.kt`, `MainActivity.kt`, `NavHubsV2.kt`) |
| **#30** | **Main UI & Navigation (Fourth Cycle Hardening)** | `NeuroFlowAppV2.kt`, `MainActivity.kt`, `NavHubsV2.kt`, `NavigationAuditTest.kt`, `NeuroFlowV2UiTest.kt` | **Verified Clean** (Historical launch intent re-execution suppressed on recents resume, `ACTION_MAIN` action preservation, asynchronous focus resolution race guarded with `isResolvingFocusTask`, future-scheduled and blocked Part 2 task exclusion in `ACTION_OPEN_FOCUS` fallback, blank task ID consumption to prevent permanent dialog lockout, and full-screen layout sizing resolved; 100% unit tests passing) | **PlanScreenV2 (Main UI Screens - Fourth Cycle Hardening)** (`PlanScreenV2.kt`, `PlanViewModelV2.kt`, `PlanAndComposerUpgradesV2.kt`) |
| **#31** | **PlanScreenV2 (Main UI Screens - Fourth Cycle Hardening)** | `PlanScreenV2.kt`, `PlanViewModelV2.kt`, `PlanAndComposerUpgradesV2.kt`, `PlanScreenV2AuditTest.kt` | **Verified Clean** (Intraday deadline epoch date anchoring in `tasksForDay`, unscheduled backlog preservation & absolute epoch timestamp normalization in `addTask`, iceboxed task exclusion from timeline & unscheduled staging tray, `contextTag` inclusion in search queries, manual circadian peak override across timeline & `shiftTaskToPeak`, split Part 1/2 ultradian buffer restoration with untimed suppression, and note title preservation & quadrant cycle Frog clearing resolved; 100% unit tests passing) | **FlowScreenV2 (Main UI Screens - Fourth Cycle Hardening)** (`FlowScreenV2.kt`, `FlowViewModelV2.kt`, `FlowHubUpgradesV2.kt`) |
| **#32** | **FlowScreenV2 (Main UI Screens - Fourth Cycle Hardening)** | `FlowScreenV2.kt`, `FlowViewModelV2.kt`, `FlowHubUpgradesV2.kt`, `EnergyTimelineStream.kt`, `FlowScreenV2AuditTest.kt` | **Verified Clean** (Unscheduled backlog date resurrection & epoch scheduledTime normalization, explicit note title preservation & breakdown sync, `#part2` split continuation & blocked prerequisite spotlight prevention, manual peak end hour alignment, and untimed task ultradian buffer/NOW marker suppression resolved; 100% unit tests passing) | **FocusScreenV2 (Main UI Screens - Fourth Cycle Hardening)** (`FocusScreenV2.kt`, `FocusViewModelV2.kt`, `FocusService.kt`, `FocusAndRegulationUpgradesV2.kt`) |
| **#33** | **FocusScreenV2 (Main UI Screens - Fourth Cycle Hardening)** | `FocusScreenV2.kt`, `FocusViewModelV2.kt`, `FocusService.kt`, `FocusAndRegulationUpgradesV2.kt`, `FocusScreenV2AuditTest.kt` | **Verified Clean** (`#part2` split continuation queue leakage & prerequisite blocking, completeTask elapsed time background recalculation, FocusService overdue sprint duration capping & end anchoring, AI breakdown skippedMicroStepIndices reset, and decompression rest timer state leakage & auto-tracker review guard resolved; 100% unit tests passing) | **Main Core Logic (Fourth Cycle Hardening)** (`TaskSchedulerService.kt`, `AutoSchedulingRationales.kt`, `PlacementStability.kt`, `DependencyGraphValidator.kt`, `SchedulePreparationOrchestrator.kt`) |
| **#34** | **Main Core Logic (Fourth Cycle Hardening)** | `TaskSchedulerService.kt`, `AutoSchedulingRationales.kt`, `PlacementStability.kt`, `DependencyGraphValidator.kt`, `SchedulePreparationOrchestrator.kt`, `SchedulerAuditTest.kt` | **Verified Clean** (Hysteresis Part 2 completionEndMap synchronization, workday end headroom ceiling in Phase 1 ultradian split, regex-safe split task ID extraction without `_part` corruption, unplaced Part 2 silent drop prevention, parameterized `nowMillis` deadline epoch resolution in `PlacementStability`, and parentTaskId prerequisite inheritance resolved; 100% unit tests passing) | **Secondary UI & Features (Fourth Cycle Hardening)** (`SmartTaskComposerSheetV2.kt`, `QuickNotesBottomSheet.kt`, `CalendarSyncDataSource.kt`, `TaskTagViewModel.kt`) |
| **#35** | **Secondary UI & Features (Fourth Cycle Hardening)** | `SmartTaskComposerSheetV2.kt`, `QuickNotesBottomSheet.kt`, `CalendarSyncDataSource.kt`, `TaskTagViewModel.kt`, `SmartTaskComposerSheetV2Test.kt`, `CalendarWriteBackSyncTest.kt`, `CalendarSyncRepositoryTest.kt` | **Verified Clean** (Selective onValueChange NLP directive override preservation, dangling intraday time & phantom deadline cleanup on date clear, initialDate habit date anchoring in Create mode, TimePicker date un-clearing, title-only quick note AI synthesis trigger, per-item SimpleDateFormat caching, deprecated icon migration, and deleted tombstone filtering & title prefix deduplication in CalendarSync resolved; 100% unit tests passing) | **Deeper Subsystems & Background Services (Fourth Cycle Hardening)** (`NeuralSoundscapeGenerator.kt`, `NeuralSoundscapePlayer.kt`, `FocusService.kt`, `ScheduleAutoTasksWorker.kt`, `NotificationWorker.kt`, `FocusWidgetUpdateWorker.kt`) |
| **#36** | **Deeper Subsystems & Background Services (Fourth Cycle Hardening)** | `NeuralSoundscapeGenerator.kt`, `NeuralSoundscapePlayer.kt`, `FocusService.kt`, `ScheduleAutoTasksWorker.kt`, `NotificationWorker.kt`, `FocusWidgetUpdateWorker.kt`, `CompleteTaskWidgetAction.kt`, `SleepPressureRefreshWorker.kt`, `FocusWidgetAuditTest.kt`, `FocusSessionRecoveryTest.kt` | **Verified Clean** (Split continuation `#part2` & subtasks blocked when parent active in widgets & daily plan, one-tap widget completion sprint duration capped & end time anchored, scheduler worker Part 2 candidate eligibility when parent completed & regex grouping & `(Part 2/2)` title preservation, Icebox `Quadrant.ELIMINATE` deadline escalation suppression, epoch deadline timestamp autonomy nudge recovery, AudioTrack release pending reset on rapid re-entry, and CancellationException rethrown across CoroutineWorkers; 100% unit tests passing) | **Deep Concurrency, Database Migrations & Edge Cases (Fourth Cycle Hardening)** (`NeuroFlowDatabase.kt`, `SessionRepository.kt`, `TaskRepository.kt`, `Converters.kt`) |
| **#37** | **Deep Concurrency, Database Migrations & Edge Cases (Fourth Cycle Hardening)** | `NeuroFlowDatabase.kt`, `SessionRepository.kt`, `TaskRepository.kt`, `Converters.kt`, `TimeSessionDao.kt`, `TaskDao.kt`, `FocusSessionDao.kt`, `TaskMicroStepDao.kt`, `PlanViewModelV2.kt`, `RoomDatabaseAuditTest.kt` | **Verified Clean** (Zombie open session leakage & tracked minutes drop in completeAndRecur resolved, regex-safe continuation slug extraction in delete & autoScheduleDay enforced, continuation part recurrence advancement prevention wired, micro-step cloning in splitTaskIntoSessions enabled, SQL indexed range querying in getSessionsSince and null/zero scheduledDate resolution verified; 100% unit tests passing) | **Main UI & Navigation (Fifth Cycle Hardening)** (`NeuroFlowAppV2.kt`, `MainActivity.kt`, `NavHubsV2.kt`) |
| **#38** | **Main UI & Navigation (Fifth Cycle Hardening)** | `NeuroFlowAppV2.kt`, `MainActivity.kt`, `NavHubsV2.kt`, `NavigationAuditTest.kt`, `NeuroFlowV2UiTest.kt` | **Verified Clean** (In-flight async focus resolution coroutine resumed on activity recreation to prevent zombie state, multi-part continuation tasks `#part3`/`_part3` blocked when parent active via regex-safe matching in `ACTION_OPEN_FOCUS` fallback, `launchSingleTop = true` applied across all sub-screens to debounce rapid clicks, external focus/WOOP backstack replacement enabled, and unhandled intent action fallback wired; 100% unit tests passing) | **PlanScreenV2 (Main UI Screens - Fifth Cycle Hardening)** (`PlanScreenV2.kt`, `PlanViewModelV2.kt`, `PlanAndComposerUpgradesV2.kt`) |
| **#39** | **PlanScreenV2 (Main UI Screens - Fifth Cycle Hardening)** | `PlanScreenV2.kt`, `PlanViewModelV2.kt`, `PlanAndComposerUpgradesV2.kt`, `PlanScreenV2AuditTest.kt` | **Verified Clean** (Continuation Part 2 auto-scheduling eligibility and standalone title/deletion preservation, overdue task inclusion on Today's timeline and future unscheduled tray, hour/minute modulo sanitization preventing 1970 epoch reset, dual-representation deadline normalization, contextTag synchronization, and Icebox autonomy nudge suppression resolved; 100% unit tests passing) | **FlowScreenV2 (Main UI Screens - Fifth Cycle Hardening)** (`FlowScreenV2.kt`, `FlowViewModelV2.kt`, `FlowHubUpgradesV2.kt`) |
| **#40** | **FlowScreenV2 (Main UI Screens - Fifth Cycle Hardening)** | `FlowScreenV2.kt`, `FlowViewModelV2.kt`, `FlowHubUpgradesV2.kt`, `EnergyTimelineStream.kt`, `FlowScreenV2AuditTest.kt` | **Verified Clean** (Multi-part continuation tasks `#part3`/`_part3` blocked when parent active & trailing comma dependency parsing enabled, absolute epoch deadline normalization & contextTag synchronization & Icebox autonomy nudge cancellation wired, overdue & untimed task deferral from `now` preserved, and timeline task time-of-day circadian alignment & multi-part split buffer/labeling resolved; 100% unit tests passing) | **FocusScreenV2 (Main UI Screens - Fifth Cycle Hardening)** (`FocusScreenV2.kt`, `FocusViewModelV2.kt`, `FocusService.kt`, `FocusAndRegulationUpgradesV2.kt`) |
| **#41** | **FocusScreenV2 (Main UI Screens - Fifth Cycle Hardening)** | `FocusScreenV2.kt`, `FocusViewModelV2.kt`, `FocusService.kt`, `FocusAndRegulationUpgradesV2.kt`, `FocusScreenV2AuditTest.kt` | **Verified Clean** (Dynamic multi-part continuation blocking `[#_]part(\d+)` and preceding active part check in `findNextActionableTask()` enforced, future scheduled day filtering & queue exhaustion fallback to null wired, sprint completion & expired recovery session count / duration persistence across decompression rest preserved, and completed task micro-step badge suppression & expired timer widget sync verified; 100% unit tests passing) | **Main Core Logic (Fifth Cycle Hardening)** (`TaskSchedulerService.kt`, `AutoSchedulingRationales.kt`, `PlacementStability.kt`, `DependencyGraphValidator.kt`, `SchedulePreparationOrchestrator.kt`) |
| **#42** | **Main Core Logic (Fifth Cycle Hardening)** | `TaskSchedulerService.kt`, `AutoSchedulingRationales.kt`, `PlacementStability.kt`, `DependencyGraphValidator.kt`, `SchedulePreparationOrchestrator.kt`, `SchedulerAuditTest.kt` | **Verified Clean** (Multi-part continuation final part selection determinism in DAG chaining, continuation part prerequisite inference, cross-day parent prerequisite deadlock prevention, and Part 1 continuation isolation resolved; 100% unit tests passing) | **Secondary UI & Features (Fifth Cycle Hardening)** (`SmartTaskComposerSheetV2.kt`, `QuickNotesBottomSheet.kt`, `CalendarSyncDataSource.kt`, `TaskTagViewModel.kt`) |
| **#43** | **Secondary UI & Features (Fifth Cycle Hardening)** | `SmartTaskComposerSheetV2.kt`, `QuickNotesBottomSheet.kt`, `CalendarSyncDataSource.kt`, `TaskTagViewModel.kt`, `UserPreferencesDataStore.kt`, `TaskInferenceEngine.kt`, `SmartTaskComposerSheetV2Test.kt`, `CalendarWriteBackSyncTest.kt`, `TaskTagAndCategoryTest.kt` | **Verified Clean** (Intraday deadline classification and due keyword parsing without day tokens resolved, implicit clock time date override preservation across Create & Edit modes enabled, deadline-only task calendar focus block anchor alignment & stale event update fallback wired, and multi-hashtag space-separated tag catalog splitting resolved; 100% unit tests passing) | **Deeper Subsystems & Background Services (Fifth Cycle Hardening)** (`NeuralSoundscapeGenerator.kt`, `NeuralSoundscapePlayer.kt`, `FocusService.kt`, `ScheduleAutoTasksWorker.kt`, `NotificationWorker.kt`, `FocusWidgetUpdateWorker.kt`) |
| **#44** | **Deeper Subsystems & Background Services (Fifth Cycle Hardening)** | `NeuralSoundscapeGenerator.kt`, `NeuralSoundscapePlayer.kt`, `FocusService.kt`, `ScheduleAutoTasksWorker.kt`, `NotificationWorker.kt`, `FocusWidgetUpdateWorker.kt`, `AutonomyNudgeWorker.kt`, `CompleteTaskWidgetAction.kt`, `ScheduleAutoTasksWorkerTest.kt`, `NotificationWorkerAuditTest.kt`, `AutonomyNudgeWorkerTest.kt` | **Verified Clean** (Standalone Part 2 continuation scheduling preservation in `ScheduleAutoTasksWorker`, multi-part continuation `[#_]part(\d+)` and preceding active part blocking across workers, `DailyPlanWorker` trailing comma dependency parsing & `prerequisiteIds` wiring, `AutonomyNudgeWorker` prerequisite and continuation blocking, and `CompleteTaskWidgetAction` / `FocusService` reminder, nudge & notification cancellation resolved; 100% unit tests passing) | **Deep Concurrency, Database Migrations & Edge Cases (Fifth Cycle Hardening)** (`NeuroFlowDatabase.kt`, `Converters.kt`, `SessionRepository.kt`, `TaskRepository.kt`, `TimeSessionDao.kt`, `TaskDao.kt`) |
| **#45** | **Deep Concurrency, Database Migrations & Edge Cases (Fifth Cycle Hardening)** | `NeuroFlowDatabase.kt`, `Converters.kt`, `SessionRepository.kt`, `TaskRepository.kt`, `TimeSessionDao.kt`, `TaskDao.kt`, `FocusSessionDao.kt`, `TaskMicroStepDao.kt`, `RoomDatabaseAuditTest.kt` | **Verified Clean** (Continuation part recurrence advancement suppression across `#part3+`, dynamic cascade deletion & dependency cleanup on multi-part continuations in `TaskDao` & `TaskRepository`, scheduled date habit isolation in `observeTasksForDate` & `rescheduleToTomorrow`, `energyAtStart` preservation in `SessionRepository.syncFocusSession`, and lazy `ifThenPlan` micro-step cloning in `splitTaskIntoSessions` resolved; 100% unit tests passing) | **Main UI & Navigation (Sixth Cycle Hardening)** (`NeuroFlowAppV2.kt`, `MainActivity.kt`, `NavHubsV2.kt`) |
| **#46** | **Main UI & Navigation (Sixth Cycle Hardening)** | `NeuroFlowAppV2.kt`, `MainActivity.kt`, `NavHubsV2.kt`, `NavigationAuditTest.kt`, `NeuroFlowV2UiTest.kt` | **Verified Clean** (Non-actionable `waitingFor` task selection resolved, multi-part continuation `#part1` false blocking & preceding active part checking enforced, chronological scheduled time prioritization enabled, redundant active focus screen teardown & timer reset suppressed, async WOOP reflection resolution state preserved across activity recreation, alternate intent extra `extra_task_id` compatibility wired, and onboarding task metadata preserved; 100% unit tests passing) | **PlanScreenV2 (Main UI Screens - Sixth Cycle Hardening)** (`PlanScreenV2.kt`, `PlanViewModelV2.kt`, `PlanAndComposerUpgradesV2.kt`) |
| **#47** | **PlanScreenV2 (Main UI Screens - Sixth Cycle Hardening)** | `PlanScreenV2.kt`, `PlanViewModelV2.kt`, `PlanAndComposerUpgradesV2.kt`, `PlanScreenV2AuditTest.kt` | **Verified Clean** (Multi-part continuation preceding part blocking & parent slug extraction in `autoScheduleDay` fixed, dynamic multi-part continuation labeling & consecutive split buffers in `TimelineScheduleView` resolved, midnight-wrapping circadian peak shift in `shiftTaskToPeak` preserved, recurring task `habitDate` anchoring in `addTask` and Icebox autonomy nudge cancellation in `uncompleteTask` wired; 100% unit tests passing) | **FlowScreenV2 (Main UI Screens - Sixth Cycle Hardening)** (`FlowScreenV2.kt`, `FlowViewModelV2.kt`, `FlowHubUpgradesV2.kt`, `EnergyTimelineStream.kt`) |
| **#48** | **FlowScreenV2 (Main UI Screens - Sixth Cycle Hardening)** | `FlowScreenV2.kt`, `FlowViewModelV2.kt`, `FlowHubUpgradesV2.kt`, `EnergyTimelineStream.kt`, `FlowScreenV2AuditTest.kt` | **Verified Clean** (Continuation self-blocking on UUID split tasks in `isContinuationBlocked` & `FlowSpotlightHeroCard` resolved, midnight-wrapping circadian peak shift in `shiftTaskToPeak` preserved, recurring task `habitDate` anchoring in `addTask` & Icebox autonomy nudge cancellation in `breakDownTask`/`shiftTaskToPeak` wired, and untimed split part ultradian buffer suppression in `shouldShowUltradianBufferAfter` fixed; 100% unit tests passing) | **FocusScreenV2 (Main UI Screens - Sixth Cycle Hardening)** (`FocusScreenV2.kt`, `FocusViewModelV2.kt`, `FocusService.kt`, `FocusAndRegulationUpgradesV2.kt`) |
| **#49** | **FocusScreenV2 (Main UI Screens - Sixth Cycle Hardening)** | `FocusScreenV2.kt`, `FocusViewModelV2.kt`, `FocusService.kt`, `FocusAndRegulationUpgradesV2.kt`, `FocusScreenV2AuditTest.kt` | **Verified Clean** (Eliminate icebox autonomy nudge leakage on focus exit fixed, reminder notification & WorkManager cancellation on completion enforced, UUID split continuation sequential blocking wired, FocusService ACTION_PAUSE null session drop & stale seconds recalculated, and trapped completed state & zombie WOOP prompt resolved; 100% unit tests passing) | **Main Core Logic (Seventh Cycle Hardening)** (`TaskSchedulerService.kt`, `AutoSchedulingRationales.kt`, `PlacementStability.kt`, `DependencyGraphValidator.kt`, `PlanViewModelV2.kt`, `FlowViewModelV2.kt`) |
| **#50** | **Main Core Logic (Seventh Cycle Hardening)** | `TaskSchedulerService.kt`, `AutoSchedulingRationales.kt`, `PlacementStability.kt`, `DependencyGraphValidator.kt`, `SchedulePreparationOrchestrator.kt`, `AutoSchedulingContracts.kt`, `SchedulerAuditTest.kt` | **Verified Clean** (Part 1 and subtask continuation misclassification & premature parent child unlocking resolved, UUID continuation cross-day prerequisite deadlock & hysteresis recognition fixed, untimed date-only task midnight start anchor & Part 1 parent dependency omission corrected, and dual-representation epoch manual schedule protection verified; 100% unit tests passing) | **Secondary UI & Features (Sixth Cycle Hardening)** (`SmartTaskComposerSheetV2.kt`, `QuickNotesBottomSheet.kt`, `CalendarSyncDataSource.kt`, `TaskTagViewModel.kt`) |

---

### Summary of Findings

| ID | Title | Severity | Category | File & Line | Status |
|---|---|---|---|---|---|
| **BUG-001** | Coroutine Creation During Compose Composition | High | Runtime / Concurrency | [MainActivity.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L305-L320) | Resolved |
| **BUG-002** | Redundant / Dead Condition in MiniFocusBar Check | Low | Code Quality / Dead Code | [NeuroFlowAppV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L107-L122) | Resolved |
| **BUG-003** | `QUERY_ALL_PACKAGES` Lint Error and Policy Risk | Medium | Android Manifest / Policy | [AndroidManifest.xml](file:///home/takian/Project/PROCUS/app/src/main/AndroidManifest.xml#L25) | Resolved |
| **BUG-004** | Midnight Boundary Flakiness in Analytics Clamping Test | Medium | Unit Testing / Date Boundary | [Adr030RobustnessTest.kt](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/Adr030RobustnessTest.kt#L206-L235) | Resolved |
| **BUG-005** | Exclusion of Focused Tasks from `fixedWindows` in PlanViewModelV2 | High | Scheduling Engine / Overlap | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L446-L460) | Resolved |
| **BUG-006** | In-Progress Task Not Frozen in `PlacementStability.verdict` | Medium | Scheduler Stability / Churn | [PlacementStability.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/PlacementStability.kt#L59-L62) | Resolved |
| **BUG-007** | Duplicate Task Scheduling via Synthetic Continuation Part 2 Entities (`_part2`) | High | Data Integrity / Task Duplication | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L455), [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L141-L182) | Resolved |
| **BUG-008** | Accidental Deletion of Vice Head Skill in Commit `26d117d` | Low | Workspace Invariant / Living Skill | [.agents/skills/vice-head/SKILL.md](file:///home/takian/Project/PROCUS/.agents/skills/vice-head/SKILL.md) | Resolved |
| **BUG-009** | Background Auto-Scheduler Ingests Future Tasks and Displaces Manual Schedules | High | Scheduling Engine / Temporal Leaks | [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L141-L188) | Resolved |
| **BUG-010** | Locked Task Fallback Displaces Pinned Slot in `TaskSchedulerService` | Medium | Scheduler Correctness / Invariant Violation | [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L678-L709) | Resolved |
| **BUG-011** | Preserved `isAutoScheduled = true` Overrides User Manual Schedule Edits in Composer | Medium | Data Integrity / User Intent Override | [SmartTaskComposerSheetV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L234-L255) | Resolved |
| **BUG-012** | Orphaned Continuation Tasks on Parent Deletion & Completed Parent Continuation Collision | Medium | Data Integrity / Continuation Lifecycle | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L207-L225), [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L455-L465) | Resolved |
| **BUG-013** | Dead Code and Unhandled Intent Action in `FocusWidgetProvider` | Low | Code Quality / Dead Code | [FocusWidgetProvider.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/widget/FocusWidgetProvider.kt#L38-L49) | Resolved |
| **BUG-014** | `.gitignore` Lacks Exclusion for Generated Code Bundles (`core_engine_code/`, `ui_code/`) | Low | Tooling / Version Control Hygiene | [.gitignore](file:///home/takian/Project/PROCUS/.gitignore#L65-L72) | Resolved |
| **BUG-015** | Inaccurate Dependency Block Deferral Rationale in Sweep | Low | Scheduler Accuracy / Diagnostics | [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L944-L966) | Resolved |
| **BUG-016** | Chronotype Precedence Inversion in Widget & Notification Workers | Medium | Chronobiology Consistency | [FocusWidgetUpdateWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L64), [NotificationWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L162) | Resolved |
| **BUG-017** | Redundant Consecutive Room DB Writes on Focus Session Completion | Low | Performance / Redundant I/O | [FocusService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L384-L386) | Resolved |
| **BUG-018** | Date-Only Tasks Resolving to Midnight Trigger Premature Expiration | Medium | Scheduling Engine / Temporal Logic | [TaskTimeExtensions.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/entity/TaskTimeExtensions.kt#L37-L39), [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L147-L160) | Resolved |
| **BUG-019** | Self-Defeating `buildBlockedTaskIds` Candidate Inclusion Tautology | Medium | Scheduling Pre-Filter / Logic Bug | [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L134-L144), [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L344-L349) | Resolved |
| **BUG-020** | Continuation Part 2 Inherits Recurrence Leading to Exponential Task Duplication | High | Data Integrity / Recurrence Lifecycle | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L540-L558), [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L295-L315), [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L320-L340) | Resolved |
| **BUG-021** | Bypassed Timestamp Normalization in Widget & Notification Workers Causes Year-2082 & Time-Skew Calculations | Medium | Data Integrity / Temporal Logic | [FocusWidgetUpdateWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L103-L104), [NotificationWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L201-L202) | Resolved |
| **BUG-022** | Deleted Frog and Locked Tasks Leave Orphaned Events in Native Android Calendar | Low | Integration / Calendar Write-Back Lifecycle | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L207-L227) | Resolved |
| **BUG-023** | `replanCandidates` Overwrites `missedTasks` Cleared Timestamps in `ScheduleAutoTasksWorker` | High | Scheduling Engine / State Overwrite | [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L170-L186) | Resolved |
| **BUG-024** | Manual Timeline Time Assignment Preserves `isAutoScheduled = true` in `PlanViewModelV2` | Medium | Data Integrity / User Intent Override | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L804-L830) | Resolved |
| **BUG-025** | Concurrent Auto-Schedule Race Condition Between UI Coroutine and WorkManager in Plan Screen | Medium | Concurrency / Race Condition | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L583-L587) | Resolved |
| **BUG-026** | `workDayEnd` Midnight Value (0) Inversion in `TaskSchedulerService` | Low | Temporal Logic / Boundary Edge Case | [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L427-L435) | Resolved |
| **BUG-027** | Background Auto-Scheduler Traps Prior-Day Missed Tasks Due to Day Horizon Boundary Filter | High | Scheduling Engine / Temporal Leaks & Ingestion | [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L182-L211), [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L446-L452) | Resolved |
| **BUG-028** | `PlacementStability.isMoveWorthApplying` Short-Circuit Traps Part 2 Continuations and Title Restorations in Background Worker | High | Scheduling Engine / State Inconsistency & Ghost Entities | [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L307-L370) | Resolved |
| **BUG-029** | Direct Deletion of Continuation Part 2 Leaves Parent Task Truncated with Permanent Halved Duration & Stale `(Part 1/2)` Title | Medium | Data Integrity / Continuation Lifecycle | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L207-L227) | Resolved |
| **BUG-030** | Midnight Value (`workDayEnd = 0`) Inversion in `ConflictResolver.findConflictFreeStart` | Low | Temporal Logic / Boundary Edge Case | [AutoSchedulingRationales.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingRationales.kt#L127-L139) | Resolved |
| **BUG-031** | Reconnecting to Running Focus Session Leaves Soundscape Muted & Overwrites Selected Profile | Medium | Audio Engine / UI State Desynchronization | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L169-L178) | Resolved |
| **BUG-032** | Date-Only Intraday Tasks Erroneously Classified as `NEAR_TERM_FREEZE` with `allowsReplan = false` | Medium | Scheduler Stability / Temporal Logic Bug | [PlacementStability.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/PlacementStability.kt#L63) | Resolved |
| **BUG-033** | Omission of `MONAURAL_GAMMA_40HZ` and `BETA_16HZ` from 500k-Frame PCM Clipping DSP Stress Test | Low | Unit Testing / Test Coverage Regression | [NeuralSoundscapeDspStressTest.kt](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapeDspStressTest.kt#L39-L46) | Resolved |
| **BUG-034** | Redundant and Inconsistent Calendar Allocations vs Canonical `startOfLocalDay` in `PlanViewModelV2` | Low | Code Quality / Redundant Code & Consistency | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L505-L512) | Resolved |
| **BUG-035** | Date-Only Tasks Resolving to Midnight Trigger False Overdue Guilt & Premature Icebox Rollover in `FlowViewModelV2` | High | UI & Flow / Temporal Logic Bug | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L327-L331) | Resolved |
| **BUG-036** | Task Edits & Unlocking/Un-frogging Bypass Calendar Write-Back Leaving Phantom Events on Native Calendar | Medium | Integration / Calendar Sync Write-Back Lifecycle | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L605), [CalendarSyncDataSource.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt#L378-L383) | Resolved |
| **BUG-037** | Material 3 DatePicker UTC-To-Local Day Shift Corrupts Scheduled & Deadline Dates Across Negative UTC Timezones | High | UI & Temporal Logic / Timezone Skew | [SmartTaskComposerSheetV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L1008), [ProtectedSlotBottomSheet.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/schedule/ProtectedSlotBottomSheet.kt#L737) | Resolved |
| **BUG-038** | Pending Task Deletion Evaporation on Concurrent Delete, Screen Navigation, or Cancellation in `PlanViewModelV2` | High | Data Integrity / Concurrency & State Management | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L944-L972), [PlanScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L101-L116) | Resolved |
| **BUG-039** | Streak Check Worker Erroneously Resets Daily Streak to 0 Due to Stale `lastActiveDate` and Missing Task Completion Check | High | Gamification / Background Worker Logic Bug | [NotificationWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L260-L270), [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L841-L854) | Resolved |
| **BUG-040** | Raw Timestamp Addition in `DeadlineEscalationWorker` Causes Year-2082 Bypasses and Midnight Dropouts | Medium | Temporal Logic / Worker Alert Failure | [NotificationWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L319-L325) | Resolved |
| **BUG-041** | Redundant Consecutive Room DB Writes on Focus Screen Task Completion | Low | Performance / Redundant SQLite Transactions | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L597-L598) | Resolved |
| **BUG-042** | Flow Screen Task Deferral Preserves `isAutoScheduled = true` Causing Immediate Background Scheduler Clashing | Medium | Scheduling Engine / User Intent Override | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L675-L693) | Resolved |
| **BUG-043** | Recurrence Instance Orphaned on Task Completion Undo in `FlowViewModelV2` and `PlanViewModelV2` | Medium | Data Integrity / Recurrence Lifecycle & Ghost Duplicates | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L660-L673), [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L856-L866) | Resolved |
| **BUG-044** | Missing `setContentIntent` on All Background Worker Notification Alerts | Medium | Android System Integration / Notification Routing | [NotificationWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L231-L241) | Resolved |
| **BUG-045** | Concurrent ModalBottomSheet Collision on Sprint & Task Completion in `FocusScreenV2` | High | UI / Compose State Management & Modal Collision | [FocusScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt#L648-L656), [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L629-L643) | Resolved |
| **BUG-046** | Fresh Start Emergency Reset Disconnect & False Empty Rollover in `FlowScreenV2` | High | UI & Domain Logic / ADHD Safety Net Failure | [FlowScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt#L272-L291), [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L755-L777) | Resolved |
| **BUG-047** | TimePicker Hardcoded to 12:00 PM Noon Overwriting Existing Due and Slot Times | Low | UI & Temporal Logic / UX Edge Case | [SmartTaskComposerSheetV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L1391-L1393) | Resolved |
| **BUG-048** | Calendar Sync Write-Back Bypass on Task Insertion in `FlowViewModelV2` and Task Undo in `undoDeferTask` | Medium | Integration / Native Calendar Sync Lifecycle | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L489-L494) | Resolved |
| **BUG-049** | Settings Screen Blocks Midnight (`workDayEnd = 0`) & Inverts Work Window on Start Hour Modification | Low | UI / Settings & Constraint Inversion | [SettingsScreen.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/settings/SettingsScreen.kt#L331-L337) | Resolved |
| **BUG-050** | Unfiltered Ingestion of Future, Blocked, and Icebox Tasks in `DailyPlanWorker` and `FocusWidgetUpdateWorker` | High | Background Workers & UI / Data Integrity | [NotificationWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L153), [FocusWidgetUpdateWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L53) | Resolved |
| **BUG-051** | Morning Notification and Widget Circadian Crash via 24-Hour Wake Inversion in `calculateWakeTimeMillis` | High | Chronobiology / Temporal Logic Bug | [NotificationWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L156-L174), [FocusWidgetUpdateWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L58-L76) | Resolved |
| **BUG-052** | Interactive Home Screen Widget Completion Bypasses Daily Streak, Activity Telemetry, and Native Calendar Write-Back | Medium | Integration & Gamification / State Desynchronization | [CompleteTaskWidgetAction.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/widget/CompleteTaskWidgetAction.kt#L29-L39), [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L343-L365) | Resolved |
| **BUG-053** | Autonomy Nudge Notification Body Lacks `setContentIntent` Launch Routing | Low | Android System Integration / Notification Routing | [AutonomyNudgeWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/AutonomyNudgeWorker.kt#L73-L88) | Resolved |
| **BUG-054** | Dead Code: Unwired `TaskReminderWorker` and Orphaned `reminderFlags` Schema Field | Low | Code Quality / Dead Code & Unwired Feature | [NotificationWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L368-L429), [TaskEntity.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/entity/TaskEntity.kt#L43) | Resolved |
| **BUG-055** | Multi-Timezone Date Inversion & Stale Date Display in `SleepLogsScreen` | High | Temporal Logic / UI State | [SleepLogsScreen.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/settings/SleepLogsScreen.kt#L81-L114) | Resolved |
| **BUG-056** | Un-Debounced Gemini AI Synthesizer Fires HTTP Keystroke Storm & Trips 429 Quota Guard in `QuickNotesBottomSheet` | High | Online AI / Quota & Network Rate Limiting | [QuickNotesBottomSheet.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/notes/QuickNotesBottomSheet.kt#L45-L63), [FlowScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt#L430) | Resolved |
| **BUG-057** | Soundscape Fade-Out Bypass and Speaker Pop on Disabling Audio Stream in `NeuralSoundscapePlayer` | Medium | Audio Engine / DSP Runtime Bug | [NeuralSoundscapePlayer.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapePlayer.kt#L122-L127) | Resolved |
| **BUG-058** | Circadian Alignment Rate & Hourly Density Corrupted by Epoch Timestamps in `InsightsViewModelV2` | Medium | Analytics & Chronobiology / Temporal Skew | [InsightsViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/insights/InsightsViewModelV2.kt#L160-L162) | Resolved |
| **BUG-059** | Recurring Task Completion Inherits Pre-Checked Micro-Steps (`ifThenPlan`) Breaking Habit Checklists in `TaskRepository` | Medium | Data Integrity / Recurrence Lifecycle | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L412-L438) | Resolved |
| **BUG-060** | Phantom 10-Minute Workday Start Offset & Locked-Task Rejection via Defaulted `maxPrereqEnd` in `TaskSchedulerService` | High | Scheduling Engine / Logic Bug | [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L679-L685) | Resolved |
| **BUG-061** | Unfiltered Ingestion of Icebox Tasks (`Quadrant.ELIMINATE`) in `ScheduleAutoTasksWorker` Displaces Legitimate Schedule Slots | High | Scheduling Engine / Candidate Selection Bug | [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L176-L201) | Resolved |
| **BUG-062** | Missing Zone-Aware Resolution and Potential Crash on Null `scheduledDate` in `ConflictResolver.analyzeScheduleConflicts` | Medium | Scheduling Engine / Null Safety & Skew | [AutoSchedulingRationales.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingRationales.kt#L151), [AutoSchedulingRationales.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingRationales.kt#L265-L272) | Resolved |
| **BUG-063** | PlanViewModelV2 Evaluates `anchor > dateMillis` Shifting Projected Energy Timeline and Circadian Summary by 24 Hours into Severe Fatigue on Every Date | High | Chronobiology / Temporal Logic Bug | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L231-L236) | Resolved |
| **BUG-064** | Unscheduled Tasks Silently Dropped in `PlanViewModelV2.assignTaskTime(taskId, newStartMillis)` via `tasksForDay`-Only Lookup and Date Overwrite | High | UI & Data Integrity / State Management Bug | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L835-L840) | Resolved |
| **BUG-065** | Flow Screen Hero Card Recommends Blocked, Future-Dated, Icebox, and Synthetic Part 2 Tasks via Unfiltered `candidatePool` in `FlowViewModelV2` | High | UI & Flow / Candidate Filtering Bug | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L345-L384) | Resolved |
| **BUG-066** | Background Focus Timer Expiration in `FocusService` Bypasses Streak, Activity Telemetry, and Total Focus Minutes DataStore Persistence | High | Gamification & State Persistence / Service Lifecycle | [FocusService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L380-L394) | Resolved |
| **BUG-067** | `TaskInferenceEngine.inferFromInput` Relative Hour Offset ("in X hours") Wraps Past Midnight into Yesterday via Hardcoded `todayStartMillis` | High | Temporal Logic / NLP Parsing | [TaskInferenceEngine.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/inference/TaskInferenceEngine.kt#L248-L258) | Resolved |
| **BUG-068** | Weekly Schedule Screen Stacks All Unscheduled & Deadline-Only Tasks Overlapping at 12:00 AM Midnight | High | UI / Weekly Timetable Rendering Bug | [WeeklyScheduleScreen.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/schedule/WeeklyScheduleScreen.kt#L459-L464), [WeeklyPlanUtils.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/common/WeeklyPlanUtils.kt#L40) | Resolved |
| **BUG-069** | Multi-Pathway Focus Session Completion Bypasses Native Android Calendar Cleanup Leaving Phantom Events | Medium | Integration / Native Calendar Sync Lifecycle | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L602), [FocusService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L385), [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L354) | Resolved |
| **BUG-070** | Recurring Task Completion Silently Drops All Subtasks / Microsteps (`TaskMicroStepEntity`) | High | Data Integrity / Recurrence Lifecycle | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L407-L439) | Resolved |
| **BUG-071** | Missing Cache Lookup and Storage in `GeminiAiOnlineService.synthesizeQuickNote` Enables Un-Debounced API Quota Exhaustion | High | Online AI / Quota & Performance | [GeminiAiOnlineService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/GeminiAiOnlineService.kt#L448-L500) | Resolved |
| **BUG-072** | Unfiltered Ingestion and Reverse-Chronological Ordering of All Database Tasks in "Today's Energy Stream" on Flow Screen | High | UI & Flow / Candidate Filtering & Ordering Bug | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L386-L394), [EnergyTimelineStream.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt#L66-L110) | Resolved |
| **BUG-073** | Consecutive Back-to-Back Locked Tasks Rejected and Deferred by Transition Buffer in `TaskSchedulerService` | High | Scheduling Engine / Invariant Violation & Rejection Bug | [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L537-L543) | Resolved |
| **BUG-074** | Total Tasks Completed Metric Irreversibly Inflated by Completion and Undo Loops in `FlowViewModelV2` and `PlanViewModelV2` | Medium | Gamification & State Persistence / Undo Symmetry Bug | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L670-L700), [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L855-L895) | Resolved |
| **BUG-075** | AI Scratchpad Synthesis Metadata (2-Minute Action Step, Estimated Duration, Tags) Silently Dropped on "Convert to Task" in `QuickNotesBottomSheet` | Medium | UI & AI Pipeline / Data Loss & UX Disconnect | [QuickNotesBottomSheet.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/notes/QuickNotesBottomSheet.kt#L194-L203), [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L563-L588) | Resolved |
| **BUG-076** | `TaskNaturalLanguageParser` Dead Code and Architectural Divergence in `DomainServiceModule` | Low | Code Quality / Dead Code & Architectural Divergence | [TaskNaturalLanguageParser.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskNaturalLanguageParser.kt#L15-L115), [DomainServiceModule.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/di/DomainServiceModule.kt#L37-L39) | Resolved |
| **BUG-077** | "Next DayOfWeek" Token Parsing in `TaskInferenceEngine.inferFromInput` Resolves to Today on Matching Days | Medium | Temporal Logic / NLP Parsing Edge Case | [TaskInferenceEngine.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/inference/TaskInferenceEngine.kt#L351-L357) | Resolved |
| **BUG-078** | `TaskInferenceEngine.inferFromInput` Drops Deadline Time and Erroneously Schedules Tasks for Today via `CLOCK_TIME_REGEX` | High | Temporal Logic / NLP Parsing Bug | [TaskInferenceEngine.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/inference/TaskInferenceEngine.kt#L209-L246) | Resolved |
| **BUG-079** | `FlowViewModelV2.addTaskFromNote` Uses Raw Un-Normalized Timestamp for `scheduledDate` Corrupting Intraday Schedule Offsets | Medium | Data Integrity / Temporal Normalization | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L563-L588) | Resolved |
| **BUG-080** | Pre-Wake Auto-Scheduling and Daily Plan Notifications Invert Circadian Calculation by 24 Hours into Severe Fatigue | High | Chronobiology / Temporal Logic Bug | [SchedulePreparationOrchestrator.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt#L180-L188), [NotificationWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L156-L174), [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L84-L97) | Resolved |
| **BUG-081** | `GeminiAiOnlineService.synthesizeQuickNote` Omits User Directive and Skips Response Cache Bypassing In-Memory TTL Quota Protection | Medium | Online AI / Quota Guard & Meta-Prompting | [GeminiAiOnlineService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/GeminiAiOnlineService.kt#L448-L501) | Resolved |
| **BUG-082** | Completed Tasks Retain `AVAILABILITY_BUSY` on Native Android Calendar Blocking External Schedulers | Low | Integration / Calendar Synchronization Lifecycle | [CalendarSyncDataSource.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt#L301-L325) | Resolved |
| **BUG-083** | "Edit Identity" and "Refill Goals" Hijacked to Affirmation Dialog in `IdentityScreen` | Medium | UI & State Management / User Intent Disconnect | [IdentityScreen.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/identity/IdentityScreen.kt#L65-L70), [IdentityViewModel.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/identity/IdentityViewModel.kt#L33-L56) | Resolved |
| **BUG-084** | MEQ Chronotype Quiz Omits Updating `peakEnergyStart`/`peakEnergyEnd` Leaving App Clocks & Schedules Desynchronized | High | Chronobiology / Settings & Preferences Synchronization | [MEQQuizViewModel.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/settings/MEQQuizViewModel.kt#L256-L271) | Resolved |
| **BUG-085** | Missing Back Navigation Interceptor on Android 8–12 in `HyperFocusActivity` Allows Trivial Distraction Lock Bypass | High | Android System Integration / Kiosk Security & Policy Bypass | [HyperFocusActivity.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/feature/hyperfocus/HyperFocusActivity.kt#L66-L79) | Resolved |
| **BUG-086** | Unconditional Sheet Dismissal in `HyperFocusActivationSheet` Swallows Error Messages and Silently Aborts Activation | Medium | UI / State Management & Error Feedback | [HyperFocusActivationSheet.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/HyperFocusActivationSheet.kt#L412-L421) | Resolved |
| **BUG-087** | `EndSessionGateScreen` Unreferenced Dead Code Bypassing ADHD Friction Gate via 1-Tap `AlertDialog` | High | ADHD Domain Integrity / Dead Code & Enforcement Bypass | [HyperFocusHomeActivity.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/HyperFocusHomeActivity.kt#L44-L84), [EndSessionGateScreen.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/EndSessionGateScreen.kt#L50-L195) | Resolved |
| **BUG-088** | Focus Widget Root Container Lacks Click Listener Leaving Widget Unresponsive to Taps When Empty | Low | Android Widgets & RemoteViews / Touch Responsiveness | [FocusWidgetUpdateWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L124-L152) | Resolved |
| **BUG-089** | `SmartTaskDecompositionService` Hardcodes Empty User Directive Discarding ADHD Coaching Meta-Prompting | Medium | Online AI / Meta-Prompting & Quota Invariants | [SmartTaskDecompositionService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/SmartTaskDecompositionService.kt#L41-L59), [BehavioralSupportService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/BehavioralSupportService.kt#L127-L134) | Resolved |
| **BUG-090** | Unscheduled Date-Only Tasks Default to Midnight (12:00 AM) Sync on External Android Calendar | Medium | Integration / Calendar Synchronization Lifecycle | [CalendarSyncDataSource.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt#L284-L293), [TaskTimeExtensions.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/entity/TaskTimeExtensions.kt#L37-L39) | Resolved |
| **BUG-091** | `PlanViewModelV2.addTask` Drops Selected Day Context in `SmartTaskComposerSheetV2` Routing Task to Backlog | High | UI & Workflow / Task Scheduling Loss | [PlanScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L660-L669), [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L674-L681), [SmartTaskComposerSheetV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L163) | Resolved |
| **BUG-092** | Invalid Navigation Route `focus_v2/tab_focus` Triggered When No Active Tasks Exist on `ACTION_OPEN_FOCUS` | Medium | Navigation & Runtime / State Inconsistency | [MainActivity.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L100-L103), [NeuroFlowAppV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L76-L81), [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L118) | Resolved |
| **BUG-093** | Recurring Habit Completion Freezes Stale Epoch Timestamp in `nextDeadlineTime` Corrupting Recurrence Schedule | High | Data Integrity / Recurrence & Temporal Calculation Bug | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L389-L405), [TaskTimeExtensions.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/entity/TaskTimeExtensions.kt#L48-L56) | Resolved |
| **BUG-094** | `HistoryViewModel.restoreTask` Re-Inserts Task with `TaskStatus.COMPLETED` and Bypasses Calendar Sync & DataStore Metrics | Medium | Data Integrity & State Management / Restore Failure | [HistoryViewModel.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/history/HistoryViewModel.kt#L125-L131) | Resolved |
| **BUG-095** | Unbounded Growth and Memory Leak Risk in `lastSpawnedRecurringTaskByTaskId` Map in `PlanViewModelV2` | Low | Memory Hygiene / State Leak | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L842) | Resolved |
| **BUG-096** | `UlyssesEvaluatorWorker` Permanently Strands Orphaned Active Contracts on Task Deletion, Inverts Deadline Boundary, and Omits Real-Time Completion Updates | High | Data Integrity / Contract Lifecycle & State Leak | [UlyssesEvaluatorWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/UlyssesEvaluatorWorker.kt#L24-L35), [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L216-L235), [UlyssesContractDao.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/UlyssesContractDao.kt#L25-L27) | Resolved |
| **BUG-097** | Notification ID Collision (ID 3001) Between `FocusService` Foreground Notification and `AccessibilityWatchdogWorker` Ongoing Alert | High | Android System Integration & Concurrency / Notification Collisions | [FocusService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L440), [AccessibilityWatchdogWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/AccessibilityWatchdogWorker.kt#L33-L91) | Resolved |
| **BUG-098** | Dead Code / Unwired Architecture: `NotificationBadgeService` and `NotificationBadgeManager` Run Background Listener Unobserved Without UI Badge Rendering | Low | Code Quality / Dead Code & Unwired Architecture | [NotificationBadgeService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/badge/NotificationBadgeService.kt#L1-L108), [NotificationBadgeManager.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/badge/NotificationBadgeManager.kt#L1-L28), [AndroidManifest.xml](file:///home/takian/Project/PROCUS/app/src/main/AndroidManifest.xml#L155-L163) | Resolved |
| **BUG-099** | `QuickNoteEntity` and `quick_notes` Scratchpad Table Completely Omitted from Cloud Backup and Restore in `CloudBackupManager` | Medium | Data Integrity / Cloud Backup & Restore Omission | [CloudBackupManager.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/sync/CloudBackupManager.kt#L60-L96), [FirestoreBackupRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/sync/FirestoreBackupRepository.kt#L34-L47) | Resolved |
| **BUG-100** | Stale Weekly Intention Persists Indefinitely Across All Past & Future Weeks and Bypasses ISO Week Metadata in `WeeklyScheduleViewModel` | Medium | UI & Temporal Logic / State Inconsistency & Date Boundary | [WeeklyScheduleViewModel.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/schedule/WeeklyScheduleViewModel.kt#L124-L128), [WeeklyScheduleScreen.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/schedule/WeeklyScheduleScreen.kt#L230-L236) | Resolved |
| **BUG-101** | Missing `lastActiveDate` Update on Manual Focus Time Logging in `LogTimeViewModel`, `FlowViewModelV2`, and `PlanViewModelV2` Causes Streak Check Erasure | Medium | Gamification & State Management / Telemetry Omission | [LogTimeViewModel.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/logtime/LogTimeViewModel.kt#L55-L57), [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L894-L896), [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L966-L968), [NotificationWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L260-L275) | Resolved |
| **BUG-102** | Missing Recurrence Interval Days Input Field and Defaulting to 0 in `SmartTaskComposerSheetV2` Silently Forces Custom Recurrence to Daily | Medium | UI & Recurrence Lifecycle / UX & State Bug | [SmartTaskComposerSheetV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L236-L281), [SmartTaskComposerSheetV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L1200-L1217), [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L43) | Resolved |
| **BUG-103** | Orphaned `TaskMicroStepEntity` Records Persist in SQLite on Task Deletion Due to Missing `deleteByTaskId` in `TaskRepository.delete` and Inactive Foreign Keys | Medium | Data Integrity / SQLite Foreign Keys & Orphan Cleanup | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L216-L260), [AppModule.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/di/AppModule.kt#L53-L86), [TaskMicroStepDao.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/TaskMicroStepDao.kt#L21-L22) | Resolved |
| **BUG-104** | 23:55 Minute Increment Overflow Inverts to 23:00 Decreasing Logged Time by 55 Minutes in `ManualTimeLogSheet` | Low | UI / Input Validation Edge Case | [ManualTimeLogSheet.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/common/ManualTimeLogSheet.kt#L107-L111) | Resolved |
| **BUG-105** | New Year Calendar Boundary Fails "Yesterday" Date Check on January 1 in `NotesScreen.kt` | Low | UI & Temporal Logic / Date Boundary | [NotesScreen.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/notes/NotesScreen.kt#L241-L245) | Resolved |
| **BUG-214** | All-Day Events Ingested as 24-Hour Busy Windows in `CalendarSyncDataSource.queryInstances` Blocking Daily Auto-Scheduling & Primary Calendar Coupling | High | Integration / Calendar Synchronization | [CalendarSyncDataSource.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt#L80-L135) | Resolved |
| **BUG-215** | Starter Tags Re-injected Unconditionally on Every Read and Save in `UserPreferencesDataStore.kt` Preventing Tag Deletion | High | Core Data Integrity / Tag Catalog Lifecycle | [UserPreferencesDataStore.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/UserPreferencesDataStore.kt#L374) | Resolved |
| **BUG-216** | Hash/At Prefix Omission in Tag Normalization Causes Inconsistent Tag Names, `##tag` Rendering, and Profile Lookup Failures | Medium | UI & Data Integrity / Tag Normalization | [TaskTagSchedulingProfile.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/TaskTagSchedulingProfile.kt#L122) | Resolved |
| **BUG-217** | Hardcoded Tag Filter Bar and Missing Catalog Invalidation on `PlanScreenV2` via `PlanViewModelV2.kt` | Medium | UI & State Management / Tag Filter Desynchronization | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L99) | Resolved |
| **BUG-218** | Naive Substring Tag Matching and Unbounded Word Title Ingestion Distorts Filtered Tasks | Medium | UI & Task Filtering / Substring False Positives | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L302) | Resolved |
| **BUG-219** | `play(SoundscapeType.NONE)` Truncates Volume Before Fade-Out Producing Speaker Pop | High | Audio DSP / Playback Lifecycle & Volume Mismatch | [NeuralSoundscapePlayer.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapePlayer.kt#L187) | Resolved |
| **BUG-220** | `AudioTrack.flush()` Immediately After Raised-Cosine Fade-Out Purges Hardware Ring Buffer Producing Audible Click | Medium | Audio DSP / AudioTrack Flush Discontinuity | [NeuralSoundscapePlayer.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapePlayer.kt#L236) | Resolved |
| **BUG-221** | `FocusViewModelV2.onCleared()` Omits Native `AudioTrack` Release Causing Resource Exhaustion on Navigation | High | Native Resource Management / AudioTrack Leak | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L986) | Resolved |
| **BUG-222** | Streaming Soundscape Preset Switching Without Chunk Crossfade Causes Waveform Discontinuity (>25,000 PCM Units) | Medium | Audio DSP / Preset Crossfading | [NeuralSoundscapePlayer.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapePlayer.kt#L280) | Resolved |
| **BUG-223** | Timer Mode & Duration Lost Across Session Recovery in `FocusViewModelV2.kt` Causing Premature Sprint Expiration | High | State Recovery / Focus Timer Mode Persistence | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L140-L175) | Resolved |
| **BUG-224** | `FocusService.kt` Drops `accumulatedPauseMillis` on Service Reconnection Causing Notification Time Drift | High | Background Service / Pause State Recovery | [FocusService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L74-L95) | Resolved |
| **BUG-225** | `FocusViewModelV2.keepGoing()` Reuses Closed Session Primary Key Causing SQLite Replace Overwrite & History Data Loss | Critical | Data Integrity / Room Session Key Collision | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L782) | Resolved |
| **BUG-226** | `FocusViewModelV2.kt` Auto-Tracker Fires on Already-Completed Tasks Creating Orphaned Time Sessions | Medium | UI & Background Services / Auto-Tracker Guard | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L293) | Resolved |
| **BUG-227** | `TimeSessionDao.getOpenSessionForTask` Missing `ORDER BY startedAt DESC` Returning Arbitrary Stale Session | Medium | Room Database / Query Determinism | [TimeSessionDao.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/TimeSessionDao.kt#L24) | Resolved |
| **BUG-228** | Protected Time Slot End-Time Integer Overflow Past Midnight (24:xx / 25:xx Clock Display) | Medium | UI & Boundary Handling / Time Formatting | [PlanScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L1327) | Resolved |
| **BUG-229** | Dynamic Circadian Peak Profile Ignored in `PlanScreenV2` (Hardcoded Fallback to Static Preference) | Low | UI & State Synchronization / Circadian Alignment | [PlanScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L853) | Resolved |
| **BUG-230** | Stale Task Snapshot in `rememberSwipeToDismissBoxState` Recomposition Closure Overwrites Edits on Dismiss | Medium | Runtime / State Synchronization | [PlanScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L971) | Resolved |
| **BUG-231** | `task.resolveScheduledEpochMillis()` in `PlanScreenV2` Energy Badge Defaults to Current Day Instead of `selectedDateMillis` | Medium | UI & Logic / Multi-Day Timeline Alignment | [PlanScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L1149) | Resolved |
| **BUG-232** | Ineffective Task Inference Calibration in `SmartTaskComposerSheetV2` (Always-Empty Completed Tasks Passed From `PlanScreenV2`) | Medium | Core Logic / Task Duration Calibration | [PlanScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L671) | Resolved |
| **BUG-233** | Stale Task Snapshot in `rememberSwipeToDismissBoxState` Recomposition Closure Overwrites Edits on Dismiss in `EnergyTimelineStream.kt` | Medium | Runtime / State Synchronization | [EnergyTimelineStream.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt#L244-L265) | Resolved |
| **BUG-234** | Future / Icebox Task Leakage in `FlowSpotlightHeroCard` When Today's Tasks Are Completed or Filtered | High | UI & State Synchronization / Task Filtering Leakage | [FlowScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt#L242) | Resolved |
| **BUG-235** | Untimed Tasks Scheduled for Today Sorted to Midnight (00:00) Ahead of Morning Timed Tasks in `FlowViewModelV2.todayActiveTasks` | Medium | UI & Task Scheduling / Chronological Sorting | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L414) | Resolved |
| **BUG-236** | "Step 4 of 3" Display and Micro-Step Overflow in `FlowSpotlightHeroCard` When All Micro-Steps Completed | Low | UI & Boundary Handling / Micro-Step Clamping | [FlowHubUpgradesV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowHubUpgradesV2.kt#L234) | Resolved |
| **BUG-237** | `AutonomyNudgeEngine` Omitted in `FlowViewModelV2.shiftTaskToPeak()` and Missing Dynamic Circadian Peak / Anchor Support | Medium | Core Logic & Background Services / Notification & Peak Calibration | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L687) | Resolved |
| **BUG-238** | Undo Snackbar Invalidation Race in `FlowScreenV2.kt` Clobbering Restoration Confirmation Message | Low | UI & State Synchronization / Action Message Race | [FlowScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt#L80) | Resolved |
| **BUG-239** | `FocusViewModelV2.onCleared()` Destroys Paused Background Session & Foreground Service on Navigation | High | Lifecycle & Background Service / Paused State Destruction | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L1120) | Resolved |
| **BUG-240** | Stopwatch Mode Elapsed Time Frozen at `00:00` in `FocusService.activeSession` and `MiniFocusSessionBar` | High | UI & Service Synchronization / Stopwatch Mode Time Drift | [FocusService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L99) | Resolved |
| **BUG-241** | Zombie WOOP Bottom Sheet Loop Re-triggering on State Recomposition in `FocusScreenV2.kt` | Medium | UI & State Synchronization / Bottom Sheet Navigation Loop | [FocusScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt#L72) | Resolved |
| **BUG-242** | Completed Micro-Steps Resurrected as Active Step 1 in `ZenMicroStepSpotlightCard` | Medium | UI & Boundary Handling / Completed Step Resurrection | [FocusScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt#L262) | Resolved |
| **BUG-243** | Focus Mode Selector Chips Overflow Screen Width on Standard Android Devices | Medium | UI Layout & Responsiveness / Horizontal Chip Overflow | [FocusScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt#L158) | Resolved |
| **BUG-244** | Completing Untracked Task Fabricates Focus Minutes & Session Telemetry and Distorts Paused Durations | Medium | Data Integrity & Telemetry / False Focus Metrics | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L765) | Resolved |
| **BUG-245** | Physiological Sigh Coroutine Cancelled by Atomic Stage Race in `InteractivePhysiologicalSighSheet` | Medium | Concurrency & Haptics / Coroutine Recomposition Race | [FocusAndRegulationUpgradesV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusAndRegulationUpgradesV2.kt#L176) | Resolved |
| **BUG-246** | External & Cross-Day Prerequisites Stripped by `DependencyGraphValidator.sanitizeDag` in Multi-Task Schedules | Critical | Core Logic / Dependency Graph Sanitization | [DependencyGraphValidator.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/DependencyGraphValidator.kt#L83-L89) | Resolved |
| **BUG-247** | Overdue Tasks Permanently Blocked from Scheduling by Reverse-DAG Deadline Ceiling | High | Core Logic & Scheduling / Reverse-DAG Deadline Cutoff | [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L790-L845) | Resolved |
| **BUG-248** | Calendar Slot Budget Overflow in `AutoSchedulingEngineCoordinator.planSchedule` | Medium | Core Logic / Cognitive Budget & Slot Accounting | [AutoSchedulingRationales.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingRationales.kt#L78) | Resolved |
| **BUG-249** | Future-Day Tasks Dropped and Desynchronized in `ConflictResolver.analyzeScheduleConflicts` Due to Default Current-Day Epoch Resolution | Medium | UI & Core Logic / Multi-Day Conflict Resolution | [AutoSchedulingRationales.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingRationales.kt#L153) | Resolved |
| **BUG-250** | Atomic Ultradian Split Verifier Omits `referenceNowMillis` Causing Anchor Distortion and Drift | Medium | Core Logic / Pharmacokinetic & Urgency Pacing | [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L960) | Resolved |
| **BUG-251** | Natural Language Scheduling Date Shadowed and Overridden by `initialDate` in `SmartTaskComposerSheetV2` | High | UI & NLP Inference / Schedule Date Shadowing | [SmartTaskComposerSheetV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L136-L175) | Resolved |
| **BUG-252** | Natural Language Directives Ignored in Edit Mode Due to Pre-Populated Override Variables | High | UI & NLP Inference / Edit Mode Directive Paralysis | [SmartTaskComposerSheetV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L120-L175) | Resolved |
| **BUG-253** | Selecting Due Time in TimePicker Destroys Inferred Due Date by Re-Defaulting to Today | Medium | UI & Boundary Handling / TimePicker Date Clobbering | [SmartTaskComposerSheetV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L1530-L1545) | Resolved |
| **BUG-254** | User Note Title Clobbered by AI Task Title and Unhandled Coroutine Exception on Network Failure in `QuickNotesBottomSheet` | Medium | UI & Data Integrity / Note Title Overwrite & Crash Resilience | [QuickNotesBottomSheet.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/notes/QuickNotesBottomSheet.kt#L48-L225) | Resolved |
| **BUG-255** | Duration Chip Reset Discontinuity on Non-Preset Durations in `SmartTaskComposerSheetV2` | Medium | UI & State Synchronization / Duration Chip Cycling | [SmartTaskComposerSheetV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L550-L555) | Resolved |
| **BUG-256** | `DeadlineEscalationWorker` Never Updates Task to `DO_FIRST` Causing Continuous 4-Hour Notification Spam | High | Background Services / Notification & Escalation State | [NotificationWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L400) | Resolved |
| **BUG-257** | Future-Scheduled Tasks Have Autonomy Nudges Permanently Dropped by Premature Delay Expiration in `AutonomyNudgeEngine` & `AutonomyNudgeWorker` | High | Background Services & UI / Autonomy Nudge Lifecycle | [AutonomyNudgeEngine.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/engine/AutonomyNudgeEngine.kt#L16), [AutonomyNudgeWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/AutonomyNudgeWorker.kt#L48) | Resolved |
| **BUG-258** | Completed Part 2 Continuation Tasks Deleted or Re-scheduled by `ScheduleAutoTasksWorker` | High | Core Data Integrity / Split Task History Retention | [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L332), [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L396) | Resolved |
| **BUG-259** | Iceboxed Tasks (`Quadrant.ELIMINATE`) Treated as Fixed Busy Windows Blocking Schedule Slots in `ScheduleAutoTasksWorker` | Medium | Core Logic & Scheduling / Phantom Busy Windows | [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L237) | Resolved |
| **BUG-260** | `TaskReminderWorker` Uses Stale Scheduling Title Snapshot Instead of Fresh Task Entity Title | Low | UI & Background Services / Notification Title Desynchronization | [NotificationWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L485) | Resolved |
| **BUG-261** | `CompleteTaskWidgetAction.kt` Unconditionally Stops `FocusService` Killing Active Focus Sessions on Unrelated Tasks | High | Background Services / Service Isolation & Focus Tracking | [CompleteTaskWidgetAction.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/widget/CompleteTaskWidgetAction.kt#L56) | Resolved |
| **BUG-262** | `FocusWidgetProvider` Start Button Remains Visible on Empty State and Fires Stale Pending Intent or Becomes Dead Click | Medium | UI & State Synchronization / RemoteViews Empty State | [FocusWidgetUpdateWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L151-L171) | Resolved |
| **BUG-263** | `CompleteTaskReceiver` Uncaught Coroutine Exception Crashes Process & Omits Task ID Intent Extra Fallbacks | Medium | Runtime / Crash Resilience & Intent Extra Normalization | [CompleteTaskReceiver.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/widget/CompleteTaskReceiver.kt#L28-L40), [CompleteTaskWidgetAction.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/widget/CompleteTaskWidgetAction.kt#L58) | Resolved |
| **BUG-264** | Split Continuation Tasks (`_part2`) Permanently Excluded from Widgets Even When Part 1 is Completed | High | UI & Data Integrity / Split Task Continuation Lifecycle | [FocusWidgetUpdateWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L65) | Resolved |
| **BUG-265** | Blocked Tasks with Unmet Active Prerequisites Recommended as Top Action on Home Screen Widgets | High | Core Logic & Scheduling / Dependency Graph Enforcement | [FocusWidgetUpdateWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L61-L68) | Resolved |
| **BUG-266** | `CancellationException` Swallowed in `FocusWidgetUpdateWorker.doWork()` Breaking Coroutine Cancellation Contract | Medium | Concurrency & WorkManager / Structured Concurrency | [FocusWidgetUpdateWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L238-L241) | Resolved |
| **BUG-267** | Double-Tap or Redundant `markTaskCompleted` on Widgets Triggers Duplicate Recurring Instances & Habit Streak Inflation | High | Core Data Integrity & Recurrence / Completion Idempotency | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L364-L375) | Resolved |
| **BUG-268** | `MIGRATION_1_2` Crashes with Duplicate Column Error on Databases Initialized on Schema v1 | High | Database Migrations & Schema Drift | [NeuroFlowDatabase.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/NeuroFlowDatabase.kt#L40-L55) | Resolved |
| **BUG-269** | Unhandled SQLite Foreign Key Constraint Crash in `SessionRepository.syncFocusSession` on Deleted or Untracked Tasks | High | Database Concurrency & Foreign Key Integrity | [SessionRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/SessionRepository.kt#L83-L115) | Resolved |
| **BUG-270** | Missing `@TypeConverter` for `HyperFocusState` in `Converters.kt` Causing Crash on Corrupted or Unknown States | Medium | Room Database / Enum Deserialization | [Converters.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/Converters.kt#L93-L101), [HyperFocusSessionEntity.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/feature/hyperfocus/data/HyperFocusSessionEntity.kt#L12) | Resolved |
| **BUG-271** | SQLite Statement Leak and Duration Calculation Mismatch in `MIGRATION_21_22` Micro-Step & Session Extraction | Medium | Database Migrations & Resource Management | [NeuroFlowDatabase.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/NeuroFlowDatabase.kt#L400-L450) | Resolved |
| **BUG-272** | Desynchronized Micro-Steps in Scheduling Orchestrators (`microStepCounts` Returns `(0, 0)` for Tasks Created via `ifThenPlan`) | Medium | Data Integrity & Scheduler Concurrency | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L95-L117) | Resolved |
| **BUG-273** | Direct Intent Hijacked and App Viewport Overwritten by `NewChapterCard` / `TopGoalsRefillCard` Upon Intent Consumption | High | UI & Navigation / Lifecycle State Synchronization | [MainActivity.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L205-L380) | Resolved |
| **BUG-274** | Notification & Widget Launch Intents Silently Ignored When Activity Restored from `savedInstanceState` | High | Android Lifecycle / Intent Dispatch & Process Restoration | [MainActivity.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L170-L185) | Resolved |
| **BUG-275** | Duplicate `FlowScreenV2` Pushed to Backstack on App Launch via `initialTaskId == "tab_focus"` and Unbounded Task Stack Accumulation | Medium | Navigation / Backstack Integrity & Route Duplication | [NeuroFlowAppV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L76-L175) | Resolved |
| **BUG-276** | Redundant Dual `FlowViewModelV2` Allocation and Desynchronized Ambient Phase Telemetry | Medium | Architecture & Memory / ViewModel Lifecycle Desynchronization | [NeuroFlowAppV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L124-L230) | Resolved |
| **BUG-277** | Material You Dynamic Color Preference Wiped Out by Inner Theme Override in `NeuroFlowAppV2.kt` | Low | UI & Theming / Theme Configuration Overwrite | [NeuroFlowAppV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L67-L123) | Resolved |
| **BUG-278** | Hardcoded Backstack Purge in `FocusScreenV2.onNavigateToNextTask` Destroys Prior Navigation History | Low | Navigation / Backstack Integrity & User Flow | [NeuroFlowAppV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L415-L445) | Resolved |
| **BUG-279** | Completed Part 2 Continuation Tasks Deleted or Overwritten by `PlanViewModelV2.autoScheduleDay()` | High | Core Data Integrity / Split Task History Retention | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L581-L610) | Resolved |
| **BUG-280** | Missing `effectivePeakStart`/`effectivePeakEnd` & `peakEnergyStart`/`peakEnergyEnd` in `PlanViewModelV2` Preference Flow Suppression | Medium | UI & State Synchronization / Circadian Peak Desynchronization | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L212-L229) | Resolved |
| **BUG-281** | Dynamic Circadian Peak & Anchor Ignored and Autonomy Nudge Omitted in `PlanViewModelV2.shiftTaskToPeak()` and `assignTaskTime()` | High | Core Logic & Background Services / Notification & Peak Calibration | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L652-L950) | Resolved |
| **BUG-282** | Immediate Dismissal Race of Undo Snackbar on Task Completion in `PlanScreenV2.kt` | Medium | UI & State Synchronization / Action Message Race | [PlanScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L86-L124) | Resolved |
| **BUG-283** | Cognitive Budget Meter Miscalculation in `PlanScreenV2.kt` Using Raw `estimatedDurationMinutes` Instead of `TaskBlockPlan.plannedMinutes` | Medium | Core Logic & UI / Cognitive Load Accounting | [PlanScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L881) | Resolved |
| **BUG-284** | Missing Circadian Peak Fields in `FlowViewModelV2` Preference Flow Suppression | Medium | UI & State Synchronization / Circadian Peak Desynchronization | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L150-L165) | Resolved |
| **BUG-285** | Unconditional Exclusion of Part 2 Continuation Tasks from Actionable and Recommended Task Pool in `FlowViewModelV2.kt` | High | Core Logic & Task Recommendation / Split Task Lifecycle | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L363) | Resolved |
| **BUG-286** | Missing Autonomy Nudge Rescheduling on `updateTask()`, `undoCompleteTask()`, and `applyEmergencyReset()`, and Faulty Cancellation on `undoDeferTask()` in `FlowViewModelV2.kt` | High | Background Services & Notifications / Autonomy Nudge Lifecycle | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L774-L920) | Resolved |
| **BUG-287** | Bypassing `TaskBlockPlan.plannedMinutes` in `EnergyTimelineStream.kt` & `FlowScreenV2.kt` Causes Inflated Block Duration Display and Premature Ultradian Rest Buffer | Medium | Core Logic & UI / Cognitive Load Accounting | [EnergyTimelineStream.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt#L103), [FlowScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt#L269) | Resolved |
| **BUG-288** | Missing `habitDate` Updates in `applyEmergencyReset()` & `freshStartRollover()` Leaving Recurring Habits Overdue, and Stale Null Fallback in `EnergyTimelineStream.kt` | High | Core Data Integrity / Recurring Habit Emergency Reset | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L910), [EnergyTimelineStream.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt#L441) | Resolved |
| **BUG-289** | Premature Completion Status in `FocusViewModelV2` and `FocusService` Causing `TaskRepository.completeAndRecur` Silent Abort and Dropped Completion Lifecycle | High | Core Logic / Task Completion Lifecycle | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L644-L670), [FocusService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L236-L255) | Resolved |
| **BUG-290** | Completion Celebration Dialog Modal Trap on Already Completed Tasks in `FocusScreenV2.kt` & Duplicate Completion Inflation in `FocusViewModelV2.kt` | High | UI & State Management / Completion Modal Trap | [FocusScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt#L520), [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L630-L640) | Resolved |
| **BUG-291** | Autonomy Nudge Orphaned on `FocusService` Direct Completion and Lost on Non-Started Screen Exit in `FocusViewModelV2.kt` | High | Background Services & Notifications / Autonomy Nudge Synchronization | [FocusService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L220-L240), [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L980-L1010) | Resolved |
| **BUG-292** | Frozen `00:00` Timer Display on Decompression Bypass and Stale Pause Count in `FocusViewModelV2.kt` | Medium | UI & Timer State / Decompression Zone Transitions | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L360-L470) | Resolved |
| **BUG-293** | Bypassing `TaskBlockPlan.plannedMinutes` Distorts Duration Meters and Estimation Metrics (MAPE/sMAPE) in `FocusScreenV2.kt`, `FocusViewModelV2.kt`, and `FocusService.kt` | Medium | Core Logic & UI / Cognitive Accounting | [FocusScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt#L295-L350), [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L640-L660), [FocusService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L240-L260) | Resolved |
| **BUG-294** | `attemptSplitPlacement` Unbounded Phase 1 Ceiling Traps Ultradian Continuation Part 2 and Falsely Defers Multi-Hour Tasks | High | Core Logic & Scheduling / Ultradian Split Verifier | [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L936-L965) | Resolved |
| **BUG-295** | `SchedulePreparationOrchestrator` Future-Day Scheduling Desynchronization for Dateless Timed Tasks Causing False Lock Conflict Deferrals | High | Core Logic & Orchestration / Multi-Day Epoch Anchoring | [SchedulePreparationOrchestrator.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt#L108-L155) | Resolved |
| **BUG-296** | `PlacementStability.verdict` Ignores Parameterized `nowMillis` Defaulting to Real-Time System Clock and Skewing Frozen/Rescue Verdicts | High | Core Logic & Stability / Parameterized Clock Anchoring | [PlacementStability.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/PlacementStability.kt#L48-L68) | Resolved |
| **BUG-297** | Cognitive Load Meter & Slot Accounting Bypasses `TaskBlockPlan.plannedMinutes` in `AutoSchedulingEngineCoordinator.planSchedule` | Medium | Core Logic / Cognitive Load Accounting | [AutoSchedulingRationales.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingRationales.kt#L68) | Resolved |
| **BUG-298** | `subtractInterval` Zero-Width / Inverted Block Artificial Interval Fragmentation in `TaskSchedulerService.kt` | Medium | Core Logic & Calendar / Free Interval Accounting | [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L1368) | Resolved |
| **BUG-299** | Untimed Tasks in Edit Mode Force `effectiveDuration = 0` Bypassing Task Archetype and Inferred Baseline Duration in `SmartTaskComposerSheetV2.kt` | High | UI & NLP Inference / Task Duration Collapse | [SmartTaskComposerSheetV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L161-L162) | Resolved |
| **BUG-300** | Picking Due Time or Scheduled Time Without Explicit Date Clobbers Future `effectiveScheduledDate` / `effectiveDeadlineDate` to Today in `SmartTaskComposerSheetV2.kt` | High | UI & Boundary Handling / TimePicker Date Clobbering | [SmartTaskComposerSheetV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L276-L285) | Resolved |
| **BUG-301** | `CalendarSyncDataSource.upsertTaskEvent` Defaults to System Clock Midnight For Dateless Timed Tasks and Bypasses `TaskBlockPlan.plannedMinutes` | High | Integration / Calendar Synchronization & Temporal Anchoring | [CalendarSyncDataSource.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt#L302-L315) | Resolved |
| **BUG-302** | Newly Composed and Updated Task Tags in `FlowViewModelV2` and `PlanViewModelV2` Dropped from User Preferences `tagCatalog` Until Process Restart | Medium | Core Data Integrity / Tag Catalog Synchronization | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L566-L785), [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L652-L786) | Resolved |
| **BUG-303** | `FocusService` Sprint Completion Notification Cancelled Immediately by `onDestroy()` Race Condition and Omitted Home Screen Widget Refresh on Complete | High | Background Services / Notification & Service Lifecycle Race | [FocusService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L212-L266) | Resolved |
| **BUG-304** | Future Deadline Date (`task.deadlineDate > dayEnd`) Falsely Suppresses Autonomy Nudges for Tasks Scheduled for Today in `AutonomyNudgeWorker.kt` | High | Background Services & Notifications / Autonomy Nudge False Suppression | [AutonomyNudgeWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/AutonomyNudgeWorker.kt#L60-L63) | Resolved |
| **BUG-305** | `AutonomyNudgeEngine` Omitted from `TaskRepository` Write Paths Leading to Missing Autonomy Nudges for Background Auto-Scheduled & Recurring Tasks | High | Core Data Integrity & Background Services / Autonomy Nudge Lifecycle Desynchronization | [AutonomyNudgeEngine.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/engine/AutonomyNudgeEngine.kt#L16), [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L167-L184) | Resolved |
| **BUG-306** | `DailyPlanWorker` Unconditionally Bans Split Part 2 Tasks and Bypasses `TaskBlockPlan.plannedMinutes` Across Workers & Widgets | Medium | Background Services & UI / Continuation Task Exclusion & Cognitive Budget Meter | [NotificationWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L177-L228), [FocusWidgetUpdateWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L104-L187), [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L378) | Resolved |
| **BUG-307** | Non-Nullable String Parameter in Room `@TypeConverter` Methods Crashes on SQLite NULL Values with Uncaught `NullPointerException` | High | Room Database / Enum Deserialization & Crash Resilience | [Converters.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/Converters.kt#L21-L100) | Resolved |
| **BUG-308** | `TaskRepository.delete` Leaves Orphaned Subtasks, Retains Deadlocked Part 2 Prerequisites, and Leaks Zombie Open Time Sessions | High | Room Database & Concurrency / Cascade Deletion & Dependency Deadlock | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L262-L310), [TaskDao.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/TaskDao.kt#L73-L77), [TimeSessionDao.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/TimeSessionDao.kt#L45-L49) | Resolved |
| **BUG-309** | `splitTaskIntoSessions` Wipes Out Prerequisites, Schedule Dates, and External Dependencies on Part 1 and Falsely Unlocks Downstream Tasks Early | High | Core Logic & Scheduling / Task Splitting Integrity & Dependency Preservation | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L330-L390) | Resolved |
| **BUG-310** | Non-Idempotent Legacy Micro-Step Extraction in `MIGRATION_21_22` Inserts Duplicate Steps on Migration Re-runs | Medium | Database Migrations & Data Integrity / Micro-Step Deduplication | [NeuroFlowDatabase.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/NeuroFlowDatabase.kt#L430-L450) | Resolved |
| **BUG-311** | Intent Extras Retained Across Configuration Changes Cause Repeated Intent Re-execution, Closed Task Reopening, and Volatile State Reset in `MainActivity.kt` | High | Android Lifecycle / Intent Dispatch & Configuration Change State Synchronization | [MainActivity.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L72-L184) | Resolved |
| **BUG-312** | Unencoded Task IDs, Missing `launchSingleTop`, and Blank Target Vulnerability in Navigation Routes in `NeuroFlowAppV2.kt` | Medium | Navigation / Route Encoding & Backstack Duplication | [NeuroFlowAppV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L80-L87) | Resolved |
| **BUG-313** | Dynamic Circadian Peak Ignored in `PlanScreenV2.kt` Timeline Header & Peak Badge Desynchronization with `shiftTaskToPeak` | Medium | UI & State Synchronization / Circadian Peak Calibration | [PlanScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L855) | Resolved |
| **BUG-314** | `TaskBlockPlan.plannedMinutes` Bypassed in `PlanScreenV2.kt` Timeline & Matrix Views Causing Inflated Display and Premature Ultradian Recovery Rest Buffer | Medium | UI & Core Logic / Cognitive Load Accounting | [PlanScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L824) | Resolved |
| **BUG-315** | `PlanViewModelV2.autoScheduleDay` Treats Unscheduled Backlog Tasks & Icebox Tasks as Fixed Busy Windows Blocking Daily Auto-Scheduling | High | Core Logic & Scheduling / Phantom Busy Windows | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L518) | Resolved |
| **BUG-316** | `PlanViewModelV2.uncompleteTask` and `moveTaskToQuadrant` Omit Autonomy Nudge Rescheduling and Calendar Sync Synchronization | High | Background Services & Notifications / Autonomy Nudge Synchronization | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L689) | Resolved |
| **BUG-317** | `PlanViewModelV2` Tag Filtering Ignores `task.contextTag` Hiding Matching Tasks for Starter Tags (#deepwork, #offline) | Medium | UI & Task Filtering / Context Tag Desynchronization | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L309) | Resolved |
| **BUG-318** | Dual-Representation Absolute Epoch Timestamps (`scheduledTime >= ONE_DAY_MILLIS`) Dropped from `tasksForDay` in `PlanViewModelV2.kt` | High | UI & Data Integrity / Temporal Epoch Representation | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L348) | Resolved |
| **BUG-319** | Absolute Epoch Timestamps (`scheduledTime >= ONE_DAY_MILLIS`) Ignored in `FlowViewModelV2.kt` Leaking Future-Scheduled Tasks into Today and Dropping Today's Timed Tasks | High | UI & Data Integrity / Temporal Epoch Representation | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L363) | Resolved |
| **BUG-320** | Tag Filtering and Dynamic Tag Aggregation Ignore `task.contextTag` in `FlowViewModelV2.kt` | Medium | UI & Task Filtering / Context Tag Desynchronization | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L351) | Resolved |
| **BUG-321** | Manual Peak Override and Absolute Timestamp Epoch Anchoring Ignored in `FlowViewModelV2.shiftTaskToPeak()` | High | Core Logic & Scheduling / Dynamic Peak Calibration & Multi-Day Anchoring | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L722) | Resolved |
| **BUG-322** | Split Continuation Indicator in `EnergyTimelineStream.kt` Omits `_part` ID Suffix and Evening Shutdown Null `completedAt` Fallback | Low | UI & Boundary Handling / Split Task Badge & Completion Resiliency | [EnergyTimelineStream.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt#L408), [FlowScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt#L343) | Resolved |
| **BUG-323** | Foreground Service Notification ID Collision in `FocusService.updateNotification()` Spawns Duplicate Notification and Leaves Primary Notification Frozen | High | Background Services & Notifications / Notification Channel & ID Collision | [FocusService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L276) | Resolved |
| **BUG-324** | External Task Completion Ignored When Sprint Completed, Causing Double Completion and Metric Inflation in `FocusViewModelV2.observeActiveSession()` | High | UI & State Synchronization / External Service Synchronization | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L328) | Resolved |
| **BUG-325** | Dual-Representation Timestamp Epoch Leak & Blocked Prerequisite/Continuation Tasks in `FocusViewModelV2` Next Task Queue Ranking | High | Core Logic & Task Queue / Temporal Epoch & Prerequisite Validation | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L134) | Resolved |
| **BUG-326** | Decompression Zone Exit Leaves Stale `startedAtMillis` Fabricating Phantom Focus Minutes on Subsequent Direct Completion in `FocusViewModelV2.kt` | Medium | UI & Timer State / Decompression Zone Transitions | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L964-L990) | Resolved |
| **BUG-327** | Micro-Step Deletion on Empty Heuristic Fallback in `FocusViewModelV2.autoBreakDownTask()` & `ZenMicroStepSpotlightCard` 0m Badge | Low | UI & Data Integrity / Micro-Step Preservation & Duration Floor | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L1002), [FocusScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt#L301) | Resolved |
| **BUG-328** | Continuation Part 2 Duration Wiped and Title Clobbered in `SchedulePreparationOrchestrator.kt` | High | Core Logic & Scheduling / Split Continuation Duration Preservation | [SchedulePreparationOrchestrator.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt#L62-L85) | Resolved |
| **BUG-329** | `TaskSchedulerService.kt` Omits `_part2` Suffix and `parentTaskId` in Ultradian Recovery Buffer and Completion End Time Tracking | High | Core Logic & Scheduling / Ultradian Recovery Buffer & Split Tracking | [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L206-L250) | Resolved |
| **BUG-330** | Downstream Tasks Scheduled Concurrently or Ahead of Split Continuation Part 2 Due to Missing Topological Chaining in `DependencyGraphValidator.kt` | High | Core Logic & Scheduling / Dependency Graph Sanitization & Topological Chaining | [DependencyGraphValidator.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/DependencyGraphValidator.kt#L80-L115) | Resolved |
| **BUG-331** | Intraday Deadlines Without Explicit `deadlineDate` Ignored by `resolveDeadlineEpochMillis()` in `TaskTimeExtensions.kt` | Medium | Core Data Integrity / Deadline Epoch Resolution | [TaskTimeExtensions.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/entity/TaskTimeExtensions.kt#L53-L73) | Resolved |
| **BUG-332** | Downstream Child Unlocking Infinite Loop and Inconsistent Suffix Deferral Sweeps in `TaskSchedulerService.kt` | High | Core Logic & Concurrency / Ready Queue Unlocking & Deferral Cleanup | [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L530-L580) | Resolved |
| **BUG-333** | Cleared Scheduled Date & Deadline Date Resurrected on Timed Tasks in `SmartTaskComposerSheetV2.kt` | High | UI & Boundary Handling / Date Clearing Clobbering | [SmartTaskComposerSheetV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L280-L290) | Resolved |
| **BUG-334** | `contextTag` Dropped in Create Mode and Stale Tag Retained on Removal in Edit Mode in `SmartTaskComposerSheetV2.kt` | High | UI & Data Integrity / Context Tag Synchronization | [SmartTaskComposerSheetV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L127-L350) | Resolved |
| **BUG-335** | User Note Title Overwritten by AI Extracted Title on Note-to-Task Conversion in `QuickNotesBottomSheet.kt` | Medium | UI & State Synchronization / Note Title Preservation | [QuickNotesBottomSheet.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/notes/QuickNotesBottomSheet.kt#L213-L325) | Resolved |
| **BUG-336** | Calendar Event Duplication After Cloud Sync Wipes Android-Proprietary Custom Columns in `CalendarSyncDataSource.kt` | High | Integration / Calendar Synchronization & Cloud Sync Resilience | [CalendarSyncDataSource.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt#L260-L360) | Resolved |
| **BUG-337** | Comma-Separated Tag Strings Ingested as Single Entries in `TaskTagViewModel.kt` and `UserPreferencesDataStore.kt` | Medium | Core Data Integrity / Tag Normalization & Catalog Ingestion | [TaskTagViewModel.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/common/TaskTagViewModel.kt#L48), [UserPreferencesDataStore.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/UserPreferencesDataStore.kt#L891) | Resolved |
| **BUG-338** | Dateless Timed Tasks with Absolute Epoch Timestamp Return Null Effective Scheduled Date Leaking Future Tasks into Widgets & Schedules | High | Core Data Integrity / Temporal Epoch Representation | [TaskTimeExtensions.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/entity/TaskTimeExtensions.kt#L19-L38) | Resolved |
| **BUG-339** | `FocusService` Notification Actions Drop Task Identity Extras and Missing `startedAtMillis` Guard Fabricates 50 Years Focus Time | High | Background Services / Notification Action Extras & State Restoration | [FocusService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L130-L330) | Resolved |
| **BUG-340** | Sprint Completion Alerts Posted to Low-Importance Silent Live Channel Suppressing Notification Sound and Vibration | Medium | Background Services & Notifications / Notification Channel Configuration | [FocusService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L30-L50) | Resolved |
| **BUG-341** | `CompleteTaskWidgetAction` Drops Session Focus Minutes and Telemetry Across Direct Widget Task Completions | High | UI & Data Integrity / Widget Session Tracking & Telemetry | [CompleteTaskWidgetAction.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/widget/CompleteTaskWidgetAction.kt#L45-L75) | Resolved |
| **BUG-342** | Expired Focus Sessions Permanently Lock Tasks as Focused Blocking Auto-Scheduling and Daily Replanning | High | Core Logic & Scheduling / Focus Session Expiration | [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L236), [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L525) | Resolved |
| **BUG-343** | Overdue Tasks Trapped in Non-Urgent Quadrants and At-Target Task Reminders Silently Dropped in `NotificationWorker.kt` | Medium | Background Services & Notifications / Deadline Escalation & Reminder Delivery | [NotificationWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L400-L520) | Resolved |
| **BUG-344** | Audio Playback Auto-Resumes After Manual Pause on Audio Focus Gain and Missing DSP Generator Reset Leaks DC Offsets | Medium | Audio DSP / Audio Focus Lifecycle & State Reset | [NeuralSoundscapePlayer.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapePlayer.kt#L45-L250) | Resolved |
| **BUG-345** | `TaskDao.insert` with `OnConflictStrategy.REPLACE` Triggered SQLite `ON DELETE CASCADE` Deleting Micro-Steps and Focus Sessions on Task Updates | Critical | Room Database & Foreign Keys / Cascade Deletion Prevention | [TaskDao.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/TaskDao.kt#L11-L15) | Resolved |
| **BUG-346** | `TaskDao.observeTasksForDate` Omitted Dateless Timed Tasks with Absolute Epoch Timestamps Dropping Scheduled Tasks | High | Core Data Integrity / Temporal Query Resolution | [TaskDao.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/TaskDao.kt#L41-L55) | Resolved |
| **BUG-347** | `TaskRepository.delete` Ignored `#part` Continuation Tasks Leaving Orphaned Split Tasks and Deadlocked Prerequisites | High | Core Data Integrity & Scheduling / Continuation Part Cascade Deletion | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L265-L310) | Resolved |
| **BUG-348** | `TaskRepository.splitTaskIntoSessions` Caused Extinction of Recurring Habits by Bypassing Recurrence Lifecycle and Leaving Orphaned Open Sessions | Critical | Core Data Integrity & Recurrence / Recurring Habit Extinction & Session Cleanup | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L400-L415) | Resolved |
| **BUG-349** | `completeAndRecur` Dropped Scheduled and Deadline Dates to Null for Recurring Habits with Absolute Epoch Timestamps | High | Core Data Integrity & Recurrence / Temporal Recurrence Anchor Preservation | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L490-L540) | Resolved |
| **BUG-350** | `SessionRepository` and `FocusSessionDao` Missing Task ID Deletion and Open Session Purge Methods Leaking Focus Sessions | Medium | Room Database & Concurrency / Cross-Table Session Synchronization | [FocusSessionDao.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/FocusSessionDao.kt#L30-L38), [SessionRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/SessionRepository.kt#L120-L130) | Resolved |
| **BUG-351** | `MainActivity.onCreate` Omits `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY` Guard Re-executing Historical Intents on Recents Resume & `ACTION_MAIN` Intent Wiping | High | Android Lifecycle / Intent Dispatch & Task History | [MainActivity.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L187) | Resolved |
| **BUG-352** | Asynchronous Focus Task Query in `MainActivity.handleIntent` Leaves State Null Flashing `NewChapterCard` / `TopGoalsRefillCard` | High | UI & State Synchronization / Async Focus Resolution Race | [MainActivity.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L110-L140) | Resolved |
| **BUG-353** | `ACTION_OPEN_FOCUS` Fallback Task Selection Omits Future Task & Blocked Part 2 Continuation Filtering Erroneously Selecting Future Tasks | High | Core Logic & Task Scheduling / Actionable Task Selection | [MainActivity.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L120-L135) | Resolved |
| **BUG-354** | Blank / Whitespace Task IDs in `NeuroFlowAppV2.kt` Bypass Consumed Callback Causing Permanent Lockout of Welcome and Refill Sheets | Medium | UI & State Management / Initial Task Consumption Guard | [NeuroFlowAppV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L77-L105) | Resolved |
| **BUG-355** | Missing `Modifier.fillMaxSize()` on Analytics and History Scaffold Content Host `Box` in `NeuroFlowAppV2.kt` | Low | UI Layout / Viewport Sizing & Measurement | [NeuroFlowAppV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L338-L375) | Resolved |
| **BUG-356** | Intraday Deadline Epoch Defaulting to `System.currentTimeMillis()` in `PlanViewModelV2.tasksForDay` Causing Task Drop on Multi-Day Views and False Leak into Today | High | UI & Data Integrity / Multi-Day Temporal Epoch Resolution | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L363) | Resolved |
| **BUG-357** | Cleared Scheduled Date Resurrected and Absolute Epoch Timestamps Mismatched in `PlanViewModelV2.addTask()` Preventing Unscheduled Backlog Creation | High | Core Data Integrity / Temporal Scheduling & Backlog Preservation | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L780-L797) | Resolved |
| **BUG-358** | Iceboxed Tasks (`Quadrant.ELIMINATE`) Leaking into `tasksForDay` and `unscheduledTasks` Staging Drawer in `PlanViewModelV2.kt` | Medium | UI & State Synchronization / Icebox Task Filtering | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L354-L378) | Resolved |
| **BUG-359** | `contextTag` Omitted in Search Query Filter and Manual Peak Profile Ignored in `PlanViewModelV2` and `TimelineScheduleView` | Medium | UI & Search / Context Tag & Circadian Calibration | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L341), [PlanScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L855) | Resolved |
| **BUG-360** | Missing Ultradian Buffer Between Split Parts and Untimed Task Ultradian Buffer Leak in `PlanScreenV2.shouldShowUltradianBufferAfter` | Medium | UI & Core Logic / Ultradian Recovery Buffer Display | [PlanScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L824-L850) | Resolved |
| **BUG-361** | User Note Title Overwritten by Inferred Sanitized Title in `PlanViewModelV2.createTaskFromNote(title, description)` and Frog Status Stuck on Quadrant Cycling | Medium | UI & Data Integrity / Note Title Preservation & Frog State Management | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L707), [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L850) | Resolved |
| **BUG-362** | Cleared Scheduled Date Resurrected to Today and Future Absolute Epoch Timestamps Unnormalized in `FlowViewModelV2.addTask()` | High | Core Data Integrity / Temporal Scheduling & Backlog Preservation | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L572-L600) | Resolved |
| **BUG-363** | User Note Title Clobbered by Inferred Title in `FlowViewModelV2.addTaskFromNote(title, content)` and Omitted Nudge/Calendar Sync in `breakDownTask()` | Medium | UI & Data Integrity / Note Title Preservation & Breakdown State Sync | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L730), [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L1060) | Resolved |
| **BUG-364** | `#part2` Split Continuation Tasks and `_part1` Parent Tasks Not Detected as Blocked in `FlowViewModelV2` Actionable and Recommendation Pools | High | Core Logic & Scheduling / Split Task Continuation & Prerequisite Enforcement | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L371), [FlowScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt#L243) | Resolved |
| **BUG-365** | Algorithmic Peak End Hour Overrides Manual Peak Window in `FlowViewModelV2.shiftTaskToPeak()` Inverting Morning Active Check | Medium | Core Logic & Circadian Calibration / Manual Peak Profile Alignment | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L751) | Resolved |
| **BUG-366** | Untimed Tasks Accumulate into Ultradian Recovery Dividers and Stale Gap Accumulation in `EnergyTimelineStream.kt`, Plus Untimed "NOW" Playhead Leak | Medium | UI & Core Logic / Ultradian Recovery Buffer & Playhead Accuracy | [EnergyTimelineStream.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt#L101), [EnergyTimelineStream.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt#L665) | Resolved |
| **BUG-367** | `#part2` Split Continuation Tasks and `#part1` Parent Tasks Not Detected as Blocked in `FocusViewModelV2.findNextActionableTask()` | High | Core Logic & Scheduling / Split Task Continuation & Prerequisite Enforcement | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L983) | Resolved |
| **BUG-368** | `FocusViewModelV2.completeTask()` Reads Stale `uiState.elapsedSeconds` Truncating Focus Minutes on Background Completion | High | Core Logic & Telemetry / Elapsed Focus Time Background Recalculation | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L690) | Resolved |
| **BUG-369** | Unbounded Overdue Sprint Duration in `FocusService.handleCompleteAndStop()` Fabricates Phantom Focus Hours When Completed from Notification | High | Background Services & Telemetry / Overdue Sprint Duration Capping & End Time Anchoring | [FocusService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L236) | Resolved |
| **BUG-370** | `FocusViewModelV2.autoBreakDownTask()` Retains `skippedMicroStepIndices` from Prior Decompositions Skipping Fresh Step 0 | Medium | UI & State Management / Micro-Step Decomposition Reset | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L450) | Resolved |
| **BUG-371** | Decompression Rest Session Timestamps Leak into Subsequent Task Completion and Auto-Tracker Resumes During Sprint Completion Review | Medium | UI & Timer State / Rest Session State Leakage & Auto-Tracker Review Guard | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L380), [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L318) | Resolved |
| **BUG-372** | `PlacementStability.verdict()` Omits Parameterized `nowMillis` in `task.resolveDeadlineEpochMillis()` Defaulting to Real-Time System Clock and Distorting Deadline Urgency | Medium | Core Logic & Scheduling / Parameterized Deadline Epoch Resolution | [PlacementStability.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/PlacementStability.kt#L66) | Resolved |
| **BUG-373** | `TaskSchedulerService.scheduleDayWithOutcome()` Omits `completionEndMap[parentId]` Update During Hysteresis Placement and Omits `parentTaskId` / Title Checks in Dynamic `isSplitContinuation` | High | Core Logic & Scheduling / Hysteresis Continuation Completion Synchronization | [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L555), [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L880-L886) | Resolved |
| **BUG-374** | `TaskSchedulerService.splitIntoUltradianParts()` Phase 1 Ceiling Ignores `dayEndMillis` Causing Phase 1 to Starve Part 2 Near Workday End | High | Core Logic & Scheduling / Ultradian Split Workday End Headroom | [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L948-L953) | Resolved |
| **BUG-375** | `TaskSchedulerService.cleanSplitTaskId()` and `DependencyGraphValidator.sanitizeDag()` Use Naive `substringBefore("_part")` Corrupting Task IDs and Silently Dropping Unplaced Part 2 | High | Core Logic & Data Integrity / Split Task ID Regex Safety & Completion Sweep | [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L1098), [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L1136), [DependencyGraphValidator.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/DependencyGraphValidator.kt#L34-L36) | Resolved |
| **BUG-376** | `SchedulePreparationOrchestrator` Omits `parentTaskId` and Legacy `_part2` Inference from `SchedulableTask.prerequisiteIds` Causing Premature Subtask Placement | Medium | Core Logic & Orchestration / Continuation & Subtask Dependency Synchronization | [SchedulePreparationOrchestrator.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt#L172-L175) | Resolved |
| **BUG-377** | Typo Edits in Smart Task Composer Unconditionally Wipe Manual Date/Time/Chip Overrides and Un-clear Explicitly Cleared Dates | High | UI & State Management / Composer Override Invalidation | [SmartTaskComposerSheetV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L503-L516) | Open |
| **BUG-378** | QuickNotes AI Task Synthesis Evaluates Content Length Strictly Ignoring Note Title | Medium | UI & NLP Inference / Working Memory AI Synthesis Trigger | [QuickNotesBottomSheet.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/notes/QuickNotesBottomSheet.kt#L48-L77) | Open |
| **BUG-379** | Deprecated `Icons.Default.HelpOutline` Vector Asset Usage in Smart Task Composer Header | Low | Code Quality / Deprecated API | [SmartTaskComposerSheetV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L405) | Open |
| **BUG-380** | Repeated `SimpleDateFormat` & `Date` Allocations on Every Item Recomposition in QuickNotes Recent Notes List | Low | Performance & Resource Churn / Date Formatting Allocation | [QuickNotesBottomSheet.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/notes/QuickNotesBottomSheet.kt#L248-L249) | Open |
| **BUG-381** | `CalendarSyncDataSource.findEventIdByTaskId` Queries Omit `${CalendarContract.Events.DELETED} = 0` Matching Tombstoned Events and Stack Duplicate Title Prefixes | High | Integration / Calendar Synchronization | [CalendarSyncDataSource.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt#L257) | Resolved |
| **BUG-382** | Clearing Deadline or Scheduled Date in `SmartTaskComposerSheetV2` Leaves Dangling Intraday Times Causing Phantom Deadlines and Invisible Timeline Slots | High | UI & Data Integrity / Dangling Intraday Time & Phantom Deadline | [SmartTaskComposerSheetV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L287-L359) | Resolved |
| **BUG-383** | `SmartTaskComposerSheetV2` Recurring Task `effectiveHabitDate` Ignores `initialDate` Defaulting to Current Day in Create Mode | Medium | UI & Chronobiology / Habit Creation Date Anchor | [SmartTaskComposerSheetV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L307) | Resolved |
| **BUG-384** | `TimePickerDialog` Confirmation Blocked from Setting Date When Previously Cleared Due to Stale `cleared` Flags | Medium | UI & State Synchronization / TimePicker Confirmation Date Assignment | [SmartTaskComposerSheetV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L1583-L1596) | Resolved |
| **BUG-385** | Split Continuation `#part2` Tasks and Subtasks Prematurely Eligible as Top Action in `FocusWidgetUpdateWorker` and `DailyPlanWorker` | High | Core Logic & UI / Continuation & Dependency Graph Enforcement | [FocusWidgetUpdateWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L116), [FocusWidgetUpdateWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L263-L279), [NotificationWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L181-L191) | Resolved |
| **BUG-386** | `CompleteTaskWidgetAction` Telemetry Inflation and Unbounded Sprint Duration on One-Tap Widget Completion | High | Data Integrity & Telemetry / Overdue Sprint Capping & End Time Anchoring | [CompleteTaskWidgetAction.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/widget/CompleteTaskWidgetAction.kt#L49-L59) | Resolved |
| **BUG-387** | `ScheduleAutoTasksWorker` Candidate and Fixed Task Filtering Fails on `#part2` Split Continuation Tasks and ID Regex Discontinuity | High | Core Logic & Data Integrity / Split Continuation Scheduling & Grouping | [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L212), [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L232), [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L243), [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L260), [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L339) | Resolved |
| **BUG-388** | `SleepPressureRefreshWorker`, `DistractionSyncWorker`, `EnergyTelemetryCleanupWorker`, and `KioskPolicyWorker` Catch Coroutine `CancellationException` Breaking Structured Concurrency | Medium | Concurrency & WorkManager / Structured Concurrency Contract | [SleepPressureRefreshWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/SleepPressureRefreshWorker.kt#L37), [DistractionSyncWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/DistractionSyncWorker.kt#L114), [EnergyTelemetryCleanupWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/EnergyTelemetryCleanupWorker.kt#L49), [KioskPolicyWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/KioskPolicyWorker.kt#L23) | Resolved |
| **BUG-389** | `DeadlineEscalationWorker` Resurrects Iceboxed Tasks (`Quadrant.ELIMINATE`) into `DO_FIRST` Triggering Notification Spam | High | Background Services & Notifications / Icebox State Integrity | [NotificationWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L410-L425) | Resolved |
| **BUG-390** | `AutonomyNudgeWorker` Silently Drops Nudges for Tasks with Absolute Epoch Deadline Timestamps (`deadlineDate == null`) | High | Background Services & Notifications / Autonomy Nudge Lifecycle | [AutonomyNudgeWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/AutonomyNudgeWorker.kt#L55-L65) | Resolved |
| **BUG-391** | `NeuralSoundscapePlayer.play()` and `stop()` Fail to Clear `isReleasePending`, Causing Premature `AudioTrack` Teardown on Rapid Re-Entry | Medium | Audio DSP / Playback Lifecycle & Native AudioTrack Release | [NeuralSoundscapePlayer.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapePlayer.kt#L143-L226) | Resolved |
| **BUG-392** | `FocusService.onStartCommand(ACTION_START)` Fails to Cancel Running `tickerJob` When Session is Started Paused | Low | Background Services & Performance / Ticker Job Lifecycle | [FocusService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L133) | Resolved |
| **BUG-393** | Zombie Open Time Sessions Leaked and Tracked Minutes Lost on Direct Task Completion in `TaskRepository.completeAndRecur()` & `markTaskCompleted()` | High | Core Data Integrity & Session Tracking | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L484-L494) | Resolved |
| **BUG-394** | Corrupted Task IDs and Title Slicing on Slug String Truncation in Task Deletion and Auto-Schedule Grouping | Medium | Data Integrity / Task Splitting | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L320), [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L584) | Resolved |
| **BUG-395** | Part 2 Continuation Recurrence Advancement and Ghost Recurring Task Duplication | High | Scheduling / Recurrence | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L340) | Resolved |
| **BUG-396** | Checklist and Micro-Step Dropping on Split Task Sessions | Medium | Data Persistence / Micro-Steps | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L455) | Resolved |
| **BUG-397** | Unbounded Memory Scanning in Session Interruption Summary Computation | Medium | Performance / Database | [TimeSessionDao.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/TimeSessionDao.kt#L42), [SessionRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/SessionRepository.kt#L41) | Resolved |
| **BUG-398** | `MainActivity.onCreate` Omits In-Flight Async Task Resolution Resumption Leaking Zombie Resolution State Across Activity Recreation | High | UI & State Synchronization / Async Lifecycle Race | [MainActivity.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L224) | Resolved |
| **BUG-399** | `ACTION_OPEN_FOCUS` Fallback Task Selection Omits Multi-Part Continuation Tasks (`#part3`, `_part3`, etc.) Prematurely Selecting Split Tasks Ahead of Active Parents | Medium | Core Logic & Task Scheduling / Split Task Continuation Prerequisite Check | [MainActivity.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L130) | Resolved |
| **BUG-400** | Missing `launchSingleTop = true` on Sub-Screen Navigation Routes in `NeuroFlowAppV2.kt` Allowing Duplicate Backstack Stacking on Rapid Clicks | Medium | UI & Navigation / Backstack Integrity & Rapid Click Debouncing | [NeuroFlowAppV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L229-L280) | Resolved |
| **BUG-401** | External Focus / WOOP Intent Pushes Duplicate Focus Destination Over Active Focus Session | Medium | UI & State Management / Focus Backstack Stacking | [NeuroFlowAppV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L77-L108) | Resolved |
| **BUG-402** | Split Continuation Part 2+ Tasks Excluded From Auto-Scheduling and Dropped/Deleted When Parent Completed in `PlanViewModelV2.autoScheduleDay()` | High | Core Logic & Scheduling / Split Continuation Lifecycle | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L526-L670) | Resolved |
| **BUG-403** | Overdue Past Tasks Completely Vanish from `PlanScreenV2` on Today's Plan View | High | UI & State Synchronization / Overdue Task Visibility | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L355-L382) | Resolved |
| **BUG-404** | Integer Overflow Past Midnight in `PlanViewModelV2.assignTaskTime` and `resolveConflict` Corrupts Scheduled Date to 1970 | High | UI & Temporal Integrity / Midnight Epoch Overflow | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L702), [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L1075) | Resolved |
| **BUG-405** | Un-normalized Absolute Epoch Deadlines and Dropped Context Tags in `PlanViewModelV2.createTaskFromNote` and `addTask` | Medium | Core Data Integrity / Temporal Normalization & Tag Sync | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L804-L880) | Resolved |
| **BUG-406** | Multi-part continuation tasks (`#part3`, `_part3`, etc.) and trailing commas in dependencies bypass blocking in `FlowViewModelV2.kt` & `FlowScreenV2.kt` | High | UI & Core Logic / Continuation Prerequisite & Dependency Integrity | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L369-L382), [FlowScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt#L246-L265) | Resolved |
| **BUG-407** | Un-normalized Absolute Epoch Deadlines, Omitted `contextTag`, and Unconditional Autonomy Nudge on Icebox Tasks in `FlowViewModelV2.kt` | Medium | Core Data Integrity / Temporal Normalization & Tag Sync | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L586-L740) | Resolved |
| **BUG-408** | `FlowViewModelV2.deferTask()` Anchors Base Epoch to Midnight or Past for Overdue and Untimed Tasks Leaving Tasks Overdue | High | UI & Temporal Integrity / Task Deferral Anchor | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L943-L965) | Resolved |
| **BUG-409** | Overdue Tasks Mapped to Midnight (00:00) on Today's Circadian Curve in `EnergyTimelineStream.kt`, Multi-Part Continuation Label Collapses to `Part 1/2`, and Interleaved Untimed Tasks Mask Break Resets | Medium | UI & Circadian Alignment / Timeline Energy Badge & Ultradian Buffer | [EnergyTimelineStream.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt#L114-L690) | Resolved |
| **BUG-410** | Multi-Part Continuation Tasks (`#part3`, `_part3`, etc.) and Active Subtasks Ingested Out-of-Order in `FocusViewModelV2.findNextActionableTask()` | High | UI & Core Logic / Continuation Prerequisite & Subtask Integrity | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L1060-L1110) | Resolved |
| **BUG-411** | Future Scheduled Day Tasks Ingested Ahead of Today's Backlog & Stale Fallback on Queue Exhaustion in `FocusViewModelV2.findNextActionableTask()` | High | UI & Temporal Integrity / Focus Queue Prioritization & Navigation Flow | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L644-L1110) | Resolved |
| **BUG-412** | Sprint Completion Telemetry (`sessionCount`, `lastSessionDurationMinutes`) Dropped Across Decompression Rest and Expired Recovery in `FocusViewModelV2.kt` | Medium | Data Integrity & Telemetry / Focus Metrics & Decompression Rest Retention | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L140-L680) | Resolved |
| **BUG-413** | Incomplete Micro-Step Badge Displayed on Completed Tasks in `HeroTaskCard` and Missing Widget Refresh on Expired Background Timer in `FocusService.kt` | Medium | UI & Background Services / Widget Synchronization & Micro-Step Badge | [FocusScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt#L236), [FocusService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L112-L255) | Resolved |
| **BUG-414** | Multi-Part Continuation Final Part Selection Determinism and Alphanumeric Inversion in `DependencyGraphValidator.kt` | High | Core Logic & Scheduling / Multi-Part Continuation Chaining | [DependencyGraphValidator.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/DependencyGraphValidator.kt#L33-L43) | Resolved |
| **BUG-415** | Continuation Part Prerequisite Omission for Multi-Part Tasks (`#part2`, `#part3`, `_part3`) in `SchedulePreparationOrchestrator.kt` | High | Core Logic & Scheduling / Split Continuation Prerequisite Inference | [SchedulePreparationOrchestrator.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt#L149-L163) | Resolved |
| **BUG-416** | Cross-Day Parent Prerequisite Deadlock and Hysteresis Rejection for Split Continuation Tasks in `TaskSchedulerService.kt` | High | Core Logic & Scheduling / Continuation Parent Prerequisite Deadlock | [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L506-L675) | Resolved |
| **BUG-417** | Part 1 Tasks Erroneously Classified as Part 2 Continuations and Title Cleaning Defeat in `SchedulePreparationOrchestrator.kt` | Medium | Core Logic & Scheduling / Part 1 Continuation Isolation | [SchedulePreparationOrchestrator.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt#L127-L187) | Resolved |
| **BUG-418** | `TaskInferenceEngine.kt` Due Time Detection Failure for Intraday Deadlines (`due at 5pm`, `deadline at 5pm`, `by 5pm`, `due this afternoon`) Forcing Conversion to Scheduled Start Times and Dangling Title Keywords | High | UI & NLP Inference / Intraday Due Time Misclassification | [TaskInferenceEngine.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/inference/TaskInferenceEngine.kt#L81-L380) | Resolved |
| **BUG-419** | Implicit Clock Time Scheduled Date Default Overriding Explicit `initialDate` and `existingTask.scheduledDate` in `SmartTaskComposerSheetV2.kt` | High | UI & State Synchronization / Scheduled Date Shadowing | [SmartTaskComposerSheetV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L174-L545), [TaskInferenceEngine.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/inference/TaskInferenceEngine.kt#L38-L385) | Resolved |
| **BUG-420** | Calendar Sync Focus Block Anchored to 9:00 AM Default for Deadline-Only Tasks in `CalendarSyncDataSource.kt`, Causing Focus Block to Span Past Deadline | Medium | Integration / Calendar Synchronization Deadline Alignment | [CalendarSyncDataSource.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt#L325-L395) | Resolved |
| **BUG-421** | Space-Separated Multi-Hashtag Input (`#deepwork #coding`) Ingested as Single Malformed Catalog Tag in `TaskTagViewModel.kt` and `UserPreferencesDataStore.kt` | Low | Core Data Integrity / Tag Normalization | [TaskTagViewModel.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/common/TaskTagViewModel.kt#L47), [UserPreferencesDataStore.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/UserPreferencesDataStore.kt#L890) | Resolved |
| **BUG-422** | `ScheduleAutoTasksWorker.applyPlacementsTransactionally` Groups Placements By Stripping Suffixes, Corrupting Part 2 / Continuation Task Placements | High | Core Logic & Background Services / Continuation Task Scheduling & Persistence | [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L348-L385) | Resolved |
| **BUG-423** | Continuation Part (`_part3`, `#part3`) and Preceding Part Dependency Blocking Missing in `ScheduleAutoTasksWorker`, `NotificationWorker`, and `FocusWidgetUpdateWorker` | High | Background Services & Logic / Multi-Part Continuation & Prerequisite Blocking | [ScheduleAutoTasksWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L184-L189), [NotificationWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L182-L185), [FocusWidgetUpdateWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L268-L271) | Resolved |
| **BUG-424** | Trailing Commas and Empty Tokens in `dependsOnTaskIds` Permanently Block Tasks in `DailyPlanWorker`, and SchedulableTask Prerequisite IDs Omitted | Medium | Background Services & Notifications / Daily Plan Dependency Parsing | [NotificationWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L180-L181), [NotificationWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L224-L248) | Resolved |
| **BUG-425** | `AutonomyNudgeWorker` Fires for Blocked Tasks and Inactive Continuation Parts | Medium | Background Services & Autonomy Engine / Blocked Task Nudge Suppression | [AutonomyNudgeWorker.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/AutonomyNudgeWorker.kt#L40-L65) | Resolved |
| **BUG-426** | `CompleteTaskWidgetAction` and `FocusService` Fail to Cancel Active Notifications, Reminders, and Nudges on Completion | Medium | Background Services & Notifications / Task Completion Notification Cleanup | [CompleteTaskWidgetAction.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/widget/CompleteTaskWidgetAction.kt#L73-L103), [FocusService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L480-L482) | Resolved |
| **BUG-427** | `completeAndRecur` Only Suppresses Recurrence on Part 2, Allowing Multi-Part (`#part3+`) Continuations to Advance Recurring Habit Chains | High | Core Data Integrity & Recurrence / Multi-Part Habit Recurrence Isolation | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L573-L578) | Resolved |
| **BUG-428** | `TaskRepository.delete` Misses Multi-Part (`#part3+`) Continuations, Causing Database Zombie Tasks, Stranded WorkManager Reminders, Leaked Sessions, and Downstream Dependency Deadlocks | High | Core Data Integrity / Dynamic Multi-Part Cascade Deletion & Dependency Cleanup | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L283-L325), [TaskDao.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/TaskDao.kt#L96-L109) | Resolved |
| **BUG-429** | `TaskDao.observeTasksForDate` Unconditionally Matches `habitDate` for Recurring Tasks When Explicitly Rescheduled to Other Days, Duplicating Tasks Across Multiple Days | Medium | Room Database & Scheduling / Habit Date Temporal Isolation | [TaskDao.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/TaskDao.kt#L48-L66), [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L265-L277) | Resolved |
| **BUG-430** | `SessionRepository.syncFocusSession` Overwrites Real Recorded `energyAtStart` Back to Hardcoded Default 50 on Session Updates | Medium | Core Data Integrity / Focus Session Energy Telemetry Preservation | [SessionRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/SessionRepository.kt#L91-L105) | Resolved |
| **BUG-431** | `splitTaskIntoSessions` Omits Lazy Micro-Step Extraction From `ifThenPlan` and Fabricates Redundant Plan Copies on Later Continuation Parts | Medium | Core Data Integrity & Task Splitting / Lazy Micro-Step Plan Propagation | [TaskRepository.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L394-L445) | Resolved |
| **BUG-432** | `MainActivity.resolveDefaultFocusTask` Ingests Non-Actionable `waitingFor` Tasks and Omits Preceding Part (`#part1`, `#part2`) Blocking for Multi-Part Continuations | High | UI & Navigation / Default Focus Task Resolution & Continuation Integrity | [MainActivity.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L165-L215) | Resolved |
| **BUG-433** | Redundant Focus Destination Re-Creation in `NeuroFlowAppV2.kt` When Navigating to Already Active Focus Task Resets Session State | Medium | UI & State Synchronization / Redundant Navigation & Focus State Reset | [NeuroFlowAppV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L91-L125) | Resolved |
| **BUG-434** | Asynchronous WOOP Reflection Intent Mode (`ACTION_WOOP_REFLECT`) Lost Across Activity Recreation / Device Rotation in `MainActivity.kt` | Medium | UI Lifecycle & State Persistence / Async Intent Resolution Restoration | [MainActivity.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L215-L260) | Resolved |
| **BUG-435** | Alternate Intent Extra `extra_task_id` Ignored in `MainActivity.kt` Pending Action Verification and Intent Parsing | Low | Integration & Intent Handling / Alternate Extra Key Compatibility | [MainActivity.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L80-L95) | Resolved |
| **BUG-436** | `PlanViewModelV2.autoScheduleDay` Uses Incomplete String Slicing When Deriving Parent ID for Multi-Part Continuations (`#part3+`) and Omits Preceding Active Part Blocking | High | Core Logic & Auto-Scheduling / Multi-Part Continuation Integrity & Self-Blocking Prevention | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L525-L565) | Resolved |
| **BUG-437** | `PlanScreenV2` Hardcodes `Part 1/2` Label for Multi-Part Continuation Tasks and Omits Inter-Part Ultradian Buffers on `TimelineScheduleView` | Medium | UI & State Management / Multi-Part Continuation Labeling & Ultradian Recovery Dividers | [PlanScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L824-L857, #L1140-L1160) | Resolved |
| **BUG-438** | `PlanViewModelV2.shiftTaskToPeak` Defers Tasks to Tomorrow When Circadian Peak Wraps Around Midnight Due to Strict `peakHour <= peakEndHour` Guard | Medium | Core Logic & Circadian Alignment / Midnight-Wrapping Peak Window Calculation | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L1055-L1085) | Resolved |
| **BUG-439** | `PlanViewModelV2.addTask` Drops `habitDate` on Recurring Tasks With Explicit `scheduledDate` and `uncompleteTask` Re-schedules Autonomy Nudge for Iceboxed Tasks | Low | Core Data Integrity & Notifications / Habit Date Initialization & Icebox Nudge Guard | [PlanViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L850-L865, #L1200-L1210) | Resolved |
| **BUG-440** | Multi-Part Continuation Tasks Without `#part` in ID (UUIDs / Custom Slugs) Match Themselves in `isPart1Active` and `isPrecedingPartActive`, Permanently Self-Blocking From Actionable Pool and Spotlight | High | Core Logic & Task Actionability / Continuation Self-Blocking Prevention | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L363-L380), [FlowScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt#L256-L265) | Resolved |
| **BUG-441** | `FlowViewModelV2.shiftTaskToPeak` Defers Tasks to Tomorrow When Circadian Peak Wraps Around Midnight Due to Strict `peakHour <= peakEndHour` Guard | Medium | Core Logic & Circadian Alignment / Midnight-Wrapping Peak Window Calculation | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L836-L856) | Resolved |
| **BUG-442** | `FlowViewModelV2.addTask` Drops `habitDate` on Recurring Tasks With Explicit `scheduledDate` and `shiftTaskToPeak` / `breakDownTask` Omit Icebox Nudge Guard | Low | Core Data Integrity & Notifications / Habit Date Anchoring & Icebox Nudge Guard | [FlowViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L606-L614, #L864-L868, #L1130-L1135) | Resolved |
| **BUG-443** | `EnergyTimelineStream.shouldShowUltradianBufferAfter` Emits 15m Ultradian Rest Buffer Between Untimed Split Parts Due to Inverted Untimed Task Guard | Low | UI & Boundary Handling / Untimed Split Buffer Suppression | [EnergyTimelineStream.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt#L676-L695) | Resolved |
| **BUG-444** | Icebox Nudge Alarm Leak on Focus Screen Exit | Medium | Alarm & Notification | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L1010-L1020) | Resolved |
| **BUG-445** | Missing Reminder Notification & WorkManager Cancellation on Focus Task Completion | High | WorkManager / Notification | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L788-L797) | Resolved |
| **BUG-446** | UUID Multi-Part Continuation Tasks Prematurely Unblocked Ahead of Sequential Predecessors | High | Scheduling / Dependency | [FocusViewModelV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L1257-L1270) | Resolved |
| **BUG-447** | FocusService ACTION_PAUSE Null Active Session Drop & Stale Remaining Seconds | Medium | Background Service / Concurrency | [FocusService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L140-L165) | Resolved |
| **BUG-448** | Trapped Completed State & Zombie WOOP Prompt on Completed Tasks | Medium | UI / State Machine | [FocusScreenV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt#L170-L185) | Resolved |
| **BUG-449** | `TaskSchedulerService.kt` Prematurely Updates `completionEndMap[parentId]` and Unlocks Downstream Dependents on Part 1 or Intermediate Continuation Parts | High | Core Logic & Scheduling / Multi-Part Continuation Completion Synchronization | [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L562-L572), [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L914-L974) | Resolved |
| **BUG-450** | `TaskSchedulerService.kt` Erroneously Blocks Multi-Part UUID Continuation Parts as Cross-Day Prerequisites and Omits Anchored Status in Hysteresis | High | Core Logic & Scheduling / UUID Continuation Prerequisite Resolution | [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L511-L516), [TaskSchedulerService.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L683-L687) | Resolved |
| **BUG-451** | `SchedulePreparationOrchestrator.kt` Anchors Untimed Date-Only Tasks to Midnight 00:00:00 and Injects Parent Task ID as Prerequisite for Part 1 | High | Core Logic & Orchestration / Temporal Anchoring & Part 1 Prerequisite Isolation | [SchedulePreparationOrchestrator.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt#L157-L160), [SchedulePreparationOrchestrator.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt#L194) | Resolved |
| **BUG-452** | `AutoSchedulingContracts.hasManualScheduleData()` Fails to Recognize Dual-Representation Epoch Timestamps Allowing Auto-Scheduler Overwrite | Medium | Core Contracts & Data Integrity / Manual Schedule Recognition | [AutoSchedulingContracts.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingContracts.kt#L14-L16) | Resolved |

---

### Detailed Findings

#### BUG-001: Coroutine Creation During Compose Composition
- **Severity**: High
- **Category**: Runtime / Concurrency
- **File**: `app/src/main/java/com/neuroflow/app/MainActivity.kt` (lines 305–320)
- **Problem Description**:
  Inside the main Composable UI content branch (`else -> { ... }`), `scope.launch` is called directly during composition when `!freshStartHandled`. Calling coroutine builders directly within composition without `LaunchedEffect` can trigger multiple coroutines on recomposition, causes side effects before layout completion, and violates Compose contract (`CoroutineCreationDuringComposition`).
- **Expected Behavior**:
  One-shot side-effects triggered on app start should execute inside a lifecycle-aware effect handler (`LaunchedEffect(Unit)`).

---

#### BUG-002: Redundant / Dead Condition in MiniFocusBar Check
- **Severity**: Low
- **Category**: Code Quality / Dead Code
- **File**: `app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt` (line 122)
- **Problem Description**:
  `showMiniFocusBar` is evaluated as:
  ```kotlin
  val showMiniFocusBar = session != null && !currentRoute.startsWith("focus_v2") && !currentRoute.startsWith("focus/")
  ```
  On line 122, the condition checks:
  ```kotlin
  if (showMiniFocusBar && session != null)
  ```
  Since `session != null` is already guaranteed by `showMiniFocusBar`, the compiler warns that `session != null` is always `true`.
- **Expected Behavior**:
  Clean, non-redundant boolean checks without dead conditions.

---

#### BUG-003: `QUERY_ALL_PACKAGES` Lint Error and Policy Risk
- **Severity**: Medium
- **Category**: Android Manifest / Policy
- **File**: `app/src/main/AndroidManifest.xml` (line 25)
- **Problem Description**:
  `<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />` is flagged as a fatal error by Android Lint (`QueryAllPackagesPermission`) because it exposes all installed packages and requires explicit justification under Google Play policies.
- **Expected Behavior**:
  If full package visibility is required for the app-blocking / focus-lock feature, the lint check should explicitly suppress the warning with justification, or targeted queries should be used.

---

#### BUG-004: Midnight Boundary Flakiness in Analytics Clamping Test
- **Severity**: Medium
- **Category**: Unit Testing / Date Boundary
- **File**: `app/src/test/java/com/neuroflow/app/domain/service/Adr030RobustnessTest.kt` (lines 206–235)
- **Problem Description**:
  `testAnalyticsEngine_clampsFutureCompletedTasks` initialized `val now = System.currentTimeMillis()` and tested task completions with `validTask.completedAt = now - 1800_000L` (30 minutes ago). Whenever tests ran between 00:00 and 00:30 local midnight, `now - 30 minutes` crossed the day boundary into yesterday (`< todayStart`). `AnalyticsEngine.buildSummary` clamps today's completions via `c >= todayStart && c <= nowMillis`. Consequently, `validTask` was excluded from today's completed count, producing `0` instead of `1` and failing the unit test suite (`AssertionError: expected:<1> but was:<0>`).
- **Expected Behavior**:
  Unit tests verifying relative time offsets must remain deterministic across all 24 hours of the day without failing around local midnight boundaries.
- **Resolution**:
  Anchored `now` to a fixed deterministic midday timestamp (`ZonedDateTime.of(LocalDate.of(2026, 10, 23), LocalTime.of(14, 0), zone).toInstant().toEpochMilli()`).

---

#### BUG-005: Exclusion of Focused Tasks from `fixedWindows` in PlanViewModelV2
- **Severity**: High
- **Category**: Scheduling Engine / Concurrency & Overlap
- **File**: `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt` (lines 446–460)
- **Problem Description**:
  `fixedWindows` was computed using:
  ```kotlin
  val fixedWindows = allTasks.filterNot { AutoSchedulingContracts.isMutableByAutoScheduler(it, nowMillis) || it.id in focusedTaskIds }.mapNotNull { ... }
  ```
  Because `|| it.id in focusedTaskIds` was included inside `filterNot`, any task currently being focused on in an active session was stripped from `fixedWindows`. Since active focused tasks are also excluded from `candidateTasks` (`task.id !in focusedTaskIds`), the auto-scheduler treated the active focus time slot as vacant and scheduled other tasks directly over the user's ongoing session.
- **Expected Behavior**:
  Active focused tasks must not be rescheduled, but their occupied time slots MUST be retained as immutable fixed busy windows.
- **Resolution**:
  Replaced with ID-set exclusion: `allTasks.filterNot { it.id in candidateIds || it.id.endsWith("_part2") }`, guaranteeing all non-candidate tasks (locked, manual, and active focused sessions) are properly protected as fixed windows.

---

#### BUG-006: In-Progress Task Not Frozen in `PlacementStability.verdict`
- **Severity**: Medium
- **Category**: Scheduler Stability / Churn
- **File**: `app/src/main/java/com/neuroflow/app/domain/scheduler/PlacementStability.kt` (lines 59–62)
- **Problem Description**:
  When adjusting `PlacementStability.verdict`, the freeze check was changed to:
  ```kotlin
  if (scheduledMillis - nowMillis in 0 until NEAR_TERM_FREEZE_MILLIS) return ReplanVerdict.NEAR_TERM_FREEZE
  ```
  For an in-progress task whose scheduled block started in the past (`scheduledMillis <= nowMillis`) but has not expired (`!TaskBlockPlan.blockHasExpired(...)`), `scheduledMillis - nowMillis` is negative ($\le 0$). It bypassed `in 0 until NEAR_TERM_FREEZE_MILLIS`, fell through the cooldown check if `taskAge > cooldownMillis`, and was flagged for `PERIODIC_REFRESH` with `allowsReplan = true`. Consequently, background auto-scheduling passes could reschedule or displace a task currently in progress.
- **Expected Behavior**:
  Any task block currently ongoing (`nowMillis in scheduledMillis..scheduledEndMillis + gracePeriod`) must be frozen (`NEAR_TERM_FREEZE`) during periodic refreshes when `forceReplan == false`.
- **Resolution**:
  Moved `if (forceReplan) return ReplanVerdict.FORCED` first, followed by `if (scheduledMillis - nowMillis < NEAR_TERM_FREEZE_MILLIS) return ReplanVerdict.NEAR_TERM_FREEZE`. Added dedicated unit test in `PlacementStabilityTest`.

---

#### BUG-007: Duplicate Task Scheduling via Synthetic Continuation Part 2 Entities (`_part2`)
- **Severity**: High
- **Category**: Data Integrity / Task Duplication
- **File**: `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt` (line 455), `app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt` (lines 141–182)
- **Problem Description**:
  When an ultradian split occurs on an 80m+ task, the scheduler stores Part 1 in `baseTaskId` and creates/inserts a linked continuation entity `${baseTaskId}_part2` into Room DB. On subsequent schedule runs, neither `PlanViewModelV2` nor `ScheduleAutoTasksWorker` filtered out `_part2` entities from candidate tasks. Simultaneously, `SchedulePreparationOrchestrator` cleaned `(Part 1/2)` from `baseTaskId`'s title and evaluated its full original 80m duration from `task.estimatedDurationMinutes`.
  As a result, both the full 80m parent task and the 40m continuation Part 2 entity were passed to the scheduler simultaneously, expanding an 80-minute task into 120 minutes of reserved calendar time and creating duplicate bookings.
- **Expected Behavior**:
  Synthetic continuation entities (`_part2`) should never be ingested as independent schedulable tasks on subsequent runs. The root task represents the complete work unit and will be re-split if necessary, updating or removing `${baseTaskId}_part2` as appropriate.
- **Resolution**:
  Filtered out `!it.id.endsWith("_part2")` from candidate task selection and fixed window evaluation in both `PlanViewModelV2` and `ScheduleAutoTasksWorker`.

---

#### BUG-008: Accidental Deletion of Vice Head Skill in Commit `26d117d`
- **Severity**: Low / Operational
- **Category**: Workspace Invariant / Living Skill
- **File**: `.agents/skills/vice-head/SKILL.md`
- **Problem Description**:
  Git commit `26d117d` deleted `.agents/skills/vice-head/SKILL.md` during a workspace cleanup. Section 1 of `/home/takian/Project/PROCUS/AGENTS.md` mandates that the Vice Head living skill must exist and be maintained across all handoffs.
- **Expected Behavior**:
  `.agents/skills/vice-head/SKILL.md` must be present in the workspace.
- **Resolution**:
  Restored `.agents/skills/vice-head/SKILL.md` from `26d117d^` and verified file integrity.

---

#### BUG-009: Background Auto-Scheduler Ingests Future Tasks and Displaces Manual Schedules
- **Severity**: High
- **Category**: Scheduling Engine / Data Integrity & Temporal Leaks
- **File**: `app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt` (lines 141–188)
- **Problem Description**:
  In `ScheduleAutoTasksWorker.kt`, the background pass for today (`dayDate = today`) collects target tasks via three queries that lack day-horizon boundary filters:
  1. `unscheduledTasks` checks `(task.scheduledDate == null || task.scheduledTime == null) && !AutoSchedulingContracts.hasManualScheduleData(task)`. If a user assigns a task to a future date with date selected but time unset (`task.scheduledDate > dayEnd && task.scheduledTime == null`), it is treated as an unscheduled task for TODAY and placed onto today's calendar, overwriting `scheduledDate`.
  2. `replanCandidates` checks `task.status == TaskStatus.ACTIVE && !task.isScheduleLocked && scheduledMillis != null && PlacementStability.verdict(task, nowMillis, prefs, forceReplan).allowsReplan`. It lacks the boundary check `scheduledMillis in dayStart..dayEnd`. Whenever periodic refresh evaluates (`isAutoScheduleToggleOn` is true and cooldown passes), tasks scheduled on future dates return `allowsReplan = true` and are fed into today's scheduler (`dayDate = today`), prematurely pulling future tasks into today.
  3. `replanCandidates` also omits `AutoSchedulingContracts.isMutableByAutoScheduler(task, nowMillis)`. Consequently, manual future tasks (`!task.isAutoScheduled`) whose cooldown has elapsed are incorrectly treated as replan candidates in `targetTasksMap`, causing them to be excluded from `fixedTasks` and `fixedWindows`, stripping their schedule protection and creating overlaps.
  4. `fixedWindows` checks `if (end > nowMillis)` rather than bounding to `if (end > dayStart && start < dayEnd)`, adding tasks from days and weeks ahead into the single-day busy windows list.
- **Expected Behavior**:
  The background auto-scheduler for today must strictly constrain candidate task selection and fixed windows to today's horizon (`dayStart..dayEnd`), never pull future-dated tasks into today, and preserve the protected windows of manual schedules.
- **Suggested Fix**:
  - In `unscheduledTasks`, check: `task.scheduledDate == null || (task.scheduledDate in dayStart..dayEnd && task.scheduledTime == null) || (task.scheduledDate < dayStart && task.scheduledTime == null)`.
  - In `replanCandidates`, enforce: `AutoSchedulingContracts.isMutableByAutoScheduler(task, nowMillis) && scheduledMillis in dayStart..dayEnd && PlacementStability.verdict(task, nowMillis, prefs, forceReplan).allowsReplan`.
  - In `fixedWindows`, filter: `if (end > dayStart && start < dayEnd) TimeSlotWindow(start, end) else null`.

---

#### BUG-010: Locked Task Fallback Displaces Pinned Slot in `TaskSchedulerService`
- **Severity**: Medium
- **Category**: Scheduler Correctness / Invariant Violation
- **File**: `app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt` (lines 678–709)
- **Problem Description**:
  In `TaskSchedulerService.kt` (Step 4 ready-queue placement), for a task with `isLocked == true`, the scheduler attempts to construct `lockedWindow` at `current.scheduledStartMillis`.
  If `lockedWindow` is null (e.g. because an antecedent prerequisite finished later than the locked start, or a busy window conflict occurred), line 709 falls back via elvis operator:
  ```kotlin
  val bestSlot = lockedWindow ?: findBestCircadianWindow(...)
  ```
  This causes the locked task to be moved to an arbitrary available circadian window, violating the user contract: *"Schedule Locked (Pinned): Auto-scheduler will never displace this task"*.
- **Expected Behavior**:
  If a task is marked `isLocked`, it must only ever be scheduled in its pinned window. If the pinned slot cannot be satisfied due to dependency ordering or capacity constraints, the task must be deferred with an explanatory `DeferralRationale` (`DEPENDENCY_BLOCKED` or `INSUFFICIENT_TIME`), rather than displaced to a different time.
- **Resolution**:
  Guarded slot resolution with `val bestSlot = if (current.isLocked) lockedWindow else findBestCircadianWindow(...)`. When `lockedWindow` is unavailable, locked tasks are deferred with explicit rationale rather than displaced. Added `testLockedTaskFallbackNeverDisplacesToArbitraryCircadianWindow` in `TaskSchedulerServiceTest`.

---

#### BUG-011: Preserved `isAutoScheduled = true` Overrides User Manual Schedule Edits in Composer
- **Severity**: Medium
- **Category**: Data Integrity / User Intent Override
- **File**: `app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt` (lines 234–255)
- **Problem Description**:
  In `SmartTaskComposerSheetV2.kt` (`handleSave`), when editing an existing task (`existingTask != null`), `existingTask.copy(...)` unconditionally preserves `existingTask.isAutoScheduled`.
  If an auto-scheduled task (`isAutoScheduled = true`) is manually edited by the user to set a specific date and time:
  Because `isAutoScheduled` remains `true`, `AutoSchedulingContracts.hasManualScheduleData(task)` returns `false` (`task.isAutoScheduled` is true).
  As a result, `AutoSchedulingContracts.isMutableByAutoScheduler(task)` continues to evaluate to `true`, and subsequent auto-scheduling passes displace the user's manually chosen date and time unless the user also remembered to manually enable the lock switch.
- **Expected Behavior**:
  When a user manually modifies or sets `scheduledDate` or `scheduledTime` in `SmartTaskComposerSheetV2`, `isAutoScheduled` must be set to `false` to mark it as a user-authored manual schedule.
- **Resolution**:
  In `SmartTaskComposerSheetV2.kt` `handleSave()`, set `isAutoScheduled = if (effectiveScheduledDate != existingTask.scheduledDate || effectiveScheduledTime != existingTask.scheduledTime) false else existingTask.isAutoScheduled`.

---

#### BUG-012: Orphaned Continuation Tasks on Parent Deletion & Completed Parent Continuation Collision
- **Severity**: Medium
- **Category**: Data Integrity / Continuation Lifecycle
- **File**: `app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt` (lines 207–225), `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt` (lines 455–465)
- **Problem Description**:
  When an ultradian split creates a continuation entity `${baseTaskId}_part2`:
  1. If `baseTaskId` is deleted via `TaskRepository.delete`, no cascading deletion occurs for `${baseTaskId}_part2`. The continuation entity is left as an orphaned ghost task in Room DB.
  2. If Part 1 is completed (`baseTaskId.status == TaskStatus.COMPLETED`), `${baseTaskId}_part2` remains active to represent the remaining work. However, `fixedWindows` unconditionally filters out `!it.id.endsWith("_part2")`. Because `baseTaskId` is no longer active and not in `candidateIds`, the continuation's time slot is not treated as a fixed busy window, allowing subsequent auto-scheduling passes to schedule other tasks directly over Part 2.
- **Expected Behavior**:
  Deleting a parent task must cascade-delete its continuation parts. In `fixedWindows`, `_part2` entities should only be excluded if their parent task is currently undergoing re-scheduling in `candidateIds`. If the parent task is already completed, the continuation block must be respected as an immutable fixed window.
- **Resolution**:
  Added cascade deletion of `${task.id}_part2` in `TaskRepository.delete`. Updated `fixedWindows` filtering in both `PlanViewModelV2` and `ScheduleAutoTasksWorker` to only exclude `_part2` if its root parent is actively in `candidateIds`/`targetTasksMap`. Added regression test in `TaskRepositoryRecurringTest`.

---

#### BUG-013: Dead Code and Unhandled Intent Action in `FocusWidgetProvider`
- **Severity**: Low
- **Category**: Code Quality / Dead Code
- **File**: `app/src/main/java/com/neuroflow/app/presentation/widget/FocusWidgetProvider.kt` (lines 38–49)
- **Problem Description**:
  `FocusWidgetProvider.kt` overrides `onReceive` to handle `"com.neuroflow.app.WIDGET_START"`.
  However:
  1. No component produces or broadcasts `"com.neuroflow.app.WIDGET_START"`. Both `FocusWidgetProvider.onUpdate` and `FocusWidgetUpdateWorker` use `action = "com.procus.ACTION_OPEN_FOCUS"`.
  2. The launch intent constructed inside `onReceive` omits setting `action = "com.procus.ACTION_OPEN_FOCUS"`. In `MainActivity.kt`, intent routing strictly checks `when (action)`. Even if this receiver method were triggered, `MainActivity` would not handle the intent.
- **Expected Behavior**:
  Clean, functional code without unused broadcast receiver handlers.
- **Resolution**:
  Aligned `FocusWidgetProvider.onReceive` to handle both `"com.procus.ACTION_OPEN_FOCUS"` and `"com.neuroflow.app.WIDGET_START"`, and explicitly set `action = "com.procus.ACTION_OPEN_FOCUS"` on the launch intent.

---

#### BUG-014: `.gitignore` Lacks Exclusion for Generated Code Bundles (`core_engine_code/`, `ui_code/`)
- **Severity**: Low
- **Category**: Tooling / Version Control Hygiene
- **File**: `.gitignore` (lines 65–72)
- **Problem Description**:
  Commits `937b216`, `2943f94`, `0bbd06c`, and `3e74cbb` deleted 100k+ lines of duplicate markdown and raw code bundles (`core_engine_code/`, `ui_code/`, `NEUROFLOW_ALL_CODE.md`, `VICE_HEAD_ALL_CODE.md`) generated by `scripts/export_core_bundle.py` and `scripts/export_ui_bundle.py`.
  However, `.gitignore` does not include these paths. Running any bundle generation script immediately dirties the git working directory with over 100,000 lines of untracked files.
- **Expected Behavior**:
  Generated bundle directories and master files should be ignored in `.gitignore`.
- **Resolution**:
  Added `core_engine_code/`, `ui_code/`, `NEUROFLOW_ALL_CODE.md`, and `VICE_HEAD_ALL_CODE.md` to `.gitignore`.

---

#### BUG-015: Inaccurate Dependency Block Deferral Rationale in Sweep
- **Severity**: Low
- **Category**: Scheduler Accuracy / Diagnostics
- **File**: `app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt` (lines 944–966)
- **Problem Description**:
  During Step 4 post-placement sweep in `DefaultTaskSchedulerService.kt`, remaining unplaced tasks in `schedulableTodayTasks` have their deferrals generated by finding the first missing prerequisite via:
  ```kotlin
  val missingPrereq = task.prerequisiteIds.firstOrNull { prereqId ->
      placements.none { it.task.id.substringBefore("#part") == prereqId.substringBefore("#part") }
  }
  ```
  However, if a task has multiple prerequisites where one prerequisite was already completed on a prior day (or earlier today before the run), that completed prerequisite was never in today's `placements`.
  If `task.prerequisiteIds.firstOrNull` evaluates the completed prerequisite first, it reports:
  `"Prerequisite task '$missingPrereq' could not be scheduled today."` even though that prerequisite was already completed and satisfied.
- **Expected Behavior**:
  The rationale must point to the actual unsatisfied prerequisite (active in DB but not placed today, or uncompleted cross-day dependency), ignoring already-completed tasks.
- **Suggested Fix**:
  Filter `missingPrereq` by prerequisites that are in `activeUncompletedTaskIds` or in `schedulableTodayIds`, and exclude tasks that are already `COMPLETED` in the database / task repository.

---

#### BUG-016: Chronotype Precedence Inversion in Widget & Notification Workers
- **Severity**: Medium
- **Category**: Chronobiology Consistency
- **File**: `app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt` (line 64), `app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt` (line 162)
- **Problem Description**:
  Throughout the app (`SchedulePreparationOrchestrator.kt:278`, `PlanViewModelV2.kt:259`, `FlowViewModelV2.kt:395`, `SettingsScreen.kt:668`), user chronotype profile is resolved prioritizing the empirical 19-question MEQ quiz over the manual selector:
  ```kotlin
  val chronotype = ChronotypeProfile.fromString(prefs.quizChronotype ?: prefs.manualChronotype)
  ```
  However, `FocusWidgetUpdateWorker.kt:64` and `DailyPlanWorker` in `NotificationWorker.kt:162` invert this precedence:
  ```kotlin
  val chronotype = ChronotypeProfile.fromString(prefs.manualChronotype ?: prefs.quizChronotype)
  ```
  If a user manually selected an archetype and subsequently calibrated via the MEQ quiz, the home screen widget and daily plan notification worker continue using the stale manual chronotype, calculating circadian curves and task scores that diverge from what the user sees inside the application.
- **Expected Behavior**:
  Consistent chronotype resolution across all background workers and UI view models (`quizChronotype ?: manualChronotype`).
- **Resolution**:
  Aligned `FocusWidgetUpdateWorker.kt` and `NotificationWorker.kt` to prioritize `quizChronotype ?: manualChronotype`.

---

#### BUG-017: Redundant Consecutive Room DB Writes on Focus Session Completion
- **Severity**: Low
- **Category**: Performance / Redundant I/O
- **File**: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt` (lines 384–386)
- **Problem Description**:
  In `FocusService.kt` (`handleSessionCompletion`), upon completing a focus block:
  ```kotlin
  taskRepository.update(updatedTask)
  taskRepository.completeAndRecur(updatedTask, now)
  ```
  Inside `TaskRepository.kt:320`, `completeAndRecur` immediately calls `update(...)` on Room DB again:
  ```kotlin
  update(task.copy(status = TaskStatus.COMPLETED, completedAt = now, ...))
  ```
  Executing two consecutive `taskRepository.update` writes within milliseconds causes redundant SQLite disk transactions, redundant database change triggers on `TaskDao`, and duplicate flow emissions to observing UI components.
- **Expected Behavior**:
  Perform a single atomic write via `taskRepository.completeAndRecur(updatedTask, now)`.
- **Resolution**:
  Removed redundant `taskRepository.update(updatedTask)` call in `FocusService.kt`, ensuring a single atomic SQLite write through `completeAndRecur`.

---

#### BUG-018: Date-Only Tasks Resolving to Midnight Trigger Premature Expiration
- **Severity**: Medium
- **Category**: Scheduling Engine / Temporal Logic
- **File**: `app/src/main/java/com/neuroflow/app/data/local/entity/TaskTimeExtensions.kt` (lines 37–39), `app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt` (lines 147–160)
- **Problem Description**:
  In `TaskTimeExtensions.kt`, `resolveScheduledEpochMillis()` implements:
  ```kotlin
  if (scheduledDate != null) {
      return scheduledDate + (rawTime ?: 0L)
  }
  ```
  When a task has a date assigned (`scheduledDate != null`) but no specific time of day (`scheduledTime == null`), it resolves to `scheduledDate + 0L` (local midnight 00:00:00).
  In `ScheduleAutoTasksWorker.kt:154`, `TaskBlockPlan.blockHasExpired` checks:
  `nowMillis > startMillis + duration + gracePeriod`.
  At 9:00 AM on the scheduled date, `nowMillis` is 9 hours past midnight. The date-only task is diagnosed as an "expired" block (`blockHasExpired == true`) and moved into `missedTasks` where its `scheduledDate` is wiped to null (`scheduledDate = null, scheduledTime = null`), rather than recognizing that the user intended to do it sometime today.
- **Expected Behavior**:
  A task with `scheduledDate != null` and `scheduledTime == null` represents an intraday un-timed task, not an event that began and expired at 00:00:00 AM.
- **Resolution**:
  Guarded expiration evaluation in `PlacementStability.kt` and `ScheduleAutoTasksWorker.kt` with `task.scheduledTime != null`. Added regression test in `PlacementStabilityTest`.

---

#### BUG-019: Self-Defeating `buildBlockedTaskIds` Candidate Inclusion Tautology
- **Severity**: Medium
- **Category**: Scheduling Pre-Filter / Logic Bug
- **File**: `app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt` (lines 134, 344–349)
- **Problem Description**:
  In `ScheduleAutoTasksWorker.kt`, candidate task pre-filtering invokes:
  `val blockedTaskIds = buildBlockedTaskIds(allTasks, allKnownTasks)`
  where `allTasks` contains all active tasks in the database.
  Inside `buildBlockedTaskIds`:
  `val candidateIds = tasks.map { it.id }.toSet()` -> this contains every active task in the database.
  Then line 345 evaluates:
  `if (depTask.status == TaskStatus.ACTIVE && depId in candidateIds) return@any false`
  Because `depId` is active, it is in `candidateIds`, so this condition returns `false` (meaning "not blocked") for EVERY active task in the system, even if the dependency is scheduled for next week or next month!
  The function intended to exempt dependencies that are being scheduled in the *current batch*, but because `tasks` passed was the entire active table, the condition became an accidental tautology that stripped all dependency blocking protection during the worker's pre-filter phase.
- **Expected Behavior**:
  Only dependencies that are scheduled or placing on the *same day* (`dayStart..dayEnd`) should bypass the blocked filter. Cross-day future dependencies must correctly mark the dependent task as blocked so it is not ingested today.
- **Resolution**:
  Added `dayEnd: Long` parameter to `buildBlockedTaskIds`. Evaluated `depTask.resolveScheduledEpochMillis()`: if `depStart > dayEnd`, the dependency cannot be completed today and blocks the downstream task.

---

#### BUG-020: Continuation Part 2 Inherits Recurrence Leading to Exponential Task Duplication
- **Severity**: High
- **Category**: Data Integrity / Recurrence Lifecycle & Task Duplication
- **File**: `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt` (lines 540–558), `app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt` (lines 295–315), `app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt` (lines 320–340)
- **Problem Description**:
  When an ultradian split creates a continuation entity `${baseTaskId}_part2`, both `PlanViewModelV2` and `ScheduleAutoTasksWorker` instantiate `part2Entity` via `(existingPart2 ?: fresh).copy(...)` without resetting `recurrence` to `Recurrence.NONE`.
  Consequently, if the root task has a recurring rule (e.g. `Recurrence.DAILY`):
  1. When the user completes Part 1, `TaskRepository.completeAndRecur(task, now)` advances the recurrence cycle and inserts a new active task for tomorrow.
  2. When the user completes Part 2, `TaskRepository.completeAndRecur(part2Entity, now)` ALSO triggers recurrence advancement because `part2Entity.recurrence` is `DAILY`, inserting a *second* new active task for tomorrow!
  Each day, every recurring task that undergoes an ultradian split doubles in quantity ($2^N$), causing exponential task explosion and ghost duplicate tasks in Room DB.
  Additionally, when the root task completes in `completeAndRecur`, its title retains `(Part 1/2)` and `plannedBlockMinutes` remains set to the partial duration (e.g. 40m). Because `TaskBlockPlan.expectedMinutes` reads `plannedBlockMinutes` when `title` contains `(Part `, all subsequent recurring occurrences are permanently stuck with the partial block size and mutated title.
- **Expected Behavior**:
  Continuation entities (`_part2`, `#part2`) must NEVER inherit recurrence rules (`recurrence = Recurrence.NONE`). Completing a continuation part must never trigger recurrence advancement. Completing a root task must clean the title and reset `plannedBlockMinutes` to 0 so the next occurrence uses the full original duration.
- **Resolution**:
  Set `recurrence = Recurrence.NONE`, `recurrenceIntervalDays = 0`, and `isHabitual = false` when instantiating `part2Entity` in `PlanViewModelV2` and `ScheduleAutoTasksWorker`. Added guard in `TaskRepository.completeAndRecur` blocking recurrence advancement for continuation tasks (`id.endsWith("_part2") || id.contains("#part") || parentTaskId != null`). Sanitized title and reset `plannedBlockMinutes = 0` when generating the next recurring instance. Added regression tests in `TaskRepositoryRecurringTest`.

---

#### BUG-021: Bypassed Timestamp Normalization in Widget & Notification Workers Causes Year-2082 & Time-Skew Calculations
- **Severity**: Medium
- **Category**: Data Integrity / Temporal Logic
- **File**: `app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt` (lines 103–104), `app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt` (lines 201–202)
- **Problem Description**:
  In `FocusWidgetUpdateWorker.kt` and `NotificationWorker.kt`, when mapping `TaskEntity` into `SchedulableTask` for circadian scoring:
  ```kotlin
  scheduledStartMillis = task.scheduledTime?.let { (task.scheduledDate ?: nowMillis) + it },
  deadlineMillis = task.deadlineDate?.let { it + (task.deadlineTime ?: 0L) },
  ```
  This implementation suffers from two major temporal bugs:
  1. When `task.scheduledDate` is null, it offsets `task.scheduledTime` from `nowMillis` (current wall clock, e.g. 14:00 PM) rather than the local start of day (00:00:00). For a 10:00 AM task (`scheduledTime = 10 * 3600_000L`), `scheduledStartMillis` evaluates to 14:00 + 10h = midnight tonight (24:00), distorting circadian scoring and widget recommendations.
  2. For recurring tasks or externally anchored tasks, `scheduledTime` or `deadlineTime` may be stored as an absolute epoch timestamp (`>= 86_400_000L`). When this occurs, adding `nowMillis` or `task.deadlineDate` to `it` adds two full epoch timestamps together (~3.52 trillion ms, landing in year 2082) and breaks all priority weight and deadline urgency calculations.
- **Expected Behavior**:
  Both workers must use the canonical extension functions `task.resolveScheduledEpochMillis(nowMillis)` and `task.resolveDeadlineEpochMillis()` which correctly normalize between intraday offsets and epoch millis.
- **Resolution**:
  Replaced ad-hoc timestamp additions with canonical `task.resolveScheduledEpochMillis(nowMillis)` and `task.resolveDeadlineEpochMillis()` in both `FocusWidgetUpdateWorker.kt` and `NotificationWorker.kt`.

---

#### BUG-022: Deleted Frog and Locked Tasks Leave Orphaned Events in Native Android Calendar
- **Severity**: Low
- **Category**: Integration / Calendar Write-Back Lifecycle
- **File**: `app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt` (lines 207–227)
- **Problem Description**:
  Under ADR-006 (bidirectional calendar write-back), tasks marked as `isFrog` or `isScheduleLocked` write synchronized events to Android Native Calendar via `CalendarSyncRepository.upsertTaskEvent`.
  However, when a user deletes a task via `TaskRepository.delete(task: TaskEntity)`, the repository only deletes the entity from Room `taskDao` and updates dependent tasks. It never invokes `calendarSyncRepository?.deleteTaskEvent(task.id)` or `calendarSyncRepository?.handleTaskStateChanged(...)`.
  Consequently, deleting a Frog or locked focus block from inside neuroFlow leaves its calendar event permanently orphaned in the user's Google Calendar or Outlook calendar.
- **Expected Behavior**:
  Deleting a task from the app must trigger calendar write-back cleanup, deleting any associated event previously synced to the device calendar provider.
- **Resolution**:
  Injected `CalendarSyncRepository?` into `TaskRepository` and invoked `calendarSyncRepository?.deleteTaskEvent(task.id)` (and for any continuation part) upon task deletion. Added unit test in `TaskRepositoryRecurringTest`.

---

#### BUG-023: `replanCandidates` Overwrites `missedTasks` Cleared Timestamps in `ScheduleAutoTasksWorker`
- **Severity**: High
- **Category**: Scheduling Engine / State Overwrite & Dead Code
- **File**: `app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt` (lines 170–186)
- **Problem Description**:
  In `ScheduleAutoTasksWorker.kt`, candidate ingestion creates:
  ```kotlin
  val targetTasksMap = LinkedHashMap<String, TaskEntity>()
  unscheduledTasks.forEach { targetTasksMap[it.id] = it }
  missedTasks.forEach { targetTasksMap[it.id] = it.copy(scheduledDate = null, scheduledTime = null) }
  replanCandidates.forEach { targetTasksMap[it.id] = it }
  ```
  An expired task correctly matches `missedTasks` (`TaskBlockPlan.blockHasExpired(...)`). However, it ALSO matches `replanCandidates` because `PlacementStability.verdict` returns `ReplanVerdict.PAST_EXPIRED_RESCUE`, which sets `allowsReplan = true`, and `scheduledMillis in dayStart..dayEnd` holds for today's past expired tasks.
  Because `replanCandidates` did not exclude `missedTasks`, iterating `replanCandidates.forEach { targetTasksMap[it.id] = it }` immediately overwrote `targetTasksMap[it.id]`, clobbering the cleared `scheduledDate = null, scheduledTime = null` and restoring the stale expired timestamps.
  Consequently, `missedTasks.forEach { ... }` was rendered completely dead code, and expired tasks were passed to `SchedulePreparationOrchestrator` with past timestamps, distorting placement priority and candidate DAG anchoring.
- **Expected Behavior**:
  Tasks identified as missed/expired must have their stale past timestamps wiped and preserved in `targetTasksMap` as unscheduled candidate tasks, without being clobbered by `replanCandidates`.
- **Resolution**:
  Extracted `val missedTaskIds = missedTasks.map { it.id }.toSet()` and filtered out `it.id in missedTaskIds` from `replanCandidates`. Added unit test in `ScheduleAutoTasksWorkerTest`.

---

#### BUG-024: Manual Timeline Time Assignment Preserves `isAutoScheduled = true` in `PlanViewModelV2`
- **Severity**: Medium
- **Category**: Data Integrity / User Intent Override
- **File**: `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt` (lines 804–830)
- **Problem Description**:
  In `PlanViewModelV2.kt` (`assignTaskTime` and `rescheduleTask`), when a user manually assigns a task to a timeline time slot or shifts it to a peak slot:
  ```kotlin
  val updatedTask = task.copy(
      scheduledDate = uiState.value.selectedDateMillis,
      scheduledTime = millisInDay,
      isScheduleLocked = true,
      updatedAt = System.currentTimeMillis()
  )
  ```
  `updatedTask` preserved `task.isAutoScheduled`. If the task was previously auto-scheduled, `isAutoScheduled` remained `true`.
  If the user subsequently toggles off the lock switch (`isScheduleLocked = false`), `AutoSchedulingContracts.isMutableByAutoScheduler` evaluates to `true` (`task.isAutoScheduled == true`), treating the user's manual time assignment as an auto-scheduled block and displacing it on subsequent auto-scheduling passes.
- **Expected Behavior**:
  Explicit user time assignments on the timeline (via drag-and-drop or peak shifting) must set `isAutoScheduled = false` to mark them as user-authored manual schedules.
- **Resolution**:
  Set `isAutoScheduled = false` in both `assignTaskTime` and `rescheduleTask` in `PlanViewModelV2.kt`.

---

#### BUG-025: Concurrent Auto-Schedule Race Condition Between UI Coroutine and WorkManager in Plan Screen
- **Severity**: Medium
- **Category**: Concurrency / Race Condition
- **File**: `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt` (lines 583–587)
- **Problem Description**:
  In `PlanViewModelV2.kt`, `triggerAutoScheduleNow()` previously launched `autoScheduleDay()` on `viewModelScope` AND concurrently enqueued `ScheduleAutoTasksWorker` via `workManager.enqueueUniqueWork("AutoScheduleNowV2", ExistingWorkPolicy.REPLACE, request)`.
  Triggering two independent scheduler passes simultaneously on the same database rows caused database race conditions, overlapping time slot allocations, and duplicate continuation task insertions in SQLite.
- **Expected Behavior**:
  Triggering auto-schedule from the UI must run as a single atomic operation without triggering redundant background workers for the same target day.
- **Resolution**:
  Removed redundant WorkManager invocation from `triggerAutoScheduleNow()` and guarded `autoScheduleDay()` with reentrancy check `if (_isAutoScheduling.value) return`.

---

#### BUG-026: `workDayEnd` Midnight Value (0) Inversion in `TaskSchedulerService`
- **Severity**: Low
- **Category**: Temporal Logic / Boundary Edge Case
- **File**: `app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt` (lines 427–435)
- **Problem Description**:
  In `TaskSchedulerService.kt`, `latestDayEnd` was computed via:
  ```kotlin
  dayDate.atTime(LocalTime.of(workDayEndHour.coerceIn(0, 23), 0)).atZone(zoneId).toInstant().toEpochMilli()
  ```
  If `workDayEndHour` was set to `0` (intended as midnight at day's close, 24:00), `LocalTime.of(0, 0)` evaluated to midnight at the START of `dayDate` (00:00:00).
  Because `dayStartMillis` is at least 06:00:00 or `workDayStartHour`, `dayStartMillis >= dayEndMillis` was immediately triggered, resulting in all tasks being deferred with `"Past workday end for this date"` and preventing any task from ever being placed.
- **Expected Behavior**:
  A `workDayEndHour` of 0 must be interpreted as midnight closing the day (`dayDate.plusDays(1).atStartOfDay() - 1ms`), providing a valid end-of-day boundary.
- **Resolution**:
  Updated `TaskSchedulerService.kt` so that `workDayEndHour == 0` resolves to `dayDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1`. Added unit test `testWorkDayEndHourZeroResolvesToEndOfDay` in `TaskSchedulerServiceTest.kt`.

---

#### BUG-027: Background Auto-Scheduler Traps Prior-Day Missed Tasks Due to Day Horizon Boundary Filter
- **Severity**: High
- **Category**: Scheduling Engine / Temporal Leaks & Missing Ingestion
- **File**: `app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt` (lines 182–211), `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt` (lines 446–452)
- **Problem Description**:
  In `ScheduleAutoTasksWorker.kt`, candidate task ingestion partitions active tasks into `unscheduledTasks`, `missedTasks`, and `replanCandidates`.
  To prevent future-dated tasks from being pulled into today (BUG-009), boundary checks were introduced.
  However, the bounds inadvertently trapped active tasks that were scheduled on previous days and missed:
  1. `unscheduledTasks` requires `(sDate == null || task.scheduledTime == null)`. If a task had an explicit scheduled time on a previous day (`task.scheduledDate < dayStart && task.scheduledTime != null`), it fails this check and is excluded from `unscheduledTasks`.
  2. `missedTasks` requires `scheduledStart in dayStart..dayEnd`. An uncompleted task from yesterday has `scheduledStart < dayStart`, so it fails the range check and is excluded from `missedTasks`.
  3. `replanCandidates` requires `scheduledMillis in dayStart..dayEnd`, similarly excluding any prior-day task.
  Likewise, in `PlanViewModelV2.kt`, candidate task filtering enforces `(task.scheduledDate == null || task.scheduledDate in dayStart..dayEnd)`.
  As a consequence, any active task from yesterday or earlier with a scheduled time is completely dropped from auto-scheduling. It is never diagnosed as missed, never has its expired timestamps cleared, and is never scheduled onto today's calendar, leaving overdue tasks permanently stranded in previous days.
- **Root Cause**:
  The boundary constraint implemented to prevent pulling in *future* tasks (`> dayEnd`) erroneously used a closed interval `in dayStart..dayEnd`, inadvertently excluding expired *past* tasks (`< dayStart`).
- **Suggested Fix**:
  In `ScheduleAutoTasksWorker.kt`, update `missedTasks` to accept any expired task up to `dayEnd`:
  ```kotlin
  val missedTasks = allTasks.filter { task ->
      val scheduledStart = task.resolveScheduledEpochMillis()
      task.status == TaskStatus.ACTIVE &&
          !task.isScheduleLocked &&
          task.isAutoScheduled &&
          task.scheduledTime != null &&
          scheduledStart != null &&
          scheduledStart <= dayEnd &&
          TaskBlockPlan.blockHasExpired(task, scheduledStart, nowMillis, gracePeriodMillis)
  }
  ```
  In `PlanViewModelV2.kt`, when auto-scheduling for the current date (`dayDate == nowLocalDate`), allow past active tasks:
  ```kotlin
  val isTargetingToday = dayDate == java.time.Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
  val isDateEligible = task.scheduledDate == null ||
      task.scheduledDate in dayStart..dayEnd ||
      (isTargetingToday && task.scheduledDate <= dayEnd)
  ```

---

#### BUG-028: `PlacementStability.isMoveWorthApplying` Short-Circuit Traps Part 2 Continuations and Title Restorations in Background Worker
- **Severity**: High
- **Category**: Scheduling Engine / State Inconsistency & Ghost Entities
- **File**: `app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt` (lines 307–370)
- **Problem Description**:
  In `ScheduleAutoTasksWorker.kt` (`applyPlacementsTransactionally`), placement application contains:
  ```kotlin
  if (!PlacementStability.isMoveWorthApplying(
          previousStartMillis = previousStartMillis,
          nextStartMillis = primaryPlacement.window.startMillis,
          nowMillis = nowMillis
      ) && !blockLengthChanged
  ) {
      continue
  }
  ```
  If Part 1's placement start time did not shift by $\ge 30$ minutes and its block duration remained identical, line 313 executes `continue` immediately.
  However, the entire synchronization and deletion lifecycle for ultradian split Part 2 (`${baseTaskId}_part2`) is located *after* this `continue` statement.
  This triggers three severe failure states:
  1. **Orphaned Ghost Part 2**: If an 80m task was split into Part 1 and Part 2 on a previous run, but in the current run is no longer split (`isSplit == false`, e.g. due to duration change or open slot availability), line 364 (`taskRepository.delete(orphanedPart2)`) is skipped. `${baseTaskId}_part2` remains orphaned in Room DB.
  2. **Stale Split Title**: Part 1's title is never updated back to `cleanTitle` because `taskRepository.update(updatedTask)` is skipped. The task remains permanently stuck with `(Part 1/2)` in its title.
  3. **Stranded Part 2 Slot Collision**: If Part 1 stayed at 10:00 AM but Part 2 was shifted from 14:00 to 16:00 (e.g. to accommodate an external calendar meeting), Part 2's new schedule is never persisted. Part 2 remains scheduled at 14:00 directly on top of the user's meeting.
- **Root Cause**:
  `PlacementStability.isMoveWorthApplying` only evaluated Part 1's start displacement, completely ignoring Part 2's displacement, continuation deletion requirements, and parent title sanitization.
- **Suggested Fix**:
  Before short-circuiting with `continue`, check if Part 2 requires database synchronization or cleanup, and whether the parent title requires restoration:
  ```kotlin
  val part2Id = "${baseTaskId}_part2"
  val part2Placement = taskPlacements.getOrNull(1)
  val existingPart2 = taskRepository.getById(part2Id)
  val part2NeedsUpdate = if (isSplit && part2Placement != null && part2Placement.task.id.endsWith("#part2")) {
      existingPart2 == null ||
          existingPart2.resolveScheduledEpochMillis() != part2Placement.window.startMillis ||
          existingPart2.plannedBlockMinutes != part2Placement.window.durationMinutes
  } else {
      existingPart2 != null
  }
  val titleChanged = freshTask.title != titleToUse

  if (!part2NeedsUpdate &&
      !titleChanged &&
      !blockLengthChanged &&
      !PlacementStability.isMoveWorthApplying(
          previousStartMillis = previousStartMillis,
          nextStartMillis = primaryPlacement.window.startMillis,
          nowMillis = nowMillis
      )
  ) {
      continue
  }
  ```

---

#### BUG-029: Direct Deletion of Continuation Part 2 Leaves Parent Task Truncated with Permanent Halved Duration & Stale `(Part 1/2)` Title
- **Severity**: Medium
- **Category**: Data Integrity / Continuation Lifecycle
- **File**: `app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt` (lines 207–227)
- **Problem Description**:
  When an ultradian split occurs on an 80m+ task, the parent task is renamed to `"${task.title} (Part 1/2)"` and its `plannedBlockMinutes` is set to half the original duration (e.g. 40m).
  Under BUG-012, cascade deletion was implemented when deleting the parent task.
  However, if a user directly deletes the continuation task Part 2 (`task.id.endsWith("_part2")` or `task.parentTaskId != null` via swipe-to-dismiss or context menu):
  `TaskRepository.delete` deletes Part 2, but leaves the parent task untouched in Room DB.
  The parent task remains active with `title = "... (Part 1/2)"` and `plannedBlockMinutes = 40`.
  Because `TaskBlockPlan.expectedMinutes` inspects `if (task.title.contains("(Part ")) plannedBlockMinutes`, the parent task is permanently truncated to half its original duration and displays an obsolete `(Part 1/2)` badge, even though Part 2 no longer exists.
- **Root Cause**:
  `TaskRepository.delete` lacks an inverse cascade check to restore and clean the active parent task when a child continuation part is deleted.
- **Suggested Fix**:
  In `TaskRepository.delete`, when deleting a task where `task.id.endsWith("_part2") || task.parentTaskId != null`, retrieve the parent task. If the parent exists, is active, and contains `(Part `, sanitize its title and reset `plannedBlockMinutes = 0`:
  ```kotlin
  if (task.id.endsWith("_part2") || task.parentTaskId != null) {
      val parentId = task.parentTaskId ?: task.id.removeSuffix("_part2")
      val parent = taskDao.getById(parentId)
      if (parent != null && parent.status == TaskStatus.ACTIVE && parent.title.contains("(Part ")) {
          val cleanTitle = parent.title.replace(Regex("""\s*\(Part \d+/\d+\)"""), "").trim()
          taskDao.update(parent.copy(title = cleanTitle, plannedBlockMinutes = 0, updatedAt = System.currentTimeMillis()))
      }
  }
  ```

---

#### BUG-030: Midnight Value (`workDayEnd = 0`) Inversion in `ConflictResolver.findConflictFreeStart`
- **Severity**: Low
- **Category**: Temporal Logic / Boundary Edge Case
- **File**: `app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingRationales.kt` (lines 127–139, 198, 240)
- **Problem Description**:
  In `ConflictResolver.findConflictFreeStart`, the end-of-day boundary is calculated as:
  ```kotlin
  val maxMinute = workDayEndHour * 60
  ```
  If the user configures `workDayEnd` to 0 (intended as midnight closing the day, 24:00, as addressed in BUG-026 for `TaskSchedulerService`), `maxMinute` evaluates to 0.
  Because `candidate` is initialized to at least `workDayStartHour * 60` (> 0), the search loop:
  ```kotlin
  while (candidate + duration <= maxMinute)
  ```
  terminates immediately on its very first check ($480 + 30 \le 0$ is false).
  Consequently, `findConflictFreeStart` can never find or suggest a conflict-free slot when `workDayEndHour == 0`, consistently reporting:
  `"No free slot remaining today before 12:00 AM; move to tomorrow."` even when the entire workday is completely empty.
- **Root Cause**:
  `workDayEndHour` of 0 was multiplied directly by 60 without applying the midnight wrap convention (`0` hour = 24:00 = 1440 minutes).
- **Suggested Fix**:
  In `AutoSchedulingRationales.kt`, interpret `workDayEndHour == 0` as 24:00 (1440 minutes):
  ```kotlin
  val effectiveEndHour = if (workDayEndHour == 0) 24 else workDayEndHour
  val maxMinute = effectiveEndHour * 60
  ```
  Similarly update lines 198 and 240 to format `(if (workDayEnd == 0) 24 else workDayEnd) * 60`.

---

#### BUG-031: Reconnecting to Running Focus Session Leaves Soundscape Muted & Overwrites Selected Profile
- **Severity**: Medium
- **Category**: Audio Engine / UI State Desynchronization
- **File**: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt` (lines 68, 169–178, 891–895)
- **Problem Description**:
  When a user starts a focus session with a soundscape (e.g. `THETA_GAMMA_ENCODE`) and navigates away from `FocusScreenV2` (e.g. to review their schedule or check settings), `onCleared()` pauses the soundscape audio track via `soundscapePlayer.pauseWithFadeOut()`, while `FocusService` continues running the session in the foreground.
  When the user returns to `FocusScreenV2` by tapping the mini focus bar or notification, `FocusViewModelV2` is recreated with its default state `selectedSoundscape = SoundscapeType.DEFAULT`.
  In `loadTask()`, the ViewModel detects an ongoing active session (`openSession != null && isRunning`), resumes the countdown timer, and rebinds `FocusService`.
  However, it never calls `soundscapePlayer.play(...)`!
  As a result:
  1. The audio output remains completely silent even though the UI chip indicates a soundscape is active.
  2. If the user had previously selected `SoundscapeType.NONE` (or another profile like `BROWN_NOISE`), recreating the ViewModel overwrites their choice back to `SoundscapeType.DEFAULT`.
- **Root Cause**:
  `loadTask()` omitted audio resumption upon reconnecting to an ongoing session, and `selectedSoundscape` is held only as transient ViewModel state rather than persisted across screen transitions.
- **Suggested Fix**:
  In `loadTask()`, if `isRunning && _uiState.value.selectedSoundscape != SoundscapeType.NONE`, resume audio playback:
  ```kotlin
  if (isRunning) {
      startTicker()
      FocusService.startService(...)
      if (_uiState.value.selectedSoundscape != SoundscapeType.NONE) {
          soundscapePlayer.setVolume(_uiState.value.soundscapeVolume)
          soundscapePlayer.play(_uiState.value.selectedSoundscape)
      }
  }
  ```
  Persist user soundscape choice in `UserPreferences` or session metadata to retain state across navigation.

---

#### BUG-032: Date-Only Intraday Tasks Erroneously Classified as `NEAR_TERM_FREEZE` with `allowsReplan = false`
- **Severity**: Medium
- **Category**: Scheduler Stability / Temporal Logic Bug
- **File**: `app/src/main/java/com/neuroflow/app/domain/scheduler/PlacementStability.kt` (line 63)
- **Problem Description**:
  In `PlacementStability.kt`, line 63 evaluates:
  ```kotlin
  if (scheduledMillis - nowMillis < NEAR_TERM_FREEZE_MILLIS) return ReplanVerdict.NEAR_TERM_FREEZE
  ```
  For an un-timed date-only task (`task.scheduledTime == null`), `task.resolveScheduledEpochMillis()` resolves to local midnight (`task.scheduledDate + 0L`).
  During daylight hours, `scheduledMillis - nowMillis` is negative (e.g. -10 hours), which satisfies `< NEAR_TERM_FREEZE_MILLIS` (2 hours).
  As a consequence, `verdict()` diagnoses the un-timed task as `NEAR_TERM_FREEZE` with `allowsReplan = false`.
  However, the `NEAR_TERM_FREEZE` policy contract is explicitly designed to protect an imminent or ongoing time-of-day block ("A block this close to starting is the user's current plan, not the scheduler's draft"). An un-timed date-only task has no time-of-day block and should not be frozen in near-term.
- **Root Cause**:
  The freeze check checked `scheduledMillis - nowMillis < NEAR_TERM_FREEZE_MILLIS` without guarding `task.scheduledTime != null`, causing midnight date-only timestamps to satisfy the condition.
- **Suggested Fix**:
  In `PlacementStability.kt`, ensure `NEAR_TERM_FREEZE` only applies to tasks with an explicit scheduled time of day:
  ```kotlin
  if (task.scheduledTime != null && scheduledMillis - nowMillis < NEAR_TERM_FREEZE_MILLIS) return ReplanVerdict.NEAR_TERM_FREEZE
  ```

---

#### BUG-033: Omission of `MONAURAL_GAMMA_40HZ` and `BETA_16HZ` from 500k-Frame PCM Clipping DSP Stress Test
- **Severity**: Low
- **Category**: Unit Testing / Test Coverage Regression
- **File**: `app/src/test/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapeDspStressTest.kt` (lines 39–46)
- **Problem Description**:
  In `NeuralSoundscapeDspStressTest.kt`, `testBufferOverflowAndClipping_500kFrames_allSoundscapeTypes` verifies that PCM samples strictly lie within `[-32768, 32767]` without clipping or arithmetic overflow across 500,000 frames.
  The test hardcodes a list of 5 types (`NONE`, `GAMMA_40HZ`, `BROWN_NOISE`, `PINK_NOISE`, `THETA_GAMMA_ENCODE`), omitting the newly added `MONAURAL_GAMMA_40HZ` and `BETA_16HZ` enum profiles.
- **Root Cause**:
  Hardcoded enum list in unit test was not updated when `MONAURAL_GAMMA_40HZ` and `BETA_16HZ` profiles were introduced.
- **Suggested Fix**:
  In `NeuralSoundscapeDspStressTest.kt`, iterate over `SoundscapeType.entries` instead of a hardcoded list:
  ```kotlin
  val soundscapeTypes = SoundscapeType.entries
  ```

---

#### BUG-034: Redundant and Inconsistent Calendar Allocations vs Canonical `startOfLocalDay` in `PlanViewModelV2`
- **Severity**: Low
- **Category**: Code Quality / Redundant Code & Consistency
- **File**: `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt` (lines 505–512, 524–531)
- **Problem Description**:
  In `PlanViewModelV2.kt`, after scheduling placements, lines 505–512 and lines 524–531 instantiate new `java.util.Calendar` objects per placement with manual zeroing of hour/minute/second/millis fields to split epoch millis into date and intraday time offsets:
  ```kotlin
  val calSlot = Calendar.getInstance().apply { timeInMillis = primaryPlacement.window.startMillis }
  val startOfDay = calSlot.apply {
      set(Calendar.HOUR_OF_DAY, 0)
      ...
  }.timeInMillis
  val timeOffset = primaryPlacement.window.startMillis - startOfDay
  ```
  In contrast, `ScheduleAutoTasksWorker.kt` and `TaskTimeExtensions.kt` already define canonical, optimized, and DST-safe utilities `startOfLocalDay(millis)` and `splitMillisToDateAndTime(millis)`.
- **Root Cause**:
  Duplicate boilerplate `Calendar` manipulation retained in `PlanViewModelV2.kt` instead of reusing shared time helpers.
- **Suggested Fix**:
  Refactor `PlanViewModelV2.kt` to reuse `startOfLocalDay(primaryPlacement.window.startMillis)`:
  ```kotlin
  val startOfDay = startOfLocalDay(primaryPlacement.window.startMillis)
  val timeOffset = primaryPlacement.window.startMillis - startOfDay
  ```

---

#### BUG-215: Starter Tags Re-injected Unconditionally on Every Read and Save in `UserPreferencesDataStore.kt` Preventing Tag Deletion
- **Severity**: High
- **Category**: Core Data Integrity / Tag Catalog Lifecycle & Deletion Block
- **File**: [`UserPreferencesDataStore.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/UserPreferencesDataStore.kt#L374), [`UserPreferencesDataStore.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/UserPreferencesDataStore.kt#L715)
- **Problem Description**:
  In `SettingsScreen.kt`, users can view all task tags in the "Task Tags Catalog" and tap 'X' to remove any tag. However, `UserPreferencesDataStore.kt` invoked `withStarterTagCatalog(updated.tagCatalog)` unconditionally on saving to DataStore (line 715), on every read (lines 374, 584), and inside `mergeTags` (line 913).
  `withStarterTagCatalog` loops through all 32 starter tags (`study`, `chores`, `physical`, `gym`, `mindfulness`, etc.) and re-adds them if missing. As a result, when a user deleted ANY starter tag, it was immediately re-injected. Starter tags were completely undeletable.
- **Root Cause**:
  Applying `withStarterTagCatalog` on every DataStore mutation and read, rather than only defaulting to `starterTags` when `prefs[Keys.TAG_CATALOG]` is uninitialized on first launch.
- **Suggested / Applied Fix**:
  1. Updated `UserPreferencesDataStore.tagCatalog` reading to check `if (prefs.contains(Keys.TAG_CATALOG)) parseAffirmations(...) else TaskTagSchedulingProfile.starterTags`.
  2. In `updatePreferences`, directly encoded `updated.tagCatalog` without re-wrapping with `withStarterTagCatalog`.
  3. Removed `withStarterTagCatalog` from `mergeTags`.
  4. Verified in [`TaskTagAndCategoryTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/common/TaskTagAndCategoryTest.kt).

---

#### BUG-216: Hash/At Prefix Omission in Tag Normalization Causes Inconsistent Tag Names, `##tag` Rendering, and Profile Lookup Failures
- **Severity**: Medium
- **Category**: UI & Data Integrity / Tag Normalization & Profile Resolution
- **File**: [`TaskTagSchedulingProfile.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/TaskTagSchedulingProfile.kt#L122), [`TaskTagViewModel.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/common/TaskTagViewModel.kt#L34-L50), [`SmartTaskComposerSheetV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L127-L178)
- **Problem Description**:
  1. In `SettingsScreen.kt`, the input placeholder prompts `"Add tag (e.g. #deepwork)"`. When users followed this instruction, `TaskTagViewModel.normalizeTags` and `UserPreferencesDataStore.mergeTagCatalog` only trimmed whitespace, preserving leading `#` or `@`.
  2. When saved as `"#deepwork"`, `SmartTaskComposerSheetV2.kt` rendered `"#$tag ✕"` as `"##deepwork ✕"`.
  3. `toggleTag` and `removeTag` evaluated `Regex("""(?i)(?:^|\s)[#@]\Q$tagName\E(?=\s|$)""")`, which looked for `[#@]#deepwork` (`##deepwork`), failing to match and strip `#deepwork` from the input title.
  4. In `TaskTagSchedulingProfile.kt`, `normalize(tag)` only executed `trimStart('@')`, failing to strip `#`. When `SchedulePreparationOrchestrator.kt` looked up `TaskTagSchedulingProfile.profilesFor(task.tags)` for `#`-prefixed tags, lookups returned null, discarding fragmentation tolerance (falling back from 0.20 to 0.50 default) and preferred context.
  5. In `HistoryViewModel.kt`, duplicate tags (`"deepwork"` and `"#deepwork"`) appeared in filter chips, and selecting one failed to match tasks with the other.
- **Root Cause**:
  Lack of uniform stripping of `#` and `@` prefixes across tag normalization and profile resolution.
- **Suggested / Applied Fix**:
  1. Updated `TaskTagSchedulingProfile.normalize`, `determineCategory`, and `isNonWorkOrErrandTask` to `trimStart('@', '#')`.
  2. Updated `TaskTagViewModel.normalizeTags`, `removeTag`, and `UserPreferencesDataStore.mergeTagCatalog`/`removeTagFromCatalog` to strip `#` and `@`.
  3. Updated `SmartTaskComposerSheetV2.kt` `initialTags`, `allActiveTags`, `toggleTag`, `removeTag`, and suggestion chips to strip leading `#` and `@`.
  4. Updated `HistoryViewModel.kt` to normalize catalog and task tags.
  5. Verified in [`TaskTagAndCategoryTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/common/TaskTagAndCategoryTest.kt).

---

#### BUG-217: Hardcoded Tag Filter Bar and Missing Catalog Invalidation on `PlanScreenV2` via `PlanViewModelV2.kt`
- **Severity**: Medium
- **Category**: UI & State Management / Tag Filter Bar Desynchronization
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L99), [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L208-L224), [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L383-L409)
- **Problem Description**:
  `PlanUiStateV2` declared a default `availableTags = listOf("All", "#deepwork", "#offline", "#admin", "#quickwin")`. However, `PlanViewModelV2.kt` never passed `availableTags` when constructing `PlanUiStateV2`, leaving it permanently fixed to those 5 hardcoded items.
  Furthermore, `PlanViewModelV2.kt`'s `distinctUntilChanged` on `preferencesFlow` omitted `old.tagCatalog == new.tagCatalog`. As a result, custom tags added in Settings (`TaskTagViewModel`), catalog updates, or tags on tasks never appeared in `PlanScreenV2`'s filter chip bar, preventing users from filtering their daily plan by custom tags.
- **Root Cause**:
  Omission of dynamic tag merging and `availableTags` assignment in `PlanViewModelV2.kt`, and missing `tagCatalog` check in `distinctUntilChanged`.
- **Suggested / Applied Fix**:
  1. Added `old.tagCatalog == new.tagCatalog` to `preferencesFlow.distinctUntilChanged` in `PlanViewModelV2.kt`.
  2. Dynamically merged `defaultTags`, `prefs.tagCatalog`, and `baseTasks.tags` into `mergedTags`.
  3. Passed `availableTags = mergedTags` when instantiating `PlanUiStateV2`.
  4. Updated `confirmDeleteTask` and `onCleared` in `PlanViewModelV2.kt` to record `task.determineCategory().name` for ADHD avoidance telemetry.

---

#### BUG-218: Naive Substring Tag Matching and Unbounded Word Title Ingestion Distorts Filtered Tasks in `PlanViewModelV2.kt`, `FlowViewModelV2.kt`, and `HistoryViewModel.kt`
- **Severity**: Medium
- **Category**: UI & Task Filtering / Substring False Positives & Title Leak
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L302-L304), [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L424-L427), [`HistoryViewModel.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/history/HistoryViewModel.kt#L89-L91)
- **Problem Description**:
  In `PlanViewModelV2.kt` and `FlowViewModelV2.kt`, tag filtering executed:
  `baseTasks.filter { it.tags.contains(q, ignoreCase = true) || it.title.contains(q, ignoreCase = true) }`.
  1. Calling `it.tags.contains(q)` performed raw substring matching on comma-separated tags. Filtering by `#out` matched tasks with `tags = "routine"` (`r-out-ine`) or `"workout"`. Filtering by `#ad` matched `#admin`, but also `#read` or `#cadence`. Filtering by `#ing` matched all continuous actions (`coding`, `writing`, `planning`).
  2. Calling `it.title.contains(q)` performed unbounded substring matching on the task title. Filtering by `#art` matched tasks with `"Smart thermostat"`, `"Start engine"`, and `"Part 1"`. Filtering by `#read` matched `"Already completed"`, `"Thread spool"`, and `"Spreadsheet calculation"`.
- **Root Cause**:
  Using un-tokenized `String.contains()` rather than exact token equality on `task.tags.split(",")` and whole-word regex matching on titles.
- **Suggested / Applied Fix**:
  1. Split `task.tags` by comma and checked exact token equality: `task.tags.split(",").map { it.trim().removePrefix("#").removePrefix("@") }.any { it.equals(q, ignoreCase = true) }`.
  2. Constrained title matching to whole words/hashtags using `Regex("""(?i)(?:^|\s)[#@]?\Q$q\E(?=\s|$)""")`.
  3. Verified across test cases in [`TaskTagAndCategoryTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/common/TaskTagAndCategoryTest.kt).

---

#### BUG-219: `play(SoundscapeType.NONE)` Truncates Volume Before Fade-Out Producing Speaker Pop
- **Severity**: High
- **Category**: Audio DSP / Playback Lifecycle & Volume Mismatch
- **File**: [`NeuralSoundscapePlayer.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapePlayer.kt#L187)
- **Problem Description**:
  When `play(SoundscapeType.NONE)` was called during active soundscape playback, `currentType = SoundscapeType.NONE` was assigned immediately prior to calling `pauseWithFadeOut()`. In `generateFrames`, `type == SoundscapeType.NONE` immediately zeroed output buffers. Consequently, PCM amplitude dropped abruptly from ~26,000 to 0 on frame 0, producing an audible pop and rendering the 400ms fade-out useless on silence.
- **Root Cause**:
  Premature state transition to `NONE` before initiating the raised-cosine fade-out envelope.
- **Suggested / Applied Fix**:
  Preserved the active soundscape type during the fade-out by passing `targetTypeOnComplete = SoundscapeType.NONE` to `pauseWithFadeOut`.

---

#### BUG-220: `AudioTrack.flush()` Immediately After Raised-Cosine Fade-Out Purges Hardware Ring Buffer Producing Audible Click
- **Severity**: Medium
- **Category**: Audio DSP / AudioTrack Flush Discontinuity
- **File**: [`NeuralSoundscapePlayer.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapePlayer.kt#L236)
- **Problem Description**:
  On completing the 400ms fade-out, `track.flush()` was invoked immediately after writing the final zero-amplitude chunk. Flushing purged Android's hardware audio buffer containing the final ~46-100ms of rendered samples, abruptly truncating the smooth envelope tail and causing an audible click.
- **Root Cause**:
  Unnecessary hardware buffer purging on clean fade-outs.
- **Suggested / Applied Fix**:
  Removed `track.flush()` from the fade-out completion block, allowing hardware ring buffers to naturally play out to zero.

---

#### BUG-221: `FocusViewModelV2.onCleared()` Omits Native `AudioTrack` Release Causing Resource Exhaustion on Navigation
- **Severity**: High
- **Category**: Native Resource Management / AudioTrack Leak
- **File**: [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L986)
- **Problem Description**:
  `FocusViewModelV2.onCleared()` invoked `pauseWithFadeOut()` but omitted releasing the native `AudioTrack` when navigating away from an active session. Because ViewModel instances are recreated upon returning to `FocusScreenV2`, each navigation cycle permanently leaked a native `AudioTrack` instance, eventually exhausting Android's global track limit (~32 tracks) and crashing audio services.
- **Root Cause**:
  Missing native disposal path on ViewModel teardown during active playback.
- **Suggested / Applied Fix**:
  Introduced `releaseWithFadeOut()` in `NeuralSoundscapePlayer` which smoothly fades out active audio and disposes `AudioTrack` upon completion, invoked in `FocusViewModelV2.onCleared()`.

---

#### BUG-222: Streaming Soundscape Preset Switching Without Chunk Crossfade Causes Waveform Discontinuity (>25,000 PCM Units)
- **Severity**: Medium
- **Category**: Audio DSP / Preset Crossfading
- **File**: [`NeuralSoundscapePlayer.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapePlayer.kt#L280)
- **Problem Description**:
  Switching soundscape presets while streaming instantly swapped synthesis enums on chunk boundaries without interpolation. Because phase accumulators and filter states are uncoupled across presets, instantaneous delta spikes exceeded 25,000 PCM units, producing audible pops.
- **Root Cause**:
  Abrupt synthesis engine switching without cross-buffer blending.
- **Suggested / Applied Fix**:
  Implemented single-chunk (1024 frames = ~23ms) linear crossfading between outgoing and incoming presets during active streaming transitions.

---

#### BUG-223: Timer Mode & Duration Lost Across Session Recovery in `FocusViewModelV2.kt` Causing Premature Sprint Expiration
- **Severity**: High
- **Category**: State Recovery / Focus Timer Mode Persistence
- **File**: [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L140-L175), [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L400-L435)
- **Problem Description**:
  When a user started a timer in a non-default mode (`JUST_5_MIN` = 300s, `ULTRADIAN_90` = 5400s, or `FLOW_STOPWATCH` = 0s), `startTimer()` inserted a `TimeSessionEntity` with default `sessionType = "MANUAL"` instead of recording `timerMode.name`.
  Upon leaving and returning to `FocusScreenV2`, `FocusViewModelV2` was recreated with default `timerMode = FocusTimerModeV2.POMODORO_25` (1500s).
  In `loadTask()`, `FocusTimingCalculator.calculate()` evaluated expiration against `1500L`. For a 90-minute ultradian sprint, after only 25 minutes, `isExpired` evaluated to `true`, prematurely terminating the session, halting soundscapes, closing the DB session, and displaying the sprint completion sheet. For stopwatch mode, it forced countdown expiration.
- **Root Cause**:
  Failure to persist and recover `timerMode.name` in `TimeSessionEntity.sessionType`.
- **Suggested / Applied Fix**:
  1. Updated `startTimer()` to persist `sessionType = _uiState.value.timerMode.name`.
  2. Updated `loadTask()` to recover `val recoveredMode = FocusTimerModeV2.entries.firstOrNull { it.name == openSession.sessionType } ?: _uiState.value.timerMode` and update `_uiState`.
  3. Evaluated expiration and passed duration to `FocusService` based on `recoveredMode.durationSeconds`.
  4. Verified in [`FocusSessionRecoveryTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusSessionRecoveryTest.kt).

---

#### BUG-224: `FocusService.kt` Drops `accumulatedPauseMillis` on Service Reconnection Causing Notification Time Drift
- **Severity**: High
- **Category**: Background Service / Pause State Recovery
- **File**: [`FocusService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L74-L95), [`FocusService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L476-L515)
- **Problem Description**:
  When `FocusService.startService` was invoked from `loadTask()` to reconnect to an active session, only `taskId`, `taskTitle`, `durationSeconds`, and `startedAtMillis` were passed. If `FocusService` was restarted by Android, `startedAtMillis` in the fresh service instance was `0L`.
  `isSameTaskAndSession` evaluated to `false`, causing `FocusService` to reset `accumulatedPauseMillis = 0L`, `pausedAtMillis = null`, and `isPaused = false`, discarding all previous pause durations stored in Room. The notification drifted immediately by the full paused duration (e.g. showing 15 minutes elapsed instead of 5 minutes). Additionally, `handleCompleteAndStop` credited users with false focus minutes.
- **Root Cause**:
  Missing pause state extras (`EXTRA_ACCUMULATED_PAUSE_MILLIS`, `EXTRA_PAUSED_AT_MILLIS`, `EXTRA_IS_PAUSED`) in `FocusService`.
- **Suggested / Applied Fix**:
  1. Added `EXTRA_ACCUMULATED_PAUSE_MILLIS`, `EXTRA_PAUSED_AT_MILLIS`, and `EXTRA_IS_PAUSED` to `FocusService.kt`.
  2. Retained accumulated pause durations and paused flags across service restarts.
  3. In `handleCompleteAndStop`, ensured `effectiveAccPause = maxOf(accumulatedPauseMillis, openSession?.totalPausedMs ?: 0L)`.
  4. Detached the foreground notification using `stopForeground(STOP_FOREGROUND_DETACH)` on expiration to prevent Android 14 FGS policy violations.

---

#### BUG-225: `FocusViewModelV2.keepGoing()` Reuses Closed Session Primary Key Causing SQLite Replace Overwrite & History Data Loss
- **Severity**: Critical
- **Category**: Data Integrity / Room Session Key Collision
- **File**: [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L394-L415), [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L782)
- **Problem Description**:
  When a sprint completed (`isSprintCompleted = true`), the session was closed in Room with `endedAt = now`. When the user tapped "Keep Going", `keepGoing()` updated `_uiState` with `isSprintCompleted = false` and called `startTimer()`.
  Inside `startTimer()`:
  `val isFreshOrFinished = _uiState.value.isSprintCompleted || _uiState.value.isCompleted || _uiState.value.activeSessionId == null`
  Because `isSprintCompleted` had just been set to `false`, `isFreshOrFinished` evaluated to `false`, causing `startTimer()` to reuse `activeSessionId` from the PREVIOUSLY FINISHED session!
  Room executed `@Insert(onConflict = OnConflictStrategy.REPLACE)` with the same primary key, overwriting and permanently erasing the completed session record and its tracked minutes.
- **Root Cause**:
  Clearing `isSprintCompleted` before `startTimer()` without resetting `activeSessionId = null`.
- **Suggested / Applied Fix**:
  1. Updated `keepGoing()` to explicitly reset `activeSessionId = null`.
  2. In `startTimer()`, always generate a fresh `val sessionId = UUID.randomUUID().toString()`.
  3. Verified two distinct session entities with distinct UUIDs are created in [`FocusSessionRecoveryTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusSessionRecoveryTest.kt).

---

#### BUG-226: `FocusViewModelV2.kt` Auto-Tracker Fires on Already-Completed Tasks Creating Orphaned Time Sessions
- **Severity**: Medium
- **Category**: UI & Background Services / Auto-Tracker Guard
- **File**: [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L250-L260), [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L293)
- **Problem Description**:
  In `loadTask()`, when `openSession == null` and `task.status == TaskStatus.COMPLETED`, `_uiState.isCompleted` remained `false`.
  `checkAutoTracker()` only checked:
  `if (!prefs.autoTrackerEnabled || autoTrackerTriggered || _uiState.value.isRunning || _uiState.value.isPaused) return`
  omitting checks for completed tasks.
  When a user opened a completed task with `autoTrackerEnabled == true`, the 8-second delay timer fired `startTimer()`, creating an open focus session in Room for a completed task and spinning up `FocusService`.
- **Root Cause**:
  Omission of `task.status == TaskStatus.COMPLETED` and `isCompleted` guards in `loadTask()`, `checkAutoTracker()`, and `startTimer()`.
- **Suggested / Applied Fix**:
  1. In `loadTask()`, set `isCompleted = (task?.status == TaskStatus.COMPLETED)`.
  2. In `checkAutoTracker()`, added guards for `_uiState.value.isCompleted || _uiState.value.task?.status == TaskStatus.COMPLETED`.
  3. In `startTimer()`, added guard `if (_uiState.value.isCompleted || _uiState.value.task?.status == TaskStatus.COMPLETED) return`.
  4. Verified in [`FocusSessionRecoveryTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusSessionRecoveryTest.kt).

---

#### BUG-227: `TimeSessionDao.getOpenSessionForTask` Missing `ORDER BY startedAt DESC` Returning Arbitrary Stale Session
- **Severity**: Medium
- **Category**: Room Database / Query Determinism
- **File**: [`TimeSessionDao.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/TimeSessionDao.kt#L24)
- **Problem Description**:
  The Room DAO query was declared as:
  `@Query("SELECT * FROM time_sessions WHERE taskId = :taskId AND endedAt IS NULL LIMIT 1")`
  omitting `ORDER BY startedAt DESC`. If multiple unclosed rows existed (e.g. from an OS force-kill or crash), SQLite returned an arbitrary row, often a stale session from days ago with an ancient `startedAt` timestamp, skewing elapsed calculation.
- **Root Cause**:
  Missing `ORDER BY startedAt DESC` clause in DAO query.
- **Suggested / Applied Fix**:
  Updated query to:
  `@Query("SELECT * FROM time_sessions WHERE taskId = :taskId AND endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")`.

---

#### BUG-228: Protected Time Slot End-Time Integer Overflow Past Midnight (24:xx / 25:xx Clock Display)
- **Severity**: Medium
- **Category**: UI & Boundary Handling / Time Formatting
- **File**: [`PlanScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L1327-L1335), [`PlanScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L1740-L1748)
- **Problem Description**:
  In both `TimelineScheduleView` and `AutopilotConfigView`, the end time of a `ProtectedTimeSlot` was computed as:
  ```kotlin
  val endH = (slot.startMinuteOfDay + slot.durationMinutes) / 60
  val endM = (slot.startMinuteOfDay + slot.durationMinutes) % 60
  Text(text = String.format("%02d:%02d – %02d:%02d (%s)", startH, startM, endH, endM, slot.category.name))
  ```
  Protected slots support start times up to 1439 (23:59) and durations up to 360 minutes (6 hours). When a protected sleep, rest, or wind-down block ends at or past midnight (e.g. 22:30 with 120m duration = 1470m, or 23:00 with 60m duration = 1440m), `endH` evaluated to `24`, `25`, or higher.
  This resulted in invalid clock times rendered to the user such as `"22:30 – 24:30 (BREAK)"` or `"23:00 – 24:00 (BREAK)"`.
- **Root Cause**:
  Missing 1440-minute modulo wraparound when projecting the ending hour of multi-hour protected time slots.
- **Suggested / Applied Fix**:
  Wrapped the end minute in a 1440-minute (24-hour) modulo normalization in both `TimelineScheduleView` and `AutopilotConfigView`:
  ```kotlin
  val endMinuteOfDay = (slot.startMinuteOfDay + slot.durationMinutes) % 1440
  val endH = endMinuteOfDay / 60
  val endM = endMinuteOfDay % 60
  ```
  Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).

---

#### BUG-229: Dynamic Circadian Peak Profile Ignored in `PlanScreenV2` (Hardcoded Fallback to Static Preference)
- **Severity**: Low
- **Category**: UI & State Synchronization / Circadian Alignment
- **File**: [`PlanScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L853)
- **Problem Description**:
  In `TimelineScheduleView`, the peak biological window was extracted exclusively from raw static preferences:
  ```kotlin
  val peakStart = state.preferences.peakEnergyStart.coerceIn(0, 23)
  val peakEnd = state.preferences.peakEnergyEnd.coerceIn(0, 23)
  ```
  `UserPreferences` dynamically tracks the user's calibrated biological peak through `effectivePeakStart` and `effectivePeakEnd` (which lerps manual settings with detected peaks from chronotype quizzes and sleep telemetry).
  `PlanScreenV2` ignored `effectivePeakStart` / `effectivePeakEnd`, displaying static default (9..12) hours even when circadian detection had calibrated a custom peak for the user (e.g. 14..17 for evening chronotypes).
- **Root Cause**:
  Omitting check for `state.preferences.effectivePeakStart != -1` and `effectivePeakEnd != -1` before falling back to manual default preferences.
- **Suggested / Applied Fix**:
  Updated peak start/end resolution:
  ```kotlin
  val peakStart = (if (state.preferences.effectivePeakStart != -1) state.preferences.effectivePeakStart else state.preferences.peakEnergyStart).coerceIn(0, 23)
  val peakEnd = (if (state.preferences.effectivePeakEnd != -1) state.preferences.effectivePeakEnd else state.preferences.peakEnergyEnd).coerceIn(0, 23)
  ```
  Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).

---

#### BUG-230: Stale Task Snapshot in `rememberSwipeToDismissBoxState` Recomposition Closure Overwrites Edits on Dismiss
- **Severity**: Medium
- **Category**: Runtime / State Synchronization
- **File**: [`PlanScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L971-L986)
- **Problem Description**:
  In `TimelineScheduleView`, `itemsIndexed(state.tasksForDay, key = { _, it -> it.id })` instantiated `rememberSwipeToDismissBoxState` whose `confirmValueChange` lambda closed directly over `task`.
  Because `rememberSwipeToDismissBoxState` uses `rememberSaveable` without tracking entity mutation inputs, modifying task details (title, estimate, tags, energy level) in the composer sheet and subsequently swiping to complete passed the stale, pre-edit `TaskEntity` snapshot to `onCompleteTask(task)`.
  `completeAndRecur(task, now)` then wrote the stale snapshot back into Room, silently discarding the user's edits.
- **Root Cause**:
  Direct capture of mutable item state inside an un-updated `remember` lambda rather than leveraging `rememberUpdatedState`.
- **Suggested / Applied Fix**:
  Wrapped the task reference in `val currentTask by rememberUpdatedState(task)` and routed swipe dismiss callbacks through `currentTask`.

---

#### BUG-231: `task.resolveScheduledEpochMillis()` in `PlanScreenV2` Energy Badge Defaults to Current Day Instead of `selectedDateMillis`
- **Severity**: Medium
- **Category**: UI & Logic / Multi-Day Timeline Alignment
- **File**: [`PlanScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L1149)
- **Problem Description**:
  When calculating task slot energy compatibility (`TaskSlotEnergyFitBadge`), the scheduled timestamp was resolved using:
  ```kotlin
  val scheduledMillis = if (task.scheduledTime != null) {
      task.resolveScheduledEpochMillis() ?: (task.scheduledDate?.let { it + (task.scheduledTime ?: 0L) })
  } else null
  ```
  For tasks scheduled without an explicit `scheduledDate` (e.g. deadline-anchored or intraday-timed tasks), `resolveScheduledEpochMillis()` defaults `referenceDateMillis = System.currentTimeMillis()` (today).
  When the user browsed future days (e.g. tomorrow or next Monday) in the date carousel, `scheduledMillis` resolved to today while `state.energyTimeline` projected the future day. `TaskSlotEnergyFitBadge` then compared timestamps across a 24-hour+ delta, yielding completely mismatched circadian energy fit percentages.
- **Root Cause**:
  Omitting `state.selectedDateMillis` as the `referenceDateMillis` parameter in `resolveScheduledEpochMillis()`.
- **Suggested / Applied Fix**:
  Explicitly supplied `state.selectedDateMillis`:
  `task.resolveScheduledEpochMillis(state.selectedDateMillis) ?: ...`
  Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).

---

#### BUG-232: Ineffective Task Inference Calibration in `SmartTaskComposerSheetV2` (Always-Empty Completed Tasks Passed From `PlanScreenV2`)
- **Severity**: Medium
- **Category**: Core Logic / Task Duration Calibration
- **File**: [`PlanScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L671), [`PlanScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L685), [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L167)
- **Problem Description**:
  `PlanScreenV2` passed `completedTasks = state.tasksForDay.filter { it.completedAt != null }` to `SmartTaskComposerSheetV2`.
  However, `state.tasksForDay` is queried exclusively from `taskRepository.observeActiveTasks()`, which filters `status = 'ACTIVE' AND completedAt IS NULL`.
  Consequently, `completedTasks` was permanently empty (`emptyList()`), disabling `TaskInferenceEngine.inferFromInput(rawInput, completedTasks)` from ever utilizing historical completion times to calibrate duration and effort estimates.
- **Root Cause**:
  Attempting to filter completed tasks out of a collection that only contains active tasks.
- **Suggested / Applied Fix**:
  Exposed `val completedTasks: StateFlow<List<TaskEntity>> = taskRepository.observeCompletedTasks().stateIn(...)` in `PlanViewModelV2` and wired it directly to `SmartTaskComposerSheetV2` in `PlanScreenV2`.

---

#### BUG-233: Stale Task Snapshot in `rememberSwipeToDismissBoxState` Recomposition Closure Overwrites Edits on Dismiss in `EnergyTimelineStream.kt`
- **Severity**: Medium
- **Category**: Runtime / State Synchronization
- **File**: [`EnergyTimelineStream.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt#L181), [`EnergyTimelineStream.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt#L249)
- **Problem Description**:
  In `EnergyTimelineStream`, `TimelineTaskCard` instantiated `rememberSwipeToDismissBoxState` whose `confirmValueChange` lambda closed over `onComplete` and `onDefer`.
  Because `rememberSwipeToDismissBoxState` uses `rememberSaveable` without tracking entity mutation inputs, modifying task details (title, estimate, tags, energy level) in the composer sheet and subsequently swiping to complete/defer passed the stale, pre-edit `TaskEntity` snapshot to `onCompleteTask(task)`.
  `completeAndRecur(task, now)` then wrote the stale snapshot back into Room, silently discarding the user's edits.
- **Root Cause**:
  Direct capture of mutable item state inside an un-updated `remember` lambda rather than leveraging `rememberUpdatedState`.
- **Suggested / Applied Fix**:
  Wrapped `task` in `val currentTask by rememberUpdatedState(task)` and routed swipe dismiss callbacks through `val currentOnComplete by rememberUpdatedState(onComplete)` and `val currentOnDefer by rememberUpdatedState(onDefer)`.
  Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).

---

#### BUG-234: Future / Icebox Task Leakage in `FlowSpotlightHeroCard` When Today's Tasks Are Completed or Filtered
- **Severity**: High
- **Category**: UI & State Synchronization / Task Filtering Leakage
- **File**: [`FlowScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt#L243)
- **Problem Description**:
  In `FlowScreenV2`, `topTask` was evaluated as:
  ```kotlin
  val topTask = state.recommendedTask ?: state.activeTasks.firstOrNull { it.isFrog } ?: state.activeTasks.firstOrNull()
  ```
  When all of today's tasks were completed or there were no actionable tasks scheduled for today, `state.recommendedTask` evaluated to `null`.
  The unconditional fallback `state.activeTasks.firstOrNull()` retrieved tasks from future dates (e.g. next week or next month) or tasks residing in `Quadrant.ELIMINATE` (icebox) from the raw database active task pool, displaying them in the Spotlight Hero Card as "⚡ OPTIMAL NEXT ACTION" for today. This directly contradicted the "🧘 Quiet Waters: No tasks scheduled for today" empty-state card displayed immediately below.
  Furthermore, selecting a tag filter (e.g. `#admin`) did not update `topTask`, causing the Spotlight Hero Card to display a global `#deepwork` task while the stream only showed `#admin` tasks.
- **Root Cause**:
  Unconditional fallback to raw, un-filtered `state.activeTasks` rather than synchronizing against `state.filteredTasks` and `state.selectedTag`.
- **Suggested / Applied Fix**:
  Resolved `topTask` against `state.filteredTasks` and `state.selectedTag`:
  ```kotlin
  val topTask = remember(state.recommendedTask, state.filteredTasks, state.selectedTag) {
      if (state.selectedTag.isNullOrBlank() || state.selectedTag.equals("All", ignoreCase = true)) {
          state.recommendedTask ?: state.filteredTasks.firstOrNull { it.isFrog } ?: state.filteredTasks.firstOrNull()
      } else {
          state.filteredTasks.firstOrNull { it.id == state.recommendedTask?.id }
              ?: state.filteredTasks.firstOrNull { it.isFrog }
              ?: state.filteredTasks.firstOrNull()
      }
  }
  ```
  Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).

---

#### BUG-235: Untimed Tasks Scheduled for Today Sorted to Midnight (00:00) Ahead of Morning Timed Tasks in `FlowViewModelV2.todayActiveTasks`
- **Severity**: Medium
- **Category**: UI & Task Scheduling / Chronological Sorting
- **File**: [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L414), [`EnergyTimelineStream.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt#L99-L103)
- **Problem Description**:
  In `FlowViewModelV2`, `todayActiveTasks` sorted tasks using:
  ```kotlin
  compareBy<TaskEntity> { it.resolveScheduledEpochMillis() ?: Long.MAX_VALUE }
      .thenByDescending { it.isFrog }
      .thenByDescending { it.createdAt }
  ```
  For untimed tasks with `scheduledDate = todayStart` and `scheduledTime = null`, `resolveScheduledEpochMillis()` evaluated to `scheduledDate + 0L` = 00:00 midnight.
  Consequently, date-only untimed tasks were placed at the very beginning of the day ahead of morning 9:00 AM timed tasks.
  In `EnergyTimelineStream`, `activeOrNextIndex` compared `end >= nowMillis` where `end` for untimed tasks was `00:30 AM`, causing untimed tasks to be treated as expired midnight events and skipping the "NOW" playhead.
- **Root Cause**:
  Missing `compareBy<TaskEntity> { it.scheduledTime == null }` tie-breaker to ensure timed tasks sort chronologically first and untimed tasks sort after.
- **Suggested / Applied Fix**:
  1. Updated sort in `FlowViewModelV2.kt`:
     ```kotlin
     compareBy<TaskEntity> { it.scheduledTime == null }
         .thenBy { it.resolveScheduledEpochMillis() ?: Long.MAX_VALUE }
         .thenByDescending { it.isFrog }
         .thenByDescending { it.createdAt }
     ```
  2. Updated `activeOrNextIndex` in `EnergyTimelineStream.kt` to treat untimed tasks (`scheduledTime == null`) as actionable events for today.
  3. Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).

---

#### BUG-236: "Step 4 of 3" Display and Micro-Step Overflow in `FlowSpotlightHeroCard` When All Micro-Steps Completed
- **Severity**: Low
- **Category**: UI & Boundary Handling / Micro-Step Clamping
- **File**: [`FlowHubUpgradesV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowHubUpgradesV2.kt#L231-L260)
- **Problem Description**:
  When all micro-steps of a task were completed (`completedCount == totalCount`), `completedCount + 1` evaluated to `totalCount + 1`, displaying invalid text such as `"🪜 2m Doorway: Step 4 of 3"`, and `currentStep` reset to `topTaskMicroSteps.first()` while progress indicated `100% completed`.
- **Root Cause**:
  Unbounded `completedCount + 1` arithmetic without clamping or handling the all-completed state.
- **Suggested / Applied Fix**:
  Clamped `currentStep` to `topTaskMicroSteps.last()` when all completed, and formatted label as `"🪜 2m Doorway: All $totalCount steps completed"`.
  Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).

---

#### BUG-237: `AutonomyNudgeEngine` Omitted in `FlowViewModelV2.shiftTaskToPeak()` and Missing Dynamic Circadian Peak / Anchor Support
- **Severity**: Medium
- **Category**: Core Logic & Background Services / Notification & Peak Calibration
- **File**: [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L687-L755)
- **Problem Description**:
  When a task was shifted to peak biological hours via `shiftTaskToPeak(task)`:
  1. `AutonomyNudgeEngine.scheduleNudge(context, updated)` was not invoked, leaving previous reminder notifications anchored to the old time.
  2. `baseDate` was extracted from `task.scheduledDate ?: now`, breaking recurring tasks anchored by `habitDate`.
  3. `peakHour` fell back to static `peakEnergyStart` instead of `effectivePeakStart`, and read from `uiState.value.preferences` rather than `preferencesDataStore.preferencesFlow.first()`, causing stale preference reads if `uiState` had not completed its initial combine.
- **Root Cause**:
  Missing nudge engine invocation, direct `scheduledDate` access instead of `effectiveScheduledDateMillis()`, and direct `uiState` preference inspection.
- **Suggested / Applied Fix**:
  Added `AutonomyNudgeEngine.scheduleNudge(it, updated)`, used `task.effectiveScheduledDateMillis() ?: now`, and read `prefs = preferencesDataStore.preferencesFlow.first()` with fallback to `effectivePeakStart`.
  Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).

---

#### BUG-238: Undo Snackbar Invalidation Race in `FlowScreenV2.kt` Clobbering Restoration Confirmation Message
- **Severity**: Low
- **Category**: UI & State Synchronization / Action Message Race
- **File**: [`FlowScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt#L80-L90)
- **Problem Description**:
  When a user clicked "Undo" on a completion or deferral snackbar, `undoCompleteTask()` or `undoDeferTask()` set `_lastActionMessage.value = "Restored: ..."` inside their coroutines.
  However, `FlowScreenV2` unconditionally called `viewModel.clearActionMessage()` immediately following the click callback, resetting `_lastActionMessage.value = null` and clobbering the restoration confirmation snackbar before it could render.
- **Root Cause**:
  Unconditionally clearing the action message on action perform instead of only on dismissal/timeout.
- **Suggested / Applied Fix**:
  Moved `viewModel.clearActionMessage()` into the `else` branch of `if (result == SnackbarResult.ActionPerformed)`.

---

#### BUG-239: `FocusViewModelV2.onCleared()` Destroys Paused Background Session & Foreground Service on Navigation
- **Severity**: High
- **Category**: Lifecycle & Background Service / Paused State Destruction
- **File**: [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L1120)
- **Problem Description**:
  When a user paused an active sprint (`isRunning = false, isPaused = true`) and navigated back to the Plan, Flow, or Notes screens, `FocusViewModelV2.onCleared()` executed:
  ```kotlin
  if (!_uiState.value.isRunning || _uiState.value.isCompleted) {
      soundscapePlayer.release()
      FocusService.stopService(context)
  }
  ```
  Because `isRunning` was `false`, this condition evaluated to `true`, unconditionally terminating `FocusService`, purging the ongoing foreground notification, and clearing `_activeSession.value`. Consequently, the paused session was destroyed, `MiniFocusSessionBar` vanished from the app, and the user could not resume their session from outside the screen.
- **Root Cause**:
  Conflating "not running" with "not active" in ViewModel teardown logic without checking `isPaused`.
- **Suggested / Applied Fix**:
  1. Updated `isSessionActive` property in `FocusUiStateV2` to `isRunning || isPaused`.
  2. Changed `onCleared()` guard to `if ((!_uiState.value.isRunning && !_uiState.value.isPaused) || _uiState.value.isCompleted)`.
  3. Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).

---

#### BUG-240: Stopwatch Mode Elapsed Time Frozen at `00:00` in `FocusService.activeSession` and `MiniFocusSessionBar`
- **Severity**: High
- **Category**: UI & Service Synchronization / Stopwatch Mode Time Drift
- **File**: [`FocusService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L99), [`FocusService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L213)
- **Problem Description**:
  In `FocusTimerModeV2.FLOW_STOPWATCH`, `durationSeconds` is defined as `0L`. `FocusTimingCalculator.calculate()` returns `remainingSeconds = 0L`.
  When `FocusService.startTicker()` updated `_activeSession.value`, it assigned `remainingSeconds = timing.remainingSeconds` (which was permanently `0L`).
  As a result, `MiniFocusSessionBar` in `NeuroFlowAppV2` received `remainingSeconds = 0L` and displayed `00:00` indefinitely throughout the entire stopwatch session when the user browsed other tabs.
- **Root Cause**:
  Emitting countdown `remainingSeconds` into `activeSession` for stopwatch sessions instead of `timing.elapsedSeconds`.
- **Suggested / Applied Fix**:
  1. Updated `FocusService.startTicker()` to assign `remainingSeconds = if (isStopwatch) timing.elapsedSeconds else timing.remainingSeconds`.
  2. Updated `ACTION_START` initial session payload to handle `durationSeconds <= 0L`.
  3. Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).

---

#### BUG-241: Zombie WOOP Bottom Sheet Loop Re-triggering on State Recomposition in `FocusScreenV2.kt`
- **Severity**: Medium
- **Category**: UI & State Synchronization / Bottom Sheet Navigation Loop
- **File**: [`FocusScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt#L72)
- **Problem Description**:
  When navigating to `FocusScreenV2` with `openWoop = true` from deep links or the habit tracker, `LaunchedEffect(state.task, state.preferences, initialOpenWoop)` checked `if (initialOpenWoop || ...)`.
  Whenever user preferences updated, or whenever task updates were emitted into the state flow, `LaunchedEffect` re-executed with `initialOpenWoop = true` and forced `showWoopPromptSheet = true`.
  Users tapping "Maybe Later" or dismissing the sheet found it immediately popping back open.
- **Root Cause**:
  Failure to consume one-shot navigation intent flag, causing recurring side effects across recompositions.
- **Suggested / Applied Fix**:
  Added a remembered `initialWoopHandled` state guard so `initialOpenWoop` is consumed strictly once on entry.

---

#### BUG-242: Completed Micro-Steps Resurrected as Active Step 1 in `ZenMicroStepSpotlightCard`
- **Severity**: Medium
- **Category**: UI & Boundary Handling / Completed Step Resurrection
- **File**: [`FocusScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt#L262)
- **Problem Description**:
  In `FocusScreenV2`, `firstIncompleteIndex` was resolved with an unconditional `?: 0` fallback:
  ```kotlin
  val firstIncompleteIndex = activeMicroSteps.indices.firstOrNull { ... } ?: activeMicroSteps.indexOfFirst { !it.completed }.takeIf { it != -1 } ?: 0
  ```
  When all micro-steps of a task were checked off, `firstIncompleteIndex` defaulted to `0`. `ZenMicroStepSpotlightCard` treated step 0 as incomplete, displayed `"STEP 1 OF N"`, and displayed the title of the already-finished step. Tapping "Complete Step" toggled the completed step back to incomplete.
- **Root Cause**:
  Fallthrough default to index 0 when no incomplete steps remain.
- **Suggested / Applied Fix**:
  Removed the `?: 0` default and guarded `currentMicroStep = if (firstIncompleteIndex != null) activeMicroSteps.getOrNull(firstIncompleteIndex) else null`, allowing the spotlight card to cleanly dismiss when all micro-steps are finished.

---

#### BUG-243: Focus Mode Selector Chips Overflow Screen Width on Standard Android Devices
- **Severity**: Medium
- **Category**: UI Layout & Responsiveness / Horizontal Chip Overflow
- **File**: [`FocusScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt#L158)
- **Problem Description**:
  The 4 timer mode chips (`"5m Micro-Start"`, `"25m Pomodoro"`, `"90m Ultradian Block"`, `"Flow Stopwatch"`) were laid out inside a fixed `Row(modifier = Modifier.fillMaxWidth())` without horizontal scroll capability.
  The combined width of all 4 chips and padding exceeds 520dp. On typical Android phones (360dp-412dp screen width), the chips were aggressively clipped and pushed off-screen.
- **Root Cause**:
  Missing `Modifier.horizontalScroll(rememberScrollState())` on the mode selector chip row.
- **Suggested / Applied Fix**:
  Added `.horizontalScroll(rememberScrollState())` to the mode selector `Row`.

---

#### BUG-244: Completing Untracked Task Fabricates Focus Minutes & Session Telemetry and Distorts Paused Durations
- **Severity**: Medium
- **Category**: Data Integrity & Telemetry / False Focus Metrics
- **File**: [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L765)
- **Problem Description**:
  1. In `completeTask()`, `actualMinutes` was calculated as `max(1, (durationSec / 60).toInt())`. If a user opened a task and immediately marked it complete without running a timer (`durationSec == 0L` and `startedAtMillis == 0L`), `actualMinutes` evaluated to 1, fabricating a 1-minute deep focus session and artificially inflating user focus statistics.
  2. When a user completed a task while paused, `session.totalPausedMs` only saved `accumulatedPauseMillis`, dropping the ongoing pause duration `(now - pausedAtMillis)`.
  3. `nextTask` selection in `loadTask()` naively picked `activeTasks.firstOrNull { it.id != taskId }`, leaking icebox (`Quadrant.ELIMINATE`) or arbitrary future tasks into the post-completion celebration card.
- **Root Cause**:
  Unconditional `max(1, ...)` without checking if a session had run, omission of ongoing pause delta, and lack of priority filtering for next task candidates.
- **Suggested / Applied Fix**:
  1. Only increment `sessionCount` and credit focus minutes when `durationSec > 0L || startedAtMillis > 0L`.
  2. Factored ongoing pause duration into `totalPausedMs` on completion.
  3. Filtered out `Quadrant.ELIMINATE` tasks and sorted candidates (Daily Frogs first, scheduled tasks first).
  4. Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).

---

#### BUG-245: Physiological Sigh Coroutine Cancelled by Atomic Stage Race in `InteractivePhysiologicalSighSheet`
- **Severity**: Medium
- **Category**: Concurrency & Haptics / Coroutine Recomposition Race
- **File**: [`FocusAndRegulationUpgradesV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusAndRegulationUpgradesV2.kt#L176)
- **Problem Description**:
  In `InteractivePhysiologicalSighSheet`, `LaunchedEffect(cycle, breathStage)` transitioned cycles inside stage 2 by executing `cycle++` immediately followed by `breathStage = 0`.
  Updating `cycle` triggered recomposition and cancelled the active `LaunchedEffect` coroutine before `breathStage = 0` was applied, restarting the effect with `(cycle + 1, stage 2)` and causing skipped breath cycles or erratic phase pacing.
- **Root Cause**:
  Multi-variable state transitions triggering premature coroutine cancellation in LaunchedEffect.
- **Suggested / Applied Fix**:
  Replaced stage-keyed restarts with a single sequential `for (currentCycle in 1..totalCycles)` loop inside `LaunchedEffect(totalCycles)`.

---

#### BUG-246: External & Cross-Day Prerequisites Stripped by `DependencyGraphValidator.sanitizeDag` in Multi-Task Schedules
- **Severity**: Critical
- **Category**: Core Logic / Dependency Graph Sanitization
- **File**: [`DependencyGraphValidator.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/DependencyGraphValidator.kt#L83-L89)
- **Problem Description**:
  In `DependencyGraphValidator.sanitizeDag(tasks)`:
  ```kotlin
  for (t in tasks) {
      val validPrereqs = t.prerequisiteIds
          .filter { it != t.id && it in taskMap }
          .toMutableSet()
      prereqMap[t.id] = validPrereqs
  }
  ...
  val sanitizedTasks = tasks.map { task ->
      val cleanPrereqs = prereqMap[task.id].orEmpty()
      task.copy(prerequisiteIds = cleanPrereqs)
  }
  ```
  `prereqMap` was initialized containing ONLY prerequisites satisfying `it in taskMap`.
  Whenever `tasks.size > 1`, any prerequisite not in `tasks` (e.g. cross-day active uncompleted tasks scheduled for tomorrow in `activeUncompletedTaskIds`) was stripped from `task.prerequisiteIds`.
  Consequently, in `TaskSchedulerService.kt`:
  `val blockedByPrereq = allPrereqs.firstOrNull { prereqId -> prereqId in activeUncompletedTaskIds ... }`
  received an empty set of prerequisites. Tasks whose prerequisites were active on future dates bypassed dependency guards and were scheduled prematurely today.
- **Root Cause**:
  `sanitizeDag` conflated internal cycle detection edge filtering with persistent prerequisite filtering. External incoming prerequisite edges cannot form cycles in the candidate subgraph and must not be deleted.
- **Suggested / Applied Fix**:
  Preserved all external prerequisites when mapping `sanitizedTasks`, only severing direct self-references and edges specifically recorded in `brokenEdges` during cycle breaking:
  ```kotlin
  val severedPairs = brokenEdges.toSet()
  val sanitizedTasks = tasks.map { task ->
      val cleanPrereqs = task.prerequisiteIds
          .filter { it != task.id && (it to task.id) !in severedPairs }
          .toSet()
      task.copy(prerequisiteIds = cleanPrereqs, parentTaskId = task.parentTaskId)
  }
  ```
  Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).

---

#### BUG-247: Overdue Tasks Permanently Blocked from Scheduling by Reverse-DAG Deadline Ceiling
- **Severity**: High
- **Category**: Core Logic & Scheduling / Reverse-DAG Deadline Cutoff
- **File**: [`TaskSchedulerService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L790-L845)
- **Problem Description**:
  In `TaskSchedulerService`, `latestAllowedEndMap` set `deadlineCeiling = task.deadlineMillis ?: Long.MAX_VALUE` and enforced it as a hard cutoff in `findBestCircadianWindow`:
  `val effectiveIntervalEnd = min(interval.endMillis, latestAllowedEndMillis)`.
  When a task was overdue (`deadlineMillis < dayStartMillis`), `effectiveIntervalEnd < interval.startMillis`.
  The scheduling loop `while (candidateStart + durationMillis <= effectiveIntervalEnd)` never executed, and `bestSlot` evaluated to `null`.
  The overdue task was permanently deferred every day and could never be placed on the schedule, stranding overdue tasks indefinitely.
  Furthermore, `deadlineBreachPenalty` (-35.0 pts in `utility`) was unreachable dead code because slots past `latestAllowedEndMillis` were already cut off.
- **Root Cause**:
  Conflating hard structural DAG dependency constraints (locked dependent start times) with soft deadline constraints, and failing to provide a fallback window when a task cannot meet its deadline or is overdue.
- **Suggested / Applied Fix**:
  1. Maintained a distinct `structuralCeilingMap` tracking only locked/placed dependent start times.
  2. When `primarySlot` cannot fit before `taskCeiling` and `structuralCeiling > taskCeiling`, fell back to `fallbackStructuralSlot` using `structuralCeiling`, allowing the task to be placed today with `deadlineBreachPenalty = 35.0`.
  3. Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).

---

#### BUG-248: Calendar Slot Budget Overflow in `AutoSchedulingEngineCoordinator.planSchedule`
- **Severity**: Medium
- **Category**: Core Logic / Cognitive Budget & Slot Accounting
- **File**: [`AutoSchedulingRationales.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingRationales.kt#L78)
- **Problem Description**:
  In `AutoSchedulingEngineCoordinator.planSchedule`:
  ```kotlin
  for (task in tasks) {
      val duration = ...
      if (usedMinutes + duration > dailyCognitiveCapacityMinutes) {
          ...
      } else if (duration > availableSlotsMinutes) {
          deferrals.add(...)
      } else {
          placedRationales.add(...)
          usedMinutes += duration
      }
  }
  ```
  `availableSlotsMinutes` represents the total available time across free focus blocks. While `dailyCognitiveCapacityMinutes` checked cumulative `usedMinutes + duration`, `availableSlotsMinutes` checked only `duration > availableSlotsMinutes` without checking against remaining slot minutes (`availableSlotsMinutes - usedMinutes`).
  Consequently, multiple small tasks (e.g. four 20m tasks = 80m total) were all placed into a 50m slot budget because each individual task (20m) was `<= 50m`, drastically overflowing available calendar capacity.
- **Root Cause**:
  Checking static `duration > availableSlotsMinutes` rather than remaining available slot minutes (`duration > availableSlotsMinutes - usedMinutes`).
- **Suggested / Applied Fix**:
  Updated check to:
  ```kotlin
  val remainingSlots = availableSlotsMinutes - usedMinutes
  ...
  else if (duration > remainingSlots) {
      deferrals.add(
          DeferralRationale(
              taskId = task.id,
              reasonCode = DeferralReasonCode.INSUFFICIENT_TIME,
              explanation = "Task duration (${duration}m) exceeds available slot (${maxOf(0, remainingSlots)}m).",
              actionableSuggestion = "Break this task into smaller 25m focus blocks."
          )
      )
  }
  ```
  Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).

---

#### BUG-249: Future-Day Tasks Dropped and Desynchronized in `ConflictResolver.analyzeScheduleConflicts` Due to Default Current-Day Epoch Resolution
- **Severity**: Medium
- **Category**: UI & Core Logic / Multi-Day Conflict Resolution
- **File**: [`AutoSchedulingRationales.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingRationales.kt#L153), [`AutoSchedulingRationales.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingRationales.kt#L268)
- **Problem Description**:
  When `ConflictResolver.analyzeScheduleConflicts(tasks, ..., dayMillis = targetDay)` was called for a future date (e.g. tomorrow or next Monday in PlanScreenV2), line 268 executed:
  `val sMillis = task.resolveScheduledEpochMillis() ?: return@filter false`
  Omitting the `referenceDateMillis` argument caused `resolveScheduledEpochMillis()` to default to `System.currentTimeMillis()` (today).
  For dateless timed tasks (`scheduledTime != null` and `scheduledDate == null`), `sMillis` resolved to today instead of `targetDay`.
  `Instant.ofEpochMilli(sMillis).atZone(zoneId).toLocalDate() == targetDate` evaluated to `false`, silently dropping future-day tasks and failing to detect intraday schedule collisions on future dates.
- **Root Cause**:
  Missing `dayMillis` reference parameter in `resolveScheduledEpochMillis()` and `taskStartMinuteOfDay()`.
- **Suggested / Applied Fix**:
  Passed `dayMillis` into `resolveScheduledEpochMillis(referenceNow)` on lines 153 & 268, and updated `taskStartMinuteOfDay` to accept `referenceDateMillis = targetDayMillis`.
  Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).

---

#### BUG-250: Atomic Ultradian Split Verifier Omits `referenceNowMillis` Causing Anchor Distortion and Drift
- **Severity**: Medium
- **Category**: Core Logic / Pharmacokinetic & Urgency Pacing
- **File**: [`TaskSchedulerService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L960-L995)
- **Problem Description**:
  In `TaskSchedulerService.scheduleDayWithOutcome`, regular task evaluation passed `referenceNowMillis = nowMillis` to anchor urgency and age calculations.
  In the two-phase atomic ultradian split verifier, `referenceNowMillis` was omitted from both `findBestCircadianWindow` calls for Part 1 and Part 2.
  Inside `findBestCircadianWindow`:
  `referenceNowMillis = referenceNowMillis ?: earliestStartMillis`
  For Part 2, `earliestP2Start = p1Window.endMillis + 15m`.
  `evaluateTaskScoreWithWeights` then evaluated urgency and age against `earliestP2Start` (hours into the future) rather than `nowMillis`, skewing the urgency vector `(deadline - nowAnchor)` and distorting the relative scoring of continuation blocks.
- **Root Cause**:
  Omitting `referenceNowMillis = nowMillis` in the atomic ultradian split verifier calls.
- **Suggested / Applied Fix**:
  Supplied `referenceNowMillis = nowMillis` to both Part 1 and Part 2 `findBestCircadianWindow` invocations, and enabled structural ceiling fallback in the split verifier.
  Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).

---

#### BUG-251: Natural Language Scheduling Date Shadowed and Overridden by `initialDate` in `SmartTaskComposerSheetV2`
- **Severity**: High
- **Category**: UI & NLP Inference / Schedule Date Shadowing
- **File**: [`SmartTaskComposerSheetV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L136-L175)
- **Problem Description**:
  In `SmartTaskComposerSheetV2.kt`:
  ```kotlin
  var userScheduledDate by remember(existingTask, initialDate) {
      mutableStateOf<Long?>(existingTask?.scheduledDate ?: (if (mode is SheetMode.Create) initialDate else null))
  }
  ...
  val effectiveScheduledDate = if (scheduledDateCleared) null else (userScheduledDate ?: inferred.scheduledDate)
  ```
  When opened from `PlanScreenV2`, `initialDate` is always passed as `state.selectedDateMillis` (e.g. today's date).
  Consequently, `userScheduledDate` was pre-populated with a non-null timestamp in Create mode.
  When a user typed natural language date directives into the composer (e.g., `"Ship patch tomorrow"`, `"Meeting Friday at 10am"`, `"Sprint review next Monday"`), `inferred.scheduledDate` correctly calculated the target day's epoch millis.
  However, because `userScheduledDate` was already non-null, the Elvis operator `userScheduledDate ?: inferred.scheduledDate` always evaluated to `userScheduledDate` (`initialDate`), silently dropping and ignoring the user's natural language scheduling directive.
- **Root Cause**:
  Conflating the fallback default date (`initialDate`) with the manual user override (`userScheduledDateOverride`), causing the fallback to unconditionally shadow natural language date inference.
- **Suggested / Applied Fix**:
  1. Decoupled the manual DatePicker selection (`userScheduledDateOverride`) so it starts as `null`.
  2. Resolved `effectiveScheduledDate` with prioritized precedence:
     ```kotlin
     val effectiveScheduledDate = when {
         scheduledDateCleared -> null
         userScheduledDateOverride != null -> userScheduledDateOverride
         inferred.scheduledDate != null -> inferred.scheduledDate
         else -> if (mode is SheetMode.Create) initialDate else existingTask?.scheduledDate
     }
     ```
  3. Reset `userScheduledDateOverride` and `scheduledDateCleared = false` in `onValueChange` when the user edits `rawInput`.
  4. Verified in [`SmartTaskComposerSheetV2Test.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2Test.kt).

---

#### BUG-252: Natural Language Directives Ignored in Edit Mode Due to Pre-Populated Override Variables
- **Severity**: High
- **Category**: UI & NLP Inference / Edit Mode Directive Paralysis
- **File**: [`SmartTaskComposerSheetV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L120-L175)
- **Problem Description**:
  In `SmartTaskComposerSheetV2.kt`, state variables were initialized with `existingTask` values:
  ```kotlin
  var userDurationOverride by remember(existingTask) { mutableStateOf<Int?>(existingTask?.estimatedDurationMinutes) }
  var userEnergyOverride by remember(existingTask) { mutableStateOf<EnergyLevel?>(existingTask?.energyLevel) }
  var userPriorityOverride by remember(existingTask) { mutableStateOf<Priority?>(existingTask?.priority) }
  var userFrogOverride by remember(existingTask) { mutableStateOf<Boolean?>(existingTask?.isFrog) }
  var userQuadrantOverride by remember(existingTask) { mutableStateOf<Quadrant?>(existingTask?.quadrant) }
  ```
  In Edit mode, all `user*Override` variables were non-null. Furthermore, `onValueChange` only cleared overrides `if (mode is SheetMode.Create)`.
  When a user opened an existing task to edit its title with NLP tokens (e.g. appending `60m`, `#high`, `!frog`, `!q1`, or `!urgent`), `inferred.*` computed the requested attributes.
  However, because `effective* = user*Override ?: inferred.*` gave precedence to the non-null `user*Override` variables, all explicit NLP tokens in Edit mode were inert. When saved, the NLP tokens were stripped from the title, but the task retained its old duration, energy, priority, frog, and quadrant.
- **Root Cause**:
  Pre-populating transient override variables with existing entity state and guarding override resets to Create mode only.
- **Suggested / Applied Fix**:
  1. Exposed `isEnergyExplicit`, `isPriorityExplicit`, and `isQuadrantExplicit` from `TaskInferenceEngine.InferredTaskMetadata`.
  2. Initialized all `user*Override` variables to `null`.
  3. Evaluated `effective*` attributes dynamically:
     ```kotlin
     val effectiveDuration = userDurationOverride
         ?: if (inferred.isDurationExplicit) inferred.durationMinutes else (existingTask?.estimatedDurationMinutes ?: inferred.durationMinutes)
     val effectiveEnergy = userEnergyOverride
         ?: if (inferred.isEnergyExplicit) inferred.energyDemand else (existingTask?.energyLevel ?: inferred.energyDemand)
     val effectiveFrog = userFrogOverride
         ?: if (inferred.isFrog) true else (existingTask?.isFrog ?: false)
     val effectivePriority = userPriorityOverride
         ?: if (inferred.isPriorityExplicit || effectiveFrog) inferred.priority else (existingTask?.priority ?: inferred.priority)
     val effectiveQuadrant = userQuadrantOverride
         ?: if (inferred.isQuadrantExplicit) inferred.quadrant else (existingTask?.quadrant ?: inferred.quadrant)
     ```
  4. Reset manual overrides in `onValueChange` and un-excluded re-typed tag tokens.
  5. Verified in [`SmartTaskComposerSheetV2Test.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2Test.kt).

---

#### BUG-253: Selecting Due Time in TimePicker Destroys Inferred Due Date by Re-Defaulting to Today
- **Severity**: Medium
- **Category**: UI & Boundary Handling / TimePicker Date Clobbering
- **File**: [`SmartTaskComposerSheetV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L1530-L1545)
- **Problem Description**:
  In `SmartTaskComposerSheetV2`, when confirming a time in the `TimePickerDialog`:
  ```kotlin
  if (timePickerTarget == "deadline") {
      userDeadlineTime = millis
      deadlineTimeCleared = false
      if (userDeadlineDate == null && !deadlineDateCleared) {
          userDeadlineDate = initialDate ?: startOfLocalDay(System.currentTimeMillis())
      }
  }
  ```
  If a user typed `"Ship feature due Friday"` in NLP, `inferred.deadlineDate` was set to Friday, while `userDeadlineDate` remained `null`.
  When the user subsequently tapped "Pick Due Time" to select 5:00 PM, `if (userDeadlineDate == null)` evaluated to `true`.
  The handler assigned `userDeadlineDate = initialDate ?: startOfLocalDay(System.currentTimeMillis())` (today), wiping out the inferred Friday deadline and pulling the deadline to today.
- **Root Cause**:
  Checking raw `userDeadlineDate == null` rather than `effectiveDeadlineDate == null`.
- **Suggested / Applied Fix**:
  Checked `if (effectiveDeadlineDate == null && !deadlineDateCleared)` and `if (effectiveScheduledDate == null && !scheduledDateCleared)`, preserving inferred dates when setting clock times.
  Verified in [`SmartTaskComposerSheetV2Test.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2Test.kt).

---

#### BUG-254: User Note Title Clobbered by AI Task Title and Unhandled Coroutine Exception on Network Failure in `QuickNotesBottomSheet`
- **Severity**: Medium
- **Category**: UI & Data Integrity / Note Title Overwrite & Crash Resilience
- **File**: [`QuickNotesBottomSheet.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/notes/QuickNotesBottomSheet.kt#L48-L225)
- **Problem Description**:
  1. In `QuickNotesBottomSheet.kt`, when converting an AI-synthesized note to a task via `QuickNoteAiSynthesizerCard`, the handler invoked:
     `onSaveNote(finalSyn.extractedTaskTitle, content)`
     If a user had entered a custom title for their note (e.g. `"Strategic Quarterly Roadmap"`), the user's title was discarded and replaced with `finalSyn.extractedTaskTitle`.
  2. In `LaunchedEffect(content, title)`, `onSynthesizeNote(full)` was invoked without exception handling. If the device was offline or encountering network timeouts during Gemini online synthesis, unhandled runtime exceptions crashed the Compose coroutine and the application instead of falling back to local heuristics.
  3. The bottom sheet root column lacked vertical scrolling and IME insets, causing soft keyboards to cover the note action buttons and card on standard Android screens.
- **Root Cause**:
  Unconditional title overwrite in note conversion, absence of exception boundary around suspendable AI callbacks, and missing `.verticalScroll()` / `.imePadding()`.
- **Suggested / Applied Fix**:
  1. Preserved custom note titles: `val noteTitle = title.ifBlank { finalSyn.extractedTaskTitle }`.
  2. Wrapped `onSynthesizeNote(full)` in a try-catch block catching non-cancellation exceptions and falling back to `TaskInferenceEngine.inferFromInput(full)`.
  3. Added `.verticalScroll(rememberScrollState())` and `.imePadding()` to the sheet layout.
  4. Verified in [`SmartTaskComposerSheetV2Test.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2Test.kt).

---

#### BUG-255: Duration Chip Reset Discontinuity on Non-Preset Durations in `SmartTaskComposerSheetV2`
- **Severity**: Medium
- **Category**: UI & State Synchronization / Duration Chip Cycling
- **File**: [`SmartTaskComposerSheetV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L550-L555)
- **Problem Description**:
  In `SmartTaskComposerSheetV2.kt`, clicking the duration chip evaluated:
  ```kotlin
  val cycles = listOf(15, 25, 45, 60, 90, 120)
  val nextIdx = (cycles.indexOf(effectiveDuration) + 1) % cycles.size
  userDurationOverride = cycles[nextIdx]
  ```
  Whenever `effectiveDuration` was not in the preset list (e.g. 30m from `"30m"` NLP input, 33m from history calibration, 50m, or 75m), `cycles.indexOf(effectiveDuration)` returned `-1`.
  `nextIdx` evaluated to `0`, resetting `userDurationOverride` to `cycles[0]` (15m). A user attempting to step up a 30m or 50m task saw it unexpectedly jump down to 15m.
- **Root Cause**:
  Relying on exact element matching in `indexOf` rather than monotonic threshold search.
- **Suggested / Applied Fix**:
  Replaced with `cycles.firstOrNull { it > effectiveDuration } ?: cycles.first()`, ensuring smooth monotonic progression regardless of starting duration.
  Verified in [`SmartTaskComposerSheetV2Test.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2Test.kt).

---

#### BUG-256: `DeadlineEscalationWorker` Never Updates Task to `DO_FIRST` Causing Continuous 4-Hour Notification Spam
- **Severity**: High
- **Category**: Background Services / Notification & Escalation State
- **File**: [`NotificationWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L400)
- **Problem Description**:
  In `DeadlineEscalationWorker.doWork()`, the worker queries active tasks whose non-recurring deadline is within 48 hours and whose quadrant is not `Quadrant.DO_FIRST`:
  `task.quadrant != com.neuroflow.app.domain.model.Quadrant.DO_FIRST`
  The user preference contract and Settings screen describe this toggle as `"Allow schedule-to-do-first escalation notifications"`.
  However, the worker omitted updating the matching tasks in `taskRepository` to `Quadrant.DO_FIRST`.
  Because the tasks remained in `SCHEDULE` or `DELEGATE`, every periodic execution of the worker (every 4 hours, 24/7) matched the exact same tasks again and repeatedly posted the same notification up to 12 times over 48 hours, spamming the user and never actually escalating the task into Do First.
- **Root Cause**:
  Missing database update `taskRepository.update(task.copy(quadrant = Quadrant.DO_FIRST, updatedAt = now))` upon escalation.
- **Suggested / Applied Fix**:
  1. Updated `DeadlineEscalationWorker` to execute `taskRepository.update(task.copy(quadrant = Quadrant.DO_FIRST, updatedAt = now))` for all matching tasks before dispatching the notification.
  2. Once escalated to `DO_FIRST`, tasks are immediately excluded on subsequent runs, breaking the 4-hour notification spam loop.
  3. Wrapped notification dispatch in error boundaries so framework notification failures do not fail the background job.
  4. Verified in [`NotificationWorkerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/worker/NotificationWorkerAuditTest.kt).

---

#### BUG-257: Future-Scheduled Tasks Have Autonomy Nudges Permanently Dropped by Premature Delay Expiration in `AutonomyNudgeEngine` & `AutonomyNudgeWorker`
- **Severity**: High
- **Category**: Background Services & UI / Autonomy Nudge Lifecycle
- **File**: [`AutonomyNudgeEngine.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/engine/AutonomyNudgeEngine.kt#L16), [`AutonomyNudgeWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/AutonomyNudgeWorker.kt#L48)
- **Problem Description**:
  When a user created or scheduled a task (e.g. at 9:00 AM for 4:00 PM), `AutonomyNudgeEngine.scheduleNudge` unconditionally scheduled a OneTimeWorkRequest with a static 2-hour delay (`DELAY_HOURS = 2L`) from scheduling time.
  After 2 hours (11:00 AM), `AutonomyNudgeWorker` woke up and checked:
  `if (dueAt != null && dueAt > now + 10 * 60_000L) return Result.success()`
  Because 4:00 PM was in the future, it returned `Result.success()` and terminated.
  WorkManager marked the one-time work request as SUCCEEDED. It was never re-enqueued.
  When 4:00 PM arrived and the task remained unstarted, no nudge notification was ever fired.
  Additionally, `scheduleNudge` returned early if `POST_NOTIFICATIONS` was not granted at scheduling time, dropping the nudge even if the user granted permissions moments later.
- **Root Cause**:
  Hardcoded 2-hour delay from creation rather than anchoring to `dueAt - now` for future tasks, and dropping the nudge permanently upon premature execution.
- **Suggested / Applied Fix**:
  1. Updated `AutonomyNudgeEngine.scheduleNudge` to compute `delayMillis = if (dueAt != null && dueAt > now) (dueAt - now) else TimeUnit.HOURS.toMillis(DELAY_HOURS)`.
  2. In `AutonomyNudgeWorker`, when `dueAt != null && dueAt > now + 10 * 60_000L`, re-anchors the nudge via `AutonomyNudgeEngine.scheduleNudge(appContext, task)` before returning.
  3. Removed premature permission check at scheduling time (delegating permission verification to notification dispatch time in `AutonomyNudgeWorker`).
  4. Verified in [`AutonomyNudgeWorkerTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/worker/AutonomyNudgeWorkerTest.kt).

---

#### BUG-258: Completed Part 2 Continuation Tasks Deleted or Re-scheduled by `ScheduleAutoTasksWorker`
- **Severity**: High
- **Category**: Core Data Integrity / Split Task History Retention
- **File**: [`ScheduleAutoTasksWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L332), [`ScheduleAutoTasksWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L396)
- **Problem Description**:
  In `ScheduleAutoTasksWorker.applyPlacementsTransactionally()`:
  When a previously split task was scheduled without a split:
  `if (existingPart2 != null) taskRepository.delete(existingPart2)`
  This unconditionally deleted `existingPart2` even if the user had already marked Part 2 as `TaskStatus.COMPLETED`, permanently erasing completed task records and tracked time sessions from Room.
  Conversely, if a split was re-applied while `existingPart2` was completed, `existingPart2` was updated with fresh scheduled times today, reserving calendar focus slots for an already completed task.
- **Root Cause**:
  Missing `existingPart2.status != TaskStatus.COMPLETED` guards in split reconciliation and cleanup.
- **Suggested / Applied Fix**:
  1. Guarded Part 2 deletion: `if (existingPart2 != null && existingPart2.status != TaskStatus.COMPLETED) taskRepository.delete(existingPart2)`.
  2. Guarded Part 2 update: `existingPart2?.status != TaskStatus.COMPLETED`.
  3. Verified in [`ScheduleAutoTasksWorkerTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/worker/ScheduleAutoTasksWorkerTest.kt).

---

#### BUG-259: Iceboxed Tasks (`Quadrant.ELIMINATE`) Treated as Fixed Busy Windows Blocking Schedule Slots in `ScheduleAutoTasksWorker`
- **Severity**: Medium
- **Category**: Core Logic & Scheduling / Phantom Busy Windows
- **File**: [`ScheduleAutoTasksWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L237)
- **Problem Description**:
  In `ScheduleAutoTasksWorker.doWork()`, `fixedTasks` was computed as `allTasks.filterNot { targetTasksMap.containsKey(task.id) ... }`.
  Because `targetTasksMap` excludes `Quadrant.ELIMINATE` (icebox) tasks via `isMutableByAutoScheduler`, any deprioritized/iceboxed task with a pre-existing `scheduledTime` fell into `fixedTasks`.
  `fixedWindows` subsequently converted those icebox tasks into calendar busy windows (`TimeSlotWindow`), blocking candidate active tasks from being scheduled in those hours.
- **Root Cause**:
  Failure to filter out `it.quadrant == Quadrant.ELIMINATE` when constructing `fixedTasks`.
- **Suggested / Applied Fix**:
  1. Added `.filter { it.quadrant != Quadrant.ELIMINATE }` to `fixedTasks`.
  2. Verified in [`ScheduleAutoTasksWorkerTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/worker/ScheduleAutoTasksWorkerTest.kt).

---

#### BUG-260: `TaskReminderWorker` Uses Stale Scheduling Title Snapshot Instead of Fresh Task Entity Title
- **Severity**: Low
- **Category**: UI & Background Services / Notification Title Desynchronization
- **File**: [`NotificationWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L485)
- **Problem Description**:
  In `TaskReminderWorker.doWork()`, the notification title was resolved as:
  `val taskTitle = inputData.getString("taskTitle") ?: task.title`
  Because `taskTitle` is always bundled into `inputData` when the reminder is initially enqueued, the fallback `?: task.title` was unreachable. If a user edited, renamed, or refined the task title between schedule time and reminder trigger time, the reminder notification displayed the stale, pre-edit title.
- **Root Cause**:
  Prioritizing immutable `inputData` over the freshly loaded entity from `taskRepository.getById(taskId)`.
- **Suggested / Applied Fix**:
  1. Updated title resolution to `val taskTitle = task.title.ifBlank { inputData.getString("taskTitle").orEmpty() }`.
  2. Verified in [`NotificationWorkerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/worker/NotificationWorkerAuditTest.kt).

---

#### BUG-261: `CompleteTaskWidgetAction.kt` Unconditionally Stops `FocusService` Killing Active Focus Sessions on Unrelated Tasks
- **Severity**: High
- **Category**: Background Services / Service Isolation & Focus Tracking
- **File**: [`CompleteTaskWidgetAction.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/widget/CompleteTaskWidgetAction.kt#L56)
- **Problem Description**:
  When a user tapped the 1-tap "✓ Done" button on the Next Action home screen widget, `CompleteTaskWidgetAction.execute(context, taskId)` executed `FocusService.stopService(context)` unconditionally.
  If the user was actively running a foreground focus session (with binaural beats audio and ongoing timer) on Task A and completed a quick 2-minute habit Task B from the widget, `FocusService` was instantly destroyed, stopping audio playback and killing the notification for Task A.
- **Root Cause**:
  Calling `FocusService.stopService(context)` without checking if the active session in `FocusService` actually belongs to the completed task (`taskId`).
- **Suggested / Applied Fix**:
  1. Guarded service termination with `if (FocusService.activeSession.value?.taskId == taskId) { FocusService.stopService(context) }`.
  2. Verified in [`FocusWidgetAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/widget/FocusWidgetAuditTest.kt).

---

#### BUG-262: `FocusWidgetProvider` Start Button Remains Visible on Empty State and Fires Stale Pending Intent or Becomes Dead Click
- **Severity**: Medium
- **Category**: UI & State Synchronization / RemoteViews Empty State
- **File**: [`FocusWidgetUpdateWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L151-L171)
- **Problem Description**:
  In `FocusWidgetUpdateWorker`, when `topTask == null`, the widget displayed "No tasks — add one in neuroFlow" and "Score: —", but never called `focusViews.setViewVisibility(R.id.widget_start_button, View.GONE)`.
  Because `widget_focus.xml` has `widget_start_button` set to visible by default with text "▶ Start Focus", the button remained visible on an empty task list. If a task was previously displayed, clicking the start button fired the stale pending intent with the old completed/deleted task ID; if no task had ever been scheduled, it resulted in a dead click. In addition, clicking the empty card body failed to route to the task composer.
- **Root Cause**:
  Missing empty-state visibility toggle on `R.id.widget_start_button` and missing `ACTION_OPEN_NEW_TASK` fallback intent.
- **Suggested / Applied Fix**:
  1. Added `focusViews.setViewVisibility(R.id.widget_start_button, View.VISIBLE)` when `topTask != null` and `View.GONE` when `topTask == null`.
  2. Configured empty-state container click to route to `com.procus.ACTION_OPEN_NEW_TASK`.
  3. Applied the same container action routing to `widget_next_action_container`.

---

#### BUG-263: `CompleteTaskReceiver` Uncaught Coroutine Exception Crashes Process & Omits Task ID Intent Extra Fallbacks
- **Severity**: Medium
- **Category**: Runtime / Crash Resilience & Intent Extra Normalization
- **File**: [`CompleteTaskReceiver.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/widget/CompleteTaskReceiver.kt#L28-L40), [`CompleteTaskWidgetAction.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/widget/CompleteTaskWidgetAction.kt#L58)
- **Problem Description**:
  In `CompleteTaskReceiver.onReceive`, the coroutine launched via `CoroutineScope(SupervisorJob() + Dispatchers.IO).launch` contained a `try-finally` block without a `catch` block.
  Because `entryPoint.taskRepository().markTaskCompleted(taskId)` in `CompleteTaskWidgetAction` was unwrapped, any SQLite, calendar provider, or storage exception escaped `execute()` and propagated out of the coroutine. Uncaught exceptions in coroutines launched via `SupervisorJob().launch` are delivered directly to `Thread.getDefaultUncaughtExceptionHandler()`, crashing the entire application process from the background broadcast receiver.
  Additionally, `CompleteTaskReceiver` only parsed `EXTRA_TASK_ID` (`"extra_task_id"`), rejecting standard `"taskId"` and `"task_id"` intent extras.
- **Root Cause**:
  Missing error boundary in `CompleteTaskReceiver.launch` and unwrapped `markTaskCompleted` invocation.
- **Suggested / Applied Fix**:
  1. Wrapped `CompleteTaskWidgetAction.execute` database mutations in `runCatching`.
  2. Added `catch (_: Throwable)` defensive handler inside `CompleteTaskReceiver.launch`.
  3. Added fallback extraction for `"taskId"` and `"task_id"` extras.
  4. Verified in [`FocusWidgetAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/widget/FocusWidgetAuditTest.kt).

---

#### BUG-264: Split Continuation Tasks (`_part2`) Permanently Excluded from Widgets Even When Part 1 is Completed
- **Severity**: High
- **Category**: UI & Data Integrity / Split Task Continuation Lifecycle
- **File**: [`FocusWidgetUpdateWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L65)
- **Problem Description**:
  In `FocusWidgetUpdateWorker.kt`, candidate tasks were filtered with `!task.id.endsWith("_part2")`.
  When a task is split into two ultradian blocks, Part 1 is assigned `task.id` and Part 2 is assigned `task.id + "_part2"`. When the user completes Part 1, Part 2 becomes the active, top-priority continuation task. Due to the unconditional `!task.id.endsWith("_part2")` exclusion, Part 2 was permanently hidden from both the Focus widget and Next Action widget. If no other tasks were active today, the widget displayed "No tasks — add one in neuroFlow" despite Part 2 awaiting focus.
- **Root Cause**:
  Filtering out all tasks ending in `_part2` without checking if the parent task was still active in the database.
- **Suggested / Applied Fix**:
  1. Replaced unconditional filter with `isPart2Blocked`: Part 2 is blocked only if its parent task (`task.parentTaskId` or `task.id.removeSuffix("_part2")`) is still active in the active task set.
  2. Verified in [`FocusWidgetAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/widget/FocusWidgetAuditTest.kt).

---

#### BUG-265: Blocked Tasks with Unmet Active Prerequisites Recommended as Top Action on Home Screen Widgets
- **Severity**: High
- **Category**: Core Logic & Scheduling / Dependency Graph Enforcement
- **File**: [`FocusWidgetUpdateWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L61-L68)
- **Problem Description**:
  `FocusWidgetUpdateWorker` evaluated candidate tasks without checking `task.dependsOnTaskIds`. If Task B depended on uncompleted prerequisite Task A (`dependsOnTaskIds = "task_a"`), but had high priority or impact, `FocusWidgetUpdateWorker` scored Task B higher than Task A and displayed Task B on the home screen as the recommended action. Users tapping "Start Focus" or "✓ Done" on the widget would bypass prerequisite execution. Furthermore, `SchedulableTask` was constructed with an empty `prerequisiteIds` set.
- **Root Cause**:
  Omission of prerequisite verification against active tasks in `FocusWidgetUpdateWorker`.
- **Suggested / Applied Fix**:
  1. Added `isBlockedByPrereq`: excluded tasks whose `dependsOnTaskIds` contains any active uncompleted task ID.
  2. Populated `prerequisiteIds` in `SchedulableTask`.
  3. Added base leverage boost `(if (task.isFrog) 1000.0 else 0.0) + task.impactScore` to align widget recommendations with `FlowViewModelV2`.
  4. Verified in [`FocusWidgetAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/widget/FocusWidgetAuditTest.kt).

---

#### BUG-266: `CancellationException` Swallowed in `FocusWidgetUpdateWorker.doWork()` Breaking Coroutine Cancellation Contract
- **Severity**: Medium
- **Category**: Concurrency & WorkManager / Structured Concurrency
- **File**: [`FocusWidgetUpdateWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L238-L241)
- **Problem Description**:
  In `FocusWidgetUpdateWorker.doWork()`, the top-level execution was wrapped in `try { ... } catch (_: Exception) { ... }` which caught all exceptions and returned `Result.success()`. In Kotlin Coroutines, `CancellationException` is an `Exception`. When WorkManager cancelled an in-flight expedited update (e.g. upon replacement by a subsequent `enqueueImmediateUpdate`), the `CancellationException` was caught and swallowed, reporting false success and violating structured coroutine cancellation contracts.
- **Root Cause**:
  Catching `Exception` without re-throwing `kotlinx.coroutines.CancellationException`.
- **Suggested / Applied Fix**:
  1. Updated catch block to `if (e is kotlinx.coroutines.CancellationException) throw e`.

---

#### BUG-267: Double-Tap or Redundant `markTaskCompleted` on Widgets Triggers Duplicate Recurring Instances & Habit Streak Inflation
- **Severity**: High
- **Category**: Core Data Integrity & Recurrence / Completion Idempotency
- **File**: [`TaskRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L364-L375)
- **Problem Description**:
  When a user double-tapped the widget's "✓ Done" button in rapid succession (or triggered task completion for an already-completed task), `TaskRepository.markTaskCompleted(taskId)` retrieved the entity via `getById(taskId)` and unconditionally executed `completeAndRecur(task, now)`. Because `completeAndRecur` did not check `if (task.status == TaskStatus.COMPLETED)`, the second invocation incremented `habitStreak` again, inserted a duplicate recurring task into SQLite, and `CompleteTaskWidgetAction` incremented `totalTasksCompleted` again.
- **Root Cause**:
  Lack of status guards on `markTaskCompleted` and `completeAndRecur`.
- **Suggested / Applied Fix**:
  1. Added `if (task.status == TaskStatus.COMPLETED) return false` in `TaskRepository.markTaskCompleted`.
  2. Added `if (task.status == TaskStatus.COMPLETED) return null` in `TaskRepository.completeAndRecur`.
  3. Verified in [`FocusWidgetAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/widget/FocusWidgetAuditTest.kt).

---

#### BUG-268: `MIGRATION_1_2` Crashes with Duplicate Column Error on Databases Initialized on Schema v1
- **Severity**: High
- **Category**: Database Migrations & Schema Drift
- **File**: [`NeuroFlowDatabase.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/NeuroFlowDatabase.kt#L40-L55)
- **Problem Description**:
  In Room schema `1.json`, the `tasks` table was already generated with column `recurrenceIntervalDays INTEGER NOT NULL`.
  When an existing user database created under schema v1 was upgraded to v2 or above, `MIGRATION_1_2` executed:
  `ALTER TABLE tasks ADD COLUMN recurrenceIntervalDays INTEGER NOT NULL DEFAULT 1` unconditionally.
  Because SQLite forbids adding a column that already exists on a table, SQLite threw:
  `android.database.sqlite.SQLiteException: duplicate column name: recurrenceIntervalDays`
  aborting migration and crashing application launch.
- **Root Cause**:
  Missing column existence check before executing `ALTER TABLE ADD COLUMN`.
- **Suggested / Applied Fix**:
  1. Updated `MIGRATION_1_2` to inspect `PRAGMA table_info(tasks)` before executing the `ALTER TABLE` statement.
  2. If `recurrenceIntervalDays` is already present, the migration no-ops idempotently; if absent, it adds the column.
  3. Verified in [`RoomDatabaseAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/local/RoomDatabaseAuditTest.kt).

---

#### BUG-269: Unhandled SQLite Foreign Key Constraint Crash in `SessionRepository.syncFocusSession` on Deleted or Untracked Tasks
- **Severity**: High
- **Category**: Database Concurrency & Foreign Key Integrity
- **File**: [`SessionRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/SessionRepository.kt#L83-L115)
- **Problem Description**:
  The `focus_sessions` table defines a foreign key constraint referencing `tasks(id)` with `ON DELETE CASCADE`. In `AppModule.kt`, `PRAGMA foreign_keys = ON;` is enforced on every database connection.
  When a task is deleted while a focus session is in progress, or when time sessions are synced/logged for untracked tasks, `SessionRepository.insert(session)` and `update(session)` invoke `syncFocusSession(session)`.
  Inside `syncFocusSession`, `dao.insert(FocusSessionEntity(...))` was executed without error boundaries.
  Attempting to insert a `FocusSessionEntity` with a `taskId` that does not exist in `tasks` causes SQLite to throw:
  `android.database.sqlite.SQLiteConstraintException: FOREIGN KEY constraint failed (code 787 SQLITE_CONSTRAINT_FOREIGNKEY)`
  Escaping the repository coroutine, this uncaught exception crashed the process during background synchronization or timer completion.
  Furthermore, if `session.endedAt != null` but `session.durationMinutes <= 0f`, `(session.durationMinutes * 60).toInt()` wrote `0` to `durationSeconds` instead of calculating actual elapsed time, and `SessionRepository.deleteAll()` omitted deleting rows from `focus_sessions`.
- **Root Cause**:
  Lack of foreign key constraint handling in `syncFocusSession`, missing duration fallback calculation, and omitted `focusSessionDao.deleteAll()`.
- **Suggested / Applied Fix**:
  1. Wrapped `dao.insert` in `runCatching` inside `SessionRepository.syncFocusSession`.
  2. Added fallback duration calculation: if `durationMinutes <= 0f` and `endedAt != null`, calculates `session.trackedMinutes(endedAt) * 60`.
  3. Added `@Query("DELETE FROM focus_sessions") suspend fun deleteAll()` to `FocusSessionDao` and called it in `SessionRepository.deleteAll()`.
  4. Verified in [`RoomDatabaseAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/local/RoomDatabaseAuditTest.kt).

---

#### BUG-270: Missing `@TypeConverter` for `HyperFocusState` in `Converters.kt` Causing Crash on Corrupted or Unknown States
- **Severity**: Medium
- **Category**: Room Database / Enum Deserialization
- **File**: [`Converters.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/Converters.kt#L93-L101), [`HyperFocusSessionEntity.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/feature/hyperfocus/data/HyperFocusSessionEntity.kt#L12)
- **Problem Description**:
  `HyperFocusSessionEntity` persists `val state: HyperFocusState`.
  While all other enums (`Quadrant`, `Priority`, `TaskStatus`, `Recurrence`, `EnergyLevel`, `TaskType`, `ContractOutcome`, `RewardTier`) have defensive `@TypeConverter` functions in `Converters.kt` with `try-catch` blocks falling back to safe defaults, `HyperFocusState` had no converter.
  When Room deserializes enums without a converter, it generates `Enum.valueOf(string)` code.
  If an unknown, forward-compatible, or corrupted state string is present in SQLite (e.g. via cloud restore, corrupted storage, or downgraded versions), Room throws:
  `java.lang.IllegalArgumentException: No enum constant com.neuroflow.app.domain.model.HyperFocusState.<value>`
  immediately crashing any screen or background worker querying `hyperfocus_sessions`.
- **Root Cause**:
  Omission of `@TypeConverter` for `HyperFocusState` in `Converters.kt`.
- **Suggested / Applied Fix**:
  1. Added `@TypeConverter fun fromHyperFocusState(value: HyperFocusState): String = value.name`.
  2. Added `@TypeConverter fun toHyperFocusState(value: String): HyperFocusState` catching exceptions and defaulting safely to `HyperFocusState.INACTIVE`.
  3. Verified in [`RoomCorruptedEnumDeserializationTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/local/RoomCorruptedEnumDeserializationTest.kt).

---

#### BUG-271: SQLite Statement Leak and Duration Calculation Mismatch in `MIGRATION_21_22` Micro-Step & Session Extraction
- **Severity**: Medium
- **Category**: Database Migrations & Resource Management
- **File**: [`NeuroFlowDatabase.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/NeuroFlowDatabase.kt#L400-L450)
- **Problem Description**:
  In step 7 of `MIGRATION_21_22`, `db.compileStatement("INSERT INTO task_micro_steps ...")` was invoked inside the inner loop for every single micro-step of every task without calling `stmt.close()`. For users with dozens of tasks and hundreds of micro-steps, this leaked dozens of native SQLite statement handles (`sqlite3_stmt`), produced Android logcat warnings, exhausted SQLite statement caches, and incurred repetitive query compilation overhead.
  In step 6 of `MIGRATION_21_22`, `durationSeconds` was computed strictly as `CAST(ROUND(s.durationMinutes * 60) AS INTEGER)`. For legacy closed sessions where `durationMinutes` remained uncomputed (0.0f), this populated `durationSeconds` as 0, discarding elapsed session duration history.
- **Root Cause**:
  Compiling statements inside the inner loop without closing them, and assuming `s.durationMinutes > 0` for all closed sessions.
- **Suggested / Applied Fix**:
  1. Compiled the statement once outside the loop, reused it with `clearBindings()`, and closed it cleanly via `db.compileStatement(...).use { stmt -> ... }`.
  2. In step 6, added fallback: `CASE WHEN s.durationMinutes > 0 THEN CAST(ROUND(s.durationMinutes * 60) AS INTEGER) WHEN s.endedAt IS NOT NULL THEN CAST(MAX(0, (s.endedAt - s.startedAt - s.totalPausedMs) / 1000) AS INTEGER) ELSE 0 END`.
  3. Verified in [`RoomDatabaseAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/local/RoomDatabaseAuditTest.kt).

---

#### BUG-272: Desynchronized Micro-Steps in Scheduling Orchestrators (`microStepCounts` Returns `(0, 0)` for Tasks Created via `ifThenPlan`)
- **Severity**: Medium
- **Category**: Data Integrity & Scheduler Concurrency
- **File**: [`TaskRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L95-L117)
- **Problem Description**:
  `PlanViewModelV2` and `ScheduleAutoTasksWorker` compute `microStepCounts` by querying `taskRepository.getMicroSteps(task.id)`. However, tasks created via `SmartTaskComposerSheetV2`, `FlowViewModelV2.addTaskFromNote`, or NLP task breakdown store their micro-steps in `task.ifThenPlan`. Because `TaskRepository.getMicroSteps` only queried `taskMicroStepDao`, it returned an empty list for any task whose micro-steps had not yet been migrated or separately written to Room. Consequently, the auto-scheduler treated all newly composed tasks as having 0 micro-steps, nullifying doorway effect boosts, completion pacing, and cognitive budget calculations.
- **Root Cause**:
  Lack of fallback in `TaskRepository.getMicroSteps(taskId)` to parse `task.ifThenPlan` when `taskMicroStepDao` has no records.
- **Suggested / Applied Fix**:
  1. Updated `TaskRepository.getMicroSteps(taskId)`: when `taskMicroStepDao.getByTaskId(taskId)` is empty, lazily reads `taskDao.getById(taskId)?.ifThenPlan`, parses the steps using `StepByStepPlanCodec`, and caches them to `taskMicroStepDao`.
  2. Verified in [`RoomDatabaseAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/local/RoomDatabaseAuditTest.kt).

---

#### BUG-273: Direct Intent Hijacked and App Viewport Overwritten by `NewChapterCard` / `TopGoalsRefillCard` Upon Intent Consumption
- **Severity**: High
- **Category**: UI & Navigation / Lifecycle State Synchronization
- **File**: [`MainActivity.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L205-L380)
- **Problem Description**:
  When `MainActivity` was launched via an actionable intent from a notification or widget (e.g. `ACTION_OPEN_FOCUS`, `ACTION_OPEN_NEW_TASK`, `ACTION_OPEN_SCHEDULE`), `hasDirectIntent` was initially evaluated as `true`. As expected, this bypassed `NewChapterCard` and `TopGoalsRefillCard` on the initial frame, composing `NeuroFlowAppV2`.
  However, as soon as `NeuroFlowAppV2` consumed the intent on frame 1 (e.g. `onInitialTaskConsumed()` set `initialFocusTaskId = null`), `hasDirectIntent` immediately became `false` on the next recomposition.
  If the session fell on a Sunday / fresh-start rollover or if goals were due for refill (`lastYearlyGoalHandledYear != currentYear` or `lastWeeklyGoalHandledWeek != yearWeekKey`), the outer/inner `when` block in `MainActivity` immediately unmounted `NeuroFlowAppV2` and rendered `NewChapterCard` or `TopGoalsRefillCard`. The user's intended focus screen or task composer was abruptly ripped away and replaced within ~100ms of tapping the notification.
- **Root Cause**:
  Computing `hasDirectIntent` exclusively from transient variables (`initialFocusTaskId != null`, etc.) that are cleared immediately upon consumption, rather than latching a session-level flag to protect the user's intent destination throughout the session.
- **Suggested / Applied Fix**:
  1. Latched `directIntentSessionHandled = true` whenever a direct intent is present or handled during the session.
  2. Defined `hasDirectIntent = pendingDirectIntent || directIntentSessionHandled` so intent-driven sessions remain protected after intent consumption.
  3. Verified in [`NavigationAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/navigation/NavigationAuditTest.kt).

---

#### BUG-274: Notification & Widget Launch Intents Silently Ignored When Activity Restored from `savedInstanceState`
- **Severity**: High
- **Category**: Android Lifecycle / Intent Dispatch & Process Restoration
- **File**: [`MainActivity.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L170-L185)
- **Problem Description**:
  In `MainActivity.onCreate`:
  ```kotlin
  if (savedInstanceState != null) {
      initialFocusTaskId = savedInstanceState.getString("saved_focus_task_id")
      ...
  } else {
      handleIntent(intent)
  }
  ```
  When Android killed the app in the background due to memory pressure and the user subsequently relaunched the app by tapping a notification (e.g. task reminder, autonomy nudge, sprint complete) or widget action ("▶ Start Focus"), Android passed both `savedInstanceState != null` and the new launch `intent`.
  Because `handleIntent(intent)` was placed exclusively in the `else` branch of `savedInstanceState != null`, the incoming intent action and extras were completely ignored. The app restored stale background state instead of navigating to the requested task.
- **Root Cause**:
  Restricting `handleIntent` to cold starts with `savedInstanceState == null`.
- **Suggested / Applied Fix**:
  1. Checked `val hasPendingAction = !intent.action.isNullOrBlank() || intent.hasExtra("taskId") || intent.hasExtra("task_id")`.
  2. Executed `handleIntent(intent)` whenever `hasPendingAction` is true, falling back to `savedInstanceState` restoration only when no actionable intent is pending.
  3. Verified in [`NavigationAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/navigation/NavigationAuditTest.kt).

---

#### BUG-275: Duplicate `FlowScreenV2` Pushed to Backstack on App Launch via `initialTaskId == "tab_focus"` and Unbounded Task Stack Accumulation
- **Severity**: Medium
- **Category**: Navigation / Backstack Integrity & Route Duplication
- **File**: [`NeuroFlowAppV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L76-L175)
- **Problem Description**:
  1. In `MainActivity.kt`, when `ACTION_OPEN_FOCUS` arrived with no tasks available, `initialFocusTaskId` was assigned `"tab_focus"`. In `NeuroFlowAppV2.kt`, `LaunchedEffect(initialTaskId)` handled `"tab_focus"` by calling `navController.navigate(NavHubsV2.Flow.route)` without `launchSingleTop = true` or `popUpTo`. Because `NavHost` starts at `NavHubsV2.Flow.route`, this pushed a duplicate `FlowScreenV2` destination onto the backstack. Pressing the system back button returned the user to an identical Flow screen instead of closing the app.
  2. In `LaunchedEffect(initialScheduleOpen)`, `navController.navigate(NavHubsV2.Plan.route)` was invoked without `popUpTo(findStartDestination) { saveState = true }` or `restoreState = true`, pushing duplicate Plan screens and losing saved tab state.
  3. In `MiniFocusSessionBar.onTap`, `navController.navigate("focus_v2/${currentSession.taskId}")` lacked `launchSingleTop = true` and failed to validate `currentSession.taskId.isNotBlank()`. If `taskId` was blank, Navigation Compose threw `IllegalArgumentException: Navigation destination ... cannot be found` and crashed the application.
- **Root Cause**:
  Unconditional `navigate()` calls without `launchSingleTop = true`, missing `findStartDestination` popUpTo options on hub navigation, and missing blank `taskId` validation in `MiniFocusSessionBar`.
- **Suggested / Applied Fix**:
  1. For `"tab_focus"`, guarded `if (currentRoute != NavHubsV2.Flow.route)` and applied start-destination `popUpTo` with `launchSingleTop = true` and `restoreState = true`.
  2. For `initialScheduleOpen` and `initialOpenComposer`, navigated with start-destination `popUpTo`, `saveState = true`, `launchSingleTop = true`, and `restoreState = true`.
  3. In `MiniFocusSessionBar.onTap`, guarded `if (currentSession.taskId.isNotBlank())` and applied `launchSingleTop = true` with URI-encoded taskId.
  4. Verified in [`NavigationAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/navigation/NavigationAuditTest.kt).

---

#### BUG-276: Redundant Dual `FlowViewModelV2` Allocation and Desynchronized Ambient Phase Telemetry
- **Severity**: Medium
- **Category**: Architecture & Memory / ViewModel Lifecycle Desynchronization
- **File**: [`NeuroFlowAppV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L124-L230)
- **Problem Description**:
  At the `NeuroFlowAppV2` container level, line 124 instantiated `val flowViewModel: FlowViewModelV2 = hiltViewModel()` to observe `flowState.energyTimeline` for the phase-ambient backdrop container. However, inside `composable(NavHubsV2.Flow.route)`, `FlowScreenV2` was invoked without passing `viewModel = flowViewModel`. As a result, `FlowScreenV2` instantiated a second independent `FlowViewModelV2` scoped to the `NavBackStackEntry`.
  Both ViewModels maintained duplicate database observers, coroutine jobs, and state flows. When actions were taken in `FlowScreenV2` (e.g. deferring tasks, shifting to peak, logging time), the outer `flowViewModel` at the `NeuroFlowAppV2` level duplicated queries and wasted host memory and CPU cycles.
- **Root Cause**:
  Failure to forward `flowViewModel` to `FlowScreenV2(..., viewModel = flowViewModel)` matching the pattern used for `planViewModel`.
- **Suggested / Applied Fix**:
  Passed `viewModel = flowViewModel` to `FlowScreenV2` in `NeuroFlowAppV2.kt`.

---

#### BUG-277: Material You Dynamic Color Preference Wiped Out by Inner Theme Override in `NeuroFlowAppV2.kt`
- **Severity**: Low
- **Category**: UI & Theming / Theme Configuration Overwrite
- **File**: [`NeuroFlowAppV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L67-L123)
- **Problem Description**:
  In `MainActivity.kt`, `NeuroFlowThemeV2` was invoked with `dynamicColor = preferences?.dynamicColorEnabled ?: false`, correctly enabling dynamic Material You theming on Android 12+ devices.
  However, inside `MainActivity`, `NeuroFlowAppV2` was rendered, which invoked its own nested `NeuroFlowThemeV2(darkTheme = darkTheme, amoledDark = isAmoled)`.
  `NeuroFlowAppV2` did not accept a `dynamicColor` parameter, so the inner `NeuroFlowThemeV2` defaulted to `dynamicColor = false`. This nested call unconditionally overrode the outer theme, disabling Material You dynamic color palette generation across all Compose screens even when enabled by the user.
- **Root Cause**:
  Omitting `dynamicColor` parameter in `NeuroFlowAppV2` and defaulting to `false` in nested `NeuroFlowThemeV2`.
- **Suggested / Applied Fix**:
  Added `dynamicColor: Boolean = false` to `NeuroFlowAppV2` and forwarded it to the inner `NeuroFlowThemeV2`.

---

#### BUG-278: Hardcoded Backstack Purge in `FocusScreenV2.onNavigateToNextTask` Destroys Prior Navigation History
- **Severity**: Low
- **Category**: Navigation / Backstack Integrity & User Flow
- **File**: [`NeuroFlowAppV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L415-L445)
- **Problem Description**:
  In `NeuroFlowAppV2.kt`, `onNavigateToNextTask` was defined as:
  ```kotlin
  onNavigateToNextTask = { nextTaskId ->
      navController.navigate("focus_v2/$nextTaskId") {
          popUpTo(NavHubsV2.Flow.route)
      }
  }
  ```
  When a user navigated to focus from `PlanScreenV2` (`plan_v2`), `WeeklyScheduleScreen` (`weekly_schedule`), or `HistoryScreen` (`history`), completing or transitioning to the next recommended task via `onNavigateToNextTask` executed `popUpTo(NavHubsV2.Flow.route)`.
  This unconditionally blew away all destinations back to `Flow` (including `Plan`, `WeeklySchedule`, or `History`), destroying the user's navigational backstack. When the user finished the next task and tapped the back button, they were unexpectedly dumped on the `Flow` screen rather than returning to `Plan` or `WeeklySchedule`.
- **Root Cause**:
  Hardcoding `popUpTo(NavHubsV2.Flow.route)` instead of popping the previous `focus_v2` entry via `popUpTo(currentDestination.id) { inclusive = true }`.
- **Suggested / Applied Fix**:
  Updated `onNavigateToNextTask` to use `popUpTo(navController.currentDestination?.id ?: ...) { inclusive = true }` with `launchSingleTop = true` and URI encoding.
  Verified in [`NavigationAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/navigation/NavigationAuditTest.kt).

---

#### BUG-279: Completed Part 2 Continuation Tasks Deleted or Overwritten by `PlanViewModelV2.autoScheduleDay()`
- **Severity**: High
- **Category**: Core Data Integrity / Split Task History Retention
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L581-L610)
- **Problem Description**:
  When a user tapped "Auto-Plan" or `PlanViewModelV2.autoScheduleDay()` ran:
  1. If the base task was split again, `PlanViewModelV2` copied and overwrote `existingPart2` (`taskRepository.update(part2Entity)`) without checking if `existingPart2` had already been completed (`status == TaskStatus.COMPLETED`) or was actively focused in a session (`existingPart2.id in focusedTaskIds`). Completed Part 2 tasks were resurrected into active scheduled tasks, distorting user completion history.
  2. If the base task was no longer split, the `else` branch unconditionally called `taskRepository.delete(orphanedPart2)`. This permanently wiped completed Part 2 entities from the SQLite database, destroying completion metrics, focus minutes, and streak accounting.
- **Root Cause**:
  Missing `status != TaskStatus.COMPLETED` and `id !in focusedTaskIds` guards on Part 2 task entity updates and deletion in `PlanViewModelV2.kt` (mirroring the flaw previously patched in `ScheduleAutoTasksWorker` under BUG-258).
- **Suggested / Applied Fix**:
  1. Guarded Part 2 entity persistence with `existingPart2?.status != TaskStatus.COMPLETED && existingPart2?.id !in focusedTaskIds`.
  2. Guarded orphan cleanup with `if (existingPart2 != null && existingPart2.status != TaskStatus.COMPLETED && existingPart2.id !in focusedTaskIds)`.
  3. Added `AutonomyNudgeEngine.cancelNudge` on orphan deletion and `scheduleNudge` on Part 2 creation/update.
  4. Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).

---

#### BUG-280: Missing `effectivePeakStart`/`effectivePeakEnd` & `peakEnergyStart`/`peakEnergyEnd` in `PlanViewModelV2` Preference Flow Suppression
- **Severity**: Medium
- **Category**: UI & State Synchronization / Circadian Peak Desynchronization
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L212-L229)
- **Problem Description**:
  In `PlanViewModelV2`, `preferencesDataStore.preferencesFlow.distinctUntilChanged { old, new -> ... }` was configured to suppress unnecessary recompositions from irrelevant preference updates.
  However, the equality lambda omitted `peakEnergyStart`, `peakEnergyEnd`, `effectivePeakStart`, and `effectivePeakEnd`. When dynamic peak detection adjusted the circadian energy curve, or when the user altered peak energy hours in Settings, `distinctUntilChanged` evaluated the preference change as identical (`true`).
  As a result, `uiState` was never re-emitted, leaving `PlanScreenV2` displaying stale circadian peak badges (`Peak Circadian Window: XX:00 – YY:00`) and stale `isDuringPeak` task highlighting until process restart.
- **Root Cause**:
  Omitting circadian peak fields from the `distinctUntilChanged` comparator in `PlanViewModelV2.kt`.
- **Suggested / Applied Fix**:
  1. Added `old.peakEnergyStart == new.peakEnergyStart`, `old.peakEnergyEnd == new.peakEnergyEnd`, `old.effectivePeakStart == new.effectivePeakStart`, and `old.effectivePeakEnd == new.effectivePeakEnd` to `distinctUntilChanged`.
  2. Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).

---

#### BUG-281: Dynamic Circadian Peak & Anchor Ignored and Autonomy Nudge Omitted in `PlanViewModelV2.shiftTaskToPeak()` and `assignTaskTime()`
- **Severity**: High
- **Category**: Core Logic & Background Services / Notification & Peak Calibration
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L652-L950)
- **Problem Description**:
  1. In `PlanViewModelV2.shiftTaskToPeak()`, `peakHour` fell back directly to `uiState.value.preferences.peakEnergyStart`, ignoring dynamic `preferences.effectivePeakStart`.
  2. `baseDate` was evaluated as `task.scheduledDate ?: _selectedDate.value` instead of `task.effectiveScheduledDateMillis() ?: _selectedDate.value`. For recurring tasks whose date is anchored on `habitDate`, `scheduledDate` could be null, causing the task to lose its habit anchor and drift to `_selectedDate`.
  3. `AutonomyNudgeEngine.scheduleNudge(context, updated)` was omitted from `shiftTaskToPeak()`, `assignTaskTime(task, hour, minute)`, `assignTaskTime(taskId, newStartMillis)`, and `updateTask(task)`. When a user adjusted schedules or edited tasks from `PlanScreenV2`, WorkManager nudges were never rescheduled, firing at stale times or failing to alert the user.
- **Root Cause**:
  Failure to wire `effectivePeakStart` fallback and `AutonomyNudgeEngine.scheduleNudge` across task modification and scheduling mutators in `PlanViewModelV2.kt`.
- **Suggested / Applied Fix**:
  1. Updated `shiftTaskToPeak()` to check `if (prefs.effectivePeakStart in 0..23) prefs.effectivePeakStart else prefs.peakEnergyStart`.
  2. Anchored base date using `task.effectiveScheduledDateMillis() ?: _selectedDate.value`.
  3. Dispatched `context?.let { AutonomyNudgeEngine.scheduleNudge(it, updated) }` in `shiftTaskToPeak()`, `assignTaskTime()`, `updateTask()`, and `autoScheduleDay()`.

---

#### BUG-282: Immediate Dismissal Race of Undo Snackbar on Task Completion in `PlanScreenV2.kt`
- **Severity**: Medium
- **Category**: UI & State Synchronization / Action Message Race
- **File**: [`PlanScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L86-L124)
- **Problem Description**:
  In `PlanScreenV2.kt`, when a user marked a task completed (via timeline swipe or checkbox in Timeline or Matrix view), `handleCompleteTask` displayed a snackbar with `actionLabel = "Undo"`.
  Simultaneously, `PlanViewModelV2.completeTask()` emitted `"Completed: ${task.title}$affirmationSuffix"` to `_userActionMessage`.
  The `LaunchedEffect(Unit)` collecting `userActionMessage` immediately invoked `snackbarHostState.currentSnackbarData?.dismiss()` and showed the text message without an action button.
  This race condition dismissed the interactive "Undo" snackbar within milliseconds of appearance, preventing users from undoing accidental task completions.
- **Root Cause**:
  Unconditionally collecting and displaying completion messages in `LaunchedEffect` that clobber the active Undo snackbar created by `handleCompleteTask`.
- **Suggested / Applied Fix**:
  1. Filtered out `msg.startsWith("Completed:")` in `LaunchedEffect(Unit)` collecting `userActionMessage`, preserving the completion snackbar with the interactive Undo action.
  2. Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).

---

#### BUG-283: Cognitive Budget Meter Miscalculation in `PlanScreenV2.kt` Using Raw `estimatedDurationMinutes` Instead of `TaskBlockPlan.plannedMinutes`
- **Severity**: Medium
- **Category**: Core Logic & UI / Cognitive Load Accounting
- **File**: [`PlanScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L881)
- **Problem Description**:
  In `PlanScreenV2.kt`:
  ```kotlin
  val scheduledMinutes = state.tasksForDay.sumOf { it.estimatedDurationMinutes }
  ```
  The cognitive budget meter calculated total planned time directly from raw `estimatedDurationMinutes`.
  When tasks were Bayesian-calibrated or split into ultradian parts (Part 1 and Part 2), the actual scheduled block duration was recorded in `plannedBlockMinutes` (e.g. 45m on Part 1 and 45m on Part 2), but `estimatedDurationMinutes` on Part 1 remained the unsplit original (e.g. 90m).
  Summing `estimatedDurationMinutes` produced an inflated total (90 + 45 = 135m instead of 90m), misleading the user into believing their cognitive budget was exceeded.
- **Root Cause**:
  Summing `estimatedDurationMinutes` directly instead of using `TaskBlockPlan.plannedMinutes(task)`.
- **Suggested / Applied Fix**:
  1. Changed calculation to `state.tasksForDay.sumOf { TaskBlockPlan.plannedMinutes(it) }`.
  2. Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).

---

#### BUG-284: Missing Circadian Peak Fields in `FlowViewModelV2` Preference Flow Suppression
- **Severity**: Medium
- **Category**: UI & State Synchronization / Circadian Peak Desynchronization
- **File**: [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L150-L165)
- **Problem Description**:
  In `FlowViewModelV2`, `preferencesDataStore.preferencesFlow.distinctUntilChanged { old, new -> ... }` omitted `peakEnergyStart`, `peakEnergyEnd`, `effectivePeakStart`, `effectivePeakEnd`, and `detectedPeakMinuteOfDay`.
  When circadian peak calibration dynamically shifted the user's peak window, or when the user modified their peak hours in Settings, `distinctUntilChanged` evaluated the preference object as identical.
  Consequently, `uiState` was not re-emitted, leaving `FlowScreenV2` displaying stale peak window badges, stale energy recommendations, and desynchronized Hero Barometer curves until process restart.
- **Root Cause**:
  Omission of circadian peak metrics from the `distinctUntilChanged` comparator in `FlowViewModelV2.kt`.
- **Suggested / Applied Fix**:
  1. Added `old.peakEnergyStart == new.peakEnergyStart`, `old.peakEnergyEnd == new.peakEnergyEnd`, `old.effectivePeakStart == new.effectivePeakStart`, `old.effectivePeakEnd == new.effectivePeakEnd`, and `old.detectedPeakMinuteOfDay == new.detectedPeakMinuteOfDay` to `distinctUntilChanged`.
  2. Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).

---

#### BUG-285: Unconditional Exclusion of Part 2 Continuation Tasks from Actionable and Recommended Task Pool in `FlowViewModelV2.kt`
- **Severity**: High
- **Category**: Core Logic & Task Recommendation / Split Task Lifecycle
- **File**: [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L363)
- **Problem Description**:
  In `FlowViewModelV2.kt`, `actionableActiveTasks` (which feeds `recommendedTask` for the Flow Spotlight Hero Card and Hero Barometer) contained an unconditional filter: `!task.id.endsWith("_part2")`.
  Even after Part 1 was finished and marked `COMPLETED` in the database, Part 2 could NEVER appear as the recommended top task or optimal next action.
  The user was forced to manually locate and launch Part 2 from the timeline stream.
- **Root Cause**:
  Unconditionally filtering out `_part2` tasks rather than checking whether Part 1 was still active (`isPart2Blocked`).
- **Suggested / Applied Fix**:
  1. Replaced `!task.id.endsWith("_part2")` with `!isPart2Blocked`, where:
     ```kotlin
     val isPart2Blocked = task.id.endsWith("_part2") && (
         (task.parentTaskId != null && task.parentTaskId in activeTaskIds) ||
         task.id.removeSuffix("_part2") in activeTaskIds
     )
     ```
  2. Applied the same `!isPart2Blocked` guard to `todayActiveTasks` unscheduled task resolution.
  3. Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).

---

#### BUG-286: Missing Autonomy Nudge Rescheduling on `updateTask()`, `undoCompleteTask()`, and `applyEmergencyReset()`, and Faulty Cancellation on `undoDeferTask()` in `FlowViewModelV2.kt`
- **Severity**: High
- **Category**: Background Services & Notifications / Autonomy Nudge Lifecycle
- **File**: [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L774-L920)
- **Problem Description**:
  1. In `FlowViewModelV2.updateTask()`, `AutonomyNudgeEngine.scheduleNudge(context, task)` was completely omitted. When tasks were edited via `SmartTaskComposerSheetV2` in `FlowScreenV2`, notification triggers became stale or never fired.
  2. In `FlowViewModelV2.undoCompleteTask()`, restored active tasks were never re-enrolled into `AutonomyNudgeEngine.scheduleNudge(context, updated)` (their nudges had been cancelled upon completion).
  3. In `FlowViewModelV2.undoDeferTask()`, the method invoked `AutonomyNudgeEngine.cancelNudge(context, restored.id)`. Cancelling the reminder when undoing deferral left the restored task with zero notifications.
  4. In `FlowViewModelV2.applyEmergencyReset()`, rescheduled kept tasks never had their nudges updated.
- **Root Cause**:
  Incomplete lifecycle wiring between `FlowViewModelV2` mutators and `AutonomyNudgeEngine`.
- **Suggested / Applied Fix**:
  1. Wired `context?.let { AutonomyNudgeEngine.scheduleNudge(it, task) }` into `updateTask()`, `undoCompleteTask()`, `undoDeferTask()`, and `applyEmergencyReset()`.
  2. Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).

---

#### BUG-287: Bypassing `TaskBlockPlan.plannedMinutes` in `EnergyTimelineStream.kt` & `FlowScreenV2.kt` Causes Inflated Block Duration Display and Premature Ultradian Rest Buffer
- **Severity**: Medium
- **Category**: Core Logic & UI / Cognitive Load Accounting
- **File**: [`EnergyTimelineStream.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt#L103), [`FlowScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt#L269)
- **Problem Description**:
  In `EnergyTimelineStream.kt` and `FlowScreenV2.kt`, task block duration was read directly from raw `task.estimatedDurationMinutes`:
  - `val end = start + task.estimatedDurationMinutes * 60_000L`
  - `cumulativeMinutes += task.estimatedDurationMinutes`
  - `Text("${task.estimatedDurationMinutes}m")`
  - `topTaskDurationMinutes = topTask?.estimatedDurationMinutes ?: 25`
  For split ultradian tasks (Part 1 and Part 2) or calibrated tasks, `estimatedDurationMinutes` remained the original total (e.g. 90m), whereas the actual reserved block was recorded in `plannedBlockMinutes` (e.g. 45m).
  This caused:
  1. The "NOW" active playhead indicator to miscalculate when the current block ended.
  2. `cumulativeMinutes` to exceed 90m prematurely, displaying unwarranted `UltradianRecoveryDivider` buffers.
  3. Cards displaying "90m" for Part 1 instead of 45m.
- **Root Cause**:
  Directly using raw `estimatedDurationMinutes` rather than `TaskBlockPlan.plannedMinutes(task)`.
- **Suggested / Applied Fix**:
  1. Refactored all duration reads in `EnergyTimelineStream.kt` and `FlowScreenV2.kt` to use `TaskBlockPlan.plannedMinutes(task)`.
  2. Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).

---

#### BUG-288: Missing `habitDate` Updates in `applyEmergencyReset()` & `freshStartRollover()` Leaving Recurring Habits Overdue, and Stale Null Fallback in `EnergyTimelineStream.kt`
- **Severity**: High
- **Category**: Core Data Integrity / Recurring Habit Emergency Reset
- **File**: [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L910), [`EnergyTimelineStream.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt#L441)
- **Problem Description**:
  1. In `FlowViewModelV2.applyEmergencyReset()`, rescheduled kept tasks updated `scheduledDate = todayStart`, but omitted `habitDate = todayStart` for recurring tasks (`recurrence != Recurrence.NONE`). Because `TaskEntity.effectiveScheduledDateMillis()` anchors on `habitDate` for recurring tasks, the task continued to resolve to its old past date, remaining flagged as overdue immediately after reset.
  2. In `FlowViewModelV2.applyEmergencyReset()` and `freshStartRollover()`, recurring tasks moved to the backlog/icebox cleared `scheduledDate = null` but left `habitDate` intact, causing them to leak into today's timeline.
  3. In `EnergyTimelineStream.kt`, line 441 resolved `scheduledMillis` with `task.resolveScheduledEpochMillis() ?: task.scheduledDate`. For recurring tasks with `scheduledDate == null`, fallback was null instead of `task.effectiveScheduledDateMillis()`.
- **Root Cause**:
  Failure to synchronize `habitDate` when updating schedule dates on recurring habits during emergency resets and stream rendering.
- **Suggested / Applied Fix**:
  1. Added `habitDate = if (task.recurrence != Recurrence.NONE) todayStart else task.habitDate` on kept tasks in `applyEmergencyReset()`.
  2. Added `habitDate = if (task.recurrence != Recurrence.NONE) null else task.habitDate` on backlogged tasks in `applyEmergencyReset()` and `freshStartRollover()`.
  3. Updated fallback in `EnergyTimelineStream.kt` to `task.effectiveScheduledDateMillis()`.
  4. Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).

---

#### BUG-289: Premature Completion Status in `FocusViewModelV2` and `FocusService` Causing `TaskRepository.completeAndRecur` Silent Abort and Dropped Completion Lifecycle
- **Severity**: High
- **Category**: Core Logic / Task Completion Lifecycle
- **File**: [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L644-L670), [`FocusService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L236-L255)
- **Problem Description**:
  In `FocusViewModelV2.completeTask()` and `FocusService.handleCompleteAndStop()`, the methods updated the task entity with `status = TaskStatus.COMPLETED` before passing it to `taskRepository.completeAndRecur(updatedTask, now)`.
  In `TaskRepository.kt`:
  ```kotlin
  if (task.status == TaskStatus.COMPLETED) {
      return null
  }
  ```
  This guard was implemented to prevent re-completion of already-completed tasks. Because `updatedTask` was explicitly marked completed before the call, `completeAndRecur` immediately returned null and silently aborted without:
  1. Updating the task status or completion timestamp in Room.
  2. Generating the next occurrence for recurring tasks.
  3. Triggering calendar write-back synchronization.
- **Root Cause**:
  Callers prematurely applied `TaskStatus.COMPLETED` to the entity prior to invoking `TaskRepository.completeAndRecur`, tripping the repository's re-completion guard.
- **Suggested / Applied Fix**:
  1. Updated `FocusViewModelV2` and `FocusService` to retain active status on `taskToComplete` when calling `taskRepository.completeAndRecur(taskToComplete, now)`.
  2. Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).

---

#### BUG-290: Completion Celebration Dialog Modal Trap on Already Completed Tasks in `FocusScreenV2.kt` & Duplicate Completion Inflation in `FocusViewModelV2.kt`
- **Severity**: High
- **Category**: UI & State Management / Completion Modal Trap
- **File**: [`FocusScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt#L520), [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L630-L640)
- **Problem Description**:
  1. In `FocusScreenV2.kt`, `TaskCompletedCelebrationDialog` was displayed conditionally whenever `state.isCompleted` was true:
     `if (state.isCompleted) { TaskCompletedCelebrationDialog(...) }`
     When a user opened or navigated into any task that had already been marked completed, the celebration dialog appeared immediately and could not be dismissed without calling `onNavigateBack()`. This trapped the user, preventing inspection of task micro-steps, duration meters, or notes.
  2. In `FocusViewModelV2.kt`, `completeTask()` lacked an idempotency guard for already-completed tasks (`_uiState.value.isCompleted` or `task?.status == TaskStatus.COMPLETED`), allowing repeated completion triggers that inflated daily completion metrics and streak counters.
- **Root Cause**:
  Coupling transient celebration UI presentation directly to persistent entity completion status (`state.isCompleted`), and missing early-return guard in `FocusViewModelV2.completeTask()`.
- **Suggested / Applied Fix**:
  1. Added `showCompletionCelebration: Boolean = false` to `FocusUiStateV2` and `dismissCompletionCelebration()` to `FocusViewModelV2`.
  2. Updated `FocusScreenV2.kt` to trigger the celebration dialog only on `state.showCompletionCelebration`, and added a persistent "Task Completed ✓" non-clickable badge in primary control actions.
  3. Guarded `FocusViewModelV2.completeTask()` with `if (_uiState.value.isCompleted || _uiState.value.task?.status == TaskStatus.COMPLETED) return`.
  4. Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).

---

#### BUG-291: Autonomy Nudge Orphaned on `FocusService` Direct Completion and Lost on Non-Started Screen Exit in `FocusViewModelV2.kt`
- **Severity**: High
- **Category**: Background Services & Notifications / Autonomy Nudge Synchronization
- **File**: [`FocusService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L220-L240), [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L980-L1010)
- **Problem Description**:
  1. When a task was completed directly via the notification action handled by `FocusService.handleCompleteAndStop()`, the service completed the task in Room and stopped the foreground notification, but omitted `AutonomyNudgeEngine.cancelNudge(applicationContext, activeTaskId)`. This left scheduled WorkManager autonomy reminders active, resulting in phantom notifications popping up for already-completed tasks.
  2. When entering `FocusViewModelV2`, any active autonomy nudge was cancelled to avoid distracting the user during focus. However, if the user exited the screen (`onCleared()`) without starting or completing the session, the nudge was never rescheduled, silencing reminders for tasks that remained uncompleted.
- **Root Cause**:
  Asymmetric autonomy nudge lifecycle: missing cancellation in background service completion and missing fallback rescheduling on uncompleted ViewModel teardown.
- **Suggested / Applied Fix**:
  1. Added `AutonomyNudgeEngine.cancelNudge(applicationContext, activeTaskId)` in `FocusService.handleCompleteAndStop()`.
  2. In `FocusViewModelV2.onCleared()`, restored active task autonomy nudges via `AutonomyNudgeEngine.scheduleNudge(context, task)` if `!state.isCompleted` and timer was never started.
  3. Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).

---

#### BUG-292: Frozen `00:00` Timer Display on Decompression Bypass and Stale Pause Count in `FocusViewModelV2.kt`
- **Severity**: Medium
- **Category**: UI & Timer State / Decompression Zone Transitions
- **File**: [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L360-L470)
- **Problem Description**:
  1. When a focus session ended and transitioned to the decompression zone, `remainingSeconds` reached 0. If the user bypassed the decompression zone via `readyNowBypassDecompression()` or `dismissDecompressionZone()` to prepare for a subsequent round without completing the task (`!state.isCompleted`), `elapsedSeconds` remained at 0 and `remainingSeconds` was not reset to `timerMode.durationSeconds`, displaying a frozen `00:00` timer until manual interaction.
  2. In `FocusViewModelV2.startTimer()`, `pauseCount` was not reset to 0, causing pause counts from prior sessions to accumulate and unfairly penalize focus quality scoring.
- **Root Cause**:
  Incomplete timer state reinitialization when bypassing decompression, and stale pause accumulator across `startTimer()`.
- **Suggested / Applied Fix**:
  1. In `readyNowBypassDecompression()` and `dismissDecompressionZone()`, reset `elapsedSeconds = 0L` and `remainingSeconds = it.timerMode.durationSeconds` when `!it.isCompleted`.
  2. In `FocusViewModelV2.startTimer()`, reset `pauseCount = 0`.
  3. Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).

---

#### BUG-293: Bypassing `TaskBlockPlan.plannedMinutes` Distorts Duration Meters and Estimation Metrics (MAPE/sMAPE) in `FocusScreenV2.kt`, `FocusViewModelV2.kt`, and `FocusService.kt`
- **Severity**: Medium
- **Category**: Core Logic & UI / Cognitive Accounting
- **File**: [`FocusScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt#L295-L350), [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L640-L660), [`FocusService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L240-L260)
- **Problem Description**:
  1. In `FocusScreenV2.kt`, `HeroTaskCard` and `ZenMicroStepSpotlightCard` rendered duration text directly using `task.estimatedDurationMinutes`. For split ultradian tasks (e.g. Part 1 scheduled for 45 minutes from a 90-minute parent task), the header and card displayed "90m" rather than the actual planned block duration of 45 minutes.
  2. In `FocusViewModelV2.completeTask()` and `FocusService.handleCompleteAndStop()`, `mape` and `smape` estimation error formulas compared actual tracked time against raw `task.estimatedDurationMinutes`. For split tasks, tracked time (45m) compared against parent total (90m) yielded a false 50% estimation error, polluting circadian and difficulty calibration telemetry.
  3. In `FocusViewModelV2.autoBreakDownTask()`, the prompt builder used raw `task.estimatedDurationMinutes` rather than `TaskBlockPlan.plannedMinutes(task)`.
- **Root Cause**:
  Reading raw `task.estimatedDurationMinutes` instead of `TaskBlockPlan.plannedMinutes(task)`.
- **Suggested / Applied Fix**:
  1. Updated `HeroTaskCard` and `ZenMicroStepSpotlightCard` to display `TaskBlockPlan.plannedMinutes(task)`.
  2. Updated `mape` and `smape` calculation in `FocusViewModelV2` and `FocusService` to use `TaskBlockPlan.plannedMinutes(task)`.
  3. Updated `autoBreakDownTask()` to pass `TaskBlockPlan.plannedMinutes(task)` to AI breakdown prompt.
  4. Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).

---

#### BUG-294: `attemptSplitPlacement` Unbounded Phase 1 Ceiling Traps Ultradian Continuation Part 2 and Falsely Defers Multi-Hour Tasks
- **Severity**: High
- **Category**: Core Logic & Scheduling / Ultradian Split Verifier
- **File**: [`TaskSchedulerService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L936-L965)
- **Problem Description**:
  In `TaskSchedulerService.scheduleDayWithOutcome`, when an unsplit task failed primary placement, `splitIntoUltradianParts` partitioned the task into Part 1 and Part 2 (e.g. 40m each with a 15m ultradian buffer in between).
  Inside `attemptSplitPlacement(ceilingToUse: Long)`, Phase 1 called `findBestCircadianWindow(..., task = part1, latestAllowedEndMillis = ceilingToUse)`.
  Because `ceilingToUse` was passed directly without headroom for Part 2, `findBestCircadianWindow` was free to place Part 1 in the highest circadian peak slot immediately preceding `ceilingToUse` (e.g. ending within 55 minutes of `ceilingToUse`).
  When this occurred, Phase 2 evaluated Part 2 with `earliestP2Start = p1Window.endMillis + 15m`. Because `earliestP2Start + p2Duration > ceilingToUse`, Phase 2 was mathematically impossible to place before the ceiling.
  Consequently, `attemptSplitPlacement` returned false and rolled back, deferring the entire task with `DeferralReasonCode.INSUFFICIENT_TIME`, even though both parts could easily have been scheduled earlier in the day.
- **Root Cause**:
  Passing the overall deadline ceiling (`ceilingToUse`) directly to Phase 1 for Part 1, rather than bounding Part 1 to `ceilingToUse - 15m - p2DurationMillis`.
- **Suggested / Applied Fix**:
  1. In `attemptSplitPlacement`, computed `val p1Ceiling = if (ceilingToUse != Long.MAX_VALUE) ceilingToUse - (p1BufferMins * 60_000L + p2DurationMillis) else Long.MAX_VALUE`.
  2. Early-returned `false` if `p1Ceiling < earliestStart + p1DurationMillis` to immediately fall back to `structuralCeil` without running Phase 2.
  3. Passed `latestAllowedEndMillis = p1Ceiling` to Phase 1 `findBestCircadianWindow`.
  4. Populated `parentTaskId = task.id` on Part 2 in `splitIntoUltradianParts`.
  5. Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).

---

#### BUG-295: `SchedulePreparationOrchestrator` Future-Day Scheduling Desynchronization for Dateless Timed Tasks Causing False Lock Conflict Deferrals
- **Severity**: High
- **Category**: Core Logic & Orchestration / Multi-Day Epoch Anchoring
- **File**: [`SchedulePreparationOrchestrator.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt#L108-L155)
- **Problem Description**:
  In `SchedulePreparationOrchestrator.orchestrateDayScheduleWithOutcome`, `val dayStartMillis = dayDate.atStartOfDay(zoneId).toInstant().toEpochMilli()` was computed on line 202, well after candidate tasks were mapped into `rawSchedulable` on lines 108–173.
  On line 152, `SchedulableTask` was constructed with `scheduledStartMillis = task.resolveScheduledEpochMillis()`.
  Calling `resolveScheduledEpochMillis()` without parameters defaulted `referenceDateMillis` to `System.currentTimeMillis()`.
  For tasks where `scheduledDate == null` but an intraday time of day was set (e.g. daily recurring habits or dateless timed tasks with `scheduledTime = 10:00 AM`), `resolveScheduledEpochMillis` anchored to today's local midnight instead of the target `dayDate`'s local midnight.
  When preparing a schedule for tomorrow or a future date (e.g. via `PlanViewModelV2.autoScheduleDay()`), the task's `scheduledStartMillis` was evaluated as 24+ hours in the past relative to tomorrow's `freeIntervals`.
  Consequently, `fitsInFreeIntervals(freeIntervals, existingWindow)` failed, and locked tasks (`task.isScheduleLocked == true`) were rejected and wrongly deferred with `"Locked task could not be placed in its pinned time window due to calendar conflict"`.
- **Root Cause**:
  Late computation of `dayStartMillis` and failing to forward the target day's epoch anchor to `task.resolveScheduledEpochMillis(dayStartMillis)`.
- **Suggested / Applied Fix**:
  1. Moved `val dayStartMillis = dayDate.atStartOfDay(zoneId).toInstant().toEpochMilli()` above `rawSchedulable` in `SchedulePreparationOrchestrator.kt`.
  2. Passed `scheduledStartMillis = task.resolveScheduledEpochMillis(dayStartMillis)` during `SchedulableTask` instantiation.
  3. Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).

---

#### BUG-296: `PlacementStability.verdict` Ignores Parameterized `nowMillis` Defaulting to Real-Time System Clock and Skewing Frozen/Rescue Verdicts
- **Severity**: High
- **Category**: Core Logic & Stability / Parameterized Clock Anchoring
- **File**: [`PlacementStability.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/PlacementStability.kt#L48-L68)
- **Problem Description**:
  In `PlacementStability.verdict(task, nowMillis, prefs, forceReplan)`, the method explicitly accepts `nowMillis: Long` to enable deterministic evaluation across background workers, cross-midnight rollovers, and simulation tests.
  However, on line 48, it called `val scheduledMillis = task.resolveScheduledEpochMillis() ?: return ReplanVerdict.KEEP` without arguments, defaulting to `System.currentTimeMillis()`.
  For tasks where `scheduledDate == null` and `scheduledTime != null`, `resolveScheduledEpochMillis` anchored to the real-time system clock's midnight rather than `nowMillis`'s midnight.
  Whenever `nowMillis` represented a simulated time or a different calendar day, `scheduledMillis` and `nowMillis` were desynchronized by multiples of 24 hours.
  This caused tasks to spuriously trip `NEAR_TERM_FREEZE` or `PAST_EXPIRED_RESCUE` based on whatever time the host happened to run the process.
  Furthermore, `taskAge = nowMillis - (task.lastAutoScheduledAt ?: task.updatedAt)` produced negative values on clock skew or forward-dated updates, erroneously falling into `ReplanVerdict.COOLDOWN`.
- **Root Cause**:
  Omitting `nowMillis` in `task.resolveScheduledEpochMillis(nowMillis)` and unbounded negative `taskAge` evaluation.
- **Suggested / Applied Fix**:
  1. Updated line 48 to `val scheduledMillis = task.resolveScheduledEpochMillis(nowMillis) ?: return ReplanVerdict.KEEP`.
  2. Clamped `val taskAge = maxOf(0L, nowMillis - (task.lastAutoScheduledAt ?: task.updatedAt))`.
  3. Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).

---

#### BUG-297: Cognitive Load Meter & Slot Accounting Bypasses `TaskBlockPlan.plannedMinutes` in `AutoSchedulingEngineCoordinator.planSchedule`
- **Severity**: Medium
- **Category**: Core Logic / Cognitive Load Accounting
- **File**: [`AutoSchedulingRationales.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingRationales.kt#L68)
- **Problem Description**:
  In `AutoSchedulingEngineCoordinator.planSchedule(tasks, availableSlotsMinutes, dailyCognitiveCapacityMinutes)`, task duration was evaluated as:
  `val duration = if (task.estimatedDurationMinutes > 0) task.estimatedDurationMinutes else 25`
  For split ultradian tasks (Part 1 and Part 2) or Bayesian-calibrated tasks, `estimatedDurationMinutes` stores the unsplit total (e.g. 90m), whereas the actual planned block is stored in `plannedBlockMinutes` (e.g. 45m).
  Using raw `estimatedDurationMinutes` inflated cognitive load accounting, causing split continuation tasks to falsely trip `COGNITIVE_BUDGET_EXCEEDED` or `INSUFFICIENT_TIME` deferrals.
  Additionally, for untimed tasks (`estimatedDurationMinutes <= 0`), it hardcoded 25m, ignoring `TaskBlockPlan.defaultMinutes(task)` which derives 30m, 45m, or 60m from `effortScore`.
  Similarly, in `TaskSchedulerService.evaluateTaskScoreWithWeights`, `lowFrictionBonus` read raw `task.estimatedMinutes` directly, risking exponential instability if `estimatedMinutes <= 0`.
- **Root Cause**:
  Reading raw duration fields instead of standardizing on `TaskBlockPlan.plannedMinutes(task)`.
- **Suggested / Applied Fix**:
  1. Updated `AutoSchedulingEngineCoordinator.planSchedule` to `val duration = TaskBlockPlan.plannedMinutes(task)`.
  2. Clamped `max(5, task.estimatedMinutes)` in `TaskSchedulerService.evaluateTaskScoreWithWeights`.
  3. Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).

---

#### BUG-298: `subtractInterval` Zero-Width / Inverted Block Artificial Interval Fragmentation in `TaskSchedulerService.kt`
- **Severity**: Medium
- **Category**: Core Logic & Calendar / Free Interval Accounting
- **File**: [`TaskSchedulerService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L1368)
- **Problem Description**:
  In `TaskSchedulerService.subtractInterval(intervals, blocked)`, if a zero-width or inverted window (`blocked.startMillis >= blocked.endMillis`) was passed (e.g. empty nap bout, 0-minute calendar event, or malformed physiological log), the loop sliced any overlapping `[start, end]` window into two adjacent contiguous sub-intervals `[start, cut]` and `[cut, end]`.
  Because `findBestCircadianWindow` searches strictly within each individual element of `freeIntervals`, this zero-width cut artificially fragmented uninterrupted free time.
  Any subsequent task whose duration exceeded either sub-interval could not be placed, causing spurious task deferrals despite 100% continuous calendar availability.
- **Root Cause**:
  Missing early return guard for `blocked.startMillis >= blocked.endMillis` in `subtractInterval`.
- **Suggested / Applied Fix**:
  1. Added `if (blocked.startMillis >= blocked.endMillis) return intervals.toMutableList()` at the beginning of `subtractInterval`.
  2. Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).

---

#### BUG-299: Untimed Tasks in Edit Mode Force `effectiveDuration = 0` Bypassing Task Archetype and Inferred Baseline Duration in `SmartTaskComposerSheetV2.kt`
- **Severity**: High
- **Category**: UI & NLP Inference / Task Duration Collapse
- **File**: [`SmartTaskComposerSheetV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L161-L162)
- **Problem Description**:
  When opening `SmartTaskComposerSheetV2` in `SheetMode.Edit(task)` for an untimed task or a task created without an explicit duration, `existingTask.estimatedDurationMinutes` is `0` (primitive `Int` default in `TaskEntity`).
  The duration evaluation was written as:
  `val effectiveDuration = userDurationOverride ?: if (inferred.isDurationExplicit) inferred.durationMinutes else (existingTask?.estimatedDurationMinutes ?: inferred.durationMinutes)`
  Because `existingTask?.estimatedDurationMinutes` is `0` (which is non-null in Kotlin), the Elvis operator `?: inferred.durationMinutes` never fired.
  As a result:
  - `effectiveDuration` evaluated to `0`.
  - The duration chip in the composer UI displayed `"⏱ 0m"`.
  - Saving the task without manually tapping the duration chip persisted `estimatedDurationMinutes = 0`.
  - The task was stripped of inferred baseline duration based on its task type archetype (e.g. 45m for analytical, 40m for creative, 25m for general) and Bayesian calibration.
- **Root Cause**:
  Using `existingTask?.estimatedDurationMinutes ?: inferred.durationMinutes` on primitive non-nullable `Int` field instead of filtering out `0`.
- **Suggested / Applied Fix**:
  1. Updated to `existingTask?.estimatedDurationMinutes?.takeIf { it > 0 } ?: inferred.durationMinutes`.
  2. Verified in [`SmartTaskComposerSheetV2Test.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2Test.kt).

---

#### BUG-300: Picking Due Time or Scheduled Time Without Explicit Date Clobbers Future `effectiveScheduledDate` / `effectiveDeadlineDate` to Today in `SmartTaskComposerSheetV2.kt`
- **Severity**: High
- **Category**: UI & Boundary Handling / TimePicker Date Clobbering
- **File**: [`SmartTaskComposerSheetV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L276-L285), [`SmartTaskComposerSheetV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L1532-L1542)
- **Problem Description**:
  In `SmartTaskComposerSheetV2.kt`, when a task was scheduled for tomorrow or a future date (either via NLP e.g. `"Review mockups tomorrow"` or by selecting a slot date) and the user subsequently set a due time (e.g. 5:00 PM via TimePicker), lines 1533 and 282 evaluated:
  `userDeadlineDateOverride = initialDate ?: startOfLocalDay(System.currentTimeMillis())`
  `finalDeadlineDate = initialDate ?: startOfLocalDay(System.currentTimeMillis())`
  Because `effectiveDeadlineDate` was null, the deadline date defaulted to today's start of day.
  This caused the task to be scheduled for tomorrow while its deadline was set to today (24+ hours in the past relative to its start time), immediately causing it to be flagged as overdue.
  Symmetrically, setting a scheduled slot time for a task with a future deadline date clobbered `userScheduledDateOverride` and `finalScheduledDate` to today rather than aligning with `effectiveDeadlineDate`.
- **Root Cause**:
  Defaulting missing date targets directly to `initialDate ?: startOfLocalDay(System.currentTimeMillis())` instead of cross-anchoring to `effectiveScheduledDate` or `effectiveDeadlineDate`.
- **Suggested / Applied Fix**:
  1. Updated TimePickerDialog confirmation:
     - For deadline target: `userDeadlineDateOverride = effectiveScheduledDate ?: initialDate ?: startOfLocalDay(System.currentTimeMillis())`
     - For scheduled target: `userScheduledDateOverride = effectiveDeadlineDate ?: initialDate ?: startOfLocalDay(System.currentTimeMillis())`
  2. Updated `handleSave()`:
     - `finalScheduledDate = if (normalizedScheduledTime != null && effectiveScheduledDate == null) effectiveDeadlineDate ?: initialDate ?: startOfLocalDay(System.currentTimeMillis()) else effectiveScheduledDate`
     - `finalDeadlineDate = if (normalizedDeadlineTime != null && effectiveDeadlineDate == null) effectiveScheduledDate ?: initialDate ?: startOfLocalDay(System.currentTimeMillis()) else effectiveDeadlineDate`
  3. Verified in [`SmartTaskComposerSheetV2Test.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2Test.kt).

---

#### BUG-301: `CalendarSyncDataSource.upsertTaskEvent` Defaults to System Clock Midnight For Dateless Timed Tasks and Bypasses `TaskBlockPlan.plannedMinutes`
- **Severity**: High
- **Category**: Integration / Calendar Synchronization & Temporal Anchoring
- **File**: [`CalendarSyncDataSource.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt#L302-L315)
- **Problem Description**:
  In `CalendarSyncDataSource.upsertTaskEvent(task, startMillis, durationMinutes)`:
  1. On line 304, `task.resolveScheduledEpochMillis()` was invoked with zero arguments. For dateless timed tasks (`task.scheduledDate == null`, `task.scheduledTime != null`) where `effectiveDate` is non-null (e.g. from `deadlineDate` or passed context), `referenceDateMillis` defaulted to `System.currentTimeMillis()`. The calendar event was written onto today's date rather than anchoring to `effectiveDate`.
  2. On line 313, when both `plannedBlockMinutes` and `estimatedDurationMinutes` were 0 (untimed tasks), duration fell back to hardcoded `25` minutes instead of delegating to `TaskBlockPlan.plannedMinutes(task)`. This ignored the user's effort score mapping (30m, 45m, 60m) and split continuation task reserved lengths.
  3. Line 405 omitted checking `task.deadlineDate == null`, risking premature deletion of active deadline frogs during status synchronization.
- **Root Cause**:
  Missing reference epoch parameter in `task.resolveScheduledEpochMillis(anchor)`, hardcoded fallback duration bypassing `TaskBlockPlan.plannedMinutes`, and missing `deadlineDate` check in `handleTaskStateChanged`.
- **Suggested / Applied Fix**:
  1. In `upsertTaskEvent`:
     - Computed `val effectiveDate = task.effectiveScheduledDateMillis() ?: task.deadlineDate`
     - Passed `anchor = effectiveDate ?: System.currentTimeMillis()` to `task.resolveScheduledEpochMillis(anchor)`
     - Standardized `effectiveDuration = durationMinutes ?: TaskBlockPlan.plannedMinutes(task)`
  2. In `handleTaskStateChanged`: Added `task.deadlineDate == null` guard to `TaskStatus.ACTIVE` branch.
  3. Verified in [`CalendarWriteBackSyncTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/calendar/CalendarWriteBackSyncTest.kt).

---

#### BUG-302: Newly Composed and Updated Task Tags in `FlowViewModelV2` and `PlanViewModelV2` Dropped from User Preferences `tagCatalog` Until Process Restart
- **Severity**: Medium
- **Category**: Core Data Integrity / Tag Catalog Synchronization
- **File**: [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L566-L785), [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L652-L786)
- **Problem Description**:
  When a user created or updated a task with new tags (via `SmartTaskComposerSheetV2` or note conversion), the tags were saved in `TaskEntity.tags`.
  However, `userPreferencesDataStore.mergeTagCatalog(...)` was never invoked from ViewModel task creation/update actions (`FlowViewModelV2.addTask`, `FlowViewModelV2.updateTask`, `FlowViewModelV2.addTaskFromNote`, `PlanViewModelV2.addTask`, `PlanViewModelV2.updateTask`, `PlanViewModelV2.createTaskFromNote`).
  Because `TaskTagViewModel` (used on `SettingsScreen.kt`) and `HistoryViewModel.kt` observe `userPreferencesDataStore.preferencesFlow.map { it.tagCatalog }`, new tags remained completely invisible in Settings, Tag Management, and History until the app process was terminated and re-initialized (which runs `NeuroFlowApplication` startup seeding).
  Furthermore, in `FlowViewModelV2.addTask`, if `task.recurrence != Recurrence.NONE` and `task.scheduledDate == null`, it updated `scheduledDate = dayStart` but left `habitDate = null`, causing recurring habit date desynchronization.
- **Root Cause**:
  Missing `preferencesDataStore.mergeTagCatalog` invocations in ViewModel write paths and omitted `habitDate` binding in `FlowViewModelV2.addTask`.
- **Suggested / Applied Fix**:
  1. Added tag normalization and `preferencesDataStore.mergeTagCatalog(newTags)` calls across `addTask`, `updateTask`, and `addTaskFromNote`/`createTaskFromNote` in both `FlowViewModelV2` and `PlanViewModelV2`.
  2. Bounded `habitDate = if (task.recurrence != Recurrence.NONE) dayStart else task.habitDate` in `FlowViewModelV2.addTask`.
  3. Verified in [`TaskTagAndCategoryTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/common/TaskTagAndCategoryTest.kt).

---

#### BUG-303: `FocusService` Sprint Completion Notification Cancelled Immediately by `onDestroy()` Race Condition and Omitted Home Screen Widget Refresh on Complete
- **Severity**: High
- **Category**: Background Services / Notification & Service Lifecycle Race
- **File**: [`FocusService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L212-L266)
- **Problem Description**:
  When a focus sprint timer expired (`timing.isExpired == true`), `FocusService` called `showSprintCompletedNotification()`, posting the "🎯 Sprint Complete: $title" notification with `NOTIFICATION_ID` (3001) and immediately calling `stopForeground(STOP_FOREGROUND_DETACH)` and `stopSelf()`.
  When Android destroyed `FocusService`, `onDestroy()` executed and called `nm?.cancel(NOTIFICATION_ID)` (ID 3001).
  This race condition instantly cancelled and cleared the sprint completion notification within milliseconds of posting it, preventing the user from ever seeing or tapping the completion notification.
  Additionally, completing a task via the notification action button in `handleCompleteAndStop` failed to invoke `FocusWidgetUpdateWorker.enqueueImmediateUpdate(applicationContext)`, leaving Home Screen widgets displaying stale active task data.
- **Root Cause**:
  Reusing the active foreground service notification ID (`NOTIFICATION_ID = 3001`) for the terminal sprint completion notification, which is subsequently purged by `onDestroy()`, and missing widget update enqueue on direct notification task completion.
- **Suggested / Applied Fix**:
  1. Introduced distinct `NOTIFICATION_ID_COMPLETED = 3002` for completed sprint notifications.
  2. In `startTicker()`, removed the ongoing notification (3001) with `stopForeground(STOP_FOREGROUND_REMOVE)` and posted the completion notification under `NOTIFICATION_ID_COMPLETED` (3002), which cleanly survives `onDestroy()`.
  3. In `handleCompleteAndStop`, enqueued `FocusWidgetUpdateWorker.enqueueImmediateUpdate(applicationContext)` upon task completion.
  4. Verified in [`FocusWidgetAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/widget/FocusWidgetAuditTest.kt).

---

#### BUG-304: Future Deadline Date (`task.deadlineDate > dayEnd`) Falsely Suppresses Autonomy Nudges for Tasks Scheduled for Today in `AutonomyNudgeWorker.kt`
- **Severity**: High
- **Category**: Background Services & Notifications / Autonomy Nudge False Suppression
- **File**: [`AutonomyNudgeWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/AutonomyNudgeWorker.kt#L60-L63)
- **Problem Description**:
  In `AutonomyNudgeWorker.doWork()`, the future task filter was written as:
  `if ((sDate != null && sDate > dayEnd) || (task.deadlineDate != null && task.deadlineDate > dayEnd)) return Result.success()`
  Because of the unconditional `|| (task.deadlineDate != null && task.deadlineDate > dayEnd)`, any task scheduled for today (`sDate <= dayEnd`) that had a deadline in the future (e.g. tomorrow, in 3 days, or at the end of the week) triggered the condition and was prematurely suppressed.
  Users received zero autonomy nudges on the scheduled day for any task with a future deadline.
- **Root Cause**:
  Checking `deadlineDate > dayEnd` without guarding with `sDate == null`. When `sDate` is present, it is the primary scheduling anchor and a non-immediate deadline must not classify the task as a future task.
- **Suggested / Applied Fix**:
  1. Updated filter to:
     `if ((sDate != null && sDate > dayEnd) || (sDate == null && task.deadlineDate != null && task.deadlineDate > dayEnd)) return Result.success()`
  2. Verified in [`AutonomyNudgeWorkerTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/worker/AutonomyNudgeWorkerTest.kt).

---

#### BUG-305: `AutonomyNudgeEngine` Omitted from `TaskRepository` Write Paths Leading to Missing Autonomy Nudges for Background Auto-Scheduled & Recurring Tasks
- **Severity**: High
- **Category**: Core Data Integrity & Background Services / Autonomy Nudge Lifecycle Desynchronization
- **File**: [`AutonomyNudgeEngine.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/engine/AutonomyNudgeEngine.kt#L16), [`TaskRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L167-L184)
- **Problem Description**:
  `AutonomyNudgeEngine` previously required a `Context` instance (`scheduleNudge(context: Context, task: TaskEntity)`). Because `TaskRepository` has `WorkManager` injected rather than `Context`, `scheduleNudge` was never invoked from `TaskRepository.insert` or `TaskRepository.update` (only `cancelNudge` was called on inactive tasks).
  As a result:
  - Background auto-scheduling passes executed by `ScheduleAutoTasksWorker` updated task placement in Room but never scheduled or refreshed the autonomy nudge.
  - New recurring habit occurrences generated by `completeAndRecur` never received an autonomy nudge.
  - Tasks created via widgets or background receivers had no nudge scheduled.
- **Root Cause**:
  Missing `WorkManager` overload in `AutonomyNudgeEngine` and omitted `scheduleNudge` invocation in `TaskRepository.insert` and `update`.
- **Suggested / Applied Fix**:
  1. Added `scheduleNudge(workManager: WorkManager, task: TaskEntity)` to `AutonomyNudgeEngine.kt` and delegated `scheduleNudge(context: Context, task: TaskEntity)` to it.
  2. Wired `AutonomyNudgeEngine.scheduleNudge(workManager, ...)` into `TaskRepository.insert` and `TaskRepository.update` for `TaskStatus.ACTIVE` tasks.
  3. Verified in [`AutonomyNudgeWorkerTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/worker/AutonomyNudgeWorkerTest.kt).

---

#### BUG-306: `DailyPlanWorker` Unconditionally Bans Split Part 2 Tasks and Bypasses `TaskBlockPlan.plannedMinutes` Across Workers & Widgets
- **Severity**: Medium
- **Category**: Background Services & UI / Continuation Task Exclusion & Cognitive Budget Meter
- **File**: [`NotificationWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L177-L228), [`FocusWidgetUpdateWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L104-L187), [`ScheduleAutoTasksWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L378)
- **Problem Description**:
  1. In `NotificationWorker.kt` (`DailyPlanWorker`), line 177 unconditionally filtered out `!task.id.endsWith("_part2")`. When Part 1 of a multi-hour task was completed earlier, Part 2 (which is the active continuation task for today) was permanently banned from appearing in the morning "Your top tasks today" notification.
  2. In `DailyPlanWorker` line 228 and `FocusWidgetUpdateWorker.kt` lines 104 and 187, `SchedulableTask` and the widget meta string used raw `task.estimatedDurationMinutes` instead of `TaskBlockPlan.plannedMinutes(task)`. Untimed tasks (0 duration) bypassed user effort score mapping (30m, 45m, 60m) and archetype defaults, displaying "Quick" or 0m in widget metadata and distorting cognitive scoring in background workers.
  3. In `ScheduleAutoTasksWorker.kt` line 378 and `PlanViewModelV2.kt` line 591, when Part 2 was created afresh (`existingPart2 == null`), it copied `freshTask` directly without resetting `sessionCount = 0`, `totalTimeTrackedMinutes = 0f`, `actualDurationMinutes = 0f`, inheriting Part 1's focus history and falsely causing `AutonomyNudgeWorker` to treat Part 2 as already started/in-progress (`task.sessionCount > 0`).
- **Root Cause**:
  Hardcoded `_part2` exclusion without checking parent task completion status, bypassing `TaskBlockPlan.plannedMinutes`, and missing session metric initialization on newly split continuation tasks.
- **Suggested / Applied Fix**:
  1. In `DailyPlanWorker`, updated candidate filtering to:
     `val isPart2Blocked = task.id.endsWith("_part2") && ((task.parentTaskId != null && task.parentTaskId in activeTaskIds) || task.id.removeSuffix("_part2") in activeTaskIds)` and checked `!isPart2Blocked`.
  2. Standardized `TaskBlockPlan.plannedMinutes(task)` across `DailyPlanWorker` and `FocusWidgetUpdateWorker`.
  3. Explicitly reset `sessionCount = existingPart2?.sessionCount ?: 0`, `totalTimeTrackedMinutes = existingPart2?.totalTimeTrackedMinutes ?: 0f`, `actualDurationMinutes = existingPart2?.actualDurationMinutes ?: 0f`, and `lastSessionDurationMinutes = existingPart2?.lastSessionDurationMinutes ?: 0f` when creating `part2Entity` in `ScheduleAutoTasksWorker.kt` and `PlanViewModelV2.kt`.
  4. Verified in [`NotificationWorkerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/worker/NotificationWorkerAuditTest.kt) and [`FocusWidgetAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/widget/FocusWidgetAuditTest.kt).

---

#### BUG-307: Non-Nullable String Parameter in Room `@TypeConverter` Methods Crashes on SQLite NULL Values with Uncaught `NullPointerException`
- **Severity**: High
- **Category**: Room Database / Enum Deserialization & Crash Resilience
- **File**: [`Converters.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/Converters.kt#L21-L100)
- **Problem Description**:
  In `Converters.kt`, all enum deserializer methods (`toQuadrant`, `toPriority`, `toTaskStatus`, `toRecurrence`, `toEnergyLevel`, `toTaskType`, `toRewardTier`, `toHyperFocusState`) took non-nullable `value: String`.
  In Room's generated DAO implementation code (e.g., `TaskDao_Impl.java`), `_cursor.getString(...)` is queried and passed directly into `__converters.toQuadrant(_tmp)`.
  If any row in the SQLite database contained a `NULL` column value (resulting from partial schema migrations, manual database edits, corrupted backup restorations, or legacy schema drift), Kotlin's compiled bytecode inserted an implicit `Intrinsics.checkNotNullParameter(value, "value")`. This threw an unhandled `NullPointerException` before the method body or its internal `try-catch` block could execute, crashing query dispatch and preventing task retrieval throughout the app.
- **Root Cause**:
  Non-nullable String parameter types in Room type converters preventing graceful fallback on SQLite NULL values.
- **Suggested / Applied Fix**:
  1. Updated all enum converter signatures in `Converters.kt` to accept `String?`.
  2. Wrapped conversions with `value?.let { try { ... } catch ... } ?: DefaultEnum` so null or invalid values safely default to valid enum values (`Quadrant.SCHEDULE`, `Priority.MEDIUM`, `TaskStatus.ACTIVE`, `Recurrence.NONE`, `EnergyLevel.MEDIUM`, `TaskType.ANALYTICAL`, `RewardTier.NONE`, `HyperFocusState.INACTIVE`).
  3. Verified in [`RoomDatabaseAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/local/RoomDatabaseAuditTest.kt).

---

#### BUG-308: `TaskRepository.delete` Leaves Orphaned Subtasks, Retains Deadlocked Part 2 Prerequisites, and Leaks Zombie Open Time Sessions
- **Severity**: High
- **Category**: Room Database & Concurrency / Cascade Deletion & Dependency Deadlock
- **File**: [`TaskRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L262-L310), [`TaskDao.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/TaskDao.kt#L73-L77), [`TimeSessionDao.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/TimeSessionDao.kt#L45-L49)
- **Problem Description**:
  1. When a task was deleted in `TaskRepository.delete(task)`, the method only looked up `val part2 = taskDao.getById("${task.id}_part2")`. Any subtasks linked via `parentTaskId == task.id` (such as sequential sessions produced by `splitTaskIntoSessions`) were never queried or deleted, remaining in the `tasks` table as orphaned rows with invalid parent references.
  2. When `part2` was deleted, the dependency cleanup loop below only checked `if (task.id in currentDeps)`. Any active task that depended on `part2.id` (or child subtask IDs) retained the dependency on the deleted Part 2 entity. Because Part 2 was deleted from the database and would never be completed, the dependent task was permanently deadlocked from being auto-scheduled or executed.
  3. While `focus_sessions` rows were cascade-deleted by SQLite foreign keys, `time_sessions` rows have no foreign key constraint. Open time sessions (`endedAt == null`) for the deleted task were neither closed nor deleted. Consequently, `sessionRepository.getOpenSessions()` leaked ghost sessions indefinitely, causing `ScheduleAutoTasksWorker` and `PlanViewModelV2` to falsely treat the deleted task as currently focused.
- **Root Cause**:
  Missing subtask queries and deletions in `TaskDao` (`getSubtasks`, `deleteSubtasksByParentId`), missing cleanup of `part2.id` and subtask IDs from dependency graphs, and omission of `timeSessionDao.deleteOpenSessionsByTaskId` on task deletion.
- **Suggested / Applied Fix**:
  1. Added `getSubtasks(parentId)` and `deleteSubtasksByParentId(parentId)` queries to `TaskDao.kt`.
  2. Added `deleteOpenSessionsByTaskId(taskId)` and `deleteByTaskId(taskId)` to `TimeSessionDao.kt` and `SessionRepository.kt`.
  3. In `TaskRepository.delete(task)`, collected all deleted task IDs (`deletedIds = {task.id, part2?.id, ...subtaskIds}`) and deleted external reminders, nudges, calendar events, micro-steps, and open time sessions for all of them; deleted the tasks and subtasks; and stripped all `deletedIds` from active tasks' `dependsOnTaskIds`.
  4. Verified in [`RoomDatabaseAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/local/RoomDatabaseAuditTest.kt).

---

#### BUG-309: `splitTaskIntoSessions` Wipes Out Prerequisites, Schedule Dates, and External Dependencies on Part 1 and Falsely Unlocks Downstream Tasks Early
- **Severity**: High
- **Category**: Core Logic & Scheduling / Task Splitting Integrity & Dependency Preservation
- **File**: [`TaskRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L330-L390)
- **Problem Description**:
  In `TaskRepository.splitTaskIntoSessions(task, sessionDurationMinutes)`:
  1. For Part 1 (`i == 1`), `dependsOnTaskIds` was set to `previousSessionId` (`""`), completely erasing any prerequisites `task.dependsOnTaskIds` previously held. Part 1 could run before its prerequisites were fulfilled.
  2. `scheduledDate` and `scheduledTime` were omitted, collapsing Part 1 to unscheduled (`null`) even if the parent task was scheduled for today at a specific time.
  3. External dependency `waitingFor` was dropped for Part 1.
  4. When parent `task` was archived as `COMPLETED`, other tasks in the system that depended on `task.id` immediately saw the prerequisite as satisfied (since parent `status == COMPLETED`), scheduling downstream work before any of the newly split session parts were actually executed.
  5. Splitting used direct `taskDao.insert` instead of `insert(sessionTask)`, bypassing reminder and autonomy nudge workers.
- **Root Cause**:
  Missing inheritance of scheduling/prerequisite fields on Part 1 and failure to redirect downstream task dependencies to the final split part upon archiving the parent task.
- **Suggested / Applied Fix**:
  1. For Part 1 (`i == 1`), preserved `dependsOnTaskIds = task.dependsOnTaskIds`, `scheduledDate = task.scheduledDate`, `scheduledTime = task.scheduledTime`, and `waitingFor = task.waitingFor`.
  2. Replaced `taskDao.insert` with `insert(sessionTask)` so WorkManager reminders and autonomy nudges are registered.
  3. Redirected all active downstream tasks depending on `task.id` to depend on `createdSessions.last().id` before archiving the parent task.
  4. Verified in [`RoomDatabaseAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/local/RoomDatabaseAuditTest.kt).

---

#### BUG-310: Non-Idempotent Legacy Micro-Step Extraction in `MIGRATION_21_22` Inserts Duplicate Steps on Migration Re-runs
- **Severity**: Medium
- **Category**: Database Migrations & Data Integrity / Micro-Step Deduplication
- **File**: [`NeuroFlowDatabase.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/NeuroFlowDatabase.kt#L430-L450)
- **Problem Description**:
  In step 7 of `MIGRATION_21_22`, legacy micro-steps were extracted from `tasks.ifThenPlan` using:
  `INSERT INTO task_micro_steps` with `UUID.randomUUID().toString()`.
  If the migration was retried or executed on a database where `task_micro_steps` already had entries, new random UUIDs were generated and duplicate micro-step rows were inserted for each step. Furthermore, the random UUIDs mismatched the deterministic ID scheme used by `TaskRepository.getMicroSteps` (`"${task.id}_step_$index"`).
- **Root Cause**:
  Using non-idempotent `INSERT INTO` with non-deterministic random UUIDs during SQLite migration.
- **Suggested / Applied Fix**:
  Changed SQL statement to `INSERT OR IGNORE INTO task_micro_steps` and standardized `stepId` generation to `"${taskId}_step_$index"` in `MIGRATION_21_22`.
- **Status**: Resolved (verified in unit tests).

---

#### BUG-311: Intent Extras Retained Across Configuration Changes Cause Repeated Intent Re-execution, Closed Task Reopening, and Volatile State Reset in `MainActivity.kt`
- **Severity**: High
- **Category**: Android Lifecycle / Intent Dispatch & Configuration Change State Synchronization
- **File**: [`MainActivity.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L72-L184)
- **Problem Description**:
  1. In `MainActivity.handleIntent(intent)`, when handling notification or widget launch intents with `taskId` or `task_id`, `intent.action` was set to `null`, but the intent extras (`"taskId"`, `"task_id"`, `"openWoop"`, `"notificationId"`) were never removed.
     When the Activity was subsequently destroyed and recreated across configuration changes (e.g. screen rotation, dark/light theme switch, multi-window split), Android passed the original `Intent` to the new activity instance.
     Because `val hasPendingAction = !intent.action.isNullOrBlank() || intent.hasExtra("taskId") || intent.hasExtra("task_id")`, `hasPendingAction` evaluated to `true` repeatedly on every configuration change, completely bypassing `savedInstanceState` and re-invoking `handleIntent(intent)`.
     As a result, `initialFocusTaskId` was repeatedly resurrected, violently forcing the user back into a task focus session they had already finished or backed out of.
  2. In `MainActivity.setContent`, session tracking flags `freshStartHandled`, `yearlyGoalHandled`, `weeklyGoalHandled`, and `directIntentSessionHandled` were instantiated with volatile `remember { mutableStateOf(...) }` instead of `rememberSaveable { mutableStateOf(...) }` and were omitted from `onSaveInstanceState`.
     If the user opened the app via a direct notification or widget intent on a fresh-start or goal refill day and subsequently rotated the screen during their focus sprint, `directIntentSessionHandled` and `freshStartHandled` reset to `false`.
     This caused `NewChapterCard` or `TopGoalsRefillCard` to suddenly pop up in composition, unmounting `NeuroFlowAppV2` and destroying the user's active focus timer.
  3. When an incoming intent specified `action == "com.procus.ACTION_OPEN_FOCUS"` alongside extra `"openWoop" == true`, `handleIntent` unconditionally assigned `initialFocusTaskId`, ignoring `initialWoopTaskId` reflection routing.
- **Root Cause**:
  Missing `removeExtra` on handled launch intent extras, using volatile `remember` instead of `rememberSaveable` for session lifecycle guard flags, and missing `openWoop` check inside `ACTION_OPEN_FOCUS`.
- **Suggested / Applied Fix**:
  1. In `MainActivity.handleIntent(intent)`, added explicit clean-up calls: `intent.action = null`, `intent.removeExtra("taskId")`, `intent.removeExtra("task_id")`, `intent.removeExtra("openWoop")`, and `intent.removeExtra("notificationId")`.
  2. In `MainActivity.setContent`, replaced `remember` with `rememberSaveable` for `freshStartHandled`, `yearlyGoalHandled`, `weeklyGoalHandled`, and `directIntentSessionHandled`.
  3. Checked `openWoop` inside `ACTION_OPEN_FOCUS` to route correctly to `initialWoopTaskId`.
  4. Verified in [`NavigationAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/navigation/NavigationAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-312: Unencoded Task IDs, Missing `launchSingleTop`, and Blank Target Vulnerability in Navigation Routes in `NeuroFlowAppV2.kt`
- **Severity**: Medium
- **Category**: Navigation / Route Encoding & Backstack Duplication
- **File**: [`NeuroFlowAppV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L80-L87)
- **Problem Description**:
  1. In `NeuroFlowAppV2.kt`, lines 226, 236, and 292 routed focus starts via raw string interpolation: `navController.navigate("focus_v2/$taskId")` without `Uri.encode(taskId)` and without `launchSingleTop = true`.
     If a task ID contained path separators (e.g. split continuation tasks, calendar sync IDs, hierarchical subtask IDs) or query/hash tokens (`/`, `#`, `?`, `&`, spaces), Navigation Compose failed to match the destination route and crashed with `IllegalArgumentException: Navigation destination ... cannot be found`. Rapid double-tapping on task cards also pushed duplicate instances onto the backstack.
  2. In `onNavigateToNextTask`, if `nextTaskId` was blank or empty, calling `navController.navigate("focus_v2/${Uri.encode(nextTaskId)}")` produced route `"focus_v2/"`, crashing Navigation Compose.
  3. In `LaunchedEffect(initialTaskId)` for `"tab_focus"` and `LaunchedEffect(initialOpenComposer)`, if `currentRoute` was empty string `""` (during the initial attachment frame before NavHost destination is established), `currentRoute != NavHubsV2.Flow.route` evaluated to `true`, triggering a redundant navigation pass on top of `NavHost`'s start destination.
- **Root Cause**:
  Raw string interpolation without `Uri.encode` and `launchSingleTop = true` in Flow, Plan, and WeeklyScheduleScreen callbacks, unvalidated `nextTaskId`, and unanchored `currentRoute` checks on cold start.
- **Suggested / Applied Fix**:
  1. Wrapped all focus navigation callbacks in `FlowScreenV2`, `PlanScreenV2`, and `WeeklyScheduleScreen` with `if (taskId.isNotBlank())` and navigated using `android.net.Uri.encode(taskId)` with `launchSingleTop = true`.
  2. Guarded `onNavigateToNextTask` with `if (nextTaskId.isNotBlank())`, falling back cleanly to `navController.popBackStack()`.
  3. Guarded start-destination routing in `LaunchedEffect` with `currentRoute.isNotEmpty() && currentRoute != NavHubsV2.Flow.route`.
  4. Verified in [`NavigationAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/navigation/NavigationAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-313: Dynamic Circadian Peak Ignored in `PlanScreenV2.kt` Timeline Header & Peak Badge Desynchronization with `shiftTaskToPeak`
- **Severity**: Medium
- **Category**: UI & State Synchronization / Circadian Peak Calibration
- **File**: [`PlanScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L855)
- **Problem Description**:
  In `TimelineScheduleView`, `peakStart` and `peakEnd` were computed strictly from static `state.preferences.effectivePeakStart` and `state.preferences.peakEnergyStart`.
  `state.circadianSummary?.peakWindowStartHour` and `state.circadianSummary?.peakWindowEndHour`, which are dynamically calculated by `CircadianEnergyService` from real-time sleep logs, naps, stimulants, and chronotype profiles, were completely bypassed.
  As a result:
  1. The header banner displayed static peak hours (e.g. 09:00–12:00) instead of the actual dynamic circadian peak window (e.g. 14:00–17:00).
  2. Tasks scheduled during the actual peak failed to display the "PEAK" badge on the timeline.
  3. When the user tapped "Shift to Peak", `PlanViewModelV2.shiftTaskToPeak` (which correctly uses `circadianSummary.peakWindowStartHour`) shifted the task to the dynamic peak hour (e.g. 14:00), which then did not match the timeline's header banner, creating a jarring UX mismatch.
- **Root Cause**:
  Hardcoded preference fallback omitting `state.circadianSummary?.peakWindowStartHour` and `peakWindowEndHour` in `TimelineScheduleView`.
- **Suggested / Applied Fix**:
  Updated `peakStart` and `peakEnd` in `TimelineScheduleView` to prioritize `state.circadianSummary?.peakWindowStartHour` and `state.circadianSummary?.peakWindowEndHour`, falling back to `effectivePeakStart` and `peakEnergyStart` in `state.preferences`. Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-314: `TaskBlockPlan.plannedMinutes` Bypassed in `PlanScreenV2.kt` Timeline & Matrix Views Causing Inflated Display and Premature Ultradian Recovery Rest Buffer
- **Severity**: Medium
- **Category**: UI & Core Logic / Cognitive Load Accounting
- **File**: [`PlanScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L824)
- **Problem Description**:
  1. In `PlanScreenV2.kt`, `shouldShowUltradianBufferAfter()` accumulated raw `task.estimatedDurationMinutes`. For an ultradian-split task (e.g. 90m split into Part 1 and Part 2 of 45m each), Part 1 retains `estimatedDurationMinutes = 90`. Thus `runSum` reached 90m immediately after Part 1, erroneously displaying a 15-minute `UltradianRecoveryDivider` ahead of time!
  2. In `TimelineScheduleView` (line 1111) and `MatrixPlanView` (line 1490), task metadata displayed `"${task.estimatedDurationMinutes}m"` instead of `"${TaskBlockPlan.plannedMinutes(task)}m"`, presenting incorrect inflated block durations on split and calibrated tasks.
- **Root Cause**:
  Direct inspection of `task.estimatedDurationMinutes` instead of `TaskBlockPlan.plannedMinutes(task)`.
- **Suggested / Applied Fix**:
  Replaced all occurrences of `task.estimatedDurationMinutes` in `shouldShowUltradianBufferAfter`, timeline task subtitle, and matrix task cards with `com.neuroflow.app.domain.scheduler.TaskBlockPlan.plannedMinutes(task)`. Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-315: `PlanViewModelV2.autoScheduleDay` Treats Unscheduled Backlog Tasks & Icebox Tasks as Fixed Busy Windows Blocking Daily Auto-Scheduling
- **Severity**: High
- **Category**: Core Logic & Scheduling / Phantom Busy Windows
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L518)
- **Problem Description**:
  In `PlanViewModelV2.autoScheduleDay()`, `fixedWindows` calculation suffered from two critical defects:
  1. It did not exclude tasks with `quadrant == Quadrant.ELIMINATE`. Cut/icebox tasks with an old time setting were included as busy windows, blocking auto-scheduled tasks from valid slots.
  2. For non-candidate tasks (e.g. locked or non-mutable), `task.resolveScheduledEpochMillis(dateMillis)` anchored dateless tasks (`scheduledDate == null`) to `dateMillis`. If an unscheduled task in the backlog had a lingering `scheduledTime` (e.g. 10:00 AM), it was treated as a fixed busy window on *every* day the user selected, permanently blocking 10:00 AM across the entire calendar.
- **Root Cause**:
  Missing `quadrant != Quadrant.ELIMINATE` filter and missing verification that `task.effectiveScheduledDateMillis()` (or dual-epoch timestamp) actually matches `dayStart..dayEnd` before adding to `fixedWindows`.
- **Suggested / Applied Fix**:
  Filtered out `it.quadrant == Quadrant.ELIMINATE` and verified that the task is scheduled on `dayStart..dayEnd` (`(sDate != null && sDate in dayStart..dayEnd) || (isDualEpoch && task.scheduledTime in dayStart..dayEnd)`) before including in `fixedWindows`. Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-316: `PlanViewModelV2.uncompleteTask` and `moveTaskToQuadrant` Omit Autonomy Nudge Rescheduling and Calendar Sync Synchronization
- **Severity**: High
- **Category**: Background Services & Notifications / Autonomy Nudge Synchronization
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L689)
- **Problem Description**:
  1. In `PlanViewModelV2.uncompleteTask()`, when a completed task was restored to `ACTIVE` (e.g. via the interactive Undo snackbar on `PlanScreenV2`), `AutonomyNudgeEngine.scheduleNudge(context, updated)` was omitted. The restored task was left in the daily plan without background autonomy nudges.
  2. In `PlanViewModelV2.moveTaskToQuadrant()`, `calendarSyncRepository?.handleTaskStateChanged(updated)` was never invoked, leaving external calendar events orphaned or desynchronized. Furthermore, `AutonomyNudgeEngine` was not updated, leaving active alarms firing for tasks moved to `Quadrant.ELIMINATE` (cut/icebox).
- **Root Cause**:
  Missing `AutonomyNudgeEngine` and `calendarSyncRepository` coordination calls in `uncompleteTask()` and `moveTaskToQuadrant()`.
- **Suggested / Applied Fix**:
  Added `AutonomyNudgeEngine.scheduleNudge(it, updated)` and user feedback emission to `uncompleteTask()`. In `moveTaskToQuadrant()`, updated `isFrog` status, invoked `calendarSyncRepository?.handleTaskStateChanged(updated)`, and cancelled or scheduled nudges based on whether the target quadrant is `Quadrant.ELIMINATE`. Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-317: `PlanViewModelV2` Tag Filtering Ignores `task.contextTag` Hiding Matching Tasks for Starter Tags (#deepwork, #offline)
- **Severity**: Medium
- **Category**: UI & Task Filtering / Context Tag Desynchronization
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L309)
- **Problem Description**:
  In `PlanViewModelV2.uiState`, tag filter matching checked only `task.tags.split(",")`. However, the domain model separates comma-delimited `task.tags` and `task.contextTag` (e.g. `deepwork`, `offline`, `admin`).
  When the user selected `#deepwork` or `#offline` from the starter tag filter chips, tasks where the archetype context was saved into `contextTag` were falsely excluded from the daily plan and matrix views.
- **Root Cause**:
  Omitting `task.contextTag` from `tagFilteredAll` and `dynamicTags` aggregation in `PlanViewModelV2`.
- **Suggested / Applied Fix**:
  Updated `dynamicTags` and `tagFilteredAll` to inspect `(task.tags.split(",") + task.contextTag)`, harmonizing behavior with `FlowViewModelV2`. Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-318: Dual-Representation Absolute Epoch Timestamps (`scheduledTime >= ONE_DAY_MILLIS`) Dropped from `tasksForDay` in `PlanViewModelV2.kt`
- **Severity**: High
- **Category**: UI & Data Integrity / Temporal Epoch Representation
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L348)
- **Problem Description**:
  In `PlanViewModelV2.uiState`, `tasksForDay` filtered tasks strictly by `task.effectiveScheduledDateMillis() in dayStart..dayEnd` or `resolveDeadlineEpochMillis()`.
  If a task was created or scheduled with `scheduledTime >= ONE_DAY_MILLIS` (dual-representation absolute epoch timestamp) and `scheduledDate == null`, `effectiveScheduledDateMillis()` returned `null`.
  If the task had no deadline, it was completely dropped from `tasksForDay` and falsely relegated to `unscheduledTasks`, even though its scheduled epoch fell directly on that day.
- **Root Cause**:
  Missing check for `task.scheduledTime >= ONE_DAY_MILLIS && task.scheduledTime in dayStart..dayEnd` in `tasksForDay` and `unscheduledTasks` filters.
- **Suggested / Applied Fix**:
  Added dual-representation timestamp check `rawScheduledTime != null && rawScheduledTime >= ONE_DAY_MILLIS && rawScheduledTime in dayStart..dayEnd` to `tasksForDay`, excluded dual-epoch scheduled tasks from `unscheduledTasks`, and added secondary sorting by `isFrog` and `createdAt`. Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-319: Absolute Epoch Timestamps (`scheduledTime >= ONE_DAY_MILLIS`) Ignored in `FlowViewModelV2.kt` Leaking Future-Scheduled Tasks into Today and Dropping Today's Timed Tasks
- **Severity**: High
- **Category**: UI & Data Integrity / Temporal Epoch Representation
- **File**: [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L363)
- **Problem Description**:
  In `FlowViewModelV2.uiState`, task inclusion in `actionableActiveTasks` and `todayActiveTasks` inspected only `task.effectiveScheduledDateMillis()` (which checks `task.scheduledDate` or `task.habitDate`).
  1. When a task was scheduled with an absolute epoch timestamp (`scheduledTime >= ONE_DAY_MILLIS`) in the future (e.g. tomorrow or next week) and `scheduledDate == null`, `sDate` and `dDate` were `null`. The condition `sDate == null && dDate == null` evaluated to `true`, treating the future task as an unscheduled/today task, rendering it in `todayActiveTasks`, and even promoting it as today's top recommended action (`recommendedTask`) in the Spotlight Hero Card.
  2. Conversely, when a task was scheduled for today with an absolute epoch timestamp but possessed a deadline next week (`dDate > dayEnd`), `sDate` was `null` and `dDate > dayEnd`. The conditions `sDate != null && sDate <= dayEnd` and `dDate <= dayEnd` and `dDate == null` all evaluated to `false`, causing tasks explicitly scheduled for today to be dropped completely from `todayActiveTasks` and `actionableActiveTasks`.
- **Root Cause**:
  Failing to resolve the effective scheduled date anchor when `scheduledTime >= ONE_DAY_MILLIS` in `actionableActiveTasks` and `todayActiveTasks`.
- **Suggested / Applied Fix**:
  Unified schedule date resolution to `task.effectiveScheduledDateMillis() ?: task.scheduledTime?.takeIf { it >= ONE_DAY_MILLIS }?.let { startOfLocalDay(it) }` in both `actionableActiveTasks` and `todayActiveTasks`. Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-320: Tag Filtering and Dynamic Tag Aggregation Ignore `task.contextTag` in `FlowViewModelV2.kt`
- **Severity**: Medium
- **Category**: UI & Task Filtering / Context Tag Desynchronization
- **File**: [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L351)
- **Problem Description**:
  1. `dynamicTags` in `FlowViewModelV2` aggregated tags solely from `activeTasks.flatMap { it.tags.split(",") }`, omitting `task.contextTag`.
  2. In `filteredTasks`, matching logic checked only `task.tags.split(",")`. Tasks where starter archetypes (`deepwork`, `offline`, `admin`) were populated into `task.contextTag` were falsely excluded when the user selected that filter chip.
  3. In `todayCompletedDeepWorkMinutes`, deep work accounting inspected `task.tags.contains("deepwork", ignoreCase = true)` but omitted `task.contextTag.equals("deepwork", ignoreCase = true)`, leading to undercounted deep work minutes in the circadian energy summary.
- **Root Cause**:
  Omitting `task.contextTag` from tag aggregation, filter matching, and deep work completion calculation.
- **Suggested / Applied Fix**:
  Harmonized with `PlanViewModelV2` by checking `(task.tags.split(",") + task.contextTag)` for both `dynamicTags` and `hasMatchingTag`, and checking `task.contextTag.equals("deepwork", ignoreCase = true)` in `todayCompletedDeepWorkMinutes`. Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-321: Manual Peak Override and Absolute Timestamp Epoch Anchoring Ignored in `FlowViewModelV2.shiftTaskToPeak()`
- **Severity**: High
- **Category**: Core Logic & Scheduling / Dynamic Peak Calibration & Multi-Day Anchoring
- **File**: [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L722)
- **Problem Description**:
  1. In `FlowViewModelV2.shiftTaskToPeak()`, `peakHour` resolved strictly from `summary?.peakWindowStartHour` or preference defaults. When the user configured and enabled a manual circadian peak profile (`prefs.manualPeakProfileEnabled`), `prefs.manualPeakAnchorMinuteOfDay` was completely ignored, desynchronizing behavior with `PlanViewModelV2.shiftTaskToPeak()`.
  2. `baseDate` was resolved as `task.effectiveScheduledDateMillis() ?: now`. When shifting a task scheduled for a future day with an absolute epoch timestamp (`scheduledDate == null`, `scheduledTime >= ONE_DAY_MILLIS`), `baseDate` collapsed to `now`, overwriting the future date and snapping the task to today or tomorrow.
- **Root Cause**:
  Missing `prefs.manualPeakProfileEnabled` check and omission of `scheduledTime >= ONE_DAY_MILLIS` in `baseDate` fallback resolution.
- **Suggested / Applied Fix**:
  Added manual peak check `val manualPeak = if (prefs.manualPeakProfileEnabled) (prefs.manualPeakAnchorMinuteOfDay / 60).coerceIn(0, 23) else null` to `peakHour` resolution, and anchored `baseDate` to `task.scheduledTime` when `scheduledTime >= ONE_DAY_MILLIS`. Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-322: Split Continuation Indicator in `EnergyTimelineStream.kt` Omits `_part` ID Suffix and Evening Shutdown Null `completedAt` Fallback
- **Severity**: Low
- **Category**: UI & Boundary Handling / Split Task Badge & Completion Resiliency
- **File**: [`EnergyTimelineStream.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt#L408), [`FlowScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt#L343)
- **Problem Description**:
  1. In `TimelineTaskCard`, ultradian split continuation detection checked `task.id.contains("#part") || task.title.contains("(Part ")`. Since split continuation tasks generated across `TaskRepository`, `PlanViewModelV2`, and `ScheduleAutoTasksWorker` utilize `_part2` (or `_part1`) IDs, tasks with customized titles lost their `🔗 Part 2/2` or `🔗 Part 1/2` badge.
  2. In `FlowScreenV2.kt`, the evening shutdown ritual completed task counter checked `val comp = it.completedAt ?: return@count false`. Tasks completed or migrated where `completedAt` was null were excluded from the evening conquered task count even if `it.updatedAt >= todayStart`.
- **Root Cause**:
  Omission of `_part` and `_part2` substring matching in split badge detection, and lack of `updatedAt` fallback for completed task counting.
- **Suggested / Applied Fix**:
  Added `task.id.contains("_part")` and `_part2` to `isUltradianPart` and `partLabel`, and fell back to `it.updatedAt` when `it.completedAt` is null in `FlowScreenV2.kt`. Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-323: Foreground Service Notification ID Collision in `FocusService.updateNotification()` Spawns Duplicate Notification and Leaves Primary Notification Frozen
- **Severity**: High
- **Category**: Background Services & Notifications / Notification Channel & ID Collision
- **File**: [`FocusService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L276)
- **Problem Description**:
  In `FocusService`, the foreground service is started via `startForeground(NOTIFICATION_ID, notification)` where `NOTIFICATION_ID = 3001`.
  However, in `FocusService.updateNotification()`, which is called every 1 second during live countdowns and upon state transitions (start, pause, resume), the method invoked:
  `nm.notify(NOTIFICATION_ID_COMPLETED, notif)` where `NOTIFICATION_ID_COMPLETED = 3002`.
  This introduced two critical bugs:
  1. It spawned a duplicate, separate notification in the Android notification shade under ID 3002, while the primary foreground service notification (ID 3001) was never updated and remained frozen at its initial 00:00 or start timestamp.
  2. On service teardown (`stopForegroundAndSelf()` and `onDestroy()`), only `NOTIFICATION_ID` (3001) was cancelled, leaving the second notification (3002) permanently orphaned and stuck in the user's status bar notification tray.
- **Root Cause**:
  `updateNotification()` incorrectly targeted terminal completion notification ID `NOTIFICATION_ID_COMPLETED` (3002) rather than the active ongoing foreground service notification ID `NOTIFICATION_ID` (3001).
- **Suggested / Applied Fix**:
  Updated `updateNotification()` to target `NOTIFICATION_ID` (3001). Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-324: External Task Completion Ignored When Sprint Completed, Causing Double Completion and Metric Inflation in `FocusViewModelV2.observeActiveSession()`
- **Severity**: High
- **Category**: UI & State Synchronization / External Service Synchronization
- **File**: [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L328)
- **Problem Description**:
  In `FocusViewModelV2.observeActiveSession()`, when `serviceSession == null`, the ViewModel only inspected `if (_uiState.value.isRunning || _uiState.value.isPaused)`.
  When a focus sprint expired naturally (`isSprintCompleted = true`, `isRunning = false`, `isPaused = false`) and the user completed the task externally (via the notification action button or Home Screen widget), `FocusService` cleared its active session (`activeSession = null`) and updated the task to `COMPLETED` in the database.
  Because neither `isRunning` nor `isPaused` was true in `FocusViewModelV2`, `observeActiveSession()` ignored the session termination, leaving `FocusScreenV2` displaying `SprintCompletionSheet` with active action buttons. If the user subsequently tapped "Complete Task" on the sheet, `FocusViewModelV2.completeTask()` executed against the already completed task, calling `taskRepository.completeAndRecur()` again and double-incrementing `prefs.totalTasksCompleted`.
- **Root Cause**:
  Gating external completion synchronization on `isRunning || isPaused` instead of checking `!_uiState.value.isCompleted` when `serviceSession == null`.
- **Suggested / Applied Fix**:
  Updated `observeActiveSession()` to check `if (!_uiState.value.isCompleted)`, dismiss `SprintCompletionSheet`, show completion celebration, and synchronize task entity state. Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-325: Dual-Representation Timestamp Epoch Leak & Blocked Prerequisite/Continuation Tasks in `FocusViewModelV2` Next Task Queue Ranking
- **Severity**: High
- **Category**: Core Logic & Task Queue / Temporal Epoch & Prerequisite Validation
- **File**: [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L134)
- **Problem Description**:
  In `FocusViewModelV2.loadTask()`, next actionable task resolution exhibited multiple ordering and filtering deficiencies:
  1. Candidate tasks were sorted using `.thenBy { it.effectiveScheduledDateMillis() ?: Long.MAX_VALUE }`. For tasks scheduled with an absolute epoch timestamp (`scheduledDate == null`, `scheduledTime >= ONE_DAY_MILLIS`), `effectiveScheduledDateMillis()` returned `null`, collapsing the effective date to `Long.MAX_VALUE`. Tasks scheduled for today at specific hours were falsely deprioritized behind unscheduled tasks or tasks scheduled weeks/months into the future with explicit dates.
  2. Candidate `next` tasks did not filter out blocked tasks: Part 2 continuation tasks whose parents were still active (`isPart2Blocked`), tasks with active prerequisites in `dependsOnTaskIds`, or tasks blocked on external dependencies (`waitingFor.isNotBlank()`).
  3. When `completeTask()` executed, `nextTaskId` was not refreshed to reflect newly unblocked tasks (e.g. Part 2 of the completed task).
- **Root Cause**:
  Omission of `scheduledTime >= ONE_DAY_MILLIS` in effective date resolution, lack of dependency and continuation validation in next task candidate filtering, and static unrefreshed `nextTaskId` across completion.
- **Suggested / Applied Fix**:
  Introduced `findNextActionableTask()` filtering out `isPart2Blocked`, active prerequisites, and `waitingFor`, standardizing effective date resolution to `task.effectiveScheduledDateMillis() ?: task.scheduledTime?.takeIf { it >= ONE_DAY_MILLIS }?.let { startOfLocalDay(it) } ?: Long.MAX_VALUE`, and refreshing `next` in `completeTask()`. Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-326: Decompression Zone Exit Leaves Stale `startedAtMillis` Fabricating Phantom Focus Minutes on Subsequent Direct Completion in `FocusViewModelV2.kt`
- **Severity**: Medium
- **Category**: UI & Timer State / Decompression Zone Transitions
- **File**: [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L964-L990)
- **Problem Description**:
  When a focus sprint completes, focus minutes are already credited to the task and preferences.
  When the user enters the decompression zone via `takeDecompressionRest()`, `isSprintCompleted` is set to `false`.
  Upon exiting or bypassing the decompression zone (`readyNowBypassDecompression()` / `dismissDecompressionZone()`), `elapsedSeconds` was reset to `0L`, but `startedAtMillis` remained set to the prior sprint's start timestamp (`startedAtMillis > 0L`).
  If the user subsequently clicked the primary "Complete Task" button without starting another timer, `completeTask()` evaluated `hasRunTimer = durationSec > 0L || startedAtMillis > 0L == true`. Because `durationSec == 0L`, `actualMinutes = max(1, 0) = 1`. Because `wasSprintCompleted` was false, this fabricated a phantom 1 minute added to both `task.totalTimeTrackedMinutes` and `prefs.totalFocusMinutes`.
- **Root Cause**:
  Failing to reset `startedAtMillis = 0L`, `accumulatedPauseMillis = 0L`, and `pausedAtMillis = null` when exiting decompression zone with an incomplete task.
- **Suggested / Applied Fix**:
  Reset `startedAtMillis = 0L`, `accumulatedPauseMillis = 0L`, and `pausedAtMillis = null` in both `readyNowBypassDecompression()` and `dismissDecompressionZone()` when `!it.isCompleted`. Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-327: Micro-Step Deletion on Empty Heuristic Fallback in `FocusViewModelV2.autoBreakDownTask()` & `ZenMicroStepSpotlightCard` 0m Badge
- **Severity**: Low
- **Category**: UI & Data Integrity / Micro-Step Preservation & Duration Floor
- **File**: [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L1002), [`FocusScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt#L301)
- **Problem Description**:
  1. In `FocusViewModelV2.autoBreakDownTask()`, if `behavioralSupportService.generateMicroStepsWithAi` returned an empty list (e.g. offline heuristic failure or network timeout), `taskRepository.deleteMicroStepsByTaskId(task.id)` was executed unconditionally, purging all existing micro-steps and wiping them from the task plan.
  2. In `FocusScreenV2.kt`, `ZenMicroStepSpotlightCard` estimated minutes per step was computed via integer division `(plannedMinutes) / activeMicroSteps.size` without a minimum floor, rendering `~0m target` for short tasks.
  3. `HeroTaskCard` next step preview ignored `skippedMicroStepIndices`.
- **Root Cause**:
  Missing empty-list guard in `autoBreakDownTask()`, missing floor on estimated step duration in `ZenMicroStepSpotlightCard`, and omission of skipped index checking in `HeroTaskCard`.
- **Suggested / Applied Fix**:
  Added `if (generated.isEmpty()) return@launch` in `autoBreakDownTask()`, enforced `maxOf(1, ...)` in `ZenMicroStepSpotlightCard` step duration, and accounted for `skippedMicroStepIndices` in `HeroTaskCard`. Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-328: Continuation Part 2 Duration Wiped and Title Clobbered in `SchedulePreparationOrchestrator.kt`
- **Severity**: High
- **Category**: Core Logic & Scheduling / Split Continuation Duration Preservation
- **File**: [`SchedulePreparationOrchestrator.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt#L62-L85)
- **Problem Description**:
  In `SchedulePreparationOrchestrator.prepareTasksForScheduling()`, when preparing tasks for auto-scheduling, continuation tasks (e.g. `_part2`, `#part2`, `parentTaskId != null`, or `(Part 2/)`) had their duration calibration and metadata clobbered:
  1. Title sanitization logic stripped `(Part 2/2)` from titles, causing split continuation tasks to duplicate the parent task name and lose their part distinction in UI and scheduler logs.
  2. For split tasks without explicit user duration override (`isDurationExplicit == false`), `calibrationRatio` scaling was applied unconditionally, shrinking or expanding the split session unexpectedly.
  3. Tasks with 0 estimated duration had their `plannedBlockMinutes` set to 0 rather than preserving the split block minutes allocated during the ultradian partition.
- **Root Cause**:
  Lack of continuation task isolation in `SchedulePreparationOrchestrator`, stripping part titles and applying default duration overrides that clobbered the calculated split session duration.
- **Suggested / Applied Fix**:
  Isolated Part 2 continuation tasks (`isPart2Continuation = id.contains("_part2") || id.contains("#part2") || parentTaskId != null || title.contains("(Part 2/")`), preserving `plannedBlockMinutes`, maintaining title suffix, and setting `isDurationExplicit = true`. Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-329: `TaskSchedulerService.kt` Omits `_part2` Suffix and `parentTaskId` in Ultradian Recovery Buffer and Completion End Time Tracking
- **Severity**: High
- **Category**: Core Logic & Scheduling / Ultradian Recovery Buffer & Split Tracking
- **File**: [`TaskSchedulerService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L206-L250)
- **Problem Description**:
  In `TaskSchedulerService.kt`, ultradian recovery buffer enforcement (`ULTRADIAN_RECOVERY_BUFFER_MINUTES = 15`) and completion tracking previously only recognized dynamic split task IDs containing `#part2`. Persisted Room entities use the `${baseId}_part2` convention and link `parentTaskId`.
  Consequently:
  1. Tasks dependent on Part 2 (`isUltradianDep` or `isUltradianPrereq`) failed to trigger the mandatory 15-minute ultradian cognitive recovery buffer after Part 1 completed or Part 2 was scheduled.
  2. `completionEndMap` was never updated for `parentId` when `_part2` finished placement, causing tasks that depended on the parent task to use Part 1's end time (or 0) rather than the true completion of the entire split task.
- **Root Cause**:
  Hardcoded string checks using only `.endsWith("#part2")` or `.substringBefore("#part")` rather than matching both `_part2` and `#part2` as well as verifying `parentTaskId`.
- **Suggested / Applied Fix**:
  Expanded Part 2 checks across `isUltradianPrereq`, `isUltradianDep`, and `completionEndMap` updates to detect `_part2`, `#part2`, and `parentTaskId`. Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-330: Downstream Tasks Scheduled Concurrently or Ahead of Split Continuation Part 2 Due to Missing Topological Chaining in `DependencyGraphValidator.kt`
- **Severity**: High
- **Category**: Core Logic & Scheduling / Dependency Graph Sanitization & Topological Chaining
- **File**: [`DependencyGraphValidator.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/DependencyGraphValidator.kt#L80-L115)
- **Problem Description**:
  When a multi-hour task `A` is split into `A_part1` and `A_part2`, any downstream task `B` that had `A` in its `dependsOnTaskIds` was only dependent on `A` (which becomes Part 1). In `DependencyGraphValidator.sanitizeDag`, when `A` was placed, `B`'s prerequisite count immediately reached zero, allowing `B` to be scheduled in the interval between `A_part1` and `A_part2`, or even concurrently with `A_part2`.
  This violated task sequencing invariants: tasks depending on `A` must not begin until the entire task (including its continuation part) completes.
- **Root Cause**:
  `DependencyGraphValidator.sanitizeDag` did not re-link or chain downstream dependencies to the continuation part (`_part2` / `#part2`) when the parent task was split.
- **Suggested / Applied Fix**:
  In `sanitizeDag`, detect when a continuation task exists for parent `P` (matching `id.contains("_part") || id.contains("#part")` with `parentTaskId == P.id`), and re-route downstream dependencies of `P` to wait for the continuation part instead. Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-331: Intraday Deadlines Without Explicit `deadlineDate` Ignored by `resolveDeadlineEpochMillis()` in `TaskTimeExtensions.kt`
- **Severity**: Medium
- **Category**: Core Data Integrity / Deadline Epoch Resolution
- **File**: [`TaskTimeExtensions.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/entity/TaskTimeExtensions.kt#L53-L73)
- **Problem Description**:
  `task.resolveDeadlineEpochMillis()` previously required `deadlineDate != null` to calculate the epoch deadline. If a task was created or edited with only `deadlineTime` (intraday deadline, e.g. "by 5:00 PM") but `deadlineDate` was null, `resolveDeadlineEpochMillis()` returned `null`.
  As a result:
  1. Reverse-DAG deadline ceilings in `TaskSchedulerService` ignored the deadline, allowing the task to be pushed to the evening or into tomorrow.
  2. Quadrant calculations and urgency scoring treated the task as having no deadline.
- **Root Cause**:
  Strict `if (deadlineDate == null) return null` check without falling back to `habitDate`, `scheduledDate`, or the parameterized reference date.
- **Suggested / Applied Fix**:
  Updated `resolveDeadlineEpochMillis(referenceDateMillis: Long = System.currentTimeMillis())` to fall back to `habitDate`, `scheduledDate`, and `referenceDateMillis` when `deadlineDate` is null but `deadlineTime != null`. Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-332: Downstream Child Unlocking Infinite Loop and Inconsistent Suffix Deferral Sweeps in `TaskSchedulerService.kt`
- **Severity**: High
- **Category**: Core Logic & Concurrency / Ready Queue Unlocking & Deferral Cleanup
- **File**: [`TaskSchedulerService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L530-L580)
- **Problem Description**:
  1. When unlocking child tasks in `TaskSchedulerService`, `children[parentId]` contained `_part2`. If `_part2` was placed, unlocking `children[parentId]` would attempt to unlock `_part2` again, re-enqueueing it into `readyQueue` and creating a scheduling loop or duplicate placements.
  2. In deferral sweeps, `cleanSplitTaskId(id: String)` only stripped `#part`, leaving `_part` intact and causing deferred continuation tasks to lose tracking with their parent task across subsequent scheduling passes.
  3. Pre-existing anchored Part 1 tasks in hysteresis placement did not reserve the 15-minute ultradian rest buffer before Part 2 or downstream tasks.
- **Root Cause**:
  Self-unlocking of Part 2 from `children[parentId]`, incomplete regex/substring cleaning in `cleanSplitTaskId`, and omission of title/suffix detection in hysteresis buffer reservation.
- **Suggested / Applied Fix**:
  Excluded `child.id != current.id` when unlocking `children[parentId]` and deduplicated `childrenToUnlock`, added unified `cleanSplitTaskId(id: String)` stripping both `#part` and `_part`, and added `_part1` and `(Part 1/2)` support to hysteresis interval subtraction. Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).
"""

with open("BUG_REPORT.md", "w") as f:
    f.write(base_content + additions + "\n")

print("Successfully written BUG_REPORT.md")

---

#### BUG-333: Cleared Scheduled Date & Deadline Date Resurrected on Timed Tasks in `SmartTaskComposerSheetV2.kt`
- **Severity**: High
- **Category**: UI & Boundary Handling / Date Clearing Clobbering
- **File**: [`SmartTaskComposerSheetV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L280-L290)
- **Problem Description**:
  When a user clicked the "Clear" (✕) button on either the Scheduled Slot Date picker or the Deadline Date picker in `SmartTaskComposerSheetV2`, the state variables `scheduledDateCleared` or `deadlineDateCleared` were properly set to `true`, and `effectiveScheduledDate` or `effectiveDeadlineDate` evaluated to `null`.
  However, in `handleSave()`:
  ```kotlin
  val finalScheduledDate = if (normalizedScheduledTime != null && effectiveScheduledDate == null) {
      effectiveDeadlineDate ?: initialDate ?: startOfLocalDay(System.currentTimeMillis())
  } else { effectiveScheduledDate }
  ```
  Because the check only evaluated `effectiveScheduledDate == null`, any task that had a timed component (`scheduledTime` or `deadlineTime`, e.g. from NLP inference or an existing task entity) immediately resurrected `finalScheduledDate` or `finalDeadlineDate` to `initialDate` or today's epoch start-of-day. The user's explicit clearing of the date was discarded, making it impossible to clear dates on timed tasks.
- **Root Cause**:
  Omission of `!scheduledDateCleared` and `!deadlineDateCleared` guards in `handleSave()` fallback date resolution.
- **Suggested / Applied Fix**:
  Added `&& !scheduledDateCleared` and `&& !deadlineDateCleared` to `handleSave()` date fallback logic. Verified in [`SmartTaskComposerSheetV2Test.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2Test.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-334: `contextTag` Dropped in Create Mode and Stale Tag Retained on Removal in Edit Mode in `SmartTaskComposerSheetV2.kt`
- **Severity**: High
- **Category**: UI & Data Integrity / Context Tag Synchronization
- **File**: [`SmartTaskComposerSheetV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L127-L350)
- **Problem Description**:
  1. In `SmartTaskComposerSheetV2`, `initialTags` was initialized exclusively from `existingTask?.tags?.split(",")`, completely ignoring `existingTask.contextTag`. If an existing task had its primary tag stored in `contextTag` (e.g. `deepwork`), the tag was missing from the composer's active chip bar.
  2. In `SheetMode.Create`, `TaskEntity(...)` did not assign `contextTag`, leaving it as `""` even when the user selected starter or context tags.
  3. In `SheetMode.Edit`, `existingTask.copy(...)` omitted `contextTag`. If a user removed a tag (e.g. `#deepwork`), the entity retained `contextTag = "deepwork"` in Room. Consequently, the task continued to match the removed tag in `PlanViewModelV2` and `FlowViewModelV2` tag filter bars, clustered incorrectly in `TaskSchedulerService`, and distorted analytics in `AnalyticsEngine`.
- **Root Cause**:
  Omission of `contextTag` in `initialTags`, `TaskEntity` construction, and `existingTask.copy()` in `SmartTaskComposerSheetV2`.
- **Suggested / Applied Fix**:
  Included `existingTask.contextTag` in `initialTags`, dynamically resolved `effectiveContextTag` ensuring deleted tags are cleared from `contextTag`, and assigned `contextTag = effectiveContextTag` across both Create and Edit branches. Verified in [`SmartTaskComposerSheetV2Test.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2Test.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-335: User Note Title Overwritten by AI Extracted Title on Note-to-Task Conversion in `QuickNotesBottomSheet.kt`
- **Severity**: Medium
- **Category**: UI & State Synchronization / Note Title Preservation
- **File**: [`QuickNotesBottomSheet.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/notes/QuickNotesBottomSheet.kt#L213-L325)
- **Problem Description**:
  1. In `QuickNotesBottomSheet.kt`, when converting an unsaved scratchpad note to a task via `onConvertToTaskClick`, `onCreateTaskSynthesis(finalSyn, content)` and `onCreateTask(finalSyn.extractedTaskTitle, content)` passed `finalSyn.extractedTaskTitle` (the AI synthesized title), ignoring the user's explicit title typed in the `title` text field.
  2. In the recent notes list, converting an existing note evaluated `inferred.sanitizedTitle.ifBlank { note.title.ifBlank { note.content.take(35) } }`. Because `inferred.sanitizedTitle` was extracted from the note body, it clobbered the note's existing title `note.title`.
- **Root Cause**:
  Passing un-modified `finalSyn` to `onCreateTaskSynthesis` instead of prioritizing the user's explicit note title, and prioritizing body NLP sanitization over `note.title`.
- **Suggested / Applied Fix**:
  Prioritized the user's explicit title: `val finalTaskTitle = title.trim().ifBlank { finalSyn.extractedTaskTitle }`, updating `finalSyn.copy(extractedTaskTitle = finalTaskTitle)` before calling `onCreateTaskSynthesis` and `onCreateTask`. For existing notes, prioritized `note.title.trim().takeIf { it.isNotBlank() }`. Verified in [`SmartTaskComposerSheetV2Test.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2Test.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-336: Calendar Event Duplication After Cloud Sync Wipes Android-Proprietary Custom Columns in `CalendarSyncDataSource.kt`
- **Severity**: High
- **Category**: Integration / Calendar Synchronization & Cloud Sync Resilience
- **File**: [`CalendarSyncDataSource.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt#L260-L360)
- **Problem Description**:
  `CalendarSyncDataSource.findEventIdByTaskId(taskId)` relied exclusively on querying `CalendarContract.Events.CUSTOM_APP_PACKAGE` and `CalendarContract.Events.CUSTOM_APP_URI`.
  External calendar sync adapters (Google Calendar Sync, Samsung Calendar, Exchange, DavX5) sync events via the standard RFC 5545 iCalendar protocol. Because iCalendar does not define `customAppPackage` or `customAppUri`, these proprietary Android columns are wiped / set to `null` during cloud synchronization round-trips.
  On subsequent scheduling or status update passes, `findEventIdByTaskId` failed to find the existing event and inserted a new duplicate event in the user's calendar on every synchronization cycle.
- **Root Cause**:
  Lack of an RFC 5545 compliant fallback marker in calendar event descriptions to correlate task entities across cloud sync cycles.
- **Suggested / Applied Fix**:
  1. Appended standard marker `[neuroflow:task:${task.id}]` to `CalendarContract.Events.DESCRIPTION` on `upsertTaskEvent`.
  2. Added fallback in `findEventIdByTaskId` querying `DESCRIPTION LIKE '%[neuroflow:task:$taskId]%'` when custom columns are null.
  3. Enforced a minimum duration floor of 5 minutes on `effectiveDuration` to prevent invalid zero-duration calendar events.
  Verified in [`CalendarWriteBackSyncTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/calendar/CalendarWriteBackSyncTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-337: Comma-Separated Tag Strings Ingested as Single Entries in `TaskTagViewModel.kt` and `UserPreferencesDataStore.kt`
- **Severity**: Medium
- **Category**: Core Data Integrity / Tag Normalization & Catalog Ingestion
- **File**: [`TaskTagViewModel.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/common/TaskTagViewModel.kt#L48), [`UserPreferencesDataStore.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/UserPreferencesDataStore.kt#L891)
- **Problem Description**:
  `TaskTagViewModel.normalizeTags` and `UserPreferencesDataStore.mergeTagCatalog` stripped `#` and `@` prefixes from input strings, but did not split by comma (`","`).
  When `seedFromTasks` was called with task tag strings (e.g. `listOf("deepwork, coding", "study, #exercise")`) or when a user entered multiple tags separated by commas in Settings (e.g. `"admin, finance"`), the entire un-split string `"deepwork, coding"` was inserted into `tagCatalog` as a single tag token. This caused malformed chip labels like `"#deepwork, coding"` in filter bars and composer pickers.
- **Root Cause**:
  Missing `flatMap { it.split(",") }` in tag normalization pipelines.
- **Suggested / Applied Fix**:
  Added `flatMap { it.split(",") }` to both `TaskTagViewModel.normalizeTags` and `UserPreferencesDataStore.mergeTagCatalog`. Verified in [`TaskTagAndCategoryTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/common/TaskTagAndCategoryTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-338: Dateless Timed Tasks with Absolute Epoch Timestamp Return Null Effective Scheduled Date Leaking Future Tasks into Widgets & Schedules
- **Severity**: High
- **Category**: Core Data Integrity / Temporal Epoch Representation
- **File**: [`TaskTimeExtensions.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/entity/TaskTimeExtensions.kt#L19-L38)
- **Problem Description**:
  In `TaskEntity.effectiveScheduledDateMillis()`, the method checked:
  ```kotlin
  val time = scheduledTime ?: return null
  val date = scheduledDate ?: habitDate
  if (date != null) return date
  return null
  ```
  However, in PROCUS's dual-representation timestamp architecture, timed tasks can carry absolute epoch millisecond timestamps (`scheduledTime >= ONE_DAY_MILLIS`) even when `scheduledDate` and `habitDate` are null (e.g. from calendar synchronization, direct intent placement, or specific time pickers).
  Because `effectiveScheduledDateMillis()` returned `null`:
  1. `FocusWidgetUpdateWorker`'s `isScheduledForFuture()` evaluated `effectiveScheduledDateMillis() > dayEnd` which yielded `false`. Future-scheduled tasks were deemed not-future and leaked into today's home screen focus widgets.
  2. `DailyPlanWorker` and `AutonomyNudgeWorker` similarly failed to recognize future-dated tasks, causing them to leak into today's morning briefing.
  3. `ScheduleAutoTasksWorker` misclassified them as unscheduled tasks.
- **Root Cause**:
  Omission of `if (time >= ONE_DAY_MILLIS) startOfLocalDay(time)` fallback in `effectiveScheduledDateMillis()`.
- **Suggested / Applied Fix**:
  Added epoch timestamp fallback to `effectiveScheduledDateMillis()`:
  ```kotlin
  if (time >= ONE_DAY_MILLIS) return startOfLocalDay(time)
  ```
  Also added explicit `scheduledStart > dayEnd` guards in `FocusWidgetUpdateWorker.kt` and `NotificationWorker.kt`. Verified in [`FocusWidgetAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/widget/FocusWidgetAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-339: `FocusService` Notification Actions Drop Task Identity Extras and Missing `startedAtMillis` Guard Fabricates 50 Years Focus Time
- **Severity**: High
- **Category**: Background Services / Notification Action Extras & State Restoration
- **File**: [`FocusService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L130-L330)
- **Problem Description**:
  1. When building foreground notification action `PendingIntent`s (`ACTION_PAUSE`, `ACTION_RESUME`, `ACTION_COMPLETE`), `FocusService` created intents without attaching `EXTRA_TASK_ID`, `EXTRA_TASK_TITLE`, `EXTRA_DURATION_SECONDS`, or `EXTRA_STARTED_AT_MILLIS`. If Android OS Low-Memory Killer (LMK) killed the service process while running or paused in the background, clicking "Complete" or "Pause" restarted the service with `intent.getStringExtra(EXTRA_TASK_ID) == null`. The service initialized with blank `taskId = ""`, silently aborting `handleCompleteAndStop()` or session updates and leaving the task uncompleted.
  2. In `handleCompleteAndStop()`, `rawDurationMs = now - startedAtMillis` was computed unconditionally. If `startedAtMillis <= 0L` (as when restarted with an empty intent), `rawDurationMs` evaluated to `System.currentTimeMillis() - 0L` (~1.77 billion seconds / ~50 years), fabricating astronomical focus times and corrupting user telemetry.
- **Root Cause**:
  Missing intent extras on action pending intents and missing guard on uninitialized `startedAtMillis`.
- **Suggested / Applied Fix**:
  1. Attached `EXTRA_TASK_ID`, `EXTRA_TASK_TITLE`, `EXTRA_DURATION_SECONDS`, `EXTRA_STARTED_AT_MILLIS`, and `EXTRA_ACCUMULATED_PAUSE_MILLIS` to all notification action PendingIntents.
  2. Added fallback task identity restoration in `onStartCommand` if `taskId.isBlank()`.
  3. Guarded `rawDurationMs` calculation against `startedAtMillis <= 0L`, falling back to `targetDurationSeconds * 1000L` or 0L.
  Verified in [`FocusService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-340: Sprint Completion Alerts Posted to Low-Importance Silent Live Channel Suppressing Notification Sound and Vibration
- **Severity**: Medium
- **Category**: Background Services & Notifications / Notification Channel Configuration
- **File**: [`FocusService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L30-L50)
- **Problem Description**:
  When a focus sprint timer finished, `FocusService.showSprintCompletedNotification()` posted the completion notification using `CHANNEL_ID = "focus_live"`.
  However, `CHANNEL_ID` is created with `NotificationManager.IMPORTANCE_LOW`, silent sound (`setSound(null, null)`), and vibration disabled (`enableVibration(false)`) specifically for the persistent ongoing live ticker.
  Consequently, when the user's focus sprint ended while their screen was off or they were in another app, the completion notification arrived completely silently with no heads-up banner, no sound, and no vibration alert.
- **Root Cause**:
  Reusing the low-importance ongoing live ticker channel for transient high-importance sprint completion alerts.
- **Suggested / Applied Fix**:
  Created a dedicated `CHANNEL_ID_COMPLETED = "focus_completed"` with `NotificationManager.IMPORTANCE_HIGH`, default notification sound, and vibration enabled (`enableVibration(true)`), and directed `showSprintCompletedNotification()` to post to this channel. Verified in [`FocusWidgetAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/widget/FocusWidgetAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-341: `CompleteTaskWidgetAction` Drops Session Focus Minutes and Telemetry Across Direct Widget Task Completions
- **Severity**: High
- **Category**: UI & Data Integrity / Widget Session Tracking & Telemetry
- **File**: [`CompleteTaskWidgetAction.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/widget/CompleteTaskWidgetAction.kt#L45-L75)
- **Problem Description**:
  When a task was marked completed directly from the home screen widget while an open focus session was active, `CompleteTaskWidgetAction` closed the open session via `sessionRepository.closeSession(openSession.id, now, durationMinutes)`.
  However, it then called `taskRepository.markTaskCompleted(taskId)`. `markTaskCompleted()` only sets `status = TaskStatus.COMPLETED` and `completedAt = now`; it did not increment `task.totalTimeTrackedMinutes`, `task.actualDurationMinutes`, `task.sessionCount`, or `userPreferencesDataStore.totalFocusMinutes`.
  As a result, all focus work tracked in that session was permanently lost from the task entity and user profile statistics, distorting completion velocity and MAPE metrics.
- **Root Cause**:
  Calling simple status-flipping `markTaskCompleted()` rather than accumulating session focus minutes and telemetry before completing the task.
- **Suggested / Applied Fix**:
  Updated `CompleteTaskWidgetAction` to fetch the task entity, add `durationMinutes` to `task.totalTimeTrackedMinutes` and `task.actualDurationMinutes`, increment `sessionCount`, update estimation error telemetry (MAPE/sMAPE), persist via `taskRepository.update(updatedTask)`, call `userPreferencesDataStore.addFocusMinutes(durationMinutes)`, and then invoke `taskRepository.markTaskCompleted(taskId)`. Verified in [`FocusWidgetAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/widget/FocusWidgetAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-342: Expired Focus Sessions Permanently Lock Tasks as Focused Blocking Auto-Scheduling and Daily Replanning
- **Severity**: High
- **Category**: Core Logic & Scheduling / Focus Session Expiration
- **File**: [`ScheduleAutoTasksWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L236), [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L525)
- **Problem Description**:
  `ScheduleAutoTasksWorker` and `PlanViewModelV2.autoScheduleDay()` queried `sessionRepository.getOpenSessions()` to collect `focusedTaskIds`, and excluded those tasks from being scheduled or moved during auto-scheduling passes.
  However, if a user started a 25-minute focus session, the service completed or the app process was terminated, and the user did not return to `FocusScreenV2` to tap "Decompress" or "Complete", the session remained in SQLite with `endedAt = null`.
  Because the query did not verify whether the open session had expired (`startedAt + targetDuration + gracePeriod < now`), the task was permanently treated as actively focused hours or days later. The task was indefinitely locked out of daily replanning and auto-scheduling.
- **Root Cause**:
  Treating all open sessions as active regardless of elapsed time since `startedAt`.
- **Suggested / Applied Fix**:
  Added session expiration filtering: an open session is only considered actively focused if the background service is running or elapsed time is within `targetDurationSeconds * 1000L + 15 * 60 * 1000L` (15-minute grace period). Open sessions exceeding this threshold are ignored, preventing auto-scheduling deadlocks. Verified in [`FocusWidgetAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/widget/FocusWidgetAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-343: Overdue Tasks Trapped in Non-Urgent Quadrants and At-Target Task Reminders Silently Dropped in `NotificationWorker.kt`
- **Severity**: Medium
- **Category**: Background Services & Notifications / Deadline Escalation & Reminder Delivery
- **File**: [`NotificationWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L400-L520)
- **Problem Description**:
  1. In `DeadlineEscalationWorker`, the escalation condition checked `(deadlineMillis - now) in 0L..threshold`. If a task's deadline passed while the device was asleep or before the periodic worker executed (`deadlineMillis < now`), the task was omitted from escalation. Overdue tasks remained trapped in `SCHEDULE` (Schedule) or `DELEGATE` instead of escalating to urgent `DO_FIRST` (Do First).
  2. In `TaskReminderWorker.scheduleReminder()`, the worker checked `val triggerMs = targetMs - 15 * 60 * 1000L`. If a task was scheduled within 15 minutes of the current time (`triggerMs <= now`), it unconditionally cancelled the reminder. The user received no notification whatsoever, even when the scheduled task actually started.
- **Root Cause**:
  1. Excluding negative deltas (`deadlineMillis < now`) in `DeadlineEscalationWorker`.
  2. Bailing out on `triggerMs <= now` rather than scheduling an immediate/at-target reminder when `targetMs > now`.
- **Suggested / Applied Fix**:
  1. Updated `DeadlineEscalationWorker` to escalate tasks with `deadlineMillis <= now + threshold && deadlineMillis >= now - 14 * 86_400_000L` (up to 14 days overdue).
  2. Updated `TaskReminderWorker.scheduleReminder()` so that if `triggerMs <= now && targetMs > now`, it schedules an at-target reminder (`minutesBefore = 0L`, `delay = targetMs - now`).
  Verified in [`NotificationWorkerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/worker/NotificationWorkerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-344: Audio Playback Auto-Resumes After Manual Pause on Audio Focus Gain and Missing DSP Generator Reset Leaks DC Offsets
- **Severity**: Medium
- **Category**: Audio DSP / Audio Focus Lifecycle & State Reset
- **File**: [`NeuralSoundscapePlayer.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapePlayer.kt#L45-L250)
- **Problem Description**:
  1. In `NeuralSoundscapePlayer`, `wasPlayingBeforeFocusLoss` was a non-volatile boolean variable accessed across audio threads and main thread. When the user explicitly paused audio via `pauseWithFadeOut()`, `wasPlayingBeforeFocusLoss` was not cleared. If a transient audio focus loss occurred earlier, a subsequent system `AUDIOFOCUS_GAIN` event caused the player to unexpectedly resume soundscape playback despite the user's manual pause.
  2. In `releaseWithFadeOut()`, when the playback thread terminated, `generator.reset()` was omitted. Internal biquad filter states, pink noise accumulator poles, and DC blocking filter values persisted in memory, causing clicks or DC transient offsets on subsequent playback sessions.
- **Root Cause**:
  Missing `@Volatile` and state reset on `wasPlayingBeforeFocusLoss`, and missing `generator.reset()` in thread release sequence.
- **Suggested / Applied Fix**:
  1. Marked `wasPlayingBeforeFocusLoss` as `@Volatile` and explicitly set `wasPlayingBeforeFocusLoss = false` in `pauseWithFadeOut()`.
  2. Added `generator.reset()` to the playback thread `finally` block upon release.
  3. Added Android API 33+ `RECEIVER_NOT_EXPORTED` flag to `registerNoisyReceiver()`.
  Verified in [`NeuralSoundscapeAdversarialStressTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapeAdversarialStressTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-345: `TaskDao.insert` with `OnConflictStrategy.REPLACE` Triggered SQLite `ON DELETE CASCADE` Deleting Micro-Steps and Focus Sessions on Task Updates
- **Severity**: Critical
- **Category**: Room Database & Foreign Keys / Cascade Deletion Prevention
- **File**: [`TaskDao.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/TaskDao.kt#L11-L15)
- **Problem Description**:
  In `TaskDao.kt`, `insert` was annotated with `@Insert(onConflict = OnConflictStrategy.REPLACE)`.
  In SQLite, an `INSERT OR REPLACE` conflict resolution is implemented internally as an explicit `DELETE` of the old row followed by an `INSERT` of the new row.
  Because `task_micro_steps` and `focus_sessions` define `FOREIGN KEY(taskId) REFERENCES tasks(id) ON DELETE CASCADE`, and production SQLite enables foreign key constraints via `PRAGMA foreign_keys = ON;`, any call to `taskDao.insert(task)` on an existing task entity (e.g. during cloud restore, task import, or repository write paths) triggered an involuntary SQLite `ON DELETE CASCADE` that deleted all attached `task_micro_steps` and `focus_sessions`.
- **Root Cause**:
  Using destructive `OnConflictStrategy.REPLACE` instead of non-destructive `@Upsert` (`INSERT ... ON CONFLICT DO UPDATE`).
- **Suggested / Applied Fix**:
  Replaced `@Insert(onConflict = OnConflictStrategy.REPLACE)` with `@Upsert` on `TaskDao.insert` and added `upsert(task: TaskEntity)`. Room 2.6.1 generates `INSERT INTO tasks (...) VALUES (...) ON CONFLICT(id) DO UPDATE SET ...`, which updates the task in-place without deleting the row and completely avoids triggering `ON DELETE CASCADE`. Verified in [`RoomDatabaseAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/local/RoomDatabaseAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-346: `TaskDao.observeTasksForDate` Omitted Dateless Timed Tasks with Absolute Epoch Timestamps Dropping Scheduled Tasks
- **Severity**: High
- **Category**: Core Data Integrity / Temporal Query Resolution
- **File**: [`TaskDao.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/TaskDao.kt#L41-L55)
- **Problem Description**:
  `TaskDao.observeTasksForDate(dayStart, dayEnd)` evaluated:
  ```sql
  WHERE status = 'ACTIVE'
    AND (
          (scheduledDate >= :dayStart AND scheduledDate < :dayEnd)
          OR
          (recurrence != 'NONE' AND habitDate >= :dayStart AND habitDate < :dayEnd)
        )
  ```
  In PROCUS's dual-representation timestamp architecture, tasks created via time pickers or calendar sync frequently carry absolute epoch timestamps (`scheduledTime >= ONE_DAY_MILLIS`) with `scheduledDate = null`.
  Because `observeTasksForDate` strictly required `scheduledDate` or `habitDate` to match the query range, any dateless task scheduled for today (e.g. today at 14:00) evaluated to false and was completely omitted from `TaskRepository.observeTasksForDate()` and UI daily flows.
- **Root Cause**:
  Missing `OR (scheduledDate IS NULL AND scheduledTime >= :dayStart AND scheduledTime < :dayEnd)` in SQL query.
- **Suggested / Applied Fix**:
  Added `OR (scheduledDate IS NULL AND scheduledTime >= :dayStart AND scheduledTime < :dayEnd)` to `observeTasksForDate` and added direct suspend query `getTasksForDate(dayStart, dayEnd)`. Verified in [`RoomDatabaseAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/local/RoomDatabaseAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-347: `TaskRepository.delete` Ignored `#part` Continuation Tasks Leaving Orphaned Split Tasks and Deadlocked Prerequisites
- **Severity**: High
- **Category**: Core Data Integrity & Scheduling / Continuation Part Cascade Deletion
- **File**: [`TaskRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L265-L310)
- **Problem Description**:
  1. In `TaskRepository.delete(task)`, the method queried continuation Part 2 exclusively as `taskDao.getById("${task.id}_part2")`. However, `TaskSchedulerService` splits tasks using `#part1` and `#part2` (e.g. `"${task.id}#part2"`). Consequently, when a parent task was deleted, any continuation task with `#part` remained orphaned in SQLite.
  2. Downstream tasks that depended on `"${task.id}#part2"` remained deadlocked forever because `#part2` was not included in `deletedIds`.
  3. When an individual `#part` continuation task was directly deleted, the inverse restoration check checked `task.id.endsWith("_part2")`, failing to recognize `#part2` or `#part1` and leaving the parent task title permanently scarred with `(Part 1/2)`.
- **Root Cause**:
  Hardcoded `_part2` string lookups without checking `#part1`, `#part2`, and `_part1`.
- **Suggested / Applied Fix**:
  1. Updated `TaskRepository.delete` to check and delete `${task.id}_part2`, `${task.id}#part2`, `${task.id}#part1`, and `${task.id}_part1`, adding all IDs to `deletedIds` for downstream dependency cleanup.
  2. Updated inverse parent restoration to check `task.id.contains("#part") || task.id.contains("_part") || task.parentTaskId != null`, resolving `parentId = task.parentTaskId ?: task.id.substringBefore("#part").substringBefore("_part")`.
  Verified in [`RoomDatabaseAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/local/RoomDatabaseAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-348: `TaskRepository.splitTaskIntoSessions` Caused Extinction of Recurring Habits by Bypassing Recurrence Lifecycle and Leaving Orphaned Open Sessions
- **Severity**: Critical
- **Category**: Core Data Integrity & Recurrence / Recurring Habit Extinction & Session Cleanup
- **File**: [`TaskRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L400-L415)
- **Problem Description**:
  When a task was split into chunks via `TaskRepository.splitTaskIntoSessions(task)`:
  1. The parent task was marked completed via direct `taskDao.update(task.copy(status = COMPLETED))`, completely bypassing `completeAndRecur(task, now)`. All spawned child sessions were assigned `recurrence = Recurrence.NONE`. Consequently, if a daily or weekly recurring habit was split, the recurrence lifecycle was completely bypassed and no future instance was ever spawned. The habit permanently went extinct and stopped recurring forever.
  2. Direct `taskDao.update` bypassed `TaskReminderWorker.cancelReminder` and `AutonomyNudgeEngine.cancelNudge`, leaving zombie WorkManager notifications scheduled for the completed parent task.
  3. Any active focus session on the parent task was left open with `endedAt = null`, corrupting telemetry and replanning.
- **Root Cause**:
  Direct `taskDao.update` archive bypassing the `completeAndRecur()` recurrence advancement lifecycle and omission of open session closure.
- **Suggested / Applied Fix**:
  1. Closed any open session on `task.id` via `timeSessionDao.update(openSession.copy(endedAt = now, durationMinutes = tracked))`.
  2. Replaced raw `taskDao.update` with `completeAndRecur(task, now)`, properly cancelling WorkManager reminders/nudges for the parent task and advancing the recurrence lifecycle to spawn the next occurrence for tomorrow/next cycle.
  Verified in [`RoomDatabaseAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/local/RoomDatabaseAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-349: `completeAndRecur` Dropped Scheduled and Deadline Dates to Null for Recurring Habits with Absolute Epoch Timestamps
- **Severity**: High
- **Category**: Core Data Integrity & Recurrence / Temporal Recurrence Anchor Preservation
- **File**: [`TaskRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L490-L540)
- **Problem Description**:
  In `TaskRepository.completeAndRecur(task, now)`:
  ```kotlin
  val nextScheduledDate = if (task.scheduledDate != null) nextDayStart else null
  val (nextDeadlineDate, nextDeadlineTime) = if (task.deadlineDate != null) { ... } else { ... }
  ```
  If a recurring habit had an absolute epoch timestamp on `scheduledTime` (e.g. `scheduledDate == null`, `scheduledTime = 1730300000000L`) or `deadlineTime` without explicit date fields, `task.scheduledDate != null` and `task.deadlineDate != null` evaluated to false.
  `nextScheduledDate` and `nextDeadlineDate` were set to `null` on the spawned task. On all subsequent recurrences, the recurring task lost its scheduled slot and deadline date, reverting to an unscheduled backlog task.
- **Root Cause**:
  Ignoring absolute epoch timestamps (`time >= ONE_DAY_MILLIS`) when determining whether a scheduled date or deadline date should be preserved on recurring occurrences.
- **Suggested / Applied Fix**:
  Updated `nextScheduledDate` and `nextDeadlineDate` calculations:
  ```kotlin
  val nextScheduledDate = if (task.scheduledDate != null || (task.scheduledTime != null && task.scheduledTime >= ONE_DAY_MILLIS)) nextDayStart else null
  ```
  And computed `effectiveDeadlineDateMillis` with `startOfLocalDay(task.deadlineTime)` fallback when `deadlineDate` is null, and also added a fallback to extract micro-steps from `task.ifThenPlan` when cloning micro-steps to the spawned task. Verified in [`RoomDatabaseAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/local/RoomDatabaseAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-350: `SessionRepository` and `FocusSessionDao` Missing Task ID Deletion and Open Session Purge Methods Leaking Focus Sessions
- **Severity**: Medium
- **Category**: Room Database & Concurrency / Cross-Table Session Synchronization
- **File**: [`FocusSessionDao.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/FocusSessionDao.kt#L30-L38), [`SessionRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/SessionRepository.kt#L120-L130)
- **Problem Description**:
  `FocusSessionDao` lacked `deleteByTaskId(taskId)` and `deleteOpenSessionsByTaskId(taskId)` methods.
  When `SessionRepository.deleteByTaskId(taskId)` or `deleteOpenSessionsByTaskId(taskId)` was called, only `timeSessionDao` was modified. Any corresponding sessions in `focus_sessions` remained in SQLite, causing cross-table session desynchronization and leaking stale open focus sessions.
- **Root Cause**:
  Missing DAO queries and repository delegation to `focusSessionDao` on task-scoped deletion operations.
- **Suggested / Applied Fix**:
  1. Added `@Query("DELETE FROM focus_sessions WHERE taskId = :taskId") suspend fun deleteByTaskId(taskId: String)` and `@Query("DELETE FROM focus_sessions WHERE taskId = :taskId AND endedAt IS NULL") suspend fun deleteOpenSessionsByTaskId(taskId: String)` to `FocusSessionDao`.
  2. Wired both calls into `SessionRepository.deleteByTaskId` and `deleteOpenSessionsByTaskId`.
  Verified in [`RoomDatabaseAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/local/RoomDatabaseAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-351: `MainActivity.onCreate` Omits `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY` Guard Re-executing Historical Intents on Recents Resume & `ACTION_MAIN` Intent Wiping
- **Severity**: High
- **Category**: Android Lifecycle / Intent Dispatch & Task History
- **File**: [`MainActivity.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L187)
- **Problem Description**:
  1. When an activity was originally launched from a notification or home screen widget (e.g. `ACTION_OPEN_FOCUS` or `ACTION_OPEN_NEW_TASK`), and the user later resumed the app from the Recent Apps / Overview multitasking tray after OS process termination, Android redelivered the original launch intent with `Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY` set.
  Because `MainActivity.onCreate` checked `val hasPendingAction = !intent.action.isNullOrBlank() || intent.hasExtra("taskId") || intent.hasExtra("task_id")` without guarding against `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`, `handleIntent` re-executed the historical notification intent. This forced reopening completed tasks or launching unnecessary focus sessions while completely bypassing `savedInstanceState` restoration.
  2. Additionally, standard launcher icon clicks (`Intent.ACTION_MAIN` with `CATEGORY_LAUNCHER`) satisfied `!intent.action.isNullOrBlank()`, causing `handleIntent` to unconditionally wipe `intent.action = null` from the root activity intent on every cold start.
- **Root Cause**:
  Missing `(intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0` check and failure to exclude `Intent.ACTION_MAIN` from pending action evaluation.
- **Suggested / Applied Fix**:
  Evaluated `isFromHistory = (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0` and `isMainLauncher = intent.action == Intent.ACTION_MAIN && intent.categories?.contains(Intent.CATEGORY_LAUNCHER) == true`.
  Defined `hasPendingAction = !isFromHistory && (!isMainLauncher && !intent.action.isNullOrBlank() || intent.hasExtra("taskId") || intent.hasExtra("task_id"))`. Verified in [`NavigationAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/navigation/NavigationAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-352: Asynchronous Focus Task Query in `MainActivity.handleIntent` Leaves State Null Flashing `NewChapterCard` / `TopGoalsRefillCard`
- **Severity**: High
- **Category**: UI & State Synchronization / Async Focus Resolution Race
- **File**: [`MainActivity.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L110-L140)
- **Problem Description**:
  When `ACTION_OPEN_FOCUS` was triggered without a specific `taskId` (e.g. tapping "Start Focus" on the home screen widget with no active session), `MainActivity.handleIntent` launched a background coroutine (`lifecycleScope.launch`) to query Room for the first active task.
  Because the coroutine executed asynchronously, `initialFocusTaskId` remained `null` during initial Compose layout.
  Consequently, `pendingDirectIntent` and `hasDirectIntent` evaluated to `false`. If a Fresh Start reset (`NewChapterCard`) or Top Goals Refill (`TopGoalsRefillCard`) was due, that dialog immediately flashed onto the screen for 10-50ms.
  When the database coroutine finished and assigned `initialFocusTaskId`, `hasDirectIntent` flipped to `true`, abruptly destroying the dialog and hijacking the viewport to navigate to `focus_v2`.
- **Root Cause**:
  Lack of asynchronous state synchronization during fallback task resolution, causing Compose to evaluate `hasDirectIntent = false` prematurely.
- **Suggested / Applied Fix**:
  Introduced `isResolvingFocusTask` tracked in mutable state and persisted in `onSaveInstanceState`. Included `isResolvingFocusTask` in `pendingDirectIntent` and `hasDirectIntent`, ensuring welcome and refill cards are suppressed while Room determines the actionable task. Verified in [`NavigationAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/navigation/NavigationAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-353: `ACTION_OPEN_FOCUS` Fallback Task Selection Omits Future Task & Blocked Part 2 Continuation Filtering Erroneously Selecting Future Tasks
- **Severity**: High
- **Category**: Core Logic & Task Scheduling / Actionable Task Selection
- **File**: [`MainActivity.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L120-L135)
- **Problem Description**:
  In `MainActivity.handleIntent` when resolving an actionable task for `ACTION_OPEN_FOCUS`:
  1. The filter only verified `it.status == TaskStatus.ACTIVE && it.quadrant != Quadrant.ELIMINATE` and active prerequisites. It did not filter out future-scheduled tasks where `scheduledStart > dayEnd` or `effectiveScheduledDateMillis() > dayEnd`.
  If a user had a high-priority Frog task scheduled for next week, tapping the Focus widget today launched focus mode on next week's task rather than today's scheduled tasks.
  2. It omitted checks for split continuation Part 2 tasks (`_part2` or `#part2`) whose parent task remained active in SQLite, allowing Part 2 to be started before Part 1 was finished.
- **Root Cause**:
  Incomplete candidate task validation in `MainActivity.kt` compared to `FocusWidgetUpdateWorker` and `FlowViewModelV2`.
- **Suggested / Applied Fix**:
  Excluded tasks scheduled past `dayEnd`, excluded Part 2 tasks whose parent is still in `activeIds`, prioritized tasks scheduled for today over unscheduled backlog tasks, and used `TaskEntity.dependencyIds()` to prevent whitespace parsing defects. Verified in [`NavigationAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/navigation/NavigationAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-354: Blank / Whitespace Task IDs in `NeuroFlowAppV2.kt` Bypass Consumed Callback Causing Permanent Lockout of Welcome and Refill Sheets
- **Severity**: Medium
- **Category**: UI & State Management / Initial Task Consumption Guard
- **File**: [`NeuroFlowAppV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L77-L105)
- **Problem Description**:
  In `NeuroFlowAppV2.kt`, `LaunchedEffect(initialTaskId)` and `LaunchedEffect(initialWoopTaskId)` only executed `onInitialTaskConsumed()` / `onInitialWoopConsumed()` inside `if (!initialTaskId.isNullOrBlank())`.
  If an incoming intent passed a blank or whitespace-only task ID string, navigation was skipped but the consumed callback was never triggered.
  As a result, `initialFocusTaskId` / `initialWoopTaskId` remained non-null in `MainActivity.kt`. Because `pendingDirectIntent` checked `initialFocusTaskId != null`, `hasDirectIntent` was latched to `true` for the rest of the application lifecycle, permanently preventing `NewChapterCard` and `TopGoalsRefillCard` from ever being displayed.
- **Root Cause**:
  Conditional invocation of the consumed callback inside the non-blank guard rather than whenever the parameter is non-null.
- **Suggested / Applied Fix**:
  Normalized task IDs in `MainActivity.handleIntent` using `.takeIf { it.isNotBlank() }`, and updated `NeuroFlowAppV2.kt` to unconditionally trigger `onInitialTaskConsumed()` and `onInitialWoopConsumed()` whenever `initialTaskId != null` / `initialWoopTaskId != null`. Expanded `WOOP_REFLECT` action matching to accept package-prefixed variants. Verified in [`NavigationAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/navigation/NavigationAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-355: Missing `Modifier.fillMaxSize()` on Analytics and History Scaffold Content Host `Box` in `NeuroFlowAppV2.kt`
- **Severity**: Low
- **Category**: UI Layout / Viewport Sizing & Measurement
- **File**: [`NeuroFlowAppV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L338-L375)
- **Problem Description**:
  In `NeuroFlowAppV2.kt`, routes `"analytics"` and `"history"` wrapped `AnalyticsScreen` and `HistoryScreen` inside `Box(modifier = Modifier.padding(innerPadding))` omitting `Modifier.fillMaxSize()`.
  Both child screens define nested Scaffolds and lazy lists expecting to fill the available viewport. Without `fillMaxSize()`, the content box wrapped content on initial measurement, causing potential clipping and inconsistent scroll boundary calculation on dynamic window sizes and tablets.
- **Root Cause**:
  Omission of `Modifier.fillMaxSize()` on the Scaffold content container Box.
- **Suggested / Applied Fix**:
  Updated the container Box modifier to `Modifier.fillMaxSize().padding(innerPadding)` for both `"analytics"` and `"history"` routes, and added `fromRoute` and `isHubRoute` helpers to `NavHubsV2`. Verified in [`NavigationAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/navigation/NavigationAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-356: Intraday Deadline Epoch Defaulting to `System.currentTimeMillis()` in `PlanViewModelV2.tasksForDay` Causing Task Drop on Multi-Day Views and False Leak into Today
- **Severity**: High
- **Category**: UI & Data Integrity / Multi-Day Temporal Epoch Resolution
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L363)
- **Problem Description**:
  In `PlanViewModelV2.tasksForDay`, tasks with an intraday deadline time (e.g. `deadlineTime = 17:00`) but no explicit `deadlineDate` evaluated their deadline epoch via:
  ```kotlin
  val dDate = task.resolveDeadlineEpochMillis()
  dDate != null && dDate in dayStart..dayEnd
  ```
  Because `resolveDeadlineEpochMillis()` defines an optional parameter `referenceDateMillis: Long = System.currentTimeMillis()`, calling it without arguments anchored the intraday deadline to current local system time (today).
  Consequently, when a user browsed future days (e.g. Tomorrow or +2 days) via `DateCarouselStrip`, `dDate` evaluated to today at 17:00. The task was dropped from tomorrow's plan (`dDate in dayStart..dayEnd` was false), while simultaneously leaking onto today's plan even when viewing future horizons.
- **Root Cause**:
  Calling `resolveDeadlineEpochMillis()` with default parameter fallback rather than passing `dateMillis`.
- **Suggested / Applied Fix**:
  Passed the selected date epoch `dateMillis`:
  ```kotlin
  val dDate = task.resolveDeadlineEpochMillis(dateMillis)
  dDate != null && dDate in dayStart..dayEnd
  ```
  Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-357: Cleared Scheduled Date Resurrected and Absolute Epoch Timestamps Mismatched in `PlanViewModelV2.addTask()` Preventing Unscheduled Backlog Creation
- **Severity**: High
- **Category**: Core Data Integrity / Temporal Scheduling & Backlog Preservation
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L780-L797)
- **Problem Description**:
  In `PlanViewModelV2.addTask(task)`:
  ```kotlin
  val boundTask = if (task.scheduledDate == null) {
      task.copy(
          scheduledDate = selectedDay,
          habitDate = if (task.recurrence != Recurrence.NONE) selectedDay else task.habitDate
      )
  } else { task }
  ```
  1. If a user in `SmartTaskComposerSheetV2` explicitly cleared the scheduled date or left a task unscheduled for backlog triage, `addTask` unconditionally forced `scheduledDate = selectedDay`. This resurrected the cleared date, preventing users from ever creating unscheduled backlog tasks from `PlanScreenV2`.
  2. If a task arrived with an absolute epoch timestamp on `scheduledTime` (e.g. `scheduledTime >= ONE_DAY_MILLIS` representing next week), `scheduledDate` was null. `addTask` assigned `scheduledDate = selectedDay` (today), creating an asynchronous date/time mismatch where `scheduledDate` pointed to today while `scheduledTime` pointed to next week.
- **Root Cause**:
  Unconditional fallback to `selectedDay` whenever `task.scheduledDate == null`.
- **Suggested / Applied Fix**:
  Implemented pattern matching in `addTask`:
  - When `task.scheduledTime >= ONE_DAY_MILLIS`: Normalized the absolute epoch timestamp to start-of-day `scheduledDate` and intraday offset `scheduledTime`.
  - When `task.scheduledDate != null`: Preserved explicit date.
  - When `task.recurrence != Recurrence.NONE`: Anchored recurring habits with null date to `selectedDay`.
  - When `task.scheduledTime != null`: Anchored intraday timed tasks without date to `selectedDay`.
  - Otherwise (`else`): Preserved `scheduledDate = null` for untimed backlog tasks.
  Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-358: Iceboxed Tasks (`Quadrant.ELIMINATE`) Leaking into `tasksForDay` and `unscheduledTasks` Staging Drawer in `PlanViewModelV2.kt`
- **Severity**: Medium
- **Category**: UI & State Synchronization / Icebox Task Filtering
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L354-L378)
- **Problem Description**:
  Neither `tasksForDay` nor `unscheduledTasks` filtered out `task.quadrant == Quadrant.ELIMINATE`.
  Consequently:
  1. Tasks moved to the Icebox / Someday quadrant (`ELIMINATE`) that had a deadline or scheduled date for today leaked into `tasksForDay`, occupying timeline blocks despite being cut or shelved.
  2. Unscheduled tasks in `Quadrant.ELIMINATE` leaked into the "📥 Unscheduled (N)" staging drawer with the prompt "Tap to schedule", even though `autoScheduleDay` and `AutoSchedulingContracts` strictly forbid scheduling eliminate tasks.
- **Root Cause**:
  Missing `task.quadrant != Quadrant.ELIMINATE` guards in `tasksForDay` and `unscheduled`.
- **Suggested / Applied Fix**:
  Added `if (task.quadrant == Quadrant.ELIMINATE) return@filter false` in `tasksForDay` and `task.quadrant != Quadrant.ELIMINATE` in `unscheduled`. Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-359: `contextTag` Omitted in Search Query Filter and Manual Peak Profile Ignored in `PlanViewModelV2` and `TimelineScheduleView`
- **Severity**: Medium
- **Category**: UI & Search / Context Tag & Circadian Calibration
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L341), [`PlanScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L855)
- **Problem Description**:
  1. In `PlanViewModelV2.searchFilteredAll`, search matching checked `task.title`, `task.description`, and `task.tags`, but omitted `task.contextTag`. Tasks created via starter context chips (e.g. `#deepwork`, `#offline`) whose tag is stored in `task.contextTag` with empty `task.tags` were hidden when searching for the tag name.
  2. In `TimelineScheduleView` and `PlanViewModelV2.shiftTaskToPeak`, `preferences.manualPeakProfileEnabled` and `preferences.manualPeakAnchorMinuteOfDay` were ignored, falling back to static non-manual peak hours when `circadianSummary` was absent or during peak calculation.
- **Root Cause**:
  Missing `task.contextTag` check in search filtering and omission of manual peak profile evaluation prior to circadian summary fallback.
- **Suggested / Applied Fix**:
  Included `task.contextTag.contains(query, ignoreCase = true)` in `searchFilteredAll`, and checked `manualPeak` from `manualPeakProfileEnabled` and `manualPeakAnchorMinuteOfDay` first in `TimelineScheduleView` and `PlanViewModelV2.shiftTaskToPeak`. Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-360: Missing Ultradian Buffer Between Split Parts and Untimed Task Ultradian Buffer Leak in `PlanScreenV2.shouldShowUltradianBufferAfter`
- **Severity**: Medium
- **Category**: UI & Core Logic / Ultradian Recovery Buffer Display
- **File**: [`PlanScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L824-L850)
- **Problem Description**:
  In `PlanScreenV2.shouldShowUltradianBufferAfter(tasks, index)`:
  1. It never checked `isBetweenSplitParts`. When a long task was split into Part 1 (45m) and Part 2 (45m), Part 1 alone was below 90m (`runSum = 45`), so the `UltradianRecoveryDivider` was never rendered between split parts.
  2. Untimed date-only tasks (with `scheduledTime == null`) accumulated into `runSum`, rendering false timeline rest break dividers between untimed backlog tasks.
  3. When two consecutive timed tasks already had a natural rest break (gap >= 15m), `runSum` failed to reset and erroneously rendered a rest break after short subsequent tasks.
- **Root Cause**:
  Missing `isBetweenSplitParts` detection, failure to guard against `scheduledTime == null`, and lack of natural rest break reset logic.
- **Suggested / Applied Fix**:
  Updated `shouldShowUltradianBufferAfter`:
  - Returns `true` if `isBetweenSplitParts` is detected between index and index + 1.
  - Returns `false` if `current.scheduledTime == null` or `next.scheduledTime == null`.
  - Resets `runSum = 0` if a natural gap >= 15 minutes exists between consecutive timed tasks.
  Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-361: User Note Title Overwritten by Inferred Sanitized Title in `PlanViewModelV2.createTaskFromNote(title, description)` and Frog Status Stuck on Quadrant Cycling
- **Severity**: Medium
- **Category**: UI & Data Integrity / Note Title Preservation & Frog State Management
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L707), [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L850)
- **Problem Description**:
  1. In `PlanViewModelV2.createTaskFromNote(title, description)`:
     ```kotlin
     extractedTaskTitle = inferred.sanitizedTitle.ifBlank { title.ifBlank { description.take(35) } }
     ```
     `inferred.sanitizedTitle` was checked before the user's explicit note `title`. If a user entered a note with title "Doctor Appointment" and description "call clinic", `inferred.sanitizedTitle` ("call clinic") overwrote and discarded the user's title.
  2. In `PlanViewModelV2.moveTaskToQuadrant`:
     ```kotlin
     isFrog = if (newQuadrant == Quadrant.DO_FIRST) true else (if (newQuadrant == Quadrant.ELIMINATE) false else task.isFrog)
     ```
     When cycling a task out of `Quadrant.DO_FIRST` into `Quadrant.SCHEDULE` or `Quadrant.DELEGATE`, `task.isFrog` remained `true`, leaving non-DO_FIRST tasks permanently tagged as Frogs.
- **Root Cause**:
  Inverted title fallback precedence and failure to clear `isFrog = false` when transitioning tasks out of `Quadrant.DO_FIRST`.
- **Suggested / Applied Fix**:
  Prioritized explicit title: `extractedTaskTitle = title.trim().ifBlank { inferred.sanitizedTitle.ifBlank { description.take(35) } }`.
  Cleared Frog status when cycling out of `DO_FIRST`:
  ```kotlin
  isFrog = if (newQuadrant == Quadrant.DO_FIRST) true else (if (newQuadrant == Quadrant.ELIMINATE) false else (if (task.quadrant == Quadrant.DO_FIRST) false else task.isFrog))
  ```
  Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-362: Cleared Scheduled Date Resurrected to Today and Future Absolute Epoch Timestamps Unnormalized in `FlowViewModelV2.addTask()`
- **Severity**: High
- **Category**: Core Data Integrity / Temporal Scheduling & Backlog Preservation
- **File**: [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L572-L600)
- **Problem Description**:
  In `FlowViewModelV2.addTask(task)`:
  ```kotlin
  val boundTask = if (task.scheduledDate == null) {
      task.copy(
          scheduledDate = dayStart,
          habitDate = if (task.recurrence != Recurrence.NONE) dayStart else task.habitDate
      )
  } else { task }
  ```
  1. When a user created an unscheduled backlog task (`scheduledDate == null && scheduledTime == null`), `addTask` unconditionally attached `scheduledDate = dayStart` (today), forcing backlog triage items into today's active schedule.
  2. When a task arrived with an absolute epoch timestamp on `scheduledTime` (`scheduledTime >= ONE_DAY_MILLIS` representing future days), `scheduledDate` was assigned to `dayStart` (today) while `scheduledTime` retained the future epoch millisecond timestamp, creating an asynchronous date/time mismatch.
- **Root Cause**:
  Unconditional fallback to `dayStart` whenever `task.scheduledDate == null`.
- **Suggested / Applied Fix**:
  Updated `addTask` to match on temporal structure:
  - When `task.scheduledTime >= ONE_DAY_MILLIS`: Normalized the absolute epoch timestamp to start-of-day `scheduledDate = epochStart` and intraday offset `scheduledTime = timeOffset`.
  - When `task.scheduledDate != null`: Preserved explicit date.
  - When `task.recurrence != Recurrence.NONE`: Anchored recurring habits without date to `dayStart`.
  - When `task.scheduledTime != null`: Anchored intraday timed tasks without date to `dayStart`.
  - Otherwise (`else`): Preserved `scheduledDate = null` for untimed backlog tasks.
  Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-363: User Note Title Clobbered by Inferred Title in `FlowViewModelV2.addTaskFromNote(title, content)` and Omitted Nudge/Calendar Sync in `breakDownTask()`
- **Severity**: Medium
- **Category**: UI & Data Integrity / Note Title Preservation & Breakdown State Sync
- **File**: [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L730), [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L1060)
- **Problem Description**:
  1. In `FlowViewModelV2.addTaskFromNote(title, content)`:
     ```kotlin
     extractedTaskTitle = inferred.sanitizedTitle.ifBlank { title.ifBlank { content.take(35) } }
     ```
     `inferred.sanitizedTitle` was checked before the user's explicit note `title`. If a user entered a note with title "Doctor Appointment" and description "call clinic", `inferred.sanitizedTitle` ("call clinic") overwrote and discarded the user's title.
  2. In `FlowViewModelV2.breakDownTask(task)`:
     When micro-steps were generated and stored in `task.ifThenPlan`, `taskRepository.update(task)` was executed, but `calendarSyncRepository?.handleTaskStateChanged(updated)` and `AutonomyNudgeEngine.scheduleNudge(it, updated)` were omitted, leaving background services out of sync with decomposed micro-steps.
- **Root Cause**:
  Inverted title fallback precedence in `addTaskFromNote` and omitted background service notification/calendar sync in `breakDownTask`.
- **Suggested / Applied Fix**:
  1. Prioritized explicit title: `extractedTaskTitle = title.trim().ifBlank { inferred.sanitizedTitle.ifBlank { content.take(35) } }`.
  2. Dispatched `calendarSyncRepository?.handleTaskStateChanged(updated)` and `AutonomyNudgeEngine.scheduleNudge(it, updated)` in `breakDownTask`.
  Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-364: `#part2` Split Continuation Tasks and `_part1` Parent Tasks Not Detected as Blocked in `FlowViewModelV2` Actionable and Recommendation Pools
- **Severity**: High
- **Category**: Core Logic & Scheduling / Split Task Continuation & Prerequisite Enforcement
- **File**: [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L371), [`FlowScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt#L243)
- **Problem Description**:
  1. In `FlowViewModelV2.kt`, `isPart2Blocked` checked only `task.id.endsWith("_part2")` and `task.id.removeSuffix("_part2") in activeTaskIds`.
     Split continuation tasks generated with the `#part2` convention (e.g. `task#part2`) or whose parents are named `task_part1` / `task#part1` evaluated `isPart2Blocked = false`, causing incomplete split Part 2 tasks to leak into `actionableActiveTasks` and become recommended ahead of Part 1.
  2. In `FlowScreenV2.kt`, when `recommendedTask` was null or filtered out by a tag, `topTask` fell back to `filteredTasks.firstOrNull { it.isFrog } ?: filteredTasks.firstOrNull()` without checking prerequisite dependencies or blocked Part 2 continuations, erroneously elevating blocked tasks into the spotlight hero.
- **Root Cause**:
  Omission of `#part2` / `_part1` naming variations in `isPart2Blocked` and lack of actionability vetting on spotlight fallback tasks.
- **Suggested / Applied Fix**:
  1. Updated `isPart2Blocked` to check:
     ```kotlin
     val isPart2Continuation = task.id.endsWith("_part2") || task.id.contains("#part2")
     val isPart2Blocked = isPart2Continuation && (
         (task.parentTaskId != null && task.parentTaskId in activeTaskIds) ||
         task.id.removeSuffix("_part2").substringBefore("#part2") in activeTaskIds ||
         task.id.replace("_part2", "_part1").replace("#part2", "#part1") in activeTaskIds
     )
     ```
  2. Vetted `topTask` in `FlowScreenV2.kt` against actionability (`isTaskActionable`) before displaying in `FlowSpotlightHeroCard`.
  Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-365: Algorithmic Peak End Hour Overrides Manual Peak Window in `FlowViewModelV2.shiftTaskToPeak()` Inverting Morning Active Check
- **Severity**: Medium
- **Category**: Core Logic & Circadian Calibration / Manual Peak Profile Alignment
- **File**: [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L751)
- **Problem Description**:
  In `FlowViewModelV2.shiftTaskToPeak`:
  ```kotlin
  val peakHour = manualPeak ?: summary?.peakWindowStartHour ?: ...
  val peakEndHour = summary?.peakWindowEndHour ?: ((peakHour + 3) % 24)
  ```
  When a user configured a manual peak window (e.g. 14:00 PM), `peakHour` was set to 14, but `peakEndHour` fell back to `summary?.peakWindowEndHour` (e.g. 12:00 PM from the algorithmic model).
  `peakHour <= peakEndHour` (14 <= 12) evaluated to `false`, causing `isPeakActive` to evaluate `currentHour >= 14 || currentHour < 12`.
  Consequently, during all morning hours (`currentHour < 12`), `isPeakActive` erroneously evaluated to `true`, scheduling tasks at the current morning hour with label `"Peak Window (${currentHour}:00 Active)"` rather than shifting them to the user's manual peak at 14:00.
- **Root Cause**:
  Omission of `manualPeak` guard in `peakEndHour` calculation.
- **Suggested / Applied Fix**:
  Calculated `peakEndHour` as `if (manualPeak != null) ((manualPeak + 3) % 24) else (summary?.peakWindowEndHour ?: ((peakHour + 3) % 24))`.
  Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-366: Untimed Tasks Accumulate into Ultradian Recovery Dividers and Stale Gap Accumulation in `EnergyTimelineStream.kt`, Plus Untimed "NOW" Playhead Leak
- **Severity**: Medium
- **Category**: UI & Core Logic / Ultradian Recovery Buffer & Playhead Accuracy
- **File**: [`EnergyTimelineStream.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt#L101), [`EnergyTimelineStream.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt#L665)
- **Problem Description**:
  1. In `EnergyTimelineStream.kt`, `cumulativeMinutes += TaskBlockPlan.plannedMinutes(task)` accumulated across untimed tasks (`scheduledTime == null`). When multiple untimed tasks reached 90 minutes, an `UltradianRecoveryDivider` ("15m Ultradian Rest Buffer") was rendered between untimed backlog tasks.
  2. Natural gaps between consecutive timed tasks (e.g. 3 hours between morning and afternoon blocks) never reset `cumulativeMinutes`, rendering false rest break dividers after short afternoon tasks.
  3. `activeOrNextIndex` evaluated `else { true }` for untimed tasks, placing a pulsing "NOW" playhead above untimed backlog tasks at the bottom of the list when all timed tasks had passed.
- **Root Cause**:
  Lack of untimed task guards, missing natural rest break reset logic, and default `true` fallback in `activeOrNextIndex`.
- **Suggested / Applied Fix**:
  1. Implemented `shouldShowUltradianBufferAfter(tasks, index)`:
     - Returns `true` if `isBetweenSplitParts` is detected.
     - Returns `false` if `current.scheduledTime == null || next.scheduledTime == null`.
     - Resets running sum to 0 if a natural gap >= 15 minutes exists between consecutive timed tasks.
     - Returns `true` when timed continuous focus exceeds 90 minutes.
  2. Updated `activeOrNextIndex` to evaluate `else { false }`, ensuring untimed tasks are not stamped with the "NOW" indicator.
  Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-367: `#part2` Split Continuation Tasks and `#part1` Parent Tasks Not Detected as Blocked in `FocusViewModelV2.findNextActionableTask()`
- **Severity**: High
- **Category**: Core Logic & Scheduling / Split Task Continuation & Prerequisite Enforcement
- **File**: [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L983)
- **Problem Description**:
  In `FocusViewModelV2.findNextActionableTask()`, the next recommended task algorithm queried candidate tasks for navigation and celebration recommendations. When checking if a task was an ultradian split continuation, it only checked:
  ```kotlin
  val isPart2Blocked = task.id.endsWith("_part2") && (
      (task.parentTaskId != null && task.parentTaskId in activeTaskIds) ||
      task.id.removeSuffix("_part2") in activeTaskIds ||
      task.id.replace("_part2", "_part1") in activeTaskIds
  )
  ```
  However, throughout PROCUS (such as in AI decomposition, task splitting, and external note/calendar synchronizers), split continuations frequently employ `#part2` and `#part1` naming conventions (e.g. `task_123#part2` and `task_123#part1`). Because `findNextActionableTask()` only checked `.endsWith("_part2")`, `#part2` continuation tasks were not identified as blocked while Part 1 was still incomplete or actively running. This leaked Part 2 continuation tasks into `nextTaskId` recommendations and auto-transition prompts prematurely.
- **Root Cause**:
  Incomplete pattern matching in `isPart2Continuation` and `isPart2Blocked` that omitted `#part2` substrings and `#part1` parent ID substitutions.
- **Suggested / Applied Fix**:
  Expanded `isPart2Blocked` in `FocusViewModelV2.kt` to:
  ```kotlin
  val isPart2Continuation = task.id.endsWith("_part2") || task.id.contains("#part2")
  val isPart2Blocked = isPart2Continuation && (
      (task.parentTaskId != null && task.parentTaskId in activeTaskIds) ||
      task.id.removeSuffix("_part2").substringBefore("#part2") in activeTaskIds ||
      task.id.replace("_part2", "_part1").replace("#part2", "#part1") in activeTaskIds
  )
  if (isPart2Blocked) return@filter false
  ```
  Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-368: `FocusViewModelV2.completeTask()` Reads Stale `uiState.elapsedSeconds` Truncating Focus Minutes on Background Completion
- **Severity**: High
- **Category**: Core Logic & Telemetry / Elapsed Focus Time Background Recalculation
- **File**: [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L690)
- **Problem Description**:
  In `FocusViewModelV2.completeTask()`:
  ```kotlin
  val currentElapsed = _uiState.value.elapsedSeconds
  val elapsedMins = (currentElapsed / 60).coerceAtLeast(1)
  ```
  When the app is placed in the background or the screen turns off during an active focus sprint, the Compose UI stops ticking, or the UI state may lag behind true wall-clock time. If the user completes the task immediately upon returning from the background, or if the viewmodel processes completion from an external intent while paused, `_uiState.value.elapsedSeconds` holds a stale value recorded at the moment the UI paused or ticked last. This dropped the actual focused duration and truncated the recorded minutes in `sessionRepository.syncFocusSession` and `taskRepository.completeAndRecur`.
- **Root Cause**:
  Direct reliance on mutable and UI-ticking `_uiState.value.elapsedSeconds` instead of recalculating real elapsed time from canonical session timestamps (`startedAtMillis`, `accumulatedPauseMillis`, `isPaused`, `pauseStartedAtMillis`) via `FocusTimingCalculator.calculate()`.
- **Suggested / Applied Fix**:
  Updated `completeTask()` to dynamically recalculate elapsed time using `FocusTimingCalculator.calculate()`:
  ```kotlin
  val realElapsedSeconds = if (sessionStartedAt > 0L) {
      val now = System.currentTimeMillis()
      val timing = FocusTimingCalculator.calculate(
          targetDurationSeconds = if (state.mode == FocusTimerMode.STOPWATCH) {
              FocusTimingCalculator.LIVE_SESSION_CEILING_MINUTES * 60
          } else {
              state.mode.durationMinutes * 60
          },
          startedAtMillis = sessionStartedAt,
          accumulatedPauseMillis = state.accumulatedPauseMillis,
          isPaused = state.isPaused,
          pauseStartedAtMillis = state.pauseStartedAtMillis,
          nowMillis = now
      )
      timing.elapsedSeconds.coerceAtLeast(state.elapsedSeconds)
  } else {
      state.elapsedSeconds
  }
  val elapsedMins = (realElapsedSeconds / 60).coerceAtLeast(1)
  ```
  Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-369: Unbounded Overdue Sprint Duration in `FocusService.handleCompleteAndStop()` Fabricates Phantom Focus Hours When Completed from Notification
- **Severity**: High
- **Category**: Background Services & Telemetry / Overdue Sprint Duration Capping & End Time Anchoring
- **File**: [`FocusService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L236)
- **Problem Description**:
  In `FocusService.handleCompleteAndStop(activeTaskId)`:
  ```kotlin
  val netDurationMs = (now - baseStartedAt - totalPause).coerceAtLeast(0L)
  val durationMins = (netDurationMs / 60000f).coerceAtLeast(1f)
  ```
  When a user starts a timed sprint (e.g. 25 minutes) and leaves the device unattended for several hours after the timer completes, the notification action "Done" or "Complete" may be tapped hours later. Because `handleCompleteAndStop()` used raw `now - baseStartedAt`, a 25-minute sprint tapped 5 hours later would record 300 minutes of focus into `FocusSessionEntity` and `SessionRepository.syncFocusSession`, distorting the user's focus metrics and deep work logs. Additionally, `calculatedEndAt` defaulted to `now` rather than the true completion of the sprint duration.
- **Root Cause**:
  Missing upper bound ceiling for sprint durations and lack of end-timestamp anchoring in `FocusService.handleCompleteAndStop()`.
- **Suggested / Applied Fix**:
  Added duration ceiling bounds and end-timestamp anchoring in `FocusService.kt`:
  ```kotlin
  val durationMs = durationSeconds * 1000L
  val maxAllowedMins = if (durationSeconds > 0) {
      durationSeconds / 60f
  } else {
      FocusTimingCalculator.LIVE_SESSION_CEILING_MINUTES.toFloat()
  }
  val durationMins = (netDurationMs / 60000f).coerceIn(1f, maxAllowedMins)
  val calculatedEndAt = if (durationSeconds > 0) {
      (baseStartedAt + durationMs + totalPause).coerceAtMost(now)
  } else {
      now
  }
  ```
  Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-370: `FocusViewModelV2.autoBreakDownTask()` Retains `skippedMicroStepIndices` from Prior Decompositions Skipping Fresh Step 0
- **Severity**: Medium
- **Category**: UI & State Management / Micro-Step Decomposition Reset
- **File**: [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L450)
- **Problem Description**:
  In `FocusViewModelV2.autoBreakDownTask()`, when generating new micro-steps via Gemini or heuristic fallback, the viewmodel updated `_uiState` with the new steps and reset `activeMicroStepIndex = 0`. However, `skippedMicroStepIndices` was not reset to `emptySet()`. If the user had previously skipped micro-step 0 on a different task or plan, the newly decomposed task immediately considered step 0 skipped, causing the doorway card to bypass the first step or display distorted progress indices.
- **Root Cause**:
  `skippedMicroStepIndices` was omitted from the state reset in `autoBreakDownTask()`.
- **Suggested / Applied Fix**:
  Added `skippedMicroStepIndices = emptySet()` to `_uiState.update` when applying newly decomposed steps in `autoBreakDownTask()`.
  Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-371: Decompression Rest Session Timestamps Leak into Subsequent Task Completion and Auto-Tracker Resumes During Sprint Completion Review
- **Severity**: Medium
- **Category**: UI & Timer State / Rest Session State Leakage & Auto-Tracker Review Guard
- **File**: [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L380), [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L318)
- **Problem Description**:
  1. In `FocusViewModelV2.takeDecompressionRest(durationMinutes)`, when the user opted to rest after a sprint, the rest timer configured `_uiState.update { it.copy(mode = FocusTimerMode.DECOMPRESSION_REST, ...) }`. However, it left `startedAtMillis` intact from the previous focus session (or activeSessionId retained). If the user subsequently tapped "Complete Task" from the decompression zone or after resting, `completeTask()` used the old session's `startedAtMillis` and `elapsedSeconds`, contaminating the session tracking records with rest minutes or double-counting previous focus intervals.
  2. In `FocusViewModelV2.checkAutoTracker()`, the auto-tracker periodically inspected whether the timer should start. When a sprint completed and the sprint completion review sheet (`showSprintCompletionSheet`) or `isSprintCompleted` was displayed to the user, `checkAutoTracker()` had no check for `isSprintCompleted`, allowing the auto-tracker to inadvertently restart timers while the user was reviewing their completed session.
- **Root Cause**:
  Omission of `startedAtMillis = 0L`, `elapsedSeconds = 0L`, and `activeSessionId = null` reset on entering decompression rest, and omission of `isSprintCompleted` / `showSprintCompletionSheet` guards in `checkAutoTracker()`.
- **Suggested / Applied Fix**:
  1. Updated `takeDecompressionRest()` to reset `startedAtMillis = 0L`, `elapsedSeconds = 0L`, `remainingSeconds = durationSeconds`, `accumulatedPauseMillis = 0L`, and `activeSessionId = null`.
  2. Added `if (_uiState.value.isSprintCompleted || _uiState.value.showSprintCompletionSheet) return` guard in `checkAutoTracker()`.
  Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-372: `PlacementStability.verdict()` Omits Parameterized `nowMillis` in `task.resolveDeadlineEpochMillis()` Defaulting to Real-Time System Clock and Distorting Deadline Urgency
- **Severity**: Medium
- **Category**: Core Logic & Scheduling / Parameterized Deadline Epoch Resolution
- **File**: [`PlacementStability.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/PlacementStability.kt#L66)
- **Problem Description**:
  In `PlacementStability.verdict(task, nowMillis, prefs, forceReplan)`, line 49 correctly passed `nowMillis` to resolve the task's scheduled start time:
  ```kotlin
  val scheduledMillis = task.resolveScheduledEpochMillis(nowMillis)
  ```
  However, on line 66:
  ```kotlin
  val deadlineMillis = task.resolveDeadlineEpochMillis()
  ```
  It omitted `nowMillis`, defaulting `referenceDateMillis` to real-time `System.currentTimeMillis()`. When the scheduler evaluated tasks for a future simulated date (e.g. tomorrow or several days ahead in background scheduling or testing), a dateless task with an intraday deadline time (e.g. 5:00 PM) resolved its deadline epoch relative to today's date rather than the evaluated future date. As a result, `(deadlineMillis - nowMillis)` resulted in a large negative number, causing `hoursUntilDeadline < 6f` to evaluate to true and falsely triggering `ReplanVerdict.DEADLINE_RESCUE` on non-urgent future tasks.
- **Root Cause**:
  `task.resolveDeadlineEpochMillis()` was invoked without passing `nowMillis`.
- **Suggested / Applied Fix**:
  Updated line 66 of `PlacementStability.kt` to pass `nowMillis`:
  ```kotlin
  val deadlineMillis = task.resolveDeadlineEpochMillis(nowMillis)
  ```
  Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-373: `TaskSchedulerService.scheduleDayWithOutcome()` Omits `completionEndMap[parentId]` Update During Hysteresis Placement and Omits `parentTaskId` / Title Checks in Dynamic `isSplitContinuation`
- **Severity**: High
- **Category**: Core Logic & Scheduling / Hysteresis Continuation Completion Synchronization
- **File**: [`TaskSchedulerService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L555), [`TaskSchedulerService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L880-L886)
- **Problem Description**:
  1. When candidate tasks are anchored via hysteresis or locked (lines 495-559), `completionEndMap[task.id]` was populated with `existingWindow.endMillis`. However, if `task` was Part 2 of a split continuation, `completionEndMap[parentId]` was never updated. When a downstream task waiting on the parent task was subsequently evaluated, it either found `completionEndMap[parentId]` matching Part 1's earlier completion time (allowing the downstream task to be scheduled concurrently with or before Part 2) or found `null` (causing the downstream task to fail placement).
  2. In dynamic placement (line 880), `isSplitContinuation` was defined strictly as `current.id.endsWith("#part2") || current.id.endsWith("_part2")`. Continuation tasks created with random UUIDs (such as those from `TaskRepository.splitTaskIntoSessions`), tasks with explicit `parentTaskId`, or tasks formatted as `(Part 2/2)` failed this check, preventing `completionEndMap[parentId]` from being updated and failing to unlock downstream dependents in Kahn's algorithm.
- **Root Cause**:
  Missing parent completion synchronization in hysteresis placement and overly narrow ID check in dynamic placement.
- **Suggested / Applied Fix**:
  1. Added parent completion synchronization in `TaskSchedulerService.kt` hysteresis placement:
     ```kotlin
     completionEndMap[task.id] = existingWindow.endMillis
     val isSplitContinuation = task.parentTaskId != null ||
         task.id.endsWith("#part2") || task.id.endsWith("_part2") ||
         task.id.contains(Regex("""[#_]part[2-9]""")) ||
         task.title.contains(Regex("""\((Part|part) [2-9]/\d+\)"""))
     val parentId = if (isSplitContinuation) {
         task.parentTaskId ?: cleanSplitTaskId(task.id)
     } else null
     if (parentId != null && parentId != task.id) {
         completionEndMap[parentId] = existingWindow.endMillis
     }
     ```
  2. Expanded dynamic `isSplitContinuation` in `TaskSchedulerService.kt` to check `parentTaskId != null`, regex `[#_]part[2-9]`, and `(Part 2/2)` titles.
  Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-374: `TaskSchedulerService.splitIntoUltradianParts()` Phase 1 Ceiling Ignores `dayEndMillis` Causing Phase 1 to Starve Part 2 Near Workday End
- **Severity**: High
- **Category**: Core Logic & Scheduling / Ultradian Split Workday End Headroom
- **File**: [`TaskSchedulerService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L948-L953)
- **Problem Description**:
  In `TaskSchedulerService.attemptSplitPlacement(ceilingToUse)`:
  ```kotlin
  val p1BufferMins = 15
  val p1Ceiling = if (ceilingToUse != Long.MAX_VALUE) {
      ceilingToUse - (p1BufferMins * 60_000L + p2DurationMillis)
  } else {
      Long.MAX_VALUE
  }
  ```
  When a task had no explicit deadline, `ceilingToUse` defaulted to `Long.MAX_VALUE`. Consequently, `p1Ceiling` remained `Long.MAX_VALUE`. In `findBestCircadianWindow`, Part 1 evaluated all free intervals, and could select a slot late in the afternoon/evening (e.g. 16:20 when the user's configured `workDayEndHour` was 17:00). At 16:20, Part 1 (40m) completed at 17:00, leaving zero minutes for the 15-minute recovery buffer and Part 2 before `dayEndMillis`. Phase 2 then failed to place Part 2, aborting the split placement and causing the entire task to be deferred to tomorrow, even when ample space existed earlier in the workday (e.g. morning peak slots).
- **Root Cause**:
  `p1Ceiling` only considered the task's deadline ceiling without bounding by `dayEndMillis`.
- **Suggested / Applied Fix**:
  Bounded `p1Ceiling` by `min(ceilingToUse, dayEndMillis)`:
  ```kotlin
  val p1BufferMins = 15
  val effectiveCeiling = min(ceilingToUse, dayEndMillis)
  val p1Ceiling = if (effectiveCeiling != Long.MAX_VALUE) {
      effectiveCeiling - (p1BufferMins * 60_000L + p2DurationMillis)
  } else {
      Long.MAX_VALUE
  }
  ```
  Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-375: `TaskSchedulerService.cleanSplitTaskId()` and `DependencyGraphValidator.sanitizeDag()` Use Naive `substringBefore("_part")` Corrupting Task IDs and Silently Dropping Unplaced Part 2
- **Severity**: High
- **Category**: Core Logic & Data Integrity / Split Task ID Regex Safety & Completion Sweep
- **File**: [`TaskSchedulerService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L1098), [`TaskSchedulerService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L1136), [`DependencyGraphValidator.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/DependencyGraphValidator.kt#L34-L36)
- **Problem Description**:
  1. `cleanSplitTaskId(id)` was implemented as `id.substringBefore("#part").substringBefore("_part")`. Any legitimate task containing `_part` followed by non-digits (e.g. `"team_party"`, `"project_partner"`, `"legal_participant"`) was truncated to `"team"`, `"project"`, `"legal"`, distorting deferral rationales and grouping unrelated tasks under truncated parent keys.
  2. In the unplaced task sweep (line 1098), `isPlaced` was calculated as `placements.any { cleanSplitTaskId(it.task.id) == baseId }`. If Part 1 (`"task_a"`) was placed, but an input Part 2 task (`"task_a_part2"`) could not be placed due to time constraints, `cleanSplitTaskId("task_a_part2")` evaluated to `"task_a"`, which matched Part 1. Consequently, `isPlaced` was marked true for Part 2, causing the unplaced Part 2 task to be silently dropped without any placement or deferral rationale.
  3. `DependencyGraphValidator.sanitizeDag()` filtered continuation parts strictly with `it.id.contains("_part") || it.id.contains("#part")`, failing to chain downstream dependencies for continuation tasks whose IDs were UUIDs (from `splitTaskIntoSessions`).
- **Root Cause**:
  Destructive string prefix matching instead of digit-specific regex and conflating Part 1 presence with Part 2 completion in the sweep loop.
- **Suggested / Applied Fix**:
  1. Updated `cleanSplitTaskId` in `TaskSchedulerService.kt`:
     ```kotlin
     internal fun cleanSplitTaskId(id: String): String =
         id.replace(Regex("""[#_]part\d+.*$"""), "")
     ```
  2. Updated the unplaced task sweep in `TaskSchedulerService.kt`:
     ```kotlin
     val isPlaced = placements.any {
         it.task.id == task.id ||
         (it.task.id.startsWith("${task.id}#part") || it.task.id.startsWith("${task.id}_part"))
     }
     ```
  3. Updated `DependencyGraphValidator.sanitizeDag()` to support `parentTaskId != null`, regex `[#_]part\d+`, and `(Part 2/)` titles.
  Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-376: `SchedulePreparationOrchestrator` Omits `parentTaskId` and Legacy `_part2` Inference from `SchedulableTask.prerequisiteIds` Causing Premature Subtask Placement
- **Severity**: Medium
- **Category**: Core Logic & Orchestration / Continuation & Subtask Dependency Synchronization
- **File**: [`SchedulePreparationOrchestrator.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt#L172-L175)
- **Problem Description**:
  In `SchedulePreparationOrchestrator.orchestrateDayScheduleWithOutcome`, when mapping `TaskEntity` to `SchedulableTask`, `prerequisiteIds` was constructed exclusively from `task.dependsOnTaskIds.split(",")`. If a task had `task.parentTaskId != null` (e.g. a subtask or split continuation part created through a flow where `dependsOnTaskIds` was empty or not synchronized), `SchedulableTask.prerequisiteIds` remained empty. In Kahn's topological sort algorithm, the subtask or continuation part had `inDegree = 0` and could be scheduled before its parent task finished.
- **Root Cause**:
  `task.parentTaskId` was omitted from `SchedulableTask.prerequisiteIds` construction.
- **Suggested / Applied Fix**:
  Updated `SchedulePreparationOrchestrator.kt` to merge `parentTaskId` and legacy `_part2` inferred parents into `prerequisiteIds`:
  ```kotlin
  val parsedPrereqs = task.dependsOnTaskIds.split(",")
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .toMutableSet()
  val parent = task.parentTaskId?.trim().orEmpty()
  if (parent.isNotBlank() && parent != task.id) {
      parsedPrereqs.add(parent)
  }
  if (task.id.endsWith("_part2")) {
      val inferredParent = task.id.removeSuffix("_part2")
      if (inferredParent.isNotBlank() && inferredParent != task.id) {
          parsedPrereqs.add(inferredParent)
      }
  }
  ```
  Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-377: Typo Edits in Smart Task Composer Unconditionally Wipe Manual Date/Time/Chip Overrides and Un-clear Explicitly Cleared Dates
- **Severity**: High
- **Category**: UI & State Management / Composer Override Invalidation
- **File**: [`SmartTaskComposerSheetV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L503-L516)
- **Problem Description**:
  In `OutlinedTextField.onValueChange`, every keystroke in `rawInput` resets all `user*Override` variables to `null` and sets `scheduledDateCleared = false`, `deadlineDateCleared = false`, `scheduledTimeCleared = false`, `deadlineTimeCleared = false`. If a user manually selects a scheduled date or deadline via DatePicker, overrides duration/priority chips, or clears an auto-inferred date, and then subsequently fixes a typo in the title or continues typing, all of their manual overrides are silently discarded and replaced by whatever raw regex/NLP inference extracts from the partial text.
- **Root Cause**:
  Coarse-grained `onValueChange` handler resets all override and clear flags indiscriminately whenever the text string changes.
- **Suggested / Applied Fix**:
  Only reset overrides if the user explicitly clears them or if the edited input contains explicit overriding keywords, or preserve explicit user overrides unless the user explicitly interacts with the chip/picker to change it.
- **Status**: Open.

---

#### BUG-378: QuickNotes AI Task Synthesis Evaluates Content Length Strictly Ignoring Note Title
- **Severity**: Medium
- **Category**: UI & NLP Inference / Working Memory AI Synthesis Trigger
- **File**: [`QuickNotesBottomSheet.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/notes/QuickNotesBottomSheet.kt#L48-L77)
- **Problem Description**:
  `LaunchedEffect(content, title)` checks `if (trimmed.length >= 5)` solely on `content.trim()`. If the user writes an actionable note entirely in the `title` field (e.g. `"Review architecture RFC Friday at 10am"`) and leaves `content` empty or short, `trimmed.length` is `< 5`. Consequently, the AI task synthesis card is never rendered, and the user cannot convert their note into an actionable task via the synthesis banner.
- **Root Cause**:
  `trimmed` is computed solely from `content` (`val trimmed = content.trim()`), ignoring `title`.
- **Suggested / Applied Fix**:
  Combine `title` and `content` when evaluating length and preparing prompt for task synthesis:
  ```kotlin
  val combinedText = listOf(title.trim(), content.trim()).filter { it.isNotBlank() }.joinToString(" — ")
  if (combinedText.length >= 5) { ... }
  ```
- **Status**: Open.

---

#### BUG-379: Deprecated `Icons.Default.HelpOutline` Vector Asset Usage in Smart Task Composer Header
- **Severity**: Low
- **Category**: Code Quality / Deprecated API
- **File**: [`SmartTaskComposerSheetV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L405)
- **Problem Description**:
  `Icons.Default.HelpOutline` is deprecated in Compose Material icons and produces compiler warnings during build.
- **Root Cause**:
  Legacy icon reference not migrated to `Icons.AutoMirrored.Filled.HelpOutline` or non-deprecated alternative.
- **Suggested / Applied Fix**:
  Replace `Icons.Default.HelpOutline` with `Icons.AutoMirrored.Filled.HelpOutline`.
- **Status**: Open.

---

#### BUG-380: Repeated `SimpleDateFormat` & `Date` Allocations on Every Item Recomposition in QuickNotes Recent Notes List
- **Severity**: Low
- **Category**: Performance & Resource Churn / Date Formatting Allocation
- **File**: [`QuickNotesBottomSheet.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/notes/QuickNotesBottomSheet.kt#L248-L249)
- **Problem Description**:
  In `QuickNotesBottomSheet`'s `items(notes)` block, `SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(note.updatedAt))` allocates a new `SimpleDateFormat` and a new `Date` instance on every note during every recomposition pass of the lazy list.
- **Root Cause**:
  Instantiating `SimpleDateFormat` inside the item composable without `remember` or static/cached helper.
- **Suggested / Applied Fix**:
  Extract a remembered or top-level/companion date formatting function or remember the formatter outside the lazy item scope.
- **Status**: Open.

---

#### BUG-381: `CalendarSyncDataSource.findEventIdByTaskId` Queries Omit `${CalendarContract.Events.DELETED} = 0` Matching Tombstoned Events and Stack Duplicate Title Prefixes
- **Severity**: High
- **Category**: Integration / Calendar Synchronization
- **File**: [`CalendarSyncDataSource.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt#L257), [`CalendarSyncDataSource.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt#L281), [`CalendarSyncDataSource.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt#L341-L358)
- **Problem Description**:
  1. In Android `CalendarContract`, when an event is deleted from a synced calendar, Android marks `CalendarContract.Events.DELETED = 1` until synced to the server. `CalendarSyncDataSource.findEventIdByTaskId` queried `CalendarContract.Events.CONTENT_URI` by package/URI and description marker without filtering for `DELETED = 0`. If a tombstoned deleted event existed, `findEventIdByTaskId` returned the deleted event ID. `upsertTaskEvent` treated this as an existing event and called `update()`, failing to insert a fresh event on the primary calendar for the active task.
  2. `task.title.removePrefix("✓ [Flow] ").removePrefix("🎯 [Flow] ").removePrefix("[Flow] ")` was case-sensitive and failed to strip `[flow]` or non-spaced prefix variants, leading to stacked prefixes (`[Flow] [Flow] ...`).
  3. `formattedDesc` unconditionally appended `\n\n[neuroflow] Managed focus block\n$taskMarker` even if `task.description` already contained it.
- **Root Cause**:
  Missing `CalendarContract.Events.DELETED = 0` clause in event queries, case-sensitive prefix stripping, and duplicate description footer appending.
- **Suggested / Applied Fix**:
  1. Added `AND ${CalendarContract.Events.DELETED} = 0` to both `selection` and `descSelection` in `findEventIdByTaskId`.
  2. Stripped prefixes safely using regex `replace(Regex("""^(?:[✓🎯]\s*)?\[flow\]\s*""", RegexOption.IGNORE_CASE), "")`.
  3. Stripped existing `[neuroflow]` footers from `task.description` before appending the standardized footer.
  Verified in [`CalendarWriteBackSyncTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/calendar/CalendarWriteBackSyncTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-382: Clearing Deadline or Scheduled Date in `SmartTaskComposerSheetV2` Leaves Dangling Intraday Times Causing Phantom Deadlines and Invisible Timeline Slots
- **Severity**: High
- **Category**: UI & Data Integrity / Dangling Intraday Time & Phantom Deadline
- **File**: [`SmartTaskComposerSheetV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L287-L359), [`SmartTaskComposerSheetV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L1142-L1144), [`SmartTaskComposerSheetV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L1244-L1246)
- **Problem Description**:
  1. In `SmartTaskComposerSheetV2.handleSave()`, when `finalDeadlineDate` evaluated to `null` (because `deadlineDateCleared == true` or no deadline date was chosen), `deadlineTime` was saved with `normalizedDeadlineTime`. In `TaskEntity.resolveDeadlineEpochMillis()`, line 75 checked `scheduledDate != null && rawTime != null -> scheduledDate + rawTime`, and line 79 checked `rawTime != null -> startOfLocalDay(referenceDateMillis) + rawTime`. Consequently, clearing the deadline date still left an active deadline at e.g. 17:00 today.
  2. For non-recurring tasks (`recurrence == Recurrence.NONE`), when `finalScheduledDate == null` (e.g. moving a task to the backlog), `scheduledTime` was saved as an intraday offset (e.g. 36,000,000L). In `TaskDao.observeTasksForDate` / `getTasksForDate`:
     ```sql
     (scheduledDate IS NULL AND scheduledTime >= :dayStart AND scheduledTime < :dayEnd)
     ```
     Because 36,000,000L < `ONE_DAY_MILLIS` and never >= `:dayStart` (an epoch timestamp), the task became permanently invisible on daily timeline queries while retaining a phantom scheduled time.
  3. When clearing the date via the '✕' icon or in `DatePickerDialog` dismiss button, `deadlineTimeCleared` and `scheduledTimeCleared` were not updated, leaving the time picker showing a stale time offset for a cleared date.
- **Root Cause**:
  Failing to nullify `deadlineTime` when `finalDeadlineDate == null`, failing to nullify `scheduledTime` when `finalScheduledDate == null && recurrence == Recurrence.NONE`, and failing to clear associated time states when dates are cleared.
- **Suggested / Applied Fix**:
  1. In `handleSave()`:
     ```kotlin
     val finalSavedDeadlineTime = if (finalDeadlineDate == null) null else normalizedDeadlineTime
     val finalSavedScheduledTime = if (finalScheduledDate == null && recurrence == Recurrence.NONE) null else normalizedScheduledTime
     ```
  2. Synchronized `userDeadlineTimeOverride = null; deadlineTimeCleared = true` when clearing deadline date.
  3. Synchronized `userScheduledTimeOverride = null; scheduledTimeCleared = true` when clearing scheduled date for non-recurring tasks.
  Verified in [`SmartTaskComposerSheetV2Test.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2Test.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-383: `SmartTaskComposerSheetV2` Recurring Task `effectiveHabitDate` Ignores `initialDate` Defaulting to Current Day in Create Mode
- **Severity**: Medium
- **Category**: UI & Chronobiology / Habit Creation Date Anchor
- **File**: [`SmartTaskComposerSheetV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L307)
- **Problem Description**:
  In `SmartTaskComposerSheetV2.handleSave()`, line 307 resolved `effectiveHabitDate` as:
  ```kotlin
  existingTask?.habitDate
      ?: finalScheduledDate
      ?: finalDeadlineDate
      ?: startOfLocalDay(System.currentTimeMillis())
  ```
  When a user opened the composer from `PlanScreenV2` for tomorrow or a future date (where `initialDate` was passed as `selectedDateMillis`) to create a recurring habit without picking an explicit time slot or deadline, `effectiveHabitDate` defaulted to today's local midnight instead of honoring `initialDate`. As a result, the habit anchored on the current day rather than the planning day selected by the user.
- **Root Cause**:
  Omission of `initialDate` in the fallback chain for `effectiveHabitDate`.
- **Suggested / Applied Fix**:
  Updated fallback chain to:
  ```kotlin
  existingTask?.habitDate
      ?: finalScheduledDate
      ?: finalDeadlineDate
      ?: initialDate
      ?: startOfLocalDay(System.currentTimeMillis())
  ```
  Verified in [`SmartTaskComposerSheetV2Test.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2Test.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-384: `TimePickerDialog` Confirmation Blocked from Setting Date When Previously Cleared Due to Stale `cleared` Flags
- **Severity**: Medium
- **Category**: UI & State Synchronization / TimePicker Confirmation Date Assignment
- **File**: [`SmartTaskComposerSheetV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L1583-L1596)
- **Problem Description**:
  In `SmartTaskComposerSheetV2.kt`, when confirming a time in `TimePickerDialog`:
  ```kotlin
  if (timePickerTarget == "deadline") {
      userDeadlineTimeOverride = millis
      deadlineTimeCleared = false
      if (effectiveDeadlineDate == null && !deadlineDateCleared) {
          userDeadlineDateOverride = effectiveScheduledDate ?: initialDate ?: startOfLocalDay(System.currentTimeMillis())
      }
  }
  ```
  If a user had previously cleared the date (e.g. `deadlineDateCleared = true`), and subsequently opened the TimePicker to choose a due time, `!deadlineDateCleared` evaluated to `false`. `userDeadlineDateOverride` was never assigned, and `deadlineDateCleared` remained `true`. Upon saving, `finalDeadlineDate` was saved as `null`, dropping the deadline date and rendering the confirmed due time ineffective.
- **Root Cause**:
  Checking `!deadlineDateCleared` / `!scheduledDateCleared` before un-clearing the date on explicit time confirmation.
- **Suggested / Applied Fix**:
  Un-cleared the date flag and populated the date override on explicit time confirmation:
  ```kotlin
  if (timePickerTarget == "deadline") {
      userDeadlineTimeOverride = millis
      deadlineTimeCleared = false
      deadlineDateCleared = false
      if (effectiveDeadlineDate == null) {
          userDeadlineDateOverride = effectiveScheduledDate ?: initialDate ?: startOfLocalDay(System.currentTimeMillis())
      }
  } else {
      userScheduledTimeOverride = millis
      scheduledTimeCleared = false
      scheduledDateCleared = false
      if (effectiveScheduledDate == null) {
          userScheduledDateOverride = effectiveDeadlineDate ?: initialDate ?: startOfLocalDay(System.currentTimeMillis())
      }
  }
  ```
  Verified in [`SmartTaskComposerSheetV2Test.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2Test.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-385: Split Continuation `#part2` Tasks and Subtasks Prematurely Eligible as Top Action in `FocusWidgetUpdateWorker` and `DailyPlanWorker`
- **Severity**: High
- **Category**: Core Logic & UI / Continuation & Dependency Graph Enforcement
- **File**: [`FocusWidgetUpdateWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L116), [`FocusWidgetUpdateWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L263-L279), [`NotificationWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L181-L191)
- **Problem Description**:
  1. In `FocusWidgetUpdateWorker.filterCandidateTasks()`:
     ```kotlin
     val isPart2Blocked = task.id.endsWith("_part2") && (
         (task.parentTaskId != null && task.parentTaskId in activeTaskIds) ||
         task.id.removeSuffix("_part2") in activeTaskIds
     )
     ```
     And in `NotificationWorker.DailyPlanWorker.doWork()`:
     ```kotlin
     val isPart2Blocked = task.id.endsWith("_part2") && (
         (task.parentTaskId != null && task.parentTaskId in activeTaskIds) ||
         task.id.removeSuffix("_part2") in activeTaskIds
     )
     ```
     `TaskSchedulerService.splitIntoUltradianParts()` generates continuation tasks with `#part2` (e.g. `"${task.id}#part2"`), and `TaskRepository.splitTaskIntoSessions()` generates UUIDs with `parentTaskId != null` and title `"... (Part 2/2)"`. In both `FocusWidgetUpdateWorker` and `NotificationWorker`, `isPart2Blocked` checks strictly `task.id.endsWith("_part2")`. Continuation tasks with `#part2`, regex `[#_]part[2-9]`, or tasks with explicit `parentTaskId` whose parent is active and incomplete evaluate `isPart2Blocked` as `false`.
  2. In `FocusWidgetUpdateWorker.doWork()` (line 116), `prerequisiteIds` in `SchedulableTask` was mapped strictly from `task.dependsOnTaskIds.split(",")`, omitting `task.parentTaskId` and continuation parent inference, enabling subtasks with empty `dependsOnTaskIds` to bypass topological dependency scoring.
  Consequently, Part 2 continuation blocks and unready subtasks can be selected as the home screen widget's spotlight top task or daily plan's top recommended task before Part 1 is finished.
- **Root Cause**:
  `isPart2Blocked` used naive `endsWith("_part2")` without supporting `#part2`, regex `[#_]part[2-9]`, `parentTaskId`, or `(Part 2/)` titles, and `SchedulableTask.prerequisiteIds` omitted `task.parentTaskId`.
- **Suggested / Applied Fix**:
  Expand continuation detection in `FocusWidgetUpdateWorker` and `NotificationWorker`:
  ```kotlin
  val isPart2Continuation = task.parentTaskId != null ||
      task.id.endsWith("#part2") || task.id.endsWith("_part2") ||
      task.id.contains(Regex("""[#_]part[2-9]""")) ||
      task.title.contains(Regex("""\((Part|part) [2-9]/\d+\)"""))
  val parentId = if (isPart2Continuation) {
      task.parentTaskId ?: task.id.replace(Regex("""[#_]part\d+.*$"""), "")
  } else null
  val isPart2Blocked = isPart2Continuation && parentId != null && parentId in activeTaskIds
  ```
  And merge `parentTaskId` into `prerequisiteIds` when building `SchedulableTask` in `FocusWidgetUpdateWorker`.
- **Status**: Open.

---

#### BUG-386: `CompleteTaskWidgetAction` Telemetry Inflation and Unbounded Sprint Duration on One-Tap Widget Completion
- **Severity**: High
- **Category**: Data Integrity & Telemetry / Overdue Sprint Capping & End Time Anchoring
- **File**: [`CompleteTaskWidgetAction.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/widget/CompleteTaskWidgetAction.kt#L49-L59)
- **Problem Description**:
  When a user completes a task via the NextActionWidget button:
  ```kotlin
  val openSession = sessionRepo?.getOpenSessionForTask(taskId)
  if (openSession != null) {
      durationMins = openSession.trackedMinutes(now)
      sessionRepo.update(
          openSession.copy(
              endedAt = now,
              pausedAt = null,
              durationMinutes = durationMins
          )
      )
  }
  ```
  `openSession.trackedMinutes(now)` computes `(now - startedAt - totalPausedMs) / 60_000f` capped only at `LIVE_SESSION_CEILING_MINUTES` (120 minutes). If a user completed a 5-minute micro-start (`JUST_5_MIN`) or 25-minute Pomodoro (`POMODORO_25`) earlier, and taps "Complete" on the widget hours later, `durationMins` is capped at 120 minutes rather than the sprint's configured target duration (5m or 25m). This fabricates up to 120 minutes of focus time in `TimeSessionEntity`, `totalFocusMinutes`, and task tracked time. Additionally, `endedAt` is anchored to current wall-clock time (`now`) rather than the true completion time of the sprint, and `openSession.totalPausedMs` fails to accumulate the ongoing pause duration (`currentPauseMs`) if the session was paused when completed.
- **Root Cause**:
  `CompleteTaskWidgetAction` does not cap duration by `openSession.sessionType` target duration, does not anchor `endedAt` to sprint completion time, and does not accumulate ongoing pause duration before updating `openSession`.
- **Suggested / Applied Fix**:
  Mirror the duration capping and end timestamp anchoring from `FocusService.handleCompleteAndStop`:
  ```kotlin
  val targetDurationSec = FocusTimerModeV2.entries.firstOrNull { it.name == openSession.sessionType }?.durationSeconds ?: 0L
  val pauseStart = openSession.pausedAt
  val currentPauseMs = if (pauseStart != null) maxOf(0L, now - pauseStart) else 0L
  val effectiveTotalPause = openSession.totalPausedMs + currentPauseMs
  val netDurationMs = maxOf(0L, now - openSession.startedAt - effectiveTotalPause)
  val maxAllowedMins = if (targetDurationSec > 0L) (targetDurationSec / 60f) else TimeSessionEntity.LIVE_SESSION_CEILING_MINUTES
  durationMins = (netDurationMs / 60000f).coerceIn(1f, maxAllowedMins)
  val calculatedEndAt = if (targetDurationSec > 0L) {
      minOf(now, openSession.startedAt + (targetDurationSec * 1000L) + effectiveTotalPause)
  } else {
      minOf(now, openSession.startedAt + (maxAllowedMins * 60_000L).toLong() + effectiveTotalPause)
  }
  sessionRepo.update(
      openSession.copy(
          endedAt = calculatedEndAt,
          pausedAt = null,
          totalPausedMs = effectiveTotalPause,
          durationMinutes = durationMins
      )
  )
  ```
- **Status**: Resolved (verified in [`FocusWidgetAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/widget/FocusWidgetAuditTest.kt)).

---

#### BUG-387: `ScheduleAutoTasksWorker` Candidate and Fixed Task Filtering Fails on `#part2` Split Continuation Tasks and ID Regex Discontinuity
- **Severity**: High
- **Category**: Core Logic & Data Integrity / Split Continuation Scheduling & Grouping
- **File**: [`ScheduleAutoTasksWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L212), [`ScheduleAutoTasksWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L232), [`ScheduleAutoTasksWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L243), [`ScheduleAutoTasksWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L260), [`ScheduleAutoTasksWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L339)
- **Problem Description**:
  In `ScheduleAutoTasksWorker.doWork()`:
  1. Lines 212, 232, 243 filter continuation tasks using strictly `.filterNot { ... || it.id.endsWith("_part2") }`. Continuation tasks with `#part2` (from `splitIntoUltradianParts`) or tasks with `parentTaskId != null` are not filtered out, causing them to be passed as independent candidates into the scheduler alongside Part 1. Furthermore, if Part 1 is already completed, Part 2 is erroneously excluded from scheduling.
  2. In line 260, `fixedTasks` checks `if (task.id.endsWith("_part2"))`, failing to correlate `#part2` tasks with their parents in `targetTasksMap`.
  3. In line 339, `placements.groupBy { it.task.id.substringBefore("#part") }` fails to strip `_part1` or `_part2` from tasks created via `_part` notation, causing Part 1 and Part 2 to be grouped under separate base keys (`"task_part1"` and `"task_part2"`), breaking transactional update and orphaned Part 2 cleanup logic.
  4. In `applyPlacementsTransactionally`, Part 2 task titles were stripped of `(Part 2/2)` suffix when placed individually.
- **Root Cause**:
  Hardcoded `endsWith("_part2")` and naive `substringBefore("#part")` string slicing instead of unified regex-based continuation and base ID extraction.
- **Suggested / Applied Fix**:
  1. Updated continuation check to:
     ```kotlin
     val isPart2Blocked: (TaskEntity) -> Boolean = { task ->
         (task.id.endsWith("_part2") || task.id.contains("#part2")) && (
             (task.parentTaskId != null && task.parentTaskId in activeTaskIds) ||
             task.id.removeSuffix("_part2").substringBefore("#part2") in activeTaskIds
         )
     }
     ```
  2. Updated `fixedTasks` filter to check both `_part2`, `#part2`, and `parentTaskId`.
  3. Updated `placementsByBaseId` to `placements.groupBy { it.task.id.replace(Regex("""[#_]part\d+.*$"""), "") }`.
  4. Preserved `(Part 2/2)` title on continuation tasks in `titleToUse`.
- **Status**: Resolved (verified in [`ScheduleAutoTasksWorkerTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/worker/ScheduleAutoTasksWorkerTest.kt)).

---

#### BUG-388: `SleepPressureRefreshWorker`, `DistractionSyncWorker`, `EnergyTelemetryCleanupWorker`, and `KioskPolicyWorker` Catch Coroutine `CancellationException` Breaking Structured Concurrency
- **Severity**: Medium
- **Category**: Concurrency & WorkManager / Structured Concurrency Contract
- **File**: [`SleepPressureRefreshWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/SleepPressureRefreshWorker.kt#L37), [`DistractionSyncWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/DistractionSyncWorker.kt#L114), [`EnergyTelemetryCleanupWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/EnergyTelemetryCleanupWorker.kt#L49), [`KioskPolicyWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/KioskPolicyWorker.kt#L23)
- **Problem Description**:
  In `doWork()`, these workers wrap background execution in `try { ... } catch (e: Exception) { ... Result.retry() }`. In Kotlin coroutines, `CancellationException` is an `Exception`. When WorkManager stops or cancels these workers, the `CancellationException` is swallowed and treated as a generic failure, prompting WorkManager to retry the cancelled job with exponential backoff instead of terminating cleanly.
- **Root Cause**:
  Omitting `if (e is kotlinx.coroutines.CancellationException) throw e` before catching generic `Exception`.
- **Suggested / Applied Fix**:
  Added `if (e is kotlinx.coroutines.CancellationException) throw e` in the catch blocks of all four worker classes.
- **Status**: Resolved (verified in unit tests).

---

#### BUG-389: `DeadlineEscalationWorker` Resurrects Iceboxed Tasks (`Quadrant.ELIMINATE`) into `DO_FIRST` Triggering Notification Spam
- **Severity**: High
- **Category**: Background Services & Notifications / Icebox State Integrity
- **File**: [`NotificationWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L410-L425)
- **Problem Description**:
  `DeadlineEscalationWorker.doWork()` queried all active tasks and filtered with:
  ```kotlin
  task.quadrant != com.neuroflow.app.domain.model.Quadrant.DO_FIRST &&
  task.recurrence == com.neuroflow.app.domain.model.Recurrence.NONE && ...
  ```
  It omitted checking `task.quadrant != Quadrant.ELIMINATE`. If a user deliberately placed an approaching or overdue task into the Icebox (`Quadrant.ELIMINATE`) to ignore it, every 4 hours the worker mutated its quadrant to `DO_FIRST` and fired repeated notification spam (`⚠️ X tasks due soon — escalated to Do First`).
- **Root Cause**:
  Missing `Quadrant.ELIMINATE` filter in `DeadlineEscalationWorker`.
- **Suggested / Applied Fix**:
  Added `task.quadrant != com.neuroflow.app.domain.model.Quadrant.ELIMINATE` to the urgentTasks filter in `NotificationWorker.kt`.
- **Status**: Resolved (verified in [`NotificationWorkerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/worker/NotificationWorkerAuditTest.kt)).

---

#### BUG-390: `AutonomyNudgeWorker` Silently Drops Nudges for Tasks with Absolute Epoch Deadline Timestamps (`deadlineDate == null`)
- **Severity**: High
- **Category**: Background Services & Notifications / Autonomy Nudge Lifecycle
- **File**: [`AutonomyNudgeWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/AutonomyNudgeWorker.kt#L55-L65)
- **Problem Description**:
  In `AutonomyNudgeWorker.doWork()`, the worker checked:
  ```kotlin
  val sDate = task.effectiveScheduledDateMillis()
  if (sDate == null && task.deadlineDate == null) return Result.success()
  ```
  When a task is scheduled with a deadline time as an absolute epoch timestamp (e.g. `deadlineTime >= ONE_DAY_MILLIS`), `task.deadlineDate` is `null`. When `AutonomyNudgeWorker` wakes up at the deadline, it evaluated `sDate == null && task.deadlineDate == null` as true, mistaking the timed task for an unscheduled backlog task, and immediately exited without showing the autonomy nudge notification.
- **Root Cause**:
  Checking raw `task.deadlineDate` rather than resolved deadline epoch timestamp `task.resolveDeadlineEpochMillis(now)`.
- **Suggested / Applied Fix**:
  Updated `AutonomyNudgeWorker.kt` to resolve deadline epoch:
  ```kotlin
  val sDate = task.effectiveScheduledDateMillis()
  val dEpoch = task.resolveDeadlineEpochMillis(now)
  if (sDate == null && dEpoch == null) return Result.success()
  if ((sDate != null && sDate > dayEnd) || (sDate == null && dEpoch != null && dEpoch > dayEnd)) return Result.success()
  ```
- **Status**: Resolved (verified in [`AutonomyNudgeWorkerTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/worker/AutonomyNudgeWorkerTest.kt)).

---

#### BUG-391: `NeuralSoundscapePlayer.play()` and `stop()` Fail to Clear `isReleasePending`, Causing Premature `AudioTrack` Teardown on Rapid Re-Entry
- **Severity**: Medium
- **Category**: Audio DSP / Playback Lifecycle & Native AudioTrack Release
- **File**: [`NeuralSoundscapePlayer.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapePlayer.kt#L143-L226)
- **Problem Description**:
  When `releaseWithFadeOut()` is called (e.g. upon leaving a focus screen), `isReleasePending.set(true)` is set. If the user quickly re-enters the focus screen or restarts audio before the 400ms fade-out thread finishes, `play()` was invoked, but `isReleasePending` was never cleared. Consequently, when the fade finished or the thread exited, line 396 evaluated `isReleasePending.get() == true`, released the native `AudioTrack`, and reset the generator, terminating the newly started playback session.
- **Root Cause**:
  `isReleasePending.set(false)` was missing from `play()` and `stop()`.
- **Suggested / Applied Fix**:
  Added `isReleasePending.set(false)` in both `play()` and `stop()` in `NeuralSoundscapePlayer.kt`.
- **Status**: Resolved (verified in unit tests).

---

#### BUG-392: `FocusService.onStartCommand(ACTION_START)` Fails to Cancel Running `tickerJob` When Session is Started Paused
- **Severity**: Low
- **Category**: Background Services & Performance / Ticker Job Lifecycle
- **File**: [`FocusService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L133)
- **Problem Description**:
  In `FocusService.onStartCommand()`, under `ACTION_START`, if `!isPaused` was false (the session was started or synchronized in a paused state), `startTicker()` was skipped, but any previously active `tickerJob` was not cancelled. The while-loop inside `tickerJob` continued to tick every 1000ms in the background, consuming CPU resources while the service was paused.
- **Root Cause**:
  Missing `else` branch to cancel `tickerJob` when `isPaused` is true in `ACTION_START`.
- **Suggested / Applied Fix**:
  Added `else { tickerJob?.cancel(); tickerJob = null }` to `ACTION_START` in `FocusService.kt`.
- **Status**: Resolved (verified in unit tests).'''

detailed_new_entries = r'''

---

#### BUG-393: Zombie Open Session Leakage and Tracked Minutes Dropped on Swipe/Notification Completion
- **Severity**: High
- **Category**: Concurrency & Database / Session Lifecycle
- **File**: [`TaskRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L360)
- **Problem Description**:
  When completing a task via swipe or notifications (`completeAndRecur(task, now)` or `markTaskCompleted(taskId)`), running sessions (`endedAt == null`) remained open in the `time_sessions` table. Because `splitTaskIntoSessions()` properly closed running sessions but `completeAndRecur()` did not, swiping to complete or completing via notifications permanently leaked open sessions into `time_sessions` and dropped focus minutes from historical session summaries and daily metrics.
- **Root Cause**:
  `completeAndRecur()` did not query or finalize active sessions in `timeSessionDao` before marking the task completed, leaving zombie open sessions where `endedAt` remained null.
- **Suggested / Applied Fix**:
  In `TaskRepository.completeAndRecur()`, query `timeSessionDao.getOpenSessionByTaskId(task.id)`; if present, calculate `trackedMinutes` up to `nowMillis`, set `endedAt = nowMillis`, update the session via `timeSessionDao.update(closedSession)`, and then delete any remaining dangling open sessions.
- **Status**: Resolved (verified in unit tests).

---

#### BUG-394: Corrupted Task IDs and Title Slicing on Slug String Truncation in Task Deletion and Auto-Schedule Grouping
- **Severity**: Medium
- **Category**: Data Integrity / Task Splitting & Slug Extraction
- **File**: [`TaskRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L320), [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L584)
- **Problem Description**:
  In `TaskRepository.delete()`, parent ID derivation used `task.id.substringBefore("#part").substringBefore("_part")`. When deleting a task whose slug naturally contained `_part` (e.g. `car_parts_catalog#part2`), `substringBefore("_part")` sliced the string at the first occurrence, falsely corrupting the parent ID to `car`. In `PlanViewModelV2.kt`, auto-schedule grouping used `substringBefore("#part")` which failed to recognize underscore-separated split continuations (`_part2`).
- **Root Cause**:
  Naive string prefix truncation instead of targeted regex anchoring to the continuation suffix `[#_]part\d+.*$`.
- **Suggested / Applied Fix**:
  Standardized continuation slug stripping across `TaskRepository.delete()` and `PlanViewModelV2.kt` using `replace(Regex("""[#_]part\d+.*$"""), "")`.
- **Status**: Resolved (verified in unit tests).

---

#### BUG-395: Part 2 Continuation Recurrence Advancement and Ghost Recurring Task Duplication
- **Severity**: High
- **Category**: Scheduling / Recurrence Invariants
- **File**: [`TaskRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L340)
- **Problem Description**:
  When an ultradian split task with recurrence (e.g., a daily habit split into Part 1 and Part 2) had its Part 2 continuation completed via `completeAndRecur()`, the recurrence guard allowed recurrence calculation to execute if `task.parentTaskId` was null. This advanced the recurrence date and spawned a duplicate recurring task for tomorrow, while leaving Part 1 detached.
- **Root Cause**:
  The recurrence guard in `completeAndRecur()` did not check for continuation identifiers (`#part2`, `_part2`, or title suffix `(Part 2/)`) on split tasks where `parentTaskId` was not explicitly populated.
- **Suggested / Applied Fix**:
  Added explicit guard in `completeAndRecur()`: if `task.parentTaskId != null || task.id.contains("#part2") || task.id.contains("_part2") || task.title.contains("(Part 2/")`, return `null` immediately and do not advance recurrence. Part 1 base tasks advance recurrence and strip `(Part 1/2)`.
- **Status**: Resolved (verified in unit tests).

---

#### BUG-396: Checklist and Micro-Step Dropping on Split Task Sessions
- **Severity**: Medium
- **Category**: Data Persistence / Micro-Steps
- **File**: [`TaskRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L455)
- **Problem Description**:
  When splitting an overloaded task into sessions via `TaskRepository.splitTaskIntoSessions()`, the parent task's associated `task_micro_steps` were not cloned to the newly created Part 1 session (`createdSessions.first()`). Consequently, opening the split task in Focus mode showed an empty checklist, dropping user progress and subtask checklists.
- **Root Cause**:
  `splitTaskIntoSessions()` created new task entities for Part 1 and Part 2 and closed active time sessions, but did not query `taskMicroStepDao` to copy existing micro-steps to the new Part 1 entity.
- **Suggested / Applied Fix**:
  In `splitTaskIntoSessions()`, query `taskMicroStepDao.getByTaskId(parentTask.id)` and clone all micro-step entities with new UUIDs targeting `createdSessions.first().id`, preserving titles, sort order, and completion statuses.
- **Status**: Resolved (verified in unit tests).

---

#### BUG-397: Unbounded Memory Scanning in Session Interruption Summary Computation
- **Severity**: Medium
- **Category**: Performance / Database Query Optimization
- **File**: [`TimeSessionDao.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/TimeSessionDao.kt#L42), [`SessionRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/SessionRepository.kt#L41)
- **Problem Description**:
  `SessionRepository.getInterruptionSummary(fromMillis)` called `timeSessionDao.getAll().filter { it.startedAt >= fromMillis }`. As users accumulate hundreds or thousands of focus sessions over months, loading the entire table into memory on every metrics screen visit creates unbounded heap pressure and garbage collection stutter on 7.4 GB host/mobile devices.
- **Root Cause**:
  Missing indexed range query in `TimeSessionDao`, forcing full table retrieval into Kotlin collection memory.
- **Suggested / Applied Fix**:
  Added `@Query("SELECT * FROM time_sessions WHERE startedAt >= :fromMillis ORDER BY startedAt DESC") suspend fun getSessionsSince(fromMillis: Long): List<TimeSessionEntity>` to `TimeSessionDao` and refactored `SessionRepository.getInterruptionSummary(fromMillis)` to invoke it directly.
- **Status**: Resolved (verified in unit tests).'''

if detailed_target in content:
    content = content.replace(detailed_target, detailed_target + detailed_new_entries)
    print('Detailed findings updated.')
else:
    print('ERROR: Could not find detailed_target!')

with open('BUG_REPORT.md', 'w') as f:
    f.write(content)
print('Saved updated BUG_REPORT.md successfully.')

---

#### BUG-398: `MainActivity.onCreate` Omits In-Flight Async Task Resolution Resumption Leaking Zombie Resolution State Across Activity Recreation
- **Severity**: High
- **Category**: UI & State Synchronization / Async Lifecycle Race
- **File**: [`MainActivity.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L224)
- **Problem Description**:
  When `ACTION_OPEN_FOCUS` arrived without a task ID, `isResolvingFocusTask` was set to `true` and a background coroutine was launched in `lifecycleScope` to query Room. If the activity was destroyed and recreated (such as on screen rotation or configuration change) while this query was in flight, the coroutine was cancelled. In `onCreate`, `savedInstanceState.getBoolean("saved_resolving_focus_task")` restored `isResolvingFocusTask = true`, but no coroutine was ever started to resume the query. Consequently, `isResolvingFocusTask` remained permanently stuck as `true`, latching `hasDirectIntent = true` forever, permanently suppressing `NewChapterCard` and `TopGoalsRefillCard`, never navigating to the resolved task, and stranding the user on an inert screen.
- **Root Cause**:
  Restoring `isResolvingFocusTask = true` from `savedInstanceState` without re-triggering `resolveDefaultFocusTask()` to complete the interrupted query.
- **Suggested / Applied Fix**:
  Extracted `resolveDefaultFocusTask()` in `MainActivity.kt`. When restoring from `savedInstanceState`, if `wasResolving` is true and `initialFocusTaskId == null && initialWoopTaskId == null`, re-trigger `resolveDefaultFocusTask()`. Otherwise, reset `isResolvingFocusTask = false`.
- **Status**: Resolved (verified in unit tests).

---

#### BUG-399: `ACTION_OPEN_FOCUS` Fallback Task Selection Omits Multi-Part Continuation Tasks (`#part3`, `_part3`, etc.) Prematurely Selecting Split Tasks Ahead of Active Parents
- **Severity**: Medium
- **Category**: Core Logic & Task Scheduling / Split Task Continuation Prerequisite Check
- **File**: [`MainActivity.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L130)
- **Problem Description**:
  In `MainActivity` fallback task selection for `ACTION_OPEN_FOCUS`, the filter only guarded against `(it.id.endsWith("_part2") || it.id.contains("#part2"))` and stripped the parent ID using naive `removeSuffix("_part2").substringBefore("#part2")`. For tasks split into 3 or more ultradian sessions (Part 3, Part 4), Part 3 was not detected as a continuation part. If Part 3 was marked as a Frog task, it was selected ahead of Part 1 even when Part 1 remained active in the database.
- **Root Cause**:
  Hardcoded `_part2` string matching instead of standardized regex continuation matching.
- **Suggested / Applied Fix**:
  Standardized continuation detection using `it.id.contains(Regex("""[#_]part\d+""")) || it.parentTaskId != null` and derived parent ID via `it.parentTaskId ?: it.id.replace(Regex("""[#_]part\d+.*$"""), "")`. Added `else` fallback in `handleIntent` to ensure valid task IDs from non-standard intent actions are never dropped.
- **Status**: Resolved (verified in unit tests).

---

#### BUG-400: Missing `launchSingleTop = true` on Sub-Screen Navigation Routes in `NeuroFlowAppV2.kt` Allowing Duplicate Backstack Stacking on Rapid Clicks
- **Severity**: Medium
- **Category**: UI & Navigation / Backstack Integrity & Rapid Click Debouncing
- **File**: [`NeuroFlowAppV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L229-L280)
- **Problem Description**:
  In `NeuroFlowAppV2.kt`, navigation callbacks for sub-screens (`settings`, `meq_quiz`, `sleep_logs`, `notes`, `weekly_schedule`, `analytics`, `history`, `identity`, `log_time`, `priority_weights`, `hyperfocus_allowlist`, `app_guide`, `privacy_permissions`, `account_sync`) invoked `navController.navigate("...")` without `{ launchSingleTop = true }`. Rapid double-tapping on buttons or list items pushed multiple identical destination entries onto the navigation backstack, creating duplicate stacked screens that required repeated back button presses to exit.
- **Root Cause**:
  Missing `launchSingleTop = true` navigation builder option on secondary route navigations.
- **Suggested / Applied Fix**:
  Configured `{ launchSingleTop = true }` across all sub-screen navigation routes in `NeuroFlowAppV2.kt`.
- **Status**: Resolved (verified in unit tests).

---

#### BUG-401: External Focus / WOOP Intent Pushes Duplicate Focus Destination Over Active Focus Session
- **Severity**: Medium
- **Category**: UI & State Management / Focus Backstack Stacking
- **File**: [`NeuroFlowAppV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L77-L108)
- **Problem Description**:
  In `NeuroFlowAppV2.kt`, `LaunchedEffect(initialTaskId)` and `LaunchedEffect(initialWoopTaskId)` navigated to `"focus_v2/$encodedId"` with `launchSingleTop = true`. When a user was already inside an active focus screen (`focus_v2/{taskId}` or legacy `focus/{taskId}`) and an intent or notification arrived for a different task, `launchSingleTop` did not match because the route arguments differed. This pushed the new focus destination on top of the old one. Pressing the system Back button on the new task returned the user to the old, abandoned focus session rather than returning to the Flow or Plan hub.
- **Root Cause**:
  Failure to pop the existing focus destination when navigating to a new focus task from an external intent.
- **Suggested / Applied Fix**:
  In `LaunchedEffect(initialTaskId)` and `LaunchedEffect(initialWoopTaskId)`, checked if `currentRoute.startsWith("focus_v2") || currentRoute.startsWith("focus/")`; if true, popped the current focus destination using `popUpTo(currentDestId) { inclusive = true }` before navigating to the new task.
- **Status**: Resolved (verified in unit tests).

---

#### BUG-402: Split Continuation Part 2+ Tasks Excluded From Auto-Scheduling and Dropped/Deleted When Parent Completed in `PlanViewModelV2.autoScheduleDay()`
- **Severity**: High
- **Category**: Core Logic & Scheduling / Split Continuation Lifecycle
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L526-L670)
- **Problem Description**:
  In `PlanViewModelV2.autoScheduleDay()`, candidate task filtering unconditionally excluded all tasks ending with `_part2` (`!task.id.endsWith("_part2")`). Consequently, once a user completed Part 1 of a split task, Part 2 was permanently barred from being auto-scheduled into the user's daily plan. Furthermore, if a continuation task was identified by `#part2` or had `parentTaskId != null`, it bypassed `!task.id.endsWith("_part2")` and was submitted to `scheduleOrchestrator`, but upon receiving placements, `placementsByBaseId` derived `baseTaskId` by stripping continuation suffixes and fetched the completed parent task (`taskRepository.getById(baseTaskId)`). Because the parent's status was `COMPLETED`, the loop executed `continue`, silently dropping the placement. If the parent happened to be active, the single placement failed the `isSplit` check and executed the orphan deletion branch, permanently deleting the user's active Part 2 task entity from Room.
- **Root Cause**:
  Hardcoded exclusion of `_part2` without checking if the parent task was still active in `allTasks`, coupled with assuming `baseTaskId` always corresponds to an active Part 1 task rather than recognizing standalone continuation placements.
- **Suggested / Applied Fix**:
  1. Standardized continuation task detection via `val isContinuation = task.id.contains(Regex("""[#_]part[2-9]""")) || task.id.endsWith("_part2") || task.parentTaskId != null`.
  2. Checked if parent is active: `val parentId = task.parentTaskId ?: task.id.removeSuffix("_part2").substringBefore("#part2")`. Part 2 tasks are only excluded when their parent is active (`!isParentActive`).
  3. In `fixedWindows`, similarly excluded continuation tasks if their parent is in `candidateIds`.
  4. In placement application, detected standalone continuation tasks (`isStandalonePart2`), resolved `targetTaskId = if (isStandalonePart2) primaryPlacement.task.id else baseTaskId`, preserved `fresh.title`, updated schedules, and guarded orphan deletion so active standalone continuation tasks are never deleted.
  5. Fallback-checked both `${baseTaskId}_part2` and `${baseTaskId}#part2` when locating existing Part 2 entities.
- **Status**: Resolved (verified in unit tests).

---

#### BUG-403: Overdue Past Tasks Completely Vanish from `PlanScreenV2` on Today's Plan View
- **Severity**: High
- **Category**: UI & State Synchronization / Overdue Task Visibility
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L355-L382)
- **Problem Description**:
  When a user opened `PlanScreenV2` targeting today, any active task scheduled for a past date (`effectiveScheduledDateMillis() < dayStart`) was completely omitted from the timeline because `tasksForDay` strictly required `sDate in dayStart..dayEnd`. At the same time, `unscheduledTasks` strictly required `task.effectiveScheduledDateMillis() == null`. Because the task had a non-null past date, it was also excluded from the Unscheduled Staging Drawer. Furthermore, `DateCarouselStrip` only rendered days `0..6` (today and 6 future days), making past dates unreachable. Active tasks that slipped past yesterday completely vanished from the user's visibility on the Plan screen.
- **Root Cause**:
  `tasksForDay` did not account for `isTargetingToday && sDate <= dayEnd` (unlike `FlowViewModelV2` and `autoScheduleDay`), and `unscheduledTasks` did not catch past overdue tasks on future-day views.
- **Suggested / Applied Fix**:
  1. In `PlanViewModelV2.uiState`, when `isTargetingToday` is true, include tasks with `sDate <= dayEnd` (and `rawScheduledTime <= dayEnd` and `dDate <= dayEnd`) in `tasksForDay`, matching `FlowViewModelV2` and ensuring overdue tasks are visible on today's timeline.
  2. In `unscheduledTasks`, allowed past overdue tasks (`sDate != null && sDate < todayStart`) to appear in the staging drawer when viewing future days (`!isTargetingToday`), ensuring overdue tasks can be scheduled into upcoming days.
- **Status**: Resolved (verified in unit tests).

---

#### BUG-404: Integer Overflow Past Midnight in `PlanViewModelV2.assignTaskTime` and `resolveConflict` Corrupts Scheduled Date to 1970
- **Severity**: High
- **Category**: UI & Temporal Integrity / Midnight Epoch Overflow
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L702), [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L1075)
- **Problem Description**:
  In `PlanViewModelV2.assignTaskTime(task, hour, minute)`, `millisInDay` was computed as `hour * 3600_000L + minute * 60_000L` without bounds checking. When `resolveConflict()` calculated `suggestedMinute / 60` for a conflict resolved at midnight (minute 1440), `hour` became `24`, producing `millisInDay = 86_400_000L` (`ONE_DAY_MILLIS`). Because `TaskTimeExtensions.kt` treats any `scheduledTime >= ONE_DAY_MILLIS` as an absolute epoch timestamp, `startOfLocalDay(86_400_000L)` interpreted this as January 2, 1970, instantly corrupting the task's schedule date to epoch day 1.
- **Root Cause**:
  Missing modulo and range coercion on `hour` and `minute` inputs in `assignTaskTime()` and un-clamped `suggestedMinute` in `resolveConflict()`.
- **Suggested / Applied Fix**:
  1. Sanitized hour and minute in `assignTaskTime`: `val safeHour = (hour % 24).coerceIn(0, 23)` and `val safeMinute = (minute % 60).coerceIn(0, 59)`.
  2. In `resolveConflict`, sanitized `val safeMinuteOfDay = (suggestedMinute % 1440).coerceIn(0, 1439)`.
  3. Added user confirmation message upon time assignment: `"Scheduled '...' for HH:mm ⚡"`.
- **Status**: Resolved (verified in unit tests).

---

#### BUG-405: Un-normalized Absolute Epoch Deadlines and Dropped Context Tags in `PlanViewModelV2.createTaskFromNote` and `addTask`
- **Severity**: Medium
- **Category**: Core Data Integrity / Temporal Normalization & Tag Sync
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L804-L880)
- **Problem Description**:
  In `PlanViewModelV2.createTaskFromNote()` and `addTask()`, tasks created with absolute epoch deadline timestamps (`deadlineTime >= ONE_DAY_MILLIS`) were stored directly in Room without decomposing into `deadlineDate` and intraday `deadlineTime`. In `createTaskFromNote()`, `inferred.contextTags` were joined into generic comma-separated `tags` but omitted from `newTask.contextTag`. Furthermore, `createTaskFromNote()` and `updateTask()` unconditionally invoked `AutonomyNudgeEngine.scheduleNudge()` even when tasks were placed in `Quadrant.ELIMINATE` (Icebox) or were non-active.
- **Root Cause**:
  Missing normalization of `deadlineTime >= ONE_DAY_MILLIS` and missing quadrant/status checks before scheduling autonomy nudges.
- **Suggested / Applied Fix**:
  1. In `addTask()` and `createTaskFromNote()`, decomposed any `scheduledTime` or `deadlineTime` exceeding `ONE_DAY_MILLIS` into `startOfLocalDay` dates and intraday time offsets.
  2. Populated `contextTag = inferred.contextTags.firstOrNull().orEmpty()` in `createTaskFromNote()`.
  3. Automatically inserted initial micro-steps into `taskMicroStepDao` in `createTaskFromNote()`.
  4. Guarded all autonomy nudge scheduling with `if (task.quadrant != Quadrant.ELIMINATE && task.status == TaskStatus.ACTIVE)`.
- **Status**: Resolved (verified in unit tests).

---

#### BUG-406: Multi-Part Continuation Tasks (`#part3`, `_part3`, etc.) and Trailing Commas in Dependencies Bypass Blocking in `FlowViewModelV2.kt` & `FlowScreenV2.kt`
- **Severity**: High
- **Category**: UI & Core Logic / Continuation Prerequisite & Dependency Integrity
- **File**: [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L369-L382), [`FlowScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt#L246-L265)
- **Problem Description**:
  1. In `FlowViewModelV2.kt` (lines 371-376 and 428-433) and `FlowScreenV2.kt` (lines 249-254), continuation task prerequisite checks strictly matched `task.id.endsWith("_part2") || task.id.contains("#part2")`. Any multi-part continuation beyond Part 2 (`#part3`, `_part3`, `(Part 3/3)`, or tasks referencing `parentTaskId`) evaluated `isPart2Continuation` to false and was never detected as blocked, leaking into `actionableActiveTasks`, getting recommended, and getting spotlighted in `FlowSpotlightHeroCard` while the parent task or earlier parts remained incomplete.
  2. In `FlowViewModelV2.kt` line 370, `task.dependsOnTaskIds.split(",").map { it.trim() }.all { it in completedIds }` omitted `.filter { it.isNotBlank() }`. Any task entity with trailing commas or whitespace (e.g. `"id1, "`) resulted in `"" in completedIds` evaluating to false, permanently blocking the task from recommendation even when all prerequisites were satisfied.
- **Root Cause**:
  Hardcoded Part 2 suffix checks instead of dynamic multi-part regex matching, and missing blank token filtering on comma-separated dependency string splits.
- **Suggested / Applied Fix**:
  1. Introduced dynamic multi-part regex matching `[#_]part(\d+)` and `(Part (\d+)/` alongside `parentTaskId` in both `FlowViewModelV2.kt` and `FlowScreenV2.kt`, checking whether the parent task, Part 1, or any preceding part (`prevPart in 1 until partNum`) is currently active.
  2. Added `.filter { it.isNotBlank() }` to `dependsOnTaskIds` parsing in `FlowViewModelV2.kt`.
  3. Added `takeIf { isTaskActionable(it) }` to `recommendedTask` ingestion in `FlowSpotlightHeroCard` selection logic.
  4. Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-407: Un-normalized Absolute Epoch Deadlines, Omitted `contextTag`, and Unconditional Autonomy Nudge on Icebox Tasks in `FlowViewModelV2.kt`
- **Severity**: Medium
- **Category**: Core Data Integrity / Temporal Normalization & Tag Sync
- **File**: [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L586-L740)
- **Problem Description**:
  1. In `FlowViewModelV2.addTask()` and `addTaskFromNote()`, tasks with absolute epoch deadline timestamps (`deadlineTime >= ONE_DAY_MILLIS`) were persisted without decomposing into `deadlineDate` and intraday `deadlineTime`.
  2. In `addTaskFromNote()`, inferred context tags were combined into `tags` but omitted from `newTask.contextTag`.
  3. In `addTask()`, `addTaskFromNote()`, and `updateTask()`, `AutonomyNudgeEngine.scheduleNudge()` was called unconditionally, scheduling reminder notifications for Icebox tasks (`Quadrant.ELIMINATE`) or completed/non-active tasks instead of canceling them.
- **Root Cause**:
  Missing deadline epoch decomposition and missing quadrant/status checks on autonomy nudge invocation in `FlowViewModelV2`.
- **Suggested / Applied Fix**:
  1. Decomposed any `deadlineTime >= ONE_DAY_MILLIS` into `startOfLocalDay` date and intraday offset in `addTask()` and `addTaskFromNote()`.
  2. Set `contextTag = inferred.contextTags.firstOrNull().orEmpty()` in `addTaskFromNote()`.
  3. Guarded `AutonomyNudgeEngine.scheduleNudge()` with `if (task.quadrant != Quadrant.ELIMINATE && task.status == TaskStatus.ACTIVE)`.
  4. Added `AutonomyNudgeEngine.cancelNudge()` in `updateTask()` when a task is moved to `Quadrant.ELIMINATE` or non-active status.
  5. Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-408: `FlowViewModelV2.deferTask()` Anchors Base Epoch to Midnight or Past for Overdue and Untimed Tasks Leaving Tasks Overdue
- **Severity**: High
- **Category**: UI & Temporal Integrity / Task Deferral Anchor
- **File**: [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L943-L965)
- **Problem Description**:
  When swiping to defer or tapping "Defer +2h" in `FlowScreenV2` on an untimed task (`scheduledTime == null`) or an overdue task from earlier in the day/past days:
  `task.resolveScheduledEpochMillis(now)` returned `scheduledDate + 0L` (midnight 00:00:00).
  Adding `hours * 3600_000L` resulted in `newEpoch = 02:00:00 AM` of the scheduled day. If the current time was 14:00 PM, the deferred task was scheduled for 02:00 AM this morning (12 hours in the past!), leaving the task still flagged as overdue. Similarly, deferring an overdue morning task (e.g. 09:00 AM) by 2 hours at 14:00 PM produced 11:00 AM (still in the past).
- **Root Cause**:
  Computing deferral epoch offsets from past schedule anchors instead of `maxOf(now, currentEpoch)`.
- **Suggested / Applied Fix**:
  1. Defined `val baseEpoch = if (task.scheduledTime == null || currentEpoch < now) now else currentEpoch`.
  2. Calculated `val newEpoch = baseEpoch + hours * 3600_000L`, ensuring deferred tasks are always placed into the future relative to the current moment.
  3. Added `calendarSyncRepository?.handleTaskStateChanged(updated)` to `logPastTime()` to synchronize retroactive logs with external calendars.
  4. Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-409: Overdue Tasks Mapped to Midnight (00:00) on Today's Circadian Curve in `EnergyTimelineStream.kt`, Multi-Part Continuation Label Collapses to `Part 1/2`, and Interleaved Untimed Tasks Mask Break Resets
- **Severity**: Medium
- **Category**: UI & Circadian Alignment / Timeline Energy Badge & Ultradian Buffer
- **File**: [`EnergyTimelineStream.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt#L114-L690)
- **Problem Description**:
  1. In `EnergyTimelineStream.kt` lines 114-120 and 434-445, `taskPhase` and `TaskSlotEnergyFitBadge` compared `task.resolveScheduledEpochMillis()` directly against `timeline` timestamps. For overdue tasks scheduled on previous days, the minimum distance to today's 24-hour timeline always matched `todayStart` (00:00:00 midnight), causing 14:00 PM tasks to display as midnight sleep/dip windows with false "Dip Mismatch" badges.
  2. In `TimelineTaskCard`, multi-part continuation tasks (e.g. `(Part 3/3)` or `#part3`) checked only `#part2`/`_part2`/`(Part 2/2)`, falling back to `"🔗 Part 1/2"` for Part 3.
  3. In `shouldShowUltradianBufferAfter`, split part detection only handled Part 1 and Part 2, ignoring Part 2 to Part 3 transitions, and the 15-minute natural break check inspected only `tasks[i - 1].scheduledTime != null`, causing interleaved untimed tasks to mask break resets across long temporal gaps.
- **Root Cause**:
  Direct absolute epoch comparison against day-bounded circadian curves, binary Part 1/2 string checks, and immediate neighbor inspection in buffer accumulators.
- **Suggested / Applied Fix**:
  1. Aligned task timestamps to today's timeline using `alignedMillis = timelineStart + task.timelineStartMinuteOfDay() * 60_000L`.
  2. Dynamically parsed multi-part labels with `Regex("""\(Part (\d+/\d+)\)""")` and `Regex("""[#_]part(\d+)""")`.
  3. Enhanced `shouldShowUltradianBufferAfter` with consecutive split part detection (`nextPart == curPart + 1`) and `lastTimedEndMinute` tracking to maintain natural break resets across interleaved untimed tasks.
  4. Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-410: Multi-Part Continuation Tasks (`#part3`, `_part3`, etc.) and Active Subtasks Ingested Out-of-Order in `FocusViewModelV2.findNextActionableTask()`
- **Severity**: High
- **Category**: UI & Core Logic / Continuation Prerequisite & Subtask Integrity
- **File**: [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L1060-L1110)
- **Problem Description**:
  In `FocusViewModelV2.findNextActionableTask()`, multi-part continuation tasks were filtered using naive hardcoded string checks (`endsWith("_part2") || contains("#part2")`). Consequently, continuation tasks with three or more parts (e.g., `#part3`, `_part3`, or `(Part 3/3)`), subtasks with an active parent task (`task.parentTaskId != null`), and multi-part continuation tasks whose preceding parts were still active were evaluated as actionable. These unready continuation parts were selected as the `nextTaskId` recommended on the Focus screen and in the "Next Task" prompt upon completing a task, violating execution order and dependency graph invariants.
- **Root Cause**:
  Hardcoded Part 2 string checks and omission of parent task / preceding part completion checks in `findNextActionableTask()`.
- **Suggested / Applied Fix**:
  1. Replaced hardcoded string matching with regex-based multi-part continuation detection (`[#_]part(\d+)` and `\(Part (\d+)/\d+\)`).
  2. Verified whether any preceding part (`prevPart in 1 until partNum`) or parent task (`task.parentTaskId != null`) remains in the active tasks pool before allowing a task to be considered actionable.
  3. Filtered out any task whose explicitly declared dependencies (`task.dependencyIds()`) are still active.
  4. Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-411: Future Scheduled Day Tasks Ingested Ahead of Today's Backlog & Stale Fallback on Queue Exhaustion in `FocusViewModelV2.findNextActionableTask()`
- **Severity**: High
- **Category**: UI & Temporal Integrity / Focus Queue Prioritization & Navigation Flow
- **File**: [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L644-L1110)
- **Problem Description**:
  1. In `FocusViewModelV2.findNextActionableTask()`, tasks scheduled for future calendar days (`scheduledStart > dayEnd || sDate > dayEnd`) were not filtered out. Because candidate tasks were sorted using `.thenBy { it.scheduledTime == null }`, timed tasks scheduled weeks into the future sorted ahead of today's unscheduled backlog tasks, recommending distant future tasks as the immediate next focus item.
  2. In `FocusViewModelV2.completeTask()`, when all actionable tasks in the backlog were finished, `nextTaskId = freshNext?.id ?: it.nextTaskId` fell back to the stale existing `nextTaskId` instead of setting `nextTaskId = null`. This prevented `FocusScreenV2` from showing the "Return to Flow" dialog option and instead repeatedly looped the user into a stale task.
- **Root Cause**:
  Missing future-day calendar bounds check in `findNextActionableTask()` and fallback to stale `it.nextTaskId` when `freshNext` is null.
- **Suggested / Applied Fix**:
  1. Added calendar day boundary filter (`isScheduledInFuture = (scheduledStart != null && scheduledStart > dayEnd) || (sDate != null && sDate > dayEnd)`) to exclude future-scheduled tasks from `findNextActionableTask()`.
  2. Updated sorting comparator to order today's timed tasks first by resolved epoch timestamp, followed by today's unscheduled tasks by priority.
  3. In `completeTask()` and `observeActiveSession()`, set `nextTaskId = freshNext?.id`, cleanly setting `nextTaskId = null` when the actionable queue is exhausted.
  4. Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-412: Sprint Completion Telemetry (`sessionCount`, `lastSessionDurationMinutes`) Dropped Across Decompression Rest and Expired Recovery in `FocusViewModelV2.kt`
- **Severity**: Medium
- **Category**: Data Integrity & Telemetry / Focus Metrics & Decompression Rest Retention
- **File**: [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L140-L680)
- **Problem Description**:
  1. When a focus sprint timer completed naturally in `tickSecond()` or was recovered from an expired background session in `loadTask()`, `sessionCount` and `lastSessionDurationMinutes` were not recorded on the active `TaskSlot`.
  2. When the user entered decompression rest (`takeDecompressionRest()`), `elapsedSeconds` and `startedAtMillis` were cleared to 0. When the task was subsequently marked complete from the decompression zone, `hasRunTimer` evaluated to `false`, causing the task slot to be saved with `sessionCount = 0` and `lastSessionDurationMinutes = 0f`, dropping all logged focus telemetry and corrupting session analytics.
- **Root Cause**:
  Missing session count / duration assignment on timer expiration and relying on reset `elapsedSeconds` during decompression completion.
- **Suggested / Applied Fix**:
  1. Updated `tickSecond()` and `loadTask()` (for recovered expired sessions) to increment `sessionCount = sessionCount + 1` and record `lastSessionDurationMinutes = actualMinutes.toFloat()`.
  2. In `completeTask()`, guarded session count calculation with `val hasUnfinishedSession = hasRunTimer && !wasSprintCompleted`, preserving existing session counts and durations logged during the sprint when completing from decompression rest.
  3. Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-413: Incomplete Micro-Step Badge Displayed on Completed Tasks in `HeroTaskCard` and Missing Widget Refresh on Expired Background Timer in `FocusService.kt`
- **Severity**: Medium
- **Category**: UI & Background Services / Widget Synchronization & Micro-Step Badge
- **File**: [`FocusScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt#L236), [`FocusService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L112-L255)
- **Problem Description**:
  1. In `FocusScreenV2.kt`, `HeroTaskCard` computed `nextStepForHero` based only on whether the timer was running (`if (state.isRunning && !state.isPaused) null else ...`). When a task with incomplete micro-steps was marked completed, `nextStepForHero` continued to display the "Next step: ..." chip on the completed task card, visually contradicting the task's completed state.
  2. In `FocusService.kt`, when a focus sprint expired while running in the background (`isExpired == true`), `FocusWidgetUpdateWorker.enqueueImmediateUpdate(applicationContext)` was not triggered, leaving home screen widgets displaying stale countdown timers until the app was reopened.
- **Root Cause**:
  Omission of `state.isCompleted` in `nextStepForHero` guard and missing widget refresh enqueue on background timer expiration.
- **Suggested / Applied Fix**:
  1. Updated `FocusScreenV2.kt` line 236 to guard `nextStepForHero = if (state.isCompleted || (state.isRunning && !state.isPaused)) null else ...`.
  2. Added `FocusWidgetUpdateWorker.enqueueImmediateUpdate(applicationContext)` to `FocusService.startTicker()` on sprint timer expiration and in `stopForegroundAndSelf()` on `ACTION_STOP`.
  3. Verified in [`FocusScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-414: Multi-Part Continuation Final Part Selection Determinism and Alphanumeric Inversion in `DependencyGraphValidator.kt`
- **Severity**: High
- **Category**: Core Logic & Scheduling / Multi-Part Continuation Chaining
- **File**: [`DependencyGraphValidator.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/DependencyGraphValidator.kt#L33-L43)
- **Problem Description**:
  When a multi-session task is partitioned (e.g. via `splitTaskIntoSessions` in `TaskRepository.kt`), parts are allocated random UUIDs and titled `(Part 1/3)`, `(Part 2/3)`, etc. `DependencyGraphValidator.sanitizeDag()` resolved the final part of a parent task using `parts.maxByOrNull { it.id }!!`. Because UUIDs are random hashes, string comparison selected an arbitrary part (e.g. Part 1 if its UUID started with `'z'`) instead of the true final part (Part 3). Furthermore, for alphanumeric IDs, `"part2" > "part10"`, causing multi-part tasks with >= 10 parts to pick Part 2 as the final part. Downstream tasks chained to the parent task thus get chained to an intermediate part or Part 1, allowing them to be scheduled and executed before subsequent continuation parts are completed.
- **Root Cause**:
  Relying on lexical string `it.id` comparison to determine the final continuation part rather than parsing numeric part indices from titles or IDs.
- **Suggested / Applied Fix**:
  1. Added `extractContinuationPartIndex(task: SchedulableTask): Int` to extract the numeric part index from either title (`\((Part|part) (\d+)/(\d+)\)`) or ID (`[#_]part(\d+)`).
  2. Updated `continuationPartMap` resolution to sort by numeric part index (`parts.maxWithOrNull(compareBy<SchedulableTask> { extractContinuationPartIndex(it) }.thenBy { it.id })!!`).
  3. Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-415: Continuation Part Prerequisite Omission for Multi-Part Tasks (`#part2`, `#part3`, `_part3`) in `SchedulePreparationOrchestrator.kt`
- **Severity**: High
- **Category**: Core Logic & Scheduling / Split Continuation Prerequisite Inference
- **File**: [`SchedulePreparationOrchestrator.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt#L149-L163)
- **Problem Description**:
  In `SchedulePreparationOrchestrator.kt`, parent prerequisite inference for split continuation tasks only checked `if (task.id.endsWith("_part2"))`. Tasks with `#part2`, `#part3`, `_part3`, etc. that did not have `parentTaskId` explicitly pre-populated (e.g. from dynamic splits or legacy entries) were not linked to their parent task or earlier parts in `parsedPrereqs`. Consequently, `task#part2` had an empty prerequisite set and in-degree 0 in `TaskSchedulerService`, allowing Part 2 to be scheduled earlier in the day than Part 1.
- **Root Cause**:
  Narrow hardcoded check for `_part2` omitting `#part2` and multi-part `part3+` continuation identifiers.
- **Suggested / Applied Fix**:
  1. Expanded continuation prerequisite inference to check `task.id.contains(Regex("""[#_]part([2-9]|\d{2,})"""))`.
  2. Extracted the base parent (`task.id.replace(Regex("""[#_]part\d+.*$"""), "")`) and added the parent and any preceding candidate part 1 (`#part1` or `_part1`) to `parsedPrereqs`.
  3. Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-416: Cross-Day Parent Prerequisite Deadlock and Hysteresis Rejection for Split Continuation Tasks in `TaskSchedulerService.kt`
- **Severity**: High
- **Category**: Core Logic & Scheduling / Continuation Parent Prerequisite Deadlock
- **File**: [`TaskSchedulerService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L506-L675)
- **Problem Description**:
  1. When a task is split into parts (`task_a#part1`, `task_a#part2`), `task_a` is active in `allKnownTasks`, so `activeUncompletedTaskIds` contains `"task_a"`.
  2. Because `"task_a"` was split into parts, `"task_a"` itself is not in `sanitizedTasks` or `remainingTasks`.
  3. In the hysteresis loop, `task_a#part2` checked `allPrereqs` which contains `"task_a"`. Since `"task_a" !in sanitizedTaskMap`, it executed `prereqId !in activeUncompletedTaskIds`, which failed (`"task_a"` is active), falsely rejecting `task_a#part2` from hysteresis placement even if `task_a#part1` was placed earlier.
  4. In `schedulableTodayTasks` cross-day prerequisite filter (line 661), `task_a#part2` checked if `"task_a"` is in `activeUncompletedTaskIds && prereqId !in remainingIds && prereqId !in completionEndMap`. Because Part 1 is Part 1 (not Part 2), `completionEndMap["task_a"]` was never set. So `task_a#part2` was deferred as `"Prerequisite task 'task_a' is active and pending on a future date."` and permanently excluded from today's schedule!
  5. In the unplaced sweep (lines 1120-1140), `isPlacedPrereq` only checked `it.task.id.contains(Regex("""[#_]part2"""))`, failing to detect placed Part 3+ continuation tasks.
- **Root Cause**:
  Treating a task's own active parent ID as an external cross-day prerequisite when the parent's split parts are scheduled in today's active session.
- **Suggested / Applied Fix**:
  1. In hysteresis placement (lines 506-515), if `prereqId` is the task's parent (`prereqId == task.parentTaskId || prereqId == cleanSplitTaskId(task.id)`), check if any prior part of the parent was anchored in `completionEndMap` before `existingWindow.startMillis`.
  2. In `schedulableTodayTasks` cross-day prerequisite check (line 661), exempt `prereqId` if it is the task's parent and any part of the parent is present in today's `sanitizedTasks` or `placements`.
  3. In the unplaced task sweep (lines 1120-1140), expand `isPlacedPrereq` to recognize all continuation parts (`[#_]part[2-9]` and `\((Part|part) [2-9]/\d+\)`).
  4. Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-417: Part 1 Tasks Erroneously Classified as Part 2 Continuations and Title Cleaning Defeat in `SchedulePreparationOrchestrator.kt`
- **Severity**: Medium
- **Category**: Core Logic & Scheduling / Part 1 Continuation Isolation
- **File**: [`SchedulePreparationOrchestrator.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt#L127-L187)
- **Problem Description**:
  1. In `SchedulePreparationOrchestrator.kt`, `isPart2Continuation` was defined as `task.parentTaskId != null` unconditionally. Tasks created by `splitTaskIntoSessions` have `parentTaskId` set on ALL sessions including Part 1 (`sessionTask.parentTaskId = task.id`). This caused Part 1 to be treated as a Part 2+ continuation (`isPart2Continuation = true`), completely defeating the negative lookahead `(?!1/)` in `\(Part (?!1/)\d+/\d+\)`.
  2. In line 187, `isCurrentlyActive` checked `task.id.startsWith("$activeTaskId#")` but omitted `task.id.startsWith("${activeTaskId}_")` and `task.parentTaskId == activeTaskId`, causing active split tasks using `_part` or UUIDs to not be flagged as currently active.
- **Root Cause**:
  Unconditional `task.parentTaskId != null` in `isPart2Continuation` overriding Part 1 isolation, and incomplete ID prefix matching in `isCurrentlyActive`.
- **Suggested / Applied Fix**:
  1. Guarded `task.parentTaskId != null` in `isPart2Continuation` with `!isPart1`.
  2. Updated `isCurrentlyActive` to support `_part` and `task.parentTaskId == activeTaskId`.
  3. Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-418: `TaskInferenceEngine.kt` Due Time Detection Failure for Intraday Deadlines (`due at 5pm`, `deadline at 5pm`, `by 5pm`, `due this afternoon`) Forcing Conversion to Scheduled Start Times and Dangling Title Keywords
- **Severity**: High
- **Category**: UI & NLP Inference / Intraday Due Time Misclassification
- **File**: [`TaskInferenceEngine.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/inference/TaskInferenceEngine.kt#L81-L380)
- **Problem Description**:
  When users enter a deadline time without an explicit day-of-week token (e.g. `"Submit report due at 5pm"`, `"Pay rent deadline at 5pm"`, `"due this afternoon"`, or `"by 5pm"`), `isDueTime` failed because `inferredDeadlineDate != null` was required (which only `DUE_DATE_REGEX` set). Because `DUE_DATE_REGEX` only matches day tokens (`today`, `tomorrow`, `monday`, etc.), `inferredDeadlineDate` remained null. As a result:
  1. The due time was erroneously assigned to `inferredScheduledTimeMillis` instead of `inferredDeadlineTimeMillis`.
  2. `inferredScheduledDate` was defaulted to `todayStartMillis`, scheduling the task to start at 5:00 PM with no deadline.
  3. `CLOCK_TIME_REGEX` only matched `\bat\s+`, completely missing `\bby\s+` (e.g. `"Submit tax forms by 5pm"`).
  4. Title sanitization removed `"at 5pm"` but left dangling `"due"` and `"deadline"` words in the task title (`"Submit report due"`).
- **Root Cause**:
  Requiring `inferredDeadlineDate != null` before recognizing `due at` / `deadline at` keywords, narrow `\bat\s+` regex, and missing cleanup for dangling due/deadline words.
- **Suggested / Applied Fix**:
  1. Expanded `CLOCK_TIME_REGEX` to match `\b(?:at|by|due(?:\s+at|\s+by)?|deadline(?:\s+at|\s+by)?)\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\b`.
  2. Updated `isDueTime` for clock time and time-of-day to check if the prefix begins with `"by"`, `"due"`, or `"deadline"`, or if due keywords are present without conflicting `scheduleMatch`.
  3. When `isDueTime` is true and `inferredDeadlineDate == null`, default `inferredDeadlineDate` to `todayStartMillis` while setting `isDeadlineDateExplicit = false`.
  4. Added `DANGLING_DUE_REGEX` to title sanitization to strip leftover `"due"` or `"deadline"` tokens.
  5. Verified in [`TaskInferenceEngineTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/inference/TaskInferenceEngineTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-419: Implicit Clock Time Scheduled Date Default Overriding Explicit `initialDate` and `existingTask.scheduledDate` in `SmartTaskComposerSheetV2.kt`
- **Severity**: High
- **Category**: UI & State Synchronization / Scheduled Date Shadowing
- **File**: [`SmartTaskComposerSheetV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt#L174-L545), [`TaskInferenceEngine.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/inference/TaskInferenceEngine.kt#L38-L385)
- **Problem Description**:
  When `TaskInferenceEngine` parsed clock time or time-of-day without an explicit date (e.g. `"Meeting at 3pm"`), it defaulted `inferredScheduledDate = todayStartMillis`. In `SmartTaskComposerSheetV2.kt`, `effectiveScheduledDate` selected `inferred.scheduledDate != null -> inferred.scheduledDate`, ignoring `initialDate` in Create mode (e.g. creating a task while viewing tomorrow or Friday on PlanScreen) and overwriting `existingTask.scheduledDate` in Edit mode with today. Furthermore, `onValueChange` cleared user manual date overrides whenever `newInferred.scheduledDate != null`.
- **Root Cause**:
  Lack of distinction between explicit date tokens (`"tomorrow"`, `"on Monday"`) and implicit fallback dates injected due to clock time parsing.
- **Suggested / Applied Fix**:
  1. Added `isScheduledDateExplicit` and `isDeadlineDateExplicit` to `InferredTaskMetadata`.
  2. In `SmartTaskComposerSheetV2.kt`, updated `effectiveScheduledDate` to prioritize `inferred.scheduledDate` only when `inferred.isScheduledDateExplicit` is true, otherwise respecting `initialDate` in Create mode or `existingTask.scheduledDate` in Edit mode.
  3. Guarded date override resets in `onValueChange` with `newInferred.isScheduledDateExplicit` and `newInferred.isDeadlineDateExplicit`.
  4. Verified in [`SmartTaskComposerSheetV2Test.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2Test.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-420: Calendar Sync Focus Block Anchored to 9:00 AM Default for Deadline-Only Tasks in `CalendarSyncDataSource.kt`, Causing Focus Block to Span Past Deadline
- **Severity**: Medium
- **Category**: Integration / Calendar Synchronization Deadline Alignment
- **File**: [`CalendarSyncDataSource.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt#L325-L395)
- **Problem Description**:
  1. In `CalendarSyncDataSource.upsertTaskEvent`, when a Daily Frog or schedule locked task had an explicit deadline time (`task.deadlineTime != null`) but no scheduled time (`task.scheduledTime == null`), `effectiveStart` defaulted to `startOfLocalDay(effectiveDate) + 9 * 3600_000L` (9:00 AM). If the task's deadline was in the morning (e.g. 8:30 AM or 9:30 AM), the focus block event was scheduled from 9:00 AM onwards, starting or finishing AFTER the task's deadline!
  2. If `contentResolver.update` returned 0 (e.g. event was deleted in the calendar provider), `upsertTaskEvent` silently returned `existingEventId` without recreating or inserting the calendar event.
- **Root Cause**:
  Ignoring `task.deadlineTime` when calculating calendar block start times, and omitting return value verification on provider update calls.
- **Suggested / Applied Fix**:
  1. Updated `CalendarSyncDataSource.kt` line 332 to check `else if (task.deadlineTime != null)` and anchor the focus block immediately prior to the deadline: `val deadlineEpoch = task.resolveDeadlineEpochMillis(anchor); if (deadlineEpoch != null) deadlineEpoch - durationMs else ...`.
  2. Verified `updatedRows > 0` on `contentResolver.update`, falling back to `findPrimaryWritableCalendarId()` and insert when `updatedRows == 0`.
  3. Verified in [`CalendarWriteBackSyncTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/calendar/CalendarWriteBackSyncTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-421: Space-Separated Multi-Hashtag Input (`#deepwork #coding`) Ingested as Single Malformed Catalog Tag in `TaskTagViewModel.kt` and `UserPreferencesDataStore.kt`
- **Severity**: Low
- **Category**: Core Data Integrity / Tag Normalization
- **File**: [`TaskTagViewModel.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/common/TaskTagViewModel.kt#L47), [`UserPreferencesDataStore.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/UserPreferencesDataStore.kt#L890)
- **Problem Description**:
  `normalizeTags` in `TaskTagViewModel.kt` and `mergeTagCatalog` in `UserPreferencesDataStore.kt` only split incoming strings on commas (`,`). When users entered space-separated hashtags (e.g. `"#deepwork #coding"` in the Settings tag input or during task seeding), the input was not split, creating a corrupted tag entry `"deepwork #coding"` with an embedded `#` in the catalog.
- **Root Cause**:
  Naive comma-only string splitting omitting `#` and `@` token boundaries.
- **Suggested / Applied Fix**:
  1. Updated splitting logic to `it.split(Regex("""(?=[#@])|[,;]"""))` in both `TaskTagViewModel.kt` and `UserPreferencesDataStore.kt`.
  2. Verified that space-separated hashtags are split into individual clean tokens while preserving multi-word phrases (e.g. `"deep work"`).
  3. Verified in [`TaskTagAndCategoryTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/common/TaskTagAndCategoryTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-422: `ScheduleAutoTasksWorker.applyPlacementsTransactionally` Groups Placements By Stripping Suffixes, Corrupting Part 2 / Continuation Task Placements
- **Severity**: High
- **Category**: Core Logic & Background Services / Continuation Task Scheduling & Persistence
- **File**: [`ScheduleAutoTasksWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L348-L385)
- **Problem Description**:
  In `ScheduleAutoTasksWorker.applyPlacementsTransactionally`, placements were grouped using `placements.groupBy { it.task.id.replace(Regex("""[#_]part\d+.*$"""), "") }`.
  When an existing continuation part entity (e.g. `task1_part2`) was scheduled independently (e.g. after parent `task1` was already completed earlier in the day), the placement's `task.id` was `"task1_part2"`.
  The regex stripped `_part2`, reducing `baseTaskId` to `"task1"`.
  `taskRepository.getById("task1")` loaded the parent task `task1`, which had `status == TaskStatus.COMPLETED`.
  Because `freshTask.status != TaskStatus.ACTIVE`, line 355 executed `continue`, silently dropping the placement!
  As a result, `task1_part2` never received its scheduled time in Room and remained permanently unscheduled in the backlog.
- **Root Cause**:
  Indiscriminately stripping all `[#_]part` suffixes when grouping placements instead of only grouping temporary in-flight split parts created by `TaskSchedulerService` (which specifically use `#part1` and `#part2`).
- **Suggested / Applied Fix**:
  1. Group placements by `if (placement.task.id.contains("#part")) placement.task.id.substringBefore("#part") else placement.task.id`.
  2. Guard continuation tasks so that when `primaryPlacement.task.id` is not an in-flight split (`!primaryPlacement.task.id.endsWith("#part1")`), it is treated as an independent placement and only evaluates/cleans up child `existingPart2` if `freshTask` is an actual base task (not already a continuation task).
  3. Verified in [`ScheduleAutoTasksWorkerTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/worker/ScheduleAutoTasksWorkerTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-423: Continuation Part (`_part3`, `#part3`) and Preceding Part Dependency Blocking Missing in `ScheduleAutoTasksWorker`, `NotificationWorker`, and `FocusWidgetUpdateWorker`
- **Severity**: High
- **Category**: Background Services & Logic / Multi-Part Continuation & Prerequisite Blocking
- **File**: [`ScheduleAutoTasksWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt#L184-L189), [`NotificationWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L182-L185), [`FocusWidgetUpdateWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt#L268-L271)
- **Problem Description**:
  All three background workers defined `isPart2Blocked` with hardcoded checks for `_part2` or `#part2`.
  1. This failed to identify multi-part continuation splits with `#part3`, `_part3`, or `(Part 3/3)` if `parentTaskId == null`.
  2. Even when `parentTaskId != null`, none of these workers checked whether preceding parts (e.g. Part 1 or Part 2) were still active.
  Consequently, `FocusWidgetUpdateWorker` and `DailyPlanWorker` could select `Part 3` as the next recommended action while `Part 2` was still active, and `ScheduleAutoTasksWorker` could schedule `Part 3` concurrently before `Part 2` was completed.
- **Root Cause**:
  Hardcoded `_part2` checks that omitted the full regex `[#_]part(\d+)` and preceding part index checks (`isPrecedingPartActive`).
- **Suggested / Applied Fix**:
  1. Standardized continuation blocking logic across `ScheduleAutoTasksWorker.kt`, `NotificationWorker.kt`, and `FocusWidgetUpdateWorker.kt` to inspect `Regex("""[#_]part(\d+)""")` and verify both parent active status and preceding parts `(1 until partNum).any { ... in activeTaskIds }`.
  2. Verified in [`ScheduleAutoTasksWorkerTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/worker/ScheduleAutoTasksWorkerTest.kt) and [`NotificationWorkerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/worker/NotificationWorkerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-424: Trailing Commas and Empty Tokens in `dependsOnTaskIds` Permanently Block Tasks in `DailyPlanWorker`, and SchedulableTask Prerequisite IDs Omitted
- **Severity**: Medium
- **Category**: Background Services & Notifications / Daily Plan Dependency Parsing
- **File**: [`NotificationWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L180-L181), [`NotificationWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt#L224-L248)
- **Problem Description**:
  In `DailyPlanWorker`:
  `task.dependsOnTaskIds.split(",").map { it.trim() }.all { it in completedIds }`
  omitted `.filter { it.isNotBlank() }`. If `dependsOnTaskIds` had a trailing comma (e.g. `"task_1,"`), `split` yielded `""`, which was never in `completedIds`, permanently blocking the task from ever appearing in the daily plan.
  Additionally, `SchedulableTask` in `DailyPlanWorker` omitted `prerequisiteIds`, distorting task scoring.
- **Root Cause**:
  Missing `.filter { it.isNotBlank() }` on comma splitting, and omitting `prerequisiteIds` in `SchedulableTask` mapping.
- **Suggested / Applied Fix**:
  1. Added `.filter { it.isNotBlank() }` to dependency splitting in `DailyPlanWorker`.
  2. Mapped `prerequisiteIds` in `SchedulableTask` construction with `parentTaskId` inclusion.
  3. Formatted start vs. deadline reminders in `TaskReminderWorker` using `resolveNotificationTitle` to clearly distinguish "starts in 15 minutes" from "due in 15 minutes".
  4. Verified in [`NotificationWorkerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/worker/NotificationWorkerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-425: `AutonomyNudgeWorker` Fires for Blocked Tasks and Inactive Continuation Parts
- **Severity**: Medium
- **Category**: Background Services & Autonomy Engine / Blocked Task Nudge Suppression
- **File**: [`AutonomyNudgeWorker.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/worker/AutonomyNudgeWorker.kt#L40-L65)
- **Problem Description**:
  `AutonomyNudgeWorker` checked `task.status == TaskStatus.ACTIVE` and `task.quadrant != Quadrant.ELIMINATE`, but completely omitted checking whether the task was blocked by incomplete prerequisites (`task.dependsOnTaskIds`), waiting for an external condition (`task.waitingFor`), or was an un-actionable continuation part (`#part2`, `#part3`) whose parent or preceding part was still active.
  As a result, users received "Ready to tackle [Task]?" notifications for tasks they could not currently start.
- **Root Cause**:
  Missing readiness guards (`waitingFor`, `isContinuationBlocked`, active prerequisite check) before posting autonomy nudge notifications.
- **Suggested / Applied Fix**:
  1. In `AutonomyNudgeWorker.doWork()`, added checks for `task.waitingFor.isBlank()`, verified dependencies are completed (`task.dependsOnTaskIds`), and ensured no preceding continuation part is active before displaying the nudge.
  2. Verified in [`AutonomyNudgeWorkerTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/worker/AutonomyNudgeWorkerTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-426: `CompleteTaskWidgetAction` and `FocusService` Fail to Cancel Active Notifications, Reminders, and Nudges on Completion
- **Severity**: Medium
- **Category**: Background Services & Notifications / Task Completion Notification Cleanup
- **File**: [`CompleteTaskWidgetAction.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/widget/CompleteTaskWidgetAction.kt#L73-L103), [`FocusService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt#L480-L482)
- **Problem Description**:
  When a task was completed via the Home Screen 1-tap Widget button or via the `FocusService` notification "Complete" action:
  1. `CompleteTaskWidgetAction.kt` failed to call `AutonomyNudgeEngine.cancelNudge(context, taskId)` and `TaskReminderWorker.cancelReminder(context, taskId)`.
  2. `FocusService.kt` called `AutonomyNudgeEngine.cancelNudge`, but failed to call `TaskReminderWorker.cancelReminder`.
  3. Neither `CompleteTaskWidgetAction.kt` nor `FocusService.kt` cancelled active notifications in the system notification shade for `taskId` (`NotificationManager.cancel(taskId.hashCode())`), leaving stale reminder and nudge notifications stranded in the user's notification drawer after the task was completed.
- **Root Cause**:
  Incomplete teardown and notification cancellation in background widget and service task completion paths.
- **Suggested / Applied Fix**:
  1. In `CompleteTaskWidgetAction.execute`, invoked `AutonomyNudgeEngine.cancelNudge`, `TaskReminderWorker.cancelReminder`, and `NotificationManager.cancel(taskId.hashCode())`.
  2. In `FocusService.handleCompleteAndStop`, added `TaskReminderWorker.cancelReminder` and `NotificationManager.cancel(targetTaskId.hashCode())`.
  3. Verified across test suite.
- **Status**: Resolved (verified in unit tests).

---

#### BUG-427: `completeAndRecur` Only Suppresses Recurrence on Part 2, Allowing Multi-Part (`#part3+`) Continuations to Advance Recurring Habit Chains
- **Severity**: High
- **Category**: Core Data Integrity & Recurrence / Multi-Part Habit Recurrence Isolation
- **File**: [`TaskRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L573-L578)
- **Problem Description**:
  `TaskRepository.completeAndRecur` guarded against premature recurrence advancement with:
  ```kotlin
  if (task.id.endsWith("_part2") || task.id.contains("#part2") || task.title.contains("(Part 2/") || task.parentTaskId != null) return null
  ```
  This check only guarded against Part 2. When tasks were split into 3 or more parts (e.g., `#part3`, `_part3`, `(Part 3/3)`), or when a multi-part continuation lacked `parentTaskId`, completing Part 3 or subsequent parts evaluated to false. The method then proceeded to advance `habitStreak`, compute `nextRecurringAnchorAfter`, and insert a premature or duplicate recurring instance into Room.
- **Root Cause**:
  Hardcoded Part 2 checks instead of regex pattern matching across all multi-part continuation suffixes (`[#_]part[2-9]\d*` and `(Part [2-9]\d*/\d+)`).
- **Suggested / Applied Fix**:
  1. Updated recurrence advancement guard to:
     ```kotlin
     if (task.id.contains(Regex("""[#_]part[2-9]\d*""")) ||
         task.title.contains(Regex("""\((Part|part) [2-9]\d*/\d+\)""")) ||
         task.parentTaskId != null
     ) return null
     ```
  2. Verified in [`RoomDatabaseAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/local/RoomDatabaseAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-428: `TaskRepository.delete` Misses Multi-Part (`#part3+`) Continuations, Causing Database Zombie Tasks, Stranded WorkManager Reminders, Leaked Sessions, and Downstream Dependency Deadlocks
- **Severity**: High
- **Category**: Core Data Integrity / Dynamic Multi-Part Cascade Deletion & Dependency Cleanup
- **File**: [`TaskRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L283-L325), [`TaskDao.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/TaskDao.kt#L96-L109)
- **Problem Description**:
  `TaskRepository.delete()` only explicitly searched for `_part2`, `#part2`, `#part1`, and `_part1`. If a task had multi-part continuations beyond Part 2 (`#part3`, `#part4`, `_part3`, etc.), those parts:
  1. Were not deleted from SQLite.
  2. Had their reminders and autonomy nudges left running in `WorkManager`.
  3. Leaked open sessions in `focus_sessions` and `time_sessions`.
  4. Were never stripped from downstream tasks' `dependsOnTaskIds`, permanently deadlocking downstream tasks that depended on `#part3`.
  In addition, `focusSessionDao` was omitted from `TaskRepository` injection and was not cleaned up during task deletion.
- **Root Cause**:
  Static suffix lookup for parts 1 and 2 rather than dynamic prefix querying (`parentTaskId = :parentId OR id LIKE :parentId || '#part%' OR id LIKE :parentId || '_part%'`), and missing `focusSessionDao` dependency in `TaskRepository`.
- **Suggested / Applied Fix**:
  1. Added `getContinuationPartsAndSubtasks` and `deleteContinuationPartsAndSubtasks` in [`TaskDao.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/TaskDao.kt).
  2. Injected `FocusSessionDao` into [`TaskRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt) and purged open sessions in both tables.
  3. Collected all related part IDs into `deletedIds` and cleaned up downstream dependencies in `TaskRepository.delete`.
  4. Verified in [`RoomDatabaseAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/local/RoomDatabaseAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-429: `TaskDao.observeTasksForDate` Unconditionally Matches `habitDate` for Recurring Tasks When Explicitly Rescheduled to Other Days, Duplicating Tasks Across Multiple Days
- **Severity**: Medium
- **Category**: Room Database & Scheduling / Habit Date Temporal Isolation
- **File**: [`TaskDao.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/TaskDao.kt#L48-L66), [`TaskRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L265-L277)
- **Problem Description**:
  In `TaskDao.observeTasksForDate` and `getTasksForDate`:
  ```sql
  WHERE status = 'ACTIVE'
    AND (
          (scheduledDate >= :dayStart AND scheduledDate < :dayEnd)
          OR
          (recurrence != 'NONE' AND habitDate >= :dayStart AND habitDate < :dayEnd)
          OR ...
  ```
  If a recurring habit had `habitDate = today`, but the user rescheduled it to tomorrow (setting `scheduledDate = tomorrow`), the query matched tomorrow's range via `scheduledDate`, but ALSO matched today's range via `habitDate >= :dayStart AND habitDate < :dayEnd`. As a result, the same task appeared on both today's and tomorrow's timeline simultaneously. Furthermore, `TaskRepository.rescheduleToTomorrow` updated `scheduledDate` but failed to advance `habitDate`.
- **Root Cause**:
  Missing `(scheduledDate IS NULL OR scheduledDate <= 0)` guard on the `habitDate` query branch, and un-updated `habitDate` in `rescheduleToTomorrow`.
- **Suggested / Applied Fix**:
  1. Updated `TaskDao.observeTasksForDate` and `getTasksForDate` to require `(recurrence != 'NONE' AND (scheduledDate IS NULL OR scheduledDate <= 0) AND habitDate >= :dayStart AND habitDate < :dayEnd)`.
  2. Updated `TaskRepository.rescheduleToTomorrow` to set `habitDate = if (task.recurrence != Recurrence.NONE) tomorrowStart else task.habitDate`.
  3. Verified in [`RoomDatabaseAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/local/RoomDatabaseAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-430: `SessionRepository.syncFocusSession` Overwrites Real Recorded `energyAtStart` Back to Hardcoded Default 50 on Session Updates
- **Severity**: Medium
- **Category**: Core Data Integrity / Focus Session Energy Telemetry Preservation
- **File**: [`SessionRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/SessionRepository.kt#L91-L105)
- **Problem Description**:
  In `SessionRepository.syncFocusSession()`, when synchronizing a `time_sessions` record to `focus_sessions` during session updates (e.g. updating notes or closing a session):
  ```kotlin
  dao.insert(
      FocusSessionEntity(
          ...
          energyAtStart = 50,
          ...
      )
  )
  ```
  Because `FocusSessionDao.insert` uses `OnConflictStrategy.REPLACE`, any focus session that had previously recorded a user's actual energy level (e.g. 85%) had its `energyAtStart` overwritten back to the hardcoded default `50`.
- **Root Cause**:
  Unconditionally hardcoding `energyAtStart = 50` rather than checking for an existing record in `focusSessionDao.getById(session.id)`.
- **Suggested / Applied Fix**:
  1. In `SessionRepository.syncFocusSession`, queried `dao.getById(session.id)` and preserved `existing?.energyAtStart ?: 50`.
  2. Verified in [`RoomDatabaseAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/local/RoomDatabaseAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-431: `splitTaskIntoSessions` Omits Lazy Micro-Step Extraction From `ifThenPlan` and Fabricates Redundant Plan Copies on Later Continuation Parts
- **Severity**: Medium
- **Category**: Core Data Integrity & Task Splitting / Lazy Micro-Step Plan Propagation
- **File**: [`TaskRepository.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt#L394-L445)
- **Problem Description**:
  In `TaskRepository.splitTaskIntoSessions`:
  1. `taskMicroStepDao.getByTaskId(task.id)` was queried directly. If micro-steps were defined in `task.ifThenPlan` but not yet loaded into `task_micro_steps`, `getByTaskId` returned empty, and Part 1 received zero micro-steps.
  2. All created parts (`Part 1`, `Part 2`, `Part 3`, etc.) received `ifThenPlan = task.ifThenPlan`, fabricating duplicate micro-step doorway plans across all continuation parts.
  3. When closing open sessions on the parent task, only `timeSessionDao` was updated, leaving zombie open sessions in `focus_sessions`.
- **Root Cause**:
  Omitting lazy fallback parsing via `getMicroSteps(task.id)` and duplicating `ifThenPlan` across non-first continuation parts.
- **Suggested / Applied Fix**:
  1. In `splitTaskIntoSessions`, fetched micro-steps using `getMicroSteps(task.id)` and cloned them to Part 1.
  2. Assigned `ifThenPlan = if (i == 1) task.ifThenPlan else ""` across session parts.
  3. Closed open focus sessions in both `timeSessionDao` and `focusSessionDao` with `wasCompleted = true`.
  4. Verified in [`RoomDatabaseAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/data/local/RoomDatabaseAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-432: `MainActivity.resolveDefaultFocusTask` Ingests Non-Actionable `waitingFor` Tasks and Omits Preceding Part (`#part1`, `#part2`) Blocking for Multi-Part Continuations
- **Severity**: High
- **Category**: UI & Navigation / Default Focus Task Resolution & Continuation Integrity
- **File**: [`MainActivity.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L165-L215)
- **Problem Description**:
  In `MainActivity.resolveDefaultFocusTask()`:
  1. Candidate task filtering failed to check `it.waitingFor.isBlank()`. When the user launched focus via an intent or widget without a specific task, tasks marked as waiting on third-party responses (e.g. "Waiting for legal approval") were selected as active focus targets, blocking actionable tasks.
  2. Continuation detection used naive `it.id.contains(Regex("""[#_]part\d+"""))`, which erroneously matched `#part1` as a continuation and blocked Part 1 if the parent task was still in `activeIds`.
  3. For multi-part continuations (`#part3`), it only checked `parentSlug in activeIds`. Once the parent task was completed or closed, `parentSlug` was no longer in `activeIds`, allowing `#part3` to run out of order even while `#part1` and `#part2` were still active.
  4. Sorting ranked tasks by `priority.ordinal` ahead of earlier intraday scheduled times, allowing late-afternoon tasks to supersede morning scheduled tasks of identical priority/quadrant.
- **Root Cause**:
  Missing `waitingFor` filter, unconstrained continuation regex matching Part 1, absence of preceding-part active checks, and inverted chronological sort precedence.
- **Suggested / Applied Fix**:
  1. Filtered out tasks where `it.waitingFor.isNotBlank()`.
  2. Extracted `partNum` via `Regex("""[#_]part(\d+)""")` and restricted continuation identification to `partNum >= 2`, `endsWith("_part2")`, `contains("#part2")`, or `parentTaskId != null`.
  3. Enforced preceding-part blocking across `(1 until partNum)` to ensure Part 3 waits for Part 1 and Part 2.
  4. Added `it.resolveScheduledEpochMillis(now) ?: Long.MAX_VALUE` to sort earlier intraday scheduled tasks ahead of later tasks.
  5. Verified in [`NavigationAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/navigation/NavigationAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-433: Redundant Focus Destination Re-Creation in `NeuroFlowAppV2.kt` When Navigating to Already Active Focus Task Resets Session State
- **Severity**: Medium
- **Category**: UI & State Synchronization / Redundant Navigation & Focus State Reset
- **File**: [`NeuroFlowAppV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L91-L125)
- **Problem Description**:
  When an external notification or widget intent arrived for `initialTaskId` while the user was already focused on that exact task (`focus_v2/{taskId}`), `LaunchedEffect(initialTaskId)` detected `isAlreadyOnFocus = true` and unconditionally popped the current destination (`popUpTo(currentDestId) { inclusive = true }`) before navigating back to `focus_v2/$encodedId`. This destroyed the active `FocusViewModelV2` instance, wiping out in-progress timer countdowns, stopwatch elapsed durations, and local UI state (e.g. expanded micro-steps, parked thoughts).
- **Root Cause**:
  Missing check comparing incoming `initialTaskId` against `navBackStackEntry.arguments?.getString("taskId")`.
- **Suggested / Applied Fix**:
  1. Extracted `currentTaskId = navBackStackEntry?.arguments?.getString("taskId")`.
  2. Guarded navigation in `LaunchedEffect(initialTaskId)`: only pop and navigate when `!isAlreadyOnFocus || currentTaskId != initialTaskId`.
  3. Guarded navigation in `LaunchedEffect(initialWoopTaskId)`: only pop and navigate when `!isAlreadyOnFocus || currentTaskId != initialWoopTaskId || !currentOpenWoop`.
  4. Verified in [`NavigationAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/navigation/NavigationAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-434: Asynchronous WOOP Reflection Intent Mode (`ACTION_WOOP_REFLECT`) Lost Across Activity Recreation / Device Rotation in `MainActivity.kt`
- **Severity**: Medium
- **Category**: UI Lifecycle & State Persistence / Async Intent Resolution Restoration
- **File**: [`MainActivity.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L215-L260)
- **Problem Description**:
  When `ACTION_WOOP_REFLECT` was handled without a specified `taskId`, `MainActivity` executed `resolveDefaultFocusTask(openWoop = true)`. While the coroutine was querying Room, `onSaveInstanceState` saved `saved_resolving_focus_task = true`, but omitted whether `openWoop` was active (`isResolvingWoopTask`). Upon activity recreation, `onCreate` resumed resolution with the default `openWoop = false`, causing the app to resolve to a regular focus mode and dropping the WOOP reflection sheet requested by the user.
- **Root Cause**:
  Missing `isResolvingWoopTask` state tracking and omission of `saved_resolving_woop_task` in `onSaveInstanceState` / `onCreate`.
- **Suggested / Applied Fix**:
  1. Added `isResolvingWoopTask` state variable to `MainActivity`.
  2. Saved `saved_resolving_woop_task` in `onSaveInstanceState` and restored it in `onCreate`, calling `resolveDefaultFocusTask(openWoop = wasResolvingWoop)`.
  3. Verified in [`NavigationAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/navigation/NavigationAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-435: Alternate Intent Extra `extra_task_id` Ignored in `MainActivity.kt` Pending Action Verification and Intent Parsing
- **Severity**: Low
- **Category**: Integration & Intent Handling / Alternate Extra Key Compatibility
- **File**: [`MainActivity.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L80-L95, #L235)
- **Problem Description**:
  In `FocusWidgetProvider.kt` and `CompleteTaskReceiver.kt`, tasks are referenced using alternate extra keys including `extra_task_id` (`EXTRA_TASK_ID`). However, `MainActivity.handleIntent` only inspected `intent.getStringExtra("taskId") ?: intent.getStringExtra("task_id")` and `MainActivity.onCreate` only checked `intent.hasExtra("taskId") || intent.hasExtra("task_id")`. Any intent directing the user to a task via `extra_task_id` without an explicit custom action was ignored during cold start and intent dispatch.
- **Root Cause**:
  Missing `extra_task_id` lookup in `handleIntent`, `hasPendingAction`, and intent cleanup.
- **Suggested / Applied Fix**:
  1. In `MainActivity.handleIntent`, parsed `intent.getStringExtra("extra_task_id")` alongside `taskId` and `task_id`, and cleaned it up via `intent.removeExtra("extra_task_id")`.
  2. In `MainActivity.onCreate`, added `intent.hasExtra("extra_task_id")` to `hasPendingAction`.
  3. Verified in [`NavigationAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/navigation/NavigationAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-436: `PlanViewModelV2.autoScheduleDay` Uses Incomplete String Slicing When Deriving Parent ID for Multi-Part Continuations (`#part3+`) and Omits Preceding Active Part Blocking
- **Severity**: High
- **Category**: Core Logic & Auto-Scheduling / Multi-Part Continuation Integrity & Self-Blocking Prevention
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L525-L565)
- **Problem Description**:
  In `PlanViewModelV2.autoScheduleDay()`:
  1. When determining `candidateTasks` and `fixedWindows`, continuation detection and parent resolution used naive string slicing:
     `val parentId = task.parentTaskId ?: task.id.removeSuffix("_part2").substringBefore("#part2")`
     For any multi-part continuation numbered 3 or higher without explicit `parentTaskId` (e.g. `task-1#part3` or `task-1_part3`), `removeSuffix("_part2").substringBefore("#part2")` evaluated to `"task-1#part3"`. Because `"task-1#part3"` was in `activeTaskIds`, `parentId in activeTaskIds` evaluated to `true`, causing Part 3 to match itself as its own active parent and permanently blocking it from being auto-scheduled.
  2. For multi-part continuations where `parentTaskId` was set and the base parent was completed, `isParentActive` only checked if the base parent was active, ignoring preceding active parts (`#part1`, `#part2`). Consequently, Part 3 tasks were scheduled prematurely while preceding parts were still uncompleted.
  3. The exact same parent ID corruption and missing part blocking affected `fixedWindows` exclusion.
- **Root Cause**:
  Hardcoded `_part2`/`#part2` string slicing instead of regex-safe `replace(Regex("""[#_]part\d+.*$"""), "")` and omission of preceding active part checks.
- **Suggested / Applied Fix**:
  1. Introduced standard `isContinuationBlocked(task, activeIds)` in `autoScheduleDay` matching `FlowViewModelV2`, `ScheduleAutoTasksWorker`, and `MainActivity`.
  2. Extracted `partNum` via `Regex("""[#_]part(\d+)""")` / `(Part (\d+)/` and validated `parentSlug != task.id && parentSlug in activeIds`, `isPart1Active`, and `(1 until partNum).any { ... in activeIds }`.
  3. Applied regex-safe parent slug checking in `fixedWindows`.
  4. Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-437: `PlanScreenV2` Hardcodes `Part 1/2` Label for Multi-Part Continuation Tasks and Omits Inter-Part Ultradian Buffers on `TimelineScheduleView`
- **Severity**: Medium
- **Category**: UI & State Management / Multi-Part Continuation Labeling & Ultradian Recovery Dividers
- **File**: [`PlanScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt#L824-L857, #L1140-L1160)
- **Problem Description**:
  On `PlanScreenV2`:
  1. In `TimelineScheduleView`, the continuation badge label used hardcoded binary logic:
     `val partLabel = if (task.id.contains("#part2") || task.title.contains("(Part 2/2)")) "🔗 Part 2/2" else "🔗 Part 1/2"`
     Any task belonging to a 3-part or 4-part split task (e.g. `task#part3` or `(Part 3/3)`) evaluated the condition to false and rendered with an erroneous `"🔗 Part 1/2"` badge.
  2. In `shouldShowUltradianBufferAfter`, `isBetweenSplitParts` only checked `Part 1/2` and `Part 2/2`. Between consecutive parts of higher-degree split tasks (e.g. Part 2/3 and Part 3/3), the required 15-minute ultradian recovery buffer divider was omitted.
  3. `isBetweenSplitParts` was evaluated prior to checking `current.scheduledTime == null || next.scheduledTime == null`. If two untimed tasks were split parts, it returned true, erroneously inserting an ultradian divider among untimed tasks on the timeline.
  4. The running duration sum loop only inspected `tasks[i - 1].scheduledTime != null`. When an untimed task was interleaved, it failed to reset `runSum` across valid 15-minute rest gaps.
- **Root Cause**:
  Binary part checking and inverted untimed task guarding in `PlanScreenV2.kt`.
- **Suggested / Applied Fix**:
  1. Parsed dynamic part numbers using `Regex("""\(Part (\d+/\d+)\)""")` and `Regex("""[#_]part(\d+)""")` to format accurate badges (e.g. `"🔗 Part 3/3"`).
  2. Guarded `shouldShowUltradianBufferAfter` immediately with `if (current.scheduledTime == null || next.scheduledTime == null) return false`.
  3. Checked consecutive split parts dynamically via `nextPart == curPart + 1`.
  4. Tracked `lastTimedEndMinute` across the running duration sum loop.
  5. Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-438: `PlanViewModelV2.shiftTaskToPeak` Defers Tasks to Tomorrow When Circadian Peak Wraps Around Midnight Due to Strict `peakHour <= peakEndHour` Guard
- **Severity**: Medium
- **Category**: Core Logic & Circadian Alignment / Midnight-Wrapping Peak Window Calculation
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L1055-L1085)
- **Problem Description**:
  In `PlanViewModelV2.shiftTaskToPeak()`, when evaluating whether today's circadian peak window is upcoming for tasks on today's schedule:
  ```kotlin
  if (currentHour < peakHour && peakHour <= peakEndHour) {
      targetDate = todayStart
      targetHour = peakHour
      ...
  ```
  For an evening chronotype whose peak wraps around midnight (e.g. `peakHour = 22` and `peakEndHour = 1`), `peakHour <= peakEndHour` is `false`. At 19:00 (7 PM), `currentHour < peakHour` (19 < 22) is true, but the condition evaluated to `false`. After falling through subsequent checks, the method executed `targetDate = startOfNextLocalDay(todayStart)`, pushing the task to tomorrow's peak and skipping today's upcoming peak at 22:00.
- **Root Cause**:
  Missing handling for midnight-wrapping peak windows in the upcoming peak check.
- **Suggested / Applied Fix**:
  1. Formulated `isPeakUpcoming = if (peakHour <= peakEndHour) currentHour < peakHour else (currentHour in peakEndHour until peakHour)`.
  2. Guarded the branch with `if (isPeakUpcoming)`, correctly assigning `targetDate = todayStart` and `targetHour = peakHour` for wrapping windows.
  3. Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-439: `PlanViewModelV2.addTask` Drops `habitDate` on Recurring Tasks With Explicit `scheduledDate` and `uncompleteTask` Re-schedules Autonomy Nudge for Iceboxed Tasks
- **Severity**: Low
- **Category**: Core Data Integrity & Notifications / Habit Date Initialization & Icebox Nudge Guard
- **File**: [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L850-L865, #L1200-L1210)
- **Problem Description**:
  1. In `PlanViewModelV2.addTask()`, the first branch of the `when` binding was:
     `currentTask.scheduledDate != null -> currentTask`
     When a recurring task was created with an explicit `scheduledDate` but null `habitDate`, `habitDate` was never initialized. Consequently, `isRecurringWithAnchor()` returned `false`, breaking recurrence progression and anchor isolation.
  2. In `PlanViewModelV2.uncompleteTask()`, `AutonomyNudgeEngine.scheduleNudge(it, updated)` was executed unconditionally upon restoring a completed task. If the restored task was categorized in `Quadrant.ELIMINATE` (icebox backlog), an autonomy nudge notification was scheduled, violating the invariant that iceboxed tasks must never trigger proactive notifications.
- **Root Cause**:
  Missing `habitDate` assignment in `addTask` when `scheduledDate != null`, and omission of `updated.quadrant != Quadrant.ELIMINATE` in `uncompleteTask`.
- **Suggested / Applied Fix**:
  1. In `addTask`, set `habitDate = currentTask.scheduledDate` when `recurrence != NONE && habitDate == null`.
  2. In `uncompleteTask`, guarded nudge scheduling with `if (updated.quadrant != Quadrant.ELIMINATE && updated.status == TaskStatus.ACTIVE)`.
  3. Verified in [`PlanScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-440: Multi-Part Continuation Tasks Without `#part` in ID (UUIDs / Custom Slugs) Match Themselves in `isPart1Active` and `isPrecedingPartActive`, Permanently Self-Blocking From Actionable Pool and Spotlight
- **Severity**: High
- **Category**: Core Logic & Task Actionability / Continuation Self-Blocking Prevention
- **File**: [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L363-L380), [`FlowScreenV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt#L256-L265), [`PlanViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt#L540-L545), [`FocusViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt#L1232-L1237)
- **Problem Description**:
  When evaluating whether a continuation task is blocked:
  ```kotlin
  val isPart1Active = task.id.replace(Regex("""([#_]part)\d+"""), "$1" + "1") in activeIds
  val isPrecedingPartActive = partNum != null && (1 until partNum).any { prevPart ->
      task.id.replace(Regex("""([#_]part)\d+"""), "$1$prevPart") in activeIds
  }
  ```
  For continuation tasks created with arbitrary UUIDs or custom IDs (e.g. from `splitTaskIntoSessions`, task composer, or cloud calendar sync) where `task.id` does not contain `[#_]part\d+` but `task.parentTaskId` is set and the title contains `(Part 2/2)`:
  1. `task.id.replace(...)` failed to match and returned `task.id` unmodified.
  2. Because `task.id` is in `activeIds` (the task itself is currently active), `task.id in activeIds` evaluated to `true`.
  3. `isPart1Active` evaluated to `true` by checking the task against itself, even though its parent task was completed.
  4. Consequently, `isContinuationBlocked` returned `true`, permanently excluding the task from `actionableActiveTasks`, `todayActiveTasks`, recommendation eligibility, and `FlowSpotlightHeroCard`.
- **Root Cause**:
  Omission of `task.id.contains(Regex("""[#_]part\d+"""))` and `partId != task.id` checks before performing string substitution in `isPart1Active` and `isPrecedingPartActive`.
- **Suggested / Applied Fix**:
  1. Added `val hasPartRegex = task.id.contains(Regex("""[#_]part\d+"""))`.
  2. Guarded `isPart1Active` with `if (hasPartRegex) { val part1Id = task.id.replace(...); part1Id != task.id && part1Id in activeIds } else false`.
  3. Guarded `isPrecedingPartActive` with `if (hasPartRegex && partNum != null) { ... prevId != task.id && prevId in activeIds } else false`.
  4. Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-441: `FlowViewModelV2.shiftTaskToPeak` Defers Tasks to Tomorrow When Circadian Peak Wraps Around Midnight Due to Strict `peakHour <= peakEndHour` Guard
- **Severity**: Medium
- **Category**: Core Logic & Circadian Alignment / Midnight-Wrapping Peak Window Calculation
- **File**: [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L836-L856)
- **Problem Description**:
  In `FlowViewModelV2.shiftTaskToPeak()`, when evaluating upcoming circadian peaks:
  ```kotlin
  if (currentHour < peakHour && peakHour <= peakEndHour) {
      targetDate = todayStart
      targetHour = peakHour
      ...
  ```
  For an evening chronotype or manual peak anchor that wraps around midnight (e.g. `peakHour = 22` and `peakEndHour = 1`), `peakHour <= peakEndHour` is `false`. At 19:00 (7 PM), `currentHour < peakHour` (19 < 22) is true, but the condition evaluated to `false`. After falling through subsequent branches, the method defaulted to `targetDate = startOfNextLocalDay(todayStart)`, pushing the task to tomorrow's peak and skipping today's upcoming peak at 22:00.
- **Root Cause**:
  Missing handling for midnight-wrapping peak windows in the upcoming peak condition.
- **Suggested / Applied Fix**:
  1. Formulated `isPeakUpcoming = if (peakHour <= peakEndHour) currentHour < peakHour else (currentHour in peakEndHour until peakHour)`.
  2. Guarded the branch with `if (isPeakUpcoming)`, correctly assigning `targetDate = todayStart` and `targetHour = peakHour` for wrapping windows.
  3. Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-442: `FlowViewModelV2.addTask` Drops `habitDate` on Recurring Tasks With Explicit `scheduledDate` and `shiftTaskToPeak` / `breakDownTask` Omit Icebox Nudge Guard
- **Severity**: Low
- **Category**: Core Data Integrity & Notifications / Habit Date Anchoring & Icebox Nudge Guard
- **File**: [`FlowViewModelV2.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt#L606-L614, #L864-L868, #L1130-L1135)
- **Problem Description**:
  1. In `FlowViewModelV2.addTask()`, the second branch of the `when` binding was:
     `task.scheduledDate != null -> task`
     When a recurring task was created with an explicit `scheduledDate` but null `habitDate`, `habitDate` was never initialized. Consequently, `isRecurringWithAnchor()` returned `false`, breaking recurrence progression and anchor isolation.
  2. In `shiftTaskToPeak()` and `breakDownTask()`, `AutonomyNudgeEngine.scheduleNudge(it, updated)` was executed unconditionally without checking whether `updated.quadrant == Quadrant.ELIMINATE`. If an iceboxed backlog task was shifted to peak or decomposed into micro-steps, an autonomy nudge notification was scheduled, violating the invariant that iceboxed tasks must never trigger proactive notifications.
- **Root Cause**:
  Missing `habitDate` initialization in `addTask` when `scheduledDate != null`, and omission of `updated.quadrant != Quadrant.ELIMINATE` in `shiftTaskToPeak` and `breakDownTask`.
- **Suggested / Applied Fix**:
  1. In `addTask`, set `habitDate = task.scheduledDate` when `recurrence != NONE && habitDate == null`.
  2. In `shiftTaskToPeak` and `breakDownTask`, guarded nudge scheduling with `if (updated.quadrant != Quadrant.ELIMINATE && updated.status == TaskStatus.ACTIVE)`.
  3. Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-443: `EnergyTimelineStream.shouldShowUltradianBufferAfter` Emits 15m Ultradian Rest Buffer Between Untimed Split Parts Due to Inverted Untimed Task Guard
- **Severity**: Low
- **Category**: UI & Boundary Handling / Untimed Split Buffer Suppression
- **File**: [`EnergyTimelineStream.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnergyTimelineStream.kt#L676-L695)
- **Problem Description**:
  In `EnergyTimelineStream.shouldShowUltradianBufferAfter()`, `isBetweenConsecutiveSplitParts` was evaluated prior to checking `current.scheduledTime == null || next.scheduledTime == null`. If two untimed tasks on today's schedule were consecutive split parts (e.g. Part 1 and Part 2 without a scheduled time), `if (isBetweenConsecutiveSplitParts) return true` returned true, erroneously inserting an `UltradianRecoveryDivider` ("☕ 15m Ultradian Rest Buffer") in the middle of untimed tasks on the timeline stream.
- **Root Cause**:
  Untimed task check placed after split parts evaluation in `EnergyTimelineStream.kt`.
- **Suggested / Applied Fix**:
  1. Guarded `shouldShowUltradianBufferAfter` immediately with `if (current.scheduledTime == null || next.scheduledTime == null) return false`.
  2. Verified in [`FlowScreenV2AuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2AuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-444: Icebox Nudge Alarm Leak on Focus Screen Exit
- **Severity**: Medium
- **Category**: Alarm & Notification / Autonomy Engine
- **File**: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt` (lines 1010–1020)
- **Problem Description**:
  When a user exited `FocusScreenV2` without starting or completing a task, `onCleared()` invoked `AutonomyNudgeEngine.scheduleNudge(ctx, task)` for all tasks with `task.status == TaskStatus.ACTIVE`. However, if the task was in the Icebox (`Quadrant.ELIMINATE`), previewing it on the focus screen inadvertently scheduled autonomy nudge alarms, defeating the purpose of the icebox where non-actionable/eliminated tasks should remain dormant without proactive nudges.
- **Expected Behavior**:
  Autonomy nudges must only be rescheduled for active tasks that do not belong to `Quadrant.ELIMINATE`.
- **Recommended Fix**:
  ```kotlin
  if (task.status == TaskStatus.ACTIVE && task.quadrant != Quadrant.ELIMINATE) {
      AutonomyNudgeEngine.scheduleNudge(ctx, task)
  }
  ```

---

#### BUG-445: Missing Reminder Notification & WorkManager Cancellation on Focus Task Completion
- **Severity**: High
- **Category**: WorkManager / Notification Lifecycle
- **File**: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt` (lines 788–797)
- **Problem Description**:
  When a user completed a task via `completeTask()`, `AutonomyNudgeEngine.cancelNudge(ctx, taskId)` was called, but `TaskReminderWorker.cancelReminder(ctx, taskId)` and active status bar notifications posted for the task (`NotificationManager.cancel(taskId.hashCode())`) were not cancelled. As a result, reminder alarms scheduled for upcoming task deadlines or start times continued to fire for already completed tasks, and notifications posted to the notification shade remained stuck until manually dismissed.
- **Expected Behavior**:
  Completing a task in the focus view must immediately cancel its scheduled `TaskReminderWorker` WorkManager request and clear any active notifications in the system notification tray.
- **Recommended Fix**:
  ```kotlin
  context?.let { ctx ->
      runCatching { AutonomyNudgeEngine.cancelNudge(ctx, taskId) }
      runCatching { com.neuroflow.app.worker.TaskReminderWorker.cancelReminder(ctx, taskId) }
      runCatching {
          val nm = ctx.getSystemService(android.app.NotificationManager::class.java)
          nm?.cancel(taskId.hashCode())
      }
  }
  ```

---

#### BUG-446: UUID Multi-Part Continuation Tasks Prematurely Unblocked Ahead of Sequential Predecessors
- **Severity**: High
- **Category**: Scheduling / Dependency Resolution
- **File**: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt` (lines 1257–1270), `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt`, `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt`, `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt`
- **Problem Description**:
  In `findNextActionableTask()` and task dependency check algorithms across ViewModels, `isPrecedingPartActive` only checked for continuation tasks matching the regex `[#_]part\d+` within the task ID itself. For multi-part tasks decomposed or split with UUID IDs sharing a `parentTaskId` and labeled with `(Part X/Y)` in their title, `isPrecedingPartActive` returned `false`. Consequently, once Part 1 completed, Part 3 would become eligible and could be selected as the "next actionable task" while Part 2 was still active and incomplete, violating sequential workflow execution.
- **Expected Behavior**:
  Continuation task checking must evaluate both ID-based parts (`#partX` / `_partX`) and parentTaskId/title-based parts (`(Part X/Y)`). Any part `N` must remain blocked whenever an active sibling part `< N` sharing the same `parentTaskId` exists.
- **Recommended Fix**:
  ```kotlin
  val isPrecedingPartActive = if (hasPartRegex && partNum != null) {
      (1 until partNum).any { prevPart ->
          val prevId = task.id.replace(Regex("""([#_]part)\d+"""), "$1$prevPart")
          prevId != task.id && prevId in activeIds
      }
  } else if (task.parentTaskId != null && partNum != null && partNum > 1) {
      activeTasks.any { other ->
          other.id != task.id &&
              (other.parentTaskId == task.parentTaskId || other.id == task.parentTaskId) &&
              (Regex("""\(Part (\d+)/""").find(other.title)?.groupValues?.get(1)?.toIntOrNull() ?: 1) < partNum
      }
  } else false
  ```

---

#### BUG-447: FocusService ACTION_PAUSE Null Active Session Drop & Stale Remaining Seconds
- **Severity**: Medium
- **Category**: Background Service / Concurrency
- **File**: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt` (lines 140–165), `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt`
- **Problem Description**:
  In `FocusService.onStartCommand()`, handling `ACTION_PAUSE` executed `_activeSession.value = _activeSession.value?.copy(isPaused = true)`. If `_activeSession.value` was null (e.g. after a process restart or when the notification action was received before initial binding), the null check failed and `_activeSession` remained null, losing the session for external observers such as `MiniFocusSessionBar`. Furthermore, the remaining duration was never recalculated via `FocusTimingCalculator.calculate()`, leaving stale `remainingSeconds` values on pause.
- **Expected Behavior**:
  `ACTION_PAUSE` must construct a complete `ActiveFocusSession` with accurate elapsed/remaining seconds calculated from `FocusTimingCalculator.calculate()` and update `_activeSession.value` regardless of whether previous state was populated.
- **Recommended Fix**:
  ```kotlin
  val timing = FocusTimingCalculator.calculate(
      startedAtMillis = startedAtMillis,
      accumulatedPauseMillis = accumulatedPauseMillis,
      pausedAtMillis = pausedAtMillis,
      nowMillis = now,
      targetDurationSeconds = durationSeconds
  )
  val isStopwatch = durationSeconds <= 0L
  _activeSession.value = ActiveFocusSession(
      taskId = taskId,
      taskTitle = taskTitle,
      remainingSeconds = if (isStopwatch) timing.elapsedSeconds else timing.remainingSeconds,
      totalSeconds = durationSeconds,
      isPaused = true
  )
  ```

---

#### BUG-448: Trapped Completed State & Zombie WOOP Prompt on Completed Tasks
- **Severity**: Medium
- **Category**: UI / State Machine
- **File**: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt` (lines 170–185, 340–370)
- **Problem Description**:
  In `FocusScreenV2.kt`, a `LaunchedEffect(state.task, state.isWoopLoaded)` checked `if (task != null && !state.isWoopLoaded && !state.woopDismissed) { showWoopPromptSheet = true }` without checking `!state.isCompleted && task.status != TaskStatus.COMPLETED && !state.isSessionActive`. As a result, opening a completed task from history prompted a WOOP setup sheet. Additionally, when a user dismissed the completion celebration dialog, the UI displayed only a static "Task Completed" card with no navigation action to proceed to the next queued task (`state.nextTaskId`), stranding the user on a completed screen.
- **Expected Behavior**:
  Completed tasks must never trigger the WOOP prompt sheet. When the celebration dialog is closed, the completed state view should present a primary action button to "Start Next Task" whenever `state.nextTaskId != null`.
- **Recommended Fix**:
  Add status checks in the `LaunchedEffect` and render a `Button` to start `state.nextTaskId` in the completion surface.

---

#### BUG-449: `TaskSchedulerService.kt` Prematurely Updates `completionEndMap[parentId]` and Unlocks Downstream Dependents on Part 1 or Intermediate Continuation Parts
- **Severity**: High
- **Category**: Core Logic & Scheduling / Multi-Part Continuation Completion Synchronization
- **File**: [`TaskSchedulerService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L562-L572), [`TaskSchedulerService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L914-L974)
- **Problem Description**:
  In `TaskSchedulerService.scheduleDayWithOutcome()`, both hysteresis placement (line 562) and dynamic placement (line 901) defined `isSplitContinuation` as:
  ```kotlin
  val isSplitContinuation = task.parentTaskId != null ||
      task.id.endsWith("#part2") || task.id.endsWith("_part2") ||
      task.id.contains(Regex("""[#_]part[2-9]""")) ||
      task.title.contains(Regex("""\((Part|part) [2-9]/\d+\)"""))
  ```
  Because `task.parentTaskId != null` was checked unconditionally via `||`, Part 1 of split sessions generated by `TaskRepository.splitTaskIntoSessions` (which sets `sessionTask.parentTaskId = task.id` on all parts) and ordinary subtasks were evaluated as `isSplitContinuation = true`.
  When Part 1 was anchored or placed, `completionEndMap[parentId]` was immediately assigned Part 1's end time, and in line 948 `children[parentId]` (all downstream tasks waiting for the parent task to complete) were prematurely unlocked into Kahn's `readyQueue`. Consequently, downstream tasks were scheduled concurrently with or before Part 2 or Part 3 ran, breaking topological dependency ordering.
- **Root Cause**:
  Unconditional `task.parentTaskId != null` evaluating to true for Part 1 and subtasks, and failure to verify whether the placed part was the true final continuation part of the parent task.
- **Suggested / Applied Fix**:
  1. Guarded continuation parts with `!isPart1` and required either `#part2+` identifiers or explicit `(Part X/Y)` titles.
  2. Verified whether the placed task is the true final continuation sibling among `sanitizedTasks` using `DependencyGraphValidator.extractContinuationPartIndex()`.
  3. Only updated `completionEndMap[parentId]` and unlocked `children[parentId]` when `isFinalPart` is true.
  4. Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-450: `TaskSchedulerService.kt` Erroneously Blocks Multi-Part UUID Continuation Parts as Cross-Day Prerequisites and Omits Anchored Status in Hysteresis
- **Severity**: High
- **Category**: Core Logic & Scheduling / UUID Continuation Prerequisite Resolution
- **File**: [`TaskSchedulerService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L511-L516), [`TaskSchedulerService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L683-L687), [`TaskSchedulerService.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt#L1160-L1178)
- **Problem Description**:
  In `TaskSchedulerService.scheduleDayWithOutcome()`, candidate anchor hysteresis checking (line 511), cross-day prerequisite checking (line 673), and unplaced task sweep (line 1135) matched parent prerequisites strictly using `cleanSplitTaskId(it) == prereqId`.
  When tasks are split via `TaskRepository.splitTaskIntoSessions`, sessions are allocated random UUIDs and link to their parent via `parentTaskId`. Calling `cleanSplitTaskId(UUID)` produces the UUID itself, which never matches `prereqId` (the parent's UUID).
  As a result:
  1. In line 673, when the parent entity is active in `activeUncompletedTaskIds`, Part 2 was falsely diagnosed as `blockedByPrereq` and deferred with `"Prerequisite task '$blockedByPrereq' is active and pending on a future date"`.
  2. In line 511, hysteresis anchor placement rejected anchored UUID continuation parts.
  3. In line 1135, unplaced sweep failed to detect placed UUID continuation parts.
- **Root Cause**:
  Restricting parent task resolution to regex stripping of `#part` and `_part` suffixes without querying `sanitizedTaskMap[it]?.parentTaskId == prereqId` or `it.task.parentTaskId == prereqId`.
- **Suggested / Applied Fix**:
  Expanded `cleanSplitTaskId(it) == prereqId` across hysteresis checks, cross-day prerequisite filtering, and the unplaced sweep to check `sanitizedTaskMap.values.any { it.parentTaskId == prereqId || cleanSplitTaskId(it.id) == prereqId }` and `completionEndMap.keys.any { cleanSplitTaskId(it) == prereqId || sanitizedTaskMap[it]?.parentTaskId == prereqId }`.
  Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-451: `SchedulePreparationOrchestrator.kt` Anchors Untimed Date-Only Tasks to Midnight 00:00:00 and Injects Parent Task ID as Prerequisite for Part 1
- **Severity**: High
- **Category**: Core Logic & Orchestration / Temporal Anchoring & Part 1 Prerequisite Isolation
- **File**: [`SchedulePreparationOrchestrator.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt#L157-L160), [`SchedulePreparationOrchestrator.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt#L194)
- **Problem Description**:
  1. In `SchedulePreparationOrchestrator.kt` line 194, `scheduledStartMillis` was mapped using `task.resolveScheduledEpochMillis(dayStartMillis)`. For date-only tasks without a specific scheduled time (`task.scheduledTime == null`), `resolveScheduledEpochMillis` returns `scheduledDate + 0L` (local midnight 00:00:00). Passing a non-null midnight start caused untimed tasks to be sorted to the front of `candidateAnchors`, anchored at 00:00:00 in hysteresis if `workDayStart == 0`, or pull reverse-DAG deadline ceilings before midnight when locked.
  2. In lines 157-160, `task.parentTaskId` was unconditionally added to `parsedPrereqs` for all tasks where `parent.isNotBlank()`. When a task is partitioned, Part 1 represents the initial session and must not depend on its parent; adding `parent` caused Part 1 to become dependent on its own active parent, causing cross-day prerequisite deadlocks.
- **Root Cause**:
  Mapping `scheduledStartMillis` without checking `task.scheduledTime != null`, and adding `parentTaskId` to `parsedPrereqs` on Part 1 tasks.
- **Suggested / Applied Fix**:
  1. Guarded `scheduledStartMillis = if (task.scheduledTime != null) task.resolveScheduledEpochMillis(dayStartMillis) else null` in `SchedulePreparationOrchestrator.kt` and `FlowViewModelV2.kt`.
  2. Guarded parent prerequisite injection with `!isPart1`.
  3. Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---

#### BUG-452: `AutoSchedulingContracts.hasManualScheduleData()` Fails to Recognize Dual-Representation Epoch Timestamps Allowing Auto-Scheduler Overwrite
- **Severity**: Medium
- **Category**: Core Contracts & Data Integrity / Manual Schedule Recognition
- **File**: [`AutoSchedulingContracts.kt`](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingContracts.kt#L14-L16)
- **Problem Description**:
  In `AutoSchedulingContracts.kt`, `hasManualScheduleData(task)` checked `task.scheduledDate != null && task.scheduledTime != null && !task.isAutoScheduled`. Tasks created or scheduled using dual epoch representation (`task.scheduledTime >= ONE_DAY_MILLIS` and `task.scheduledDate == null`) store both the date and the time in `scheduledTime`. Because `task.scheduledDate` is null, `hasManualScheduleData` returned false, causing `isMutableByAutoScheduler` to evaluate to true. Both foreground auto-scheduling in `PlanViewModelV2` and background workers could overwrite user-scheduled appointments.
- **Expected Behavior**:
  Dual-representation epoch timestamps (`task.scheduledTime >= ONE_DAY_MILLIS`) must be recognized as having valid manual schedule data.
- **Suggested / Applied Fix**:
  Updated `hasManualScheduleData(task)` to check `hasDate = task.scheduledDate != null || (hasTime && task.scheduledTime!! >= ONE_DAY_MILLIS)`.
  Verified in [`SchedulerAuditTest.kt`](file:///home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/domain/service/SchedulerAuditTest.kt).
- **Status**: Resolved (verified in unit tests).

---
