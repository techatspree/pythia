#!/bin/bash

# Refuse to be sourced. This script exports APP_AUTH_PROVIDER=dev and
# QUARKUS_PROFILE=dev so its child gradle/vite processes pick the dev auth
# module and dev run profile. Those exports are meant for THIS process and its
# children only — when the script is executed (./scripts/dev.sh) they stay in
# the subshell and never touch your interactive shell. If you `source` it
# instead, QUARKUS_PROFILE=dev (and APP_AUTH_PROVIDER=dev) leak into your shell
# and then silently steer a later `./gradlew build` in the same shell. Detect
# sourcing under both bash and zsh and bail BEFORE `set -e`/`export`, so a
# sourced call leaks nothing at all (not the env vars, not the shell options).
_sourced=0
if [ -n "${ZSH_EVAL_CONTEXT:-}" ]; then
    case "$ZSH_EVAL_CONTEXT" in *:file*) _sourced=1 ;; esac
elif [ -n "${BASH_SOURCE:-}" ]; then
    [ "${BASH_SOURCE[0]}" != "${0}" ] && _sourced=1
fi
if [ "$_sourced" = 1 ]; then
    echo "Error: do not 'source' this script — run it directly:" >&2
    echo "  ./scripts/dev.sh" >&2
    echo "Sourcing leaks QUARKUS_PROFILE=dev / APP_AUTH_PROVIDER=dev into your shell and steers './gradlew build'." >&2
    # SC2317: `return` succeeds when sourced (short-circuiting the `||`); the
    # `exit` only runs in the executed case. Dual-mode idiom, not dead code.
    # shellcheck disable=SC2317
    return 1 2>/dev/null || exit 1
fi
unset _sourced

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
FRONTEND_DIR="$PROJECT_ROOT/src/frontend"

# Modular auth selector — same value must be set on backend and
# frontend for the auth modules to agree.
export APP_AUTH_PROVIDER=dev
export VITE_AUTH_PROVIDER=dev

# Quarkus run profile. Gradle does not forward -Dquarkus.profile to the
# dev-mode JVM, so pass the profile via the environment. The `dev` profile
# runs PostgreSQL via Quarkus Dev Services, so DOCKER MUST BE RUNNING.
export QUARKUS_PROFILE=dev

# Snapshot Kotlin daemons that already exist before we spawn ours, so
# cleanup only kills the daemon THIS script started — NOT IntelliJ's.
KOTLIN_DAEMONS_BEFORE=$(pgrep -u "$USER" -f KotlinCompileDaemon 2>/dev/null | sort -n | xargs || true)

# List the Quarkus Dev Services / Testcontainers containers (the postgres:16
# Dev Services DB plus its ryuk reaper). If Docker is down these calls return
# empty, which is harmless.
dev_service_containers() {
    {
        docker ps -q --filter "label=org.testcontainers=true"
        docker ps -q --filter "label=org.testcontainers.ryuk=true"
    } 2>/dev/null | sort -u
}

# Snapshot the ones that already exist before we start, so cleanup only stops
# containers THIS run spawned — never a PostgreSQL a developer runs by hand.
DEV_CONTAINERS_BEFORE=$(dev_service_containers | xargs || true)

cleanup() {
    echo "Stopping services..."
    kill $BACKEND_PID $FRONTEND_PID 2>/dev/null
    wait $BACKEND_PID $FRONTEND_PID 2>/dev/null
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
    # Stop the Dev Services PostgreSQL container (and its ryuk reaper) that this
    # run started but that outlived the kill above — a JVM killed abruptly does
    # not always tear down its Testcontainers container, which then keeps port
    # 5432 bound so the next dev.sh fails with "Bind for 0.0.0.0:5432 failed:
    # port is already allocated". Only touch containers that appeared AFTER this
    # script started.
    for cid in $(dev_service_containers); do
        case " $DEV_CONTAINERS_BEFORE " in
            *" $cid "*) ;;  # pre-existing; leave alone
            *) docker stop -t 3 "$cid" >/dev/null 2>&1 || true ;;
        esac
    done
    echo "Done."
}
trap cleanup EXIT INT TERM

echo "Starting backend (dev profile, PostgreSQL via Dev Services)..."
cd "$PROJECT_ROOT"
# Redirect stdin from /dev/null: Quarkus dev mode runs an interactive console
# that reads stdin, and a backgrounded process reading the controlling terminal
# is suspended with SIGTTIN — which would freeze the backend (and hang the
# health-check loop below, so the frontend never starts). --console=plain stops
# Gradle from driving the TTY too.
"$PROJECT_ROOT/gradlew" --console=plain :backend:implementation:quarkusDev < /dev/null &
BACKEND_PID=$!

echo "Waiting for backend to be ready..."
until curl -s -o /dev/null http://localhost:8090/q/health 2>/dev/null; do
    sleep 2
done
echo "Backend ready at http://localhost:8090"

echo "Starting frontend..."
cd "$FRONTEND_DIR"
npm run dev < /dev/null &
FRONTEND_PID=$!

echo "Frontend starting at http://localhost:5173"
echo "Press Ctrl+C to stop all services."
wait
