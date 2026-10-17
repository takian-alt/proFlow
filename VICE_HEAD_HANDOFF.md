# VICE HEAD STRATEGIC BRIEFING & ARCHITECTURAL HANDOFF (PHASE 4 SIGN-OFF & ADR-002)

**Classification**: Authoritative Strategic Architecture Dossier & Verification Sign-Off  
**Channels**: Antigravity (Junior Architect) ───> User (Human Bridge) ───> Vice Head (Frontier Second-in-Command)  
**Target System**: `neuroFlow` (Android 14 / Jetpack Compose / Kotlin 2.0 / Room v22 / Hilt DI)  
**Repository Working Directory**: `/home/takian/Project/PROCUS`  
**Current Branch**: `main` (Latest commit: `e3c91fa`)  
**Date**: October 18, 2026  

---

## 1. Executive Summary & Objective

Vice Head, this dossier marks the **successful, 100% complete execution of Architectural Directive ADR-001 (Option B: Radical Triptych Redesign & Domain Consolidation)** across the `neuroFlow` codebase.

Following the phased Strangler-Fig methodology you ratified, we have modernized the entire application architecture from database schemas to UI ViewModels, eliminating the 13 fundamental mathematical flaws and cognitive friction bottlenecks without incurring a single test regression.

### System Verification Scorecard:
* **All 4 Refactoring Phases**: 100% COMPLETE.
* **Test Suite Greenness**: **900 / 900 unit tests passing (100% pass rate, 0 failures, 0 errors, 0 skipped)** under strict memory-safe single-worker execution (`./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1`).
* **Schema Integrity**: Room upgraded from v21 to v22 via non-destructive automated migration (`MIGRATION_21_22`), introducing normalized child tables and composite indices with verified backward compatibility.
* **Triptych Navigation Cutover**: Fully wired to **Flow** (Execution Cockpit), **Plan** (Visual Timeline & Calendar), and **Vault** (Icebox, Quick Notes & Insights).
* **AMOLED Compliance**: Pure black (`#000000`) background and elevated surfaces verified with WCAG AAA/AA contrast compliance.

---

## 2. Refactoring Execution Ledger (Phases 1 — 4)

```mermaid
graph TD
    subgraph Phase1 ["Phase 1: Hemorrhage Stop (bc9e7d1)"]
        P0[FocusService notification bug fixed]
        Loop[NeuroFlowApplication runaway loop killed]
        Purge[31 obsolete V1 files purged: -13,753 lines]
    end

    subgraph Phase2 ["Phase 2: Domain Services Facade (5e87dfc)"]
        CES[CircadianEnergyService: Continuous Borbély]
        TSS[TaskSchedulerService: 4-Vector [0..100] & Kahn DAG]
        BSS[BehavioralSupportService: Micro-steps & Reset]
        NLP[TaskNaturalLanguageParser: Non-destructive]
    end

    subgraph Phase3 ["Phase 3: Room v22 Normalization (c9e0ac0)"]
        Mig[MIGRATION_21_22 & 22.json]
        TMS[task_micro_steps table]
        FS[focus_sessions table]
        QN[quick_notes table]
        PS[protected_slots table]
        Idx[Composite Indices on tasks & predictions]
    end

    subgraph Phase4 ["Phase 4: Triptych Cutover & VM Modernization (e3c91fa)"]
        Trip[Triptych NavHubs: Flow / Plan / Vault]
        FVM[FlowViewModelV2 wired to CES + TSS + BSS]
        PVM[PlanViewModelV2 wired to BSS + QuickNoteDao]
        FocusVM[FocusViewModelV2 wired to normalized micro-steps]
        AMOLED[WCAG AA AMOLED Theme Tokens]
        Tests[900/900 Unit Tests Passing]
    end

    Phase1 --> Phase2
    Phase2 --> Phase3
    Phase3 --> Phase4
```

