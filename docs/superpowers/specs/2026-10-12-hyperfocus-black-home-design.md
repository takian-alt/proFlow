# HyperFocus black-home redesign — design spec

Date: 2026-10-12
Status: approved in conversation; awaiting written-spec review
Decisions locked: **A, B2, D3, E1, F4, G1** (see §2). These are settled — do not re-open them in implementation.

---

## 1. Problem

NeuroFlow currently ships two products in one APK: a full replacement Android launcher
(63 files under `presentation/launcher/` — app drawer, widgets, folders, icon packs, theming,
paged home screen, notification badges, keep-alive services) and a focus-session enforcer
(HyperFocus). HyperFocus is filed *inside* the launcher package and inherits the launcher's
assumptions, and its blocking model is a **denylist**: the user ticks which apps to block
(`HyperFocusPreferences.blockedPackages`), pre-selected by `distractionScore > 70`
(`HyperFocusActivationSheet.kt:112`). A denylist always misses the app that was not thought of.

The redesign retires the launcher product and replaces HyperFocus's surface with a single
black home screen that shows only an **allowlist**, with hidden apps reachable only through a
costly gate.

## 2. Locked decisions

| ID | Decision | Consequence |
|----|----------|-------------|
| **A** | One permanent minimal `CATEGORY_HOME` activity. | The launcher *product* is deleted; the HOME role survives in a minimal form. Chosen because it is the only enforcement option that does not depend on device-owner privileges. |
| **B2** | Two tiers, both global: `essentialPackages` and `allowedPackages`, configured once and identical across sessions. | Explicitly **not** per-session (that was B3, overruled). The allowlist does not rotate with the session. |
| **D3** | Unlock is a **per-app pass**. Granting an unlock makes exactly one app launchable; every additional app switch requires re-entering the gate. | Explicitly **not** "widen the allowlist" (D2, overruled) and not "lift the black home" (D1, rejected). |
| **E1** | Budget ticks in wall-clock from the moment of grant; the pass ends when the granted app leaves the foreground; elapsed time is not refunded. | Chosen over E2 (forfeit remaining budget) as non-punitive, and over E3 (pass survives backgrounding) as leak-proof. |
| **F4** | The gate is a randomized effort-only cognition task plus a free-text reason, and every request is logged. Always completable — never a lockout. | The log is the only part of the design that produces information rather than obstruction. |
| **G1** | Git-only archive: extract HyperFocus, then delete the launcher behind a tag. | Explicitly not an unwired Gradle module (G2, silent rot) and not dead code left in the tree (G3, actively harmful in a repo this size). |

## 3. Architecture

### 3.1 Package layout after

```
com.neuroflow.app.feature.hyperfocus/
  home/        HyperFocusHomeActivity, HyperFocusHomeViewModel, HyperFocusHomeScreen
  gate/        GateTaskEngine, GateTaskScreen, GatewayScreen, EmergencyControl
  surface/     FocusSurfaceModel, AllowlistSettingsScreen
  data/        HyperFocusDataStore, HyperFocusSessionDao/Entity, UnlockCodeDao/Entity,
               GateRequestDao/Entity, AppCatalogRepository
  domain/      HyperFocusManager, RewardEngine, UnlockCodeRepository, AESUtil
  service/     AppBlockingService, HyperFocusMonitorService, UnlockTimerService
  screens/     BlockingOverlayScreen, CodeEntryScreen, HyperFocusActivationSheet,
               PermissionSetupScreen, PlanningPromptScreen, RewardSection, RewardsScreen
  components/  HyperFocusStatusBar
  util/        AccessibilityUtil
```

`launcher/work/ScheduleAutoTasksWorker` → `com.neuroflow.app.worker/` (it is app scheduling, not
launcher behavior; referenced by `NeuroFlowApplication.kt:13` and `TaskRepository.kt:14`).
Its test `app/src/test/.../launcher/work/EnergyProjectionBlendingTest.kt` moves with it.

### 3.2 Extraction is a move, not a rewrite

Step 1 changes package declarations and imports only. No logic, no signatures, no behavior.
Room entity moves are safe because Room keys on `@TableName`, not Kotlin package; `@Database`
(`NeuroFlowDatabase.kt:295`) import paths change but `version` stays 20 until step 2 adds the
new entity.

### 3.3 Deleted with the launcher

