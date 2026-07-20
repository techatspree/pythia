import type { AuthAccount, AuthProvider } from './AuthProvider';

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

	async init(): Promise<void> {}

	async login(): Promise<void> {
		// The actual user picker is rendered by DevLoginDialog.svelte,
		// which calls setSubject() below when the tester chooses one.
		// This method is a no-op so callers can `await login()` and
		// then check getAccount() once the user has picked.
	}

	async logout(): Promise<void> {
		if (typeof localStorage !== 'undefined') {
			localStorage.removeItem(STORAGE_KEY);
		}
	}

	// The dev provider's hard-wired roles already agree with the backend dev
	// augmentor (both derive from the Dev <subject> header), so it resolves the
	// account client-side without a /api/auth/me round-trip.
	async loadAccount(): Promise<AuthAccount | null> {
		return this.getAccount();
	}

	getAccount(): AuthAccount | null {
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
