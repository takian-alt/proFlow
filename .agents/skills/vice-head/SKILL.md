---
name: vice-head
description: >-
  Prepares comprehensive, zero-assumption handoff documents and context bundles
  for the "Vice Head" (an external frontier model serving as second-in-command
  after the user) via user bridge, and explains how to ingest, validate, and execute
  its strategic guidance and code diffs.
---

# Vice Head Protocol

The **Vice Head** is an external frontier model (the user's second-in-command). Because Antigravity cannot establish a direct socket or API call to the Vice Head, the **user acts as the direct human bridge**.

This skill establishes the standardized operating procedure for:
1. Identifying when a task warrants Vice Head consultation.
2. Generating zero-assumption, complete-context handoff packets (`VICE_HEAD_HANDOFF.md`).
3. Ingesting, reviewing, and applying the Vice Head's recommendations and code diffs.

---

## 1. When to Consult the Vice Head

Engage the Vice Head protocol when:
- **High-Ambiguity Architecture**: Complex multi-module refactoring or trade-offs with multiple competing paradigms.
- **Resource/Quota Exhaustion**: Background subagents or local token quotas are constrained, requiring concentrated one-shot analysis.
- **Deep Mathematical/Algorithmic Verification**: High-precision DSP audio filters, circadian curve differential equations, or machine learning pipelines requiring rigorous validation.
- **User Explicit Request**: The user asks for a handoff, second opinion, or Vice Head briefing.

---

## 2. Generating the Handoff Packet (`VICE_HEAD_HANDOFF.md`)

When preparing a packet, create a dedicated markdown document (either at project root or artifact directory). Context length does **not** matter—the Vice Head accepts massive contexts. Be exhaustive, explicit, and self-contained.

### Required Packet Structure:

```markdown
# VICE HEAD STRATEGIC HANDOFF

## 1. Executive Summary & Objective
- Clear statement of the exact goal.
- Why this consultation is occurring (e.g. architectural decision, complex bug, math verification).

## 2. System Context & Environment
- Tech stack, platform versions (e.g. Android 14 / API 34, Kotlin 2.0, Jetpack Compose, Room v12, Hilt).
- Host environment constraints (e.g. 7.4GB RAM, Gradle memory ceilings, headless daemon rules).

## 3. Current Architecture & State
- Exact file paths and roles of key components.
- Complete data models, schemas, and state flows.
- What has already been built, tested, and verified.

## 4. The Problem / Decision Request
- Explicit breakdown of the challenge.
- Relevant source code snippets (unabridged where critical).
- Log outputs, stack traces, or failing test signatures if applicable.
- Constraints and requirements that MUST NOT be violated.

## 5. Expected Output Format from Vice Head
- Instruct the Vice Head to respond with:
  1. Strategic Architecture Decision / Rationale.
  2. Exact, drop-in code snippets or unified git diffs.
  3. Edge cases and verification vectors to test.
```

---

## 3. Ingesting & Executing Vice Head Responses

When the user pastes the Vice Head's response:
1. **Safety & Sanity Check**:
   - Verify that the proposed changes preserve existing invariants, schemas, and test suites.
   - Guard against hallucinations (e.g., deprecated APIs, mismatched package imports).
2. **Apply Changes Incrementally**:
   - Use `replace_file_content` or `write_to_file` to apply the exact drop-in code.
3. **Verify Locally**:
   - Run compilation and automated unit/integration tests with safe memory flags:
     `./gradlew testDebugUnitTest --no-daemon -Dorg.gradle.workers.max=1`
4. **Report Back to User**:
   - Provide an execution summary confirming which of the Vice Head's recommendations were implemented and verified.