Everything under `presentation/launcher/` that is not listed in §3.1: `LauncherActivity`,
`HomeScreen`, `LauncherViewModel`, `AppRepository`, `PinnedAppsDataStore`, `AppDrawer`,
`LauncherSettings`, `LauncherTheme`, `DockRow`, `HomeScreenGrid`, `HomeScreenPages`,
`WidgetHost`, `AppWidgetHostWrapper`, `AdaptiveIconProcessor`, `IconPackManager`,
`ShortcutPopup`, `AppFolderOverlay`, `BiometricAppLock`, `LauncherSearchEngine*`,
`LauncherGestureHandler`, `LauncherBackupManager`, `NotificationBadge*`,
`PackageChangeReceiver`, `LauncherKeepAlive*`, `LauncherModule`, `SafeHomeScreen`,
`components/DateTimeDisplay`, `components/FocusTaskCard`, `components/HabitQuickRow`,
`components/AppIcon`.

`AppRepository`'s useful part — installed-app enumeration and icon loading via `LauncherApps`
— is **re-implemented small** as `AppCatalogRepository` (~80 lines: `getLaunchableApps()`,
`launch(pkg)`, label+icon lookup). It is not copied, because the original carries hidden-package
filtering, work-profile handling, icon LRU tuning, and recents tracking that the new model
does not use.

## 4. Data model

### 4.1 `HyperFocusPreferences` changes

Remove:
- `blockedPackages: Set<String>` — deleted outright, not kept alongside. A coexisting denylist
  and allowlist would make `AppBlockingService` ambiguous.

Add:
- `essentialPackages: Set<String>` — global tier 1.
- `allowedPackages: Set<String>` — global tier 2.
- `activePassPackage: String?` — D3's single granted app.
- `activePassExpiresAt: Long?` — E1's wall-clock budget deadline, set at grant time.
- `realHomeComponent: String?` — flattened `pkg/class` for the off-state handoff (§5.1).

The store is a single hand-rolled JSON blob (`HyperFocusDataStore.kt:103-179`), so each new
field is one `json.put` plus one `obj.optX` with a default. Unknown keys are ignored on read,
so an old blob read by new code yields empty allowlists — handled by migration in §4.4.

`RewardTier.unlockMinutes` (MICRO=2, PARTIAL=10, EARNED=30, FULL=`Int.MAX_VALUE`,
`Enums.kt:71-77`) becomes the **pass budget in minutes** rather than a device-wide unlock window.
Values are unchanged in this spec; MICRO=2 is a poor experience under E1 and is listed as a
tuning follow-up, not a design change.

### 4.2 Effective allowlist — one function, two consumers

`FocusSurfaceModel.resolvedAllowlist(prefs, systemExempt, selfPackage): Set<String>` =
`essentialPackages ∪ allowedPackages ∪ {activePassPackage if not expired} ∪ systemExempt ∪ {selfPackage}`

Both the black home's grid and `AppBlockingService`'s gate read this one function. No component
computes its own view of what is allowed.

### 4.3 `GateRequestEntity` (F4's log)

Room entity, `@TableName("gate_requests")`, fields: `id`, `sessionId`, `packageName`,
`appLabel`, `statedReason`, `taskType`, `elapsedMillis`, `grantSource`
(`REWARD_CODE` | `EMERGENCY` | `ABANDONED`, see §5.5), `createdAtMillis`.
Added to `NeuroFlowDatabase` → **version 21** plus a migration that only creates the table.
No `exportSchema` change (already `true`).

This is the only new table. It is not backed up to Firestore in this spec (§12).

### 4.4 Migration of existing state

On first read after upgrade, if the stored blob has `blockedPackages` non-empty and
`allowedPackages` absent:

1. Enumerate launchable packages via `AppCatalogRepository`.
2. `essentialPackages` = the §6.4 default seed intersected with installed packages.
3. `allowedPackages` = (installed launchable packages) − `blockedPackages` − `systemExempt`.
4. Discard `blockedPackages`.

The rule: **the complement of the old denylist becomes the new allowlist**, so the user's
previous intent is preserved and the newly-default-blocked set is exactly the set they had
already chosen to block. Logged as a one-shot migration; never re-run.

`distractionScores` relocation: the map currently lives in launcher-owned `launcher_prefs`
(`PinnedAppsDataStore.kt:79`) and its **only writer is launcher UI** (`LauncherSettings.kt:193`
and `:198` → `LauncherViewModel.updateDistractionScore`). Because HyperFocus's picker reads it,
the map moves into `hyperfocus_prefs` with the new allowlist screen as its writer. On first read,
copy any existing `launcher_prefs` value across before `launcher_prefs` stops being read at all.
`UserPreferencesDataStore.distractingPackages` (`:118`, seeded from
`DistractionEngine.DEFAULT_DISTRACTING_PACKAGES`) lives outside the launcher, is a different
concept, and is untouched.

