---
description: Implement a planning task end to end — its steps, every validation, the build, static-analysis and i18n gates — then mark it done. Never commits.
argument-hint: <task-id>
---

Implement the planning task whose ID is given in $ARGUMENTS (e.g. `task-041`).

## Protocol

1. **Read the task file** at `planning/tasks/$ARGUMENTS.yaml`. Read and understand the full `description`, every `steps` entry, all `validation` checks, and the `outputs` list before writing a single line of code.

2. **Mark in-progress**: run `./scripts/task.sh start $ARGUMENTS`.

3. **Implement each step in order**. Read every affected file before editing it. Follow the step instructions exactly — do not add abstractions, error handling, comments, or features beyond what the step requires.

4. **Verify outputs**: confirm that every path listed in the `outputs` section exists (or is deleted, as specified). If an expected file is missing, fix it before continuing.

5. **Run the validation checks** listed in the `validation` section of the task. Each check is a shell command or a structural assertion. Run the shell commands with Bash. For structural assertions (e.g. "all four call sites accept EstimationVersion") inspect the relevant files and confirm. All checks must pass. **Legacy tasks (pre-task-082) may reference `./mvnw` / `mvn`; translate those to the equivalent `./gradlew` invocation** (e.g. `./mvnw verify` → `./gradlew build`, `./mvnw -pl backend/implementation test` → `./gradlew :backend:implementation:test`).
   - **A check that passes on the unchanged code proves nothing.** Before trusting a validation, ask whether it would have FAILED before your change; if not (a grep for text that already exists elsewhere in the file, an assertion the old code also satisfies), tighten it and say so in the report.
   - **Prove every new gate, guard or test can fail.** Break it on purpose once — remove the annotation, inject the forbidden input, mutate the rule — run it, see it fail *for the right reason*, then revert and confirm the file is restored. A gate that has never failed may be wired to nothing.
   - **When a decision in the task turns out wrong** while implementing (it breaks something the task did not foresee), implement the working alternative, record it in the task YAML's `description` as "Deviation found during implementation" (and align its validation and outputs), and name it in the report. Do not silently follow a step you have shown to be broken, and do not silently depart from it either.

