import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
	testDir: './e2e',
	timeout: 30_000,
	reporter: 'list',
	use: {
		baseURL: 'http://localhost:5173',
		trace: 'on-first-retry',
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
