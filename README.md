# TheEstimator

Project effort estimation tool for software development tasks.

## Features

- **Three-point estimation** — capture optimistic, likely, and pessimistic effort values per task
- **Tree-structured estimations** — group items into arbitrarily deep subtrees; drag rows with mouse or keyboard (Tab to a row, Space to pick up, arrow keys to move, Space to drop) to restructure
- **Versioned snapshots** — create and compare estimation versions across the lifetime of a project
- **Audit trail** — track who changed what and when
- **Phase grouping** — organize tasks into project phases for structured planning
- **Time-dependent estimates** — model effort that scales with elapsed time or project stage

## Documentation

- [Architecture](docs/architecture.md) — tech stack and repository structure
- [Development](docs/development.md) — prerequisites, build commands, and development profiles

## Code quality / Static analysis

The `./mvnw verify` reactor build also runs static analysis across every
source layer. Reports are currently **informational** (the build stays
green — exit code 0 — regardless of findings). A follow-up task can
flip these to enforcing once the existing backlog has been triaged.

| Layer       | Tool   | Reports                                                       | Run in isolation                                                      |
|-------------|--------|---------------------------------------------------------------|-----------------------------------------------------------------------|
| Kotlin (backend)  | detekt | `src/backend/implementation/target/detekt/detekt.{xml,html}`     | `./mvnw -pl src/backend/implementation detekt:check`                  |
| Kotlin (domain)   | detekt | `src/domain/build/reports/detekt/detekt.{xml,html}`              | `cd src/domain && ./gradlew detekt`                                   |
| TS / Svelte / HTML | ESLint | `src/frontend/reports/eslint.{json,html}`                       | `cd src/frontend && npm run lint:report`                              |

Configuration lives in `config/detekt/detekt.yml` (shared by both
Kotlin scopes) and `src/frontend/eslint.config.js` (flat config
combining typescript-eslint, eslint-plugin-svelte, and
@html-eslint).