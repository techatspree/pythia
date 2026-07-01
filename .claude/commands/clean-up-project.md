Run the project's **static-analysis** validation, collect every finding, and fix them one after another. Persist the set of remaining findings to a state file so the cleanup can be interrupted at any point and resumed later from exactly where it stopped.

Findings are scoped to **static analysis only** — detekt (Kotlin: domain + backend), eslint (frontend), and svelte-check (frontend type errors). Test failures are **not** findings: a green test suite is a precondition for running this command and is re-checked after every fix (see below).

`$ARGUMENTS` may contain:
- `--rescan` — discard any existing state and regenerate findings from a fresh validation run, even if a state file already exists.
- a source filter (`detekt`, `eslint`, `check`) — only collect/fix findings from that source.

If `$ARGUMENTS` is empty, default to: resume an existing state file if present, otherwise generate a fresh one.

## State file

Path: `planning/cleanup-state.json`. It is the single source of truth for what is left to do. Schema:

```json
{
  "generated_at": "<ISO-8601 UTC>",
  "validation_commands": ["./gradlew staticAnalysis"],
  "findings": [
    {
      "id": "f001",
      "source": "detekt-domain | detekt-backend | eslint | svelte-check",
      "file": "<repo-relative path>",
      "line": 42,
      "rule": "<rule id, e.g. MagicNumber / no-unused-vars>",
      "message": "<the finding text>",
      "status": "pending"
    }
  ]
}
```

`status` is one of `pending`, `fixed`, or `skipped`. Only `pending` findings remain to be worked. **The file MUST be rewritten after every single finding is resolved** — never batch the persistence. This is what makes the cleanup resumable: if interrupted, the next invocation reads the file and continues with whatever is still `pending`.

The first time you create the state file, ensure it is not committed: if `planning/cleanup-state.json` is not already covered by `.gitignore`, append a line for it.

## Precondition — tests must be green

This command may only run against a project whose tests all pass. Before doing anything else (on a fresh run **and** on resume), confirm the test suites are green:

```bash
./gradlew test                        # domain + backend unit + @QuarkusTest (Docker required)
cd src/frontend && npm run test:e2e   # only if the frontend was likely affected / on request
```

**Docker must be running** — the backend tests start a throwaway PostgreSQL container via Quarkus Dev Services. If Docker is down, the precondition cannot be checked: report that and stop.

If any test fails, **abort immediately**: report the failing tests and tell the user to fix them first. Do not collect findings, do not modify the state file, do not fix anything. Cleaning up static-analysis findings on top of a broken build is out of scope for this command.

(Unlike a single Maven `verify`, the Gradle build separates concerns: `./gradlew test` is the precondition, and `./gradlew staticAnalysis` in step 2 regenerates all the analysis reports. `./gradlew build` runs both at once but is heavier; either approach is fine.)

## Protocol

### 1. Decide: resume or rescan

- If `planning/cleanup-state.json` exists, is valid JSON, and has at least one `pending` finding — and `--rescan` was NOT passed — **resume**: load it and skip straight to step 4. Do not re-run the validation phase yet (that happens at the end).
- Otherwise (no file, empty/all-resolved file, corrupt file, or `--rescan`): proceed to step 2 to generate findings.

### 2. Run the static analysis

Run the static analysis from the repo root and capture all output. These commands are informational here — let them run to completion even if they report findings; do not stop on the first warning:

```bash
./gradlew staticAnalysis   # one goal: detekt (domain + backend) + frontend svelte-check + ESLint report
```

These write machine-readable reports — parse these rather than scraping console text where possible:

- Domain detekt:  `src/domain/build/reports/detekt/detekt.xml`
- Backend detekt: `src/backend/implementation/build/reports/detekt/detekt.xml`
  (use the Gradle `build/reports/` path — a stale Maven-era `target/detekt/`
  report may still exist; ignore it. Glob `**/build/reports/detekt/detekt.xml`
  to catch any other module, e.g. `:backend:end2end`.)
- Frontend eslint: `src/frontend/reports/eslint.json` (produced by the `lint:report` npm script the `:frontend:check` Gradle task runs)

`staticAnalysis` runs svelte-check too (via the frontend `npmCheck` task); its warnings/errors are in the Gradle console output. To capture them cleanly for parsing, run `cd src/frontend && npm run check` directly.

Collect **static-analysis** findings from, in this order:

