# Handoff Report: R1 Calm Sensory Design System ("Obsidian Sanctuary")

**Agent**: explorer_v2_1  
**Timestamp**: 2026-10-16T13:18:30Z  
**Scope**: R1. Calm Sensory Design System ("Obsidian Sanctuary")  
**Target Milestone**: neuroFlow V2 UI Modernization

---

## 1. Observation

### 1.1 Existing Theme & Token Definitions
1. **Classic Theme System**:
   - `app/src/main/java/com/neuroflow/app/presentation/common/theme/Theme.kt`:
     - Line 16: `val LocalIsDarkTheme = staticCompositionLocalOf { false }`
     - Lines 18–43: `private val LightColorScheme = lightColorScheme(...)`
     - Lines 45–70: `private val DarkColorScheme = darkColorScheme(...)`
     - Lines 72–115: `fun NeuroFlowTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true, amoledDark: Boolean = false, content: @Composable () -> Unit)`
     - Lines 96–104: AMOLED dark mode switches `background` and `surface` to `Color.Black` (`#000000`), and `surfaceVariant` to `Color(0xFF121212)`.
   - `app/src/main/java/com/neuroflow/app/presentation/common/theme/Color.kt`:
     - Contains legacy `NeuroFlowColors` object.
     - Line 7: `val Purple = Color(0xFF5C5FA8)`
     - Lines 18–20: `val DoFirstBg = Color(0xFFFDE2E2)`, `val DoFirstText = Color(0xFFC62828)`, `val DoFirstTextDark = Color(0xFFEF5350)`
     - Lines 21–23: `val ScheduleBg = Color(0xFFDFF4E6)`, `val ScheduleText = Color(0xFF2E7D32)`, `val ScheduleTextDark = Color(0xFF81C784)`
     - Lines 24–26: `val DelegateBg = Color(0xFFFFF1C7)`, `val DelegateText = Color(0xFFF57F17)`, `val DelegateTextDark = Color(0xFFFFB74D)`
     - Lines 27–28: `val EliminateBg = Color(0xFFE8EDF5)`, `val EliminateText = Color(0xFF424242)`
     - Line 31: `val FocusBg = Color(0xFFE8F5E9)` (legacy light green pastel)
     - Lines 34–36: `val DeadlineCard = Color(0xFFFFEBEE)`, `val ScheduledCard = Color(0xFFE3F2FD)`, `val PomodoroCard = Color(0xFFF3E5F5)`
     - Lines 39–40: `val TrackingRed = Color(0xFFD32F2F)`, `val LockedGray = Color(0xFF424242)`
     - Line 45: `val MapeBad = Color(0xFFBF360C)`
     - Lines 48–53: `DarkBackground = Color(0xFF121212)`, `DarkSurface = Color(0xFF1E1E1E)`, `DarkSurfaceAlt = Color(0xFF2C2C2C)`, `DarkText = Color(0xFFE1E1E1)`, `DarkTextMuted = Color(0xFFA0A0A0)`, `DarkBorder = Color(0xFF383838)`
   - `app/src/main/java/com/neuroflow/app/presentation/common/theme/Type.kt`:
     - Lines 8–20: `NeuroFlowTypography` Material 3 typography definitions.

2. **Modern V2 Theme System ("Obsidian Sanctuary")**:
   - `app/src/main/java/com/neuroflow/app/presentation/v2/theme/NeuroFlowThemeV2.kt`:
     - Lines 7–34:
       ```kotlin
       object NeuroFlowThemeV2Colors {
           val Obsidian = Color(0xFF0A0B0E)
           val SlateSurface = Color(0xFF14171F)        // R1 Requirement: #13151B
           val SlateSurfaceAlt = Color(0xFF1C202B)     // R1 Requirement: #1C1F28
           val SlateBorder = Color(0xFF282D3D)         // R1 Requirement: #2A2E3B
           val SlateDivider = Color(0xFF222736)

           val ElectricIndigo = Color(0xFF6366F1)      // R1 Requirement: #6366F1
           val ElectricIndigoDark = Color(0xFF4F46E5)
           val ElectricIndigoGlow = Color(0xFF818CF8)

           val SageGreen = Color(0xFF10B981)           // R1 Requirement: #10B981
           val SageGreenDark = Color(0xFF059669)
           val SageGreenContainer = Color(0xFF064E3B)

           val WarmAmber = Color(0xFFF59E0B)           // R1 Requirement: #F59E0B
           val WarmAmberContainer = Color(0xFF78350F)

           val RoseCoral = Color(0xFFF43F5E)           // R1 Requirement: #F43F5E (Muted Coral)
           val RoseCoralContainer = Color(0xFF881337)

           val TextPrimary = Color(0xFFF8FAFC)
           val TextSecondary = Color(0xFF94A3B8)
           val TextTertiary = Color(0xFF64748B)
       }
       ```
     - Lines 36–67: `NeuroFlowV2DarkColorScheme = darkColorScheme(...)`
       - `primary = ElectricIndigo`
       - `secondary = SageGreen`
       - `tertiary = WarmAmber`
       - `error = RoseCoral`
       - `background = Obsidian`
       - `surface = SlateSurface`
       - `surfaceVariant = SlateSurfaceAlt`
       - `outline = SlateBorder`
       - `outlineVariant = SlateDivider`
     - Lines 70–76:
       ```kotlin
       @Composable
       fun NeuroFlowThemeV2(content: @Composable () -> Unit) {
           MaterialTheme(
               colorScheme = NeuroFlowV2DarkColorScheme,
               typography = Typography(),
               content = content
           )
       }
       ```
     - Notably, `NeuroFlowThemeV2` does not take `amoledDark: Boolean` or provide pure black `#000000` overrides.

