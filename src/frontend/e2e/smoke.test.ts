import { test, expect, type Page, type Locator } from '@playwright/test';

const API = 'http://localhost:8090';

// The API endpoints are role-protected (task-091). Raw `page.request` calls
// bypass the frontend's apiFetch, so authenticate the request context as
// dev-admin (all roles) — the UI navigations already run as dev-admin via the
// global storageState.
//
// The header is attached via `page.route(**/api/**, …)` (below) instead of
// `test.use({ extraHTTPHeaders })` because the latter also decorates the
// browser's cross-origin requests (including font fetches to
// fonts.gstatic.com), which then trigger a CORS preflight the CDN rejects —
// the failure surfaces as spurious "Access to font … blocked by CORS policy"
// console errors that trip the `collectErrors` assertions.
const API_HEADERS = { Authorization: 'Dev dev-admin' } as const;

test.beforeEach(async ({ page }) => {
	// Attach Authorization ONLY to backend `/api/**` requests the browser makes.
	// `page.request.*` calls from the helpers below still need the header — we
	// pass it there explicitly via the `headers` option.
	await page.route('**/api/**', async (route) => {
		const headers = { ...route.request().headers(), ...API_HEADERS };
		await route.continue({ headers });
	});
});

/** Minimal shape of an estimation node as returned by the REST API, used to
    type the JSON traversal in assertions below. */
type FetchedNode = {
	type?: string;
	title?: string;
	description?: string;
	children?: FetchedNode[];
};

// ── helpers ────────────────────────────────────────────────────────────────

async function createProject(page: Page): Promise<string> {
	const res = await page.request.post(`${API}/api/projects`, {
		headers: API_HEADERS,
		data: { name: 'Smoke Project', description: 'e2e smoke', client: 'Tester' }
	});
	expect(res.status(), `POST /api/projects failed: ${await res.text()}`).toBe(201);
	return (await res.json()).id;
}

async function createEstimation(page: Page, projectId: string): Promise<string> {
	const res = await page.request.post(`${API}/api/projects/${projectId}/estimations`, {
		headers: API_HEADERS,
		data: { offer: 'SMOKE-001', description: 'smoke estimation' }
	});
	expect(res.status(), `POST estimations failed: ${await res.text()}`).toBe(201);
	return (await res.json()).id;
}

async function createDraft(page: Page, estimationId: string): Promise<number> {
	const res = await page.request.post(`${API}/api/estimations/${estimationId}/versions`, {
		headers: API_HEADERS
	});
	expect(res.status(), `POST versions failed: ${await res.text()}`).toBe(201);
	return (await res.json()).versionNumber;
}

