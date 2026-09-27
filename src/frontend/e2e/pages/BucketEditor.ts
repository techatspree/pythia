import type { Locator, Page } from '@playwright/test';
import { expect } from '@playwright/test';

/**
 * The bucket + sampled editor: the bucket panel (add / rename / delete) and the
 * two views' row actions. Row actions are per-row, so the caller supplies the
 * row locator it already has.
 */
export class BucketEditor {
	constructor(private readonly page: Page) {}

	get bucketNames(): Locator {
		return this.page.getByTestId('bucket.name-input');
	}

	async addBucket(): Promise<void> {
		await this.page.getByTestId('bucket.add-bucket').click();
	}

	async deleteBucket(index: number): Promise<void> {
		await this.page.getByTestId('bucket.delete').nth(index).click();
	}

	/** Root-level "add group" / "add item", present in the footer and empty state. */
	get addGroupRowButton(): Locator {
		return this.page.getByTestId('bucket.add-group-row');
	}

	get addItemRowButton(): Locator {
		return this.page.getByTestId('bucket.add-item-row');
	}

	/** The per-row actions, which render only on a GROUP row. */
	addChildGroupIn(row: Locator): Locator {
		return row.getByTestId('bucket.add-group').first();
	}

	addChildItemIn(row: Locator): Locator {
		return row.getByTestId('bucket.add-item').first();
	}

	async expectSaved(): Promise<void> {
		await expect(this.page.getByTestId('editor.saved')).toBeVisible();
	}

	async submit(): Promise<void> {
		await this.page.getByTestId('editor.submit').click();
	}

	async expectSubmittedReadOnly(): Promise<void> {
		await expect(this.page.getByTestId('editor.submitted-banner')).toBeVisible();
	}
}
