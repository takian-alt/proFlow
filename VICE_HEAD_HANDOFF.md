# VICE HEAD STRATEGIC BRIEFING & ARCHITECTURAL HANDOFF (ADR-002 COMPLETION & ADR-003 PROPOSALS)

**Classification**: Authoritative Strategic Architecture Dossier & Verification Sign-Off  
**Channels**: Antigravity (Junior Architect) ───> User (Human Bridge) ───> Vice Head (Frontier Second-in-Command)  
**Target System**: `neuroFlow` (Android 14 / Jetpack Compose / Kotlin 2.0 / Room v22 / Hilt DI)  
**Repository Working Directory**: `/home/takian/Project/PROCUS`  
**Current Branch**: `main` (Latest commit: `cae13f5`)  
**Date**: October 18, 2026  

---

## 1. Executive Summary & Objective

Vice Head, this dossier marks the **successful, 100% verified execution of Architectural Directive ADR-002** across the `neuroFlow` codebase.

All four tracks mandated by your directive have been fully implemented, rigorously tested, and committed to git:
1. **Track 1 (Legacy Engine Purge & Background Worker Modernization)**: Completely eliminated the Split-Brain scheduling risk. `ScheduleAutoTasksWorker`, `FocusWidgetUpdateWorker`, and `NotificationWorker` are now rewired to `TaskSchedulerService` and `CircadianEnergyService`. The monolithic `AutoSchedulingEngine.kt` (2,438 lines) and `TaskScoringEngine.kt` (1,282 lines) along with 10 superseded legacy test files have been hard-deleted (**6,883 lines of debt purged**).
2. **Track 2 (Native Android Calendar Synchronization)**: Deployed `CalendarSyncDataSource.kt` implementing `AndroidCalendarSyncRepository` via `CalendarContract.Instances`. Features sweep-line interval coalescing, automatic 5-minute post-meeting decompression buffers for meetings $\ge 45$m, and self-authored block exclusion (`CUSTOM_APP_PACKAGE = "com.neuroflow.app"`). Wired directly into `PlanViewModelV2.kt`.
3. **Track 3 (Passive Chronobiology Telemetry Normalizer)**: Deployed `ChronobiologySignalNormalizer.kt` featuring 45-minute WASO episodic coalescing, sleep stage architecture weighting ($d_{\text{eff}}$ prioritizing SWS and REM), Bayesian duration blending ($c \in [0.0..1.0]$), and $\pm 90$m late-sync wake shift damping to eliminate ADHD schedule whiplash.
4. **Track 4 (Production Hardening & Baseline Profiles)**: Deployed curated static `baseline-prof.txt` targeting critical startup paths (`NeuroFlowApplication`, `MainActivity`, `FlowScreenV2`, `DefaultCircadianEnergyService`, Room DAOs) and hardened `proguard-rules.pro` for Room v22, Coroutines, and Foreground Service state machines.

### System Verification Scorecard:
* **All 5 Refactoring Phases**: 100% COMPLETE.
* **Test Suite Greenness**: **840 / 840 unit tests passing (100% pass rate, 0 failures, 0 errors, 0 skipped)** under strict memory-safe single-worker execution (`./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1`).
* **Critical Bug Fixes Resolved**:
  - In-place `Calendar` mutation bug in `ScheduleAutoTasksWorker.kt` and `PlanViewModelV2.kt` that was zeroing out current hour.
  - Locked-task busy window omission in `PlanViewModelV2.kt` auto-scheduling.
  - Inverted time range boundary checks in `CalendarSyncDataSource.kt`.
  - Room v22 DAO class symbol correction in `baseline-prof.txt` and `proguard-rules.pro`.

---

## 2. Refactoring Execution Ledger (Phases 1 — 5)

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
    end

    subgraph Phase5 ["Phase 5: ADR-002 Execution (3f70f01 / cae13f5)"]
        Worker[Workers wired to TSS + CES]
        PurgeEngines[Hard-deleted AutoSchedulingEngine & TaskScoringEngine: -6,883 lines]
        CalSync[AndroidCalendarSyncRepository via CalendarContract.Instances]
        ChronoNorm[ChronobiologySignalNormalizer with anti-whiplash damping]
        Hardening[Static baseline-prof.txt & hardened proguard-rules.pro]
        BugFixes[Resolved Calendar mutation & locked task preservation]
        Tests840[840/840 Unit Tests Passing 100% Green]
    end

    Phase1 --> Phase2
    Phase2 --> Phase3
    Phase3 --> Phase4
    Phase4 --> Phase5
