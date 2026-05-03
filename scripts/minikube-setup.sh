#!/bin/bash
set -e

echo "Starting Minikube..."
minikube start --cpus=4 --memory=8192 --driver=docker

echo "Enabling addons..."
minikube addons enable ingress
minikube addons enable metrics-server

#echo "Creating namespace..."
#kubectl create namespace estimation-tool --dry-run=client -o yaml | kubectl apply -f -

echo "Minikube is ready."
echo "  kubectl get nodes"
kubectl get nodes
