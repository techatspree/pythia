#!/bin/bash
#
# Cut a Pythia release (task-193).
#
#   ./scripts/release.sh <X.Y.Z> [--next <A.B.C-SNAPSHOT>] [--skip-tests] [--dry-run] [--yes]
#
# The release model (docs/deployment.md, "Releasing"): the application version
# lives in ONE line, `version=` in the root gradle.properties. Between releases
# it is a -SNAPSHOT. A release is
#   1. a commit "Release X.Y.Z" that sets version=X.Y.Z (and carries the OpenAPI
#      client regenerated with that version),
#   2. the annotated tag vX.Y.Z on exactly that commit,
#   3. a commit "Start <next>" that moves main to the next -SNAPSHOT,
# pushed atomically: main and the tag land together or not at all.
#
# Tags are immutable — a mistaken push burns a version number forever — so this
# script refuses anything doubtful BEFORE it changes or pushes anything. CI's
# `release-tag` job re-checks the tag after the push and stays the authority.
#
# It never force-pushes and never moves or deletes a tag. main takes direct
# pushes today (the history has no merge commits); if main ever requires pull
# requests, this script has to open one instead of pushing.

# Refuse to be sourced: `set -euo pipefail` and the variables below would leak
# into your interactive shell. Detect sourcing under both bash and zsh and bail
# before anything else runs.
_sourced=0
if [ -n "${ZSH_EVAL_CONTEXT:-}" ]; then
    case "$ZSH_EVAL_CONTEXT" in *:file*) _sourced=1 ;; esac
elif [ -n "${BASH_SOURCE:-}" ]; then
    [ "${BASH_SOURCE[0]}" != "${0}" ] && _sourced=1
fi
if [ "$_sourced" = 1 ]; then
    echo "Error: do not 'source' this script — run it directly:" >&2
    echo "  ./scripts/release.sh <X.Y.Z>" >&2
    # SC2317: `return` succeeds when sourced (short-circuiting the `||`); the
    # `exit` only runs in the executed case. Dual-mode idiom, not dead code.
    # shellcheck disable=SC2317
    return 1 2>/dev/null || exit 1
fi
unset _sourced

# -E: the ERR trap below also fires for failures inside functions.
set -Eeuo pipefail

usage() {
    cat <<'EOF'
Usage: ./scripts/release.sh <X.Y.Z> [--next <A.B.C-SNAPSHOT>] [--skip-tests] [--dry-run] [--yes]

Cuts the release vX.Y.Z from the current main:
  - commit "Release X.Y.Z"  (version=X.Y.Z, OpenAPI client regenerated)
  - annotated tag vX.Y.Z on that commit
  - commit "Start <next>"   (version=<next>, default X.Y.(Z+1)-SNAPSHOT)
  - git push --atomic origin main refs/tags/vX.Y.Z

Options:
  --next <A.B.C-SNAPSHOT>  the SNAPSHOT version main continues with
  --skip-tests             build the release without running the tests
  --dry-run                run every check and print the plan; change nothing
  --yes                    do not ask for confirmation before pushing
  -h, --help               show this help

It refuses to run unless the working tree is clean, the current branch is main
and equal to origin/main, gradle.properties holds a -SNAPSHOT version not above
X.Y.Z, and vX.Y.Z is new and greater than every existing v* tag.
EOF
}

# Set once the script has created local commits or the tag; until the push
# succeeds, every failure explains how to discard them.
made_changes=0
tag=""
discard_hint() {
    if [ "$made_changes" = 1 ]; then
        echo >&2
        echo "Nothing was published. To discard the local release commits and tag:" >&2
        echo "  git reset --hard origin/main" >&2
        echo "  git tag -d $tag 2>/dev/null || true" >&2
        echo "then fix the problem and rerun ./scripts/release.sh ${tag#v}." >&2
    fi
}

# An explicit `exit` does not fire the ERR trap, so die prints the hint itself.
die() {
    echo "Error: $*" >&2
    discard_hint
    exit 1
}

SEMVER='^[0-9]+\.[0-9]+\.[0-9]+$'
SNAPSHOT='^[0-9]+\.[0-9]+\.[0-9]+-SNAPSHOT$'

version=""
next=""
skip_tests=0
dry_run=0
assume_yes=0
while [ $# -gt 0 ]; do
    case "$1" in
        -h|--help) usage; exit 0 ;;
        --next) [ $# -ge 2 ] || die "--next needs a value"; next="$2"; shift 2 ;;
        --skip-tests) skip_tests=1; shift ;;
        --dry-run) dry_run=1; shift ;;
        --yes) assume_yes=1; shift ;;
        -*) die "unknown option '$1' (see --help)" ;;
        *) [ -z "$version" ] || die "only one version may be given"; version="$1"; shift ;;
    esac
done

[ -n "$version" ] || { usage >&2; exit 1; }
[[ "$version" =~ $SEMVER ]] || die "'$version' is not a strict MAJOR.MINOR.PATCH version (no 'v', no suffix)"

# True when $1 < $2, comparing numerically per component (1.10.0 > 1.9.0).
version_lt() {
    [ "$1" != "$2" ] && [ "$(printf '%s\n%s\n' "$1" "$2" | sort -V | head -n1)" = "$1" ]
}

