# Tech Stack

| Layer    | Technology                      |
|----------|---------------------------------|
| Frontend | SvelteKit + TypeScript (Vite)   |
| Styling  | Tailwind CSS 4                  |
| Backend  | Quarkus (Java 21)               |
| Database | PostgreSQL 16                   |
| Platform | Kubernetes (Minikube for local) |
| Auth     | Microsoft Entra ID (MSAL/OIDC)  |

# Repository Structure

```
src/
  backend/    — Quarkus REST API
  frontend/   — SvelteKit SPA
  domain/     — Kotlin Multiplatform: single source of truth for domain logic
  k8s/        — Kubernetes manifests (Kustomize)
docs/         — Architecture and design documents
scripts/      — Helper scripts for local development
planning/     — Project plan and task definitions
```

# Distributed components

At runtime the system is a small distributed system of four components, plus
one external dependency. These component names — **Browser**, **Frontend**,
**Backend**, **Database** — are the canonical vocabulary used throughout this
documentation, and each maps directly onto a Kubernetes resource.

| Component    | What it is                                                | Kubernetes resource(s)                                                              | Port |
|--------------|-----------------------------------------------------------|-------------------------------------------------------------------------------------|------|
| **Browser**  | The end user's web browser running the SPA (the client)   | — (not deployed)                                                                    | —    |
| **Frontend** | nginx serving the SvelteKit SPA and proxying `/api`        | `frontend` Deployment + Service                                                     | 80   |
| **Backend**  | Quarkus REST API under `/api/…`                            | `backend` Deployment + Service, `backend-config` ConfigMap, `backend` HPA           | 8080 |
| **Database** | PostgreSQL 16                                              | `postgres` StatefulSet + Service, `postgres-credentials` Secret, `postgres-data` PVC | 5432 |

The external **Identity Provider** (Microsoft Entra ID) is not deployed by this
project: the Browser obtains tokens from it (MSAL) and the Backend validates
them (OIDC). In the `dev` profile it is replaced by the static dev auth module —
see [authentication.md](./authentication.md).

```
┌─────────┐   HTTP(S)   ┌──────────┐   /api/* (proxy)   ┌──────────┐   JDBC   ┌──────────┐
│ Browser │ ──────────▶ │ Frontend │ ─────────────────▶ │ Backend  │ ───────▶ │ Database │
│  (SPA)  │             │ (nginx)  │                    │ (Quarkus)│          │(Postgres)│
└─────────┘             └──────────┘                    └──────────┘          └──────────┘
     │                                                        ▲
     │ login: obtain token (MSAL)        validate token (OIDC)│
     └────────────────▶  Identity Provider (Microsoft Entra ID)  ◀───────────┘
```

The Browser loads the SPA from the Frontend; the SPA's API calls hit the
Frontend's `/api` path, which nginx proxies to the Backend Service on `:8080`;
the Backend persists to the Database on `:5432`. Every API call carries an
`Authorization` header — a `Bearer` token under Entra, or `Dev <subjectId>`
under the dev module.

## Kubernetes as the component model

Kubernetes is the deployment and component model: each component above is one
Kubernetes workload — a Deployment for the stateless Frontend and Backend, a
StatefulSet for the stateful Database — fronted by a Service, all in the
`estimation` namespace, with a single Ingress (`estimation.local`) routing
external traffic to the Frontend. Manifests live under `k8s/` and are composed
with Kustomize: a `base/` plus overlays (`overlays/minikube` for local,
`overlays/production` which adds Backend OIDC config and a TLS Ingress). The
Backend scales via a HorizontalPodAutoscaler; the Database keeps a persistent
volume. The Backend and Frontend container images
(`theestimator/estimation-backend`, `theestimator/estimation-frontend`) are produced by
the Gradle build (Jib for the Backend) and deployed locally with
`./scripts/minikube-deploy.sh`.

# Domain module: single source of truth

The `domain` module is a Kotlin Multiplatform project that contains **all
business logic and domain models** shared between frontend and backend.

- **Backend** consumes it as a regular JVM dependency via
  `implementation(project(":domain"))` (the KMP JVM target).
- **Frontend** consumes it as TypeScript, compiled to JS/TS by the
  Kotlin/JS compiler and unpacked into `src/lib/domain` from the domain's
  `typescriptDist` Gradle configuration during the build.

Domain logic (calculations, validation, data structures) must live in `domain`.
Neither the Quarkus backend nor the SvelteKit frontend should duplicate or
reimplement domain rules — they only call into the shared domain code.

### Single Gradle multi-project build

