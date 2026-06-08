import { test, expect, type Page } from '@playwright/test';

const API = 'http://localhost:8080';

// ── helpers ────────────────────────────────────────────────────────────────

async function createProject(page: Page): Promise<string> {
	const res = await page.request.post(`${API}/api/projects`, {
		data: { name: 'Smoke Project', description: 'e2e smoke', client: 'Tester' }
	});
	expect(res.status(), `POST /api/projects failed: ${await res.text()}`).toBe(201);
	return (await res.json()).id;
}

async function createEstimation(page: Page, projectId: string): Promise<string> {
	const res = await page.request.post(`${API}/api/projects/${projectId}/estimations`, {
		data: { offer: 'SMOKE-001', description: 'smoke estimation' }
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
			notes: 'smoke test notes',
			parameters: [
				{ name: 'Tagessatz', value: 900 },
				{ name: 'Standardabweichungsfaktor', value: 2 },
				{ name: 'Vertriebszuschlag', value: 0.1 }
			],
			effortDrivers: [{ description: 'QA', factor: 0.15, comment: null }],
			roots: [{
				type: 'GROUP',
				title: 'Development',
				children: [
					{ type: 'FIXED', description: 'Feature A', minEffort: 2, expectedEffort: 4, maxEffort: 6 },
					{ type: 'FIXED', description: 'Feature B', minEffort: 1, expectedEffort: 3, maxEffort: 5 }
				]
			}]
		}
	});
	expect(res.status(), `PUT draft failed: ${await res.text()}`).toBe(200);
}

async function populateDraftWithTree(page: Page, estimationId: string) {
	// Backend > Auth > {Token, Session} — three levels deep.
	const res = await page.request.put(`${API}/api/estimations/${estimationId}/versions/draft`, {
		data: {
			parameters: [
				{ name: 'Tagessatz', value: 900 },
				{ name: 'Standardabweichungsfaktor', value: 0 }
			],
			roots: [{
				type: 'GROUP',
				title: 'Backend',
				children: [
					{
						type: 'GROUP',
						title: 'Auth',
						children: [
							{ type: 'FIXED', description: 'Token endpoint', minEffort: 1, expectedEffort: 2, maxEffort: 3 },
							{ type: 'FIXED', description: 'Session storage', minEffort: 2, expectedEffort: 4, maxEffort: 6 }
						]
					}
				]
			}]
		}
	});
	expect(res.status(), `PUT tree-draft failed: ${await res.text()}`).toBe(200);
}

async function submitDraft(page: Page, estimationId: string): Promise<number> {
	const res = await page.request.post(`${API}/api/estimations/${estimationId}/versions/draft/submit`);
	expect(res.status(), `POST submit failed: ${await res.text()}`).toBe(200);
	return (await res.json()).versionNumber;
}

/** Collect console errors from the page during navigation */
function collectErrors(page: Page): string[] {
	const errors: string[] = [];
	page.on('console', (msg) => {
		if (msg.type() === 'error') errors.push(msg.text());
	});
	page.on('pageerror', (err) => errors.push(err.message));
	return errors;
}

/** Asserts the page shows no error message in the UI */
async function expectNoUiError(page: Page, context: string) {
	// Only match actual error paragraphs (class="text-red-600"), not delete buttons or hover states
	const errorEl = page.locator('p.text-red-600').first();
	const visible = await errorEl.isVisible().catch(() => false);
	if (visible) {
		const text = await errorEl.textContent();
		throw new Error(`[${context}] UI shows error: "${text}"`);
	}
}

// ── tests ──────────────────────────────────────────────────────────────────

test('home page loads without errors', async ({ page }) => {
	const errors = collectErrors(page);
	await page.goto('/');
	await page.waitForLoadState('networkidle');
	expect(errors, `console errors on home page`).toHaveLength(0);
	await expectNoUiError(page, 'home');
});

test('projects list loads without errors', async ({ page }) => {
	const errors = collectErrors(page);
	await page.goto('/projects');
	await page.waitForLoadState('networkidle');
	expect(errors, `console errors on projects list`).toHaveLength(0);
	await expectNoUiError(page, 'projects list');
});

test('project detail loads without errors', async ({ page }) => {
	const projectId = await createProject(page);
	const errors = collectErrors(page);
	await page.goto(`/projects/${projectId}`);
	await page.waitForLoadState('networkidle');
	expect(errors, `console errors on project detail`).toHaveLength(0);
	await expectNoUiError(page, `project/${projectId}`);
});

test('estimation detail loads without errors', async ({ page }) => {
	const projectId = await createProject(page);
	const estimationId = await createEstimation(page, projectId);
	const errors = collectErrors(page);
	await page.goto(`/estimations/${estimationId}`);
	await page.waitForLoadState('networkidle');
	expect(errors, `console errors on estimation detail`).toHaveLength(0);
	await expectNoUiError(page, `estimation/${estimationId}`);
});

