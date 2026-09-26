import type { Page } from '@playwright/test';
import { expect } from '@playwright/test';

/** The create-project modal. */
export class CreateProjectDialog {
	constructor(private readonly page: Page) {}

	async expectVisible(): Promise<void> {
		await expect(this.page.getByTestId('create-project.dialog')).toBeVisible();
	}

	async fillName(name: string): Promise<void> {
		await this.page.getByTestId('create-project.name-input').fill(name);
	}

	async submit(): Promise<void> {
		await this.page.getByTestId('create-project.submit').click();
	}

	async cancel(): Promise<void> {
		await this.page.getByTestId('create-project.cancel').click();
	}
}
