import type { Locator, Page } from '@playwright/test';
import { expect } from '@playwright/test';

/** Shown when an undo is refused with 409 because another user moved the draft on. */
export class UndoConflictDialog {
	constructor(private readonly page: Page) {}

	/** Exposed so a spec can assert visibility, hiding, and the blocking user's name. */
	get dialog(): Locator {
		return this.page.getByTestId('undo-conflict.dialog');
	}

	async expectVisible(): Promise<void> {
		await expect(this.dialog).toBeVisible();
	}

	async expectHidden(): Promise<void> {
		await expect(this.dialog).toBeHidden();
	}

	async reload(): Promise<void> {
		await this.page.getByTestId('undo-conflict.reload').click();
	}
}
