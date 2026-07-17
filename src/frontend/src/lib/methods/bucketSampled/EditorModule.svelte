<script lang="ts">
	import TreeTable from '$lib/components/treetable/TreeTable.svelte';
	import type { TreeColumn, TreeNodeContext } from '$lib/components/treetable/types';
	import ParametersPanel from '$lib/components/ParametersPanel.svelte';
	import EffortDriversPanel from '$lib/components/EffortDriversPanel.svelte';
	import PhasesPanel from '$lib/components/PhasesPanel.svelte';
	import AdditionalCostsPanel from '$lib/components/AdditionalCostsPanel.svelte';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import BucketPanel from './BucketPanel.svelte';
	import { newId, type Node, type Leaf, type NodePath, type CalcEntry } from '$lib/estimationNodes';
	import type { ApiAdditionalCost } from '$lib/api/types.js';
	import { log } from '$lib/log';

	type Bucket = { id: string; position: number; label: string };

	// Bucket + sampled editor (task-104). Same $bindable ownership as the PERT
	// module: the route owns the state; this is the pure view. Bucketed leaves
	// live at the root (buckets are the grouping, so no nested tree here); the
	// domain reducer (task-102) derives each non-sample leaf's mean from its
	// bucket's samples — no bucket math in the frontend.
	let {
		roots = $bindable<Node[]>([]),
		parameters = $bindable(),
		effortDrivers = $bindable(),
		phases = $bindable(),
		additionalCosts = $bindable(),
		buckets = $bindable<Bucket[]>([]),
		calcMap = new Map<string, CalcEntry>(),
		editable
	}: {
		roots: Node[];
		parameters: any[];
		effortDrivers: any[];
		phases: any[];
		additionalCosts: ApiAdditionalCost[];
		buckets: Bucket[];
		calcMap: Map<string, CalcEntry>;
		editable: boolean;
	} = $props();

	$effect(() => {
		log.debug('Bucket+sampled editor module mounted');
	});

	function getId(n: Node): string {
		return n.logicalId;
	}
	// Flat list: buckets provide the grouping, so leaves have no children.
	function getChildren(_n: Node): Node[] | null {
		return null;
	}

	function makeBucketedLeaf(): Leaf {
		return {
			logicalId: newId(),
			type: 'BUCKETED',
			description: '',
			minEffort: null,
			expectedEffort: null,
			maxEffort: null,
			assumptions: null,
			phaseAbbreviation: null,
			unit: null,
			bucketId: buckets[0]?.id ?? null,
			isSample: false
		};
	}

	function addItem() {
		roots = [...roots, makeBucketedLeaf()];
	}

	function deleteAt(path: NodePath) {
		const idx = path[path.length - 1];
		roots = roots.filter((_, i) => i !== idx);
	}

	// Data-integrity guard: a leaf assigned to a bucket that no longer exists.
	const bucketIds = $derived(new Set(buckets.map((b) => b.id)));
	const unknownLeaves = $derived(
		roots.filter((n): n is Leaf => n.type !== 'GROUP' && n.bucketId != null && !bucketIds.has(n.bucketId))
	);
	let bannerDismissed = $state(false);
	$effect(() => {
		if (unknownLeaves.length > 0) {
			bannerDismissed = false;
			log.error(
				`Bucketed leaves reference unknown buckets: ${unknownLeaves.map((l) => l.bucketId).join(', ')}`
			);
		}
	});

	const columns: TreeColumn<Node>[] = [
		{ key: 'description', header: 'Description', width: '1fr', cell: descriptionCell },
		{ key: 'bucket', header: 'Bucket', width: '8rem', cell: bucketCell },
		{ key: 'sample', header: 'Sample', width: '5rem', align: 'center', cell: sampleCell },
		{ key: 'optimistic', header: 'Optimistic', width: '6rem', align: 'right', cell: optimisticCell },
		{ key: 'likely', header: 'Likely', width: '6rem', align: 'right', cell: likelyCell },
		{ key: 'pessimistic', header: 'Pessimistic', width: '6rem', align: 'right', cell: pessimisticCell },
		{ key: 'mean', header: 'Mean', width: '6rem', align: 'right', cell: meanCell },
		{ key: 'offerPT', header: 'offerPT (PT)', width: '6rem', align: 'right', cell: offerPTCell },
		{ key: 'cost', header: 'Cost (EUR)', width: '7rem', align: 'right', cell: costCell, collapsible: true },
		{
			key: 'offerPrice',
			header: 'Offer Price (EUR)',
			width: '7rem',
			align: 'right',
			cell: offerPriceCell,
			collapsible: true
		}
	];
</script>

