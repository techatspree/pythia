import { test, expect } from '@playwright/test';
import { DevLoginDialog, ProjectsListPage, UserMenu } from './pages';

test('header shows the dev-admin user and the three role badges when authenticated', async ({
	page
}) => {
	const projects = new ProjectsListPage(page);
	const userMenu = new UserMenu(page);

	await projects.goto();

	// 'Dev Admin' is the fixture user's display name — data, not UI chrome.
	await userMenu.expectDisplayName('Dev Admin');
	await userMenu.expectRoleBadge('viewer');
	await userMenu.expectRoleBadge('estimator');
	await userMenu.expectRoleBadge('admin');
	await userMenu.expectLogoutVisible();
});

test('clicking Logout clears the account and shows the dev login dialog', async ({ page }) => {
	const projects = new ProjectsListPage(page);
	const userMenu = new UserMenu(page);
	const devLogin = new DevLoginDialog(page);

	await projects.goto();

	await userMenu.logout();

	const stored = await page.evaluate(() => localStorage.getItem('devAuthSubject'));
	expect(stored).toBeNull();

	await devLogin.expectVisible();
});
