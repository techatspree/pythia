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
	apply(session: SessionDto): void {
		this.session = session;
		this.connected = true;
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
}
