import { getAuthProvider } from '$lib/auth';
import { log } from '$lib/log';

// Single entry point for backend calls: attaches the active auth provider's
// Authorization header so requests are authenticated (dev: `Dev <subject>`,
// entra: `Bearer <token>`). Every `/api/...` call must go through this rather
// than bare `fetch`, otherwise the backend sees an anonymous request.
export async function apiFetch(input: string, init: RequestInit = {}): Promise<Response> {
	const headers = new Headers(init.headers);
	let auth: string | null;
	try {
		auth = await getAuthProvider().getAuthorizationHeader();
	} catch (e) {
		// Token acquisition failed (e.g. MSAL cache stale after an Entra app-
		// registration change, consent required, or the OIDC endpoint is
		// unreachable). Do NOT fall through and send an anonymous request — that
		// just 401s and surfaces a misleading "session expired". Throw the real
		// reason so the caller's try/catch → ErrorBanner shows it; we still log
		// per the error-surfacing rule.
		const reason = e instanceof Error ? e.message : String(e);
		log.error('Authorization failed; request not sent:', e);
		throw new Error(`Authentication failed: ${reason}`, { cause: e });
	}
	// A null header is "no credentials available" (not signed in), NOT a failure:
	// the request goes out anonymous and the backend 401 drives the sign-in message.
	if (auth) headers.set('Authorization', auth);
	return fetch(input, { ...init, headers });
}