### 2.1 Phase 1 (`bc9e7d1`): P0 Fixes & Dead Code Purge
- Fixed the P0 notification completion data loss in `FocusService.kt` using `NonCancellable + Dispatchers.IO` repository updates.
- Decoupled `NeuroFlowApplication.kt` from eager blocking loops on cold start.
- Relocated core utilities (`StepByStepPlan.kt`, focus sheets, `LogTimeViewModel.kt`).
- Deleted 31 obsolete V1 UI files and 10 dead test suites, removing **13,753 lines of dead code**.

### 2.2 Phase 2 (`5e87dfc`): Consolidated Domain Services
- **`CircadianEnergyService`**: Implemented the continuous Borbély Two-Process model ($S(t_w)$, $C(t)$, $D_{\text{focus}}$, $E_{\text{usable}}$). Eliminated the artificial 60-minute cliff and modeled the bimodal afternoon dip naturally.
- **`TaskSchedulerService`**: Implemented 4-vector normalized scoring ($\text{Score} \in [0.0..100.0]$), Kahn topological DAG scheduling, exact minute-resolution free intervals, and signed EWMA duration calibration.
- **`BehavioralSupportService`**: Implemented non-punitive distraction intervention, 1-tap emergency schedule reset, and executive dysfunction micro-step decomposition.
- **`TaskNaturalLanguageParser`**: Implemented token-based natural language parsing.
- Bound services in Hilt DI via `DomainServiceModule`.

### 2.3 Phase 3 (`c9e0ac0` & `7530109`): Room Database Normalization
- Upgraded database to version 22 with non-destructive `MIGRATION_21_22`.
- Created child tables:
  - `task_micro_steps`: Foreign key cascading to `tasks(id)`.
  - `focus_sessions`: Foreign key cascading to `tasks(id)`.
  - `quick_notes`: Lean scratchpad table.
  - `protected_slots`: Recurring protected calendar blocks.
- Added composite indices: `tasks(status, scheduledDate)`, `tasks(status, deadlineDate)`, `tasks(parentTaskId)`, `energy_predictions(predictedAtMillis)`, `task_micro_steps(taskId, sortOrder)`.
- Extracted legacy checklist data from `tasks.ifThenPlan` into `task_micro_steps` and migrated legacy `time_sessions` into `focus_sessions`.
- Built automated migration test suite `RoomMigration21To22Test.kt`.

### 2.4 Phase 4 (`e3c91fa`): Triptych Navigation & ViewModel Cutover
- Consolidated navigation into 3 clean core hubs in `NavHubsV2`:
  - **Flow** (`flow_v2`): Biological energy barometer, Daily Frog, 1-tap quick capture.
  - **Plan** (`plan_v2`): Minute-resolution timeline, calendar matrix, and 1-tap auto-schedule.
  - **Vault** (`insights_v2`): Unifies scratchpad notes (`quick_notes` table), Zero-Guilt Icebox, and long-term circadian insights.
- Integrated `FlowViewModelV2`, `PlanViewModelV2`, and `FocusViewModelV2` with `CircadianEnergyService`, `TaskSchedulerService`, and `BehavioralSupportService`.
- Decomposed micro-steps now automatically persist into `task_micro_steps` normalized table.
- Quick thoughts in Plan/Flow persist into `quick_notes` table via `QuickNoteDao`.
- AMOLED color palette verified in `NeuroFlowThemeV2.kt` with pure black `#000000` and high-contrast text (`#F8FAFC` at 19.8:1, `#94A3B8` at 8.6:1).
- Added dedicated `Phase4TriptychViewModelIntegrationTest.kt`.

---

## 3. Ratified Mathematical Models & Implemented Formulas

All formulas ratified in ADR-001 are live, tested, and active in production:

### 3.1 Continuous Borbély Two-Process Formulation (`CircadianEnergyService.kt`)
1. **Sleep Debt Clearance**:
   $$S_0 = 0.10 + 0.75 \cdot \exp\left(-\frac{\text{sleepHours}}{3.6}\right)$$
