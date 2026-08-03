<script lang="ts">
	import { _, locale } from 'svelte-i18n';
	import { formatFixed, DEFAULT_LOCALE } from '$lib/format';
	import { log } from '$lib/log';
	import { submitVote, agree, finalize, updateNotes } from '$lib/session/api';
	import type { SessionStore } from '$lib/session/store.svelte';
	// Single source of truth for mean/spread: reduce the votes with the SAME
	// domain code the backend uses (task-062), reached through the JS-friendly
	// factory in DomainFactory.kt — the same pattern adapter.ts uses to call the
	// Kotlin/JS bundle. The result must match the backend AggregateDto (asserted
	// in the e2e); we compute it locally only for snappy display.
	import { EstimatorVote, aggregateVotes } from '$lib/domain/domain.mjs';

	let {
		store,
		sessionId,
		onError
	}: { store: SessionStore; sessionId: string; onError: (msg: string) => void } = $props();

	const loc = $derived($locale ?? DEFAULT_LOCALE);

	const votes = $derived(store.currentItem?.votes ?? []);

	// Locally-computed aggregate via the domain bundle (not a hand-rolled average).
	const aggregate = $derived.by(() => {
		if (votes.length === 0) return null;
		return aggregateVotes(
			votes.map((v) => new EstimatorVote(v.triple.minEffort, v.triple.expectedEffort, v.triple.maxEffort))
		);
	});

	const estimators = $derived(
		store.session?.participants.filter((p) => p.role === 'ESTIMATOR') ?? []
	);
	const agreedCount = $derived(estimators.filter((p) => p.agreed).length);
	const allAgreed = $derived(estimators.length > 0 && agreedCount === estimators.length);

	// Estimator revision triple.
	let optimistic = $state(0);
	let likely = $state(0);
	let pessimistic = $state(0);
	let busy = $state(false);

	async function submitRevision() {
		busy = true;
		try {
			store.apply(
				await submitVote(sessionId, {
					minEffort: optimistic,
					expectedEffort: likely,
					maxEffort: pessimistic
				})
			);
		} catch (e: unknown) {
			log.error('phase2: submit revision failed', e);
			onError(e instanceof Error ? e.message : String(e));
		} finally {
			busy = false;
		}
	}

	async function toggleAgree() {
		busy = true;
		try {
			store.apply(await agree(sessionId));
		} catch (e: unknown) {
			log.error('phase2: agree failed', e);
			onError(e instanceof Error ? e.message : String(e));
		} finally {
			busy = false;
		}
	}

	async function finalizeItem() {
		busy = true;
		try {
			store.apply(await finalize(sessionId));
		} catch (e: unknown) {
			log.error('phase2: finalize failed', e);
			onError(e instanceof Error ? e.message : String(e));
		} finally {
			busy = false;
		}
	}

	// Moderator discussion-notes textarea (debounced PUT) — also editable in
	// phase 2 so discussion captured at reveal time is persisted on finalize.
	let notesTimer: ReturnType<typeof setTimeout> | undefined;
	function onNotesInput(e: Event) {
		const value = (e.currentTarget as HTMLTextAreaElement).value;
		clearTimeout(notesTimer);
		notesTimer = setTimeout(async () => {
			try {
				store.apply(await updateNotes(sessionId, value));
			} catch (err: unknown) {
				log.error('phase2: update notes failed', err);
				onError(err instanceof Error ? err.message : String(err));
			}
		}, 500);
	}
</script>

