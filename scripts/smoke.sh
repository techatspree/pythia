#!/bin/bash

# Post-deploy smoke test.
#
#   ./scripts/smoke.sh <base-url>
#
# The internal GitLab pipeline runs this after every rollout, staging and
# production alike. A deploy that "succeeded" because `kubectl rollout status`
# returned — while the app answers 502 through the ingress, or starts and then
# fails its first database query — is exactly the failure this catches.
#
# Two checks:
#   1. /q/health/ready must return 200. That is the Quarkus readiness probe,
#      which includes the datasource, so it proves the pod reached PostgreSQL.
#   2. A known API endpoint must answer through the same ingress. With
#      SMOKE_TOKEN set the call must return 200; without it, 401 — which still
#      proves the route reaches the backend AND that authentication is enforced.
#      An unauthenticated 200 would mean the API is wide open and is a failure.

# Refuse to be sourced: `set -euo pipefail` would leak into the caller's shell.
_sourced=0
if [ -n "${ZSH_EVAL_CONTEXT:-}" ]; then
    case "$ZSH_EVAL_CONTEXT" in *:file*) _sourced=1 ;; esac
elif [ -n "${BASH_SOURCE:-}" ]; then
    [ "${BASH_SOURCE[0]}" != "${0}" ] && _sourced=1
fi
if [ "$_sourced" = 1 ]; then
    echo "Error: do not 'source' this script — run it directly:" >&2
    echo "  ./scripts/smoke.sh <base-url>" >&2
    # SC2317: `return` succeeds when sourced (short-circuiting the `||`); the
    # `exit` only runs in the executed case. Dual-mode idiom, not dead code.
    # shellcheck disable=SC2317
    return 1 2>/dev/null || exit 1
fi
unset _sourced

set -euo pipefail

BASE_URL="${1:-}"
if [ -z "$BASE_URL" ]; then
    echo "Usage: ./scripts/smoke.sh <base-url>" >&2
    echo "  e.g. ./scripts/smoke.sh https://\${INGRESS_HOST}" >&2
    exit 1
fi
BASE_URL="${BASE_URL%/}"

status_of() {
    # --max-time bounds a hung connection; the pipeline must fail, not stall.
    # `|| true` keeps a connection failure from tripping `set -e` before the
    # caller can report it: curl then prints 000, and the check below turns that
    # into "expected 200, got 000" instead of an abrupt exit with no diagnosis.
    curl -sS -o /dev/null -w '%{http_code}' --max-time 20 "$@" || true
}

echo "Smoke-testing $BASE_URL"

echo -n "  /q/health/ready ... "
ready="$(status_of "$BASE_URL/q/health/ready")"
if [ "$ready" != "200" ]; then
    echo "$ready"
    echo "Error: the backend is not ready (expected 200, got $ready)." >&2
    exit 1
fi
echo "200"

echo -n "  /api/estimations ... "
if [ -n "${SMOKE_TOKEN:-}" ]; then
    expected=200
    api="$(status_of -H "Authorization: Bearer $SMOKE_TOKEN" "$BASE_URL/api/estimations")"
else
    expected=401
    api="$(status_of "$BASE_URL/api/estimations")"
fi
if [ "$api" != "$expected" ]; then
    echo "$api"
    echo "Error: the API answered $api, expected $expected." >&2
    if [ -z "${SMOKE_TOKEN:-}" ] && [ "$api" = "200" ]; then
        echo "An unauthenticated request must NOT succeed — authentication is not enforced." >&2
    fi
    exit 1
fi
echo "$api"

echo "Smoke test passed."
