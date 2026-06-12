import { test, expect } from '@playwright/test';

test('TreeTable shows all columns at full viewport', async ({ page }) => {
	await page.setViewportSize({ width: 1280, height: 720 });
	await page.goto('/dev/tree-table-demo');
	await page.waitForLoadState('networkidle');

	const headerStückpreis = page.getByText('Stückpreis', { exact: true });
	await expect(headerStückpreis).toBeVisible();
});

test('TreeTable hides collapsible columns when narrow AND inner wrapper scrolls horizontally', async ({
	page
}) => {
	await page.setViewportSize({ width: 600, height: 720 });
	await page.goto('/dev/tree-table-demo');
	await page.waitForLoadState('networkidle');

	// The "Stückpreis" header text is hidden because unitPrice is
	// collapsible and the container is narrower than the 900 px default.
	await expect(page.getByText('Stückpreis', { exact: true })).toHaveCount(0);

	// The inner .overflow-x-auto wrapper has natural width > viewport.
	const scrollHost = page.locator('.overflow-x-auto').first();
	const dims = await scrollHost.evaluate((el) => ({
		scrollWidth: el.scrollWidth,
		clientWidth: el.clientWidth
	}));
	expect(dims.scrollWidth).toBeGreaterThan(dims.clientWidth);

	// And it actually scrolls when asked to (catches the silent failure
	// mode where `min-width: max-content` is missing).
	const reached = await scrollHost.evaluate((el) => {
		el.scrollLeft = 9999;
		return el.scrollLeft;
	});
	expect(reached).toBeGreaterThan(0);
});
