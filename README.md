# proFlow (NeuroFlow)

> **Science-backed Android task prioritization — because not all tasks are created equal.**

proFlow is an advanced productivity and task management application for Android, built around neuroscience and behavioral psychology principles. It intelligently scores and ranks your tasks using a composite engine that draws on 16+ evidence-based frameworks so you always know what to work on next.

---

## Features

- **Eisenhower Matrix** — visualize tasks across the classic four-quadrant do/schedule/delegate/eliminate grid
- **Smart Scoring Engine** — composite priority scores (0–1000) driven by Temporal Motivation Theory, Cognitive Load Theory, Circadian Rhythm research, the Zeigarnik Effect, BJ Fogg's Tiny Habits, and more
- **Focus Timer** — built-in Pomodoro-style focus sessions with per-task time tracking and post-session WOOP reflection
- **Schedule View** — day and week timeline with locked-slot scheduling and work-hour awareness
- **Analytics Dashboard** — completion rates, productivity streaks, goal progress, Vico-powered 7-day trend charts, XP/gamification points, Neuro Boost Insights (frog tasks, anxiety tasks, public commitments), Procrastination Radar, and Dynamic Peak Energy detection
- **Task History** — searchable archive of completed tasks with full audit trail
- **Goal Linking** — attach tasks to long-term goals and track goal risk exposure; manage yearly and weekly goals from the navigation drawer
- **Background Intelligence** — WorkManager workers for daily planning notifications (7 am), streak checks (9 pm), automatic deadline escalation (every 4 hours), autonomy nudges, Ulysses Contract evaluation, and home screen widget updates; all workers are rescheduled automatically on device reboot
- **Onboarding Flow** — guided first-launch setup for identity, peak energy hours, and first task
- **Identity Affirmations** — dedicated screen for composing and managing personal identity statements (e.g. "I finish what I start"); affirmations are persisted and used as self-concept anchors throughout the app
- **WOOP Framework** — structured Wish-Outcome-Obstacle-Plan prompts before focus sessions and a mini reflection screen after; affective forecasting tracks expectation vs. reality over time
- **Ulysses Contracts** — create binding commitment contracts for critical tasks; outcome (win/loss) is automatically evaluated at the deadline
- **Autonomy Nudge System** — if a task goes untouched for 2 hours a smart notification fires with action buttons: *Not ready yet* (snooze) and *It feels too big* (auto-split into subtasks)
- **Task Splitter** — automatically breaks an oversized task into three equal subtasks inheriting quadrant, priority, and a third of the impact score; the parent task is archived
- **Fresh Start Engine** — detects motivational fresh-start moments (Monday, month start, streak break, 3-day+ absence) and surfaces an encouraging prompt, at most once per ISO week
- **Peak Energy Detector** — learns your personal focus windows from session history using exponential recency weighting; blends with manually configured peak hours
- **Manual Time Logging** — log time spent on a task outside of timed focus sessions via a dedicated bottom sheet and log-time screen
- **Affordance Rating** — after a focus session rate how difficult the task felt vs. your expectation; data feeds into WOOP calibration insights
- **Home Screen Widget** — RemoteViews AppWidget that always shows your current top-priority task; tapping the Start button launches the app; updated by a background worker on every task change
- **Theming** — Material 3 dark/light theme with user-configurable priority weights

---

## Screenshots

_Full screenshot gallery coming with the first public release. Key screens include the Eisenhower Matrix, Schedule, Focus Timer with WOOP prompts, Analytics Dashboard, Identity Setup, and the home screen widget._

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose 2.8.5 |
| Architecture | MVVM, Clean Architecture |
| DI | Hilt 2.51.1 |
| Database | Room 2.6.1 (SQLite, 6 migrations) |
| Preferences | Proto DataStore |
| Background | WorkManager |
| Charts | Vico |
| Build | Gradle 8.x with Kotlin DSL, KSP |
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Android 15 (API 35) |

---

## Architecture

```
app/
└── src/main/java/com/neuroflow/app/
    ├── di/                    # Hilt dependency injection modules
    ├── domain/
    │   ├── engine/            # TaskScoringEngine, AnalyticsEngine,
    │   │                      #   AutonomyNudgeEngine, FreshStartEngine,
    │   │                      #   WoopEngine, TaskSplitter, PeakEnergyDetector
    │   └── model/             # Enums (Quadrant, Priority, TaskStatus …)
    ├── data/
    │   ├── local/             # Room database, DAOs, entities (incl.
    │   │                      #   UlyssesContractEntity, WoopEntity), DataStore
    │   └── repository/        # Repository pattern over DAOs (incl.
    │                          #   UlyssesContractRepository, WoopRepository)
    ├── presentation/
    │   ├── common/            # Navigation scaffold, shared composables, theme,
    │   │                      #   UlyssesContractSheet, ManualTimeLogSheet,
    │   │                      #   NewChapterCard, TopGoalsRefillCard,
    │   │                      #   DrawerGoalsSection
    │   ├── matrix/            # Eisenhower Matrix screen
    │   ├── schedule/          # Day/week schedule screen
    │   ├── focus/             # Focus timer screen, WoopPromptSheet,
    │   │                      #   MiniWoopReflectionScreen, AffordanceRatingSheet
    │   ├── analytics/         # Analytics screen
    │   ├── history/           # Completed tasks screen
    │   ├── identity/          # Identity affirmations screen
    │   ├── logtime/           # Manual time log screen
    │   ├── settings/          # Settings & priority weights screens
    │   ├── onboarding/        # Onboarding & app guide screens
    │   └── widget/            # Home screen widget (RemoteViews AppWidget)
    ├── worker/                # WorkManager background workers (incl.
    │                          #   AutonomyNudgeWorker, UlyssesEvaluatorWorker,
    │                          #   FocusWidgetUpdateWorker, NotificationWorker)
    └── receiver/              # BroadcastReceivers: BootReceiver,
                               #   NudgeSnoozeReceiver
```

Data flows **unidirectionally**: Compose UI → ViewModel (StateFlow) → Repository → Room / DataStore.

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK with API level 26–35

### Build

```bash
# Clone the repository
git clone https://github.com/takian-alt/proFlow.git
cd proFlow

# Build a debug APK
./gradlew assembleDebug

# Run all tests
./gradlew test
```

Open the project in Android Studio, allow Gradle to sync, then run the **app** configuration on an emulator or physical device (Android 8.0+).

### Install a debug APK

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Contributing

We welcome contributions of all sizes! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.

---

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for a full history of releases and changes.

---

## Security

If you discover a security vulnerability, please follow the responsible disclosure process described in [SECURITY.md](SECURITY.md).

---

## Code of Conduct

This project is governed by the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating you agree to uphold its terms.

---

## License

Distributed under the [MIT License](LICENSE).
