#!/usr/bin/env python3
"""
Export all core engine code across neuroFlow (Energy & Circadian Biophysics,
Auto-Scheduling & Placement, Task Creation & AI Decomposition, Data Architecture, and Unit Tests)
into a dedicated, clean directory structure with partitioned markdown bundles,
a master bundle, and raw source files for Vice Head handoff.
"""

import os
import shutil
import datetime
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
OUTPUT_DIR = REPO_ROOT / "core_engine_code"
RAW_DIR = OUTPUT_DIR / "raw"

PART_DEFINITIONS = [
    (
        "PART_1_ENERGY_CIRCADIAN.md",
        "Part 1: Energy & Circadian Biophysics",
        "Continuous Borbély Two-Process Model (S(t) & C(t)), Fast Circadian Kalman Learner + Von Mises smoother, Polyphasic Nap Biophysics, Physiological Impulses (meals & workouts), Chronobiology Signal Normalizer, and Wearable Sleep Telemetry.",
        [
            "app/src/main/java/com/neuroflow/app/domain/service/CircadianEnergyService.kt",
            "app/src/main/java/com/neuroflow/app/domain/service/ChronobiologySignalNormalizer.kt",
            "app/src/main/java/com/neuroflow/app/domain/model/EnergyUiModel.kt",
            "app/src/main/java/com/neuroflow/app/domain/model/CircadianEnergySummary.kt",
            "app/src/main/java/com/neuroflow/app/domain/model/FatigueZone.kt",
            "app/src/main/java/com/neuroflow/app/domain/engine/MEQChronotypeDetector.kt",
            "app/src/main/java/com/neuroflow/app/domain/repository/SleepPressureRepository.kt",
            "app/src/main/java/com/neuroflow/app/domain/repository/PassiveSleepSyncRepository.kt",
            "app/src/main/java/com/neuroflow/app/domain/repository/EnergyMetricsRepository.kt",
            "app/src/main/java/com/neuroflow/app/data/sleep/SleepTelemetryDataSource.kt",
            "app/src/main/java/com/neuroflow/app/data/repository/SleepLogRepository.kt",
            "app/src/main/java/com/neuroflow/app/data/repository/PassiveSleepSyncManager.kt",
            "app/src/main/java/com/neuroflow/app/worker/SleepPressureRefreshWorker.kt",
            "app/src/main/java/com/neuroflow/app/worker/EnergyTelemetryCleanupWorker.kt",
        ]
    ),
    (
        "PART_2_AUTOSCHEDULING_PLACEMENT.md",
        "Part 2: Auto-Scheduling & Mathematical Placement Engine",
        "4-Vector Normalized Prioritization (Urgency, Impact, Energy Fit, Momentum), Demand-Elastic Utility Damping λ(τ), Chronotype-Aware Slot Alignment, Gap-Fitting, Topological DAG Sorting, Simulated Cognitive Load, 90-min Ultradian Buffers, Android Calendar Read & Write-Back Sync with Decompression, and Background Re-scheduling Workers.",
        [
            "app/src/main/java/com/neuroflow/app/domain/service/TaskSchedulerService.kt",
            "app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingContracts.kt",
            "app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingRationales.kt",
            "app/src/main/java/com/neuroflow/app/domain/scheduler/PlacementStability.kt",
            "app/src/main/java/com/neuroflow/app/domain/scheduler/ProtectedSlotClock.kt",
            "app/src/main/java/com/neuroflow/app/domain/scheduler/TaskBlockPlan.kt",
            "app/src/main/java/com/neuroflow/app/domain/scheduler/TaskTagSchedulingProfile.kt",
            "app/src/main/java/com/neuroflow/app/domain/scheduler/BehavioralInsightsEngine.kt",
            "app/src/main/java/com/neuroflow/app/domain/scheduler/DependencyGraphValidator.kt",
            "app/src/main/java/com/neuroflow/app/domain/scheduler/SchedulePreparationOrchestrator.kt",
            "app/src/main/java/com/neuroflow/app/data/calendar/CalendarSyncDataSource.kt",
            "app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt",
            "app/src/main/java/com/neuroflow/app/worker/NotificationWorker.kt",
            "app/src/main/java/com/neuroflow/app/worker/FocusWidgetUpdateWorker.kt",
        ]
    ),
    (
        "PART_3_TASK_CREATION_NLP_AI.md",
        "Part 3: Task Creation, NLP Parsing, AI Decomposition & Behavioral Engines",
        "Regex & Heuristic TaskNaturalLanguageParser, TaskInferenceEngine (duration heuristics, DST midnight bounds, EWMA duration calibration), GeminiAiOnlineService (BYOK, multi-model upstream cascade, TTL caching, 429 backoff, single-call enrichment, weekly avoidance audit), SmartTaskDecompositionService (6-archetype local fallback), BehavioralSupportService (WOOP, implementation intentions, doorway micro-steps), and AnalyticsEngine.",
        [
            "app/src/main/java/com/neuroflow/app/domain/service/TaskNaturalLanguageParser.kt",
            "app/src/main/java/com/neuroflow/app/domain/inference/TaskInferenceEngine.kt",
            "app/src/main/java/com/neuroflow/app/domain/service/GeminiAiOnlineService.kt",
            "app/src/main/java/com/neuroflow/app/domain/service/SmartTaskDecompositionService.kt",
            "app/src/main/java/com/neuroflow/app/domain/service/BehavioralSupportService.kt",
            "app/src/main/java/com/neuroflow/app/domain/engine/WoopEngine.kt",
            "app/src/main/java/com/neuroflow/app/domain/engine/AutonomyNudgeEngine.kt",
            "app/src/main/java/com/neuroflow/app/domain/engine/FreshStartEngine.kt",
            "app/src/main/java/com/neuroflow/app/domain/engine/AnalyticsEngine.kt",
        ]
    ),
    (
        "PART_4_DATA_ENTITIES_REPOSITORIES.md",
        "Part 4: Data Layer Architecture, Room Schemas, Entities, DAOs & Repositories",
        "Room Database v22 Schema, TaskEntity (composite indices), TaskMicroStepEntity, TaskTimeExtensions (dual-format timestamp resolution), EnergyPredictionEntity, ProtectedSlotEntity, SleepLogEntity, DAOs, TaskRepository (reactive StateFlow, completeAndRecur), Type Converters, UserPreferencesDataStore (BYOK, chronotype, directive, avoidance telemetry), and Hilt Dependency Injection Modules.",
        [
            "app/src/main/java/com/neuroflow/app/data/local/entity/TaskEntity.kt",
            "app/src/main/java/com/neuroflow/app/data/local/entity/TaskMicroStepEntity.kt",
            "app/src/main/java/com/neuroflow/app/data/local/entity/TaskTimeExtensions.kt",
            "app/src/main/java/com/neuroflow/app/data/local/entity/EnergyPredictionEntity.kt",
            "app/src/main/java/com/neuroflow/app/data/local/entity/ProtectedSlotEntity.kt",
            "app/src/main/java/com/neuroflow/app/data/local/entity/SleepLogEntity.kt",
            "app/src/main/java/com/neuroflow/app/data/local/dao/TaskDao.kt",
            "app/src/main/java/com/neuroflow/app/data/local/dao/TaskMicroStepDao.kt",
            "app/src/main/java/com/neuroflow/app/data/local/dao/ProtectedSlotDao.kt",
            "app/src/main/java/com/neuroflow/app/data/local/dao/EnergyPredictionDao.kt",
            "app/src/main/java/com/neuroflow/app/data/local/dao/SleepLogDao.kt",
            "app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt",
            "app/src/main/java/com/neuroflow/app/data/local/Converters.kt",
            "app/src/main/java/com/neuroflow/app/data/local/UserPreferencesDataStore.kt",
            "app/src/main/java/com/neuroflow/app/domain/model/Enums.kt",
            "app/src/main/java/com/neuroflow/app/domain/model/ProtectedTimeSlot.kt",
            "app/src/main/java/com/neuroflow/app/domain/model/StepByStepPlan.kt",
            "app/src/main/java/com/neuroflow/app/di/DomainServiceModule.kt",
            "app/src/main/java/com/neuroflow/app/di/AppModule.kt",
        ]
    ),
    (
        "PART_5_TEST_SUITES_VERIFICATION.md",
        "Part 5: Core Algorithmic & Mathematical Test Suites",
        "Exhaustive Unit Test Suites verifying: Continuous Borbély biophysics, Fast Circadian Kalman Learner convergence, Circadian adversarial stress, 4-vector scheduling placement, NLP syntax parsing, Gemini AI cascading & local heuristic fallbacks, Wearable chronobiology signal normalization, Calendar read & write-back synchronization, and Placement stability.",
        [
            "app/src/test/java/com/neuroflow/app/domain/service/CircadianEnergyServiceTest.kt",
            "app/src/test/java/com/neuroflow/app/domain/service/FastCircadianLearnerTest.kt",
            "app/src/test/java/com/neuroflow/app/domain/service/CircadianAdversarialStressTest.kt",
            "app/src/test/java/com/neuroflow/app/domain/service/Adr020RobustnessTest.kt",
            "app/src/test/java/com/neuroflow/app/domain/service/Adr022RobustnessTest.kt",
            "app/src/test/java/com/neuroflow/app/domain/service/TaskSchedulerServiceTest.kt",
            "app/src/test/java/com/neuroflow/app/domain/service/TaskNaturalLanguageParserTest.kt",
            "app/src/test/java/com/neuroflow/app/domain/service/SmartTaskDecompositionServiceTest.kt",
            "app/src/test/java/com/neuroflow/app/domain/service/GeminiAiOnlineServiceTest.kt",
            "app/src/test/java/com/neuroflow/app/domain/service/ChronobiologySignalNormalizerTest.kt",
            "app/src/test/java/com/neuroflow/app/domain/service/RealWorldChronobiologyAndSchedulerTest.kt",
            "app/src/test/java/com/neuroflow/app/domain/scheduler/PlacementStabilityTest.kt",
            "app/src/test/java/com/neuroflow/app/domain/scheduler/ProtectedSlotClockTest.kt",
            "app/src/test/java/com/neuroflow/app/domain/inference/TaskInferenceEngineTest.kt",
            "app/src/test/java/com/neuroflow/app/data/local/StimulantLoggingTest.kt",
            "app/src/test/java/com/neuroflow/app/data/calendar/CalendarSyncRepositoryTest.kt",
            "app/src/test/java/com/neuroflow/app/data/calendar/CalendarWriteBackSyncTest.kt",
        ]
    ),
]