IFS=. read -r major minor patch <<<"$version"
next="${next:-$major.$minor.$((patch + 1))-SNAPSHOT}"
[[ "$next" =~ $SNAPSHOT ]] || die "--next '$next' is not MAJOR.MINOR.PATCH-SNAPSHOT"
version_lt "$version" "${next%-SNAPSHOT}" || die "--next $next must be greater than $version"

tag="v$version"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
PROPS=gradle.properties

current_version() {
    grep '^version=' "$PROPS" | cut -d= -f2
}

# --- Checks: nothing is changed until all of them pass. ---------------------

[ -z "$(git status --porcelain)" ] || die "the working tree is not clean; commit or stash first"
branch="$(git rev-parse --abbrev-ref HEAD)"
[ "$branch" = main ] || die "releases are cut from main, but the current branch is '$branch'"

echo "Fetching origin..."
git fetch --quiet origin --tags
[ "$(git rev-parse main)" = "$(git rev-parse origin/main)" ] \
    || die "main differs from origin/main; pull (or push) first, so the release is cut from what everyone sees"

cur="$(current_version)"
[ -n "$cur" ] || die "no 'version=' line in $PROPS"
[[ "$cur" =~ $SNAPSHOT ]] || die "$PROPS says version=$cur, which is not a -SNAPSHOT; a release starts from a SNAPSHOT"
base="${cur%-SNAPSHOT}"
version_lt "$version" "$base" && die "$version is below the current SNAPSHOT base $base"

git rev-parse -q --verify "refs/tags/$tag" >/dev/null && die "tag $tag already exists locally (tags are immutable; release the next version)"
[ -z "$(git ls-remote --tags origin "refs/tags/$tag")" ] || die "tag $tag already exists on origin (tags are immutable; release the next version)"

latest="$(git tag -l 'v*' | sed 's/^v//' | grep -E "$SEMVER" | sort -V | tail -n1 || true)"
if [ -n "$latest" ] && ! version_lt "$latest" "$version"; then
    die "$version is not greater than the newest release v$latest (releases are monotonic)"
fi

build_args=(build)
[ "$skip_tests" = 1 ] && build_args+=(-x test)

echo
echo "Release plan"
echo "  current version : $cur"
echo "  release         : $version  (tag $tag, previous release: ${latest:+v}${latest:-none})"
echo "  then main moves : $next"
echo "  release build   : ./gradlew ${build_args[*]}"
echo "  push            : git push --atomic origin main refs/tags/$tag"
if [ "$dry_run" = 1 ]; then
    echo
    echo "Dry run: every check passed; nothing was changed."
    exit 0
fi

# --- From here on, local changes exist until the push succeeds. -------------

trap discard_hint ERR

set_version() {
    local tmp
    tmp="$(mktemp)"
    awk -v v="$1" '/^version=/ { print "version=" v; next } { print }' "$PROPS" > "$tmp"
    mv "$tmp" "$PROPS"
}

# Files a release commit may change: the version line and the OpenAPI client the
# build regenerates from it. Anything else changing means the build is not what
# was committed, which must not be released silently.
RELEASE_FILES=("$PROPS" src/frontend/src/lib/api/openapi.json src/frontend/src/lib/api/schema.d.ts)
commit_release_files() {
    local unexpected
    unexpected="$(git status --porcelain | awk '{print $2}' | grep -vxF -f <(printf '%s\n' "${RELEASE_FILES[@]}") || true)"
    [ -z "$unexpected" ] || die "the build changed files other than the version and the OpenAPI client:
$unexpected"
    git add -- "${RELEASE_FILES[@]}"
    git commit --quiet -m "$1"
}

echo
echo "1/3  Release $version: setting version, building (this regenerates the OpenAPI client)..."
made_changes=1
set_version "$version"
./gradlew "${build_args[@]}"
commit_release_files "Release $version"
git tag -a "$tag" -m "Pythia $version"

echo "2/3  Moving main to $next..."
set_version "$next"
./gradlew build -x test
commit_release_files "Start $next"

echo
echo "Ready to publish:"
git --no-pager log --oneline --decorate -3
echo
echo "Changes since ${latest:+v}${latest:-the beginning}:"
if [ -n "$latest" ]; then
    git --no-pager log --oneline "v$latest..$tag"
else
    git --no-pager log --oneline -15 "$tag"
fi

if [ "$assume_yes" != 1 ]; then
    echo
    read -r -p "Push main and $tag to origin? [y/N] " answer
    case "$answer" in
        y|Y|yes|YES) ;;
        *) die "aborted before pushing" ;;
    esac
fi

echo "3/3  Pushing main and $tag atomically..."
if ! git push --atomic origin main "refs/tags/$tag"; then
    trap - ERR
    echo >&2
    echo "The push was rejected. --atomic refused BOTH refs, so nothing was published." >&2
    echo "Most likely someone pushed to main after this script fetched." >&2
    echo "Discard the local release and rerun:" >&2
    echo "  git reset --hard origin/main" >&2
    echo "  git tag -d $tag" >&2
    echo "  ./scripts/release.sh $version" >&2
    exit 1
fi
trap - ERR

echo
echo "Released $tag. CI now builds the tag and runs its release-tag checks."
echo "main continues as $next."
