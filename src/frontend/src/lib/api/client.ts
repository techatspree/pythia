import createClient from 'openapi-fetch';
import type { paths } from './schema.d.ts';

export function createApiClient(baseUrl: string = '/') {
	return createClient<paths>({ baseUrl });
}

export type ApiClient = ReturnType<typeof createApiClient>;
