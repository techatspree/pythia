<script lang="ts">
	import { page } from '$app/state';
	import { onMount } from 'svelte';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import type { ApiVersionComparison, ApiVersionResponse } from '$lib/api/types.js';

	let comparison = $state<ApiVersionComparison | null>(null);
	let versionA = $state<ApiVersionResponse | null>(null);
	let versionB = $state<ApiVersionResponse | null>(null);
	let loading = $state(true);
	let bannerMessage = $state<string | null>(null);

	let paramA = $derived(page.url.searchParams.get('a') ?? '');
	let paramB = $derived(page.url.searchParams.get('b') ?? '');

	const labelOf = (ref: string) => (ref === 'draft' ? 'draft' : `v${ref}`);
	const labelA = $derived(labelOf(paramA));
	const labelB = $derived(labelOf(paramB));

	const effortDelta = $derived.by(() => {
		if (!versionA || !versionB) return 0;
		return versionB.totalEffort - versionA.totalEffort;
	});

	const itemCountA = $derived.by(() =>
		versionA ? versionA.itemGroups.reduce((s, g) => s + g.items.length, 0) : 0
	);
	const itemCountB = $derived.by(() =>
		versionB ? versionB.itemGroups.reduce((s, g) => s + g.items.length, 0) : 0
	);

	const isEmpty = $derived.by(() =>
		comparison
			? comparison.addedItems.length === 0 &&
			  comparison.removedItems.length === 0 &&
			  comparison.modifiedItems.length === 0 &&
			  comparison.addedGroups.length === 0 &&
			  comparison.removedGroups.length === 0 &&
			  comparison.parameterChanges.length === 0
			: false
	);

	onMount(async () => {
		loading = true;
		bannerMessage = null;
		try {
			const id = page.params.id;
			const a = page.url.searchParams.get('a');
			const b = page.url.searchParams.get('b');
			if (!a || !b) throw new Error('Missing ?a and ?b parameters');

			const urlA = a === 'draft' ? `/api/estimations/${id}/versions/draft` : `/api/estimations/${id}/versions/${a}`;
			const urlB = b === 'draft' ? `/api/estimations/${id}/versions/draft` : `/api/estimations/${id}/versions/${b}`;
			const [resA, resB, resCmp] = await Promise.all([
				fetch(urlA),
				fetch(urlB),
				fetch(`/api/estimations/${id}/versions/${a}/compare/${b}`)
			]);
			if (!resA.ok) throw new Error(`Failed to load version ${a} (${resA.status})`);
			if (!resB.ok) throw new Error(`Failed to load version ${b} (${resB.status})`);
			if (!resCmp.ok) throw new Error(`Failed to load comparison (${resCmp.status})`);

			versionA = await resA.json();
			versionB = await resB.json();
			comparison = await resCmp.json();
		} catch (e: any) {
			bannerMessage = e.message;
		} finally {
			loading = false;
		}
	});

	function fmt(n: number | null | undefined): string {
		return n != null ? n.toFixed(1) : '—';
	}

	function deltaClass(delta: number): string {
		if (delta < 0) return 'text-green-600';
		if (delta > 0) return 'text-red-600';
		return 'text-gray-500';
	}
</script>

