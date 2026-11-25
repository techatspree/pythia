import { expect, test, type APIRequestContext } from '@playwright/test';
import { SystemSettingsPage } from './pages';

/**
 * The per-tenant switch of the read-only MCP interface (task-218): an admin
 * turns it on in the system settings, and /api/mcp answers; off, it is 404.
 *
 * The setting is GLOBAL — one value for the whole backend — so afterEach turns
 * it off again, even on failure. Tests within this file run one after another
 * (Playwright's `fullyParallel` is off). No other spec touches /api/mcp.
 */

const ADMIN = { 'Content-Type': 'application/json', Authorization: 'Dev dev-admin' } as const;

const INITIALIZE = {
	jsonrpc: '2.0',
	id: 1,
	method: 'initialize',
	params: {
		protocolVersion: '2025-06-18',
		capabilities: {},
		clientInfo: { name: 'e2e-mcp-settings', version: '1' }
	}
};

async function setMcp(request: APIRequestContext, enabled: boolean) {
	const res = await request.put('/api/system/mcp', { headers: ADMIN, data: { enabled } });
	expect(res.status()).toBe(204);
}

/** An MCP `initialize` as a viewer; returns the HTTP status. */
async function initializeAsViewer(request: APIRequestContext): Promise<number> {
	const res = await request.post('/api/mcp', {
		headers: {
			'Content-Type': 'application/json',
			Accept: 'application/json, text/event-stream',
			Authorization: 'Dev dev-viewer'
		},
		data: INITIALIZE
	});
	return res.status();
}

test.afterEach(async ({ request }) => {
	await setMcp(request, false);
});

test('the MCP interface is off by default and its endpoint is hidden', async ({ page, request }) => {
	await setMcp(request, false);
	const settings = new SystemSettingsPage(page);
	await settings.open();

	await expect(settings.mcpEnabledCheckbox).not.toBeChecked();
	await expect(settings.mcpEndpoint).toBeHidden();
	expect(await initializeAsViewer(request)).toBe(404);
});

test('an admin switches the MCP interface on and off', async ({ page, request }) => {
	const settings = new SystemSettingsPage(page);
	await settings.open();

	await settings.saveMcp(true);
	await settings.expectNotice();
	await expect(settings.mcpEndpointUrl).toHaveText(/\/api\/mcp$/);
	expect(await initializeAsViewer(request)).toBe(200);

	await page.reload();
	await settings.open();
	await expect(settings.mcpEnabledCheckbox).toBeChecked();
	await expect(settings.mcpEndpoint).toBeVisible();

	await settings.saveMcp(false);
	await settings.expectNotice();
	await expect(settings.mcpEndpoint).toBeHidden();
	expect(await initializeAsViewer(request)).toBe(404);
});
