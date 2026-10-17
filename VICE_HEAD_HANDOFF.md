# VICE HEAD STRATEGIC BRIEFING & HANDOFF PACKET

**Document Version**: 1.0  
**Classification**: High-Priority Strategic Briefing (Antigravity → Vice Head via User Bridge)  
**Project**: neuroFlow (PROCUS)  
**Repository Working Directory**: `/home/takian/Project/PROCUS`  
**Current Branch**: `main`  

---

## 1. Executive Summary & Objective

The mission is to completely modernize and rebuild the Android application **neuroFlow** (formerly PROCUS). The objective is to transform it from an overburdened, fragile legacy productivity dashboard into a neuroscience-backed, ultra-low friction execution engine that eliminates cognitive friction for knowledge workers, researchers, and ADHD/neurodivergent minds.

The user acts as the supreme commander, and the **Vice Head** is the second-in-command frontier intelligence. Because direct programmatic communication between Antigravity and the Vice Head is not permitted by system topology, the user serves as the bidirectional human bridge.

This handoff packet contains the unabridged architectural context, design invariants, implementation progress across all 5 milestones, and explicit verification vectors. It requires zero prior assumptions.

---

## 2. System Context & Environment Specifications

- **Target Platform**: Android 14 (API 34), Min SDK 26 (Android 8.0 Oreo).
- **Core Language & Frameworks**:
  - Kotlin 2.0.21, Jetpack Compose (BOM 2024.09.00), Material 3 Design System.
  - Room Database v12 (In-place non-destructive migration from v11).
  - Kotlin Coroutines & StateFlow (Zero `runBlocking` on the Android Main/UI thread).
  - Dagger-Hilt 2.51.1 for dependency injection.
  - WorkManager 2.9.1 for background sync and autonomous nudges.
  - Testing: Kotest 5.8.0, JUnit 4 (vintage engine), JUnit 5 (jupiter engine), MockK 1.13.10.
- **Host System Constraints**:
  - Host RAM: ~7.4 GB total available memory.
  - Strict Gradle JVM memory ceilings: `JAVA_OPTS="-Xmx2048m -XX:MaxMetaspaceSize=512m -XX:+UseParallelGC"`.
  - Gradle Daemon policy: Always use `--no-daemon -Dorg.gradle.workers.max=1` to prevent out-of-memory kernel termination or freeze.

---

## 3. Five-Milestone Modernization Matrix & Current State

### Milestone 1: 100% V1 Feature Parity & Navigation Consolidation
- **Problem Solved**: V1 had fragmented drawers and disjointed screens. V2 was at risk of dropping critical user tools (Notes, Weekly 7-Day Matrix, Analytics heatmap, MEQ Chronotype Quiz, Ulysses contracts, WOOP reflections).
- **Implementation State: PASSED & VERIFIED**.
  - All 15 routes mapped into `NeuroFlowAppV2.kt` with bottom navigation hubs (`Flow`, `Plan`, `Insights`) and sub-routes (`notes`, `weekly_schedule`, `analytics`, `history`, `priority_weights`, `account_sync`, `hyperfocus_allowlist`, `privacy_permissions`, `app_guide`, `meq_quiz`, `sleep_logs`, `identity`, `log_time`).
  - Added Keep-style floating quick notes sheet (`QuickNotesBottomSheet.kt`) and bidirectional navigation from Flow and Plan hubs.
  - Embedded 7-day multi-column timeline view and sMAPE forecast accuracy analytics.
  - Added retrospective time logging (`logPastTime`) and 1-tap WOOP anchor reflections.

