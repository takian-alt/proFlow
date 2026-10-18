## 2026-10-16T17:39:17Z

You are the Milestone 3 Worker (worker_v2_m3) responsible for Drift-Proof Background Focus & Lock-Screen Live Notification (R3).

Your working directory is: `/home/takian/Project/PROCUS/.agents/worker_v2_m3`
The project root is: `/home/takian/Project/PROCUS`
Authoritative User Request: `/home/takian/Project/PROCUS/.agents/ORIGINAL_REQUEST.md` (read section `## 2026-10-16T15:15:13Z` - R3).
Project Master Document: `/home/takian/Project/PROCUS/.agents/orchestrator_3/PROJECT.md`
Harness Reference: `/home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/e2e/harness/NeuroFlowV2Harness.kt` (`FocusTimingOracle` at lines 235-291).
E2E Test Reference: `/home/takian/Project/PROCUS/app/src/test/java/com/neuroflow/app/e2e/NeuroFlowV2FocusDriftE2eTest.kt`.

Your mission:
Implement drift-proof wall-clock timing and the foreground lock-screen live notification service for neuroFlow V2:

1. Create `app/src/main/java/com/neuroflow/app/presentation/v2/focus/engine/FocusTimingCalculator.kt`:
   - Package: `com.neuroflow.app.presentation.v2.focus.engine`
   - Pure Kotlin functions mirroring `FocusTimingOracle` in `NeuroFlowV2Harness.kt`:
     ```kotlin
     data class CalculatedTiming(
         val elapsedSeconds: Long,
         val remainingSeconds: Long,
         val isExpired: Boolean
     )

     object FocusTimingCalculator {
         fun calculate(
             startedAtMillis: Long,
             accumulatedPauseMillis: Long,
             pausedAtMillis: Long?,
             nowMillis: Long,
             targetDurationSeconds: Long
         ): CalculatedTiming {
             val safeNow = maxOf(nowMillis, startedAtMillis)
             val effectiveElapsedMillis = if (pausedAtMillis != null) {
                 val safePause = maxOf(pausedAtMillis, startedAtMillis)
                 maxOf(0L, safePause - startedAtMillis - accumulatedPauseMillis)
             } else {
                 maxOf(0L, safeNow - startedAtMillis - accumulatedPauseMillis)
             }
             val elapsedSeconds = effectiveElapsedMillis / 1000L
             val isStopwatch = targetDurationSeconds <= 0L
             val remainingSeconds = if (isStopwatch) 0L else maxOf(0L, targetDurationSeconds - elapsedSeconds)
             val isExpired = (!isStopwatch) && (elapsedSeconds >= targetDurationSeconds)
             return CalculatedTiming(elapsedSeconds, remainingSeconds, isExpired)
         }

         fun onPause(currentPausedAt: Long?, nowMillis: Long): Long {
             return currentPausedAt ?: nowMillis
         }

         fun onResume(pausedAtMillis: Long, accumulatedPauseMillis: Long, nowMillis: Long): Pair<Long, Long?> {
             val pauseDuration = maxOf(0L, nowMillis - pausedAtMillis)
             val newAccumulated = accumulatedPauseMillis + pauseDuration
             return Pair(newAccumulated, null)
         }
     }
     ```

