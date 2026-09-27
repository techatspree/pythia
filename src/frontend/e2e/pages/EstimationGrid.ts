import type { Locator, Page } from '@playwright/test';

/**
 * The PERT estimation grid. Rows are addressed by their PATH
 * (`row-0`, `row-0-1`), and a group's drop zone by the group's TITLE — both
 * test data, never a translated label.
 */
export class EstimationGrid {
	constructor(private readonly page: Page) {}

	row(path: string): Locator {
		return this.page.locator(`[data-testid="row-${path}"]`);
	}

	/** The children dndzone of the group with this title. */
	childrenZoneOf(groupTitle: string): Locator {
		return this.page.locator(`[data-zone-of="${groupTitle}"]`);
	}

	/** Adds the first root group — rendered in the empty state and in the footer. */
	async addRootGroup(): Promise<void> {
		await this.page.getByTestId('grid.add-group-row').first().click();
	}

	addChildGroupIn(row: Locator): Locator {
		return row.getByTestId('grid.add-group').first();
	}

	addChildItemIn(row: Locator): Locator {
		return row.getByTestId('grid.add-item').first();
	}
}
