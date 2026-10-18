# Milestone 2 Handoff: Neural Soundscapes & Auditory Focus Stimulation

**Worker Agent**: `worker_v2_m2`  
**Working Directory**: `/home/takian/Project/PROCUS/.agents/worker_v2_m2`  
**Target Milestone**: Milestone 2 (Neural Soundscapes & Auditory Focus Stimulation)  
**Parent Conversation ID**: `f4f30ac2-dcff-4c62-ab0b-f13bcfa63ad1`  

---

## 1. Observation

1. **Missing Audio Synthesis Layer**:
   Prior to this milestone, inspection of `app/src/main/java/com/neuroflow/app/presentation/v2/focus/` revealed that no soundscape generation or audio streaming existed. Grep search for `AudioTrack`, `binaural`, or `SoundscapeType` in the main codebase returned zero results.
2. **FocusViewModelV2 Timer and State Hooks**:
   In `FocusViewModelV2.kt`, `FocusUiStateV2` had no state for soundscape selection or audio volume, and `startTimer()`, `pauseTimer()`, `completeTask()`, and `onCleared()` had no hooks to control audio playback or fade-out.
3. **FocusScreenV2 UI Controls**:
   In `FocusScreenV2.kt`, there were no soundscape selector chips, volume slider, active audio indicator, headphone hint, or lifecycle cleanup hooks.
4. **Unit and E2E Test Verification**:
   - Tool command: `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1`
   - Initial run result: `BUILD SUCCESSFUL in 11s`, 34 actionable tasks up-to-date, 0 failures.
   - Post-implementation run result: `BUILD SUCCESSFUL in 36s`, 34 actionable tasks: 7 executed, 27 up-to-date, 0 failures, 0 regressions.
   - Specific soundscape tests execution: `./gradlew testDebugUnitTest --tests "*Soundscape*" --no-daemon -Dorg.gradle.workers.max=1`
   - Result:
     - `NeuralSoundscapeGeneratorTest`: 9 tests executed, 0 skipped, 0 failures, 0 errors.
     - `NeuroFlowV2SoundscapeE2eTest`: 15 tests executed, 0 skipped, 0 failures, 0 errors.

---

## 2. Logic Chain

1. **Separation of Concerns (Pure DSP vs Android Audio Sink)**:
   - To achieve 100% testability on JVM runners without requiring Android stubs or native audio drivers, the core audio math was separated into `NeuralSoundscapeGenerator.kt`, which operates purely with primitive types (`ShortArray`, `Float`, `Double`) and standard math (`kotlin.math.sin`, `kotlin.math.PI`).
   - The streaming controller was placed into `NeuralSoundscapePlayer.kt`, encapsulating `android.media.AudioTrack` in `MODE_STREAM` and `AudioManager.OnAudioFocusChangeListener`.
2. **Frequency Synthesis & Binaural Phase Tracking**:
   - `SoundscapeType.GAMMA_40HZ` was implemented with carrier frequency $f_L = 200.0\text{ Hz}$ on the left channel and $f_R = 240.0\text{ Hz}$ on the right channel ($200 + 40\text{ Hz}$).
   - Left and right phase variables (`phaseLeft`, `phaseRight`) are preserved across consecutive buffer generations (`generateFrames`), eliminating phase discontinuities/pops across chunk boundaries.
   - Zero-crossing analysis verified $400\text{ crossings/s}$ on the left channel and $480\text{ crossings/s}$ on the right channel, demonstrating an exact $80\text{ crossings/s}$ difference ($2 \times 40\text{ Hz}$ binaural entrainment frequency difference).
3. **Noise Synthesis Algorithms**:
   - `SoundscapeType.BROWN_NOISE`: Implemented via independent stereo leaky random walks ($y[n] = (y[n-1] + 0.02 \cdot w) / 1.02$) with $w \in [-1, 1]$. The $1.02$ leaky denominator bounds energy to prevent numerical drift while $3.2\text{x}$ gain delivers optimal acoustic masking.
   - `SoundscapeType.PINK_NOISE`: Implemented using Paul Kellet's 6-pole IIR filter approximation on independent white noise per channel, producing a $1/f$ spectral slope.
