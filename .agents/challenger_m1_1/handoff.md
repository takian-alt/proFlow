# Milestone 1 Adversarial Challenge Report: Math & Energy Pipeline (R1)

## Verdict: REJECT

---

## 1. Observation

### 1.1 Empirical Verification Test Suite
Created and executed empirical test harness at `app/src/test/java/com/neuroflow/app/domain/engine/Milestone1AdversarialChallengeTest.kt` using `./gradlew testDebugUnitTest --tests "com.neuroflow.app.domain.engine.Milestone1AdversarialChallengeTest" --info`.

Output:
```
BUILD SUCCESSFUL in 17s
com.neuroflow.app.domain.engine.Milestone1AdversarialChallengeTest > EMPIRICAL CHALLENGE: EnergyScoreEngine continuity at exact peak minute STANDARD_OUT
    === ADVERSARIAL DISCOVERY CHECK ===
    Minute 569 (9:29 AM): circadianFactor=0.99833333, peakScore=99.70854, usableEnergy=99.85427
    Minute 570 (9:30 AM): circadianFactor=0.35, peakScore=35.0, usableEnergy=67.5
    Circadian factor drop at peak minute: 0.6483333

com.neuroflow.app.domain.engine.Milestone1AdversarialChallengeTest > EMPIRICAL CHALLENGE: TaskScoringEngine float range boundary hole at 69.9995% STANDARD_OUT
    === ADVERSARIAL GAP TEST RESULTS ===
    Score at 70.0% (HIGH): 415.0
    Score at 69.0% (MODERATE): 310.0
    Score at 30.0% (DEPLETED): 225.0
    Score at 69.9995% (BOUNDARY GAP): 265.0
    Breakdown at 69.9995%: Energy matching = -35.0, Circadian = 15.0
    BUG CONFIRMED: Energy matching at 69.9995% gave -35f (DEPLETED) instead of +10f (MODERATE)!
```

### 1.2 Bug 1: Discontinuous Circadian Factor Cliff-Drop at Peak Minute
- **File**: `app/src/main/java/com/neuroflow/app/domain/engine/EnergyScoreEngine.kt`
- **Lines 69–75**:
  ```kotlin
  if (minutesWakeToPeak in 1..(12 * 60) && minutesSinceWake < minutesWakeToPeak) {
      // Pre-peak morning ramp: circadian rhythm rises continuously from morning wake baseline (0.75f) to 1.0f
      val ramp = minutesSinceWake.toFloat() / minutesWakeToPeak.toFloat()
      (0.75f + 0.25f * ramp).coerceIn(0f, 1f)
  } else {
      (0.35f + 0.65f * rawCircadian).coerceIn(0f, 1f)
  }
  ```
- **Lines 107–139**:
  ```kotlin
  fun circadianFactor(
      minutesSincePeak: Int,
      profile: PeakEnergyEngine.CircadianProfile? = null
  ): Float { ... }

  private fun peakWindow(
      minute: Int,
      startMinute: Int,
      durationMinutes: Int,
      amplitude: Float
  ): Float {
      if (durationMinutes <= 0) return 0f
      val normalizedStart = wrapMinutes(startMinute)
      val elapsed = (minute - normalizedStart + MINUTES_PER_DAY) % MINUTES_PER_DAY
      if (elapsed >= durationMinutes) return 0f

      val phase = elapsed.toFloat() / durationMinutes.toFloat()
      val raw = 0.5f * (1f - cos((2.0 * PI * phase).toFloat()))
      return (amplitude * raw).coerceAtLeast(0f)
  }
  ```