The whole repo is one **Gradle** build (`./gradlew build`, `./gradlew detekt`,
…). The Gradle wrapper at the repo root (`gradlew`, `gradlew.bat`,
`gradle/wrapper/`) pins the Gradle version, so contributors don't need a
system `gradle`. `settings.gradle.kts` includes four projects mapped onto the
`src/` layout: `:domain`, `:backend:implementation`, `:backend:end2end`, and
`:frontend`. Plugin versions are declared once in the root `build.gradle.kts`
with `apply false` so every subproject applies the same version — in
particular the Kotlin Gradle plugin, which is one shared artifact across
`:domain` (multiplatform) and `:backend:implementation` (jvm) and must not
diverge.

`src/domain/` is the only Kotlin Multiplatform module, producing **two**
artefacts from one source tree:

1. a JVM jar consumed by the backend via `implementation(project(":domain"))`,
   and
2. a TypeScript-typed JS library consumed by the frontend.

The KMP toolchain — `kotlin("multiplatform")` + `binaries.library()` +
`generateTypeScriptDefinitions()` + the `prepareTypescriptArtifacts` /
`packageTypescript` tasks — produces `domain.mjs` / `domain.d.mts` and packages
them into a zip. The domain exposes that zip as a consumable `typescriptDist`
configuration; `:frontend` declares a dependency on it and a `Sync` task
(`unpackDomainTypescript`) unpacks it into `src/lib/domain` before the
SvelteKit build, so there is no published npm package or Maven classifier
artifact in the loop.

The backend uses the `io.quarkus` Gradle plugin (dev mode: `quarkusDev`; image
build: `imageBuild`). The frontend uses the Gradle Node plugin
(`com.github.node-gradle.node`) to install Node/npm and run the `gen:api`,
`build`, `check`, and `lint:report` npm scripts. detekt (Kotlin) and ESLint
(TS/Svelte/HTML) run as Gradle tasks with informational reports.

This replaced an earlier hybrid where a top-level Maven reactor delegated the
KMP module to Gradle via `exec-maven-plugin` ("Maven outside, Gradle inside").
That hybrid could not be synced by IntelliJ IDEA (the same `src/domain`
directory was both a Maven module and a Gradle build); the single Gradle build
removes that conflict.

# Authentication

This application uses a modular approach for authentication. For easy development there is a module with static authentication.

Authenticated principals are provisioned into the `users` table just-in-time: `UserProvisioningFilter` — a provider-agnostic `@Blocking` request filter — calls `CurrentUserService.ensureUser(principal)` on every authenticated mutating request (POST/PUT/PATCH/DELETE), find-or-creating a `User` keyed by the unique `entraSubjectId` (`GET /api/auth/me` also provisions). This guarantees that `User` foreign keys (audit, ownership, the undo/mutation log) always resolve, even though the auth modules themselves only expose an in-memory `CurrentUser`. (The frontend builds its account client-side and does not call `/api/auth/me`, so the filter is the trigger that matters in normal use.)

More details are described in [authentication.md](./authentication.md).

# Frontend conventions

## Error handling: no silent try/catch

Every `try`/`catch` block in the SvelteKit frontend MUST surface failures to
the user via the shared `ErrorBanner` component
(`src/frontend/src/lib/components/ErrorBanner.svelte`). A catch block must NOT:

- return a fallback value while logging only to `console.error` (the failure
  is invisible to the user — see the original calcMap bug that hid a
  Kotlin/JS stack overflow as blank `—` cells), nor
- revert UI state silently (e.g. an autosave catch that just sets the status
  back to `idle`).

Pattern to follow — set a banner-visible state, render
`<ErrorBanner message={...} ondismiss={() => (...)} />` near the top of the
view, and (optionally) keep a `console.error` for developers.

## State management: `$bindable`, not snapshot + callback

Editor components let Svelte own state via two-way binding rather than copying
props into local state. The version page
(`routes/estimations/[id]/versions/[versionNumber]/+page.svelte`) keeps a single
source-of-truth `$state` per collection — parameters, effort drivers, phases,
additional costs, roots, notes — normalised once at load (`normalizeRoots` from
`$lib/estimationNodes`). It passes each down with `bind:`; the editor children
(`ParametersPanel`, `EffortDriversPanel`, `PhasesPanel`, `AdditionalCostsPanel`,
`EstimationGrid`) declare the data prop as `$bindable()` and **mutate it
directly**. No local snapshot, no `notify()`, no `onchange` callback — Svelte
propagates child edits back to the page.

Autosave is one guarded `$effect` on the page: it deep-reads the editable state
with `$state.snapshot(...)`, compares against a baseline captured at load, and
debounces the draft PUT only when an actual edit changed something (never on
load or reload).

Exception: `TreeTable`'s `collapsed` stays a deliberately local `$state` seeded
from the `initialCollapsed` prop — it is private expand/collapse UI state that
the page does not own, so it is not bindable.