<div class="space-y-5">
	{#if aggregate?.diverged}
		<div
			class="border border-amber-300 bg-amber-50 text-amber-800 rounded px-3 py-2 text-sm"
			data-testid="diverged-banner"
		>
			{$_('session.phaseTwo.diverged')}
		</div>
	{/if}

	{#if store.isModerator}
		<div>
			<label class="block text-sm font-medium mb-1" for="notes-p2">{$_('session.phaseOne.notes')}</label>
			<textarea
				id="notes-p2"
				rows="3"
				value={store.currentItem?.discussionNotes ?? ''}
				oninput={onNotesInput}
				placeholder={$_('session.phaseOne.notesPlaceholder')}
				class="w-full border rounded px-3 py-2 text-sm"
			></textarea>
		</div>
	{:else if store.currentItem?.discussionNotes}
		<div class="border rounded p-3 bg-gray-50 text-sm">
			<span class="block text-xs font-semibold uppercase tracking-wide text-gray-500 mb-1">
				{$_('session.phaseOne.notes')}
			</span>
			<p class="whitespace-pre-wrap">{store.currentItem.discussionNotes}</p>
		</div>
	{/if}

	<div>
		<h3 class="text-sm font-semibold uppercase tracking-wide text-gray-500 mb-2">
			{$_('session.phaseTwo.title')}
		</h3>
		{#if votes.length === 0}
			<p class="text-sm text-gray-500">{$_('session.phaseTwo.noVotes')}</p>
		{:else}
			<div class="overflow-x-auto">
				<table class="w-full text-sm border rounded">
					<thead class="bg-gray-50 text-xs uppercase tracking-wide text-gray-500">
						<tr>
							<th class="text-left px-3 py-2">{$_('session.phaseTwo.estimator')}</th>
							<th class="text-right px-3 py-2">{$_('session.phaseOne.optimistic')}</th>
							<th class="text-right px-3 py-2">{$_('session.phaseOne.likely')}</th>
							<th class="text-right px-3 py-2">{$_('session.phaseOne.pessimistic')}</th>
							<th class="text-left px-3 py-2">{$_('session.phaseTwo.phase')}</th>
						</tr>
					</thead>
					<tbody>
						{#each votes as v (v.participantSubjectId + v.phase)}
							<tr class="border-t">
								<td class="px-3 py-2">{v.displayName ?? v.participantSubjectId}</td>
								<td class="px-3 py-2 text-right">{formatFixed(v.triple.minEffort, loc, 1)}</td>
								<td class="px-3 py-2 text-right">{formatFixed(v.triple.expectedEffort, loc, 1)}</td>
								<td class="px-3 py-2 text-right">{formatFixed(v.triple.maxEffort, loc, 1)}</td>
								<td class="px-3 py-2 text-gray-400">{$_(`session.phase.${v.phase}`)}</td>
							</tr>
						{/each}
					</tbody>
				</table>
			</div>
		{/if}
	</div>

	{#if aggregate}
		<div class="border rounded-lg p-4 bg-white" data-testid="aggregate">
			<h3 class="text-sm font-semibold uppercase tracking-wide text-gray-500 mb-3">
				{$_('session.phaseTwo.aggregate')}
			</h3>
			<dl class="grid grid-cols-2 gap-x-6 gap-y-2 text-sm">
				<dt class="text-gray-500">{$_('session.phaseTwo.meanTriple')}</dt>
				<dd class="text-right font-medium" data-testid="aggregate-mean">
					{formatFixed(aggregate.meanMin, loc, 1)} / {formatFixed(aggregate.meanExpected, loc, 1)} / {formatFixed(
						aggregate.meanMax,
						loc,
						1
					)}
				</dd>
				<dt class="text-gray-500">{$_('session.phaseTwo.pertMean')}</dt>
				<dd class="text-right font-medium">{formatFixed(aggregate.pertMean, loc, 2)}</dd>
				<dt class="text-gray-500">{$_('session.phaseTwo.range')}</dt>
				<dd class="text-right">{formatFixed(aggregate.expectedRange, loc, 1)}</dd>
				<dt class="text-gray-500">{$_('session.phaseTwo.stdDev')}</dt>
				<dd class="text-right">{formatFixed(aggregate.expectedStdDev, loc, 2)}</dd>
				<dt class="text-gray-500">{$_('session.phaseTwo.cv')}</dt>
				<dd class="text-right">{formatFixed(aggregate.expectedCv, loc, 2)}</dd>
			</dl>
		</div>
	{/if}

	{#if store.iEstimate}
		<div class="space-y-3">
			<h3 class="text-sm font-semibold uppercase tracking-wide text-gray-500">
				{$_('session.phaseTwo.revise')}
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
					onclick={submitRevision}
					disabled={busy}
					data-testid="revise-submit"
					class="px-4 py-2 text-sm border border-brand-green text-brand-green rounded hover:bg-brand-green/10 disabled:opacity-50"
				>
					{$_('session.phaseTwo.reviseSubmit')}
				</button>
				{#if !store.isModerator}
					<button
						type="button"
						onclick={toggleAgree}
						disabled={busy || store.myParticipant?.agreed}
						class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45] disabled:opacity-50"
					>
						{$_('session.phaseTwo.agree')}
					</button>
					{#if store.myParticipant?.agreed}
						<span class="text-sm text-green-600">✓ {$_('session.phaseTwo.agreed')}</span>
					{/if}
				{/if}
			</div>
		</div>
	{/if}

	{#if store.isModerator}
		<div class="flex items-center gap-3">
			<span class="text-sm {allAgreed ? 'text-green-600' : 'text-gray-500'}">
				{allAgreed
					? $_('session.phaseTwo.allAgreed')
					: $_('session.phaseTwo.notAllAgreed', {
							values: { count: agreedCount, total: estimators.length }
						})}
			</span>
			<button
				type="button"
				onclick={finalizeItem}
				disabled={busy}
				class="ml-auto px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45] disabled:opacity-50"
			>
				{$_('session.phaseTwo.finalize')}
			</button>
		</div>
	{/if}
</div>
