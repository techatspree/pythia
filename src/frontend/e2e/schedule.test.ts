import { test, expect, type APIRequestContext, type Page } from '@playwright/test';

// The API request fixture carries no auth of its own; the dev module needs an
// explicit `Dev <subjectId>` header, as every other spec does.
const AUTH = { Authorization: 'Dev dev-admin' } as const;
const JSON_HEADERS = { 'Content-Type': 'application/json', ...AUTH } as const;

interface Seeded {
	estimationId: string;
	groupA: string;
	groupB: string;
	leafA: string;
	leafB: string;
}

/**
 * Two root groups, one leaf each, PERT. Seeded through the REST API the way the
 * other specs do, so the spec exercises the real persistence path (task-156).
 */
async function seed(request: APIRequestContext): Promise<Seeded> {
	const projectRes = await request.post('/api/projects', {
		headers: JSON_HEADERS,
		data: { name: `E2E Schedule ${Date.now()}` }
	});
	expect(projectRes.status()).toBe(201);
	const project = await projectRes.json();

	const estRes = await request.post(`/api/projects/${project.id}/estimations`, {
		headers: JSON_HEADERS,
		data: { offer: `E2E-SCHED-${Date.now()}`, method: 'THREE_POINT_PERT' }
	});
	expect(estRes.status()).toBe(201);
	const estimationId = (await estRes.json()).id;

	const versionRes = await request.post(`/api/estimations/${estimationId}/versions`, {
		headers: JSON_HEADERS
	});
	expect(versionRes.status()).toBe(201);

	const groupA = crypto.randomUUID();
	const groupB = crypto.randomUUID();
	const leafA = crypto.randomUUID();
	const leafB = crypto.randomUUID();

	// stdDevFactor 0 keeps the numbers exact: offerPT == mean, so a 10/10/10
	// leaf is exactly 10 days of work for one person.
	const putRes = await request.put(`/api/estimations/${estimationId}/versions/draft`, {
		headers: JSON_HEADERS,
		data: {
			stdDevFactor: 0.0,
			teamFte: 1,
			dependencies: [],
			roots: [
				{
					type: 'GROUP',
					logicalId: groupA,
					title: 'Alpha',
					children: [
						{
							type: 'FIXED',
							logicalId: leafA,
							description: 'Alpha work',
							minEffort: 10,
							expectedEffort: 10,
							maxEffort: 10
						}
					]
				},
				{
					type: 'GROUP',
					logicalId: groupB,
					title: 'Beta',
					children: [
						{
							type: 'FIXED',
							logicalId: leafB,
							description: 'Beta work',
							minEffort: 10,
							expectedEffort: 10,
							maxEffort: 10
						}
					]
				}
			]
		}
	});
	expect(putRes.status()).toBe(200);

	return { estimationId, groupA, groupB, leafA, leafB };
}

/** The graph has its own route since task-167. */
async function openSchedule(page: Page, estimationId: string) {
	await page.goto(`/estimations/${estimationId}/versions/draft/schedule?draft=true`);
	await page.waitForLoadState('networkidle');
	await expect(page.getByTestId('dependency-editor')).toBeVisible();
}

/**
 * The schedule page autosaves on a debounce, so a navigation straight after a
 * drag can outrun the PUT. Waiting on the API rather than on a fixed timeout
 * keeps this deterministic — the rest of the spec asserts client state, but
 * these tests need the OTHER route to load what was just drawn.
 */
async function waitForDependencies(
	request: APIRequestContext,
	estimationId: string,
	count: number
) {
	await expect
		.poll(
			async () => {
				const res = await request.get(`/api/estimations/${estimationId}/versions/draft`, {
					headers: AUTH
				});
				if (!res.ok()) return -1;
				const draft = (await res.json()) as { dependencies?: unknown[] };
				return (draft.dependencies ?? []).length;
			},
			{ timeout: 15_000 }
		)
		.toBe(count);
}

const card = (id: string) => `[data-testid="schedule-card"][data-logical-id="${id}"]`;

