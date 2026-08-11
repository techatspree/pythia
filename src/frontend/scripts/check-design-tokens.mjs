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

function walk(dir, out = []) {
	for (const entry of readdirSync(dir)) {
		const full = join(dir, entry);
		if (statSync(full).isDirectory()) walk(full, out);
		else if (SCANNED.has(extname(entry))) out.push(full);
	}
	return out;
}

const offenders = [];
for (const file of walk(SRC)) {
	readFileSync(file, 'utf8')
		.split('\n')
		.forEach((line, i) => {
			if (ARBITRARY_COLOUR.test(line)) {
				offenders.push(`${file.slice(root.length + 1)}:${i + 1}: ${line.trim()}`);
			}
		});
}

if (offenders.length > 0) {
	console.error(
		`\n✖ ${offenders.length} arbitrary colour value(s) found. Add a token to the ` +
			`@theme block in src/app.css and use it instead:\n`
	);
	for (const o of offenders) console.error(`  ${o}`);
	console.error('');
	process.exit(1);
}

console.log(`✓ design tokens: no arbitrary colour values (${walk(SRC).length} files scanned)`);
