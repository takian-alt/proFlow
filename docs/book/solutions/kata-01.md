# Kata 1 — solution

Everything below was run on 2026-10-12 in **the author's tree** (`/home/takian/Project/PROCUS`,
`book/part1-neuroflow`, 118 dirty paths, 70 staged), whose committed `app/src` tip this book cites as
`b781cbb`; **a clone of `b781cbb`** is the other tree, and every output line is the author's-tree run's.
Where the two answer differently, both numbers are printed — the point, not a hedge. Three files this key
quotes are dirty: `MainActivity.kt` (309 lines here, 293 at the tip) and `UserPreferencesDataStore.kt`
(796 / 733) are `MM`, `AndroidManifest.xml` (233 / 208) is `M `. `NeuroFlowApplication.kt` and
`DatabaseCleaner.kt` are clean, so their citations read the same in both trees.

## 1. Recall

The seven commands, in order, with the two that are easy to drop marked:

```bash
./gradlew :app:assembleDebug --console=plain   # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew --stop                               # RAM: 4 GiB Gradle heap + 2.5 GiB emulator do not fit
~/Android/Sdk/platform-tools/adb -e shell getprop sys.boot_completed   # poll until it prints 1
~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
~/Android/Sdk/platform-tools/adb shell am force-stop com.neuroflow.app # ← without this, not a cold start
~/Android/Sdk/platform-tools/adb shell am start -W -n com.neuroflow.app/.MainActivity
~/Android/Sdk/platform-tools/adb logcat -d -s BookCh1
```

The defect that is yours: the committed `gradlew`, whose `eval set --` line ends `…GradleWrapperMain "$@\""`
and must end `…GradleWrapperMain \"\$@\""` — one backslash before the `$`, so `eval` expands `"$@"` itself
instead of receiving a string whose word boundaries were already flattened. The defect that is **not** yours:
`LocalIsDarkTheme` and `DoFirstTextDark`, used by three committed files, defined only in the owner's
uncommitted theme edits.

`last_crash.txt` always begins with three fields, in this order: `Thread:`, `Timestamp:`, `StackTrace:` —
the literal format written at `NeuroFlowApplication.kt:61`, in a file clean in git, so `:61` is `:61` in
both trees.

**The crash signal is `LaunchState`, not `Status`.** A completed launch reads `COLD` (or `HOT`); one whose
process died before attaching an activity reads `UNKNOWN (0)`, and `TotalTime` goes with it. `Complete`
prints anyway — it means *this command finished*, not *the app came up*. Do not diagnose off `Status`: the
author's-tree crashed run printed no `Status` line at all, while the clone of `b781cbb` running the same
perturbation printed `Status: ok` directly above `LaunchState: UNKNOWN (0)`. The field that tracked the truth
in both runs is the discriminator; the one that reads like a verdict is the one that lied.

## 2. Modify

**The starting state.** Perturb item 3 had already deleted your two lines and proved the file was back, so
this item began from a clean `MainActivity.kt` beside a stale APK: re-adding a line was a new edit, not an
undo of the undo.

**What you should have predicted is not the same as what this page measured.** A launch started by
`am force-stop` and then `am start -W` is a fresh task with no restored state, so `savedInstanceState` is
`null` and the line reads `D BookCh1 : savedInstanceState=null`. That is reasoning about the Android API,
not evidence from a walk — Chapter 1 measured the plain `I am here` line
(`10-12 18:11:28.938  2114  2114 D BookCh1 : I am here`) and never captured this variant. If your run printed
something else, your run is the evidence and this paragraph is the claim; log the difference.

**The second launch, which this key does not own either.** `am start -W` with no `force-stop` before it is no
promise that `onCreate` runs: `MainActivity` is `android:launchMode="singleTask"`
(`grep -n "launchMode" app/src/main/AndroidManifest.xml` answers `61:` here and `60:` at the tip), so the
command can deliver to the task already on screen and report a launch that never reaches your line — the case
Perturb warns about: "`am start` on an existing task is not the launch you think you are making." Both
readings are API reasoning and this walk ran no device; your dump adjudicates, which is why the kata asked for
two `Result:` fields. Presence, absence and content each carrying information is Logcat as an instrument, not
a heartbeat.

