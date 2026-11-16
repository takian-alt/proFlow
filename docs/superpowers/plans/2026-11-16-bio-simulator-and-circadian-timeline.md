# Bio-Simulator & Dual-Process Circadian Timeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the interactive "Bio-Simulator" sandbox in the Flow Hub, dual-process Borbély ($C$ vs. $S$) curve visualization, 1-tap "Apply to Plan" scheduling, and the Plan Screen vertical circadian phase track.

**Architecture:** MVI / Clean Architecture in Jetpack Compose. `CircadianEnergyService` provides deterministic off-canvas timeline simulations; `FlowViewModelV2` manages ephemeral `SimulationState` and commits time-blocked slots to `UserPreferencesDataStore` / `TaskSchedulerService`; `EnhancedCircadianEnergyGraph` and `PlanScreenV2` render dual-process curves and circadian alignment ribbons.

**Tech Stack:** Kotlin 2.0+, Jetpack Compose (Material 3), Coroutines/Flow, Clean Architecture, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-11-16-bio-simulator-and-circadian-timeline-design.md`

## Global Constraints

- Host RAM ceiling ~7.4 GB: Always run `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1` with JVM bounds `-Xmx2048m -XX:MaxMetaspaceSize=512m`.
- 100% test greenness: Maintain 0 failures across all 1,460+ tests throughout all refactoring phases.
- Pure BYOK & Local Fallback: 100% offline, deterministic local simulation algorithms with zero cloud LLM dependencies or network blockers.
- Memory & Canvas safety: Never compute differential equations or timeline projections inside Canvas draw passes.

## Review Focus

1. **Past-Dip Nap Scheduling:** When `+20m Nap` is applied at 16:00 (past the 13:30–14:30 dip), slot placement must gracefully fallback to `now + 5m` instead of scheduling in the past.
2. **Division-by-Zero / Empty Timeline on Canvas:** `EnhancedCircadianEnergyGraph` must safely handle single-point or empty timelines without coordinate clipping or NaN path coordinates.
3. **Simulation Ephemerality:** Toggling simulation mode off or tapping `[Reset]` must completely restore the live unmasked timeline with zero orphaned state in preferences.
4. **Schedule Collision Rebalancing:** Applying a simulated nap or walk block must trigger 2-opt re-balancing so existing overlapping tasks are pushed smoothly rather than silently overwritten.
5. **Sleep Pressure Scale Inversion:** Canvas coordinate mapping for $S(t)$ must correctly map rising adenosine pressure ($S \to 1.0$) downward ($y_S = \text{padTop} + \text{chartHeight} \cdot (1 - S)$) to visually reflect fatigue pressing down on energy headroom.

---

### Task 1: Circadian Engine Simulation Helpers & Deterministic Insights

**Files:**
- Modify: `app/src/main/java/com/neuroflow/app/domain/service/CircadianEnergyService.kt`
- Test: `app/src/test/java/com/neuroflow/app/domain/service/CircadianSimulationTest.kt`

**Interfaces:**
- Consumes: `projectDayTimeline(...)`, `CircadianSnapshot`, `DaytimeNapBout`, `StimulantDoseBout`
- Produces:
  ```kotlin
  data class SimulationOutcome(
      val simulatedTimeline: List<CircadianSnapshot>,
      val netEnergyDeltaPercent: Int,
      val netFocusMinutesSaved: Int,
      val sleepPressureDeltaPercent: Int,
      val summaryInsight: String
  )

  fun simulateDayInterventions(
      baseTimeline: List<CircadianSnapshot>,
      dayStartMillis: Long,
      wakeTimeMillis: Long,
      lastSleepDurationMinutes: Int,
      stepMinutes: Int = 15,
      zoneId: ZoneId,
      simulatedNaps: List<DaytimeNapBout> = emptyList(),
      simulatedStimulants: List<StimulantDoseBout> = emptyList(),
      simulatedImpulses: List<CognitiveBlockWindow> = emptyList(),
      simulatedBedtimeDeltaMinutes: Int = 0,
      activeBlocks: List<CognitiveBlockWindow> = emptyList(),
      chronotype: ChronotypeProfile = ChronotypeProfile.INTERMEDIATE,
      manualPeakStartHour: Int? = null,
      learnedProfile: LearnedCircadianProfile? = null,
      rollingAverageSleepMinutes: Double? = null,
      socialJetlagLagHours: Double = 0.0
  ): SimulationOutcome
  ```

- [ ] **Step 1: Write the failing unit test for `simulateDayInterventions`**

```kotlin
// app/src/test/java/com/neuroflow/app/domain/service/CircadianSimulationTest.kt
@Test
fun testSimulatePowerNapDischargesSleepPressureAndBoostsAfternoonAlertness() {
    val service = CircadianEnergyService()
    val today = LocalDate.of(2026, 10, 18)
    val zoneId = ZoneId.of("UTC")
    val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

    val baseTimeline = service.projectDayTimeline(
        dayStartMillis = dayStart,
        wakeTimeMillis = wakeTime,
        lastSleepDurationMinutes = 480,
        stepMinutes = 15,
        zoneId = zoneId
    )

    val napStart = today.atTime(LocalTime.of(14, 0)).atZone(zoneId).toInstant().toEpochMilli()
    val simulatedNap = DaytimeNapBout(napStart, napStart + 20 * 60_000L, 20)

    val outcome = service.simulateDayInterventions(
        baseTimeline = baseTimeline,
        dayStartMillis = dayStart,
        wakeTimeMillis = wakeTime,
        lastSleepDurationMinutes = 480,
        stepMinutes = 15,
        zoneId = zoneId,
        simulatedNaps = listOf(simulatedNap)
    )

    assertTrue("Net energy delta should be positive", outcome.netEnergyDeltaPercent > 0)
    assertTrue("Sleep pressure delta should reflect discharge", outcome.sleepPressureDeltaPercent > 0)
    assertTrue("Insight should mention sleep pressure", outcome.summaryInsight.contains("sleep pressure", ignoreCase = true))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.neuroflow.app.domain.service.CircadianSimulationTest" --no-daemon -Dorg.gradle.workers.max=1`  
Expected: Compilation failure or missing `simulateDayInterventions` method.

- [ ] **Step 3: Implement `simulateDayInterventions` and deterministic insight generator in `CircadianEnergyService.kt`**

Add `SimulationOutcome` data class and `simulateDayInterventions` method using the ratified local formulas:
- $\Delta \text{Energy} = \frac{1}{M} \sum_{t \ge \text{now}} (E_{\text{sim}}(t) - E_{\text{base}}(t))$
- $\Delta S = \max_{t} (S_{\text{base}}(t) - S_{\text{sim}}(t)) \times 100$
- Parameterized deterministic template catalog for `summaryInsight`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.neuroflow.app.domain.service.CircadianSimulationTest" --no-daemon -Dorg.gradle.workers.max=1`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/domain/service/CircadianEnergyService.kt app/src/test/java/com/neuroflow/app/domain/service/CircadianSimulationTest.kt
git commit -m "feat(energy): add deterministic circadian simulation and insight generator"
```

---

### Task 2: Dual-Process Borbély & Ghost Simulated Curves on Canvas

**Files:**
- Modify: `app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnhancedCircadianEnergyGraph.kt`
- Test: `app/src/test/java/com/neuroflow/app/domain/service/CircadianEnergyServiceTest.kt` (or Compose test rule)

**Interfaces:**
- Consumes: `timeline: List<CircadianSnapshot>`, `simulatedTimeline: List<CircadianSnapshot>?`, `showSleepPressure: Boolean = true`
- Produces: Enhanced Canvas draw pass with:
  1. Primary cubic path for `usableEnergy`
  2. Secondary dashed path for `homeostaticPressureS` (scaled vertically $y_S = \text{padTop} + \text{chartHeight} \cdot (1f - S)$)
  3. Ghost cubic path for `simulatedTimeline` with translucent fill delta
  4. Updated legend (`Usable Energy`, `Sleep Pressure (S)`, `Simulated Delta`)

- [ ] **Step 1: Write unit test validating Process S calculation across timeline snapshots**

```kotlin
// In CircadianEnergyServiceTest.kt
@Test
fun testTimelineSnapshotsIncludeMonotonicallyAccumulatingSleepPressure() {
    val service = CircadianEnergyService()
    val today = LocalDate.of(2026, 10, 18)
    val zoneId = ZoneId.of("UTC")
    val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val wakeTime = today.atTime(LocalTime.of(7, 0)).atZone(zoneId).toInstant().toEpochMilli()

    val timeline = service.projectDayTimeline(dayStart, wakeTime, 480, 15, zoneId)
    val wakeIndex = timeline.indexOfFirst { it.timestampMillis >= wakeTime }
    val afternoonIndex = timeline.indexOfFirst { it.timestampMillis >= wakeTime + 8 * 3600_000L }

    assertTrue("Wake snapshot must have S < afternoon snapshot",
        timeline[wakeIndex].homeostaticPressureS < timeline[afternoonIndex].homeostaticPressureS)
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.neuroflow.app.domain.service.CircadianEnergyServiceTest.testTimelineSnapshotsIncludeMonotonicallyAccumulatingSleepPressure" --no-daemon -Dorg.gradle.workers.max=1`  
Expected: PASS.

- [ ] **Step 3: Implement Dual-Process & Ghost Curve rendering in `EnhancedCircadianEnergyGraph.kt`**

- In `EnhancedCircadianEnergyGraph`:
  - Add parameters `simulatedTimeline: List<CircadianSnapshot>? = null`, `showSleepPressure: Boolean = true`.
  - Calculate `sleepPressurePoints`:
    ```kotlin
    val pressurePoints = timeline.mapIndexed { i, snap ->
        val x = (i.toFloat() / (timeline.size - 1)) * chartWidth
        val y = padTop + chartHeight * (1f - snap.homeostaticPressureS.toFloat().coerceIn(0f, 1f))
        Offset(x, y)
    }
    ```
  - Draw `pressurePath` with dashed effect (`PathEffect.dashPathEffect(floatArrayOf(6f, 6f))`) in `NeuroFlowPaletteV2.InertiaLavender.copy(alpha = 0.85f)`.
  - When `simulatedTimeline != null`, compute `ghostPoints` for simulated usable energy and render dashed stroke with alpha fill delta.
  - Update `LegendItem` row to display "Energy", "Sleep Pressure", and "Simulated".

- [ ] **Step 4: Verify test suite greenness and compilation**

Run: `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1`  
Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/presentation/v2/flow/EnhancedCircadianEnergyGraph.kt
git commit -m "feat(ui): render dual-process Borbely sleep pressure and ghost simulation curves"
```

---

### Task 3: Simulation State Machine, Levers & "Apply to Plan" Commitment

**Files:**
- Modify: `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt`
- Modify: `app/src/main/java/com/neuroflow/app/presentation/v2/flow/CircadianHeroBarometer.kt`
- Modify: `app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt`
- Test: `app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelSimulationTest.kt`

**Interfaces:**
- Consumes: `CircadianEnergyService.simulateDayInterventions`, `UserPreferencesDataStore`, `TaskSchedulerService`
- Produces:
  ```kotlin
  data class SimulationState(
      val isActive: Boolean = false,
      val simulatedNaps: List<DaytimeNapBout> = emptyList(),
      val simulatedStimulants: List<StimulantDoseBout> = emptyList(),
      val simulatedImpulses: List<CognitiveBlockWindow> = emptyList(),
      val simulatedBedtimeDeltaMinutes: Int = 0,
      val outcome: SimulationOutcome? = null
  )
  ```
  - Methods in `FlowViewModelV2`:
    - `toggleSimulationMode(active: Boolean)`
    - `toggleSimulatedLever(leverType: SimulationLeverType)`
    - `resetSimulation()`
    - `applySimulationToPlan()`

- [ ] **Step 1: Write unit test for `FlowViewModelV2` simulation state transitions and commitment**

```kotlin
// app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelSimulationTest.kt
@Test
fun testToggleNapLeverUpdatesSimulationOutcomeAndApplyCreatesProtectedSlot() = runTest {
    // Verify toggleSimulationMode(true) -> toggleSimulatedLever(NAP) -> outcome != null
    // Verify applySimulationToPlan() saves ProtectedTimeSlot and resets simulation
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.neuroflow.app.presentation.v2.flow.FlowViewModelSimulationTest" --no-daemon -Dorg.gradle.workers.max=1`  
Expected: Compilation failure or missing methods.

- [ ] **Step 3: Implement `SimulationState`, levers, and "Apply to Plan" in `FlowViewModelV2.kt` and `CircadianHeroBarometer.kt`**

- In `FlowViewModelV2`:
  - Wire `SimulationState` into `FlowUiState`.
  - Implement `toggleSimulatedLever(type)`:
    - `NAP`: 20-min bout at postprandial dip (~13:30) or `now + 5m` if past 13:30.
    - `CAFFEINE`: 100mg caffeine at current hour.
    - `WALK`: 30-min brisk walk cognitive block window.
    - `BEDTIME`: -45m sleep shift.
  - Implement `applySimulationToPlan()`:
    - If nap simulated: Insert `ProtectedTimeSlot("Power Nap", 20)` into preferences + planned `DaytimeNapBout`, trigger auto-schedule 2-opt rebalance.
    - If walk simulated: Insert `ProtectedTimeSlot("Energy Walk", 30)`.
    - If bedtime simulated: Update `targetBedtimeHour` in `UserPreferencesDataStore`.
    - Reset `SimulationState.isActive = false`.
- In `CircadianHeroBarometer.kt`:
  - Add "⚡ Simulate" toggle pill in header.
  - Add expandable `SimulationTray` containing 1-tap chips, summary badge, and `[Reset]`, `[Apply to Plan]` buttons.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.neuroflow.app.presentation.v2.flow.FlowViewModelSimulationTest" --no-daemon -Dorg.gradle.workers.max=1`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelV2.kt app/src/main/java/com/neuroflow/app/presentation/v2/flow/CircadianHeroBarometer.kt app/src/main/java/com/neuroflow/app/presentation/v2/flow/FlowScreenV2.kt app/src/test/java/com/neuroflow/app/presentation/v2/flow/FlowViewModelSimulationTest.kt
git commit -m "feat(flow): implement bio-simulator levers, state machine, and Apply-to-Plan commitment"
```

---

### Task 4: Plan Screen Circadian Timeline Integration

**Files:**
- Modify: `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt`
- Modify: `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt`
- Test: `app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelCircadianTimelineTest.kt`

**Interfaces:**
- Consumes: `PlanUiState.energyTimeline: List<CircadianSnapshot>`, `ScheduledTask` / `TaskEntity`
- Produces:
  1. Left-margin 4dp vertical accent strip on `TimelineTaskCard` colored by start time `EnergyPhase`.
  2. Circadian alignment badge (`⚡ Optimal Peak Alignment`, `⚠️ Dip Mismatch`, `ℹ️ Cannibalizes Peak`).
  3. Sticky `CircadianHorizonMiniBar` below Cognitive Budget Meter.

- [ ] **Step 1: Write unit test validating task circadian alignment classification in `PlanViewModelV2`**

```kotlin
// app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelCircadianTimelineTest.kt
@Test
fun testTaskInPeakWindowReceivesOptimalAlignmentScore() {
    // High energy task scheduled at 10:00 AM (PEAK) resolves to Optimal Peak Alignment
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.neuroflow.app.presentation.v2.plan.PlanViewModelCircadianTimelineTest" --no-daemon -Dorg.gradle.workers.max=1`  
Expected: Compilation failure or missing helper.

- [ ] **Step 3: Implement circadian timeline strip and task alignment indicators in `PlanScreenV2.kt`**

- In `PlanViewModelV2`: Expose `dayTimeline: List<CircadianSnapshot>` in `PlanUiState`.
- In `PlanScreenV2.kt` (`TimelineScheduleView`):
  - Add compact 6dp horizontal `CircadianHorizonMiniBar` below the cognitive budget card.
  - In each task card, draw a 4dp vertical strip on the left border matching the scheduled hour's `EnergyPhase` color.
  - Render an alignment pill (`Optimal Peak Alignment` or `Circadian Dip Mismatch`).
  - Wire "Shift to Peak" button to `viewModel.shiftTaskToPeak(task.id)`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.neuroflow.app.presentation.v2.plan.PlanViewModelCircadianTimelineTest" --no-daemon -Dorg.gradle.workers.max=1`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelV2.kt app/src/test/java/com/neuroflow/app/presentation/v2/plan/PlanViewModelCircadianTimelineTest.kt
git commit -m "feat(plan): integrate vertical circadian phase ribbon and task alignment indicators"
```

---

### Task 5: End-to-End Test Suite Verification & Living Skill Synchronization

**Files:**
- Modify: `.agents/skills/vice-head/SKILL.md`
- Modify: `~/.gemini/config/skills/vice-head/SKILL.md`

- [ ] **Step 1: Execute complete unit test suite under memory invariants**

Run: `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1`  
Expected: 100% green pass rate across all 1,460+ tests.

- [ ] **Step 2: Synchronize Vice Head living skill files**

Update Section 5 (Living State & ADR Registry) and Section 6 in `.agents/skills/vice-head/SKILL.md` and `~/.gemini/config/skills/vice-head/SKILL.md` with ADR-062 (Bio-Simulator & Dual-Process Circadian Planning).

- [ ] **Step 3: Commit**

```bash
git add .agents/skills/vice-head/SKILL.md
git commit -m "docs(adr): record ADR-062 Bio-Simulator and Dual-Process Circadian Planning in living skill"
```
