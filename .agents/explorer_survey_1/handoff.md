# Core Engineering & Math Pipeline Survey & Reconnaissance Report (R1)

## 1. Observation

### 1.1 Gradle Build & Test Runner Configuration
- **File**: `app/build.gradle.kts`
  - Lines 70–77:
    ```kotlin
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            all {
                it.useJUnitPlatform()
            }
        }
    }
    ```
  - Lines 144–150:
    ```kotlin
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.9.1")
    testImplementation("io.kotest:kotest-property-jvm:5.9.1")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.work:work-testing:2.10.0")
    ```
- **Test execution observation**:
  - Running `./gradlew testDebugUnitTest` discovered Kotest tests via `io.kotest.runner.junit5.KotestJvmEngine`.
  - Inspection of `app/build/test-results/testDebugUnitTest/` confirms that pure JUnit 4 test suites and JUnit 4 / Robolectric tests are **completely omitted** from test results:
    1. `com.neuroflow.app.presentation.analytics.AbstentionRateTrend7dTest` (`app/src/test/java/com/neuroflow/app/presentation/analytics/AbstentionRateTrend7dTest.kt`: lines 10–13, using `org.junit.Test`)
    2. `com.neuroflow.app.presentation.analytics.AbstentionReasonMixTest` (`app/src/test/java/com/neuroflow/app/presentation/analytics/AbstentionReasonMixTest.kt`: lines 7–10, using `org.junit.Test`)
    3. `com.neuroflow.app.presentation.common.EnergyInsightTest` (`app/src/test/java/com/neuroflow/app/presentation/common/EnergyInsightTest.kt`: lines 8–10, using `org.junit.Test`)
    4. `com.neuroflow.app.presentation.common.TimeUtilsTest` (`app/src/test/java/com/neuroflow/app/presentation/common/TimeUtilsTest.kt`: lines 7–9, using `org.junit.Test`)
    5. `com.neuroflow.app.worker.SleepPressureRefreshWorkerTest` (`app/src/test/java/com/neuroflow/app/worker/SleepPressureRefreshWorkerTest.kt`: lines 20–23, using `@RunWith(RobolectricTestRunner::class)` and `org.junit.Test`)
- **Dependency inspection**:
  - Running `./gradlew app:dependencies --configuration debugUnitTestRuntimeClasspath` revealed:
    `org.junit:junit-bom:5.8.2` is pulled transitively by Kotest, but `org.junit.vintage:junit-vintage-engine` is completely absent from the runtime classpath.

---

### 1.2 Math & Energy Engines (Borbély Two-Process & Peak Energy)
#### A. Pre-Peak Hour Zeroing Bug
- **File**: `app/src/main/java/com/neuroflow/app/domain/engine/PeakEnergyEngine.kt`
  - Lines 143–162:
    ```kotlin
    fun currentValueAt(nowMillis: Long = System.currentTimeMillis()): Int {
        val nowMinuteOfDay = minuteOfDay(nowMillis)
        val minutesSincePeak = minutesSincePeak(nowMinuteOfDay)
        val decay = (minutesSincePeak * (peakValue.toFloat() / 960f)).roundToInt()
        return (peakValue - decay).coerceAtLeast(0)
    }

    private fun minutesSincePeak(nowMinuteOfDay: Int): Int {
        return if (nowMinuteOfDay >= peakMinuteOfDay) {
            nowMinuteOfDay - peakMinuteOfDay
        } else {
            nowMinuteOfDay + (24 * 60) - peakMinuteOfDay
        }
    }
    ```
- **File**: `app/src/main/java/com/neuroflow/app/domain/engine/EnergyScoreEngine.kt`
  - Lines 50–54:
    ```kotlin
    val reservoirFactor = if (peak != null) {
        (peak.currentValueAt(snapshot.nowMillis).toFloat() / peak.peakValue.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    ```
  - Lines 75–79:
    ```kotlin
    val peakScore = (PERCENT_SCALE * reservoirFactor * circadianFactor * confidenceFactor)
        .coerceIn(0f, PERCENT_SCALE)
    val fatiguePenalty = (PERCENT_SCALE * sleepPressureRatio).coerceIn(0f, PERCENT_SCALE)
    val rawEnergy = (peakScore - fatiguePenalty).coerceIn(-PERCENT_SCALE, PERCENT_SCALE)
    val usableEnergy = ((rawEnergy + PERCENT_SCALE) / 2f).coerceIn(0f, PERCENT_SCALE)
    ```
