# CycloneDX 1.6 JSON schema

`scripts/sbom-schema.test.mjs` validates every CycloneDX fixture under
`scripts/fixtures/` against these files (see `docs/development.md`, "The SBOM").
They are committed because the tests run offline.

They come from [CycloneDX/specification](https://github.com/CycloneDX/specification),
tag **1.6.2**, directory `schema/`, unchanged:

- `bom-1.6.schema.json`, the BOM schema;
- `spdx.schema.json` and `jsf-0.82.schema.json`, which it references.

    for f in bom-1.6.schema.json spdx.schema.json jsf-0.82.schema.json; do
      curl -sfL "https://raw.githubusercontent.com/CycloneDX/specification/1.6.2/schema/$f" \
        -o "compliance/cyclonedx-schema/$f"
    done

If the SBOMs move to a newer CycloneDX version, add that version's schema here
and switch the test to it. The test refuses a fixture of any other `specVersion`.
