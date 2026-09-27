import type { Locator, Page } from '@playwright/test';
import { expect } from '@playwright/test';

/** The session setup screen at `/sessions?projectId=…&estimationId=…`. */
export class SessionSetupPage {
	constructor(private readonly page: Page) {}

	get selectedCount(): Locator {
		return this.page.getByTestId('session-setup.selected-count');
	}

	get startButton(): Locator {
		return this.page.getByTestId('session-setup.start');
	}

	async selectAll(): Promise<void> {
		await this.page.getByTestId('session-setup.select-all').click();
	}

	async clearSelection(): Promise<void> {
		await this.page.getByTestId('session-setup.clear-selection').click();
	}

	async start(): Promise<void> {
		await this.startButton.click();
	}

	/** The "nothing selected" hint that explains why Start is disabled. */
	async expectNoneSelectedHint(): Promise<void> {
		await expect(this.page.getByTestId('session-setup.error')).toBeVisible();
	}

	get itemPicker(): Locator {
		return this.page.getByTestId('session-setup.item-picker');
	}

	/**
	 * The count renders as "20 von 40 ausgewählt"; the numbers are mirrored onto
	 * data attributes so the assertion pins the VALUES without pinning the
	 * sentence to a language.
	 */
	async expectSelectedCount(selected: number, total: number): Promise<void> {
		await expect(this.selectedCount).toHaveAttribute('data-selected', String(selected));
		await expect(this.selectedCount).toHaveAttribute('data-total', String(total));
	}
}