- **Observed Behavior**:
  - When a user wakes at 7:00 AM (`nowMinuteOfDay = 420`) and their peak is at 9:30 AM (`peakMinuteOfDay = 570`), `nowMinuteOfDay < peakMinuteOfDay`.
  - `minutesSincePeak = 420 + 1440 - 570 = 1290` minutes (over 21.5 hours).
  - With a 16-hour wake window (`960` minutes), decay is `(1290 * 4000 / 960) = 5375`.
  - `peakValue - decay = 4000 - 5375 = -1375`, which is clamped to `0`.
  - `reservoirFactor` becomes `0.0f`, making `peakScore = 0.0f`.
  - Result: The user wakes up completely rested, but their baseline raw energy is negative (`-fatiguePenalty`) and usable energy drops to `(100 - fatiguePenalty)/2 <= 50%` for all pre-peak morning hours.

#### B. Circadian Midnight Wrap for Evening Chronotypes
- **File**: `app/src/main/java/com/neuroflow/app/domain/engine/EnergyScoreEngine.kt`
  - Lines 114–127:
    ```kotlin
    private fun peakWindow(
        minute: Int,
        startMinute: Int,
        durationMinutes: Int,
        amplitude: Float
    ): Float {
        if (durationMinutes <= 0) return 0f
        val endMinute = startMinute + durationMinutes
        if (minute < startMinute || minute >= endMinute) return 0f

        val phase = (minute - startMinute).toFloat() / durationMinutes.toFloat()
        val raw = 0.5f * (1f - cos((2.0 * PI * phase).toFloat()))
        return (amplitude * raw).coerceAtLeast(0f)
    }
    ```
- **File**: `app/src/main/java/com/neuroflow/app/domain/engine/PeakEnergyEngine.kt`
  - Lines 250–257 (Evening chronotype default profile):
    ```kotlin
    MEQChronotypeDetector.Chronotype.MODERATE_EVENING,
    MEQChronotypeDetector.Chronotype.DEFINITE_EVENING -> CircadianProfile(
        windows = listOf(
            PeakWindow(startMinuteOffset = 0, durationMinutes = 180, amplitude = 1.0f),
            PeakWindow(startMinuteOffset = 330, durationMinutes = 240, amplitude = 0.85f),
            PeakWindow(startMinuteOffset = 600, durationMinutes = 60, amplitude = 0.6f)
        )
    )
    ```
- **Observed Behavior**:
  - When an evening chronotype wakes late (e.g. 11:00 AM / 660m), their anchor is at 16:30 (990m). Window 2 (offset 330m) runs from 22:00 to 02:00 (`startMinute = 1320`, `durationMinutes = 240`, `endMinute = 1560`).
  - At 00:30 (minute 30), `minute (30) < startMinute (1320)`, so `peakWindow` returns `0f`.
  - The window abruptly terminates at 00:00:00 midnight rather than smoothly wrapping across the midnight boundary.

#### C. Confidence Factor Coupled into Baseline Energy Magnitude
- **File**: `app/src/main/java/com/neuroflow/app/domain/engine/EnergyScoreEngine.kt`
  - Line 69:
    ```kotlin
    val confidenceFactor = peak?.confidence?.coerceIn(0f, 1f) ?: 0f
    ```
  - Line 75:
    ```kotlin
    val peakScore = (PERCENT_SCALE * reservoirFactor * circadianFactor * confidenceFactor)
        .coerceIn(0f, PERCENT_SCALE)
    ```
- **Observed Behavior**:
  - `confidenceFactor` directly scales the user's physiological energy level.
  - On a fresh install or cold start with zero sleep logs, `confidence` is ~0.20–0.50 (statistical sample uncertainty).
  - This uncertainty reduces `peakScore` by 50% to 80%, artificially punishing the user with an "exhausted" usable energy rating simply because the app lacks historical data.

