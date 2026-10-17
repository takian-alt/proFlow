---
name: vice-head
description: >-
  Prepares comprehensive, zero-assumption handoff documents and context bundles
  for the "Vice Head" (an external frontier model serving as second-in-command
  after the user) via user bridge. Covers macro/micro architecture, system design,
  concurrency, data modeling, trade-off evaluations, and code execution.
---

# Vice Head Protocol: Architecture, System Design & Strategic Handoff

The **Vice Head** is an external frontier intelligence serving as second-in-command after the user. Because direct programmatic sockets or network bridges are unavailable, the **user acts as the direct human bridge**.

This skill establishes the standardized operating procedure for:
1. Identifying high-leverage architectural and system design crossroads.
2. Generating zero-assumption, complete-context architectural packets (`VICE_HEAD_HANDOFF.md` / `ARCH_SYSTEM_DESIGN.md`).
3. Ingesting, evaluating, and executing the Vice Head's architectural guidance, System Design RFCs, and code diffs.

---

## 1. When to Consult the Vice Head

Engage the Vice Head protocol for:
- **Macro & Micro Architecture Decisions**:
  - Module decomposition, dependency boundaries, and architectural patterns (e.g. Unidirectional Data Flow / MVI, Clean Architecture, CQRS).
  - Component decoupling, interface segregation, and plugin/sidecar abstractions.
- **System Design & High-Scalability Data Pipelines**:
  - Complex schema designs, Room/SQL migrations, indices, and data lifecycle management.
  - Offline-first synchronization, conflict resolution (CRDTs / Last-Write-Wins), and multi-tier cache invalidation.
  - Concurrency models, reactive streams (`StateFlow` / `SharedFlow`), backpressure handling, and thread confinement.
- **Resilience, Resource Ceilings & Failure Domains**:
  - Memory bounds (e.g. strict JVM heap limits, OOM avoidance, buffer pooling).
  - Background process survival (Foreground Services, WorkManager, Doze mode, Android App Standby buckets, process death & saved state restoration).
  - Fault tolerance, exponential backoff, circuit breakers, and crash recovery.
- **Deep Algorithmic & Mathematical Modeling**:
  - DSP audio synthesis (IIR/FIR filters, phase accumulators, binaural beat calculations).
  - Neuroscience / chronobiology algorithms (Borbély Two-Process model, circadian math, non-linear scoring engines).
- **User Explicit Request**: Whenever the user asks for high-level architectural review, system design input, or a second opinion.

---

## 2. Architectural Consultation Modes

Select the appropriate template mode based on the challenge:

### Mode A: Architectural RFC & Trade-Off Evaluation
Used when deciding between competing paradigms (e.g., Option A vs. Option B).
- Must include a **Trade-Off Matrix** comparing:
  - Latency & Runtime Performance
  - Memory & Garbage Collection Footprint
  - Developer Ergonomics & Maintenance Complexity
  - Extensibility & Testability

### Mode B: System Design & Data Modeling
Used when introducing new databases, entities, migrations, or sync engines.
- Must include:
  - Complete Entity ERD / Schema definitions with indices and foreign keys.
  - Single-Source-of-Truth (SSOT) dataflow diagram (e.g., Remote $\rightarrow$ Room $\rightarrow$ Repository $\rightarrow$ UI).
  - Migration plan (e.g. non-destructive Room migration script with pre/post integrity checks).

### Mode C: Concurrency, Threading & Background Services
Used for ongoing background services, audio engines, or async processing.
- Must include:
  - Coroutine scope ownership, cancellation hierarchy, and dispatcher confinement (`Dispatchers.Default` vs `Dispatchers.IO` vs `Dispatchers.Main.immediate`).
  - Lifecycle binding (Activity/ViewModel lifecycle vs Application/Service lifecycle).
  - State machine transitions (Idle $\rightarrow$ Running $\rightarrow$ Paused $\rightarrow$ Decompressing $\rightarrow$ Completed).

---

## 3. Generating the Architectural Handoff Packet

Create a dedicated, self-contained markdown bundle (`VICE_HEAD_HANDOFF.md` or `ARCH_SYSTEM_DESIGN.md`). Context length is unlimited—be completely exhaustive, explicit, and self-contained.

