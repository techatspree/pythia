<script lang="ts" module>
	export type Leaf = {
		logicalId: string;
		type: 'FIXED' | 'TIME_RELATIVE';
		description: string;
		minEffort: number | null;
		expectedEffort: number | null;
		maxEffort: number | null;
		assumptions: string | null;
		phaseAbbreviation: string | null;
		unit: string | null;
	};

	export type Group = {
		logicalId: string;
		type: 'GROUP';
		title: string;
		children: Node[];
	};

	export type Node = Leaf | Group;

	export type NodePath = number[];

	export type CalcEntry = { offerPT: number; cost: number; offerPrice: number };
</script>

<script lang="ts">
	import TreeTable from './treetable/TreeTable.svelte';
	import type {
		TreeColumn,
		TreeNodeContext,
		ChildrenChangeEvent
	} from './treetable/types';

	let {
		version,
		editable,
		onchange,
		calcMap = new Map<string, CalcEntry>(),
		phases = []
	}: {
		version: any;
		editable: boolean;
		onchange: (roots: Node[]) => void;
		calcMap?: Map<string, CalcEntry>;
		phases?: any[];
	} = $props();

	function pert(o: number | null, m: number | null, p: number | null): number {
		return ((o ?? 0) + 4 * (m ?? 0) + (p ?? 0)) / 6;
	}

	function newId(): string {
		return crypto.randomUUID();
	}

	function initRoots(v: any): Node[] {
		return (v?.roots ?? []).map(initNode);
	}

	function initNode(n: any): Node {
		if (n.type === 'GROUP') {
			return {
				logicalId: n.logicalId ?? newId(),
				type: 'GROUP',
				title: n.title ?? '',
				children: (n.children ?? []).map(initNode)
			};
		}
		return {
			logicalId: n.logicalId ?? newId(),
			type: n.type === 'TIME_RELATIVE' ? 'TIME_RELATIVE' : 'FIXED',
			description: n.description ?? '',
			minEffort: n.minEffort ?? null,
			expectedEffort: n.expectedEffort ?? null,
			maxEffort: n.maxEffort ?? null,
			assumptions: n.assumptions ?? null,
			phaseAbbreviation: n.phaseAbbreviation ?? null,
			unit: n.unit ?? null
		};
	}

	let roots = $state<Node[]>(initRoots(version));

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

	function serialize(nodes: Node[]): Node[] {
		return nodes.map((n) => {
			if (n.type === 'GROUP') {
				return {
					logicalId: n.logicalId,
					type: 'GROUP',
					title: n.title,
					children: serialize(n.children)
				};
			}
			return {
				logicalId: n.logicalId,
				type: n.type,
				description: n.description,
				minEffort: n.minEffort,
				expectedEffort: n.expectedEffort,
				maxEffort: n.maxEffort,
				assumptions: n.assumptions,
				phaseAbbreviation: n.phaseAbbreviation,
				unit: n.unit
			};
		});
	}

	function notify() {
		onchange(serialize(roots));
	}

	function handleChildrenChange(e: ChildrenChangeEvent<Node>) {
		if (e.phase === 'finalize') notify();
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
		notify();
	}

	function addChildGroupAt(path: NodePath) {
		const g = nodeAt(path) as Group;
		g.children = [
			...g.children,
			{ logicalId: newId(), type: 'GROUP', title: 'New group', children: [] }
		];
		roots = [...roots];
		notify();
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
		notify();
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
		notify();
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
	const calcEntries = $derived(Array.from(calcMap.values()));
	const totalOfferPT = $derived(calcEntries.reduce((s, v) => s + v.offerPT, 0));
	const totalCost = $derived(calcEntries.reduce((s, v) => s + v.cost, 0));
	const totalOfferPrice = $derived(calcEntries.reduce((s, v) => s + v.offerPrice, 0));

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
		{ key: 'description', header: 'Description', width: '1fr', cell: descriptionCell },
		{ key: 'type', header: 'Type', width: '5rem', cell: typeCell },
		{ key: 'phase', header: 'Phase', width: '6rem', cell: phaseCell },
		{ key: 'optimistic', header: 'Optimistic', width: '6rem', align: 'right', cell: optimisticCell },
		{ key: 'likely', header: 'Likely', width: '6rem', align: 'right', cell: likelyCell },
		{
			key: 'pessimistic',
			header: 'Pessimistic',
			width: '6rem',
			align: 'right',
			cell: pessimisticCell
		},
		{ key: 'pert', header: 'PERT', width: '6rem', align: 'right', cell: pertCell },
		{ key: 'assumptions', header: 'Assumptions', width: '1fr', cell: assumptionsCell },
		{
			key: 'offerPT',
			header: 'offerPT (PT)',
			width: '6rem',
			align: 'right',
			cell: offerPTCell
		},
		{
			key: 'cost',
			header: 'Cost (EUR)',
			width: '7rem',
			align: 'right',
			cell: costCell,
			collapsible: true
		},
		{
			key: 'offerPrice',
			header: 'Offer Price (EUR)',
			width: '7rem',
			align: 'right',
			cell: offerPriceCell,
			collapsible: true
		}
	];

	function legacyRowAttrs(_node: Node, ctx: TreeNodeContext<Node>): Record<string, string> {
		return { 'data-testid': `row-${pathKey(ctx.path)}` };
	}

	function legacyZoneAttrs(parent: Node | null): Record<string, string> {
		if (parent == null) return { 'aria-label': 'Estimation root nodes' };
		if (parent.type === 'GROUP') return { 'aria-label': `Children of ${parent.title}` };
		return { 'aria-label': 'Children' };
	}

	const gridTemplateColumns = $derived(
		(editable ? '2rem ' : '') + columns.map((c) => c.width).join(' ') + (editable ? ' 4rem' : '')
	);