1. **detekt** (domain + backend) — each `<error>`/`<warning>` element in the XML: file, line, the `source` rule id, and the message.
2. **eslint** (frontend) — each message in `eslint.json`: `filePath`, `line`, `ruleId`, `message`.
3. **svelte-check** — each error/warning line from `npm run check` output: file, line, message (rule = `svelte-check`).

Do **not** record test results as findings — tests are the precondition/verification gate, not cleanup work. If a source filter was given in `$ARGUMENTS`, keep only matching findings.

### 3. Write the state file

Assign sequential ids (`f001`, `f002`, …). Write all collected findings with `status: "pending"` to `planning/cleanup-state.json` using the schema above. Report the total count grouped by source. If there are zero findings, report that the project is already clean, remove the state file, and stop.

### 4. Fix loop — one finding at a time

Process `pending` findings one after another (lowest id first). For each:

1. Read the target file around the reported line and understand the finding in context.
2. Apply the smallest correct fix that resolves the finding **without changing behaviour**. Follow the conventions in `CLAUDE.md` — especially: business logic stays in `src/domain/`; the Kotlin/JS super-property and secondary-constructor pitfalls; the frontend error-surfacing rule and `log` (never bare `console.*`); the `$bindable` two-way-binding convention for editor components.
3. If a finding is a genuine false positive or intentionally allowed, set its `status` to `skipped` with a one-line `message` note explaining why (prefer the tool's documented suppression mechanism — e.g. a detekt `@Suppress`, an eslint disable comment with justification — over editing the shared config) instead of `fixed`.
4. **Confirm no test fails because of the fix.** Run the tests for the module(s) the fix touched before marking it done (Docker must be running for the backend/domain test tasks):
   - domain change → `./gradlew :domain:test`
   - backend change → `./gradlew :backend:implementation:test`
   - frontend change → `cd src/frontend && npm run check` (and `npm run test:e2e` if behaviour-adjacent)
   - a change spanning modules, or any doubt → `./gradlew test`

   If a test fails, the fix is wrong: revert or correct it until the relevant tests are green again. Do **not** mark the finding `fixed` while any test is failing.
5. Once tests are green, mark the finding `fixed` (or `skipped`) and **immediately rewrite `planning/cleanup-state.json`**. Do not move on until the file is saved — this is the interrupt-safety guarantee.

Do not re-run the full analysis inside the loop. The per-fix test run above is the required gate; beyond it, you may run a cheap targeted analysis check (e.g. `npm run check` for one svelte-check error) to confirm a fix, but keep the loop moving.

### 5. Final verification

When no `pending` findings remain, re-run the full static analysis and the tests: `./gradlew staticAnalysis` (regenerates all the reports) and `./gradlew test` (Docker required). `./gradlew build` runs all of it in one pass if you prefer.

- If it surfaces **new** static-analysis findings (e.g. a fix introduced a different warning), append them to the state file as fresh `pending` entries and return to step 4.
- If any **test** fails at this point, stop and report it — the run is not clean.
- When everything is clean — `./gradlew test` passes and `./gradlew staticAnalysis` reports no findings — delete `planning/cleanup-state.json` and report.

### 6. Report

A short summary: how many static-analysis findings were found, how many fixed vs. skipped (with reasons), which files changed, and confirmation that both the tests and the static analysis are now green. Do not commit anything unless the user asks.

## Hard rules

- **Tests are a hard gate.** The command must not start unless all tests pass (precondition), and must not mark a finding `fixed` while any test it could affect is failing (per-fix check). A red test suite halts the command.
- Findings are **static analysis only** (detekt, eslint, svelte-check). Never turn a test failure into a finding.
- The state file is rewritten after **every** resolved finding — never batch. An interrupt at any moment must leave the file accurately listing the remaining `pending` work.
- Fixes must preserve behaviour. Cleanup means satisfying the validator, not changing what the code does. If a finding cannot be fixed without a behaviour or design change, mark it `skipped` with a note and surface it in the final report rather than guessing.
- Never relax or delete a rule in the shared detekt/eslint config to make a finding disappear. Use a justified, scoped suppression at the finding site if it is a true false positive.
- Never delete or edit unrelated findings' entries; only the finding currently being worked changes status.
- Resume must never duplicate work: a finding already `fixed`/`skipped` in the state file is not reprocessed.