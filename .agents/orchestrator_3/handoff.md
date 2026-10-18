# Orchestrator Soft Handoff — Orchestrator 3 to Successor (Gen 1)

**Target Workspace**: `/home/takian/Project/PROCUS/.agents/orchestrator_3`  
**Project Root**: `/home/takian/Project/PROCUS`  
**Parent Conversation ID**: `d445b7c2-bfb2-45b7-9afe-f5cbedd274d5` (Sentinel)  
**Timestamp**: 2026-10-16T17:15:30Z  

---

## 1. Milestone State

| Milestone | Scope / Requirements | Status | Notes |
|-----------|----------------------|--------|-------|
| **M1: 100% V1 Feature Parity & Navigation Connectivity** | R1: Notes Keep-style, Weekly Schedule, Deep Analytics (7d/30d velocity, 24h circadian heatmap, sMAPE), all 5 settings routes, Identity affirmations, 1-tap WOOP anchor sheet, Retrospective time logging | **DONE** (PASS) | Gate PASSED. Verified by 2 Reviewers, 2 Challengers, and Forensic Auditor (CLEAN). Zero regressions, 11 routes wired. |
| **E2E Testing Track** | Requirements-driven test suite across Tiers 1–4 (Category-Partition, BVA, Pairwise, Workload) | **DONE** | Published `/home/takian/Project/PROCUS/TEST_READY.md`. 5 test suites under `app/src/test/java/com/neuroflow/app/e2e/`, 864+ tests passing. |
| **M2: Neural Soundscapes & Auditory Focus Stimulation** | R2: Pure Kotlin DSP (40Hz Gamma binaural, Brown Noise 1/f², Pink Noise / Rain Paul Kellet 1/f), tactile volume slider (min 48dp), fade-out on pause/stop, leak-free AudioTrack | **IN_REMEDIATION** | Iteration 1 evaluated: Auditor CLEAN, Challenger 1 APPROVE. Reviewers requested 2 critical fixes: `Float.NaN` volume poisoning & zombie thread without `finally`. Ready for `worker_v2_m2_fix`. |
| **M3: Drift-Proof Background Focus & Live Notification** | R3: Wall-clock timing engine (`startedAtMillis`, `accumulatedPauseMillis`, `pausedAtMillis`), ongoing `FocusService` with countdown progress bar, task title, 1-tap Pause/Resume/Complete actions | **PLANNED** | Ready for dispatch immediately after M2 gate passes. |
| **M4: Default V2 UI & Widget Interoperability** | R4: `useV2Ui = true` default in `UserPreferencesDataStore.kt:165,416`, `MainActivity` intent routing (`ACTION_OPEN_NEW_TASK`, `ACTION_OPEN_FOCUS`), home screen widget providers wired to V2 | **PLANNED** | Ready for dispatch after M3. |
| **M5: Hands-Free Voice Quick-Capture & Executive Transition Buffer** | R5: 1-tap mic speech recognition in `SmartTaskComposerSheetV2` -> `TaskInferenceEngine`, 3-min Decompression Zone with 60s physiological sigh guide and "Ready Now" bypass | **PLANNED** | Ready for dispatch after M4. |
| **M6: Full Verification, E2E Testing & Coverage Hardening** | Feature 16: Full `./gradlew testDebugUnitTest`, memory bound checks, adversarial verification, victory forensic audit | **PLANNED** | Final victory sign-off before parent reporting. |

---

## 2. Active Subagents
- **Currently Active**: None. All 18 previously spawned subagents (explorers, workers, reviewers, challengers, auditors) have finished their tasks and delivered reports.
- **Cumulative Spawn Count**: 18 / 16 (Succession trigger condition met).

---

