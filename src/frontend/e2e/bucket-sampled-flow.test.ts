import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { readFileSync } from 'node:fs';

/**
 * The bucket + sampled method end to end (task-108): the gap between the specs
 * that already cover parts of it.
 *
 * `create-estimation.spec.ts` drives the method picker, `bucket-views.test.ts`
 * the two projections, `bucket-hierarchy.test.ts` the nested-group rendering
 * and `export.test.ts` the DRAFT download. What none of them touch, and what
 * this spec pins, is the actual estimating loop: administering buckets,
 * assigning leaves to them, marking samples, and — the point of the whole
 * method — a NON-sample leaf inheriting its bucket's average. Then the same
 * numbers surviving a submit, and the export of a SUBMITTED version carrying
 * the method's own columns (the dispatch task-107 added).
 *
 * Seeding goes through the API like every other bucket spec; only the
 * behaviour under test is driven through the UI, which is what keeps each test
 * inside the 30-second per-test budget `playwright.config.ts` allows.
 */

// Four panels stack above the table and three columns are `collapsible` —
// the same tall viewport the other two bucket specs use.
test.use({ viewport: { width: 1400, height: 1000 } });

const AUTH = { Authorization: 'Dev dev-admin' } as const;
const JSON_HEADERS = { 'Content-Type': 'application/json', ...AUTH } as const;

/** The only test id TreeTable emits — there is none per cell (task-108). */
const row = (id: string) => `[data-testid="tt-row-${id}"]`;

/** Project + BUCKET_SAMPLED_PERT estimation + an empty draft. Deliberately
 *  does NOT PUT a draft: the route seeds the XS…XL defaults only when a bucket
 *  draft loads with zero buckets. */
async function createBucketEstimation(
	request: APIRequestContext,
	label: string
): Promise<string> {
	const projectRes = await request.post('/api/projects', {
		headers: JSON_HEADERS,
		data: { name: `E2E BucketFlow ${label} ${Date.now()}` }
	});
	expect(projectRes.status()).toBe(201);
	const project = await projectRes.json();

	const estRes = await request.post(`/api/projects/${project.id}/estimations`, {
		headers: JSON_HEADERS,
		data: { offer: `E2E-BF-${label}-${Date.now()}`, method: 'BUCKET_SAMPLED_PERT' }
	});
	expect(estRes.status()).toBe(201);
	const estimationId = (await estRes.json()).id;

	const versionRes = await request.post(`/api/estimations/${estimationId}/versions`, {
		headers: JSON_HEADERS
	});
	expect(versionRes.status()).toBe(201);
	return estimationId;
}

/**
 * A draft whose four leaves all sit in bucket A with no sample among them —
 * the starting point the UI then edits. A BUCKETED leaf with a null `bucketId`
 * is rejected by the backend (400), so the seed must name a bucket even though
 * the test reassigns half of them afterwards.
 *
 * `stdDevFactor: 0` keeps the risk surcharge out of the picture; the `mean`
 * column is the PERT mean either way, but it makes the fixture unambiguous.
 */
async function seedUnsampledDraft(request: APIRequestContext) {
	const estimationId = await createBucketEstimation(request, 'edit');
	const bucketA = crypto.randomUUID();
	const bucketB = crypto.randomUUID();
	const groupId = crypto.randomUUID();
	const leaves = [0, 1, 2, 3].map(() => crypto.randomUUID());

	const putRes = await request.put(`/api/estimations/${estimationId}/versions/draft`, {
		headers: JSON_HEADERS,
		data: {
			stdDevFactor: 0.0,
			buckets: [
				{ id: bucketA, position: 0, label: 'A' },
				{ id: bucketB, position: 1, label: 'B' }
			],
			roots: [
				{
					type: 'GROUP',
					logicalId: groupId,
					title: 'Arbeitspaket',
					children: leaves.map((logicalId, i) => ({
						type: 'BUCKETED',
						logicalId,
						description: `Leaf ${i + 1}`,
						bucketId: bucketA,
						isSample: false
					}))
				}
			]
		}
	});
	expect(putRes.status()).toBe(200);
	return { estimationId, bucketA, bucketB, leaves };
}

/**
 * A draft already carrying one sample (1/2/3 → PERT mean 2.0) and one
 * non-sample in the same bucket, so the derived value exists without any UI
 * editing. Used by the submit and export tests, which are about what happens
 * AFTER the numbers are in.
 */
