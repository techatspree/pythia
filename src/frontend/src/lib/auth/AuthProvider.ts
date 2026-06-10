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
	getAccount(): AuthAccount | null;
	getAuthorizationHeader(): Promise<string | null>;
}
