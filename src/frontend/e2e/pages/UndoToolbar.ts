import type { Locator, Page } from '@playwright/test';
import { expect } from '@playwright/test';

/** Undo / redo / show-history controls above the version editor. */
export class UndoToolbar {
	constructor(private readonly page: Page) {}

	/** Exposed so a spec can assert enablement or wait on the POST it triggers. */
	get undoButton(): Locator {
		return this.page.getByTestId('undo-toolbar.undo');
	}

	get redoButton(): Locator {
		return this.page.getByTestId('undo-toolbar.redo');
	}

	get historyButton(): Locator {
		return this.page.getByTestId('undo-toolbar.show-history');
	}

	async undo(): Promise<void> {
		await this.undoButton.click();
	}

	async redo(): Promise<void> {
		await this.redoButton.click();
	}

	async toggleHistory(): Promise<void> {
		await this.historyButton.click();
	}

	async expectHistoryPressed(pressed: boolean): Promise<void> {
		await expect(this.historyButton).toHaveAttribute('aria-pressed', String(pressed));
	}
}
