import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { BucketEditor } from './pages';

/**
 * Accompanying work (h/Woche) inside a bucket + sampled estimation (task-187).
 *
 * Before it, such a leaf rendered as if it were bucketed — a bucket select, a
 * sample checkbox, "—" for its effort and no unit — and could not be created.
 * Now the editor's type column switches a leaf between the two kinds, an
 * accompanying leaf edits its own triple and needs a phase, and the bucket view
 * collects it in its own row.
 *
 * Seeding goes through the API like the other bucket specs; only the behaviour
 * under test is driven through the UI.
 */

test.use({ viewport: { width: 1400, height: 1000 } });

const AUTH = { Authorization: 'Dev dev-admin' } as const;
const JSON_HEADERS = { 'Content-Type': 'application/json', ...AUTH } as const;

const PHASES = [
	{ name: 'Konzeption', abbreviation: 'KO', durationWeeks: 3 },
	{ name: 'Umsetzung', abbreviation: 'UM', durationWeeks: 12 }
];

async function createBucketEstimation(request: APIRequestContext, label: string): Promise<string> {
	const projectRes = await request.post('/api/projects', {
		headers: JSON_HEADERS,
		data: { name: `E2E Accompanying ${label} ${Date.now()}` }
	});
	expect(projectRes.status()).toBe(201);
	const project = await projectRes.json();

	const estRes = await request.post(`/api/projects/${project.id}/estimations`, {
		headers: JSON_HEADERS,
		data: { offer: `E2E-AC-${label}-${Date.now()}`, method: 'BUCKET_SAMPLED_PERT' }
	});
	expect(estRes.status()).toBe(201);
	const estimationId = (await estRes.json()).id;

	const versionRes = await request.post(`/api/estimations/${estimationId}/versions`, {
		headers: JSON_HEADERS
	});
	expect(versionRes.status()).toBe(201);
	return estimationId;
}

async function putDraft(request: APIRequestContext, estimationId: string, data: object) {
	const res = await request.put(`/api/estimations/${estimationId}/versions/draft`, {
		headers: JSON_HEADERS,
		data
	});
	expect(res.status()).toBe(200);
}

async function openHierarchyView(page: Page, estimationId: string) {
	await page.goto(`/estimations/${estimationId}/versions/draft`);
	await page.waitForLoadState('networkidle');
	await page.getByTestId('bucket-view-toggle-hierarchy').click();
	await expect(page.getByTestId('bucket-view-toggle-hierarchy')).toHaveAttribute(
		'aria-pressed',
		'true'
	);
}

test('a bucketed leaf becomes accompanying work and keeps it across a reload', async ({
	page,
	request
}) => {
	const estimationId = await createBucketEstimation(request, 'switch');
	const bucketId = crypto.randomUUID();
	const leafId = crypto.randomUUID();
	await putDraft(request, estimationId, {
		phases: PHASES,
		buckets: [{ id: bucketId, position: 0, label: 'M' }],
		roots: [
			{
				type: 'BUCKETED',
				logicalId: leafId,
				description: 'Projektbegleitung',
				bucketId,
				isSample: false
			}
		]
	});

	const editor = new BucketEditor(page);
	await openHierarchyView(page, estimationId);
	await editor.expectLeafType(leafId, 'BUCKETED');

	// Switch to accompanying work: no bucket picker, no sample checkbox, an
	// editable triple — and, without a phase, the explanation instead of a 0.
	await editor.setLeafType(leafId, 'TIME_RELATIVE');
	await expect(editor.leafBucketSelect(leafId)).toHaveCount(0);
	await expect(editor.leafRow(leafId).locator('input[type="checkbox"]')).toHaveCount(0);
	await expect(editor.timeRelativeOptimistic(leafId)).toBeEditable();
	await editor.expectNeedsPhase(leafId);

	await editor.fillTimeRelativeOptimistic(leafId, '0.5');
	await editor.leafPhaseSelect(leafId).selectOption('UM');
	await editor.expectNoNeedsPhase(leafId);

	// Autosave is debounced; the reload proves the kind, the number and the
	// phase reached the backend rather than only the in-memory model.
	await editor.expectSaved();
	await openHierarchyView(page, estimationId);
	await editor.expectLeafType(leafId, 'TIME_RELATIVE');
	await expect(editor.timeRelativeOptimistic(leafId)).toHaveValue('0.5');
	await expect(editor.leafPhaseSelect(leafId)).toHaveValue('UM');
});

test('accompanying work has its own bucket-view row, and switching back takes the first bucket', async ({
	page,
	request
}) => {
	const estimationId = await createBucketEstimation(request, 'back');
	const bucketId = crypto.randomUUID();
	const leafId = crypto.randomUUID();
	await putDraft(request, estimationId, {
		phases: PHASES,
		buckets: [{ id: bucketId, position: 0, label: 'M' }],
		roots: [
			{
				type: 'TIME_RELATIVE',
				logicalId: leafId,
				description: 'Projektbegleitung',
				unit: 'h/Woche',
				minEffort: 0.5,
				expectedEffort: 1,
				maxEffort: 2,
				phaseAbbreviation: 'UM'
			}
		]
	});

	const editor = new BucketEditor(page);
	await page.goto(`/estimations/${estimationId}/versions/draft`);
	await page.waitForLoadState('networkidle');
	await page.getByTestId('bucket-view-toggle-bucket').click();
	await editor.expectAccompanyingRow();

	await page.getByTestId('bucket-view-toggle-hierarchy').click();
	await editor.setLeafType(leafId, 'BUCKETED');
	await expect(editor.leafBucketSelect(leafId)).toHaveValue(bucketId);

	await page.getByTestId('bucket-view-toggle-bucket').click();
	await expect(editor.accompanyingRow).toHaveCount(0);
	await editor.expectSaved();
});

test('without any bucket, a leaf cannot be switched to "Bucket"', async ({ page, request }) => {
	// Seeded with accompanying work only. Reaching "no bucket" via "add item"
	// would create a BUCKETED leaf without a bucket, which the autosave rejects
	// (400) before this test could assert anything.
	const estimationId = await createBucketEstimation(request, 'nobucket');
	const leafId = crypto.randomUUID();
	await putDraft(request, estimationId, {
		phases: PHASES,
		roots: [
			{
				type: 'TIME_RELATIVE',
				logicalId: leafId,
				description: 'Projektbegleitung',
				unit: 'h/Woche',
				minEffort: 0.5,
				expectedEffort: 1,
				maxEffort: 2,
				phaseAbbreviation: 'UM'
			}
		]
	});

	const editor = new BucketEditor(page);
	await openHierarchyView(page, estimationId);

	// The route seeds the XS…XL defaults for a draft with no buckets, so the
	// zero-bucket state is reached by deleting them.
	const names = editor.bucketNames;
	await expect(names).not.toHaveCount(0);
	for (let left = await names.count(); left > 0; left--) {
		await editor.deleteBucket(0);
		await expect(names).toHaveCount(left - 1);
	}

	await expect(
		editor.leafTypeSelect(leafId).locator('option[value="BUCKETED"]')
	).toBeDisabled();
	await editor.expectLeafType(leafId, 'TIME_RELATIVE');
});
