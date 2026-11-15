# Architecture Design Document: Auto-Pilot Daily Blueprint, UI Polish & Hardened Scheduling Engine

**Status:** Ratified & Approved  
**Date:** 2026-11-15  
**Target Subsystems:** Presentation V2 (Flow, Plan), Domain Schedulers (`MultiDayScheduleOrchestrator`, `SchedulePreparationOrchestrator`), AI Domain Services (`GeminiAiOnlineService`), Calendar Integration (`CalendarSyncRepository`, `CalendarSyncDataSource`)  
**Scope:** Project (Multi-Component Refinement & Enhancement)  

---

## 1. Intent and the "Why"
PROCUS (NeuroFlow) is an ADHD-adapted chronobiological productivity engine. While individual mechanisms (circadian modeling, auto-scheduling heuristics, and Gemini BYOK task decomposition) function independently, the daily user experience currently lacks a unified, proactive entry point:
1. **Planning Friction:** Users must manually juggle unscheduled backlog tasks, evaluate their cognitive budget, and guess how well slots match their biological energy curves.
2. **Scheduling Disconnect:** The current `MultiDayScheduleOrchestrator` uses hardcoded assumptions (e.g., fixed 7:00 AM wake, fixed 8-hour sleep) rather than pulling live physiological anchors, and task splitting across multiple days does not gracefully cascade subsequent parts with warmup buffers.
3. **UI / Interaction Gaps:** Task cards across Flow and Plan lack clear duration badges, split-task progress indicators (e.g., `Part 1 of 3`), and vivid circadian energy phase accents; the Flow Hero screen does not offer a proactive 1-tap "Plan My Day" experience when idle.
4. **Shallow Feedback Loop:** When users dislike a scheduled slot (thumbs-down), the system applies a blind numeric penalty and triggers a raw reschedule without understanding *why* (e.g., "Too low energy", "Too early", "Too close to lunch"), failing to provide intelligent instant remedies.

This upgrade introduces an interactive, AI-assisted **Daily Blueprint** workflow, hardens multi-day chronobiological scheduling with true sleep telemetry, upgrades task cards and hero components with tactile modern UI, and closes the feedback loop with context-aware AI remedies.

---

## 2. Goals, Non-Goals, and Anti-Goals

### Goals
- **G-1: Interactive Daily Blueprint Sheet:** Provide a cohesive, 1-tap bottom sheet accessible from Flow Hub (hero card when idle) and Plan Hub (action bar) showing an AI coaching rationale, cognitive capacity meter (Planned vs. 240m target), proposed schedule timeline with include/exclude toggles, and 1-tap "Apply Schedule".
- **G-2: Hybrid AI & Deterministic Division:** Combine deterministic constraint scheduling (exact non-overlapping slots, busy window collision avoidance, calendar buffers) with single-bundled Gemini BYOK inference (holistic coaching, task prioritization, energy peak explanations) and 100% offline heuristic fallbacks.
- **G-3: Hardened Chronobiology & Multi-Day Scheduling:**
  - Pull actual wake times and sleep debt from `SleepTelemetryRepository` and `UserPreferencesDataStore` into `MultiDayScheduleOrchestrator` (retiring hardcoded 7:00 AM / 8h assumptions).
  - Implement multi-day load leveling and cognitive capacity overflow (automatically cascading overflow tasks to the next best day in the 7-day horizon, while scoping commit to the target day).
  - Implement zero-jitter calendar reconciliation in the domain layer (nudging overlapping tasks with minimal displacement rather than thrashing the whole schedule).
- **G-4: Context-Aware Feedback & Instant AI Remedy:**
  - Upgrade slot rating into an actionable dialog with quick reason tags (`Too low energy`, `Too early`, `Needs longer block`, `Conflict with routine`).
  - Invoke `GeminiAiOnlineService.remedyRejectedSlot(...)` to generate an immediate, reasoned alternative slot suggestion while updating the local `adaptiveSystemDirective` and `SchedulingFeedbackStore`.
- **G-5: Visual & Tactile UI Polish:**
  - Enhance `TimelineTaskCard` with clear duration chips, split-task pill progress indicators (e.g., `Part 1 of 3`), circadian glow borders, and tactile swipe gestures.
  - Upgrade `FlowSpotlightHeroCard` and `CircadianHeroBarometer` with animated energy state transitions and clean distraction-free styling.