## 5. Components

### 5.1 `HyperFocusHomeActivity` — the black home

`CATEGORY_HOME` + `DEFAULT` + `MAIN` intent filter, `singleTask`, empty `taskAffinity`,
`stateNotNeeded`, `excludeFromRecents`, opaque black theme.

Behavior:
- `isActive == true`: render `HyperFocusHomeScreen` — a `LazyVerticalGrid` of the resolved
  allowlist, scrolling enabled, no clock, no tasks, no quotes, no dock, no pages.
- `isActive == false`: `onResume` starts `ACTION_MAIN` + `CATEGORY_HOME` targeting
  `realHomeComponent`, and paints a minimal "HyperFocus is off — [Start session]" placeholder
  behind it. The placeholder is the fallback if the handoff is denied, so the off state is never
  an unusable black wall.
- Tapping a grid icon: if the package is in `essential ∪ allowed`, launch directly. If it is
  only present because of `activePassPackage`, launch and it is already budgeted.
- Two persistent controls sit below the grid, always visible without scrolling out of reach:
  **Gate** (opens `GateTaskScreen` → `GatewayScreen`, §5.2) and **Emergency** (one tap → direct
  app picker → 10-minute single-app pass, §5.3). Emergency shows its remaining one-shot status;
  once used it renders spent and disabled until the next session.
- Back: consumed, never finishes. Same rule the old launcher used (`LauncherActivity.kt:425`).
- Icon taps use `LauncherApps.startMainActivity` with the user handle from the catalog.

`realHomeComponent` resolution, once at first launch: `queryIntentActivities(HOME intent)`,
drop own package. Exactly one left → store it. More than one → one-time picker. None →
stay on the black home in off state (a device with only NeuroFlow installed cannot hand off).

### 5.2 `GateTaskScreen` + `GateTaskEngine` (F4)

Entry to `GatewayScreen`. One randomized cognition task, then a mandatory free-text reason.

Task types, chosen uniformly at random per attempt:
- `TRANSCRIBE` — display a short phrase, retype it exactly.
- `COUNT_BACK` — start at a random 3-digit number that is not a multiple of 100, step by −7,
  enter the next four values.
- `RESTATED` — display a sentence, retype it exactly.

Non-negotiable properties, because a gate that can be failed is a trap and traps get uninstalled:
- Reason must be ≥ 8 characters. This is the only hard requirement.
- Cognition-task mismatch shows which item was wrong and allows retry; after **3 mismatches the
  task auto-passes**. The friction is time and deliberation, not correctness.
- Submit is disabled until at least **12 seconds** have elapsed, so the gate cannot be
  muscle-memory-skipped.
- Every attempt, pass or abandon, writes a `GateRequestEntity`. Abandoning writes
  `grantSource = ABANDONED` with the elapsed time — the abandoned attempts are the most
  informative rows in the table.

### 5.3 `GatewayScreen`

Behind the gate. Contains, in this order: the reward-code entry (`CodeEntryScreen` logic),
then the full hidden-app list. Selecting an app here requires an active earned budget; with no
budget the list is visible but inert and points at the emergency control.

**Emergency does not live behind the gate.** The one-time-per-session emergency control is a
persistent small affordance on the black home itself: one tap, no cognition task, opens a
direct app picker, grants a 10-minute single-app pass. This is a correction to the verbal
design, which placed both the gate and "one tap, never gated" in the same screen — an
unimplementable pair. The gate charges for *reward-funded* access, which is repeatable; it must
not charge for the *only* exit, which is one-time.

### 5.4 Pass lifecycle (D3 + E1)

- Grant sets `activePassPackage` and `activePassExpiresAt = now + tierMinutes`. FULL's
  `Int.MAX_VALUE` minutes (`Enums.kt:76`) converts to a deadline that never arrives in practice;
  no special-casing is added for it.
- Emergency sets `activePassPackage` and `activePassExpiresAt = now + 10 min`, and
  `emergencyUsed = true`. One time per session; `HyperFocusManager.triggerEmergencyBypass`
  already enforces both, and already refuses `TIME_BASED` sessions (`HyperFocusManager.kt:330`).
