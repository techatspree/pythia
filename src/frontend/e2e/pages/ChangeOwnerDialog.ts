import type { Page } from '@playwright/test';
import { expect } from '@playwright/test';

/** The change-owner modal on a project's detail page (task-200). */
export class ChangeOwnerDialog {
	constructor(private readonly page: Page) {}

	async expectVisible(): Promise<void> {
		await expect(this.page.getByTestId('change-owner-dialog')).toBeVisible();
	}

	async expectHidden(): Promise<void> {
		await expect(this.page.getByTestId('change-owner-dialog')).toHaveCount(0);
	}

	/** Picks a person by display name — test DATA (a seeded user), not a UI label. */
	async selectOwnerByDisplayName(displayName: string): Promise<void> {
		const select = this.page.getByTestId('change-owner-dialog.select');
		// The options arrive with GET /api/users after the dialog opens.
		await expect(select.locator('option', { hasText: displayName })).toHaveCount(1);
		await select.selectOption({ label: displayName });
	}

	async save(): Promise<void> {
		await this.page.getByTestId('change-owner-dialog.save').click();
	}

	async cancel(): Promise<void> {
		await this.page.getByTestId('change-owner-dialog.cancel').click();
	}
}