### 1.2 Audit of Alarmist Red, Hardcoded Colors, and Shaming Badges
1. **Alarmist Red (#C62828, #D32F2F, #E53935) & Shaming Elements**:
   - `TodayScreen.kt:284`:
     ```kotlin
     SectionHeader("Overdue", badge = "${uiState.overdueTasks.size}", badgeColor = NeuroFlowColors.DoFirstText)
     ```
     Uses `#C62828` for an alarming overdue badge with raw count.
   - `TodayScreen.kt:1051`: `color = NeuroFlowColors.DoFirstText` (`#C62828`).
   - `FocusScreen.kt:276, 289`: `uiState.urgencyFraction > 0.8f -> NeuroFlowColors.DoFirstText` (`#C62828` urgency alert).
   - `FocusScreen.kt:687`: `Text("⏰ DEADLINE", fontWeight = FontWeight.Bold, color = NeuroFlowColors.DoFirstText, fontSize = 12.sp)` (`#C62828`).
   - `TaskRow.kt:148`: `if (task.energyLevel == EnergyLevel.HIGH) add("🔴 High energy")` (red circle emoji).
   - `TaskRow.kt:155`: `if (task.goalRiskLevel == 2) add("🚨 Critical")` (red alarmist siren emoji).
   - `TaskRow.kt:185`: `Priority.HIGH -> NeuroFlowColors.DoFirstText` (`#C62828` red priority dot).
   - `TaskRow.kt:249`: `Quadrant.DO_FIRST -> NeuroFlowColors.DoFirstText` (`#C62828`).
   - `PlanScreenV2.kt:471`:
     ```kotlin
     Quadrant.DO_FIRST to ("🔴 Do First (Urgent & High Impact / Frog)" to NeuroFlowThemeV2Colors.RoseCoral)
     ```
     Contains alarmist `🔴` emoji in quadrant header.
   - `AnalyticsScreen.kt:188`: `FatigueZone.CRITICAL -> Color(0xFFE53935)`.
   - `AnalyticsScreen.kt:234, 259, 282, 1097, 1404, 1451`: Multiple conditional fallbacks to `Color(0xFFC62828)`.
   - `AnalyticsScreen.kt:904`: `Text("Deferred ${task.postponeCount}×", fontSize = 12.sp, color = NeuroFlowColors.MapeBad)` (uses harsh `#BF360C` for postpone shaming).
   - `MatrixScreen.kt:675, 680`: `Color(0xFFE53935)` used for urgent deadline badges.
   - `ScheduleScreen.kt:1030`: `val indicatorColor = Color(0xFFE53935)`.
   - `BlockingOverlayScreen.kt:334`: `color = Color(0xFFD32F2F)`.
   - `TimeUtils.kt:20, 28, 42`: Copy strings `"Overdue by ${diffDays} days"`, `"Overdue"`.

2. **Light Pastel Backgrounds Breaking Dark/AMOLED Contrast**:
   - `TaskRow.kt:199–202`:
     ```kotlin
     val chipColors = listOf(
         NeuroFlowColors.LightSurfaceAlt, NeuroFlowColors.PomodoroCard, NeuroFlowColors.ScheduleBg,
         NeuroFlowColors.DelegateBg, NeuroFlowColors.DoFirstBg, Color(0xFFE0F1F2)
     )
     ```
     Hardcoded light pastel backgrounds (`#FDE2E2`, `#FFF1C7`, `#DFF4E6`, `#F3E5F5`, `#E0F1F2`) on task chips clash violently in dark/AMOLED modes.
   - `Theme.kt:33, 60`: `errorContainer = NeuroFlowColors.DoFirstBg` (`#FDE2E2` in light mode, `#381519` in dark mode).

### 1.3 AMOLED & Dark Theme Toggling
1. `UserPreferencesDataStore.kt`:
   - Line 54: `val theme: AppTheme = AppTheme.SYSTEM`
   - Line 162: `val dynamicColorEnabled: Boolean = true`
   - Line 163: `val amoledDarkEnabled: Boolean = false`
   - Line 165: `val useV2Ui: Boolean = false`
2. `MainActivity.kt`:
   - Lines 135–144: Wraps composition in `NeuroFlowTheme(darkTheme = isDark, dynamicColor = preferences?.dynamicColorEnabled ?: true, amoledDark = preferences?.amoledDarkEnabled ?: false)`.
   - Line 302: When `prefs.useV2Ui` is true, renders `NeuroFlowAppV2`.
3. `NeuroFlowAppV2.kt`:
   - Line 41: Nests `NeuroFlowThemeV2 {}` inside `NeuroFlowAppV2`.
   - `NeuroFlowThemeV2` does not take `amoledDark` from preferences, so AMOLED black background (`#000000`) is never activated in V2.
4. `SettingsScreen.kt`:
   - Exposes `useV2Ui` toggle (lines 105–138).
   - Does NOT expose `theme` (LIGHT/DARK/SYSTEM) or `amoledDarkEnabled` switches to the user.

### 1.4 Test Coverage Analysis
1. `Tier1FeatureTest.kt` (lines 517–557):
   - F11 test cases assert AMOLED mode pure black (`0xFF000000`), WCAG AA text contrast (>= 4.5:1), elimination of legacy pastel green (`0xFFE8F5E9`), dark card container contrast (>= 4.5:1), and button contrast (>= 3.0:1).
2. `Tier2BoundaryTest.kt` (lines 472–498):
   - Asserts pure white on pure black = 21.0:1, disabled text contrast >= 2.5:1, and divider-to-surface contrast >= 1.3:1.
3. `Tier3CrossFeatureTest.kt` (lines 139–149, 176–181):
   - Asserts 21:1 contrast on AMOLED and shield intervention contrast >= 4.5:1.
4. `./gradlew testDebugUnitTest` runs 34 tasks and passes 100%.

---

## 2. Logic Chain

1. **Token Delta in V2**:
   - *Observation*: In `NeuroFlowThemeV2.kt`, `SlateSurface` is `Color(0xFF14171F)`, `SlateSurfaceAlt` is `Color(0xFF1C202B)`, and `SlateBorder` is `Color(0xFF282D3D)`.
   - *Requirement*: R1 specifies Elevated Slate surfaces as `#13151B` and `#1C1F28`, and subtle borders as `#2A2E3B`.
   - *Inference*: Updating these 3 token values in `NeuroFlowThemeV2Colors` aligns `NeuroFlowThemeV2` 100% with the R1 specification while preserving backwards compatibility across all V2 screens that reference `NeuroFlowThemeV2Colors.SlateSurface` and `MaterialTheme.colorScheme.surface`.

2. **Contrast Verification**:
   - *Observation*: Calculated WCAG relative luminances and contrast ratios with the R1 tokens:
     - TextPrimary (`#F8FAFC`) on SlateSurface (`#13151B`) = 17.44:1 (exceeds AAA 7.0:1)
     - TextSecondary (`#94A3B8`) on SlateSurface (`#13151B`) = 7.12:1 (exceeds AAA 7.0:1)
     - SageGreen (`#10B981`) on SlateSurface (`#13151B`) = 7.19:1 (exceeds AAA 7.0:1)
     - WarmAmber (`#F59E0B`) on SlateSurface (`#13151B`) = 8.50:1 (exceeds AAA 7.0:1)
     - MutedCoral (`#F43F5E`) on SlateSurface (`#13151B`) = 4.97:1 (exceeds AA 4.5:1)
     - SubtleBorder (`#2A2E3B`) vs SlateSurface (`#13151B`) = 1.35:1 (satisfies `Tier2BoundaryTest` ratio >= 1.3:1)
     - Pure AMOLED Black (`#000000`) vs Pure White (`#FFFFFF`) = 21.0:1 (satisfies `Tier1FeatureTest` and `Tier2BoundaryTest`)
   - *Inference*: The R1 palette strictly adheres to and surpasses all WCAG AA/AAA and test harness contrast requirements.

3. **Elimination of Alarmist Red**:
   - *Observation*: Overdue badges in `TodayScreen.kt:284`, `FocusScreen.kt:687`, and priority dots in `TaskRow.kt:185` use `NeuroFlowColors.DoFirstText` (`#C62828`). Furthermore, `TaskRow.kt` and `PlanScreenV2.kt` use alarmist red emojis (`🔴`, `🚨`).
   - *Requirement*: R1 requires "Zero Alarmist Red: Replace punishing red overdue warnings with supportive, low-anxiety visual indicators to prevent amygdala threat responses."
   - *Inference*:
     - Softening `NeuroFlowColors.DoFirstText` from `#C62828` to `MutedCoral` (`#F43F5E`) or `WarmAmber` (`#F59E0B`) globally neutralizes alarmist red across all classic screens.
     - In V2 `PlanScreenV2.kt:471`, replacing `"🔴 Do First"` with `"🎯 Do First"` or `"⚡ Do First"` removes the alarmist visual trigger.
     - Changing overdue badges from punitive counts to supportive copy ("Ready when you are", "Gentle rollover") satisfies the ADHD cognitive safety net requirement.

4. **AMOLED Theme Architecture in V2**:
   - *Observation*: `MainActivity.kt` collects `preferences?.amoledDarkEnabled`, but `NeuroFlowAppV2` ignores it and statically instantiates `NeuroFlowThemeV2`.
   - *Inference*: `NeuroFlowThemeV2` should accept an optional `amoledDark: Boolean = false` parameter. When true, `background` and `surface` are set to `Color.Black` (`#000000`) instead of `#0A0B0E` / `#13151B`, allowing true pure black AMOLED power savings while defaulting to the Obsidian Sanctuary palette.

---

## 3. Caveats & Risks

1. **Backward Compatibility with Classic V1 Screens**:
   - Multiple classic screens (`TodayScreen`, `MatrixScreen`, `AnalyticsScreen`) still import `NeuroFlowColors`. If `NeuroFlowColors.DoFirstText` is modified directly in `Color.kt`, it gracefully neutralizes alarmist red in classic screens without touching 30+ separate UI files. However, any external expectations on `#C62828` must be considered.
2. **Gradle Javac Compilation with Clean Rerun**:
   - Running `./gradlew testDebugUnitTest` passes 100%. Running with `--rerun-tasks` directly against single unit test classes causes AGP KSP intermediate javac dependency ordering errors on Room DAOs if classes are not pre-packaged. Standard `./gradlew testDebugUnitTest` should always be used.
3. **Settings UI Missing Theme Controls**:
   - `UserPreferencesDataStore` holds `amoledDarkEnabled` and `theme`, but `SettingsScreen.kt` currently only exposes `useV2Ui`. A future worker could expose the AMOLED Dark toggle in Settings to allow users to toggle pure black mode.

---

## 4. Conclusion

The current neuroFlow codebase already has the scaffolding for V2 (`NeuroFlowThemeV2.kt`, `NeuroFlowAppV2.kt`), but requires four specific adjustments to achieve 100% compliance with R1 Calm Sensory Design System ("Obsidian Sanctuary"):
1. Align the exact hex codes in `NeuroFlowThemeV2Colors`:
   - `SlateSurface`: change `0xFF14171F` -> `0xFF13151B`
   - `SlateSurfaceAlt`: change `0xFF1C202B` -> `0xFF1C1F28`
   - `SlateBorder`: change `0xFF282D3D` -> `0xFF2A2E3B`
   - Add explicit alias `MutedCoral = RoseCoral` (`0xFFF43F5E`)
2. Add `amoledDark: Boolean = false` support to `NeuroFlowThemeV2(amoledDark: Boolean = false, content: @Composable () -> Unit)` and wire it from `NeuroFlowAppV2`.
3. Eliminate alarmist red across both V1 and V2:
   - In `PlanScreenV2.kt:471`: replace `"🔴 Do First"` with `"🎯 Do First"`.
   - In `TaskRow.kt:148, 155`: replace `"🔴 High energy"` with `"⚡ High energy"`, and `"🚨 Critical"` with `"⏳ Priority"`.
   - In `NeuroFlowColors` (`Color.kt`): soften `DoFirstText` from `0xFFC62828` to `0xFFF43F5E` (Muted Coral), and `DoFirstBg` from `0xFFFDE2E2` to `0xFF1C1F28` (Slate Surface Alt).
   - In `TaskRow.kt:199–202`: replace hardcoded pastel light chip backgrounds with theme-adaptive slate containers (`MaterialTheme.colorScheme.surfaceVariant`).
4. Update `TodayScreen.kt:284` overdue badge to use `WarmAmber` / `MutedCoral` with supportive copy rather than alarming red.

---

## 5. Concrete Implementation Recommendations for Worker

### Recommendation 1: Update `NeuroFlowThemeV2.kt`
**File**: `app/src/main/java/com/neuroflow/app/presentation/v2/theme/NeuroFlowThemeV2.kt`
- Update hex values in `NeuroFlowThemeV2Colors`:
  ```kotlin
  val SlateSurface = Color(0xFF13151B)        // Exactly matches R1
  val SlateSurfaceAlt = Color(0xFF1C1F28)     // Exactly matches R1
  val SlateBorder = Color(0xFF2A2E3B)         // Exactly matches R1
  val MutedCoral = Color(0xFFF43F5E)          // Canonical R1 token name
  val RoseCoral = MutedCoral                  // Compatibility alias
  ```
- Support `amoledDark` in `NeuroFlowThemeV2`:
  ```kotlin
  @Composable
  fun NeuroFlowThemeV2(
      amoledDark: Boolean = false,
      content: @Composable () -> Unit
  ) {
      val baseScheme = NeuroFlowV2DarkColorScheme
      val colorScheme = if (amoledDark) {
          baseScheme.copy(
              background = Color.Black,
              surface = Color.Black,
              surfaceVariant = Color(0xFF0F1116)
          )
      } else {
          baseScheme
      }

      MaterialTheme(
          colorScheme = colorScheme,
          typography = Typography(),
          content = content
      )
  }
  ```

### Recommendation 2: Wire AMOLED Preference in `NeuroFlowAppV2.kt`
**File**: `app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt`
- Pass `amoledDark: Boolean = false` into `NeuroFlowAppV2` (from `MainActivity.kt` via DataStore `preferences.amoledDarkEnabled`).
- Pass `amoledDark` into `NeuroFlowThemeV2(amoledDark = amoledDark)`.

### Recommendation 3: Neutralize Alarmist Red in `NeuroFlowColors` (`Color.kt`)
**File**: `app/src/main/java/com/neuroflow/app/presentation/common/theme/Color.kt`
- Change:
  ```kotlin
  val DoFirstBg       = Color(0xFF1C1F28) // Was 0xFFFDE2E2 (harsh pastel pink)
  val DoFirstText     = Color(0xFFF43F5E) // Was 0xFFC62828 (alarmist red) -> Muted Coral
  val DoFirstTextDark = Color(0xFFF43F5E) // Was 0xFFEF5350 -> Muted Coral
  val TrackingRed     = Color(0xFFF43F5E) // Was 0xFFD32F2F -> Muted Coral
  val MapeBad         = Color(0xFFF59E0B) // Was 0xFFBF360C (harsh orange-red) -> Warm Amber
  ```

### Recommendation 4: Remove Alarmist Emojis and Copy
**Files**:
- `app/src/main/java/com/neuroflow/app/presentation/v2/plan/PlanScreenV2.kt`:
  - Line 471: Replace `"🔴 Do First (Urgent & High Impact / Frog)"` with `"🎯 Do First (High Impact & Frog)"`.
- `app/src/main/java/com/neuroflow/app/presentation/common/TaskRow.kt`:
  - Line 148: Replace `"🔴 High energy"` with `"⚡ High energy"`.
  - Line 155: Replace `"🚨 Critical"` with `"⏳ High Priority"`.
  - Lines 199–202: Replace light pastel `chipColors` with dark-safe slate variants.
- `app/src/main/java/com/neuroflow/app/presentation/today/TodayScreen.kt`:
  - Line 284: Replace `SectionHeader("Overdue", badge = "${uiState.overdueTasks.size}", badgeColor = NeuroFlowColors.DoFirstText)` with a low-anxiety supportive indicator using `WarmAmber` or `MutedCoral` and gentle copy.

---

## 6. Verification Method

To verify the implementation independently:
1. **Automated Unit & Contrast Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```
   Must pass with 0 failures and 34 up-to-date/executed tasks.
2. **Contrast Formula Verification**:
   Execute the WCAG contrast script to confirm all text and divider tokens pass:
   - `TextPrimary` on `SlateSurface` >= 7.0:1 (AAA)
   - `TextSecondary` on `SlateSurface` >= 4.5:1 (AA)
   - `MutedCoral` on `SlateSurface` >= 4.5:1 (AA)
   - `SubtleBorder` vs `SlateSurface` >= 1.3:1
   - `PureWhite` on `AmoledBlack` == 21.0:1
3. **Grep Search Verification for Alarmist Red**:
   Run grep search to confirm no instances of `#C62828`, `#D32F2F`, or `🔴` remain in active UI displays.
