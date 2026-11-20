// Finds a component's own licence and notice files (task-208, shared since
// task-210). One implementation for both readers:
//   - scripts/generate-third-party-licenses.mjs, which copies them into each
//     image's THIRD-PARTY-LICENSES.txt;
//   - scripts/sbom-finalize.mjs, which takes the copyright lines from them into
//     the SBOM's `component.copyright`.
//
// Where they are looked up:
//   - npm packages in a node_modules tree, by `name@version` (nested ones too),
//     the package directory's top level;
//   - Maven jars and Kotlin/JS klibs (both zips), at the zip's root or anywhere
//     under META-INF/, by file name `<group>.<name>-<version>` or
//     `<name>-<version>` from a list of paths.
//
// Zero dependencies on purpose: it runs on the project's pinned Node.

import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { basename, join } from 'node:path';
import { inflateRawSync } from 'node:zlib';

export const LICENSE_FILE = /^(licen[cs]e|copying)([._-][^/]*)?$/i;
export const NOTICE_FILE = /^notice([._-][^/]*)?$/i;

const cmp = (a, b) => (a < b ? -1 : a > b ? 1 : 0);
const kindOf = (name) => (LICENSE_FILE.test(name) ? 'license' : NOTICE_FILE.test(name) ? 'notice' : null);

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
export function zipFiles(path) {
	const out = [];
	for (const [name, read] of readZip(readFileSync(path))) {
		if (name.endsWith('/') || name.endsWith('.class')) continue;
		if (name.includes('/') && !name.startsWith('META-INF/')) continue;
		const kind = kindOf(basename(name));
		if (kind) out.push({ path: name, kind, text: read().toString('utf8') });
	}
	return out;
}

/** A package directory's licence and notice files (top level only). */
export function dirFiles(dir) {
	const out = [];
	for (const f of readdirSync(dir)) {
		const kind = kindOf(f);
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

/** Jar/klib paths by file name without the extension. */
export function indexJars(paths) {
	return new Map(paths.map((j) => [basename(j).replace(/\.(jar|klib)$/, ''), j]));
}

/**
 * One component's licence and notice files, sorted by path. Returns
 * { files, missing }: `missing` explains why no package was found (then
 * `files` is empty), and is undefined when the package was found.
 */
export function findPackageFiles(c, { npm = new Map(), jarByName = new Map() }) {
	let files;
	let missing;
	if ((c.purl ?? '').startsWith('pkg:npm/')) {
		const dir = npm.get(`${c.group ? `${c.group}/` : ''}${c.name}@${c.version}`);
		if (!dir) missing = 'not in --node-modules';
		files = dir ? dirFiles(dir) : [];
	} else {
		const names = [`${c.group}.${c.name}-${c.version}`, `${c.name}-${c.version}`];
		const jar = names.map((n) => jarByName.get(n)).find(Boolean);
		if (!jar) missing = `no ${names.join(' / ')} in --jars`;
		files = jar ? zipFiles(jar) : [];
	}
	files.sort((a, b) => cmp(a.path, b.path));
	return { files, missing };
}