</script>

{#snippet descriptionCell(node: Node, ctx: TreeNodeContext<Node>)}
	{#if node.type === 'GROUP'}
		{#if editable}
			<input
				type="text"
				class="bg-transparent font-semibold focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
				value={node.title}
				oninput={(e) => {
					node.title = e.currentTarget.value;
					notify();
				}}
			/>
		{:else}
			<span class="font-semibold text-gray-700">{node.title}</span>
		{/if}
		<span class="ml-2 text-xs font-normal text-gray-400">
			{node.children.length} child{node.children.length !== 1 ? 'ren' : ''}
		</span>
	{:else if editable}
		<input
			type="text"
			class="bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
			value={node.description}
			data-cell="{pathKey(ctx.path)}-0"
			onkeydown={(e) => onKeyDown(e, ctx.path, 0)}
			oninput={(e) => {
				node.description = e.currentTarget.value;
				notify();
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
					notify();
				}}
			>
				<option value="FIXED">Fixed</option>
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
					notify();
				}}
			>
				<option value="">— none —</option>
				{#each phases as p}
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
					notify();
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
					notify();
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
					notify();
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
			{pert(node.minEffort, node.expectedEffort, node.maxEffort).toFixed(2)}
		</span>
	{/if}
{/snippet}

{#snippet assumptionsCell(node: Node, ctx: TreeNodeContext<Node>)}
	{#if node.type !== 'GROUP'}
		{#if editable}
			<input
				type="text"
				class="w-full bg-transparent focus:outline-none focus:bg-brand-green/5 focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
				value={node.assumptions ?? ''}
				placeholder="…"
				data-cell="{pathKey(ctx.path)}-5"
				onkeydown={(e) => onKeyDown(e, ctx.path, 5)}
				oninput={(e) => {
					node.assumptions = e.currentTarget.value || null;
					notify();
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
		<span class="text-amber-500 text-xs">⚠ needs phase</span>
	{:else}
		<span class="text-gray-600 tabular-nums">
			{calc != null ? calc.offerPT.toFixed(2) : '—'}
		</span>
	{/if}
{/snippet}

{#snippet costCell(node: Node, _ctx: TreeNodeContext<Node>)}
	{@const calc = calcMap.get(node.logicalId)}
	<span class="text-gray-600 tabular-nums">
		{calc != null ? calc.cost.toFixed(0) : '—'}
	</span>
{/snippet}

{#snippet offerPriceCell(node: Node, _ctx: TreeNodeContext<Node>)}
	{@const calc = calcMap.get(node.logicalId)}
	<span class="text-gray-600 tabular-nums">
		{calc != null ? calc.offerPrice.toFixed(0) : '—'}
	</span>
{/snippet}

{#snippet rowActionsSnippet(node: Node, ctx: TreeNodeContext<Node>)}
	<div class="flex items-center gap-1">
		{#if node.type === 'GROUP'}
			<button
				type="button"
				onclick={() => addChildGroupAt(ctx.path)}
				class="text-xs text-brand-green hover:text-[#007a45]"
				title="Add child group">+ group</button
			>
			<button
				type="button"
				onclick={() => addChildLeafAt(ctx.path)}
				class="text-xs text-brand-green hover:text-[#007a45]"
				title="Add child item">+ item</button
			>
		{/if}
		<button
			type="button"
			onclick={() => deleteAt(ctx.path)}
			class="text-gray-300 hover:text-red-500 transition-colors leading-none"
			title="Delete"
			aria-label="Delete row">✕</button
		>
	</div>
{/snippet}

{#snippet totalsFooter(_allRoots: Node[])}
	<div
		class="grid items-center -mx-3 -my-2 font-semibold text-sm bg-gray-50"
		style="grid-template-columns: {gridTemplateColumns}"
	>
		{#if editable}<div class="py-2 px-1"></div>{/if}
		<div class="py-2 px-3 text-xs text-gray-400 uppercase tracking-wide">Total</div>
		<div class="py-2 px-2"></div>
		<div class="py-2 px-2"></div>
		<div class="py-2 px-2 text-right tabular-nums">{totalOpt.toFixed(2)}</div>
		<div class="py-2 px-2 text-right tabular-nums">{totalLik.toFixed(2)}</div>
		<div class="py-2 px-2 text-right tabular-nums">{totalPes.toFixed(2)}</div>
		<div class="py-2 px-2 text-right text-brand-green tabular-nums">{totalExp.toFixed(2)}</div>
		<div class="py-2 px-3"></div>
		<div class="py-2 px-2 text-right text-gray-600 tabular-nums">{totalOfferPT.toFixed(2)}</div>
		<div class="py-2 px-2 text-right text-gray-600 tabular-nums">{totalCost.toFixed(0)}</div>
		<div class="py-2 px-2 text-right text-gray-600 tabular-nums">{totalOfferPrice.toFixed(0)}</div>
		{#if editable}<div class="py-2 px-3"></div>{/if}
	</div>
{/snippet}

<div class="border rounded-lg overflow-hidden">
	{#if roots.length === 0}
		<div class="p-10 text-center text-gray-400">
			<p class="mb-4 text-sm">No items yet.</p>
			{#if editable}
				<button
					type="button"
					onclick={addRootGroup}
					class="px-4 py-2 text-sm bg-brand-green text-white rounded hover:bg-[#007a45]"
					>Add group</button
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
			onChildrenChange={handleChildrenChange}
			rowActions={rowActionsSnippet}
			rowAttrs={legacyRowAttrs}
			childrenZoneAttrs={legacyZoneAttrs}
			footer={totalsFooter}
		/>
		{#if editable}
			<div class="p-3 border-t bg-gray-50/40">
				<button
					type="button"
					onclick={addRootGroup}
					class="text-sm text-brand-green hover:text-[#007a45]">+ Add group</button
				>
			</div>
		{/if}
	{/if}
</div>
