import type { Page } from '@playwright/test';
import { expect } from '@playwright/test';

type Role = 'viewer' | 'estimator' | 'admin';

/** The account area in the app header: display name, role badges, language, logout. */
export class UserMenu {
	constructor(private readonly page: Page) {}

	/** The signed-in user's display name is fixture data, not UI chrome. */
	async expectDisplayName(name: string): Promise<void> {
		await expect(this.page.locator('header')).toContainText(name);
	}

	async expectRoleBadge(role: Role): Promise<void> {
		await expect(this.page.getByTestId(`user-menu.role.${role}`)).toBeVisible();
	}

	async expectLogoutVisible(): Promise<void> {
		await expect(this.page.getByTestId('logout-button')).toBeVisible();
	}

	async logout(): Promise<void> {
		await this.page.getByTestId('logout-button').click();
	}
}
