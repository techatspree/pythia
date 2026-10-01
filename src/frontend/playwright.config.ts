import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
	testDir: './e2e',
	timeout: 30_000,
	reporter: 'list',
	// No retries, deliberately. Re-running a whole failed test is a blunt
	// instrument: it hides an infrastructure hiccup and a genuine race behind
	// the same green tick. Where a step is legitimately racy the retry belongs
	// INSIDE the test, bounded and visible — Playwright's web-first assertions
	// already poll, and `settleAndDrop` in e2e/bucket-views.test.ts chases a
	// moving drop zone for a fixed number of passes. Both known flakes were
	// fixed that way rather than retried away: a drag that released before the
	// layout settled, and a page-height assertion that measured other specs'
	// concurrently-created sessions.
	retries: 0,
	use: {
		baseURL: 'http://localhost:5173',
		// `on-first-retry` produces NOTHING when retries are 0, which is how a
		// CI failure once shipped an artifact containing no trace at all. Keep
		// a trace for every failed test, so a failure that only reproduces on
		// CI can still be opened locally with `npx playwright show-trace`.
		trace: 'retain-on-failure',
		// No browser `locale`, deliberately (task-190). The UI language is the
		// dev user's PERSISTED preference, which `e2e/global-setup.ts` pins to
		// `E2E_LANG` before every run. Accept-Language only seeds a user on FIRST
		// sighting (task-123), so a browser locale could never re-language an
		// existing one — it would only suggest the suite is German by construction.
		// Pre-seed the dev-auth provider with dev-admin so every test
		// starts as a logged-in admin. Tests that need to assert the
		// unauthenticated path (e.g. e2e/auth-gate.test.ts) opt out with
		// test.use({ storageState: { cookies: [], origins: [] } }).
		storageState: {
			cookies: [],
			origins: [
				{
					origin: 'http://localhost:5173',
					localStorage: [{ name: 'devAuthSubject', value: 'dev-admin' }]
				}
			]
		}
	},
	globalSetup: './e2e/global-setup.ts',
	projects: [
		{
			name: 'chromium',
			use: { ...devices['Desktop Chrome'] },
		},
	],
});
