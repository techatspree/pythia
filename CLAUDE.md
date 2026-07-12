# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

TheEstimator is a project-effort estimation tool: PERT three-point estimates (optimistic / likely / pessimistic) per work item, organised in an arbitrary-depth tree of groups and leaves, with versioned draft → submitted snapshots, audit trail, and comparison between versions. The German-language UI and domain vocabulary (Aufwand, Risiko, Tagessatz, …) are intentional.

## Build & run

A single **Gradle** multi-project build drives every module. **Use `./gradlew`** (Gradle wrapper at the repo root — pins the Gradle version, downloads it on first run so contributors don't need a system `gradle`). The Kotlin plugin is unified at one version across `:domain` and `:backend:implementation` (it is a single shared artifact); compilation runs in-process (`kotlin.compiler.execution.strategy=in-process` in `gradle.properties`) to avoid stale Kotlin-daemon version clashes.

```
./gradlew build                            # full build, all tests (domain + backend + frontend)
./gradlew build -x test                    # build everything, skip tests
./gradlew staticAnalysis                   # ALL static analysis: detekt + frontend svelte-check & ESLint (informational, no Docker); also writes a consolidated build/reports/static-analysis/static-analysis.html
./gradlew detekt                           # Kotlin-only static analysis (detekt reports)
./gradlew :domain:build                    # domain (KMP) build + tests only
./gradlew :backend:implementation:test     # backend unit + @QuarkusTest (PostgreSQL via Dev Services — Docker required)
./gradlew :frontend:check                  # TS/Svelte type-check + ESLint
cd src/frontend && npm run test:e2e        # Playwright

./scripts/dev.sh                           # PostgreSQL (Dev Services) backend + Vite frontend, one command
./scripts/minikube-deploy.sh               # full PostgreSQL + Quarkus stack on minikube
```

Run one backend test class:
`./gradlew :backend:implementation:test --tests "io.github.theestimator.rest.EstimationVersionResourceIT"`

All profiles use PostgreSQL 16 — there is no H2. Local dev (`dev` profile) and tests (`%test`) start a throwaway PostgreSQL container via Quarkus Dev Services, so **Docker must be running** for local backend runs and for the test suite; dev-minikube and prod use real PostgreSQL via Flyway migrations (`src/backend/implementation/src/main/resources/db/migration/V*.sql`).

## Architecture

A single Gradle build with these projects (`settings.gradle.kts`): `:domain`, `:backend:implementation`, `:backend:end2end`, `:frontend`.

- **`src/domain`** (`:domain`) — Kotlin Multiplatform (JVM + JS). Compiles to a JVM jar consumed by the backend via `project(":domain")` and to TypeScript + JS for the frontend (Gradle tasks `compileKotlinJvm`, `jvmTest`, `packageTypescript`); the packaged TS zip is exposed as a consumable `typescriptDist` configuration that `:frontend` unpacks into `src/lib/domain`. **Holds all business logic** — PERT calculation, accumulation, the tree-shape domain model. The backend and frontend MUST NOT reimplement domain rules; they call into it.
- **`src/backend/implementation`** (`:backend:implementation`) — Quarkus 3 (Java 21, Kotlin) via the `io.quarkus` Gradle plugin, Hibernate ORM Panache, Flyway, Jib for container images. REST endpoints under `/api/`.
- **`src/frontend`** — SvelteKit 5 (runes) + TypeScript + Tailwind 4, Vite, adapter-static (SPA). Talks to the backend via `apiFetch` (`$lib/api/fetch.ts`), a thin `fetch` wrapper that attaches the active auth provider's `Authorization` header; request/response types are generated from the backend OpenAPI via openapi-typescript into `$lib/api/schema.d.ts`. Raw `fetch(...)` is banned by ESLint (`no-restricted-syntax`) in favour of `apiFetch` (task-090).

### Domain model: trees, not flat groups

The estimation hierarchy is an **arbitrary-depth tree**. Sealed hierarchy in `src/domain/src/main/kotlin/io/github/theestimator/model/`:

```
EstimationNode (sealed)
├── EstimationGroup           — interior node; title + List<EstimationNode> children;
│                               accumulates mean/variance/offerPT/cost/offerPrice
│                               over its subtree via `get() = children.sumOf { … }`
└── EstimationItem (sealed)   — leaf; carries the raw PERT values
    ├── FixedEstimationItem
    └── TimeRelativeEstimationItem    — mean scales by phase.durationWeeks
```

`EstimationVersion.roots: List<EstimationNode>` is the tree root. `EstimationVersion.calculate()` flattens leaves, computes the risk factor + driver factor + daily rate, builds `CalculationParameters`, then maps the tree via `withCalculationParameters(...)`. Groups derive their numbers from children — never store accumulated values on `EstimationGroup`.

A `leaves()` extension on `EstimationNode` flat-walks the tree and returns the leaf items.

#### Pluggable estimation methods (SPI)

Estimation methods are pluggable behind a domain SPI in `io.github.theestimator.method` (phase-15, planned in task-096). `EstimationMethod` is a `@JsExport` enum (`THREE_POINT_PERT`, `BUCKET_SAMPLED_PERT`); `EstimationMethodModule` is the interface each method implements — it supplies the per-leaf `calculate(item, params)` (mirroring `EstimationItem.withCalculationParameters`) plus the export `exportRow` / `exportColumnHeaders` shaping; and `EstimationMethodRegistry` is a singleton `object` mapping enum → module (`register` / `get` / `require` / `all`, where `require` throws for an unregistered method). **One Kotlin package per method** — each concrete method MODULE lives in its own `io.github.theestimator.method.<method>` package and cross-package references between methods are forbidden; all interaction goes through the SPI. Only the enum is `@JsExport`ed; the interface and registry stay domain-internal. The registry self-populates via `EstimationMethodRegistry.installStandardMethods()`, called from the object's `init {}` (runs on first access, e.g. the export writers' `require(...)`).

