#!/bin/bash

# Refuse to be sourced. This script exports the OIDC_* manifest coordinates
# derived from your ENTRA_* ids and turns on `set -e`/`pipefail`. Those are
# meant for THIS process and its children only — when the script is executed
# (./scripts/minikube-deploy.sh) they stay in the subshell and never touch your
# interactive shell. If you `source` it instead, the exports and the shell
# options leak into your shell, where `set -e` closes it on the next failing
# command. The auth provider is no longer set here at all: a default
# production build always contains the Entra module (task-194). Detect
# sourcing under both bash and zsh and bail BEFORE `set -e`/`export`, so a
# sourced call leaks nothing at all.
_sourced=0
if [ -n "${ZSH_EVAL_CONTEXT:-}" ]; then
    case "$ZSH_EVAL_CONTEXT" in *:file*) _sourced=1 ;; esac
elif [ -n "${BASH_SOURCE:-}" ]; then
    [ "${BASH_SOURCE[0]}" != "${0}" ] && _sourced=1
fi
if [ "$_sourced" = 1 ]; then
    echo "Error: do not 'source' this script — run it directly:" >&2
    echo "  ./scripts/minikube-deploy.sh" >&2
    echo "Sourcing leaks the OIDC_* exports and 'set -e' into your interactive shell." >&2
    # SC2317: `return` succeeds when sourced (short-circuiting the `||`); the
    # `exit` only runs in the executed case. Dual-mode idiom, not dead code.
    # shellcheck disable=SC2317
    return 1 2>/dev/null || exit 1
fi
unset _sourced

set -e
set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
K8S_OVERLAY="$PROJECT_ROOT/k8s/overlays/minikube"

# Shared settings for the local cluster (task-196): namespace, registry ports,
# image tag. Plain variables, not exported — see the file.
# minikube-env.sh is linted on its own; plain `shellcheck` (no -x) does not follow it.
# shellcheck source=scripts/minikube-env.sh disable=SC1091
. "$SCRIPT_DIR/minikube-env.sh"

# Fail fast on missing Entra ids. The minikube stack runs the real Entra login,
# and these ids become the OIDC_* values that envsubst writes into the
# backend-config and frontend-config ConfigMaps. Nothing else would catch an
# empty one: the derived OIDC_* variables are always set, so envsubst quietly
# renders an issuer URL like https://login.microsoftonline.com//v2.0 and empty
# client ids, and the failure only surfaces after build, push and rollout, as a
# backend OIDC error or a broken browser login that names no variable.
for v in ENTRA_TENANT_ID ENTRA_API_CLIENT_ID ENTRA_SPA_CLIENT_ID; do
    if [ -z "${!v}" ]; then
        echo "Error: $v must be set in the environment before deploying." >&2
        echo "See docs/entra-setup.md for how to obtain these values." >&2
        exit 1
    fi
done

# The frontend no longer takes its auth config at BUILD time (task-162): the
# image is stage-independent and the SPA fetches /config.json, rendered from the
# frontend-config ConfigMap. So these ids render a manifest now instead of
# feeding a Docker build arg.

# The k8s manifests use ONE provider-neutral placeholder convention across every
# overlay (task-161): ${OIDC_*} names carrying FULL values, rather than
# identity-provider-specific fragments a manifest has to compose into a URL.
# This script keeps asking the developer for the ENTRA_* ids (that is what
# docs/entra-setup.md and EntraIdVariables.sh hand out) and derives the
# manifest-facing names here.
export OIDC_AUTH_SERVER_URL="https://login.microsoftonline.com/${ENTRA_TENANT_ID}/v2.0"
export OIDC_CLIENT_ID="$ENTRA_API_CLIENT_ID"
# Accept BOTH audience forms: v1.0 access tokens carry aud=api://<client-id>,
# v2.0 tokens carry the bare aud=<client-id>.
export OIDC_TOKEN_AUDIENCE="api://${ENTRA_API_CLIENT_ID},${ENTRA_API_CLIENT_ID}"
# The SPA's own coordinates (task-162). OIDC_CLIENT_ID above is reused as the
# apiClientId — it is already the API client id.
export OIDC_TENANT_ID="$ENTRA_TENANT_ID"
export OIDC_SPA_CLIENT_ID="$ENTRA_SPA_CLIENT_ID"
# Must be a registered redirect URI on the Entra SPA app registration AND match
# how you open the SPA. Defaults to the port-forward URL this script prints
# below; override it for the Ingress host (e.g. http://estimation.local).
export OIDC_REDIRECT_URI="${OIDC_REDIRECT_URI:-http://localhost:8080}"

