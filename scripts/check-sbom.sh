#!/bin/bash

# Checks Pythia's application SBOM, build/sbom/pythia.cdx.json (task-206),
# against what the build actually ships. Run by `./gradlew sbom`.
#
#   ./scripts/check-sbom.sh [<sbom>]
#
# scripts/sbom-finalize.mjs builds the file; this script does not trust it:
#   - CycloneDX 1.6, root `pythia` with the Gradle version, exactly the two
#     parts `pythia-backend` and `pythia-frontend`, no serial, no timestamp;
#   - every bom-ref unique, every dependency reference resolvable;
#   - backend part: one component per jar in build/quarkus-app/lib and vice versa;
#   - frontend part: the bundle's packages and the Kotlin/JS libraries, and none
#     of the dev toolchain;
#   - our own components are Apache-2.0.

# Refuse to be sourced: `set -euo pipefail` would leak into the caller's shell.
_sourced=0
if [ -n "${ZSH_EVAL_CONTEXT:-}" ]; then
    case "$ZSH_EVAL_CONTEXT" in *:file*) _sourced=1 ;; esac
elif [ -n "${BASH_SOURCE:-}" ]; then
    [ "${BASH_SOURCE[0]}" != "${0}" ] && _sourced=1
fi
if [ "$_sourced" = 1 ]; then
    echo "Error: do not 'source' this script — run it directly:" >&2
    echo "  ./scripts/check-sbom.sh" >&2
    # SC2317: `return` succeeds when sourced (short-circuiting the `||`); the
    # `exit` only runs in the executed case. Dual-mode idiom, not dead code.
    # shellcheck disable=SC2317
    return 1 2>/dev/null || exit 1
fi
unset _sourced

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
S="${1:-$ROOT/build/sbom/pythia.cdx.json}"
LIB="$ROOT/src/backend/implementation/build/quarkus-app/lib"
VERSION="$(grep '^version=' "$ROOT/gradle.properties" | cut -d= -f2)"

fail() { echo "check-sbom: $*" >&2; exit 1; }

[ -f "$S" ] || fail "no SBOM at $S (run ./gradlew sbom)"

jq -e --arg v "$VERSION" '
    .bomFormat == "CycloneDX" and .specVersion == "1.6"
    and (.serialNumber == null) and (.metadata.timestamp == null)
    and .metadata.component.name == "pythia" and .metadata.component.version == $v
    and ([.components[].name] | sort) == ["pythia-backend", "pythia-frontend"]
' "$S" > /dev/null || fail "header: expected CycloneDX 1.6, root pythia $VERSION, parts pythia-backend + pythia-frontend, no serial or timestamp"

jq -e '[.. | objects | select(has("bom-ref")) | ."bom-ref"] | length == (unique | length)' "$S" > /dev/null \
    || fail "bom-refs are not unique"
jq -e '[.. | objects | select(has("bom-ref")) | ."bom-ref"] as $r
       | [.dependencies[] | .ref, (.dependsOn // [])[]] | all(. as $x | $r | index($x) != null)' "$S" > /dev/null \
    || fail "a dependency reference does not resolve to a component"

# Backend part ↔ shipped jars, by the same mapping the finalize step uses:
# group.name-version[-classifier].jar, our own modules as their -jvm variant.
backend_jars="$(jq -r '
    .components[] | select(.name == "pythia-backend") | .components[] | .purl
    | capture("^pkg:maven/(?<g>[^/]+)/(?<n>[^@]+)@(?<v>[^?]+)(\\?(?<q>.*))?$")
    | (.q // "" | capture("classifier=(?<c>[^&]+)") // {c: null}) as $cl
    | "\(.g).\(.n)\(if .g == "io.pythia" then "-jvm" else "" end)-\(.v)\(if $cl.c then "-\($cl.c)" else "" end)"
' "$S" | sort)"
shipped_jars="$(find "$LIB/boot" "$LIB/main" -name '*.jar' -exec basename {} .jar \; | sort)"
if [ "$backend_jars" != "$shipped_jars" ]; then
    echo "check-sbom: backend part and quarkus-app/lib differ:" >&2
    diff <(echo "$backend_jars") <(echo "$shipped_jars") | grep '^[<>]' | sed 's/^</  only in SBOM:/; s/^>/  only in lib: /' >&2
    exit 1
fi

frontend_names="$(jq -r '.components[] | select(.name == "pythia-frontend") | .components[]
                         | if .group then "\(.group)/\(.name)" else .name end' "$S")"
for p in svelte @sveltejs/kit @azure/msal-browser svelte-i18n svelte-dnd-action loglevel devalue \
         org.jetbrains.kotlin/kotlin-stdlib-js org.jetbrains.kotlin/kotlin-dom-api-compat \
         org.jetbrains.kotlinx/kotlinx-datetime-js org.jetbrains.kotlinx/kotlinx-serialization-core-js \
         io.github.oshai/kotlin-logging-js; do
    grep -qxF "$p" <<< "$frontend_names" || fail "frontend part lacks $p"
done
for p in eslint @playwright/test typescript lightningcss vite; do
    if grep -qxF "$p" <<< "$frontend_names"; then fail "frontend part contains dev tooling: $p"; fi
done
jq -e '[.components[] | select(.name == "pythia-frontend") | .components[].purl | select(test("type=pom"))] | length == 0' "$S" > /dev/null \
    || fail "frontend part contains a type=pom umbrella entry"

jq -e '[.metadata.component, .components[], (.. | objects | select((.purl? // "") | startswith("pkg:maven/io.pythia/")))]
       | all([.licenses[]?.license.id] | index("Apache-2.0") != null)' "$S" > /dev/null \
    || fail "pythia, its parts and every io.pythia component must be Apache-2.0"

echo "check-sbom: $S OK — $(jq '.components[0].components | length' "$S") backend + $(jq '.components[1].components | length' "$S") frontend components"
