import { get } from 'svelte/store';
import { _ } from 'svelte-i18n';
import { log } from '$lib/log';

// RUNTIME configuration for the SPA (task-162).
//
// Vite inlines `VITE_*` into the bundle at BUILD time, which made the frontend
// image the configuration: staging and production needed separately built
// images and an image could not be promoted between stages. A compiled SPA
// cannot read a pod environment variable — only the browser can read what the
// server sends it — so the coordinates arrive as a `/config.json` that nginx
// serves from a Kubernetes ConfigMap. One image, every stage.
//
// This module FAILS CLOSED. `getAuthProvider()` used to read
// `VITE_AUTH_PROVIDER ?? 'dev'`; keeping any such default here would silently
// downgrade a misconfigured stage to the forgeable dev auth module. Mirrors the
// backend, where `app.auth.provider` has no unprofiled default and
// AuthProviderGuard refuses to boot dev under a NORMAL launch (task-092).

export type AuthProviderName = 'dev' | 'entra' | 'keycloak';

export type EntraConfig = {
	tenantId: string;
	spaClientId: string;
	apiClientId: string;
	redirectUri: string;
};

export type RuntimeConfig = {
	authProvider: AuthProviderName;
	entra?: EntraConfig;
};

const CONFIG_URL = '/config.json';
const KNOWN_PROVIDERS: readonly string[] = ['dev', 'entra', 'keycloak'];

let cached: RuntimeConfig | null = null;

// Resolve the message BEFORE throwing. `+error.svelte` renders
// `page.error?.message` VERBATIM, so a raw English `throw new Error(...)` would
// put untranslated text on screen while the catalog keys sat unused. `get(_)`
// is the module-context form of `$_(...)`; it is safe here because
// `+layout.ts` awaits `waitLocale()` before `loadRuntimeConfig()`.
function fail(reason: string): never {
	log.error(`runtime config: ${reason}`);
	throw new Error(`${get(_)('config.load.failed')} ${get(_)('config.load.hint')}`);
}

function requireField(entra: Record<string, unknown>, name: keyof EntraConfig): string {
	const value = entra[name];
	if (typeof value !== 'string' || value.trim() === '') {
		fail(`entra.${name} is missing or empty in ${CONFIG_URL}`);
	}
	return value as string;
}

function parse(raw: unknown): RuntimeConfig {
	if (raw == null || typeof raw !== 'object') {
		fail(`${CONFIG_URL} is not a JSON object`);
	}
	const provider = (raw as Record<string, unknown>).authProvider;
	if (typeof provider !== 'string' || !KNOWN_PROVIDERS.includes(provider)) {
		// Covers the "UNCONFIGURED" sentinel the base ConfigMap carries, so a
		// stage overlay that forgets to override it fails loudly here.
		fail(`authProvider "${String(provider)}" is not one of ${KNOWN_PROVIDERS.join(' | ')}`);
	}

	const config: RuntimeConfig = { authProvider: provider as AuthProviderName };

	if (provider === 'entra') {
		const entra = (raw as Record<string, unknown>).entra;
		if (entra == null || typeof entra !== 'object') {
			fail(`authProvider is "entra" but no entra block is present in ${CONFIG_URL}`);
		}
		const e = entra as Record<string, unknown>;
		// All four are REQUIRED, redirectUri included. It used to fall back to
		// http://localhost:5173, which in a deployed stage is a broken login that
		// looks like a configuration that worked.
		config.entra = {
			tenantId: requireField(e, 'tenantId'),
			spaClientId: requireField(e, 'spaClientId'),
			apiClientId: requireField(e, 'apiClientId'),
			redirectUri: requireField(e, 'redirectUri')
		};
	}

	return config;
}

/**
 * Fetch and cache `/config.json`. Called once from the root `+layout.ts` load,
 * which resolves before any component initialises — that ordering is what makes
 * [getRuntimeConfig] safe to write as "throws if not loaded".
 */
export async function loadRuntimeConfig(): Promise<RuntimeConfig> {
	if (cached !== null) return cached;

	let response: Response;
	try {
		// Deliberate raw fetch, not apiFetch: /config.json is a static file served
		// by nginx, not an /api call, and it carries no Authorization header.
		// apiFetch would also DEADLOCK here — it calls
		// getAuthProvider().getAuthorizationHeader(), and getAuthProvider() reads
		// the very config this function is fetching.
		// eslint-disable-next-line no-restricted-syntax
		response = await fetch(CONFIG_URL, { cache: 'no-store' });
	} catch (err) {
		log.error('runtime config: fetch failed', err);
		fail(`could not fetch ${CONFIG_URL}`);
	}
	if (!response.ok) {
		fail(`${CONFIG_URL} returned ${response.status}`);
	}

	let raw: unknown;
	try {
		raw = await response.json();
	} catch (err) {
		// nginx serves index.html for unknown paths, so a missing config would
		// arrive as HTML with status 200 and land here. The `location =
		// /config.json { try_files $uri =404; }` block makes that a clean 404
		// instead; this stays as the second line of defence.
		log.error('runtime config: response was not JSON', err);
		fail(`${CONFIG_URL} did not contain JSON`);
	}

	cached = parse(raw);
	log.debug(`runtime config: authProvider=${cached.authProvider}`);
	return cached;
}

/** The loaded config. Throws when [loadRuntimeConfig] has not resolved yet. */
export function getRuntimeConfig(): RuntimeConfig {
	if (cached === null) {
		throw new Error('Runtime config accessed before loadRuntimeConfig() resolved');
	}
	return cached;
}
