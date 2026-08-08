<script lang="ts">
	import { _, locale } from 'svelte-i18n';
	import { formatFixed, DEFAULT_LOCALE } from '$lib/format';
	import TreeTable from './treetable/TreeTable.svelte';
	import type { TreeColumn, TreeNodeContext } from './treetable/types';

	const num = (v: number, frac: number) => formatFixed(v, $locale ?? DEFAULT_LOCALE, frac);
	import {
		newId,
		type Node,
		type Leaf,
		type Group,
		type NodePath,
		type CalcEntry
	} from '$lib/estimationNodes';
	import { ZERO_TOTALS, type EstimationTotalsView } from '$lib/adapter';

	let {
		roots = $bindable<Node[]>([]),
		editable,
		calcMap = new Map<string, CalcEntry>(),
		phases = [],
		totals = ZERO_TOTALS
	}: {
		roots?: Node[];
		editable: boolean;
		calcMap?: Map<string, CalcEntry>;
		phases?: any[];
		totals?: EstimationTotalsView;
	} = $props();

	function pert(o: number | null, m: number | null, p: number | null): number {
		return ((o ?? 0) + 4 * (m ?? 0) + (p ?? 0)) / 6;
	}

	function pathKey(p: NodePath): string {
		return p.join('-');
	}

	function getId(n: Node): string {
		return n.logicalId;
	}
	function getChildren(n: Node): Node[] | null {
		return n.type === 'GROUP' ? n.children : null;
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

	function addRootGroup() {
		roots = [
			...roots,
			{
				logicalId: newId(),
				type: 'GROUP',
				title: 'New group',
				children: [
					{
						logicalId: newId(),
						type: 'FIXED',
						description: '',
						minEffort: null,
						expectedEffort: null,
						maxEffort: null,
						assumptions: null,
						phaseAbbreviation: null,
						unit: null
					}
				]
			}
		];
	}

	function addChildGroupAt(path: NodePath) {
		const g = nodeAt(path) as Group;
		g.children = [
			...g.children,
			{ logicalId: newId(), type: 'GROUP', title: 'New group', children: [] }
		];
		roots = [...roots];
	}

	function addChildLeafAt(path: NodePath) {
		const g = nodeAt(path) as Group;
		g.children = [
			...g.children,
			{
				logicalId: newId(),
				type: 'FIXED',
				description: '',
				minEffort: null,
				expectedEffort: null,
				maxEffort: null,
				assumptions: null,
				phaseAbbreviation: null,
				unit: null
			}
		];
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

	function allLeaves(nodes: Node[]): Leaf[] {
		const out: Leaf[] = [];
		for (const n of nodes) {
			if (n.type === 'GROUP') out.push(...allLeaves(n.children));
			else out.push(n);
		}
		return out;
	}

	const leaves = $derived(allLeaves(roots));
	const totalOpt = $derived(leaves.reduce((s, i) => s + (i.minEffort ?? 0), 0));
	const totalLik = $derived(leaves.reduce((s, i) => s + (i.expectedEffort ?? 0), 0));
	const totalPes = $derived(leaves.reduce((s, i) => s + (i.maxEffort ?? 0), 0));
	const totalExp = $derived(
		leaves.reduce((s, i) => s + pert(i.minEffort, i.expectedEffort, i.maxEffort), 0)
	);
	// The money totals come from the domain reducer, NOT from reducing
	// calcMap.values(): that map holds an entry for every node INCLUDING groups,
	// and a group accumulates its whole subtree — so reducing it double-counted
	// every grouped estimation. The triple sums above are leaf-based and correct.
	const totalOfferPT = $derived(totals.offerPT);
	const totalCost = $derived(totals.developmentCost);
	// developmentOfferPrice, not totalOfferPrice: this column shows each leaf's
	// own offer price, so its total must exclude additional costs or the column
	// would not add up.
	const totalOfferPrice = $derived(totals.developmentOfferPrice);

	const editCols = [0, 1, 2, 3, 5];

	function focusCell(p: NodePath, col: number) {
		(document.querySelector(`[data-cell="${pathKey(p)}-${col}"]`) as HTMLElement)?.focus();
	}

	function visibleLeafPathsInDoc(): NodePath[] {
		const inputs = Array.from(document.querySelectorAll('[data-cell$="-0"]')) as HTMLElement[];
		return inputs.map((el) => {
			const key = el.getAttribute('data-cell')!.replace(/-0$/, '');
			return key.split('-').map((s) => parseInt(s, 10));
		});
	}

	function onKeyDown(e: KeyboardEvent, path: NodePath, col: number) {
		const flat = visibleLeafPathsInDoc();
		const fi = flat.findIndex((p) => pathKey(p) === pathKey(path));
		const ci = editCols.indexOf(col);
		const isNumeric = col === 1 || col === 2 || col === 3;

		if (e.key === 'Tab') {
			e.preventDefault();
			if (!e.shiftKey) {
				if (ci < editCols.length - 1) focusCell(path, editCols[ci + 1]);
				else if (fi < flat.length - 1) focusCell(flat[fi + 1], editCols[0]);
			} else {
				if (ci > 0) focusCell(path, editCols[ci - 1]);
				else if (fi > 0) focusCell(flat[fi - 1], editCols[editCols.length - 1]);
			}
		} else if (e.key === 'Enter' || e.key === 'ArrowDown') {
			e.preventDefault();
			if (fi < flat.length - 1) focusCell(flat[fi + 1], col);
		} else if (e.key === 'ArrowUp') {
			e.preventDefault();
			if (fi > 0) focusCell(flat[fi - 1], col);
		} else if (e.key === 'ArrowRight') {
			const input = e.currentTarget as HTMLInputElement;
			const atEnd = isNumeric || input.selectionEnd === input.value.length;
			if (atEnd && ci < editCols.length - 1) {
				e.preventDefault();
				focusCell(path, editCols[ci + 1]);
			}
		} else if (e.key === 'ArrowLeft') {
			const input = e.currentTarget as HTMLInputElement;
			const atStart = isNumeric || input.selectionStart === 0;
			if (atStart && ci > 0) {
				e.preventDefault();
				focusCell(path, editCols[ci - 1]);
			}
		}
	}

	const columns: TreeColumn<Node>[] = [
		{ key: 'description', header: $_('grid.colDescription'), width: '1fr', cell: descriptionCell },
		{ key: 'type', header: $_('grid.colType'), width: '5rem', cell: typeCell },
		{ key: 'phase', header: $_('grid.colPhase'), width: '6rem', cell: phaseCell },
		{ key: 'optimistic', header: $_('grid.colOptimistic'), width: '6rem', align: 'right', cell: optimisticCell },
		{ key: 'likely', header: $_('grid.colLikely'), width: '6rem', align: 'right', cell: likelyCell },
		{
			key: 'pessimistic',
			header: $_('grid.colPessimistic'),
			width: '6rem',
			align: 'right',
			cell: pessimisticCell
		},
		{ key: 'pert', header: $_('grid.colPert'), width: '6rem', align: 'right', cell: pertCell },
		{ key: 'assumptions', header: $_('grid.colAssumptions'), width: '1fr', cell: assumptionsCell },
		{
			key: 'offerPT',
			header: $_('grid.colOfferPT'),
			width: '6rem',
			align: 'right',
			cell: offerPTCell
		},
		{
			key: 'cost',
			header: $_('grid.colCost'),
			width: '8rem',
			align: 'right',
			cell: costCell,
			collapsible: true
		},
		{
			key: 'offerPrice',
			header: $_('grid.colOfferPrice'),
			width: '8rem',
			align: 'right',
			cell: offerPriceCell,
			collapsible: true
		}
	];

	function legacyRowAttrs(_node: Node, ctx: TreeNodeContext<Node>): Record<string, string> {
		return { 'data-testid': `row-${pathKey(ctx.path)}` };
	}

	function legacyZoneAttrs(parent: Node | null): Record<string, string> {
		if (parent == null) return { 'aria-label': $_('grid.ariaRootNodes') };
		if (parent.type === 'GROUP')
			return { 'aria-label': $_('grid.ariaChildrenOf', { values: { title: parent.title ?? '' } }) };
		return { 'aria-label': $_('grid.ariaChildren') };
	}

	const gridTemplateColumns = $derived(
		(editable ? '2rem ' : '') + columns.map((c) => c.width).join(' ')
	);
</script>

{#snippet descriptionCell(node: Node, ctx: TreeNodeContext<Node>)}
	{#if node.type === 'GROUP'}
		{#if editable}
			<input
				type="text"
				class="w-full min-w-0 bg-transparent font-semibold focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
				value={node.title}
				oninput={(e) => {
					node.title = e.currentTarget.value;
				}}
			/>
		{:else}
			<span class="font-semibold text-gray-700">{node.title}</span>
		{/if}
		<span class="ml-2 text-xs font-normal text-gray-400">
			{$_('grid.childCount', { values: { count: node.children.length } })}
		</span>
	{:else if editable}
		<input
			type="text"
			class="w-full min-w-0 bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
			value={node.description}
			data-cell="{pathKey(ctx.path)}-0"
			onkeydown={(e) => onKeyDown(e, ctx.path, 0)}
			oninput={(e) => {
				node.description = e.currentTarget.value;
			}}
		/>
	{:else}
		<span class="px-1">{node.description}</span>
	{/if}
{/snippet}

{#snippet typeCell(node: Node, _ctx: TreeNodeContext<Node>)}
	{#if node.type !== 'GROUP'}
		{#if editable}
			<select
				class="w-full bg-transparent text-xs focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
				value={node.type}
				onchange={(e) => {
					const v = e.currentTarget.value;
					node.type = v === 'TIME_RELATIVE' ? 'TIME_RELATIVE' : 'FIXED';
					node.unit = v === 'TIME_RELATIVE' ? 'h/Woche' : null;
				}}
			>
				<option value="FIXED">{$_('grid.typeFixed')}</option>
				<option value="TIME_RELATIVE">h/Woche</option>
			</select>
		{:else if node.type === 'TIME_RELATIVE'}
			<span class="px-1.5 py-0.5 text-xs bg-blue-50 text-blue-600 rounded"
				>{node.unit ?? 'h/Woche'}</span
			>
		{/if}
	{/if}
{/snippet}

{#snippet phaseCell(node: Node, _ctx: TreeNodeContext<Node>)}
	{#if node.type !== 'GROUP'}
		{#if editable && phases.length > 0}
			<select
				class="w-full bg-transparent text-sm focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
				value={node.phaseAbbreviation ?? ''}
				onchange={(e) => {
					node.phaseAbbreviation = e.currentTarget.value === '' ? null : e.currentTarget.value;
				}}
			>
				<option value="">{$_('grid.phaseNone')}</option>
				{#each phases as p (p.abbreviation)}
					<option value={p.abbreviation}>{p.abbreviation}</option>
				{/each}
			</select>
		{:else}
			<span class="px-1 text-xs text-gray-500">{node.phaseAbbreviation ?? ''}</span>
		{/if}
	{/if}
{/snippet}

{#snippet optimisticCell(node: Node, ctx: TreeNodeContext<Node>)}
	{#if node.type !== 'GROUP'}
		{#if editable}
			<input
				type="number"
				step="0.1"
				min="0"
				class="w-full text-right bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
				value={node.minEffort ?? ''}
				data-cell="{pathKey(ctx.path)}-1"
				onkeydown={(e) => onKeyDown(e, ctx.path, 1)}
				oninput={(e) => {
					const v = e.currentTarget.value;
					node.minEffort = v === '' ? null : parseFloat(v);
				}}
			/>
		{:else}
			<span class="tabular-nums">{node.minEffort ?? ''}</span>
		{/if}
	{/if}
{/snippet}

{#snippet likelyCell(node: Node, ctx: TreeNodeContext<Node>)}
	{#if node.type !== 'GROUP'}
		{#if editable}
			<input
				type="number"
				step="0.1"
				min="0"
				class="w-full text-right bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
				value={node.expectedEffort ?? ''}
				data-cell="{pathKey(ctx.path)}-2"
				onkeydown={(e) => onKeyDown(e, ctx.path, 2)}
				oninput={(e) => {
					const v = e.currentTarget.value;
					node.expectedEffort = v === '' ? null : parseFloat(v);
				}}
			/>
		{:else}
			<span class="tabular-nums">{node.expectedEffort ?? ''}</span>
		{/if}
	{/if}
{/snippet}

{#snippet pessimisticCell(node: Node, ctx: TreeNodeContext<Node>)}
	{#if node.type !== 'GROUP'}
		{#if editable}
			<input
				type="number"
				step="0.1"
				min="0"
				class="w-full text-right bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
				value={node.maxEffort ?? ''}
				data-cell="{pathKey(ctx.path)}-3"
				onkeydown={(e) => onKeyDown(e, ctx.path, 3)}
				oninput={(e) => {
					const v = e.currentTarget.value;
					node.maxEffort = v === '' ? null : parseFloat(v);
				}}
			/>
		{:else}
			<span class="tabular-nums">{node.maxEffort ?? ''}</span>
		{/if}
	{/if}
{/snippet}

{#snippet pertCell(node: Node, _ctx: TreeNodeContext<Node>)}
	{#if node.type !== 'GROUP'}
		<span class="text-brand-green tabular-nums">
			{num(pert(node.minEffort, node.expectedEffort, node.maxEffort), 2)}
		</span>
	{/if}
{/snippet}

{#snippet assumptionsCell(node: Node, ctx: TreeNodeContext<Node>)}
	{#if node.type !== 'GROUP'}
		{#if editable}
			<input
				type="text"
				class="w-full min-w-0 bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
				value={node.assumptions ?? ''}
				placeholder="…"
				data-cell="{pathKey(ctx.path)}-5"
				onkeydown={(e) => onKeyDown(e, ctx.path, 5)}
				oninput={(e) => {
					node.assumptions = e.currentTarget.value || null;
				}}
			/>
		{:else}
			<span class="px-1 text-gray-500">{node.assumptions ?? ''}</span>
		{/if}
	{/if}
{/snippet}

{#snippet offerPTCell(node: Node, _ctx: TreeNodeContext<Node>)}
	{@const calc = calcMap.get(node.logicalId)}
	{#if node.type !== 'GROUP' && node.type === 'TIME_RELATIVE' && !node.phaseAbbreviation}
		<span class="text-amber-500 text-xs">{$_('grid.needsPhase')}</span>
	{:else}
		<span
			class="text-gray-600 tabular-nums"
			class:group-aggregate-bold={node.type === 'GROUP'}
		>
			{calc != null ? num(calc.offerPT, 2) : '—'}
		</span>
	{/if}
{/snippet}

{#snippet costCell(node: Node, _ctx: TreeNodeContext<Node>)}
	{@const calc = calcMap.get(node.logicalId)}
	<span
		class="text-gray-600 tabular-nums"
		class:group-aggregate-bold={node.type === 'GROUP'}
	>
		{calc != null ? num(calc.cost, 0) : '—'}
	</span>
{/snippet}

{#snippet offerPriceCell(node: Node, _ctx: TreeNodeContext<Node>)}
	{@const calc = calcMap.get(node.logicalId)}
	<span
		class="text-gray-600 tabular-nums"
		class:group-aggregate-bold={node.type === 'GROUP'}
	>
		{calc != null ? num(calc.offerPrice, 0) : '—'}
	</span>
{/snippet}

{#snippet rowActionsSnippet(node: Node, ctx: TreeNodeContext<Node>)}
	<div class="flex items-center gap-1 shrink-0">
		{#if node.type === 'GROUP'}
			<button
				type="button"
				onclick={() => addChildGroupAt(ctx.path)}
				class="text-xs text-brand-green hover:text-[#007a45]"
				title={$_('grid.actionAddChildGroupTitle')}>{$_('grid.actionAddChildGroup')}</button
			>
			<button
				type="button"
				onclick={() => addChildLeafAt(ctx.path)}
				class="text-xs text-brand-green hover:text-[#007a45]"
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

{#snippet totalsFooter(_allRoots: Node[])}
	<div
		class="grid items-center -mx-3 -my-2 font-semibold text-sm bg-gray-50"
		style="grid-template-columns: {gridTemplateColumns}"
	>
		{#if editable}<div class="py-2 px-1"></div>{/if}
		<div class="py-2 px-3 text-xs text-gray-400 uppercase tracking-wide">{$_('grid.total')}</div>
		<div class="py-2 px-2"></div>
		<div class="py-2 px-2"></div>
		<div class="py-2 px-2 text-right tabular-nums">{num(totalOpt, 2)}</div>
		<div class="py-2 px-2 text-right tabular-nums">{num(totalLik, 2)}</div>
		<div class="py-2 px-2 text-right tabular-nums">{num(totalPes, 2)}</div>
		<div class="py-2 px-2 text-right text-brand-green tabular-nums">{num(totalExp, 2)}</div>
		<div class="py-2 px-3"></div>
		<div class="py-2 px-2 text-right text-gray-600 tabular-nums" data-testid="grid-total.offerPT">
			{num(totalOfferPT, 2)}
		</div>
		<div class="py-2 px-2 text-right text-gray-600 tabular-nums" data-testid="grid-total.cost">
			{num(totalCost, 0)}
		</div>
		<div class="py-2 px-2 text-right text-gray-600 tabular-nums" data-testid="grid-total.offerPrice">
			{num(totalOfferPrice, 0)}
		</div>
	</div>
{/snippet}

<!-- data-undo-aware marks the grid cells as the undo surface: the global
	undo/redo shortcut (task-076) acts here instead of the browser's native
	input undo. -->
<div class="border rounded-lg overflow-hidden" data-undo-aware="true">
	{#if roots.length === 0}
		<div class="p-10 text-center text-gray-400">
			<p class="mb-4 text-sm">{$_('grid.empty')}</p>
			{#if editable}
				<button
					type="button"
					onclick={addRootGroup}
					class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]"
					>{$_('grid.addGroup')}</button
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
			rowAttrs={legacyRowAttrs}
			childrenZoneAttrs={legacyZoneAttrs}
			footer={totalsFooter}
		/>
		{#if editable}
			<div class="p-3 border-t bg-gray-50/40">
				<button
					type="button"
					onclick={addRootGroup}
					class="text-sm text-brand-green hover:text-[#007a45]">{$_('grid.addGroupRow')}</button
				>
			</div>
		{/if}
	{/if}
</div>

<style>
	.group-aggregate-bold {
		font-weight: 600;
	}
</style>
