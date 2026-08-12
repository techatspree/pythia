# Development

## Prerequisites

- Java 21 (managed via jenv)
- Node.js (downloaded automatically by the Gradle build)
- Docker (required — local dev and the test suite start PostgreSQL via Quarkus Dev Services)
- Minikube
- kubectl

Gradle itself is **not** a prerequisite — the repo ships a Gradle wrapper
(`./gradlew` and `./gradlew.bat` at the repo root, with the version pinned by
`gradle/wrapper/gradle-wrapper.properties`). On first run `./gradlew`
downloads the right Gradle version; subsequent runs reuse it.

## Building

The project is a single Gradle multi-project build covering domain, backend,
and frontend.

```bash
# Full build (domain + backend + frontend, all tests)
./gradlew build

# Build, skipping tests
./gradlew build -x test

# Domain (KMP) build + tests only
./gradlew :domain:build

# Backend unit + @QuarkusTest (PostgreSQL via Dev Services — Docker required)
./gradlew :backend:implementation:test

# Frontend type-check + ESLint
./gradlew :frontend:check

# Run end-to-end tests (requires a running backend)
./gradlew :backend:end2end:e2eTest
```

## Gradle 10 readiness

The build runs on the Gradle **9.6.1** wrapper. Our own build scripts are
deprecation-free (`./gradlew build -x test --warning-mode all` reports nothing
from any `*.gradle.kts` in this repo). The wrapper is intentionally **not**
advanced to Gradle 10 yet, because three deprecations remain — all emitted
**inside third-party plugins**, none of which has a released version that fixes
them (verified 2026-07; Gradle 10 turns these into hard errors):

### Known upstream deprecations (Gradle 10)

| Deprecated API | Emitted by | Status |
|----------------|-----------|--------|
| `ReportingExtension.file(String)` | detekt (`io.gitlab.arturbosch.detekt`) `DetektPlugin.apply` | **1.23.8 is the latest release** and still calls it; no fix available. detekt 1.23.8 also caps the supported Kotlin version, so it constrains any Kotlin/Quarkus bump. |
| `Project.getProperties` (will *error* in Gradle 10) | Quarkus Gradle plugin (`io.quarkus`) `ApplicationDeploymentClasspathBuilder` | The latest Quarkus (**3.37.1**, tested) still calls it; bumping from 3.35.1 gives no benefit, so we stay on the validated 3.35.1. |
| `Configuration.getTaskDependencyFromProjectDependency` | Kotlin Multiplatform plugin (`addDependsOnTaskInOtherProjects`) | From the Kotlin Gradle plugin (2.3.21); no compatible fixed release (the latest stable is beta-adjacent and would break detekt 1.23.8). |

**When to revisit:** once detekt ships a Gradle-10-compatible release (that also
supports a newer Kotlin) and Quarkus/KMP clear their calls, bump those plugins
(Kotlin + Quarkus BOM in lockstep — see the ICE note in the Gradle-build memory),
run `./gradlew build -x test --warning-mode fail` to confirm zero deprecations,
then advance the wrapper to Gradle 10.

## Static code analysis

`./gradlew staticAnalysis` is the single goal that runs **every** static-analysis
tool across all modules in one invocation: detekt over the Kotlin modules
(`:domain`, `:backend:implementation`) plus svelte-check and ESLint over the
frontend (via the `lint:report` script). It runs no tests and needs no Docker.

It also produces a **consolidated, project-wide report** at
`build/reports/static-analysis/static-analysis.html` (plus a merged
`static-analysis.sarif`) covering detekt and ESLint together. This is generated
by a dependency-free Node script (`scripts/sarif-to-html.mjs`) run through the
gradle-node plugin's managed Node — so it needs no extra install (no Python, no
Docker). `svelte-check` has no machine-readable report and stays a console-only
gate, so it is not part of the consolidated report.

As per-layer alternatives, `./gradlew detekt` (Kotlin only) and
`./gradlew :frontend:check` (frontend svelte-check + ESLint) cover one layer
each; all of these also run as part of `./gradlew build`. The reports are
currently **informational** — the build stays green regardless of how many
findings each tool produces. A follow-up task can flip the gates to enforcing
once the existing backlog has been triaged.

### Tools and report locations

