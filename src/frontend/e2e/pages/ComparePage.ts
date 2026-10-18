import type { Locator, Page } from '@playwright/test';

/** The version comparison at `/estimations/<id>/compare?a=…&b=…`. */
export class ComparePage {
	constructor(private readonly page: Page) {}

	async open(estimationId: string, a: string | number, b: string | number): Promise<void> {
		await this.page.goto(`/estimations/${estimationId}/compare?a=${a}&b=${b}`);
		await this.page.waitForLoadState('networkidle');
	}

	/** A row of the parameter-changes table, by the parameter's field name (e.g. `hoursPerDay`). */
	parameterChange(name: string): Locator {
		return this.page.locator(`[data-testid="compare.parameter-change"][data-name="${name}"]`);
	}
}
