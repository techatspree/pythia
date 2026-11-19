# Open-source compliance

Pythia is licensed under Apache-2.0 and distributed as binaries: two container
images, the JavaScript bundle inside the frontend image, and a release's deploy
bundle. Every third-party library in those binaries is distributed by us too, so
its license has to allow that. This page explains which licenses are acceptable,
how the build checks it, and what to do when the check fails.

The policy itself is [`compliance/oss-policy.json`](../compliance/oss-policy.json).
It is the specification; this page explains it. Where the two disagree, the JSON
file is right and this page is out of date.

## What is checked

`./gradlew build` runs `:frontend:checkOssCompliance`, which reads the
application SBOM `build/sbom/pythia.cdx.json` (see
[Development](development.md), "The SBOM") and checks every component in it
against the policy. **The check blocks**: a component that fails it fails the
build. `:frontend:testOssCompliance` unit-tests the checker itself
(`scripts/check-oss-compliance.mjs`).

To run it on its own:

```bash
./gradlew :frontend:checkOssCompliance        # builds the SBOM first
node scripts/check-oss-compliance.mjs          # on an SBOM that already exists
```

It prints every *conditional* component with the obligation we accept for it,
one warning per exception still pending review, one line per failure, and a
summary. It is deliberately not part of `./gradlew staticAnalysis`: reading the
SBOM needs the whole application built, and that task is a fast lint pass.

## The three categories

Every license the policy knows is in exactly one category, each with its reason
in the JSON file.

- **Allowed** — permissive licenses (Apache-2.0, MIT, the BSD licenses, ISC,
  CC0-1.0, …). Distributing them inside an Apache-2.0 product is fine; what
  they ask for is attribution (the license text and notices), which the
  release's attribution material covers.
- **Conditional** — weak copyleft (EPL, MPL-2.0, LGPL, CDDL, GPL-2.0 with the
  Classpath exception). Acceptable for an **unmodified** third-party library
  shipped as its own component — a jar or a package, not code copied into ours.
  Their source is available upstream. If we ever modify such a library, the
  modified source has to be published under its license; that would need a new
  decision, not just a passing check. The check lists every conditional
  component so these obligations stay visible.
- **Forbidden** — strong or network copyleft (GPL without the Classpath
  exception, AGPL) and non-open licenses (SSPL, BUSL, non-commercial or
  share-alike Creative Commons). Shipping one would put the combined work under
  terms incompatible with Apache-2.0.

**A license in no category fails as unknown.** The policy is a closed
allow-list, not a deny-list: a new license is unacceptable until someone has
classified it with a reason.

`GPL-2.0-with-classpath-exception` (a deprecated SPDX id, which the SBOM uses
today) and `GPL-2.0-only WITH Classpath-exception-2.0` (its current spelling)
are the same license; both are listed as conditional so a change of spelling
cannot fail the build.

## How a component is judged

For every component except Pythia's own application components:

1. A purl starting with one of the `firstParty` prefixes (`pkg:maven/io.pythia/`)
   is our own code and passes.
2. An `exceptions` entry with exactly the component's purl decides it, and it
   passes. An entry marked `pendingReview` passes with a warning.
3. Otherwise the component's license is read from the SBOM — an SPDX id, an
   SPDX expression, or a license name mapped to an SPDX id through `aliases`.
   - `A OR B` passes if either side does: dual licensing lets us choose.
   - `A AND B` passes only if both do.
   - `A WITH <exception>` is judged as `A`, unless the combined form is listed.
     An exception only adds permissions, so this is the cautious reading.
   - **Several license entries on one component count as AND.** CycloneDX does
     not say whether such a list is a choice, so only an explicit `OR` is one.
     Many Vert.x and Jakarta libraries list two licenses this way; all of them
     pass under this reading.
4. Allowed passes; conditional passes and is listed; forbidden, unknown, or no
   license at all fails.

The policy is checked too. An exception whose purl matches no shipped component
(it was removed or upgraded) fails as stale, as does an entry without a reason
and a license listed in two categories. Otherwise a removed dependency would
leave a permanent hole in the policy.

## When the check fails

Do not edit the check. Pick the case that applies and change
`compliance/oss-policy.json` in a pull request, which is reviewed like code:

