import type { Locator, Page } from '@playwright/test';
import { expect } from '@playwright/test';

/** The projects overview at `/projects`. */
export class ProjectsListPage {
	constructor(private readonly page: Page) {}

	/** Opens the list; "Only my projects" is on unless `scope=all` asks otherwise. */
	async goto(options: { scope?: 'all' } = {}): Promise<void> {
		await this.page.goto(options.scope ? `/projects?scope=${options.scope}` : '/projects');
		await this.page.waitForLoadState('networkidle');
	}

	/** Switches the "Only my projects" filter (task-200) and waits for the reload. */
	async setOnlyMine(on: boolean): Promise<void> {
		const checkbox = this.page.getByTestId('projects.only-mine');
		if ((await checkbox.isChecked()) === on) return;
		const owner = (url: URL) => url.searchParams.get('owner') === 'me';
		const reload = this.page.waitForResponse(
			(res) => new URL(res.url()).pathname === '/api/projects' && owner(new URL(res.url())) === on
		);
		await checkbox.setChecked(on);
		await reload;
		await this.expectLoaded();
	}

	async expectOnlyMine(on: boolean): Promise<void> {
		const checkbox = this.page.getByTestId('projects.only-mine');
		if (on) await expect(checkbox).toBeChecked();
		else await expect(checkbox).not.toBeChecked();
	}

	/** The ids of the rows currently listed, in display order. */
	async rowIds(): Promise<string[]> {
		await this.expectLoaded();
		const testids = await this.page
			.locator('[data-testid^="project-list.row."]')
			.evaluateAll((rows) => rows.map((r) => r.getAttribute('data-testid') ?? ''));
		return testids.map((t) => t.slice('project-list.row.'.length));
	}

	/** The owner cell of one row, as rendered (a display name, or "—"). */
	ownerCell(projectId: string): Locator {
		return this.page.getByTestId(`project-list.row.${projectId}`).getByTestId('project-list.owner');
	}

	async expectEmptyMine(): Promise<void> {
		await expect(this.page.getByTestId('projects.empty-mine')).toBeVisible();
	}

	/** The empty state's "show all projects" button: turns the filter off. */
	async showAllFromEmptyState(): Promise<void> {
		const reload = this.page.waitForResponse(
			(res) => new URL(res.url()).pathname === '/api/projects'
		);
		await this.page.getByTestId('projects.show-all').click();
		await reload;
		await this.expectLoaded();
	}

	/** Either rows or one of the empty states are on screen — the load finished. */
	private async expectLoaded(): Promise<void> {
		await expect(
			this.page
				.locator('[data-testid^="project-list.row."]')
				.or(this.page.getByTestId('projects.empty-mine'))
				.first()
		).toBeVisible();
	}

	/** Opens the create-project dialog. The button lives on the route, not in ProjectList. */
	async openCreateProjectDialog(): Promise<void> {
		await this.page.getByTestId('projects.new').click();
	}

	async expectRowCount(count: number): Promise<void> {
		await expect(this.page.locator('[data-testid^="project-list.row."]')).toHaveCount(count);
	}
}
