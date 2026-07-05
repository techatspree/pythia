#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
STATUS_FILE="$PROJECT_ROOT/planning/status.json"
TASKS_DIR="$PROJECT_ROOT/planning/tasks"
PLAN_FILE="$PROJECT_ROOT/planning/plan.yaml"

# Only list tasks that are still pending (neither done nor in progress).
PENDING_ONLY=false
for arg in "$@"; do
  case "$arg" in
    --pending) PENDING_ONLY=true ;;
    -h|--help)
      echo "Usage: $(basename "$0") [--pending]"
      echo "  --pending   Only list tasks that are still pending."
      exit 0
      ;;
    *)
      echo "Error: unknown option '$arg' (try --help)" >&2
      exit 1
      ;;
  esac
done

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
if [[ "$total" -gt 0 ]]; then
  pct=$((done_count * 100 / total))
else
  pct=0
fi

bar_width=30
filled=$((pct * bar_width / 100))
empty=$((bar_width - filled))
bar=$(printf '%0.s#' $(seq 1 $filled 2>/dev/null) || true)
bar+=$(printf '%0.s-' $(seq 1 $empty 2>/dev/null) || true)

# Build one tab-delimited record per task so we read each task file only once:
#   phase <TAB> task_id <TAB> status <TAB> title <TAB> deps
# Titles/deps never contain tabs, so the tab delimiter is safe.
RECORDS="$(mktemp)"
trap 'rm -f "$RECORDS"' EXIT

while IFS=$'\t' read -r task_id status; do
  task_file="$TASKS_DIR/${task_id}.yaml"
  title="" ; phase="" ; deps=""
  if [[ -f "$task_file" ]]; then
    title=$(grep "^title:" "$task_file" | sed 's/^title: *//')
    phase=$(grep "^phase:" "$task_file" | sed 's/^phase: *//')
    deps=$(grep "^depends_on:" "$task_file" | sed 's/^depends_on: *\[//;s/\]//;s/ //g')
  fi
  [[ -z "$phase" ]] && phase="(none)"
  printf '%s\t%s\t%s\t%s\t%s\n' "$phase" "$task_id" "$status" "$title" "$deps"
done < <(jq -r '.tasks | to_entries[] | "\(.key)\t\(.value.status)"' "$STATUS_FILE") > "$RECORDS"

echo ""
echo "  Project Status"
echo "  [$bar] $pct% ($done_count/$total done)"
echo ""
printf "  %-10s %-12s %-44s %s\n" "TASK" "STATUS" "TITLE" "DEPENDS ON"

# Render every task row belonging to a single phase, sorted by task id.
# Task ids are zero-padded (task-001 … task-078), so a plain lexical sort of
# the record lines orders them numerically within the phase.
# Returns non-zero (without printing) when the phase has no tasks.
render_phase() {
  local pid="$1" pname="$2"
  local rows
  rows=$(awk -F'\t' -v p="$pid" '$1 == p' "$RECORDS")
  [[ -z "$rows" ]] && return 1

  local ptotal pdone ppct phase_num
  ptotal=$(printf '%s\n' "$rows" | wc -l | tr -d ' ')
  pdone=$(printf '%s\n' "$rows" | awk -F'\t' '$3 == "done"' | wc -l | tr -d ' ')
  if [[ "$ptotal" -gt 0 ]]; then ppct=$((pdone * 100 / ptotal)); else ppct=0; fi

  # In --pending mode, list only pending rows (counts above stay full so the
  # per-phase progress remains truthful). Skip the phase entirely if none pend.
  local display_rows="$rows"
  if [[ "$PENDING_ONLY" == true ]]; then
    display_rows=$(printf '%s\n' "$rows" | awk -F'\t' '$3 != "done" && $3 != "in_progress"')
    [[ -z "$display_rows" ]] && return 1
  fi

  phase_num="${pid#phase-}"
  printf "\n  \033[1;36m▌ Phase %s — %s\033[0m  \033[90m(%s/%s done, %s%%)\033[0m\n" \
    "$phase_num" "$pname" "$pdone" "$ptotal" "$ppct"

  printf '%s\n' "$display_rows" | sort | while IFS=$'\t' read -r _ task_id status title deps; do
    local icon status_col pad
    case "$status" in
      done)        icon="\033[32m✓\033[0m"; status_col="\033[32m$status\033[0m" ;;
      in_progress) icon="\033[33m●\033[0m"; status_col="\033[33m$status\033[0m" ;;
      *)           icon="\033[90m○\033[0m"; status_col="\033[90m$status\033[0m" ;;
    esac
    printf "  $icon %-9s $status_col" "$task_id"
    # pad status column manually since color codes mess up printf width
    pad=$((12 - ${#status}))
    printf '%*s' "$pad" ""
    if [[ -n "$deps" ]]; then
      printf "%-44s %s\n" "$title" "← $deps"
    else
      printf "%-44s\n" "$title"
    fi
  done
}

# Phase id → name, in the order declared under `phases:` in plan.yaml.
parse_plan_phases() {
  [[ -f "$PLAN_FILE" ]] || return 0
  awk '
    /^phases:/ { inphase = 1; next }
    inphase && /^[^[:space:]]/ { inphase = 0 }
    !inphase { next }
    /- id:/   { id = $0; sub(/.*id:[[:space:]]*/, "", id); next }
    /name:/   { name = $0; sub(/.*name:[[:space:]]*/, "", name)
                if (id != "") { print id "\t" name; id = "" } }
  ' "$PLAN_FILE"
}

printed_phases=""  # space-delimited list of phase ids already rendered

# 1) Phases in plan.yaml order.
while IFS=$'\t' read -r pid pname; do
  [[ -z "$pid" ]] && continue
  if render_phase "$pid" "$pname"; then
    printed_phases="$printed_phases $pid "
  fi
done < <(parse_plan_phases)

# 2) Any phase present on a task but missing from plan.yaml (natural order),
#    excluding the unassigned bucket which is rendered last.
while read -r pid; do
  [[ -z "$pid" || "$pid" == "(none)" ]] && continue
  case " $printed_phases " in *" $pid "*) continue ;; esac
  render_phase "$pid" "$pid (not in plan.yaml)" || true
done < <(cut -d$'\t' -f1 "$RECORDS" | sort -u | sort -t'-' -k2 -n)

# 3) Tasks with no phase, last.
render_phase "(none)" "no phase assigned" || true

echo ""
echo "  Summary: $done_count done, $in_progress in progress, $pending pending"
echo ""