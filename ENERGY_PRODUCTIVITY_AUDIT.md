# Energy & Productivity Audit — NeuroFlow / PROCUS

Date: 2026-10-10
Scope: the whole energy pipeline (sleep pressure → peak detection → moment energy → task scoring → auto-scheduling → UI guidance), plus the data-integrity and UX layers that feed or consume it.
Method: direct read of the pure engines and repositories, three parallel subsystem audits (scheduler, telemetry/focus capture, user-facing surfaces), and a numerical re-implementation of the scoring formulas (Appendix A).

Every finding carries `file:line`. **Verified** = checked by me in source or reproduced numerically. **Reported** = came from a subsystem audit with a line reference I did not independently re-open.

---

## Executive summary: why the app currently wastes user energy

The app has a genuinely impressive model — MEQ chronotype, three configurable circadian peak windows, sleep pressure, multi-horizon moment signals, a 23-factor priority engine, an energy-aware scheduler. The problem is not missing capability. It is that **the model's output scale does not match the thresholds that act on it**, and **the surfaces where decisions are actually made never read the live energy signal**.

Three compounding defects mean the app almost never says "do deep work now":

| # | Defect | Consequence |
|---|---|---|
| E-1 | Peak "reservoir" decays linearly to zero 16h after peak and stays clamped at 0 all pre-peak hours | Energy is 0 for every hour *before* your peak; the entire circadian profile is multiplied away |
| E-2 | Model **confidence is multiplied into the energy value** (not just into uncertainty) | Every user's energy is scaled down 25–55%; new users (confidence floor 0.45) can never exceed ~72% |
| E-5 | Guidance thresholds are `>=75` deep work / `>=50` medium / `>=30` admin | With E-1+E-2 the score rarely clears 75, so peak hours get demoted to "admin and interruption cleanup" |

Reproduced over a full day (confidence 0.70, realistic fatigue):

| Profile | Hours of 18 awake that trigger "Do deep work now" | Hours demoted to "admin/cleanup" |
|---|---|---|
| Intermediate, peak 09:30 | **0** | 13 |
| Intermediate, new user (conf 0.45) | **0** | 14 |
| Evening type, peak 12:30 | **0** | 13 |
| Definite morning, peak 06:00 | 1 | 11 |

So for three of four chronotypes, the app's top-tier recommendation is mathematically unreachable, and its own secondary/tertiary peak windows — which the user configured in Settings — contribute almost nothing because the reservoir has already decayed by the time they occur. The night-owl case is worst: their configured window at ~23:00 scores `circadian 0.74` but `peakScore 18`, because `reservoir = 0.22` caps it. **The app tells night owls to do admin work during their actual best hours.**

And because the fatigue model can never reach HIGH/CRITICAL for anyone who sleeps at all (S-1), the app has no way to notice chronic sleep restriction — exactly the user an energy app should protect.

Net effect today: energy modeling costs the user real effort (19-question quiz, daily manual sleep logging, affordance slider after every session, WOOP fields) and returns guidance that is compressed, jittery, unreachable at the top, and absent from the screens where tasks are actually chosen.

---

## Part A — Energy model (the math)

### E-1. Peak reservoir is a sawtooth that zeroes out half the day · **Verified** · Critical
`PeakEnergyEngine.kt:143-148`, `:156-162` → `EnergyScoreEngine.kt:50-54`, `:75`

`currentValueAt()` = `peakValue - minutesSincePeak * (peakValue/960)`, clamped at 0. `minutesSincePeak()` wraps through midnight, so every minute *before* today's peak reads as 1290–1439 minutes since peak → clamped to 0 → `reservoirFactor = 0` → `peakScore = 0`.

Two problems:
1. **Dead zone.** All pre-peak hours carry zero information, yet the value is consumed as a real signal (scheduler energy blocks, UI percentages).
2. **Discontinuity.** Energy snaps from 0 to maximum at the peak minute. A user watching the number sees it rocket from 46% to 61% between 09:00 and 10:00 with no change in their state.

Fix: make the reservoir a bounded *recovery* curve across the wake window (e.g. rise from wake to peak, decay after peak with a floor of ~0.4 rather than 0), or drop the reservoir term and let the circadian windows carry time-of-day shape — they already encode peak *and* secondary windows.

### E-2. Confidence multiplies into the energy value · **Verified** · Critical
`EnergyScoreEngine.kt:69`, `:75` (`peakScore = 100 * reservoir * circadian * confidence`); confidence floor `PeakEnergyEngine.kt:570` (`coerceIn(0.45f, 1f)`)

Confidence answers "how sure am I?" but is applied as "how much energy do you have?". A well-calibrated 0.7 model silently caps displayed energy at 70% of true; the 0.45 cold-start floor caps it at 45%. Model uncertainty should scale the *spread toward neutral*, not the *level*, and it must be shown to the user rather than baked into the score.

Related inconsistency (**Verified**): the confidence-gated abstention branch clamps to `≤0.4` (`:555-562`) while the normal path floors at `≥0.45` (`:570`) — abstaining can produce a lower number than the floor, but the two paths are 0.05 apart at best, so "abstain" barely changes anything while `EnergyScoreEngine` keeps multiplying it in either way.

### E-3. Circadian windows are spiky, and normalization couples amplitude to level · **Verified** · High
`EnergyScoreEngine.kt:96-127`

- `peakWindow()` returns a cosine that hits 0 at *both* edges, and the sum is floored to 0.35 elsewhere (`:62-67`). Result from the sim: `circadian` = 0.51 → 1.00 → 0.51 across 13:00/14:00/15:00. Hour-to-hour energy guidance swings by 2x purely from window edge geometry, and the scheduler's LOW/CRITICAL hard blocks flip with it.
- Normalization divides by `maxOf(amplitude).coerceAtLeast(1.0f)` (`:102`). Since the manual profile permits amplitudes down to 0.2 (`TaskScoringEngine.kt:121`, `PeakEnergyEngine.kt:300`), a user who lowers amplitude gets a globally lower curve — amplitude silently becomes a volume knob rather than peak shape. Overlapping windows sum past 1 and get coerced, flattening the curve into a plateau.

### E-4. Windows that cross midnight are unreachable · **Verified** · High
`EnergyScoreEngine.kt:120-122` — `endMinute = startMinute + durationMinutes`, and the guard `minute < startMinute || minute >= endMinute` never wraps. A window starting 23:00 with 120 min duration can never be active, because `wrapped` is always < 1440.

`TaskScoringEngine.kt:93-103` (`isMinuteInWindow`) handles the wrap correctly — proof the codebase knows the right pattern. Concretely: night owls and shift workers lose their evening peak entirely. This is the same population E-1/E-3 already penalize.

### E-5. Action thresholds don't match the achievable score range · **Verified** (numerically) · Critical
`EnergyInsight.kt:143-165` (`whatToDoNow`: 75 / 50 / 30), `:167-188` (`taskEnergyMismatchHint`: 60 / 35 / 75)

Thresholds are written for a 0–100 scale the pipeline cannot produce (see Executive summary). Fix is a pair: correct the scale (E-1/E-2) **and** derive thresholds from the user's own distribution (percentile-based: "your best 2 hours today") instead of absolute constants. Percentile framing also survives cold start.

