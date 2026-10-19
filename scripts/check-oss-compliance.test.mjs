// Unit tests for scripts/check-oss-compliance.mjs (task-207). Run: node --test scripts/check-oss-compliance.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { check, parseExpression } from './check-oss-compliance.mjs';

const fixture = (name) =>
	JSON.parse(readFileSync(new URL(`./fixtures/oss-compliance/${name}.json`, import.meta.url), 'utf8'));

const PURL = 'pkg:npm/probe@1.0.0';

/** The fixture SBOM with one extra component, nested two levels deep in the frontend part. */
function sbomWith(licenses) {
	const bom = fixture('sbom');
	const libB = bom.components.find((c) => c.name === 'pythia-frontend').components[0];
	libB.components.push({ type: 'library', 'bom-ref': `pythia-frontend:${PURL}`, name: 'probe', purl: PURL, licenses });
	return bom;
}

const run = (licenses, policyChange = (p) => p) => check(policyChange(fixture('policy')), [sbomWith(licenses)]);
const id = (spdx) => [{ license: { id: spdx } }];
const expr = (e) => [{ expression: e }];
const failsOn = (result, purl = PURL) => result.errors.some((e) => e.startsWith(`${purl}:`));

test('the fixture SBOM alone passes', () => {
	const r = check(fixture('policy'), [fixture('sbom')]);
	assert.deepEqual(r.errors, []);
	assert.equal(r.counts.allowed, 3);
});

test('an allowed id passes', () => {
	const r = run(id('MIT'));
	assert.deepEqual(r.errors, []);
	assert.equal(r.conditional.size, 0);
});

test('a conditional id passes and is reported with its obligation', () => {
	const r = run(id('EPL-2.0'));
	assert.deepEqual(r.errors, []);
	assert.deepEqual(r.conditional.get(PURL), [{ license: 'EPL-2.0', obligation: 'unmodified library, source upstream' }]);
});

test('a forbidden id fails, and the failure names the component', () => {
	const r = run(id('GPL-3.0-only'));
	assert.ok(failsOn(r));
	assert.match(r.errors.join('\n'), /GPL-3\.0-only is forbidden/);
});

test('an id in no category fails as unknown', () => {
	assert.ok(failsOn(run(id('WTFPL'))));
});

test('a component without a license fails', () => {
	assert.ok(failsOn(run([])));
	assert.ok(failsOn(run(undefined)));
});

test('OR passes when one side is allowed', () => {
	const r = run(expr('GPL-3.0-only OR MIT'));
	assert.deepEqual(r.errors, []);
});

test('AND fails when one side is forbidden', () => {
	assert.ok(failsOn(run(expr('MIT AND GPL-3.0-only'))));
});

test('parentheses are honoured', () => {
	// The grouping decides: (forbidden OR allowed) AND conditional passes, while
	// forbidden AND (allowed OR conditional) fails. Unparenthesised, AND binds tighter.
	assert.deepEqual(run(expr('(GPL-3.0-only OR MIT) AND EPL-2.0')).errors, []);
	assert.ok(failsOn(run(expr('GPL-3.0-only AND (MIT OR EPL-2.0)'))));
	assert.deepEqual(parseExpression('A OR B AND C'), {
		op: 'OR',
		left: { id: 'A' },
		right: { op: 'AND', left: { id: 'B' }, right: { id: 'C' } }
	});
});

test('WITH an exception is judged as its base license', () => {
	assert.deepEqual(run(expr('MIT WITH Some-exception')).errors, []);
	assert.ok(failsOn(run(expr('GPL-3.0-only WITH Some-exception'))));
});

test('a combined WITH form listed in the policy is judged as listed, not as its base', () => {
	const r = run(expr('GPL-2.0-only WITH Classpath-exception-2.0'));
	assert.deepEqual(r.errors, []);
	assert.equal(r.conditional.get(PURL)[0].license, 'GPL-2.0-only WITH Classpath-exception-2.0');
});

test('several license entries are combined with AND', () => {
	const allowedAndConditional = run([...id('Apache-2.0'), ...id('EPL-2.0')]);
	assert.deepEqual(allowedAndConditional.errors, []);
	assert.ok(allowedAndConditional.conditional.has(PURL));
	assert.ok(failsOn(run([...id('Apache-2.0'), ...id('GPL-3.0-only')])));
});

test('a license name is mapped through aliases; an unmapped name fails', () => {
	assert.deepEqual(run([{ license: { name: 'The MIT License' } }]).errors, []);
	assert.ok(failsOn(run([{ license: { name: 'Public Domain' } }])));
});

test('a first-party purl passes without a license', () => {
	// io.pythia/core in the fixture carries no license at all.
	const r = check(fixture('policy'), [fixture('sbom')]);
	assert.equal(r.counts.firstParty, 1);
	assert.ok(!failsOn(r, 'pkg:maven/io.pythia/core@9.9.9'));
});

test('an exception overrides the license found', () => {
	const r = run(id('GPL-3.0-only'), (p) => ({
		...p,
		exceptions: [{ purl: PURL, license: 'MIT', reason: 'mislabelled upstream; the source says MIT' }]
	}));
	assert.deepEqual(r.errors, []);
	assert.deepEqual(r.warnings, []);
});

test('a pendingReview exception passes with a warning', () => {
	const r = run(id('WTFPL'), (p) => ({
		...p,
		exceptions: [{ purl: PURL, license: 'WTFPL', reason: 'pending review in task-208', pendingReview: true }]
	}));
	assert.deepEqual(r.errors, []);
	assert.equal(r.warnings.length, 1);
	assert.match(r.warnings[0], /pending review/);
});

test('an exception matching no component is stale and fails', () => {
	const r = run(id('MIT'), (p) => ({
		...p,
		exceptions: [{ purl: 'pkg:npm/gone@0.0.1', license: 'MIT', reason: 'was a dependency once' }]
	}));
	assert.ok(r.errors.some((e) => /pkg:npm\/gone@0\.0\.1.*stale/.test(e)));
});

test('an exception without a reason fails', () => {
	const r = run(id('GPL-3.0-only'), (p) => ({ ...p, exceptions: [{ purl: PURL, license: 'MIT', reason: ' ' }] }));
	assert.ok(r.errors.some((e) => /no reason/.test(e)));
});

test('a license in two categories fails', () => {
	const r = run(id('MIT'), (p) => ({
		...p,
		licenses: { ...p.licenses, forbidden: { ...p.licenses.forbidden, MIT: 'contradiction' } }
	}));
	assert.ok(r.errors.some((e) => /MIT is in two categories/.test(e)));
});

test('a malformed expression fails instead of passing', () => {
	assert.ok(failsOn(run(expr('MIT OR'))));
	assert.ok(failsOn(run(expr('(MIT'))));
});
