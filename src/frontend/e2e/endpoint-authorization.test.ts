import { test, expect, request as playwrightRequest } from '@playwright/test';
import { loginAsDev } from './helpers';

const API = 'http://localhost:8080';

// ── API-level authorization contract ────────────────────────────────────────

test('POST /api/projects with NO Authorization header is 401', async () => {
	const ctx = await playwrightRequest.newContext({ baseURL: API });
	const res = await ctx.post('/api/projects', { data: { name: 'NoAuth' } });
	expect(res.status()).toBe(401);
	await ctx.dispose();
});

test('POST /api/projects as dev-viewer is 403 (VIEWER cannot write)', async () => {
	const ctx = await playwrightRequest.newContext({
		baseURL: API,
		extraHTTPHeaders: { Authorization: 'Dev dev-viewer' }
	});
	const res = await ctx.post('/api/projects', { data: { name: 'ViewerWrite' } });
	expect(res.status()).toBe(403);
	await ctx.dispose();
});

test('POST /api/projects as dev-estimator is 201', async () => {
	const ctx = await playwrightRequest.newContext({
		baseURL: API,
		extraHTTPHeaders: { Authorization: 'Dev dev-estimator' }
	});
	const res = await ctx.post('/api/projects', {
		data: { name: 'EstimatorWrite' }
	});
	expect(res.status()).toBe(201);
	await ctx.dispose();
});

// ── Frontend seam: the UI must actually send the auth header ─────────────────

test.describe('creating a project through the UI sends the auth header', () => {
	// Log in as an estimator (can write). addInitScript overrides the globally
	// pre-seeded dev-admin localStorage for this describe block.
	test.use({ storageState: { cookies: [], origins: [] } });

	test('a UI create carries Authorization and succeeds', async ({ page }) => {
		await loginAsDev(page, 'dev-estimator');

		const [postRequest] = await Promise.all([
			page.waitForRequest(
				(r) => r.url().includes('/api/projects') && r.method() === 'POST'
			),
			(async () => {
				await page.goto('/projects');
				await page.getByRole('button', { name: 'New Project' }).click();
				await page.getByLabel('Name *').fill('E2E Auth Project');
				await page.getByRole('button', { name: 'Create' }).click();
			})()
		]);

		// The exact seam no backend IT can reach: the browser's request carried
		// the Authorization header produced by apiFetch.
		expect(postRequest.headers()['authorization']).toBe('Dev dev-estimator');

		// And because writes require ESTIMATOR, a successful create proves the
		// header was accepted end-to-end.
		await expect(page.getByText('E2E Auth Project')).toBeVisible();
	});
});
