# Baseline Profile for proFlow

## Overview

This directory holds the baseline profile generator for the HyperFocus black home. The profile causes
ART to AOT-compile the code the home screen runs on its first frame, removing JIT warm-up work on
launch. The composition-time target below has never been measured on a device — treat it as intent,
not a result.

## Critical Paths Covered

The generator (`HyperFocusBaselineProfileGenerator.kt`) covers:

1. **HyperFocusHomeActivity cold start**
   - Activity initialization
   - `HyperFocusHomeViewModel` creation
   - First read of the allowlist preferences

2. **Essentials grid composition**
   - `RealHomeResolver` deciding the surface state
   - Tile rendering for the pinned essential apps

3. **Icon resolution during scroll**
   - Per-tile `PackageManager` icon lookup

The launcher surfaces this document used to list — app drawer, dock, focus task card, quick-stats
overlay — were archived at tag `archive/launcher-2026-10-12` and deleted by Task 17, so they are no
longer profiled.

## Requirements

### Device Requirements
- Android device or emulator running API 26+
- **Recommended**: a low/mid-tier device, since warm-up cost is what is being measured
- Device should be rooted or using a userdebug build for best results
- proFlow should be set as the default home app (Settings → Default apps → Home app) while a session
  is active, which is what makes `pressHome()` land on the black home

### Build Requirements
- Gradle 8.7.3+
- Android Gradle Plugin 8.7.3+
- Baseline Profile Gradle Plugin 1.2.4+

## Generating the Baseline Profile

### Method 1: Using the Script (Recommended)

```bash
./scripts/generate-baseline-profile.sh
```

### Method 2: Manual Gradle Command

```bash
./gradlew :app:generateBaselineProfile
```

### Method 3: Android Studio

1. Open the project in Android Studio
2. Connect a device or start an emulator
3. Open the Gradle tool window
4. Navigate to: app → Tasks → other → generateBaselineProfile
5. Double-click to run

## What Happens During Generation

1. The test builds the app in release mode
2. Installs the app on the connected device
3. Runs the `HyperFocusBaselineProfileGenerator` test
4. The test performs the following actions:
   - Launches the app (cold start)
   - Waits for the black home to render
   - Scrolls the essentials grid down and back up
5. Collects profiling data across 5 iterations (3 stable)
6. Generates `baseline-prof.txt` in `app/src/main/`

## Verifying the Profile

After generation, verify the profile is working:

```bash
# Build release APK
./gradlew :app:assembleRelease

# Install on device
adb install app/build/outputs/apk/release/app-release.apk

# Launch the black home and measure its first frame
```

## About the checked-in `app/src/main/baseline-prof.txt`

That file was generated against the launcher. Task 17 deleted those classes, so the entries naming
them were removed rather than renamed — a profile rule for a class that does not exist is dead weight
in every APK. `TaskScoringEngine` is the one entry that still names live code. **The file is therefore
stale in the other direction too: it no longer covers the black home.** Regenerate it on a device
before trusting startup performance numbers from it.

## Troubleshooting

### "Could not find or load main class org.gradle.wrapper.GradleWrapperMain"
- The Gradle wrapper may be corrupted
- Regenerate the wrapper: `gradle wrapper --gradle-version 8.7.3`

### "No connected devices"
- Connect an Android device via USB with USB debugging enabled
- Or start an Android emulator

### "Baseline profile generation failed"
- Ensure proFlow is set as the default home app and a session is active
- Ensure the device is not locked
- Check logcat for errors: `adb logcat | grep BaselineProfile`

### "Profile is empty or incomplete"
- The test may have failed to interact with the UI
- Check that the app is installed and set as the default home app
- Verify the allowlist has at least one essential app pinned, otherwise the black home shows the
  activation prompt instead of a grid

## Performance Targets

Unmeasured on this project so far — no device has run either the generator or the profile:

- **Home screen composition**: < 100ms on a mid-tier device (target, not a result)
