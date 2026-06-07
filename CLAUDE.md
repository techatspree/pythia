# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

TheEstimator is a project-effort estimation tool: PERT three-point estimates (optimistic / likely / pessimistic) per work item, organised in an arbitrary-depth tree of groups and leaves, with versioned draft → submitted snapshots, audit trail, and comparison between versions. The German-language UI and domain vocabulary (Aufwand, Risiko, Tagessatz, …) are intentional.

## Build & run

Top-level Maven reactor drives every module — there is **no `mvnw` wrapper**, use system `mvn`.

```
mvn clean install                       # full build, all tests (frontend + backend)
mvn -DskipTests package                 # build everything, skip tests
mvn -pl src/domain -am test             # domain (KMP) tests only
mvn -pl src/backend/implementation -am test   # backend unit + IT (Testcontainers)
cd src/frontend && npm run check        # TS/Svelte type-check
cd src/frontend && npm run test:e2e     # Playwright

./scripts/dev-local.sh                  # H2 backend + Vite frontend, one command
./scripts/minikube-deploy.sh            # full PostgreSQL + Quarkus stack on minikube
```

Run one backend test class:
`mvn -pl src/backend/implementation test -Dtest=EstimationVersionResourceIT`

The dev-local profile uses H2 in-memory; dev-minikube and prod use PostgreSQL 16 via Flyway migrations (`src/backend/implementation/src/main/resources/db/migration/V*.sql`). `%test` uses PostgreSQL Testcontainers automatically — integration tests need Docker running locally.

## Architecture

Three Maven modules:

- **`src/domain`** — Kotlin Multiplatform (JVM + JS). Compiles to a JVM jar for the backend and to TypeScript + JS for the frontend (published as an npm package during the Maven build via Gradle: `compileKotlinJvm`, `jvmTest`, `packageTypescript`). **Holds all business logic** — PERT calculation, accumulation, the tree-shape domain model. The backend and frontend MUST NOT reimplement domain rules; they call into it.
- **`src/backend/implementation`** — Quarkus 3 (Java 21, Kotlin), Hibernate ORM Panache, Flyway, Jib for container images. REST endpoints under `/api/`.
- **`src/frontend`** — SvelteKit 5 (runes) + TypeScript + Tailwind 4, Vite, adapter-static (SPA). Talks to the backend via `$lib/api/client.ts` (openapi-fetch with types regenerated from the backend OpenAPI).

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

### Persistence shape

Single self-referential table per side, JPA `SINGLE_TABLE` inheritance with a `node_type` discriminator (`GROUP` | `FIXED` | `TIME_RELATIVE`). Children ordered by an integer `position` column (`@OrderColumn`). The version's `roots` use `@SQLRestriction("parent_id IS NULL")` to pick out the top-level rows.

- **Draft** side: `draft_estimation_nodes` (V6 migration); JPA entity `DraftEstimationNode` with three `@DiscriminatorValue` subclasses (`DraftGroupNode`, `DraftFixedItemNode`, `DraftTimeRelativeItemNode`). Mutable, edited via the REST PUT endpoint.
- **Submitted** side: still flat `submitted_estimation_item_groups` + `submitted_estimation_items` as of the current state — task-051 moves it to the same tree shape. **Submitted entities are immutable snapshots** that store the calculated values directly (mean, offerPT, cost, …); the backend never recomputes on read.

Use `@SQLRestriction`, not the deprecated `@Where`, for filtered collections.

### Frontend ↔ domain bridge (`adapter.ts`)

`src/frontend/src/lib/adapter.ts` computes the local calc map by calling into the Kotlin/JS-compiled domain (`createVersion(...).calculate()`) and then walking the result. The frontend still talks to the JS-exported **legacy `EstimationVersion.itemGroups`** view (a `@Deprecated` shim on the domain `EstimationVersion`) until task-054 rewrites adapter.ts to walk `roots`. Domain factories in `DomainFactory.kt` keep accepting `itemGroups: Array<EstimationItemGroup>` for the same reason. Do not remove the shim before task-054.

### Kotlin/JS gotcha — super-property recursion