<div class="p-6">
	<a href="/estimations/{page.params.id}" class="text-sm text-brand-green hover:underline">
		&larr; Back to estimation
	</a>

	<ErrorBanner message={bannerMessage} ondismiss={() => (bannerMessage = null)} />

	{#if loading}
		<p class="mt-4 text-gray-500">Loading…</p>
	{:else if comparison && versionA && versionB}
		<h1 class="text-2xl font-bold mt-4 mb-4">{labelA} → {labelB}</h1>

		<div class="flex items-center gap-6 mb-6 p-4 bg-gray-50 border rounded-lg text-sm">
			<span>
				Effort:
				<span class="font-medium">{fmt(versionA.totalEffort)} PT</span>
				→
				<span class="font-medium">{fmt(versionB.totalEffort)} PT</span>
				<span class="ml-1 {deltaClass(effortDelta)}">
					({effortDelta >= 0 ? '+' : ''}{fmt(effortDelta)} PT)
				</span>
			</span>
			<span>
				Items:
				<span class="font-medium">{itemCountA}</span>
				→
				<span class="font-medium">{itemCountB}</span>
			</span>
		</div>

		{#if isEmpty}
			<p class="text-gray-500">No differences between {labelA} and {labelB}.</p>
		{:else}
			{#if comparison.parameterChanges.length > 0}
				<h2 class="text-lg font-semibold mb-2">Parameters</h2>
				<table class="w-full text-sm border rounded-lg overflow-hidden mb-6">
					<thead class="bg-gray-100 text-left">
						<tr>
							<th class="px-3 py-2">Parameter</th>
							<th class="px-3 py-2">{labelA}</th>
							<th class="px-3 py-2">{labelB}</th>
						</tr>
					</thead>
					<tbody>
						{#each comparison.parameterChanges as pc}
							<tr class="border-t bg-yellow-50">
								<td class="px-3 py-2 font-medium">{pc.name}</td>
								<td class="px-3 py-2">{pc.oldValue ?? '—'}</td>
								<td class="px-3 py-2">{pc.newValue ?? '—'}</td>
							</tr>
						{/each}
					</tbody>
				</table>
			{/if}

			<h2 class="text-lg font-semibold mb-2">Items</h2>
			<table class="w-full text-sm border rounded-lg overflow-hidden">
				<thead class="bg-gray-100 text-left">
					<tr>
						<th class="px-3 py-2">Group</th>
						<th class="px-3 py-2">Item</th>
						<th class="px-3 py-2 text-right">Min</th>
						<th class="px-3 py-2 text-right">Exp</th>
						<th class="px-3 py-2 text-right">Max</th>
						<th class="px-3 py-2 text-right">Offer PT</th>
					</tr>
				</thead>
				<tbody>
					{#each comparison.addedGroups as g}
						<tr class="border-t bg-green-100">
							<td colspan="6" class="px-3 py-2 font-medium text-green-800">+ Group added: {g.title}</td>
						</tr>
					{/each}

					{#each comparison.removedGroups as g}
						<tr class="border-t bg-red-100">
							<td colspan="6" class="px-3 py-2 font-medium text-red-800">− Group removed: {g.title}</td>
						</tr>
					{/each}

					{#each comparison.addedItems as item}
						<tr class="border-t bg-green-50 text-green-800">
							<td class="px-3 py-2">{item.groupTitle ?? '—'}</td>
							<td class="px-3 py-2">{item.description}</td>
							<td class="px-3 py-2 text-right">{fmt(item.minEffort)}</td>
							<td class="px-3 py-2 text-right">{fmt(item.expectedEffort)}</td>
							<td class="px-3 py-2 text-right">{fmt(item.maxEffort)}</td>
							<td class="px-3 py-2 text-right">{fmt(item.offerPT)}</td>
						</tr>
					{/each}

					{#each comparison.removedItems as item}
						<tr class="border-t bg-red-50 text-red-800">
							<td class="px-3 py-2">{item.groupTitle ?? '—'}</td>
							<td class="px-3 py-2">{item.description}</td>
							<td class="px-3 py-2 text-right">{fmt(item.minEffort)}</td>
							<td class="px-3 py-2 text-right">{fmt(item.expectedEffort)}</td>
							<td class="px-3 py-2 text-right">{fmt(item.maxEffort)}</td>
							<td class="px-3 py-2 text-right">{fmt(item.offerPT)}</td>
						</tr>
					{/each}

					{#each comparison.modifiedItems as mod}
						{@const cf = new Set(mod.changedFields)}
						<tr class="border-t bg-yellow-50">
							<td class="px-3 py-2">{mod.after.groupTitle ?? '—'}</td>
							<td class="px-3 py-2 {cf.has('description') ? 'font-semibold bg-yellow-200' : ''}">
								{#if cf.has('description')}
									{mod.before.description} → {mod.after.description}
								{:else}
									{mod.after.description}
								{/if}
							</td>
							<td class="px-3 py-2 text-right {cf.has('minEffort') ? 'font-semibold bg-yellow-200' : ''}">
								{#if cf.has('minEffort')}
									{fmt(mod.before.minEffort)} → {fmt(mod.after.minEffort)}
								{:else}
									{fmt(mod.after.minEffort)}
								{/if}
							</td>
							<td class="px-3 py-2 text-right {cf.has('expectedEffort') ? 'font-semibold bg-yellow-200' : ''}">
								{#if cf.has('expectedEffort')}
									{fmt(mod.before.expectedEffort)} → {fmt(mod.after.expectedEffort)}
								{:else}
									{fmt(mod.after.expectedEffort)}
								{/if}
							</td>
							<td class="px-3 py-2 text-right {cf.has('maxEffort') ? 'font-semibold bg-yellow-200' : ''}">
								{#if cf.has('maxEffort')}
									{fmt(mod.before.maxEffort)} → {fmt(mod.after.maxEffort)}
								{:else}
									{fmt(mod.after.maxEffort)}
								{/if}
							</td>
							<td class="px-3 py-2 text-right {cf.has('offerPT') ? 'font-semibold bg-yellow-200' : ''}">
								{#if cf.has('offerPT')}
									{fmt(mod.before.offerPT)} → {fmt(mod.after.offerPT)}
								{:else}
									{fmt(mod.after.offerPT)}
								{/if}
							</td>
						</tr>
					{/each}
				</tbody>
			</table>
		{/if}
	{/if}
</div>
