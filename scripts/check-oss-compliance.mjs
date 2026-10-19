#!/usr/bin/env node
// Checks the shipped components of Pythia's SBOM against the open-source
// compliance policy, compliance/oss-policy.json (task-207). Blocking: run by
// `:frontend:checkOssCompliance`, part of `./gradlew build`.
//
//   node scripts/check-oss-compliance.mjs [--policy <file>] [<sbom>...]
//
// Defaults: compliance/oss-policy.json and the SBOMs in its `scope`, both
// relative to the repository root. Exit 0 on pass (warnings allowed), 1 on any
// failure, 2 on unreadable input.
//
// The rules, in order, for every component that is not one of the application
// components (the root and its parts `pythia-backend` / `pythia-frontend`),
// at any depth of the component tree:
//   1. a `firstParty` purl prefix passes — our own code, Apache-2.0;
//   2. an `exceptions` entry with exactly this purl decides it and passes;
//      `pendingReview: true` passes with a warning;
//   3. otherwise the license is read from the SBOM — an SPDX `id`, an
//      `expression`, or a `name` mapped through `aliases` — and evaluated.
//      `A OR B` takes the better side (that is what dual licensing means),
//      `A AND B` the worse, `A WITH x` counts as `A` unless the policy lists
//      the combined form. SEVERAL `licenses` entries are combined with AND:
//      CycloneDX does not say a list is a choice, so only an explicit OR is one;
//   4. allowed passes; conditional passes and is reported with its obligation;
//      forbidden, unknown or missing FAILS.
//   5. The policy itself is checked: an exception matching no component
//      (stale), an entry without a reason, and an id in two categories are
//      errors — otherwise a removed dependency leaves a permanent hole.
//
// Zero dependencies on purpose: it runs on the project's pinned Node.

import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const CATEGORIES = ['allowed', 'conditional', 'forbidden'];

// Verdict ranks: lower is better. OR takes the minimum, AND the maximum.
const RANK = { allowed: 0, conditional: 1, forbidden: 2, unknown: 2 };

/** Parses an SPDX license expression into a tree of {op, left, right} and {id, with}. */
export function parseExpression(text) {
	const tokens = text.match(/\(|\)|[^\s()]+/g) ?? [];
	let pos = 0;
	const peek = () => tokens[pos];
	const isOp = (t, op) => typeof t === 'string' && t.toUpperCase() === op;
	const fail = (why) => {
		throw new Error(`invalid license expression "${text}": ${why}`);
	};

	function or() {
		let node = and();
		while (isOp(peek(), 'OR')) {
			pos++;
			node = { op: 'OR', left: node, right: and() };
		}
		return node;
	}
	function and() {
		let node = atom();
		while (isOp(peek(), 'AND')) {
			pos++;
			node = { op: 'AND', left: node, right: atom() };
		}
		return node;
	}
	function atom() {
		const t = tokens[pos++];
		if (t === undefined) fail('unexpected end');
		if (t === '(') {
			const node = or();
			if (tokens[pos++] !== ')') fail('missing )');
			return node;
		}
		if (t === ')' || isOp(t, 'OR') || isOp(t, 'AND') || isOp(t, 'WITH')) fail(`unexpected "${t}"`);
		if (isOp(peek(), 'WITH')) {
			pos++;
			const exc = tokens[pos++];
			if (exc === undefined || exc === '(' || exc === ')') fail('WITH needs an exception id');
			return { id: t, with: exc };
		}
		return { id: t };
	}

	const tree = or();
	if (pos !== tokens.length) fail(`unexpected "${tokens[pos]}"`);
	return tree;
}

/** The category of one SPDX id in the policy, or 'unknown'. */
export function categoryOf(id, policy) {
	for (const c of CATEGORIES) if (Object.hasOwn(policy.licenses?.[c] ?? {}, id)) return c;
	return 'unknown';
}

/**
 * Evaluates a license tree. Returns {category, conditional: [ids], bad: [{license, category}]}:
 * the overall category, the conditional licenses the chosen reading relies on,
 * and — when it fails — the licenses that made it fail.
 */