6. **MANDATORY BUILD CHECK** — this step is non-negotiable and must not be skipped:
   - Run the task's own build/test command (typically its last `steps` entry, e.g. `./gradlew :backend:implementation:test`) exactly as written.
   - If the task covers frontend-only changes, run `./gradlew :frontend:check` (do NOT substitute `npm run check` — that runs only svelte-check and skips ESLint).
   - If the task touches the end2end module, also run `./gradlew :backend:end2end:test`.
   - **Then run `./gradlew build` as a final safety net.** This is the canonical full build (domain + backend + frontend) and catches cases where the task's narrow test command missed a module the change actually affected. A task-specific `:module:test` passing is not enough — `./gradlew build` must be green.
   - **Toolchain and build-configuration changes need a build from SCRATCH**, not just the incremental `./gradlew build`: any change to a Gradle plugin or platform version (Kotlin, Quarkus, Node, detekt, the node-gradle plugin), to a `gradle.properties`, or to how a toolchain is configured. An incremental build reuses outputs the OLD toolchain produced and can pass while a fresh checkout fails — task-198's Kotlin upgrade changed the bundled Yarn, the committed `kotlin-js-store/yarn.lock` no longer matched, and every incremental build (and a `--rerun` on the wrong task) masked it until a clean build failed on `kotlinStoreYarnLock`. GitHub CI always builds from a fresh checkout, so this is what CI will see. Copy the working tree WITHOUT build outputs (uncommitted changes included — `git clone` would drop them) and build it the way CI does:
     ```bash
     F=<scratchpad>/fresh; rm -rf "$F"; mkdir -p "$F"
     rsync -a --exclude '.gradle/' --exclude 'build/' --exclude 'node_modules/' --exclude '.svelte-kit/' \
       --exclude 'src/frontend/node/' --exclude 'src/frontend/reports/' --exclude 'test-results/' \
       --exclude 'src/frontend/static/config.json' --exclude 'src/frontend/src/lib/domain/' --exclude 'target/' ./ "$F/"
     (cd "$F" && CI=true ./gradlew build --no-build-cache -x :frontend:verifyOpenApiSchemaCommitted)
     ```
     `CI=true` switches on the OpenAPI drift gate, which compares the regenerated `openapi.json`/`schema.d.ts` against the last COMMIT and therefore fails by design while your regenerated files are uncommitted — exclude it here, as above, and nothing else. Delete the copy afterwards.
     Remember that `--rerun` applies only to the task named directly before it; it does not rerun that task's dependencies.
   - **Playwright e2e — required for tasks that touch the frontend, REST DTOs, or auth behaviour**; optional (but encouraged) for pure `:domain` internal refactors when `./gradlew :domain:jvmTest` is green. When required, run it against a dev stack YOU started, in both languages:
     1. **Check the ports first**: `lsof -nP -iTCP:<port> -sTCP:LISTEN` for 8090, 5173 and 5432. If any is taken, another stack is running — very likely the user's own (IntelliJ, a terminal) — and the tests would silently run against it and its data. Do not stop it and do not run against it: stop and ask the user.
     2. Start `./scripts/dev.sh` in the background (Docker must be up) and wait until `GET http://localhost:8090/api/ping` and `http://localhost:5173/` answer.
     3. `cd src/frontend && E2E_LANG=en npx playwright test`, then `E2E_LANG=de npx playwright test` (sequentially; English first leaves the stack German).
     4. Stop ONLY the `dev.sh` you started — SIGTERM to the `bash ./scripts/dev.sh` process whose parent chain leads to this session, never `pkill`, never the user's — and confirm the three ports are free and no Dev Services PostgreSQL container is left.
     If you genuinely cannot start the stack, do NOT mark the task done: stop and report the e2e run as the outstanding gate (per the hard rule on unfixable build steps).
   - **Every e2e test you write or change is self-contained.** It creates the project, estimation and draft contents it needs through the REST API (the pattern: `createBucketEstimation` + `PUT …/versions/draft` in the bucket specs) and asserts only on that data. It must NOT depend on the dev seed (`TestDataSeeder`) — no seeded project names, offers, ids or values — and it does not import or mirror seed constants. A test-local fixture constant (e.g. `PHASES` in `e2e/bucket-accompanying.test.ts`) is fine, even when it happens to resemble seed values. Why: the seed changes for its own reasons (task-186 and task-187 both changed it), and the e2e reset `DELETE /api/dev/test-data` (task-176) keeps the seeded projects INCLUDING any edits made to them, so a seed-dependent test either breaks or silently tests something else.
   - **Do not mark the task done until every required build/test above passes with zero failures.** If anything fails, diagnose and fix the root cause, then re-run. Never skip or work around the build step.

