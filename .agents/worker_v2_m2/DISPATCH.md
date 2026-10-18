## 2026-10-16T16:35:13Z

You are a specialized worker implementing Milestone 2: Neural Soundscapes & Auditory Focus Stimulation.

Your working directory is: `/home/takian/Project/PROCUS/.agents/worker_v2_m2`
The project root is: `/home/takian/Project/PROCUS`
Authoritative User Request: `/home/takian/Project/PROCUS/.agents/ORIGINAL_REQUEST.md` (read section `## 2026-10-16T15:15:13Z` - R2).
Master Plan: `/home/takian/Project/PROCUS/.agents/orchestrator_3/PROJECT.md`
Blueprint & Findings: `/home/takian/Project/PROCUS/.agents/explorer_v2_survey_2/handoff.md` (see Blueprint 4.1 & Blueprint 4.4).

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Your exclusive write boundaries:
- `app/src/main/java/com/neuroflow/app/presentation/v2/focus/audio/` (`NeuralSoundscapeGenerator.kt`, `NeuralSoundscapePlayer.kt`, `SoundscapeType.kt`)
- `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusScreenV2.kt` (Soundscapes UI, volume slider, active animated indicator, lifecycle cleanup)
- `app/src/main/java/com/neuroflow/app/presentation/v2/focus/FocusViewModelV2.kt` (Soundscape selection state, volume state, start/pause/fade-out hooks)
- `app/src/test/java/com/neuroflow/app/presentation/v2/focus/audio/NeuralSoundscapeGeneratorTest.kt`

Implementation tasks:
1. Pure Kotlin DSP Generator (`NeuralSoundscapeGenerator.kt` & `SoundscapeType.kt`):
   - Pure JVM Kotlin code, 100% unit-testable.
   - `SoundscapeType`: `NONE`, `GAMMA_40HZ`, `BROWN_NOISE`, `PINK_NOISE`.
   - `GAMMA_40HZ`: 200 Hz carrier left channel, 240 Hz right channel (40 Hz binaural entrainment difference).
   - `BROWN_NOISE`: 1/f² stochastic acoustic masking using stereo leaky random walk.
   - `PINK_NOISE`: Paul Kellet 1/f filter approximation for calming spectral density.
   - Frame-interpolated smooth volume attenuation and fade-out to prevent audio pops and clicks.
2. Audio Track Player (`NeuralSoundscapePlayer.kt`):
   - Streaming `AudioTrack` with audio focus handling (`AudioManager.OnAudioFocusChangeListener`).
   - `play(type)`, `setVolume(vol)`, `pauseWithFadeOut()`, `stop()`, `release()`.
   - Leak-proof cleanup on session end or screen exit.
3. State & Controls in `FocusViewModelV2.kt`:
   - `selectedSoundscape: SoundscapeType` and `soundscapeVolume: Float` in `FocusUiStateV2`.
   - `selectSoundscape(type)` and `setSoundscapeVolume(volume)`.
   - Trigger fade-out on pause or completion.
4. UI in `FocusScreenV2.kt`:
   - Soundscape selector chips (Off, 40Hz Gamma 🧠, Deep Brown 🌊, Pink Rain 🌧️).
   - Tactile volume slider (0% to 100%) with 48dp touch target.
   - Headphone hint for 40Hz Gamma waves.
   - Clean lifecycle binding via `DisposableEffect`.
5. Unit Tests (`NeuralSoundscapeGeneratorTest.kt`):
   - Test silence on NONE.
   - Test 40Hz Gamma binaural phase generation and bounded sample amplitudes (`Short.MIN_VALUE .. Short.MAX_VALUE`).
   - Test Brown noise and Pink noise generation stability.
   - Test volume scaling and fade-out to zero.
6. Verify with `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1` (0 failures, 0 regressions).
7. Write report to `/home/takian/Project/PROCUS/.agents/worker_v2_m2/handoff.md` and message parent when complete.