async function seedSampledDraft(request: APIRequestContext, label: string) {
	const estimationId = await createBucketEstimation(request, label);
	const bucketId = crypto.randomUUID();
	const sampleId = crypto.randomUUID();
	const derivedId = crypto.randomUUID();

	const putRes = await request.put(`/api/estimations/${estimationId}/versions/draft`, {
		headers: JSON_HEADERS,
		data: {
			stdDevFactor: 0.0,
			buckets: [{ id: bucketId, position: 0, label: 'M' }],
			roots: [
				{
					type: 'BUCKETED',
					logicalId: sampleId,
					description: 'Stichprobe',
					bucketId,
					isSample: true,
					minEffort: 1,
					expectedEffort: 2,
					maxEffort: 3
				},
				{
					type: 'BUCKETED',
					logicalId: derivedId,
					description: 'Abgeleitet',
					bucketId,
					isSample: false
				}
			]
		}
	});
	expect(putRes.status()).toBe(200);
	return { estimationId, sampleId, derivedId };
}

/** The editor opens in the BUCKET view, which has no group affordance and
 *  starts every bucket row collapsed. The hierarchy view is where leaves are
 *  individually addressable. */
async function openHierarchyView(page: Page, url: string) {
	await page.goto(url);
	await page.waitForLoadState('networkidle');
	await page.getByTestId('bucket-view-toggle-hierarchy').click();
	await expect(page.getByTestId('bucket-view-toggle-hierarchy')).toHaveAttribute(
		'aria-pressed',
		'true'
	);
}

/** The derived/PERT mean cell of a LEAF row. The critical-path dot shares
 *  `text-brand-green` but not `tabular-nums`, so both classes are required. */
const meanOf = (page: Page, leafId: string) =>
	page.locator(`${row(leafId)} span.text-brand-green.tabular-nums`);

test('the bucket panel seeds XS…XL and supports add, rename and delete', async ({
	page,
	request
}) => {
	const estimationId = await createBucketEstimation(request, 'panel');

	await page.goto(`/estimations/${estimationId}/versions/draft`);
	await page.waitForLoadState('networkidle');

	// A fresh bucket draft is seeded client-side with the five default sizes so
	// the estimator can assign items immediately. `toHaveValues` is for a
	// multi-select, not a list of text inputs, so read the labels out directly.
	const names = page.getByLabel('Bucket-Name');
	const labels = () =>
		names.evaluateAll((els) => els.map((e) => (e as HTMLInputElement).value));

	await expect.poll(labels).toEqual(['XS', 'S', 'M', 'L', 'XL']);

	// Add: the new bucket arrives under the placeholder label.
	await page.getByRole('button', { name: '+ Bucket' }).click();
	await expect(names).toHaveCount(6);
	await expect(names.nth(5)).toHaveValue('Neu');

	// Rename.
	await names.nth(5).fill('XXL');
	await expect.poll(labels).toEqual(['XS', 'S', 'M', 'L', 'XL', 'XXL']);

	// Delete it again — the five defaults survive untouched.
	await page.getByLabel('Bucket löschen').nth(5).click();
	await expect.poll(labels).toEqual(['XS', 'S', 'M', 'L', 'XL']);
});

test('a non-sample leaf inherits the average of its bucket', async ({ page, request }) => {
	const { estimationId, bucketB, leaves } = await seedUnsampledDraft(request);
	const [leaf1, leaf2, leaf3, leaf4] = leaves;

	await openHierarchyView(page, `/estimations/${estimationId}/versions/draft`);

	// Move half the leaves into bucket B. The row <select> has no accessible
	// name, but a LEAF row contains no descendant rows, so scoping by row id
	// makes it unambiguous.
	await page.locator(`${row(leaf3)} select`).selectOption(bucketB);
	await page.locator(`${row(leaf4)} select`).selectOption(bucketB);

	// One sample per bucket. The three-point inputs render only once `isSample`
	// is set, so the checkbox has to come first.
	await page.locator(`${row(leaf1)} input[type="checkbox"]`).check();
	const a = page.locator(`${row(leaf1)} input[type="number"]`);
	await a.nth(0).fill('1');
	await a.nth(1).fill('2');
	await a.nth(2).fill('3');

	await page.locator(`${row(leaf3)} input[type="checkbox"]`).check();
	const b = page.locator(`${row(leaf3)} input[type="number"]`);
	await b.nth(0).fill('2');
	await b.nth(1).fill('4');
	await b.nth(2).fill('6');

	// PERT means: (1 + 4·2 + 3)/6 = 2 and (2 + 4·4 + 6)/6 = 4. Each bucket has a
	// single sample, so its average IS that sample's mean — which is what the
	// non-sample rows must now show. Formatting is German: playwright.config.ts
	// pins locale de-DE and the grid renders through $lib/format.ts.
	await expect(meanOf(page, leaf1)).toHaveText('2,00');
	await expect(meanOf(page, leaf2)).toHaveText('2,00');
	await expect(meanOf(page, leaf3)).toHaveText('4,00');
	await expect(meanOf(page, leaf4)).toHaveText('4,00');

	// The derivation is a projection, not a copy: a non-sample leaf keeps its
	// own inputs empty (the em dash) rather than acquiring the sample's triple.
	await expect(page.locator(`${row(leaf2)} input[type="number"]`)).toHaveCount(0);
});