#### D. Live Energy State Disconnected from Task Scoring & Scheduling
- **File**: `app/src/main/java/com/neuroflow/app/domain/engine/TaskScoringEngine.kt`
  - Lines 438–443:
    ```kotlin
    fun score(
        task: TaskEntity,
        prefs: UserPreferences,
        context: ScoringContext = ScoringContext.EMPTY,
        nowMillis: Long = System.currentTimeMillis()
    ): Float
    ```
  - Lines 460–462:
    ```kotlin
    val isPeakHour = peakTierScale > 0f
    val isMorning = hour < peakContext.primaryStartHour
    val isLowEnergySlot = hour in 13..15
    ```
  - Lines 561–583:
    ```kotlin
    // ── 8. ENERGY MATCHING (Cognitive Load Theory) ───────────────────────
    val energyBonus = when {
        isPeakHour -> when (task.energyLevel) {
            EnergyLevel.HIGH   ->  70f * peakTierScale
            EnergyLevel.MEDIUM ->  20f * peakTierScale
            EnergyLevel.LOW    -> -35f * peakTierScale
        }
        isLowEnergySlot -> when (task.energyLevel) {
            EnergyLevel.LOW    ->  70f
            EnergyLevel.MEDIUM ->  15f
            EnergyLevel.HIGH   -> -35f
        }
        isMorning -> when (task.energyLevel) {
            EnergyLevel.HIGH   ->  30f
            EnergyLevel.MEDIUM ->  10f
            EnergyLevel.LOW    ->   0f
        }
        else -> when (task.energyLevel) {
            EnergyLevel.MEDIUM ->  10f
            else               ->   0f
        }
    }
    ```
- **File**: `app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingEngine.kt`
  - Lines 1708:
    ```kotlin
    val baseScore = TaskScoringEngine.score(task, prefs, context, nowMillis)
    ```
- **File**: `app/src/main/java/com/neuroflow/app/presentation/today/TodayViewModel.kt`
  - Line 272:
    ```kotlin
    task to TaskScoringEngine.score(task, prefs, visibleActive, now)
    ```
- **Observed Behavior**:
  - `TaskScoringEngine.score` relies entirely on wall-clock approximations: `hour in 13..15` as low energy, and `isPeakHour` based on static schedule windows.
  - The live calculated energy state (`usableEnergy`, `fatiguePenalty`, `sleepPressurePoints`) from `EnergyScoreEngine` or `EnergyScoreRepository` is never provided to `TaskScoringEngine`.
  - When a user is depleted or fatigued at 10 AM, high-energy analytical tasks still receive the full +70pt boost; when full of energy at 2 PM, high-energy tasks receive a -35pt penalty.

---

### 1.3 Database & Room Persistence
#### A. Unknown Enum Deserialization Crash Vulnerability
- **File**: `app/src/main/java/com/neuroflow/app/data/local/Converters.kt`
  - Lines 14–39:
    ```kotlin
    @TypeConverter fun fromQuadrant(value: Quadrant): String = value.name
    @TypeConverter fun toQuadrant(value: String): Quadrant = Quadrant.valueOf(value)

    @TypeConverter fun fromPriority(value: Priority): String = value.name
    @TypeConverter fun toPriority(value: String): Priority = Priority.valueOf(value)

    @TypeConverter fun fromTaskStatus(value: TaskStatus): String = value.name
    @TypeConverter fun toTaskStatus(value: String): TaskStatus = TaskStatus.valueOf(value)

    @TypeConverter fun fromRecurrence(value: Recurrence): String = value.name
    @TypeConverter fun toRecurrence(value: String): Recurrence = Recurrence.valueOf(value)

    @TypeConverter fun fromEnergyLevel(value: EnergyLevel): String = value.name
    @TypeConverter fun toEnergyLevel(value: String): EnergyLevel = EnergyLevel.valueOf(value)

    @TypeConverter fun fromTaskType(value: TaskType): String = value.name
    @TypeConverter fun toTaskType(value: String): TaskType =
        try { TaskType.valueOf(value) } catch (_: Exception) { TaskType.ANALYTICAL }

    @TypeConverter fun fromContractOutcome(value: ContractOutcome?): String? = value?.name
    @TypeConverter fun toContractOutcome(value: String?): ContractOutcome? =
        value?.let { ContractOutcome.valueOf(it) }

    @TypeConverter fun fromRewardTier(value: RewardTier): String = value.name
    @TypeConverter fun toRewardTier(value: String): RewardTier = RewardTier.valueOf(value)
    ```
- **Observed Behavior**:
  - Only `toTaskType` handles invalid or unknown strings gracefully.
  - `toQuadrant`, `toPriority`, `toTaskStatus`, `toRecurrence`, `toEnergyLevel`, `toContractOutcome`, and `toRewardTier` call `valueOf` unconditionally.
  - If a corrupted string, legacy value, or unsupported enum name is read from SQLite, Room throws `IllegalArgumentException` during SQLite cursor processing, terminating the query with an uncaught exception.