Method #1 is three-point PERT (task-098): `ThreePointMethodModule` in `io.github.theestimator.method.threepoint` wraps the existing per-leaf PERT calc and owns the PERT export column shape (`exportColumnHeaders` = `Min/Expected/Max`, in English); both the CSV and Excel exporters source their input-column headers from it, so the module is the single source of the PERT column shape. **The sealed leaf types `FixedEstimationItem` / `TimeRelativeEstimationItem` stay in `io.github.theestimator.model`** — Kotlin requires subclasses of the `sealed class EstimationItem` to share its package, so per-method leaf types cannot move into the method package (the same applies to task-102's `BucketedEstimationItem`). Registry-driven export dispatch by `estimation.method` lands in task-107.

### Persistence shape

The `estimations` table carries a method column (`method`, `VARCHAR(255)`, V9 migration; JPA `@Enumerated(STRING) var method: EstimationMethod`) — the estimation's `EstimationMethod`, chosen at creation and **immutable** (there is no estimation-update endpoint, so nothing mutates it after create). Every version of one estimation shares its method.

Single self-referential table per side, JPA `SINGLE_TABLE` inheritance with a `node_type` discriminator (`GROUP` | `FIXED` | `TIME_RELATIVE`). Children ordered by an integer `position` column (`@OrderColumn`). The version's `roots` use `@SQLRestriction("parent_id IS NULL")` to pick out the top-level rows.

- **Draft** side: `draft_estimation_nodes` (V6 migration); JPA entity `DraftEstimationNode` with three `@DiscriminatorValue` subclasses (`DraftGroupNode`, `DraftFixedItemNode`, `DraftTimeRelativeItemNode`). Mutable, edited via the REST PUT endpoint.
- **Submitted** side: `submitted_estimation_nodes` (V7 migration); mirrors the draft shape with `SubmittedEstimationNode` + `SubmittedGroupNode`/`SubmittedFixedItemNode`/`SubmittedTimeRelativeItemNode`. **Submitted entities are immutable snapshots** that store the calculated values directly on every row (mean, offerPT, cost, …); group rows store accumulated values for their subtree. The backend never recomputes on read.

