# PROCUS Bug & Feature Audit Report

*Generated automatically during the bug hunt test run.*  
*Last updated: 2026-10-24*

---

### Summary of Findings

| ID | Title | Severity | Category | File & Line | Status |
|---|---|---|---|---|---|
| **BUG-001** | Coroutine Creation During Compose Composition | High | Runtime / Concurrency | [MainActivity.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/MainActivity.kt#L305-L320) | Open |
| **BUG-002** | Redundant / Dead Condition in MiniFocusBar Check | Low | Code Quality / Dead Code | [NeuroFlowAppV2.kt](file:///home/takian/Project/PROCUS/app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt#L107-L122) | Open |
| **BUG-003** | `QUERY_ALL_PACKAGES` Lint Error and Policy Risk | Medium | Android Manifest / Policy | [AndroidManifest.xml](file:///home/takian/Project/PROCUS/app/src/main/AndroidManifest.xml#L25) | Open |

---

### Detailed Findings

#### BUG-001: Coroutine Creation During Compose Composition
- **Severity**: High
- **Category**: Runtime / Concurrency
- **File**: `app/src/main/java/com/neuroflow/app/MainActivity.kt` (lines 305–320)
- **Problem Description**:
  Inside the main Composable UI content branch (`else -> { ... }`), `scope.launch` is called directly during composition when `!freshStartHandled`. Calling coroutine builders directly within composition without `LaunchedEffect` can trigger multiple coroutines on recomposition, causes side effects before layout completion, and violates Compose contract (`CoroutineCreationDuringComposition`).
- **Expected Behavior**:
  One-shot side-effects triggered on app start should execute inside a lifecycle-aware effect handler (`LaunchedEffect(Unit)`).
- **Recommended Fix**:
  ```kotlin
  LaunchedEffect(Unit) {
      if (!freshStartHandled) {
          freshStartHandled = true
          preferencesDataStore.updatePreferences { p ->
              p.copy(
                  lastAppOpenMillis = System.currentTimeMillis(),
                  firstAppOpenMillis = if (p.firstAppOpenMillis == 0L) {
                      if (p.onboardingCompleted) {
                          System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000L
                      } else {
                          System.currentTimeMillis()
                      }
                  } else {
                      p.firstAppOpenMillis
                  }
              )
          }
      }
  }
  ```

---

#### BUG-002: Redundant / Dead Condition in MiniFocusBar Check
- **Severity**: Low
- **Category**: Code Quality / Dead Code
- **File**: `app/src/main/java/com/neuroflow/app/presentation/v2/navigation/NeuroFlowAppV2.kt` (line 122)
- **Problem Description**:
  `showMiniFocusBar` is evaluated as:
  ```kotlin
  val showMiniFocusBar = session != null && !currentRoute.startsWith("focus_v2") && !currentRoute.startsWith("focus/")
  ```
  On line 122, the condition checks:
  ```kotlin
  if (showMiniFocusBar && session != null)
  ```
  Since `session != null` is already guaranteed by `showMiniFocusBar`, the compiler warns that `session != null` is always `true`.
- **Expected Behavior**:
  Clean, non-redundant boolean checks without dead conditions.
- **Recommended Fix**:
  Simplify to:
  ```kotlin
  if (showMiniFocusBar && session != null) -> if (showMiniFocusBar && session != null) // or simply:
  if (showMiniFocusBar) {
      val s = session ?: return@Column
      MiniFocusSessionBar(...)
  }
  ```

---

#### BUG-003: `QUERY_ALL_PACKAGES` Lint Error and Policy Risk
- **Severity**: Medium
- **Category**: Android Manifest / Policy
- **File**: `app/src/main/AndroidManifest.xml` (line 25)
- **Problem Description**:
  `<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />` is flagged as a fatal error by Android Lint (`QueryAllPackagesPermission`) because it exposes all installed packages and requires explicit justification under Google Play policies.
- **Expected Behavior**:
  If full package visibility is required for the app-blocking / focus-lock feature, the lint check should explicitly suppress the warning with justification, or targeted queries should be used.
- **Recommended Fix**:
  ```xml
  <uses-permission
      android:name="android.permission.QUERY_ALL_PACKAGES"
      tools:ignore="QueryAllPackagesPermission" />
  ```
