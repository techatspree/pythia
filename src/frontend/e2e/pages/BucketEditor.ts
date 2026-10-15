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

	// ── Leaf kind: bucketed item or accompanying work (task-187) ────────────
	// A leaf is addressed by its logicalId, which a spec seeds as test DATA;
	// TreeTable's `tt-row-<id>` is the only per-row handle it emits.

	leafRow(leafId: string): Locator {
		return this.page.getByTestId(`tt-row-${leafId}`);
	}

	leafTypeSelect(leafId: string): Locator {
		return this.leafRow(leafId).getByTestId('bucket.leaf-type');
	}

	leafBucketSelect(leafId: string): Locator {
		return this.leafRow(leafId).getByTestId('bucket.leaf-bucket');
	}

	leafPhaseSelect(leafId: string): Locator {
		return this.leafRow(leafId).getByTestId('bucket.leaf-phase');
	}

	async setLeafType(leafId: string, type: 'BUCKETED' | 'TIME_RELATIVE'): Promise<void> {
		await this.leafTypeSelect(leafId).selectOption(type);
	}

	async expectLeafType(leafId: string, type: 'BUCKETED' | 'TIME_RELATIVE'): Promise<void> {
		await expect(this.leafTypeSelect(leafId)).toHaveValue(type);
	}

	/** Accompanying work's optimistic input; only a TIME_RELATIVE row has it. */
	timeRelativeOptimistic(leafId: string): Locator {
		return this.leafRow(leafId).getByTestId('bucket.leaf-optimistic').locator('input');
	}

	async fillTimeRelativeOptimistic(leafId: string, value: string): Promise<void> {
		await this.timeRelativeOptimistic(leafId).fill(value);
	}

	async expectNeedsPhase(leafId: string): Promise<void> {
		await expect(this.leafRow(leafId).getByTestId('bucket.needs-phase')).toBeVisible();
	}

	async expectNoNeedsPhase(leafId: string): Promise<void> {
		await expect(this.leafRow(leafId).getByTestId('bucket.needs-phase')).toHaveCount(0);
	}

	/** The bucket view's "Begleitende Leistungen" row. */
	get accompanyingRow(): Locator {
		return this.page.getByTestId('bucket.row.accompanying');
	}

	async expectAccompanyingRow(): Promise<void> {
		await expect(this.accompanyingRow).toBeVisible();
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
