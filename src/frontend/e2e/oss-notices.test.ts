import { test, type Page } from '@playwright/test';
import { loginAsDev } from './helpers';
import { AppFooter, OssNoticesDialog, ProjectsListPage, stubNotices, stubNoticesStatus } from './pages';
import type { NoticesFixture } from './pages/OssNoticesDialog';

// The "Open-Source-Lizenzen" dialog (task-210). The notices file is stubbed in
// every test: on a dev stack it exists only after a full build.
const NOTICES: NoticesFixture = {
	parts: [
		{
			part: 'frontend',
			components: [
				{
					name: '@azure/msal-browser',
					licenses: ['MIT'],
					copyright: ['Copyright (c) Microsoft Corporation.'],
					authors: ['Microsoft'],
					homepage: 'https://github.com/AzureAD/microsoft-authentication-library-for-js#readme'
				},
				{
					name: 'org.jetbrains.kotlin:kotlin-stdlib-js',
					licenses: ['Apache-2.0'],
					copyright: [],
					authors: [],
					homepage: 'https://github.com/JetBrains/kotlin'
				},
				{
					name: 'svelte',
					licenses: ['MIT'],
					copyright: ['Copyright (c) 2016-2025 Svelte Contributors'],
					authors: [],
					homepage: 'https://svelte.dev'
				}
			]
		},
		{
			part: 'backend',
			components: [
				{
					name: 'org.hibernate.orm:hibernate-core',
					licenses: ['Apache-2.0'],
					copyright: [],
					authors: ['Hibernate.org'],
					homepage: 'https://hibernate.org'
				},
				{
					name: 'org.postgresql:postgresql',
					licenses: ['BSD-2-Clause'],
					copyright: ['Copyright (c) 1997, PostgreSQL Global Development Group'],
					authors: ['PostgreSQL Global Development Group'],
					homepage: null
				}
			]
		}
	]
};

async function openAsViewer(page: Page): Promise<OssNoticesDialog> {
	await loginAsDev(page, 'dev-viewer');
	await new ProjectsListPage(page).goto();
	const footer = new AppFooter(page);
	await footer.expectVisible();
	await footer.openOssNotices();
	const dialog = new OssNoticesDialog(page);
	await dialog.expectVisible();
	return dialog;
}

test('a viewer opens the licences from the footer and sees both parts', async ({ page }) => {
	await stubNotices(page, NOTICES);
	const dialog = await openAsViewer(page);

	await dialog.expectPartCount('frontend', 3);
	await dialog.expectPartCount('backend', 2);
	await dialog.expectComponentLine('svelte', 'Copyright (c) 2016-2025 Svelte Contributors');
	await dialog.expectAuthorsLine('org.hibernate.orm:hibernate-core', 'Hibernate.org');
	await dialog.expectCopyrightUnknown('org.jetbrains.kotlin:kotlin-stdlib-js');
	await dialog.expectFullTextLink();
});

test('the filter matches name, licence, copyright line and authors, case-insensitively', async ({ page }) => {
	await stubNotices(page, NOTICES);
	const dialog = await openAsViewer(page);

	await dialog.filter('postgres');
	await dialog.expectPartCount('backend', 1);
	await dialog.expectComponent('org.postgresql:postgresql');
	await dialog.expectPartHidden('frontend');

	await dialog.filter('bsd-2');
	await dialog.expectPartCount('backend', 1);
	await dialog.expectComponent('org.postgresql:postgresql');

	// Each case matches through ONE field only: 'contributors' is only in
	// svelte's copyright line, 'hibernate.org' only in an author.
	await dialog.filter('CONTRIBUTORS');
	await dialog.expectPartCount('frontend', 1);
	await dialog.expectComponent('svelte');
	await dialog.expectNoComponent('@azure/msal-browser');
	await dialog.expectPartHidden('backend');

	await dialog.filter('hibernate.org');
	await dialog.expectPartCount('backend', 1);
	await dialog.expectComponent('org.hibernate.orm:hibernate-core');
	await dialog.expectPartHidden('frontend');

	await dialog.filter('zzz');
	await dialog.expectNoMatch();
});

test('Escape and the close button both close the dialog', async ({ page }) => {
	await stubNotices(page, NOTICES);
	const dialog = await openAsViewer(page);
	await dialog.closeWithEscape();
	await dialog.expectHidden();

	await new AppFooter(page).openOssNotices();
	await dialog.expectVisible();
	await dialog.close();
	await dialog.expectHidden();
});

test('no notices file (404) is the "not available yet" state, not an error', async ({ page }) => {
	await stubNoticesStatus(page, 404);
	const dialog = await openAsViewer(page);
	await dialog.expectUnavailable();
});

test('a failed load (500) shows the error banner', async ({ page }) => {
	await stubNoticesStatus(page, 500);
	const dialog = await openAsViewer(page);
	await dialog.expectLoadFailed();
});
