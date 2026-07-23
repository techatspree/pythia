import { apiFetch } from '$lib/api/fetch';
import { assertOk } from '$lib/api/errors';
import { log } from '$lib/log';
import type { components } from '$lib/api/schema';
import type { AuthAccount } from './AuthProvider';

type CurrentUserDto = components['schemas']['CurrentUserDto'];

const KNOWN_ROLES = ['VIEWER', 'ESTIMATOR', 'ADMIN'] as const;
type KnownRole = (typeof KNOWN_ROLES)[number];

/**
 * Resolve the current user's account from the backend — the authoritative
 * source of roles (the backend derives them from the access token). Returns
 * `null` on `401` (unauthenticated → treat as "not signed in", not an error);
 * any other non-ok response throws via `assertOk` for the caller to surface.
 */
export async function fetchCurrentUserAccount(): Promise<AuthAccount | null> {
	const res = await apiFetch('/api/auth/me');
	if (res.status === 401) return null;
	await assertOk(res, 'Failed to load your account');
	const me = (await res.json()) as CurrentUserDto;
	const roles = (me.roles ?? [])
		.map((r) => String(r).toUpperCase())
		.filter((r): r is KnownRole => (KNOWN_ROLES as readonly string[]).includes(r));
	log.debug('fetchCurrentUserAccount: resolved account for', me.subjectId, 'roles', roles);
	return {
		subjectId: me.subjectId,
		email: me.email ?? undefined,
		displayName: me.displayName ?? undefined,
		roles,
		providerName: me.providerName as AuthAccount['providerName'],
		language: me.language ?? undefined
	};
}
