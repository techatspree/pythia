import type { AuthProvider } from './AuthProvider';

export type { AuthAccount, AuthProvider } from './AuthProvider';

export function getAuthProvider(): AuthProvider {
	const selected = (import.meta.env.VITE_AUTH_PROVIDER ?? 'dev') as
		| 'dev'
		| 'entra'
		| 'keycloak';
	switch (selected) {
		case 'dev':
			throw new Error("Provider 'dev' not yet wired up — see task-006");
		case 'entra':
			throw new Error("Provider 'entra' not yet wired up — see task-007");
		case 'keycloak':
			throw new Error("Provider 'keycloak' not yet wired up — see task-060");
		default:
			throw new Error(`Unknown VITE_AUTH_PROVIDER value: ${selected}`);
	}
}