### E-6. "No data" renders as medium energy · **Verified** · Medium
`EnergyScoreEngine.kt:50-54`, `:78-79` — with `peak == null`: reservoir 0, circadian 0, `rawEnergy = -fatiguePenalty`, `usableEnergy = (raw+100)/2` → 42–50%. Absence of signal is indistinguishable from moderate capacity, and higher than several genuine working hours. A cold-start user sees a plausible-looking number that is a constant. Surface "learning" state instead of a value (see T-4).

### E-7. Freshness logic says the opposite of the truth · **Verified** · High
`EnergyScoreRepository.kt:126-142`

```kotlin
val overallFreshnessAgeMillis = listOfNotNull(...).minOrNull() ?: 0L
```
- No sessions, no tasks (day 1) → list empty → `0L` → `hasRecentData = true` (`:207`) → `whatToDoNow` proceeds to give confident guidance for a user with zero data.
- Uses `min()` across sources, so one fresh signal masks stale ones.
- `sessionDataAgeMillis` derives from `startedAt` (`:126-129`), so during a 3-hour session the signal is flagged stale while the user is actively working.

Fix: use `max()` of relevant ages, treat empty as `Long.MAX_VALUE`, and drive `hasRecentData` from *observation* recency (`endedAt`/`updatedAt`), not session start.

### E-8. Confidence and chronotype inputs are mismeasured at the source · **Verified** · High
- `MEQChronotypeDetector.kt:46`: `confidence = answeredQuestions / 19`. MEQ items score ≥1, so anyone who finishes the quiz gets confidence ≈ 1.0. It measures completion, not certainty, and it seeds the whole stack (`PeakEnergyEngine.kt:526`). Manual profile paths hardcode `1f` too (`PeakEnergyRepository.kt:242-248`, `:959` — Reported).
- `PeakEnergyEngine.kt:215-223`: five chronotypes collapse to three offsets (moderate-morning = intermediate = 2.5h; moderate-evening = definite-evening = 5.5h). A 19-question instrument's discrimination is discarded, and it contradicts `MEQChronotypeDetector.kt:68-76`, where `DEFINITE_EVENING` peaks 15:00–21:00 but the anchor math yields wake+5.5h = 12:30 for a 07:00 waker — a ~2.5–5h disagreement between two subsystems describing the same thing.
- `MEQChronotypeDetector.kt:43`: `coerceIn(16, 86)` silently clamps malformed input instead of rejecting it.

### E-9. Moment energy: unit errors and "silence is flattering" · **Verified** · High
`MomentEnergyEngine.kt`

- `:124-128` — interruption weights (0.18/0.12/0.08) are *then* divided by 8, so ~44 interruptions are needed to saturate `interruptionLoad`. Interruptions — the most direct cause of wasted energy — are the weakest signal in the model. The `/8` reads as a leftover normalization.
- `:137` — `fatigueRatio(sleepPressurePoints)` uses the default `SOFT_MAX_REFERENCE`, ignoring the user-configured soft max that `EnergyScoreEngine.kt:70-73` does pass. Two fatigue scales in one pipeline: tuning the setting moves the baseline but not the moment term.
- `:140-145` — with no sessions/tasks/notifications, `supportScore = 0.55` (absence of interruptions reads as *supportive*), and `:188` floors confidence at 0.35, so ±8.4 of moment adjustment always applies. Cold-start users get confident, ungrounded boosts.
- `:25-52` — `trendStrength()` values range 0..5 (5-minute-equivalent focus) but diffs are clipped to ±1, so almost any change reads as a maximal trend.
- `:143` and `:150` — `taskPressure` is subtracted in support *and* added in pressure; `interruptionLoad` recurs in support and in `calmFactor` (`:160`). Double counting silently reweights the blend versus the documented intent.
- `EnergyScoreRepository.kt:321-338` — `summarizeRecentSessions` adds a session's **full** duration when it merely overlaps the window (`endedAt >= cutoff`), so a session that ended one minute ago fills the 5-minute bucket to capacity. Short-horizon features — and therefore the trend — are systematically inflated.
- `EnergyScoreRepository.kt:355-363` — overdue/due-soon counts use `status != COMPLETED`, so CANCELLED and archived tasks count as pressure and lower predicted energy. Cleaning up your list by cancelling tasks makes the app think you're more exhausted.

---

## Part B — Sleep pressure and recovery protection

### S-1. Sleep debt is mathematically impossible to represent · **Verified** (numerically) · Critical
`SleepPressureDetector.kt:22-27`, `:94-122`; applied in `SleepPressureRepository.kt:179-206`

Awake accrues **1 point/min**; a 90-min sleep cycle recovers **279 points** — 3.1x stronger than the wake that produced it. `applySleepSession` then floors at 0 (`:114`). Simulated, carrying 600 points of debt into a 16-hour wake day:

| Night length | Pressure after sleep | End of next day | Fatigue |
|---|---|---|---|
| 8 h | 0 | 960 | 39% |
| 6 h | 0 | 960 | 39% |
| 4 h | 0 | 960 | 39% |
| 3 h | 42 | 1002 | 41% |

Someone sleeping 4 h/night is modeled **identically** to someone sleeping 8 h. Showing HIGH requires ~21 h continuous wake; **CRITICAL requires 34.9 h with no sleep at all** — so the recovery-protective branch of the app is dead for every user who sleeps. This is the single most important "save energy" bug: the app cannot see the condition where protecting energy matters most, and it feeds that 39% ceiling into `fatiguePenalty`, capping energy further (compounding E-2).

Fix: make accrual non-linear (or recovery sub-linear), remove the hard 0 floor so debt carries across days, and calibrate `SOFT_MAX_REFERENCE` to a realistic single-day wake window (~960–1080 min, not 3000).

### S-2. Reference constants duplicated and mis-scaled · **Verified** · High
`SleepPressureDetector.kt:28` (`SOFT_MAX_REFERENCE = 3000`) and `PeakEnergyEngine.kt:23` (`SOFT_MAX_SLEEP_PRESSURE = 3000f`) are two constants for one concept — they will drift. Both encode 50 hours of wake as "maximum fatigue", ~3x too generous for a normal day, which is why the daily curve compresses to 0–39%.

### S-3. Recovery cliffs at 60 and 90 minutes · **Verified** · Medium
`SleepPressureDetector.kt:171-183` — a 60-min nap scores 186, a 59-min nap scores 59 (a 127-point cliff for one minute); 449 min scores 1360 vs 450 min at 1395. Users round sleep to "7.5 h" or "8 h", and the model's fatigue output jumps discontinuously at cycle boundaries. Bonuses should accrue proportionally.

### S-4. One mistyped sleep date can pin you at CRITICAL forever · **Verified** · High
`SleepPressureRepository.kt:85-97` sets `sleepPressureTrackingStartedAtMillis = min(existing, newStart)` with no lower bound and no future/past sanity check on the input (no validation in `addSleepLog`, `:51-59`). A past-dated log permanently anchors the recomputation walk, and accrual caps at `MAX_PRESSURE_POINTS = 14_400` (`SleepPressureDetector.kt:32`) → fatigue ratio coerced to 1.0 → the app reports CRITICAL every day, telling you to do only easy tasks. The only escape found is `clearSleepLogsAndReset()` (`:115-141`), which destroys all history.

