import { apiFetch } from '$lib/api/fetch';
import { assertOk } from '$lib/api/errors';
import type { components } from '$lib/api/schema';

// Thin typed wrappers over apiFetch for the per-installation system settings
// (task-146). The stylesheet itself is NOT fetched here — the root layout pulls
// it in with a plain <link rel="stylesheet" href="/api/system/css">, which is
// why that endpoint is @PermitAll on the backend.

export type SystemSettingsDto = components['schemas']['SystemSettingsDto'];
export type EffortDriverDto = components['schemas']['EffortDriverDto'];

const BASE = '/api/system';

export async function getSystemSettings(): Promise<SystemSettingsDto> {
	const res = await apiFetch(BASE);
	await assertOk(res, 'Failed to load the system settings');
	return (await res.json()) as SystemSettingsDto;
}

export async function updateSystemSettings(displayName: string | null): Promise<void> {
	const res = await apiFetch(BASE, {
		method: 'PUT',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ displayName })
	});
	await assertOk(res, 'Failed to save the system settings');
}

export async function getStandardDrivers(): Promise<EffortDriverDto[]> {
	const res = await apiFetch(`${BASE}/effort-drivers`);
	await assertOk(res, 'Failed to load the standard effort drivers');
	return (await res.json()) as EffortDriverDto[];
}

export async function replaceStandardDrivers(
	drivers: EffortDriverDto[]
): Promise<EffortDriverDto[]> {
	const res = await apiFetch(`${BASE}/effort-drivers`, {
		method: 'PUT',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(drivers)
	});
	await assertOk(res, 'Failed to save the standard effort drivers');
	return (await res.json()) as EffortDriverDto[];
}

export async function uploadSystemCss(file: File): Promise<void> {
	const form = new FormData();
	form.append('file', file);
	const res = await apiFetch(`${BASE}/css`, { method: 'PUT', body: form });
	await assertOk(res, 'Failed to upload the stylesheet');
}

export async function deleteSystemCss(): Promise<void> {
	const res = await apiFetch(`${BASE}/css`, { method: 'DELETE' });
	await assertOk(res, 'Failed to remove the stylesheet');
}
