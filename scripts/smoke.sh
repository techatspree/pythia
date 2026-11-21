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
#   1. GET /api/system must return the backend's JSON (200, application/json,
#      a body with "hoursPerDay"). The endpoint is @PermitAll and reads the
#      installation's settings row, so it proves the ingress reaches the backend
#      AND the backend reaches PostgreSQL. It used to probe /q/health/ready, which
#      cannot work through the ingress: the ingress routes everything to the
#      frontend, whose nginx proxies only /api/ and /ws/ and answers any other
#      path with index.html and 200 — whatever state the backend is in
#      (task-087). /q/health/ready stays the in-cluster readiness probe that
#      deploy.sh's `kubectl rollout status` waits on.
#   2. GET /api/projects (a VIEWER read) must answer through the same ingress.
#      With SMOKE_TOKEN set the call must return 200; without it, 401 — which
#      still proves the route reaches the backend AND that authentication is
#      enforced. An unauthenticated 200 would mean the API is wide open and is a
#      failure. It used to call /api/estimations, which has no collection GET
#      and answered 404 with or without a token (task-087).

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

echo -n "  /api/system ... "
body_file="$(mktemp)"
trap 'rm -f "$body_file"' EXIT
# One request: the body goes to the file, status and content type to stdout.
# Same --max-time / `|| true` reasoning as status_of above.
meta="$(curl -sS -o "$body_file" -w '%{http_code} %{content_type}' --max-time 20 \
    "$BASE_URL/api/system" || true)"
system_status="${meta%% *}"
system_type="${meta#* }"
if [ "$system_status" != "200" ]; then
    echo "$system_status"
    echo "Error: /api/system answered $system_status, expected 200 — the backend or its database is not available." >&2
    exit 1
fi
case "$system_type" in
    application/json*) ;;
    *)
        echo "$system_status ($system_type)"
        echo "Error: /api/system answered with '$system_type', not JSON — the request was answered by the SPA fallback, not the backend." >&2
        exit 1
        ;;
esac
if ! grep -q '"hoursPerDay"' "$body_file"; then
    echo "$system_status"
    echo "Error: /api/system returned JSON without \"hoursPerDay\" — not the backend's settings." >&2
    exit 1
fi
echo "200"

echo -n "  /api/projects ... "
if [ -n "${SMOKE_TOKEN:-}" ]; then
    expected=200
    api="$(status_of -H "Authorization: Bearer $SMOKE_TOKEN" "$BASE_URL/api/projects")"
else
    expected=401
    api="$(status_of "$BASE_URL/api/projects")"
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
