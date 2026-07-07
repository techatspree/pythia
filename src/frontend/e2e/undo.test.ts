import { test, expect, type Page } from '@playwright/test';

const API = 'http://localhost:8080';

// The API endpoints are role-protected (task-091); authenticate the raw
// request context as dev-admin. UI navigations already run as dev-admin via
// the global storageState.
test.use({ extraHTTPHeaders: { Authorization: 'Dev dev-admin' } });

async function createProject(page: Page): Promise<string> {
	const res = await page.request.post(`${API}/api/projects`, {
		data: { name: 'Undo Project', description: 'e2e undo', client: 'Tester' }
	});
	expect(res.status(), `POST /api/projects failed: ${await res.text()}`).toBe(201);
	return (await res.json()).id;
}

async function createEstimation(page: Page, projectId: string): Promise<string> {
	const res = await page.request.post(`${API}/api/projects/${projectId}/estimations`, {
		data: { offer: 'UNDO-001', description: 'undo estimation' }
	});
	expect(res.status(), `POST estimations failed: ${await res.text()}`).toBe(201);
	return (await res.json()).id;
}

async function createDraft(page: Page, estimationId: string): Promise<number> {
	const res = await page.request.post(`${API}/api/estimations/${estimationId}/versions`);
	expect(res.status(), `POST versions failed: ${await res.text()}`).toBe(201);
	return (await res.json()).versionNumber;
}

async function populateDraft(page: Page, estimationId: string) {
	const res = await page.request.put(`${API}/api/estimations/${estimationId}/versions/draft`, {
		data: {
			parameters: [
				{ name: 'Tagessatz', value: 900 },
				{ name: 'Standardabweichungsfaktor', value: 2 }
			],
			roots: [
				{
					type: 'GROUP',
					title: 'Development',
					children: [
						{ type: 'FIXED', description: 'Feature A', minEffort: 2, expectedEffort: 4, maxEffort: 6 }
					]
				}
			]
		}
	});
	expect(res.status(), `PUT draft failed: ${await res.text()}`).toBe(200);
}

test('undo/redo revert and re-apply a grid edit via keyboard', async ({ page }) => {
	const projectId = await createProject(page);
	const estimationId = await createEstimation(page, projectId);
	const versionNumber = await createDraft(page, estimationId);
	await populateDraft(page, estimationId);

	await page.goto(`/estimations/${estimationId}/versions/${versionNumber}?draft=true`);
	await page.waitForLoadState('networkidle');

	// Optimistic cell of the first leaf (group 0 › child 0, column 1).
	const cellSelector = '[data-cell="0-0-1"]';
	await expect(page.locator(cellSelector)).toHaveValue('2');

	// Edit it and let the autosave PUT (+ history refresh) record the mutation.
	await page.locator(cellSelector).fill('9');
	await page.waitForTimeout(1500); // debounce (800ms) + PUT + history refresh
	await page.waitForLoadState('networkidle');
	await expect(page.locator(cellSelector)).toHaveValue('9');

	// Ctrl+Z reverts to the pre-edit value.
	const undoResp = page.waitForResponse(
		(r) => r.url().includes('/versions/draft/undo') && r.request().method() === 'POST'
	);
	await page.keyboard.press('Control+z');
	await undoResp;
	await expect(page.locator(cellSelector)).toHaveValue('2');

	// Ctrl+Shift+Z re-applies the edit.
	const redoResp = page.waitForResponse(
		(r) => r.url().includes('/versions/draft/redo') && r.request().method() === 'POST'
	);
	await page.keyboard.press('Control+Shift+z');
	await redoResp;
	await expect(page.locator(cellSelector)).toHaveValue('9');
});
