#!/usr/bin/env bash
set -euo pipefail

# Read-only audit of the planning system's phase data (task-165).
#
# `planning/plan.yaml` no longer carries per-phase task lists: a task's phase
# lives in its own file and `./scripts/status.sh` groups by it. That makes the
# per-task `phase:` field authoritative, so a WRONG field is now a wrong answer
# rather than a disagreement between two copies. This script surfaces the two
# ways it can be wrong.
#
# It changes nothing. It exits non-zero ONLY for an unknown phase id, which is
# unambiguously broken; a cross-phase dependency may be a deliberate exception
# and is reported for a human to judge.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TASKS_DIR="$PROJECT_ROOT/planning/tasks"
PLAN_FILE="$PROJECT_ROOT/planning/plan.yaml"

[[ -f "$PLAN_FILE" ]] || { echo "No plan file at $PLAN_FILE" >&2; exit 1; }
[[ -d "$TASKS_DIR" ]] || { echo "No tasks directory at $TASKS_DIR" >&2; exit 1; }

# Phase ids in declaration order, so a phase's ordinal is its position. Matches
# how status.sh reads the block: `id:` lines under `phases:`, nothing else.
phase_ids() {
  awk '
    /^phases:/ { inphase = 1; next }
    inphase && /^[^[:space:]#]/ { inphase = 0 }
    !inphase { next }
    /- id:/ { id = $0; sub(/.*id:[[:space:]]*/, "", id); print id }
  ' "$PLAN_FILE"
}

# task id <TAB> phase <TAB> space-separated deps, one row per task file.
task_rows() {
  for f in "$TASKS_DIR"/task-*.yaml; do
    [[ -e "$f" ]] || continue
    awk -v file="$f" '
      /^id:/    { id = $2 }
      /^phase:/ { ph = $2 }
      /^depends_on:/ {
        deps = $0
        sub(/^depends_on:[[:space:]]*/, "", deps)
        gsub(/[][,]/, " ", deps)
      }
      END {
        if (id == "") { id = file }
        printf "%s\t%s\t%s\n", id, ph, deps
      }
    ' "$f"
  done
}

ORDINALS="$(mktemp)"; ROWS="$(mktemp)"
trap 'rm -f "$ORDINALS" "$ROWS"' EXIT
phase_ids | awk '{ printf "%s\t%d\n", $0, NR }' > "$ORDINALS"
task_rows > "$ROWS"

echo "Phase audit — $(wc -l < "$ROWS" | tr -d ' ') task files, $(wc -l < "$ORDINALS" | tr -d ' ') phases"
echo

# ---------------------------------------------------------------- unknown phase
unknown=0
echo "Tasks whose phase does not exist in plan.yaml:"
while IFS=$'\t' read -r id ph _deps; do
  [[ -z "$ph" ]] && continue
  if ! cut -f1 "$ORDINALS" | grep -qx -- "$ph"; then
    echo "  $id declares $ph, which plan.yaml does not define"
    unknown=$((unknown + 1))
  fi
done < "$ROWS"
[[ $unknown -eq 0 ]] && echo "  none"
echo

# ------------------------------------------------- dependency ordering (report)
# A task should not depend on one in a LATER phase. This is the rule improve-task
# applies per task; running it across the plan is what surfaces a mis-declared
# `phase:` field. Reported only — a violation may be deliberate.
ordinal_of() { awk -F'\t' -v p="$1" '$1 == p { print $2; exit }' "$ORDINALS"; }
violations=0
echo "Tasks depending on a LATER phase (informational — not an error):"
while IFS=$'\t' read -r id ph deps; do
  [[ -z "$ph" || -z "$deps" ]] && continue
  mine="$(ordinal_of "$ph")"
  [[ -z "$mine" ]] && continue
  for d in $deps; do
    [[ "$d" =~ ^task-[0-9]+$ ]] || continue
    dfile="$TASKS_DIR/$d.yaml"
    [[ -f "$dfile" ]] || continue
    dph="$(awk '/^phase:/ { print $2; exit }' "$dfile")"
    [[ -z "$dph" ]] && continue
    theirs="$(ordinal_of "$dph")"
    [[ -z "$theirs" ]] && continue
    if [[ "$theirs" -gt "$mine" ]]; then
      echo "  $id ($ph) depends on $d ($dph)"
      violations=$((violations + 1))
    fi
  done
done < "$ROWS"
[[ $violations -eq 0 ]] && echo "  none"
echo

if [[ $unknown -gt 0 ]]; then
  echo "FAIL: $unknown task(s) name a phase that does not exist." >&2
  exit 1
fi
echo "OK: every task's phase exists. $violations ordering note(s) above."