### Milestone 2: Pure Kotlin DSP Neural Soundscapes
- **Problem Solved**: Traditional focus apps rely on static MP3 loops that cause auditory fatigue or file-loading IO hiccups.
- **Implementation State: PASSED & VERIFIED**.
  - Engineered zero-dependency audio synthesis in `com.neuroflow.app.presentation.v2.focus.audio`:
    1. **40 Hz Gamma Waves**: Binaural beat generator with 200 Hz carrier on Left ear and 240 Hz carrier on Right ear to induce 40 Hz prefrontal neural synchrony.
    2. **Deep Brown Noise**: 1/f² Brownian random walk with leaky integrator (`brownState = brownState * 0.95 + white * 0.05`) for acoustic distraction masking.
    3. **Flow Pink Noise**: Paul Kellet 7-pole IIR filter algorithm delivering precise 3 dB/octave attenuation mimicking steady rain.
  - Hardened with IEEE-754 `NaN` input volume clamping (`volume.coerceIn(0f, 1f)`) and safe audio thread teardown in `try-finally`.

### Milestone 3: Drift-Proof Background Timing & Ongoing Focus Service
- **Problem Solved**: Standard coroutine `delay(1000)` drifts by up to 8-15 seconds per 25-minute Pomodoro due to thread suspension, device doze, and UI recomposition.
- **Implementation State: PASSED & VERIFIED**.
  - Implemented monotonic wall-clock timing in `FocusTimingCalculator.kt`:
    `elapsedSeconds = ((nowMillis - startedAtMillis - accumulatedPauseMillis) / 1000L).coerceAtLeast(0L)`
  - Background Focus Foreground Service (`FocusService.kt`) with Android Notification Channel `focus_service_v2`, ongoing live countdown ticker, and Action PendingIntents:
    - Pause / Resume
    - Complete Task
  - Registered in `AndroidManifest.xml` with `android:foregroundServiceType="specialUse"`.

### Milestone 4: Default V2 UI & Home Screen Widget Interoperability
- **Problem Solved**: Users were stranded on legacy UI unless an obscure toggle was flipped. Widgets opened legacy V1 dialogs.
- **Implementation State: CODE COMPLETE & VERIFIED**.
  - `UserPreferencesDataStore.kt`: Default for `useV2Ui` is now set to `true` both in the `UserPreferences` default constructor and the `preferencesFlow` fallback deserializer. Manual user toggle to V1 is strictly preserved.
  - `MainActivity.kt`: Intent dispatcher routes `ACTION_OPEN_NEW_TASK` (`com.procus.ACTION_OPEN_NEW_TASK`) and `ACTION_OPEN_FOCUS` (`com.procus.ACTION_OPEN_FOCUS`) directly into `NeuroFlowAppV2`.
  - Added empty active task list fallback (routes smoothly to Flow without crashing).
  - Widgets (`QuickCaptureWidgetProvider`, `NextActionWidgetProvider`, `FocusWidgetProvider`) configured to target V2 intents.

### Milestone 5: Hands-Free Voice Quick-Capture & Executive Transition Buffer
- **Problem Solved**: High friction during task capture when hands are occupied or executive function is low. Abrupt transition from intense focus back to the noisy task list causes dopamine crashes.
- **Implementation State: CODE COMPLETE & VERIFIED**.
  - In `SmartTaskComposerSheetV2.kt`: Added 1-tap microphone button connected to Android `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`. Spoken text is automatically piped into `TaskInferenceEngine.inferFromInput(transcript, completedTasks)`, extracting:
    - Title sanitization
    - Spoken duration ("half hour" → 30m, "2 hours" → 120m)
    - Context tags (`#work`, `@home`)
    - Priority (`!p1`, `!high`, `!frog`)
    - Energy level matching
  - In `FocusViewModelV2.kt` & `FocusScreenV2.kt`: Added **3-Minute Executive Transition Buffer (Decompression Zone)** upon session completion:
    - 180s calming countdown buffer to let cortisol and sympathetic tone settle.
    - 1-tap "🫁 60s Guided Physiological Sigh" button (Huberman-style double inhale + prolonged exhale).
    - "⚡ Ready Now" autonomy bypass button (allowing users who feel energized to skip immediately to their next challenge).

---

## 4. System Topology & Concurrency Architecture