#### B. Database Migrations & User Notes Preservation
- **File**: `app/src/main/java/com/neuroflow/app/data/local/NeuroFlowDatabase.kt`
  - Lines 143–161 (`MIGRATION_11_12`):
    ```kotlin
    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS sleep_logs (
                    id TEXT NOT NULL PRIMARY KEY,
                    startAt INTEGER NOT NULL,
                    endAt INTEGER NOT NULL,
                    durationMinutes INTEGER NOT NULL,
                    source TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sleep_logs_startAt ON sleep_logs(startAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sleep_logs_endAt ON sleep_logs(endAt)")
        }
    }
    ```
  - Lines 248–288 (`MIGRATION_18_19` & `MIGRATION_17_19`):
    ```kotlin
    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS index_notes_updatedAt")
            db.execSQL("DROP INDEX IF EXISTS index_notes_isPinned")
            db.execSQL("DROP TABLE IF EXISTS `notes`")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `notes` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `colorHex` TEXT NOT NULL,
                    `isPinned` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `tags` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
        }
    }
    ```
- **Schema File Directory**: `app/schemas/com.neuroflow.app.data.local.NeuroFlowDatabase/`
  - Present schemas: `1.json` through `11.json`, then directly jumps to `15.json` through `21.json`.
  - **Missing schemas**: `12.json`, `13.json`, and `14.json` are absent from disk.
- **Observed Behavior**:
  1. Because `12.json` is missing from the schemas directory, Room's `MigrationTestHelper` cannot independently validate `MIGRATION_11_12`.
  2. `MIGRATION_18_19` and `MIGRATION_17_19` contain `DROP TABLE IF EXISTS notes`. If a user had created notes in version 18, upgrading directly drops the table and loses all user notes.

---

### 1.4 Application Startup Blocking
- **File**: `app/src/main/java/com/neuroflow/app/NeuroFlowApplication.kt`
  - Lines 74–93:
    ```kotlin
    if (BuildConfig.DEBUG) {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
    }
    enforceRestoreVersionPolicy()
    ```
  - Lines 125–140:
    ```kotlin
    private fun enforceRestoreVersionPolicy() {
        val firstLaunchAfterInstall = markFirstLaunchAfterInstall()
        runBlocking {
            databaseCleaner.enforceRestoreVersionPolicy(firstLaunchAfterInstall)
        }
    }

    private fun markFirstLaunchAfterInstall(): Boolean {
        val marker = File(noBackupFilesDir, INSTALL_MARKER_FILE)
        if (marker.exists()) return false
        return runCatching {
            marker.parentFile?.mkdirs()
            marker.writeText("1")
            true
        }.getOrDefault(true)
    }
    ```
- **File**: `app/src/main/java/com/neuroflow/app/data/local/DatabaseCleaner.kt`
  - Lines 84–103:
    ```kotlin
    suspend fun enforceRestoreVersionPolicy(firstLaunchAfterInstall: Boolean): Boolean = withContext(Dispatchers.IO) {
        val currentVersion = currentAppVersionCode()
        val recordedVersion = readRecordedBackupVersionCode()
        val shouldPurge = firstLaunchAfterInstall &&
            recordedVersion > 0L &&
            recordedVersion != currentVersion

        if (shouldPurge) {
            database.clearAllTables()
            preferencesDataStore.clearAll()
            requestBackupRefresh()
        }

        writeRecordedBackupVersionCode(currentVersion)
        shouldPurge
    }
    ```
  - Lines 113–117:
    ```kotlin
    private fun writeRecordedBackupVersionCode(versionCode: Long) {
        backupState.edit()
            .putLong(BACKUP_APP_VERSION_CODE, versionCode)
            .commit()
    }
    ```
- **Observed Behavior**:
  - `markFirstLaunchAfterInstall()` executes synchronous file checks and file writes (`marker.exists()`, `marker.writeText("1")`) on the UI thread during cold start.
  - `enforceRestoreVersionPolicy()` invokes `runBlocking` on the main thread, blocking the UI thread until `DatabaseCleaner` finishes.
  - `DatabaseCleaner` executes `backupState.edit().commit()` (synchronous disk write) and conditionally calls `database.clearAllTables()`.
  - In debug builds, StrictMode detects these disk reads and disk writes on the UI thread, outputting StrictMode violation warnings in logcat and slowing cold boot.

