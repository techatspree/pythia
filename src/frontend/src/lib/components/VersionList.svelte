<script lang="ts">
	import { resolve } from '$app/paths';
	import { _ } from 'svelte-i18n';
	import type { ApiVersionSummary } from '$lib/api/types.js';
	let { versions, estimationId, oncreate, onsubmit }: { versions: ApiVersionSummary[]; estimationId: string; oncreate: () => void; onsubmit: () => void } = $props();

	let compareSelection = $state<string[]>([]);

	function statusLabel(version: ApiVersionSummary): string {
		return version.isDraft ? $_('version.statusDraft') : $_('version.statusSubmitted');
	}

	function statusColor(version: ApiVersionSummary): string {
		return version.isDraft ? 'bg-yellow-100 text-yellow-800' : 'bg-brand-green/20 text-brand-green';
	}

	const refOf = (v: ApiVersionSummary) => (v.isDraft ? 'draft' : String(v.versionNumber));

	function toggleCompare(ref: string) {
		if (compareSelection.includes(ref)) {
			compareSelection = compareSelection.filter((x) => x !== ref);
		} else if (compareSelection.length < 2) {
			compareSelection = [...compareSelection, ref];
		} else {
			compareSelection = [compareSelection[1], ref];
		}
	}

	function sortRefs(refs: string[]): string[] {
		return [...refs].sort((x, y) => {
			if (x === 'draft') return 1;
			if (y === 'draft') return -1;
			return Number(x) - Number(y);
		});
	}

	const labelOf = (ref: string) => (ref === 'draft' ? 'draft' : `v${ref}`);

	const compareHref = $derived.by(() => {
		if (compareSelection.length !== 2) return '';
		const [a, b] = sortRefs(compareSelection);
		// Route is resolved via resolve(); the dynamic ?a/?b query is appended
		// separately because resolve() can only type-check a literal search suffix.
		return `${resolve('/estimations/[id]/compare', { id: estimationId })}?a=${a}&b=${b}`;
	});

	const compareLabel = $derived.by(() => {
		if (compareSelection.length !== 2) return '';
		const [a, b] = sortRefs(compareSelection);
		return $_('version.compare', { values: { a: labelOf(a), b: labelOf(b) } });
	});
</script>

<div class="mt-6">
	<div class="flex items-center justify-between mb-3">
		<h2 class="text-lg font-semibold">{$_('version.listTitle')}</h2>
		<button onclick={oncreate} class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]">
			{$_('version.createNew')}
		</button>
	</div>

	{#if versions.length === 0}
		<p class="text-gray-500">{$_('version.listEmpty')}</p>
	{:else}
		<div class="space-y-3">
			{#each versions as version (refOf(version))}
				<div class="flex items-stretch border rounded-lg hover:bg-gray-50 transition-colors">
					<label class="flex items-center px-3 cursor-pointer">
						<input
							type="checkbox"
							checked={compareSelection.includes(refOf(version))}
							onclick={(e: MouseEvent) => { e.stopPropagation(); toggleCompare(refOf(version)); }}
							class="w-4 h-4 accent-brand-green"
						/>
					</label>
					<a
						href={version.isDraft
							? resolve('/estimations/[id]/versions/[versionNumber]?draft=true', {
									id: estimationId,
									versionNumber: String(version.versionNumber)
								})
							: resolve('/estimations/[id]/versions/[versionNumber]', {
									id: estimationId,
									versionNumber: String(version.versionNumber)
								})}
						class="flex-1 p-4"
					>
						<div class="flex items-center justify-between">
							<div class="flex items-center gap-3">
								<span class="text-lg font-medium">v{version.versionNumber}</span>
								<span class="px-2 py-0.5 text-xs rounded-full {statusColor(version)}">
									{statusLabel(version)}
								</span>
							</div>
							<div class="flex items-center gap-3">
								{#if version.isDraft}
									<button
										onclick={(e: MouseEvent) => { e.stopPropagation(); onsubmit(); }}
										class="px-3 py-1 text-xs bg-brand-green text-white rounded hover:bg-[#007a45]"
									>
										{$_('version.submit')}
									</button>
								{/if}
								<span class="text-sm text-gray-500">
									{version.createdAt ? new Date(version.createdAt).toLocaleDateString() : ''}
								</span>
							</div>
						</div>
						<div class="mt-1 flex items-center gap-4 text-sm text-gray-600">
							{#if version.totalEffort != null}
								<span>{$_('version.effort', { values: { effort: version.totalEffort } })}</span>
							{/if}
							{#if version.notes}
								<span class="truncate">{version.notes}</span>
							{/if}
						</div>
					</a>
				</div>
			{/each}
		</div>
	{/if}

	{#if compareSelection.length === 2}
		<!-- compareHref resolves the route via resolve(); only the dynamic ?a/?b
		     query is concatenated, which this rule cannot model. -->
		<!-- eslint-disable-next-line svelte/no-navigation-without-resolve -->
		<a href={compareHref} class="mt-3 inline-block px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]">
			{compareLabel}
		</a>
	{/if}
</div>
