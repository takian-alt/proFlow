# E2E Test Infra: neuroFlow Modernization

## Test Philosophy
- Opaque-box, requirement-driven testing derived strictly from `ORIGINAL_REQUEST.md` and user acceptance criteria.
- Complete independence from internal private implementation details.
- Systematic 4-tier coverage methodology:
  - **Tier 1**: Feature Coverage (>=5 test cases per feature covering equivalence class representatives)
  - **Tier 2**: Boundary & Corner Cases (>=5 test cases per feature at limit extremes, wrap boundaries, null/unknown inputs)
  - **Tier 3**: Cross-Feature Interactions (pairwise combinatorial tests: energy + inference + scheduling + focus)
  - **Tier 4**: Real-World Application Scenarios (realistic end-to-end workflows representing neurodivergent/ADHD user days)

## Feature Inventory & Test Mapping
| # | Feature | Source | Tier 1 Target | Tier 2 Target | Tier 3 Target |
|---|---------|--------|:-------------:|:-------------:|:-------------:|
| F1 | JUnit 4 & 5 Runner Execution | ORIGINAL_REQUEST §R1 | 5 | 5 | ✓ |
| F2 | Borbély Pre-Peak Baseline Fix | ORIGINAL_REQUEST §R1 | 5 | 5 | ✓ |
| F3 | Circadian Midnight Wrap | ORIGINAL_REQUEST §R1 | 5 | 5 | ✓ |
| F4 | Confidence Factor Decoupling | ORIGINAL_REQUEST §R1 | 5 | 5 | ✓ |
| F5 | Live Energy ↔ Task Scoring Link | ORIGINAL_REQUEST §R1 | 5 | 5 | ✓ |
| F6 | Room TypeConverters Enum Hardening | ORIGINAL_REQUEST §R1 | 5 | 5 | ✓ |
| F7 | Database Migration & Notes Preservation | ORIGINAL_REQUEST §R1 | 5 | 5 | ✓ |
| F8 | Startup Non-Blocking Execution | ORIGINAL_REQUEST §R1 | 5 | 5 | ✓ |
| F9 | Task Inference Engine (Keywords/Regex) | ORIGINAL_REQUEST §R2 | 5 | 5 | ✓ |
| F10 | Quick Task ModalBottomSheet | ORIGINAL_REQUEST §R2 | 5 | 5 | ✓ |
| F11 | Material 3 Light/Dark/AMOLED Contrast | ORIGINAL_REQUEST §R3 | 5 | 5 | ✓ |
| F12 | 48dp Touch Targets & A11y Semantics | ORIGINAL_REQUEST §R3 | 5 | 5 | ✓ |
| F13 | 3-Hub Navigation & Route Cleanliness | ORIGINAL_REQUEST §R3 | 5 | 5 | ✓ |
| F14 | Supportive Focus Tiers (Zen/Shield/Fortress) | ORIGINAL_REQUEST §R4 | 5 | 5 | ✓ |
| F15 | Modular Auto-Scheduler & Deferrals | ORIGINAL_REQUEST §R5 | 5 | 5 | ✓ |
| F16 | Behavioral Pattern Insights | ORIGINAL_REQUEST §R5 | 5 | 5 | ✓ |

## Test Architecture
- **Runner**: `./gradlew testDebugUnitTest`
- **Location**: `app/src/test/java/com/neuroflow/app/e2e/`
- **Output Artifact**: Test suites organized by tier (`Tier1FeatureTest`, `Tier2BoundaryTest`, `Tier3CrossFeatureTest`, `Tier4RealWorldWorkflowTest`).

## Real-World Application Scenarios (Tier 4)
1. **Morning Bootstrapping Workflow**: Fresh app launch, chronotype detection, non-blocking startup, energy curve generation without morning zeroing.
2. **ADHD Quick Capture Blitz**: Ingesting 5 rapid raw-text tasks with varying durations ("15m quick email", "deep coding 90m @work", "!frog urgent report"), auto-inferring metadata.
3. **Daily Auto-Schedule with Energy Alignment**: Auto-scheduler schedules high-energy tasks during circadian peaks, surfaces clear placement rationales, and provides actionable deferral recommendations for overloaded slots.
4. **Distraction-Free HyperFocus Session**: Activating Focus mode on a frog task, selecting Shield/Zen tier, experiencing supportive reframing upon distraction attempt, and logging session completion.
5. **Night-Owl Circadian Rollover & Migration Persistence**: Simulating 11:30 PM to 1:30 AM workflow for evening chronotypes across midnight, performing database schema verification with complete notes preservation.

## Coverage Goals
- Total Minimum Tests: >= 180 test cases across Tiers 1-4.
- Test Suite Gate: 100% pass rate before Phase 2 Tier 5 adversarial hardening.
