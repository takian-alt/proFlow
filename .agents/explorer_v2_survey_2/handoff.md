# Handoff Report: neuroFlow V2 Survey for Milestones R2 & R3

**Agent**: teamwork_preview_explorer  
**Working Directory**: `/home/takian/Project/PROCUS/.agents/explorer_v2_survey_2`  
**Target Milestones**:  
- **R2**: Neural Soundscapes & Auditory Focus Stimulation (40 Hz Gamma, Brown Noise, Pink Noise / Rain, tactile volume slider, fade-out, audio leak prevention, lifecycle cleanup)  
- **R3**: Drift-Proof Background Focus & Lock-Screen Live Notification (Wall-clock timestamps `startedAtMillis`, `accumulatedPauseMillis`, ongoing foreground service notification, live countdown progress, 1-tap Pause/Resume/Complete actions)

---

## 1. Observation

### 1.1 FocusViewModelV2 Timer Drift and Pause Flaws
Direct inspection of `/home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt`:
- **Coroutine Tick Increment**: Lines 145-151 and 154-169:
  ```kotlin
  timerJob?.cancel()
  timerJob = viewModelScope.launch {
      while (isActive && _uiState.value.isRunning && !_uiState.value.isPaused) {
          delay(1000)
          if (!isActive || !_uiState.value.isRunning || _uiState.value.isPaused) break
          tickSecond()
      }
  }
  ```
  ```kotlin
  fun tickSecond() {
      if (!_uiState.value.isRunning || _uiState.value.isPaused) return
      _uiState.update { current ->
          if (!current.isRunning || current.isPaused) return@update current
          val newElapsed = current.elapsedSeconds + 1
          val newRemaining = if (current.timerMode == FocusTimerModeV2.FLOW_STOPWATCH) {
              0L
          } else {
              (current.remainingSeconds - 1).coerceAtLeast(0)
          }
          current.copy(
              elapsedSeconds = newElapsed,
              remainingSeconds = newRemaining
          )
      }
  }
  ```
- **Pause Handling**: Lines 171-178:
  ```kotlin
  fun pauseTimer() {
      pauseCount++
      timerJob?.cancel()
      timerJob = null
      disableDnd()
      _uiState.update { it.copy(isPaused = true, isRunning = false) }
      updateSessionRecord(isOpen = true)
  }
  ```
  Observation: `pauseTimer()` increments a count and sets `isPaused = true`, but does **not** record the pause timestamp `pausedAtMillis` or accumulate pause durations in milliseconds.
- **Resume Handling**: Line 180-182:
  ```kotlin
  fun resumeTimer() {
      startTimer()
  }
  ```
  Observation: `resumeTimer()` calls `startTimer()`, which restarts the `delay(1000)` loop. Because no pause duration was recorded, wall-clock time elapsed during the pause cannot be subtracted from overall time.
- **Session Restoration from Database**: Lines 80-101 (`loadTask()`):
  `loadTask()` loads `task`, `woop`, and `microSteps`, but never checks `sessionRepository.getOpenSessionForTask(taskId)`. If a user navigates away from `FocusScreenV2` and returns, `_uiState` resets to 0 seconds (`elapsedSeconds = 0L`, `remainingSeconds = 1500L`).

### 1.2 Existing Room Database Schema & DAO Support
Direct inspection of `/home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/entity/TimeSessionEntity.kt` and `/home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/data/local/dao/TimeSessionDao.kt`:
- `TimeSessionEntity` already declares:
  ```kotlin
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val taskId: String,
  val startedAt: Long,
  val endedAt: Long? = null,
  val pausedAt: Long? = null,
  val totalPausedMs: Long = 0L,
  val durationMinutes: Float = 0f,
  val pauseResumeCount: Int = 0
  ```
- Lines 36-39 of `TimeSessionEntity.kt` already define the drift-proof formula:
  ```kotlin
  val currentPauseMs = if (isLive && pauseStart != null) maxOf(0L, end - pauseStart) else 0L
  val elapsed = (end - startedAt - totalPausedMs - currentPauseMs).coerceAtLeast(0L) / 60_000f
  ```
