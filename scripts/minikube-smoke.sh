#!/bin/bash

# Read-only smoke test of the local minikube stack (task-087).
#
#   ./scripts/minikube-smoke.sh
#   SMOKE_TOKEN=<bearer> ./scripts/minikube-smoke.sh     # adds an authenticated read
#
# Port-forwards svc/backend and svc/frontend of the minikube cluster, runs the
# JUnit smoke suite (`:backend:end2end:smokeTest`) and then scripts/smoke.sh,
# the release smoke test, against the forwarded frontend. Every request is a
# GET; nothing is written and no data is assumed to exist. Never part of
# `./gradlew build`.
#
# The forwards use SMOKE_BACKEND_PORT / SMOKE_FRONTEND_PORT (18080 / 18081),
# NOT 8080/8081: 8080 is kept free for the documented frontend forward on that
# port (CLAUDE.md) and 8081 is docs/deployment.md's staging forward. A port that is
# already bound is refused rather than shared, because whatever holds it would
# silently be tested instead.

# Refuse to be sourced: `set -euo pipefail` and the trap would leak into the
# caller's shell.
_sourced=0
if [ -n "${ZSH_EVAL_CONTEXT:-}" ]; then
    case "$ZSH_EVAL_CONTEXT" in *:file*) _sourced=1 ;; esac
elif [ -n "${BASH_SOURCE:-}" ]; then
    [ "${BASH_SOURCE[0]}" != "${0}" ] && _sourced=1
fi
if [ "$_sourced" = 1 ]; then
    echo "Error: do not 'source' this script — run it directly:" >&2
    echo "  ./scripts/minikube-smoke.sh" >&2
    # SC2317: `return` succeeds when sourced (short-circuiting the `||`); the
    # `exit` only runs in the executed case. Dual-mode idiom, not dead code.
    # shellcheck disable=SC2317
    return 1 2>/dev/null || exit 1
fi
unset _sourced

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# minikube-env.sh is linted on its own; plain `shellcheck` (no -x) does not follow it.
# shellcheck source=scripts/minikube-env.sh disable=SC1091
. "$SCRIPT_DIR/minikube-env.sh"

: "${SMOKE_BACKEND_PORT:=18080}"
: "${SMOKE_FRONTEND_PORT:=18081}"

for port in "$SMOKE_BACKEND_PORT" "$SMOKE_FRONTEND_PORT"; do
    if holder="$(lsof -nP -iTCP:"$port" -sTCP:LISTEN 2>/dev/null)"; then
        echo "Error: port $port is already in use; it would be tested instead of minikube:" >&2
        echo "$holder" >&2
        echo "Free it, or pick other ports via SMOKE_BACKEND_PORT / SMOKE_FRONTEND_PORT." >&2
        exit 1
    fi
done

FORWARD_PIDS=()
cleanup() {
    for pid in "${FORWARD_PIDS[@]+"${FORWARD_PIDS[@]}"}"; do
        kill "$pid" 2>/dev/null || true
    done
}
trap cleanup EXIT

kubectl --context minikube -n "$MINIKUBE_NAMESPACE" \
    port-forward svc/backend "$SMOKE_BACKEND_PORT:8080" > /dev/null &
FORWARD_PIDS+=("$!")
kubectl --context minikube -n "$MINIKUBE_NAMESPACE" \
    port-forward svc/frontend "$SMOKE_FRONTEND_PORT:80" > /dev/null &
FORWARD_PIDS+=("$!")

BACKEND_URL="http://localhost:$SMOKE_BACKEND_PORT"
FRONTEND_URL="http://localhost:$SMOKE_FRONTEND_PORT"

wait_for() {
    # wait_for <name> <url>: 30 tries, one second apart.
    for _ in $(seq 1 30); do
        if curl -s -o /dev/null --max-time 2 "$2"; then return 0; fi
        sleep 1
    done
    echo "Error: the $1 port-forward did not come up ($2)." >&2
    exit 1
}
wait_for backend "$BACKEND_URL/q/health"
wait_for frontend "$FRONTEND_URL/"

echo "READ-ONLY smoke test of minikube namespace '$MINIKUBE_NAMESPACE' (GET requests only)."

failed=()
"$PROJECT_ROOT/gradlew" -p "$PROJECT_ROOT" :backend:end2end:smokeTest \
    -Dbackend.url="$BACKEND_URL" -Dfrontend.url="$FRONTEND_URL" || failed+=("JUnit smoke suite")
"$PROJECT_ROOT/scripts/smoke.sh" "$FRONTEND_URL" || failed+=("scripts/smoke.sh")

if [ "${#failed[@]}" -gt 0 ]; then
    echo "Smoke test FAILED: ${failed[*]}" >&2
    exit 1
fi
echo "Smoke test passed: JUnit smoke suite and scripts/smoke.sh."