**The filter is not a delete.** After you change the tag to `BookCh1x`, `logcat -d -s BookCh1` goes silent
while the old line is still in the buffer, because `-s <tag>` selects at *read* time;
`$ADB logcat -d | grep BookCh1` still finds the previous process's line until the ring rolls over. What a tool
prints is a query, not the world.

**The undo.** `grep -c "BookCh1" app/src/main/java/com/neuroflow/app/MainActivity.kt` printing `0`, with your
own pre-edit hash back in place, is the whole ritual: delete what you typed, check with a count. `git restore`
was never on the table here — the file was `MM` before you opened it, so restore would have discarded the
owner's staged half along with yours.

## 3. Diagnose

**Observation one is a gate, not a bug: `app/src/main/java/com/neuroflow/app/MainActivity.kt:149-154` in the
author's tree, `135-140` in a clone of `b781cbb`.** The block is pasted from the author's tree with its own
numbers marked:

```kotlin
                    val prefs = preferences            // :149
                    when {                              // :150
                        // Still loading — render nothing to avoid flash
                        prefs == null -> Unit           // :152
                        // First launch — show onboarding
                        !prefs.onboardingCompleted -> OnboardingScreen(   // :154
```

`preferences` arrives from `preferencesDataStore.preferencesFlow.collectAsState(initial = null)` just above
the block — `:127` here, `:122` at the tip — so the first frame takes the `prefs == null` branch and renders
*nothing*. That is why the tempting suspect in this kata is the `Unit` line, and why it is innocent: it draws
no screen at all. Onboarding is the next branch, because the flag is `false` until the `onComplete` callback
writes `onboardingCompleted = true` (`:171` here, `:157` at the tip). The grep that prints all three:

```bash
grep -n "prefs == null\|!prefs.onboardingCompleted\|onboardingCompleted = true" app/src/main/java/com/neuroflow/app/MainActivity.kt
152:                        prefs == null -> Unit
154:                        !prefs.onboardingCompleted -> OnboardingScreen(
171:                                        onboardingCompleted = true
```

In a clone of `b781cbb` the same command answers `138:`, `140:`, `157:` — run it on
`git show b781cbb:app/src/main/java/com/neuroflow/app/MainActivity.kt` and read your own. The default and the
read path in `UserPreferencesDataStore.kt` (also `MM`) sit at `:52` and `:318`, at the tip `:43` and `:286`;
both say the same thing — no stored record means "not onboarded". So a fresh install is *logically* a first
launch even when the device is not new, and the Today screen is unreachable until someone finishes the flow.
Cause one is a design decision you can walk through in twenty seconds.

**Observation two is that nothing was ever going to return the note: `app/src/main/AndroidManifest.xml:37`
in the author's tree and `:36` in a clone of `b781cbb`** — the manifest is `M ` here (233 lines against the
tip's 208), which makes this the citation that moves across a commit boundary, and the reason the kata asks
you to name its tree.

```xml
    <application                                   // :35 here, :34 at the tip
        android:name=".NeuroFlowApplication"
        android:allowBackup="false"                // :37 here, :36 at the tip
        android:restoreAnyVersion="false"          // :38 here, :37 at the tip
        android:enableOnBackInvokedCallback="true"
```

```bash
grep -n "allowBackup\|restoreAnyVersion\|fullBackupContent" app/src/main/AndroidManifest.xml
37:        android:allowBackup="false"
38:        android:restoreAnyVersion="false"
```

At the tip the same grep answers `36:` and `37:`. One attribute decides the case, and the absence of any
`fullBackupContent` line in it is part of the answer: with auto-backup off there is no backup set to restore
and no include/exclude rules being negotiated. An uninstall deletes `/data/data/com.neuroflow.app/` — the Room
database, the DataStore file, `shared_prefs/`, the install marker under `noBackupFilesDir`, and
`files/last_crash.txt` — and Android hands nothing back. The note was deleted, not hidden.