# The images reach the cluster through its registry addon (task-196), not by
# side-loading: a registry can hold a multi-arch index, and the node pulls its
# native variant (arm64 on Apple Silicon, so nothing runs under emulation).
# Idempotent; minikube-setup.sh enables it too, this covers older clusters.
minikube addons enable registry

# The host reaches the in-cluster registry through a port-forward. The port is
# MINIKUBE_REGISTRY_HOST_PORT (5005 by default, because macOS's AirPlay Receiver
# holds 5000); pods pull the same registry as MINIKUBE_IMAGE_REGISTRY.
echo "Port-forwarding the registry addon to localhost:${MINIKUBE_REGISTRY_HOST_PORT}..."
kubectl -n kube-system port-forward svc/registry "${MINIKUBE_REGISTRY_HOST_PORT}:80" > /dev/null 2>&1 &
REGISTRY_PF_PID=$!
trap 'kill "$REGISTRY_PF_PID" 2>/dev/null || true' EXIT
push_registry="localhost:${MINIKUBE_REGISTRY_HOST_PORT}"
for _ in $(seq 1 30); do
    curl -fsS "http://${push_registry}/v2/" > /dev/null 2>&1 && break
    sleep 1
done
if ! curl -fsS "http://${push_registry}/v2/" > /dev/null 2>&1; then
    echo "Error: the registry addon did not answer on http://${push_registry}/v2/ within 30s." >&2
    echo "Check 'kubectl -n kube-system get pods -l kubernetes.io/minikube-addons=registry'." >&2
    exit 1
fi

echo "Building and pushing backend and frontend images (amd64 + arm64)..."
cd "$PROJECT_ROOT"
# Pushing implies multi-arch for both images. The registry lives INSIDE the
# cluster, so a reset leaves it empty — yet an unchanged backend build would be
# UP-TO-DATE (or replayed FROM-CACHE) and push nothing. quarkusAppPartsBuild is
# where the image is actually built and pushed, so it is forced with --rerun.
# The frontend's imageBuild is never up to date. Tests are skipped here.
"$PROJECT_ROOT/gradlew" \
    :backend:implementation:quarkusAppPartsBuild --rerun \
    :backend:implementation:imageBuild \
    -Dquarkus.container-image.push=true \
    -Dquarkus.container-image.registry="$push_registry" \
    -Dquarkus.container-image.tag="$MINIKUBE_IMAGE_TAG" \
    -Dquarkus.container-image.insecure=true \
    :frontend:imageBuild -PfrontendImagePush=true \
    -PfrontendImageRegistry="$push_registry" -PfrontendImageTag="$MINIKUBE_IMAGE_TAG" \
    -x test

echo "Applying Kustomize overlay..."
# Render the overlay, substitute ONLY the OIDC coordinates (kustomize can't do
# env substitution; these are per-user values kept out of the committed
# manifests), then apply. Restricting envsubst to these names leaves any other
# ${...} in the manifests untouched.
if ! command -v envsubst >/dev/null 2>&1; then
    echo "Error: envsubst is required (install gettext, e.g. 'brew install gettext')." >&2
    exit 1
