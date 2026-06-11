import {
	PublicClientApplication,
	InteractionRequiredAuthError,
	type AccountInfo,
	type Configuration
} from '@azure/msal-browser';
import type { AuthAccount, AuthProvider } from './AuthProvider';

function requireEnv(name: keyof ImportMetaEnv): string {
	const v = import.meta.env[name];
	if (v == null || v === '') {
		throw new Error(`EntraAuthProvider: missing required env var ${String(name)}`);
	}
	return v as string;
}

function toAuthAccount(account: AccountInfo): AuthAccount {
	const claims = (account.idTokenClaims ?? {}) as Record<string, unknown>;
	const subjectId =
		(claims.oid as string | undefined) ??
		(claims.sub as string | undefined) ??
		account.localAccountId ??
		account.homeAccountId;
	const email =
		(claims.email as string | undefined) ??
		(claims.preferred_username as string | undefined) ??
		account.username;
	const displayName = (claims.name as string | undefined) ?? account.name;
	const rolesClaim = claims.roles;
	const roles: ('VIEWER' | 'ESTIMATOR' | 'ADMIN')[] = Array.isArray(rolesClaim)
		? rolesClaim
				.map((r) => String(r).toUpperCase())
				.filter((r): r is 'VIEWER' | 'ESTIMATOR' | 'ADMIN' =>
					r === 'VIEWER' || r === 'ESTIMATOR' || r === 'ADMIN'
				)
		: [];
	return {
		subjectId,
		email,
		displayName,
		roles,
		providerName: 'entra'
	};
}

class EntraAuthProviderImpl implements AuthProvider {
	readonly name = 'entra' as const;

	private msal: PublicClientApplication | null = null;
	private initialized = false;
	private apiScope = '';

	async init(): Promise<void> {
		if (this.initialized) return;
		const tenantId = requireEnv('VITE_ENTRA_TENANT_ID');
		const spaClientId = requireEnv('VITE_ENTRA_SPA_CLIENT_ID');
		const apiClientId = requireEnv('VITE_ENTRA_API_CLIENT_ID');
		const redirectUri =
			import.meta.env.VITE_ENTRA_REDIRECT_URI ?? 'http://localhost:5173';
		this.apiScope = `api://${apiClientId}/access`;
		const config: Configuration = {
			auth: {
				clientId: spaClientId,
				authority: `https://login.microsoftonline.com/${tenantId}`,
				redirectUri
			},
			cache: { cacheLocation: 'localStorage' }
		};
		this.msal = new PublicClientApplication(config);
		await this.msal.initialize();
		await this.msal.handleRedirectPromise();
		this.initialized = true;
	}

	async login(): Promise<void> {
		await this.init();
		await this.msal!.loginRedirect({ scopes: [this.apiScope] });
	}

	async logout(): Promise<void> {
		await this.init();
		await this.msal!.logoutRedirect();
	}

	getAccount(): AuthAccount | null {
		if (!this.initialized || this.msal == null) return null;
		const accounts = this.msal.getAllAccounts();
		if (accounts.length === 0) return null;
		return toAuthAccount(accounts[0]);
	}

	async getAuthorizationHeader(): Promise<string | null> {
		await this.init();
		const accounts = this.msal!.getAllAccounts();
		if (accounts.length === 0) return null;
		try {
			const result = await this.msal!.acquireTokenSilent({
				scopes: [this.apiScope],
				account: accounts[0]
			});
			return `Bearer ${result.accessToken}`;
		} catch (e) {
			if (e instanceof InteractionRequiredAuthError) {
				await this.msal!.acquireTokenRedirect({ scopes: [this.apiScope] });
			}
			throw e;
		}
	}
}

let singleton: EntraAuthProviderImpl | null = null;

export function getEntraAuthProvider(): AuthProvider {
	if (singleton == null) singleton = new EntraAuthProviderImpl();
	return singleton;
}