**Undo log** (V8 migration): `draft_estimation_versions` gains a `revision` counter plus `last_modified_by` / `last_modified_at` audit columns, and a new append-only `draft_mutation_log` table records one row per applied `DraftMutation` (`DraftMutationLogEntry`): monotonic `sequence_number` per draft, `revision_before`/`revision_after`, `kind` discriminator, `payload` + `inverse_payload` (`jsonb`, mapped `String` via `@JdbcTypeCode(SqlTypes.JSON)`), and an `ACTIVE`/`UNDONE` status. Finders live on `DraftMutationLogRepository`. `UndoService` records one entry per draft-changing `updateDraft` (skipping no-op PUTs via `EstimationVersion.diff`), and provides per-user `undoLastForUser` / `redoLastForUser` (restoring the stored `DraftUpdateDto` snapshot via `DraftUpdateApplier`) plus `historyFor`; a cross-user change on top of the target raises `UndoConflictException`. Snapshots are stored as `StoredMutation(kind, before, after)` DTO JSON (`DraftMutationJackson`), not raw domain types. Submitting a draft cascade-deletes its log.

Use `@SQLRestriction`, not the deprecated `@Where`, for filtered collections.

### Frontend ↔ domain bridge (`adapter.ts`)

`src/frontend/src/lib/adapter.ts` computes the local calc map by calling into the Kotlin/JS-compiled domain (`createVersion(...).calculate()`) and then walking the result tree recursively. Both the wire DTOs (`EstimationVersionDto.roots`, `DraftUpdateDto.roots`) and the Kotlin/JS factories (`createVersion(roots: Array<EstimationNode>)`, `createGroup(children: Array<EstimationNode>) → EstimationGroup`) speak the canonical tree shape — no legacy `itemGroups` field anywhere. If you see a stale `.d.mts` (TypeScript picks `.d.mts` for `.mjs` imports), force a full rebuild: `./gradlew :domain:clean :frontend:clean :frontend:check`. (Kotlin 2.3.x emits `domain.d.mts` directly; `prepareTypescriptArtifacts` copies it through.)

### Kotlin/JS gotcha — super-property recursion

`super.<computed property>` infinite-loops on Kotlin/JS but works on JVM (see the comment in `TimeRelativeEstimationItem.kt`). When overriding a getter that needs the parent's formula, **inline the formula**; never call `super.mean` / `super.variance`. The build will compile and the JVM tests will pass, but the frontend will throw "Maximum call stack size exceeded" at runtime.

### Kotlin/JS gotcha — secondary constructors

`@JsExport`-ed classes need `@JsName("…")` on secondary constructors (Kotlin/JS rejects them otherwise). Two `data class` constructors whose value types erase to the same JVM signature (e.g. two `List<X>`) cause a platform declaration clash — differentiate by arity, not by type alone.

### Reproducible Jib backend image

`src/backend/implementation/gradle.properties` configures Jib for byte-deterministic images. **Both** `quarkus.jib.use-current-timestamp` and `quarkus.jib.use-current-timestamp-file-modification` must be `false`. Setting only one yields non-reproducible builds.

## Conventions

### Single source of truth — domain logic

Anything you'd want to call "business logic" (PERT, accumulation, risk surcharge, cost rules, version equality, invariants) lives in `src/domain/`. Do not duplicate it in backend services or frontend stores. The backend and frontend exist to map persisted state ↔ domain and to render / edit; they do not re-derive numbers.

### Backend run-of-the-mill patterns

- Domain → entity mapping in `…/service/DraftVersionMapper.kt`; entity → DTO mapping in `…/rest/dto/EstimationVersionMapper.kt`; entity → `DraftUpdateDto` snapshot in `…/service/DraftSnapshotMapper.kt` (`DraftEstimationVersion.toUpdateDto()`).
- Writing a `DraftUpdateDto` into a draft entity is centralised in `…/service/DraftUpdateApplier.kt` (`apply(draft, update)`) — the `dto.field?.let { … list.clear(); list.add(…) }` clear-and-rebuild shape (rather than diffing). Both the REST PUT `updateDraft` and the Undo service call it; `toUpdateDto()` is its inverse (capture), so `apply(draft, draft.toUpdateDto())` is a no-op on state.
- Repositories are Panache (`PanacheRepository<T>`). Add finders as extension methods on the repository class.