fi
# shellcheck disable=SC2016  # single quotes are intentional: this literal is
# envsubst's allow-list of variable NAMES, not a shell expansion.
# MINIKUBE_IMAGE_REGISTRY is passed explicitly: minikube-env.sh does not export.
kubectl kustomize "$K8S_OVERLAY" \
    | MINIKUBE_IMAGE_REGISTRY="$MINIKUBE_IMAGE_REGISTRY" envsubst '${OIDC_AUTH_SERVER_URL} ${OIDC_CLIENT_ID} ${OIDC_TOKEN_AUDIENCE} ${OIDC_TENANT_ID} ${OIDC_SPA_CLIENT_ID} ${OIDC_REDIRECT_URI} ${MINIKUBE_IMAGE_REGISTRY}' \
    | kubectl apply -f -

# The image tag is fixed (MINIKUBE_IMAGE_TAG), so a pushed image or a changed
# ConfigMap leaves the pod spec unchanged and nothing rolls on its own. The
# overlay sets imagePullPolicy: Always, so a forced rollout pulls the image just
# pushed and picks up the current config.
echo "Restarting deployments to pick up the new images/config..."
kubectl -n "$MINIKUBE_NAMESPACE" rollout restart deployment/backend deployment/frontend

echo "Waiting for PostgreSQL to be ready..."
kubectl -n "$MINIKUBE_NAMESPACE" wait --for=condition=ready pod -l app=postgres --timeout=120s

# Wait on the ROLLOUT (not just "a pod is ready"): `rollout status` blocks until
# the new ReplicaSet is fully available and the old pods are gone, so we never
# declare success on a still-terminating old pod mid-rollout.
echo "Waiting for the new backend rollout to complete..."
kubectl -n "$MINIKUBE_NAMESPACE" rollout status deployment/backend --timeout=180s
echo "Waiting for the new frontend rollout to complete..."
kubectl -n "$MINIKUBE_NAMESPACE" rollout status deployment/frontend --timeout=180s

# Freshness safety net: confirm each pod runs the image the registry serves for
# the tag right now (i.e. the rollout pulled what was just pushed). Both sides
# are the digest of the multi-arch INDEX: the registry returns it for a HEAD with
# the index Accept header, and the node's runtime records the digest it pulled by
# in the pod's imageID.
verify_image() {
    app="$1"
    # `|| true`: with `set -e -o pipefail`, a grep with no match would otherwise
    # abort the whole deploy on this best-effort verification step.
    pushed="$(curl -fsSI \
        -H 'Accept: application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json' \
        "http://${push_registry}/v2/pythia/pythia-${app}/manifests/${MINIKUBE_IMAGE_TAG}" 2>/dev/null \
        | grep -i '^docker-content-digest:' | grep -oE '[0-9a-f]{64}' | head -1 || true)"
    running="$(kubectl -n "$MINIKUBE_NAMESPACE" get pod -l "app=$app" \
        -o jsonpath='{.items[0].status.containerStatuses[0].imageID}' 2>/dev/null \
        | grep -oE '[0-9a-f]{64}' | head -1 || true)"
    if [ -n "$pushed" ] && [ -n "$running" ] && [ "$pushed" != "$running" ]; then
        echo "WARNING: $app pod is not running the image the registry currently serves" >&2
        echo "  in registry: $pushed" >&2
        echo "  in pod:      $running" >&2
        echo "  The rollout may not have pulled the pushed image; re-run this script." >&2
    else
        echo "  $app pod runs the pushed image (${pushed:-unknown})"
    fi
}
echo "Verifying pods run the freshly pushed images..."
verify_image backend
verify_image frontend

echo "Deployment complete."
kubectl -n "$MINIKUBE_NAMESPACE" get pods
echo ""
echo "Access the backend with:"
echo "  kubectl -n $MINIKUBE_NAMESPACE port-forward svc/backend 8080:8080"
echo "Access the frontend with:"
echo "  kubectl -n $MINIKUBE_NAMESPACE port-forward svc/frontend 8080:80"
echo "Access the backend logs with:"
echo "  kubectl -n $MINIKUBE_NAMESPACE logs -f deploy/backend"

