// The single seam that turns a failed HTTP Response into a user-facing message.
// Complements the request seam `#lib/api/fetch.ts` (apiFetch). Call sites do the
// logging + ErrorBanner surfacing; this helper only produces the message.
//
// The messages come from the i18n catalogs (`apiError.*`, task-225) through
// `get(_)`, the module-context form of `$_(...)`. That is safe because the root
// `+layout.ts` awaits `waitLocale()` before any page can call `assertOk`.

import { get } from 'svelte/store';
import { _ } from 'svelte-i18n';

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
 * A meaningful, translated message for a failed status. 401/403 return the
 * catalog's authorization sentences (`apiError.unauthorized`/`apiError.forbidden`);
 * 404 and everything else return the caller's contextual `fallback` when given,
 * otherwise `apiError.requestFailed` with the bare status code.
 */
export function messageForStatus(status: number, fallback?: string): string {
	if (status === 401) return get(_)('apiError.unauthorized');
	if (status === 403) return get(_)('apiError.forbidden');
	if (fallback) return fallback;
	// String, not number: "HTTP 500", never a locale-formatted "HTTP 5.00"-style value.
	return get(_)('apiError.requestFailed', { values: { status: String(status) } });
}

/**
 * Throws a meaningful `ApiError` when `res` is not ok (returns immediately when
 * it is). The status rides along on the error.
 *
 * 401 and 403 ALWAYS show the translated `messageForStatus` text and ignore the
 * body (task-225): the backend's authorization mappers send fixed English
 * machine strings that carry nothing beyond the status, so preferring them put
 * English on a German screen. For every other status a non-empty `message` from
 * the JSON error body (e.g. the undo-conflict and Merlin-structure mappers) wins,
 * otherwise `messageForStatus(res.status, fallback)`. Uses `res.clone()` so a
 * caller that still wants the body is unaffected.
 */
export async function assertOk(res: Response, fallback?: string): Promise<void> {
	if (res.ok) return;
	if (res.status === 401 || res.status === 403) {
		throw new ApiError(messageForStatus(res.status, fallback), res.status);
	}
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
