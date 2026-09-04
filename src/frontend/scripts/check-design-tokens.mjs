// Design-token gate (task-149).
//
// Fails the build when a Tailwind *arbitrary colour value* — `bg-[#007a45]`,
// `text-[#abc]`, … — appears in component source. Those bypass the `@theme`
// token layer in `src/app.css`, which is how one brand shade ended up retyped
// as an unnamed literal at 38 sites.
//
// Unlike detekt/ESLint here, this gate is NOT informational: it exits non-zero.
// A token rule that only warns is a rule that decays, and it is enforceable at
// all only because task-149 first brought the count to zero.
//
// `.svg` is deliberately excluded: SVG `fill`/`stroke` cannot reference a
// Tailwind class, so the literal hexes in `src/lib/assets/logo.svg` are correct
// and must not be flagged.

import { readdirSync, readFileSync, statSync } from 'node:fs';
import { dirname, join, extname } from 'node:path';
import { fileURLToPath } from 'node:url';

// Resolve relative to THIS FILE, never the working directory: Gradle runs the
// script with the :frontend project dir as cwd while a manual run happens from
// the repo root. A cwd-relative root would silently scan nothing — and a gate
// that scans nothing still exits 0 and looks healthy.
const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const SRC = join(root, 'src');

const SCANNED = new Set(['.svelte', '.ts', '.html', '.css']);
const ARBITRARY_COLOUR = /-\[#[0-9a-fA-F]{3,8}\]/;

// task-168, rule 2: a raw disclosure-triangle glyph. The pair lives in
// `$lib/ui/DisclosureTriangle.svelte` and nowhere else — retyped at seven sites
// it had already drifted into two different Unicode pairs (full-size U+25BC/B6
// at five sites, the visibly lighter SMALL variants U+25BE/B8 at two).
const TRIANGLE = /[\u25B6\u25B7\u25BC\u25BD\u25B8\u25BE]/;
const TRIANGLE_HOME = 'src/lib/ui/DisclosureTriangle.svelte';

// task-168, rule 3: a raw neutral palette class. The PREFIX ANCHOR is required,
// not stylistic: `.css` is scanned and `app.css` defines the tokens as
// `var(--color-gray-600)`, which a bare `-gray-\d+` pattern would flag — the
// gate would reject the very definitions it exists to enforce.
const RAW_NEUTRAL = /\b(text|bg|border|ring|fill|stroke|divide|placeholder)-gray-[0-9]{2,3}\b/;

function walk(dir, out = []) {
	for (const entry of readdirSync(dir)) {
		const full = join(dir, entry);
		if (statSync(full).isDirectory()) walk(full, out);
		else if (SCANNED.has(extname(entry))) out.push(full);
	}
	return out;
}

const colours = [];
const triangles = [];
const neutrals = [];
for (const file of walk(SRC)) {
	const rel = file.slice(root.length + 1);
	const isTriangleHome = rel === TRIANGLE_HOME;
	readFileSync(file, 'utf8')
		.split('\n')
		.forEach((line, i) => {
			const where = `${rel}:${i + 1}: ${line.trim()}`;
			if (ARBITRARY_COLOUR.test(line)) colours.push(where);
			if (!isTriangleHome && TRIANGLE.test(line)) triangles.push(where);
			if (RAW_NEUTRAL.test(line)) neutrals.push(where);
		});
}

// Each rule names its FIX, not just its violation: that is what made the
// original colour rule actionable rather than merely noisy.
const failures = [
	[colours, 'arbitrary colour value(s)', 'add a token to the @theme block in src/app.css and use it instead'],
	[triangles, 'raw disclosure-triangle glyph(s)', 'use $lib/ui/DisclosureTriangle'],
	[neutrals, 'raw neutral palette class(es)', 'use an ink-*/surface-*/hairline token from @theme']
];

let failed = false;
for (const [found, what, fix] of failures) {
	if (found.length === 0) continue;
	failed = true;
	console.error(`\n✖ ${found.length} ${what} found — ${fix}:\n`);
	for (const o of found) console.error(`  ${o}`);
}
if (failed) {
	console.error('');
	process.exit(1);
}

console.log(
	`✓ design tokens: no arbitrary colours, no raw triangles, no raw neutrals ` +
		`(${walk(SRC).length} files scanned)`
);
