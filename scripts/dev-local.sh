#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

BACKEND_DIR="$PROJECT_ROOT/src/backend/implementation"
FRONTEND_DIR="$PROJECT_ROOT/src/frontend"

# Modular auth selector — same value must be set on backend and
# frontend for the auth modules to agree.
export APP_AUTH_PROVIDER=dev
export VITE_AUTH_PROVIDER=dev

cleanup() {
    echo "Stopping services..."
    kill $BACKEND_PID $FRONTEND_PID 2>/dev/null
    wait $BACKEND_PID $FRONTEND_PID 2>/dev/null
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