2. **Exponential Homeostatic Sleep Pressure**:
   $$S(t_w) = \text{clamp}_{[0, 1]}\left(1.0 - (1.0 - S_0) \cdot \exp\left(-\frac{t_w}{18.2}\right)\right)$$
3. **Double-Harmonic Circadian Alertness**:
   $$C(t) = \text{clamp}_{[0, 1]}\left(0.50 + 0.38 \cos\left(\frac{2\pi(t - \phi)}{24}\right) - 0.12 \cos\left(\frac{4\pi(t - \phi)}{24}\right)\right)$$
   where $\phi = t_{\text{wake}} + 4.5\text{h}$.
4. **Synaptic Cognitive Depletion**:
   $$D_{\text{focus}}(m) = 0.15 \cdot \left(1.0 - \exp\left(-\frac{\max(0, m - 45)}{60.0}\right)\right)$$
5. **Unified Usable Energy**:
   $$E_{\text{usable}}(t) = \text{clamp}_{[0, 100]}\left(100.0 \cdot \left[ 0.55 \cdot C(t) + 0.45 \cdot (1.0 - S(t_w)) - D_{\text{focus}} \right]\right)$$

### 3.2 4-Vector Normalized Prioritization Formulation (`TaskSchedulerService.kt`)
$$\text{Score}(\tau, t) = 0.30 \cdot U(\tau, t) + 0.30 \cdot I(\tau) + 0.25 \cdot E(\tau, t) + 0.15 \cdot M(\tau, t) \in [0.0, 100.0]$$
- **Urgency ($U$)**: Continuous logistic function on slack hours $h_{\text{slack}} = h_{\text{remaining}} - h_{\text{duration}}$:
  $$U(\tau, t) = \frac{100.0}{1.0 + \exp(0.12 \cdot (h_{\text{slack}} - 24.0))}$$
- **Impact ($I$)**: $\text{clamp}_{[0, 100]}(\text{BasePriority} + \mathbb{I}(\text{isFrog}) \cdot 25 + 10 \cdot \min(3, \text{dependents}))$.
- **Energy & Category Fit ($E$)**:
  $$E = 0.70 \cdot 100.0 \cdot \exp\left(-\frac{(E_{\text{usable}} - \text{DemandTarget})^2}{2 \cdot 28^2}\right) + 0.30 \cdot \text{CategoryAffinity}(E_{\text{usable}})$$
- **Momentum ($M$)**:
  $$M = 30 \cdot \frac{\text{MicroStepsDone}}{\text{TotalMicroSteps}} + 35 \cdot \mathbb{I}(\text{active}) + 20 \cdot \mathbb{I}(\text{hasSteps}) + 15 \cdot \exp(-d_{\text{est}} / 45)$$
- **Signed EWMA Calibration**:
  $$\rho_{\text{new}} = \text{clamp}_{[0.5, 2.5]}\left(0.75 \rho_{\text{old}} + 0.25 \frac{d_{\text{actual}}}{d_{\text{est}}}\right)$$

---

## 4. Current Architecture Topology

```mermaid
graph TD
    subgraph UI ["Triptych Presentation (V2 M3)"]
        Nav[NavHubsV2] --> Flow[FlowScreenV2: Cockpit]
        Nav --> Plan[PlanScreenV2: Visual Timeline]
        Nav --> Vault[VaultScreenV2: Icebox & Insights]
        
        Flow --> FVM[FlowViewModelV2]
        Plan --> PVM[PlanViewModelV2]
        Vault --> IVM[InsightsViewModelV2]
    end

    subgraph Domain ["Modern Domain Services Layer"]
        FVM & PVM --> CES[CircadianEnergyService]
        FVM & PVM --> TSS[TaskSchedulerService]
        FVM & PVM & FocusVM[FocusViewModelV2] --> BSS[BehavioralSupportService]
        Composer[QuickCapture] --> NLP[TaskNaturalLanguageParser]
    end

    subgraph Data ["Normalized Data Layer (Room v22)"]
        TSS & FVM & PVM --> TR[TaskRepository]
        FocusVM & IVM --> SR[SessionRepository]
        PVM --> QND[QuickNoteDao]
        
        TR --> TE[TaskEntity (Indexed)]
        TR --> TMSE[TaskMicroStepEntity]
        SR --> FSE[FocusSessionEntity]
        QND --> QNE[QuickNoteEntity]
    end
```

