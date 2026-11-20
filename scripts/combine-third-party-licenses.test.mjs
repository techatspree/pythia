// Unit tests for scripts/combine-third-party-licenses.mjs (task-210).
// Run: node --test scripts/combine-third-party-licenses.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const FIXTURES = fileURLToPath(new URL('./fixtures/combine-third-party-licenses/', import.meta.url));
const SCRIPT = fileURLToPath(new URL('./combine-third-party-licenses.mjs', import.meta.url));
const FRONTEND = join(FIXTURES, 'frontend.txt');
const BACKEND = join(FIXTURES, 'backend.txt');

function run(frontend = FRONTEND, backend = BACKEND) {
	const dir = mkdtempSync(join(tmpdir(), 'combine-tpl-'));
	try {
		const out = join(dir, 'THIRD-PARTY-LICENSES.txt');
		const r = spawnSync(process.execPath, [SCRIPT, '--frontend', frontend, '--backend', backend, '--out', out], {
			encoding: 'utf8'
		});
		let text = null;
		try {
			text = readFileSync(out, 'utf8');
		} catch {
			// nothing written
		}
		return { status: r.status, stderr: r.stderr, text };
	} finally {
		rmSync(dir, { recursive: true, force: true });
	}
}

test('both headers, underlined, frontend first', () => {
	const { status, text } = run();
	assert.equal(status, 0);
	const fe = text.indexOf('Web interface (pythia-frontend)\n===============================\n\n');
	const be = text.indexOf('\nServer (pythia-backend)\n=======================\n\n');
	assert.equal(fe, 0);
	assert.ok(be > fe);
});

test('each part is byte-identical to its input, one blank line between', () => {
	const { text } = run();
	const fe = readFileSync(FRONTEND, 'utf8');
	const be = readFileSync(BACKEND, 'utf8');
	assert.ok(text.includes(`\n\n${fe}\nServer (pythia-backend)\n`));
	assert.ok(text.endsWith(`=\n\n${be}`));
});

test('deterministic: two runs, same bytes', () => {
	assert.equal(run().text, run().text);
});

test('a missing input exits 1 and writes nothing', () => {
	const r = run(join(FIXTURES, 'missing.txt'));
	assert.equal(r.status, 1);
	assert.match(r.stderr, /combine-third-party-licenses: .*missing\.txt/);
	assert.equal(r.text, null);
});
