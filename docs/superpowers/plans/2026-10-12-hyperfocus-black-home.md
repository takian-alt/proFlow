# HyperFocus Black-Home Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace NeuroFlow's launcher product with a single HyperFocus-only black home screen that shows a global two-tier app allowlist, where every hidden app is reachable only through a per-app pass granted from behind a cognitive gate.

**Architecture:** Three phases, each ending in a green build. Phase 1 moves HyperFocus out of `presentation/launcher/` into `feature/hyperfocus/` with zero behavior change, so the launcher can later be deleted without dragging the focus engine with it. Phase 2 inverts the denylist (`blockedPackages`) into an allowlist (`essentialPackages` ∪ `allowedPackages`), adds the black home activity, the gate, the gateway, and the per-app pass lifecycle, and re-points every enforcement path at the new model. Phase 3 tags the launcher and deletes the 37 files that remain under `presentation/launcher/` plus their 8 test files.

**Tech Stack:** Kotlin, Jetpack Compose (BOM 2024.12.01), Hilt 2.51.1 (KSP), Room 2.6.1, DataStore Preferences 1.1.1, WorkManager 2.10.0, Navigation Compose 2.8.5, kotest 5.9.1 `StringSpec` + MockK 1.13.12, Android `AccessibilityService` + `LauncherApps` + `DevicePolicyManager`.

**Spec:** `docs/superpowers/specs/2026-10-12-hyperfocus-black-home-design.md` — read it before starting. This plan argues from that spec; decisions A/B2/D3/E1/F4/G1 are locked there and are **not** re-opened below.

## Global Constraints

- **Locked decisions — do not re-litigate:** A (one permanent `CATEGORY_HOME` activity), B2 (two global allowlist tiers, not per-session), D3 (unlock is a per-app pass), E1 (pass budget is wall-clock, never refunded, ends when the granted app leaves the foreground), F4 (randomized effort-only cognitive task + written reason, logged every request), G1 (git-only archive: extract → invert → delete behind a tag).
- **minSdk 26, compileSdk 35, targetSdk 35, JVM 17.** No new dependencies.
- **New unit tests must be kotest `StringSpec`.** `app/build.gradle.kts:68-75` sets `testOptions.unitTests.all { it.useJUnitPlatform() }`, which makes JUnit4 `@Test` classes and Robolectric **inert** in `app/src/test` — a JUnit4 test there compiles, silently never runs, and looks green. Any logic that needs an Android `Context` must be split so the decision-making is a pure JVM function under test, with a thin untested Android shell.
- **Work in the isolated worktree, never in `/home/takian/Project/PROCUS`.** This plan is executed from a linked worktree; every path here is relative to the repo root of *your* workspace (`git rev-parse --show-toplevel`). An absolute `cd` into the main checkout would move and delete files in the user's working tree.
- **Verify command for every task:** `bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh assemble`, plus `verify.sh testone "<fqcn>"` for tasks that add or change a suite, and `verify.sh tests` at the phase gates. The script `cd`s to its own worktree, so it works from any directory; the other commands in a step that use relative paths still need the repo root (`cd "$(git rev-parse --show-toplevel)"`). **Never call `./gradlew` directly, and never "fix" a wrapper error by re-quoting it:** `gradlew:105` ends with `… GradleWrapperMain \"\$@\""`, which puts every CLI argument after the first into one quoted cluster, so a wrapper call for `:app:testDebugUnitTest --tests "com.foo.BarTest"` reaches Gradle as the single task name `:app:testDebugUnitTest --tests com.foo.BarTest` → *task not found*. Commit `b478278` ("Fix Gradle wrapper quoting issue in gradlew script") added a comment describing this exact bug and left the quoting unchanged, so it is broken at every commit on this branch — a single-argument call is the only form that works. The `--tests=<pattern>` spelling does reach Gradle but then fails inside the JUnit Platform engine with *no tests found to execute*, so it is not a workaround either. Mapping: `:app:assembleDebug` → `verify.sh assemble`, `:app:testDebugUnitTest` → `verify.sh tests`, `:app:testDebugUnitTest --tests X` → `verify.sh testone "X"`, `:app:compileDebugAndroidTestKotlin` → `verify.sh atcompile`, `:app:lintDebug` → `verify.sh lint`, `:app:clean` → `verify.sh clean`, `--stop` → `verify.sh stop`.
- **Commits must be pathspec-limited. Never a bare `git commit`.** The user keeps ~96 files permanently staged in this tree; a plain commit sweeps their work into mine. Form:
  ```bash
  git add <exact paths> && git commit -q -m "<message>" -- <exact same paths>
  git show --name-only HEAD   # expect ONLY my paths
  ```
  A `needs merge` line on stderr is expected and benign (two stale `DU` entries in `docs/book/`); confirm success with `git show --name-only HEAD`, never by the exit message. Never `git add -A`, never `--amend`, never `--no-verify`, never `git stash`/`checkout`/`reset`.
- **A move is one commit: `git add` the new paths, and put BOTH the new and the old paths in the *commit* pathspec.** `git mv` consumes the old path's index entry, so `git add -- <old path>` afterwards fails with *pathspec did not match any files* — do not put old paths in the `git add` list. The commit pathspec is matched against HEAD, not the index, so naming the old path there records its deletion in the same commit (verified in a throwaway repo: `git mv` → `git add <new>` → `git commit -- <new> <old>` yields one commit, `R066 old -> new`, with only the new path in the tree). Omit it and HEAD keeps two copies of the class: `:app:assembleDebug` still passes (main sources resolve to the new package), and only `:app:compileDebugUnitTestKotlin` fails. Always run the gate after committing: `git ls-tree -r --name-only HEAD <old path> | grep . && echo DEFECT || echo "old paths gone"`. If it prints DEFECT, the staged deletion is still in the index, so commit it on its own naming the old path — a second commit, never an amend. Tasks 1 and 2 each produced a DEFECT and needed exactly that follow-up (`e68088f`→`a5bbaa9`, `4f0f1d2`→`d8e4cec`); Task 3 produced a third (`8ec4827`→`eb0e438`) before this recipe was measured. A task that moves files is not done until the gate prints `old paths gone`.
- **`BUILD SUCCESSFUL` is not evidence that tests ran.** `:app:testDebugUnitTest` reports `NO-SOURCE` and succeeds with zero tests executed whenever `app/build` is emptied under a live daemon (the ASM transform's cached output comes back empty — observed 2026-10-13 on this plan: 345 tests on disk, task reported NO-SOURCE, build green). Trust only a printed test count parsed from `app/build/test-results/testDebugUnitTest/TEST-*.xml`, which `verify.sh tests` now does; it retries with `--no-build-cache` on NO-SOURCE and fails at 0. A *decrease* in the count versus the ledger's baseline is a dropped suite, and a run that executes 311 instead of 345 still looks green — so compare the number, don't just check it is nonzero.
- **Every kotest matcher named in a task's test code must exist in 5.9.1 — verified against the sources jar, not from memory.** `io.kotest.matchers.strings.shouldHaveLengthGreaterThan` appeared in Task 6's draft and does **not** exist (kotest 5.9.1 ships `shouldHaveLength`, `shouldHaveLengthBetween`, `shouldHaveLengthIn` in `io.kotest.matchers.string`, singular). GT-2's replacement was itself replaced in Task 6's fix round: it now asserts `task.prompt.contains(task.expected.first()) shouldBe true`, because the `prompt.length in 4..Int.MAX_VALUE` form the correction first used turned out to be a tautology that could never fail — which is exactly why the suite stayed green while two of three task types rendered no stimulus at all. A nonexistent import fails the whole test-source set with *unresolved reference*, which is indistinguishable from the intended red state in Step 2, so an implementer can burn a fix round on it. Check with: `unzip -o -q <kotest-assertions-core-jvm-5.9.1-sources.jar> -d /tmp/ktm` then grep that tree.
- **`SystemExempt` has two roles and passing the wrong one is a bug: `packages()` for enforcement, `SystemExempt.STATIC` for anything the user can see or select.** `packages()` = `STATIC` + `CALL_PATH` (the dialers, added in Task 5's fix round for spec §6.2) + self + live IME packages, and it is the only correct argument for `FocusSurfaceModel.resolvedAllowlist`/`isGated` (Tasks 12, 13, 14's `hiddenApps`). `STATIC` is the never-gated **and** never-shown machinery set — OS internals, permission controllers, installers, Settings and its vendor twins — and it is the correct argument for `renderableEssentials`/`homeSurfaceStateOf` (Task 15 grid), `decideActivation` (Task 16, both call sites), `editableApps` (Task 16 picker) and `AllowlistMigrator.derive` (Task 11). All five were corrected in the plan after Task 5's re-review: with `packages()` there, the phone vanishes from the home grid, a dialer-only essentials tier is rejected as `EmptySurface`, the phone cannot be pinned in the editor, and the Pixel dialer (`com.google.android.dialer`, which is not in the seed) lands in neither tier while the footer counts an always-reachable app as unreachable. A new call site must choose by question: *does this argument decide whether something may run (packages) or whether it is displayed (STATIC)?*
- **Phase 3 is gated on the user:** creating the `archive/launcher-2026-10-12` tag and deleting the 37 remaining launcher files (45 with their tests) is irreversible-in-practice. Task 17 stops and asks before the delete lands.
- **HOME-role behavior cannot be verified on this machine** (no AVD in use for this project, `adb` not on `PATH`). Tasks that end in device-only behavior must report the item as **unverified**, never as working. Full list at the end of this plan.
- **The target device is an active device *administrator*, not a device owner** (user, 2026-10-13). This is not a small downgrade: every `isDeviceOwnerApp` guard in `DeviceOwnerKioskManager` (`:134`, `:258`, `:291`) returns early, so `setPackagesSuspended`, `addPersistentPreferredActivity`, the kiosk user restrictions and lock-task are **dead code on this device**. Two consequences shape Phase 2. (1) Task 12's accessibility service is the *only* enforcement, so a blocked app's window is genuinely visible for the interval between its `TYPE_WINDOW_STATE_CHANGED` event and the overlay — the allowlist is reactive, not preemptive. (2) The black home is HOME only because the user picks it in **Settings → Default apps → Home app**; nothing pins it, and clearing defaults is an escape the code cannot close. Therefore: no user-visible string may describe suspension as protection, and a `dpm` call returning without exception is never evidence that anything happened.
- **A HOME-role regression and a package-move regression must never share a commit** (G1). Phase 1 changes imports only; Phase 2 owns the manifest.
- **A predicate that gates an action must not consume state that only that action can advance.** Task 6's review caught the first version of this rule and its fix round caught a second, subtler case, so it is now a constraint rather than a comment: `GateTaskEngine.allowsSubmit` decides whether the Continue button is clickable, and `mismatches` increments only inside that button's `onClick` — so *any* clause on the count in `allowsSubmit` is a deadlock, not a difficulty. `< MAX_MISMATCHES` lets a wrong answer through; `% MAX_MISMATCHES` (the version that shipped at `6da749c`) shuts again at 4 and 5 and strands anyone who reaches them; `>= MAX_MISMATCHES` shuts at 1 and 2 and strands anyone who mistypes once, which is most users. Only the last of the three is caught by reading the predicate alone — you have to follow it into the UI to see it. The same shape is live in the gate's neighbours: `elapsedMillis >= MIN_ELAPSED_MILLIS` is fine *because* the button's own click cannot be the only thing that advances elapsed time, and `isReasonValid` is fine because the user types it. When a later task adds a counter that a screen advances (the `hiddenApps` retry count, a pass-grant count), it belongs in the grant decision (`shouldAutoPass`/`grantPass`), never in the guard that enables the click.
- **`launchableApps()` is complete by construction: no namespace or package-prefix rule may drop an app from it.** `LauncherApps.getActivityList` already returns only packages that have a real launcher activity, so a `com.android.` prefix test cannot hide the internals it is named after — `SystemExempt.STATIC` re-admits every one of them via the exempt clause — and its only reachable effect is to hide *user* apps in that namespace: `com.android.chrome`, `com.android.vending` (Play Store), and OEM gallery/email/filemanager variants, none of which is in `STATIC`, `CALL_PATH` or the essential seed. Because Task 11's migrator tiers only `launchable` packages and Task 16's picker is fed by the same list, a package dropped here has **no route back**: it is invisible forever, which is the exact defect B2's allowlist was chosen to eliminate. Hiding is `FocusSurfaceModel`'s single job (`isGated`), and the display sites filter with `STATIC`; the catalog is not where it happens. Added after Task 7's review, which traced the consequence end to end.

## File structure after Phase 2

```
com/neuroflow/app/
  feature/hyperfocus/
    home/        HyperFocusHomeActivity  HyperFocusHomeViewModel  HyperFocusHomeScreen
                 HomeSurfaceState  RealHomeChoice  RealHomeResolver
    gate/        GateTaskEngine  GateTaskScreen  GatewayScreen  EmergencyControl
    surface/     FocusSurfaceModel  SystemExempt  AllowlistEditor
    data/        HyperFocusDataStore  HyperFocusSessionDao/Entity  UnlockCodeDao/Entity
                 GateRequestDao/Entity  AppCatalogRepository  AppCatalogFilter
    domain/      HyperFocusManager  RewardEngine  UnlockCodeRepository  AESUtil
                 ActivationGuard
    service/     AppBlockingService  HyperFocusMonitorService  UnlockTimerService
    screens/     BlockingOverlayScreen  CodeEntryScreen  HyperFocusActivationSheet
                 PermissionSetupScreen  PlanningPromptScreen  RewardSection  RewardsScreen
                 AllowlistSettingsScreen
    components/  HyperFocusStatusBar
    util/        AccessibilityUtil
    di/          HyperFocusModule
  worker/        ScheduleAutoTasksWorker  (moved in from launcher/work)
  data/badge/    NotificationBadgeManager  NotificationBadgeService  (moved in from launcher/data)
```

Five files carry the decisions most likely to be misread by a later editor, so each gets one job: `FocusSurfaceModel` is the only place that answers "is this package allowed right now?"; `GateTaskEngine` is the only place that answers "has this user paid the gate?"; `RewardEngine`'s two pass functions are the only place that answers "is there a live pass?"; `AppBlockingService` consumes those three and adds no policy of its own; `HyperFocusHomeScreen` renders the resolved allowlist and nothing else.

## Spec deltas found while planning

Each is a consequence the spec did not trace, or a place the spec is silent and this plan has to pick. None contradicts a locked decision. If the user overrules one, the task named here is the only place to change.

1. **Spec §3.3 and §11 delete `NotificationBadge*`, but `EnergyScoreRepository.kt:30,150` consumes `notificationBadgeManager.badgeCounts` as a scoring input, and `EnergyScoreRepositoryContractTest.kt:13` injects it.** Deleting the manager would either break the build or silently drop the notification signal from the energy model (`EnergyScoreRepository.kt:188`) — a behavior change the spec never mentions. Task 3 therefore **relocates** `NotificationBadgeManager` + `NotificationBadgeService` to `data/badge/` and keeps them. What is deleted with the launcher is the badge-rendering feature (icons drawing dots), which lives in `AppIcon.kt` and `PinnedAppsDataStore` and genuinely is launcher-only.
2. **Spec §3.1 puts `HyperFocusModule` inside `feature/hyperfocus/`; Phase 3 deletes `LauncherModule`.** `LauncherModule` also provides `PackageManager`, `PinnedAppsDataStore`, `AppWidgetHostWrapper`, `AppRepository`, `IconPackManager`, `LauncherBackupManager`, so the split has to happen in Phase 1 (Task 1) or HyperFocus stops compiling the moment the launcher goes. Task 1 creates `feature/hyperfocus/di/HyperFocusModule.kt` holding only HyperFocus bindings and leaves the launcher providers in a trimmed `LauncherModule`.
3. **`activeUnlockExpiresAt` becomes vestigial and is deleted (Task 8), not renamed.** Spec §4.3 describes it as the unlock deadline; once D3 makes validity `activePassPackage != null && activePassExpiresAt > now`, a second timestamp with no package attached cannot be distinguished from a stale one. Task 8 deletes the field and every reader rather than keeping two clocks that can disagree.
4. **The denylist column in the session history survives (Task 16).** `HyperFocusSessionEntity.blockedPackages` and `CREATE TABLE hyperfocus_sessions … blockedPackages TEXT NOT NULL` (`NeuroFlowDatabase.kt:124`) stay, because grep shows nothing in the app writes that table — only `FirestoreBackupRepository.kt:730,739` map it for cloud restore — so a migration that drops the column would rewrite archived sessions for no behavioral gain. Spec §11's "remove the denylist" is about the live policy, not the audit row.
5. **An essentials-empty but allowed-full configuration is refused at activation (Task 16, AG-3).** Spec §8 says "allowlist empty at activation → blocked", without saying which tier has to be non-empty. This plan reads §5.2's promise ("the home shows your essential apps") as requiring a visible tile, so `decideActivation` keys off `renderableEssentials`, and an allowed-only surface gets an explicit message instead of a session whose home screen is empty.

---

# Phase 1 — Extraction (no behavior change)

Three tasks. Each is a `git mv` plus import rewrites plus a green build. If any task in this phase changes runtime behavior, it is wrong — stop and redo it as a pure move.

## Task 1: Move HyperFocus to `feature/hyperfocus/` and split the DI module

**Files:**
- Move: all 23 files under `app/src/main/java/com/neuroflow/app/presentation/launcher/hyperfocus/` → `app/src/main/java/com/neuroflow/app/feature/hyperfocus/` (same subdirectory names; add a new `di/`)
- Create: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/di/HyperFocusModule.kt`
- Modify: `app/src/main/java/com/neuroflow/app/presentation/launcher/di/LauncherModule.kt` (drop the HyperFocus bindings now provided elsewhere)
- Modify (import sites, verified by grep — these are all of them): `data/local/NeuroFlowDatabase.kt:24-27`, `data/repository/TaskRepository.kt:13`, `data/sync/CloudBackupManager.kt:14`, `data/sync/FirestoreBackupRepository.kt:23`, `di/SyncModule.kt:18`, `NeuroFlowApplication.kt:12`, `presentation/common/NeuroFlowApp.kt:66`, `receiver/BootReceiver.kt:15,108`, `worker/AccessibilityWatchdogWorker.kt:14,49`, `kiosk/DeviceOwnerKioskManager.kt` (fully-qualified `presentation.launcher.hyperfocus` references, if any — grep in step 4)
- Modify: `app/src/main/AndroidManifest.xml:190,200,213,223` (activity + three service `android:name` values)
- Move: `app/src/test/java/com/neuroflow/app/presentation/launcher/hyperfocus/**` (5 files) → `app/src/test/java/com/neuroflow/app/feature/hyperfocus/**`
- Modify: `app/src/test/java/com/neuroflow/app/data/repository/TaskRepositoryEstimateEditTest.kt:6`, `TaskRepositoryRecurringTest.kt:9`, `app/src/test/java/com/neuroflow/app/data/sync/CloudBackupManagerTest.kt:15`

**Interfaces:**
- Consumes: nothing new.
- Produces: every HyperFocus type at its `com.neuroflow.app.feature.hyperfocus.*` path, with identical class names, signatures and behavior. Later tasks refer only to the new paths. `HyperFocusModule` binds `HyperFocusManager`, `UnlockCodeRepository` and provides `HyperFocusDataStore`, `UnlockCodeDao`, `HyperFocusSessionDao`.

- [ ] **Step 1: Move the main-source package with git, preserving history**

```bash
cd "$(git rev-parse --show-toplevel)"   # never an absolute path: this runs in a worktree
mkdir -p app/src/main/java/com/neuroflow/app/feature
git mv app/src/main/java/com/neuroflow/app/presentation/launcher/hyperfocus \
       app/src/main/java/com/neuroflow/app/feature/hyperfocus
mkdir -p app/src/main/java/com/neuroflow/app/feature/hyperfocus/di
```

`git mv` on a directory inside a dirty tree is safe: it renames tracked paths and leaves the staged contents of the moved files untouched. Do not follow up with `git checkout` or `git reset`.

- [ ] **Step 2: Rewrite the package declaration and internal imports in the moved tree**

Every moved file declares `package com.neuroflow.app.presentation.launcher.hyperfocus[.sub]` and imports its siblings by fully-qualified path. Two substitutions cover the whole tree:

```bash
cd app/src/main/java/com/neuroflow/app/feature/hyperfocus
grep -rl "presentation\.launcher\.hyperfocus" . | while read -r f; do
  sed -i 's/presentation\.launcher\.hyperfocus/feature.hyperfocus/g' "$f"
done
cd "$(git rev-parse --show-toplevel)"
grep -rn "presentation\.launcher\.hyperfocus" app/src/main/java/com/neuroflow/app/feature/ || echo "CLEAN"
```

Expected: `CLEAN`. The substitution also fixes in-body fully-qualified references such as `HyperFocusManager.kt:76` (`...hyperfocus.service.HyperFocusMonitorService::class.java`) and `:196`.

- [ ] **Step 3: Rewrite the nine external main-source files**

Apply the same substitution to each of the import sites listed under **Files** — they only ever *import* HyperFocus, never subclass it:

```bash
for f in app/src/main/java/com/neuroflow/app/data/local/NeuroFlowDatabase.kt \
         app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt \
         app/src/main/java/com/neuroflow/app/data/sync/CloudBackupManager.kt \
         app/src/main/java/com/neuroflow/app/data/sync/FirestoreBackupRepository.kt \
         app/src/main/java/com/neuroflow/app/di/SyncModule.kt \
         app/src/main/java/com/neuroflow/app/NeuroFlowApplication.kt \
         app/src/main/java/com/neuroflow/app/presentation/common/NeuroFlowApp.kt \
         app/src/main/java/com/neuroflow/app/receiver/BootReceiver.kt \
         app/src/main/java/com/neuroflow/app/worker/AccessibilityWatchdogWorker.kt; do
  sed -i 's/presentation\.launcher\.hyperfocus/feature.hyperfocus/g' "$f"
done
```

- [ ] **Step 4: Confirm no main-source reference to the old path survives**

```bash
grep -rn "presentation\.launcher\.hyperfocus" app/src/main/java app/src/main/AndroidManifest.xml || echo "NO REMAINING REFS"
```

Expected: `NO REMAINING REFS` — including `kiosk/DeviceOwnerKioskManager.kt`, which was listed as a precaution in **Files** and should already be clean (its launcher imports are `.LauncherActivity` and `.service.LauncherKeepAlive*`, handled in Task 15). If it prints anything, that file needs the same substitution.

- [ ] **Step 5: Update the four HyperFocus component names in the manifest**

`AndroidManifest.xml` lines 190, 200, 213, 223 currently read `.presentation.launcher.hyperfocus.…`. Each becomes `.feature.hyperfocus.…`:

```xml
        <activity
            android:name=".feature.hyperfocus.HyperFocusActivity"
            android:launchMode="singleTask"
            android:taskAffinity="com.neuroflow.app.hyperfocus"
            android:excludeFromRecents="true"
            android:exported="false"
            android:enableOnBackInvokedCallback="false"
            android:theme="@style/Theme.NeuroFlow" />
```

and, in their existing blocks, `.feature.hyperfocus.service.AppBlockingService`, `.feature.hyperfocus.service.UnlockTimerService`, `.feature.hyperfocus.service.HyperFocusMonitorService`. A stale `android:name` here does not fail the build — it fails at boot with `ClassNotFoundException` in the accessibility settings list, so verify by grep:

```bash
grep -n "android:name=\"\.\(presentation\|feature\)" app/src/main/AndroidManifest.xml
```

- [ ] **Step 6: Create `HyperFocusModule` and trim `LauncherModule`**

Create `app/src/main/java/com/neuroflow/app/feature/hyperfocus/di/HyperFocusModule.kt`:

```kotlin
package com.neuroflow.app.feature.hyperfocus.di

import android.content.Context
import com.neuroflow.app.data.local.NeuroFlowDatabase
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusDataStore
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusDataStoreImpl
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusSessionDao
import com.neuroflow.app.feature.hyperfocus.data.UnlockCodeDao
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManagerImpl
import com.neuroflow.app.feature.hyperfocus.domain.UnlockCodeRepository
import com.neuroflow.app.feature.hyperfocus.domain.UnlockCodeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HyperFocusModule {

    @Binds
    @Singleton
    abstract fun bindUnlockCodeRepository(impl: UnlockCodeRepositoryImpl): UnlockCodeRepository

    @Binds
    @Singleton
    abstract fun bindHyperFocusManager(impl: HyperFocusManagerImpl): HyperFocusManager

    companion object {
        @Provides
        @Singleton
        fun provideHyperFocusDataStore(@ApplicationContext context: Context): HyperFocusDataStore =
            HyperFocusDataStoreImpl(context)

        @Provides
        fun provideUnlockCodeDao(database: NeuroFlowDatabase): UnlockCodeDao = database.unlockCodeDao()

        @Provides
        fun provideHyperFocusSessionDao(database: NeuroFlowDatabase): HyperFocusSessionDao =
            database.hyperFocusSessionDao()
    }
}
```

Then delete from `presentation/launcher/di/LauncherModule.kt`: the four `@Binds`/`@Provides` members just moved (`bindUnlockCodeRepository`, `bindHyperFocusManager`, `provideHyperFocusDataStore`, `provideUnlockCodeDao`, `provideHyperFocusSessionDao`) and their six now-unused imports (`HyperFocusDataStore`, `HyperFocusDataStoreImpl`, `HyperFocusSessionDao`, `UnlockCodeDao`, `HyperFocusManager`, `HyperFocusManagerImpl`, `UnlockCodeRepository`, `UnlockCodeRepositoryImpl`, `NeuroFlowDatabase`). `bindLauncherSearchEngine`, `providePackageManager`, `providePinnedAppsDataStore`, `provideAppWidgetHost`, `provideNotificationBadgeManager`, `provideAppRepository`, `provideIconPackManager`, `provideLauncherBackupManager` stay exactly as they are.

Do not add a `@Provides` for `LauncherApps`: `AppModule` already provides it (`LauncherModule.kt:72` comment records the duplicate-binding hazard).

- [ ] **Step 7: Move the HyperFocus test tree and rewrite its imports**

```bash
mkdir -p app/src/test/java/com/neuroflow/app/feature
git mv app/src/test/java/com/neuroflow/app/presentation/launcher/hyperfocus \
       app/src/test/java/com/neuroflow/app/feature/hyperfocus
for f in $(find app/src/test -name "*.kt"); do
  sed -i 's/presentation\.launcher\.hyperfocus/feature.hyperfocus/g' "$f"
done
grep -rn "presentation\.launcher\.hyperfocus" app/src/test || echo "TESTS CLEAN"
```

Expected: `TESTS CLEAN`. The `for` loop is scoped to test sources only, so `presentation.launcher.data` / `.domain` imports in launcher tests are untouched — they still compile against the launcher, which is still present.

- [ ] **Step 8: Build and run the HyperFocus suites**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh assemble && \
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.*"
```

Expected: build succeeds; the four HyperFocus suites (`AESUtilTest`, `HyperFocusManagerTest`, `RewardEngineTest`, `UnlockCodeRepositoryTest`) plus `HyperFocusProgressTest` pass with the same test counts as before the move. Record the counts — a suite that drops out of the report means the `--tests` filter matched nothing, which is a task failure, not a pass.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/feature \
        app/src/main/java/com/neuroflow/app/presentation/launcher/di/LauncherModule.kt \
        app/src/main/AndroidManifest.xml \
        app/src/test/java/com/neuroflow/app/feature app/src/test/java/com/neuroflow/app/data \
        app/src/main/java/com/neuroflow/app/data/local/NeuroFlowDatabase.kt \
        app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt \
        app/src/main/java/com/neuroflow/app/data/sync/CloudBackupManager.kt \
        app/src/main/java/com/neuroflow/app/data/sync/FirestoreBackupRepository.kt \
        app/src/main/java/com/neuroflow/app/di/SyncModule.kt \
        app/src/main/java/com/neuroflow/app/NeuroFlowApplication.kt \
        app/src/main/java/com/neuroflow/app/presentation/common/NeuroFlowApp.kt \
        app/src/main/java/com/neuroflow/app/receiver/BootReceiver.kt \
        app/src/main/java/com/neuroflow/app/worker/AccessibilityWatchdogWorker.kt
git commit -q -m "refactor: extract HyperFocus from the launcher package into feature/hyperfocus

Moves the focus engine out of presentation/launcher so the launcher product can be
archived without taking the enforcement path with it. Adds HyperFocusModule for the
bindings LauncherModule used to own. No behavior change." -- <same paths>
git show --name-only HEAD
```

---

## Task 2: Move `ScheduleAutoTasksWorker` to `app/worker/`

The worker is the app's auto-scheduler, not launcher behavior — spec §3.1 flags it because it is the only remaining file under `launcher/work/` and it is referenced from three main-source call sites outside the launcher. Its sibling test is currently a JUnit4 class, which means its 13 assertions never execute in this repo's test configuration; moving it is the moment to make them live.

**Files:**
- Move: `app/src/main/java/com/neuroflow/app/presentation/launcher/work/ScheduleAutoTasksWorker.kt` → `app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt` (the `worker/` package already holds `AccessibilityWatchdogWorker.kt`)
- Move: `app/src/test/java/com/neuroflow/app/presentation/launcher/work/EnergyProjectionBlendingTest.kt` → `app/src/test/java/com/neuroflow/app/worker/EnergyProjectionBlendingTest.kt`
- Modify: `data/repository/TaskRepository.kt:14`, `NeuroFlowApplication.kt:13`, `presentation/schedule/ScheduleViewModel.kt:18` (import rewrite only)
- Modify: `app/src/main/AndroidManifest.xml` — no change needed; the worker is not a manifest component.

**Interfaces:**
- Consumes: `HyperFocusManager` is not used by this worker, so nothing from Task 1 is touched.
- Produces: `com.neuroflow.app.worker.ScheduleAutoTasksWorker` with unchanged public surface — `WORK_NAME`, `FOREGROUND_TICK_WORK_NAME`, `buildPeriodicWorkRequest(throttleMinutes: Long)`, `buildOneTimeWorkRequest()` — which `NeuroFlowApplication.kt:169-190` calls.

- [ ] **Step 1: Move both files**

```bash
git mv app/src/main/java/com/neuroflow/app/presentation/launcher/work/ScheduleAutoTasksWorker.kt \
       app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt
git mv app/src/test/java/com/neuroflow/app/presentation/launcher/work/EnergyProjectionBlendingTest.kt \
       app/src/test/java/com/neuroflow/app/worker/EnergyProjectionBlendingTest.kt
rmdir app/src/main/java/com/neuroflow/app/presentation/launcher/work \
      app/src/test/java/com/neuroflow/app/presentation/launcher/work
```

- [ ] **Step 2: Rewrite the package declaration at all five sites**

`sed -i 's/presentation\.launcher\.work/worker/g'` on: the two moved files (first line each) plus `TaskRepository.kt`, `NeuroFlowApplication.kt`, `ScheduleViewModel.kt`. Then confirm:

```bash
grep -rn "presentation\.launcher\.work" app/src || echo "CLEAN"
```

- [ ] **Step 3: Convert the moved test from inert JUnit4 to kotest `StringSpec`**

Replace the whole body of `app/src/test/java/com/neuroflow/app/worker/EnergyProjectionBlendingTest.kt` with the same assertions in the repo's live convention. Keep `calculateLiveWeight` as the local replica it always was — do not reach into the worker, which needs a `Context`.

```kotlin
package com.neuroflow.app.worker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.floats.shouldBeCloseTo

class EnergyProjectionBlendingTest : StringSpec({

    fun liveWeight(hoursAhead: Float): Float = when {
        hoursAhead <= 2f -> 0.35f
        hoursAhead <= 6f -> 0.20f
        hoursAhead <= 12f -> 0.10f
        else -> 0.0f
    }

    fun blend(baseline: Float, moment: Float, hoursAhead: Float): Float {
        val w = liveWeight(hoursAhead)
        return baseline * (1f - w) + moment * w
    }

    "EPB-1: 0h ahead uses 35% moment weight" { liveWeight(0f) shouldBeCloseTo 0.35f abs 0.001f }
    "EPB-2: 1h ahead uses 35% moment weight" { liveWeight(1f) shouldBeCloseTo 0.35f abs 0.001f }
    "EPB-3: 2h boundary uses 35% moment weight" { liveWeight(2f) shouldBeCloseTo 0.35f abs 0.001f }
    "EPB-4: 3h ahead uses 20% moment weight" { liveWeight(3f) shouldBeCloseTo 0.20f abs 0.001f }
    "EPB-5: 6h boundary uses 20% moment weight" { liveWeight(6f) shouldBeCloseTo 0.20f abs 0.001f }
    "EPB-6: 8h ahead uses 10% moment weight" { liveWeight(8f) shouldBeCloseTo 0.10f abs 0.001f }
    "EPB-7: 12h boundary uses 10% moment weight" { liveWeight(12f) shouldBeCloseTo 0.10f abs 0.001f }
    "EPB-8: 13h ahead drops moment weight entirely" { liveWeight(13f) shouldBeCloseTo 0.0f abs 0.001f }
    "EPB-9: 24h ahead drops moment weight entirely" { liveWeight(24f) shouldBeCloseTo 0.0f abs 0.001f }
    "EPB-10: 48h ahead drops moment weight entirely" { liveWeight(48f) shouldBeCloseTo 0.0f abs 0.001f }
    "EPB-11: far-future slot is 100% baseline" { blend(75f, 50f, 24f) shouldBeCloseTo 75f abs 0.001f }
    "EPB-12: near-future slot blends 35/65" { blend(75f, 50f, 1f) shouldBeCloseTo 66.25f abs 0.001f }
    "EPB-13: medium-future slot blends 20/80" { blend(80f, 40f, 4f) shouldBeCloseTo 72f abs 0.001f }
})
```

`io.kotest.matchers.floats.shouldBeCloseTo` takes an `abs` infix; if the compiler rejects the form, use `shouldBeCloseTo(0.35f, 0.001f)` instead — both exist in kotest 5.9.1.

- [ ] **Step 4: Build and run — the count is the point**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh assemble && \
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.worker.EnergyProjectionBlendingTest"
```

Expected: build succeeds and the report shows **13 tests** for this class. Before this task it showed 0 (inert JUnit4). Zero tests matched means the class name or package is wrong — treat that as a failed task, not a pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt \
        app/src/test/java/com/neuroflow/app/worker/EnergyProjectionBlendingTest.kt \
        app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt \
        app/src/main/java/com/neuroflow/app/NeuroFlowApplication.kt \
        app/src/main/java/com/neuroflow/app/presentation/schedule/ScheduleViewModel.kt
git commit -q -m "refactor: move ScheduleAutoTasksWorker out of the launcher package

The worker is app-level auto-scheduling, not launcher behavior, and its test was a
JUnit4 class that this repo's JUnit-Platform test config never runs. Converted to
kotest StringSpec so its 13 assertions execute." \
  -- app/src/main/java/com/neuroflow/app/worker/ScheduleAutoTasksWorker.kt \
     app/src/test/java/com/neuroflow/app/worker/EnergyProjectionBlendingTest.kt \
     app/src/main/java/com/neuroflow/app/data/repository/TaskRepository.kt \
     app/src/main/java/com/neuroflow/app/NeuroFlowApplication.kt \
     app/src/main/java/com/neuroflow/app/presentation/schedule/ScheduleViewModel.kt \
     app/src/main/java/com/neuroflow/app/presentation/launcher/work/ScheduleAutoTasksWorker.kt \
     app/src/test/java/com/neuroflow/app/presentation/launcher/work/EnergyProjectionBlendingTest.kt
git show --name-only HEAD
# Name the two OLD paths in the commit pathspec (never in the `git add` above — `git mv` has already
# consumed their index entries and `git add` would fail). The commit pathspec matches against HEAD, so
# this is what records the deletions. Omit them and HEAD holds BOTH copies: `:app:assembleDebug` still
# passes, and only the unit-test compile breaks. This gate catches it:
git ls-tree -r --name-only HEAD app/src/main/java/com/neuroflow/app/presentation/launcher/work \
  app/src/test/java/com/neuroflow/app/presentation/launcher/work | grep . && echo "DEFECT: old copies still in HEAD" || echo "old paths gone"
```

Expected: the gate prints `old paths gone`. It did not on this task's first run: commit `4f0f1d2` left
`presentation/launcher/work/ScheduleAutoTasksWorker.kt` in HEAD and needed follow-up `d8e4cec`, because
the recipe then told me to `git add` the old path, which `git mv` makes impossible. Task 3 hit the same
shape (`8ec4827` → `eb0e438`). If the gate still prints DEFECT, the deletion is sitting in the index —
commit it on its own naming the old path, never an amend.

## Task 3: Relocate `NotificationBadgeManager` and its service to `data/badge/`

Spec delta 1 above: these two files are deleted by spec §3.3, but `EnergyScoreRepository` consumes `badgeCounts` as a live scoring input (`EnergyScoreRepository.kt:150,188`) and `EnergyScoreRepositoryContractTest.kt:13` injects the type. They move out of the launcher instead, so Phase 3 can delete `presentation/launcher/` wholesale without touching the energy model.

**Files:**
- Move: `app/src/main/java/com/neuroflow/app/presentation/launcher/data/NotificationBadgeManager.kt` → `app/src/main/java/com/neuroflow/app/data/badge/NotificationBadgeManager.kt`
- Move: `app/src/main/java/com/neuroflow/app/presentation/launcher/data/NotificationBadgeService.kt` → `app/src/main/java/com/neuroflow/app/data/badge/NotificationBadgeService.kt`
- Modify: `domain/repository/EnergyScoreRepository.kt:14`, `presentation/launcher/LauncherViewModel.kt` (import line only), `presentation/launcher/di/LauncherModule.kt:10` + `provideNotificationBadgeManager` moves to `HyperFocusModule`? **No** — it belongs to neither feature; provide it in `di/AppModule.kt` (already the app-wide module).
- Modify: `app/src/main/AndroidManifest.xml:180` (`android:name=".presentation.launcher.data.NotificationBadgeService"` → `.data.badge.NotificationBadgeService`)
- Modify: `app/src/test/java/com/neuroflow/app/domain/repository/EnergyScoreRepositoryContractTest.kt:13`

**Interfaces:**
- Consumes: nothing from Tasks 1-2.
- Produces: `com.neuroflow.app.data.badge.NotificationBadgeManager` unchanged in surface (`badgeCounts: StateFlow<Map<String, Int>>`, `update(...)`) and `com.neuroflow.app.data.badge.NotificationBadgeService`.

- [ ] **Step 1: Move both files and rewrite the package**

```bash
mkdir -p app/src/main/java/com/neuroflow/app/data/badge
git mv app/src/main/java/com/neuroflow/app/presentation/launcher/data/NotificationBadgeManager.kt \
       app/src/main/java/com/neuroflow/app/data/badge/NotificationBadgeManager.kt
git mv app/src/main/java/com/neuroflow/app/presentation/launcher/data/NotificationBadgeService.kt \
       app/src/main/java/com/neuroflow/app/data/badge/NotificationBadgeService.kt
sed -i 's/^package com\.neuroflow\.app\.presentation\.launcher\.data$/package com.neuroflow.app.data.badge/' \
       app/src/main/java/com/neuroflow/app/data/badge/NotificationBadgeManager.kt \
       app/src/main/java/com/neuroflow/app/data/badge/NotificationBadgeService.kt
```

Both files sit in the same package today, so the service resolves the manager without a new import. If `NotificationBadgeService.kt` carries an explicit `import …launcher.data.NotificationBadgeManager`, add `import com.neuroflow.app.data.badge.NotificationBadgeManager` — check with `grep -n "NotificationBadgeManager" app/src/main/java/com/neuroflow/app/data/badge/NotificationBadgeService.kt`.

- [ ] **Step 2: Point every consumer at the new path**

```bash
sed -i 's/com\.neuroflow\.app\.presentation\.launcher\.data\.NotificationBadge\(Manager\|Service\)/com.neuroflow.app.data.badge.NotificationBadge\1/g' \
  app/src/main/java/com/neuroflow/app/domain/repository/EnergyScoreRepository.kt \
  app/src/main/java/com/neuroflow/app/presentation/launcher/LauncherViewModel.kt \
  app/src/main/java/com/neuroflow/app/presentation/launcher/di/LauncherModule.kt \
  app/src/test/java/com/neuroflow/app/domain/repository/EnergyScoreRepositoryContractTest.kt
sed -i 's/android:name="\.presentation\.launcher\.data\.NotificationBadgeService"/android:name=".data.badge.NotificationBadgeService"/' \
  app/src/main/AndroidManifest.xml
grep -rn "launcher\.data\.NotificationBadge" app/src || echo "CLEAN"
grep -n "NotificationBadgeService" app/src/main/AndroidManifest.xml
```

Expected: `CLEAN`, then the manifest line reading `.data.badge.NotificationBadgeService`.

- [ ] **Step 3: Move the provider out of `LauncherModule` into `AppModule`**

Delete `provideNotificationBadgeManager` and the `NotificationBadgeManager` import from `presentation/launcher/di/LauncherModule.kt`, and add to the `companion object` of `di/AppModule.kt`:

```kotlin
        @Provides
        @Singleton
        fun provideNotificationBadgeManager(): com.neuroflow.app.data.badge.NotificationBadgeManager =
            com.neuroflow.app.data.badge.NotificationBadgeManager()
```

`NotificationBadgeManager` has `@Inject constructor()`, so a `@Binds`-free `@Provides` of a concrete type is redundant-but-harmless; prefer deleting the provider entirely and letting Hilt construct it via `@Inject`. Do that first; only restore an explicit provider if KSP reports a missing binding.

- [ ] **Step 4: Verify**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh assemble && bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.domain.repository.EnergyScoreRepositoryContractTest"
```

Expected: build succeeds; the contract suite passes. It is already kotest or JUnit4 — if it is JUnit4 it will report no tests, which is pre-existing, not a regression from this task.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/data/badge \
        app/src/main/java/com/neuroflow/app/domain/repository/EnergyScoreRepository.kt \
        app/src/main/java/com/neuroflow/app/presentation/launcher/LauncherViewModel.kt \
        app/src/main/java/com/neuroflow/app/presentation/launcher/di/LauncherModule.kt \
        app/src/main/AndroidManifest.xml \
        app/src/test/java/com/neuroflow/app/domain/repository/EnergyScoreRepositoryContractTest.kt
git commit -q -m "refactor: move notification badge listener out of the launcher package

EnergyScoreRepository reads badgeCounts as an energy-model signal, so the manager and
its listener service survive the launcher archive instead of being deleted with it." \
  -- app/src/main/java/com/neuroflow/app/data/badge \
     app/src/main/java/com/neuroflow/app/domain/repository/EnergyScoreRepository.kt \
     app/src/main/java/com/neuroflow/app/presentation/launcher/LauncherViewModel.kt \
     app/src/main/java/com/neuroflow/app/presentation/launcher/di/LauncherModule.kt \
     app/src/main/AndroidManifest.xml \
     app/src/test/java/com/neuroflow/app/domain/repository/EnergyScoreRepositoryContractTest.kt \
     app/src/main/java/com/neuroflow/app/presentation/launcher/data/NotificationBadgeManager.kt \
     app/src/main/java/com/neuroflow/app/presentation/launcher/data/NotificationBadgeService.kt
git show --name-only HEAD
git ls-tree -r --name-only HEAD app/src/main/java/com/neuroflow/app/presentation/launcher/data/NotificationBadgeManager.kt \
  app/src/main/java/com/neuroflow/app/presentation/launcher/data/NotificationBadgeService.kt | grep . \
  && echo "DEFECT: old copies still in HEAD" || echo "old paths gone"
```

Expected: `old paths gone`, and if it prints DEFECT, follow the Global Constraint's step 3: the staged
deletion is still in the index, so commit it on its own naming the two old paths. Never `git add` those
two paths first — `git mv` has already consumed their index entries and `git add` will fail with *pathspec
did not match any files*. Name the two files, not the directory: `presentation/launcher/data/` keeps six
launcher files (`AppInfo`, `AppRepository`, `IconPackManager`, `LauncherBackupManager`,
`PackageChangeReceiver`, `PinnedAppsDataStore`) until Task 17, so a directory gate there would always
report DEFECT. (This is the Task 1 `e68088f`/`a5bbaa9` and Task 2 `4f0f1d2`/`d8e4cec` defect.)

**Phase 1 exit check.** `bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh assemble && bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh tests` green, and `find app/src/main/java/com/neuroflow/app/presentation/launcher -type f | wc -l` prints **37** (63 − 23 hyperfocus − 1 worker − 2 badge; `HyperFocusModule.kt` lands in `feature/`, not here). Nothing in Phase 1 is user-visible; if it is, a mistake was made.

---

# Phase 2 — Allowlist inversion and the black home

Thirteen tasks. This is the risky phase (spec §10): it changes what the device does when the Home button is pressed, and none of that is verifiable on this machine. Every task still ends in a green build plus a green unit suite; the device-only items are collected at the end of the plan into one test plan to run before release.

Logic is placed where it can be tested: each new component is a pure JVM core (`FocusSurfaceModel`, `GateTaskEngine`, `AppCatalogFilter`, `RewardEngine`) plus an Android shell (`SystemExempt`, `AppCatalogRepository`, `HyperFocusHomeActivity`, screens).

## Task 4: Add the allowlist and pass fields to `HyperFocusPreferences`

Additive only. `blockedPackages` **stays** for now — Task 11 consumes it to build the first allowlist and deletes it in the same commit. `activeUnlockExpiresAt` also stays; Task 8 replaces it with `activePassExpiresAt` and updates every reader. Adding both new fields here means Tasks 5-10 compile against their final shape.

**Files:**
- Modify: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/data/HyperFocusDataStore.kt` (data class `:22-51`, `serialize` `:103-130`, `deserialize` `:132-179`)

**Interfaces:**
- Consumes: nothing.
- Produces: `HyperFocusPreferences(essentialPackages: Set<String>, allowedPackages: Set<String>, activePassPackage: String?, activePassExpiresAt: Long?, realHomeComponent: String?)` — all defaulting to empty/null, so `HyperFocusPreferences()` remains constructible with no arguments. **9 such call sites exist** (measured after Task 4, and Task 4 itself adds one of them): `HyperFocusViewModel.kt:43`, `service/AppBlockingService.kt:40`, `data/HyperFocusDataStore.kt:81`/`:88`/`:196`, `domain/HyperFocusManager.kt:68`, `presentation/launcher/LauncherViewModel.kt:499` (deleted by Task 17), `HyperFocusManagerTest.kt:130`, and the new `HyperFocusAllowlistFieldsTest.kt`. Every one of them is a corrupt-blob/off-state seed, so **no default in this data class may ever become a non-empty allowlist or an active pass** — that is §8's "corrupt blob must render off-state, not a locked device", and dropping a default would break 9 sites at once rather than the two this plan originally named.

- [ ] **Step 1: Add the five fields to the data class**

Insert immediately after the existing `blockedPackages` line (`:26`), keeping the denylist where it is:

```kotlin
    // --- Allowlist model (B2): global tiers, identical across sessions ---
    val essentialPackages: Set<String> = emptySet(),
    val allowedPackages: Set<String> = emptySet(),
    // --- Per-app pass (D3 + E1): exactly one granted package, wall-clock budget ---
    val activePassPackage: String? = null,
    val activePassExpiresAt: Long? = null,
    // The user's real launcher, resolved once so the off state can hand Home back.
    val realHomeComponent: String? = null,
```

- [ ] **Step 2: Serialize them**

In `serialize`, after `json.put("blockedPackages", packagesArray)` (`:110`), follow the exact shape the existing `lockedTaskIds` block uses at `:122-124`:

```kotlin
        val essentialArray = JSONArray()
        prefs.essentialPackages.forEach { essentialArray.put(it) }
        json.put("essentialPackages", essentialArray)
        val allowedArray = JSONArray()
        prefs.allowedPackages.forEach { allowedArray.put(it) }
        json.put("allowedPackages", allowedArray)
        json.put("activePassPackage", prefs.activePassPackage ?: JSONObject.NULL)
        json.put("activePassExpiresAt", prefs.activePassExpiresAt ?: JSONObject.NULL)
        json.put("realHomeComponent", prefs.realHomeComponent ?: JSONObject.NULL)
```

- [ ] **Step 3: Deserialize them**

Add a local helper next to the existing `blockedPackages` set-building in `deserialize` (`:135-142`) rather than repeating the loop five times:

```kotlin
            fun stringSet(key: String): Set<String> = buildSet {
                val arr = obj.optJSONArray(key)
                if (arr != null) for (i in 0 until arr.length()) add(arr.getString(i))
            }
```

Then pass the new arguments into the `HyperFocusPreferences(...)` constructor call at the end of `deserialize`:

```kotlin
                essentialPackages = stringSet("essentialPackages"),
                allowedPackages = stringSet("allowedPackages"),
                activePassPackage = if (obj.isNull("activePassPackage")) null
                    else obj.optString("activePassPackage").takeIf { it.isNotEmpty() },
                activePassExpiresAt = if (obj.isNull("activePassExpiresAt")) null
                    else obj.optLong("activePassExpiresAt").takeIf { it > 0L },
                realHomeComponent = if (obj.isNull("realHomeComponent")) null
                    else obj.optString("realHomeComponent").takeIf { it.isNotEmpty() },
```

The `stringSet` helper is also the right moment to collapse the two existing hand-rolled set readers (`blockedPackages` at `:135-142` and `lockedTaskIds` at `:159-162`) onto it. Refactoring those two is in scope: same file, same semantics, and leaving three copies of the loop is how a future edit updates two of them.

- [ ] **Step 4: Verify the old-blob contract**

An existing install's blob has none of the new keys. Both halves of the null-coalescing are load-bearing and neither may be simplified away later: measured against the real AOSP `org.json` (`android-all-instrumented-14`) in Task 4's review, `isNull(absent) = true`, `isNull(JSONObject.NULL) = true`, `isNull("") = false` with `optString(absent) = ""` and `optLong(absent) = 0` — so on-device the absent key takes the `isNull` branch, while on any implementation where `isNull(absent)` is false the `takeIf { it.isNotEmpty() }` / `takeIf { it > 0L }` guards catch exactly the `""` and `0L` the optionals return. The two paths converge on `null` only because the guards are there; deleting one silently changes what an old blob deserializes to. `optJSONArray(absent)` and `optJSONArray(non-array)` are both `null`, so `stringSet` yields `emptySet()` for missing keys. Verify with a targeted suite rather than by reading. `HyperFocusDataStoreImpl` needs a `Context` and Robolectric is inert here, so test the pure half: extract nothing, but assert that a fresh `HyperFocusPreferences()` has empty allowlists and no pass, which is what `deserialize` returns for a corrupt or absent blob:

Create `app/src/test/java/com/neuroflow/app/feature/hyperfocus/data/HyperFocusAllowlistFieldsTest.kt`:

```kotlin
package com.neuroflow.app.feature.hyperfocus.data

import com.neuroflow.app.domain.model.HyperFocusState
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class HyperFocusAllowlistFieldsTest : StringSpec({

    "HP-1: defaults carry an empty allowlist and no pass" {
        val prefs = HyperFocusPreferences()
        prefs.essentialPackages.shouldBeEmpty()
        prefs.allowedPackages.shouldBeEmpty()
        prefs.activePassPackage.shouldBeNull()
        prefs.activePassExpiresAt.shouldBeNull()
        prefs.realHomeComponent.shouldBeNull()
        prefs.isActive shouldBe false
        prefs.state shouldBe HyperFocusState.INACTIVE
    }
})
```

This suite is a guard on the §8 error-handling row "DataStore blob corrupt → renders off-state, not a locked device": if a default ever becomes a non-empty allowlist or an active pass, an unreadable blob would silently lock the user out.

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh assemble && \
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.data.HyperFocusAllowlistFieldsTest"
```

Expected: 1 test, passing.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/feature/hyperfocus/data/HyperFocusDataStore.kt \
        app/src/test/java/com/neuroflow/app/feature/hyperfocus/data/HyperFocusAllowlistFieldsTest.kt
git commit -q -m "feat: add allowlist and per-app pass fields to HyperFocusPreferences

Adds essentialPackages/allowedPackages (B2), activePassPackage/activePassExpiresAt
(D3/E1) and realHomeComponent for the off-state handoff. The denylist stays until the
one-shot migration in a later task has consumed it." -- <same paths>
git show --name-only HEAD
```

## Task 5: `FocusSurfaceModel` and `SystemExempt` — one answer to "is this allowed?"

Spec §4.2 and §6.2. Both the black home's grid and `AppBlockingService` read `resolvedAllowlist`; no other component may compute its own view. `systemExempt` is the guard against the classic allowlist failure mode of locking out your own keyboard, permission dialogs and incoming calls.

**Files:**
- Create: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/surface/FocusSurfaceModel.kt`
- Create: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/surface/SystemExempt.kt`
- Test: `app/src/test/java/com/neuroflow/app/feature/hyperfocus/surface/FocusSurfaceModelTest.kt`

**Interfaces:**
- Consumes: `HyperFocusPreferences` from Task 4.
- Produces:
  ```kotlin
  object FocusSurfaceModel {
      val DEFAULT_ESSENTIAL_SEED: Set<String>
      fun resolvedAllowlist(prefs: HyperFocusPreferences, systemExempt: Set<String>,
                            selfPackage: String, nowMillis: Long): Set<String>
      fun isGated(packageName: String, prefs: HyperFocusPreferences, systemExempt: Set<String>,
                  selfPackage: String, nowMillis: Long): Boolean
  }
  class SystemExempt @Inject constructor(@ApplicationContext context: Context) {
      fun packages(): Set<String>
      companion object { val STATIC: Set<String> }
  }
  ```
  Both `object`s take `nowMillis` as a parameter instead of calling `System.currentTimeMillis()`, because expiry is the single most-tested behavior in this plan and a hidden clock makes every test a timing test.

- [ ] **Step 1: Write the failing test**

Create `FocusSurfaceModelTest.kt`:

```kotlin
package com.neuroflow.app.feature.hyperfocus.surface

import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

private const val SELF = "com.neuroflow.app"
private val EXEMPT = setOf("com.android.systemui", "android")
private const val NOW = 1_000_000L

class FocusSurfaceModelTest : StringSpec({

    "FS-1: essential and allowed both appear" {
        val prefs = HyperFocusPreferences(
            essentialPackages = setOf("com.android.dialer"),
            allowedPackages = setOf("com.todoist")
        )
        FocusSurfaceModel.resolvedAllowlist(prefs, EXEMPT, SELF, NOW) shouldContainAll
            setOf("com.android.dialer", "com.todoist", "com.android.systemui", "android", SELF)
    }

    "FS-2: an expired pass contributes nothing" {
        val prefs = HyperFocusPreferences(
            essentialPackages = setOf("com.android.dialer"),
            activePassPackage = "com.instagram.android",
            activePassExpiresAt = NOW - 1
        )
        FocusSurfaceModel.resolvedAllowlist(prefs, EXEMPT, SELF, NOW) shouldNotContain "com.instagram.android"
    }

    "FS-3: a live pass contributes exactly one package" {
        val prefs = HyperFocusPreferences(
            essentialPackages = setOf("com.android.dialer"),
            activePassPackage = "com.instagram.android",
            activePassExpiresAt = NOW + 60_000
        )
        FocusSurfaceModel.resolvedAllowlist(prefs, EXEMPT, SELF, NOW) shouldContain "com.instagram.android"
    }

    "FS-4: a live deadline with no package contributes nothing" {
        val prefs = HyperFocusPreferences(activePassExpiresAt = NOW + 60_000)
        FocusSurfaceModel.resolvedAllowlist(prefs, EXEMPT, SELF, NOW).size shouldBe EXEMPT.size + 1
    }

    "FS-5: a package with no deadline is not a pass" {
        val prefs = HyperFocusPreferences(activePassPackage = "com.instagram.android")
        FocusSurfaceModel.resolvedAllowlist(prefs, EXEMPT, SELF, NOW) shouldNotContain "com.instagram.android"
    }

    "FS-6: gating is the negation of the allowlist" {
        val prefs = HyperFocusPreferences(essentialPackages = setOf("com.android.dialer"))
        FocusSurfaceModel.isGated("com.android.dialer", prefs, EXEMPT, SELF, NOW) shouldBe false
        FocusSurfaceModel.isGated("com.netflix.mediaclient", prefs, EXEMPT, SELF, NOW) shouldBe true
        FocusSurfaceModel.isGated("com.android.systemui", prefs, EXEMPT, SELF, NOW) shouldBe false
        FocusSurfaceModel.isGated(SELF, prefs, EXEMPT, SELF, NOW) shouldBe false
    }

    "FS-7: empty user tiers still never gate the exempt set or self" {
        val prefs = HyperFocusPreferences()
        FocusSurfaceModel.resolvedAllowlist(prefs, EXEMPT, SELF, NOW) shouldContainAll setOf(
            "com.android.systemui", "android", SELF
        )
    }

    "FS-8: the default seed is the six spec packages" {
        FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED shouldBe setOf(
            "com.android.dialer", "com.google.android.apps.dialer", "com.samsung.android.dialer",
            "com.android.mms", "com.google.android.apps.messaging",
            "com.android.contacts", "com.android.camera", "com.google.android.Camera",
            "com.android.deskclock", "com.google.android.deskclock", SELF
        )
    }
})
```

- [ ] **Step 2: Run it and confirm it fails for the right reason**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.surface.FocusSurfaceModelTest"
```

Expected: compile failure `unresolved reference: FocusSurfaceModel`. That is the failing state — the class does not exist yet.

- [ ] **Step 3: Implement `FocusSurfaceModel`**

```kotlin
package com.neuroflow.app.feature.hyperfocus.surface

import com.neuroflow.app.BuildConfig
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences

/**
 * The single source of truth for "may this package be used right now".
 * Everything that gates an app reads resolvedAllowlist(); nothing computes its own view.
 */
object FocusSurfaceModel {

    val DEFAULT_ESSENTIAL_SEED: Set<String> = setOf(
        "com.android.dialer", "com.google.android.apps.dialer", "com.samsung.android.dialer",
        "com.android.mms", "com.google.android.apps.messaging",
        "com.android.contacts",
        "com.android.camera", "com.google.android.Camera",
        "com.android.deskclock", "com.google.android.deskclock",
        BuildConfig.APPLICATION_ID
    )

    fun resolvedAllowlist(
        prefs: HyperFocusPreferences,
        systemExempt: Set<String>,
        selfPackage: String,
        nowMillis: Long
    ): Set<String> = buildSet {
        addAll(prefs.essentialPackages)
        addAll(prefs.allowedPackages)
        addAll(systemExempt)
        add(selfPackage)
        val pass = prefs.activePassPackage
        val deadline = prefs.activePassExpiresAt
        if (pass != null && deadline != null && deadline > nowMillis) add(pass)
    }

    fun isGated(
        packageName: String,
        prefs: HyperFocusPreferences,
        systemExempt: Set<String>,
        selfPackage: String,
        nowMillis: Long
    ): Boolean = packageName !in resolvedAllowlist(prefs, systemExempt, selfPackage, nowMillis)
}
```

No comment beyond the class KDoc — the function names carry it. (Spec §6.4 puts Settings *outside* the essential seed on purpose; it stays reachable through `systemExempt`'s permission/installer entries and the tamper guard, but not as an icon. That is one of the two values the user has not explicitly ratified — see the open questions at the end of this plan. If they overrule it, this set and FS-8 are the only two places to change.)

- [ ] **Step 4: Implement `SystemExempt`**

```kotlin
package com.neuroflow.app.feature.hyperfocus.surface

import android.content.Context
import android.view.inputmethod.InputMethodManager
import com.neuroflow.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Packages that must never be gated: gating them locks the user out of their own device.
 * Read fresh because the enabled keyboard changes without our knowing.
 */
@Singleton
class SystemExempt @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun packages(): Set<String> = buildSet {
        addAll(STATIC)
        add(BuildConfig.APPLICATION_ID)
        val imm = context.getSystemService(InputMethodManager::class.java)
        imm?.enabledInputMethodList?.forEach { add(it.packageName) }
    }

    companion object {
        val STATIC: Set<String> = setOf(
            "android",
            "com.android.systemui",
            "com.android.server.telecom",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.miui.packageinstaller",
            "com.android.settings",
            "com.samsung.android.settings",
            "com.miui.securitycenter"
        )
    }
}
```

`com.android.settings` and its two vendor twins join `BuildConfig.APPLICATION_ID` here. Spec §6.2 lists them as the exemption mechanism that keeps Settings reachable while §6.4 keeps it off the home screen — the exemption set is "never blocked", not "shown as an icon", so both hold. Exempting Settings is also what stops the F4 gate from trapping the user: if the accessibility service is ever disabled, Settings must remain openable to re-enable it.

Its `packages()` return is not unit-testable here (needs `Context`), which is exactly why `FocusSurfaceModel` takes the set as a parameter instead of asking for it.

- [ ] **Step 5: Run the test**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.surface.FocusSurfaceModelTest"
```

