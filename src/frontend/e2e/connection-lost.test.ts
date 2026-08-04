import { test, expect } from '@playwright/test';

const LOCAL = 'http://localhost:5173';

// Connection watchdog (task-136): a genuine backend connection loss blocks the
// whole app with a modal that only clears after a heartbeat + acknowledge.
function seed(subject: string) {
	return {
		cookies: [],
		origins: [{ origin: LOCAL, localStorage: [{ name: 'devAuthSubject', value: subject }] }]
	};
}

test('connection loss blocks the app until the backend returns and the user acknowledges', async ({
	browser
}) => {
	const ctx = await browser.newContext({
		baseURL: LOCAL,
		locale: 'de-DE',
		storageState: seed('dev-admin')
	});
	const page = await ctx.newPage();
	// Match ONLY root-level backend calls (`//host/api/…`), not Vite source
	// modules like `/src/lib/api/fetch.ts` — a `**/api/**` glob would abort those
	// too and break the app's own JS loading.
	const backendApi = /https?:\/\/[^/]+\/api\//;
	try {
		await page.goto('/projects');
		await expect(page.getByTestId('brand-logo')).toBeVisible();

		// Simulate backend loss: abort every backend /api call (the heartbeat too).
		await page.route(backendApi, (r) => r.abort());

		// A reload re-runs the layout's loadAccount → GET /api/auth/me (via apiFetch)
		// → aborted → the watchdog blocks the app.
		await page.reload();
		const dialog = page.getByRole('alertdialog');
		await expect(dialog).toBeVisible();
		const acknowledge = dialog.getByRole('button');
		await expect(acknowledge).toBeDisabled();

		// Backend returns: stop aborting; the ~3s heartbeat re-enables acknowledge.
		await page.unroute(backendApi);
		await expect(acknowledge).toBeEnabled({ timeout: 8000 });

		// Acknowledge → full reload → the block is gone.
		await acknowledge.click();
		await expect(page.getByRole('alertdialog')).toHaveCount(0);
	} finally {
		await ctx.close();
	}
});
