# Original User Request

## 2026-10-15T21:09:25Z

Rebuild and modernize the Android application neuroFlow (in /home/takian/Project/PROCUS), transforming it from a complex, bug-ridden productivity dashboard into a clean, neuroscience-backed, highly automated execution engine that drastically reduces cognitive friction.

Working directory: /home/takian/Project/PROCUS
Integrity mode: development

## Requirements

### R1. Core Engineering & Math Pipeline Fixes
- Fix Borbély Two-Process and Peak Energy engines: continuous baseline without zeroing pre-peak hours, support circadian midnight wrap for evening chronotypes, and decouple confidence factor from baseline energy magnitude.
- Connect live energy state directly to task scoring and auto-scheduling decisions.
- Harden Room type converters against unknown enum deserialization, fix database migrations non-destructively, and resolve main thread startup blocking (runBlocking in Application class).
- Configure test runners so all JUnit 4 & JUnit 5 test suites execute properly without being silently skipped.

### R2. Minimal-Friction Task Creation & Smart Inference
- Implement single-field quick task capture that auto-infers task type, estimated duration, energy demands, and context tags using lightweight heuristics/keywords.
- Convert monolithic task creation dialogs into a streamlined ModalBottomSheet with progressive disclosure.

### R3. Modern Design System & Accessible UI
- Establish a consistent Material 3 design system supporting Light, Dark, and AMOLED modes with zero hardcoded contrast-breaking colors (especially on FocusScreen and Today/Flow screens).
- Ensure all interactive elements conform to Android accessibility guidelines (minimum 48x48dp touch targets, semantic roles).
- Consolidate navigation into 3 clean core hubs (Flow / Plan / Insights) eliminating orphan routes and legacy dead drawer code.

### R4. Supportive Focus & Tiered HyperFocus
- Rebuild Focus Mode with clean Pomodoro/custom timers, collapsible WOOP/implementation intentions, and affordance difficulty feedback.
- Replace punitive/hostile blocking copy with constructive, supportive interventions across three user-selectable tiers (Zen, Shield, Fortress).

### R5. Auto-Scheduling Transparency & Behavioral Insights
- Decompose the auto-scheduling engine into modular components that provide transparent placement rationales and actionable deferral suggestions instead of silent drops.
- Implement behavioral pattern insights (productivity peaks, estimation errors) that adaptively inform scheduling and prioritization.

## Acceptance Criteria

### Verification & Testing
- [ ] `./gradlew testDebugUnitTest` runs all unit test suites without missing JUnit 4 tests, and all engine tests pass.
- [ ] Application cold start executes without disk I/O or `runBlocking` on the Android UI thread.
- [ ] Database migration from v11 to v12 executes cleanly without dropping user notes or data.

### Functional & UI Correctness
- [ ] Quick task creation allows instant creation with title only, auto-inferring sensible duration and energy tags.
- [ ] Focus screen and dashboard display correctly in both pure Dark and AMOLED themes with readable contrast and no hardcoded light pastel backgrounds.
- [ ] All primary interactive icons/buttons have touch target dimensions of at least 48dp.
- [ ] Auto-scheduling reports clear placement status or actionable deferral reasons for any unplaced tasks.