Fix: bound tracking start to a rolling window (e.g. newest 30 days), validate log dates against now, and provide a non-destructive reset.

### S-5. Fabricated sleep logs become ground truth without consent · **Reported** · High
`SleepPressureRepository.kt:164-175`, `:247-322` — `AUTO_DEFAULT` intervals are inserted during a *read* path (`refreshCurrentPressure` writes to the DB) and then feed sleep pressure as if observed. If configured wake/sleep hours are wrong, fatigue is wrong permanently and invisibly. Also `atTime(sleepHour, 0)` (`:267-281`) throws on a nonexistent spring-forward hour, and the fixed `+= 24 * HOUR_MILLIS` across midnight is DST-wrong.

---

## Part C — Task prioritization (what to do next)

### P-1. The priority engine never reads live energy · **Verified** · Critical
`TaskScoringEngine.score()` (`:369-374`) takes `task`, `prefs`, `allActiveTasks`, `nowMillis` — no energy input. Energy logic is re-derived from static prefs and hardcoded clock ranges, e.g. `isLowEnergySlot = hour in 13..15` (`:397`). The assumption "everyone dips at 13:00–15:00" directly contradicts the chronotype model the rest of the app builds: for a night owl, 13:00–15:00 is the peak, and the engine rewards LOW-energy tasks there (`:503-507`) while penalizing HIGH-energy ones.

This is the disconnect with the largest productivity cost: the scheduler *does* consult energy (`AutoSchedulingEngine.kt:845`, `:1611-1696`), but the ranking that answers "what should I do right now?" does not.

### P-2. `isMorning` means "anytime before the peak" · **Verified** · High
`TaskScoringEngine.kt:396`: `isMorning = hour < peakContext.primaryStartHour`. For a `DEFINITE_EVENING` user with peak starting 19:00–21:00, `isMorning` is true from 00:00 to ~20:00 — almost the whole day. Because the `when` blocks test `isPeakHour` then `isMorning` **before** `isLowEnergySlot` (`:508`, `:523-527`, `:552-556`), an evening type at 14:00 gets `+30` analytical, `+60` frog-in-morning, and positive effort boosts — during what their own profile calls a dip. Morning-weighted "eat the frog" logic fires all day for the users it was least intended for.

Fix: define morning as a window after wake (`hour in wakeHour..wakeHour+3`, wrap-aware), and make it independent of peak start.

### P-3. Three conflicting definitions of "peak" summed into one score · **Verified** · High
In a single `score()` call: `resolvePeakContext` (`:169-204`) uses profile minute-windows; `categorySuitabilityScore` (`:275-284`) recomputes `isPeakHour` from `prefs.effectivePeakStart/End` **hours**; `:292-299` hardcodes chronotype end-hours. Terms 8, 9, 15.5 can therefore disagree about the current moment for the same task and cancel or double each other out. `scoreBreakdown` (`:744+`) faithfully shows all of them, so the user sees "Energy matching +70" next to "Category suitability −20" and cannot act on it.

Fix: resolve peak once per call into a single value (minute-precision, wrap-aware, energy-aware) and pass it to every term.

### P-4. Amplitude quantization can silently disable energy matching · **Verified** · Medium
`peakAmplitudeTier` (`:160-167`) returns 0 below amplitude 0.55; `isPeakHour = peakTierScale > 0f` (`:394-395`). Manual profiles permit amplitudes from 0.2 (`:121-131`), so a user who dials amplitude down to model a "gentle" peak exits peak detection entirely — all energy and circadian bonuses become 0, *including the −35 penalties* (`:501`). The tier steps (1.0/0.75/0.5/0) also make task ordering jump discontinuously as the adaptive model nudges amplitude across 0.55/0.75/0.9.

### P-5. Display score saturates exactly where discrimination matters · **Verified** · Medium
`THEORETICAL_MAX = 1200f` (`:65`) contradicts the file's own changelog ("recalibrated to actual component sum (~2445)"). Summing the documented maxima (300 + 625 + 170 + 150 + 100 + 60 + 55 + 40 + 70 + 60 + 120 + 180 + 100 + unbounded `sqrt(n)*80` + ...) far exceeds 1200, so `displayScore` (`:661-669`) clamps to 999 for genuinely top tasks. The 0–999 bar goes flat for the most important decisions — and this is the number the UI shows as "priority".

### P-6. Postponement is rewarded forever, with no decay · **Verified** · Medium
`:563`: `min(postponeCount * 30f, 180f)` — a permanent +180. Six postponements, and a task outranks everything in the list for the rest of its life. Combined with `:639-641` (+40/+60 for `isAnxietyTask`), dread is amplified without any energy-context gating: when the user is at their lowest capacity, the engine most strongly recommends the most intimidating task, which is precisely when an easy win is the right answer. Also note `:639-641` and `:409` can make an `ELIMINATE` quadrant task (+20 base, anxiety +60) outrank real `SCHEDULE` work.

Fix: exponential decay on `postponeCount`, and make the dread-surfacing boost conditional on predicted energy ≥ threshold, with a "2-minute starter step" variant when it isn't.

### P-7. Effort is conflated with importance · **Verified** · Medium
`autoAssignQuadrant` (`:1067`): `task.effortScore >= 70` counts as evidence a task is *important*. Difficulty ≠ importance, so hard-but-low-value tasks route to Q1 and compete for peak energy that should go to high-leverage work.

### P-8. Hour wrap breaks night owls and shift workers · **Verified** · Medium
`categorySuitabilityScore` (`:304-306`): `sleepHour.coerceIn(18, 27)` — a user who sleeps at 00:00 (`sleepHour = 0`) is coerced to 18, so wind-down begins at 16:00. `:288` `morningWindowEnd = (wakeHour + 3).coerceAtMost(23)` breaks the same way for a 22:00 waker: the hour right after waking (01:00) is not in the morning window. `:332` compounds it (`hour in (wakeHour+1)..(sleepHour.coerceAtMost(22)-1)`).

### P-9. Scoring is O(n³) and uses wall clock per call · **Verified** · Medium
`:571-574` splits `dependsOnTaskIds` while counting over `allActiveTasks`, inside a per-task `score()` called from a sort comparator (`AutoSchedulingEngine.kt:218`). `score()` and `scoreBreakdown()` each call `System.currentTimeMillis()` separately, so a breakdown can contradict the displayed score when the hour ticks between the two calls.

---

## Part D — Auto-scheduler (energy in action)

Reported by the scheduler audit; A-1, A-2, A-3, A-5 and A-9 are **Verified** in source.

### A-1. Tasks that don't fit are dropped silently · **Verified** · Critical for trust
`AutoSchedulingEngine.kt:469` — `?: return@forEach`: no candidate → no decision emitted, no telemetry, no user-visible reason. `AutoSchedulingRejectionReason` (`AutoSchedulingContracts.kt:33`) and `rejectionReason` (`:51`) are populated nowhere in the engine (only pre-checks in `NewTaskFlowViewModel.kt:326-328`); `NO_CAPACITY_IN_HORIZON`, `LOW_ENERGY_PROTECTION`, `PROTECTED_SLOT`, `OUTSIDE_WORKDAY` are dead enum values. The user sees tasks stay unscheduled with no explanation and no suggestion of what to drop.

