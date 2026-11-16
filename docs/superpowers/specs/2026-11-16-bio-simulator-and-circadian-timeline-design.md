# Design Document: Bio-Simulator & Dual-Process Circadian Timeline (Approach 1)

**Date:** 2026-11-16  
**Status:** Approved for Implementation Planning  
**Target Architecture:** Android / Jetpack Compose / Clean Architecture (`neuroFlow`)  

---

## 1. Intent & The Why

Users currently experience three distinct points of friction in neuroFlow's daily circadian planning workflow:
1. **The Sleep Pressure Disconnect:** While the Borbély Two-Process mathematical engine (`CircadianEnergyService`) computes homeostatic sleep pressure ($S$), the canvas renderer in `EnhancedCircadianEnergyGraph` only plots a single composite line (`usableEnergy`). Users cannot see their adenosine debt accumulating or visualize how power naps discharge sleep pressure. In addition, when no sleep log is recorded today, the graph defaults to an ideal 8-hour sleep baseline, appearing static and disconnected from real bodily fatigue.
2. **Lack of "What-If" Agency:** Users cannot simulate or forecast how biological actions (taking a 20-min power nap, drinking coffee at 1:30 PM vs 4:30 PM, taking a brisk walk, or shifting bedtime) would alter their energy curve or prevent an afternoon crash before committing to those actions.
3. **Plan Screen Disconnect:** In `PlanScreenV2` (`TimelineScheduleView`), tasks are listed as time blocks without visual context of the underlying circadian energy or sleep pressure backdrop, making it difficult to gauge whether high-effort tasks collide with afternoon dips.

This project introduces an interactive **"Bio-Simulator" Sandbox Mode** directly within the Flow Hub hero card, visualizes the authentic **Borbély Dual-Process curves** ($C$ vs. $S$), allows 1-tap **"Apply to Plan" scheduling**, and integrates a **circadian phase indicator ribbon** alongside scheduled tasks on the Plan Screen.

---

## 2. Goals, Non-Goals & Anti-Goals

### Goals
- **Dual-Process Borbély Visualization:** Render both the composite Usable Energy curve and the Homeostatic Sleep Pressure ($S$) trace on `EnhancedCircadianEnergyGraph`, clearly displaying adenosine accumulation and nap clearance drops.
- **Interactive Bio-Simulator Sandbox:** Provide a "Simulate" mode in `CircadianHeroBarometer` with 1-tap intervention chips (`+20m Nap`, `+100mg Caffeine`, `+30m Walk`, `Shift Bedtime`) that dynamically compute and render a ghost comparison curve and $\Delta \text{Energy}$ delta.
- **Calendar Time-Block "Apply to Plan" (User Decision Option A):** Committing simulated actions creates dedicated time-blocked rest/break slots on the Plan schedule (e.g., "😴 20m Power Nap", "🚶 30m Energy Walk", "☕ Planned Caffeine Dose"), updates tonight's target bedtime in preferences, and triggers 2-opt rebalancing to adjust surrounding tasks.
- **Plan Screen Circadian Ribbon:** Integrate a sleek vertical circadian phase track on task cards in `TimelineScheduleView` in `PlanScreenV2`, color-coded by circadian phase (Peak, Dip, Ramp, Wind Down) with task energy fit indicators.
- **Strict BYOK & Memory Invariants:** Zero hardcoded API keys, 100% deterministic local calculation, bounded Compose allocations, no heavy recomposition loops on 60 Hz Canvas draws.

### Non-Goals
- Adding external wearable Bluetooth syncing (e.g. Oura/Whoop live GATT streaming) in this phase.
- Changing the underlying mathematical constants of ADR-061 ($\tau_{\text{wake}}$, $\tau_{\text{recovery}}$, Hill masking).
- Replacing the 2-opt scheduler; rather, using it to accommodate simulated blocks when applied.

### Anti-Goals
- **No Fidget Spinner Complexity:** Do not introduce overwhelming multi-step forms or dense spreadsheets. Presets must work in 1–2 taps.
- **No Canvas Stuttering:** Never calculate heavy differential equations directly on the Compose UI draw pass. All simulations run in background coroutines.
- **No Destructive Overrides:** "Simulate" mode is purely ephemeral until the user explicitly taps "Apply to Plan".

