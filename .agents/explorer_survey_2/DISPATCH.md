## 2026-10-15T21:11:58Z
You are the UI and Architecture Explorer for the neuroFlow modernization project.
Your assigned working directory is: /home/takian/Project/PROCUS/.agents/explorer_survey_2
The project root directory is: /home/takian/Project/PROCUS
The authoritative user request is at: /home/takian/Project/PROCUS/.agents/ORIGINAL_REQUEST.md

OBJECTIVE:
Perform a deep survey and code reconnaissance of the existing codebase focusing on Requirements R2 & R3:
1. Task Creation & Inference (R2):
   - Locate current task creation UI (monolithic dialogs, screens, viewmodels).
   - Locate any existing task inference or heuristic logic.
   - Identify how to implement a single-field quick task capture (auto-inferring task type, duration, energy demands, context tags via lightweight keywords/heuristics).
   - Plan conversion of dialogs to a streamlined ModalBottomSheet with progressive disclosure.
2. Design System & Theming (R3):
   - Locate Material 3 theme definitions, color schemes, typography, shapes.
   - Analyze Light, Dark, and AMOLED modes.
   - Identify all hardcoded contrast-breaking colors (especially in FocusScreen, Today/Flow screens, cards, pastel backgrounds).
3. Accessibility (R3):
   - Check touch target dimensions across all interactive elements (minimum 48x48dp).
   - Check semantic roles, content descriptions, and accessibility compliance.
4. Navigation (R3):
   - Locate navigation graphs, routes, bottom navigation / drawer code.
   - Identify legacy dead drawer code and orphan routes.
   - Plan consolidation into 3 clean core hubs: Flow, Plan, Insights.

SCOPE BOUNDARIES:
- Read-only analysis. Do NOT modify source code or tests.
- Do NOT make code changes.

OUTPUT REQUIREMENTS:
- Maintain your progress in `/home/takian/Project/PROCUS/.agents/explorer_survey_2/progress.md`.
- Produce a comprehensive survey report and handoff at `/home/takian/Project/PROCUS/.agents/explorer_survey_2/handoff.md` detailing:
  - Exact file paths, UI components, composables, viewmodels, theme files.
  - Hardcoded colors found and recommended token replacements.
  - Navigation refactoring plan and legacy code to remove.
  - Touch target accessibility audits.
  - Quick task capture architecture and heuristic rules.
- Once done, send a message back to parent orchestrator referencing your handoff file.
