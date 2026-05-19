#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
K8S_OVERLAY="$PROJECT_ROOT/k8s/overlays/minikube"

echo "Building backend and frontend container images..."
cd "$PROJECT_ROOT"
mvn package -Pdev-minikube -DskipTests

echo "Loading images into Minikube..."
minikube image load theestimator/estimation-backend:1.0.0-SNAPSHOT
minikube image load theestimator/estimation-frontend:1.0.0-SNAPSHOT

echo "Applying Kustomize overlay..."
kubectl apply -k "$K8S_OVERLAY"

echo "Waiting for PostgreSQL to be ready..."
kubectl -n estimation wait --for=condition=ready pod -l app=postgres --timeout=120s

echo "Waiting for backend to be ready..."
kubectl -n estimation wait --for=condition=ready pod -l app=backend --timeout=120s

echo "Waiting for frontend to be ready..."
kubectl -n estimation wait --for=condition=ready pod -l app=frontend --timeout=120s

echo "Deployment complete."
kubectl -n estimation get pods
echo ""
echo "Access the backend with:"
echo "  kubectl -n estimation port-forward svc/backend 8080:8080"
echo "Access the frontend with:"
echo "  kubectl -n estimation port-forward svc/frontend 8080:80"
