#!/usr/bin/env python3
"""
Bundle all software source code in the neuroFlow repository into a single unified file
suitable for comprehensive analysis, auditing, and LLM processing.
"""

import os
import subprocess
import datetime
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
OUTPUT_FILE = REPO_ROOT / "NEUROFLOW_ALL_CODE.md"

CODE_EXTENSIONS = (".kt", ".kts", ".xml", ".properties", ".sh", ".pro", ".py")
EXCLUDED_NAMES = {"gradle-wrapper.jar"}

def get_language(filepath: str) -> str:
    if filepath.endswith((".kt", ".kts")):
        return "kotlin"
    elif filepath.endswith(".xml"):
        return "xml"
    elif filepath.endswith((".sh", "gradlew")):
        return "bash"
    elif filepath.endswith(".properties"):
        return "properties"
    elif filepath.endswith(".pro"):
        return "pro"
    elif filepath.endswith(".json"):
        return "json"
    elif filepath.endswith(".py"):
        return "python"
    return "text"

def categorize_file(filepath: str) -> tuple[int, str]:
    if filepath in ("build.gradle.kts", "settings.gradle.kts", "gradle.properties", "gradlew") or filepath.startswith("gradle/"):
        return (1, "Build & Environment Configuration")
    if filepath in ("app/build.gradle.kts", "app/proguard-rules.pro"):
        return (1, "Build & Environment Configuration")
    if filepath == "app/src/main/AndroidManifest.xml":
        return (2, "Android Manifest & Core Resources")
    if filepath.startswith("app/src/main/res/"):
        return (2, "Android Manifest & Core Resources")
    if filepath.startswith("app/src/main/java/"):
        sub = filepath.replace("app/src/main/java/com/neuroflow/app/", "")
        if sub.startswith("domain/"):
            return (3, "Main Source: Domain Layer (Engines, Models, Services)")
        elif sub.startswith("data/"):
            return (4, "Main Source: Data Layer (Room, Repositories, Telemetry)")
        elif sub.startswith("feature/"):
            return (5, "Main Source: Features (HyperFocus, Gate Control)")
        elif sub.startswith("presentation/"):
            return (6, "Main Source: Presentation & UI (Compose, ViewModels, Themes)")
        elif sub.startswith("worker/"):
            return (7, "Main Source: Background Workers & Observers")
        elif sub.startswith("kiosk/"):
            return (8, "Main Source: Device Kiosk & Lock Task Management")
        elif sub.startswith("di/") or sub.startswith("startup/"):
            return (9, "Main Source: DI & App Startup")
        else:
            return (10, "Main Source: Application Root & Entrypoints")
    if filepath.startswith("app/src/test/java/"):
        if "/e2e/" in filepath:
            return (11, "Test Suite: E2E Integration & Parity Scenarios")
        return (12, "Test Suite: Unit & Domain Architecture Tests")
    if filepath.startswith("scripts/"):
        return (13, "Automation & Provisioning Scripts")
    return (14, "Other Code & Configurations")

def main():
    os.chdir(REPO_ROOT)
    tracked_files = subprocess.check_output(
        ["git", "ls-files", "-co", "--exclude-standard"], text=True
    ).splitlines()

    code_files = []
    for f in tracked_files:
        if f in EXCLUDED_NAMES:
            continue
        if f.endswith(CODE_EXTENSIONS) or f == "gradlew":
            if os.path.isfile(f):
                code_files.append(f)

    # Sort by category order, then path
    categorized = []
    for f in code_files:
        cat_order, cat_name = categorize_file(f)
        categorized.append((cat_order, cat_name, f))

    categorized.sort(key=lambda x: (x[0], x[2]))

    # Gather stats
    total_files = len(categorized)
    total_lines = 0
    total_bytes = 0
    file_stats = []

    for _, cat_name, f in categorized:
        with open(f, "rb") as fp:
            data = fp.read()
            size = len(data)
            lines = data.count(b"\n") + (1 if data and not data.endswith(b"\n") else 0)
            total_bytes += size
            total_lines += lines
            file_stats.append((f, cat_name, lines, size))

    now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    with open(OUTPUT_FILE, "w", encoding="utf-8") as out:
        out.write("# neuroFlow (takian-alt/proFlow) — Complete Unified Codebase\n\n")
        out.write(f"**Generated:** {now}  \n")
        out.write(f"**Total Source Files:** {total_files}  \n")
        out.write(f"**Total Lines of Code:** {total_lines:,}  \n")
        out.write(f"**Total Uncompressed Size:** {total_bytes / (1024*1024):.2f} MB ({total_bytes:,} bytes)  \n\n")

        out.write("---\n\n")
        out.write("## 1. Codebase Summary by Category\n\n")

        # Category summary
        cat_summary = {}
        for _, cat_name, lines, size in file_stats:
            if cat_name not in cat_summary:
                cat_summary[cat_name] = {"files": 0, "lines": 0, "bytes": 0}
            cat_summary[cat_name]["files"] += 1
            cat_summary[cat_name]["lines"] += lines
            cat_summary[cat_name]["bytes"] += size

        out.write("| Category | Files | Lines of Code | Size (KB) |\n")
        out.write("| :--- | :---: | :---: | :---: |\n")
        for cat_name, data in cat_summary.items():
            out.write(f"| {cat_name} | {data['files']} | {data['lines']:,} | {data['bytes'] / 1024:.1f} KB |\n")
        out.write(f"| **Total** | **{total_files}** | **{total_lines:,}** | **{total_bytes / 1024:.1f} KB** |\n\n")

        out.write("---\n\n")
        out.write("## 2. File Index & Manifest\n\n")
        for f, cat_name, lines, size in file_stats:
            anchor = f.replace("/", "").replace(".", "").replace("-", "").replace("_", "").lower()
            out.write(f"- [`{f}`](#{anchor}) — *{lines:,} lines, {size/1024:.1f} KB* ({cat_name})\n")

        out.write("\n---\n\n")
        out.write("## 3. Source Code Files\n\n")

        current_category = None
        for f, cat_name, lines, size in file_stats:
            if cat_name != current_category:
                current_category = cat_name
                out.write(f"### Category: {current_category}\n\n")

            lang = get_language(f)
            anchor = f.replace("/", "").replace(".", "").replace("-", "").replace("_", "").lower()

            out.write(f"<a id=\"{anchor}\"></a>\n")
            out.write(f"#### File: `{f}`\n\n")
            out.write(f"- **Path:** `{f}`\n")
            out.write(f"- **Lines:** {lines:,}\n")
            out.write(f"- **Size:** {size:,} bytes\n")
            out.write(f"- **Language:** {lang}\n\n")

            out.write(f"````{lang}\n")
            with open(f, "r", encoding="utf-8", errors="replace") as code_in:
                code_text = code_in.read()
                out.write(code_text)
                if not code_text.endswith("\n"):
                    out.write("\n")
            out.write("````\n\n")
            out.write("---\n\n")

    print(f"Successfully generated {OUTPUT_FILE}")
    print(f"Total files: {total_files}")
    print(f"Total lines: {total_lines:,}")
    print(f"Output size: {os.path.getsize(OUTPUT_FILE) / (1024*1024):.2f} MB")

if __name__ == "__main__":
    main()
