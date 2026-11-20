#!/usr/bin/env node
// Combines task-208's two generated THIRD-PARTY-LICENSES.txt files into the ONE
// web copy nginx serves at /THIRD-PARTY-LICENSES.txt (task-210). The
// "Open-Source-Lizenzen" dialog lists the server's components as well as the
// web interface's, so its "full license texts" link must cover both.
//
//   node scripts/combine-third-party-licenses.mjs --frontend <file> --backend <file> --out <file>
//
// Each part gets a fixed English header underlined with `=`, then its file
// verbatim (byte-identical to the image's /licenses/pythia/ copy); frontend
// first, one blank line between. No timestamp: same input, same bytes.
//
// Zero dependencies on purpose: it runs on the project's pinned Node.

import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname } from 'node:path';
import { pathToFileURL } from 'node:url';

const HEADERS = {
	frontend: 'Web interface (pythia-frontend)',
	backend: 'Server (pythia-backend)'
};

/** The combined text of the two parts' files. */
export function combine({ frontend, backend }) {
	const section = (header, text) => `${header}\n${'='.repeat(header.length)}\n\n${text.endsWith('\n') ? text : `${text}\n`}`;
	return `${section(HEADERS.frontend, frontend)}\n${section(HEADERS.backend, backend)}`;
}

function parseArgs(argv) {
	const args = {};
	for (let i = 0; i < argv.length; i += 2) {
		if (!argv[i].startsWith('--') || argv[i + 1] === undefined) throw new Error(`bad argument: ${argv[i]}`);
		args[argv[i].slice(2)] = argv[i + 1];
	}
	for (const k of ['frontend', 'backend', 'out']) {
		if (!args[k]) throw new Error(`missing --${k}`);
	}
	return args;
}

function main() {
	let args;
	let text;
	try {
		args = parseArgs(process.argv.slice(2));
		text = combine({
			frontend: readFileSync(args.frontend, 'utf8'),
			backend: readFileSync(args.backend, 'utf8')
		});
	} catch (e) {
		console.error(`combine-third-party-licenses: ${e.message}`);
		process.exit(1);
	}
	mkdirSync(dirname(args.out), { recursive: true });
	writeFileSync(args.out, text);
	console.log(`Wrote ${args.out}`);
}

if (import.meta.url === pathToFileURL(process.argv[1] ?? '').href) main();
