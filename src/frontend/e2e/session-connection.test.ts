import { test, expect, type APIRequestContext } from '@playwright/test';

const API = 'http://localhost:8090';
const LOCAL = 'http://localhost:5173';
const H = { Authorization: 'Dev dev-admin' };

// The session room's connection indicator must tell the TRUTH (task-147).
//
// Before this, `connected` was set to true inside SessionStore.apply() — i.e.
// when the first payload arrived — and never reset, so the green dot stayed
// green over a dead socket and told the user stale data was live. These specs
// pin both directions: it goes green when the socket is up, and it goes back to
// grey when the socket dies.

function seed(subject: string) {
	return {
		cookies: [],
		origins: [{ origin: LOCAL, localStorage: [{ name: 'devAuthSubject', value: subject }] }]
	};
}

async function setUp(req: APIRequestContext) {
	const proj = await req.post(`${API}/api/projects`, {
		headers: H,
		data: { name: 'Connection Project', description: 'e2e', client: 'Tester' }
	});
	const projectId = (await proj.json()).id;
	const est = await req.post(`${API}/api/projects/${projectId}/estimations`, {
		headers: H,
		data: { offer: 'CONN-001', description: 'connection indicator' }
	});
	const estimationId = (await est.json()).id;
	await req.post(`${API}/api/estimations/${estimationId}/versions`, { headers: H });
	await req.put(`${API}/api/estimations/${estimationId}/versions/draft`, {
		headers: H,
		data: {
			roots: [
				{ type: 'FIXED', description: 'Feature A', minEffort: 1, expectedEffort: 2, maxEffort: 3 }
			]
		}
	});
	const draft = await req.get(`${API}/api/estimations/${estimationId}/versions/draft`, {
		headers: H
	});
	const leafIds = ((await draft.json()).roots as Array<{ logicalId: string }>).map(
		(r) => r.logicalId
	);
	const session = await req.post(`${API}/api/sessions`, {
		headers: H,
		data: { estimationId, title: 'Connection Session', itemLogicalIds: leafIds }
	});
	return (await session.json()).id as string;
}

test('the connection indicator reports a live socket', async ({ browser }) => {
	const ctx = await browser.newContext({
		baseURL: LOCAL,
		locale: 'de-DE',
		storageState: seed('dev-admin')
	});
	const page = await ctx.newPage();
	try {
		const sessionId = await setUp(page.request);
		await page.goto(`/sessions/${sessionId}`);
		await expect(page.getByTestId('session-connection')).toHaveAttribute('data-connected', 'true');
	} finally {
		await ctx.close();
	}
});

test('the indicator flips to disconnected when the socket dies', async ({ browser }) => {
	const ctx = await browser.newContext({
		baseURL: LOCAL,
		locale: 'de-DE',
		storageState: seed('dev-admin')
	});
	const page = await ctx.newPage();
	try {
		// Record every WebSocket the page opens so the test can kill the live one.
		// Chromium's offline emulation does NOT tear down an already-established
		// socket, so there is no way to do this from the outside.
		await page.addInitScript(() => {
			const w = window as unknown as { __sockets: WebSocket[] };
			w.__sockets = [];
			const Original = window.WebSocket;
			class Recorded extends Original {
				constructor(url: string | URL, protocols?: string | string[]) {
					super(url, protocols);
					w.__sockets.push(this);
				}
			}
			window.WebSocket = Recorded as unknown as typeof WebSocket;
		});

		const sessionId = await setUp(page.request);
		await page.goto(`/sessions/${sessionId}`);
		await expect(page.getByTestId('session-connection')).toHaveAttribute('data-connected', 'true');

		// Block the ws-ticket mint so reconnect attempts cannot succeed, then kill
		// the live socket. The indicator must go grey and STAY grey — the point of
		// the task is that a dead socket is visible rather than silently stale.
		await page.route('**/api/sessions/*/ws-ticket', (route) => route.abort());
		const killed = await page.evaluate(() => {
			const w = window as unknown as { __sockets: WebSocket[] };
			w.__sockets.forEach((s) => s.close());
			return w.__sockets.length;
		});
		expect(killed, 'expected the page to have opened a session socket').toBeGreaterThan(0);

		await expect(page.getByTestId('session-connection')).toHaveAttribute(
			'data-connected',
			'false'
		);
	} finally {
		await ctx.close();
	}
});

