# NeuroFlow data flow — map index

Visual documentation of how data is created, stored, computed, written back, and read in this
app. Generated 2026-10-11 from `app/src/main/java/com/neuroflow/app/`, Room schema version 20.
Static source reading only — no runtime traces (see *Unknowns*).

## Artifacts

| File | What it answers | Form |
| --- | --- | --- |
| [`data-flow.html`](data-flow.html) | **The document.** 12 sections, each a flowchart plus the evidence table under it. Open this first. | Self-contained HTML with hand-placed inline SVG — no network, no renderer needed |
| [`flow-task-lifecycle.dot`](flow-task-lifecycle.dot) | One task row: created → scored → placed → focused → completed → next occurrence, with the replan loop and the write gate | Graphviz |
| [`flow-engine-feedback.dot`](flow-engine-feedback.dot) | Which engine reads which store and which loop it writes back — the 11 feedback loops as one graph | Graphviz |
| [`flow-task-field-lineage.dot`](flow-task-field-lineage.dot) | Who writes each `tasks` column and who later reads it | Graphviz |
| [`data-flow-evidence.md`](data-flow-evidence.md) | Citation index: every trigger, read, write, and transition with `file:line` | Markdown |
| [`data-flow-risks-and-gaps.md`](data-flow-risks-and-gaps.md) | 7 hazards, 14 divergent derivations, 4 unknowns — with severity and the validation action | Markdown |

## Section map of `data-flow.html`

| # | Section | Question that section answers |
| --- | --- | --- |
| 0 | How to read these diagrams | Colour/line legend + the three facts that explain most of the rest |
| 1 | Layer map | Who creates, who stores, who computes, who reads — one page, 16 numbered edges |
| 2 | Task lifecycle | What happens to a single task row, step by step, including the recurrence fork |
| 3 | Scheduler tick | How a task gets its time: 19 worker/engine steps + the 4 triggers + the stability ladder |
| 4 | Energy chain | From a sleep record to "is this slot worth taking", including where the chain forks |
| 5 | Distraction loop | Phone usage → per-task score → priority → placement → session → back to usage |
| 6 | Feedback loops | The 11 places where today's result changes tomorrow's plan |
| 7 | Field-level lineage | Writer, readers, and reset path for every machine-owned value |
| 8 | Engine inventory | 16 engines: reads / produces / persists + every scoring constant |
| 9 | Stores, workers, surfaces | Room v20 table inventory, 13 workers and what each mutates, startup order |
| 10 | Rebuild checklist | 12 invariants to keep if you wrote this again + suggested code reading order |
| 11 | Gaps and hazards | What the map could not confirm, and where two parts of the app disagree |

## Reading order

**To understand the system** — sections 0 → 1 → 2 → 3. Sections 1–3 carry the whole mental model;
4–9 are depth on demand.

**To change scheduling behaviour** — 3 (the tick and the stability ladder), then 10 (invariants 2,
3, 4, 5), then `flow-task-lifecycle.dot`.

**To change a score** — 8 (the 1200-point budget), then 7 (who else reads that column), then 11
(the divergent-derivation table; a score usually has a second implementation).

**To add a new fact the app should remember** — 7 (preference growth needs a generator: one key is
touched in ~5–6 places), then 9 (backup symmetry: 31 of 104 keys round-trip).

**To debug "the app says two different things"** — 11, then 6.

## Rendering the `.dot` sources

No Graphviz, Mermaid CLI, or PlantUML binary is installed on the machine this document was written
on, which is why the HTML carries inline SVG instead of depending on a renderer. The `.dot` files
are still the editable source of truth for the three densest views. To render them:

```bash
# with graphviz installed (dot -V)
for f in flow-*.dot; do dot -Tsvg "$f" -o "${f%.dot}.svg"; done
```

Or drop any of them into the Qoder Canvas / Structurizr-adjacent diagram viewer, or paste the body
into Graphviz Online. Base path implied by every unqualified citation inside the `.dot` and Markdown
files is `app/src/main/java/com/neuroflow/app/`.

## Confidence vocabulary

Used as chips in the HTML and as a column in the two findings files.

- **hi** — read directly in source; the cited lines show the behaviour.
- **med** — source shows the mechanism; frequency, ordering under load, or real-device effect is inferred.
- **low** — the map assumes something the source does not prove (e.g. that WorkManager survives Doze, or that an unreachable route is truly unreachable).

## Scope and limits

- Covers only the `com.neuroflow.app` package plus `AndroidManifest.xml`. The `app/src/test`
  suites are cited only where they pin an invariant.
- Nothing here is target-state. Every node is current behaviour or an explicit dead end.
- Counts that came from reading source (104 preference keys, 31 backed up, 10 entities, 13 workers,
  20 migrations) should be re-counted on a rebuild rather than reused as truth.
- The HTML was rendered and checked in a browser: all 6 diagrams were measured for text overflowing
  its box, boxes overlapping each other, edges crossing boxes, unresolved `marker-end` ids, and dead
  in-page anchors — all clean after the fixes recorded in `data-flow-evidence.md` §11. Diagrams are
  1240 px wide inside a horizontally scrollable card (`.scroll svg { min-width: 1000px }`), so a
  narrow window scrolls rather than shrinking the type. The `.dot` files have **not** been rendered
  (no local renderer).
