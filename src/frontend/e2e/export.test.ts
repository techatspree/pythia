import { test, expect, type APIRequestContext } from '@playwright/test';
import { readFileSync } from 'node:fs';

/**
 * The version editor's xlsx/csv export.
 *
 * Regression: the export menu used plain `<a href="/api/…" download>` links.
 * Browser navigation carries no `Authorization` header, so the (strict) dev
 * auth module answered `401` and the browser SAVED THAT JSON ERROR BODY as the
 * export file — no spreadsheet, no visible error. These tests download the real
 * bytes and assert they are a spreadsheet / a CSV, which a JSON error body can
 * never satisfy.
 *
 * Covered for the bucket + sampled method (where it was reported) and for
 * three-point PERT, since the defect was in the shared route.
 */

const AUTH = { Authorization: 'Dev dev-admin' } as const;
const JSON_HEADERS = { 'Content-Type': 'application/json', ...AUTH } as const;

async function seedProject(request: APIRequestContext, name: string): Promise<string> {
	const res = await request.post('/api/projects', { headers: JSON_HEADERS, data: { name } });
	expect(res.status()).toBe(201);
	return (await res.json()).id;
}

/** BUCKET_SAMPLED_PERT estimation with a draft: one sample + one non-sample. */
async function seedBucketDraft(request: APIRequestContext): Promise<string> {
	const projectId = await seedProject(request, `E2E Export ${Date.now()}`);
	const estRes = await request.post(`/api/projects/${projectId}/estimations`, {
		headers: JSON_HEADERS,
		data: { offer: `E2E-EXP-B-${Date.now()}`, method: 'BUCKET_SAMPLED_PERT' }
	});
	expect(estRes.status()).toBe(201);
	const estimationId = (await estRes.json()).id;

	const versionRes = await request.post(`/api/estimations/${estimationId}/versions`, {
		headers: JSON_HEADERS
	});
	expect(versionRes.status()).toBe(201);

	const bucket = crypto.randomUUID();
	const putRes = await request.put(`/api/estimations/${estimationId}/versions/draft`, {
		headers: JSON_HEADERS,
		data: {
			buckets: [{ id: bucket, position: 0, label: 'Frontend' }],
			roots: [
				{
					type: 'BUCKETED',
					logicalId: crypto.randomUUID(),
					description: 'Sample item',
					bucketId: bucket,
					isSample: true,
					minEffort: 1.0,
					expectedEffort: 2.0,
					maxEffort: 3.0
				},
				{
					type: 'BUCKETED',
					logicalId: crypto.randomUUID(),
					description: 'Inherits the bucket average',
					bucketId: bucket,
					isSample: false
				}
			]
		}
	});
	expect(putRes.status()).toBe(200);
	return estimationId;
}

/** THREE_POINT_PERT estimation with a draft carrying one leaf. */
async function seedPertDraft(request: APIRequestContext): Promise<string> {
	const projectId = await seedProject(request, `E2E Export PERT ${Date.now()}`);
	const estRes = await request.post(`/api/projects/${projectId}/estimations`, {
		headers: JSON_HEADERS,
		data: { offer: `E2E-EXP-P-${Date.now()}`, method: 'THREE_POINT_PERT' }
	});
	expect(estRes.status()).toBe(201);
	const estimationId = (await estRes.json()).id;

	const versionRes = await request.post(`/api/estimations/${estimationId}/versions`, {
		headers: JSON_HEADERS
	});
	expect(versionRes.status()).toBe(201);

	const putRes = await request.put(`/api/estimations/${estimationId}/versions/draft`, {
		headers: JSON_HEADERS,
		data: {
			roots: [
				{
					type: 'FIXED',
					logicalId: crypto.randomUUID(),
					description: 'Fixed item',
					minEffort: 1.0,
					expectedEffort: 2.0,
					maxEffort: 4.0
				}
			]
		}
	});
	expect(putRes.status()).toBe(200);
	return estimationId;
}

/** Open the draft editor and download via the export menu. Returns the file. */
async function downloadExport(
	page: import('@playwright/test').Page,
	estimationId: string,
	label: RegExp
): Promise<{ filename: string; body: Buffer }> {
	await page.goto(`/estimations/${estimationId}/versions/draft`);
	await page.waitForLoadState('networkidle');

	await page.locator('details > summary', { hasText: /^Export$/ }).click();
	const downloadPromise = page.waitForEvent('download');
	await page.getByRole('button', { name: label }).click();
	const download = await downloadPromise;
	const path = await download.path();
	return { filename: download.suggestedFilename(), body: readFileSync(path) };
}

test('bucket estimation exports a real xlsx, not a 401 JSON body', async ({ page, request }) => {
	const estimationId = await seedBucketDraft(request);
	const { filename, body } = await downloadExport(page, estimationId, /Excel/);

	expect(filename).toBe('estimation-draft.xlsx');
	// xlsx is a zip container: "PK\x03\x04". A JSON error body starts with "{".
	expect(body.subarray(0, 2).toString('latin1')).toBe('PK');
	expect(body.length).toBeGreaterThan(1000);
});

test('bucket estimation exports a real csv, not a 401 JSON body', async ({ page, request }) => {
	const estimationId = await seedBucketDraft(request);
	const { filename, body } = await downloadExport(page, estimationId, /CSV/);

	expect(filename).toBe('estimation-draft.csv');
	const text = body.toString('utf8');
	expect(text.split('\n')[0]).toContain('Path,Group,Description');
	expect(text).toContain('Sample item');
});

test('pert estimation exports a real xlsx, not a 401 JSON body', async ({ page, request }) => {
	const estimationId = await seedPertDraft(request);
	const { filename, body } = await downloadExport(page, estimationId, /Excel/);

	expect(filename).toBe('estimation-draft.xlsx');
	expect(body.subarray(0, 2).toString('latin1')).toBe('PK');
});