**The distinction the brief hides**, and the reason "reinstall" is two words wearing one: `adb install -r`
replaces the APK and **keeps** the app's data directory, so the note survives and the app does not consider
itself freshly installed — neither symptom appears. Uninstalling first removes the directory and fires *both*
symptoms from *one* action: DataStore gone, so `onboardingCompleted` defaults to `false`; database gone, so
the note is gone. Two different causes, one a default in a model, the other a manifest policy. If your answer
worked for both observations in a single sentence, it was the tautology, not the diagnosis.

**The suspect you must clear, because Chapter 1's Compare item 2 planted it and labelled it the kata's red
herring:** `DatabaseCleaner.enforceRestoreVersionPolicy`, called from `NeuroFlowApplication.kt:90` — an
ordinary call in a file clean in git, so `:90` is `:90` in both trees.

```bash
grep -n "fun enforceRestoreVersionPolicy" app/src/main/java/com/neuroflow/app/data/local/DatabaseCleaner.kt
84:    suspend fun enforceRestoreVersionPolicy(firstLaunchAfterInstall: Boolean): Boolean = withContext(Dispatchers.IO) {
```

The purge is **three conditions at once**, `:87-89` (a clean file, so every line here is a both-tree fact):
`firstLaunchAfterInstall`; `recordedVersion > 0L`; `recordedVersion != currentVersion`. The first is the
marker `install_marker_v1` in `noBackupFilesDir` (`NeuroFlowApplication.kt:35`; `:134` returns `false` the
moment it exists). The second comes from the private `SharedPreferences` file `backup_state` (`:24`, opened at
`:28-30`, defaulting to `0L` at `:109-111`). The third sets that integer against the installed `versionCode`
— `11` in the author's tree, `10` at the tip.

Run the kata's two shapes through it. **Uninstall, then install:** the marker is gone, so condition one is
*true*, but `backup_state` went with it, `recordedVersion` is `0L`, condition two is *false*, and
`database.clearAllTables()` at `:92` never runs. **`adb install -r`:** the marker survives, condition one is
*false*, and a bumped `versionCode` does not rescue it — `:101` merely records the new number and returns
`false`. Neither shape fires; an answer naming `:90` has walked into the chapter's own table.

What does fire it? One shape only: old data arriving **without the marker but with `backup_state` intact**,
its recorded version differing from the installed APK's — a *restore*, since `noBackupFilesDir` is outside the
backup set by definition, which is why a restored tree has preferences and no marker. The comment above `:84`
says it: data "created by a different app version" is purged. `allowBackup="false"` forecloses even that, so
the attribute that dooms the note is the same one that disarms the purge. Watching `:92` run would take
manufacturing the mismatch on a debuggable build — delete only the marker, keep the prefs, install a different
`versionCode` — **and this key ran no device and prints no output for that recipe**. What it does claim is
arithmetic on eight lines, plus the one case those lines name.

Wrong turns worth naming, because they feel like answers: "onboarding is Firebase deciding you are new" (the
gate reads a local DataStore value; Firebase is not in that `when` block); "the note is in Room so the Room
file is corrupted" (a missing row and an unreadable file are different claims —
`$ADB shell run-as com.neuroflow.app ls databases` tells them apart, and this walk never ran it, so its output
is yours to record, not ours to print); "completing onboarding again fixes it" (it clears cause one and
restores nothing of cause two); and the tautology, "a fresh install has no data", which explains the note by
assuming the answer.

**Why the two causes matter more than either.** One is a state machine you can walk out of; the other is a
policy in one manifest attribute that no amount of tapping undoes. Confuse them and you get the classic
wasted afternoon: debugging the wrong layer while the answer sits in a file nobody opened. The skill this kata
buys is not Android's backup rules — it is refusing one cause for two observations until a command separates
them.

verified 2026-10-12 in the author's tree — `118` dirty paths, `70` staged, `app/src` tip `b781cbb` — every
citation re-run read-only since, at the same two counts. Nothing is pinned to `HEAD`, which the book's own
commits keep moving: `git log --oneline b781cbb..HEAD -- app/src` prints two `docs(book)` commits touching
only `app/src/main/java/com/neuroflow/app/lab/README.md`, and any *code or Gradle* file in that output makes
the tip half of every pair above stale.