### Non-Goals
- **NG-1:** We are NOT replacing the underlying Room database schema or adding new database tables (avoids Room migration churn; transient blueprint state is kept in memory / ViewModel scope).
- **NG-2:** We are NOT building an unconstrained conversational chatbot interface (adheres to the single-bundled prompt and 20-min TTL quota invariants).
- **NG-3:** We are NOT eliminating manual task placement or manual drag-and-drop; manual scheduling remains fully supported as high-priority locked anchors.

### Anti-Goals
- **AG-1 (No Silent Hallucinations):** The AI must never invent phantom time slots that overlap with calendar events or violate user-configured protected windows. All final slot bounds remain mathematically enforced by `TaskSchedulerService`.
- **AG-2 (No Network Hard-Dependency):** The app must never fail or block the user if Gemini API key is missing or network is unavailable; offline heuristics must synthesize the Daily Blueprint and feedback remedies seamlessly.
- **AG-3 (No Schedule Thrashing):** Re-balancing or calendar reconciliation must never move pinned, completed, or in-focus tasks, and must preserve placement stability for previously accepted slots.

---

## 3. Evaluated Approaches & Selection

### Approach 1: Integrated Domain Orchestration & Reusable UI Modules (Selected)
- **Domain:** Dedicated `DailyBlueprintOrchestrator` domain service coordinating `CircadianEnergyService`, `MultiDayScheduleOrchestrator`, and `GeminiAiOnlineService`.
- **Presentation:** Reusable `DailyBlueprintBottomSheet`, enhanced `TimelineTaskCard`, and `SchedulingFeedbackDialog`.
- **Decision:** Selected because it strictly adheres to the Strangler-Fig refactoring pattern, cleanly separates domain logic from UI presentation, and guarantees 100% test pass rate across existing 1,331 unit tests.

### Approach 2: Monolithic ViewModel-Coupled Pipeline (Rejected)
- Inlines orchestration directly into `PlanViewModelV2` and `FlowViewModelV2`. Rejected due to heavy code duplication and bloated ViewModel complexity.

### Approach 3: WorkManager Background Pipeline (Rejected)
- Offloads blueprint generation to an asynchronous background worker and writes to Room. Rejected because it introduces unnecessary database migrations and causes sluggish UI response times for real-time interactive morning planning.

---

## 4. Architectural Specifications & Detailed Design

```
+-----------------------------------------------------------------------------------+
|                              Presentation Layer (V2)                             |
|                                                                                   |
|   +--------------------------+                     +--------------------------+   |
|   |       FlowScreenV2       |                     |       PlanScreenV2       |   |
|   |  - FlowSpotlightHeroCard |                     |  - TimelineScheduleView  |   |
|   |  - "Plan My Day" Action  |                     |  - "Auto-Plan Blueprint" |   |
|   +------------+-------------+                     +------------+-------------+   |
|                |                                                |                 |
|                +-----------------------+------------------------+                 |
|                                        |                                          |
|                                        v                                          |
|                       +----------------------------------+                        |
|                       |    DailyBlueprintBottomSheet     |                        |
|                       |  - AI Energy Coaching Rationale  |                        |
|                       |  - Cognitive Capacity Meter      |                        |
|                       |  - Proposed Task Timeline List   |                        |
|                       |  - Include/Exclude Task Toggles  |                        |
|                       |  - 1-Tap "Apply Schedule" CTA    |                        |
|                       +----------------------------------+                        |
+----------------------------------------+------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                               Domain Orchestration Layer                          |
|                                                                                   |
|                       +----------------------------------+                        |
|                       |    DailyBlueprintOrchestrator    |                        |
|                       +----------------+-----------------+                        |
|                                        |                                          |
|            +---------------------------+--------------------------+               |
|            |                           |                          |               |
|            v                           v                          v               |
|  +--------------------+     +---------------------+    +-----------------------+  |
|  | MultiDaySchedule   |     | CircadianEnergy     |    | GeminiAiOnlineService |  |
|  | Orchestrator       |     | Service             |    | - Blueprint Coaching  |  |
|  | - Load leveling    |     | - Sleep telemetry   |    | - Instant Remedy      |  |
|  | - Capacity overflow|     | - Dynamic wake/peak |    | - Local heuristic     |  |
|  +--------------------+     +---------------------+    +-----------------------+  |
+-----------------------------------------------------------------------------------+
```

