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