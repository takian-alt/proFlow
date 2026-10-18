# Dispatch: worker_v2_m3_verify — Milestone 3 Test Verification & Completion

## Mission
Verify, test, and finalize Milestone 3 (R3: Drift-Proof Background Focus & Lock-Screen Live Notification).
Predecessor worker `worker_v2_m3` implemented `FocusTimingCalculator.kt`, `FocusService.kt`, `FocusViewModelV2.kt`, `AndroidManifest.xml`, and `FocusTimingCalculatorTest.kt`.
Your job is to run the unit and E2E test suites for Focus and the full project, verify that all requirements from ORIGINAL_REQUEST.md and PROJECT.md are met with 0 test failures and 0 regressions, resolve any compilation or test issues if found, and produce a comprehensive `handoff.md`.

## Context & Scope
- Working directory: `/home/takian/Project/PROCUS/.agents/worker_v2_m3_verify`
- Project root: `/home/takian/Project/PROCUS`
- Authoritative Request: `/home/takian/Project/PROCUS/.agents/ORIGINAL_REQUEST.md` (read `## 2026-10-16T15:15:13Z`, section R3)
- Master Scope: `/home/takian/Project/PROCUS/.agents/orchestrator_4/PROJECT.md`
- Predecessor worker workspace: `/home/takian/Project/PROCUS/.agents/worker_v2_m3`

## Exclusive Write Ownership
- `app/src/main/java/com/neuroflow/app/presentation/v2/focus/engine/FocusTimingCalculator.kt`
- `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt`
- `app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/test/java/com/neuroflow/app/presentation/v2/focus/engine/FocusTimingCalculatorTest.kt`
- `app/src/test/java/com/neuroflow/app/e2e/NeuroFlowV2FocusDriftE2eTest.kt` (if needed for M3 tests)
- Agent metadata in `/home/takian/Project/PROCUS/.agents/worker_v2_m3_verify/`

## Mandatory Integrity Warning
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Tasks
1. Run `./gradlew testDebugUnitTest --tests "*Focus*" --no-daemon -Dorg.gradle.workers.max=1` to check current Focus test suite status.
2. If any tests fail, inspect failure logs and fix them cleanly and genuinely without breaking existing functionality.
3. Ensure `FocusTimingCalculator.kt` accurately handles:
   - Wall-clock epoch timing: `elapsedSeconds = (effectiveElapsedMillis) / 1000L`
   - Pause accumulation: `accumulatedPauseMillis` tracks all elapsed pause time
   - Doze / screen-off time jumps: when `nowMillis` jumps by hours, elapsed time advances accurately
   - Target expiration: `isExpired = (!isStopwatch) && (elapsedSeconds >= targetDurationSeconds)`
4. Ensure `FocusService.kt`:
   - Notification channel `"focus_live"`, `IMPORTANCE_LOW`, `VISIBILITY_PUBLIC`
   - Shows countdown progress bar (`setProgress(total, elapsed, false)`), task title
   - Action PendingIntents for "Pause", "Resume", "Complete"
   - Content PendingIntent launching `MainActivity` with action `com.procus.ACTION_OPEN_FOCUS` and `taskId`
   - Manifest declares foregroundServiceType `specialUse|mediaPlayback` (or required types)
5. Run full test suite: `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1` and ensure 0 failures.
6. Write `handoff.md` in `/home/takian/Project/PROCUS/.agents/worker_v2_m3_verify/handoff.md` and report completion via `send_message`.

## 2026-10-16T19:12:16Z
You are worker_v2_m3_verify.
Your working directory is: /home/takian/Project/PROCUS/.agents/worker_v2_m3_verify
Your dispatch instructions are at: /home/takian/Project/PROCUS/.agents/worker_v2_m3_verify/DISPATCH.md
Read /home/takian/Project/PROCUS/.agents/ORIGINAL_REQUEST.md (read ## 2026-10-16T15:15:13Z section R3) and /home/takian/Project/PROCUS/.agents/orchestrator_4/PROJECT.md.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Execute the tasks in DISPATCH.md:
1. Run `./gradlew testDebugUnitTest --tests "*Focus*" --no-daemon -Dorg.gradle.workers.max=1` to test the implemented FocusTimingCalculator, FocusService, and FocusViewModelV2.
2. If any test fails, fix it cleanly with genuine logic.
3. Run the full unit test suite `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1` and ensure 0 failures and 0 regressions.
4. Maintain progress.md with timestamps.
5. Write your comprehensive handoff.md in /home/takian/Project/PROCUS/.agents/worker_v2_m3_verify/handoff.md.
6. When complete, call send_message to report your results to parent (conversation ID cdb427d8-8541-4ba5-a5e6-350149302a53).
