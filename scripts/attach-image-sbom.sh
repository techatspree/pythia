#!/bin/bash

# Attach an image's SBOM part to the image as an OCI referrer (task-220).
#
#   ./scripts/attach-image-sbom.sh [--plain-http] <image-ref> <sbom.cdx.json>
#
# <image-ref> is registry/repo:tag or registry/repo@sha256:…, and must be a
# multi-arch index with linux/amd64 and linux/arm64. The SBOM is attached to the
# index AND to both platform manifests, because a scanner resolves a tag to one
# platform's manifest and asks for THAT digest's referrers, while
# `oras discover` on the tag sees the index.
#
# Before attaching, the file is proven byte-identical to the copy the image
# carries at /licenses/pythia/sbom.cdx.json (task-219), so the referrer can
# never disagree with the image. Re-running is safe: a subject that already
# has a CycloneDX referrer with the same blob is skipped, so nothing piles up.
#
# Credentials come from `oras login` and `docker login` done by the caller,
# never as arguments. --plain-http is for a local throwaway registry.
#
# Registries without the OCI 1.1 referrers API (GHCR, registry:2) get oras'
# standard fallback, the referrers tag schema: a `sha256-<hex>` tag per subject.
# Those tags ARE the referrers there; do not delete them.

# Refuse to be sourced: `set -euo pipefail` and the trap would leak into the
# caller's shell.
_sourced=0
if [ -n "${ZSH_EVAL_CONTEXT:-}" ]; then
    case "$ZSH_EVAL_CONTEXT" in *:file*) _sourced=1 ;; esac
elif [ -n "${BASH_SOURCE:-}" ]; then
    [ "${BASH_SOURCE[0]}" != "${0}" ] && _sourced=1
fi
if [ "$_sourced" = 1 ]; then
    echo "Error: do not 'source' this script — run it directly:" >&2
    echo "  ./scripts/attach-image-sbom.sh [--plain-http] <image-ref> <sbom.cdx.json>" >&2
    # SC2317: `return` succeeds when sourced (short-circuiting the `||`); the
    # `exit` only runs in the executed case. Dual-mode idiom, not dead code.
    # shellcheck disable=SC2317
    return 1 2>/dev/null || exit 1
fi
unset _sourced

set -euo pipefail

ARTIFACT_TYPE=application/vnd.cyclonedx+json
IN_IMAGE_PATH=/licenses/pythia/sbom.cdx.json

usage() {
    echo "Usage: ./scripts/attach-image-sbom.sh [--plain-http] <image-ref> <sbom.cdx.json>" >&2
    exit 1
}

ORAS_FLAGS=()
if [ "${1:-}" = "--plain-http" ]; then
    ORAS_FLAGS+=(--plain-http)
    shift
fi
[ "$#" -eq 2 ] || usage
IMAGE_REF="$1"
SBOM="$2"

# 0. The file, before contacting any registry.
if [ ! -f "$SBOM" ]; then
    echo "Error: SBOM file not found: $SBOM" >&2
    exit 1
fi
if ! jq -e '.bomFormat == "CycloneDX"' "$SBOM" > /dev/null 2>&1; then
    echo "Error: $SBOM is not a CycloneDX JSON document." >&2
    exit 1
fi
if command -v sha256sum > /dev/null; then
    BLOB="sha256:$(sha256sum < "$SBOM" | cut -d' ' -f1)"
else
    BLOB="sha256:$(shasum -a 256 < "$SBOM" | cut -d' ' -f1)"
fi

# registry/repo without :tag or @digest. A tag's colon comes after the last '/';
# a registry port's colon does not.
REPO="${IMAGE_REF%@*}"
case "${REPO##*/}" in *:*) REPO="${REPO%:*}" ;; esac

TMP="$(mktemp -d)"
CONTAINER=""
cleanup() {
    if [ -n "$CONTAINER" ]; then docker rm "$CONTAINER" > /dev/null 2>&1 || true; fi
    rm -rf "$TMP"
}
trap cleanup EXIT

