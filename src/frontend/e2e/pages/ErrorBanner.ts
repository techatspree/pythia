import type { Page } from '@playwright/test';
import { expect } from '@playwright/test';

/** The shared error banner (`role="alert"`). */
export class ErrorBanner {
	constructor(private readonly page: Page) {}

	async expectVisible(): Promise<void> {
		await expect(this.page.getByTestId('error-banner')).toBeVisible();
	}

	async expectMessage(pattern: RegExp | string): Promise<void> {
		await expect(this.page.getByTestId('error-banner')).toContainText(pattern);
	}
}
