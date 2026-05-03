# TheEstimator

Project effort estimation tool for software development tasks.

## Overview

TheEstimator helps teams create, manage, and track effort estimations for software projects. It supports three-point estimation (optimistic, likely, pessimistic), versioned estimation snapshots, and audit trails.

## Tech Stack

| Layer    | Technology                     |
|----------|--------------------------------|
| Frontend | React + TypeScript (Vite)      |
| Backend  | Quarkus (Java 21)              |
| Database | PostgreSQL 16                  |
| Platform | Kubernetes (Minikube for local)|
| Auth     | Microsoft Entra ID (OIDC)      |

## Repository Structure

```
src/
  backend/    — Quarkus REST API
  frontend/   — React SPA
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

### Build the Backend

```bash
cd src/backend/implementation
mvn package
```

This compiles the application and builds a Docker image `theestimator/estimation-backend:1.0.0-SNAPSHOT` into your local Docker daemon.

### Deploy to Minikube

1. Start minikube (if not running):
   ```bash
   minikube start
   ```

2. Load the locally-built Docker image into minikube:
   ```bash
   minikube image load theestimator/estimation-backend:1.0.0-SNAPSHOT
   ```

3. Deploy:
   ```bash
   kubectl apply -k src/backend/k8s/base/
   ```

4. Check status:
   ```bash
   kubectl -n estimation get pods
   ```

5. Access the backend:
   ```bash
   kubectl -n estimation port-forward svc/backend 8080:8080
   ```
   Then open http://localhost:8080/q/health or http://localhost:8080/q/swagger-ui
