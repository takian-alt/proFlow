#!/usr/bin/env python3
"""
Android Version Bump Utility
Safely inspects and updates versionCode and versionName in Android Gradle files
(supports both build.gradle.kts and build.gradle).

By default, increments versionCode (+1) and automatically bumps versionName
to preserve synchronized release numbering.

Usage:
    bump_version.py [--file PATH] [--bump {auto,patch,minor,major}]
                    [--code-only] [--name-only]
                    [--version-code CODE] [--version-name NAME]
                    [--info] [--dry-run] [--json]
"""

import argparse
import json
import os
import re
import sys
from pathlib import Path

VERSION_CODE_PATTERNS = [
    re.compile(r'^(?P<prefix>\s*versionCode\s*=\s*)(?P<code>\d+)(?P<suffix>.*)$'),
    re.compile(r'^(?P<prefix>\s*versionCode\s+)(?P<code>\d+)(?P<suffix>.*)$'),
]

VERSION_NAME_PATTERNS = [
    re.compile(r'^(?P<prefix>\s*versionName\s*=\s*["\'])(?P<name>[^"\']+)(?P<suffix>["\'].*)$'),
    re.compile(r'^(?P<prefix>\s*versionName\s+["\'])(?P<name>[^"\']+)(?P<suffix>["\'].*)$'),
]

def find_gradle_file(explicit_path=None) -> Path:
    if explicit_path:
        p = Path(explicit_path)
        if not p.is_file():
            raise FileNotFoundError(f"Specified gradle file not found: {explicit_path}")
        return p

    candidates = [
        Path("app/build.gradle.kts"),
        Path("app/build.gradle"),
        Path("build.gradle.kts"),
        Path("build.gradle"),
    ]
    for c in candidates:
        if c.is_file():
            return c

    # Search upwards for repo root
    current = Path.cwd()
    for parent in [current] + list(current.parents):
        for candidate in [parent / "app/build.gradle.kts", parent / "app/build.gradle"]:
            if candidate.is_file():
                return candidate

    raise FileNotFoundError("Could not locate app/build.gradle.kts or app/build.gradle")

def parse_versions(content: str):
    code = None
    name = None

    for line in content.splitlines():
        if code is None:
            for pat in VERSION_CODE_PATTERNS:
                m = pat.match(line)
                if m:
                    code = int(m.group("code"))
                    break
        if name is None:
            for pat in VERSION_NAME_PATTERNS:
                m = pat.match(line)
                if m:
                    name = m.group("name")
                    break

    return code, name

def bump_semver(current_name: str, part: str = "auto") -> str:
    """
    Increments semver strings like '20.0', '1.2.3', 'v2.1'.
    part: 'auto', 'patch', 'minor', 'major'.
    In 'auto' mode, bumps the lowest existing numeric segment:
      '20.0' -> '20.1'
      '1.0.0' -> '1.0.1'
      '2' -> '3'
    """
    prefix = "v" if current_name.startswith("v") else ""
    raw = current_name.lstrip("v")
    tokens = raw.split(".")

    numeric = []
    suffix = ""
    for t in tokens:
        m = re.match(r'^(\d+)(.*)$', t)
        if m:
            numeric.append(int(m.group(1)))
            if m.group(2):
                suffix = m.group(2)
        else:
            suffix = t

    if not numeric:
        numeric = [1]

    if part == "major":
        numeric[0] += 1
        for i in range(1, len(numeric)):
            numeric[i] = 0
    elif part == "minor":
        if len(numeric) < 2:
            numeric.append(1)
        else:
            numeric[1] += 1
            for i in range(2, len(numeric)):
                numeric[i] = 0
    elif part == "patch":
        if len(numeric) < 3:
            numeric.append(1)
        else:
            numeric[2] += 1
            for i in range(3, len(numeric)):
                numeric[i] = 0
    elif part == "auto":
        # Increments the lowest-order existing numeric segment
        numeric[-1] += 1
    else:
        raise ValueError(f"Unknown semver bump part: {part}")

    new_str = ".".join(str(n) for n in numeric)
    if suffix and not suffix.startswith("-"):
        suffix = "-" + suffix
    return f"{prefix}{new_str}{suffix}"

