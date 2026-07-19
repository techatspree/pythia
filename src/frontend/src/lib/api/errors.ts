// The single seam that turns a failed HTTP Response into a user-facing message.
// Complements the request seam `$lib/api/fetch.ts` (apiFetch). Call sites do the
// logging + ErrorBanner surfacing; this helper only produces the message.

export interface ApiErrorBody {
	message?: string;
	status?: number;
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
 * Throws a meaningful `Error` when `res` is not ok (returns immediately when it
 * is). Prefers a non-empty `message` from the JSON error body (e.g. the
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
	throw new Error(bodyMessage ?? messageForStatus(res.status, fallback));
}
