# E2E Test Infra: neuroFlow V2 Parity & Neuro-Capabilities

## Test Philosophy
- Opaque-box, requirement-driven derived strictly from ORIGINAL_REQUEST.md.
- Methodology: Category-Partition + Boundary Value Analysis + Pairwise Interactions + Real-World Workload Scenarios.

## Feature Inventory & Test Coverage
| # | Feature | Requirement | Tier 1 | Tier 2 | Tier 3 |
|---|---------|-------------|:------:|:------:|:------:|
| 1 | Notes System Connectivity | ORIGINAL_REQUEST §2026-10-16 R1 | 5 | 5 | ✓ |
| 2 | Weekly Schedule Integration | ORIGINAL_REQUEST §2026-10-16 R1 | 5 | 5 | ✓ |
| 3 | Deep Analytics & Heatmap Hub | ORIGINAL_REQUEST §2026-10-16 R1 | 5 | 5 | ✓ |
| 4 | Settings Routes Parity | ORIGINAL_REQUEST §2026-10-16 R1 | 5 | 5 | ✓ |
| 5 | Identity & WOOP Anchor | ORIGINAL_REQUEST §2026-10-16 R1 | 5 | 5 | ✓ |
| 6 | Retrospective Time Logging | ORIGINAL_REQUEST §2026-10-16 R1 | 5 | 5 | ✓ |
| 7 | Neural Soundscapes DSP & UI | ORIGINAL_REQUEST §2026-10-16 R2 | 5 | 5 | ✓ |
| 8 | Drift-Proof Wall-Clock Timing | ORIGINAL_REQUEST §2026-10-16 R3 | 5 | 5 | ✓ |
| 9 | Foreground Focus Live Notification | ORIGINAL_REQUEST §2026-10-16 R3 | 5 | 5 | ✓ |
| 10 | Default V2 UI & Preference | ORIGINAL_REQUEST §2026-10-16 R4 | 5 | 5 | ✓ |
| 11 | MainActivity & Widget Interop | ORIGINAL_REQUEST §2026-10-16 R4 | 5 | 5 | ✓ |
| 12 | Voice Quick-Capture & Inference | ORIGINAL_REQUEST §2026-10-16 R5 | 5 | 5 | ✓ |
| 13 | Executive Decompression Zone | ORIGINAL_REQUEST §2026-10-16 R5 | 5 | 5 | ✓ |

## Test Architecture
- Test Runner: `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1`
- Directory Layout: `app/src/test/java/com/neuroflow/app/e2e/`
- Test Output: Generates `TEST_READY.md` when Tier 1-4 tests are implemented and ready.
