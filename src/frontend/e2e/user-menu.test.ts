import { test, expect } from '@playwright/test';

test('header shows the dev-admin user and the three role badges when authenticated', async ({
	page
}) => {
	await page.goto('/projects');
	await page.waitForLoadState('networkidle');

	const header = page.locator('header');
	await expect(header).toContainText('Dev Admin');
	await expect(header.getByText('VIEWER', { exact: true })).toBeVisible();
	await expect(header.getByText('ESTIMATOR', { exact: true })).toBeVisible();
	await expect(header.getByText('ADMIN', { exact: true })).toBeVisible();
	await expect(page.locator('[data-testid="logout-button"]')).toBeVisible();
});

test('clicking Logout clears the account and shows the dev login dialog', async ({ page }) => {
	await page.goto('/projects');
	await page.waitForLoadState('networkidle');

	await page.locator('[data-testid="logout-button"]').click();

	const stored = await page.evaluate(() => localStorage.getItem('devAuthSubject'));
	expect(stored).toBeNull();

	await expect(
		page.locator('[role="dialog"][aria-label="Dev login picker"]')
	).toBeVisible();
});
