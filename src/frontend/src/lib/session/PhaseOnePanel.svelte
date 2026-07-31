<script lang="ts">
	import { _ } from 'svelte-i18n';
	import { log } from '$lib/log';
	import { submitVote, updateNotes, revealPhase2 } from '$lib/session/api';
	import type { SessionStore } from '$lib/session/store.svelte';

	// PHASE1 — blind individual estimate (task-067). Every participant (including
	// the moderator, who also estimates) submits a PERT triple; only the
	// submitted/total count is ever visible, never others' numbers. The moderator
	// additionally edits the shared discussion notes (debounced PUT) and reveals
	// PHASE2. Mutations return the fresh SessionDto which we apply straight to the
	// store for snappy feedback; the socket also pushes it to everyone.
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

	// Everyone is expected to vote, so the denominator is the participant count.
	const voterCount = $derived(store.session?.participants.length ?? 0);

	async function submit() {
		busy = true;
		try {
			const dto = await submitVote(sessionId, {
				minEffort: optimistic,
				expectedEffort: likely,
				maxEffort: pessimistic
			});
			store.apply(dto);
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
				store.apply(await updateNotes(sessionId, value));
			} catch (err: unknown) {
				log.error('phase1: update notes failed', err);
				onError(err instanceof Error ? err.message : String(err));
			}
		}, 500);
	}

	async function reveal() {
		busy = true;
		try {
			store.apply(await revealPhase2(sessionId));
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
		<div class="border rounded p-3 bg-gray-50 text-sm">
			<span class="block text-xs font-semibold uppercase tracking-wide text-gray-500 mb-1">
				{$_('session.phaseOne.notes')}
			</span>
			<p class="whitespace-pre-wrap">{store.currentItem.discussionNotes}</p>
		</div>
	{/if}

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
		<button
			type="button"
			onclick={submit}
			disabled={busy}
			class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45] disabled:opacity-50"
		>
			{submitted ? $_('session.phaseOne.resubmit') : $_('session.phaseOne.submit')}
		</button>
		{#if submitted}
			<span class="text-sm text-green-600">✓ {$_('session.phaseOne.submitted')}</span>
		{/if}
	</div>

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
			<button
				type="button"
				onclick={reveal}
				disabled={busy}
				class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45] disabled:opacity-50"
			>
				{$_('session.phaseOne.advance')}
			</button>
		</div>
	{/if}
</div>
