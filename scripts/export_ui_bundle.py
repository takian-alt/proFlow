#!/usr/bin/env python3
"""
Export all UI code across neuroFlow into a dedicated, clean directory structure
with partitioned markdown bundles, a master bundle, and raw source files for Vice Head handoff.
"""

import os
import shutil
import datetime
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
OUTPUT_DIR = REPO_ROOT / "ui_code"
RAW_DIR = OUTPUT_DIR / "raw"

PART_DEFINITIONS = [
    (
        "PART_1_THEME_NAVIGATION.md",
        "Theme Engine, Design Tokens, Typography & Navigation Hubs",
        "Foundational theme palette (Obsidian, AMOLED, Light), dynamic tokens, typography, and root navigation topology.",
        lambda p: (
            p.startswith("presentation/v2/theme/")
            or p.startswith("presentation/common/theme/")
            or p.startswith("presentation/v2/navigation/")
        ),
    ),
    (
        "PART_2_FLOW_SCREEN.md",
        "Flow Execution Cockpit & Circadian Barometer",
        "FlowScreenV2, FlowViewModelV2, EnhancedCircadianEnergyGraph, CircadianHeroBarometer, and EnergyTimelineStream.",
        lambda p: p.startswith("presentation/v2/flow/"),
    ),
    (
        "PART_3_PLAN_COMPOSER.md",
        "Plan Screen, Scheduling Timeline & Smart Composer Sheet",
        "PlanScreenV2, PlanViewModelV2, SmartTaskComposerSheetV2, PlanAndComposerUpgradesV2, and WeeklySchedule.",
        lambda p: (
            p.startswith("presentation/v2/plan/")
            or p.startswith("presentation/v2/composer/")
            or p.startswith("presentation/schedule/")
        ),
    ),
    (
        "PART_4_FOCUS_REGULATION.md",
        "Focus Deep Work Cockpit & Audio Soundscape Synthesizer",
        "FocusScreenV2, FocusViewModelV2, FocusService, NeuralSoundscapePlayer/Generator, Solfeggio bell, and regulation sheets.",
        lambda p: p.startswith("presentation/v2/focus/"),
    ),
    (
        "PART_5_INSIGHTS_ANALYTICS.md",
        "Insights Hub, Chronobiology Analytics & Velocity Reports",
        "InsightsScreenV2, InsightsViewModelV2, InsightsTabbedUpgradesV2, AnalyticsScreen/ViewModel, and HistoryScreen.",
        lambda p: (
            p.startswith("presentation/v2/insights/")
            or p.startswith("presentation/analytics/")
            or p.startswith("presentation/history/")
        ),
    ),
    (
        "PART_6_SETTINGS_NOTES.md",
        "Settings Hub, Sleep Logs, MEQ Quiz & Notes Workspace",
        "SettingsScreen/ViewModel, MEQQuiz, SleepLogs, PriorityWeights, NotesScreen/Editor, and QuickNotesBottomSheet.",
        lambda p: (
            p.startswith("presentation/settings/")
            or p.startswith("presentation/notes/")
            or p.startswith("presentation/v2/notes/")
            or p.startswith("presentation/identity/")
            or p.startswith("presentation/onboarding/")
            or p.startswith("presentation/logtime/")
            or p.startswith("presentation/common/")
            or p.startswith("presentation/v2/common/")
            or p.startswith("presentation/v2/settings/")
        ),
    ),
    (
        "PART_7_HYPERFOCUS_WIDGETS.md",
        "HyperFocus Lock Engine, Gateways & Home Screen Widgets",
        "HyperFocusHomeScreen/Activity, BlockingOverlay, GateTaskScreen, PermissionSetup, and Glance App Widgets.",
        lambda p: (
            p.startswith("feature/hyperfocus/")
            or p.startswith("presentation/widget/")
        ),
    ),
]