# 1. The bytes must be the ones the image carries.
docker pull -q "$IMAGE_REF" > /dev/null
CONTAINER="$(docker create "$IMAGE_REF")"
if ! docker cp "$CONTAINER:$IN_IMAGE_PATH" "$TMP/in-image.cdx.json" > /dev/null 2>&1; then
    echo "Error: $IMAGE_REF has no $IN_IMAGE_PATH." >&2
    exit 1
fi
if ! cmp -s "$SBOM" "$TMP/in-image.cdx.json"; then
    echo "Error: $SBOM differs from the copy inside the image ($IMAGE_REF:$IN_IMAGE_PATH)." >&2
    exit 1
fi

# 2. The subjects: the index and both platform manifests.
INDEX_DIGEST="$(oras resolve ${ORAS_FLAGS[@]+"${ORAS_FLAGS[@]}"} "$IMAGE_REF")"
oras manifest fetch ${ORAS_FLAGS[@]+"${ORAS_FLAGS[@]}"} "$REPO@$INDEX_DIGEST" > "$TMP/index.json"
if ! jq -e '.manifests | type == "array"' "$TMP/index.json" > /dev/null; then
    echo "Error: $IMAGE_REF is not a multi-arch index (expected linux/amd64 and linux/arm64)." >&2
    exit 1
fi
SUBJECTS=("$INDEX_DIGEST")
for platform in linux/amd64 linux/arm64; do
    d="$(jq -r --arg p "$platform" \
        '[.manifests[] | select("\(.platform.os)/\(.platform.architecture)" == $p) | .digest][0] // empty' \
        "$TMP/index.json")"
    if [ -z "$d" ]; then
        echo "Error: the index of $IMAGE_REF has no $platform manifest." >&2
        exit 1
    fi
    SUBJECTS+=("$d")
done

# Whether <digest> already has a CycloneDX referrer carrying our blob. `oras
# discover` lists referrer descriptors without their layers, so each referrer's
# manifest is fetched and its layers compared.
has_sbom() {
    local subject="$1" ref
    for ref in $(oras discover ${ORAS_FLAGS[@]+"${ORAS_FLAGS[@]}"} --format json --artifact-type "$ARTIFACT_TYPE" \
            "$REPO@$subject" | jq -r '.referrers[]?.digest'); do
        if oras manifest fetch ${ORAS_FLAGS[@]+"${ORAS_FLAGS[@]}"} "$REPO@$ref" \
                | jq -e --arg b "$BLOB" 'any(.layers[]?; .digest == $b)' > /dev/null; then
            return 0
        fi
    done
    return 1
}

# 3. Attach under a clean name: oras records the file name as the layer title,
# which is the name a consumer gets when pulling the referrer.
NAME="${REPO##*/}.cdx.json"
cp "$SBOM" "$TMP/$NAME"
declare -a STATE=()
for subject in "${SUBJECTS[@]}"; do
    if has_sbom "$subject"; then
        STATE+=("already present")
    else
        (cd "$TMP" && oras attach ${ORAS_FLAGS[@]+"${ORAS_FLAGS[@]}"} --artifact-type "$ARTIFACT_TYPE" \
            "$REPO@$subject" "$NAME:$ARTIFACT_TYPE" > /dev/null)
        STATE+=("attached")
    fi
done

# 4. Verify every subject now carries it.
missing=0
for i in "${!SUBJECTS[@]}"; do
    subject="${SUBJECTS[$i]}"
    if has_sbom "$subject"; then
        echo "${STATE[$i]}: $REPO@$subject"
    else
        echo "MISSING: $REPO@$subject has no CycloneDX referrer with $BLOB" >&2
        missing=1
    fi
done
# Conditional on purpose: shellcheck (0.11) treats an unconditional `exit` as
# the script's last command as making the EXIT trap unreachable, and would then
# report `cleanup` as never invoked (SC2329).
if [ "$missing" -ne 0 ]; then exit 1; fi