{#snippet descriptionCell(node: Node)}
	{#if node.type !== 'GROUP'}
		{#if editable}
			<input
				type="text"
				class="w-full min-w-0 bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
				value={node.description}
				oninput={(e) => (node.description = e.currentTarget.value)}
			/>
		{:else}
			<span class="px-1">{node.description}</span>
		{/if}
	{/if}
{/snippet}

{#snippet bucketCell(node: Node)}
	{#if node.type !== 'GROUP'}
		{#if editable}
			<select
				class="w-full bg-transparent text-sm focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
				class:text-red-500={node.bucketId != null && !bucketIds.has(node.bucketId)}
				value={node.bucketId ?? ''}
				onchange={(e) => (node.bucketId = e.currentTarget.value === '' ? null : e.currentTarget.value)}
			>
				<option value="">— none —</option>
				{#each buckets as b (b.id)}
					<option value={b.id}>{b.label}</option>
				{/each}
			</select>
		{:else}
			<span class="px-1 text-sm text-gray-600">{buckets.find((b) => b.id === node.bucketId)?.label ?? ''}</span>
		{/if}
	{/if}
{/snippet}

{#snippet sampleCell(node: Node)}
	{#if node.type !== 'GROUP'}
		<input
			type="checkbox"
			class="accent-brand-green"
			checked={node.isSample ?? false}
			disabled={!editable}
			aria-label="Als Stichprobe schätzen"
			onchange={(e) => (node.isSample = e.currentTarget.checked)}
		/>
	{/if}
{/snippet}

{#snippet numberCell(node: Leaf, field: 'minEffort' | 'expectedEffort' | 'maxEffort')}
	{#if node.isSample}
		{#if editable}
			<input
				type="number"
				step="0.1"
				min="0"
				class="w-full text-right bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
				value={node[field] ?? ''}
				oninput={(e) => (node[field] = e.currentTarget.value === '' ? null : parseFloat(e.currentTarget.value))}
			/>
		{:else}
			<span class="tabular-nums">{node[field] ?? ''}</span>
		{/if}
	{:else}
		<span class="text-gray-300">—</span>
	{/if}
{/snippet}

{#snippet optimisticCell(node: Node)}
	{#if node.type !== 'GROUP'}{@render numberCell(node, 'minEffort')}{/if}
{/snippet}
{#snippet likelyCell(node: Node)}
	{#if node.type !== 'GROUP'}{@render numberCell(node, 'expectedEffort')}{/if}
{/snippet}
{#snippet pessimisticCell(node: Node)}
	{#if node.type !== 'GROUP'}{@render numberCell(node, 'maxEffort')}{/if}
{/snippet}

{#snippet meanCell(node: Node)}
	{@const calc = calcMap.get(node.logicalId)}
	{#if node.type !== 'GROUP'}
		<!-- Derived: PERT of the sample triple, or the bucket average for a
			non-sample. Always read-only (the domain owns the math). -->
		<span class="text-brand-green tabular-nums" class:italic={!node.isSample}>
			{calc != null ? calc.mean.toFixed(2) : '—'}
		</span>
	{/if}
{/snippet}

{#snippet offerPTCell(node: Node)}
	{@const calc = calcMap.get(node.logicalId)}
	<span class="text-gray-600 tabular-nums">{calc != null ? calc.offerPT.toFixed(2) : '—'}</span>
{/snippet}
{#snippet costCell(node: Node)}
	{@const calc = calcMap.get(node.logicalId)}
	<span class="text-gray-600 tabular-nums">{calc != null ? calc.cost.toFixed(0) : '—'}</span>
{/snippet}
{#snippet offerPriceCell(node: Node)}
	{@const calc = calcMap.get(node.logicalId)}
	<span class="text-gray-600 tabular-nums">{calc != null ? calc.offerPrice.toFixed(0) : '—'}</span>
{/snippet}

{#snippet rowActionsSnippet(_node: Node, ctx: TreeNodeContext<Node>)}
	<button
		type="button"
		onclick={() => deleteAt(ctx.path)}
		class="text-gray-300 hover:text-red-500 transition-colors leading-none"
		title="Delete"
		aria-label="Delete row">✕</button
	>
{/snippet}

<ParametersPanel bind:parameters {editable} />

<EffortDriversPanel bind:effortDrivers {editable} />

<PhasesPanel bind:phases {roots} {calcMap} {editable} />

<AdditionalCostsPanel bind:costs={additionalCosts} {phases} {editable} />

<BucketPanel bind:buckets {editable} />

{#if unknownLeaves.length > 0 && !bannerDismissed}
	<ErrorBanner
		message={`${unknownLeaves.length} Item(s) verweisen auf einen gelöschten Bucket — bitte neu zuordnen.`}
		ondismiss={() => (bannerDismissed = true)}
	/>
{/if}

<div class="border rounded-lg overflow-hidden" data-undo-aware="true">
	{#if roots.length === 0}
		<div class="p-10 text-center text-gray-400">
			<p class="mb-4 text-sm">No items yet.</p>
			{#if editable}
				<button
					type="button"
					onclick={addItem}
					class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]">Add item</button
				>
			{/if}
		</div>
	{:else}
		<TreeTable
			bind:roots
			{columns}
			{getId}
			{getChildren}
			treeColumnKey="description"
			{editable}
			rowActions={rowActionsSnippet}
			actionsPlacement="treeColumn"
		/>
		{#if editable}
			<div class="p-3 border-t bg-gray-50/40">
				<button
					type="button"
					onclick={addItem}
					class="text-sm text-brand-green hover:text-[#007a45]">+ Add item</button
				>
			</div>
		{/if}
	{/if}
</div>
