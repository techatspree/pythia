# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Detail that only matters inside one module lives next to it and loads on demand:

- `src/domain/CLAUDE.md` — domain model, calculation inputs, the estimation-method SPI
- `src/backend/CLAUDE.md` — persistence shape, REST surface, auth, Jib
- `src/frontend/CLAUDE.md` — `apiFetch`, error surfacing, editor state, TreeTable, i18n, brand
- `.claude/skills/merlin-integration/` — Merlin `.mproject` import/export
- `.claude/skills/estimation-sessions/` — collaborative sessions (see also `docs/estimation-sessions.md`)

## What this is

TheEstimator is a project-effort estimation tool: PERT three-point estimates (optimistic / likely / pessimistic) per work item, organised in an arbitrary-depth tree of groups and leaves, with versioned draft → submitted snapshots, audit trail, and comparison between versions.

The German-language UI is intentional. **Domain source code is English, though** (phase-16, task-121): the German semantic parameter keys were Anglicised — `Tagessatz` → `dailyRate`, `Standardabweichungsfaktor` → `stdDevFactor`, `Vertriebszuschlag` → `salesSurcharge` (persisted rows migrated in `V11`; those keys became typed fields in task-138) — and domain comments/`InvariantResult` descriptors are English. The backend is likewise English-only in source — identifiers, comments, and `io.quarkus.logging.Log` messages (phase-16, task-122) — with the customer's German Excel sheet/column labels centralised as external data in `ExcelGermanLabels` (in `io.github.theestimator.service`, alongside `ExcelImporter`/`ExcelExporter`); the German sample-data content in `testdata/TestDataSeeder.kt` is likewise intentional external fixture data. The German UI strings live in per-user i18n catalogs (phase-16).

## Licence and namespace (task-145)

The project is **open source under Apache-2.0** — `LICENSE` (verbatim) + `NOTICE` at the root, `"license": "Apache-2.0"` in `src/frontend/package.json`, and an `SPDX-License-Identifier` jar-manifest attribute set for every project in the root `build.gradle.kts`. Copyright is held by "The Estimator Contributors"; there are deliberately **no per-file SPDX headers**. Those manifest attributes are constants — never add a time- or environment-derived one, or the reproducible Jib image stops being byte-deterministic.

The Kotlin/Java namespace is **`io.github.theestimator`** and the Gradle `group` matches it; the container-image group is `theestimator` (so images are `theestimator/estimation-{backend,frontend}`). The repository previously carried the originating company's reverse-DNS prefix and brand assets; task-145 renamed the namespace, removed the proprietary input documents, and rewrote the whole git history (blobs, paths, commit messages and author/committer identities) so none of it is recoverable. **Do not reintroduce a company name** into package names, image names, sample data, commit metadata or `planning/inputdata/`. Note that `espree` in `src/frontend/package-lock.json` is ESLint's parser, not a stray legacy token.

## Build & run

