#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
# Overridable so scripts/task.test.sh can run against a temporary copy.
STATUS_FILE="${TASK_STATUS_FILE:-$PROJECT_ROOT/planning/status.json}"

if ! command -v jq &>/dev/null; then
  echo "Error: jq is required. Install with: brew install jq" >&2
  exit 1
fi

if [[ ! -f "$STATUS_FILE" ]]; then
  echo "Error: $STATUS_FILE not found" >&2
  exit 1
fi

usage() {
  echo "Usage: $0 <command> <args>"
  echo ""
  echo "Commands:"
  echo "  add      <description>   Register a new task (auto-assigns next id) and"
  echo "                           print the new task id on stdout"
  echo "  start    <task-id>       Mark task as in_progress with current timestamp"
  echo "  done     <task-id>       Mark task as done with current timestamp"
  echo "  pending  <task-id>       Reset task back to pending (its notes are kept)"
  echo ""
  echo "Environment:"
  echo "  TASK_STATUS_FILE         Status file to edit (default: planning/status.json)"
  echo ""
  echo "Examples:"
  echo "  $0 add \"Add CSV export to the version comparison endpoint\""
  echo "  $0 start task-013"
  echo "  $0 done task-013"
  exit 1
}

if [[ $# -lt 2 ]]; then
  usage
fi

COMMAND="$1"

# 'add' takes a free-text description rather than an existing task id; handle it
# before the task-id validation that the other commands share.
if [[ "$COMMAND" == "add" ]]; then
  DESCRIPTION="$2"
  if [[ -z "${DESCRIPTION// }" ]]; then
    echo "Error: a non-empty task description is required" >&2
    usage
  fi
  # Next id = (highest existing numeric id) + 1, zero-padded to three digits.
  NEXT_NUM=$(jq -r '([.tasks | keys[] | capture("task-(?<n>[0-9]+)").n | tonumber] | max // 0) + 1' "$STATUS_FILE")
  NEW_ID=$(printf "task-%03d" "$NEXT_NUM")
  if jq -e ".tasks[\"$NEW_ID\"]" "$STATUS_FILE" >/dev/null 2>&1; then
    echo "Error: $NEW_ID already exists in $STATUS_FILE" >&2
    exit 1
  fi
  jq --arg id "$NEW_ID" --arg notes "$DESCRIPTION" \
    '.tasks[$id] = {status: "pending", started_at: null, completed_at: null, notes: $notes}' \
    "$STATUS_FILE" > "$STATUS_FILE.tmp" && mv "$STATUS_FILE.tmp" "$STATUS_FILE"
  echo "✓ Registered $NEW_ID (pending)" >&2
  # The id goes to stdout alone so callers can capture it: NEW_ID=$(task.sh add "...")
  echo "$NEW_ID"
  exit 0
fi

TASK_ID="$2"

# Validate task exists in status.json
if ! jq -e --arg id "$TASK_ID" '.tasks[$id]' "$STATUS_FILE" >/dev/null 2>&1; then
  echo "Error: $TASK_ID not found in $STATUS_FILE" >&2
  exit 1
fi

NOW=$(date -u +"%Y-%m-%dT%H:%M:%S+00:00")

case "$COMMAND" in
  start)
    current_status=$(jq -r --arg id "$TASK_ID" '.tasks[$id].status' "$STATUS_FILE")
    if [[ "$current_status" == "done" ]]; then
      echo "Warning: $TASK_ID is already done. Use 'pending' first to reset." >&2
      exit 1
    fi
    jq --arg id "$TASK_ID" --arg now "$NOW" \
      '.tasks[$id].status = "in_progress" | .tasks[$id].started_at = $now' \
      "$STATUS_FILE" > "$STATUS_FILE.tmp" && mv "$STATUS_FILE.tmp" "$STATUS_FILE"
    echo "✓ $TASK_ID marked as in_progress (started: $NOW)"
    ;;
  done)
    current_status=$(jq -r --arg id "$TASK_ID" '.tasks[$id].status' "$STATUS_FILE")
    if [[ "$current_status" == "done" ]]; then
      echo "Warning: $TASK_ID is already done." >&2
      exit 1
    fi
    # If task was never started, set started_at too
    jq --arg id "$TASK_ID" --arg now "$NOW" \
      '.tasks[$id].status = "done" | .tasks[$id].completed_at = $now
       | .tasks[$id].started_at = (.tasks[$id].started_at // $now)' \
      "$STATUS_FILE" > "$STATUS_FILE.tmp" && mv "$STATUS_FILE.tmp" "$STATUS_FILE"
    echo "✓ $TASK_ID marked as done (completed: $NOW)"
    ;;
  pending)
    # Only the status and its timestamps: the notes are the task's record and
    # stay (task-214 — resetting task-181 once wiped them).
    jq --arg id "$TASK_ID" \
      '.tasks[$id].status = "pending" | .tasks[$id].started_at = null | .tasks[$id].completed_at = null' \
      "$STATUS_FILE" > "$STATUS_FILE.tmp" && mv "$STATUS_FILE.tmp" "$STATUS_FILE"
    echo "✓ $TASK_ID reset to pending"
    ;;
  *)
    echo "Error: Unknown command '$COMMAND'" >&2
    usage
    ;;

esac