## 3. Pending Decisions & Immediate Context
- **No Blockers or Ambiguities**. All requirements R1–R5 are fully specified in `/home/takian/Project/PROCUS/.agents/ORIGINAL_REQUEST.md` (§`## 2026-10-16T15:15:13Z`) and broken down in `/home/takian/Project/PROCUS/.agents/orchestrator_3/PROJECT.md`.
- **Milestone 2 Exact Remediation Specifications**:
  1. `app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapeGenerator.kt`:
     - Line 89: `val safeVolume = if (volumeMultiplier.isNaN()) 0f else volumeMultiplier`
     - Line 89: `val targetVol = safeVolume.coerceIn(0f, 1f)`
     - Line 90: `val startVol = if (currentVolume.isNaN()) 0f else currentVolume`
     - Line 163 / `setVolumeInstant`: `currentVolume = if (volume.isNaN()) 0f else volume.coerceIn(0f, 1f)`
  2. `app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapePlayer.kt`:
     - Line 118: `currentVolume = if (volume.isNaN()) 0f else volume.coerceIn(0f, 1f)`
     - Lines 206–253: In `startPlaybackThread()`, wrap thread body in `try-finally`:
       ```kotlin
       playbackThread = thread(name = "NeuralSoundscapeAudio", isDaemon = true) {
           try {
               // streaming loop ...
           } finally {
               isPlaying.set(false)
               isFadingOut.set(false)
               abandonAudioFocus()
               track?.stop()
           }
       }
       ```
  3. `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt:251`:
     - Add `.horizontalScroll(rememberScrollState())` to the soundscape chips `Row`.
  4. `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt:214-229`:
     - In `tickSecond()`, when `newRemaining == 0L` and mode != Stopwatch, invoke `soundscapePlayer.pauseWithFadeOut()`.

---

## 4. Remaining Work (Step-by-Step for Successor)

### Step 1: Remediate Milestone 2 & Close Gate
1. Spawn `worker_v2_m2_fix` (`teamwork_preview_worker`) with exclusive write ownership of:
   - `NeuralSoundscapeGenerator.kt`
   - `NeuralSoundscapePlayer.kt`
   - `FocusScreenV2.kt`
   - `FocusViewModelV2.kt`
   Instruct worker to apply the 4 fixes above and run `./gradlew testDebugUnitTest --tests "*NeuralSoundscape*" --no-daemon -Dorg.gradle.workers.max=1`.
2. Spawn `reviewer_v2_m2_it2_2` (`teamwork_preview_reviewer`) to verify fixes and ensure `testVolumeBoundaryFuzzing_generator_extremeValuesAndNan` passes.
3. Upon APPROVE from reviewer, mark Gate Result: **PASS** in `GATE_STATUS.md` and mark M2 **DONE** in `PROJECT.md` and `progress.md`.

### Step 2: Implement Milestone 3 (Drift-Proof Background Focus & Live Notification)
1. Spawn `worker_v2_m3` (`teamwork_preview_worker`):
   - Create `app/src/main/java/com/neuroflow/app/presentation/v2/focus/engine/FocusTimingCalculator.kt` with pure functions using wall-clock epoch timestamps (`startedAtMillis`, `accumulatedPauseMillis`, `pausedAtMillis`, `nowMillis`, `targetDurationSeconds`).
   - Wire `FocusViewModelV2.kt` to calculate elapsed/remaining time from `FocusTimingCalculator` and restore any open session on resume via `sessionRepository.getOpenSessionForTask(taskId)`.
   - Implement `app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt`:
     - Channel `"focus_live"`, `IMPORTANCE_LOW`, `VISIBILITY_PUBLIC`.
     - Live progress countdown (`.setProgress(total, elapsed, false)`), task title, and PendingIntents for "Pause", "Resume", "Complete", and content intent opening `MainActivity` with `ACTION_OPEN_FOCUS` and `taskId`.
     - Register `FocusService` in `AndroidManifest.xml` with foreground service types `specialUse|mediaPlayback`.
   - Run tests: `./gradlew testDebugUnitTest --tests "*FocusDrift*" --tests "*Focus*" --no-daemon -Dorg.gradle.workers.max=1`.