2. Re-architect `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt`:
   - Add timing state fields to `FocusUiStateV2`:
     `val startedAtMillis: Long = 0L`, `val accumulatedPauseMillis: Long = 0L`, `val pausedAtMillis: Long? = null`.
   - In `loadTask(taskId)`:
     Check `sessionRepository.getOpenSessionForTask(taskId)`. If an open session exists, restore:
     `startedAtMillis = session.startedAt`, `accumulatedPauseMillis = session.totalPausedMs`, `pausedAtMillis = session.pausedAt`.
     If `session.pausedAt != null`, set `isPaused = true`, `isRunning = false`.
     If `session.pausedAt == null`, set `isRunning = true`, `isPaused = false`, and start ticker.
     Calculate current elapsed & remaining seconds from `FocusTimingCalculator.calculate(...)`.
   - In `startTimer()`:
     If starting fresh (or resuming from stopped): set `startedAtMillis = System.currentTimeMillis()`, `accumulatedPauseMillis = 0L`, `pausedAtMillis = null`.
     In the ticker loop (`while (isActive ...)`), call `tickSecond()` where elapsed and remaining are calculated via `FocusTimingCalculator.calculate(...)` using `System.currentTimeMillis()`.
     Trigger `FocusService.startService(...)` to show ongoing live notification.
   - In `pauseTimer()`:
     Set `pausedAtMillis = FocusTimingCalculator.onPause(pausedAtMillis, System.currentTimeMillis())`.
     Update Room session record (`session.copy(pausedAt = pausedAtMillis, totalPausedMs = accumulatedPauseMillis)`).
     Trigger `FocusService.pauseService(...)`.
   - In `resumeTimer()`:
     If `pausedAtMillis != null`:
       `val (newAcc, _) = FocusTimingCalculator.onResume(pausedAtMillis!!, accumulatedPauseMillis, System.currentTimeMillis())`
       `accumulatedPauseMillis = newAcc`
       `pausedAtMillis = null`
     Start ticker loop. Update Room session record. Trigger `FocusService.resumeService(...)`.
   - In `completeTask()`:
     Trigger `FocusService.stopService(...)`. Update session with `endedAt = System.currentTimeMillis()`.
   - In `onCleared()`:
     Trigger `FocusService.stopService(...)`.

3. Implement `app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt`:
   - Package: `com.neuroflow.app.presentation.v2.focus.service`
   - Foreground Service supporting:
     - Notification channel `"focus_live"`, name: `"Active Focus Session"`, `IMPORTANCE_LOW`, `VISIBILITY_PUBLIC`.
     - Ongoing notification (`setOngoing(true)`):
       - Title: Task title or "Deep Focus"
       - Text: formatted time ("MM:SS remaining" or "MM:SS elapsed")
       - Progress bar: `.setProgress(totalSec, elapsedSec, isStopwatch)`
       - Small icon: `android.R.drawable.ic_media_play` or app icon
       - ContentIntent: PendingIntent opening `MainActivity` with action `com.procus.ACTION_OPEN_FOCUS` and extra `taskId`
       - Action 1: "Pause" (when running) or "Resume" (when paused) PendingIntent sending Intent with `ACTION_PAUSE` / `ACTION_RESUME` to `FocusService`
       - Action 2: "Complete" PendingIntent sending Intent with `ACTION_COMPLETE` to `FocusService`
     - Companion object helpers: `start(context, taskId, taskTitle, durationSeconds, startedAtMillis)`, `pause(context)`, `resume(context)`, `stop(context)`.
     - Internal coroutine ticker running while active to update the notification every second without drifting.
     - Defensive error handling so headless / unit test execution does not crash.

4. Register `FocusService` in `app/src/main/AndroidManifest.xml`:
   ```xml
   <service
       android:name=".presentation.v2.focus.service.FocusService"
       android:exported="false"
       android:foregroundServiceType="specialUse">
       <property
           android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
           android:value="Live focus session tracking with drift-proof wall-clock countdown" />
   </service>
   ```

5. Create unit test `app/src/test/java/com/neuroflow/app/presentation/v2/focus/engine/FocusTimingCalculatorTest.kt`:
   - Test starting state, running progression, pause freeze, resume accumulation, simulated Doze/time jump (e.g. 30 minutes jump), and countdown expiration.

6. Verification:
   - Run: `./gradlew testDebugUnitTest --tests "*Focus*" --no-daemon -Dorg.gradle.workers.max=1`
   - Run: `./gradlew testDebugUnitTest --tests "*FocusDrift*" --no-daemon -Dorg.gradle.workers.max=1`
   - Run full project test suite: `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1`
   - Ensure 100% test pass rate with 0 failures and 0 regressions.

Exclusive Write Ownership:
You own ONLY:
- `app/src/main/java/com/neuroflow/app/presentation/v2/focus/engine/FocusTimingCalculator.kt`
- `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt`
- `app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/test/java/com/neuroflow/app/presentation/v2/focus/engine/FocusTimingCalculatorTest.kt`
- Metadata in `/home/takian/Project/PROCUS/.agents/worker_v2_m3/`

## 2026-10-16T17:54:56Z

**Context**: Milestone 3 Execution
**Content**: Checking in on test execution progress for FocusTimingCalculator, FocusService, and FocusViewModelV2.
**Action**: Please update progress.md or send completion report when tests finish.

