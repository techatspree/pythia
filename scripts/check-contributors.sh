#!/bin/bash
# Checks that CONTRIBUTORS lists every commit author (task-209):
#
#   ./scripts/check-contributors.sh [<contributors-file>]
#
#   <contributors-file>   defaults to CONTRIBUTORS at the repository root
#
# NOTICE names the copyright holder "Pythia Contributors", and CONTRIBUTORS
# defines who that is. This keeps the definition complete: every author of a
# non-merge commit (after .mailmap, should one ever exist) must be listed by the
# email the commit carries. It does not flag listed people without commits —
# design or review work may be listed by hand.
#
# Not counted, because they are not copyright holders: bot accounts
# (`*[bot]@users.noreply.github.com`, `noreply@github.com`) and AI assistants,
# which only ever appear in Co-Authored-By trailers, never as the author. Merge
# commits carry no authored content and are ignored. The flip side: a HUMAN
# named only in a Co-authored-by trailer is not seen either and is added by hand.
#
# Exit 0 when complete, 1 when an author is missing or a line is malformed,
# 2 in a shallow clone (one commit would make the check pass meaninglessly).
# Runs in the `build` job of .github/workflows/ci.yaml, with fetch-depth: 0.

# Refuse to be sourced: `set -euo pipefail` would leak into the caller's shell.
_sourced=0
if [ -n "${ZSH_EVAL_CONTEXT:-}" ]; then
    case "$ZSH_EVAL_CONTEXT" in *:file*) _sourced=1 ;; esac
elif [ -n "${BASH_SOURCE:-}" ]; then
    [ "${BASH_SOURCE[0]}" != "${0}" ] && _sourced=1
fi
if [ "$_sourced" = 1 ]; then
    echo "Error: do not 'source' this script — run it directly:" >&2
    echo "  ./scripts/check-contributors.sh [<contributors-file>]" >&2
    # SC2317: `return` succeeds when sourced (short-circuiting the `||`); the
    # `exit` only runs in the executed case. Dual-mode idiom, not dead code.
    # shellcheck disable=SC2317
    return 1 2>/dev/null || exit 1
fi
unset _sourced

set -euo pipefail

if [ "$#" -gt 1 ]; then
    echo "Usage: ./scripts/check-contributors.sh [<contributors-file>]" >&2
    exit 1
fi

if [ "$(git rev-parse --is-shallow-repository)" = true ]; then
    echo "Error: needs full history (actions/checkout fetch-depth: 0)." >&2
    exit 2
fi

FILE="${1:-$(git rev-parse --show-toplevel)/CONTRIBUTORS}"
if [ ! -r "$FILE" ]; then
    echo "Error: cannot read $FILE." >&2
    exit 1
fi

lower() { tr '[:upper:]' '[:lower:]'; }

# The listed emails, one per line, lower-cased.
LINE_FORMAT='^.+ <[^<>@ ]+@[^<>@ ]+>$'
listed=""
bad=0
while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in '' | '#'*) continue ;; esac
    if ! [[ "$line" =~ $LINE_FORMAT ]]; then
        echo "Malformed line in $FILE (expected 'Name <email>'): $line" >&2
        bad=1
        continue
    fi
    email="${line##*<}"
    listed+="$(printf '%s' "${email%>}" | lower)"$'\n'
done < "$FILE"
[ "$bad" = 0 ] || exit 1

# The authors, minus bots. The bot suffix is matched LITERALLY: as a regex,
# `[bot]` would be a character class and miss `…dependabot[bot]@…`.
missing=""
count=0
while IFS= read -r author; do
    [ -n "$author" ] || continue
    email="${author##*<}"
    email="$(printf '%s' "${email%>}" | lower)"
    case "$email" in
        *'[bot]@users.noreply.github.com' | 'noreply@github.com') continue ;;
    esac
    count=$((count + 1))
    if ! printf '%s' "$listed" | grep -Fxq -- "$email"; then
        missing+="  $author"$'\n'
    fi
done < <(git log --no-merges --use-mailmap --format='%aN <%aE>' | sort -u)

if [ -n "$missing" ]; then
    {
        echo "Not listed in CONTRIBUTORS:"
        printf '%s' "$missing"
        echo "Add each line above to $FILE, keeping it sorted by name (see docs/development.md, \"Contributors\")."
    } >&2
    exit 1
fi
echo "CONTRIBUTORS lists all $count commit authors."
