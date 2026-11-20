import type { Page } from '@playwright/test';
import { expect } from '@playwright/test';
import { catalogText } from '../helpers';

type Part = 'frontend' | 'backend';

/** The JSON the dialog reads, as scripts/generate-oss-notices.mjs writes it. */
export interface NoticesFixture {
	parts: {
		part: Part;
		components: {
			name: string;
			licenses: string[];
			copyright: string[];
			authors: string[];
			homepage: string | null;
		}[];
	}[];
}

// The file may or may not exist on a dev stack (it comes from the last full
// build), so every test stubs it. A route takes precedence over the dev plugin.
export async function stubNotices(page: Page, json: NoticesFixture): Promise<void> {
	await page.route('**/third-party-notices.json', (route) =>
		route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(json) })
	);
}

export async function stubNoticesStatus(page: Page, status: number): Promise<void> {
	await page.route('**/third-party-notices.json', (route) => route.fulfill({ status, body: '' }));
}

/** The "Open-Source-Lizenzen" dialog (task-210); opened via AppFooter.openOssNotices(). */
export class OssNoticesDialog {
	constructor(private readonly page: Page) {}

	private get dialog() {
		return this.page.getByTestId('oss-notices-dialog');
	}

	private component(name: string) {
		return this.dialog.locator(`[data-testid="oss-notices-dialog.component"][data-name="${name}"]`);
	}

	async expectVisible(): Promise<void> {
		await expect(this.dialog).toBeVisible();
		await expect(this.page.getByTestId('oss-notices-dialog.intro')).toBeVisible();
	}

	async expectHidden(): Promise<void> {
		await expect(this.dialog).toHaveCount(0);
	}

	async filter(text: string): Promise<void> {
		await this.page.getByTestId('oss-notices-dialog.filter').fill(text);
	}

	/** Component names are test DATA (the stubbed fixture), not UI chrome. */
	async expectComponent(name: string): Promise<void> {
		await expect(this.component(name)).toBeVisible();
	}

	async expectNoComponent(name: string): Promise<void> {
		await expect(this.component(name)).toHaveCount(0);
	}

	/** A component row shows this line (a copyright line: fixture data). */
	async expectComponentLine(name: string, line: string): Promise<void> {
		await expect(this.component(name)).toContainText(line);
	}

	/** The "Autoren: {names}" line, with the fixture's names. */
	async expectAuthorsLine(name: string, authors: string): Promise<void> {
		await expect(this.component(name)).toContainText(catalogText('ossNotices.authors').replace('{names}', authors));
	}

	/** The "see the license text" line of a component with neither copyright nor authors. */
	async expectCopyrightUnknown(name: string): Promise<void> {
		await expect(this.component(name)).toContainText(catalogText('ossNotices.copyrightUnknown'));
	}

	/** The heading shows the filtered count (small fixture numbers: no grouping), and that many rows. */
	async expectPartCount(part: Part, n: number): Promise<void> {
		await expect(this.page.getByTestId(`oss-notices-dialog.part.${part}`)).toHaveText(
			catalogText(`ossNotices.part.${part}`).replace('{count}', String(n))
		);
		await expect(
			this.page.getByTestId(`oss-notices-dialog.section.${part}`).getByTestId('oss-notices-dialog.component')
		).toHaveCount(n);
	}

	async expectPartHidden(part: Part): Promise<void> {
		await expect(this.page.getByTestId(`oss-notices-dialog.part.${part}`)).toHaveCount(0);
	}

	async expectNoMatch(): Promise<void> {
		await expect(this.page.getByTestId('oss-notices-dialog.no-match')).toBeVisible();
		await this.expectPartHidden('frontend');
		await this.expectPartHidden('backend');
	}

	async expectUnavailable(): Promise<void> {
		await expect(this.page.getByTestId('oss-notices-dialog.unavailable')).toBeVisible();
		await expect(this.page.getByTestId('oss-notices-dialog.filter')).toHaveCount(0);
		await expect(this.page.getByTestId('oss-notices-dialog.full-text')).toHaveCount(0);
		await expect(this.dialog.getByTestId('error-banner')).toHaveCount(0);
	}

	async expectLoadFailed(): Promise<void> {
		await expect(this.dialog.getByTestId('error-banner')).toContainText(catalogText('ossNotices.loadFailed'));
		await expect(this.page.getByTestId('oss-notices-dialog.filter')).toHaveCount(0);
		await expect(this.page.getByTestId('oss-notices-dialog.full-text')).toHaveCount(0);
	}

	async expectFullTextLink(): Promise<void> {
		const link = this.page.getByTestId('oss-notices-dialog.full-text');
		await expect(link).toHaveAttribute('href', '/THIRD-PARTY-LICENSES.txt');
		await expect(link).toHaveAttribute('target', '_blank');
	}

	async close(): Promise<void> {
		await this.page.getByTestId('oss-notices-dialog.close').click();
	}

	async closeWithEscape(): Promise<void> {
		await this.page.keyboard.press('Escape');
	}
}
