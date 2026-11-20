#!/usr/bin/env node
// Generates THIRD-PARTY-LICENSES.txt for ONE part of Pythia's application SBOM
// (task-208), the attribution file each image carries in /licenses/pythia/.
//
//   node scripts/generate-third-party-licenses.mjs \
//       --sbom build/sbom/pythia.cdx.json --part pythia-frontend|pythia-backend \
//       [--node-modules <dir>] [--jars <file listing jar/klib paths, one per line>] \
//       --first-party-group io.pythia --license-texts compliance/license-texts \
//       --policy compliance/oss-policy.json --out <file>
//
// The file is grouped by licence, so each text appears once:
//   - `## <id>`: one `  * <group/>name <version>` line per component, then the
//     canonical text from --license-texts (committed from SPDX, see its README);
//   - `## Package-supplied licence and notice files`: each package's own
//     LICENSE/COPYING verbatim, unless it equals a canonical text of its groups
//     after whitespace normalisation, and every NOTICE verbatim (Apache-2.0 §4(d));
//   - `## Pythia itself`: the first-party components, pointing at our own files.
// Licences are normalised exactly as scripts/check-oss-compliance.mjs does
// (its licenseTree and the policy's aliases); a component with an `exceptions`
// entry takes that entry's `license`, which records the licence actually checked.
// Each component's files are looked up in --node-modules (npm) or --jars (Maven
// jars and Kotlin/JS klibs, both zips). A component whose files cannot be found,
// or a licence with neither a text file nor an entry on the README's "no text"
// list, fails the run. Sorted, no timestamps: the same input gives the same bytes.
//
// Zero dependencies on purpose: it runs on the project's pinned Node.

