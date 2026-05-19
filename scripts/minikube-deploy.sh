#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
K8S_DIR="$PROJECT_ROOT/src/backend/k8s"

echo "Building backend and frontend container images..."
cd "$PROJECT_ROOT"
mvn package -Pdev-minikube -DskipTests

echo "Loading images into Minikube..."
minikube image load theestimator/estimation-backend:1.0.0-SNAPSHOT
minikube image load theestimator/estimation-frontend:1.0.0-SNAPSHOT

echo "Applying namespace..."
kubectl apply -f "$PROJECT_ROOT/src/k8s/namespace.yaml"

echo "Deploying PostgreSQL..."
kubectl apply -f "$K8S_DIR/postgres/"

echo "Waiting for PostgreSQL to be ready..."
kubectl -n estimation wait --for=condition=ready pod -l app=postgres --timeout=120s

echo "Deploying backend..."
kubectl apply -f "$K8S_DIR/backend.yaml"

echo "Waiting for backend to be ready..."
kubectl -n estimation wait --for=condition=ready pod -l app=backend --timeout=120s

echo "Deployment complete."
kubectl -n estimation get pods
echo ""
echo "Access the backend with:"
echo "  kubectl -n estimation port-forward svc/backend 8080:8080"
echo ""
echo "Frontend image theestimator/estimation-frontend:1.0.0-SNAPSHOT is loaded"
echo "into Minikube but not deployed (no frontend Kubernetes manifest yet)."