/**
 * HTML5 drag-and-drop, driven through the DataTransfer the browser would build.
 * Deliberately NOT a mouse-move drag: the pointer-driven helper in
 * bucket-views.test.ts exists for svelte-dnd-action's cursor-based detection,
 * and task-163 is still open on a settled-but-no-op drop there. This editor
 * uses native draggable/drop, which has no geometry race at all.
 */
async function dragDependency(page: Page, fromId: string, toId: string) {
	const handle = page.locator(`${card(fromId)} [data-testid="schedule-handle"]`);
	await expect(handle).toBeVisible();
	await handle.dragTo(page.locator(card(toId)));
}

test('a dependency lengthens the plan and survives a reload', async ({ page, request }) => {
	const { estimationId, groupA, groupB } = await seed(request);
	await openSchedule(page, estimationId);

	// One worker, two independent 10-day leaves: levelled to 20 days.
	await expect(page.getByTestId('schedule-planned-length')).toContainText('20');

	// Two workers: they run concurrently, so 10 days.
	await page.getByTestId('team-fte').fill('2');
	await expect(page.getByTestId('schedule-planned-length')).toContainText('10');

	// A dependency re-imposes the order even with capacity to spare.
	await dragDependency(page, groupA, groupB);
	await expect(page.getByTestId('schedule-edge')).toHaveCount(1);
	await expect(page.getByTestId('schedule-planned-length')).toContainText('20');

	// Autosave, then prove it round-tripped through the backend.
	await expect(page.getByTestId('save-status')).toBeVisible({ timeout: 10_000 }).catch(() => {});
	await page.waitForTimeout(1500);
	await page.reload();
	await page.waitForLoadState('networkidle');
	await expect(page.getByTestId('schedule-edge')).toHaveCount(1);
	await expect(page.getByTestId('team-fte')).toHaveValue('2');
	await expect(page.getByTestId('schedule-planned-length')).toContainText('20');
});

test('a group collapses and expands, and an edge on it survives expanding', async ({
	page,
	request
}) => {
	const { estimationId, groupA, groupB, leafA } = await seed(request);
	await openSchedule(page, estimationId);

	// Groups start collapsed (task-157), so only the two roots are cards.
	await expect(page.getByTestId('schedule-card')).toHaveCount(2);
	await expect(page.locator(card(leafA))).toHaveCount(0);

	// The edge is drawn on the COLLAPSED group.
	await dragDependency(page, groupA, groupB);
	await expect(page.getByTestId('schedule-edge')).toHaveCount(1);

	// Expanding reveals the child and keeps the edge — it collapses onto the
	// visible ancestor rather than disappearing.
	await page.locator(`${card(groupA)} [data-testid="schedule-toggle"]`).click();
	await expect(page.locator(card(leafA))).toBeVisible();
	await expect(page.getByTestId('schedule-edge')).toHaveCount(1);

	// And collapsing again is symmetrical.
	await page.locator(`${card(groupA)} [data-testid="schedule-toggle"]`).click();
	await expect(page.locator(card(leafA))).toHaveCount(0);
	await expect(page.getByTestId('schedule-edge')).toHaveCount(1);
});

test('a cycle is reported rather than prevented, and the figures hide', async ({
	page,
	request
}) => {
	const { estimationId, groupA, groupB } = await seed(request);
	await openSchedule(page, estimationId);

	await dragDependency(page, groupA, groupB);
	await dragDependency(page, groupB, groupA);

	await expect(page.getByTestId('schedule-cycle')).toBeVisible();
	await expect(page.getByTestId('schedule-planned-length')).toHaveCount(0);

	// The editor's critical-path column is driven by a set that is EMPTY on a
	// schedule error (task-170), so a cycle blanks the column rather than
	// leaving yesterday's dots on screen.
	await waitForDependencies(request, estimationId, 2);
	await page.goto(`/estimations/${estimationId}/versions/draft?draft=true`);
	await page.waitForLoadState('networkidle');
	await expect(page.getByTestId('grid-critical-dot')).toHaveCount(0);
});

test('the team size is a worker count, and 0 is refused by the domain', async ({
	page,
	request
}) => {
	const { estimationId } = await seed(request);
	await openSchedule(page, estimationId);

	await page.getByTestId('team-fte').fill('0');
	await expect(page.getByTestId('team-fte-error')).toBeVisible();

	// A fraction rounds DOWN to whole workers: 2.5 behaves as 2.
	await page.getByTestId('team-fte').fill('2.5');
	await expect(page.getByTestId('schedule-planned-length')).toContainText('10');
});

