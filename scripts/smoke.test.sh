#!/usr/bin/env bash
# Tests scripts/smoke.sh against a stub HTTP server (task-087), never a real
# deployment:
#
#   ./scripts/smoke.test.sh
#
# The stub imitates the ingress: nginx in front of the backend, which proxies
# /api/ and answers every other path with the SPA's index.html and 200. Each
# mode is one state of the stack, and smoke.sh must pass or fail as tabulated
# in planning/tasks/task-087.yaml. The stub answers 404 for any /api path the
# real backend does not serve. Two bugs task-087 fixed are pinned here:
#   - `db-down`: smoke.sh used to probe /q/health/ready, which the SPA fallback
#     answers with 200, so it passed while PostgreSQL was unreachable;
#   - `healthy`: smoke.sh used to call /api/estimations, which has no collection
#     GET and answered 404, so it failed against every healthy stack.
#
# One line per case; exits non-zero on the first failure. Needs bash (3.2 is
# enough), curl and python3.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SMOKE_SH="$SCRIPT_DIR/smoke.sh"

TMP="$(mktemp -d)"
STUB_PID=""
cleanup() {
    if [ -n "$STUB_PID" ]; then kill "$STUB_PID" 2>/dev/null || true; fi
    rm -rf "$TMP"
}
trap cleanup EXIT

cat > "$TMP/stub.py" <<'PY'
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer

MODE = sys.argv[1]
SPA = b"<!doctype html><html><body>Pythia</body></html>"


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def send(self, status, content_type, body):
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        path = self.path.split("?")[0]
        if not path.startswith("/api/"):
            return self.send(200, "text/html", SPA)  # nginx's SPA fallback
        if MODE == "backend-down":
            return self.send(502, "text/html", b"<html>502 Bad Gateway</html>")
        if path == "/api/system":
            if MODE == "db-down":
                return self.send(500, "application/json", b'{"message":"Internal Server Error"}')
            return self.send(200, "application/json",
                             b'{"displayName":null,"hasCustomCss":false,"customCssFilename":null,'
                             b'"customCssUpdatedAt":null,"hoursPerDay":8.0}')
        if path == "/api/projects":
            auth = self.headers.get("Authorization")
            if auth == "Bearer good" or MODE == "api-open":
                return self.send(200, "application/json", b"[]")
            return self.send(401, "application/json", b'{"message":"Authentication required.","status":401}')
        return self.send(404, "application/json", b'{"message":"Not Found"}')


server = HTTPServer(("127.0.0.1", 0), Handler)
print(server.server_address[1], flush=True)
server.serve_forever()
PY

start_stub() {
    # start_stub <mode>: starts the stub and sets PORT.
    python3 -I "$TMP/stub.py" "$1" > "$TMP/port" &
    STUB_PID=$!
    PORT=""
    for _ in $(seq 1 50); do
        PORT="$(head -n1 "$TMP/port" 2>/dev/null || true)"
        if [ -n "$PORT" ] && curl -s -o /dev/null "http://127.0.0.1:$PORT/"; then return 0; fi
        sleep 0.1
    done
    echo "FAIL could not start the stub server in mode $1" >&2
    exit 1
}

stop_stub() {
    kill "$STUB_PID" 2>/dev/null || true
    wait "$STUB_PID" 2>/dev/null || true
    STUB_PID=""
}

check() {
    # check <mode> <token or ""> <expect: pass|fail>
    local mode="$1" token="$2" expect="$3" label rc
    label="$mode (SMOKE_TOKEN ${token:-unset}): expected smoke.sh to $expect"
    start_stub "$mode"
    rc=0
    if [ -n "$token" ]; then
        SMOKE_TOKEN="$token" "$SMOKE_SH" "http://127.0.0.1:$PORT" > "$TMP/out" 2>&1 || rc=$?
    else
        env -u SMOKE_TOKEN "$SMOKE_SH" "http://127.0.0.1:$PORT" > "$TMP/out" 2>&1 || rc=$?
    fi
    stop_stub
    if { [ "$expect" = pass ] && [ "$rc" -eq 0 ]; } || { [ "$expect" = fail ] && [ "$rc" -ne 0 ]; }; then
        echo "ok   $label"
    else
        echo "FAIL $label (exit $rc)" >&2
        sed 's/^/     /' "$TMP/out" >&2
        exit 1
    fi
}

check healthy      ""   pass
check healthy      good pass
check db-down      ""   fail
check backend-down ""   fail
check api-open     ""   fail