- **The license is fine but not listed** — add it to the right category with
  its reason.
- **The SBOM names it in a way the policy does not know** (a non-SPDX name such
  as "The Apache Software License, Version 2.0") — add an `aliases` entry. Only
  for a name that unambiguously means one SPDX license; anything that needs
  judgement ("Public Domain") is an exception instead.
- **This one component is acceptable although its metadata says otherwise**
  (wrong or missing metadata upstream, or a license reviewed individually) —
  add an `exceptions` entry with the exact purl, version included, the license
  it really has, and the reason. An exception names one version, so an upgrade
  makes it stale and forces a new look.
- **The license is forbidden** — the dependency cannot ship. Replace it, or
  remove what pulls it in. Never park it as an exception.

`pendingReview: true` marks an exception that is recorded but not yet judged;
the build passes but warns. It exists for the first introduction of the check,
not for new dependencies.

## Pythia's own licence in the artifacts

Apache-2.0 asks every redistribution to give its recipients a copy of the
licence and to carry the NOTICE file, and a tool that scans an artifact needs
machine-readable licence information on the artifact itself. Since task-219
every published artifact carries it:

| artifact | where it carries Pythia's licence |
|---|---|
| deploy bundle `pythia-deploy-vX.Y.Z.tar.gz` | `LICENSE`, `NOTICE`, `CONTRIBUTORS` at its top level |
| release SBOM `pythia-vX.Y.Z.cdx.json` | `Apache-2.0` on the root component and on both parts |
| backend image `pythia/pythia-backend` | label `org.opencontainers.image.licenses=Apache-2.0`; `/licenses/pythia/` with `LICENSE`, `NOTICE`, `CONTRIBUTORS` and `sbom.cdx.json` (the image's part of the release SBOM) |
| frontend image `pythia/pythia-frontend` | the same label and the same `/licenses/pythia/`, as an image layer outside the web root (not served) |
| backend app jar (`quarkus-app/app/implementation-*.jar`, the jar that runs) | `SPDX-License-Identifier: Apache-2.0` in its manifest, from `quarkus.package.jar.manifest.attributes` |

**One place in both images: `/licenses/pythia/`.** `/licenses/` is the Red Hat
UBI convention for licence files (the backend's base is UBI 9, and Red Hat's
certification tooling looks there), so the one directory a tool does look at by
name is the one used. All later licence material goes there too: task-208's
`THIRD-PARTY-LICENSES.txt`.

**Why no label points at that directory.** No OCI annotation names a licence
path: `org.opencontainers.image.licenses` is the standard key, and its value is
an SPDX expression, not a path. Scanners such as Trivy and Syft find licences in
package metadata and in SBOMs, not in a directory a label names. A custom label
would be read by no tool, so the location is documented here instead. The SBOM
is what tools actually use, and for the frontend it is the ONLY source: the
minified bundle has no `package.json` or `node_modules` left. That is why each
image carries its SBOM part. Attaching the SBOM to the images in the registry,
where scanners discover it without pulling, is task-220.

**The gate.** CI's `images` job runs `scripts/check-artifact-licenses.sh` on the
images it has just built. It checks the jar's manifest, each image's label, the
three files (byte-identical to the repository root), and each image's
`sbom.cdx.json` against a fresh `scripts/sbom-part.mjs` cut of the build's SBOM,
which catches an image built from a stale staging. With `--bundle <dir>` it also
checks an unpacked deploy bundle. How the backend image gets its directory, and
why that takes two Gradle invocations, is in [Deployment](deployment.md), "How
the images are built".

## What this does not cover

- **The base images' operating-system packages.** The images also contain the
  packages of their base images (UBI 9 for the backend, Alpine for the
  frontend), including GPL tools such as busybox and coreutils. Those are
  separate programs shipped side by side with ours (aggregation), we do not
  build them, and a gate on them would break whenever a base image moves. They
  are reviewed by hand and documented, not gated.
- **Attribution.** Shipping the license texts and notices that the allowed and
  conditional licenses require is separate work, not this check.
- **Development tools.** Only what ships is in the SBOM; build and test tools
  (Gradle plugins, ESLint, Playwright, …) are not distributed and not checked.