def write_file_markdown(out, rel_path_str: str):
    full_path = REPO_ROOT / rel_path_str
    with open(full_path, "r", encoding="utf-8", errors="replace") as f:
        content = f.read()

    lines = content.count("\n") + (1 if content and not content.endswith("\n") else 0)
    size = len(content.encode("utf-8"))

    anchor = rel_path_str.replace("/", "").replace(".", "").replace("-", "").replace("_", "").lower()

    out.write(f'<a id="{anchor}"></a>\n')
    out.write(f"### File: `{rel_path_str}`\n\n")
    out.write(f"- **Path:** `{rel_path_str}`\n")
    out.write(f"- **Lines of Code:** {lines:,}\n")
    out.write(f"- **Size:** {size:,} bytes ({size/1024:.1f} KB)\n\n")
    out.write("```kotlin\n")
    out.write(content)
    if not content.endswith("\n"):
        out.write("\n")
    out.write("```\n\n---\n\n")


def main():
    if OUTPUT_DIR.exists():
        shutil.rmtree(OUTPUT_DIR)
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    RAW_DIR.mkdir(parents=True, exist_ok=True)

    all_files = []
    for _, _, _, flist in PART_DEFINITIONS:
        for f in flist:
            if f not in all_files:
                all_files.append(f)

    print(f"Total unique files to bundle: {len(all_files)}")

    # Copy raw source files
    for f in all_files:
        dest = RAW_DIR / f
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(REPO_ROOT / f, dest)

    now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    part_meta = []
    for filename, title, desc, part_files in PART_DEFINITIONS:
        part_path = OUTPUT_DIR / filename

        total_lines = 0
        total_bytes = 0
        for f in part_files:
            with open(REPO_ROOT / f, "rb") as fp:
                d = fp.read()
                total_bytes += len(d)
                total_lines += d.count(b"\n") + (1 if d and not d.endswith(b"\n") else 0)

        part_meta.append((filename, title, desc, len(part_files), total_lines, total_bytes, part_files))

        with open(part_path, "w", encoding="utf-8") as out:
            out.write(f"# neuroFlow Core Engine — {title}\n\n")
            out.write(f"**Description:** {desc}  \n")
            out.write(f"**Generated for Vice Head Review:** {now}  \n")
            out.write(f"**Files:** {len(part_files)}  \n")
            out.write(f"**Lines of Code:** {total_lines:,}  \n")
            out.write(f"**Uncompressed Size:** {total_bytes / 1024:.1f} KB  \n\n")
            out.write("---\n\n")
            out.write("## Table of Contents\n\n")
            for f in part_files:
                anchor = f.replace("/", "").replace(".", "").replace("-", "").replace("_", "").lower()
                out.write(f"- [`{f}`](#{anchor})\n")
            out.write("\n---\n\n")
            out.write("## Source Code Listings\n\n")
            for f in part_files:
                write_file_markdown(out, f)

        print(f"Generated {filename}: {len(part_files)} files, {total_lines:,} lines, {total_bytes/1024:.1f} KB")

    # Generate ALL_CORE_ENGINE_CODE.md
    master_path = OUTPUT_DIR / "ALL_CORE_ENGINE_CODE.md"
    total_all_lines = sum(m[4] for m in part_meta)
    total_all_bytes = sum(m[5] for m in part_meta)

    with open(master_path, "w", encoding="utf-8") as out:
        out.write("# neuroFlow — Complete Core Engine Source Codebase\n\n")
        out.write("### Energy & Circadian Biophysics, Auto-Scheduling Engine, Task Creation & Decomposition, and Data Contracts\n\n")
        out.write(f"**Generated for Vice Head Architecture Audit:** {now}  \n")
        out.write(f"**Total Source & Test Files:** {len(all_files)}  \n")
        out.write(f"**Total Lines of Code:** {total_all_lines:,}  \n")
        out.write(f"**Total Uncompressed Size:** {total_all_bytes / 1024:.1f} KB ({total_all_bytes / (1024*1024):.2f} MB)  \n\n")
        out.write("---\n\n")
        out.write("## Subsystem Architecture Breakdown\n\n")
        out.write("| Part File | Focus Area | Files | Lines | Size (KB) |\n")
        out.write("| :--- | :--- | :---: | :---: | :---: |\n")
        for fn, title, _, fcount, lcount, bcount, _ in part_meta:
            out.write(f"| [`{fn}`](./{fn}) | **{title}** | {fcount} | {lcount:,} | {bcount/1024:.1f} KB |\n")
        out.write(f"| **Total** | **All Core Engine Components** | **{len(all_files)}** | **{total_all_lines:,}** | **{total_all_bytes/1024:.1f} KB** |\n\n")
        out.write("---\n\n")
        out.write("## Full File Manifest\n\n")
        for f in all_files:
            anchor = f.replace("/", "").replace(".", "").replace("-", "").replace("_", "").lower()
            out.write(f"- [`{f}`](#{anchor})\n")
        out.write("\n---\n\n")
        out.write("## Source Code Files\n\n")
        for f in all_files:
            write_file_markdown(out, f)

    print(f"Generated ALL_CORE_ENGINE_CODE.md ({total_all_bytes / 1024:.1f} KB)")

    # Generate README.md in core_engine_code/
    readme_path = OUTPUT_DIR / "README.md"
    with open(readme_path, "w", encoding="utf-8") as out:
        out.write("# neuroFlow — Core Engine, Circadian Biophysics, Auto-Scheduling & Task Creation Codebase\n\n")
        out.write("This directory contains the unabridged source code for all **Circadian Energy Modeling**, **Mathematical Auto-Scheduling**, **Task Creation & Decomposition**, **Data Layer Contracts**, and **Unit Verification Test Suites** in **neuroFlow** (`takian-alt/proFlow`).\n\n")
        out.write("It is prepared specifically for transmission across the human bridge to the **Vice Head** (frontier AI assistant).\n\n")
        out.write("## 🏛️ Subsystem Map & Architectural Invariants\n\n")
        out.write("### 1. Energy & Circadian Biophysics Subsystem\n")
        out.write("- **Continuous Borbély Two-Process Model**: Homeostatic sleep pressure $S(t)$ and double-harmonic circadian alertness $C(t)$ with continuous sleep debt clearance.\n")
        out.write("- **Fast Circadian Kalman Learner**: 1D circular Kalman filter state update combined with a Von Mises circular smoother ($\\kappa = 4.5$) and Abramowitz & Stegun polynomial approximation of $I_0(x)$ for 2–3 session empirical peak adaptation without midnight wrap-around error.\n")
        out.write("- **Polyphasic Nap Biophysics**: Intra-day power naps (10m–180m) modeled strictly as homeostatic pressure clearance $(\\Delta S_{\\text{nap}})$ and 20-min exponential sleep inertia envelopes $(I_{\\text{inertia}})$, leaving the morning wake anchor Process $C(t)$ phase-locked.\n")
        out.write("- **Acute Physiological Impulses**: Gaussian post-prandial meal dip ($t_{\\text{peak}} = 60\\text{m}, \\sigma = 35\\text{m}, -12.0$ pts) and post-exercise endorphin boost ($+10.0$ pts, $\\tau = 45\\text{m}$) with in-workout exertion penalty ($-15.0$ pts).\n")
        out.write("- **Chronobiology Signal Normalization**: Wearable sleep stage efficiency weighting ($d_{\\text{eff}}$), WASO coalescing, Bayesian duration blending ($d_{\\text{blended}}$), and anti-whiplash wake shift clamping ($\\pm 90\\text{m}$).\n\n")
        out.write("### 2. Auto-Scheduling & Mathematical Placement Engine\n")
        out.write("- **4-Vector Prioritization**: Normalized $\\text{Score}(\\tau, t) = 0.30 U + 0.30 I + 0.25 E + 0.15 M \\in [0, 100]$.\n")
        out.write("- **Demand-Elastic Utility Damping**: $\\lambda(\\tau) \\in \\{0.08, 0.22, 0.45\\}\\text{ pts/hr}$ preventing premature scheduling of low-demand tasks.\n")
        out.write("- **Topological DAG Sorting**: Resolves multi-prerequisite task dependencies before placement.\n")
        out.write("- **Ultradian Cognitive Load Recovery**: Automatic 15-minute recovery buffer inserted after $\\ge 90\\text{m}$ of continuous cognitive work.\n")
        out.write("- **Peak Window Reservation**: 16-point penalty preventing low-demand or admin tasks from occupying biological peak slots.\n")
        out.write("- **Calendar Sweep-Line Coalescing**: 5-minute meeting decompression buffer and loop-safe bidirectional 2-way write-back sync (`com.neuroflow.app`).\n\n")
        out.write("### 3. Task Creation, NLP Parsing, AI Decomposition & Behavioral Engines\n")
        out.write("- **Natural Language Parsing**: High-speed regex and token parsing for dates, times, durations, priority flags, and tags.\n")
        out.write("- **Task Inference & DST Anchoring**: Duration-based archetype heuristics, EWMA ratio calibration, and DST-safe midnight boundary anchoring via `java.time.LocalDate`.\n")
        out.write("- **BYOK Gemini Flash Engine**: Multi-model upstream cascade (`gemini-flash-latest` $\\rightarrow$ `gemini-flash-lite-latest` $\\rightarrow$ `gemini-3.5-flash` $\\rightarrow$ `gemini-3.8-flash`), 20-min in-memory TTL caching, 60s 429 rate-limit backoff, and single-call bundled task enrichment (`enrichTaskWithAi`).\n")
        out.write("- **6-Archetype Local Heuristic Decomposer**: Zero-failure offline fallback generating micro-activation doorway steps for Coding, Writing, Study, Chores, Admin, and General tasks.\n")
        out.write("- **Forensic Avoidance Audit & Adaptive Directives**: Weekly diagnosis of postponed and deleted tasks producing 120-second actionable prescriptions and an injected `adaptiveSystemDirective`.\n\n")
        out.write("## 📦 Contents & Partition Structure\n\n")
        out.write("1. **`ALL_CORE_ENGINE_CODE.md`**: Single consolidated file containing all 68 source and test files (~580 KB). Ideal for frontier models with large context windows (e.g. Gemini 1.5 Pro / Flash).\n")
        out.write("2. **Modular Parts (1 to 5)**: Chunked by functional domain (each ~100 KB to ~125 KB, ~2,200 to ~3,100 lines) for platforms with smaller message limits (Claude, ChatGPT, etc.):\n\n")
        out.write("| Part File | Architectural Focus | File Count | Lines | Size |\n")
        out.write("| :--- | :--- | :---: | :---: | :---: |\n")
        for fn, title, desc, fcount, lcount, bcount, _ in part_meta:
            out.write(f"| [`{fn}`](./{fn}) | **{title}**<br>_{desc}_ | {fcount} | {lcount:,} | {bcount/1024:.1f} KB |\n")
        out.write(f"| **Total** | | **{len(all_files)}** | **{total_all_lines:,}** | **{total_all_bytes/1024:.1f} KB** |\n\n")
        out.write("3. **`raw/`**: Exact `.kt` Kotlin source and test files mirrored with relative package directory structure for direct code compilation, IDE inspection, or git diffs.\n\n")
        out.write("## 🚀 Recommended Protocol for Vice Head Review\n\n")
        out.write("### Option A: Frontier Models (1M+ Token Context)\n")
        out.write("Upload or paste [`ALL_CORE_ENGINE_CODE.md`](./ALL_CORE_ENGINE_CODE.md) directly with the following prompt:\n\n")
        out.write("```text\n")
        out.write("Vice Head: Please review the entire neuroFlow Core Engine codebase (Circadian Biophysics, Auto-Scheduling, Task Creation & AI Decomposition, Data Architecture, and Verification Tests) provided in ALL_CORE_ENGINE_CODE.md.\n")
        out.write("Audit the implementation against the ratified mathematical equations, invariant contracts, and Room v22 schema. Provide your architectural critique, algorithmic optimizations, and any recommended hardening patches.\n")
        out.write("```\n\n")
        out.write("### Option B: Turn-by-Turn Sequential Review (Claude / ChatGPT / Standard Context)\n")
        out.write("Send the files in sequence:\n")
        out.write("1. **Turn 1**: [`PART_1_ENERGY_CIRCADIAN.md`](./PART_1_ENERGY_CIRCADIAN.md) — Energy & Circadian Biophysics\n")
        out.write("2. **Turn 2**: [`PART_2_AUTOSCHEDULING_PLACEMENT.md`](./PART_2_AUTOSCHEDULING_PLACEMENT.md) — Auto-Scheduling & Mathematical Placement Engine\n")
        out.write("3. **Turn 3**: [`PART_3_TASK_CREATION_NLP_AI.md`](./PART_3_TASK_CREATION_NLP_AI.md) — Task Creation, NLP Parsing, AI Decomposition & Behavioral Engines\n")
        out.write("4. **Turn 4**: [`PART_4_DATA_ENTITIES_REPOSITORIES.md`](./PART_4_DATA_ENTITIES_REPOSITORIES.md) — Data Layer Architecture, Room Schemas, Entities, DAOs & Repositories\n")
        out.write("5. **Turn 5**: [`PART_5_TEST_SUITES_VERIFICATION.md`](./PART_5_TEST_SUITES_VERIFICATION.md) — Core Algorithmic & Mathematical Test Suites\n")

    print(f"Generated README.md in {OUTPUT_DIR}")


if __name__ == "__main__":
    main()