- **Observed Behavior**:
  For a user waking at 7:00 AM (minute 420) with peak anchor at 9:30 AM (minute 570):
  - At 9:29 AM (`nowMinute = 569`, 1 minute before peak): `minutesSinceWake = 149 < 150`. Pre-peak ramp evaluates `0.75 + 0.25 * (149/150) = 0.9983`. `circadianFactor = 0.9983`, `peakScore = 99.71`, `usableEnergy = 99.85%`.
  - At 9:30 AM (`nowMinute = 570`, exact peak time): `minutesSinceWake = 150 < 150` evaluates to `false`. Execution jumps to the `else` branch: `(0.35f + 0.65f * rawCircadian)`. At peak time, `minutesSincePeak = 0`. For default profile `PeakWindow(startMinuteOffset = 0, durationMinutes = 210)`, `elapsed = 0`, `phase = 0`, `cos(0) = 1.0`, resulting in `raw = 0.5 * (1.0 - 1.0) = 0.0`.
  - Result: `rawCircadian = 0.0`. `circadianFactor` drops instantly from `0.9983` to `0.3500` (a drop of 64.8 percentage points). `peakScore` crashes from `99.71` to `35.00`. `usableEnergy` crashes from `99.85%` to `67.50%` (a 32.35% drop in 60 seconds).
  - The apex of the cosine window is at `phase = 0.5` (105 minutes past peak anchor, 11:15 AM). The ramp ramps up to 9:30 AM, then falls into a 105-minute pit before rising back to the cosine peak.

### 1.3 Bug 2: Floating-Point Boundary Gap in `TaskScoringEngine` (69.999f to 70.0f)
- **File**: `app/src/main/java/com/neuroflow/app/domain/engine/TaskScoringEngine.kt`
- **Lines 575–591 & 1034–1050**:
  ```kotlin
  when {
      usable >= 70f -> when (task.energyLevel) {
          EnergyLevel.HIGH   ->  70f
          EnergyLevel.MEDIUM ->  20f
          EnergyLevel.LOW    -> -35f
      }
      usable in 40f..69.999f -> when (task.energyLevel) {
          EnergyLevel.HIGH   ->  10f
          EnergyLevel.MEDIUM ->  30f
          EnergyLevel.LOW    ->  15f
      }
      else -> when (task.energyLevel) {
          EnergyLevel.HIGH   -> -35f
          EnergyLevel.MEDIUM ->  10f
          EnergyLevel.LOW    ->  70f
      }
  }
  ```
- **Observed Behavior**:
  `usable in 40f..69.999f` checks `usable >= 40f && usable <= 69.999f`.
  Any calculated `usableEnergy` in the open interval `(69.999f, 70.0f)` (e.g. `69.9995f`, which commonly occurs via floating-point cosine and decay math):
  - Fails `usable >= 70f`.
  - Fails `usable in 40f..69.999f`.
  - Falls into `else`, which is the DEPLETED state (`< 40f`).
  - Empirical verification: At `69.0f`, high-energy analytical task receives +10f energy bonus (Moderate, score = 310.0). At `70.0f`, it receives +70f (High, score = 415.0). At `69.9995f`, it receives `-35.0f` (Depleted, score = 265.0).
  - Internal inconsistency: In Section 9 (`TaskScoringEngine.kt:620`), the check is `effectiveLiveEnergy.usableEnergy >= 70f -> 60f`, `effectiveLiveEnergy.usableEnergy < 40f -> -25f`, `else -> 15f`. So Section 9 awards +15f (Moderate) while Section 8 simultaneously penalizes with -35f (Depleted).

### 1.4 Passing Implementations Verified
- **Circadian Midnight Wrap**: `PeakEnergyEngine` and `EnergyScoreEngine` successfully wrap modulo 1440 across midnight. Window spanning 23:00 to 01:00 produces continuous values (1439 -> 0 -> 1) with apex at 00:00 midnight (`circadianFactor = 1.0f`).
- **Confidence Decoupling**: Decoupled from physical energy magnitude. For `confidence in listOf(0.0f, 0.1f, 0.5f, 1.0f)`, `peakScore`, `usableEnergy`, `rawEnergy`, and `fatiguePenalty` are identical to within 0.0001f, while `confidenceFactor` correctly preserves the input confidence.
- **Extreme Chronotypes & Offsets**: Handled safely without crash or arithmetic exception across 0h, 3h, 5.5h, and 12h offsets, including wake hour 0 (midnight) and wake hour 23 (night owl).
- **Live Energy Dynamic Ranking**: 12-way matrix test confirmed that under peak energy (90%), analytical high-energy tasks rank #1, whereas under depleted energy (15%), admin low-energy tasks rank #1.

---

## 2. Logic Chain

1. **R1 Mandate**: `ORIGINAL_REQUEST.md` specifies:
   > "Fix Borbély Two-Process and Peak Energy engines: continuous baseline without zeroing pre-peak hours"