---

## 2. Logic Chain

1. **Test Runner Engine**:
   - `app/build.gradle.kts` sets `unitTests.all { it.useJUnitPlatform() }`.
   - JUnit Platform requires specific `TestEngine` implementations on the test runtime classpath to discover test classes.
   - Kotest provides `io.kotest.runner.junit5.KotestJvmEngine`. JUnit Jupiter provides `org.junit.jupiter.engine.JupiterTestEngine`.
   - JUnit 4 tests (using `@org.junit.Test`, `@RunWith(RobolectricTestRunner::class)`) require `org.junit.vintage:junit-vintage-engine`.
   - Because `junit-vintage-engine` is absent from dependencies, the JUnit Platform runner ignores JUnit 4 classes.
   - Therefore, 5 test suites were never executed during `./gradlew testDebugUnitTest`. Adding `testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.8.2")` makes JUnit Platform execute both Kotest and JUnit 4 / Robolectric suites.

2. **Pre-Peak Baseline Decay**:
   - `PeakEnergyEngine.currentValueAt` calculates minutes since peak.
   - When `nowMinuteOfDay < peakMinuteOfDay`, it assumes the peak occurred yesterday, calculating `nowMinuteOfDay + 1440 - peakMinuteOfDay`.
   - Since typical morning hours (e.g. 7:00 AM) are 21.5+ hours after yesterday's peak, the elapsed minutes exceed the 16-hour wake window (`960` minutes).
   - Linear decay subtracts more than the entire `peakValue`, clamping to zero.
   - In `EnergyScoreEngine`, `reservoirFactor` becomes 0, zeroing `peakScore`.
   - Therefore, a continuous ramp from wake-time baseline (or circadian curve) is required instead of treating pre-peak hours as elapsed decay from yesterday.

3. **Midnight Window Wrapping**:
   - In `EnergyScoreEngine.peakWindow`, the boundary condition is `if (minute < startMinute || minute >= endMinute) return 0f`.
   - When a circadian window starts before midnight (e.g. 23:00 / 1380m) and ends after midnight (e.g. 02:00 / 120m), `endMinute` is `1560`.
   - After midnight, `minute` resets to `0..120`. Since `minute < startMinute (1380)`, it returns 0.
   - Therefore, calculating circular elapsed minutes `(minute - startMinute + 1440) % 1440` properly preserves the circadian window across midnight.

4. **Confidence vs Magnitude Decoupling**:
   - `EnergyScoreEngine` line 75 defines `peakScore = 100 * reservoirFactor * circadianFactor * confidenceFactor`.
   - Statistical confidence measures sample size and data completeness, not physical fatigue.
   - Scaling energy magnitude down by confidence penalizes new users or low-sample days.
   - Therefore, confidence must be separated from magnitude, acting as a telemetry/uncertainty metric or a blend factor between population priors and personalized adjustments.

5. **Live Energy Integration**:
   - `TaskScoringEngine.score` and `AutoSchedulingEngine.calculateCompositePriorityScore` compute priority scores using hardcoded time intervals (`13..15`) instead of observing the user's actual live energy.
   - Therefore, providing `liveEnergy` (or `ScoreResult`) into `ScoringContext` allows cognitive-load matching to reflect the user's real fatigue state.

6. **Enum Serialization Safety**:
   - Room `@TypeConverter` functions run inside SQLite query execution.
   - When an unexpected string is encountered by `Enum.valueOf(str)`, an uncaught `IllegalArgumentException` is thrown, crashing the active query and UI.
   - Wrapping with safe fallbacks (`try { Enum.valueOf(...) } catch (_: Exception) { DEFAULT }`) hardens Room against corruption or forward-compatibility issues.

7. **Database Migration Integrity**:
   - In `MIGRATION_18_19` and `MIGRATION_17_19`, executing `DROP TABLE IF EXISTS notes` deletes user data.
   - Removing `DROP TABLE` and using non-destructive table/index creation preserves user notes.
   - Generating `12.json` enables testing `MIGRATION_11_12` with `MigrationTestHelper`.

8. **UI Thread Cold Start Optimization**:
   - `NeuroFlowApplication.onCreate()` performs file operations and calls `runBlocking { databaseCleaner.enforceRestoreVersionPolicy(...) }`.
   - This directly blocks the Android UI thread and violates StrictMode.
   - Moving this logic to `applicationScope.launch(Dispatchers.IO)` completely eliminates UI thread disk I/O and `runBlocking`.

