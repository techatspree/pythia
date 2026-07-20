import {
	PublicClientApplication,
	InteractionRequiredAuthError,
	type Configuration
} from '@azure/msal-browser';
import type { AuthAccount, AuthProvider } from './AuthProvider';
import { fetchCurrentUserAccount } from './currentUser';

function requireEnv(name: keyof ImportMetaEnv): string {
	const v = import.meta.env[name];
	if (v == null || v === '') {
		throw new Error(`EntraAuthProvider: missing required env var ${String(name)}`);
	}
	return v as string;
}

class EntraAuthProviderImpl implements AuthProvider {
	readonly name = 'entra' as const;

	private msal: PublicClientApplication | null = null;
	private initialized = false;
	private apiScope = '';
	// The authoritative account, resolved from GET /api/auth/me by loadAccount().
	// getAccount() returns this synchronously; roles come from the backend (the
	// access token), NOT the id token — so app roles only need to live on
	// estimation-api, and the UI can never disagree with backend enforcement.
	private cachedAccount: AuthAccount | null = null;

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
		// Process the auth-code response IN PLACE on `redirectUri` instead of
		// bouncing the browser back to the page login started on. Our redirectUri
		// is the SPA root (`/`) while login is triggered from `/projects` (the root
		// route `goto()`s there); with the default `navigateToLoginRequestUrl: true`
		// MSAL navigates `/` -> `/projects?state=…` to finish the exchange — which
		// races our own `goto('/projects')` and drops the redirect response, leaving
		// an account but no cached access token (anonymous /api calls → 401). With
		// `false` the exchange completes here on `/`, caches the token, and our
		// router then navigates normally. (In msal-browser v5 this is a
		// handleRedirectPromise option, not a Configuration.auth field.)
		await this.msal.handleRedirectPromise({ navigateToLoginRequestUrl: false });
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

	async loadAccount(): Promise<AuthAccount | null> {
		await this.init();
		// No MSAL account means the user is not signed in — return null so
		// RequireAuth triggers the login redirect. Only fetch /api/auth/me once
		// MSAL has an account (so apiFetch can attach the bearer token).
		if (this.msal!.getAllAccounts().length === 0) {
			this.cachedAccount = null;
			return null;
		}
		this.cachedAccount = await fetchCurrentUserAccount();
		return this.cachedAccount;
	}

	getAccount(): AuthAccount | null {
		return this.cachedAccount;
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