def collect_ui_files():
    ui_files = []

    # 1. Presentation files
    pres_dir = REPO_ROOT / "app/src/main/java/com/neuroflow/app/presentation"
    for root, _, files in os.walk(pres_dir):
        for f in files:
            if f.endswith(".kt"):
                full_path = Path(root) / f
                rel_path = full_path.relative_to(REPO_ROOT)
                ui_files.append(rel_path)

    # 2. Feature HyperFocus UI files
    feat_dir = REPO_ROOT / "app/src/main/java/com/neuroflow/app/feature"
    for root, _, files in os.walk(feat_dir):
        for f in files:
            if f.endswith(".kt") and (
                "Screen" in f
                or "Activity" in f
                or "Sheet" in f
                or "Section" in f
                or "surface" in root
                or "gate" in root
                or "Model" in f
            ):
                full_path = Path(root) / f
                rel_path = full_path.relative_to(REPO_ROOT)
                ui_files.append(rel_path)

    ui_files.sort()
    return ui_files


def assign_part(rel_path_str: str):
    sub = rel_path_str.replace("app/src/main/java/com/neuroflow/app/", "").lower()
    for filename, title, desc, matcher in PART_DEFINITIONS:
        if matcher(sub):
            return filename, title, desc
    return PART_DEFINITIONS[5][0], PART_DEFINITIONS[5][1], PART_DEFINITIONS[5][2]


