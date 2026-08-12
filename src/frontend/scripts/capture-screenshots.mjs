// Captures the README screenshots from a RUNNING dev stack into docs/images/.
// Driven by Playwright's bundled Chromium so the repo needs no extra image
// dependency (same reasoning as generate-icons.mjs); the PNGs are committed, so
// a plain `npm run build` never needs a browser. Run with
// `npm run gen:screenshots` while `./scripts/dev.sh` is up.
//
// Three of the four shots use data the dev profile already seeds (Webshop
// Redesign has a draft + a submitted version; Data Platform Migration is the
// bucket+sampled estimation). Only the session room is seeded here, through the
// REST API, the same way e2e/session.test.ts does it.
//
// The UI language is per USER, and the README is English, so the capture user is
// switched to EN first — a German UI under English prose reads as an accident.
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium } from '@playwright/test';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const outDir = join(root, '../../docs/images');
const API = 'http://localhost:8090';
const APP = 'http://localhost:5173';
const AUTH = { Authorization: 'Dev dev-admin', 'Content-Type': 'application/json' };

async function api(path, init = {}) {
	const res = await fetch(`${API}${path}`, { ...init, headers: { ...AUTH, ...init.headers } });
	if (!res.ok) throw new Error(`${init.method ?? 'GET'} ${path} -> ${res.status} ${await res.text()}`);
	return res.status === 204 ? null : res.json();
}

// Fail loudly rather than writing blank images against a stack that is not up.
async function requireStack() {
	for (const url of [`${API}/api/ping`, APP]) {
		try {
			const res = await fetch(url);
			if (!res.ok) throw new Error(`status ${res.status}`);
		} catch (e) {
			console.error(
				`\nCannot reach ${url} (${e.message}).\n` +
					`Start the dev stack first:  ./scripts/dev.sh   (backend :8090, Vite :5173)\n`
			);
			process.exit(1);
		}
	}
}

// The seeded demo data: the PERT estimation with two versions, and the
// bucket+sampled one. Resolved by offer number so a reordered seed cannot make
// this silently capture the wrong project.
async function resolveSeed() {
	const projects = await api('/api/projects');
	const found = {};
	for (const p of projects) {
		const detail = await api(`/api/projects/${p.id}`);
		for (const e of detail.estimations ?? []) {
			if (e.offer === 'WS-2026-001') found.pert = { ...e, projectId: p.id };
			if (e.offer === 'DP-2026-001') found.bucket = { ...e, projectId: p.id };
		}
	}
	if (!found.pert || !found.bucket) {
		throw new Error(
			'Seeded demo estimations not found (expected offers WS-2026-001 and DP-2026-001). ' +
				'Is the dev profile seeding TestDataSeeder?'
		);
	}
	return found;
}

// A session revealed into PHASE2, so the shot shows the votes table and the
// aggregate rather than an empty room.
async function seedRevealedSession(estimationId) {
	const draft = await api(`/api/estimations/${estimationId}/versions/draft`);
	const leaves = [];
	const walk = (n) => {
		if (n.type === 'GROUP') (n.children ?? []).forEach(walk);
		else leaves.push(n.logicalId);
	};
	(draft.roots ?? []).forEach(walk);

	const session = await api('/api/sessions', {
		method: 'POST',
		body: JSON.stringify({
			estimationId,
			title: 'Sprint sizing — Webshop Redesign',
			itemLogicalIds: leaves.slice(0, 4)
		})
	});
	await api(`/api/sessions/${session.id}/start`, { method: 'POST' });

	// Two divergent votes so the reveal shows a spread worth looking at.
	await api(`/api/sessions/${session.id}/votes`, {
		method: 'POST',
		body: JSON.stringify({ minEffort: 3, expectedEffort: 5, maxEffort: 8 })
	});
	await fetch(`${API}/api/sessions/${session.id}/join`, {
		method: 'POST',
		headers: { Authorization: 'Dev dev-estimator' }
	});
	await fetch(`${API}/api/sessions/${session.id}/votes`, {
		method: 'POST',
		headers: { Authorization: 'Dev dev-estimator', 'Content-Type': 'application/json' },
		body: JSON.stringify({ minEffort: 8, expectedEffort: 13, maxEffort: 21 })
	});
	await api(`/api/sessions/${session.id}/items/current/phase2`, { method: 'POST' });
	return session.id;
}

async function shoot(page, url, file, { selector, settle = 900 } = {}) {
	await page.goto(url, { waitUntil: 'networkidle' });
	await page.waitForTimeout(settle);
	const target = selector ? page.locator(selector).first() : page;
	await target.screenshot({ path: join(outDir, file) });
	console.log(`wrote docs/images/${file}`);
}

await requireStack();
mkdirSync(outDir, { recursive: true });

// The language preference lives on the user row, so set it before the browser
// ever renders (the layout reads it once on load).
await api('/api/auth/me/language', { method: 'PUT', body: JSON.stringify({ language: 'en' }) });

const seed = await resolveSeed();
const sessionId = await seedRevealedSession(seed.pert.id);

const browser = await chromium.launch();
const context = await browser.newContext({
	viewport: { width: 1440, height: 900 },
	deviceScaleFactor: 2,
	locale: 'en-GB',
	storageState: {
		cookies: [],
		origins: [{ origin: APP, localStorage: [{ name: 'devAuthSubject', value: 'dev-admin' }] }]
	}
});
const page = await context.newPage();

await shoot(
	page,
	`${APP}/estimations/${seed.pert.id}/versions/2?draft=true`,
	'estimation-editor.png'
);
await shoot(page, `${APP}/sessions/${sessionId}`, 'session-room.png');
await shoot(page, `${APP}/estimations/${seed.pert.id}/compare?a=1&b=draft`, 'version-compare.png');
await shoot(page, `${APP}/estimations/${seed.bucket.id}/versions/1?draft=true`, 'bucket-editor.png');

await context.close();
await browser.close();

writeFileSync(
	join(outDir, 'README.md'),
	'<!-- Generated by `npm run gen:screenshots` (src/frontend/scripts/capture-screenshots.mjs).\n' +
		'     Do not edit by hand; re-run the script against a running dev stack instead. -->\n'
);
console.log('\ndone — 4 screenshots in docs/images/');
