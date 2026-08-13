import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
	testDir: './e2e',
	timeout: 30_000,
	reporter: 'list',
	// Retry only on CI, and only there. Locally a flake should be visible, not
	// smoothed over. On CI the retry count is also diagnostic: a test that
	// passes on attempt 2 is timing-sensitive, while one that fails all three
	// times is a real defect on that environment — a distinction a single
	// attempt cannot make.
	retries: process.env.CI ? 2 : 0,
	use: {
		baseURL: 'http://localhost:5173',
		// `on-first-retry` produces NOTHING when retries are 0, which is how a
		// CI failure once shipped an artifact containing no trace at all. Keep
		// a trace for every failed test, so a failure that only reproduces on
		// CI can still be opened locally with `npx playwright show-trace`.
		trace: 'retain-on-failure',
		// Force a German browser locale so Accept-Language seeds freshly-provisioned
		// dev users to German (task-123 seeds language from Accept-Language on first
		// sighting). This keeps the app default (`de`) and the suite's German
		// assertions valid now that the dev provider re-reads the persisted language
		// from /api/auth/me on startup (task-127).
		locale: 'de-DE',
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
	projects: [
		{
			name: 'chromium',
			use: { ...devices['Desktop Chrome'] },
		},
	],
});
