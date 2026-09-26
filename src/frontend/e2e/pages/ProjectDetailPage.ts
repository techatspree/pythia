import type { Page } from '@playwright/test';

/** A single project's detail page at `/projects/<id>`. */
export class ProjectDetailPage {
	constructor(private readonly page: Page) {}

	async goto(projectId: string): Promise<void> {
		await this.page.goto(`/projects/${projectId}`);
		await this.page.waitForLoadState('networkidle');
	}

	/** Opens the "new offer" dialog (ESTIMATOR-gated). */
	async openCreateEstimationDialog(): Promise<void> {
		await this.page.getByTestId('project-detail.new-offer').click();
	}
}
