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

```bash
cd src/backend/implementation
mvn quarkus:dev -Pdev-local
```

The backend is available at http://localhost:8080. No container image is built.

#### dev-minikube — Full stack on Minikube

Backend, frontend, and PostgreSQL run as pods on your local Minikube cluster.

1. Build the container image:
   ```bash
   cd src/backend/implementation
   mvn package -Pdev-minikube
   ```

2. Start minikube (if not running):
   ```bash
   minikube start
   ```

3. Load the image into minikube:
   ```bash
   minikube image load theestimator/estimation-backend:1.0.0-SNAPSHOT
   ```

4. Deploy:
   ```bash
   kubectl apply -k src/backend/k8s/base/
   ```

5. Check status:
   ```bash
   kubectl -n estimation get pods
   ```

6. Access the backend:
   ```bash
   kubectl -n estimation port-forward svc/backend 8080:8080
   ```
   Then open http://localhost:8080/q/health or http://localhost:8080/q/swagger-ui
