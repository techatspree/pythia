import type { components } from '$lib/api/schema';
import type { SessionDto } from './api';

type SessionItemDto = components['schemas']['SessionItemDto'];
type ParticipantDto = components['schemas']['ParticipantDto'];

// Runes store for a live session (task-066). Holds the latest SessionDto pushed
// over the WebSocket as `$state` and exposes `$derived` helpers. The current
// user's subjectId is fixed at construction (from the auth account), so
// isModerator / myParticipant recompute purely from the session state.
export class SessionStore {
	session = $state<SessionDto | null>(null);
	connected = $state(false);

	private readonly subjectId: string | null;

	constructor(subjectId: string | null) {
		this.subjectId = subjectId;
	}

	// Replace the whole snapshot from a socket frame.
	//
	// ONLY two callers are legitimate (task-160): the socket's onSession
	// callback, and the room's initial load — which runs before the socket is
	// opened, so no frame can precede it. In particular a MUTATION's own REST
	// response must never be applied: it is built inside that mutation's
	// transaction and is blind to anything committed concurrently, so applying
	// it makes this slot a second, unordered writer that can roll the room back
	// to a state the socket has already superseded. Every mutation publishes a
	// SessionChangedEvent, so the acting client's socket carries the
	// authoritative snapshot anyway.
	//
	// This deliberately does NOT touch `connected` (task-147): liveness inferred
	// from "a payload arrived once" can never become false again, so the room's
	// indicator stayed green over a dead socket and told the user stale data was
	// live. Connection state comes only from setConnected(), driven by the
	// socket's open/close/error/watchdog events.
	apply(session: SessionDto): void {
		this.session = session;
	}

	setConnected(value: boolean): void {
		this.connected = value;
	}

	// Reactive getters (not `$derived` fields, which would read the
	// constructor-assigned `subjectId` before it is set): each reads the
	// `session` `$state`, so Svelte tracks it when used in a template.
	get currentItem(): SessionItemDto | null {
		return this.session ? (this.session.items[this.session.currentItemIndex] ?? null) : null;
	}

	get isModerator(): boolean {
		return this.session != null && this.session.moderatorSubjectId === this.subjectId;
	}

	get myParticipant(): ParticipantDto | null {
		return this.session?.participants.find((p) => p.subjectId === this.subjectId) ?? null;
	}

	get submittedCount(): number {
		return this.currentItem?.submittedVoteCount ?? 0;
	}

	// True when the current user is expected to estimate: an ESTIMATOR
	// participant, or the moderator when the session lets the moderator vote.
	get iEstimate(): boolean {
		if (!this.session) return false;
		if (this.isModerator) return this.session.moderatorEstimates;
		return this.myParticipant?.role === 'ESTIMATOR';
	}

	// Denominator for the submitted/total count: the ESTIMATOR participants
	// plus the moderator iff the moderator estimates.
	get expectedVoterCount(): number {
		if (!this.session) return 0;
		const estimators = this.session.participants.filter((p) => p.role === 'ESTIMATOR').length;
		return estimators + (this.session.moderatorEstimates ? 1 : 0);
	}
}
