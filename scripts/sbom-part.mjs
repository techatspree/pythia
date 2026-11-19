#!/usr/bin/env node
// Cuts ONE part out of Pythia's application SBOM (task-219), as a standalone
// CycloneDX document: the SBOM each container image carries in
// /licenses/pythia/sbom.cdx.json.
//
//   node scripts/sbom-part.mjs --sbom build/sbom/pythia.cdx.json \
//       --part pythia-backend|pythia-frontend --out <file>
//
// The result keeps bomFormat, specVersion and version; its metadata.component
// is the part itself (with its licence, without the nested components), its
// components are the part's nested components, and its dependencies are the
// edges whose ref lies inside the part, each pointing only at refs inside it.
// No timestamp and the input's order, so the same SBOM always gives the same
// bytes: the image build stays reproducible, and scripts/check-artifact-licenses.sh
// can compare an image's copy with a fresh cut to catch a stale one.
//
// Zero dependencies on purpose: it runs on the project's pinned Node.

import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname } from 'node:path';
import { pathToFileURL } from 'node:url';

/** The part named `name` of `bom` as a standalone CycloneDX document; throws for an unknown part. */
export function sbomPart(bom, name) {
	const part = (bom.components ?? []).find((c) => c.name === name);
	if (!part) {
		const known = (bom.components ?? []).map((c) => c.name).join(', ');
		throw new Error(`no part "${name}" in the SBOM (parts: ${known || 'none'})`);
	}
	const { components = [], ...self } = part;
	const inside = new Set([self['bom-ref'], ...components.map((c) => c['bom-ref'])]);
	const dependencies = (bom.dependencies ?? [])
		.filter((d) => inside.has(d.ref))
		.map((d) => ({ ref: d.ref, dependsOn: (d.dependsOn ?? []).filter((r) => inside.has(r)) }));
	return {
		bomFormat: bom.bomFormat,
		specVersion: bom.specVersion,
		version: bom.version,
		metadata: { component: self },
		components,
		dependencies
	};
}

/** The exact bytes written for a part: two-space JSON plus a trailing newline. */
export const serialise = (doc) => JSON.stringify(doc, null, 2) + '\n';

function parseArgs(argv) {
	const args = {};
	for (let i = 0; i < argv.length; i += 2) {
		if (!argv[i].startsWith('--') || argv[i + 1] === undefined) throw new Error(`bad argument: ${argv[i]}`);
		args[argv[i].slice(2)] = argv[i + 1];
	}
	for (const k of ['sbom', 'part', 'out']) {
		if (!args[k]) throw new Error(`missing --${k}`);
	}
	return args;
}

function main() {
	let args;
	let doc;
	try {
		args = parseArgs(process.argv.slice(2));
		doc = sbomPart(JSON.parse(readFileSync(args.sbom, 'utf8')), args.part);
	} catch (e) {
		console.error(`sbom-part: ${e.message}`);
		process.exit(2);
	}
	mkdirSync(dirname(args.out), { recursive: true });
	writeFileSync(args.out, serialise(doc));
	console.log(`Wrote ${args.out}: ${args.part}, ${doc.components.length} components`);
}

if (import.meta.url === pathToFileURL(process.argv[1] ?? '').href) main();
