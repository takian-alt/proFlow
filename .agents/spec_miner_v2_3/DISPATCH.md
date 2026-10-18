## 2026-10-16T19:09:14+06:00
You are spec_miner_v2_3, a read-only specification and architecture investigator for the neuroFlow V2 UI modernization project.

Working Directory: /home/takian/Project/PROCUS/.agents/spec_miner_v2_3
Workspace Root: /home/takian/Project/PROCUS
Original Request: /home/takian/Project/PROCUS/.agents/ORIGINAL_REQUEST.md (specifically see Section ## 2026-10-16T13:05:30Z)
Scope: R3. Focus Experience & Autonomic Recovery AND R4. Contextual Integration of Secondary Tools

Objective:
Investigate the Focus experience and classic capability integration across the codebase:
1. Focus Experience (R3):
   - Locate `FocusScreen.kt`, `FocusViewModel.kt`, `FocusService.kt`, or related files.
   - Investigate 4 modes: "Just 5 Minutes" dopamine micro-start, 25m Pomodoro, 90m Ultradian Rhythm, Flow Stopwatch.
   - Investigate Autonomic Reset: 60-second physiological sigh breathing guide (2 quick inhales, 1 slow exhale) during break transitions.
   - Investigate Micro-Step Checklist & Thought Parking Drawer: embedded checklist on focus screen with an instant bottom drawer to park intrusive thoughts.
   - Investigate Post-Session Affordance Rating: 1-tap effort rating (1–5 scale).
   - Investigate System DND: Priority Do Not Disturb policy during active tracking.
2. Secondary Tools & Classic Capabilities Migration (R4):
   - Search the codebase for all classic capabilities mentioned in R4 and Acceptance Criteria:
     * Distraction-free Notes scratchpad (and how to surface it via bottom sheets from Flow and Plan).
     * Chronotype MEQ Quiz and Sleep Log visualizer (and how to link from Circadian Barometer).
     * Ulysses Pre-Commitment Contracts with stakes (and how to connect into Insights and high-resistance tasks).
     * Floating context/tag pills (`#deepwork`, `#offline`, `#admin`) on Flow and Plan screens.
     * Protected slots and any other classic features.
   - Check existing DAOs, entities, tables, and ViewModels for Notes, Sleep, MEQ, Ulysses to ensure 100% backward compatibility and zero data loss.
   - Determine how to bridge/surface them directly into V2 navigation without launching the classic navigation tree.

Deliverables:
Write your structured findings to `/home/takian/Project/PROCUS/.agents/spec_miner_v2_3/handoff.md` with:
- Observation (classes, database entities, DAOs, UI fragments/screens, navigation routes)
- Logic Chain (exact mapping of how R3 and R4 can be implemented cleanly in V2)
- Compatibility Verification (confirming DB tables, migrations, preferences)
- Concrete Implementation Recommendations for the Worker
- Send a completion message back to parent.
