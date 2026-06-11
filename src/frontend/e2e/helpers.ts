import type { Page } from '@playwright/test';

export async function loginAsDev(page: Page, subjectId: string): Promise<void> {
	await page.addInitScript(
		([k, v]) => {
			localStorage.setItem(k, v);
		},
		['devAuthSubject', subjectId]
	);
}

export async function logoutDev(page: Page): Promise<void> {
	await page.addInitScript((k) => {
		localStorage.removeItem(k);
	}, 'devAuthSubject');
}
