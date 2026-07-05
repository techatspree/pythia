import { getAuthProvider } from '$lib/auth';
import { log } from '$lib/log';

// Single entry point for backend calls: attaches the active auth provider's
// Authorization header so requests are authenticated (dev: `Dev <subject>`,
// entra: `Bearer <token>`). Every `/api/...` call must go through this rather
// than bare `fetch`, otherwise the backend sees an anonymous request.
export async function apiFetch(input: string, init: RequestInit = {}): Promise<Response> {
	const headers = new Headers(init.headers);
	try {
		const auth = await getAuthProvider().getAuthorizationHeader();
		if (auth) headers.set('Authorization', auth);
	} catch (e) {
		log.error('Auth provider unavailable:', e);
	}
	return fetch(input, { ...init, headers });
}
