import { test, expect, request as playwrightRequest } from '@playwright/test';

const API = 'http://localhost:8080';

test('unauthenticated /api/auth/me is honoured under default-user fallback (dev-admin)', async () => {
	const ctx = await playwrightRequest.newContext({ baseURL: API });
	const res = await ctx.get('/api/auth/me');
	expect(res.status()).toBe(200);
	const body = await res.json();
	expect(body.subjectId).toBe('dev-admin');
	expect(body.providerName).toBe('dev');
	await ctx.dispose();
});

test('Authorization: Dev dev-viewer routes to the viewer user on /api/auth/me', async () => {
	const ctx = await playwrightRequest.newContext({
		baseURL: API,
		extraHTTPHeaders: { Authorization: 'Dev dev-viewer' }
	});
	const res = await ctx.get('/api/auth/me');
	expect(res.status()).toBe(200);
	const body = await res.json();
	expect(body.subjectId).toBe('dev-viewer');
	expect(body.roles).toEqual(['VIEWER']);
	await ctx.dispose();
});

test('Authorization: Dev nope-not-a-user is rejected with 401', async () => {
	const ctx = await playwrightRequest.newContext({
		baseURL: API,
		extraHTTPHeaders: { Authorization: 'Dev nope-not-a-user' }
	});
	const res = await ctx.get('/api/auth/me');
	expect(res.status()).toBe(401);
	await ctx.dispose();
});

test('GET /api/admin/ping with dev-viewer returns 403', async () => {
	const ctx = await playwrightRequest.newContext({
		baseURL: API,
		extraHTTPHeaders: { Authorization: 'Dev dev-viewer' }
	});
	const res = await ctx.get('/api/admin/ping');
	expect(res.status()).toBe(403);
	await ctx.dispose();
});

test('GET /api/admin/ping with dev-admin returns 200 with the pong payload', async () => {
	const ctx = await playwrightRequest.newContext({
		baseURL: API,
		extraHTTPHeaders: { Authorization: 'Dev dev-admin' }
	});
	const res = await ctx.get('/api/admin/ping');
	expect(res.status()).toBe(200);
	const body = await res.json();
	expect(body).toEqual({ message: 'pong', user: 'dev-admin' });
	await ctx.dispose();
});