export function evaluate(node, policy) {
	if (node.op) {
		const l = evaluate(node.left, policy);
		const r = evaluate(node.right, policy);
		if (node.op === 'OR') return RANK[r.category] < RANK[l.category] ? r : l;
		const worse = RANK[r.category] > RANK[l.category] ? r : l;
		return {
			category: worse.category,
			conditional: [...l.conditional, ...r.conditional],
			bad: [...l.bad, ...r.bad]
		};
	}
	if (node.unknownName !== undefined) {
		return { category: 'unknown', conditional: [], bad: [{ license: `"${node.unknownName}"`, category: 'not an SPDX id and has no alias' }] };
	}
	let id = node.id;
	let category = 'unknown';
	if (node.with) {
		id = `${node.id} WITH ${node.with}`;
		category = categoryOf(id, policy);
		// An exception only ADDS permissions, so judging the base license alone is conservative.
		if (category === 'unknown') {
			id = node.id;
			category = categoryOf(id, policy);
		}
	} else {
		category = categoryOf(id, policy);
	}
	return {
		category,
		conditional: category === 'conditional' ? [id] : [],
		bad: RANK[category] === 2 ? [{ license: id, category }] : []
	};
}

/** The license tree of one SBOM component, or null when it declares none. */
export function licenseTree(component, policy) {
	const nodes = (component.licenses ?? []).map((entry) => {
		if (entry.expression) return parseExpression(entry.expression);
		const l = entry.license ?? {};
		if (l.id) return { id: l.id };
		if (l.name) {
			const alias = policy.aliases?.[l.name];
			return alias ? parseExpression(alias) : { unknownName: l.name };
		}
		return { unknownName: '' };
	});
	if (nodes.length === 0) return null;
	return nodes.reduce((left, right) => ({ op: 'AND', left, right }));
}

/** A component's licenses as written in the SBOM, for messages. */
function describeLicenses(component) {
	const parts = (component.licenses ?? []).map(
		(e) => e.expression ?? e.license?.id ?? (e.license?.name ? `"${e.license.name}"` : '?')
	);
	return parts.length ? parts.join(' + ') : '(none)';
}

/** Every checked component of a BOM: everything below the application components, at any depth. */
export function shippedComponents(bom) {
	const out = [];
	const walk = (components) => {
		for (const c of components ?? []) {
			const isApplication = c.type === 'application' && !c.purl;
			if (!isApplication) out.push(c);
			walk(c.components);
		}
	};
	walk(bom.components);
	return out;
}

/** Rule 5: the policy's own consistency, independent of any SBOM. */
export function checkPolicy(policy) {
	const errors = [];
	const seen = new Map();
	for (const c of CATEGORIES) {
		for (const [id, reason] of Object.entries(policy.licenses?.[c] ?? {})) {
			if (typeof reason !== 'string' || reason.trim() === '') errors.push(`policy: ${c} license ${id} has no reason`);
			if (seen.has(id)) errors.push(`policy: ${id} is in two categories (${seen.get(id)} and ${c})`);
			seen.set(id, c);
		}
	}
	const purls = new Set();
	for (const e of policy.exceptions ?? []) {
		const name = e.purl || '(exception without purl)';
		if (!e.purl) errors.push(`policy: an exception has no purl`);
		if (!e.license) errors.push(`policy: exception ${name} has no license`);
		if (typeof e.reason !== 'string' || e.reason.trim() === '') errors.push(`policy: exception ${name} has no reason`);
		if (e.purl && purls.has(e.purl)) errors.push(`policy: exception ${e.purl} is listed twice`);
		purls.add(e.purl);
	}
	return errors;
}

/**
 * Checks the BOMs against the policy. Returns {errors, warnings, conditional, counts}:
 * one entry per distinct purl — the same purl ships in both parts (our io.pythia
 * modules as JVM jars AND as Kotlin/JS), and it should be reported once.
 */