test('draft version page loads without errors', async ({ page }) => {
	const projectId = await createProject(page);
	const estimationId = await createEstimation(page, projectId);
	const versionNumber = await createDraft(page, estimationId);
	await populateDraft(page, estimationId);

	const errors = collectErrors(page);
	await page.goto(`/estimations/${estimationId}/versions/${versionNumber}?draft=true`);
	await page.waitForLoadState('networkidle');
	expect(errors, `console errors on draft version page`).toHaveLength(0);
	await expectNoUiError(page, `draft version ${versionNumber}`);
});

test('submitted version page loads without errors', async ({ page }) => {
	const projectId = await createProject(page);
	const estimationId = await createEstimation(page, projectId);
	await createDraft(page, estimationId);
	await populateDraft(page, estimationId);
	const versionNumber = await submitDraft(page, estimationId);

	const errors = collectErrors(page);
	await page.goto(`/estimations/${estimationId}/versions/${versionNumber}`);
	await page.waitForLoadState('networkidle');
	expect(errors, `console errors on submitted version page`).toHaveLength(0);
	await expectNoUiError(page, `submitted version ${versionNumber}`);
});

test('draft version: editing items triggers save without 500', async ({ page }) => {
	const projectId = await createProject(page);
	const estimationId = await createEstimation(page, projectId);
	const versionNumber = await createDraft(page, estimationId);
	await populateDraft(page, estimationId);

	const apiErrors: { url: string; status: number }[] = [];
	page.on('response', (res) => {
		if (res.url().includes('/api/') && res.status() >= 500) {
			apiErrors.push({ url: res.url(), status: res.status() });
		}
	});

	await page.goto(`/estimations/${estimationId}/versions/${versionNumber}?draft=true`);
	await page.waitForLoadState('networkidle');
	await expectNoUiError(page, 'draft version initial load');

	// Edit notes to trigger auto-save
	const notesArea = page.locator('textarea').first();
	await notesArea.fill('Updated via smoke test');
	await page.waitForTimeout(1200); // wait for debounce + save
	await page.waitForLoadState('networkidle');

	expect(apiErrors, `500 errors during editing: ${JSON.stringify(apiErrors)}`).toHaveLength(0);
	await expectNoUiError(page, 'draft version after edit');
});

test('draft autosave payload uses canonical roots (not legacy itemGroups)', async ({ page }) => {
	const projectId = await createProject(page);
	const estimationId = await createEstimation(page, projectId);
	const versionNumber = await createDraft(page, estimationId);
	await populateDraft(page, estimationId);

	const putBodies: any[] = [];
	page.on('request', (req) => {
		if (req.method() === 'PUT' && req.url().includes('/api/estimations/') && req.url().endsWith('/draft')) {
			const body = req.postData();
			if (body) putBodies.push(JSON.parse(body));
		}
	});

	await page.goto(`/estimations/${estimationId}/versions/${versionNumber}?draft=true`);
	await page.waitForLoadState('networkidle');

	// Trigger an autosave by editing notes.
	await page.locator('textarea').first().fill('autosave-payload-check');
	await page.waitForTimeout(1200);
	await page.waitForLoadState('networkidle');

	expect(putBodies.length, 'expected at least one autosave PUT').toBeGreaterThan(0);
	for (const body of putBodies) {
		expect(body, 'autosave body must carry the canonical roots field').toHaveProperty('roots');
		expect(body, 'autosave body must NOT carry the legacy itemGroups field').not.toHaveProperty('itemGroups');
	}
});

test('three-level tree round-trips through the draft REST API', async ({ page }) => {
	const projectId = await createProject(page);
	const estimationId = await createEstimation(page, projectId);
	const versionNumber = await createDraft(page, estimationId);
	await populateDraftWithTree(page, estimationId);

	const errors = collectErrors(page);
	await page.goto(`/estimations/${estimationId}/versions/${versionNumber}?draft=true`);
	await page.waitForLoadState('networkidle');
	expect(errors, `console errors on three-level draft`).toHaveLength(0);
	await expectNoUiError(page, `three-level draft ${versionNumber}`);

	// Re-fetch the draft and verify the depth-3 shape survives.
	const fetched = await page.request.get(`${API}/api/estimations/${estimationId}/versions/draft`).then((r) => r.json());
	expect(fetched.roots).toHaveLength(1);
	expect(fetched.roots[0].type).toBe('GROUP');
	expect(fetched.roots[0].title).toBe('Backend');
	expect(fetched.roots[0].children).toHaveLength(1);
	expect(fetched.roots[0].children[0].type).toBe('GROUP');
	expect(fetched.roots[0].children[0].title).toBe('Auth');
	expect(fetched.roots[0].children[0].children).toHaveLength(2);
	expect(fetched.roots[0].children[0].children[0].description).toBe('Token endpoint');
	expect(fetched.roots[0].children[0].children[1].description).toBe('Session storage');
});
