import { test, expect, type Page } from '@playwright/test';
import { E2E_LANG, loginAsDev } from './helpers';

// The language preference is persisted per-user on the backend. This spec runs
// as dev-estimator, NOT the globally pre-seeded dev-admin, so the switch it
// persists never reaches the specs that act as dev-admin. dev-estimator is NOT
// dedicated, though: session.test.ts and undo.test.ts act as it too, and a
// session test running in parallel while this spec holds it in a language other
// than E2E_LANG sees the wrong language. That race is a known limitation until a
// fourth dev user exists (files run in parallel against one shared backend).
const API = 'http://localhost:8090';
const AUTH = { Authorization: 'Dev dev-estimator' } as const;

async function setLanguage(page: Page, code: 'de' | 'en'): Promise<void> {
	// The PUT also provisions the dev user, so this doubles as a deterministic
	// reset independent of what a previous run left behind.
	const res = await page.request.put(`${API}/api/auth/me/language`, {
		headers: { ...AUTH, 'Content-Type': 'application/json' },
		data: { language: code }
	});
	expect(res.status(), `PUT /api/auth/me/language ${code}`).toBe(204);
}

test.describe('language switch + reload persistence', () => {
	// Drop the global dev-admin storageState; log in as dev-estimator instead.
	test.use({ storageState: { cookies: [], origins: [] } });

	test.beforeEach(async ({ page }) => {
		await loginAsDev(page, 'dev-estimator');
		// German explicitly, whatever E2E_LANG is: this spec's subject IS the DE→EN switch.
		await setLanguage(page, 'de');
	});

	test.afterEach(async ({ page }) => {
		// Restore the RUN's language, not a hardcoded one: dev-estimator is shared.
		await setLanguage(page, E2E_LANG);
	});

	test('German default → switch to English → survives a reload', async ({ page }) => {
		await page.goto('/projects');
		await page.waitForLoadState('networkidle');

		// Default is German (seeded 'de' + the reset above).
		await expect(page.getByRole('heading', { name: 'Projekte', exact: true })).toBeVisible();
		await expect(page.locator('[data-testid="logout-button"]')).toHaveText('Abmelden');

		// Switch to English through the real user-menu switcher.
		await page.locator('[data-testid="language-select"]').selectOption('en');

		// The locale change is reactive (PUT then setUserLanguage) — no reload yet.
		await expect(page.getByRole('heading', { name: 'Projects', exact: true })).toBeVisible();
		await expect(page.locator('[data-testid="logout-button"]')).toHaveText('Logout');

		// Reload: the language must stick — proves the dev provider re-reads the
		// persisted preference from GET /api/auth/me on startup.
		await page.reload();
		await page.waitForLoadState('networkidle');
		await expect(page.getByRole('heading', { name: 'Projects', exact: true })).toBeVisible();
		await expect(page.locator('[data-testid="logout-button"]')).toHaveText('Logout');
	});
});
