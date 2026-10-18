## 2026-10-16T15:33:34Z
You are a test writer implementing the opaque-box E2E Testing Track for neuroFlow V2 Parity & Neuro-Capabilities.

Your working directory is: `/home/takian/Project/PROCUS/.agents/test_writer_e2e_1`
The project root is: `/home/takian/Project/PROCUS`
Authoritative User Request: `/home/takian/Project/PROCUS/.agents/ORIGINAL_REQUEST.md` (You MUST read this file, especially section `## 2026-10-16T15:15:13Z`).
Master Plan: `/home/takian/Project/PROCUS/.agents/orchestrator_3/PROJECT.md`
Test Infra: `/home/takian/Project/PROCUS/.agents/orchestrator_3/TEST_INFRA.md`

Your exclusive write boundaries:
- `app/src/test/java/com/neuroflow/app/e2e/` (test files only; NEVER edit implementation code).
- `/home/takian/Project/PROCUS/TEST_READY.md` (publish when test suite is complete).

Task:
Design and implement comprehensive requirement-driven test cases (Tiers 1-4):
1. `NeuroFlowV2ParityE2eTest.kt`:
   - Full Notes System navigation and dark theme styling
   - Weekly schedule 7-day calendar navigation from Plan
   - Insights analytics (velocity, 24-hr heatmap density, sMAPE)
   - Settings routes (priority weights, account sync, hyperfocus allowlist, privacy permissions, app guide)
   - Identity affirmations & empty WOOP anchor sheet
   - Retrospective time logging action and state update
2. `NeuroFlowV2SoundscapeE2eTest.kt`:
   - 40Hz Gamma binaural audio DSP synthesis (200Hz left / 240Hz right)
   - Deep Brown Noise stochastic resonance random walk
   - Flow Pink Noise Paul Kellet 1/f filter approximation
   - Volume slider scaling (0.0 to 1.0) and frame-interpolated fade-out to 0
3. `NeuroFlowV2FocusDriftE2eTest.kt`:
   - Wall-clock timing engine (`startedAtMillis`, `accumulatedPauseMillis`, `pausedAtMillis`)
   - Drift-proof calculation across simulated Doze mode / screen-off time jumps
   - Single and multiple pause accumulation
   - Stopwatch mode vs Pomodoro mode
4. `NeuroFlowV2WidgetInteropE2eTest.kt`:
   - `useV2Ui = true` default in `UserPreferencesDataStore`
   - `ACTION_OPEN_NEW_TASK` and `ACTION_OPEN_FOCUS` intent handling
   - Widget pending intent targets
5. `NeuroFlowV2VoiceAndDecompressionE2eTest.kt`:
   - Speech transcription string piped into `TaskInferenceEngine.inferFromInput()`
   - 3-minute (180s) Decompression Zone countdown, sigh guide action, and "Ready Now" bypass

Verify all tests pass with `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1`.
When tests are complete and passing, publish `/home/takian/Project/PROCUS/TEST_READY.md`.
Write full report to `/home/takian/Project/PROCUS/.agents/test_writer_e2e_1/handoff.md` and message parent when complete.