import { existsSync, mkdirSync, readFileSync, readdirSync, statSync, writeFileSync } from 'node:fs';
import { basename, dirname, join, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
import { inflateRawSync } from 'node:zlib';
import { licenseTree, parseExpression } from './check-oss-compliance.mjs';

// Licences whose conditions require the component's own copyright line, which
// the canonical text only has as a placeholder.
const NEEDS_COPYRIGHT_LINE = new Set(['MIT', 'BSD-2-Clause', 'BSD-3-Clause', 'ISC']);
const LICENSE_FILE = /^(licen[cs]e|copying)([._-][^/]*)?$/i;
const NOTICE_FILE = /^notice([._-][^/]*)?$/i;
const NO_TEXT_NOTE = 'no licence text exists for this licence';

const cmp = (a, b) => (a < b ? -1 : a > b ? 1 : 0);
const label = (c) => `${c.group ? `${c.group}/` : ''}${c.name} ${c.version}`;
const normalise = (s) => s.replace(/\s+/g, ' ').trim();
const withNewline = (s) => (s.endsWith('\n') ? s : `${s}\n`);

/** The entries of a zip (jar, klib): name -> () => Buffer. Central directory only; no ZIP64. */
export function readZip(buf) {
	let eocd = -1;
	for (let i = buf.length - 22; i >= Math.max(0, buf.length - 22 - 0xffff); i--) {
		if (buf.readUInt32LE(i) === 0x06054b50) {
			eocd = i;
			break;
		}
	}
	if (eocd < 0) throw new Error('not a zip file (no end of central directory)');
	const count = buf.readUInt16LE(eocd + 10);
	let p = buf.readUInt32LE(eocd + 16);
	if (p === 0xffffffff) throw new Error('ZIP64 is not supported');
	const entries = new Map();
	for (let n = 0; n < count; n++) {
		if (buf.readUInt32LE(p) !== 0x02014b50) throw new Error('corrupt central directory');
		const method = buf.readUInt16LE(p + 10);
		const size = buf.readUInt32LE(p + 20);
		const nameLen = buf.readUInt16LE(p + 28);
		const extraLen = buf.readUInt16LE(p + 30);
		const commentLen = buf.readUInt16LE(p + 32);
		const local = buf.readUInt32LE(p + 42);
		const name = buf.toString('utf8', p + 46, p + 46 + nameLen);
		entries.set(name, () => {
			const start = local + 30 + buf.readUInt16LE(local + 26) + buf.readUInt16LE(local + 28);
			const data = buf.subarray(start, start + size);
			if (method === 0) return Buffer.from(data);
			if (method === 8) return inflateRawSync(data);
			throw new Error(`${name}: unsupported compression method ${method}`);
		});
		p += 46 + nameLen + extraLen + commentLen;
	}
	return entries;
}

/** A zip's licence and notice files: at its root or anywhere under META-INF/. */
function zipFiles(path) {
	const out = [];
	for (const [name, read] of readZip(readFileSync(path))) {
		if (name.endsWith('/') || name.endsWith('.class')) continue;
		if (name.includes('/') && !name.startsWith('META-INF/')) continue;
		const base = basename(name);
		const kind = LICENSE_FILE.test(base) ? 'license' : NOTICE_FILE.test(base) ? 'notice' : null;
		if (kind) out.push({ path: name, kind, text: read().toString('utf8') });
	}
	return out;
}

/** A package directory's licence and notice files (top level only). */
function dirFiles(dir) {
	const out = [];
	for (const f of readdirSync(dir)) {
		const kind = LICENSE_FILE.test(f) ? 'license' : NOTICE_FILE.test(f) ? 'notice' : null;
		if (kind && statSync(join(dir, f)).isFile()) out.push({ path: f, kind, text: readFileSync(join(dir, f), 'utf8') });
	}
	return out;
}

/** Every package under a node_modules tree, nested ones included: "name@version" -> dir. */
export function indexNodeModules(root) {
	const index = new Map();
	const walk = (nm) => {
		if (!existsSync(nm)) return;
		for (const entry of readdirSync(nm)) {
			if (entry.startsWith('.')) continue;
			const dirs = entry.startsWith('@') ? readdirSync(join(nm, entry)).map((e) => join(nm, entry, e)) : [join(nm, entry)];
			for (const dir of dirs) {
				const pkg = join(dir, 'package.json');
				if (!existsSync(pkg)) continue;
				const { name, version } = JSON.parse(readFileSync(pkg, 'utf8'));
				const key = `${name}@${version}`;
				if (!index.has(key)) index.set(key, dir);
				walk(join(dir, 'node_modules'));
			}
		}
	};
	walk(root);
	return index;
}

/** The names on compliance/license-texts/README.md's "Licence names without a text" list. */
export function readNoTextNames(licenseTexts) {
	const readme = join(licenseTexts, 'README.md');
	if (!existsSync(readme)) return new Set();
	const section = readFileSync(readme, 'utf8').split(/^## Licence names without a text$/m)[1] ?? '';
	const body = section.split(/^## /m)[0];
	return new Set([...body.matchAll(/^- `([^`]+)`$/gm)].map((m) => m[1]));
}

/** The licence group keys of one tree: ids, `A WITH B`, or {name} for an unknown name. */
function groupsOf(node, out = []) {
	if (node.op) {
		groupsOf(node.left, out);
		groupsOf(node.right, out);
	} else if (node.unknownName !== undefined) out.push(node.unknownName || '(unnamed licence)');
	else out.push(node.with ? `${node.id} WITH ${node.with}` : node.id);
	return out;
}

/** The licence tree of a component: its exception's licence if it has one, else the SBOM's. */
function treeOf(component, policy) {
	const exception = (policy.exceptions ?? []).find((e) => e.purl && e.purl === component.purl);
	if (!exception) return licenseTree(component, policy);
	try {
		return parseExpression(exception.license);
	} catch {
		return { unknownName: exception.license };
	}
}

/** The canonical text of a group, null for a listed "no text" name, undefined when missing. */
function canonicalText(group, licenseTexts, noText) {
	if (noText.has(group)) return null;
	const file = (id) => join(licenseTexts, `${id}.txt`);
	const [id, exc] = group.split(' WITH ');
	if (existsSync(file(group))) return readFileSync(file(group), 'utf8');
	if (exc && existsSync(file(id)) && existsSync(file(exc))) {
		return `${withNewline(readFileSync(file(id), 'utf8'))}\n${readFileSync(file(exc), 'utf8')}`;
	}
	return undefined;
}

/**
 * Builds the file for one part. Returns {text, summary}; throws on a missing
 * licence text, an unknown part, or a component whose files cannot be found.
 */
export function generate({ bom, part, policy, nodeModules, jars = [], firstPartyGroup, licenseTexts }) {
	const self = (bom.components ?? []).find((c) => c.name === part);
	if (!self) throw new Error(`no part "${part}" in the SBOM`);
	const noText = readNoTextNames(licenseTexts);
	const npm = nodeModules ? indexNodeModules(nodeModules) : new Map();
	const jarByName = new Map(jars.map((j) => [basename(j).replace(/\.(jar|klib)$/, ''), j]));

	const groups = new Map();
	const firstParty = [];
	const supplied = [];
	const missingText = new Map();
	const notFound = [];
	const counts = { components: 0, withFile: 0, canonicalOnly: 0, noHolder: 0 };

	const components = [...(self.components ?? [])].sort((a, b) => cmp(label(a), label(b)));
	for (const c of components) {
		if (firstPartyGroup && c.group === firstPartyGroup) {
			firstParty.push(c);
			continue;
		}
		counts.components++;
		let files;
		if ((c.purl ?? '').startsWith('pkg:npm/')) {
			const dir = npm.get(`${c.group ? `${c.group}/` : ''}${c.name}@${c.version}`);
			if (!dir) notFound.push(`${label(c)} (not in --node-modules)`);
			files = dir ? dirFiles(dir) : [];
		} else {
			const names = [`${c.group}.${c.name}-${c.version}`, `${c.name}-${c.version}`];
			const jar = names.map((n) => jarByName.get(n)).find(Boolean);
			if (!jar) notFound.push(`${label(c)} (no ${names.join(' / ')} in --jars)`);
			files = jar ? zipFiles(jar) : [];
		}
		files.sort((a, b) => cmp(a.path, b.path));

		const tree = treeOf(c, policy);
		const ids = tree ? [...new Set(groupsOf(tree))] : ['(no licence declared)'];
		const texts = [];
		for (const id of ids) {
			const text = canonicalText(id, licenseTexts, noText);
			if (text === undefined) missingText.set(id, [...(missingText.get(id) ?? []), label(c)]);
			else if (text !== null) texts.push(normalise(text));
		}

		const ownLicense = files.some((f) => f.kind === 'license');
		const noHolder = !ownLicense && ids.some((id) => NEEDS_COPYRIGHT_LINE.has(id));
		if (ownLicense) counts.withFile++;
		else counts.canonicalOnly++;
		if (noHolder) counts.noHolder++;
		for (const id of ids) {
			if (!groups.has(id)) groups.set(id, []);
			groups.get(id).push({ c, noHolder });
		}
		for (const f of files) {
			if (f.kind === 'license' && texts.includes(normalise(f.text))) continue;
			supplied.push({ c, f });
		}
	}

	if (missingText.size || notFound.length) {
		const lines = [
			...[...missingText].map(
				([id, cs]) => `no licence text for "${id}" (add compliance/license-texts/${id}.txt, see its README): ${cs.join(', ')}`
			),
			...notFound.map((n) => `files not found for ${n}`)
		];
		throw new Error(lines.join('\n'));
	}

	const out = [`Third-party licences: ${part}`, "Generated from Pythia's SBOM. Do not edit.", ''];
	for (const id of [...groups.keys()].sort(cmp)) {
		out.push(`## ${id}`);
		for (const { c, noHolder } of groups.get(id)) {
			out.push(`  * ${label(c)}`);
			if (noHolder && NEEDS_COPYRIGHT_LINE.has(id)) out.push('      copyright holder not stated in the package');
		}
		out.push('');
		const text = canonicalText(id, licenseTexts, noText);
		out.push(text === null ? NO_TEXT_NOTE : withNewline(text).trimEnd(), '');
	}
	out.push('## Package-supplied licence and notice files');
	for (const { c, f } of supplied) {
		out.push(`-- ${label(c)}: ${f.path} --`, withNewline(f.text).trimEnd(), '');
	}
	if (!supplied.length) out.push('');
	out.push('## Pythia itself');
	for (const c of firstParty) out.push(`  * ${label(c)}`);
	out.push('See /licenses/pythia/LICENSE and /licenses/pythia/NOTICE.', '');

	const summary =
		`${counts.components} components: ${counts.withFile} with a package-supplied licence file, ` +
		`${counts.canonicalOnly} on the canonical text only, ${counts.noHolder} without a copyright holder`;
	return { text: out.join('\n'), summary };
}

function parseArgs(argv) {
	const args = {};
	for (let i = 0; i < argv.length; i += 2) {
		if (!argv[i].startsWith('--') || argv[i + 1] === undefined) throw new Error(`bad argument: ${argv[i]}`);
		args[argv[i].slice(2)] = argv[i + 1];
	}
	for (const k of ['sbom', 'part', 'license-texts', 'policy', 'out']) {
		if (!args[k]) throw new Error(`missing --${k}`);
	}
	return args;
}

function main() {
	let args;
	let result;
	try {
		args = parseArgs(process.argv.slice(2));
		const read = (f) => JSON.parse(readFileSync(f, 'utf8'));
		const jars = args.jars
			? readFileSync(args.jars, 'utf8')
					.split('\n')
					.map((l) => l.trim())
					.filter(Boolean)
					.map((l) => resolve(dirname(args.jars), l))
			: [];
		result = generate({
			bom: read(args.sbom),
			part: args.part,
			policy: read(args.policy),
			nodeModules: args['node-modules'],
			jars,
			firstPartyGroup: args['first-party-group'],
			licenseTexts: args['license-texts']
		});
	} catch (e) {
		console.error(`generate-third-party-licenses: ${e.message}`);
		process.exit(1);
	}
	mkdirSync(dirname(args.out), { recursive: true });
	writeFileSync(args.out, result.text);
	console.log(`Wrote ${args.out}: ${result.summary}`);
}

if (import.meta.url === pathToFileURL(process.argv[1] ?? '').href) main();
