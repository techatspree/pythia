#!/bin/bash

# Deploys one environment's Kustomize overlay to the cluster the current kubectl
# context points at.
#
#   ./scripts/deploy.sh <staging|production>
#
# The internal GitLab job runs this over SSH ON the server, where minikube and
# its kubeconfig are both local — the minikube API server is deliberately not
# exposed to the runner. A developer runs the identical command against a local
# minikube by exporting the same variables, which is how the script gets tested
# without the corporate network.
#
# Every environment-specific coordinate — registry host, image tag, ingress
# host, OIDC endpoints — arrives through the environment, and there are
# deliberately NO defaults. A default naming a host or registry would put an
# internal coordinate into a public repository, and a default image tag would
# quietly deploy the wrong build.

# Refuse to be sourced. `set -euo pipefail` below would otherwise leak into the
# caller's interactive shell, where the next failing command or unset variable
# silently kills the session. Detect sourcing under both bash and zsh and bail
# BEFORE `set -e`, so a sourced call changes nothing at all.
_sourced=0
if [ -n "${ZSH_EVAL_CONTEXT:-}" ]; then
    case "$ZSH_EVAL_CONTEXT" in *:file*) _sourced=1 ;; esac
elif [ -n "${BASH_SOURCE:-}" ]; then
    [ "${BASH_SOURCE[0]}" != "${0}" ] && _sourced=1
fi
if [ "$_sourced" = 1 ]; then
    echo "Error: do not 'source' this script — run it directly:" >&2
    echo "  ./scripts/deploy.sh <staging|production>" >&2
    echo "Sourcing leaks 'set -euo pipefail' into your shell." >&2
    # SC2317: `return` succeeds when sourced (short-circuiting the `||`); the
    # `exit` only runs in the executed case. Dual-mode idiom, not dead code.
    # shellcheck disable=SC2317
    return 1 2>/dev/null || exit 1
fi
unset _sourced

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

ENVIRONMENT="${1:-}"
case "$ENVIRONMENT" in
    staging)    NAMESPACE=estimation-staging ;;
    production) NAMESPACE=estimation ;;
    *)
        echo "Usage: ./scripts/deploy.sh <staging|production>" >&2
        echo "" >&2
        echo "Required environment variables are reported by the script itself:" >&2
        echo "they are whichever \${...} placeholders the chosen overlay contains" >&2
        echo "(REGISTRY, IMAGE_TAG, INGRESS_HOST, and the OIDC endpoints)." >&2
        exit 1
        ;;
esac

OVERLAY="$PROJECT_ROOT/k8s/overlays/$ENVIRONMENT"

for tool in kubectl envsubst; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "Error: $tool is required." >&2
        exit 1
    fi
done

RENDERED="$(mktemp)"
trap 'rm -f "$RENDERED"' EXIT

echo "Rendering the $ENVIRONMENT overlay..."
kubectl kustomize "$OVERLAY" > "$RENDERED"

# Discover the placeholders the overlay actually left in the rendered manifests
# and require each of them to be set, rather than checking a hardcoded list.
# Two reasons: adding a variable to a manifest must not also mean editing this
# script, and envsubst replaces an UNSET variable with the empty string — so
# without this check a missing REGISTRY would deploy `/pythia-backend:` and a
# missing INGRESS_HOST an ingress with no host, instead of failing.
# shellcheck disable=SC2016  # single quotes are intentional in both: the grep
# pattern matches a literal ${NAME}, and tr's argument is the character SET
# $ { } to strip — neither is a shell expansion.
REQUIRED="$(grep -oE '\$\{[A-Z_][A-Z0-9_]*\}' "$RENDERED" | tr -d '${}' | sort -u)"

MISSING=""
ALLOW_LIST=""
for name in $REQUIRED; do
    ALLOW_LIST="${ALLOW_LIST}\${${name}} "
    if [ -z "${!name:-}" ]; then
        MISSING="$MISSING $name"
    fi
done

if [ -n "$MISSING" ]; then
    echo "Error: the $ENVIRONMENT overlay needs these variables, and they are unset:" >&2
    for name in $MISSING; do
        echo "  $name" >&2
    done
    echo "See docs/deployment.md for what each one holds." >&2
    exit 1
fi

echo "Applying to namespace $NAMESPACE..."
envsubst "$ALLOW_LIST" < "$RENDERED" | kubectl apply -f -

# Wait on the ROLLOUT rather than on "a pod is ready": `rollout status` blocks
# until the new ReplicaSet is fully available and the old pods are gone, so a
# still-terminating old pod cannot be mistaken for a finished deploy. The
# timeout is what makes a broken image fail the pipeline instead of hanging it.
echo "Waiting for the backend rollout..."
kubectl -n "$NAMESPACE" rollout status deployment/backend --timeout=300s
echo "Waiting for the frontend rollout..."
kubectl -n "$NAMESPACE" rollout status deployment/frontend --timeout=300s

echo "Deployed $ENVIRONMENT (image tag ${IMAGE_TAG:-unknown}) to $NAMESPACE."