### A-2. Earliness beats energy fit · **Verified** · High
`AutoSchedulingEngine.kt:432` adds `earlinessBonus` up to +0.25; `:454-455` selects within a 0.15 score band by `minByOrNull { startMillis }`. A slot that fits the user's peak loses to any adequate earlier slot — and `:450` prefers *any* today candidate (`score >= 0.30`) over the best future one. Note the intent is fine (energy is *also* the scarcest resource, so filling today's slack is a deliberate decision per `memory/scheduler-free-time-policy.md` — do not reintroduce a `workDayEnd` wall). What's wrong is the tie-break *inside* the day: given it's going on today, it should be at the best-fit moment, not the earliest adequate one. Weights also under-serve energy: `energyMatch 0.13 + categoryFit 0.18` vs `dayFrontLoad 0.20 + timeProximity 0.15` (`:1470-1527`).

### A-3. Fallback pass discards energy entirely · **Verified** · High
`:333-363` — when strict matching finds no candidate, the fallback loop checks only that the slot has capacity, is before the deadline and is under 0.90 utilization; it calls `energyDemandFitPenalty` but destructures `val (_, energyPenalty)` at `:350`, **throwing away `energyAllowed`** and applying `energyPenalty - 0.20f` (`:358`). A task whose energy demand is a hard mismatch is placed anyway with a slightly softened score. Deadline-urgent tasks bypass energy hard-blocks on the strict path too (`:316`).

### A-4. Two heavy tasks can sit 5 minutes apart · **Reported** · High
`:733` (inter-task `bufferMs` = 5 min), `:1099` (the "+10 transition" is bookkeeping only), `:536` (a real break is reserved only once accumulated cognitive minutes cross the interval, default 90), `:2142-2150` (only blocks 3+ consecutive heavy hours). Nothing prevents back-to-back cognitive load with no recovery — the pattern that produces the worst afternoon crashes.

### A-5. Burnout budget is leaky and one-sided · **Verified** · High
`calculateDailyCognitiveBudgetMinutes` `:2235` yields 75–170 min (floored at 60), then `canAssignWithoutBurnout` inflates it — `:2116` ×1.2 when a deadline is within 24 h, `:2117` ×1.35 whenever today has slack — and `:2128-2131` lets **a single task larger than the whole budget** through on an empty day (up to the 360-min realistic-duration cap at `:995`). And `:2107` — `if (!isCognitivelyIntense(task)) return true` — so ADMIN/PHYSICAL/ROUTINE volume is uncapped per day (`AutoSchedulingEngineDeadlineBufferTest.kt:189` asserts 7 h of ADMIN lands today). The only fatigue guard, `:2149`, is `fatigue >= 70 && nearBedtime` — see A-9 for why it almost never fires.

### A-6. Day index computed with fixed 86 400 000 ms · **Reported** · High
`:2184` — DST days are 23/25 h, so integer division truncates and tomorrow's cognitive load is billed to today, corrupting the burnout budget on the two days a year it matters most. Everything nearby uses `Calendar` correctly (`isSlotOnDay` `:2095`), so this is an outlier, not a convention.

### A-7. Tasks drift up to 4 h past their assigned slot · **Reported** · Medium
`findEarliestAvailableStartTime` (`:739`, `:791`) bounds candidates by `slot.endMillis + 4h`, and `latestAllowedEnd` (`:754`) is recomputed from the *drifted* start, so placement can cross midnight while `actualSlotIndex` (`:476`) silently falls back to `chosen.slotIndex`, desynchronizing utilization accounting from reality.

### A-8. Plan reshuffles itself; 60-min meeting assumption; dead safety logic · **Reported** · Medium
- `ScheduleAutoTasksWorker.kt:149` strips `scheduledDate/Time` from every non-anchor auto task each cycle and re-solves; `:549-551` refreshes any auto task older than 4 h when the toggle is on. The plan the user looked at moves on its own → users stop trusting the schedule.
- `:227-229` (and worker `buildBusySlotIndex:410`) block a whole hour for a 15-min calendar entry and under-block a 90-min meeting; `occupyTaskSlots:1093` assumes 60-min slots while `calculateCapacityHorizon:1281` trims the first slot shorter; `calculateUnusedDayMinutes:2077-2083` truncates, inflating the slack that unlocks A-5's overruns.
- `hasContiguousAvailability:1024`, `resolvePreferredLatestStartMillis:1790`, `calculateDeadlineSafetyBufferFit:1876`, `calculateOptimalSpacing:804` are never called — the elaborate deadline-safety buffer is **not** applied to placement.
- No weekend/rest-day concept (`calculateCapacityHorizon:1222-1324` builds Sat/Sun identically).
- `ScheduleAutoTasksWorker.kt:230` catches `Exception → Result.retry()` — a persistent failure retries forever.

### A-9. The scheduler's only live fatigue gate is dead twice over · **Verified** · High
`calculateSleepPressureFit` (`:2239-2255`) is the **only** place in the whole scheduler that reads the user's current sleep pressure, and it is short-circuited by `if (!lateEvening || pressure < 40) return 1.0f` (`:2245`). Two independent defects make it inert:
- **S-1 makes `pressure < 40` the normal case.** Even a 5-hour night barely moves `fatiguePercent`, so a chronically restricted user is scored as if fully rested. The scheduler never sees the one signal that should stop it stacking deep work at 23:00.
- **`:2244` and `:2144` both use `(prefs.sleepHour - 2).coerceAtLeast(18)`** for "late evening". For a user who sleeps at 01:00–04:00 (night owl, new parent, shift worker) `sleepHour - 2` is negative or small, so it clamps to **18:00** — their *best* hours get branded bedtime and penalized 0.35–0.8×. Same wrap bug family as P-8; the correct bound is the slot's distance from the actual sleep time on a circular clock.

Net: light-work days are uncapped (A-5), fatigue is invisible (S-1), and the guard written to catch late-night heavy work fires at the wrong hours for exactly the users who need it. Fix alongside item 8/9 — after fatigue becomes representable, re-key these two bounds on circular hours-before-sleep rather than a wall-clock floor.

---

## Part E — Telemetry quality (garbage in, wrong guidance out)

### T-1. The energy flow writes to the DB once per collector, once a minute · **Verified** (structure) · High
`EnergyScoreRepository.kt:79-291` — `observeEnergy()` is a **cold** flow whose `combine` lambda performs `recordEnergyPrediction(...)` (`:245-272`). Every collector (Launcher, Analytics, Focus, Matrix + the worker) runs the whole chain and inserts its own row every 60 s. Audit measured 3–5 duplicate rows/minute, ~5–7 k rows/day, and each collector also re-triggers full-table reads in peak detection (`PeakEnergyRepository.kt:276-278`). Beyond battery, this inflates the very telemetry the model learns from.
Fix: `SharingStarted.WhileSubscribed(5_000)` + sample on *material change* (or one shared writer), not on a per-collector ticker.