`super.<computed property>` infinite-loops on Kotlin/JS but works on JVM (see the comment in `TimeRelativeEstimationItem.kt`). When overriding a getter that needs the parent's formula, **inline the formula**; never call `super.mean` / `super.variance`. The build will compile and the JVM tests will pass, but the frontend will throw "Maximum call stack size exceeded" at runtime.

### Kotlin/JS gotcha — secondary constructors

`@JsExport`-ed classes need `@JsName("…")` on secondary constructors (Kotlin/JS rejects them otherwise). Two `data class` constructors whose value types erase to the same JVM signature (e.g. two `List<X>`) cause a platform declaration clash — differentiate by arity, not by type alone.

### Reproducible Jib backend image

`src/backend/implementation/pom.xml` configures Jib for byte-deterministic images. **Both** `quarkus.container-image.jib.use-current-timestamp` and `…use-current-timestamp-for-system-libraries` must be `false`. Setting only one yields non-reproducible builds.

## Conventions

### Single source of truth — domain logic

Anything you'd want to call "business logic" (PERT, accumulation, risk surcharge, cost rules, version equality, invariants) lives in `src/domain/`. Do not duplicate it in backend services or frontend stores. The backend and frontend exist to map persisted state ↔ domain and to render / edit; they do not re-derive numbers.

### Backend run-of-the-mill patterns

- Domain → entity mapping in `…/service/DraftVersionMapper.kt`; entity → DTO mapping in `…/rest/dto/EstimationVersionMapper.kt`.
- REST PUT (e.g. `updateDraft`) follows a `dto.field?.let { … list.clear(); list.add(…) }` shape for each collection — clear-and-rebuild rather than diffing.
- Repositories are Panache (`PanacheRepository<T>`). Add finders as extension methods on the repository class.

### Frontend — error surfacing rule

Every `try`/`catch` MUST surface failure to the user via the shared `ErrorBanner` component (`src/frontend/src/lib/components/ErrorBanner.svelte`). A `catch` that returns a fallback while only logging to `console.error` is **not** acceptable — that is the bug pattern that hid the Kotlin/JS stack-overflow as blank `—` cells in the grid.

### Frontend — `$state` snapshot idiom (panels)

Sibling panels (`PhasesPanel`, `ParametersPanel`, `EffortDriversPanel`, `AdditionalCostsPanel`) all snapshot the prop into local `$state` once and never re-sync. Do NOT use `$derived` or two-way bindings to keep `items` in sync with the prop — match the existing pattern verbatim.

## Planning system

This project uses a YAML-based task plan under `planning/`:

- `planning/plan.yaml` — stack, domain, phases, task index.
- `planning/tasks/task-NNN.yaml` — one self-contained spec per task (description, steps, validation commands, outputs).
- `planning/status.json` — mutable progress; updated via `./scripts/task.sh start|done|pending <task-id>` (requires `jq`).
- `.claude/commands/` — slash commands `add-task`, `implement-task`, `improve-task` automate the lifecycle.

When implementing a task, read its YAML in full, run `./scripts/task.sh start`, follow the steps, run **every** validation command, and only call `./scripts/task.sh done` after `mvn package` and `cd src/frontend && npm run check` are green. Never amend the existing build wrapper to `./mvnw` — it doesn't exist.

## REST API surface

Base path `/api/`. Key endpoints under `EstimationVersionResource`:

- `GET  /api/estimations/{eid}/versions` — list (draft first, then submitted)
- `POST /api/estimations/{eid}/versions` — create draft (clones from latest submitted if any)
- `GET  /api/estimations/{eid}/versions/draft` — returns the draft with **on-the-fly calculated** values
- `PUT  /api/estimations/{eid}/versions/draft` — body is `DraftUpdateDto`; replaces collections wholesale
- `POST /api/estimations/{eid}/versions/draft/submit` — snapshot draft → SubmittedEstimationVersion
- `GET  /api/estimations/{eid}/versions/{n}` — read a submitted version (stored calc values)
- `GET  /api/estimations/{eid}/versions/{a}/compare/{b}` — diff (use `draft` for the live draft)
- `GET  /api/estimations/{eid}/versions/{n}/export?format=xlsx|csv` — exports

`DraftUpdateDto.roots: List<EstimationNodeUpdateDto>` carries the editable tree (recursive children, `type` discriminator). Responses use `EstimationNodeDto` with calculated fields populated at every level (groups carry accumulated values).
