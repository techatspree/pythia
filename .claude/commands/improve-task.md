Review and improve the planning task whose ID is given in $ARGUMENTS (e.g. `task-055`).

## Protocol

### 1. Read context (all in parallel)

- `planning/tasks/$ARGUMENTS.yaml` — the task under review
- `planning/plan.yaml` — architecture, stack, phases, task index
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

### 4. Task organization alignment

Check structural conventions shared across all tasks:

- `status.json` must have an entry for `$ARGUMENTS`. If it is missing, that is a FAIL — the entry must be added before the task can be executed.
- The last step or the validation section must include a build/test command. Accepted forms:
  - `./mvnw test` (or `mvn test`) for backend-only changes
  - `npm run check` inside `src/frontend` for frontend-only
  - Both, for cross-module tasks
  - The KMP build (`./gradlew jvmJar publishToMavenLocal`) for domain changes
  If none of these is present, flag it.
- `validation` must contain at least one assertion that can be run as a shell command (not just "visual inspection"). If all validation items are prose-only, flag it.
- The `depends_on` list must be consistent with the phase ordering in `plan.yaml`. A task in an earlier phase should not depend on a task in a later phase.

### 5. Documentation completeness

Check whether the task adequately addresses documentation:

- **CLAUDE.md**: if `outputs` includes new or renamed REST endpoints, new domain entities, new modules, or a changed API contract, at least one step must update a CLAUDE.md (project root or module level). Flag if absent.
- **Flyway migration**: if `steps` mention creating or altering database tables, a migration file (`V<N>__*.sql`) must appear in `outputs`. Flag if absent.
- **planning/plan.yaml and planning/README.md**: if the task adds a new phase or changes the phase structure, both files must be listed in `outputs`. Flag if absent.
- **Memory**: if the task changes a fundamental architectural invariant (e.g. the shape of the domain model, the KMP compilation pipeline, a cross-cutting naming convention), add a note suggesting a memory update via the auto-memory system.

### 6. Report

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

### Improvement suggestions
<bullet list of optional improvements, or "None.">

### Summary
<one or two sentences on the overall quality and the most important action needed>
```

Rating rules:
- **PASS** — no problems found in this category
- **WARN** — issues that should be addressed but won't block execution
- **FAIL** — issues that must be fixed before running `/implement-task $ARGUMENTS`

### 7. Apply fixes (if requested)

If the user asks you to apply fixes after seeing the report:

- Edit `planning/tasks/$ARGUMENTS.yaml` to address all FAIL items and any WARN items the user confirms.
- If `status.json` is missing the entry for `$ARGUMENTS`, add it:
  ```json
  "$ARGUMENTS": { "status": "pending", "started_at": null, "completed_at": null, "notes": "" }
  ```
- Do not change any other task entries in `status.json`.
- Do not create new task files or documentation files unless the task explicitly requires them.
- After applying fixes, re-run the checks mentally and confirm the FAILs are resolved before reporting done.