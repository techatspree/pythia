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

async function openSchedule(page: Page, estimationId: string) {
	await page.goto(`/estimations/${estimationId}/versions/draft`);
	await page.waitForLoadState('networkidle');
	await page.getByTestId('schedule-section-toggle').click();
	await expect(page.getByTestId('dependency-editor')).toBeVisible();
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
	await page.getByTestId('schedule-section-toggle').click();
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

test('a cycle is reported rather than prevented, and the durations hide', async ({
	page,
	request
}) => {
	const { estimationId, groupA, groupB } = await seed(request);
	await openSchedule(page, estimationId);

	await dragDependency(page, groupA, groupB);
	await dragDependency(page, groupB, groupA);

	await expect(page.getByTestId('schedule-cycle')).toBeVisible();
	await expect(page.getByTestId('schedule-durations')).toHaveCount(0);
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
