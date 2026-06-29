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

## Static code analysis

`./gradlew detekt` (Kotlin) and `./gradlew :frontend:check` (ESLint, via the
`lint:report` script) run static analysis across every source layer; both also
run as part of `./gradlew build`. The reports are currently **informational**
— the build stays green regardless of how many findings each tool produces. A
follow-up task can flip the gates to enforcing once the existing backlog has
been triaged.

### Tools and report locations

| Layer              | Tool   | Task                  | Reports                                                       |
|--------------------|--------|-----------------------|---------------------------------------------------------------|
| Kotlin — backend   | detekt | `:backend:implementation:detekt` | `src/backend/implementation/build/reports/detekt/detekt.{xml,html}` |
| Kotlin — domain    | detekt | `:domain:detekt`      | `src/domain/build/reports/detekt/detekt.{xml,html}`           |
| TS / Svelte / HTML | ESLint | `:frontend:npmLintReport` | `src/frontend/reports/eslint.{json,html}`                 |

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