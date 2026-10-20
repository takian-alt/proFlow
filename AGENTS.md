# neuroFlow Workspace Guidelines & Operating Invariants

## 1. Vice Leader Protocol & Living Skill Rule
- The **Vice Head (Vice Leader)** is an external frontier model serving as second-in-command after the user, with the user serving as the human bridge.
- **Mandatory Living Skill Rule**: EVERY TIME a handoff is prepared for or received from the Vice Leader, the agent MUST immediately edit/update the `vice-head` skill (`.agents/skills/vice-head/SKILL.md` and `~/.gemini/config/skills/vice-head/SKILL.md`).
- Update Section 5 (Living State & ADR Registry) and Section 6 (Ratified Mathematical Formulas) to preserve all architectural decisions and prevent context evaporation.

## 2. Memory & Build Safety Invariants
- Host RAM is ~7.4 GB. NEVER run Gradle with multi-worker daemons or unbounded heaps.
- Always execute test and build commands with:
  `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1`
- Keep JVM ceiling bounded to `-Xmx2048m -XX:MaxMetaspaceSize=512m`.

## 3. Strangler-Fig Refactoring & Test Greenness
- Maintain 100% test pass rate across all refactoring phases.
- Never delete legacy code or schemas until replacement domain services are implemented, wired, and verified with dedicated unit tests.

## 4. Online AI & LLM Service Invariants (Gemini BYOK, Quota Guard & Adaptive Directives)
- **Model Standardization**: All online AI features standardise on `gemini-flash-latest`.
- **Pure BYOK (Zero Hardcoded Secrets)**:
  - NEVER commit, embed, or fallback to hardcoded API keys in source code or git history.
  - API keys must be provided exclusively by the user via Settings (`UserPreferencesDataStore`).
  - When no key is configured or network is unavailable, the system MUST seamlessly degrade to local heuristic algorithms with zero user-facing errors or blockers.
- **Quota & Latency Preservation**:
  - Never trigger multiple sequential or atomized LLM requests for related attributes of a single entity.
  - Consolidate multi-dimensional inference (e.g. breakdown, scheduling slots, circadian energy, tag classification) into a single bundled prompt.
  - Protect endpoints with in-memory TTL caching (>= 20 min) and an adaptive 60-second backoff guard upon encountering HTTP 429.
- **Closed-Loop Adaptive Meta-Prompting**:
  - Personalization is achieved through forensic local telemetry (e.g., recorded task deletions and avoidances) synthesized during periodic audits into a concise, persistent `adaptiveSystemDirective`.
  - Always inject the persistent directive into task enrichment prompts to maintain continuous coaching alignment with minimal token footprint.

