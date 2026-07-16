#!/bin/bash
set -e
set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
K8S_OVERLAY="$PROJECT_ROOT/k8s/overlays/minikube"

# Modular auth selector — minikube uses Entra in production-like mode.
export APP_AUTH_PROVIDER=entra
export VITE_AUTH_PROVIDER=entra

# Fail fast on missing tenant config so we never silently ship the
# unresolved ${VAR} placeholders into the running backend Pod.
for v in ENTRA_TENANT_ID ENTRA_API_CLIENT_ID ENTRA_SPA_CLIENT_ID; do
    if [ -z "${!v}" ]; then
        echo "Error: $v must be set in the environment before deploying." >&2
        echo "See docs/entra-setup.md for how to obtain these values." >&2
        exit 1
    fi
done

# The frontend picks its auth module (and Entra/MSAL config) from VITE_* at
# BUILD time — Vite inlines them into the SPA bundle. Derive them from the
# ENTRA_* ids above and forward them into the frontend Docker build (the
# :frontend:dockerBuildImage task passes these as --build-args). Without this,
# the SPA would default to the dev auth module while the backend runs Entra,
# so logins would send a "Dev <user>" header the Entra backend rejects.
export VITE_ENTRA_TENANT_ID="$ENTRA_TENANT_ID"
export VITE_ENTRA_SPA_CLIENT_ID="$ENTRA_SPA_CLIENT_ID"
export VITE_ENTRA_API_CLIENT_ID="$ENTRA_API_CLIENT_ID"
# The SPA redirect URI must be a registered redirect URI on the Entra SPA app
# registration AND match how you open the SPA. Defaults to the port-forward URL
# this script prints below; override VITE_ENTRA_REDIRECT_URI if you use the
# Ingress host (e.g. http://estimation.local) or a different port.
export VITE_ENTRA_REDIRECT_URI="${VITE_ENTRA_REDIRECT_URI:-http://localhost:8080}"

echo "Building backend and frontend container images..."
cd "$PROJECT_ROOT"
# Backend image via Quarkus/Jib (container-image build enabled); frontend image
# via the Gradle Docker task. Tests are skipped for the deploy build.
"$PROJECT_ROOT/gradlew" \
    :backend:implementation:imageBuild -Dquarkus.container-image.build=true \
    :frontend:dockerBuildImage \
    -x test

echo "Loading images into Minikube..."
# NOTE: do NOT use `minikube image load` here. With the docker driver it caches
# the exported tarball under ~/.minikube/cache/images/ and, for an image whose
# tag already exists (ours is the fixed 1.0.0-SNAPSHOT), reuses the STALE cached
# copy — so a freshly rebuilt image silently never reaches the cluster and pods
# keep running yesterday's bundle. Piping `docker save` straight into the
# in-cluster docker daemon bypasses that cache and always installs the new
# content, retagging 1.0.0-SNAPSHOT to the fresh image id.
for img in theestimator/estimation-backend:1.0.0-SNAPSHOT theestimator/estimation-frontend:1.0.0-SNAPSHOT; do
    echo "  -> $img"
    docker save "$img" | minikube ssh --native-ssh=false -- docker load
done

echo "Applying Kustomize overlay..."
# Render the overlay, substitute ONLY the Entra ids (kustomize can't do env
# substitution; these are per-user secrets kept out of the committed manifests),
# then apply. Restricting envsubst to these two names leaves any other ${...}
# in the manifests untouched.
if ! command -v envsubst >/dev/null 2>&1; then
    echo "Error: envsubst is required (install gettext, e.g. 'brew install gettext')." >&2
    exit 1
fi
kubectl kustomize "$K8S_OVERLAY" \
    | envsubst '${ENTRA_TENANT_ID} ${ENTRA_API_CLIENT_ID}' \
    | kubectl apply -f -

# The image tag is fixed (1.0.0-SNAPSHOT) with imagePullPolicy: Never, so a
# reloaded image or a changed ConfigMap is NOT picked up by pods whose spec is
# otherwise unchanged. Force a rollout so the freshly built images and current
# config actually take effect.
echo "Restarting deployments to pick up the new images/config..."
kubectl -n estimation rollout restart deployment/backend deployment/frontend

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
echo "Access the backend logs with:"
echo "  kubectl -n estimation logs deploy/backend"