### Frontend — error surfacing rule

Every `try`/`catch` MUST surface failure to the user via the shared `ErrorBanner` component (`src/frontend/src/lib/components/ErrorBanner.svelte`). A `catch` that returns a fallback while only logging to `console.error` is **not** acceptable — that is the bug pattern that hid the Kotlin/JS stack-overflow as blank `—` cells in the grid.

### Frontend — `$bindable` two-way binding (editor components)

The version editor lets Svelte own state end-to-end. The page (`routes/estimations/[id]/versions/[versionNumber]/+page.svelte`) holds one source-of-truth `$state` per collection (`currentParameters`, `currentDrivers`, `currentPhases`, `currentAdditionalCosts`, `currentRoots`, `currentNotes`), normalised once in `loadVersion()` (roots via `normalizeRoots` from `$lib/estimationNodes`). It passes each down with `bind:` to the editor children — the four sibling panels (`ParametersPanel`, `EffortDriversPanel`, `PhasesPanel`, `AdditionalCostsPanel`) and `EstimationGrid` — which declare the data prop as `$bindable()` and **mutate it directly** (`push`/`splice`/field assignment, or reassign for wholesale replacement). There is no local snapshot, no `notify()`, and no `onchange` callback; Svelte propagates child mutations back to the page. Autosave is a single guarded `$effect` in the page: it deep-reads the editable state via `$state.snapshot(...)`, compares against a `lastSavedSnapshot` baseline set at load, and debounces a PUT only on an actual edit to a draft (never on load/reload). Do NOT reintroduce the old "snapshot prop into local `$state` + onchange" pattern. (`TreeTable.collapsed` is the one exception that stays a deliberately local `$state` seeded from `initialCollapsed` — it is private UI state the page does not own.)

### Frontend — undo/redo store (`UndoStore`)

