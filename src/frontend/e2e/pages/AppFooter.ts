import type { Page } from '@playwright/test';
import { expect } from '@playwright/test';

/** The app footer with the legal links (task-210), on every route for a signed-in user. */
export class AppFooter {
	constructor(private readonly page: Page) {}

	async expectVisible(): Promise<void> {
		await expect(this.page.getByRole('contentinfo')).toBeVisible();
		await expect(this.page.getByTestId('app-footer')).toBeVisible();
	}

	/** Opens the "Open-Source-Lizenzen" dialog; drive it with OssNoticesDialog. */
	async openOssNotices(): Promise<void> {
		await this.page.getByTestId('app-footer').getByTestId('app-footer.oss-notices').click();
	}
}
