import type { Page } from '@playwright/test';
import { expect } from '@playwright/test';

/** The create-estimation ("new offer") modal on the project detail page. */
export class CreateEstimationDialog {
	constructor(private readonly page: Page) {}

	async expectVisible(): Promise<void> {
		await expect(this.page.getByTestId('create-estimation.dialog')).toBeVisible();
	}

	async fillOffer(offer: string): Promise<void> {
		await this.page.getByTestId('create-estimation.offer-input').fill(offer);
	}

	async fillDescription(description: string): Promise<void> {
		await this.page.getByTestId('create-estimation.description-input').fill(description);
	}

	/** `method` is the EstimationMethod enum name, which is the option value. */
	async selectMethod(method: string): Promise<void> {
		await this.page.getByTestId('create-estimation.method-select').selectOption(method);
	}

	async submit(): Promise<void> {
		await this.page.getByTestId('create-estimation.submit').click();
	}

	async cancel(): Promise<void> {
		await this.page.getByTestId('create-estimation.cancel').click();
	}
}
