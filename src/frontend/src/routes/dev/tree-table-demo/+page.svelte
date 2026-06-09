<script lang="ts">
	import TreeTable from '$lib/components/treetable/TreeTable.svelte';
	import type {
		TreeColumn,
		ChildrenChangeEvent,
		TreeNodeContext
	} from '$lib/components/treetable/types';

	type CatalogLeaf = {
		kind: 'leaf';
		id: string;
		name: string;
		quantity: number;
		unitPrice: number;
	};
	type CatalogGroup = {
		kind: 'group';
		id: string;
		name: string;
		children: CatalogNode[];
	};
	type CatalogNode = CatalogLeaf | CatalogGroup;

	function leaf(id: string, name: string, quantity: number, unitPrice: number): CatalogLeaf {
		return { kind: 'leaf', id, name, quantity, unitPrice };
	}
	function group(id: string, name: string, children: CatalogNode[]): CatalogGroup {
		return { kind: 'group', id, name, children };
	}

	let roots = $state<CatalogNode[]>([
		group('g1', 'Werkzeuge', [
			group('g1a', 'Handwerkzeuge', [
				leaf('l1', 'Hammer', 3, 12.5),
				leaf('l2', 'Schraubenzieher', 5, 6.0)
			]),
			group('g1b', 'Elektrowerkzeuge', [
				leaf('l3', 'Bohrmaschine', 1, 89.0),
				leaf('l4', 'Stichsäge', 1, 65.0)
			])
		]),
		group('g2', 'Verbrauchsmaterial', [
			group('g2a', 'Schrauben', [
				leaf('l5', 'Holzschrauben 4×40', 50, 0.12),
				leaf('l6', 'Spax 5×60', 30, 0.18)
			]),
			group('g2b', 'Klebstoffe', [
				leaf('l7', 'Holzleim 250ml', 2, 4.5),
				leaf('l8', 'Sekundenkleber', 4, 2.99)
			])
		])
	]);

	function getId(n: CatalogNode): string {
		return n.id;
	}
	function getChildren(n: CatalogNode): CatalogNode[] | null {
		return n.kind === 'group' ? n.children : null;
	}

	function leavesOf(n: CatalogNode): CatalogLeaf[] {
		if (n.kind === 'leaf') return [n];
		return n.children.flatMap(leavesOf);
	}

	function sumQuantity(n: CatalogNode): number {
		return leavesOf(n).reduce((s, l) => s + l.quantity, 0);
	}
	function sumLineTotal(n: CatalogNode): number {
		return leavesOf(n).reduce((s, l) => s + l.quantity * l.unitPrice, 0);
	}
	function rootsTotal(rs: CatalogNode[]): number {
		return rs.reduce((s, r) => s + sumLineTotal(r), 0);
	}

	function nodeAtPath(path: number[]): CatalogNode | null {
		if (path.length === 0) return null;
		let current: CatalogNode[] = roots;
		let node: CatalogNode | null = null;
		for (const idx of path) {
			node = current[idx] ?? null;
			if (node == null) return null;
			if (node.kind === 'group') current = node.children;
		}
		return node;
	}

	function handleChildrenChange(e: ChildrenChangeEvent<CatalogNode>) {
		if (e.phase !== 'finalize') return;
		if (e.parentPath.length === 0) {
			roots = e.newChildren;
			return;
		}
		const parent = nodeAtPath(e.parentPath);
		if (parent != null && parent.kind === 'group') {
			parent.children = e.newChildren;
			roots = [...roots];
		}
	}

	const columns: TreeColumn<CatalogNode>[] = [
		{ key: 'name', header: 'Name', width: 'minmax(16rem, 1fr)', cell: nameCell },
		{ key: 'quantity', header: 'Menge', width: '6rem', align: 'right', cell: quantityCell },
		{ key: 'unitPrice', header: 'Stückpreis', width: '7rem', align: 'right', cell: unitPriceCell },
		{ key: 'lineTotal', header: 'Summe', width: '8rem', align: 'right', cell: lineTotalCell }
	];
</script>

{#snippet nameCell(node: CatalogNode, _ctx: TreeNodeContext<CatalogNode>)}
	{#if node.kind === 'group'}
		<span class="font-semibold text-gray-800">{node.name}</span>
	{:else}
		<input
			class="w-full bg-transparent focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
			data-cell="name"
			bind:value={node.name}
		/>
	{/if}
{/snippet}

{#snippet quantityCell(node: CatalogNode, _ctx: TreeNodeContext<CatalogNode>)}
	{#if node.kind === 'group'}
		<span class="text-gray-500 tabular-nums">{sumQuantity(node)}</span>
	{:else}
		<input
			type="number"
			min="0"
			class="w-full text-right bg-transparent focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
			data-cell="quantity"
			bind:value={node.quantity}
		/>
	{/if}
{/snippet}

{#snippet unitPriceCell(node: CatalogNode, _ctx: TreeNodeContext<CatalogNode>)}
	{#if node.kind === 'group'}
		<span></span>
	{:else}
		<input
			type="number"
			min="0"
			step="0.01"
			class="w-full text-right bg-transparent focus:outline-none focus:ring-1 focus:ring-brand-green/40 rounded px-1 py-0.5"
			data-cell="unitPrice"
			bind:value={node.unitPrice}
		/>
	{/if}
{/snippet}

{#snippet lineTotalCell(node: CatalogNode, _ctx: TreeNodeContext<CatalogNode>)}
	{#if node.kind === 'group'}
		<span class="text-gray-700 tabular-nums">€{sumLineTotal(node).toFixed(2)}</span>
	{:else}
		<span class="text-gray-600 tabular-nums">€{(node.quantity * node.unitPrice).toFixed(2)}</span>
	{/if}
{/snippet}

{#snippet totalsFooter(rs: CatalogNode[])}
	<span class="font-semibold">Total: €{rootsTotal(rs).toFixed(2)}</span>
{/snippet}

<div class="p-8 max-w-5xl mx-auto">
	<h1 class="text-2xl font-semibold mb-2">TreeTable demo</h1>
	<p class="text-sm text-gray-600 mb-6">
		A static catalog fixture exercising the generic TreeTable component.
		Drag rows with mouse or keyboard (Tab → Space → Arrow → Space) to reorganise.
	</p>
	<TreeTable
		roots={roots}
		columns={columns}
		getId={getId}
		getChildren={getChildren}
		treeColumnKey="name"
		editable={true}
		onChildrenChange={handleChildrenChange}
		footer={totalsFooter}
	/>
</div>