### T-2. Self-fulfilling peak windows (label leakage) · **Reported** · Critical for accuracy
`PeakEnergyRepository.kt:686-691` — a session is labeled positive if the task completed within 120 min of session end; `:763-776` lets those labels drift the peak toward the slots that produced them; the scheduler then routes the user to those same slots, generating more confirmations. No exploration/holdout. `:778-803` `weeklyBacktestError` measures distance between slots and the model's own windows rather than prediction vs. reality, so "backtest fit" improves as the model chases itself. Separately, `energy_predictions` rows are the model's own outputs, re-averaged as ground truth for analytics (`EnergyScoreRepository.kt:245-272` → `AnalyticsViewModel.kt:101-184`).

### T-3. Broken session accounting corrupts labels · **Reported** · High
- `LauncherViewModel.kt:577-591` — "Stop Session" writes `endedAt` with `durationMinutes = 0` and never updates task totals; `PeakEnergyRepository.kt:674-696` scores `0f..ABORT_MINUTES` durations as **aborts**. Stopping from the launcher therefore records a failure label for a possibly-successful session.
- `FocusViewModel.kt:590` leaves abandoned open rows in the DB; `FocusSessionManager.kt:21-28` reconstructs elapsed from wall clock, so a reopen the next day yields a huge duration (`EnergyScoreRepository.kt:320-322` caps open rows at 120 min, which is itself a silent distortion).
- `FocusSessionManager.kt:27`, `:104`, `FocusViewModel.kt:300` use `System.currentTimeMillis()` for elapsed time — no `elapsedRealtime` anywhere, so clock changes/NTP jumps corrupt durations.
- `FocusSessionManager.kt:55-71` — double pause overwrites `pausedAt` (losing the first span) and `pauseResumeCount` increments in both directions, inflating the interruption signal (which A-9 already under-weights).
- `TimeSessionDao.kt:39-43` buckets by `startedAt` only → midnight-straddling sessions land on the wrong day; `SUM(durationMinutes)` ignores open rows.

### T-4. Cold start looks calibrated · **Verified** · High
Confidence gates exist and are good (`PeakEnergyRepository.kt:44-53`, `:392-440`, 16-session gate) but only govern *personalization*; the MEQ baseline path emits confidence `1f`, and E-7 reports "fresh" with zero data. The result: day-1 users get firm, wrong guidance — and the app's most valuable asset (trust) is spent before the model can pay it back. Add an explicit "learning" UI state with a visible sample count.

### T-5. Prompt burden ignores the very model being trained · **Reported** · Critical for energy cost
Manual asks per day: affordance slider after **every** completion (`FocusViewModel.kt:497`), WOOP 4 free-text fields on first open (`WoopPromptSheet.kt:60-140`, `FocusViewModel.kt:201-217`), manual time log (`:459-467`), 3-step stop confirm (`:386-397`), manual sleep via 4 steppers in Settings (`SleepLogsScreen.kt:170-205`), goal-refill and fresh-start cards on launch (`MainActivity.kt:183-220`, `:244-277`). **None of these check predicted energy or peak window before interrupting** — they fire on screen entry/completion, so the app interrupts the user's best hours to ask metadata questions. Sleep could largely be inferred (screen-off/wake heuristics, Health Connect); the auto-tracker already exists (`FocusViewModel.kt:616-638`) and the launcher already knows elapsed time.

### T-6. Workers that fail silently, or never run · **Reported** · Medium
`NotificationWorker.kt:141-157`, `:186-212`, `:241-265`, `:295-331` have no `try/catch` in `doWork` (contrast `EnergyTelemetryCleanupWorker.kt:49-53`). `SleepPressureRefreshWorker.kt:76-77` uses `CANCEL_AND_REENQUEUE` on every app start and boot (`NeuroFlowApplication.kt:206-209`, `BootReceiver.kt:80`) — **Verified**: the daily 19:00 window restarts on each launch, so heavy users may run it rarely. Meanwhile `MainActivity.kt:296-301` recomputes pressure fully on every resume. `EnergyTelemetryCleanupWorker.kt:38` hardcodes 30-day retention and ignores the user preference read at `EnergyScoreRepository.kt:242-243`.

### T-7. Preference writes inside a flow transform · **Reported** · Medium
`PeakEnergyRepository.kt:482`, `:883` — `peakEnergyFlow` maps preferences while *writing* preferences (self-retriggering loop), mitigated only by volatile day/change checks (`:57-73`, `:592`). Fragile by construction.

### T-8. Crash-prone enum decoding · **Reported** · Medium
`Converters.kt:14-38` — `valueOf` unguarded except for `TaskType`: one corrupt restored row (post-restore, cloud sync) crashes cursor reads for the whole table. Add safe defaults.

---

## Part F — The app's own energy cost

### D-1. Interrupts are not energy-aware · **Verified** · High
`grep -c` for `energy|Energy|peak|Peak|fatigue|Fatigue` in `NotificationWorker.kt` returns **zero matches** across its 349 lines — the notification layer has no knowledge of the energy model at all. `:221`, `:275`, `:341` all post at `PRIORITY_HIGH`, `:244-279` batch a "deadline within 48 hours" escalation, `:203-224` add an evening streak nudge, and `NudgeSnoozeReceiver.kt:32-58` re-fires hourly after each snooze plus a confirmation notification. No quiet hours, no suppression inside a predicted peak window or an active focus session. The app's most damaging behavior — interrupting a peak window — is the one thing the model could prevent.

### D-2. Polling and startup cost · **Verified** (spot-checked) · High
`NeuroFlowApplication.kt:183-196` — foreground auto-schedule tick every **2–5 minutes** for the process lifetime; `:152-161` 15-min widget worker; `EnergyScoreRepository.kt:80-85` 60 s energy ticker per collector; `FocusViewModel.kt:255-263` 30 s score tick and `:331-341` 1 s timers; `LauncherViewModel.kt:554-568` — `isDefaultLauncher` `resolveActivity` every 2 s while subscribed (**and** the full O(n³) scoring pass at `:118-179`). `NeuroFlowApplication.kt:127-129` — **Verified**: `runBlocking { databaseCleaner.enforceRestoreVersionPolicy(...) }` on the main thread in startup path = ANR risk on cold start, the first impression a productivity app should not break.

### D-3. Telemetry volume · **Reported/derived from T-1** · Medium
~5–7 k prediction rows/day from duplicate collectors, plus full-table re-reads per emission. Battery drain *is* a productivity bug for an app whose thesis is energy management.

### D-4. `disableDnd()` clobbers the user's own setup · **Reported** · Medium
`FocusViewModel.kt:322-329`, `:362`, `:405`, `:589` force `INTERRUPTION_FILTER_ALL` unconditionally on stop/clear, overwriting a user-configured DND schedule.

---

## Part G — UX: where energy is actually spent

### U-1. Live energy never reaches the decision surfaces · **Verified** · Critical
`grep` of `presentation/today/TodayViewModel.kt` shows **zero** energy references; consumers of `observeEnergy` are Matrix, Focus, Analytics, Launcher `FocusTaskCard`, and the scheduler worker. The Today hero's `getRecommendationReason` (`TodayViewModel.kt:179`) and `TaskScoringEngine` (P-1) rank tasks with no knowledge of the user's state, and `ScheduleScreen` shows no peak-window shading even though the schedule worker computes exactly that (`ScheduleAutoTasksWorker.kt:162-218`). The most valuable screen ignores the most valuable signal — this is why fixing the math (Part A) is not sufficient on its own.

