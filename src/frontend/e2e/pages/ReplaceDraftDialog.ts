import type { Locator, Page } from '@playwright/test';
import { expect } from '@playwright/test';

/** The "replace the existing draft?" confirmation, shared by Merlin and xlsx import. */
export class ReplaceDraftDialog {
	constructor(private readonly page: Page) {}

	get dialog(): Locator {
		return this.page.getByTestId('replace-draft.dialog');
	}

	async expectVisible(): Promise<void> {
		await expect(this.dialog).toBeVisible();
	}

	async confirm(): Promise<void> {
		await this.page.getByTestId('replace-draft.confirm').click();
	}

	async cancel(): Promise<void> {
		await this.page.getByTestId('replace-draft.cancel').click();
	}
}
