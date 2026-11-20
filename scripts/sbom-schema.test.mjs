// Validates every CycloneDX document among the test fixtures under
// scripts/fixtures/ against the official CycloneDX 1.6 JSON schema.
// Run: node --test scripts/sbom-schema.test.mjs (part of :frontend:check).
//
// The fixtures are hand-written test input, and the scripts under test read
// only a few fields. A fixture can therefore be invalid CycloneDX (a component
// without its required `type`) while every test still passes. This test catches
// that. The schema files are committed in compliance/cyclonedx-schema/, from
// CycloneDX/specification tag 1.6.2. The validator is the frontend's pinned
// devDependency ajv (draft-07, the schema's dialect), resolved from
// src/frontend because scripts/ has no node_modules of its own.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, readdirSync } from 'node:fs';
import { createRequire } from 'node:module';
import { join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = fileURLToPath(new URL('..', import.meta.url));
const SCHEMAS = join(ROOT, 'compliance/cyclonedx-schema');
const require = createRequire(join(ROOT, 'src/frontend/package.json'));
const Ajv = require('ajv').default;
const addFormats = require('ajv-formats').default;

function validator() {
	const ajv = new Ajv({ strict: false, allErrors: true });
	addFormats(ajv);
	// ajv-formats has no IRI or IDN formats; accept any string, which is what ajv
	// does anyway for an unknown format, minus one warning per use.
	ajv.addFormat('iri-reference', true);
	ajv.addFormat('idn-email', true);
	for (const f of ['spdx.schema.json', 'jsf-0.82.schema.json']) {
		ajv.addSchema(JSON.parse(readFileSync(join(SCHEMAS, f), 'utf8')));
	}
	return ajv.compile(JSON.parse(readFileSync(join(SCHEMAS, 'bom-1.6.schema.json'), 'utf8')));
}
const validate = validator();

/** Every .json file under scripts/fixtures/ that declares bomFormat CycloneDX. */
function cycloneDxFixtures(dir = join(ROOT, 'scripts/fixtures')) {
	const out = [];
	for (const e of readdirSync(dir, { withFileTypes: true })) {
		const p = join(dir, e.name);
		if (e.isDirectory()) out.push(...cycloneDxFixtures(p));
		else if (e.name.endsWith('.json')) {
			const doc = JSON.parse(readFileSync(p, 'utf8'));
			if (doc.bomFormat === 'CycloneDX') out.push({ path: relative(ROOT, p), doc });
		}
	}
	return out.sort((a, b) => (a.path < b.path ? -1 : 1));
}

const errorsOf = (doc) =>
	validate(doc) ? [] : validate.errors.map((e) => `${e.instancePath || '/'} ${e.message}`);

const fixtures = cycloneDxFixtures();

test('the fixtures include CycloneDX documents at all', () => {
	assert.ok(fixtures.length >= 6, `found only ${fixtures.map((f) => f.path).join(', ')}`);
});

for (const { path, doc } of fixtures) {
	test(`${path} is valid CycloneDX ${doc.specVersion}`, () => {
		assert.equal(doc.specVersion, '1.6', 'only the 1.6 schema is committed');
		assert.deepEqual(errorsOf(doc), []);
	});
}

test('the schema check rejects a component without its required type', () => {
	const doc = {
		bomFormat: 'CycloneDX',
		specVersion: '1.6',
		version: 1,
		components: [{ name: 'no-type', version: '1.0' }]
	};
	assert.ok(errorsOf(doc).some((e) => e.includes("must have required property 'type'")), errorsOf(doc).join('\n'));
});
