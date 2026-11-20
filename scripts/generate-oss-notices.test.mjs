// Unit tests for scripts/generate-oss-notices.mjs (task-210).
// Run: node --test scripts/generate-oss-notices.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { generate } from './generate-oss-notices.mjs';

const SBOM = fileURLToPath(new URL('./fixtures/oss-notices/sbom.cdx.json', import.meta.url));
const SCRIPT = fileURLToPath(new URL('./generate-oss-notices.mjs', import.meta.url));
const FIRST_PARTY = ['pkg:maven/io.pythia/'];
const bom = () => JSON.parse(readFileSync(SBOM, 'utf8'));
const run = (b = bom()) => generate(b, FIRST_PARTY);
const part = (n, name) => n.parts.find((p) => p.part === name);
const comp = (n, name) => n.parts.flatMap((p) => p.components).find((c) => c.name === name);

function cli(sbomText) {
	const dir = mkdtempSync(join(tmpdir(), 'oss-notices-'));
	try {
		const sbom = join(dir, 'sbom.json');
		const out = join(dir, 'out.json');
		writeFileSync(sbom, sbomText);
		const r = spawnSync(process.execPath, [SCRIPT, '--sbom', sbom, '--out', out], { encoding: 'utf8' });
		let text = null;
		try {
			text = readFileSync(out, 'utf8');
		} catch {
			// no output written
		}
		return { status: r.status, stdout: r.stdout, stderr: r.stderr, text };
	} finally {
		rmSync(dir, { recursive: true, force: true });
	}
}

test('two parts, frontend first', () => {
	assert.deepEqual(run().parts.map((p) => p.part), ['frontend', 'backend']);
});

test('names: npm with and without group, Maven group:name', () => {
	const n = run();
	assert.deepEqual(
		part(n, 'frontend').components.map((c) => c.name),
		['@azure/msal-browser', 'org.jetbrains.kotlin:kotlin-stdlib-js', 'svelte']
	);
	assert.ok(comp(n, 'org.postgresql:postgresql'));
});

test('nested components are walked, first-party ones excluded', () => {
	const n = run();
	assert.ok(comp(n, 'com.acme:nested'), 'a component nested below another is listed');
	const names = n.parts.flatMap((p) => p.components.map((c) => c.name));
	assert.ok(!names.some((x) => x.startsWith('io.pythia')));
	assert.deepEqual(part(n, 'backend').components.map((c) => c.name), ['com.acme:nested', 'com.acme:twice', 'org.postgresql:postgresql']);
});

test('licenses by id, by name and by expression', () => {
	const n = run();
	assert.deepEqual(comp(n, 'svelte').licenses, ['MIT']);
	assert.deepEqual(comp(n, 'com.acme:nested').licenses, ['The Nested License']);
	assert.deepEqual(comp(n, 'com.acme:twice').licenses, ['MIT', 'MIT OR Apache-2.0']);
});

test('two versions of one name are merged into one entry with the union', () => {
	const twice = run().parts[1].components.filter((c) => c.name === 'com.acme:twice');
	assert.equal(twice.length, 1);
	assert.deepEqual(twice[0].copyright, ['Copyright 2020 Acme', 'Copyright 2024 Acme Two']);
	assert.deepEqual(twice[0].authors, ['Acme Supplies']);
	assert.equal(twice[0].homepage, 'https://acme.example.com');
});

test('copyright split on newlines, [] without; authors from author/authors/publisher/supplier, deduplicated', () => {
	const n = run();
	assert.deepEqual(comp(n, 'org.postgresql:postgresql').copyright, [
		'Copyright (c) 1997, PostgreSQL Global Development Group',
		'Copyright (c) 2017 OnGres, Inc.'
	]);
	assert.deepEqual(comp(n, 'org.jetbrains.kotlin:kotlin-stdlib-js').copyright, []);
	assert.deepEqual(comp(n, 'org.jetbrains.kotlin:kotlin-stdlib-js').authors, []);
	assert.deepEqual(comp(n, '@azure/msal-browser').authors, ['Microsoft', 'MSAL Team']);
	assert.deepEqual(comp(n, 'org.postgresql:postgresql').authors, ['PostgreSQL Global Development Group']);
});

test('homepage: website, else vcs with git+ stripped, only https', () => {
	const n = run();
	assert.equal(comp(n, 'svelte').homepage, 'https://github.com/sveltejs/svelte.git');
	// an http:// website does not qualify; the https vcs does
	assert.equal(comp(n, 'org.postgresql:postgresql').homepage, 'https://github.com/pgjdbc/pgjdbc');
	assert.equal(comp(n, 'com.acme:nested').homepage, null);
});

test('field order and no versions, purls or hashes', () => {
	const { text } = cli(readFileSync(SBOM, 'utf8'));
	assert.deepEqual(Object.keys(JSON.parse(text).parts[0].components[0]), ['name', 'licenses', 'copyright', 'authors', 'homepage']);
	assert.ok(!text.includes('"version"'));
	assert.ok(!text.includes('pkg:'));
	assert.ok(!text.includes('9.9.9') && !text.includes('42.7.0'));
	assert.ok(text.endsWith('}\n'));
});

test('a component without a licence exits 1 and writes nothing', () => {
	const b = bom();
	delete b.components[1].components[0].licenses;
	const r = cli(JSON.stringify(b));
	assert.equal(r.status, 1);
	assert.match(r.stderr, /svelte \(pythia-frontend\) has no licence/);
	assert.equal(r.text, null);
});

test('a missing part exits 1', () => {
	const b = bom();
	b.components = b.components.filter((c) => c.name !== 'pythia-backend');
	const r = cli(JSON.stringify(b));
	assert.equal(r.status, 1);
	assert.match(r.stderr, /no part "pythia-backend"/);
});

test('deterministic: two runs, same bytes; the count is logged', () => {
	const a = cli(readFileSync(SBOM, 'utf8'));
	const b = cli(readFileSync(SBOM, 'utf8'));
	assert.equal(a.status, 0);
	assert.equal(a.text, b.text);
	assert.match(a.stdout, /3 frontend \+ 3 backend components/);
});