### 4.1 Domain Layer: `DailyBlueprintOrchestrator`
A new singleton domain service injected via Hilt into `PlanViewModelV2` and `FlowViewModelV2`:
```kotlin
data class BlueprintTaskItem(
    val task: TaskEntity,
    val proposedStartMillis: Long,
    val proposedEndMillis: Long,
    val energyPhase: EnergyPhase,
    val isDuringPeak: Boolean,
    val isIncluded: Boolean = true,
    val conflictWarning: String? = null
)

data class DailyBlueprint(
    val targetDateMillis: Long,
    val plannedMinutes: Int,
    val capacityTargetMinutes: Int,
    val capacityUtilizationPercent: Float,
    val aiStrategyRationale: String,
    val items: List<BlueprintTaskItem>,
    val overflowTasks: List<TaskEntity>
)
```

**Key Execution Pipeline:**
1. **Dynamic Sleep Anchoring:** Query `sleepTelemetryRepository.observeAllSleepLogs().first()` and `userPreferencesDataStore.preferencesFlow.first()`. Determine actual wake time anchor and sleep debt (retiring hardcoded 7:00 AM / 8h defaults).
2. **Candidate Selection:** Filter active backlog tasks and tasks assigned to target day where `isMutableByAutoScheduler == true`.
3. **Multi-Day Load Leveling:** Run `schedulerService.scheduleDayWithOutcome` against target day. If total scheduled deep work exceeds `dailyDeepWorkTargetMinutes` (default 240m), excess tasks overflow to subsequent days in the 7-day horizon.
4. **Commit Boundary (Target Day Only):** When the user taps "Apply Schedule", placements are committed to `TaskRepository` and `CalendarSyncRepository` strictly for the target day. Overflow tasks remain safely in the user's backlog for future days without locking the upcoming week's calendar prematurely.
5. **Interactive Toggling:** Unchecking a task in `DailyBlueprintBottomSheet` (`isIncluded = false`) updates the Capacity Meter immediately in UI state. At commit time, unchecked tasks simply remain unslotted in the backlog. A "Re-pack Schedule" button is provided if the user wishes to backfill freed capacity.
6. **AI Coaching Synthesis:** Call `geminiAiOnlineService.generateDailyBlueprintCoaching(...)` with candidate tasks, user chronotype peak, capacity metrics, and persistent `adaptiveSystemDirective`.
   - If offline or no BYOK key: execute `localBlueprintCoaching(...)` generating structured, chronobiologically sound advice.

### 4.2 MultiDayScheduleOrchestrator & Calendar Hardening
1. **Dynamic Wake & Sleep Integration:** Replace hardcoded `7 * 3600_000L` wake anchor and `lastSleepDurationMinutes = 480` with dynamic wake epoch computed from user preferences / recent sleep log.
2. **Cognitive Capacity Overflow:** Respect `dailyDeepWorkTargetMinutes`. When day load fills, identify overflow tasks and project their placement onto subsequent days in memory to calculate total weekly workload.
3. **N-Part Task Continuity:** Support arbitrary `Part i/N` continuation tasks with 10–15 minute context re-entry warmup buffers across day boundaries, persisting parts in Room with `${parentTaskId}#partN` and `parentTaskId` references.
4. **Zero-Jitter Calendar Reconciliation (Domain-Level):**
   - Orchestrated in `SchedulePreparationOrchestrator` using `CalendarSyncRepository.detectExternalModifications`.
   - When an external event shifts, calculate the minimum delta necessary to nudge overlapping tasks to the nearest contiguous non-busy gap.
   - If no gap fits before bedtime, mark the task with a conflict warning rather than clearing all daily assignments or thrashing the entire schedule.

### 4.3 AI Service Enhancements in `GeminiAiOnlineService`
1. **`generateDailyBlueprintCoaching`**:
   - Bundled prompt containing: candidate tasks, planned duration, target day, peak window hours, calendar busy windows, and `adaptiveSystemDirective`.
   - Output: JSON containing `coachingHeadline`, `strategyBody`, and `peakRecommendation`.
   - In-memory 20-min cache and HTTP 429 backoff guard.
