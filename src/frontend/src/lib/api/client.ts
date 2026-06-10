import createClient from 'openapi-fetch';
import type { paths } from './schema.d.ts';
import { getAuthProvider } from '$lib/auth';

export function createApiClient(baseUrl: string = '/') {
	const client = createClient<paths>({ baseUrl });
	client.use({
		async onRequest({ request }) {
			try {
				const h = await getAuthProvider().getAuthorizationHeader();
				if (h) request.headers.set('Authorization', h);
			} catch (e) {
				console.error('Auth provider unavailable:', e);
			}
			return request;
		}
	});
	return client;
}

export type ApiClient = ReturnType<typeof createApiClient>;
