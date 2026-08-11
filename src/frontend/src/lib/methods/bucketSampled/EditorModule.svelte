<script lang="ts">
	import Button from '$lib/ui/Button.svelte';
	import type { Snippet } from 'svelte';
	import { _, locale } from 'svelte-i18n';
	import { formatFixed, DEFAULT_LOCALE } from '$lib/format';
	import TreeTable from '$lib/components/treetable/TreeTable.svelte';
	import type {
		ChildrenChangeEvent,
		TreeColumn,
		TreeNodeContext
	} from '$lib/components/treetable/types';

	const num = (v: number, frac: number) => formatFixed(v, $locale ?? DEFAULT_LOCALE, frac);
	import ParametersPanel from '$lib/components/ParametersPanel.svelte';
	import EffortDriversPanel from '$lib/components/EffortDriversPanel.svelte';
	import PhasesPanel from '$lib/components/PhasesPanel.svelte';
	import AdditionalCostsPanel from '$lib/components/AdditionalCostsPanel.svelte';
	import ErrorBanner from '$lib/components/ErrorBanner.svelte';
	import BucketPanel from './BucketPanel.svelte';
	import {
		newId,
		type Node,
		type Group,
		type Leaf,
		type NodePath,
		type CalcEntry
	} from '$lib/estimationNodes';
	import type { ApiAdditionalCost } from '$lib/api/types.js';
	import type { EstimationTotalsView } from '$lib/adapter';
	import { log } from '$lib/log';

	type Bucket = { id: string; position: number; label: string };

	// A synthetic row standing for one bucket in the bucket view (task-132). It
	// is NOT a domain node: it has no model logicalId, so it carries a synthetic
	// one (`bucket:<id>`) purely to satisfy TreeTable's getId, and its numbers
	// are summed from its leaves' calc entries rather than read from calcMap.
	type BucketRow = {
		__bucketRow: true;
		logicalId: string;
		bucketId: string | null;
		label: string;
		children: Leaf[];
	};
	type BucketViewNode = BucketRow | Leaf;

	// Bucket + sampled editor (task-104, two views since task-132). Same
	// $bindable ownership as the PERT module: the route owns the state; this is
	// the pure view. Two projections of ONE model (`roots`): the bucket view
	// groups the flattened leaves by bucket (drag = re-bucket), the hierarchy
	// view is the real nested-group tree (drag = restructure). The domain
	// reducer (task-102) derives each non-sample leaf's mean from its bucket's
	// samples — no bucket math in the frontend.
	let {
		roots = $bindable<Node[]>([]),
		dailyRate = $bindable<number>(800),
		stdDevFactor = $bindable<number>(2.0),
		salesSurcharge = $bindable<number>(0.1),
		effortDrivers = $bindable(),
		phases = $bindable(),
		additionalCosts = $bindable(),
		buckets = $bindable<Bucket[]>([]),
		calcMap = new Map<string, CalcEntry>(),
		// The whole-estimation totals are rendered by the route's summary panel
		// above this module, not here; declared only so the route can pass the
		// same prop set to every method module.
		totals: _totals = undefined,
		editable
	}: {
		roots: Node[];
		dailyRate: number;
		stdDevFactor: number;
		salesSurcharge: number;
		effortDrivers: any[];
		phases: any[];
		additionalCosts: ApiAdditionalCost[];
		buckets: Bucket[];
		calcMap: Map<string, CalcEntry>;
		totals?: EstimationTotalsView;
		editable: boolean;
	} = $props();

	$effect(() => {
		log.debug('Bucket+sampled editor module mounted');
	});

	// ── View toggle ─────────────────────────────────────────────────────────
	const VIEW_STORAGE_KEY = 'estimator.bucketView';

	function storedView(): 'bucket' | 'hierarchy' {
		if (typeof localStorage === 'undefined') return 'bucket';
		return localStorage.getItem(VIEW_STORAGE_KEY) === 'hierarchy' ? 'hierarchy' : 'bucket';
	}

	let view = $state<'bucket' | 'hierarchy'>(storedView());

	function setView(next: 'bucket' | 'hierarchy') {
		if (view === next) return;
		view = next;
		if (typeof localStorage !== 'undefined') localStorage.setItem(VIEW_STORAGE_KEY, next);
		log.debug(`Bucket editor view switched to ${next}`);
	}

	// ── Model helpers ───────────────────────────────────────────────────────
	function isBucketRow(n: BucketViewNode): n is BucketRow {
		return '__bucketRow' in n;
	}

	function flatLeaves(nodes: Node[]): Leaf[] {
		const out: Leaf[] = [];
		for (const n of nodes) {
			if (n.type === 'GROUP') out.push(...flatLeaves(n.children));
			else out.push(n);
		}
		return out;
	}

	function nodeAt(path: NodePath): Node {
		let current: Node[] = roots;
		let node: Node | undefined;
		for (const idx of path) {
			node = current[idx];
			if (node && node.type === 'GROUP') current = node.children;
		}
		return node!;
	}

	function makeBucketedLeaf(bucketId: string | null = buckets[0]?.id ?? null): Leaf {
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
			bucketId,
			isSample: false
		};
	}

	function addItem() {
		roots = [...roots, makeBucketedLeaf()];
	}

	function addChildGroupAt(path: NodePath) {
		const g = nodeAt(path) as Group;
		g.children = [
			...g.children,
			{ logicalId: newId(), type: 'GROUP', title: '', children: [] }
		];
		roots = [...roots];
	}

	function addChildLeafAt(path: NodePath) {
		const g = nodeAt(path) as Group;
		g.children = [...g.children, makeBucketedLeaf()];
		roots = [...roots];
	}

	function deleteAt(path: NodePath) {
		if (path.length === 0) return;
		const parentPath = path.slice(0, -1);
		const idx = path[path.length - 1];
		if (parentPath.length === 0) {
			roots = roots.filter((_, i) => i !== idx);
		} else {
			const g = nodeAt(parentPath) as Group;
			g.children = g.children.filter((_, i) => i !== idx);
			roots = [...roots];
		}
	}

	// Data-integrity guard: a leaf assigned to a bucket that no longer exists.
	const bucketIds = $derived(new Set(buckets.map((b) => b.id)));
	const leaves = $derived(flatLeaves(roots));
	const unknownLeaves = $derived(
		leaves.filter((l) => l.bucketId != null && !bucketIds.has(l.bucketId))
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

	// ── Bucket view projection (derived, never stored) ───────────────────────
	// One row per bucket in position order, plus a trailing row for leaves with
	// no bucket (or a bucket that was deleted). Rows are rendered even when
	// empty so every bucket stays a valid drop target.
	// Bumped when a drop is rejected. TreeTable commits a drop into its own copy
	// of the tree before we see it, so a drop we decline would otherwise stay on
	// screen: nothing the projection depends on changed, hence no re-derive.
	// Touching this forces the rebuild that snaps the row back.
	let projectionNonce = $state(0);

	const bucketRows = $derived.by<BucketViewNode[]>(() => {
		void projectionNonce;
		return [
			...buckets.map((b) => ({
				__bucketRow: true as const,
				logicalId: `bucket:${b.id}`,
				bucketId: b.id,
				label: b.label,
				children: leaves.filter((l) => l.bucketId === b.id)
			})),
			{
				__bucketRow: true as const,
				logicalId: 'bucket:unassigned',
				bucketId: null,
				label: $_('bucket.unassigned'),
				children: leaves.filter((l) => l.bucketId == null || !bucketIds.has(l.bucketId))
			}
		];
	});

	function bucketTotals(row: BucketRow): CalcEntry {
		let mean = 0;
		let offerPT = 0;
		let cost = 0;
		let offerPrice = 0;
		for (const leaf of row.children) {
			const c = calcMap.get(leaf.logicalId);
			if (c == null) continue;
			mean += c.mean;
			offerPT += c.offerPT;
			cost += c.cost;
			offerPrice += c.offerPrice;
		}
		return { mean, offerPT, cost, offerPrice };
	}

	// A drop into a bucket row's zone means ONE thing: reassign the dragged
	// leaves' bucket. It never moves anything in the real tree — the bucket view
	// is a projection, and ordering inside a bucket is not persisted (a
	// same-bucket reorder is deliberately a no-op on the model).
	function onBucketChildrenChange(e: ChildrenChangeEvent<BucketViewNode>) {
		if (e.phase !== 'finalize') return;
		// Anything other than a bucket row's own zone (e.g. the root zone, which
		// the cursor hits when hovering a bucket row's header line) carries no
		// meaning here — rebuild so the dropped row snaps back.
		if (e.parentPath.length !== 1) {
			projectionNonce++;
			return;
		}
		const row = bucketRows[e.parentPath[0]];
		if (row == null || !isBucketRow(row)) {
			projectionNonce++;
			return;
		}
		const target = row.bucketId;
		// The unassigned row is a rescue SOURCE, not a drop target: the backend
		// rejects a BUCKETED leaf with a null bucketId (400), so accepting the drop
		// would only produce a failing autosave.
		if (target == null) {
			projectionNonce++;
			return;
		}
		let changed = 0;
		for (const child of e.newChildren) {
			if (isBucketRow(child)) continue;
			const leaf = leaves.find((l) => l.logicalId === child.logicalId);
			if (leaf == null || leaf.bucketId === target) continue;
			leaf.bucketId = target;
			changed++;
			log.debug(`Bucket view: leaf ${leaf.logicalId} reassigned to bucket ${target}`);
		}
		// Nothing moved between buckets (a reorder inside one bucket, which is not
		// persisted): rebuild the projection so the rows return to tree order.
		if (changed === 0) projectionNonce++;
	}

	// ── Columns ─────────────────────────────────────────────────────────────
	// The two views need separate arrays (TreeColumn.cell is a Snippet over the
	// node type, and `roots` is $bindable hence invariant), but the metadata and
	// the per-leaf rendering are defined exactly once: each view's cells branch
	// on their own row type and then delegate to the shared Leaf snippets.
	const columnMeta = [
		{ key: 'description', header: $_('bucket.colDescription'), width: '1fr' },
		{ key: 'bucket', header: $_('bucket.colBucket'), width: '8rem' },
		{ key: 'sample', header: $_('bucket.colSample'), width: '5rem', align: 'center' as const },
		{ key: 'optimistic', header: $_('bucket.colOptimistic'), width: '6rem', align: 'right' as const },
		{ key: 'likely', header: $_('bucket.colLikely'), width: '6rem', align: 'right' as const },
		{
			key: 'pessimistic',
			header: $_('bucket.colPessimistic'),
			width: '6rem',
			align: 'right' as const
		},
		{ key: 'mean', header: $_('bucket.colMean'), width: '6rem', align: 'right' as const },
		{ key: 'offerPT', header: $_('bucket.colOfferPT'), width: '6rem', align: 'right' as const },
		{
			key: 'cost',
			header: $_('bucket.colCost'),
			width: '8rem',
			align: 'right' as const,
			collapsible: true
		},
		{
			key: 'offerPrice',
			header: $_('bucket.colOfferPrice'),
			width: '8rem',
			align: 'right' as const,
			collapsible: true
		}
	];

	function columnsFor<T>(cells: Record<string, Snippet<[T, TreeNodeContext<T>]>>): TreeColumn<T>[] {
		return columnMeta.map((m) => ({ ...m, cell: cells[m.key] }));
	}

	function hierarchyGetChildren(n: Node): Node[] | null {
		return n.type === 'GROUP' ? n.children : null;
	}

	function bucketGetChildren(n: BucketViewNode): BucketViewNode[] | null {
		return isBucketRow(n) ? n.children : null;
	}

	function getId(n: Node | BucketViewNode): string {
		return n.logicalId;
	}
</script>

<!-- ── Shared per-leaf cells (rendered by both views) ───────────────────── -->

{#snippet leafDescription(node: Leaf)}
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
{/snippet}

{#snippet leafBucket(node: Leaf)}
	{#if editable}
		<select
			class="w-full bg-transparent text-sm focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
			class:text-red-500={node.bucketId != null && !bucketIds.has(node.bucketId)}
			value={node.bucketId ?? ''}
			onchange={(e) => (node.bucketId = e.currentTarget.value === '' ? null : e.currentTarget.value)}
		>
			<option value="">{$_('bucket.phaseNone')}</option>
			{#each buckets as b (b.id)}
				<option value={b.id}>{b.label}</option>
			{/each}
		</select>
	{:else}
		<span class="px-1 text-sm text-gray-600"
			>{buckets.find((b) => b.id === node.bucketId)?.label ?? ''}</span
		>
	{/if}
{/snippet}

{#snippet leafSample(node: Leaf)}
	<input
		type="checkbox"
		class="accent-brand-green"
		checked={node.isSample ?? false}
		disabled={!editable}
		aria-label={$_('bucket.sampleAria')}
		onchange={(e) => (node.isSample = e.currentTarget.checked)}
	/>
{/snippet}

{#snippet leafNumber(node: Leaf, field: 'minEffort' | 'expectedEffort' | 'maxEffort')}
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

{#snippet leafMean(node: Leaf)}
	{@const calc = calcMap.get(node.logicalId)}
	<!-- Derived: PERT of the sample triple, or the bucket average for a
		non-sample. Always read-only (the domain owns the math). -->
	<span class="text-brand-green tabular-nums" class:italic={!node.isSample}>
		{calc != null ? num(calc.mean, 2) : '—'}
	</span>
{/snippet}

{#snippet aggregate(calc: CalcEntry | undefined, field: 'mean' | 'offerPT' | 'cost' | 'offerPrice', frac: number)}
	<span class="text-gray-600 tabular-nums">{calc != null ? num(calc[field], frac) : '—'}</span>
{/snippet}

<!-- ── Hierarchy-view cells ─────────────────────────────────────────────── -->

{#snippet hDescription(node: Node)}
	{#if node.type === 'GROUP'}
		{#if editable}
			<input
				type="text"
				class="w-full min-w-0 bg-transparent font-semibold focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
				placeholder={$_('bucket.groupTitlePlaceholder')}
				value={node.title}
				oninput={(e) => (node.title = e.currentTarget.value)}
			/>
		{:else}
			<span class="font-semibold text-gray-700">{node.title}</span>
		{/if}
		<span class="ml-2 text-xs font-normal text-gray-400">
			{$_('grid.childCount', { values: { count: node.children.length } })}
		</span>
	{:else}
		{@render leafDescription(node)}
	{/if}
{/snippet}

{#snippet hBucket(node: Node)}
	{#if node.type !== 'GROUP'}{@render leafBucket(node)}{/if}
{/snippet}
{#snippet hSample(node: Node)}
	{#if node.type !== 'GROUP'}{@render leafSample(node)}{/if}
{/snippet}
{#snippet hOptimistic(node: Node)}
	{#if node.type !== 'GROUP'}{@render leafNumber(node, 'minEffort')}{/if}
{/snippet}
{#snippet hLikely(node: Node)}
	{#if node.type !== 'GROUP'}{@render leafNumber(node, 'expectedEffort')}{/if}
{/snippet}
{#snippet hPessimistic(node: Node)}
	{#if node.type !== 'GROUP'}{@render leafNumber(node, 'maxEffort')}{/if}
{/snippet}
{#snippet hMean(node: Node)}
	{#if node.type === 'GROUP'}
		{@render aggregate(calcMap.get(node.logicalId), 'mean', 2)}
	{:else}
		{@render leafMean(node)}
	{/if}
{/snippet}
{#snippet hOfferPT(node: Node)}
	{@render aggregate(calcMap.get(node.logicalId), 'offerPT', 2)}
{/snippet}
{#snippet hCost(node: Node)}
	{@render aggregate(calcMap.get(node.logicalId), 'cost', 0)}
{/snippet}
{#snippet hOfferPrice(node: Node)}
	{@render aggregate(calcMap.get(node.logicalId), 'offerPrice', 0)}
{/snippet}

<!-- ── Bucket-view cells ────────────────────────────────────────────────── -->

{#snippet bDescription(node: BucketViewNode)}
	{#if isBucketRow(node)}
		<span class="font-semibold text-gray-700">{node.label}</span>
		<span class="ml-2 text-xs font-normal text-gray-400">
			{$_('grid.childCount', { values: { count: node.children.length } })}
		</span>
	{:else}
		{@render leafDescription(node)}
	{/if}
{/snippet}

{#snippet bBucket(node: BucketViewNode)}
	{#if !isBucketRow(node)}{@render leafBucket(node)}{/if}
{/snippet}
{#snippet bSample(node: BucketViewNode)}
	{#if !isBucketRow(node)}{@render leafSample(node)}{/if}
{/snippet}
{#snippet bOptimistic(node: BucketViewNode)}
	{#if !isBucketRow(node)}{@render leafNumber(node, 'minEffort')}{/if}
{/snippet}
{#snippet bLikely(node: BucketViewNode)}
	{#if !isBucketRow(node)}{@render leafNumber(node, 'expectedEffort')}{/if}
{/snippet}
{#snippet bPessimistic(node: BucketViewNode)}
	{#if !isBucketRow(node)}{@render leafNumber(node, 'maxEffort')}{/if}
{/snippet}
{#snippet bMean(node: BucketViewNode)}
	{#if !isBucketRow(node)}{@render leafMean(node)}{/if}
{/snippet}
{#snippet bOfferPT(node: BucketViewNode)}
	{@render aggregate(
		isBucketRow(node) ? bucketTotals(node) : calcMap.get(node.logicalId),
		'offerPT',
		2
	)}
{/snippet}
{#snippet bCost(node: BucketViewNode)}
	{@render aggregate(isBucketRow(node) ? bucketTotals(node) : calcMap.get(node.logicalId), 'cost', 0)}
{/snippet}
{#snippet bOfferPrice(node: BucketViewNode)}
	{@render aggregate(
		isBucketRow(node) ? bucketTotals(node) : calcMap.get(node.logicalId),
		'offerPrice',
		0
	)}
{/snippet}

{#snippet hierarchyActions(node: Node, ctx: TreeNodeContext<Node>)}
	<div class="flex items-center gap-1 shrink-0">
		{#if node.type === 'GROUP'}
			<button
				type="button"
				onclick={() => addChildGroupAt(ctx.path)}
				class="text-xs text-brand-green hover:text-brand-green-hover"
				title={$_('grid.actionAddChildGroupTitle')}>{$_('grid.actionAddChildGroup')}</button
			>
			<button
				type="button"
				onclick={() => addChildLeafAt(ctx.path)}
				class="text-xs text-brand-green hover:text-brand-green-hover"
				title={$_('grid.actionAddChildItemTitle')}>{$_('grid.actionAddChildItem')}</button
			>
		{/if}
		<button
			type="button"
			onclick={() => deleteAt(ctx.path)}
			class="text-gray-300 hover:text-red-500 transition-colors leading-none"
			title={$_('common.delete')}
			aria-label={$_('grid.actionDeleteRow')}>✕</button
		>
	</div>
{/snippet}

<ParametersPanel bind:dailyRate bind:stdDevFactor bind:salesSurcharge {editable} />

<EffortDriversPanel bind:effortDrivers {editable} />

<PhasesPanel bind:phases {roots} {calcMap} {editable} />

<AdditionalCostsPanel bind:costs={additionalCosts} {phases} {editable} />

<BucketPanel bind:buckets {editable} />

{#if unknownLeaves.length > 0 && !bannerDismissed}
	<ErrorBanner
		message={$_('bucket.unknownBanner', { values: { count: unknownLeaves.length } })}
		ondismiss={() => (bannerDismissed = true)}
	/>
{/if}

<div class="flex items-center gap-2 mb-2" role="group" aria-label={$_('bucket.viewAria')}>
	<button
		type="button"
		onclick={() => setView('bucket')}
		aria-pressed={view === 'bucket'}
		data-testid="bucket-view-toggle-bucket"
		class="px-3 py-1 text-sm rounded border transition-colors"
		class:bg-brand-green={view === 'bucket'}
		class:text-white={view === 'bucket'}
		class:border-brand-green={view === 'bucket'}
		class:border-gray-200={view !== 'bucket'}
		class:text-gray-600={view !== 'bucket'}>{$_('bucket.viewBucket')}</button
	>
	<button
		type="button"
		onclick={() => setView('hierarchy')}
		aria-pressed={view === 'hierarchy'}
		data-testid="bucket-view-toggle-hierarchy"
		class="px-3 py-1 text-sm rounded border transition-colors"
		class:bg-brand-green={view === 'hierarchy'}
		class:text-white={view === 'hierarchy'}
		class:border-brand-green={view === 'hierarchy'}
		class:border-gray-200={view !== 'hierarchy'}
		class:text-gray-600={view !== 'hierarchy'}>{$_('bucket.viewHierarchy')}</button
	>
</div>

<div class="border rounded-lg overflow-hidden" data-undo-aware="true">
	{#if view === 'hierarchy' && roots.length === 0}
		<div class="p-10 text-center text-gray-400">
			<p class="mb-4 text-sm">{$_('bucket.itemsEmpty')}</p>
			{#if editable}
				<Button
				
					onclick={addItem}
				
					>{$_('bucket.addItem')}</Button>
			{/if}
		</div>
	{:else if view === 'hierarchy'}
		<TreeTable
			bind:roots
			columns={columnsFor<Node>({
				description: hDescription,
				bucket: hBucket,
				sample: hSample,
				optimistic: hOptimistic,
				likely: hLikely,
				pessimistic: hPessimistic,
				mean: hMean,
				offerPT: hOfferPT,
				cost: hCost,
				offerPrice: hOfferPrice
			})}
			{getId}
			getChildren={hierarchyGetChildren}
			treeColumnKey="description"
			{editable}
			rowActions={hierarchyActions}
			actionsPlacement="treeColumn"
		/>
		{#if editable}
			<div class="p-3 border-t bg-gray-50/40">
				<button type="button" onclick={addItem} class="text-sm text-brand-green hover:text-brand-green-hover"
					>{$_('bucket.addItemRow')}</button
				>
			</div>
		{/if}
	{:else}
		<!-- `defaultCollapsed={isBucketRow}`: every bucket row starts collapsed, so
		     all buckets are visible at once and a cross-bucket drag stays a short
		     movement instead of a drag-while-scrolling past a bucket's whole
		     contents. A rule rather than a seed, so a bucket added later starts
		     collapsed too; the user's own expand/collapse always wins. -->
		<TreeTable
			roots={bucketRows}
			columns={columnsFor<BucketViewNode>({
				description: bDescription,
				bucket: bBucket,
				sample: bSample,
				optimistic: bOptimistic,
				likely: bLikely,
				pessimistic: bPessimistic,
				mean: bMean,
				offerPT: bOfferPT,
				cost: bCost,
				offerPrice: bOfferPrice
			})}
			{getId}
			getChildren={bucketGetChildren}
			treeColumnKey="description"
			{editable}
			defaultCollapsed={isBucketRow}
			onChildrenChange={onBucketChildrenChange}
		/>
		{#if editable}
			<div class="p-3 border-t bg-gray-50/40">
				<button type="button" onclick={addItem} class="text-sm text-brand-green hover:text-brand-green-hover"
					>{$_('bucket.addItemRow')}</button
				>
			</div>
		{/if}
	{/if}
</div>