def update_content(content: str, new_code: int = None, new_name: str = None) -> str:
    lines = content.splitlines(keepends=True)
    out_lines = []

    for line in lines:
        updated = False

        if new_code is not None:
            for pat in VERSION_CODE_PATTERNS:
                m = pat.match(line)
                if m:
                    line = f"{m.group('prefix')}{new_code}{m.group('suffix')}\n"
                    updated = True
                    break

        if not updated and new_name is not None:
            for pat in VERSION_NAME_PATTERNS:
                m = pat.match(line)
                if m:
                    line = f"{m.group('prefix')}{new_name}{m.group('suffix')}\n"
                    updated = True
                    break

        out_lines.append(line)

    return "".join(out_lines)

def main():
    parser = argparse.ArgumentParser(description="Android Version Code & Name Auto-Bumper")
    parser.add_argument("--file", "-f", help="Path to build.gradle[.kts] (auto-detected if omitted)")
    parser.add_argument("--info", action="store_true", help="Only display current version info without updating")
    parser.add_argument("--bump", "-b", choices=["auto", "patch", "minor", "major"], default="auto",
                        help="Semver bump strategy for versionName (default: auto)")
    parser.add_argument("--code-only", action="store_true", help="Only increment versionCode; keep versionName unchanged")
    parser.add_argument("--name-only", action="store_true", help="Only bump versionName; keep versionCode unchanged")
    parser.add_argument("--version-code", "-c", type=int, help="Explicitly set new versionCode")
    parser.add_argument("--version-name", "-n", type=str, help="Explicitly set new versionName")
    parser.add_argument("--dry-run", "-d", action="store_true", help="Preview changes without writing to file")
    parser.add_argument("--json", action="store_true", help="Output result as JSON")

    args = parser.parse_args()

    try:
        target_file = find_gradle_file(args.file)
    except FileNotFoundError as e:
        if args.json:
            print(json.dumps({"error": str(e)}))
        else:
            print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)

    content = target_file.read_text(encoding="utf-8")
    curr_code, curr_name = parse_versions(content)

    if curr_code is None:
        err = f"Could not find 'versionCode' in {target_file}"
        if args.json:
            print(json.dumps({"error": err}))
        else:
            print(f"Error: {err}", file=sys.stderr)
        sys.exit(1)

    # If --info requested or (--json alone with no modification flags), just report current info
    is_query_only = args.info or (args.json and not args.dry_run and args.version_code is None and args.version_name is None and not args.code_only and not args.name_only and args.bump == "auto" and "--bump" not in sys.argv)

    if is_query_only:
        payload = {
            "file": str(target_file),
            "versionCode": curr_code,
            "versionName": curr_name,
            "changed": False
        }
        if args.json:
            print(json.dumps(payload, indent=2))
        else:
            print(f"Current version in {target_file}:")
            print(f"  versionCode: {curr_code}")
            print(f"  versionName: {curr_name}")
        return

    # Determine desired changes (By default: BOTH versionCode and versionName are bumped)
    new_code = curr_code
    new_name = curr_name

    # 1. Version Code resolution
    if args.version_code is not None:
        new_code = args.version_code
    elif not args.name_only:
        new_code = curr_code + 1

    # 2. Version Name resolution
    if args.version_name is not None:
        new_name = args.version_name
    elif not args.code_only and curr_name is not None:
        new_name = bump_semver(curr_name, args.bump)

    payload = {
        "file": str(target_file),
        "previous": {
            "versionCode": curr_code,
            "versionName": curr_name
        },
        "target": {
            "versionCode": new_code,
            "versionName": new_name
        },
        "changed": (curr_code != new_code or curr_name != new_name),
        "dryRun": args.dry_run
    }

    if payload["changed"] and not args.dry_run:
        new_content = update_content(content, new_code=new_code, new_name=new_name)
        # Verify changes took effect
        test_code, test_name = parse_versions(new_content)
        if test_code != new_code or (new_name is not None and test_name != new_name):
            err = "Failed to update version fields safely in file content."
            if args.json:
                print(json.dumps({"error": err}))
            else:
                print(f"Error: {err}", file=sys.stderr)
            sys.exit(1)

        # Write safely
        target_file.write_text(new_content, encoding="utf-8")

    if args.json:
        print(json.dumps(payload, indent=2))
    else:
        status_suffix = " (DRY RUN)" if args.dry_run else ""
        if payload["changed"]:
            print(f"Successfully updated {target_file}{status_suffix}:")
            if curr_code != new_code:
                print(f"  versionCode: {curr_code} -> {new_code}")
            if curr_name != new_name:
                print(f"  versionName: {curr_name} -> {new_name}")
        else:
            print(f"Current version in {target_file}:")
            print(f"  versionCode: {curr_code}")
            print(f"  versionName: {curr_name}")

if __name__ == "__main__":
    main()
