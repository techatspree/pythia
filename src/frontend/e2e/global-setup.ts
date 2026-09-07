import { request as playwrightRequest } from '@playwright/test';

/**
 * Resets the e2e fixture data BEFORE a run (task-176).
 *
 * Setup, not teardown, for two reasons. Part of the accumulation that made this
 * necessary arrived through runs that were interrupted part-way, and a teardown
 * never executes in that case while a setup is idempotent against it. And a
 * failing run's data is worth keeping for debugging — a teardown would destroy
 * exactly the state someone wants to inspect.
 *
 * The suite creates roughly one project per test and removes none. One run's
 * worth is harmless; it is the SECOND run against the same stack that degrades,
 * as list pages slow into 30-second interaction timeouts and `.first()`
 * locators start resolving to the wrong row.
 */

const API = 'http://localhost:8090';

export default async function globalSetup(): Promise<void> {
	// Playwright's own request context, the same idiom the specs use — not raw
	// fetch, which is ESLint-banned here because `apiFetch` is what attaches the
	// Authorization header. `apiFetch` is unavailable in this node-side setup (it
	// is a `$lib` alias and reads a browser-held token), so this passes the dev
	// header explicitly instead.
	const ctx = await playwrightRequest.newContext({
		baseURL: API,
		extraHTTPHeaders: { Authorization: 'Dev dev-admin' },
	});

	try {
		const res = await ctx.delete('/api/dev/test-data').catch((e: unknown) => {
			// No stack at all: fail loudly rather than let every test time out
			// individually and bury the cause.
			throw new Error(
				`e2e reset: cannot reach the backend at ${API}. Is ./scripts/dev.sh running?`,
				{ cause: e },
			);
		});

		if (res.status() === 404) {
			// The endpoint is @IfBuildProfile("dev"), so a 404 means the backend is
			// not on the dev profile. That is a reason to warn and continue, not to
			// block every test.
			console.warn(
				'e2e reset: /api/dev/test-data is absent (non-dev profile?) — continuing without a reset',
			);
			return;
		}
		if (!res.ok()) {
			throw new Error(`e2e reset: ${res.status()} ${res.statusText()}`);
		}

		const body = (await res.json()) as {
			deletedProjects: number;
			deletedSessions: number;
			remainingProjects: number;
		};
		console.log(
			`e2e reset: deleted ${body.deletedProjects} project(s) and ${body.deletedSessions} session(s), ` +
				`${body.remainingProjects} remain`,
		);
	} finally {
		await ctx.dispose();
	}
}
