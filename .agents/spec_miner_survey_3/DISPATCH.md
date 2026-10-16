## 2026-10-15T21:12:30Z

<USER_REQUEST>
You are the Focus and Scheduling Spec Miner for the neuroFlow modernization project.
Your assigned working directory is: /home/takian/Project/PROCUS/.agents/spec_miner_survey_3
The project root directory is: /home/takian/Project/PROCUS
The authoritative user request is at: /home/takian/Project/PROCUS/.agents/ORIGINAL_REQUEST.md

OBJECTIVE:
Perform a rigorous specification and behavioral mining of the existing codebase and requirements focusing on R4 & R5:
1. Focus Mode & HyperFocus (R4):
   - Locate current Focus mode implementation (FocusScreen, timers, Pomodoro/custom timer logic, background services).
   - Analyze WOOP (Wish, Outcome, Obstacle, Plan) / implementation intentions implementation and affordance difficulty feedback.
   - Analyze app blocking / distraction intervention logic: identify current punitive/hostile copy and design the three supportive tiers: Zen, Shield, Fortress.
2. Auto-Scheduling Engine (R5):
   - Locate auto-scheduling algorithms and services.
   - Map out current scheduling pipeline: why does it drop tasks silently?
   - Design modular components for placement rationales (why a task was scheduled at a specific slot) and actionable deferral suggestions (why a task could not be scheduled, e.g., energy mismatch, time conflict, buffer requirements) instead of silent drops.
3. Behavioral Pattern Insights (R5):
   - Locate analytics, productivity tracking, and history tracking.
   - Identify how behavioral pattern insights (productivity peaks, estimation errors) can adaptively inform scheduling and prioritization.
4. Edge Cases & Acceptance Criteria:
   - Map all edge cases, test vectors, and invariants needed for testing R4 and R5.

SCOPE BOUNDARIES:
- Read-only analysis and specification mining. Do NOT modify source code or tests.
- Do NOT make code changes.

OUTPUT REQUIREMENTS:
- Maintain your progress in `/home/takian/Project/PROCUS/.agents/spec_miner_survey_3/progress.md`.
- Produce a comprehensive specification report and handoff at `/home/takian/Project/PROCUS/.agents/spec_miner_survey_3/handoff.md` detailing:
  - Algorithmic specifications and data structures for scheduling rationale & deferrals.
  - Focus mode architecture, tier specifications (Zen, Shield, Fortress), supportive copy matrix.
  - Behavioral insights calculation formulas.
  - Exact file locations and proposed module boundaries.
- Once done, send a message back to parent orchestrator referencing your handoff file.
</USER_REQUEST>
