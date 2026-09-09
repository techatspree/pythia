// The single seam that turns a failed HTTP Response into a user-facing message.
// Complements the request seam `$lib/api/fetch.ts` (apiFetch). Call sites do the
// logging + ErrorBanner surfacing; this helper only produces the message.

export interface ApiErrorBody {
	message?: string;
	status?: number;
}

/**
 * The error `assertOk` throws, carrying the HTTP status alongside the message
 * (task-169).
 *
 * Additive on purpose: it `extends Error` and its `message` is byte-identical to
 * what was thrown before, so every existing call site — all of which catch
 * `Error` or read `e.message` — keeps working untouched. Only a caller that
 * needs to distinguish a PERMANENT failure from a transient one narrows with
 * `e instanceof ApiError`; the session socket does exactly that to stop
 * retrying a session that is gone. Do not make callers string-match the message
 * to recover the status: the text is user-facing and changes.
 */
export class ApiError extends Error {
	constructor(
		message: string,
		readonly status: number
	) {
		super(message);
		this.name = 'ApiError';
	}
}

/**
 * A meaningful message for a failed status. 401/403 return fixed authorization
 * strings; 404 and everything else return the caller's contextual `fallback`
 * when given, otherwise a generic message.
 */
export function messageForStatus(status: number, fallback?: string): string {
	if (status === 401) {
		return 'Your session has expired or you are not signed in. Please sign in again.';
	}
	if (status === 403) {
		return 'You are not authorized to perform this action. Ask an administrator if you need access.';
	}
	if (fallback) return fallback;
	return `The request failed (HTTP ${status}).`;
}

/**
 * Throws a meaningful `ApiError` when `res` is not ok (returns immediately when
 * it is). The status rides along on the error; the message is unchanged. Prefers a non-empty `message` from the JSON error body (e.g. the
 * backend's authorization mappers), otherwise falls back to
 * `messageForStatus(res.status, fallback)`. Uses `res.clone()` so a caller that
 * still wants the body is unaffected.
 */
export async function assertOk(res: Response, fallback?: string): Promise<void> {
	if (res.ok) return;
	let bodyMessage: string | null = null;
	try {
		const body = (await res.clone().json()) as ApiErrorBody;
		if (typeof body?.message === 'string' && body.message.trim() !== '') {
			bodyMessage = body.message;
		}
	} catch {
		// Non-JSON error body — fall through to the status-based message.
	}
	throw new ApiError(bodyMessage ?? messageForStatus(res.status, fallback), res.status);
}
