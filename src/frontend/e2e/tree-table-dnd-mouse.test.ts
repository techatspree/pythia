import { test, expect, type Page } from '@playwright/test';

/**
 * Pointer (mouse) drag-and-drop tests for the generic TreeTable.
 *
 * The existing tree-table.test.ts exercises the *keyboard* DnD path, which
 * dispatches a finalize directly. The *pointer* path is different: while the
 * pointer moves, svelte-dnd-action inserts a shadow-placeholder item (carrying
 * the special id `id:dnd-shadow-placeholder-0000`) into the `consider` events,
 * and it requires the items handed back to the zone to preserve that shadow
 * identity. These tests reproduce two pointer-only regressions:
 *   1. reordering inside a nested subgroup does nothing, and
 *   2. reordering at the root level updates the model but not the DOM.
 */

const ROOT_ZONE = '[aria-label="Root nodes"]';
const childrenZone = (groupId: string) => `[data-testid="tt-row-${groupId}"] > [aria-label="Children"]`;

/** Direct child row ids of a dndzone (ignores deeper descendants). */
async function directChildIds(page: Page, zoneSelector: string): Promise<string[] | null> {
	return page.evaluate((sel) => {
		const zone = document.querySelector(sel);
		if (!zone) return null;
		return Array.from(zone.children)
			.map((c) => c.getAttribute('data-testid'))
			.filter((t): t is string => !!t && t.startsWith('tt-row-'))
			.map((t) => t.replace('tt-row-', ''));
	}, zoneSelector);
}

/**
 * Drag the source row's handle onto a target row via real pointer events.
 * Returns whether svelte-dnd-action actually engaged (the floating
 * `#dnd-action-dragged-el` is appended to <body> for the duration of a drag).
 */
async function mouseDragOnto(
	page: Page,
	sourceRowTestId: string,
	targetRowTestId: string,
	where: 'before' | 'after'
): Promise<boolean> {
	const handle = page.locator(`[data-testid="${sourceRowTestId}"] [data-dnd-handle]`).first();
	const sb = await handle.boundingBox();
	if (!sb) throw new Error('source row not found');

	const startX = sb.x + sb.width / 2;
	const startY = sb.y + sb.height / 2;

	await page.mouse.move(startX, startY);
	await page.mouse.down();
	// Exceed the 3px MIN_MOVEMENT_BEFORE_DRAG_START threshold to begin the drag.
	await page.mouse.move(startX, startY + 6, { steps: 4 });
	await page.waitForTimeout(150);

	const engaged = (await page.locator('#dnd-action-dragged-el').count()) > 0;

	// Capture the target AFTER the drag starts: picking up a tall subtree
	// collapses it to a single shadow row, so positions shift. Target the row's
	// own grid line (its first child), NOT the wrapper that encloses the subtree,
	// so the drop stays in the intended (parent) zone rather than a nested one.
	const tb = await page.locator(`[data-testid="${targetRowTestId}"] > div`).first().boundingBox();
	if (!tb) throw new Error('target row not found');
	const destX = tb.x + tb.width / 2;
	const destY = where === 'after' ? tb.y + tb.height * 0.85 : tb.y + tb.height * 0.15;
	await page.mouse.move(destX, destY, { steps: 12 });
	await page.waitForTimeout(200);
	// A small extra nudge so the library settles on the final index.
	await page.mouse.move(destX, destY + (where === 'after' ? 2 : -2), { steps: 2 });
	await page.waitForTimeout(200);
	await page.mouse.up();
	await page.waitForTimeout(450); // flipDurationMs (200) + finalize microtask + margin

	return engaged;
}

test.beforeEach(async ({ page }) => {
	await page.goto('/dev/tree-table-demo');
	await page.waitForLoadState('networkidle');
});

test('pointer drag engages svelte-dnd-action (sanity / positive control)', async ({ page }) => {
	const handle = page.locator('[data-testid="tt-row-g1"] [data-dnd-handle]').first();
	const sb = await handle.boundingBox();
	if (!sb) throw new Error('handle not found');
	await page.mouse.move(sb.x + sb.width / 2, sb.y + sb.height / 2);
	await page.mouse.down();
	await page.mouse.move(sb.x + sb.width / 2, sb.y + sb.height / 2 + 8, { steps: 4 });
	await page.waitForTimeout(150);

	// If the drag engaged, the floating dragged element exists in <body>.
	await expect(page.locator('#dnd-action-dragged-el')).toHaveCount(1);

	await page.mouse.up();
	await page.waitForTimeout(300);
});

test('pointer: reordering leaves inside a nested subgroup works', async ({ page }) => {
	expect(await directChildIds(page, childrenZone('g1a'))).toEqual(['l1', 'l2']);

	const engaged = await mouseDragOnto(page, 'tt-row-l1', 'tt-row-l2', 'after');
	expect(engaged, 'drag should have engaged svelte-dnd-action').toBe(true);

	// l1 should now sit after l2 inside g1a.
	expect(await directChildIds(page, childrenZone('g1a'))).toEqual(['l2', 'l1']);
});

test('pointer: reordering groups at the root updates the DOM immediately', async ({ page }) => {
	// Collapse both root groups first so they render no nested children zones —
	// the root dndzone is then the only drop target, making the reorder
	// unambiguous (and exercising the root zone, the one that previously failed
	// to update the DOM after a pointer drop).
	await page.locator('[data-testid="tt-row-g1"] > div button[aria-label="Collapse"]').first().click();
	await page.locator('[data-testid="tt-row-g2"] > div button[aria-label="Collapse"]').first().click();
	await page.waitForTimeout(150);

	expect(await directChildIds(page, ROOT_ZONE)).toEqual(['g1', 'g2']);

	const engaged = await mouseDragOnto(page, 'tt-row-g2', 'tt-row-g1', 'before');
	expect(engaged, 'drag should have engaged svelte-dnd-action').toBe(true);

	// After dropping g2 above g1, the rendered DOM order must reflect it without
	// any further interaction.
	expect(await directChildIds(page, ROOT_ZONE)).toEqual(['g2', 'g1']);
});
