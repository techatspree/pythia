<script lang="ts">
	import type { ApiVersionSummary } from '$lib/api/types.js';
	let { versions, estimationId, oncreate, onsubmit }: { versions: ApiVersionSummary[]; estimationId: string; oncreate: () => void; onsubmit: () => void } = $props();

	let compareSelection = $state<string[]>([]);

	function statusLabel(version: ApiVersionSummary): string {
		return version.isDraft ? 'DRAFT' : 'SUBMITTED';
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
		return `/estimations/${estimationId}/compare?a=${a}&b=${b}`;
	});

	const compareLabel = $derived.by(() => {
		if (compareSelection.length !== 2) return '';
		const [a, b] = sortRefs(compareSelection);
		return `Compare ${labelOf(a)} ↔ ${labelOf(b)}`;
	});
</script>

<div class="mt-6">
	<div class="flex items-center justify-between mb-3">
		<h2 class="text-lg font-semibold">Versions</h2>
		<button onclick={oncreate} class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]">
			Create new version
		</button>
	</div>

	{#if versions.length === 0}
		<p class="text-gray-500">No versions yet. Create one to get started.</p>
	{:else}
		<div class="space-y-3">
			{#each versions as version}
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
						href="/estimations/{estimationId}/versions/{version.versionNumber}{version.isDraft ? '?draft=true' : ''}"
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
										Submit
									</button>
								{/if}
								<span class="text-sm text-gray-500">
									{version.createdAt ? new Date(version.createdAt).toLocaleDateString() : ''}
								</span>
							</div>
						</div>
						<div class="mt-1 flex items-center gap-4 text-sm text-gray-600">
							{#if version.totalEffort != null}
								<span>Effort: {version.totalEffort} PT</span>
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
		<a
			href={compareHref}
			class="mt-3 inline-block px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]"
		>
			{compareLabel}
		</a>
	{/if}
</div>
