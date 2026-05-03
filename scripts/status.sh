#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
STATUS_FILE="$PROJECT_ROOT/planning/status.json"
TASKS_DIR="$PROJECT_ROOT/planning/tasks"

if ! command -v jq &>/dev/null; then
  echo "Error: jq is required. Install with: brew install jq" >&2
  exit 1
fi

if [[ ! -f "$STATUS_FILE" ]]; then
  echo "Error: $STATUS_FILE not found" >&2
  exit 1
fi

total=$(jq '.tasks | length' "$STATUS_FILE")
done_count=$(jq '[.tasks[] | select(.status == "done")] | length' "$STATUS_FILE")
in_progress=$(jq '[.tasks[] | select(.status == "in_progress")] | length' "$STATUS_FILE")
pending=$((total - done_count - in_progress))
pct=$((done_count * 100 / total))

bar_width=30
filled=$((pct * bar_width / 100))
empty=$((bar_width - filled))
bar=$(printf '%0.s#' $(seq 1 $filled 2>/dev/null) || true)
bar+=$(printf '%0.s-' $(seq 1 $empty 2>/dev/null) || true)

echo ""
echo "  Project Status"
echo "  [$bar] $pct% ($done_count/$total done)"
echo ""
printf "  %-10s %-12s %-50s %s\n" "TASK" "STATUS" "TITLE" "PHASE"
printf "  %-10s %-12s %-50s %s\n" "----" "------" "-----" "-----"

jq -r '.tasks | keys_unsorted[]' "$STATUS_FILE" | sort -t'-' -k2 -n | while read -r task_id; do
  status=$(jq -r ".tasks[\"$task_id\"].status" "$STATUS_FILE")
  task_file="$TASKS_DIR/${task_id}.yaml"

  title=""
  phase=""
  if [[ -f "$task_file" ]]; then
    title=$(grep "^title:" "$task_file" | sed 's/^title: *//')
    phase=$(grep "^phase:" "$task_file" | sed 's/^phase: *//')
  fi

  case "$status" in
    done)        icon="\033[32m✓\033[0m"; status_col="\033[32m$status\033[0m" ;;
    in_progress) icon="\033[33m●\033[0m"; status_col="\033[33m$status\033[0m" ;;
    *)           icon="\033[90m○\033[0m"; status_col="\033[90m$status\033[0m" ;;
  esac

  printf "  $icon %-9s $status_col" "$task_id"
  # pad status column manually since color codes mess up printf width
  pad=$((12 - ${#status}))
  printf '%*s' "$pad" ""
  printf "%-50s %s\n" "$title" "$phase"
done

echo ""
echo "  Summary: $done_count done, $in_progress in progress, $pending pending"
echo ""
