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
  k8s/        — Kubernetes manifests (Kustomize)
docs/         — Architecture and design documents
scripts/      — Helper scripts for local development
planning/     — Project plan and task definitions
```

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