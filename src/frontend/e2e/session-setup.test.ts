import { test, expect, type APIRequestContext } from '@playwright/test';

/**
 * The session setup page's item picker (task-151).
 *
 * These pin BEHAVIOUR, not styling — no assertions on class strings.
 *
 * The load-bearing one is the reachability check: the picker used to render
 * every leaf of the draft with no height cap, so a realistic estimation pushed
 * the session-title field, the moderator checkbox and "Sitzung starten" below
 * the fold and the form looked as if it had no submit at all. The list is now
 * capped and scrolls internally.
 */

const API = 'http://localhost:8090';
const H = { Authorization: 'Dev dev-admin' } as const;
const JSON_H = { 'Content-Type': 'application/json', ...H } as const;

/** A draft with enough leaves to overflow the viewport, nested under groups. */
async function seedLargeDraft(req: APIRequestContext) {
	const proj = await req.post(`${API}/api/projects`, {
		headers: JSON_H,
		data: { name: `E2E Setup ${Date.now()}`, client: 'Tester' }
	});
	expect(proj.status()).toBe(201);
	const projectId = (await proj.json()).id;

	const est = await req.post(`${API}/api/projects/${projectId}/estimations`, {
		headers: JSON_H,
		data: { offer: `SETUP-${Date.now()}`, description: 'setup picker' }
	});
	expect(est.status()).toBe(201);
	const estimationId = (await est.json()).id;

	const ver = await req.post(`${API}/api/estimations/${estimationId}/versions`, { headers: H });
	expect(ver.status()).toBe(201);

	// Two groups of 20. Half the leaves carry a triple (so they render the
	// "already estimated" chip and are NOT default-selected), half are 0/0/0.
	const groupOf = (title: string, offset: number) => ({
		type: 'GROUP',
		title,
		children: Array.from({ length: 20 }, (_, i) => ({
			type: 'FIXED',
			description: `Leaf ${offset + i}`,
			minEffort: i % 2 === 0 ? 0 : 1,
			expectedEffort: i % 2 === 0 ? 0 : 2,
			maxEffort: i % 2 === 0 ? 0 : 3
		}))
	});

	const put = await req.put(`${API}/api/estimations/${estimationId}/versions/draft`, {
		headers: JSON_H,
		data: { dailyRate: 900, stdDevFactor: 2, roots: [groupOf('Alpha', 0), groupOf('Beta', 100)] }
	});
	expect(put.status()).toBe(200);

	return { projectId, estimationId };
}

test('the item list scrolls internally instead of stretching the page', async ({ page }) => {
	const { projectId, estimationId } = await seedLargeDraft(page.request);

	// task-128's pre-scoped entry point: skips driving the two dropdowns.
	await page.goto(`/sessions?projectId=${projectId}&estimationId=${estimationId}`);
	await page.waitForLoadState('networkidle');

	// All 40 leaves are present…
	const rows = page.locator('label:has(input[type="checkbox"][class*="accent-brand-green"])');
	await expect(rows).toHaveCount(41); // 40 items + the "moderator estimates" checkbox

	// …but the list is capped and overflows INSIDE itself rather than pushing
	// the rest of the form down. This is the invariant the fix provides;
	// asserting the button's initial viewport position instead would just be
	// measuring how much chrome happens to sit above it.
	const box = await page
		.locator('.max-h-72')
		.first()
		.evaluate((el) => ({ client: el.clientHeight, scroll: el.scrollHeight }));
	expect(box.client).toBeLessThanOrEqual(300);
	expect(box.scroll).toBeGreaterThan(box.client);

	// The page therefore stays a normal length: 40 uncapped rows measured well
	// over 2000px before the cap.
	const pageHeight = await page.evaluate(() => document.body.scrollHeight);
	expect(pageHeight).toBeLessThan(1200);

	// And the submit is reachable — one short scroll, not a marathon.
	const start = page.getByRole('button', { name: 'Sitzung starten', exact: true });
	await start.scrollIntoViewIfNeeded();
	await expect(start).toBeInViewport();
});

test('the selected count tracks the checkboxes', async ({ page }) => {
	const { projectId, estimationId } = await seedLargeDraft(page.request);
	await page.goto(`/sessions?projectId=${projectId}&estimationId=${estimationId}`);
	await page.waitForLoadState('networkidle');

	// Only the not-yet-estimated leaves are preselected (task-128): 10 per group.
	const count = page.getByText(/^\d+ von 40 ausgewählt$/);
	await expect(count).toHaveText('20 von 40 ausgewählt');

	await page.getByRole('button', { name: 'Auswahl aufheben', exact: true }).click();
	await expect(count).toHaveText('0 von 40 ausgewählt');

	// With nothing selected the reason for the dead start button is stated.
	await expect(page.getByText('Mindestens ein Eintrag muss ausgewählt sein.')).toBeVisible();

	await page.getByRole('button', { name: 'Alle auswählen', exact: true }).click();
	await expect(count).toHaveText('40 von 40 ausgewählt');
});

test('the panels on the page share one width', async ({ page }) => {
	const { projectId, estimationId } = await seedLargeDraft(page.request);
	await page.goto(`/sessions?projectId=${projectId}&estimationId=${estimationId}`);
	await page.waitForLoadState('networkidle');

	// The reported symptom: "open sessions" sat at max-w-3xl while the item
	// picker's block was max-w-2xl, a 6rem step visible as a ragged right edge.
	// `Page` now owns the column and its children carry no width of their own,
	// so this fails the moment someone re-adds a max-w to a child.
	const cards = page.locator('.border.rounded-lg.overflow-hidden');
	expect(await cards.count()).toBeGreaterThanOrEqual(2);

	const widths: number[] = [];
	for (let i = 0; i < (await cards.count()); i++) {
		const box = await cards.nth(i).boundingBox();
		if (box) widths.push(box.width);
	}
	const min = Math.min(...widths);
	const max = Math.max(...widths);
	expect(max - min).toBeLessThanOrEqual(1);
});

test('each item shows the group it sits in', async ({ page }) => {
	const { projectId, estimationId } = await seedLargeDraft(page.request);
	await page.goto(`/sessions?projectId=${projectId}&estimationId=${estimationId}`);
	await page.waitForLoadState('networkidle');

	// The picker flattens the tree, so without the ancestor path two leaves in
	// different groups would be indistinguishable.
	await expect(page.getByText('Alpha', { exact: true }).first()).toBeVisible();
	await expect(page.getByText('Beta', { exact: true }).first()).toBeVisible();
});
