/// <reference types="vite/client" />

interface ImportMetaEnv {
	readonly VITE_AUTH_PROVIDER: 'dev' | 'entra' | 'keycloak';
	readonly VITE_ENTRA_TENANT_ID?: string;
	readonly VITE_ENTRA_SPA_CLIENT_ID?: string;
	readonly VITE_ENTRA_API_CLIENT_ID?: string;
	readonly VITE_ENTRA_REDIRECT_URI?: string;
}

interface ImportMeta {
	readonly env: ImportMetaEnv;
}
