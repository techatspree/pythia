import { test, expect, type Page, type APIRequestContext } from '@playwright/test';

/**
 * The version editor's whole-estimation summary panel (task-139).
 *
 * Two things are pinned here:
 *  - a BUCKET_SAMPLED_PERT estimation gets an overall total at all (that editor
 *    aggregates only per bucket, and had no estimate-wide number),
 *  - the total counts every leaf ONCE. The PERT grid footer used to reduce
 *    `calcMap.values()`, which holds an entry for every node INCLUDING groups,
 *    so a grouped estimation was double-counted.
 *
 * Both seeds use `stdDevFactor: 0` and no effort drivers, so the risk and driver
 * surcharges vanish and offerPT equals the plain PERT mean — the expected totals
 * below are therefore exact, not approximations of the app's own arithmetic.
 */

const AUTH = { Authorization: 'Dev dev-admin' } as const;
const JSON_HEADERS = { 'Content-Type': 'application/json', ...AUTH } as const;

const DAILY_RATE = 800;
const SALES_SURCHARGE = 0.1;

async function createEstimation(
	request: APIRequestContext,
	method: 'THREE_POINT_PERT' | 'BUCKET_SAMPLED_PERT'
): Promise<string> {
	const projectRes = await request.post('/api/projects', {
		headers: JSON_HEADERS,
		data: { name: `E2E Summary ${method} ${Date.now()}` }
	});
	expect(projectRes.status()).toBe(201);
	const project = await projectRes.json();

	const estRes = await request.post(`/api/projects/${project.id}/estimations`, {
		headers: JSON_HEADERS,
		data: { offer: `E2E-SUM-${Date.now()}`, method }
	});
	expect(estRes.status()).toBe(201);
	const estimationId = (await estRes.json()).id;

	const versionRes = await request.post(`/api/estimations/${estimationId}/versions`, {
		headers: JSON_HEADERS
	});
	expect(versionRes.status()).toBe(201);
	return estimationId;
}

/** A PERT draft whose leaves sit inside a GROUP (plus one at root level), so a
 *  total that also summed group rows would come out too high.
 *  Means: 2 + 4 (in the group) + 6 (root) = 12 offer PT. */
async function seedPert(request: APIRequestContext): Promise<string> {
	const estimationId = await createEstimation(request, 'THREE_POINT_PERT');

	const putRes = await request.put(`/api/estimations/${estimationId}/versions/draft`, {
		headers: JSON_HEADERS,
		data: {
			stdDevFactor: 0.0,
			dailyRate: DAILY_RATE,
			salesSurcharge: SALES_SURCHARGE,
			effortDrivers: [],
			roots: [
				{
					type: 'GROUP',
					logicalId: crypto.randomUUID(),
					title: 'Grouped work',
					children: [
						{
							type: 'FIXED',
							logicalId: crypto.randomUUID(),
							description: 'Nested one',
							minEffort: 1.0,
							expectedEffort: 2.0,
							maxEffort: 3.0
						},
						{
							type: 'FIXED',
							logicalId: crypto.randomUUID(),
							description: 'Nested two',
							minEffort: 2.0,
							expectedEffort: 4.0,
							maxEffort: 6.0
						}
					]
				},
				{
					type: 'FIXED',
					logicalId: crypto.randomUUID(),
					description: 'Root level',
					minEffort: 3.0,
					expectedEffort: 6.0,
					maxEffort: 9.0
				}
			]
		}
	});
	expect(putRes.status()).toBe(200);
	return estimationId;
}

/** A bucket draft: bucket 1 holds a sample (mean 2) plus a non-sample that
 *  inherits that mean, bucket 2 holds a sample (mean 4). Total 2+2+4 = 8. */
async function seedBucket(request: APIRequestContext): Promise<{
	estimationId: string;
	b1: string;
	b2: string;
}> {
	const estimationId = await createEstimation(request, 'BUCKET_SAMPLED_PERT');
	const b1 = crypto.randomUUID();
	const b2 = crypto.randomUUID();

	const putRes = await request.put(`/api/estimations/${estimationId}/versions/draft`, {
		headers: JSON_HEADERS,
		data: {
			stdDevFactor: 0.0,
			dailyRate: DAILY_RATE,
			salesSurcharge: SALES_SURCHARGE,
			effortDrivers: [],
			buckets: [
				{ id: b1, position: 0, label: 'Small' },
				{ id: b2, position: 1, label: 'Large' }
			],
			roots: [
				{
					type: 'GROUP',
					logicalId: crypto.randomUUID(),
					title: 'WBS',
					children: [
						{
							type: 'BUCKETED',
							logicalId: crypto.randomUUID(),
							description: 'Small sample',
							bucketId: b1,
							isSample: true,
							minEffort: 1.0,
							expectedEffort: 2.0,
							maxEffort: 3.0
						},
						{
							type: 'BUCKETED',
							logicalId: crypto.randomUUID(),
							description: 'Small non-sample',
							bucketId: b1,
							isSample: false
						}
					]
				},
				{
					type: 'BUCKETED',
					logicalId: crypto.randomUUID(),
					description: 'Large sample',
					bucketId: b2,
					isSample: true,
					minEffort: 2.0,
					expectedEffort: 4.0,
					maxEffort: 6.0
				}
			]
		}
	});
	expect(putRes.status()).toBe(200);
	return { estimationId, b1, b2 };
}

