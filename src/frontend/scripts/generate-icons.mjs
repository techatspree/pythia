// Rasterises the canonical product mark (src/lib/assets/logo.svg) into the
// static/ PNG app icons. Driven by Playwright's bundled Chromium so the repo
// needs no extra image dependency; the output is committed, so a plain
// `npm run build` never needs a browser. Run with `npm run gen:icons`.
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium } from '@playwright/test';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const sizes = [16, 32, 48, 180, 192, 512];

// Packs already-encoded PNGs into an .ico container. Every browser that still
// asks for /favicon.ico accepts PNG-compressed entries, so no re-encoding is
// needed: an ICONDIR header, one 16-byte ICONDIRENTRY per image, then the PNGs.
function buildIco(images) {
	const header = Buffer.alloc(6);
	header.writeUInt16LE(0, 0); // reserved
	header.writeUInt16LE(1, 2); // type: icon
	header.writeUInt16LE(images.length, 4);

	let offset = 6 + images.length * 16;
	const entries = images.map(({ size, data }) => {
		const entry = Buffer.alloc(16);
		entry.writeUInt8(size >= 256 ? 0 : size, 0); // width
		entry.writeUInt8(size >= 256 ? 0 : size, 1); // height
		entry.writeUInt8(0, 2); // palette size
		entry.writeUInt8(0, 3); // reserved
		entry.writeUInt16LE(1, 4); // colour planes
		entry.writeUInt16LE(32, 6); // bits per pixel
		entry.writeUInt32LE(data.length, 8);
		entry.writeUInt32LE(offset, 12);
		offset += data.length;
		return entry;
	});

	return Buffer.concat([header, ...entries, ...images.map((i) => i.data)]);
}

const svg = readFileSync(join(root, 'src/lib/assets/logo.svg'), 'utf8');
const dataUrl = `data:image/svg+xml;base64,${Buffer.from(svg).toString('base64')}`;

const browser = await chromium.launch();
const rendered = new Map();

for (const size of sizes) {
	const page = await browser.newPage({ viewport: { width: size, height: size } });
	await page.setContent(
		`<style>html,body{margin:0;padding:0;background:transparent}img{display:block;width:${size}px;height:${size}px}</style><img src="${dataUrl}">`
	);
	const buffer = await page.screenshot({ omitBackground: true });
	await page.close();
	rendered.set(size, buffer);
	writeFileSync(join(root, `static/icon-${size}.png`), buffer);
	console.log(`wrote static/icon-${size}.png`);
}

await browser.close();

writeFileSync(join(root, 'static/favicon-32.png'), rendered.get(32));
console.log('wrote static/favicon-32.png');
writeFileSync(join(root, 'static/apple-touch-icon.png'), rendered.get(180));
console.log('wrote static/apple-touch-icon.png');

// Browsers and iOS probe these two well-known paths on their own, regardless of
// the <link> tags, so serve them rather than collect 404s.
writeFileSync(join(root, 'static/apple-touch-icon-precomposed.png'), rendered.get(180));
console.log('wrote static/apple-touch-icon-precomposed.png');
writeFileSync(
	join(root, 'static/favicon.ico'),
	buildIco([16, 32, 48].map((size) => ({ size, data: rendered.get(size) })))
);
console.log('wrote static/favicon.ico');