- `TimeSessionDao.kt` lines 24-25 and 27-28 provide:
  ```kotlin
  @Query("SELECT * FROM time_sessions WHERE taskId = :taskId AND endedAt IS NULL LIMIT 1")
  suspend fun getOpenSessionForTask(taskId: String): TimeSessionEntity?
  ```
- Classic V1 `FocusSessionManager.kt` lines 25-28 also uses:
  ```kotlin
  val pausedSinceMs = if (isPaused) now - openSession.pausedAt!! else 0L
  val elapsedMs = (now - openSession.startedAt) - openSession.totalPausedMs - pausedSinceMs
  val elapsedSec = maxOf(0L, elapsedMs / 1000L)
  ```

### 1.3 State of Audio & Neural Soundscapes in the Codebase
- Grep search for `AudioTrack`, `MediaPlayer`, `Soundscape`, `binaural`, or `noise` in `app/src/` returned **0 matches** (outside of test files and agent documents).
- `FocusScreenV2.kt` lines 1-512 has no audio controls, volume slider, soundscape selector, or fade-out mechanisms.
- `FocusViewModelV2.kt` lines 1-331 has no audio lifecycle handling.

### 1.4 State of Foreground Services and Notifications
- Grep search for `FocusService` returned **0 matches** in source code.
- Services in `app/src/main/java/com/neuroflow/app/` currently are:
  - `feature/hyperfocus/service/HyperFocusMonitorService.kt` (monitors accessibility service for app blocking)
  - `feature/hyperfocus/service/UnlockTimerService.kt` (unlock countdown timer foreground service)
  - `feature/hyperfocus/service/AppBlockingService.kt` (accessibility service)
  - `data/badge/NotificationBadgeService.kt` (notification listener service)
- `AndroidManifest.xml` lines 5-13 and lines 193-212 declare:
  ```xml
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
  ```
  Both `UnlockTimerService` and `HyperFocusMonitorService` declare `android:foregroundServiceType="specialUse"` with `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" ... />`.
- Notification Channels: `NotificationWorker.kt` lines 112-132 initializes `CHANNEL_TASKS`, `CHANNEL_FOCUS` (`"focus"`, `"Focus & Streaks"`, `IMPORTANCE_DEFAULT`), `CHANNEL_DAILY`, and `"autonomy_nudge"`.
- Navigation & Deep Linking: In `MainActivity.kt` lines 79-82:
  ```kotlin
  "com.procus.ACTION_OPEN_FOCUS" -> {
      initialFocusTaskId = taskId
      return
  }
  ```
  In lines 302-306, `initialFocusTaskId` triggers `NeuroFlowAppV2`, which navigates to `"focus_v2/{taskId}"`.

### 1.5 Unit Test Execution & Build Environment
- Tool execution: `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1`
- Result: **BUILD SUCCESSFUL in 9s**, 34 actionable tasks up-to-date, 0 test failures.
- No unit tests currently exist for `FocusViewModelV2` or `FocusScreenV2`.

---

## 2. Logic Chain

