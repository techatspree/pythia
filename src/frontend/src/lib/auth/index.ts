import type { AuthProvider } from './AuthProvider';
import { getDevAuthProvider } from './DevAuthProvider';
import { getEntraAuthProvider } from './EntraAuthProvider';

export type { AuthAccount, AuthProvider } from './AuthProvider';

export function getAuthProvider(): AuthProvider {
	const selected = (import.meta.env.VITE_AUTH_PROVIDER ?? 'dev') as
		| 'dev'
		| 'entra'
		| 'keycloak';
	switch (selected) {
		case 'dev':
			return getDevAuthProvider();
		case 'entra':
			return getEntraAuthProvider();
		case 'keycloak':
			throw new Error("Provider 'keycloak' not yet wired up — see task-060");
		default:
			throw new Error(`Unknown VITE_AUTH_PROVIDER value: ${selected}`);
	}
}
