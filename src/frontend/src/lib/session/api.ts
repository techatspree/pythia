import { apiFetch } from '$lib/api/fetch';
import { assertOk } from '$lib/api/errors';
import type { components } from '$lib/api/schema';

// Thin typed wrappers over apiFetch for the collaborative-session REST surface
// (task-064/065). apiFetch attaches the auth header; assertOk turns a non-ok
// response into a user-facing message. Mutations go through here; realtime
// updates arrive over the WebSocket (socket.ts).

export type SessionDto = components['schemas']['SessionDto'];
type CreateSessionRequest = components['schemas']['CreateSessionRequest'];
type VoteRequest = components['schemas']['VoteRequest'];
type WsTicketDto = components['schemas']['WsTicketDto'];

const BASE = '/api/sessions';

async function readJson<T>(res: Response, fallback: string): Promise<T> {
	await assertOk(res, fallback);
	return (await res.json()) as T;
}

async function post<T>(path: string, fallback: string, body?: unknown): Promise<T> {
	const res = await apiFetch(path, {
		method: 'POST',
		...(body !== undefined
			? { headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }
			: {})
	});
	return readJson<T>(res, fallback);
}

export async function createSession(body: CreateSessionRequest): Promise<SessionDto> {
	return post(BASE, 'Failed to create session', body);
}

export async function getSession(id: string): Promise<SessionDto> {
	return readJson(await apiFetch(`${BASE}/${id}`), 'Failed to load session');
}

export async function listSessions(estimationId: string): Promise<SessionDto[]> {
	return readJson(await apiFetch(`${BASE}?estimationId=${estimationId}`), 'Failed to load sessions');
}

// No estimationId → all joinable (CREATED/RUNNING/SUSPENDED) sessions across
// estimations. A suspended session stays listed: this is how a parked room is
// found again and resumed.
export async function listJoinableSessions(): Promise<SessionDto[]> {
	return readJson(await apiFetch(BASE), 'Failed to load open sessions');
}

export async function join(id: string): Promise<SessionDto> {
	return post(`${BASE}/${id}/join`, 'Failed to join session');
}

export async function start(id: string): Promise<SessionDto> {
	return post(`${BASE}/${id}/start`, 'Failed to start session');
}

export async function updateNotes(id: string, notes: string): Promise<SessionDto> {
	const res = await apiFetch(`${BASE}/${id}/items/current/notes`, {
		method: 'PUT',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ notes })
	});
	return readJson(res, 'Failed to update notes');
}

export async function revealPhase2(id: string): Promise<SessionDto> {
	return post(`${BASE}/${id}/items/current/phase2`, 'Failed to reveal phase 2');
}

export async function finalize(id: string): Promise<SessionDto> {
	return post(`${BASE}/${id}/items/current/finalize`, 'Failed to finalize item');
}

export async function submitVote(id: string, vote: VoteRequest): Promise<SessionDto> {
	return post(`${BASE}/${id}/votes`, 'Failed to submit vote', vote);
}

export async function agree(id: string): Promise<SessionDto> {
	return post(`${BASE}/${id}/agree`, 'Failed to record agreement');
}

export async function cancel(id: string): Promise<SessionDto> {
	return post(`${BASE}/${id}/cancel`, 'Failed to cancel session');
}

export async function suspendSession(id: string): Promise<SessionDto> {
	return post(`${BASE}/${id}/suspend`, 'Failed to pause session');
}

export async function resumeSession(id: string): Promise<SessionDto> {
	return post(`${BASE}/${id}/resume`, 'Failed to resume session');
}

export async function endSessionEarly(id: string): Promise<SessionDto> {
	return post(`${BASE}/${id}/end-early`, 'Failed to end session early');
}

export async function getWsTicket(id: string): Promise<string> {
	const dto = await post<WsTicketDto>(`${BASE}/${id}/ws-ticket`, 'Failed to obtain a session ticket');
	return dto.ticket;
}
