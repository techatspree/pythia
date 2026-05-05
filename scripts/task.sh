#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
STATUS_FILE="$PROJECT_ROOT/planning/status.json"

if ! command -v jq &>/dev/null; then
  echo "Error: jq is required. Install with: brew install jq" >&2
  exit 1
fi

if [[ ! -f "$STATUS_FILE" ]]; then
  echo "Error: $STATUS_FILE not found" >&2
  exit 1
fi

usage() {
  echo "Usage: $0 <command> <task-id>"
  echo ""
  echo "Commands:"
  echo "  start    <task-id>   Mark task as in_progress with current timestamp"
  echo "  done     <task-id>   Mark task as done with current timestamp"
  echo "  pending  <task-id>   Reset task back to pending"
  echo ""
  echo "Examples:"
  echo "  $0 start task-013"
  echo "  $0 done task-013"
  exit 1
}

if [[ $# -lt 2 ]]; then
  usage
fi

COMMAND="$1"
TASK_ID="$2"

# Validate task exists in status.json
if ! jq -e ".tasks[\"$TASK_ID\"]" "$STATUS_FILE" >/dev/null 2>&1; then
  echo "Error: $TASK_ID not found in $STATUS_FILE" >&2
  exit 1
fi

NOW=$(date -u +"%Y-%m-%dT%H:%M:%S+00:00")

case "$COMMAND" in
  start)
    current_status=$(jq -r ".tasks[\"$TASK_ID\"].status" "$STATUS_FILE")
    if [[ "$current_status" == "done" ]]; then
      echo "Warning: $TASK_ID is already done. Use 'pending' first to reset." >&2
      exit 1
    fi
    jq ".tasks[\"$TASK_ID\"].status = \"in_progress\" | .tasks[\"$TASK_ID\"].started_at = \"$NOW\"" \
      "$STATUS_FILE" > "$STATUS_FILE.tmp" && mv "$STATUS_FILE.tmp" "$STATUS_FILE"
    echo "✓ $TASK_ID marked as in_progress (started: $NOW)"
    ;;
  done)
    current_status=$(jq -r ".tasks[\"$TASK_ID\"].status" "$STATUS_FILE")
    if [[ "$current_status" == "done" ]]; then
      echo "Warning: $TASK_ID is already done." >&2
      exit 1
    fi
    # If task was never started, set started_at too
    started=$(jq -r ".tasks[\"$TASK_ID\"].started_at" "$STATUS_FILE")
    if [[ "$started" == "null" ]]; then
      jq ".tasks[\"$TASK_ID\"].status = \"done\" | .tasks[\"$TASK_ID\"].started_at = \"$NOW\" | .tasks[\"$TASK_ID\"].completed_at = \"$NOW\"" \
        "$STATUS_FILE" > "$STATUS_FILE.tmp" && mv "$STATUS_FILE.tmp" "$STATUS_FILE"
    else
      jq ".tasks[\"$TASK_ID\"].status = \"done\" | .tasks[\"$TASK_ID\"].completed_at = \"$NOW\"" \
        "$STATUS_FILE" > "$STATUS_FILE.tmp" && mv "$STATUS_FILE.tmp" "$STATUS_FILE"
    fi
    echo "✓ $TASK_ID marked as done (completed: $NOW)"
    ;;
  pending)
    jq ".tasks[\"$TASK_ID\"].status = \"pending\" | .tasks[\"$TASK_ID\"].started_at = null | .tasks[\"$TASK_ID\"].completed_at = null | .tasks[\"$TASK_ID\"].notes = \"\"" \
      "$STATUS_FILE" > "$STATUS_FILE.tmp" && mv "$STATUS_FILE.tmp" "$STATUS_FILE"
    echo "✓ $TASK_ID reset to pending"
    ;;
  *)
    echo "Error: Unknown command '$COMMAND'" >&2
    usage
    ;;

esac