2. **`remedyRejectedSlot`**:
   - Inputs: `task`, `rejectedStartHour`, `reasonTag` (e.g. `TOO_LOW_ENERGY`, `TOO_EARLY`, `NEEDS_LONGER_BLOCK`, `ROUTINE_CONFLICT`), `freeWindows`, `circadianTimeline`.
   - Output: JSON with `recommendedSlot`, `reasoning`, and updated `directivePatch`.
   - Offline fallback: Rule-based heuristics shifting high-energy tasks into peak circadian windows and low-energy tasks into recovery phases.
3. **Feedback Persistence Semantics:**
   - `SchedulingFeedbackStore` updates stored JSON with `{ts, neg, tag}` (100% backward compatible with existing `{ts, neg}` entries).
   - When the user confirms "Accept & Move" on an AI remedy, the `directivePatch` is merged into `UserPreferencesDataStore.adaptiveSystemDirective`, and the single task is updated with its new slot and schedule lock.

### 4.4 Presentation Layer V2: UI & Interaction Polish
1. **`DailyBlueprintBottomSheet`**:
   - Modern Material 3 bottom sheet.
   - Header with glowing AI badge, date label, and cognitive load progress bar (`180m / 240m`).
   - Strategy callout card displaying AI coaching with expandable details.
   - Reorderable / toggleable list of proposed task placements with energy badges, time chips, and split-task indicators.
   - Primary sticky CTA: "Apply Schedule (1-Tap)" which commits placements to `TaskRepository` and syncs with `CalendarSyncRepository`.
2. **`TimelineTaskCard` Enhancements**:
   - Prominent duration pill with icon (e.g., `⚡ 45m`).
   - Split task progress indicator: `Part 1/3` badge with horizontal progress bar.
   - Circadian energy phase accent border (`Peak Mint`, `Moderate Amber`, `Low Coral`).
   - Clear start-end time labels (`09:30 – 10:15 AM`).
   - Tactile swipe-to-complete and swipe-to-reschedule with haptic feedback.
3. **`FlowSpotlightHeroCard` & `CircadianHeroBarometer` Polish**:
   - When idle, showcase a prominent "✨ Plan My Day" action button in `FreeFlowHeroCard` launching the Blueprint sheet.
   - Smooth animated transitions for the live energy barometer using Compose canvas rendering.
4. **`SchedulingFeedbackDialog`**:
   - Clean modal presenting quick reason chips:
     - ⚡ "Too low energy at this time"
     - ⏰ "Too early in the day"
     - ⏱️ "Needs longer uninterrupted block"
     - 🛑 "Conflicts with my daily routine"
   - On tap, shows immediate AI alternative slot proposal with 1-tap "Accept & Move" or "Re-schedule".

---

## 5. Summary of Settled Builder Calls
- **Apply Schedule Scope:** Scoped to the target day only; overflow tasks remain unassigned in the backlog.
- **Calendar Reconciliation Layer:** Orchestrated at the domain layer (`SchedulePreparationOrchestrator`), keeping `CalendarSyncDataSource` purely focused on Android Calendar provider queries.
- **Conflict Nudge Policy:** Minimum displacement nudge to nearest contiguous gap; overflow warning if no gap remains before bedtime.
- **N-Part Task Representation:** Persisted with `${parentTaskId}#partN` and `parentTaskId` in Room, matching BUG-WORKER-01 patterns.
- **Blueprint Sheet Toggling:** Synchronous immediate capacity meter update on checkbox toggle; commit respects unchecked state without requiring an expensive intermediate network re-solve.

---

## 6. Verification & Greenness Plan
- Run full unit test suite with invariant flags:
  `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1`
- Ensure 100% of existing 1,331 tests pass with 0 failures and 0 errors.
- Add dedicated unit test suites:
  1. `DailyBlueprintOrchestratorTest.kt`: Validates capacity bounding, task selection, offline fallback, and inclusion/exclusion logic.
  2. `SchedulingFeedbackRemedyTest.kt`: Validates reason tag ingestion, penalty store update, and alternative slot recommendation.
  3. `MultiDayLoadLevelingTest.kt`: Validates dynamic sleep anchors, capacity overflow cascading across 7 days, and N-part task continuity.
