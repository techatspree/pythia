import type { Locator, Page } from '@playwright/test';
import { expect } from '@playwright/test';

/** The 409 structure-drift dialog shown when a Merlin document no longer matches. */
export class MerlinStructureDialog {
	constructor(private readonly page: Page) {}

	get dialog(): Locator {
		return this.page.getByTestId('merlin-structure.dialog');
	}

	async expectVisible(): Promise<void> {
		await expect(this.dialog).toBeVisible();
	}

	async overwrite(): Promise<void> {
		await this.page.getByTestId('merlin-structure.overwrite').click();
	}

	async cancel(): Promise<void> {
		await this.page.getByTestId('merlin-structure.cancel').click();
	}
}
