#!/usr/bin/env bash
# Builds (or pushes) the backend image: the ONLY supported way (task-219).
#
#   ./scripts/build-backend-image.sh -Dquarkus.container-image.build=true
#   ./scripts/build-backend-image.sh -Dquarkus.container-image.push=true -Dquarkus.container-image.registry=… -x test
#
# Two Gradle invocations, because one cannot do it:
#   1. :backend:implementation:stageImageLicenses writes the image's
#      /licenses/pythia/ (LICENSE, NOTICE, CONTRIBUTORS and the image's SBOM part)
#      into src/main/jib/. The SBOM needs the jars of this project's quarkusBuild,
#      but the image is built INSIDE that same augmentation, so staging has to
#      happen first, in its own invocation.
#   2. The image build itself, with `quarkusAppPartsBuild --rerun`. Invocation 1
#      has just run the augmentation WITHOUT an image, and the container-image
#      properties are not among its inputs: without --rerun it would be
#      UP-TO-DATE, report BUILD SUCCESSFUL and build no image at all.
# Your arguments go to invocation 2 only. Credentials stay environment variables
# (QUARKUS_CONTAINER_IMAGE_USERNAME/_PASSWORD), never arguments.
#
# See docs/deployment.md, "How the images are built".
set -euo pipefail

usage() {
    echo "Usage: ./scripts/build-backend-image.sh [-D<property>=<value> | -x <task> | -q | --<gradle option>]..." >&2
    echo "Builds the backend image and nothing else: pass options, not task names." >&2
    exit 2
}

# Options only. A task name (":frontend:imageBuild", "build") would be run in the
# image invocation and silently widen what this script does. `-x` takes a task
# name as its value, which is allowed.
prev=""
for arg in "$@"; do
    if [ "$prev" != "-x" ] && [[ "$arg" == :* || "$arg" != -* ]]; then
        echo "Error: '$arg' is not an option." >&2
        usage
    fi
    prev="$arg"
done

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

./gradlew :backend:implementation:stageImageLicenses
./gradlew :backend:implementation:quarkusAppPartsBuild --rerun \
    :backend:implementation:imageBuild --no-build-cache "$@"
