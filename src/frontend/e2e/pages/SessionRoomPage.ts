import type { Locator, Page } from '@playwright/test';
import { expect } from '@playwright/test';

/**
 * A Wideband-Delphi session room. One instance wraps ONE participant's page —
 * the session specs open a separate browser context per participant (moderator
 * and estimator), so construct one of these per page rather than scoping a
 * single instance.
 */
export class SessionRoomPage {
	constructor(private readonly page: Page) {}

	get submitEstimateButton(): Locator {
		return this.page.getByTestId('session.submit-estimate');
	}

	async fillEstimate(min: string, expected: string, max: string): Promise<void> {
		await this.page.getByTestId('session.min-input').fill(min);
		await this.page.getByTestId('session.expected-input').fill(expected);
		await this.page.getByTestId('session.max-input').fill(max);
	}

	async submitEstimate(): Promise<void> {
		await this.submitEstimateButton.click();
	}

	async start(): Promise<void> {
		await this.page.getByTestId('session.start').click();
	}

	async advanceToPhaseTwo(): Promise<void> {
		await this.page.getByTestId('session.to-phase-two').click();
	}

	async submitRevision(): Promise<void> {
		await this.page.getByTestId('revise-submit').click();
	}

	async agree(): Promise<void> {
		await this.page.getByTestId('session.agree').click();
	}

	async finalizeItem(): Promise<void> {
		await this.page.getByTestId('session.finalize-item').click();
	}

	get itemPosition(): Locator {
		return this.page.getByTestId('session.item-position');
	}

	get agreementStatus(): Locator {
		return this.page.getByTestId('session.agreement-status');
	}

	/** The room's contextual link back to its estimation. */
	get backToEstimation(): Locator {
		return this.page.getByTestId('session.back-to-estimation');
	}

	get summaryTitle(): Locator {
		return this.page.getByTestId('session.summary-title');
	}

	async expectFinished(): Promise<void> {
		await expect(this.summaryTitle).toBeVisible();
	}

	/**
	 * Asserts WHICH item is current. The panel renders "Eintrag 2 von 2", so the
	 * numbers are mirrored onto data attributes — asserting the sentence would
	 * pin the spec to German, and asserting only that the element is visible
	 * would pass at every position.
	 */
	async expectItemPosition(position: number, total: number): Promise<void> {
		await expect(this.itemPosition).toHaveAttribute('data-position', String(position));
		await expect(this.itemPosition).toHaveAttribute('data-total', String(total));
	}

	/** True only once every estimator has agreed — the element renders either way. */
	async expectAllAgreed(): Promise<void> {
		await expect(this.agreementStatus).toHaveAttribute('data-all-agreed', 'true');
	}
}
