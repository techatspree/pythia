import { test, expect, type APIRequestContext } from '@playwright/test';
import { fileURLToPath } from 'node:url';

const API = 'http://localhost:8080';
const LOCAL = 'http://localhost:5173';

// Merlin WBS import (task-131): from the estimation detail page, upload the real
// sample document's state.sql through the hidden file input and assert a draft
// version is created (checked via the API, not by scraping the grid).

function seed(subject: string) {
	return {
		cookies: [],
		origins: [{ origin: LOCAL, localStorage: [{ name: 'devAuthSubject', value: subject }] }]
	};
}

const SAMPLE = fileURLToPath(
	new URL('../../../planning/inputdata/MerlinDemoProject.mproject/state.sql', import.meta.url)
);

async function createEstimation(req: APIRequestContext): Promise<string> {
	const H = { Authorization: 'Dev dev-admin' };
	const proj = await req.post(`${API}/api/projects`, { headers: H, data: { name: 'Merlin e2e' } });
	expect(proj.status(), await proj.text()).toBe(201);
	const projectId = (await proj.json()).id;
	const est = await req.post(`${API}/api/projects/${projectId}/estimations`, {
		headers: H,
		data: { offer: 'MERLIN-E2E' }
	});
	expect(est.status(), await est.text()).toBe(201);
	return (await est.json()).id;
}

test('import a Merlin project file creates a draft with the WBS tree', async ({ browser }) => {
	const ctx = await browser.newContext({ baseURL: LOCAL, locale: 'de-DE', storageState: seed('dev-admin') });
	const page = await ctx.newPage();
	try {
		const estimationId = await createEstimation(page.request);

		await page.goto(`/estimations/${estimationId}`);
		await expect(page.getByRole('button', { name: 'Aus Merlin importieren' })).toBeVisible();

		// Drive the hidden file input directly (the button opens a native picker).
		const importResponse = page.waitForResponse(
			(r) => r.url().includes('/versions/import/merlin') && r.request().method() === 'POST'
		);
		await page.getByTestId('merlin-import-input').setInputFiles(SAMPLE);
		expect((await importResponse).status()).toBe(201);

		// Assert the draft now holds the imported tree (roots present).
		const draft = await page.request.get(`${API}/api/estimations/${estimationId}/versions/draft`, {
			headers: { Authorization: 'Dev dev-admin' }
		});
		expect(draft.status()).toBe(200);
		const roots = (await draft.json()).roots as unknown[];
		expect(roots.length).toBeGreaterThan(0);
	} finally {
		await ctx.close();
	}
});

test('importing when a draft exists asks to confirm before replacing it', async ({ browser }) => {
	const ctx = await browser.newContext({ baseURL: LOCAL, locale: 'de-DE', storageState: seed('dev-admin') });
	const page = await ctx.newPage();
	try {
		const estimationId = await createEstimation(page.request);
		// Create a draft first so the import is blocked (409) → confirmation dialog.
		const created = await page.request.post(`${API}/api/estimations/${estimationId}/versions`, {
			headers: { Authorization: 'Dev dev-admin' }
		});
		expect(created.status()).toBe(201);

		await page.goto(`/estimations/${estimationId}`);
		await page.getByTestId('merlin-import-input').setInputFiles(SAMPLE);

		// The destructive replace is gated behind a confirmation dialog.
		const dialog = page.getByRole('dialog', { name: 'Entwurf ersetzen?' });
		await expect(dialog).toBeVisible();

		// Confirm → the draft is deleted and re-imported from the WBS.
		const importResponse = page.waitForResponse(
			(r) => r.url().includes('/versions/import/merlin') && r.request().method() === 'POST'
		);
		await dialog.getByRole('button', { name: 'Entwurf ersetzen und importieren' }).click();
		expect((await importResponse).status()).toBe(201);
		await expect(dialog).toBeHidden();

		const draft = await page.request.get(`${API}/api/estimations/${estimationId}/versions/draft`, {
			headers: { Authorization: 'Dev dev-admin' }
		});
		const roots = (await draft.json()).roots as unknown[];
		expect(roots.length).toBeGreaterThan(0);
	} finally {
		await ctx.close();
	}
});
