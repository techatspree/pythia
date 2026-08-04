import { test, expect, type Page, type Locator } from '@playwright/test';

async function keyboardReorder(page: Page, sourceRow: Locator, direction: 'up' | 'down') {
	await sourceRow.focus();
	await page.waitForTimeout(100);
	await page.keyboard.press('Space');
	await page.waitForTimeout(200);
	await page.keyboard.press(direction === 'down' ? 'ArrowDown' : 'ArrowUp');
	await page.waitForTimeout(200);
	await page.keyboard.press('Space');
	await page.waitForTimeout(400);
}

async function keyboardReparent(page: Page, sourceRow: Locator, targetZone: Locator) {
	await sourceRow.focus();
	await page.waitForTimeout(100);
	await page.keyboard.press('Space');
	await page.waitForTimeout(200);
	await targetZone.focus();
	await page.waitForTimeout(200);
	await page.keyboard.press('Space');
	await page.waitForTimeout(400);
}

test('tree-table demo renders header, all rows, and footer total', async ({ page }) => {
	await page.goto('/dev/tree-table-demo');
	await page.waitForLoadState('networkidle');

	for (const header of ['Name', 'Menge', 'Stückpreis', 'Summe']) {
		await expect(page.locator('text=' + header).first()).toBeVisible();
	}

	for (const id of ['g1', 'g1a', 'g1b', 'g2', 'g2a', 'g2b', 'l1', 'l2', 'l3', 'l4', 'l5', 'l6', 'l7', 'l8']) {
		await expect(page.locator(`[data-testid="tt-row-${id}"]`)).toBeVisible();
	}

	await expect(page.locator('text=/Total: €[0-9]/')).toBeVisible();
});

test('collapsing a group hides its descendants', async ({ page }) => {
	await page.goto('/dev/tree-table-demo');
	await page.waitForLoadState('networkidle');

	await expect(page.locator('[data-testid="tt-row-l1"]')).toBeVisible();
	await expect(page.locator('[data-testid="tt-row-g1a"]')).toBeVisible();

	await page.locator('button[aria-label="Collapse"]').first().click();
	await page.waitForTimeout(200);

	// Descendants stay in the DOM by design (task-134): a group's dndzone must
	// exist at DRAG START to be a valid drop target, so collapsing hides the
	// rows visually (`h-0 overflow-hidden invisible`) instead of removing them.
	// Assert what the user perceives — hidden, and taking up no space.
	await expect(page.locator('[data-testid="tt-row-g1a"]')).toBeHidden();
	await expect(page.locator('[data-testid="tt-row-l1"]')).toBeHidden();
	expect((await page.locator('[data-testid="tt-row-g1a"]').boundingBox())?.height ?? 0).toBe(0);
	await expect(page.locator('[data-testid="tt-row-g2"]')).toBeVisible();
});

test('keyboard reorder within a group', async ({ page }) => {
	await page.goto('/dev/tree-table-demo');
	await page.waitForLoadState('networkidle');

	const l1Row = page.locator('[data-testid="tt-row-l1"]');
	await keyboardReorder(page, l1Row, 'down');

	const visibleLeafIds = await page.evaluate(() => {
		const zones = Array.from(document.querySelectorAll('[aria-label="Children"]'));
		for (const zone of zones) {
			const rows = Array.from(zone.querySelectorAll('[data-testid^="tt-row-l"]'));
			if (rows.some((r) => r.getAttribute('data-testid') === 'tt-row-l1')) {
				return rows.map((r) => r.getAttribute('data-testid'));
			}
		}
		return [];
	});

	expect(visibleLeafIds.indexOf('tt-row-l1')).toBeGreaterThan(visibleLeafIds.indexOf('tt-row-l2'));
});

test('keyboard reparent across groups', async ({ page }) => {
	await page.goto('/dev/tree-table-demo');
	await page.waitForLoadState('networkidle');

	const l1Row = page.locator('[data-testid="tt-row-l1"]');
	const g2bZone = page.locator('[data-testid="tt-row-g2b"] > [aria-label="Children"]').first();
	await keyboardReparent(page, l1Row, g2bZone);

	await expect(page.locator('[data-testid="tt-row-l1"]')).toHaveCount(1);
});

test('cycle protection rejects dropping a group into its descendant', async ({ page }) => {
	await page.goto('/dev/tree-table-demo');
	await page.waitForLoadState('networkidle');

	const initialRowCount = await page.locator('[data-testid^="tt-row-"]').count();

	const g1Row = page.locator('[data-testid="tt-row-g1"]');
	const g1aZone = page.locator('[data-testid="tt-row-g1a"] > [aria-label="Children"]').first();
	await keyboardReparent(page, g1Row, g1aZone);

	await expect(page.locator('[data-testid^="tt-row-"]')).toHaveCount(initialRowCount);
	await expect(page.locator('[data-testid="tt-row-g1"]')).toHaveCount(1);
});
