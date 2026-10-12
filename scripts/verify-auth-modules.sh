#!/bin/bash

# Regression guard for task-194, shared by CI's `build` and `publish` jobs
# (task-199): a DEFAULT production augmentation (no APP_AUTH_PROVIDER at build
# time) must contain the Entra auth module and never the forgeable dev one.
# ArC generates a `<Class>_Bean` only for beans that survive build-time gating,
# so the generated-bytecode jar of the augmentation is the proof. Before
# task-194 a default build contained NEITHER module and still started,
# authenticating nobody.
#
#   ./scripts/verify-auth-modules.sh [<generated-bytecode.jar>]

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
J="${1:-$ROOT/src/backend/implementation/build/quarkus-app/quarkus/generated-bytecode.jar}"

[ -f "$J" ] || { echo "no augmentation output at $J" >&2; exit 1; }
# List once, then grep the text. `unzip -l | grep -q` under pipefail is a race:
# grep exits at the first match, unzip dies of SIGPIPE mid-listing, and the
# pipeline reports failure for a jar that is fine.
listing="$(unzip -l "$J")"
grep -q 'io/pythia/auth/entra/EntraAuthModule_Bean.class' <<< "$listing" \
    || { echo "default build lacks EntraAuthModule_Bean" >&2; exit 1; }
if grep -q 'io/pythia/auth/dev/DevAuthModule_Bean.class' <<< "$listing"; then
    echo "default build contains the forgeable DevAuthModule_Bean" >&2
    exit 1
fi
echo "Default build: Entra present, dev absent."
