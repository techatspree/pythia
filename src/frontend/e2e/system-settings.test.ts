import { test, expect, type APIRequestContext } from '@playwright/test';

const API = 'http://localhost:8090';
const LOCAL = 'http://localhost:5173';
const H = { Authorization: 'Dev dev-admin' };

// Per-installation system settings (task-146): the display name reaches the
// header, and an uploaded stylesheet actually restyles the app.
//
// These settings are GLOBAL (one singleton row + one driver template for the
// whole backend), so this spec resets them in afterEach — a leftover name or
// stylesheet would otherwise leak into every other spec sharing the backend,
// and a leftover driver template would seed their drafts.
//
// For the same reason these tests MUST stay serial: they rely on Playwright's
// default of running tests within a file one at a time. Do NOT add
// `test.describe.configure({ mode: 'parallel' })` here and do not run this file
// with `--repeat-each` — concurrent copies fight over the same singleton row and
// fail on each other's state, not on a product defect.

function seed(subject: string) {
	return {
		cookies: [],
		origins: [{ origin: LOCAL, localStorage: [{ name: 'devAuthSubject', value: subject }] }]
	};
}

async function reset(req: APIRequestContext) {
	await req.put(`${API}/api/system`, { headers: H, data: { displayName: null } });
	await req.put(`${API}/api/system/effort-drivers`, { headers: H, data: [] });
	await req.delete(`${API}/api/system/css`, { headers: H });
}

test.describe('system settings', () => {
	test.afterEach(async ({ request }) => {
		await reset(request);
	});

	test('the configured display name replaces the built-in brand name in the header', async ({
		browser
	}) => {
		const ctx = await browser.newContext({
			baseURL: LOCAL,
			locale: 'de-DE',
			storageState: seed('dev-admin')
		});
		const page = await ctx.newPage();
		try {
			await reset(page.request);

			await page.goto('/projects');
			await expect(page.getByTestId('brand-name')).toHaveText('The Estimator');

			await page.goto('/admin/system');
			// The form only renders once the settings have loaded, so its presence
			// is the signal that a fill() will not be overwritten by the load.
			await expect(page.getByTestId('system-display-name')).toBeVisible();
			await page.getByTestId('system-display-name').fill('Contoso GmbH');
			await page.getByTestId('system-save-name').click();
			await expect(page.getByTestId('system-notice')).toBeVisible();

			// Survives a reload — it is persisted, not just local state.
			await page.reload();
			await expect(page.getByTestId('brand-name')).toHaveText('Contoso GmbH');

			// Clearing it falls back to the built-in i18n name.
			await expect(page.getByTestId('system-display-name')).toBeVisible();
			await expect(page.getByTestId('system-display-name')).toHaveValue('Contoso GmbH');
			await page.getByTestId('system-display-name').fill('');
			await page.getByTestId('system-save-name').click();
			await expect(page.getByTestId('system-notice')).toBeVisible();
			await page.reload();
			await expect(page.getByTestId('brand-name')).toHaveText('The Estimator');
		} finally {
			await ctx.close();
		}
	});

	test('an uploaded stylesheet overrides the standard styling and can be removed', async ({
		browser
	}) => {
		const ctx = await browser.newContext({
			baseURL: LOCAL,
			locale: 'de-DE',
			storageState: seed('dev-admin')
		});
		const page = await ctx.newPage();
		try {
			await reset(page.request);

			await page.goto('/admin/system');
			await expect(page.getByTestId('system-css-none')).toBeVisible();

			const brandGreen = () =>
				page.evaluate(() =>
					getComputedStyle(document.documentElement).getPropertyValue('--color-brand-green').trim()
				);
			const before = await brandGreen();
			expect(before).not.toBe('');

			await page.getByTestId('system-css-input').setInputFiles({
				name: 'brand.css',
				mimeType: 'text/css',
				buffer: Buffer.from(':root { --color-brand-green: rgb(1, 2, 3); }')
			});

			// The page reloads itself after a successful upload; the <link> then
			// renders (it is conditional on hasCustomCss) and the stylesheet
			// applies, so poll rather than reading the value once.
			await expect(page.getByTestId('system-css-current')).toBeVisible();
			await expect.poll(brandGreen).toBe('rgb(1, 2, 3)');

			await page.getByTestId('system-css-remove').click();
			await expect(page.getByTestId('system-css-none')).toBeVisible();
			await expect.poll(brandGreen).toBe(before);
		} finally {
			await ctx.close();
		}
	});

	test('standard effort drivers seed a first draft', async ({ browser }) => {
		const ctx = await browser.newContext({
			baseURL: LOCAL,
			locale: 'de-DE',
			storageState: seed('dev-admin')
		});
		const page = await ctx.newPage();
		try {
			await reset(page.request);

			await page.goto('/admin/system');
			await page.getByTestId('system-add-driver').click();
			const row = page.getByTestId('system-driver-row').first();
			await row.locator('input').nth(0).fill('Legacy integration');
			await row.locator('input').nth(1).fill('0.15');
			await page.getByTestId('system-save-drivers').click();
			await expect(page.getByTestId('system-notice')).toBeVisible();

			// A brand-new estimation has no submitted version, so its first draft
			// is seeded from the template.
			const proj = await page.request.post(`${API}/api/projects`, {
				headers: H,
				data: { name: 'System Settings E2E', description: 'e2e', client: 'Tester' }
			});
			const projectId = (await proj.json()).id;
			const est = await page.request.post(`${API}/api/projects/${projectId}/estimations`, {
				headers: H,
				data: { offer: 'SYSSET-E2E', description: 'seeded drivers' }
			});
			const estimationId = (await est.json()).id;
			const created = await page.request.post(`${API}/api/estimations/${estimationId}/versions`, {
				headers: H
			});
			expect(created.status()).toBe(201);

			const draft = await page.request.get(
				`${API}/api/estimations/${estimationId}/versions/draft`,
				{ headers: H }
			);
			const drivers = (await draft.json()).effortDrivers as Array<{
				description: string;
				factor: number;
			}>;
			expect(drivers).toHaveLength(1);
			expect(drivers[0].description).toBe('Legacy integration');
			expect(drivers[0].factor).toBeCloseTo(0.15, 5);
		} finally {
			await ctx.close();
		}
	});
});
