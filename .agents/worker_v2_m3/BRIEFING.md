# BRIEFING — 2026-10-16T17:39:17Z

## Mission
Implement drift-proof wall-clock timing and foreground lock-screen live notification service for neuroFlow V2 (Milestone 3 / R3).

## 🔒 My Identity
- Archetype: implementer, qa, specialist
- Roles: implementer, qa, specialist
- Working directory: /home/takian/Project/PROCUS/.agents/worker_v2_m3
- Original parent: f4f30ac2-dcff-4c62-ab0b-f13bcfa63ad1
- Milestone: Milestone 3 (R3 - Drift-Proof Background Focus & Lock-Screen Live Notification)

## 🔒 Key Constraints
- Exclusive write ownership:
  - `app/src/main/java/com/neuroflow/app/presentation/v2/focus/engine/FocusTimingCalculator.kt`
  - `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt`
  - `app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt`
  - `app/src/main/AndroidManifest.xml`
  - `app/src/test/java/com/neuroflow/app/presentation/v2/focus/engine/FocusTimingCalculatorTest.kt`
  - Metadata in `/home/takian/Project/PROCUS/.agents/worker_v2_m3/`
- DO NOT CHEAT. Genuine implementation only. No hardcoded test outputs or dummy facades.
- All tests must pass: 100% test pass rate with 0 failures and 0 regressions.
- Minimal change principle.

## Current Parent
- Conversation ID: f4f30ac2-dcff-4c62-ab0b-f13bcfa63ad1
- Updated: not yet

## Task Summary
- **What to build**:
  1. `FocusTimingCalculator.kt`: Pure Kotlin timing calculation mirroring `FocusTimingOracle`.
  2. Re-architect `FocusViewModelV2.kt`: Integrate wall-clock timestamp-based timing calculation and `FocusService` lifecycle triggers.
  3. `FocusService.kt`: Android Foreground Service with ongoing live notification, actions, ticker, defensive headless execution.
  4. Register `FocusService` in `AndroidManifest.xml` with `specialUse` foregroundServiceType and property.
  5. `FocusTimingCalculatorTest.kt`: Unit tests for timing calculator.
- **Success criteria**:
  - `FocusTimingCalculatorTest` passes covering all edge cases (start, progression, pause, resume, Doze time jump, expiration).
  - `NeuroFlowV2FocusDriftE2eTest` passes without failures.
  - Full suite `./gradlew testDebugUnitTest` passes.
- **Interface contracts**: `/home/takian/Project/PROCUS/.agents/orchestrator_3/PROJECT.md`
- **Code layout**: Standard project structure in `app/src/main/java/...` and `app/src/test/java/...`

## Key Decisions Made
- [TBD]

## Artifact Index
- `.agents/worker_v2_m3/DISPATCH.md` — assignment dispatch
- `.agents/worker_v2_m3/BRIEFING.md` — persistent memory
- `.agents/worker_v2_m3/progress.md` — liveness heartbeat and progress
- `.agents/worker_v2_m3/handoff.md` — completion report

## Change Tracker
- **Files modified**: none yet
- **Build status**: pending
- **Pending issues**: none

## Quality Status
- **Build/test result**: pending
- **Lint status**: pending
- **Tests added/modified**: pending

## Loaded Skills
- None requested in dispatch