Expected: 8 tests, all passing.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/feature/hyperfocus/surface \
        app/src/test/java/com/neuroflow/app/feature/hyperfocus/surface
git commit -q -m "feat: add FocusSurfaceModel allowlist resolution and SystemExempt

One function answers whether a package is gated, so the black home grid and the
accessibility enforcer cannot disagree. SystemExempt carries IMEs, systemui, telecom,
permission controllers, installers and Settings so an allowlist can never lock the
user out of the keyboard, an incoming call, or the screen that re-enables the service." -- <same paths>
git show --name-only HEAD
```

## Task 6: `GateTaskEngine` — the cognitive task, its floor, and its auto-pass

Spec §5.2. This is F4's core: effort-only, always completable, and the only place that decides whether the gate may open.

**Files:**
- Create: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/gate/GateTaskEngine.kt`
- Test: `app/src/test/java/com/neuroflow/app/feature/hyperfocus/gate/GateTaskEngineTest.kt`

**Interfaces:**
- Consumes: nothing (pure JVM).
- Produces:
  ```kotlin
  enum class GateTaskType { TRANSCRIBE, COUNT_BACK, RESTATED }
  data class GateTask(val type: GateTaskType, val prompt: String, val expected: List<String>)
  class GateTaskEngine(private val random: Random = Random.Default) {
      fun newTask(): GateTask
      fun checkAnswer(task: GateTask, answer: List<String>): Boolean
      fun isReasonValid(reason: String): Boolean
      fun shouldAutoPass(mismatchCount: Int): Boolean
      fun allowsSubmit(elapsedMillis: Long, reason: String): Boolean
  }
  ```
  `GateTaskEngine` takes `Random` as a constructor parameter so the selection is injectable; `elapsedMillis` is a parameter, never `System.currentTimeMillis()`, because the 12-second floor must be testable without sleeping.

  `allowsSubmit` deliberately does **not** take the mismatch count. It is the authority for one question — *may the user act yet?* — and the answer is the floor plus the reason. `shouldAutoPass` is the authority for the other question — *has this count earned the pass?* — and `submitGateAnswer` combines the two with `checkAnswer`. Feeding the count into `allowsSubmit` as well (both `% MAX` and `>= MAX` readings were tried) disables the button at count 1 and 2, and since the count only increments on a click, that makes the auto-pass unreachable: a user who mistypes once can never reach the three failures that would let them through, only cancel and re-roll. See ruling 2 below.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.neuroflow.app.feature.hyperfocus.gate

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class GateTaskEngineTest : StringSpec({

    val engine = GateTaskEngine(Random(42))

    "GT-1: count-back produces four descending values stepping by 7" {
        repeat(20) {
            val task = GateTaskEngine(Random(it)).newTask()
            if (task.type == GateTaskType.COUNT_BACK) {
                task.expected shouldHaveSize 4
                val values = task.expected.map { it.toInt() }
                values[1] shouldBe values[0] - 7
                values[2] shouldBe values[0] - 14
                values[3] shouldBe values[0] - 21
                values[0] shouldBeInRange 100..999
                (values[0] % 100 == 0) shouldBe false
            }
        }
    }

    "GT-2: every task shows its stimulus — the prompt contains the first expected value" {
        repeat(30) {
            val task = GateTaskEngine(Random(it)).newTask()
            when (task.type) {
                GateTaskType.TRANSCRIBE, GateTaskType.RESTATED -> task.expected shouldHaveSize 1
                else -> {}
            }
            task.prompt.contains(task.expected.first()) shouldBe true
        }
    }

    "GT-3: exact answer passes, case and padding insensitive" {
        val task = GateTask(GateTaskType.TRANSCRIBE, "Type exactly what you see", listOf("quiet desk"))
        engine.checkAnswer(task, listOf("  QUIET DESK ")) shouldBe true
    }

    "GT-4: one wrong character fails" {
        val task = GateTask(GateTaskType.TRANSCRIBE, "p", listOf("quiet desk"))
        engine.checkAnswer(task, listOf("quiet deskl")).shouldBeFalse()
    }

    "GT-5: count-back must match all four values" {
        val task = GateTask(GateTaskType.COUNT_BACK, "p", listOf("411", "404", "397", "390"))
        engine.checkAnswer(task, listOf("411", "404", "397", "391")).shouldBeFalse()
        engine.checkAnswer(task, listOf("411", "404", "397", "390")).shouldBeTrue()
    }

    "GT-6: blank answer on a shorter list fails instead of crashing" {
        val task = GateTask(GateTaskType.COUNT_BACK, "p", listOf("411", "404", "397", "390"))
        engine.checkAnswer(task, emptyList()).shouldBeFalse()
    }

    "GT-7: reason needs eight trimmed characters" {
        engine.isReasonValid("It's me").shouldBeFalse()         // 7 trimmed
        engine.isReasonValid("It's me ").shouldBeFalse()        // still 7 after trim
        engine.isReasonValid("I need this one").shouldBeTrue()   // 15
        engine.isReasonValid("        ").shouldBeFalse()
    }

    "GT-8: three mismatches auto-pass, two do not" {
        engine.shouldAutoPass(2).shouldBeFalse()
        engine.shouldAutoPass(3).shouldBeTrue()
        engine.shouldAutoPass(7).shouldBeTrue()
    }

    "GT-9: submit is blocked before the twelve-second floor" {
        engine.allowsSubmit(11_999L, "because it is mine").shouldBeFalse()
        engine.allowsSubmit(12_000L, "because it is mine").shouldBeTrue()
    }

    "GT-10: submit is blocked while the reason is short even after the floor" {
        engine.allowsSubmit(60_000L, "short").shouldBeFalse()
    }

    "GT-11: the mismatch count never closes the button, and one mismatch earns no pass" {
        engine.allowsSubmit(60_000L, "because it is mine").shouldBeTrue()
        engine.shouldAutoPass(1).shouldBeFalse()
    }

    "GT-12: the task types are reachable and never null" {
        val seen = (0 until 200).map { GateTaskEngine(Random(it)).newTask().type }.toSet()
        seen shouldBe GateTaskType.entries.toSet()
    }
})
```

- [ ] **Step 2: Run it and confirm the failure is a missing type**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.gate.GateTaskEngineTest"
```

Expected: compile failure `unresolved reference: GateTaskEngine`.

- [ ] **Step 3: Implement `GateTaskEngine`**

Create `app/src/main/java/com/neuroflow/app/feature/hyperfocus/gate/GateTaskEngine.kt`:

```kotlin
package com.neuroflow.app.feature.hyperfocus.gate

import kotlin.random.Random

enum class GateTaskType { TRANSCRIBE, COUNT_BACK, RESTATED }

data class GateTask(
    val type: GateTaskType,
    val prompt: String,
    val expected: List<String>
)

/**
 * Effort-only cognition gate. It can always be passed: mismatches are reported, never fatal,
 * and after three of them the task auto-passes. What it costs is seconds and attention.
 */
class GateTaskEngine(
    private val random: Random = Random.Default
) {
    fun newTask(): GateTask = when (GateTaskType.entries.random(random)) {
        GateTaskType.TRANSCRIBE ->
            single(TRANSCRIPTIONS, "Type exactly what you see", GateTaskType.TRANSCRIBE)
        GateTaskType.RESTATED ->
            single(SENTENCES, "Retype this sentence word for word, including punctuation", GateTaskType.RESTATED)
        GateTaskType.COUNT_BACK -> countBack()
    }

    private fun single(pool: List<String>, instruction: String, type: GateTaskType): GateTask {
        val value = pool[random.nextInt(pool.size)]
        return GateTask(type, "$instruction:\n\n$value", listOf(value))
    }

    private fun countBack(): GateTask {
        var start = random.nextInt(100, 1000)
        while (start % 100 == 0) start = random.nextInt(100, 1000)
        val values = (0 until 4).map { (start - it * 7).toString() }
        return GateTask(
            GateTaskType.COUNT_BACK,
            "Start at $start and count back by 7. Enter the four values, starting at $start.",
            values
        )
    }

    fun checkAnswer(task: GateTask, answer: List<String>): Boolean =
        task.expected.indices.all { i ->
            task.expected[i].trim().equals(answer.getOrNull(i)?.trim().orEmpty(), ignoreCase = true)
        }

    fun isReasonValid(reason: String): Boolean = reason.trim().length >= MIN_REASON_CHARS

    fun shouldAutoPass(mismatchCount: Int): Boolean = mismatchCount >= MAX_MISMATCHES

    fun allowsSubmit(elapsedMillis: Long, reason: String): Boolean =
        isReasonValid(reason) && elapsedMillis >= MIN_ELAPSED_MILLIS

    companion object {
        const val MAX_MISMATCHES = 3
        const val MIN_ELAPSED_MILLIS = 12_000L
        const val MIN_REASON_CHARS = 8

        private val TRANSCRIPTIONS = listOf(
            "quiet desk", "left hinge", "amber frost", "plain route", "cold anchor",
            "dim margin", "slow orbit", "brick valve", "firm slate", "gray punch"
        )
        private val SENTENCES = listOf(
            "The train leaves before the shop opens.",
            "Nine files went to the wrong folder again.",
            "She paid for lunch with the last note in her coat.",
            "A short walk clears more than an hour of staring.",
            "The ladder leaned on the window, not the wall.",
            "Every alarm I keep is one I chose to miss."
        )
    }
}
```

