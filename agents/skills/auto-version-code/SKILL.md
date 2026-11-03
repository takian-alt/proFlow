---
name: auto-version-code
description: >-
  Automatically inspects, increments, and manages Android versionCode and versionName in app/build.gradle.kts.
  Use when asked to "bump version", "update version code", "increment versionCode", "prepare release build",
  or tag a new version in this Android repository.
---

# Auto Version Code & Version Management Skill

## 1. Overview & Purpose
This skill provides deterministic, safe automation for managing Android `versionCode` and `versionName` in `app/build.gradle.kts` (or `app/build.gradle`).

By default, executing this skill updates **both** `versionCode` ($+1$) and `versionName` (lowest semver token increment, e.g. `20.0` $\to$ `20.1`) in lockstep.

It guarantees compliance with Android and Google Play Store distribution invariants:
1. **Monotonic Integer Invariant**: `versionCode` MUST strictly increment ($v_{n+1} > v_n$) on every published release. Reusing or decrementing a `versionCode` breaks APK/AAB upload pipelines.
2. **Synchronized Version Name**: `versionName` is automatically kept in sync with the release cycle unless `--code-only` is explicitly requested.
3. **Format Preservation**: Edits are applied cleanly with exact indentation and comment preservation without breaking Kotlin Gradle DSL syntax.
4. **Build & Test Verification**: Validates syntax immediately after bumping to ensure compilation is 100% green before commits or tags are generated.

---

## 2. Bundled Script: `bump_version.py`

Located at: `.agents/skills/auto-version-code/scripts/bump_version.py`

### Common CLI Invocations

```bash
# 1. Query current version without altering files (JSON output)
.agents/skills/auto-version-code/scripts/bump_version.py --json

# 2. Preview standard version code + version name bump (DRY RUN)
.agents/skills/auto-version-code/scripts/bump_version.py --dry-run

# 3. Standard update: Increments versionCode (+1) AND bumps versionName (e.g., 20.0 -> 20.1)
.agents/skills/auto-version-code/scripts/bump_version.py

# 4. Bump minor release explicitly (e.g., 20.0 -> 20.1, increments versionCode)
.agents/skills/auto-version-code/scripts/bump_version.py --bump minor

# 5. Bump patch release (e.g., 20.0 -> 20.0.1, increments versionCode)
.agents/skills/auto-version-code/scripts/bump_version.py --bump patch

# 6. Bump major release (e.g., 20.0 -> 21.0, increments versionCode)
.agents/skills/auto-version-code/scripts/bump_version.py --bump major

# 7. Only increment versionCode (leave versionName untouched)
.agents/skills/auto-version-code/scripts/bump_version.py --code-only

# 8. Explicitly set version parameters
.agents/skills/auto-version-code/scripts/bump_version.py --version-code 22 --version-name "20.1"
```

---

## 3. Standard Operating Procedure for Release Version Bumps

When the user requests a version bump or release preparation, follow this 4-step sequence:

### Step 1: Inspect Working Tree & Current Version
Verify the git status and display the current version code:
```bash
git status -s
.agents/skills/auto-version-code/scripts/bump_version.py --json
```

### Step 2: Execute Version Bump
Depending on user intent:
```bash
# Standard auto update (bumps both versionCode and versionName):
.agents/skills/auto-version-code/scripts/bump_version.py

# Or if a specific semver tier is requested:
.agents/skills/auto-version-code/scripts/bump_version.py --bump minor # or patch/major
```

### Step 3: Run Build & Sanity Check
Ensure the Gradle file parses cleanly and the compilation succeeds under neuroFlow memory invariants:
```bash
./gradlew checkDebugAarMetadata --no-daemon -Dorg.gradle.workers.max=1
```

### Step 4: Commit & Tag (If Requested)
When the user requests committing or tagging the release:
```bash
NEW_VERSION=$(.agents/skills/auto-version-code/scripts/bump_version.py --json | grep '"versionName"' | head -1 | awk -F'"' '{print $4}')
NEW_CODE=$(.agents/skills/auto-version-code/scripts/bump_version.py --json | grep '"versionCode"' | head -1 | awk -F': ' '{print $2}' | tr -d ',')

git add app/build.gradle.kts
git commit -m "chore(release): bump version to v${NEW_VERSION} (code ${NEW_CODE})"
git tag -a "v${NEW_VERSION}" -m "Release v${NEW_VERSION} (build ${NEW_CODE})"
```

---

## 4. Invariants & Safety Guardrails
- **Host Memory Safety**: Always append `--no-daemon -Dorg.gradle.workers.max=1` to any verification gradle command.
- **Never Decrement `versionCode`**: If the user asks to downgrade `versionName` (e.g., reverting to 19.5), ensure `versionCode` still increments so Google Play does not reject the package.
- **Diff Verification**: Always perform `git diff app/build.gradle.kts` after execution to confirm only `versionCode` and/or `versionName` were altered.
