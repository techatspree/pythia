import { apiFetch } from '$lib/api/fetch';
import { log } from '$lib/log';
import type { components } from '$lib/api/schema';

type MutationLogEntryDto = components['schemas']['MutationLogEntryDto'];
type ConflictDetailsDto = components['schemas']['ConflictDetailsDto'];
type EstimationVersionDto = components['schemas']['EstimationVersionDto'];

function messageOf(e: unknown): string {
	return e instanceof Error ? e.message : String(e);
}

// Client for the backend undo/redo/history endpoints (task-075). It owns only
// the mutation-log view plus the conflict/error signals — NOT the draft state:
// the editor page keeps that and applies the version returned by undo/redo via
// the `onResult` hook. All HTTP goes through the auth-aware `apiFetch`.
export class UndoStore {
	estimationId: string;
	history = $state<MutationLogEntryDto[]>([]);
	loading = $state(false);
	conflict = $state<ConflictDetailsDto | null>(null);
	// Non-409 failure message; the editor page mirrors this into its ErrorBanner.
	error = $state<string | null>(null);
	// Set by the owner: invoked with the fresh version after a successful
	// undo/redo so the page can replace its draft state.
	onResult: ((version: EstimationVersionDto) => void) | null = null;

	canUndo = $derived(this.history.some((e) => e.status === 'ACTIVE'));
	// Redo targets the latest UNDONE entry; a later ACTIVE entry supersedes it,
	// mirroring the backend redo-conflict rule.
	canRedo = $derived.by(() => {
		const undone = this.history.filter((e) => e.status === 'UNDONE');
		if (undone.length === 0) return false;
		const latest = undone.reduce((a, b) => (a.sequenceNumber > b.sequenceNumber ? a : b));
		return !this.history.some(
			(e) => e.status === 'ACTIVE' && e.sequenceNumber > latest.sequenceNumber
		);
	});

	constructor(estimationId: string) {
		this.estimationId = estimationId;
	}

	async refresh(): Promise<void> {
		this.loading = true;
		try {
			const res = await apiFetch(`/api/estimations/${this.estimationId}/versions/draft/history`);
			if (res.ok) {
				this.history = (await res.json()) as MutationLogEntryDto[];
			} else {
				this.error = `Could not load undo history:  ${res.status})`;
				log.error('UndoStore.refresh failed:');
			}
		} catch (e) {
			// todo: check if an exception can occur at all
			this.error = `Could not load undo history: ${messageOf(e)}`;
			log.error('UndoStore.refresh failed:', e);
		} finally {
			this.loading = false;
		}
	}

	async undo(): Promise<EstimationVersionDto | null> {
		return this.mutate('undo');
	}

	async redo(): Promise<EstimationVersionDto | null> {
		return this.mutate('redo');
	}

	clearConflict(): void {
		this.conflict = null;
	}

	// Shared undo/redo POST. On 409 it parses ConflictDetailsDto into `conflict`
	// and returns null (no throw); any other failure sets `error` + logs.
	private async mutate(kind: 'undo' | 'redo'): Promise<EstimationVersionDto | null> {
		this.loading = true;
		this.conflict = null;
		try {
			const res = await apiFetch(
				`/api/estimations/${this.estimationId}/versions/draft/${kind}`,
				{ method: 'POST' }
			);
			if (res.status === 409) {
				this.conflict = (await res.json()) as ConflictDetailsDto;
				return null;
			}
			if (!res.ok) throw new Error(`${kind} failed (${res.status})`);
			const version = (await res.json()) as EstimationVersionDto;
			await this.refresh();
			this.onResult?.(version);
			return version;
		} catch (e) {
			this.error = `Could not ${kind}: ${messageOf(e)}`;
			log.error(`UndoStore.${kind} failed:`, e);
			return null;
		} finally {
			this.loading = false;
		}
	}
}
