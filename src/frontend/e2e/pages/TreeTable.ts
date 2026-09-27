import type { Locator, Page } from '@playwright/test';
import { expect } from '@playwright/test';

/**
 * The generic TreeTable. Shared by the PERT grid, both bucket views and the
 * `/dev/tree-table-demo` fixture, so it addresses rows and columns by KEY —
 * never by header text. The demo fixture is deliberately un-internationalised
 * (see src/frontend/CLAUDE.md), which is exactly why its German column labels
 * must not be selectors.
 */
export class TreeTable {
	constructor(private readonly page: Page) {}

	/** A header cell by its column key (`name`, `quantity`, `unitPrice`, …). */
	headerCell(columnKey: string): Locator {
		return this.page.getByTestId(`tt-header-cell.${columnKey}`);
	}

	row(id: string): Locator {
		return this.page.locator(`[data-testid="tt-row-${id}"]`);
	}

	async expectColumnVisible(columnKey: string): Promise<void> {
		await expect(this.headerCell(columnKey)).toBeVisible();
	}

	/**
	 * A collapsed column keeps its header cell but renders no label, so assert
	 * on the text rather than the element's presence.
	 */
	async expectColumnCollapsed(columnKey: string): Promise<void> {
		await expect(this.headerCell(columnKey)).toHaveText('');
	}

	async expectRowsVisible(ids: readonly string[]): Promise<void> {
		for (const id of ids) {
			await expect(this.row(id)).toBeVisible();
		}
	}
}