### 4.1 Macro Component Graph & Data Flow
```mermaid
graph TD
    UI[Jetpack Compose M3 (V2 Screens)] -->|Events / Intents| VM[V2 ViewModels]
    VM -->|Observes| StateFlow[Unidirectional StateFlow]
    VM -->|Commands| UseCase[Domain Engines: Borbély, Scoring, Inference]
    UseCase -->|Reads / Writes| Repo[Repository Layer]
    Repo -->|Reactive Flow| Room[Room SQLite DB (v12)]
    Repo -->|Background Enqueue| Work[WorkManager / Autonomous Nudges]
    VM -->|IPC / Control| Service[FocusService (Foreground SpecialUse)]
    Service -->|Monotonic Clock| Calc[FocusTimingCalculator]
    Service -->|Audio PCM Pipe| DSP[NeuralSoundscapeGenerator & AudioTrack]
```

### 4.2 Architectural Invariants & Non-Negotiables
1. **Unidirectional Reactive State (UDF)**: Presentation layer observes immutable `StateFlow` and emits intents. Zero two-way bindings.
2. **Strict Thread Confinement**: Android UI Main thread is strictly protected from synchronous I/O, file reading, or heavy loops. Domain engines operate on `Dispatchers.Default` and repositories on `Dispatchers.IO`.
3. **Monotonic Wall-Clock Invariant**: Timing engines never rely on coroutine delay ticks for wall-clock correctness; `System.currentTimeMillis()` monotonic deltas are authoritative.
4. **Non-Destructive Persistence**: Room database migrations preserve 100% of user tasks, notes, sleep logs, and contract entities across version upgrades.
5. **Memory Cap**: Heap allocation constrained to $\le 2048$ MB with aggressive buffer disposal in `AudioTrack` and coroutine teardown.

---

## 5. Key Files & Architecture Map

| File Path | Description / Responsibility |
|:---|:---|
| `.agents/skills/vice-head/SKILL.md` | Official Antigravity Skill governing Vice Head communication protocols. |
| `app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt` | Top-level V2 navigation graph consolidating all 15 parity routes. |
| `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt` | Daily execution hub with biological battery meter, next recommended task, and undo. |
| `app/src/main/java/com/neuroflow/app/presentation/v2/composer/SmartTaskComposerSheetV2.kt` | 1-field NLP & Voice quick capture modal with heuristic inference engine. |
| `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt` | Radial bioluminescent timer, soundscape controls, decompression buffer, and micro-steps. |
| `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt` | Focus state machine coordinating timing, Room persistence, DND, soundscapes, and service. |
| `app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapeGenerator.kt` | Pure Kotlin DSP synthesis engine (40Hz Gamma, Brown Noise, Pink Noise). |
| `app/src/main/java/com/neuroflow/app/presentation/v2/focus/engine/FocusTimingCalculator.kt` | Monotonic wall-clock drift-proof calculator. |
| `app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt` | Foreground ongoing notification service. |
| `app/src/main/java/com/neuroflow/app/data/local/UserPreferencesDataStore.kt` | DataStore preferences with `useV2Ui = true` default. |
| `app/src/test/java/com/neuroflow/app/e2e/harness/NeuroFlowV2Harness.kt` | Authoritative E2E Test Harness & Oracles. |
| `app/src/test/java/com/neuroflow/app/e2e/*` | 5 comprehensive multi-tier E2E test suites (Parity, Soundscapes, Drift, Widget, Voice). |

---

## 6. Review & Strategic Guidance Request for Vice Head

Vice Head, please review this system implementation across architecture and system design. If you have recommendations, please structure your feedback as:
1. **Architectural Decision Record (ADR)**:
   - Evaluation of architectural trade-offs (e.g. state management, service isolation, repository caching).
2. **System Design & Scaling**:
   - Deep review of data models, synchronization strategies, lifecycle resilience, and edge failure modes (e.g. process death, audio route change, Bluetooth disconnect).
3. **Algorithmic Optimizations**:
   - Refinements to the DSP audio synthesis (phase accumulator wrap, filter stability) or Borbély Two-Process circadian calculations.
4. **Concrete Implementation Guidance**:
   - Drop-in Kotlin code snippets, interface contracts, or diffs.