`EnergyInsight.whatToDoNow` / `whyEnergyChanged` / backtest readouts exist but live only under Analytics (`AnalyticsScreen.kt:80-370`) — not opened daily. Low-energy users never get the "easier win" suggestion where they'd act on it.

### U-2. Capture costs 5+ taps into a heavyweight form · **Reported** · High
Widget tap → `MainActivity` → possible goal/new-chapter full-screen gate → `NewTaskSheet` with When/Duration/Frog/Quadrant/Impact all pre-decision (`NewTaskSheet.kt:248-373`, `MainActivity.kt:244-288`). No true 1-field quick add, despite `quickBrainDump` defaults already existing (`TodayViewModel.kt:191`). Deep links from the "start focus" widget are gated behind the same modals (`MainActivity.kt:182-288`).

### U-3. The app asks users to tune what it should infer · **Reported** · High
11 scoring-weight sliders (`PriorityWeightsScreen.kt:172-245`) plus a full raw circadian editor — peak start/end, three window offsets in 0–1439 min, three durations, three amplitudes, moment sensitivities, AUTO/WORKDAY/WEEKEND (`SettingsScreen.kt:127-133`, `:450-644`) — while backtest/confidence machinery to auto-tune them already exists. This is expert configuration exposed to a person who just wants to know what to do next. Every slider also persists to DataStore on each drag step, un-debounced (`PriorityWeightsScreen.kt:175`, `SettingsScreen.kt:533`).

New tasks hard-default to `DO_FIRST`, impact 50, quadrant SCHEDULE (`NewTaskFlowViewModel.kt:393`, `:448`, `TodayViewModel.kt:191-209`) — so Q1 fills with undifferentiated items and the matrix loses its meaning.

### U-4. Five "next best action" surfaces, three stats surfaces · **Reported** · High
Today hero, `MatrixScreen.kt:292-484`, `FocusTaskCard.kt`, `FocusWidgetUpdateWorker.kt:66-91`, `AppDrawer.kt:633` — each with its own logic and visual grammar. `FocusScreen` stacks ~17 competing cards; `FocusTaskCard`'s energy "Insights" is 8+ diagnostic lines (`:345-576`). Four separate capture sinks (Today brain-dump, Notes, OnHold, widget) and three stats surfaces (`QuickStatsPanel`, Progress, Analytics). Scanning cost is paid by the user every time.

### U-5. Input loss and undo mismatch · **Reported** · High
`TodayScreen.kt:76-84` shows a `SnackbarDuration.Short` undo while the VM's real window is a 4000 ms `delay` (`TodayViewModel.kt:211-256`) — the button disappears before/after the action is actually revertible. Non-`rememberSaveable` state loses input on rotation/nav: `TodayScreen.kt:56`, `:173-205` (brain dump), `:353` (weekly intent), `PlanScreen.kt` (tab state). `NewTaskSheet.kt:178-185` double-save hazard on rapid taps.

### U-6. Empty states that lie, and theme/accessibility gaps · **Reported** · Medium
`TodayScreen.kt:218-220`, `:310-343`, `:586-617` shows "All Clear for Today!" whenever `recommendedTask == null`, even with an unscheduled backlog shown in a separate pill. `:629-664` hardcodes `Color(0xFF1E293B)` (black card in light mode). Sub-48dp targets on the primary completion affordances: `TaskRow.kt:56` (32dp circle), `TodayScreen.kt:749` (play, 36dp), `:813` (clear-X, 32dp), `OnHoldPlanningScreen.kt:304` (28dp), `SettingsScreen.kt:1029-1033` (32dp steppers), `SleepLogsScreen.kt:302`. The gesture users repeat most is the one that's hardest to hit.

### U-7. Recovery-hostile and trust-eroding copy · **Verified** · High
`BlockingOverlayScreen.kt:129-130`: **"Do you really want to become dumber?"** and **"Lol… you are still stupid"** — shown to someone actively fighting a craving. `:644-645` frames the emergency bypass as punishment. Elsewhere: weekly deltas colored error-red (`ProgressScreen.kt:116-119`), streaks and Seinfeld chains front-and-center with no rest-day concept (`QuickStatsPanel.kt`). Shame is expensive: it costs the user energy now and predicts abandonment later. The app already has a warm affirmations pool (`IdentityScreen.kt`) to use instead.

### U-8. Brand and UI-language inconsistencies · **Verified** · Medium
App is NeuroFlow; user-facing strings say "proFlow" in at least 8 places — `NeuroFlowApp.kt:208` ("Welcome to proFlow"), `MoreMenuSheet.kt:126`, `SettingsScreen.kt:841`, `AppGuideScreen.kt:34`, `:241`, `PrivacyPermissionsScreen.kt:90`, `LauncherKeepAliveService.kt:57` (a persistent notification title), `CloudBackupManager.kt:23`. `MEQQuizScreen.kt:181-186` marks a *selected* answer with a Close/X icon, which reads as "deselect".

### U-9. Dead and unreachable code shipped · **Reported** · Medium
NewTaskFlow step machine (`NewTaskFlowViewModel.kt:199-250`) and private `BasicInfoStep`/`ClassificationStep`/`RecurrenceStep` (`NewTaskSheet.kt:727`, `:766`, `:979`); `MatrixViewModel.kt:77-82` `addTask` auto-quadrant never called; launcher `QuotePage` + 51 quotes and `customQuotes` CRUD never rendered (`HomeScreen.kt:255-434`, `LauncherViewModel.kt:394-479`); `HomeScreen.kt:816-829` renders a "Quick Stats Panel (Coming in future task)" placeholder while the real panel exists; orphan `NavHost` routes for Matrix/Schedule/History/Analytics (`NeuroFlowApp.kt:317-373`). Also un-keyed lists (`AppDrawer.kt:879`, `IdentityScreen.kt:91`), unconditional `rememberInfiniteTransition` even when `hasPulse == false` (`MatrixScreen.kt:510`, `:986`), and non-memoized scoring during composition (`FocusTaskCard.kt:643-670`) — sluggishness is a real energy cost.

---

## Improvement roadmap

Ordered by **impact on productivity-per-unit-energy ÷ risk**. Nothing here requires new data sources or new permissions.

### Phase 1 — make the energy signal true (highest ROI, smallest diffs)

