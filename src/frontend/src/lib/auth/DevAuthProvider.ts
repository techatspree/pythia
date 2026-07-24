import type { AuthAccount, AuthProvider } from './AuthProvider';
import { fetchCurrentUserAccount } from './currentUser';
import { log } from '$lib/log';

type DevUser = {
	subjectId: string;
	email: string;
	displayName: string;
	roles: ('VIEWER' | 'ESTIMATOR' | 'ADMIN')[];
};

export const DEV_USERS: Record<string, DevUser> = {
	'dev-viewer': {
		subjectId: 'dev-viewer',
		email: 'viewer@dev.local',
		displayName: 'Dev Viewer',
		roles: ['VIEWER']
	},
	'dev-estimator': {
		subjectId: 'dev-estimator',
		email: 'estimator@dev.local',
		displayName: 'Dev Estimator',
		roles: ['VIEWER', 'ESTIMATOR']
	},
	'dev-admin': {
		subjectId: 'dev-admin',
		email: 'admin@dev.local',
		displayName: 'Dev Admin',
		roles: ['VIEWER', 'ESTIMATOR', 'ADMIN']
	}
};

const STORAGE_KEY = 'devAuthSubject';

function readSubject(): string | null {
	if (typeof localStorage === 'undefined') return null;
	return localStorage.getItem(STORAGE_KEY);
}

function toAccount(user: DevUser): AuthAccount {
	return {
		subjectId: user.subjectId,
		email: user.email,
		displayName: user.displayName,
		roles: user.roles,
		providerName: 'dev'
	};
}

class DevAuthProviderImpl implements AuthProvider {
	readonly name = 'dev' as const;

	// The last account resolved by loadAccount() (with the backend language),
	// so getAccount() returns it synchronously — mirrors the Entra provider.
	private cached: AuthAccount | null = null;

	async init(): Promise<void> {}

	async login(): Promise<void> {
		// The actual user picker is rendered by DevLoginDialog.svelte,
		// which calls setSubject() below when the tester chooses one.
		// This method is a no-op so callers can `await login()` and
		// then check getAccount() once the user has picked.
	}

	async logout(): Promise<void> {
		this.cached = null;
		if (typeof localStorage !== 'undefined') {
			localStorage.removeItem(STORAGE_KEY);
		}
	}

	// Roles/displayName stay client-side — the dev provider's hard-wired roles
	// already agree with the backend dev augmentor (task-120). Only the persisted
	// UI `language` preference is read from the backend (`GET /api/auth/me`), so a
	// language switch survives a reload (task-127); the dev user is provisioned,
	// so `/me` returns its stored language.
	async loadAccount(): Promise<AuthAccount | null> {
		const account = this.clientAccount();
		if (account == null) {
			this.cached = null;
			return null;
		}
		try {
			const backend = await fetchCurrentUserAccount();
			if (backend?.language != null) account.language = backend.language;
		} catch (e) {
			log.error('DevAuthProvider: could not read language from /api/auth/me', e);
		}
		this.cached = account;
		return account;
	}

	getAccount(): AuthAccount | null {
		return this.cached ?? this.clientAccount();
	}

	private clientAccount(): AuthAccount | null {
		const subject = readSubject();
		if (subject == null) return null;
		const user = DEV_USERS[subject];
		if (user == null) return null;
		return toAccount(user);
	}

	async getAuthorizationHeader(): Promise<string | null> {
		const subject = readSubject();
		if (subject == null) return null;
		return `Dev ${subject}`;
	}
}

let singleton: DevAuthProviderImpl | null = null;

export function getDevAuthProvider(): AuthProvider {
	if (singleton == null) singleton = new DevAuthProviderImpl();
	return singleton;
}

export function setDevSubject(subjectId: string): void {
	if (typeof localStorage === 'undefined') return;
	localStorage.setItem(STORAGE_KEY, subjectId);
}
