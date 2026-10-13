# Scripts

Build-time and provisioning helpers invoked manually or from Gradle.

- `generate-baseline-profile.sh` — records and converts a Baseline Profile for release builds.
- `setup-device-owner-kiosk.sh` — provisions the test device as device owner for kiosk mode (adb, debug builds only).

Verification scripts that used to live here enforced isolation for `presentation/launcher/`, which
was archived at tag `archive/launcher-2026-10-12` and deleted by the HyperFocus black-home redesign.

## Adding new checks

To add additional verification scripts:

1. Create a new `.sh` file in this directory
2. Make it executable: `chmod +x scripts/your-script.sh`
3. Register it as a Gradle task in `app/build.gradle.kts` or a separate `.gradle.kts` file
4. Add it as a dependency of the `check` task
5. Add it to the list above
