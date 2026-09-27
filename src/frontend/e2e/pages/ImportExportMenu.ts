import type { Locator, Page } from '@playwright/test';
import { expect } from '@playwright/test';

/**
 * The import triggers on the estimation detail page and the export menu in the
 * version editor. The export format is a KEY (`xlsx` / `csv`), never the menu
 * item's translated label — that indirection is what let a translated RegExp
 * hide from both the grep gate and the ESLint rule before task-188.
 */
export class ImportExportMenu {
	constructor(private readonly page: Page) {}

	get importMerlinButton(): Locator {
		return this.page.getByTestId('estimation-detail.import-merlin');
	}

	get importXlsxButton(): Locator {
		return this.page.getByTestId('estimation-detail.import-xlsx');
	}

	async expectImportMerlinVisible(): Promise<void> {
		await expect(this.importMerlinButton).toBeVisible();
	}

	async expectImportXlsxVisible(): Promise<void> {
		await expect(this.importXlsxButton).toBeVisible();
	}

	/** Opens the export dropdown and clicks the given format. */
	async export(format: 'xlsx' | 'csv'): Promise<void> {
		await this.page.getByTestId('export-menu.open').click();
		await this.page.getByTestId(`export-menu.${format}`).click();
	}
}
