Implement the planning task whose ID is given in $ARGUMENTS (e.g. `task-041`).

## Protocol

1. **Read the task file** at `planning/tasks/$ARGUMENTS.yaml`. Read and understand the full `description`, every `steps` entry, all `validation` checks, and the `outputs` list before writing a single line of code.

2. **Mark in-progress**: run `./scripts/task.sh start $ARGUMENTS`.

3. **Implement each step in order**. Read every affected file before editing it. Follow the step instructions exactly — do not add abstractions, error handling, comments, or features beyond what the step requires.

4. **Verify outputs**: confirm that every path listed in the `outputs` section exists (or is deleted, as specified). If an expected file is missing, fix it before continuing.

5. **Run the validation checks** listed in the `validation` section of the task. Each check is a shell command or a structural assertion. Run the shell commands with Bash. For structural assertions (e.g. "all four call sites accept EstimationVersion") inspect the relevant files and confirm. All checks must pass.

6. **MANDATORY BUILD CHECK** — this step is non-negotiable and must not be skipped:
   - The task's last `steps` entry is typically the build/test command (e.g. `./mvnw test -pl src/backend/implementation`). Run it exactly as written.
   - If the task covers frontend-only changes, run `npm run check` inside `src/frontend`.
   - If the task covers both backend and frontend, run both.
   - **Do not mark the task done until the build passes with zero failures.** If the build fails, diagnose and fix the root cause, then re-run. Never skip or work around the build step.

7. **Update documentation**: after a green build, update any documentation affected by the changes.
   - If the task added, removed, or renamed a module, endpoint, schema, or public API: update `CLAUDE.md` (or the relevant section of it) to reflect the new state.
   - If the task's `outputs` section lists `.md` files, ensure they exist and are accurate.
   - If nothing in the codebase's documented structure changed (e.g. pure internal refactor), skip this step.
   - Do not create new documentation files unless the task explicitly requires it.

8. **Mark done**: only after all validations pass, the build is green, and documentation is up to date, run `./scripts/task.sh done $ARGUMENTS`.

9. **Report**: one short paragraph — what was changed, what the build outcome was, and (if applicable) what documentation was updated. No bullet lists of every file touched.

## Hard rules

- Never call `./scripts/task.sh done` before a successful build run in the same session.
- Never delete a file that is not explicitly listed as deleted in the task's `outputs` section.
- If the task says "do not delete X", treat that as an absolute constraint.
- If a build failure cannot be fixed within the scope of the task (e.g. it requires changes to a different module), stop, explain the blocker, and do NOT mark done.