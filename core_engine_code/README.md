# neuroFlow — Core Engine, Circadian Biophysics, Auto-Scheduling & Task Creation Codebase

This directory contains the unabridged source code for all **Circadian Energy Modeling**, **Mathematical Auto-Scheduling**, **Task Creation & Decomposition**, **Data Layer Contracts**, and **Unit Verification Test Suites** in **neuroFlow** (`takian-alt/proFlow`).

It is prepared specifically for transmission across the human bridge to the **Vice Head** (frontier AI assistant).

## 🏛️ Subsystem Map & Architectural Invariants

### 1. Energy & Circadian Biophysics Subsystem
- **Continuous Borbély Two-Process Model**: Homeostatic sleep pressure $S(t)$ and double-harmonic circadian alertness $C(t)$ with continuous sleep debt clearance.
- **Fast Circadian Kalman Learner**: 1D circular Kalman filter state update combined with a Von Mises circular smoother ($\kappa = 4.5$) and Abramowitz & Stegun polynomial approximation of $I_0(x)$ for 2–3 session empirical peak adaptation without midnight wrap-around error.
- **Polyphasic Nap Biophysics**: Intra-day power naps (10m–180m) modeled strictly as homeostatic pressure clearance $(\Delta S_{\text{nap}})$ and 20-min exponential sleep inertia envelopes $(I_{\text{inertia}})$, leaving the morning wake anchor Process $C(t)$ phase-locked.
- **Acute Physiological Impulses**: Gaussian post-prandial meal dip ($t_{\text{peak}} = 60\text{m}, \sigma = 35\text{m}, -12.0$ pts) and post-exercise endorphin boost ($+10.0$ pts, $\tau = 45\text{m}$) with in-workout exertion penalty ($-15.0$ pts).
- **Chronobiology Signal Normalization**: Wearable sleep stage efficiency weighting ($d_{\text{eff}}$), WASO coalescing, Bayesian duration blending ($d_{\text{blended}}$), and anti-whiplash wake shift clamping ($\pm 90\text{m}$).

### 2. Auto-Scheduling & Mathematical Placement Engine
- **4-Vector Prioritization**: Normalized $\text{Score}(\tau, t) = 0.30 U + 0.30 I + 0.25 E + 0.15 M \in [0, 100]$.
- **Demand-Elastic Utility Damping**: $\lambda(\tau) \in \{0.08, 0.22, 0.45\}\text{ pts/hr}$ preventing premature scheduling of low-demand tasks.
- **Topological DAG Sorting**: Resolves multi-prerequisite task dependencies before placement.
- **Ultradian Cognitive Load Recovery**: Automatic 15-minute recovery buffer inserted after $\ge 90\text{m}$ of continuous cognitive work.
- **Peak Window Reservation**: 16-point penalty preventing low-demand or admin tasks from occupying biological peak slots.
- **Calendar Sweep-Line Coalescing**: 5-minute meeting decompression buffer and loop-safe bidirectional 2-way write-back sync (`com.neuroflow.app`).

### 3. Task Creation, NLP Parsing, AI Decomposition & Behavioral Engines
- **Natural Language Parsing**: High-speed regex and token parsing for dates, times, durations, priority flags, and tags.
- **Task Inference & DST Anchoring**: Duration-based archetype heuristics, EWMA ratio calibration, and DST-safe midnight boundary anchoring via `java.time.LocalDate`.
- **BYOK Gemini Flash Engine**: Multi-model upstream cascade (`gemini-flash-latest` $\rightarrow$ `gemini-flash-lite-latest` $\rightarrow$ `gemini-3.5-flash` $\rightarrow$ `gemini-3.8-flash`), 20-min in-memory TTL caching, 60s 429 rate-limit backoff, and single-call bundled task enrichment (`enrichTaskWithAi`).
- **6-Archetype Local Heuristic Decomposer**: Zero-failure offline fallback generating micro-activation doorway steps for Coding, Writing, Study, Chores, Admin, and General tasks.
- **Forensic Avoidance Audit & Adaptive Directives**: Weekly diagnosis of postponed and deleted tasks producing 120-second actionable prescriptions and an injected `adaptiveSystemDirective`.

## 📦 Contents & Partition Structure

1. **`ALL_CORE_ENGINE_CODE.md`**: Single consolidated file containing all 68 source and test files (~580 KB). Ideal for frontier models with large context windows (e.g. Gemini 1.5 Pro / Flash).
2. **Modular Parts (1 to 5)**: Chunked by functional domain (each ~100 KB to ~125 KB, ~2,200 to ~3,100 lines) for platforms with smaller message limits (Claude, ChatGPT, etc.):