---

## 3. Constraints & Operating Invariants

1. **Host RAM Ceiling:** Build and test commands must adhere strictly to `--no-daemon -Dorg.gradle.workers.max=1` with JVM cap `-Xmx2048m -XX:MaxMetaspaceSize=512m`.
2. **100% Test Greenness:** Zero regressions across all 1,460 unit tests.
3. **Pure BYOK & Local Fallback:** Simulation logic relies entirely on local algorithmic evaluation (`CircadianEnergyService.projectDayTimeline`); no cloud AI calls required for simulation.

---

## 4. Approaches Considered & Trade-Offs

### Approach 1: Integrated "Sandbox Mode" in Flow + Plan Energy Backdrop *(Selected & User Ratified)*
- **Description:** Adds a "Simulate" toggle to `CircadianHeroBarometer`, rendering dual-process curves ($C$ vs. $S$) and ghost what-if lines on `EnhancedCircadianEnergyGraph`. Features 1-tap quick intervention chips and an "Apply to Plan" CTA. Plan Screen timeline receives a matching vertical energy ribbon.
- **Why Selected:** Delivers immediate, high-context feedback directly on the screens users already visit dozens of times daily. Eliminates modal navigation friction while visually demonstrating the Borbély dynamics.

### Approach 2: Dedicated "Energy Strategy Lab" Modal
- **Description:** A standalone full-screen bottom sheet with detailed sliders for every micro-parameter.
- **Why Dropped:** Adds navigation friction; separates the simulation from the live moment-to-moment timeline view.

### Approach 3: Proactive Smart Recommendation Cards Only
- **Description:** Automatic AI cards without manual sandbox controls.
- **Why Dropped:** Restricts user agency and experimentation; users specifically requested the ability to test what-if actions themselves.

---

## 5. Architectural & Implementation Specifications

### 5.1 Dual-Process Borbély Visualization (`EnhancedCircadianEnergyGraph.kt`)
The canvas renderer is enhanced to support three synchronous traces:
1. **Usable Energy Curve ($E(t)$):** The existing primary monotone cubic path, styled with phase-adaptive gradients and glowing ambient stroke.
2. **Homeostatic Sleep Pressure Trace ($S(t)$):**
   - Plotted as a distinct, dashed secondary path in lavender (`NeuroFlowPaletteV2.InertiaLavender` / `Color(0xFFA78BFA)`).
   - Starts low at wake ($S_0$), monotonically climbs throughout wakefulness, and drops sharply during naps ($S_{\text{mid-nap}}$).
   - Mapped vertically: $y_S = \text{padTop} + \text{chartHeight} \cdot (1f - S(t))$, giving the intuitive biological visual of rising fatigue pressure pushing down on circadian drive.
   - Legend indicator added: `Usable Energy` (Solid Gradient) vs. `Sleep Pressure (Process S)` (Dashed Lavender).
3. **Simulated Ghost Curve ($E_{\text{sim}}(t)$):**
   - Active only when `isSimulating == true`.
   - Drawn as a high-contrast electric dashed path with translucent fill between $E_{\text{sim}}(t)$ and $E_{\text{baseline}}(t)$ indicating net energy gain or saved focus capacity.

### 5.2 Flow Hub Simulation Sandbox (`CircadianHeroBarometer.kt` & `FlowViewModelV2.kt`)
- **State Model:**
  ```kotlin
  data class SimulationState(
      val isActive: Boolean = false,
      val simulatedNaps: List<DaytimeNapBout> = emptyList(),
      val simulatedStimulants: List<StimulantDoseBout> = emptyList(),
      val simulatedPhysiologicalImpulses: List<CognitiveBlockWindow> = emptyList(),
      val simulatedBedtimeHourDelta: Double = 0.0,
      val simulatedTimeline: List<CircadianSnapshot> = emptyList(),
      val netEnergyDeltaPercent: Int = 0,
      val netFocusMinutesSaved: Int = 0,
      val summaryInsight: String = ""
  )
  ```
