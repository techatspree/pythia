// Unit tests for scripts/sbom-part.mjs (task-219). Run: node --test scripts/sbom-part.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { execFileSync, spawnSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { sbomPart, serialise } from './sbom-part.mjs';

const FIXTURE = fileURLToPath(new URL('./fixtures/sbom-part/pythia.cdx.json', import.meta.url));
const SCRIPT = fileURLToPath(new URL('./sbom-part.mjs', import.meta.url));
const bom = () => JSON.parse(readFileSync(FIXTURE, 'utf8'));

test('the backend part: its own header, its components, its edges only', () => {
	const doc = sbomPart(bom(), 'pythia-backend');
	assert.deepEqual([doc.bomFormat, doc.specVersion, doc.version], ['CycloneDX', '1.6', 1]);
	assert.equal(doc.metadata.component.name, 'pythia-backend');
	assert.deepEqual(doc.metadata.component.licenses, [{ license: { id: 'Apache-2.0' } }]);
	assert.equal(doc.metadata.component.components, undefined);
	assert.deepEqual(doc.components.map((c) => c.name), ['lib-a', 'lib-b']);
	assert.deepEqual(doc.dependencies.map((d) => d.ref), [
		'pythia-backend',
		'pythia-backend:pkg:maven/com.acme/lib-a@1.0?type=jar',
		'pythia-backend:pkg:maven/com.acme/lib-b@2.0?type=jar'
	]);
});

test('the frontend part, and no edge leaves the part', () => {
	const doc = sbomPart(bom(), 'pythia-frontend');
	assert.deepEqual(doc.components.map((c) => c.name), ['svelte']);
	const refs = new Set(['pythia-frontend', 'pythia-frontend:pkg:npm/svelte@5.0.0']);
	for (const d of doc.dependencies) {
		assert.ok(refs.has(d.ref), d.ref);
		for (const r of d.dependsOn) assert.ok(refs.has(r), r);
	}
	assert.ok(!doc.dependencies.some((d) => d.ref === 'pythia'));
});

test('no timestamp in the part', () => {
	const doc = sbomPart(bom(), 'pythia-backend');
	assert.deepEqual(Object.keys(doc.metadata), ['component']);
	assert.ok(!serialise(doc).includes('2026-01-01'));
});

test('an unknown part throws, naming the parts that exist', () => {
	assert.throws(() => sbomPart(bom(), 'pythia-nope'), /no part "pythia-nope".*pythia-backend, pythia-frontend/);
});

test('CLI: two runs give byte-identical output; an unknown part exits non-zero', () => {
	const dir = mkdtempSync(join(tmpdir(), 'sbom-part-'));
	try {
		const out = (n) => join(dir, `${n}.json`);
		for (const n of ['a', 'b']) {
			execFileSync(process.execPath, [SCRIPT, '--sbom', FIXTURE, '--part', 'pythia-backend', '--out', out(n)]);
		}
		assert.ok(readFileSync(out('a')).equals(readFileSync(out('b'))));
		const bad = spawnSync(process.execPath, [SCRIPT, '--sbom', FIXTURE, '--part', 'pythia-nope', '--out', out('c')]);
		assert.notEqual(bad.status, 0);
		assert.match(bad.stderr.toString(), /no part "pythia-nope"/);
	} finally {
		rmSync(dir, { recursive: true, force: true });
	}
});
