import type { Page } from '@playwright/test';
import { expect } from '@playwright/test';

/** The projects overview at `/projects`. */
export class ProjectsListPage {
	constructor(private readonly page: Page) {}

	async goto(): Promise<void> {
		await this.page.goto('/projects');
		await this.page.waitForLoadState('networkidle');
	}

	/** Opens the create-project dialog. The button lives on the route, not in ProjectList. */
	async openCreateProjectDialog(): Promise<void> {
		await this.page.getByTestId('projects.new').click();
	}

	async expectRowCount(count: number): Promise<void> {
		await expect(this.page.locator('[data-testid^="project-list.row."]')).toHaveCount(count);
	}
}
