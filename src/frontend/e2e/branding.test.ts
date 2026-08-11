import { test, expect } from '@playwright/test';

// The product mark replaced an earlier third-party corporate logo (task-130).
// These assertions pin the swap: the mark is the canonical SVG asset and the
// wordmark renders beside it.
test('the header shows Pythia mark and wordmark', async ({ page }) => {
	await page.goto('/projects');

	const mark = page.locator('[data-testid="brand-logo"]');
	await expect(mark).toBeVisible();

	// Vite serves the asset as a URL under `vite dev` but inlines it as a
	// base64 data URI in a production build (assetsInlineLimit is 4096 bytes
	// and the mark is smaller), so both forms are valid.
	const src = await mark.getAttribute('src');
	expect(src).toMatch(/(^data:image\/svg\+xml)|(\.svg)/);

	await expect(page.locator('header')).toContainText('Pythia');
	await expect(page).toHaveTitle('Pythia');
});
