import { test, expect, type Page } from '@playwright/test';

// Guards the alignment invariant: the header grid and every body-row grid
// must resolve to pixel-identical column tracks, at any depth and after a
// subtree is collapsed/expanded. Each row is its own CSS grid sharing only
// the grid-template STRING, so a flexible track that grows to fit deep-indent
// content in one row (but not the header) would drift the columns. See
// task-061.

function parseTracks(value: string): number[] {
	return value
		.trim()
		.split(/\s+/)
		.map((t) => parseFloat(t));
}

function expectAligned(headerTpl: string, rowTpl: string) {
	const h = parseTracks(headerTpl);
	const r = parseTracks(rowTpl);
	expect(r.length).toBe(h.length);
	for (let i = 0; i < h.length; i++) {
		// allow sub-pixel rounding only
		expect(Math.abs(h[i] - r[i])).toBeLessThanOrEqual(0.5);
	}
}

async function headerTracks(page: Page): Promise<string> {
	return page.evaluate(() => {
		const header = [...document.querySelectorAll('.grid')].find((e) =>
			e.className.includes('bg-brand-green')
		) as HTMLElement;
		return getComputedStyle(header).gridTemplateColumns;
	});
}

async function rowTracks(page: Page, testid: string): Promise<string> {
	return page.evaluate((id) => {
		const row = document.querySelector(`[data-testid="${id}"] > .grid`) as HTMLElement;
		return getComputedStyle(row).gridTemplateColumns;
	}, testid);
}

test('TreeTable header and nested rows share identical column tracks', async ({ page }) => {
	await page.setViewportSize({ width: 1280, height: 720 });
	await page.goto('/dev/tree-table-demo');
	await page.waitForLoadState('networkidle');

	const header = await headerTracks(page);
	// depth-1 group and depth-2 leaf must both line up with the header
	expectAligned(header, await rowTracks(page, 'tt-row-g1a'));
	expectAligned(header, await rowTracks(page, 'tt-row-l1'));
});

test('TreeTable alignment holds after a collapsed subtree is re-expanded', async ({ page }) => {
	await page.setViewportSize({ width: 1280, height: 720 });
	await page.goto('/dev/tree-table-demo');
	await page.waitForLoadState('networkidle');

	// Collapse then re-expand g1 so its subtree is freshly re-rendered.
	await page.locator('[data-testid="tt-row-g1"] > .grid button[aria-label="Collapse"]').click();
	await page.locator('[data-testid="tt-row-g1"] > .grid button[aria-label="Expand"]').click();

	const header = await headerTracks(page);
	expectAligned(header, await rowTracks(page, 'tt-row-l1'));
});