export function check(policy, boms) {
	const errors = checkPolicy(policy);
	const warnings = [];
	const conditional = new Map();
	const counts = { firstParty: 0, exception: 0, allowed: 0, conditional: 0, failed: 0 };
	const exceptions = new Map((policy.exceptions ?? []).filter((e) => e.purl).map((e) => [e.purl, e]));
	const used = new Set();
	const judged = new Set();

	for (const bom of boms) {
		for (const c of shippedComponents(bom)) {
			const id = c.purl ?? c['bom-ref'] ?? c.name;
			if (judged.has(id)) {
				if (exceptions.has(id)) used.add(id);
				continue;
			}
			judged.add(id);

			if (!c.purl) {
				errors.push(`${id}: component has no purl, so the policy cannot identify it`);
				counts.failed++;
				continue;
			}
			if ((policy.firstParty ?? []).some((p) => c.purl.startsWith(p))) {
				counts.firstParty++;
				continue;
			}
			const exception = exceptions.get(c.purl);
			if (exception) {
				used.add(c.purl);
				counts.exception++;
				if (exception.pendingReview) {
					warnings.push(`${c.purl}: ${exception.license} — pending review: ${exception.reason}`);
				}
				continue;
			}

			let tree;
			try {
				tree = licenseTree(c, policy);
			} catch (e) {
				errors.push(`${c.purl}: ${e.message}`);
				counts.failed++;
				continue;
			}
			if (!tree) {
				errors.push(`${c.purl}: (none) — no license declared`);
				counts.failed++;
				continue;
			}
			const result = evaluate(tree, policy);
			if (RANK[result.category] === 2) {
				const why = result.bad
					.map((b) => `${b.license} is ${b.category === 'unknown' ? 'in no policy category' : b.category}`)
					.join(', ');
				errors.push(`${c.purl}: ${describeLicenses(c)} — ${why}`);
				counts.failed++;
			} else {
				counts[result.category]++;
				if (result.category === 'conditional') {
					conditional.set(
						c.purl,
						[...new Set(result.conditional)].map((l) => ({ license: l, obligation: policy.licenses.conditional[l] }))
					);
				}
			}
		}
	}

	for (const purl of exceptions.keys()) {
		if (!used.has(purl)) errors.push(`policy: exception ${purl} matches no shipped component (stale; remove it)`);
	}
	return { errors, warnings, conditional, counts };
}

function parseArgs(argv) {
	const args = { policy: resolve(ROOT, 'compliance/oss-policy.json'), sboms: [] };
	for (let i = 0; i < argv.length; i++) {
		if (argv[i] === '--policy') {
			if (argv[i + 1] === undefined) throw new Error('--policy needs a file');
			args.policy = argv[++i];
		} else if (argv[i].startsWith('--')) {
			throw new Error(`unknown option ${argv[i]}`);
		} else {
			args.sboms.push(argv[i]);
		}
	}
	return args;
}

function main() {
	let policy;
	let boms;
	try {
		const args = parseArgs(process.argv.slice(2));
		const read = (f) => JSON.parse(readFileSync(f, 'utf8'));
		policy = read(args.policy);
		const sboms = args.sboms.length ? args.sboms : (policy.scope ?? []).map((s) => resolve(ROOT, s));
		if (sboms.length === 0) throw new Error('no SBOM given and the policy has no scope');
		boms = sboms.map(read);
	} catch (e) {
		console.error(`check-oss-compliance: ${e.message}`);
		process.exit(2);
	}

	const { errors, warnings, conditional, counts } = check(policy, boms);
	for (const [purl, items] of [...conditional].sort(([a], [b]) => a.localeCompare(b))) {
		for (const { license, obligation } of items) console.log(`conditional  ${purl}: ${license} — ${obligation}`);
	}
	for (const w of warnings) console.log(`WARNING      ${w}`);
	for (const e of errors) console.error(`FAIL         ${e}`);
	console.log(
		`OSS compliance: ${counts.allowed} allowed, ${counts.conditional} conditional, ` +
			`${counts.firstParty} first-party, ${counts.exception} by exception ` +
			`(${warnings.length} pending review), ${counts.failed} failed` +
			(errors.length > counts.failed ? `, ${errors.length - counts.failed} policy error(s)` : '')
	);
	if (errors.length) process.exit(1);
}

if (import.meta.url === pathToFileURL(process.argv[1] ?? '').href) main();
