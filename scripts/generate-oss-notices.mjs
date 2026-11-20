#!/usr/bin/env node
// Projects Pythia's application SBOM onto the list the "Open-Source-Lizenzen"
// dialog shows (task-210):
//
//   node scripts/generate-oss-notices.mjs --sbom build/sbom/pythia.cdx.json --out <file> \
//       [--policy compliance/oss-policy.json]
//
// It reads ONLY the finalized SBOM: the dialog shows what the SBOM says. The
// copyright holders got there through scripts/sbom-finalize.mjs.
//
// Output, third-party-notices.json:
//   { "parts": [ { "part": "frontend", "components": [...] },
//                { "part": "backend",  "components": [...] } ] }
// each component { name, licenses, copyright, authors, homepage }:
//   - name: npm `@group/name` (or `name`), Maven `group:name`;
//   - licenses: each entry's license.id, else license.name, else the expression;
//     never empty (a component without one fails the run);
//   - copyright: the SBOM's `copyright` split on newlines, or [];
//   - authors: author, authors[].name, publisher, supplier.name, or [];
//   - homepage: the `website` reference, else the `vcs` one with `git+` stripped,
//     the first of them that is https://, else null.
// A name that occurs more than once (two versions) is listed once, with the
// union. First-party components (the policy's `firstParty` purl prefixes) and
// the application components are left out.
//
// Deliberately NO versions, purls or hashes: nginx serves this file publicly,
// and the deployed dependency versions are not published over HTTP (a
// fingerprint of the server). Sorted, no timestamp: same SBOM, same bytes.
//
// Zero dependencies on purpose: it runs on the project's pinned Node.

import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const PARTS = [
	['frontend', 'pythia-frontend'],
	['backend', 'pythia-backend']
];
const DEFAULT_POLICY = fileURLToPath(new URL('../compliance/oss-policy.json', import.meta.url));

const cmp = (a, b) => (a < b ? -1 : a > b ? 1 : 0);
const union = (...lists) => [...new Set(lists.flat().filter(Boolean))];

/** Every component below a node, any depth. */
function descendants(node, out = []) {
	for (const c of node.components ?? []) {
		out.push(c);
		descendants(c, out);
	}
	return out;
}

function displayName(c) {
	if (!c.group) return c.name;
	return (c.purl ?? '').startsWith('pkg:npm/') ? `${c.group}/${c.name}` : `${c.group}:${c.name}`;
}

function licensesOf(c) {
	return union((c.licenses ?? []).map((l) => l.license?.id ?? l.license?.name ?? l.expression));
}

function homepageOf(c) {
	const refs = c.externalReferences ?? [];
	const candidates = [
		...refs.filter((r) => r.type === 'website').map((r) => r.url),
		...refs.filter((r) => r.type === 'vcs').map((r) => (r.url ?? '').replace(/^git\+/, ''))
	];
	return candidates.find((u) => /^https:\/\//.test(u ?? '')) ?? null;
}

/** The pure core: SBOM + first-party purl prefixes in, the notices object out. */
export function generate(bom, firstParty) {
	const isFirstParty = (c) => firstParty.some((p) => (c.purl ?? '').startsWith(p));
	const parts = PARTS.map(([part, sbomName]) => {
		const node = (bom.components ?? []).find((c) => c.name === sbomName);
		if (!node) throw new Error(`no part "${sbomName}" in the SBOM`);
		const byName = new Map();
		for (const c of descendants(node)) {
			if (c.type === 'application' || isFirstParty(c)) continue;
			const name = displayName(c);
			const licenses = licensesOf(c);
			if (!licenses.length) throw new Error(`${name} (${sbomName}) has no licence in the SBOM`);
			const entry = {
				name,
				licenses,
				copyright: c.copyright ? c.copyright.split('\n') : [],
				authors: union(c.author, (c.authors ?? []).map((a) => a.name), c.publisher, c.supplier?.name),
				homepage: homepageOf(c)
			};
			const seen = byName.get(name);
			if (!seen) byName.set(name, entry);
			else {
				seen.licenses = union(seen.licenses, entry.licenses);
				seen.copyright = union(seen.copyright, entry.copyright);
				seen.authors = union(seen.authors, entry.authors);
				seen.homepage ??= entry.homepage;
			}
		}
		const components = [...byName.values()].sort(
			(a, b) => cmp(a.name, b.name) || cmp(a.licenses.join(), b.licenses.join())
		);
		return { part, components };
	});
	return { parts };
}

function parseArgs(argv) {
	const args = {};
	for (let i = 0; i < argv.length; i += 2) {
		if (!argv[i].startsWith('--') || argv[i + 1] === undefined) throw new Error(`bad argument: ${argv[i]}`);
		args[argv[i].slice(2)] = argv[i + 1];
	}
	for (const k of ['sbom', 'out']) {
		if (!args[k]) throw new Error(`missing --${k}`);
	}
	return args;
}

function main() {
	let args;
	let notices;
	try {
		args = parseArgs(process.argv.slice(2));
		const read = (f) => JSON.parse(readFileSync(f, 'utf8'));
		notices = generate(read(args.sbom), read(args.policy ?? DEFAULT_POLICY).firstParty ?? []);
	} catch (e) {
		console.error(`generate-oss-notices: ${e.message}`);
		process.exit(1);
	}
	mkdirSync(dirname(args.out), { recursive: true });
	writeFileSync(args.out, JSON.stringify(notices, null, 2) + '\n');
	const [fe, be] = notices.parts;
	console.log(`Wrote ${args.out}: ${fe.components.length} frontend + ${be.components.length} backend components`);
}

if (import.meta.url === pathToFileURL(process.argv[1] ?? '').href) main();