- **`TIME_BASED` sessions have no route to hidden apps at all.** Codes come from task
  completion, which timed sessions do not score (`onTaskCompleted` returns early at
  `HyperFocusManager.kt:228`), and emergency already refuses them. The only exit from a timed
  black home is deactivation. This is intended and is the point of a timed session, but it must
  be stated at activation so it is not discovered mid-session.
- `activePassPackage` clears when the pass expires or when the granted app leaves the
  foreground (detected by `AppBlockingService` observing a foreground package that is neither the
  pass nor allowed). The expiry deadline does **not** move — E1's clock runs continuously.
- A second switch requires re-entering the gate. If budget remains, no new code is consumed;
  the cost is the cognition task plus elapsed time.
- A deep link or share sheet that hands off to a package outside the resolved allowlist lands on
  the gate like any other arrival. D3 does not distinguish "arrived by icon" from "arrived by
  intent"; a passed app cannot be used as a doorway to a second one.

### 5.5 State machine cleanup

`MICRO_UNLOCKED`, `PARTIAL_UNLOCKED`, `EARNED_UNLOCKED`, `PLANNING` and `FULLY_UNLOCKED` have
**no write sites anywhere in `app/src/main/java`** (verified by grep; they appear only in
`Enums.kt:57-63` and in read positions in `RewardEngine.kt:106,113`,
`HyperFocusManager.kt:244,250`, `HyperFocusMonitorService.kt:75`, `HyperFocusStatusBar.kt:157`).
So `RewardEngine.isUnlockActive`'s `FULLY_UNLOCKED` short-circuit is dead, and the state machine
in practice only ever holds `INACTIVE`, `ACTIVE`, `FULL_REWARD_PENDING`.

This spec reduces `HyperFocusState` to those three and expresses "is there a usable pass" solely
through `activePassPackage != null && activePassExpiresAt > now`. Pass validity and session state
stop being two overlapping sources of truth.

Deleting the five unused constants requires removing their read sites, not just the enum entries:
`RewardEngine.kt:106` and `:113` (the dead `FULLY_UNLOCKED` short-circuits inside `isUnlockActive`
and `secondsRemaining`), `HyperFocusManager.kt:244` and `:250` (the tier-progression guards),
`HyperFocusMonitorService.kt:75`, and `HyperFocusStatusBar.kt:157`. `GateRequestEntity.grantSource`
in §4.3 correspondingly drops its `FULLY_UNLOCKED` case and uses `REWARD_CODE`, `EMERGENCY`,
`ABANDONED`.

## 6. Enforcement

### 6.1 `AppBlockingService` inversion

`checkAndBlockApp` (`AppBlockingService.kt:229-288`) currently reads:

```kotlin
val isUserBlockedApp = pkg in prefs.blockedPackages
if (!isUserBlockedApp && !isTamperSensitive) return@launch
if (RewardEngine.isUnlockActive(prefs)) return@launch          // line 248 — whole-device exemption
```

becomes:

```kotlin
val gated = pkg !in FocusSurfaceModel.resolvedAllowlist(prefs, SYSTEM_EXEMPT, selfPackage)
if (!gated && !isTamperSensitive) return@launch
```

The line-248 device-wide exemption is **removed**, not narrowed: allowance is already expressed
by `activePassPackage` being inside the resolved allowlist. This is a real tightening of
existing behavior — today any active unlock frees every app, and after this change it frees one.

`evaluateProtectedSurface` (`:290-340`, the settings / app-info / device-admin tamper guard) and
`packageTamperReceiver` (`:119-139`) are orthogonal to the denylist→allowlist flip. The one edit
is `packageTamperReceiver`'s `packageName in prefs.blockedPackages` check (`:133`), which becomes
"not in the resolved allowlist".

### 6.2 `systemExempt` — mandatory, not optional

A pure "not in allowlist → block" rule locks the user out of their own keyboard, permission
dialogs, and incoming calls. This is the standard failure mode of allowlist blockers and it is
not negotiable technically. `SYSTEM_EXEMPT` is a constant set, not user-configurable:

- The enabled IME subservice packages, read from `InputMethodManager.enabledInputMethodList` at
  check time (their package names, not hardcoded).
- `com.android.systemui`, `android` (system dialog windows).
- `com.android.server.telecom` and the `DIAL_ACTION`-handling dialer packages — an incoming call
  must never be bounced.
- `com.google.android.permissioncontroller`, `com.android.permissioncontroller`,
  `com.android.packageinstaller`, `com.google.android.packageinstaller`,
  `com.miui.packageinstaller`.