A single **Gradle** multi-project build drives every module. **Use `./gradlew`** (wrapper at the repo root — pins the Gradle version, downloads it on first run so contributors don't need a system `gradle`). The Kotlin plugin is unified at one version across `:domain` and `:backend:implementation` (it is a single shared artifact); compilation runs in-process (`kotlin.compiler.execution.strategy=in-process` in `gradle.properties`) to avoid stale Kotlin-daemon version clashes.

Standard Gradle invocations apply (`./gradlew build`, `:domain:build`, `:backend:implementation:test`, `:frontend:check`, `--tests "<FQCN>"` for one class). The project-specific entry points:

```
./gradlew staticAnalysis                   # ALL static analysis: detekt + frontend svelte-check & ESLint
                                           #   (informational, no Docker); also writes a consolidated
                                           #   build/reports/static-analysis/static-analysis.html
cd src/frontend && npm run test:e2e        # Playwright
./scripts/dev.sh                           # PostgreSQL (Dev Services) backend + Vite frontend, one command
#   → backend on :8090 (NOT 8080), Vite on :5173
./scripts/minikube-deploy.sh               # full PostgreSQL + Quarkus stack on minikube
```

The local **dev backend listens on :8090**, not Quarkus' default 8080 (`%dev.quarkus.http.port`, task-137). 8080 is deliberately left free for `kubectl -n estimation port-forward svc/frontend 8080:80`: both can bind simultaneously (the port-forward takes `127.0.0.1`, Quarkus `0.0.0.0`), the loopback binding wins for `localhost`, and the Vite proxy plus the Playwright suite then silently reach the CLUSTER — which rejects `Authorization: Dev …` with `401` while both stacks look healthy. Do NOT move it back to 8080. The setting is profile-scoped on purpose: the generated OpenAPI `servers` block comes from the augmentation-time port, and prod augmentation must keep emitting 8080 so the committed `src/frontend/src/lib/api/openapi.json` does not churn. In-cluster ports (`k8s/base/backend/*`) stay 8080 — they are unrelated.

All profiles use PostgreSQL 16 — there is no H2. Local dev (`dev` profile) and tests (`%test`) start a throwaway PostgreSQL container via Quarkus Dev Services, so **Docker must be running** for local backend runs and for the test suite; dev-minikube and prod use real PostgreSQL via Flyway migrations (`src/backend/implementation/src/main/resources/db/migration/V*.sql`).

Stop `./scripts/dev.sh` with SIGTERM (Ctrl-C) — never `pkill` the child JVMs, or the Dev Services PostgreSQL container orphans and keeps port 5432 bound.

## Architecture

A single Gradle build; the projects are declared in `settings.gradle.kts`.

The domain is **four** projects since task-143, so the estimation-method boundary is compiler-enforced rather than a convention: `:domain:core` (`src/domain/core`) holds the model, i18n, service and the method SPI + registry and depends on **no** method module; `:domain:method-threepoint` and `:domain:method-bucketsampled` each depend only on `:domain:core`; and `:domain` (`src/domain`) is the **aggregator** that depends on all three and is what `:backend:implementation` and `:frontend` consume. Package names are unchanged — only which project compiles them. A cross-method import is now an "Unresolved reference" compile error, not merely a review comment.

**Every one of those dependencies must be `api(...)`, never `implementation(...)`**: the backend depends on the aggregator and imports `io.github.theestimator.model.*` in ~13 files, and `implementation` dependencies do not reach a consumer's compile classpath, so downgrading one breaks the backend build wholesale.

- **`src/domain`** — Kotlin Multiplatform (JVM + JS). Compiles to a JVM jar consumed by the backend and to TypeScript + JS for the frontend (`compileKotlinJvm`, `jvmTest`, `packageTypescript`); the packaged TS zip is exposed as a consumable `typescriptDist` configuration that `:frontend` unpacks into `src/lib/domain`.
- **`src/backend/implementation`** — Quarkus 3 (Java 21, Kotlin), Hibernate ORM Panache, Flyway, Jib. REST endpoints under `/api/`.
- **`src/frontend`** — SvelteKit 5 (runes) + TypeScript + Tailwind 4, Vite, adapter-static (SPA).

## Conventions

### Single source of truth — domain logic

Anything you'd want to call "business logic" (PERT, accumulation, risk surcharge, cost rules, version equality, invariants) lives in `src/domain/`. Do not duplicate it in backend services or frontend stores. The backend and frontend exist to map persisted state ↔ domain and to render / edit; they do not re-derive numbers.

### Frontend hard rules

Detail and rationale: `src/frontend/CLAUDE.md`. The bans themselves:

- **All HTTP goes through `apiFetch`** (`$lib/api/fetch.ts`); raw `fetch(...)` is ESLint-banned.
- **File downloads are no exception** — a plain `<a href="/api/…" download>` carries no `Authorization` header and silently saves the 401 JSON body as the "export" file. Use `apiFetch` → `assertOk` → `res.blob()` → `downloadResponse(...)`.
- **Every `try`/`catch` MUST surface failure via `ErrorBanner`.** A `catch` that only logs and returns a fallback is not acceptable — that is the pattern that hid a Kotlin/JS stack overflow as blank cells.
- **Never write bare `console.*`** — use `log` from `$lib/log.ts`, in addition to (not instead of) the banner.
- **Never inline UI strings** — they belong in the `svelte-i18n` catalogs, both `de.json` and `en.json`, key-for-key.
- **Never call `.toFixed()`/`toLocale*()` or hardcode a locale** — all number/currency/date formatting goes through `$lib/format.ts`.
- **Editor components use `$bindable` two-way binding**, never the "snapshot prop into local `$state` + onchange" pattern.

### Kotlin/JS gotcha — super-property recursion

`super.<computed property>` infinite-loops on Kotlin/JS but works on JVM (see the comment in `TimeRelativeEstimationItem.kt`). When overriding a getter that needs the parent's formula, **inline the formula**; never call `super.mean` / `super.variance`. The build will compile and the JVM tests will pass, but the frontend will throw "Maximum call stack size exceeded" at runtime.

### Kotlin/JS gotcha — secondary constructors

`@JsExport`-ed classes need `@JsName("…")` on secondary constructors (Kotlin/JS rejects them otherwise). Two `data class` constructors whose value types erase to the same JVM signature (e.g. two `List<X>`) cause a platform declaration clash — differentiate by arity, not by type alone.

### Logging

One logging approach per module — use it; do not invent a second mechanism.

- **Backend** (Quarkus): log via the static `io.quarkus.logging.Log` API (`Log.info/debug/warn/error`) — no per-class logger field. Levels are configured per profile in `application.properties`: app code (`io.github.theestimator`) at DEBUG in `%dev`/`%test`, INFO in `%prod`; the root level stays at the Quarkus default. The PROD profile emits structured JSON (`%prod.quarkus.log.console.json.enabled=true`, backed by `quarkus-logging-json`) for Kubernetes; the human-readable console stays the default in dev. `EstimationVersionService` is the reference site (INFO on create/submit/delete with the estimation id, DEBUG for finer detail, ERROR on failure paths). Every request carries a **correlation id** (task-026): `CorrelationIdFilter` (`io.github.theestimator.observability`, a JAX-RS request+response `@Provider`) reads the `X-Correlation-ID` header (or generates a UUID), puts it in the logging MDC under `correlationId` — so the JSON logs carry it per request — and echoes it back on the response header. Prometheus metrics are served at `/q/metrics` (Micrometer, `http_server_requests_*` timers) via `quarkus-micrometer-registry-prometheus`.
- **Domain** (Kotlin Multiplatform): use the `io.github.oshai:kotlin-logging` facade — `private val logger = KotlinLogging.logger {}` and the lambda form `logger.debug { "…" }` (allocation-free when the level is off). On the JVM target it delegates to slf4j (the backend supplies a provider via Quarkus' JBoss LogManager bridge; the domain's own JVM tests pull `slf4j-simple` as `runtimeOnly`); on the JS target it writes to the browser console, so the frontend sees domain logs in dev. Logging is observation only — never control flow; the business math stays the single source of truth.
- **Frontend** (SvelteKit): import `log` from `$lib/log.ts` (a configured `loglevel` instance — `debug` under `import.meta.env.DEV`, else `warn`).

## Planning system

This project uses a YAML-based task plan under `planning/`:

- `planning/plan.yaml` — stack, domain, phases, task index.
- `planning/tasks/task-NNN.yaml` — one self-contained spec per task (description, steps, validation commands, outputs).
- `planning/status.json` — mutable progress; updated via `./scripts/task.sh start|done|pending <task-id>` (requires `jq`).
- `.claude/commands/` — slash commands `add-task`, `implement-task`, `improve-task` automate the lifecycle.

When implementing a task, read its YAML in full, run `./scripts/task.sh start`, follow the steps, run **every** validation command, and only call `./scripts/task.sh done` after `./gradlew build` and `./gradlew :frontend:check` are green. Older task YAMLs may reference `./mvnw …` / bare `mvn …` from before the Gradle migration (task-082); translate those to the equivalent `./gradlew …` invocation.
