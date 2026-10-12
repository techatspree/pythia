import type { Locator, Page } from '@playwright/test';
import { expect } from '@playwright/test';

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

	/** The owner line (task-200): "Owner: <name>" or the no-owner text. */
	ownerText(): Locator {
		return this.page.getByTestId('project-detail.owner');
	}

	/** The change-owner button exists only when the backend says `canChangeOwner`. */
	async expectChangeOwnerVisible(visible: boolean): Promise<void> {
		const button = this.page.getByTestId('project-detail.change-owner');
		// The owner line renders with the project, so an absent button is only
		// meaningful once it is there.
		await expect(this.ownerText()).toBeVisible();
		if (visible) await expect(button).toBeVisible();
		else await expect(button).toHaveCount(0);
	}

	async openChangeOwner(): Promise<void> {
		await this.page.getByTestId('project-detail.change-owner').click();
	}
}
