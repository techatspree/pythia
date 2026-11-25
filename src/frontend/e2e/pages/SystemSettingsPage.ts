import type { Locator, Page } from '@playwright/test';
import { expect } from '@playwright/test';

/** The admin system-settings page (task-146); the working-time section is task-213, the MCP switch task-218. */
export class SystemSettingsPage {
	constructor(private readonly page: Page) {}

	/** Opens the page and waits for the form, which renders only once the settings loaded. */
	async open(): Promise<void> {
		await this.page.goto('/admin/system');
		await expect(this.hoursPerDayInput).toBeVisible();
	}

	get hoursPerDayInput(): Locator {
		return this.page.getByTestId('system.hours-per-day');
	}

	async saveHoursPerDay(value: string): Promise<void> {
		await this.hoursPerDayInput.fill(value);
		await this.page.getByTestId('system.save-hours-per-day').click();
	}

	/** The MCP interface switch (task-218). */
	get mcpEnabledCheckbox(): Locator {
		return this.page.getByTestId('system.mcp-enabled');
	}

	/** The endpoint line; shown only while the SAVED setting is on. */
	get mcpEndpoint(): Locator {
		return this.page.getByTestId('system.mcp-endpoint');
	}

	get mcpEndpointUrl(): Locator {
		return this.page.getByTestId('system.mcp-endpoint-url');
	}

	async saveMcp(enabled: boolean): Promise<void> {
		await this.mcpEnabledCheckbox.setChecked(enabled);
		await this.page.getByTestId('system.save-mcp').click();
	}

	async expectNotice(): Promise<void> {
		await expect(this.page.getByTestId('system-notice')).toBeVisible();
	}
}
