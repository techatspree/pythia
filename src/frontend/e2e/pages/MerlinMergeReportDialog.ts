import type { Locator, Page } from '@playwright/test';
import { expect } from '@playwright/test';

/** The sections the merge report can show, in their fixed order (task-223). */
export type MerlinMergeReportSection =
	| 'activities-added'
	| 'activities-removed'
	| 'activities-moved'
	| 'dependencies-removed'
	| 'dependencies-added'
	| 'dependencies-skipped'
	| 'content-removed'
	| 'efforts';

/** The report shown after a merge into Merlin has downloaded the merged file. */
export class MerlinMergeReportDialog {
	constructor(private readonly page: Page) {}

	get dialog(): Locator {
		return this.page.getByTestId('merlin-merge-report.dialog');
	}

	async expectVisible(): Promise<void> {
		await expect(this.dialog).toBeVisible();
	}

	section(key: MerlinMergeReportSection): Locator {
		return this.page.getByTestId(`merlin-merge-report.section.${key}`);
	}

	async expectEmpty(): Promise<void> {
		await expect(this.page.getByTestId('merlin-merge-report.empty')).toBeVisible();
	}

	async close(): Promise<void> {
		await this.page.getByTestId('merlin-merge-report.close').click();
		await expect(this.dialog).toBeHidden();
	}
}