def write_file_markdown(out, rel_path: Path):
    full_path = REPO_ROOT / rel_path
    with open(full_path, "r", encoding="utf-8", errors="replace") as f:
        content = f.read()

    lines = content.count("\n") + (1 if content and not content.endswith("\n") else 0)
    size = len(content.encode("utf-8"))

    anchor = str(rel_path).replace("/", "").replace(".", "").replace("-", "").replace("_", "").lower()

    out.write(f'<a id="{anchor}"></a>\n')
    out.write(f"### File: `{rel_path}`\n\n")
    out.write(f"- **Path:** `{rel_path}`\n")
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

    files = collect_ui_files()
    print(f"Total UI files collected: {len(files)}")

    # Copy raw source files
    for f in files:
        sub_rel = str(f).replace("app/src/main/java/com/neuroflow/app/", "")
        dest = RAW_DIR / sub_rel
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(REPO_ROOT / f, dest)

    # Bucket into parts
    bucketed = {filename: [] for filename, _, _, _ in PART_DEFINITIONS}
    for f in files:
        pt_filename, _, _ = assign_part(str(f))
        bucketed[pt_filename].append(f)

    now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    # Generate each partitioned Markdown file
    part_meta = []
    for filename, title, desc, _ in PART_DEFINITIONS:
        part_files = bucketed[filename]
        part_path = OUTPUT_DIR / filename

        total_lines = 0
        total_bytes = 0
        for f in part_files:
            with open(REPO_ROOT / f, "rb") as fp:
                d = fp.read()
                total_bytes += len(d)
                total_lines += d.count(b"\n") + (1 if d and not d.endswith(b"\n") else 0)

        part_meta.append((filename, title, desc, len(part_files), total_lines, total_bytes))

        with open(part_path, "w", encoding="utf-8") as out:
            out.write(f"# neuroFlow UI Codebase — {title}\n\n")
            out.write(f"**Description:** {desc}  \n")
            out.write(f"**Generated for Vice Head Review:** {now}  \n")
            out.write(f"**Files:** {len(part_files)}  \n")
            out.write(f"**Lines of Code:** {total_lines:,}  \n")
            out.write(f"**Uncompressed Size:** {total_bytes / 1024:.1f} KB  \n\n")
            out.write("---\n\n")
            out.write("## Table of Contents\n\n")
            for f in part_files:
                anchor = str(f).replace("/", "").replace(".", "").replace("-", "").replace("_", "").lower()
                out.write(f"- [`{f}`](#{anchor})\n")
            out.write("\n---\n\n")
            out.write("## Source Code Listings\n\n")
            for f in part_files:
                write_file_markdown(out, f)

        print(f"Generated {filename}: {len(part_files)} files, {total_lines:,} lines, {total_bytes/1024:.1f} KB")

    # Generate ALL_UI_CODE.md
    master_path = OUTPUT_DIR / "ALL_UI_CODE.md"
    total_all_lines = sum(m[4] for m in part_meta)
    total_all_bytes = sum(m[5] for m in part_meta)

    with open(master_path, "w", encoding="utf-8") as out:
        out.write("# neuroFlow — Complete Unified UI Source Codebase\n\n")
        out.write(f"**Generated for Vice Head Architecture Audit:** {now}  \n")
        out.write(f"**Total UI Source Files:** {len(files)}  \n")
        out.write(f"**Total Lines of Code:** {total_all_lines:,}  \n")
        out.write(f"**Total Uncompressed Size:** {total_all_bytes / (1024*1024):.2f} MB ({total_all_bytes:,} bytes)  \n\n")
        out.write("---\n\n")
        out.write("## Modular Architecture Breakdown\n\n")
        out.write("| Part | Module Title | Files | Lines | Size (KB) |\n")
        out.write("| :--- | :--- | :---: | :---: | :---: |\n")
        for fn, title, _, fcount, lcount, bcount in part_meta:
            out.write(f"| [`{fn}`](./{fn}) | {title} | {fcount} | {lcount:,} | {bcount/1024:.1f} KB |\n")
        out.write(f"| **Total** | **All UI Components** | **{len(files)}** | **{total_all_lines:,}** | **{total_all_bytes/1024:.1f} KB** |\n\n")
        out.write("---\n\n")
        out.write("## Full File Manifest\n\n")
        for f in files:
            anchor = str(f).replace("/", "").replace(".", "").replace("-", "").replace("_", "").lower()
            out.write(f"- [`{f}`](#{anchor})\n")
        out.write("\n---\n\n")
        out.write("## Source Code Files\n\n")
        for f in files:
            write_file_markdown(out, f)

    print(f"Generated ALL_UI_CODE.md ({total_all_bytes / (1024*1024):.2f} MB)")

    # Generate README.md in ui_code/
    readme_path = OUTPUT_DIR / "README.md"
    with open(readme_path, "w", encoding="utf-8") as out:
        out.write("# neuroFlow — UI Codebase Directory for Vice Head Handoff\n\n")
        out.write("This directory contains the unabridged source code for all UI surfaces, Jetpack Compose screens, ViewModels, themes, audio soundscape synthesizers, and widgets in **neuroFlow** (`takian-alt/proFlow`).\n\n")
        out.write("It is prepared specifically for transmission across the human bridge to the **Vice Head** (frontier AI assistant).\n\n")
        out.write("## 📦 Contents & Structure\n\n")
        out.write("1. **`ALL_UI_CODE.md`**: Single consolidated file containing all UI source files (~1.2 MB). Ideal for frontier models with large context windows (e.g. Gemini 1.5 Pro / Flash).\n")
        out.write("2. **Modular Parts (1 to 7)**: Chunked by architectural layer (each ~50 KB to ~340 KB) for platforms with smaller message limits (Claude, ChatGPT, etc.):\n\n")
        out.write("| Part File | Architectural Focus | File Count | Lines | Size |\n")
        out.write("| :--- | :--- | :---: | :---: | :---: |\n")
        for fn, title, desc, fcount, lcount, bcount in part_meta:
            out.write(f"| [`{fn}`](./{fn}) | **{title}**<br>_{desc}_ | {fcount} | {lcount:,} | {bcount/1024:.1f} KB |\n")
        out.write(f"| **Total** | | **{len(files)}** | **{total_all_lines:,}** | **{total_all_bytes/1024:.1f} KB** |\n\n")
        out.write("3. **`raw/`**: Exact `.kt` Kotlin source files mirrored with relative package directory structure for direct code compilation or IDE inspection.\n\n")
        out.write("## 🚀 Recommended Protocol for Vice Head Review\n\n")
        out.write("- If your model has a **1M+ token context window**, upload or paste `ALL_UI_CODE.md` directly.\n")
        out.write("- If submitting in **stages or conversational turns**, submit `PART_1` through `PART_7` sequentially.\n")

    print(f"Generated README.md in {OUTPUT_DIR}")


if __name__ == "__main__":
    main()
