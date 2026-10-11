# Design spec — *Learning Android by Dissecting NeuroFlow*

Status: approved in conversation 2026-10-11 (spine, anatomy, chapter map, binding).
Author: Qoder agent + takian-alt. Scope: a full programming book, ~100k words, 22 chapters numbered 0-21.
Self-review 2026-10-11: all cited paths, line numbers, sizes and counts re-verified against the working tree (see §9).

---

## 1. The bet

Most codebase-teaching books fail the same way: they tour the code, the reader follows along, and the reader cannot do it alone afterwards. This book inverts the order. **The reader's own code fails in a way they can feel, they invent the fix, and only then do they meet the production version of the idea.** Reading NeuroFlow's source is the review step, never the introduction.

The case study is unusually good for this: NeuroFlow (this repo, `com.neuroflow.app`, 208 main files / 57,450 LOC) contains real algorithms, real hand-written SQL migrations with real data loss, and real architectural mistakes. A beginner can be taken from `fun main` to critiquing a 2,438-line bin-packing scheduler without ever leaving a working app.

## 2. Reader (locked by Q2 = B2)

Can program in another language (Python / JS / Java). Has never written Kotlin, has never opened Android Studio, has never shipped a feature.

- No chapters on "what is a loop / what is a function". Programming concepts are introduced only as *Kotlin syntax deltas* ("in Kotlin this is `val`, and here is why the compiler forces you to say it").
- Every Android-specific idea (Activity, process death, permission grant, recomposition) is assumed **novel** and taught from scratch.
- "True beginner" escape hatches: a boxed `If this is your first language` note wherever a chapter leans on a general programming idea, pointing at a 10-line explanation. Never a gate, never a prerequisite chapter.

## 3. Scope (locked by Q3 = S1)

**In:** the productivity app — 24 navigation routes in `presentation/common/NeuroFlowApp.kt`; the Room graph (`tasks`, `notes`, `time_sessions`, `goals`, `woop_data`, `ulysses_contracts`, `sleep_logs`, `energy_predictions`); `data/repository/*`; `domain/engine/*` (11 files); `domain/scheduler/*` (6 files); `domain/repository/*` (4 files); `worker/*`; `receiver/*`; DataStore; Firebase backup/sync; the kotest suite.

**Out:** the launcher (`presentation/launcher/`, 62 files / 19,293 LOC), HyperFocus, `kiosk/DeviceOwnerKioskManager`, widgets, `NotificationBadgeService`, `LauncherSettings.kt` (2,616 lines). These appear exactly twice: once as "there is a second app in this APK, here is the boundary", and once in Ch 19 as a map the reader must not get lost in.

Because dropping the launcher removes the natural "walk into 19k unfamiliar lines" exercise, Ch 19 restores it against `data/sync/FirestoreBackupRepository.kt` (822 lines) — cold, real, untaught elsewhere in the book, and containing a genuine trap (only 31 of the 117 preference fields round-trip through backup).

## 4. Substrate (locked by Q4 = W1 base, all three folded in)

