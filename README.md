# TheEstimator

Project effort estimation tool for software development tasks.

## Overview

TheEstimator helps teams create, manage, and track effort estimations for software projects. It supports three-point estimation (optimistic, likely, pessimistic), versioned estimation snapshots, and audit trails.

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

## Getting Started

### Prerequisites

- Java 21 (managed via jenv)
- Maven 3.9+
- Docker
- Minikube
- kubectl

### Development Profiles

The project provides two development stages:

#### dev-local — Local with H2 in-memory database

Backend and frontend run locally on your machine. The backend uses an H2 in-memory database (no Docker or PostgreSQL needed).

Quick start (runs both backend and frontend):
```bash
./scripts/dev-local.sh
```

Or start them individually:

Start the backend:
```bash
cd src/backend/implementation
mvn quarkus:dev -Pdev-local
```

Start the frontend (in a second terminal):
```bash
cd src/frontend
npm run dev
```

The backend is available at http://localhost:8080, the frontend at http://localhost:5173. The frontend proxies API requests to the locally running backend. No Docker or container image is needed.

#### dev-minikube — Full stack on Minikube

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
