import { expect, test } from '@playwright/test';
import { CreateEstimationDialog, ProjectDetailPage } from './pages';

// Creates a fresh project through the API, opens its detail page, and drives
// the create-estimation dialog end-to-end. Uses the shared dev-admin
// storageState from playwright.config.ts (dev-admin has ESTIMATOR + ADMIN).

test('create new estimation via the project detail dialog', async ({ page, request }) => {
	// Seed a unique project so the spec is idempotent even across re-runs.
	const projectName = `E2E Create ${Date.now()}`;
	const created = await request.post('/api/projects', {
		headers: { 'Content-Type': 'application/json', Authorization: 'Dev dev-admin' },
		data: { name: projectName }
	});
	expect(created.status()).toBe(201);
	const project = await created.json();

	const uniqueOffer = `E2E-OFFER-${Date.now()}`;

	const detailPage = new ProjectDetailPage(page);
	const dialog = new CreateEstimationDialog(page);

	await detailPage.goto(project.id);

	await detailPage.openCreateEstimationDialog();
	await dialog.expectVisible();

	await dialog.fillOffer(uniqueOffer);
	await dialog.fillDescription('E2E-generated Kalkulation');

	await dialog.submit();

	// Success navigates to /estimations/<uuid>.
	await expect(page).toHaveURL(/\/estimations\/[a-f0-9-]+/);
	expect(page.url()).toContain('/estimations/');

	// Returning to the project detail shows the new row in the table. The offer
	// is test-generated DATA, not UI chrome, so matching its text is correct.
	await detailPage.goto(project.id);
	await expect(page.getByText(uniqueOffer, { exact: true })).toBeVisible();
});

test('submitting with an empty Angebot is prevented', async ({ page, request }) => {
	const created = await request.post('/api/projects', {
		headers: { 'Content-Type': 'application/json', Authorization: 'Dev dev-admin' },
		data: { name: `E2E Guard ${Date.now()}` }
	});
	expect(created.status()).toBe(201);
	const project = await created.json();

	const detailPage = new ProjectDetailPage(page);
	const dialog = new CreateEstimationDialog(page);

	await detailPage.goto(project.id);

	await detailPage.openCreateEstimationDialog();
	await dialog.expectVisible();

	// Submit without filling the offer — the native `required` constraint on
	// the input keeps the dialog open (URL does not change).
	await dialog.submit();

	await dialog.expectVisible();
	await expect(page).toHaveURL(new RegExp(`/projects/${project.id}$`));
});

test('create estimation with a chosen non-default method (bucket + sampled)', async ({
	page,
	request
}) => {
	const created = await request.post('/api/projects', {
		headers: { 'Content-Type': 'application/json', Authorization: 'Dev dev-admin' },
		data: { name: `E2E Method ${Date.now()}` }
	});
	expect(created.status()).toBe(201);
	const project = await created.json();

	const uniqueOffer = `E2E-METHOD-${Date.now()}`;

	const detailPage = new ProjectDetailPage(page);
	const dialog = new CreateEstimationDialog(page);

	await detailPage.goto(project.id);

	await detailPage.openCreateEstimationDialog();
	await dialog.expectVisible();

	await dialog.fillOffer(uniqueOffer);
	// Pick the non-default method (the option value is the EstimationMethod
	// enum name, so this is locale-independent by construction).
	await dialog.selectMethod('BUCKET_SAMPLED_PERT');
	await dialog.submit();

	await expect(page).toHaveURL(/\/estimations\/[a-f0-9-]+/);
	const estimationId = page.url().split('/estimations/')[1];

	// Verify the created estimation actually carries the chosen method.
	const detail = await request.get(`/api/estimations/${estimationId}`, {
		headers: { Authorization: 'Dev dev-admin' }
	});
	expect(detail.status()).toBe(200);
	expect((await detail.json()).method).toBe('BUCKET_SAMPLED_PERT');
});
