import { test, expect, request as playwrightRequest } from '@playwright/test';

const STRICT = process.env.STRICT_BACKEND_URL;
const SKIP_MSG =
	'set STRICT_BACKEND_URL to run the strict-backend auth tests (e.g. http://localhost:8081)';

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

		await expect(page.locator('[role="dialog"][aria-label="Dev login picker"]')).toBeVisible();
		await expect(page.locator('[data-testid="dev-login-dev-admin"]')).toBeVisible();
		await expect(page.locator('[data-testid^="row-"]')).toHaveCount(0);
	});
});

test('strict backend rejects a request with NO Authorization header (401)', async () => {
	test.skip(!STRICT, SKIP_MSG);
	const ctx = await playwrightRequest.newContext({ baseURL: STRICT });
	const res = await ctx.get('/api/projects');
	expect(res.status()).toBe(401);
	await ctx.dispose();
});

test('strict backend rejects `Authorization: Dev not-a-real-user` (401)', async () => {
	test.skip(!STRICT, SKIP_MSG);
	const ctx = await playwrightRequest.newContext({
		baseURL: STRICT,
		extraHTTPHeaders: { Authorization: 'Dev not-a-real-user' }
	});
	const res = await ctx.get('/api/projects');
	expect(res.status()).toBe(401);
	await ctx.dispose();
});

test('strict backend rejects a `Bearer` token (wrong scheme for the dev module, 401)', async () => {
	test.skip(!STRICT, SKIP_MSG);
	const ctx = await playwrightRequest.newContext({
		baseURL: STRICT,
		extraHTTPHeaders: { Authorization: 'Bearer eyJhbGciOiJIUzI1NiJ9.dummy.signature' }
	});
	const res = await ctx.get('/api/projects');
	expect(res.status()).toBe(401);
	await ctx.dispose();
});