/** Playwright pins `locale: 'de-DE'`, so rendered numbers use '.' as the
 *  thousands separator and ',' as the decimal mark. */
function parseDe(text: string): number {
	return parseFloat(text.trim().replace(/\./g, '').replace(',', '.'));
}

async function readNumber(page: Page, testId: string): Promise<number> {
	return parseDe(await page.getByTestId(testId).innerText());
}

/**
 * The text of one column cell of a TreeTable row. Cells carry no key of their
 * own, so they are addressed positionally: the row's first child is the grid,
 * whose children are the drag handle (editable rows only) followed by one div
 * per column in `columns` order.
 */
async function cellText(page: Page, rowId: string, columnIndex: number): Promise<string> {
	const nth = columnIndex + 2; // 1-based, and the drag handle occupies slot 1
	return page
		.locator(`[data-testid="tt-row-${rowId}"] > div:first-child > div:nth-child(${nth})`)
		.innerText();
}

function collectPageErrors(page: Page): string[] {
	const errors: string[] = [];
	page.on('pageerror', (e) => errors.push(e.message));
	return errors;
}

test('summary panel totals a grouped PERT estimation without double-counting groups', async ({
	page,
	request
}) => {
	const errors = collectPageErrors(page);
	const estimationId = await seedPert(request);

	await page.goto(`/estimations/${estimationId}/versions/draft`);
	await page.waitForLoadState('networkidle');

	const summary = page.getByTestId('version-summary');
	await expect(summary).toBeVisible();

	// The panel sits ABOVE the editor grid.
	const summaryBox = await summary.boundingBox();
	const gridBox = await page.getByTestId('grid-total.offerPT').boundingBox();
	expect(summaryBox!.y).toBeLessThan(gridBox!.y);

	// 2 + 4 + 6 = 12. Summing group rows as well would yield 24.
	expect(await readNumber(page, 'version-summary.offerPT')).toBeCloseTo(12, 2);
	expect(await readNumber(page, 'version-summary.developmentCost')).toBeCloseTo(
		12 * DAILY_RATE,
		2
	);
	expect(await readNumber(page, 'version-summary.totalOfferPrice')).toBeCloseTo(
		12 * DAILY_RATE * (1 + SALES_SURCHARGE),
		2
	);

	// The grid footer now reads the same domain totals, so the two agree.
	expect(await readNumber(page, 'grid-total.offerPT')).toBeCloseTo(12, 2);
	expect(await readNumber(page, 'grid-total.cost')).toBeCloseTo(12 * DAILY_RATE, 2);

	expect(errors).toEqual([]);
});

test('summary panel shows a bucket estimation total matching the bucket rows', async ({
	page,
	request
}) => {
	const errors = collectPageErrors(page);
	const { estimationId, b1, b2 } = await seedBucket(request);

	await page.goto(`/estimations/${estimationId}/versions/draft`);
	await page.waitForLoadState('networkidle');
	await expect(page.getByTestId('bucket-view-toggle-bucket')).toBeVisible();

	const summary = page.getByTestId('version-summary');
	await expect(summary).toBeVisible();

	// The bucket editor aggregates only per bucket; this is the estimate-wide
	// number it never had. Sample 2 + inherited 2 + sample 4 = 8.
	const panelOfferPT = await readNumber(page, 'version-summary.offerPT');
	expect(panelOfferPT).toBeCloseTo(8, 2);

	// …and it equals the sum of what the bucket rows themselves display. Bucket
	// rows stay visible while collapsed (task-134), so no expansion is needed.
	// Column 7 of the bucket view's columns is offerPT.
	const OFFER_PT_COLUMN = 7;
	const bucketSum =
		parseDe(await cellText(page, `bucket:${b1}`, OFFER_PT_COLUMN)) +
		parseDe(await cellText(page, `bucket:${b2}`, OFFER_PT_COLUMN)) +
		parseDe(await cellText(page, 'bucket:unassigned', OFFER_PT_COLUMN));
	expect(bucketSum).toBeCloseTo(panelOfferPT, 1);

	expect(errors).toEqual([]);
});
