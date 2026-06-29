#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/src/backend/implementation"

# Strict dev backend: same dev profile (PostgreSQL via Dev Services —
# Docker must be running), but with the convenience default-user fallback
# explicitly disabled (empty) and on port 8081 so it can coexist with the
# primary dev backend on :8080.
# Used by the auth-gate Playwright cases to assert that unauthenticated
# requests are rejected with 401.
export APP_AUTH_PROVIDER=dev
export APP_AUTH_DEV_DEFAULT_USER=
export QUARKUS_HTTP_PORT=8081
export QUARKUS_PROFILE=dev

# Snapshot Kotlin daemons that already exist before we spawn ours, so
# cleanup only kills the daemon THIS script started — NOT the one the
# primary dev backend on :8080 may still be using.
KOTLIN_DAEMONS_BEFORE=$(pgrep -u "$USER" -f KotlinCompileDaemon 2>/dev/null | sort -n | xargs || true)

cleanup() {
    echo "Stopping strict backend..."
    kill $BACKEND_PID 2>/dev/null
    wait $BACKEND_PID 2>/dev/null
    # Sweep only Kotlin daemons that appeared after this script started.
    # Quarkus dev mode can leave a detached Kotlin daemon behind, so it would
    # otherwise outlive the kill above and accumulate across sessions (symptom:
    # subsequent builds fail with "Failed connecting to the daemon in 4 retries").
    KOTLIN_DAEMONS_NOW=$(pgrep -u "$USER" -f KotlinCompileDaemon 2>/dev/null || true)
    for pid in $KOTLIN_DAEMONS_NOW; do
        case " $KOTLIN_DAEMONS_BEFORE " in
            *" $pid "*) ;;  # was already running; leave alone
            *) kill -9 "$pid" 2>/dev/null || true ;;
        esac
    done
    echo "Done."
}
trap cleanup EXIT INT TERM

echo "Starting strict backend on :8081 (dev profile, fallback disabled)..."
cd "$PROJECT_ROOT"
# stdin from /dev/null + plain console: Quarkus dev mode's interactive console
# reads stdin, and a backgrounded process reading the TTY is SIGTTIN-suspended.
"$PROJECT_ROOT/gradlew" --console=plain :backend:implementation:quarkusDev < /dev/null &
BACKEND_PID=$!

echo "Waiting for strict backend to be ready..."
until curl -s -o /dev/null http://localhost:8081/q/health 2>/dev/null; do
    sleep 2
done
echo "Strict backend ready at http://localhost:8081"
echo "Run: STRICT_BACKEND_URL=http://localhost:8081 npx playwright test e2e/auth-gate.test.ts"
echo "Press Ctrl+C to stop."
wait
