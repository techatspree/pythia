#!/usr/bin/env node
// Assembles Pythia's ONE CycloneDX SBOM from the raw tool outputs (task-206):
//
//   pythia                       (application, the release version, Apache-2.0)
//   ├─ pythia-backend            the jars Quarkus packages into build/quarkus-app/lib
//   └─ pythia-frontend           the npm packages bundled into the SPA, plus the
//                                Kotlin/JS code compiled into src/lib/domain/domain.mjs
//
// Inputs, all produced by the build:
//   --backend   the CycloneDX Gradle plugin's output for :backend:implementation
//               (quarkusProdRuntimeClasspathConfiguration)
//   --lib-dir   build/quarkus-app/lib, i.e. what actually ships
//   --frontend  rollup-plugin-sbom's CLIENT output (.svelte-kit/output/client/cyclonedx)
//   --domain    the CycloneDX Gradle plugin's output for :domain (jsRuntimeClasspath)
//   --version   the application version (gradle.properties)
//   --out       where to write the result (build/sbom/pythia.cdx.json)
//
// What it does, and why:
//   * Trims the backend part to the shipped jars. The Gradle configuration is
//     the closest one to the package but not equal to it: BOM and Kotlin
//     Multiplatform umbrella entries (`type=pom`) have no jar, Quarkus strips some
//     artifacts, and our own modules ship as `<name>-jvm` jars. A jar that no
//     component describes is an ERROR, because it would ship undescribed.
//   * Drops the same `type=pom` umbrellas from the Kotlin/JS part and merges it
//     into the frontend part.
//   * Removing an entry reconnects its parents to its children, so the
//     dependency graph keeps every edge between components that remain.
//   * Prefixes every library bom-ref with its part (`pythia-backend:` /
//     `pythia-frontend:`): the same purl ships in both parts (our io.pythia
//     modules as JVM jars AND as Kotlin/JS), and bom-refs must be unique.
//   * Marks our own components Apache-2.0 (task-145); the tools leave them blank.
//   * Deterministic output: no serial number, no timestamp, sorted entries.
//
// Zero dependencies on purpose: it runs on the project's pinned Node.