2. **Discontinuity Proof (Bug 1)**:
   - Observation 1.2 confirms that at `nowMinuteOfDay = peakMinuteOfDay - 1`, `circadianFactor = 0.9983`.
   - At `nowMinuteOfDay = peakMinuteOfDay`, `rawCircadian = 0.0`, resulting in `circadianFactor = 0.3500`.
   - This creates an abrupt 64.8-point drop in circadian factor and a 32.35% drop in usable energy right at the designated peak minute.
   - Therefore, the baseline is NOT continuous across the pre-peak ramp boundary, violating R1.
3. **Dead Zone Proof (Bug 2)**:
   - In `TaskScoringEngine.kt:581` and `1040`, the range `40f..69.999f` leaves an unhandled gap between `69.999f` and `70.0f`.
   - Any float in `(69.999f, 70.0f)` falls through to the depleted branch (`else`), assigning `-35f` to high-energy tasks.
   - Observation 1.3 empirically confirms that `usableEnergy = 69.9995f` drops task score to 265.0 with `Energy matching = -35.0f`.
   - This causes erroneous prioritization and violates cognitive load energy matching contracts.
4. **Assessment**:
   Because both bugs produce severe, reproducible behavioral and mathematical anomalies in core engines, the work product cannot be approved in its current state.

---

## 3. Caveats
- No other areas of Milestone 1 were found defective: Room converters, migrations, startup async dispatching, and JUnit vintage engine tests are verified and passing.
- The two identified bugs are localized to math expressions and can be resolved without architectural refactoring.

---

## 4. Conclusion & Actionable Mitigations

### Verdict: REJECT

Milestone 1 is rejected due to two confirmed empirical defects:

### Mitigation 1: Fix Circadian Factor Transition in `EnergyScoreEngine.kt`
In `app/src/main/java/com/neuroflow/app/domain/engine/EnergyScoreEngine.kt`, align the pre-peak ramp with the circadian window profile.
Currently, `rawCircadian` at `minutesSincePeak = 0` is 0.0 because `PeakWindow` starts at offset 0 and cosine apex is at `durationMinutes / 2` (105 min).
To ensure a continuous circadian alertness curve:
- Option A: In `EnergyScoreEngine.kt:69-75`, ensure the circadian factor never drops below the wake baseline during the peak window's rise:
  ```kotlin
  val blendedCircadian = (0.35f + 0.65f * rawCircadian).coerceIn(0f, 1f)
  if (minutesWakeToPeak in 1..(12 * 60) && minutesSinceWake < minutesWakeToPeak) {
      val ramp = minutesSinceWake.toFloat() / minutesWakeToPeak.toFloat()
      (0.75f + 0.25f * ramp).coerceIn(0f, 1f)
  } else {
      maxOf(blendedCircadian, if (minutesSinceWake < minutesWakeToPeak + 105) 0.85f else 0.35f)
  }
  ```
  Or center the peak window at the peak anchor so that offset 0 is the apex (`cos(0) = 1.0`).

### Mitigation 2: Eliminate Float Range Hole in `TaskScoringEngine.kt`
In `app/src/main/java/com/neuroflow/app/domain/engine/TaskScoringEngine.kt`, lines 581 and 1040:
Replace:
```kotlin
usable >= 70f -> ...
usable in 40f..69.999f -> ...
else -> ...
```
With sequential threshold checks:
```kotlin
usable >= 70f -> ...
usable >= 40f -> ...
else -> ...
```
This guarantees contiguous partitioning over `[0f, 100f]` without floating-point dead zones.

---

## 5. Verification Method

### 5.1 Test Execution Commands
Run the empirical adversarial test harness:
```bash
./gradlew testDebugUnitTest --tests "com.neuroflow.app.domain.engine.Milestone1AdversarialChallengeTest" --info
```

Run the full project test suite:
```bash
./gradlew testDebugUnitTest
```

### 5.2 Invalidation Conditions
This rejection is invalidated and eligible for **APPROVE** when:
1. `EnergyScoreEngine.calculateDetailed` at `nowMinuteOfDay = peakMinuteOfDay` produces `circadianFactor >= 0.85f` and `usableEnergy >= 80f` (no cliff drop).
2. `TaskScoringEngine.score` with `usableEnergy = 69.9995f` awards `+10f` (Moderate) instead of `-35f` (Depleted) for `EnergyLevel.HIGH`.
3. All existing 688+ unit tests continue to pass.