Persistent undo/redo (task-076) is a thin client over the backend endpoints, wired into the version editor page. `src/frontend/src/lib/stores/undo.svelte.ts` exports the `UndoStore` runes class (`history`/`loading`/`conflict`/`error` `$state`, `canUndo`/`canRedo` `$derived`, and `refresh`/`undo`/`redo`/`clearConflict`). It owns **only** the mutation-log view — NOT the draft state: after a successful `undo`/`redo` it calls the owner-supplied `onResult(version)` hook, and the page replaces its draft `$state` through the same `applyVersionData` normalize path used on load (which also resets the autosave baseline, so re-hydrating from an undo never triggers a spurious PUT). The page calls `store.refresh()` after the initial draft load and after every successful autosave PUT, and mirrors `store.error` into its existing `bannerMessage`/`ErrorBanner`. A 409 is parsed into `conflict` (no throw). `src/frontend/src/lib/stores/undoKeyboard.svelte.ts` exports `installUndoShortcuts(store)` (Ctrl/Cmd+Z = undo, Ctrl/Cmd+Shift+Z / Ctrl+Y = redo), installed via an `$effect`; it defers to native input undo unless the target sits inside the grid's `data-undo-aware="true"` surface (set on `EstimationGrid`'s container). All HTTP goes through `apiFetch`; DTOs are the **generated** `components['schemas']` types from `$lib/api/schema` — this store is the first consumer of the generated `schema.d.ts` (existing components still use the hand-written `$lib/api/types.ts`). The visible UI (task-077) lives on the version editor page: Rückgängig/Wiederholen toolbar buttons (disabled via `canUndo`/`canRedo`, tooltips drawn from `history`), a collapsible `UndoHistoryPanel` below `EstimationGrid`, and a `UndoConflictDialog` shown whenever `conflict` is non-null (its "Aktuellen Stand neu laden" action re-runs the page's `loadVersion` + `refresh` + `clearConflict`; 409s go to this dialog, never to `ErrorBanner`).

### Frontend — generic `TreeTable` component

`src/frontend/src/lib/components/treetable/TreeTable.svelte` is a domain-agnostic, OmniOutliner-style tree-table component generic over a node type `T`. Callers supply `getId` / `getChildren` + a column array whose `cell` snippets render per-row content (branching on the node's data discriminator to draw editors on leaves and aggregates on group rows). The component owns indent guide lines, expand/collapse, nested per-group `svelte-dnd-action` dndzones, cycle protection (deep-clone snapshot + microtask-deferred structural-anomaly check: duplicate IDs OR lost IDs), and the header / optional footer / optional per-row actions slot. The `roots` prop is `$bindable` so callers can run imperative add/delete mutations that the component reflects immediately. Optional `rowAttrs` and `childrenZoneAttrs` hooks let callers attach extra attributes (e.g. legacy testids, per-group aria-labels) to outer row wrappers and to children dndzones. Types live in `treetable/types.ts`. **Production consumer**: `src/frontend/src/lib/components/EstimationGrid.svelte` (the estimation editor — composes TreeTable with 11 column snippets, calc-map aggregation, keyboard cell-nav, and the `+ group` / `+ item` / `✕` row actions). **Regression harness**: `src/frontend/src/routes/dev/tree-table-demo/+page.svelte` (a static product-catalog fixture exercising the generic API, accessible at `/dev/tree-table-demo`). Columns may opt in to auto-collapse on narrow containers via `collapsible: true`; the threshold is configurable per-TreeTable through `collapseBreakpointPx` (default 900 px). The outer wrapper is horizontally scrollable, so content that exceeds the viewport stays reachable even when no columns collapse further. Per-row action buttons (the `rowActions` snippet) live in a dedicated trailing column by default; callers can pass `actionsPlacement="treeColumn"` to inline the actions inside the tree column instead — EstimationGrid uses this so its `+ group` / `+ item` / `✕` controls sit directly after the row title rather than off to the right past every value column.

### Logging

One logging approach per module — use it; do not invent a second mechanism.

- **Backend** (Quarkus): log via the static `io.quarkus.logging.Log` API (`Log.info/debug/warn/error`) — no per-class logger field. Levels are configured per profile in `application.properties`: app code (`io.github.theestimator`) at DEBUG in `%dev`/`%test`, INFO in `%prod`; the root level stays at the Quarkus default. The PROD profile emits structured JSON (`%prod.quarkus.log.console.json=true`, backed by the `quarkus-logging-json` dependency) for Kubernetes; the human-readable console stays the default in dev. `EstimationVersionService` is the reference site (INFO on create/submit/delete with the estimation id, DEBUG for finer detail, ERROR on failure paths).
- **Domain** (Kotlin Multiplatform): use the `io.github.oshai:kotlin-logging` facade — `private val logger = KotlinLogging.logger {}` and the lambda form `logger.debug { "…" }` (allocation-free when the level is off). On the JVM target it delegates to slf4j (the backend supplies a provider via Quarkus' JBoss LogManager bridge; the domain's own JVM tests pull `slf4j-simple` as `runtimeOnly`); on the JS target it writes to the browser console, so the frontend sees domain logs in dev. Logging is observation only — never control flow; the business math stays the single source of truth.
- **Frontend** (SvelteKit): import `log` from `$lib/log.ts` (a configured `loglevel` instance — `debug` under `import.meta.env.DEV`, else `warn`). **Never write bare `console.*`.** The error-surfacing rule still holds: a `catch` must surface failure via `ErrorBanner` — `log.error(...)` is *in addition to*, not instead of, user-facing surfacing.

### Modular authentication and authorization

Auth is provider-modular. The active provider is picked by the backend property `app.auth.provider` and the matching frontend env var `VITE_AUTH_PROVIDER` — values `dev` | `entra` | `keycloak`. Backend SPI lives in `src/backend/implementation/src/main/kotlin/io/github/theestimator/auth/`: `AuthModule` (interface), `Role` (enum `VIEWER` / `ESTIMATOR` / `ADMIN`), `CurrentUser` (provider-agnostic principal), `CurrentUserProvider` (`@RequestScoped` bean populated by each concrete module via a filter or `SecurityIdentityAugmentor`), and `AuthConfig` (binds `app.auth.provider`). The `/api/auth/me` endpoint returns the current user. Frontend SPI lives in `src/frontend/src/lib/auth/`: `AuthProvider` interface (`init`/`login`/`logout`/`getAccount`/`getAuthorizationHeader`) and `getAuthProvider()` factory. The API wrapper `apiFetch` (`src/frontend/src/lib/api/fetch.ts`) consults `getAuthProvider().getAuthorizationHeader()` and attaches the result as the `Authorization` header on every `/api` call. Concrete providers land in task-006 (dev / hard-wired users — see `docs/auth-dev.md`), task-007 (Entra ID via MSAL + quarkus-oidc — see `docs/entra-setup.md`), and task-060 (Keycloak); resources, panels, and routes never touch provider specifics — they read `CurrentUserProvider` / `getAuthProvider()` only. `@RolesAllowed` is operational under both modules — the dev and Entra augmentors populate the `SecurityIdentity` with Quarkus role strings derived from `CurrentUserProvider` / OIDC claims respectively. The REST resources are **role-protected** (task-091): reads (GET) require `VIEWER`, writes (POST/PUT/DELETE) require `ESTIMATOR`, and `/api/admin/ping` requires `ADMIN`; an anonymous request → `401`, a wrong-role request → `403` (see `docs/authentication.md`). The dev module is **strict**: there is no default-user fallback, so a request without a valid `Authorization: Dev <subjectId>` header yields `401`. Its `Dev <subjectId>` header is **forgeable** (no token/signature), so it is fail-closed against production (task-092): `app.auth.provider` has no default value (a profile that omits it fails to start), and `AuthProviderGuard` (a `@Observes StartupEvent` bean) refuses to boot when the `dev` provider is active under a `NORMAL` launch. Tests authenticate via the `DevAdminAuth` JUnit5 extension (`src/backend/implementation/src/test/kotlin/io/github/theestimator/auth/DevAuthTestSupport.kt`).

**Just-in-time user provisioning**: the auth modules only populate an in-memory `CurrentUser`; a persisted `User` row is created on first sighting by `CurrentUserService.ensureUser(principal)` (keyed by the unique `entraSubjectId`). The reliable trigger is `UserProvisioningFilter` — a provider-agnostic `@Blocking` request filter (`auth/`) that provisions the current user on every authenticated **mutating** request (POST/PUT/PATCH/DELETE); `GET /api/auth/me` also provisions. This is what makes `User` FKs (audit, ownership, the undo log) resolvable. `ensureUser` takes the principal as a parameter (pure to test; no DB writes on the Vert.x event loop). Note the frontend builds its account client-side and does **not** call `/api/auth/me`, so the filter — not `/me` — is what provisions users in normal use.

## Planning system

This project uses a YAML-based task plan under `planning/`:

- `planning/plan.yaml` — stack, domain, phases, task index.
- `planning/tasks/task-NNN.yaml` — one self-contained spec per task (description, steps, validation commands, outputs).
- `planning/status.json` — mutable progress; updated via `./scripts/task.sh start|done|pending <task-id>` (requires `jq`).
- `.claude/commands/` — slash commands `add-task`, `implement-task`, `improve-task` automate the lifecycle.

When implementing a task, read its YAML in full, run `./scripts/task.sh start`, follow the steps, run **every** validation command, and only call `./scripts/task.sh done` after `./gradlew build` and `./gradlew :frontend:check` are green. Older task YAMLs may reference `./mvnw …` / bare `mvn …` from before the Gradle migration (task-082); translate those to the equivalent `./gradlew …` invocation (`./gradlew build`, `./gradlew :backend:implementation:test`, `./gradlew :frontend:check`).

## REST API surface

Every JSON endpoint documents its **request and response** schemas plus real status codes (401/403/404/409/204) in the generated OpenAPI document (task-094). The JAX-RS methods keep their `jakarta.ws.rs.core.Response` return type; response bodies are declared with MicroProfile OpenAPI annotations (`@APIResponse` + `@Content`/`@Schema`, `@Tag`, `@Operation`). SmallRye writes the document to `src/backend/implementation/target/openapi/openapi.json` on build (`quarkus.smallrye-openapi.store-schema-directory`); the committed copy at `src/frontend/src/lib/api/openapi.json` and the typed client `src/frontend/src/lib/api/schema.d.ts` are regenerated from it via `cd src/frontend && npm run gen:api`. (Frontend component code still consumes the hand-written `$lib/api/types.ts`; migrating it onto the generated `schema.d.ts` is a separate follow-up.)

Base path `/api/`. Key endpoints under `EstimationVersionResource`:

- `GET  /api/estimations/{eid}/versions` — list (draft first, then submitted)
- `POST /api/estimations/{eid}/versions` — create draft (clones from latest submitted if any)
- `GET  /api/estimations/{eid}/versions/draft` — returns the draft with **on-the-fly calculated** values
- `PUT  /api/estimations/{eid}/versions/draft` — body is `DraftUpdateDto`; replaces collections wholesale
- `POST /api/estimations/{eid}/versions/draft/submit` — snapshot draft → SubmittedEstimationVersion
- `POST /api/estimations/{eid}/versions/draft/undo` — undo this user's last mutation → recalculated draft; `409` (`ConflictDetailsDto`) if a newer change blocks it (task-075)
- `POST /api/estimations/{eid}/versions/draft/redo` — redo the last undone mutation → recalculated draft; `409` on conflict
- `GET  /api/estimations/{eid}/versions/draft/history` — the draft's mutation log (`List<MutationLogEntryDto>`, ACTIVE + UNDONE, ordered by sequence)
- `GET  /api/estimations/{eid}/versions/{n}` — read a submitted version (stored calc values)
- `GET  /api/estimations/{eid}/versions/{a}/compare/{b}` — diff (use `draft` for the live draft)
- `GET  /api/estimations/{eid}/versions/{n}/export?format=xlsx|csv` — exports

Auth endpoints (provider-agnostic, populated by whichever concrete module is active):

- `GET  /api/auth/me` — current `CurrentUser` (subjectId, email, displayName, roles, providerName) or `401` when no user is populated for the request.
- `GET  /api/admin/ping` — `@RolesAllowed("ADMIN")` canary that returns `{message: "pong", user: <subjectId>}`. Used by `e2e/auth.test.ts` to prove role enforcement under the dev module; survives feature churn.

`DraftUpdateDto.roots: List<EstimationNodeUpdateDto>` carries the editable tree (recursive children, `type` discriminator). Responses use `EstimationNodeDto` with calculated fields populated at every level (groups carry accumulated values). Both leaf DTOs are **flat data classes** with a `type: String` discriminator (`@Schema(enumeration = ["GROUP","FIXED","TIME_RELATIVE","BUCKETED"])`) — no Jackson polymorphism. `BUCKETED` is **reserved** (task-100) for the bucket+sampled method, with `bucketId` / `isSample` fields on the flat DTO; `DraftUpdateApplier` currently rejects any `type: "BUCKETED"` node with `400` (a WARN is logged) pending task-103's persistence path.

`POST /api/projects/{id}/estimations` (`ProjectResource.createEstimation`) accepts an `EstimationCreateDto` whose `method: EstimationMethod` selects the estimation method (defaults to `THREE_POINT_PERT` when omitted); the estimation read DTOs (`EstimationSummaryDto`, `EstimationDetailDto`) return it. The method is immutable — there is no estimation-update endpoint, so it cannot change after creation.