import { readFileSync, readdirSync, writeFileSync, mkdirSync, statSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { pathToFileURL } from 'node:url';

const APACHE = [{ license: { id: 'Apache-2.0' } }];
const FIRST_PARTY = 'pkg:maven/io.pythia/';

/** `pkg:maven/<group>/<name>@<version>?<qualifiers>` → its parts. */
export function parsePurl(purl) {
	const m = /^pkg:([^/]+)\/(.+)@([^?#]+)(?:\?([^#]*))?/.exec(purl ?? '');
	if (!m) return null;
	const [, type, path, version, query] = m;
	const slash = path.lastIndexOf('/');
	const qualifiers = Object.fromEntries(new URLSearchParams(query ?? ''));
	return {
		type,
		group: slash < 0 ? null : decodeURIComponent(path.slice(0, slash)),
		name: decodeURIComponent(slash < 0 ? path : path.slice(slash + 1)),
		version: decodeURIComponent(version),
		qualifiers
	};
}

const isPomEntry = (c) => parsePurl(c.purl)?.qualifiers.type === 'pom';
const isFirstParty = (c) => (c.purl ?? '').startsWith(FIRST_PARTY);

/** The jar file names (without `.jar`) a Maven component may ship as. */
function jarNamesFor(c) {
	const p = parsePurl(c.purl);
	if (!p || p.type !== 'maven' || !p.group) return [];
	const classifier = p.qualifiers.classifier ? `-${p.qualifiers.classifier}` : '';
	const names = [`${p.group}.${p.name}-${p.version}${classifier}`];
	// Our own Kotlin Multiplatform modules ship as their JVM variant.
	if (isFirstParty(c)) names.push(`${p.group}.${p.name}-jvm-${p.version}${classifier}`);
	return names;
}

/**
 * Rebuilds a dependency graph over the components that remain: an edge to a
 * removed node is replaced by that node's own (resolved) children.
 */
function contractGraph(dependencies, keep) {
	const children = new Map(dependencies.map((d) => [d.ref, d.dependsOn ?? []]));
	const resolve = (ref, seen = new Set()) => {
		if (keep(ref)) return [ref];
		if (seen.has(ref)) return [];
		seen.add(ref);
		return (children.get(ref) ?? []).flatMap((c) => resolve(c, seen));
	};
	const out = new Map();
	for (const [ref, deps] of children) {
		if (!keep(ref)) continue;
		out.set(ref, [...new Set(deps.flatMap((d) => resolve(d)))].filter((d) => d !== ref));
	}
	return out;
}

const sortBy = (key) => (a, b) => (a[key] < b[key] ? -1 : a[key] > b[key] ? 1 : 0);

/**
 * The pure core: raw BOMs (+ the shipped jar names) in, one SBOM out.
 * Returns { bom, dropped, errors }.
 */
export function finalize({ backend, jars, frontend, domain, version }) {
	const errors = [];
	const dropped = [];

	// ---- backend part: trim to the shipped jars --------------------------------
	const jarSet = new Set(jars);
	const claimedBy = new Map();
	const backendKept = new Map(); // original bom-ref -> component
	for (const c of backend.components ?? []) {
		if (isPomEntry(c)) {
			dropped.push({ part: 'pythia-backend', purl: c.purl, why: 'type=pom (BOM or umbrella), no jar' });
			continue;
		}
		const jar = jarNamesFor(c).find((n) => jarSet.has(n));
		if (!jar) {
			dropped.push({ part: 'pythia-backend', purl: c.purl, why: 'no jar in quarkus-app/lib (not packaged)' });
			continue;
		}
		if (claimedBy.has(jar)) {
			errors.push(`jar ${jar}.jar is claimed by both ${claimedBy.get(jar)} and ${c.purl}`);
			continue;
		}
		claimedBy.set(jar, c.purl);
		backendKept.set(c['bom-ref'], c);
	}
	for (const jar of jars) {
		if (!claimedBy.has(jar)) errors.push(`jar ${jar}.jar ships but no SBOM component describes it`);
	}

	// ---- frontend part: bundled npm packages + Kotlin/JS -----------------------
	const frontendKept = new Map();
	for (const c of frontend.components ?? []) frontendKept.set(c['bom-ref'], c);
	const npmPurls = new Set([...frontendKept.values()].map((c) => c.purl));
	const domainRoot = domain.metadata?.component;
	const domainKept = new Map();
	for (const c of [...(domainRoot ? [domainRoot] : []), ...(domain.components ?? [])]) {
		if (isPomEntry(c)) {
			dropped.push({ part: 'pythia-frontend', purl: c.purl, why: 'type=pom (umbrella of a -js klib)' });
			continue;
		}
		if (npmPurls.has(c.purl)) continue;
		domainKept.set(c['bom-ref'], c);
	}

	// ---- assemble ---------------------------------------------------------------
	const part = (name, keptMaps, rawDeps, rawRoots) => {
		const prefix = `${name}:`;
		const kept = new Map(keptMaps.flatMap((m) => [...m]));
		const roots = new Set(rawRoots.filter(Boolean));
		const keep = (ref) => kept.has(ref) || roots.has(ref);
		const graph = contractGraph(rawDeps, keep);
		const mapRef = (ref) => (roots.has(ref) ? name : prefix + ref);

		const components = [...kept.values()]
			.map((c) => {
				const out = structuredClone(c);
				out['bom-ref'] = prefix + c['bom-ref'];
				if (isFirstParty(c)) out.licenses = APACHE;
				return out;
			})
			.sort(sortBy('bom-ref'));

		// The part depends on what its raw roots depended on.
		const partDeps = new Set();
		const deps = [];
		for (const [ref, children] of graph) {
			const mapped = children.map(mapRef).filter((r) => r !== name);
			if (roots.has(ref)) mapped.forEach((r) => partDeps.add(r));
			else deps.push({ ref: prefix + ref, dependsOn: mapped.sort() });
		}
		for (const c of components) {
			if (!deps.some((d) => d.ref === c['bom-ref'])) deps.push({ ref: c['bom-ref'], dependsOn: [] });
		}
		deps.push({ ref: name, dependsOn: [...partDeps].sort() });
		return {
			component: {
				type: 'application',
				'bom-ref': name,
				name,
				version,
				licenses: APACHE,
				components
			},
			deps
		};
	};

	const backendRoot = backend.metadata?.component?.['bom-ref'];
	const frontendRoot = frontend.metadata?.component?.['bom-ref'];
	const be = part('pythia-backend', [backendKept], backend.dependencies ?? [], [backendRoot]);
	// The SPA uses the domain, so the frontend part depends on the domain module
	// as well as on its bundled npm packages.
	const feDeps = [...(frontend.dependencies ?? []), ...(domain.dependencies ?? [])];
	if (domainRoot) {
		const rootEntry = feDeps.find((d) => d.ref === frontendRoot);
		if (rootEntry) rootEntry.dependsOn = [...(rootEntry.dependsOn ?? []), domainRoot['bom-ref']];
		else feDeps.push({ ref: frontendRoot, dependsOn: [domainRoot['bom-ref']] });
	}
	const fe = part('pythia-frontend', [frontendKept, domainKept], feDeps, [frontendRoot]);

	const bom = {
		bomFormat: 'CycloneDX',
		specVersion: '1.6',
		version: 1,
		metadata: {
			component: {
				type: 'application',
				'bom-ref': 'pythia',
				name: 'pythia',
				version,
				licenses: APACHE
			}
		},
		components: [be.component, fe.component],
		dependencies: [
			{ ref: 'pythia', dependsOn: ['pythia-backend', 'pythia-frontend'] },
			...[...be.deps, ...fe.deps].sort(sortBy('ref'))
		]
	};
	dropped.sort((a, b) => (a.part + a.purl < b.part + b.purl ? -1 : 1));
	return { bom, dropped, errors };
}

/** Jar names (without `.jar`) under lib/boot and lib/main. */
export function listJars(libDir) {
	const out = [];
	for (const sub of ['boot', 'main']) {
		const dir = join(libDir, sub);
		let entries = [];
		try {
			if (statSync(dir).isDirectory()) entries = readdirSync(dir);
		} catch {
			continue;
		}
		for (const f of entries) if (f.endsWith('.jar')) out.push(f.slice(0, -4));
	}
	return out.sort();
}

function parseArgs(argv) {
	const args = {};
	for (let i = 0; i < argv.length; i += 2) {
		if (!argv[i].startsWith('--') || argv[i + 1] === undefined) throw new Error(`bad argument: ${argv[i]}`);
		args[argv[i].slice(2)] = argv[i + 1];
	}
	for (const k of ['backend', 'lib-dir', 'frontend', 'domain', 'version', 'out']) {
		if (!args[k]) throw new Error(`missing --${k}`);
	}
	return args;
}

function main() {
	let args;
	let inputs;
	try {
		args = parseArgs(process.argv.slice(2));
		const read = (f) => JSON.parse(readFileSync(f, 'utf8'));
		inputs = {
			backend: read(args.backend),
			frontend: read(args.frontend),
			domain: read(args.domain),
			jars: listJars(args['lib-dir']),
			version: args.version
		};
		if (inputs.jars.length === 0) throw new Error(`no jars under ${args['lib-dir']}/{boot,main}`);
	} catch (e) {
		console.error(`sbom-finalize: ${e.message}`);
		process.exit(2);
	}
	const { bom, dropped, errors } = finalize(inputs);
	for (const d of dropped) console.log(`dropped from ${d.part}: ${d.purl} (${d.why})`);
	if (errors.length) {
		for (const e of errors) console.error(`ERROR: ${e}`);
		process.exit(1);
	}
	mkdirSync(dirname(args.out), { recursive: true });
	writeFileSync(args.out, JSON.stringify(bom, null, 2) + '\n');
	const [be, fe] = bom.components;
	console.log(
		`Wrote ${args.out}: pythia ${args.version}, ${be.components.length} backend + ${fe.components.length} frontend components`
	);
}

if (import.meta.url === pathToFileURL(process.argv[1] ?? '').href) main();