| Layer              | Tool   | Task                  | Reports                                                       |
|--------------------|--------|-----------------------|---------------------------------------------------------------|
| Kotlin — backend   | detekt | `:backend:implementation:detekt` | `src/backend/implementation/build/reports/detekt/detekt.{xml,html}` |
| Kotlin — domain    | detekt | `:domain:detekt`      | `src/domain/build/reports/detekt/detekt.{xml,html}`           |
| TS / Svelte / HTML | ESLint | `:frontend:npmLintReport` | `src/frontend/reports/eslint.{json,html}`                 |
| All (consolidated) | Node   | `:frontend:sarifHtmlReport` | `build/reports/static-analysis/static-analysis.{html,sarif}` |

Both detekt scopes share `config/detekt/detekt.yml` (top-level config
with `buildUponDefaultConfig: true`). The frontend's ESLint flat config
lives at `src/frontend/eslint.config.js` and composes
`typescript-eslint` (non-type-checked), `eslint-plugin-svelte`
(parses `<script lang="ts">` blocks via `tseslint.parser`), and
`@html-eslint`'s flat-recommended set for plain `*.html`.

### Running a single tool in isolation

Fastest iteration loops during a focused fix:

```bash
# Backend Kotlin only
./gradlew :backend:implementation:detekt

# Domain Kotlin only
./gradlew :domain:detekt

# Frontend (TS + Svelte + HTML)
cd src/frontend && npm run lint          # console output, exits non-zero on findings
cd src/frontend && npm run lint:report   # writes reports/eslint.{json,html}, always exits 0
```

### Skipping static analysis on quick runs

