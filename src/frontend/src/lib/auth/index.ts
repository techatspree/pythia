import type { AuthProvider } from './AuthProvider';
import { getRuntimeConfig } from '$lib/config/runtimeConfig';
import { getDevAuthProvider } from './DevAuthProvider';
import { getEntraAuthProvider } from './EntraAuthProvider';

export type { AuthAccount, AuthProvider } from './AuthProvider';

// Reads the RUNTIME config (task-162), not a build-time VITE_ var. There is
// deliberately NO `?? 'dev'` fallback: defaulting a misconfigured stage to the
// forgeable dev auth module is an auth downgrade. runtimeConfig.ts has already
// rejected an unknown provider (and the "UNCONFIGURED" sentinel) by the time
// this runs, and the root +layout.ts load guarantees it has run.
export function getAuthProvider(): AuthProvider {
	const selected = getRuntimeConfig().authProvider;
	switch (selected) {
		case 'dev':
			return getDevAuthProvider();
		case 'entra':
			return getEntraAuthProvider();
		case 'keycloak':
			throw new Error("Provider 'keycloak' not yet wired up — see task-060");
		default:
			throw new Error(`Unknown authProvider value in /config.json: ${selected}`);
	}
}
