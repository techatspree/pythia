#!/bin/bash
set -e

# Shared settings for the local cluster (task-196); override any of them per run,
# e.g. MINIKUBE_MEMORY=12288 ./scripts/minikube-setup.sh
# minikube-env.sh is linted on its own; plain `shellcheck` (no -x) does not follow it.
# shellcheck source=scripts/minikube-env.sh disable=SC1091
. "$(dirname "$0")/minikube-env.sh"

echo "Starting Minikube..."
minikube start --cpus="$MINIKUBE_CPUS" --memory="$MINIKUBE_MEMORY" --driver="$MINIKUBE_DRIVER"

echo "Enabling addons..."
minikube addons enable ingress
minikube addons enable metrics-server
# The images are pushed into this in-cluster registry by minikube-deploy.sh
# (task-196). Pods pull from the node's localhost:5000, which the addon's
# registry-proxy serves; the host pushes through a port-forward on 5005,
# because macOS's AirPlay Receiver holds 5000 there.
minikube addons enable registry

#echo "Creating namespace..."
#kubectl create namespace estimation-tool --dry-run=client -o yaml | kubectl apply -f -

echo "Minikube is ready."
echo "  kubectl get nodes"
kubectl get nodes
