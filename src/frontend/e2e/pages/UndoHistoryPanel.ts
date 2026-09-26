import type { Locator, Page } from '@playwright/test';
import { expect } from '@playwright/test';

/** The mutation-log panel rendered directly under the undo toolbar (task-109). */
export class UndoHistoryPanel {
	constructor(private readonly page: Page) {}

	get title(): Locator {
		return this.page.getByTestId('undo-history.title');
	}

	async expectTitleInViewport(): Promise<void> {
		await expect(this.title).toBeInViewport();
	}
}