Two authorities, two questions, no overlap. `allowsSubmit(elapsedMillis, reason)` answers *may the user act yet?* — the floor and the reason, nothing else. `shouldAutoPass(mismatchCount)` answers *has this count earned the pass?* — three or more. `submitGateAnswer` composes them with `checkAnswer`: a row is logged when `(correct || shouldAutoPass(count)) && allowsSubmit(elapsed, reason)`. Do **not** weaken the floor or the reason check; those are the two properties F4 actually depends on.

Six rulings this text went through, all recorded in the ledger:
1. **First draft: `mismatchCount < MAX_MISMATCHES`.** Open at 0, 1 and 2, so a wrong answer at count 1 would have logged a pass — the effort F4 buys was free. Superseded.
2. **Second draft: `mismatchCount % MAX_MISMATCHES == 0L`.** Open at 0 and every multiple of three, shut at 1, 2, 4, 5. Superseded — and so was the `>= MAX_MISMATCHES` replacement that followed it, for the same reason: **any count clause inside `allowsSubmit` deadlocks the gate.** `mismatches` increments only inside the button's `onClick`, and `allowsSubmit` is what makes the button clickable, so a user at count 1 can never click again, can never reach the three failures that earn the auto-pass, and can only cancel and re-roll. The `>= MAX` reading also makes the plan's own hint text ("N more tries before this one is given to you") a lie, and §5.2's non-negotiable is explicit: a gate that can be failed is a trap and traps get uninstalled. Both drafts are recorded here because both were ratified before the trap was traced through the UI path — a reviewer reading either ruling as too clever is reading a mistake, not a trade.
3. **Final: `allowsSubmit` takes no count at all.** The count is not an input to *may I act*; it is an input to *did this earn it*, which is `shouldAutoPass`'s job and is applied by `submitGateAnswer`. GT-11 is rewritten to pin exactly that pairing — the button stays open while the tally is short, and `shouldAutoPass(1)` is false so a premature click earns nothing. GT-8 keeps the 2/3/7 ladder.
4. **Task 14 keeps the honest tally; do not reset it.** `GateRequestEntity` stores `mismatchCount` precisely so "passed on the first try" and "passed on the third" stay distinguishable (see Task 10), so the correct-answer branch submits with the count as it stands. The `mismatches = 0` line an earlier ruling demanded is removed — it was a workaround for clause 2's lockout, and it would have rewritten history.
5. **The stimulus belongs in the prompt** (review C1). `TRANSCRIBE` and `RESTATED` put the drawn phrase only in `expected` and left `prompt` a bare instruction ("Type exactly what you see" — with nothing to see), while `COUNT_BACK` correctly embedded `$start`. Task 14's screen renders `Text(task.prompt)` and destructures `task.expected` with `{ index, _ -> }`, discarding the value, so two of three task types were unanswerable and entry happened only after three forced mismatches — F4 inverted into "fail until the counter reaches 3". `single()` now interpolates the value, and GT-2's `task.prompt.contains(task.expected.first())` keeps it there; the old `prompt.length in 4..Int.MAX_VALUE` was a tautology that could never fail.
6. **`COUNT_BACK`'s wording contradicted its own answer key** (review I2): "Enter the next four values" after "Start at 411" reads as 404, 397, 390, 383, while `expected` is 411, 404, 397, 390 — a user doing the arithmetic correctly and following the sentence is failed four times for the wrong reason. GT-5 pins the inclusive list, so the list stays and the sentence becomes "Enter the four values, starting at $start". Spec §5.2 has the same loose phrasing and inherits the fix.

GT-1 also gains `values[2] shouldBe values[0] - 14`: the drafted assertions on `values[1]` and `values[3]` alone would pass a series with a wrong middle step.

- [ ] **Step 4: Verify — and confirm the suite actually ran**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.gate.GateTaskEngineTest" && \
grep -o 'tests="[0-9]*"' app/build/test-results/testDebugUnitTest/TEST-com.neuroflow.app.feature.hyperfocus.gate.GateTaskEngineTest.xml
```

Expected: green and `tests="12"`. A green build with `tests="0"` means the filter matched nothing — that is a failed task.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/feature/hyperfocus/gate/GateTaskEngine.kt \
        app/src/test/java/com/neuroflow/app/feature/hyperfocus/gate/GateTaskEngineTest.kt
git commit -q -m "feat: add GateTaskEngine, the effort-only cognitive gate

Randomized transcribe/count-back/restate task with a written-reason requirement, a
12-second floor and a three-mismatch auto-pass, so the gate costs deliberation and can
never become a trap that gets the app uninstalled." -- <same paths>
git show --name-only HEAD
```

## Task 7: `AppCatalogFilter` and `AppCatalogRepository` — enumerate and launch

Spec §3.3: the launcher's `AppRepository` is not copied. The filtering rule becomes a pure function under test; the Android shell stays thin.

**Files:**
- Create: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/data/AppCatalogFilter.kt`
- Create: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/data/AppCatalogRepository.kt`
- Test: `app/src/test/java/com/neuroflow/app/feature/hyperfocus/data/AppCatalogFilterTest.kt`

**Interfaces:**
- Consumes: `SystemExempt` from Task 5.
- Produces:
  ```kotlin
  data class InstalledApp(packageName: String, label: String, activity: String)
  object AppCatalogFilter { fun keep(apps: List<InstalledApp>, systemExempt: Set<String>): List<InstalledApp> }
  @Singleton class AppCatalogRepository @Inject constructor(
      @ApplicationContext context: Context,
      launcherApps: LauncherApps,
      systemExempt: SystemExempt
  ) {
      data class AppEntry(packageName: String, label: String, component: ComponentName, user: UserHandle)
      suspend fun launchableApps(): List<AppEntry>
      suspend fun launch(packageName: String): Boolean
      fun labelFor(packageName: String): String
  }
  ```

- [ ] **Step 1: Write the failing test**

```kotlin
package com.neuroflow.app.feature.hyperfocus.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private fun app(pkg: String, label: String = pkg) = InstalledApp(pkg, label, "$pkg.MainActivity")

class AppCatalogFilterTest : StringSpec({

    "AC-1: ordinary launchable apps keep" {
        AppCatalogFilter.keep(listOf(app("com.todoist")), setOf("android"))
            .map { it.packageName } shouldContainExactly listOf("com.todoist")
    }

    "AC-2: framework packages drop unless exempt" {
        val kept = AppCatalogFilter.keep(
            listOf(app("com.android.systemui"), app("android"), app("com.android.dialer")),
            setOf("android", "com.android.dialer")
        )
        // "android" is named in the exempt set, so it is kept; only com.android.systemui,
        // which is neither exempt nor seeded, drops.
        kept.map { it.packageName } shouldContainExactly listOf("android", "com.android.dialer")
    }

    "AC-3: blank activity rows are not launchable and drop" {
        AppCatalogFilter.keep(
            listOf(InstalledApp("com.example", "Example", "")), emptySet()
        ).size shouldBe 0
    }

    "AC-4: duplicates by package collapse to the first, in input order" {
        val kept = AppCatalogFilter.keep(
            listOf(app("com.todoist", "Todoist"), app("com.todoist", "Todoist Lite")), emptySet()
        )
        kept.size shouldBe 1
        kept.first().label shouldBe "Todoist"
    }

    "AC-5: ordering is stable by label, then package" {
        val kept = AppCatalogFilter.keep(
            listOf(app("z", "Alpha"), app("m", "Beta"), app("a", "Alpha")), emptySet()
        )
        kept.map { it.packageName } shouldContainExactly listOf("a", "z", "m")
    }

    "AC-6: the default essential seed is never filtered out of the catalog" {
        val seeded = listOf(
            "com.android.dialer", "com.android.mms", "com.android.contacts",
            "com.android.camera", "com.android.deskclock"
        )
        AppCatalogFilter.keep(seeded.map { app(it) }, emptySet())
            .map { it.packageName } shouldContainExactly seeded.sorted()
    }
})
```

- [ ] **Step 2: Run it, expect `unresolved reference: AppCatalogFilter`**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.data.AppCatalogFilterTest"
```

- [ ] **Step 3: Implement the filter**

```kotlin
package com.neuroflow.app.feature.hyperfocus.data

import com.neuroflow.app.feature.hyperfocus.surface.FocusSurfaceModel

data class InstalledApp(
    val packageName: String,
    val label: String,
    val activity: String
)

object AppCatalogFilter {

    private val FRAMEWORK_PREFIX = "com.android."

    fun keep(apps: List<InstalledApp>, systemExempt: Set<String>): List<InstalledApp> =
        apps.asSequence()
            .filter { it.packageName.isNotBlank() && it.activity.isNotBlank() }
            .filter {
                !it.packageName.startsWith(FRAMEWORK_PREFIX) ||
                    it.packageName in systemExempt ||
                    it.packageName in FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy({ it.label.lowercase() }, { it.packageName }))
            .toList()
}
```

Two escape hatches, not one, and the second is what Task 5's `CALL_PATH` ruling exposed. The `|| it.packageName in systemExempt` clause covers what must never be *gated*; the `|| … in DEFAULT_ESSENTIAL_SEED` clause covers what must be *offerable*. Only the dialer earns both, because Task 5 promoted the dialers into `SystemExempt`; `com.android.mms`, `com.android.contacts`, `com.android.camera` and `com.android.deskclock` are seeded essentials that appear in neither exempt set, and each starts with the rejected prefix. Without the seed clause the allowlist picker could never offer Messages, Contacts, Camera or Clock, so a user who cleared their tiers could never put them back — a hole exactly where the feature promises none. AC-6 pins it.

Note the ordering: `distinctBy` runs **before** `sortedWith`, so AC-4's "first in input order wins" survives the sort. Reversing those two operators silently changes which label a duplicated package shows.

- [ ] **Step 4: Implement the thin Android shell**

```kotlin
package com.neuroflow.app.feature.hyperfocus.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.neuroflow.app.feature.hyperfocus.surface.SystemExempt
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppCatalogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val launcherApps: LauncherApps,
    private val systemExempt: SystemExempt
) {
    data class AppEntry(
        val packageName: String,
        val label: String,
        val component: ComponentName,
        val user: UserHandle
    )

    suspend fun launchableApps(): List<AppEntry> = withContext(Dispatchers.Default) {
        val users: List<UserHandle> =
            context.getSystemService(UserManager::class.java)?.userProfiles
                ?: listOf(Process.myUserHandle())
        val rows = users.flatMap { user ->
            launcherApps.getActivityList(null, user).map { info ->
                InstalledApp(
                    packageName = info.componentName.packageName,
                    label = info.label.toString(),
                    activity = info.componentName.className
                ) to AppEntry(info.componentName.packageName, info.label.toString(), info.componentName, user)
            }
        }
        val keep = AppCatalogFilter.keep(rows.map { it.first }, systemExempt.packages())
            .map { it.packageName }.toSet()
        rows.filter { it.first.packageName in keep }.map { it.second }.distinctBy { it.packageName }
    }

    suspend fun launch(packageName: String): Boolean = withContext(Dispatchers.Default) {
        val target = launchableApps().firstOrNull { it.packageName == packageName }
            ?: return@withContext false
        runCatching {
            launcherApps.startMainActivity(target.component, target.user, null, null)
        }.isSuccess
    }

    fun labelFor(packageName: String): String =
        runCatching {
            launcherApps.getActivityList(packageName, Process.myUserHandle())
                .firstOrNull()?.label?.toString()
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: packageName
}
```

`launch()` re-queries the catalog rather than accepting a cached `AppEntry`: a `UserHandle` for a paused work profile goes stale and `startMainActivity` throws `StaleOperationException`, so re-querying is what makes the retry-after-pause path work without a special case. `QUERY_ALL_PACKAGES` must stay in the manifest (spec §7) because `getActivityList(null, user)` returns nothing useful under package-visibility filtering.

- [ ] **Step 5: Verify, including the Hilt graph**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh assemble && \
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.data.AppCatalogFilterTest" && \
grep -o 'tests="[0-9]*"' app/build/test-results/testDebugUnitTest/TEST-com.neuroflow.app.feature.hyperfocus.data.AppCatalogFilterTest.xml
```

Expected: build green (this is also the check that `LauncherApps` is already bound — `AppModule` provides it at `AppModule.kt:129`, verified before Task 7 was dispatched; if KSP reports a missing binding for `LauncherApps` or `SystemExempt`, add a `@Provides` to `HyperFocusModule`, never to `LauncherModule`, which Phase 3 deletes); then `tests="6"`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/feature/hyperfocus/data/AppCatalogFilter.kt \
        app/src/main/java/com/neuroflow/app/feature/hyperfocus/data/AppCatalogRepository.kt \
        app/src/test/java/com/neuroflow/app/feature/hyperfocus/data/AppCatalogFilterTest.kt
git commit -q -m "feat: add AppCatalogRepository for allowlist enumeration and launch

Small replacement for the launcher AppRepository: the keep/drop rule is a pure
filterable function and the LauncherApps shell stays thin." -- <same paths>
git show --name-only HEAD
```

### Task 7 amendment — ruling after its review (supersedes the Step 3 filter code and the AC-2/AC-4/AC-6 listings above)

Task 7 passed spec review byte-verbatim and still earned six Important findings, because the brief
itself was wrong in three places it could not see. This block is the corrected contract; the listings
above are kept unedited so the change and its reason stay visible.

**Ruling (the reviewer explicitly routed I6 to the controller): the `com.android.` prefix rule is
deleted, not narrowed.** It cannot do its job — every internal it aims at (`systemui`, `settings`,
`server.telecom`, the installers) is in `SystemExempt.STATIC` and therefore re-admitted by the exempt
clause on the very next line — so the *only* package it can ever drop is one the user installed or a
vendor app they can see on their own home screen. `com.android.chrome` and `com.android.vending` carry
that prefix, have launcher activities, and appear in neither `STATIC`, `CALL_PATH` nor the essential
seed. Trace what that costs: dropped from `launchableApps()` → Task 11's migrator tiers only launchable
packages → in no tier → `FocusSurfaceModel.isGated` says gated → Task 16's picker, fed by the same list,
cannot offer it. **The user has no route to allow their own browser**, which is the invisible-forever
bug B2 was chosen to kill. A "narrow it to an explicit internals list" option is a denylist and is
rejected for the same reason; "keep it for the grid but not the picker" splits the definition of
launchable and breaks the migrator. Consequence to carry into Tasks 12/14/15/16: `launchableApps()` now
*includes* Settings, systemui, installers and enabled IMEs, so the `STATIC` re-filter at the five display
sites is load-bearing — a display site that forgets it puts Settings on the black home.

**Ruling on I4 (sort computed then discarded):** the repository preserves the filter's order instead of
rebuilding in enumeration order. The black home's value is a surface that does not move; `getActivityList`
order changes when an app is installed or a badge clears, so an unsorted grid reads as unreliability — and
the spec says nothing about ordering, which made this plan-internal drift.

**Ruling on I1 (the crash path), with a correction to the reviewer's own fix.** Agreed that `launch()`
must not throw — its return type is `Boolean` and a method that reports failure by throwing reports
nothing — but the snippet in the review is itself defective: inside `runCatching { … }`,
`?: return@runCatching false` produces `Result.success(false)`, so `.isSuccess` is `true` and the method
returns *success* for a package that was never found. That is the same class of bug the review was filed
for, so it is fixed here rather than passed on: guard the lookup with `runCatching{…}.getOrNull()` and
return from `withContext`, not from `runCatching`.

```kotlin
// AppCatalogFilter.kt — one argument now; hiding is FocusSurfaceModel's job, not the catalog's.
object AppCatalogFilter {

    /**
     * No namespace rule by design: a package dropped here has no route back, because the migrator
     * and the picker both read this list. See the Global Constraint on catalog completeness.
     */
    fun keep(apps: List<InstalledApp>): List<InstalledApp> =
        apps.asSequence()
            .filter { it.packageName.isNotBlank() && it.activity.isNotBlank() }
            .distinctBy { it.packageName }
            .sortedWith(compareBy({ it.label.lowercase() }, { it.packageName }))
            .toList()
}
```

```kotlin
// AppCatalogRepository.kt — the three corrected members; `SystemExempt` leaves the constructor.
suspend fun launchableApps(): List<AppEntry> = withContext(Dispatchers.Default) {
    val users: List<UserHandle> =
        context.getSystemService(UserManager::class.java)?.userProfiles
            ?: listOf(Process.myUserHandle())
    // One paused work profile must not empty the catalog: getActivityList throws there.
    val rows = users.flatMap { user ->
        runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())
            .map { info ->
                val pkg = info.componentName.packageName
                val label = info.label.toString()
                InstalledApp(pkg, label, info.componentName.className) to
                    AppEntry(pkg, label, info.componentName, user)
            }
    }
    val firstByPackage = mutableMapOf<String, AppEntry>()
    rows.forEach { (_, entry) -> firstByPackage.putIfAbsent(entry.packageName, entry) }
    AppCatalogFilter.keep(rows.map { it.first }).mapNotNull { firstByPackage[it.packageName] }
}

/** Re-queried, not cached: a `UserHandle` from an earlier enumeration throws on launch. */
suspend fun launch(packageName: String): Boolean = withContext(Dispatchers.Default) {
    val target = runCatching { launchableApps().firstOrNull { it.packageName == packageName } }
        .getOrNull() ?: return@withContext false
    runCatching {
        launcherApps.startMainActivity(target.component, target.user, null, null)
    }.onFailure { Log.w("AppCatalog", "launch failed for $packageName", it) }.isSuccess
}

