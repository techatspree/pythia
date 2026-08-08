import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { loginAsDev } from './helpers';

/**
 * TreeTable column headers must never paint outside their own column (task-140).
 *
 * The header row is a CSS grid with fixed tracks. A single long word cannot
 * wrap, so a header cell without `min-w-0`/`overflow-hidden` simply overflows
 * its grid item and paints over the neighbouring header — which is what made
 * the estimation editor's labels overlap and become unreadable.
 *
 * The guard is a MEASUREMENT, not a screenshot: every header cell must satisfy
 * `scrollWidth <= clientWidth`. It runs in BOTH locales because the defect is
 * translation-dependent — "PESSIMISTISCH" and "PESSIMISTIC" need different
 * widths, so a German-only check would let an English regression through.
 */

// Wide enough that no `collapsible` column is auto-hidden by
// `collapseBreakpointPx` (900), so every header is actually measured.
test.use({ viewport: { width: 1400, height: 1000 } });

const API = 'http://localhost:8090';
const ADMIN = { Authorization: 'Dev dev-admin' } as const;
const JSON_HEADERS = { 'Content-Type': 'application/json', ...ADMIN } as const;

async function seedProject(request: APIRequestContext, name: string): Promise<string> {
	const res = await request.post('/api/projects', { headers: JSON_HEADERS, data: { name } });
	expect(res.status()).toBe(201);
	return (await res.json()).id;
}

/** THREE_POINT_PERT estimation whose draft holds a group with two leaves. */
async function seedPertDraft(request: APIRequestContext): Promise<string> {
	const projectId = await seedProject(request, `E2E HeaderFit PERT ${Date.now()}`);
	const estRes = await request.post(`/api/projects/${projectId}/estimations`, {
		headers: JSON_HEADERS,
		data: { offer: `E2E-HF-P-${Date.now()}`, method: 'THREE_POINT_PERT' }
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
					type: 'GROUP',
					logicalId: crypto.randomUUID(),
					title: 'WBS 1',
					children: [
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
			]
		}
	});
	expect(putRes.status()).toBe(200);
	return estimationId;
}

/** BUCKET_SAMPLED_PERT estimation whose draft nests bucketed leaves in a group,
 *  so both the bucket view and the hierarchy view have rows to render. */
async function seedBucketDraft(request: APIRequestContext): Promise<string> {
	const projectId = await seedProject(request, `E2E HeaderFit Bucket ${Date.now()}`);
	const estRes = await request.post(`/api/projects/${projectId}/estimations`, {
		headers: JSON_HEADERS,
		data: { offer: `E2E-HF-B-${Date.now()}`, method: 'BUCKET_SAMPLED_PERT' }
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
					type: 'GROUP',
					logicalId: crypto.randomUUID(),
					title: 'WBS 1',
					children: [
						{
							type: 'BUCKETED',
							logicalId: crypto.randomUUID(),
							description: 'Nested sample',
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
			]
		}
	});
	expect(putRes.status()).toBe(200);
	return estimationId;
}

/** Every header cell must fit its own grid track. */
async function assertHeadersFit(page: Page, where: string) {
	await expect(page.locator('[data-testid="tt-header"]').first()).toBeVisible();
	const bad = await page.$$eval('[data-testid="tt-header"] > div', (cells) =>
		cells
			.filter((c) => c.scrollWidth > c.clientWidth + 1)
			.map((c) => `${c.textContent?.trim()}: ${c.scrollWidth}>${c.clientWidth}`)
	);
	expect(bad, `header labels overflowing their column (${where})`).toEqual([]);
}

async function checkAllEditors(page: Page, pertId: string, bucketId: string, locale: string) {
	await page.goto(`/estimations/${pertId}/versions/draft`);
	await page.waitForLoadState('networkidle');
	await assertHeadersFit(page, `${locale}: PERT grid`);

	await page.goto(`/estimations/${bucketId}/versions/draft`);
	await page.waitForLoadState('networkidle');
	await expect(page.getByTestId('bucket-view-toggle-bucket')).toBeVisible();
	await assertHeadersFit(page, `${locale}: bucket view`);

	await page.getByTestId('bucket-view-toggle-hierarchy').click();
	await assertHeadersFit(page, `${locale}: hierarchy view`);
}

test('every TreeTable header fits its column', async ({ page, request }) => {
	const errors: string[] = [];
	page.on('pageerror', (e) => errors.push(e.message));

	const pertId = await seedPertDraft(request);
	const bucketId = await seedBucketDraft(request);

	await checkAllEditors(page, pertId, bucketId, 'de');

	expect(errors, 'uncaught page errors').toEqual([]);
});

test.describe('English', () => {
	// A DEDICATED dev user, not the globally pre-seeded dev-admin: specs run in
	// parallel against ONE backend, and the language preference is persisted
	// per user — switching dev-admin to English would break the German
	// assertions in every other spec, and an "restore it afterwards" step does
	// not run when an assertion fails mid-test.
	test.use({ storageState: { cookies: [], origins: [] } });

	test('every TreeTable header fits its column in English', async ({ page, request }) => {
		const errors: string[] = [];
		page.on('pageerror', (e) => errors.push(e.message));

		const pertId = await seedPertDraft(request);
		const bucketId = await seedBucketDraft(request);

		await loginAsDev(page, 'dev-estimator');
		const res = await page.request.put(`${API}/api/auth/me/language`, {
			headers: { Authorization: 'Dev dev-estimator', 'Content-Type': 'application/json' },
			data: { language: 'en' }
		});
		expect(res.status(), 'PUT /api/auth/me/language en').toBe(204);

		await checkAllEditors(page, pertId, bucketId, 'en');

		expect(errors, 'uncaught page errors').toEqual([]);
	});

	test.afterEach(async ({ page }) => {
		// Leave the dedicated user German so the next run starts clean.
		await page.request.put(`${API}/api/auth/me/language`, {
			headers: { Authorization: 'Dev dev-estimator', 'Content-Type': 'application/json' },
			data: { language: 'de' }
		});
	});
});