test('the version editor links to the schedule page, and keeps the numbers', async ({
	page,
	request
}) => {
	const { estimationId } = await seed(request);
	await page.goto(`/estimations/${estimationId}/versions/draft?draft=true`);
	await page.waitForLoadState('networkidle');
	await page.getByTestId('schedule-section-toggle').click();

	// The numbers stay in the editor; the graph does not — and neither does the
	// per-item table, which task-170 removed as a second copy of the grid.
	await expect(page.getByTestId('schedule-planned-length')).toBeVisible();
	await expect(page.getByTestId('schedule-durations')).toHaveCount(0);
	await expect(page.getByTestId('dependency-editor')).toHaveCount(0);

	// Criticality moved onto the estimation grid instead.
	await expect(page.getByText('Krit. Pfad', { exact: true })).toBeVisible();

	// The link opens the dedicated page, which renders the graph.
	await page.getByRole('link', { name: /Abhängigkeiten bearbeiten/ }).click();
	await page.waitForLoadState('networkidle');
	await expect(page.getByTestId('schedule-page-title')).toBeVisible();
	await expect(page.getByTestId('dependency-editor')).toBeVisible();

	// And back again.
	await page.getByTestId('schedule-page-back').click();
	await page.waitForLoadState('networkidle');
	await expect(page.getByTestId('editor-title')).toBeVisible().catch(() => {});
	expect(page.url()).not.toContain('/schedule');
});

test('the grid marks exactly the rows the schedule calls critical', async ({ page, request }) => {
	const { estimationId, groupA, groupB } = await seed(request);

	// Two independent 10-day branches at teamFte 1: levelling serialises them,
	// so the makespan is 20 and only the branch scheduled SECOND has zero
	// slack. One leaf plus its group therefore carry the dot, and the other
	// pair does not — which is what proves the column discriminates rather
	// than marking everything.
	await page.goto(`/estimations/${estimationId}/versions/draft?draft=true`);
	await page.waitForLoadState('networkidle');
	await expect(page.getByTestId('grid-critical-dot')).toHaveCount(2);

	// Chaining the two groups puts every row on the one critical chain.
	await openSchedule(page, estimationId);
	await dragDependency(page, groupA, groupB);
	await expect(page.getByTestId('schedule-edge')).toHaveCount(1);
	await waitForDependencies(request, estimationId, 1);

	await page.goto(`/estimations/${estimationId}/versions/draft?draft=true`);
	await page.waitForLoadState('networkidle');
	await expect(page.getByTestId('grid-critical-dot')).toHaveCount(4);
});

test('every edge carries a direction marker', async ({ page, request }) => {
	const { estimationId, groupA, groupB } = await seed(request);
	await openSchedule(page, estimationId);

	await dragDependency(page, groupA, groupB);
	const edge = page.getByTestId('schedule-edge').first();
	await expect(edge).toHaveAttribute('marker-end', 'url(#schedule-arrow)');
});

test('an expanded group outlines its subtree; a collapsed one does not', async ({
	page,
	request
}) => {
	const { estimationId, groupA, leafA } = await seed(request);
	await openSchedule(page, estimationId);

	// Groups start collapsed: a single card IS the group, so no outline.
	await expect(page.getByTestId('schedule-group-box')).toHaveCount(0);

	await page.locator(`${card(groupA)} [data-testid="schedule-toggle"]`).click();
	await expect(page.locator(card(leafA))).toBeVisible();

	// Expanded: exactly one outline, around this group and its child.
	const box = page.getByTestId('schedule-group-box');
	await expect(box).toHaveCount(1);
	await expect(box).toHaveAttribute('data-logical-id', groupA);

	// And the child is indented FURTHER right than its parent.
	const parentBox = await page.locator(card(groupA)).boundingBox();
	const childBox = await page.locator(card(leafA)).boundingBox();
	expect(parentBox).not.toBeNull();
	expect(childBox).not.toBeNull();
	expect(childBox!.x).toBeGreaterThan(parentBox!.x);

	// Collapsing removes the outline again.
	await page.locator(`${card(groupA)} [data-testid="schedule-toggle"]`).click();
	await expect(page.getByTestId('schedule-group-box')).toHaveCount(0);
});