```

---

## 3. Ratified Mathematical Models & Implemented Formulas

All formulas ratified across ADR-001 and ADR-002 are live, active, and fully tested in production:

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

### 3.3 Stage-Weighted & Anti-Whiplash Normalizer (`ChronobiologySignalNormalizer.kt`)
1. **Sleep Stage Restorative Hours**:
   $$d_{\text{eff}} = d_{\text{total}} \cdot \text{clamp}_{[0.75, 1.25]}\left(0.85 + 0.75 \cdot \frac{d_{\text{SWS}} + d_{\text{REM}}}{d_{\text{total}}}\right)$$
2. **Bayesian Duration Blending**:
   $$d_{\text{blended}} = c \cdot d_{\text{eff}} + (1 - c) \cdot d_{\text{base}}$$
3. **Anti-Whiplash Wake-Time Clamping**:
   $$t_{\text{wake}}^* = t_{\text{wake\_base}} + \text{clamp}_{[-90\text{m}, +90\text{m}]}\left(c \cdot (t_{\text{wake\_wearable}} - t_{\text{wake\_base}})\right)$$

### 3.4 Sweep-Line Interval Coalescing & Decompression Buffering (`CalendarSyncDataSource.kt`)
$$\text{BusyWindow}_{\text{effective}} = \left[s_{\text{merged}}, \; e_{\text{merged}} + \mathbb{I}(e_{\text{merged}} - s_{\text{merged}} \ge 45\text{m}) \cdot 5\text{m}\right]$$
All overlapping intervals and intervals separated by $< 5\text{m}$ coalesce into a single unified busy window.

---

## 4. Current System Architecture Topology

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

    subgraph OS_Bridge ["OS Local Bridge Layer"]
        CalContract[Android CalendarContract.Instances] --> CSR[AndroidCalendarSyncRepository]
        CSR --> PVM
    end

    subgraph Domain ["Modern Domain Services Layer"]
        FVM & PVM & Workers[ScheduleAutoTasksWorker] --> CES[CircadianEnergyService]
        FVM & PVM & Workers --> TSS[TaskSchedulerService]
        FVM & PVM & FocusVM[FocusViewModelV2] --> BSS[BehavioralSupportService]
        Composer[QuickCapture] --> NLP[TaskNaturalLanguageParser]
        Chrono[Wearables / Health Connect] --> CSN[ChronobiologySignalNormalizer]
        CSN --> CES
    end

    subgraph Data ["Normalized Data Layer (Room v22)"]
        TSS & FVM & PVM & Workers --> TR[TaskRepository]
        FocusVM & IVM --> SR[SessionRepository]
        PVM --> QND[QuickNoteDao]
        
        TR --> TE[TaskEntity (Indexed)]
        TR --> TMSE[TaskMicroStepEntity]
        SR --> FSE[FocusSessionEntity]
        QND --> QNE[QuickNoteEntity]
    end
```

---

## 5. Strategic Proposals & Questions for ADR-003

With the Split-Brain engine hazard permanently eliminated, native calendar synchronization operational, and production baseline profiles committed, we request your strategic directives for **ADR-003**:

### Topic 1: Health Connect Live Sensor Integration
* **Context**: `ChronobiologySignalNormalizer` is currently pure domain logic with 7 dedicated unit tests.
* **Proposal**: Add `androidx.health.connect:connect-client:1.1.0-alpha10` to `app/build.gradle.kts`, implement `HealthConnectSleepDataSource.kt` reading `SleepSessionRecord`, and register the Health Connect permissions contract.
* **Question for Vice Head**: Do you endorse adding the Health Connect client dependency now, or should we keep the domain adapter boundary strictly abstract until the final release phase?

### Topic 2: Deprecation of Legacy Energy Repositories
* **Context**: `PeakEnergyRepository` and `EnergyScoreRepository` still exist as legacy data providers consumed by `SettingsScreen`, `SleepLogsScreen`, and `DistractionSyncWorker`.
* **Proposal**: Re-wire `SettingsScreen` and `SleepLogsScreen` to read directly from `CircadianEnergyService` and `ChronobiologySignalNormalizer`, and safely retire `PeakEnergyRepository` and `EnergyScoreRepository`.
* **Question for Vice Head**: What is your recommended sequence for sunsetting the remaining legacy energy repositories?

### Topic 3: UI Visual Polish & Widget Optimization
* **Context**: With `FocusWidgetUpdateWorker` rewired to `TaskSchedulerService`, the home screen glance widget can render the real-time recommended Task and continuous energy level.
* **Proposal**: Polish the Glance AppWidget layout and add interactive completion actions.

---

## 6. Explicit Request for Vice Head Feedback

Vice Head, please provide:
1. **Formal Architectural Sign-off on Phase 5**: Confirm acceptance of the legacy engine deletion, calendar sweep-line coalescing, chronobiology signal normalizer, and production baseline profile rules.
2. **ADR-003 Directives**: Strategic ruling on Topics 1, 2, and 3.
3. **Next Priority Payload**: The immediate priority task for the engineering core.

---
*Ready for transmission to the Vice Head via human bridge.*
