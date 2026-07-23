import { expect, test } from '@playwright/test';

// Creates a fresh project through the API, opens its detail page, and drives
// the "Neue Kalkulation" dialog end-to-end. Uses the shared dev-admin
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

	await page.goto(`/projects/${project.id}`);
	await page.waitForLoadState('networkidle');

	await page.getByRole('button', { name: 'Neues Angebot' }).click();
	await expect(page.getByRole('dialog')).toBeVisible();

	await page.getByLabel('Angebot *').fill(uniqueOffer);
	await page.getByLabel('Beschreibung').fill('E2E-generated Kalkulation');

	await page.getByRole('button', { name: 'Speichern', exact: true }).click();

	// Success navigates to /estimations/<uuid>.
	await expect(page).toHaveURL(/\/estimations\/[a-f0-9-]+/);
	expect(page.url()).toContain('/estimations/');

	// Returning to the project detail shows the new row in the table.
	await page.goto(`/projects/${project.id}`);
	await page.waitForLoadState('networkidle');
	await expect(page.getByText(uniqueOffer, { exact: true })).toBeVisible();
});

test('submitting with an empty Angebot is prevented', async ({ page, request }) => {
	const created = await request.post('/api/projects', {
		headers: { 'Content-Type': 'application/json', Authorization: 'Dev dev-admin' },
		data: { name: `E2E Guard ${Date.now()}` }
	});
	expect(created.status()).toBe(201);
	const project = await created.json();

	await page.goto(`/projects/${project.id}`);
	await page.waitForLoadState('networkidle');

	await page.getByRole('button', { name: 'Neues Angebot' }).click();
	await expect(page.getByRole('dialog')).toBeVisible();

	// Click Speichern without filling Angebot — the native `required`
	// constraint on the input keeps the dialog open (URL does not change).
	await page.getByRole('button', { name: 'Speichern', exact: true }).click();

	await expect(page.getByRole('dialog')).toBeVisible();
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

	await page.goto(`/projects/${project.id}`);
	await page.waitForLoadState('networkidle');

	await page.getByRole('button', { name: 'Neues Angebot' }).click();
	await expect(page.getByRole('dialog')).toBeVisible();

	await page.getByLabel('Angebot *').fill(uniqueOffer);
	// Pick the non-default method from the "Methode" dropdown (option value is
	// the EstimationMethod enum name).
	await page.getByLabel('Methode').selectOption('BUCKET_SAMPLED_PERT');
	await page.getByRole('button', { name: 'Speichern', exact: true }).click();

	await expect(page).toHaveURL(/\/estimations\/[a-f0-9-]+/);
	const estimationId = page.url().split('/estimations/')[1];

	// Verify the created estimation actually carries the chosen method.
	const detail = await request.get(`/api/estimations/${estimationId}`, {
		headers: { Authorization: 'Dev dev-admin' }
	});
	expect(detail.status()).toBe(200);
	expect((await detail.json()).method).toBe('BUCKET_SAMPLED_PERT');
});