test('a live arrow is drawn while dragging, before the drop', async ({ page, request }) => {
	// The arrow used to be driven by `pointermove`, which the browser SUPPRESSES
	// for the duration of a native HTML5 drag — so it never appeared. It is
	// driven by `dragover` now. Uses page.mouse rather than dragTo because
	// dragTo cannot observe the MID-drag state, and re-reads boundingBox for
	// every target rather than assuming fixed geometry.
	const { estimationId, groupA, groupB } = await seed(request);
	await openSchedule(page, estimationId);

	await expect(page.getByTestId('schedule-drag-arrow')).toHaveCount(0);

	const handle = page.locator(`${card(groupA)} [data-testid="schedule-handle"]`);
	const hb = await handle.boundingBox();
	const tb = await page.locator(card(groupB)).boundingBox();
	expect(hb).not.toBeNull();
	expect(tb).not.toBeNull();

	await handle.hover();
	await page.mouse.down();
	await page.mouse.move(hb!.x + hb!.width / 2 + 12, hb!.y + hb!.height / 2 + 12, { steps: 5 });
	await page.mouse.move(tb!.x + tb!.width / 2, tb!.y + tb!.height / 2, { steps: 12 });

	const arrow = page.getByTestId('schedule-drag-arrow');
	await expect(arrow).toHaveCount(1);
	// A real path, not an empty one.
	const d = await arrow.getAttribute('d');
	expect(d).toMatch(/^M \d/);
	// The live arrow has its OWN marker id: it lives in a separate overlay svg
	// (so it paints above the cards) and duplicate ids in one document are invalid.
	expect(await arrow.getAttribute('marker-end')).toBe('url(#schedule-arrow-live)');

	// It must paint ON TOP of the cards, or the half of it under the target card
	// is invisible — which is exactly where the user is looking.
	const stacking = await page.evaluate(() => {
		const arrow = document.querySelector('[data-testid="schedule-drag-arrow"]');
		const cardEl = document.querySelector('[data-testid="schedule-card"]');
		if (!arrow || !cardEl) return null;
		const layer = arrow.closest('svg');
		const z = (el: Element) => Number(getComputedStyle(el).zIndex) || 0;
		return {
			arrow: layer ? z(layer) : 0,
			card: z(cardEl),
			// the overlay must also come after the cards in document order
			afterCards: !!(layer && cardEl.compareDocumentPosition(layer) & Node.DOCUMENT_POSITION_FOLLOWING)
		};
	});
	expect(stacking).not.toBeNull();
	expect(stacking!.arrow).toBeGreaterThan(stacking!.card);
	expect(stacking!.afterCards).toBe(true);

	await page.mouse.up();
	// It disappears once the drag ends.
	await expect(page.getByTestId('schedule-drag-arrow')).toHaveCount(0);
});

test('a child card is never left of its parent, even in a later layer', async ({
	page,
	request
}) => {
	// `layer` comes from DEPENDENCIES alone, so an undependent child of a group
	// that sits in a later layer used to be placed at layer 0 — far to its
	// parent's LEFT, which destroyed the containment the outline implies.
	const { estimationId, groupA, groupB, leafA, leafB } = await seed(request);
	await openSchedule(page, estimationId);

	// groupA -> groupB puts groupB in a later layer; neither leaf has an edge.
	await dragDependency(page, groupA, groupB);

	await page.locator(`${card(groupA)} [data-testid="schedule-toggle"]`).click();
	await page.locator(`${card(groupB)} [data-testid="schedule-toggle"]`).click();

	for (const [group, leaf] of [
		[groupA, leafA],
		[groupB, leafB]
	] as const) {
		const parentBox = await page.locator(card(group)).boundingBox();
		const childBox = await page.locator(card(leaf)).boundingBox();
		expect(parentBox).not.toBeNull();
		expect(childBox).not.toBeNull();
		expect(childBox!.x).toBeGreaterThan(parentBox!.x);
	}
});
