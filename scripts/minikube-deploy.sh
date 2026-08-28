#!/bin/bash

# Refuse to be sourced. This script exports APP_AUTH_PROVIDER=entra (plus the
# ENTRA_*/VITE_* build vars) so its child gradle/docker builds pick the Entra
# auth module. Those exports are meant for THIS process and its children only —
# when the script is executed (./scripts/minikube-deploy.sh) they stay in the
# subshell and never touch your interactive shell. If you `source` it instead,
# APP_AUTH_PROVIDER=entra leaks into your shell, and a later `./gradlew build`
# in the same shell then augments the OIDC-less test profile with the Entra
# provider — EntraAuthFilter can't resolve its JsonWebToken and the build fails
# with a cryptic ArC UnsatisfiedResolutionException. Detect sourcing under both
# bash and zsh and bail BEFORE `set -e`/`export`, so a sourced call leaks
# nothing at all (not the auth var, not the shell options).
_sourced=0
if [ -n "${ZSH_EVAL_CONTEXT:-}" ]; then
    case "$ZSH_EVAL_CONTEXT" in *:file*) _sourced=1 ;; esac
elif [ -n "${BASH_SOURCE:-}" ]; then
    [ "${BASH_SOURCE[0]}" != "${0}" ] && _sourced=1
fi
if [ "$_sourced" = 1 ]; then
    echo "Error: do not 'source' this script — run it directly:" >&2
    echo "  ./scripts/minikube-deploy.sh" >&2
    echo "Sourcing leaks APP_AUTH_PROVIDER=entra into your shell and then breaks './gradlew build'." >&2
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

# Modular auth selector — minikube uses Entra in production-like mode.
export APP_AUTH_PROVIDER=entra

# Fail fast on missing tenant config so we never silently ship the
# unresolved ${VAR} placeholders into the running backend Pod.
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
for img in pythia/pythia-backend:1.0.0-SNAPSHOT pythia/pythia-frontend:1.0.0-SNAPSHOT; do
    echo "  -> $img"
    docker save "$img" | minikube ssh --native-ssh=false -- docker load
done

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
kubectl kustomize "$K8S_OVERLAY" \
    | envsubst '${OIDC_AUTH_SERVER_URL} ${OIDC_CLIENT_ID} ${OIDC_TOKEN_AUDIENCE} ${OIDC_TENANT_ID} ${OIDC_SPA_CLIENT_ID} ${OIDC_REDIRECT_URI}' \
    | kubectl apply -f -

# The image tag is fixed (1.0.0-SNAPSHOT) with imagePullPolicy: Never, so a
# reloaded image or a changed ConfigMap is NOT picked up by pods whose spec is
# otherwise unchanged. Force a rollout so the freshly built images and current
# config actually take effect.
echo "Restarting deployments to pick up the new images/config..."
kubectl -n estimation rollout restart deployment/backend deployment/frontend

echo "Waiting for PostgreSQL to be ready..."
kubectl -n estimation wait --for=condition=ready pod -l app=postgres --timeout=120s

# Wait on the ROLLOUT (not just "a pod is ready"): `rollout status` blocks until
# the new ReplicaSet is fully available and the old pods are gone, so we never
# declare success on a still-terminating old pod mid-rollout.
echo "Waiting for the new backend rollout to complete..."
kubectl -n estimation rollout status deployment/backend --timeout=180s
echo "Waiting for the new frontend rollout to complete..."
kubectl -n estimation rollout status deployment/frontend --timeout=180s

# Freshness safety net: confirm the pod is running the image currently tagged
# INSIDE minikube (i.e. the rollout picked up the reload). Both digests below are
# the classic docker config digest, so they are directly comparable.
#
# Do NOT compare against the host `docker inspect .Id`: when Docker Desktop uses
# the containerd image store, `.Id` there is the MANIFEST digest, which never
# equals minikube's classic config digest — the same image reads as two
# different hashes and the check false-positives every time.
verify_image() {
    app="$1"; tag="$2"
    # `|| true`: with `set -e -o pipefail`, a grep with no match would otherwise
    # abort the whole deploy on this best-effort verification step.
    incluster="$(minikube ssh -- "docker inspect --format '{{.Id}}' $tag" 2>/dev/null | grep -oE '[0-9a-f]{64}' | head -1 || true)"
    running="$(kubectl -n estimation get pod -l "app=$app" \
        -o jsonpath='{.items[0].status.containerStatuses[0].imageID}' 2>/dev/null \
        | grep -oE '[0-9a-f]{64}' | head -1 || true)"
    if [ -n "$incluster" ] && [ -n "$running" ] && [ "$incluster" != "$running" ]; then
        echo "WARNING: $app pod is not running the image currently tagged in minikube" >&2
        echo "  in minikube: $incluster" >&2
        echo "  in pod:      $running" >&2
        echo "  The rollout may not have picked up the reloaded image; re-run this script." >&2
    else
        echo "  $app pod runs the in-cluster image (${incluster:-unknown})"
    fi
}
echo "Verifying pods run the freshly built images..."
verify_image backend  pythia/pythia-backend:1.0.0-SNAPSHOT
verify_image frontend pythia/pythia-frontend:1.0.0-SNAPSHOT

echo "Deployment complete."
kubectl -n estimation get pods
echo ""
echo "Access the backend with:"
echo "  kubectl -n estimation port-forward svc/backend 8080:8080"
echo "Access the frontend with:"
echo "  kubectl -n estimation port-forward svc/frontend 8080:80"
echo "Access the backend logs with:"
echo "  kubectl -n estimation logs -f deploy/backend"

