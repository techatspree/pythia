// Unit tests for scripts/generate-third-party-licenses.mjs (task-208).
// Run: node --test scripts/generate-third-party-licenses.test.mjs
//
// The jars/klibs and the nested node_modules package are built in a temp dir at
// test time: the repository's .gitignore excludes `*.jar` and `node_modules/`.
import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { cpSync, existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { deflateRawSync } from 'node:zlib';
import { generate, readZip, readNoTextNames } from './generate-third-party-licenses.mjs';
import { licenseTree } from './check-oss-compliance.mjs';

const FIXTURES = fileURLToPath(new URL('./fixtures/third-party-licenses/', import.meta.url));
const SCRIPT = fileURLToPath(new URL('./generate-third-party-licenses.mjs', import.meta.url));
const ROOT = fileURLToPath(new URL('..', import.meta.url));
const read = (f) => JSON.parse(readFileSync(join(FIXTURES, f), 'utf8'));

// A minimal zip writer: `store` entries uncompressed, the rest deflated.
function zip(entries, store = false) {
	const locals = [];
	const central = [];
	let offset = 0;
	for (const [name, content] of Object.entries(entries)) {
		const raw = Buffer.from(content);
		const data = store ? raw : deflateRawSync(raw);
		const n = Buffer.from(name);
		const local = Buffer.alloc(30);
		local.writeUInt32LE(0x04034b50, 0);
		local.writeUInt16LE(store ? 0 : 8, 8);
		local.writeUInt32LE(data.length, 18);
		local.writeUInt32LE(raw.length, 22);
		local.writeUInt16LE(n.length, 26);
		const cen = Buffer.alloc(46);
		cen.writeUInt32LE(0x02014b50, 0);
		cen.writeUInt16LE(store ? 0 : 8, 10);
		cen.writeUInt32LE(data.length, 20);
		cen.writeUInt32LE(raw.length, 24);
		cen.writeUInt16LE(n.length, 28);
		cen.writeUInt32LE(offset, 42);
		locals.push(local, n, data);
		central.push(cen, n);
		offset += 30 + n.length + data.length;
	}
	const cd = Buffer.concat(central);
	const eocd = Buffer.alloc(22);
	eocd.writeUInt32LE(0x06054b50, 0);
	eocd.writeUInt16LE(Object.keys(entries).length, 8);
	eocd.writeUInt16LE(Object.keys(entries).length, 10);
	eocd.writeUInt32LE(cd.length, 12);
	eocd.writeUInt32LE(offset, 16);
	return Buffer.concat([...locals, cd, eocd]);
}

let work;
let jars;
let packages;
before(() => {
	work = mkdtempSync(join(tmpdir(), 'tpl-'));
	packages = join(work, 'packages');
	cpSync(join(FIXTURES, 'packages'), packages, { recursive: true });
	const inner = join(packages, '@scope/scoped/node_modules/inner');
	mkdirSync(inner, { recursive: true });
	writeFileSync(join(inner, 'package.json'), '{"name":"inner","version":"6.0.0"}\n');
	writeFileSync(join(inner, 'COPYING'), 'BSD 3-Clause\nCopyright (c) 2020 Inner Authors\n');
	const files = {
		'com.acme.lib-a-1.0.jar': zip({
			'META-INF/MANIFEST.MF': 'x',
			'META-INF/NOTICE': 'lib-a\nCopyright 2023 Acme Inc.\n',
			'org/acme/License.class': 'not a licence file'
		}),
		'com.acme.lib-b-2.0.jar': zip({ 'META-INF/MANIFEST.MF': 'x' }, true),
		'com.acme.relicensed-1.0.jar': zip({ 'META-INF/MANIFEST.MF': 'x' }),
		'lib-js-3.0.klib': zip({ 'default/manifest': 'x', 'META-INF/LICENSE': 'Apache License 2.0\nCopyright 2018 Kit Author\n' })
	};
	jars = Object.entries(files).map(([name, buf]) => {
		writeFileSync(join(work, name), buf);
		return join(work, name);
	});
});
after(() => rmSync(work, { recursive: true, force: true }));

const run = (part, overrides = {}) =>
	generate({
		bom: read('sbom.cdx.json'),
		part,
		policy: read('policy.json'),
		nodeModules: packages,
		jars,
		firstPartyGroup: 'io.pythia',
		licenseTexts: join(FIXTURES, 'license-texts'),
		...overrides
	});

/** The lines of one `## <id>` section, up to the next section. */
const section = (text, id) => {
	const lines = text.split('\n');
	const start = lines.indexOf(`## ${id}`);
	assert.ok(start >= 0, `no section ## ${id}`);
	const end = lines.findIndex((l, i) => i > start && l.startsWith('## '));
	return lines.slice(start, end < 0 ? undefined : end);
};
const count = (text, needle) => text.split(needle).length - 1;

test('the zip reader inflates deflated entries and reads stored ones', () => {
	const entries = readZip(zip({ 'a.txt': 'hello', 'b/c.txt': 'world' }));
	assert.equal(entries.get('a.txt')().toString(), 'hello');
	assert.equal(readZip(zip({ 'x': 'stored' }, true)).get('x')().toString(), 'stored');
	assert.throws(() => readZip(Buffer.from('not a zip at all, definitely not')), /not a zip/);
});

test('two Apache-2.0 components: ONE Apache text, both component lines', () => {
	const { text } = run('pythia-backend');
	const apache = section(text, 'Apache-2.0');
	assert.ok(apache.includes('  * com.acme/lib-a 1.0'));
	assert.ok(apache.includes('  * com.acme/lib-b 2.0'));
	assert.equal(count(text, 'You must give recipients a copy of this License.'), 1);
	assert.equal(count(text, '\n## Apache-2.0\n'), 1);
});

test('an npm MIT package whose LICENSE carries a copyright line: its file in the package-supplied section', () => {
	const { text } = run('pythia-frontend');
	const supplied = section(text, 'Package-supplied licence and notice files').join('\n');
	assert.match(supplied, /-- mit-own 1\.0\.0: LICENSE --\nMIT License\n\nCopyright \(c\) 2024 Jane Roe/);
	const mit = section(text, 'MIT');
	assert.notEqual(mit[mit.indexOf('  * mit-own 1.0.0') + 1], '      copyright holder not stated in the package');
});

test('an MIT package without a file: canonical text only, marked as missing a copyright holder', () => {
	const { text, summary } = run('pythia-frontend');
	const mit = section(text, 'MIT');
	const i = mit.indexOf('  * mit-bare 2.0.0');
	assert.ok(i >= 0, 'mit-bare listed under MIT (through the alias)');
	assert.equal(mit[i + 1], '      copyright holder not stated in the package');
	assert.ok(!text.includes('-- mit-bare 2.0.0'));
	// dual (MIT OR Apache-2.0, no file) is marked too: conservative, either side may be chosen
	assert.equal(mit[mit.indexOf('  * dual 4.0.0') + 1], '      copyright holder not stated in the package');
	assert.match(summary, /, 2 without a copyright holder$/);
});

test('a package LICENSE equal to the canonical text up to whitespace is not repeated', () => {
	const { text } = run('pythia-frontend');
	assert.ok(section(text, 'MIT').includes('  * mit-canonical 3.0.0'));
	assert.ok(!text.includes('-- mit-canonical 3.0.0'));
});

test('a jar carrying META-INF/NOTICE: the NOTICE is included; a .class named License is not a licence file', () => {
	const { text } = run('pythia-backend');
	assert.match(text, /-- com\.acme\/lib-a 1\.0: META-INF\/NOTICE --\nlib-a\nCopyright 2023 Acme Inc\.\n/);
	assert.ok(!text.includes('License.class'));
});

test('a klib carrying META-INF/LICENSE: found through the --jars list by name-version', () => {
	const { text } = run('pythia-frontend');
	assert.ok(section(text, 'Apache-2.0').includes('  * org.example/lib-js 3.0'));
	assert.match(text, /-- org\.example\/lib-js 3\.0: META-INF\/LICENSE --\nApache License 2\.0\nCopyright 2018 Kit Author/);
});

test('an `A OR B` expression puts the component under both groups', () => {
	const { text } = run('pythia-frontend');
	assert.ok(section(text, 'MIT').includes('  * dual 4.0.0'));
	assert.ok(section(text, 'Apache-2.0').includes('  * dual 4.0.0'));
});

test('a scoped package and a nested node_modules package are both found', () => {
	const { text } = run('pythia-frontend');
	assert.ok(section(text, 'Apache-2.0').includes('  * @scope/scoped 5.0.0'));
	assert.match(text, /-- inner 6\.0\.0: COPYING --\nBSD 3-Clause\nCopyright \(c\) 2020 Inner Authors/);
});

test('a first-party component goes under "Pythia itself", with no text', () => {
	const { text } = run('pythia-backend');
	const own = section(text, 'Pythia itself');
	assert.deepEqual(own, ['## Pythia itself', '  * io.pythia/core 1.0.0', 'See /licenses/pythia/LICENSE and /licenses/pythia/NOTICE.', '']);
	assert.ok(!section(text, 'Apache-2.0').includes('  * io.pythia/core 1.0.0'));
});

test("an exception's licence overrides the SBOM's name", () => {
	const { text } = run('pythia-backend');
	assert.ok(section(text, 'BSD-3-Clause').includes('  * com.acme/relicensed 1.0'));
	assert.ok(!text.includes('## Public Domain'));
});

test('a listed licence name without a text: its own group, with the note', () => {
	const bom = read('sbom.cdx.json');
	bom.components[0].components.push({
		group: 'com.acme', name: 'pd', version: '1.0', purl: 'pkg:maven/com.acme/pd@1.0?type=jar',
		licenses: [{ license: { name: 'Public Domain' } }]
	});
	writeFileSync(join(work, 'com.acme.pd-1.0.jar'), zip({ 'META-INF/MANIFEST.MF': 'x' }));
	const { text } = run('pythia-backend', { bom, jars: [...jars, join(work, 'com.acme.pd-1.0.jar')] });
	assert.deepEqual(section(text, 'Public Domain').slice(0, 4), [
		'## Public Domain', '  * com.acme/pd 1.0', '', 'no licence text exists for this licence'
	]);
});

test('the "no text" list is read only from its own README section', () => {
	assert.deepEqual([...readNoTextNames(join(FIXTURES, 'license-texts'))], ['Public Domain']);
});

test('a licence id with no text file fails, naming the id and the component', () => {
	const bom = read('sbom.cdx.json');
	bom.components[0].components[0].licenses = [{ license: { id: 'EPL-2.0' } }];
	assert.throws(() => run('pythia-backend', { bom }), /no licence text for "EPL-2\.0".*com\.acme\/lib-b 2\.0/);
});

test('an unlisted licence name fails like a missing id', () => {
	const bom = read('sbom.cdx.json');
	bom.components[0].components[0].licenses = [{ license: { name: 'Not On The List' } }];
	assert.throws(() => run('pythia-backend', { bom }), /no licence text for "Not On The List"/);
});

test('a component whose files cannot be found fails', () => {
	assert.throws(() => run('pythia-backend', { jars: jars.slice(1) }), /files not found for com\.acme\/lib-a 1\.0/);
	assert.throws(() => run('pythia-frontend', { nodeModules: join(work, 'nowhere') }), /files not found for mit-own 1\.0\.0/);
});

test('the summary line counts the third-party components', () => {
	assert.equal(
		run('pythia-backend').summary,
		'3 components: 0 with a package-supplied licence file, 3 on the canonical text only, 1 without a copyright holder'
	);
});

test('the same input twice gives byte-identical output; the CLI writes it and exits 1 on failure', () => {
	assert.equal(run('pythia-frontend').text, run('pythia-frontend').text);
	const list = join(work, 'jars.txt');
	writeFileSync(list, jars.join('\n') + '\n');
	const out = join(work, 'out', 'THIRD-PARTY-LICENSES.txt');
	const args = (part) => [
		SCRIPT, '--sbom', join(FIXTURES, 'sbom.cdx.json'), '--part', part, '--node-modules', packages, '--jars', list,
		'--first-party-group', 'io.pythia', '--license-texts', join(FIXTURES, 'license-texts'),
		'--policy', join(FIXTURES, 'policy.json'), '--out', out
	];
	const ok = spawnSync(process.execPath, args('pythia-frontend'), { encoding: 'utf8' });
	assert.equal(ok.status, 0, ok.stderr);
	assert.match(ok.stdout, /components: .* with a package-supplied licence file/);
	assert.equal(readFileSync(out, 'utf8'), run('pythia-frontend').text);
	const bad = spawnSync(process.execPath, args('no-such-part'), { encoding: 'utf8' });
	assert.equal(bad.status, 1);
	assert.match(bad.stderr, /no part "no-such-part"/);
});

// Over the REAL tree: the test that fails when a dependency bump brings a new licence.
test('every licence of the real SBOM has a committed text or a "no text" entry', (t) => {
	const sbom = join(ROOT, 'build/sbom/pythia.cdx.json');
	if (!existsSync(sbom)) return t.skip('build/sbom/pythia.cdx.json not built');
	const bom = JSON.parse(readFileSync(sbom, 'utf8'));
	const policy = JSON.parse(readFileSync(join(ROOT, 'compliance/oss-policy.json'), 'utf8'));
	const texts = join(ROOT, 'compliance/license-texts');
	const noText = readNoTextNames(texts);
	const exceptions = new Map((policy.exceptions ?? []).map((e) => [e.purl, e.license]));
	const leaves = (n, out = []) => {
		if (n.op) return leaves(n.right, leaves(n.left, out));
		out.push(n.unknownName !== undefined ? n.unknownName : n.with ? [n.id, n.with] : n.id);
		return out;
	};
	const missing = new Set();
	for (const part of bom.components) {
		for (const c of part.components ?? []) {
			if ((c.purl ?? '').startsWith('pkg:maven/io.pythia/')) continue;
			const tree = exceptions.has(c.purl)
				? licenseTree({ licenses: [{ expression: exceptions.get(c.purl) }] }, policy)
				: licenseTree(c, policy);
			for (const leaf of leaves(tree)) {
				const ids = Array.isArray(leaf) ? leaf : [leaf];
				const whole = Array.isArray(leaf) ? leaf.join(' WITH ') : leaf;
				if (noText.has(whole) || existsSync(join(texts, `${whole}.txt`))) continue;
				for (const id of ids) if (!existsSync(join(texts, `${id}.txt`))) missing.add(`${id} (${c.name})`);
			}
		}
	}
	assert.deepEqual([...missing], [], 'add compliance/license-texts/<id>.txt from SPDX v3.29.0');
});