7. **STATIC-ANALYSIS GATE** — non-negotiable, like the build check. The Gradle build runs detekt (Kotlin: domain + backend) and ESLint (TypeScript / Svelte / HTML), but the reports are *informational*: the build stays green even when there are findings (see task-056) — except the `e2e/` subset of ESLint, which the `lintE2e` task enforces, so any ESLint finding under `src/frontend/e2e/` fails `:frontend:check` (task-191). A passing build therefore does NOT prove the code is clean — check explicitly, scoped to what this task changed:
   - Regenerate the reports for the affected module(s): `./gradlew :backend:implementation:detekt` and/or `./gradlew :domain:detekt`; for frontend, `./gradlew :frontend:npmLintReport`.
   - For every **source** file in the task's `outputs` (its `.kt`, `.kts`, `.ts`, `.svelte`, `.html` paths), confirm it introduces no new findings:
     - Kotlin — the path must NOT appear in `src/backend/implementation/build/reports/detekt/detekt.xml` or `src/domain/build/reports/detekt/detekt.xml` (detekt's checkstyle XML emits a `<file>` block only for files that have findings).
     - Frontend — the file's entry in `src/frontend/reports/eslint.json` must show `errorCount` and `warningCount` of `0`, e.g. `! jq -e '.[] | select(.filePath | endswith("Foo.svelte")) | select(.errorCount > 0 or .warningCount > 0)' src/frontend/reports/eslint.json > /dev/null`.
     - Shell scripts — for every `.sh` file in the task's `outputs`, run `bash -n <path>` (syntax) and, **if `shellcheck` is installed** (`command -v shellcheck`), `shellcheck <path>` and fix any new finding on the touched script. If `shellcheck` is not installed, skip it and say so in the report rather than treating it as passed.
   - Do NOT gate on the whole repo — the legacy baseline already has findings; only the files this task touched must be clean. Fix any new finding on a touched file before marking done. **Distinguish new vs pre-existing findings** on a touched file: check whether the same finding exists on `main` (e.g. `git stash && ./gradlew :backend:implementation:detekt`, inspect, then `git stash pop`). If pre-existing and unrelated to this change, leave it and note it in the report rather than expanding the task's scope.

8. **I18N GATE** — non-negotiable whenever the task introduces or changes **user-visible GUI text**. Every string a user can read must be an i18n key that exists in **every** supported language catalog, and every catalog must carry a real translation.
   - The supported languages are whatever catalogs exist in `src/frontend/src/lib/i18n/` (today `de.json` and `en.json`, mirroring the domain `SupportedLanguage` enum). Enumerate that directory rather than assuming two — if a language is added later, this gate must cover it automatically.
   - **Never inline a literal in a component.** Add the key to every catalog and render it with `$_('key')` (or `get(_)` in module context), per the CLAUDE.md i18n convention. This includes text that is not element content: `title`, `aria-label`, `alt`, `placeholder`, `<option>` labels, and any string assigned to a variable that ends up rendered (a toast, a banner message, a confirm prompt).
   - **Translate, do not copy.** A German catalog containing the English sentence is a missing translation, not a filled key. Proper nouns and symbols that are genuinely identical across languages (a product name, "OK", "CSS") are fine — everything else must differ. Re-read each new German string and ask whether a German-speaking user would actually read it that way.
   - Catalogs must stay **key-for-key identical**, so a key added to one is added to all. Verify with a runnable check, e.g.:
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
     print(f'catalogs: {sorted(cats)}')
     print('missing keys:', bad or 'none')
     sys.exit(1 if bad else 0)
     PY
     ```
   - Also confirm the new keys actually render: no raw dotted key (`admin.system.title`) visible in the UI or in an e2e snapshot, which is what a typo or a missing catalog entry looks like at runtime.
   - If the task is backend-only or touches no user-visible text, say so in the report and skip — do not invent strings.

9. **Update documentation**: after a green build, update any documentation affected by the changes.
   - If the task added, removed, or renamed a module, endpoint, schema, or public API: update the `CLAUDE.md` that documents that area to reflect the new state. Topic detail lives in nested `CLAUDE.md` files beside the code (each module's `CLAUDE.md` opens with a "Detail next to the code" list) — update the one nearest the code, and the module file only for module-wide rules.
   - Regenerated, committed artefacts (`openapi.json`, `schema.d.ts`, lockfiles) stay in the working tree for the user to commit; name the CI drift gate in the report when they changed.
   - If the task's `outputs` section lists `.md` files, ensure they exist and are accurate.
   - If nothing in the codebase's documented structure changed (e.g. pure internal refactor), skip this step.
   - Do not create new documentation files unless the task explicitly requires it.

10. **Mark done**: only after all validations pass, the build is green, the static-analysis gate shows no new findings, every new GUI string is translated in all catalogs, and documentation is up to date, run `./scripts/task.sh done $ARGUMENTS`.

11. **Report**: one short paragraph — what was changed, what the build outcome was, whether static analysis was clean on the touched files, whether new GUI text was translated in every language (or that none was added), and (if applicable) what documentation was updated. No bullet lists of every file touched.

## Hard rules

- Never call `./scripts/task.sh done` before a successful build run in the same session, while any file the task changed shows a new static-analysis finding, or while any user-visible string the task added is missing from a language catalog or left untranslated in one.
- Never ship a GUI string as an inline literal, and never fill a catalog by copying another language's text — an untranslated entry is a missing translation that silently looks done.
- Never delete a file that is not explicitly listed as deleted in the task's `outputs` section.
- If the task says "do not delete X", treat that as an absolute constraint.
- If a build failure cannot be fixed within the scope of the task (e.g. it requires changes to a different module), stop, explain the blocker, and do NOT mark done.
- Never run `git commit`, `git push`, `git tag`, or any other VCS-mutating command unless the task's `steps` explicitly request it. Marking a task done via `./scripts/task.sh done` is the endpoint; committing is a separate, user-initiated action.