// A PERMANENT ticket failure must be terminal (task-169).
//
// The client used to call scheduleReconnect() for every ticket failure alike, so
// a tab left open on a session that no longer exists re-requested a ticket every
// 15s for as long as the tab lived. Note what does NOT reproduce this: cancelling
// a session leaves the row and its participants in place, and `wsTicket` only
// asserts existence + participation, so a cancelled session still issues tickets.
// A genuinely absent id is the reachable case — and it is what every ./scripts/dev.sh
// restart produces for previously-created sessions, because Dev Services starts a
// throwaway PostgreSQL.
test('a session that is gone stops the ticket retry loop', async ({ browser }) => {
	// Deliberately waits past MAX_BACKOFF_MS (15s) so this cannot pass merely
	// because the next attempt had not come due yet.
	test.setTimeout(45_000);
	const ctx = await browser.newContext({
		baseURL: LOCAL,
		locale: 'de-DE',
		storageState: seed('dev-admin')
	});
	const page = await ctx.newPage();
	try {
		const tickets: string[] = [];
		page.on('request', (r) => {
			if (r.url().includes('/ws-ticket')) tickets.push(r.url());
		});

		// A well-formed UUID that no session has: the path param parses, so the
		// backend answers 404 rather than 400.
		await page.goto(`/sessions/${crypto.randomUUID()}`);

		// The room says so, in the user's language, rather than leaking a raw
		// ticket error.
		await expect(page.getByRole('alert')).toContainText('nicht mehr verfügbar');
		// The connection indicator is deliberately NOT asserted here: it renders
		// from the session DTO, and for an id that never resolved there is no DTO
		// and no indicator in the DOM. The next test pins the indicator on a real
		// session whose ticket is refused.

		const afterGiveUp = tickets.length;
		expect(afterGiveUp, 'expected the client to have tried once').toBeGreaterThan(0);
		await page.waitForTimeout(17_000);
		expect(tickets.length, 'the client must not schedule another attempt').toBe(afterGiveUp);
	} finally {
		await ctx.close();
	}
});

// The other half of task-169, and the regression guard for task-147: a TRANSIENT
// failure must still retry forever. Unbounded retry is what heals a room after a
// laptop sleep, a NAT timeout or a proxy idle-kill, so the fix classifies by
// permanence and never by an attempt cap.
test('a transient ticket failure keeps retrying', async ({ browser }) => {
	const ctx = await browser.newContext({
		baseURL: LOCAL,
		locale: 'de-DE',
		storageState: seed('dev-admin')
	});
	const page = await ctx.newPage();
	try {
		const sessionId = await setUp(page.request);
		let calls = 0;
		await page.route('**/api/sessions/*/ws-ticket', (route) => {
			calls += 1;
			return route.fulfill({
				status: 503,
				contentType: 'application/json',
				body: JSON.stringify({ message: 'temporarily unavailable' })
			});
		});

		await page.goto(`/sessions/${sessionId}`);
		// Backoff is 1s, 2s, 4s, … so several attempts are due within this window.
		await page.waitForTimeout(6_000);
		expect(calls, 'a 503 is transient and must keep retrying').toBeGreaterThan(2);
		await expect(page.getByTestId('session-connection')).toHaveAttribute(
			'data-connected',
			'false'
		);
	} finally {
		await ctx.close();
	}
});

// The indicator half of the terminal case, on a room that DID load: the session
// exists (so the DTO renders the chrome) but its ticket is refused permanently.
// Together with the test above — real server 404, no further attempts — this
// covers stop, message and indicator, each asserted where it is observable.
test('a terminal ticket failure marks the room disconnected', async ({ browser }) => {
	const ctx = await browser.newContext({
		baseURL: LOCAL,
		locale: 'de-DE',
		storageState: seed('dev-admin')
	});
	const page = await ctx.newPage();
	try {
		const sessionId = await setUp(page.request);
		let calls = 0;
		await page.route('**/api/sessions/*/ws-ticket', (route) => {
			calls += 1;
			return route.fulfill({
				status: 404,
				contentType: 'application/json',
				body: JSON.stringify({ message: 'Session not found' })
			});
		});

		await page.goto(`/sessions/${sessionId}`);
		await expect(page.getByTestId('session-connection')).toHaveAttribute(
			'data-connected',
			'false'
		);
		await expect(page.getByRole('alert')).toContainText('nicht mehr verfügbar');

		// Backoff would have fired at 1s, 2s and 4s had the failure been treated
		// as transient.
		await page.waitForTimeout(6_000);
		expect(calls, 'a 404 is permanent and must not be retried').toBe(1);
	} finally {
		await ctx.close();
	}
});
