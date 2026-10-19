---
description: Review a planning task against the architecture, the task conventions and the real repository, report PASS/WARN/FAIL per category, and apply the fixes on request.
argument-hint: <task-id>
---

Review and improve the planning task whose ID is given in $ARGUMENTS (e.g. `task-055`).

## Protocol

### 1. Read context (all in parallel)

- `planning/tasks/$ARGUMENTS.yaml` — the task under review
- `planning/plan.yaml` — architecture, stack, phases (phase `id`/`name` only; it
  is not a task index — a task's phase lives in its own `phase:` field)
- `planning/status.json` — task statuses and declared dependencies
- Any `CLAUDE.md` files at the project root or module level (use `find . -maxdepth 3 -name CLAUDE.md`)

### 2. Architecture alignment

Verify the task is consistent with the project's architecture:

- **Module boundaries**: domain changes belong in `src/domain/`, backend in `src/backend/`, frontend in `src/frontend/`. A task that touches multiple modules must list all affected paths in `outputs`.
- **KMP constraint**: the `src/domain/` module compiles to both JVM (used by backend) and Kotlin/JS (used by frontend via adapter.ts). Any domain change that adds or modifies `calculate()`, a data class, or an exported symbol must mention running `./gradlew jvmJar publishToMavenLocal` and regenerating TypeScript types. If the task modifies domain code but omits this, flag it.
- **Single source of truth**: computation logic must live in the KMP domain, not be duplicated in the frontend or backend. Flag any step that suggests replicating business logic outside `src/domain/`.
- **Stack fit**: check that the tools and approaches in `steps` match the declared stack (Quarkus/Kotlin backend, SvelteKit/TypeScript frontend, PostgreSQL via Flyway, Kubernetes via Kustomize).

### 3. Errors and omissions

Check mechanically for problems:

- Every task ID in `depends_on` must exist as a key in `status.json`. Report any that are missing.
- The `phase` field must match a phase ID in `plan.yaml`. Report any mismatch.
- Every file path mentioned in `outputs` should also appear (explicitly or implicitly) in `steps`. Report any file listed as an output that no step accounts for.
- Every file path mentioned in `steps` or `validation` that clearly refers to a specific module or class should be plausible given the stack and existing modules. Flag obviously wrong paths (e.g. `.java` files in a Kotlin module, non-existent subdirectories).
- The task must have all four required sections: `description`, `steps`, `validation`, `outputs`. Report any that are missing or empty.
- **Check the task's factual claims against the repository**, not just its structure: counts ("51 operations", "5 findings"), named files, line references, quoted text, the current state it describes, the CLAUDE.md file a step says holds a section (topic detail lives in nested `CLAUDE.md` files beside the code). Run the command or grep that establishes each claim. A claim that is no longer true is a WARN; one that would make a step or validation wrong is a FAIL. Earlier tasks in the same chain often change what a later one assumes.
- **Run the tools the task relies on, on today's code, when that is cheap** — a linter, a script, an SBOM query. What they report today often contradicts the task's prediction (task-204's "two findings" were warnings that would not have failed the gate).
- External tools invoked in steps or validation must be **version-pinned** (`npx -y @redocly/cli@2.64.1`, not `@latest`): a floating version can change the verdict without a commit. WARN.

### 4. Task organization alignment

Check structural conventions shared across all tasks:

- `status.json` must have an entry for `$ARGUMENTS`. If it is missing, that is a FAIL — the entry must be added before the task can be executed.
- The last step or the validation section must include a build/test command. Accepted forms:
  - `./gradlew :backend:implementation:test` for backend-only changes
  - `./gradlew :frontend:check` for frontend-only
  - `./gradlew build` for cross-module / full-stack tasks
  - `./gradlew :domain:build` for domain (KMP) changes
  If none of these is present, flag it.
- `validation` must contain at least one assertion that can be run as a shell command (not just "visual inspection"). If all validation items are prose-only, flag it.
- The `depends_on` list must be consistent with the phase ordering in `plan.yaml`. A task in an earlier phase should not depend on a task in a later phase.
- **No step may commit, push or tag.** The user commits. A step saying "commit the regenerated files" is a FAIL; it becomes "leave them in the working tree and name the drift gate in the report".
- **A step that starts the dev stack or runs Playwright** must carry the port protocol: check :8090, :5173 and :5432 are free first (otherwise stop — the user's own stack is running and the tests would hit it), and afterwards stop only the `dev.sh` this task started. Missing: WARN.
- **Every validation must be able to fail.** Flag a check that already passes on the CURRENT code (e.g. a grep for text that exists elsewhere in the same file, or `fetch-depth: 0` when another job already has it) as WARN, and propose a scoped one. A new gate, guard or test should be proven by a deliberate failure (mutate, run, see it fail, revert); suggest that step when it is missing.

### 5. Documentation completeness

Check whether the task adequately addresses documentation:

- **CLAUDE.md**: if `outputs` includes new or renamed REST endpoints, new domain entities, new modules, or a changed API contract, at least one step must update a CLAUDE.md (project root or module level). Flag if absent.
- **Flyway migration**: if `steps` mention creating or altering database tables, a migration file (`V<N>__*.sql`) must appear in `outputs`. Flag if absent.
- **planning/plan.yaml and planning/README.md**: if the task adds a new phase or changes the phase structure, both files must be listed in `outputs`. Flag if absent.
- **Memory**: if the task changes a fundamental architectural invariant (e.g. the shape of the domain model, the KMP compilation pipeline, a cross-cutting naming convention), add a note suggesting a memory update via the auto-memory system.

### 6. Logging completeness

Check whether the task introduces work that the CLAUDE.md "Logging" conventions require to be logged:

- If `outputs`/`steps` add a **backend service or REST endpoint** (a JAX-RS resource method, an `@ApplicationScoped` service operation), the task must add `io.quarkus.logging.Log` statements at appropriate levels (INFO on the operation with key ids, DEBUG for finer detail, ERROR on failure paths). Flag if absent.
- If they add a **domain operation** (a new `calculate()`-style computation or significant model behaviour), the task should add a `KotlinLogging.logger {}` debug observation (lambda form). Flag if a substantial new domain operation has none.
- If they add a **significant frontend flow** (a new route load, store action, or data-mutation path with a `try`/`catch`), the task must log via `log` from `$lib/log.ts` — never bare `console.*` — while keeping `ErrorBanner` surfacing. Flag bare `console.*` or missing logging.
- Reference the CLAUDE.md logging conventions in any finding. Purely structural/config/docs tasks with no new service, domain op, or frontend flow are exempt — rate PASS.

### 7. I18n completeness

Check whether the task introduces **user-visible GUI text** and, if so, whether it actually requires that text to be translated into every supported language. The languages are whatever catalogs exist in `src/frontend/src/lib/i18n/` (today `de.json` and `en.json`, mirroring the domain `SupportedLanguage` enum) — enumerate that directory rather than assuming two, so a language added later is covered.

- If `steps`/`outputs` add or change anything a user can read — a new route or panel, a button, a label, a heading, a table header, a toast/banner message, an empty-state line, a validation error, a status label — the task must have a step that adds the key to **every** catalog. Flag if the step is missing, and flag equally if it names only one catalog (`de.json` but not `en.json`).
- The task must forbid inline literals: strings belong in the catalogs and render via `$_('key')` (or `get(_)` in module context), per the CLAUDE.md i18n convention. Flag a step that spells out visible English text to hardcode in a component.
- Check the **non-obvious surfaces** are covered when relevant: `title`, `aria-label`, `alt`, `placeholder`, `<option>` labels, and strings assigned to a variable before being rendered. These are the ones tasks forget; a task that only says "add a button labelled X" and whose UI needs a tooltip or aria-label should be flagged as incomplete rather than assumed fine.
- The task should require a **real translation**, not the same sentence in both files. If it dictates literal string values, check the German ones are actually German. Proper nouns and symbols that are genuinely identical (a product name, "OK", "CSS") are fine.
- `validation` must contain a runnable check that the catalogs stay key-for-key identical and carry the new keys. Flag if the only i18n evidence is prose. A good shape:
  ```bash
  python3 - <<'PY'
  import json, pathlib, sys
  cats = {p.stem: json.loads(p.read_text())
          for p in pathlib.Path('src/frontend/src/lib/i18n').glob('*.json')}
  def flat(o, p=''):
      s = set()
      for k, v in o.items():
          n = f'{p}.{k}' if p else k
          s |= flat(v, n) if isinstance(v, dict) else {n}
      return s
  keys = {l: flat(d) for l, d in cats.items()}
  ref = set().union(*keys.values())
  bad = {l: sorted(ref - k) for l, k in keys.items() if ref - k}
  print('missing keys:', bad or 'none')
  sys.exit(1 if bad else 0)
  PY
  ```
- Backend-only tasks, pure refactors, and tasks that touch no user-visible text are exempt — rate PASS and say so in one line rather than inventing a finding.

### 8. Wireframe completeness

Check whether the task adds or changes a **GUI function** — a screen, a dialog, a panel, a control, or a new state of an existing view (empty, error, filtered, role-dependent) — and, if so, whether its `description` carries wireframes in the form `/add-task` prescribes (reference: `planning/tasks/task-200.yaml`).

- A GUI task must have a `## Wireframes` section in `description`. If it has none, that is a FAIL — `/implement-task` would have to invent the layout.
- There must be one `### <screen>: <state>` wireframe for **every** affected screen and every state the steps introduce. Compare against the steps: a dialog, an empty state, a disabled/hidden variant or a role-dependent view that the steps build but no wireframe shows is a FAIL.
- Each wireframe is an ASCII box showing the German UI text as it will appear and realistic seeded test data, not `Lorem`/`XXX` placeholders. Placeholder content is a WARN.
- Each wireframe is annotated with bullets naming the `data-testid` of every new element, the i18n key of every new string, who sees what (role/user), when elements are hidden or disabled, and which `$lib/ui` primitives or existing dialogs it reuses. Missing annotations are a WARN.
- **Consistency:** the testids and i18n keys in the wireframe annotations must match the steps, the i18n table and the `validation` greps exactly. Flag every mismatch (a testid in a step that no wireframe shows, a key in a wireframe missing from the catalog step) as FAIL.
- Frontend steps should refer to the wireframes ("as wireframed") rather than re-describe the layout differently. Flag contradictions between a step and its wireframe as FAIL.
- Backend-only, domain-only, infrastructure and documentation tasks with no GUI change are exempt — rate PASS and say so in one line.

### 9. Report

Produce this exact structure:

```
## improve-task: $ARGUMENTS — <task title>

### Architecture alignment
[PASS | WARN | FAIL]
<one line per finding, or "No issues found.">

### Errors and omissions
[PASS | WARN | FAIL]
<one line per finding, or "No issues found.">

### Task organization
[PASS | WARN | FAIL]
<one line per finding, or "No issues found.">

### Documentation completeness
[PASS | WARN | FAIL]
<one line per finding, or "No issues found.">

### Logging completeness
[PASS | WARN | FAIL]
<one line per finding, or "No issues found.">

### I18n completeness
[PASS | WARN | FAIL]
<one line per finding, or "No issues found." — or "No user-visible text — exempt.">

### Wireframe completeness
[PASS | WARN | FAIL]
<one line per finding, or "No issues found." — or "No GUI change — exempt.">

### Improvement suggestions
<bullet list of optional improvements, or "None.">

### Summary
<one or two sentences on the overall quality and the most important action needed>
```

Rating rules:
- **PASS** — no problems found in this category
- **WARN** — issues that should be addressed but won't block execution
- **FAIL** — issues that must be fixed before running `/implement-task $ARGUMENTS`

### 10. Apply fixes (if requested)

If the user asks you to apply fixes after seeing the report:

- Edit `planning/tasks/$ARGUMENTS.yaml` to address all FAIL items and any WARN items the user confirms.
- For a missing or incomplete wireframe section, draw the missing wireframes into `description` from the steps and the current components (read the affected `.svelte` files and the i18n catalogs so the text and testids are real), then align steps, i18n table and validation with them.
- If `status.json` is missing the entry for `$ARGUMENTS`, add it:
  ```json
  "$ARGUMENTS": { "status": "pending", "started_at": null, "completed_at": null, "notes": "" }
  ```
- Do not change any other task entries in `status.json`.
- Do not create new task files or documentation files unless the task explicitly requires them.
- After applying fixes, re-run the checks mentally and confirm the FAILs are resolved before reporting done.