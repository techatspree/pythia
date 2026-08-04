import { getAuthProvider } from '$lib/auth';
import { log } from '$lib/log';
import { connection } from '$lib/stores/connection.svelte';

// A hung backend (connection accepted, response never arrives) never rejects on
// its own, so cap every request and treat the timeout as a connection loss.
const REQUEST_TIMEOUT_MS = 10000;

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

	// Impose our own timeout, forwarding a caller-supplied signal so a caller can
	// still cancel. A caller-initiated cancel is NOT a connection loss; our
	// timeout or a network rejection is.
	const controller = new AbortController();
	const timer = setTimeout(
		() => controller.abort(new DOMException('Request timed out', 'TimeoutError')),
		REQUEST_TIMEOUT_MS
	);
	const callerSignal = init.signal ?? undefined;
	callerSignal?.addEventListener('abort', () => controller.abort(callerSignal.reason), { once: true });

	let res: Response;
	try {
		res = await fetch(input, { ...init, headers, signal: controller.signal });
	} catch (e) {
		if (callerSignal?.aborted !== true) {
			connection.reportLost('The backend could not be reached.');
		}
		throw e;
	} finally {
		clearTimeout(timer);
	}

	// Gateway / service-unavailable statuses mean the backend is not reachable
	// behind the proxy — a connection loss, unlike an ordinary 4xx/5xx business
	// error (those keep the non-blocking ErrorBanner path via assertOk).
	if (res.status === 502 || res.status === 503 || res.status === 504) {
		connection.reportLost('The backend is temporarily unavailable.');
	}
	return res;
}