test('submitting freezes the derived numbers into a read-only version', async ({
	page,
	request
}) => {
	const { estimationId, sampleId, derivedId } = await seedSampledDraft(request, 'submit');

	await page.goto(`/estimations/${estimationId}/versions/draft`);
	await page.waitForLoadState('networkidle');
	await page.getByRole('button', { name: 'Festschreiben' }).click();

	// Submit navigates back to the estimation detail.
	await expect(page).toHaveURL(new RegExp(`/estimations/${estimationId}$`));

	await openHierarchyView(page, `/estimations/${estimationId}/versions/1`);
	await expect(page.getByText('Festgeschrieben — schreibgeschützt')).toBeVisible();

	// The sample keeps its raw PT triple (rendered verbatim when read-only)…
	const raw = page.locator(`${row(sampleId)} span.tabular-nums`);
	await expect(raw.nth(0)).toHaveText('1');
	await expect(raw.nth(1)).toHaveText('2');
	await expect(raw.nth(2)).toHaveText('3');

	// …and both rows still carry the derived mean.
	await expect(meanOf(page, sampleId)).toHaveText('2,00');
	await expect(meanOf(page, derivedId)).toHaveText('2,00');

	// Read-only really is read-only. The text and number cells collapse to
	// spans; the sample checkbox is the one input that still renders, and it
	// renders DISABLED rather than disappearing — so a reader can still see
	// which rows were the samples.
	await expect(page.locator(`${row(sampleId)} input[type="text"]`)).toHaveCount(0);
	await expect(page.locator(`${row(sampleId)} input[type="number"]`)).toHaveCount(0);
	await expect(page.locator(`${row(sampleId)} input[type="checkbox"]`)).toBeDisabled();
	await expect(page.locator(`${row(sampleId)} input[type="checkbox"]`)).toBeChecked();
});

test('exporting a submitted version carries the method’s own columns', async ({
	page,
	request
}) => {
	const { estimationId } = await seedSampledDraft(request, 'export');
	const submitRes = await request.post(
		`/api/estimations/${estimationId}/versions/draft/submit`,
		{ headers: JSON_HEADERS }
	);
	expect(submitRes.ok()).toBe(true);

	await page.goto(`/estimations/${estimationId}/versions/1`);
	await page.waitForLoadState('networkidle');

	async function download(label: RegExp) {
		await page.locator('details > summary', { hasText: /^Export$/ }).click();
		const downloadPromise = page.waitForEvent('download');
		await page.getByRole('button', { name: label }).click();
		const file = await downloadPromise;
		const path = await file.path();
		return { filename: file.suggestedFilename(), body: readFileSync(path) };
	}

	// `estimation-v1`, not `estimation-1`: the name comes from the backend's
	// Content-Disposition (`v$versionNumber` for a submitted snapshot), which
	// `downloadResponse` prefers over the route's own fallback string.
	const csv = await download(/CSV/);
	expect(csv.filename).toBe('estimation-v1.csv');
	// The header is the neutral columns with the METHOD's five spliced in —
	// BucketMethodModule.exportColumnHeaders() reached through CsvExporter
	// (task-107). A PERT estimation puts three different ones in that slot.
	expect(csv.body.toString('utf8').split('\n')[0]).toBe(
		'Path,Group,Description,Bucket,Is Sample,Optimistic,Likely,Pessimistic,Mean,OfferPT,Node type'
	);

	// The xlsx is fed by the same exportColumnHeaders() call, so its columns
	// need no separate assertion — only that real spreadsheet bytes arrive
	// rather than a JSON error body. There is no xlsx parser in this project
	// and task-108 deliberately does not add one.
	const xlsx = await download(/Excel/);
	expect(xlsx.filename).toBe('estimation-v1.xlsx');
	expect(xlsx.body.subarray(0, 2).toString('latin1')).toBe('PK');
});