---

## 3. Caveats

1. **Robolectric in SleepPressureRefreshWorkerTest**:
   - While `TimeUtilsTest`, `EnergyInsightTest`, `AbstentionRateTrend7dTest`, and `AbstentionReasonMixTest` are standard pure JUnit 4 unit tests, `SleepPressureRefreshWorkerTest` uses `@RunWith(RobolectricTestRunner::class)`.
   - In `SleepPressureRefreshWorkerTest`, `TestListenableWorkerBuilder` currently does not provide a custom `WorkerFactory` for the `@AssistedInject` constructor of `SleepPressureRefreshWorker`. When vintage engine executes it, it may require a test worker factory to pass.
2. **Database Versioning vs Git History**:
   - Current database version in `NeuroFlowDatabase.kt` is `21`, while `app/build.gradle.kts` specifies `versionCode = 11`.
   - Schemas `12.json`, `13.json`, and `14.json` are absent from `app/schemas/`. They can be synthesized from the migrations (`11.json` + `sleep_logs` table for `12.json`) to allow automated test validation.

---

## 4. Conclusion & Concrete Worker Recommendations

### R1.1: Gradle Test Configuration
- **Target File**: `app/build.gradle.kts`
- **Recommended Changes**:
  Add `org.junit.vintage:junit-vintage-engine` under `dependencies`:
  ```kotlin
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.8.2")
  ```
  Ensure `testOptions.unitTests.all { it.useJUnitPlatform() }` remains intact.
  Fix `SleepPressureRefreshWorkerTest.kt` to inject the mock `SleepPressureRepository` using `setWorkerFactory`.

### R1.2: Math & Energy Pipeline Fixes
- **Target Files**:
  - `app/src/main/java/com/neuroflow/app/domain/engine/PeakEnergyEngine.kt`
  - `app/src/main/java/com/neuroflow/app/domain/engine/EnergyScoreEngine.kt`
  - `app/src/main/java/com/neuroflow/app/domain/engine/TaskScoringEngine.kt`
  - `app/src/main/java/com/neuroflow/app/domain/scheduler/AutoSchedulingEngine.kt`
- **Recommended Changes**:
  1. **Continuous Baseline (Pre-Peak)**:
     In `PeakEnergyEngine.DetectionResult.currentValueAt`:
     Check if `nowMinuteOfDay` is between `wakeUpHour * 60` and `peakMinuteOfDay`.
     If so, ramp energy smoothly from wake baseline (e.g., `0.80f * peakValue`) to `peakValue` rather than decaying past 960 minutes.
  2. **Circadian Midnight Wrap**:
     In `EnergyScoreEngine.peakWindow`:
     ```kotlin
     private fun peakWindow(minute: Int, startMinute: Int, durationMinutes: Int, amplitude: Float): Float {
         if (durationMinutes <= 0) return 0f
         val elapsed = (minute - startMinute + MINUTES_PER_DAY) % MINUTES_PER_DAY
         if (elapsed >= durationMinutes) return 0f
         val phase = elapsed.toFloat() / durationMinutes.toFloat()
         val raw = 0.5f * (1f - cos((2.0 * PI * phase).toFloat()))
         return (amplitude * raw).coerceAtLeast(0f)
     }
     ```
  3. **Decouple Confidence**:
     In `EnergyScoreEngine.calculateDetailed`:
     ```kotlin
     val peakScore = (PERCENT_SCALE * reservoirFactor * circadianFactor).coerceIn(0f, PERCENT_SCALE)
     ```
     Keep `confidenceFactor` in `ScoreResult` for telemetry and UI insights, but do not multiply it into the user's raw energy reservoir.
  4. **Connect Live Energy to Scoring**:
     In `TaskScoringEngine`:
     Add optional `liveUsableEnergy: Int? = null` to `ScoringContext`.
     In `energyBonus`, if `liveUsableEnergy` is provided:
     - Energy >= 70: HIGH +70, MEDIUM +20, LOW -35
     - Energy in 40..69: HIGH +10, MEDIUM +30, LOW +15
     - Energy < 40: HIGH -35, MEDIUM +10, LOW +70
     Pass `liveEnergy` from `TodayViewModel` and `ScheduleAutoTasksWorker` into the scoring pipeline.