- **1-Tap Quick Levers:**
  - `[😴 +20m Power Nap]`: Scheduled for the upcoming postprandial dip (~13:30–14:30) or `now + 5m` if the dip has passed.
  - `[☕ +100mg Caffeine]`: Scheduled for the current hour; validates SWS cutoff warning if within 8h of bedtime.
  - `[🚶 +30m Walk / Exercise]`: Injects physiological impulse (+8% alertness for 90m).
  - `[🌙 Sleep 45m Earlier]`: Models next-day sleep debt reduction ($S_0$ reduction).
- **Local Deterministic Calculations (Builder Call Ratified):**
  - $\Delta \text{Energy} = \frac{1}{N} \sum (E_{\text{sim}}(t_i) - E_{\text{base}}(t_i))$ across the remainder of the waking day.
  - $\text{FocusMinutesSaved} = \text{round}\left(\sum_{\text{blocks}} \Delta \text{depletion} \times 60 + \text{napMinutes} \times 1.25\right)$.
  - `summaryInsight` generated from a deterministic template catalog:
    - Nap: `"Clears ${sDelta}% sleep pressure, lifting afternoon alertness by +${energyDelta}%."`
    - Caffeine: `"Boosts peak alertness by +${energyDelta}%; Slow-Wave Sleep safe."`
    - Walk: `"Transient +${energyDelta}% alertness surge via physiological activation."`
    - Bedtime: `"Reduces tomorrow's baseline sleep debt, expanding peak focus by ${mins}m."`
- **"Apply to Plan" Execution (User Ratified Option A):**
  - **Nap:** Creates a `ProtectedTimeSlot(name = "Power Nap", durationMinutes = 20, isEnabled = true)` + planned `DaytimeNapBout`, and triggers `scheduleDayWithOutcome` 2-opt re-balancing.
  - **Walk:** Creates a `ProtectedTimeSlot(name = "Energy Walk", durationMinutes = 30, isEnabled = true)`.
  - **Caffeine:** Creates a calendar notification / reminder block before cutoff.
  - **Bedtime Shift:** Updates `targetBedtimeHour` in `UserPreferencesDataStore`.

### 5.3 Plan Screen Circadian Timeline Integration (`PlanScreenV2.kt`)
- **Vertical Circadian Phase Track (Builder Call Ratified):**
  - Maintains `TimelineScheduleView`'s existing, stable `LazyColumn` architecture.
  - Each task card (`TimelineTaskCard`) is augmented with a left-margin 4dp vertical accent strip indicating the task's start time `EnergyPhase` (Peak = Emerald, Surge = Cyan, Morning Ramp = Mint, Rebound = Indigo, Dip = Amber, Wind Down = Violet, Crash = Rust).
- **Task Energy Alignment Indicator:**
  - High Energy task in Peak -> `⚡ Optimal Peak Alignment` (Green)
  - High Energy task in Dip -> `⚠️ Circadian Dip Mismatch (-20%)` (Amber)
  - Low Energy task in Peak -> `ℹ️ Cannibalizes Peak Focus` (Subtle)
  - One-tap quick action: "Shift to Peak Window" directly invokes `viewModel.shiftTaskToPeak(taskId)`.
- **Top Circadian Horizon Mini-Bar:**
  - A compact, non-intrusive 6dp horizontal gradient bar below the Cognitive Budget Meter summarizing the day's peak and dip windows.

---

## 6. Verification & Test Plan

1. **Circadian Energy Engine Verification:**
   - Unit tests validating `projectDayTimeline` when supplied with simulated naps, stimulants, and physiological events.
   - Assertions verifying Process S discharge reflects accurately in `homeostaticPressureS`.
2. **ViewModel State Machine Verification:**
   - Tests for `FlowViewModelV2`: toggling simulation mode, applying simulated levers, computing deterministic deltas, and resetting state.
   - Tests for "Apply to Plan" writing protected slots and triggering 2-opt re-balance.
3. **UI Composition & Draw Verification:**
   - Verification of `EnhancedCircadianEnergyGraph` rendering `usablePoints`, `sleepPressurePoints`, and `ghostPoints` without division-by-zero or coordinate clipping.
4. **Regression Safety:**
   - Full suite execution: `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1`.
   - Maintain 100% pass rate (1,460+ tests).
