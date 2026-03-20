# Changelog

All notable changes to proFlow are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added

**Neuroscience & Behavioral Psychology Engines**
- `AutonomyNudgeEngine` — detects tasks that have been untouched for 2+ hours and triggers a smart notification with "Not ready yet" (snooze) and "It feels too big" (auto-split) action buttons
- `FreshStartEngine` — identifies motivational fresh-start moments (Monday, month start, streak break, 3-day+ absence) and surfaces an encouraging prompt, capped at once per ISO week to avoid repetition
- `WoopEngine` — implements the Wish-Outcome-Obstacle-Plan framework with affective forecasting: tracks expected vs. actual difficulty and surfaces calibration insights
- `TaskSplitter` — auto-splits an oversized task into three subtasks (Part 1/3, 2/3, 3/3) that inherit the parent's quadrant, priority, and one-third of the impact score; the parent is archived
- `PeakEnergyDetector` — learns personal focus windows from session history using exponential recency weighting (last 7 days = 3×, last 30 days = 1.5×, older = 0.5×); blends detected windows with user-configured peak hours via linear interpolation

**Data Layer**
- `UlyssesContractEntity` / `UlyssesContractDao` / `UlyssesContractRepository` — binding commitment contracts attached to tasks; stores contract text, deadline, and win/loss outcome
- `WoopEntity` / `WoopDao` / `WoopRepository` — persists WOOP entries (wish, outcome, obstacle, plan) and affective forecast results per task

**Screens**
- **Identity** — dedicated screen for composing and managing personal identity affirmations (e.g. "I finish what I start"); duplicates are rejected; affirmations are persisted and used as self-concept anchors throughout the app
- **Log Time** — manual time-logging screen for recording effort spent outside of timed focus sessions
- **Mini WOOP Reflection** — post-focus reflection screen walking users through the four WOOP fields to close the loop on a completed session

**Analytics additions**
- **XP / Points card** — gamification layer surfacing total XP, today's XP, this-week's XP, and the top-5 point-earning tasks
- **Neuro Boost Insights card** — completion breakdown for frog tasks, anxiety tasks, public commitments, and if-then plan usage rate
- **Procrastination Radar card** — lists the top repeatedly-postponed active tasks as a motivational nudge
- **Dynamic Peak Energy card** — shows the auto-detected peak window vs. the manually configured one and the detection confidence level
- **Ulysses Contracts card** — live view of active contracts and a WIN/LOSS record for closed contracts

**Background Workers**
- `AutonomyNudgeWorker` — fires the autonomy nudge notification with smart action buttons; tagged per task so it can be cancelled if the task is touched before the 2-hour window
- `UlyssesEvaluatorWorker` — evaluates Ulysses Contract outcomes at deadline; marks WIN if the task was completed before the deadline, LOSS otherwise
- `FocusWidgetUpdateWorker` — periodically queries `TaskScoringEngine` and pushes the top-scored task to the home screen widget
- `NotificationWorker` — general-purpose notification dispatching used by the daily plan and streak check flows

**BroadcastReceivers**
- `BootReceiver` — listens for `BOOT_COMPLETED` and reschedules all WorkManager periodic workers so background intelligence resumes after a device restart
- `NudgeSnoozeReceiver` — handles the *Not ready yet* action from autonomy nudge notifications, re-queuing the nudge for a later window

**UI Components**
- `UlyssesContractSheet` — bottom sheet for creating and viewing a Ulysses Contract on a task
- `ManualTimeLogSheet` — bottom sheet for quick manual time entry without navigating to the full Log Time screen
- `WoopPromptSheet` — pre-focus bottom sheet that invites users to set a WOOP wish and anticipate obstacles before starting the timer
- `AffordanceRatingSheet` — post-focus bottom sheet for rating perceived task difficulty vs. expectation; data feeds WOOP calibration
- `NewChapterCard` — card composable for creating a new goal or life chapter in the drawer goals section
- `TopGoalsRefillCard` — card composable for surfacing and replenishing the top yearly/weekly goals in the drawer
- `DrawerGoalsSection` / `DrawerViewModel` — navigation drawer section displaying yearly and weekly goals with inline editing
- `FocusWidgetProvider` — RemoteViews-based AppWidget (`AppWidgetProvider`) showing the current top-priority task; tapping the **Start** button launches MainActivity; update is triggered via `FocusWidgetUpdateWorker`

**Project documentation**
- README, CONTRIBUTING, LICENSE, CHANGELOG, CODE_OF_CONDUCT, SECURITY

---

## [1.0.0] — 2024-01-01

### Added

**Core Application**
- `MainActivity` — entry point with onboarding flow and theme switching
- `NeuroFlowApplication` — Hilt initialization, notification channels, background worker scheduling

**Neuroscience Scoring Engine**
- `TaskScoringEngine` — composite priority scores (0–1000) integrating 16+ evidence-based frameworks:
  - Eisenhower Matrix quadrant weighting
  - Temporal Motivation Theory (deadline discounting)
  - Cognitive Load Theory (energy matching)
  - Circadian Rhythm optimization (peak hours)
  - "Eat the Frog" methodology (tackle hardest tasks first)
  - BJ Fogg Tiny Habits (momentum building)
  - Zeigarnik Effect (psychological pressure of open loops)
  - Progress Principle (motivation from partial completion)
  - Implementation Intentions (if-then planning)
  - Temptation Bundling (enjoyment modifiers)
  - Commitment Devices (social accountability)
  - Loss Aversion (goal-risk amplification)
- `AnalyticsEngine` — productivity metrics: completion rates, streaks, time tracking, goal progress

**Data Layer**
- Room database (`NeuroFlowDatabase`) with schema versions 1–6 and full migration support
- `TaskEntity` — task model with 40+ fields covering scoring inputs, timing, reminders, energy, and neuroscience boosters
- `TimeSessionEntity` — time tracking sessions with pause support
- `GoalEntity` — long-term goals linked to tasks
- Repository pattern (`TaskRepository`, `SessionRepository`, `GoalRepository`)
- Proto DataStore (`UserPreferencesDataStore`) for user preferences

**Screens**
- **Eisenhower Matrix** — four-quadrant task visualization with quadrant detail view
- **Schedule** — day/week timeline with locked-slot scheduling and work-hour awareness
- **Focus Timer** — Pomodoro-style sessions with per-task time tracking
- **Analytics** — Vico-powered charts for completion rates, streaks, and goal progress
- **History** — searchable archive of completed tasks
- **Settings** — theme selection, work hours, energy peak configuration, priority weight customization
- **Onboarding** — guided first-launch setup and in-app guide

**Background Workers**
- `DailyPlanWorker` — daily 7 am notification with top 3 prioritized tasks
- `StreakCheckWorker` — daily 9 pm habit streak verification
- `DeadlineEscalationWorker` — 4-hourly escalation of tasks ≤48 hours from deadline (SCHEDULE → DO_FIRST)

**UI**
- Material 3 theming with dynamic dark/light mode
- `NewTaskSheet` — bottom sheet composable for full task creation and editing
- `TaskRow` — reusable task list item component

---

[Unreleased]: https://github.com/takian-alt/proFlow/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/takian-alt/proFlow/releases/tag/v1.0.0
