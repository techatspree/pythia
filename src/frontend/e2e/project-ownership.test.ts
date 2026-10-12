import { test, expect, request as playwrightRequest } from '@playwright/test';
import { catalogText, loginAsDev } from './helpers';
import { ChangeOwnerDialog, ProjectDetailPage, ProjectsListPage } from './pages';

// Project ownership (task-200): "Only my projects" is the list's default, the
// filter survives a reload through `?scope=all`, and the owner (or an admin)
// can hand a project to someone else.

const API = 'http://localhost:8090';

// A colleague the dev seeder provisions (src/backend/.../testdata/SeededUsers.kt).
// Test DATA, not a UI label.
const TRANSFER_TARGET = 'Anna Becker';

// Each test signs in as the user it is about, not the global dev-admin.
test.use({ storageState: { cookies: [], origins: [] } });

function suffix(): string {
	return Math.random().toString(36).slice(2, 8);
}

/** Creates a project through the API as `subjectId`, who thereby owns it. */
async function createProjectAs(subjectId: string, name: string): Promise<string> {
	const ctx = await playwrightRequest.newContext({
		baseURL: API,
		extraHTTPHeaders: { Authorization: `Dev ${subjectId}` }
	});
	try {
		const res = await ctx.post('/api/projects', {
			data: { name, client: 'Nordwind Logistik GmbH' }
		});
		expect(res.status()).toBe(201);
		return (await res.json()).id as string;
	} finally {
		await ctx.dispose();
	}
}

function ownerLine(displayName: string): string {
	return catalogText('project.detailOwner').replace('{owner}', displayName);
}

test('the list shows only my projects by default, and all of them on request', async ({
	page
}) => {
	const id = suffix();
	const mine = await createProjectAs('dev-estimator', `Kundenportal ${id}`);
	const theirs = await createProjectAs('dev-admin', `Lagerverwaltung ${id}`);
	await loginAsDev(page, 'dev-estimator');
	const list = new ProjectsListPage(page);

	await list.goto();
	await list.expectOnlyMine(true);
	let ids = await list.rowIds();
	expect(ids).toContain(mine);
	expect(ids).not.toContain(theirs);

	await list.setOnlyMine(false);
	ids = await list.rowIds();
	expect(ids).toContain(mine);
	expect(ids).toContain(theirs);
	await expect(list.ownerCell(theirs)).toHaveText('Dev Admin');

	// The choice lives in the URL, so a reload keeps the full list.
	expect(new URL(page.url()).searchParams.get('scope')).toBe('all');
	await page.reload();
	await page.waitForLoadState('networkidle');
	await list.expectOnlyMine(false);
	expect(await list.rowIds()).toContain(theirs);
});

test('a viewer who owns nothing sees the empty state and can show all', async ({ page }) => {
	await createProjectAs('dev-estimator', `Reisekostenportal ${suffix()}`);
	await loginAsDev(page, 'dev-viewer');
	const list = new ProjectsListPage(page);

	await list.goto();
	await list.expectEmptyMine();

	await list.showAllFromEmptyState();
	await list.expectOnlyMine(false);
	expect((await list.rowIds()).length).toBeGreaterThan(0);
});

test('the owner transfers a project to a colleague and loses the change button', async ({
	page
}) => {
	const projectId = await createProjectAs('dev-estimator', `Vertragsarchiv ${suffix()}`);
	await loginAsDev(page, 'dev-estimator');
	const detail = new ProjectDetailPage(page);
	const dialog = new ChangeOwnerDialog(page);

	await detail.goto(projectId);
	await expect(detail.ownerText()).toHaveText(ownerLine('Dev Estimator'));
	await detail.expectChangeOwnerVisible(true);

	await detail.openChangeOwner();
	await dialog.expectVisible();
	await dialog.selectOwnerByDisplayName(TRANSFER_TARGET);
	await dialog.save();
	await dialog.expectHidden();

	await expect(detail.ownerText()).toHaveText(ownerLine(TRANSFER_TARGET));
	await detail.expectChangeOwnerVisible(false);
});

test('a viewer sees the owner but no change button', async ({ page }) => {
	const projectId = await createProjectAs('dev-estimator', `Schulungsplattform ${suffix()}`);
	await loginAsDev(page, 'dev-viewer');
	const detail = new ProjectDetailPage(page);

	await detail.goto(projectId);
	await expect(detail.ownerText()).toHaveText(ownerLine('Dev Estimator'));
	await detail.expectChangeOwnerVisible(false);
});
