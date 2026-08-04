Implement the planning task whose ID is given in $ARGUMENTS (e.g. `task-041`).

## Protocol

1. **Read the task file** at `planning/tasks/$ARGUMENTS.yaml`. Read and understand the full `description`, every `steps` entry, all `validation` checks, and the `outputs` list before writing a single line of code.

2. **Mark in-progress**: run `./scripts/task.sh start $ARGUMENTS`.

3. **Implement each step in order**. Read every affected file before editing it. Follow the step instructions exactly — do not add abstractions, error handling, comments, or features beyond what the step requires.

4. **Verify outputs**: confirm that every path listed in the `outputs` section exists (or is deleted, as specified). If an expected file is missing, fix it before continuing.

5. **Run the validation checks** listed in the `validation` section of the task. Each check is a shell command or a structural assertion. Run the shell commands with Bash. For structural assertions (e.g. "all four call sites accept EstimationVersion") inspect the relevant files and confirm. All checks must pass. **Legacy tasks (pre-task-082) may reference `./mvnw` / `mvn`; translate those to the equivalent `./gradlew` invocation** (e.g. `./mvnw verify` → `./gradlew build`, `./mvnw -pl backend/implementation test` → `./gradlew :backend:implementation:test`).

6. **MANDATORY BUILD CHECK** — this step is non-negotiable and must not be skipped:
   - Run the task's own build/test command (typically its last `steps` entry, e.g. `./gradlew :backend:implementation:test`) exactly as written.
   - If the task covers frontend-only changes, run `./gradlew :frontend:check` (do NOT substitute `npm run check` — that runs only svelte-check and skips ESLint).
   - If the task touches the end2end module, also run `./gradlew :backend:end2end:test`.
   - **Then run `./gradlew build` as a final safety net.** This is the canonical full build (domain + backend + frontend) and catches cases where the task's narrow test command missed a module the change actually affected. A task-specific `:module:test` passing is not enough — `./gradlew build` must be green.
   - **Playwright e2e — required for tasks that touch the frontend, REST DTOs, or auth behaviour**; optional (but encouraged) for pure `:domain` internal refactors when `./gradlew :domain:jvmTest` is green. When required: `cd src/frontend && npm run test:e2e`, with the dev stack running (`./scripts/dev.sh`, Docker up, `dev` backend on :8090, Vite frontend on :5173). If you genuinely cannot start the stack in this environment, do NOT mark the task done: stop and report the e2e run as the outstanding gate (per the hard rule on unfixable build steps).
   - **Do not mark the task done until every required build/test above passes with zero failures.** If anything fails, diagnose and fix the root cause, then re-run. Never skip or work around the build step.

7. **STATIC-ANALYSIS GATE** — non-negotiable, like the build check. The Gradle build runs detekt (Kotlin: domain + backend) and ESLint (TypeScript / Svelte / HTML), but the reports are *informational*: the build stays green even when there are findings (see task-056). A passing build therefore does NOT prove the code is clean — check explicitly, scoped to what this task changed:
   - Regenerate the reports for the affected module(s): `./gradlew :backend:implementation:detekt` and/or `./gradlew :domain:detekt`; for frontend, `./gradlew :frontend:npmLintReport`.
   - For every **source** file in the task's `outputs` (its `.kt`, `.kts`, `.ts`, `.svelte`, `.html` paths), confirm it introduces no new findings:
     - Kotlin — the path must NOT appear in `src/backend/implementation/build/reports/detekt/detekt.xml` or `src/domain/build/reports/detekt/detekt.xml` (detekt's checkstyle XML emits a `<file>` block only for files that have findings).
     - Frontend — the file's entry in `src/frontend/reports/eslint.json` must show `errorCount` and `warningCount` of `0`, e.g. `! jq -e '.[] | select(.filePath | endswith("Foo.svelte")) | select(.errorCount > 0 or .warningCount > 0)' src/frontend/reports/eslint.json > /dev/null`.
     - Shell scripts — for every `.sh` file in the task's `outputs`, run `bash -n <path>` (syntax) and, **if `shellcheck` is installed** (`command -v shellcheck`), `shellcheck <path>` and fix any new finding on the touched script. If `shellcheck` is not installed, skip it and say so in the report rather than treating it as passed.
   - Do NOT gate on the whole repo — the legacy baseline already has findings; only the files this task touched must be clean. Fix any new finding on a touched file before marking done. **Distinguish new vs pre-existing findings** on a touched file: check whether the same finding exists on `main` (e.g. `git stash && ./gradlew :backend:implementation:detekt`, inspect, then `git stash pop`). If pre-existing and unrelated to this change, leave it and note it in the report rather than expanding the task's scope.

8. **Update documentation**: after a green build, update any documentation affected by the changes.
   - If the task added, removed, or renamed a module, endpoint, schema, or public API: update `CLAUDE.md` (or the relevant section of it) to reflect the new state.
   - If the task's `outputs` section lists `.md` files, ensure they exist and are accurate.
   - If nothing in the codebase's documented structure changed (e.g. pure internal refactor), skip this step.
   - Do not create new documentation files unless the task explicitly requires it.

9. **Mark done**: only after all validations pass, the build is green, the static-analysis gate shows no new findings, and documentation is up to date, run `./scripts/task.sh done $ARGUMENTS`.

10. **Report**: one short paragraph — what was changed, what the build outcome was, whether static analysis was clean on the touched files, and (if applicable) what documentation was updated. No bullet lists of every file touched.

## Hard rules

- Never call `./scripts/task.sh done` before a successful build run in the same session, or while any file the task changed shows a new static-analysis finding.
- Never delete a file that is not explicitly listed as deleted in the task's `outputs` section.
- If the task says "do not delete X", treat that as an absolute constraint.
- If a build failure cannot be fixed within the scope of the task (e.g. it requires changes to a different module), stop, explain the blocker, and do NOT mark done.
- Never run `git commit`, `git push`, `git tag`, or any other VCS-mutating command unless the task's `steps` explicitly request it. Marking a task done via `./scripts/task.sh done` is the endpoint; committing is a separate, user-initiated action.