<script lang="ts">
	import { _, locale } from 'svelte-i18n';
	import { resolve } from '$app/paths';
	import { formatFixed, DEFAULT_LOCALE } from '$lib/format';
	import type { SessionStore } from '$lib/session/store.svelte';

	// FINALIZED view (task-067): every item with its written-back final PERT
	// triple, plus a link back to the estimation (its draft now carries these
	// values — the finalize path wrote them through the draft-update endpoint).
	let { store }: { store: SessionStore } = $props();

	const loc = $derived($locale ?? DEFAULT_LOCALE);
	const items = $derived(store.session?.items ?? []);
	const estimationId = $derived(store.session?.estimationId ?? '');
</script>

<div class="space-y-5">
	<h2 class="text-xl font-bold">{$_('session.summary.title')}</h2>

	<div class="overflow-x-auto">
		<table class="w-full text-sm border rounded">
			<thead class="bg-gray-50 text-xs uppercase tracking-wide text-gray-500">
				<tr>
					<th class="text-left px-3 py-2">{$_('session.summary.item')}</th>
					<th class="text-right px-3 py-2">{$_('session.summary.finalEstimate')}</th>
				</tr>
			</thead>
			<tbody>
				{#each items as item (item.nodeLogicalId)}
					<tr class="border-t">
						<td class="px-3 py-2">{item.description ?? item.nodeLogicalId}</td>
						<td class="px-3 py-2 text-right font-medium">
							{#if item.finalTriple}
								{formatFixed(item.finalTriple.minEffort, loc, 1)} / {formatFixed(
									item.finalTriple.expectedEffort,
									loc,
									1
								)} / {formatFixed(item.finalTriple.maxEffort, loc, 1)}
							{:else}
								<span class="text-gray-400">{$_('session.summary.noFinal')}</span>
							{/if}
						</td>
					</tr>
				{/each}
			</tbody>
		</table>
	</div>

	{#if estimationId}
		<a
			href={resolve('/estimations/[id]', { id: estimationId })}
			class="inline-block text-brand-green hover:underline"
		>
			{$_('session.summary.backToDraft')} →
		</a>
	{/if}
</div>