### R1.3: Room Persistence & Migrations
- **Target Files**:
  - `app/src/main/java/com/neuroflow/app/data/local/Converters.kt`
  - `app/src/main/java/com/neuroflow/app/data/local/NeuroFlowDatabase.kt`
  - `app/schemas/com.neuroflow.app.data.local.NeuroFlowDatabase/12.json`
- **Recommended Changes**:
  1. **Harden Enum Converters**:
     Replace direct `.valueOf()` calls in `Converters.kt` with safe fallback blocks:
     - `toQuadrant`: fallback `Quadrant.SCHEDULE`
     - `toPriority`: fallback `Priority.MEDIUM`
     - `toTaskStatus`: fallback `TaskStatus.ACTIVE`
     - `toRecurrence`: fallback `Recurrence.NONE`
     - `toEnergyLevel`: fallback `EnergyLevel.MEDIUM`
     - `toContractOutcome`: fallback `null`
     - `toRewardTier`: fallback `RewardTier.NONE`
  2. **Non-Destructive Migrations**:
     In `NeuroFlowDatabase.kt`:
     In `MIGRATION_18_19` and `MIGRATION_17_19`, delete `DROP TABLE IF EXISTS notes`. Keep `CREATE TABLE IF NOT EXISTS notes`.
  3. **Provide Missing Schema**:
     Synthesize and add `12.json` to `app/schemas/com.neuroflow.app.data.local.NeuroFlowDatabase/12.json`.
     Add a migration test `RoomMigrationV11ToV12Test.kt` verifying that `tasks`, `time_sessions`, and newly created `sleep_logs` migrate cleanly without dropping data.

### R1.4: Application Startup
- **Target Files**:
  - `app/src/main/java/com/neuroflow/app/NeuroFlowApplication.kt`
  - `app/src/main/java/com/neuroflow/app/data/local/DatabaseCleaner.kt`
- **Recommended Changes**:
  1. Remove `runBlocking` from `NeuroFlowApplication.onCreate()`.
  2. In `NeuroFlowApplication.onCreate()`:
     Launch restore version policy and install marker checks asynchronously:
     ```kotlin
     applicationScope.launch(Dispatchers.IO) {
         val firstLaunchAfterInstall = markFirstLaunchAfterInstall()
         databaseCleaner.enforceRestoreVersionPolicy(firstLaunchAfterInstall)
     }
     ```
  3. In `DatabaseCleaner.kt`:
     Change `commit()` to `apply()` in `writeRecordedBackupVersionCode`.

---

## 5. Verification Method

1. **Gradle Test Suite Verification**:
   - Command:
     ```bash
     ./gradlew testDebugUnitTest
     ```
   - Invalidation Condition:
     If any of the 5 previously skipped tests (`AbstentionRateTrend7dTest`, `AbstentionReasonMixTest`, `EnergyInsightTest`, `TimeUtilsTest`, `SleepPressureRefreshWorkerTest`) fail to run or appear in `app/build/test-results/testDebugUnitTest/TEST-*.xml`, the JUnit Platform vintage runner is misconfigured.

2. **Energy Engine Pre-Peak & Midnight Verification**:
   - Write unit tests in `app/src/test/java/com/neuroflow/app/domain/engine/EnergyScoreEngineTest.kt`:
     - Test energy score at 7:00 AM for wake=7:00 AM, peak=9:30 AM: assert `peakScore > 50f` and `usableEnergy > 60f`.
     - Test circadian factor at 00:30 AM for an evening window spanning 23:00 to 02:00: assert `circadianFactor > 0.5f`.
     - Test cold-start confidence 0.20: assert `peakScore` is not reduced by 80%.

3. **Room TypeConverters Resilience**:
   - Write unit test in `app/src/test/java/com/neuroflow/app/data/local/ConvertersTest.kt`:
     - Pass `"INVALID_ENUM"`, `""`, and corrupted strings to all `@TypeConverter` methods; assert none throw `IllegalArgumentException` and all return documented fallback defaults.

4. **Room Migration Verification**:
   - Run instrumentation / Robolectric migration test for `MIGRATION_11_12`:
     ```bash
     ./gradlew testDebugUnitTest --tests "*Migration*"
     ```
   - Verify `sleep_logs` is created with indices, and existing `tasks` and `time_sessions` rows and notes are preserved.

5. **Cold Start StrictMode Verification**:
   - Inspect `NeuroFlowApplication.onCreate()` to verify zero calls to `runBlocking`, `File.exists()`, or `File.writeText()` on the main thread.
