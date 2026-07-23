import { test, expect } from '@playwright/test';
import { loginAsDev } from './helpers';

// A VIEWER can open the (intentionally ungated) create-project dialog and
// submit; the POST is denied with 403, and the app must surface the MEANINGFUL
// authorization message in the ErrorBanner — not a generic "failed" string
// (task-093).
test.describe('authorization failure surfaces a meaningful message', () => {
	// Override the globally pre-seeded dev-admin storageState for this block.
	test.use({ storageState: { cookies: [], origins: [] } });

	test('a viewer create-project 403 shows the authorization message', async ({ page }) => {
		await loginAsDev(page, 'dev-viewer');

		await page.goto('/projects');
		await page.waitForLoadState('networkidle');

		await page.getByRole('button', { name: 'Neues Projekt' }).click();
		await expect(page.getByRole('dialog')).toBeVisible();

		await page.getByLabel('Name *').fill('E2E Viewer Denied');
		await page.getByRole('button', { name: 'Anlegen' }).click();

		// The ErrorBanner (role="alert") shows the meaningful "not authorized"
		// message, proving the 403 was mapped to a human string rather than a
		// generic "Failed to create project".
		const banner = page.getByRole('alert');
		await expect(banner).toBeVisible();
		await expect(banner).toContainText(/not authorized/i);
	});
});