- `BuildConfig.APPLICATION_ID` (already special-cased at `:230`).

Emergency-call behavior is `telecom`'s, not ours: we do not gate `DIAL_ACTION` launches at all.

### 6.3 Device-owner suspension

`DeviceOwnerKioskManager.syncHyperFocusBlockedPackagesSuspension` (`:285`) takes a set to suspend
and is called from `activateInternal` (`HyperFocusManager.kt:217`) and `deactivate` (`:62`).
Re-implemented as `syncAllowlistSuspension(context, keepSet, enabled)` where `keepSet` =
`resolvedAllowlist` − `systemExempt` − self, and the suspended set = all other launchable
packages. Requires device owner; no-op otherwise (`:290` already guards). Suspension is the
strong path; §6.1's accessibility watchdog remains the path that works without device owner.

`shouldRequireLauncherAsHome` (`:122`), `launchKioskHome` (`:211`) and `bringLauncherToFront`
(`:230`) re-point from `LauncherActivity` to `HyperFocusHomeActivity`, keeping the
`isAppDefaultLauncher` (`:374`) guard.

### 6.4 Default `essentialPackages` seed

Dialer, Messages, Contacts, Camera, Clock, and NeuroFlow itself. **Settings is deliberately
excluded** — reachable through `systemExempt`'s permission/installer entries and through the
tamper guard, but not presented as an icon. This is the one value in the spec the user may
most plausibly want to overrule.

## 7. Manifest, DI, and navigation deltas

Manifest:
- Move `MAIN`/`HOME`/`DEFAULT` filter and the `taskAffinity=""`/`stateNotNeeded`/
  `excludeFromRecents`/`clearTaskOnLaunch`/`enableOnBackInvokedCallback=false` attributes from
  `LauncherActivity` (`AndroidManifest.xml:70-85`) to `HyperFocusHomeActivity`.
- Delete `LauncherKeepAliveService` (`:169-176`), `LauncherKeepAliveAlarmReceiver` (`:102-104`),
  `NotificationBadgeService` (`:179-186`).
- Delete permissions `BIND_APPWIDGET`, `READ_CONTACTS`, `USE_BIOMETRIC`, `USE_FINGERPRINT`,
  `EXPAND_STATUS_BAR` and the `org.adw.launcher.THEMES` `<queries>` block (`:20-33`).
  `USE_BIOMETRIC`/`USE_FINGERPRINT` are only used by `BiometricAppLock`, which is archived.
- **Keep `QUERY_ALL_PACKAGES`** — both the allowlist picker and the foreground gate must
  enumerate launchable apps.
- Add a `<queries>` element for `ACTION_MAIN` + `CATEGORY_HOME` resolution (§5.1), required on
  API 30+ even with `QUERY_ALL_PACKAGES` for role resolution.
- `HyperFocusActivity` (`:189-196`) stays; its nav graph is rebuilt in §5.3/§5.4.
- New theme `Theme.HyperFocusHome`: opaque `#FF000000` `windowBackground`, no wallpaper show,
  edge-to-edge. `Theme.Launcher.Transparent` and `values/` launcher themes deleted.

DI: `LauncherModule` is deleted; a `HyperFocusModule` binds `HyperFocusDataStore`,
`HyperFocusManager`, `AppCatalogRepository`, `GateRequestDao`. `AppModule` and `SyncModule`
import paths follow the entity/DAO moves (`SyncModule.kt:18`, `NeuroFlowDatabase.kt:24-27`,
`CloudBackupManager.kt:14`, `FirestoreBackupRepository.kt:23`).

Navigation in `presentation/common/NeuroFlowApp.kt`:
- Delete route `launcher_settings` (`:93`, composable `:463`, and `LauncherSettingsRoute`
  `:565-571`, which is the only non-launcher use of `LauncherViewModel` at `:570`).
- Keep route `rewards` (`:94`, `:470`) — `RewardsScreen` is HyperFocus-owned.
- Add route `hyperfocus_allowlist` for the B2 tier editor.
- `MoreMenuSheet.kt:112` label "HyperFocus & Launcher" → "HyperFocus"; its target becomes the
  allowlist screen.

## 8. Error handling

