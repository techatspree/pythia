import { log } from '$lib/log';

// Global backend-connection watchdog (task-136). When `apiFetch` detects a
// genuine connection loss — a network rejection, our request timeout, or a
// 502/503/504 — it calls `reportLost(...)`, which BLOCKS the whole app via
// `ConnectionLostDialog` (mounted in the root layout). The block is intentional:
// a top-of-page ErrorBanner is missed by a user editing near the bottom of a long
// estimation, who then keeps typing while autosave silently fails → data loss.
//
// The dialog cannot be dismissed until the backend answers a heartbeat
// (GET /api/ping) AND the user acknowledges. Acknowledging does a FULL page reload
// so the unsaved (lost) in-memory edit is discarded and the editor re-syncs from
// the backend's last-saved state — rather than a later autosave silently
// persisting the "lost" change.

const HEARTBEAT_INTERVAL_MS = 3000;

class ConnectionStore {
	blocked = $state(false);
	backendAlive = $state(false);
	message = $state('');

	private heartbeat: ReturnType<typeof setInterval> | undefined;

	// Idempotent: the first failure wins. Subsequent failures (e.g. a retrying
	// autosave) must not reset the state or restart the heartbeat.
	reportLost(message: string): void {
		if (this.blocked) return;
		this.blocked = true;
		this.backendAlive = false;
		this.message = message;
		log.error('Backend connection lost:', message);
		this.startHeartbeat();
	}

	private startHeartbeat(): void {
		this.heartbeat = setInterval(async () => {
			try {
				// Deliberate raw fetch, not apiFetch: the heartbeat carries no auth
				// token and must NOT route back through this connection watchdog.
				// eslint-disable-next-line no-restricted-syntax
				const res = await fetch('/api/ping');
				if (res.ok) {
					this.backendAlive = true;
					log.info('Backend reachable again (heartbeat)');
					this.stopHeartbeat();
				}
			} catch {
				// Still unreachable — keep polling.
			}
		}, HEARTBEAT_INTERVAL_MS);
	}

	private stopHeartbeat(): void {
		if (this.heartbeat !== undefined) {
			clearInterval(this.heartbeat);
			this.heartbeat = undefined;
		}
	}

	acknowledge(): void {
		this.stopHeartbeat();
		this.blocked = false;
		// Full reload (NOT invalidateAll): discards the unsaved editor state and
		// re-hydrates from the backend's last-saved version.
		window.location.reload();
	}
}

export const connection = new ConnectionStore();