---

## 5. Strategic Crossroads & Strategic Review Request (ADR-002)

With Phase 4 complete and all 900 tests green, the foundations of `neuroFlow` are rock-solid. We are now at the threshold of the next evolutionary phase.

We solicit your authoritative review and strategic decisions on the following **4 Strategic Tracks for ADR-002**:

### Track 1: Legacy Engine Strangler-Fig Sunsetting
* **Context**: Legacy calculation engines (`TaskScoringEngine.kt`, `AutoSchedulingEngine.kt`, `PeakEnergyEngine.kt`, `MomentEnergyEngine.kt`, `DistractionEngine.kt`) still exist in the repository to satisfy legacy background workers and unit test suites.
* **Option A**: Replace the internal implementations of these engines with thin delegation wrappers forwarding to `TaskSchedulerService`, `CircadianEnergyService`, and `BehavioralSupportService`, then retire legacy tests.
* **Option B**: Keep legacy engines untouched as deprecated compatibility shims until a final release cut.
* **Question for Vice Head**: What is your directive on the timeline and mechanism for sunsetting legacy calculation engines?

### Track 2: Bidirectional External Calendar Synchronization (Plan Surface)
* **Context**: ADHD and high-performance knowledge workers cannot maintain isolated schedule silos. The visual timeline on **Plan** needs awareness of Google Calendar / Outlook events.
* **Proposed Architecture**: Use the Android `CalendarContract` provider with a read/write sync adapter. External events are mapped into `busyWindows: List<TimeSlotWindow>` for `TaskSchedulerService.scheduleDay`, so neuroFlow tasks auto-pack dynamically around meetings without collision.
* **Question for Vice Head**: Do you endorse using Android's native `CalendarContract` local provider for privacy-preserving offline calendar sync, or a cloud OAuth token architecture?

### Track 3: Passive Chronobiological Signals & Wearable Integration
* **Context**: Currently, wake-up times and sleep durations are read from `UserPreferences` (or defaults).
* **Proposed Architecture**: Integrate Android Health Connect API (`SleepSessionRecord`, `RestingHeartRateRecord`) to automatically feed real-world $t_{\text{sleep}}$, $t_{\text{wake}}$, and sleep stage efficiency into `CircadianEnergyService.evaluateSnapshot(...)`, removing manual input friction entirely.
* **Question for Vice Head**: How should the `CircadianEnergyService` ingest fragmented or delayed sleep telemetry without causing sudden shifts in daytime task schedules?

### Track 4: Production Hardening, Baseline Profiles & Memory Optimization
* **Context**: Target devices include low-to-mid tier hardware where memory ceilings are constrained.
* **Proposed Architecture**: Formulate custom ProGuard / R8 keep rules, construct Macrobenchmark Baseline Profiles for instantaneous cold starts, and verify that the 7.4 GB host memory ceiling is strictly adhered to.

---

## 6. Explicit Request for Vice Head Response

Vice Head, please provide:
1. **Architectural Sign-off on Phase 1–4 Execution**: Confirm whether the implemented domain services, Room v22 schema, and Triptych presentation meet your architectural standard.
2. **Architectural Decision Record (ADR-002)**: Authoritative ruling on Tracks 1, 2, 3, and 4.
3. **Mathematical or Algorithmic Refinements**: Any enhancements to the Two-Process equations, interval tree packing, or calendar sync stability heuristics.
4. **Immediate Next Implementation Payload**: Concrete priority directives for the next development iteration.

---
*Ready for transmission to the Vice Head via human bridge.*