/** Personal profile only, so a work-profile package logs its package name in the audit row. */
suspend fun labelFor(packageName: String): String = withContext(Dispatchers.Default) {
    runCatching {
        launcherApps.getActivityList(packageName, Process.myUserHandle())
            .firstOrNull()?.label?.toString()
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: packageName
}
```

`firstByPackage` uses `putIfAbsent` — the plain `associateBy` the reviewer suggested keeps the **last**
row per package, which would hand a duplicated package to the work profile and invert the very ordering
rule AC-4 pins.

The test class becomes six cases with `keep()` taking one argument; the fixture gains an `activity`
default. AC-2, AC-4 and AC-6 are the three that were mutation-silent, and each is now red under the
mutation that used to survive it — AC-2 under a reintroduced prefix rule, AC-4 under a
`distinctBy`/`sortedWith` swap (its labels must *disagree* with the comparator: `Zeta` then `Alpha`),
AC-6 under any new silent drop.

```kotlin
private fun app(pkg: String, label: String = pkg, activity: String = "$pkg.MainActivity") =
    InstalledApp(pkg, label, activity)

"AC-1: ordinary launchable apps keep" {
    AppCatalogFilter.keep(listOf(app("com.todoist")))
        .map { it.packageName } shouldContainExactly listOf("com.todoist")
}

"AC-2: the catalog hides nothing by namespace — a com.android user app survives" {
    val kept = AppCatalogFilter.keep(
        listOf(app("com.android.chrome"), app("com.android.vending"),
               app("com.android.systemui"), app("com.todoist"))
    )
    kept.map { it.packageName } shouldContainExactly
        listOf("com.android.chrome", "com.android.systemui", "com.android.vending", "com.todoist")
}

"AC-3: blank activity rows are not launchable and drop" {
    AppCatalogFilter.keep(listOf(InstalledApp("com.example", "Example", ""))).size shouldBe 0
}

"AC-4: duplicates collapse to the row seen first, not the row that sorts first" {
    val kept = AppCatalogFilter.keep(listOf(app("com.todoist", "Zeta"), app("com.todoist", "Alpha")))
    kept.size shouldBe 1
    kept.first().label shouldBe "Zeta"
}

"AC-5: ordering is stable by label, then package" {
    val kept = AppCatalogFilter.keep(listOf(app("z", "Alpha"), app("m", "Beta"), app("a", "Alpha")))
    kept.map { it.packageName } shouldContainExactly listOf("a", "z", "m")
}

"AC-6: the seeded essentials are never dropped from the catalog" {
    val seed = FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED.toList()
    val kept = AppCatalogFilter.keep(seed.map { app(it) })
    kept.size shouldBe seed.size
    kept.map { it.packageName } shouldContainAll seed
}
```

Suite count is unchanged at **6**, so the full suite stays at **387**. Not taken in this round, on
purpose: a last-known-good catalog cache (the shipped `AppRepository.kt:103-107` has one) — that is
Task 15's per-tap latency measurement to justify, recorded as U5, not a Phase 2 guess.

## Task 8: Per-app pass lifecycle (D3 + E1) and deletion of the device-wide unlock

Three things happen here and they cannot be separated without leaving a broken build: `activeUnlockExpiresAt` is replaced by `activePassExpiresAt`; `RewardEngine.isUnlockActive`/`secondsRemaining` (which free the *whole device*) are replaced by `isPassActive`/`passSecondsRemaining` (which free one package); and `submitCode` stops being the thing that unlocks, becoming the thing that buys a budget that `grantPass` then spends on one app.

**Spec delta 3:** spec §4.1 lists only `blockedPackages` as removed, yet §5.5 states pass validity must be expressed *solely* by `activePassPackage != null && activePassExpiresAt > now`. `activeUnlockExpiresAt` is therefore vestigial once this task lands and is deleted here rather than left as a second, overlapping source of truth.

**Files:**
- Modify: `feature/hyperfocus/data/HyperFocusDataStore.kt` (`:28` field, `:112` serialize, `:149` deserialize)
- Modify: `feature/hyperfocus/domain/RewardEngine.kt:104-116`
- Modify: `feature/hyperfocus/domain/HyperFocusManager.kt` (interface `:31-41`, `:182`, `submitCode` `:257-303`, `triggerEmergencyBypass` `:327-354`)
- Modify: `feature/hyperfocus/service/UnlockTimerService.kt:50,53`
- Modify: `feature/hyperfocus/HyperFocusViewModel.kt:89-96` and `:180-183`
- Modify: `feature/hyperfocus/screens/BlockingOverlayScreen.kt:78,470`
- Modify: `presentation/launcher/components/AppIcon.kt:116` — a launcher file that still has to *compile* until Phase 3 deletes it; repoint the call, touch nothing else
- Test: `app/src/test/java/com/neuroflow/app/feature/hyperfocus/domain/PassLifecycleTest.kt`
- Modify test: `app/src/test/java/com/neuroflow/app/feature/hyperfocus/domain/RewardEngineTest.kt` (HF-3 and the three `isUnlockActive`/`secondsRemaining` cases become pass cases)

**Interfaces:**
- Consumes: `activePassPackage` / `activePassExpiresAt` from Task 4; `FocusSurfaceModel` semantics from Task 5 (a pass is only useful because it enters the resolved allowlist).
- Produces:
  ```kotlin
  object RewardEngine {
      fun isPassActive(prefs: HyperFocusPreferences, nowMillis: Long): Boolean
      fun passSecondsRemaining(prefs: HyperFocusPreferences, nowMillis: Long): Long?
  }
  interface HyperFocusManager {
      suspend fun grantPass(packageName: String, durationMinutes: Int)
      suspend fun clearActivePass()
      suspend fun expirePassIfDue(nowMillis: Long)
      suspend fun triggerEmergencyBypass(packageName: String)
  }
  ```
  `activate` / `activateTimed` keep their `blockedPackages` parameter in this task and lose it in Task 12, when the activation UI moves to the allowlist.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.neuroflow.app.feature.hyperfocus.domain

import com.neuroflow.app.domain.model.HyperFocusSessionMode
import com.neuroflow.app.domain.model.HyperFocusState
import com.neuroflow.app.domain.model.RewardTier
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val NOW = 5_000_000L

class PassLifecycleTest : StringSpec({

    "PL-1: a package with no deadline is not an active pass" {
        RewardEngine.isPassActive(
            HyperFocusPreferences(activePassPackage = "com.instagram.android"), NOW
        ).shouldBeFalse()
    }

    "PL-2: a deadline with no package is not an active pass" {
        RewardEngine.isPassActive(
            HyperFocusPreferences(activePassExpiresAt = NOW + 60_000), NOW
        ).shouldBeFalse()
    }

    "PL-3: both present and in the future is an active pass" {
        val prefs = HyperFocusPreferences(activePassPackage = "com.instagram.android", activePassExpiresAt = NOW + 60_000)
        RewardEngine.isPassActive(prefs, NOW).shouldBeTrue()
        RewardEngine.passSecondsRemaining(prefs, NOW) shouldBe 60L
    }

    "PL-4: an expired pass reports inactive and no remaining seconds" {
        val prefs = HyperFocusPreferences(activePassPackage = "com.instagram.android", activePassExpiresAt = NOW - 1)
        RewardEngine.isPassActive(prefs, NOW).shouldBeFalse()
        RewardEngine.passSecondsRemaining(prefs, NOW).shouldBeNull()
    }

    "PL-5: at the exact deadline the pass is gone, not pending" {
        RewardEngine.isPassActive(
            HyperFocusPreferences(activePassPackage = "x", activePassExpiresAt = NOW), NOW
        ).shouldBeFalse()
    }

    "PL-6: FULL tier's Int.MAX_VALUE minutes yield a deadline that never arrives" {
        val minutes = RewardTier.FULL.unlockMinutes
        val deadline = NOW + minutes.toLong() * 60_000L
        deadline shouldBe 5_000_000L + 2147483647L * 60_000L
        RewardEngine.isPassActive(
            HyperFocusPreferences(activePassPackage = "x", activePassExpiresAt = deadline),
            System.currentTimeMillis()
        ).shouldBeTrue()
    }

    "PL-7: clearing the package keeps the budget deadline so E1's clock survives the switch" {
        val prefs = HyperFocusPreferences(activePassPackage = "com.instagram.android", activePassExpiresAt = NOW + 300_000)
        val cleared = prefs.copy(activePassPackage = null)
        cleared.activePassExpiresAt shouldBe prefs.activePassExpiresAt
        RewardEngine.isPassActive(cleared, NOW).shouldBeFalse()
    }

    "PL-8: pass validity ignores isActive - the session check belongs to the caller" {
        RewardEngine.isPassActive(
            HyperFocusPreferences(isActive = false, state = HyperFocusState.INACTIVE,
                activePassPackage = "x", activePassExpiresAt = NOW + 60_000), NOW
        ).shouldBeTrue()   // pass validity is a fact about the two fields, not about isActive
    }

    "PL-9: TIME_BASED sessions are refused by emergency, not by the pass model" {
        val prefs = HyperFocusPreferences(isActive = true, sessionMode = HyperFocusSessionMode.TIME_BASED,
            emergencyUsed = false, activePassPackage = null)
        prefs.sessionMode shouldBe HyperFocusSessionMode.TIME_BASED
        prefs.emergencyUsed shouldBe false
    }
})
```

PL-8 deliberately pins the split: `isPassActive` knows nothing about `isActive`, because `AppBlockingService` already returns early when the session is off (`:234`) and a second guard there would make the two conditions impossible to reason about separately. PL-9 is a documentation test — the refusal lives in `triggerEmergencyBypass`, and the only way to keep it visible is to name it.

- [ ] **Step 2: Run it and expect `unresolved reference: isPassActive`**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.domain.PassLifecycleTest"
```

- [ ] **Step 3: Replace the two `RewardEngine` unlock functions**

Delete `isUnlockActive` (`:104-109`) and `secondsRemaining` (`:111-116`) entirely — including the dead `FULLY_UNLOCKED` short-circuits — and add:

```kotlin
    fun isPassActive(prefs: HyperFocusPreferences, nowMillis: Long): Boolean =
        prefs.activePassPackage != null && (prefs.activePassExpiresAt ?: 0L) > nowMillis

    fun passSecondsRemaining(prefs: HyperFocusPreferences, nowMillis: Long): Long? {
        if (!isPassActive(prefs, nowMillis)) return null
        return ((prefs.activePassExpiresAt ?: return null) - nowMillis) / 1000
    }
```

- [ ] **Step 4: Swap the preference field**

In `HyperFocusDataStore.kt`: delete the `activeUnlockExpiresAt` declaration at `:28` and its two serializer lines (`:112`, `:149`). An old blob that still carries `activeUnlockExpiresAt` is ignored on read, and the write side now emits `activePassPackage`/`activePassExpiresAt`, so an upgrading device simply starts with no pass — which is the correct read of "the previous unlock was device-wide and no longer means anything".

- [ ] **Step 5: Add the three pass methods to `HyperFocusManager`**

Add to the interface (`:31-41`) and implement in `HyperFocusManagerImpl`:

```kotlin
    override suspend fun grantPass(packageName: String, durationMinutes: Int) {
        if (packageName.isBlank() || durationMinutes <= 0) return
        val now = System.currentTimeMillis()
        var granted = false
        hyperFocusDataStore.update { prefs ->
            if (!prefs.isActive) {
                prefs
            } else {
                granted = true
                // E1: an unspent budget keeps ticking, so a re-grant inherits its deadline
                // rather than starting a fresh window.
                val liveDeadline = prefs.activePassExpiresAt?.takeIf { it > now }
                val deadline = liveDeadline ?: (now + durationMinutes.toLong() * 60_000L)
                prefs.copy(activePassPackage = packageName, activePassExpiresAt = deadline)
            }
        }
        if (granted) {
            runCatching { context.startForegroundService(Intent(context, UnlockTimerService::class.java)) }
        }
    }

    override suspend fun clearActivePass() {
        hyperFocusDataStore.update { it.copy(activePassPackage = null) }
    }

    override suspend fun expirePassIfDue(nowMillis: Long) {
        hyperFocusDataStore.update { prefs ->
            val deadline = prefs.activePassExpiresAt
            if (prefs.activePassPackage == null || deadline == null || deadline > nowMillis) prefs
            else prefs.copy(activePassPackage = null, activePassExpiresAt = null)
        }
    }
```

Then in `activateInternal` change `activeUnlockExpiresAt = null` (`:182`) to `activePassExpiresAt = null`, and in `deactivate` add `activePassPackage = null` — `HyperFocusPreferences()` already covers it, since `deactivate` writes a fresh default instance at `:68`.

- [ ] **Step 6: Make `submitCode` buy a budget instead of unlocking the device**

Rewrite the tail of `submitCode` (`:281-302`). The `FULL` tier's permanent unlock becomes a deadline that never arrives (spec §5.4), so the special-cased `null` disappears and both branches share one expression:

```kotlin
        val deadline = now + match.tier.unlockMinutes.toLong() * 60_000L
        unlockCodeRepository.markUsed(match.id, deadline)
        hyperFocusDataStore.update {
            val newState = if (match.tier == RewardTier.FULL) HyperFocusState.FULL_REWARD_PENDING else it.state
            it.copy(
                activePassExpiresAt = deadline,
                wrongCodeAttempts = 0,
                pendingCodeId = null,
                state = newState
            )
        }
        runCatching { context.startForegroundService(Intent(context, UnlockTimerService::class.java)) }
        return UnlockResult.Success(match.tier.unlockMinutes)
```

Note what is *not* here: no package. Consuming a code sets `activePassExpiresAt` only, so `isPassActive` stays false until the user picks an app in the gateway and `grantPass` supplies the package. That is D3 in one line — a code buys minutes, never access.

The `activeUnlockExpiresAt = null` guard on the old `if (expiresAt != null)` service start disappears with it; `UnlockTimerService` now owns a budget countdown rather than an unlock countdown.

- [ ] **Step 7: Emergency grants a single-app pass**

Replace `triggerEmergencyBypass()` (`:327-354`) with a package-taking version. Keep the existing double-check pattern (pre-read plus re-check inside the update lambda) — it exists to close the race where two taps both pass the one-time check:

```kotlin
    override suspend fun triggerEmergencyBypass(packageName: String) {
        if (packageName.isBlank()) return
        val prefs = hyperFocusDataStore.current()
        if (!prefs.isActive || prefs.emergencyUsed) return
        if (prefs.sessionMode == HyperFocusSessionMode.TIME_BASED) return

        val now = System.currentTimeMillis()
        val expiresAt = now + EMERGENCY_MINUTES * 60_000L
        var shouldStartService = false

        hyperFocusDataStore.update {
            if (!it.isActive || it.emergencyUsed || it.sessionMode == HyperFocusSessionMode.TIME_BASED) {
                it
            } else {
                shouldStartService = true
                it.copy(
                    activePassPackage = packageName,
                    activePassExpiresAt = expiresAt,
                    emergencyUsed = true,
                    currentTier = RewardTier.NONE
                )
            }
        }

        if (shouldStartService) {
            runCatching { context.startForegroundService(Intent(context, UnlockTimerService::class.java)) }
        }
    }
```

Add `private const val EMERGENCY_MINUTES = 10` to the companion object at `:52-54`, replacing the inline `10 * 60 * 1000L` comment at `:333`. Emergency reuses the *same two fields* as a reward pass on purpose: there is then exactly one thing for `AppBlockingService` and the home screen to read, and a spent emergency cannot leave a stale device-wide unlock behind.

- [ ] **Step 8: Update every reader of the deleted API**

Exact sites, all found by grep (`grep -rn "isUnlockActive\|activeUnlockExpiresAt\|secondsRemaining\|triggerEmergencyBypass" app/src`):

`UnlockTimerService.kt:50,53` — `prefs.activeUnlockExpiresAt` → `prefs.activePassExpiresAt`, and the clearing update → `it.copy(activePassPackage = null, activePassExpiresAt = null)`.

`HyperFocusViewModel.kt:89-96` — replace both flows:

```kotlin
    val isPassActive: StateFlow<Boolean> = hyperFocusDataStore.flow
        .map { RewardEngine.isPassActive(it, System.currentTimeMillis()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val passSecondsRemaining: StateFlow<Long?> = combine(tickerFlow, hyperFocusDataStore.flow) { _, prefs ->
        RewardEngine.passSecondsRemaining(prefs, System.currentTimeMillis())
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
```

`HyperFocusViewModel.kt:180-183` — `activateEmergencyBypass()` becomes `activateEmergencyBypass(packageName: String)` and forwards it; the UI that calls it is Task 14's `EmergencyControl`, mounted on Task 15's home surface.

`BlockingOverlayScreen.kt:78,470` — rename the collected value to `isPassActive` and adjust the `if (isUnlockActive)` branch to `if (isPassActive)`; the overlay now means "this one app is open to you", so its copy is re-worded in Task 13 when the gateway absorbs this screen. Do not delete the screen here.

`presentation/launcher/components/AppIcon.kt:116` — `!RewardEngine.isUnlockActive(prefs)` becomes `!RewardEngine.isPassActive(prefs, System.currentTimeMillis())`. This file is deleted in Phase 3; the one-line edit exists only so Phase 2 keeps compiling.

`RewardEngineTest.kt` — the four cases named `isUnlockActive`/`secondsRemaining` (`:49`, `:63`, `:70`, `:92`, `:98`) and the header comment at `:20` are rewritten to the pass API. Delete them rather than translating each: `PassLifecycleTest` from Step 1 already covers the same ground with `nowMillis` injected instead of a real clock.

- [ ] **Step 9: Verify**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh assemble && \
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.domain.*" && \
grep -rn "isUnlockActive\|activeUnlockExpiresAt\|RewardEngine.secondsRemaining" app/src || echo "OLD UNLOCK API GONE"
```

Expected: build green, all HyperFocus domain suites green (`PassLifecycleTest` 9 tests, plus the surviving `RewardEngineTest` / `HyperFocusManagerTest` / `AESUtilTest` / `UnlockCodeRepositoryTest` cases), then `OLD UNLOCK API GONE`. `HyperFocusManagerTest` may need its `submitCode` expectations updated — the assertion at `:196-197` reads `updated.activeUnlockExpiresAt shouldNotBe null`; change it to `activePassExpiresAt` and keep the same "is in the future" check.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/feature/hyperfocus \
        app/src/main/java/com/neuroflow/app/presentation/launcher/components/AppIcon.kt \
        app/src/test/java/com/neuroflow/app/feature/hyperfocus
git commit -q -m "feat: replace device-wide unlock with a per-app pass budget

D3 and E1: a reward code now buys wall-clock minutes and grantPass spends them on one
package, so an unlock no longer frees every app on the device. activeUnlockExpiresAt is
replaced by activePassPackage + activePassExpiresAt, and emergency uses the same pair.
This tightens existing behavior on purpose." -- <same paths>
git show --name-only HEAD
```

### Task 8 amendment — pre-flight corrections (apply with the task; the listings above are left as written so the deltas stay legible)

Read against the tree, not against itself, Task 8's brief contained two changes that would have failed
to compile, one access leak, one frozen flow and one test that cannot fail.

1. **`AppBlockingService.kt:248` is a sixth call site, and the brief's own grep prints it.** Step 8
   claims "exact sites, all found by grep" and lists five files; `AppBlockingService` is not among them,
   yet it holds `if (RewardEngine.isUnlockActive(prefs)) return@launch`. Deleting `isUnlockActive`
   without editing here fails `assemble`. It becomes a **per-package** exemption —
   `if (RewardEngine.isPassActive(prefs, passNow) && prefs.activePassPackage == pkg) return@launch`
   (`pkg` is already in scope) — never the device-wide form, which would free every app the moment any
   pass is live and contradict D3 plus this task's own commit message. Side effect to accept: tamper
   reporting at `:251-255` resumes for every app except the pass app, because the old guard skipped it
   wholesale. `FULLY_UNLOCKED` was verified to have **no write site** in `app/src/main`, so deleting the
   short-circuits strands no live session.
2. **`HyperFocusManagerTest.kt:186` and `:215` call `triggerEmergencyBypass()` with no arguments** and
   break the test-source compile; the file is absent from the brief's list. Both take
   `"com.instagram.android"`. The brief's Step 9 mis-attributes `:196-197` to `submitCode` — those lines
   are inside the *emergency* case; repoint them to `activePassExpiresAt` and add
   `updated.activePassPackage shouldBe "com.instagram.android"`, which is the only JVM-visible statement
   of what emergency actually grants under D3.
3. **`submitCode` leaks the previously-open app onto the freshly bought budget.** Step 6 copies
   `activePassExpiresAt` alone, so submitting a code while a pass is still live makes `isPassActive`
   true for the **old** package with the **new** deadline — a reward code re-opens whatever was last open,
   with no gateway pick. Add `activePassPackage = null` to the same `update`, which is what Step 6's prose
   already claims, and pin it with `updated.activePassPackage shouldBe null` in the existing
   `submitCode with valid code returns UnlockResult.Success` case (`:67`).
4. **`isPassActive` as a `map` flow freezes at its last value.** A wall-clock predicate re-evaluated only
   on a prefs write keeps reporting `true` after the deadline passes. `passSecondsRemaining` two lines
   below already uses `combine(tickerFlow, …)`, and `tickerFlow` exists at `HyperFocusViewModel.kt:45`;
   drive both from it.
5. **PL-9 is a tautology** — it builds prefs with `sessionMode = TIME_BASED, emergencyUsed = false` and
   asserts those two constants back. Same defect class as Task 6's GT-2, which is why that suite stayed
   green over an unanswerable gate. Replace it with E1's arithmetic, which has no test at all, by
   extracting the rule Step 5 inlines into `grantPass`:

   ```kotlin
   /** E1: a live budget keeps ticking and is inherited; only a spent or absent budget restarts. */
   fun passDeadline(existingExpiresAt: Long?, nowMillis: Long, durationMinutes: Int): Long =
       existingExpiresAt?.takeIf { it > nowMillis }
           ?: nowMillis + durationMinutes.toLong() * 60_000L
   ```

   `grantPass` computes its deadline through this. PL-9 then asserts inherit (`NOW+300_000` stays),
   spent-restarts (`NOW-1` → `NOW+600_000`), absent-restarts (`null` → `NOW+600_000`) and never-extends
   (`NOW+1` with `Int.MAX_VALUE` minutes stays `NOW+1`). Suite stays at **9**.
6. **Counts and stale refs.** `RewardEngineTest` loses **five** cases (`:49, :63, :70, :91, :96` — the
   brief says four), keeping 7, so the suite is 387 − 5 + 9 = **391**; anything lower is a dropped suite.
   `HyperFocusDataStore.kt` refs move to `:36`/`:129`/`:171`. Step 8's grep pattern also matches
   `UnlockResult.Lockout(val secondsRemaining)` at `HyperFocusManager.kt:27` and the two emergency case
   *names* — Step 9's narrower pattern is the one that must print `OLD UNLOCK API GONE`. `AESUtilTest`
   is inert and never appears in the XMLs; Task 18 owns it.

### Task 8 amendment, part 2 — deltas found while executing it (these supersede the corresponding lines above)

1. **A seventh call site: `presentation/launcher/components/AppIcon.kt:116`.** The correction above fixes
   `AppBlockingService` from device-wide to per-package, then Step 8 asks for the same translation here as
   `!RewardEngine.isPassActive(prefs, System.currentTimeMillis())`. That is inverted-dangerous: under a
   per-app pass, `isPassActive` is true whenever *any* package holds a pass, so the `!` form suppresses
   the gate redirect for every blocked app tapped in the drawer while an unrelated app is open. Written
   instead as `!(RewardEngine.isPassActive(prefs, now) && prefs.activePassPackage == appInfo.packageName)`.
2. **An eighth call site: `presentation/launcher/HomeScreen.kt:483`** reads the renamed flow
   (`hyperFocusViewModel.unlockSecondsRemaining` → `passSecondsRemaining`). Only the property access
   changes; the local name and `HyperFocusStatusBar`'s parameter stay, because both are UI copy that
   Task 13/15 rewrite and this file dies in Phase 3. **Task 18 sweep:** `HyperFocusStatusBar`'s parameter
   is still `unlockSecondsRemaining` and its text still says "Apps unlocked", as does
   `UnlockTimerService`'s notification — all three now describe a per-app pass.
3. **Item 3's assertion, as written, cannot fail.** `updated.activePassPackage shouldBe null` passed with
   the reset line deleted, because the fixture's prefs left the package at its default `null`. The
   fixture must carry a live grant (`activePassPackage = "com.instagram.android"` plus a live budget) for
   the reset to be observable — otherwise this amendment documents the exact tautology it exists to
   prevent. Verified: with the fixture fixed, deleting `activePassPackage = null` turns the case RED.
4. **Step 6's guard is narrowed, not deleted.** Removing `if (expiresAt != null)` entirely starts
   `UnlockTimerService` for the FULL tier, whose `unlockMinutes` is `Int.MAX_VALUE` — a deadline about
   4,000 years out that the service counts down in a *persistent foreground notification*, formatting it
   with `(remainingMillis / 60_000).toInt()`, which overflows `Int` into garbage. Kept
   `if (match.tier != RewardTier.FULL)`: every timed tier still gets the ticking budget Step 6 wants, and
   FULL has nothing to count down. Same hazard still ahead of Task 14 — a gateway that grants
   `grantPass(pkg, Int.MAX_VALUE)` starts the service unguarded, so either skip the timer for a
   never-expiring grant or clamp the display.
5. **Step 5 and Step 7 keep the shipped service-flag idiom** (default `true` after the pre-read, flip to
   `false` inside the conflicting branch) rather than the brief's inverted one. `coEvery { update(any()) }
   just Runs` never invokes the lambda in the existing mockk harness, so a flag set *inside* it can never
   become `true` in a test, and `verify(exactly = 1) { context.startForegroundService(…) }` fails.
   `grantPass` also gains the pre-read `if (!hyperFocusDataStore.current().isActive) return` that
   `triggerEmergencyBypass` already had: it is what makes "no session, no write" testable and stops a
   stale grant from starting a foreground service.
6. **Emergency deliberately does not inherit.** It sets `now + EMERGENCY_MINUTES` directly instead of
   `RewardEngine.passDeadline(…)`, because inheritance would let a 30-second live budget swallow a
   10-minute emergency. The one tap that must never under-deliver gets a floor. Accepted cost: an
   emergency can shorten a longer bought budget — stricter, not a trap.
7. **Six cases added beyond the brief's test list** (item 6's count changes: the suite is 387 − 5 + 9 + 6 =
   **397**, `HyperFocusManagerTest` 10 → 16). `grantPass`, `clearActivePass` and `expirePassIfDue` had zero
   coverage, and `expirePassIfDue` is the function that enforces E1's "the pass ends when you leave the
   granted app". Covered: inherit-a-live-budget, refuse-without-a-session, clear-when-spent, leave-a-live-
   pass-alone, keep-a-package-less-budget, close-the-app-but-keep-the-budget. Each was mutation-proven to
   go RED when its rule is removed.

## Task 9: Reduce `HyperFocusState` to the three states that are actually written

Spec §5.5. `MICRO_UNLOCKED`, `PARTIAL_UNLOCKED`, `EARNED_UNLOCKED`, `PLANNING` and `FULLY_UNLOCKED` have no write sites anywhere in `app/src/main/java`; they are read in six places that Task 8 already removed two of. Leaving unreachable enum entries alongside live ones is how the next reader decides the state machine means something it does not.

**Files:**
- Modify: `app/src/main/java/com/neuroflow/app/domain/model/Enums.kt:54-65`
- Modify: `feature/hyperfocus/domain/HyperFocusManager.kt:244,250`
- Modify: `feature/hyperfocus/service/HyperFocusMonitorService.kt:75`
- Modify: `feature/hyperfocus/components/HyperFocusStatusBar.kt:157`

**Interfaces:**
- Consumes: Task 8's pass functions (the deleted branches were all attempts to derive "unlocked" from `state`).
- Produces: `enum class HyperFocusState { INACTIVE, ACTIVE, FULL_REWARD_PENDING }`.

- [ ] **Step 1: Confirm the write-site claim before deleting anything**

```bash
grep -rn "state = HyperFocusState\.\|state == HyperFocusState\.\|\.state\s*=\s*HyperFocusState\." app/src/main/java
```

Expected: assignments only of `ACTIVE` (`HyperFocusManager.kt:177`), `FULL_REWARD_PENDING` (`:251`, `:286`) and the `INACTIVE` default in the data class. If the grep turns up an assignment of one of the five doomed constants, **stop** — that state is live, §5.5's premise is wrong, and this task needs re-scoping before it deletes anything.

- [ ] **Step 2: Cut the enum to three entries**

`Enums.kt:54-65` becomes:

```kotlin
enum class HyperFocusState {
    INACTIVE,
    ACTIVE,
    FULL_REWARD_PENDING
}
```

Whether a pass exists is now expressed only by `activePassPackage` + `activePassExpiresAt` (Task 8), so `state` describes the session and nothing else. One consequence to accept explicitly: a device upgrading with a persisted `state` of `FULLY_UNLOCKED` hits the `try/catch` around `HyperFocusState.valueOf` in `deserialize` and reads back as `INACTIVE` while `isActive` stays `true`. `isActive` is the flag every enforcement path checks, so the session survives and the tier display simply resets — which is preferable to a stale `FULLY_UNLOCKED` that Task 8 has already decided means nothing.

- [ ] **Step 3: Delete the four remaining read sites**

`HyperFocusManager.kt:241-254` — the tier-progression guard loses both `FULLY_UNLOCKED` comparisons and keeps the `FULL_REWARD_PENDING` one, which is the only state that can actually be present:

```kotlin
        if (newTier.ordinal > prefs.currentTier.ordinal ||
            (completedSinceActivation >= prefs.dailyTaskTarget &&
             prefs.state != HyperFocusState.FULL_REWARD_PENDING)
        ) {
            hyperFocusDataStore.update {
                val updatedTier = if (newTier.ordinal > it.currentTier.ordinal) newTier else it.currentTier
                val updatedState = if (completedSinceActivation >= prefs.dailyTaskTarget &&
                    it.state != HyperFocusState.FULL_REWARD_PENDING
                ) HyperFocusState.FULL_REWARD_PENDING else it.state
                it.copy(currentTier = updatedTier, state = updatedState)
            }
        }
```

`HyperFocusMonitorService.kt:75` — drop the `prefs.state == HyperFocusState.FULLY_UNLOCKED ||` term from whatever condition it is one operand of, leaving the other operands untouched. Read the surrounding `if` first: this is the service that keeps the session alive, so deleting the wrong operand here silently changes when the monitor re-arms.

`HyperFocusStatusBar.kt:157` — delete the `prefs.state == HyperFocusState.FULLY_UNLOCKED -> { … }` branch of that `when`. If the `when` is exhaustive without an `else`, keep it exhaustive by folding the branch's body into the `ACTIVE` case rather than removing the arm.

- [ ] **Step 4: Verify**

```bash
grep -rn "FULLY_UNLOCKED\|MICRO_UNLOCKED\|PARTIAL_UNLOCKED\|EARNED_UNLOCKED\|HyperFocusState.PLANNING" app/src || echo "DEAD STATES GONE"
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh assemble && bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.*"
```

Expected: `DEAD STATES GONE`, then green. Note `RewardTier` keeps its `MICRO`/`PARTIAL`/`EARNED`/`FULL` entries — those are live and are the pass budget in minutes (spec §4.1); only `HyperFocusState` shrinks. Do not confuse the two enums.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/domain/model/Enums.kt \
        app/src/main/java/com/neuroflow/app/feature/hyperfocus
git commit -q -m "refactor: reduce HyperFocusState to the three states that are written

Five of the eight entries had no write site anywhere in main sources; pass validity is
expressed by activePassPackage/activePassExpiresAt, so the tier-shaped states were
second, stale descriptions of the same fact." -- <same paths>
git show --name-only HEAD
```

## Task 10: `GateRequestEntity`, its DAO, and database version 21

F4's log — the one part of the gate that produces information. Spec §4.3: this is the only new table, and it is deliberately not backed up to Firestore (§11).

**Files:**
- Create: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/data/GateRequestEntity.kt`
- Create: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/data/GateRequestDao.kt`
- Modify: `app/src/main/java/com/neuroflow/app/data/local/NeuroFlowDatabase.kt` (entity list `:295`, `version = 20` `:296`, new DAO getter, new `MIGRATION_20_21` after `MIGRATION_19_20` `:288-292`)
- Modify: `app/src/main/java/com/neuroflow/app/di/AppModule.kt:59-80` (register the migration)
- Modify: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/di/HyperFocusModule.kt` (provider)
- Test: `app/src/androidTest/java/com/neuroflow/app/data/local/RoomMigrationV20ToV21Test.kt`

**Interfaces:**
- Consumes: nothing.
- Produces:
  ```kotlin
  @Entity(tableName = "gate_requests") data class GateRequestEntity(…){ companion object { fun new(…) } }
  @Dao interface GateRequestDao {
      @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entity: GateRequestEntity)
      @Query("SELECT * FROM `gate_requests` ORDER BY `createdAtMillis` DESC LIMIT :limit")
      suspend fun recent(limit: Int): List<GateRequestEntity>
      @Query("SELECT … ") suspend fun countsByPackage(from: Long, to: Long): List<PackageRequestCount>
  }
  data class PackageRequestCount(val packageName: String, val requests: Int, val abandoned: Int)
  enum class GateGrantSource { REWARD_CODE, EMERGENCY, ABANDONED }
  ```
  `NeuroFlowDatabase.gateRequestDao()` and database `version = 21`.

- [ ] **Step 1: Create the entity**

```kotlin
package com.neuroflow.app.feature.hyperfocus.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "gate_requests")
data class GateRequestEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String?,
    val packageName: String,
    val appLabel: String,
    val statedReason: String,
    val taskType: String,
    val mismatchCount: Int,
    val elapsedMillis: Long,
    val grantSource: String,
    val createdAtMillis: Long
)
```

`sessionId`, `taskType` and `grantSource` stay `String` rather than the enum types, matching how `UnlockCodeEntity` stores its tier and keeping the table readable after an enum is edited (Task 9 just deleted five enum values — that is exactly the failure this avoids). `mismatchCount` is included because "passed on the first try" and "passed on the third" are different signals, and the elapsed time alone cannot tell them apart.

- [ ] **Step 2: Create the DAO**

```kotlin
package com.neuroflow.app.feature.hyperfocus.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

data class PackageRequestCount(
    val packageName: String,
    val requests: Int,
    val abandoned: Int
)

@Dao
interface GateRequestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: GateRequestEntity)

    @Query("SELECT * FROM `gate_requests` ORDER BY `createdAtMillis` DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<GateRequestEntity>

    @Query(
        "SELECT packageName, COUNT(*) AS requests, " +
            "SUM(CASE WHEN grantSource = 'ABANDONED' THEN 1 ELSE 0 END) AS abandoned " +
            "FROM `gate_requests` WHERE createdAtMillis BETWEEN :from AND :to " +
            "GROUP BY packageName ORDER BY requests DESC"
    )
    suspend fun countsByPackage(from: Long, to: Long): List<PackageRequestCount>

    @Query("DELETE FROM `gate_requests` WHERE createdAtMillis < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long): Int
}
```

`countsByPackage` is the query that makes the log worth having: it answers "which apps do you keep reaching for mid-session" without the analytics screen needing to know a gate exists. `deleteOlderThan` exists so the table cannot grow without bound; nothing calls it yet, and Task 11's note says where to wire it.

- [ ] **Step 3: Register the entity, bump the version, add the getter**

In `NeuroFlowDatabase.kt`: add `GateRequestEntity::class` to the `entities = [...]` list at `:295`, change `version = 20` to `version = 21`, add `abstract fun gateRequestDao(): GateRequestDao` beside the other getters, and add the import for the two new types. Then, directly after `MIGRATION_19_20` (`:288-292`):

```kotlin
val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `gate_requests` (
                `id` TEXT NOT NULL,
                `sessionId` TEXT,
                `packageName` TEXT NOT NULL,
                `appLabel` TEXT NOT NULL,
                `statedReason` TEXT NOT NULL,
                `taskType` TEXT NOT NULL,
                `mismatchCount` INTEGER NOT NULL,
                `elapsedMillis` INTEGER NOT NULL,
                `grantSource` TEXT NOT NULL,
                `createdAtMillis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
    }
}
```

Column order and nullability must match the entity exactly — Room validates the created schema against its own expectation at the first query after migration and throws `IllegalStateException: Room cannot verify the data integrity` on a mismatch, on device, days later. `sessionId` is the only nullable column; if you mark it `TEXT NOT NULL` the app will boot fine and then crash the first time a user opens the gate before ever activating a session.

- [ ] **Step 4: Register the migration and the provider**

`di/AppModule.kt`: add `MIGRATION_20_21` after `MIGRATION_19_20` in the `addMigrations(...)` list (`:59-80`). Missing this is not a compile error — Room falls through to `fallbackToDestructiveMigrationOnDowngrade()`'s sibling behavior and, with no migration path registered, throws at database open. Verify the list ends:

```bash
sed -n '59,82p' app/src/main/java/com/neuroflow/app/di/AppModule.kt | grep -c "MIGRATION"
```

Expected: 20 (the 19 existing plus one).

`feature/hyperfocus/di/HyperFocusModule.kt` companion object:

```kotlin
        @Provides
        fun provideGateRequestDao(database: NeuroFlowDatabase): GateRequestDao = database.gateRequestDao()
```

- [ ] **Step 5: Write the migration test where it can actually run**

Room migrations need a real SQLite/Android runtime, and Robolectric is inert in `app/src/test`, so this belongs in `androidTest`:

Create `app/src/androidTest/java/com/neuroflow/app/data/local/RoomMigrationV20ToV21Test.kt`:

```kotlin
package com.neuroflow.app.data.local

import androidx.room.testing.migration.MigrationTestHelpers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMigrationV20ToV21Test {

    @Test
    fun migration20to21CreatesGateRequests() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = MigrationTestHelper.createDatabaseFromAsset(
            context, "neuroflow_database", NeuroFlowDatabase::class.java, 20
        )
        val migration = MIGRATION_20_21
        migration.migrate(db)
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='gate_requests'")
        assertTrue(cursor.moveToFirst())
        cursor.use {
            assertTrue(it.getString(0) == "gate_requests")
        }
        db.close()
    }
}
```

This is the one place in the plan where JUnit4 is correct: `androidTest` runs on the real JUnit4 runner, unlike `app/src/test`. It needs the `room-migration-testing` helper and the exported schemas under `app/schemas` (already enabled by `exportSchema = true` and the KSP `room.schemaLocation` arg in `app/build.gradle.kts:104-106`). If `androidTestImplementation("androidx.room:room-testing:2.6.1")` is not already in the dependencies block, add it — it is not there today.

- [ ] **Step 6: Build, and be honest about what you could not run**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh assemble && bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh atcompile
```

Expected: both green. **`connectedAndroidTest` cannot run on this machine** (no AVD in use for this project, `adb` not on `PATH`), so the migration itself is *written but unexecuted*. Report it that way; do not claim a passing migration test. The build does prove the schema-compilation half: KSP validates the DAO queries and the entity against Room's expected table at compile time, which catches most typos in `countsByPackage` and the column list.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/feature/hyperfocus/data/GateRequestEntity.kt \
        app/src/main/java/com/neuroflow/app/feature/hyperfocus/data/GateRequestDao.kt \
        app/src/main/java/com/neuroflow/app/feature/hyperfocus/di/HyperFocusModule.kt \
        app/src/main/java/com/neuroflow/app/data/local/NeuroFlowDatabase.kt \
        app/src/main/java/com/neuroflow/app/di/AppModule.kt \
        app/src/androidTest/java/com/neuroflow/app/data/local/RoomMigrationV20ToV21Test.kt \
        app/build.gradle.kts
git commit -q -m "feat: add gate_requests table and DAO for the cognitive gate log

Every gate attempt is recorded with its stated reason, task type, mismatch count and
elapsed time, plus countsByPackage for the 'which apps do you keep reaching for'
question. Database version 20 -> 21." -- <same paths>
git show --name-only HEAD
```

### Task 10 amendment — deltas found while executing it (these supersede the corresponding lines above)

Eight defects in the brief, all caught before the first green build. Recorded so the next
schema task doesn't re-derive them.

1. **Step 5's test body does not exist.** `MigrationTestHelper.createDatabaseFromAsset(context, …)`
   is not an API in `androidx.room:room-testing:2.6.1`, and neither is the import
   `androidx.room.testing.migration.MigrationTestHelper` — the class is
   `androidx.room.testing.MigrationTestHelper`. Verified against the real artifact (downloaded the
   AAR, `javap` on `classes.jar`), which exposes exactly:
   `MigrationTestHelper(Instrumentation, Class<out RoomDatabase>, List<AutoMigrationSpec>)`
   (the 2-arg `(Instrumentation, Class)` overload is `@Deprecated`),
   `createDatabase(name: String, version: Int): SupportSQLiteDatabase`, and
   `runMigrationsAndValidate(name: String, version: Int, validateDroppedTables: Boolean, vararg Migration)`.
   The shipped test uses `@get:Rule` + `createDatabase("neuroflow_database", 20)` +
   `runMigrationsAndValidate(…, 21, true, MIGRATION_20_21)`.
2. **`createDatabase` reads the schema from androidTest *assets*, and nothing wired them.** The
   helper sets `assetsFolder = RoomDatabaseClass.canonicalName`, so it opens
   `assets/com.neuroflow.app.data.local.NeuroFlowDatabase/20.json`. `exportSchema = true` plus the
   KSP `room.schemaLocation` arg put those files under `app/schemas/`, which is **not** an asset
   directory — Step 5's "already enabled by … the KSP room.schemaLocation arg" is only half true.
   Added to `app/build.gradle.kts`:
   `sourceSets { getByName("androidTest") { assets.srcDir("$projectDir/schemas") } }`.
3. **`room-testing` was not in the dependencies block** (the brief said "add it — it is not there
   today", correctly this time) — and it was not in the local Gradle cache either, so the first
   `atcompile` resolved it from Google Maven. Network is available on this machine; the cache was
   not.
4. **Step 4's expected migration count is off by one.** `addMigrations(...)` already listed **20**
   migrations (`1_2`…`16_17`, `17_18`, `18_19`, `17_19`, `19_20`), not 19, so after `MIGRATION_20_21`
   `grep -c MIGRATION_` over that block must read **21**, not 20.
5. **`AppModule.kt` imports every migration by name.** Adding `MIGRATION_20_21` to the
   `addMigrations(...)` list without `import com.neuroflow.app.data.local.MIGRATION_20_21` is the
   first thing that fails the build — which is the good case, since it is a compile error rather
   than the runtime "no migration path" the step warns about. The brief's Step 4 never mentions the
   import.
6. **Step 1's precedent for `String` columns is wrong, and the decision still stands.**
   `UnlockCodeEntity` does *not* store its tier as text: it declares `val tier: RewardTier` and
   `@TypeConverters(Converters::class)` maps it with `RewardTier.valueOf(value)`, which **throws** on
   a persisted name that no longer exists. Task 9 deleted five enum values from this codebase two
   commits ago, so crash-on-read is not hypothetical — but that is a reason to keep the *log* table
   text-typed that has nothing to do with `UnlockCodeEntity`. `grantSource`/`taskType` stay `String`
   for that reason; the enum names are still the values written, so `grantSource = 'ABANDONED'` in
   `countsByPackage` remains correct.
7. **`new()` takes the enums, so the write site stays type-checked.** The brief left the signature as
   `(…)`. Shipped: `GateRequestEntity.new(packageName, appLabel, statedReason,
   taskType: GateTaskType, mismatchCount, elapsedMillis, grantSource: GateGrantSource,
   sessionId: String? = null, createdAtMillis: Long = System.currentTimeMillis())`, storing `.name`.
   `GateTaskType` (`gate/GateTaskEngine.kt:5`) already exists, so Task 14 cannot typo either value
   into the log.
8. **`SUM(...)` wrapped in `COALESCE(…, 0)`.** `ELSE 0` already makes the column non-null in practice;
   the wrapper states that intent where Room's nullability check can see it. No behaviour change.

Two more things the brief got wrong in its favour, and one it left undone:

- Step 3's "column order must match the entity exactly" overstates it: Room's runtime check compares
  parsed columns, indices and PK order, not SQL text. Order was matched anyway, and the parity was
  proved on this machine by diffing `MIGRATION_20_21`'s SQL against the `createSql` Room put in the
  freshly exported `21.json` — identical modulo whitespace around parentheses.
- **`app/schemas/.../21.json` was missing from Step 7's commit list** while `app/schemas` is tracked
  (versions 1–20 are all committed). It is in the commit; without it the repo contradicts its own
  schema export.
- **Step 2's "Task 11's note says where to wire `deleteOlderThan`" is false** — nothing in Tasks
  11–18 mentions it, so as planned the table grows without bound and the DAO carries a dead method.
  **Ruling:** Task 14 calls `deleteOlderThan(nowMillis - RETENTION_MILLIS)` right after its
  `gateRequestDao.insert(…)` (`:2789`, `:2810`), with one `private const val` retention of 90 days in
  the ViewModel. Chosen over a new worker because the insert path is the only place that learns a row
  arrived, it needs no scheduling surface, and the sweep deletes nothing on almost every call. Cost if
  wrong: gate history older than 90 days disappears — which only shrinks what the analytics screen can
  show, never what it enforces.
- **`recent()` and `countsByPackage()` have no caller in any task.** Grepping the plan for the DAO's
  read methods finds only `gateRequestDao.insert(…)` in Task 14 — the log as planned is write-only.
  That inverts §4.3's bargain: the user pays the cognitive floor so the gate can produce information,
  and as planned the price is charged while nothing reads the receipt. Both queries are kept (a read
  surface is the cheapest thing to add after the table exists), but this is now a **Task 18
  obligation**: either wire one of them into the analytics screen, or delete the unread method rather
  than ship an unused query. Do not let it end as a third entry in the "nothing calls
  it" column.

**Verification actually run:** `assemble` PASS (so KSP accepted both DAO queries and generated
`GateRequestDao_Impl`), exported-`21.json` SQL parity as above, 21 migrations registered,
`atcompile` PASS (so the rewritten test type-checks against the real `MigrationTestHelper`),
unit suite 397 green / 0 failures — unchanged, because this task's test is an instrumentation test.
`connectedAndroidTest` still cannot run here (no AVD, no `adb` on `PATH`), so **the migration is
written but unexecuted**; that is device-test table row 10, which Task 10 now makes real, and ledger
entry U8. What the build *does* prove is the
schema-compilation half: a typo in the column list or in `countsByPackage` fails the build instead of
the device.

## Task 11: One-shot migration from the old denylist to the new allowlist

Spec §4.4: the complement of the old denylist becomes the new allowlist, so an upgrading user's intent survives and the newly-default-blocked set is exactly what they had already chosen to block. The decision is a pure function; only the store/catalog plumbing around it needs a device.

**Files:**
- Create: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/data/AllowlistMigrator.kt`
- Create: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/data/AllowlistMigrationRunner.kt`
- Modify: `feature/hyperfocus/data/HyperFocusDataStore.kt` (add `allowlistMigratedAt: Long?` and `distractionScores: Map<String, Int>`; `blockedPackages` **stays** — Task 16 deletes it in the same commit that stops reading it)
- Modify: `NeuroFlowApplication.kt` (invoke the runner at startup)
- Modify: `presentation/launcher/LauncherViewModel.kt:339-340,818-823` (the distraction-scores map changes store; this file dies in Phase 3 but must compile and keep working now)
- Test: `app/src/test/java/com/neuroflow/app/feature/hyperfocus/data/AllowlistMigratorTest.kt`

**Interfaces:**
- Consumes: `FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED` (Task 5), `AppCatalogRepository.launchableApps()` (Task 7), `SystemExempt.packages()` (Task 5).
- Produces:
  ```kotlin
  data class AllowlistTiers(val essentialPackages: Set<String>, val allowedPackages: Set<String>)
  object AllowlistMigrator {
      fun derive(blockedPackages: Set<String>, launchable: Set<String>, essentialSeed: Set<String>,
                 systemExempt: Set<String>, selfPackage: String): AllowlistTiers
  }
  @Singleton class AllowlistMigrationRunner @Inject constructor(…) {
      suspend fun runIfNeeded()
  }
  ```
  And `HyperFocusPreferences` gains `allowlistMigratedAt: Long? = null` and `distractionScores: Map<String, Int> = emptyMap()`. `blockedPackages` is still present at the end of this task — Task 16 removes the field and its last readers together, so no build state ever has an enforcement path with nothing to read.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.neuroflow.app.feature.hyperfocus.data

import com.neuroflow.app.feature.hyperfocus.surface.FocusSurfaceModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.collections.shouldNotContainAnyOf

private const val SELF = "com.neuroflow.app"
private val LAUNCHABLE = setOf(
    "com.android.dialer", "com.android.mms", "com.android.settings", "com.android.systemui",
    "com.instagram.android", "com.netflix.mediaclient", "com.todoist", "org.telegram.messenger", SELF
)
private val EXEMPT = setOf("android", "com.android.systemui", "com.android.settings")

class AllowlistMigratorTest : StringSpec({

    val blocked = setOf("com.instagram.android", "com.netflix.mediaclient")

    "AM-1: the complement of the denylist becomes the allowed tier" {
        val tiers = AllowlistMigrator.derive(blocked, LAUNCHABLE, FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED, EXEMPT, SELF)
        tiers.allowedPackages shouldContain "com.todoist"
        tiers.allowedPackages shouldContain "org.telegram.messenger"
        tiers.allowedPackages shouldNotContainAnyOf listOf("com.instagram.android", "com.netflix.mediaclient")
    }

    "AM-2: the essential tier is the seed intersected with what is installed" {
        val tiers = AllowlistMigrator.derive(blocked, LAUNCHABLE, FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED, EXEMPT, SELF)
        tiers.essentialPackages shouldContainExactly setOf("com.android.dialer", "com.android.mms", SELF)
    }

    "AM-3: blocked packages are never promoted into essential" {
        val tiers = AllowlistMigrator.derive(
            setOf("com.android.dialer"), LAUNCHABLE, FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED, EXEMPT, SELF
        )
        tiers.essentialPackages shouldNotContain "com.android.dialer"
        tiers.allowedPackages shouldNotContain "com.android.dialer"
    }

    "AM-4: exempt packages and self never appear in the user-facing allowed tier" {
        val tiers = AllowlistMigrator.derive(blocked, LAUNCHABLE, FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED, EXEMPT, SELF)
        tiers.allowedPackages shouldNotContainAnyOf listOf("com.android.systemui", "com.android.settings", SELF)
    }

    "AM-5: an empty denylist still yields a usable essential tier" {
        val tiers = AllowlistMigrator.derive(emptySet(), LAUNCHABLE, FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED, EXEMPT, SELF)
        tiers.essentialPackages.isNotEmpty() shouldBe true
        tiers.allowedPackages shouldContain "com.instagram.android"
    }

    "AM-6: nothing installed means nothing to allow, without inventing packages" {
        val tiers = AllowlistMigrator.derive(emptySet(), emptySet(), FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED, EXEMPT, SELF)
        tiers.essentialPackages shouldBe emptySet()
        tiers.allowedPackages shouldBe emptySet()
    }

    "AM-7: derive is pure and idempotent for the same inputs" {
        val a = AllowlistMigrator.derive(blocked, LAUNCHABLE, FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED, EXEMPT, SELF)
        val b = AllowlistMigrator.derive(blocked, LAUNCHABLE, FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED, EXEMPT, SELF)
        a shouldBe b
    }
})
```

Add `import io.kotest.matchers.shouldBe` and use `.shouldBe true` where the draft above says `shouldBe true` on a bare boolean — the kotest form is `boolean shouldBe true`. AM-2 and AM-6 are the two that matter: AM-2 proves the migration cannot promise a dialer that is not installed, and AM-6 proves it cannot invent one.

- [ ] **Step 2: Run it, expect `unresolved reference: AllowlistMigrator`**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.data.AllowlistMigratorTest"
```

- [ ] **Step 3: Implement the migrator**

```kotlin
package com.neuroflow.app.feature.hyperfocus.data

data class AllowlistTiers(
    val essentialPackages: Set<String>,
    val allowedPackages: Set<String>
)

object AllowlistMigrator {

    fun derive(
        blockedPackages: Set<String>,
        launchable: Set<String>,
        essentialSeed: Set<String>,
        systemExempt: Set<String>,
        selfPackage: String
    ): AllowlistTiers {
        val essential = (launchable intersect essentialSeed) - blockedPackages
        val allowed = launchable - blockedPackages - systemExempt - essential - selfPackage
        return AllowlistTiers(essentialPackages = essential, allowedPackages = allowed)
    }
}
```

Subtracting `blockedPackages` from `essential` is AM-3: a user who explicitly blocked their dialer had a reason, and silently re-listing it as essential because a seed list said so would override a decision they already made. `FocusSurfaceModel` always re-adds `systemExempt` and `selfPackage` at read time, so keeping them out of the stored tiers here costs no safety and keeps the editor list honest.

- [ ] **Step 4: Add the two preference fields**

In `HyperFocusDataStore.kt`, add two fields where Task 4 left the allowlist block:

```kotlin
    val allowlistMigratedAt: Long? = null,
    val distractionScores: Map<String, Int> = emptyMap(),
```

Serialize them in the same shape as the existing fields — `allowlistMigratedAt` exactly like `sessionEndsAtMillis` (`JSONObject.NULL` on write, `isNull`/`optLong`/`takeIf { it > 0L }` on read), and `distractionScores` as a `JSONObject` of `pkg → Int`, following `PinnedAppsDataStore.kt:199-215` (`parseDistractionScores`) and `:245-256` (`encodeDistractionScores`) as the reference for the wire format so a copied map survives the round trip:

```kotlin
        val scores = JSONObject()
        prefs.distractionScores.forEach { (pkg, score) -> scores.put(pkg, score) }
        json.put("distractionScores", scores)
```

```kotlin
                distractionScores = buildMap {
                    val objScores = obj.optJSONObject("distractionScores")
                    if (objScores != null) {
                        objScores.keys().forEach { key -> put(key, objScores.optInt(key, 50)) }
                    }
                },
```

- [ ] **Step 5: Implement the runner and wire it into startup**

```kotlin
package com.neuroflow.app.feature.hyperfocus.data

import com.neuroflow.app.BuildConfig
import com.neuroflow.app.feature.hyperfocus.surface.FocusSurfaceModel
import com.neuroflow.app.feature.hyperfocus.surface.SystemExempt
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot: reads the pre-allowlist launcher prefs, converts the denylist into the
 * complement allowlist, and copies the launcher's distraction scores across. Guarded by
 * allowlistMigratedAt so it runs once per install and never again.
 */
@Singleton
class AllowlistMigrationRunner @Inject constructor(
    private val hyperFocusDataStore: HyperFocusDataStore,
    private val appCatalogRepository: AppCatalogRepository,
    private val systemExempt: SystemExempt,
    private val legacyLauncherPrefs: LegacyLauncherPrefs
) {
    suspend fun runIfNeeded() {
        val prefs = hyperFocusDataStore.current()
        if (prefs.allowlistMigratedAt != null) return
        if (prefs.isActive) return   // never rewrite tiers underneath a live session

        val launchable = appCatalogRepository.launchableApps().map { it.packageName }.toSet()
        val tiers = AllowlistMigrator.derive(
            blockedPackages = prefs.blockedPackages,
            launchable = launchable,
            essentialSeed = FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED,
            // STATIC, not packages(): a dialer that is exempt for enforcement reasons must still
            // land in a user tier, or the Pixel dialer belongs to neither tier and the
            // unreachable-count footer lies about an app that is always reachable.
            systemExempt = SystemExempt.STATIC,
            selfPackage = BuildConfig.APPLICATION_ID
        )
        val legacyScores = legacyLauncherPrefs.distractionScoresSnapshot()

        hyperFocusDataStore.update {
            it.copy(
                essentialPackages = tiers.essentialPackages,
                allowedPackages = tiers.allowedPackages,
                distractionScores = if (legacyScores.isNotEmpty()) legacyScores else it.distractionScores,
                allowlistMigratedAt = System.currentTimeMillis()
            )
        }
    }
}
```

`blockedPackages` is still a live field at the end of this task (Task 16 removes it together with its last readers), so the migration reads the denylist straight off the preferences. That is precisely why these two tasks must stay in this order: the value has to outlive the code that reads it by exactly one commit.

The `if (prefs.isActive) return` guard is the difference between a migration and a surprise. An upgrading user who is mid-session would otherwise watch their allowlist change underneath them, and `AppBlockingService` reads that list on every foreground event. Skipping an active session costs nothing: the guard runs again at the next startup, and a session's own deactivation (`HyperFocusManagerImpl.deactivate`, which writes a fresh `HyperFocusPreferences()` at `:68`) leaves `isActive` false for the run that follows.

`LegacyLauncherPrefs` is a separate reader because the distraction scores live in `launcher_prefs`, not in HyperFocus's own store — `PinnedAppsDataStore.kt:15` declares `private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_prefs")`, and `:115` defines `DISTRACTION_SCORES = stringPreferencesKey("distraction_scores")`. The delegate is `private` to that file, so duplicate the delegate declaration in the new file rather than editing the launcher:

```kotlin
private val Context.legacyLauncherDataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_prefs")

@Singleton
class LegacyLauncherPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun distractionScoresSnapshot(): Map<String, Int> =
        context.legacyLauncherDataStore.data.first()[KEY_DISTRACTION_SCORES]
            ?.let(::parseLegacyScores)
            ?: emptyMap()

    private fun parseLegacyScores(json: String?): Map<String, Int> { … }

    companion object {
        private val KEY_DISTRACTION_SCORES = stringPreferencesKey("distraction_scores")
    }
}
```

Copy `parseDistractionScores` from `PinnedAppsDataStore.kt:199-215` into `parseLegacyScores` verbatim — same wire format, so a map written by the launcher today reads back identically here. Duplicating the `preferencesDataStore(name = "launcher_prefs")` delegate is safe: DataStore hands out one instance per name per process, the second declaration is in a file that survives Phase 3 while the launcher's does not, and after Phase 3 this class is the only reader left. There is **no** `blocked_packages` key in `launcher_prefs` (checked: `PinnedAppsDataStore.kt:105-125` lists every key; `hidden_packages` and `locked_packages` are launcher display concepts, not the HyperFocus denylist), so the migration must not go looking for one.

Wire the call into `NeuroFlowApplication.kt`, where `hyperFocusDataStore` is already injected at `:42`, inside the same `onCreate` scope that pre-warms `AppRepository` (`:95`):

```kotlin
    @Inject lateinit var allowlistMigrationRunner: AllowlistMigrationRunner
    …
    applicationScope.launch {
        runCatching { allowlistMigrationRunner.runIfNeeded() }
            .onFailure { Log.w(TAG, "Allowlist migration did not complete", it) }
    }
```

`runCatching` here is required, not defensive: `launchableApps()` throws on a work profile that is paused, and a failed migration must leave the app bootable with an empty allowlist — which Task 14's activation guard then refuses to start a session over, per spec §8 — instead of crashing on every launch.

- [ ] **Step 6: Update the two remaining readers**

`HyperFocusActivationSheet.kt:115` still pre-checks `distractionScores.filter { it.value > 70 }.keys` to build the old denylist picker. Leave it working: it receives the map as a parameter (`:57`), and Task 14 replaces this sheet's body with the allowlist editor, at which point the pre-selection disappears with it. Editing it here would mean reworking a picker that is about to be deleted.

`LauncherViewModel.kt:339-340` sources `distractionScores` from `pinnedAppsDataStore`; repoint it to `hyperFocusDataStore.flow.map { it.distractionScores }`, and `updateDistractionScore` at `:818-823` writes through `hyperFocusDataStore.update`. `LauncherSettings.kt:193,198` calls that function and needs no edit. Both launcher files die in Phase 3 — this edit exists so the map keeps a working writer between Task 11 and Task 14, when the new allowlist screen becomes the permanent writer.

- [ ] **Step 7: Verify**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh assemble && \
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.data.AllowlistMigratorTest" && \
grep -o 'tests="[0-9]*"' app/build/test-results/testDebugUnitTest/TEST-com.neuroflow.app.feature.hyperfocus.data.AllowlistMigratorTest.xml
```

Expected: `tests="7"` and green. Then confirm exactly who still reads the denylist:

```bash
grep -rn "blockedPackages" app/src/main/java | grep -v "AllowlistMigrat"
```

Expected: three groups and nothing else — `AppBlockingService.kt:133` and `:246` (Task 15's enforcement flip), the `HyperFocusManager` `activate`/`activateTimed` parameters plus `DeviceOwnerKioskManager.syncHyperFocusBlockedPackagesSuspension` (Tasks 12 and 16), and `HyperFocusActivationSheet`'s picker (Task 14). If a fourth group shows up, it is a denylist reader no task owns and this plan has a hole — stop and add it to the owning task before committing.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/feature/hyperfocus/data \
        app/src/main/java/com/neuroflow/app/NeuroFlowApplication.kt \
        app/src/test/java/com/neuroflow/app/feature/hyperfocus/data \
        app/src/main/java/com/neuroflow/app/presentation/launcher/LauncherViewModel.kt
git commit -q -m "feat: migrate the app denylist into a one-shot complement allowlist

The complement of the old blockedPackages becomes allowedPackages, essential comes from
the default seed intersected with what is actually installed, and the launcher's
distraction scores copy across before launcher_prefs stops being read. Guarded by
allowlistMigratedAt so it runs once per install." -- <same paths>
git show --name-only HEAD
```

### Task 11 amendment (recorded during execution)

**1. Step 7's expected inventory is wrong, and the error hides the plan's one real hole.** Run
against HEAD as shipped it reports nine groups, not three. Six are owned and were simply not listed
here: `HyperFocusViewModel.kt:158-177` (Task 16 `:4410`), `AppIcon.kt:115` and
`LauncherSettings.kt:935-948` (Task 16 `:4294` and its Phase-3 deletion), `HyperFocusDataStore.kt`
(the field's own declaration/serialize/deserialize, deleted by Task 16 `:4252`), and
`HyperFocusSessionEntity.kt:13` with `FirestoreBackupRepository.kt:730,745` and
`NeuroFlowDatabase.kt:126` — that last trio is the *session history table*, not the live policy, and
Task 16's own grep at `:4473` already excludes it. Two groups in this task's text name the wrong
owner: the enforcement flip is **Task 12**, not 15, and the kiosk suspension is **Task 13**, not 12/16.

**2. One live reader had no owner anywhere in the plan: `NeuroFlowApplication.kt:110-115`.** It calls
`syncHyperFocusBlockedPackagesSuspension(context, hfPrefs.blockedPackages, …)` and is named by
neither Task 13's Files (`:2646`, which lists only the two callers inside `HyperFocusManager`) nor
Task 16's reader list (`:4012`, which lists `BootReceiver:84` and `LauncherViewModel:1219,:1246` but
not this one), nor Task 17's `NeuroFlowApplication.kt:9,39,95-97`. As planned it survives Task 13's
rename and the build goes red with no owner. **Ruling: Task 13 owns all six call sites**, because the
rename plus the new `launchable` parameter breaks every caller in the same commit — an owner who
delays any of them to Task 16 or 17 leaves a commit that does not compile. Task 13's Files is
corrected below. Cost if wrong: none to correctness; if the complement turns out to be wrong for a
boot-time caller, the fix is one line in one commit rather than four scattered across three tasks.

**3. The plan contradicts itself about which task deletes `blockedPackages`.** `:2248`, `:2266` and
`:2452` all say "Task 15 removes the field"; the deletion is actually specified in **Task 16**
(`:4012`, `:4252`, `:4294`), and `:70` agrees. Ruling: Task 16, since Task 16 is where the last
readers die and the rule is to delete a field in the commit that deletes its last reader. Task 15
must not touch the field.

**4. `deactivate()` and `activateInternal()` would have erased this task's own output.** Both built
a fresh `HyperFocusPreferences()` (`:72` and the `update {}` inside `activateInternal`). Under B2 the
two tiers, `distractionScores` and `allowlistMigratedAt` are global, so a deactivation that writes a
blank object destroys the allowlist the user curated *and* re-arms the one-shot migration — which
then runs against the `blockedPackages` that same reset just emptied and derives "allow everything".
That is not a hypothetical: it is the state of every install after its first session ends. Fixed by
`HyperFocusPreferences.afterSession()`, which clears the session fields and leaves the globals alone,
used by both call sites. Proved by SR-1/SR-2 and by mutating `afterSession` to wipe the four globals
(both cases fail).

**5. The brief's `LegacyLauncherPrefs` duplicated the DataStore delegate.** Adding a second
`preferencesDataStore(name = "launcher_prefs")` property in a new file while
`PinnedAppsDataStore.kt:15` still declares one gives the process two `DataStore` instances over the
same file, which is the documented crash condition ("The data store must be a top-level private
property… only one instance per file can exist for a given context") — and the delegate caches per
property, not per name, so both exist. Fixed by hoisting the single delegate to
`feature/hyperfocus/data/LegacyLauncherPrefs.kt` as `launcherPreferencesDataStore`, deleting the one
in `PinnedAppsDataStore.kt` and repointing its three call sites. Task 17's launcher deletion must keep
this file alive: the delegate outlives the launcher because the tiers' scores live in
`HyperFocusPreferences`, but the *legacy read* needs the old name for one migration.

**6. Dropped the brief's unused `systemExempt: SystemExempt` constructor injection.** The runner
passes `SystemExempt.STATIC`, a static companion val, so injecting the singleton bought nothing and
made the runner need a context in tests. Its absence is what lets all seven MR cases run on the JVM.

**7. Three places where the brief's code would have written data the user never had.**
`distractionScores = it.distractionScores.ifEmpty { legacyScores }` instead of a plain assignment (a
score written between `current()` and `update {}` must not be clobbered — MR-6), `launchable.isEmpty()
→ return` before deriving (an empty catalog is a failed `PackageManager` query, not a user with zero
apps; stamping it as migrated would pin an empty allowlist for the life of the install — MR-4), and a
`Number`-checked parse of the `distractionScores` JSON object instead of the brief's
`optInt(key, 50)`, which would invent a score of 50 for any value that failed to parse.

**8. Test count is 17, not the 7 Step 7 expects.** AM-1..AM-6 as briefed, **AM-7 replaced** (the
brief's "derive is pure and idempotent" called `derive` twice and compared the results — it passes for
any function, including one that returns the wrong constant; it now asserts the two tiers are a
partition, which is what caught the `- essential` mutation), plus AM-8, SR-1, SR-2, MR-1..MR-7.
**MR-7 exists because the `SystemExempt.STATIC` decision was unpinned**: every other clause of
`derive` has a mutation that fails a test, but passing `packages()` instead of `STATIC` — which would
drop the dialer out of *both* tiers and leave it invisible to the editor — failed nothing. It now
fails MR-7.

**9. Step 8's commit list is short by five files.** It covers `feature/hyperfocus/data`,
`NeuroFlowApplication.kt`, the test package and `LauncherViewModel.kt`; the task also changes
`HyperFocusManager.kt` (defect 4) and `presentation/launcher/data/PinnedAppsDataStore.kt` (defect 5),
and those must be in the same commit or the build state after Step 8 has two `launcher_prefs`
delegates again — or a `deactivate()` that no longer compiles.

**Verification actually run:** `assemble` PASS; `AllowlistMigratorTest` 8, `HyperFocusSessionResetTest`
2, `AllowlistMigrationRunnerTest` 7 green; full unit suite 414 / 0 failures (baseline 397 + 17). Nine
mutations, each restored and confirmed byte-identical by `diff -q`: dropping `- blockedPackages` from
essential fails AM-3 alone; dropping `- essential` from allowed fails AM-7 alone; dropping
`- systemExempt` fails AM-4 + AM-7; dropping `- blockedPackages` from allowed fails AM-1, AM-3, AM-7;
making `afterSession()` wipe the four globals fails SR-1 + SR-2; dropping each of the runner's three
guards fails MR-2/MR-3/MR-4 respectively; a plain `legacyScores` assignment fails MR-6; and
`STATIC + CALL_PATH` fails MR-7. **Nothing in this task was run on a device** — the migration is
JVM-proved against a mocked catalog, and whether it derives the right tiers from a real install's
denylist is device-test table row 11 (ledger U9).

## Task 12: Invert `AppBlockingService` from denylist to allowlist

Spec §6.1. This is the enforcement path that works without device-owner privileges, so it is the one that actually decides the user experience. Two things die here: the `blockedPackages` membership test, and the device-wide `isUnlockActive` exemption that currently frees *every* app whenever any unlock is live. The second is a real tightening and the commit message must say so.

**Files:**
- Modify: `feature/hyperfocus/service/AppBlockingService.kt` — `@Inject` fields (`:32-33`), `packageTamperReceiver` (`:119-139`), `checkAndBlockApp` (`:229-288`), imports
- Test: none on the JVM (an `AccessibilityService` needs a device); behavior is in the Phase 2 device test plan

**Interfaces:**
- Consumes: `FocusSurfaceModel.isGated` / `resolvedAllowlist` (Task 5), `SystemExempt.packages()` (Task 5), `RewardEngine.isPassActive` (Task 8), `HyperFocusManager.clearActivePass` / `expirePassIfDue` (Task 8).
- Produces: no new public API. `checkAndBlockApp` keeps its signature `(pkg: String, className: String?, eventSnapshot: String?)`; its three callers (`:109`, the event handler, and the polling fallback) are unchanged.

- [ ] **Step 1: Inject the exemption source**

Add beside the existing `@Inject` fields at `:32-33`:

```kotlin
    @Inject lateinit var systemExempt: com.neuroflow.app.feature.hyperfocus.surface.SystemExempt
```

`@AndroidEntryPoint` is already on the class (`:30`), so no other wiring is needed. An `AccessibilityService` cannot get constructor injection, which is why `SystemExempt` is a field here and a constructor parameter everywhere else.

- [ ] **Step 2: Replace the two gating decisions in `checkAndBlockApp`**

Lines `:244-254` currently read:

```kotlin
            val protectedSurfaceDecision = evaluateProtectedSurface(pkg, className, eventSnapshot)
            val isTamperSensitive = protectedSurfaceDecision?.shouldBlock == true
            val isUserBlockedApp = pkg in prefs.blockedPackages
            if (!isUserBlockedApp && !isTamperSensitive) return@launch
            if (RewardEngine.isUnlockActive(prefs)) return@launch
```

Replace with:

```kotlin
            val now = System.currentTimeMillis()
            val exempt = systemExempt.packages()
            val protectedSurfaceDecision = evaluateProtectedSurface(pkg, className, eventSnapshot)
            val isTamperSensitive = protectedSurfaceDecision?.shouldBlock == true
            val isGatedApp = FocusSurfaceModel.isGated(
                pkg, prefs, exempt, BuildConfig.APPLICATION_ID, now
            )
            if (!isGatedApp && !isTamperSensitive) {
                // E1: leaving the granted app ends the pass; the budget clock keeps running.
                if (!isGatedApp && RewardEngine.isPassActive(prefs, now) && pkg != prefs.activePassPackage) {
                    hyperFocusManager.clearActivePass()
                }
                return@launch
            }
```

`if (RewardEngine.isUnlockActive(prefs)) return@launch` is **deleted, not narrowed**. An allowed app arriving in the foreground now ends the pass, which is what makes D3's "one app at a time" real; the `pkg != prefs.activePassPackage` guard is what stops the pass from ending the instant the enforcer notices the very app it just permitted. Order matters here: `clearActivePass()` must run before the `return@launch`, because after the return the coroutine is gone.

- [ ] **Step 3: Keep the tamper branch and re-derive the "blocked" wording**

`:250-254` reports tamper when a protected settings surface opens. The condition `if (isTamperSensitive && !isUserBlockedApp)` becomes `if (isTamperSensitive && !isGatedApp)` — same shape, and both variables are in scope from Step 2. `evaluateProtectedSurface` (`:290-340`) is orthogonal to the denylist flip and is not touched; Settings packages are in `SystemExempt.STATIC` (Task 5) so they are never *gated*, which is precisely why the device-admin and app-management guards still have work to do.

- [ ] **Step 4: Update the tamper receiver's package test**

`:133` reads `if (packageName in prefs.blockedPackages)`. Replace with:

```kotlin
                if (packageName !in FocusSurfaceModel.resolvedAllowlist(
                        prefs, systemExempt.packages(), BuildConfig.APPLICATION_ID, System.currentTimeMillis()
                    )) {
                    Log.w(TAG, "Tamper: event detected for a gated package ($action) for $packageName")
                    hyperFocusManager.reportTamper("Gated app package event: $action for $packageName")
                }
```

Uninstalled-and-reinstalled events on a gated package are the signal that someone is trying to reset an app's state mid-session; on an allowed package the same broadcast is noise. This is the one place the receiver's meaning changes, so the log strings change with it.

- [ ] **Step 5: Expire the pass on the heartbeat, not only on the foreground event**

`UnlockTimerService` broadcasts expiry and the receiver at `:101-115` re-checks the foreground app, but if that service dies the pass would linger past its deadline while `isGatedApp` stays false — the allowlist already hides the app from the grid, yet the enforcer would let it run. In the existing polling path (`POLLING_INTERVAL_MS` at `:44`, which drives `checkAndBlockApp` from the foreground check), before the gating decision, add:

```kotlin
                hyperFocusManager.expirePassIfDue(System.currentTimeMillis())
```

Read the surrounding function first and place it once per poll, not once per event — the same call appears in the heartbeat loop and in the event handler, and duplicating it per event costs a DataStore write on every window change.

- [ ] **Step 6: Fix the fallback wording and imports**

`:276-286`'s background-launch fallback (`bringLauncherToFront` under strict kiosk, else `GLOBAL_ACTION_HOME`) is behaviorally correct under the allowlist — landing on Home lands on the black home — so do not change its logic; Task 15 re-points the `bringLauncherToFront` target. Update imports: add `FocusSurfaceModel` and `RewardEngine` stays (still used for `isPassActive`), and delete any now-unused `HyperFocusPreferences` import if Step 2 inlined it.

- [ ] **Step 7: Verify, then state plainly what you did not verify**

```bash
grep -n "blockedPackages\|isUnlockActive" app/src/main/java/com/neuroflow/app/feature/hyperfocus/service/AppBlockingService.kt || echo "DENYLIST GONE FROM ENFORCER"
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh assemble && bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.*"
```

Expected: `DENYLIST GONE FROM ENFORCER`, then green. Note the path in that grep is still the pre-Task-1 location until Phase 1 has actually run; substitute `feature/hyperfocus` if you are executing the phases in order. **Unverifiable on this machine:** that a gated app is bounced, that an IME still opens, that the pass ends on foreground departure. These go to the device test plan.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/feature/hyperfocus/service/AppBlockingService.kt
git commit -q -m "feat: gate by allowlist in AppBlockingService and drop the device-wide unlock

The enforcer now asks FocusSurfaceModel whether a package is allowed instead of whether
it was ticked as blocked, and the blanket unlock exemption is deleted: a pass frees one
app and every other switch costs the gate again. This tightens existing behavior.
Unverified on this machine; HOME-role and bounce behavior need a device." -- <same paths>
git show --name-only HEAD
```

## Task 13: Device-owner suspension suspends the complement of the allowlist

Spec §6.3. `syncHyperFocusBlockedPackagesSuspension` is the strong enforcement path and takes a set to suspend. Under an allowlist the suspended set is "everything launchable that is not allowed", which is a different and much larger set — so this function is re-expressed rather than renamed, and it must enumerate the catalog itself.

**Files:**
- Modify: `app/src/main/java/com/neuroflow/app/kiosk/DeviceOwnerKioskManager.kt` — `syncHyperFocusBlockedPackagesSuspension` (`:285-330`), its tracked-set helpers (`getTrackedHyperFocusSuspendedPackages`), and the two callers in `feature/hyperfocus/domain/HyperFocusManager.kt` (`:62-67` in `deactivate`, `:216-222` in `activateInternal`)
  - **Corrected during Task 11 (see that amendment, defect 2):** this function has **six** callers, not two. Replacing it breaks all of them in this commit, so this task owns every one:
    - `app/src/main/java/com/neuroflow/app/NeuroFlowApplication.kt:110-115` — startup re-sync, owned by **no other task** as originally written
    - `app/src/main/java/com/neuroflow/app/receiver/BootReceiver.kt:83-88`
    - `app/src/main/java/com/neuroflow/app/presentation/launcher/LauncherViewModel.kt:1220` and `:1247`
    All four read `hfPrefs.blockedPackages` and have no `AppCatalogRepository`. Rather than inject the catalog into a `Application`, a `BroadcastReceiver` and a launcher ViewModel that Phase 3 deletes, **compute the complement once inside `HyperFocusManager`** and expose it: `suspend fun resyncSuspension(active: Boolean)` on the interface, which reads prefs, builds `keepSet`/`launchable` with the injections this task already adds, and calls `DeviceOwnerKioskManager.syncAllowlistSuspension`. The four external callers then call `hyperFocusManager.resyncSuspension(hfPrefs.isActive)` (BootReceiver and NeuroFlowApplication already hold a manager reference or can inject it; LauncherViewModel must, since its two sites are the activation and deactivation paths). Task 16's reader list at `:4012` therefore no longer claims `BootReceiver:84` or `LauncherViewModel:1219,:1246`.
- Modify: `feature/hyperfocus/domain/HyperFocusManager.kt` — inject `AppCatalogRepository` and `SystemExempt`, compute the keep-set at both call sites

**Interfaces:**
- Consumes: `FocusSurfaceModel.resolvedAllowlist` (Task 5), `AppCatalogRepository.launchableApps()` (Task 7), `SystemExempt.packages()` (Task 5).
- Produces:
  ```kotlin
  // DeviceOwnerKioskManager
  fun syncAllowlistSuspension(
      context: Context, keepSet: Set<String>, launchable: Set<String>, enabled: Boolean
  ): Boolean
  ```
  and `HyperFocusManagerImpl` gaining `appCatalogRepository` + `systemExempt` constructor parameters.

- [ ] **Step 1: Re-express the kiosk call**

Replace the function at `:285` with:

```kotlin
    /**
     * Suspends every launchable package outside keepSet while Hyper Focus is active
     * (device-owner only). keepSet must already include the system exemptions; this
     * function never widens it.
     */
    fun syncAllowlistSuspension(
        context: Context,
        keepSet: Set<String>,
        launchable: Set<String>,
        enabled: Boolean
    ): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return false
        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            Log.i(TAG, "Hyper Focus package suspension skipped: not device owner")
            return false
        }
        val admin = adminComponent(context)
        val toSuspend = launchable - keepSet - context.packageName
        val trackedSuspended = getTrackedHyperFocusSuspendedPackages(context)
        return try {
            if (enabled) {
                val stale = trackedSuspended - toSuspended
                if (stale.isNotEmpty()) dpm.setPackagesSuspended(admin, stale.toTypedArray(), false)
                if (toSuspended.isNotEmpty()) dpm.setPackagesSuspended(admin, toSuspended.toTypedArray(), true)
                setTrackedHyperFocusSuspendedPackages(context, toSuspended)
            } else {
                if (trackedSuspended.isNotEmpty()) {
                    dpm.setPackagesSuspended(admin, trackedSuspended.toTypedArray(), false)
                }
                setTrackedHyperFocusSuspendedPackages(context, emptySet())
            }
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to sync Hyper Focus allowlist suspension", e)
            false
        }
    }
```

Keep the existing `getTrackedHyperFocusSuspendedPackages` / its setter (read them at `:300` and wherever the tracked set is stored — they persist across calls so a second sync unsuspends what the first suspended). The `launchable - keepSet - context.packageName` expression is the whole point: under the denylist this function suspended a handful of ticked apps; under the allowlist it suspends most of the device, which is why the tracked-set bookkeeping cannot be dropped — an interrupted session that leaves apps suspended is worse than no suspension at all.

- [ ] **Step 2: Compute the keep-set at both manager call sites**

In `HyperFocusManagerImpl.deactivate` (`:61-67`), suspension must be lifted before the preferences are wiped (`:68`), and the launchable set is not needed to lift it:

```kotlin
        runCatching {
            DeviceOwnerKioskManager.syncAllowlistSuspension(
                context,
                keepSet = FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED + systemExempt.packages(),
                launchable = emptySet(),
                enabled = false
            )
        }
```

`enabled = false` unsuspends exactly the tracked set and ignores `keepSet`/`launchable`, so the empty set here is not a lie — but read Step 1 again before changing it, because passing `enabled = true` with an empty launchable set would unsuspend everything.

In `activateInternal` (`:216-222`), replace the blocked-packages call with a computed keep-set:

```kotlin
        runCatching {
            val prefs = hyperFocusDataStore.current()
            val keepSet = FocusSurfaceModel.resolvedAllowlist(
                prefs, systemExempt.packages(), context.packageName, System.currentTimeMillis()
            )
            val launchable = appCatalogRepository.launchableApps().map { it.packageName }.toSet()
            DeviceOwnerKioskManager.syncAllowlistSuspension(
                context, keepSet, launchable,
                DeviceOwnerKioskManager.isStrictKioskEnforcementEnabled(context)
            )
        }
```

`activateInternal` is already `private suspend fun` (`:145`), so the catalog call is legal where it sits. Order matters: `activateInternal` writes the new preferences at `:173-191` **before** this block runs, so `hyperFocusDataStore.current()` here reads the session's allowlist rather than the previous one. If a later refactor moves the suspension call above the `update`, the keep-set goes stale and the device suspends the wrong apps — there is no test that will catch that, so leave a one-line comment at the call site saying the update must precede it.

- [ ] **Step 3: Add the injected dependencies**

`HyperFocusManagerImpl`'s constructor (`:45-50`) gains:

```kotlin
    private val appCatalogRepository: AppCatalogRepository,
    private val systemExempt: SystemExempt,
```

with imports from `feature.hyperfocus.data` and `feature.hyperfocus.surface`. Both are `@Singleton` with `@Inject constructor()`, so no module edit is needed.

- [ ] **Step 4: Fix the tests that mock the manager's collaborators**

`app/src/test/java/com/neuroflow/app/feature/hyperfocus/domain/HyperFocusManagerTest.kt` constructs `HyperFocusManagerImpl` directly (MockK `mockk<…>(relaxed = true)` per the repo convention), so every construction site needs the two new parameters:

```kotlin
    val catalog = mockk<AppCatalogRepository>(relaxed = true)
    val exempt = mockk<SystemExempt>(relaxed = true)
    every { exempt.packages() } returns setOf("android", "com.android.systemui")
    coEvery { catalog.launchableApps() } returns emptyList()
```

Adding them keeps the existing assertions meaningful: with `launchableApps()` returning empty, no test can accidentally depend on device state.

- [ ] **Step 5: Verify**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh assemble && bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManagerTest"
grep -rn "syncHyperFocusBlockedPackagesSuspension" app/src || echo "OLD SUSPENSION API GONE"
```

Expected: green, then `OLD SUSPENSION API GONE`. **This path never executes on the target device** — it is admin-only (Global Constraints), so `setPackagesSuspended` no-ops at the guard in `:290-294` and the accessibility path from Task 12 is the only enforcement. Keep the suite anyway: it proves the keep-set math, and the same complement set is what drives Task 12's bounce decision, so a bug here is a bug in the overlay's target list.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/kiosk/DeviceOwnerKioskManager.kt \
        app/src/main/java/com/neuroflow/app/feature/hyperfocus/domain/HyperFocusManager.kt \
        app/src/test/java/com/neuroflow/app/feature/hyperfocus/domain/HyperFocusManagerTest.kt
git commit -q -m "feat: suspend the complement of the allowlist under device owner

syncHyperFocusBlockedPackagesSuspension took a ticked denylist; syncAllowlistSuspension
takes the keep-set and the launchable set and suspends everything between them. Tracked
set bookkeeping is retained so an interrupted session cannot leave apps suspended." -- <same paths>
git show --name-only HEAD
```

## Task 14: The gate, the gateway, and the emergency control

Spec §5.2, §5.3 and §12's correction. Three screens added to `HyperFocusActivity`'s existing NavHost (`:47-75`), which stays the only surface that talks to the user during a session: `gate` (the cognitive task), `gateway` (code entry + the hidden-app list), and the `launch_app` route changes meaning — it now has to spend a pass before launching.

**Files:**
- Create: `feature/hyperfocus/gate/GateTaskScreen.kt`, `feature/hyperfocus/gate/GatewayScreen.kt`, `feature/hyperfocus/gate/EmergencyControl.kt`
- Modify: `feature/hyperfocus/HyperFocusActivity.kt` (start destination from intent, two new routes, `launch_app` grants before launching)
- Modify: `feature/hyperfocus/HyperFocusViewModel.kt` (gate state, `grantPass`, `hiddenApps`, emergency with a package, `GateRequestDao` writes)
- Modify: `feature/hyperfocus/screens/BlockingOverlayScreen.kt` — its "unlock" affordance becomes "open the gate"

**Interfaces:**
- Consumes: `GateTaskEngine` (Task 6), `AppCatalogRepository` (Task 7), `HyperFocusManager.grantPass` / `triggerEmergencyBypass(packageName)` / `clearActivePass` (Task 8), `GateRequestDao` / `GateRequestEntity` (Task 10), `RewardEngine.isPassActive` / `passSecondsRemaining` (Task 8), `FocusSurfaceModel` (Task 5).
- Produces:
  ```kotlin
  // HyperFocusViewModel additions
  val hiddenApps: StateFlow<List<AppCatalogRepository.AppEntry>>
  val gateBudgetSecondsRemaining: StateFlow<Long?>
  val emergencyAvailable: StateFlow<Boolean>
  fun submitGateAnswer(answer: List<String>, reason: String, mismatchCount: Int, elapsedMillis: Long)
  fun grantPassFor(packageName: String)
  fun grantEmergencyFor(packageName: String)
  // Activity extra
  const val EXTRA_START_DESTINATION = "start_destination"   // "gate" | "gateway" | default overlay
  ```

- [ ] **Step 1: Add the gate state to `HyperFocusViewModel`**

Inject two more collaborators (`:37-42`) — `gateRequestDao: GateRequestDao` and `appCatalogRepository: AppCatalogRepository` — and add:

```kotlin
    val gateTask = MutableStateFlow(GateTaskEngine().newTask())
    private val gateStartedAt = System.currentTimeMillis()

    val hiddenApps: StateFlow<List<AppCatalogRepository.AppEntry>> = combine(
        hyperFocusDataStore.flow,
        tickerFlow
    ) { prefs, _ -> prefs to System.currentTimeMillis() }
        .mapLatest { (prefs, now) ->
            val allowed = FocusSurfaceModel.resolvedAllowlist(
                prefs, systemExempt.packages(), BuildConfig.APPLICATION_ID, now
            )
            appCatalogRepository.launchableApps().filter { it.packageName !in allowed }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gateBudgetSecondsRemaining: StateFlow<Long?> = combine(tickerFlow, hyperFocusDataStore.flow) { _, prefs ->
        val now = System.currentTimeMillis()
        prefs.activePassExpiresAt?.let { deadline ->
            if (deadline > now) ((deadline - now) / 1000) else null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val emergencyAvailable: StateFlow<Boolean> = hyperFocusDataStore.flow
        .map { it.isActive && !it.emergencyUsed && it.sessionMode != HyperFocusSessionMode.TIME_BASED }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
```

`systemExempt` is a third new injected dependency. `gateBudgetSecondsRemaining` reads the deadline even when no package holds it — that is E1's "the budget keeps ticking between passes", and the gateway screen needs it to tell the user how much of an earned window is left.

Then the three functions:

```kotlin
    fun submitGateAnswer(answer: List<String>, reason: String, mismatchCount: Int, elapsedMillis: Long) {
        val engine = GateTaskEngine()
        val task = gateTask.value
        val correct = engine.checkAnswer(task, answer)
        val passed = correct || engine.shouldAutoPass(mismatchCount)
        val willLog = passed && engine.allowsSubmit(elapsedMillis, reason)
        viewModelScope.launch {
            if (willLog) {
                logRequest(task, reason, mismatchCount, elapsedMillis, grantSource = "REWARD_CODE")
            }
            _gatePassed.value = willLog
        }
    }

    fun grantPassFor(packageName: String) {
        viewModelScope.launch {
            val minutes = gateBudgetSecondsRemaining.value?.let { (it / 60).coerceAtLeast(1) }
                ?: RewardTier.MICRO.unlockMinutes
            hyperFocusManager.grantPass(packageName, minutes)
        }
    }

    fun grantEmergencyFor(packageName: String) {
        viewModelScope.launch {
            val before = hyperFocusDataStore.current()
            hyperFocusManager.triggerEmergencyBypass(packageName)
            if (before.emergencyUsed) return@launch
            gateRequestDao.insert(
                GateRequestEntity(
                    sessionId = before.sessionId,
                    packageName = packageName,
                    appLabel = appCatalogRepository.labelFor(packageName),
                    statedReason = EMERGENCY_REASON,
                    taskType = "NONE",
                    mismatchCount = 0,
                    elapsedMillis = 0,
                    grantSource = "EMERGENCY",
                    createdAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun logRequest(
        task: GateTask, reason: String, mismatchCount: Int, elapsedMillis: Long, grantSource: String
    ) {
        val prefs = hyperFocusDataStore.current()
        runCatching {
            gateRequestDao.insert(
                GateRequestEntity(
                    sessionId = prefs.sessionId, packageName = prefs.pendingGatePackage.orEmpty(),
                    appLabel = appCatalogRepository.labelFor(prefs.pendingGatePackage.orEmpty()),
                    statedReason = reason.trim(), taskType = task.type.name,
                    mismatchCount = mismatchCount, elapsedMillis = elapsedMillis,
                    grantSource = grantSource, createdAtMillis = System.currentTimeMillis()
                )
            )
        }
    }
```

Add `val pendingGatePackage: String? = null` to `HyperFocusPreferences` (Task 4 added five fields; this is the sixth, same serialize/deserialize shape as `realHomeComponent`) and `val gatePassed = MutableStateFlow(false)` exposed as `val gatePassed: StateFlow<Boolean> = _gatePassed`. The row records which app the user was reaching for, which is the only reason the log can answer "which app" later — the gate screen is entered from the enforcer with a package, or from the home with a selection, and neither survives the navigation unless it is stored. `runCatching` around the insert is spec §8: "GateRequestEntity write fails → gate still opens. Losing a log row must never cost the user access." Add `private const val EMERGENCY_REASON = "Emergency bypass (no gate)"` to the companion.

- [ ] **Step 2: `GateTaskScreen`**

```kotlin
package com.neuroflow.app.feature.hyperfocus.gate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.neuroflow.app.feature.hyperfocus.HyperFocusViewModel

@Composable
fun GateTaskScreen(
    viewModel: HyperFocusViewModel,
    onPassed: () -> Unit,
    onCancel: () -> Unit
) {
    val task by viewModel.gateTask.collectAsState()
    val passed by viewModel.gatePassed.collectAsState()
    var answers by remember(task) { mutableStateOf(List(task.expected.size) { "" }) }
    var reason by remember { mutableStateOf("") }
    var mismatches by remember(task) { mutableIntStateOf(0) }
    val startedAt = remember(task) { System.currentTimeMillis() }
    var now by remember(task) { mutableLongStateOf(startedAt) }

    LaunchedEffect(task) {
        while (true) {
            now = System.currentTimeMillis()
            delay(500)
        }
    }

    LaunchedEffect(passed) { if (passed) onPassed() }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("What are you reaching for?", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Text(task.prompt, style = MaterialTheme.typography.bodyLarge, color = Color.White)
        task.expected.forEachIndexed { index, _ ->
            OutlinedTextField(
                value = answers.getOrElse(index) { "" },
                onValueChange = { updated -> answers = answers.toMutableList().also { it[index] = updated } },
                singleLine = true,
                label = { Text(if (task.type == GateTaskType.COUNT_BACK) "Value ${index + 1}" else "Answer") }
            )
        }
        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Why do you need this now? (min 8 characters)") },
            minLines = 2
        )
        if (mismatches > 0) {
            Text(
                "Not quite. ${(GateTaskEngine.MAX_MISMATCHES - mismatches).coerceAtLeast(0)} " +
                    "more tries before this one is given to you.",
                color = Color(0xFFE8A33D)
            )
        }
        val elapsed = now - startedAt
        Button(
            onClick = {
                if (viewModel.gateEngine.checkAnswer(task, answers)) {
                    viewModel.submitGateAnswer(answers, reason, mismatches, elapsed)
                } else {
                    mismatches++
                    // auto-pass does not require a correct answer, only the floor and the reason
                    if (mismatches >= GateTaskEngine.MAX_MISMATCHES) {
                        viewModel.submitGateAnswer(answers, reason, mismatches, elapsed)
                    }
                }
            },
            enabled = viewModel.gateEngine.allowsSubmit(elapsedMillis = elapsed, reason = reason)
        ) { Text("Continue") }
        TextButton(onClick = onCancel) { Text("Back to focus", color = Color.Gray) }
        Spacer(modifier = Modifier.height(8.dp))
        Row { Text("${(GateTaskEngine.MIN_ELAPSED_MILLIS - elapsed).coerceAtLeast(0) / 1000 + 1}s", color = Color.Gray) }
    }
}
```

`mismatches` is a **tally, not a lock**. It lives in the screen, is scoped to `remember(task)` so a re-rolled task restarts it, and is read by exactly two things: the `onClick`'s `>= MAX_MISMATCHES` check, which decides when a wrong answer has earned the auto-pass, and `submitGateAnswer`'s `shouldAutoPass`, which decides whether a row is logged. It must never feed the button's `enabled` state and must never be reset to `0` on a correct answer — Task 10 stores `mismatchCount` precisely so "passed on the first try" and "passed on the third" stay distinguishable signals, and an earlier draft of this task did both of those things and thereby made the auto-pass unreachable (Task 6's rulings 2-4).

Add the two imports this draft omits rather than leaving them to guesswork: `androidx.compose.material3.TextButton` and `androidx.compose.runtime.LaunchedEffect`, plus `kotlinx.coroutines.delay`. `viewModel.gateEngine` is a `val gateEngine = GateTaskEngine()` on the ViewModel — expose the same instance the screen reads the task from, so a rotated device does not re-roll the task.

The button's `enabled` state is `allowsSubmit` and nothing else. An earlier draft OR-ed a second copy of the floor and reason test onto it, described as "deliberately redundant"; that copy was load-bearing in the wrong direction — it was the only thing that would reopen the button at three or more mismatches under the count-clause predicate, and once the count leaves `allowsSubmit` alone it is pure duplication that hides a lockout whenever someone deletes one half. One question, one authority. The engine still decides whether a row is logged, in `submitGateAnswer`, and that is why the count-down text is a hint rather than the gate.

- [ ] **Step 3: `GatewayScreen`**

```kotlin
package com.neuroflow.app.feature.hyperfocus.gate

@Composable
fun GatewayScreen(
    viewModel: HyperFocusViewModel,
    onLaunch: (String) -> Unit,
    onBack: () -> Unit
) {
    val hiddenApps by viewModel.hiddenApps.collectAsState()
    val budgetSeconds by viewModel.gateBudgetSecondsRemaining.collectAsState()
    val passActivePackage by viewModel.activePassPackage.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black)
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Gateway", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Text(
            when {
                budgetSeconds != null -> "Budget remaining: ${budgetSeconds / 60}m ${budgetSeconds % 60}s"
                else -> "No budget. Enter a reward code or use the emergency control on the home screen."
            },
            color = Color(0xFF9AA0A6)
        )
        CodeEntrySection(viewModel)
        if (hiddenApps.isEmpty()) {
            Text("Nothing is hidden — every launchable app is on your allowlist.", color = Color(0xFF9AA0A6))
        }
        hiddenApps.forEach { entry ->
            val spendable = budgetSeconds != null || passActivePackage == entry.packageName
            ListItem(
                headlineContent = { Text(entry.label, color = Color.White) },
                supportingContent = { Text(entry.packageName, color = Color(0xFF6B7280)) },
                trailingContent = {
                    Button(
                        onClick = { viewModel.grantPassFor(entry.packageName); onLaunch(entry.packageName) },
                        enabled = spendable
                    ) { Text("Open") }
                }
            )
        }
        TextButton(onClick = onBack) { Text("Cancel", color = Color.Gray) }
    }
}
```

Fill in the imports from Step 2's list plus `androidx.compose.material3.ListItem` and `com.neuroflow.app.feature.hyperfocus.screens.CodeEntrySection`. `CodeEntrySection` is the existing `CodeEntryScreen`'s body lifted into a composable that takes only the ViewModel — `CodeEntryScreen` already renders the code field, the wrong-code lockout countdown and the result snackbar (`HyperFocusViewModel.submitCodeResult`), and the gateway needs exactly that without its own nav route. Extract it rather than duplicating it; leave `CodeEntryScreen` as a thin wrapper so `blocking_overlay`'s existing route keeps working.

`onLaunch` is the activity's `launchApp(packageName)` from `HyperFocusActivity.kt:79`, which is changed in Step 5 to wait for the grant. The `spendable` check is what makes spec §5.3's "visible but inert" list real: the user can see what they are missing, and the reason, without the list being a mystery.

- [ ] **Step 4: `EmergencyControl` — on the home screen, not behind the gate**

```kotlin
package com.neuroflow.app.feature.hyperfocus.gate

@Composable
fun EmergencyControl(
    available: Boolean,
    hiddenApps: List<AppCatalogRepository.AppEntry>,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    TextButton(onClick = { showPicker = true }, enabled = available, modifier = modifier) {
        Text(if (available) "Emergency — 10 min, once" else "Emergency used",
            color = if (available) Color(0xFFE8A33D) else Color(0xFF4B5563))
    }
    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {},
            title = { Text("Emergency access") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("One app, ten minutes, once per session. No gate.")
                    hiddenApps.forEach { entry ->
                        TextButton(onClick = { showPicker = false; onPick(entry.packageName) }) {
                            Text(entry.label)
                        }
                    }
                }
            }
        )
    }
}
```

`hiddenApps` is a required parameter, not a default. An `= emptyList()` default here would compile, render an empty dialog, and silently break the emergency path — which is the one failure this design cannot tolerate because it is the session's only exit, and the only test that could catch it is a Compose UI test, which cannot run in this repo. The caller is `HyperFocusHomeScreen`'s action row (Task 15, Step 8), and the list it passes is `HyperFocusHomeViewModel.hiddenApps`. The picker lists **hidden** apps only: allowed apps are already one tap away on the grid, so an emergency on them would spend the session's only escape for nothing.

- [ ] **Step 5: Wire the routes and make `launch_app` spend a pass**

In `HyperFocusActivity.onCreate` (`:42-76`), replace the hardcoded start destination and add the two routes:

```kotlin
            val startDestination = intent.getStringExtra(EXTRA_START_DESTINATION) ?: "blocking_overlay"
            NavHost(navController = navController, startDestination = startDestination) {
                composable("blocking_overlay") { BlockingOverlayScreen(blockedPackage, viewModel, navController) }
                composable("code_entry") { CodeEntryScreen(viewModel, navController, blockedPackage) }
                composable("planning_prompt") { PlanningPromptScreen(viewModel, onDismiss = { navController.popBackStack() }) }
                composable("gate") {
                    GateTaskScreen(
                        viewModel = viewModel,
                        onPassed = { navController.navigate("gateway") },
                        onCancel = { finish() }
                    )
                }
                composable("gateway") {
                    GatewayScreen(
                        viewModel = viewModel,
                        onLaunch = { pkg -> pendingLaunch = pkg; navController.navigate("launch_app") },
                        onBack = { finish() }
                    )
                }
                composable("launch_app") { /* unchanged transparent box */ }
            }
```

Add `private var pendingLaunch: String? = null` to the activity and change the `launch_app` `LaunchedEffect` (`:70-73`) from `launchApp(blockedPackage)` to `launchApp(pendingLaunch ?: blockedPackage)`, then `pendingLaunch = null`. The route exists to launch whatever the last decision named; keeping that in a field rather than a nav argument is what stops a stale `blocked_package` extra from launching the app the enforcer bounced, immediately after the user chose a different one in the gateway.

Declare the destination vocabulary once, on the activity's companion, and use these names everywhere:

```kotlin
        const val EXTRA_START_DESTINATION = "start_destination"
        const val EXTRA_START_DESTINATION_GATE = "gate"
        const val EXTRA_START_DESTINATION_GATEWAY = "gateway"
        const val EXTRA_START_DESTINATION_SETUP = "setup"
```

`SETUP`'s route is not added until Task 16, so declaring it here leaves one unused constant for two commits — that is cheaper than the alternative, which is Task 15 writing `EXTRA_START_DESTINATION_SETUP` against a constant that does not exist yet and failing to compile. Strings are not types: a destination typed as a literal anywhere is a route that fails at runtime with a blank screen, so the rule is *no string literals for these four values outside this companion*.

Add `putExtra` at the one place that constructs this intent (`AppBlockingService.kt:263-268`) so a bounced app lands on `blocking_overlay` as today, while the home surface's Gate control passes `EXTRA_START_DESTINATION_GATE` (Task 15, Step 9).

- [ ] **Step 6: `BlockingOverlayScreen`'s affordance becomes the gate**

`:78` now collects `isPassActive` (renamed in Task 8) and `:470` branches on it. In the branch that today says the device is unlocked, the copy must say the granted app is open and everything else is not, and the route that used to jump to `code_entry` jumps to `gate` instead. Keep the "open this app" button when `blockedPackage == prefs.activePassPackage` and `isPassActive` — that is the D3 path working as designed: the enforcer bounced the app, the pass exists, so this one app opens.

- [ ] **Step 7: Verify**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh assemble && bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.*"
```

Expected: green. There is no JVM test for these three composables — Compose UI needs instrumentation and Robolectric is inert here — so the behaviors that matter in this task are the ones Task 6 already proved in `GateTaskEngine`, and the screens are checked on device. **Unverifiable here:** that the submit button's enabled state tracks the engine, that the gateway list is inert without budget, and that the emergency picker lists hidden apps.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/feature/hyperfocus
git commit -q -m "feat: add the cognitive gate, the gateway, and the emergency control

Gate charges effort (randomized task, 12s floor, written reason) before the gateway
opens; a code buys minutes and the gateway spends them on exactly one app. Emergency
lives on the home surface instead of behind the gate, per spec 12, because the gate
must not charge for the session's only exit." -- <same paths>
git show --name-only HEAD
```

---

## Task 15: `HyperFocusHomeActivity` — the black home, and the HOME role moves onto it

Four things happen in this task and they must land in one commit: the new surface is built, the `CATEGORY_HOME` filter moves from `LauncherActivity` to it, the persistent-preferred HOME component in `DeviceOwnerKioskManager` follows, and the off-state hand-off to the user's real launcher exists. Split any of them and the device is left in one of two bad states: two HOME-role activities in one package (the system lists the app twice in the launcher chooser and the pinned component decides which one the user actually gets, so the new screen can be built and never open), or zero HOME activities (`HyperFocusManager:212` and `HyperFocusMonitorService:119,124` call `bringLauncherToFront`, which would then resolve to a screen that is no longer the home).

**Files:**
- Create: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/HomeSurfaceState.kt`
- Create: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/RealHomeChoice.kt`
- Create: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/RealHomeResolver.kt`
- Create: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/HyperFocusHomeViewModel.kt`
- Create: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/HyperFocusHomeScreen.kt`
- Create: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/HyperFocusHomeActivity.kt`
- Create: `app/src/test/java/com/neuroflow/app/feature/hyperfocus/home/HomeSurfaceStateTest.kt`
- Create: `app/src/test/java/com/neuroflow/app/feature/hyperfocus/home/RealHomeChoiceTest.kt`
- Modify: `app/src/main/res/values/themes.xml:14` (add a style; delete nothing here)
- Modify: `app/src/main/AndroidManifest.xml:29-33` and `:69-85`
- Modify: `app/src/main/java/com/neuroflow/app/kiosk/DeviceOwnerKioskManager.kt:19,178,221,240`

**Interfaces:**
- Consumes: `FocusSurfaceModel.resolvedAllowlist(prefs, systemExempt, selfPackage, nowMillis): Set<String>` (Task 5), `SystemExempt.packages(): Set<String>` (Task 5), `AppCatalogRepository.launchableApps(): List<AppEntry>` / `launch(packageName): Boolean` / `labelFor(packageName): String` (Task 7), `RewardEngine.isPassActive(prefs, nowMillis)` / `passSecondsRemaining(prefs, nowMillis): Long?` (Task 8), `HyperFocusManager.deactivate()` / `triggerEmergencyBypass(packageName)` / `grantPass(packageName, durationMinutes)` (Task 8), `HyperFocusPreferences.realHomeComponent: String?` (Task 4), `EXTRA_START_DESTINATION` (Task 14).
- Produces: `HomeSurfaceState` (`HandOff` | `Active(essentials, passSecondsRemaining, passPackage)`) and `homeSurfaceStateOf(prefs, labels, systemExempt, selfPackage, nowMillis)`; `renderableEssentials(prefs, systemExempt, selfPackage): Set<String>` — Task 16's activation guard calls this same function, which is what makes "the guard says it is empty" and "the screen would show nothing" the same fact; `HyperFocusHomeViewModel` with `state: StateFlow<HomeSurfaceState>`, `handoffTarget: StateFlow<ComponentName?>`, `hiddenApps: StateFlow<List<AppCatalogRepository.AppEntry>>`, `emergencyAvailable: StateFlow<Boolean>`, `resolveHandoff()`, `refresh()`, `open(pkg)`, `emergencyOpen(pkg)`, `deactivate()`, `openHomeAppSettings()`; `HyperFocusHomeScreen(state, canHandOff, hiddenApps, emergencyAvailable, onOpenEssential, onOpenGateway, onOpenGate, onEmergencyOpen, onDeactivate, onOpenAllowlistEditor, onOpenHomeAppSettings, modifier)`; `HyperFocusHomeActivity` as the package's only `CATEGORY_HOME` activity; `RealHomeChoice.componentKey(ComponentName): String` / `parseComponentKey(String): ComponentName?` / `pickHandoff(candidates, storedKey, selfPackage): ComponentName?`; `DeviceOwnerKioskManager.bringHyperFocusHomeToFront(context)` (renamed from `bringLauncherToFront`).

- [ ] **Step 1: Write the failing test for the surface model**

Create `HomeSurfaceStateTest.kt`:

```kotlin
package com.neuroflow.app.feature.hyperfocus.home

import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val SELF = "com.neuroflow.app"
private val EXEMPT = setOf("com.android.systemui", "com.android.settings")
private const val NOW = 1_000_000L
private val LABELS = mapOf(
    "com.todoist" to "Todoist",
    "com.android.dialer" to "Dialer",
    "com.somelab.frog" to "Frog"
)

class HomeSurfaceStateTest : StringSpec({

    "HFH-1: an inactive session is a hand-off, not an empty black screen" {
        val prefs = HyperFocusPreferences(
            isActive = false,
            essentialPackages = setOf("com.todoist")
        )
        homeSurfaceStateOf(prefs, LABELS, EXEMPT, SELF, NOW) shouldBe HomeSurfaceState.HandOff
    }

    "HFH-2: essentials render as tiles sorted by label" {
        val prefs = HyperFocusPreferences(
            isActive = true,
            essentialPackages = setOf("com.todoist", "com.somelab.frog")
        )
        val state = homeSurfaceStateOf(prefs, LABELS, EXEMPT, SELF, NOW)
            as HomeSurfaceState.Active
        state.essentials.map { it.label } shouldBe listOf("Frog", "Todoist")
    }

    "HFH-3: an exempt package is never a tile even when the user put it in the tier" {
        val prefs = HyperFocusPreferences(
            isActive = true,
            essentialPackages = setOf("com.android.settings", "com.todoist")
        )
        val state = homeSurfaceStateOf(prefs, LABELS, EXEMPT, SELF, NOW)
            as HomeSurfaceState.Active
        state.essentials.map { it.packageName } shouldContainExactly setOf("com.todoist")
    }

    "HFH-4: this app is never a tile" {
        val prefs = HyperFocusPreferences(
            isActive = true,
            essentialPackages = setOf(SELF, "com.todoist")
        )
        renderableEssentials(prefs, EXEMPT, SELF) shouldContainExactly setOf("com.todoist")
    }

    "HFH-5: a missing label falls back to the package name, never blank" {
        val prefs = HyperFocusPreferences(
            isActive = true,
            essentialPackages = setOf("com.unlabelled.app")
        )
        val state = homeSurfaceStateOf(prefs, LABELS, EXEMPT, SELF, NOW)
            as HomeSurfaceState.Active
        state.essentials.single().label shouldBe "com.unlabelled.app"
    }

    "HFH-6: a live pass surfaces its seconds and package; an expired one surfaces neither" {
        val live = HyperFocusPreferences(
            isActive = true,
            essentialPackages = setOf("com.todoist"),
            activePassPackage = "com.instagram.android",
            activePassExpiresAt = NOW + 120_000L
        )
        val state = homeSurfaceStateOf(live, LABELS, EXEMPT, SELF, NOW)
            as HomeSurfaceState.Active
        state.passSecondsRemaining shouldBe 120L
        state.passPackage shouldBe "com.instagram.android"

        val dead = live.copy(activePassExpiresAt = NOW - 1L)
        val expired = homeSurfaceStateOf(dead, LABELS, EXEMPT, SELF, NOW)
            as HomeSurfaceState.Active
        expired.passSecondsRemaining.shouldBeNull()
        expired.passPackage.shouldBeNull()
    }

    "HFH-7: zero essentials is still an active surface" {
        val prefs = HyperFocusPreferences(isActive = true, essentialPackages = emptySet())
        val state = homeSurfaceStateOf(prefs, LABELS, EXEMPT, SELF, NOW)
        (state is HomeSurfaceState.Active) shouldBe true
        (state as HomeSurfaceState.Active).essentials shouldBe emptyList()
    }
})
```

HFH-7 is the one that saves the design from itself. The hand-off in HFH-1 is driven by `isActive`, never by "is there anything to show" — otherwise a user who empties their essentials tier mid-session would be bounced out of a session that is still running, and the enforcer would keep blocking apps from a screen that is no longer on screen. HFH-3 and HFH-4 are the same rule seen from two sides and both are `renderableEssentials`, which is why the guard in Task 16 can trust it.

- [ ] **Step 2: Run it and expect `unresolved reference: homeSurfaceStateOf`**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.home.HomeSurfaceStateTest"
```

- [ ] **Step 3: Implement `HomeSurfaceState.kt`**

```kotlin
package com.neuroflow.app.feature.hyperfocus.home

import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import com.neuroflow.app.feature.hyperfocus.domain.RewardEngine

data class HomeTile(val packageName: String, val label: String)

sealed interface HomeSurfaceState {
    /** Session is off: the shell must give the device back to the user's real launcher. */
    data object HandOff : HomeSurfaceState

    data class Active(
        val essentials: List<HomeTile>,
        val passSecondsRemaining: Long?,
        val passPackage: String?
    ) : HomeSurfaceState
}

/**
 * Exemption means "never gated", not "shown as an icon" (spec 6.4): Settings keeps working
 * while Hyper Focus is on, but it does not live on the home surface.
 */
fun renderableEssentials(
    prefs: HyperFocusPreferences,
    systemExempt: Set<String>,
    selfPackage: String
): Set<String> = prefs.essentialPackages - systemExempt - selfPackage

fun homeSurfaceStateOf(
    prefs: HyperFocusPreferences,
    labels: Map<String, String>,
    systemExempt: Set<String>,
    selfPackage: String,
    nowMillis: Long
): HomeSurfaceState {
    if (!prefs.isActive) return HomeSurfaceState.HandOff
    val seconds = RewardEngine.passSecondsRemaining(prefs, nowMillis)
    return HomeSurfaceState.Active(
        essentials = renderableEssentials(prefs, systemExempt, selfPackage)
            .map { HomeTile(packageName = it, label = labels[it] ?: it) }
            .sortedBy { it.label.lowercase() },
        passSecondsRemaining = seconds,
        passPackage = if (seconds != null) prefs.activePassPackage else null
    )
}
```

`passPackage` is derived from `seconds != null` rather than read straight off prefs so the two fields can never disagree — an `Active` state that names a pass nobody granted would render a countdown for a pass that grants nothing.

- [ ] **Step 4: Write the failing test for the hand-off choice**

Create `RealHomeChoiceTest.kt`:

```kotlin
package com.neuroflow.app.feature.hyperfocus.home

import android.content.ComponentName
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val SELF = "com.neuroflow.app"

class RealHomeChoiceTest : StringSpec({

    "RH-1: round-trips a component through the prefs string" {
        val component = ComponentName("com.google.android.apps.nexuslauncher", "NexusLauncherActivity")
        parseComponentKey(componentKey(component)) shouldBe component
    }

    "RH-2: a malformed stored key is ignored, not crashed on" {
        pickHandoff(
            candidates = listOf(ComponentName("sec.androidlauncher", ".Launch")),
            storedKey = "not-a-component",
            selfPackage = SELF
        ) shouldBe ComponentName("sec.androidlauncher", ".Launch")
    }

    "RH-3: a stored key still installed wins over the first candidate" {
        val current = ComponentName("com.google.android.apps.nexuslauncher", ".Launcher")
        val other = ComponentName("com.sec.android.app.launcher", ".Launcher")
        pickHandoff(listOf(other, current), componentKey(current), SELF) shouldBe current
    }

    "RH-4: this app is never the hand-off target" {
        pickHandoff(
            candidates = listOf(ComponentName(SELF, "com.neuroflow.app.feature.hyperfocus.home.HyperFocusHomeActivity")),
            storedKey = null,
            selfPackage = SELF
        ).shouldBeNull()
    }

    "RH-5: a candidate that matches the stored key but is gone falls through" {
        val uninstalled = ComponentName("com.thirdparty.launcher", ".Main")
        val installed = ComponentName("com.google.android.apps.nexuslauncher", ".Launcher")
        pickHandoff(
            candidates = listOf(installed),
            storedKey = componentKey(uninstalled),
            selfPackage = SELF
        ) shouldBe installed
    }
})
```

RH-4 is the loop-breaker written as a unit test. `pickHandoff` returning our own component would mean `onResume → startActivity(HOME) → onResume`, and because `launchMode="singleTask"` the second intent is delivered to the same live activity instead of failing — an infinite re-delivery that looks like a frozen black screen. RH-2 and RH-5 both exist because `realHomeComponent` is a string in a DataStore blob and the app it names can be uninstalled.

- [ ] **Step 5: Implement `RealHomeChoice.kt` and `RealHomeResolver.kt`**

`RealHomeChoice.kt` — pure, no Android besides `ComponentName`:

```kotlin
package com.neuroflow.app.feature.hyperfocus.home

import android.content.ComponentName

fun componentKey(component: ComponentName): String =
    "${component.packageName}/${component.className}"

fun parseComponentKey(key: String?): ComponentName? {
    val (pkg, cls) = key?.split('/') ?: return null
    if (pkg.isBlank() || cls.isNullOrBlank()) return null
    return ComponentName(pkg, cls)
}

/**
 * Chooses which launcher to hand the device to when Hyper Focus is off. Null means
 * "there is nowhere to hand off to" and the caller must show the off-state placeholder.
 */
fun pickHandoff(
    candidates: List<ComponentName>,
    storedKey: String?,
    selfPackage: String
): ComponentName? {
    val others = candidates.filterNot { it.packageName == selfPackage }
    val stored = parseComponentKey(storedKey)
    return others.firstOrNull { componentKey(it) == componentKey(stored) }
        ?: others.firstOrNull()
}
```

`RealHomeResolver.kt` — the Android shell:

```kotlin
package com.neuroflow.app.feature.hyperfocus.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enumerates the HOME candidates and picks this app out of them. `queryIntentActivities`
 * is not filtered by the current default, which is what makes the real launcher findable
 * even while a persistent-preferred rule (or the user's own default choice) points HOME at us.
 */
@Singleton
class RealHomeResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun candidates(): List<ComponentName> {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return context.packageManager
            .queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { info ->
                info.activityInfo?.let { ComponentName(it.packageName, it.name) }
            }
            .distinctBy { it.packageName }
    }

    fun handoff(storedKey: String?): ComponentName? =
        pickHandoff(candidates(), storedKey, context.packageName)
}
```

- [ ] **Step 6: Add the theme and move the manifest role**

Append to `app/src/main/res/values/themes.xml` (inside `<resources>`, after the existing `Theme.Launcher.Transparent`):

```xml
    <!-- HyperFocus black home: opaque black, no wallpaper, edge-to-edge.
         Parent is the dark Material theme so windowLight* stays false and the
         system-bar buttons remain visible; Theme.Launcher.Transparent sets both
         light flags true for wallpapers, and copying that here would make the
         navigation bar buttons invisible. -->
    <style name="Theme.HyperFocusHome" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@android:color/black</item>
        <item name="android:statusBarColor">@android:color/black</item>
        <item name="android:navigationBarColor">@android:color/black</item>
        <item name="android:windowLightStatusBar">false</item>
        <item name="android:windowLightNavigationBar">false</item>
        <item name="android:windowShowWallpaper">false</item>
        <item name="android:windowDisablePreview">true</item>
    </style>
```

`Theme.Launcher.Transparent` stays in place — Phase 3 deletes it along with the launcher.

In `AndroidManifest.xml`, replace the whole `LauncherActivity` block at `:69-85` with the new activity plus a stripped old one:

```xml
        <!-- HyperFocus black home: the only HOME-role activity in this package -->
        <activity
            android:name=".feature.hyperfocus.home.HyperFocusHomeActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:taskAffinity=""
            android:stateNotNeeded="true"
            android:excludeFromRecents="true"
            android:clearTaskOnLaunch="true"
            android:enableOnBackInvokedCallback="false"
            android:theme="@style/Theme.HyperFocusHome">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>

        <!-- Archived launcher: no HOME filter, kept only until Phase 3 deletes it -->
        <activity
            android:name=".presentation.launcher.LauncherActivity"
            android:exported="false"
            android:launchMode="singleTask"
            android:taskAffinity=""
            android:stateNotNeeded="true"
            android:excludeFromRecents="true"
            android:clearTaskOnLaunch="true"
            android:enableOnBackInvokedCallback="false"
            android:theme="@style/Theme.Launcher.Transparent" />
```

Copy every attribute onto the new activity rather than writing a shorter list. `taskAffinity=""` is the one worth naming: with the default affinity the black home joins the main app's task, so Recents shows one card containing the todo app inside the NeuroFlow stack and "home" stops meaning a separate task. `stateNotNeeded` is what lets the system recreate the home after a kill without the extras it was started with, and `enableOnBackInvokedCallback="false"` keeps a back press on the home from popping the task it just became.

Extend the existing `<queries>` block at `:29-33` (`org.adw.launcher.THEMES` stays; Phase 3 removes it) so HOME resolution works on API 30+ even with `QUERY_ALL_PACKAGES` present — role resolution is filtered by visibility, not by the broad permission:

```xml
    <queries>
        <intent>
            <action android:name="org.adw.launcher.THEMES" />
        </intent>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.HOME" />
        </intent>
    </queries>
```

- [ ] **Step 7: Write the ViewModel**

```kotlin
package com.neuroflow.app.feature.hyperfocus.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.domain.model.HyperFocusSessionMode
import com.neuroflow.app.feature.hyperfocus.data.AppCatalogRepository
import com.neuroflow.app.feature.hyperfocus.data.HyperFocusDataStore
import com.neuroflow.app.feature.hyperfocus.domain.HyperFocusManager
import com.neuroflow.app.feature.hyperfocus.surface.FocusSurfaceModel
import com.neuroflow.app.feature.hyperfocus.surface.SystemExempt
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class HyperFocusHomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hyperFocusDataStore: HyperFocusDataStore,
    private val hyperFocusManager: HyperFocusManager,
    private val appCatalogRepository: AppCatalogRepository,
    private val systemExempt: SystemExempt,
    private val realHomeResolver: RealHomeResolver
) : ViewModel() {

    private val tick = MutableStateFlow(System.currentTimeMillis())
    private val labels = MutableStateFlow<Map<String, String>>(emptyMap())
    private val entries = MutableStateFlow<List<AppCatalogRepository.AppEntry>>(emptyList())
    private var storedHome: String? = null
    private val _handoffTarget = MutableStateFlow<ComponentName?>(null)
    val handoffTarget: StateFlow<ComponentName?> = _handoffTarget.asStateFlow()

    val state: StateFlow<HomeSurfaceState> =
        combine(hyperFocusDataStore.flow, tick, labels) { prefs, now, known ->
            homeSurfaceStateOf(
                prefs = prefs,
                labels = known,
                // STATIC, not packages(): the grid must hide never-gated machinery, but the
                // dialers are in packages() and must stay visible tiles (spec 6.2 + 6.4).
                systemExempt = SystemExempt.STATIC,
                selfPackage = context.packageName,
                nowMillis = now
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeSurfaceState.HandOff)

    init {
        viewModelScope.launch {
            while (isActive) {
                tick.value = System.currentTimeMillis()
                delay(1_000L)
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val all = appCatalogRepository.launchableApps()
            entries.value = all
            labels.value = all.associate { it.packageName to it.label }
        }
    }

    /** Hidden = launchable and not in the resolved allowlist: what the gate and the emergency both spend. */
    val hiddenApps: StateFlow<List<AppCatalogRepository.AppEntry>> =
        combine(hyperFocusDataStore.flow, entries) { prefs, all ->
            if (!prefs.isActive) emptyList()
            else {
                val keep = FocusSurfaceModel.resolvedAllowlist(
                    prefs, systemExempt.packages(), context.packageName, System.currentTimeMillis()
                )
                all.filter { it.packageName !in keep }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val emergencyAvailable: StateFlow<Boolean> = hyperFocusDataStore.flow
        .map { it.isActive && !it.emergencyUsed && it.sessionMode != HyperFocusSessionMode.TIME_BASED }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Called from the activity's `onCreate` and `onResume`; cheap and idempotent. */
    fun resolveHandoff() {
        viewModelScope.launch {
            val prefs = hyperFocusDataStore.current()
            storedHome = prefs.realHomeComponent
            val target = realHomeResolver.handoff(storedHome)
            _handoffTarget.value = target
            if (storedHome == null && target != null) {
                hyperFocusDataStore.update { it.copy(realHomeComponent = componentKey(target)) }
            }
        }
    }

    fun open(packageName: String) {
        viewModelScope.launch { appCatalogRepository.launch(packageName) }
    }

    fun emergencyOpen(packageName: String) {
        viewModelScope.launch {
            hyperFocusManager.triggerEmergencyBypass(packageName)
            appCatalogRepository.launch(packageName)
        }
    }

    fun deactivate() {
        viewModelScope.launch { hyperFocusManager.deactivate() }
    }

    fun openHomeAppSettings() {
        runCatching {
            context.startActivity(
                Intent("android.settings.HOME_SETTINGS")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
```

Two things here are deliberate. The 1-second `tick` exists so a pass countdown and an expiring pass move without any event from DataStore — `passSecondsRemaining` is a wall-clock fact and the alternative is a screen that says 0:37 for a minute. And `resolveHandoff` stores the first real launcher it finds *once*: after this app becomes the user's default HOME, `candidates()` can still list the old launcher, but the moment the user uninstalls that launcher the stored key points at nothing, and RH-5's fall-through then returns `null` — so the fallback has to be captured while it is still observable, which is `onCreate`/`onResume` of the home.

Note that `resolveHandoff()` is a `launch`, so `handoffTarget` is filled a beat *after* the call. Step 9 therefore triggers the hand-off from the collected pair rather than from `onResume` — reading `viewModel.handoffTarget.value` synchronously right after `resolveHandoff()` would see `null` on every cold start and leave the user on the placeholder until they pressed Home again. The anti-loop latch is a field on the activity, not a `@Volatile` on the ViewModel companion: a VM outlives its activity, and a latch that survives re-creation would suppress the *second legitimate* hand-off after the next session ends.

- [ ] **Step 8: Write the screen**

```kotlin
package com.neuroflow.app.feature.hyperfocus.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.neuroflow.app.feature.hyperfocus.data.AppCatalogRepository
import com.neuroflow.app.feature.hyperfocus.gate.EmergencyControl

@Composable
fun HyperFocusHomeScreen(
    state: HomeSurfaceState,
    canHandOff: Boolean,
    hiddenApps: List<AppCatalogRepository.AppEntry>,
    emergencyAvailable: Boolean,
    onOpenEssential: (String) -> Unit,
    onOpenGateway: () -> Unit,
    onOpenGate: () -> Unit,
    onEmergencyOpen: (String) -> Unit,
    onDeactivate: () -> Unit,
    onOpenAllowlistEditor: () -> Unit,
    onOpenHomeAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        HomeSurfaceState.HandOff ->
            if (!canHandOff) OffPlaceholder(
                onOpenAllowlistEditor = onOpenAllowlistEditor,
                onOpenHomeAppSettings = onOpenHomeAppSettings
            )
            // When a hand-off target exists the activity is already finishing; painting a
            // black frame here only makes the swap visible.
            else Box(modifier = modifier.fillMaxSize().background(Color.Black))

        is HomeSurfaceState.Active -> ActiveSurface(
            state = state,
            hiddenApps = hiddenApps,
            emergencyAvailable = emergencyAvailable,
            onOpenEssential = onOpenEssential,
            onOpenGateway = onOpenGateway,
            onOpenEmergency = onEmergencyOpen,
            onDeactivate = onDeactivate,
            onOpenGate = onOpenGate,
            modifier = modifier
        )
    }
}
```

`ActiveSurface` is a `LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 84.dp), contentPadding = PaddingValues(24.dp))` — scrolling comes from the grid, which is what the user asked for ("scrolling allowed") without a nested-scroll Column. Its first item is a `GridItemSpan(maxLineSpan)` header row: session state, the pass countdown when `passSecondsRemaining != null`, and the controls. Then one tile per `HomeTile`, then a span-row of actions: `Gate` (`onOpenGate`), `Hidden apps` (`onOpenGateway`), `Emergency` — which is `EmergencyControl(available = emergencyAvailable, hiddenApps = hiddenApps, onPick = onOpenEmergency)` imported from `feature/hyperfocus/gate/` (Task 14), not a hand-rolled dialog — and `End session` (`onDeactivate`). Importing Task 14's control rather than duplicating it is what keeps "emergency is one app, ten minutes, once" written down in exactly one place.

A tile is icon over label:

```kotlin
@Composable
private fun HomeTileView(tile: HomeTile, onClick: () -> Unit) {
    val context = LocalContext.current
    val icon = remember(tile.packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(tile.packageName)
                .toBitmap(96, 96).asImageBitmap()
        }.getOrNull()
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = tile.label,
                contentScale = ContentScale.Fitting,
                modifier = Modifier.size(56.dp)
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(14.dp))
            ) {
                Text(
                    text = tile.label.take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = tile.label,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
```

The monogram branch is not decoration: `getApplicationIcon` throws `NameNotFoundException` for a package uninstalled mid-session, which is exactly spec §8's "App uninstalled mid-pass" row, and a throwing icon lookup inside a grid item crashes the whole home instead of one tile.

`OffPlaceholder` is a centered Column with three lines of copy and two buttons — `Set my home app` (`onOpenHomeAppSettings`) and `Edit allowlist` (`onOpenAllowlistEditor`) — and its text states why it is showing: no other launcher on this device could be found, so Hyper Focus is not pretending to be one. This is spec §8 rows 1-3 rendered as one screen.

- [ ] **Step 9: Write the activity**

```kotlin
package com.neuroflow.app.feature.hyperfocus.home

import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.neuroflow.app.feature.hyperfocus.HyperFocusActivity
import com.neuroflow.app.kiosk.DeviceOwnerKioskManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicBoolean

@AndroidEntryPoint
class HyperFocusHomeActivity : ComponentActivity() {

    // A field, not a call inside onResume: hiltViewModel() needs the activity's
    // LocalViewModelStoreOwner and has none when invoked from a lifecycle callback body.
    private val viewModel: HyperFocusHomeViewModel by viewModels()
    private val handoffGuard = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.resolveHandoff()
        setContent {
            val state by viewModel.state.collectAsState()
            val target by viewModel.handoffTarget.collectAsState()
            val hiddenApps by viewModel.hiddenApps.collectAsState()
            val emergencyAvailable by viewModel.emergencyAvailable.collectAsState()
            // The hand-off is driven off the collected pair, not off onResume: resolveHandoff()
            // is a coroutine, so on a cold start the target arrives after onResume has finished.
            LaunchedEffect(state, target) {
                if (state is HomeSurfaceState.HandOff && target != null) handOffIfAllowed()
            }
            HyperFocusHomeScreen(
                state = state,
                canHandOff = target != null,
                hiddenApps = hiddenApps,
                emergencyAvailable = emergencyAvailable,
                onOpenEssential = viewModel::open,
                onOpenGateway = { openHyperFocus(HyperFocusActivity.EXTRA_START_DESTINATION_GATEWAY) },
                onOpenGate = { openHyperFocus(HyperFocusActivity.EXTRA_START_DESTINATION_GATE) },
                onEmergencyOpen = viewModel::emergencyOpen,
                onDeactivate = viewModel::deactivate,
                onOpenAllowlistEditor = { openHyperFocus(HyperFocusActivity.EXTRA_START_DESTINATION_SETUP) },
                onOpenHomeAppSettings = viewModel::openHomeAppSettings
            )
        }
    }

    override fun onResume() {
        super.onResume()
        DeviceOwnerKioskManager.syncLockTaskMode(this)
        handoffGuard.set(false)
        viewModel.resolveHandoff()
        viewModel.refresh()
    }

    override fun onPause() {
        super.onPause()
        handoffGuard.set(false)
    }

    private fun openHyperFocus(startDestination: String) {
        startActivity(
            Intent(this, HyperFocusActivity::class.java)
                .putExtra(HyperFocusActivity.EXTRA_START_DESTINATION, startDestination)
        )
    }

    private fun handOffIfAllowed() {
        val target = viewModel.handoffTarget.value ?: return
        if (target.packageName == packageName) return
        if (!handoffGuard.compareAndSet(false, true)) return
        runCatching {
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .setComponent(target)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }.onFailure {
            handoffGuard.set(false)
            Log.w(TAG, "Hand-off to ${componentKey(target)} failed", it)
        }
    }

    companion object {
        private const val TAG = "HyperFocusHome"
    }
}
```

Three notes the implementer must not lose. **`setComponent` is load-bearing.** A bare `ACTION_MAIN`+`CATEGORY_HOME` intent re-resolves through `addPersistentPreferredActivity` (`DeviceOwnerKioskManager.kt:175-179`), which pins *our* component, so the hand-off would deliver HOME back to this activity and loop; the explicit component is the only way out, and it is why `RealHomeResolver` needs the `<queries>` entry from Step 6. On an admin-only device that pin is never installed and a bare HOME intent would resolve to the user's real launcher — but do not "simplify" to the bare intent: `setComponent` is the only form that behaves identically under both provisioning states, and the resolver must not care which one the device is in. **The latch is per-visit**, reset in both `onPause` and `onResume`: `FLAG_ACTIVITY_CLEAR_TASK` plus `launchMode="singleTask"` means the system can re-deliver `onResume` once before the other launcher's window appears, and an unguarded second `startActivity` is what turns a slow hand-off into a visible fight between two homes. **No `runBlocking` anywhere near a HOME activity.** The first draft of this step read preferences synchronously from `onCreate` to decide the hand-off; on a cold boot that blocks the main thread on DataStore's single dispatcher, and the screen the user cannot escape by pressing Home again is the one that ANRs. `resolveHandoff()` is a `viewModelScope.launch` and the hand-off fires from the `LaunchedEffect` that watches the collected pair.

- [ ] **Step 10: Re-point the kiosk manager**

In `DeviceOwnerKioskManager.kt`:
- `:19` — replace `import com.neuroflow.app.presentation.launcher.LauncherActivity` with `import com.neuroflow.app.feature.hyperfocus.home.HyperFocusHomeActivity`. Leave the `LauncherKeepAliveService`/`LauncherKeepAliveScheduler` imports at `:20-21` alone; that keep-alive is a foreground-notification process guard whose tap-target is `MainActivity`, it never launches a home screen, and Phase 3 deletes it.
- `:178` — `ComponentName(context, LauncherActivity::class.java)` → `ComponentName(context, HyperFocusHomeActivity::class.java)`. This is the persistent preferred rule; if it keeps naming the archived activity, the device-owner path opens the *old* launcher and the black home never appears on an enrolled device. On the target device it is never reached at all: `applyDeviceOwnerPolicies` short-circuits at the `isDeviceOwnerApp` guard in `:134`, which also drops `setLockTaskPackages`, `setStatusBarDisabled` and the `DISALLOW_SAFE_BOOT`/`FACTORY_RESET`/`ADD_USER` restrictions — none of those have ever applied here. Repoint the line anyway: it must name a class that still exists after Phase 3, and an enrolled device should get the correct home.
- `:221` and `:240` — same class swap in `launchKioskHome` and `bringLauncherToFront`.
- Rename `bringLauncherToFront` → `bringHyperFocusHomeToFront` and update its four callers, none of which is the launcher:

```bash
grep -rln "bringLauncherToFront" app/src/main/java | xargs sed -i 's/bringLauncherToFront/bringHyperFocusHomeToFront/g'
```

- `isAppDefaultLauncher` (`:374-383`) needs no change: the API 29+ branch asks `RoleManager.isRoleHeld(ROLE_HOME)` and the older branch compares `resolveActivity(HOME).activityInfo.packageName`, both of which are package-level facts that survive moving the filter between activities.

- [ ] **Step 11: Verify**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh assemble && \
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.home.*" && \
grep -o 'tests="[0-9]*"' \
  app/build/test-results/testDebugUnitTest/TEST-com.neuroflow.app.feature.hyperfocus.home.HomeSurfaceStateTest.xml \
  app/build/test-results/testDebugUnitTest/TEST-com.neuroflow.app.feature.hyperfocus.home.RealHomeChoiceTest.xml
```

Expected: `tests="7"` and `tests="5"`, build green. Then confirm exactly one HOME filter exists in the manifest:

```bash
grep -c "category.HOME" app/src/main/AndroidManifest.xml
```

Expected: `1`. **Unverifiable on this machine** (no emulator, no device — see the device test plan): that the black home actually becomes the home screen, that the hand-off lands on the user's previous launcher, that the navigation-bar buttons stay visible on the black background, and that the icons render. Do not mark this task as device-verified.

- [ ] **Step 12: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/feature/hyperfocus/home \
        app/src/test/java/com/neuroflow/app/feature/hyperfocus/home \
        app/src/main/AndroidManifest.xml \
        app/src/main/res/values/themes.xml \
        app/src/main/java/com/neuroflow/app/kiosk/DeviceOwnerKioskManager.kt \
        app/src/main/java/com/neuroflow/app/feature/hyperfocus/domain/HyperFocusManager.kt \
        app/src/main/java/com/neuroflow/app/feature/hyperfocus/service/HyperFocusMonitorService.kt \
        app/src/main/java/com/neuroflow/app/feature/hyperfocus/service/AppBlockingService.kt
git commit -q -m "feat: add the HyperFocus black home and move the HOME role to it

One HOME activity in the package, owning the role the archived launcher held.
Off state hands the device back to the previously-resolved launcher by explicit
component, because the persistent-preferred rule pins HOME at us and a bare
HOME intent would loop. bringLauncherToFront becomes what it now actually does." -- \
        app/src/main/java/com/neuroflow/app/feature/hyperfocus/home \
        app/src/test/java/com/neuroflow/app/feature/hyperfocus/home \
        app/src/main/AndroidManifest.xml \
        app/src/main/res/values/themes.xml \
        app/src/main/java/com/neuroflow/app/kiosk/DeviceOwnerKioskManager.kt \
        app/src/main/java/com/neuroflow/app/feature/hyperfocus/domain/HyperFocusManager.kt \
        app/src/main/java/com/neuroflow/app/feature/hyperfocus/service/HyperFocusMonitorService.kt \
        app/src/main/java/com/neuroflow/app/feature/hyperfocus/service/AppBlockingService.kt
git show --name-only HEAD
```

Check `git show --name-only HEAD` against the file list above before moving on: the `sed` in Step 10 can reach files outside this pathspec if a caller was missed, and a partial commit here leaves the tree between two HOME filters.

---

## Task 16: The allowlist editor, activation without a denylist, and the navigation swap

This is the task that deletes `HyperFocusPreferences.blockedPackages`. Task 11 deliberately left the field in place because `AppBlockingService` and `LauncherViewModel` still read it; every reader is now accounted for — `AppBlockingService:133,:246` (Task 12), `AppIcon.kt:116` (Task 8), the manager and its suspension calls (Task 13), and the five sites below (`AppIcon.kt:115`, `BootReceiver:84`, `LauncherViewModel:1219,:1246`, `HyperFocusActivationSheet:359-364`, `LauncherSettings:935-948`). Delete the field in the same commit that deletes its last reader, or the build goes red with no owner.

> **Corrected during Task 11.** Two fixes to that list. (a) The three suspension call sites — `BootReceiver:84`, `LauncherViewModel:1219,:1246` and the `NeuroFlowApplication:110-115` this list omitted entirely — die in **Task 13**, not here, because Task 13 replaces the function they call and cannot leave them reading `blockedPackages`; see Task 13's corrected Files. What remains owned here is the field and its serialize/deserialize (`:4252`), `AppIcon.kt:115`, `LauncherSettings:935-948`, and the activation path. (b) The line numbers for the last two have drifted since the plan was written: `HyperFocusActivationSheet` is at `feature/hyperfocus/screens/` and its picker is `:392` ("Apps to block"), not `:359-364`; `HyperFocusViewModel`'s activate/activateTimed pair is `:158-177`, not `:157-176`. Locate both by content, not by number.

**Files:**
- Create: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/surface/AllowlistEditor.kt`
- Create: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/domain/ActivationGuard.kt`
- Create: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/AllowlistSettingsScreen.kt`
- Create: `app/src/test/java/com/neuroflow/app/feature/hyperfocus/surface/AllowlistEditorTest.kt`
- Create: `app/src/test/java/com/neuroflow/app/feature/hyperfocus/domain/ActivationGuardTest.kt`
- Modify: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/data/HyperFocusDataStore.kt:26,109-110,135-147`
- Modify: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/domain/HyperFocusManager.kt:31,32,84-97,145-178`
- Modify: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/HyperFocusViewModel.kt:157-183`
- Modify: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/screens/HyperFocusActivationSheet.kt:55-58,113,154,355-370,410-450`
- Modify: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/HyperFocusActivity.kt` (companion constants + `setup` route)
- Modify: `app/src/main/java/com/neuroflow/app/feature/hyperfocus/home/HyperFocusHomeActivity.kt` (literals → constants)
- Modify: `app/src/main/java/com/neuroflow/app/presentation/launcher/LauncherViewModel.kt:1219-1224,1246-1251`
- Modify: `app/src/main/java/com/neuroflow/app/presentation/launcher/components/AppIcon.kt:115`
- Modify: `app/src/main/java/com/neuroflow/app/presentation/launcher/settings/LauncherSettings.kt:276,935-948`
- Modify: `app/src/main/java/com/neuroflow/app/receiver/BootReceiver.kt:83-87`
- Modify: `app/src/main/java/com/neuroflow/app/presentation/common/NeuroFlowApp.kt:93,463-468,565-577`
- Modify: `app/src/main/java/com/neuroflow/app/presentation/common/MoreMenuSheet.kt:111-118`

**Interfaces:**
- Consumes: `renderableEssentials(prefs, systemExempt, selfPackage)` (Task 15), `FocusSurfaceModel.resolvedAllowlist` (Task 5), `SystemExempt.packages()` (Task 5), `AppCatalogRepository.launchableApps()/launch()` (Task 7), `HyperFocusPreferences.distractionScores` (Task 11), `HyperFocusPreferences.essentialPackages` / `allowedPackages` (Task 4), `DeviceOwnerKioskManager.syncAllowlistSuspension(context, keepSet, launchable, enabled)` (Task 13), `HyperFocusManager.triggerEmergencyBypass(packageName)` (Task 8), `HomeSurfaceState` + `HomeTile` (Task 15).
- Produces: `AllowlistTier { Essential, Allowed }`, `AllowlistDraft(essential, allowed)`, `draftFrom(prefs)`, `toggleTier(draft, packageName, tier)`, `applyDraft(prefs, draft)`, `editableApps(all, systemExempt, selfPackage)`; `ActivationDecision` + `decideActivation(prefs, systemExempt, selfPackage)`; `HyperFocusManager.activate(dailyTaskTarget, lockedTaskIds)` / `activateTimed(durationMinutes)` (denylist argument gone); `AllowlistSettingsScreen(viewModel, onNavigateBack, onStartSession, modifier)`; `Screen.HyperFocusAllowlist` route `"hyperfocus_allowlist"`, and the `"setup"` destination in `HyperFocusActivity`'s nav graph (the `EXTRA_START_DESTINATION_*` constants are Task 14's); `HyperFocusViewModel.saveAllowlist(essential, allowed)`, `loadEditableApps(): List<AppCatalogRepository.AppEntry>`, `selfPackage: String` and `activationMessage: StateFlow<String?>`.

- [ ] **Step 1: Write the failing editor test**

Create `AllowlistEditorTest.kt`:

```kotlin
package com.neuroflow.app.feature.hyperfocus.surface

import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private const val SELF = "com.neuroflow.app"
private val EXEMPT = setOf("com.android.systemui", "com.android.settings")

class AllowlistEditorTest : FunSpec({

    test("AE-1: promoting an allowed app to essential moves it instead of duplicating it") {
        val draft = AllowlistDraft(essential = emptySet(), allowed = setOf("com.todoist"))
        val next = toggleTier(draft, "com.todoist", AllowlistTier.Essential)
        next.essential shouldContainExactly setOf("com.todoist")
        next.allowed shouldBe emptySet()
    }

    test("AE-2: tapping the tier a package is already in removes it") {
        val draft = AllowlistDraft(essential = setOf("com.todoist"), allowed = emptySet())
        val next = toggleTier(draft, "com.todoist", AllowlistTier.Essential)
        next.essential shouldBe emptySet()
        next.allowed shouldBe emptySet()
    }

    test("AE-3: no toggle sequence can leave a package in both tiers") {
        var draft = AllowlistDraft(emptySet(), emptySet())
        val pkgs = listOf("a", "b", "c")
        for (i in 0 until 24) {
            draft = toggleTier(draft, pkgs[i % 3], if (i % 2 == 0) AllowlistTier.Essential else AllowlistTier.Allowed)
            (draft.essential intersect draft.allowed) shouldBe emptySet()
        }
    }

    test("AE-4: draftFrom and applyDraft round-trip through preferences") {
        val prefs = HyperFocusPreferences(
            essentialPackages = setOf("com.android.dialer"),
            allowedPackages = setOf("com.todoist"),
        )
        applyDraft(prefs, draftFrom(prefs)) shouldBe prefs
    }

    test("AE-5: applying a draft rewrites only the two tiers") {
        val prefs = HyperFocusPreferences(
            isActive = true,
            sessionId = "session-1",
            dailyTaskTarget = 4,
            essentialPackages = emptySet(),
            allowedPackages = emptySet()
        )
        val saved = applyDraft(prefs, AllowlistDraft(setOf("com.todoist"), setOf("com.somelab.frog")))
        saved.isActive shouldBe true
        saved.sessionId shouldBe "session-1"
        saved.dailyTaskTarget shouldBe 4
        saved.essentialPackages shouldContainExactly setOf("com.todoist")
        saved.allowedPackages shouldContainExactly setOf("com.somelab.frog")
    }

    test("AE-6: exempt packages and this app are not offered for the tiers") {
        editableApps(
            all = setOf("com.todoist", "com.android.settings", "com.android.systemui", SELF),
            systemExempt = EXEMPT,
            selfPackage = SELF
        ) shouldContainExactly setOf("com.todoist")
    }
})
```

AE-3 is a loop rather than three examples because mutual exclusion is the kind of thing an implementation gets right for one direction and wrong for the other: `Essential → Allowed` and `Allowed → Essential` are two separate branches, and a fixed sequence only exercises whichever one was written last. AE-5 is the one that prevents real damage — the editor writes into the *live* preferences object, and a `copy()` that forgets a field would silently end the session the user is in while they were rearranging icons.

This suite is `FunSpec` rather than `StringSpec` because AE-3 needs a `for` loop over the draft; both are kotest and both run under the repo's JUnit Platform config.

- [ ] **Step 2: Run it and expect `unresolved reference: AllowlistDraft`**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.surface.AllowlistEditorTest"
```

- [ ] **Step 3: Implement `AllowlistEditor.kt`**

```kotlin
package com.neuroflow.app.feature.hyperfocus.surface

import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences

enum class AllowlistTier { Essential, Allowed }

data class AllowlistDraft(val essential: Set<String>, val allowed: Set<String>)

fun draftFrom(prefs: HyperFocusPreferences): AllowlistDraft =
    AllowlistDraft(essential = prefs.essentialPackages, allowed = prefs.allowedPackages)

fun applyDraft(prefs: HyperFocusPreferences, draft: AllowlistDraft): HyperFocusPreferences =
    prefs.copy(essentialPackages = draft.essential, allowedPackages = draft.allowed)

/**
 * The two tiers are exclusive: a package is either always on the home surface or
 * only reachable through the gateway. Both at once would make the gateway's list
 * disagree with the home's grid about the same app.
 */
fun toggleTier(draft: AllowlistDraft, packageName: String, tier: AllowlistTier): AllowlistDraft =
    when (tier) {
        AllowlistTier.Essential ->
            if (packageName in draft.essential) draft.copy(essential = draft.essential - packageName)
            else draft.copy(essential = draft.essential + packageName, allowed = draft.allowed - packageName)
        AllowlistTier.Allowed ->
            if (packageName in draft.allowed) draft.copy(allowed = draft.allowed - packageName)
            else draft.copy(allowed = draft.allowed + packageName, essential = draft.essential - packageName)
    }

fun editableApps(all: Set<String>, systemExempt: Set<String>, selfPackage: String): Set<String> =
    all - systemExempt - selfPackage
```

- [ ] **Step 4: Write the failing activation-guard test**

Create `ActivationGuardTest.kt`:

```kotlin
package com.neuroflow.app.feature.hyperfocus.domain

import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private const val SELF = "com.neuroflow.app"
private val EXEMPT = setOf("com.android.settings", "com.android.systemui")

class ActivationGuardTest : StringSpec({

    "AG-1: a surface with one real tile activates" {
        val prefs = HyperFocusPreferences(essentialPackages = setOf("com.todoist"))
        decideActivation(prefs, EXEMPT, SELF) shouldBe ActivationDecision.Allowed
    }

    "AG-2: an essentials tier made only of exempt packages cannot activate" {
        val prefs = HyperFocusPreferences(
            essentialPackages = setOf("com.android.settings", "com.android.systemui", SELF)
        )
        decideActivation(prefs, EXEMPT, SELF) shouldBe
            ActivationDecision.Rejected(ActivationRejection.EmptySurface)
    }

    "AG-3: an empty essentials tier cannot activate even when the allowed tier is full" {
        val prefs = HyperFocusPreferences(
            essentialPackages = emptySet(),
            allowedPackages = setOf("com.todoist", "com.somelab.frog")
        )
        decideActivation(prefs, EXEMPT, SELF) shouldBe
            ActivationDecision.Rejected(ActivationRejection.EmptySurface)
    }

    "AG-4: activation while a session is already running is refused" {
        val prefs = HyperFocusPreferences(isActive = true, essentialPackages = setOf("com.todoist"))
        decideActivation(prefs, EXEMPT, SELF) shouldBe
            ActivationDecision.Rejected(ActivationRejection.AlreadyActive)
    }

    "AG-5: the guard and the renderer agree about what counts as empty" {
        val cases = listOf(
            HyperFocusPreferences(essentialPackages = setOf("com.todoist")),
            HyperFocusPreferences(essentialPackages = EXEMPT),
            HyperFocusPreferences(essentialPackages = emptySet(), allowedPackages = setOf("com.todoist")),
            HyperFocusPreferences(essentialPackages = setOf(SELF))
        )
        for (prefs in cases) {
            val allowed = decideActivation(prefs.copy(isActive = false), EXEMPT, SELF) == ActivationDecision.Allowed
            val hasTiles = com.neuroflow.app.feature.hyperfocus.home.renderableEssentials(prefs, EXEMPT, SELF).isNotEmpty()
            allowed shouldBe hasTiles
        }
    }
})
```

AG-2 and AG-3 are the two halves of spec §8's "Allowlist empty at activation" row, and AG-3 is the case the spec does not spell out: an allowed-only surface is technically non-empty and would still start a session whose home screen shows zero icons, with every app behind the gate. That is a legitimate design, but it is not what §5.2 promises the user at activation, so the guard refuses it and the editor tells them to pin at least one thing. AG-5 is the test that keeps the two functions honest as they drift apart in later edits — if `renderableEssentials` gains a rule (hide paused work-profile apps, say) and `decideActivation` does not, the guard starts permitting sessions that render an empty home, and only this loop notices.

- [ ] **Step 5: Implement `ActivationGuard.kt`**

```kotlin
package com.neuroflow.app.feature.hyperfocus.domain

import com.neuroflow.app.feature.hyperfocus.data.HyperFocusPreferences
import com.neuroflow.app.feature.hyperfocus.home.renderableEssentials

enum class ActivationRejection { AlreadyActive, EmptySurface }

sealed interface ActivationDecision {
    data object Allowed : ActivationDecision
    data class Rejected(val reason: ActivationRejection) : ActivationDecision
}

fun decideActivation(
    prefs: HyperFocusPreferences,
    systemExempt: Set<String>,
    selfPackage: String
): ActivationDecision = when {
    prefs.isActive -> ActivationDecision.Rejected(ActivationRejection.AlreadyActive)
    renderableEssentials(prefs, systemExempt, selfPackage).isEmpty() ->
        ActivationDecision.Rejected(ActivationRejection.EmptySurface)
    else -> ActivationDecision.Allowed
}
```

- [ ] **Step 6: Run both new suites, then delete the denylist from the preferences**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.surface.AllowlistEditorTest" && bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.domain.ActivationGuardTest"
```

Expected: `tests="6"` and `tests="5"`. Now in `HyperFocusDataStore.kt`: delete `val blockedPackages: Set<String> = emptySet()` (`:26`), the two serialize lines (`:109-110`), and the deserialize block that builds the set and passes it to the constructor (`:135-147`). An old blob carrying `blockedPackages` is now ignored on read, which is safe because Task 11's `AllowlistMigratorRunner` has already derived the two tiers from it on first launch after the upgrade.

`HyperFocusSessionEntity.blockedPackages` and the `hyperfocus_sessions` column at `NeuroFlowDatabase.kt:124` **stay**. Nothing in the app writes that table — grep shows only `FirestoreBackupRepository:730,:739` mapping it for cloud restore, so it is a backup-only shape, and dropping a column from it would need a migration that rewrites archived session rows for no behavioral gain. A future local session log should record the allowlist snapshot instead.

- [ ] **Step 7: Change the activation API**

In `HyperFocusManager.kt`:
- `:31-32` — `suspend fun activate(dailyTaskTarget: Int, lockedTaskIds: Set<String>)` and `suspend fun activateTimed(durationMinutes: Int)`.
- `:84-100` — the two overrides forward to `activateInternal` without the package argument.
- `:145-178` — `activateInternal(dailyTaskTarget: Int, lockedTaskIds: Set<String>, sessionMode: HyperFocusSessionMode, sessionDurationMinutes: Int?)`; delete the `Log.d(TAG, "Blocked packages (${blockedPackages.size})…")` line at `:161` and replace it with the allowlist it is now built on:

```kotlin
        val keep = FocusSurfaceModel.resolvedAllowlist(
            prefs = HyperFocusPreferences(essentialPackages = essentialPackages, allowedPackages = allowedPackages),
            systemExempt = systemExempt.packages(),
            selfPackage = context.packageName,
            nowMillis = now
        )
        Log.d(TAG, "Allowlist keeps $keep; everything else is gated")
```

Read the tiers from `hyperFocusDataStore.current()` at the top of `activateInternal` rather than adding two more parameters — the editor has already written them, and activation reads whatever is saved.
- In the `HyperFocusPreferences(` construction at `:170-186`: delete `blockedPackages = blockedPackages`, delete `activeUnlockExpiresAt = null` (Task 8 removed the field), and add `essentialPackages = essentialPackages, allowedPackages = allowedPackages` so the snapshot is explicit about what it is preserving. **The rest of the field list must stay** — `activateInternal` builds a fresh `HyperFocusPreferences` rather than copying, so any field it forgets resets to its default, and after Task 4 that includes `realHomeComponent`: an activation that dropped it would leave the black home with no hand-off target and the user with no way back to their launcher when the session ends.
- `:62-68` (`deactivate`) and `:217-222` (activation) were rewritten in Task 13 to `syncAllowlistSuspension`; confirm they no longer reference the deleted property.

- [ ] **Step 8: Repoint the three non-HyperFocus readers**

`BootReceiver.kt:83-87` — inject `AppCatalogRepository` and `SystemExempt` (the receiver is already `@AndroidEntryPoint` and already runs inside a `CoroutineScope(Dispatchers.IO).launch`), then:

```kotlin
                val now = System.currentTimeMillis()
                val launchable = appCatalogRepository.launchableApps().map { it.packageName }.toSet()
                DeviceOwnerKioskManager.syncAllowlistSuspension(
                    context,
                    FocusSurfaceModel.resolvedAllowlist(hfPrefs, systemExempt.packages(), context.packageName, now),
                    launchable,
                    hfPrefs.isActive && DeviceOwnerKioskManager.isStrictKioskEnforcementEnabled(context)
                )
```

`LauncherViewModel.kt:1219-1224` and `:1246-1251` — delete both `syncHyperFocusBlockedPackagesSuspension(…)` blocks outright. These are the strict-mode and companion-mode toggles in the archived launcher's settings; suspension is re-applied by `HyperFocusManager` on activation/deactivation, by `AppBlockingService` on each foreground check, and by `BootReceiver` on start, so removing the eager re-sync costs a short delay after a kiosk toggle and buys back the archived file's dependency on a live subsystem. `enableHybridProtection(context)` stays on the next line in both functions.

`AppIcon.kt:115` — delete the `appInfo.packageName in prefs.blockedPackages &&` clause from the blocked-overlay condition and let the remaining `isPassActive` check (`:116`, edited in Task 8) decide. This is not a behavior change worth reviewing: Task 15 removed the HOME filter from `LauncherActivity`, so the grid that renders this icon is no longer reachable as home, and the file is deleted in Phase 3.

- [ ] **Step 9: Rewrite the activation sheet as a confirm-only sheet**

`HyperFocusActivationSheet.kt` keeps its mode/duration/task-target controls and loses the app picker:
- `:55-58` — drop nothing from the signature except that `distractionScores` stays (Task 11 moved it onto prefs; read it as `viewModel.hyperFocusPrefs.value.distractionScores` at the call site instead and delete the parameter).
- `:113` (`selectedPackages`) and `:154` (`selectedCount`) — delete.
- `:410-450` — delete the app checkbox list block (the `LazyColumn` of `selectedPackages` rows including the `isChecked` branch at `:423` and the two writers at `:437,:446`).
- `:355-370` — the start button becomes:

```kotlin
                    val essentialCount = viewModel.hyperFocusPrefs.value.essentialPackages.size
                    Text(
                        text = "Blocking everything except $essentialCount essential apps and " +
                            "the ones you allowed. Hidden apps need a pass.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    viewModel.activateTimed(selectedDurationMinutes)
                    // or, in the task-based branch: viewModel.activate(selectedTasks)
```

- The sheet must surface the guard instead of silently returning: `viewModel.activationMessage` collected into a `LaunchedEffect` that toasts/hides is fine, but the copy has to say *why* start failed — "Pin at least one app as essential first" is the only sentence that makes the editor findable.

- [ ] **Step 10: Write the editor screen**

Create `AllowlistSettingsScreen.kt`. Its state is a draft, and the draft is only written to prefs on Save:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllowlistSettingsScreen(
    viewModel: HyperFocusViewModel,
    onNavigateBack: () -> Unit,
    onStartSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.hyperFocusPrefs.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf(draftFrom(prefs)) }
    var apps by remember { mutableStateOf<List<AppCatalogRepository.AppEntry>>(emptyList()) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { apps = viewModel.loadEditableApps() }
```

`loadEditableApps()` lives on the ViewModel, not in the screen, because the exempt/self filter needs `SystemExempt` and `context` and a composable must not reach for either:

```kotlin
    suspend fun loadEditableApps(): List<AppCatalogRepository.AppEntry> {
        val all = appCatalogRepository.launchableApps()
        val keep = editableApps(
            all = all.map { it.packageName }.toSet(),
            // STATIC: the editor lists what the user may pin, so machinery and Settings stay off
            // it while the dialers remain offerable (spec 6.4).
            systemExempt = SystemExempt.STATIC,
            selfPackage = context.packageName
        )
        return all.filter { it.packageName in keep }
    }
```

Add `val selfPackage: String get() = context.packageName` to `HyperFocusViewModel` too — Step 11 injects `context`, and the screen's footer text names the app it is excluding.

The body is a `Scaffold(topBar = TopAppBar("HyperFocus", back))` over a `Column` with: a search `TextField` filtering by `label` (`query`), then a `LazyColumn` of app rows, each row = label + package name + a two-segment choice (`Essential` / `Allowed`) calling `toggleTier(draft, pkg, tier)`; a footer card showing the resulting counts — `draft.essential.size` tiles, `draft.allowed.size` behind the gate, and `apps.size - draft.essential.size - draft.allowed.size` never reachable without a pass — then `Save` (`viewModel.saveAllowlist(draft.essential, draft.allowed)`) and `Start session` (`onStartSession`, disabled while `decideActivation(prefs, …)` rejects, with its `reason` rendered as the button's supporting text).

Counts come from the draft, never from prefs, so the preview of what the user is arranging matches what Save will write. The third number is the one the old denylist UI could not show: "what disappears if you press start" was always implicit there and is now a line of text.

- [ ] **Step 11: Add the two ViewModel members**

In `HyperFocusViewModel.kt`:

```kotlin
    private val _activationMessage = MutableStateFlow<String?>(null)
    val activationMessage: StateFlow<String?> = _activationMessage.asStateFlow()

    fun saveAllowlist(essential: Set<String>, allowed: Set<String>) {
        viewModelScope.launch {
            hyperFocusDataStore.update { applyDraft(it, AllowlistDraft(essential, allowed)) }
        }
    }

    fun activate(selectedTaskIds: Set<String>) {
        viewModelScope.launch {
            val prefs = hyperFocusDataStore.current()
            when (val decision = decideActivation(prefs, SystemExempt.STATIC, context.packageName)) {
                is ActivationDecision.Rejected -> _activationMessage.value = when (decision.reason) {
                    ActivationRejection.AlreadyActive -> "A session is already running."
                    ActivationRejection.EmptySurface -> "Pin at least one app as essential first."
                }
                ActivationDecision.Allowed -> {
                    val activeIds = taskRepository.getActiveTasks().map { it.id }.toSet()
                    val normalized = selectedTaskIds.intersect(activeIds)
                    if (normalized.isEmpty()) {
                        _activationMessage.value = "Pick at least one task to lock."
                    } else {
                        hyperFocusManager.activate(
                            dailyTaskTarget = normalized.size,
                            lockedTaskIds = normalized
                        )
                    }
                }
            }
        }
    }

    fun activateTimed(durationMinutes: Int) {
        viewModelScope.launch {
            val prefs = hyperFocusDataStore.current()
            if (decideActivation(prefs, SystemExempt.STATIC, context.packageName) == ActivationDecision.Allowed) {
                hyperFocusManager.activateTimed(durationMinutes)
            } else {
                _activationMessage.value = "Pin at least one app as essential first."
            }
        }
    }
```

Replace `:157-176` wholesale — including both `if (blockedPackages.isEmpty()) return@launch` guards, which are the denylist's version of the same rule and now silently weaker than `decideActivation` (a user could block nothing and still activate). Add three constructor dependencies to `HyperFocusViewModel`: `@ApplicationContext private val context: Context`, `private val systemExempt: SystemExempt`, `private val appCatalogRepository: AppCatalogRepository` — the ViewModel currently injects none of them (its constructor is `hyperFocusManager`, `hyperFocusDataStore`, `unlockCodeRepository`, `taskRepository`), and all three are already bound (`AppModule` provides `LauncherApps`, Task 5 binds `SystemExempt`, Task 7 binds the repository). `activateEmergencyBypass()` at `:180-183` takes a `packageName` argument now (Task 8) and the picker that supplies it is the home surface's, not this ViewModel's — keep the function, add the parameter, and let `HyperFocusHomeViewModel.emergencyOpen` be the caller.

- [ ] **Step 12: Move the navigation**

`NeuroFlowApp.kt`:
- `:93` — delete `data object LauncherSettingsScreen : Screen("launcher_settings", …)`; add `data object HyperFocusAllowlist : Screen("hyperfocus_allowlist", "HyperFocus", null)`.
- `:463-468` — the `composable(Screen.LauncherSettingsScreen.route)` block becomes `composable(Screen.HyperFocusAllowlist.route)` calling `HyperFocusAllowlistRoute(onNavigateBack = { navController.popBackStack() })`. The old route also took `onNavigateToRewards`, and the new one does not: `Screen.Rewards` stays reachable from the More menu exactly as today, and wiring a second exit into the editor would be a nav argument nobody asked for.
- `:565-577` — replace `LauncherSettingsRoute` (the only non-launcher use of `LauncherViewModel`, which is why this file must change here rather than in Phase 3) with:

```kotlin
@Composable
private fun HyperFocusAllowlistRoute(onNavigateBack: () -> Unit) {
    val viewModel: HyperFocusViewModel = hiltViewModel()
    var showActivationSheet by remember { mutableStateOf(false) }

    AllowlistSettingsScreen(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
        onStartSession = { showActivationSheet = true }
    )
    if (showActivationSheet) {
        HyperFocusActivationSheet(
            viewModel = viewModel,
            distractionScores = viewModel.hyperFocusPrefs.value.distractionScores,
            onDismiss = { showActivationSheet = false }
        )
    }
}
```

`AppCatalogRepository` and `SystemExempt` are constructor-injected into `HyperFocusViewModel` (Step 11) and reached through `loadEditableApps()`, never pulled from a second ViewModel in the route — this file is `presentation/common`, and a route that hand-assembles dependencies is how the launcher's DI accreted. Step 9 deleted the `distractionScores` parameter from `HyperFocusActivationSheet`, so drop that argument if the sheet's signature has already changed; the call above is the pre-Step-9 shape that `LauncherSettings.kt:276` uses today, and the same line in `HyperFocusActivity`'s `setup` route needs the same treatment. Keeping the sheet host in the route rather than inside `AllowlistSettingsScreen` is what lets the *same* screen be mounted from `HyperFocusActivity` (Step 13) without the editor owning navigation it does not control.

`MoreMenuSheet.kt:111-118` — `title = "HyperFocus & Launcher"` → `"HyperFocus"`, `subtitle = "App blocking, kiosk mode & reward vaults"` → `"Allowlist, gate & kiosk mode"`, and `onNavigate(Screen.LauncherSettingsScreen.route)` → `onNavigate(Screen.HyperFocusAllowlist.route)`.

`LauncherSettings.kt:935-948` — delete the "Blocked Apps (n)" summary card (four references to the property that no longer exists). No replacement goes in this file; the equivalent information is the footer card in `AllowlistSettingsScreen`, and this file is archived in Phase 3.

- [ ] **Step 13: Give `HyperFocusActivity` the `setup` route and the destination constants**

The four destination constants were declared in Task 14's Step 5 (`EXTRA_START_DESTINATION`, `EXTRA_START_DESTINATION_GATE`, `EXTRA_START_DESTINATION_GATEWAY`, `EXTRA_START_DESTINATION_SETUP`); this step only adds the route that `SETUP` names. If they are not on the companion — Task 14 was executed with the literals — hoist them into the companion now and replace the literals, because three call sites in two activities and one service are about to depend on them.

Read the extra once into a local and use it as `startDestination` (Task 14 added the plumbing; this adds one branch):

```kotlin
            val startDestination = intent.getStringExtra(EXTRA_START_DESTINATION) ?: "blocking_overlay"
            NavHost(navController = navController, startDestination = startDestination) {
                composable("setup") {
                    AllowlistSettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { finish() },
                        onStartSession = { showActivationSheet = true }
                    )
                }
                // gate / gateway / blocking_overlay / code_entry / launch_app unchanged
            }
```

`appCatalogRepository` and `systemExempt` arrive through `HyperFocusViewModel`'s constructor (Step 11), which is also what makes the same `AllowlistSettingsScreen` composable mountable from two different hosts with two different back behaviors — `popBackStack()` in the main app's graph, `finish()` in `HyperFocusActivity`, where the screen is a destination rather than a setting. Add the `showActivationSheet` state and the `if (showActivationSheet) { HyperFocusActivationSheet(...) }` block to this destination the same way Step 12 does for the main-app route. This is why the black home's off-state placeholder and the More-menu entry land on the *same* composable: the editor is reachable from inside a session, and it is reachable when there is no session and no launcher to hand off to — which is precisely the state where it is the only useful thing on the screen.

- [ ] **Step 14: Verify**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh assemble && \
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh testone "com.neuroflow.app.feature.hyperfocus.*" && \
grep -rn "blockedPackages" app/src/main/java | grep -v "HyperFocusSessionEntity\|hyperfocus_sessions\|FirestoreBackupRepository\|NeuroFlowDatabase"
```

Expected: build green, suites green, and the grep returns **only** the session-history column and its backup mapping — the four files the exclusion list names. Anything else still reading the denylist is a reader Step 8 missed. Then:

```bash
grep -rn "launcher_settings\|LauncherSettingsScreen\|LauncherSettingsRoute" app/src/main/java
```

Expected: no hits in `presentation/common`; a hit only inside `presentation/launcher/**` (the archived screen referencing its own test tag at `LauncherSettings.kt:98`), which Phase 3 deletes.

- [ ] **Step 15: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/feature/hyperfocus \
        app/src/test/java/com/neuroflow/app/feature/hyperfocus \
        app/src/main/java/com/neuroflow/app/presentation/common/NeuroFlowApp.kt \
        app/src/main/java/com/neuroflow/app/presentation/common/MoreMenuSheet.kt \
        app/src/main/java/com/neuroflow/app/presentation/launcher/LauncherViewModel.kt \
        app/src/main/java/com/neuroflow/app/presentation/launcher/components/AppIcon.kt \
        app/src/main/java/com/neuroflow/app/presentation/launcher/settings/LauncherSettings.kt \
        app/src/main/java/com/neuroflow/app/receiver/BootReceiver.kt
git commit -q -m "feat: invert HyperFocus to an allowlist and retire the denylist field

Two-tier editor with exclusive membership, an activation guard that agrees with
the home renderer about what counts as empty, and blockedPackages deleted with
its last readers. The launcher's kiosk toggles stop syncing suspension directly;
BootReceiver and HyperFocusManager own that path now. hyperfocus_allowlist
replaces launcher_settings, which was the only live use of LauncherViewModel." -- \
        app/src/main/java/com/neuroflow/app/feature/hyperfocus \
        app/src/test/java/com/neuroflow/app/feature/hyperfocus \
        app/src/main/java/com/neuroflow/app/presentation/common/NeuroFlowApp.kt \
        app/src/main/java/com/neuroflow/app/presentation/common/MoreMenuSheet.kt \
        app/src/main/java/com/neuroflow/app/presentation/launcher/LauncherViewModel.kt \
        app/src/main/java/com/neuroflow/app/presentation/launcher/components/AppIcon.kt \
        app/src/main/java/com/neuroflow/app/presentation/launcher/settings/LauncherSettings.kt \
        app/src/main/java/com/neuroflow/app/receiver/BootReceiver.kt
git show --name-only HEAD
```

---

**Phase 2 exit check.** `bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh assemble && bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh tests` green, and the tree should now be policy-consistent: one `CATEGORY_HOME` activity (`grep -c "category.HOME" app/src/main/AndroidManifest.xml` → `1`), no `blockedPackages` outside the four history/backup files, `RewardEngine.isUnlockActive` gone (`grep -rn "isUnlockActive" app/src/main/java` → no hits), and every new pure component covered by a kotest suite that runs (`app/build/test-results/testDebugUnitTest/` should contain `FocusSurfaceModelTest`, `GateTaskEngineTest`, `PassLifecycleTest`, `AppCatalogFilterTest`, `AllowlistMigratorTest`, `HomeSurfaceStateTest`, `RealHomeChoiceTest`, `AllowlistEditorTest`, `ActivationGuardTest`, `HyperFocusManagerTest`). Also confirm the migration was *not* applied locally — `MIGRATION_20_21` exists in `NeuroFlowDatabase.kt` and `AppModule`'s `addMigrations` list, and Room's schema JSON for version 21 is generated, but nothing on this machine runs it (Task 10's note). `presentation/launcher` should still hold exactly 37 files, all of them compiling and none of them reachable as home.

---

# Phase 3 — Archive the launcher

Two tasks. G1 says the archive is git-only: no `launcher/` copy directory, no `_deprecated` folder, no feature flag. The recovery path is one tag.

## Task 17: Tag, then delete the launcher

**Files:**
- Delete: `app/src/main/java/com/neuroflow/app/presentation/launcher/` (37 files: 36 `.kt` + `folder/README.md`), `app/src/test/java/com/neuroflow/app/presentation/launcher/` (6 files), `app/src/androidTest/java/com/neuroflow/app/presentation/launcher/` (2 files)
- Delete: `app/lint-module-isolation.gradle.kts`, `app/proguard-launcher.pro`, `scripts/check-launcher-isolation.sh`
- Modify: `app/build.gradle.kts:12-13,44-50`
- Modify: `app/src/main/AndroidManifest.xml` (launcher activity, keep-alive receiver + service, five permissions, the icon-pack `<queries>` intent)
- Modify: `app/src/main/res/values/themes.xml:5-19`
- Modify: `app/src/main/java/com/neuroflow/app/NeuroFlowApplication.kt:9,39,95-97`
- Modify: `app/src/main/java/com/neuroflow/app/kiosk/DeviceOwnerKioskManager.kt:20-21,91,96`

**Interfaces:**
- Consumes: everything Tasks 15 and 16 produced — this task is only safe because no live subsystem still names a launcher class.
- Produces: `archive/launcher-2026-10-12` (the tag), and a tree in which `grep -rn "presentation.launcher" app/src` returns nothing.

- [ ] **Step 1: Prove the launcher is unreferenced, then stop and ask the user**

```bash
grep -rn "presentation\.launcher" app/src --include="*.kt" | grep -v "^app/src/main/java/com/neuroflow/app/presentation/launcher/" | grep -v "^app/src/test/java/com/neuroflow/app/presentation/launcher/" | grep -v "^app/src/androidTest/java/com/neuroflow/app/presentation/launcher/"
```

Expected: no output. Any hit is a live dependency on code about to be deleted — read it, fix it in an earlier task, and re-run before continuing. Then check the launcher's own worktree state:

```bash
git status --short -- app/src/main/java/com/neuroflow/app/presentation/launcher \
                      app/src/test/java/com/neuroflow/app/presentation/launcher
```

Expected: empty. **STOP HERE AND ASK THE USER.** Creating the tag and deleting 45 files is the one step in this plan that cannot be undone by the agent: the tag survives, but the user's uncommitted launcher work would not, and a deletion of this size is a decision they made in the abstract ("archive that") on a different day. Present the tag name, the file counts, and the recovery command (`git checkout archive/launcher-2026-10-12 -- app/src/main/java/com/neuroflow/app/presentation/launcher`) and wait for a yes.

- [ ] **Step 2: Create the archive ref**

```bash
git tag -a archive/launcher-2026-10-12 -m "Launcher archived before the HyperFocus black-home redesign (decisions A/B2/D3/E1/F4/G1).
Recovery: git checkout archive/launcher-2026-10-12 -- \
  app/src/main/java/com/neuroflow/app/presentation/launcher \
  app/src/test/java/com/neuroflow/app/presentation/launcher \
  app/src/androidTest/java/com/neuroflow/app/presentation/launcher"
git show --stat --no-patch archive/launcher-2026-10-12
```

A tag, not a branch: a branch invites someone to develop on it, and the archived launcher cannot compile against Phase 2's model anyway (`LauncherViewModel` reads `blockedPackages`, `AppBlockingService` it no longer exposes). The tag points at a *commit*, so it captures exactly what was committed — which is why Step 1's `git status` check comes first.

- [ ] **Step 3: Delete the sources**

```bash
git rm -r -q -- app/src/main/java/com/neuroflow/app/presentation/launcher \
                app/src/test/java/com/neuroflow/app/presentation/launcher \
                app/src/androidTest/java/com/neuroflow/app/presentation/launcher
```

The unit-test files going with them: `ArbGenerators.kt` (kotest `Arb`s for launcher types — grep confirms no non-launcher suite imports it), `HomeScreenGridRenderingBugTest.kt`, `ShortcutPopupTest.kt`, `LauncherSearchEngineTest.kt`, `LauncherBackupManagerTest.kt`, `PinnedAppsDataStoreRegressionTest.kt`. The two `androidTest` files (`LauncherIntegrationTest`, `WorkProfileIntegrationTest`) go too — they have never run on this machine (no emulator, `memory: procus-local-environment-and-git-hygiene`) and they drive `LauncherActivity`.

`presentation/launcher/hyperfocus/` and `presentation/launcher/work/` and `launcher/data/NotificationBadge*` are already gone (Tasks 1-3); if any of them still appears in this directory, Phase 1 or 2 was incomplete and this task must not run.

- [ ] **Step 4: Delete the launcher's manifest surface**

Remove from `AndroidManifest.xml`:
- The `LauncherActivity` entry (the stub Task 15 left at `:88-99`) — the whole `<activity>` block.
- `LauncherKeepAliveAlarmReceiver` (`:102-104`) and `LauncherKeepAliveService` (`:169-176`).
- Permissions `BIND_APPWIDGET` (`:21`), `READ_CONTACTS` (`:23`), `USE_BIOMETRIC` (`:24`), `USE_FINGERPRINT` (`:25`), `EXPAND_STATUS_BAR` (`:26`). Verified by grep: `READ_CONTACTS` is referenced only inside `LauncherSearchEngineImpl.kt:96`, `USE_BIOMETRIC`/`USE_FINGERPRINT` only inside `BiometricAppLock.kt`, and the other three have no code references at all. The app's three `AppWidgetProvider`s in `presentation/widget/` do **not** need `BIND_APPWIDGET` — that permission belongs to the process that *binds* widgets, i.e. the launcher being deleted.
- The `org.adw.launcher.THEMES` `<intent>` from the `<queries>` block (`:29-33`), leaving the `MAIN`+`HOME` `<intent>` Task 15 added.

**Keep** `QUERY_ALL_PACKAGES` (`:22`) — the allowlist picker and the foreground gate both enumerate launchable apps (spec §7). **Keep** `NotificationBadgeService` (now `.data.badge.NotificationBadgeService` per Task 3) despite spec §7 listing it for deletion — delta 1 above is the reason.

- [ ] **Step 5: Delete the launcher's build and resource surface**

- `app/build.gradle.kts:12-13` — the comment and `apply(from = "lint-module-isolation.gradle.kts")`; delete `app/lint-module-isolation.gradle.kts`.
- `app/build.gradle.kts:44-50` — drop the `"proguard-launcher.pro"` entry from `proguardFiles`; delete `app/proguard-launcher.pro`. Its rules only cover `presentation.launcher.**`, so the shrinking config would keep a dangling file with nothing to keep.
- Delete `scripts/check-launcher-isolation.sh` after checking nothing invokes it:

```bash
grep -rn "check-launcher-isolation" --exclude-dir=.git . ; ls .git/hooks | grep -v sample
```

Expected: no hits outside the script itself and no non-sample git hooks. If a hook does call it, delete the hook line in the same commit rather than leaving a hook that fails on every commit.

- `app/src/main/res/values/themes.xml:5-19` — delete `Theme.Launcher.Transparent`; leave `Theme.NeuroFlow` and `Theme.HyperFocusHome`.
- `app/src/main/java/com/neuroflow/app/NeuroFlowApplication.kt` — delete `:9` (`import …launcher.data.AppRepository`), `:39` (`@Inject lateinit var appRepository: AppRepository`), and the pre-warm block at `:95-97`. That pre-warm exists to make the launcher's icon cache hot before the home screen opens; the black home resolves icons per tile through `PackageManager`, so the cache has no consumer. This is the last non-launcher file that names a launcher class, which is why Step 1's grep is the gate.
- `app/src/main/java/com/neuroflow/app/kiosk/DeviceOwnerKioskManager.kt` — delete `:20-21` (keep-alive imports) and, in `enableHybridProtection`, the `LauncherKeepAliveService.start(context)` try/catch at `:90-94` and `LauncherKeepAliveScheduler.schedule(context)` at `:96`. `schedulePolicyWatchdog(context)` (`:97`) and `applyDeviceOwnerPolicies(context)` (`:98`) stay — the WorkManager watchdog is what re-arms the accessibility service, which is now the entire enforcement story. The function's KDoc (`:83-88`, "2) Alarm-based keepalive service schedule") loses that numbered line.

- [ ] **Step 6: Verify**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh assemble && bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh tests && bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh atcompile
```

Expected: green. `compileDebugAndroidTestKotlin` is in the list because the two deleted `androidTest` files are the only instrumentation sources that referenced `LauncherActivity`, and their removal is otherwise unchecked until someone plugs in a device. **Unverifiable here:** whether deleting `BIND_APPWIDGET` breaks adding a widget from a third-party launcher — that path binds *our* providers from *their* process and should not need our permission, but it is a device check, not a grep.

- [ ] **Step 7: Commit**

```bash
git commit -q -m "chore: archive the launcher behind tag archive/launcher-2026-10-12

HyperFocus owns the HOME role, the allowlist and the enforcement path now, and
nothing outside presentation/launcher referenced it after Task 16. Recovery is
git checkout archive/launcher-2026-10-12 -- <the three launcher source trees>;
the tag is annotated with the same command because the archived code cannot
compile against the allowlist model by design." -- \
        app/src/main/java/com/neuroflow/app/presentation/launcher \
        app/src/test/java/com/neuroflow/app/presentation/launcher \
        app/src/androidTest/java/com/neuroflow/app/presentation/launcher \
        app/src/main/AndroidManifest.xml \
        app/src/main/res/values/themes.xml \
        app/build.gradle.kts \
        app/lint-module-isolation.gradle.kts \
        app/proguard-launcher.pro \
        scripts/check-launcher-isolation.sh \
        app/src/main/java/com/neuroflow/app/NeuroFlowApplication.kt \
        app/src/main/java/com/neuroflow/app/kiosk/DeviceOwnerKioskManager.kt
git show --stat HEAD | tail -20
git diff --cached --name-only | wc -l
```

The `git diff --cached --name-only | wc -l` at the end is not decoration: this repository keeps roughly 70 user files staged, and the pathspec commit is what keeps them there. If the count moved, something was committed that was not launcher code — `git show --name-only HEAD` and fix it before continuing.

## Task 18: Dangling-reference sweep and the full verification pass

**Files:**
- Modify: whatever the greps below turn up; nothing is expected, and a hit is a defect in an earlier task rather than work for this one.

**Interfaces:**
- Consumes: the completed tree.
- Produces: the verification evidence for the release decision.

- [ ] **Step 1: Sweep every retired name**

```bash
for pat in "presentation\.launcher" "blockedPackages" "isUnlockActive" "activeUnlockExpiresAt" \
           "LauncherActivity" "LauncherViewModel" "bringLauncherToFront" "Theme\.Launcher" \
           "launcher_prefs" "launcher_settings" "NotificationBadgeService\""; do
  printf '%-28s ' "$pat"; grep -rn "$pat" app/src scripts docs/superpowers/specs 2>/dev/null \
    | grep -v "docs/superpowers" | wc -l
done
```

Expected: `blockedPackages` → 4 (the session entity, the Room column line, and the two backup mappings). `NotificationBadgeService"` → 1 (the `.data.badge` manifest entry, renamed in Task 3). **Everything else → 0.** A non-zero "everything else" means a reference survived into a shipped file; fix it at the site, not by re-adding a retired name.

- [ ] **Step 2: Full suite plus a clean build**

```bash
bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh stop && bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh clean && bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh assemble && bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh tests && \
  bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh atcompile && bash .superpowers/sdd/2026-10-12-hyperfocus-black-home/verify.sh lint
```

Expected: green, and `app/build/test-results/testDebugUnitTest/` should now contain a result file for every `StringSpec`/`FunSpec` in `app/src/test` — count them against the suites:

```bash
ls app/build/test-results/testDebugUnitTest/*.xml | wc -l
find app/src/test/java -name "*Test.kt" | wc -l
```

If the second number is materially larger than the first, some suite is inert — a JUnit4 `@Test` class that survived the migration (`memory: test-suites-must-be-kotest-stringspec`, 27 orphaned tests recorded there). Report the gap rather than fixing it silently: an inert suite in code *this plan* introduced is a task defect; an inert suite the plan never touched is pre-existing debt the user may want in its own commit.

- [ ] **Step 3: Record the delta between "built" and "verified"**

Write the results into the release note or the PR description, with the device-unverified list from the section below copied verbatim. The honest sentence for this plan is: *every policy decision is covered by a JVM test; nothing about becoming the home screen has been executed.* Do not summarize the app as "tested and working" — see `superpowers:verification-before-completion`, and see the note in Global Constraints about not laundering an unverified HOME claim through a green build.

- [ ] **Step 4: Commit only if the sweep found something**

```bash
git status --short
```

If clean, there is no commit — Task 18 is a verification task, and an empty commit to mark it adds noise. If it found a real dangling reference, fix it and commit by pathspec with a message naming which earlier task's assumption was wrong.

---

## Device test plan (blocking before release)

Nothing in this list can be run on this machine — no emulator, no attached device (`memory: procus-local-environment-and-git-hygiene`). Each item is a behavior the plan asserts and the build cannot prove.

| # | Check | Why the build cannot prove it |
|---|-------|-------------------------------|
| 1 | Installing the APK, choosing it in **Settings → Default apps → Home app**, then starting a session: pressing Home lands on black. With the session off, pressing Home must land on the previous launcher via the hand-off, not on black | `CATEGORY_HOME` resolution happens in `PackageManagerService`. On this device the manual choice is the *only* thing that makes the black home a home — `addPersistentPreferredActivity` never runs, so nothing pins it and clearing defaults is a real escape |
| 2 | With HyperFocus **on**: a non-allowlisted app is bounced to the overlay; the allowlisted essentials are not. **Measure the gap** — count how long the blocked app's own UI is visible before the overlay lands, on a cold start and a warm one | `AppBlockingService` needs a real foreground app + `rootInActiveWindow`. Because suspension is dead here, this reactive window is the entire enforcement, and decision A's "no state where the phone is briefly normal" is only as true as this interval |
| 3 | Turning the session **off** hands the device back to the previously-default launcher in one press, and does not loop back into the black home | The `setComponent` hand-off and the anti-reset latch in `HyperFocusHomeActivity.onPause` are runtime-only |
| 4 | Emptying `essentialPackages` does **not** bounce the user out mid-session (HFH-7's claim about a live device) | The test proves the pure function, not `onResume` |
| 5 | The gate's 12-second floor and 3-mismatch auto-pass hold against a real keyboard (IME composition text vs `TextFieldValue`) | `GateTaskEngine` is tested on strings; Compose input is not |
| 6 | A granted pass opens exactly one app and bounces the second | D3's whole claim |
| 7 | `systemExempt` really keeps the IME, permission dialogs and the dialer usable inside a session | Requires a device with the vendor packages the list names |
| 8 | Suspension is **absent**, not working: a gated app still appears installed and launchable in Settings when no session is active, and during a session it is bounced by the overlay rather than by the system hiding it | The device is admin-only, so `setPackagesSuspended` returns at the `:291` guard. Testing real suspension requires a device-owner enrolment, which requires a factory reset (`dpm set-device-owner` refuses a provisioned device) |
| 9 | Navigation-bar buttons are visible on `Theme.HyperFocusHome`'s black background, and the wallpaper does not show through | XML theme resolution, not compiled here |
| 10 | The Room 20 → 21 migration runs on a real install with existing data | `androidTest` cannot run; Task 10 ships the migration **unexecuted** |
| 11 | `AllowlistMigrationRunner` derives a usable first allowlist from a real device's denylist, and never runs while a session is active | Task 11's test uses an in-memory prefs fake |
| 12 | Removing `BIND_APPWIDGET` does not stop widgets from another launcher | Cross-process permission check |
| 13 | The off-state placeholder's "Set my home app" opens `android.settings.HOME_SETTINGS` on the vendor ROM | Intent availability |
| 14 | **After installing a build from Task 1 onward, re-enable HyperFocus in Settings → Accessibility**, then confirm a session still blocks. Until it is re-enabled, enforcement is silently off — no crash, no error, the phone just behaves normally | The service's component ID changed from `.presentation.launcher.hyperfocus.service.AppBlockingService` to `.feature.hyperfocus.service.AppBlockingService`. Android keys `enabled_accessibility_services` by component ID, so the device's existing tick belongs to a component that no longer exists and the new one starts disabled. `AccessibilityUtil.kt:20` builds the string from `context.packageName` plus the new suffix and must match `AndroidManifest.xml:200` — a mismatch compiles and installs fine, and `AccessibilityWatchdogWorker.kt:41-45` only posts a notification. This is the one step in the plan that cannot be recovered from the code |
| 15 | **After installing a build from Task 3 onward, re-enable Notification access** (Settings → Special app access → Notification access), then confirm badge counts still move the energy score. This failure is invisible | `BIND_NOTIFICATION_LISTENER_SERVICE` grants live in `Settings.Secure.enabled_notification_listeners`, keyed by component ID — the same mechanism as row 14. The component moved from `.presentation.launcher.data.NotificationBadgeService` to `.data.badge.NotificationBadgeService` (`AndroidManifest.xml:180`), so the existing tick belongs to a component that no longer exists and the new one starts unbound. No code reads the grant (`isNotificationServiceEnabled` has no call sites), there is no watchdog reminder for it, `onListenerConnected` never runs, `badgeCounts` stays `emptyMap()`, `EnergyScoreRepository.kt:188` sums it to 0, and all 358 unit tests stay green while the energy model silently degrades. **Gap beyond this task:** the only in-app route to that screen is `presentation/launcher/settings/LauncherSettings.kt:2275-2298`, which Task 17 deletes while the service survives — after Task 17 the grant cannot be re-earned from inside the app at all |

Run 1, 3, 6 and 10 before merging to `main`; the rest can be a fast-follow on the first device build.

## Open questions owed by the user

Answering these changes code in this plan; they are not blockers for Phase 1.

1. **What is in the default `essentialPackages` seed?** `FocusSurfaceModel.DEFAULT_ESSENTIAL_SEED` (Task 5) is a guess: dialer, messages, clock, calendar, camera, settings-adjacent tools. Spec §6.4 explicitly puts **Settings outside the seed**, which the user has not ratified — and with the accessibility-based enforcer, Settings is the only route back to re-enabling it if enforcement ever misfires. Task 5's FS-8 and the seed are the two places to change.
2. **Does the D3 tightening survive as written?** Decision E1 says the pass budget keeps ticking between passes (wall-clock, never refunded), and D3 says a pass opens exactly one app. Read together, that means a 5-minute FULL-tier code buys 5 minutes *total* across however many apps the user opens in the gateway — so a second app costs another gate + another code. Spec §5.4's wording suggests the user pictured "one code, one app, that app's minutes". If the intended reading is per-app minutes, `grantPass` should reset `activePassExpiresAt` from the *tier* rather than inherit the shared deadline, and Task 8's PL-7 is the assertion to delete.
3. ~~**Is this device actually enrolled as device owner?**~~ **Answered 2026-10-13: no — the app is held as an active device administrator.** Admin grants nothing this plan depends on, so suspension, HOME pinning, lock-task and the uninstall/force-stop protections are all dead on the target and the accessibility service is the sole enforcement. Folded into Global Constraints, Task 13 Step 5, Task 15 Steps 9-10 and device checks 1, 2 and 8. **Remaining sub-question:** do you want a device-owner enrolment (which needs a factory reset — `dpm set-device-owner` refuses a provisioned device) as a follow-up, or is admin-only the permanent target? If permanent, Task 13 keeps its keep-set math but its commit message and `HyperFocusActivationSheet`'s copy must not promise that apps are "disabled" or "suspended" — say "blocked" instead.

## Self-review record

Run at plan-writing time against the spec, not left for the executor. Six defects found and fixed in the plan text; all six would have produced a red build or a silent runtime failure.

1. **Task 1's commit block had lost its closing fence**, so `## Task 2` rendered inside a bash block and Task 1's commit never showed its file list. Fence pairing is now machine-checked: 156 blocks, all closed.
2. **`AppCatalogRepository` was imported from `feature.hyperfocus.service`** in Task 15's ViewModel; Task 7 creates it under `data/`. Both the import and the file-structure map now say `data/`.
3. **`AllowlistSettingsScreen` was in `surface/` in the map and `screens/` in Task 16.** The map is authoritative and now lists it under `screens/`, alongside `home/`'s three new files and `domain/ActivationGuard`.
4. **Task 15 referenced `EXTRA_START_DESTINATION_GATEWAY`/`_SETUP`, which only Task 16 declared** — Phase 2 could not compile between the two commits. The four constants moved to Task 14's Step 5, where `EXTRA_START_DESTINATION` already lived.
5. **`EmergencyControl` had `val hiddenApps = /* hoisted */ emptyList()`** as a placeholder default: it compiles, renders an empty picker, and silently kills the session's only exit — the one path with no JVM test that could catch it. Now a required parameter, supplied by `HyperFocusHomeViewModel.hiddenApps`, and Task 8's stale "Task 12's `EmergencyControl`" cross-reference is corrected to Task 14/15.
6. **Task 15's activity read preferences with `runBlocking` on a HOME activity's main thread and triggered the hand-off from `onResume`, which raced `resolveHandoff()`'s coroutine.** Rewritten: `resolveHandoff()` takes no argument and reads the store inside `viewModelScope`, and the hand-off fires from a `LaunchedEffect(state, target)`. The companion-object latch that belonged to the activity is gone.

Also corrected: the Phase 3 file counts (37 main + 8 test, not "63"), `Phase 3 is gated on the user` naming Task 17 rather than Task 16, and `onNavigateToRewards` dropped from the new route instead of being carried in as an unused parameter.

**Spec coverage check:** §3.3/§11 → Tasks 3, 17; §4.1 → Task 4; §4.2 → Task 5; §4.3 → Task 10; §4.4 → Task 11; §5.1 → Task 15; §5.2 → Tasks 6, 14; §5.3 → Task 14; §5.4 → Task 8; §5.5 → Task 9; §6.1 → Task 12; §6.2 → Task 5; §6.3 → Task 13; §6.4 → Task 5's seed (open question 1); §7 → Tasks 15, 16, 17; §8 → every row is claimed by a task or by the device test plan; §9 → the ten suites named in the Phase 2 exit check; §10 → the three phases; §12 → the locked-decisions line in Global Constraints. **One naming divergence left on purpose:** spec §9 calls the migration suite `AllowlistMigrationTest`, this plan writes `AllowlistMigratorTest` because the class under test is `AllowlistMigrator` (Task 11). Cosmetic; rename either side if it matters.

**Placeholder scan:** no "TBD", "similar to Task N", "add appropriate error handling", or unwritten test bodies. Every test case in the plan is written out with its assertion, and every deletion is given by file and line range.
