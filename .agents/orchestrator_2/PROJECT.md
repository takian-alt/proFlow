# Project: neuroFlow V2 UI & ADHD Cognitive Safety Nets Modernization

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
│  │  - Timeline Stream   │ │  - Autopilot Protect │ │ - Ulysses Card │  │
│  │  - Zero-Guilt Rollov │ │  - Tag Filter Pills  │ │ - Post-Session │  │
│  │  - Magic Wand Scaff. │ │  - Notes BottomSheet │ │   Calibration  │  │
│  │  - 4s Non-Destr Undo │ └──────────────────────┘ └────────────────┘  │
│  │  - Evening Shutdown  │                                              │
│  │  - Notes BottomSheet │ ┌──────────────────────────────────────────┐  │
│  └──────────────────────┘ │          Focus Screen & Service          │  │
│                           │  - 4 Modes: 5m, 25m, 90m, Flow Stopwatch │  │
│                           │  - 60s Physiological Sigh Guide          │  │
│                           │  - Micro-Step Checklist (StepByStepPlan) │  │
│                           │  - Thought Parking Lot Drawer            │  │
│                           │  - 1-5 Affordance Effort Rating          │  │
│                           │  - System Priority DND Integration       │  │
│                           └──────────────────────────────────────────┘  │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         V2 Domain Engine Layer                         │
│  - BreakItDownEngine: 3-step initiation scaffolder -> ifThenPlan       │
│  - TaskScoringEngine: LiveEnergyContext ranking & rationale generation │
│  - TaskInferenceEngine: Natural language keyword & tag inference       │
│  - Borbély & Peak Engines: Circadian alertness & sleep pressure model  │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          Data & Storage Layer                          │
│  - Room Database (v21 schema, 11 tables, 21 migrations, 100% backward) │
│  - TaskStatus: ACTIVE, COMPLETED, ARCHIVED, ICEBOX (non-destructive)   │
│  - Repositories: Task, Note, SleepLog, UlyssesContract, EnergyScore    │
└────────────────────────────────────────────────────────────────────────┘
```

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Obsidian Sanctuary Palette Tokens | Update `NeuroFlowThemeV2Colors` with exact Slate surfaces (`#13151B`, `#1C1F28`) and border (`#2A2E3B`), Electric Indigo, Sage, Amber, Muted Coral | M1 | Survey / R1 |
| 2 | AMOLED Mode Support in V2 | Wire `amoledDark` preference through `NeuroFlowThemeV2` to set pure black (`#000000`) background and surfaces | M1 | Survey / R1 |
| 3 | Universal Elimination of Alarmist Red | Replace `#C62828`, `#D32F2F`, `🔴`, `🚨` with calm indicators (`MutedCoral`, `WarmAmber`, `🎯`, `⚡`) across classic and V2 components | M1 | Survey / R1 |
| 4 | Flow 3-Section Layout & Hero Card | Decompose Flow into Circadian Barometer, standalone `RecommendedNextActionHeroCard`, and vertical timeline | M2 | Survey / R2 |
| 5 | LiveEnergyContext Hero Recommendation | Wire `TaskScoringEngine.rankTasks` with `LiveEnergyContext` in `FlowViewModelV2` with clear neuroscience rationale badges | M2 | Survey / R2 |
| 6 | Zero-Guilt Fresh Start (>3 Overdue) | Add `ICEBOX` to `TaskStatus`, detect >3 overdue tasks, prompt gentle 1-tap rollover clearing clutter | M2 | Survey / R2 |
| 7 | "Break It Down" Magic Wand Scaffolder | Implement `BreakItDownEngine` generating 3 microscopic steps, serialized into `ifThenPlan` with interactive checkboxes | M2 | Survey / R2 |
| 8 | 4-Second Non-Destructive Undo | Add floating undo snackbar on Flow screen with 4000ms delay coroutine for task completion & deferral | M2 | Survey / R2 |
| 9 | Evening Shutdown Complete Reflection | Dynamic card appearing after 19:00 with completed tasks, focus minutes, and 1-tap "Push Remaining to Tomorrow" | M2 | Survey / R2 |
| 10 | 4 Focus Modes | Support "Just 5 Minutes" (300s), 25m Pomodoro (1500s), 90m Ultradian (5400s), and Flow Stopwatch in `FocusViewModelV2` | M3 | Survey / R3 |
| 11 | 60s Physiological Sigh Breathing Guide | Stanford/Huberman cyclic sighing guide (10 cycles: 2s inhale + 1s top-off + 3s slow exhale) on break transitions / on-demand | M3 | Survey / R3 |
| 12 | Focus Checklist & Thought Parking Drawer | Embedded checklist from `task.ifThenPlan` and instant bottom drawer to park intrusive thoughts as low-energy tasks | M3 | Survey / R3 |
| 13 | Post-Session Affordance Rating | 1-tap 1-5 effort rating mapped to -2f..+2f `affectiveForecastError` feeding analytics & priority weighting | M3 | Survey / R3 |
| 14 | Priority System DND Integration | Automatic Priority DND (`INTERRUPTION_FILTER_PRIORITY`) engagement during active focus tracking | M3 | Survey / R3 |
| 15 | Notes Scratchpad Bottom Sheet | Distraction-free Notes bottom sheet surfaced from Flow and Plan top app bars, backed by `NoteRepository` | M4 | Survey / R4 |
| 16 | Circadian Barometer Classic Links | Direct action pills on `CircadianHeroBarometer` linking to `MEQQuizScreen` and `SleepLogsScreen` | M4 | Survey / R4 |
| 17 | Ulysses Pre-Commitment Contracts | Active contracts card and "+ Create Contract" on `InsightsScreenV2`, plus 1-tap binding pill on high-resistance tasks | M4 | Survey / R4 |
| 18 | Floating Context/Tag Pills | Sticky tag pills (`#deepwork`, `#offline`, `#admin`) on Flow & Plan for instant stream filtering | M4 | Survey / R4 |
| 19 | E2E Suite & Forensic Verification | Pass 100% of unit and E2E tests, verified by Forensic Auditor and Challengers | M5 | Survey / AC |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Calm Sensory Design System ("Obsidian Sanctuary") | Features 1-3: Token updates, AMOLED mode support, universal alarmist red elimination | None | PLANNED |
| M2 | Anti-Paralysis Scaffolding & Flow Screen Experience | Features 4-9: Hero card separation, LiveEnergy ranking, Zero-Guilt Fresh Start, Break-It-Down magic wand, 4s undo, Evening Shutdown | M1 | PLANNED |
| M3 | Focus Experience, Autonomic Recovery & DND | Features 10-14: 4 focus modes, 60s physiological sigh, micro-checklist & thought parking, 1-5 rating, Priority DND | M1, M2 | PLANNED |
| M4 | Contextual Integration of Secondary Tools | Features 15-18: Notes bottom sheet, Circadian MEQ/Sleep links, Ulysses contracts in Insights, floating tag pills | M1, M2, M3 | PLANNED |
| M5 | Full Verification, E2E Testing & Coverage Hardening | Feature 19: Full `./gradlew testDebugUnitTest` pass, adversarial stress testing, and forensic integrity audit | M1, M2, M3, M4 | PLANNED |

