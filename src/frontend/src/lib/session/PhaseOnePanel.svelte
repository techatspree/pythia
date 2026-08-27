<script lang="ts">
	import Button from '$lib/ui/Button.svelte';
	import { _ } from 'svelte-i18n';
	import { log } from '$lib/log';
	import { submitVote, updateNotes, revealPhase2 } from '$lib/session/api';
	import type { SessionStore } from '$lib/session/store.svelte';

	// PHASE1 — blind individual estimate (task-067). Every participant (including
	// the moderator, who also estimates) submits a PERT triple; only the
	// submitted/total count is ever visible, never others' numbers. The moderator
	// additionally edits the shared discussion notes (debounced PUT) and reveals
	// PHASE2.
	//
	// These mutations do NOT apply their own response to the store (task-160).
	// They used to, "for snappy feedback" — but the returned DTO is built inside
	// the mutation's own transaction and is blind to anything committed
	// concurrently, so it could overwrite newer state the socket had already
	// delivered. The socket is the single writer; see the room page.
	let {
		store,
		sessionId,
		onError
	}: { store: SessionStore; sessionId: string; onError: (msg: string) => void } = $props();

	// Own triple (person-days). Blind: never seeded from anyone else.
	let optimistic = $state(0);
	let likely = $state(0);
	let pessimistic = $state(0);
	let submitted = $state(false);
	let busy = $state(false);

	// Denominator = participants expected to estimate (excludes a moderate-only
	// moderator).
	const voterCount = $derived(store.expectedVoterCount);

	async function submit() {
		busy = true;
		try {
			await submitVote(sessionId, {
				minEffort: optimistic,
				expectedEffort: likely,
				maxEffort: pessimistic
			});
			submitted = true;
		} catch (e: unknown) {
			log.error('phase1: submit vote failed', e);
			onError(e instanceof Error ? e.message : String(e));
		} finally {
			busy = false;
		}
	}

	// Moderator discussion-notes textarea (debounced PUT).
	let notesTimer: ReturnType<typeof setTimeout> | undefined;
	function onNotesInput(e: Event) {
		const value = (e.currentTarget as HTMLTextAreaElement).value;
		clearTimeout(notesTimer);
		notesTimer = setTimeout(async () => {
			try {
				await updateNotes(sessionId, value);
			} catch (err: unknown) {
				log.error('phase1: update notes failed', err);
				onError(err instanceof Error ? err.message : String(err));
			}
		}, 500);
	}

	async function reveal() {
		busy = true;
		try {
			await revealPhase2(sessionId);
		} catch (e: unknown) {
			log.error('phase1: reveal failed', e);
			onError(e instanceof Error ? e.message : String(e));
		} finally {
			busy = false;
		}
	}
</script>

<div class="space-y-4">
	{#if store.currentItem?.discussionNotes && !store.isModerator}
		<div class="border rounded p-3 bg-gray-50/40 text-sm">
			<span class="block text-sm font-medium mb-1">
				{$_('session.phaseOne.notes')}
			</span>
			<p class="whitespace-pre-wrap">{store.currentItem.discussionNotes}</p>
		</div>
	{/if}

	{#if store.iEstimate}
		<h3 class="text-sm font-semibold uppercase tracking-wide text-gray-500">
			{$_('session.phaseOne.yourEstimate')}
		</h3>
		<div class="grid grid-cols-3 gap-3">
			<label class="text-sm">
				<span class="block mb-1">{$_('session.phaseOne.optimistic')}</span>
				<input type="number" min="0" step="0.5" bind:value={optimistic} class="w-full border rounded px-2 py-1 text-sm text-right" />
			</label>
			<label class="text-sm">
				<span class="block mb-1">{$_('session.phaseOne.likely')}</span>
				<input type="number" min="0" step="0.5" bind:value={likely} class="w-full border rounded px-2 py-1 text-sm text-right" />
			</label>
			<label class="text-sm">
				<span class="block mb-1">{$_('session.phaseOne.pessimistic')}</span>
				<input type="number" min="0" step="0.5" bind:value={pessimistic} class="w-full border rounded px-2 py-1 text-sm text-right" />
			</label>
		</div>

		<div class="flex items-center gap-3">
			<Button
			
				onclick={submit}
				disabled={busy}
			
			>
				{submitted ? $_('session.phaseOne.resubmit') : $_('session.phaseOne.submit')}
			</Button>
			{#if submitted}
				<span class="text-sm text-green-600">✓ {$_('session.phaseOne.submitted')}</span>
			{/if}
		</div>
	{/if}

	<p class="text-sm text-gray-500" data-testid="phase1-count">
		{$_('session.phaseOne.submittedCount', {
			values: { count: store.submittedCount, total: voterCount }
		})}
	</p>

	{#if store.isModerator}
		<div class="border-t pt-4 space-y-4">
			<p class="text-xs text-gray-500">{$_('session.phaseOne.moderatorHint')}</p>
			<div>
				<label class="block text-sm font-medium mb-1" for="notes">
					{$_('session.phaseOne.notes')}
				</label>
				<textarea
					id="notes"
					rows="3"
					value={store.currentItem?.discussionNotes ?? ''}
					oninput={onNotesInput}
					placeholder={$_('session.phaseOne.notesPlaceholder')}
					class="w-full border rounded px-3 py-2 text-sm"
				></textarea>
			</div>
			<Button
			
				onclick={reveal}
				disabled={busy}
			
			>
				{$_('session.phaseOne.advance')}
			</Button>
		</div>
	{/if}
</div>
