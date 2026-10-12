#!/bin/bash

# Builds a release's deploy bundle (task-199):
#
#   ./scripts/release-bundle.sh <version> <image-base> <backend-digest> \
#                               <frontend-digest> <out-dir> [<git-ref>]
#
#   <version>       the release tag, e.g. v1.2.0
#   <image-base>    where the images live, e.g. ghcr.io/<owner>/pythia
#   <*-digest>      sha256:<64 hex> of each image's published multi-arch index
#   <out-dir>       receives pythia-deploy-<version>.tar.gz and its .sha256
#   <git-ref>       the content to package; defaults to <version>
#
# The bundle is everything a deployment needs and nothing else: the manifests,
# deploy.sh, smoke.sh, LICENSE/NOTICE, and release.env naming the release and
# pinning both images by digest. Its layout and release.env's keys are a public
# contract, documented in docs/deployment.md ("What a release publishes").
#
# Why `git archive` rather than tar over the working tree: it packages exactly
# the committed content of <git-ref>, never a dirty or untracked file, and it is
# byte-reproducible — git's built-in gzip writes no timestamp and every entry
# carries the commit time, so two runs over the same inputs give the same
# SHA-256. No GNU tar or other tool is involved.

# Refuse to be sourced: `set -euo pipefail` would leak into the caller's shell.
_sourced=0
if [ -n "${ZSH_EVAL_CONTEXT:-}" ]; then
    case "$ZSH_EVAL_CONTEXT" in *:file*) _sourced=1 ;; esac
elif [ -n "${BASH_SOURCE:-}" ]; then
    [ "${BASH_SOURCE[0]}" != "${0}" ] && _sourced=1
fi
if [ "$_sourced" = 1 ]; then
    echo "Error: do not 'source' this script — run it directly:" >&2
    echo "  ./scripts/release-bundle.sh <version> <image-base> <backend-digest> <frontend-digest> <out-dir> [<git-ref>]" >&2
    # SC2317: `return` succeeds when sourced (short-circuiting the `||`); the
    # `exit` only runs in the executed case. Dual-mode idiom, not dead code.
    # shellcheck disable=SC2317
    return 1 2>/dev/null || exit 1
fi
unset _sourced

set -euo pipefail

usage() {
    echo "Usage: ./scripts/release-bundle.sh <version> <image-base> <backend-digest> <frontend-digest> <out-dir> [<git-ref>]" >&2
    exit 1
}

[ $# -ge 5 ] && [ $# -le 6 ] || usage
V="$1"
BASE="$2"
BACKEND_DIGEST="$3"
FRONTEND_DIGEST="$4"
OUT="$5"
REF="${6:-$V}"

[[ "$V" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]] || { echo "Error: version '$V' is not v<MAJOR>.<MINOR>.<PATCH>." >&2; exit 1; }
[ -n "$BASE" ] || { echo "Error: the image base must not be empty." >&2; exit 1; }
for d in "$BACKEND_DIGEST" "$FRONTEND_DIGEST"; do
    [[ "$d" =~ ^sha256:[0-9a-f]{64}$ ]] || { echo "Error: '$d' is not a digest of the form sha256:<64 hex>." >&2; exit 1; }
done

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMMIT="$(git -C "$ROOT" rev-parse --verify "$REF^{commit}")"

# KEY=value lines only, no quoting, no comments: deploy.sh parses it.
ENV="PYTHIA_VERSION=$V
SOURCE_COMMIT=$COMMIT
BACKEND_IMAGE=$BASE/pythia-backend
BACKEND_DIGEST=$BACKEND_DIGEST
FRONTEND_IMAGE=$BASE/pythia-frontend
FRONTEND_DIGEST=$FRONTEND_DIGEST
"

NAME="pythia-deploy-$V"
mkdir -p "$OUT"
OUT="$(cd "$OUT" && pwd)"

git -C "$ROOT" archive --format=tar.gz --prefix="$NAME/" \
    --add-virtual-file="$NAME/release.env:$ENV" \
    -o "$OUT/$NAME.tar.gz" \
    "$COMMIT" -- LICENSE NOTICE scripts/deploy.sh scripts/smoke.sh \
    k8s/base k8s/overlays/staging k8s/overlays/production

# sha256sum format, so `sha256sum -c` works from <out-dir>. macOS has only shasum.
if command -v sha256sum >/dev/null 2>&1; then
    (cd "$OUT" && sha256sum "$NAME.tar.gz" > "$NAME.tar.gz.sha256")
else
    (cd "$OUT" && shasum -a 256 "$NAME.tar.gz" > "$NAME.tar.gz.sha256")
fi

echo "Wrote $OUT/$NAME.tar.gz ($(cut -d' ' -f1 < "$OUT/$NAME.tar.gz.sha256"))"