- **W1 — the reader hacks PROCUS itself.** Chapter 1 sets up a protected branch (`book/progress`) and teaches `git restore` as a self-rescue, so breaking things is recoverable and cheap. Reading and perturbing production files is the default activity.
- **W2 — `app/src/main/java/com/neuroflow/app/lab/`** is the reader's fenced package, grown from empty. It is *additive only*: the reader's own `LabNote`, `LabNoteDao`, `LabScheduler`. Nothing in the app imports it. It compiles as part of `:app` (that is the point — the reader's code is real code), but it declares its own `LabDatabase` and is **never** registered into `NeuroFlowDatabase`, so a beginner's half-finished schema cannot corrupt the app's 20-version one. This gives the "I built it from nothing" experience without endangering the app, i.e. W2 inside W1's repo. Ch 0 ships a `lab/README.md` stating those rules.
- **W3 — perturbations on production code**: "predict, then change, then observe". Used for the concepts that cannot be faked: recomposition, migration behaviour, WorkManager scheduling, permission denial.

**The loop budget decides the medium.** Most verification runs `./gradlew :app:testDebugUnitTest --tests "com.neuroflow.app.lab.*"` — kotest on the JVM, no emulator, seconds. Compose Preview (`@Preview`, interactive mode) covers most UI. The emulator appears only where nothing else can prove the point: Ch 1 (first run), Ch 13 (usage-stats grant), Ch 14 (WorkManager deferral), Ch 19 (backup round-trip). A beginner on a laptop should not wait 4 minutes to learn what a `data class` is.

## 5. Spine (locked at design approval = Problem ladder)

Rejected alternatives, recorded so nobody re-litigates them: *layer tour* (`data/` → `domain/` → `presentation/`, which is what the repo's own five guides do, and which means six chapters before the reader sees anything run); *feature ladder* (vertical slices in order of convenience, where each abstraction merely appears because the author chose it).

The chain of failures that drives the order — each link is demonstrable in code that exists today:

| Ch | The reader's code fails because | Concept it forces |
|---|---|---|
| 3 | the screen forgets everything on process death | persistence |
| 4 | the list is stale after an insert | reactive data (Flow) |
| 5 | the subscription dies with the Activity | ViewModel + `stateIn` |
| 6 | hand-wiring 4 constructors per screen | DI (Hilt) |
| 7 | `notes` was easy; `tasks` has 53 fields | schema/enum mapping, cost of a fat table |
| 8 | business rules live in the data layer | layering as a *correction*, not a rule |
| 9 | naive scorer recomputes every pair, `O(n²)` | precomputed context (`ScoringContext`) |
| 10 | whole matrix re-renders on one keystroke | recomposition, state hoisting |
| 11 | toy scheduler double-books slots and moves pinned tasks | capacity horizon, hysteresis, placement stability |
| 12 | scheduler treats all hours as equal | circadian + sleep-pressure energy model |
| 13 | energy model is guesswork | `UsageStatsManager`, runtime permissions |
| 14 | nothing happens while the app is closed | WorkManager, boot, main-thread safety |
| 15 | date arithmetic breaks the week DST changes | local-day boundaries, instants vs walls |
| 16 | tests you wrote may never have run | test platform mechanics, proof of execution |
| 17 | mocked DAO means no SQL was ever executed | in-memory Room, test doubles that are real |
| 18 | a schema change wipes user notes | migrations, `AutoMigration`, restore-version policy |
| 19 | backup silently loses 86 of 117 preferences | cold-code reconnaissance, round-trip proofs |
| 20 | the docs describe an architecture the code violates | judgment: reviewing a design you did not write |

## 6. Chapter anatomy — fixed, all 22 chapters

1. **Predict first.** Real code, one question, one line for the answer. Explanation is withheld until the answer is written. This is the core habit; Ch 0 spends a full page making the contract explicit ("if you skip the prediction you get a book you read, not a skill you have").
2. **The Gap.** Why last chapter's code fails.
3. **Build it in `lab/`.** Smallest step that runs. Reader types, does not copy-paste.
4. **Meet the grown-up version.** Open the production file, diff against theirs, *count* the gap in lines and concerns.
5. **Perturb to verify.** Break or change production code deliberately; predict; observe.
6. **Kata** — three tiers, always: **Recall** (predict output), **Modify** (make it do X), **Diagnose** (given broken code, find the bug). Solutions in a separate tree, never inline.
7. **Interleaved review.** Five questions drawn from chapters −2, −5, −10, so nothing decays.
8. **Exit ticket.** One artifact the reader produced plus one sentence of self-explanation (Feynman check).

Hard rules: no code block longer than 25 lines without the reader having typed part of it; no production file introduced before the reader has needed it; every "run this" has a stated expected output.

## 7. Chapter list

### Part 0 — Getting a loop running (Ch 0-2)

- **Ch 0 — How to read this book.** The contract (predict before reading), the branch ritual, `lab/` rules, the loop budget, the toolchain (JDK 21, SDK at `~/Android/Sdk`). *Artifact: a `predictions.md` file with the first three answers written before any explanation.*
- **Ch 1 — Make it run.** Clone, sync, build, install, launch. What Gradle actually does, what `minSdk 26 / compileSdk 35` commits you to, why Firebase needs `google-services.json`, and why some screens behave differently on a fresh install. Reading: `app/build.gradle.kts`, `AndroidManifest.xml`, `NeuroFlowApplication.kt` (211 lines) at arm's length — *what runs at launch*, not *how it works*. *Artifact: a log line printed by the reader's code, visible in Logcat.* Kata (Diagnose): the app crashes on a fresh install — find `last_crash.txt`, which the global `Thread.setDefaultUncaughtExceptionHandler` in `onCreate` writes, before learning to read a stack trace.
- **Ch 2 — Kotlin for people who already code.** `val`/nullability/`?.`/`?:`, `data class`, sealed classes, enums, extension functions, `when` exhaustiveness, `object` vs companion. Taught against real files: `domain/model/Enums.kt` (77 lines, 10 enums in one file — a discussion about why that's allowed and when it hurts) and `data/local/entity/TaskEntity.kt` (103 lines, **53** fields). *Artifact: `lab/LabTask.kt` + 8 kotest assertions that pass.* Kata (Recall): five nullability traps drawn from the repo, predict which compile.

### Part 1 — One complete vertical slice (Ch 3-6)

- **Ch 3 — A screen that exists.** Composables, `Column`/`Row`/`Text`/`Button`, modifiers, the Preview window, and the one thing a beginner must internalise: composable functions are *re-run*, not *called*. *Artifact: `lab/LabNotesScreen.kt` rendering a hard-coded list.* Then read `presentation/notes/NotesScreen.kt` (330) for shape only.
- **Ch 4 — Where data lives.** Room: `@Entity`, `@PrimaryKey`, `@Dao`, `@Query`, `@Insert`, type converters. Reader hand-builds `LabNoteEntity` + `LabNoteDao` and sees rows in Database Inspector. Then compare with `data/local/entity/NoteEntity.kt` (18) and `data/local/dao/NoteDao.kt` (38), including its `ORDER BY isPinned DESC, updatedAt DESC` and `LIKE '%' || :query || '%'`. *The Gap: the UI list does not refresh after an insert.*
- **Ch 5 — Reactive data.** Coroutines (`suspend`, `launch`, scopes), `Flow` vs `StateFlow`, `flatMapLatest`, `combine`, `stateIn(WhileSubscribed(5000))`, dispatchers, and why `Dispatchers.IO` is not a magic word. Taught by making the reader's list update itself, then reading `presentation/notes/NotesViewModel.kt` (125), whose single `combine(...)` expression is the best compact teaching sample in the repo. *The Gap: configuration change kills the subscription.*
- **Ch 6 — Wiring.** ViewModel as state owner, `hiltViewModel()`, the `@HiltViewModel @Inject constructor` pattern, `@Module`/`@Provides`/`@Singleton`, the 3 real modules (`di/AppModule.kt` 135 lines / 15 `@Provides`, `di/SyncModule.kt`, `presentation/launcher/di/LauncherModule.kt`), navigation with 24 routes, and the `MainActivity` (309) launch gate: `prefs == null` → onboarding → FreshStart → `NeuroFlowApp`. *Artifact: the reader's `lab/` notes feature works end-to-end — persisted, injected, navigable.* This is the book's first real milestone, and it lands before chapter 7 of 22.

### Part 2 — The app's actual subject: tasks (Ch 7-11)

- **Ch 7 — A table with 53 columns.** Entity vs domain model, `Converters.kt` enum-as-string, why `tasks` has no `@ForeignKey` and no `@Index`, nullable-as-business-state. *Kata (Diagnose): how many of the 53 fields are never read by any UI?*
- **Ch 8 — Reading a big class without drowning.** `data/repository/TaskRepository.kt` (353 lines, 25 `fun`/`suspend fun` declarations): recurrence rollover, `completeAndRecur`, `splitTaskIntoSessions`, `postponeTask`. Technique taught: build a method map, then trace one verb down to the DAO. *The Gap (real, verifiable): this data-layer file imports `presentation.launcher.hyperfocus.domain.HyperFocusManager` and `...launcher.work.ScheduleAutoTasksWorker` at lines 13-14 — the book's first deliberate architecture argument.*
- **Ch 9 — Priority is not a number.** `domain/engine/TaskScoringEngine.kt` (1,163 lines): additive weighted scoring, hard blocks first, `scoreBreakdown` explainability, weights living in `UserPreferences` so users retune them in `PriorityWeightsScreen`. Reader writes a naive scorer for 20 tasks, then reads `ScoringContext` (line 98: `class ScoringContext private constructor(...)` + `companion.from(tasks)` precomputed indices) and discovers *why* it exists.
- **Ch 10 — The Eisenhower Matrix, as a refactoring target.** `presentation/matrix/MatrixScreen.kt` (982) is not toured, it is *judged*. State hoisting, `Lazy` grids, quadrant colours in `theme/Color.kt` (`NeuroFlowColors`), the `Screen.Quadrant` route. *Artifact: extract one component and measure recomposition.*
- **Ch 11 — The scheduler (technical peak).** In `lab/`: a toy scheduler over `List<Slot>` that fails three ways — double-books, ignores breaks, moves a task the user placed by hand. Then the real one in stages: `AutoSchedulingEngine.planAutoSchedule` (declared at line 164), `calculateCapacityHorizon`, `EnergyProfile.from` zone hysteresis (re-entry ≥77 vs entry ≥80, with the comment naming the shared-state race it fixed), `TaskSlotFitScore : Comparable`, the ~12 weighted terms of `scoreTaskSlotFit`, the utilisation caps (0.70 / 0.50 in CRITICAL / 0.85), the urgency bypass, `reserveBreakSlotsAfterTask`, and `PlacementStability.kt` (109) + `AutoSchedulingContracts.kt` (62) as safety gates. *Exit: the reader lists five constraints the toy version violated that they had not thought of.* Companion tests read next — six `AutoSchedulingEngine*Test` files splitting a 2,438-line class by concern.

### Part 3 — Time, energy, measurement (Ch 12-15)

- **Ch 12 — The energy model.** `MEQChronotypeDetector.kt` (87) → `PeakEnergyEngine.kt` (840, `DetectionResult.currentValueAt` decaying over a 960-minute wake window) → `SleepPressureDetector.kt` (184, `SOFT_MAX_REFERENCE` → `FatZone`) → `EnergyScoreEngine.kt` (133): `rawEnergy = peakScore − fatiguePenalty ∈ [−100,100]`, `usableEnergy = (rawEnergy+100)/2`, cosine `peakWindow = 0.5·(1−cos(2π·phase))`. The reader works one example by hand on paper first — the only arithmetic the book demands, deliberately.
- **Ch 13 — Getting truth off the device.** `DistractionEngine.kt` (656): `UsageStatsManager`, `DISTRACTION_WEIGHTS`, `SYSTEM_PACKAGE_PREFIXES`, the `-1f` "never measured" sentinel on `tasks.distractionScore`, and `PACKAGE_USAGE_STATS` as a *special access* grant you cannot request with a dialog. Lesson: features that silently degrade when permission is refused, and how to design for that.
- **Ch 14 — Background work and main-thread safety.** WorkManager with Hilt (`Configuration.Provider`, plus the manifest's `tools:node="remove"` of `WorkManagerInitializer`), the worker set, `worker/NotificationWorker.kt` (351 lines holding **four** `*Worker` classes plus `delayUntilHour` — the "one file, many classes" trap), `receiver/BootReceiver.kt` (152), `receiver/NudgeSnoozeReceiver.kt` (61). Then the three honest hazards in `NeuroFlowApplication.onCreate`: `runBlocking` around `enforceRestoreVersionPolicy()` on the main thread, the `while(true) { … delay(...) }` loop in `applicationScope`, and re-enqueuing WorkManager on every `preferencesFlow` emission. StrictMode is enabled in debug — the reader turns it on and catches something real.
- **Ch 15 — Clocks lie.** `data/local/entity/TaskTimeExtensions.kt` (89: `startOfLocalDay`, `effectiveScheduleAnchorMillis`, `localTimeOfDayOffset`), millis-as-instant vs wall time, `wrapMinutes` mod 1440. Taught through the suites that exist to prove it: `AutoSchedulingEngineDstTest.kt` (241) and `AnalyticsEngineDstTest.kt`. *Kata (Diagnose): a task scheduled at 01:30 on a DST Sunday — predict where it lands, then make it true.*

### Part 4 — Trust: how do you know it works (Ch 16-18)

- **Ch 16 — Tests that actually run.** kotest `StringSpec` (56 of the 63 files in `app/src/test`), `shouldBe`, `mockk`, `coVerify`, `coroutinesTest`. The discovery exercise: 6 files (`TimeUtilsTest`, `EnergyInsightTest`, `EnergyProjectionBlendingTest`, `AbstentionRateTrend7dTest`, `AbstentionReasonMixTest`, `SleepPressureRefreshWorkerTest`) carry **27** `@Test` methods, but `app/build.gradle.kts:72` sets `useJUnitPlatform()` with `junit-vintage-engine` absent — so those 27 never execute. The reader proves it by inverting an assertion and watching the suite stay green. Model test for the chapter: `NoteRepositoryTest.kt` (88 lines).
- **Ch 17 — Testing the database nobody tests.** Every DAO in the repo is mocked, so no `@Query` string has ever been executed by a test (`data/local/dao/` has zero test files). The reader deals with the 27 orphans (add the vintage engine, or convert them — tradeoff discussed), then writes the repo's first real in-memory Room test (`Room.inMemoryDatabaseBuilder`) against `LabNoteDao`, and watches a SQL typo fail loudly.
- **Ch 18 — Schema evolution, and the time the app ate user notes.** `data/local/NeuroFlowDatabase.kt`: `version = 20` (line 296), 20 hand-written `object : Migration(...)` — not AutoMigrations — registered in `di/AppModule.kt`, with `fallbackToDestructiveMigrationOnDowngrade()` but *no* plain `fallbackToDestructiveMigration()`, so a missing upgrade path crashes rather than silently wiping. The real incident: `MIGRATION_18_19` and `MIGRATION_17_19` both `DROP TABLE IF EXISTS \`notes\`` (lines 250 and 271), and `MIGRATION_17_19` jumps 17→19 while `17_18`/`18_19` also exist, so behaviour depends on which path Room takes. `app/schemas/com.neuroflow.app.data.local.NeuroFlowDatabase/` contains 1-11 and 15-20 — **12, 13, 14 are missing**, so those versions cannot be diffed. *Artifact: the reader designs a non-destructive path for one of these and writes its migration test.* `DatabaseCleaner.kt` (128) and its install-marker restore guard close the chapter.

### Part 5 — Mastery: judgment (Ch 19-21)

- **Ch 19 — Cold code.** A timed, unassisted reconnaissance of `data/sync/FirestoreBackupRepository.kt` (822 lines): produce one page — what it stores, its document/subcollection shape, what happens on partial failure, what the 500-document chunking protects. Then find the trap in `CloudBackupManager.kt`: `internal fun UserPreferences.toBackupMap()` (line 220) writes **31** entries while `UserPreferences` has **117** fields (class opens line 22, closes 163), so 86 settings do not survive a backup round-trip; the reader writes the failing test that proves it, with `fromBackupMap` (line 264) as the mirror. Also here, for the first time: the boundary map of the second app in the APK (`presentation/launcher/`, 19,293 LOC), read as a *map*, not as code.
- **Ch 20 — What architecture is actually for.** The brochure in `ARCHITECTURE_GUIDE.md` says data flows one way and `domain/repository/` holds interfaces. Verified reality: `TaskRepository` (data) imports from `presentation/` (lines 13-14); `data/local/NeuroFlowDatabase.kt` imports types from `presentation/launcher/hyperfocus/data/`; `domain/repository/` holds 4 concrete `@Singleton class`es and no DI seam — the only `interface` in all of `domain/` is `sealed interface AddSleepLogResult` at `domain/repository/SleepPressureRepository.kt:29`, a result type; `DistractionEngine` and `AutonomyNudgeEngine` import `android.*`; and `scripts/check-launcher-isolation.sh` enforces isolation for `launcher/` only. Two themes and two settings screens exist because this is one APK containing two apps. Final deliverable: a written review that (a) states which violations actually cost something, (b) proposes a prioritised fix plan with a cost each, and (c) defends leaving three of them alone. **Mastery = arguing with the design, not following it.**
- **Ch 21 (capstone) — Ship a feature you designed.** Reader picks one of three briefs, all real gaps found earlier (a preference field that must round-trip through backup; a scheduling constraint the toy scheduler missed; a DAO query with no test) and writes it themselves — entity → DAO → repository → ViewModel → screen → kotest suite → migration if needed — with the book reduced to a checklist. Exit standard: a reviewer who has never seen the book can check out the branch and see green tests and a working screen.

Front/back matter: `docs/book/README.md` (TOC + how to start), glossary, kata answer key, and **Appendix: errata of the repo's existing five guides** — the phantom files (`TaskScheduler.kt`, `EnergyAwareScheduler.kt`, `SleepPressureEngine.kt`, `domain/model/Task.kt`, `AddTaskScreen.kt`, `presentation/common/components/`, `TaskSplitter`, `PeakEnergyDetector`) and the wrong numbers (`README.md:75` "10 migrations, schema v11" vs actual v20 / 20 migrations; `README.md:15` "v5.0.0 (latest)" linking a `version_4.0.0` tag while `app/build.gradle.kts:27-28` says `versionCode = 11`, `versionName = "10.5"`; `ARCHITECTURE_GUIDE.md` describing `domain/repository/` as interfaces). Purpose: train the reader to trust code over documentation.

## 8. Deliverable layout (D1 structure, D3 binding)

```
docs/book/
  README.md                     TOC, reading order, the contract
  00-how-to-read-this-book.md ... 21-capstone-ship-it.md
  appendix-glossary.md
  appendix-doc-errata.md
  exercises/kata-NN-*.md        prompts only, no answers
  solutions/kata-NN-*.md        full solutions + why the wrong turns are wrong
  build-book.sh                 concatenates in order -> book.html (+ print CSS)
```

`app/src/main/java/com/neuroflow/app/lab/README.md` documents the fenced package rules. The book's own files live only under `docs/book/` and that one `lab/` package; production code is never modified or merged by the author. Two chapters deliberately have the *reader* change more on their branch — Ch 17 (`app/build.gradle.kts` test dependencies) and Ch 18 (a migration) — and both are exercises, not repo changes. The working tree's ~50 uncommitted files are untouched.

Binding: no `pandoc` on this machine (verified), `pdflatex`/`xelatex` present. Plan is (b) then (a): emit one self-contained styled HTML with a print stylesheet via `build-book.sh` (zero new dependencies, readable in a browser on day one, printable to PDF), then a real EPUB/PDF via pandoc if the user opts to `sudo dnf install pandoc`. **System packages will not be installed without an explicit yes.**

## 9. Verification policy (what makes this book trustworthy)

1. **Nothing is cited from the repo's markdown guides.** Every path is `find`-confirmed; every line number and count is `grep`/`wc`-confirmed at writing time; every quote is pasted from the source file, not from memory.
2. **Every kata is executed.** Prompt runs must fail as described; solution runs must pass, via `./gradlew :app:testDebugUnitTest --tests "com.neuroflow.app.lab.*"` and the named production suites. UI claims checked in Compose Preview, or on the emulator where the chapter promises one.
3. **Numbers drift; claims rot.** Each chapter carries a `verified against` line (date + `git rev-parse --short HEAD`) so a future reader knows how stale it is.
4. **No dangling exercises.** A kata ships only with a solution file and a recorded expected output.
5. If a check cannot be run in this environment, the chapter says so in those words rather than implying the reader's run is guaranteed.

## 10. Out of scope

The launcher / HyperFocus / kiosk as taught systems; XML-Views-era Android; iOS or Compose Multiplatform; Firebase console and Play Store release process; any refactor of production code as a side effect of writing chapters (a fix a beginner would make belongs in an exercise, run on their branch, not merged by the author); the app's *product* methodology — that is a different book, and `ENERGY_PRODUCTIVITY_AUDIT.md` already covers it.

## 11. Risks

- **Length creep.** 22 chapters at 4-6k words is ~100k. Mitigation: the anatomy is fixed, and a chapter exceeding ~6k words splits rather than bloats.
- **Gradle turnaround during writing.** Building to verify claims may take minutes per run. Mitigation: JVM-test-first design; batch verification per chapter, not per snippet.
- **`lab/` could break the app build** (a stray Room entity needing registration, an unused import failing lint). Mitigation: `lab/` owns a separate `LabDatabase`, never touches `NeuroFlowDatabase`, and is compiled with the app so failures are visible immediately rather than at chapter 20.
- **Beginner + emulator + dirty tree = self-inflicted confusion.** Mitigation: the Ch 1 branch ritual, and every chapter states which files it permits the reader to touch.
- **Sensitive file.** `app/google-services.json` is committed with a live project id, OAuth client and API key. The book will not reproduce its contents; Ch 1 treats it as a deliberate sidebar on what is and isn't secret in a Firebase config, rather than an accident.

## 12. Success criteria

A reader who finishes can: (1) build and run this app from a fresh clone; (2) trace a user action from a Compose click to a Room write and back to a redrawn list, unprompted; (3) write and pass their own kotest suites, including real in-memory Room tests; (4) read a 2,400-line algorithm and enumerate its constraints from memory; (5) deliver a branch with a working feature and green tests; (6) name three architectural defects in this codebase and argue the cost of fixing each. (6) is what separates this from a tutorial.

## 13. Next step

On approval of this spec: `superpowers:writing-plans` produces the chapter-by-chapter implementation plan, sequenced so each chapter is written → verified → kata-executed before the next, with Part 0-1 first as the highest-risk pedagogical bet — if the Problem ladder does not land by Ch 6, the structure gets revisited before another 15 chapters are written.