### Standard Architectural Packet Template:

```markdown
# VICE HEAD ARCHITECTURAL & SYSTEM DESIGN HANDOFF

## 1. Executive Summary & Architectural Objective
- **Problem Statement**: What architectural dilemma or system design bottleneck is being addressed?
- **Core Invariants**: What non-negotiables MUST be preserved? (e.g. zero UI blocking, backward-compatible DB, strict heap bounds).
- **Consultation Mode**: [RFC / System Design / Concurrency & Service / Math & Algorithmic].

## 2. Environment & Resource Constraints
- **Platform & Runtime**: Android 14 (API 34), Min SDK 26, Kotlin 2.0, Jetpack Compose, Room v12, Hilt.
- **Host System Limits**: 7.4GB RAM host, Gradle JVM ceiling `-Xmx2048m -XX:MaxMetaspaceSize=512m`, single-worker rule.
- **Execution Lifecycle**: Process death resilience, configuration change survival, battery optimization allowances.

## 3. Current Architecture & System Topology
### 3.1 Component Graph
```mermaid
graph TD
    UI[Jetpack Compose M3 UI] --> VM[ViewModel / StateFlow]
    VM --> UC[Domain UseCases / Engines]
    UC --> Repo[Repository Layer]
    Repo --> Local[Room Database / DataStore]
    Repo --> Remote[Remote Sync / WorkManager]
```

### 3.2 Data Models & Schema Contracts
- Unabridged data entities, Room schemas, and StateFlow contracts relevant to the problem.

### 3.3 Current Implementation & Pain Points
- Existing source code snippets exhibiting architectural debt, concurrency bottlenecks, or scaling limits.
- Stack traces, failing test signatures, or performance profiling benchmarks (if any).

## 4. Architectural Proposals Under Consideration
### Option 1: [Name / Approach]
- **Mechanics**: How it works.
- **Pros & Cons**: Immediate vs long-term trade-offs.

### Option 2: [Name / Approach]
- **Mechanics**: How it works.
- **Pros & Cons**: Immediate vs long-term trade-offs.

## 5. Explicit Review & Guidance Request for Vice Head
Instruct the Vice Head to return:
1. **Architectural Decision Record (ADR)**:
   - **Decision**: Recommended option or novel synthesis.
   - **Rationale**: Deep technical justification referencing system constraints.
   - **Trade-Off Analysis**: Why the trade-offs of this approach are acceptable.
2. **System Design & Component Interaction**:
   - Mermaid diagram illustrating reactive data flow, state transitions, or component boundaries.
   - Interface definitions and public API contracts.
3. **Drop-in Implementation Code**:
   - Exact Kotlin / Compose / Room code or git diffs ready for direct execution.
4. **Resilience, Edge Cases & Verification Plan**:
   - Concurrency race conditions, process death survival, memory leak vectors.
   - Concrete test cases (Kotest / JUnit 5 / MockK) to prove correctness.
```

---

## 4. Ingesting & Executing Vice Head Recommendations

When the user returns with the Vice Head's recommendations:

1. **Architectural Sanity & Invariant Check**:
   - **Clean Dependency Rule**: Domain layer must not depend on UI or Data layers.
   - **Thread Confinement**: No synchronous disk I/O, heavy computation, or `runBlocking` on the Android Main thread.
   - **Memory & Resource Safety**: Ensure all background threads, AudioTracks, and Coroutine jobs have clean `onCleared()` or `try-finally` cancellation.
   - **Schema Safety**: Verify that any Room schema change includes non-destructive migration scripts with fallback safeguards.

2. **Incremental Application**:
   - Apply core data models and domain contracts first.
   - Implement repository, engine, and service logic.
   - Update presentation, UI composables, and navigation graphs.

3. **Rigorous Local Verification**:
   - Always run the test suite with memory-safe flags:
     ```bash
     ./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1
     ```
   - Verify zero regressions across existing unit and E2E suites.

4. **Document Architectural Decision**:
   - Record the finalized architectural choice in `walkthrough.md` or git commit message for future traceability.