async function populateDraft(page: Page, estimationId: string) {
	const res = await page.request.put(`${API}/api/estimations/${estimationId}/versions/draft`, {
		headers: API_HEADERS,
		data: {
			notes: 'smoke test notes',
			parameters: [
				{ name: 'dailyRate', value: 900 },
				{ name: 'stdDevFactor', value: 2 },
				{ name: 'salesSurcharge', value: 0.1 }
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
		headers: API_HEADERS,
		data: {
			parameters: [
				{ name: 'dailyRate', value: 900 },
				{ name: 'stdDevFactor', value: 0 }
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
	const res = await page.request.post(`${API}/api/estimations/${estimationId}/versions/draft/submit`, {
		headers: API_HEADERS
	});
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

	const putBodies: unknown[] = [];
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
	const fetched = await page.request.get(`${API}/api/estimations/${estimationId}/versions/draft`, { headers: API_HEADERS }).then((r) => r.json());
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

// ── DnD / UI tree-building (task-057) ──────────────────────────────────────

/** PUT a custom roots tree to the draft. */
async function putRoots(page: Page, estimationId: string, roots: unknown[]) {
	const res = await page.request.put(`${API}/api/estimations/${estimationId}/versions/draft`, {
		headers: API_HEADERS,
		data: { roots }
	});
	expect(res.status(), `PUT roots failed: ${await res.text()}`).toBe(200);
}

/** Keyboard DnD helpers. svelte-dnd-action supports mouse + keyboard DnD;
    Playwright's mouse synthesis doesn't reliably trigger the library's
    pointer handlers (the lib refuses mousedown on inputs, and Playwright's
    dragTo lands the cursor unpredictably). Keyboard DnD is rock-solid and
    is also the accessibility-recommended interaction. Flow:
      1. focus a draggable row → press Space to pick up
      2. focus a different zone or use ArrowUp/Down within the same zone
      3. press Space to drop
    This is the same flow exposed to keyboard users in production. */
async function keyboardReorder(page: Page, sourceRow: Locator, direction: 'up' | 'down') {
	await sourceRow.focus();
	await page.waitForTimeout(100);
	await page.keyboard.press('Space');
	await page.waitForTimeout(200);
	await page.keyboard.press(direction === 'down' ? 'ArrowDown' : 'ArrowUp');
	await page.waitForTimeout(200);
	await page.keyboard.press('Space');
	await page.waitForTimeout(1500); // autosave debounce
}

async function keyboardReparent(page: Page, sourceRow: Locator, targetZone: Locator) {
	await sourceRow.focus();
	await page.waitForTimeout(100);
	await page.keyboard.press('Space');
	await page.waitForTimeout(200);
	await targetZone.focus();
	await page.waitForTimeout(200);
	await page.keyboard.press('Space');
	await page.waitForTimeout(1500); // autosave debounce
}

test('build a three-level tree via UI buttons', async ({ page }) => {
	const projectId = await createProject(page);
	const estimationId = await createEstimation(page, projectId);
	const versionNumber = await createDraft(page, estimationId);

	await page.goto(`/estimations/${estimationId}/versions/${versionNumber}?draft=true`);
	await page.waitForLoadState('networkidle');

	// Empty-state "Gruppe hinzufügen" button creates the first root group with a default leaf.
	await page.locator('button', { hasText: 'Gruppe hinzufügen' }).first().click();
	await page.waitForTimeout(200);

	// "+ Gruppe" button on the root group adds a nested sub-group.
	await page.locator('[data-testid="row-0"] button', { hasText: '+ Gruppe' }).click();
	await page.waitForTimeout(200);

	// "+ Element" button on the nested sub-group adds a leaf inside it.
	await page.locator('[data-testid="row-0-1"] button', { hasText: '+ Element' }).click();
	await page.waitForTimeout(1500); // autosave debounce + flush

	// Reload and verify via the REST API that the structure survived.
	await page.reload();
	await page.waitForLoadState('networkidle');
	const fetched = await page.request.get(`${API}/api/estimations/${estimationId}/versions/draft`, { headers: API_HEADERS }).then((r) => r.json());
	expect(fetched.roots).toHaveLength(1);
	expect(fetched.roots[0].type).toBe('GROUP');
	// Default root group contains a default leaf (from addRootGroup) + the new sub-group with its leaf.
	const subGroup = fetched.roots[0].children.find((c: FetchedNode) => c.type === 'GROUP');
	expect(subGroup, 'expected a nested GROUP under the root').toBeTruthy();
	expect(subGroup.children.length).toBeGreaterThanOrEqual(1);
});

test('drag a leaf into a different group reparents it', async ({ page }) => {
	const projectId = await createProject(page);
	const estimationId = await createEstimation(page, projectId);
	const versionNumber = await createDraft(page, estimationId);

	// Two top-level groups, one leaf each.
	await putRoots(page, estimationId, [
		{
			type: 'GROUP', title: 'G1',
			children: [{ type: 'FIXED', description: 'L1', minEffort: 1, expectedEffort: 2, maxEffort: 3 }]
		},
		{
			type: 'GROUP', title: 'G2',
			children: [{ type: 'FIXED', description: 'L2', minEffort: 1, expectedEffort: 2, maxEffort: 3 }]
		}
	]);

	await page.goto(`/estimations/${estimationId}/versions/${versionNumber}?draft=true`);
	await page.waitForLoadState('networkidle');

	// L1 sits at path 0-0 (root index 0 = G1, child index 0 = L1).
	// G2's children-zone is the dndzone div with aria-label="Unterelemente von G2".
	const sourceRow = page.locator('[data-testid="row-0-0"]');
	const targetZone = page.locator('[aria-label="Unterelemente von G2"]');
	await keyboardReparent(page, sourceRow, targetZone);

	const fetched = await page.request.get(`${API}/api/estimations/${estimationId}/versions/draft`, { headers: API_HEADERS }).then((r) => r.json());
	const g1 = fetched.roots.find((r: FetchedNode) => r.title === 'G1');
	const g2 = fetched.roots.find((r: FetchedNode) => r.title === 'G2');
	expect(g1.children).toHaveLength(0);
	expect(g2.children.length).toBe(2);
	const movedDescriptions = g2.children.map((c: FetchedNode) => c.description);
	expect(movedDescriptions).toContain('L1');
	expect(movedDescriptions).toContain('L2');
});

test('drag a leaf onto another leaf reorders siblings', async ({ page }) => {
	const projectId = await createProject(page);
	const estimationId = await createEstimation(page, projectId);
	const versionNumber = await createDraft(page, estimationId);

	await putRoots(page, estimationId, [
		{
			type: 'GROUP', title: 'G1',
			children: [
				{ type: 'FIXED', description: 'A', minEffort: 1, expectedEffort: 2, maxEffort: 3 },
				{ type: 'FIXED', description: 'B', minEffort: 1, expectedEffort: 2, maxEffort: 3 }
			]
		}
	]);

	await page.goto(`/estimations/${estimationId}/versions/${versionNumber}?draft=true`);
	await page.waitForLoadState('networkidle');

	const a = page.locator('[data-testid="row-0-0"]'); // A at path 0-0

	// Reorder A down past B via keyboard.
	await keyboardReorder(page, a, 'down');

	const fetched = await page.request.get(`${API}/api/estimations/${estimationId}/versions/draft`, { headers: API_HEADERS }).then((r) => r.json());
	const g1 = fetched.roots[0];
	expect(g1.children).toHaveLength(2);
	const descriptions = g1.children.map((c: FetchedNode) => c.description);
	expect(descriptions).toEqual(['B', 'A']);
});

test('dragging a group onto its own descendant is a no-op (cycle protection)', async ({ page }) => {
	const projectId = await createProject(page);
	const estimationId = await createEstimation(page, projectId);
	const versionNumber = await createDraft(page, estimationId);

	// G1 > InnerG > Leaf — dragging G1 INTO Leaf would create a cycle.
	await putRoots(page, estimationId, [
		{
			type: 'GROUP', title: 'G1',
			children: [
				{
					type: 'GROUP', title: 'InnerG',
					children: [{ type: 'FIXED', description: 'Leaf', minEffort: 1, expectedEffort: 2, maxEffort: 3 }]
				}
			]
		}
	]);

	await page.goto(`/estimations/${estimationId}/versions/${versionNumber}?draft=true`);
	await page.waitForLoadState('networkidle');

	const g1Row = page.locator('[data-testid="row-0"]');
	// InnerG's children-zone has aria-label="Unterelemente von InnerG"; dropping G1
	// inside it would create a cycle (G1 contains InnerG, can't contain itself).
	const innerGZone = page.locator('[aria-label="Unterelemente von InnerG"]');
	await keyboardReparent(page, g1Row, innerGZone);

	// Tree must be unchanged — cycle protection restored the snapshot.
	const fetched = await page.request.get(`${API}/api/estimations/${estimationId}/versions/draft`, { headers: API_HEADERS }).then((r) => r.json());
	expect(fetched.roots).toHaveLength(1);
	expect(fetched.roots[0].title).toBe('G1');
	expect(fetched.roots[0].children).toHaveLength(1);
	expect(fetched.roots[0].children[0].title).toBe('InnerG');
	expect(fetched.roots[0].children[0].children).toHaveLength(1);
	expect(fetched.roots[0].children[0].children[0].description).toBe('Leaf');
});

test('drag a subgroup into a deeper nested subgroup reparents it', async ({ page }) => {
	const projectId = await createProject(page);
	const estimationId = await createEstimation(page, projectId);
	const versionNumber = await createDraft(page, estimationId);

	// G1 > Sub (a subgroup with one leaf), and a separately nested
	// G2 > Mid > Deep (Deep is a depth-2 subgroup). We drag Sub into Deep.
	await putRoots(page, estimationId, [
		{
			type: 'GROUP', title: 'G1',
			children: [
				{
					type: 'GROUP', title: 'Sub',
					children: [{ type: 'FIXED', description: 'SubLeaf', minEffort: 1, expectedEffort: 2, maxEffort: 3 }]
				}
			]
		},
		{
			type: 'GROUP', title: 'G2',
			children: [
				{
					type: 'GROUP', title: 'Mid',
					children: [
						{
							type: 'GROUP', title: 'Deep',
							children: [{ type: 'FIXED', description: 'DeepLeaf', minEffort: 1, expectedEffort: 2, maxEffort: 3 }]
						}
					]
				}
			]
		}
	]);

	await page.goto(`/estimations/${estimationId}/versions/${versionNumber}?draft=true`);
	await page.waitForLoadState('networkidle');

	// Sub is the first child of G1 (path 0-0). Drop it into Deep's children zone.
	const subRow = page.locator('[data-testid="row-0-0"]');
	const deepZone = page.locator('[aria-label="Unterelemente von Deep"]');
	await keyboardReparent(page, subRow, deepZone);

	const fetched = await page.request.get(`${API}/api/estimations/${estimationId}/versions/draft`, { headers: API_HEADERS }).then((r) => r.json());
	const g1 = fetched.roots.find((r: FetchedNode) => r.title === 'G1');
	const g2 = fetched.roots.find((r: FetchedNode) => r.title === 'G2');
	// Sub left G1 entirely...
	expect(g1.children).toHaveLength(0);
	// ...and now lives inside Deep (G2 > Mid > Deep), carrying its own leaf.
	const deep = g2.children[0].children[0];
	expect(deep.title).toBe('Deep');
	const deepChildTitles = deep.children.map((c: FetchedNode) => c.title ?? c.description);
	expect(deepChildTitles).toContain('Sub');
	expect(deepChildTitles).toContain('DeepLeaf');
	const sub = deep.children.find((c: FetchedNode) => c.title === 'Sub');
	expect(sub.children.map((c: FetchedNode) => c.description)).toEqual(['SubLeaf']);
});
