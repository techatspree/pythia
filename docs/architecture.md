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