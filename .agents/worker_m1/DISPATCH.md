## 2026-10-15T21:26:50Z

You are the Core Engineering Worker (Milestone 1) for the neuroFlow modernization project.
Your assigned working directory is: /home/takian/Project/PROCUS/.agents/worker_m1
The project root directory is: /home/takian/Project/PROCUS
The authoritative user request is at: /home/takian/Project/PROCUS/.agents/ORIGINAL_REQUEST.md
The project master plan is at: /home/takian/Project/PROCUS/PROJECT.md
The deep reconnaissance report with exact code references and blueprints is at: /home/takian/Project/PROCUS/.agents/explorer_survey_1/handoff.md

DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

OBJECTIVE:
Execute and implement all fixes for Milestone 1 (Requirement R1 and related acceptance criteria):
1. **Gradle Build & Test Runner Configuration**:
   - Update `app/build.gradle.kts` to add `testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.8.2")`.
   - Verify that `./gradlew testDebugUnitTest` discovers and runs all JUnit 4 test suites (e.g. `AbstentionRateTrend7dTest`, `AbstentionReasonMixTest`, `EnergyInsightTest`, `TimeUtilsTest`, `SleepPressureRefreshWorkerTest`) without skipping.
2. **Math & Energy Engines (Borbély Two-Process & Peak Energy)**:
   - In `PeakEnergyEngine.kt` and `EnergyScoreEngine.kt`, fix pre-peak morning hour zeroing. Calculate decay from wake time or continuous baseline curve rather than treating morning pre-peak hours as 21.5h past yesterday's peak.
   - In `EnergyScoreEngine.kt` (`peakWindow`), support circadian midnight wrap across day boundaries (modulo 1440 arithmetic) for evening chronotypes.
   - In `EnergyScoreEngine.kt`, decouple statistical confidence factor from baseline physical energy magnitude. Confidence should modulate uncertainty or weight against defaults, not zero out physical energy.
   - Connect live energy state directly to `TaskScoringEngine.score` and `AutoSchedulingEngine`. Support passing `LiveEnergyContext` so task scoring dynamically matches cognitive demand to live physiological energy.
3. **Database & Room Persistence**:
   - In `Converters.kt`, harden all enum converters against unknown enum deserialization (`Quadrant`, `Priority`, `TaskStatus`, `Recurrence`, `EnergyLevel`, `ContractOutcome`, `RewardTier`) with safe try-catch fallbacks and error logging.
   - In `NeuroFlowDatabase.kt`, create `app/schemas/com.neuroflow.app.data.local.NeuroFlowDatabase/12.json` schema, verify migration 11->12 operates cleanly without error.
   - Fix destructive migrations (such as 18->19 and 17->19) so `notes` table and user notes data are never dropped or destroyed.
4. **Application Startup Non-Blocking**:
   - In `NeuroFlowApplication.kt`, eliminate `runBlocking` and main-thread disk I/O in `onCreate()`. Offload marker file checks and `databaseCleaner.enforceRestoreVersionPolicy` to a background coroutine (`Dispatchers.IO`).
5. **Testing & Verification**:
   - Run `./gradlew testDebugUnitTest` and confirm that all unit test suites execute and pass.
   - Write comprehensive unit tests for the energy engine fixes, enum hardening, and live energy scoring in `app/src/test/java/com/neuroflow/app/domain/engine/` and `app/src/test/java/com/neuroflow/app/data/local/`.

FILE OWNERSHIP:
You exclusively own:
- `app/build.gradle.kts`
- `app/src/main/java/com/neuroflow/app/domain/engine/PeakEnergyEngine.kt`
- `app/src/main/java/com/neuroflow/app/domain/engine/EnergyScoreEngine.kt`
- `app/src/main/java/com/neuroflow/app/domain/engine/TaskScoringEngine.kt`
- `app/src/main/java/com/neuroflow/app/data/local/Converters.kt`
- `app/src/main/java/com/neuroflow/app/data/local/NeuroFlowDatabase.kt`
- `app/schemas/com.neuroflow.app.data.local.NeuroFlowDatabase/12.json`
- `app/src/main/java/com/neuroflow/app/NeuroFlowApplication.kt`
- Unit tests under `app/src/test/java/com/neuroflow/app/domain/engine/` and `app/src/test/java/com/neuroflow/app/data/local/`
