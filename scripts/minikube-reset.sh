#!/bin/bash
set -e

echo "Deleting namespace estimation (all resources)..."
kubectl delete namespace estimation --ignore-not-found

echo "Stopping Minikube..."
minikube stop

echo "Deleting Minikube cluster..."
minikube delete

echo "Minikube reset complete."
