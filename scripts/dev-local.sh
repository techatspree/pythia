#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

BACKEND_DIR="$PROJECT_ROOT/src/backend/implementation"
FRONTEND_DIR="$PROJECT_ROOT/src/frontend"

# Modular auth selector — same value must be set on backend and
# frontend for the auth modules to agree.
export APP_AUTH_PROVIDER=devgner
export VITE_AUTH_PROVIDER=dev

# Snapshot Kotlin daemons that already exist before we spawn ours, so
# cleanup only kills the daemon THIS script started — NOT IntelliJ's
# and NOT the strict backend's (scripts/dev-local-strict.sh) if you're
# running them in parallel.
KOTLIN_DAEMONS_BEFORE=$(pgrep -u "$USER" -f KotlinCompileDaemon 2>/dev/null | sort -n | xargs || true)

cleanup() {
    echo "Stopping services..."
    kill $BACKEND_PID $FRONTEND_PID 2>/dev/null
    wait $BACKEND_PID $FRONTEND_PID 2>/dev/null
    # Sweep only Kotlin daemons that appeared after this script started.
    # `mvn quarkus:dev` detaches its daemon with -Dkotlin.environment.keepalive,
    # so it would otherwise outlive the kill above and accumulate across
    # sessions (symptom: subsequent builds fail with "Failed connecting
    # to the daemon in 4 retries").
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

echo "Starting backend (dev-local)..."
cd "$BACKEND_DIR"
mvn quarkus:dev -Pdev-local &
BACKEND_PID=$!

echo "Waiting for backend to be ready..."
until curl -s -o /dev/null http://localhost:8080/q/health 2>/dev/null; do
    sleep 2
done
echo "Backend ready at http://localhost:8080"

echo "Starting frontend..."
cd "$FRONTEND_DIR"
npm run dev &
FRONTEND_PID=$!

echo "Frontend starting at http://localhost:5173"
echo "Press Ctrl+C to stop all services."
wait