| # | Change | Why it helps | Effort / Risk | Files |
|---|---|---|---|---|
| 1 | Split **level** from **certainty**: `usable = f(reservoir, circadian)`, then blend toward neutral by confidence instead of multiplying; never apply a moment/energy block below a confidence floor | Removes the systematic 25–55% compression that makes "deep work" unreachable (E-2, T-4) | Low / Medium | `EnergyScoreEngine.kt:69-79` |
| 2 | Replace the sawtooth reservoir with a bounded wake-window curve (floor ≈0.4, pre-peak ramp) | Kills the dead zone and the 0→max discontinuity; makes 8 pre-peak hours usable signal (E-1) | Low / Low | `PeakEnergyEngine.kt:143-162` |
| 3 | Wrap-aware window math everywhere (single shared `MinuteWindow.contains(minute)` util) | Restores evening/night-owl/shift-worker windows (E-4, P-8) | Low / Low | `EnergyScoreEngine.kt:114-127`, `TaskScoringEngine.kt:93-103`, `:304-306` |
| 4 | Re-derive guidance thresholds from the user's own daily distribution (percentiles: "your best ~2 h") instead of 75/50/30 | Correct advice even before calibration; survives scale changes (E-5) | Medium / Low | `EnergyInsight.kt:143-188` |
| 5 | Feed live energy into `TaskScoringEngine.score()` as a parameter; delete hardcoded `13..15`; define `isMorning` relative to wake with wrap; resolve peak **once** per call | The ranking that picks "what next" finally uses the model (P-1, P-2, P-3) | Medium / Medium | `TaskScoringEngine.kt:369-400`, `:496-547` |
| 6 | Fix freshness: `max()` not `min()`, empty → `Long.MAX_VALUE`, age from `endedAt`/`updatedAt` | Stops "fresh" for zero-data users; stops "stale" mid-session (E-7, T-4) | Low / Low | `EnergyScoreRepository.kt:126-142`, `:207` |
| 7 | Make MEQ confidence reflect answer extremity and decay with contradiction; use per-chronotype offsets consistent with `baselinePeakWindow` | A 19-question quiz should produce 5 distinct profiles (E-8) | Medium / Low | `MEQChronotypeDetector.kt:46`, `PeakEnergyEngine.kt:215-223` |
| 8 | Recalibrate `SOFT_MAX_REFERENCE` to a realistic wake window; share one constant | End-of-day fatigue reads meaningfully instead of ceiling at 39% (S-1, S-2) | Low / Medium | `SleepPressureDetector.kt:28`, `PeakEnergyEngine.kt:23` |
| 8b | Re-key the scheduler's two "late evening" bounds on circular hours-before-sleep instead of `coerceAtLeast(18)`, and drop the `pressure < 40` short-circuit once fatigue is representable | The only live fatigue gate starts firing for the users it was written for (A-9) | Low / Low | `AutoSchedulingEngine.kt:2144`, `:2244-2245` |

### Phase 2 — protect recovery (the "save maximum energy" half)

| # | Change | Why it helps | Effort / Risk | Files |
|---|---|---|---|---|
| 9 | Let sleep debt accumulate across days: remove the hard 0 floor, make accrual super-linear late in the day, recovery sub-linear | The app can then *see* chronic restriction and protect it (S-1) — currently CRITICAL needs 35 h of wake | Medium / Medium | `SleepPressureDetector.kt:94-147` |
| 10 | Smooth the 60/90-min recovery cliffs (proportional bonus accrual) | Removes ±127-point swings from ordinary logging (S-3) | Low / Low | `SleepPressureDetector.kt:171-183` |
| 11 | Real inter-task buffer (configurable 10–15 min, not gated on the 90-min break threshold) + a per-day **total** focus-minute cap covering all task types | Stops back-to-back heavy blocks and 8–10 h admin days (A-4, A-5) while keeping the deliberate fill-today behavior | Medium / Medium | `AutoSchedulingEngine.kt:733`, `:536`, `:2107` |
| 12 | Surface unplaced tasks with reasons — populate `AutoScheduleRejectionReason`, show "N tasks didn't fit: why" + what to drop | Converts silent drops into an actionable trade-off decision (A-1) | Low / Low | `AutoSchedulingEngine.kt:469`, `AutoSchedulingContracts.kt:33-51` |
| 13 | Tie-break candidate selection on energy fit, then earliness; drop the "any today slot ≥0.30 over best future slot" preference for non-urgent work | Same fill-today intent, but the moment chosen is the one that fits the user's state (A-2, A-3) | Low / Medium | `AutoSchedulingEngine.kt:432-455`, `:317-345` |
| 14 | DST-safe day index (`Calendar` DAY_OF_YEAR) and bound placement to its slot/day | Removes 4 h drift, midnight crossings, and mis-billed burnout budget (A-6, A-7) | Low / Low | `AutoSchedulingEngine.kt:2184`, `:739-791` |
| 15 | Rest-day / non-working-weekday support in the capacity horizon | Prevents Sat/Sun being planned like Tue (A-8) | Medium / Low | `AutoSchedulingEngine.kt:1222-1324` |

### Phase 3 — stop the app spending the energy it's managing

| # | Change | Why it helps | Effort / Risk | Files |
|---|---|---|---|---|
| 16 | Notification policy: quiet hours + suppress nudges inside predicted peak / active session; try/catch + retry in all workers; `UPDATE_AND_RETAIN` instead of `CANCEL_AND_REENQUEUE` | Protects the best hours instead of interrupting them (D-1, T-6) | Low / Low | `NotificationWorker.kt:100-331`, `SleepPressureRefreshWorker.kt:76-77` |
| 17 | One shared `observeEnergy` (`shareIn`) + event-based telemetry writes; slow the foreground tick to ~15 min | ~95% fewer DB writes and full-table re-reads; less battery and heat (T-1, D-2, D-3) | Medium / Medium | `EnergyScoreRepository.kt:79-291`, `NeuroFlowApplication.kt:183-196` |
| 18 | Move `runBlocking` DB cleanup off the startup path | No cold-start ANR on first impression (D-2) | Low / Low | `NeuroFlowApplication.kt:127-129` |
| 19 | Prompt diet: batch WOOP to once per project; affordance slider only when forecast-error history exists; 1-step stop; skip any non-critical prompt during peak or high-fatigue | Every prompt costs energy; today none check state (T-5) | Medium / Low | `FocusViewModel.kt:386-497`, `WoopPromptSheet.kt:60-140` |
| 20 | Infer sleep from device wake/first-interaction + screen-off windows (and/or Health Connect); demote manual logs and auto-fallback to corrections only | Removes 4 daily steppers for the single most important input (T-5, S-5) | Medium / Medium | `SleepLogsScreen.kt:170-205`, `SleepPressureRepository.kt:247-322` |
| 21 | Collapse weights to 4 named presets + hide the raw circadian editor behind "Advanced"; debounce persistence | Removes ~15 sliders from non-experts; defaults to the adaptive profile the app already computes (U-3) | Medium / Medium | `PriorityWeightsScreen.kt:172-245`, `SettingsScreen.kt:450-644` |
| 22 | Session janitor: finalize orphaned open rows on startup/worker; set `durationMinutes` on stop; `elapsedRealtime` checkpoints; don't score 0-duration as abort | Largest data-integrity win — mislabeled aborts currently poison training labels (T-3) | Medium / Medium | `LauncherViewModel.kt:577-591`, `FocusSessionManager.kt:21-71`, `PeakEnergyRepository.kt:674-696` |
| 23 | Break the label loop: score divergence against post-guidance outcomes with a small exploration holdout; stop re-averaging predictions as ground truth | Otherwise the model keeps confirming its own suggestions (T-2) | High / Medium | `PeakEnergyRepository.kt:686-803`, `AnalyticsViewModel.kt:101-184` |

### Phase 4 — put the signal where decisions happen, and cut friction

