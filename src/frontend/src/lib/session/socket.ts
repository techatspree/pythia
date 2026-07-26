import { getWsTicket, type SessionDto } from './api';
import { log } from '$lib/log';

// Push-only WebSocket client for a collaborative session (task-066). The server
// never accepts domain messages — mutations go through the REST api.ts. Auth is
// a single-use ticket on the handshake query string (browsers cannot set an
// Authorization header on a WS handshake). Auto-reconnects with capped backoff,
// re-fetching a fresh ticket each attempt. Failures both log and surface via
// onError (the room maps that to ErrorBanner).

export interface SessionSocketHandle {
	close: () => void;
}

const MAX_BACKOFF_MS = 15_000;
const BASE_BACKOFF_MS = 1_000;

export function connectSessionSocket(
	sessionId: string,
	onSession: (session: SessionDto) => void,
	onError: (message: string) => void
): SessionSocketHandle {
	let socket: WebSocket | null = null;
	let closed = false;
	let attempt = 0;
	let reconnectTimer: ReturnType<typeof setTimeout> | null = null;

	function wsUrl(ticket: string): string {
		const scheme = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
		return `${scheme}//${window.location.host}/ws/sessions/${sessionId}?ticket=${encodeURIComponent(ticket)}`;
	}

	function handleMessage(data: unknown): void {
		if (typeof data !== 'string') return;
		try {
			const msg = JSON.parse(data) as { type?: string; session?: SessionDto; message?: string };
			if (msg.type === 'session' && msg.session) {
				onSession(msg.session);
			} else if (msg.type === 'error') {
				log.error('session socket error frame:', msg.message);
				onError(msg.message ?? 'Session error');
			}
		} catch (e) {
			log.error('session socket: could not parse frame', e);
		}
	}

	function scheduleReconnect(): void {
		if (closed) return;
		const delay = Math.min(BASE_BACKOFF_MS * 2 ** attempt, MAX_BACKOFF_MS);
		attempt += 1;
		reconnectTimer = setTimeout(() => void open(), delay);
	}

	async function open(): Promise<void> {
		if (closed) return;
		let ticket: string;
		try {
			ticket = await getWsTicket(sessionId);
		} catch (e) {
			log.error('session socket: failed to obtain ticket', e);
			onError(e instanceof Error ? e.message : String(e));
			scheduleReconnect();
			return;
		}
		if (closed) return;
		const ws = new WebSocket(wsUrl(ticket));
		socket = ws;
		ws.onopen = () => {
			attempt = 0;
			log.debug('session socket open', sessionId);
		};
		ws.onmessage = (ev) => handleMessage(ev.data);
		ws.onclose = () => {
			if (closed) return;
			log.debug('session socket closed — reconnecting', sessionId);
			scheduleReconnect();
		};
	}

	void open();

	return {
		close() {
			closed = true;
			if (reconnectTimer) clearTimeout(reconnectTimer);
			socket?.close();
		}
	};
}