The static-analysis tasks add a few seconds per layer. For most workflows this
is fine; if you need a faster loop (e.g. you're iterating on a single test and
don't care about lint output), the per-tool tasks above let you run just what
you need instead of the full `./gradlew build`.

Several task validation blocks gate their heaviest checks behind the
environment variable `staticCodeAnalysis`. Set it to `1` before
running a validation pass when you want detekt/ESLint included; leave
it unset (the default) when you just want the cheap structural greps:

```bash
# Cheap validation only (file-exists, grep — milliseconds)
./scripts/task.sh ...

# Include the heavy static-analysis runs
export staticCodeAnalysis=1
./scripts/task.sh ...
```

This convention was introduced in `task-056` and applies to the
validation blocks of any task whose work spans the lint pipeline.

## Development Profiles

### Authentication per profile

Each profile runs one auth module, selected at **build time** by the
`app.auth.provider` property (Quarkus `@IfBuildProperty`) — so the value must
be set when the image is built, not just at runtime:

| Profile          | Auth module        | `app.auth.provider` |
|------------------|--------------------|---------------------|
| `dev` (local)    | dev (static users) | `dev`               |
| `test`           | dev (static users) | `dev`               |
| `dev-minikube`   | Entra (OIDC)       | `entra`             |
| `prod`           | Entra (OIDC)       | `entra`             |

`scripts/minikube-deploy.sh` selects Entra by exporting
`APP_AUTH_PROVIDER=entra` / `VITE_AUTH_PROVIDER=entra` before the Gradle image
build (`./gradlew :backend:implementation:imageBuild`). The mapping is
regression-guarded by `AuthProviderProfileTest`.

### dev — Local with PostgreSQL via Dev Services

Backend and frontend run locally on your machine. The backend uses PostgreSQL
started automatically as a throwaway container by Quarkus Dev Services, so
**Docker must be running**.

Quick start (runs both backend and frontend):
```bash
./scripts/dev.sh
```

Or start them individually:

Start the backend:
```bash
QUARKUS_PROFILE=dev ./gradlew :backend:implementation:quarkusDev
```

Start the frontend (in a second terminal):
```bash
cd src/frontend
npm run dev
```

The backend is available at http://localhost:8080, the frontend at http://localhost:5173. The frontend proxies API requests to the locally running backend. No Docker or container image is needed.

The authentication is done using a static authentication provider described in [authentication.md](./authentication.md).



### dev-minikube — Full stack on Minikube

Backend, frontend, and PostgreSQL run as pods on your local Minikube cluster.

Authentication uses the **Entra** module here (production-like), not the dev
static provider. Export `ENTRA_TENANT_ID`, `ENTRA_API_CLIENT_ID`, and
`ENTRA_SPA_CLIENT_ID` before `./scripts/minikube-deploy.sh` — it fails fast if
any is missing. See [entra-setup.md](./entra-setup.md) for how to obtain the
values and [authentication.md](./authentication.md) for module behaviour.

Quick start (setup, build, and deploy):
```bash
./scripts/minikube-setup.sh
./scripts/minikube-deploy.sh
```

Or step by step:

1. Set up Minikube with addons:
   ```bash
   ./scripts/minikube-setup.sh
   ```

2. Build and deploy everything:
   ```bash
   ./scripts/minikube-deploy.sh
   ```

3. Access the backend:
   ```bash
   kubectl -n estimation port-forward svc/backend 8080:8080
   ```
   Then open http://localhost:8080/q/health or http://localhost:8080/q/swagger-ui

4. Reset (stop and delete Minikube):
   ```bash
   ./scripts/minikube-reset.sh
   ```

### Viewing backend logs on Minikube

Minikube runs the dedicated **`dev-minikube`** profile — the overlay sets
`QUARKUS_PROFILE: dev-minikube` in the `backend-config` ConfigMap
(`k8s/overlays/minikube/backend-oidc.yaml`). That profile gives **plain-text**
console logs (JSON is only enabled under `%prod`) and **DEBUG** for the
application package. Fetch logs from the `estimation` namespace and the
`backend` deployment:

```bash
kubectl -n estimation logs -f deploy/backend            # follow
kubectl -n estimation logs deploy/backend --previous     # after a crash-loop
```

The DEBUG level lives in `application.properties`, not in a k8s env var, and it
takes **two** properties:

```properties
%dev-minikube.quarkus.log.category."io.pythia".level=DEBUG
quarkus.log.category."io.pythia".min-level=DEBUG   # unprofiled — see below
```

**Why `min-level` too?** `.level` is a runtime property, but `min-level` is the
build-time floor below which a category can never log, and it is fixed under the
profile active when the image is **packaged** — which is `prod`, not
`dev-minikube`. Without an **unprofiled** `min-level=DEBUG`, the runtime
`.level=DEBUG` gets clamped back up to the `prod`-baked INFO floor and DEBUG
lines never appear (this is why `%dev` works — dev mode augments and runs under
the same profile — but the packaged `dev-minikube` image did not). Keeping
`min-level` unprofiled lowers the floor at build time; `prod` still logs at INFO
because its `.level` stays INFO.

**Why not an env var?** `quarkus.log.category."io.pythia".level` has
dots inside the quoted category name, and an environment variable turns every
dot into `_` — Quarkus then can't tell `io.pythia` from
`io_pythia`, so `QUARKUS_LOG_CATEGORY__IO_PYTHIA__LEVEL` is
silently ignored. (Simple keys like `QUARKUS_LOG_CONSOLE_JSON_ENABLED` map fine.)
Selecting the profile via the dot-free `QUARKUS_PROFILE` env var and keeping the
dotted category level in `application.properties` sidesteps the limitation.

Since the level is baked into the image, changing it means a rebuild + redeploy
(`./scripts/minikube-deploy.sh`). A running pod also won't pick up a changed
ConfigMap until it restarts:

```bash
kubectl -n estimation rollout restart deploy/backend
kubectl -n estimation exec deploy/backend -- printenv | grep QUARKUS_PROFILE   # verify dev-minikube
```
## Generated assets

Two sets of committed files are generated by scripts rather than edited by hand. Both
drive Playwright's bundled Chromium so the repository needs no image dependency, and both
outputs are committed so a plain `npm run build` never needs a browser.

| Command | Generates | Re-run when |
|---|---|---|
| `npm run gen:icons` | the app icons and `site.webmanifest` in `src/frontend/static/` | `src/lib/assets/logo.svg` changes |
| `npm run gen:screenshots` | the README screenshots in `docs/images/` | the screens they show change materially |

`gen:screenshots` (`src/frontend/scripts/capture-screenshots.mjs`) needs a **running dev
stack** — start `./scripts/dev.sh` first, or the script exits non-zero with a message
rather than writing blank images. It captures against the seeded demo data, switches the
capture user's UI language to English first (the README is English, the seed content is
German), and seeds one collaborative session through the REST API so the session shot shows
a revealed round rather than an empty room.