| # | Change | Why it helps | Effort / Risk | Files |
|---|---|---|---|---|
| 24 | Energy-aware Today hero: use `whatToDoNow` + live energy in `getRecommendationReason`; when low, promote a short easy win; when high, promote the frog | The highest-traffic screen starts consuming the model (U-1) | Medium / Low | `TodayViewModel.kt:152-209`, `TodayScreen.kt:218-343` |
| 25 | Peak-window shading + "why here" chips on the Schedule timeline (data already computed) | Makes the schedule legible as an energy plan rather than a list (U-1) | Low / Low | `ScheduleScreen.kt`, `ScheduleAutoTasksWorker.kt:162-218` |
| 26 | One true 1-field quick add (title → save); full sheet behind "more options"; remove goal/new-chapter modals from widget/deep-link paths | Cuts the two most repeated flows to their minimum (U-2) | Medium / Low | `NewTaskSheet.kt:248-373`, `MainActivity.kt:182-288` |
| 27 | One shared next-action engine + one visual grammar across Today/Matrix/launcher/widget; demote secondary surfaces to summaries | Removes five divergent answers to one question (U-4) | Medium / Medium | `MatrixScreen.kt:292-484`, `FocusTaskCard.kt`, `AppDrawer.kt:633` |
| 28 | Rewrite blocking/emergency copy to the existing affirmations pool; neutral (not punitive) bypass framing; add rest days to streak framing | Shame costs energy and predicts churn; the pool already exists (U-7) | Low / Low | `BlockingOverlayScreen.kt:113-130`, `:644-645` |
| 29 | Undo window = snackbar duration (single 5 s source of truth); `rememberSaveable` for capture/dialog/plan state; guard double-submit | Ends lost input and trust leaks (U-5) | Low / Low | `TodayScreen.kt:56-84`, `:353`, `NewTaskSheet.kt:178-185` |
| 30 | Enlarge repeated-affordance targets to ≥48dp; theme-safe card colors; fix "All Clear" empty state to respect backlog | The completion tap is the app's most-used gesture (U-6) | Low / Low | `TaskRow.kt:56`, `TodayScreen.kt:218-664` |
| 31 | Decay `postponeCount`, gate anxiety-surfacing on energy state, drop `effortScore` from importance, and give the "dread" path a 2-minute starter step | Stops the engine from pushing your worst task at your worst hour (P-6, P-7) | Medium / Medium | `TaskScoringEngine.kt:563`, `:639-641`, `:1067` |
| 32 | Fix `THEORETICAL_MAX` to the real component sum (or normalize by observed percentile); memoize derived scores, key lists, guard infinite transitions, hoist composition-time scoring | Restores score discrimination at the top and removes jank (P-5, U-9) | Low / Medium | `TaskScoringEngine.kt:65`, `:661-669` |
| 33 | Global brand sweep to one name; MEQ selected-answer icon to a check; delete dead code (step machine, `QuotePage`, `addTask`, placeholder panel, orphan routes) | Trust, and a smaller surface to break (U-8, U-9) | Low / Low | 8 sites, see U-8 |

### Suggested test coverage to add alongside Phases 1–2
Existing tests are strong on the scheduler (`AutoSchedulingEngine*Test` — capacity, dependency, DST, break reservation, work boundary) and thin exactly where the above bugs live. Add:
1. **Guidance reachability test** — for each chronotype and confidence in {0.45, 0.7, 0.9}, assert `whatToDoNow` emits `DEEP WORK` for at least N hours inside the model's own primary window. Fails today for 3 of 4 types; regression-proofs items 1–4.
2. **Midnight-crossing window test** — window at 23:00 + 120 min must be active at 00:30 (item 3).
3. **Sleep debt accumulation test** — three consecutive 5-h nights must produce measurably higher fatigue than 8 h (item 9; fails by ~5x today).
4. **Wrap/`isMorning` test for evening types** — assert no morning boost at 14:00 for `DEFINITE_EVENING` (item 5).
5. **Label-integrity test** — launcher stop must not be scored as abort (item 22).
6. **Idempotency test** — two `planAutoSchedule` runs on unchanged inputs produce identical placements (A-7 stability).
7. **Fatigue-gate reachability test** — with 5 h of sleep for 3 nights, assert `calculateSleepPressureFit` returns < 1.0 for a heavy task placed 2 h before the configured sleep time, for both an 08:00-sleep and a 01:00-sleep user (item 8b; fails today because `pressure < 40` short-circuits and `coerceAtLeast(18)` mislabels the night owl's best hours).

---

## What NOT to change (deliberate decisions found during this audit)

- **Fill-today past `workDayEnd`.** The scheduler filling today's leftover hours even beyond the configured workday end is an intentional decision recorded in project memory (`memory/scheduler-free-time-policy.md`): energy is *also* the scarcest resource, so today's slack should not be wasted. Items 11/13 above respect this — they constrain *volume* and improve the *choice of moment within today*, not the right to plan past `workDayEnd`.
- **The confidence-gated abstention machinery and the 16-session personalization gate** (`PeakEnergyRepository.kt:44-53`, `:392-440`) are good design; the problem is that confidence is miscomputed (E-8) and misapplied (E-2), not that abstention exists.
- **The 23-factor priority theory stack** is well-conceived. The defect is duplicated, contradictory peak definitions inside it (P-3), not its breadth.

## Appendix A — how the numbers were produced
`/tmp/energy_sim.js` re-implements `SleepPressureDetector.fatigueRatio/recoveryForSleepMinutes` and `EnergyScoreEngine.{reservoir, circadianFactor, peakScore, usableEnergy}` plus `EnergyInsight.whatToDoNow` thresholds verbatim from the cited lines, then walks the day hourly for four chronotype/wake/confidence combinations. Key outputs: `usableEnergy ≥ 75` reached 0/18 awake hours for intermediate and evening types; 1 h for definite morning; `fatigueRatio` at 960 pressure points = 39%; CRITICAL requires 2095 awake minutes with no sleep.

## Appendix B — verification status
Personally re-opened and confirmed in source: all of Part A, Part B (S-1..S-4), P-1..P-8, E-5/E-7/E-8 numerically, A-1 (silent drop + dead `rejectionReason`), A-2 (`earlinessBonus` / `minByOrNull { startMillis }` / today-preference), **A-3** (fallback destructures `energyAllowed` away at `:350`), **A-5** (`:2107` non-cognitive early return, `:2116-2117` budget inflation, `:2128-2131` long-task exception), **A-9** (`:2239-2255` sole fatigue gate + `:2144`/`:2244` `coerceAtLeast(18)`), U-1 (zero energy refs in `TodayViewModel`), **D-1** (zero `energy|peak|fatigue` matches in `NotificationWorker.kt`), D-2 (`grep` of `runBlocking`, polling interval, `CANCEL_AND_REENQUEUE`), U-5 (exact lines), U-7 (exact lines), U-8 (naming sites), T-1 structure. Reported with line references from the three subsystem audits and not independently re-opened: most of Part E (T-2, T-3, T-5 details), A-4, A-6, A-7, A-8 internals, D-4, U-2/U-3/U-4/U-6/U-9 counts. Treat the reported figures (e.g. "~5–7 k rows/day", "17 cards on FocusScreen") as directionally accurate pending a quick check of the cited lines.
