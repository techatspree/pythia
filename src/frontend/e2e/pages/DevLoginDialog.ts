import type { Page } from '@playwright/test';
import { expect } from '@playwright/test';

/** The dev-auth login dialog shown when no account is signed in. */
export class DevLoginDialog {
	constructor(private readonly page: Page) {}

	async expectVisible(): Promise<void> {
		await expect(this.page.getByTestId('dev-login.dialog')).toBeVisible();
	}

	/** Each seeded dev user gets its own `dev-login-<subjectId>` button. */
	async expectUserOffered(subjectId: string): Promise<void> {
		await expect(this.page.getByTestId(`dev-login-${subjectId}`)).toBeVisible();
	}
}
