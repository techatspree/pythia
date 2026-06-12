# Architecture

## Tech Stack

| Layer    | Technology                      |
|----------|---------------------------------|
| Frontend | SvelteKit + TypeScript (Vite)   |
| Styling  | Tailwind CSS 4                  |
| Backend  | Quarkus (Java 21)               |
| Database | PostgreSQL 16 (H2 for dev-local)|
| Platform | Kubernetes (Minikube for local) |
| Auth     | Microsoft Entra ID (MSAL/OIDC)  |

## Repository Structure

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

## Domain module: single source of truth

The `domain` module is a Kotlin Multiplatform project that contains **all
business logic and domain models** shared between frontend and backend.

- **Backend** consumes it as a regular JVM dependency (compiled to a JAR via
  the `domain-jvm` Gradle target).
- **Frontend** consumes it as TypeScript, compiled to JS/TS by the
  Kotlin/JS compiler and published as an npm package during the Maven build.

Domain logic (calculations, validation, data structures) must live in `domain`.
Neither the Quarkus backend nor the SvelteKit frontend should duplicate or
reimplement domain rules — they only call into the shared domain code.

### Why `src/domain/` ships BOTH a `pom.xml` AND a `build.gradle.kts`

The top-level build is **Maven** (`./mvnw install`, `./mvnw verify`, …) so
the reactor drives the whole repo uniformly. The Maven wrapper at the repo
root (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/`) pins the Maven version, so
contributors don't need a system `mvn`. For pure JVM Kotlin (the backend at
`src/backend/implementation/`) the standard `kotlin-maven-plugin` is enough —
there is no Gradle file there.

`src/domain/` is the exception because it's a **Kotlin Multiplatform** module
producing **two** artefacts from one source tree:

1. a JVM jar consumed by the backend, and
2. a TypeScript-typed JS library consumed by the frontend (via the `domain`
   npm dependency).

The toolchain that does the second job — `kotlin("multiplatform")` +
`binaries.library()` + `generateTypeScriptDefinitions()` + the
`prepareTypescriptArtifacts` / `packageTypescript` Gradle tasks — is
**Gradle-only**:

- The official `kotlin-maven-plugin` only compiles JVM Kotlin. It has no
  `js()` target, no `commonMain` source-set graph, and no way to emit `.d.ts`
  declaration files.
- No community Maven plugin wires KMP's JS target end-to-end.

So `src/domain/` runs a "Maven outside, Gradle inside" sandwich:

- `src/domain/pom.xml` is the Maven face of the module. Inside it,
  `exec-maven-plugin` delegates the real work to `./gradlew`: there are
  executions for `gradle-compile-jvm` (in the `compile` phase),
  `gradle-test-jvm` (`test`), `gradle-package-typescript` (`package`), and
  `gradle-detekt` (`verify`).
- `src/domain/build.gradle.kts` is what Gradle reads to do that work.

For everything else in the repo, `pom.xml` is the single build file — Gradle
is contained inside `src/domain/` only.

#### Could we remove one of them?

Three options if the dual build ever feels like too much:

1. **Drop the KMP/TypeScript pipeline** and ship only a JVM jar. The
   frontend would then have to re-implement domain calc in TypeScript —
   which violates the single-source-of-truth rule above. Don't do this.
2. **Hand-roll the JS target** with another tool (e.g. `kotlin2js` via
   exec-maven-plugin, plus a separate Kotlin-to-TypeScript step). Doable
   but fragile.
3. **Switch the whole reactor to Gradle.** Cleanest end-state if you want a
   single build tool — but it means redoing `quarkus-maven-plugin`,
   `frontend-maven-plugin`, Jib, Flyway lifecycle wiring, and the
   `detekt-maven-plugin` setup in Gradle equivalents.

Option (3) is the right migration target if the duplication ever bites
hard. Doing it piecemeal usually makes things worse — keep the current
"Maven outside, Gradle inside `src/domain/`" pattern until you're ready to
commit to a full switch.

## Frontend conventions

### Error handling: no silent try/catch

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