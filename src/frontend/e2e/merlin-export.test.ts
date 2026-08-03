import { test, expect, type APIRequestContext } from '@playwright/test';
import { readFileSync } from 'node:fs';

/**
 * Merlin export (task-133): the estimation's offerPT is written back into a
 * COPY of an uploaded Merlin document. Covers the happy path (a download) and
 * the structure-drift decision the user has to make.
 */

const SAMPLE = 'planning/inputdata/MerlinDemoProject.mproject/state.sql';
const JSON_HEADERS = { 'Content-Type': 'application/json', Authorization: 'Dev dev-admin' } as const;

async function importedEstimation(request: APIRequestContext): Promise<string> {
	const project = await (
		await request.post('/api/projects', {
			headers: JSON_HEADERS,
			data: { name: `E2E MerlinExport ${Date.now()}` }
		})
	).json();

	const estimation = await (
		await request.post(`/api/projects/${project.id}/estimations`, {
			headers: JSON_HEADERS,
			data: { offer: `E2E-MX-${Date.now()}` }
		})
	).json();

	const imported = await request.post(
		`/api/estimations/${estimation.id}/versions/import/merlin`,
		{
			headers: { Authorization: 'Dev dev-admin' },
			multipart: {
				file: { name: 'state.sql', mimeType: 'application/octet-stream', buffer: sampleBuffer() }
			}
		}
	);
	expect(imported.status()).toBe(201);
	return estimation.id;
}

function sampleBuffer(): Buffer {
	return readFileSync(`../../${SAMPLE}`);
}

test('exporting to Merlin downloads the modified copy', async ({ page, request }) => {
	const estimationId = await importedEstimation(request);
	await page.goto(`/estimations/${estimationId}`);
	await page.waitForLoadState('networkidle');

	const downloadPromise = page.waitForEvent('download');
	await page.getByTestId('merlin-export-input').setInputFiles(`../../${SAMPLE}`);

	const download = await downloadPromise;
	expect(download.suggestedFilename()).toContain('estimated');
});

test('exporting still works after the draft has been submitted', async ({ page, request }) => {
	const estimationId = await importedEstimation(request);

	// Submitting CONSUMES the draft. Exporting must then fall back to the latest
	// submitted version — this is the moment the estimate is finished and you
	// actually want it back in Merlin, and it used to 404.
	const submitted = await request.post(
		`/api/estimations/${estimationId}/versions/draft/submit`,
		{ headers: { Authorization: 'Dev dev-admin' } }
	);
	expect(submitted.status()).toBe(200);

	await page.goto(`/estimations/${estimationId}`);
	await page.waitForLoadState('networkidle');

	const exportResponse = page.waitForResponse((r) => r.url().includes('/export/merlin'));
	const downloadPromise = page.waitForEvent('download');
	await page.getByTestId('merlin-export-input').setInputFiles(`../../${SAMPLE}`);

	const res = await exportResponse;
	expect(res.status()).toBe(200);
	// The request targeted the submitted version, not "draft".
	expect(res.url()).toContain('/versions/1/export/merlin');
	expect((await downloadPromise).suggestedFilename()).toContain('estimated');
	await expect(page.locator('[role="alert"]')).toHaveCount(0);
});

test('a drifted structure asks the user before overwriting Merlin', async ({ page, request }) => {
	const estimationId = await importedEstimation(request);

	// Drift the estimation away from the imported WBS.
	const put = await request.put(`/api/estimations/${estimationId}/versions/draft`, {
		headers: JSON_HEADERS,
		data: {
			roots: [
				{
					type: 'FIXED',
					description: 'Added after the import',
					minEffort: 1,
					expectedEffort: 2,
					maxEffort: 3
				}
			]
		}
	});
	expect(put.status()).toBe(200);

	await page.goto(`/estimations/${estimationId}`);
	await page.waitForLoadState('networkidle');
	await page.getByTestId('merlin-export-input').setInputFiles(`../../${SAMPLE}`);

	// The 409 opens the decision dialog rather than failing the export.
	const dialog = page.getByRole('dialog', { name: 'Struktur weicht ab' });
	await expect(dialog).toBeVisible();
	await expect(dialog).toContainText('Added after the import');

	// Confirming overwrites the Merlin structure and completes the download.
	const downloadPromise = page.waitForEvent('download');
	await dialog.getByRole('button', { name: 'Struktur überschreiben' }).click();
	const download = await downloadPromise;
	expect(download.suggestedFilename()).toContain('estimated');
});
