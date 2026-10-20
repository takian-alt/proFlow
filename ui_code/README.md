# neuroFlow — UI Codebase Directory for Vice Head Handoff

This directory contains the unabridged source code for all UI surfaces, Jetpack Compose screens, ViewModels, themes, audio soundscape synthesizers, and widgets in **neuroFlow** (`takian-alt/proFlow`).

It is prepared specifically for transmission across the human bridge to the **Vice Head** (frontier AI assistant).

## 📦 Contents & Structure

1. **`ALL_UI_CODE.md`**: Single consolidated file containing all UI source files (~1.2 MB). Ideal for frontier models with large context windows (e.g. Gemini 1.5 Pro / Flash).
2. **Modular Parts (1 to 7)**: Chunked by architectural layer (each ~50 KB to ~340 KB) for platforms with smaller message limits (Claude, ChatGPT, etc.):

| Part File | Architectural Focus | File Count | Lines | Size |
| :--- | :--- | :---: | :---: | :---: |
| [`PART_1_THEME_NAVIGATION.md`](./PART_1_THEME_NAVIGATION.md) | **Theme Engine, Design Tokens, Typography & Navigation Hubs**<br>_Foundational theme palette (Obsidian, AMOLED, Light), dynamic tokens, typography, and root navigation topology._ | 7 | 1,369 | 53.6 KB |
| [`PART_2_FLOW_SCREEN.md`](./PART_2_FLOW_SCREEN.md) | **Flow Execution Cockpit & Circadian Barometer**<br>_FlowScreenV2, FlowViewModelV2, EnhancedCircadianEnergyGraph, CircadianHeroBarometer, and EnergyTimelineStream._ | 6 | 2,821 | 116.9 KB |
| [`PART_3_PLAN_COMPOSER.md`](./PART_3_PLAN_COMPOSER.md) | **Plan Screen, Scheduling Timeline & Smart Composer Sheet**<br>_PlanScreenV2, PlanViewModelV2, SmartTaskComposerSheetV2, PlanAndComposerUpgradesV2, and WeeklySchedule._ | 7 | 4,652 | 215.7 KB |
| [`PART_4_FOCUS_REGULATION.md`](./PART_4_FOCUS_REGULATION.md) | **Focus Deep Work Cockpit & Audio Soundscape Synthesizer**<br>_FocusScreenV2, FocusViewModelV2, FocusService, NeuralSoundscapePlayer/Generator, Solfeggio bell, and regulation sheets._ | 10 | 3,478 | 139.4 KB |
| [`PART_5_INSIGHTS_ANALYTICS.md`](./PART_5_INSIGHTS_ANALYTICS.md) | **Insights Hub, Chronobiology Analytics & Velocity Reports**<br>_InsightsScreenV2, InsightsViewModelV2, InsightsTabbedUpgradesV2, AnalyticsScreen/ViewModel, and HistoryScreen._ | 7 | 4,385 | 189.4 KB |
| [`PART_6_SETTINGS_NOTES.md`](./PART_6_SETTINGS_NOTES.md) | **Settings Hub, Sleep Logs, MEQ Quiz & Notes Workspace**<br>_SettingsScreen/ViewModel, MEQQuiz, SleepLogs, PriorityWeights, NotesScreen/Editor, and QuickNotesBottomSheet._ | 33 | 8,289 | 342.4 KB |
| [`PART_7_HYPERFOCUS_WIDGETS.md`](./PART_7_HYPERFOCUS_WIDGETS.md) | **HyperFocus Lock Engine, Gateways & Home Screen Widgets**<br>_HyperFocusHomeScreen/Activity, BlockingOverlay, GateTaskScreen, PermissionSetup, and Glance App Widgets._ | 26 | 4,560 | 184.9 KB |
| **Total** | | **96** | **29,554** | **1242.3 KB** |

3. **`raw/`**: Exact `.kt` Kotlin source files mirrored with relative package directory structure for direct code compilation or IDE inspection.

## 🚀 Recommended Protocol for Vice Head Review

- If your model has a **1M+ token context window**, upload or paste `ALL_UI_CODE.md` directly.
- If submitting in **stages or conversational turns**, submit `PART_1` through `PART_7` sequentially.
