import { test, expect, type APIRequestContext } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { ImportExportMenu, MerlinMergeReportDialog, MerlinStructureDialog } from './pages';

/**
 * Merge into Merlin (task-133, renamed in task-223): the estimation's changed
 * efforts and its dependencies are merged into a COPY of an uploaded Merlin
 * document, which is downloaded, and a report of the changes opens. Covers the
 * happy path, the structure-drift decision the user has to make, an added
 * dependency, and a second merge that has nothing left to change.
 */

const SAMPLE = 'planning/inputdata/MerlinDemoProject.mproject/state.sql';
const JSON_HEADERS = { 'Content-Type': 'application/json', Authorization: 'Dev dev-admin' } as const;

async function importedEstimation(request: APIRequestContext): Promise<string> {
	const project = await (
		await request.post('/api/projects', {
			headers: JSON_HEADERS,
			data: { name: `E2E MerlinMerge ${Date.now()}` }
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

test('merging into Merlin downloads the merged copy and reports it', async ({ page, request }) => {
	const estimationId = await importedEstimation(request);
	await page.goto(`/estimations/${estimationId}`);
	await page.waitForLoadState('networkidle');
	await expect(new ImportExportMenu(page).mergeMerlinButton).toBeEnabled();

	const mergeResponse = page.waitForResponse((r) => r.url().includes('/merge/merlin'));
	const downloadPromise = page.waitForEvent('download');
	await page.getByTestId('merlin-export-input').setInputFiles(`../../${SAMPLE}`);

	expect((await mergeResponse).status()).toBe(200);
	const download = await downloadPromise;
	expect(download.suggestedFilename()).toContain('merged');
	await new MerlinMergeReportDialog(page).expectVisible();
});

test('merging still works after the draft has been submitted', async ({ page, request }) => {
	const estimationId = await importedEstimation(request);

	// Submitting CONSUMES the draft. Merging must then fall back to the latest
	// submitted version — this is the moment the estimate is finished and you
	// actually want it back in Merlin, and it used to 404.
	const submitted = await request.post(
		`/api/estimations/${estimationId}/versions/draft/submit`,
		{ headers: { Authorization: 'Dev dev-admin' } }
	);
	expect(submitted.status()).toBe(200);

	await page.goto(`/estimations/${estimationId}`);
	await page.waitForLoadState('networkidle');

	const mergeResponse = page.waitForResponse((r) => r.url().includes('/merge/merlin'));
	const downloadPromise = page.waitForEvent('download');
	await page.getByTestId('merlin-export-input').setInputFiles(`../../${SAMPLE}`);

	const res = await mergeResponse;
	expect(res.status()).toBe(200);
	// The request targeted the submitted version, not "draft".
	expect(res.url()).toContain('/versions/1/merge/merlin');
	expect((await downloadPromise).suggestedFilename()).toContain('merged');
	await expect(page.locator('[role="alert"]')).toHaveCount(0);
	await new MerlinMergeReportDialog(page).expectVisible();
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

	// The 409 opens the decision dialog rather than failing the merge.
	const structureDialog = new MerlinStructureDialog(page);
	const dialog = structureDialog.dialog;
	await expect(dialog).toBeVisible();
	await expect(dialog).toContainText('Added after the import');

	// Confirming overwrites the Merlin structure and completes the download.
	const downloadPromise = page.waitForEvent('download');
	await structureDialog.overwrite();
	const download = await downloadPromise;
	expect(download.suggestedFilename()).toContain('merged');

	// The report names the activity the overwrite created.
	const report = new MerlinMergeReportDialog(page);
	await report.expectVisible();
	await expect(report.section('activities-added')).toContainText('Added after the import');
});

type DraftNode = { logicalId: string; title?: string | null };
type Draft = { roots: DraftNode[]; dependencies: { fromLogicalId: string; toLogicalId: string }[] };

test('merging adds a Pythia dependency and reports it', async ({ page, request }) => {
	const estimationId = await importedEstimation(request);

	// Add Initialisation → Closure (the demo file has no such edge) to the
	// imported draft's own edges.
	const draft: Draft = await (
		await request.get(`/api/estimations/${estimationId}/versions/draft`, { headers: JSON_HEADERS })
	).json();
	const byTitle = (title: string) => {
		const node = draft.roots.find((r) => r.title === title);
		if (!node) throw new Error(`no root titled ${title}`);
		return node.logicalId;
	};
	const put = await request.put(`/api/estimations/${estimationId}/versions/draft`, {
		headers: JSON_HEADERS,
		data: {
			dependencies: [
				...draft.dependencies,
				{ fromLogicalId: byTitle('Initialisation'), toLogicalId: byTitle('Closure') }
			]
		}
	});
	expect(put.status()).toBe(200);

	await page.goto(`/estimations/${estimationId}`);
	await page.waitForLoadState('networkidle');
	const downloadPromise = page.waitForEvent('download');
	await page.getByTestId('merlin-export-input').setInputFiles(`../../${SAMPLE}`);
	await downloadPromise;

	const report = new MerlinMergeReportDialog(page);
	await report.expectVisible();
	const added = report.section('dependencies-added');
	await expect(added).toContainText('Initialisation');
	await expect(added).toContainText('Closure');
});

test('merging the merged file again reports no changes', async ({ page, request }) => {
	const estimationId = await importedEstimation(request);
	await page.goto(`/estimations/${estimationId}`);
	await page.waitForLoadState('networkidle');
	const report = new MerlinMergeReportDialog(page);

	const firstDownload = page.waitForEvent('download');
	await page.getByTestId('merlin-export-input').setInputFiles(`../../${SAMPLE}`);
	const merged = await (await firstDownload).path();
	await report.expectVisible();
	await report.close();

	const secondDownload = page.waitForEvent('download');
	await page.getByTestId('merlin-export-input').setInputFiles(merged);
	await secondDownload;
	await report.expectEmpty();
});
