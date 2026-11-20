// Test helper (task-208, shared since task-210): writes the jar and klib
// fixtures of the node --test suites, so no binary zip is committed.

import { deflateRawSync } from 'node:zlib';

// A minimal zip writer: `store` entries uncompressed, the rest deflated.
export function zip(entries, store = false) {
	const locals = [];
	const central = [];
	let offset = 0;
	for (const [name, content] of Object.entries(entries)) {
		const raw = Buffer.from(content);
		const data = store ? raw : deflateRawSync(raw);
		const n = Buffer.from(name);
		const local = Buffer.alloc(30);
		local.writeUInt32LE(0x04034b50, 0);
		local.writeUInt16LE(store ? 0 : 8, 8);
		local.writeUInt32LE(data.length, 18);
		local.writeUInt32LE(raw.length, 22);
		local.writeUInt16LE(n.length, 26);
		const cen = Buffer.alloc(46);
		cen.writeUInt32LE(0x02014b50, 0);
		cen.writeUInt16LE(store ? 0 : 8, 10);
		cen.writeUInt32LE(data.length, 20);
		cen.writeUInt32LE(raw.length, 24);
		cen.writeUInt16LE(n.length, 28);
		cen.writeUInt32LE(offset, 42);
		locals.push(local, n, data);
		central.push(cen, n);
		offset += 30 + n.length + data.length;
	}
	const cd = Buffer.concat(central);
	const eocd = Buffer.alloc(22);
	eocd.writeUInt32LE(0x06054b50, 0);
	eocd.writeUInt16LE(Object.keys(entries).length, 8);
	eocd.writeUInt16LE(Object.keys(entries).length, 10);
	eocd.writeUInt32LE(cd.length, 12);
	eocd.writeUInt32LE(offset, 16);
	return Buffer.concat([...locals, cd, eocd]);
}

