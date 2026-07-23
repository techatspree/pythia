import { test, expect, request as playwrightRequest } from '@playwright/test';

// The dev module is strict (no default-user fallback), so the dev backend on
// :8080 rejects unauthenticated/invalid requests itself — no separate strict
// backend needed.
const API = 'http://localhost:8080';

test.describe('unauthenticated SPA gate', () => {
	// Override the globally pre-seeded dev-admin localStorage so this
	// test starts with a logged-out browser and the dev login dialog
	// must appear.
	test.use({ storageState: { cookies: [], origins: [] } });

	test('unauthenticated SPA visit shows the dev login dialog, not the application UI', async ({
		page
	}) => {
		await page.goto('/projects');
		await page.waitForLoadState('networkidle');

		await expect(page.locator('[role="dialog"][aria-label="Dev-Anmeldungsauswahl"]')).toBeVisible();
		await expect(page.locator('[data-testid="dev-login-dev-admin"]')).toBeVisible();
		await expect(page.locator('[data-testid^="row-"]')).toHaveCount(0);
	});
});

test('dev backend rejects a request with NO Authorization header (401)', async () => {
	const ctx = await playwrightRequest.newContext({ baseURL: API });
	const res = await ctx.get('/api/projects');
	expect(res.status()).toBe(401);
	await ctx.dispose();
});

test('dev backend rejects `Authorization: Dev not-a-real-user` (401)', async () => {
	const ctx = await playwrightRequest.newContext({
		baseURL: API,
		extraHTTPHeaders: { Authorization: 'Dev not-a-real-user' }
	});
	const res = await ctx.get('/api/projects');
	expect(res.status()).toBe(401);
	await ctx.dispose();
});

test('dev backend rejects a `Bearer` token (wrong scheme for the dev module, 401)', async () => {
	const ctx = await playwrightRequest.newContext({
		baseURL: API,
		extraHTTPHeaders: { Authorization: 'Bearer eyJhbGciOiJIUzI1NiJ9.dummy.signature' }
	});
	const res = await ctx.get('/api/projects');
	expect(res.status()).toBe(401);
	await ctx.dispose();
});
