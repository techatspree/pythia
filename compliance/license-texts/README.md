# Canonical licence texts (task-208)

`scripts/generate-third-party-licenses.mjs` writes
`/licenses/pythia/THIRD-PARTY-LICENSES.txt` into both images. Every licence
text in it comes from this directory, once per licence. That covers the many
packages that ship no licence file of their own: 127 of the 261 backend jars
and 4 of the 5 Kotlin/JS libraries, measured 2026-11-19. The build is offline,
so nothing is downloaded at build time. The texts are committed here instead.

## Source

Each `<id>.txt` is `text/<id>.txt` of
[spdx/license-list-data](https://github.com/spdx/license-list-data), tag
**v3.29.0**, unchanged:

    curl -sfL https://raw.githubusercontent.com/spdx/license-list-data/v3.29.0/text/<id>.txt \
      -o compliance/license-texts/<id>.txt

The file name is the SPDX id the SBOM uses, after the `aliases` of
`compliance/oss-policy.json` have been applied (and, for a component with an
`exceptions` entry, the `license` of that entry).

### Composed texts

- `GPL-2.0-with-classpath-exception.txt` is a deprecated id with no text of
  its own. It is `GPL-2.0-only.txt`, one blank line, then
  `Classpath-exception-2.0.txt`, both from v3.29.0.
- An expression `A WITH B` needs no file of its own: the generator joins
  `A.txt` and `B.txt` the same way.

## Adding a text

The build fails when a dependency brings a licence that has no file here.
`:frontend:generateThirdPartyLicenses` or
`:frontend:generateBackendThirdPartyLicenses` names the id and the components.
The unit test over the real SBOM in
`scripts/generate-third-party-licenses.test.mjs` fails the same way. To fix it:

1. Make sure `compliance/oss-policy.json` allows the licence. That is
   `checkOssCompliance`'s verdict, not this directory's.
2. Download the text with the command above, same tag, unchanged.
3. Rebuild. Never edit a text by hand, and never fetch one at build time.

## Licence names without a text

Some SBOM entries name a licence that has no canonical text, such as a bare
"Public Domain". The generator puts such a component in a group of its own,
with the note "no licence text exists for this licence". It recognises these
names ONLY from the list below, one name per line in the form ``- `<name>` ``.
Any other id or name without a file here fails the generator.

None today. The one candidate, "Public Domain" on
`org.jboss:jboss-transaction-spi:8.0.0.Final`, turned out to be
LGPL-2.1-or-later (task-208, see the exception in `compliance/oss-policy.json`).