4. **Click-Free Frame Interpolation & Smooth Fade-Out**:
   - To eliminate step discontinuities and high-frequency audio pops when adjusting volume or pausing, `NeuralSoundscapeGenerator` interpolates volume linearly frame-by-frame:
     $$\text{vol}[i] = \text{startVol} + (\text{targetVol} - \text{startVol}) \cdot \frac{i+1}{N}$$
   - When pausing or completing a session, `NeuralSoundscapePlayer.pauseWithFadeOut()` ramps the volume down to 0 over 400ms before pausing and flushing `AudioTrack`.
5. **Lifecycle & AudioFocus Safety**:
   - In `FocusViewModelV2`, `soundscapePlayer.release()` is invoked in `onCleared()`.
   - In `FocusScreenV2`, `DisposableEffect(Unit)` triggers `viewModel.onScreenExit()` when exiting the screen composition, ensuring zero audio leaks or orphaned threads.

---

## 3. Caveats

1. **Hardware Transducers**:
   40 Hz Gamma binaural beat entrainment operates via the brain's superior olivary complex and requires stereo acoustic separation. When played through a single monaural device loudspeaker, binaural phase interaction transitions into monaural acoustic beating. The UI explicitly includes an informational prompt: *"Stereo headphones recommended for 40 Hz binaural entrainment"*.
2. **Unit Test Host Environments**:
   `NeuralSoundscapePlayer` defensively wraps `AudioTrack` initialization in `try-catch` to allow safe instantiation in headless JVM test suites where Android audio hal classes may be stubbed or unavailable.

---

## 4. Conclusion

Milestone 2 (Neural Soundscapes & Auditory Focus Stimulation) is fully implemented, adhering strictly to the architecture specifications, exclusive write boundaries, and zero-defect mandate:
- `SoundscapeType.kt`: Declares `NONE`, `GAMMA_40HZ`, `BROWN_NOISE`, and `PINK_NOISE` with descriptive subtitles, emoji tags, and chip labels.
- `NeuralSoundscapeGenerator.kt`: Pure DSP PCM synthesis engine with continuous carrier phase tracking, leaky Brownian random walks, Kellet pink noise filtering, and frame-interpolated anti-pop volume ramps.
- `NeuralSoundscapePlayer.kt`: Real-time streaming `AudioTrack` player running on a dedicated daemon thread with dynamic AudioFocus handling, smooth 400ms fade-out, and leak-proof lifecycle teardown.
- `FocusViewModelV2.kt`: Added `selectedSoundscape` and `soundscapeVolume` to `FocusUiStateV2`, wired `selectSoundscape()`, `setSoundscapeVolume()`, and integrated `pauseWithFadeOut()` hooks into timer pause, task completion, and screen exit.
- `FocusScreenV2.kt`: Added tactile soundscape selector chips (min 48dp target), volume slider, active animated spectral bars indicator (`ActiveAudioIndicator`), headphone advice card, and `DisposableEffect` cleanup.
- `NeuralSoundscapeGeneratorTest.kt`: 9 new unit tests covering silence, Gamma binaural frequency delta, Brownian noise stability, Kellet pink noise stability, volume scaling, smooth fade-out, and argument boundary validation. 100% pass rate.

---

## 5. Verification Method

To independently verify the implementation:

1. **Execute Soundscape Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest --tests "*Soundscape*" --no-daemon -Dorg.gradle.workers.max=1
   ```
   *Expected Result*: `BUILD SUCCESSFUL`, 24 tests passed (9 in `NeuralSoundscapeGeneratorTest`, 15 in `NeuroFlowV2SoundscapeE2eTest`), 0 failures.

2. **Execute Full Project Unit Test Suite**:
   ```bash
   ./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1
   ```
   *Expected Result*: `BUILD SUCCESSFUL`, 0 failures, 0 regressions across all test suites.

3. **Inspect Output Files**:
   - `app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/SoundscapeType.kt`
   - `app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapeGenerator.kt`
   - `app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapePlayer.kt`
   - `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt`
   - `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt`
   - `app/src/test/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapeGeneratorTest.kt`
