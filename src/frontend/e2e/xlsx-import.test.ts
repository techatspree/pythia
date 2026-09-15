import { test, expect, type APIRequestContext } from '@playwright/test';

const API = 'http://localhost:8090';
const LOCAL = 'http://localhost:5173';
const H = { Authorization: 'Dev dev-admin' } as const;

/**
 * xlsx import (task-183): the estimation detail page uploads a workbook this
 * application exported, and the backend turns it into a new draft.
 *
 * Every fixture is BUILT BY THE TEST — export a draft through the real endpoint
 * and upload those bytes back — so the round trip is what is under test and no
 * binary workbook is committed to the repository.
 */

function seed(subject: string) {
	return {
		cookies: [],
		origins: [{ origin: LOCAL, localStorage: [{ name: 'devAuthSubject', value: subject }] }]
	};
}

async function createEstimation(req: APIRequestContext, method: string): Promise<string> {
	const proj = await req.post(`${API}/api/projects`, {
		headers: H,
		data: { name: `Xlsx e2e ${Date.now()}` }
	});
	expect(proj.status(), await proj.text()).toBe(201);
	const projectId = (await proj.json()).id;

	const est = await req.post(`${API}/api/projects/${projectId}/estimations`, {
		headers: H,
		data: { offer: `XLSX-E2E-${Date.now()}`, method }
	});
	expect(est.status(), await est.text()).toBe(201);
	return (await est.json()).id;
}

/** A workbook exported from a fresh draft of a `method` estimation. */
async function exportedWorkbook(req: APIRequestContext, method: string): Promise<Buffer> {
	const estimationId = await createEstimation(req, method);
	const created = await req.post(`${API}/api/estimations/${estimationId}/versions`, { headers: H });
	expect(created.status(), await created.text()).toBe(201);

	const res = await req.get(
		`${API}/api/estimations/${estimationId}/versions/draft/export?format=xlsx`,
		{ headers: H }
	);
	expect(res.status(), await res.text()).toBe(200);
	const body = await res.body();
	// A real xlsx is a zip container; a JSON error body would start with '{'.
	expect(body.subarray(0, 2).toString('latin1')).toBe('PK');
	return body;
}

const upload = (buffer: Buffer) => ({
	name: 'estimation.xlsx',
	mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
	buffer
});

test('a workbook exported from one estimation imports as a draft in another', async ({
	browser
}) => {
	const ctx = await browser.newContext({
		baseURL: LOCAL,
		locale: 'de-DE',
		storageState: seed('dev-admin')
	});
	const page = await ctx.newPage();
	try {
		const workbook = await exportedWorkbook(page.request, 'THREE_POINT_PERT');
		const targetId = await createEstimation(page.request, 'THREE_POINT_PERT');

		await page.goto(`/estimations/${targetId}`);
		await expect(page.getByRole('button', { name: 'Aus Excel importieren' })).toBeVisible();

		// Drive the hidden input directly — the button opens a native picker.
		const importResponse = page.waitForResponse(
			(r) => r.url().includes('/versions/import/xlsx') && r.request().method() === 'POST'
		);
		await page.getByTestId('xlsx-import-input').setInputFiles(upload(workbook));
		expect((await importResponse).status()).toBe(201);

		const draft = await page.request.get(`${API}/api/estimations/${targetId}/versions/draft`, {
			headers: H
		});
		expect(draft.status()).toBe(200);
		expect((await draft.json()).isDraft).toBe(true);
	} finally {
		await ctx.close();
	}
});

test('a workbook written for another method is refused, in the user’s language', async ({
	browser
}) => {
	const ctx = await browser.newContext({
		baseURL: LOCAL,
		locale: 'de-DE',
		storageState: seed('dev-admin')
	});
	const page = await ctx.newPage();
	try {
		// A bucket+sampled workbook carries five method columns; read as PERT
		// (three) every trailing value would come off the wrong offset. The
		// backend refuses it with 422 rather than importing silently wrong numbers.
		const bucketWorkbook = await exportedWorkbook(page.request, 'BUCKET_SAMPLED_PERT');
		const pertId = await createEstimation(page.request, 'THREE_POINT_PERT');

		await page.goto(`/estimations/${pertId}`);
		const importResponse = page.waitForResponse(
			(r) => r.url().includes('/versions/import/xlsx') && r.request().method() === 'POST'
		);
		await page.getByTestId('xlsx-import-input').setInputFiles(upload(bucketWorkbook));
		expect((await importResponse).status()).toBe(422);

		// The refusal reaches the user as the translated mismatch message — not a
		// generic failure, and not the backend's English sentence.
		await expect(page.getByText(/anderen Schätzmethode/)).toBeVisible();

		// Nothing was created by the rejected upload.
		const draft = await page.request.get(`${API}/api/estimations/${pertId}/versions/draft`, {
			headers: H
		});
		expect(draft.status()).toBe(404);
	} finally {
		await ctx.close();
	}
});

test('importing when a draft exists asks to confirm before replacing it', async ({ browser }) => {
	const ctx = await browser.newContext({
		baseURL: LOCAL,
		locale: 'de-DE',
		storageState: seed('dev-admin')
	});
	const page = await ctx.newPage();
	try {
		const workbook = await exportedWorkbook(page.request, 'THREE_POINT_PERT');
		const targetId = await createEstimation(page.request, 'THREE_POINT_PERT');
		// A draft already exists → the import is blocked (409), and replacing it
		// is destructive, so it must be confirmed rather than just happening.
		const created = await page.request.post(`${API}/api/estimations/${targetId}/versions`, {
			headers: H
		});
		expect(created.status()).toBe(201);

		await page.goto(`/estimations/${targetId}`);
		await page.getByTestId('xlsx-import-input').setInputFiles(upload(workbook));

		const dialog = page.getByRole('dialog', { name: 'Entwurf ersetzen?' });
		await expect(dialog).toBeVisible();

		// Confirming replaces the draft and imports; the shared dialog's confirm
		// label is method-neutral since task-183.
		await dialog.getByRole('button', { name: 'Entwurf ersetzen und importieren' }).click();

		await expect
			.poll(async () => {
				const d = await page.request.get(`${API}/api/estimations/${targetId}/versions/draft`, {
					headers: H
				});
				return d.status();
			})
			.toBe(200);
	} finally {
		await ctx.close();
	}
});
