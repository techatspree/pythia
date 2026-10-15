import type { Locator, Page } from '@playwright/test';

/** The collapsible "Phasen" panel of the version editor (task-215). */
export class PhasesPanel {
	constructor(private readonly page: Page) {}

	/** Opens the panel; a no-op when it is already open (its mode selects are showing). */
	async open(): Promise<void> {
		if ((await this.page.getByTestId('phase-duration-mode').count()) > 0) return;
		await this.page.getByTestId('panel.phases.toggle').click();
		await this.page.getByTestId('phase-duration-mode').first().waitFor();
	}

	/** The duration-mode select of the phase at this row index. */
	durationModeSelect(index: number): Locator {
		return this.page.getByTestId('phase-duration-mode').nth(index);
	}

	async setDurationMode(index: number, mode: 'EXPLICIT' | 'AUTOMATIC'): Promise<void> {
		await this.durationModeSelect(index).selectOption(mode);
	}
}
