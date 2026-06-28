# Development

## Prerequisites

- Java 21 (managed via jenv)
- Node.js (managed automatically by Maven for builds)
- Docker
- Minikube
- kubectl

Maven itself is **not** a prerequisite — the repo ships a Maven wrapper
(`./mvnw` and `./mvnw.cmd` at the repo root, with the version pinned by
`.mvn/wrapper/`). On first run `./mvnw` downloads the right Maven version
into your local Maven cache; subsequent runs reuse it. If you already
have a matching `mvn` on `PATH` it works too — but examples below use
`./mvnw` so contributors get a consistent version regardless of their
local setup.

## Building

The project uses a top-level Maven reactor build that covers both frontend and backend.

```bash
# Full build (frontend + backend, all tests)
./mvnw clean install

# Build with dev-local profile (H2, no container image)
./mvnw clean install -Pdev-local

# Build with dev-minikube profile (PostgreSQL, builds container image)
./mvnw clean install -Pdev-minikube

# Run all tests (backend unit tests + frontend type-check)
./mvnw test

# Skip frontend build (backend only)
./mvnw clean install -Dskip.frontend=true

# Run end-to-end tests (requires running backend)
./mvnw verify -pl src/backend/end2end -DskipITs=false
```

## Static code analysis

`./mvnw verify` runs static analysis across every source layer in
addition to the regular tests. The reports are currently
**informational** — the build stays green (exit code 0) regardless of
how many findings each tool produces. A follow-up task can flip the
gates to enforcing once the existing backlog has been triaged.

### Tools and report locations

| Layer              | Tool   | Phase    | Reports                                                       |
|--------------------|--------|----------|---------------------------------------------------------------|
| Kotlin — backend   | detekt | `verify` | `src/backend/implementation/target/detekt/detekt.{xml,html}`  |
| Kotlin — domain    | detekt | `verify` | `src/domain/build/reports/detekt/detekt.{xml,html}`           |
| TS / Svelte / HTML | ESLint | `verify` | `src/frontend/reports/eslint.{json,html}`                     |

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
./mvnw -pl src/backend/implementation detekt:check

# Domain Kotlin only (KMP via Gradle)
cd src/domain && ./gradlew detekt

# Frontend (TS + Svelte + HTML)
cd src/frontend && npm run lint          # console output, exits non-zero on findings
cd src/frontend && npm run lint:report   # writes reports/eslint.{json,html}, always exits 0
```

### Skipping static analysis on quick runs

The static-analysis suite adds a few seconds per layer to `./mvnw
verify`. For most workflows this is fine; if you need a faster build
(e.g. you're iterating on a single test and don't care about lint
output), the per-tool commands above let you skip the full reactor.

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

### dev-local — Local with H2 in-memory database

Backend and frontend run locally on your machine. The backend uses an H2 in-memory database (no Docker or PostgreSQL needed).

Quick start (runs both backend and frontend):
```bash
./scripts/dev-local.sh
```

Or start them individually:

Start the backend:
```bash
cd src/backend/implementation
../../../mvnw quarkus:dev -Pdev-local
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