```
Observation 1.1: FocusViewModelV2 increments elapsedSeconds + 1 inside a delay(1000) coroutine loop.
Observation 1.1: FocusViewModelV2 does not record pausedAtMillis or accumulate pause intervals.
Observation 1.1: loadTask() never checks getOpenSessionForTask(taskId) to restore state.
        │
        ▼
[Root Cause of Drift & Freezing]: Android puts the CPU to sleep and halts or throttles coroutine dispatchers during screen-off / Doze mode. When the user unlocks the screen 30 minutes later, only the ticks that fired before sleep ran, causing catastrophic timing drift. Furthermore, switching away resets the timer to 0.
        │
        ▼
[Wall-Clock Timing Engine Requirement]:
State must track:
  - startedAtMillis: Long (epoch time when timer started)
  - accumulatedPauseMillis: Long (sum of all completed pause durations)
  - pausedAtMillis: Long? (null when running, epoch time when paused)
Formula for any instant `now = System.currentTimeMillis()`:
  currentPauseDelta = if (pausedAtMillis != null) maxOf(0L, now - pausedAtMillis) else 0L
  totalPaused = accumulatedPauseMillis + currentPauseDelta
  elapsedSeconds = maxOf(0L, (now - startedAtMillis - totalPaused) / 1000L)
  remainingSeconds = if (isStopwatch) 0L else maxOf(0L, durationSeconds - elapsedSeconds)
Because this formula evaluates wall-clock epoch time, even if the CPU slept for 45 minutes, the next tick or onResume instantly calculates the exact true elapsed time with 0 drift.
        │
        ▼
Observation 1.3: No audio synthesis or soundscape files exist.
Observation 1.4: No FocusService exists in the codebase.
        │
        ▼
[Clean Modular Design for R2 Soundscapes]:
Decompose into two distinct layers:
  1. Pure Kotlin DSP Generator (`NeuralSoundscapeGenerator`):
     - Pure JVM code, zero Android framework classes.
     - 100% testable in JVM unit tests (`app/src/test/`).
     - Generates 16-bit PCM stereo sample arrays (`ShortArray`).
     - 40 Hz Gamma: Left channel 200 Hz, Right channel 240 Hz (binaural beat delta = 40 Hz).
     - Deep Brown Noise: Leaky random walk integration ($y[n] = (y[n-1] + 0.02 \cdot w) / 1.02$) with stereo decorrelation.
     - Flow Pink Noise / Rain: Paul Kellet 1/f filter approximation.
     - Volume & Smooth Fade: Frame-interpolated amplitude ramp (0 to V or V to 0) to eliminate audio pops and clicks.
  2. Android AudioTrack Player (`NeuralSoundscapePlayer`):
     - Encapsulates `android.media.AudioTrack` in streaming mode.
     - Controlled via `play()`, `setVolume()`, `pauseWithFadeOut()`, `stop()`, `release()`.
     - Handles AudioFocus (`AudioManager.OnAudioFocusChangeListener`) and leak-proof lifecycle cleanup.
        │
        ▼
[Foreground Focus Service for R3 Lock-Screen Control & Background Audio]:
Create `FocusService : Service()`:
  - Declared with `android:foregroundServiceType="specialUse"` (or `mediaPlayback|specialUse`).
  - Holds ongoing notification in `CHANNEL_FOCUS_LIVE` (`IMPORTANCE_LOW`, `VISIBILITY_PUBLIC`, `setOnlyAlertOnce(true)`).
  - Displays: Task title, live countdown progress bar (`.setProgress(totalSeconds, elapsedSeconds, false)`), time remaining.
  - Action PendingIntents:
    - 1-tap "Pause" (`ACTION_PAUSE`)
    - 1-tap "Resume" (`ACTION_RESUME`)
    - 1-tap "Complete" (`ACTION_COMPLETE`)
    - Content Intent opening `MainActivity` with `ACTION_OPEN_FOCUS` and `taskId`.
  - Coordinates with `FocusViewModelV2` via shared state / repository and keeps soundscapes streaming in background when screen is off.
```

---

## 3. Caveats

1. **Mono Speakers vs Stereo Headphones**: 40 Hz Gamma binaural beats rely on phase differences between the left and right auditory pathways (superior olivary complex). Over single phone speakers, binaural entrainment transitions into acoustic monaural beating. The UI should display a subtle hint ("Headphones recommended for 40 Hz Gamma waves").
2. **Audio Focus Handling**: Incoming phone calls or alarms should pause the soundscape with a fast fade-out (~300ms) and resume after call completion.
3. **Android 14+ Foreground Service Policy**: `FOREGROUND_SERVICE_SPECIAL_USE` is already declared in `AndroidManifest.xml` and utilized by `UnlockTimerService` and `HyperFocusMonitorService`. If `mediaPlayback` is added, `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />` must also be declared.
4. **Room Database Concurrency**: When `FocusService` completes a session from a notification action while the app is in the background, updating `TimeSessionEntity` and calling `taskRepository.completeAndRecur()` must be executed on `Dispatchers.IO` to avoid any main-thread blocking.