| Part File | Architectural Focus | File Count | Lines | Size |
| :--- | :--- | :---: | :---: | :---: |
| [`PART_1_ENERGY_CIRCADIAN.md`](./PART_1_ENERGY_CIRCADIAN.md) | **Part 1: Energy & Circadian Biophysics**<br>_Continuous Borbély Two-Process Model (S(t) & C(t)), Fast Circadian Kalman Learner + Von Mises smoother, Polyphasic Nap Biophysics, Physiological Impulses (meals & workouts), Chronobiology Signal Normalizer, and Wearable Sleep Telemetry._ | 14 | 4,067 | 163.9 KB |
| [`PART_2_AUTOSCHEDULING_PLACEMENT.md`](./PART_2_AUTOSCHEDULING_PLACEMENT.md) | **Part 2: Auto-Scheduling & Mathematical Placement Engine**<br>_4-Vector Normalized Prioritization (Urgency, Impact, Energy Fit, Momentum), Demand-Elastic Utility Damping λ(τ), Chronotype-Aware Slot Alignment, Gap-Fitting, Topological DAG Sorting, Simulated Cognitive Load, 90-min Ultradian Buffers, Android Calendar Read & Write-Back Sync with Decompression, and Background Re-scheduling Workers._ | 14 | 4,029 | 178.2 KB |
| [`PART_3_TASK_CREATION_NLP_AI.md`](./PART_3_TASK_CREATION_NLP_AI.md) | **Part 3: Task Creation, NLP Parsing, AI Decomposition & Behavioral Engines**<br>_Regex & Heuristic TaskNaturalLanguageParser, TaskInferenceEngine (duration heuristics, DST midnight bounds, EWMA duration calibration), GeminiAiOnlineService (BYOK, multi-model upstream cascade, TTL caching, 429 backoff, single-call enrichment, weekly avoidance audit), SmartTaskDecompositionService (6-archetype local fallback), BehavioralSupportService (WOOP, implementation intentions, doorway micro-steps), and AnalyticsEngine._ | 9 | 2,484 | 111.4 KB |
| [`PART_4_DATA_ENTITIES_REPOSITORIES.md`](./PART_4_DATA_ENTITIES_REPOSITORIES.md) | **Part 4: Data Layer Architecture, Room Schemas, Entities, DAOs & Repositories**<br>_Room Database v22 Schema, TaskEntity (composite indices), TaskMicroStepEntity, TaskTimeExtensions (dual-format timestamp resolution), EnergyPredictionEntity, ProtectedSlotEntity, SleepLogEntity, DAOs, TaskRepository (reactive StateFlow, completeAndRecur), Type Converters, UserPreferencesDataStore (BYOK, chronotype, directive, avoidance telemetry), and Hilt Dependency Injection Modules._ | 19 | 2,674 | 125.0 KB |
| [`PART_5_TEST_SUITES_VERIFICATION.md`](./PART_5_TEST_SUITES_VERIFICATION.md) | **Part 5: Core Algorithmic & Mathematical Test Suites**<br>_Exhaustive Unit Test Suites verifying: Continuous Borbély biophysics, Fast Circadian Kalman Learner convergence, Circadian adversarial stress, 4-vector scheduling placement, NLP syntax parsing, Gemini AI cascading & local heuristic fallbacks, Wearable chronobiology signal normalization, Calendar read & write-back synchronization, and Placement stability._ | 17 | 3,526 | 142.1 KB |
| **Total** | | **73** | **16,780** | **720.5 KB** |

3. **`raw/`**: Exact `.kt` Kotlin source and test files mirrored with relative package directory structure for direct code compilation, IDE inspection, or git diffs.

## 🚀 Recommended Protocol for Vice Head Review

### Option A: Frontier Models (1M+ Token Context)
Upload or paste [`ALL_CORE_ENGINE_CODE.md`](./ALL_CORE_ENGINE_CODE.md) directly with the following prompt:

```text
Vice Head: Please review the entire neuroFlow Core Engine codebase (Circadian Biophysics, Auto-Scheduling, Task Creation & AI Decomposition, Data Architecture, and Verification Tests) provided in ALL_CORE_ENGINE_CODE.md.
Audit the implementation against the ratified mathematical equations, invariant contracts, and Room v22 schema. Provide your architectural critique, algorithmic optimizations, and any recommended hardening patches.
```

### Option B: Turn-by-Turn Sequential Review (Claude / ChatGPT / Standard Context)
Send the files in sequence:
1. **Turn 1**: [`PART_1_ENERGY_CIRCADIAN.md`](./PART_1_ENERGY_CIRCADIAN.md) — Energy & Circadian Biophysics
2. **Turn 2**: [`PART_2_AUTOSCHEDULING_PLACEMENT.md`](./PART_2_AUTOSCHEDULING_PLACEMENT.md) — Auto-Scheduling & Mathematical Placement Engine
3. **Turn 3**: [`PART_3_TASK_CREATION_NLP_AI.md`](./PART_3_TASK_CREATION_NLP_AI.md) — Task Creation, NLP Parsing, AI Decomposition & Behavioral Engines
4. **Turn 4**: [`PART_4_DATA_ENTITIES_REPOSITORIES.md`](./PART_4_DATA_ENTITIES_REPOSITORIES.md) — Data Layer Architecture, Room Schemas, Entities, DAOs & Repositories
5. **Turn 5**: [`PART_5_TEST_SUITES_VERIFICATION.md`](./PART_5_TEST_SUITES_VERIFICATION.md) — Core Algorithmic & Mathematical Test Suites
