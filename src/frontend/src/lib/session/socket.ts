import { getWsTicket, type SessionDto } from './api';
import { log } from '$lib/log';

// Push-only WebSocket client for a collaborative session (task-066). The server
// never accepts domain messages — mutations go through the REST api.ts. Auth is
// a single-use ticket on the handshake query string (browsers cannot set an
// Authorization header on a WS handshake). Auto-reconnects with capped backoff,
// re-fetching a fresh ticket each attempt. Failures both log and surface via
// onError (the room maps that to ErrorBanner).
//
// Connection state is reported through onConnectionChange (task-147). It is the
// ONLY source of truth for "am I live": the store must never infer liveness from
// a payload having arrived, because that can never become false again.
//
// Liveness needs an application-level heartbeat because the browser gives us
// nothing to work with: JS cannot send WebSocket protocol pings and is not
// notified of pongs, and a healthy but quiet room legitimately sends no frames
// for minutes. The server therefore emits {"type":"heartbeat"} periodically and
// ANY received frame feeds the watchdog below. Without it, a connection dropped
// without a close frame (sleep, NAT/proxy idle-kill, a silently broken path)
// would never fire `onclose`, so we would never reconnect and never notice.

export interface SessionSocketHandle {
	close: () => void;
}

const MAX_BACKOFF_MS = 15_000;
const BASE_BACKOFF_MS = 1_000;
// > 2 server heartbeats, so a single dropped frame is not a false positive.
const IDLE_TIMEOUT_MS = 45_000;

export function connectSessionSocket(
	sessionId: string,
	onSession: (session: SessionDto) => void,
	onError: (message: string) => void,
	onConnectionChange: (connected: boolean) => void
): SessionSocketHandle {
	let socket: WebSocket | null = null;
	let closed = false;
	let attempt = 0;
	let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
	let idleTimer: ReturnType<typeof setTimeout> | null = null;

	function wsUrl(ticket: string): string {
		const scheme = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
		return `${scheme}//${window.location.host}/ws/sessions/${sessionId}?ticket=${encodeURIComponent(ticket)}`;
	}

	function clearIdleTimer(): void {
		if (idleTimer) {
			clearTimeout(idleTimer);
			idleTimer = null;
		}
	}

	// Any frame is proof of life. On expiry the connection is dead in a way the
	// browser has not noticed, so tear it down ourselves and reopen immediately —
	// this is a first failure, not a retry, so the backoff is reset rather than
	// waiting out a delay earned by earlier failures.
	function armWatchdog(): void {
		clearIdleTimer();
		if (closed) return;
		idleTimer = setTimeout(() => {
			if (closed) return;
			log.warn(`session socket: no frame for ${IDLE_TIMEOUT_MS} ms — assuming dead`, sessionId);
			onConnectionChange(false);
			attempt = 0;
			const dead = socket;
			socket = null;
			dead?.close();
		}, IDLE_TIMEOUT_MS);
	}

	function handleMessage(data: unknown): void {
		if (typeof data !== 'string') return;
		// Reset before parsing: even a frame we do not understand proves the
		// connection is alive.
		armWatchdog();
		try {
			const msg = JSON.parse(data) as { type?: string; session?: SessionDto; message?: string };
			if (msg.type === 'heartbeat') {
				// Liveness only — deliberately not a state update.
				return;
			}
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
			onConnectionChange(true);
			armWatchdog();
		};
		ws.onmessage = (ev) => handleMessage(ev.data);
		// The browser fires onclose after onerror, and that is what drives the
		// reconnect — scheduling one here too would double the backoff per failure.
		ws.onerror = () => {
			log.error('session socket: transport error', sessionId);
			onConnectionChange(false);
		};
		ws.onclose = () => {
			clearIdleTimer();
			if (closed) return;
			log.debug('session socket closed — reconnecting', sessionId);
			onConnectionChange(false);
			scheduleReconnect();
		};
	}

	void open();

	return {
		close() {
			closed = true;
			if (reconnectTimer) clearTimeout(reconnectTimer);
			clearIdleTimer();
			onConnectionChange(false);
			socket?.close();
		}
	};
}