2. Dispatch 2 Reviewers, 2 Challengers, and Forensic Auditor for M3. Close Gate.

### Step 3: Implement Milestone 4 (Default V2 UI & Widget Interoperability)
1. Spawn `worker_v2_m4` (`teamwork_preview_worker`):
   - In `UserPreferencesDataStore.kt:165,416`, set `useV2Ui = true` by default.
   - In `MainActivity.kt`, forward `initialNewTaskOpen` to `NeuroFlowAppV2` and `FlowScreenV2` to directly trigger `SmartTaskComposerSheetV2`.
   - In `MainActivity.kt`, if `ACTION_OPEN_FOCUS` lacks `taskId`, asynchronously fetch the top active task ID before navigating to `focus_v2/{taskId}`.
   - In `FocusWidgetUpdateWorker.kt`, assign `ACTION_OPEN_NEW_TASK` pending intent when `topTask == null` so tapping an empty widget launches the V2 composer.
   - Run tests: `./gradlew testDebugUnitTest --tests "*Widget*" --no-daemon -Dorg.gradle.workers.max=1`.
2. Dispatch Reviewers, Challengers, Auditor for M4. Close Gate.

### Step 4: Implement Milestone 5 (Voice Quick-Capture & Executive Transition Buffer)
1. Spawn `worker_v2_m5` (`teamwork_preview_worker`):
   - In `SmartTaskComposerSheetV2.kt`, add 1-tap mic button using `rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())` launching `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` guarded with `try-catch(ActivityNotFoundException)`.
   - Pipe transcribed speech directly to `rawInput`, triggering `TaskInferenceEngine.inferFromInput()` to auto-populate title, duration, energy demand, frog priority, and tags.
   - Declare `<queries><intent><action android:name="android.speech.RecognitionService"/></intent></queries>` in `AndroidManifest.xml`.
   - In `FocusViewModelV2.kt` and `FocusScreenV2.kt`, implement the 3-minute (180s) Decompression Zone state machine and calm overlay upon task completion, with 60s physiological sigh guide action and "Ready Now" bypass button.
   - Run tests: `./gradlew testDebugUnitTest --tests "*Voice*" --no-daemon -Dorg.gradle.workers.max=1`.
2. Dispatch Reviewers, Challengers, Auditor for M5. Close Gate.

### Step 5: Full Verification & Victory Audit (Milestone 6)
1. Run `./gradlew testDebugUnitTest --rerun-tasks --no-daemon -Dorg.gradle.workers.max=1`.
2. Verify memory limits in `gradle.properties` (0 OOM errors).
3. Dispatch Final Victory Auditor (`teamwork_preview_auditor`) for universal forensic integrity audit across all milestones.
4. Send final completion handoff report to Sentinel parent (`d445b7c2-bfb2-45b7-9afe-f5cbedd274d5`).

---

## 5. Key Artifacts
- `/home/takian/Project/PROCUS/.agents/ORIGINAL_REQUEST.md` — Authoritative User Request
- `/home/takian/Project/PROCUS/.agents/orchestrator_3/PROJECT.md` — Master Architecture & Feature Inventory
- `/home/takian/Project/PROCUS/.agents/orchestrator_3/TEST_INFRA.md` — Test methodology & coverage plan
- `/home/takian/Project/PROCUS/TEST_READY.md` — E2E test suite ready signal & checklist
- `/home/takian/Project/PROCUS/.agents/orchestrator_3/GATE_STATUS.md` — Gate verdicts & history
- `/home/takian/Project/PROCUS/.agents/orchestrator_3/progress.md` — Milestone progress tracking
- `/home/takian/Project/PROCUS/.agents/orchestrator_3/BRIEFING.md` — Persistent briefing
- `/home/takian/Project/PROCUS/.agents/orchestrator_3/DISPATCH.md` — Dispatch record
