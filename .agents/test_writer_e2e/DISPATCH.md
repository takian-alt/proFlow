## 2026-10-15T21:26:50Z

You are the E2E Test Writer for the neuroFlow modernization project.
Your assigned working directory is: /home/takian/Project/PROCUS/.agents/test_writer_e2e
The project root directory is: /home/takian/Project/PROCUS
The authoritative user request is at: /home/takian/Project/PROCUS/.agents/ORIGINAL_REQUEST.md
The project master plan is at: /home/takian/Project/PROCUS/PROJECT.md
The E2E test infra blueprint is at: /home/takian/Project/PROCUS/TEST_INFRA.md

DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

OBJECTIVE:
Build the comprehensive 4-tier opaque-box E2E test suite based on requirements in ORIGINAL_REQUEST.md and TEST_INFRA.md:
1. **Directory**: Create tests in `app/src/test/java/com/neuroflow/app/e2e/`.
2. **Tier 1 - Feature Coverage (>=5 test cases per feature)**:
   - F1: JUnit 4 & 5 test runners execution
   - F2: Borbély energy baseline continuous pre-peak calculation (no morning zeroing)
   - F3: Circadian midnight wrap for evening chronotypes (crossing 24:00/00:00)
   - F4: Statistical confidence decoupling from energy magnitude
   - F5: Live energy state connection to task scoring
   - F6: Room TypeConverters hardening against unknown/corrupt enum values
   - F7: Database migration 11->12 and notes data preservation
   - F8: Application startup without UI thread disk I/O or runBlocking
   - F9: TaskInferenceEngine keyword & regex parsing (duration, energy, type, tags, frog)
   - F10: QuickTaskModalBottomSheet creation flow
   - F11: Material 3 contrast & pure AMOLED background enforcement
   - F12: 48dp minimum touch target dimensions & semantic roles
   - F13: Navigation 3 core hubs (Flow, Plan, Insights) & dead drawer absence
   - F14: Focus mode supportive tiers (Zen, Shield, Fortress) & reframing copy
   - F15: Modular auto-scheduling with placement rationales & actionable deferrals (no silent drops)
   - F16: Behavioral pattern insights (empirical circadian density & duration calibration)
3. **Tier 2 - Boundary & Corner Cases (>=5 test cases per feature)**:
   - Extreme inputs, wrap boundaries (minute 1439 -> 0), empty titles, invalid tags, maximum values, missing preferences, zero durations.
4. **Tier 3 - Cross-Feature Combinations**:
   - Pairwise interactions between energy levels, task inference, scheduling rationales, and focus mode.
5. **Tier 4 - Real-World Application Scenarios (the 5 scenarios from TEST_INFRA.md)**:
   - Morning Bootstrapping Workflow
   - ADHD Quick Capture Blitz
   - Daily Auto-Schedule with Energy Alignment
   - Distraction-Free HyperFocus Session
   - Night-Owl Circadian Rollover & Migration Persistence
6. **Publication**:
   - When test suites are ready, verify they compile with `./gradlew testDebugUnitTest`.
   - Publish `TEST_READY.md` at `/home/takian/Project/PROCUS/TEST_READY.md` containing runner command and coverage breakdown.

FILE OWNERSHIP:
You exclusively own:
- `app/src/test/java/com/neuroflow/app/e2e/`
- `/home/takian/Project/PROCUS/TEST_READY.md`

OUTPUT REQUIREMENTS:
- Maintain progress in `/home/takian/Project/PROCUS/.agents/test_writer_e2e/progress.md`.
- Produce completion handoff report in `/home/takian/Project/PROCUS/.agents/test_writer_e2e/handoff.md`.
- Once done, send a message back to parent orchestrator.
