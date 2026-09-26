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
}
