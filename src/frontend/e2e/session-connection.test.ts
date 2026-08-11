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
