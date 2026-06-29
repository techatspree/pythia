# Tech Stack

| Layer    | Technology                      |
|----------|---------------------------------|
| Frontend | SvelteKit + TypeScript (Vite)   |
| Styling  | Tailwind CSS 4                  |
| Backend  | Quarkus (Java 21)               |
| Database | PostgreSQL 16 (H2 for dev-local)|
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

# Athentication

This application uses a modular approach for authentication. For easy development there is a module with static authentication.

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