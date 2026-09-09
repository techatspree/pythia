// Captures the README screenshots from a RUNNING dev stack into docs/images/.
// Driven by Playwright's bundled Chromium so the repo needs no extra image
// dependency (same reasoning as generate-icons.mjs); the PNGs are committed, so
// a plain `npm run build` never needs a browser. Run with
// `npm run gen:screenshots` while `./scripts/dev.sh` is up.
//
// Five of the six shots use data the dev profile already seeds (Webshop
// Redesign has a draft + a submitted version and carries the seeded schedule
// dependencies; Data Platform Migration is the bucket+sampled estimation). Only
// the session room is seeded here, through the REST API, the same way
// e2e/session.test.ts does it.
//
// The UI language is per USER, and the README is English, so the capture user is
// switched to EN first — a German UI under English prose reads as an accident.
// The previous preference is RESTORED when the script finishes, failures
// included: the Playwright suite asserts GERMAN text, so a stack left in
// English fails 28 of its tests with nothing in the output pointing back here.
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

async function shoot(page, url, file, { selector, settle = 900, prepare } = {}) {
	await page.goto(url, { waitUntil: 'networkidle' });
	await page.waitForTimeout(settle);
	// `prepare` runs after the page has settled and before the capture, for a
	// shot that needs an input filled first (the Gantt's start date).
	if (prepare) await prepare(page);
	const target = selector ? page.locator(selector).first() : page;
	await target.screenshot({ path: join(outDir, file) });
	console.log(`wrote docs/images/${file}`);
}

// The schedule view renders an empty-state paragraph instead of the editor or
// the chart when the version has no scheduled work. Refuse to commit a picture
// of nothing, the same way requireStack() refuses to shoot a dead stack.
async function refuseEmpty(page, testid, what) {
	if (await page.locator(`[data-testid="${testid}"]`).count()) {
		throw new Error(
			`${what} rendered its empty state — the seeded draft has no scheduled work ` +
				`or no dependencies. Check TestDataSeeder (task-173 seeds them).`
		);
	}
}

await requireStack();
mkdirSync(outDir, { recursive: true });

// The language preference lives on the user row, so set it before the browser
// ever renders (the layout reads it once on load). Read what is there first so
// the finally below can put it back. GET /api/auth/me does not mutate an
// existing preference — ensureUser seeds from Accept-Language on first sighting
// only — and the column is NOT NULL defaulting to German, so the fallback here
// is unreachable in practice and only guards against a shape change.
const previousLanguage = (await api('/api/auth/me')).language ?? 'de';
await api('/api/auth/me/language', { method: 'PUT', body: JSON.stringify({ language: 'en' }) });

let browser;
try {
	const seed = await resolveSeed();
	const sessionId = await seedRevealedSession(seed.pert.id);

	browser = await chromium.launch();
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

	// Both schedule shots come from ONE page: the dependency editor and the Gantt
	// live on the same route, so they differ only by selector.
	const scheduleUrl = `${APP}/estimations/${seed.pert.id}/versions/draft/schedule?draft=true`;

	// The inner canvas div, not the `overflow-x-auto` wrapper: the wrapper stretches
	// to the viewport, so shooting it leaves half the image blank, while the canvas
	// carries an explicit content width.
	await shoot(page, scheduleUrl, 'schedule-dependencies.png', {
		selector: '[data-testid="dependency-editor"] > div',
		prepare: (p) => refuseEmpty(p, 'schedule-empty', 'The dependency editor')
	});

	// The chart's start date defaults to TODAY, which would bake the capture date
	// into the PNG and churn it on every regeneration. Pin it — and pin it to a
	// Monday, so the first bar starts on a week boundary.
	await shoot(page, scheduleUrl, 'schedule-gantt.png', {
		selector: '[data-testid="gantt-chart"]',
		prepare: async (p) => {
			await refuseEmpty(p, 'gantt-empty', 'The Gantt chart');
			await p.locator('[data-testid="gantt-start-date"]').fill('2026-03-02');
			await p.waitForTimeout(300);
		}
	});
} finally {
	// Cleanup is best-effort in BOTH halves: a throw in here would replace the
	// real capture error with a cleanup error and destroy the diagnosis.
	try {
		await api('/api/auth/me/language', {
			method: 'PUT',
			body: JSON.stringify({ language: previousLanguage })
		});
	} catch (e) {
		console.warn(`could not restore the language preference to ${previousLanguage}: ${e.message}`);
	}
	try {
		// Closing the browser closes its contexts. Guarded on `browser` because a
		// failure in resolveSeed() reaches this block before chromium.launch().
		if (browser) await browser.close();
	} catch (e) {
		console.warn(`could not close the browser: ${e.message}`);
	}
}

writeFileSync(
	join(outDir, 'README.md'),
	'<!-- Generated by `npm run gen:screenshots` (src/frontend/scripts/capture-screenshots.mjs).\n' +
		'     Do not edit by hand; re-run the script against a running dev stack instead. -->\n'
);
console.log('\ndone — 6 screenshots in docs/images/');
