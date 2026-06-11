#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/src/backend/implementation"

# Strict dev backend: same dev-local profile, but with the convenience
# default-user fallback explicitly disabled (empty) and on port 8081 so
# it can coexist with the primary dev-local backend on :8080.
# Used by the auth-gate Playwright cases to assert that unauthenticated
# requests are rejected with 401.
export APP_AUTH_PROVIDER=dev
export APP_AUTH_DEV_DEFAULT_USER=
export QUARKUS_HTTP_PORT=8081

cleanup() {
    echo "Stopping strict backend..."
    kill $BACKEND_PID 2>/dev/null
    wait $BACKEND_PID 2>/dev/null
    echo "Done."
}
trap cleanup EXIT INT TERM

echo "Starting strict backend on :8081 (dev-local profile, fallback disabled)..."
cd "$BACKEND_DIR"
mvn quarkus:dev -Pdev-local &
BACKEND_PID=$!

echo "Waiting for strict backend to be ready..."
until curl -s -o /dev/null http://localhost:8081/q/health 2>/dev/null; do
    sleep 2
done
echo "Strict backend ready at http://localhost:8081"
echo "Run: STRICT_BACKEND_URL=http://localhost:8081 npx playwright test e2e/auth-gate.test.ts"
echo "Press Ctrl+C to stop."
wait
