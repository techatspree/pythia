# Development

## Prerequisites

- Java 21 (managed via jenv)
- Maven 3.9+
- Node.js (managed automatically by Maven for builds)
- Docker
- Minikube
- kubectl

## Building

The project uses a top-level Maven reactor build that covers both frontend and backend.

```bash
# Full build (frontend + backend, all tests)
mvn clean install

# Build with dev-local profile (H2, no container image)
mvn clean install -Pdev-local

# Build with dev-minikube profile (PostgreSQL, builds container image)
mvn clean install -Pdev-minikube

# Run all tests (backend unit tests + frontend type-check)
mvn test

# Skip frontend build (backend only)
mvn clean install -Dskip.frontend=true

# Run end-to-end tests (requires running backend)
mvn verify -pl src/backend/end2end -DskipITs=false
```

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
mvn quarkus:dev -Pdev-local
```

Start the frontend (in a second terminal):
```bash
cd src/frontend
npm run dev
```

The backend is available at http://localhost:8080, the frontend at http://localhost:5173. The frontend proxies API requests to the locally running backend. No Docker or container image is needed.

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