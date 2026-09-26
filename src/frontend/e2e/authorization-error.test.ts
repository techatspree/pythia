import { test } from '@playwright/test';
import { loginAsDev } from './helpers';
import { CreateProjectDialog, ErrorBanner, ProjectsListPage } from './pages';

// A VIEWER can open the (intentionally ungated) create-project dialog and
// submit; the POST is denied with 403, and the app must surface the MEANINGFUL
// authorization message in the ErrorBanner — not a generic "failed" string
// (task-093).
test.describe('authorization failure surfaces a meaningful message', () => {
	// Override the globally pre-seeded dev-admin storageState for this block.
	test.use({ storageState: { cookies: [], origins: [] } });

	test('a viewer create-project 403 shows the authorization message', async ({ page }) => {
		await loginAsDev(page, 'dev-viewer');

		const projects = new ProjectsListPage(page);
		const dialog = new CreateProjectDialog(page);
		const banner = new ErrorBanner(page);

		await projects.goto();

		await projects.openCreateProjectDialog();
		await dialog.expectVisible();

		await dialog.fillName('E2E Viewer Denied');
		await dialog.submit();

		// The ErrorBanner shows the meaningful "not authorized" message, proving
		// the 403 was mapped to a human string rather than a generic
		// "Failed to create project".
		await banner.expectVisible();
		await banner.expectMessage(/not authorized/i);
	});
});
