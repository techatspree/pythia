#!/bin/bash
set -e

# Shared settings for the local cluster (task-196).
# minikube-env.sh is linted on its own; plain `shellcheck` (no -x) does not follow it.
# shellcheck source=scripts/minikube-env.sh disable=SC1091
. "$(dirname "$0")/minikube-env.sh"

# minikube-deploy.sh port-forwards the registry addon and its EXIT trap ends
# that port-forward; this covers a deploy that was killed before the trap ran.
# Matched on the exact command line, so no other process is touched.
pkill -f "port-forward svc/registry ${MINIKUBE_REGISTRY_HOST_PORT}:80" || true

echo "Deleting namespace $MINIKUBE_NAMESPACE (all resources)..."
kubectl delete namespace "$MINIKUBE_NAMESPACE" --ignore-not-found

echo "Stopping Minikube..."
minikube stop

echo "Deleting Minikube cluster (this also deletes the registry addon and every image pushed to it)..."
minikube delete

echo "Minikube reset complete."