## Code Layout
- `app/src/main/java/com/neuroflow/app/presentation/v2/theme/NeuroFlowThemeV2.kt`: Master V2 Obsidian Sanctuary theme & tokens
- `app/src/main/java/com/neuroflow/app/presentation/common/theme/Color.kt`: Classic theme color definitions & neutralized alarmist colors
- `app/src/main/java/com/neuroflow/app/presentation/v2/flow/`: FlowScreenV2, FlowViewModelV2, RecommendedNextActionHeroCard, EnergyTimelineStream, ZeroGuiltFreshStartCard, EveningShutdownCard
- `app/src/main/java/com/neuroflow/app/domain/engine/BreakItDownEngine.kt`: Initiation micro-step scaffolder
- `app/src/main/java/com/neuroflow/app/domain/model/Enums.kt`: TaskStatus with ICEBOX
- `app/src/main/java/com/neuroflow/app/presentation/v2/focus/`: FocusScreenV2, FocusViewModelV2, PhysiologicalSighGuide, ThoughtParkingBottomSheet
- `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt`: Plan screen with tag filter pills & Notes bottom sheet
- `app/src/main/java/com/neuroflow/app/presentation/v2/insights/InsightsScreenV2.kt`: Insights with Ulysses contracts & chronotype/circadian cards
- `app/src/main/java/com/neuroflow/app/presentation/v2/components/`: NotesBottomSheetV2, CircadianHeroBarometer
- `app/src/test/java/com/neuroflow/app/`: Unit tests and E2E test suites
