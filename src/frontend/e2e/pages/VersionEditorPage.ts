import type { Locator, Page } from '@playwright/test';

/** The version editor at `/estimations/<id>/versions/<n>`. */
export class VersionEditorPage {
	constructor(private readonly page: Page) {}

	async gotoDraft(estimationId: string, versionNumber: number | string): Promise<void> {
		await this.page.goto(`/estimations/${estimationId}/versions/${versionNumber}?draft=true`);
		await this.page.waitForLoadState('networkidle');
	}

	/**
	 * A grid cell by its `data-cell="<row>-<row>-<col>"` coordinate. Structural,
	 * not a translated label — the grid addresses cells by position.
	 */
	cell(coordinate: string): Locator {
		return this.page.locator(`[data-cell="${coordinate}"]`);
	}

	/** A SUBMITTED version (no `?draft=true`). */
	async gotoSubmitted(estimationId: string, versionNumber: number): Promise<void> {
		await this.page.goto(`/estimations/${estimationId}/versions/${versionNumber}`);
		await this.page.waitForLoadState('networkidle');
	}

	/** The summary panel's offer-PT figure, as rendered (parse with `parseNumber`). */
	get summaryOfferPT(): Locator {
		return this.page.getByTestId('version-summary.offerPT');
	}

	/** The summary panel's conversion-basis line (task-213). */
	get summaryHoursPerDay(): Locator {
		return this.page.getByTestId('version-summary.hours-per-day');
	}
}
