## 2026-10-16T17:15:17Z
You are the Milestone 2 Remediation Worker (worker_v2_m2_fix).

Your working directory is: `/home/takian/Project/PROCUS/.agents/worker_v2_m2_fix`
The project root is: `/home/takian/Project/PROCUS`
Authoritative User Request: `/home/takian/Project/PROCUS/.agents/ORIGINAL_REQUEST.md` (read section `## 2026-10-16T15:15:13Z` - R2).
Project Master Document: `/home/takian/Project/PROCUS/.agents/orchestrator_3/PROJECT.md`
Gate Status & Remediation Spec: `/home/takian/Project/PROCUS/.agents/orchestrator_3/GATE_STATUS.md`

Your mission:
Remediate the critical input boundary and concurrency issues identified in Milestone 2:

1. `app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapeGenerator.kt`:
   - In `generateFrames()` (lines 89-106, 163):
     In Kotlin, `Float.NaN.coerceIn(0f, 1f)` returns `Float.NaN`. Passing `Float.NaN` causes `currentVolume` to evaluate to `Float.NaN`, permanently silencing the generator on subsequent calls.
     Fix:
     ```kotlin
     val safeVolume = if (volumeMultiplier.isNaN()) 0f else volumeMultiplier
     val targetVol = safeVolume.coerceIn(0f, 1f)
     val startVol = if (currentVolume.isNaN()) 0f else currentVolume
     ```
   - In `setVolumeInstant(volume: Float)`:
     ```kotlin
     currentVolume = if (volume.isNaN()) 0f else volume.coerceIn(0f, 1f)
     ```

2. `app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapePlayer.kt`:
   - In `setVolume(volume: Float)` (line 118):
     ```kotlin
     currentVolume = if (volume.isNaN()) 0f else volume.coerceIn(0f, 1f)
     ```
   - In `startPlaybackThread()` (lines 206-253):
     When the loop breaks on `currentType == SoundscapeType.NONE`, `track.state != STATE_INITIALIZED`, or `track.write < 0`, the thread terminates without a `finally` block. `isPlaying.get()` remains `true` and `abandonAudioFocus()` is never called, locking out subsequent `play()` calls.
     Wrap the streaming loop body in `try-finally`:
     ```kotlin
     playbackThread = thread(name = "NeuralSoundscapeAudio", isDaemon = true) {
         try {
             // ... existing streaming while loop ...
         } finally {
             isPlaying.set(false)
             isFadingOut.set(false)
             abandonAudioFocus()
             track?.stop()
         }
     }
     ```

3. `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt`:
   - At line 251 (the soundscape chips `Row`):
     Add `Modifier.horizontalScroll(rememberScrollState())` to prevent chip clipping on compact screens (320dp width) or high font-scale configurations.

4. `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt`:
   - In `tickSecond()` (lines 214-229):
     When countdown hits 0 (`newRemaining == 0L` and mode != Stopwatch), trigger `soundscapePlayer.pauseWithFadeOut()`.

5. Verification:
   - Run: `./gradlew testDebugUnitTest --tests "*NeuralSoundscape*" --no-daemon -Dorg.gradle.workers.max=1`
   - Run: `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1`
   - Ensure `NeuralSoundscapeAdversarialStressTest.testVolumeBoundaryFuzzing_generator_extremeValuesAndNan` passes.
   - Verify 0 test failures across the entire project (all 914+ tests pass).

Exclusive Write Ownership:
You own ONLY:
- `app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapeGenerator.kt`
- `app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapePlayer.kt`
- `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt`
- `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt`
- Your metadata in `/home/takian/Project/PROCUS/.agents/worker_v2_m2_fix/`
