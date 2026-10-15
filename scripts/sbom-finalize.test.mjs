// Unit tests for scripts/sbom-finalize.mjs (task-206). Run: node --test scripts/sbom-finalize.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { finalize, parsePurl } from './sbom-finalize.mjs';

const fixture = (name) =>
	JSON.parse(readFileSync(new URL(`./fixtures/sbom/${name}.json`, import.meta.url), 'utf8'));

// The jars "Quarkus packaged" for the backend fixture: lib-a, the -jvm half of
// the umbrella, and our own module as its -jvm variant. `stripped` has no jar.
const JARS = ['com.acme.lib-a-1.0', 'com.acme.umbrella-jvm-2.0', 'io.pythia.core-jvm-9.9.9'];

const run = (overrides = {}) =>
	finalize({
		backend: fixture('backend'),
		frontend: fixture('frontend'),
		domain: fixture('domain'),
		jars: JARS,
		version: '9.9.9',
		...overrides
	});

const part = (bom, name) => bom.components.find((c) => c.name === name);
const purls = (p) => p.components.map((c) => c.purl);
const deps = (bom, ref) => bom.dependencies.find((d) => d.ref === ref)?.dependsOn;

test('parsePurl splits group, name, version and qualifiers', () => {
	const p = parsePurl('pkg:npm/%40azure/msal-browser@5.0.0?vcs_url=x');
	assert.deepEqual([p.type, p.group, p.name, p.version, p.qualifiers.vcs_url], ['npm', '@azure', 'msal-browser', '5.0.0', 'x']);
	assert.equal(parsePurl('pkg:maven/a/b@1?type=pom').qualifiers.type, 'pom');
});

test('root and parts: names, version, Apache-2.0, no serial, no timestamp', () => {
	const { bom, errors } = run();
	assert.deepEqual(errors, []);
	assert.equal(bom.metadata.component.name, 'pythia');
	assert.equal(bom.metadata.component.version, '9.9.9');
	assert.deepEqual(bom.components.map((c) => c.name), ['pythia-backend', 'pythia-frontend']);
	for (const c of [bom.metadata.component, ...bom.components]) {
		assert.equal(c.licenses[0].license.id, 'Apache-2.0');
		assert.equal(c.type, 'application');
	}
	assert.equal(bom.serialNumber, undefined);
	assert.equal(bom.metadata.timestamp, undefined);
	assert.deepEqual(deps(bom, 'pythia'), ['pythia-backend', 'pythia-frontend']);
});

test('backend: a type=pom entry is dropped and reported', () => {
	const { bom, dropped } = run();
	assert.ok(!purls(part(bom, 'pythia-backend')).some((p) => p.includes('type=pom')));
	assert.ok(dropped.some((d) => d.purl.includes('umbrella@2.0') && d.why.includes('type=pom')));
});

test('backend: a component without a jar is dropped and reported', () => {
	const { bom, dropped } = run();
	assert.ok(!purls(part(bom, 'pythia-backend')).some((p) => p.includes('stripped')));
	assert.ok(dropped.some((d) => d.purl.includes('stripped') && d.why.includes('not packaged')));
});

test('backend: our own module matches its -jvm jar and gets Apache-2.0', () => {
	const { bom } = run();
	const core = part(bom, 'pythia-backend').components.find((c) => c.name === 'core');
	assert.ok(core, 'io.pythia:core must match io.pythia.core-jvm-9.9.9.jar');
	assert.equal(core.licenses[0].license.id, 'Apache-2.0');
});

test('backend: a jar no component describes is an error', () => {
	const { errors } = run({ jars: [...JARS, 'com.evil.undeclared-6.6.6'] });
	assert.equal(errors.length, 1);
	assert.match(errors[0], /com\.evil\.undeclared-6\.6\.6\.jar ships but no SBOM component/);
});

test('graph: an edge through a dropped node is reconnected, edges to dropped leaves are pruned', () => {
	const { bom } = run();
	const be = deps(bom, 'pythia-backend');
	// implementation -> umbrella(pom) -> umbrella-jvm becomes pythia-backend -> umbrella-jvm
	assert.ok(be.includes('pythia-backend:pkg:maven/com.acme/umbrella-jvm@2.0?type=jar'));
	assert.ok(!be.some((r) => r.includes('umbrella@2.0') || r.includes('stripped')));
});

test('frontend: Kotlin/JS merged, pom umbrella dropped, -js klib kept, own modules Apache-2.0', () => {
	const { bom, dropped } = run();
	const fe = part(bom, 'pythia-frontend');
	const names = fe.components.map((c) => c.name).sort();
	assert.deepEqual(names, ['core', 'domain', 'kotlin-stdlib-js', 'msal-browser', 'svelte']);
	assert.ok(dropped.some((d) => d.part === 'pythia-frontend' && d.purl.includes('kotlin-stdlib@2.4.10?type=pom')));
	for (const c of fe.components.filter((c) => c.group === 'io.pythia')) {
		assert.equal(c.licenses[0].license.id, 'Apache-2.0');
	}
	// The SPA depends on the domain module as well as on its npm packages.
	const feDeps = deps(bom, 'pythia-frontend');
	assert.ok(feDeps.some((r) => r.includes('io.pythia/domain')));
	assert.ok(feDeps.includes('pythia-frontend:pkg:npm/svelte@5.0.0'));
});

test('the same purl in both parts gets two distinct bom-refs, and the graph resolves', () => {
	const { bom } = run();
	const refs = [];
	const walk = (o) => {
		if (Array.isArray(o)) return o.forEach(walk);
		if (o && typeof o === 'object') {
			if ('bom-ref' in o) refs.push(o['bom-ref']);
			Object.values(o).forEach(walk);
		}
	};
	walk(bom);
	assert.equal(new Set(refs).size, refs.length, 'bom-refs must be unique');
	const corePurl = 'pkg:maven/io.pythia/core@9.9.9?project_path=%3Adomain%3Acore';
	assert.ok(refs.includes(`pythia-backend:${corePurl}`));
	assert.ok(refs.includes(`pythia-frontend:${corePurl}`));
	for (const d of bom.dependencies) {
		for (const r of [d.ref, ...d.dependsOn]) assert.ok(refs.includes(r), `unresolved ref ${r}`);
	}
});

test('output is deterministic', () => {
	assert.equal(JSON.stringify(run().bom), JSON.stringify(run().bom));
});