| Case | Behavior |
|------|----------|
| `realHomeComponent` missing or unresolvable in off state | Stay on the off placeholder. Never a blank black screen. |
| Off-state handoff denied by the system | Placeholder remains with "Start session" and a "Set my home app" shortcut to Settings. |
| `queryIntentActivities(HOME)` returns only self | Off state is the black home with the allowlist editor; no handoff attempted. |
| Allowlist empty at activation | Activation is blocked with an explicit message; an empty allowlist would gate the keyboard and the dialer. |
| App uninstalled mid-pass | Pass cleared; `PackageChangeReceiver` is gone, so this is detected lazily at next foreground check. |
| `GateRequestEntity` write fails | Gate still opens. Losing a log row must never cost the user access. |
| DataStore blob corrupt | `deserialize` already returns defaults (`HyperFocusDataStore.kt:176`). Defaults are: empty allowlist, no pass, session `INACTIVE`. The home then renders off-state, not a locked device. |
| `AppBlockingService` cannot start `HyperFocusActivity` (API 12+ background-launch denial) | Existing fallback preserved (`:276-286`): `bringLauncherToFront` under strict kiosk, else `GLOBAL_ACTION_HOME`. With the allowlist this lands the user back on the black home, which is the desired outcome. |
| Accessibility service off during session | `AccessibilityWatchdogWorker` and `HyperFocusMonitorService` already handle re-arming and tamper reporting; unchanged apart from imports. |

## 9. Testing

Repo rule: suites in `app/src/test` must be kotest `StringSpec` — JUnit4 `@Test` and Robolectric
are inert there. New suites:

- `FocusSurfaceModelTest` — allowlist resolution, pass expiry, `systemExempt` inclusion, empty
  allowlist rejection.
- `GateTaskEngineTest` — random task selection, 3-mismatch auto-pass, 12 s floor, reason
  validation, ABANDONED logging.
- `PassLifecycleTest` — grant sets package and deadline; expiry does not move on re-entry; FULL
  tier deadline; emergency one-time; `TIME_BASED` refuses emergency.
- `AllowlistMigrationTest` — old denylist blob → complement allowlist, run once, idempotent on
  re-read.
- `HyperFocusManagerTest` (exists, `app/src/test/.../hyperfocus/domain/`) — rewritten for the
  inverted field set; `RewardEngineTest` similarly.
- `RoomMigrationV20ToV21Test` — table creation only.

Device-only, **cannot be verified on this machine** (no AVD in use for this project, `adb` not on
PATH) and will be reported as unverified rather than as working: HOME-role registration and the
off-state handoff, grid rendering and scroll, IME exemption in a real session, the accessibility
bounce, device-owner suspension breadth.

Existing `app/src/androidTest/.../launcher/LauncherIntegrationTest.kt` and
`WorkProfileIntegrationTest.kt` are archived with the launcher, along with their
`MANUAL_TESTING_GUIDE.md` and `TASK_30_SUMMARY.md`.

## 10. Implementation sequence

Three steps, each ending in a green build and independently revertible:

1. **Extraction.** Package move only (§3.1, §3.2). Behavior identical. Green build.
2. **Inversion + new surface.** Allowlist model, migration, black home, gate, gateway, pass
   lifecycle, blocking-service flip, manifest nav/DI deltas, tests. Launcher files still exist
   but are unreferenced and unreachable.
3. **Archive.** `git tag archive/launcher-2026-10-12`, then delete the launcher files, the
   launcher tests, `launcher_prefs`, and `Theme.Launcher.*`.

Rationale for the split: a HOME-role regression and a package-move regression must not land in
one commit, because neither is verifiable locally and bisecting them on a device later would be
guesswork.

Step 2 is the risky one and is where a device test plan is needed before release, not after.

## 11. Out of scope

- Firestore backup of `gate_requests`.
- Any change to `TaskScoringEngine`, `DistractionEngine`'s usage-stats computation, or per-task
  `TaskEntity.distractionScore` (a different concept from the per-package map).
- Icon packs, theming, widgets, app-locking, notification badges — deleted, not redesigned.
- Replacing the launcher on non-HOME-permission devices (Option C, rejected at design time).
- Changing `RewardTier` minute values.

## 12. Corrections to the verbal design

Two places where the conversation as stated is not implementable, resolved here:

1. **Emergency placement.** The design said the gateway screen contains the reward code *and* the
   one-tap emergency button, and also that the emergency path is never gated. The gateway is
   gated. Emergency therefore moves onto the black home (§5.3). The gate charges for repeatable
   reward access, not for the one-time exit.
2. **`blockedPackages` deletion.** The design implied the allowlist was additive. It is
   substitutive (§4.1) — holding both makes §6.1 ambiguous, and the migration only works if the
   denylist is consumed once and discarded.
