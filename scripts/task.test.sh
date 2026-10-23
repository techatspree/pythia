#!/usr/bin/env bash
# Tests scripts/task.sh against a temporary copy of a fixture status file
# (task-214), never the real planning/status.json:
#
#   ./scripts/task.test.sh
#
# One line per check; exits non-zero on the first failure. Runs on bash 3.2
# (macOS's /usr/bin/env bash) as well as on CI's bash.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TASK_SH="$SCRIPT_DIR/task.sh"
FIXTURE="$SCRIPT_DIR/fixtures/task-status.json"
REAL="$(dirname "$SCRIPT_DIR")/planning/status.json"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
STATUS="$TMP/status.json"
cp "$FIXTURE" "$STATUS"
real_before="$(cksum < "$REAL")"

task() { TASK_STATUS_FILE="$STATUS" "$TASK_SH" "$@"; }
field() { jq -r --arg id "$1" ".tasks[\$id].$2" "$STATUS"; }

pass() { echo "ok   $1"; }
fail() { echo "FAIL $1" >&2; exit 1; }
check() {
    # check <description> <actual> <expected>
    if [ "$2" = "$3" ]; then pass "$1"; else fail "$1 (expected '$3', got '$2')"; fi
}
check_set() {
    # check_set <description> <actual>: the value must not be null
    if [ "$2" != "null" ]; then pass "$1"; else fail "$1 (still null)"; fi
}
refused() {
    # refused <description> <task.sh args...>: the command must exit non-zero
    local description="$1"; shift
    if task "$@" >/dev/null 2>&1; then fail "$description"; else pass "$description"; fi
}

# pending on a done task resets status and timestamps — and keeps the notes.
notes_before="$(field task-001 notes)"
task pending task-001 >/dev/null
check "pending: status is pending" "$(field task-001 status)" "pending"
check "pending: started_at is null" "$(field task-001 started_at)" "null"
check "pending: completed_at is null" "$(field task-001 completed_at)" "null"
check "pending: notes are unchanged" "$(field task-001 notes)" "$notes_before"

# start sets in_progress and started_at.
task start task-002 >/dev/null
check "start: status is in_progress" "$(field task-002 status)" "in_progress"
check_set "start: started_at is set" "$(field task-002 started_at)"

# done sets done and completed_at.
task "done" task-002 >/dev/null
check "done: status is done" "$(field task-002 status)" "done"
check_set "done: completed_at is set" "$(field task-002 completed_at)"

# start and done refuse a task that is already done.
refused "done on a done task is refused" "done" task-002
refused "start on a done task is refused" start task-002

# add returns the next id and stores the description as the notes.
new_id="$(task add "Write the release notes" 2>/dev/null)"
check "add: prints the next id" "$new_id" "task-004"
check "add: stores the notes" "$(field task-004 notes)" "Write the release notes"
check "add: registers it as pending" "$(field task-004 status)" "pending"

# done on a task that was never started sets started_at too; on a started
# task it keeps the original started_at.
task "done" task-004 >/dev/null
check_set "done on a never-started task: started_at is set" "$(field task-004 started_at)"
task "done" task-003 >/dev/null
check "done on a started task: started_at is kept" "$(field task-003 started_at)" "2026-01-03T10:00:00+00:00"

# An unknown id is an error, not a new entry.
refused "an unknown id is refused" start task-999

check "the real planning/status.json is untouched" "$(cksum < "$REAL")" "$real_before"
