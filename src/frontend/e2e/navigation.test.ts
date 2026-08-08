import { test, expect, type APIRequestContext, type Page } from '@playwright/test';

const API = 'http://localhost:8090';

// One consistent top navigation on EVERY route (task-141). The session route
// group used to replace the app header with its own bar, so a user in a room
// had no recognisable way back and the chrome looked nothing like the rest of
// the app. These assertions pin the single AppHeader, its active-item marking,
// and the room's contextual back link.

async function seedSession(req: APIRequestContext) {
	const H = { Authorization: 'Dev dev-admin' };

	const proj = await req.post(`${API}/api/projects`, {
		headers: H,
		data: { name: 'Nav Project', description: 'e2e navigation', client: 'Tester' }
	});
	expect(proj.status(), `POST projects: ${await proj.text()}`).toBe(201);
	const projectId = (await proj.json()).id;

	const est = await req.post(`${API}/api/projects/${projectId}/estimations`, {
		headers: H,
		data: { offer: 'NAV-001', description: 'navigation estimation' }
	});
	expect(est.status(), `POST estimations: ${await est.text()}`).toBe(201);
	const estimationId = (await est.json()).id;

	const draft = await req.post(`${API}/api/estimations/${estimationId}/versions`, { headers: H });
	expect(draft.status(), `POST versions: ${await draft.text()}`).toBe(201);

	const put = await req.put(`${API}/api/estimations/${estimationId}/versions/draft`, {
		headers: H,
		data: {
			roots: [
				{ type: 'FIXED', description: 'Feature A', minEffort: 1, expectedEffort: 2, maxEffort: 3 }
			]
		}
	});
	expect(put.status(), `PUT draft: ${await put.text()}`).toBe(200);

	const draftRes = await req.get(`${API}/api/estimations/${estimationId}/versions/draft`, {
		headers: H
	});
	const roots = (await draftRes.json()).roots as Array<{ logicalId: string }>;

	const session = await req.post(`${API}/api/sessions`, {
		headers: H,
		data: {
			estimationId,
			title: 'E2E Navigation Session',
			itemLogicalIds: roots.map((r) => r.logicalId)
		}
	});
	expect(session.status(), `POST sessions: ${await session.text()}`).toBe(201);

	return { projectId, estimationId, sessionId: (await session.json()).id as string };
}

/** The header renders only once the account has resolved, so every assertion
 *  waits for the nav rather than the bare page load. */
async function expectHeader(page: Page) {
	await expect(page.getByTestId('main-nav')).toBeVisible();
	await expect(page.getByTestId('brand-logo')).toBeVisible();
}

test('the same header and menu render on every route, including a session room', async ({
	page,
	request
}) => {
	const errors: string[] = [];
	page.on('pageerror', (e) => errors.push(String(e)));

	const { projectId, sessionId } = await seedSession(request);

	await page.goto('/projects');
	await expectHeader(page);

	await page.goto(`/projects/${projectId}`);
	await expectHeader(page);

	await page.goto('/sessions');
	await expectHeader(page);

	// The regression this task fixes: the room used to hide the app header.
	await page.goto(`/sessions/${sessionId}`);
	await expectHeader(page);
	await expect(page.getByTestId('nav-projects')).toBeVisible();
	await expect(page.getByTestId('nav-sessions')).toBeVisible();

	// The old room chrome is gone.
	await expect(page.getByText('The Estimator · Session')).toHaveCount(0);

	expect(errors, 'the room must not throw').toEqual([]);
});

test('the menu marks the active destination', async ({ page, request }) => {
	const { projectId, sessionId } = await seedSession(request);

	// A destination stays marked below its own subtree.
	await page.goto(`/projects/${projectId}`);
	await expectHeader(page);
	await expect(page.getByTestId('nav-projects')).toHaveAttribute('aria-current', 'page');
	await expect(page.getByTestId('nav-sessions')).not.toHaveAttribute('aria-current', 'page');

	await page.goto(`/sessions/${sessionId}`);
	await expectHeader(page);
	await expect(page.getByTestId('nav-sessions')).toHaveAttribute('aria-current', 'page');
	await expect(page.getByTestId('nav-projects')).not.toHaveAttribute('aria-current', 'page');
});

test('the menu navigates out of a session room', async ({ page, request }) => {
	const { sessionId } = await seedSession(request);

	await page.goto(`/sessions/${sessionId}`);
	await expectHeader(page);

	await page.getByTestId('nav-projects').click();
	await expect(page).toHaveURL(/\/projects$/);
});

test('the session room links back to its estimation', async ({ page, request }) => {
	const { estimationId, sessionId } = await seedSession(request);

	await page.goto(`/sessions/${sessionId}`);
	const back = page.getByRole('link', { name: 'Zur Schätzung' });
	await expect(back).toBeVisible();

	await back.click();
	await expect(page).toHaveURL(new RegExp(`/estimations/${estimationId}$`));
});
