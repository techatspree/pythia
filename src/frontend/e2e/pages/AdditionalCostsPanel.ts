import type { Locator, Page } from '@playwright/test';

/** The collapsible "Zusatzkosten" panel of the version editor (task-215). */
export class AdditionalCostsPanel {
	constructor(private readonly page: Page) {}

	/** Opens the panel; a no-op when it is already open (its phase selects are showing). */
	async open(): Promise<void> {
		if ((await this.page.getByTestId('additional-costs.phase').count()) > 0) return;
		await this.page.getByTestId('panel.additional-costs.toggle').click();
		await this.page.getByTestId('additional-costs.phase').first().waitFor();
	}

	/**
	 * The phase select of the cost at this index, in DOM order: the one-time
	 * table's rows first, then the recurring table's.
	 */
	phaseSelect(index: number): Locator {
		return this.page.getByTestId('additional-costs.phase').nth(index);
	}

	/** `''` selects the "no phase" option. */
	async setPhase(index: number, abbreviation: string): Promise<void> {
		await this.phaseSelect(index).selectOption(abbreviation);
	}
}
