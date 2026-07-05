<script lang="ts">
	import { page } from '$app/state';
	import { resolve } from '$app/paths';
	import { onMount } from 'svelte';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import type { ApiVersionComparison, ApiVersionResponse } from '$lib/api/types.js';
	import { apiFetch } from '$lib/api/fetch';

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

	function countLeaves(nodes: any[]): number {
		let n = 0;
		for (const node of nodes) {
			if (node.type === 'GROUP') {
				n += countLeaves(node.children ?? []);
			} else {
				n += 1;
			}
		}
		return n;
	}

	const itemCountA = $derived.by(() => (versionA ? countLeaves((versionA as any).roots ?? []) : 0));
	const itemCountB = $derived.by(() => (versionB ? countLeaves((versionB as any).roots ?? []) : 0));

	const isEmpty = $derived.by(() =>
		comparison
			? comparison.addedNodes.length === 0 &&
			  comparison.removedNodes.length === 0 &&
			  comparison.modifiedNodes.length === 0 &&
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
				apiFetch(urlA),
				apiFetch(urlB),
				apiFetch(`/api/estimations/${id}/versions/${a}/compare/${b}`)
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

	function pathLabel(path: string[]): string {
		return path.length === 0 ? '—' : path.join(' > ');
	}

	function nodeLabel(node: { type: string; title: string | null; description: string | null }): string {
		return node.type === 'GROUP' ? (node.title ?? '') : (node.description ?? '');
	}
</script>

<div class="p-6">
	<a href={resolve('/estimations/[id]', { id: page.params.id! })} class="text-sm text-brand-green hover:underline">
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
						{#each comparison.parameterChanges as pc (pc.name)}
							<tr class="border-t bg-yellow-50">
								<td class="px-3 py-2 font-medium">{pc.name}</td>
								<td class="px-3 py-2">{pc.oldValue ?? '—'}</td>
								<td class="px-3 py-2">{pc.newValue ?? '—'}</td>
							</tr>
						{/each}
					</tbody>
				</table>
			{/if}

			<h2 class="text-lg font-semibold mb-2">Nodes</h2>
			<table class="w-full text-sm border rounded-lg overflow-hidden">
				<thead class="bg-gray-100 text-left">
					<tr>
						<th class="px-3 py-2">Path</th>
						<th class="px-3 py-2">Type</th>
						<th class="px-3 py-2">Name</th>
						<th class="px-3 py-2 text-right">Min</th>
						<th class="px-3 py-2 text-right">Exp</th>
						<th class="px-3 py-2 text-right">Max</th>
						<th class="px-3 py-2 text-right">Offer PT</th>
					</tr>
				</thead>
				<tbody>
					{#each comparison.addedNodes as node (node.path.join('.'))}
						<tr class="border-t bg-green-50 text-green-800">
							<td class="px-3 py-2">{pathLabel(node.path)}</td>
							<td class="px-3 py-2">{node.type}</td>
							<td class="px-3 py-2">+ {nodeLabel(node)}</td>
							<td class="px-3 py-2 text-right">{fmt(node.minEffort)}</td>
							<td class="px-3 py-2 text-right">{fmt(node.expectedEffort)}</td>
							<td class="px-3 py-2 text-right">{fmt(node.maxEffort)}</td>
							<td class="px-3 py-2 text-right">{fmt(node.offerPT)}</td>
						</tr>
					{/each}

					{#each comparison.removedNodes as node (node.path.join('.'))}
						<tr class="border-t bg-red-50 text-red-800">
							<td class="px-3 py-2">{pathLabel(node.path)}</td>
							<td class="px-3 py-2">{node.type}</td>
							<td class="px-3 py-2">− {nodeLabel(node)}</td>
							<td class="px-3 py-2 text-right">{fmt(node.minEffort)}</td>
							<td class="px-3 py-2 text-right">{fmt(node.expectedEffort)}</td>
							<td class="px-3 py-2 text-right">{fmt(node.maxEffort)}</td>
							<td class="px-3 py-2 text-right">{fmt(node.offerPT)}</td>
						</tr>
					{/each}

					{#each comparison.modifiedNodes as mod (mod.after.path.join('.'))}
						{@const cf = new Set(mod.changedFields)}
						<tr class="border-t bg-yellow-50">
							<td class="px-3 py-2 {cf.has('parent') ? 'font-semibold bg-yellow-200' : ''}">
								{#if cf.has('parent')}
									{pathLabel(mod.before.path)} → {pathLabel(mod.after.path)}
								{:else}
									{pathLabel(mod.after.path)}
								{/if}
							</td>
							<td class="px-3 py-2">{mod.type}</td>
							<td class="px-3 py-2 {cf.has('title') || cf.has('description') ? 'font-semibold bg-yellow-200' : ''}">
								{#if cf.has('title') || cf.has('description')}
									{nodeLabel(mod.before)} → {nodeLabel(mod.after)}
								{:else}
									{nodeLabel(mod.after)}
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
							<td class="px-3 py-2 text-right">
								{fmt(mod.after.offerPT)}
							</td>
						</tr>
					{/each}
				</tbody>
			</table>
		{/if}
	{/if}
</div>
