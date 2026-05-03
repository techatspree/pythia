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

See `docs/` for setup instructions (coming soon).
