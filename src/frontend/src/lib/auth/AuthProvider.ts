export type AuthAccount = {
	subjectId: string;
	email?: string;
	displayName?: string;
	roles: ('VIEWER' | 'ESTIMATOR' | 'ADMIN')[];
	providerName: 'dev' | 'entra' | 'keycloak';
};

export interface AuthProvider {
	readonly name: 'dev' | 'entra' | 'keycloak';
	init(): Promise<void>;
	login(): Promise<void>;
	logout(): Promise<void>;
	// Async, provider-specific resolution of the current account. Entra resolves
	// it from the backend (GET /api/auth/me — authoritative roles); dev builds it
	// client-side. `getAccount()` returns the already-resolved account synchronously.
	loadAccount(): Promise<AuthAccount | null>;
	getAccount(): AuthAccount | null;
	getAuthorizationHeader(): Promise<string | null>;
}
