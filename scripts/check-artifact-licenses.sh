#!/usr/bin/env bash
# Checks that what Pythia ships carries Pythia's own licence (task-219):
#
#   ./scripts/check-artifact-licenses.sh --jar <app jar> \
#       --backend-image <ref> --frontend-image <ref> \
#       [--bundle <unpacked bundle dir>] [--root <dir, default: this repository>]
#
#   * the shipped app jar's manifest has SPDX-License-Identifier: Apache-2.0;
#   * each image has the OCI label org.opencontainers.image.licenses=Apache-2.0;
#   * each image has /licenses/pythia/{LICENSE,NOTICE,CONTRIBUTORS}, byte-identical
#     to <root>/<file>;
#   * each image has /licenses/pythia/sbom.cdx.json, byte-identical to a fresh
#     scripts/sbom-part.mjs cut of <root>/build/sbom/pythia.cdx.json, which
#     catches an image built from a stale staging;
#   * with --bundle, the bundle has the three files, identical too.
#
# One line per check; every failure is reported, not just the first. Images are
# read from the local Docker daemon. --root exists so a deliberate-failure test
# can point the comparison at altered copies.
set -euo pipefail

SCRIPTS="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPTS/.." && pwd)"
JAR="" BACKEND="" FRONTEND="" BUNDLE=""
FILES=(LICENSE NOTICE CONTRIBUTORS)

usage() {
    echo "Usage: $0 --jar <app jar> --backend-image <ref> --frontend-image <ref> [--bundle <dir>] [--root <dir>]" >&2
    exit 2
}

while [ $# -gt 0 ]; do
    [ $# -ge 2 ] || usage
    case "$1" in
        --jar) JAR="$2" ;;
        --backend-image) BACKEND="$2" ;;
        --frontend-image) FRONTEND="$2" ;;
        --bundle) BUNDLE="$2" ;;
        --root) ROOT="$(cd "$2" && pwd)" ;;
        *) usage ;;
    esac
    shift 2
done
[ -n "$JAR" ] && [ -n "$BACKEND" ] && [ -n "$FRONTEND" ] || usage

TMP="$(mktemp -d)"
CONTAINERS=()
cleanup() {
    for c in "${CONTAINERS[@]+"${CONTAINERS[@]}"}"; do docker rm -f "$c" > /dev/null 2>&1 || true; done
    rm -rf "$TMP"
}
trap cleanup EXIT

failures=0
ok() { echo "ok    $*"; }
fail() { echo "FAIL  $*"; failures=$((failures + 1)); }

# --- the shipped app jar -----------------------------------------------------
if [ -f "$JAR" ] && unzip -p "$JAR" META-INF/MANIFEST.MF 2> /dev/null | tr -d '\r' \
        | grep -qx 'SPDX-License-Identifier: Apache-2.0'; then
    ok "jar $JAR: SPDX-License-Identifier: Apache-2.0"
else
    fail "jar $JAR: no 'SPDX-License-Identifier: Apache-2.0' in META-INF/MANIFEST.MF"
fi

# --- each image ----------------------------------------------------------------
check_image() {
    local image="$1" part="$2"
    local label
    if ! label=$(docker image inspect -f '{{ index .Config.Labels "org.opencontainers.image.licenses" }}' "$image" 2> /dev/null); then
        fail "$image: no such image in the local Docker daemon"
        return
    fi
    if [ "$label" = "Apache-2.0" ]; then
        ok "$image: label org.opencontainers.image.licenses=Apache-2.0"
    else
        fail "$image: label org.opencontainers.image.licenses is '$label', not Apache-2.0"
    fi

    local c dir="$TMP/$part"
    # stderr: on an arm64 host Docker warns that the amd64 image's platform differs.
    c=$(docker create "$image" 2> /dev/null)
    CONTAINERS+=("$c")
    mkdir -p "$dir"
    for f in "${FILES[@]}"; do
        if docker cp "$c:/licenses/pythia/$f" "$dir/$f" > /dev/null 2>&1 && cmp -s "$dir/$f" "$ROOT/$f"; then
            ok "$image: /licenses/pythia/$f matches $f"
        else
            fail "$image: /licenses/pythia/$f is missing or differs from $ROOT/$f"
        fi
    done

    if ! node "$SCRIPTS/sbom-part.mjs" --sbom "$ROOT/build/sbom/pythia.cdx.json" --part "$part" \
            --out "$dir/expected.cdx.json" > /dev/null; then
        fail "$image: cannot cut $part out of $ROOT/build/sbom/pythia.cdx.json"
    elif docker cp "$c:/licenses/pythia/sbom.cdx.json" "$dir/sbom.cdx.json" > /dev/null 2>&1 \
            && cmp -s "$dir/sbom.cdx.json" "$dir/expected.cdx.json"; then
        ok "$image: /licenses/pythia/sbom.cdx.json is the current $part part"
    else
        fail "$image: /licenses/pythia/sbom.cdx.json is missing or not the current $part part (stale staging?)"
    fi
}

check_image "$BACKEND" pythia-backend
check_image "$FRONTEND" pythia-frontend

# --- the release bundle ----------------------------------------------------------
if [ -n "$BUNDLE" ]; then
    for f in "${FILES[@]}"; do
        if cmp -s "$BUNDLE/$f" "$ROOT/$f"; then
            ok "bundle $BUNDLE: $f matches"
        else
            fail "bundle $BUNDLE: $f is missing or differs from $ROOT/$f"
        fi
    done
fi

if [ "$failures" -gt 0 ]; then
    echo "$failures check(s) failed." >&2
    exit 1
fi
echo "All licence checks passed."