---

## 4. Conclusion & Implementation Blueprints

### Blueprint 4.1: Pure Kotlin Soundscape DSP Generator (`NeuralSoundscapeGenerator.kt`)
**Target path**: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapeGenerator.kt`

```kotlin
package com.neuroflow.app.presentation.v2.focus.audio

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class SoundscapeType(val displayName: String, val subtitle: String) {
    NONE("Off", "Silence"),
    GAMMA_40HZ("40 Hz Gamma", "Binaural Prefrontal Synchrony"),
    BROWN_NOISE("Deep Brown Noise", "1/f² Stochastic Acoustic Masking"),
    PINK_NOISE("Flow Pink Noise / Rain", "1/f Calming Spectral Density")
}

class NeuralSoundscapeGenerator(
    private val sampleRate: Int = 44100
) {
    private var phaseLeft = 0.0
    private var phaseRight = 0.0
    
    // Brownian motion state (independent stereo random walks)
    private var brownLeft = 0.0f
    private var brownRight = 0.0f

    // Kellet 1/f Pink noise filter state
    private var b0L = 0f; private var b1L = 0f; private var b2L = 0f; private var b3L = 0f; private var b4L = 0f; private var b5L = 0f; private var b6L = 0f
    private var b0R = 0f; private var b1R = 0f; private var b2R = 0f; private var b3R = 0f; private var b4R = 0f; private var b5R = 0f; private var b6R = 0f

    companion object {
        const val CARRIER_HZ = 200.0
        const val GAMMA_BEAT_HZ = 40.0
        const val LEFT_FREQ = CARRIER_HZ
        const val RIGHT_FREQ = CARRIER_HZ + GAMMA_BEAT_HZ // 240.0 Hz
    }

    /**
     * Generates stereo 16-bit PCM frames into [outputBuffer] (size = numFrames * 2).
     * [volumeMultiplier] ranges 0.0f .. 1.0f.
     */
    fun generateFrames(
        type: SoundscapeType,
        outputBuffer: ShortArray,
        numFrames: Int,
        volumeMultiplier: Float = 0.7f
    ) {
        val vol = volumeMultiplier.coerceIn(0f, 1f)
        if (type == SoundscapeType.NONE || vol <= 0.0001f) {
            outputBuffer.fill(0, 0, numFrames * 2)
            return
        }

        val stepLeft = 2.0 * PI * LEFT_FREQ / sampleRate
        val stepRight = 2.0 * PI * RIGHT_FREQ / sampleRate

        for (i in 0 until numFrames) {
            var leftSampleFloat: Float
            var rightSampleFloat: Float

            when (type) {
                SoundscapeType.GAMMA_40HZ -> {
                    leftSampleFloat = sin(phaseLeft).toFloat()
                    rightSampleFloat = sin(phaseRight).toFloat()
                    phaseLeft += stepLeft
                    phaseRight += stepRight
                    if (phaseLeft >= 2.0 * PI) phaseLeft -= 2.0 * PI
                    if (phaseRight >= 2.0 * PI) phaseRight -= 2.0 * PI
                }
                SoundscapeType.BROWN_NOISE -> {
                    val whiteL = Random.nextFloat() * 2f - 1f
                    val whiteR = Random.nextFloat() * 2f - 1f
                    brownLeft = (brownLeft + (0.02f * whiteL)) / 1.02f
                    brownRight = (brownRight + (0.02f * whiteR)) / 1.02f
                    leftSampleFloat = (brownLeft * 3.2f).coerceIn(-1f, 1f)
                    rightSampleFloat = (brownRight * 3.2f).coerceIn(-1f, 1f)
                }
                SoundscapeType.PINK_NOISE -> {
                    val wL = Random.nextFloat() * 2f - 1f
                    val wR = Random.nextFloat() * 2f - 1f
                    b0L = 0.99886f * b0L + wL * 0.0555179f
                    b1L = 0.99332f * b1L + wL * 0.0750759f
                    b2L = 0.96900f * b2L + wL * 0.1538520f
                    b3L = 0.86650f * b3L + wL * 0.3104856f
                    b4L = 0.55000f * b4L + wL * 0.5329522f
                    b5L = -0.7616f * b5L - wL * 0.0168980f
                    leftSampleFloat = ((b0L + b1L + b2L + b3L + b4L + b5L + b6L + wL * 0.5362f) * 0.11f).coerceIn(-1f, 1f)
                    b6L = wL * 0.115926f

                    b0R = 0.99886f * b0R + wR * 0.0555179f
                    b1R = 0.99332f * b1R + wR * 0.0750759f
                    b2R = 0.96900f * b2R + wR * 0.1538520f
                    b3R = 0.86650f * b3R + wR * 0.3104856f
                    b4R = 0.55000f * b4R + wR * 0.5329522f
                    b5R = -0.7616f * b5R - wR * 0.0168980f
                    rightSampleFloat = ((b0R + b1R + b2R + b3R + b4R + b5R + b6R + wR * 0.5362f) * 0.11f).coerceIn(-1f, 1f)
                    b6R = wR * 0.115926f
                }
                SoundscapeType.NONE -> {
                    leftSampleFloat = 0f
                    rightSampleFloat = 0f
                }
            }

            outputBuffer[2 * i] = (leftSampleFloat * vol * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            outputBuffer[2 * i + 1] = (rightSampleFloat * vol * 32767f).toInt().coerceIn(-32768, 32767).toShort()
        }
    }
}
```

### Blueprint 4.2: Drift-Proof Timing Engine (`FocusTimingCalculator.kt`)
**Target path**: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/engine/FocusTimingCalculator.kt`

```kotlin
package com.neuroflow.app.presentation.v2.focus.engine

data class CalculatedTiming(
    val elapsedSeconds: Long,
    val remainingSeconds: Long,
    val isExpired: Boolean
)

object FocusTimingCalculator {
    /**
     * Drift-proof calculation based strictly on wall-clock epoch timestamps.
     */
    fun calculate(
        startedAtMillis: Long,
        accumulatedPauseMillis: Long,
        pausedAtMillis: Long?,
        nowMillis: Long,
        targetDurationSeconds: Long
    ): CalculatedTiming {
        if (startedAtMillis <= 0L) {
            return CalculatedTiming(
                elapsedSeconds = 0L,
                remainingSeconds = targetDurationSeconds,
                isExpired = false
            )
        }

        val currentPauseDelta = if (pausedAtMillis != null && pausedAtMillis > 0L) {
            (nowMillis - pausedAtMillis).coerceAtLeast(0L)
        } else {
            0L
        }

        val totalPaused = accumulatedPauseMillis + currentPauseDelta
        val elapsedMillis = (nowMillis - startedAtMillis - totalPaused).coerceAtLeast(0L)
        val elapsedSeconds = elapsedMillis / 1000L

        val isStopwatch = targetDurationSeconds <= 0L
        val remainingSeconds = if (isStopwatch) {
            0L
        } else {
            (targetDurationSeconds - elapsedSeconds).coerceAtLeast(0L)
        }

        val isExpired = !isStopwatch && elapsedSeconds >= targetDurationSeconds

        return CalculatedTiming(
            elapsedSeconds = elapsedSeconds,
            remainingSeconds = remainingSeconds,
            isExpired = isExpired
        )
    }
}
```

### Blueprint 4.3: Foreground Service & Lock-Screen Live Notification (`FocusService.kt`)
**Target path**: `app/src/main/java/com/neuroflow/app/presentation/v2/focus/service/FocusService.kt`
- **Channel**: `"focus_live"` with `NotificationManager.IMPORTANCE_LOW` and lock screen visibility `VISIBILITY_PUBLIC`.
- **Notification**:
  - Title: Task Title
  - Text: e.g. `"22:45 remaining • 25m Pomodoro"` or `"PAUSED (08:15 elapsed)"`
  - Progress: `.setProgress(totalSeconds.toInt(), elapsedSeconds.toInt(), false)`
  - Action 1: "Pause" / "Resume" PendingIntent (`ACTION_PAUSE` / `ACTION_RESUME`)
  - Action 2: "Complete" PendingIntent (`ACTION_COMPLETE`)
  - Content Intent: `PendingIntent.getActivity` targeting `MainActivity` with action `"com.procus.ACTION_OPEN_FOCUS"` and `taskId`.
- **Continuous Background Soundscape**: Runs `NeuralSoundscapePlayer` inside the foreground service so audio streams continuously even with screen locked.
- **Manifest Entry**:
  ```xml
  <service
      android:name=".presentation.v2.focus.service.FocusService"
      android:exported="false"
      android:foregroundServiceType="specialUse|mediaPlayback">
      <property
          android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
          android:value="Ongoing focus session tracking with soundscape and lock-screen live notification" />
  </service>
  ```

### Blueprint 4.4: UI Redesign in `FocusScreenV2.kt`
- Add **Neural Soundscape Section**:
  - Soundscape selector chips (Off, 40Hz Gamma 🧠, Deep Brown 🌊, Pink Rain 🌧️)
  - Tactile Volume Slider (0% to 100%) with volume icons and min 48dp touch target
  - Animated soundscape active indicator
- Add **Empty WOOP Anchor CTA**:
  - When `state.woopData` is empty/blank, render a prominent 1-tap "Set WOOP Anchor" card opening the WOOP reflection modal sheet.

---

## 5. Verification Method

### 5.1 Automated Unit Tests
Run the test suite via Gradle:
```bash
./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1
```
The following new unit tests should be added to `app/src/test/java/com/neuroflow/app/presentation/v2/focus/`:
1. `NeuralSoundscapeGeneratorTest.kt`:
   - `testGenerateSilenceOnNone()`: buffer fills with zeros.
   - `testGenerateGamma40Hz()`: verifies non-zero PCM frames, Left (200Hz) vs Right (240Hz) phase advancement, samples within `Short.MIN_VALUE .. Short.MAX_VALUE`.
   - `testGenerateBrownNoise()`: verifies stochastic signal, bounded random walk, stereo decorrelation.
   - `testGeneratePinkNoise()`: verifies 1/f Kellet filter output stability across 44,100 frames.
   - `testVolumeScalingAndFadeOut()`: verifies monotonic decrease to zero without clipping.
2. `FocusTimingCalculatorTest.kt`:
   - `testElapsedCalculationNormal()`: verifies exact elapsed seconds.
   - `testSimulatedScreenOffSleepNoDrift()`: jump `now` by 1200 seconds, verify exact 1200s elapsed and 300s remaining.
   - `testSingleAndMultiplePauseAccumulation()`: pause for 5 minutes, advance clock, resume, verify pause interval is subtracted.
   - `testFrozenTimeWhilePaused()`: verify elapsed time does not advance while `pausedAtMillis` is active.
   - `testStopwatchMode()`: verify stopwatch counts indefinitely with 0 remaining seconds.

### 5.2 Manual / Functional Verification
1. Start a 25m Pomodoro on `FocusScreenV2`. Turn phone screen off for 5 minutes. Turn screen back on: verify the timer displays exactly `20:00 remaining` (0 drift).
2. Inspect lock screen: verify the ongoing live notification displays the task title, live countdown progress bar, and "Pause" / "Complete" buttons.
3. Tap "Pause" on the lock screen notification: verify the session pauses, timer freezes, notification button changes to "Resume", and soundscape smoothly fades out over 1s.
4. Tap "Resume" on the lock screen: verify session resumes, timer continues from frozen moment, and soundscape smoothly fades back in.
5. Tap "Complete" on the lock screen: verify task is marked complete in Room DB, notification disappears, and foreground service terminates cleanly.
6. Verify soundscape clean cleanup: exit `FocusScreenV2` while soundscape is playing -> verify no audio leaks or memory leaks in Logcat.
